package com.pim.shell

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity

class SetupActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_setup)

        val store = ServerSettingsStore(this)
        val input = findViewById<EditText>(R.id.serverInput)
        val hint = findViewById<TextView>(R.id.hintText)
        val button = findViewById<Button>(R.id.connectButton)
        val checker = HealthChecker { HttpHealthFetcher.fetchStatus(it) }

        button.setOnClickListener {
            button.isEnabled = false
            hint.text = getString(R.string.connecting)
            Thread {
                val normalized = checker.check(input.text.toString())
                runOnUiThread {
                    button.isEnabled = true
                    when {
                        normalized == null -> hint.text = getString(R.string.unreachable)
                        ServerSettingsStore.isInsecure(normalized) -> confirmInsecure(normalized, store)
                        else -> enterBrowser(normalized, store)
                    }
                }
            }.start()
        }
    }

    private fun confirmInsecure(url: String, store: ServerSettingsStore) {
        AlertDialog.Builder(this)
            .setTitle(R.string.insecure_title)
            .setMessage(R.string.insecure_warning)
            .setNegativeButton(android.R.string.cancel, null)
            .setPositiveButton(android.R.string.ok) { _, _ -> enterBrowser(url, store) }
            .show()
    }

    private fun enterBrowser(url: String, store: ServerSettingsStore) {
        store.saveServerUrl(url)
        startActivity(BrowserActivity.intent(this, url))
        finish()
    }
}
