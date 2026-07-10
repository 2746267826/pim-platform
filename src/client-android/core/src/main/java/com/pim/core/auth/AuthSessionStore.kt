package com.pim.core.auth

enum class AuthMode { Required, Anonymous }

interface AuthSessionStore {
    fun accessToken(): String?
    fun refreshToken(): String?
    fun expiresAtUtcMillis(): Long?
    fun save(accessToken: String, refreshToken: String, expiresAtUtcMillis: Long)
    fun clear()
}

fun interface AuthRefreshOperation {
    suspend fun refresh(refreshToken: String): Boolean
}
