package com.pim.app.forensics

import org.json.JSONArray
import org.json.JSONObject

/**
 * 取证事件负载的唯一构造点。
 *
 * 负载**只含结构化诊断字段**：不含经纬度、令牌、口令、账号信息（AC-27.1 / AC-27.3），
 * 也**不记录任何应用使用内容**，前台应用只留包名与应用名（AC-4.2）。
 */
object ForensicPayloads {

    /** 心跳负载（REQ-3 / REQ-4）。 */
    fun heartbeat(snapshot: HeartbeatSnapshot): String {
        val json = JSONObject()
            .put("bootElapsedMs", snapshot.bootElapsedMillis)
            .put("sinceLastHeartbeatMs", snapshot.sinceLastHeartbeatMillis ?: JSONObject.NULL)
            .put("standbyBucket", snapshot.standbyBucket ?: JSONObject.NULL)
            .put("standbyBucketLabel", snapshot.standbyBucketLabel)
            .put("ignoringBatteryOptimizations", snapshot.ignoringBatteryOptimizations ?: JSONObject.NULL)
            .put("dozeMode", snapshot.dozeMode ?: JSONObject.NULL)
            .put("powerSaveMode", snapshot.powerSaveMode ?: JSONObject.NULL)
            .put("foregroundServiceRunning", snapshot.foregroundServiceRunning)
        putContext(json, snapshot.context)
        return json.toString()
    }

    /**
     * 进程退出记录负载（REQ-1 / REQ-4）。
     * [description] 是系统描述文本，可为空；为空时不写该字段，而不是填猜测值（AC-4.2）。
     */
    fun processExit(
        reason: String,
        occurredAtMillis: Long,
        importance: Int?,
        pssKb: Long?,
        rssKb: Long?,
        description: String?,
        inference: String?,
        context: ForensicContext
    ): String {
        val json = JSONObject()
            .put("reason", reason)
            .put("timestampMs", occurredAtMillis)
            .put("importance", importance ?: JSONObject.NULL)
            .put("pssKb", pssKb ?: JSONObject.NULL)
            .put("rssKb", rssKb ?: JSONObject.NULL)
        if (!description.isNullOrBlank()) {
            json.put("description", description)
        }
        if (!inference.isNullOrBlank()) {
            json.put("inference", inference)
        }
        putContext(json, context)
        return json.toString()
    }

    /** 强停 / 重启记录负载（REQ-2）。 */
    fun forceStop(
        kind: String,
        evidence: String,
        inference: String?,
        context: ForensicContext
    ): String {
        val json = JSONObject()
            .put("kind", kind)
            .put("evidence", evidence)
        if (!inference.isNullOrBlank()) {
            json.put("inference", inference)
        }
        putContext(json, context)
        return json.toString()
    }

    private fun putContext(json: JSONObject, context: ForensicContext) {
        json.put("screenOn", context.screenOn ?: JSONObject.NULL)
        json.put("unlocked", context.unlocked ?: JSONObject.NULL)
        json.put("foregroundPackage", context.foregroundPackage ?: JSONObject.NULL)
        json.put("foregroundAppLabel", context.foregroundAppLabel ?: JSONObject.NULL)
        json.put("charging", context.charging ?: JSONObject.NULL)
        json.put("batteryPercent", context.batteryPercent ?: JSONObject.NULL)
        if (context.unavailableFields.isNotEmpty()) {
            json.put("unavailableFields", JSONArray(context.unavailableFields))
        }
    }
}
