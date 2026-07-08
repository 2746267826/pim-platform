package com.pim.app.location.quality

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class LocationQualityGateTest {
    private val gate = LocationQualityGate(
        maxAccuracyMetersExclusive = 50f,
        altitudeWaitTimeoutMillis = 15_000L
    )

    @Test
    fun missingHorizontalAccuracyIsDropped() {
        val result = gate.evaluate(fix(horizontalAccuracyMeters = null), nowMillis = 2_000L)

        assertTrue(result is QualityDecision.Drop)
        val dropped = result as QualityDecision.Drop
        assertEquals("missing-horizontal-accuracy", dropped.reason)
        assertEquals("PowerSavingNormal", dropped.fix.policyMode)
        assertEquals("Unknown", dropped.fix.motionSignal)
    }

    @Test
    fun accuracyBelowFiftyMetersIsAccepted() {
        val result = gate.evaluate(
            fix(horizontalAccuracyMeters = 49.9f, altitudeMeters = 12.0),
            nowMillis = 2_000L
        )

        assertTrue(result is QualityDecision.AcceptNow)
        val accepted = (result as QualityDecision.AcceptNow).accepted
        assertEquals(12.0, accepted.altitudeMeters!!, 0.001)
        assertEquals(2_000L, accepted.acceptedAtMillis)
        assertFalse(accepted.qualityFlags.contains("altitude-missing-timeout"))
        assertEquals("PowerSavingNormal", accepted.fix.policyMode)
    }

    @Test
    fun accuracyAtFiftyMetersIsDropped() {
        val result = gate.evaluate(fix(horizontalAccuracyMeters = 50.0f), nowMillis = 2_000L)

        assertTrue(result is QualityDecision.Drop)
        val dropped = result as QualityDecision.Drop
        assertEquals("horizontal-accuracy-too-low", dropped.reason)
        assertEquals(50.0f, dropped.fix.horizontalAccuracyMeters)
    }

    @Test
    fun nonFiniteAccuracyIsDropped() {
        val result = gate.evaluate(fix(horizontalAccuracyMeters = Float.NaN), nowMillis = 2_000L)

        assertTrue(result is QualityDecision.Drop)
        assertEquals("horizontal-accuracy-too-low", (result as QualityDecision.Drop).reason)
    }

    @Test
    fun missingAltitudeWaitsUntilDeadline() {
        val result = gate.evaluate(
            fix(horizontalAccuracyMeters = 18f, altitudeMeters = null, recordedAtMillis = 1_000L),
            nowMillis = 1_000L
        )

        assertTrue(result is QualityDecision.WaitForAltitude)
        val pending = (result as QualityDecision.WaitForAltitude).pending
        assertEquals(16_000L, pending.deadlineMillis)

        val stillWaiting = gate.timeoutDecision(pending, nowMillis = 15_999L)
        assertTrue(stillWaiting is QualityDecision.WaitForAltitude)
    }

    @Test
    fun missingAltitudeTimeoutAcceptsNullAltitudeWithQualityFlag() {
        val waiting = gate.evaluate(
            fix(horizontalAccuracyMeters = 18f, altitudeMeters = null, recordedAtMillis = 1_000L),
            nowMillis = 1_000L
        ) as QualityDecision.WaitForAltitude

        val result = gate.timeoutDecision(waiting.pending, nowMillis = 16_000L)

        assertTrue(result is QualityDecision.AcceptNow)
        val accepted = (result as QualityDecision.AcceptNow).accepted
        assertNull(accepted.altitudeMeters)
        assertEquals(16_000L, accepted.acceptedAtMillis)
        assertTrue(accepted.qualityFlags.contains("altitude-missing-timeout"))
        assertEquals("PowerSavingNormal", accepted.fix.policyMode)
        assertEquals("Unknown", accepted.fix.motionSignal)
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
