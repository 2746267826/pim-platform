package com.pim.app.ui.status

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pim.app.location.service.ForegroundLocationController
import com.pim.app.status.StatusCenterRepository
import com.pim.app.status.StatusCenterState
import com.pim.app.status.StatusActionTarget
import com.pim.app.status.StatusIssue
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

@HiltViewModel
class StatusCenterViewModel @Inject constructor(
    repository: StatusCenterRepository,
    private val foregroundLocationController: ForegroundLocationController
) : ViewModel() {
    val state: StateFlow<StatusCenterState> = repository.observe()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = StatusCenterState.empty()
        )

    fun onIssueAction(issue: StatusIssue): StatusActionTarget {
        return issue.target
    }

    fun syncNow() {
        foregroundLocationController.syncNow()
    }
}
