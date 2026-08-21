package com.pim.shell

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.os.Bundle
import android.view.View
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Button
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity

class BrowserActivity : AppCompatActivity() {
    private lateinit var webView: WebView
    private lateinit var errorOverlay: View
    private var pendingShare: SharePayload? = null

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_browser)

        pendingShare = if (savedInstanceState?.containsKey(KEY_PENDING_SHARE_TEXT) == true) {
            val text = savedInstanceState.getString(KEY_PENDING_SHARE_TEXT)
            if (text != null) SharePayload(text, savedInstanceState.getString(KEY_PENDING_SHARE_URL)) else ShareIntentParser.parse(intent)
        } else {
            ShareIntentParser.parse(intent)
        }

        val serverUrl = intent.getStringExtra(EXTRA_SERVER_URL)
        if (serverUrl == null) { finish(); return }

        webView = findViewById(R.id.webView)
        errorOverlay = findViewById(R.id.errorOverlay)
        webView.settings.javaScriptEnabled = true
        webView.settings.domStorageEnabled = true
        if (BuildConfig.DEBUG) WebView.setWebContentsDebuggingEnabled(true)

        webView.webViewClient = object : WebViewClient() {
            override fun onPageStarted(view: WebView, url: String, favicon: Bitmap?) {
                injectBridge()
            }
            override fun onPageFinished(view: WebView, url: String) {
                pendingShare?.let { dispatchShare(it) }
            }
            override fun onReceivedError(view: WebView, request: WebResourceRequest, error: WebResourceError) {
                if (request.isForMainFrame) errorOverlay.visibility = View.VISIBLE
            }
        }

        findViewById<Button>(R.id.retryButton).setOnClickListener {
            errorOverlay.visibility = View.GONE
            webView.loadUrl(serverUrl)
        }
        findViewById<Button>(R.id.changeServerButton).setOnClickListener {
            startActivity(Intent(this, SetupActivity::class.java))
            finish()
        }

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (webView.canGoBack()) webView.goBack() else moveTaskToBack(true)
            }
        })

        if (savedInstanceState == null) webView.loadUrl(serverUrl)
        else webView.restoreState(savedInstanceState)

        Thread {
            try {
                val u = java.net.URL("${serverUrl.trimEnd('/')}/api/client/shell/latest")
                val conn = u.openConnection() as java.net.HttpURLConnection
                conn.connectTimeout = 3000
                conn.readTimeout = 3000
                val text = conn.inputStream.bufferedReader().readText()
                val json = org.json.JSONObject(text)
                val remote = json.optString("androidVersion").takeIf { it.isNotBlank() }
                val dl = json.optString("androidUrl").takeIf { it.isNotBlank() }
                if (remote != null && dl != null && UpdateChecker.isNewer(BuildConfig.VERSION_NAME, remote)) {
                    runOnUiThread {
                        android.widget.Toast.makeText(this, "发现新版 $remote", android.widget.Toast.LENGTH_LONG).show()
                    }
                }
            } catch (_: Exception) { }
        }.start()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        webView.saveState(outState)
        pendingShare?.let {
            outState.putString(KEY_PENDING_SHARE_TEXT, it.text)
            it.url?.let { url -> outState.putString(KEY_PENDING_SHARE_URL, url) }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        ShareIntentParser.parse(intent)?.let { payload ->
            if (::webView.isInitialized) dispatchShare(payload) else pendingShare = payload
        }
    }

    private fun dispatchShare(payload: SharePayload) {
        val json = ShareIntentParser.toJson(payload)
        webView.evaluateJavascript(
            "(function(p){ window.dispatchEvent(new CustomEvent('pim-shell:share',{detail:p})); })( $json );",
            null
        )
        pendingShare = null
    }

    private fun injectBridge() {
        webView.evaluateJavascript(
            "window.__PIM_SHELL__ = Object.freeze({ version: 1, platform: 'android' });", null
        )
    }

    companion object {
        private const val EXTRA_SERVER_URL = "server_url"
        private const val KEY_PENDING_SHARE_TEXT = "pending_share_text"
        private const val KEY_PENDING_SHARE_URL = "pending_share_url"
        fun intent(context: Context, serverUrl: String): Intent =
            Intent(context, BrowserActivity::class.java).putExtra(EXTRA_SERVER_URL, serverUrl)
    }
}
