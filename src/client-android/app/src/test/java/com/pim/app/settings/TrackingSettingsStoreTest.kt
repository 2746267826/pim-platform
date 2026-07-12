package com.pim.app.settings

import android.content.SharedPreferences
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class TrackingSettingsStoreTest {

    // ===== Existing tests (backward compatible) =====

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
        assertEquals(false, defaults.syncOnUnmeteredOnly)

        assertEquals(7, defaults.logRetentionDays)
        assertNull(defaults.verboseLoggingUntilUtcMillis)
    }

    @Test
    fun storePersistsCollectionAndPolicyValues() {
        val store = TrackingSettingsStore(InMemorySharedPreferences())

        store.write(
            TrackingSettings.defaults().copy(
                continuousCollectionEnabled = true,
                normalIntervalMillis = 120_000L,
                scheduleLowFrequencyIntervalMillis = 600_000L,
                movementIntervalMillis = 30_000L,
                scheduleRecoveryThresholdMeters = 150.0,
                altitudeWaitTimeoutMillis = 10_000L,
                maxUploadAccuracyMetersExclusive = 40f
            )
        )

        val stored = store.read()
        assertEquals(true, stored.continuousCollectionEnabled)
        assertEquals(120_000L, stored.normalIntervalMillis)
        assertEquals(600_000L, stored.scheduleLowFrequencyIntervalMillis)
        assertEquals(30_000L, stored.movementIntervalMillis)
        assertEquals(150.0, stored.scheduleRecoveryThresholdMeters, 0.001)
        assertEquals(10_000L, stored.altitudeWaitTimeoutMillis)
        assertEquals(40f, stored.maxUploadAccuracyMetersExclusive)
    }

    @Test
    fun setContinuousCollectionPreservesPolicyValues() {
        val store = TrackingSettingsStore(InMemorySharedPreferences())
        store.write(TrackingSettings.defaults().copy(normalIntervalMillis = 120_000L))

        val stored = store.setContinuousCollectionEnabled(true)

        assertEquals(true, stored.continuousCollectionEnabled)
        assertEquals(120_000L, stored.normalIntervalMillis)
    }

    @Test
    fun syncOnUnmeteredOnlyDefaultIsFalse() {
        val store = TrackingSettingsStore(InMemorySharedPreferences())
        val stored = store.read()
        assertEquals(false, stored.syncOnUnmeteredOnly)
    }

    @Test
    fun syncOnUnmeteredOnlyPersistsTrue() {
        val store = TrackingSettingsStore(InMemorySharedPreferences())
        store.write(TrackingSettings.defaults().copy(syncOnUnmeteredOnly = true))
        val stored = store.read()
        assertEquals(true, stored.syncOnUnmeteredOnly)
    }

    // ===== PresetCatalog tests =====

    @Test
    fun powerSavingPresetHasCorrectValues() {
        val preset = TrackingPresetCatalog.get("power-saving")
            ?: throw AssertionError("preset not found")
        assertEquals("power-saving", preset.id)
        assertEquals("省电", preset.displayName)
        assertEquals(180_000L, preset.normalIntervalMillis)
        assertEquals(900_000L, preset.scheduleLowFrequencyIntervalMillis)
        assertEquals(60_000L, preset.movementIntervalMillis)
        assertEquals(100.0, preset.scheduleRecoveryThresholdMeters, 0.001)
        assertEquals(50f, preset.maxUploadAccuracyMetersExclusive)
        assertEquals(15_000L, preset.altitudeWaitTimeoutMillis)
    }

    @Test
    fun standardPresetHasCorrectValues() {
        val preset = TrackingPresetCatalog.get("standard")
            ?: throw AssertionError("preset not found")
        assertEquals("standard", preset.id)
        assertEquals("标准", preset.displayName)
        assertEquals(120_000L, preset.normalIntervalMillis)
        assertEquals(600_000L, preset.scheduleLowFrequencyIntervalMillis)
        assertEquals(45_000L, preset.movementIntervalMillis)
        assertEquals(75.0, preset.scheduleRecoveryThresholdMeters, 0.001)
        assertEquals(35f, preset.maxUploadAccuracyMetersExclusive)
        assertEquals(20_000L, preset.altitudeWaitTimeoutMillis)
    }

    @Test
    fun highPrecisionPresetHasCorrectValues() {
        val preset = TrackingPresetCatalog.get("high-precision")
            ?: throw AssertionError("preset not found")
        assertEquals("high-precision", preset.id)
        assertEquals("高精度", preset.displayName)
        assertEquals(60_000L, preset.normalIntervalMillis)
        assertEquals(300_000L, preset.scheduleLowFrequencyIntervalMillis)
        assertEquals(30_000L, preset.movementIntervalMillis)
        assertEquals(50.0, preset.scheduleRecoveryThresholdMeters, 0.001)
        assertEquals(20f, preset.maxUploadAccuracyMetersExclusive)
        assertEquals(30_000L, preset.altitudeWaitTimeoutMillis)
    }

    @Test
    fun customPresetIdNotInCatalog() {
        assertNull(TrackingPresetCatalog.get("custom"))
    }

    @Test
    fun applyPresetOnlyReplacesProfileAndSixCollectionParams() {
        val current = TrackingSettings(
            profile = "custom",
            continuousCollectionEnabled = true,
            normalIntervalMillis = 99_999L,
            scheduleLowFrequencyIntervalMillis = 99_999L,
            movementIntervalMillis = 99_999L,
            scheduleRecoveryThresholdMeters = 999.0,
            altitudeWaitTimeoutMillis = 99_999L,
            maxUploadAccuracyMetersExclusive = 99f,
            syncOnUnmeteredOnly = true,
            logRetentionDays = 14,
            verboseLoggingUntilUtcMillis = 12_345L
        )
        val preset = TrackingPresetCatalog.get("standard")!!
        val result = preset.applyTo(current)

        assertEquals("standard", result.profile)
        assertEquals(120_000L, result.normalIntervalMillis)
        assertEquals(600_000L, result.scheduleLowFrequencyIntervalMillis)
        assertEquals(45_000L, result.movementIntervalMillis)
        assertEquals(75.0, result.scheduleRecoveryThresholdMeters, 0.001)
        assertEquals(35f, result.maxUploadAccuracyMetersExclusive)
        assertEquals(20_000L, result.altitudeWaitTimeoutMillis)

        assertEquals(true, result.continuousCollectionEnabled)
        assertEquals(true, result.syncOnUnmeteredOnly)
        assertEquals(14, result.logRetentionDays)
        assertEquals(12_345L, result.verboseLoggingUntilUtcMillis)
    }

    // ===== Validator tests =====

    @Test
    fun validDefaultSettingsPassValidation() {
        val errors = TrackingSettingsValidator.validate(TrackingSettings.defaults())
        assertTrue("Expected no errors but got: $errors", errors.isEmpty())
    }

    @Test
    fun normalIntervalBelowMinimumFails() {
        val errors = TrackingSettingsValidator.validate(
            TrackingSettings.defaults().copy(normalIntervalMillis = 59_999L)
        )
        assertTrue(errors.any { it.code == "NORMAL_INTERVAL_OUT_OF_RANGE" })
    }

    @Test
    fun normalIntervalBoundaryValuesAreValid() {
        assertTrue(TrackingSettingsValidator.validate(
            TrackingSettings.defaults().copy(normalIntervalMillis = 60_000L)
        ).none { it.code == "NORMAL_INTERVAL_OUT_OF_RANGE" })
        assertTrue(TrackingSettingsValidator.validate(
            TrackingSettings.defaults().copy(normalIntervalMillis = 900_000L)
        ).none { it.code == "NORMAL_INTERVAL_OUT_OF_RANGE" })
    }

    @Test
    fun normalIntervalAboveMaximumFails() {
        val errors = TrackingSettingsValidator.validate(
            TrackingSettings.defaults().copy(normalIntervalMillis = 900_001L)
        )
        assertTrue(errors.any { it.code == "NORMAL_INTERVAL_OUT_OF_RANGE" })
    }

    @Test
    fun scheduleIntervalBelowMinimumFails() {
        val errors = TrackingSettingsValidator.validate(
            TrackingSettings.defaults().copy(scheduleLowFrequencyIntervalMillis = 299_999L)
        )
        assertTrue(errors.any { it.code == "SCHEDULE_INTERVAL_OUT_OF_RANGE" })
    }

    @Test
    fun scheduleIntervalBoundaryValuesAreValid() {
        assertTrue(TrackingSettingsValidator.validate(
            TrackingSettings.defaults().copy(scheduleLowFrequencyIntervalMillis = 300_000L)
        ).none { it.code == "SCHEDULE_INTERVAL_OUT_OF_RANGE" })
        assertTrue(TrackingSettingsValidator.validate(
            TrackingSettings.defaults().copy(scheduleLowFrequencyIntervalMillis = 3_600_000L)
        ).none { it.code == "SCHEDULE_INTERVAL_OUT_OF_RANGE" })
    }

    @Test
    fun scheduleIntervalAboveMaximumFails() {
        val errors = TrackingSettingsValidator.validate(
            TrackingSettings.defaults().copy(scheduleLowFrequencyIntervalMillis = 3_600_001L)
        )
        assertTrue(errors.any { it.code == "SCHEDULE_INTERVAL_OUT_OF_RANGE" })
    }

    @Test
    fun movementIntervalBelowMinimumFails() {
        val errors = TrackingSettingsValidator.validate(
            TrackingSettings.defaults().copy(movementIntervalMillis = 29_999L)
        )
        assertTrue(errors.any { it.code == "MOVEMENT_INTERVAL_OUT_OF_RANGE" })
    }

    @Test
    fun movementIntervalBoundaryValuesAreValid() {
        assertTrue(TrackingSettingsValidator.validate(
            TrackingSettings.defaults().copy(movementIntervalMillis = 30_000L)
        ).none { it.code == "MOVEMENT_INTERVAL_OUT_OF_RANGE" })
        assertTrue(TrackingSettingsValidator.validate(
            TrackingSettings.defaults().copy(movementIntervalMillis = 300_000L)
        ).none { it.code == "MOVEMENT_INTERVAL_OUT_OF_RANGE" })
    }

    @Test
    fun movementIntervalAboveMaximumFails() {
        val errors = TrackingSettingsValidator.validate(
            TrackingSettings.defaults().copy(movementIntervalMillis = 300_001L)
        )
        assertTrue(errors.any { it.code == "MOVEMENT_INTERVAL_OUT_OF_RANGE" })
    }

    @Test
    fun recoveryThresholdBelowMinimumFails() {
        val errors = TrackingSettingsValidator.validate(
            TrackingSettings.defaults().copy(scheduleRecoveryThresholdMeters = 24.9)
        )
        assertTrue(errors.any { it.code == "RECOVERY_THRESHOLD_OUT_OF_RANGE" })
    }

    @Test
    fun recoveryThresholdBoundaryValuesAreValid() {
        assertTrue(TrackingSettingsValidator.validate(
            TrackingSettings.defaults().copy(scheduleRecoveryThresholdMeters = 25.0)
        ).none { it.code == "RECOVERY_THRESHOLD_OUT_OF_RANGE" })
        assertTrue(TrackingSettingsValidator.validate(
            TrackingSettings.defaults().copy(scheduleRecoveryThresholdMeters = 500.0)
        ).none { it.code == "RECOVERY_THRESHOLD_OUT_OF_RANGE" })
    }

    @Test
    fun recoveryThresholdAboveMaximumFails() {
        val errors = TrackingSettingsValidator.validate(
            TrackingSettings.defaults().copy(scheduleRecoveryThresholdMeters = 500.1)
        )
        assertTrue(errors.any { it.code == "RECOVERY_THRESHOLD_OUT_OF_RANGE" })
    }

    @Test
    fun nanRecoveryThresholdProducesOutOfRange() {
        val errors = TrackingSettingsValidator.validate(
            TrackingSettings.defaults().copy(scheduleRecoveryThresholdMeters = Double.NaN)
        )
        assertTrue(errors.any { it.code == "RECOVERY_THRESHOLD_OUT_OF_RANGE" })
    }

    @Test
    fun nanAccuracyProducesOutOfRange() {
        val errors = TrackingSettingsValidator.validate(
            TrackingSettings.defaults().copy(maxUploadAccuracyMetersExclusive = Float.NaN)
        )
        assertTrue(errors.any { it.code == "ACCURACY_OUT_OF_RANGE" })
    }

    @Test
    fun accuracyBelowMinimumFails() {
        val errors = TrackingSettingsValidator.validate(
            TrackingSettings.defaults().copy(maxUploadAccuracyMetersExclusive = 9f)
        )
        assertTrue(errors.any { it.code == "ACCURACY_OUT_OF_RANGE" })
    }

    @Test
    fun accuracyBoundaryValuesAreValid() {
        assertTrue(TrackingSettingsValidator.validate(
            TrackingSettings.defaults().copy(maxUploadAccuracyMetersExclusive = 10f)
        ).none { it.code == "ACCURACY_OUT_OF_RANGE" })
        assertTrue(TrackingSettingsValidator.validate(
            TrackingSettings.defaults().copy(maxUploadAccuracyMetersExclusive = 50f)
        ).none { it.code == "ACCURACY_OUT_OF_RANGE" })
    }

    @Test
    fun accuracyAboveMaximumFails() {
        val errors = TrackingSettingsValidator.validate(
            TrackingSettings.defaults().copy(maxUploadAccuracyMetersExclusive = 51f)
        )
        assertTrue(errors.any { it.code == "ACCURACY_OUT_OF_RANGE" })
    }

    @Test
    fun altitudeWaitBelowMinimumFails() {
        val errors = TrackingSettingsValidator.validate(
            TrackingSettings.defaults().copy(altitudeWaitTimeoutMillis = -1L)
        )
        assertTrue(errors.any { it.code == "ALTITUDE_WAIT_OUT_OF_RANGE" })
    }

    @Test
    fun altitudeWaitBoundaryValuesAreValid() {
        assertTrue(TrackingSettingsValidator.validate(
            TrackingSettings.defaults().copy(altitudeWaitTimeoutMillis = 0L)
        ).none { it.code == "ALTITUDE_WAIT_OUT_OF_RANGE" })
        assertTrue(TrackingSettingsValidator.validate(
            TrackingSettings.defaults().copy(altitudeWaitTimeoutMillis = 30_000L)
        ).none { it.code == "ALTITUDE_WAIT_OUT_OF_RANGE" })
    }

    @Test
    fun altitudeWaitAboveMaximumFails() {
        val errors = TrackingSettingsValidator.validate(
            TrackingSettings.defaults().copy(altitudeWaitTimeoutMillis = 30_001L)
        )
        assertTrue(errors.any { it.code == "ALTITUDE_WAIT_OUT_OF_RANGE" })
    }

    @Test
    fun logRetentionOnlyAccepts1_7_14_30() {
        for (valid in listOf(1, 7, 14, 30)) {
            val s = TrackingSettings.defaults().copy(logRetentionDays = valid)
            val errors = TrackingSettingsValidator.validate(s)
            assertFalse("Expected $valid to be valid", errors.any { it.code == "LOG_RETENTION_INVALID" })
        }
        for (invalid in listOf(0, 2, 3, 5, 6, 8, 10, 15, 20, 31)) {
            val s = TrackingSettings.defaults().copy(logRetentionDays = invalid)
            val errors = TrackingSettingsValidator.validate(s)
            assertTrue("Expected $invalid to be invalid", errors.any { it.code == "LOG_RETENTION_INVALID" })
        }
    }

    @Test
    fun multipleValidationErrorsReportedTogether() {
        val settings = TrackingSettings.defaults().copy(
            normalIntervalMillis = 59_999L,
            scheduleLowFrequencyIntervalMillis = 299_999L,
            movementIntervalMillis = 29_999L,
            scheduleRecoveryThresholdMeters = 24.0,
            maxUploadAccuracyMetersExclusive = 9f,
            altitudeWaitTimeoutMillis = -1L,
            logRetentionDays = 3
        )
        val errors = TrackingSettingsValidator.validate(settings)
        val codes = errors.map { it.code }.toSet()
        assertTrue(codes.contains("NORMAL_INTERVAL_OUT_OF_RANGE"))
        assertTrue(codes.contains("SCHEDULE_INTERVAL_OUT_OF_RANGE"))
        assertTrue(codes.contains("MOVEMENT_INTERVAL_OUT_OF_RANGE"))
        assertTrue(codes.contains("RECOVERY_THRESHOLD_OUT_OF_RANGE"))
        assertTrue(codes.contains("ACCURACY_OUT_OF_RANGE"))
        assertTrue(codes.contains("ALTITUDE_WAIT_OUT_OF_RANGE"))
        assertTrue(codes.contains("LOG_RETENTION_INVALID"))
    }

    // ===== Extended store tests =====

    @Test
    fun newFieldsHaveCorrectDefaults() {
        val defaults = TrackingSettings.defaults()
        assertEquals(7, defaults.logRetentionDays)
        assertNull(defaults.verboseLoggingUntilUtcMillis)
    }

    @Test
    fun newFieldsPersist() {
        val store = TrackingSettingsStore(InMemorySharedPreferences())
        store.write(TrackingSettings.defaults().copy(
            logRetentionDays = 14,
            verboseLoggingUntilUtcMillis = 1000L
        ))
        val stored = store.read()
        assertEquals(14, stored.logRetentionDays)
        assertEquals(1000L, stored.verboseLoggingUntilUtcMillis)
    }

    @Test
    fun applyPresetReplacesSettings() {
        val store = TrackingSettingsStore(InMemorySharedPreferences())
        store.write(TrackingSettings.defaults().copy(
            continuousCollectionEnabled = true,
            normalIntervalMillis = 99_999L,
            syncOnUnmeteredOnly = true,
            logRetentionDays = 30
        ))

        store.applyPreset("standard")
        val stored = store.read()

        assertEquals("standard", stored.profile)
        assertEquals(120_000L, stored.normalIntervalMillis)
        assertEquals(600_000L, stored.scheduleLowFrequencyIntervalMillis)
        assertEquals(45_000L, stored.movementIntervalMillis)
        assertEquals(75.0, stored.scheduleRecoveryThresholdMeters, 0.001)
        assertEquals(35f, stored.maxUploadAccuracyMetersExclusive)
        assertEquals(20_000L, stored.altitudeWaitTimeoutMillis)

        assertEquals(true, stored.continuousCollectionEnabled)
        assertEquals(true, stored.syncOnUnmeteredOnly)
        assertEquals(30, stored.logRetentionDays)
        assertNull(stored.verboseLoggingUntilUtcMillis)
    }

    @Test
    fun applyPresetPreservesNonCollectionFields() {
        val store = TrackingSettingsStore(InMemorySharedPreferences())
        store.write(TrackingSettings.defaults().copy(
            continuousCollectionEnabled = true,
            syncOnUnmeteredOnly = true,
            logRetentionDays = 30,
            verboseLoggingUntilUtcMillis = 12_345L
        ))

        store.applyPreset("standard")
        val stored = store.read()

        assertEquals(true, stored.continuousCollectionEnabled)
        assertEquals(true, stored.syncOnUnmeteredOnly)
        assertEquals(30, stored.logRetentionDays)
        assertEquals(12_345L, stored.verboseLoggingUntilUtcMillis)
    }

    @Test(expected = IllegalArgumentException::class)
    fun applyPresetWithUnknownIdThrows() {
        val store = TrackingSettingsStore(InMemorySharedPreferences())
        store.applyPreset("nonexistent")
    }

    @Test
    fun setVerboseLoggingEnabledSetsExactly24Hours() {
        val store = TrackingSettingsStore(InMemorySharedPreferences())
        store.setVerboseLoggingEnabled(true, 1000L)

        val stored = store.read()
        assertEquals(1000L + 24 * 60 * 60 * 1000L, stored.verboseLoggingUntilUtcMillis)
    }

    @Test
    fun setVerboseLoggingEnabledDisabledClearsTimestamp() {
        val store = TrackingSettingsStore(InMemorySharedPreferences())
        store.setVerboseLoggingEnabled(true, 1000L)
        store.setVerboseLoggingEnabled(false, 2000L)

        assertNull(store.read().verboseLoggingUntilUtcMillis)
    }

    @Test
    fun isVerboseLoggingEnabledReturnsTrueWithin24h() {
        val store = TrackingSettingsStore(InMemorySharedPreferences())
        store.setVerboseLoggingEnabled(true, 1000L)

        assertTrue(store.isVerboseLoggingEnabled(5000L))
        assertTrue(store.isVerboseLoggingEnabled(1000L))
        assertTrue(store.isVerboseLoggingEnabled(1000L + 24 * 60 * 60 * 1000L - 1))
    }

    @Test
    fun isVerboseLoggingEnabledReturnsFalseAfterExpiry() {
        val store = TrackingSettingsStore(InMemorySharedPreferences())
        store.setVerboseLoggingEnabled(true, 1000L)

        assertFalse(store.isVerboseLoggingEnabled(1000L + 24 * 60 * 60 * 1000L))
        assertFalse(store.isVerboseLoggingEnabled(1000L + 24 * 60 * 60 * 1000L + 1))
    }

    @Test
    fun isVerboseLoggingEnabledCleansUpExpired() {
        val store = TrackingSettingsStore(InMemorySharedPreferences())
        store.setVerboseLoggingEnabled(true, 1000L)

        store.isVerboseLoggingEnabled(1000L + 24 * 60 * 60 * 1000L + 1)

        assertNull(store.read().verboseLoggingUntilUtcMillis)
    }

    @Test
    fun isVerboseLoggingEnabledReturnsFalseWhenNeverSet() {
        val store = TrackingSettingsStore(InMemorySharedPreferences())
        assertFalse(store.isVerboseLoggingEnabled(0L))
    }

    @Test
    fun resetOperationalDefaultsResetsAllStoreFields() {
        val store = TrackingSettingsStore(InMemorySharedPreferences())
        store.write(TrackingSettings.defaults().copy(
            profile = "custom",
            continuousCollectionEnabled = true,
            normalIntervalMillis = 120_000L,
            scheduleLowFrequencyIntervalMillis = 600_000L,
            movementIntervalMillis = 45_000L,
            scheduleRecoveryThresholdMeters = 75.0,
            altitudeWaitTimeoutMillis = 20_000L,
            maxUploadAccuracyMetersExclusive = 35f,
            syncOnUnmeteredOnly = true,
            logRetentionDays = 30,
            verboseLoggingUntilUtcMillis = 1000L
        ))

        store.resetOperationalDefaults()
        val stored = store.read()

        assertEquals("power-saving", stored.profile)
        assertEquals(false, stored.continuousCollectionEnabled)
        assertEquals(180_000L, stored.normalIntervalMillis)
        assertEquals(900_000L, stored.scheduleLowFrequencyIntervalMillis)
        assertEquals(60_000L, stored.movementIntervalMillis)
        assertEquals(100.0, stored.scheduleRecoveryThresholdMeters, 0.001)
        assertEquals(15_000L, stored.altitudeWaitTimeoutMillis)
        assertEquals(50f, stored.maxUploadAccuracyMetersExclusive)
        assertEquals(false, stored.syncOnUnmeteredOnly)
        assertEquals(7, stored.logRetentionDays)
        assertNull(stored.verboseLoggingUntilUtcMillis)
    }

    @Test
    fun invalidWriteIsRejectedAndOldValuesPreserved() {
        val store = TrackingSettingsStore(InMemorySharedPreferences())
        val original = store.read()

        try {
            store.write(original.copy(normalIntervalMillis = 1L))
            fail("Expected exception on invalid write")
        } catch (e: IllegalArgumentException) {
            // expected
        }

        val after = store.read()
        assertEquals(original.normalIntervalMillis, after.normalIntervalMillis)
    }
}

private class InMemorySharedPreferences : SharedPreferences {
    private val values = mutableMapOf<String, Any?>()

    override fun getAll(): MutableMap<String, *> = values.toMutableMap()
    override fun getString(key: String, defValue: String?): String? = (values[key] as? String) ?: defValue
    override fun getStringSet(key: String, defValues: MutableSet<String>?): MutableSet<String>? = defValues
    override fun getInt(key: String, defValue: Int): Int = (values[key] as? Int) ?: defValue
    override fun getLong(key: String, defValue: Long): Long = (values[key] as? Long) ?: defValue
    override fun getFloat(key: String, defValue: Float): Float = (values[key] as? Float) ?: defValue
    override fun getBoolean(key: String, defValue: Boolean): Boolean = (values[key] as? Boolean) ?: defValue
    override fun contains(key: String): Boolean = values.containsKey(key)
    override fun edit(): SharedPreferences.Editor = Editor()
    override fun registerOnSharedPreferenceChangeListener(listener: SharedPreferences.OnSharedPreferenceChangeListener?) = Unit
    override fun unregisterOnSharedPreferenceChangeListener(listener: SharedPreferences.OnSharedPreferenceChangeListener?) = Unit

    private inner class Editor : SharedPreferences.Editor {
        private val edits = mutableMapOf<String, Any?>()
        private val removals = mutableSetOf<String>()
        private var clearAll = false

        override fun putString(key: String, value: String?): SharedPreferences.Editor = apply { edits[key] = value }
        override fun putStringSet(key: String, values: MutableSet<String>?): SharedPreferences.Editor = apply {
            edits[key] = values
        }
        override fun putInt(key: String, value: Int): SharedPreferences.Editor = apply { edits[key] = value }
        override fun putLong(key: String, value: Long): SharedPreferences.Editor = apply { edits[key] = value }
        override fun putFloat(key: String, value: Float): SharedPreferences.Editor = apply { edits[key] = value }
        override fun putBoolean(key: String, value: Boolean): SharedPreferences.Editor = apply { edits[key] = value }
        override fun remove(key: String): SharedPreferences.Editor = apply { removals += key }
        override fun clear(): SharedPreferences.Editor = apply { clearAll = true }
        override fun commit(): Boolean {
            apply()
            return true
        }
        override fun apply() {
            if (clearAll) values.clear()
            removals.forEach(values::remove)
            values.putAll(edits)
        }
    }
}
