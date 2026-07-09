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
}
