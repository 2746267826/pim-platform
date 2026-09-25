package com.pim.app.keepalive

import android.app.AlarmManager
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.pim.app.mobile.logs.StructuredLogRepository
import com.pim.app.settings.TrackingSettingsStore
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

/**
 * REQ-15 闹钟实现方式：**只用 `setExactAndAllowWhileIdle`**（AC-15.3 反面禁止 `setAlarmClock`）。
 *
 * 断言打在 Robolectric 的 `ShadowAlarmManager` 上——即**真实调用到的系统 API**，
 * 而不是「实现里有没有出现某个字符串」。这样把实现换成 setAlarmClock 会立刻被抓到。
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class KeepAliveAlarmSchedulerTest {

    private fun logs(): StructuredLogRepository {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val settings = TrackingSettingsStore(
            context.getSharedPreferences("keepalive-scheduler-test", Context.MODE_PRIVATE)
        )
        return StructuredLogRepository(context, settings) { 0L }
    }

    private fun scheduler(
        nowMillis: Long = 1_000_000_000_000L,
        permission: Boolean = true,
        sdkInt: Int = 34
    ): Pair<KeepAliveAlarmScheduler, AlarmManager> {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val manager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val instance = KeepAliveAlarmScheduler(
            alarmManager = manager,
            appContext = context,
            logs = logs(),
            nowUtcMillis = { nowMillis },
            canScheduleExactAlarms = { permission },
            platformSdkInt = sdkInt
        )
        return instance to manager
    }

    /** AC-15.1：默认 30 分钟，登记的下一次触发时刻 = 现在 + 间隔。 */
    @Test
    fun `AC-15_1 按间隔登记下一次触发`() = runTest {
        val now = 1_000_000_000_000L
        val (instance, manager) = scheduler(nowMillis = now)

        val result = instance.scheduleNext(AlarmSuppressionPolicy.DEFAULT_INTERVAL_MINUTES, enabled = true)

        assertTrue(result is KeepAliveSchedulePort.Result.Scheduled)
        val expected = now + 30 * 60_000L
        assertEquals(expected, (result as KeepAliveSchedulePort.Result.Scheduled).triggerAtUtcMillis)

        val scheduled = shadowOf(manager).nextScheduledAlarm
        assertEquals(
            "必须用 setExactAndAllowWhileIdle 登记（注册类型为 RTC_WAKEUP）",
            AlarmManager.RTC_WAKEUP,
            scheduled.type
        )
        assertEquals(expected, scheduled.triggerAtTime)
    }

    /**
     * AC-15.3（反面）：不得使用 `setAlarmClock`——那会在状态栏留下闹钟图标。
     *
     * 断言方式：登记之后，系统里**不应**出现 AlarmClockInfo（`setAlarmClock` 的产物）。
     */
    @Test
    fun `AC-15_3 不使用 setAlarmClock 因此状态栏无闹钟图标`() = runTest {
        val (instance, manager) = scheduler()

        instance.scheduleNext(30, enabled = true)

        val shadow = shadowOf(manager)
        assertEquals(
            "不得调用 setAlarmClock（会导致状态栏出现闹钟图标）",
            null,
            shadow.nextScheduledAlarm?.showIntent
        )
    }

    /** 节奏必须被夹到 10-120 分钟（AC-15.1）。 */
    @Test
    fun `AC-15_1 越界节奏被夹到允许区间`() = runTest {
        val now = 1_000_000_000_000L
        val (tooSmall, m1) = scheduler(nowMillis = now)
        tooSmall.scheduleNext(1, enabled = true)
        assertEquals(now + 10 * 60_000L, shadowOf(m1).nextScheduledAlarm.triggerAtTime)

        val (tooLarge, m2) = scheduler(nowMillis = now)
        tooLarge.scheduleNext(999, enabled = true)
        assertEquals(now + 120 * 60_000L, shadowOf(m2).nextScheduledAlarm.triggerAtTime)
    }

    /** AC-14.4 / REQ-28：未授权时不得静默失败，要返回可见结果且不登记。 */
    @Test
    fun `AC-14_4 未授权时返回可见结果且不登记`() = runTest {
        val (instance, manager) = scheduler(permission = false)

        val result = instance.scheduleNext(30, enabled = true)

        assertEquals(KeepAliveSchedulePort.Result.PermissionMissing, result)
        assertEquals("未授权时不得登记闹钟", null, shadowOf(manager).nextScheduledAlarm)
    }

    /** AC-22.2：总开关关闭时不登记，并取消已有的。 */
    @Test
    fun `AC-22_2 关闭时不登记`() = runTest {
        val (instance, manager) = scheduler()

        val result = instance.scheduleNext(30, enabled = false)

        assertEquals(KeepAliveSchedulePort.Result.Disabled, result)
        assertEquals(null, shadowOf(manager).nextScheduledAlarm)
    }

    /** AC-15.2：跟随本地时间——登记时刻基于壁钟（改时区后下一次自然落到新的本地时间）。 */
    @Test
    fun `AC-15_2 触发时刻基于壁钟时间`() = runTest {
        val now = 1_700_000_000_000L
        val (instance, manager) = scheduler(nowMillis = now)

        instance.scheduleNext(45, enabled = true)

        assertEquals(now + 45 * 60_000L, shadowOf(manager).nextScheduledAlarm.triggerAtTime)
    }

    /**
     * 平台事实（决定了 AC-21.1 的检测手段）：`AlarmManager.cancel()` 只移除**闹钟**，
     * 不会移除 `PendingIntent`。因此「PendingIntent 是否还存在」不能用来判断闹钟是否还在，
     * 否则会永远报告「闹钟在」，掩盖「闹钟被清空」这一要检测的情况。
     * 这里把该事实固定下来，防止有人日后又用 PendingIntent 来检测。
     */
    @Test
    fun `平台事实 cancel 后 PendingIntent 仍在，不能据此判断闹钟存在`() = runTest {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val (instance, manager) = scheduler()

        instance.scheduleNext(30, enabled = true)
        instance.cancel()

        assertEquals("cancel 后系统里不应再有闹钟", null, shadowOf(manager).nextScheduledAlarm)
        assertTrue(
            "cancel 后 PendingIntent 依然存在——所以不能用它判断闹钟是否还在（AC-21.1）",
            KeepAliveAlarmScheduler.buildPendingIntent(
                context, android.app.PendingIntent.FLAG_NO_CREATE
            ) != null
        )
    }
}
