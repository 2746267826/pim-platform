package com.pim.app.ui.settings

import android.Manifest
import android.app.Application
import android.content.Context
import android.content.ContextWrapper
import android.content.SharedPreferences
import android.os.Looper
import androidx.test.core.app.ApplicationProvider
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.testing.WorkManagerTestInitHelper
import com.pim.app.TestPimApp
import com.pim.app.location.service.ForegroundLocationController
import com.pim.app.schedule.ScheduleCacheStore
import com.pim.app.schedule.ScheduleCacheWindow
import com.pim.app.schedule.ScheduleCacheDocument
import com.pim.app.status.PermissionStatusSnapshot
import com.pim.app.mobile.sync.MobileSyncScheduler
import com.pim.app.mobile.usage.UsageAccessChecker
import com.pim.app.permissions.PermissionStatusRepository
import com.pim.app.settings.TrackingSettingsStore
import com.pim.app.status.ConnectionProbeOutcome
import com.pim.app.status.ConnectionProbeResult
import com.pim.app.status.ConnectionProbeService
import com.pim.app.status.ConnectionProbeStage
import com.pim.app.status.ConnectionProbeStore
import com.pim.app.status.ProbeTokenSource
import com.pim.app.status.ServerCapabilities
import com.pim.core.auth.SecurePreferencesFactory
import com.pim.core.auth.ServerBoundLoginCoordinator
import com.pim.core.auth.ServerBoundLoginTransport
import com.pim.core.auth.TokenManager
import com.pim.core.settings.PimServerEndpoints
import com.pim.core.settings.ServerSettingsStore
import java.util.ArrayDeque
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import java.io.File
import com.pim.app.location.service.ForegroundLocationService
import com.pim.app.mobile.diagnostics.DiagnosticExportResult
import com.pim.app.mobile.diagnostics.DiagnosticOperations
import com.pim.app.mobile.logs.StructuredLogRepository
import com.pim.app.recovery.RunningStateRestorer
import kotlinx.coroutines.CompletableDeferred

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = TestPimApp::class)
class SettingsServerMutationTest {
    private val mainDispatcher = UnconfinedTestDispatcher()
    private lateinit var context: Context
    private val cacheDirs = mutableListOf<File>()

    @Before
    fun setUp() {
        Dispatchers.setMain(mainDispatcher)
        val application = ApplicationProvider.getApplicationContext<Application>()
        context = application
        WorkManagerTestInitHelper.initializeTestWorkManager(context)
        shadowOf(application).grantPermissions(
            Manifest.permission.POST_NOTIFICATIONS,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_BACKGROUND_LOCATION,
            Manifest.permission.ACTIVITY_RECOGNITION
        )
        listOf(SERVER_PREFS, AUTH_PREFS, TRACKING_PREFS, PROBE_PREFS).forEach { name ->
            check(context.getSharedPreferences(name, Context.MODE_PRIVATE).edit().clear().commit())
        }
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        cacheDirs.forEach { it.deleteRecursively() }
        cacheDirs.clear()
    }

    @Test
    fun saveCommitFailureAfterTokenClearReloadsServerAWithoutSession() {
        val fixture = fixture()
        fixture.serverPreferences.enqueueCommitResult(false)
        fixture.viewModel.updateApiAddress(SERVER_B_URL)

        assertFalse(fixture.viewModel.saveApiAddress())

        val state = fixture.viewModel.state.value
        assertEquals(SERVER_A_URL, state.apiAddress)
        assertFalse(state.isLoggedIn)
        assertTrue(state.continuousCollectionEnabled)
        assertTrue(fixture.trackingSettings.read().continuousCollectionEnabled)
    }

    @Test
    fun successfulServerSwitchReloadsServerBAndClearedSessionWithoutChangingCollectionIntent() {
        val fixture = fixture()
        fixture.viewModel.updateApiAddress(SERVER_B_URL)

        assertTrue(fixture.viewModel.saveApiAddress())

        val state = fixture.viewModel.state.value
        assertEquals(SERVER_B_URL, state.apiAddress)
        assertFalse(state.isLoggedIn)
        assertTrue(state.continuousCollectionEnabled)
        assertTrue(fixture.trackingSettings.read().continuousCollectionEnabled)
    }

    @Test
    fun failedSessionClearAbortsUrlSwitch() {
        val fixture = fixture(failSessionClear = true)
        fixture.serverPreferences.enqueueCommitResult(true)
        fixture.serverPreferences.enqueueCommitResult(false)
        fixture.viewModel.updateApiAddress(SERVER_B_URL)

        assertFalse(fixture.viewModel.saveApiAddress())

        val actualServerUrl = fixture.serverSettings.getBaseUrl()
        val state = fixture.viewModel.state.value
        assertEquals(actualServerUrl, state.apiAddress)
        assertEquals(
            !fixture.tokenManager.getAccessTokenForServer(actualServerUrl).isNullOrBlank(),
            state.isLoggedIn
        )
        assertTrue(fixture.trackingSettings.read().continuousCollectionEnabled)
    }

    @Test
    fun collectionServerSaveFailureAfterTokenClearReloadsServerAWithoutSession() {
        val fixture = fixture()
        fixture.serverPreferences.enqueueCommitResult(false)
        fixture.viewModel.updateApiAddress(SERVER_B_URL)

        fixture.viewModel.setContinuousCollectionEnabled(true)

        val state = fixture.viewModel.state.value
        assertEquals(SERVER_A_URL, state.apiAddress)
        assertFalse(state.isLoggedIn)
        assertTrue(state.continuousCollectionEnabled)
        assertTrue(fixture.trackingSettings.read().continuousCollectionEnabled)
    }

    @Test
    fun successfulCollectionServerSwitchKeepsIntentWhileNewServerSessionIsMissing() {
        val fixture = fixture()
        fixture.viewModel.updateApiAddress(SERVER_B_URL)

        fixture.viewModel.setContinuousCollectionEnabled(true)

        val state = fixture.viewModel.state.value
        assertEquals(SERVER_B_URL, state.apiAddress)
        assertFalse(state.isLoggedIn)
        assertTrue(state.continuousCollectionEnabled)
        assertTrue(fixture.trackingSettings.read().continuousCollectionEnabled)
    }

    @Test
    fun logoutClearsSessionWithoutChangingCollectionIntent() {
        val fixture = fixture()

        fixture.viewModel.logout()

        assertFalse(fixture.viewModel.state.value.isLoggedIn)
        assertTrue(fixture.viewModel.state.value.continuousCollectionEnabled)
        assertTrue(fixture.trackingSettings.read().continuousCollectionEnabled)
    }

    @Test
    fun applyTrackingPresetUpdatesStateAndPersists() {
        val fixture = fixture()
        assertEquals("power-saving", fixture.viewModel.state.value.trackingProfile)

        fixture.viewModel.applyTrackingPreset("standard")

        val state = fixture.viewModel.state.value
        assertEquals("standard", state.trackingProfile)
        val stored = fixture.trackingSettings.read()
        assertEquals("standard", stored.profile)
        assertEquals(120_000L, stored.normalIntervalMillis)
        assertEquals(600_000L, stored.scheduleLowFrequencyIntervalMillis)
        assertEquals(45_000L, stored.movementIntervalMillis)
    }

    @Test
    fun invalidAdvancedInputDoesNotPersist() {
        val fixture = fixture()
        val before = fixture.trackingSettings.read()
        fixture.viewModel.updateNormalMinText(Long.MAX_VALUE.toString())
        fixture.viewModel.updateScheduleMinText("61")
        fixture.viewModel.updateMovementSecText("29")
        fixture.viewModel.updateRecoveryMetersText("NaN")
        fixture.viewModel.updateAccuracyMetersText("51")
        fixture.viewModel.updateAltitudeSecText("31")

        assertFalse(fixture.viewModel.saveAdvancedSettings())

        val state = fixture.viewModel.state.value
        assertTrue(state.advancedErrors.containsKey("normalInterval"))
        assertTrue(state.advancedErrors.containsKey("scheduleInterval"))
        assertTrue(state.advancedErrors.containsKey("movementInterval"))
        assertTrue(state.advancedErrors.containsKey("recoveryThreshold"))
        assertTrue(state.advancedErrors.containsKey("accuracy"))
        assertTrue(state.advancedErrors.containsKey("altitudeWait"))
        assertEquals(Long.MAX_VALUE.toString(), state.normalMinText)
        assertEquals("61", state.scheduleMinText)
        assertEquals("NaN", state.recoveryMetersText)
        assertEquals(before, fixture.trackingSettings.read())
    }

    @Test
    fun validAdvancedValuesSaveCustomProfile() {
        val fixture = fixture()
        fixture.viewModel.updateNormalMinText("2")
        fixture.viewModel.updateScheduleMinText("30")
        fixture.viewModel.updateMovementSecText("45")
        fixture.viewModel.updateRecoveryMetersText("75.5")
        fixture.viewModel.updateAccuracyMetersText("35.5")
        fixture.viewModel.updateAltitudeSecText("20")

        assertTrue(fixture.viewModel.saveAdvancedSettings())

        val stored = fixture.trackingSettings.read()
        assertEquals("custom", stored.profile)
        assertEquals(120_000L, stored.normalIntervalMillis)
        assertEquals(1_800_000L, stored.scheduleLowFrequencyIntervalMillis)
        assertEquals(45_000L, stored.movementIntervalMillis)
        assertEquals(75.5, stored.scheduleRecoveryThresholdMeters, 0.001)
        assertEquals(35.5f, stored.maxUploadAccuracyMetersExclusive)
        assertEquals(20_000L, stored.altitudeWaitTimeoutMillis)
        val state = fixture.viewModel.state.value
        assertEquals("custom", state.trackingProfile)
        assertTrue(state.advancedErrors.isEmpty())
    }

    @Test
    fun networkPreferencePersistsAndUpdatesPeriodicWork() {
        val fixture = fixture()
        assertFalse(fixture.viewModel.state.value.syncOnUnmeteredOnly)

        fixture.viewModel.setSyncOnUnmeteredOnly(true)

        assertTrue(fixture.viewModel.state.value.syncOnUnmeteredOnly)
        assertTrue(fixture.trackingSettings.read().syncOnUnmeteredOnly)

        val workInfos = WorkManager.getInstance(context)
            .getWorkInfosForUniqueWork(MobileSyncScheduler.PERIODIC_NAME).get()
        val enqueued = workInfos.filter { it.state == WorkInfo.State.ENQUEUED }
        assertEquals(1, enqueued.size)
    }

    @Test
    fun verboseLoggingPersists() {
        val fixture = fixture()
        assertFalse(fixture.viewModel.state.value.verboseLoggingEnabled)
        assertNull(fixture.viewModel.state.value.verboseLoggingUntilUtcMillis)
        val before = System.currentTimeMillis()

        fixture.viewModel.setVerboseLoggingEnabled(true)

        assertTrue(fixture.viewModel.state.value.verboseLoggingEnabled)
        val deadline = fixture.viewModel.state.value.verboseLoggingUntilUtcMillis
        assertNotNull(deadline)
        assertTrue(deadline!! > before)
        assertEquals(deadline, fixture.trackingSettings.read().verboseLoggingUntilUtcMillis)
    }

    @Test
    fun logRetentionPersists() {
        val fixture = fixture()
        assertEquals(7, fixture.viewModel.state.value.logRetentionDays)

        fixture.viewModel.setLogRetentionDays(14)

        assertEquals(14, fixture.viewModel.state.value.logRetentionDays)
        assertEquals(14, fixture.trackingSettings.read().logRetentionDays)
    }

    @Test
    fun invalidLogRetentionDayIsIgnored() {
        val fixture = fixture()

        fixture.viewModel.setLogRetentionDays(99)

        assertEquals(7, fixture.viewModel.state.value.logRetentionDays)
    }

    @Test
    fun resetOperationalDefaultsPreservesServerAndAuth() {
        val fixture = fixture(isServiceRunning = { true })
        fixture.viewModel.onResume()
        await(1000) {
            fixture.viewModel.state.value.collectionStatus == "持续采集正在运行。"
        }
        fixture.trackingSettings.setContinuousCollectionEnabled(true)
        fixture.trackingSettings.write(
            fixture.trackingSettings.read().copy(
                profile = "custom",
                normalIntervalMillis = 120_000L,
                syncOnUnmeteredOnly = true,
                logRetentionDays = 30
            )
        )

        fixture.viewModel.resetOperationalDefaults()

        assertEquals(SERVER_A_URL, fixture.serverSettings.getBaseUrl())
        assertNotNull(fixture.tokenManager.getAccessTokenForServer(SERVER_A_URL))

        val stored = fixture.trackingSettings.read()
        assertEquals("power-saving", stored.profile)
        assertEquals(180_000L, stored.normalIntervalMillis)
        assertEquals(false, stored.syncOnUnmeteredOnly)
        assertEquals(7, stored.logRetentionDays)
        assertEquals(false, stored.continuousCollectionEnabled)
        assertFalse(fixture.viewModel.state.value.continuousCollectionEnabled)
        assertEquals("持续采集已关闭。", fixture.viewModel.state.value.collectionStatus)
    }

    @Test
    fun `saveApiAddress success clears old server cache when identity changes`() {
        val fixture = fixture()
        fixture.scheduleCacheStore.write(
            SERVER_A_CACHE_IDENTITY,
            ScheduleCacheDocument(windows = listOf(ScheduleCacheWindow("1", "a", "", 100L, 200L)), rangeStartMillis = 100L, rangeEndMillis = 200L)
        )
        fixture.scheduleCacheStore.write(
            SERVER_B_CACHE_IDENTITY,
            ScheduleCacheDocument(windows = listOf(ScheduleCacheWindow("2", "b", "", 100L, 200L)), rangeStartMillis = 100L, rangeEndMillis = 200L)
        )
        assertNotNull(fixture.scheduleCacheStore.read(SERVER_A_CACHE_IDENTITY))

        fixture.viewModel.updateApiAddress(SERVER_B_URL)
        assertTrue(fixture.viewModel.saveApiAddress())

        assertNull("old server cache must be cleared", fixture.scheduleCacheStore.read(SERVER_A_CACHE_IDENTITY))
        assertNotNull("new server cache must remain", fixture.scheduleCacheStore.read(SERVER_B_CACHE_IDENTITY))
    }

    @Test
    fun `saveApiAddress validation failure does not clear cache`() {
        val fixture = fixture()
        fixture.scheduleCacheStore.write(
            SERVER_A_CACHE_IDENTITY,
            ScheduleCacheDocument(windows = listOf(ScheduleCacheWindow("1", "a", "", 100L, 200L)), rangeStartMillis = 100L, rangeEndMillis = 200L)
        )

        fixture.viewModel.updateApiAddress("not-a-valid-url")
        assertFalse(fixture.viewModel.saveApiAddress())

        assertNotNull("cache must not be cleared on validation failure", fixture.scheduleCacheStore.read(SERVER_A_CACHE_IDENTITY))
    }

    @Test
    fun `saveApiAddress commit failure does not clear cache`() {
        val fixture = fixture()
        fixture.scheduleCacheStore.write(
            SERVER_A_CACHE_IDENTITY,
            ScheduleCacheDocument(windows = listOf(ScheduleCacheWindow("1", "a", "", 100L, 200L)), rangeStartMillis = 100L, rangeEndMillis = 200L)
        )
        fixture.serverPreferences.enqueueCommitResult(false)

        fixture.viewModel.updateApiAddress(SERVER_B_URL)
        assertFalse(fixture.viewModel.saveApiAddress())

        assertNotNull("cache must not be cleared when save fails", fixture.scheduleCacheStore.read(SERVER_A_CACHE_IDENTITY))
    }

    @Test
    fun `setContinuousCollectionEnabled success switch clears old server cache`() {
        val fixture = fixture()
        fixture.scheduleCacheStore.write(
            SERVER_A_CACHE_IDENTITY,
            ScheduleCacheDocument(windows = listOf(ScheduleCacheWindow("1", "a", "", 100L, 200L)), rangeStartMillis = 100L, rangeEndMillis = 200L)
        )
        fixture.scheduleCacheStore.write(
            SERVER_B_CACHE_IDENTITY,
            ScheduleCacheDocument(windows = listOf(ScheduleCacheWindow("2", "b", "", 100L, 200L)), rangeStartMillis = 100L, rangeEndMillis = 200L)
        )

        fixture.viewModel.updateApiAddress(SERVER_B_URL)
        fixture.viewModel.setContinuousCollectionEnabled(true)

        assertNull("old server cache must be cleared", fixture.scheduleCacheStore.read(SERVER_A_CACHE_IDENTITY))
        assertNotNull("new server cache must remain", fixture.scheduleCacheStore.read(SERVER_B_CACHE_IDENTITY))
    }

    @Test
    fun `setContinuousCollectionEnabled commit failure does not clear cache`() {
        val fixture = fixture()
        fixture.scheduleCacheStore.write(
            SERVER_A_CACHE_IDENTITY,
            ScheduleCacheDocument(windows = listOf(ScheduleCacheWindow("1", "a", "", 100L, 200L)), rangeStartMillis = 100L, rangeEndMillis = 200L)
        )
        fixture.serverPreferences.enqueueCommitResult(false)

        fixture.viewModel.updateApiAddress(SERVER_B_URL)
        fixture.viewModel.setContinuousCollectionEnabled(true)

        assertNotNull("cache must not be cleared when save fails", fixture.scheduleCacheStore.read(SERVER_A_CACHE_IDENTITY))
    }

    @Test
    fun `logout success clears current server cache`() {
        val fixture = fixture()
        fixture.scheduleCacheStore.write(
            SERVER_A_CACHE_IDENTITY,
            ScheduleCacheDocument(windows = listOf(ScheduleCacheWindow("1", "a", "", 100L, 200L)), rangeStartMillis = 100L, rangeEndMillis = 200L)
        )

        fixture.viewModel.logout()

        assertNull("current server cache must be cleared on logout", fixture.scheduleCacheStore.read(SERVER_A_CACHE_IDENTITY))
    }

    @Test
    fun `logout token clear failure does not clear cache`() {
        val fixture = fixture(failSessionClear = true)
        fixture.scheduleCacheStore.write(
            SERVER_A_CACHE_IDENTITY,
            ScheduleCacheDocument(windows = listOf(ScheduleCacheWindow("1", "a", "", 100L, 200L)), rangeStartMillis = 100L, rangeEndMillis = 200L)
        )

        fixture.viewModel.logout()

        assertNotNull("cache must not be cleared when token clear fails", fixture.scheduleCacheStore.read(SERVER_A_CACHE_IDENTITY))
    }

    @Test
    fun initialStateDraftsMatchTrackingDefaults() {
        val fixture = fixture()
        val state = fixture.viewModel.state.value
        assertEquals("power-saving", state.trackingProfile)
        assertEquals("3", state.normalMinText)
        assertEquals("15", state.scheduleMinText)
        assertEquals("60", state.movementSecText)
        assertEquals("100", state.recoveryMetersText)
        assertEquals("50", state.accuracyMetersText)
        assertEquals("15", state.altitudeSecText)
    }

    @Test
    fun uiStateDefaultsMatchTrackingDefaults() {
        val state = SettingsUiState()
        assertEquals("power-saving", state.trackingProfile)
        assertEquals("3", state.normalMinText)
        assertEquals("15", state.scheduleMinText)
        assertEquals("60", state.movementSecText)
        assertEquals("100", state.recoveryMetersText)
        assertEquals("50", state.accuracyMetersText)
        assertEquals("15", state.altitudeSecText)
    }

    @Test
    fun fractionalAdvancedValuesPersistCorrectly() {
        val fixture = fixture()
        fixture.viewModel.updateNormalMinText("2.5")
        fixture.viewModel.updateScheduleMinText("30.5")
        fixture.viewModel.updateMovementSecText("45.5")
        fixture.viewModel.updateRecoveryMetersText("75.3")
        fixture.viewModel.updateAccuracyMetersText("35.3")
        fixture.viewModel.updateAltitudeSecText("20.5")

        assertTrue(fixture.viewModel.saveAdvancedSettings())

        val stored = fixture.trackingSettings.read()
        assertEquals(150_000L, stored.normalIntervalMillis)
        assertEquals(1_830_000L, stored.scheduleLowFrequencyIntervalMillis)
        assertEquals(45_500L, stored.movementIntervalMillis)
        assertEquals(75.3, stored.scheduleRecoveryThresholdMeters, 0.001)
        assertEquals(35.3f, stored.maxUploadAccuracyMetersExclusive)
        assertEquals(20_500L, stored.altitudeWaitTimeoutMillis)

        val state = fixture.viewModel.state.value
        assertEquals("2.5", state.normalMinText)
        assertEquals("30.5", state.scheduleMinText)
        assertEquals("45.5", state.movementSecText)
        assertEquals("75.3", state.recoveryMetersText)
        assertEquals("35.3", state.accuracyMetersText)
        assertEquals("20.5", state.altitudeSecText)
    }

    @Test
    fun applyTrackingPresetUpdatesPersistedAndDisplayDrafts() {
        val fixture = fixture()
        fixture.viewModel.applyTrackingPreset("standard")
        val state = fixture.viewModel.state.value
        assertEquals("standard", state.trackingProfile)
        assertEquals("2", state.normalMinText)
        assertEquals("10", state.scheduleMinText)
        assertEquals("45", state.movementSecText)
        assertEquals("75", state.recoveryMetersText)
        assertEquals("35", state.accuracyMetersText)
        assertEquals("20", state.altitudeSecText)
    }

    @Test
    fun nanTimeFieldValuesAreRejected() {
        val fixture = fixture()
        val before = fixture.trackingSettings.read()
        fixture.viewModel.updateNormalMinText("NaN")
        fixture.viewModel.updateScheduleMinText("NaN")
        fixture.viewModel.updateMovementSecText("NaN")
        fixture.viewModel.updateRecoveryMetersText("100")
        fixture.viewModel.updateAccuracyMetersText("30")
        fixture.viewModel.updateAltitudeSecText("NaN")

        assertFalse(fixture.viewModel.saveAdvancedSettings())

        val state = fixture.viewModel.state.value
        assertTrue(state.advancedErrors.containsKey("normalInterval"))
        assertTrue(state.advancedErrors.containsKey("scheduleInterval"))
        assertTrue(state.advancedErrors.containsKey("movementInterval"))
        assertFalse(state.advancedErrors.containsKey("recoveryThreshold"))
        assertFalse(state.advancedErrors.containsKey("accuracy"))
        assertTrue(state.advancedErrors.containsKey("altitudeWait"))
        assertEquals("NaN", state.normalMinText)
        assertEquals("NaN", state.scheduleMinText)
        assertEquals("NaN", state.movementSecText)
        assertEquals("100", state.recoveryMetersText)
        assertEquals("30", state.accuracyMetersText)
        assertEquals("NaN", state.altitudeSecText)
        assertEquals(before, fixture.trackingSettings.read())
    }

    @Test
    fun saveAdvancedSettingsReloadsForegroundServiceWhenCollectionActive() {
        val fixture = fixture()
        val application = ApplicationProvider.getApplicationContext<Application>()
        drainStartedServices(application)
        fixture.viewModel.setContinuousCollectionEnabled(true)

        val startIntent = shadowOf(application).nextStartedService
        assertNotNull("service must be started after enabling collection", startIntent)
        assertEquals(
            ForegroundLocationController.ACTION_START_COLLECTION,
            startIntent!!.action
        )

        fixture.viewModel.updateNormalMinText("2")
        fixture.viewModel.updateMovementSecText("45")
        fixture.viewModel.updateAccuracyMetersText("35")
        fixture.viewModel.updateAltitudeSecText("20")
        fixture.viewModel.saveAdvancedSettings()

        val reloadIntent = shadowOf(application).nextStartedService
        assertNotNull(
            "saveAdvancedSettings must reload foreground service when collection is active",
            reloadIntent
        )
        assertEquals(
            ForegroundLocationController.ACTION_START_COLLECTION,
            reloadIntent!!.action
        )
    }

    @Test
    fun applyTrackingPresetReloadsForegroundServiceWhenCollectionActive() {
        val fixture = fixture()
        val application = ApplicationProvider.getApplicationContext<Application>()
        drainStartedServices(application)
        fixture.viewModel.setContinuousCollectionEnabled(true)

        val startIntent = shadowOf(application).nextStartedService
        assertNotNull("service must be started after enabling collection", startIntent)
        assertEquals(
            ForegroundLocationController.ACTION_START_COLLECTION,
            startIntent!!.action
        )

        fixture.viewModel.applyTrackingPreset("standard")

        val reloadIntent = shadowOf(application).nextStartedService
        assertNotNull(
            "applyTrackingPreset must reload foreground service when collection is active",
            reloadIntent
        )
        assertEquals(
            ForegroundLocationController.ACTION_START_COLLECTION,
            reloadIntent!!.action
        )
    }

    @Test
    fun saveAdvancedSettingsDoesNotStartServiceWhenCollectionIsDisabled() {
        val fixture = fixture()
        val application = ApplicationProvider.getApplicationContext<Application>()
        fixture.trackingSettings.setContinuousCollectionEnabled(false)
        drainStartedServices(application)
        fixture.viewModel.updateNormalMinText("2")

        fixture.viewModel.saveAdvancedSettings()

        assertNull(shadowOf(application).nextStartedService)
    }

    @Test
    fun applyTrackingPresetDoesNotStartServiceWhenCollectionIsDisabled() {
        val fixture = fixture()
        val application = ApplicationProvider.getApplicationContext<Application>()
        fixture.trackingSettings.setContinuousCollectionEnabled(false)
        drainStartedServices(application)

        fixture.viewModel.applyTrackingPreset("standard")

        assertNull(shadowOf(application).nextStartedService)
    }

    @Test
    fun missingPermissionPreservesTrueCollectionIntent() {
        val fixture = fixture()
        val application = ApplicationProvider.getApplicationContext<Application>()
        fixture.viewModel.setContinuousCollectionEnabled(false)
        shadowOf(application).denyPermissions(Manifest.permission.POST_NOTIFICATIONS)

        fixture.viewModel.setContinuousCollectionEnabled(true)

        val state = fixture.viewModel.state.value
        assertTrue("collection intent must remain true when permission missing", state.continuousCollectionEnabled)
        assertNotNull("blocker message must be shown", state.collectionStatus)
        assertTrue(state.collectionStatus!!.contains("缺少权限"))
        assertTrue(fixture.trackingSettings.read().continuousCollectionEnabled)
    }

    @Test
    fun missingRecommendedPermissionDoesNotBlockCollection() {
        val fixture = fixture()
        val application = ApplicationProvider.getApplicationContext<Application>()
        shadowOf(application).denyPermissions(Manifest.permission.ACTIVITY_RECOGNITION)
        drainStartedServices(application)
        fixture.viewModel.setContinuousCollectionEnabled(false)
        drainStartedServices(application)

        fixture.viewModel.setContinuousCollectionEnabled(true)

        val state = fixture.viewModel.state.value
        assertTrue(state.continuousCollectionEnabled)
        val intent = shadowOf(application).nextStartedService
        assertNotNull("service must start when only recommended permission is missing", intent)
        assertEquals(ForegroundLocationController.ACTION_START_COLLECTION, intent?.action)
    }

    @Test
    fun onResumeRestartsCollectionWhenHardPermissionsRestored() {
        val fixture = fixture()
        val application = ApplicationProvider.getApplicationContext<Application>()
        drainStartedServices(application)

        shadowOf(application).denyPermissions(
            Manifest.permission.POST_NOTIFICATIONS,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_BACKGROUND_LOCATION
        )
        fixture.viewModel.onResume()
        await(1000) { fixture.viewModel.state.value.collectionStatus != null }

        val afterDeny = fixture.viewModel.state.value
        assertTrue("collection intent must persist", afterDeny.continuousCollectionEnabled)
        assertNull("service must not start when hard permissions missing", shadowOf(application).nextStartedService)
        val denySnapshot = fixture.permissionStatusRepository.snapshot()
        assertEquals(denySnapshot, afterDeny.permissions)
        assertFalse(denySnapshot.notificationGranted)
        assertFalse(denySnapshot.preciseLocationGranted)
        assertFalse(denySnapshot.backgroundLocationGranted)
        assertNotNull("blocker message must be shown", afterDeny.collectionStatus)
        assertTrue(afterDeny.collectionStatus!!.contains("通知"))
        assertTrue(afterDeny.collectionStatus!!.contains("精确定位"))
        assertTrue(afterDeny.collectionStatus!!.contains("后台定位"))

        shadowOf(application).grantPermissions(
            Manifest.permission.POST_NOTIFICATIONS,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_BACKGROUND_LOCATION
        )
        fixture.viewModel.onResume()
        await(1000) { fixture.viewModel.state.value.collectionStatus != null }

        val afterGrant = fixture.viewModel.state.value
        val grantSnapshot = fixture.permissionStatusRepository.snapshot()
        assertEquals(grantSnapshot, afterGrant.permissions)
        val intent = shadowOf(application).nextStartedService
        assertNotNull("service must restart after hard permissions restored", intent)
        assertEquals(ForegroundLocationController.ACTION_START_COLLECTION, intent?.action)
    }

    @Test
    fun onResumeDoesNotStartWhenHardPermissionStillMissing() {
        val fixture = fixture()
        val application = ApplicationProvider.getApplicationContext<Application>()
        drainStartedServices(application)

        shadowOf(application).denyPermissions(Manifest.permission.POST_NOTIFICATIONS)
        fixture.viewModel.onResume()
        await(1000) { fixture.viewModel.state.value.collectionStatus != null }

        val state = fixture.viewModel.state.value
        assertTrue("collection intent must persist", state.continuousCollectionEnabled)
        assertNull("service must not start when hard permissions missing", shadowOf(application).nextStartedService)
        val snapshot = fixture.permissionStatusRepository.snapshot()
        assertEquals(snapshot, state.permissions)
        assertFalse(snapshot.notificationGranted)
        assertNotNull("blocker message must be shown", state.collectionStatus)
        assertTrue(state.collectionStatus!!.contains("通知"))
    }

    @Test
    fun onResumePopulatesPermissionsSnapshot() {
        val fixture = fixture()
        val application = ApplicationProvider.getApplicationContext<Application>()
        shadowOf(application).denyPermissions(Manifest.permission.POST_NOTIFICATIONS)

        fixture.viewModel.onResume()

        val snapshot = fixture.permissionStatusRepository.snapshot()
        val state = fixture.viewModel.state.value
        assertEquals(snapshot, state.permissions)
        assertFalse(snapshot.notificationGranted)
    }

    @Test
    fun onResumeStartsServiceWhenOnlyActivityRecognitionMissing() {
        val fixture = fixture()
        val application = ApplicationProvider.getApplicationContext<Application>()
        drainStartedServices(application)

        shadowOf(application).denyPermissions(Manifest.permission.ACTIVITY_RECOGNITION)
        fixture.viewModel.onResume()

        val state = fixture.viewModel.state.value
        assertTrue(state.continuousCollectionEnabled)
        val intent = shadowOf(application).nextStartedService
        assertNotNull("service must start when only activity recognition is missing", intent)
        assertEquals(ForegroundLocationController.ACTION_START_COLLECTION, intent?.action)
    }

    @Test
    fun automaticSettingsProbeIsSilentOnSuccess() {
        val now = System.currentTimeMillis()
        val fixture = fixture()
        val fresh = successfulProbe(SERVER_A_URL).copy(checkedAtUtcMillis = now)
        fixture.probeStore.save(fresh)

        fixture.viewModel.refresh()

        val state = fixture.viewModel.state.value
        assertNull("auto probe must not set apiStatus on success", state.apiStatus)
    }

    @Test
    fun automaticSettingsProbeIsSilentOnFailure() {
        val fixture = fixture(probeFailure = IllegalStateException("SECRET_URL_OR_PATH"))
        fixture.viewModel.refresh()
        mainDispatcher.scheduler.advanceUntilIdle()
        val state = fixture.viewModel.state.value
        assertNull("auto probe must not set apiStatus on failure", state.apiStatus)
    }

    @Test
    fun `login failure does not leak raw exception message`() {
        val fixture = fixture(
            loginTransport = { _, _ -> throw IllegalStateException("SECRET_URL_OR_PATH") }
        )
        fixture.viewModel.login("valid_user", "valid_pass")
        mainDispatcher.scheduler.advanceUntilIdle()

        val state = fixture.viewModel.state.value
        assertNotNull(state.loginStatus)
        assertFalse(
            "loginStatus must not contain raw exception message",
            state.loginStatus!!.contains("SECRET_URL_OR_PATH")
        )
        assertEquals("登录失败，请重试", state.loginStatus)
    }

    @Test
    fun initializationDoesNotDuplicateVisibleScreenProbe() {
        var probeAttempts = 0
        val fixture = fixture(
            probeFailure = IllegalStateException("stop after counting"),
            onProbeStarted = { probeAttempts++ }
        )

        assertEquals(0, probeAttempts)

        fixture.viewModel.refresh()

        assertEquals(1, probeAttempts)
    }

    @Test
    fun onResumeRefreshesPermissionsOnlyWhenIntentFalse() {
        val fixture = fixture()
        val application = ApplicationProvider.getApplicationContext<Application>()
        fixture.trackingSettings.setContinuousCollectionEnabled(false)
        drainStartedServices(application)

        fixture.viewModel.onResume()

        val snapshot = fixture.permissionStatusRepository.snapshot()
        val state = fixture.viewModel.state.value
        assertEquals(snapshot, state.permissions)
        assertFalse(state.continuousCollectionEnabled)
        assertNull(shadowOf(application).nextStartedService)
    }

    @Test
    fun requestClearDiagnosticsShowsConfirmation() {
        val fixture = fixture()
        fixture.viewModel.requestClearDiagnostics()
        assertTrue(fixture.viewModel.state.value.showClearDiagnosticsConfirmation)
    }

    @Test
    fun dismissClearDiagnosticsConfirmationHidesDialog() {
        val fixture = fixture()
        fixture.viewModel.requestClearDiagnostics()
        assertTrue(fixture.viewModel.state.value.showClearDiagnosticsConfirmation)
        fixture.viewModel.dismissClearDiagnosticsConfirmation()
        assertFalse(fixture.viewModel.state.value.showClearDiagnosticsConfirmation)
    }

    @Test
    fun confirmClearDiagnosticsOnSuccessClearsAndSetsFeedback() {
        val fixture = fixture()
        fixture.viewModel.requestClearDiagnostics()
        fixture.viewModel.confirmClearDiagnostics()
        val state = fixture.viewModel.state.value
        assertFalse(state.showClearDiagnosticsConfirmation)
        assertFalse(state.isBusy)
        assertFalse(state.isClearingDiagnostics)
        assertEquals(DiagnosticClearFeedback.Cleared, state.diagnosticClearFeedback)
        assertEquals(1, fixture.diagnosticOperations.clearCallCount)
    }

    @Test
    fun confirmClearDiagnosticsOnExceptionSetsFailedFeedback() {
        val fixture = fixture(diagnosticClearFails = true)
        fixture.viewModel.requestClearDiagnostics()
        fixture.viewModel.confirmClearDiagnostics()
        val state = fixture.viewModel.state.value
        assertFalse(state.showClearDiagnosticsConfirmation)
        assertFalse(state.isBusy)
        assertFalse(state.isClearingDiagnostics)
        assertEquals(DiagnosticClearFeedback.Failed, state.diagnosticClearFeedback)
        assertEquals(1, fixture.diagnosticOperations.clearCallCount)
    }

    @Test
    fun confirmClearDiagnosticsWhenAlreadyBusyDoesNotDuplicateCall() {
        val fixture = fixture()
        fixture.diagnosticOperations.clearContinue = CompletableDeferred()
        fixture.viewModel.requestClearDiagnostics()
        fixture.viewModel.confirmClearDiagnostics()
        assertTrue(fixture.viewModel.state.value.isBusy)
        assertTrue(fixture.viewModel.state.value.isClearingDiagnostics)
        fixture.viewModel.confirmClearDiagnostics()
        assertEquals(1, fixture.diagnosticOperations.clearCallCount)
        fixture.diagnosticOperations.clearContinue?.complete(Unit)
    }

    @Test
    fun onResumeCallsEnsureRunningStateEvenWhenIntentFalse() {
        var ensureCalled = false
        val fixture = fixture(onEnsureRunningState = { ensureCalled = true })
        fixture.trackingSettings.setContinuousCollectionEnabled(false)
        fixture.viewModel.onResume()
        mainDispatcher.scheduler.advanceUntilIdle()
        assertTrue("ensureRunningState must be called even when intent is false", ensureCalled)
    }

    @Test
    fun toDisplayPermissionNameMapsUnknownCodeToSafeFallback() {
        assertEquals("必要权限", toDisplayPermissionName("some_unknown_code"))
    }

    private fun drainStartedServices(application: Application) {
        while (shadowOf(application).nextStartedService != null) {
            // Drain intents left by earlier actions in the shared Robolectric application.
        }
    }

    private fun await(timeoutMs: Long, condition: () -> Boolean) {
        val deadline = System.currentTimeMillis() + timeoutMs
        while (System.currentTimeMillis() < deadline) {
            if (condition()) return
            shadowOf(Looper.getMainLooper()).idle()
            mainDispatcher.scheduler.advanceUntilIdle()
            Thread.sleep(5)
        }
    }

    private fun fixture(
        failSessionClear: Boolean = false,
        probeFailure: Throwable? = null,
        onProbeStarted: () -> Unit = {},
        diagnosticClearFails: Boolean = false,
        onEnsureRunningState: () -> Unit = {},
        isServiceRunning: () -> Boolean = { ForegroundLocationService.isRunning() },
        loginTransport: ServerBoundLoginTransport? = null
    ): Fixture {
        val cacheDir = File(context.filesDir, "settings-cache-test-" + System.nanoTime())
        cacheDir.mkdirs()
        cacheDirs.add(cacheDir)
        val serverPreferences = ScriptedCommitSharedPreferences(
            context.getSharedPreferences(SERVER_PREFS, Context.MODE_PRIVATE)
        )
        val authPreferences = ScriptedCommitSharedPreferences(
            context.getSharedPreferences(AUTH_PREFS, Context.MODE_PRIVATE)
        )
        val securePreferencesFactory = TestSecurePreferencesFactory(preferences = authPreferences)
        val tokenManager = TokenManager(securePreferencesFactory, nowMillis = { 1_000L })
        val serverSettings = ServerSettingsStore(
            SharedPreferencesContext(context, serverPreferences),
            tokenManager
        )
        serverSettings.setBaseUrl(SERVER_A_URL)
        check(tokenManager.save("access-a", "refresh-a", Long.MAX_VALUE, SERVER_A_IDENTITY))
        if (failSessionClear) authPreferences.enqueueCommitResult(false)

        val trackingSettings = TrackingSettingsStore(
            context.getSharedPreferences(TRACKING_PREFS, Context.MODE_PRIVATE)
        )
        trackingSettings.setContinuousCollectionEnabled(true)
        val transport = loginTransport ?: ServerBoundLoginTransport { _, _ -> error("login transport is not used") }
        val coordinator = ServerBoundLoginCoordinator(
            serverSettings,
            tokenManager,
            transport
        )
        val probeStore = ConnectionProbeStore(
            context.getSharedPreferences(PROBE_PREFS, Context.MODE_PRIVATE),
            Json { ignoreUnknownKeys = true }
        )
        val probeService = ConnectionProbeService(
            anonymousClient = OkHttpClient(),
            authenticatedClient = OkHttpClient(),
            tokenSource = ProbeTokenSource { null },
            wallClockMillis = {
                onProbeStarted()
                if (probeFailure != null) throw probeFailure else 1_000L
            },
            monotonicNanos = { 0L }
        )
        val mobileSyncScheduler = MobileSyncScheduler(context, trackingSettings)
        val permissionStatusRepository = PermissionStatusRepository(
            context,
            UsageAccessChecker(context)
        )
        val foregroundLocationController = ForegroundLocationController(context)
        val diagnosticOperations = FakeDiagnosticOperations().also { ops ->
            ops.shouldFail = diagnosticClearFails
        }
        val structuredLogRepository = StructuredLogRepository(
            context,
            trackingSettings
        ) { System.currentTimeMillis() }
        val runningStateRestorer = RunningStateRestorer(
            trackingSettingsStore = trackingSettings,
            permissionStatusRepository = permissionStatusRepository,
            structuredLogRepository = structuredLogRepository,
            cancelLegacySyncWork = {},
            ensurePeriodicSync = {
                mobileSyncScheduler.ensurePeriodic()
                onEnsureRunningState()
            },
            isServiceRunning = isServiceRunning,
            startCollection = { foregroundLocationController.start() }
        )
        val scheduleCacheStore = ScheduleCacheStore(cacheDir, Json { ignoreUnknownKeys = true })
        val viewModel = SettingsViewModel(
            serverSettingsStore = serverSettings,
            tokenManager = tokenManager,
            serverBoundLoginCoordinator = coordinator,
            trackingSettingsStore = trackingSettings,
            foregroundLocationController = foregroundLocationController,
            permissionStatusRepository = permissionStatusRepository,
            connectionProbeService = probeService,
            connectionProbeStore = probeStore,
            mobileSyncScheduler = mobileSyncScheduler,
            diagnosticOperations = diagnosticOperations,
            runningStateRestorer = runningStateRestorer,
            scheduleCacheStore = scheduleCacheStore
        )
        return Fixture(
            viewModel = viewModel,
            serverSettings = serverSettings,
            tokenManager = tokenManager,
            trackingSettings = trackingSettings,
            serverPreferences = serverPreferences,
            mobileSyncScheduler = mobileSyncScheduler,
            permissionStatusRepository = permissionStatusRepository,
            probeStore = probeStore,
            diagnosticOperations = diagnosticOperations,
            scheduleCacheStore = scheduleCacheStore
        )
    }

    private fun successfulProbe(serverUrl: String): ConnectionProbeResult {
        return ConnectionProbeResult(
            outcome = ConnectionProbeOutcome.Reachable,
            checkedAtUtcMillis = 1_000L,
            serverIdentity = PimServerEndpoints.from(serverUrl).apiBaseUrl.toString(),
            lastCompletedStage = ConnectionProbeStage.WebRoot,
            latencyMillisByStage = emptyMap(),
            capabilities = ServerCapabilities(
                mobileItemResultsV1 = true,
                androidEmbedV1 = true
            )
        )
    }

    private data class Fixture(
        val viewModel: SettingsViewModel,
        val serverSettings: ServerSettingsStore,
        val tokenManager: TokenManager,
        val trackingSettings: TrackingSettingsStore,
        val serverPreferences: ScriptedCommitSharedPreferences,
        val mobileSyncScheduler: MobileSyncScheduler,
        val permissionStatusRepository: PermissionStatusRepository,
        val probeStore: ConnectionProbeStore,
        val diagnosticOperations: FakeDiagnosticOperations,
        val scheduleCacheStore: ScheduleCacheStore
    )

    private companion object {
        const val SERVER_PREFS = "settings_server_mutation_server"
        const val AUTH_PREFS = "settings_server_mutation_auth"
        const val TRACKING_PREFS = "settings_server_mutation_tracking"
        const val PROBE_PREFS = "settings_server_mutation_probe"
        const val SERVER_A_URL = "https://server-a.example/api/v1/"
        const val SERVER_B_URL = "https://server-b.example/api/v1/"
        const val SERVER_A_IDENTITY = "https://server-a.example"
        val SERVER_A_CACHE_IDENTITY = PimServerEndpoints.from(SERVER_A_URL).apiBaseUrl.toString()
        val SERVER_B_CACHE_IDENTITY = PimServerEndpoints.from(SERVER_B_URL).apiBaseUrl.toString()
    }
}

private class FakeDiagnosticOperations : DiagnosticOperations {
    var clearCallCount = 0
    var shouldFail = false
    var clearContinue: CompletableDeferred<Unit>? = null

    override suspend fun export(includeRecentLocations: Boolean): DiagnosticExportResult {
        return DiagnosticExportResult(File(""), 0)
    }

    override suspend fun clearDiagnostics() {
        clearCallCount++
        clearContinue?.await()
        if (shouldFail) throw RuntimeException("simulated clear error")
    }
}

private class SharedPreferencesContext(
    base: Context,
    private val preferences: SharedPreferences
) : ContextWrapper(base) {
    override fun getSharedPreferences(name: String?, mode: Int): SharedPreferences = preferences
}

private class TestSecurePreferencesFactory(
    private val preferences: SharedPreferences
) : SecurePreferencesFactory {
    override fun open(): SharedPreferences = preferences
}

private class ScriptedCommitSharedPreferences(
    private val delegate: SharedPreferences
) : SharedPreferences {
    private val commitResults = ArrayDeque<Boolean>()

    fun enqueueCommitResult(result: Boolean) {
        commitResults.addLast(result)
    }

    override fun getAll() = delegate.getAll()
    override fun getString(key: String, defValue: String?) = delegate.getString(key, defValue)
    override fun getStringSet(key: String, defValues: MutableSet<String>?) = delegate.getStringSet(key, defValues)
    override fun getInt(key: String, defValue: Int) = delegate.getInt(key, defValue)
    override fun getLong(key: String, defValue: Long) = delegate.getLong(key, defValue)
    override fun getFloat(key: String, defValue: Float) = delegate.getFloat(key, defValue)
    override fun getBoolean(key: String, defValue: Boolean) = delegate.getBoolean(key, defValue)
    override fun contains(key: String) = delegate.contains(key)
    override fun edit(): SharedPreferences.Editor = ScriptedEditor(delegate.edit())
    override fun registerOnSharedPreferenceChangeListener(listener: SharedPreferences.OnSharedPreferenceChangeListener?) =
        delegate.registerOnSharedPreferenceChangeListener(listener)
    override fun unregisterOnSharedPreferenceChangeListener(listener: SharedPreferences.OnSharedPreferenceChangeListener?) =
        delegate.unregisterOnSharedPreferenceChangeListener(listener)

    private inner class ScriptedEditor(
        private val delegate: SharedPreferences.Editor
    ) : SharedPreferences.Editor {
        override fun putString(key: String, value: String?): SharedPreferences.Editor = apply {
            delegate.putString(key, value)
        }

        override fun putStringSet(
            key: String,
            values: MutableSet<String>?
        ): SharedPreferences.Editor = apply {
            delegate.putStringSet(key, values)
        }

        override fun putInt(key: String, value: Int): SharedPreferences.Editor = apply {
            delegate.putInt(key, value)
        }

        override fun putLong(key: String, value: Long): SharedPreferences.Editor = apply {
            delegate.putLong(key, value)
        }

        override fun putFloat(key: String, value: Float): SharedPreferences.Editor = apply {
            delegate.putFloat(key, value)
        }

        override fun putBoolean(key: String, value: Boolean): SharedPreferences.Editor = apply {
            delegate.putBoolean(key, value)
        }

        override fun remove(key: String): SharedPreferences.Editor = apply {
            delegate.remove(key)
        }

        override fun clear(): SharedPreferences.Editor = apply {
            delegate.clear()
        }

        override fun commit(): Boolean {
            val shouldCommit = if (commitResults.isEmpty()) true else commitResults.removeFirst()
            if (!shouldCommit) return false
            return delegate.commit()
        }

        override fun apply() {
            delegate.apply()
        }
    }
}
