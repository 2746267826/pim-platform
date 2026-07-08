package com.pim.app.mobile.summary

import com.pim.app.data.MobileDataDao
import com.pim.core.models.MobileLocationAnalyticsOverviewResponse
import com.pim.core.models.MobileLocationTrackDto
import com.pim.core.models.MobileUsageSummaryResponse
import com.pim.core.network.ApiService
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import javax.inject.Inject
import kotlinx.coroutines.flow.first

data class MobileOverview(
    val usageSummary: MobileUsageSummaryResponse,
    val locationOverview: MobileLocationAnalyticsOverviewResponse,
    val tracks: List<MobileLocationTrackDto>,
    val pendingLocationCount: Int
)

class MobileOverviewRepository @Inject constructor(
    private val apiService: ApiService,
    private val mobileDataDao: MobileDataDao
) {
    suspend fun loadToday(
        date: LocalDate = LocalDate.now(ZoneOffset.UTC),
        deviceId: String? = null
    ): MobileOverview {
        val start = date.atStartOfDay().toInstant(ZoneOffset.UTC)
        val end = date.plusDays(1).atStartOfDay().toInstant(ZoneOffset.UTC)
        return loadRange(
            date = date,
            rangeStartUtc = start,
            rangeEndUtc = end,
            deviceId = deviceId
        )
    }

    suspend fun loadRange(
        date: LocalDate,
        rangeStartUtc: Instant,
        rangeEndUtc: Instant,
        deviceId: String? = null
    ): MobileOverview {
        val usage = apiService.getMobileSummary(date = date.toString(), deviceId = deviceId).data
            ?: error("移动端使用摘要为空")
        val location = apiService.getMobileLocationOverview(
            rangeStartUtc = rangeStartUtc.toString(),
            rangeEndUtc = rangeEndUtc.toString(),
            deviceId = deviceId,
            maxAccuracyMeters = 50.0
        ).data ?: error("位置概览为空")
        val tracks = apiService.getMobileLocationTracks(
            rangeStartUtc = rangeStartUtc.toString(),
            rangeEndUtc = rangeEndUtc.toString(),
            deviceId = deviceId,
            maxAccuracyMeters = 50.0
        ).data.orEmpty()

        return MobileOverview(
            usageSummary = usage,
            locationOverview = location,
            tracks = tracks,
            pendingLocationCount = mobileDataDao.pendingLocationPointCount().first()
        )
    }
}
