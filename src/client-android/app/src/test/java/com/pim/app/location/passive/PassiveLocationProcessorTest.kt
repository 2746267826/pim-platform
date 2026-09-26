package com.pim.app.location.passive

import com.pim.app.location.LocationSnapshot
import com.pim.app.location.quality.LocationQualityGate
import com.pim.app.location.quality.QualityAcceptedLocation
import com.pim.app.location.quality.QualityDecision
import com.pim.app.location.quality.RawLocationFix
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import kotlinx.coroutines.test.runTest
import org.junit.Test

/**
 * WO-ANDROID-GATE-20260926 REQ-14（AC-14.2 / AC-14.3 / AC-14.4 / AC-14.5 / AC-14.6）。
 *
 * [PassiveLocationProcessor] 是被动点的纯判定器：走同一道 30 米质量门、
 * 不设限流、达标全收、不达标与重复**都留痕**（三个数对账缺口 = 0）。
 *
 * 「不设限流」这条在实现上是**结构性的**：本类里不存在任何间隔/条数参数，
 * 因此不可能悄悄引入节流 —— 断言里直接检查处理器没有时间窗状态。
 */
class PassiveLocationProcessorTest {

    private val accepted = mutableListOf<QualityAcceptedLocation>()
    private val dropped = mutableListOf<Pair<RawLocationFix, String>>()

    private fun processor(activeFixes: List<RawLocationFix> = emptyList()): PassiveLocationProcessor {
        val processor = PassiveLocationProcessor(
            qualityGate = LocationQualityGate(),
            activeFixProvider = { activeFixes }
        )
        processor.onDrop = { fix, reason -> dropped += fix to reason }
        processor.onAccepted = { result -> accepted += result.accepted }
        return processor
    }

    /** AC-14.2 / AC-14.3：符合精度的被动点**全部**收下，逐条入库。 */
    @Test
    fun `符合精度的被动点全部收下`() = runTest {
        val processor = processor()
        val fixes = (0 until 300).map { fix(at = START + it * 1_000L, accuracy = 8f) }

        fixes.forEach { processor.handle(it) }

        assertEquals(
            "AC-14.3：300 次回调必须逐条收下（不设限流、无裁剪）",
            300,
            accepted.size
        )
        assertEquals("AC-14.2：合格点不得产生丢弃记录", 0, dropped.size)
        assertEquals(300, processor.countersSnapshot().callbackCount)
        assertEquals(300, processor.countersSnapshot().acceptedCount)
        assertEquals(0, processor.countersSnapshot().droppedCount)
        assertEquals(0, processor.countersSnapshot().duplicateCount)
    }

    /** AC-14.3：**1 秒 1 次持续定位**场景不得被限流。 */
    @Test
    fun `一秒一次的持续被动回调不被限流`() = runTest {
        val processor = processor()

        // 连续 600 次、每次间隔 1 秒、坐标完全相同 —— 若实现里藏了「最小入库间隔」
        // 或「同坐标去重」，这里就会掉点。
        val outcomes = (0 until 600).map { index ->
            processor.handle(fix(at = START + index * 1_000L, accuracy = 12f))
        }

        assertEquals(
            "AC-14.3：1 秒 1 次持续 10 分钟不得被限流（逐条入库）",
            600,
            outcomes.count { it != null }
        )
        assertEquals(600, processor.countersSnapshot().acceptedCount)
        assertEquals(
            "AC-14.2：被动回调总数必须逐条计数（去重前）",
            600,
            processor.countersSnapshot().callbackCount
        )
    }

    /** AC-14.2 / AC-14.6：不达标的被动点必须留下丢弃记录，缺一不可。 */
    @Test
    fun `不达标的被动点全部留下丢弃记录`() = runTest {
        val processor = processor()

        val accuracies = listOf(30f, 45f, 200f, 30.0f)
        accuracies.forEach { accuracy ->
            val outcome = processor.handle(fix(at = START, accuracy = accuracy))
            assertEquals("不达标点不得被收下", null, outcome)
        }

        val counters = processor.countersSnapshot()
        assertEquals(4, counters.callbackCount)
        assertEquals(0, counters.acceptedCount)
        assertEquals(
            "AC-14.6：每一条被丢的被动点都必须留下记录",
            4,
            counters.droppedCount
        )
        assertEquals(
            "AC-14.2：回调 = 入库 + 丢弃 + 重复，缺口必须为 0",
            0,
            counters.unaccountedCount
        )
    }

    /** AC-14.2：精度字段缺失的被动点同样留痕（不得静默）。 */
    @Test
    fun `缺少精度的被动点留下记录`() = runTest {
        val processor = processor()

        processor.handle(fix(at = START, accuracy = null))

        val counters = processor.countersSnapshot()
        assertEquals(1, counters.callbackCount)
        assertEquals(0, counters.acceptedCount)
        assertEquals(1, counters.droppedCount)
        assertEquals(0, counters.unaccountedCount)
        assertEquals(
            "缺少精度的被动点必须用被动专属原因编码",
            PassiveLocationContract.REASON_MISSING_ACCURACY,
            processor.lastDropReason()
        )
    }

    /** AC-14.4：丢弃记录用 `passive-` 前缀的原因编码，可与主动流区分。 */
    @Test
    fun `丢弃原因使用被动专属编码`() = runTest {
        val processor = processor()

        processor.handle(fix(at = START, accuracy = 40f))

        val reason = processor.lastDropReason()!!
        assertTrue(
            "AC-14.4：被动丢弃记录必须可按 reason 前缀 passive- 筛选",
            PassiveLocationContract.isPassiveReason(reason)
        )
        assertEquals("passive-horizontal-accuracy-too-low", reason)
    }

    /** AC-14.5：与主动流同一 fix（时间差 ≤1 秒 + 坐标差 ≤0.5 米）判定为重复并留痕。 */
    @Test
    fun `与主动流重复的被动点只留痕不入库`() = runTest {
        val activeFix = fix(at = START + 500L, accuracy = 10f, latitude = 31.230416).toRawFix()
        val processor = processor(activeFixes = listOf(activeFix))

        val outcome = processor.handle(
            fix(at = START, accuracy = 10f, latitude = 31.230416)
        )

        assertEquals("AC-14.5：与主动流同一 fix 只入库一条", null, outcome)
        val counters = processor.countersSnapshot()
        assertEquals(1, counters.callbackCount)
        assertEquals(0, counters.acceptedCount)
        assertEquals(
            "AC-14.5：重复事件也必须有记录，客户端不留痕就看不到重复率",
            1,
            counters.duplicateCount
        )
        assertEquals(0, counters.droppedCount)
        assertEquals(0, counters.unaccountedCount)
        assertEquals(
            PassiveLocationContract.REASON_DUPLICATE_FIX,
            processor.lastDropReason()
        )
    }

    /** AC-14.5：时间差 > 1 秒不算重复。 */
    @Test
    fun `时间差超过一秒不算重复`() = runTest {
        val activeFix = fix(at = START + 1_500L, accuracy = 10f).toRawFix()
        val processor = processor(activeFixes = listOf(activeFix))

        val outcome = processor.handle(fix(at = START, accuracy = 10f))

        assertTrue("AC-14.5：时间差 > 1 秒不得判为重复", outcome != null)
        assertEquals(0, processor.countersSnapshot().duplicateCount)
    }

    /** AC-14.5：坐标差 > 0.5 米不算重复。 */
    @Test
    fun `坐标差超过半米不算重复`() = runTest {
        // 纬度 1e-5 度 ≈ 1.1 米
        val activeFix = fix(at = START, accuracy = 10f, latitude = 31.230426).toRawFix()
        val processor = processor(activeFixes = listOf(activeFix))

        val outcome = processor.handle(fix(at = START, accuracy = 10f, latitude = 31.230416))

        assertTrue("AC-14.5：坐标差 > 0.5 米不得判为重复", outcome != null)
        assertEquals(0, processor.countersSnapshot().duplicateCount)
    }

    /** AC-14.4：入库点按 `source = passive` 标记，provider 保持原始提供方。 */
    @Test
    fun `入库点标记为 passive 且 provider 保持原始值`() = runTest {
        val processor = processor()

        val outcome = processor.handle(
            fix(at = START, accuracy = 10f, provider = "fused")
        )

        assertEquals(
            "AC-14.4：入库点必须能按 source = passive 筛选",
            PassiveLocationContract.SOURCE,
            outcome!!.source
        )
        assertEquals(
            "REQ-14 规则 6：provider 保持原始提供方，不得改写为 passive",
            "fused",
            outcome.accepted.fix.provider
        )
    }

    /** AC-14.8：被动点不得计入冲刺台账口径（处理器与冲刺台账无耦合）。 */
    @Test
    fun `被动点不产生冲刺台账口径`() = runTest {
        val processor = processor()

        repeat(10) { processor.handle(fix(at = START + it * 1_000L, accuracy = 10f)) }

        val counters = processor.countersSnapshot()
        assertEquals(
            "AC-14.8：被动回调计数独立于冲刺的 sprintSampleCount / sprintAcceptedCount",
            10,
            counters.callbackCount
        )
        // 处理器只暴露被动口径，不存在任何 sprint* 字段
        assertFalse(
            "AC-14.8：被动处理器不得暴露冲刺口径字段",
            PassiveLocationCounters::class.java.declaredFields.any { it.name.contains("sprint") }
        )
    }

    /** AC-14.7：被动处理不得改变主动流（处理器是纯函数式判定，无主动流副作用）。 */
    @Test
    fun `被动处理不触碰主动流注册`() = runTest {
        val processor = processor()

        repeat(100) { processor.handle(fix(at = START + it * 1_000L, accuracy = 10f)) }

        // 处理器不持有 runner / coordinator，也不暴露任何注册接口：
        // 「被动监听不扰动主流」在结构上成立，而不是靠约定。
        assertFalse(
            "AC-14.7：被动处理器不得持有定位引擎（不得改变主流注册）",
            PassiveLocationProcessor::class.java.declaredFields.any { field ->
                field.type.simpleName.contains("Runner", ignoreCase = true) ||
                    field.type.simpleName.contains("Coordinator", ignoreCase = true)
            }
        )
    }

    /** AC-14.2：计数可重置（按窗口对账用）。 */
    @Test
    fun `计数可按窗口重置`() = runTest {
        val processor = processor()
        processor.handle(fix(at = START, accuracy = 10f))
        processor.handle(fix(at = START, accuracy = 40f))

        val window = processor.drainCounters(now = START + 60_000L, windowStart = START)

        assertEquals(2, window.callbackCount)
        assertEquals(1, window.acceptedCount)
        assertEquals(1, window.droppedCount)
        assertEquals(0, window.duplicateCount)
        assertEquals("对账缺口必须为 0", 0, window.unaccountedCount)
        assertEquals(0, processor.countersSnapshot().callbackCount)
    }

    /** AC-14.2：每个原因编码都有中文文案（未映射会显示「其他原因」）。 */
    @Test
    fun `被动丢弃原因都有中文文案`() = runTest {
        PassiveDropReasons.ALL.forEach { reason ->
            assertTrue(
                "被动丢弃原因 $reason 必须映射为中文文案",
                !PassiveDropReasons.label(reason).isNullOrBlank()
            )
        }
        assertEquals(null, PassiveDropReasons.label("some-unmapped-reason"))
    }

    /** 质量门与被动判定使用同一道门（D11：共用 30 米门槛，不设被动专属容差）。 */
    @Test
    fun `被动点共用主动流的门槛`() = runTest {
        val processor = processor()

        assertTrue("29.9 米必须收下", processor.handle(fix(at = START, accuracy = 29.9f)) != null)
        assertEquals("30.0 米必须丢弃", null, processor.handle(fix(at = START, accuracy = 30.0f)))

        val gate = LocationQualityGate()
        assertEquals(
            "D11：被动与主动使用同一个门槛常量",
            30f,
            LocationQualityGate.MAX_ACCURACY_METERS_EXCLUSIVE
        )
        assertTrue(gate.evaluate(fix(at = START, accuracy = 29.9f).toRawFix()) is QualityDecision.AcceptNow)
    }

    private fun fix(
        at: Long,
        accuracy: Float?,
        latitude: Double = 31.230416,
        provider: String = "gps"
    ) = PassiveFix(
        latitude = latitude,
        longitude = 121.473701,
        horizontalAccuracyMeters = accuracy,
        altitudeMeters = 10.0,
        provider = provider,
        recordedAtMillis = at
    )

    private fun PassiveFix.toRawFix() = RawLocationFix(
        latitude = latitude,
        longitude = longitude,
        horizontalAccuracyMeters = horizontalAccuracyMeters,
        altitudeMeters = altitudeMeters,
        provider = provider,
        recordedAtMillis = recordedAtMillis,
        policyMode = "PowerSavingNormal",
        scheduleLowFrequency = false,
        motionSignal = "Unknown"
    )

    private companion object {
        const val START = 1_700_000_000_000L
    }
}
