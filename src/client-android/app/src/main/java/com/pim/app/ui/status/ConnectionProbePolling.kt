package com.pim.app.ui.status

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.yield

internal suspend fun Lifecycle.repeatConnectionProbePolling(
    refresh: suspend () -> Long
) {
    repeatOnLifecycle(Lifecycle.State.STARTED) {
        while (isActive) {
            val delayMillis = refresh()
            if (delayMillis > 0L) delay(delayMillis) else yield()
        }
    }
}
