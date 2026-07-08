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
