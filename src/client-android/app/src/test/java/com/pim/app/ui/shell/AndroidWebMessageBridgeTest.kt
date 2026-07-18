package com.pim.app.ui.shell

import android.content.Context
import android.net.Uri
import java.io.File
import android.webkit.WebView
import androidx.test.core.app.ApplicationProvider
import androidx.webkit.JavaScriptReplyProxy
import androidx.webkit.WebMessageCompat
import androidx.webkit.WebViewCompat
import com.pim.core.auth.AuthRefreshOperation
import com.pim.core.auth.AuthRefreshResult
import com.pim.core.auth.AuthSessionSnapshot
import com.pim.core.auth.AuthSessionStore
import com.pim.core.auth.AuthTokens
import com.pim.core.network.AuthRefreshCoordinator
import com.pim.core.settings.ServerSettingsStore
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.boolean
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class AndroidWebMessageBridgeTest {

    private lateinit var context: Context
    private val json = Json { ignoreUnknownKeys = true }

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        context.getSharedPreferences("pim_server_settings", Context.MODE_PRIVATE)
            .edit().clear().commit()
    }

    @Test
    fun `token request returns access token for valid session`() = runBlocking {
        val authStore = TestAuthSessionStore()
        authStore.save(ACCESS_TOKEN, REFRESH_TOKEN, FAR_FUTURE, TRUSTED_ORIGIN)
        val settings = ServerSettingsStore(context, authStore)
        settings.setBaseUrl(SERVER_URL)
        val bridge = createBridge(authStore = authStore, settings = settings)

        val response = bridge.handleMessage(
            """{"version":1,"id":"req1","type":"token.request"}""",
            TRUSTED_ORIGIN,
            true
        )

        val obj = parse(response)
        assertEquals("req1", obj["id"]?.jsonPrimitive?.contentOrNull)
        assertTrue(obj["ok"]?.jsonPrimitive?.boolean ?: false)
        assertEquals(ACCESS_TOKEN, obj["accessToken"]?.jsonPrimitive?.contentOrNull)
        assertNull(obj["refreshToken"])
        assertNull(obj["errorCode"])
    }

    @Test
    fun `token request without session returns login_expired`() = runBlocking {
        val authStore = TestAuthSessionStore()
        val settings = ServerSettingsStore(context, authStore)
        settings.setBaseUrl(SERVER_URL)
        val bridge = createBridge(authStore = authStore, settings = settings)

        val response = bridge.handleMessage(
            """{"version":1,"id":"req2","type":"token.request"}""",
            TRUSTED_ORIGIN,
            true
        )

        val obj = parse(response)
        assertEquals("req2", obj["id"]?.jsonPrimitive?.contentOrNull)
        assertFalse(obj["ok"]?.jsonPrimitive?.boolean ?: true)
        assertEquals("login_expired", obj["errorCode"]?.jsonPrimitive?.contentOrNull)
        assertNull(obj["accessToken"])
    }

    @Test
    fun `token request triggers only one refresh when token is expired`() = runBlocking {
        val authStore = TestAuthSessionStore()
        authStore.save(ACCESS_TOKEN, REFRESH_TOKEN, 100L, TRUSTED_ORIGIN)
        val now = { 200L }
        var refreshCount = 0
        val operation = AuthRefreshOperation { _, _ ->
            refreshCount++
            AuthRefreshResult.Success(AuthTokens("refreshed-access", "refreshed-refresh", FAR_FUTURE))
        }
        val coordinator = AuthRefreshCoordinator(authStore, operation, nowMillis = now)
        val settings = ServerSettingsStore(context, authStore)
        settings.setBaseUrl(SERVER_URL)
        val bridge = createBridge(authStore = authStore, refreshCoordinator = coordinator, settings = settings)

        val response = bridge.handleMessage(
            """{"version":1,"id":"req3","type":"token.request"}""",
            TRUSTED_ORIGIN,
            true
        )

        val obj = parse(response)
        assertEquals(1, refreshCount)
        assertEquals("req3", obj["id"]?.jsonPrimitive?.contentOrNull)
        assertTrue(obj["ok"]?.jsonPrimitive?.boolean ?: false)
        assertEquals("refreshed-access", obj["accessToken"]?.jsonPrimitive?.contentOrNull)
    }

    @Test
    fun `token refresh succeeds`() = runBlocking {
        val authStore = TestAuthSessionStore()
        authStore.save(ACCESS_TOKEN, REFRESH_TOKEN, FAR_FUTURE, TRUSTED_ORIGIN)
        val operation = AuthRefreshOperation { _, _ ->
            AuthRefreshResult.Success(AuthTokens("refreshed-access", "refreshed-refresh", FAR_FUTURE))
        }
        val coordinator = AuthRefreshCoordinator(authStore, operation, nowMillis = { 0L })
        val settings = ServerSettingsStore(context, authStore)
        settings.setBaseUrl(SERVER_URL)
        val bridge = createBridge(authStore = authStore, refreshCoordinator = coordinator, settings = settings)

        val response = bridge.handleMessage(
            """{"version":1,"id":"req4","type":"token.refresh","failedAccessToken":"$ACCESS_TOKEN"}""",
            TRUSTED_ORIGIN,
            true
        )

        val obj = parse(response)
        assertEquals("req4", obj["id"]?.jsonPrimitive?.contentOrNull)
        assertTrue(obj["ok"]?.jsonPrimitive?.boolean ?: false)
        assertEquals("refreshed-access", obj["accessToken"]?.jsonPrimitive?.contentOrNull)
        assertNull(obj["refreshToken"])
    }

    @Test
    fun `token refresh when rejected returns login_expired`() = runBlocking {
        val authStore = TestAuthSessionStore()
        authStore.save(ACCESS_TOKEN, REFRESH_TOKEN, FAR_FUTURE, TRUSTED_ORIGIN)
        val operation = AuthRefreshOperation { _, _ -> AuthRefreshResult.Rejected }
        val coordinator = AuthRefreshCoordinator(authStore, operation, nowMillis = { 0L })
        val settings = ServerSettingsStore(context, authStore)
        settings.setBaseUrl(SERVER_URL)
        val bridge = createBridge(authStore = authStore, refreshCoordinator = coordinator, settings = settings)

        val response = bridge.handleMessage(
            """{"version":1,"id":"req5","type":"token.refresh","failedAccessToken":"$ACCESS_TOKEN"}""",
            TRUSTED_ORIGIN,
            true
        )

        val obj = parse(response)
        assertEquals("req5", obj["id"]?.jsonPrimitive?.contentOrNull)
        assertFalse(obj["ok"]?.jsonPrimitive?.boolean ?: true)
        assertEquals("login_expired", obj["errorCode"]?.jsonPrimitive?.contentOrNull)
        assertNull(obj["accessToken"])
    }

    @Test
    fun `request from wrong origin returns server_mismatch`() = runBlocking {
        val authStore = TestAuthSessionStore()
        authStore.save(ACCESS_TOKEN, REFRESH_TOKEN, FAR_FUTURE, TRUSTED_ORIGIN)
        val settings = ServerSettingsStore(context, authStore)
        settings.setBaseUrl(SERVER_URL)
        val bridge = createBridge(authStore = authStore, settings = settings)

        val response = bridge.handleMessage(
            """{"version":1,"id":"req6","type":"token.request"}""",
            "https://evil.example",
            true
        )

        val obj = parse(response)
        assertEquals("req6", obj["id"]?.jsonPrimitive?.contentOrNull)
        assertFalse(obj["ok"]?.jsonPrimitive?.boolean ?: true)
        assertEquals("server_mismatch", obj["errorCode"]?.jsonPrimitive?.contentOrNull)
        assertNull(obj["accessToken"])
    }

    @Test
    fun `request without configured server returns server_mismatch`() = runBlocking {
        val authStore = TestAuthSessionStore()
        val emptySettings = ServerSettingsStore(context, authStore)
        val bridge = createBridge(authStore = authStore, settings = emptySettings)

        val response = bridge.handleMessage(
            """{"version":1,"id":"req7","type":"token.request"}""",
            "https://any.example",
            true
        )

        val obj = parse(response)
        assertEquals("req7", obj["id"]?.jsonPrimitive?.contentOrNull)
        assertFalse(obj["ok"]?.jsonPrimitive?.boolean ?: true)
        assertEquals("server_mismatch", obj["errorCode"]?.jsonPrimitive?.contentOrNull)
    }

    @Test
    fun `malformed json returns invalid_request`() = runBlocking {
        val authStore = TestAuthSessionStore()
        val settings = ServerSettingsStore(context, authStore)
        settings.setBaseUrl(SERVER_URL)
        val bridge = createBridge(authStore = authStore, settings = settings)

        val response = bridge.handleMessage(
            "not json at all",
            TRUSTED_ORIGIN,
            true
        )

        val obj = parse(response)
        assertFalse(obj["ok"]?.jsonPrimitive?.boolean ?: true)
        assertEquals("invalid_request", obj["errorCode"]?.jsonPrimitive?.contentOrNull)
    }

    @Test
    fun `missing version field returns invalid_request`() = runBlocking {
        val bridge = createBridge()

        val response = bridge.handleMessage(
            """{"id":"req","type":"token.request"}""",
            TRUSTED_ORIGIN,
            true
        )

        val obj = parse(response)
        assertEquals("invalid_request", obj["errorCode"]?.jsonPrimitive?.contentOrNull)
    }

    @Test
    fun `unsupported protocol version returns invalid_request`() = runBlocking {
        val bridge = createBridge()

        val response = bridge.handleMessage(
            """{"version":2,"id":"req","type":"token.request"}""",
            TRUSTED_ORIGIN,
            true
        )

        val obj = parse(response)
        assertEquals("invalid_request", obj["errorCode"]?.jsonPrimitive?.contentOrNull)
    }

    @Test
    fun `missing id field returns invalid_request`() = runBlocking {
        val bridge = createBridge()

        val response = bridge.handleMessage(
            """{"version":1,"type":"token.request"}""",
            TRUSTED_ORIGIN,
            true
        )

        val obj = parse(response)
        assertEquals("invalid_request", obj["errorCode"]?.jsonPrimitive?.contentOrNull)
    }

    @Test
    fun `missing type field returns invalid_request`() = runBlocking {
        val bridge = createBridge()

        val response = bridge.handleMessage(
            """{"version":1,"id":"req"}""",
            TRUSTED_ORIGIN,
            true
        )

        val obj = parse(response)
        assertEquals("invalid_request", obj["errorCode"]?.jsonPrimitive?.contentOrNull)
    }

    @Test
    fun `blank id returns invalid_request`() = runBlocking {
        val bridge = createBridge()

        val response = bridge.handleMessage(
            """{"version":1,"id":"   ","type":"token.request"}""",
            TRUSTED_ORIGIN,
            true
        )

        val obj = parse(response)
        assertEquals("invalid_request", obj["errorCode"]?.jsonPrimitive?.contentOrNull)
    }

    @Test
    fun `blank type returns invalid_request`() = runBlocking {
        val bridge = createBridge()

        val response = bridge.handleMessage(
            """{"version":1,"id":"req","type":""}""",
            TRUSTED_ORIGIN,
            true
        )

        val obj = parse(response)
        assertEquals("invalid_request", obj["errorCode"]?.jsonPrimitive?.contentOrNull)
    }

    @Test
    fun `unknown type returns invalid_request`() = runBlocking {
        val bridge = createBridge()

        val response = bridge.handleMessage(
            """{"version":1,"id":"req","type":"unknown.type"}""",
            TRUSTED_ORIGIN,
            true
        )

        val obj = parse(response)
        assertEquals("invalid_request", obj["errorCode"]?.jsonPrimitive?.contentOrNull)
    }

    @Test
    fun `non main frame returns invalid_request`() = runBlocking {
        val authStore = TestAuthSessionStore()
        authStore.save(ACCESS_TOKEN, REFRESH_TOKEN, FAR_FUTURE, TRUSTED_ORIGIN)
        val settings = ServerSettingsStore(context, authStore)
        settings.setBaseUrl(SERVER_URL)
        val bridge = createBridge(authStore = authStore, settings = settings)

        val response = bridge.handleMessage(
            """{"version":1,"id":"req","type":"token.request"}""",
            TRUSTED_ORIGIN,
            false
        )

        val obj = parse(response)
        assertEquals("invalid_request", obj["errorCode"]?.jsonPrimitive?.contentOrNull)
    }

    @Test
    fun `native state request returns lightweight snapshot`() = runBlocking {
        val bridge = createBridge(
            nativeStateProvider = {
                mapOf(
                    "collectionMode" to "foreground",
                    "uploading" to true,
                    "pending" to 5,
                    "secret" to "must-not-cross"
                )
            }
        )

        val response = bridge.handleMessage(
            """{"version":1,"id":"req","type":"native.state.request"}""",
            TRUSTED_ORIGIN,
            true
        )

        val obj = parse(response)
        assertTrue(obj["ok"]?.jsonPrimitive?.boolean ?: false)
        val state = obj["nativeState"]?.jsonObject
        assertNotNull(state)
        assertEquals("foreground", state?.get("collectionMode")?.jsonPrimitive?.contentOrNull)
        assertEquals(true, state?.get("uploading")?.jsonPrimitive?.boolean)
        assertEquals(5, state?.get("pending")?.jsonPrimitive?.int)
        assertNull(state?.get("secret"))
    }

    @Test
    fun `native state provider failure returns a visible error`() = runBlocking {
        val bridge = createBridge(
            nativeStateProvider = { error("state unavailable") }
        )

        val response = bridge.handleMessage(
            """{"version":1,"id":"req","type":"native.state.request"}""",
            TRUSTED_ORIGIN,
            true
        )

        val obj = parse(response)
        assertFalse(obj["ok"]?.jsonPrimitive?.boolean ?: true)
        assertEquals("native_state_unavailable", obj["errorCode"]?.jsonPrimitive?.contentOrNull)
    }

    @Test
    fun `page report accepts only whitelist fields`() = runBlocking {
        var capturedReport: Map<String, String?>? = null
        val bridge = createBridge(
            pageReportSink = { capturedReport = it }
        )

        val response = bridge.handleMessage(
            """{"version":1,"id":"req","type":"page.report","report":{"hasServerData":true,"generatedAt":"2025-01-01T00:00:00Z","error":null,"arbitraryField":"should be ignored"}}""",
            TRUSTED_ORIGIN,
            true
        )

        val obj = parse(response)
        assertTrue(obj["ok"]?.jsonPrimitive?.boolean ?: false)
        assertNotNull(capturedReport)
        assertEquals("true", capturedReport?.get("hasServerData"))
        assertEquals("2025-01-01T00:00:00Z", capturedReport?.get("generatedAt"))
        assertNull(capturedReport?.get("error"))
        assertFalse(capturedReport?.containsKey("arbitraryField") ?: true)
    }

    @Test
    fun `response never contains refresh token`() = runBlocking {
        val authStore = TestAuthSessionStore()
        authStore.save(ACCESS_TOKEN, REFRESH_TOKEN, FAR_FUTURE, TRUSTED_ORIGIN)
        val settings = ServerSettingsStore(context, authStore)
        settings.setBaseUrl(SERVER_URL)
        val bridge = createBridge(authStore = authStore, settings = settings)

        val response = bridge.handleMessage(
            """{"version":1,"id":"req","type":"token.request"}""",
            TRUSTED_ORIGIN,
            true
        )
        assertNull(parse(response)["refreshToken"])

        val operation = AuthRefreshOperation { _, _ ->
            AuthRefreshResult.Success(AuthTokens("new-access", "new-refresh", FAR_FUTURE))
        }
        val coordinator = AuthRefreshCoordinator(authStore, operation, nowMillis = { 0L })
        val bridge2 = createBridge(authStore = authStore, refreshCoordinator = coordinator, settings = settings)

        val response2 = bridge2.handleMessage(
            """{"version":1,"id":"req","type":"token.refresh","failedAccessToken":"$ACCESS_TOKEN"}""",
            TRUSTED_ORIGIN,
            true
        )
        assertNull(parse(response2)["refreshToken"])
    }

    @Test
    fun `install returns false when web message listener is unsupported`() {
        val bridge = createBridge()
        val webView = WebView(context)

        val installed = bridge.install(webView, isFeatureSupported = { false })

        assertFalse(installed)
    }

    @Test
    fun `install returns true when web message listener is supported`() {
        val bridge = createBridge()
        val webView = WebView(context)

        val installed = bridge.install(
            webView,
            isFeatureSupported = { true },
            addListener = { _, _, _, _ -> }
        )

        assertTrue(installed)
    }

    @Test
    fun `install returns false when listener installation fails`() {
        val bridge = createBridge()
        val webView = WebView(context)

        val installed = bridge.install(
            webView,
            isFeatureSupported = { true },
            addListener = { _, _, _, _ -> throw IllegalStateException("listener unavailable") }
        )

        assertFalse(installed)
    }

    @Test
    fun `token request from non-matching origin after server switch returns server_mismatch`() = runBlocking {
        val authStore = TestAuthSessionStore()
        authStore.save(ACCESS_TOKEN, REFRESH_TOKEN, FAR_FUTURE, TRUSTED_ORIGIN)
        val settings = ServerSettingsStore(context, authStore)
        settings.setBaseUrl(SERVER_URL)
        val bridge = createBridge(authStore = authStore, settings = settings)

        val response = bridge.handleMessage(
            """{"version":1,"id":"req","type":"token.request"}""",
            "https://other-server.example",
            true
        )

        val obj = parse(response)
        assertEquals("server_mismatch", obj["errorCode"]?.jsonPrimitive?.contentOrNull)
    }

    @Test
    fun `reply failure after web view destruction is contained`() {
        var captured: Throwable? = null
        val scope = CoroutineScope(
            SupervisorJob() + Dispatchers.Unconfined +
                CoroutineExceptionHandler { _, throwable -> captured = throwable }
        )

        val authStore = TestAuthSessionStore()
        authStore.save(ACCESS_TOKEN, REFRESH_TOKEN, FAR_FUTURE, TRUSTED_ORIGIN)
        val settings = ServerSettingsStore(context, authStore)
        settings.setBaseUrl(SERVER_URL)
        val bridge = createBridge(scope = scope, authStore = authStore, settings = settings)

        val webView = WebView(context)
        var listener: WebViewCompat.WebMessageListener? = null
        bridge.install(
            webView,
            isFeatureSupported = { true },
            addListener = { _, _, _, l -> listener = l }
        )

        val message = WebMessageCompat(
            """{"version":1,"id":"req","type":"token.request"}"""
        )
        val replyProxy = object : JavaScriptReplyProxy() {
            override fun postMessage(message: String) {
                throw IllegalStateException("webview destroyed")
            }

            override fun postMessage(message: ByteArray) {
                throw IllegalStateException("webview destroyed")
            }
        }

        listener?.onPostMessage(webView, message, Uri.parse(TRUSTED_ORIGIN), true, replyProxy)

        scope.cancel()

        assertNull(captured)
    }

    @Test
    fun `token request refresh exception returns auth_unavailable`() = runBlocking {
        val authStore = TestAuthSessionStore()
        authStore.save(ACCESS_TOKEN, REFRESH_TOKEN, 100L, TRUSTED_ORIGIN)
        val operation = AuthRefreshOperation { _, _ -> throw IllegalStateException("network down") }
        val coordinator = AuthRefreshCoordinator(authStore, operation, nowMillis = { 200L })
        val settings = ServerSettingsStore(context, authStore)
        settings.setBaseUrl(SERVER_URL)
        val bridge = createBridge(authStore = authStore, refreshCoordinator = coordinator, settings = settings)

        val outcome = runCatching {
            bridge.handleMessage(
                """{"version":1,"id":"req-auth-unavailable","type":"token.request"}""",
                TRUSTED_ORIGIN,
                true
            )
        }
        assertTrue(outcome.isSuccess)
        val obj = parse(outcome.getOrThrow())
        assertEquals("req-auth-unavailable", obj["id"]?.jsonPrimitive?.contentOrNull)
        assertFalse(obj["ok"]?.jsonPrimitive?.boolean ?: true)
        assertEquals("auth_unavailable", obj["errorCode"]?.jsonPrimitive?.contentOrNull)
        assertNull(obj["accessToken"])
        assertNull(obj["refreshToken"])
    }

    @Test
    fun `token request cancellation still propagates`() = runBlocking {
        val authStore = TestAuthSessionStore()
        authStore.save(ACCESS_TOKEN, REFRESH_TOKEN, 100L, TRUSTED_ORIGIN)
        val operation = AuthRefreshOperation { _, _ -> throw CancellationException("cancelled") }
        val coordinator = AuthRefreshCoordinator(authStore, operation, nowMillis = { 200L })
        val settings = ServerSettingsStore(context, authStore)
        settings.setBaseUrl(SERVER_URL)
        val bridge = createBridge(authStore = authStore, refreshCoordinator = coordinator, settings = settings)

        val outcome = runCatching {
            bridge.handleMessage(
                """{"version":1,"id":"req-cancel","type":"token.request"}""",
                TRUSTED_ORIGIN,
                true
            )
        }
        assertTrue(outcome.isFailure)
        assertTrue(outcome.exceptionOrNull() is CancellationException)
    }

    // --- helpers ---

    private fun parse(raw: String): JsonObject {
        return json.parseToJsonElement(raw).jsonObject
    }

    private fun createBridge(
        authStore: AuthSessionStore = TestAuthSessionStore(),
        refreshCoordinator: AuthRefreshCoordinator? = null,
        settings: ServerSettingsStore? = null,
        nativeStateProvider: (() -> Map<String, Any?>)? = null,
        pageReportSink: ((Map<String, String?>) -> Unit)? = null,
        scope: CoroutineScope = CoroutineScope(Dispatchers.Unconfined)
    ): AndroidWebMessageBridge {
        val resolvedSettings = settings ?: ServerSettingsStore(context, authStore).also {
            it.setBaseUrl(SERVER_URL)
        }
        val coord = refreshCoordinator ?: AuthRefreshCoordinator(
            authStore,
            AuthRefreshOperation { _, _ -> error("refresh not expected") },
            nowMillis = { 0L }
        )
        return AndroidWebMessageBridge(
            authSessionStore = authStore,
            refreshCoordinator = coord,
            serverSettingsStore = resolvedSettings,
            scope = scope,
            nativeStateProvider = nativeStateProvider,
            pageReportSink = pageReportSink
        )
    }

    // --- source contract: remove(webView) must not be gated by a shared marker ---

    @Test
    fun `AndroidWebMessageBridge does not use installedObjectName field for cleanup`() {
        val bridgeFile = repoFile(
            "src", "main", "java", "com", "pim", "app", "ui", "shell", "AndroidWebMessageBridge.kt"
        )
        val source = bridgeFile.readText()
        assertFalse(
            "installedObjectName is a shared mutable marker; remove(webView) must " +
                "always best-effort remove JS_OBJECT_NAME from the supplied WebView.",
            source.contains("installedObjectName")
        )
    }

    private fun repoFile(vararg parts: String): File {
        var current: File? = File("").canonicalFile
        while (current != null) {
            val candidate = parts.fold(current) { dir, part -> dir.resolve(part) }
            if (candidate.exists()) return candidate
            current = current.parentFile
        }
        error("Could not find ${parts.joinToString(File.separator)}")
    }

    private class TestAuthSessionStore : AuthSessionStore {
        private var snapshot: AuthSessionSnapshot = AuthSessionSnapshot(null)

        override fun snapshot(): AuthSessionSnapshot = snapshot

        override fun save(
            accessToken: String,
            refreshToken: String,
            expiresAtUtcMillis: Long,
            serverIdentity: String
        ): Boolean {
            snapshot = AuthSessionSnapshot(
                tokens = AuthTokens(accessToken, refreshToken, expiresAtUtcMillis),
                serverIdentity = serverIdentity
            )
            return true
        }

        override fun clear(): Boolean {
            snapshot = AuthSessionSnapshot(null)
            return true
        }
    }

    companion object {
        const val TRUSTED_ORIGIN = "https://pim.example"
        const val SERVER_URL = "https://pim.example/api/v1/"
        const val ACCESS_TOKEN = "access-token-valid"
        const val REFRESH_TOKEN = "refresh-token-valid"
        const val FAR_FUTURE = Long.MAX_VALUE
    }
}
