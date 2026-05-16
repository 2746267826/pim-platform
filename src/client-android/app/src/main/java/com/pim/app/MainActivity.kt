package com.pim.app

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.pim.app.daemon.PimDaemonService
import com.pim.app.daemon.DataCollector
import com.pim.app.daemon.scheduleUploadWorker

class MainActivity : AppCompatActivity() {
    private lateinit var collector: DataCollector

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Start daemon service
        startService(Intent(this, PimDaemonService::class.java))

        // Start data collection
        collector = DataCollector(this)
        collector.start()

        // Schedule upload worker
        scheduleUploadWorker(this)

        // Open PIM web UI in browser
        val browserIntent = Intent(Intent.ACTION_VIEW,
            android.net.Uri.parse("http://<NAS_IP>:5000"))
        startActivity(browserIntent)

        finish()
    }

    override fun onDestroy() {
        collector.stop()
        super.onDestroy()
    }
}
