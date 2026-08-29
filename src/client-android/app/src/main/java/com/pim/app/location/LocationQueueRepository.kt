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
    private val dao: MobileDataDao,
    private val compressor: TrajectoryCompressor = TrajectoryCompressor()
) {
    @Volatile
    private var lastAccepted: QualityAcceptedLocation? = null

    suspend fun enqueueAccepted(
        accepted: QualityAcceptedLocation,
        rawJson: String,
        source: String = "auto"
    ): Long {
        val prev = lastAccepted
        if (prev != null && compressor.shouldClusterDrop(prev, accepted)) {
            // Stationary cluster: drop to avoid膨胀 (2.5s挡长期高频)
            return -1L
        }
        val id = dao.insertLocationPoint(MobileLocationPointEntity.fromAccepted(accepted, rawJson, source))
        if (id != -1L) lastAccepted = accepted
        return id
    }

    /**
     * Batch compression helper for upload path: Douglas-Peucker reduces synced payload.
     */
    fun compressForUpload(points: List<MobileLocationPointEntity>): List<MobileLocationPointEntity> {
        return compressor.compress(points)
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
