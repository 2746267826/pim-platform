package com.pim.core.network

import com.pim.core.auth.AuthMode
import com.pim.core.auth.AuthRefreshOperation
import com.pim.core.auth.AuthRefreshResult
import com.pim.core.auth.AuthSessionSnapshot
import com.pim.core.auth.AuthSessionStore
import com.pim.core.auth.AuthTokens
import kotlinx.coroutines.runBlocking
import okhttp3.Call
import okhttp3.EventListener
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.mockwebserver.Dispatcher
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.RecordedRequest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.util.Collections
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger

class AuthInterceptorTest {
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
    fun server401RefreshesOnceAndRetriesWithRotatedAccessToken() {
        server.enqueue(MockResponse().setResponseCode(401))
        server.enqueue(MockResponse().setResponseCode(200).setBody("{}"))
        val store = FakeAuthSessionStore("token-a", "refresh-a", Long.MAX_VALUE)
        val refresh = RecordingRefresh(store, succeeds = true)
        val client = authenticatedClient(store, refresh)

        client.execute().use { response ->
            assertEquals(200, response.code)
        }

        assertEquals(1, refresh.calls.get())
        assertEquals(listOf("refresh-a"), refresh.tokens)
        assertEquals("Bearer token-a", server.takeRequest().getHeader("Authorization"))
        assertEquals("Bearer token-b", server.takeRequest().getHeader("Authorization"))
    }

    @Test
    fun rejectedRefreshClearsOnceAndReturnsOriginal401WithoutRetry() {
        server.enqueue(MockResponse().setResponseCode(401))
        val store = FakeAuthSessionStore("token-a", "refresh-a", Long.MAX_VALUE)
        val refresh = RecordingRefresh(store, succeeds = false)
        val client = authenticatedClient(store, refresh)

        client.execute().use { response ->
            assertEquals(401, response.code)
        }

        assertEquals(1, refresh.calls.get())
        assertEquals(1, store.clearCalls.get())
        assertEquals(1, server.requestCount)
    }

    @Test
    fun first401BodyIsClosedBeforeRefreshIsAttempted() {
        server.enqueue(MockResponse().setResponseCode(401).setBody("unauthorized"))
        val store = FakeAuthSessionStore("token-a", "refresh-a", Long.MAX_VALUE)
        val responseBodyEnded = CountDownLatch(1)
        val refreshSawClosedBody = AtomicBoolean(false)
        val refresh = AuthRefreshOperation { _, _ ->
            refreshSawClosedBody.set(responseBodyEnded.await(1, TimeUnit.SECONDS))
            AuthRefreshResult.Rejected
        }
        store.bindTo(serverIdentity())
        val coordinator = AuthRefreshCoordinator(store, refresh, nowMillis = { 1_000L })
        val client = OkHttpClient.Builder()
            .eventListener(object : EventListener() {
                override fun responseBodyEnd(call: Call, byteCount: Long) {
                    responseBodyEnded.countDown()
                }
            })
            .addInterceptor(AuthInterceptor(store, coordinator))
            .build()

        client.execute().use { response ->
            assertEquals(401, response.code)
        }

        assertTrue(refreshSawClosedBody.get())
    }

    @Test
    fun missingRefreshTokenClearsOnceAndReturnsOriginal401() {
        server.enqueue(MockResponse().setResponseCode(401))
        val store = FakeAuthSessionStore("token-a", null, Long.MAX_VALUE)
        val refresh = RecordingRefresh(store, succeeds = true)
        val client = authenticatedClient(store, refresh)

        client.execute().use { response ->
            assertEquals(401, response.code)
        }

        assertEquals(0, refresh.calls.get())
        assertEquals(1, store.clearCalls.get())
        assertEquals(1, server.requestCount)
    }

    @Test
    fun anonymousRequestOmitsAuthorizationAndNeverRefreshes() {
        server.enqueue(MockResponse().setResponseCode(200))
        val store = FakeAuthSessionStore("token-a", "refresh-a", 0L)
        val refresh = RecordingRefresh(store, succeeds = true)
        val client = authenticatedClient(store, refresh)
        val request = Request.Builder()
            .url(server.url("/health"))
            .header("Authorization", "Bearer caller-token")
            .tag(AuthMode::class.java, AuthMode.Anonymous)
            .build()

        client.newCall(request).execute().close()

        assertNull(server.takeRequest().getHeader("Authorization"))
        assertEquals(0, refresh.calls.get())
        assertEquals(0, store.clearCalls.get())
    }

    @Test
    fun requestToDifferentOriginNeverSendsOrRefreshesBoundCredentials() {
        server.enqueue(MockResponse().setResponseCode(401))
        val store = FakeAuthSessionStore(
            "token-a",
            "refresh-a",
            Long.MAX_VALUE,
            serverIdentity = "https://server-a.example"
        )
        val refresh = RecordingRefresh(store, succeeds = true)
        val client = authenticatedClient(store, refresh, bindStoreToServer = false)

        client.execute().use { response ->
            assertEquals(401, response.code)
        }

        assertNull(server.takeRequest().getHeader("Authorization"))
        assertEquals(0, refresh.calls.get())
        assertEquals(0, store.clearCalls.get())
        assertEquals(1, server.requestCount)
    }

    @Test
    fun expiredSessionRefreshesBeforeFirstNetworkRequest() {
        server.enqueue(MockResponse().setResponseCode(200))
        val store = FakeAuthSessionStore("token-a", "refresh-a", 999L)
        val refresh = RecordingRefresh(store, succeeds = true)
        val client = authenticatedClient(store, refresh, nowMillis = { 1_000L })

        client.execute().close()

        assertEquals(1, refresh.calls.get())
        assertEquals(1, server.requestCount)
        assertEquals("Bearer token-b", server.takeRequest().getHeader("Authorization"))
    }

    @Test
    fun forcedRefreshRejectsSuccessWithoutAccessTokenRotation() = runBlocking {
        val store = FakeAuthSessionStore("token-a", "refresh-a", Long.MAX_VALUE)
        val coordinator = AuthRefreshCoordinator(
            store,
            AuthRefreshOperation { _, _ ->
                AuthRefreshResult.Success(AuthTokens("token-a", "refresh-a", Long.MAX_VALUE))
            },
            nowMillis = { 1_000L }
        )

        val refreshed = coordinator.refreshAfterUnauthorized("token-a")

        assertFalse(refreshed)
        assertEquals(1, store.clearCalls.get())
    }

    @Test
    fun forcedRefreshRejectsExpiryOnlyChangeWithSameFailedAccessToken() = runBlocking {
        val store = FakeAuthSessionStore("token-a", "refresh-a", 999L)
        val coordinator = AuthRefreshCoordinator(
            store,
            AuthRefreshOperation { _, _ ->
                AuthRefreshResult.Success(AuthTokens("token-a", "refresh-a", 2_000L))
            },
            nowMillis = { 1_000L }
        )

        val refreshed = coordinator.refreshAfterUnauthorized("token-a")

        assertFalse(refreshed)
        assertEquals(1, store.clearCalls.get())
    }

    @Test
    fun expiryRefreshRejectsNoOpSuccessWithExpiredSession() = runBlocking {
        val store = FakeAuthSessionStore("token-a", "refresh-a", 999L)
        val coordinator = AuthRefreshCoordinator(
            store,
            AuthRefreshOperation { _, _ ->
                AuthRefreshResult.Success(AuthTokens("token-a", "refresh-a", 999L))
            },
            nowMillis = { 1_000L }
        )

        val refreshed = coordinator.refreshIfExpired()

        assertFalse(refreshed)
        assertEquals(1, store.clearCalls.get())
    }

    @Test
    fun expiryRefreshRequiresExpiryStrictlyAfterCurrentTime() = runBlocking {
        val store = FakeAuthSessionStore("token-a", "refresh-a", 999L)
        val coordinator = AuthRefreshCoordinator(
            store,
            AuthRefreshOperation { _, _ ->
                AuthRefreshResult.Success(AuthTokens("token-b", "refresh-b", 1_000L))
            },
            nowMillis = { 1_000L }
        )

        val refreshed = coordinator.refreshIfExpired()

        assertFalse(refreshed)
        assertEquals(1, store.clearCalls.get())
    }

    @Test
    fun expiryRefreshAcceptsNonblankTokenWithFutureExpiry() = runBlocking {
        val store = FakeAuthSessionStore("token-a", "refresh-a", 999L)
        val coordinator = AuthRefreshCoordinator(
            store,
            AuthRefreshOperation { _, _ ->
                AuthRefreshResult.Success(AuthTokens("token-b", "refresh-b", 1_001L))
            },
            nowMillis = { 1_000L }
        )

        val refreshed = coordinator.refreshIfExpired()

        assertTrue(refreshed)
        assertEquals(0, store.clearCalls.get())
        assertEquals("token-b", store.accessToken())
        assertEquals(1_001L, store.expiresAtUtcMillis())
    }

    @Test
    fun concurrentGenerationFastPathRejectsSessionInvalidatedAfterRefresh() {
        val store = ControlledConcurrentAuthSessionStore("token-a", "refresh-a", 999L)
        val refreshEntered = CountDownLatch(1)
        val releaseRefresh = CountDownLatch(1)
        val refreshCalls = AtomicInteger(0)
        val coordinator = AuthRefreshCoordinator(
            store,
            AuthRefreshOperation { _, _ ->
                refreshCalls.incrementAndGet()
                refreshEntered.countDown()
                check(releaseRefresh.await(5, TimeUnit.SECONDS))
                AuthRefreshResult.Success(AuthTokens("token-b", "refresh-b", 2_000L))
            },
            nowMillis = { 1_000L }
        )
        val firstExecutor = Executors.newSingleThreadExecutor { runnable ->
            Thread(runnable, "first-refresh")
        }
        val secondExecutor = Executors.newSingleThreadExecutor { runnable ->
            Thread(runnable, "second-refresh")
        }

        try {
            val first = firstExecutor.submit<Boolean> {
                runBlocking { coordinator.refreshAfterUnauthorized("token-a") }
            }
            assertTrue(refreshEntered.await(5, TimeUnit.SECONDS))
            val second = secondExecutor.submit<Boolean> {
                runBlocking {
                    store.markCurrentThreadAsSecond()
                    coordinator.refreshAfterUnauthorized("token-a")
                }
            }
            assertTrue(store.secondObservedBeforeMutex.await(5, TimeUnit.SECONDS))
            releaseRefresh.countDown()

            assertTrue(first.get(5, TimeUnit.SECONDS))
            assertTrue(store.secondReadInsideMutex.await(5, TimeUnit.SECONDS))
            store.save("token-b", "refresh-b", 1_000L, "https://pim.example")
            store.releaseSecondRead.countDown()

            assertFalse(second.get(5, TimeUnit.SECONDS))
            assertEquals(0, store.clearCalls.get())
            assertEquals(1, refreshCalls.get())
        } finally {
            store.releaseSecondRead.countDown()
            releaseRefresh.countDown()
            firstExecutor.shutdownNow()
            secondExecutor.shutdownNow()
        }
    }

    @Test
    fun concurrent401ResponsesCauseOneRefresh() {
        val bothOldTokenRequestsArrived = CountDownLatch(2)
        server.dispatcher = object : Dispatcher() {
            override fun dispatch(request: RecordedRequest): MockResponse {
                return when (request.getHeader("Authorization")) {
                    "Bearer token-a" -> {
                        bothOldTokenRequestsArrived.countDown()
                        bothOldTokenRequestsArrived.await(5, TimeUnit.SECONDS)
                        MockResponse().setResponseCode(401)
                    }
                    "Bearer token-b" -> MockResponse().setResponseCode(200)
                    else -> MockResponse().setResponseCode(500)
                }
            }
        }
        val store = FakeAuthSessionStore("token-a", "refresh-a", Long.MAX_VALUE)
        val refresh = RecordingRefresh(store, succeeds = true)
        val client = authenticatedClient(store, refresh)
        val executor = Executors.newFixedThreadPool(2)

        try {
            val responses = List(2) {
                executor.submit<Int> { client.execute().use { response -> response.code } }
            }.map { it.get(10, TimeUnit.SECONDS) }

            assertEquals(listOf(200, 200), responses)
            assertEquals(1, refresh.calls.get())
            assertEquals(listOf("refresh-a"), refresh.tokens)
            assertEquals(4, server.requestCount)
        } finally {
            executor.shutdownNow()
        }
    }

    @Test
    fun concurrentExpiredRequestsRefreshOnceBeforeSending() {
        repeat(2) { server.enqueue(MockResponse().setResponseCode(200)) }
        val store = FakeAuthSessionStore("token-a", "refresh-a", 999L)
        val refreshEntered = CountDownLatch(1)
        val releaseRefresh = CountDownLatch(1)
        val refresh = RecordingRefresh(
            store = store,
            succeeds = true,
            beforeResult = {
                refreshEntered.countDown()
                releaseRefresh.await(5, TimeUnit.SECONDS)
            }
        )
        val client = authenticatedClient(store, refresh, nowMillis = { 1_000L })
        val executor = Executors.newFixedThreadPool(2)

        try {
            val first = executor.submit<Int> { client.execute().use { response -> response.code } }
            refreshEntered.await(5, TimeUnit.SECONDS)
            val second = executor.submit<Int> { client.execute().use { response -> response.code } }
            releaseRefresh.countDown()

            assertEquals(200, first.get(10, TimeUnit.SECONDS))
            assertEquals(200, second.get(10, TimeUnit.SECONDS))
            assertEquals(1, refresh.calls.get())
            assertEquals(2, server.requestCount)
            repeat(2) {
                assertEquals("Bearer token-b", server.takeRequest().getHeader("Authorization"))
            }
        } finally {
            executor.shutdownNow()
        }
    }

    @Test
    fun second401IsReturnedWithoutAnotherRefreshOrRetry() {
        server.enqueue(MockResponse().setResponseCode(401))
        server.enqueue(MockResponse().setResponseCode(401))
        val store = FakeAuthSessionStore("token-a", "refresh-a", Long.MAX_VALUE)
        val refresh = RecordingRefresh(store, succeeds = true)
        val client = authenticatedClient(store, refresh)

        client.execute().use { response ->
            assertEquals(401, response.code)
        }

        assertEquals(1, refresh.calls.get())
        assertEquals(2, server.requestCount)
        assertEquals("Bearer token-a", server.takeRequest().getHeader("Authorization"))
        assertEquals("Bearer token-b", server.takeRequest().getHeader("Authorization"))
    }

    private fun authenticatedClient(
        store: FakeAuthSessionStore,
        refresh: AuthRefreshOperation,
        nowMillis: () -> Long = { 1_000L },
        bindStoreToServer: Boolean = true
    ): OkHttpClient {
        if (bindStoreToServer) store.bindTo(serverIdentity())
        val coordinator = AuthRefreshCoordinator(store, refresh, nowMillis)
        return OkHttpClient.Builder()
            .addInterceptor(AuthInterceptor(store, coordinator))
            .build()
    }

    private fun serverIdentity(): String {
        val url = server.url("/")
        return "${url.scheme}://${url.host}:${url.port}"
    }

    private fun OkHttpClient.execute(): Response {
        return newCall(
            Request.Builder()
                .url(server.url("/api/v1/status/summary"))
                .build()
        ).execute()
    }

    private class RecordingRefresh(
        private val store: FakeAuthSessionStore,
        private val succeeds: Boolean,
        private val beforeResult: () -> Unit = {}
    ) : AuthRefreshOperation {
        val calls = AtomicInteger(0)
        val tokens: MutableList<String> = Collections.synchronizedList(mutableListOf())

        override suspend fun refresh(
            refreshToken: String,
            serverIdentity: String
        ): AuthRefreshResult {
            calls.incrementAndGet()
            tokens += refreshToken
            beforeResult()
            return if (succeeds) {
                AuthRefreshResult.Success(AuthTokens("token-b", "refresh-b", Long.MAX_VALUE))
            } else {
                AuthRefreshResult.Rejected
            }
        }
    }

    private class FakeAuthSessionStore(
        access: String?,
        refresh: String?,
        expiry: Long?,
        serverIdentity: String? = "https://pim.example"
    ) : AuthSessionStore {
        val clearCalls = AtomicInteger(0)
        private val lock = Any()
        private var current = AuthSessionSnapshot(
            tokens = if (access != null && refresh != null && expiry != null) {
                AuthTokens(access, refresh, expiry)
            } else {
                null
            },
            generation = 0L,
            serverIdentity = serverIdentity
        )

        fun bindTo(serverIdentity: String) {
            synchronized(lock) {
                current = current.copy(serverIdentity = serverIdentity)
            }
        }

        override fun snapshot(): AuthSessionSnapshot = synchronized(lock) { current }

        override fun save(
            accessToken: String,
            refreshToken: String,
            expiresAtUtcMillis: Long,
            serverIdentity: String
        ): Boolean {
            synchronized(lock) {
                current = AuthSessionSnapshot(
                    AuthTokens(accessToken, refreshToken, expiresAtUtcMillis),
                    current.generation + 1L,
                    serverIdentity
                )
            }
            return true
        }

        override fun compareAndSave(expected: AuthSessionSnapshot, tokens: AuthTokens): Boolean {
            return synchronized(lock) {
                if (current != expected) return@synchronized false
                current = AuthSessionSnapshot(
                    tokens,
                    current.generation + 1L,
                    expected.serverIdentity
                )
                true
            }
        }

        override fun clear(): Boolean {
            return synchronized(lock) {
                clearCalls.incrementAndGet()
                current = AuthSessionSnapshot(null, current.generation + 1L)
                true
            }
        }

        override fun clearIfUnchanged(expected: AuthSessionSnapshot): Boolean {
            return synchronized(lock) {
                if (current != expected) return@synchronized false
                clear()
            }
        }
    }

    private class ControlledConcurrentAuthSessionStore(
        access: String?,
        refresh: String?,
        expiry: Long?
    ) : AuthSessionStore {
        val secondObservedBeforeMutex = CountDownLatch(1)
        val secondReadInsideMutex = CountDownLatch(1)
        val releaseSecondRead = CountDownLatch(1)
        val clearCalls = AtomicInteger(0)
        private val secondSnapshotReads = AtomicInteger(0)
        @Volatile private var secondThread: Thread? = null
        @Volatile private var current = AuthSessionSnapshot(
            tokens = if (access != null && refresh != null && expiry != null) {
                AuthTokens(access, refresh, expiry)
            } else {
                null
            },
            generation = 0L,
            serverIdentity = "https://pim.example"
        )

        fun markCurrentThreadAsSecond() {
            secondThread = Thread.currentThread()
        }

        override fun snapshot(): AuthSessionSnapshot {
            if (Thread.currentThread() === secondThread) {
                when (secondSnapshotReads.incrementAndGet()) {
                    1 -> secondObservedBeforeMutex.countDown()
                    2 -> {
                        secondReadInsideMutex.countDown()
                        check(releaseSecondRead.await(5, TimeUnit.SECONDS))
                    }
                }
            }
            return current
        }

        override fun save(
            accessToken: String,
            refreshToken: String,
            expiresAtUtcMillis: Long,
            serverIdentity: String
        ): Boolean {
            val before = current
            current = AuthSessionSnapshot(
                AuthTokens(accessToken, refreshToken, expiresAtUtcMillis),
                before.generation + 1L,
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
            clearCalls.incrementAndGet()
            current = AuthSessionSnapshot(null, current.generation + 1L)
            return true
        }

        override fun clearIfUnchanged(expected: AuthSessionSnapshot): Boolean {
            if (current != expected) return false
            return clear()
        }
    }
}
