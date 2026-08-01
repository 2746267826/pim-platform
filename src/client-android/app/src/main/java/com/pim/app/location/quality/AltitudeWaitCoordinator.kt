package com.pim.app.location.quality

import kotlinx.coroutines.delay

class AltitudeWaitCoordinator(
    private val gate: LocationQualityGate = LocationQualityGate(),
    private val nowMillis: () -> Long = { System.currentTimeMillis() },
    private val delayMillis: suspend (Long) -> Unit = { delay(it) }
) {
    private var pendingAltitudeFix: PendingAltitudeFix? = null

    fun cancelPending() {
        pendingAltitudeFix = null
    }

    suspend fun handleFix(
        fix: RawLocationFix,
        deadlineCapMillis: Long? = null,
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
                waitThenHandleTimeout(pending, onAccepted, onDropped)
            }
        }
    }

    private suspend fun waitThenHandleTimeout(
        pending: PendingAltitudeFix,
        onAccepted: suspend (QualityAcceptedLocation) -> Unit,
        onDropped: suspend (RawLocationFix, String) -> Unit
    ) {
        val remainingMillis = (pending.deadlineMillis - nowMillis()).coerceAtLeast(0L)
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
                onAccepted,
                onDropped
            )
        }
    }
}
