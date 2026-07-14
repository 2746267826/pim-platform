package com.pim.app.status

import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class StatusSyncActionRunner(
    private val syncNow: suspend () -> Unit,
    private val refresh: () -> Unit,
    private val acceptedSignal: StatusAcceptedSignal
) {
    suspend fun run(route: StatusActionRoute) {
        if (route != StatusActionRoute.TriggerSync) return
        try {
            syncNow()
            acceptedSignal.trigger()
        } finally {
            refresh()
        }
    }
}

@Singleton
class StatusAcceptedSignal @Inject constructor() {
    private val _accepted = MutableStateFlow(false)
    val accepted: StateFlow<Boolean> = _accepted.asStateFlow()

    fun trigger() {
        _accepted.value = true
    }

    fun clearIfSet() {
        if (_accepted.value) _accepted.value = false
    }
}

@Singleton
class StatusRefreshSignal @Inject constructor() {
    private val _version = MutableStateFlow(0L)
    val version: StateFlow<Long> = _version.asStateFlow()

    fun requestRefresh() {
        _version.update { it + 1L }
    }
}
