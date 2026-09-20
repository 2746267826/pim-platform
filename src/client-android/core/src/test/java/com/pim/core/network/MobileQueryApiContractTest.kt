package com.pim.core.network

import java.io.File
import org.junit.Assert.assertTrue
import org.junit.Test

class MobileQueryApiContractTest {
    @Test
    fun apiServiceContainsMobileQueryEndpoints() {
        val api = repoFile("src", "main", "java", "com", "pim", "core", "network", "ApiService.kt").readText()

        assertTrue(api.contains("@GET(\"mobile/summary\")"))
        assertTrue(api.contains("@GET(\"mobile/timeline\")"))
        assertTrue(api.contains("@GET(\"mobile/quality\")"))
        assertTrue(api.contains("@GET(\"mobile/location/history\")"))
        assertTrue(api.contains("@GET(\"mobile/location/analytics/overview\")"))
        assertTrue(api.contains("@GET(\"mobile/location/analytics/tracks\")"))
        assertTrue(api.contains("@GET(\"mobile/location/analytics/segments/{segmentId}/points\")"))
        assertTrue(api.contains("@Query(\"date\") date: String? = null"))
        assertTrue(api.contains("@Query(\"rangeStartUtc\") rangeStartUtc: String? = null"))
        assertTrue(api.contains("@Query(\"rangeEndUtc\") rangeEndUtc: String? = null"))
        assertTrue(api.contains("@Query(\"timezone\") timezone: String? = null"))
    }

    @Test
    fun mobileTimelineSupportsPaginationAndTruncationMarkers() {
        // #330：单日会话可达数千条，服务端分页返回；客户端必须能请求指定页
        // 并识别 hasMore/truncated，否则只能显示一天的前一段。
        val api = repoFile("src", "main", "java", "com", "pim", "core", "network", "ApiService.kt").readText()
        val models = repoFile("src", "main", "java", "com", "pim", "core", "models", "MobileModels.kt").readText()

        assertTrue(api.contains("@Query(\"page\") page: Int? = null"))
        assertTrue(api.contains("@Query(\"pageSize\") pageSize: Int? = null"))
        assertTrue(models.contains("val totalCount: Int = 0"))
        assertTrue(models.contains("val hasMore: Boolean = false"))
        assertTrue(models.contains("val truncated: Boolean = false"))
    }

    @Test
    fun mobileModelsContainQueryDtosUsedByAndroidV2() {
        val models = repoFile("src", "main", "java", "com", "pim", "core", "models", "MobileModels.kt").readText()

        for (name in listOf(
            "MobileUsageSummaryResponse",
            "MobileAppUsageSummaryDto",
            "MobileTimelineResponse",
            "MobileQualityResponse",
            "MobileLocationHistoryResponse",
            "MobileLocationAnalyticsOverviewResponse",
            "MobileLocationTrackDto",
            "MobileLocationSegmentDto",
            "MobileLocationPathPointDto",
            "MobileGeoBoundsDto"
        )) {
            assertTrue("$name must exist", models.contains("data class $name"))
        }
        assertTrue(models.contains("val rangeStartUtc: String"))
        assertTrue(models.contains("val rangeEndUtc: String"))
        assertTrue(models.contains("val localStartDate: String"))
        assertTrue(models.contains("val localEndDate: String"))
    }

    private fun repoFile(vararg parts: String): File {
        var current: File? = File("").canonicalFile
        while (current != null) {
            val candidate = parts.fold(current) { dir, part -> dir.resolve(part) }
            if (candidate.exists()) return candidate
            current = current.parentFile
        }
        error("Could not find ${parts.joinToString(File.separator)}")
    }
}
