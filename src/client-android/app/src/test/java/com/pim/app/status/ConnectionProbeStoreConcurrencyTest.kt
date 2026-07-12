package com.pim.app.status

import android.content.SharedPreferences
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

class ConnectionProbeStoreConcurrencyTest {
    @Test
    fun saveAndClearAreSerializedWithPersistenceAndStateFlow() {
        val preferences = CoordinatedSharedPreferences()
        val json = Json { ignoreUnknownKeys = true }
        val store = ConnectionProbeStore(preferences, json)
        val result = ConnectionProbeResult(
            outcome = ConnectionProbeOutcome.Reachable,
            checkedAtUtcMillis = 1_000L,
            serverIdentity = "https://pim.example/api/v1/",
            lastCompletedStage = ConnectionProbeStage.EmbedBootstrap,
            latencyMillisByStage = emptyMap(),
            capabilities = ServerCapabilities(true, true)
        )
        val executor = Executors.newFixedThreadPool(2)

        try {
            val save = executor.submit<Boolean> { store.save(result) }
            assertTrue(preferences.firstCommitPersisted.await(5, TimeUnit.SECONDS))
            val clear = executor.submit<Boolean> { store.clear() }

            val overlapped = preferences.secondCommitPersisted.await(250, TimeUnit.MILLISECONDS)
            preferences.releaseFirstCommit.countDown()

            assertFalse("save and clear must not overlap persistence", overlapped)
            assertTrue(save.get(5, TimeUnit.SECONDS))
            assertTrue(clear.get(5, TimeUnit.SECONDS))
            assertNull(store.result.value)
            assertNull(ConnectionProbeStore(preferences, json).result.value)
        } finally {
            preferences.releaseFirstCommit.countDown()
            executor.shutdownNow()
        }
    }
}

private class CoordinatedSharedPreferences : SharedPreferences {
    private val values = linkedMapOf<String, Any?>()
    private val commitCount = AtomicInteger(0)
    val firstCommitPersisted = CountDownLatch(1)
    val secondCommitPersisted = CountDownLatch(1)
    val releaseFirstCommit = CountDownLatch(1)

    override fun getAll(): MutableMap<String, *> = synchronized(values) { values.toMutableMap() }
    override fun getString(key: String, defValue: String?): String? = synchronized(values) {
        values[key] as? String ?: defValue
    }
    override fun getStringSet(key: String, defValues: MutableSet<String>?): MutableSet<String>? = defValues
    override fun getInt(key: String, defValue: Int): Int = defValue
    override fun getLong(key: String, defValue: Long): Long = defValue
    override fun getFloat(key: String, defValue: Float): Float = defValue
    override fun getBoolean(key: String, defValue: Boolean): Boolean = defValue
    override fun contains(key: String): Boolean = synchronized(values) { values.containsKey(key) }
    override fun edit(): SharedPreferences.Editor = Editor()
    override fun registerOnSharedPreferenceChangeListener(listener: SharedPreferences.OnSharedPreferenceChangeListener?) = Unit
    override fun unregisterOnSharedPreferenceChangeListener(listener: SharedPreferences.OnSharedPreferenceChangeListener?) = Unit

    private inner class Editor : SharedPreferences.Editor {
        private val edits = linkedMapOf<String, Any?>()
        private var clearAll = false

        override fun putString(key: String, value: String?): SharedPreferences.Editor = apply { edits[key] = value }
        override fun putStringSet(key: String, values: MutableSet<String>?): SharedPreferences.Editor = this
        override fun putInt(key: String, value: Int): SharedPreferences.Editor = this
        override fun putLong(key: String, value: Long): SharedPreferences.Editor = this
        override fun putFloat(key: String, value: Float): SharedPreferences.Editor = this
        override fun putBoolean(key: String, value: Boolean): SharedPreferences.Editor = this
        override fun remove(key: String): SharedPreferences.Editor = apply { edits[key] = null }
        override fun clear(): SharedPreferences.Editor = apply { clearAll = true }

        override fun commit(): Boolean {
            synchronized(values) {
                if (clearAll) values.clear()
                edits.forEach { (key, value) ->
                    if (value == null) values.remove(key) else values[key] = value
                }
            }
            when (commitCount.incrementAndGet()) {
                1 -> {
                    firstCommitPersisted.countDown()
                    check(releaseFirstCommit.await(5, TimeUnit.SECONDS))
                }
                2 -> secondCommitPersisted.countDown()
            }
            return true
        }

        override fun apply() {
            commit()
        }
    }
}
