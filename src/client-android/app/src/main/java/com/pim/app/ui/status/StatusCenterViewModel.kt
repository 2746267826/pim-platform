package com.pim.app.ui.status

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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
    repository: StatusCenterRepository
) : ViewModel() {
    val state: StateFlow<StatusCenterState> = repository.observe()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = StatusCenterState.empty()
        )

    fun onIssueAction(issue: StatusIssue): StatusActionTarget {
        // Navigation and Android permission request surfaces stay owned by the screen layer.
        // Keeping the target here makes status rows actionable without starting requests from repositories.
        return issue.target
    }
}
