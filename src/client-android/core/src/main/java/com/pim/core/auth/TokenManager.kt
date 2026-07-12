package com.pim.core.auth

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKeys
import com.pim.core.settings.PimServerEndpoints
import java.time.Instant

enum class SecureStorageStatus { Available, Recovered, Ephemeral }

interface SecurePreferencesFactory {
    fun clearLegacyStorage() = Unit
    fun open(): SharedPreferences
    fun reset()
    fun markSessionInvalidated(): Boolean = false
    fun hasSessionInvalidationTombstone(): Boolean = false
    fun clearSessionInvalidationTombstone() = Unit
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
            MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC)
        } catch (failure: Exception) {
            throw SecureStorageUnavailableException("Android master key is unavailable", failure)
        }
        return try {
            EncryptedSharedPreferences.create(
                PREFS_NAME,
                masterKey,
                context,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )
        } catch (failure: Exception) {
            throw SecureStorageCorruptionException("Encrypted preferences could not be opened", failure)
        }
    }

    override fun clearLegacyStorage() {
        context.deleteSharedPreferences(LEGACY_FALLBACK_PREFS_NAME)
    }

    override fun reset() {
        if (!context.deleteSharedPreferences(PREFS_NAME)) {
            throw IllegalStateException("Encrypted token preferences could not be deleted")
        }
        context.deleteSharedPreferences(LEGACY_FALLBACK_PREFS_NAME)
    }

    override fun markSessionInvalidated(): Boolean {
        return statePreferences()
            .edit()
            .putBoolean(KEY_SESSION_INVALIDATED, true)
            .commit()
    }

    override fun hasSessionInvalidationTombstone(): Boolean {
        return statePreferences().getBoolean(KEY_SESSION_INVALIDATED, false)
    }

    override fun clearSessionInvalidationTombstone() {
        val committed = statePreferences()
            .edit()
            .remove(KEY_SESSION_INVALIDATED)
            .commit()
        check(committed) { "Secure session invalidation tombstone could not be cleared" }
    }

    private fun statePreferences(): SharedPreferences {
        return context.getSharedPreferences(STATE_PREFS_NAME, Context.MODE_PRIVATE)
    }

    private companion object {
        const val PREFS_NAME = "pim_auth"
        const val LEGACY_FALLBACK_PREFS_NAME = "pim_auth_fallback"
        const val STATE_PREFS_NAME = "pim_auth_state"
        const val KEY_SESSION_INVALIDATED = "session_invalidated"
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
    @Volatile
    var storageStatus: SecureStorageStatus
        private set
    private var currentSnapshot: AuthSessionSnapshot

    init {
        val initialized = initializeStorage()
        prefs = initialized.preferences
        storageStatus = initialized.status
        currentSnapshot = AuthSessionSnapshot(
            tokens = initialized.tokens,
            generation = 0L,
            serverIdentity = initialized.serverIdentity
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
            if (!persist(tokens, normalizedIdentity)) return@synchronized false
            currentSnapshot = AuthSessionSnapshot(
                tokens = tokens,
                generation = currentSnapshot.generation + 1L,
                serverIdentity = normalizedIdentity
            )
            true
        }
    }

    override fun compareAndSave(expected: AuthSessionSnapshot, tokens: AuthTokens): Boolean {
        if (!tokens.isValidAt(nowMillis())) return false

        return synchronized(lock) {
            val serverIdentity = expected.serverIdentity ?: return@synchronized false
            if (currentSnapshot != expected || !persist(tokens, serverIdentity)) {
                return@synchronized false
            }
            currentSnapshot = AuthSessionSnapshot(
                tokens = tokens,
                generation = currentSnapshot.generation + 1L,
                serverIdentity = serverIdentity
            )
            true
        }
    }

    override fun clear(): Boolean {
        return synchronized(lock) {
            if (!clearPersistedTokens()) return@synchronized false
            currentSnapshot = AuthSessionSnapshot(null, currentSnapshot.generation + 1L)
            true
        }
    }

    override fun clearIfUnchanged(expected: AuthSessionSnapshot): Boolean {
        return synchronized(lock) {
            if (currentSnapshot != expected) return@synchronized false
            if (!clearPersistedTokens()) return@synchronized false
            currentSnapshot = AuthSessionSnapshot(null, currentSnapshot.generation + 1L)
            true
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

    fun clearIfBoundToDifferentServer(serverUrl: String): Boolean {
        val serverIdentity = runCatching {
            PimServerEndpoints.from(serverUrl).trustedOrigin
        }.getOrNull() ?: return false
        return synchronized(lock) {
            val current = currentSnapshot
            if (current.tokens == null || current.serverIdentity == serverIdentity) {
                return@synchronized false
            }
            if (!clearPersistedTokens()) return@synchronized false
            currentSnapshot = AuthSessionSnapshot(
                tokens = null,
                generation = current.generation + 1L,
                serverIdentity = null
            )
            true
        }
    }

    fun isExpired(): Boolean {
        return expiresAtUtcMillis()?.let { nowMillis() >= it } ?: true
    }

    private fun initializeStorage(): InitializedStorage {
        runCatching { securePreferencesFactory.clearLegacyStorage() }
            .onFailure { failure ->
                reportStorageError("Legacy plaintext token cleanup failed", failure)
            }
        val invalidated = try {
            securePreferencesFactory.hasSessionInvalidationTombstone()
        } catch (failure: Exception) {
            return ephemeralStorage(failure)
        }
        if (invalidated) return recoverInvalidatedStorage()
        val firstFailure = try {
            return openAndRead(SecureStorageStatus.Available)
        } catch (failure: Exception) {
            failure
        }
        reportStorageError(
            "Secure token storage initialization failed; retrying without reset",
            firstFailure
        )

        val secondFailure = try {
            return openAndRead(SecureStorageStatus.Recovered)
        } catch (failure: Exception) {
            failure
        }
        if (
            firstFailure is SecureStorageCorruptionException &&
            secondFailure is SecureStorageCorruptionException
        ) {
            return recoverCorruptedStorage(secondFailure)
        }
        return ephemeralStorage(secondFailure)
    }

    private fun recoverInvalidatedStorage(): InitializedStorage {
        return try {
            val recovered = securePreferencesFactory.open()
            if (!recovered.edit().clear().commit()) {
                error("Invalidated secure token storage could not be cleared")
            }
            securePreferencesFactory.clearSessionInvalidationTombstone()
            InitializedStorage(
                preferences = recovered,
                status = SecureStorageStatus.Recovered,
                tokens = null,
                serverIdentity = null
            )
        } catch (failure: Exception) {
            ephemeralStorage(failure)
        }
    }

    private fun openAndRead(status: SecureStorageStatus): InitializedStorage {
        val available = securePreferencesFactory.open()
        val session = try {
            available.readSession()
        } catch (failure: Exception) {
            throw SecureStorageCorruptionException(
                "Encrypted preferences could not be read",
                failure
            )
        }
        return InitializedStorage(
            preferences = available,
            status = status,
            tokens = session?.tokens,
            serverIdentity = session?.serverIdentity
        )
    }

    private fun recoverCorruptedStorage(readFailure: Exception): InitializedStorage {
        reportStorageError("Secure token storage is unreadable; resetting it", readFailure)
        return try {
            securePreferencesFactory.reset()
            val recovered = securePreferencesFactory.open()
            if (!recovered.edit().clear().commit()) {
                error("Recovered secure token storage could not be cleared")
            }
            val session = try {
                recovered.readSession()
            } catch (failure: Exception) {
                throw SecureStorageCorruptionException(
                    "Recovered encrypted preferences could not be read",
                    failure
                )
            }
            InitializedStorage(
                preferences = recovered,
                status = SecureStorageStatus.Recovered,
                tokens = session?.tokens,
                serverIdentity = session?.serverIdentity
            )
        } catch (recoveryFailure: Exception) {
            ephemeralStorage(recoveryFailure)
        }
    }

    private fun ephemeralStorage(failure: Exception): InitializedStorage {
        reportStorageError(
            "Secure token storage unavailable; using ephemeral session",
            failure
        )
        return InitializedStorage(null, SecureStorageStatus.Ephemeral, null, null)
    }

    private fun persist(tokens: AuthTokens, serverIdentity: String): Boolean {
        val storage = prefs
        if (storage == null) {
            return invalidateInaccessibleStorage()
        }
        val committed = runCatching {
            storage.edit()
                .putString("access_token", tokens.accessToken)
                .putString("refresh_token", tokens.refreshToken)
                .putLong("expires_at", tokens.expiresAtUtcMillis)
                .putString("server_identity", serverIdentity)
                .commit()
        }.getOrDefault(false)
        if (!committed) {
            return degradeToEphemeral("Secure token storage write failed")
        }
        return true
    }

    private fun clearPersistedTokens(): Boolean {
        val storage = prefs
        if (storage == null) {
            return invalidateInaccessibleStorage()
        }
        val committed = runCatching { storage.edit().clear().commit() }.getOrDefault(false)
        if (!committed) {
            return degradeToEphemeral("Secure token storage clear failed")
        }
        return true
    }

    private fun degradeToEphemeral(message: String): Boolean {
        val failure = IllegalStateException(message)
        reportStorageError("$message; using ephemeral session", failure)
        val marked = markSessionInvalidated()
        val reset = runCatching { securePreferencesFactory.reset() }
            .onFailure { resetFailure ->
                reportStorageError("Secure token storage reset failed", resetFailure)
            }
            .isSuccess
        prefs = null
        storageStatus = SecureStorageStatus.Ephemeral
        return marked || reset
    }

    private fun invalidateInaccessibleStorage(): Boolean {
        val marked = markSessionInvalidated()
        val reset = runCatching { securePreferencesFactory.reset() }
            .onFailure { failure ->
                reportStorageError("Inaccessible secure token storage reset failed", failure)
            }
            .isSuccess
        return marked || reset
    }

    private fun markSessionInvalidated(): Boolean {
        return runCatching { securePreferencesFactory.markSessionInvalidated() }
            .onFailure { failure ->
                reportStorageError("Secure session invalidation tombstone failed", failure)
            }
            .getOrDefault(false)
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

    private data class InitializedStorage(
        val preferences: SharedPreferences?,
        val status: SecureStorageStatus,
        val tokens: AuthTokens?,
        val serverIdentity: String?
    )

    private data class StoredSession(
        val tokens: AuthTokens,
        val serverIdentity: String
    )

    private companion object {
        const val TAG = "TokenManager"
    }
}
