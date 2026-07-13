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
                pendingAltitudeFix = decision.pending
                waitThenHandleTimeout(decision.pending, onAccepted, onDropped)
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

        when (val decision = gate.timeoutDecision(pending, nowMillis())) {
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
