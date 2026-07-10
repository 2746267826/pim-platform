package com.pim.core.auth

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKeys

class TokenManager(context: Context) : AuthSessionStore {
    private val prefs: SharedPreferences = try {
        val masterKey = MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC)
        EncryptedSharedPreferences.create(
            "pim_auth",
            masterKey,
            context,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    } catch (e: Exception) {
        Log.e(TAG, "Secure token storage initialization failed")
        throw IllegalStateException("Secure token storage is unavailable", e)
    }

    fun saveTokens(accessToken: String, refreshToken: String, expiresAt: String? = null) {
        save(accessToken, refreshToken, parseExpiry(expiresAt))
    }

    override fun save(accessToken: String, refreshToken: String, expiresAtUtcMillis: Long) {
        prefs.edit()
            .putString("access_token", accessToken)
            .putString("refresh_token", refreshToken)
            .putLong("expires_at", expiresAtUtcMillis)
            .apply()
    }

    override fun accessToken(): String? = prefs.getString("access_token", null)

    override fun refreshToken(): String? = prefs.getString("refresh_token", null)

    override fun expiresAtUtcMillis(): Long? {
        return if (prefs.contains("expires_at")) prefs.getLong("expires_at", 0L) else null
    }

    fun getAccessToken(): String? = accessToken()

    fun isExpired(): Boolean {
        return expiresAtUtcMillis()?.let { System.currentTimeMillis() >= it } ?: true
    }

    override fun clear() {
        prefs.edit().clear().apply()
    }

    private fun parseExpiry(serverExpiresAt: String?): Long {
        if (serverExpiresAt.isNullOrBlank()) {
            // Fallback: 24 hours if server didn't send expiry
            return System.currentTimeMillis() + 24 * 60 * 60 * 1000L
        }
        return try {
            java.time.Instant.parse(serverExpiresAt).toEpochMilli()
        } catch (_: Exception) {
            Log.w(TAG, "Failed to parse server token expiry")
            System.currentTimeMillis() + 24 * 60 * 60 * 1000L
        }
    }

    private companion object {
        const val TAG = "TokenManager"
    }
}
