package com.pim.app.schedule

import java.io.File
import org.junit.Assert.assertTrue
import org.junit.Test

class AndroidCompanionShellTest {
    @Test
    fun shellSourcesDeclareWebViewPermissionCenterAndRoutes() {
        val manifest = repoFile("src", "main", "AndroidManifest.xml").readText()
        val shell = repoFile("src", "main", "java", "com", "pim", "app", "ui", "shell", "PimShellActivity.kt").readText()
        val webView = repoFile("src", "main", "java", "com", "pim", "app", "ui", "shell", "PimWebViewScreen.kt").readText()
        val permissions = repoFile("src", "main", "java", "com", "pim", "app", "ui", "permissions", "PermissionCenterScreen.kt").readText()

        assertTrue(manifest.contains("POST_NOTIFICATIONS"))
        assertTrue(manifest.contains(".ui.shell.PimShellActivity"))
        assertTrue(manifest.contains("NotificationActionReceiver"))
        assertTrue(shell.contains("PermissionCenterScreen"))
        assertTrue(shell.contains("PimWebViewScreen"))
        assertTrue(permissions.contains("collection quality"))

        for (route in listOf("/today", "/tasks", "/calendar", "/reports", "/sync", "/data-center", "/confirmations")) {
            assertTrue("$route should be present", shell.contains(route) || webView.contains(route))
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
