package com.pim.app.mobile.usage

import android.app.usage.UsageStatsManager
import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UsageAccessChecker @Inject constructor(
    @ApplicationContext private val context: Context
) {
    fun hasUsageAccess(
        nowUtc: Long = System.currentTimeMillis(),
        lookbackMs: Long = DEFAULT_LOOKBACK_MS
    ): Boolean {
        val usageStatsManager = context.getSystemService(Context.USAGE_STATS_SERVICE) as? UsageStatsManager
            ?: return false
        val beginUtc = (nowUtc - lookbackMs).coerceAtLeast(0L)

        return try {
            usageStatsManager
                .queryUsageStats(UsageStatsManager.INTERVAL_DAILY, beginUtc, nowUtc)
                .orEmpty()
                .isNotEmpty()
        } catch (_: SecurityException) {
            false
        }
    }

    companion object {
        private const val DEFAULT_LOOKBACK_MS = 24 * 60 * 60 * 1000L
    }
}
