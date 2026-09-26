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
 * REQ-16 叫醒执行链 + REQ-20 暂停/手动会话语义 + REQ-17 计数推进。
 *
 * 用假的 [WakeEnvironment] 逐条走分支。重点是**顺序**与**不打扰用户**：
 * 工单明确「暂停时只写台账不拉起」「手动采集会话期间跳过且不打断会话」，
 * 这两条若写错，用户会在自己主动暂停后仍被应用偷偷拉起。
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class WakeExecutionChainTest {

    /** 与阶段一台账测试同款：Robolectric 提供真实 Context，结构化日志用固定时钟。 */
    private fun logs(): StructuredLogRepository {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val settings = TrackingSettingsStore(
            context.getSharedPreferences("keepalive-test", Context.MODE_PRIVATE)
        )
        return StructuredLogRepository(context, settings) { 0L }
    }

    /** 记录调用顺序的环境替身。 */
    private class FakeEnvironment(
        var paused: Boolean = false,
        var manualSession: Boolean = false,
        var serviceRunning: Boolean = false,
        var startSucceeds: Boolean = true,
        var syncSucceeds: Boolean = true,
        var fallbackCaptures: Boolean = true
    ) : WakeEnvironment {
        val calls = mutableListOf<String>()

        override suspend fun isPaused(): Boolean {
            calls += "isPaused"
            return paused
        }

        override suspend fun isManualSessionActive(): Boolean {
            calls += "isManualSessionActive"
            return manualSession
        }

        override fun isForegroundServiceRunning(): Boolean {
            calls += "isForegroundServiceRunning"
            return serviceRunning
        }

        override suspend fun requestSyncNow(): Boolean {
            calls += "requestSyncNow"
            return syncSucceeds
        }

        override suspend fun startForegroundService(): Boolean {
            calls += "startForegroundService"
            return startSucceeds
        }

        override suspend fun captureSinglePointFallback(): Boolean {
            calls += "captureSinglePointFallback"
            return fallbackCaptures
        }
    }

    /** 内存台账替身（只实现执行链需要的那一个方法）。 */
    private class FakeRecorder : AlarmFulfillmentRecorder {
        val written = mutableListOf<AlarmFulfillmentRecord>()
        override suspend fun recordFulfillment(record: AlarmFulfillmentRecord): Boolean {
            written += record
            return true
        }
    }

    /** 内存设置替身。 */
    private class FakeSettings(var current: KeepAliveSettings) : KeepAliveSettingsAccessor {
        override fun read(): KeepAliveSettings = current
        override fun write(settings: KeepAliveSettings): KeepAliveSettings {
            current = settings
            return current
        }
    }

    private val now = 1_700_000_000_000L

    private fun chain(
        env: FakeEnvironment,
        settings: KeepAliveSettings = KeepAliveSettings.defaults()
    ): Triple<WakeExecutionChain, FakeRecorder, FakeSettings> {
        val recorder = FakeRecorder()
        val store = FakeSettings(settings)
        val execution = WakeExecutionChain(
            ledger = recorder,
            settingsStore = store,
            environment = env,
            logs = logs(),
            nowUtcMillis = { now }
        )
        return Triple(execution, recorder, store)
    }

    /** AC-16.2：服务未运行时被拉起，且台账记录本次动作。 */
    @Test
    fun `AC-16_2 服务未运行则拉起并记台账`() = runTest {
        val env = FakeEnvironment(serviceRunning = false, startSucceeds = true)
        val (execution, recorder, _) = chain(env)

        val record = execution.execute(now)

        assertEquals(AlarmOutcomes.EXECUTED, record.outcome)
        assertTrue("必须真的尝试拉起服务", env.calls.contains("startForegroundService"))
        assertEquals("本次动作必须落台账（AC-16.1）", 1, recorder.written.size)
    }

    /** AC-16.1：已运行则触发一次补传，而不是重复拉服务。 */
    @Test
    fun `AC-16_1 服务已运行则触发一次补传`() = runTest {
        val env = FakeEnvironment(serviceRunning = true)
        val (execution, _, _) = chain(env)

        val record = execution.execute(now)

        assertEquals(AlarmOutcomes.EXECUTED, record.outcome)
        assertTrue(env.calls.contains("requestSyncNow"))
        assertFalse("已在运行时不得重复拉起", env.calls.contains("startForegroundService"))
    }

    /** AC-16.3：拉起失败时至少抓一颗定位点存本地待传。 */
    @Test
    fun `AC-16_3 拉起失败时兜底抓一颗点`() = runTest {
        val env = FakeEnvironment(serviceRunning = false, startSucceeds = false)
        val (execution, recorder, _) = chain(env)

        val record = execution.execute(now)

        assertEquals(AlarmOutcomes.PULL_FAILED, record.outcome)
        assertTrue("拉起失败必须兜底抓点", env.calls.contains("captureSinglePointFallback"))
        assertEquals(1, recorder.written.size)
    }

    /** AC-20.1 / AC-20.3：暂停后一个周期内只写台账，不拉起服务、不采集。 */
    @Test
    fun `AC-20_1 暂停时只写台账不拉起服务`() = runTest {
        val env = FakeEnvironment(paused = true)
        val (execution, recorder, _) = chain(env)

        val record = execution.execute(now)

        assertEquals(AlarmOutcomes.SKIPPED_PAUSED, record.outcome)
        assertFalse("暂停期间绝不能拉起服务", env.calls.contains("startForegroundService"))
        assertFalse("暂停期间不得请求补传", env.calls.contains("requestSyncNow"))
        assertFalse("暂停期间不得抓点", env.calls.contains("captureSinglePointFallback"))
        assertEquals("仍要留下台账证据", 1, recorder.written.size)
        assertNull("AC-18.3：未执行没有实际时刻，不进兑现率分母", record.actualAtUtcMillis)
    }

    /** AC-20.2：手动采集会话期间跳过该次闹钟，且不得打断会话。 */
    @Test
    fun `AC-20_2 手动采集会话期间跳过且不打断`() = runTest {
        val env = FakeEnvironment(manualSession = true, serviceRunning = true)
        val (execution, _, _) = chain(env)

        val record = execution.execute(now)

        assertEquals(AlarmOutcomes.SKIPPED_MANUAL_SESSION, record.outcome)
        assertFalse("不得拉起服务（会打断会话）", env.calls.contains("startForegroundService"))
        assertFalse("不得触发补传（会打断会话）", env.calls.contains("requestSyncNow"))
        assertFalse("不得抓点（会干扰会话）", env.calls.contains("captureSinglePointFallback"))
    }

    /** AC-20.3：暂停判定必须发生在任何拉起动作之前（顺序错了会变成「先拉起再判断」）。 */
    @Test
    fun `AC-20_3 暂停判定先于任何拉起动作`() = runTest {
        val env = FakeEnvironment(paused = true)
        val (execution, _, _) = chain(env)

        execution.execute(now)

        val pauseIndex = env.calls.indexOf("isPaused")
        assertTrue("暂停判定必须在拉起之前，实际调用顺序=${env.calls}", pauseIndex >= 0)
        assertFalse(
            "暂停判定之后不得再出现拉起动作，实际顺序=${env.calls}",
            env.calls.drop(pauseIndex).contains("startForegroundService")
        )
    }

    /** AC-22.2：总开关关闭时叫醒不执行、不采集。 */
    @Test
    fun `AC-22_2 保活关闭时不执行任何采集动作`() = runTest {
        val env = FakeEnvironment()
        val settings = KeepAliveSettings.defaults().copy(enabled = false)
        val (execution, _, _) = chain(env, settings)

        val record = execution.execute(now)

        assertEquals(AlarmOutcomes.SKIPPED_DISABLED, record.outcome)
        assertFalse(env.calls.contains("startForegroundService"))
        assertFalse(env.calls.contains("captureSinglePointFallback"))
    }

    /**
     * AC-16.4（反面）：叫醒不产生重复采集会话。
     *
     * 具体守两件事：
     * 1. 服务**已在运行**时只请求补传，绝不发起新的采集会话；
     * 2. 只有在拉起失败时才走兜底抓点（一次），且此时不得再额外拉起服务——
     *    否则一次叫醒可能同时产生「拉起的自动采集」与「兜底的手动会话」两套会话。
     */
    @Test
    fun `AC-16_4 服务已运行时不得发起新的采集会话`() = runTest {
        val env = FakeEnvironment(serviceRunning = true)
        val (execution, _, _) = chain(env)

        execution.execute(now)

        assertTrue("已运行时应请求补传", env.calls.contains("requestSyncNow"))
        assertFalse("已运行时不得发起兜底会话（AC-16.4）", env.calls.contains("captureSinglePointFallback"))
        assertFalse("已运行时不得重复拉起服务", env.calls.contains("startForegroundService"))
    }

    /** AC-16.4：拉起失败时兜底只抓一次点，且不再重复拉起服务。 */
    @Test
    fun `AC-16_4 拉起失败时兜底只执行一次`() = runTest {
        val env = FakeEnvironment(serviceRunning = false, startSucceeds = false)
        val (execution, _, _) = chain(env)

        execution.execute(now)

        assertEquals(
            "兜底抓点必须恰好一次",
            1,
            env.calls.count { it == "captureSinglePointFallback" }
        )
        assertEquals(
            "拉起只尝试一次（失败后不得反复拉起）",
            1,
            env.calls.count { it == "startForegroundService" }
        )
    }

    /** 手动采集会话进行中时不得走任何会话相关路径（AC-20.2 与 AC-16.4 同向）。 */
    @Test
    fun `AC-16_4 手动会话期间不产生任何采集调用`() = runTest {
        val env = FakeEnvironment(manualSession = true, serviceRunning = true)
        val (execution, _, _) = chain(env)

        execution.execute(now)

        assertFalse(env.calls.contains("captureSinglePointFallback"))
        assertFalse(env.calls.contains("requestSyncNow"))
        assertFalse(env.calls.contains("startForegroundService"))
    }

    /** AC-17.1：延迟大于阈值时结果构成「被压制」证据。 */
    @Test
    fun `AC-17_1 延迟过大时构成被压制证据`() = runTest {
        val env = FakeEnvironment(serviceRunning = true)
        val (execution, _, _) = chain(env)

        // 预定时刻比现在早 20 分钟 → 延迟 20 分钟 > 15 分钟
        val record = execution.execute(now - 20 * 60_000L)

        assertEquals(20 * 60_000L, record.delayMillis)
        assertTrue("延迟 20 分钟必须构成被压制证据", record.isSuppressedEvidence)
    }

    /** AC-17.5：拉起失败不推进被压制计数（否则失败会被误当成被压制而降频）。 */
    @Test
    fun `AC-17_5 拉起失败只推失败计数不推压制计数`() = runTest {
        val env = FakeEnvironment(serviceRunning = false, startSucceeds = false)
        val (execution, _, store) = chain(env)

        execution.execute(now)

        assertEquals("拉起失败不得被当成被压制", 0, store.current.consecutiveSuppressed)
        assertEquals(1, store.current.consecutiveWakeFailures)
    }

    /** AC-17.3：按时执行推进按时计数并清零压制计数。 */
    @Test
    fun `AC-17_3 按时执行推进按时计数`() = runTest {
        val env = FakeEnvironment(serviceRunning = true)
        val (execution, _, store) = chain(env)

        execution.execute(now)

        assertEquals(1, store.current.consecutiveOnTime)
        assertEquals(0, store.current.consecutiveSuppressed)
    }

    /** AC-17.2：连续被压制时执行链应把生效间隔翻倍（与计数器联动）。 */
    @Test
    fun `AC-17_2 连续三次被压制后生效间隔翻倍`() = runTest {
        val env = FakeEnvironment(serviceRunning = true)
        val settings = KeepAliveSettings.defaults().copy(
            configuredIntervalMinutes = 30,
            effectiveIntervalMinutes = 30
        )
        val (execution, _, store) = chain(env, settings)

        // 三次都延迟 20 分钟（>15）
        repeat(3) { execution.execute(now - 20 * 60_000L) }

        assertEquals(3, store.current.consecutiveSuppressed)
        val next = AlarmSuppressionPolicy.resolveEffectiveMinutes(
            configuredMinutes = store.current.configuredIntervalMinutes,
            effectiveMinutes = store.current.effectiveIntervalMinutes,
            recentRecords = listOf(
                AlarmFulfillmentRecord(now - 20 * 60_000L, now, AlarmOutcomes.SUPPRESSED),
                AlarmFulfillmentRecord(now - 20 * 60_000L, now, AlarmOutcomes.SUPPRESSED),
                AlarmFulfillmentRecord(now - 20 * 60_000L, now, AlarmOutcomes.SUPPRESSED)
            )
        )
        assertEquals("连续三次被压制后应为配置值的两倍", 60, next)
    }
}
