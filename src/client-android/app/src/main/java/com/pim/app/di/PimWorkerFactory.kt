package com.pim.app.di

import android.content.Context
import androidx.work.ListenableWorker
import androidx.work.WorkerFactory
import androidx.work.WorkerParameters
import com.pim.app.daemon.UploadWorker
import com.pim.app.sync.EndpointUploadWorker
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PimWorkerFactory @Inject constructor(
    private val uploadWorkerFactory: UploadWorker.Factory
) : WorkerFactory() {

    override fun createWorker(
        appContext: Context,
        workerClassName: String,
        workerParameters: WorkerParameters
    ): ListenableWorker? {
        return when (workerClassName) {
            UploadWorker::class.java.name ->
                uploadWorkerFactory.create(appContext, workerParameters)
            EndpointUploadWorker::class.java.name ->
                EndpointUploadWorker(appContext, workerParameters)
            else -> null
        }
    }
}
