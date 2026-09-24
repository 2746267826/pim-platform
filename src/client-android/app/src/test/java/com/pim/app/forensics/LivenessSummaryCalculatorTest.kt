package com.pim.app.forensics

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * REQ-13 / REQ-8 的客户端口径（AC-13.1 / AC-13.2 / AC-7.4 / AC-8.2）。
 * 与服务端 `DeviceLivenessCalculator` 用同一套规则，因此这里也用注入时钟复算同一个结果。
 */
class LivenessSummaryCalculatorTest {

    private val hour = 3_600_000L
    private val minute = 60_000L
    private val day = 24 * hour

    private val rangeStart = 1_756_684_800_000L // 2026-09-01T00:00:00Z
    private val rangeEnd = rangeStart + 7 * day

    @Test
    fun `AC-8_2 without heartbeats the conclusion is no data and coverage is absent`() {
        val summary = LivenessSummaryCalculator.summarize(
            heartbeatTimestampsUtcMillis = emptyList(),
            rangeStartUtcMillis = rangeStart,
            rangeEndUtcMillis = rangeEnd
        )

        assertFalse(summary.hasData)
        assertEquals(LivenessRules.NO_DATA_CONCLUSION, summary.conclusion)
        assertNull(summary.coverageByHour)
        assertNull(summary.coverageByExpectedHeartbeat)
        assertNull(summary.lastHeartbeatAtUtcMillis)
        // 覆盖率不能显示成 0%（更不允许 100%），页面用 "—"。
        assertEquals("—", CoverageFormatting.format(summary.coverageByHour))
    }

    @Test
    fun `AC-13_1 both coverages are recomputable from the exposed numerator and denominator`() {
        val heartbeats = listOf(
            rangeStart + hour,
            rangeStart + hour + 30 * minute,
            rangeStart + 5 * hour,
        )

        val summary = LivenessSummaryCalculator.summarize(heartbeats, rangeStart, rangeEnd)

        assertEquals(7 * 24, summary.totalHours)
        assertEquals(2, summary.observedHours)
        assertEquals(2.0 / (7 * 24), summary.coverageByHour!!, 0.0001)

        val expected = Math.ceil((7 * 24 * 60).toDouble() / 15.0).toInt()
        assertEquals(expected, summary.expectedHeartbeats)
        assertEquals(3, summary.observedHeartbeats)
        assertEquals(3.0 / expected, summary.coverageByExpectedHeartbeat!!, 0.0001)
    }

    @Test
    fun `AC-13_2 the same silence length is classified the same at night and during the day`() {
        val night = 1_756_719_000_000L // 2026-09-01T23:30:00Z
        val day = 1_756_756_800_000L // 2026-09-02T10:00:00Z

        val nightSummary = LivenessSummaryCalculator.summarize(
            listOf(night, night + 30 * minute), night, night + 30 * minute
        )
        val daySummary = LivenessSummaryCalculator.summarize(
            listOf(day, day + 30 * minute), day, day + 30 * minute
        )

        assertEquals(SilenceSeverity.Warning, nightSummary.longestSilenceSeverity)
        assertEquals(SilenceSeverity.Warning, daySummary.longestSilenceSeverity)
        assertEquals(nightSummary.longestSilenceMinutes, daySummary.longestSilenceMinutes)
    }

    @Test
    fun `AC-7_4 gaps below thirty minutes are not marked, thirty minutes warns, one hour is critical`() {
        val base = rangeStart + 8 * hour

        assertTrue(LivenessSummaryCalculator.buildSilences(
            listOf(base, base + 29 * minute), base, base + 29 * minute
        ).isEmpty())
        assertEquals(
            SilenceSeverity.None,
            LivenessSummaryCalculator.classifySilence(29.9)
        )
        assertEquals(
            SilenceSeverity.Warning,
            LivenessSummaryCalculator.classifySilence(30.0)
        )
        assertEquals(
            SilenceSeverity.Critical,
            LivenessSummaryCalculator.classifySilence(60.0)
        )
    }

    @Test
    fun `trailing silence until the range end is counted`() {
        val summary = LivenessSummaryCalculator.summarize(
            listOf(rangeStart),
            rangeStart,
            rangeStart + (53.2 * 60).toLong() * minute
        )

        assertEquals(SilenceSeverity.Critical, summary.longestSilenceSeverity)
        assertEquals((53.2 * 60).toInt(), summary.longestSilenceMinutes)
    }

    @Test
    fun `heartbeats outside the range are ignored`() {
        val summary = LivenessSummaryCalculator.summarize(
            listOf(rangeStart - day, rangeEnd + minute),
            rangeStart,
            rangeEnd
        )

        assertFalse(summary.hasData)
    }

    @Test
    fun `coverage formatting shows numerator and denominator so it can be checked by hand`() {
        val summary = LivenessSummaryCalculator.summarize(
            listOf(rangeStart + hour),
            rangeStart,
            rangeStart + 4 * hour
        )

        assertEquals("25.0%（1/4 小时）", CoverageFormatting.formatByHour(summary))
        // 4 小时 = 240 分钟，应有心跳 = ceil(240 / 15) = 16 次；观测 1 次 → 1/16 = 6.25% → 6.3%
        assertEquals("6.3%（1/16 次）", CoverageFormatting.formatByExpectedHeartbeat(summary))
    }

    @Test
    fun `coverage is capped at one hundred percent`() {
        val heartbeats = (0 until 500).map { rangeStart + it * minute.toLong() }
        val summary = LivenessSummaryCalculator.summarize(heartbeats, rangeStart, rangeStart + 4 * hour)

        assertEquals(1.0, summary.coverageByExpectedHeartbeat!!, 0.0)
        assertEquals("100.0%", CoverageFormatting.format(summary.coverageByExpectedHeartbeat))
    }
}
