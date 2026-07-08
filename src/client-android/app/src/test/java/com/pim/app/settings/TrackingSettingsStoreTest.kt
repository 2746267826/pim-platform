package com.pim.app.settings

import org.junit.Assert.assertEquals
import org.junit.Test

class TrackingSettingsStoreTest {
    @Test
    fun defaultProfileIsPowerSavingAndConfigurableValuesMatchSpec() {
        val defaults = TrackingSettings.defaults()

        assertEquals("power-saving", defaults.profile)
        assertEquals(false, defaults.continuousCollectionEnabled)
        assertEquals(3 * 60 * 1000L, defaults.normalIntervalMillis)
        assertEquals(15 * 60 * 1000L, defaults.scheduleLowFrequencyIntervalMillis)
        assertEquals(60 * 1000L, defaults.movementIntervalMillis)
        assertEquals(100.0, defaults.scheduleRecoveryThresholdMeters, 0.001)
        assertEquals(15 * 1000L, defaults.altitudeWaitTimeoutMillis)
        assertEquals(50f, defaults.maxUploadAccuracyMetersExclusive)
    }
}
