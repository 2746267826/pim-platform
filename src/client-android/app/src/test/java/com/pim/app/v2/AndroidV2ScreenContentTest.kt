package com.pim.app.v2

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AndroidV2ScreenContentTest {
    @Test
    fun screensExposeApprovedInformationArchitecture() {
        assertContains("ui/today/TodayScreen.kt", listOf("syncButtonLabel", "syncButtonShowSpinner", "待传总数", "定位待传", "上传中", "已确认", "本轮拒绝", "永久拒绝", "服务器版本不支持嵌入页面", "打开设置"))
        assertContains("ui/tracks/TracksScreen.kt", listOf("PimWebViewScreen", "/embed/android/tracks", "viewModel.bridge", "服务器版本不支持嵌入页面", "打开设置", "embedSupported"))
        assertContains(
            "ui/settings/SettingsScreen.kt",
            listOf("API 地址", "账号", "持续采集", "采集预设", "高级参数", "网络", "日志", "权限", "恢复默认")
        )
        assertContains("ui/location/LocationScreen.kt", listOf(
            "location-status-section", "location-best-section", "location-actions-section", "location-queue-section",
            "location-start", "location-cancel", "location-restart", "location-open-settings",
            "location-low-quality-warning",
            "location-pending-total", "location-pending-points",
            "location-accuracy", "location-provider", "location-latitude", "location-longitude",
            "location-altitude", "location-speed", "location-bearing", "location-recorded-time"
        ))
    }

    @Test
    fun schedulePolicyScreenUsesComposeLifecyclePatterns() {
        val src = readSource("ui", "schedule", "SchedulePolicyScreen.kt")
        assertTrue("SchedulePolicyScreen must use hiltViewModel", src.contains("hiltViewModel"))
        assertTrue("SchedulePolicyScreen must use collectAsStateWithLifecycle", src.contains("collectAsStateWithLifecycle"))
        assertTrue("SchedulePolicyScreen must use LaunchedEffect", src.contains("LaunchedEffect"))
        assertTrue("SchedulePolicyScreen must call refreshIfStale", src.contains("refreshIfStale"))
    }

    @Test
    fun schedulePolicyScreenExposesRequiredLabels() {
        val src = readSource("ui", "schedule", "SchedulePolicyScreen.kt")
        for (label in listOf(
            "日程",
            "服务端日程",
            "当前日程",
            "下一项",
            "近期日程",
            "当前策略",
            "日程数据可能过期",
            "重试",
            "前往设置"
        )) {
            assertTrue("SchedulePolicyScreen missing label: $label", src.contains(label))
        }
    }

    @Test
    fun schedulePolicyScreenExposesRequiredTestTags() {
        val src = readSource("ui", "schedule", "SchedulePolicyScreen.kt")
        for (tag in listOf(
            "schedule-refresh",
            "schedule-retry",
            "schedule-settings",
            "schedule-current",
            "schedule-upcoming",
            "schedule-policy"
        )) {
            assertTrue("SchedulePolicyScreen missing testTag: $tag", src.contains(tag))
        }
        val count = src.split("schedule-refresh").size - 1
        assertTrue("schedule-refresh must appear exactly once as stable tag, found $count", count == 1)
    }

    @Test
    fun schedulePolicyScreenExcludesOldHardcodedPlaceholders() {
        val src = readSource("ui", "schedule", "SchedulePolicyScreen.kt")
        assertFalse("SchedulePolicyScreen must NOT contain old placeholder", src.contains("当前没有带位置信息的日程。"))
        assertFalse("SchedulePolicyScreen must NOT contain old placeholder", src.contains("进入带地点的日程后，定位间隔会放宽到 15 分钟。"))
    }

    @Test
    fun rootScreenPassesSettingsCallbackForSchedule() {
        val src = readSource("ui", "root", "PimRootScreen.kt")
        val marker = "PimDestination.Schedule ->"
        val startIdx = src.indexOf(marker)
        assertTrue("Schedule destination block not found", startIdx >= 0)
        val endMarker = "PimDestination.Status ->"
        val endIdx = src.indexOf(endMarker, startIdx + marker.length)
        assertTrue("Status destination block not found", endIdx > startIdx)
        val block = src.substring(startIdx + marker.length, endIdx)
        assertTrue("Schedule block must contain onOpenSettings", block.contains("onOpenSettings"))
        assertTrue("Schedule block must navigate to Settings", block.contains("PimDestination.Settings"))
    }

    @Test
    fun scheduleRefreshTagIsStableAcrossStates() {
        val src = readSource("ui", "schedule", "SchedulePolicyScreen.kt")
        val headerStart = "private fun PolicyHeader"
        val headerIdx = src.indexOf(headerStart)
        assertTrue("PolicyHeader function not found", headerIdx >= 0)
        val tagIdx = src.indexOf("schedule-refresh", headerIdx)
        assertTrue("schedule-refresh testTag must exist in PolicyHeader", tagIdx >= 0)
        val ifIdx = src.indexOf("if (isRefreshing)", headerIdx)
        assertTrue("if (isRefreshing) must exist in PolicyHeader", ifIdx >= 0)
        assertTrue("schedule-refresh must appear BEFORE if (isRefreshing) to prove stable wrapper",
            tagIdx < ifIdx)
    }

    @Test
    fun emptyStateIncludesPolicySummary() {
        val src = readSource("ui", "schedule", "SchedulePolicyScreen.kt")
        val emptyStart = "is SchedulePolicyUiState.Empty ->"
        val emptyIdx = src.indexOf(emptyStart)
        assertTrue("Empty state not found", emptyIdx >= 0)
        val nextState = "is SchedulePolicyUiState"
        val nextIdx = src.indexOf(nextState, emptyIdx + emptyStart.length)
        val emptyBlock = if (nextIdx >= 0) src.substring(emptyIdx, nextIdx) else src.substring(emptyIdx)
        assertTrue("Empty state must contain empty message", emptyBlock.contains("暂无日程安排"))
        assertTrue("Empty state must contain PolicySummarySection", emptyBlock.contains("PolicySummarySection"))
    }

    @Test
    fun headerShowsTwoSeparateTimestampRows() {
        val src = readSource("ui", "schedule", "SchedulePolicyScreen.kt")
        assertFalse("Header must not combine timestamps in single Text buildString",
            src.contains("append(\"  上次尝试: \")"))
        assertTrue("Header must contain '上次成功:' label", src.contains("上次成功:"))
        assertTrue("Header must contain '上次尝试:' label", src.contains("上次尝试:"))
    }

    @Test
    fun recoveryThresholdPreservesDecimals() {
        val src = readSource("ui", "schedule", "SchedulePolicyScreen.kt")
        assertFalse("recoveryThreshold must not use toInt() truncation",
            src.contains("recoveryThresholdMeters.toInt()"))
    }

    @Test
    fun loadingStateDisabledRefreshNode() {
        val src = readSource("ui", "schedule", "SchedulePolicyScreen.kt")
        val headerStart = "private fun PolicyHeader"
        val headerIdx = src.indexOf(headerStart)
        assertTrue("PolicyHeader not found", headerIdx >= 0)
        val tagIdx = src.indexOf("schedule-refresh", headerIdx)
        assertTrue("schedule-refresh must exist", tagIdx >= 0)
        val enabledIdx = src.indexOf("enabled =", headerIdx)
        assertTrue("PolicyHeader must have enabled/disabled on refresh node", enabledIdx >= 0)
        assertTrue("enabled must be controlled",
            src.substring(headerIdx, src.indexOf("\n}", headerIdx + 50)).contains("enabled")
        )
    }

    private fun assertContains(path: String, labels: List<String>) {
        val file = repoFile("src", "main", "java", "com", "pim", "app", *path.split('/').toTypedArray()).readText()
        labels.forEach { label -> assertTrue("$path missing $label", file.contains(label)) }
    }

    private fun readSource(vararg parts: String): String {
        val full = listOf("src", "main", "java", "com", "pim", "app") + parts.toList()
        return repoFile(*full.toTypedArray()).readText()
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
