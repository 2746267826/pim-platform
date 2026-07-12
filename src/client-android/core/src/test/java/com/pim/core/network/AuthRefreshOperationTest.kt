package com.pim.core.network

import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import com.pim.core.auth.AuthSessionStore
import com.pim.core.auth.AuthRefreshResult
import com.pim.core.auth.AuthSessionSnapshot
import com.pim.core.auth.AuthTokens
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
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

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
    fun realRefreshOperationMapsHttp401ToRejectedAndCoordinatorClears() = runBlocking {
        server.enqueue(MockResponse().setResponseCode(401))
        val apiService = apiService(OkHttpClient())
        val operation = RetrofitAuthRefreshOperation(
            refreshCall = { _, request -> apiService.refresh(request) }
        )
        val store = RecordingSessionStore()
        val coordinator = AuthRefreshCoordinator(store, operation, nowMillis = { 1_000L })

        val refreshed = coordinator.refreshAfterUnauthorized("token-a")

        assertFalse(refreshed)
        assertEquals(1, store.clearCalls)
        val request = server.takeRequest()
        assertEquals("/api/v1/auth/refresh", request.path)
        assertNull(request.getHeader("Authorization"))
    }

    @Test
    fun realRefreshOperationReturnsValidatedRotatedSession() = runBlocking {
        server.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setBody(
                    """{"code":0,"message":"OK","data":{"accessToken":"token-b","refreshToken":"refresh-b","expiresAt":"2099-01-01T00:00:00Z"}}"""
                )
        )
        val apiService = apiService(OkHttpClient())
        val operation = RetrofitAuthRefreshOperation(
            refreshCall = { _, request -> apiService.refresh(request) },
            nowMillis = { 1_000L }
        )

        val result = operation.refresh("refresh-a", TEST_SERVER_IDENTITY)

        assertTrue(result is AuthRefreshResult.Success)
        val tokens = (result as AuthRefreshResult.Success).tokens
        assertEquals("token-b", tokens.accessToken)
        assertEquals("refresh-b", tokens.refreshToken)
        assertEquals(java.time.Instant.parse("2099-01-01T00:00:00Z").toEpochMilli(), tokens.expiresAtUtcMillis)
    }

    @Test
    fun serverFailureRemainsAnHttpException() {
        server.enqueue(MockResponse().setResponseCode(503))
        val apiService = apiService(OkHttpClient())
        val operation = RetrofitAuthRefreshOperation(
            refreshCall = { _, request -> apiService.refresh(request) }
        )
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
        val failingApiService = apiService(client)
        val operation = RetrofitAuthRefreshOperation(
            refreshCall = { _, request -> failingApiService.refresh(request) }
        )
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
            refreshCall = { _, _ -> throw CancellationException("cancelled") }
        )
        val store = RecordingSessionStore()
        val coordinator = AuthRefreshCoordinator(store, operation, nowMillis = { 1_000L })

        assertThrows(CancellationException::class.java) {
            runBlocking { coordinator.refreshAfterUnauthorized("token-a") }
        }
        assertEquals(0, store.clearCalls)
    }

    @Test
    fun invalidRefreshPayloadsAreRejectedBeforeSessionCommit() = runBlocking {
        val invalidPayloads = listOf(
            """{"code":0,"message":"OK","data":{"accessToken":"","refreshToken":"refresh-b","expiresAt":"1970-01-01T00:00:02Z"}}""",
            """{"code":0,"message":"OK","data":{"accessToken":"access-b","refreshToken":"","expiresAt":"1970-01-01T00:00:02Z"}}""",
            """{"code":0,"message":"OK","data":{"accessToken":"access-b","refreshToken":"refresh-b","expiresAt":"invalid"}}""",
            """{"code":0,"message":"OK","data":{"accessToken":"access-b","refreshToken":"refresh-b","expiresAt":"1970-01-01T00:00:01Z"}}"""
        )
        val apiService = apiService(OkHttpClient())
        val operation = RetrofitAuthRefreshOperation(
            refreshCall = { _, request -> apiService.refresh(request) },
            nowMillis = { 1_000L }
        )

        for (payload in invalidPayloads) {
            server.enqueue(
                MockResponse()
                    .setResponseCode(200)
                    .setHeader("Content-Type", "application/json")
                    .setBody(payload)
            )

            assertEquals(
                AuthRefreshResult.Rejected,
                operation.refresh("refresh-a", TEST_SERVER_IDENTITY)
            )
        }
    }

    @Test
    fun capturedServerIdentityPinsRefreshAcrossConcurrentSettingsSwitch() {
        val serverB = MockWebServer()
        serverB.start()
        val entered = CountDownLatch(1)
        val release = CountDownLatch(1)
        val executor = Executors.newSingleThreadExecutor()
        val serverAIdentity = com.pim.core.settings.PimServerEndpoints
            .trustedOriginOf(server.url("/"))
        val serverBIdentity = com.pim.core.settings.PimServerEndpoints
            .trustedOriginOf(serverB.url("/"))
        server.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setBody(
                    """{"code":0,"message":"OK","data":{"accessToken":"token-b","refreshToken":"refresh-b","expiresAt":"2099-01-01T00:00:00Z"}}"""
                )
        )
        val serviceA = apiService(OkHttpClient())
        val serviceB = Retrofit.Builder()
            .baseUrl(serverB.url("/api/v1/"))
            .client(OkHttpClient())
            .addConverterFactory(
                Json { ignoreUnknownKeys = true }
                    .asConverterFactory("application/json".toMediaType())
            )
            .build()
            .create(ApiService::class.java)
        val operation = RetrofitAuthRefreshOperation(
            refreshCall = { serverIdentity, request ->
                entered.countDown()
                check(release.await(5, TimeUnit.SECONDS))
                when (serverIdentity) {
                    serverAIdentity -> serviceA.refresh(request)
                    serverBIdentity -> serviceB.refresh(request)
                    else -> error("unexpected server identity: $serverIdentity")
                }
            },
            nowMillis = { 1_000L }
        )

        try {
            val result = executor.submit<AuthRefreshResult> {
                runBlocking { operation.refresh("refresh-a", serverAIdentity) }
            }
            assertTrue(entered.await(5, TimeUnit.SECONDS))

            val currentSettingsIdentity = serverBIdentity
            release.countDown()

            assertEquals(serverBIdentity, currentSettingsIdentity)
            assertTrue(result.get(5, TimeUnit.SECONDS) is AuthRefreshResult.Success)
            assertEquals(1, server.requestCount)
            assertEquals(0, serverB.requestCount)
            assertEquals("/api/v1/auth/refresh", server.takeRequest().path)
        } finally {
            release.countDown()
            executor.shutdownNow()
            serverB.shutdown()
        }
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
        private var current = AuthSessionSnapshot(
            AuthTokens("token-a", "refresh-secret", Long.MAX_VALUE),
            generation = 0L,
            serverIdentity = TEST_SERVER_IDENTITY
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
                current.generation + 1L,
                serverIdentity
            )
            return true
        }

        override fun compareAndSave(expected: AuthSessionSnapshot, tokens: AuthTokens): Boolean {
            if (current != expected) return false
            current = AuthSessionSnapshot(
                tokens,
                current.generation + 1L,
                expected.serverIdentity
            )
            return true
        }

        override fun clear(): Boolean {
            clearCalls++
            current = AuthSessionSnapshot(null, current.generation + 1L)
            return true
        }

        override fun clearIfUnchanged(expected: AuthSessionSnapshot): Boolean {
            if (current != expected) return false
            return clear()
        }
    }

    private companion object {
        const val TEST_SERVER_IDENTITY = "https://pim.example"
    }
}
