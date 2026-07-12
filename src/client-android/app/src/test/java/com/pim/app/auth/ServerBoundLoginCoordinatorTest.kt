package com.pim.app.auth

import android.content.Context
import android.content.SharedPreferences
import androidx.test.core.app.ApplicationProvider
import com.pim.core.auth.SecurePreferencesFactory
import com.pim.core.auth.ServerBoundLoginCoordinator
import com.pim.core.auth.ServerBoundLoginResult
import com.pim.core.auth.ServerBoundLoginTransport
import com.pim.core.auth.TokenManager
import com.pim.core.models.ApiResponse
import com.pim.core.models.AuthResponse
import com.pim.core.models.LoginRequest
import com.pim.core.settings.PimServerEndpoints
import com.pim.core.settings.ServerSettingsStore
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ServerBoundLoginCoordinatorTest {
    private lateinit var context: Context

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        context.getSharedPreferences("pim_server_settings", Context.MODE_PRIVATE)
            .edit()
            .clear()
            .commit()
        context.getSharedPreferences(AUTH_PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .clear()
            .commit()
    }

    @Test
    fun delayedServerAResponseIsDiscardedAfterConcurrentSwitchToServerB() = runBlocking {
        val tokenManager = tokenManager()
        val settings = ServerSettingsStore(context, tokenManager)
        settings.setBaseUrl(SERVER_A_URL)
        val transport = BlockingLoginTransport(successfulResponse())
        val coordinator = ServerBoundLoginCoordinator(settings, tokenManager, transport)

        val login = async(Dispatchers.IO) {
            coordinator.login(" alice ", "secret")
        }
        assertTrue(transport.entered.await(5, TimeUnit.SECONDS))

        settings.setBaseUrl(SERVER_B_URL)
        transport.release.countDown()

        assertEquals(ServerBoundLoginResult.StaleServer, login.await())
        assertEquals(SERVER_B_URL, settings.getBaseUrl())
        assertEquals(PimServerEndpoints.from(SERVER_A_URL).trustedOrigin, transport.serverIdentity.get())
        assertEquals(LoginRequest("alice", "secret"), transport.request.get())
        assertNull(tokenManager.snapshot().tokens)
        assertNull(tokenManager.snapshot().serverIdentity)
    }

    @Test
    fun responseIsSavedWhenCapturedServerRemainsCurrent() = runBlocking {
        val tokenManager = tokenManager()
        val settings = ServerSettingsStore(context, tokenManager)
        settings.setBaseUrl(SERVER_A_URL)
        val transport = ServerBoundLoginTransport { _, _ -> successfulResponse() }
        val coordinator = ServerBoundLoginCoordinator(settings, tokenManager, transport)

        val result = coordinator.login("alice", "secret")

        assertEquals(ServerBoundLoginResult.Success, result)
        assertEquals("access-a", tokenManager.snapshot().tokens?.accessToken)
        assertEquals("refresh-a", tokenManager.snapshot().tokens?.refreshToken)
        assertEquals(
            PimServerEndpoints.from(SERVER_A_URL).trustedOrigin,
            tokenManager.snapshot().serverIdentity
        )
    }

    @Test
    fun secureStorageSaveFailureIsReportedWithoutCreatingSession() = runBlocking {
        val backingPreferences = context.getSharedPreferences(AUTH_PREFS_NAME, Context.MODE_PRIVATE)
        val tokenManager = tokenManager(
            preferences = CommitFailingSharedPreferences(backingPreferences)
        )
        val settings = ServerSettingsStore(context, tokenManager)
        settings.setBaseUrl(SERVER_A_URL)
        val transport = ServerBoundLoginTransport { _, _ -> successfulResponse() }
        val coordinator = ServerBoundLoginCoordinator(settings, tokenManager, transport)

        val result = coordinator.login("alice", "secret")

        assertEquals(ServerBoundLoginResult.SessionSaveFailed, result)
        assertEquals(SERVER_A_URL, settings.getBaseUrl())
        assertNull(tokenManager.snapshot().tokens)
        assertNull(tokenManager.snapshot().serverIdentity)
    }

    @Test
    fun transportCancellationIsPropagated() {
        val tokenManager = tokenManager()
        val settings = ServerSettingsStore(context, tokenManager)
        settings.setBaseUrl(SERVER_A_URL)
        val coordinator = ServerBoundLoginCoordinator(
            settings,
            tokenManager,
            ServerBoundLoginTransport { _, _ -> throw CancellationException("cancelled") }
        )

        assertThrows(CancellationException::class.java) {
            runBlocking { coordinator.login("alice", "secret") }
        }
    }

    private fun tokenManager(
        preferences: SharedPreferences = context.getSharedPreferences(AUTH_PREFS_NAME, Context.MODE_PRIVATE)
    ): TokenManager {
        return TokenManager(
            securePreferencesFactory = TestSecurePreferencesFactory(preferences),
            nowMillis = { 1_000L }
        )
    }

    private fun successfulResponse(): ApiResponse<AuthResponse> {
        return ApiResponse(
            code = 0,
            message = "ok",
            data = AuthResponse(
                accessToken = "access-a",
                refreshToken = "refresh-a",
                expiresAt = "2099-01-01T00:00:00Z"
            )
        )
    }

    private companion object {
        const val AUTH_PREFS_NAME = "server_bound_login_auth_test"
        const val SERVER_A_URL = "https://server-a.example/api/v1/"
        const val SERVER_B_URL = "https://server-b.example/api/v1/"
    }
}

private class BlockingLoginTransport(
    private val response: ApiResponse<AuthResponse>
) : ServerBoundLoginTransport {
    val entered = CountDownLatch(1)
    val release = CountDownLatch(1)
    val serverIdentity = AtomicReference<String>()
    val request = AtomicReference<LoginRequest>()

    override suspend fun login(
        serverIdentity: String,
        request: LoginRequest
    ): ApiResponse<AuthResponse> {
        this.serverIdentity.set(serverIdentity)
        this.request.set(request)
        entered.countDown()
        check(release.await(5, TimeUnit.SECONDS))
        return response
    }
}

private class TestSecurePreferencesFactory(
    private val preferences: SharedPreferences
) : SecurePreferencesFactory {
    override fun open(): SharedPreferences = preferences
}

private class CommitFailingSharedPreferences(
    private val delegate: SharedPreferences
) : SharedPreferences by delegate {
    override fun edit(): SharedPreferences.Editor = CommitFailingEditor(delegate.edit())

    private class CommitFailingEditor(
        private val delegate: SharedPreferences.Editor
    ) : SharedPreferences.Editor by delegate {
        override fun putString(key: String, value: String?): SharedPreferences.Editor = apply {
            delegate.putString(key, value)
        }

        override fun putLong(key: String, value: Long): SharedPreferences.Editor = apply {
            delegate.putLong(key, value)
        }

        override fun commit(): Boolean = false

        override fun apply() = Unit
    }
}