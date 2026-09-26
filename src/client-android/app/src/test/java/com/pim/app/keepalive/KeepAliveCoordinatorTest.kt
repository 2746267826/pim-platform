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
            settings = settingsStore,
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

    /**
     * 复用同一份 settings / health / scheduler 的 fixture。
     *
     * 用途：需要「同一台设备先失败一次、再成功一次」这类**跨两次叫醒**的断言时，
     * 两次必须共享同一份持久化状态，否则测的是两个互不相干的实例。
     */
    private fun fixtureWithSharedHealth(
        env: FakeEnvironment,
        settings: FakeSettings,
        scheduler: FakeScheduler,
        notifications: FakeNotifications,
        health: KeepAliveHealthMonitor
    ): Fixture {
        val logRepo = logs()
        val recorder = FakeRecorder()
        val chain = WakeExecutionChain(
            ledger = recorder,
            settingsStore = settings,
            environment = env,
            logs = logRepo,
            nowUtcMillis = { 1_000_000L }
        )
        val coordinator = KeepAliveCoordinator(
            settingsStore = settings,
            scheduler = scheduler,
            executionChain = chain,
            ledger = keepAliveLedger(logRepo),
            health = health,
            notifications = notifications,
            logs = logRepo,
            nowUtcMillis = { 1_000_000L }
        )
        return Fixture(coordinator, scheduler, settings, notifications, health)
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

    /**
     * AC-21.1（四类原因之一）：拉起失败点亮红点。
     *
     * 口径说明：工单 §9.1 **没有**给出「连续失败 N 次」的 N，决策索引 R1-Q7/D13 的原话是
     * 「拉起失败：记台账 + 状态页红点 + 下轮重试（不降频）」，未要求连续次数；
     * 工单 §8.6 又明确禁止按自拟数值实现。因此本实现取「当前连续失败计数非零即点亮」，
     * 一次成功即清零熄灭，「连续」由计数器承担而不是一个凭空规定的门槛。
     * 本用例固定这一口径，防止日后有人偷偷塞一个自拟阈值进来。
     */
    @Test
    fun `AC-21_1 拉起失败即点亮红点（不自拟连续次数阈值）`() = runTest {
        val env = FakeEnvironment(serviceRunning = false, startSucceeds = false)
        val f = fixture(env = env)

        f.coordinator.onAlarmFired()

        assertTrue("拉起失败必须点亮红点", f.health.isAlerting())
        assertTrue(f.health.reasons().contains(KeepAliveHealthReasons.WAKE_CALL_FAILED))
    }

    /** AC-21.2：失败后一次成功即熄灭「拉起失败」红点（成功路径清除该原因）。 */
    @Test
    fun `AC-21_2 成功一次后拉起失败红点熄灭`() = runTest {
        // 先失败一次点亮红点
        val failing = FakeEnvironment(serviceRunning = false, startSucceeds = false)
        val f = fixture(env = failing)
        f.coordinator.onAlarmFired()
        assertTrue("前置：失败应点亮红点", f.health.isAlerting())

        // 再成功一次：执行链会清零失败计数，编排据结果清除该原因
        val succeeding = FakeEnvironment(serviceRunning = true)
        val succeedingFixture = fixtureWithSharedHealth(
            env = succeeding,
            settings = f.settings,
            scheduler = f.scheduler,
            notifications = f.notifications,
            health = f.health
        )
        succeedingFixture.coordinator.onAlarmFired()

        assertFalse(
            "成功一次后「拉起失败」红点应熄灭（AC-21.2）",
            f.health.reasons().contains(KeepAliveHealthReasons.WAKE_CALL_FAILED)
        )
        assertFalse("红点整体应熄灭", f.health.isAlerting())
    }

    /** AC-19.1：叫醒后通知内容更新为最近一次叫醒时间。 */
    @Test
    fun `AC-19_1 叫醒后更新通知内容`() = runTest {
        val f = fixture()

        f.coordinator.onAlarmFired()

        assertEquals(1_000_000L, f.notifications.lastWake)
        assertTrue("通知应处于常驻状态", f.notifications.resident)
    }

    /**
     * AC-21.1：闹钟被清空 → 点亮「闹钟被清空」并重新登记。
     *
     * 检测口径是「预定叫醒时刻已过且宽限期内仍未触发」——不用 PendingIntent 是否存在
     * （平台事实：cancel 后它仍在，见 KeepAliveAlarmSchedulerTest）。
     */
    @Test
    fun `AC-21_1 预定时刻已过却未触发时判定闹钟被清空`() = runTest {
        val scheduler = FakeScheduler()
        // 预定时刻远在过去（比 now=1_000_000 早 1 小时），说明那一枪没打响。
        val settings = KeepAliveSettings.defaults()
            .copy(pendingScheduledAtUtcMillis = 1_000_000L - 60 * 60_000L)
        val f = fixture(settings = settings, scheduler = scheduler)

        val outcome = f.coordinator.reconcile("app-start")

        assertTrue("应重新登记", outcome is KeepAliveScheduleOutcome.Scheduled)
        assertTrue(
            "闹钟打空了必须点亮红点",
            f.health.reasons().contains(KeepAliveHealthReasons.ALARM_CLEARED)
        )
    }

    /** AC-21.2：闹钟尚未到期时不点亮红点（原因消除后自动熄灭）。 */
    @Test
    fun `AC-21_2 闹钟尚未到期时不点亮红点`() = runTest {
        val scheduler = FakeScheduler()
        // 预定时刻在未来：闹钟还没到，属于正常状态。
        val settings = KeepAliveSettings.defaults()
            .copy(pendingScheduledAtUtcMillis = 1_000_000L + 10 * 60_000L)
        val f = fixture(settings = settings, scheduler = scheduler)

        val outcome = f.coordinator.reconcile("app-start")

        assertTrue(outcome is KeepAliveScheduleOutcome.AlreadyRegistered)
        assertFalse(f.health.isAlerting())
        assertTrue("尚未到期时不得重复登记", f.scheduler.scheduledIntervals.isEmpty())
    }

    /**
     * AC-15.4（Critical，由独立 review 指出）：**重启 / 应用更新后必须重建闹钟**。
     *
     * 平台事实（android-平台依据 §4）：被强行停止会清空该应用全部闹钟与作业；
     * 设备重启同样不会保留闹钟。也就是说在这两类事件之后，「系统里已经没有我们的闹钟」
     * 是**已知事实**，不需要靠「预定时刻是否已过」去猜。
     *
     * 原实现只看预定时刻是否过期：若重启发生在预定时刻**之前**（例如刚登记完 30 分钟
     * 就重启），`overdueBy < 0` → 判定「已登记」→ 不重建 → 保活永久停摆，
     * 而且界面还会显示「已登记」、红点也被清掉——把失效报成健康。
     */
    @Test
    fun `AC-15_4 重启后即使预定时刻未到也必须重建闹钟`() = runTest {
        val scheduler = FakeScheduler()
        // 预定时刻在未来 10 分钟（模拟「刚登记完就重启」）
        val settings = KeepAliveSettings.defaults()
            .copy(pendingScheduledAtUtcMillis = 1_000_000L + 10 * 60_000L)
        val f = fixture(settings = settings, scheduler = scheduler)

        val outcome = f.coordinator.reconcile("boot-or-update")

        assertTrue(
            "重启后系统里必定没有闹钟，必须重建（实际返回：$outcome）",
            outcome is KeepAliveScheduleOutcome.Scheduled
        )
        assertEquals("必须真的发起一次登记", 1, scheduler.scheduledIntervals.size)
    }

    /** AC-15.4 同理适用于应用更新（MY_PACKAGE_REPLACED）。 */
    @Test
    fun `AC-15_4 应用更新后即使预定时刻未到也必须重建闹钟`() = runTest {
        val scheduler = FakeScheduler()
        val settings = KeepAliveSettings.defaults()
            .copy(pendingScheduledAtUtcMillis = 1_000_000L + 10 * 60_000L)
        val f = fixture(settings = settings, scheduler = scheduler)

        f.coordinator.reconcile("package-replaced")

        assertEquals("应用更新后必须重建闹钟", 1, scheduler.scheduledIntervals.size)
    }

    /**
     * AC-21.1 的第四类原因「检测到强停」必须真的能被点亮。
     *
     * 此前 [KeepAliveHealthReasons.FORCE_STOPPED] 只被声明、从未被 raise，
     * 而操作卡（REQ-24）向需求方承诺强停后会出现「检测到应用被强行停止」的红点——
     * 承诺了却产生不出来的提示，比没有提示更糟。
     */
    @Test
    fun `AC-21_1 强停后点亮检测到强停的红点`() = runTest {
        val scheduler = FakeScheduler()
        val settings = KeepAliveSettings.defaults()
            .copy(pendingScheduledAtUtcMillis = 1_000_000L + 10 * 60_000L)
        val f = fixture(settings = settings, scheduler = scheduler)

        f.coordinator.reconcile("boot-or-update", forceStopped = true)

        assertTrue(
            "强停必须点亮「检测到应用被强行停止」的红点",
            f.health.reasons().contains(KeepAliveHealthReasons.FORCE_STOPPED)
        )
    }

    /** 首次开启保活（从未登记）时直接登记，且不算异常（不点红点）。 */
    @Test
    fun `首次开启保活时登记且不点亮红点`() = runTest {
        val f = fixture(settings = KeepAliveSettings.defaults().copy(pendingScheduledAtUtcMillis = null))

        val outcome = f.coordinator.reconcile("first-enable")

        assertTrue(outcome is KeepAliveScheduleOutcome.Scheduled)
        assertFalse("从未登记不是异常", f.health.isAlerting())
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
        val health = KeepAliveHealthMonitor(ledger, notifications, settingsStore, logRepo) { 1_000_000L }
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
