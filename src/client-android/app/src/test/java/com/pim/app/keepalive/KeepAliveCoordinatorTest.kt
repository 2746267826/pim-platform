package com.pim.app.keepalive

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
import org.robolectric.annotation.Config

/**
 * REQ-14 / REQ-15 / REQ-17 / REQ-22 的编排规则。
 *
 * 这里守的核心是「**续登记**」：保活最常见的失败形态是叫醒一次之后不再登记下一次，
 * 于是第一个周期之后就永久静默。用例明确断言「执行链失败/权限缺失之后仍然尝试续登记」。
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class KeepAliveCoordinatorTest {

    private class FakeScheduler(
        var permission: Boolean = true,
        var registered: Boolean = false,
        var failWith: String? = null
    ) : KeepAliveSchedulePort {
        val scheduledIntervals = mutableListOf<Int>()
        var cancelCount = 0

        override suspend fun scheduleNext(intervalMinutes: Int, enabled: Boolean): KeepAliveSchedulePort.Result {
            if (!enabled) {
                cancelCount++
                return KeepAliveSchedulePort.Result.Disabled
            }
            if (!permission) return KeepAliveSchedulePort.Result.PermissionMissing
            failWith?.let { return KeepAliveSchedulePort.Result.Failed(it) }
            scheduledIntervals += intervalMinutes
            registered = true
            return KeepAliveSchedulePort.Result.Scheduled(1_000_000L + intervalMinutes, intervalMinutes)
        }

        override suspend fun cancel() {
            cancelCount++
            registered = false
        }

        override suspend fun isAlarmRegistered(): Boolean = registered
        override fun hasExactAlarmPermission(): Boolean = permission
    }

    private class FakeNotifications : KeepAliveNotificationPort {
        var resident = false
        var lastWake: Long? = null
        var dismissCount = 0
        var enabled = true

        override fun isEnabled(): Boolean = enabled
        override suspend fun ensureResident() {
            resident = true
        }

        override suspend fun updateLastWake(atUtcMillis: Long?) {
            lastWake = atUtcMillis
        }

        override suspend fun dismissResident() {
            resident = false
            dismissCount++
        }
    }

    private class FakeSettings(var current: KeepAliveSettings) : KeepAliveSettingsAccessor {
        override fun read(): KeepAliveSettings = current
        override fun write(settings: KeepAliveSettings): KeepAliveSettings {
            current = settings
            return current
        }
    }

    private class FakeEnvironment(
        var paused: Boolean = false,
        var serviceRunning: Boolean = true,
        var startSucceeds: Boolean = true
    ) : WakeEnvironment {
        override suspend fun isPaused(): Boolean = paused
        override suspend fun isManualSessionActive(): Boolean = false
        override fun isForegroundServiceRunning(): Boolean = serviceRunning
        override suspend fun requestSyncNow(): Boolean = true
        override suspend fun startForegroundService(): Boolean = startSucceeds
        override suspend fun captureSinglePointFallback(): Boolean = true
    }

    private class FakeRecorder : AlarmFulfillmentRecorder {
        val written = mutableListOf<AlarmFulfillmentRecord>()
        override suspend fun recordFulfillment(record: AlarmFulfillmentRecord): Boolean {
            written += record
            return true
        }
    }

    private fun logs(): StructuredLogRepository {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val store = TrackingSettingsStore(
            context.getSharedPreferences("keepalive-coordinator-test", Context.MODE_PRIVATE)
        )
        return StructuredLogRepository(context, store) { 0L }
    }

    private class Fixture(
        val coordinator: KeepAliveCoordinator,
        val scheduler: FakeScheduler,
        val settings: FakeSettings,
        val notifications: FakeNotifications,
        val health: KeepAliveHealthMonitor
    )

    private fun fixture(
        settings: KeepAliveSettings = KeepAliveSettings.defaults(),
        scheduler: FakeScheduler = FakeScheduler(),
        env: FakeEnvironment = FakeEnvironment()
    ): Fixture {
        val settingsStore = FakeSettings(settings)
        val recorder = FakeRecorder()
        val notifications = FakeNotifications()
        val logRepo = logs()
        val health = KeepAliveHealthMonitor(
            ledger = keepAliveLedger(logRepo),
            notifications = notifications,
            logs = logRepo,
            nowUtcMillis = { 1_000_000L }
        )
        val chain = WakeExecutionChain(
            ledger = recorder,
            settingsStore = settingsStore,
            environment = env,
            logs = logRepo,
            nowUtcMillis = { 1_000_000L }
        )
        val coordinator = KeepAliveCoordinator(
            settingsStore = settingsStore,
            scheduler = scheduler,
            executionChain = chain,
            ledger = keepAliveLedger(logRepo),
            health = health,
            notifications = notifications,
            logs = logRepo,
            nowUtcMillis = { 1_000_000L }
        )
        return Fixture(coordinator, scheduler, settingsStore, notifications, health)
    }

    /** 真实 Room 台账（Robolectric 内存库），用于 recordAlarmRegistered 等落库调用。 */
    private fun keepAliveLedger(logRepo: StructuredLogRepository): KeepAliveLedger {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val db = androidx.room.Room.inMemoryDatabaseBuilder(context, com.pim.app.data.AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        return KeepAliveLedger(db.forensicEventDao(), logRepo)
    }

    /** AC-14.1：已授权时登记成功，并落一条闹钟登记事件。 */
    @Test
    fun `AC-14_1 已授权时登记成功`() = runTest {
        val f = fixture()

        val outcome = f.coordinator.scheduleNext("test")

        assertTrue(outcome is KeepAliveScheduleOutcome.Scheduled)
        assertEquals(listOf(AlarmSuppressionPolicy.DEFAULT_INTERVAL_MINUTES), f.scheduler.scheduledIntervals)
        assertFalse("成功时不应点亮红点", f.health.isAlerting())
    }

    /** AC-14.3：权限被撤销 → 红点点亮 + 不登记。 */
    @Test
    fun `AC-14_3 权限撤销时点亮红点且不登记`() = runTest {
        val f = fixture(scheduler = FakeScheduler(permission = false))

        val outcome = f.coordinator.scheduleNext("test")

        assertEquals(KeepAliveScheduleOutcome.PermissionMissing, outcome)
        assertTrue("权限问题必须点亮红点（AC-21.1）", f.health.isAlerting())
        assertTrue(f.health.reasons().contains(KeepAliveHealthReasons.PERMISSION_REVOKED))
        assertTrue("不得在未授权时登记", f.scheduler.scheduledIntervals.isEmpty())
    }

    /** AC-22.2 / AC-22.3：关闭总开关后不登记（且要取消已有的）。 */
    @Test
    fun `AC-22_2 关闭总开关后不登记`() = runTest {
        val f = fixture(settings = KeepAliveSettings.defaults().copy(enabled = false))

        val outcome = f.coordinator.scheduleNext("test")

        assertEquals(KeepAliveScheduleOutcome.Disabled, outcome)
        assertTrue("关闭时必须取消已登记的闹钟", f.scheduler.scheduledIntervals.isEmpty())
        assertEquals(1, f.scheduler.cancelCount)
    }

    /** AC-22.3：关闭期间不得暗中登记——多次调用都不得产生登记。 */
    @Test
    fun `AC-22_3 关闭期间反复调用都不登记`() = runTest {
        val f = fixture(settings = KeepAliveSettings.defaults().copy(enabled = false))

        repeat(5) { f.coordinator.scheduleNext("test") }

        assertTrue("关闭期间绝不能出现任何登记", f.scheduler.scheduledIntervals.isEmpty())
    }

    /** 续登记：闹钟触发后必须登记下一次（保活的核心）。 */
    @Test
    fun `闹钟触发后自动续登记下一次`() = runTest {
        val f = fixture()

        f.coordinator.onAlarmFired()

        assertEquals("触发后必须再登记一次", 1, f.scheduler.scheduledIntervals.size)
    }

    /** 续登记不能被一次拉起失败跳过（否则一次失败就永久停摆）。 */
    @Test
    fun `拉起失败后仍然续登记下一次`() = runTest {
        val env = FakeEnvironment(serviceRunning = false, startSucceeds = false)
        val f = fixture(env = env)

        f.coordinator.onAlarmFired()

        assertEquals("失败也必须续登记，否则保活停摆", 1, f.scheduler.scheduledIntervals.size)
    }

    /** AC-19.1：叫醒后通知内容更新为最近一次叫醒时间。 */
    @Test
    fun `AC-19_1 叫醒后更新通知内容`() = runTest {
        val f = fixture()

        f.coordinator.onAlarmFired()

        assertEquals(1_000_000L, f.notifications.lastWake)
        assertTrue("通知应处于常驻状态", f.notifications.resident)
    }

    /** AC-21.1：系统里闹钟消失 → 点亮「闹钟被清空」并重新登记。 */
    @Test
    fun `AC-21_1 闹钟被清空时点亮红点并重新登记`() = runTest {
        val scheduler = FakeScheduler(registered = false)
        val settings = KeepAliveSettings.defaults().copy(pendingScheduledAtUtcMillis = 999_000L)
        val f = fixture(settings = settings, scheduler = scheduler)

        val outcome = f.coordinator.reconcile("app-start")

        assertTrue("应重新登记", outcome is KeepAliveScheduleOutcome.Scheduled)
        assertTrue(f.health.reasons().contains(KeepAliveHealthReasons.ALARM_CLEARED))
    }

    /** AC-21.2：闹钟存在时不点亮红点（原因消除后自动熄灭）。 */
    @Test
    fun `AC-21_2 闹钟存在时不点亮红点`() = runTest {
        val scheduler = FakeScheduler(registered = true)
        val settings = KeepAliveSettings.defaults().copy(pendingScheduledAtUtcMillis = 999_000L)
        val f = fixture(settings = settings, scheduler = scheduler)

        f.coordinator.reconcile("app-start")

        assertFalse(f.health.isAlerting())
        assertTrue(f.scheduler.scheduledIntervals.isEmpty())
    }

    /** AC-17.2：连续被压制后，登记时使用翻倍后的间隔。 */
    @Test
    fun `AC-17_2 降频后按翻倍间隔登记`() = runTest {
        val settings = KeepAliveSettings.defaults().copy(
            configuredIntervalMinutes = 30,
            effectiveIntervalMinutes = 30,
            consecutiveSuppressed = 3
        )
        // 台账里已有 3 次被压制记录
        val logRepo = logs()
        val context = ApplicationProvider.getApplicationContext<Context>()
        val db = androidx.room.Room.inMemoryDatabaseBuilder(context, com.pim.app.data.AppDatabase::class.java)
            .allowMainThreadQueries().build()
        val ledger = KeepAliveLedger(db.forensicEventDao(), logRepo)
        // 三次的预定时刻必须各不相同：幂等键含预定时刻，完全相同的记录会被去重成一条
        // （真实运行中每个周期的预定时刻本来就不同）。
        repeat(3) { index ->
            val scheduled = 1_000_000L - (3 - index) * 30 * 60_000L
            ledger.recordFulfillment(
                AlarmFulfillmentRecord(
                    scheduledAtUtcMillis = scheduled,
                    actualAtUtcMillis = scheduled + 20 * 60_000L,
                    outcome = AlarmOutcomes.SUPPRESSED
                )
            )
        }

        val settingsStore = FakeSettings(settings)
        val notifications = FakeNotifications()
        val scheduler = FakeScheduler()
        val health = KeepAliveHealthMonitor(ledger, notifications, logRepo) { 1_000_000L }
        val chain = WakeExecutionChain(
            ledger = FakeRecorder(),
            settingsStore = settingsStore,
            environment = FakeEnvironment(),
            logs = logRepo,
            nowUtcMillis = { 1_000_000L }
        )
        val coordinator = KeepAliveCoordinator(
            settingsStore = settingsStore,
            scheduler = scheduler,
            executionChain = chain,
            ledger = ledger,
            health = health,
            notifications = notifications,
            logs = logRepo,
            nowUtcMillis = { 1_000_000L }
        )

        coordinator.scheduleNext("test")

        assertEquals("连续三次被压制后应按 60 分钟登记", listOf(60), scheduler.scheduledIntervals)
    }
}
