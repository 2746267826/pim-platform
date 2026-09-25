package com.pim.app

import android.app.Application
import androidx.work.Configuration
import com.pim.app.di.PimWorkerFactory
import com.pim.app.forensics.StartupForensics
import com.pim.app.location.liveupdate.LocationLiveUpdatePublisher
import com.pim.app.recovery.RunningStateRestorer
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltAndroidApp
class PimApp : Application(), Configuration.Provider {

    @Inject
    lateinit var workerFactory: PimWorkerFactory

    @Inject
    lateinit var runningStateRestorer: RunningStateRestorer

    @Inject
    lateinit var liveUpdatePublisher: LocationLiveUpdatePublisher

    @Inject
    lateinit var startupForensics: StartupForensics

    @Inject
    lateinit var keepAliveCoordinator: com.pim.app.keepalive.KeepAliveCoordinator

    @Inject
    lateinit var logs: com.pim.app.mobile.logs.StructuredLogRepository

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        liveUpdatePublisher.cancelStaleNotification()
        liveUpdatePublisher.start(scope)
        // 启动取证（REQ-1 ~ REQ-4）：读退出原因、判强停/重启、写存活心跳。
        // 独立于采集恢复，任一步失败都不阻断采集与同步（AC-1.4 / AC-3.3）。
        scope.launch {
            // AC-28.1：启动取证失败必须有可见出口，不能被 runCatching 静默吞掉。
            try {
                startupForensics.recordOnStartup()
            } catch (ex: kotlinx.coroutines.CancellationException) {
                throw ex
            } catch (ex: Exception) {
                logs.error("forensics", "启动取证失败：${ex.message ?: ex::class.java.simpleName}", ex)
            }
        }
        scope.launch {
            runningStateRestorer.ensureRunningState()
        }
        // REQ-15 / REQ-22：每次打开应用都对账一次保活闹钟——补上被系统清空的登记
        // （AC-21.1）、在权限恢复后重新登记（AC-14.3）、在总开关打开后立即生效（AC-22.3）。
        scope.launch {
            try {
                val outcome = keepAliveCoordinator.reconcile("app-start")
                logs.info("keepalive", "启动时保活对账：$outcome")
            } catch (ex: kotlinx.coroutines.CancellationException) {
                throw ex
            } catch (ex: Exception) {
                // REQ-28：不能静默失败。
                logs.error("keepalive", "启动时保活对账失败：${ex.message ?: ex::class.java.simpleName}", ex)
            }
        }
    }

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()
}
