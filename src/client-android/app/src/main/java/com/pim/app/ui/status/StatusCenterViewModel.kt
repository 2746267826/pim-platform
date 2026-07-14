package com.pim.app.ui.status

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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
import com.pim.app.status.resolveProbeResult
import com.pim.core.settings.ServerSettingsStore
import com.pim.core.settings.PimServerEndpoints
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel
class StatusCenterViewModel @Inject constructor(
    private val repository: StatusCenterRepository,
    private val mobileSyncScheduler: MobileSyncScheduler,
    private val serverSettingsStore: ServerSettingsStore,
    private val connectionProbeService: ConnectionProbeService,
    private val connectionProbeStore: ConnectionProbeStore,
    private val acceptedSignal: StatusAcceptedSignal
) : ViewModel() {
    val state: StateFlow<StatusCenterState> = repository.observe()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = StatusCenterState.empty()
        )

    private val syncRunner = StatusSyncActionRunner(
        syncNow = { mobileSyncScheduler.enqueueNow() },
        refresh = { repository.requestRefresh() },
        acceptedSignal = acceptedSignal
    )

    init {
        refresh()
    }

    fun onIssueAction(issue: StatusIssue): StatusActionTarget {
        repository.requestRefresh()
        return issue.target
    }

    fun syncNow() {
        viewModelScope.launch {
            syncRunner.run(StatusActionRoute.TriggerSync)
        }
    }

    fun forceConnectionProbe() {
        viewModelScope.launch {
            refreshConnectionForVisibleScreen(force = true)
        }
    }

    fun refresh() {
        repository.requestRefresh()
        viewModelScope.launch {
            refreshConnectionForVisibleScreen()
        }
    }

    suspend fun refreshConnectionForVisibleScreen(force: Boolean = false): Long {
        val serverUrl = serverSettingsStore.getBaseUrl()
        val serverIdentity = runCatching {
            PimServerEndpoints.from(serverUrl).apiBaseUrl.toString()
        }.getOrNull()
        val succeeded = runCatching {
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
        }.isSuccess
        repository.requestRefresh()
        if (!succeeded) return PROBE_RETRY_MILLIS
        val current = connectionProbeStore.result.value ?: return 0L
        if (current.serverIdentity != serverIdentity) return 0L
        val ageMillis = System.currentTimeMillis() - current.checkedAtUtcMillis
        if (ageMillis < 0L) return 0L
        return (ConnectionProbeStore.FRESHNESS_MILLIS - ageMillis).coerceAtLeast(0L)
    }
}

private const val PROBE_RETRY_MILLIS = 30_000L
