package com.pim.core.models

import kotlinx.serialization.Serializable

@Serializable
data class MobileDeviceRegisterRequest(
    val deviceId: String,
    val androidIdHash: String? = null,
    val displayName: String,
    val manufacturer: String,
    val brand: String,
    val model: String,
    val androidVersion: String,
    val sdkInt: Int,
    val appVersion: String,
    val metadataJson: String
)

@Serializable
data class MobileDeviceDto(
    val id: String,
    val deviceId: String,
    val androidIdHash: String? = null,
    val displayName: String,
    val manufacturer: String,
    val brand: String,
    val model: String,
    val androidVersion: String,
    val sdkInt: Int,
    val appVersion: String,
    val metadataJson: String,
    val firstSeenAt: String,
    val lastSeenAt: String,
    val lastHeartbeatAt: String? = null,
    val lastSyncAt: String? = null,
    val isActive: Boolean
)

@Serializable
data class MobileGapRequest(
    val deviceId: String,
    val rangeStartUtc: String,
    val rangeEndUtc: String,
    val capabilityJson: String
)

@Serializable
data class MobileGapResponse(
    val maxBackfillStartUtc: String,
    val windows: List<MobileGapWindowDto> = emptyList()
)

@Serializable
data class MobileGapWindowDto(
    val windowStartUtc: String,
    val windowEndUtc: String,
    val reason: String,
    val sourcePreference: String
)

@Serializable
data class MobileUsageEventsUploadRequest(
    val deviceId: String,
    val clientBatchId: String,
    val sourceWindowStartUtc: String,
    val sourceWindowEndUtc: String,
    val apps: List<MobileAppMetadataDto>,
    val events: List<MobileUsageEventDto>,
    val fallbackSummaries: List<MobileUsageSummaryDto>
)

@Serializable
data class MobileAppMetadataDto(
    val packageName: String,
    val displayName: String,
    val versionName: String? = null,
    val versionCode: Long,
    val isSystemApp: Boolean,
    val categoryName: String? = null,
    val installerPackageName: String? = null,
    val firstInstallTimeUtc: String,
    val lastUpdateTimeUtc: String,
    val rawJson: String
)

@Serializable
data class MobileUsageEventDto(
    val packageName: String,
    val eventType: String,
    val eventTimestampUtc: String,
    val className: String? = null,
    val collectedAtUtc: String,
    val rawJson: String
)

@Serializable
data class MobileUsageSummaryDto(
    val packageName: String,
    val windowStartUtc: String,
    val windowEndUtc: String,
    val totalTimeForegroundMs: Long,
    val lastTimeUsedUtc: String,
    val sourceKind: String,
    val rawJson: String
)

@Serializable
data class MobileIngestResponse(
    val batchId: String = "",
    val acceptedCount: Int = 0,
    val skippedCount: Int = 0,
    val rejectedCount: Int = 0,
    val failedCount: Int = 0
)

@Serializable
data class MobileLocationPointRequest(
    val deviceId: String,
    val recordedAtUtc: String,
    val latitude: Double,
    val longitude: Double,
    val horizontalAccuracyMeters: Double,
    val provider: String,
    val sourceKind: String,
    val altitudeMeters: Double? = null,
    val verticalAccuracyMeters: Double? = null,
    val speedMetersPerSecond: Double? = null,
    val speedAccuracyMetersPerSecond: Double? = null,
    val bearingDegrees: Double? = null,
    val bearingAccuracyDegrees: Double? = null,
    val isAutoSubmitted: Boolean,
    val rawJson: String
)

@Serializable
data class MobileLocationPointDto(
    val id: String,
    val deviceId: String,
    val recordedAtUtc: String,
    val submittedAtUtc: String,
    val latitude: Double,
    val longitude: Double,
    val horizontalAccuracyMeters: Double,
    val provider: String,
    val sourceKind: String,
    val altitudeMeters: Double? = null,
    val verticalAccuracyMeters: Double? = null,
    val speedMetersPerSecond: Double? = null,
    val speedAccuracyMetersPerSecond: Double? = null,
    val bearingDegrees: Double? = null,
    val bearingAccuracyDegrees: Double? = null,
    val isAutoSubmitted: Boolean,
    val quality: String,
    val rawJson: String
)
