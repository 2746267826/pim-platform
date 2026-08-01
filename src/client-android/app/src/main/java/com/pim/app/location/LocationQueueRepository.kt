package com.pim.app.location

import com.pim.app.data.MobileDataDao
import com.pim.app.data.MobileLocationDroppedDiagnosticEntity
import com.pim.app.data.MobileLocationPointEntity
import com.pim.app.data.MobileLocationPolicyTransitionEntity
import com.pim.app.location.policy.LocationPolicyMode
import com.pim.app.location.policy.PolicyDecision
import com.pim.app.location.quality.QualityAcceptedLocation
import com.pim.app.location.quality.RawLocationFix
import javax.inject.Inject

class LocationQueueRepository @Inject constructor(
    private val dao: MobileDataDao
) {
    suspend fun enqueueAccepted(
        accepted: QualityAcceptedLocation,
        rawJson: String,
        source: String = "auto"
    ): Long {
        return dao.insertLocationPoint(MobileLocationPointEntity.fromAccepted(accepted, rawJson, source))
    }

    suspend fun recordDropped(
        fix: RawLocationFix,
        reason: String,
        createdAtUtc: Long = System.currentTimeMillis()
    ): Long {
        return dao.insertDroppedLocationDiagnostic(
            MobileLocationDroppedDiagnosticEntity.fromDropped(fix, reason, createdAtUtc)
        )
    }

    suspend fun recordPolicyTransition(
        fromMode: LocationPolicyMode?,
        decision: PolicyDecision,
        occurredAtUtc: Long = System.currentTimeMillis()
    ): Long {
        return dao.insertPolicyTransition(
            MobileLocationPolicyTransitionEntity.fromDecision(fromMode, decision, occurredAtUtc)
        )
    }
}
