package com.pim.app.recovery

import android.Manifest
import android.app.Application
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.testing.WorkManagerTestInitHelper
import com.pim.app.TestPimApp
import com.pim.app.location.service.ForegroundLocationController
import com.pim.app.location.service.ForegroundLocationService
import com.pim.app.mobile.logs.StructuredLogRepository
import com.pim.app.mobile.sync.MobileSyncScheduler
import com.pim.app.mobile.usage.UsageAccessChecker
import com.pim.app.permissions.PermissionStatusRepository
import com.pim.app.settings.TrackingSettingsStore
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.test.runTest
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

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = TestPimApp::class)
class RunningStateRestorerTest {

    private lateinit var app: Application
    private lateinit var prefs: android.content.SharedPreferences
    private lateinit var store: TrackingSettingsStore
    private lateinit var logRepo: StructuredLogRepository

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        app = ApplicationProvider.getApplicationContext()
        WorkManagerTestInitHelper.initializeTestWorkManager(context)
        prefs = context.getSharedPreferences("restorer_test", Context.MODE_PRIVATE)
        prefs.edit().clear().apply()
        store = TrackingSettingsStore(prefs)
        File(context.filesDir, "logs").deleteRecursively()
        drainStartedServices()
    }

    @After
    fun tearDown() {
        drainStartedServices()
        File(app.filesDir, "logs").deleteRecursively()
    }

    private fun createRestorer(
        cancelLegacySyncWork: () -> Unit = {
            val ctx = ApplicationProvider.getApplicationContext<Context>()
            MobileSyncScheduler(ctx, store).cancelOldWork()
        },
        ensurePeriodicSync: () -> Unit = {
            val ctx = ApplicationProvider.getApplicationContext<Context>()
            MobileSyncScheduler(ctx, store).ensurePeriodic()
        },
        isServiceRunning: () -> Boolean = { ForegroundLocationService.isRunning() },
        startCollection: () -> Unit = {
            val ctx = ApplicationProvider.getApplicationContext<Context>()
            ForegroundLocationController(ctx).start()
        }
    ): RunningStateRestorer {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val permissionRepo = PermissionStatusRepository(context, UsageAccessChecker(context))
        logRepo = StructuredLogRepository(context, store) { 1000000L }
        return RunningStateRestorer(
            trackingSettingsStore = store,
            permissionStatusRepository = permissionRepo,
            structuredLogRepository = logRepo,
            cancelLegacySyncWork = cancelLegacySyncWork,
            ensurePeriodicSync = ensurePeriodicSync,
            isServiceRunning = isServiceRunning,
            startCollection = startCollection
        )
    }

    @Test
    fun `collection disabled schedules periodic work and does not start service`() = runTest {
        store.setContinuousCollectionEnabled(false)
        val restorer = createRestorer()
        drainStartedServices()

        val result = restorer.ensureRunningState()

        assertTrue("syncScheduled must be true", result.syncScheduled)
        assertEquals(CollectionState.Disabled, result.collectionState)

        val workInfos = WorkManager.getInstance(app)
            .getWorkInfosForUniqueWork(MobileSyncScheduler.PERIODIC_NAME).get()
        assertTrue("Periodic work must be enqueued", workInfos.any { it.state == WorkInfo.State.ENQUEUED })

        assertNull("Service must not be started when collection disabled", shadowOf(app).nextStartedService)
    }

    @Test
    fun `collection enabled with all hard permissions requests start`() = runTest {
        grantHardPermissions()
        store.setContinuousCollectionEnabled(true)
        val restorer = createRestorer()
        drainStartedServices()

        val result = restorer.ensureRunningState()

        assertTrue("syncScheduled must be true", result.syncScheduled)
        assertEquals(CollectionState.StartRequested, result.collectionState)

        val intent = shadowOf(app).nextStartedService
        assertNotNull("Service must be started", intent)
        assertEquals(ForegroundLocationController.ACTION_START_COLLECTION, intent?.action)
    }

    @Test
    fun `missing hard permissions blocks with detail list and chinese log`() = runTest {
        store.setContinuousCollectionEnabled(true)
        val restorer = createRestorer()
        drainStartedServices()

        val result = restorer.ensureRunningState()

        assertEquals(CollectionState.Blocked, result.collectionState)
        assertEquals("missing-hard-permissions", result.detail)
        assertEquals(listOf("notification", "precise_location", "background_location"), result.missingPermissions)
        assertNull("Service must not start when blocked", shadowOf(app).nextStartedService)
        assertTrue("continuousCollectionEnabled must remain true", store.read().continuousCollectionEnabled)

        val logs = logRepo.recent(10)
        val blockedLog = logs.find { it.level == "warn" && it.message.contains("缺少必需权限") }
        assertNotNull("Blocked state must log Chinese warning", blockedLog)
        assertEquals("running-state-recovery", blockedLog?.tag)
    }

    @Test
    fun `repeated ensureRunningState leaves single periodic work`() = runTest {
        store.setContinuousCollectionEnabled(false)
        val restorer = createRestorer()

        restorer.ensureRunningState()
        restorer.ensureRunningState()
        restorer.ensureRunningState()

        val workInfos = WorkManager.getInstance(app)
            .getWorkInfosForUniqueWork(MobileSyncScheduler.PERIODIC_NAME).get()
        val active = workInfos.filter { it.state == WorkInfo.State.ENQUEUED }
        assertEquals(1, active.size)
    }

    @Test
    fun `service already running does not resubmit start`() = runTest {
        grantHardPermissions()
        store.setContinuousCollectionEnabled(true)
        val restorer = createRestorer(isServiceRunning = { true })
        drainStartedServices()

        val result = restorer.ensureRunningState()

        assertEquals(CollectionState.AlreadyRunning, result.collectionState)
        assertNull("Must not send start intent when already running", shadowOf(app).nextStartedService)
    }

    @Test
    fun `start failure keeps setting logs chinese error`() = runTest {
        grantHardPermissions()
        store.setContinuousCollectionEnabled(true)
        val restorer = createRestorer(
            isServiceRunning = { false },
            startCollection = { throw RuntimeException("start failed") }
        )
        drainStartedServices()

        val result = restorer.ensureRunningState()

        assertEquals(CollectionState.Failed, result.collectionState)
        assertEquals("service-start-failed", result.detail)
        assertTrue("Setting must remain true after failure", store.read().continuousCollectionEnabled)

        val logs = logRepo.recent(10)
        val failedLog = logs.find { it.level == "error" && it.message.contains("启动采集服务失败") }
        assertNotNull("Failed state must log Chinese error", failedLog)
        assertEquals("running-state-recovery", failedLog?.tag)
    }

    @Test
    fun `cancelLegacySyncWork exception still calls ensurePeriodic`() = runTest {
        var ensurePeriodicCalled = false
        store.setContinuousCollectionEnabled(false)
        val restorer = createRestorer(
            cancelLegacySyncWork = { throw RuntimeException("legacy failed") },
            ensurePeriodicSync = { ensurePeriodicCalled = true }
        )

        val result = restorer.ensureRunningState()

        assertTrue("ensurePeriodic must be called even if cancelLegacy fails", ensurePeriodicCalled)
        assertTrue("syncScheduled must be true when ensurePeriodic succeeds", result.syncScheduled)
        assertEquals(CollectionState.Disabled, result.collectionState)
    }

    @Test
    fun `ensurePeriodicSync exception still processes collection state`() = runTest {
        grantHardPermissions()
        store.setContinuousCollectionEnabled(true)
        val restorer = createRestorer(
            cancelLegacySyncWork = {},
            ensurePeriodicSync = { throw RuntimeException("periodic failed") },
            isServiceRunning = { false },
            startCollection = {}
        )
        drainStartedServices()

        val result = restorer.ensureRunningState()

        assertFalse("syncScheduled must be false when ensurePeriodic fails", result.syncScheduled)
        assertEquals(CollectionState.StartRequested, result.collectionState)
    }

    @Test
    fun `service running check exception returns safe detail`() = runTest {
        grantHardPermissions()
        store.setContinuousCollectionEnabled(true)
        val restorer = createRestorer(
            isServiceRunning = { throw RuntimeException("check failed") }
        )

        val result = restorer.ensureRunningState()

        assertEquals(CollectionState.Failed, result.collectionState)
        assertEquals("service-state-read-failed", result.detail)
    }

    @Test
    fun `CancellationException from cancelLegacySyncWork propagates`() = runTest {
        store.setContinuousCollectionEnabled(false)
        val restorer = createRestorer(
            cancelLegacySyncWork = { throw CancellationException("cancelled") }
        )

        try {
            restorer.ensureRunningState()
            assertTrue("Should have thrown CancellationException", false)
        } catch (_: CancellationException) {
        }
    }

    private fun grantHardPermissions() {
        shadowOf(app).grantPermissions(
            Manifest.permission.POST_NOTIFICATIONS,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_BACKGROUND_LOCATION
        )
    }

    private fun drainStartedServices() {
        while (shadowOf(app).nextStartedService != null) { }
    }
}
