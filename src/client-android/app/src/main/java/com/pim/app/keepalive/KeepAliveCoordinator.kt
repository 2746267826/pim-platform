package com.pim.app.keepalive

import com.pim.app.mobile.logs.StructuredLogRepository
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CancellationException

/**
 * 保活编排：把「登记闹钟 → 被叫醒 → 执行 → 再登记」串成一个闭环（REQ-15 / REQ-16 / REQ-17）。
 *
 * 工单要求的关键点是**续登记**：只叫醒一次、不登记下一次，等于没有保活。
 * 因此 [onAlarmFired] 在走完执行链后一定会尝试登记下一次（除非总开关关闭或权限缺失）。
 *
 * 与权限的关系（REQ-14）：
 * - 未授权时不登记，并记一条健康事件点亮红点（AC-14.3 / AC-14.4 / AC-21.1），本次记为
 *   「未执行」而不是「被压制」——权限问题不是延迟问题。
 * - 重新授权后由 [reconcile] 自动恢复登记。
 */
@Singleton
class KeepAliveCoordinator internal constructor(
    private val settingsStore: KeepAliveSettingsAccessor,
    private val scheduler: KeepAliveSchedulePort,
    private val executionChain: WakeExecutionChain,
    private val ledger: KeepAliveLedger,
    private val health: KeepAliveHealthMonitor,
    private val notifications: KeepAliveNotificationPort,
    private val logs: StructuredLogRepository,
    private val nowUtcMillis: () -> Long
) {
    @Inject
    constructor(
        settingsStore: KeepAliveSettingsAccessor,
        scheduler: KeepAliveSchedulePort,
        executionChain: WakeExecutionChain,
        ledger: KeepAliveLedger,
        health: KeepAliveHealthMonitor,
        notifications: KeepAliveNotificationPort,
        logs: StructuredLogRepository
    ) : this(
        settingsStore, scheduler, executionChain, ledger, health, notifications, logs,
        System::currentTimeMillis
    )
    /** 登记（或续登记）下一次叫醒。返回实际登记结果。 */
    suspend fun scheduleNext(trigger: String): KeepAliveScheduleOutcome {
        val settings = settingsStore.read()

        if (!settings.enabled) {
            // AC-22.2 / AC-22.3：关闭期间不得暗中登记。
            scheduler.cancel()
            return KeepAliveScheduleOutcome.Disabled
        }

        val effective = AlarmSuppressionPolicy.resolveEffectiveMinutes(
            configuredMinutes = settings.configuredIntervalMinutes,
            effectiveMinutes = settings.effectiveIntervalMinutes,
            recentRecords = ledger.recentFulfillments()
        )

        // 生效值变化（降频/复位）要落盘，AC-17.2/AC-17.3 的状态页提示与通知都读它。
        if (effective != settings.effectiveIntervalMinutes) {
            settingsStore.write(settings.copy(effectiveIntervalMinutes = effective))
            logs.info("keepalive", "保活间隔调整为 $effective 分钟（触发于 $trigger）")
        }

        return when (val result = scheduler.scheduleNext(effective, enabled = true)) {
            is KeepAliveSchedulePort.Result.Scheduled -> {
                settingsStore.write(
                    settingsStore.read().copy(pendingScheduledAtUtcMillis = result.triggerAtUtcMillis)
                )
                ledger.recordAlarmRegistered(effective, result.triggerAtUtcMillis, trigger)
                // 登记成功说明权限可用，因此清掉「权限被撤销」。
                // 但**不清**「闹钟被清空」：那是「观察到闹钟曾消失」这一事实，
                // 由 [reconcile] 在下次确认闹钟确实存在时清除（AC-21.1 可被观察到 / AC-21.2 才熄灭）。
                health.clear(KeepAliveHealthReasons.PERMISSION_REVOKED)
                KeepAliveScheduleOutcome.Scheduled(result.triggerAtUtcMillis, effective)
            }

            KeepAliveSchedulePort.Result.PermissionMissing -> {
                // AC-14.3 / AC-21.1：权限被撤销 → 红点 + 通知栏提示。
                health.raise(
                    KeepAliveHealthReasons.PERMISSION_REVOKED,
                    "未获得「闹钟和提醒」权限，保活闹钟无法登记。"
                )
                KeepAliveScheduleOutcome.PermissionMissing
            }

            is KeepAliveSchedulePort.Result.Failed -> {
                health.raise(KeepAliveHealthReasons.ALARM_CLEARED, result.message)
                KeepAliveScheduleOutcome.Failed(result.message)
            }

            KeepAliveSchedulePort.Result.Disabled -> KeepAliveScheduleOutcome.Disabled
        }
    }

    /**
     * 闹钟触发后的完整处理（REQ-16 + 续登记）。
     *
     * 顺序：执行链 → 通知更新 → 续登记。即使执行链失败也要续登记，
     * 否则一次失败会让保活永久停摆。
     */
    suspend fun onAlarmFired(): AlarmFulfillmentRecord {
        val settings = settingsStore.read()
        val scheduledAt = settings.pendingScheduledAtUtcMillis ?: nowUtcMillis()

        val record = executionChain.execute(scheduledAt)

        // REQ-19：常驻通知更新为最近一次叫醒时间（内容滚动更新，仍是一条）。
        notifications.updateLastWake(record.actualAtUtcMillis ?: record.scheduledAtUtcMillis)

        // AC-17.5 / REQ-21：连续拉起失败要点亮红点（不降频，只提示并下轮重试）。
        if (record.outcome == AlarmOutcomes.PULL_FAILED) {
            val failures = settingsStore.read().consecutiveWakeFailures
            if (failures >= KEEPALIVE_FAILURE_ALERT_THRESHOLD) {
                health.raise(
                    KeepAliveHealthReasons.WAKE_CALL_FAILED,
                    "连续 $failures 次未能拉起采集服务，将在下个周期重试。"
                )
            }
        } else if (record.outcome == AlarmOutcomes.EXECUTED) {
            health.clear(KeepAliveHealthReasons.WAKE_CALL_FAILED)
        }

        // 续登记：保活的核心，不能被上面的失败跳过。
        val outcome = scheduleNext(trigger = "alarm-fired")
        if (outcome is KeepAliveScheduleOutcome.Scheduled && notifications.isEnabled()) {
            notifications.ensureResident()
        }

        return record
    }

    /**
     * 与系统真实状态对账（启动时 / 权限变化 / 返回前台时调用）。
     *
     * 覆盖三种需要修复的情形：
     * - 权限撤销导致系统连带取消闹钟（AC-14.3）；
     * - 被强行停止或系统清理导致闹钟消失（AC-21.1 的「闹钟被清空」）；
     * - 从未登记过（首次开启保活）。
     */
    suspend fun reconcile(trigger: String): KeepAliveScheduleOutcome {
        val settings = settingsStore.read()
        if (!settings.enabled) {
            scheduler.cancel()
            return KeepAliveScheduleOutcome.Disabled
        }

        if (!scheduler.hasExactAlarmPermission()) {
            health.raise(
                KeepAliveHealthReasons.PERMISSION_REVOKED,
                "未获得「闹钟和提醒」权限，保活闹钟无法登记。"
            )
            return KeepAliveScheduleOutcome.PermissionMissing
        }

        if (!scheduler.isAlarmRegistered()) {
            // 闹钟不在系统里：可能是被强停清空、被系统回收、或从未登记。
            if (settings.pendingScheduledAtUtcMillis != null) {
                health.raise(
                    KeepAliveHealthReasons.ALARM_CLEARED,
                    "系统里已找不到保活闹钟，已重新登记。"
                )
            }
            return scheduleNext(trigger)
        }

        health.clear(KeepAliveHealthReasons.ALARM_CLEARED)
        return KeepAliveScheduleOutcome.AlreadyRegistered(settings.pendingScheduledAtUtcMillis)
    }

    private companion object {
        /** 连续拉起失败达到该次数即点亮红点（R1-Q7/D13：记台账 + 红点 + 下轮重试）。 */
        const val KEEPALIVE_FAILURE_ALERT_THRESHOLD = 2
    }
}

/** 登记结果（供界面与状态展示，AC-22.2「台账与界面均显示已关闭」）。 */
sealed interface KeepAliveScheduleOutcome {
    data class Scheduled(val triggerAtUtcMillis: Long, val intervalMinutes: Int) : KeepAliveScheduleOutcome
    data class AlreadyRegistered(val triggerAtUtcMillis: Long?) : KeepAliveScheduleOutcome
    data object PermissionMissing : KeepAliveScheduleOutcome
    data object Disabled : KeepAliveScheduleOutcome
    data class Failed(val message: String) : KeepAliveScheduleOutcome
}
