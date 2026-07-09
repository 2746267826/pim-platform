package com.pim.app.di

import android.content.Context
import androidx.work.ListenableWorker
import androidx.work.WorkerFactory
import androidx.work.WorkerParameters
import com.pim.app.daemon.UploadWorker
import com.pim.app.mobile.sync.LocationSyncWorker
import com.pim.app.mobile.sync.MobileSyncWorker
import com.pim.app.sync.EndpointUploadWorker
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PimWorkerFactory @Inject constructor(
    private val uploadWorkerFactory: UploadWorker.Factory,
    private val locationSyncWorkerFactory: LocationSyncWorker.Factory,
    private val mobileSyncWorkerFactory: MobileSyncWorker.Factory
) : WorkerFactory() {

    override fun createWorker(
        appContext: Context,
        workerClassName: String,
        workerParameters: WorkerParameters
    ): ListenableWorker? {
        return when (workerClassName) {
            UploadWorker::class.java.name ->
                uploadWorkerFactory.create(appContext, workerParameters)
            LocationSyncWorker::class.java.name ->
                locationSyncWorkerFactory.create(appContext, workerParameters)
            MobileSyncWorker::class.java.name ->
                mobileSyncWorkerFactory.create(appContext, workerParameters)
            EndpointUploadWorker::class.java.name ->
                EndpointUploadWorker(appContext, workerParameters)
            else -> null
        }
    }
}
