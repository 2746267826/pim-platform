package com.pim.shell

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val serverUrl = ServerSettingsStore(this).serverUrl()
        startActivity(
            if (serverUrl != null) BrowserActivity.intent(this, serverUrl)
            else Intent(this, SetupActivity::class.java)
        )
        finish()
    }
}
