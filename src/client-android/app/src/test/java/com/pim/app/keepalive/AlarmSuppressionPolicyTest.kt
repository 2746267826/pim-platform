package com.pim.app.keepalive

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * REQ-17 被压制判定与自动降频的参数边界（工单 §9.1 P2 需求方已确认，不得自行改数）。
 *
 * 每条用例后标注它守的是哪条 AC；反面用例（AC-17.4 / AC-17.5）与正向同等重要——
 * 工单明确要求「因暂停、设备关机导致的未执行不计为被压制」「调用失败不触发降频」。
 */
class AlarmSuppressionPolicyTest {

    private fun record(scheduledMinutesAgo: Long, delayMinutes: Long?): AlarmFulfillmentRecord {
        val scheduled = 1_700_000_000_000L - scheduledMinutesAgo * 60_000L
        return AlarmFulfillmentRecord(
            scheduledAtUtcMillis = scheduled,
            actualAtUtcMillis = delayMinutes?.let { scheduled + it * 60_000L },
            outcome = when {
                delayMinutes == null -> AlarmOutcomes.NOT_EXECUTED
                delayMinutes > AlarmSuppressionPolicy.SUPPRESSION_THRESHOLD_MINUTES ->
                    AlarmOutcomes.SUPPRESSED
                else -> AlarmOutcomes.EXECUTED
            }
        )
    }

    /**
     * AC-17.1：延迟 >15 分钟的样本被标记为「被压制」，≤15 分钟的不被标记。
     *
     * 断言直接打在阈值判定函数上（执行链与降频策略都用它），而不是打在预先造好的
     * `outcome` 字段上——否则把阈值改错、甚至把判定函数改坏，这些用例都还是会绿。
     */
    @Test
    fun `AC-17_1 恰好 15 分钟不算被压制，超过才算`() {
        val fifteen = 15 * 60_000L
        assertFalse("15 分钟是阈值本身，判定线是「>15」", AlarmSuppressionPolicy.isSuppressedDelay(fifteen))
        assertTrue("16 分钟必须算被压制", AlarmSuppressionPolicy.isSuppressedDelay(fifteen + 1))
        assertFalse(AlarmSuppressionPolicy.isSuppressedDelay(0L))
        assertTrue(AlarmSuppressionPolicy.isOnTimeDelay(fifteen))
        assertFalse(AlarmSuppressionPolicy.isOnTimeDelay(fifteen + 1))
    }

    /** AC-17.1：判定线是 15 分钟这个数本身也必须守住（需求方确认过的参数）。 */
    @Test
    fun `AC-17_1 阈值就是 15 分钟`() {
        assertEquals(15, AlarmSuppressionPolicy.SUPPRESSION_THRESHOLD_MINUTES)
        assertEquals(15 * 60_000L, AlarmSuppressionPolicy.SUPPRESSION_THRESHOLD_MILLIS)
    }

    /** AC-17.2：连续 3 次被压制后，下一个周期间隔为配置值 ×2 且不超过 120 分钟。 */
    @Test
    fun `AC-17_2 连续三次被压制后间隔翻倍`() {
        val suppressed = listOf(record(60, 20), record(30, 30), record(1, 25))
        val effective = AlarmSuppressionPolicy.resolveEffectiveMinutes(
            configuredMinutes = 30,
            effectiveMinutes = 30,
            recentRecords = suppressed
        )
        assertEquals(60, effective)
    }

    /** AC-17.2：降频上限 120 分钟——不能无限翻倍。 */
    @Test
    fun `AC-17_2 降频封顶 120 分钟`() {
        val suppressed = listOf(record(60, 20), record(30, 30), record(1, 25))
        val effective = AlarmSuppressionPolicy.resolveEffectiveMinutes(
            configuredMinutes = 120,
            effectiveMinutes = 120,
            recentRecords = suppressed
        )
        assertEquals(AlarmSuppressionPolicy.MAX_BACKOFF_INTERVAL_MINUTES, effective)
        assertEquals(120, effective)
    }

    /** AC-17.2：两次被压制还不到降频门槛（必须连续 3 次）。 */
    @Test
    fun `AC-17_2 连续两次被压制不降频`() {
        val suppressed = listOf(record(30, 20), record(1, 25))
        val effective = AlarmSuppressionPolicy.resolveEffectiveMinutes(30, 30, suppressed)
        assertEquals(30, effective)
        assertFalse(AlarmSuppressionPolicy.isBackedOff(30, effective))
    }

    /** AC-17.3：连续 2 次延迟 ≤15 分钟后回到配置值，提示消失。 */
    @Test
    fun `AC-17_3 连续两次按时候回到配置值`() {
        val onTime = listOf(record(30, 0), record(1, 5))
        val effective = AlarmSuppressionPolicy.resolveEffectiveMinutes(
            configuredMinutes = 30,
            effectiveMinutes = 60,
            recentRecords = onTime
        )
        assertEquals(30, effective)
        assertFalse("回到配置值后状态页不应再显示降频提示", AlarmSuppressionPolicy.isBackedOff(30, effective))
    }

    /** AC-17.3 反面：只有 1 次按时不足以复位（必须连续 2 次）。 */
    @Test
    fun `AC-17_3 仅一次按时不足以复位`() {
        val mixed = listOf(record(30, 0))
        val effective = AlarmSuppressionPolicy.resolveEffectiveMinutes(30, 60, mixed)
        assertEquals(60, effective)
    }

    /**
     * AC-17.4（反面）：因用户暂停、设备关机导致的未执行**不计为**被压制。
     * 这条守的是「暂停久了反而把间隔越推越长」这种荒唐结果。
     */
    @Test
    fun `AC-17_4 因暂停或关机未执行不计为被压制`() {
        val notExecuted = listOf(
            AlarmFulfillmentRecord(3000, null, AlarmOutcomes.SKIPPED_PAUSED),
            AlarmFulfillmentRecord(2000, null, AlarmOutcomes.NOT_EXECUTED),
            AlarmFulfillmentRecord(1000, null, AlarmOutcomes.SKIPPED_MANUAL_SESSION),
            AlarmFulfillmentRecord(500, null, AlarmOutcomes.SKIPPED_DISABLED)
        )
        val effective = AlarmSuppressionPolicy.resolveEffectiveMinutes(30, 30, notExecuted)

        assertEquals("未执行不构成被压制证据，间隔必须保持配置值", 30, effective)
        assertFalse(notExecuted.any { it.isSuppressedEvidence })
        assertFalse(notExecuted.any { it.isOnTimeEvidence })
    }

    /**
     * AC-17.5（反面）：调用服务被系统拒绝（拉起失败）**不**触发降频，只记台账与红点，
     * 并在下一个周期重试。
     */
    @Test
    fun `AC-17_5 拉起失败不触发降频`() {
        val pullFailures = listOf(
            record(90, null).copy(outcome = AlarmOutcomes.PULL_FAILED),
            record(60, null).copy(outcome = AlarmOutcomes.PULL_FAILED),
            record(30, null).copy(outcome = AlarmOutcomes.PULL_FAILED),
            record(1, null).copy(outcome = AlarmOutcomes.PULL_FAILED)
        )
        val effective = AlarmSuppressionPolicy.resolveEffectiveMinutes(30, 30, pullFailures)

        assertEquals("拉起失败必须保持原间隔并在下轮重试，不得降频", 30, effective)
        assertFalse(AlarmSuppressionPolicy.isBackedOff(30, effective))
    }

    /** AC-17.2 复位优先级：新近的 2 次按时应能盖过更早的 3 次被压制。 */
    @Test
    fun `AC-17_3 新近按时记录优先于更早的被压制记录`() {
        val records = listOf(
            record(10, 0),
            record(5, 3),
            record(60, 30),
            record(50, 25),
            record(40, 20)
        )
        val effective = AlarmSuppressionPolicy.resolveEffectiveMinutes(30, 60, records)
        assertEquals(30, effective)
    }

    /** AC-15.1：节奏必须夹在 10-120 分钟，默认 30。 */
    @Test
    fun `AC-15_1 节奏夹在 10 到 120 分钟且默认 30`() {
        assertEquals(30, AlarmSuppressionPolicy.DEFAULT_INTERVAL_MINUTES)
        assertEquals(10, AlarmSuppressionPolicy.clampConfiguredMinutes(1))
        assertEquals(10, AlarmSuppressionPolicy.clampConfiguredMinutes(10))
        assertEquals(45, AlarmSuppressionPolicy.clampConfiguredMinutes(45))
        assertEquals(120, AlarmSuppressionPolicy.clampConfiguredMinutes(120))
        assertEquals(120, AlarmSuppressionPolicy.clampConfiguredMinutes(999))
    }

    /** AC-18.1：预定、实际、延迟三者算术一致。 */
    @Test
    fun `AC-18_1 延迟等于实际减预定`() {
        val record = record(scheduledMinutesAgo = 10, delayMinutes = 7)
        assertEquals(7 * 60_000L, record.delayMillis)
    }

    /** AC-18.3：缺少实际时刻的记录 delay 为 null，不得算成 0（那样会污染兑现率与判定）。 */
    @Test
    fun `AC-18_3 无实际时刻的记录没有延迟值`() {
        val record = AlarmFulfillmentRecord(1000L, null, AlarmOutcomes.NOT_EXECUTED)
        assertEquals(null, record.delayMillis)
        assertFalse(record.isOnTimeEvidence)
        assertFalse(record.isSuppressedEvidence)
    }

    /** AC-26.1 / AC-26.2：降频提示为简体中文，且带具体数值。 */
    @Test
    fun `AC-26_1 降频提示为中文并含具体分钟数`() {
        val hint = AlarmSuppressionPolicy.backoffHint(configuredMinutes = 30, effectiveMinutes = 60)
        assertTrue(hint.contains("30"))
        assertTrue(hint.contains("60"))
        assertTrue("不得出现英文占位串", hint.none { it.code in 'a'.code..'z'.code })
    }
}
