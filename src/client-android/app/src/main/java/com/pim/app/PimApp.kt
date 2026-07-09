package com.pim.app

import android.app.Application
import androidx.work.Configuration
import com.pim.app.daemon.scheduleUploadWorker
import com.pim.app.di.PimWorkerFactory
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject
import timber.log.Timber

@HiltAndroidApp
class PimApp : Application(), Configuration.Provider {

    @Inject
    lateinit var workerFactory: PimWorkerFactory

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()

    override fun onCreate() {
        super.onCreate()
        // Register periodic sync on app startup; KEEP makes this idempotent.
        runCatching { scheduleUploadWorker(this) }
            .onFailure { error ->
                Timber.e(error, "Failed to schedule periodic upload worker")
            }
    }
}
