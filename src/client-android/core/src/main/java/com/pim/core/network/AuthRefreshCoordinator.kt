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
        if (observed.tokens == null) return false
        if (!isBoundToRequiredServer(observed, serverIdentity)) return false
        if (!isExpired(observed.tokens)) return true

        return refreshMutex.withLock {
            val current = sessionStore.snapshot()
            if (current.tokens == null) return@withLock false
            if (!isBoundToRequiredServer(current, serverIdentity)) return@withLock false
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
            refreshLocked(current, requirement, serverIdentity)
        }
    }

    private suspend fun refreshLocked(
        beforeRefresh: AuthSessionSnapshot,
        requirement: RefreshRequirement,
        serverIdentity: String?
    ): Boolean {
        val refreshToken = beforeRefresh.tokens?.refreshToken.nonblank()
        if (refreshToken == null) {
            sessionStore.clear()
            return isValidCompletedRefresh(sessionStore.snapshot(), requirement, serverIdentity)
        }

        val refreshServerIdentity = beforeRefresh.serverIdentity
            ?: return clearAndReject(requirement, serverIdentity)

        val reRead = sessionStore.snapshot()
        if (reRead.tokens?.refreshToken != refreshToken || reRead.serverIdentity != refreshServerIdentity) {
            return isValidCompletedRefresh(reRead, requirement, serverIdentity)
        }

        return when (
            val result = refreshOperation.refresh(refreshToken, refreshServerIdentity)
        ) {
            AuthRefreshResult.Rejected -> {
                val afterRefresh = sessionStore.snapshot()
                if (afterRefresh.tokens?.refreshToken != refreshToken ||
                    afterRefresh.serverIdentity != refreshServerIdentity
                ) {
                    return isValidCompletedRefresh(afterRefresh, requirement, serverIdentity)
                }
                clearAndReject(requirement, serverIdentity)
            }
            is AuthRefreshResult.Success -> {
                val afterRefresh = sessionStore.snapshot()
                if (afterRefresh.tokens?.refreshToken != refreshToken ||
                    afterRefresh.serverIdentity != refreshServerIdentity
                ) {
                    return isValidCompletedRefresh(afterRefresh, requirement, serverIdentity)
                }
                if (!isValidRefreshTokens(result.tokens, requirement)) {
                    return clearAndReject(requirement, serverIdentity)
                }
                sessionStore.save(
                    result.tokens.accessToken,
                    result.tokens.refreshToken,
                    result.tokens.expiresAtUtcMillis,
                    refreshServerIdentity
                )
            }
        }
    }

    private fun clearAndReject(
        requirement: RefreshRequirement,
        serverIdentity: String?
    ): Boolean {
        sessionStore.clear()
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