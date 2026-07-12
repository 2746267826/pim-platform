package com.pim.core.network

import com.pim.core.auth.AuthRefreshOperation
import com.pim.core.auth.AuthRefreshResult
import com.pim.core.auth.AuthSessionSnapshot
import com.pim.core.auth.AuthSessionStore
import com.pim.core.auth.AuthTokens
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class AuthRefreshCoordinator(
    private val sessionStore: AuthSessionStore,
    private val refreshOperation: AuthRefreshOperation,
    private val nowMillis: () -> Long = System::currentTimeMillis
) {
    private val refreshMutex = Mutex()

    suspend fun refreshIfExpired(serverIdentity: String? = null): Boolean {
        val observed = sessionStore.snapshot()
        if (!isBoundToRequiredServer(observed, serverIdentity)) return false
        if (!isExpired(observed.tokens)) return true

        return refreshMutex.withLock {
            val current = sessionStore.snapshot()
            if (!isBoundToRequiredServer(current, serverIdentity)) return@withLock false
            if (current != observed) {
                return@withLock isValidCompletedRefresh(
                    current,
                    RefreshRequirement.Expiry,
                    serverIdentity
                )
            }
            if (!isExpired(current.tokens)) return@withLock true
            refreshLocked(current, RefreshRequirement.Expiry, serverIdentity)
        }
    }

    suspend fun refreshAfterUnauthorized(
        failedAccessToken: String?,
        serverIdentity: String? = null
    ): Boolean {
        val requirement = RefreshRequirement.Forced(failedAccessToken.nonblank())
        val observed = sessionStore.snapshot()
        if (!isBoundToRequiredServer(observed, serverIdentity)) return false
        if (isValidCompletedRefresh(observed, requirement, serverIdentity)) return true

        return refreshMutex.withLock {
            val current = sessionStore.snapshot()
            if (!isBoundToRequiredServer(current, serverIdentity)) return@withLock false
            if (isValidCompletedRefresh(current, requirement, serverIdentity)) return@withLock true
            if (current != observed) {
                return@withLock false
            }
            refreshLocked(current, requirement, serverIdentity)
        }
    }

    private suspend fun refreshLocked(
        expected: AuthSessionSnapshot,
        requirement: RefreshRequirement,
        serverIdentity: String?
    ): Boolean {
        val refreshToken = expected.tokens?.refreshToken.nonblank()
        if (refreshToken == null) {
            return clearExpectedOrValidateCurrent(expected, requirement, serverIdentity)
        }

        val refreshServerIdentity = expected.serverIdentity
            ?: return clearExpectedOrValidateCurrent(expected, requirement, serverIdentity)
        return when (
            val result = refreshOperation.refresh(refreshToken, refreshServerIdentity)
        ) {
            AuthRefreshResult.Rejected -> {
                clearExpectedOrValidateCurrent(expected, requirement, serverIdentity)
            }
            is AuthRefreshResult.Success -> {
                if (!isValidRefreshTokens(result.tokens, requirement)) {
                    return clearExpectedOrValidateCurrent(expected, requirement, serverIdentity)
                }
                if (sessionStore.compareAndSave(expected, result.tokens)) {
                    true
                } else {
                    isValidCompletedRefresh(
                        sessionStore.snapshot(),
                        requirement,
                        serverIdentity
                    )
                }
            }
        }
    }

    private fun clearExpectedOrValidateCurrent(
        expected: AuthSessionSnapshot,
        requirement: RefreshRequirement,
        serverIdentity: String?
    ): Boolean {
        if (sessionStore.clearIfUnchanged(expected)) return false
        return isValidCompletedRefresh(sessionStore.snapshot(), requirement, serverIdentity)
    }

    private fun isValidCompletedRefresh(
        snapshot: AuthSessionSnapshot,
        requirement: RefreshRequirement,
        serverIdentity: String?
    ): Boolean {
        if (!isBoundToRequiredServer(snapshot, serverIdentity)) return false
        return isValidRefreshTokens(snapshot.tokens, requirement)
    }

    private fun isValidRefreshTokens(
        tokens: AuthTokens?,
        requirement: RefreshRequirement
    ): Boolean {
        if (!isValidSession(tokens)) return false
        return when (requirement) {
            RefreshRequirement.Expiry -> true
            is RefreshRequirement.Forced -> {
                requirement.failedAccessToken == null ||
                    tokens?.accessToken != requirement.failedAccessToken
            }
        }
    }

    private fun isBoundToRequiredServer(
        snapshot: AuthSessionSnapshot,
        serverIdentity: String?
    ): Boolean {
        if (snapshot.tokens == null || serverIdentity == null) return true
        return snapshot.serverIdentity == serverIdentity
    }

    private fun isValidSession(tokens: AuthTokens?): Boolean {
        return tokens != null &&
            tokens.accessToken.isNotBlank() &&
            tokens.refreshToken.isNotBlank() &&
            tokens.expiresAtUtcMillis > nowMillis()
    }

    private fun isExpired(tokens: AuthTokens?): Boolean {
        return tokens != null && nowMillis() >= tokens.expiresAtUtcMillis
    }

    private fun String?.nonblank(): String? = this?.takeIf(String::isNotBlank)

    private sealed interface RefreshRequirement {
        data object Expiry : RefreshRequirement
        data class Forced(val failedAccessToken: String?) : RefreshRequirement
    }
}
