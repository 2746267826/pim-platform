package com.pim.app.status

import androidx.work.WorkInfo
import com.pim.app.mobile.sync.MobileSyncOutcome
import com.pim.app.mobile.sync.MobileSyncState

object StatusResultMapper {

    private val BLOCKED_SYNC_PHASES = setOf(
        "server-missing",
        "auth-missing",
        "usage-permission-missing"
    )

    fun resolveSyncPhase(
        periodic: List<WorkInfo>,
        immediate: List<WorkInfo>,
        syncState: MobileSyncState,
        justAccepted: Boolean
    ): SyncPhase {
        if (justAccepted) return SyncPhase.Accepted
        val active = periodic + immediate
        if (active.any { it.state == WorkInfo.State.RUNNING }) return SyncPhase.Running
        if (immediate.any { it.state == WorkInfo.State.ENQUEUED } ||
            active.any { it.state == WorkInfo.State.BLOCKED }
        ) return SyncPhase.Waiting

        val phase = syncState.phase.lowercase()
        if (phase in BLOCKED_SYNC_PHASES) return SyncPhase.Blocked
        if (syncState.outcome != MobileSyncOutcome.SUCCESS ||
            phase == "failed" || phase.endsWith("-failed") || phase == "completed-with-errors"
        ) return SyncPhase.Failed
        if (phase in setOf("completed", "uploaded", "location-uploaded")) return SyncPhase.Completed
        return SyncPhase.Idle
    }

    fun computeNextAttemptAtMillis(
        periodic: List<WorkInfo>,
        immediate: List<WorkInfo>
    ): Long? {
        return (periodic + immediate)
            .filter { it.state == WorkInfo.State.ENQUEUED && it.nextScheduleTimeMillis > 0 }
            .minOfOrNull { it.nextScheduleTimeMillis }
    }

    fun buildExternalIssues(
        networkAvailability: NetworkAvailability,
        probeResult: ConnectionProbeResult?
    ): List<StatusIssue> {
        val issues = mutableListOf<StatusIssue>()
        when (networkAvailability) {
            NetworkAvailability.Unavailable -> {
                issues += StatusIssue.networkDisconnected()
            }
            NetworkAvailability.Restricted -> {
                val probeReachable = probeResult?.outcome == ConnectionProbeOutcome.Reachable
                if (probeReachable) {
                    issues += StatusIssue.systemNetworkRestricted(StatusSeverity.Info)
                } else {
                    issues += StatusIssue.systemNetworkRestricted(StatusSeverity.Warning)
                }
            }
            NetworkAvailability.Validated -> { }
        }
        if (probeResult != null) {
            when (probeResult.outcome) {
                ConnectionProbeOutcome.Blocked -> issues += StatusIssue.probeBlocked()
                ConnectionProbeOutcome.Partial -> issues += StatusIssue.probePartial()
                ConnectionProbeOutcome.Reachable -> { }
            }
        }
        return issues
    }

    fun buildState(
        snapshot: StatusCenterSnapshot,
        syncState: MobileSyncState,
        workInfos: StatusWorkInfos,
        permanentRejected: Int,
        networkAvailability: NetworkAvailability,
        probeResult: ConnectionProbeResult?,
        justAccepted: Boolean
    ): StatusCenterState {
        val syncPhase = resolveSyncPhase(
            periodic = workInfos.periodic,
            immediate = workInfos.immediate,
            syncState = syncState,
            justAccepted = justAccepted
        )
        val nextAttempt = computeNextAttemptAtMillis(
            periodic = workInfos.periodic,
            immediate = workInfos.immediate
        )
        val baseIssues = StatusIssuePlanner.plan(snapshot)
        val externalIssues = buildExternalIssues(
            networkAvailability = networkAvailability,
            probeResult = probeResult
        )
        val allIssues = (baseIssues + externalIssues).distinctBy { it.code }
        val finalIssues = if (syncPhase == SyncPhase.Failed) {
            (allIssues + StatusIssue.syncFailure()).distinctBy { it.code }
        } else {
            allIssues
        }
        return StatusCenterState(
            snapshot = snapshot,
            issues = finalIssues,
            syncPhase = syncPhase,
            pendingTotal = snapshot.queues.pendingUploadTotal,
            acceptedCount = syncState.acceptedCount,
            permanentRejectedCount = permanentRejected,
            rejectedCount = syncState.rejectedCount,
            lastSuccessfulUploadAt = syncState.lastSuccessfulUploadAt,
            lastAttemptedUploadAt = syncState.lastAttemptedUploadAt,
            nextAttemptAtMillis = nextAttempt,
            networkAvailability = networkAvailability,
            lastProbeResult = probeResult,
            lastProbeCheckedAtMillis = probeResult?.checkedAtUtcMillis,
            isLoading = false
        )
    }

    fun shouldClearAcceptedSignal(
        justAccepted: Boolean,
        immediate: List<WorkInfo>
    ): Boolean = justAccepted && immediate.any {
        it.state == WorkInfo.State.ENQUEUED ||
        it.state == WorkInfo.State.RUNNING ||
        it.state == WorkInfo.State.BLOCKED
    }

}
