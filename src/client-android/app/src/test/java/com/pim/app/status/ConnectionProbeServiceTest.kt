package com.pim.app.status

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.pim.core.auth.AuthMode
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.async
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import okhttp3.Interceptor
import okhttp3.OkHttpClient
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
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong
import javax.net.ssl.SSLHandshakeException

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ConnectionProbeServiceTest {
    private lateinit var server: MockWebServer
    private lateinit var preferencesContext: Context
    private lateinit var tokenSource: FakeProbeTokenSource
    private lateinit var anonymousModes: MutableList<AuthMode?>
    private lateinit var authenticatedModes: MutableList<AuthMode?>
    private lateinit var service: ConnectionProbeService

    private val serverUrl: String
        get() = server.url("/api/v1/").toString()

    @Before
    fun setUp() {
        server = MockWebServer()
        server.start()
        preferencesContext = ApplicationProvider.getApplicationContext()
        tokenSource = FakeProbeTokenSource("probe-access")
        anonymousModes = mutableListOf()
        authenticatedModes = mutableListOf()
        val anonymousClient = OkHttpClient.Builder()
            .addInterceptor { chain ->
                anonymousModes += chain.request().tag(AuthMode::class.java)
                chain.proceed(chain.request())
            }
            .build()
        val authenticatedClient = OkHttpClient.Builder()
            .addInterceptor { chain ->
                authenticatedModes += chain.request().tag(AuthMode::class.java)
                chain.proceed(
                    chain.request().newBuilder()
                        .header("Authorization", "Bearer probe-access")
                        .build()
                )
            }
            .build()
        service = ConnectionProbeService(
            anonymousClient = anonymousClient,
            authenticatedClient = authenticatedClient,
            tokenSource = tokenSource,
            wallClockMillis = { 1_000L },
            monotonicNanos = { 0L }
        )
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    @Test
    fun allSuccessfulStagesWithFutureCapabilitiesAreReachableInExactOrder() = runTest {
        enqueueVersion(capabilities = listOf("mobileItemResultsV1", "androidEmbedV1"))
        enqueueJson(200, """{"code":0,"message":"OK","data":{"status":"Healthy"}}""")
        enqueueHtml(200)

        val result = service.probe(serverUrl)

        assertEquals(ConnectionProbeOutcome.Reachable, result.outcome)
        assertEquals(ConnectionProbeStage.WebRoot, result.lastCompletedStage)
        assertEquals(ServerCapabilities(mobileItemResultsV1 = true, androidEmbedV1 = true), result.capabilities)
        assertNull(result.failureKind)
        assertEquals(1_000L, result.checkedAtUtcMillis)
        assertEquals(serverUrl, result.serverIdentity)
        assertEquals(ConnectionProbeStage.entries.toSet(), result.latencyMillisByStage.keys)

        val requests = List(3) { server.takeRequest() }
        assertEquals(
            listOf(
                "/api/version",
                "/api/v1/status/summary",
                "/"
            ),
            requests.map { it.path }
        )
        assertNull(requests[0].getHeader("Authorization"))
        assertEquals("Bearer probe-access", requests[1].getHeader("Authorization"))
        assertNull(requests[2].getHeader("Authorization"))
        assertEquals(List(2) { AuthMode.Anonymous }, anonymousModes)
        assertEquals(listOf(AuthMode.Required), authenticatedModes)
    }

    @Test
    fun transportFailuresHaveStableKindsAndSafeEvidence() = runTest {
        val cases = listOf(
            UnknownHostException("access=token-secret") to ConnectionFailureKind.Dns,
            ConnectException("refresh=refresh-secret") to ConnectionFailureKind.Connect,
            SocketTimeoutException("Authorization: Bearer token-secret") to ConnectionFailureKind.Timeout,
            SSLHandshakeException("certificate token-secret") to ConnectionFailureKind.Tls
        )

        for ((failure, expectedKind) in cases) {
            val throwingClient = OkHttpClient.Builder()
                .addInterceptor(Interceptor { throw failure })
                .build()
            val result = serviceFor(throwingClient).probe("https://pim.invalid/api/v1/")

            assertEquals(ConnectionProbeOutcome.Blocked, result.outcome)
            assertEquals(expectedKind, result.failureKind)
            assertEquals(ConnectionProbeStage.Url, result.lastCompletedStage)
            assertTrue(result.latencyMillisByStage.containsKey(ConnectionProbeStage.Version))
            assertFalse(result.safeMessage.orEmpty().contains("token-secret"))
            assertFalse(result.safeMessage.orEmpty().contains("refresh-secret"))
            assertFalse(result.safeMessage.orEmpty().contains("Authorization"))
        }
    }

    @Test
    fun version404IsWrongPathAndBlocked() = runTest {
        server.enqueue(MockResponse().setResponseCode(404))

        val result = service.probe(serverUrl)

        assertEquals(ConnectionProbeOutcome.Blocked, result.outcome)
        assertEquals(ConnectionFailureKind.WrongPath, result.failureKind)
        assertEquals(404, result.httpStatus)
        assertEquals(ConnectionProbeStage.Url, result.lastCompletedStage)
    }

    @Test
    fun non404HttpFailureDoesNotExposeResponseBody() = runTest {
        server.enqueue(
            MockResponse()
                .setResponseCode(503)
                .setBody("access=token-secret refresh=refresh-secret")
        )

        val result = service.probe(serverUrl)

        assertEquals(ConnectionProbeOutcome.Blocked, result.outcome)
        assertEquals(ConnectionFailureKind.Http, result.failureKind)
        assertEquals(503, result.httpStatus)
        assertFalse(result.safeMessage.orEmpty().contains("token-secret"))
        assertFalse(result.safeMessage.orEmpty().contains("refresh-secret"))
    }

    @Test
    fun missingMobileCapabilityIsBlockedAsIncompatible() = runTest {
        enqueueVersion(capabilities = emptyList())

        val result = service.probe(serverUrl)

        assertEquals(ConnectionProbeOutcome.Blocked, result.outcome)
        assertEquals(ConnectionFailureKind.IncompatibleVersion, result.failureKind)
        assertEquals(ConnectionProbeStage.Version, result.lastCompletedStage)
        assertEquals(ServerCapabilities(false, false), result.capabilities)
        assertEquals(1, server.requestCount)
    }

    @Test
    fun phaseOneCapabilitiesRemainPartialEvenWhenHtmlBootstraps() = runTest {
        enqueueVersion(capabilities = listOf("mobileItemResultsV1"))
        enqueueJson(200, """{"code":0,"message":"OK","data":{"status":"Healthy"}}""")
        enqueueHtml(200)

        val result = service.probe(serverUrl)

        assertEquals(ConnectionProbeOutcome.Partial, result.outcome)
        assertEquals(ConnectionFailureKind.IncompatibleVersion, result.failureKind)
        assertTrue(result.capabilities.mobileItemResultsV1)
        assertFalse(result.capabilities.androidEmbedV1)
        assertEquals(ConnectionProbeStage.WebRoot, result.lastCompletedStage)
    }

    @Test
    fun webRoot404IsWrongPathAndPartial() = runTest {
        enqueueVersion(capabilities = listOf("mobileItemResultsV1", "androidEmbedV1"))
        enqueueJson(200, """{"code":0,"message":"OK","data":{"status":"Healthy"}}""")
        server.enqueue(MockResponse().setResponseCode(404))

        val result = service.probe(serverUrl)

        assertEquals(ConnectionProbeOutcome.Partial, result.outcome)
        assertEquals(ConnectionFailureKind.WrongPath, result.failureKind)
        assertEquals(404, result.httpStatus)
        assertEquals(ConnectionProbeStage.AuthenticatedStatus, result.lastCompletedStage)
    }

    @Test
    fun unusableWebRootIsPartial() = runTest {
        enqueueVersion(capabilities = listOf("mobileItemResultsV1", "androidEmbedV1"))
        enqueueJson(200, """{"code":0,"message":"OK","data":{"status":"Healthy"}}""")
        enqueueJson(200, """{"not":"html"}""")

        val result = service.probe(serverUrl)

        assertEquals(ConnectionProbeOutcome.Partial, result.outcome)
        assertEquals(ConnectionFailureKind.Http, result.failureKind)
        assertEquals(ConnectionProbeStage.AuthenticatedStatus, result.lastCompletedStage)
        assertEquals(3, server.requestCount)
    }

    @Test
    fun authenticatedStatus401IsUnauthorizedAndBlocked() = runTest {
        enqueueVersion(capabilities = listOf("mobileItemResultsV1", "androidEmbedV1"))
        server.enqueue(MockResponse().setResponseCode(401))

        val result = service.probe(serverUrl)

        assertEquals(ConnectionProbeOutcome.Blocked, result.outcome)
        assertEquals(ConnectionFailureKind.Unauthorized, result.failureKind)
        assertEquals(401, result.httpStatus)
        assertEquals(ConnectionProbeStage.Version, result.lastCompletedStage)
        assertEquals(2, server.requestCount)
    }

    @Test
    fun tokenlessProbeSkipsAuthenticatedStatusStage() = runTest {
        tokenSource.accessToken = null
        enqueueVersion(capabilities = listOf("mobileItemResultsV1", "androidEmbedV1"))
        enqueueHtml(200)

        val result = service.probe(serverUrl)

        assertEquals(ConnectionProbeOutcome.Reachable, result.outcome)
        assertFalse(result.latencyMillisByStage.containsKey(ConnectionProbeStage.AuthenticatedStatus))
        assertEquals(0, authenticatedModes.size)
        assertEquals(
            listOf("/api/version", "/"),
            List(2) { server.takeRequest().path }
        )
    }

    @Test
    fun tokenBoundToAnotherServerStillAllowsAnonymousStagesAndSkipsAuthenticatedStatus() = runTest {
        tokenSource.serverUrl = "https://server-a.example/api/v1/"
        enqueueVersion(capabilities = listOf("mobileItemResultsV1", "androidEmbedV1"))
        enqueueHtml(200)

        val result = service.probe(serverUrl)

        assertEquals(ConnectionProbeOutcome.Reachable, result.outcome)
        assertFalse(result.latencyMillisByStage.containsKey(ConnectionProbeStage.AuthenticatedStatus))
        assertEquals(0, authenticatedModes.size)
        assertEquals(
            listOf("/api/version", "/"),
            List(2) { server.takeRequest().path }
        )
    }

    @Test
    fun invalidUrlIsBlockedWithoutNetworkEvidenceLeak() = runTest {
        val configuredUrl = "https://user:secret@pim.example/wrong?access=token-secret"

        val result = service.probe(configuredUrl)

        assertEquals(ConnectionProbeOutcome.Blocked, result.outcome)
        assertEquals(ConnectionFailureKind.InvalidUrl, result.failureKind)
        assertNull(result.lastCompletedStage)
        assertTrue(result.latencyMillisByStage.containsKey(ConnectionProbeStage.Url))
        assertEquals(0, server.requestCount)
        assertFalse(result.safeMessage.orEmpty().contains("secret"))
        assertFalse(result.safeMessage.orEmpty().contains(configuredUrl))
    }

    @Test
    fun wallClockChangesDoNotAffectMonotonicStageLatency() = runTest {
        val wallCalls = AtomicInteger(0)
        val monotonicNanos = AtomicLong(0L)
        val throwingClient = OkHttpClient.Builder()
            .addInterceptor(Interceptor { throw ConnectException("offline") })
            .build()
        val clockedService = ConnectionProbeService(
            anonymousClient = throwingClient,
            authenticatedClient = throwingClient,
            tokenSource = FakeProbeTokenSource(null),
            wallClockMillis = {
                if (wallCalls.getAndIncrement() == 0) 10_000L else 1_000L
            },
            monotonicNanos = { monotonicNanos.getAndAdd(7_000_000L) }
        )

        val result = clockedService.probe("https://pim.example/api/v1/")

        assertEquals(10_000L, result.checkedAtUtcMillis)
        assertEquals(7L, result.latencyMillisByStage[ConnectionProbeStage.Url])
        assertEquals(7L, result.latencyMillisByStage[ConnectionProbeStage.Version])
    }

    @Test
    fun probeEvidencePersistsAndExpiresAtExactlyFiveMinutes() {
        val preferences = preferencesContext.getSharedPreferences("probe-test", Context.MODE_PRIVATE)
        preferences.edit().clear().commit()
        val json = Json { ignoreUnknownKeys = true }
        val store = ConnectionProbeStore(preferences, json)
        val result = ConnectionProbeResult(
            outcome = ConnectionProbeOutcome.Reachable,
            checkedAtUtcMillis = 1_000L,
            serverIdentity = "https://pim.example/api/v1/",
            lastCompletedStage = ConnectionProbeStage.WebRoot,
            latencyMillisByStage = mapOf(ConnectionProbeStage.Version to 12L),
            capabilities = ServerCapabilities(true, true)
        )

        store.save(result)

        assertEquals(result, store.result.value)
        assertEquals(result, ConnectionProbeStore(preferences, json).result.value)
        assertTrue(store.isFresh("https://pim.example/api/v1/", 300_999L))
        assertFalse(store.isFresh("https://other.example/api/v1/", 1_001L))
        assertFalse(store.isFresh("https://pim.example/api/v1/", 301_000L))
        assertFalse(store.isFresh("https://pim.example/api/v1/", 999L))
    }

    @Test
    fun corruptProbeEvidenceLoadsAsNullWithoutCrashing() {
        val preferences = preferencesContext.getSharedPreferences("probe-corrupt-test", Context.MODE_PRIVATE)
        preferences.edit()
            .clear()
            .putString("connection_probe_result", "{accessToken:token-secret")
            .commit()

        val store = ConnectionProbeStore(preferences, Json { ignoreUnknownKeys = true })

        assertNull(store.result.value)
        assertFalse(store.isFresh("https://pim.example/api/v1/", 1_000L))
    }

    @Test
    fun concurrentProbesExecuteSequentially() = runTest {
        val firstRequestArrived = CountDownLatch(1)
        val competingRequestArrived = CountDownLatch(1)
        val releaseFirstRequest = CountDownLatch(1)
        val concurrentRequests = AtomicInteger(0)
        val maxConcurrentRequests = AtomicInteger(0)
        val requestNum = AtomicInteger(0)

        val jsonOk: (Int, String) -> MockResponse = { code, body ->
            MockResponse().setResponseCode(code)
                .setHeader("Content-Type", "application/json")
                .setBody(body)
        }
        val htmlOk: (Int) -> MockResponse = { code ->
            MockResponse().setResponseCode(code)
                .setHeader("Content-Type", "text/html; charset=utf-8")
                .setBody("<html><body><div id=\"root\"></div></body></html>")
        }

        server.dispatcher = object : okhttp3.mockwebserver.Dispatcher() {
            override fun dispatch(request: RecordedRequest): MockResponse {
                val n = requestNum.incrementAndGet()
                val active = concurrentRequests.incrementAndGet()
                maxConcurrentRequests.updateAndGet { maxOf(it, active) }
                try {
                    if (n == 1) {
                        firstRequestArrived.countDown()
                        assertTrue(
                            "releaseFirstRequest timed out",
                            releaseFirstRequest.await(10, TimeUnit.SECONDS)
                        )
                    }
                    if (n > 1) {
                        competingRequestArrived.countDown()
                    }
                    return when (request.path) {
                        "/api/version" -> jsonOk(
                            200,
                            """{"version":"1.0","capabilities":["mobileItemResultsV1","androidEmbedV1"]}"""
                        )
                        "/api/v1/status/summary" -> jsonOk(
                            200,
                            """{"code":0,"message":"OK","data":{"status":"Healthy"}}"""
                        )
                        "/" -> htmlOk(200)
                        else -> MockResponse().setResponseCode(404)
                    }
                } finally {
                    concurrentRequests.decrementAndGet()
                }
            }
        }

        val probe1 = async(start = CoroutineStart.UNDISPATCHED) { service.probe(serverUrl) }
        assertTrue(firstRequestArrived.await(5, TimeUnit.SECONDS))

        val probe2 = async(start = CoroutineStart.UNDISPATCHED) { service.probe(serverUrl) }
        try {
            assertFalse(competingRequestArrived.await(200, TimeUnit.MILLISECONDS))
        } finally {
            releaseFirstRequest.countDown()
        }

        val result1 = probe1.await()
        val result2 = probe2.await()

        assertEquals(0, concurrentRequests.get())
        assertEquals(1, maxConcurrentRequests.get())
        assertEquals(ConnectionProbeOutcome.Reachable, result1.outcome)
        assertEquals(ConnectionProbeOutcome.Reachable, result2.outcome)
        assertEquals(6, requestNum.get())
    }

    private fun enqueueVersion(capabilities: List<String>) {
        enqueueJson(
            200,
            """{"version":"1.2.3","capabilities":${Json.encodeToString(capabilities)}}"""
        )
    }

    private fun enqueueJson(code: Int, body: String) {
        server.enqueue(
            MockResponse()
                .setResponseCode(code)
                .setHeader("Content-Type", "application/json")
                .setBody(body)
        )
    }

    private fun enqueueHtml(code: Int) {
        server.enqueue(
            MockResponse()
                .setResponseCode(code)
                .setHeader("Content-Type", "text/html; charset=utf-8")
                .setBody("<html><body><div id=\"root\"></div></body></html>")
        )
    }

    private fun serviceFor(client: OkHttpClient): ConnectionProbeService {
        return ConnectionProbeService(
            anonymousClient = client,
            authenticatedClient = client,
            tokenSource = FakeProbeTokenSource(null),
            wallClockMillis = { 1_000L },
            monotonicNanos = { 0L }
        )
    }

    private data class FakeProbeTokenSource(
        var accessToken: String?,
        var serverUrl: String? = null
    ) : ProbeTokenSource {
        override fun currentAccessToken(serverUrl: String): String? {
            val boundServerUrl = this.serverUrl ?: return accessToken
            val boundIdentity = com.pim.core.settings.PimServerEndpoints.from(boundServerUrl).trustedOrigin
            val requestedIdentity = com.pim.core.settings.PimServerEndpoints.from(serverUrl).trustedOrigin
            return accessToken.takeIf { boundIdentity == requestedIdentity }
        }
    }
}
