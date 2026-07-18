package com.pim.app.location.service

import com.pim.app.location.policy.LocationPolicyMode
import com.pim.app.location.policy.PolicyDecision

internal class PolicyTransitionDeduper {
    data class Transition(
        val fromMode: LocationPolicyMode?,
        val decision: PolicyDecision
    )

    var lastRecordedDecision: PolicyDecision? = null
        private set

    fun note(decision: PolicyDecision): Transition? {
        val previous = lastRecordedDecision
        val changed = previous == null ||
            previous.mode != decision.mode ||
            previous.requestIntervalMillis != decision.requestIntervalMillis ||
            previous.reason != decision.reason
        if (!changed) return null
        lastRecordedDecision = decision
        return Transition(fromMode = previous?.mode, decision = decision)
    }
}
