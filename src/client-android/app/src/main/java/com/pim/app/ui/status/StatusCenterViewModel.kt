package com.pim.app.ui.status

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pim.app.mobile.sync.MobileSyncCoordinator
import com.pim.app.status.ConnectionProbeRunner
import com.pim.app.status.StatusCenterRepository
import com.pim.app.status.StatusCenterState
import com.pim.app.status.StatusActionTarget
import com.pim.app.status.StatusActionRoute
import com.pim.app.status.StatusIssue
import com.pim.app.status.StatusSyncActionRunner
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel
class StatusCenterViewModel @Inject constructor(
    private val repository: StatusCenterRepository,
    private val mobileSyncCoordinator: MobileSyncCoordinator,
    private val connectionProbeRunner: ConnectionProbeRunner
) : ViewModel() {
    val state: StateFlow<StatusCenterState> = repository.observe()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = StatusCenterState.empty()
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
            StatusSyncActionRunner(
                syncNow = { mobileSyncCoordinator.syncOnOpen() },
                refresh = { repository.requestRefresh() }
            ).run(StatusActionRoute.TriggerSync)
        }
    }

    fun refresh() {
        repository.requestRefresh()
        viewModelScope.launch {
            refreshConnectionForVisibleScreen()
        }
    }

    suspend fun refreshConnectionForVisibleScreen(): Long {
        val succeeded = runCatching {
            connectionProbeRunner.run(force = false)
        }.isSuccess
        repository.requestRefresh()
        return if (succeeded) {
            connectionProbeRunner.millisUntilRefresh()
        } else {
            PROBE_RETRY_MILLIS
        }
    }
}

private const val PROBE_RETRY_MILLIS = 30_000L
