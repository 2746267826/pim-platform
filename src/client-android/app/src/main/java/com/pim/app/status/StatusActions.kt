package com.pim.app.status

import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class StatusSyncActionRunner(
    private val syncNow: suspend () -> Unit,
    private val refresh: () -> Unit
) {
    suspend fun run(route: StatusActionRoute) {
        if (route != StatusActionRoute.TriggerSync) return
        syncNow()
        refresh()
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
