package com.pim.app.mobile.sync

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import kotlinx.coroutines.CancellationException

/**
 * Background periodic worker that runs MobileSyncCoordinator.syncOnOpen()
 * to upload queued usage and location data even when the app is not actively opened.
 *
 * Replaces the old UploadWorker which used a deprecated stats endpoint.
 */
class MobileSyncWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val syncCoordinator: MobileSyncCoordinator
) : CoroutineWorker(context, params) {

    @AssistedFactory
    interface Factory {
        fun create(context: Context, params: WorkerParameters): MobileSyncWorker
    }

    override suspend fun doWork(): Result {
        return try {
            val state = syncCoordinator.syncOnOpen()
            mapOutcomeToWorkerResult(state.outcome)
        } catch (ex: CancellationException) {
            throw ex
        } catch (ex: Exception) {
            val outcome = MobileSyncErrorClassifier.classify(ex)
            mapOutcomeToWorkerResult(outcome)
        }
    }
}
