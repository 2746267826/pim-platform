package com.pim.core.settings

import android.content.Context
import android.content.SharedPreferences
import com.pim.core.auth.AuthSessionStore
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

enum class ServerSessionCommitResult {
    Committed,
    ServerChanged,
    SaveFailed,
    InvalidServer
}

@Singleton
class ServerSettingsStore @Inject constructor(
    @ApplicationContext context: Context,
    private val authSessionStore: AuthSessionStore
) {
    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    @Synchronized
    fun getBaseUrl(): String {
        return normalizeBaseUrl(prefs.getString(KEY_SERVER_BASE_URL, DEFAULT_BASE_URL))
    }

    @Synchronized
    fun setBaseUrl(baseUrl: String): String {
        val validation = ServerUrlValidator.validate(baseUrl)
        require(validation.isValid) {
            "API address is not configured or invalid: ${validation.reasonCode ?: "unknown"}"
        }
        val normalized = validation.normalizedUrl
        val serverIdentity = PimServerEndpoints.from(normalized).trustedOrigin
        invalidateSessionBoundToAnotherServer(serverIdentity)
        val committed = prefs.edit()
            .putString(KEY_SERVER_BASE_URL, normalized)
            .commit()
        if (!committed) {
            throw IllegalStateException("API address could not be persisted")
        }
        return normalized
    }

    @Synchronized
    fun saveSessionIfCurrentServer(
        expectedServerIdentity: String,
        saveSession: () -> Boolean
    ): Boolean {
        return commitSessionIfCurrentServer(expectedServerIdentity, saveSession) ==
            ServerSessionCommitResult.Committed
    }

    @Synchronized
    fun commitSessionIfCurrentServer(
        expectedServerIdentity: String,
        saveSession: () -> Boolean
    ): ServerSessionCommitResult {
        val normalizedIdentity = runCatching {
            PimServerEndpoints.normalizeTrustedOrigin(expectedServerIdentity)
        }.getOrNull() ?: return ServerSessionCommitResult.InvalidServer
        val currentIdentity = runCatching {
            PimServerEndpoints.from(getBaseUrl()).trustedOrigin
        }.getOrNull() ?: return ServerSessionCommitResult.InvalidServer
        if (currentIdentity != normalizedIdentity) {
            return ServerSessionCommitResult.ServerChanged
        }
        return if (saveSession()) {
            ServerSessionCommitResult.Committed
        } else {
            ServerSessionCommitResult.SaveFailed
        }
    }

    private fun invalidateSessionBoundToAnotherServer(serverIdentity: String) {
        val current = authSessionStore.snapshot()
        if (current.tokens == null || current.serverIdentity == serverIdentity) return
        if (!authSessionStore.clear()) {
            throw IllegalStateException("Existing authentication session could not be cleared")
        }
    }

    companion object {
        const val DEFAULT_BASE_URL = ""
        const val KEY_SERVER_BASE_URL = "server_base_url"
        private const val PREFS_NAME = "pim_server_settings"

        fun normalizeBaseUrl(value: String?): String {
            return ServerUrlValidator.validate(value).normalizedUrl
        }
    }
}
