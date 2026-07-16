package com.pim.app.ui.shell

import android.annotation.SuppressLint
import android.webkit.WebView
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.webkit.WebViewFeature
import com.pim.core.settings.PimServerEndpoints
import com.pim.core.settings.ServerSettingsStore

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun PimWebViewScreen(
    route: String,
    modifier: Modifier = Modifier,
    serverUrl: String = ServerSettingsStore.DEFAULT_BASE_URL,
    bridge: AndroidWebMessageBridge? = null
) {
    val targetUrl = buildPimWebUrl(serverUrl, route)
    val webViewRef = remember { mutableStateOf<WebView?>(null) }
    val bridgeInstallFailed = remember(bridge, serverUrl) { mutableStateOf(false) }
    val secureBridgeAvailable = remember(bridge, serverUrl) {
        if (bridge == null) {
            true
        } else {
            runCatching {
                WebViewFeature.isFeatureSupported(WebViewFeature.WEB_MESSAGE_LISTENER) &&
                    PimServerEndpoints.from(serverUrl).trustedOrigin.isNotBlank()
            }.getOrDefault(false)
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            val webView = webViewRef.value
            if (webView != null) {
                bridge?.remove(webView)
                webView.loadUrl("about:blank")
                webView.destroy()
            }
        }
    }

    if (bridge != null && (!secureBridgeAvailable || bridgeInstallFailed.value)) {
        Box(
            modifier = modifier.fillMaxSize().padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "当前系统 WebView 不支持或未能建立安全消息桥，无法加载受保护页面。",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.error
            )
        }
        return
    }

    AndroidView(
        modifier = modifier,
        factory = { context ->
            WebView(context).apply {
                settings.javaScriptEnabled = true
                settings.domStorageEnabled = true
                settings.databaseEnabled = true
                if (bridge != null) {
                    val installed = bridge.install(this)
                    bridgeInstallFailed.value = !installed
                    if (installed) {
                        loadUrl(targetUrl)
                    }
                } else {
                    loadUrl(targetUrl)
                }
                webViewRef.value = this
            }
        },
        update = { webView ->
            if (bridge != null && bridgeInstallFailed.value) {
                return@AndroidView
            }
            if (webView.url != targetUrl) {
                webView.loadUrl(targetUrl)
            }
        }
    )
}

fun buildPimWebUrl(serverUrl: String, route: String): String {
    val normalizedRoute = route.ifBlank { "/today" }.let { value ->
        if (value.startsWith("/")) value else "/$value"
    }
    val endpoints = runCatching { PimServerEndpoints.from(serverUrl) }.getOrNull()
    if (endpoints != null) {
        return endpoints.webOrigin.newBuilder()
            .encodedPath(normalizedRoute)
            .build()
            .toString()
    }
    val root = serverUrl.trim().trimEnd('/')
    return "$root$normalizedRoute"
}
