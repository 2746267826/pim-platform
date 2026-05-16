package com.pim.app.daemon

import android.app.usage.UsageStatsManager
import android.content.Context
import android.os.Build
import kotlinx.coroutines.*
import timber.log.Timber

class DataCollector(private val context: Context) {
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    fun start() {
        scope.launch {
            while (isActive) {
                try {
                    collectUsageStats()
                    Timber.d("UsageStats collected")
                } catch (e: Exception) {
                    Timber.e(e, "UsageStats collection failed")
                }
                delay(5 * 60 * 1000L) // 5 minutes
            }
        }
    }

    private fun collectUsageStats() {
        val usm = context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
        val end = System.currentTimeMillis()
        val begin = end - 5 * 60 * 1000L
        val interval = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            UsageStatsManager.INTERVAL_BEST
        } else {
            UsageStatsManager.INTERVAL_DAILY
        }
        val stats = usm.queryUsageStats(interval, begin, end)
        Timber.d("Collected ${stats.size} usage stat entries")
    }

    fun stop() { scope.cancel() }
}
