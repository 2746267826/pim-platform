package com.pim.core.auth

import android.content.SharedPreferences
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
import java.util.concurrent.atomic.AtomicLong

class TokenManagerTest {
    @Test
    fun saveClearAndCompareAndSaveUseSynchronizedGeneration() {
        val manager = manager(nowMillis = 1_000L)
        val initial = manager.snapshot()

        assertTrue(manager.saveTokens("access-a", "refresh-a", "1970-01-01T00:00:02Z", TEST_SERVER_URL))
        val saved = manager.snapshot()
        manager.clear()
        val cleared = manager.snapshot()
        val staleCommit = manager.compareAndSave(
            expected = saved,
            tokens = AuthTokens("access-stale", "refresh-stale", 3_000L)
        )

        assertEquals(initial.generation + 1L, saved.generation)
        assertEquals(saved.generation + 1L, cleared.generation)
        assertFalse(staleCommit)
        assertNull(manager.accessToken())
        assertEquals(cleared.generation, manager.snapshot().generation)
    }

    @Test
    fun transientSecureStorageOpenFailureRetriesWithoutDeletingSession() {
        val preferences = InMemorySharedPreferences().apply {
            edit()
                .putString("access_token", "stale-access")
                .putString("refresh_token", "stale-refresh")
                .putLong("expires_at", Long.MAX_VALUE)
                .putString("server_identity", "https://pim.example")
                .commit()
        }
        val factory = FakeSecurePreferencesFactory(preferences, failuresBeforeSuccess = 1)

        val manager = TokenManager(factory, nowMillis = { 1_000L })

        assertEquals(SecureStorageStatus.Recovered, manager.storageStatus)
        assertEquals(0, factory.resetCalls)
        assertEquals("stale-access", manager.accessToken())
        assertEquals("stale-refresh", manager.refreshToken())
        assertEquals(Long.MAX_VALUE, manager.expiresAtUtcMillis())
    }

    @Test
    fun legacyPlaintextStorageIsRemovedBeforeSecureStorageIsOpened() {
        val events = mutableListOf<String>()
        val factory = object : SecurePreferencesFactory {
            override fun clearLegacyStorage() {
                events += "legacy-cleanup"
            }

            override fun open(): SharedPreferences {
                events += "open"
                return InMemorySharedPreferences()
            }

            override fun reset() = Unit
        }

        TokenManager(factory, nowMillis = { 1_000L })

        assertEquals(listOf("legacy-cleanup", "open"), events)
    }

    @Test
    fun persistentClassifiedOpenCorruptionIsResetAndRecreated() {
        val preferences = InMemorySharedPreferences()
        var corrupted = true
        var resetCalls = 0
        val factory = object : SecurePreferencesFactory {
            override fun open(): SharedPreferences {
                if (corrupted) throw SecureStorageCorruptionException("corrupt keyset")
                return preferences
            }

            override fun reset() {
                resetCalls++
                corrupted = false
                preferences.edit().clear().commit()
            }
        }

        val manager = TokenManager(factory, nowMillis = { 1_000L })

        assertEquals(SecureStorageStatus.Recovered, manager.storageStatus)
        assertEquals(1, resetCalls)
        assertNull(manager.accessToken())
    }

    @Test
    fun unavailableSecureStorageFallsBackToProcessMemoryWithoutThrowing() {
        val preferences = InMemorySharedPreferences()
        val factory = FakeSecurePreferencesFactory(preferences, failuresBeforeSuccess = Int.MAX_VALUE)

        val manager = TokenManager(factory, nowMillis = { 1_000L })
        assertEquals(SecureStorageStatus.Ephemeral, manager.storageStatus)
        assertTrue(
            manager.saveTokens(
                "access-memory",
                "refresh-memory",
                "1970-01-01T00:00:02Z",
                TEST_SERVER_URL
            )
        )
        assertEquals("access-memory", manager.accessToken())
        assertTrue(preferences.all.isEmpty())

        val nextProcessStore = TokenManager(factory, nowMillis = { 1_000L })
        assertEquals(SecureStorageStatus.Ephemeral, nextProcessStore.storageStatus)
        assertNull(nextProcessStore.accessToken())
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
    fun ephemeralLogoutInvalidatesInaccessibleEncryptedSessionBeforeRestart() {
        val preferences = InMemorySharedPreferences().apply {
            edit()
                .putString("access_token", "old-access")
                .putString("refresh_token", "old-refresh")
                .putLong("expires_at", Long.MAX_VALUE)
                .putString("server_identity", "https://old.example")
                .commit()
        }
        val factory = FakeSecurePreferencesFactory(preferences, failuresBeforeSuccess = 2)
        val ephemeral = TokenManager(factory, nowMillis = { 1_000L })
        assertEquals(SecureStorageStatus.Ephemeral, ephemeral.storageStatus)

        ephemeral.clear()
        val restarted = TokenManager(factory, nowMillis = { 1_000L })

        assertNull(restarted.accessToken())
        assertNull(restarted.refreshToken())
    }

    @Test
    fun ephemeralAccountSwitchCannotResurrectPreviousEncryptedSession() {
        val preferences = InMemorySharedPreferences().apply {
            edit()
                .putString("access_token", "old-access")
                .putString("refresh_token", "old-refresh")
                .putLong("expires_at", Long.MAX_VALUE)
                .putString("server_identity", "https://old.example")
                .commit()
        }
        val factory = FakeSecurePreferencesFactory(preferences, failuresBeforeSuccess = 2)
        val ephemeral = TokenManager(factory, nowMillis = { 1_000L })

        assertTrue(
            ephemeral.saveTokens(
                "new-access",
                "new-refresh",
                "1970-01-01T00:00:02Z",
                "https://new.example/api/v1/"
            )
        )
        val restarted = TokenManager(factory, nowMillis = { 1_000L })

        assertNull(restarted.accessToken())
        assertNull(restarted.refreshToken())
    }

    @Test
    fun invalidationTombstonePreventsResurrectionWhenImmediateResetFails() {
        val preferences = InMemorySharedPreferences().apply {
            edit()
                .putString("access_token", "old-access")
                .putString("refresh_token", "old-refresh")
                .putLong("expires_at", Long.MAX_VALUE)
                .putString("server_identity", "https://old.example")
                .commit()
        }
        val factory = TombstonedSecurePreferencesFactory(preferences)
        val ephemeral = TokenManager(factory, nowMillis = { 1_000L })
        assertEquals(SecureStorageStatus.Ephemeral, ephemeral.storageStatus)

        ephemeral.clear()
        assertTrue(factory.hasSessionInvalidationTombstone())
        val restarted = TokenManager(factory, nowMillis = { 1_000L })

        assertEquals(SecureStorageStatus.Recovered, restarted.storageStatus)
        assertNull(restarted.accessToken())
        assertFalse(factory.hasSessionInvalidationTombstone())
    }

    @Test
    fun logoutFailsWhenNeitherTombstoneNorResetCanInvalidateOldStorage() {
        val preferences = InMemorySharedPreferences().apply {
            edit()
                .putString("access_token", "old-access")
                .putString("refresh_token", "old-refresh")
                .putLong("expires_at", Long.MAX_VALUE)
                .putString("server_identity", "https://old.example")
                .commit()
        }
        val factory = UndurableSecurePreferencesFactory(preferences)
        val ephemeral = TokenManager(factory, nowMillis = { 1_000L })
        assertEquals(SecureStorageStatus.Ephemeral, ephemeral.storageStatus)

        assertFalse(ephemeral.clear())
        val restarted = TokenManager(factory, nowMillis = { 1_000L })

        assertEquals("old-access", restarted.accessToken())
        assertEquals("old-refresh", restarted.refreshToken())
    }

    @Test
    fun corruptedSecureStorageReadIsResetAndReopenedLoggedOut() {
        val preferences = ReadFailingSharedPreferences(InMemorySharedPreferences(), failuresRemaining = 2)
        var resetCalls = 0
        val factory = object : SecurePreferencesFactory {
            override fun open(): SharedPreferences = preferences

            override fun reset() {
                resetCalls++
                preferences.allowReads()
                preferences.edit().clear().commit()
            }
        }

        val manager = TokenManager(factory, nowMillis = { 1_000L })

        assertEquals(SecureStorageStatus.Recovered, manager.storageStatus)
        assertEquals(1, resetCalls)
        assertNull(manager.accessToken())
    }

    @Test
    fun persistentlyUnreadableSecureStorageFallsBackToEphemeralSession() {
        val preferences = ReadFailingSharedPreferences(
            InMemorySharedPreferences(),
            failuresRemaining = Int.MAX_VALUE
        )
        val factory = object : SecurePreferencesFactory {
            override fun open(): SharedPreferences = preferences
            override fun reset() {
                preferences.edit().clear().commit()
            }
        }

        val manager = TokenManager(factory, nowMillis = { 1_000L })

        assertEquals(SecureStorageStatus.Ephemeral, manager.storageStatus)
        assertTrue(
            manager.saveTokens(
                "memory-access",
                "memory-refresh",
                "1970-01-01T00:00:02Z",
                TEST_SERVER_URL
            )
        )
        assertEquals("memory-access", manager.accessToken())
    }

    @Test
    fun failedSecureClearResetsStorageAndContinuesEphemerally() {
        val delegate = InMemorySharedPreferences().apply {
            edit()
                .putString("access_token", "access-a")
                .putString("refresh_token", "refresh-a")
                .putLong("expires_at", Long.MAX_VALUE)
                .putString("server_identity", "https://pim.example")
                .commit()
        }
        val preferences = ClearCommitFailingSharedPreferences(delegate)
        var resetCalls = 0
        val factory = object : SecurePreferencesFactory {
            override fun open(): SharedPreferences = preferences

            override fun reset() {
                resetCalls++
                preferences.forceClear()
            }
        }
        val manager = TokenManager(factory, nowMillis = { 1_000L })

        manager.clear()

        assertEquals(1, resetCalls)
        assertEquals(SecureStorageStatus.Ephemeral, manager.storageStatus)
        assertNull(manager.accessToken())
        assertNull(TokenManager(factory, nowMillis = { 1_000L }).accessToken())
    }

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
    fun switchingFromServerAToBBeforeConnectionProbeClearsBoundSession() {
        val manager = manager(nowMillis = 1_000L)
        assertTrue(
            manager.saveTokens(
                "access-a",
                "refresh-a",
                "1970-01-01T00:00:03Z",
                "https://server-a.example/api/v1/"
            )
        )

        assertFalse(
            manager.clearIfBoundToDifferentServer("https://server-a.example/api/v1")
        )
        assertEquals(
            "access-a",
            manager.getAccessTokenForServer("https://server-a.example/api/v1/")
        )

        assertTrue(
            manager.clearIfBoundToDifferentServer("https://server-b.example/api/v1/")
        )
        assertNull(manager.snapshot().tokens)
        assertNull(manager.getAccessTokenForServer("https://server-b.example/api/v1/"))
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
    private val preferences: SharedPreferences,
    private val failuresBeforeSuccess: Int = 0
) : SecurePreferencesFactory {
    var openCalls: Int = 0
        private set
    var resetCalls: Int = 0
        private set

    override fun open(): SharedPreferences {
        openCalls++
        if (openCalls <= failuresBeforeSuccess) error("secure storage unavailable")
        return preferences
    }

    override fun reset() {
        resetCalls++
        preferences.edit().clear().commit()
    }
}

private class TombstonedSecurePreferencesFactory(
    private val preferences: SharedPreferences
) : SecurePreferencesFactory {
    private var openFailuresRemaining = 2
    private var resetFailuresRemaining = 1
    private var tombstoned = false

    override fun open(): SharedPreferences {
        if (openFailuresRemaining > 0) {
            openFailuresRemaining--
            error("secure storage temporarily unavailable")
        }
        return preferences
    }

    override fun reset() {
        if (resetFailuresRemaining > 0) {
            resetFailuresRemaining--
            error("secure storage reset temporarily unavailable")
        }
        preferences.edit().clear().commit()
    }

    override fun markSessionInvalidated(): Boolean {
        tombstoned = true
        return true
    }

    override fun hasSessionInvalidationTombstone(): Boolean = tombstoned

    override fun clearSessionInvalidationTombstone() {
        tombstoned = false
    }
}

private class UndurableSecurePreferencesFactory(
    private val preferences: SharedPreferences
) : SecurePreferencesFactory {
    private var openFailuresRemaining = 2

    override fun open(): SharedPreferences {
        if (openFailuresRemaining > 0) {
            openFailuresRemaining--
            error("secure storage temporarily unavailable")
        }
        return preferences
    }

    override fun reset() {
        error("secure storage reset failed")
    }

    override fun markSessionInvalidated(): Boolean = false
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

private class ReadFailingSharedPreferences(
    private val delegate: SharedPreferences,
    private var failuresRemaining: Int
) : SharedPreferences by delegate {
    override fun getString(key: String, defValue: String?): String? {
        if (failuresRemaining > 0) {
            failuresRemaining--
            error("encrypted preferences are unreadable")
        }
        return delegate.getString(key, defValue)
    }

    fun allowReads() {
        failuresRemaining = 0
    }
}

private class ClearCommitFailingSharedPreferences(
    private val delegate: SharedPreferences
) : SharedPreferences by delegate {
    private var failNextClear = true

    override fun edit(): SharedPreferences.Editor {
        return ClearCommitFailingEditor(delegate.edit())
    }

    fun forceClear() {
        delegate.edit().clear().commit()
    }

    private inner class ClearCommitFailingEditor(
        private val delegateEditor: SharedPreferences.Editor
    ) : SharedPreferences.Editor by delegateEditor {
        private var clears = false

        override fun clear(): SharedPreferences.Editor {
            clears = true
            delegateEditor.clear()
            return this
        }

        override fun commit(): Boolean {
            if (clears && failNextClear) {
                failNextClear = false
                return false
            }
            return delegateEditor.commit()
        }

        override fun apply() {
            commit()
        }
    }
}
