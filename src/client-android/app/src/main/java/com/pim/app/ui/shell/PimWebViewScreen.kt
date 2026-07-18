package com.pim.app.ui.shell

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.net.http.SslError
import android.util.Log
import android.webkit.SslErrorHandler
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.webkit.WebViewFeature
import com.pim.core.settings.PimServerEndpoints
import com.pim.core.settings.ServerSettingsStore
import java.net.URI

sealed class PimWebViewState {
    object Loading : PimWebViewState()
    data class Content(val url: String) : PimWebViewState()
    data class Error(val reason: String, val isLoginExpired: Boolean = false) : PimWebViewState()

    val isLoading: Boolean get() = this is Loading
    val isContent: Boolean get() = this is Content
    val isError: Boolean get() = this is Error
}

enum class PimWebNavigationAction {
    LoadInWebView,
    OpenInSystemBrowser,
    Block
}

data class ErrorMappingResult(val reason: String, val isLoginExpired: Boolean)

fun extractPath(url: String): String {
    return runCatching { URI(url).path ?: url }.getOrDefault(url)
}

fun extractOrigin(url: String): String? {
    return runCatching {
        val uri = URI(url)
        val scheme = uri.scheme?.lowercase()?.takeIf { it == "http" || it == "https" }
            ?: return null
        val host = uri.host?.lowercase() ?: return null
        val displayHost = when {
            host.startsWith('[') && host.endsWith(']') -> host
            ':' in host -> "[$host]"
            else -> host
        }
        val defaultPort = if (scheme == "https") 443 else 80
        buildString {
            append(scheme)
            append("://")
            append(displayHost)
            if (uri.port > 0 && uri.port != defaultPort) {
                append(':')
                append(uri.port)
            }
        }
    }.getOrNull()
}

internal fun webViewDocumentKey(url: String): String {
    val origin = extractOrigin(url) ?: return url
    return origin + extractPath(url)
}

fun isTrustedEmbedPath(path: String): Boolean {
    return path == "/embed/android/today" || path == "/embed/android/tracks"
}

fun shouldOpenInSystemBrowser(navigationUrl: String, trustedOrigin: String): Boolean {
    val navigationOrigin = extractOrigin(navigationUrl) ?: return true
    val expectedOrigin = extractOrigin(trustedOrigin) ?: return true
    return navigationOrigin != expectedOrigin || !isTrustedEmbedPath(extractPath(navigationUrl))
}

fun decidePimWebNavigation(
    navigationUrl: String,
    trustedOrigin: String?
): PimWebNavigationAction {
    if (trustedOrigin == null) return PimWebNavigationAction.Block
    return if (shouldOpenInSystemBrowser(navigationUrl, trustedOrigin)) {
        PimWebNavigationAction.OpenInSystemBrowser
    } else {
        PimWebNavigationAction.LoadInWebView
    }
}

fun isHttpScheme(url: String): Boolean {
    return runCatching { URI(url).scheme.equals("http", ignoreCase = true) }.getOrDefault(false)
}

fun errorCodeToErrorMessage(errorCode: Int, description: String): ErrorMappingResult {
    return when (errorCode) {
        401, 403 -> ErrorMappingResult("登录已过期", true)
        else -> ErrorMappingResult("加载失败 (HTTP $errorCode: $description)", false)
    }
}

fun webViewInternalErrorToReason(description: String): String {
    return "页面加载失败：$description"
}

internal fun shouldReloadWebView(previousKey: Long, currentKey: Long): Boolean {
    return currentKey != previousKey
}

fun shouldSurfaceSslError(mainFrameUrl: String?, failedUrl: String?): Boolean {
    if (mainFrameUrl.isNullOrBlank() || failedUrl.isNullOrBlank()) return true
    return mainFrameUrl == failedUrl
}

fun safeUrlForLogging(url: String?): String {
    if (url == null) return "<unknown>"
    val origin = extractOrigin(url)
    if (origin != null) {
        return origin + extractPath(url)
    }
    val scheme = runCatching { URI(url).scheme }.getOrNull()
    return if (scheme != null) "$scheme:" else "<unknown>"
}

internal fun matchesTrustedRoute(url: String, serverUrl: String, route: String): Boolean {
    val endpoints = runCatching { PimServerEndpoints.from(serverUrl) }.getOrNull() ?: return false
    val urlOrigin = extractOrigin(url) ?: return false
    if (urlOrigin != endpoints.trustedOrigin) return false
    val expectedPath = extractPath(buildPimWebUrl(serverUrl, route))
    return extractPath(url) == expectedPath
}

fun resolveTracksEmbedUrl(candidate: String?, serverUrl: String): String {
    if (candidate != null && matchesTrustedRoute(candidate, serverUrl, "/embed/android/tracks")) {
        return candidate
    }
    return buildPimWebUrl(serverUrl, "/embed/android/tracks")
}

fun buildPimWebUrl(serverUrl: String, route: String): String {
    val normalizedRoute = route.trim().ifBlank { "/today" }.let { value ->
        if (value.startsWith('/')) value else "/$value"
    }
    val endpoints = runCatching { PimServerEndpoints.from(serverUrl) }.getOrNull()
    if (endpoints != null) {
        val resolved = endpoints.webOrigin.resolve(normalizedRoute)
        if (resolved != null) {
            val base = when (resolved.encodedPath) {
                "/today", "/embed/android/today" -> endpoints.todayEmbedUrl
                "/tracks", "/embed/android/tracks" -> endpoints.tracksEmbedUrl
                else -> resolved
            }
            return base.newBuilder()
                .encodedQuery(resolved.encodedQuery)
                .fragment(resolved.fragment)
                .build()
                .toString()
        }
    }
    return serverUrl.trim().trimEnd('/') + normalizedRoute
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun PimWebViewScreen(
    route: String,
    modifier: Modifier = Modifier,
    serverUrl: String = ServerSettingsStore.DEFAULT_BASE_URL,
    bridge: AndroidWebMessageBridge? = null,
    reloadKey: Long = 0L,
    initialUrl: String? = null,
    onUrlChanged: ((String) -> Unit)? = null
) {
    val context = LocalContext.current
    val currentOnUrlChanged by rememberUpdatedState(onUrlChanged)
    val currentServerUrl by rememberUpdatedState(serverUrl)
    val currentRoute by rememberUpdatedState(route)
    val targetUrl = remember(serverUrl, route, initialUrl) {
        if (initialUrl != null && matchesTrustedRoute(initialUrl, serverUrl, route)) {
            initialUrl
        } else {
            buildPimWebUrl(serverUrl, route)
        }
    }
    val trustedOrigin = remember(serverUrl) {
        runCatching { PimServerEndpoints.from(serverUrl).trustedOrigin }.getOrNull()
    }
    val navigationAction = remember(targetUrl, trustedOrigin) {
        decidePimWebNavigation(targetUrl, trustedOrigin)
    }
    val documentKey = remember(targetUrl) { webViewDocumentKey(targetUrl) }
    val webViewRef = remember { mutableStateOf<WebView?>(null) }
    var externalOpenError by remember(targetUrl) { mutableStateOf<String?>(null) }

    DisposableEffect(bridge, navigationAction, trustedOrigin, documentKey) {
        onDispose {
            webViewRef.value?.let { webView ->
                val lastUrl = webView.url?.takeIf { it.isNotEmpty() } ?: ""
                if (lastUrl.isNotEmpty() && matchesTrustedRoute(lastUrl, currentServerUrl, currentRoute)) {
                    currentOnUrlChanged?.invoke(lastUrl)
                }
                bridge?.remove(webView)
                webView.stopLoading()
                webView.loadUrl("about:blank")
                webView.destroy()
            }
            webViewRef.value = null
        }
    }

    LaunchedEffect(targetUrl, navigationAction) {
        if (navigationAction == PimWebNavigationAction.OpenInSystemBrowser) {
            externalOpenError = openSystemBrowser(context, targetUrl)
        }
    }

    when (navigationAction) {
        PimWebNavigationAction.Block -> {
            PimWebViewMessage(
                modifier = modifier,
                title = "无法加载页面",
                reason = "服务器地址无效或不受支持。"
            )
            return
        }
        PimWebNavigationAction.OpenInSystemBrowser -> {
            PimWebViewMessage(
                modifier = modifier,
                title = if (externalOpenError == null) "已在系统浏览器中打开" else "无法打开页面",
                reason = externalOpenError ?: "此页面不在受信任的嵌入范围内。",
                actionLabel = "再次打开",
                onAction = { externalOpenError = openSystemBrowser(context, targetUrl) }
            )
            return
        }
        PimWebNavigationAction.LoadInWebView -> Unit
    }

    var state by remember(documentKey) { mutableStateOf<PimWebViewState>(PimWebViewState.Loading) }

    val previousReloadKey = remember(documentKey) { mutableStateOf(reloadKey) }
    LaunchedEffect(reloadKey) {
        if (shouldReloadWebView(previousReloadKey.value, reloadKey)) {
            val webView = webViewRef.value
            if (webView != null) {
                state = PimWebViewState.Loading
                webView.reload()
            }
        }
        previousReloadKey.value = reloadKey
    }

    val secureBridgeAvailable = remember(bridge, serverUrl) {
        bridge == null || runCatching {
            WebViewFeature.isFeatureSupported(WebViewFeature.WEB_MESSAGE_LISTENER)
        }.getOrDefault(false)
    }
    var bridgeInstallFailed by remember(bridge, serverUrl) { mutableStateOf(false) }

    if (!secureBridgeAvailable || bridgeInstallFailed) {
        PimWebViewMessage(
            modifier = modifier,
            title = "无法安全加载页面",
            reason = "当前系统 WebView 不支持安全嵌入，请更新 Android System WebView。"
        )
        return
    }
    var httpWarningVisible by remember(documentKey) { mutableStateOf(isHttpScheme(targetUrl)) }

    Column(modifier = modifier) {
        if (httpWarningVisible) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFFFFF3E0))
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Filled.WarningAmber,
                    contentDescription = null,
                    tint = Color(0xFFE65100)
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    text = "连接未加密，数据可能被第三方查看",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color(0xFFE65100)
                )
            }
        }

        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
            key(bridge, trustedOrigin, documentKey) {
                AndroidView(
                    modifier = Modifier.fillMaxSize(),
                    factory = { webContext ->
                        WebView(webContext).apply {
                            settings.javaScriptEnabled = true
                            settings.domStorageEnabled = true
                            settings.databaseEnabled = true
                            webViewClient = createPimWebViewClient(
                                trustedOrigin = trustedOrigin,
                                openExternal = { url -> openSystemBrowser(context, url) },
                                onLoading = { url ->
                                    state = PimWebViewState.Loading
                                    httpWarningVisible = isHttpScheme(url)
                                },
                                onContent = { url ->
                                    state = PimWebViewState.Content(url)
                                    if (matchesTrustedRoute(url, currentServerUrl, currentRoute)) {
                                        currentOnUrlChanged?.invoke(url)
                                    }
                                },
                                onError = { error -> state = error },
                                onHistoryUrlChanged = { url ->
                                    if (matchesTrustedRoute(url, currentServerUrl, currentRoute)) {
                                        currentOnUrlChanged?.invoke(url)
                                    }
                                }
                            )
                            webViewRef.value = this
                            val bridgeInstalled = bridge == null || bridge.install(this)
                            bridgeInstallFailed = !bridgeInstalled
                            if (bridgeInstalled) {
                                loadUrl(targetUrl)
                            } else {
                                webViewRef.value = null
                                destroy()
                            }
                        }
                    },
                    update = { }
                )
            }

            when (val current = state) {
                PimWebViewState.Loading -> PimWebViewLoadingOverlay()
                is PimWebViewState.Error -> PimWebViewErrorOverlay(
                    error = current,
                    onRetry = {
                        state = PimWebViewState.Loading
                        webViewRef.value?.loadUrl(targetUrl)
                    }
                )
                is PimWebViewState.Content -> Unit
            }
        }
    }
}

private fun createPimWebViewClient(
    trustedOrigin: String?,
    openExternal: (String) -> String?,
    onLoading: (String) -> Unit,
    onContent: (String) -> Unit,
    onError: (PimWebViewState.Error) -> Unit,
    onHistoryUrlChanged: ((String) -> Unit)? = null
): WebViewClient {
    return object : WebViewClient() {
        private val TAG = "PimWebView"
        private var mainFrameError = false
        private var currentMainFrameUrl: String? = null

        override fun doUpdateVisitedHistory(view: WebView?, url: String?, isReload: Boolean) {
            onHistoryUrlChanged?.invoke(url.orEmpty())
        }

        override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
            mainFrameError = false
            currentMainFrameUrl = if (url.isNullOrEmpty()) null else url
            onLoading(url.orEmpty())
        }

        override fun onPageFinished(view: WebView?, url: String?) {
            if (!mainFrameError) onContent(url.orEmpty())
        }

        override fun onReceivedError(
            view: WebView?,
            request: WebResourceRequest?,
            error: WebResourceError?
        ) {
            if (request?.isForMainFrame != true) {
                Log.w(TAG, "SubresourceError ${safeUrlForLogging(request?.url?.toString())} ${error?.errorCode} ${error?.description}")
                return
            }
            mainFrameError = true
            onError(
                PimWebViewState.Error(
                    webViewInternalErrorToReason(error?.description?.toString() ?: "未知错误")
                )
            )
        }

        override fun onReceivedHttpError(
            view: WebView?,
            request: WebResourceRequest?,
            errorResponse: WebResourceResponse?
        ) {
            if (request?.isForMainFrame != true) {
                Log.w(TAG, "SubresourceHttpError ${safeUrlForLogging(request?.url?.toString())} ${errorResponse?.statusCode} ${errorResponse?.reasonPhrase}")
                return
            }
            mainFrameError = true
            val code = errorResponse?.statusCode ?: 0
            val mapped = errorCodeToErrorMessage(
                code,
                errorResponse?.reasonPhrase ?: "HTTP $code"
            )
            onError(PimWebViewState.Error(mapped.reason, mapped.isLoginExpired))
        }

        override fun onReceivedSslError(
            view: WebView?,
            handler: SslErrorHandler?,
            error: SslError?
        ) {
            handler?.cancel()
            val failedUrl = error?.url
            if (!shouldSurfaceSslError(currentMainFrameUrl, failedUrl)) {
                Log.w(TAG, "SslWarning ${safeUrlForLogging(failedUrl)} ${error?.primaryError}")
                return
            }
            mainFrameError = true
            val reason = when (error?.primaryError) {
                SslError.SSL_EXPIRED -> "SSL 证书已过期"
                SslError.SSL_NOTYETVALID -> "SSL 证书尚未生效"
                SslError.SSL_UNTRUSTED -> "SSL 证书不受信任"
                SslError.SSL_IDMISMATCH -> "SSL 证书域名不匹配"
                SslError.SSL_DATE_INVALID -> "SSL 证书日期无效"
                else -> "SSL 连接失败"
            }
            onError(PimWebViewState.Error(reason))
        }

        override fun shouldOverrideUrlLoading(
            view: WebView?,
            request: WebResourceRequest?
        ): Boolean {
            if (request?.isForMainFrame != true) return false
            val url = request.url.toString()
            return when (decidePimWebNavigation(url, trustedOrigin)) {
                PimWebNavigationAction.LoadInWebView -> false
                PimWebNavigationAction.OpenInSystemBrowser -> {
                    openExternal(url)?.let { reason ->
                        onError(PimWebViewState.Error("无法打开外部页面：$reason"))
                    }
                    true
                }
                PimWebNavigationAction.Block -> {
                    onError(PimWebViewState.Error("已阻止不受信任的页面导航。"))
                    true
                }
            }
        }
    }
}

private fun openSystemBrowser(context: Context, url: String): String? {
    val exception = runCatching {
        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
    }.exceptionOrNull() ?: return null
    return exception.message ?: "系统浏览器不可用"
}

@Composable
private fun PimWebViewLoadingOverlay() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.92f)),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator()
            Spacer(Modifier.height(12.dp))
            Text("加载中...", style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
private fun PimWebViewErrorOverlay(
    error: PimWebViewState.Error,
    onRetry: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.96f))
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = if (error.isLoginExpired) "登录已过期" else "页面加载失败",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.error
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = error.reason,
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(16.dp))
        Button(onClick = onRetry) {
            Icon(Icons.Filled.Refresh, contentDescription = null)
            Spacer(Modifier.width(6.dp))
            Text("重试")
        }
    }
}

@Composable
private fun PimWebViewMessage(
    modifier: Modifier,
    title: String,
    reason: String,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null
) {
    Column(
        modifier = modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = reason,
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center
        )
        if (actionLabel != null && onAction != null) {
            Spacer(Modifier.height(16.dp))
            Button(onClick = onAction) {
                Icon(Icons.Filled.OpenInBrowser, contentDescription = null)
                Spacer(Modifier.width(6.dp))
                Text(actionLabel)
            }
        }
    }
}
