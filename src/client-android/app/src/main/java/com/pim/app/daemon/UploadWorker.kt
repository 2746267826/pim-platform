package com.pim.app.daemon

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.pim.app.mobile.sync.MobileSyncCoordinator
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
    private val mobileSyncCoordinator: MobileSyncCoordinator
) : CoroutineWorker(context, params) {

    @AssistedFactory
    interface Factory {
        fun create(context: Context, params: WorkerParameters): UploadWorker
    }

    override suspend fun doWork(): Result {
        Timber.d("UploadWorker starting periodic mobile sync")
        return try {
            val state = mobileSyncCoordinator.syncOnOpen()
            Timber.d("UploadWorker finished: phase=${state.phase} failed=${state.failedCount}")
            if (state.failedCount > 0 && runAttemptCount < 3) Result.retry() else Result.success()
        } catch (e: Exception) {
            Timber.e(e, "UploadWorker failed")
            if (runAttemptCount < 3) Result.retry() else Result.failure()
        }
    }

    companion object {
        const val WORK_NAME = "pim_upload"
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
        .enqueueUniquePeriodicWork(
            UploadWorker.WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            request
        )
}

fun cancelUploadWorker(context: Context) {
    WorkManager.getInstance(context).cancelUniqueWork(UploadWorker.WORK_NAME)
}
