package com.pim.app.location

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LocationSubmissionPolicyTest {
    @Test
    fun manualSubmissionRejectsFiftyMeterAccuracy() {
        val decision = LocationSubmissionPolicy.decide(50f, autoAlreadySubmitted = false)

        assertFalse(decision.canSubmitManually)
        assertFalse(decision.shouldAutoSubmit)
    }

    @Test
    fun manualSubmissionAcceptsAccuracyBelowFiftyMeters() {
        val decision = LocationSubmissionPolicy.decide(49.9f, autoAlreadySubmitted = false)

        assertTrue(decision.canSubmitManually)
        assertFalse(decision.shouldAutoSubmit)
    }

    @Test
    fun maxUploadAccuracyThresholdReplacesHardcodedFiftyMeters() {
        val decision = LocationSubmissionPolicy.decide(
            horizontalAccuracyMeters = 45f,
            maxUploadAccuracyMetersExclusive = 35.5f,
            autoAlreadySubmitted = false
        )

        assertFalse(decision.canSubmitManually)
        assertTrue(decision.reason?.contains("35.5") == true)
    }

    @Test
    fun configuredThresholdIsAppliedBeforeAutomaticSubmissionThreshold() {
        val decision = LocationSubmissionPolicy.decide(
            horizontalAccuracyMeters = 10f,
            maxUploadAccuracyMetersExclusive = 10f,
            autoAlreadySubmitted = false
        )

        assertFalse(decision.canSubmitManually)
        assertFalse(decision.shouldAutoSubmit)
    }

    @Test
    fun nonFiniteAccuracyCannotBeSubmitted() {
        listOf(Float.NaN, Float.POSITIVE_INFINITY).forEach { accuracy ->
            val decision = LocationSubmissionPolicy.decide(
                horizontalAccuracyMeters = accuracy,
                autoAlreadySubmitted = false
            )

            assertFalse(decision.canSubmitManually)
            assertFalse(decision.shouldAutoSubmit)
        }
    }

    @Test
    fun `accurate manual location never auto-submits`() {
        // shouldAutoSubmit must always be false per the new coordinator design
        val decision = LocationSubmissionPolicy.decide(
            horizontalAccuracyMeters = 5f,
            autoAlreadySubmitted = false
        )

        assertTrue(decision.canSubmitManually)
        assertFalse("auto-submit must be disabled", decision.shouldAutoSubmit)
    }
}
