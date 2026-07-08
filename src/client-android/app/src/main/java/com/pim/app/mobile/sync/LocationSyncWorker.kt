package com.pim.app.mobile.sync

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequest
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkerParameters
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import java.util.concurrent.TimeUnit

class LocationSyncWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val uploadCoordinator: LocationUploadCoordinator
) : CoroutineWorker(context, params) {
    @AssistedFactory
    interface Factory {
        fun create(context: Context, params: WorkerParameters): LocationSyncWorker
    }

    override suspend fun doWork(): Result {
        return try {
            val updates = uploadCoordinator.uploadPending()
            if (updates.shouldRetry) {
                if (runAttemptCount < MAX_ATTEMPTS_BEFORE_FAILURE) {
                    Result.retry()
                } else {
                    Result.failure()
                }
            } else {
                Result.success()
            }
        } catch (_: Exception) {
            if (runAttemptCount < MAX_ATTEMPTS_BEFORE_FAILURE) {
                Result.retry()
            } else {
                Result.failure()
            }
        }
    }

    companion object {
        const val WORK_NAME = "pim_location_upload"
        private const val MAX_ATTEMPTS_BEFORE_FAILURE = 3

        fun oneTimeRequest(): OneTimeWorkRequest {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            return OneTimeWorkRequestBuilder<LocationSyncWorker>()
                .setConstraints(constraints)
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
                .build()
        }
    }
}
