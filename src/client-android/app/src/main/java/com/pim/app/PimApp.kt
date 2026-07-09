package com.pim.app

import android.app.Application
import androidx.work.Configuration
import com.pim.app.daemon.scheduleUploadWorker
import com.pim.app.di.PimWorkerFactory
import com.pim.app.mobile.sync.MobileSyncWorker
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject
import timber.log.Timber

@HiltAndroidApp
class PimApp : Application(), Configuration.Provider {

    @Inject
    lateinit var workerFactory: PimWorkerFactory

    override fun onCreate() {
        super.onCreate()
        // Schedule background periodic sync for mobile data upload
        MobileSyncWorker.schedule(this)
        // Register periodic sync on app startup; KEEP makes this idempotent.
        runCatching { scheduleUploadWorker(this) }
            .onFailure { error ->
                Timber.e(error, "Failed to schedule periodic upload worker")
            }
    }

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()
}
