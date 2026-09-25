package com.pim.app.keepalive

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.pim.app.mobile.logs.StructuredLogRepository
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * 叫醒广播接收器（REQ-16）。
 *
 * 系统把精确闹钟派发到这里（`setExactAndAllowWhileIdle`，见 [KeepAliveAlarmScheduler]）。
 * 接收后交给 [KeepAliveCoordinator] 执行完整链路并登记下一次闹钟——
 * 「叫醒一次就不管了」是保活最常见的失败形态，因此这里必须把下一次闹钟续上。
 *
 * 注意：`goAsync()` 用于让广播在异步工作完成前不被回收，这是 `BroadcastReceiver` 里
 * 做少量异步工作的标准做法；工作本身有超时保护（见 [KeepAliveCoordinator]）。
 */
@AndroidEntryPoint
class KeepAliveAlarmReceiver : BroadcastReceiver() {

    @Inject
    lateinit var coordinator: KeepAliveCoordinator

    @Inject
    lateinit var logs: StructuredLogRepository

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != KeepAliveAlarmScheduler.ACTION_KEEPALIVE_ALARM) return

        val pendingResult = goAsync()
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
        scope.launch {
            try {
                coordinator.onAlarmFired()
            } catch (ex: Exception) {
                // REQ-28：叫醒链路的失败必须留下可见出口，不能静默。
                logs.error("keepalive", "处理叫醒广播失败：${ex.message ?: ex::class.java.simpleName}", ex)
            } finally {
                pendingResult.finish()
            }
        }
    }
}
