package com.pim.app.v2

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AndroidV2NativeShellTest {
    @Test
    fun rootDefinesApprovedFiveTabsAndNoWebViewPrimaryExperience() {
        val destination = repoFile("src", "main", "java", "com", "pim", "app", "ui", "root", "PimDestination.kt").readText()
        val root = repoFile("src", "main", "java", "com", "pim", "app", "ui", "root", "PimRootScreen.kt").readText()

        for (label in listOf("今日", "轨迹", "日程", "状态", "设置")) {
            assertTrue("$label tab must be present", destination.contains(label))
        }
        assertTrue(root.contains("NavigationBar"))
        assertTrue(root.contains("PimTheme"))
        assertFalse(root.contains("PimWebViewScreen"))
        assertFalse(root.contains("WebView"))
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
