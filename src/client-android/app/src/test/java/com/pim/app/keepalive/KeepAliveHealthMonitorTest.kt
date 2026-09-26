package com.pim.app.keepalive

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.pim.app.mobile.logs.StructuredLogRepository
import com.pim.app.settings.TrackingSettingsStore
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * REQ-21 健康红点（双通道）。
 *
 * 最关键的一条不在「能不能点亮」，而在**进程重启后还在不在**：
 * 保活要解决的正是「应用被杀」，如果红点状态只活在内存里，
 * 那么被杀之后红点会静默消失——异常被抹掉，比不显示更糟。
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class KeepAliveHealthMonitorTest {

    private class FakeNotifications : KeepAliveNotificationPort {
        var resident = false
        var dismissCount = 0
        override fun isEnabled(): Boolean = true
        override suspend fun ensureResident() {
            resident = true
        }

        override suspend fun updateLastWake(atUtcMillis: Long?) = Unit
        override suspend fun dismissResident() {
            resident = false
            dismissCount++
        }
    }

    private fun logs(tag: String): StructuredLogRepository {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val store = TrackingSettingsStore(
            context.getSharedPreferences("health-test-$tag", Context.MODE_PRIVATE)
        )
        return StructuredLogRepository(context, store) { 0L }
    }

    private fun ledger(tag: String, logRepo: StructuredLogRepository): KeepAliveLedger {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val db = androidx.room.Room.inMemoryDatabaseBuilder(
            context,
            com.pim.app.data.AppDatabase::class.java
        ).allowMainThreadQueries().build()
        return KeepAliveLedger(db.forensicEventDao(), logRepo)
    }

    /** 内存设置替身（红点状态要落这里，见 AC-21.2 的「记住」语义）。 */
    private class FakeSettings(var current: KeepAliveSettings) : KeepAliveSettingsAccessor {
        override fun read(): KeepAliveSettings = current
        override fun write(settings: KeepAliveSettings): KeepAliveSettings {
            current = settings
            return current
        }
    }

    private fun fixture(
        tag: String,
        settings: KeepAliveSettings = KeepAliveSettings.defaults()
    ): Triple<KeepAliveHealthMonitor, FakeNotifications, FakeSettings> {
        val logRepo = logs(tag)
        val notifications = FakeNotifications()
        val settingsStore = FakeSettings(settings)
        val monitor = KeepAliveHealthMonitor(
            ledger = ledger(tag, logRepo),
            notifications = notifications,
            settings = settingsStore,
            logs = logRepo,
            nowUtcMillis = { 1_000_000L }
        )
        return Triple(monitor, notifications, settingsStore)
    }

    /** AC-21.1：四类原因各自能点亮。 */
    @Test
    fun `AC-21_1 四类原因各自能点亮红点`() = runTest {
        KeepAliveHealthReasons.ALL.forEachIndexed { index, reason ->
            val (monitor, _, _) = fixture("raise-$index")

            monitor.raise(reason, "测试原因")

            assertTrue("$reason 必须能点亮红点", monitor.isAlerting())
            assertTrue(monitor.reasons().contains(reason))
        }
    }

    /**
     * AC-21.2：原因消除后红点自动熄灭。
     */
    @Test
    fun `AC-21_2 原因消除后红点熄灭`() = runTest {
        val (monitor, notifications, _) = fixture("clear")

        monitor.raise(KeepAliveHealthReasons.PERMISSION_REVOKED, null)
        assertTrue(monitor.isAlerting())

        monitor.clear(KeepAliveHealthReasons.PERMISSION_REVOKED)

        assertFalse("唯一原因消除后红点必须熄灭", monitor.isAlerting())
        assertNull(monitor.summaryText())
        assertEquals("红点熄灭时应移除常驻通知", 1, notifications.dismissCount)
    }

    /** AC-21.2：还有其它原因时不得提前熄灭（多原因并存）。 */
    @Test
    fun `AC-21_2 多原因并存时只消除指定的那个`() = runTest {
        val (monitor, _, _) = fixture("multi")

        monitor.raise(KeepAliveHealthReasons.PERMISSION_REVOKED, null)
        monitor.raise(KeepAliveHealthReasons.ALARM_CLEARED, null)
        monitor.clear(KeepAliveHealthReasons.PERMISSION_REVOKED)

        assertTrue("还有其它原因时红点应保持点亮", monitor.isAlerting())
        assertTrue(monitor.reasons().contains(KeepAliveHealthReasons.ALARM_CLEARED))
        assertFalse(monitor.reasons().contains(KeepAliveHealthReasons.PERMISSION_REVOKED))
    }

    /**
     * 关键：**进程重启后红点状态不得丢失**。
     *
     * 保活面对的就是「应用被杀」，若红点只存在于内存，被杀之后异常会静默消失。
     * 这里用一个全新的 monitor 实例模拟重启，状态必须从持久化来源恢复。
     */
    @Test
    fun `进程重启后红点状态仍然保留`() = runTest {
        val settingsStore = FakeSettings(KeepAliveSettings.defaults())
        val logRepo = logs("restart")
        val notifications = FakeNotifications()
        val first = KeepAliveHealthMonitor(
            ledger = ledger("restart", logRepo),
            notifications = notifications,
            settings = settingsStore,
            logs = logRepo,
            nowUtcMillis = { 1_000_000L }
        )

        first.raise(KeepAliveHealthReasons.PERMISSION_REVOKED, "权限被撤销")

        // 模拟进程重启：同一份持久化设置、全新的 monitor 实例。
        val second = KeepAliveHealthMonitor(
            ledger = ledger("restart", logRepo),
            notifications = FakeNotifications(),
            settings = settingsStore,
            logs = logRepo,
            nowUtcMillis = { 2_000_000L }
        )

        assertTrue(
            "进程重启后红点必须仍然点亮（否则被杀即静默抹掉异常）",
            second.isAlerting()
        )
        assertTrue(second.reasons().contains(KeepAliveHealthReasons.PERMISSION_REVOKED))
    }

    /** AC-21.3（反面）：普通延迟不进红点——只有 REQ-21 的四类原因才允许。 */
    @Test
    fun `AC-21_3 不允许用延迟类原因点亮红点`() = runTest {
        val (monitor, _, _) = fixture("delay")

        val thrown = runCatching {
            monitor.raise("suppressed", "普通延迟")
        }.exceptionOrNull()

        assertTrue(
            "≤15 分钟延迟或压制延迟不属于 REQ-21 四类原因，必须被拒绝",
            thrown is IllegalArgumentException
        )
        assertFalse(monitor.isAlerting())
    }

    /** 红点文案为简体中文（AC-26.1）。 */
    @Test
    fun `AC-26_1 红点文案为中文`() = runTest {
        val (monitor, _, _) = fixture("label")

        monitor.raise(KeepAliveHealthReasons.ALARM_CLEARED, null)

        val text = monitor.summaryText()
        assertTrue("应有中文文案：$text", text != null && text.contains("闹钟"))
    }

    /** 通知栏通道：点亮时确保常驻通知存在（AC-21.1 双通道）。 */
    @Test
    fun `AC-21_1 点亮时确保通知栏提示存在`() = runTest {
        val (monitor, notifications, _) = fixture("notify")

        monitor.raise(KeepAliveHealthReasons.WAKE_CALL_FAILED, null)

        assertTrue("双通道：状态页红点之外，通知栏也要有提示", notifications.resident)
    }
}
