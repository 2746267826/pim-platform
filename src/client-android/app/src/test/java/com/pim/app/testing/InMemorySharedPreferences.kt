package com.pim.app.testing

import android.content.SharedPreferences

// SharedPreferences fake used by unit tests that construct
// TrackingSettingsStore without Robolectric. Covers the keys
// TrackingSettingsStore reads/writes; not a general-purpose
// SharedPreferences abstraction.
class InMemorySharedPreferences : SharedPreferences {
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
