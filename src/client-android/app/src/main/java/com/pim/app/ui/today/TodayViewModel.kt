package com.pim.app.ui.today

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pim.app.mobile.sync.MobileSyncScheduler
import com.pim.app.status.StatusAcceptedSignal
import com.pim.app.status.StatusActionRoute
import com.pim.app.status.StatusCenterRepository
import com.pim.app.status.StatusCenterState
import com.pim.app.status.StatusSyncActionRunner
import com.pim.app.status.SyncPhase
import com.pim.app.ui.shell.AndroidWebMessageBridge
import com.pim.core.auth.AuthSessionStore
import com.pim.core.network.AuthRefreshCoordinator
import com.pim.core.settings.PimServerEndpoints
import com.pim.core.settings.ServerSettingsStore
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class TodayStatus {
    Loading,
    NotStarted,
    PendingUpload,
    ServerEmpty,
    Ready,
    Error
}

data class TodayPageReport(
    val hasServerData: Boolean? = null,
    val generatedAt: String? = null,
    val error: String? = null
) {
    companion object {
        val EMPTY = TodayPageReport()

        fun fromMap(map: Map<String, String?>): TodayPageReport {
            val hasServerData = when (map["hasServerData"]) {
                "true" -> true
                "false" -> false
                else -> null
            }
            val generatedAt = map["generatedAt"]?.takeIf { it.isNotBlank() }
            val error = map["error"]?.trim()?.takeIf { it.isNotEmpty() }
            return TodayPageReport(
                hasServerData = hasServerData,
                generatedAt = generatedAt,
                error = error
            )
        }
    }
}

data class TodayPageReportEnvelope(
    val serverIdentity: String,
    val report: TodayPageReport
)

internal fun resolveReportFromEnvelope(
    envelope: TodayPageReportEnvelope?,
    currentServerIdentity: String
): TodayPageReport {
    if (envelope != null && envelope.serverIdentity == currentServerIdentity) {
        return envelope.report
    }
    return TodayPageReport.EMPTY
}

internal class ConfirmedCountTracker {
    private var baseline: Int = 0
    private var currentIdentity: String? = null

    fun observe(identity: String, acceptedCount: Int, isTerminal: Boolean): Boolean {
        if (currentIdentity != identity) {
            currentIdentity = identity
            baseline = acceptedCount
            return false
        }
        if (acceptedCount < baseline) {
            baseline = acceptedCount
            return false
        }
        if (isTerminal && acceptedCount > baseline) {
            baseline = acceptedCount
            return true
        }
        return false
    }
}

data class TodayUiState(
    val status: TodayStatus = TodayStatus.Loading,
    val statusTitle: String = "加载中",
    val statusDescription: String = "",
    val pendingCount: Int = 0,
    val confirmedCount: Int = 0,
    val rejectedCount: Int = 0,
    val permanentRejectedCount: Int = 0,
    val lastSuccessfulUploadAt: String? = null,
    val nextAttemptAt: String? = null,
    val generatedAt: String? = null,
    val embedSupported: Boolean? = null,
    val isSyncing: Boolean = false,
    val isSyncButtonDisabled: Boolean = false,
    val syncButtonLabel: String = "立即同步",
    val syncButtonShowSpinner: Boolean = false
)

internal const val TODAY_SYNC_FEEDBACK_DURATION_MS = 3_000L

internal fun syncNowWithGate(
    gate: AtomicBoolean,
    currentPhase: () -> SyncPhase,
    isInProgress: MutableStateFlow<Boolean>,
    feedback: MutableStateFlow<String?>,
    doSubmit: suspend () -> Unit,
    scope: CoroutineScope,
) {
    if (!gate.compareAndSet(false, true)) return
    val phase = currentPhase()
    if (phase == SyncPhase.Accepted || phase == SyncPhase.Running) {
        gate.set(false)
        return
    }
    scope.launch {
        try {
            runTodaySyncSubmit(
                enqueue = doSubmit,
                setFeedback = { feedback.value = it },
                setInProgress = { isInProgress.value = it }
            )
            val terminalMsg = feedback.value
            if (terminalMsg != null) {
                clearTodaySyncFeedbackAfterDelay(TODAY_SYNC_FEEDBACK_DURATION_MS, feedback, terminalMsg)
            }
        } finally {
            isInProgress.value = false
            gate.set(false)
        }
    }
}

internal suspend fun clearTodaySyncFeedbackAfterDelay(
    delayMs: Long,
    feedbackFlow: MutableStateFlow<String?>,
    expectedValue: String,
) {
    delay(delayMs)
    feedbackFlow.compareAndSet(expectedValue, null)
}

internal suspend fun runTodaySyncSubmit(
    enqueue: suspend () -> Unit,
    setFeedback: (String?) -> Unit,
    setInProgress: (Boolean) -> Unit
) {
    setInProgress(true)
    setFeedback("正在提交同步请求")
    try {
        enqueue()
        setFeedback("同步请求已提交")
    } catch (e: CancellationException) {
        throw e
    } catch (_: Exception) {
        setFeedback("同步失败：提交异常")
    } finally {
        setInProgress(false)
    }
}

internal fun isConfirmedRefreshTerminal(phase: SyncPhase): Boolean {
    return phase == SyncPhase.Completed || phase == SyncPhase.Failed
}

object TodayStatusMapper {

    private val isoFormatter = DateTimeFormatter.ISO_INSTANT.withZone(ZoneId.of("UTC"))

    fun fromStatus(
        state: StatusCenterState,
        pageReport: TodayPageReport = TodayPageReport.EMPTY,
        isSyncActionInProgress: Boolean = false,
        syncFeedback: String? = null,
        currentServerIdentity: String? = null
    ): TodayUiState {
        val snapshot = state.snapshot

        val pendingTotal = state.pendingTotal
        val isSyncing = state.syncPhase == SyncPhase.Running
        val nextAttemptStr = state.nextAttemptAtMillis?.let { formatIso(it) }
        val embedSupported = if (currentServerIdentity == null ||
            state.lastProbeResult?.serverIdentity == currentServerIdentity
        ) {
            state.lastProbeResult?.capabilities?.androidEmbedV1
        } else {
            null
        }

        val (status, title, desc) = determineStatus(
            isLoading = state.isLoading,
            isCollectionEnabled = snapshot.service.continuousCollectionEnabled,
            pendingTotal = pendingTotal,
            syncPhase = state.syncPhase,
            pageReport = pageReport
        )

        val (syncButtonLabel, syncButtonShowSpinner) = resolveSyncButtonPresentation(
            syncPhase = state.syncPhase,
            isSyncActionInProgress = isSyncActionInProgress,
            syncFeedback = syncFeedback
        )

        return TodayUiState(
            status = status,
            statusTitle = title,
            statusDescription = desc,
            pendingCount = pendingTotal,
            confirmedCount = state.acceptedCount,
            rejectedCount = state.rejectedCount,
            permanentRejectedCount = state.permanentRejectedCount,
            lastSuccessfulUploadAt = state.lastSuccessfulUploadAt,
            nextAttemptAt = nextAttemptStr,
            generatedAt = pageReport.generatedAt,
            embedSupported = embedSupported,
            isSyncing = isSyncing,
            isSyncButtonDisabled = isSyncButtonDisabled(
                isSyncActionInProgress = isSyncActionInProgress,
                syncPhase = state.syncPhase,
                syncFeedback = syncFeedback
            ),
            syncButtonLabel = syncButtonLabel,
            syncButtonShowSpinner = syncButtonShowSpinner
        )
    }

    fun toNativeState(state: StatusCenterState): Map<String, Any?> {
        val result = linkedMapOf<String, Any?>(
            "collectionMode" to state.snapshot.service.continuousCollectionEnabled,
            "triggerReason" to state.snapshot.tracking.currentPolicyReason,
            "nextLocationAt" to state.snapshot.tracking.nextExpectedLocationAtMillis?.let { formatIso(it) },
            "pending" to state.pendingTotal,
            "uploading" to (state.syncPhase == SyncPhase.Running),
            "confirmed" to state.acceptedCount,
            "rejected" to (state.rejectedCount + state.permanentRejectedCount),
            "lastSuccessAt" to state.lastSuccessfulUploadAt,
            "nextAttemptAt" to state.nextAttemptAtMillis?.let { formatIso(it) }
        )
        return result
    }

    fun isSyncButtonDisabled(
        isSyncActionInProgress: Boolean,
        syncPhase: SyncPhase,
        syncFeedback: String? = null
    ): Boolean {
        return isSyncActionInProgress ||
            syncPhase == SyncPhase.Accepted ||
            syncPhase == SyncPhase.Running ||
            syncFeedback != null
    }

    fun resolveSyncButtonPresentation(
        syncPhase: SyncPhase,
        isSyncActionInProgress: Boolean,
        syncFeedback: String?
    ): Pair<String, Boolean> {
        return when {
            syncPhase == SyncPhase.Running -> "同步中" to true
            syncPhase == SyncPhase.Accepted -> "已提交" to false
            isSyncActionInProgress -> "正在提交" to true
            syncFeedback == "同步请求已提交" -> "已提交" to false
            syncFeedback == "同步失败：提交异常" -> "提交失败" to false
            else -> "立即同步" to false
        }
    }

    private fun determineStatus(
        isLoading: Boolean,
        isCollectionEnabled: Boolean,
        pendingTotal: Int,
        syncPhase: SyncPhase,
        pageReport: TodayPageReport
    ): Triple<TodayStatus, String, String> {
        if (isLoading) {
            return Triple(TodayStatus.Loading, "加载中", "正在获取状态...")
        }

        if (pendingTotal > 0) {
            val (title, desc) = when (syncPhase) {
                SyncPhase.Accepted -> "请求已接受" to "等待系统开始同步"
                SyncPhase.Waiting -> "等待同步" to "等待网络或系统调度"
                SyncPhase.Running -> "正在同步" to "等待上传至服务器"
                else -> "有 $pendingTotal 项待上传" to "等待上传至服务器"
            }
            return Triple(TodayStatus.PendingUpload, title, desc)
        }

        if (syncPhase == SyncPhase.Accepted) {
            return Triple(TodayStatus.PendingUpload, "请求已接受", "等待系统开始同步")
        }
        if (syncPhase == SyncPhase.Waiting) {
            return Triple(TodayStatus.PendingUpload, "等待同步", "等待网络或系统调度")
        }
        if (syncPhase == SyncPhase.Running) {
            return Triple(TodayStatus.PendingUpload, "正在同步", "同步正在进行中")
        }

        if (pageReport.error != null) {
            return Triple(TodayStatus.Error, "页面加载错误", pageReport.error)
        }

        if (syncPhase == SyncPhase.Failed) {
            return Triple(TodayStatus.Error, "数据同步失败", "上次同步未成功完成")
        }

        if (syncPhase == SyncPhase.Blocked) {
            return Triple(TodayStatus.Error, "同步被阻止", "请检查网络和服务器连接")
        }

        when (pageReport.hasServerData) {
            true -> return Triple(TodayStatus.Ready, "有可用数据", "服务器已有今日数据")
            false -> return Triple(TodayStatus.ServerEmpty, "服务端确认无数据", "暂无今日数据")
            null -> Unit
        }

        if (!isCollectionEnabled) {
            return Triple(TodayStatus.NotStarted, "持续采集未开启", "开启持续采集后可获取今日数据")
        }

        return Triple(TodayStatus.NotStarted, "等待服务端数据", "页面尚未确认服务端数据")
    }

    private fun formatIso(millis: Long): String? {
        return try {
            isoFormatter.format(Instant.ofEpochMilli(millis))
        } catch (_: Exception) {
            null
        }
    }
}

@HiltViewModel
class TodayViewModel @Inject constructor(
    private val statusCenterRepository: StatusCenterRepository,
    private val authSessionStore: AuthSessionStore,
    private val authRefreshCoordinator: AuthRefreshCoordinator,
    private val serverSettingsStore: ServerSettingsStore,
    private val mobileSyncScheduler: MobileSyncScheduler,
    private val acceptedSignal: StatusAcceptedSignal
) : ViewModel() {

    val serverUrl: String get() = serverSettingsStore.getBaseUrl()

    private fun currentServerIdentity(): String {
        return runCatching {
            PimServerEndpoints.from(serverSettingsStore.getBaseUrl()).apiBaseUrl.toString()
        }.getOrElse { serverSettingsStore.getBaseUrl().trimEnd('/') }
    }

    private val pageReportFlow = MutableStateFlow<TodayPageReportEnvelope?>(null)
    private val syncFeedbackFlow = MutableStateFlow<String?>(null)
    private val isSyncActionInProgressFlow = MutableStateFlow(false)
    private val latestStatusState = MutableStateFlow(StatusCenterState.empty())
    private val refreshVersionFlow = MutableStateFlow(0L)
    private val confirmedCountTracker = ConfirmedCountTracker()

    val refreshVersion: StateFlow<Long> = refreshVersionFlow.asStateFlow()

    internal val syncSubmissionGate = AtomicBoolean(false)

    private val syncRunner = StatusSyncActionRunner(
        syncNow = { allowMeteredOnce -> mobileSyncScheduler.enqueueNow(allowMeteredOnce) },
        refresh = { statusCenterRepository.requestRefresh() },
        acceptedSignal = acceptedSignal
    )

    val bridge: AndroidWebMessageBridge = AndroidWebMessageBridge(
        authSessionStore = authSessionStore,
        refreshCoordinator = authRefreshCoordinator,
        serverSettingsStore = serverSettingsStore,
        scope = viewModelScope,
        nativeStateProvider = { TodayStatusMapper.toNativeState(latestStatusState.value) },
        pageReportSink = { report ->
            pageReportFlow.value = TodayPageReportEnvelope(currentServerIdentity(), TodayPageReport.fromMap(report))
        }
    )

    init {
        statusCenterRepository.observe()
            .onEach { statusState ->
                latestStatusState.value = statusState
                val isTerminal = isConfirmedRefreshTerminal(statusState.syncPhase)
                if (confirmedCountTracker.observe(currentServerIdentity(), statusState.acceptedCount, isTerminal)) {
                    refreshVersionFlow.value++
                }
            }
            .launchIn(viewModelScope)
    }

    val state: StateFlow<TodayUiState> = combine(
        latestStatusState,
        pageReportFlow,
        isSyncActionInProgressFlow,
        syncFeedbackFlow
    ) { statusState, reportEnvelope, inProgress, feedback ->
        val identity = currentServerIdentity()
        val resolvedReport = resolveReportFromEnvelope(reportEnvelope, identity)
        TodayStatusMapper.fromStatus(
            state = statusState,
            pageReport = resolvedReport,
            isSyncActionInProgress = inProgress,
            syncFeedback = feedback,
            currentServerIdentity = identity
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = TodayUiState()
    )

    val syncFeedback: StateFlow<String?> = syncFeedbackFlow.asStateFlow()

    fun syncNow() {
        syncNowWithGate(
            gate = syncSubmissionGate,
            currentPhase = { latestStatusState.value.syncPhase },
            isInProgress = isSyncActionInProgressFlow,
            feedback = syncFeedbackFlow,
            doSubmit = { syncRunner.run(StatusActionRoute.TriggerSync) },
            scope = viewModelScope,
        )
    }
}
