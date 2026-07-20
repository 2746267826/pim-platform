package com.pim.app.location.liveupdate

import com.pim.app.location.LocationSnapshot
import com.pim.app.location.acquisition.AcquisitionPhase
import com.pim.app.location.acquisition.LocationAcquisitionState
import com.pim.app.location.acquisition.TriggerType
import kotlin.OptIn
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
@OptIn(ExperimentalCoroutinesApi::class)
class LocationLiveUpdatePublisherTest {

    private lateinit var publisher: LocationLiveUpdatePublisher
    private lateinit var stateFlow: MutableStateFlow<LocationAcquisitionState>
    private lateinit var testScope: TestScope
    private var fakeClockMs: Long = 0L

    private data class PublishCall(
        val sessionId: String,
        val triggerType: TriggerType,
        val elapsedSeconds: Long,
        val accuracyMeters: Float?,
        val providerLabel: String
    )

    private val publishCalls = mutableListOf<PublishCall>()
    private var cancelCalls = 0

    @Before
    fun setUp() {
        fakeClockMs = 0L
        publishCalls.clear()
        cancelCalls = 0
        stateFlow = MutableStateFlow(LocationAcquisitionState())
        testScope = TestScope(StandardTestDispatcher())
        publisher = LocationLiveUpdatePublisher(
            stateFlow = stateFlow,
            clockMs = { fakeClockMs },
            publishFn = { content ->
                publishCalls.add(PublishCall(
                    content.sessionId,
                    content.triggerType,
                    content.elapsedSeconds,
                    content.accuracyMeters,
                    content.providerLabel
                ))
                true
            },
            cancelFn = { cancelCalls++ }
        )
    }

    @After
    fun tearDown() {
        testScope.cancel(CancellationException("test done"))
    }

    @Test
    fun `start collects from state flow and notifies on acquiring`() {
        publisher.start(testScope)

        stateFlow.value = LocationAcquisitionState(
            sessionId = "s1",
            triggerType = TriggerType.MANUAL,
            phase = AcquisitionPhase.Acquiring,
            elapsedMs = 1000,
            bestLocation = null
        )

        testScope.advanceUntilIdle()
        assertTrue("should publish on acquiring", publishCalls.isNotEmpty())
        assertEquals("s1", publishCalls.first().sessionId)
    }

    @Test
    fun `does not publish when not in acquiring phase`() {
        publisher.start(testScope)

        stateFlow.value = LocationAcquisitionState(
            sessionId = "s1",
            triggerType = TriggerType.MANUAL,
            phase = AcquisitionPhase.Preparing,
            elapsedMs = 1000
        )

        testScope.advanceUntilIdle()
        assertTrue("should not publish when preparing", publishCalls.isEmpty())
    }

    @Test
    fun `cancels notification when leaving acquiring phase`() {
        publisher.start(testScope)

        stateFlow.value = LocationAcquisitionState(
            sessionId = "s1",
            triggerType = TriggerType.MANUAL,
            phase = AcquisitionPhase.Acquiring,
            elapsedMs = 1000
        )

        testScope.advanceUntilIdle()
        val beforeCancel = cancelCalls
        assertTrue("should have published", publishCalls.isNotEmpty())

        stateFlow.value = LocationAcquisitionState(
            sessionId = null,
            phase = AcquisitionPhase.Idle,
            elapsedMs = 0
        )

        testScope.advanceUntilIdle()
        assertTrue("should have cancelled after leaving acquiring", cancelCalls > beforeCancel)
    }

    @Test
    fun `throttles updates to max 2000ms interval`() {
        publisher.start(testScope)

        stateFlow.value = LocationAcquisitionState(
            sessionId = "s1",
            triggerType = TriggerType.MANUAL,
            phase = AcquisitionPhase.Acquiring,
            elapsedMs = 1000,
            bestLocation = null
        )

        testScope.advanceUntilIdle()
        val firstCall = publishCalls.size
        assertTrue("first publish should happen", firstCall >= 1)
        fakeClockMs = 500

        stateFlow.value = LocationAcquisitionState(
            sessionId = "s1",
            triggerType = TriggerType.MANUAL,
            phase = AcquisitionPhase.Acquiring,
            elapsedMs = 1500,
            bestLocation = null
        )

        testScope.advanceUntilIdle()
        assertEquals("should not publish before 2000ms throttle", firstCall, publishCalls.size)

        fakeClockMs = 2500
        stateFlow.value = LocationAcquisitionState(
            sessionId = "s1",
            triggerType = TriggerType.MANUAL,
            phase = AcquisitionPhase.Acquiring,
            elapsedMs = 3500,
            bestLocation = null
        )

        testScope.advanceUntilIdle()
        assertTrue("should publish after throttle window", publishCalls.size > firstCall)
    }

    @Test
    fun `suppressSession prevents notifications for given session`() {
        publisher.start(testScope)
        publisher.suppressSession("s-suppressed")

        stateFlow.value = LocationAcquisitionState(
            sessionId = "s-suppressed",
            triggerType = TriggerType.MANUAL,
            phase = AcquisitionPhase.Acquiring,
            elapsedMs = 1000
        )

        testScope.advanceUntilIdle()
        assertTrue("suppressed session should not publish", publishCalls.isEmpty())
    }

    @Test
    fun `suppressSession does not affect new session`() {
        publisher.start(testScope)
        publisher.suppressSession("s-old")

        stateFlow.value = LocationAcquisitionState(
            sessionId = "s-new",
            triggerType = TriggerType.MANUAL,
            phase = AcquisitionPhase.Acquiring,
            elapsedMs = 1000
        )

        testScope.advanceUntilIdle()
        assertTrue("new session should publish despite old suppression", publishCalls.isNotEmpty())
        assertEquals("s-new", publishCalls.first().sessionId)
    }

    @Test
    fun `cancelStaleNotification cancels without error`() {
        publisher.cancelStaleNotification()
        assertEquals(1, cancelCalls)
    }

    @Test
    fun `cancelStaleNotification can be called multiple times`() {
        publisher.cancelStaleNotification()
        publisher.cancelStaleNotification()
        assertEquals(2, cancelCalls)
    }

    @Test
    fun `new session resets throttle and accuracy baseline`() {
        publisher.start(testScope)
        stateFlow.value = LocationAcquisitionState(
            sessionId = "s1",
            triggerType = TriggerType.MANUAL,
            phase = AcquisitionPhase.Acquiring,
            elapsedMs = 1000
        )
        testScope.advanceUntilIdle()
        val countAfterFirst = publishCalls.size
        assertTrue("first session should publish", countAfterFirst >= 1)

        publisher.start(testScope)

        stateFlow.value = LocationAcquisitionState(
            sessionId = "s2",
            triggerType = TriggerType.MANUAL,
            phase = AcquisitionPhase.Acquiring,
            elapsedMs = 100,
            bestLocation = null
        )
        testScope.advanceUntilIdle()
        assertTrue(
            "new session should publish immediately regardless of old throttle",
            publishCalls.size > countAfterFirst
        )
        assertEquals("s2", publishCalls.last().sessionId)
    }

    @Test
    fun `leaves acquiring immediately cancels even if never published`() {
        publisher.start(testScope)
        stateFlow.value = LocationAcquisitionState(
            sessionId = "s1",
            phase = AcquisitionPhase.Preparing,
            elapsedMs = 500
        )
        testScope.advanceUntilIdle()
        assertEquals("no publish before acquiring", 0, publishCalls.size)
        val cancelBefore = cancelCalls

        stateFlow.value = LocationAcquisitionState(
            sessionId = "s1",
            phase = AcquisitionPhase.Acquiring,
            elapsedMs = 1000
        )
        testScope.advanceUntilIdle()
        assertTrue("should publish on acquiring", publishCalls.isNotEmpty())

        stateFlow.value = LocationAcquisitionState(
            sessionId = null,
            phase = AcquisitionPhase.Idle,
            elapsedMs = 0
        )
        testScope.advanceUntilIdle()
        assertTrue(
            "should cancel when leaving acquiring",
            cancelCalls > cancelBefore
        )
    }

    @Test
    fun `accuracy improvement of 5 or more meters triggers early publish within throttle window`() {
        publisher.start(testScope)
        fakeClockMs = 10000L

        stateFlow.value = LocationAcquisitionState(
            sessionId = "s1",
            triggerType = TriggerType.MANUAL,
            phase = AcquisitionPhase.Acquiring,
            elapsedMs = 1000,
            bestLocation = locationSnapshot(accuracy = 50f)
        )
        testScope.advanceUntilIdle()
        assertEquals("first publish should happen", 1, publishCalls.size)

        fakeClockMs = 11500L

        stateFlow.value = LocationAcquisitionState(
            sessionId = "s1",
            triggerType = TriggerType.MANUAL,
            phase = AcquisitionPhase.Acquiring,
            elapsedMs = 3000,
            bestLocation = locationSnapshot(accuracy = 45f)
        )
        testScope.advanceUntilIdle()
        assertEquals(
            "improvement 5m within throttle window should trigger early publish",
            2, publishCalls.size
        )
    }

    @Test
    fun `accuracy improvement less than 5 meters does not trigger early publish within throttle`() {
        publisher.start(testScope)
        fakeClockMs = 10000L

        stateFlow.value = LocationAcquisitionState(
            sessionId = "s1",
            triggerType = TriggerType.MANUAL,
            phase = AcquisitionPhase.Acquiring,
            elapsedMs = 1000,
            bestLocation = locationSnapshot(accuracy = 50f)
        )
        testScope.advanceUntilIdle()
        assertEquals("first publish should happen", 1, publishCalls.size)

        fakeClockMs = 11900L

        stateFlow.value = LocationAcquisitionState(
            sessionId = "s1",
            triggerType = TriggerType.MANUAL,
            phase = AcquisitionPhase.Acquiring,
            elapsedMs = 3000,
            bestLocation = locationSnapshot(accuracy = 46f)
        )
        testScope.advanceUntilIdle()
        assertEquals(
            "improvement 4m within throttle window should not trigger early publish",
            1, publishCalls.size
        )
    }

    @Test
    fun `accuracy improvement less than 5 meters publishes after throttle window expires`() {
        publisher.start(testScope)
        fakeClockMs = 10000L

        stateFlow.value = LocationAcquisitionState(
            sessionId = "s1",
            triggerType = TriggerType.MANUAL,
            phase = AcquisitionPhase.Acquiring,
            elapsedMs = 1000,
            bestLocation = locationSnapshot(accuracy = 50f)
        )
        testScope.advanceUntilIdle()
        assertEquals("first publish should happen", 1, publishCalls.size)

        fakeClockMs = 13000L

        stateFlow.value = LocationAcquisitionState(
            sessionId = "s1",
            triggerType = TriggerType.MANUAL,
            phase = AcquisitionPhase.Acquiring,
            elapsedMs = 4000,
            bestLocation = locationSnapshot(accuracy = 47f)
        )
        testScope.advanceUntilIdle()
        assertEquals(
            "improvement 3m should publish after throttle window expires",
            2, publishCalls.size
        )
    }

    @Test
    fun `session id change resets throttle without requiring restart`() {
        publisher.start(testScope)
        fakeClockMs = 10000L

        stateFlow.value = LocationAcquisitionState(
            sessionId = "s1",
            triggerType = TriggerType.MANUAL,
            phase = AcquisitionPhase.Acquiring,
            elapsedMs = 1000,
            bestLocation = locationSnapshot(accuracy = 50f)
        )
        testScope.advanceUntilIdle()
        assertEquals("first session should publish", 1, publishCalls.size)

        fakeClockMs = 10500L

        stateFlow.value = LocationAcquisitionState(
            sessionId = "s2",
            triggerType = TriggerType.MANUAL,
            phase = AcquisitionPhase.Acquiring,
            elapsedMs = 2000,
            bestLocation = locationSnapshot(accuracy = 50f)
        )
        testScope.advanceUntilIdle()
        assertEquals(
            "new session should publish immediately regardless of throttle",
            2, publishCalls.size
        )
        assertEquals("s2", publishCalls.last().sessionId)
    }

    @Test
    fun `exception in handleState does not kill collector`() {
        publisher = LocationLiveUpdatePublisher(
            stateFlow = stateFlow,
            clockMs = { fakeClockMs },
            publishFn = { content ->
                if (publishCalls.isEmpty()) {
                    publishCalls.add(PublishCall(content.sessionId, content.triggerType, content.elapsedSeconds, content.accuracyMeters, content.providerLabel))
                    throw RuntimeException("simulated failure")
                }
                publishCalls.add(PublishCall("ok", content.triggerType, content.elapsedSeconds, content.accuracyMeters, content.providerLabel))
                true
            },
            cancelFn = { cancelCalls++ }
        )
        publisher.start(testScope)

        stateFlow.value = LocationAcquisitionState(
            sessionId = "s1",
            triggerType = TriggerType.MANUAL,
            phase = AcquisitionPhase.Acquiring,
            elapsedMs = 1000
        )
        testScope.advanceUntilIdle()
        assertEquals("first call should be attempted", 1, publishCalls.size)

        stateFlow.value = LocationAcquisitionState(
            sessionId = "s1",
            triggerType = TriggerType.MANUAL,
            phase = AcquisitionPhase.Acquiring,
            elapsedMs = 5000
        )
        fakeClockMs = 5000L
        testScope.advanceUntilIdle()
        assertEquals("collector should survive exception", 2, publishCalls.size)
        assertEquals("ok", publishCalls.last().sessionId)
    }

    @Test
    fun `publish returns false does not update baseline so next state can retry`() {
        var publishReturn = true
        publisher = LocationLiveUpdatePublisher(
            stateFlow = stateFlow,
            clockMs = { fakeClockMs },
            publishFn = {
                publishCalls.add(PublishCall(it.sessionId, it.triggerType, it.elapsedSeconds, it.accuracyMeters, it.providerLabel))
                publishReturn
            },
            cancelFn = { cancelCalls++ }
        )
        publisher.start(testScope)
        fakeClockMs = 10000L

        stateFlow.value = LocationAcquisitionState(
            sessionId = "s1", triggerType = TriggerType.MANUAL,
            phase = AcquisitionPhase.Acquiring, elapsedMs = 1000
        )
        testScope.advanceUntilIdle()
        assertEquals("first publish should succeed", 1, publishCalls.size)

        publishReturn = false
        fakeClockMs = 12000L
        stateFlow.value = LocationAcquisitionState(
            sessionId = "s1", triggerType = TriggerType.MANUAL,
            phase = AcquisitionPhase.Acquiring, elapsedMs = 3000
        )
        testScope.advanceUntilIdle()
        assertEquals("false return should be attempted (past throttle)", 2, publishCalls.size)

        publishReturn = true
        fakeClockMs = 12500L
        stateFlow.value = LocationAcquisitionState(
            sessionId = "s1", triggerType = TriggerType.MANUAL,
            phase = AcquisitionPhase.Acquiring, elapsedMs = 3500
        )
        testScope.advanceUntilIdle()
        assertEquals(
            "baseline not advanced after false return, retry inside would-be throttle window",
            3, publishCalls.size
        )
    }

    @Test
    fun `exception in publish does not update baseline so next state can retry`() {
        var throwOnPublish = true
        publisher = LocationLiveUpdatePublisher(
            stateFlow = stateFlow,
            clockMs = { fakeClockMs },
            publishFn = {
                publishCalls.add(PublishCall(it.sessionId, it.triggerType, it.elapsedSeconds, it.accuracyMeters, it.providerLabel))
                if (throwOnPublish) throw RuntimeException("simulated failure")
                true
            },
            cancelFn = { cancelCalls++ }
        )
        publisher.start(testScope)
        fakeClockMs = 10000L

        stateFlow.value = LocationAcquisitionState(
            sessionId = "s1", triggerType = TriggerType.MANUAL,
            phase = AcquisitionPhase.Acquiring, elapsedMs = 1000
        )
        testScope.advanceUntilIdle()
        assertEquals("first publish attempted", 1, publishCalls.size)

        throwOnPublish = false
        fakeClockMs = 10500L
        stateFlow.value = LocationAcquisitionState(
            sessionId = "s1", triggerType = TriggerType.MANUAL,
            phase = AcquisitionPhase.Acquiring, elapsedMs = 2000
        )
        testScope.advanceUntilIdle()
        assertEquals(
            "baseline not advanced after exception, retry succeeds immediately",
            2, publishCalls.size
        )
    }

    private fun locationSnapshot(accuracy: Float) = LocationSnapshot(
        latitude = 0.0,
        longitude = 0.0,
        horizontalAccuracyMeters = accuracy,
        provider = "test",
        source = "test",
        altitudeMeters = null,
        speedMetersPerSecond = null,
        bearingDegrees = null,
        timeMillis = 0L
    )
}
