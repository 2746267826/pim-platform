package com.pim.app.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

object MobileSyncStatus {
    const val PENDING = "pending"
    const val SYNCING = "syncing"
    const val SYNCED = "synced"
    const val FAILED = "failed"
}

@Entity(
    tableName = "mobile_usage_events",
    indices = [
        Index(value = ["package_name", "event_time_utc"]),
        Index(value = ["sync_status"])
    ]
)
data class MobileUsageEventEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "package_name") val packageName: String,
    @ColumnInfo(name = "class_name") val className: String? = null,
    @ColumnInfo(name = "event_type") val eventType: Int,
    @ColumnInfo(name = "event_name") val eventName: String,
    @ColumnInfo(name = "event_time_utc") val eventTimeUtc: Long,
    @ColumnInfo(name = "source") val source: String,
    @ColumnInfo(name = "source_window_start_utc") val sourceWindowStartUtc: Long,
    @ColumnInfo(name = "source_window_end_utc") val sourceWindowEndUtc: Long,
    @ColumnInfo(name = "collected_at_utc") val collectedAtUtc: Long,
    @ColumnInfo(name = "raw_json") val rawJson: String,
    @ColumnInfo(name = "sync_status") val syncStatus: String = MobileSyncStatus.PENDING,
    @ColumnInfo(name = "last_error") val lastError: String? = null,
    @ColumnInfo(name = "created_at_utc") val createdAtUtc: Long = System.currentTimeMillis(),
    @ColumnInfo(name = "updated_at_utc") val updatedAtUtc: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "mobile_usage_summaries",
    indices = [
        Index(value = ["package_name", "window_start_utc", "window_end_utc"]),
        Index(value = ["sync_status"])
    ]
)
data class MobileUsageSummaryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "package_name") val packageName: String,
    @ColumnInfo(name = "window_start_utc") val windowStartUtc: Long,
    @ColumnInfo(name = "window_end_utc") val windowEndUtc: Long,
    @ColumnInfo(name = "total_time_foreground_ms") val totalTimeForegroundMs: Long,
    @ColumnInfo(name = "last_time_used_utc") val lastTimeUsedUtc: Long,
    @ColumnInfo(name = "first_time_stamp_utc") val firstTimeStampUtc: Long,
    @ColumnInfo(name = "last_time_stamp_utc") val lastTimeStampUtc: Long,
    @ColumnInfo(name = "source") val source: String,
    @ColumnInfo(name = "source_window_start_utc") val sourceWindowStartUtc: Long,
    @ColumnInfo(name = "source_window_end_utc") val sourceWindowEndUtc: Long,
    @ColumnInfo(name = "collected_at_utc") val collectedAtUtc: Long,
    @ColumnInfo(name = "raw_json") val rawJson: String,
    @ColumnInfo(name = "sync_status") val syncStatus: String = MobileSyncStatus.PENDING,
    @ColumnInfo(name = "last_error") val lastError: String? = null,
    @ColumnInfo(name = "created_at_utc") val createdAtUtc: Long = System.currentTimeMillis(),
    @ColumnInfo(name = "updated_at_utc") val updatedAtUtc: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "mobile_app_metadata",
    indices = [Index(value = ["sync_status"])]
)
data class MobileAppMetadataEntity(
    @PrimaryKey
    @ColumnInfo(name = "package_name")
    val packageName: String,
    @ColumnInfo(name = "label") val label: String,
    @ColumnInfo(name = "version_name") val versionName: String? = null,
    @ColumnInfo(name = "version_code") val versionCode: Long,
    @ColumnInfo(name = "first_install_time_utc") val firstInstallTimeUtc: Long,
    @ColumnInfo(name = "last_update_time_utc") val lastUpdateTimeUtc: Long,
    @ColumnInfo(name = "is_system_app") val isSystemApp: Boolean,
    @ColumnInfo(name = "category") val category: Int? = null,
    @ColumnInfo(name = "installer_package_name") val installerPackageName: String? = null,
    @ColumnInfo(name = "collected_at_utc") val collectedAtUtc: Long,
    @ColumnInfo(name = "raw_json") val rawJson: String,
    @ColumnInfo(name = "sync_status") val syncStatus: String = MobileSyncStatus.PENDING,
    @ColumnInfo(name = "last_error") val lastError: String? = null,
    @ColumnInfo(name = "created_at_utc") val createdAtUtc: Long = System.currentTimeMillis(),
    @ColumnInfo(name = "updated_at_utc") val updatedAtUtc: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "mobile_location_points",
    indices = [
        Index(value = ["recorded_at_utc"]),
        Index(value = ["sync_status"])
    ]
)
data class MobileLocationPointEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "latitude") val latitude: Double,
    @ColumnInfo(name = "longitude") val longitude: Double,
    @ColumnInfo(name = "altitude_meters") val altitudeMeters: Double? = null,
    @ColumnInfo(name = "accuracy_meters") val accuracyMeters: Float? = null,
    @ColumnInfo(name = "speed_meters_per_second") val speedMetersPerSecond: Float? = null,
    @ColumnInfo(name = "bearing_degrees") val bearingDegrees: Float? = null,
    @ColumnInfo(name = "provider") val provider: String? = null,
    @ColumnInfo(name = "recorded_at_utc") val recordedAtUtc: Long,
    @ColumnInfo(name = "source") val source: String,
    @ColumnInfo(name = "collected_at_utc") val collectedAtUtc: Long,
    @ColumnInfo(name = "raw_json") val rawJson: String,
    @ColumnInfo(name = "sync_status") val syncStatus: String = MobileSyncStatus.PENDING,
    @ColumnInfo(name = "last_error") val lastError: String? = null,
    @ColumnInfo(name = "created_at_utc") val createdAtUtc: Long = System.currentTimeMillis(),
    @ColumnInfo(name = "updated_at_utc") val updatedAtUtc: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "mobile_sync_batches",
    indices = [
        Index(value = ["batch_id"], unique = true),
        Index(value = ["sync_status"])
    ]
)
data class MobileSyncBatchEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "batch_id") val batchId: String,
    @ColumnInfo(name = "entity_type") val entityType: String,
    @ColumnInfo(name = "row_count") val rowCount: Int,
    @ColumnInfo(name = "started_at_utc") val startedAtUtc: Long? = null,
    @ColumnInfo(name = "finished_at_utc") val finishedAtUtc: Long? = null,
    @ColumnInfo(name = "request_json") val requestJson: String? = null,
    @ColumnInfo(name = "response_json") val responseJson: String? = null,
    @ColumnInfo(name = "sync_status") val syncStatus: String = MobileSyncStatus.PENDING,
    @ColumnInfo(name = "last_error") val lastError: String? = null,
    @ColumnInfo(name = "created_at_utc") val createdAtUtc: Long = System.currentTimeMillis(),
    @ColumnInfo(name = "updated_at_utc") val updatedAtUtc: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "mobile_logs",
    indices = [
        Index(value = ["occurred_at_utc"]),
        Index(value = ["sync_status"])
    ]
)
data class MobileLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "level") val level: String,
    @ColumnInfo(name = "tag") val tag: String? = null,
    @ColumnInfo(name = "message") val message: String,
    @ColumnInfo(name = "throwable") val throwable: String? = null,
    @ColumnInfo(name = "occurred_at_utc") val occurredAtUtc: Long,
    @ColumnInfo(name = "source") val source: String,
    @ColumnInfo(name = "collected_at_utc") val collectedAtUtc: Long,
    @ColumnInfo(name = "raw_json") val rawJson: String,
    @ColumnInfo(name = "sync_status") val syncStatus: String = MobileSyncStatus.PENDING,
    @ColumnInfo(name = "last_error") val lastError: String? = null,
    @ColumnInfo(name = "created_at_utc") val createdAtUtc: Long = System.currentTimeMillis(),
    @ColumnInfo(name = "updated_at_utc") val updatedAtUtc: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "mobile_device_profile",
    indices = [Index(value = ["sync_status"])]
)
data class MobileDeviceProfileEntity(
    @PrimaryKey
    @ColumnInfo(name = "profile_id")
    val profileId: String = "default",
    @ColumnInfo(name = "device_id") val deviceId: String,
    @ColumnInfo(name = "manufacturer") val manufacturer: String,
    @ColumnInfo(name = "brand") val brand: String,
    @ColumnInfo(name = "model") val model: String,
    @ColumnInfo(name = "hardware") val hardware: String,
    @ColumnInfo(name = "android_version") val androidVersion: String,
    @ColumnInfo(name = "sdk_int") val sdkInt: Int,
    @ColumnInfo(name = "app_version_name") val appVersionName: String? = null,
    @ColumnInfo(name = "app_version_code") val appVersionCode: Long? = null,
    @ColumnInfo(name = "collected_at_utc") val collectedAtUtc: Long,
    @ColumnInfo(name = "raw_json") val rawJson: String,
    @ColumnInfo(name = "sync_status") val syncStatus: String = MobileSyncStatus.PENDING,
    @ColumnInfo(name = "last_error") val lastError: String? = null,
    @ColumnInfo(name = "created_at_utc") val createdAtUtc: Long = System.currentTimeMillis(),
    @ColumnInfo(name = "updated_at_utc") val updatedAtUtc: Long = System.currentTimeMillis()
)
