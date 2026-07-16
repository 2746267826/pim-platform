package com.pim.app.recovery

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class StartupRecoveryReceiver : BroadcastReceiver() {

    @Inject
    lateinit var runningStateRestorer: RunningStateRestorer

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onReceive(context: Context, intent: Intent) {
        if (!isStartupRecoveryAction(intent.action)) return
        val pendingResult = goAsync()
        scope.launch {
            try {
                dispatchStartupRecovery(intent.action) {
                    runningStateRestorer.ensureRunningState()
                }
            } finally {
                pendingResult.finish()
            }
        }
    }

    companion object {
        internal fun isStartupRecoveryAction(action: String?): Boolean {
            return action == Intent.ACTION_BOOT_COMPLETED || action == Intent.ACTION_MY_PACKAGE_REPLACED
        }

        internal suspend fun dispatchStartupRecovery(
            action: String?,
            recover: suspend () -> Unit
        ): Boolean {
            return if (isStartupRecoveryAction(action)) {
                recover()
                true
            } else {
                false
            }
        }
    }
}
