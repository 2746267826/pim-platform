package com.pim.app.location.quality

import kotlinx.coroutines.delay

class AltitudeWaitCoordinator(
    private val gate: LocationQualityGate = LocationQualityGate(),
    private val nowMillis: () -> Long = { System.currentTimeMillis() },
    private val delayMillis: suspend (Long) -> Unit = { delay(it) }
) {
    suspend fun handleFix(
        fix: RawLocationFix,
        onAccepted: suspend (QualityAcceptedLocation) -> Unit,
        onDropped: suspend (RawLocationFix, String) -> Unit
    ) {
        when (val decision = gate.evaluate(fix, nowMillis())) {
            is QualityDecision.AcceptNow -> onAccepted(decision.accepted)
            is QualityDecision.Drop -> onDropped(decision.fix, decision.reason)
            is QualityDecision.WaitForAltitude -> waitThenHandleTimeout(
                decision.pending,
                onAccepted,
                onDropped
            )
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

        when (val decision = gate.timeoutDecision(pending, nowMillis())) {
            is QualityDecision.AcceptNow -> onAccepted(decision.accepted)
            is QualityDecision.Drop -> onDropped(decision.fix, decision.reason)
            is QualityDecision.WaitForAltitude -> waitThenHandleTimeout(
                decision.pending,
                onAccepted,
                onDropped
            )
        }
    }
}
