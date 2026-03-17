package com.example.flutter_samsung_health_plugin

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.util.Log
import android.content.pm.ServiceInfo
import com.samsung.android.sdk.health.data.HealthDataService
import com.samsung.android.sdk.health.data.HealthDataStore
import com.samsung.android.sdk.health.data.request.DataType
import com.samsung.android.sdk.health.data.request.DataTypes
import com.samsung.android.sdk.health.data.request.LocalTimeFilter
import com.samsung.android.sdk.health.data.request.ReadDataRequest
import com.samsung.android.sdk.health.data.request.Ordering
import com.samsung.android.sdk.health.data.device.DeviceGroup
import com.samsung.android.sdk.health.data.helper.aggregate
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import java.time.LocalDate
import java.time.LocalDateTime

class HealthSyncService : Service() {

    companion object {
        private const val TAG = "HealthSyncService"
        private const val CHANNEL_ID = "health_sync_channel"
        private const val NOTIFICATION_ID = 1001
        private const val SYNC_INTERVAL_MS = 60_000L * 15 // 15 minutes
        private const val EXTRA_API_URL = "api_url"
        private const val EXTRA_MAC_ADDRESS = "mac_address"
        private const val EXTRA_USER_TOKEN = "user_token"
        private const val DEFAULT_API_URL = "https://rl-2443.achatsocial.com/api/ReleepWatchAPI/saveReleepHealthData/"
    }

    private var healthDataStore: HealthDataStore? = null
    private val handler = Handler(Looper.getMainLooper())
    private val scope = CoroutineScope(Dispatchers.IO)
    private var apiUrl: String = DEFAULT_API_URL
    private var macAddress: String = "UNKNOWN"
    private var userToken: String = ""
    private var isSyncing = false
    private var syncCount = 0

    private val syncRunnable = object : Runnable {
        override fun run() {
            if (!isSyncing) {
                Log.d(TAG, "Timer fired — starting sync #${syncCount + 1}...")
                syncHealthData()
            } else {
                Log.d(TAG, "Previous sync still running, skipping...")
            }
            handler.postDelayed(this, SYNC_INTERVAL_MS)
        }
    }

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "Service created")
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        apiUrl = intent?.getStringExtra(EXTRA_API_URL) ?: DEFAULT_API_URL
        macAddress = intent?.getStringExtra(EXTRA_MAC_ADDRESS) ?: "UNKNOWN"
        userToken = intent?.getStringExtra(EXTRA_USER_TOKEN) ?: ""

        Log.d(TAG, "Service starting — API: $apiUrl, MAC: $macAddress, Token: ${if (userToken.isNotEmpty()) "Present" else "MISSING"}")

        // Start as foreground service
        val notification = buildNotification("Samsung Health Sync Running", "Initializing...")
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                startForeground(NOTIFICATION_ID, notification, android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_HEALTH)
            } else {
                startForeground(NOTIFICATION_ID, notification)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start foreground service: ${e.message}")
            try {
                startForeground(NOTIFICATION_ID, notification) // Fallback for older Android versions or if type fails
            } catch (e2: Exception) {
                Log.e(TAG, "Failed to start foreground service (fallback): ${e2.message}")
            }
        }

        // Initialize Samsung Health
        scope.launch {
            try {
                Log.d(TAG, "Attempting to connect to Samsung Health SDK...")
                updateNotification("Samsung Health Sync", "Connecting to Samsung Health...")
                healthDataStore = HealthDataService.getStore(applicationContext)
                Log.d(TAG, "Samsung Health connected successfully, store: $healthDataStore")
                updateNotification("Samsung Health Sync Running", "Connected — syncing every 15 min | Token: ${if (userToken.isNotEmpty()) "OK" else "MISSING"}")

                // Start periodic sync
                handler.removeCallbacks(syncRunnable)
                handler.post(syncRunnable) // Run immediately, then every SYNC_INTERVAL_MS
            } catch (e: Exception) {
                Log.e(TAG, "Failed to connect to Samsung Health: ${e.message}", e)
                updateNotification("Samsung Health Sync ERROR", "SDK connection failed: ${e.message}")
            }
        }

        return START_STICKY
    }

    private fun syncHealthData() {
        if (healthDataStore == null) {
            Log.w(TAG, "HealthDataStore is null, skipping sync")
            return
        }

        isSyncing = true
        scope.launch {
            try {
                updateNotification("Samsung Health Sync", "Reading data...")

                val now = LocalDateTime.now()
                val historyStart = now.withHour(0).withMinute(0).withSecond(0).withNano(0)
                val historyFilter = LocalTimeFilter.of(historyStart, now)

                // Collect all data points (each with its own startTime)
                val healthDataList = JSONArray()
                var stepsCount = 0
                var heartCount = 0
                var spo2Count = 0
                var bpCount = 0
                var tempCount = 0

                val mergedDataMap = mutableMapOf<Long, JSONObject>()
                val dailyStepMap = mutableMapOf<String, Long>() // YYYY-MM-DD -> totalSteps
                
                fun getOrAddEntry(timestamp: Long): JSONObject {
                    return mergedDataMap.getOrPut(timestamp) {
                        JSONObject().apply { 
                            put("startTime", timestamp)
                            // Inject daily steps for this record's date
                            val recordDate = java.time.Instant.ofEpochSecond(timestamp)
                                .atZone(java.time.ZoneId.systemDefault())
                                .toLocalDate()
                            val stepCount = dailyStepMap[recordDate.toString()] ?: 0L
                            if (stepCount > 0) {
                                put("stepValue", stepCount)
                            }
                        }
                    }
                }

                // === Read Steps (daily aggregates for 1 day - TODAY ONLY) ===
                var latestStepValue = 0L
                try {
                    for (i in 0..0) {
                        val dayStart = LocalDate.now().minusDays(i.toLong())
                        val dayFilter = LocalTimeFilter.of(
                            dayStart.atStartOfDay(),
                            dayStart.atTime(23, 59, 59)
                        )
                        val resSteps = healthDataStore!!.aggregate(DataType.StepsType.TOTAL) {
                            setLocalTimeFilter(dayFilter)
                        }
                        var dailyTotal = 0L
                        resSteps.dataList.forEach { 
                             dailyTotal += (it.value as? Number)?.toLong() ?: 0L
                        }

                        // Always populate daily step map for injection into other metrics
                        dailyStepMap[dayStart.toString()] = dailyTotal

                        if (dailyTotal > 0) {
                            val epoch: Long = if (i == 0) {
                                System.currentTimeMillis() / 1000
                            } else {
                                dayStart.atStartOfDay()
                                    .atZone(java.time.ZoneId.systemDefault())
                                    .toInstant().epochSecond
                            }
                            
                            val entry = getOrAddEntry(epoch)
                            entry.put("stepValue", dailyTotal)
                            if (i == 0) latestStepValue = dailyTotal
                            stepsCount++
                        }
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Steps history read failed: ${e.message}")
                }

                // === Read Heart Rate History ===
                var latestHeartValue = 0
                try {
                    val request = DataTypes.HEART_RATE.readDataRequestBuilder
                        .setLocalTimeFilter(historyFilter)
                        .setOrdering(Ordering.DESC)
                        .build()
                    val response = healthDataStore!!.readData(request)
                    for ((index, data) in response.dataList.withIndex()) {
                        val hr = data.getValue(DataType.HeartRateType.HEART_RATE)?.toInt() ?: 0
                        if (hr > 0) {
                            if (index == 0) latestHeartValue = hr
                            val startTime = data.startTime
                            if (startTime != null) {
                                val epoch = startTime.atZone(java.time.ZoneId.systemDefault())
                                    .toInstant().epochSecond
                                
                                val entry = getOrAddEntry(epoch)
                                entry.put("heartValue", hr)
                                heartCount++
                            }
                        }
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Heart rate history read failed: ${e.message}")
                }

                // === Read SpO2 History ===
                try {
                    val request = DataTypes.BLOOD_OXYGEN.readDataRequestBuilder
                        .setLocalTimeFilter(historyFilter)
                        .setOrdering(Ordering.DESC)
                        .build()
                    val response = healthDataStore!!.readData(request)
                    for (data in response.dataList) {
                        val spo2 = data.getValue(DataType.BloodOxygenType.OXYGEN_SATURATION)?.toInt() ?: 0
                        if (spo2 > 0) {
                            val startTime = data.startTime
                            if (startTime != null) {
                                val epoch = startTime.atZone(java.time.ZoneId.systemDefault())
                                    .toInstant().epochSecond
                                
                                val entry = getOrAddEntry(epoch)
                                entry.put("OOValue", spo2)
                                spo2Count++
                            }
                        }
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "SpO2 history read failed: ${e.message}")
                }

                // === Read Blood Pressure History ===
                try {
                    val request = DataTypes.BLOOD_PRESSURE.readDataRequestBuilder
                        .setLocalTimeFilter(historyFilter)
                        .setOrdering(Ordering.DESC)
                        .build()
                    val response = healthDataStore!!.readData(request)
                    for (data in response.dataList) {
                        val sbp = data.getValue(DataType.BloodPressureType.SYSTOLIC)?.toInt() ?: 0
                        val dbp = data.getValue(DataType.BloodPressureType.DIASTOLIC)?.toInt() ?: 0
                        if (sbp > 0 || dbp > 0) {
                            val startTime = data.startTime
                            if (startTime != null) {
                                val epoch = startTime.atZone(java.time.ZoneId.systemDefault())
                                    .toInstant().epochSecond
                                
                                val entry = getOrAddEntry(epoch)
                                if (sbp > 0) entry.put("SBPValue", sbp)
                                if (dbp > 0) entry.put("DBPValue", dbp)
                                bpCount++
                            }
                        }
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Blood pressure history read failed: ${e.message}")
                }

                // === Read Skin Temperature History ===
                try {
                    val request = DataTypes.SKIN_TEMPERATURE.readDataRequestBuilder
                        .setLocalTimeFilter(historyFilter)
                        .setOrdering(Ordering.DESC)
                        .build()
                    val response = healthDataStore!!.readData(request)
                    for (data in response.dataList) {
                        val temp = (data.getValue(DataType.SkinTemperatureType.SKIN_TEMPERATURE) ?: 0.0f).toDouble()
                        if (temp > 0) {
                            val startTime = data.startTime
                            if (startTime != null) {
                                val epoch = startTime.atZone(java.time.ZoneId.systemDefault())
                                    .toInstant().epochSecond
                                val tempInt = temp.toInt()
                                val tempFloat = ((temp - tempInt) * 10).toInt()
                                
                                val entry = getOrAddEntry(epoch)
                                entry.put("tempIntValue", tempInt)
                                entry.put("tempFloatValue", tempFloat)
                                tempCount++
                            }
                        }
                    }
                } catch (e: Exception) {
                Log.e(TAG, "Skin temp history read failed: ${e.message}")
                }

                // Convert merged map to final JSON list with STRICT FILTERING
                // Only entries with stepValue > 0 are allowed to be sent
                val filteredList = mergedDataMap.values.filter { it.has("stepValue") && it.optLong("stepValue", 0L) > 0L }
                filteredList.forEach { healthDataList.put(it) }

                Log.d(TAG, "Data read complete — Total Merged:${mergedDataMap.size} Final Filtered:${healthDataList.length()}")
                updateNotification("Samsung Health Sync", "Read: Steps=$stepsCount HR=$heartCount Filtered=${healthDataList.length()}")
                var sleepCount = 0
                val sleepDataList = JSONArray() // Separate list for sleep API
                try {
                    val sleepRequest = DataTypes.SLEEP.readDataRequestBuilder
                        .setLocalTimeFilter(historyFilter)
                        .setOrdering(Ordering.DESC)
                        .build()

                    val sleepResponse = healthDataStore!!.readData(sleepRequest)

                    // Map to store aggregated data per DAY (YYYY-MM-DD)
                    // Map to store aggregated data per DAY (YYYY-MM-DD)
                    val dailyMap = mutableMapOf<String, DailySleepAggregator>()

                    // (Removed local class DailySleepAggregator here - using shared file)

                    for (sleepData in sleepResponse.dataList) {
                        try {
                            @Suppress("UNCHECKED_CAST")
                            val sessionsField = DataType.SleepType::class.java.getField("SESSIONS")
                            val sessionsFieldObj = sessionsField.get(null) as com.samsung.android.sdk.health.data.data.Field<List<com.samsung.android.sdk.health.data.data.entries.SleepSession>>
                            val sessions = sleepData.getValue(sessionsFieldObj)
                            
                            if (sessions != null) {
                                for (session in sessions) {
                                    val stages = session.stages
                                    if (stages != null) {
                                        var sessStart = Long.MAX_VALUE
                                        val tempStages = mutableListOf<JSONObject>()
                                        var tempDeep = 0L; var tempLight = 0L; var tempRem = 0L; var tempAwake = 0L
                                        var cDeep = 0; var cLight = 0; var cRem = 0; var cAwake = 0
                                        
                                        for (stage in stages) {
                                            val sStart = stage.startTime.toEpochMilli()
                                            val sEnd = stage.endTime.toEpochMilli()
                                            val dur = (sEnd - sStart) / 1000L
                                            
                                            if (sStart < sessStart) sessStart = sStart
                                            
                                            val stageType = stage.stage
                                            var releepType = 0
                                            when (stageType) {
                                                DataType.SleepType.StageType.DEEP -> { releepType = 241; cDeep++; tempDeep += dur }
                                                DataType.SleepType.StageType.LIGHT -> { releepType = 242; cLight++; tempLight += dur }
                                                DataType.SleepType.StageType.REM -> { releepType = 243; cRem++; tempRem += dur }
                                                DataType.SleepType.StageType.AWAKE -> { releepType = 244; cAwake++; tempAwake += dur }
                                                else -> { /* unknown */ }
                                            }

                                            if (releepType > 0) {
                                                val seg = JSONObject()
                                                seg.put("sleepStartTime", sStart as Any)
                                                seg.put("sleepLen", dur as Any)
                                                seg.put("sleepType", releepType as Any)
                                                tempStages.add(seg)
                                            }
                                        }

                                        if (sessStart != Long.MAX_VALUE) {
                                            // Generate Key (YYYY-MM-DD)
                                            val date = java.time.Instant.ofEpochMilli(sessStart)
                                                .atZone(java.time.ZoneId.systemDefault())
                                                .toLocalDate()
                                                .toString()
                                            
                                            val aggregator = dailyMap.getOrPut(date) { DailySleepAggregator() }
                                            
                                            var sessEnd = Long.MIN_VALUE
                                            for (stage in stages) {
                                                val sEnd = stage.endTime.toEpochMilli()
                                                if (sEnd > sessEnd) sessEnd = sEnd
                                            }

                                            if (sessStart < aggregator.startTime) aggregator.startTime = sessStart
                                            if (sessEnd > aggregator.endTime) aggregator.endTime = sessEnd
                                            
                                            aggregator.deepTotalSec += tempDeep
                                            aggregator.lightTotalSec += tempLight
                                            aggregator.remTotalSec += tempRem
                                            aggregator.awakeTotalSec += tempAwake
                                            
                                            aggregator.deepCount += cDeep
                                            aggregator.lightCount += cLight
                                            aggregator.remCount += cRem
                                            aggregator.awakeCount += cAwake
                                            
                                            for (seg in tempStages) {
                                                aggregator.stageArray.put(seg)
                                            }
                                        }
                                    }
                                }
                            }
                        } catch (e: Exception) {
                            Log.e(TAG, "Sleep stage read failed: ${e.message}")
                        }
                    }

                    // Process Aggregated Single Entries (One Per Day)
                    for ((dateKey, data) in dailyMap) {
                         if (data.startTime != Long.MAX_VALUE && data.endTime != Long.MIN_VALUE) {
                            
                            val allSleepSec = data.deepTotalSec + data.lightTotalSec + data.remTotalSec
                            val allSleepTotalMin = (allSleepSec + 30) / 60

                            val deepTotalMin = data.deepTotalSec / 60
                            val remTotalMin = data.remTotalSec / 60
                            val awakeTotalMin = data.awakeTotalSec / 60

                            val lightTotalMin = allSleepTotalMin - deepTotalMin - remTotalMin
                            
                            Log.d(TAG, "=== Daily Sleep Entry ($dateKey) ===")
                            Log.d(TAG, "  Total: ${allSleepTotalMin}m")

                            val entry = JSONObject()
                            entry.put("startTime", data.startTime)
                            entry.put("endTime", data.endTime)
                            
                            entry.put("deepSleepCount", data.deepCount)
                            entry.put("lightSleepCount", data.lightCount) 
                            entry.put("remCount", data.remCount)
                            entry.put("awakeCount", data.awakeCount)
                            entry.put("wakeCount", data.awakeCount)

                            entry.put("deepSleepTotal", deepTotalMin)
                            entry.put("lightSleepTotal", lightTotalMin)
                            entry.put("remTotal", remTotalMin)
                            entry.put("rapidEyeMovementTotal", remTotalMin)
                            entry.put("awakeTotal", awakeTotalMin)
                            entry.put("wakeDuration", awakeTotalMin)
                            
                            entry.put("allSleep", allSleepTotalMin)
                            
                            entry.put("sleepData", data.stageArray)
                            
                            sleepDataList.put(entry)
                            sleepCount++
                         }
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Sleep history read failed: ${e.message}")
                }

                Log.d(TAG, "Data read complete — Steps:$stepsCount HR:$heartCount SpO2:$spo2Count BP:$bpCount Temp:$tempCount Sleep:$sleepCount")
                updateNotification("Samsung Health Sync", "Read: Steps=$stepsCount HR=$heartCount Sleep=$sleepCount")

                // === Get MAC address from connected devices if not provided ===
                var watchMac = macAddress
                if (watchMac == "UNKNOWN") {
                    try {
                        val deviceManager = healthDataStore!!.getDeviceManager()
                        val devices = deviceManager.getDevices(DeviceGroup.WATCH)
                        if (devices.isNotEmpty()) {
                            val device = devices.first()
                            try {
                                val btManager = applicationContext.getSystemService(Context.BLUETOOTH_SERVICE) as? android.bluetooth.BluetoothManager
                                val adapter = btManager?.adapter
                                if (adapter != null && adapter.isEnabled) {
                                    val bondedDevices = adapter.bondedDevices
                                    val matched = bondedDevices.firstOrNull { bt ->
                                        bt.name != null && device.name != null && bt.name.contains(device.name!!, ignoreCase = true)
                                    }
                                    if (matched != null) {
                                        watchMac = if (matched.address.startsWith("SS-")) matched.address else "SS-${matched.address}"
                                    }
                                }
                            } catch (e: SecurityException) {
                                Log.w(TAG, "Bluetooth permission denied: ${e.message}")
                            }
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Device lookup failed: ${e.message}")
                    }
                }

                // === Build API Payloads ===
                
                // 1. General Health Data (Steps, HR, etc.)
                var apiResult = "No Data"
                if (healthDataList.length() > 0) {
                    val healthPayload = JSONObject().apply {
                        put("watchMacAddress", watchMac)
                        put("watchHealthData", healthDataList)
                    }
                    Log.d(TAG, "Sending Health Data (${healthDataList.length()} records) to $apiUrl")
                    updateNotification("Samsung Health Sync", "Sending ${healthDataList.length()} health records...")
                    apiResult = postToApi(apiUrl, healthPayload.toString())
                }

                // 2. Sleep Data (Separate API)
                if (sleepDataList.length() > 0) {
                    val sleepApiUrl = if (apiUrl.contains("saveReleepHealthData")) {
                        apiUrl.replace("saveReleepHealthData", "saveReleepHealthSleep")
                    } else {
                        // Fallback: If URL doesn't follow expected pattern, try appending
                        apiUrl.trimEnd('/') + "/saveReleepHealthSleep/"
                    }
                    val sleepPayload = JSONObject().apply {
                        put("watchMacAddress", watchMac)
                        put("watchHealthSleep", sleepDataList)
                    }
                    Log.d(TAG, "Sending Sleep Data (${sleepDataList.length()} records) to $sleepApiUrl")
                    updateNotification("Samsung Health Sync", "Sending ${sleepDataList.length()} sleep records...")
                    val sleepResult = postToApi(sleepApiUrl, sleepPayload.toString())
                    apiResult = "$apiResult | Sleep: $sleepResult"
                }
                
                if (healthDataList.length() == 0 && sleepDataList.length() == 0) {
                     Log.w(TAG, "No health data points to send — all reads returned empty")
                     updateNotification("Samsung Health Sync", "⚠ No data to send.")
                } else {
                     updateNotification("Samsung Health Sync", "API: $apiResult")
                }

                syncCount++
                val timeStr = "%02d:%02d:%02d".format(now.hour, now.minute, now.second)
                updateNotification(
                    "Samsung Health Sync Running",
                    "Last sync: $timeStr (#$syncCount) | Valid Sync"
                )

            } catch (e: Exception) {
                Log.e(TAG, "Sync failed: ${e.message}", e)
                updateNotification("Samsung Health Sync", "Sync failed: ${e.message}")
            } finally {
                isSyncing = false
            }
        }
    }

    private fun postToApi(urlStr: String, jsonPayload: String): String {
        try {
            val url = URL(urlStr)
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "POST"
            connection.setRequestProperty("Content-Type", "application/json")
            if (userToken.isNotEmpty()) {
                connection.setRequestProperty("Authorization", "Bearer $userToken")
            } else {
                Log.w(TAG, "⚠ No auth token — API will likely return 401")
            }
            connection.doOutput = true
            connection.connectTimeout = 15_000
            connection.readTimeout = 15_000

            val writer = OutputStreamWriter(connection.outputStream)
            writer.write(jsonPayload)
            writer.flush()
            writer.close()

            val responseCode = connection.responseCode
            val responseBody = try {
                connection.inputStream.bufferedReader().readText()
            } catch (e: Exception) {
                connection.errorStream?.bufferedReader()?.readText() ?: "No response body"
            }

            Log.d(TAG, "API Response: $responseCode — $responseBody")
            connection.disconnect()

            if (responseCode == 200 || responseCode == 201) {
                return "$responseCode OK ✓"
            } else if (responseCode == 401) {
                return "$responseCode UNAUTHORIZED — token expired?"
            } else {
                return "$responseCode ERROR — ${responseBody.take(100)}"
            }
        } catch (e: Exception) {
            Log.e(TAG, "API POST failed: ${e.message}", e)
            return "FAILED: ${e.message}"
        }
    }

    // --- Notification helpers ---

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Health Data Sync",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Periodic Samsung Health data synchronization"
                setShowBadge(false)
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    private fun buildNotification(title: String, text: String): Notification {
        return Notification.Builder(this, CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(text)
            .setSmallIcon(android.R.drawable.ic_popup_sync)
            .setOngoing(true)
            .build()
    }

    private fun updateNotification(title: String, text: String) {
        val notification = buildNotification(title, text)
        val manager = getSystemService(NotificationManager::class.java)
        manager.notify(NOTIFICATION_ID, notification)
    }

    override fun onDestroy() {
        Log.d(TAG, "Service destroyed — stopping sync")
        handler.removeCallbacks(syncRunnable)
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
