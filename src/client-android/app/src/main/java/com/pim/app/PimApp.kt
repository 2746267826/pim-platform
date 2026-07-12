package com.pim.app

import android.app.Application
import androidx.work.Configuration
import com.pim.app.di.PimWorkerFactory
import com.pim.app.mobile.sync.MobileSyncScheduler
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class PimApp : Application(), Configuration.Provider {

    @Inject
    lateinit var workerFactory: PimWorkerFactory

    @Inject
    lateinit var mobileSyncScheduler: MobileSyncScheduler

    override fun onCreate() {
        super.onCreate()
        mobileSyncScheduler.cancelOldWork()
        mobileSyncScheduler.ensurePeriodic()
    }

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()
}
