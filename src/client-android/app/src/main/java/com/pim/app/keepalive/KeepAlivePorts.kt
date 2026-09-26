package com.pim.app.keepalive

import com.pim.app.mobile.logs.StructuredLogRepository
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CancellationException

/**
 * 闹钟调度与通知的窄接口（让编排逻辑可在 JVM 上断言）。
 *
 * 生产实现分别是 [KeepAliveAlarmScheduler]（系统 AlarmManager）与
 * [AndroidKeepAliveNotifications]（通知栏）。抽出来是因为编排里有几条**必须成立**的规则：
 * 关闭时不得登记（AC-22.3）、续登记不能被一次失败跳过、权限缺失要可见——这些都要能逐条断言。
 */
interface KeepAliveSchedulePort {
    sealed interface Result {
        data class Scheduled(val triggerAtUtcMillis: Long, val intervalMinutes: Int) : Result
        data object PermissionMissing : Result
        data class Failed(val message: String) : Result
        data object Disabled : Result
    }

    suspend fun scheduleNext(intervalMinutes: Int, enabled: Boolean = true): Result
    suspend fun cancel()
    fun hasExactAlarmPermission(): Boolean
}

/** 叫醒通知的接口（REQ-19）。 */
interface KeepAliveNotificationPort {
    /** 是否具备展示通知的条件（权限/开关）。 */
    fun isEnabled(): Boolean

    /** 确保常驻的那一条静默通知存在（不是新增一条，AC-19.1）。 */
    suspend fun ensureResident()

    /** 更新「最近一次叫醒时间」（内容滚动更新，条数不变）。 */
    suspend fun updateLastWake(atUtcMillis: Long?)

    /** 移除常驻通知。 */
    suspend fun dismissResident()
}

/**
 * 健康红点（双通道：状态页 + 通知栏，REQ-21）。
 *
 * - 四类原因各自能点亮（AC-21.1），消除后自动熄灭（AC-21.2）。
 * - ≤15 分钟的普通延迟**不**点亮（AC-21.3）——因此这里只接受
 *   [KeepAliveHealthReasons] 里的四类原因，延迟类结果根本不进这个类。
 */
@Singleton
class KeepAliveHealthMonitor internal constructor(
    private val ledger: KeepAliveLedger,
    private val notifications: KeepAliveNotificationPort,
    private val settings: KeepAliveSettingsAccessor,
    private val logs: StructuredLogRepository,
    private val nowUtcMillis: () -> Long
) {
    @Inject
    constructor(
        ledger: KeepAliveLedger,
        notifications: KeepAliveNotificationPort,
        settings: KeepAliveSettingsAccessor,
        logs: StructuredLogRepository
    ) : this(ledger, notifications, settings, logs, System::currentTimeMillis)

    /**
     * 当前点亮的原因集合（状态页红点读它）。
     *
     * **以持久化设置为准**，本字段只是缓存。原因：保活要解决的正是「应用被杀」，
     * 若红点状态只活在内存里，进程重启后异常会静默消失——那比不显示更糟，
     * 用户会以为一切正常。因此每次读写都经过 [KeepAliveSettingsAccessor]。
     */
    private fun activeReasons(): Set<String> = settings.read().activeHealthReasons

    /** 点亮一个原因：写台账（可见出口）+ 落盘 + 更新通知栏提示。 */
    suspend fun raise(reason: String, detail: String?) {
        require(KeepAliveHealthReasons.ALL.contains(reason)) {
            "未知的保活健康原因：$reason（只允许 REQ-21 的四类）"
        }

        val now = nowUtcMillis()
        // 先落盘：即使后面的台账/通知失败，红点也不会丢。
        val current = settings.read()
        settings.write(current.copy(activeHealthReasons = current.activeHealthReasons + reason))

        try {
            ledger.recordHealthEvent(reason, now, detail)
        } catch (ex: CancellationException) {
            throw ex
        } catch (ex: Exception) {
            logs.warn("keepalive", "记录健康事件失败：${ex.message ?: ""}")
        }

        try {
            notifications.ensureResident()
        } catch (ex: CancellationException) {
            throw ex
        } catch (ex: Exception) {
            logs.warn("keepalive", "更新保活通知失败：${ex.message ?: ""}")
        }
        logs.warn("keepalive", "保活健康异常：${KeepAliveHealthReasons.label(reason)}")
    }

    /** 消除一个原因：原因全部消除后红点自动熄灭（AC-21.2）。 */
    suspend fun clear(reason: String) {
        val current = settings.read()
        if (!current.activeHealthReasons.contains(reason)) return

        val remaining = current.activeHealthReasons - reason
        settings.write(current.copy(activeHealthReasons = remaining))
        if (remaining.isEmpty()) {
            try {
                notifications.dismissResident()
            } catch (ex: CancellationException) {
                throw ex
            } catch (ex: Exception) {
                logs.warn("keepalive", "移除保活通知失败：${ex.message ?: ""}")
            }
        }
    }

    /** 是否应点亮红点。 */
    fun isAlerting(): Boolean = activeReasons().isNotEmpty()

    /** 当前点亮的原因（供状态页文案）。 */
    fun reasons(): Set<String> = activeReasons()

    /** 状态页红点文案。 */
    fun summaryText(): String? =
        activeReasons().takeIf { it.isNotEmpty() }
            ?.joinToString("；") { KeepAliveHealthReasons.label(it) }
}
