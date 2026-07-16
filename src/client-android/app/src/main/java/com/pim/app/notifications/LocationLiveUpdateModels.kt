package com.pim.app.notifications

import com.pim.app.location.policy.LocationPolicyMode

enum class LocationLiveUpdatePhase {
    Collecting,
    SuccessHold,
    Degraded,
    Paused
}

enum class LocationDegradedKind {
    Drop,
    Provider,
    Permission
}

sealed class LocationLiveUpdateEvent {
    data class Snapshot(
        val mode: LocationPolicyMode,
        val nextExpectedLocationText: String,
        val lastAcceptedLocationText: String,
        val lastAccuracyText: String,
        val pendingUploadCount: Int,
        val apiState: String,
        val lastDroppedReason: String?,
        val nextExpectedAtMillis: Long?,
        val lastAcceptedAtMillis: Long?,
        val requestIntervalMillis: Long? = null,
        val permissionOk: Boolean = true,
        val providerEnabled: Boolean = true
    ) : LocationLiveUpdateEvent()

    data class Accepted(
        val lastAcceptedLocationText: String,
        val lastAccuracyText: String,
        val lastAcceptedAtMillis: Long,
        val pendingUploadCount: Int? = null,
        val apiState: String? = null
    ) : LocationLiveUpdateEvent()

    data class Dropped(val reason: String) : LocationLiveUpdateEvent()

    data class PolicyChanged(
        val mode: LocationPolicyMode,
        val nextExpectedLocationText: String,
        val nextExpectedAtMillis: Long?,
        val requestIntervalMillis: Long? = null
    ) : LocationLiveUpdateEvent()

    data class ApiChanged(val apiState: String) : LocationLiveUpdateEvent()
    data class QueueChanged(val pendingUploadCount: Int) : LocationLiveUpdateEvent()
    data class ProviderDisabled(val provider: String? = null) : LocationLiveUpdateEvent()
    data object Paused : LocationLiveUpdateEvent()
    data object Tick : LocationLiveUpdateEvent()
}

data class LocationNotificationUiModel(
    val phase: LocationLiveUpdatePhase,
    val mode: LocationPolicyMode,
    val isOngoing: Boolean,
    val requestLiveUpdate: Boolean,
    val title: String,
    val collapsedText: String,
    val expandedText: String,
    val shortStatus: String,
    val progressPercent: Int?,
    val contentAction: CollectionControlAction
)
