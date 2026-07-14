package com.pim.app.v2

import java.io.File
import org.junit.Assert.assertTrue
import org.junit.Test

class AndroidV2ScreenContentTest {
    @Test
    fun screensExposeApprovedInformationArchitecture() {
        assertContains("ui/today/TodayScreen.kt", listOf("今日概览", "今日轨迹", "停留", "移动距离", "手机使用", "当前策略"))
        assertContains("ui/tracks/TracksScreen.kt", listOf("轨迹历史", "时间范围", "质量过滤", "< 50m", "原始点", "片段详情"))
        assertContains("ui/schedule/SchedulePolicyScreen.kt", listOf("日程低频策略", "当前日程", "恢复阈值", "100m", "策略切换"))
        assertContains(
            "ui/settings/SettingsScreen.kt",
            listOf("API 地址", "账号", "持续采集", "采集预设", "高级参数", "网络", "日志", "权限", "恢复默认")
        )
    }

    private fun assertContains(path: String, labels: List<String>) {
        val file = repoFile("src", "main", "java", "com", "pim", "app", *path.split('/').toTypedArray()).readText()
        labels.forEach { label -> assertTrue("$path missing $label", file.contains(label)) }
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
