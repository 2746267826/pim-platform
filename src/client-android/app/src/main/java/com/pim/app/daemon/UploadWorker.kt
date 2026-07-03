package com.pim.app.daemon

import android.content.Context
import android.provider.Settings
import androidx.work.*
import com.pim.app.data.AppUsageDao
import com.pim.core.models.AppUsageEntry
import com.pim.core.models.UploadBatch
import com.pim.core.network.ApiService
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.qualifiers.ApplicationContext
import timber.log.Timber
import java.util.concurrent.TimeUnit

class UploadWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    @ApplicationContext private val appContext: Context,
    private val dao: AppUsageDao,
    private val api: ApiService
) : CoroutineWorker(context, params) {

    @AssistedFactory
    interface Factory {
        fun create(context: Context, params: WorkerParameters): UploadWorker
    }

    override suspend fun doWork(): Result {
        Timber.d("UploadWorker starting")
        try {
            val unsynced = dao.getUnsynced(500)
            if (unsynced.isEmpty()) {
                Timber.d("No unsynced records")
                return Result.success()
            }

            val deviceId = Settings.Secure.getString(
                appContext.contentResolver, Settings.Secure.ANDROID_ID)

            val batch = UploadBatch(
                deviceId = deviceId,
                entries = unsynced.map {
                    AppUsageEntry(
                        packageName = it.packageName,
                        startTime = it.startTime,
                        endTime = it.endTime,
                        durationMs = it.durationMs,
                        lastTimeUsed = it.lastTimeUsed
                    )
                }
            )

            val response = api.uploadStats(batch)
            if (response.code == 0) {
                val ids = unsynced.map { it.id }
                dao.markSynced(ids)
                Timber.d("Uploaded ${ids.size} records, accepted ${response.data ?: 0}")

                val cutoff = System.currentTimeMillis() - 7 * 24 * 60 * 60 * 1000L
                dao.deleteSyncedOlderThan(cutoff)

                return Result.success()
            } else {
                Timber.w("Upload rejected: ${response.message}")
                return Result.retry()
            }
        } catch (e: Exception) {
            Timber.e(e, "UploadWorker failed")
            return if (runAttemptCount < 3) Result.retry() else Result.failure()
        }
    }
}

fun scheduleUploadWorker(context: Context) {
    val constraints = Constraints.Builder()
        .setRequiredNetworkType(NetworkType.CONNECTED)
        .build()

    val request = PeriodicWorkRequestBuilder<UploadWorker>(15, TimeUnit.MINUTES)
        .setConstraints(constraints)
        .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 15, TimeUnit.SECONDS)
        .build()

    WorkManager.getInstance(context)
        .enqueueUniquePeriodicWork("pim_upload", ExistingPeriodicWorkPolicy.KEEP, request)
}
