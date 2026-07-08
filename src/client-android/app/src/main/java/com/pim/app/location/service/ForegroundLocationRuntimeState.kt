package com.pim.app.location.service

data class ForegroundLocationRuntimeState(
    val isRunning: Boolean = false,
    val currentPolicyMode: String = "Off",
    val nextExpectedLocationAtMillis: Long? = null,
    val lastAcceptedLocationText: String = "无",
    val lastAccuracyText: String = "无",
    val pendingUploadCount: Int = 0,
    val apiState: String = "等待采集",
    val lastDroppedReason: String? = null
)
