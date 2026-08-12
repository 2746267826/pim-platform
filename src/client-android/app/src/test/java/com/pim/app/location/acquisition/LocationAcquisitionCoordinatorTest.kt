package com.pim.app.location.acquisition

import com.pim.app.location.LocationSnapshot
import com.pim.app.location.quality.QualityAcceptedLocation
import com.pim.app.location.quality.RawLocationFix
import com.pim.app.settings.TrackingSettings
import com.pim.app.settings.TrackingSettingsStore
import com.pim.app.testing.InMemorySharedPreferences
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
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.double
import kotlinx.serialization.json.float
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import kotlin.concurrent.thread

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

    private val mediumQualitySnapshot = LocationSnapshot(
        latitude = 31.24, longitude = 121.48,
        horizontalAccuracyMeters = 40f,
        provider = "gps", source = "test",
        altitudeMeters = 10.0,
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

    private fun createCoordinator(
        scope: CoroutineScope,
        trackingSettingsStore: TrackingSettingsStore = TrackingSettingsStore(InMemorySharedPreferences())
    ) {
        coordinator = LocationAcquisitionCoordinator(
            runner = runner,
            prerequisiteChecker = prerequisiteChecker,
            operations = operations,
            json = Json,
            trackingSettingsStore = trackingSettingsStore
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
    fun `submitManualResult enters Enqueuing then Completed enqueues once and schedules sync exactly once with source manual`() = runTest {
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
        assertEquals(1, operations.syncCount)
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
        assertEquals(0, operations.enqueueCount)
        assertEquals(0, operations.syncCount)

        operations.failNextEnqueue = false
        coordinator.submitManualResult()
        advanceUntilIdle()

        assertEquals(AcquisitionPhase.Completed, coordinator.state.value.phase)
        assertEquals(1, operations.enqueueCount)
        assertEquals(1, operations.syncCount)
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

    @Test
    fun `no candidate with engine Failed completion reaches Failed with errorReason`() = runTest {
        createCoordinator(this)
        prerequisiteChecker.ready()

        coordinator.startManualSession()
        runner.waitForAcquire()
        runner.complete(LocationEngineResult(
            sessionId = runner.acquiredRequest!!.sessionId,
            bestLocation = null,
            completion = LocationEngineCompletion.Failed("engine crashed")
        ))
        advanceUntilIdle()

        assertEquals(AcquisitionPhase.Failed, coordinator.state.value.phase)
        assertEquals("engine crashed", coordinator.state.value.errorReason)
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
    fun `matching session cancellation reaches Cancelled and retains sessionId`() = runTest {
        createCoordinator(this)
        prerequisiteChecker.ready()

        val result = coordinator.startManualSession()
        val sessionId = (result as SessionStartResult.Started).sessionId

        coordinator.cancelCurrentSession(sessionId)
        advanceUntilIdle()

        assertEquals(AcquisitionPhase.Cancelled, coordinator.state.value.phase)
        assertEquals(
            "cancelled terminal state must retain its sessionId so waiters can match it",
            sessionId,
            coordinator.state.value.sessionId
        )

        // Restart behavior: a new session can start after the cancelled terminal state.
        val restart = coordinator.startManualSession() as SessionStartResult.Started
        runner.waitForAcquire()
        assertNotEquals(sessionId, restart.sessionId)
        assertEquals(AcquisitionPhase.Acquiring, coordinator.state.value.phase)

        runner.complete(
            LocationEngineResult(
                sessionId = restart.sessionId,
                bestLocation = null,
                completion = LocationEngineCompletion.TimedOut
            )
        )
        advanceUntilIdle()
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

    // ─── Terminal cancellation is a no-op ───────────────────────

    @Test
    fun `cancel after a completed manual session is a no-op and preserves the result`() = runTest {
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
        coordinator.submitManualResult()
        advanceUntilIdle()
        assertEquals(AcquisitionPhase.Completed, coordinator.state.value.phase)

        // Late cancel intents (e.g. from NotificationActionReceiver) must not
        // relabel an already-completed result, but the sessionId must stay for
        // service waiters.
        coordinator.cancelCurrentSession()
        advanceUntilIdle()

        assertEquals(AcquisitionPhase.Completed, coordinator.state.value.phase)
        assertEquals(aSnapshot, coordinator.state.value.bestLocation)
        assertEquals(1, operations.enqueueCount)
        assertEquals(1, operations.syncCount)
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

    @Test
    fun `blocked manual restart preserves AwaitingManualSubmit and allows later submit`() = runTest {
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

        // The replacement attempt is blocked by the prerequisite check: the
        // accepted-but-unsubmitted manual result must survive the attempt.
        prerequisiteChecker.blocked("no gps")
        val result = coordinator.startManualSession(replaceAwaitingManual = true)
        assertEquals(SessionStartResult.Rejected("no gps"), result)

        assertEquals(
            "a blocked replacement must preserve the AwaitingManualSubmit result",
            AcquisitionPhase.AwaitingManualSubmit,
            coordinator.state.value.phase
        )
        assertEquals(aSnapshot, coordinator.state.value.bestLocation)

        // The preserved result can still be submitted once prerequisites are ready.
        prerequisiteChecker.ready()
        coordinator.submitManualResult()
        advanceUntilIdle()

        assertEquals(AcquisitionPhase.Completed, coordinator.state.value.phase)
        assertEquals(1, operations.enqueueCount)
        assertEquals(1, operations.syncCount)
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
        assertEquals(AcquisitionPhase.Idle, coordinator.state.value.phase)
        assertNull(coordinator.state.value.sessionId)
        assertNull(coordinator.state.value.triggerType)
        assertEquals("缺少精确定位权限", coordinator.state.value.errorReason)

        val automatic = coordinator.startAutomaticSession(automaticContext)
        assertEquals(SessionStartResult.Rejected("缺少精确定位权限"), automatic)
        assertFalse(runner.acquireCalled)
        assertEquals(AcquisitionPhase.Idle, coordinator.state.value.phase)
        assertNull(coordinator.state.value.sessionId)
        assertNull(coordinator.state.value.triggerType)
        assertEquals("缺少精确定位权限", coordinator.state.value.errorReason)
    }

    @Test
    fun `precheck blocked sets idle state preserving reason and allows retry`() = runTest {
        createCoordinator(this)
        prerequisiteChecker.blocked("系统定位服务未开启")

        val result1 = coordinator.startManualSession()
        assertTrue(result1 is SessionStartResult.Rejected)
        assertEquals("系统定位服务未开启", coordinator.state.value.errorReason)
        assertEquals(AcquisitionPhase.Idle, coordinator.state.value.phase)

        prerequisiteChecker.ready()

        val result2 = coordinator.startManualSession()
        assertTrue(result2 is SessionStartResult.Started)
        assertNull(coordinator.state.value.errorReason)
        assertEquals(AcquisitionPhase.Preparing, coordinator.state.value.phase)

        runner.waitForAcquire()
        runner.complete(LocationEngineResult(
            sessionId = (result2 as SessionStartResult.Started).sessionId,
            bestLocation = null,
            completion = LocationEngineCompletion.TimedOut
        ))
        advanceUntilIdle()
    }

    @Test
    fun `precheck blocked does not overwrite active session`() = runTest {
        createCoordinator(this)
        prerequisiteChecker.ready()

        val started = coordinator.startAutomaticSession(automaticContext) as SessionStartResult.Started
        runner.waitForAcquire()
        assertEquals(AcquisitionPhase.Acquiring, coordinator.state.value.phase)

        prerequisiteChecker.blocked("缺少精确定位权限")

        val busyResult = coordinator.startManualSession()
        assertEquals(SessionStartResult.Busy, busyResult)
        assertEquals(started.sessionId, coordinator.state.value.sessionId)
        assertEquals(TriggerType.AUTOMATIC, coordinator.state.value.triggerType)
        assertNull(coordinator.state.value.errorReason)

        runner.complete(LocationEngineResult(
            sessionId = started.sessionId,
            bestLocation = null,
            completion = LocationEngineCompletion.TimedOut
        ))
        advanceUntilIdle()
    }

    // ─── Wall-clock rollback regression (user bug report) ────────

    @Test
    fun `wall clock rollback after candidate arrival does not hang the session`() = runTest {
        val sessionStartWall = 1_000_000L
        wallClockTime = sessionStartWall
        createCoordinator(this)
        // elapsedRealtime is the monotonic authority and keeps advancing normally
        // even while the wall clock is rolled back (NTP correction after reboot).
        coordinator.elapsedRealtimeMillis = { testScheduler.currentTime }
        var rolledBack = false
        // Wall clock is correct until a candidate arrives, then NTP-style correction
        // rolls the wall clock back 600s. The GPS timestamp (Location.time) stays on
        // satellite time, so fix.recordedAtMillis is 600s ahead of nowMillis().
        coordinator.wallClockMillis = {
            if (rolledBack) sessionStartWall + testScheduler.currentTime - 600_000L
            else sessionStartWall + testScheduler.currentTime
        }
        prerequisiteChecker.ready()

        val missingAltitude = LocationSnapshot(
            latitude = 31.23,
            longitude = 121.47,
            horizontalAccuracyMeters = 5f,
            provider = "gps",
            source = "test",
            altitudeMeters = null,
            speedMetersPerSecond = null,
            bearingDegrees = null,
            timeMillis = sessionStartWall + 5_000L
        )

        val started = coordinator.startManualSession() as SessionStartResult.Started
        runner.waitForAcquire()
        advanceTimeBy(5_000L)
        rolledBack = true
        runner.emitCandidate(missingAltitude)
        runner.complete(
            LocationEngineResult(
                sessionId = started.sessionId,
                bestLocation = missingAltitude,
                completion = LocationEngineCompletion.TimedOut
            )
        )
        runCurrent()

        // Bug reproduction: with a 600s wall-clock rollback, the quality wait computes
        // wallClockRemaining = deadline(1_005_000+20_000) - nowMillis(405_000) = 620s,
        // but the monotonic cap (30_000 - 5_000 elapsed = 25_000) must win, so the
        // session ends by the 30s deadline instead of hanging ~620s.
        advanceTimeBy(30_000L)
        runCurrent()
        assertTrue(
            "session must end within the 30s deadline; wall-clock rollback must not stretch the wait",
            coordinator.state.value.phase == AcquisitionPhase.AwaitingManualSubmit ||
                coordinator.state.value.phase == AcquisitionPhase.TimedOut ||
                coordinator.state.value.phase == AcquisitionPhase.Failed
        )
        assertFalse(runner.isAcquireActive)
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

    // ─── Spec gap 7: cancel must clean up only its own snapshot's job ──

    @Test
    fun `cancellation cleanup keeps a new session owner intact when a new session starts after the Cancelled claim wins`() = runTest {
        createCoordinator(this)
        prerequisiteChecker.ready()

        val first = coordinator.startManualSession() as SessionStartResult.Started
        runner.waitForAcquire(0)
        assertEquals(AcquisitionPhase.Acquiring, coordinator.state.value.phase)

        // Test seam: a new manual session starts immediately after the old
        // cancellation wins its Cancelled state claim, before the old method
        // picks up / cancels a job. The new session must not be cancelled.
        // The seam disarms itself so a later cancel claim does not start yet
        // another session.
        var secondId: String? = null
        coordinator.afterSessionCancelledClaim = {
            coordinator.afterSessionCancelledClaim = null
            val started = coordinator.startManualSession()
            secondId = (started as SessionStartResult.Started).sessionId
        }

        val firstCancelled = coordinator.cancelCurrentSession(first.sessionId)
        assertTrue("first-session cancellation must report true", firstCancelled)
        assertNotNull("the seam must have started a second session", secondId)
        assertNotEquals("second session must have a distinct id", first.sessionId, secondId)

        runCurrent()

        assertEquals(
            "the old cancellation must not cancel the session started in its claim window",
            AcquisitionPhase.Acquiring,
            coordinator.state.value.phase
        )
        assertEquals(secondId, coordinator.state.value.sessionId)
        assertEquals(secondId, runner.sessionAt(1).request.sessionId)

        // The second session must remain cancellable through the normal path.
        val secondCancelled = coordinator.cancelCurrentSession(secondId!!)
        assertTrue("second session must be cancellable", secondCancelled)
        runCurrent()

        runner.complete(
            LocationEngineResult(
                sessionId = secondId!!,
                bestLocation = null,
                completion = LocationEngineCompletion.TimedOut
            ),
            index = 1
        )
        advanceUntilIdle()

        assertEquals(AcquisitionPhase.Cancelled, coordinator.state.value.phase)
    }

    // ─── Spec gap 5: automatic post-enqueue session guard ───────

    @Test
    fun `cancellation during Enqueuing is ignored so in-flight automatic submission completes`() = runTest {
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

        assertEquals(
            "cancellation during Enqueuing must be ignored",
            AcquisitionPhase.Enqueuing,
            coordinator.state.value.phase
        )
        assertEquals(first.sessionId, coordinator.state.value.sessionId)

        enqueueGate.complete(Unit)
        advanceUntilIdle()

        assertEquals(AcquisitionPhase.Completed, coordinator.state.value.phase)
        assertEquals(first.sessionId, coordinator.state.value.sessionId)
        assertEquals(1, operations.enqueueCount)
        assertEquals(1, operations.syncCount)
    }

    @Test
    fun `cancellation during manual Enqueuing is ignored so in-flight submission completes`() = runTest {
        createCoordinator(this)
        prerequisiteChecker.ready()

        val enqueueGate = CompletableDeferred<Unit>()
        operations.enqueueBlock = enqueueGate

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
        runCurrent()
        assertEquals(AcquisitionPhase.Enqueuing, coordinator.state.value.phase)

        coordinator.cancelCurrentSession()
        runCurrent()

        assertEquals(
            "cancellation during Enqueuing must be ignored",
            AcquisitionPhase.Enqueuing,
            coordinator.state.value.phase
        )

        enqueueGate.complete(Unit)
        advanceUntilIdle()

        assertEquals(AcquisitionPhase.Completed, coordinator.state.value.phase)
        assertEquals(1, operations.enqueueCount)
        assertEquals(1, operations.syncCount)
    }

    @Test
    fun `cancellation before automatic Enqueuing claim prevents enqueue and sync`() = runTest {
        createCoordinator(this)
        prerequisiteChecker.ready()

        val first = coordinator.startAutomaticSession(automaticContext) as SessionStartResult.Started
        runner.waitForAcquire(0)

        // Deterministic cancellation TOCTOU: the cancel lands after quality has
        // accepted the fix but before handleAccepted atomically claims Enqueuing.
        val claimReached = CompletableDeferred<Unit>()
        coordinator.beforeAutomaticEnqueueClaim = {
            coordinator.cancelCurrentSession(first.sessionId)
            claimReached.complete(Unit)
        }

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
        claimReached.await()
        assertEquals(AcquisitionPhase.Cancelled, coordinator.state.value.phase)

        advanceUntilIdle()

        assertEquals(
            "a user seeing Cancelled must not get a new automatic queued point",
            AcquisitionPhase.Cancelled,
            coordinator.state.value.phase
        )
        assertEquals(0, operations.enqueueCount)
        assertEquals(0, operations.syncCount)
    }

    @Test
    fun `cancel racing the automatic Enqueuing claim loses and the in-flight enqueue completes`() = runTest {
        createCoordinator(this)
        prerequisiteChecker.ready()

        val enqueueGate = CompletableDeferred<Unit>()
        operations.enqueueBlock = enqueueGate
        // The automatic session parks at the claim seam (a real latch) until the
        // cancel thread has paused after reading the Evaluating snapshot.
        val cancelStart = CountDownLatch(1)
        val cancelPaused = CountDownLatch(1)
        val cancelGo = CountDownLatch(1)
        var cancelResult = true
        var cancelObservedPhase: AcquisitionPhase? = null
        coordinator.beforeAutomaticEnqueueClaim = {
            cancelStart.countDown()
            cancelPaused.await(5, TimeUnit.SECONDS)
        }
        coordinator.beforeCancellingSessionJob = {
            cancelObservedPhase = coordinator.state.value.phase
            cancelPaused.countDown()
            cancelGo.await(5, TimeUnit.SECONDS)
        }

        val first = coordinator.startAutomaticSession(automaticContext) as SessionStartResult.Started
        runner.waitForAcquire(0)

        val cancelThread = thread(isDaemon = true, name = "cancel-session") {
            cancelStart.await(5, TimeUnit.SECONDS)
            cancelResult = coordinator.cancelCurrentSession(first.sessionId)
        }

        runner.emitCandidate(aSnapshot, index = 0)
        runner.complete(
            LocationEngineResult(
                sessionId = first.sessionId,
                bestLocation = aSnapshot,
                completion = LocationEngineCompletion.TimedOut
            ),
            index = 0
        )
        // The session reaches Evaluating, the cancel thread reads it and pauses at
        // its seam, then the automatic claim wins Enqueuing and blocks enqueueing.
        runCurrent()
        assertEquals(
            "the automatic claim must win the Evaluating -> Enqueuing transition",
            AcquisitionPhase.Enqueuing,
            coordinator.state.value.phase
        )
        assertEquals(
            "the cancel must have paused on the stale Evaluating snapshot",
            AcquisitionPhase.Evaluating,
            cancelObservedPhase
        )

        cancelGo.countDown()
        cancelThread.join(5_000)
        assertFalse("cancel thread must have finished", cancelThread.isAlive)
        assertFalse(
            "a cancel whose CAS lost to the Enqueuing claim must return false without cancelling the job",
            cancelResult
        )
        assertEquals(
            "the session must stay Enqueuing: the stale-read cancel must not cancel the job",
            AcquisitionPhase.Enqueuing,
            coordinator.state.value.phase
        )

        enqueueGate.complete(Unit)
        advanceUntilIdle()

        assertEquals(AcquisitionPhase.Completed, coordinator.state.value.phase)
        assertEquals(first.sessionId, coordinator.state.value.sessionId)
        assertEquals(1, operations.enqueueCount)
        assertEquals(1, operations.syncCount)
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
        assertEquals(1, operations.syncCount)
        assertEquals("manual", operations.lastSource)
    }

    // ─── Quality fix 2: structured Json encoding ───────────────

    @Test
    fun `eager dispatcher session reaches Acquiring`() = runTest(UnconfinedTestDispatcher()) {
        createCoordinator(this)
        prerequisiteChecker.ready()

        coordinator.startManualSession()
        // Before the fix, startSession launches the coroutine (and passes the
        // uninitialized lateinit job to runSession) before assigning sessionJob.
        // With an eager dispatcher this makes the phase stay at Preparing or
        // crashes with UninitializedPropertyAccessException.
        assertEquals(AcquisitionPhase.Acquiring, coordinator.state.value.phase)

        coordinator.cancelCurrentSession()
        advanceUntilIdle()
    }

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

    // ─── Fixed 20m quality gate ────────────────────────────────

    @Test
    fun `session applies the fixed 20m quality gate`() = runTest {
        createCoordinator(this)
        prerequisiteChecker.ready()

        // A 40m fix exceeds the fixed 20m gate and must never be enqueued,
        // regardless of any stored settings value.
        coordinator.startAutomaticSession(automaticContext)
        runner.waitForAcquire()
        runner.emitCandidate(mediumQualitySnapshot)
        runner.complete(LocationEngineResult(
            sessionId = runner.acquiredRequest!!.sessionId,
            bestLocation = mediumQualitySnapshot,
            completion = LocationEngineCompletion.TimedOut
        ))
        advanceUntilIdle()

        assertEquals(AcquisitionPhase.Failed, coordinator.state.value.phase)
        assertEquals(1, operations.recordDroppedCount)
        assertEquals(0, operations.enqueueCount)
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
