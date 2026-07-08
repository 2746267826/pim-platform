package com.pim.app.mobile.sync

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ListenableWorker
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequest
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkerParameters
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.CancellationException

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
            LocationSyncWorkResultPlanner.fromUpdates(updates)
        } catch (ex: Exception) {
            if (ex is CancellationException) throw ex
            LocationSyncWorkResultPlanner.fromTransientFailure()
        }
    }

    companion object {
        const val WORK_NAME = "pim_location_upload"

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

object LocationSyncWorkResultPlanner {
    fun fromUpdates(updates: LocationUploadStatusUpdates): ListenableWorker.Result {
        return if (updates.shouldRetry) {
            ListenableWorker.Result.retry()
        } else {
            ListenableWorker.Result.success()
        }
    }

    fun fromTransientFailure(): ListenableWorker.Result {
        return ListenableWorker.Result.retry()
    }
}
