package com.pim.app.schedule

import com.pim.app.location.policy.ScheduleWindow
import com.pim.core.models.EventResponse
import com.pim.core.network.ApiService
import java.time.Instant
import javax.inject.Inject

object ScheduleWindowSelector {
    fun current(windows: List<ScheduleWindow>, nowMillis: Long): ScheduleWindow? {
        return windows.firstOrNull { window ->
            nowMillis >= window.startsAtMillis &&
                nowMillis < window.endsAtMillis
        }
    }

    fun upcoming(
        windows: List<ScheduleWindow>,
        nowMillis: Long,
        limit: Int = 10
    ): List<ScheduleWindow> {
        return windows
            .filter { it.startsAtMillis > nowMillis }
            .sortedBy { it.startsAtMillis }
            .take(limit)
    }
}

class ScheduleWindowRepository @Inject constructor(
    private val apiService: ApiService
) {
    suspend fun loadWindows(startMillis: Long, endMillis: Long): List<ScheduleWindow> {
        val response = apiService.getEvents(
            start = Instant.ofEpochMilli(startMillis).toString(),
            end = Instant.ofEpochMilli(endMillis).toString()
        )
        if (response.code != 0) {
            error(response.message.ifBlank { "加载日程失败" })
        }
        return mapEvents(response.data.orEmpty())
    }

    suspend fun currentWindow(windows: List<ScheduleWindow>, nowMillis: Long): ScheduleWindow? {
        return ScheduleWindowSelector.current(windows, nowMillis)
    }

    suspend fun upcomingWindows(windows: List<ScheduleWindow>, nowMillis: Long): List<ScheduleWindow> {
        return ScheduleWindowSelector.upcoming(windows, nowMillis)
    }

    companion object {
        fun mapEvents(events: List<EventResponse>): List<ScheduleWindow> {
            return events.mapNotNull { event ->
                val location = event.location?.trim().orEmpty()
                val startsAt = event.dtStart.toEpochMillisOrNull() ?: return@mapNotNull null
                val endsAt = event.dtEnd.toEpochMillisOrNull() ?: return@mapNotNull null
                ScheduleWindow(
                    id = event.id,
                    title = event.title,
                    locationText = location,
                    startsAtMillis = startsAt,
                    endsAtMillis = endsAt
                )
            }
        }

        private fun String.toEpochMillisOrNull(): Long? {
            return runCatching { Instant.parse(this).toEpochMilli() }.getOrNull()
        }
    }
}
