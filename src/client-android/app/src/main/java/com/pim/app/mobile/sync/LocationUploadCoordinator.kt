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
import kotlinx.coroutines.CancellationException

data class LocationUploadBatchResult(
    val syncedIds: List<Long>,
    val failedIds: List<Long>,
    val errorMessage: String?,
    val retryableFailedIds: List<Long> = emptyList()
)

data class LocationUploadStatusUpdates(
    val syncedIds: List<Long>,
    val failedIds: List<Long>,
    val failedReason: String?,
    val shouldRetry: Boolean,
    val perItemErrors: Map<Long, String> = emptyMap(),
    val retryableFailedIds: List<Long> = emptyList()
)

object LocationUploadPlanner {
    fun planStatusUpdates(result: LocationUploadBatchResult): LocationUploadStatusUpdates {
        return LocationUploadStatusUpdates(
            syncedIds = result.syncedIds,
            failedIds = result.failedIds,
            failedReason = result.errorMessage,
            shouldRetry = result.retryableFailedIds.isNotEmpty(),
            retryableFailedIds = result.retryableFailedIds
        )
    }
}

@Singleton
class LocationUploadCoordinator @Inject constructor(
    @ApplicationContext private val context: Context,
    private val database: AppDatabase,
    private val api: ApiService,
    private val compressor: com.pim.app.location.TrajectoryCompressor
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
        val retryableFailed = mutableListOf<Long>()
        val permanentFailed = mutableListOf<Long>()
        val perItemErrors = linkedMapOf<Long, String>()
        var lastError: String? = null
        val deviceId = deviceId()

        for (row in rows) {
            val request = row.toRequest(deviceId)
            if (request == null) {
                permanentFailed += row.id
                perItemErrors[row.id] = "missing-horizontal-accuracy"
                lastError = lastError ?: "missing-horizontal-accuracy"
                continue
            }

            try {
                val response = api.uploadMobileLocation(request)
                if (response.code == 0 && response.data != null) {
                    synced += row.id
                } else {
                    val msg = response.message.ifBlank { "location upload failed" }
                    permanentFailed += row.id
                    perItemErrors[row.id] = msg
                    lastError = lastError ?: msg
                }
            } catch (ex: Exception) {
                if (ex is CancellationException) throw ex
                val outcome = MobileSyncErrorClassifier.classify(ex)
                when (outcome) {
                    MobileSyncOutcome.RETRY -> {
                        retryableFailed += row.id
                        val msg = ex.message ?: ex::class.java.simpleName
                        perItemErrors[row.id] = msg
                        lastError = lastError ?: msg
                    }
                    MobileSyncOutcome.BLOCKED -> {
                        permanentFailed += row.id
                        val msg = ex.message ?: ex::class.java.simpleName
                        perItemErrors[row.id] = msg
                        lastError = lastError ?: msg
                    }
                    else -> {
                        permanentFailed += row.id
                        val msg = ex.message ?: ex::class.java.simpleName
                        perItemErrors[row.id] = msg
                        lastError = lastError ?: msg
                    }
                }
            }
        }

        val allFailed = retryableFailed + permanentFailed
        val updates = LocationUploadPlanner.planStatusUpdates(
            LocationUploadBatchResult(synced, allFailed, lastError, retryableFailed)
        )
        val fullUpdates = updates.copy(perItemErrors = perItemErrors, retryableFailedIds = retryableFailed)
        applyStatusUpdates(fullUpdates)
        return fullUpdates
    }

    private suspend fun pendingRows(limit: Int): List<MobileLocationPointEntity> {
        val pending = dao.getLocationPointsBySyncStatus(MobileSyncStatus.PENDING, limit)
        if (pending.size >= limit) return compressor.compress(pending).take(limit)
        val failed = dao.getLocationPointsBySyncStatus(MobileSyncStatus.FAILED, limit - pending.size)
        val combined = pending + failed
        // PIM-035: Douglas-Peucker/5m+30s clustering reduces upload payload for high-frequency 2.5s挡
        return compressor.compress(combined).take(limit)
    }

    private suspend fun applyStatusUpdates(updates: LocationUploadStatusUpdates) {
        applyLocationStatusUpdates(dao, updates)
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

internal fun LocationUploadStatusUpdates.retryableFirstError(): String? {
    return retryableFailedIds.firstOrNull()?.let { perItemErrors[it] }
}

internal suspend fun applyLocationStatusUpdates(
    dao: MobileDataDao,
    updates: LocationUploadStatusUpdates
) {
    if (updates.syncedIds.isNotEmpty()) {
        dao.deleteLocationPointByIds(updates.syncedIds)
    }
    val retryableSet = updates.retryableFailedIds.toSet()
    val permanentIds = updates.failedIds.filter { it !in retryableSet && it !in updates.syncedIds.toSet() }
    val retryableIds = updates.failedIds.filter { it in retryableSet && it !in updates.syncedIds.toSet() }
    if (permanentIds.isNotEmpty()) {
        permanentIds.forEach { id ->
            dao.updateLocationPointSyncStatus(
                ids = listOf(id),
                syncStatus = MobileSyncStatus.REJECTED,
                lastError = updates.perItemErrors[id] ?: updates.failedReason ?: "permanent-failure"
            )
        }
    }
    if (retryableIds.isNotEmpty()) {
        retryableIds.forEach { id ->
            dao.updateLocationPointSyncStatus(
                ids = listOf(id),
                syncStatus = MobileSyncStatus.PENDING,
                lastError = updates.perItemErrors[id] ?: updates.failedReason ?: "transient-failure"
            )
        }
    }
}
