package com.pim.app.settings

import com.pim.app.location.policy.TrackingIntervalBounds
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TrackingSettingsValidatorTest {
    @Test
    fun defaultSettingsPassValidation() {
        val errors = TrackingSettingsValidator.validate(TrackingSettings.defaults())

        assertTrue("Default settings should pass validation: $errors", errors.isEmpty())
    }

    @Test
    fun normalIntervalBelowMinimumFails() {
        val settings = TrackingSettings.defaults().copy(
            normalIntervalMillis = TrackingIntervalBounds.NORMAL_MIN_MILLIS - 1
        )

        val errors = TrackingSettingsValidator.validate(settings)

        assertTrue(errors.any { it.code == "NORMAL_INTERVAL_OUT_OF_RANGE" })
    }

    @Test
    fun normalIntervalAboveMaximumFails() {
        val settings = TrackingSettings.defaults().copy(
            normalIntervalMillis = TrackingIntervalBounds.NORMAL_MAX_MILLIS + 1
        )

        val errors = TrackingSettingsValidator.validate(settings)

        assertTrue(errors.any { it.code == "NORMAL_INTERVAL_OUT_OF_RANGE" })
    }

    @Test
    fun scheduleIntervalBelowMinimumFails() {
        val settings = TrackingSettings.defaults().copy(
            scheduleLowFrequencyIntervalMillis = TrackingIntervalBounds.SCHEDULE_MIN_MILLIS - 1
        )

        val errors = TrackingSettingsValidator.validate(settings)

        assertTrue(errors.any { it.code == "SCHEDULE_INTERVAL_OUT_OF_RANGE" })
    }

    @Test
    fun scheduleIntervalAboveMaximumFails() {
        val settings = TrackingSettings.defaults().copy(
            scheduleLowFrequencyIntervalMillis = TrackingIntervalBounds.SCHEDULE_MAX_MILLIS + 1
        )

        val errors = TrackingSettingsValidator.validate(settings)

        assertTrue(errors.any { it.code == "SCHEDULE_INTERVAL_OUT_OF_RANGE" })
    }

    @Test
    fun movementIntervalBelowMinimumFails() {
        val settings = TrackingSettings.defaults().copy(
            movementIntervalMillis = TrackingIntervalBounds.MOVEMENT_MIN_MILLIS - 1
        )

        val errors = TrackingSettingsValidator.validate(settings)

        assertTrue(errors.any { it.code == "MOVEMENT_INTERVAL_OUT_OF_RANGE" })
    }

    @Test
    fun movementIntervalAboveMaximumFails() {
        val settings = TrackingSettings.defaults().copy(
            movementIntervalMillis = TrackingIntervalBounds.MOVEMENT_MAX_MILLIS + 1
        )

        val errors = TrackingSettingsValidator.validate(settings)

        assertTrue(errors.any { it.code == "MOVEMENT_INTERVAL_OUT_OF_RANGE" })
    }
}
