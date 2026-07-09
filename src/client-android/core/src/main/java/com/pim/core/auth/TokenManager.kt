package com.pim.core.auth

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKeys

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

    fun saveTokens(accessToken: String, refreshToken: String, expiresAt: String? = null) {
        val expiryMillis = parseExpiry(expiresAt)
        prefs.edit()
            .putString("access_token", accessToken)
            .putString("refresh_token", refreshToken)
            .putLong("expires_at", expiryMillis)
            .apply()
    }

    fun getAccessToken(): String? = prefs.getString("access_token", null)
    fun getRefreshToken(): String? = prefs.getString("refresh_token", null)

    fun isExpired(): Boolean {
        val expiresAt = prefs.getLong("expires_at", 0)
        // Refresh when less than 5 minutes remaining to avoid edge cases
        return System.currentTimeMillis() >= expiresAt - 5 * 60 * 1000L
    }

    fun clear() = prefs.edit().clear().apply()

    private fun parseExpiry(serverExpiresAt: String?): Long {
        if (serverExpiresAt.isNullOrBlank()) {
            // Fallback: 24 hours if server didn't send expiry
            return System.currentTimeMillis() + 24 * 60 * 60 * 1000L
        }
        return try {
            java.time.Instant.parse(serverExpiresAt).toEpochMilli()
        } catch (e: Exception) {
            Log.w("TokenManager", "Failed to parse server expiresAt: $serverExpiresAt", e)
            System.currentTimeMillis() + 24 * 60 * 60 * 1000L
        }
    }
}
