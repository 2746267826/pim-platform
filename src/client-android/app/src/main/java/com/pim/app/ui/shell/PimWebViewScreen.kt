package com.pim.app.ui.shell

import android.annotation.SuppressLint
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import com.pim.core.settings.ServerSettingsStore

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun PimWebViewScreen(
    route: String,
    modifier: Modifier = Modifier,
    serverUrl: String = ServerSettingsStore.DEFAULT_BASE_URL,
    authToken: String? = null
) {
    val targetUrl = buildPimWebUrl(serverUrl, route)
    AndroidView(
        modifier = modifier,
        factory = { context ->
            WebView(context).apply {
                webViewClient = object : WebViewClient() {
                    override fun onPageFinished(view: WebView, url: String) {
                        if (!authToken.isNullOrBlank()) {
                            view.evaluateJavascript(
                                "localStorage.setItem('accessToken', ${authToken.toJsString()});",
                                null
                            )
                        }
                    }
                }
                settings.javaScriptEnabled = true
                settings.domStorageEnabled = true
                settings.databaseEnabled = true
                loadUrl(targetUrl)
            }
        },
        update = { webView ->
            if (webView.url != targetUrl) {
                webView.loadUrl(targetUrl)
            }
        }
    )
}

fun buildPimWebUrl(serverUrl: String, route: String): String {
    val root = serverUrl.trim().trimEnd('/')
    val normalizedRoute = route.ifBlank { "/today" }.trimStart('/')
    return "$root/$normalizedRoute"
}

private fun String.toJsString(): String {
    return "\"" + replace("\\", "\\\\").replace("\"", "\\\"") + "\""
}
