package com.pim.core.auth

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKeys
import java.time.Instant
import java.time.format.DateTimeParseException

class TokenManager(context: Context) {
    private val prefs: SharedPreferences

    init {
        val p = try {
            val masterKey = MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC)
            EncryptedSharedPreferences.create(
                "pim_auth",
                masterKey,
                context,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )
        } catch (e: Exception) {
            Log.w("TokenManager", "EncryptedSharedPreferences failed, falling back to plain SharedPreferences", e)
            context.getSharedPreferences("pim_auth_fallback", Context.MODE_PRIVATE)
        }
        prefs = p
    }

    fun saveTokens(accessToken: String, refreshToken: String) {
        saveTokens(accessToken, refreshToken, defaultExpiresAtMillis())
    }

    fun saveTokens(accessToken: String, refreshToken: String, expiresAtMillis: Long) {
        val safeExpiry = if (expiresAtMillis > 0L) expiresAtMillis else defaultExpiresAtMillis()
        prefs.edit()
            .putString("access_token", accessToken)
            .putString("refresh_token", refreshToken)
            .putLong("expires_at", safeExpiry)
            .apply()
    }

    fun saveTokens(accessToken: String, refreshToken: String, expiresAtIso: String?) {
        saveTokens(accessToken, refreshToken, TokenExpiry.parseIsoToMillis(expiresAtIso) ?: 0L)
    }

    fun getAccessToken(): String? = prefs.getString("access_token", null)
    fun getRefreshToken(): String? = prefs.getString("refresh_token", null)

    fun expiresAtMillis(): Long = prefs.getLong("expires_at", 0)

    fun isExpired(): Boolean {
        val expiresAt = prefs.getLong("expires_at", 0)
        if (expiresAt <= 0L) return getAccessToken().isNullOrBlank()
        // 提前 30 秒判定过期，避免请求途中刚好到期被服务端拒绝。
        return System.currentTimeMillis() >= expiresAt - SAFETY_MARGIN_MS
    }

    private fun defaultExpiresAtMillis(): Long =
        System.currentTimeMillis() + DEFAULT_ACCESS_TOKEN_TTL_MS

    private companion object {
        const val DEFAULT_ACCESS_TOKEN_TTL_MS = 15L * 60L * 1000L
        const val SAFETY_MARGIN_MS = 30L * 1000L
    }

    fun clear() = prefs.edit().clear().apply()
}

object TokenExpiry {
    fun parseIsoToMillis(expiresAt: String?): Long? {
        if (expiresAt.isNullOrBlank()) return null
        return try {
            Instant.parse(expiresAt).toEpochMilli()
        } catch (_: DateTimeParseException) {
            try {
                Instant.parse(expiresAt.replace(' ', 'T')).toEpochMilli()
            } catch (_: DateTimeParseException) {
                null
            }
        }
    }
}
