package com.pim.app.ui.schedule

import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZoneOffset
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SchedulePolicyScreenFormattingTest {

    @Test
    fun sameDayEventRangeIncludesDateOnStartOnly() {
        val zoneId = ZoneOffset.UTC
        val start = dateTimeMillis(2026, 7, 18, 14, 0, zoneId)
        val end = dateTimeMillis(2026, 7, 18, 15, 0, zoneId)

        val result = buildEventTimeRange(start, end, zoneId, includeDate = true)

        assertTrue("Same-day must include date on start: $result", result.startsWith("07-18"))
        assertTrue("Same-day must have separator", result.contains(" - "))
        assertFalse("Same-day must NOT repeat date on end: $result",
            result.matches(Regex("""07-18 \d{2}:\d{2} - 07-18 \d{2}:\d{2}""")))
    }

    @Test
    fun crossMidnightEventRangeShowsBothDates() {
        val zoneId = ZoneOffset.UTC
        val start = dateTimeMillis(2026, 7, 18, 22, 0, zoneId)
        val end = dateTimeMillis(2026, 7, 19, 1, 0, zoneId)

        val result = buildEventTimeRange(start, end, zoneId, includeDate = true)

        assertTrue("Cross-midnight must show start date", result.contains("07-18"))
        assertTrue("Cross-midnight must show end date", result.contains("07-19"))
    }

    @Test
    fun groupedEventRangeOmitsDate() {
        val zoneId = ZoneOffset.UTC
        val start = dateTimeMillis(2026, 7, 18, 14, 0, zoneId)
        val end = dateTimeMillis(2026, 7, 18, 15, 0, zoneId)

        val result = buildEventTimeRange(start, end, zoneId, includeDate = false)

        assertFalse("Grouped must not contain date: $result", result.contains("07-18"))
        assertTrue("Grouped must contain start time", result.contains("14:00"))
        assertTrue("Grouped must contain end time", result.contains("15:00"))
    }

    @Test
    fun thresholdWholeValueShowsNoDecimal() {
        assertEquals("100 m", formatThreshold(100.0))
        assertEquals("0 m", formatThreshold(0.0))
        assertEquals("250 m", formatThreshold(250.0))
    }

    @Test
    fun thresholdNonWholeValueShowsOneDecimal() {
        assertEquals("100.5 m", formatThreshold(100.5))
        assertEquals("99.9 m", formatThreshold(99.9))
        assertEquals("150.3 m", formatThreshold(150.3))
    }

    @Test
    fun intervalMinutesAndSeconds() {
        assertEquals("5 分钟", formatInterval(300_000L))
        assertEquals("1 分钟", formatInterval(60_000L))
        assertEquals("30 秒", formatInterval(30_000L))
        assertEquals("暂无", formatInterval(0L))
        assertEquals("1分30秒", formatInterval(90_000L))
        assertEquals("15 分钟", formatInterval(900_000L))
    }

    @Test
    fun formatEventTimeWithDateIncludesDate() {
        val zoneId = ZoneOffset.UTC
        val millis = dateTimeMillis(2026, 7, 18, 14, 5, zoneId)
        assertEquals("07-18 14:05", formatEventTime(millis, zoneId, withDate = true))
    }

    @Test
    fun formatEventTimeWithoutDateOmitsDate() {
        val zoneId = ZoneOffset.UTC
        val millis = dateTimeMillis(2026, 7, 18, 14, 5, zoneId)
        assertEquals("14:05", formatEventTime(millis, zoneId, withDate = false))
    }

    private fun dateTimeMillis(year: Int, month: Int, day: Int, hour: Int, minute: Int, zoneId: ZoneId): Long {
        return LocalDateTime.of(year, month, day, hour, minute).atZone(zoneId).toInstant().toEpochMilli()
    }
}
