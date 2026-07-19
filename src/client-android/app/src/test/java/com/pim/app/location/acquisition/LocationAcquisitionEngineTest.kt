package com.pim.app.location.acquisition

import com.pim.app.location.LocationSnapshot
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class LocationAcquisitionEngineTest {

    @Test
    fun `timeout with no candidate returns TimedOut`() = runTest {
        val source = FakeLocationUpdateSource()
        val engine = LocationAcquisitionEngine(source)

        val request = LocationEngineRequest(
            sessionId = "session-1",
            priority = 100,
            timeoutMillis = 1_000L,
            startedAtWallClockMillis = 0L
        )

        val candidates = mutableListOf<LocationSnapshot>()
        val availabilities = mutableListOf<Boolean>()
        val result = engine.acquire(
            request = request,
            onCandidate = { candidates += it },
            onAvailabilityChanged = { availabilities += it }
        )

        assertTrue(result.completion is LocationEngineCompletion.TimedOut)
        assertNull(result.bestLocation)
        assertEquals("session-1", result.sessionId)
        assertTrue(candidates.isEmpty())
        assertTrue(source.isClosed)
    }

    @Test
    fun `cancelling acquire rethrows cancellation and closes source`() = runTest {
        val source = FakeLocationUpdateSource()
        val engine = LocationAcquisitionEngine(source)
        val request = LocationEngineRequest(
            sessionId = "s", priority = 100, timeoutMillis = 10_000L, startedAtWallClockMillis = 0L
        )

        var caughtCancellation = false
        val job = launch {
            try {
                engine.acquire(request, {}, {})
            } catch (e: CancellationException) {
                caughtCancellation = true
            }
        }

        delay(500)
        job.cancel()
        job.join()

        assertTrue(caughtCancellation)
        assertTrue(source.isClosed)
    }

    @Test
    fun `candidate older than startedAtWallClockMillis is ignored`() = runTest {
        val source = FakeLocationUpdateSource()
        val engine = LocationAcquisitionEngine(source)
        val request = LocationEngineRequest(
            sessionId = "s", priority = 100, timeoutMillis = 10_000L, startedAtWallClockMillis = 100L
        )

        val candidates = mutableListOf<LocationSnapshot>()
        val job = launch {
            engine.acquire(request, onCandidate = { candidates += it }, onAvailabilityChanged = { })
        }

        source.emit(LocationUpdateEvent.Candidate(locationSnapshot(18f, 5.0, 50L)))
        source.emit(LocationUpdateEvent.Candidate(locationSnapshot(10f, 5.0, 150L)))
        source.complete()
        job.join()

        assertEquals(1, candidates.size)
        assertEquals(150L, candidates.single().timeMillis)
    }

    @Test
    fun `lower horizontal accuracy wins among finite values`() = runTest {
        val source = FakeLocationUpdateSource()
        val engine = LocationAcquisitionEngine(source)
        val request = LocationEngineRequest(
            sessionId = "s", priority = 100, timeoutMillis = 10_000L, startedAtWallClockMillis = 0L
        )

        val candidates = mutableListOf<LocationSnapshot>()
        val job = launch {
            engine.acquire(request, onCandidate = { candidates += it }, onAvailabilityChanged = { })
        }

        source.emit(LocationUpdateEvent.Candidate(locationSnapshot(30f, 5.0, 100L)))
        source.emit(LocationUpdateEvent.Candidate(locationSnapshot(10f, 5.0, 200L)))
        source.complete()
        job.join()

        assertEquals(2, candidates.size)
        assertEquals(10f, candidates.last().horizontalAccuracyMeters!!, 0.001f)
    }

    @Test
    fun `equal accuracy chooses newer timeMillis`() = runTest {
        val source = FakeLocationUpdateSource()
        val engine = LocationAcquisitionEngine(source)
        val request = LocationEngineRequest(
            sessionId = "s", priority = 100, timeoutMillis = 10_000L, startedAtWallClockMillis = 0L
        )

        val candidates = mutableListOf<LocationSnapshot>()
        val job = launch {
            engine.acquire(request, onCandidate = { candidates += it }, onAvailabilityChanged = { })
        }

        source.emit(LocationUpdateEvent.Candidate(locationSnapshot(10f, 5.0, 100L)))
        source.emit(LocationUpdateEvent.Candidate(locationSnapshot(10f, 5.0, 200L)))
        source.complete()
        job.join()

        assertEquals(2, candidates.size)
        assertEquals(200L, candidates.last().timeMillis)
    }

    @Test
    fun `null accuracy not considered valid`() = runTest {
        val source = FakeLocationUpdateSource()
        val engine = LocationAcquisitionEngine(source)
        val request = LocationEngineRequest(
            sessionId = "s", priority = 100, timeoutMillis = 10_000L, startedAtWallClockMillis = 0L
        )

        val candidates = mutableListOf<LocationSnapshot>()
        val job = launch {
            engine.acquire(request, onCandidate = { candidates += it }, onAvailabilityChanged = { })
        }

        source.emit(LocationUpdateEvent.Candidate(locationSnapshot(null, 5.0, 100L)))
        source.emit(LocationUpdateEvent.Candidate(locationSnapshot(10f, 5.0, 200L)))
        source.complete()
        job.join()

        assertEquals(2, candidates.size)
        assertEquals(200L, candidates.last().timeMillis)
    }

    @Test
    fun `nonFinite accuracy not considered valid`() = runTest {
        val source = FakeLocationUpdateSource()
        val engine = LocationAcquisitionEngine(source)
        val request = LocationEngineRequest(
            sessionId = "s", priority = 100, timeoutMillis = 10_000L, startedAtWallClockMillis = 0L
        )

        val candidates = mutableListOf<LocationSnapshot>()
        val job = launch {
            engine.acquire(request, onCandidate = { candidates += it }, onAvailabilityChanged = { })
        }

        source.emit(LocationUpdateEvent.Candidate(locationSnapshot(Float.NaN, 5.0, 100L)))
        source.emit(LocationUpdateEvent.Candidate(locationSnapshot(10f, 5.0, 200L)))
        source.complete()
        job.join()

        assertEquals(2, candidates.size)
        assertEquals(200L, candidates.last().timeMillis)
    }

    @Test
    fun `later nonFinite candidate cannot replace finite best`() = runTest {
        val source = FakeLocationUpdateSource()
        val engine = LocationAcquisitionEngine(source)
        val request = LocationEngineRequest(
            sessionId = "s", priority = 100, timeoutMillis = 10_000L, startedAtWallClockMillis = 0L
        )

        val candidates = mutableListOf<LocationSnapshot>()
        val job = launch {
            engine.acquire(request, onCandidate = { candidates += it }, onAvailabilityChanged = { })
        }

        source.emit(LocationUpdateEvent.Candidate(locationSnapshot(10f, 5.0, 100L)))
        source.emit(LocationUpdateEvent.Candidate(locationSnapshot(null, 5.0, 200L)))
        source.complete()
        job.join()

        assertEquals(1, candidates.size)
        assertEquals(100L, candidates.single().timeMillis)
    }

    @Test
    fun `late candidate after flow closed cannot mutate result`() = runTest {
        val source = FakeLocationUpdateSource()
        val engine = LocationAcquisitionEngine(source)
        val request = LocationEngineRequest(
            sessionId = "s", priority = 100, timeoutMillis = 10_000L, startedAtWallClockMillis = 0L
        )

        val candidates = mutableListOf<LocationSnapshot>()
        var result: LocationEngineResult? = null
        val job = launch {
            result = engine.acquire(request, onCandidate = { candidates += it }, onAvailabilityChanged = { })
        }

        source.emit(LocationUpdateEvent.Candidate(locationSnapshot(10f, 5.0, 100L)))
        source.complete()
        source.emit(LocationUpdateEvent.Candidate(locationSnapshot(5f, 5.0, 200L)))
        job.join()

        assertEquals(1, candidates.size)
        assertEquals(100L, result!!.bestLocation!!.timeMillis)
    }

    @Test
    fun `availability events invoke callback without ending round`() = runTest {
        val source = FakeLocationUpdateSource()
        val engine = LocationAcquisitionEngine(source)
        val request = LocationEngineRequest(
            sessionId = "s", priority = 100, timeoutMillis = 10_000L, startedAtWallClockMillis = 0L
        )

        val availabilities = mutableListOf<Boolean>()
        val candidates = mutableListOf<LocationSnapshot>()
        val job = launch {
            engine.acquire(request, onCandidate = { candidates += it }, onAvailabilityChanged = { availabilities += it })
        }

        source.emit(LocationUpdateEvent.Availability(true))
        source.emit(LocationUpdateEvent.Candidate(locationSnapshot(10f, 5.0, 100L)))
        source.emit(LocationUpdateEvent.Availability(false))
        source.complete()
        job.join()

        assertEquals(listOf(true, false), availabilities)
        assertEquals(1, candidates.size)
    }

    @Test
    fun `source exception returns Failed with reason`() = runTest {
        val source = FakeLocationUpdateSource()
        val engine = LocationAcquisitionEngine(source)
        val request = LocationEngineRequest(
            sessionId = "s", priority = 100, timeoutMillis = 10_000L, startedAtWallClockMillis = 0L
        )

        val candidates = mutableListOf<LocationSnapshot>()
        var result: LocationEngineResult? = null
        val job = launch {
            result = engine.acquire(request, onCandidate = { candidates += it }, onAvailabilityChanged = { })
        }

        source.emit(LocationUpdateEvent.Candidate(locationSnapshot(10f, 5.0, 100L)))
        source.emitError(RuntimeException("GPS failure"))
        job.join()

        assertTrue(result!!.completion is LocationEngineCompletion.Failed)
        assertEquals("GPS failure", (result!!.completion as LocationEngineCompletion.Failed).reason)
        assertEquals(100L, result!!.bestLocation!!.timeMillis)
        assertTrue(source.isClosed)
    }

    private fun locationSnapshot(
        horizontalAccuracyMeters: Float?,
        altitudeMeters: Double? = null,
        timeMillis: Long = 0L
    ) = LocationSnapshot(
        latitude = 31.230416,
        longitude = 121.473701,
        horizontalAccuracyMeters = horizontalAccuracyMeters,
        provider = "gps",
        source = "test",
        altitudeMeters = altitudeMeters,
        speedMetersPerSecond = null,
        bearingDegrees = null,
        timeMillis = timeMillis
    )

    private class FakeLocationUpdateSource : LocationUpdateSource {
        private val _events = Channel<LocationUpdateEvent>(Channel.UNLIMITED)
        @Volatile
        var isClosed = false

        override fun updates(request: LocationUpdateRequest): Flow<LocationUpdateEvent> = flow {
            try {
                for (event in _events) {
                    emit(event)
                }
            } finally {
                isClosed = true
            }
        }

        fun emit(event: LocationUpdateEvent) {
            _events.trySend(event)
        }

        fun complete() {
            _events.close()
        }

        fun emitError(cause: Throwable) {
            _events.close(cause)
        }
    }
}
