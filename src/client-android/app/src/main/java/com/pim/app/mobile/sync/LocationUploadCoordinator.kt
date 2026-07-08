package com.pim.app.mobile.sync

import android.content.Context
import android.os.Build
import android.provider.Settings
import com.pim.app.data.AppDatabase
import com.pim.app.data.MobileDataDao
import com.pim.app.data.MobileLocationPointEntity
import com.pim.app.data.MobileSyncStatus
import com.pim.core.models.MobileLocationPointRequest
import com.pim.core.network.ApiService
import dagger.hilt.android.qualifiers.ApplicationContext
import java.security.MessageDigest
import java.time.Instant
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

data class LocationUploadBatchResult(
    val syncedIds: List<Long>,
    val failedIds: List<Long>,
    val errorMessage: String?
)

data class LocationUploadStatusUpdates(
    val syncedIds: List<Long>,
    val failedIds: List<Long>,
    val failedReason: String?,
    val shouldRetry: Boolean
)

object LocationUploadPlanner {
    fun planStatusUpdates(result: LocationUploadBatchResult): LocationUploadStatusUpdates {
        return LocationUploadStatusUpdates(
            syncedIds = result.syncedIds,
            failedIds = result.failedIds,
            failedReason = result.errorMessage,
            shouldRetry = result.failedIds.isNotEmpty()
        )
    }
}

@Singleton
class LocationUploadCoordinator @Inject constructor(
    @ApplicationContext private val context: Context,
    private val database: AppDatabase,
    private val api: ApiService
) {
    private val dao: MobileDataDao = database.mobileDataDao()

    suspend fun uploadPending(limit: Int = DEFAULT_LIMIT): LocationUploadStatusUpdates {
        val rows = pendingRows(limit)
        if (rows.isEmpty()) {
            return LocationUploadPlanner.planStatusUpdates(
                LocationUploadBatchResult(emptyList(), emptyList(), null)
            )
        }

        val synced = mutableListOf<Long>()
        val failed = mutableListOf<Long>()
        var lastError: String? = null
        val deviceId = deviceId()

        for (row in rows) {
            val request = row.toRequest(deviceId)
            if (request == null) {
                failed += row.id
                lastError = "missing-horizontal-accuracy"
                continue
            }

            try {
                val response = api.uploadMobileLocation(request)
                if (response.code == 0 && response.data != null) {
                    synced += row.id
                } else {
                    failed += row.id
                    lastError = response.message.ifBlank { "location upload failed" }
                }
            } catch (ex: Exception) {
                failed += row.id
                lastError = ex.message ?: ex::class.java.simpleName
            }
        }

        val updates = LocationUploadPlanner.planStatusUpdates(
            LocationUploadBatchResult(synced, failed, lastError)
        )
        applyStatusUpdates(updates)
        return updates
    }

    private suspend fun pendingRows(limit: Int): List<MobileLocationPointEntity> {
        val pending = dao.getLocationPointsBySyncStatus(MobileSyncStatus.PENDING, limit)
        if (pending.size >= limit) return pending
        val failed = dao.getLocationPointsBySyncStatus(MobileSyncStatus.FAILED, limit - pending.size)
        return pending + failed
    }

    private suspend fun applyStatusUpdates(updates: LocationUploadStatusUpdates) {
        if (updates.syncedIds.isNotEmpty()) {
            dao.updateLocationPointSyncStatus(updates.syncedIds, MobileSyncStatus.SYNCED)
        }
        if (updates.failedIds.isNotEmpty()) {
            dao.updateLocationPointSyncStatus(
                ids = updates.failedIds,
                syncStatus = MobileSyncStatus.FAILED,
                lastError = updates.failedReason
            )
        }
    }

    private fun MobileLocationPointEntity.toRequest(deviceId: String): MobileLocationPointRequest? {
        val accuracy = accuracyMeters ?: return null
        return MobileLocationPointRequest(
            deviceId = deviceId,
            recordedAtUtc = Instant.ofEpochMilli(recordedAtUtc).toString(),
            latitude = latitude,
            longitude = longitude,
            horizontalAccuracyMeters = accuracy.toDouble(),
            provider = provider ?: "unknown",
            sourceKind = source,
            altitudeMeters = altitudeMeters,
            speedMetersPerSecond = speedMetersPerSecond?.toDouble(),
            bearingDegrees = bearingDegrees?.toDouble(),
            isAutoSubmitted = source != "manual",
            rawJson = rawJson
        )
    }

    private fun deviceId(): String {
        val androidId = Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID)
        val seed = androidId ?: Build.FINGERPRINT ?: "android-device"
        return "android-${sha256(seed).take(16)}"
    }

    private fun sha256(value: String): String {
        val bytes = MessageDigest.getInstance("SHA-256").digest(value.toByteArray())
        return bytes.joinToString("") { "%02x".format(Locale.US, it) }
    }

    private companion object {
        const val DEFAULT_LIMIT = 100
    }
}
