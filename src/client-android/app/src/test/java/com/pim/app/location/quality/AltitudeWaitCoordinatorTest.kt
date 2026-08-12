package com.pim.app.location.quality

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AltitudeWaitCoordinatorTest {
    @Test
    fun missingAltitudeDelaysUntilDeadlineThenAcceptsNullAltitude() = runBlocking {
        var now = 1_000L
        var delayedMillis = 0L
        val coordinator = AltitudeWaitCoordinator(
            gate = LocationQualityGate(maxAccuracyMetersExclusive = 50f, altitudeWaitTimeoutMillis = 15_000L),
            nowMillis = { now },
            delayMillis = { millis ->
                delayedMillis += millis
                now += millis
            }
        )
        val accepted = mutableListOf<QualityAcceptedLocation>()
        val dropped = mutableListOf<String>()

        coordinator.handleFix(
            fix(horizontalAccuracyMeters = 18f, altitudeMeters = null, recordedAtMillis = 1_000L),
            onAccepted = { accepted += it },
            onDropped = { _, reason -> dropped += reason }
        )

        assertEquals(15_000L, delayedMillis)
        assertTrue(dropped.isEmpty())
        assertEquals(1, accepted.size)
        assertNull(accepted.single().altitudeMeters)
        assertTrue(accepted.single().qualityFlags.contains("altitude-missing-timeout"))
    }

    @Test
    fun droppedFixDoesNotDelayOrAccept() = runBlocking {
        var delayedMillis = 0L
        val coordinator = AltitudeWaitCoordinator(
            gate = LocationQualityGate(maxAccuracyMetersExclusive = 50f, altitudeWaitTimeoutMillis = 15_000L),
            nowMillis = { 1_000L },
            delayMillis = { millis -> delayedMillis += millis }
        )
        val accepted = mutableListOf<QualityAcceptedLocation>()
        val dropped = mutableListOf<String>()

        coordinator.handleFix(
            fix(horizontalAccuracyMeters = 50f, altitudeMeters = null),
            onAccepted = { accepted += it },
            onDropped = { _, reason -> dropped += reason }
        )

        assertEquals(0L, delayedMillis)
        assertTrue(accepted.isEmpty())
        assertEquals(listOf("horizontal-accuracy-too-low"), dropped)
    }

    @Test
    fun laterAltitudeFixBeforeDeadlineCancelsNullAltitudeTimeout() = runBlocking {
        var now = 1_000L
        val accepted = mutableListOf<QualityAcceptedLocation>()
        val dropped = mutableListOf<String>()
        lateinit var coordinator: AltitudeWaitCoordinator
        coordinator = AltitudeWaitCoordinator(
            gate = LocationQualityGate(maxAccuracyMetersExclusive = 50f, altitudeWaitTimeoutMillis = 15_000L),
            nowMillis = { now },
            delayMillis = { millis ->
                now += millis / 2
                coordinator.handleFix(
                    fix(horizontalAccuracyMeters = 18f, altitudeMeters = 12.5, recordedAtMillis = now),
                    onAccepted = { accepted += it },
                    onDropped = { _, reason -> dropped += reason }
                )
                now += millis / 2
            }
        )

        coordinator.handleFix(
            fix(horizontalAccuracyMeters = 18f, altitudeMeters = null, recordedAtMillis = 1_000L),
            onAccepted = { accepted += it },
            onDropped = { _, reason -> dropped += reason }
        )

        assertTrue(dropped.isEmpty())
        assertEquals(1, accepted.size)
        assertEquals(12.5, accepted.single().altitudeMeters!!, 0.001)
        assertTrue(accepted.single().qualityFlags.isEmpty())
    }

    @Test
    fun explicitCancellationPreventsPendingTimeoutAcceptance() = runBlocking {
        var now = 1_000L
        val accepted = mutableListOf<QualityAcceptedLocation>()
        lateinit var coordinator: AltitudeWaitCoordinator
        coordinator = AltitudeWaitCoordinator(
            gate = LocationQualityGate(maxAccuracyMetersExclusive = 50f, altitudeWaitTimeoutMillis = 15_000L),
            nowMillis = { now },
            delayMillis = { millis ->
                coordinator.cancelPending()
                now += millis
            }
        )

        coordinator.handleFix(
            fix(horizontalAccuracyMeters = 18f, altitudeMeters = null, recordedAtMillis = 1_000L),
            onAccepted = { accepted += it },
            onDropped = { _, _ -> error("fix should not be dropped") }
        )

        assertTrue(accepted.isEmpty())
    }

    @Test
    fun `altitudeWaitNeverRunsPastSessionDeadline`() = runTest {
        var accepted: QualityAcceptedLocation? = null
        val coordinator = AltitudeWaitCoordinator(
            gate = LocationQualityGate(50f, 15_000L),
            nowMillis = { testScheduler.currentTime },
            delayMillis = { delay(it) }
        )
        coordinator.handleFix(
            fix(horizontalAccuracyMeters = 18f, recordedAtMillis = 0L, altitudeMeters = null),
            deadlineCapMillis = 5_000L,
            onAccepted = { accepted = it },
            onDropped = { _, _ -> error("unexpected drop") }
        )
        assertEquals(5_000L, testScheduler.currentTime)
        assertEquals(setOf("altitude-missing-timeout"), accepted?.qualityFlags)
    }

    @Test
    fun `clock rollback after delay does not cause recursive wait`() = runBlocking {
        var now = 1_000L
        var delayCount = 0
        val coordinator = AltitudeWaitCoordinator(
            gate = LocationQualityGate(50f, 15_000L),
            nowMillis = { now },
            delayMillis = { millis ->
                if (++delayCount == 1) {
                    assertEquals(4_000L, millis)
                    now += millis
                    now = 500L
                } else {
                    throw AssertionError("unexpected second delay")
                }
            }
        )
        val accepted = mutableListOf<QualityAcceptedLocation>()
        val dropped = mutableListOf<String>()

        coordinator.handleFix(
            fix(horizontalAccuracyMeters = 18f, altitudeMeters = null, recordedAtMillis = 1_000L),
            deadlineCapMillis = 5_000L,
            onAccepted = { accepted += it },
            onDropped = { _, reason -> dropped += reason }
        )

        assertEquals(1, delayCount)
        assertTrue(dropped.isEmpty())
        assertEquals(1, accepted.size)
        assertNull(accepted.single().altitudeMeters)
        assertTrue(accepted.single().qualityFlags.contains("altitude-missing-timeout"))
    }

    @Test
    fun `wall clock rollback wait is capped by monotonic session deadline`() = runTest {
        var accepted: QualityAcceptedLocation? = null
        var delayedMillis = 0L
        val coordinator = AltitudeWaitCoordinator(
            gate = LocationQualityGate(50f, 15_000L),
            nowMillis = { testScheduler.currentTime - 600_000L },
            nowElapsedRealtimeMillis = { testScheduler.currentTime },
            delayMillis = { millis ->
                delayedMillis += millis
                delay(millis)
            }
        )
        // Wall clock rolled back 600s while the GPS timestamp (recordedAtMillis)
        // stays on satellite time; the monotonic cap must bound the wait to the
        // session deadline instead of sleeping for ~620s.
        coordinator.handleFix(
            fix(horizontalAccuracyMeters = 18f, altitudeMeters = null, recordedAtMillis = 1_000L),
            deadlineCapMillis = 5_000L,
            deadlineCapElapsedRealtimeMillis = 5_000L,
            onAccepted = { accepted = it },
            onDropped = { _, _ -> error("unexpected drop") }
        )
        assertEquals(5_000L, delayedMillis)
        assertEquals(5_000L, testScheduler.currentTime)
        assertEquals(setOf("altitude-missing-timeout"), accepted?.qualityFlags)
    }

    private fun fix(
        horizontalAccuracyMeters: Float?,
        altitudeMeters: Double? = null,
        recordedAtMillis: Long = 1_000L
    ) = RawLocationFix(
        latitude = 31.230416,
        longitude = 121.473701,
        horizontalAccuracyMeters = horizontalAccuracyMeters,
        altitudeMeters = altitudeMeters,
        provider = "gps",
        recordedAtMillis = recordedAtMillis,
        policyMode = "PowerSavingNormal",
        scheduleLowFrequency = false,
        motionSignal = "Unknown"
    )
}
