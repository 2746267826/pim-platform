package com.pim.app

import android.app.Application
import androidx.work.Configuration
import com.pim.app.daemon.scheduleUploadWorker
import com.pim.app.di.PimWorkerFactory
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

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
        // 注册周期上传任务。KEEP 策略保证重复调用幂等。
        // WorkManager 在没有网络时会等待，所以即使首次启动无网络也无副作用。
        runCatching { scheduleUploadWorker(this) }
    }
}
