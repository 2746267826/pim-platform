package com.pim.app

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.pim.app.location.service.ForegroundLocationController
import com.pim.app.mobile.sync.MobileSyncScheduler
import com.pim.app.recovery.ForegroundRecoveryObserver
import com.pim.app.recovery.RunningStateRestorer
import com.pim.app.ui.root.PimDestination
import com.pim.app.ui.root.PimRootScreen
import dagger.hilt.android.AndroidEntryPoint
import timber.log.Timber
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    @Inject
    lateinit var mobileSyncScheduler: MobileSyncScheduler

    @Inject
    lateinit var runningStateRestorer: RunningStateRestorer

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val initialDestination = when (intent.getStringExtra(ForegroundLocationController.EXTRA_OPEN_DESTINATION)) {
            "status" -> PimDestination.Status
            else -> PimDestination.Today
        }
        setContent { PimRootScreen(initialDestination = initialDestination) }
        lifecycle.addObserver(
            ForegroundRecoveryObserver(
                scope = lifecycleScope,
                enqueueImmediateSync = { mobileSyncScheduler.enqueueNow() },
                reportSyncFailure = { Timber.e(it, "前台即时同步调度失败") },
                recover = { runningStateRestorer.ensureRunningState() }
            )
        )
    }
}
