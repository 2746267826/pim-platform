package com.pim.app.ui.location

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pim.app.location.acquisition.LocationAcquisitionCoordinator
import com.pim.app.location.service.ForegroundLocationController
import com.pim.app.location.service.ForegroundLocationService
import com.pim.app.status.QueueStatusRepository
import com.pim.app.status.QueueStatusSnapshot
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

@HiltViewModel
class LocationViewModel @Inject constructor(
    private val coordinator: LocationAcquisitionCoordinator,
    private val queueStatusRepository: QueueStatusRepository,
    private val controller: ForegroundLocationController
) : ViewModel() {

    val state: StateFlow<LocationUiState> = combine(
        coordinator.state,
        queueStatusRepository.observe(),
        ForegroundLocationService.runtimeState
    ) { acqState, queueSnapshot, runtime ->
        mapToLocationUiState(acqState, queueSnapshot, runtime)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = mapToLocationUiState(
            coordinator.state.value,
            QueueStatusSnapshot(0, 0, 0, 0, 0, 0)
        )
    )

    fun startOrRestart() {
        controller.startManualSession()
    }

    fun cancel() {
        val sessionId = coordinator.state.value.sessionId
        controller.cancelLocationSession(sessionId)
    }
}
