package com.pim.core.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class AppUsageEntry(
    @SerialName("package_name") val packageName: String,
    @SerialName("start_time") val startTime: Long,
    @SerialName("end_time") val endTime: Long,
    @SerialName("duration_ms") val durationMs: Long,
    @SerialName("last_time_used") val lastTimeUsed: Long
)

@Serializable
data class UploadBatch(
    @SerialName("device_id") val deviceId: String,
    @SerialName("entries") val entries: List<AppUsageEntry>
)
