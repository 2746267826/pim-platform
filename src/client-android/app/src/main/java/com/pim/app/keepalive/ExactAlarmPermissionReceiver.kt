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
 * 精确闹钟权限变化广播（REQ-14 AC-14.3）。
 *
 * 系统在 `SCHEDULE_EXACT_ALARM` 被授予或撤销时发送
 * `ACTION_SCHEDULE_EXACT_ALARM_PERMISSION_STATE_CHANGED`。工单要求：
 * 「授权被撤销后，应用检测到并重新给出引导并点亮红点；重新授权后自动重新登记闹钟」。
 *
 * 注意：**撤销时系统会连带取消已登记的闹钟**（平台依据 §1），因此这里必须重新对账
 * （[KeepAliveCoordinator.reconcile]），不能只更新界面状态。
 */
@AndroidEntryPoint
class ExactAlarmPermissionReceiver : BroadcastReceiver() {

    @Inject
    lateinit var coordinator: KeepAliveCoordinator

    @Inject
    lateinit var logs: StructuredLogRepository

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != ACTION_PERMISSION_STATE_CHANGED) return

        val pendingResult = goAsync()
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
        scope.launch {
            try {
                val outcome = coordinator.reconcile("exact-alarm-permission-changed")
                logs.info("keepalive", "精确闹钟权限变化后对账结果：$outcome")
            } catch (ex: Exception) {
                logs.error("keepalive", "处理权限变化广播失败：${ex.message ?: ex::class.java.simpleName}", ex)
            } finally {
                pendingResult.finish()
            }
        }
    }

    companion object {
        const val ACTION_PERMISSION_STATE_CHANGED =
            "android.app.action.SCHEDULE_EXACT_ALARM_PERMISSION_STATE_CHANGED"
    }
}
