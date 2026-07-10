package com.pim.core.network

import com.pim.core.auth.AuthRefreshOperation
import com.pim.core.auth.AuthSessionStore
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class AuthRefreshCoordinator(
    private val sessionStore: AuthSessionStore,
    private val refreshOperation: AuthRefreshOperation,
    private val nowMillis: () -> Long = System::currentTimeMillis
) {
    private val refreshMutex = Mutex()

    @Volatile
    private var refreshGeneration = 0L

    suspend fun refreshIfExpired(): Boolean {
        val observedState = sessionState()
        if (!isExpired(observedState.expiresAtUtcMillis)) return true
        val observedGeneration = refreshGeneration

        return refreshMutex.withLock {
            val currentState = sessionState()
            if (refreshGeneration != observedGeneration) {
                return@withLock validateCompletedRefresh(
                    currentState,
                    RefreshRequirement.Expiry
                )
            }
            if (isValidSession(currentState)) {
                return@withLock true
            }

            refreshLocked(RefreshRequirement.Expiry)
        }
    }

    suspend fun refreshAfterUnauthorized(failedAccessToken: String?): Boolean {
        val observedState = sessionState()
        val observedGeneration = refreshGeneration
        val requirement = RefreshRequirement.Forced(failedAccessToken.nonblank())

        return refreshMutex.withLock {
            val currentState = sessionState()
            if (refreshGeneration != observedGeneration) {
                return@withLock validateCompletedRefresh(currentState, requirement)
            }
            if (
                (
                    currentState != observedState ||
                        currentState.accessToken != requirement.failedAccessToken
                ) &&
                isValidCompletedRefresh(currentState, requirement)
            ) {
                return@withLock true
            }

            refreshLocked(requirement)
        }
    }

    private suspend fun refreshLocked(requirement: RefreshRequirement): Boolean {
        val refreshToken = sessionStore.refreshToken().nonblank()
        if (refreshToken == null) {
            clearSessionOnce()
            return false
        }

        if (!refreshOperation.refresh(refreshToken)) {
            clearSessionOnce()
            return false
        }

        if (!isValidCompletedRefresh(sessionState(), requirement)) {
            clearSessionOnce()
            return false
        }

        refreshGeneration++
        return true
    }

    private fun validateCompletedRefresh(
        state: SessionState,
        requirement: RefreshRequirement
    ): Boolean {
        if (isValidCompletedRefresh(state, requirement)) return true
        clearSessionOnce()
        return false
    }

    private fun isValidCompletedRefresh(
        state: SessionState,
        requirement: RefreshRequirement
    ): Boolean {
        if (!isValidSession(state)) return false
        return when (requirement) {
            RefreshRequirement.Expiry -> true
            is RefreshRequirement.Forced -> {
                requirement.failedAccessToken == null ||
                    state.accessToken != requirement.failedAccessToken
            }
        }
    }

    private fun isValidSession(state: SessionState): Boolean {
        val expiry = state.expiresAtUtcMillis ?: return false
        return state.accessToken != null && expiry > nowMillis()
    }

    private fun sessionState(): SessionState {
        return SessionState(
            accessToken = sessionStore.accessToken().nonblank(),
            expiresAtUtcMillis = sessionStore.expiresAtUtcMillis()
        )
    }

    private fun clearSessionOnce() {
        val hasSession = sessionStore.accessToken() != null ||
            sessionStore.refreshToken() != null ||
            sessionStore.expiresAtUtcMillis() != null
        if (hasSession) sessionStore.clear()
    }

    private fun isExpired(expiresAtUtcMillis: Long?): Boolean {
        return expiresAtUtcMillis != null && nowMillis() >= expiresAtUtcMillis
    }

    private fun String?.nonblank(): String? = this?.takeIf(String::isNotBlank)

    private data class SessionState(
        val accessToken: String?,
        val expiresAtUtcMillis: Long?
    )

    private sealed interface RefreshRequirement {
        data object Expiry : RefreshRequirement
        data class Forced(val failedAccessToken: String?) : RefreshRequirement
    }
}
