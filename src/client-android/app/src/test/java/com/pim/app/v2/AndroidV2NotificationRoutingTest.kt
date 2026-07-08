package com.pim.app.v2

import java.io.File
import org.junit.Assert.assertTrue
import org.junit.Test

class AndroidV2NotificationRoutingTest {
    @Test
    fun locationStatusIntentTargetsStatusDestination() {
        val controller = repoFile("src", "main", "java", "com", "pim", "app", "location", "service", "ForegroundLocationController.kt").readText()
        val main = repoFile("src", "main", "java", "com", "pim", "app", "MainActivity.kt").readText()
        val root = repoFile("src", "main", "java", "com", "pim", "app", "ui", "root", "PimRootScreen.kt").readText()

        assertTrue(controller.contains("EXTRA_OPEN_DESTINATION"))
        assertTrue(main.contains("EXTRA_OPEN_DESTINATION"))
        assertTrue(root.contains("initialDestination"))
        assertTrue(root.contains("PimDestination.Status"))
    }

    @Test
    fun legacyEndpointNotificationDetailsRemainLegacyShellWithoutStartingCollector() {
        val receiver = repoFile("src", "main", "java", "com", "pim", "app", "notifications", "NotificationActionReceiver.kt").readText()
        val shell = repoFile("src", "main", "java", "com", "pim", "app", "ui", "shell", "PimShellActivity.kt").readText()

        assertTrue(receiver.contains("PimShellActivity.intentFor(context, route.detailUrl)"))
        assertTrue(receiver.contains("PimShellActivity.intentFor(context, \"/endpoint-shell\")"))
        assertTrue(!shell.contains("collector.start()"))
    }

    @Test
    fun foregroundServiceStopsWhenCollectionDisabledAndReregistersPolicyIntervals() {
        val service = repoFile("src", "main", "java", "com", "pim", "app", "location", "service", "ForegroundLocationService.kt").readText()

        assertTrue(service.contains("if (!settings.continuousCollectionEnabled)"))
        assertTrue(service.contains("stopSelf()"))
        assertTrue(service.contains("requestLocationUpdates(decision.requestIntervalMillis)"))
        assertTrue(service.contains("motionSignalRepository.status.value.signal"))
        assertTrue(service.contains("ScheduleWindowSelector.current"))
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
