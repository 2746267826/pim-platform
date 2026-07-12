package com.pim.app.status

import android.content.Context
import android.content.ContextWrapper
import android.content.SharedPreferences
import androidx.test.core.app.ApplicationProvider
import com.pim.core.auth.AuthSessionSnapshot
import com.pim.core.auth.AuthSessionStore
import com.pim.core.auth.AuthTokens
import com.pim.core.models.RefreshRequest
import com.pim.core.network.ApiClientProvider
import com.pim.core.settings.PimServerEndpoints
import com.pim.core.settings.ServerSettingsStore
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ServerSettingsSecurityTest {
    private lateinit var context: Context

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        context.getSharedPreferences("pim_server_settings", Context.MODE_PRIVATE)
            .edit()
            .clear()
            .commit()
    }

    @Test
    fun setBaseUrlInvalidatesDifferentOriginSessionBeforeReturning() {
        val sessions = RecordingBoundSessionStore("https://server-a.example")
        val settings = ServerSettingsStore(context, sessions)

        settings.setBaseUrl("https://server-a.example/api/v1/")
        assertEquals(0, sessions.clearCalls)

        settings.setBaseUrl("https://server-b.example/api/v1/")

        assertEquals(1, sessions.clearCalls)
        assertNull(sessions.snapshot().tokens)
    }

    @Test
    fun setBaseUrlCommitFailureClearsSessionAndThrows() {
        val scriptedPreferences = ScriptedCommitSharedPreferences(
            context.getSharedPreferences("pim_server_settings", Context.MODE_PRIVATE)
        )
        val sessions = RecordingBoundSessionStore(SERVER_A_IDENTITY)
        val settings = ServerSettingsStore(
            SharedPreferencesContext(context, scriptedPreferences),
            sessions
        )
        settings.setBaseUrl(SERVER_A_URL)
        scriptedPreferences.enqueueCommitResult(false)

        assertThrows(IllegalStateException::class.java) {
            settings.setBaseUrl(SERVER_B_URL)
        }

        assertEquals(1, sessions.clearCalls)
        assertNull(sessions.snapshot().tokens)
    }

    @Test
    fun explicitRefreshServiceStaysPinnedToServerAAfterSettingsSwitchesToB() = runBlocking {
        val serverA = MockWebServer()
        val serverB = MockWebServer()
        serverA.start()
        serverB.start()
        serverA.enqueue(MockResponse().setResponseCode(401))
        serverB.enqueue(MockResponse().setResponseCode(418))
        val sessions = RecordingBoundSessionStore(null)
        val settings = ServerSettingsStore(context, sessions)
        val serverAUrl = serverA.url("/api/v1/").toString()
        val serverBUrl = serverB.url("/api/v1/").toString()
        val serverAIdentity = PimServerEndpoints.from(serverAUrl).trustedOrigin
        val provider = ApiClientProvider(OkHttpClient(), Json { ignoreUnknownKeys = true }, settings)

        try {
            settings.setBaseUrl(serverAUrl)
            settings.setBaseUrl(serverBUrl)

            val response = provider
                .refreshApiServiceForServer(serverAIdentity)
                .refresh(RefreshRequest("refresh-a"))

            assertEquals(401, response.code())
            assertEquals(1, serverA.requestCount)
            assertEquals(0, serverB.requestCount)
            assertEquals("/api/v1/auth/refresh", serverA.takeRequest().path)
        } finally {
            serverA.shutdown()
            serverB.shutdown()
        }
    }

    @Test
    fun lateServerALoginResponseCannotCommitAfterConcurrentSwitchToServerB() {
        val sessions = RecordingBoundSessionStore(null)
        val settings = ServerSettingsStore(context, sessions)
        val serverAUrl = "https://server-a.example/api/v1/"
        val serverAIdentity = PimServerEndpoints.from(serverAUrl).trustedOrigin
        settings.setBaseUrl(serverAUrl)
        val entered = CountDownLatch(1)
        val release = CountDownLatch(1)
        val executor = Executors.newSingleThreadExecutor()

        try {
            val committed = executor.submit<Boolean> {
                entered.countDown()
                check(release.await(5, TimeUnit.SECONDS))
                settings.saveSessionIfCurrentServer(serverAIdentity) {
                    sessions.save(
                        "late-access-a",
                        "late-refresh-a",
                        Long.MAX_VALUE,
                        serverAIdentity
                    )
                }
            }
            assertEquals(true, entered.await(5, TimeUnit.SECONDS))

            settings.setBaseUrl("https://server-b.example/api/v1/")
            release.countDown()

            assertFalse(committed.get(5, TimeUnit.SECONDS))
            assertNull(sessions.snapshot().tokens)
        } finally {
            release.countDown()
            executor.shutdownNow()
        }
    }

    @Test(timeout = 2_000L)
    fun serverSwitchAbortsWhenOldSessionCannotBeDurablyCleared() {
        val sessions = RecordingBoundSessionStore(
            serverIdentity = "https://server-a.example",
            clearSucceeds = false
        )
        val settings = ServerSettingsStore(context, sessions)
        settings.setBaseUrl("https://server-a.example/api/v1/")

        assertThrows(IllegalStateException::class.java) {
            settings.setBaseUrl("https://server-b.example/api/v1/")
        }

        assertEquals("https://server-a.example/api/v1/", settings.getBaseUrl())
        assertEquals("access-a", sessions.snapshot().tokens?.accessToken)
    }

    @Test
    fun urlCommitFailureAfterSessionClearPreservesOldUrlWithTokenCleared() {
        val scriptedPreferences = ScriptedCommitSharedPreferences(
            context.getSharedPreferences("pim_server_settings", Context.MODE_PRIVATE)
        )
        val sessions = RecordingBoundSessionStore(
            serverIdentity = SERVER_A_IDENTITY
        )
        val settings = ServerSettingsStore(
            SharedPreferencesContext(context, scriptedPreferences),
            sessions
        )
        settings.setBaseUrl(SERVER_A_URL)
        scriptedPreferences.enqueueCommitResult(false)

        assertThrows(IllegalStateException::class.java) {
            settings.setBaseUrl(SERVER_B_URL)
        }

        assertEquals(SERVER_A_URL, settings.getBaseUrl())
        assertNull(sessions.snapshot().serverIdentity)
        assertNull(sessions.accessTokenForServerIdentity(SERVER_A_IDENTITY))
        assertNull(sessions.accessTokenForServerIdentity(SERVER_B_IDENTITY))
    }

    private companion object {
        const val SERVER_A_URL = "https://server-a.example/api/v1/"
        const val SERVER_B_URL = "https://server-b.example/api/v1/"
        const val SERVER_A_IDENTITY = "https://server-a.example"
        const val SERVER_B_IDENTITY = "https://server-b.example"
    }
}

private class SharedPreferencesContext(
    base: Context,
    private val preferences: SharedPreferences
) : ContextWrapper(base) {
    override fun getSharedPreferences(name: String?, mode: Int): SharedPreferences = preferences
}

private open class ScriptedCommitSharedPreferences(
    private val delegate: SharedPreferences
) : SharedPreferences {
    private val commitResults = ArrayDeque<Boolean>()

    fun enqueueCommitResult(result: Boolean) {
        commitResults.addLast(result)
    }

    override fun getAll() = delegate.getAll()
    override fun getString(key: String, defValue: String?) = delegate.getString(key, defValue)
    override fun getStringSet(key: String, defValues: MutableSet<String>?) = delegate.getStringSet(key, defValues)
    override fun getInt(key: String, defValue: Int) = delegate.getInt(key, defValue)
    override fun getLong(key: String, defValue: Long) = delegate.getLong(key, defValue)
    override fun getFloat(key: String, defValue: Float) = delegate.getFloat(key, defValue)
    override fun getBoolean(key: String, defValue: Boolean) = delegate.getBoolean(key, defValue)
    override fun contains(key: String) = delegate.contains(key)
    override fun edit(): SharedPreferences.Editor = ScriptedEditor(delegate.edit())
    override fun registerOnSharedPreferenceChangeListener(listener: SharedPreferences.OnSharedPreferenceChangeListener?) =
        delegate.registerOnSharedPreferenceChangeListener(listener)
    override fun unregisterOnSharedPreferenceChangeListener(listener: SharedPreferences.OnSharedPreferenceChangeListener?) =
        delegate.unregisterOnSharedPreferenceChangeListener(listener)

    private inner class ScriptedEditor(
        private val delegate: SharedPreferences.Editor
    ) : SharedPreferences.Editor {
        override fun putString(key: String, value: String?): SharedPreferences.Editor = apply {
            delegate.putString(key, value)
        }

        override fun putStringSet(
            key: String,
            values: MutableSet<String>?
        ): SharedPreferences.Editor = apply {
            delegate.putStringSet(key, values)
        }

        override fun putInt(key: String, value: Int): SharedPreferences.Editor = apply {
            delegate.putInt(key, value)
        }

        override fun putLong(key: String, value: Long): SharedPreferences.Editor = apply {
            delegate.putLong(key, value)
        }

        override fun putFloat(key: String, value: Float): SharedPreferences.Editor = apply {
            delegate.putFloat(key, value)
        }

        override fun putBoolean(key: String, value: Boolean): SharedPreferences.Editor = apply {
            delegate.putBoolean(key, value)
        }

        override fun remove(key: String): SharedPreferences.Editor = apply {
            delegate.remove(key)
        }

        override fun clear(): SharedPreferences.Editor = apply {
            delegate.clear()
        }

        override fun commit(): Boolean {
            val shouldCommit = if (commitResults.isEmpty()) true else commitResults.removeFirst()
            if (!shouldCommit) return false
            return delegate.commit()
        }

        override fun apply() {
            delegate.apply()
        }
    }
}

private class RecordingBoundSessionStore(
    serverIdentity: String?,
    private val clearSucceeds: Boolean = true
) : AuthSessionStore {
    private var current = AuthSessionSnapshot(
        tokens = serverIdentity?.let {
            AuthTokens("access-a", "refresh-a", Long.MAX_VALUE)
        },
        serverIdentity = serverIdentity
    )
    var clearCalls: Int = 0
        private set

    override fun snapshot(): AuthSessionSnapshot = current

    override fun save(
        accessToken: String,
        refreshToken: String,
        expiresAtUtcMillis: Long,
        serverIdentity: String
    ): Boolean {
        current = AuthSessionSnapshot(
            AuthTokens(accessToken, refreshToken, expiresAtUtcMillis),
            serverIdentity
        )
        return true
    }

    override fun clear(): Boolean {
        clearCalls++
        if (!clearSucceeds) return false
        current = AuthSessionSnapshot(null)
        return true
    }
}