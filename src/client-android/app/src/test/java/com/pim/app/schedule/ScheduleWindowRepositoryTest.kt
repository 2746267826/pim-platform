package com.pim.app.schedule

import com.pim.app.location.policy.ScheduleWindow
import com.pim.core.models.EventResponse
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ScheduleWindowRepositoryTest {
    @Test
    fun currentWindowUsesTimeOnlyNotLocationText() {
        val now = 10_000L
        val windows = listOf(
            ScheduleWindow("1", "无地点会议", "", 9_000L, 11_000L),
            ScheduleWindow("2", "办公室", "上海市黄浦区", 9_000L, 11_000L)
        )

        assertEquals("1", ScheduleWindowSelector.current(windows, now)?.id)
        assertNull(ScheduleWindowSelector.current(windows, 12_000L))
    }

    @Test
    fun upcomingReturnsAllFutureWindowsIncludingBlankLocation() {
        val windows = listOf(
            ScheduleWindow("past", "过去", "上海", 1_000L, 2_000L),
            ScheduleWindow("blank", "无地点", "", 12_000L, 13_000L),
            ScheduleWindow("future", "外出", "虹桥", 14_000L, 15_000L)
        )

        assertEquals(listOf("blank", "future"), ScheduleWindowSelector.upcoming(windows, nowMillis = 10_000L).map { it.id })
    }

    @Test
    fun mapsApiEventsPreservesEventsWithoutLocation() {
        val windows = ScheduleWindowRepository.mapEvents(
            listOf(
                EventResponse(
                    id = "1",
                    title = "办公室",
                    location = "上海市黄浦区",
                    dtStart = "2026-07-08T01:00:00Z",
                    dtEnd = "2026-07-08T02:00:00Z"
                ),
                EventResponse(
                    id = "2",
                    title = "无地点",
                    location = "",
                    dtStart = "2026-07-08T03:00:00Z",
                    dtEnd = "2026-07-08T04:00:00Z"
                )
            )
        )

        assertEquals(2, windows.size)
        assertEquals("1", windows[0].id)
        assertEquals("2", windows[1].id)
        assertEquals("", windows[1].locationText)
    }
}
