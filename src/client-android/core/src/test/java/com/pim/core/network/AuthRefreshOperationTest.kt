package com.pim.core.network

import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import com.pim.core.auth.AuthSessionStore
import com.pim.core.models.AuthResponse
import com.pim.core.models.RefreshRequest
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.HttpException
import java.net.SocketTimeoutException
import java.util.concurrent.CancellationException

class AuthRefreshOperationTest {
    private lateinit var server: MockWebServer

    @Before
    fun setUp() {
        server = MockWebServer()
        server.start()
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    @Test
    fun refreshHttp401IsReturnedWithoutThrowingHttpException() = runBlocking {
        server.enqueue(MockResponse().setResponseCode(401))
        val apiService = apiService(OkHttpClient())

        val result = runCatching {
            apiService.refresh(RefreshRequest("refresh-secret")) as Any
        }

        assertTrue("HTTP 401 must be inspectable by the native refresh operation", result.isSuccess)
        val response = result.getOrThrow()
        assertTrue(response is Response<*>)
        assertEquals(401, (response as Response<*>).code())
        val request = server.takeRequest()
        assertEquals("/api/v1/auth/refresh", request.path)
        assertNull(request.getHeader("Authorization"))
    }

    @Test
    fun realRefreshOperationMapsHttp401ToFalseWithoutSaving() = runBlocking {
        server.enqueue(MockResponse().setResponseCode(401))
        val apiService = apiService(OkHttpClient())
        val saved = mutableListOf<AuthResponse>()
        val operation = RetrofitAuthRefreshOperation(apiService::refresh, saved::add)
        val store = RecordingSessionStore()
        val coordinator = AuthRefreshCoordinator(store, operation, nowMillis = { 1_000L })

        val refreshed = coordinator.refreshAfterUnauthorized("token-a")

        assertFalse(refreshed)
        assertTrue(saved.isEmpty())
        assertEquals(1, store.clearCalls)
        val request = server.takeRequest()
        assertEquals("/api/v1/auth/refresh", request.path)
        assertNull(request.getHeader("Authorization"))
    }

    @Test
    fun realRefreshOperationSavesRotatedSessionAfterSuccessfulResponse() = runBlocking {
        server.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setBody(
                    """{"code":0,"message":"OK","data":{"accessToken":"token-b","refreshToken":"refresh-b","expiresAt":"2099-01-01T00:00:00Z"}}"""
                )
        )
        val apiService = apiService(OkHttpClient())
        val saved = mutableListOf<AuthResponse>()
        val operation = RetrofitAuthRefreshOperation(apiService::refresh, saved::add)

        val refreshed = operation.refresh("refresh-a")

        assertTrue(refreshed)
        assertEquals(1, saved.size)
        assertEquals("token-b", saved.single().accessToken)
        assertEquals("refresh-b", saved.single().refreshToken)
    }

    @Test
    fun serverFailureRemainsAnHttpException() {
        server.enqueue(MockResponse().setResponseCode(503))
        val apiService = apiService(OkHttpClient())
        val operation = RetrofitAuthRefreshOperation(apiService::refresh) {}
        val store = RecordingSessionStore()
        val coordinator = AuthRefreshCoordinator(store, operation, nowMillis = { 1_000L })

        val failure = assertThrows(HttpException::class.java) {
            runBlocking { coordinator.refreshAfterUnauthorized("token-a") }
        }

        assertEquals(503, failure.code())
        assertEquals(0, store.clearCalls)
    }

    @Test
    fun transportFailureIsPropagated() {
        val client = OkHttpClient.Builder()
            .addInterceptor { throw SocketTimeoutException("timeout") }
            .build()
        val operation = RetrofitAuthRefreshOperation(apiService(client)::refresh) {}
        val store = RecordingSessionStore()
        val coordinator = AuthRefreshCoordinator(store, operation, nowMillis = { 1_000L })

        assertThrows(SocketTimeoutException::class.java) {
            runBlocking { coordinator.refreshAfterUnauthorized("token-a") }
        }
        assertEquals(0, store.clearCalls)
    }

    @Test
    fun cancellationIsPropagated() {
        val operation = RetrofitAuthRefreshOperation(
            refreshCall = { throw CancellationException("cancelled") },
            saveTokens = {}
        )
        val store = RecordingSessionStore()
        val coordinator = AuthRefreshCoordinator(store, operation, nowMillis = { 1_000L })

        assertThrows(CancellationException::class.java) {
            runBlocking { coordinator.refreshAfterUnauthorized("token-a") }
        }
        assertEquals(0, store.clearCalls)
    }

    private fun apiService(client: OkHttpClient): ApiService {
        return Retrofit.Builder()
            .baseUrl(server.url("/api/v1/"))
            .client(client)
            .addConverterFactory(
                Json { ignoreUnknownKeys = true }
                    .asConverterFactory("application/json".toMediaType())
            )
            .build()
            .create(ApiService::class.java)
    }

    private class RecordingSessionStore : AuthSessionStore {
        private var accessToken: String? = "token-a"
        private var refreshToken: String? = "refresh-secret"
        private var expiresAtUtcMillis: Long? = Long.MAX_VALUE
        var clearCalls: Int = 0
            private set

        override fun accessToken(): String? = accessToken

        override fun refreshToken(): String? = refreshToken

        override fun expiresAtUtcMillis(): Long? = expiresAtUtcMillis

        override fun save(accessToken: String, refreshToken: String, expiresAtUtcMillis: Long) {
            this.accessToken = accessToken
            this.refreshToken = refreshToken
            this.expiresAtUtcMillis = expiresAtUtcMillis
        }

        override fun clear() {
            clearCalls++
            accessToken = null
            refreshToken = null
            expiresAtUtcMillis = null
        }
    }
}
