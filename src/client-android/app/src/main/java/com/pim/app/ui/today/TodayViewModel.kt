package com.pim.app.ui.today

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pim.app.status.StatusCenterRepository
import com.pim.app.status.StatusCenterState
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

data class TodayUiState(
    val apiStatusLabel: String = "API：待连接",
    val collectionStatusLabel: String = "持续采集：未开启"
)

object TodayStatusMapper {
    fun fromStatus(state: StatusCenterState): TodayUiState {
        val snapshot = state.snapshot
        val apiLabel = when {
            !snapshot.api.isValid -> "API：待连接"
            !snapshot.auth.hasAccessToken -> "API：待登录"
            snapshot.auth.isExpired -> "API：登录过期"
            else -> "API：已连接"
        }
        val collectionLabel = if (snapshot.service.continuousCollectionEnabled) {
            "持续采集：已开启"
        } else {
            "持续采集：未开启"
        }
        return TodayUiState(
            apiStatusLabel = apiLabel,
            collectionStatusLabel = collectionLabel
        )
    }
}

@HiltViewModel
class TodayViewModel @Inject constructor(
    statusCenterRepository: StatusCenterRepository
) : ViewModel() {
    val state: StateFlow<TodayUiState> = statusCenterRepository.observe()
        .map(TodayStatusMapper::fromStatus)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = TodayUiState()
        )
}
