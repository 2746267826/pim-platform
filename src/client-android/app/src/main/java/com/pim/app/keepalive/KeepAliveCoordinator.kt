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

        // REQ-21「连续叫醒调用失败」点亮红点（AC-21.1 四类之一）。
        //
        // 这里**刻意不设「连续 N 次」的 N**：工单 §9.1 未给出该数值，决策索引
        // （R1-Q7 / D13）的原话是「拉起失败：记台账 + 状态页红点 + 下轮重试（不降频）」，
        // 并没有说要连续几次。工单 §8.6 明确禁止按自拟数值实现。
        // 因此口径取「当前连续失败计数非零即点亮」：一次成功就会清零并熄灭，
        // 「连续」的语义由计数器本身承担，不需要一个凭空规定的门槛。
        // 若需求方希望「连续 N 次才提示」，那是一个新的待确认参数（§9.2 流程），不是本次可自定的。
        if (record.outcome == AlarmOutcomes.PULL_FAILED) {
            val failures = settingsStore.read().consecutiveWakeFailures
            health.raise(
                KeepAliveHealthReasons.WAKE_CALL_FAILED,
                "最近一次未能拉起采集服务（已连续失败 $failures 次），将在下个周期重试。"
            )
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
     *
     * **检测手段（重要）**：不用「PendingIntent 是否存在」判断闹钟是否还在——
     * `AlarmManager.cancel()` 之后 PendingIntent 依然存在（已实测），那样会永远报告
     * 「闹钟在」，恰好掩盖 AC-21.1 要检测的情况。这里改用**截止时刻**判断：
     * 已登记的下一次触发时刻如果已经过去（还留着余量），说明那一枪打空了，
     * 系统里的闹钟确实没了（被强停清空 / 被系统回收 / 权限撤销连带取消）。
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

        val pending = settings.pendingScheduledAtUtcMillis
        if (pending == null) {
            // 从未登记过（首次开启保活）：直接登记，不算异常。
            return scheduleNext(trigger)
        }

        val overdueBy = nowUtcMillis() - pending
        if (overdueBy > OVERDUE_GRACE_MILLIS) {
            // 预定时刻早该到了却没触发：闹钟不在系统里了。
            health.raise(
                KeepAliveHealthReasons.ALARM_CLEARED,
                "预定叫醒时刻已过去 ${overdueBy / 60_000L} 分钟仍未触发，系统里的保活闹钟已不存在，现已重新登记。"
            )
            return scheduleNext(trigger)
        }

        // 闹钟尚未到期且登记信息在：确认它确实存在，此时才允许熄灭「闹钟被清空」红点（AC-21.2）。
        health.clear(KeepAliveHealthReasons.ALARM_CLEARED)
        return KeepAliveScheduleOutcome.AlreadyRegistered(pending)
    }

    private companion object {
        /**
         * 判定「闹钟打空了」的宽限余量。
         *
         * 取 15 分钟：与 AC-17.1 的「压制」判定线一致，不另立一个自拟阈值；
         * 小于该值可能只是系统正常延迟（Doze 下允许延迟），不能算闹钟消失。
         */
        const val OVERDUE_GRACE_MILLIS = 15 * 60_000L
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
