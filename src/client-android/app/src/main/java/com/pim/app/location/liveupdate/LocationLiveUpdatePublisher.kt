package com.pim.app.location.liveupdate

import android.content.Context
import android.os.SystemClock
import com.pim.app.location.acquisition.AcquisitionPhase
import com.pim.app.location.acquisition.LocationAcquisitionState
import com.pim.app.location.acquisition.TriggerType
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicReference

class LocationLiveUpdatePublisher(
    private val stateFlow: StateFlow<LocationAcquisitionState>,
    private val context: Context? = null,
    private val clockMs: () -> Long = { SystemClock.elapsedRealtime() },
    publishFn: ((LocationLiveUpdateContent) -> Boolean)? = null,
    cancelFn: (() -> Unit)? = null
) {
    private val effectivePublish: (LocationLiveUpdateContent) -> Boolean = publishFn ?: { content ->
        LocationLiveUpdateNotificationRenderer.tryBuildAndNotify(
            ctx = context!!,
            content = content
        )
    }

    private val effectiveCancel: () -> Unit = cancelFn ?: {
        val mgr = context!!.getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
        mgr.cancel(LocationLiveUpdateNotificationRenderer.LIVE_UPDATE_NOTIFICATION_ID)
    }

    private var collectionJob: Job? = null
    private var lastPublishTimeMs: Long = -1L
    private var lastPublishedAccuracy: Float? = null
    private val suppressedSessionId = AtomicReference<String?>(null)
    private var currentSessionId: String? = null

    fun start(scope: CoroutineScope) {
        collectionJob?.cancel(CancellationException("restart"))
        lastPublishTimeMs = -1L
        lastPublishedAccuracy = null
        currentSessionId = null
        collectionJob = scope.launch {
            try {
                stateFlow.collect { state ->
                    runCatching { handleState(state) }
                        .onFailure { if (it is CancellationException) throw it }
                }
            } catch (_: CancellationException) {
            }
        }
    }

    fun cancelStaleNotification() {
        runCatching { effectiveCancel() }
    }

    fun suppressSession(sessionId: String) {
        suppressedSessionId.set(sessionId)
    }

    private fun handleState(state: LocationAcquisitionState) {
        val sessionId = state.sessionId

        if (state.phase != AcquisitionPhase.Acquiring) {
            cancelAndReset()
            return
        }

        if (sessionId == null) return
        if (sessionId == suppressedSessionId.get()) return

        val now = clockMs()

        if (sessionId != currentSessionId) {
            currentSessionId = sessionId
            lastPublishTimeMs = -1L
            lastPublishedAccuracy = null
        }

        val accuracy = state.bestLocation?.horizontalAccuracyMeters

        if (lastPublishTimeMs >= 0L) {
            val elapsed = now - lastPublishTimeMs
            if (elapsed < 2000L) {
                val prevAccuracy = lastPublishedAccuracy
                if (accuracy != null && prevAccuracy != null) {
                    val improvement = prevAccuracy - accuracy
                    if (improvement < 5f) return
                } else {
                    return
                }
            }
        }

        val triggerType = state.triggerType ?: TriggerType.MANUAL
        val providerLabel = state.bestLocation?.provider ?: "unknown"

        runCatching {
            effectivePublish(
                LocationLiveUpdateContent(
                    sessionId = sessionId,
                    triggerType = triggerType,
                    elapsedSeconds = state.elapsedMs / 1000,
                    accuracyMeters = accuracy,
                    providerLabel = providerLabel
                )
            )
        }.onSuccess { published ->
            if (published) {
                lastPublishTimeMs = now
                lastPublishedAccuracy = accuracy
            }
        }
    }

    private fun cancelAndReset() {
        runCatching { effectiveCancel() }
        lastPublishTimeMs = -1L
        lastPublishedAccuracy = null
        currentSessionId = null
    }
}
