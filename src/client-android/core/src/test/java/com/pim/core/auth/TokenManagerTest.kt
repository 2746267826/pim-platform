package com.pim.core.auth

import android.content.SharedPreferences
import com.pim.core.auth.AuthRefreshOperation
import com.pim.core.auth.AuthRefreshResult
import com.pim.core.auth.AuthTokens
import com.pim.core.network.AuthRefreshCoordinator
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

class TokenManagerTest {
    @Test
    fun invalidLoginTokensNeverOverwriteCurrentSession() {
        val manager = manager(nowMillis = 1_000L)
        assertTrue(manager.saveTokens("access-a", "refresh-a", "1970-01-01T00:00:03Z", TEST_SERVER_URL))
        val original = manager.snapshot()

        val invalidSaves = listOf(
            manager.saveTokens("", "refresh-b", "1970-01-01T00:00:04Z", TEST_SERVER_URL),
            manager.saveTokens("access-b", "", "1970-01-01T00:00:04Z", TEST_SERVER_URL),
            manager.saveTokens("access-b", "refresh-b", "not-an-instant", TEST_SERVER_URL),
            manager.saveTokens("access-b", "refresh-b", "1970-01-01T00:00:01Z", TEST_SERVER_URL)
        )

        assertEquals(listOf(false, false, false, false), invalidSaves)
        assertEquals(original, manager.snapshot())
    }

    @Test
    fun boundSessionCanOnlyBeReadForItsTrustedOrigin() {
        val manager = manager(nowMillis = 1_000L)

        assertTrue(
            manager.saveTokens(
                accessToken = "access-a",
                refreshToken = "refresh-a",
                expiresAt = "1970-01-01T00:00:03Z",
                serverUrl = "https://server-a.example/api/v1/"
            )
        )

        assertEquals("https://server-a.example", manager.snapshot().serverIdentity)
        assertEquals(
            "access-a",
            manager.getAccessTokenForServer("https://server-a.example/api/v1/")
        )
        assertNull(manager.getAccessTokenForServer("https://server-b.example/api/v1/"))
    }

    @Test
    fun legacyEncryptedSessionWithoutServerIdentityFailsClosed() {
        val preferences = InMemorySharedPreferences().apply {
            edit()
                .putString("access_token", "legacy-access")
                .putString("refresh_token", "legacy-refresh")
                .putLong("expires_at", Long.MAX_VALUE)
                .commit()
        }

        val manager = TokenManager(
            FakeSecurePreferencesFactory(preferences),
            nowMillis = { 1_000L }
        )

        assertNull(manager.accessToken())
        assertNull(manager.refreshToken())
    }

    @Test
    fun inFlightRefreshCannotRestoreSessionAfterLogout() {
        val manager = manager(nowMillis = 1_000L)
        assertTrue(manager.saveTokens("access-old", "refresh-old", "1970-01-01T00:00:03Z", TEST_SERVER_URL))
        val refreshEntered = CountDownLatch(1)
        val releaseRefresh = CountDownLatch(1)
        val coordinator = AuthRefreshCoordinator(
            manager,
            AuthRefreshOperation { _, _ ->
                refreshEntered.countDown()
                check(releaseRefresh.await(5, TimeUnit.SECONDS))
                AuthRefreshResult.Success(AuthTokens("access-refresh", "refresh-next", 4_000L))
            },
            nowMillis = { 1_000L }
        )
        val executor = Executors.newSingleThreadExecutor()

        try {
            val result = executor.submit<Boolean> {
                runBlocking { coordinator.refreshAfterUnauthorized("access-old") }
            }
            assertTrue(refreshEntered.await(5, TimeUnit.SECONDS))
            manager.clear()
            val logoutSnapshot = manager.snapshot()
            releaseRefresh.countDown()

            assertFalse(result.get(5, TimeUnit.SECONDS))
            assertEquals(logoutSnapshot, manager.snapshot())
            assertNull(manager.accessToken())
        } finally {
            releaseRefresh.countDown()
            executor.shutdownNow()
        }
    }

    @Test
    fun inFlightRefreshCannotOverwriteNewLoginSession() {
        val manager = manager(nowMillis = 1_000L)
        assertTrue(manager.saveTokens("access-old", "refresh-old", "1970-01-01T00:00:03Z", TEST_SERVER_URL))
        val refreshEntered = CountDownLatch(1)
        val releaseRefresh = CountDownLatch(1)
        val coordinator = AuthRefreshCoordinator(
            manager,
            AuthRefreshOperation { _, _ ->
                refreshEntered.countDown()
                check(releaseRefresh.await(5, TimeUnit.SECONDS))
                AuthRefreshResult.Success(AuthTokens("access-refresh", "refresh-next", 4_000L))
            },
            nowMillis = { 1_000L }
        )
        val executor = Executors.newSingleThreadExecutor()

        try {
            val result = executor.submit<Boolean> {
                runBlocking { coordinator.refreshAfterUnauthorized("access-old") }
            }
            assertTrue(refreshEntered.await(5, TimeUnit.SECONDS))
            assertTrue(
                manager.saveTokens(
                    "access-new-login",
                    "refresh-new-login",
                    "1970-01-01T00:00:05Z",
                    TEST_SERVER_URL
                )
            )
            val loginSnapshot = manager.snapshot()
            releaseRefresh.countDown()

            assertTrue(result.get(5, TimeUnit.SECONDS))
            assertEquals(loginSnapshot, manager.snapshot())
            assertEquals("access-new-login", manager.accessToken())
        } finally {
            releaseRefresh.countDown()
            executor.shutdownNow()
        }
    }

    @Test
    fun rejectedOldRefreshCannotClearNewLoginSession() {
        val manager = manager(nowMillis = 1_000L)
        assertTrue(manager.saveTokens("access-old", "refresh-old", "1970-01-01T00:00:03Z", TEST_SERVER_URL))
        val refreshEntered = CountDownLatch(1)
        val releaseRefresh = CountDownLatch(1)
        val coordinator = AuthRefreshCoordinator(
            manager,
            AuthRefreshOperation { _, _ ->
                refreshEntered.countDown()
                check(releaseRefresh.await(5, TimeUnit.SECONDS))
                AuthRefreshResult.Rejected
            },
            nowMillis = { 1_000L }
        )
        val executor = Executors.newSingleThreadExecutor()

        try {
            val result = executor.submit<Boolean> {
                runBlocking { coordinator.refreshAfterUnauthorized("access-old") }
            }
            assertTrue(refreshEntered.await(5, TimeUnit.SECONDS))
            assertTrue(
                manager.saveTokens(
                    "access-new-login",
                    "refresh-new-login",
                    "1970-01-01T00:00:05Z",
                    TEST_SERVER_URL
                )
            )
            val loginSnapshot = manager.snapshot()
            releaseRefresh.countDown()

            assertTrue(result.get(5, TimeUnit.SECONDS))
            assertEquals(loginSnapshot, manager.snapshot())
        } finally {
            releaseRefresh.countDown()
            executor.shutdownNow()
        }
    }

    @Test
    fun concurrentRefreshIfExpiredSecondReturnsFalseWhenFirstRejectedAndCleared() {
        val manager = manager(nowMillis = 1_000L)
        assertTrue(manager.save("access-old", "refresh-old", 3_000L, "https://pim.example"))

        val refreshEntered = CountDownLatch(1)
        val releaseFirst = CountDownLatch(1)
        val refreshCalls = AtomicInteger(0)
        val coordinator = AuthRefreshCoordinator(
            manager,
            AuthRefreshOperation { _, _ ->
                refreshEntered.countDown()
                refreshCalls.incrementAndGet()
                check(releaseFirst.await(5, TimeUnit.SECONDS))
                AuthRefreshResult.Rejected
            },
            nowMillis = { 5_000L }
        )
        val executor = Executors.newFixedThreadPool(2)

        try {
            val firstResult = executor.submit<Boolean> {
                runBlocking { coordinator.refreshIfExpired() }
            }
            assertTrue(refreshEntered.await(5, TimeUnit.SECONDS))

            val secondResult = executor.submit<Boolean> {
                runBlocking { coordinator.refreshIfExpired() }
            }
            Thread.sleep(300)

            releaseFirst.countDown()

            assertFalse(firstResult.get(5, TimeUnit.SECONDS))
            assertFalse(secondResult.get(5, TimeUnit.SECONDS))

            assertEquals(1, refreshCalls.get())
            assertNull(manager.accessToken())
        } finally {
            releaseFirst.countDown()
            executor.shutdownNow()
        }
    }

    @Test
    fun secureStorageOpenFailureFailsClosedWithoutTokens() {
        val errors = mutableListOf<String>()
        val factory = object : SecurePreferencesFactory {
            override fun open(): SharedPreferences {
                throw SecureStorageUnavailableException("key not available")
            }
        }

        val manager = TokenManager(factory, nowMillis = { 1_000L }) { msg, _ -> errors += msg }

        assertNull(manager.accessToken())
        assertNull(manager.refreshToken())
        assertNull(manager.snapshot().tokens)
        assertTrue(errors.any { it.contains("initialization failed") })
    }

    @Test
    fun saveFailureAfterSuccessfulInitClearsInMemoryState() {
        val factory = object : SecurePreferencesFactory {
            override fun open(): SharedPreferences = TrivialFailingSharedPreferences()
        }
        val manager = TokenManager(factory, nowMillis = { 1_000L })

        assertFalse(manager.save("access-a", "refresh-a", 3_000L, "https://pim.example"))
        assertNull(manager.snapshot().tokens)
    }

    @Test
    fun clearFailureClearsInMemoryState() {
        val delegate = InMemorySharedPreferences()
        val failingClear = CommittingSharedPreferences(delegate, succeeds = true) { false }
        val factory = object : SecurePreferencesFactory {
            override fun open(): SharedPreferences {
                delegate.edit()
                    .putString("access_token", "access-a")
                    .putString("refresh_token", "refresh-a")
                    .putLong("expires_at", Long.MAX_VALUE)
                    .putString("server_identity", "https://pim.example")
                    .commit()
                return failingClear
            }
        }
        val manager = TokenManager(factory, nowMillis = { 1_000L })

        assertEquals("access-a", manager.accessToken())
        manager.clear()
        assertNull(manager.snapshot().tokens)
        assertNull(manager.accessToken())
    }

    private fun manager(nowMillis: Long): TokenManager {
        return TokenManager(
            FakeSecurePreferencesFactory(InMemorySharedPreferences()),
            nowMillis = { nowMillis }
        )
    }

    private companion object {
        const val TEST_SERVER_URL = "https://pim.example/api/v1/"
    }
}

private class FakeSecurePreferencesFactory(
    private val preferences: SharedPreferences
) : SecurePreferencesFactory {
    override fun open(): SharedPreferences = preferences
}

private class InMemorySharedPreferences : SharedPreferences {
    private val values = linkedMapOf<String, Any?>()

    override fun getAll(): MutableMap<String, *> = synchronized(values) { values.toMutableMap() }
    override fun getString(key: String, defValue: String?): String? = synchronized(values) {
        values[key] as? String ?: defValue
    }
    override fun getStringSet(key: String, defValues: MutableSet<String>?): MutableSet<String>? = defValues
    override fun getInt(key: String, defValue: Int): Int = synchronized(values) { values[key] as? Int ?: defValue }
    override fun getLong(key: String, defValue: Long): Long = synchronized(values) { values[key] as? Long ?: defValue }
    override fun getFloat(key: String, defValue: Float): Float = synchronized(values) { values[key] as? Float ?: defValue }
    override fun getBoolean(key: String, defValue: Boolean): Boolean = synchronized(values) {
        values[key] as? Boolean ?: defValue
    }
    override fun contains(key: String): Boolean = synchronized(values) { values.containsKey(key) }
    override fun edit(): SharedPreferences.Editor = Editor()
    override fun registerOnSharedPreferenceChangeListener(listener: SharedPreferences.OnSharedPreferenceChangeListener?) = Unit
    override fun unregisterOnSharedPreferenceChangeListener(listener: SharedPreferences.OnSharedPreferenceChangeListener?) = Unit

    private inner class Editor : SharedPreferences.Editor {
        private val edits = linkedMapOf<String, Any?>()
        private val removals = linkedSetOf<String>()
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
            synchronized(values) {
                if (clearAll) values.clear()
                removals.forEach(values::remove)
                values.putAll(edits)
            }
            return true
        }
        override fun apply() {
            commit()
        }
    }
}

private class TrivialFailingSharedPreferences : SharedPreferences {
    private val map = linkedMapOf<String, Any?>()
    override fun getAll() = LinkedHashMap(map) as MutableMap<String, *>
    override fun getString(key: String, defValue: String?) = (map[key] as? String) ?: defValue
    override fun getStringSet(key: String, defValues: MutableSet<String>?) = defValues
    override fun getInt(key: String, defValue: Int) = (map[key] as? Int) ?: defValue
    override fun getLong(key: String, defValue: Long) = (map[key] as? Long) ?: defValue
    override fun getFloat(key: String, defValue: Float) = (map[key] as? Float) ?: defValue
    override fun getBoolean(key: String, defValue: Boolean) = (map[key] as? Boolean) ?: defValue
    override fun contains(key: String) = map.containsKey(key)
    override fun edit(): SharedPreferences.Editor = FailingEditor()
    override fun registerOnSharedPreferenceChangeListener(l: SharedPreferences.OnSharedPreferenceChangeListener?) = Unit
    override fun unregisterOnSharedPreferenceChangeListener(l: SharedPreferences.OnSharedPreferenceChangeListener?) = Unit

    private inner class FailingEditor : SharedPreferences.Editor {
        private val edits = linkedMapOf<String, Any?>()
        override fun putString(key: String, value: String?): SharedPreferences.Editor = apply { edits[key] = value }
        override fun putStringSet(key: String, values: MutableSet<String>?): SharedPreferences.Editor = this
        override fun putInt(key: String, value: Int): SharedPreferences.Editor = apply { edits[key] = value }
        override fun putLong(key: String, value: Long): SharedPreferences.Editor = apply { edits[key] = value }
        override fun putFloat(key: String, value: Float): SharedPreferences.Editor = apply { edits[key] = value }
        override fun putBoolean(key: String, value: Boolean): SharedPreferences.Editor = apply { edits[key] = value }
        override fun remove(key: String): SharedPreferences.Editor = apply { edits.remove(key) }
        override fun clear(): SharedPreferences.Editor = apply { edits.clear() }
        override fun commit(): Boolean = false
        override fun apply() {}
    }
}

private class CommittingSharedPreferences(
    private val delegate: InMemorySharedPreferences,
    private val succeeds: Boolean,
    private val commitResult: () -> Boolean
) : SharedPreferences by delegate {
    private var commitCalls = 0

    override fun edit(): SharedPreferences.Editor {
        return CommittingEditor(delegate.edit(), commitResult)
    }

    private class CommittingEditor(
        private val delegate: SharedPreferences.Editor,
        private val commitResult: () -> Boolean
    ) : SharedPreferences.Editor by delegate {
        override fun commit(): Boolean = commitResult()
        override fun apply() { commit() }
    }
}