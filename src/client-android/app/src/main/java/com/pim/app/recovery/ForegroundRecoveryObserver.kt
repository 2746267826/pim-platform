package com.pim.app.recovery

import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

internal class ForegroundRecoveryObserver(
    private val scope: CoroutineScope,
    private val enqueueImmediateSync: () -> Unit = {},
    private val reportSyncFailure: (Exception) -> Unit = {},
    private val recover: suspend () -> Unit
) : DefaultLifecycleObserver {

    override fun onStart(owner: LifecycleOwner) {
        try {
            enqueueImmediateSync()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            reportSyncFailure(e)
        }
        scope.launch { recover() }
    }
}
