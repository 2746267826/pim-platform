package com.pim.app.sync

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.pim.app.offline.OnlineOperationGuard

class EndpointUploadWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {
    private val guard = OnlineOperationGuard()

    override suspend fun doWork(): Result {
        return if (guard.canQueueOffline("collection-upload")) {
            Result.success()
        } else {
            Result.failure()
        }
    }

    companion object {
        const val WORK_NAME = "pim_endpoint_upload"
    }
}
