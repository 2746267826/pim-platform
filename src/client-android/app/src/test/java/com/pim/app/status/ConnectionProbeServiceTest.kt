package com.pim.app.status

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.pim.core.auth.AuthMode
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
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
            nowMillis = { 1_000L }
        )
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    @Test
    fun allSuccessfulStagesWithFutureCapabilitiesAreReachableInExactOrder() = runTest {
        enqueueHealthyApi(capabilities = listOf("mobileItemResultsV1", "androidEmbedV1"))
        enqueueJson(200, """{"code":0,"message":"OK","data":{"status":"Healthy"}}""")
        enqueueHtml(200)
        enqueueHtml(200)

        val result = service.probe(serverUrl)

        assertEquals(ConnectionProbeOutcome.Reachable, result.outcome)
        assertEquals(ConnectionProbeStage.EmbedBootstrap, result.lastCompletedStage)
        assertEquals(ServerCapabilities(mobileItemResultsV1 = true, androidEmbedV1 = true), result.capabilities)
        assertNull(result.failureKind)
        assertEquals(1_000L, result.checkedAtUtcMillis)
        assertEquals(ConnectionProbeStage.entries.toSet(), result.latencyMillisByStage.keys)

        val requests = List(5) { server.takeRequest() }
        assertEquals(
            listOf(
                "/health",
                "/api/version",
                "/api/v1/status/summary",
                "/",
                "/embed/android/today"
            ),
            requests.map { it.path }
        )
        assertNull(requests[0].getHeader("Authorization"))
        assertNull(requests[1].getHeader("Authorization"))
        assertEquals("Bearer probe-access", requests[2].getHeader("Authorization"))
        assertNull(requests[3].getHeader("Authorization"))
        assertNull(requests[4].getHeader("Authorization"))
        assertEquals(List(4) { AuthMode.Anonymous }, anonymousModes)
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
            assertTrue(result.latencyMillisByStage.containsKey(ConnectionProbeStage.Health))
            assertFalse(result.safeMessage.orEmpty().contains("token-secret"))
            assertFalse(result.safeMessage.orEmpty().contains("refresh-secret"))
            assertFalse(result.safeMessage.orEmpty().contains("Authorization"))
        }
    }

    @Test
    fun health404IsWrongPathAndBlocked() = runTest {
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
        enqueueHealthyApi(capabilities = emptyList())

        val result = service.probe(serverUrl)

        assertEquals(ConnectionProbeOutcome.Blocked, result.outcome)
        assertEquals(ConnectionFailureKind.IncompatibleVersion, result.failureKind)
        assertEquals(ConnectionProbeStage.Version, result.lastCompletedStage)
        assertEquals(ServerCapabilities(false, false), result.capabilities)
        assertEquals(2, server.requestCount)
    }

    @Test
    fun phaseOneCapabilitiesRemainPartialEvenWhenHtmlBootstraps() = runTest {
        enqueueHealthyApi(capabilities = listOf("mobileItemResultsV1"))
        enqueueJson(200, """{"code":0,"message":"OK","data":{"status":"Healthy"}}""")
        enqueueHtml(200)
        enqueueHtml(200)

        val result = service.probe(serverUrl)

        assertEquals(ConnectionProbeOutcome.Partial, result.outcome)
        assertEquals(ConnectionFailureKind.IncompatibleVersion, result.failureKind)
        assertTrue(result.capabilities.mobileItemResultsV1)
        assertFalse(result.capabilities.androidEmbedV1)
        assertEquals(ConnectionProbeStage.EmbedBootstrap, result.lastCompletedStage)
    }

    @Test
    fun missingEmbedIsPartialAfterCompatibleApiStages() = runTest {
        enqueueHealthyApi(capabilities = listOf("mobileItemResultsV1", "androidEmbedV1"))
        enqueueJson(200, """{"code":0,"message":"OK","data":{"status":"Healthy"}}""")
        enqueueHtml(200)
        server.enqueue(MockResponse().setResponseCode(404))

        val result = service.probe(serverUrl)

        assertEquals(ConnectionProbeOutcome.Partial, result.outcome)
        assertEquals(ConnectionFailureKind.WrongPath, result.failureKind)
        assertEquals(404, result.httpStatus)
        assertEquals(ConnectionProbeStage.WebRoot, result.lastCompletedStage)
    }

    @Test
    fun unusableWebRootIsPartialAndStopsBeforeEmbed() = runTest {
        enqueueHealthyApi(capabilities = listOf("mobileItemResultsV1", "androidEmbedV1"))
        enqueueJson(200, """{"code":0,"message":"OK","data":{"status":"Healthy"}}""")
        enqueueJson(200, """{"not":"html"}""")

        val result = service.probe(serverUrl)

        assertEquals(ConnectionProbeOutcome.Partial, result.outcome)
        assertEquals(ConnectionFailureKind.Http, result.failureKind)
        assertEquals(ConnectionProbeStage.AuthenticatedStatus, result.lastCompletedStage)
        assertEquals(4, server.requestCount)
    }

    @Test
    fun authenticatedStatus401IsUnauthorizedAndBlocked() = runTest {
        enqueueHealthyApi(capabilities = listOf("mobileItemResultsV1", "androidEmbedV1"))
        server.enqueue(MockResponse().setResponseCode(401))

        val result = service.probe(serverUrl)

        assertEquals(ConnectionProbeOutcome.Blocked, result.outcome)
        assertEquals(ConnectionFailureKind.Unauthorized, result.failureKind)
        assertEquals(401, result.httpStatus)
        assertEquals(ConnectionProbeStage.Version, result.lastCompletedStage)
        assertEquals(3, server.requestCount)
    }

    @Test
    fun tokenlessProbeSkipsAuthenticatedStatusStage() = runTest {
        tokenSource.accessToken = null
        enqueueHealthyApi(capabilities = listOf("mobileItemResultsV1", "androidEmbedV1"))
        enqueueHtml(200)
        enqueueHtml(200)

        val result = service.probe(serverUrl)

        assertEquals(ConnectionProbeOutcome.Reachable, result.outcome)
        assertFalse(result.latencyMillisByStage.containsKey(ConnectionProbeStage.AuthenticatedStatus))
        assertEquals(0, authenticatedModes.size)
        assertEquals(
            listOf("/health", "/api/version", "/", "/embed/android/today"),
            List(4) { server.takeRequest().path }
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
    fun probeEvidencePersistsAndExpiresAtExactlyFiveMinutes() {
        val preferences = preferencesContext.getSharedPreferences("probe-test", Context.MODE_PRIVATE)
        preferences.edit().clear().commit()
        val json = Json { ignoreUnknownKeys = true }
        val store = ConnectionProbeStore(preferences, json)
        val result = ConnectionProbeResult(
            outcome = ConnectionProbeOutcome.Reachable,
            checkedAtUtcMillis = 1_000L,
            lastCompletedStage = ConnectionProbeStage.EmbedBootstrap,
            latencyMillisByStage = mapOf(ConnectionProbeStage.Health to 12L),
            capabilities = ServerCapabilities(true, true)
        )

        store.save(result)

        assertEquals(result, store.result.value)
        assertEquals(result, ConnectionProbeStore(preferences, json).result.value)
        assertTrue(store.isFresh(300_999L))
        assertFalse(store.isFresh(301_000L))
        assertFalse(store.isFresh(999L))
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
        assertFalse(store.isFresh(1_000L))
    }

    private fun enqueueHealthyApi(capabilities: List<String>) {
        enqueueJson(200, """{"status":"healthy"}""")
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
            nowMillis = { 1_000L }
        )
    }

    private data class FakeProbeTokenSource(var accessToken: String?) : ProbeTokenSource {
        override fun currentAccessToken(): String? = accessToken
    }
}
