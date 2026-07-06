package com.pim.app.mobile.usage

import android.app.usage.UsageEvents
import android.app.usage.UsageStats
import android.app.usage.UsageStatsManager
import android.content.Context
import android.os.Build
import com.pim.app.data.MobileUsageEventEntity
import com.pim.app.data.MobileUsageSummaryEntity
import dagger.hilt.android.qualifiers.ApplicationContext
import org.json.JSONObject
import javax.inject.Inject
import javax.inject.Singleton

data class UsageEventCollection(
    val events: List<MobileUsageEventEntity>,
    val summaries: List<MobileUsageSummaryEntity>,
    val source: String,
    val sourceWindowStartUtc: Long,
    val sourceWindowEndUtc: Long,
    val collectedAtUtc: Long
)

@Singleton
class UsageEventCollector @Inject constructor(
    @ApplicationContext private val context: Context,
    private val usageAccessChecker: UsageAccessChecker
) {
    fun collectUsage(
        windowStartUtc: Long,
        windowEndUtc: Long,
        collectedAtUtc: Long = System.currentTimeMillis()
    ): UsageEventCollection {
        require(windowEndUtc > windowStartUtc) {
            "windowEndUtc must be greater than windowStartUtc"
        }

        val usageStatsManager = context.getSystemService(Context.USAGE_STATS_SERVICE) as? UsageStatsManager
            ?: return emptyCollection(windowStartUtc, windowEndUtc, collectedAtUtc, SOURCE_UNAVAILABLE)

        if (!usageAccessChecker.hasUsageAccess()) {
            return emptyCollection(windowStartUtc, windowEndUtc, collectedAtUtc, SOURCE_NO_ACCESS)
        }

        val usageEvents = queryUsageEvents(
            usageStatsManager = usageStatsManager,
            windowStartUtc = windowStartUtc,
            windowEndUtc = windowEndUtc,
            collectedAtUtc = collectedAtUtc
        )

        if (usageEvents.isNotEmpty()) {
            return UsageEventCollection(
                events = usageEvents,
                summaries = emptyList(),
                source = SOURCE_USAGE_EVENTS,
                sourceWindowStartUtc = windowStartUtc,
                sourceWindowEndUtc = windowEndUtc,
                collectedAtUtc = collectedAtUtc
            )
        }

        val summaries = queryUsageStatsFallback(
            usageStatsManager = usageStatsManager,
            windowStartUtc = windowStartUtc,
            windowEndUtc = windowEndUtc,
            collectedAtUtc = collectedAtUtc
        )

        return UsageEventCollection(
            events = emptyList(),
            summaries = summaries,
            source = SOURCE_USAGE_STATS_FALLBACK,
            sourceWindowStartUtc = windowStartUtc,
            sourceWindowEndUtc = windowEndUtc,
            collectedAtUtc = collectedAtUtc
        )
    }

    private fun queryUsageEvents(
        usageStatsManager: UsageStatsManager,
        windowStartUtc: Long,
        windowEndUtc: Long,
        collectedAtUtc: Long
    ): List<MobileUsageEventEntity> {
        val events = try {
            usageStatsManager.queryEvents(windowStartUtc, windowEndUtc)
        } catch (_: SecurityException) {
            return emptyList()
        }

        val rows = mutableListOf<MobileUsageEventEntity>()
        while (events.hasNextEvent()) {
            val event = UsageEvents.Event()
            events.getNextEvent(event)

            val packageName = event.packageName ?: continue
            val eventType = event.eventType
            val eventTimeUtc = event.timeStamp

            rows += MobileUsageEventEntity(
                packageName = packageName,
                className = event.className,
                eventType = eventType,
                eventName = eventName(eventType),
                eventTimeUtc = eventTimeUtc,
                source = SOURCE_USAGE_EVENTS,
                sourceWindowStartUtc = windowStartUtc,
                sourceWindowEndUtc = windowEndUtc,
                collectedAtUtc = collectedAtUtc,
                rawJson = eventRawJson(
                    event = event,
                    source = SOURCE_USAGE_EVENTS,
                    windowStartUtc = windowStartUtc,
                    windowEndUtc = windowEndUtc,
                    collectedAtUtc = collectedAtUtc
                )
            )
        }

        return rows
    }

    private fun queryUsageStatsFallback(
        usageStatsManager: UsageStatsManager,
        windowStartUtc: Long,
        windowEndUtc: Long,
        collectedAtUtc: Long
    ): List<MobileUsageSummaryEntity> {
        val interval = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            UsageStatsManager.INTERVAL_BEST
        } else {
            UsageStatsManager.INTERVAL_DAILY
        }

        val stats = try {
            usageStatsManager.queryUsageStats(interval, windowStartUtc, windowEndUtc)
        } catch (_: SecurityException) {
            return emptyList()
        }.orEmpty()

        return stats
            .filter { it.packageName != null }
            .map { usageStats ->
                MobileUsageSummaryEntity(
                    packageName = usageStats.packageName,
                    windowStartUtc = windowStartUtc,
                    windowEndUtc = windowEndUtc,
                    totalTimeForegroundMs = usageStats.totalTimeInForeground,
                    lastTimeUsedUtc = usageStats.lastTimeUsed,
                    firstTimeStampUtc = usageStats.firstTimeStamp,
                    lastTimeStampUtc = usageStats.lastTimeStamp,
                    source = SOURCE_USAGE_STATS_FALLBACK,
                    sourceWindowStartUtc = windowStartUtc,
                    sourceWindowEndUtc = windowEndUtc,
                    collectedAtUtc = collectedAtUtc,
                    rawJson = usageStatsRawJson(
                        usageStats = usageStats,
                        source = SOURCE_USAGE_STATS_FALLBACK,
                        windowStartUtc = windowStartUtc,
                        windowEndUtc = windowEndUtc,
                        collectedAtUtc = collectedAtUtc
                    )
                )
            }
    }

    private fun emptyCollection(
        windowStartUtc: Long,
        windowEndUtc: Long,
        collectedAtUtc: Long,
        source: String
    ): UsageEventCollection {
        return UsageEventCollection(
            events = emptyList(),
            summaries = emptyList(),
            source = source,
            sourceWindowStartUtc = windowStartUtc,
            sourceWindowEndUtc = windowEndUtc,
            collectedAtUtc = collectedAtUtc
        )
    }

    private fun eventRawJson(
        event: UsageEvents.Event,
        source: String,
        windowStartUtc: Long,
        windowEndUtc: Long,
        collectedAtUtc: Long
    ): String {
        return JSONObject()
            .put("source", source)
            .put("sourceWindowStartUtc", windowStartUtc)
            .put("sourceWindowEndUtc", windowEndUtc)
            .put("collectedAtUtc", collectedAtUtc)
            .putNullable("packageName", event.packageName)
            .putNullable("className", event.className)
            .put("eventType", event.eventType)
            .put("eventName", eventName(event.eventType))
            .put("eventTimeUtc", event.timeStamp)
            .toString()
    }

    private fun usageStatsRawJson(
        usageStats: UsageStats,
        source: String,
        windowStartUtc: Long,
        windowEndUtc: Long,
        collectedAtUtc: Long
    ): String {
        return JSONObject()
            .put("source", source)
            .put("sourceWindowStartUtc", windowStartUtc)
            .put("sourceWindowEndUtc", windowEndUtc)
            .put("collectedAtUtc", collectedAtUtc)
            .put("packageName", usageStats.packageName)
            .put("firstTimeStampUtc", usageStats.firstTimeStamp)
            .put("lastTimeStampUtc", usageStats.lastTimeStamp)
            .put("lastTimeUsedUtc", usageStats.lastTimeUsed)
            .put("totalTimeForegroundMs", usageStats.totalTimeInForeground)
            .toString()
    }

    private fun eventName(eventType: Int): String {
        return when (eventType) {
            UsageEvents.Event.MOVE_TO_FOREGROUND -> "MOVE_TO_FOREGROUND"
            UsageEvents.Event.MOVE_TO_BACKGROUND -> "MOVE_TO_BACKGROUND"
            UsageEvents.Event.CONFIGURATION_CHANGE -> "CONFIGURATION_CHANGE"
            UsageEvents.Event.USER_INTERACTION -> "USER_INTERACTION"
            UsageEvents.Event.SHORTCUT_INVOCATION -> "SHORTCUT_INVOCATION"
            UsageEvents.Event.STANDBY_BUCKET_CHANGED -> "STANDBY_BUCKET_CHANGED"
            else -> "UNKNOWN_$eventType"
        }
    }

    companion object {
        const val SOURCE_USAGE_EVENTS = "usage_events"
        const val SOURCE_USAGE_STATS_FALLBACK = "usage_stats_fallback"
        const val SOURCE_NO_ACCESS = "usage_access_unavailable"
        const val SOURCE_UNAVAILABLE = "usage_stats_manager_unavailable"
    }
}

private fun JSONObject.putNullable(name: String, value: Any?): JSONObject {
    return put(name, value ?: JSONObject.NULL)
}
