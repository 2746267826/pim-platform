package com.pim.app.ui.today

import com.pim.app.status.ApiConnectionSnapshot
import com.pim.app.status.ConnectionProbeOutcome
import com.pim.app.status.ConnectionProbeResult
import com.pim.app.status.ConnectionProbeStage
import com.pim.app.status.QueueStatusSnapshot
import com.pim.app.status.ServerCapabilities
import com.pim.app.status.StatusCenterSnapshot
import com.pim.app.status.StatusCenterState
import com.pim.app.status.SyncPhase
import com.pim.app.status.TrackingPolicySnapshot
import java.util.concurrent.atomic.AtomicBoolean
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class TodayViewModelTest {

    @Test
    fun `page report fromMap accepts only true false for hasServerData`() {
        assertEquals(true, TodayPageReport.fromMap(mapOf("hasServerData" to "true")).hasServerData)
        assertEquals(false, TodayPageReport.fromMap(mapOf("hasServerData" to "false")).hasServerData)
        assertNull(TodayPageReport.fromMap(mapOf("hasServerData" to "yes")).hasServerData)
        assertNull(TodayPageReport.fromMap(mapOf("hasServerData" to null)).hasServerData)
        assertNull(TodayPageReport.fromMap(emptyMap()).hasServerData)
    }

    @Test
    fun `page report fromMap trims empty error to null and keeps generatedAt`() {
        val report = TodayPageReport.fromMap(
            mapOf(
                "hasServerData" to "true",
                "generatedAt" to "2025-01-01T12:00:00Z",
                "error" to "   "
            )
        )
        assertEquals(true, report.hasServerData)
        assertEquals("2025-01-01T12:00:00Z", report.generatedAt)
        assertNull(report.error)
    }

    @Test
    fun `mapper loading state when isLoading is true`() {
        val state = StatusCenterState.empty()
        assertTrue(state.isLoading)
        val uiState = TodayStatusMapper.fromStatus(state, TodayPageReport.EMPTY)
        assertEquals(TodayStatus.Loading, uiState.status)
        assertEquals("加载中", uiState.statusTitle)
    }

    @Test
    fun `mapper not started when collection disabled no pending and no page report`() {
        val state = baseState(
            continuousCollectionEnabled = false,
            pendingTotal = 0,
            isLoading = false
        )
        val uiState = TodayStatusMapper.fromStatus(state, TodayPageReport.EMPTY)
        assertEquals(TodayStatus.NotStarted, uiState.status)
        assertEquals("持续采集未开启", uiState.statusTitle)
    }

    @Test
    fun `mapper waiting for server data when collection enabled no report no pending`() {
        val state = baseState(
            continuousCollectionEnabled = true,
            pendingTotal = 0,
            isLoading = false
        )
        val uiState = TodayStatusMapper.fromStatus(state, TodayPageReport.EMPTY)
        assertEquals(TodayStatus.NotStarted, uiState.status)
        assertEquals("等待服务端数据", uiState.statusTitle)
    }

    @Test
    fun `mapper completed without page report does not claim server empty`() {
        val state = baseState(
            continuousCollectionEnabled = true,
            pendingTotal = 0,
            isLoading = false,
            syncPhase = SyncPhase.Completed
        )
        val uiState = TodayStatusMapper.fromStatus(state, TodayPageReport.EMPTY)
        assertEquals(TodayStatus.NotStarted, uiState.status)
        assertEquals("等待服务端数据", uiState.statusTitle)
        assertFalse(uiState.statusTitle.contains("服务端确认无数据"))
    }

    @Test
    fun `mapper cancelled without page report does not claim server empty`() {
        val state = baseState(
            continuousCollectionEnabled = true,
            pendingTotal = 0,
            isLoading = false,
            syncPhase = SyncPhase.Cancelled
        )
        val uiState = TodayStatusMapper.fromStatus(state, TodayPageReport.EMPTY)
        assertEquals(TodayStatus.NotStarted, uiState.status)
        assertEquals("等待服务端数据", uiState.statusTitle)
    }

    @Test
    fun `mapper server empty only when hasServerData false without error`() {
        val state = baseState(pendingTotal = 0, isLoading = false)
        val report = TodayPageReport(hasServerData = false, generatedAt = null, error = null)
        val uiState = TodayStatusMapper.fromStatus(state, report)
        assertEquals(TodayStatus.ServerEmpty, uiState.status)
        assertEquals("服务端确认无数据", uiState.statusTitle)
    }

    @Test
    fun `mapper pending takes priority over server empty`() {
        val state = baseState(pendingTotal = 1, isLoading = false)
        val report = TodayPageReport(hasServerData = false, generatedAt = null, error = null)
        val uiState = TodayStatusMapper.fromStatus(state, report)
        assertEquals(TodayStatus.PendingUpload, uiState.status)
        assertEquals("有 1 项待上传", uiState.statusTitle)
    }

    @Test
    fun `mapper accepted waiting running take priority over server empty`() {
        val report = TodayPageReport(hasServerData = false, generatedAt = null, error = null)
        for (phase in listOf(SyncPhase.Accepted, SyncPhase.Waiting, SyncPhase.Running)) {
            val state = baseState(pendingTotal = 0, isLoading = false, syncPhase = phase)
            val uiState = TodayStatusMapper.fromStatus(state, report)
            assertEquals("phase=$phase", TodayStatus.PendingUpload, uiState.status)
            when (phase) {
                SyncPhase.Accepted -> {
                    assertEquals("请求已接受", uiState.statusTitle)
                    assertEquals("等待系统开始同步", uiState.statusDescription)
                }
                SyncPhase.Waiting -> {
                    assertEquals("等待同步", uiState.statusTitle)
                    assertEquals("等待网络或系统调度", uiState.statusDescription)
                }
                SyncPhase.Running -> {
                    assertEquals("正在同步", uiState.statusTitle)
                    assertEquals("同步正在进行中", uiState.statusDescription)
                }
                else -> Unit
            }
        }
    }

    @Test
    fun `mapper accepted waiting running with pending have distinct titles`() {
        val report = TodayPageReport(hasServerData = null, generatedAt = null, error = null)
        for (phase in listOf(SyncPhase.Accepted, SyncPhase.Waiting, SyncPhase.Running)) {
            val state = baseState(pendingTotal = 3, isLoading = false, syncPhase = phase)
            val uiState = TodayStatusMapper.fromStatus(state, report)
            assertEquals("phase=$phase", TodayStatus.PendingUpload, uiState.status)
            when (phase) {
                SyncPhase.Accepted -> {
                    assertEquals("请求已接受", uiState.statusTitle)
                    assertEquals("等待系统开始同步", uiState.statusDescription)
                }
                SyncPhase.Waiting -> {
                    assertEquals("等待同步", uiState.statusTitle)
                    assertEquals("等待网络或系统调度", uiState.statusDescription)
                }
                SyncPhase.Running -> {
                    assertEquals("正在同步", uiState.statusTitle)
                    assertEquals("等待上传至服务器", uiState.statusDescription)
                }
                else -> Unit
            }
        }
    }

    @Test
    fun `mapper ready when hasServerData true`() {
        val state = baseState(pendingTotal = 0, isLoading = false)
        val report = TodayPageReport(
            hasServerData = true,
            generatedAt = "2025-01-01T12:00:00Z",
            error = null
        )
        val uiState = TodayStatusMapper.fromStatus(state, report)
        assertEquals(TodayStatus.Ready, uiState.status)
        assertEquals("有可用数据", uiState.statusTitle)
        assertEquals("2025-01-01T12:00:00Z", uiState.generatedAt)
    }

    @Test
    fun `mapper error when page report has error`() {
        val state = baseState(pendingTotal = 0, isLoading = false, syncPhase = SyncPhase.Idle)
        val report = TodayPageReport(hasServerData = null, generatedAt = null, error = "服务器暂时不可用")
        val uiState = TodayStatusMapper.fromStatus(state, report)
        assertEquals(TodayStatus.Error, uiState.status)
        assertEquals("页面加载错误", uiState.statusTitle)
    }

    @Test
    fun `mapper error when sync phase failed`() {
        val state = baseState(pendingTotal = 0, isLoading = false, syncPhase = SyncPhase.Failed)
        val uiState = TodayStatusMapper.fromStatus(state, TodayPageReport.EMPTY)
        assertEquals(TodayStatus.Error, uiState.status)
        assertEquals("数据同步失败", uiState.statusTitle)
    }

    @Test
    fun `mapper blocked does not claim sync failed`() {
        val state = baseState(pendingTotal = 0, isLoading = false, syncPhase = SyncPhase.Blocked)
        val uiState = TodayStatusMapper.fromStatus(state, TodayPageReport.EMPTY)
        assertEquals(TodayStatus.Error, uiState.status)
        assertFalse(uiState.statusTitle.contains("同步失败"))
        assertEquals("同步被阻止", uiState.statusTitle)
    }

    @Test
    fun `mapper maps counts with separate permanent rejected and uploading`() {
        val state = baseState(
            pendingTotal = 7,
            isLoading = false,
            syncPhase = SyncPhase.Running,
            acceptedCount = 5,
            rejectedCount = 2,
            permanentRejectedCount = 1
        )
        val uiState = TodayStatusMapper.fromStatus(state, TodayPageReport.EMPTY)
        assertEquals(7, uiState.pendingCount)
        assertEquals(5, uiState.confirmedCount)
        assertEquals(2, uiState.rejectedCount)
        assertEquals(1, uiState.permanentRejectedCount)
        assertTrue(uiState.isSyncing)
    }

    @Test
    fun `mapper maps last successful upload and next attempt as ISO`() {
        val state = baseState(
            pendingTotal = 0,
            isLoading = false,
            lastSuccessfulUploadAt = "2025-01-01T12:00:00Z",
            nextAttemptAtMillis = 1_760_000_000_000L
        )
        val uiState = TodayStatusMapper.fromStatus(state, TodayPageReport.EMPTY)
        assertEquals("2025-01-01T12:00:00Z", uiState.lastSuccessfulUploadAt)
        assertEquals("2025-10-09T08:53:20Z", uiState.nextAttemptAt)
    }

    @Test
    fun `mapper embedSupported true false null comes from ConnectionProbeResult`() {
        val trueState = baseState(
            pendingTotal = 0,
            isLoading = false,
            probeAndroidEmbedV1 = true
        )
        val falseState = baseState(
            pendingTotal = 0,
            isLoading = false,
            probeAndroidEmbedV1 = false
        )
        val nullState = baseState(
            pendingTotal = 0,
            isLoading = false,
            probeAndroidEmbedV1 = null
        )

        assertEquals(true, TodayStatusMapper.fromStatus(trueState, TodayPageReport.EMPTY).embedSupported)
        assertEquals(false, TodayStatusMapper.fromStatus(falseState, TodayPageReport.EMPTY).embedSupported)
        assertNull(TodayStatusMapper.fromStatus(nullState, TodayPageReport.EMPTY).embedSupported)
    }

    @Test
    fun `native state maps whitelist boolean collectionMode and real trigger reason`() {
        val state = baseState(
            continuousCollectionEnabled = true,
            pendingTotal = 4,
            isLoading = false,
            syncPhase = SyncPhase.Running,
            acceptedCount = 9,
            rejectedCount = 2,
            permanentRejectedCount = 1,
            lastSuccessfulUploadAt = "2025-01-01T11:00:00Z",
            nextAttemptAtMillis = 1_760_000_000_000L,
            nextExpectedLocationAtMillis = 1_760_000_100_000L,
            currentPolicyReason = "schedule-low"
        )
        val native = TodayStatusMapper.toNativeState(state)

        assertEquals(
            setOf(
                "collectionMode",
                "triggerReason",
                "nextLocationAt",
                "pending",
                "uploading",
                "confirmed",
                "rejected",
                "lastSuccessAt",
                "nextAttemptAt"
            ),
            native.keys
        )
        assertEquals(true, native["collectionMode"])
        assertTrue(native["collectionMode"] is Boolean)
        assertEquals("schedule-low", native["triggerReason"])
        assertEquals("2025-10-09T08:55:00Z", native["nextLocationAt"])
        assertEquals(4, native["pending"])
        assertEquals(true, native["uploading"])
        assertEquals(9, native["confirmed"])
        assertEquals(3, native["rejected"])
        assertEquals("2025-01-01T11:00:00Z", native["lastSuccessAt"])
        assertEquals("2025-10-09T08:53:20Z", native["nextAttemptAt"])
        assertFalse(native.containsKey("secretToken"))
        assertFalse(native.containsKey("accessToken"))
    }

    @Test
    fun `native state omits unknown triggerReason and uses pendingTotal`() {
        val state = baseState(
            continuousCollectionEnabled = false,
            pendingTotal = 12,
            isLoading = false,
            syncPhase = SyncPhase.Idle,
            currentPolicyReason = null
        ).copy(
            snapshot = baseState().snapshot.copy(
                service = baseState().snapshot.service.copy(continuousCollectionEnabled = false),
                queues = QueueStatusSnapshot(1, 0, 0, 0, 0, 0),
                tracking = TrackingPolicySnapshot("power-saving", "PowerSavingNormal", null, null)
            )
        )
        val native = TodayStatusMapper.toNativeState(state)
        assertEquals(false, native["collectionMode"])
        assertNull(native["triggerReason"])
        assertFalse(native.containsKey("triggerReason") && native["triggerReason"] == "unknown")
        assertEquals(12, native["pending"])
        assertEquals(false, native["uploading"])
    }

    @Test
    fun `sync submit helper calls enqueue once success feedback and clears in progress without refresh`() = runTest {
        var enqueueCount = 0
        val feedback = mutableListOf<String?>()
        val inProgress = mutableListOf<Boolean>()

        runTodaySyncSubmit(
            enqueue = { enqueueCount++ },
            setFeedback = { feedback.add(it) },
            setInProgress = { inProgress.add(it) }
        )

        assertEquals(1, enqueueCount)
        assertEquals(
            listOf("正在提交同步请求", "同步请求已提交"),
            feedback
        )
        assertEquals(listOf(true, false), inProgress)
    }

    @Test
    fun `sync submit helper reports failure and clears in progress`() = runTest {
        val feedback = mutableListOf<String?>()
        val inProgress = mutableListOf<Boolean>()

        runTodaySyncSubmit(
            enqueue = { error("boom") },
            setFeedback = { feedback.add(it) },
            setInProgress = { inProgress.add(it) }
        )

        assertEquals(
            listOf("正在提交同步请求", "同步失败：提交异常"),
            feedback
        )
        assertEquals(listOf(true, false), inProgress)
    }

    @Test
    fun `sync submit helper rethrows CancellationException and clears in progress`() = runTest {
        val feedback = mutableListOf<String?>()
        val inProgress = mutableListOf<Boolean>()

        val result = runCatching {
            runTodaySyncSubmit(
                enqueue = { throw CancellationException("cancelled") },
                setFeedback = { feedback.add(it) },
                setInProgress = { inProgress.add(it) }
            )
        }

        assertTrue(result.exceptionOrNull() is CancellationException)
        assertEquals(listOf("正在提交同步请求"), feedback)
        assertEquals(listOf(true, false), inProgress)
    }

    @Test
    fun `sync feedback success does not permanently block another submit`() = runTest {
        var enqueueCount = 0
        val feedback = mutableListOf<String?>()
        val inProgress = mutableListOf<Boolean>()

        repeat(2) {
            runTodaySyncSubmit(
                enqueue = { enqueueCount++ },
                setFeedback = { feedback.add(it) },
                setInProgress = { inProgress.add(it) }
            )
        }

        assertEquals(2, enqueueCount)
        assertEquals(
            listOf(
                "正在提交同步请求",
                "同步请求已提交",
                "正在提交同步请求",
                "同步请求已提交"
            ),
            feedback
        )
        assertEquals(listOf(true, false, true, false), inProgress)
    }

    @Test
    fun `sync button disable uses in-progress accepted running or active feedback`() {
        assertTrue(
            TodayStatusMapper.isSyncButtonDisabled(
                isSyncActionInProgress = true,
                syncPhase = SyncPhase.Idle
            )
        )
        assertTrue(
            TodayStatusMapper.isSyncButtonDisabled(
                isSyncActionInProgress = false,
                syncPhase = SyncPhase.Accepted
            )
        )
        assertTrue(
            TodayStatusMapper.isSyncButtonDisabled(
                isSyncActionInProgress = false,
                syncPhase = SyncPhase.Running
            )
        )
        assertTrue(
            TodayStatusMapper.isSyncButtonDisabled(
                isSyncActionInProgress = false,
                syncPhase = SyncPhase.Idle,
                syncFeedback = "同步请求已提交"
            )
        )
        assertFalse(
            TodayStatusMapper.isSyncButtonDisabled(
                isSyncActionInProgress = false,
                syncPhase = SyncPhase.Idle
            )
        )
        assertFalse(
            TodayStatusMapper.isSyncButtonDisabled(
                isSyncActionInProgress = false,
                syncPhase = SyncPhase.Completed
            )
        )
    }

    @Test
    fun `sync button label and spinner derive from phase and in progress`() {
        val idleState = baseState(pendingTotal = 0, isLoading = false, syncPhase = SyncPhase.Idle)
        val acceptedState = baseState(pendingTotal = 0, isLoading = false, syncPhase = SyncPhase.Accepted)
        val runningState = baseState(pendingTotal = 0, isLoading = false, syncPhase = SyncPhase.Running)

        val idleUi = TodayStatusMapper.fromStatus(idleState, TodayPageReport.EMPTY)
        assertEquals("立即同步", idleUi.syncButtonLabel)
        assertFalse(idleUi.syncButtonShowSpinner)

        val acceptedUi = TodayStatusMapper.fromStatus(acceptedState, TodayPageReport.EMPTY)
        assertEquals("已提交", acceptedUi.syncButtonLabel)
        assertFalse(acceptedUi.syncButtonShowSpinner)

        val runningUi = TodayStatusMapper.fromStatus(runningState, TodayPageReport.EMPTY)
        assertEquals("同步中", runningUi.syncButtonLabel)
        assertTrue(runningUi.syncButtonShowSpinner)

        val inProgressUi = TodayStatusMapper.fromStatus(
            idleState,
            TodayPageReport.EMPTY,
            isSyncActionInProgress = true
        )
        assertEquals("正在提交", inProgressUi.syncButtonLabel)
        assertTrue(inProgressUi.syncButtonShowSpinner)
    }

    @Test
    fun `mapper terminal success feedback shows submitted without spinner`() {
        val idleState = baseState(pendingTotal = 0, isLoading = false, syncPhase = SyncPhase.Idle)
        val ui = TodayStatusMapper.fromStatus(
            idleState,
            TodayPageReport.EMPTY,
            isSyncActionInProgress = false,
            syncFeedback = "同步请求已提交"
        )
        assertEquals("已提交", ui.syncButtonLabel)
        assertFalse(ui.syncButtonShowSpinner)
        assertTrue(ui.isSyncButtonDisabled)
    }

    @Test
    fun `mapper terminal failure feedback shows submit failed without spinner`() {
        val idleState = baseState(pendingTotal = 0, isLoading = false, syncPhase = SyncPhase.Idle)
        val ui = TodayStatusMapper.fromStatus(
            idleState,
            TodayPageReport.EMPTY,
            isSyncActionInProgress = false,
            syncFeedback = "同步失败：提交异常"
        )
        assertEquals("提交失败", ui.syncButtonLabel)
        assertFalse(ui.syncButtonShowSpinner)
        assertTrue(ui.isSyncButtonDisabled)
    }

    @Test
    fun `mapper Running phase takes precedence over terminal success feedback`() {
        val runningState = baseState(pendingTotal = 0, isLoading = false, syncPhase = SyncPhase.Running)
        val ui = TodayStatusMapper.fromStatus(
            runningState,
            TodayPageReport.EMPTY,
            isSyncActionInProgress = false,
            syncFeedback = "同步请求已提交"
        )
        assertEquals("同步中", ui.syncButtonLabel)
        assertTrue(ui.syncButtonShowSpinner)
        assertTrue(ui.isSyncButtonDisabled)
    }

    @Test
    fun `mapper Accepted phase takes precedence over terminal failure feedback`() {
        val acceptedState = baseState(pendingTotal = 0, isLoading = false, syncPhase = SyncPhase.Accepted)
        val ui = TodayStatusMapper.fromStatus(
            acceptedState,
            TodayPageReport.EMPTY,
            isSyncActionInProgress = false,
            syncFeedback = "同步失败：提交异常"
        )
        assertEquals("已提交", ui.syncButtonLabel)
        assertFalse(ui.syncButtonShowSpinner)
        assertTrue(ui.isSyncButtonDisabled)
    }

    @Test
    fun `mapper keeps button disabled during feedback with Idle phase`() {
        val idleState = baseState(pendingTotal = 0, isLoading = false, syncPhase = SyncPhase.Idle)
        assertTrue(
            TodayStatusMapper.fromStatus(
                idleState,
                TodayPageReport.EMPTY,
                isSyncActionInProgress = false,
                syncFeedback = "同步请求已提交"
            ).isSyncButtonDisabled
        )
        assertTrue(
            TodayStatusMapper.fromStatus(
                idleState,
                TodayPageReport.EMPTY,
                isSyncActionInProgress = false,
                syncFeedback = "同步失败：提交异常"
            ).isSyncButtonDisabled
        )
    }

    @Test
    fun `resolveReportFromEnvelope uses report when identity matches`() {
        val identity = "http://myserver:5858/api/v1/"
        val report = TodayPageReport(hasServerData = true, generatedAt = "2025-01-01T12:00:00Z", error = null)
        val envelope = TodayPageReportEnvelope(serverIdentity = identity, report = report)

        val result = resolveReportFromEnvelope(envelope, identity)
        assertEquals(report, result)
    }

    @Test
    fun `resolveReportFromEnvelope returns EMPTY when identity mismatches`() {
        val envelope = TodayPageReportEnvelope(
            serverIdentity = "http://old-server:5858/api/v1/",
            report = TodayPageReport(hasServerData = true, generatedAt = "2025-01-01T12:00:00Z", error = null)
        )

        val result = resolveReportFromEnvelope(envelope, "http://new-server:5858/api/v1/")
        assertEquals(TodayPageReport.EMPTY, result)
    }

    @Test
    fun `resolveReportFromEnvelope returns EMPTY when envelope is null`() {
        val result = resolveReportFromEnvelope(null, "http://server:5858/api/v1/")
        assertEquals(TodayPageReport.EMPTY, result)
    }

    @Test
    fun `mapper embedSupported only from probe matching current server identity`() {
        val serverIdentity = "http://192.168.1.100:5858/api/v1/"
        val otherProbe = baseState(
            pendingTotal = 0,
            isLoading = false,
            probeAndroidEmbedV1 = true,
            probeServerIdentity = "http://old-server:5858/api/v1/"
        )
        val matchingProbe = baseState(
            pendingTotal = 0,
            isLoading = false,
            probeAndroidEmbedV1 = false,
            probeServerIdentity = serverIdentity
        )

        // Old probe with different identity → null
        assertNull(
            TodayStatusMapper.fromStatus(otherProbe, TodayPageReport.EMPTY, currentServerIdentity = serverIdentity).embedSupported
        )
        // Matching probe → false (as set in baseState)
        assertEquals(
            false,
            TodayStatusMapper.fromStatus(matchingProbe, TodayPageReport.EMPTY, currentServerIdentity = serverIdentity).embedSupported
        )
    }

    // ────────────────────────────────────────────────────────────────
    //  syncNowWithGate — atomic gate
    // ────────────────────────────────────────────────────────────────

    @Test
    fun `syncNowWithGate rejects second immediate submit when gate is acquired`() = runTest {
        val gate = AtomicBoolean(false)
        var submitCount = 0
        val isInProgress = MutableStateFlow(false)
        val feedback = MutableStateFlow<String?>(null)

        syncNowWithGate(
            gate = gate,
            currentPhase = { SyncPhase.Idle },
            isInProgress = isInProgress,
            feedback = feedback,
            doSubmit = { submitCount++ },
            scope = this,
        )
        syncNowWithGate(
            gate = gate,
            currentPhase = { SyncPhase.Idle },
            isInProgress = isInProgress,
            feedback = feedback,
            doSubmit = { submitCount++ },
            scope = this,
        )
        advanceUntilIdle()

        assertEquals(1, submitCount)
    }

    @Test
    fun `isInProgress is false during success feedback while gate remains held`() = runTest {
        val gate = AtomicBoolean(false)
        val isInProgress = MutableStateFlow(false)
        val feedback = MutableStateFlow<String?>(null)

        syncNowWithGate(
            gate = gate,
            currentPhase = { SyncPhase.Idle },
            isInProgress = isInProgress,
            feedback = feedback,
            doSubmit = {},
            scope = this,
        )
        // Let submit complete; stay inside feedback delay window.
        advanceTimeBy(1)
        assertEquals("同步请求已提交", feedback.value)
        assertFalse("isInProgress is only the enqueue call, not feedback delay", isInProgress.value)
        assertTrue("gate stays held through feedback delay", gate.get())

        advanceTimeBy(TODAY_SYNC_FEEDBACK_DURATION_MS)
        advanceUntilIdle()
        assertNull(feedback.value)
        assertFalse(isInProgress.value)
        assertFalse(gate.get())
    }

    @Test
    fun `syncNowWithGate gate releases after feedback delay allowing later retry`() = runTest {
        val gate = AtomicBoolean(false)
        var submitCount = 0
        val isInProgress = MutableStateFlow(false)
        val feedback = MutableStateFlow<String?>(null)

        syncNowWithGate(
            gate = gate, currentPhase = { SyncPhase.Idle },
            isInProgress = isInProgress, feedback = feedback,
            doSubmit = { submitCount++ }, scope = this,
        )
        syncNowWithGate(
            gate = gate, currentPhase = { SyncPhase.Idle },
            isInProgress = isInProgress, feedback = feedback,
            doSubmit = { submitCount++ }, scope = this,
        )
        advanceTimeBy(TODAY_SYNC_FEEDBACK_DURATION_MS)
        advanceUntilIdle()

        syncNowWithGate(
            gate = gate, currentPhase = { SyncPhase.Idle },
            isInProgress = isInProgress, feedback = feedback,
            doSubmit = { submitCount++ }, scope = this,
        )
        advanceUntilIdle()

        assertEquals(2, submitCount)
    }

    @Test
    fun `syncNowWithGate respects phase gate when phase is Accepted or Running`() = runTest {
        val gate = AtomicBoolean(false)
        var submitCount = 0
        val isInProgress = MutableStateFlow(false)
        val feedback = MutableStateFlow<String?>(null)

        for (phase in listOf(SyncPhase.Accepted, SyncPhase.Running)) {
            syncNowWithGate(
                gate = gate, currentPhase = { phase },
                isInProgress = isInProgress, feedback = feedback,
                doSubmit = { submitCount++ }, scope = this,
            )
            advanceUntilIdle()
        }
        assertEquals(0, submitCount)
    }

    @Test
    fun `syncNowWithGate releases gate after Accepted phase check`() = runTest {
        val gate = AtomicBoolean(false)
        var submitCount = 0
        val isInProgress = MutableStateFlow(false)
        val feedback = MutableStateFlow<String?>(null)

        syncNowWithGate(
            gate = gate, currentPhase = { SyncPhase.Accepted },
            isInProgress = isInProgress, feedback = feedback,
            doSubmit = { submitCount++ }, scope = this,
        )
        assertTrue("gate should be released after phase rejection", gate.compareAndSet(false, true))
        gate.set(false)

        syncNowWithGate(
            gate = gate, currentPhase = { SyncPhase.Idle },
            isInProgress = isInProgress, feedback = feedback,
            doSubmit = { submitCount++ }, scope = this,
        )
        advanceUntilIdle()
        assertEquals(1, submitCount)
    }

    @Test
    fun `syncNowWithGate ordinary exception sets terminal failure then releases gate`() = runTest {
        val gate = AtomicBoolean(false)
        val isInProgress = MutableStateFlow(false)
        val feedback = MutableStateFlow<String?>(null)

        syncNowWithGate(
            gate = gate,
            currentPhase = { SyncPhase.Idle },
            isInProgress = isInProgress,
            feedback = feedback,
            doSubmit = { error("submit boom") },
            scope = this,
        )
        advanceTimeBy(1)
        assertEquals("同步失败：提交异常", feedback.value)
        assertFalse(isInProgress.value)
        assertTrue(gate.get())

        advanceTimeBy(TODAY_SYNC_FEEDBACK_DURATION_MS)
        advanceUntilIdle()
        assertNull(feedback.value)
        assertFalse(isInProgress.value)
        assertFalse(gate.get())
    }

    @Test
    fun `syncNowWithGate CancellationException clears in-progress and releases gate`() = runTest {
        val gate = AtomicBoolean(false)
        val isInProgress = MutableStateFlow(false)
        val feedback = MutableStateFlow<String?>(null)

        syncNowWithGate(
            gate = gate,
            currentPhase = { SyncPhase.Idle },
            isInProgress = isInProgress,
            feedback = feedback,
            doSubmit = { throw CancellationException("cancelled") },
            scope = this,
        )
        advanceUntilIdle()
        assertFalse(isInProgress.value)
        assertFalse(gate.get())
    }

    // ────────────────────────────────────────────────────────────────
    //  clearTodaySyncFeedbackAfterDelay
    // ────────────────────────────────────────────────────────────────

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    @Test
    fun `clearTodaySyncFeedbackAfterDelay clears terminal feedback after delay`() = runTest {
        val flow = MutableStateFlow<String?>("同步请求已提交")
        launch {
            clearTodaySyncFeedbackAfterDelay(3000, flow, "同步请求已提交")
        }
        delay(2999)
        assertEquals("同步请求已提交", flow.value)
        delay(1)
        assertNull(flow.value)
    }

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    @Test
    fun `clearTodaySyncFeedbackAfterDelay does not clear when value changed`() = runTest {
        val flow = MutableStateFlow<String?>("同步请求已提交")
        launch {
            clearTodaySyncFeedbackAfterDelay(3000, flow, "同步请求已提交")
        }
        delay(2000)
        flow.value = "正在提交同步请求"
        delay(2000)
        assertEquals("正在提交同步请求", flow.value)
    }

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    @Test
    fun `clearTodaySyncFeedbackAfterDelay does not clear on mismatched expected value`() = runTest {
        val flow = MutableStateFlow<String?>("正在提交同步请求")
        launch {
            clearTodaySyncFeedbackAfterDelay(3000, flow, "同步请求已提交")
        }
        delay(5000)
        assertEquals("正在提交同步请求", flow.value)
    }

    @Test
    fun `mapper isSyncing true only for running phase`() {
        assertTrue(
            TodayStatusMapper.fromStatus(
                baseState(pendingTotal = 0, isLoading = false, syncPhase = SyncPhase.Running),
                TodayPageReport.EMPTY
            ).isSyncing
        )
        assertFalse(
            TodayStatusMapper.fromStatus(
                baseState(pendingTotal = 0, isLoading = false, syncPhase = SyncPhase.Idle),
                TodayPageReport.EMPTY
            ).isSyncing
        )
    }

    // ────────────────────────────────────────────────────────────────
    //  ConfirmedCountTracker — sync-triggered web refresh
    // ────────────────────────────────────────────────────────────────

    @Test
    fun `tracker first observation establishes baseline returns false`() {
        val tracker = ConfirmedCountTracker()
        assertFalse(tracker.observe("srv1", 5, true))
    }

    @Test
    fun `tracker same identity same count terminal returns false`() {
        val tracker = ConfirmedCountTracker()
        tracker.observe("srv1", 5, true)
        assertFalse(tracker.observe("srv1", 5, true))
    }

    @Test
    fun `tracker increased count terminal returns true`() {
        val tracker = ConfirmedCountTracker()
        tracker.observe("srv1", 5, true)
        assertTrue(tracker.observe("srv1", 8, true))
    }

    @Test
    fun `tracker identity change resets baseline regardless of terminal`() {
        val tracker = ConfirmedCountTracker()
        tracker.observe("srv1", 5, true)
        assertFalse("new identity must establish baseline, not trigger", tracker.observe("srv2", 5, true))
        assertTrue("after identity reset, increase should trigger", tracker.observe("srv2", 8, true))
    }

    @Test
    fun `tracker decreased count terminal returns false`() {
        val tracker = ConfirmedCountTracker()
        tracker.observe("srv1", 10, true)
        assertFalse(tracker.observe("srv1", 5, true))
    }

    @Test
    fun `tracker non-terminal increase returns false but terminal later triggers`() {
        val tracker = ConfirmedCountTracker()
        tracker.observe("srv1", 5, true)
        assertFalse(tracker.observe("srv1", 8, false))
        assertTrue(tracker.observe("srv1", 8, true))
    }

    @Test
    fun `tracker non-terminal no increase then terminal no increase returns false`() {
        val tracker = ConfirmedCountTracker()
        tracker.observe("srv1", 5, true)
        assertFalse(tracker.observe("srv1", 5, false))
        assertFalse(tracker.observe("srv1", 5, true))
    }

    @Test
    fun `tracker only triggers once per confirmed increase`() {
        val tracker = ConfirmedCountTracker()
        tracker.observe("srv1", 5, true)
        assertTrue(tracker.observe("srv1", 8, true))
        assertFalse("same value after trigger should not re-trigger", tracker.observe("srv1", 8, true))
    }

    @Test
    fun `tracker triggers again after another terminal increase`() {
        val tracker = ConfirmedCountTracker()
        tracker.observe("srv1", 5, true)
        tracker.observe("srv1", 8, true)
        assertTrue(tracker.observe("srv1", 12, true))
    }

    @Test
    fun `tracker count decrease updates baseline so future increase can trigger`() {
        val tracker = ConfirmedCountTracker()
        tracker.observe("srv1", 10, true)
        assertFalse(tracker.observe("srv1", 3, true))
        assertTrue(tracker.observe("srv1", 7, true))
    }

    @Test
    fun `tracker terminal increase above baseline triggers regardless of prior non-terminal observations`() {
        val tracker = ConfirmedCountTracker()
        tracker.observe("srv1", 5, true)
        tracker.observe("srv1", 10, false)
        assertTrue(tracker.observe("srv1", 7, true))
    }

    // ────────────────────────────────────────────────────────────────
    //  isConfirmedRefreshTerminal
    // ────────────────────────────────────────────────────────────────

    @Test
    fun `isConfirmedRefreshTerminal Completed returns true`() {
        assertTrue(isConfirmedRefreshTerminal(SyncPhase.Completed))
    }

    @Test
    fun `isConfirmedRefreshTerminal Failed returns true`() {
        assertTrue(isConfirmedRefreshTerminal(SyncPhase.Failed))
    }

    @Test
    fun `isConfirmedRefreshTerminal Idle returns false`() {
        assertFalse(isConfirmedRefreshTerminal(SyncPhase.Idle))
    }

    @Test
    fun `isConfirmedRefreshTerminal Accepted returns false`() {
        assertFalse(isConfirmedRefreshTerminal(SyncPhase.Accepted))
    }

    @Test
    fun `isConfirmedRefreshTerminal Waiting returns false`() {
        assertFalse(isConfirmedRefreshTerminal(SyncPhase.Waiting))
    }

    @Test
    fun `isConfirmedRefreshTerminal Running returns false`() {
        assertFalse(isConfirmedRefreshTerminal(SyncPhase.Running))
    }

    @Test
    fun `isConfirmedRefreshTerminal Blocked returns false`() {
        assertFalse(isConfirmedRefreshTerminal(SyncPhase.Blocked))
    }

    @Test
    fun `isConfirmedRefreshTerminal Cancelled returns false`() {
        assertFalse(isConfirmedRefreshTerminal(SyncPhase.Cancelled))
    }

    // ────────────────────────────────────────────────────────────────

    private fun baseState(
        continuousCollectionEnabled: Boolean = true,
        pendingTotal: Int = 0,
        isLoading: Boolean = true,
        syncPhase: SyncPhase = SyncPhase.Idle,
        acceptedCount: Int = 0,
        rejectedCount: Int = 0,
        permanentRejectedCount: Int = 0,
        lastSuccessfulUploadAt: String? = null,
        nextAttemptAtMillis: Long? = null,
        nextExpectedLocationAtMillis: Long? = null,
        currentPolicyReason: String? = null,
        probeAndroidEmbedV1: Boolean? = null,
        probeServerIdentity: String = "http://127.0.0.1:5858/",
        snapshotApiAddress: String = ""
    ): StatusCenterState {
        val empty = StatusCenterState.empty()
        val probe = probeAndroidEmbedV1?.let { embed ->
            ConnectionProbeResult(
                outcome = ConnectionProbeOutcome.Reachable,
                checkedAtUtcMillis = 1L,
                serverIdentity = probeServerIdentity,
                lastCompletedStage = ConnectionProbeStage.WebRoot,
                latencyMillisByStage = emptyMap(),
                capabilities = ServerCapabilities(
                    mobileItemResultsV1 = true,
                    androidEmbedV1 = embed
                )
            )
        }
        return StatusCenterState(
            snapshot = StatusCenterSnapshot(
                permissions = empty.snapshot.permissions,
                api = ApiConnectionSnapshot(address = snapshotApiAddress, isValid = snapshotApiAddress.isNotBlank(), reasonCode = null, warnings = emptySet()),
                auth = empty.snapshot.auth,
                service = empty.snapshot.service.copy(
                    continuousCollectionEnabled = continuousCollectionEnabled
                ),
                tracking = TrackingPolicySnapshot(
                    profile = "power-saving",
                    currentPolicyMode = "PowerSavingNormal",
                    nextExpectedLocationAtMillis = nextExpectedLocationAtMillis,
                    currentPolicyReason = currentPolicyReason
                ),
                queues = QueueStatusSnapshot(
                    pendingLocationPoints = pendingTotal,
                    pendingUsageEvents = 0,
                    pendingUsageSummaries = 0,
                    pendingAppMetadata = 0,
                    pendingDeviceProfile = 0
                ),
                diagnostics = empty.snapshot.diagnostics
            ),
            issues = emptyList(),
            syncPhase = syncPhase,
            pendingTotal = pendingTotal,
            acceptedCount = acceptedCount,
            rejectedCount = rejectedCount,
            permanentRejectedCount = permanentRejectedCount,
            lastSuccessfulUploadAt = lastSuccessfulUploadAt,
            nextAttemptAtMillis = nextAttemptAtMillis,
            lastProbeResult = probe,
            isLoading = isLoading
        )
    }
}
