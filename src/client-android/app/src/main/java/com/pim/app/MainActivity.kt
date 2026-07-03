package com.pim.app

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.pim.app.daemon.PimDaemonService
import com.pim.app.daemon.DataCollector
import com.pim.app.daemon.scheduleUploadWorker
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {
    @Inject lateinit var collector: DataCollector

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Start daemon service (foreground on API 26+)
        val intent = Intent(this, PimDaemonService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent)
        } else {
            startService(intent)
        }

        // Start data collection
        collector.start()

        // Schedule upload worker
        scheduleUploadWorker(this)

        // Open PIM web UI — configure server URL in settings
        val serverUrl = getSharedPreferences("pim", MODE_PRIVATE)
            .getString("server_url", null)
        if (serverUrl != null) {
            startActivity(Intent(Intent.ACTION_VIEW, android.net.Uri.parse(serverUrl)))
        } else {
            Toast.makeText(this, "PIM 后台服务已启动，请在设置中配置服务器地址", Toast.LENGTH_LONG).show()
        }

        finish()
    }

    override fun onDestroy() {
        if (::collector.isInitialized) {
            collector.stop()
        }
        super.onDestroy()
    }
}
