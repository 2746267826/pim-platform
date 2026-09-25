package com.pim.app.keepalive

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.pim.app.mobile.logs.StructuredLogRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 精确闹钟的登记与取消（REQ-15）。
 *
 * 平台约束与工单要求（`references/android-平台依据.md` §1）：
 * - **只用 `setExactAndAllowWhileIdle`**，绝不用 `setAlarmClock`——后者会在状态栏留下闹钟图标
 *   （AC-15.3 反面明确禁止）。
 * - 必须显式判断 `canScheduleExactAlarms()`：Android 14+ 起 `SCHEDULE_EXACT_ALARM` 不再默认授予，
 *   未授权时调用会被系统拒绝（`SecurityException`），必须给出可见出口而不是静默失败（AC-14.4 / REQ-28）。
 * - 跟随本地时间：用壁钟 `System.currentTimeMillis()` 计算下一次触发（AC-15.2），
 *   改时区后下一次登记自然落到新的本地时间。
 *
 * 本类只负责「按给定间隔登记下一次」，不参与降频判定（见 [AlarmSuppressionPolicy]）与执行链。
 */
@Singleton
class KeepAliveAlarmScheduler internal constructor(
    private val alarmManager: AlarmManager?,
    private val appContext: Context,
    private val logs: StructuredLogRepository,
    private val nowUtcMillis: () -> Long,
    private val canScheduleExactAlarms: () -> Boolean,
    private val platformSdkInt: Int
) : KeepAliveSchedulePort {
    @Inject
    constructor(
        @ApplicationContext context: Context,
        logs: StructuredLogRepository
    ) : this(
        alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager,
        appContext = context.applicationContext,
        logs = logs,
        nowUtcMillis = System::currentTimeMillis,
        canScheduleExactAlarms = { defaultCanScheduleExactAlarms(context) },
        platformSdkInt = Build.VERSION.SDK_INT
    )

    /**
     * 按 [intervalMinutes] 登记下一次叫醒。
     *
     * @param intervalMinutes 实际生效的间隔（已由降频策略算好，REQ-17 / REQ-22）
     * @param enabled 保活总开关（AC-22.2：关闭时不得登记）
     */
    override suspend fun scheduleNext(intervalMinutes: Int, enabled: Boolean): KeepAliveSchedulePort.Result {
        if (!enabled) {
            cancel()
            return KeepAliveSchedulePort.Result.Disabled
        }

        val manager = alarmManager ?: return KeepAliveSchedulePort.Result.Failed("系统未提供闹钟服务")

        if (!canScheduleExactAlarms()) {
            // AC-14.4：未授权必须可见，由调用方点亮红点并给引导入口。
            logs.warn("keepalive", "无法登记精确闹钟：未获得「闹钟和提醒」权限")
            return KeepAliveSchedulePort.Result.PermissionMissing
        }

        val interval = AlarmSuppressionPolicy.clampConfiguredMinutes(intervalMinutes)
        val triggerAt = nowUtcMillis() + interval * 60_000L
        return try {
            val pendingIntent = buildPendingIntent(appContext)
                ?: return KeepAliveSchedulePort.Result.Failed("无法创建叫醒的 PendingIntent")
            manager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pendingIntent)
            logs.info("keepalive", "已登记精确闹钟：$interval 分钟后（$triggerAt）")
            KeepAliveSchedulePort.Result.Scheduled(triggerAt, interval)
        } catch (ex: SecurityException) {
            // 权限在读取与登记之间被撤销时会走到这里（AC-14.3）。
            logs.warn("keepalive", "登记精确闹钟被系统拒绝：${ex.message ?: ""}")
            KeepAliveSchedulePort.Result.PermissionMissing
        } catch (ex: Exception) {
            logs.error("keepalive", "登记精确闹钟失败：${ex.message ?: ex::class.java.simpleName}", ex)
            KeepAliveSchedulePort.Result.Failed(ex.message ?: ex::class.java.simpleName)
        }
    }

    /** 取消已登记的闹钟。 */
    override suspend fun cancel() {
        try {
            val pendingIntent = buildPendingIntent(appContext) ?: return
            alarmManager?.cancel(pendingIntent)
        } catch (ex: Exception) {
            logs.warn("keepalive", "取消精确闹钟失败：${ex.message ?: ""}")
        }
    }

    // 刻意**不提供**「闹钟是否还在系统里」的查询：
    // AlarmManager 没有公开 API 能列出本应用已登记的闹钟；而 `PendingIntent` 在
    // `cancel()` 之后依然存在（实测：cancel 后 FLAG_NO_CREATE 仍返回非 null），
    // 因此「PendingIntent 是否存在」不能用来判断闹钟是否还在——那样会永远报告
    // 「闹钟在」，恰好掩盖 AC-21.1 要检测的「闹钟被清空」。
    // 正确做法是基于**截止时刻**判断（见 KeepAliveCoordinator.reconcile）。

    /** 当前是否具备精确闹钟授权（引导页与状态区据此显示）。 */
    override fun hasExactAlarmPermission(): Boolean = try {
        canScheduleExactAlarms()
    } catch (_: Exception) {
        false
    }

    /** 系统是否支持精确闹钟（API 19+ 恒为真；保留判断以便引导页对不可检测项降级）。 */
    fun isExactAlarmCapable(): Boolean = platformSdkInt >= Build.VERSION_CODES.KITKAT

    companion object {
        /** 叫醒广播的 action（[KeepAliveAlarmReceiver] 接收）。 */
        const val ACTION_KEEPALIVE_ALARM = "com.pim.app.keepalive.action.ALARM"

        /** 请求码固定，保证同一 PendingIntent 可被取消与查询。 */
        const val REQUEST_CODE = 7301

        /**
         * 构造叫醒 PendingIntent。
         *
         * @param flags 传 `FLAG_NO_CREATE` 用于查询是否已登记
         */
        internal fun buildPendingIntent(
            context: Context,
            flags: Int = PendingIntent.FLAG_UPDATE_CURRENT
        ): PendingIntent? {
            val intent = Intent(context, KeepAliveAlarmReceiver::class.java)
                .setAction(ACTION_KEEPALIVE_ALARM)
            return PendingIntent.getBroadcast(
                context,
                REQUEST_CODE,
                intent,
                flags or PendingIntent.FLAG_IMMUTABLE
            )
        }

        /** API 31+ 用系统查询结果；更低版本无需该权限。 */
        internal fun defaultCanScheduleExactAlarms(context: Context): Boolean =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                (context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager)
                    ?.canScheduleExactAlarms() ?: false
            } else {
                true
            }
    }
}
