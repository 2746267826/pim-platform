package com.pim.app.location.acquisition

import com.pim.app.location.LocationSnapshot
import com.pim.app.location.quality.QualityAcceptedLocation
import com.pim.app.location.quality.RawLocationFix
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.double
import kotlinx.serialization.json.float
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class LocationAcquisitionCoordinatorTest {

    private lateinit var runner: FakeLocationAcquisitionRunner
    private lateinit var prerequisiteChecker: FakePrerequisiteChecker
    private lateinit var operations: TestLocationAcquisitionOperations
    private lateinit var coordinator: LocationAcquisitionCoordinator
    private var uuidCounter = 0L
    private var wallClockTime = 1_000_000L
    private var elapsedTime = 0L

    private val aSnapshot = LocationSnapshot(
        latitude = 31.23, longitude = 121.47,
        horizontalAccuracyMeters = 5f,
        provider = "gps", source = "test",
        altitudeMeters = 10.0,
        speedMetersPerSecond = null, bearingDegrees = null,
        timeMillis = 100L
    )

    private val lowQualitySnapshot = LocationSnapshot(
        latitude = 31.24, longitude = 121.48,
        horizontalAccuracyMeters = 100f,
        provider = "gps", source = "test",
        altitudeMeters = null,
        speedMetersPerSecond = null, bearingDegrees = null,
        timeMillis = 200L
    )

    private val automaticContext = AutomaticSessionContext(
        priority = 100,
        policyMode = "BatterySaver",
        scheduleLowFrequency = true,
        motionSignal = "Still"
    )

    @Before
    fun setUp() {
        runner = FakeLocationAcquisitionRunner()
        prerequisiteChecker = FakePrerequisiteChecker()
        operations = TestLocationAcquisitionOperations()
        uuidCounter = 0L
        wallClockTime = 1_000_000L
        elapsedTime = 0L
    }

    private fun createCoordinator(scope: CoroutineScope) {
        coordinator = LocationAcquisitionCoordinator(
            runner = runner,
            prerequisiteChecker = prerequisiteChecker,
            operations = operations,
            json = Json
        )
        coordinator.testScope = scope
        coordinator.uuidGenerator = { "test-session-${++uuidCounter}" }
        coordinator.wallClockMillis = { wallClockTime }
        coordinator.elapsedRealtimeMillis = { elapsedTime }
    }

    // ─── Manual success ─────────────────────────────────────────

    @Test
    fun `manual success reaches AwaitingManualSubmit and enqueue count stays zero`() = runTest {
        createCoordinator(this)
        prerequisiteChecker.ready()

        val result = coordinator.startManualSession()
        assertTrue(result is SessionStartResult.Started)

        runner.waitForAcquire()
        assertEquals(AcquisitionPhase.Acquiring, coordinator.state.value.phase)

        runner.emitCandidate(aSnapshot)
        runner.complete(LocationEngineResult(
            sessionId = runner.acquiredRequest!!.sessionId,
            bestLocation = aSnapshot,
            completion = LocationEngineCompletion.TimedOut
        ))
        advanceUntilIdle()

        assertEquals(AcquisitionPhase.AwaitingManualSubmit, coordinator.state.value.phase)
        assertEquals(aSnapshot, coordinator.state.value.bestLocation)
        assertEquals(0, operations.enqueueCount)
    }

    // ─── submitManualResult ─────────────────────────────────────

    @Test
    fun `submitManualResult enters Enqueuing then Completed and enqueues once with source manual`() = runTest {
        createCoordinator(this)
        prerequisiteChecker.ready()

        coordinator.startManualSession()
        runner.waitForAcquire()
        runner.emitCandidate(aSnapshot)
        runner.complete(LocationEngineResult(
            sessionId = runner.acquiredRequest!!.sessionId,
            bestLocation = aSnapshot,
            completion = LocationEngineCompletion.TimedOut
        ))
        advanceUntilIdle()
        assertEquals(AcquisitionPhase.AwaitingManualSubmit, coordinator.state.value.phase)

        coordinator.submitManualResult()
        advanceUntilIdle()

        assertEquals(AcquisitionPhase.Completed, coordinator.state.value.phase)
        assertEquals(1, operations.enqueueCount)
        assertEquals(0, operations.syncCount)
        assertEquals("manual", operations.lastSource)
    }

    // ─── Manual enqueue failure ─────────────────────────────────

    @Test
    fun `manual enqueue failure returns to AwaitingManualSubmit with same bestLocation and can be retried`() = runTest {
        createCoordinator(this)
        prerequisiteChecker.ready()

        coordinator.startManualSession()
        runner.waitForAcquire()
        runner.emitCandidate(aSnapshot)
        runner.complete(LocationEngineResult(
            sessionId = runner.acquiredRequest!!.sessionId,
            bestLocation = aSnapshot,
            completion = LocationEngineCompletion.TimedOut
        ))
        advanceUntilIdle()

        operations.failNextEnqueue = true
        coordinator.submitManualResult()
        advanceUntilIdle()

        assertEquals(AcquisitionPhase.AwaitingManualSubmit, coordinator.state.value.phase)
        assertEquals(aSnapshot, coordinator.state.value.bestLocation)

        operations.failNextEnqueue = false
        coordinator.submitManualResult()
        advanceUntilIdle()

        assertEquals(AcquisitionPhase.Completed, coordinator.state.value.phase)
        assertEquals(1, operations.enqueueCount)
    }

    // ─── Automatic success ──────────────────────────────────────

    @Test
    fun `automatic success enqueues exactly once with source auto and schedules sync exactly once`() = runTest {
        createCoordinator(this)
        prerequisiteChecker.ready()

        val result = coordinator.startAutomaticSession(automaticContext)
        assertTrue(result is SessionStartResult.Started)

        runner.waitForAcquire()
        runner.emitCandidate(aSnapshot)
        runner.complete(LocationEngineResult(
            sessionId = runner.acquiredRequest!!.sessionId,
            bestLocation = aSnapshot,
            completion = LocationEngineCompletion.TimedOut
        ))
        advanceUntilIdle()

        assertEquals(1, operations.enqueueCount)
        assertEquals(1, operations.syncCount)
        assertEquals("auto", operations.lastSource)
        assertTrue(coordinator.state.value.phase == AcquisitionPhase.Completed)
    }

    // ─── Automatic enqueue failure ──────────────────────────────

    @Test
    fun `automatic enqueue failure reaches Failed and is not retried`() = runTest {
        createCoordinator(this)
        prerequisiteChecker.ready()

        coordinator.startAutomaticSession(automaticContext)
        runner.waitForAcquire()
        runner.emitCandidate(aSnapshot)

        operations.failNextEnqueue = true
        runner.complete(LocationEngineResult(
            sessionId = runner.acquiredRequest!!.sessionId,
            bestLocation = aSnapshot,
            completion = LocationEngineCompletion.TimedOut
        ))
        advanceUntilIdle()

        assertEquals(AcquisitionPhase.Failed, coordinator.state.value.phase)
        assertEquals(aSnapshot, coordinator.state.value.bestLocation)
    }

    // ─── Low-quality manual ─────────────────────────────────────

    @Test
    fun `low quality manual result reaches Failed preserves bestLocation and cannot submit`() = runTest {
        createCoordinator(this)
        prerequisiteChecker.ready()

        coordinator.startManualSession()
        runner.waitForAcquire()
        runner.emitCandidate(lowQualitySnapshot)
        runner.complete(LocationEngineResult(
            sessionId = runner.acquiredRequest!!.sessionId,
            bestLocation = lowQualitySnapshot,
            completion = LocationEngineCompletion.TimedOut
        ))
        advanceUntilIdle()

        assertEquals(AcquisitionPhase.Failed, coordinator.state.value.phase)
        assertEquals(lowQualitySnapshot, coordinator.state.value.bestLocation)

        coordinator.submitManualResult()
        advanceUntilIdle()

        assertEquals(AcquisitionPhase.Failed, coordinator.state.value.phase)
        assertEquals(0, operations.enqueueCount)
    }

    // ─── Low-quality automatic ──────────────────────────────────

    @Test
    fun `low quality automatic result records exactly one dropped diagnostic`() = runTest {
        createCoordinator(this)
        prerequisiteChecker.ready()

        coordinator.startAutomaticSession(automaticContext)
        runner.waitForAcquire()
        runner.emitCandidate(lowQualitySnapshot)
        runner.complete(LocationEngineResult(
            sessionId = runner.acquiredRequest!!.sessionId,
            bestLocation = lowQualitySnapshot,
            completion = LocationEngineCompletion.TimedOut
        ))
        advanceUntilIdle()

        assertEquals(1, operations.recordDroppedCount)
        assertEquals(0, operations.enqueueCount)
    }

    // ─── No candidate ───────────────────────────────────────────

    @Test
    fun `no candidate reaches TimedOut`() = runTest {
        createCoordinator(this)
        prerequisiteChecker.ready()

        coordinator.startManualSession()
        runner.waitForAcquire()
        runner.complete(LocationEngineResult(
            sessionId = runner.acquiredRequest!!.sessionId,
            bestLocation = null,
            completion = LocationEngineCompletion.TimedOut
        ))
        advanceUntilIdle()

        assertEquals(AcquisitionPhase.TimedOut, coordinator.state.value.phase)
        assertNull(coordinator.state.value.bestLocation)
    }

    // ─── Precheck failure ───────────────────────────────────────

    @Test
    fun `precheck failure never invokes the Engine`() = runTest {
        createCoordinator(this)
        prerequisiteChecker.blocked("no gps")

        val result = coordinator.startManualSession()
        assertTrue(result is SessionStartResult.Rejected)

        advanceUntilIdle()
        assertFalse(runner.acquireCalled)
    }

    // ─── Busy states ────────────────────────────────────────────

    @Test
    fun `manual busy makes automatic start return Busy`() = runTest {
        createCoordinator(this)
        prerequisiteChecker.ready()

        coordinator.startManualSession()
        runner.waitForAcquire()

        assertEquals(SessionStartResult.Busy, coordinator.startAutomaticSession(automaticContext))

        runner.complete(LocationEngineResult("s1", null, LocationEngineCompletion.TimedOut))
        advanceUntilIdle()
    }

    @Test
    fun `automatic busy makes manual start return Busy`() = runTest {
        createCoordinator(this)
        prerequisiteChecker.ready()

        coordinator.startAutomaticSession(automaticContext)
        runner.waitForAcquire()

        assertEquals(SessionStartResult.Busy, coordinator.startManualSession())

        runner.complete(LocationEngineResult("s1", null, LocationEngineCompletion.TimedOut))
        advanceUntilIdle()
    }

    // ─── Cancellation ───────────────────────────────────────────

    @Test
    fun `matching session cancellation reaches Cancelled`() = runTest {
        createCoordinator(this)
        prerequisiteChecker.ready()

        val result = coordinator.startManualSession()
        val sessionId = (result as SessionStartResult.Started).sessionId

        coordinator.cancelCurrentSession(sessionId)
        advanceUntilIdle()

        assertEquals(AcquisitionPhase.Cancelled, coordinator.state.value.phase)
    }

    @Test
    fun `stale session ID is ignored by cancelCurrentSession`() = runTest {
        createCoordinator(this)
        prerequisiteChecker.ready()

        val started = coordinator.startManualSession() as SessionStartResult.Started
        runner.waitForAcquire()
        val sessionId = runner.acquiredRequest!!.sessionId
        assertEquals(started.sessionId, sessionId)

        coordinator.cancelCurrentSession("wrong-session-id")
        // Do not advanceUntilIdle(): the session ticker is an infinite delay loop.
        runCurrent()

        assertEquals(AcquisitionPhase.Acquiring, coordinator.state.value.phase)

        runner.complete(
            LocationEngineResult(
                sessionId = sessionId,
                bestLocation = null,
                completion = LocationEngineCompletion.TimedOut
            )
        )
        advanceUntilIdle()
    }

    // ─── Late engine callbacks with old session ID ──────────────

    @Test
    fun `late engine callbacks carrying an old session ID cannot mutate current state`() = runTest {
        createCoordinator(this)
        prerequisiteChecker.ready()

        coordinator.startManualSession()
        runner.waitForAcquire()
        val firstId = runner.acquiredRequest!!.sessionId

        coordinator.cancelCurrentSession(firstId)
        advanceUntilIdle()
        assertEquals(AcquisitionPhase.Cancelled, coordinator.state.value.phase)

        runner.complete(LocationEngineResult(firstId, lowQualitySnapshot, LocationEngineCompletion.TimedOut))
        advanceUntilIdle()

        assertEquals(AcquisitionPhase.Cancelled, coordinator.state.value.phase)
        assertNull(coordinator.state.value.bestLocation)
    }

    // ─── Manual restart ─────────────────────────────────────────

    @Test
    fun `manual restart may replace only a manual AwaitingManualSubmit state`() = runTest {
        createCoordinator(this)
        prerequisiteChecker.ready()

        coordinator.startManualSession()
        runner.waitForAcquire(0)
        runner.emitCandidate(aSnapshot, index = 0)
        runner.complete(
            LocationEngineResult("s1", aSnapshot, LocationEngineCompletion.TimedOut),
            index = 0
        )
        advanceUntilIdle()
        assertEquals(AcquisitionPhase.AwaitingManualSubmit, coordinator.state.value.phase)

        val replaceResult = coordinator.startManualSession(replaceAwaitingManual = true)
        assertTrue(replaceResult is SessionStartResult.Started)
        runner.waitForAcquire(1)
        runner.complete(
            LocationEngineResult(
                sessionId = (replaceResult as SessionStartResult.Started).sessionId,
                bestLocation = null,
                completion = LocationEngineCompletion.TimedOut
            ),
            index = 1
        )
        advanceUntilIdle()
    }

    @Test
    fun `manual restart without replaceAwaitingManual returns Busy when AwaitingManualSubmit`() = runTest {
        createCoordinator(this)
        prerequisiteChecker.ready()

        coordinator.startManualSession()
        runner.waitForAcquire()
        runner.emitCandidate(aSnapshot)
        runner.complete(LocationEngineResult("s1", aSnapshot, LocationEngineCompletion.TimedOut))
        advanceUntilIdle()

        assertEquals(SessionStartResult.Busy, coordinator.startManualSession(replaceAwaitingManual = false))
    }

    @Test
    fun `manual restart cannot replace an automatic session`() = runTest {
        createCoordinator(this)
        prerequisiteChecker.ready()

        coordinator.startAutomaticSession(automaticContext)
        runner.waitForAcquire()

        assertEquals(SessionStartResult.Busy, coordinator.startManualSession(replaceAwaitingManual = true))

        runner.complete(LocationEngineResult("s1", null, LocationEngineCompletion.TimedOut))
        advanceUntilIdle()
    }

    // ─── Spec gap 1: Preparing phase ────────────────────────────

    @Test
    fun `startManualSession exposes Preparing synchronously then Acquiring before runner`() = runTest {
        createCoordinator(this)
        prerequisiteChecker.ready()

        val result = coordinator.startManualSession() as SessionStartResult.Started
        assertEquals(AcquisitionPhase.Preparing, coordinator.state.value.phase)
        assertEquals(result.sessionId, coordinator.state.value.sessionId)
        assertFalse(runner.acquireCalled)

        runCurrent()

        assertEquals(AcquisitionPhase.Acquiring, coordinator.state.value.phase)
        assertTrue(runner.acquireCalled)
        assertEquals(result.sessionId, runner.acquiredRequest!!.sessionId)

        runner.complete(
            LocationEngineResult(
                sessionId = result.sessionId,
                bestLocation = null,
                completion = LocationEngineCompletion.TimedOut
            )
        )
        advanceUntilIdle()
    }

    // ─── Spec gap 2: preserve prerequisite reason ────────────────

    @Test
    fun `precheck blocked reason is preserved for manual and automatic`() = runTest {
        createCoordinator(this)
        prerequisiteChecker.blocked("缺少精确定位权限")

        val manual = coordinator.startManualSession()
        assertEquals(SessionStartResult.Rejected("缺少精确定位权限"), manual)
        assertFalse(runner.acquireCalled)

        val automatic = coordinator.startAutomaticSession(automaticContext)
        assertEquals(SessionStartResult.Rejected("缺少精确定位权限"), automatic)
        assertFalse(runner.acquireCalled)
    }

    // ─── Spec gap 3: altitude deadline cap ───────────────────────

    @Test
    fun `altitude wait accepts at overall session wall-clock deadline cap`() = runTest {
        val sessionStartWall = 1_000_000L
        wallClockTime = sessionStartWall
        createCoordinator(this)
        coordinator.wallClockMillis = { sessionStartWall + testScheduler.currentTime }
        prerequisiteChecker.ready()

        val missingAltitudeAtT25 = LocationSnapshot(
            latitude = 31.23,
            longitude = 121.47,
            horizontalAccuracyMeters = 5f,
            provider = "gps",
            source = "test",
            altitudeMeters = null,
            speedMetersPerSecond = null,
            bearingDegrees = null,
            timeMillis = sessionStartWall + 25_000L
        )

        val started = coordinator.startManualSession() as SessionStartResult.Started
        runner.waitForAcquire()
        advanceTimeBy(25_000L)
        runner.emitCandidate(missingAltitudeAtT25)
        runner.complete(
            LocationEngineResult(
                sessionId = started.sessionId,
                bestLocation = missingAltitudeAtT25,
                completion = LocationEngineCompletion.TimedOut
            )
        )
        runCurrent()
        advanceTimeBy(5_000L)
        runCurrent()
        advanceUntilIdle()

        assertEquals(30_000L, testScheduler.currentTime)
        assertEquals(AcquisitionPhase.AwaitingManualSubmit, coordinator.state.value.phase)
        assertEquals(missingAltitudeAtT25, coordinator.state.value.bestLocation)
        assertFalse(runner.isAcquireActive)
    }

    // ─── Spec gap 4: old cleanup must not clear new job ──────────

    @Test
    fun `old session cleanup does not orphan the replacement session job`() = runTest {
        createCoordinator(this)
        prerequisiteChecker.ready()

        val first = coordinator.startManualSession() as SessionStartResult.Started
        runner.waitForAcquire(0)
        assertEquals(first.sessionId, runner.sessionAt(0).request.sessionId)

        // Hold first-session engine cleanup while a replacement starts.
        val firstCleanup = runner.holdCompletion(0)

        coordinator.cancelCurrentSession(first.sessionId)
        runCurrent()

        val second = coordinator.startManualSession() as SessionStartResult.Started
        runner.waitForAcquire(1)
        assertEquals(second.sessionId, coordinator.state.value.sessionId)
        assertEquals(AcquisitionPhase.Acquiring, coordinator.state.value.phase)

        // Release old cleanup after the new session owns sessionJob.
        firstCleanup.complete(Unit)
        runCurrent()

        // New matching session must still be cancellable / managed.
        coordinator.cancelCurrentSession(second.sessionId)
        runCurrent()
        runner.complete(
            LocationEngineResult(
                sessionId = second.sessionId,
                bestLocation = aSnapshot,
                completion = LocationEngineCompletion.TimedOut
            ),
            index = 1
        )
        advanceUntilIdle()

        assertEquals(AcquisitionPhase.Cancelled, coordinator.state.value.phase)
        assertNull(coordinator.state.value.bestLocation)
        assertFalse(runner.isAcquireActive)
    }

    // ─── Spec gap 5: automatic post-enqueue session guard ───────

    @Test
    fun `automatic enqueue completion cannot overwrite a newer session`() = runTest {
        createCoordinator(this)
        prerequisiteChecker.ready()

        val enqueueGate = CompletableDeferred<Unit>()
        operations.enqueueBlock = enqueueGate

        val first = coordinator.startAutomaticSession(automaticContext) as SessionStartResult.Started
        runner.waitForAcquire(0)
        runner.emitCandidate(aSnapshot, index = 0)
        runner.complete(
            LocationEngineResult(
                sessionId = first.sessionId,
                bestLocation = aSnapshot,
                completion = LocationEngineCompletion.TimedOut
            ),
            index = 0
        )
        runCurrent()
        assertEquals(AcquisitionPhase.Enqueuing, coordinator.state.value.phase)

        coordinator.cancelCurrentSession(first.sessionId)
        runCurrent()

        val second = coordinator.startAutomaticSession(automaticContext) as SessionStartResult.Started
        runner.waitForAcquire(1)
        assertEquals(second.sessionId, coordinator.state.value.sessionId)
        assertEquals(AcquisitionPhase.Acquiring, coordinator.state.value.phase)

        enqueueGate.complete(Unit)
        runCurrent()

        assertEquals(second.sessionId, coordinator.state.value.sessionId)
        assertEquals(AcquisitionPhase.Acquiring, coordinator.state.value.phase)
        assertTrue(operations.syncCount <= 1)
        assertTrue(operations.enqueueCount <= 1)

        runner.complete(
            LocationEngineResult(
                sessionId = second.sessionId,
                bestLocation = null,
                completion = LocationEngineCompletion.TimedOut
            ),
            index = 1
        )
        advanceUntilIdle()
        assertEquals(AcquisitionPhase.TimedOut, coordinator.state.value.phase)
        assertEquals(second.sessionId, coordinator.state.value.sessionId)
    }

    // ─── Spec gap 6: structured recordDropped ───────────────────

    @Test
    fun `automatic recordDropped is cancelled with the session and does not outlive it`() = runTest {
        createCoordinator(this)
        prerequisiteChecker.ready()

        val dropEntered = CompletableDeferred<Unit>()
        val dropGate = CompletableDeferred<Unit>()
        operations.recordDroppedEntered = dropEntered
        operations.recordDroppedBlock = dropGate

        val started = coordinator.startAutomaticSession(automaticContext) as SessionStartResult.Started
        runner.waitForAcquire()
        runner.emitCandidate(lowQualitySnapshot)
        runCurrent()
        dropEntered.await()

        coordinator.cancelCurrentSession(started.sessionId)
        runCurrent()
        dropGate.complete(Unit)
        advanceUntilIdle()

        assertEquals(0, operations.recordDroppedCount)
        assertEquals(AcquisitionPhase.Cancelled, coordinator.state.value.phase)
    }

    // ─── Quality fix 1: atomic manual submit claim ─────────────

    @Test
    fun `submitManualResult atomically claims submission before launching and drops duplicate submit`() = runTest {
        createCoordinator(this)
        prerequisiteChecker.ready()

        coordinator.startManualSession()
        runner.waitForAcquire()
        runner.emitCandidate(aSnapshot)
        runner.complete(LocationEngineResult(
            sessionId = runner.acquiredRequest!!.sessionId,
            bestLocation = aSnapshot,
            completion = LocationEngineCompletion.TimedOut
        ))
        advanceUntilIdle()
        assertEquals(AcquisitionPhase.AwaitingManualSubmit, coordinator.state.value.phase)

        coordinator.submitManualResult()
        // Phase must transition to Enqueuing synchronously, before queued coroutines run.
        assertEquals(AcquisitionPhase.Enqueuing, coordinator.state.value.phase)

        // A second submit before queued work runs must be a no-op (already claimed).
        coordinator.submitManualResult()
        assertEquals(AcquisitionPhase.Enqueuing, coordinator.state.value.phase)

        advanceUntilIdle()

        assertEquals(AcquisitionPhase.Completed, coordinator.state.value.phase)
        assertEquals(1, operations.enqueueCount)
        assertEquals("manual", operations.lastSource)
    }

    // ─── Quality fix 2: structured Json encoding ───────────────

    @Test
    fun `enqueueAccepted rawJson encodes control characters and parses back via kotlinx Json`() = runTest {
        createCoordinator(this)
        prerequisiteChecker.ready()

        val controlCharSnapshot = LocationSnapshot(
            latitude = 31.23,
            longitude = 121.47,
            horizontalAccuracyMeters = 5f,
            provider = "gp\u0001s",
            source = "test",
            altitudeMeters = 10.0,
            speedMetersPerSecond = Float.NaN,
            bearingDegrees = null,
            timeMillis = 100L
        )

        coordinator.startAutomaticSession(automaticContext)
        runner.waitForAcquire()
        runner.emitCandidate(controlCharSnapshot)
        runner.complete(LocationEngineResult(
            sessionId = runner.acquiredRequest!!.sessionId,
            bestLocation = controlCharSnapshot,
            completion = LocationEngineCompletion.TimedOut
        ))
        advanceUntilIdle()

        assertEquals(1, operations.enqueueCount)
        val rawJson = operations.lastRawJson
        assertNotNull(rawJson)
        // Valid JSON must escape control characters; the raw JSON string must
        // contain the \u0001 escape sequence, not a literal control byte.
        assertTrue("rawJson must escape U+0001 as \\u0001", rawJson!!.contains("\\u0001"))
        val parsed = Json.parseToJsonElement(rawJson!!).jsonObject
        assertEquals("gp\u0001s", parsed["provider"]!!.jsonPrimitive.content)
        assertEquals(31.23, parsed["latitude"]!!.jsonPrimitive.double, 0.0)
        assertEquals(121.47, parsed["longitude"]!!.jsonPrimitive.double, 0.0)
        assertEquals(5.0f, parsed["horizontalAccuracyMeters"]!!.jsonPrimitive.float, 0.0f)
        assertEquals("auto", parsed["source"]!!.jsonPrimitive.content)
        assertEquals(10.0, parsed["altitudeMeters"]!!.jsonPrimitive.double, 0.0)
        assertTrue(parsed["speedMetersPerSecond"] is JsonNull)
        assertTrue(parsed["bearingDegrees"] is JsonNull)
    }
}

// ─── Test fakes ─────────────────────────────────────────────────

class FakeLocationAcquisitionRunner : LocationAcquisitionRunner {

    class AcquireSession(
        val request: LocationEngineRequest,
        val onCandidate: suspend (LocationSnapshot) -> Unit,
        val result: CompletableDeferred<LocationEngineResult> = CompletableDeferred(),
        var completionHold: CompletableDeferred<Unit>? = null
    )

    private val sessions = mutableListOf<AcquireSession>()
    private val waiters = mutableListOf<CompletableDeferred<Int>>()

    val acquireCalled: Boolean get() = sessions.isNotEmpty()
    val acquiredRequest: LocationEngineRequest? get() = sessions.lastOrNull()?.request
    val isAcquireActive: Boolean
        get() = sessions.any { !it.result.isCompleted }

    fun sessionAt(index: Int): AcquireSession = sessions[index]

    override suspend fun acquire(
        request: LocationEngineRequest,
        onCandidate: suspend (LocationSnapshot) -> Unit,
        onAvailabilityChanged: suspend (Boolean) -> Unit
    ): LocationEngineResult {
        val session = AcquireSession(request, onCandidate)
        sessions += session
        waiters.toList().forEach { it.complete(sessions.lastIndex) }
        waiters.clear()
        try {
            val value = session.result.await()
            awaitCompletionHold(session)
            return value
        } catch (e: CancellationException) {
            awaitCompletionHold(session)
            if (!session.result.isCompleted) {
                session.result.cancel(e)
            }
            throw e
        }
    }

    private suspend fun awaitCompletionHold(session: AcquireSession) {
        val hold = session.completionHold ?: return
        withContext(NonCancellable) {
            try {
                withTimeout(5_000L) { hold.await() }
            } catch (error: TimeoutCancellationException) {
                throw AssertionError("completion hold was not released", error)
            }
        }
    }

    fun holdCompletion(index: Int): CompletableDeferred<Unit> {
        val hold = CompletableDeferred<Unit>()
        sessions[index].completionHold = hold
        return hold
    }

    suspend fun waitForAcquire(index: Int = 0): LocationEngineRequest {
        while (sessions.size <= index) {
            val waiter = CompletableDeferred<Int>()
            waiters += waiter
            if (sessions.size > index) {
                waiters.remove(waiter)
                break
            }
            waiter.await()
        }
        return sessions[index].request
    }

    suspend fun emitCandidate(snapshot: LocationSnapshot, index: Int = sessions.lastIndex) {
        sessions[index].onCandidate(snapshot)
    }

    fun complete(result: LocationEngineResult, index: Int = sessions.lastIndex) {
        sessions[index].result.complete(result)
    }
}

class FakePrerequisiteChecker : LocationPrerequisiteChecker {
    private var _result: LocationPrerequisiteResult = LocationPrerequisiteResult.Ready

    fun ready() { _result = LocationPrerequisiteResult.Ready }
    fun blocked(reason: String) { _result = LocationPrerequisiteResult.Blocked(reason) }

    override fun check(triggerType: TriggerType): LocationPrerequisiteResult = _result
}

class TestLocationAcquisitionOperations : LocationAcquisitionOperations {
    var enqueueCount = 0
    var recordDroppedCount = 0
    var syncCount = 0
    var failNextEnqueue = false
    var lastSource: String? = null
    var lastRawJson: String? = null
    var enqueueBlock: CompletableDeferred<Unit>? = null
    var recordDroppedBlock: CompletableDeferred<Unit>? = null
    var recordDroppedEntered: CompletableDeferred<Unit>? = null

    override suspend fun enqueueAccepted(accepted: QualityAcceptedLocation, rawJson: String, source: String) {
        enqueueBlock?.await()
        lastSource = source
        lastRawJson = rawJson
        if (failNextEnqueue) throw RuntimeException("enqueue-fail")
        enqueueCount++
    }

    override suspend fun recordDropped(fix: RawLocationFix, reason: String) {
        recordDroppedEntered?.complete(Unit)
        recordDroppedBlock?.await()
        recordDroppedCount++
    }

    override fun scheduleSync() {
        syncCount++
    }
}
