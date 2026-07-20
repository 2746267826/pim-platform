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
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
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
            json = Json
        )
        coordinator.testScope = testScope
        return coordinator
    }

    private fun setCoordinatorPhase(
        coordinator: LocationAcquisitionCoordinator,
        sessionId: String,
        phase: AcquisitionPhase
    ) {
        val field = LocationAcquisitionCoordinator::class.java.getDeclaredField("_state")
        field.isAccessible = true
        @Suppress("UNCHECKED_CAST")
        val state = field.get(coordinator) as MutableStateFlow<LocationAcquisitionState>
        state.value = LocationAcquisitionState(
            sessionId = sessionId,
            triggerType = TriggerType.MANUAL,
            phase = phase,
            elapsedMs = 1000
        )
    }

    private fun getCoordinatorPhase(coordinator: LocationAcquisitionCoordinator): AcquisitionPhase {
        val field = LocationAcquisitionCoordinator::class.java.getDeclaredField("_state")
        field.isAccessible = true
        @Suppress("UNCHECKED_CAST")
        return (field.get(coordinator) as MutableStateFlow<LocationAcquisitionState>).value.phase
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

    private fun bypassHiltInjection(receiver: NotificationActionReceiver) {
        try {
            val hiltBase = NotificationActionReceiver::class.java.superclass
            val injectedField = hiltBase.getDeclaredField("injected")
            injectedField.isAccessible = true
            injectedField.setBoolean(receiver, true)
        } catch (_: Exception) {
        }
    }

    private fun injectFields(
        receiver: NotificationActionReceiver,
        coordinator: LocationAcquisitionCoordinator,
        publisher: LocationLiveUpdatePublisher
    ) {
        val coordField = NotificationActionReceiver::class.java.getDeclaredField("coordinator")
        coordField.isAccessible = true
        coordField.set(receiver, coordinator)

        val pubField = NotificationActionReceiver::class.java.getDeclaredField("liveUpdatePublisher")
        pubField.isAccessible = true
        pubField.set(receiver, publisher)
    }

    private fun preparedReceiver(
        coordinator: LocationAcquisitionCoordinator,
        publisher: LocationLiveUpdatePublisher
    ): NotificationActionReceiver {
        val receiver = NotificationActionReceiver()
        bypassHiltInjection(receiver)
        injectFields(receiver, coordinator, publisher)
        return receiver
    }

    @Test
    fun `cancel action with delete uri does not cancel session`() {
        val coordinator = createCoordinator()
        setCoordinatorPhase(coordinator, "session-x", AcquisitionPhase.Acquiring)
        val publisher = createPublisher()
        val receiver = preparedReceiver(coordinator, publisher)

        val intent = Intent(LocationLiveUpdateNotificationRenderer.ACTION_CANCEL_LOCATION_SESSION)
            .setData(Uri.parse("pim://location-live/session-x/delete"))

        receiver.onReceive(ctx, intent)
        assertEquals(AcquisitionPhase.Acquiring, getCoordinatorPhase(coordinator))
    }

    @Test
    fun `cancel action with null uri does not cancel session`() {
        val coordinator = createCoordinator()
        setCoordinatorPhase(coordinator, "session-x", AcquisitionPhase.Acquiring)
        val publisher = createPublisher()
        val receiver = preparedReceiver(coordinator, publisher)

        val intent = Intent(LocationLiveUpdateNotificationRenderer.ACTION_CANCEL_LOCATION_SESSION)

        receiver.onReceive(ctx, intent)
        assertEquals(AcquisitionPhase.Acquiring, getCoordinatorPhase(coordinator))
    }

    @Test
    fun `cancel action with matching cancel uri cancels session`() {
        val coordinator = createCoordinator()
        setCoordinatorPhase(coordinator, "session-x", AcquisitionPhase.Acquiring)
        val publisher = createPublisher()
        val receiver = preparedReceiver(coordinator, publisher)

        val intent = Intent(LocationLiveUpdateNotificationRenderer.ACTION_CANCEL_LOCATION_SESSION)
            .setData(Uri.parse("pim://location-live/session-x/cancel"))

        receiver.onReceive(ctx, intent)
        assertEquals(AcquisitionPhase.Cancelled, getCoordinatorPhase(coordinator))
    }

    @Test
    fun `dismiss action with cancel uri does not suppress session`() {
        val coordinator = createCoordinator()
        val publisher = createPublisher()
        publisher.start(testScope)
        val receiver = preparedReceiver(coordinator, publisher)

        val intent = Intent(LocationLiveUpdateNotificationRenderer.ACTION_DISMISS_LOCATION_LIVE_UPDATE)
            .setData(Uri.parse("pim://location-live/session-x/cancel"))

        receiver.onReceive(ctx, intent)

        publisherStateFlow.value = LocationAcquisitionState(
            sessionId = "session-x",
            triggerType = TriggerType.MANUAL,
            phase = AcquisitionPhase.Acquiring,
            elapsedMs = 1000
        )
        testScope.advanceUntilIdle()
        assertTrue("should publish because session not suppressed", publishCalls.isNotEmpty())
        assertEquals("session-x", publishCalls.first().sessionId)
    }

    @Test
    fun `dismiss action with delete uri suppresses session`() {
        val coordinator = createCoordinator()
        val publisher = createPublisher()
        publisher.start(testScope)
        val receiver = preparedReceiver(coordinator, publisher)

        val intent = Intent(LocationLiveUpdateNotificationRenderer.ACTION_DISMISS_LOCATION_LIVE_UPDATE)
            .setData(Uri.parse("pim://location-live/session-x/delete"))

        receiver.onReceive(ctx, intent)

        publisherStateFlow.value = LocationAcquisitionState(
            sessionId = "session-x",
            triggerType = TriggerType.MANUAL,
            phase = AcquisitionPhase.Acquiring,
            elapsedMs = 1000
        )
        testScope.advanceUntilIdle()
        assertTrue("should suppress session, no publish calls", publishCalls.isEmpty())
    }
}
