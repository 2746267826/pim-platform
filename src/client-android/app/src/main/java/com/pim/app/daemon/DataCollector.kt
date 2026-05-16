package com.pim.app.daemon

import android.app.usage.UsageStatsManager
import android.content.Context
import android.os.Build
import com.pim.app.data.AppUsageDao
import com.pim.app.data.AppUsageEntity
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.*
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DataCollector @Inject constructor(
    @ApplicationContext private val context: Context,
    private val dao: AppUsageDao
) {
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    fun start() {
        scope.launch {
            Timber.d("DataCollector started (5min interval)")
            while (isActive) {
                try {
                    val count = collectUsageStats()
                    if (count > 0) Timber.d("Collected $count usage stat entries")
                } catch (e: Exception) {
                    Timber.e(e, "UsageStats collection failed")
                }
                delay(5 * 60 * 1000L)
            }
        }
    }

    private suspend fun collectUsageStats(): Int {
        val usm = context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
        val end = System.currentTimeMillis()
        val begin = end - 5 * 60 * 1000L
        val interval = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            UsageStatsManager.INTERVAL_BEST
        } else {
            UsageStatsManager.INTERVAL_DAILY
        }
        val stats = usm.queryUsageStats(interval, begin, end)
        if (stats.isNullOrEmpty()) return 0

        val entities = stats.map { s ->
            AppUsageEntity(
                packageName = s.packageName,
                startTime = s.firstTimeStamp,
                endTime = s.lastTimeStamp,
                durationMs = s.totalTimeInForeground,
                lastTimeUsed = s.lastTimeUsed
            )
        }
        dao.insertAll(entities)
        return entities.size
    }

    fun stop() {
        scope.cancel()
        Timber.d("DataCollector stopped")
    }
}
