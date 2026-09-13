package com.pim.app.mobile.usage

import android.app.usage.UsageStats
import android.app.usage.UsageStatsManager
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.pim.app.TestPimApp
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowUsageStatsManager

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = TestPimApp::class)
class UsageEventCollectorTest {

    private lateinit var context: Context
    private lateinit var usageStatsManager: UsageStatsManager
    private lateinit var shadowUsageStatsManager: ShadowUsageStatsManager
    private lateinit var collector: UsageEventCollector

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        usageStatsManager = context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
        shadowUsageStatsManager = Shadows.shadowOf(usageStatsManager)
        val usageAccessChecker = UsageAccessChecker(context)
        collector = UsageEventCollector(context, usageAccessChecker)
    }

    private fun createUsageStats(
        packageName: String,
        totalTimeInForeground: Long,
        firstTimeStamp: Long,
        lastTimeStamp: Long,
        lastTimeUsed: Long
    ): UsageStats {
        val constructor = UsageStats::class.java.getDeclaredConstructor()
        constructor.isAccessible = true
        val stats = constructor.newInstance()
        setField(stats, "mPackageName", packageName)
        setField(stats, "mTotalTimeInForeground", totalTimeInForeground)
        setField(stats, "mBeginTimeStamp", firstTimeStamp)
        setField(stats, "mEndTimeStamp", lastTimeStamp)
        setField(stats, "mLastTimeUsed", lastTimeUsed)
        return stats
    }

    private fun setField(target: Any, fieldName: String, value: Any) {
        val field = target.javaClass.getDeclaredField(fieldName)
        field.isAccessible = true
        field.set(target, value)
    }

    @Test
    fun queryUsageStatsFallbackClipsTotalTimeToWindowDuration() {
        val now = System.currentTimeMillis()
        val windowDurationMs = 15 * 60 * 1000L // 15 minutes = 900,000 ms
        val windowStartUtc = now - 30 * 60 * 1000L
        val windowEndUtc = windowStartUtc + windowDurationMs
        val bucketCumulativeForegroundMs = 3_572_139L // ~59.5 minutes (exceeds window duration)

        val stats = createUsageStats(
            packageName = "com.ss.android.ugc.aweme",
            totalTimeInForeground = bucketCumulativeForegroundMs,
            firstTimeStamp = now - 2 * 60 * 60 * 1000L,
            lastTimeStamp = now,
            lastTimeUsed = windowStartUtc + 5_000L
        )

        shadowUsageStatsManager.addUsageStats(UsageStatsManager.INTERVAL_DAILY, stats)
        shadowUsageStatsManager.addUsageStats(UsageStatsManager.INTERVAL_BEST, stats)

        val result = collector.collectUsage(windowStartUtc, windowEndUtc)
        assertEquals(UsageEventCollector.SOURCE_USAGE_STATS_FALLBACK, result.source)
        assertEquals(1, result.summaries.size)

        val summary = result.summaries.first()
        assertEquals("com.ss.android.ugc.aweme", summary.packageName)
        assertEquals(windowStartUtc, summary.windowStartUtc)
        assertEquals(windowEndUtc, summary.windowEndUtc)

        // Clipped to window duration
        assertEquals(windowDurationMs, summary.totalTimeForegroundMs)

        // Raw json still preserves original bucket cumulative value for auditability
        val rawJson = JSONObject(summary.rawJson)
        assertEquals(bucketCumulativeForegroundMs, rawJson.getLong("totalTimeForegroundMs"))
    }

    @Test
    fun queryUsageStatsFallbackPreservesDurationWhenWithinWindow() {
        val now = System.currentTimeMillis()
        val windowDurationMs = 15 * 60 * 1000L // 15 minutes = 900,000 ms
        val windowStartUtc = now - 30 * 60 * 1000L
        val windowEndUtc = windowStartUtc + windowDurationMs
        val withinWindowForegroundMs = 300_000L // 5 minutes (within window)

        val stats = createUsageStats(
            packageName = "com.ss.android.ugc.aweme",
            totalTimeInForeground = withinWindowForegroundMs,
            firstTimeStamp = now - 2 * 60 * 60 * 1000L,
            lastTimeStamp = now,
            lastTimeUsed = windowStartUtc + 5_000L
        )

        shadowUsageStatsManager.addUsageStats(UsageStatsManager.INTERVAL_DAILY, stats)
        shadowUsageStatsManager.addUsageStats(UsageStatsManager.INTERVAL_BEST, stats)

        val result = collector.collectUsage(windowStartUtc, windowEndUtc)
        assertEquals(UsageEventCollector.SOURCE_USAGE_STATS_FALLBACK, result.source)
        val summary = result.summaries.first()
        assertEquals(withinWindowForegroundMs, summary.totalTimeForegroundMs)
    }

    @Test
    fun queryUsageStatsFallbackClipsToEightHoursWhenWindowIsLonger() {
        val now = System.currentTimeMillis()
        val windowDurationMs = 10L * 60 * 60 * 1000L // 10 hours
        val windowStartUtc = now - 12 * 60 * 60 * 1000L
        val windowEndUtc = windowStartUtc + windowDurationMs
        val nineHoursForegroundMs = 9L * 60 * 60 * 1000L

        val stats = createUsageStats(
            packageName = "com.ss.android.ugc.aweme",
            totalTimeInForeground = nineHoursForegroundMs,
            firstTimeStamp = now - 24 * 60 * 60 * 1000L,
            lastTimeStamp = now,
            lastTimeUsed = windowStartUtc + 5_000L
        )

        shadowUsageStatsManager.addUsageStats(UsageStatsManager.INTERVAL_DAILY, stats)
        shadowUsageStatsManager.addUsageStats(UsageStatsManager.INTERVAL_BEST, stats)

        val result = collector.collectUsage(windowStartUtc, windowEndUtc)
        assertEquals(UsageEventCollector.SOURCE_USAGE_STATS_FALLBACK, result.source)
        val summary = result.summaries.first()
        assertEquals(UsageEventCollector.MAX_USAGE_DURATION_MS, summary.totalTimeForegroundMs)
    }
}
