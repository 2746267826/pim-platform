package com.pim.app

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import com.pim.app.daemon.DataCollector
import com.pim.app.ui.PimAppScaffold
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {
    @Inject lateinit var collector: DataCollector

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        collector.start()

        setContent {
            PimAppScaffold()
        }
    }

    override fun onDestroy() {
        if (::collector.isInitialized) {
            collector.stop()
        }
        super.onDestroy()
    }
}
