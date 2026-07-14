package com.pim.app.status

import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

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
class StatusAcceptedSignal internal constructor(
    private val scope: CoroutineScope,
    private val timeoutMillis: Long
) {
    @Inject
    constructor() : this(
        scope = CoroutineScope(SupervisorJob() + Dispatchers.Default),
        timeoutMillis = ACCEPTED_FALLBACK_MILLIS
    )

    private val lock = Any()
    private val _state = MutableStateFlow(StatusAcceptedState())
    internal val state: StateFlow<StatusAcceptedState> = _state.asStateFlow()

    init {
        require(timeoutMillis >= 0L)
    }

    fun trigger(): Long {
        val generation = synchronized(lock) {
            val nextGeneration = _state.value.generation + 1L
            _state.value = StatusAcceptedState(nextGeneration, isAccepted = true)
            nextGeneration
        }
        scope.launch {
            delay(timeoutMillis)
            clearIfGeneration(generation)
        }
        return generation
    }

    fun clearIfGeneration(generation: Long) {
        synchronized(lock) {
            val current = _state.value
            if (current.isAccepted && current.generation == generation) {
                _state.value = current.copy(isAccepted = false)
            }
        }
    }

    private companion object {
        const val ACCEPTED_FALLBACK_MILLIS = 10_000L
    }
}

internal data class StatusAcceptedState(
    val generation: Long = 0L,
    val isAccepted: Boolean = false
)

@Singleton
class StatusRefreshSignal @Inject constructor() {
    private val _version = MutableStateFlow(0L)
    val version: StateFlow<Long> = _version.asStateFlow()

    fun requestRefresh() {
        _version.update { it + 1L }
    }
}
