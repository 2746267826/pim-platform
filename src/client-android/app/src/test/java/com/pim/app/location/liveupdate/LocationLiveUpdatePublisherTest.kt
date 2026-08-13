package com.pim.app.location.liveupdate

import com.pim.app.location.LocationSnapshot
import com.pim.app.location.acquisition.AcquisitionPhase
import com.pim.app.location.acquisition.LocationAcquisitionState
import com.pim.app.location.acquisition.TriggerType
import com.pim.app.location.service.ForegroundLocationRuntimeState
import java.util.concurrent.CompletableFuture
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import kotlin.OptIn
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.asCoroutineDispatcher
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
    private val publishHighSpeedElapsedCalls = mutableListOf<Long>()
    private val highSpeedRuntimeFlow = MutableStateFlow(ForegroundLocationRuntimeState())

    @Before
    fun setUp() {
        fakeClockMs = 0L
        publishCalls.clear()
        cancelCalls = 0
        publishHighSpeedElapsedCalls.clear()
        highSpeedRuntimeFlow.value = ForegroundLocationRuntimeState()
        stateFlow = MutableStateFlow(LocationAcquisitionState())
        testScope = TestScope(StandardTestDispatcher())
        publisher = LocationLiveUpdatePublisher(
            stateFlow = stateFlow,
            clockMs = { fakeClockMs },
            highSpeedFlow = highSpeedRuntimeFlow,
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
            cancelFn = { cancelCalls++ },
            publishHighSpeedFn = { content ->
                publishHighSpeedElapsedCalls.add(content.elapsedSeconds)
                true
            }
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
    fun `suppress after publish immediately cancels the posted notification exactly once`() {
        publisher.start(testScope)
        stateFlow.value = LocationAcquisitionState(
            sessionId = "s1",
            triggerType = TriggerType.MANUAL,
            phase = AcquisitionPhase.Acquiring,
            elapsedMs = 1000
        )
        testScope.advanceUntilIdle()
        assertEquals(1, publishCalls.size)

        publisher.suppressSession("s1")

        assertEquals(
            "suppress must immediately cancel the already posted notification exactly once",
            1, cancelCalls
        )
    }

    @Test
    fun `suppress after publish blocks future publishes for that session`() {
        publisher.start(testScope)
        stateFlow.value = LocationAcquisitionState(
            sessionId = "s1",
            triggerType = TriggerType.MANUAL,
            phase = AcquisitionPhase.Acquiring,
            elapsedMs = 1000
        )
        testScope.advanceUntilIdle()
        assertEquals(1, publishCalls.size)

        publisher.suppressSession("s1")
        fakeClockMs = 5000L

        stateFlow.value = LocationAcquisitionState(
            sessionId = "s1",
            triggerType = TriggerType.MANUAL,
            phase = AcquisitionPhase.Acquiring,
            elapsedMs = 6000
        )
        testScope.advanceUntilIdle()
        assertEquals("suppressed session must not republish", 1, publishCalls.size)
    }

    @Test
    fun `suppress old session does not cancel notification owned by newer session`() {
        publisher.start(testScope)
        stateFlow.value = LocationAcquisitionState(
            sessionId = "s1",
            triggerType = TriggerType.MANUAL,
            phase = AcquisitionPhase.Acquiring,
            elapsedMs = 1000
        )
        testScope.advanceUntilIdle()
        assertEquals(1, publishCalls.size)

        fakeClockMs = 5000L
        stateFlow.value = LocationAcquisitionState(
            sessionId = "s2",
            triggerType = TriggerType.MANUAL,
            phase = AcquisitionPhase.Acquiring,
            elapsedMs = 2000
        )
        testScope.advanceUntilIdle()
        assertEquals(2, publishCalls.size)
        assertEquals("s2", publishCalls.last().sessionId)

        publisher.suppressSession("s1")

        assertEquals(
            "suppressing an older session must not remove the newer session notification",
            0, cancelCalls
        )
    }

    @Test
    fun `cancelStaleNotification does not remove a published session notification`() {
        publisher.start(testScope)
        stateFlow.value = LocationAcquisitionState(
            sessionId = "s1",
            triggerType = TriggerType.MANUAL,
            phase = AcquisitionPhase.Acquiring,
            elapsedMs = 1000
        )
        testScope.advanceUntilIdle()
        fakeClockMs = 5000L
        stateFlow.value = LocationAcquisitionState(
            sessionId = "s2",
            triggerType = TriggerType.MANUAL,
            phase = AcquisitionPhase.Acquiring,
            elapsedMs = 2000
        )
        testScope.advanceUntilIdle()
        assertEquals("s2", publishCalls.last().sessionId)

        publisher.cancelStaleNotification()

        assertEquals(
            "stale cleanup must not cancel the notification owned by the current published session",
            0, cancelCalls
        )
    }

    @Test
    fun `leaving acquiring after suppress does not re-cancel the removed session`() {
        publisher.start(testScope)
        stateFlow.value = LocationAcquisitionState(
            sessionId = "s1",
            triggerType = TriggerType.MANUAL,
            phase = AcquisitionPhase.Acquiring,
            elapsedMs = 1000
        )
        testScope.advanceUntilIdle()
        assertEquals(1, publishCalls.size)

        publisher.suppressSession("s1")
        assertEquals(1, cancelCalls)

        stateFlow.value = LocationAcquisitionState(
            sessionId = null,
            phase = AcquisitionPhase.Idle,
            elapsedMs = 0
        )
        testScope.advanceUntilIdle()
        assertEquals(
            "leaving acquiring must not cancel again a session already removed by suppress",
            1, cancelCalls
        )
    }

    @Test
    fun `suppress racing in-flight publish cancels exactly once and blocks republish`() {
        val publishEntered = CountDownLatch(1)
        val releasePublish = CountDownLatch(1)
        val executor = Executors.newSingleThreadExecutor()
        val scope = CoroutineScope(executor.asCoroutineDispatcher())
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
                publishEntered.countDown()
                releasePublish.await()
                true
            },
            cancelFn = { cancelCalls++ }
        )
        publisher.start(scope)

        stateFlow.value = LocationAcquisitionState(
            sessionId = "s1",
            triggerType = TriggerType.MANUAL,
            phase = AcquisitionPhase.Acquiring,
            elapsedMs = 1000
        )
        publishEntered.await()

        val suppressThread = Thread { publisher.suppressSession("s1") }
        suppressThread.start()
        releasePublish.countDown()
        suppressThread.join()

        fakeClockMs = 5000L
        stateFlow.value = LocationAcquisitionState(
            sessionId = "s1",
            triggerType = TriggerType.MANUAL,
            phase = AcquisitionPhase.Acquiring,
            elapsedMs = 6000
        )
        val collectorDrained = CompletableFuture<Void>()
        executor.execute { collectorDrained.complete(null) }
        collectorDrained.get(5, TimeUnit.SECONDS)

        scope.cancel()
        executor.shutdown()

        assertEquals(
            "concurrent suppress while publish is in flight must still cancel exactly once",
            1, cancelCalls
        )
        assertEquals("suppressed session must not republish after the race", 1, publishCalls.size)
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
    fun `null to finite accuracy within throttle window triggers immediate second publish`() {
        publisher.start(testScope)
        fakeClockMs = 10000L

        stateFlow.value = LocationAcquisitionState(
            sessionId = "s1",
            triggerType = TriggerType.MANUAL,
            phase = AcquisitionPhase.Acquiring,
            elapsedMs = 1000,
            bestLocation = null
        )
        testScope.advanceUntilIdle()
        assertEquals("first publish with no accuracy should happen", 1, publishCalls.size)
        assertEquals(null, publishCalls.first().accuracyMeters)

        fakeClockMs = 11500L

        stateFlow.value = LocationAcquisitionState(
            sessionId = "s1",
            triggerType = TriggerType.MANUAL,
            phase = AcquisitionPhase.Acquiring,
            elapsedMs = 2500,
            bestLocation = locationSnapshot(accuracy = 10f)
        )
        testScope.advanceUntilIdle()
        assertEquals(
            "null-to-finite accuracy transition should trigger immediate second publish within throttle",
            2, publishCalls.size
        )
        assertEquals(10f, publishCalls.last().accuracyMeters)
    }

    @Test
    fun `Acquiring to Evaluating candidate update neither cancels nor suppresses null-to-finite accuracy refresh`() {
        publisher.start(testScope)
        fakeClockMs = 10000L

        stateFlow.value = LocationAcquisitionState(
            sessionId = "s1",
            triggerType = TriggerType.MANUAL,
            phase = AcquisitionPhase.Acquiring,
            elapsedMs = 1000,
            bestLocation = null
        )
        testScope.advanceUntilIdle()
        assertEquals("first publish with no accuracy should happen", 1, publishCalls.size)
        assertEquals(null, publishCalls.first().accuracyMeters)

        fakeClockMs = 11500L
        stateFlow.value = LocationAcquisitionState(
            sessionId = "s1",
            triggerType = TriggerType.MANUAL,
            phase = AcquisitionPhase.Evaluating,
            elapsedMs = 2500,
            bestLocation = locationSnapshot(accuracy = 10f)
        )
        testScope.advanceUntilIdle()
        assertEquals("Evaluating must remain an active publication phase", 0, cancelCalls)
        assertEquals(
            "null-to-finite accuracy refresh must publish while Evaluating",
            2, publishCalls.size
        )
        assertEquals(10f, publishCalls.last().accuracyMeters)
    }

    @Test
    fun `Evaluating candidate transition to terminal phase still cancels the live update`() {
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
        val cancelBefore = cancelCalls

        stateFlow.value = LocationAcquisitionState(
            sessionId = "s1",
            triggerType = TriggerType.MANUAL,
            phase = AcquisitionPhase.Evaluating,
            elapsedMs = 2000,
            bestLocation = locationSnapshot(accuracy = 50f)
        )
        testScope.advanceUntilIdle()
        assertEquals("Evaluating must not cancel", 0, cancelCalls)

        stateFlow.value = LocationAcquisitionState(
            sessionId = "s1",
            triggerType = TriggerType.MANUAL,
            phase = AcquisitionPhase.Cancelled,
            elapsedMs = 3000,
            bestLocation = locationSnapshot(accuracy = 50f)
        )
        testScope.advanceUntilIdle()
        assertTrue(
            "terminal transition from Evaluating must cancel the live update",
            cancelCalls > cancelBefore
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
            "baseline not advanced after false return, retry succeeds at 12500ms (past 2000ms throttle from last successful publish at 10000ms)",
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

    // ─── 高速档 Live Update ────────────────────────────────────

    private fun highSpeedRuntime(active: Boolean = true, elapsedSeconds: Long = 0L) =
        ForegroundLocationRuntimeState(
            highSpeedActive = active,
            highSpeedElapsedSeconds = elapsedSeconds
        )

    @Test
    fun `high speed active publishes high speed content`() {
        publisher.start(testScope)

        highSpeedRuntimeFlow.value = highSpeedRuntime(active = true, elapsedSeconds = 95)

        testScope.advanceUntilIdle()
        assertEquals(listOf(95L), publishHighSpeedElapsedCalls)
        assertTrue("session content must not be published", publishCalls.isEmpty())
    }

    @Test
    fun `high speed inactive cancels the live update`() {
        publisher.start(testScope)
        highSpeedRuntimeFlow.value = highSpeedRuntime(active = true, elapsedSeconds = 10)
        testScope.advanceUntilIdle()
        assertEquals(1, publishHighSpeedElapsedCalls.size)
        assertEquals(0, cancelCalls)

        highSpeedRuntimeFlow.value = highSpeedRuntime(active = false)

        testScope.advanceUntilIdle()
        assertEquals("fallback must cancel the high-speed live update", 1, cancelCalls)
    }

    @Test
    fun `high speed publishing is throttled to ten seconds`() {
        publisher.start(testScope)
        highSpeedRuntimeFlow.value = highSpeedRuntime(active = true, elapsedSeconds = 10)
        testScope.advanceUntilIdle()
        assertEquals(1, publishHighSpeedElapsedCalls.size)

        fakeClockMs = 5_000L
        highSpeedRuntimeFlow.value = highSpeedRuntime(active = true, elapsedSeconds = 20)
        testScope.advanceUntilIdle()
        assertEquals("within throttle window must not republish", 1, publishHighSpeedElapsedCalls.size)

        fakeClockMs = 11_000L
        highSpeedRuntimeFlow.value = highSpeedRuntime(active = true, elapsedSeconds = 30)
        testScope.advanceUntilIdle()
        assertEquals("after throttle window must republish with fresh elapsed", 2, publishHighSpeedElapsedCalls.size)
        assertEquals(30L, publishHighSpeedElapsedCalls.last())
    }

    @Test
    fun `high speed suppresses session publishing while active`() {
        publisher.start(testScope)
        highSpeedRuntimeFlow.value = highSpeedRuntime(active = true)
        testScope.advanceUntilIdle()

        stateFlow.value = LocationAcquisitionState(
            sessionId = "s1",
            triggerType = TriggerType.MANUAL,
            phase = AcquisitionPhase.Acquiring,
            elapsedMs = 1000
        )
        testScope.advanceUntilIdle()
        assertTrue("session must not publish while high speed active", publishCalls.isEmpty())
    }

    @Test
    fun `session publishing resumes after high speed falls back`() {
        publisher.start(testScope)
        stateFlow.value = LocationAcquisitionState(
            sessionId = "s1",
            triggerType = TriggerType.MANUAL,
            phase = AcquisitionPhase.Acquiring,
            elapsedMs = 1000
        )
        testScope.advanceUntilIdle()
        assertTrue("session must publish while high speed inactive", publishCalls.isNotEmpty())
        val beforeHighSpeed = publishCalls.size

        highSpeedRuntimeFlow.value = highSpeedRuntime(active = true, elapsedSeconds = 10)
        testScope.advanceUntilIdle()
        assertEquals(1, publishHighSpeedElapsedCalls.size)

        fakeClockMs = 30_000L
        highSpeedRuntimeFlow.value = highSpeedRuntime(active = false)
        testScope.advanceUntilIdle()
        assertTrue(
            "in-flight session content must be republished after fallback",
            publishCalls.size > beforeHighSpeed
        )
        assertEquals(1, cancelCalls)
    }

    @Test
    fun `cancelStaleNotification keeps published high speed live update`() {
        publisher.start(testScope)
        highSpeedRuntimeFlow.value = highSpeedRuntime(active = true, elapsedSeconds = 5)
        testScope.advanceUntilIdle()
        assertEquals(1, publishHighSpeedElapsedCalls.size)

        publisher.cancelStaleNotification()

        assertEquals("stale cleanup must not cancel the high-speed live update", 0, cancelCalls)
    }
}
