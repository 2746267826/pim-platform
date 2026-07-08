package com.pim.app.v2

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AndroidV2ManifestTest {
    @Test
    fun manifestDeclaresNativeLauncherAndLocationForegroundService() {
        val manifest = repoFile("src", "main", "AndroidManifest.xml").readText()

        assertTrue(manifest.contains("android.permission.ACCESS_BACKGROUND_LOCATION"))
        assertTrue(manifest.contains("android.permission.ACTIVITY_RECOGNITION"))
        assertTrue(manifest.contains("android.permission.FOREGROUND_SERVICE_LOCATION"))
        assertTrue(manifest.contains(".location.service.ForegroundLocationService"))
        assertTrue(manifest.contains("android:foregroundServiceType=\"location\""))
        assertTrue(manifest.contains("android:name=\".MainActivity\""))
        assertFalse("Web shell must not be the launcher", launcherBlock(manifest).contains(".ui.shell.PimShellActivity"))
    }

    private fun launcherBlock(manifest: String): String {
        val launcherIndex = manifest.indexOf("android.intent.category.LAUNCHER")
        if (launcherIndex < 0) return ""
        val start = manifest.lastIndexOf("<activity", launcherIndex).coerceAtLeast(0)
        val end = manifest.indexOf("</activity>", launcherIndex).let { if (it < 0) manifest.length else it }
        return manifest.substring(start, end)
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
