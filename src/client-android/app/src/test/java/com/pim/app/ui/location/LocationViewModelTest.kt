package com.pim.app.ui.location

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import com.pim.app.TestPimApp
import com.pim.app.location.LocationSnapshot
import com.pim.app.location.acquisition.AcquisitionPhase
import com.pim.app.location.acquisition.LocationAcquisitionState
import com.pim.app.location.acquisition.LocationEngineCompletion
import com.pim.app.location.acquisition.LocationEngineResult
import com.pim.app.location.acquisition.SessionStartResult
import com.pim.app.location.acquisition.TriggerType
import com.pim.app.location.acquisition.FakeLocationAcquisitionRunner
import com.pim.app.location.acquisition.FakePrerequisiteChecker
import com.pim.app.location.acquisition.LocationAcquisitionCoordinator
import com.pim.app.location.acquisition.TestLocationAcquisitionOperations
import com.pim.app.location.service.ForegroundLocationController
import com.pim.app.settings.TrackingSettingsStore
import com.pim.app.status.QueueStatusRepository
import com.pim.app.status.QueueStatusSnapshot
import com.pim.app.testing.InMemorySharedPreferences
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.serialization.json.Json
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

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = TestPimApp::class)
class LocationViewModelTest {

    private val mainDispatcher = UnconfinedTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(mainDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `initial idle state shows start button and correct labels`() {
        val state = mapToLocationUiState(
            acqState = LocationAcquisitionState(),
            queueSnapshot = QueueStatusSnapshot(0, 0, 0, 0, 0, 0)
        )

        assertEquals("尚未开始", state.triggerLabel)
        assertEquals("空闲", state.phaseLabel)
        assertEquals("最长 30 秒", state.deadlineText)
        assertEquals("0 秒", state.elapsedText)
        assertNull(state.bestLocation)
        assertEquals(0, state.pendingUploadTotal)
        assertEquals(0, state.pendingLocationPoints)
        assertNull(state.errorMessage)
        assertTrue(state.showStart)
        assertFalse(state.showCancel)
        assertFalse(state.showRestart)
        assertFalse(state.showOpenSettings)
        assertFalse(state.showLowQualityWarning)
        assertTrue(state.manualStartEnabled)
    }

    @Test
    fun `manual session preparing shows cancel button`() {
        val state = mapToLocationUiState(
            acqState = LocationAcquisitionState(
                sessionId = "s1",
                triggerType = TriggerType.MANUAL,
                phase = AcquisitionPhase.Preparing,
                startedAtElapsedRealtimeMs = 1000L,
                deadlineAtElapsedRealtimeMs = 31000L,
                elapsedMs = 500L
            ),
            queueSnapshot = QueueStatusSnapshot(0, 0, 0, 0, 0, 0)
        )

        assertEquals("手动定位", state.triggerLabel)
        assertEquals("准备中", state.phaseLabel)
        assertFalse(state.showStart)
        assertTrue(state.showCancel)
        assertFalse(state.showRestart)
    }

    @Test
    fun `manual session acquiring shows cancel button`() {
        val state = mapToLocationUiState(
            acqState = LocationAcquisitionState(
                sessionId = "s1",
                triggerType = TriggerType.MANUAL,
                phase = AcquisitionPhase.Acquiring,
                startedAtElapsedRealtimeMs = 1000L,
                deadlineAtElapsedRealtimeMs = 31000L,
                elapsedMs = 1500L
            ),
            queueSnapshot = QueueStatusSnapshot(0, 0, 0, 0, 0, 0)
        )

        assertEquals("手动定位", state.triggerLabel)
        assertEquals("采集位置中", state.phaseLabel)
        assertTrue(state.showCancel)
        assertFalse(state.showStart)
        assertFalse(state.showRestart)
    }

    @Test
    fun `manual session evaluating shows cancel button`() {
        val state = mapToLocationUiState(
            acqState = LocationAcquisitionState(
                sessionId = "s1",
                triggerType = TriggerType.MANUAL,
                phase = AcquisitionPhase.Evaluating,
                startedAtElapsedRealtimeMs = 1000L,
                deadlineAtElapsedRealtimeMs = 31000L,
                elapsedMs = 2500L
            ),
            queueSnapshot = QueueStatusSnapshot(0, 0, 0, 0, 0, 0)
        )

        assertEquals("评估中", state.phaseLabel)
        assertTrue(state.showCancel)
    }

    @Test
    fun `completed with low-quality flag shows the low-quality warning`() {
        val state = mapToLocationUiState(
            acqState = LocationAcquisitionState(
                sessionId = "s1",
                triggerType = TriggerType.MANUAL,
                phase = AcquisitionPhase.Completed,
                startedAtElapsedRealtimeMs = 1000L,
                deadlineAtElapsedRealtimeMs = 31000L,
                elapsedMs = 5000L,
                lastQualityFlags = setOf("low-quality-accuracy"),
                bestLocation = LocationSnapshot(
                    latitude = 39.9042,
                    longitude = 116.4074,
                    horizontalAccuracyMeters = 45f,
                    provider = "fused",
                    source = "manual",
                    altitudeMeters = 50.0,
                    speedMetersPerSecond = 1.2f,
                    bearingDegrees = 180f,
                    timeMillis = 1000000L
                )
            ),
            queueSnapshot = QueueStatusSnapshot(0, 0, 0, 0, 0, 0)
        )

        assertEquals("已完成", state.phaseLabel)
        assertTrue(state.showRestart)
        assertFalse(state.showCancel)
        assertTrue(state.showLowQualityWarning)
    }

    @Test
    fun `completed phase shows restart button`() {
        val state = mapToLocationUiState(
            acqState = LocationAcquisitionState(
                sessionId = "s1",
                triggerType = TriggerType.MANUAL,
                phase = AcquisitionPhase.Completed,
                startedAtElapsedRealtimeMs = 1000L,
                deadlineAtElapsedRealtimeMs = 31000L,
                elapsedMs = 7000L
            ),
            queueSnapshot = QueueStatusSnapshot(0, 0, 0, 0, 0, 0)
        )

        assertEquals("已完成", state.phaseLabel)
        assertTrue(state.showRestart)
        assertFalse(state.showCancel)
        assertFalse(state.showLowQualityWarning)
    }

    @Test
    fun `cancelled phase shows restart button`() {
        val state = mapToLocationUiState(
            acqState = LocationAcquisitionState(
                sessionId = "s1",
                triggerType = TriggerType.MANUAL,
                phase = AcquisitionPhase.Cancelled,
                startedAtElapsedRealtimeMs = 1000L,
                deadlineAtElapsedRealtimeMs = 31000L,
                elapsedMs = 4000L
            ),
            queueSnapshot = QueueStatusSnapshot(0, 0, 0, 0, 0, 0)
        )

        assertEquals("已取消", state.phaseLabel)
        assertTrue(state.showRestart)
        assertFalse(state.showCancel)
    }

    @Test
    fun `failed phase shows restart button and error message`() {
        val state = mapToLocationUiState(
            acqState = LocationAcquisitionState(
                sessionId = "s1",
                triggerType = TriggerType.MANUAL,
                phase = AcquisitionPhase.Failed,
                errorReason = "定位失败：无法获取位置",
                startedAtElapsedRealtimeMs = 1000L,
                deadlineAtElapsedRealtimeMs = 31000L,
                elapsedMs = 8000L
            ),
            queueSnapshot = QueueStatusSnapshot(0, 0, 0, 0, 0, 0)
        )

        assertEquals("失败", state.phaseLabel)
        assertEquals("定位失败：无法获取位置", state.errorMessage)
        assertTrue(state.showRestart)
    }

    @Test
    fun `timed out phase shows restart button`() {
        val state = mapToLocationUiState(
            acqState = LocationAcquisitionState(
                sessionId = "s1",
                triggerType = TriggerType.MANUAL,
                phase = AcquisitionPhase.TimedOut,
                startedAtElapsedRealtimeMs = 1000L,
                deadlineAtElapsedRealtimeMs = 31000L,
                elapsedMs = 30000L
            ),
            queueSnapshot = QueueStatusSnapshot(0, 0, 0, 0, 0, 0)
        )

        assertEquals("超时", state.phaseLabel)
        assertTrue(state.showRestart)
    }

    @Test
    fun `auto session shows auto labels and disabled start`() {
        val state = mapToLocationUiState(
            acqState = LocationAcquisitionState(
                sessionId = "s2",
                triggerType = TriggerType.AUTOMATIC,
                phase = AcquisitionPhase.Acquiring,
                startedAtElapsedRealtimeMs = 1000L,
                deadlineAtElapsedRealtimeMs = 31000L,
                elapsedMs = 2000L
            ),
            queueSnapshot = QueueStatusSnapshot(0, 0, 0, 0, 0, 0)
        )

        assertEquals("自动定位", state.triggerLabel)
        assertFalse(state.manualStartEnabled)
        assertTrue(state.showStart)
        assertTrue(state.showCancel)
    }

    @Test
    fun `auto session in every busy phase keeps start visible but disabled`() {
        for (phase in listOf(
            AcquisitionPhase.Preparing,
            AcquisitionPhase.Acquiring,
            AcquisitionPhase.Evaluating
        )) {
            val state = mapToLocationUiState(
                acqState = LocationAcquisitionState(
                    sessionId = "s2",
                    triggerType = TriggerType.AUTOMATIC,
                    phase = phase,
                    startedAtElapsedRealtimeMs = 1000L,
                    deadlineAtElapsedRealtimeMs = 31000L,
                    elapsedMs = 2000L
                ),
                queueSnapshot = QueueStatusSnapshot(0, 0, 0, 0, 0, 0)
            )

            assertTrue("auto $phase must keep start visible", state.showStart)
            assertFalse("auto $phase must disable start", state.manualStartEnabled)
            assertTrue("auto $phase must show cancel", state.showCancel)
        }
    }

    @Test
    fun `auto session idle does not disable manual start`() {
        val state = mapToLocationUiState(
            acqState = LocationAcquisitionState(phase = AcquisitionPhase.Idle),
            queueSnapshot = QueueStatusSnapshot(0, 0, 0, 0, 0, 0)
        )

        assertTrue(state.manualStartEnabled)
    }

    @Test
    fun `pending counts from queue status`() {
        val state = mapToLocationUiState(
            acqState = LocationAcquisitionState(),
            queueSnapshot = QueueStatusSnapshot(
                pendingLocationPoints = 5,
                pendingUsageEvents = 3,
                pendingUsageSummaries = 2,
                pendingAppMetadata = 1,
                pendingDeviceProfile = 1,
                pendingSyncBatches = 0
            )
        )

        assertEquals(5, state.pendingLocationPoints)
        assertEquals(12, state.pendingUploadTotal)
    }

    @Test
    fun `best location maps all fields`() {
        val state = mapToLocationUiState(
            acqState = LocationAcquisitionState(
                sessionId = "s1",
                triggerType = TriggerType.MANUAL,
                phase = AcquisitionPhase.Completed,
                bestLocation = LocationSnapshot(
                    latitude = 39.9042,
                    longitude = 116.4074,
                    horizontalAccuracyMeters = 15f,
                    provider = "gps",
                    source = "manual",
                    altitudeMeters = 52.0,
                    speedMetersPerSecond = 2.5f,
                    bearingDegrees = 90f,
                    timeMillis = 2000000L
                ),
                startedAtElapsedRealtimeMs = 1000L,
                deadlineAtElapsedRealtimeMs = 31000L,
                elapsedMs = 5000L
            ),
            queueSnapshot = QueueStatusSnapshot(0, 0, 0, 0, 0, 0)
        )

        val best = state.bestLocation!!
        assertEquals(39.9042, best.latitude, 0.0001)
        assertEquals(116.4074, best.longitude, 0.0001)
        assertEquals(15f, best.horizontalAccuracyMeters!!, 0.01f)
        assertEquals("gps", best.provider)
        assertEquals(52.0, best.altitudeMeters!!, 0.1)
        assertEquals(2.5f, best.speedMetersPerSecond!!, 0.01f)
        assertEquals(90f, best.bearingDegrees!!, 0.01f)
        assertEquals(2000000L, best.timeMillis)
    }

    @Test
    fun `best location partial null fields does not crash`() {
        val state = mapToLocationUiState(
            acqState = LocationAcquisitionState(
                sessionId = "s1",
                triggerType = TriggerType.MANUAL,
                phase = AcquisitionPhase.Completed,
                bestLocation = LocationSnapshot(
                    latitude = 39.9042,
                    longitude = 116.4074,
                    horizontalAccuracyMeters = null,
                    provider = "gps",
                    source = "manual",
                    altitudeMeters = null,
                    speedMetersPerSecond = null,
                    bearingDegrees = null,
                    timeMillis = 3000000L
                ),
                startedAtElapsedRealtimeMs = 1000L,
                deadlineAtElapsedRealtimeMs = 31000L,
                elapsedMs = 5000L
            ),
            queueSnapshot = QueueStatusSnapshot(0, 0, 0, 0, 0, 0)
        )

        val best = state.bestLocation!!
        assertNull(best.horizontalAccuracyMeters)
        assertNull(best.altitudeMeters)
        assertNull(best.speedMetersPerSecond)
        assertNull(best.bearingDegrees)
        assertNotNull(best.provider)
        assertNotNull(best.latitude)
        assertNotNull(best.longitude)
    }

    @Test
    fun `permission precheck error shows open settings`() {
        val state = mapToLocationUiState(
            acqState = LocationAcquisitionState(
                sessionId = "s1",
                triggerType = TriggerType.MANUAL,
                phase = AcquisitionPhase.Idle,
                errorReason = "缺少精确定位权限"
            ),
            queueSnapshot = QueueStatusSnapshot(0, 0, 0, 0, 0, 0)
        )

        assertTrue(state.showOpenSettings)
        assertEquals("缺少精确定位权限", state.errorMessage)
    }

    @Test
    fun `location off precheck error shows open settings`() {
        val state = mapToLocationUiState(
            acqState = LocationAcquisitionState(
                sessionId = "s1",
                triggerType = TriggerType.MANUAL,
                phase = AcquisitionPhase.Idle,
                errorReason = "系统定位服务未开启"
            ),
            queueSnapshot = QueueStatusSnapshot(0, 0, 0, 0, 0, 0)
        )

        assertTrue(state.showOpenSettings)
        assertEquals("系统定位服务未开启", state.errorMessage)
    }

    @Test
    fun `play services precheck error shows open settings`() {
        val state = mapToLocationUiState(
            acqState = LocationAcquisitionState(
                sessionId = "s1",
                triggerType = TriggerType.MANUAL,
                phase = AcquisitionPhase.Idle,
                errorReason = "Google Play Services 不可用"
            ),
            queueSnapshot = QueueStatusSnapshot(0, 0, 0, 0, 0, 0)
        )

        assertTrue(state.showOpenSettings)
        assertEquals("Google Play Services 不可用", state.errorMessage)
    }

    @Test
    fun `deadline text is always 30 seconds`() {
        val state = mapToLocationUiState(
            acqState = LocationAcquisitionState(),
            queueSnapshot = QueueStatusSnapshot(0, 0, 0, 0, 0, 0)
        )
        assertEquals("最长 30 秒", state.deadlineText)
    }

    @Test
    fun `elapsed text shows seconds`() {
        val state = mapToLocationUiState(
            acqState = LocationAcquisitionState(
                sessionId = "s1",
                triggerType = TriggerType.MANUAL,
                phase = AcquisitionPhase.Acquiring,
                startedAtElapsedRealtimeMs = 1000L,
                deadlineAtElapsedRealtimeMs = 31000L,
                elapsedMs = 3500L
            ),
            queueSnapshot = QueueStatusSnapshot(0, 0, 0, 0, 0, 0)
        )

        assertTrue(state.elapsedText!!.contains("3.5"))
    }

    @Test
    fun `no error when no error reason in idle`() {
        val state = mapToLocationUiState(
            acqState = LocationAcquisitionState(),
            queueSnapshot = QueueStatusSnapshot(0, 0, 0, 0, 0, 0)
        )

        assertNull(state.errorMessage)
        assertFalse(state.showOpenSettings)
    }

    @Test
    fun `non precheck error does not show open settings`() {
        val state = mapToLocationUiState(
            acqState = LocationAcquisitionState(
                sessionId = "s1",
                triggerType = TriggerType.MANUAL,
                phase = AcquisitionPhase.Failed,
                errorReason = "enqueue failed"
            ),
            queueSnapshot = QueueStatusSnapshot(0, 0, 0, 0, 0, 0)
        )

        assertFalse(state.showOpenSettings)
        assertEquals("enqueue failed", state.errorMessage)
    }

    @Test
    fun `any idle error shows open settings without hardcoded keywords`() {
        val state = mapToLocationUiState(
            acqState = LocationAcquisitionState(
                phase = AcquisitionPhase.Idle,
                errorReason = "自定义前置条件检查失败"
            ),
            queueSnapshot = QueueStatusSnapshot(0, 0, 0, 0, 0, 0)
        )

        assertTrue(state.showOpenSettings)
        assertEquals("自定义前置条件检查失败", state.errorMessage)
        assertTrue(state.showStart)
        assertTrue(state.manualStartEnabled)
    }

    // ─── Action method tests ───────────────────────────────────

    @Test
    fun `startOrRestart invokes controller startManualSession exactly once`() {
        val context = ApplicationProvider.getApplicationContext<Application>()
        val controller = ForegroundLocationController(context)
        drainStartedServices(context)

        val runner = FakeLocationAcquisitionRunner()
        val prereq = FakePrerequisiteChecker()
        val ops = TestLocationAcquisitionOperations()
        val coordinator = LocationAcquisitionCoordinator(
            runner, prereq, ops, Json,
            TrackingSettingsStore(InMemorySharedPreferences())
        )

        val queueRepo = QueueStatusRepository(
            MutableStateFlow(0), MutableStateFlow(0), MutableStateFlow(0),
            MutableStateFlow(0), MutableStateFlow(0), MutableStateFlow(0)
        )

        val viewModel = LocationViewModel(coordinator, queueRepo, controller)

        viewModel.startOrRestart()

        val intent = shadowOf(context).nextStartedService
        assertNotNull("startOrRestart must send a service intent", intent)
        assertEquals(ForegroundLocationController.ACTION_START_MANUAL_SESSION, intent?.action)
        assertNull("startOrRestart must send exactly one intent", shadowOf(context).nextStartedService)
    }

    @Test
    fun `cancel passes coordinator current session ID to controller`() {
        val context = ApplicationProvider.getApplicationContext<Application>()
        val controller = ForegroundLocationController(context)
        drainStartedServices(context)

        val runner = FakeLocationAcquisitionRunner()
        val prereq = FakePrerequisiteChecker().apply { ready() }
        val ops = TestLocationAcquisitionOperations()
        val coordinator = LocationAcquisitionCoordinator(
            runner, prereq, ops, Json,
            TrackingSettingsStore(InMemorySharedPreferences())
        )
        coordinator.uuidGenerator = { "test-session-cancel" }

        val queueRepo = QueueStatusRepository(
            MutableStateFlow(0), MutableStateFlow(0), MutableStateFlow(0),
            MutableStateFlow(0), MutableStateFlow(0), MutableStateFlow(0)
        )

        val viewModel = LocationViewModel(coordinator, queueRepo, controller)

        val result = coordinator.startManualSession()
        assertTrue(result is SessionStartResult.Started)
        val sessionId = (result as SessionStartResult.Started).sessionId

        viewModel.cancel()

        val intent = shadowOf(context).nextStartedService
        assertNotNull("cancel must send a service intent", intent)
        assertEquals(ForegroundLocationController.ACTION_CANCEL_LOCATION_SESSION, intent?.action)
        assertEquals(sessionId, intent?.getStringExtra(ForegroundLocationController.EXTRA_SESSION_ID))
        assertNull("cancel must send exactly one intent", shadowOf(context).nextStartedService)
    }

    @Test
    fun `submit invokes coordinator submitManualResult`() = runTest {
        val context = ApplicationProvider.getApplicationContext<Application>()
        val controller = ForegroundLocationController(context)
        drainStartedServices(context)

        val runner = FakeLocationAcquisitionRunner()
        val prereq = FakePrerequisiteChecker().apply { ready() }
        val ops = TestLocationAcquisitionOperations()
        val coordinator = LocationAcquisitionCoordinator(
            runner, prereq, ops, Json,
            TrackingSettingsStore(InMemorySharedPreferences())
        )
        coordinator.testScope = this
        coordinator.uuidGenerator = { "test-session-submit" }

        val queueRepo = QueueStatusRepository(
            MutableStateFlow(0), MutableStateFlow(0), MutableStateFlow(0),
            MutableStateFlow(0), MutableStateFlow(0), MutableStateFlow(0)
        )

        val viewModel = LocationViewModel(coordinator, queueRepo, controller)

        val startResult = coordinator.startManualSession()
        assertTrue(startResult is SessionStartResult.Started)

        runner.waitForAcquire()

        val snapshot = LocationSnapshot(
            latitude = 31.23, longitude = 121.47,
            horizontalAccuracyMeters = 5f,
            provider = "gps", source = "test",
            altitudeMeters = 10.0,
            speedMetersPerSecond = null, bearingDegrees = null,
            timeMillis = 100L
        )
        runner.emitCandidate(snapshot)
        runner.complete(LocationEngineResult(
            sessionId = runner.acquiredRequest!!.sessionId,
            bestLocation = snapshot,
            completion = LocationEngineCompletion.TimedOut
        ))
        advanceUntilIdle()

        assertEquals(
            "manual result must enqueue directly through the shared engine",
            1,
            ops.enqueueCount
        )
        assertEquals("manual", ops.lastSource)
        assertEquals(
            "coordinator must reach Completed after direct enqueue",
            AcquisitionPhase.Completed,
            coordinator.state.value.phase
        )
    }

    private fun drainStartedServices(application: Application) {
        while (shadowOf(application).nextStartedService != null) {
            // Drain intents left by earlier actions in the shared Robolectric application.
        }
    }
}
