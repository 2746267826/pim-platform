package com.pim.app.notifications

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.test.core.app.ApplicationProvider
import com.pim.app.TestPimApp
import com.pim.app.location.LocationSnapshot
import com.pim.app.location.acquisition.AcquisitionPhase
import com.pim.app.location.acquisition.LocationAcquisitionCoordinator
import com.pim.app.location.acquisition.LocationAcquisitionOperations
import com.pim.app.location.acquisition.LocationAcquisitionRunner
import com.pim.app.location.acquisition.LocationAcquisitionState
import com.pim.app.location.acquisition.LocationEngineRequest
import com.pim.app.location.acquisition.LocationEngineResult
import com.pim.app.location.acquisition.LocationPrerequisiteChecker
import com.pim.app.location.acquisition.LocationPrerequisiteResult
import com.pim.app.location.acquisition.TriggerType
import com.pim.app.location.liveupdate.LocationLiveUpdateContent
import com.pim.app.location.liveupdate.LocationLiveUpdateNotificationRenderer
import com.pim.app.location.liveupdate.LocationLiveUpdatePublisher
import com.pim.app.location.quality.QualityAcceptedLocation
import com.pim.app.location.quality.RawLocationFix
import com.pim.app.settings.TrackingSettingsStore
import com.pim.app.testing.InMemorySharedPreferences
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.serialization.json.Json
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = TestPimApp::class)
class NotificationActionReceiverTest {

    private val ctx: Context = ApplicationProvider.getApplicationContext()
    private lateinit var testScope: TestScope
    private val publishCalls = mutableListOf<LocationLiveUpdateContent>()
    private lateinit var publisherStateFlow: MutableStateFlow<LocationAcquisitionState>

    private val noOpRunner = object : LocationAcquisitionRunner {
        override suspend fun acquire(
            request: LocationEngineRequest,
            onCandidate: suspend (LocationSnapshot) -> Unit,
            onAvailabilityChanged: suspend (Boolean) -> Unit
        ): LocationEngineResult {
            return suspendCancellableCoroutine { }
        }

        override suspend fun stream(
            request: com.pim.app.location.acquisition.LocationUpdateRequest,
            onCandidate: suspend (LocationSnapshot) -> Unit
        ) {
            return suspendCancellableCoroutine { }
        }
    }

    private val readyChecker = object : LocationPrerequisiteChecker {
        override fun check(triggerType: TriggerType): LocationPrerequisiteResult {
            return LocationPrerequisiteResult.Ready
        }
    }

    private val noOpOperations = object : LocationAcquisitionOperations {
        override suspend fun enqueueAccepted(
            accepted: QualityAcceptedLocation,
            rawJson: String,
            source: String
        ) {}
        override suspend fun recordDropped(fix: RawLocationFix, reason: String) {}
        override fun scheduleSync() {}
    }

    @Before
    fun setUp() {
        testScope = TestScope(StandardTestDispatcher())
        publishCalls.clear()
    }

    @After
    fun tearDown() {
        testScope.cancel(CancellationException("test done"))
    }

    private fun createCoordinator(): LocationAcquisitionCoordinator {
        val coordinator = LocationAcquisitionCoordinator(
            runner = noOpRunner,
            prerequisiteChecker = readyChecker,
            operations = noOpOperations,
            json = Json,
            trackingSettingsStore = TrackingSettingsStore(InMemorySharedPreferences())
        )
        coordinator.testScope = testScope
        return coordinator
    }

    private fun setCoordinatorAcquiring(
        coordinator: LocationAcquisitionCoordinator,
        sessionId: String
    ) {
        coordinator.uuidGenerator = { sessionId }
        coordinator.startManualSession()
        testScope.runCurrent()
    }

    private fun createPublisher(): LocationLiveUpdatePublisher {
        publisherStateFlow = MutableStateFlow(LocationAcquisitionState())
        publishCalls.clear()
        return LocationLiveUpdatePublisher(
            stateFlow = publisherStateFlow,
            context = ctx,
            clockMs = { 0L },
            publishFn = { content ->
                publishCalls.add(content)
                true
            },
            cancelFn = {}
        )
    }

    private fun ensureHiltInjected(receiver: NotificationActionReceiver) {
        var clazz: Class<*>? = receiver.javaClass
        while (clazz != null) {
            try {
                val field = clazz.getDeclaredField("injected")
                field.isAccessible = true
                field.setBoolean(receiver, true)
                return
            } catch (_: NoSuchFieldException) {
                clazz = clazz.superclass
            }
        }
        throw AssertionError(
            "Could not find Hilt 'injected' field in NotificationActionReceiver hierarchy"
        )
    }

    @Test
    fun `cancel action with matching cancel uri cancels session`() {
        val coordinator = createCoordinator()
        setCoordinatorAcquiring(coordinator, "session-x")

        val receiver = NotificationActionReceiver()
        ensureHiltInjected(receiver)
        receiver.coordinator = coordinator

        val intent = Intent().apply {
            action = LocationLiveUpdateNotificationRenderer.ACTION_CANCEL_LOCATION_SESSION
            data = Uri.parse("pim://location-live/session-x/cancel")
        }
        receiver.onReceive(ctx, intent)

        assertEquals(AcquisitionPhase.Cancelled, coordinator.state.value.phase)
    }

    @Test
    fun `cancel action with delete uri does not cancel session`() {
        val coordinator = createCoordinator()
        setCoordinatorAcquiring(coordinator, "session-x")

        val receiver = NotificationActionReceiver()
        ensureHiltInjected(receiver)
        receiver.coordinator = coordinator

        val intent = Intent().apply {
            action = LocationLiveUpdateNotificationRenderer.ACTION_CANCEL_LOCATION_SESSION
            data = Uri.parse("pim://location-live/session-x/delete")
        }
        receiver.onReceive(ctx, intent)

        assertEquals(AcquisitionPhase.Acquiring, coordinator.state.value.phase)
    }

    @Test
    fun `cancel action with null uri does not cancel session`() {
        val coordinator = createCoordinator()
        setCoordinatorAcquiring(coordinator, "session-x")

        val receiver = NotificationActionReceiver()
        ensureHiltInjected(receiver)
        receiver.coordinator = coordinator

        val intent = Intent().apply {
            action = LocationLiveUpdateNotificationRenderer.ACTION_CANCEL_LOCATION_SESSION
        }
        receiver.onReceive(ctx, intent)

        assertEquals(AcquisitionPhase.Acquiring, coordinator.state.value.phase)
    }

    @Test
    fun `dismiss action with delete uri suppresses session`() {
        val coordinator = createCoordinator()
        setCoordinatorAcquiring(coordinator, "session-x")
        val publisher = createPublisher()

        val receiver = NotificationActionReceiver()
        ensureHiltInjected(receiver)
        receiver.coordinator = coordinator
        receiver.liveUpdatePublisher = publisher

        val intent = Intent().apply {
            action = LocationLiveUpdateNotificationRenderer.ACTION_DISMISS_LOCATION_LIVE_UPDATE
            data = Uri.parse("pim://location-live/session-x/delete")
        }
        receiver.onReceive(ctx, intent)

        assertEquals(AcquisitionPhase.Acquiring, coordinator.state.value.phase)

        publisher.start(testScope)
        publisherStateFlow.value = LocationAcquisitionState(
            sessionId = "session-x",
            triggerType = TriggerType.MANUAL,
            phase = AcquisitionPhase.Acquiring,
            elapsedMs = 1000
        )
        testScope.runCurrent()
        assertTrue("should suppress session, no publish calls", publishCalls.isEmpty())
    }

    @Test
    fun `dismiss action with cancel uri does not suppress session`() {
        val coordinator = createCoordinator()
        setCoordinatorAcquiring(coordinator, "session-x")
        val publisher = createPublisher()

        val receiver = NotificationActionReceiver()
        ensureHiltInjected(receiver)
        receiver.coordinator = coordinator
        receiver.liveUpdatePublisher = publisher

        val intent = Intent().apply {
            action = LocationLiveUpdateNotificationRenderer.ACTION_DISMISS_LOCATION_LIVE_UPDATE
            data = Uri.parse("pim://location-live/session-x/cancel")
        }
        receiver.onReceive(ctx, intent)

        publisher.start(testScope)
        publisherStateFlow.value = LocationAcquisitionState(
            sessionId = "session-x",
            triggerType = TriggerType.MANUAL,
            phase = AcquisitionPhase.Acquiring,
            elapsedMs = 1000
        )
        testScope.runCurrent()
        assertTrue("should publish because session not suppressed", publishCalls.isNotEmpty())
        assertEquals("session-x", publishCalls.first().sessionId)
    }
}
