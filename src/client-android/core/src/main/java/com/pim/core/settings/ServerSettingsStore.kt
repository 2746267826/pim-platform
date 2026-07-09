package com.pim.core.settings

import android.content.Context
import android.content.SharedPreferences
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ServerSettingsStore @Inject constructor(
    @ApplicationContext context: Context
) {
    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun getBaseUrl(): String {
        return normalizeBaseUrl(prefs.getString(KEY_SERVER_BASE_URL, DEFAULT_BASE_URL))
    }

    fun setBaseUrl(baseUrl: String): String {
        val normalized = normalizeBaseUrl(baseUrl)
        prefs.edit()
            .putString(KEY_SERVER_BASE_URL, normalized)
            .apply()
        return normalized
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
