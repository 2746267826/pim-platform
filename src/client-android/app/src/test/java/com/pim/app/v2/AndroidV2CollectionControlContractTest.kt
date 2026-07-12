package com.pim.app.v2

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AndroidV2CollectionControlContractTest {
    @Test
    fun settingsSwitchUsesViewModelControllerAndPersistedTrackingState() {
        val screen = repoFile(
            "src",
            "main",
            "java",
            "com",
            "pim",
            "app",
            "ui",
            "settings",
            "SettingsScreen.kt"
        ).readText(Charsets.UTF_8)
        val viewModel = repoFile(
            "src",
            "main",
            "java",
            "com",
            "pim",
            "app",
            "ui",
            "settings",
            "SettingsViewModel.kt"
        ).readText(Charsets.UTF_8)

        assertFalse(
            "持续采集开关不能只保存在 Compose 本地状态",
            screen.contains("var collectionEnabled by rememberSaveable")
        )
        assertTrue(screen.contains("checked = state.continuousCollectionEnabled"))
        assertTrue(screen.contains("onCheckedChange = viewModel::setContinuousCollectionEnabled"))

        assertTrue(viewModel.contains("TrackingSettingsStore"))
        assertTrue(viewModel.contains("ForegroundLocationController"))
        assertTrue(viewModel.contains("PermissionStatusRepository"))
        assertTrue(viewModel.contains("trackingSettingsStore.read().continuousCollectionEnabled"))
        assertTrue(viewModel.contains("fun setContinuousCollectionEnabled(enabled: Boolean)"))
        assertTrue(viewModel.contains("persistedCollectionEnabled()"))
        assertTrue(viewModel.contains("trackingSettingsStore.setContinuousCollectionEnabled(false)"))
        assertTrue(viewModel.contains("trackingSettingsStore.setContinuousCollectionEnabled(true)"))
        assertTrue(viewModel.contains("foregroundLocationController.start()"))
        assertTrue(viewModel.contains("foregroundLocationController.stop()"))
    }

    @Test
    fun settingsRefreshAndLogoutPreserveDurableCollectionIntentAcrossBlockers() {
        val viewModel = repoFile(
            "src",
            "main",
            "java",
            "com",
            "pim",
            "app",
            "ui",
            "settings",
            "SettingsViewModel.kt"
        ).readText(Charsets.UTF_8)

        val refresh = viewModel.substringAfter("fun refresh()").substringBefore("fun updateApiAddress")
        val logout = viewModel.substringAfter("fun logout()").substringBefore(
            "fun setContinuousCollectionEnabled"
        )

        assertTrue(
            "refresh must display the durable collection intent without changing it",
            refresh.contains("continuousCollectionEnabled = persistedCollectionEnabled()")
        )
        assertFalse(
            "refresh must not disable collection intent when auth or permissions are blocked",
            refresh.contains("trackingSettingsStore.setContinuousCollectionEnabled(false)")
        )
        assertTrue(
            "logout must keep the durable collection intent visible",
            logout.contains("continuousCollectionEnabled = collectionIntent")
        )
        assertFalse(
            "logout must not disable collection intent or stop local collection",
            logout.contains("trackingSettingsStore.setContinuousCollectionEnabled(false)") ||
                logout.contains("foregroundLocationController.stop()")
        )
    }

    @Test
    fun enablingCollectionSwitchesServerBeforeCheckingTheBoundSession() {
        val viewModel = repoFile(
            "src",
            "main",
            "java",
            "com",
            "pim",
            "app",
            "ui",
            "settings",
            "SettingsViewModel.kt"
        ).readText(Charsets.UTF_8)

        val functionIndex = viewModel.indexOf("fun setContinuousCollectionEnabled(enabled: Boolean)")
        val serverSwitchIndex = viewModel.indexOf(
            "serverSettingsStore.setBaseUrl(validation.normalizedUrl)",
            functionIndex
        )
        val sessionCheckIndex = viewModel.indexOf("if (!hasCurrentServerSession())", functionIndex)
        val enableIndex = viewModel.indexOf(
            "trackingSettingsStore.setContinuousCollectionEnabled(true)",
            functionIndex
        )

        assertTrue("collection enable must persist the selected server", serverSwitchIndex > functionIndex)
        assertTrue(
            "collection enable must re-check the session after the server switch",
            sessionCheckIndex > serverSwitchIndex
        )
        assertTrue("collection must not enable before the bound-session check", enableIndex > sessionCheckIndex)
    }

    @Test
    fun foregroundLocationServiceChecksRequiredPermissionsBeforeStartingLocationForeground() {
        val service = repoFile(
            "src",
            "main",
            "java",
            "com",
            "pim",
            "app",
            "location",
            "service",
            "ForegroundLocationService.kt"
        ).readText(Charsets.UTF_8)

        val permissionCheck = service.indexOf("if (!hasRequiredLocationPermissions())")
        val foregroundStart = service.indexOf("startForeground(LocationNotificationRenderer.NOTIFICATION_ID, notification())")

        assertTrue("service must check location readiness before startForeground", permissionCheck >= 0)
        assertTrue("service must not start a location foreground service before permission gating", permissionCheck < foregroundStart)
        assertTrue(service.contains("Manifest.permission.ACCESS_FINE_LOCATION"))
        assertTrue(service.contains("Manifest.permission.ACCESS_BACKGROUND_LOCATION"))
        assertTrue(service.contains("fine == PackageManager.PERMISSION_GRANTED && background"))
    }

    @Test
    fun foregroundLocationServiceRegistersMotionTransitionsWhileCollecting() {
        val service = repoFile(
            "src",
            "main",
            "java",
            "com",
            "pim",
            "app",
            "location",
            "service",
            "ForegroundLocationService.kt"
        ).readText(Charsets.UTF_8)

        val register = service.indexOf("motionSignalRepository.registerActivityTransitions()")
        val requestLocations = service.indexOf("requestLocationUpdates(currentDecision.requestIntervalMillis)")
        val unregister = service.indexOf("motionSignalRepository.unregisterActivityTransitions()")

        assertTrue("motion transitions must be registered before location updates", register >= 0)
        assertTrue(register < requestLocations)
        assertTrue("motion transitions must be unregistered when collection stops", unregister >= 0)
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
