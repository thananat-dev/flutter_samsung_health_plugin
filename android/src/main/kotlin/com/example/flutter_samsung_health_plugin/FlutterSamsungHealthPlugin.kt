package com.example.flutter_samsung_health_plugin

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Build
import android.content.pm.PackageManager
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import io.flutter.embedding.engine.plugins.FlutterPlugin
import io.flutter.embedding.engine.plugins.activity.ActivityAware
import io.flutter.embedding.engine.plugins.activity.ActivityPluginBinding
import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.MethodChannel
import io.flutter.plugin.common.MethodChannel.MethodCallHandler
import io.flutter.plugin.common.MethodChannel.Result
import com.samsung.android.sdk.health.data.HealthDataService
import com.samsung.android.sdk.health.data.HealthDataStore
import com.samsung.android.sdk.health.data.permission.AccessType
import com.samsung.android.sdk.health.data.permission.Permission
import com.samsung.android.sdk.health.data.request.DataType
import com.samsung.android.sdk.health.data.request.DataTypes 
import com.samsung.android.sdk.health.data.request.LocalTimeFilter
import com.samsung.android.sdk.health.data.request.LocalDateFilter
import com.samsung.android.sdk.health.data.request.ReadDataRequest
import com.samsung.android.sdk.health.data.request.Ordering
import com.samsung.android.sdk.health.data.helper.aggregate
import com.samsung.android.sdk.health.data.device.Device
import com.samsung.android.sdk.health.data.device.DeviceGroup
import com.samsung.android.sdk.health.data.DeviceManager
 
import java.time.LocalDateTime
import java.time.LocalDate
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class FlutterSamsungHealthPlugin :
    FlutterPlugin,
    MethodCallHandler,
    ActivityAware,
    io.flutter.plugin.common.PluginRegistry.RequestPermissionsResultListener {

    private lateinit var channel: MethodChannel
    private var healthDataStore: HealthDataStore? = null
    private lateinit var activity: Activity
    private lateinit var applicationContext: Context
    private val scope = CoroutineScope(Dispatchers.Main)

    companion object {
        const val CHANNEL_NAME = "flutter_samsung_health_plugin"
        const val SUCCESS = "SUCCESS"
        const val NO_PERMISSION = "NO PERMISSION"
        const val STEP_ACTIVITY = 1
        const val HEART_RATE_ACTIVITY = 2
        const val SLEEP_ACTIVITY = 3
    }

    override fun onAttachedToEngine(flutterPluginBinding: FlutterPlugin.FlutterPluginBinding) {
        channel = MethodChannel(flutterPluginBinding.binaryMessenger, CHANNEL_NAME)
        channel.setMethodCallHandler(this)
        applicationContext = flutterPluginBinding.applicationContext
    }

    private val ANDROID_PERMISSION_REQUEST_CODE = 999

    private fun checkAndRequestAndroidPermissions() {
        if (Build.VERSION.SDK_INT >= 29) { // Android 10+
            val permissions = mutableListOf<String>()
            if (Build.VERSION.SDK_INT >= 29 && ContextCompat.checkSelfPermission(applicationContext, android.Manifest.permission.ACTIVITY_RECOGNITION) != PackageManager.PERMISSION_GRANTED) {
                permissions.add(android.Manifest.permission.ACTIVITY_RECOGNITION)
            }
             // BODY_SENSORS is required for health service type on Android 14+ if using sensors
            if (Build.VERSION.SDK_INT >= 20 && ContextCompat.checkSelfPermission(applicationContext, android.Manifest.permission.BODY_SENSORS) != PackageManager.PERMISSION_GRANTED) {
                permissions.add(android.Manifest.permission.BODY_SENSORS)
            }

            if (permissions.isNotEmpty()) {
                ActivityCompat.requestPermissions(activity, permissions.toTypedArray(), ANDROID_PERMISSION_REQUEST_CODE)
            }
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray): Boolean {
        if (requestCode == ANDROID_PERMISSION_REQUEST_CODE) {
            // We don't strictly block execution here, as Samsung Health connection flow continues.
            // But this ensures the permissions are at least requested.
            return true
        }
        return false
    }

    override fun onAttachedToActivity(binding: io.flutter.embedding.engine.plugins.activity.ActivityPluginBinding) {
        activity = binding.activity
        binding.addRequestPermissionsResultListener(this)
    }

    override fun onDetachedFromActivityForConfigChanges() {
        // No-op
    }

    override fun onReattachedToActivityForConfigChanges(binding: io.flutter.embedding.engine.plugins.activity.ActivityPluginBinding) {
        activity = binding.activity
        binding.addRequestPermissionsResultListener(this)
    }

    override fun onDetachedFromActivity() {
        // No-op
    }

    override fun onMethodCall(call: MethodCall, result: Result) {
        when (call.method) {
            "getPlatformVersion" -> {
                result.success("Android ${android.os.Build.VERSION.RELEASE}")
            }
            "getConnectedDevices" -> getConnectedDevices(result)
            "connect" -> initSamsungHealth(result)
            "checkPermissions" -> checkPermissions(result)
            "requestPermissions" -> requestPermissions(result)
            "requestStepPermission" -> requestPermissions(result) // Alias for backward compatibility
            "readSteps" -> readSteps(result)
            "readHeartRate" -> readHeartRate(result)
            "readSleep" -> readSleep(result)
            "readWaterIntake" -> readWaterIntake(result)
            "readBloodOxygen" -> readBloodOxygen(result)
            "readBloodGlucose" -> readBloodGlucose(result)
            "readBloodPressure" -> readBloodPressure(result)
            "readBodyComposition" -> readBodyComposition(result)
            "readFloorsClimbed" -> readFloorsClimbed(result)
            "readSkinTemperature" -> readSkinTemperature(result)
            "readBodyTemperature" -> readBodyTemperature(result)
            "readNutrition" -> readNutrition(result)
            "readExercise" -> readExercise(result)
            
            // History Methods
            "readStepsHistory" -> readStepsHistory(result)
            "readHeartRateHistory" -> readHeartRateHistory(result)
            "readSleepHistory" -> readSleepHistory(result)
            "readWaterIntakeHistory" -> readWaterIntakeHistory(result)
            "readBloodOxygenHistory" -> readBloodOxygenHistory(result)
            "readBloodGlucoseHistory" -> readBloodGlucoseHistory(result)
            "readBloodPressureHistory" -> readBloodPressureHistory(result)
            "readBodyCompositionHistory" -> readBodyCompositionHistory(result)
            "readFloorsClimbedHistory" -> readFloorsClimbedHistory(result)
            "readSkinTemperatureHistory" -> readSkinTemperatureHistory(result)
            "readBodyTemperatureHistory" -> readBodyTemperatureHistory(result)
            "readNutritionHistory" -> readNutritionHistory(result)
            "readExerciseHistory" -> readExerciseHistory(result)

            // Auto Sync
            "startAutoSync" -> {
                val apiUrl = call.argument<String>("apiUrl")
                val macAddress = call.argument<String>("macAddress")
                val userToken = call.argument<String>("userToken")
                startAutoSync(apiUrl, macAddress, userToken, result)
            }
            "stopAutoSync" -> stopAutoSync(result)

            else -> {
                result.notImplemented()
            }
        }
    }

    // ... (init and permissions) ...

    // Helper for History Range (Last 7 Days)
    private fun getHistoryRange(): Pair<LocalDateTime, LocalDateTime> {
        val now = LocalDateTime.now()
        val end = now
        val start = now.minusDays(7).withHour(0).withMinute(0).withSecond(0)
        return Pair(start, end)
    }

    // --- History Implementations ---

    private fun readStepsHistory(result: Result) {
        if (healthDataStore == null) { result.error("NOT_CONNECTED", "Samsung Health not connected", null); return }
        scope.launch {
            try {
                // Return daily steps AND calories (0.0 for now if unavailable)
                val historyList = mutableListOf<Map<String, Any>>()
                for (i in 0..6) {
                    val dayStart = LocalDate.now().minusDays(i.toLong())
                    val dayFilter = LocalTimeFilter.of(
                        dayStart.atStartOfDay(),
                        dayStart.atTime(23, 59, 59)
                    )
                    
                    val resSteps = healthDataStore!!.aggregate(DataType.StepsType.TOTAL) { setLocalTimeFilter(dayFilter) }
                    var dailyTotal = 0L
                    resSteps.dataList.forEach { dailyTotal += (it.value as? Number)?.toLong() ?: 0L }

                    // Fetch Calories from SDK
                    var dailyCalories = 0.0
                    try {
                        val resCals = healthDataStore!!.aggregate(DataType.ActivitySummaryType.TOTAL_ACTIVE_CALORIES_BURNED) { setLocalTimeFilter(dayFilter) }
                        resCals.dataList.forEach { dailyCalories += (it.value ?: 0.0f).toDouble() }
                    } catch (e: Exception) {
                        // Fallback or ignore if not supported
                        android.util.Log.e("FLUTTER_SH", "History Calorie fetch failed: ${e.message}")
                    }

                    historyList.add(mapOf(
                        "date" to dayStart.toString(),
                        "count" to dailyTotal,
                        "calories" to dailyCalories
                    ))
                }
                result.success(historyList)
            } catch (e: Exception) { result.error("READ_FAILED", e.message, null) }
        }
    }

    private fun readHeartRateHistory(result: Result) {
        if (healthDataStore == null) { result.error("NOT_CONNECTED", "Samsung Health not connected", null); return }
        scope.launch {
            try {
                val (start, end) = getHistoryRange()
                val request = DataTypes.HEART_RATE.readDataRequestBuilder
                   .setLocalTimeFilter(LocalTimeFilter.of(start, end))
                   .setOrdering(Ordering.DESC)
                   .build()

                val response = healthDataStore!!.readData(request)
                val list = response.dataList.map { 
                    val hr = it.getValue(DataType.HeartRateType.HEART_RATE) ?: 0.0f
                    mapOf("date" to it.startTime.toString(), "value" to hr)
                }
                result.success(list)
            } catch (e: Exception) { result.error("READ_FAILED", e.message, null) }
        }
    }
    
    private fun readSleepHistory(result: Result) {
        if (healthDataStore == null) { result.error("NOT_CONNECTED", "Samsung Health not connected", null); return }
        scope.launch {
            try {
                // Read 7 days history
                val now = LocalDateTime.now()
                val historyStart = now.minusDays(7).withHour(0).withMinute(0).withSecond(0).withNano(0)
                val historyFilter = LocalTimeFilter.of(historyStart, now)

                // Correct: Use DataTypes.SLEEP extension for builder
                val sleepRequest = DataTypes.SLEEP.readDataRequestBuilder
                    .setLocalTimeFilter(historyFilter)
                    .setOrdering(Ordering.DESC)
                    .build()

                val sleepResponse = healthDataStore!!.readData(sleepRequest)
                val sleepList = mutableListOf<Map<String, Any>>()

                for (sleepData in sleepResponse.dataList) {
                    val startTime = sleepData.startTime
                    val endTime = sleepData.endTime
                    android.util.Log.d("WoW send time raw from samsung", "Read Sleep: Start=$startTime, End=$endTime")
                    if (startTime != null && endTime != null) {
                        val sessionStartTime = startTime.atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli() / 1000L
                        val sessionEndTime = endTime.atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli() / 1000L

                        android.util.Log.d("FLUTTER_SH_DEBUG", "Read Sleep: Start=$startTime (Local), End=$endTime (Local)")
                        android.util.Log.d("FLUTTER_SH_DEBUG", "System Zone: ${java.time.ZoneId.systemDefault()}")
                        android.util.Log.d("FLUTTER_SH_DEBUG", "Converted Epoch: Start=$sessionStartTime, End=$sessionEndTime")

                        // Extract Sleep Stages from SESSIONS field
                        val stageDataList = mutableListOf<Map<String, Any>>()
                        
                        var deepCount = 0 
                        var lightCount = 0
                        var remCount = 0
                        var awakeCount = 0
                        
                        var deepTotalSec = 0L
                        var lightTotalSec = 0L
                        var remTotalSec = 0L
                        var awakeTotalSec = 0L
                        
                        try {
                            @Suppress("UNCHECKED_CAST")
                            val sessionsField = DataType.SleepType::class.java.getField("SESSIONS")
                            val sessionsFieldObj = sessionsField.get(null) as com.samsung.android.sdk.health.data.data.Field<List<com.samsung.android.sdk.health.data.data.entries.SleepSession>>
                            val sessions = sleepData.getValue(sessionsFieldObj)
                            if (sessions != null) {
                                for (session in sessions) {
                                    val stages = session.stages
                                    if (stages != null) {
                                        for (stage in stages) {
                                            val stageStartEpoch: Long = stage.startTime.toEpochMilli() / 1000L
                                            val stageEndEpoch: Long = stage.endTime.toEpochMilli() / 1000L
                                            val durationSec: Long = stageEndEpoch - stageStartEpoch
                                            val durationMin: Long = durationSec / 60L

                                            val stageType = stage.stage // StageType enum
                                            var releepType = 0
                                            when (stageType) {
                                                DataType.SleepType.StageType.DEEP -> {
                                                    releepType = 241
                                                    deepCount++
                                                    deepTotalSec += durationSec
                                                }
                                                DataType.SleepType.StageType.LIGHT -> {
                                                    releepType = 242
                                                    lightCount++
                                                    lightTotalSec += durationSec
                                                }
                                                DataType.SleepType.StageType.REM -> {
                                                    releepType = 243
                                                    remCount++
                                                    remTotalSec += durationSec
                                                }
                                                DataType.SleepType.StageType.AWAKE -> {
                                                    releepType = 244
                                                    awakeCount++
                                                    awakeTotalSec += durationSec
                                                }
                                                else -> { /* UNDEFINED - skip */ }
                                            }

                                            if (releepType > 0) {
                                                stageDataList.add(mapOf(
                                                    "sleepStartTime" to (stageStartEpoch as Any),
                                                    "sleepLen" to (durationSec as Any),
                                                    "sleepType" to (releepType as Any)
                                                ))
                                            }
                                        }
                                    }
                                }
                            }
                        } catch (e: Exception) {
                             android.util.Log.e("SamsungHealthPlugin", "Sleep stage read failed: ${e.message}")
                        }
                        
                        // Always add session, even if stages empty
                        val allSleepTotalSec = deepTotalSec + lightTotalSec + remTotalSec
                        sleepList.add(mapOf(
                            "startTime" to sessionStartTime,
                            "endTime" to sessionEndTime,
                            
                            "deepSleepCount" to deepCount,
                            "lightSleepCount" to lightCount,
                            "remCount" to remCount,
                            "awakeCount" to awakeCount,
                            "wakeCount" to awakeCount, // Duplicate
                            
                            "deepSleepTotal" to deepTotalSec,
                            "lightSleepTotal" to lightTotalSec,
                            "remTotal" to remTotalSec,
                            "rapidEyeMovementTotal" to remTotalSec, // Duplicate
                            "awakeTotal" to awakeTotalSec,
                            "wakeDuration" to awakeTotalSec, // Duplicate
                            
                            "allSleep" to allSleepTotalSec,
                            "sleepData" to stageDataList
                        ))
                    }
                }       
                
                result.success(sleepList)
            } catch (e: Exception) { result.error("READ_FAILED", e.message, null) }
        }
    }

    private fun readWaterIntakeHistory(result: Result) {
        if (healthDataStore == null) { result.error("NOT_CONNECTED", "Samsung Health not connected", null); return }
        scope.launch {
            try {
                val historyList = mutableListOf<Map<String, Any>>()
                for (i in 0..6) {
                    val dayStart = LocalDate.now().minusDays(i.toLong())
                    val filter = LocalTimeFilter.of(dayStart.atStartOfDay(), dayStart.atTime(23,59,59))
                    val res = healthDataStore!!.aggregate(DataType.WaterIntakeType.TOTAL) {
                        setLocalTimeFilter(filter)
                    }
                    var total = 0.0
                    res.dataList.forEach { total += (it.value ?: 0.0f) }
                    historyList.add(mapOf("date" to dayStart.toString(), "value" to total))
                }
                result.success(historyList)
            } catch (e: Exception) { result.error("READ_FAILED", e.message, null) }
        }
    }


    private fun readBloodOxygenHistory(result: Result) {
        if (healthDataStore == null) { result.error("NOT_CONNECTED", "Samsung Health not connected", null); return }
        scope.launch {
            try {
                val (start, end) = getHistoryRange()
                val request = DataTypes.BLOOD_OXYGEN.readDataRequestBuilder
                   .setLocalTimeFilter(LocalTimeFilter.of(start, end))
                   .setOrdering(Ordering.DESC).build()
                val response = healthDataStore!!.readData(request)
                val list = response.dataList.map { 
                    val v = it.getValue(DataType.BloodOxygenType.OXYGEN_SATURATION) ?: 0.0f
                    mapOf("date" to it.startTime.toString(), "value" to v)
                }
                result.success(list)
            } catch (e: Exception) { result.error("READ_FAILED", e.message, null) }
        }
    }

    private fun readBloodGlucoseHistory(result: Result) {
        if (healthDataStore == null) { result.error("NOT_CONNECTED", "Samsung Health not connected", null); return }
        scope.launch {
            try {
                val (start, end) = getHistoryRange()
                val request = DataTypes.BLOOD_GLUCOSE.readDataRequestBuilder
                   .setLocalTimeFilter(LocalTimeFilter.of(start, end))
                   .setOrdering(Ordering.DESC).build()
                val response = healthDataStore!!.readData(request)
                val list = response.dataList.map { 
                    val v = it.getValue(DataType.BloodGlucoseType.GLUCOSE_LEVEL) ?: 0.0f
                    mapOf("date" to it.startTime.toString(), "value" to v)
                }
                result.success(list)
            } catch (e: Exception) { result.error("READ_FAILED", e.message, null) }
        }
    }

    private fun readBloodPressureHistory(result: Result) {
        if (healthDataStore == null) { result.error("NOT_CONNECTED", "Samsung Health not connected", null); return }
        scope.launch {
            try {
                val (start, end) = getHistoryRange()
                val request = DataTypes.BLOOD_PRESSURE.readDataRequestBuilder
                   .setLocalTimeFilter(LocalTimeFilter.of(start, end))
                   .setOrdering(Ordering.DESC).build()
                val response = healthDataStore!!.readData(request)
                val list = response.dataList.map { 
                    val s = it.getValue(DataType.BloodPressureType.SYSTOLIC) ?: 0.0f
                    val d = it.getValue(DataType.BloodPressureType.DIASTOLIC) ?: 0.0f
                    mapOf("date" to it.startTime.toString(), "systolic" to s, "diastolic" to d)
                }
                result.success(list)
            } catch (e: Exception) { result.error("READ_FAILED", e.message, null) }
        }
    }

    private fun readBodyCompositionHistory(result: Result) {
        if (healthDataStore == null) { result.error("NOT_CONNECTED", "Samsung Health not connected", null); return }
        scope.launch {
            try {
                val (start, end) = getHistoryRange()
                val request = DataTypes.BODY_COMPOSITION.readDataRequestBuilder
                   .setLocalTimeFilter(LocalTimeFilter.of(start, end))
                   .setOrdering(Ordering.DESC).build()
                val response = healthDataStore!!.readData(request)
                val list = response.dataList.map { 
                    val w = it.getValue(DataType.BodyCompositionType.WEIGHT) ?: 0.0f
                    val f = it.getValue(DataType.BodyCompositionType.BODY_FAT) ?: 0.0f
                    val m = it.getValue(DataType.BodyCompositionType.SKELETAL_MUSCLE_MASS) ?: 0.0f
                    mapOf("date" to it.startTime.toString(), "weight" to w, "body_fat" to f, "muscle_mass" to m)
                }
                result.success(list)
            } catch (e: Exception) { result.error("READ_FAILED", e.message, null) }
        }
    }

    private fun readFloorsClimbedHistory(result: Result) {
        if (healthDataStore == null) { result.error("NOT_CONNECTED", "Samsung Health not connected", null); return }
        scope.launch {
            try {
                val historyList = mutableListOf<Map<String, Any>>()
                for (i in 0..6) {
                    val dayStart = LocalDate.now().minusDays(i.toLong())
                    val filter = LocalTimeFilter.of(dayStart.atStartOfDay(), dayStart.atTime(23,59,59))
                    val res = healthDataStore!!.aggregate(DataType.FloorsClimbedType.TOTAL) { setLocalTimeFilter(filter) }
                    var total = 0.0
                    res.dataList.forEach { total += (it.value ?: 0.0f) }
                    historyList.add(mapOf("date" to dayStart.toString(), "value" to total))
                }
                result.success(historyList)
            } catch (e: Exception) { result.error("READ_FAILED", e.message, null) }
        }
    }

    private fun readSkinTemperatureHistory(result: Result) {
        if (healthDataStore == null) { result.error("NOT_CONNECTED", "Samsung Health not connected", null); return }
        scope.launch {
             try {
                val (start, end) = getHistoryRange()
                val request = DataTypes.SKIN_TEMPERATURE.readDataRequestBuilder
                   .setLocalTimeFilter(LocalTimeFilter.of(start, end))
                   .setOrdering(Ordering.DESC).build()
                val response = healthDataStore!!.readData(request)
                val list = response.dataList.map { 
                    val v = it.getValue(DataType.SkinTemperatureType.SKIN_TEMPERATURE) ?: 0.0f
                    mapOf("date" to it.startTime.toString(), "value" to v)
                }
                result.success(list)
            } catch (e: Exception) { result.error("READ_FAILED", e.message, null) }
        }
    }

    private fun readBodyTemperatureHistory(result: Result) {
        if (healthDataStore == null) { result.error("NOT_CONNECTED", "Samsung Health not connected", null); return }
        scope.launch {
             try {
                val (start, end) = getHistoryRange()
                val request = DataTypes.BODY_TEMPERATURE.readDataRequestBuilder
                   .setLocalTimeFilter(LocalTimeFilter.of(start, end))
                   .setOrdering(Ordering.DESC).build()
                val response = healthDataStore!!.readData(request)
                val list = response.dataList.map { 
                    val v = it.getValue(DataType.BodyTemperatureType.BODY_TEMPERATURE) ?: 0.0f
                    mapOf("date" to it.startTime.toString(), "value" to v)
                }
                result.success(list)
            } catch (e: Exception) { result.error("READ_FAILED", e.message, null) }
        }
    }

    private fun readNutritionHistory(result: Result) {
        // Daily total calories
        if (healthDataStore == null) { result.error("NOT_CONNECTED", "Samsung Health not connected", null); return }
        scope.launch {
            try {
                val historyList = mutableListOf<Map<String, Any>>()
                for (i in 0..6) {
                    val dayStart = LocalDate.now().minusDays(i.toLong())
                    val filter = LocalTimeFilter.of(dayStart.atStartOfDay(), dayStart.atTime(23,59,59))
                    val res = healthDataStore!!.aggregate(DataType.NutritionType.TOTAL_CALORIES) { setLocalTimeFilter(filter) }
                    var total = 0.0
                    res.dataList.forEach { total += (it.value ?: 0.0f) }
                    historyList.add(mapOf("date" to dayStart.toString(), "value" to total))
                }
                result.success(historyList)
            } catch (e: Exception) { result.error("READ_FAILED", e.message, null) }
        }
    }

    private fun readExerciseHistory(result: Result) {
         // Daily total exercise calories? Or Session list?
         // User "Exercise Sessions" implies list of sessions.
         // Let's return List of Sessions (points)
        if (healthDataStore == null) { result.error("NOT_CONNECTED", "Samsung Health not connected", null); return }
        scope.launch {
             try {
                // Return SESSIONS for last 7 days? Or just Points?
                // Exercise point usually represents a session/log.
                val (start, end) = getHistoryRange()
                val request = DataTypes.EXERCISE.readDataRequestBuilder
                   .setLocalTimeFilter(LocalTimeFilter.of(start, end))
                   .setOrdering(Ordering.DESC).build()
                val response = healthDataStore!!.readData(request)
                // Map to list of sessions
                val list = response.dataList.map { 
                     // Try to get fields
                     // It seems we should look at SESSIONS field if it exists, OR fields on the point.
                     // ExerciseType has SESSIONS field.
                     // But usually the point itself has start/end.
                     val type = it.getValue(DataType.ExerciseType.EXERCISE_TYPE) // PredefinedEnum
                     // Fix: 'type' is likely an Int (ID) itself, or PredefinedEnum does not have .id exposed this way.
                     // If it's an Int:
                     val typeId = if (type is Number) type.toInt() else 0
                     
                     val title = it.getValue(DataType.ExerciseType.CUSTOM_TITLE) ?: "Exercise"
                     // Durations/Calories provided?
                     // If aggregated ops, can't view on point.
                     // Let's assume point start/end is duration.
                     val durationMins = java.time.Duration.between(it.startTime, it.endTime).toMinutes()
                     
                     // If we can't get calories easily without aggregate, maybe just return duration/type.
                     mapOf(
                         "date" to it.startTime.toString(),
                         "type_id" to typeId,
                         "title" to title,
                         "duration_mins" to durationMins
                     )
                }
                result.success(list)
            } catch (e: Exception) { result.error("READ_FAILED", e.message, null) }
        }
    }

    private fun getConnectedDevices(result: Result) {
        if (healthDataStore == null) {
            result.error("NOT_CONNECTED", "Samsung Health not connected", null)
            return
        }
        scope.launch {
            try {
                val deviceManager = healthDataStore!!.getDeviceManager()
                val allDevices = mutableListOf<Device>()
                
                // Fetch devices for each relevant group
                val groups = listOf(
                    DeviceGroup.WATCH,
                    DeviceGroup.BAND,
                    DeviceGroup.RING,
                    DeviceGroup.ACCESSORY,
                    DeviceGroup.OTHER
                )

                for (group in groups) {
                    try {
                        val devices = deviceManager.getDevices(group)
                        allDevices.addAll(devices)
                        android.util.Log.d("FLUTTER_SH_DEV", "Fetched ${devices.size} devices for group $group")
                    } catch (e: Exception) {
                        android.util.Log.e("FLUTTER_SH_DEV", "Failed to fetch for group $group: ${e.message}")
                    }
                }

                // Also try getOwnDevices just in case it covers something else
                try {
                     val own = deviceManager.getOwnDevices()
                     allDevices.addAll(own)
                     android.util.Log.d("FLUTTER_SH_DEV", "Fetched ${own.size} own devices")
                } catch (e: Exception) {
                     android.util.Log.e("FLUTTER_SH_DEV", "Failed to fetch own devices: ${e.message}")
                }

                // Deduplicate by ID
                val uniqueDevices = allDevices.distinctBy { it.id }

                // Try to enrich names from Bluetooth Paired Devices
                val enrichedDevices = uniqueDevices.map { device ->
                    var finalName = device.name
                    var macAddress: String? = null
                    try {
                        val bluetoothManager = applicationContext.getSystemService(Context.BLUETOOTH_SERVICE) as? android.bluetooth.BluetoothManager
                        val adapter = bluetoothManager?.adapter
                         if (adapter != null && adapter.isEnabled) {
                            val bondedDevices = adapter.bondedDevices
                            // Find a paired device that contains the SH device name (case-insensitive)
                            val shName = device.name
                            if (shName != null) {
                                val matchedBtDevice = bondedDevices.firstOrNull { btDevice ->
                                    btDevice.name != null && btDevice.name.contains(shName, ignoreCase = true)
                                }
                                if (matchedBtDevice != null) {
                                    finalName = matchedBtDevice.name
                                    macAddress = matchedBtDevice.address
                                    android.util.Log.d("FLUTTER_SH_DEV", "Matched Bluetooth Name: $finalName, Mac: $macAddress for SH Device: $shName")
                                }
                            }
                        }
                    } catch (e: SecurityException) {
                        android.util.Log.w("FLUTTER_SH_DEV", "Bluetooth permission denied, skipping name enrichment: ${e.message}")
                    } catch (e: Exception) {
                        android.util.Log.e("FLUTTER_SH_DEV", "Error identifying bluetooth device: ${e.message}")
                    }

                    android.util.Log.d("FLUTTER_SH_DEV", "Device Details: $device, ID: ${device.id}, Name: $finalName, Model: ${device.model}")
                    mapOf(
                        "uuid" to device.id,
                        "customName" to finalName, 
                        "manufacturer" to device.manufacturer,
                        "model" to device.model,
                        "group" to device.deviceType.toString(),
                        "type" to device.deviceType.toString(),
                        "address" to macAddress // Added MAC Address
                    )
                }
                
                android.util.Log.d("FLUTTER_SH_DEV", "Returning ${enrichedDevices.size} unique devices")
                result.success(enrichedDevices)
            } catch (e: Exception) {
                result.error("FETCH_FAILED", e.message, null)
            }
        }
    }

    private fun checkPermissions(result: Result) {
        if (healthDataStore == null) {
            result.success(false)
            return
        }
        scope.launch {
            try {
                // Probe for permission by attempting to read one data point of HEART_RATE
                val now = LocalDateTime.now()
                val request = DataTypes.HEART_RATE.readDataRequestBuilder
                    .setLocalTimeFilter(LocalTimeFilter.of(now.minusMinutes(1), now))
                    .setPageSize(1)
                    .build()
                healthDataStore!!.readData(request)
                // If no exception, permission is likely granted
                result.success(true)
            } catch (e: Exception) {
                // If any exception occurs (SecurityException etc), assume not granted
                result.success(false)
            }
        }
    }

    private fun initSamsungHealth(result: Result) {
        scope.launch {
            try {
                healthDataStore = HealthDataService.getStore(applicationContext)
                result.success("CONNECTED")
            } catch (e: Exception) {
                result.error("FAILED", e.message ?: "Failed to connect to Samsung Health", null)
            }
        }
    }

    private fun requestPermissions(result: Result) {
        // Also request Android runtime permissions (BODY_SENSORS, ACTIVITY_RECOGNITION)
        checkAndRequestAndroidPermissions()

        if (healthDataStore == null) {
            result.error("NOT_CONNECTED", "Samsung Health not connected", null)
            return
        }

        val permissions = setOf(
            Permission.of(DataTypes.STEPS, AccessType.READ),
            Permission.of(DataTypes.HEART_RATE, AccessType.READ),
            Permission.of(DataTypes.SLEEP, AccessType.READ),
            Permission.of(DataTypes.WATER_INTAKE, AccessType.READ),
            Permission.of(DataTypes.BLOOD_OXYGEN, AccessType.READ),
            Permission.of(DataTypes.BLOOD_GLUCOSE, AccessType.READ),
            Permission.of(DataTypes.BLOOD_PRESSURE, AccessType.READ),
            Permission.of(DataTypes.BODY_COMPOSITION, AccessType.READ),
            Permission.of(DataTypes.FLOORS_CLIMBED, AccessType.READ),
            Permission.of(DataTypes.SKIN_TEMPERATURE, AccessType.READ),
            Permission.of(DataTypes.BODY_TEMPERATURE, AccessType.READ),
            Permission.of(DataTypes.NUTRITION, AccessType.READ),
            Permission.of(DataTypes.EXERCISE, AccessType.READ),
            Permission.of(DataTypes.ACTIVE_CALORIES_BURNED_GOAL, AccessType.READ)
        )

        scope.launch {
            try {
                 val grantedPermissions = healthDataStore?.requestPermissions(permissions, activity)
                 if (grantedPermissions != null && grantedPermissions.containsAll(permissions)) {
                     result.success("PERMISSION_GRANTED")
                 } else {
                     result.success("PERMISSION_PARTIALLY_GRANTED")
                 }
            } catch (e: Exception) {
                 result.error("PERMISSION_FAILED", e.message, null)
            }
        }
    }
    
    // ... existing read methods ...

    private fun readSkinTemperature(result: Result) {
        if (healthDataStore == null) {
             result.error("NOT_CONNECTED", "Samsung Health not connected", null)
             return
        }
        scope.launch {
            try {
                 val now = LocalDateTime.now()
                 val startTime = now.minusDays(1)
                 val endTime = now
                 val request = DataTypes.SKIN_TEMPERATURE.readDataRequestBuilder
                    .setLocalTimeFilter(LocalTimeFilter.of(startTime, endTime))
                    .setOrdering(Ordering.DESC)
                    .setPageSize(1)
                    .build()

                 val response = healthDataStore!!.readData(request)
                 val lastData = response.dataList.firstOrNull()
                 if (lastData != null) {
                     val temp = lastData.getValue(DataType.SkinTemperatureType.SKIN_TEMPERATURE)
                     result.success(temp ?: 0.0f)
                 } else {
                     result.success(0.0f)
                 }
            } catch (e: Exception) {
                result.error("READ_FAILED", e.message, null)
            }
        }
    }

    private fun readBodyTemperature(result: Result) {
        if (healthDataStore == null) {
             result.error("NOT_CONNECTED", "Samsung Health not connected", null)
             return
        }
        scope.launch {
            try {
                 val now = LocalDateTime.now()
                 val startTime = now.minusDays(1)
                 val endTime = now
                 val request = DataTypes.BODY_TEMPERATURE.readDataRequestBuilder
                    .setLocalTimeFilter(LocalTimeFilter.of(startTime, endTime))
                    .setOrdering(Ordering.DESC)
                    .setPageSize(1)
                    .build()

                 val response = healthDataStore!!.readData(request)
                 val lastData = response.dataList.firstOrNull()
                 if (lastData != null) {
                     val temp = lastData.getValue(DataType.BodyTemperatureType.BODY_TEMPERATURE)
                     result.success(temp ?: 0.0f)
                 } else {
                     result.success(0.0f)
                 }
            } catch (e: Exception) {
                result.error("READ_FAILED", e.message, null)
            }
        }
    }

    private fun readNutrition(result: Result) {
        if (healthDataStore == null) {
             result.error("NOT_CONNECTED", "Samsung Health not connected", null)
             return
        }
        scope.launch {
            try {
                 val now = LocalDateTime.now()
                 val startTime = now.withHour(0).withMinute(0).withSecond(0).withNano(0)
                 val endTime = now
                 val filter = LocalTimeFilter.of(startTime, endTime)

                 val request = DataTypes.NUTRITION.readDataRequestBuilder
                    .setLocalTimeFilter(filter)
                    .build()
                 
                 val response = healthDataStore!!.readData(request)
                 val rawList = response.dataList.map { 
                     mapOf(
                         "calories" to (it.getValue(DataType.NutritionType.CALORIES) ?: 0.0f).toDouble(),
                         "vitamin_a" to (it.getValue(DataType.NutritionType.VITAMIN_A) ?: 0.0f).toDouble(),
                         "vitamin_c" to (it.getValue(DataType.NutritionType.VITAMIN_C) ?: 0.0f).toDouble(),
                         "calcium" to (it.getValue(DataType.NutritionType.CALCIUM) ?: 0.0f).toDouble(),
                         "iron" to (it.getValue(DataType.NutritionType.IRON) ?: 0.0f).toDouble(),
                         "start_time" to it.startTime.toString()
                     )
                 }

                 // Return RAW LIST
                 result.success(rawList)
            } catch (e: Exception) {
                result.error("READ_FAILED", e.message, null)
            }
        }
    }

    private fun readExercise(result: Result) {
         // Keep Exercise simple/aggregated for now unless requested?
         // User said "whole chunk". Let's update Exercise too just in case or leave it?
         // User specifically mentioned Steps, Sleep, Nutrition in context.
         // Let's stick to the main ones: Steps, Sleep, Nutrition.
         // Actually, Exercise was just Total Calorie.
         // Let's leave Exercise for now to verify main ones first.
        if (healthDataStore == null) {
             result.error("NOT_CONNECTED", "Samsung Health not connected", null)
             return
        }
        scope.launch {
            try {
                 val now = LocalDateTime.now()
                 val startTime = now.withHour(0).withMinute(0).withSecond(0).withNano(0)
                 val endTime = now
                 val filter = LocalTimeFilter.of(startTime, endTime)

                 val response = healthDataStore!!.aggregate(DataType.ExerciseType.TOTAL_CALORIES) {
                     setLocalTimeFilter(filter)
                 }
                 
                 var total = 0.0
                 response.dataList.forEach { data ->
                     total += (data.value ?: 0.0f).toDouble()
                 }
                 result.success(total)
            } catch (e: Exception) {
                result.error("READ_FAILED", e.message, null)
            }
        }
    }


    private fun readSteps(result: Result) {
        if (healthDataStore == null) {
             result.error("NOT_CONNECTED", "Samsung Health not connected", null)
             return
        }

        scope.launch {
            try {
                 val now = LocalDateTime.now()
                 val startTime = now.withHour(0).withMinute(0).withSecond(0).withNano(0)
                 val endTime = now
                 val filter = LocalTimeFilter.of(startTime, endTime)

                 
                 
                 val response = healthDataStore!!.aggregate(DataType.StepsType.TOTAL) {
                     setLocalTimeFilter(filter)
                 }

                 var totalCalories = 0.0
                 try {
                      // Attempt to fetch Active Calories directly
                      val response2 = healthDataStore!!.aggregate(DataType.ActivitySummaryType.TOTAL_ACTIVE_CALORIES_BURNED) {
                          setLocalTimeFilter(filter)
                      }
                      
                      response2.dataList.forEach { data ->
                          totalCalories += (data.value ?: 0.0f).toDouble()
                      }
                      android.util.Log.e("FLUTTER_SH", "ActiveCalories fetch 12312312: ${totalCalories}")
                 } catch (e: Exception) {
                      android.util.Log.e("FLUTTER_SH", "ActiveCalories fetch failed: ${e.message}")
                      // Fallback to estimation if fetch fails
                      totalCalories = 0.0
                 }
                 
                 var totalStep = 0L
                 response.dataList.forEach { data ->
                     totalStep += (data.value ?: 0.0f).toLong()
                 }

                 if (totalCalories == 0.0 && totalStep > 0) {
                     totalCalories = totalStep * 0.04
                 }
                    
                 // Return as a list of 1 item to satisfy the new List interface
                 val list = listOf(mapOf(
                     "count" to totalStep,
                     "calories" to totalCalories,
                     "start_time" to startTime.toString(),
                     "end_time" to endTime.toString()
                 ))
                 result.success(list)
                 
            } catch (e: Exception) {
                result.error("READ_FAILED", e.message, null)
            }
        }
    }
 
    private fun readHeartRate(result: Result) {
        if (healthDataStore == null) {
             result.error("NOT_CONNECTED", "Samsung Health not connected", null)
             return
        }

        scope.launch {
            try {
                 val now = LocalDateTime.now()
                 val startTime = now.minusDays(1) 
                 val endTime = now
                 
                 val request = DataTypes.HEART_RATE.readDataRequestBuilder
                    .setLocalTimeFilter(LocalTimeFilter.of(startTime, endTime))
                    .setOrdering(Ordering.DESC)
                    .setPageSize(1)
                    .build()

                 val response = healthDataStore!!.readData(request)
                 
                 val lastData = response.dataList.firstOrNull()
                 if (lastData != null) {
                     val hr = lastData.getValue(DataType.HeartRateType.HEART_RATE)
                     result.success(hr?.toInt() ?: 0)
                 } else {
                     result.success(0)
                 }
                 
            } catch (e: Exception) {
                result.error("READ_FAILED", e.message, null)
            }
        }
    }

    private fun readSleep(result: Result) {
        if (healthDataStore == null) {
             result.error("NOT_CONNECTED", "Samsung Health not connected", null)
             return
        }
        scope.launch {
            try {
                 val now = LocalDate.now()
                 
                 val startQuery = now.minusDays(1).atTime(12, 0, 0)
                 val endQuery = now.atTime(23, 59, 59)
                 
                 val request = DataTypes.SLEEP.readDataRequestBuilder
                    .setLocalTimeFilter(LocalTimeFilter.of(startQuery, endQuery))
                    .setOrdering(Ordering.ASC) 
                    .build()

                 val response = healthDataStore!!.readData(request)
                 
                 // Map to store aggregated data per DAY (YYYY-MM-DD)
                 val dailyMap = mutableMapOf<String, com.example.flutter_samsung_health_plugin.DailySleepAggregator>()

                 for (sleepData in response.dataList) {
                    val startTime = sleepData.startTime
                    val endTime = sleepData.endTime
                    
                    if (startTime != null && endTime != null) {
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
                                        val tempStages = mutableListOf<Map<String, Any>>()
                                        var tempDeep = 0L; var tempLight = 0L; var tempRem = 0L; var tempAwake = 0L
                                        var cDeep = 0; var cLight = 0; var cRem = 0; var cAwake = 0

                                        for (stage in stages) {
                                            val sStart = stage.startTime.toEpochMilli()
                                            val sEnd = stage.endTime.toEpochMilli()
                                            val dur = (sEnd - sStart) / 1000L
                                            
                                            // Find minimum start time for the session/day
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
                                                tempStages.add(mapOf(
                                                    "sleepStartTime" to (sStart as Any),
                                                    "sleepLen" to (dur as Any),
                                                    "sleepType" to (releepType as Any)
                                                ))
                                            }
                                        }

                                        if (sessStart != Long.MAX_VALUE) {
                                            // 2. Generate Key (YYYY-MM-DD)
                                            val date = java.time.Instant.ofEpochMilli(sessStart)
                                                .atZone(java.time.ZoneId.systemDefault())
                                                .toLocalDate()
                                                .toString()
                                            
                                            // 3. Get or Create Aggregator
                                            val aggregator = dailyMap.getOrPut(date) { com.example.flutter_samsung_health_plugin.DailySleepAggregator() }
                                            
                                            // 4. Update Boundaries
                                            var sessEnd = Long.MIN_VALUE
                                            for (stage in stages) {
                                                val sEnd = stage.endTime.toEpochMilli()
                                                if (sEnd > sessEnd) sessEnd = sEnd
                                            }

                                            if (sessStart < aggregator.startTime) aggregator.startTime = sessStart
                                            if (sessEnd > aggregator.endTime) aggregator.endTime = sessEnd
                                            
                                            // 5. Accumulate SECONDS
                                            aggregator.deepTotalSec += tempDeep
                                            aggregator.lightTotalSec += tempLight
                                            aggregator.remTotalSec += tempRem
                                            aggregator.awakeTotalSec += tempAwake
                                            
                                            aggregator.deepCount += cDeep
                                            aggregator.lightCount += cLight
                                            aggregator.remCount += cRem
                                            aggregator.awakeCount += cAwake
                                            
                                            // 6. Add Stages
                                            aggregator.stageList.addAll(tempStages)
                                        }
                                    }
                                }
                            }
                        } catch (e: Exception) {
                             android.util.Log.e("SamsungHealthPlugin", "Sleep stage read failed in readSleep: ${e.message}")
                        }
                    }
                 } // End Loop
                 
                 val sleepList = mutableListOf<Map<String, Any>>()

                 // Process Aggregated Entries (Day by Day)
                 for ((dateKey, data) in dailyMap) {
                     if (data.startTime != Long.MAX_VALUE && data.endTime != Long.MIN_VALUE) {
                        
                        // === SAMSUNG WATCH LOGIC ===
                        
                        // 1. Calculate Total first using STANDARD ROUNDING
                        // (Total Seconds + 30) / 60
                        val allSleepSec = data.deepTotalSec + data.lightTotalSec + data.remTotalSec
                        val allSleepTotalMin = (allSleepSec + 30) / 60

                        // 2. Calculate Deep, REM, Awake using FLOOR (Integer Division)
                        val deepTotalMin = data.deepTotalSec / 60
                        val remTotalMin = data.remTotalSec / 60
                        val awakeTotalMin = data.awakeTotalSec / 60

                        // 3. Calculate Light as the REMAINDER
                        // Light = Total - Deep - REM
                        val lightTotalMin = allSleepTotalMin - deepTotalMin - remTotalMin
                        
                        // ============================

                        sleepList.add(mapOf(
                            "startTime" to data.startTime,
                            "endTime" to data.endTime,
                            
                            "deepSleepCount" to data.deepCount,
                            "lightSleepCount" to data.lightCount,
                            "remCount" to data.remCount,
                            "awakeCount" to data.awakeCount,
                            "wakeCount" to data.awakeCount, 
                            
                            "deepSleepTotal" to deepTotalMin,
                            "lightSleepTotal" to lightTotalMin,
                            "remTotal" to remTotalMin,
                            "rapidEyeMovementTotal" to remTotalMin, 
                            "awakeTotal" to awakeTotalMin,
                            "wakeDuration" to awakeTotalMin, 
                            
                            "allSleep" to allSleepTotalMin,
                            "sleepData" to data.stageList
                        ))
                     }
                 }
                 
                 result.success(sleepList)
                 
            } catch (e: Exception) {
                result.error("READ_FAILED", e.message, null)
            }
        }
    }


    private fun readWaterIntake(result: Result) {
        if (healthDataStore == null) {
             result.error("NOT_CONNECTED", "Samsung Health not connected", null)
             return
        }

        scope.launch {
            try {
                 val now = LocalDateTime.now()
                 val startTime = now.withHour(0).withMinute(0).withSecond(0).withNano(0)
                 val endTime = now
                 val filter = LocalTimeFilter.of(startTime, endTime)

                 val response = healthDataStore!!.aggregate(DataType.WaterIntakeType.TOTAL) {
                     setLocalTimeFilter(filter)
                 }
                 
                 var totalWater = 0.0
                 response.dataList.forEach { data ->
                     totalWater += data.value ?: 0.0f
                 }
                 result.success(totalWater.toInt())
                 
            } catch (e: Exception) {
                result.error("READ_FAILED", e.message, null)
            }
        }
    }

    private fun readBloodOxygen(result: Result) {
        if (healthDataStore == null) {
             result.error("NOT_CONNECTED", "Samsung Health not connected", null)
             return
        }
        scope.launch {
            try {
                 val now = LocalDateTime.now()
                 val startTime = now.minusDays(1)
                 val endTime = now
                 val request = DataTypes.BLOOD_OXYGEN.readDataRequestBuilder
                    .setLocalTimeFilter(LocalTimeFilter.of(startTime, endTime))
                    .setOrdering(Ordering.DESC)
                    .setPageSize(1)
                    .build()

                 val response = healthDataStore!!.readData(request)
                 val lastData = response.dataList.firstOrNull()
                 if (lastData != null) {
                     val spo2 = lastData.getValue(DataType.BloodOxygenType.OXYGEN_SATURATION)
                     result.success(spo2?.toInt() ?: 0)
                 } else {
                     result.success(0)
                 }
            } catch (e: Exception) {
                result.error("READ_FAILED", e.message, null)
            }
        }
    }

    private fun readBloodGlucose(result: Result) {
        if (healthDataStore == null) {
             result.error("NOT_CONNECTED", "Samsung Health not connected", null)
             return
        }
        scope.launch {
            try {
                 val now = LocalDateTime.now()
                 val startTime = now.minusDays(1)
                 val endTime = now
                 val request = DataTypes.BLOOD_GLUCOSE.readDataRequestBuilder
                    .setLocalTimeFilter(LocalTimeFilter.of(startTime, endTime))
                    .setOrdering(Ordering.DESC)
                    .setPageSize(1)
                    .build()

                 val response = healthDataStore!!.readData(request)
                 val lastData = response.dataList.firstOrNull()
                 if (lastData != null) {
                     val glucose = lastData.getValue(DataType.BloodGlucoseType.GLUCOSE_LEVEL)
                     result.success(glucose ?: 0.0f)
                 } else {
                     result.success(0.0f)
                 }
            } catch (e: Exception) {
                result.error("READ_FAILED", e.message, null)
            }
        }
    }

    private fun readBloodPressure(result: Result) {
        if (healthDataStore == null) {
             result.error("NOT_CONNECTED", "Samsung Health not connected", null)
             return
        }
        scope.launch {
            try {
                 val now = LocalDateTime.now()
                 val startTime = now.minusDays(7) // Look back 7 days
                 val endTime = now
                 val request = DataTypes.BLOOD_PRESSURE.readDataRequestBuilder
                    .setLocalTimeFilter(LocalTimeFilter.of(startTime, endTime))
                    .setOrdering(Ordering.DESC)
                    .setPageSize(1)
                    .build()

                 val response = healthDataStore!!.readData(request)
                 val lastData = response.dataList.firstOrNull()
                 if (lastData != null) {
                     val systolic = lastData.getValue(DataType.BloodPressureType.SYSTOLIC)
                     val diastolic = lastData.getValue(DataType.BloodPressureType.DIASTOLIC)
                     val map = mapOf(
                         "systolic" to (systolic ?: 0.0f),
                         "diastolic" to (diastolic ?: 0.0f)
                     )
                     result.success(map)
                 } else {
                     result.success(emptyMap<String, Float>())
                 }
            } catch (e: Exception) {
                result.error("READ_FAILED", e.message, null)
            }
        }
    }

    private fun readBodyComposition(result: Result) {
        if (healthDataStore == null) {
             result.error("NOT_CONNECTED", "Samsung Health not connected", null)
             return
        }
        scope.launch {
            try {
                 val now = LocalDateTime.now()
                 val startTime = now.minusDays(30) // Look back 30 days
                 val endTime = now
                 val request = DataTypes.BODY_COMPOSITION.readDataRequestBuilder
                    .setLocalTimeFilter(LocalTimeFilter.of(startTime, endTime))
                    .setOrdering(Ordering.DESC)
                    .setPageSize(1)
                    .build()

                 val response = healthDataStore!!.readData(request)
                 val lastData = response.dataList.firstOrNull()
                 if (lastData != null) {
                     val weight = lastData.getValue(DataType.BodyCompositionType.WEIGHT)
                     val height = lastData.getValue(DataType.BodyCompositionType.HEIGHT)
                     val bodyFat = lastData.getValue(DataType.BodyCompositionType.BODY_FAT)
                     val muscle = lastData.getValue(DataType.BodyCompositionType.SKELETAL_MUSCLE_MASS) // or SKELETAL_MUSCLE (ratio)

                     val map = mapOf(
                         "weight" to (weight ?: 0.0f),
                         "height" to (height ?: 0.0f),
                         "body_fat" to (bodyFat ?: 0.0f),
                         "muscle_mass" to (muscle ?: 0.0f)
                     )
                     result.success(map)
                 } else {
                     result.success(emptyMap<String, Float>())
                 }
            } catch (e: Exception) {
                result.error("READ_FAILED", e.message, null)
            }
        }
    }

    private fun readFloorsClimbed(result: Result) {
        if (healthDataStore == null) {
             result.error("NOT_CONNECTED", "Samsung Health not connected", null)
             return
        }

        scope.launch {
            try {
                 val now = LocalDateTime.now()
                 val startTime = now.withHour(0).withMinute(0).withSecond(0).withNano(0)
                 val endTime = now
                 val filter = LocalTimeFilter.of(startTime, endTime)

                 val response = healthDataStore!!.aggregate(DataType.FloorsClimbedType.TOTAL) {
                     setLocalTimeFilter(filter)
                 }
                 
                 var totalFloors = 0.0
                 response.dataList.forEach { data ->
                     totalFloors += data.value ?: 0.0f
                 }
                 result.success(totalFloors)
                 
            } catch (e: Exception) {
                result.error("READ_FAILED", e.message, null)
            }
        }
    }



    override fun onDetachedFromEngine(binding: FlutterPlugin.FlutterPluginBinding) {
        channel.setMethodCallHandler(null)
    }

    // --- Auto Sync Service Control ---

    private fun startAutoSync(apiUrl: String?, macAddress: String?, userToken: String?, result: Result) {
        try {
            val intent = Intent(applicationContext, HealthSyncService::class.java).apply {
                putExtra("api_url", apiUrl ?: "https://rl-2443.achatsocial.com/api/ReleepWatchAPI/saveReleepHealthData/")
                putExtra("mac_address", macAddress ?: "UNKNOWN")
                putExtra("user_token", userToken ?: "")
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                applicationContext.startForegroundService(intent)
            } else {
                applicationContext.startService(intent)
            }
            android.util.Log.d("FLUTTER_SH", "AutoSync service started")
            result.success("AUTO_SYNC_STARTED")
        } catch (e: Exception) {
            android.util.Log.e("FLUTTER_SH", "Failed to start AutoSync: ${e.message}")
            result.error("SERVICE_START_FAILED", e.message, null)
        }
    }

    private fun stopAutoSync(result: Result) {
        try {
            val intent = Intent(applicationContext, HealthSyncService::class.java)
            applicationContext.stopService(intent)
            android.util.Log.d("FLUTTER_SH", "AutoSync service stopped")
            result.success("AUTO_SYNC_STOPPED")
        } catch (e: Exception) {
            android.util.Log.e("FLUTTER_SH", "Failed to stop AutoSync: ${e.message}")
            result.error("SERVICE_STOP_FAILED", e.message, null)
        }
    }
}
