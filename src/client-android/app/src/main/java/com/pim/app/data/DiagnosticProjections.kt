package com.pim.app.data

data class DiagnosticDatabaseCounts(
    val appUsageRowCount: Long,
    val mobileUsageEventsRowCount: Long,
    val mobileUsageSummariesRowCount: Long,
    val mobileAppMetadataRowCount: Long,
    val mobileLocationPointsRowCount: Long,
    val mobileLocationDroppedDiagnosticsRowCount: Long,
    val mobileLocationPolicyTransitionsRowCount: Long,
    val mobileSyncBatchesRowCount: Long,
    val mobileLogsRowCount: Long,
    val mobileDeviceProfileRowCount: Long,
    val mobileForensicEventsRowCount: Long
)

data class DiagnosticSyncHistoryRow(
    val entityType: String,
    val rowCount: Int,
    val startedAtUtc: Long?,
    val finishedAtUtc: Long?,
    val syncStatus: String,
    val createdAtUtc: Long
)

data class DiagnosticLocationRow(
    val latitude: Double,
    val longitude: Double,
    val altitudeMeters: Double?,
    val accuracyMeters: Float?,
    val speedMetersPerSecond: Float?,
    val bearingDegrees: Float?,
    val provider: String?,
    val recordedAtUtc: Long,
    val source: String,
    val policyMode: String,
    val scheduleLowFrequency: Boolean,
    val motionState: String?,
    val syncStatus: String
)

/** 丢弃原因明细的导出行（REQ-9 / AC-9.1）：时刻 / 原因 / 准确度 / provider / 策略档。 */
data class DiagnosticDroppedDetailRow(
    val recordedAtUtc: Long,
    val reason: String,
    val accuracyMeters: Float?,
    val provider: String?,
    val policyMode: String
)
