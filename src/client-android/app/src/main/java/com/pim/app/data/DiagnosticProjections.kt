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
    val mobileDeviceProfileRowCount: Long
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
