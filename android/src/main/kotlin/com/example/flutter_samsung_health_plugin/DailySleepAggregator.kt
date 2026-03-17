package com.example.flutter_samsung_health_plugin

import org.json.JSONArray
import org.json.JSONObject

class DailySleepAggregator {
    var startTime: Long = Long.MAX_VALUE
    var endTime: Long = Long.MIN_VALUE
    var deepTotalSec: Long = 0
    var lightTotalSec: Long = 0
    var remTotalSec: Long = 0
    var awakeTotalSec: Long = 0
    var deepCount: Int = 0
    var lightCount: Int = 0
    var remCount: Int = 0
    var awakeCount: Int = 0
    val stageArray = JSONArray()
    val stageList = mutableListOf<Map<String, Any>>()
}
