package com.pim.app.recovery

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.pim.app.keepalive.KeepAliveCoordinator
import com.pim.app.mobile.logs.StructuredLogRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class StartupRecoveryReceiver : BroadcastReceiver() {

    @Inject
    lateinit var runningStateRestorer: RunningStateRestorer

    @Inject
    lateinit var keepAliveCoordinator: KeepAliveCoordinator

    @Inject
    lateinit var logs: StructuredLogRepository

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onReceive(context: Context, intent: Intent) {
        if (!isStartupRecoveryAction(intent.action)) return
        val pendingResult = goAsync()
        scope.launch {
            try {
                // AC-15.4：设备重启或应用更新后闹钟被系统清空，必须重建。
                // 顺序：先恢复采集运行态（既有行为），再重建闹钟；两步互不阻断。
                dispatchStartupRecovery(
                    action = intent.action,
                    recover = { runningStateRestorer.ensureRunningState() },
                    rebuildKeepAliveAlarm = {
                        val outcome = keepAliveCoordinator.reconcile("boot-or-update")
                        logs.info("keepalive", "开机/更新后重建保活闹钟：$outcome")
                    },
                    onKeepAliveFailure = { ex ->
                        // REQ-28：失败必须留下可见出口，不能静默。
                        logs.error("keepalive", "开机/更新后重建保活闹钟失败：${ex.message ?: ""}", ex)
                    }
                )
            } finally {
                pendingResult.finish()
            }
        }
    }

    companion object {
        internal fun isStartupRecoveryAction(action: String?): Boolean {
            return action == Intent.ACTION_BOOT_COMPLETED || action == Intent.ACTION_MY_PACKAGE_REPLACED
        }

        internal suspend fun dispatchStartupRecovery(
            action: String?,
            recover: suspend () -> Unit
        ): Boolean {
            return dispatchStartupRecovery(action, recover, rebuildKeepAliveAlarm = {}, onKeepAliveFailure = {})
        }

        /**
         * 开机 / 应用更新后的恢复编排（AC-15.4）。
         *
         * 闹钟重建**独立于**采集恢复，两个方向都成立：
         * - 采集恢复抛错时，**仍然**尝试重建闹钟（否则一次采集恢复失败会让保活在设备重启后永久失效）；
         * - 闹钟重建失败时不抛出，只经 [onKeepAliveFailure] 上报（REQ-28 可见出口），
         *   不影响采集恢复的结果。
         *
         * 采集恢复自身的异常按既有语义向上传播，行为与改动前一致。
         */
        internal suspend fun dispatchStartupRecovery(
            action: String?,
            recover: suspend () -> Unit,
            rebuildKeepAliveAlarm: suspend () -> Unit,
            onKeepAliveFailure: suspend (Exception) -> Unit
        ): Boolean {
            if (!isStartupRecoveryAction(action)) return false

            var recoveryFailure: Exception? = null
            try {
                recover()
            } catch (ex: kotlinx.coroutines.CancellationException) {
                throw ex
            } catch (ex: Exception) {
                recoveryFailure = ex
            }

            try {
                rebuildKeepAliveAlarm()
            } catch (ex: kotlinx.coroutines.CancellationException) {
                throw ex
            } catch (ex: Exception) {
                onKeepAliveFailure(ex)
            }

            // 采集恢复的异常仍然向上传播（与改动前一致），但已确保闹钟重建被尝试过。
            recoveryFailure?.let { throw it }
            return true
        }
    }
}
