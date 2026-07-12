package com.pim.core.auth

enum class AuthMode { Required, Anonymous }

data class AuthTokens(
    val accessToken: String,
    val refreshToken: String,
    val expiresAtUtcMillis: Long
)

data class AuthSessionSnapshot(
    val tokens: AuthTokens?,
    val serverIdentity: String? = null
)

interface AuthSessionStore {
    fun snapshot(): AuthSessionSnapshot
    fun save(
        accessToken: String,
        refreshToken: String,
        expiresAtUtcMillis: Long,
        serverIdentity: String
    ): Boolean
    fun clear(): Boolean

    fun accessToken(): String? = snapshot().tokens?.accessToken
    fun refreshToken(): String? = snapshot().tokens?.refreshToken
    fun expiresAtUtcMillis(): Long? = snapshot().tokens?.expiresAtUtcMillis
    fun accessTokenForServerIdentity(serverIdentity: String): String? {
        val current = snapshot()
        return current.tokens?.accessToken?.takeIf { current.serverIdentity == serverIdentity }
    }
}

sealed interface AuthRefreshResult {
    data class Success(val tokens: AuthTokens) : AuthRefreshResult
    data object Rejected : AuthRefreshResult
}

fun interface AuthRefreshOperation {
    suspend fun refresh(refreshToken: String, serverIdentity: String): AuthRefreshResult
}
