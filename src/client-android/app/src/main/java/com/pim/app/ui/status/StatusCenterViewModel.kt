package com.pim.app.ui.status

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pim.app.mobile.diagnostics.DiagnosticExportException
import com.pim.app.mobile.diagnostics.DiagnosticExportResult
import com.pim.app.mobile.diagnostics.DiagnosticOperations
import com.pim.app.mobile.sync.MobileSyncScheduler
import com.pim.app.status.ConnectionProbeService
import com.pim.app.status.ConnectionProbeStore
import com.pim.app.status.StatusAcceptedSignal
import com.pim.app.status.StatusActionRoute
import com.pim.app.status.StatusActionTarget
import com.pim.app.status.StatusCenterRepository
import com.pim.app.status.StatusCenterState
import com.pim.app.status.StatusIssue
import com.pim.app.status.StatusSyncActionRunner
import com.pim.app.status.probeRefreshDelayMillis
import com.pim.app.status.resolveProbeResult
import com.pim.core.settings.ServerSettingsStore
import com.pim.core.settings.PimServerEndpoints
import dagger.hilt.android.lifecycle.HiltViewModel
import java.io.File
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class StatusActionFeedback {
    ProbeChecking,
    ProbeCompleted,
    ProbeFailed,
    SyncSubmitFailed
}

enum class DiagnosticExportFeedback {
    PackageReady,
    PackageReadyWithLocations,
    ShareOpened,
    ShareUnavailable,
    InsufficientStorage,
    ExportFailed
}

internal data class ExportFeedbackState(
    val isExporting: Boolean = false,
    val feedback: DiagnosticExportFeedback? = null,
    val exportedFile: File? = null,
    val coordinateCount: Int = 0
)

internal suspend fun runExportWithFeedback(
    includeRecentLocations: Boolean,
    export: suspend () -> DiagnosticExportResult,
    onState: (ExportFeedbackState) -> Unit
) {
    onState(ExportFeedbackState(isExporting = true))
    try {
        val result = export()
        val feedback = if (includeRecentLocations) {
            DiagnosticExportFeedback.PackageReadyWithLocations
        } else {
            DiagnosticExportFeedback.PackageReady
        }
        onState(
            ExportFeedbackState(
                isExporting = false,
                feedback = feedback,
                exportedFile = result.file,
                coordinateCount = result.coordinateCount
            )
        )
    } catch (e: DiagnosticExportException) {
        if (e.code == "INSUFFICIENT_STORAGE") {
            onState(
                ExportFeedbackState(
                    isExporting = false,
                    feedback = DiagnosticExportFeedback.InsufficientStorage
                )
            )
        } else {
            onState(
                ExportFeedbackState(
                    isExporting = false,
                    feedback = DiagnosticExportFeedback.ExportFailed
                )
            )
        }
    } catch (e: CancellationException) {
        throw e
    } catch (_: Exception) {
        onState(
            ExportFeedbackState(
                isExporting = false,
                feedback = DiagnosticExportFeedback.ExportFailed
            )
        )
    }
}

internal suspend fun runProbeWithFeedback(
    probe: suspend () -> Unit,
    feedbackSetter: (StatusActionFeedback?) -> Unit
) {
    feedbackSetter(StatusActionFeedback.ProbeChecking)
    try {
        probe()
        feedbackSetter(StatusActionFeedback.ProbeCompleted)
    } catch (e: CancellationException) {
        throw e
    } catch (_: Exception) {
        feedbackSetter(StatusActionFeedback.ProbeFailed)
    }
}

internal suspend fun runSyncWithFeedback(
    sync: suspend () -> Unit,
    feedbackSetter: (StatusActionFeedback?) -> Unit
) {
    feedbackSetter(null)
    try {
        sync()
    } catch (e: CancellationException) {
        throw e
    } catch (_: Exception) {
        feedbackSetter(StatusActionFeedback.SyncSubmitFailed)
    }
}

data class DiagnosticExportUiState(
    val includeRecentLocations: Boolean = false,
    val showLocationConfirmation: Boolean = false,
    val isExporting: Boolean = false,
    val exportedFile: File? = null,
    val coordinateCount: Int = 0,
    val feedback: DiagnosticExportFeedback? = null
)

internal fun beginDiagnosticExport(
    current: DiagnosticExportUiState
): DiagnosticExportUiState? {
    if (current.isExporting) return null
    return current.copy(
        showLocationConfirmation = false,
        isExporting = true,
        exportedFile = null,
        coordinateCount = 0,
        feedback = null
    )
}

@HiltViewModel
class StatusCenterViewModel @Inject constructor(
    private val repository: StatusCenterRepository,
    private val mobileSyncScheduler: MobileSyncScheduler,
    private val serverSettingsStore: ServerSettingsStore,
    private val connectionProbeService: ConnectionProbeService,
    private val connectionProbeStore: ConnectionProbeStore,
    private val acceptedSignal: StatusAcceptedSignal,
    private val diagnosticOperations: DiagnosticOperations
) : ViewModel() {
    val state: StateFlow<StatusCenterState> = repository.observe()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = StatusCenterState.empty()
        )

    private val _feedback = MutableStateFlow<StatusActionFeedback?>(null)
    val feedback: StateFlow<StatusActionFeedback?> = _feedback.asStateFlow()

    private val syncRunner = StatusSyncActionRunner(
        syncNow = { mobileSyncScheduler.enqueueNow() },
        refresh = { repository.requestRefresh() },
        acceptedSignal = acceptedSignal
    )

    private val _exportState = MutableStateFlow(DiagnosticExportUiState())
    val exportState: StateFlow<DiagnosticExportUiState> = _exportState.asStateFlow()

    fun setIncludeRecentLocations(include: Boolean) {
        if (_exportState.value.isExporting) return
        _exportState.value = _exportState.value.copy(
            includeRecentLocations = include,
            showLocationConfirmation = if (include) {
                _exportState.value.showLocationConfirmation
            } else false
        )
    }

    fun requestDiagnosticExport() {
        val current = _exportState.value
        if (current.isExporting) return
        if (current.includeRecentLocations) {
            _exportState.value = current.copy(showLocationConfirmation = true)
        } else {
            performExport(includeRecentLocations = false)
        }
    }

    fun confirmLocationExport() {
        _exportState.value = _exportState.value.copy(showLocationConfirmation = false)
        performExport(includeRecentLocations = true)
    }

    fun dismissLocationConfirmation() {
        _exportState.value = _exportState.value.copy(showLocationConfirmation = false)
    }

    fun reportShareResult(opened: Boolean) {
        if (_exportState.value.exportedFile == null) return
        _exportState.value = _exportState.value.copy(
            feedback = if (opened) DiagnosticExportFeedback.ShareOpened
            else DiagnosticExportFeedback.ShareUnavailable
        )
    }

    private fun performExport(includeRecentLocations: Boolean) {
        val started = beginDiagnosticExport(_exportState.value) ?: return
        _exportState.value = started
        viewModelScope.launch {
            runExportWithFeedback(
                includeRecentLocations = includeRecentLocations,
                export = { diagnosticOperations.export(includeRecentLocations) },
                onState = { state ->
                    _exportState.value = _exportState.value.copy(
                        isExporting = state.isExporting,
                        feedback = state.feedback,
                        exportedFile = state.exportedFile,
                        coordinateCount = state.coordinateCount
                    )
                }
            )
        }
    }

    fun onIssueAction(issue: StatusIssue): StatusActionTarget {
        repository.requestRefresh()
        return issue.target
    }

    fun syncNow() {
        viewModelScope.launch {
            runSyncWithFeedback(
                sync = { syncRunner.run(StatusActionRoute.TriggerSync) },
                feedbackSetter = { _feedback.value = it }
            )
        }
    }

    fun forceConnectionProbe() {
        viewModelScope.launch {
            runProbeWithFeedback(
                probe = { manualProbeOutcome() },
                feedbackSetter = { _feedback.value = it }
            )
        }
    }

    private suspend fun manualProbeOutcome() {
        val serverUrl = serverSettingsStore.getBaseUrl()
        try {
            val result = connectionProbeService.probe(serverUrl)
            if (serverSettingsStore.getBaseUrl() == serverUrl) {
                connectionProbeStore.save(result)
            }
        } finally {
            repository.requestRefresh()
        }
    }

    suspend fun refreshConnectionForVisibleScreen(force: Boolean = false): Long {
        val serverUrl = serverSettingsStore.getBaseUrl()
        val serverIdentity = runCatching {
            PimServerEndpoints.from(serverUrl).apiBaseUrl.toString()
        }.getOrNull()
        try {
            resolveProbeResult(
                force = force,
                serverIdentity = serverIdentity,
                store = connectionProbeStore,
                probe = { connectionProbeService.probe(serverUrl) },
                save = { result ->
                    if (serverSettingsStore.getBaseUrl() == serverUrl) {
                        connectionProbeStore.save(result)
                    } else false
                },
                nowMillis = System.currentTimeMillis()
            )
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            repository.requestRefresh()
            return PROBE_RETRY_MILLIS
        }
        repository.requestRefresh()
        return probeRefreshDelayMillis(
            result = connectionProbeStore.result.value,
            serverIdentity = serverIdentity,
            nowMillis = System.currentTimeMillis()
        )
    }
}

private const val PROBE_RETRY_MILLIS = 30_000L
