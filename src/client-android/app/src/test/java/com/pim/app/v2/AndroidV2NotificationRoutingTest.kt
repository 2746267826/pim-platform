package com.pim.app.v2

import java.io.File
import org.junit.Assert.assertFalse
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
        assertFalse(shell.contains("collector.start()"))
    }

    @Test
    fun foregroundServiceStopsWhenCollectionDisabledAndReregistersPolicyIntervals() {
        val service = repoFile("src", "main", "java", "com", "pim", "app", "location", "service", "ForegroundLocationService.kt").readText()

        assertTrue(service.contains("if (!settings.continuousCollectionEnabled)"))
        assertTrue(service.contains("stopSelf()"))
        assertTrue(service.contains("locationAcquisitionCoordinator.startAutomaticStream("))
        assertTrue(service.contains("locationAcquisitionCoordinator.updateAutomaticStream("))
        assertTrue(service.contains("withTimeoutOrNull(30_000L)"))
        assertTrue(service.contains("motionSignalRepository.status.value.signal"))
        assertTrue(service.contains("ScheduleWindowSelector.current"))
    }

    @Test
    fun liveUpdateNotificationActionReceiverHandlesCancelAndDismiss() {
        val receiver = repoFile("src", "main", "java", "com", "pim", "app", "notifications", "NotificationActionReceiver.kt").readText()
        assertTrue(receiver.contains("ACTION_CANCEL_LOCATION_SESSION"))
        assertTrue(receiver.contains("ACTION_DISMISS_LOCATION_LIVE_UPDATE"))
        assertTrue(receiver.contains("cancelCurrentSession"))
        assertTrue(receiver.contains("suppressSession"))
        assertTrue("receiver must parse sessionId via parseSessionUri", receiver.contains("parseSessionUri"))
        assertFalse("receiver must not use lastPathSegment for session ID", receiver.contains("lastPathSegment"))
    }

    @Test
    fun liveUpdateNotificationRendererUsesSeparateNotificationId() {
        val renderer = repoFile("src", "main", "java", "com", "pim", "app", "location", "liveupdate", "LocationLiveUpdateNotificationRenderer.kt").readText()
        assertTrue(renderer.contains("7102"))
        assertTrue(renderer.contains("pim_location_live_update"))
    }

    @Test
    fun pimAppCallsCancelStaleNotification() {
        val pimApp = repoFile("src", "main", "java", "com", "pim", "app", "PimApp.kt").readText()
        assertTrue(pimApp.contains("cancelStaleNotification"))
    }

    @Test
    fun pimAppCallsPublisherStartAfterCancelStaleNotification() {
        val pimApp = repoFile("src", "main", "java", "com", "pim", "app", "PimApp.kt").readText()
        assertTrue("must cancelStaleNotification before start", pimApp.contains("cancelStaleNotification()"))
        assertTrue("must contain start(scope)", pimApp.contains("start(scope)"))
        assertTrue("cancelStaleNotification must appear before start", pimApp.indexOf("cancelStaleNotification") < pimApp.indexOf("start"))
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
