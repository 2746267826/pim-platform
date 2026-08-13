package com.pim.app.location.liveupdate

import android.content.Context
import android.os.SystemClock
import com.pim.app.location.acquisition.AcquisitionPhase
import com.pim.app.location.acquisition.LocationAcquisitionState
import com.pim.app.location.acquisition.TriggerType
import com.pim.app.location.service.ForegroundLocationRuntimeState
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class LocationLiveUpdatePublisher(
    private val stateFlow: StateFlow<LocationAcquisitionState>,
    private val context: Context? = null,
    private val clockMs: () -> Long = { SystemClock.elapsedRealtime() },
    private val highSpeedFlow: StateFlow<ForegroundLocationRuntimeState>? = null,
    publishFn: ((LocationLiveUpdateContent) -> Boolean)? = null,
    cancelFn: (() -> Unit)? = null,
    publishHighSpeedFn: ((HighSpeedLiveUpdateContent) -> Boolean)? = null
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

    private val effectivePublishHighSpeed: (HighSpeedLiveUpdateContent) -> Boolean =
        publishHighSpeedFn ?: { content ->
            LocationLiveUpdateNotificationRenderer.tryBuildAndNotifyHighSpeed(
                ctx = context!!,
                content = content
            )
        }

    private val lock = Any()
    private var collectionJob: Job? = null
    private var highSpeedJob: Job? = null
    private var lastPublishTimeMs: Long = -1L
    private var lastPublishedAccuracy: Float? = null
    private var suppressedSessionId: String? = null
    private var currentSessionId: String? = null
    private var publishedSessionId: String? = null
    // 高速档 Live Update 与定位会话共用 7102 单通知 ID：高速档激活时
    // 覆盖/抑制会话发布；回落时取消高速档并重放会话内容（若有进行中会话）。
    private var highSpeedPublished = false
    private var lastHighSpeedPublishTimeMs: Long = -1L

    fun start(scope: CoroutineScope) {
        collectionJob?.cancel(CancellationException("restart"))
        highSpeedJob?.cancel(CancellationException("restart"))
        synchronized(lock) {
            lastPublishTimeMs = -1L
            lastPublishedAccuracy = null
            currentSessionId = null
            publishedSessionId = null
            highSpeedPublished = false
            lastHighSpeedPublishTimeMs = -1L
        }
        collectionJob = scope.launch {
            try {
                stateFlow.collect { state ->
                    runCatching { handleState(state) }
                        .onFailure { if (it is CancellationException) throw it }
                }
            } catch (_: CancellationException) {
            }
        }
        highSpeedJob = scope.launch {
            try {
                highSpeedFlow?.collect { runtime ->
                    runCatching { handleHighSpeed(runtime) }
                        .onFailure { if (it is CancellationException) throw it }
                }
            } catch (_: CancellationException) {
            }
        }
    }

    fun cancelStaleNotification() {
        synchronized(lock) {
            if (publishedSessionId == null && !highSpeedPublished) {
                runCatching { effectiveCancel() }
            }
        }
    }

    fun suppressSession(sessionId: String) {
        synchronized(lock) {
            suppressedSessionId = sessionId
            if (publishedSessionId == sessionId) {
                runCatching { effectiveCancel() }
                lastPublishTimeMs = -1L
                lastPublishedAccuracy = null
                currentSessionId = null
                publishedSessionId = null
            }
        }
    }

    private fun handleHighSpeed(runtime: ForegroundLocationRuntimeState) {
        synchronized(lock) {
            if (runtime.highSpeedActive) {
                val now = clockMs()
                if (!highSpeedPublished ||
                    now - lastHighSpeedPublishTimeMs >= HIGH_SPEED_PUBLISH_THROTTLE_MILLIS
                ) {
                    runCatching {
                        effectivePublishHighSpeed(
                            HighSpeedLiveUpdateContent(elapsedSeconds = runtime.highSpeedElapsedSeconds)
                        )
                    }.onSuccess { published ->
                        if (published) {
                            highSpeedPublished = true
                            lastHighSpeedPublishTimeMs = now
                        }
                    }
                }
            } else if (highSpeedPublished) {
                runCatching { effectiveCancel() }
                highSpeedPublished = false
                lastHighSpeedPublishTimeMs = -1L
                // 回落时若仍有进行中的定位会话，立即重放其 Live Update 内容，
                // 避免 7102 通知出现空窗直到下一次会话状态变更。
                runCatching { handleState(stateFlow.value) }
            }
        }
    }

    private fun handleState(state: LocationAcquisitionState) {
        if (highSpeedPublished) return

        val sessionId = state.sessionId

        if (state.phase != AcquisitionPhase.Acquiring &&
            state.phase != AcquisitionPhase.Evaluating
        ) {
            cancelAndReset()
            return
        }

        if (sessionId == null) return

        synchronized(lock) {
            if (sessionId == suppressedSessionId) return

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
                    } else if (accuracy == null) {
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
                    publishedSessionId = sessionId
                }
            }
        }
    }

    private fun cancelAndReset() {
        synchronized(lock) {
            if (publishedSessionId != null) {
                runCatching { effectiveCancel() }
            }
            lastPublishTimeMs = -1L
            lastPublishedAccuracy = null
            currentSessionId = null
            publishedSessionId = null
        }
    }

    private companion object {
        /** 高速档 Live Update 最小发布间隔：运行时状态每 2.5s 更新一次，节流到 10s。 */
        const val HIGH_SPEED_PUBLISH_THROTTLE_MILLIS = 10_000L
    }
}
