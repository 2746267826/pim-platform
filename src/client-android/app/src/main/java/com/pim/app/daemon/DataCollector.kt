package com.pim.app.daemon

import com.pim.app.mobile.sync.MobileSyncCoordinator
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DataCollector @Inject constructor(
    private val mobileSyncCoordinator: MobileSyncCoordinator
) {
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var syncJob: Job? = null

    fun start() {
        if (syncJob?.isActive == true) return

        syncJob = scope.launch {
            try {
                val state = mobileSyncCoordinator.syncOnOpen()
                Timber.d("Mobile sync-on-open finished: ${state.phase}")
            } catch (e: Exception) {
                Timber.e(e, "Mobile sync-on-open failed")
            }
        }
    }

    fun stop() {
        syncJob?.cancel()
        syncJob = null
        Timber.d("DataCollector stopped")
    }
}
