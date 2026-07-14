package com.pim.app.status

import androidx.work.WorkInfo
import com.pim.app.mobile.sync.MobileSyncState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class StatusOverallAndSyncPhaseTest {

    // --- StatusOverall computation ---

    @Test
    fun overallIsNormalWhenNoIssues() {
        assertEquals(StatusOverall.Normal, StatusOverall.compute(emptyList()))
    }

    @Test
    fun overallIsNormalWhenOnlyInfoIssues() {
        val issues = listOf(
            StatusIssue.altitudeMissingTimeout(),
            StatusIssue.recentDroppedLocation("test", null)
        )
        assertEquals(StatusOverall.Normal, StatusOverall.compute(issues))
    }

    @Test
    fun overallIsAttentionWhenWarningPresent() {
        val issues = listOf(
            StatusIssue.usageAccessMissing()
        )
        assertEquals(StatusOverall.Attention, StatusOverall.compute(issues))
    }

    @Test
    fun overallIsAbnormalWhenCriticalPresent() {
        val issues = listOf(
            StatusIssue.loginMissing()
        )
        assertEquals(StatusOverall.Abnormal, StatusOverall.compute(issues))
    }

    @Test
    fun overallIsAbnormalEvenWhenMixedWithLower() {
        val issues = listOf(
            StatusIssue.usageAccessMissing(),
            StatusIssue.apiAddressMissing(),
            StatusIssue.altitudeMissingTimeout()
        )
        assertEquals(StatusOverall.Abnormal, StatusOverall.compute(issues))
    }

    // --- new network/probe issue factories ---

    @Test
    fun networkDisconnectedIssueIsCritical() {
        val issue = StatusIssue.networkDisconnected()
        assertEquals(StatusSeverity.Critical, issue.severity)
        assertEquals("network-disconnected", issue.code)
    }

    @Test
    fun probeBlockedIssueIsCritical() {
        val issue = StatusIssue.probeBlocked()
        assertEquals(StatusSeverity.Critical, issue.severity)
        assertEquals("connection-probe-blocked", issue.code)
    }

    @Test
    fun probePartialIssueIsWarning() {
        val issue = StatusIssue.probePartial()
        assertEquals(StatusSeverity.Warning, issue.severity)
        assertEquals("connection-probe-partial", issue.code)
    }

    // --- SyncPhase mapping via production mapper ---

    @Test
    fun syncPhaseIsIdleWhenNoWorkInfos() {
        val phase = StatusResultMapper.resolveSyncPhase(
            periodic = emptyList(), immediate = emptyList(), justAccepted = false
        )
        assertEquals(SyncPhase.Idle, phase)
    }

    @Test
    fun syncPhaseIsWaitingWhenImmediateEnqueued() {
        val phase = StatusResultMapper.resolveSyncPhase(
            periodic = emptyList(),
            immediate = listOf(workInfo(WorkInfo.State.ENQUEUED, "pim_mobile_sync_now")),
            justAccepted = false
        )
        assertEquals(SyncPhase.Waiting, phase)
    }

    @Test
    fun syncPhaseIsRunningWhenImmediateRunning() {
        val phase = StatusResultMapper.resolveSyncPhase(
            periodic = emptyList(),
            immediate = listOf(workInfo(WorkInfo.State.RUNNING, "pim_mobile_sync_now")),
            justAccepted = false
        )
        assertEquals(SyncPhase.Running, phase)
    }

    @Test
    fun syncPhaseIsCompletedWhenImmediateSucceeded() {
        val phase = StatusResultMapper.resolveSyncPhase(
            periodic = emptyList(),
            immediate = listOf(workInfo(WorkInfo.State.SUCCEEDED, "pim_mobile_sync_now")),
            justAccepted = false
        )
        assertEquals(SyncPhase.Completed, phase)
    }

    @Test
    fun syncPhaseIsFailedWhenImmediateFailed() {
        val phase = StatusResultMapper.resolveSyncPhase(
            periodic = emptyList(),
            immediate = listOf(workInfo(WorkInfo.State.FAILED, "pim_mobile_sync_now")),
            justAccepted = false
        )
        assertEquals(SyncPhase.Failed, phase)
    }

    @Test
    fun syncPhaseIsCancelledWhenImmediateCancelled() {
        val phase = StatusResultMapper.resolveSyncPhase(
            periodic = emptyList(),
            immediate = listOf(workInfo(WorkInfo.State.CANCELLED, "pim_mobile_sync_now")),
            justAccepted = false
        )
        assertEquals(SyncPhase.Cancelled, phase)
    }

    @Test
    fun syncPhaseFavorsRunningOverEnqueued() {
        val phase = StatusResultMapper.resolveSyncPhase(
            periodic = emptyList(),
            immediate = listOf(
                workInfo(WorkInfo.State.ENQUEUED, "pim_mobile_sync_now"),
                workInfo(WorkInfo.State.RUNNING, "pim_mobile_sync_now")
            ),
            justAccepted = false
        )
        assertEquals(SyncPhase.Running, phase)
    }

    @Test
    fun syncPhaseIsIdleWhenOnlyPeriodicEnqueued() {
        val phase = StatusResultMapper.resolveSyncPhase(
            periodic = listOf(workInfo(WorkInfo.State.ENQUEUED, "pim_mobile_sync_periodic")),
            immediate = emptyList(),
            justAccepted = false
        )
        assertEquals(SyncPhase.Idle, phase)
    }

    @Test
    fun syncPhaseIsCompletedWhenPeriodicEnqueuedAndImmediateSucceeded() {
        val phase = StatusResultMapper.resolveSyncPhase(
            periodic = listOf(workInfo(WorkInfo.State.ENQUEUED, "pim_mobile_sync_periodic")),
            immediate = listOf(workInfo(WorkInfo.State.SUCCEEDED, "pim_mobile_sync_now")),
            justAccepted = false
        )
        assertEquals(SyncPhase.Completed, phase)
    }

    @Test
    fun syncPhaseIsAcceptedWhenJustAcceptedAndOnlyOldSucceeded() {
        val phase = StatusResultMapper.resolveSyncPhase(
            periodic = emptyList(),
            immediate = listOf(workInfo(WorkInfo.State.SUCCEEDED, "pim_mobile_sync_now")),
            justAccepted = true
        )
        assertEquals(SyncPhase.Accepted, phase)
    }

    @Test
    fun syncPhaseImmediateEnqueuedCoversAccepted() {
        val phase = StatusResultMapper.resolveSyncPhase(
            periodic = emptyList(),
            immediate = listOf(workInfo(WorkInfo.State.ENQUEUED, "pim_mobile_sync_now")),
            justAccepted = true
        )
        assertEquals(SyncPhase.Waiting, phase)
    }

    @Test
    fun syncPhaseImmediateRunningCoversAccepted() {
        val phase = StatusResultMapper.resolveSyncPhase(
            periodic = emptyList(),
            immediate = listOf(workInfo(WorkInfo.State.RUNNING, "pim_mobile_sync_now")),
            justAccepted = true
        )
        assertEquals(SyncPhase.Running, phase)
    }

    @Test
    fun syncPhaseAcceptedCoversCompleted() {
        val phase = StatusResultMapper.resolveSyncPhase(
            periodic = emptyList(),
            immediate = listOf(workInfo(WorkInfo.State.SUCCEEDED, "pim_mobile_sync_now")),
            justAccepted = true
        )
        assertEquals(SyncPhase.Accepted, phase)
    }

    // --- nextAttemptAtMillis via production mapper ---

    @Test
    fun nextAttemptAtMillisFromEarliestEnqueued() {
        val now = System.currentTimeMillis()
        val next = StatusResultMapper.computeNextAttemptAtMillis(
            periodic = emptyList(),
            immediate = listOf(
                workInfo(WorkInfo.State.ENQUEUED, "pim_mobile_sync_now", now + 60000),
                workInfo(WorkInfo.State.RUNNING, "pim_mobile_sync_periodic", 0)
            )
        )
        assertEquals(now + 60000, next)
    }

    @Test
    fun nextAttemptMergesPeriodicAndImmediate() {
        val now = System.currentTimeMillis()
        val next = StatusResultMapper.computeNextAttemptAtMillis(
            periodic = listOf(
                workInfo(WorkInfo.State.ENQUEUED, "pim_mobile_sync_periodic", now + 30000)
            ),
            immediate = listOf(
                workInfo(WorkInfo.State.ENQUEUED, "pim_mobile_sync_now", now + 60000)
            )
        )
        assertEquals(now + 30000, next)
    }

    @Test
    fun nextAttemptIgnoresZeroScheduleTime() {
        val now = System.currentTimeMillis()
        val next = StatusResultMapper.computeNextAttemptAtMillis(
            periodic = listOf(
                workInfo(WorkInfo.State.ENQUEUED, "pim_mobile_sync_periodic", 0)
            ),
            immediate = listOf(
                workInfo(WorkInfo.State.ENQUEUED, "pim_mobile_sync_now", now + 30000)
            )
        )
        assertEquals(now + 30000, next)
    }

    @Test
    fun nextAttemptIsNullWhenNoEnqueuedWithPositiveTime() {
        val next = StatusResultMapper.computeNextAttemptAtMillis(
            periodic = listOf(
                workInfo(WorkInfo.State.ENQUEUED, "pim_mobile_sync_periodic", 0)
            ),
            immediate = listOf(
                workInfo(WorkInfo.State.RUNNING, "pim_mobile_sync_now", 0)
            )
        )
        assertEquals(null, next)
    }

    // --- External issue synthesis via production mapper ---

    @Test
    fun externalIssuesEmptyWhenConnectedAndNoProbe() {
        val issues = StatusResultMapper.buildExternalIssues(connected = true, probeResult = null)
        assertEquals(emptyList<StatusIssue>(), issues)
    }

    @Test
    fun externalIssuesNetworkDisconnectedWhenNotConnected() {
        val issues = StatusResultMapper.buildExternalIssues(connected = false, probeResult = null)
        assertEquals(1, issues.size)
        assertEquals("network-disconnected", issues[0].code)
    }

    @Test
    fun externalIssuesProbeBlockedWhenBlocked() {
        val issues = StatusResultMapper.buildExternalIssues(
            connected = true,
            probeResult = ConnectionProbeResult(
                outcome = ConnectionProbeOutcome.Blocked,
                checkedAtUtcMillis = 1000L,
                lastCompletedStage = null,
                latencyMillisByStage = emptyMap(),
                capabilities = ServerCapabilities(false, false)
            )
        )
        assertEquals(1, issues.size)
        assertEquals("connection-probe-blocked", issues[0].code)
    }

    @Test
    fun externalIssuesProbePartialWhenPartial() {
        val issues = StatusResultMapper.buildExternalIssues(
            connected = true,
            probeResult = ConnectionProbeResult(
                outcome = ConnectionProbeOutcome.Partial,
                checkedAtUtcMillis = 1000L,
                lastCompletedStage = null,
                latencyMillisByStage = emptyMap(),
                capabilities = ServerCapabilities(false, false)
            )
        )
        assertEquals(1, issues.size)
        assertEquals("connection-probe-partial", issues[0].code)
    }

    @Test
    fun externalIssuesReachableDoesNotAddIssue() {
        val issues = StatusResultMapper.buildExternalIssues(
            connected = true,
            probeResult = ConnectionProbeResult(
                outcome = ConnectionProbeOutcome.Reachable,
                checkedAtUtcMillis = 1000L,
                lastCompletedStage = null,
                latencyMillisByStage = emptyMap(),
                capabilities = ServerCapabilities(false, false)
            )
        )
        assertEquals(0, issues.size)
    }

    @Test
    fun externalIssueNetworkOverridesProbe() {
        val issues = StatusResultMapper.buildExternalIssues(
            connected = false,
            probeResult = ConnectionProbeResult(
                outcome = ConnectionProbeOutcome.Blocked,
                checkedAtUtcMillis = 1000L,
                lastCompletedStage = null,
                latencyMillisByStage = emptyMap(),
                capabilities = ServerCapabilities(false, false)
            )
        )
        assertEquals(1, issues.size)
        assertEquals("network-disconnected", issues[0].code)
    }

    // --- shouldClearAcceptedSignal ---

    @Test
    fun shouldClearAcceptedSignalFalseWhenNotJustAccepted() {
        assertFalse(
            StatusResultMapper.shouldClearAcceptedSignal(
                justAccepted = false,
                immediate = listOf(workInfo(WorkInfo.State.SUCCEEDED, "pim_mobile_sync_now"))
            )
        )
    }

    @Test
    fun shouldClearAcceptedSignalFalseWhenImmediateEmpty() {
        assertFalse(
            StatusResultMapper.shouldClearAcceptedSignal(
                justAccepted = true,
                immediate = emptyList()
            )
        )
    }

    @Test
    fun shouldClearAcceptedSignalTrueWhenJustAcceptedAndImmediateNonEmpty() {
        assertTrue(
            StatusResultMapper.shouldClearAcceptedSignal(
                justAccepted = true,
                immediate = listOf(workInfo(WorkInfo.State.SUCCEEDED, "pim_mobile_sync_now"))
            )
        )
    }

    @Test
    fun shouldClearAcceptedSignalTrueForAnyImmediateState() {
        assertTrue(
            StatusResultMapper.shouldClearAcceptedSignal(
                justAccepted = true,
                immediate = listOf(workInfo(WorkInfo.State.ENQUEUED, "pim_mobile_sync_now"))
            )
        )
        assertTrue(
            StatusResultMapper.shouldClearAcceptedSignal(
                justAccepted = true,
                immediate = listOf(workInfo(WorkInfo.State.FAILED, "pim_mobile_sync_now"))
            )
        )
    }

    // --- StatusCenterState.empty() ---

    @Test
    fun emptyStateHasIsLoadingTrue() {
        val state = StatusCenterState.empty()
        assertEquals(true, state.isLoading)
    }

    @Test
    fun emptyStateHasIdleSyncPhase() {
        val state = StatusCenterState.empty()
        assertEquals(SyncPhase.Idle, state.syncPhase)
    }

    @Test
    fun emptyStateHasNormalOverall() {
        val state = StatusCenterState.empty()
        assertEquals(StatusOverall.Normal, state.overall)
    }

    // --- syncFailure issue ---

    @Test
    fun syncFailureIssueIsCriticalWithTriggerSync() {
        val issue = StatusIssue.syncFailure("connection timeout")
        assertEquals("sync-failure", issue.code)
        assertEquals(StatusSeverity.Critical, issue.severity)
        assertEquals("重新同步", issue.actionLabel)
        assertEquals(StatusActionTarget.Sync, issue.target)
    }

    @Test
    fun syncFailureIssueUsesDefaultMessageWhenNull() {
        val issue = StatusIssue.syncFailure(null)
        assertTrue(issue.message.isNotBlank())
    }

    // --- persisted sync failure (failedCount > 0) ---

    @Test
    fun buildStatePersistedFailedPhaseWithFailedCountProducesSyncFailureAndAbnormal() {
        val state = StatusResultMapper.buildState(
            snapshot = healthySnapshot,
            syncState = MobileSyncState(
                phase = "failed",
                progressText = "",
                failedCount = 1,
                lastError = "timeout"
            ),
            workInfos = StatusWorkInfos(emptyList(), emptyList()),
            permanentRejected = 0,
            connected = true,
            probeResult = null,
            justAccepted = false
        )

        assertEquals(StatusOverall.Abnormal, state.overall)
        assertTrue(state.issues.any { it.code == "sync-failure" })
    }

    @Test
    fun buildStatePersistedFailedPhaseWithFailedCountUsesLastError() {
        val state = StatusResultMapper.buildState(
            snapshot = healthySnapshot,
            syncState = MobileSyncState(
                phase = "failed",
                progressText = "",
                failedCount = 1,
                lastError = "connection timeout"
            ),
            workInfos = StatusWorkInfos(emptyList(), emptyList()),
            permanentRejected = 0,
            connected = true,
            probeResult = null,
            justAccepted = false
        )

        val syncIssue = state.issues.first { it.code == "sync-failure" }
        assertTrue(syncIssue.message.contains("connection timeout", ignoreCase = true))
    }

    @Test
    fun buildStateBlockedPhaseWithFailedCountZeroDoesNotProduceSyncFailure() {
        for (phase in listOf("server-missing", "auth-missing", "usage-permission-missing")) {
            val state = StatusResultMapper.buildState(
                snapshot = healthySnapshot,
                syncState = MobileSyncState(
                    phase = phase,
                    progressText = "",
                    failedCount = 2,
                    lastError = "some error"
                ),
                workInfos = StatusWorkInfos(emptyList(), emptyList()),
                permanentRejected = 0,
                connected = true,
                probeResult = null,
                justAccepted = false
            )
            assertFalse(
                "Phase $phase with failedCount=0 must not produce sync-failure",
                state.issues.any { it.code == "sync-failure" }
            )
        }
    }

    @Test
    fun buildStateBlockedPhaseWithFailedWorkInfoDoesNotDuplicateSyncFailure() {
        for (phase in listOf("server-missing", "auth-missing", "usage-permission-missing")) {
            val state = StatusResultMapper.buildState(
                snapshot = healthySnapshot,
                syncState = MobileSyncState(
                    phase = phase,
                    progressText = "",
                    failedCount = 0,
                    lastError = "some error"
                ),
                workInfos = StatusWorkInfos(
                    periodic = emptyList(),
                    immediate = listOf(workInfo(WorkInfo.State.FAILED, "pim_mobile_sync_now"))
                ),
                permanentRejected = 0,
                connected = true,
                probeResult = null,
                justAccepted = false
            )
            assertFalse(
                "Phase $phase with persisted failures and FAILED WorkInfo must not duplicate the blocking issue",
                state.issues.any { it.code == "sync-failure" }
            )
        }
    }

    // --- buildState aggregation ---

    private val healthySnapshot: StatusCenterSnapshot get() = StatusCenterSnapshot(
        permissions = PermissionStatusSnapshot(true, true, true, true, true, true),
        api = ApiConnectionSnapshot("https://valid.example", isValid = true, reasonCode = null, warnings = emptySet()),
        auth = AuthStatusSnapshot(hasAccessToken = true, isExpired = false),
        service = ForegroundServiceSnapshot(true, true),
        tracking = TrackingPolicySnapshot("power-saving", "PowerSavingNormal", null),
        queues = QueueStatusSnapshot(5, 3, 1, 2, 0, 1),
        diagnostics = DiagnosticSnapshot(null, null, null, null)
    )

    private val idleSyncState = com.pim.app.mobile.sync.MobileSyncState(
        phase = "idle",
        progressText = "",
        acceptedCount = 7,
        rejectedCount = 2,
        lastSuccessfulUploadAt = "2026-07-13T10:00:00Z",
        lastAttemptedUploadAt = "2026-07-13T10:05:00Z",
        lastError = null
    )

    @Test
    fun buildStateImmediateFailedWithNullErrorStillProducesSyncFailureAndAbnormal() {
        val state = StatusResultMapper.buildState(
            snapshot = healthySnapshot,
            syncState = idleSyncState.copy(lastError = null),
            workInfos = StatusWorkInfos(
                periodic = emptyList(),
                immediate = listOf(workInfo(WorkInfo.State.FAILED, "pim_mobile_sync_now"))
            ),
            permanentRejected = 0,
            connected = true,
            probeResult = null,
            justAccepted = false
        )

        assertEquals(SyncPhase.Failed, state.syncPhase)
        assertEquals(StatusOverall.Abnormal, state.overall)
        assertTrue(state.issues.any { it.code == "sync-failure" })
    }

    @Test
    fun buildStateAllHealthyProducesNormalNoIssues() {
        val state = StatusResultMapper.buildState(
            snapshot = healthySnapshot,
            syncState = idleSyncState,
            workInfos = StatusWorkInfos(emptyList(), emptyList()),
            permanentRejected = 0,
            connected = true,
            probeResult = null,
            justAccepted = false
        )

        assertEquals(StatusOverall.Normal, state.overall)
        assertEquals(emptyList<StatusIssue>(), state.issues)
        assertEquals(5 + 3 + 1 + 2 + 1, state.pendingTotal)
        assertEquals(7, state.acceptedCount)
        assertEquals(2, state.rejectedCount)
        assertEquals(0, state.permanentRejectedCount)
        assertEquals("2026-07-13T10:00:00Z", state.lastSuccessfulUploadAt)
        assertEquals("2026-07-13T10:05:00Z", state.lastAttemptedUploadAt)
    }

    @Test
    fun buildStateWarningPermissionProducesAttention() {
        val snap = healthySnapshot.copy(
            permissions = healthySnapshot.permissions.copy(notificationGranted = false)
        )
        val state = StatusResultMapper.buildState(
            snapshot = snap,
            syncState = idleSyncState,
            workInfos = StatusWorkInfos(emptyList(), emptyList()),
            permanentRejected = 0,
            connected = true,
            probeResult = null,
            justAccepted = false
        )

        assertEquals(StatusOverall.Attention, state.overall)
        assertTrue(state.issues.any { it.code == "notification-permission-missing" })
    }

    @Test
    fun buildStateImmediateFailedProducesFailedPhaseAndSyncFailureIssue() {
        val state = StatusResultMapper.buildState(
            snapshot = healthySnapshot,
            syncState = idleSyncState.copy(lastError = "server error 500"),
            workInfos = StatusWorkInfos(
                periodic = emptyList(),
                immediate = listOf(workInfo(WorkInfo.State.FAILED, "pim_mobile_sync_now"))
            ),
            permanentRejected = 1,
            connected = true,
            probeResult = null,
            justAccepted = false
        )

        assertEquals(SyncPhase.Failed, state.syncPhase)
        assertEquals(StatusOverall.Abnormal, state.overall)
        assertTrue(state.issues.any { it.code == "sync-failure" })
        assertEquals(1, state.permanentRejectedCount)
    }

    @Test
    fun buildStateProbeBlockedProducesConnectionProbeBlockedAndAbnormal() {
        val state = StatusResultMapper.buildState(
            snapshot = healthySnapshot,
            syncState = idleSyncState,
            workInfos = StatusWorkInfos(emptyList(), emptyList()),
            permanentRejected = 0,
            connected = true,
            probeResult = ConnectionProbeResult(
                outcome = ConnectionProbeOutcome.Blocked,
                checkedAtUtcMillis = 1000L,
                lastCompletedStage = null,
                latencyMillisByStage = emptyMap(),
                capabilities = ServerCapabilities(false, false)
            ),
            justAccepted = false
        )

        assertEquals(StatusOverall.Abnormal, state.overall)
        assertTrue(state.issues.any { it.code == "connection-probe-blocked" })
    }

    private fun workInfo(
        state: WorkInfo.State,
        name: String,
        nextScheduleTimeMillis: Long = 0
    ): WorkInfo {
        return WorkInfo(
            id = java.util.UUID.randomUUID(),
            state = state,
            outputData = androidx.work.Data.EMPTY,
            tags = setOf(name),
            progress = androidx.work.Data.EMPTY,
            runAttemptCount = 0,
            nextScheduleTimeMillis = nextScheduleTimeMillis
        )
    }
}
