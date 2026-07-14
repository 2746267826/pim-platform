package com.pim.app.status

import androidx.work.WorkInfo
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
        justAccepted: Boolean
    ): SyncPhase {
        if (immediate.any { it.state == WorkInfo.State.RUNNING }) return SyncPhase.Running
        if (immediate.any { it.state == WorkInfo.State.ENQUEUED }) return SyncPhase.Waiting
        if (immediate.any { it.state == WorkInfo.State.FAILED }) return SyncPhase.Failed
        if (immediate.any { it.state == WorkInfo.State.CANCELLED }) return SyncPhase.Cancelled
        if (justAccepted) return SyncPhase.Accepted
        if (immediate.any { it.state == WorkInfo.State.SUCCEEDED }) return SyncPhase.Completed
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
        connected: Boolean,
        probeResult: ConnectionProbeResult?
    ): List<StatusIssue> {
        val issues = mutableListOf<StatusIssue>()
        if (!connected) {
            issues += StatusIssue.networkDisconnected()
        } else if (probeResult != null) {
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
        connected: Boolean,
        probeResult: ConnectionProbeResult?,
        justAccepted: Boolean
    ): StatusCenterState {
        val syncPhase = resolveSyncPhase(
            periodic = workInfos.periodic,
            immediate = workInfos.immediate,
            justAccepted = justAccepted
        )
        val nextAttempt = computeNextAttemptAtMillis(
            periodic = workInfos.periodic,
            immediate = workInfos.immediate
        )
        val baseIssues = StatusIssuePlanner.plan(snapshot)
        val externalIssues = buildExternalIssues(
            connected = connected,
            probeResult = probeResult
        )
        val allIssues = (baseIssues + externalIssues).distinctBy { it.code }
        val isBlockedSync = syncState.phase in BLOCKED_SYNC_PHASES
        val hasPersistedFailure = syncState.failedCount > 0 && !isBlockedSync
        val shouldAddSyncFailure = (syncPhase == SyncPhase.Failed || hasPersistedFailure) && !isBlockedSync
        val finalIssues = if (shouldAddSyncFailure) {
            (allIssues + StatusIssue.syncFailure(syncState.lastError)).distinctBy { it.code }
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
            networkConnected = connected,
            lastProbeResult = probeResult,
            lastProbeCheckedAtMillis = probeResult?.checkedAtUtcMillis,
            isLoading = false
        )
    }

    fun shouldClearAcceptedSignal(
        justAccepted: Boolean,
        immediate: List<WorkInfo>
    ): Boolean = justAccepted && immediate.isNotEmpty()

}
