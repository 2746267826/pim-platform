package com.pim.app

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import com.pim.app.location.service.ForegroundLocationController
import com.pim.app.mobile.sync.MobileSyncScheduler
import com.pim.app.ui.root.PimDestination
import com.pim.app.ui.root.PimRootScreen
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    @Inject
    lateinit var mobileSyncScheduler: MobileSyncScheduler

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val initialDestination = when (intent.getStringExtra(ForegroundLocationController.EXTRA_OPEN_DESTINATION)) {
            "status" -> PimDestination.Status
            else -> PimDestination.Today
        }
        setContent { PimRootScreen(initialDestination = initialDestination) }
    }

    override fun onStart() {
        super.onStart()
        mobileSyncScheduler.enqueueNow()
    }
}
