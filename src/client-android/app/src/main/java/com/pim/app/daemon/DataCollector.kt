package com.pim.app.daemon

import com.pim.app.mobile.sync.MobileSyncCoordinator
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DataCollector @Inject constructor(
    private val mobileSyncCoordinator: MobileSyncCoordinator
) {
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var started = false

    fun start() {
        if (started) return
        started = true

        scope.launch {
            try {
                val state = mobileSyncCoordinator.syncOnOpen()
                Timber.d("Mobile sync-on-open finished: ${state.phase}")
            } catch (e: Exception) {
                Timber.e(e, "Mobile sync-on-open failed")
            }
        }
    }

    fun stop() {
        scope.cancel()
        Timber.d("DataCollector stopped")
    }
}
