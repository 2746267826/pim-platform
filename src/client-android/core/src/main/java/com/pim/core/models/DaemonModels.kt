package com.pim.core.models

import kotlinx.serialization.Serializable

@Serializable
data class DaemonHeartbeatRequest(
    val deviceId: String,
    val daemonKind: String,
    val version: String,
    val serverUrl: String,
    val lastSuccessfulUploadAt: String? = null,
    val lastAttemptedUploadAt: String? = null,
    val lastError: String? = null,
    val uploadQueueCount: Int? = null,
    val activityWatchState: String = DaemonSourceStates.UNKNOWN,
    val keyStatsState: String = DaemonSourceStates.UNKNOWN,
    val collectionPaused: Boolean = false,
    val statusJson: String = "{}"
)

@Serializable
data class DaemonHeartbeatDto(
    val deviceId: String,
    val daemonKind: String,
    val version: String,
    val serverUrl: String,
    val lastSuccessfulUploadAt: String? = null,
    val lastAttemptedUploadAt: String? = null,
    val lastError: String? = null,
    val uploadQueueCount: Int? = null,
    val activityWatchState: String = DaemonSourceStates.UNKNOWN,
    val keyStatsState: String = DaemonSourceStates.UNKNOWN,
    val collectionPaused: Boolean = false,
    val statusJson: String = "{}",
    val receivedAt: String
)

object DaemonSourceStates {
    const val UNKNOWN = "Unknown"
    const val AVAILABLE = "Available"
    const val UNAVAILABLE = "Unavailable"
}
