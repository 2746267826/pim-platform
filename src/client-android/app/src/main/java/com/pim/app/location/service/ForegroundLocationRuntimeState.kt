package com.pim.app.location.service

import com.pim.app.schedule.ScheduleCacheFreshness

data class ForegroundLocationRuntimeState(
    val isRunning: Boolean = false,
    val currentPolicyMode: String = "Off",
    val currentPolicyReason: String? = null,
    val requestIntervalMillis: Long? = null,
    val nextExpectedLocationAtMillis: Long? = null,
    val lastAcceptedLocationText: String = "无",
    val lastAccuracyText: String = "无",
    val pendingUploadTotal: Int = 0,
    val apiState: String = "等待日程数据",
    val lastDroppedReason: String? = null,
    val scheduleFreshness: ScheduleCacheFreshness = ScheduleCacheFreshness.Missing,
    val scheduleLastSuccessAtMillis: Long? = null,
    val scheduleLastAttemptAtMillis: Long? = null,
    val scheduleLastError: String? = null,
    val highSpeedActive: Boolean = false,
    val highSpeedElapsedSeconds: Long = 0L,
    val highSpeedSinceElapsedRealtimeMillis: Long? = null
)
