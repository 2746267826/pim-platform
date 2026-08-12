package com.pim.app.location.quality

import android.os.SystemClock
import kotlinx.coroutines.delay

class AltitudeWaitCoordinator(
    private val gate: LocationQualityGate = LocationQualityGate(),
    private val nowMillis: () -> Long = { System.currentTimeMillis() },
    private val nowElapsedRealtimeMillis: () -> Long = { SystemClock.elapsedRealtime() },
    private val delayMillis: suspend (Long) -> Unit = { delay(it) }
) {
    private var pendingAltitudeFix: PendingAltitudeFix? = null

    fun cancelPending() {
        pendingAltitudeFix = null
    }

    suspend fun handleFix(
        fix: RawLocationFix,
        deadlineCapMillis: Long? = null,
        deadlineCapElapsedRealtimeMillis: Long? = null,
        onAccepted: suspend (QualityAcceptedLocation) -> Unit,
        onDropped: suspend (RawLocationFix, String) -> Unit
    ) {
        when (val decision = gate.evaluate(fix, nowMillis())) {
            is QualityDecision.AcceptNow -> {
                pendingAltitudeFix = null
                onAccepted(decision.accepted)
            }
            is QualityDecision.Drop -> onDropped(decision.fix, decision.reason)
            is QualityDecision.WaitForAltitude -> {
                val pending = if (deadlineCapMillis != null) {
                    decision.pending.copy(
                        deadlineMillis = minOf(decision.pending.deadlineMillis, deadlineCapMillis)
                    )
                } else {
                    decision.pending
                }
                pendingAltitudeFix = pending
                waitThenHandleTimeout(
                    pending,
                    deadlineCapElapsedRealtimeMillis,
                    onAccepted,
                    onDropped
                )
            }
        }
    }

    private suspend fun waitThenHandleTimeout(
        pending: PendingAltitudeFix,
        deadlineCapElapsedRealtimeMillis: Long?,
        onAccepted: suspend (QualityAcceptedLocation) -> Unit,
        onDropped: suspend (RawLocationFix, String) -> Unit
    ) {
        // The wait must never outlive the overall session deadline. The wall-clock
        // based remaining can be arbitrarily large when the device wall clock is
        // rolled back (e.g. NTP correction after reboot) while GPS timestamps stay
        // on satellite time; the monotonic deadline caps the actual sleep duration.
        val wallClockRemaining = (pending.deadlineMillis - nowMillis()).coerceAtLeast(0L)
        val monotonicRemaining = deadlineCapElapsedRealtimeMillis
            ?.let { cap -> (cap - nowElapsedRealtimeMillis()).coerceAtLeast(0L) }
            ?: Long.MAX_VALUE
        val remainingMillis = minOf(wallClockRemaining, monotonicRemaining)
        if (remainingMillis > 0L) {
            delayMillis(remainingMillis)
        }
        if (pendingAltitudeFix != pending) return

        when (val decision = gate.timeoutDecision(pending, nowMillis().coerceAtLeast(pending.deadlineMillis))) {
            is QualityDecision.AcceptNow -> {
                pendingAltitudeFix = null
                onAccepted(decision.accepted)
            }
            is QualityDecision.Drop -> onDropped(decision.fix, decision.reason)
            is QualityDecision.WaitForAltitude -> waitThenHandleTimeout(
                decision.pending,
                deadlineCapElapsedRealtimeMillis,
                onAccepted,
                onDropped
            )
        }
    }
}
