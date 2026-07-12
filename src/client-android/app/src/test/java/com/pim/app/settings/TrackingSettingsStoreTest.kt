package com.pim.app.settings

import android.content.SharedPreferences
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
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
        assertEquals(false, defaults.syncOnUnmeteredOnly)
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
}

private class InMemorySharedPreferences : SharedPreferences {
    private val values = mutableMapOf<String, Any?>()

    override fun getAll(): MutableMap<String, *> = values.toMutableMap()
    override fun getString(key: String, defValue: String?): String? = values[key] as? String ?: defValue
    override fun getStringSet(key: String, defValues: MutableSet<String>?): MutableSet<String>? = defValues
    override fun getInt(key: String, defValue: Int): Int = values[key] as? Int ?: defValue
    override fun getLong(key: String, defValue: Long): Long = values[key] as? Long ?: defValue
    override fun getFloat(key: String, defValue: Float): Float = values[key] as? Float ?: defValue
    override fun getBoolean(key: String, defValue: Boolean): Boolean = values[key] as? Boolean ?: defValue
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
