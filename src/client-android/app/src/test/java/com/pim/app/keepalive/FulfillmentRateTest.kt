package com.pim.app.keepalive

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * REQ-18 兑现台账与兑现率。
 *
 * 重点守 AC-18.3 的反面语义：**缺少实际时刻的记录不进分母**。
 * 若把设备关机的记录当「未兑现」，一次出差关机就能把兑现率砸到很低，
 * 而那是设备状态不是保活失效——口径错了会直接误导需求方判断保活是否有效。
 */
class FulfillmentRateTest {

    private fun rec(delayMinutes: Long?, outcome: String) = AlarmFulfillmentRecord(
        scheduledAtUtcMillis = 1_700_000_000_000L,
        actualAtUtcMillis = delayMinutes?.let { 1_700_000_000_000L + it * 60_000L },
        outcome = outcome
    )

    /** AC-18.1 / 基本口径：3 次执行、2 次按时 → 2/3。 */
    @Test
    fun `AC-18_1 兑现率为按时次数除以已执行次数`() {
        val result = FulfillmentRate.compute(
            listOf(
                rec(0, AlarmOutcomes.EXECUTED),
                rec(5, AlarmOutcomes.EXECUTED),
                rec(40, AlarmOutcomes.SUPPRESSED)
            )
        )
        assertEquals(2, result.fulfilled)
        assertEquals(3, result.considered)
        val rate = result.rate
        assertTrue(rate != null && rate > 0.66 && rate < 0.67)
    }

    /** AC-18.3：缺少实际时刻的记录不计入分母，且要能被单独数出来。 */
    @Test
    fun `AC-18_3 无实际时刻的记录不计入分母`() {
        val result = FulfillmentRate.compute(
            listOf(
                rec(0, AlarmOutcomes.EXECUTED),
                rec(null, AlarmOutcomes.NOT_EXECUTED),
                rec(null, AlarmOutcomes.SKIPPED_PAUSED)
            )
        )
        assertEquals("分母只应有 1 条（那次真的执行了）", 1, result.considered)
        assertEquals(2, result.excludedNoActualTime)
        assertEquals(1.0, result.rate!!, 0.0001)
    }

    /** AC-18.2：没有可判定记录时返回空态，**不是** 0%。 */
    @Test
    fun `AC-18_2 没有已执行记录时给空态而不是百分之零`() {
        val result = FulfillmentRate.compute(
            listOf(rec(null, AlarmOutcomes.NOT_EXECUTED), rec(null, AlarmOutcomes.SKIPPED_DISABLED))
        )
        assertNull(result.rate)
        assertTrue(FulfillmentRate.format(result).contains("无数据"))
        assertTrue("不得把无数据显示成 0%", !FulfillmentRate.format(result).contains("0%"))
    }

    /** AC-18.2：完全是 0 次按时（但确实执行了）时**要**显示 0%，与「无数据」区分开。 */
    @Test
    fun `AC-18_2 确实执行但都不按时显示百分之零`() {
        val result = FulfillmentRate.compute(listOf(rec(30, AlarmOutcomes.SUPPRESSED)))
        assertEquals(0.0, result.rate!!, 0.0001)
        assertTrue(FulfillmentRate.format(result).contains("0%"))
    }

    /** AC-18.3：口径说明必须写明「不计入分母」，页面上要能读到。 */
    @Test
    fun `AC-18_3 口径说明写明了分母排除规则`() {
        assertTrue(FulfillmentRate.DEFINITION.contains("不计入分母"))
        assertTrue(FulfillmentRate.DEFINITION.contains("15"))
    }

    /** 边界：恰好 15 分钟算按时（与 AC-17.1 同一条判定线，不能两处不一致）。 */
    @Test
    fun `边界 恰好十五分钟算按时`() {
        val result = FulfillmentRate.compute(listOf(rec(15, AlarmOutcomes.EXECUTED)))
        assertEquals(1, result.fulfilled)
        assertEquals(15, AlarmSuppressionPolicy.SUPPRESSION_THRESHOLD_MINUTES)
    }
}
