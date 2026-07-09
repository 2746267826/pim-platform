package com.pim.app.v2

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AndroidV2TodayStatusContractTest {
    @Test
    fun todayApiChipUsesObservedStatusInsteadOfHardcodedPendingConnection() {
        val screen = repoFile(
            "src",
            "main",
            "java",
            "com",
            "pim",
            "app",
            "ui",
            "today",
            "TodayScreen.kt"
        ).readText(Charsets.UTF_8)
        val viewModel = repoFile(
            "src",
            "main",
            "java",
            "com",
            "pim",
            "app",
            "ui",
            "today",
            "TodayViewModel.kt"
        )

        assertFalse("今日页不能硬编码 API 待连接", screen.contains("Text(\"API：待连接\")"))
        assertTrue(screen.contains("hiltViewModel"))
        assertTrue(screen.contains("collectAsStateWithLifecycle"))
        assertTrue(screen.contains("state.apiStatusLabel"))
        assertTrue("今日页需要自己的状态 ViewModel", viewModel.exists())
    }

    private fun repoFile(vararg parts: String): File {
        var current: File? = File("").canonicalFile
        while (current != null) {
            val candidate = parts.fold(current) { dir, part -> dir.resolve(part) }
            if (candidate.exists() || parts.last().endsWith(".kt")) return candidate
            current = current.parentFile
        }
        error("Could not find ${parts.joinToString(File.separator)}")
    }
}
