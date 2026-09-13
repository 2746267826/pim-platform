package com.pim.app.mobile.usage

import android.app.Application
import android.app.usage.UsageStats
import android.app.usage.UsageStatsManager
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.pim.app.TestPimApp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = TestPimApp::class)
class UsageEventCollectorTest {

    private lateinit var context: Context
    private lateinit var usageStatsManager: UsageStatsManager
    private lateinit var usageAccessChecker: UsageAccessChecker
    private lateinit var collector: UsageEventCollector

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext<Application>()
        usageStatsManager = context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
        usageAccessChecker = UsageAccessChecker(context)
        collector = UsageEventCollector(context, usageAccessChecker)
    }

    @Test
    fun collectUsageFallbackFiltersOutZeroForegroundDurationApps() {
        val now = System.currentTimeMillis()
        val windowStart = now - 2 * 60 * 60 * 1000L
        val windowEnd = now

        val activeApp = createUsageStats("com.example.active", 60_000L, windowStart + 1000, windowStart, windowEnd)
        val zeroApp = createUsageStats("com.example.zero", 0L, windowStart + 1000, windowStart, windowEnd)
        val negativeApp = createUsageStats("com.example.negative", -500L, windowStart + 1000, windowStart, windowEnd)
        val blankApp = createUsageStats("   ", 30_000L, windowStart + 1000, windowStart, windowEnd)

        val shadowManager = shadowOf(usageStatsManager)
        shadowManager.addUsageStats(UsageStatsManager.INTERVAL_BEST, activeApp)
        shadowManager.addUsageStats(UsageStatsManager.INTERVAL_BEST, zeroApp)
        shadowManager.addUsageStats(UsageStatsManager.INTERVAL_BEST, negativeApp)
        shadowManager.addUsageStats(UsageStatsManager.INTERVAL_BEST, blankApp)
        shadowManager.addUsageStats(UsageStatsManager.INTERVAL_DAILY, activeApp)

        val result = collector.collectUsage(windowStart, windowEnd, now)

        assertEquals(UsageEventCollector.SOURCE_USAGE_STATS_FALLBACK, result.source)
        assertEquals(1, result.summaries.size)
        val summary = result.summaries.first()
        assertEquals("com.example.active", summary.packageName)
        assertEquals(60_000L, summary.totalTimeForegroundMs)
    }

    @Test
    fun collectUsageFallbackReturnsEmptyWhenAllAppsHaveZeroForegroundDuration() {
        val now = System.currentTimeMillis()
        val windowStart = now - 2 * 60 * 60 * 1000L
        val windowEnd = now

        val zeroApp1 = createUsageStats("com.example.zero1", 0L, windowStart, windowStart, windowEnd)
        val zeroApp2 = createUsageStats("com.example.zero2", 0L, windowStart, windowStart, windowEnd)

        val shadowManager = shadowOf(usageStatsManager)
        shadowManager.addUsageStats(UsageStatsManager.INTERVAL_BEST, zeroApp1)
        shadowManager.addUsageStats(UsageStatsManager.INTERVAL_BEST, zeroApp2)
        shadowManager.addUsageStats(UsageStatsManager.INTERVAL_DAILY, zeroApp1)

        val result = collector.collectUsage(windowStart, windowEnd, now)

        assertEquals(UsageEventCollector.SOURCE_USAGE_STATS_FALLBACK, result.source)
        assertTrue(result.summaries.isEmpty())
    }

    private fun createUsageStats(
        packageName: String,
        totalTimeInForeground: Long,
        lastTimeUsed: Long,
        firstTimeStamp: Long,
        lastTimeStamp: Long
    ): UsageStats {
        val constructor = UsageStats::class.java.getDeclaredConstructor().apply {
            isAccessible = true
        }
        val stats = constructor.newInstance()
        setField(stats, "mPackageName", packageName)
        setField(stats, "mTotalTimeInForeground", totalTimeInForeground)
        setField(stats, "mLastTimeUsed", lastTimeUsed)
        setField(stats, "mBeginTimeStamp", firstTimeStamp)
        setField(stats, "mEndTimeStamp", lastTimeStamp)
        return stats
    }

    private fun setField(target: Any, fieldName: String, value: Any) {
        val field = target.javaClass.getDeclaredField(fieldName)
        field.isAccessible = true
        field.set(target, value)
    }
}
