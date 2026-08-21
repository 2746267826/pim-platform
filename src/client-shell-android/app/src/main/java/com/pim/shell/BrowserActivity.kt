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

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_browser)

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
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        webView.saveState(outState)
    }

    private fun injectBridge() {
        webView.evaluateJavascript(
            "window.__PIM_SHELL__ = Object.freeze({ version: 1, platform: 'android' });", null
        )
    }

    companion object {
        private const val EXTRA_SERVER_URL = "server_url"
        fun intent(context: Context, serverUrl: String): Intent =
            Intent(context, BrowserActivity::class.java).putExtra(EXTRA_SERVER_URL, serverUrl)
    }
}
