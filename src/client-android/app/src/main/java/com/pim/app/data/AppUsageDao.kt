package com.pim.app.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface AppUsageDao {
    @Query("SELECT * FROM app_usage WHERE synced = 0 ORDER BY start_time ASC LIMIT :limit")
    suspend fun getUnsynced(limit: Int = 500): List<AppUsageEntity>

    @Query("SELECT COUNT(*) FROM app_usage WHERE synced = 0")
    fun unsyncedCount(): Flow<Int>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(entries: List<AppUsageEntity>)

    @Query("UPDATE app_usage SET synced = 1 WHERE id IN (:ids)")
    suspend fun markSynced(ids: List<Long>)

    @Query("DELETE FROM app_usage WHERE synced = 1 AND end_time < :before")
    suspend fun deleteSyncedOlderThan(before: Long)
}
