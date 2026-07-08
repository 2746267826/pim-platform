package com.pim.app.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface MobileDataDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUsageEvents(events: List<MobileUsageEventEntity>): List<Long>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUsageSummaries(summaries: List<MobileUsageSummaryEntity>): List<Long>

    @Upsert
    suspend fun upsertAppMetadata(metadata: List<MobileAppMetadataEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLocationPoints(points: List<MobileLocationPointEntity>): List<Long>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLocationPoint(point: MobileLocationPointEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDroppedLocationDiagnostic(diagnostic: MobileLocationDroppedDiagnosticEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPolicyTransition(transition: MobileLocationPolicyTransitionEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSyncBatch(batch: MobileSyncBatchEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLogs(logs: List<MobileLogEntity>): List<Long>

    @Upsert
    suspend fun upsertDeviceProfile(profile: MobileDeviceProfileEntity)

    @Query(
        """
        SELECT * FROM mobile_usage_events
        WHERE sync_status = :syncStatus
        ORDER BY event_time_utc ASC
        LIMIT :limit
        """
    )
    suspend fun getUsageEventsBySyncStatus(
        syncStatus: String = MobileSyncStatus.PENDING,
        limit: Int = 500
    ): List<MobileUsageEventEntity>

    @Query(
        """
        SELECT * FROM mobile_usage_summaries
        WHERE sync_status = :syncStatus
        ORDER BY window_start_utc ASC
        LIMIT :limit
        """
    )
    suspend fun getUsageSummariesBySyncStatus(
        syncStatus: String = MobileSyncStatus.PENDING,
        limit: Int = 500
    ): List<MobileUsageSummaryEntity>

    @Query(
        """
        SELECT * FROM mobile_app_metadata
        WHERE sync_status = :syncStatus
        ORDER BY package_name ASC
        LIMIT :limit
        """
    )
    suspend fun getAppMetadataBySyncStatus(
        syncStatus: String = MobileSyncStatus.PENDING,
        limit: Int = 500
    ): List<MobileAppMetadataEntity>

    @Query(
        """
        SELECT * FROM mobile_location_points
        WHERE sync_status = :syncStatus
        ORDER BY recorded_at_utc ASC
        LIMIT :limit
        """
    )
    suspend fun getLocationPointsBySyncStatus(
        syncStatus: String = MobileSyncStatus.PENDING,
        limit: Int = 500
    ): List<MobileLocationPointEntity>

    @Query(
        """
        SELECT * FROM mobile_sync_batches
        WHERE sync_status = :syncStatus
        ORDER BY created_at_utc ASC
        LIMIT :limit
        """
    )
    suspend fun getSyncBatchesBySyncStatus(
        syncStatus: String = MobileSyncStatus.PENDING,
        limit: Int = 500
    ): List<MobileSyncBatchEntity>

    @Query(
        """
        SELECT * FROM mobile_logs
        WHERE sync_status = :syncStatus
        ORDER BY occurred_at_utc ASC
        LIMIT :limit
        """
    )
    suspend fun getLogsBySyncStatus(
        syncStatus: String = MobileSyncStatus.PENDING,
        limit: Int = 500
    ): List<MobileLogEntity>

    @Query(
        """
        SELECT * FROM mobile_device_profile
        WHERE sync_status = :syncStatus
        LIMIT 1
        """
    )
    suspend fun getDeviceProfileBySyncStatus(
        syncStatus: String = MobileSyncStatus.PENDING
    ): MobileDeviceProfileEntity?

    @Query("SELECT COUNT(*) FROM mobile_usage_events WHERE sync_status != :syncedStatus")
    fun pendingUsageEventCount(syncedStatus: String = MobileSyncStatus.SYNCED): Flow<Int>

    @Query("SELECT COUNT(*) FROM mobile_usage_summaries WHERE sync_status != :syncedStatus")
    fun pendingUsageSummaryCount(syncedStatus: String = MobileSyncStatus.SYNCED): Flow<Int>

    @Query("SELECT COUNT(*) FROM mobile_app_metadata WHERE sync_status != :syncedStatus")
    fun pendingAppMetadataCount(syncedStatus: String = MobileSyncStatus.SYNCED): Flow<Int>

    @Query("SELECT COUNT(*) FROM mobile_location_points WHERE sync_status != :syncedStatus")
    fun pendingLocationPointCount(syncedStatus: String = MobileSyncStatus.SYNCED): Flow<Int>

    @Query("SELECT COUNT(*) FROM mobile_sync_batches WHERE sync_status != :syncedStatus")
    fun pendingSyncBatchCount(syncedStatus: String = MobileSyncStatus.SYNCED): Flow<Int>

    @Query("SELECT COUNT(*) FROM mobile_logs WHERE sync_status != :syncedStatus")
    fun pendingLogCount(syncedStatus: String = MobileSyncStatus.SYNCED): Flow<Int>

    @Query("SELECT COUNT(*) FROM mobile_device_profile WHERE sync_status != :syncedStatus")
    fun pendingDeviceProfileCount(syncedStatus: String = MobileSyncStatus.SYNCED): Flow<Int>

    @Query("SELECT * FROM mobile_logs ORDER BY occurred_at_utc DESC LIMIT :limit")
    fun recentLogs(limit: Int = 6): Flow<List<MobileLogEntity>>

    @Query("SELECT * FROM mobile_location_dropped_diagnostics ORDER BY recorded_at_utc DESC LIMIT :limit")
    fun recentDroppedLocationDiagnostics(limit: Int = 20): Flow<List<MobileLocationDroppedDiagnosticEntity>>

    @Query("SELECT * FROM mobile_location_policy_transitions ORDER BY occurred_at_utc DESC LIMIT :limit")
    fun recentPolicyTransitions(limit: Int = 20): Flow<List<MobileLocationPolicyTransitionEntity>>

    @Query(
        """
        UPDATE mobile_usage_events
        SET sync_status = :syncStatus,
            last_error = :lastError,
            updated_at_utc = :updatedAtUtc
        WHERE id IN (:ids)
        """
    )
    suspend fun updateUsageEventSyncStatus(
        ids: List<Long>,
        syncStatus: String,
        lastError: String? = null,
        updatedAtUtc: Long = System.currentTimeMillis()
    )

    @Query(
        """
        UPDATE mobile_usage_summaries
        SET sync_status = :syncStatus,
            last_error = :lastError,
            updated_at_utc = :updatedAtUtc
        WHERE id IN (:ids)
        """
    )
    suspend fun updateUsageSummarySyncStatus(
        ids: List<Long>,
        syncStatus: String,
        lastError: String? = null,
        updatedAtUtc: Long = System.currentTimeMillis()
    )

    @Query(
        """
        UPDATE mobile_app_metadata
        SET sync_status = :syncStatus,
            last_error = :lastError,
            updated_at_utc = :updatedAtUtc
        WHERE package_name IN (:packageNames)
        """
    )
    suspend fun updateAppMetadataSyncStatus(
        packageNames: List<String>,
        syncStatus: String,
        lastError: String? = null,
        updatedAtUtc: Long = System.currentTimeMillis()
    )

    @Query(
        """
        UPDATE mobile_location_points
        SET sync_status = :syncStatus,
            last_error = :lastError,
            updated_at_utc = :updatedAtUtc
        WHERE id IN (:ids)
        """
    )
    suspend fun updateLocationPointSyncStatus(
        ids: List<Long>,
        syncStatus: String,
        lastError: String? = null,
        updatedAtUtc: Long = System.currentTimeMillis()
    )

    @Query(
        """
        UPDATE mobile_sync_batches
        SET sync_status = :syncStatus,
            last_error = :lastError,
            updated_at_utc = :updatedAtUtc
        WHERE batch_id IN (:batchIds)
        """
    )
    suspend fun updateSyncBatchSyncStatus(
        batchIds: List<String>,
        syncStatus: String,
        lastError: String? = null,
        updatedAtUtc: Long = System.currentTimeMillis()
    )

    @Query(
        """
        UPDATE mobile_logs
        SET sync_status = :syncStatus,
            last_error = :lastError,
            updated_at_utc = :updatedAtUtc
        WHERE id IN (:ids)
        """
    )
    suspend fun updateLogSyncStatus(
        ids: List<Long>,
        syncStatus: String,
        lastError: String? = null,
        updatedAtUtc: Long = System.currentTimeMillis()
    )

    @Query(
        """
        UPDATE mobile_device_profile
        SET sync_status = :syncStatus,
            last_error = :lastError,
            updated_at_utc = :updatedAtUtc
        WHERE profile_id = :profileId
        """
    )
    suspend fun updateDeviceProfileSyncStatus(
        profileId: String = "default",
        syncStatus: String,
        lastError: String? = null,
        updatedAtUtc: Long = System.currentTimeMillis()
    )
}
