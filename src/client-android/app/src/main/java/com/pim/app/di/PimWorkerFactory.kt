package com.pim.app.di

import android.content.Context
import androidx.work.ListenableWorker
import androidx.work.WorkerFactory
import androidx.work.WorkerParameters
import com.pim.app.mobile.sync.MobileSyncWorker
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PimWorkerFactory @Inject constructor(
    private val mobileSyncWorkerFactory: MobileSyncWorker.Factory
) : WorkerFactory() {

    override fun createWorker(
        appContext: Context,
        workerClassName: String,
        workerParameters: WorkerParameters
    ): ListenableWorker? {
        return when (workerClassName) {
            MobileSyncWorker::class.java.name ->
                mobileSyncWorkerFactory.create(appContext, workerParameters)
            else -> null
        }
    }
}
