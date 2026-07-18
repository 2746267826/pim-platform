package com.pim.app.ui.schedule

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pim.app.location.service.ForegroundLocationService
import com.pim.app.schedule.ScheduleWindowRepository
import com.pim.app.settings.TrackingSettingsStore
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.ZoneId
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel
class SchedulePolicyViewModel @Inject constructor(
    private val repository: ScheduleWindowRepository,
    private val trackingSettingsStore: TrackingSettingsStore
) : ViewModel() {

    private val _refreshing = MutableStateFlow(false)

    val state: StateFlow<SchedulePolicyUiState> = combine(
        repository.snapshot,
        ForegroundLocationService.runtimeState,
        _refreshing
    ) { snapshot, runtimeState, refreshing ->
        SchedulePolicyMapper.stateFor(
            snapshot = snapshot,
            runtimeState = runtimeState,
            settings = trackingSettingsStore.read(),
            refreshing = refreshing,
            nowMillis = System.currentTimeMillis(),
            zoneId = ZoneId.systemDefault()
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = SchedulePolicyUiState.Loading
    )

    private var refreshInProgress = false

    init {
        refreshIfStale()
    }

    fun refreshIfStale() {
        viewModelScope.launch {
            refreshSuspend(force = false)
        }
    }

    fun refresh() {
        viewModelScope.launch {
            refreshSuspend(force = true)
        }
    }

    fun retry() {
        viewModelScope.launch {
            refreshSuspend(force = true)
        }
    }

    private suspend fun refreshSuspend(force: Boolean) {
        if (refreshInProgress) return
        refreshInProgress = true
        _refreshing.value = true
        try {
            repository.refreshIfStale(force = force)
        } catch (e: CancellationException) {
            throw e
        } finally {
            _refreshing.value = false
            refreshInProgress = false
        }
    }
}
