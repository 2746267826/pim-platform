package com.pim.app.ui.status

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pim.app.mobile.sync.MobileSyncCoordinator
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
    private val mobileSyncCoordinator: MobileSyncCoordinator
) : ViewModel() {
    val state: StateFlow<StatusCenterState> = repository.observe()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = StatusCenterState.empty()
        )

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
    }
}
