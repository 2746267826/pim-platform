package com.pim.core.auth

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.pim.core.settings.PimServerEndpoints
import java.time.Instant

interface SecurePreferencesFactory {
    fun open(): SharedPreferences
}

open class SecureStorageCorruptionException(
    message: String,
    cause: Throwable? = null
) : Exception(message, cause)

class SecureStorageUnavailableException(
    message: String,
    cause: Throwable? = null
) : Exception(message, cause)

class AndroidSecurePreferencesFactory(
    private val context: Context
) : SecurePreferencesFactory {
    override fun open(): SharedPreferences {
        val masterKey = try {
            MasterKey.Builder(context)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build()
        } catch (failure: Exception) {
            throw SecureStorageUnavailableException("Android master key is unavailable", failure)
        }
        return try {
            EncryptedSharedPreferences.create(
                context,
                PREFS_NAME,
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )
        } catch (failure: Exception) {
            throw SecureStorageCorruptionException("Encrypted preferences could not be opened", failure)
        }
    }

    private companion object {
        const val PREFS_NAME = "pim_auth"
    }
}

class TokenManager(
    private val securePreferencesFactory: SecurePreferencesFactory,
    private val nowMillis: () -> Long = System::currentTimeMillis,
    private val reportStorageError: (String, Throwable) -> Unit = { _, _ -> }
) : AuthSessionStore {
    constructor(context: Context) : this(
        securePreferencesFactory = AndroidSecurePreferencesFactory(context.applicationContext),
        reportStorageError = { message, failure -> Log.e(TAG, message, failure) }
    )

    private val lock = Any()
    private var prefs: SharedPreferences?
    private var currentSnapshot: AuthSessionSnapshot

    init {
        val (p, snapshot) = try {
            val prefs = securePreferencesFactory.open()
            val session = prefs.readSession()
            Pair(prefs, session)
        } catch (failure: Exception) {
            reportStorageError("Secure token storage initialization failed", failure)
            Pair(null, null)
        }
        prefs = p
        currentSnapshot = AuthSessionSnapshot(
            tokens = snapshot?.tokens,
            serverIdentity = snapshot?.serverIdentity
        )
    }

    fun saveTokens(
        accessToken: String,
        refreshToken: String,
        expiresAt: String?,
        serverUrl: String
    ): Boolean {
        val expiresAtUtcMillis = expiresAt
            ?.takeIf(String::isNotBlank)
            ?.let { runCatching { Instant.parse(it).toEpochMilli() }.getOrNull() }
            ?: return false
        val serverIdentity = runCatching {
            PimServerEndpoints.from(serverUrl).trustedOrigin
        }.getOrNull() ?: return false
        return save(accessToken, refreshToken, expiresAtUtcMillis, serverIdentity)
    }

    override fun snapshot(): AuthSessionSnapshot = synchronized(lock) { currentSnapshot }

    override fun save(
        accessToken: String,
        refreshToken: String,
        expiresAtUtcMillis: Long,
        serverIdentity: String
    ): Boolean {
        val tokens = AuthTokens(accessToken, refreshToken, expiresAtUtcMillis)
        if (!tokens.isValidAt(nowMillis())) return false
        val normalizedIdentity = runCatching {
            PimServerEndpoints.normalizeTrustedOrigin(serverIdentity)
        }.getOrNull() ?: return false

        return synchronized(lock) {
            try {
                if (prefs == null) throw IllegalStateException("Secure storage is unavailable")
                val committed = prefs!!.edit()
                    .putString("access_token", tokens.accessToken)
                    .putString("refresh_token", tokens.refreshToken)
                    .putLong("expires_at", tokens.expiresAtUtcMillis)
                    .putString("server_identity", normalizedIdentity)
                    .commit()
                if (!committed) throw IllegalStateException("Secure token storage write failed")
                currentSnapshot = AuthSessionSnapshot(
                    tokens = tokens,
                    serverIdentity = normalizedIdentity
                )
                true
            } catch (failure: Exception) {
                reportStorageError("Token save failed", failure)
                currentSnapshot = AuthSessionSnapshot(null)
                prefs = null
                false
            }
        }
    }

    override fun clear(): Boolean {
        return synchronized(lock) {
            try {
                if (prefs == null) throw IllegalStateException("Secure storage is unavailable")
                val committed = prefs!!.edit().clear().commit()
                if (!committed) throw IllegalStateException("Secure token storage clear failed")
                currentSnapshot = AuthSessionSnapshot(null)
                true
            } catch (failure: Exception) {
                reportStorageError("Token clear failed", failure)
                currentSnapshot = AuthSessionSnapshot(null)
                prefs = null
                false
            }
        }
    }

    fun getAccessToken(): String? = accessToken()

    fun getAccessTokenForServer(serverUrl: String): String? {
        val serverIdentity = runCatching {
            PimServerEndpoints.from(serverUrl).trustedOrigin
        }.getOrNull() ?: return null
        return accessTokenForServerIdentity(serverIdentity)
    }

    fun isExpiredForServer(serverUrl: String): Boolean {
        val serverIdentity = runCatching {
            PimServerEndpoints.from(serverUrl).trustedOrigin
        }.getOrNull() ?: return true
        val current = snapshot()
        if (current.serverIdentity != serverIdentity) return true
        return current.tokens?.expiresAtUtcMillis?.let { nowMillis() >= it } ?: true
    }


    fun isExpired(): Boolean {
        return expiresAtUtcMillis()?.let { nowMillis() >= it } ?: true
    }

    private fun SharedPreferences.readSession(): StoredSession? {
        val accessToken = getString("access_token", null)?.takeIf(String::isNotBlank)
        val refreshToken = getString("refresh_token", null)?.takeIf(String::isNotBlank)
        val serverIdentity = getString("server_identity", null)
            ?.takeIf(String::isNotBlank)
            ?.let { runCatching { PimServerEndpoints.normalizeTrustedOrigin(it) }.getOrNull() }
        val hasExpiry = contains("expires_at")
        if (accessToken == null || refreshToken == null || serverIdentity == null || !hasExpiry) {
            return null
        }
        return StoredSession(
            tokens = AuthTokens(accessToken, refreshToken, getLong("expires_at", 0L)),
            serverIdentity = serverIdentity
        )
    }

    private fun AuthTokens.isValidAt(now: Long): Boolean {
        return accessToken.isNotBlank() &&
            refreshToken.isNotBlank() &&
            expiresAtUtcMillis > now
    }

    private data class StoredSession(
        val tokens: AuthTokens,
        val serverIdentity: String
    )

    private companion object {
        const val TAG = "TokenManager"
    }
}
