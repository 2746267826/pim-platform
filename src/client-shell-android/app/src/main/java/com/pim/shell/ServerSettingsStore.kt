package com.pim.shell

import android.content.Context

class ServerSettingsStore(context: Context) {
    private val prefs = context.getSharedPreferences("shell_settings", Context.MODE_PRIVATE)

    fun serverUrl(): String? {
        val raw = prefs.getString(KEY_SERVER_URL, null) ?: return null
        return normalize(raw)
    }

    fun saveServerUrl(raw: String): String? {
        val normalized = normalize(raw) ?: return null
        prefs.edit().putString(KEY_SERVER_URL, normalized).apply()
        return normalized
    }

    fun clear() = prefs.edit().remove(KEY_SERVER_URL).apply()

    companion object {
        private const val KEY_SERVER_URL = "server_url"

        fun normalize(input: String?): String? {
            if (input.isNullOrBlank()) return null
            var candidate = input.trim()
            val schemeRegex = Regex("^[a-zA-Z][a-zA-Z0-9+.-]*://.*")
            if (!candidate.matches(schemeRegex)) {
                candidate = "https://$candidate"
            } else if (!candidate.startsWith("http://", ignoreCase = true) &&
                !candidate.startsWith("https://", ignoreCase = true)) {
                return null
            }
            candidate = candidate.trimEnd('/')
            return try {
                val uri = java.net.URI(candidate)
                val scheme = uri.scheme?.lowercase()
                if ((scheme != "http" && scheme != "https") || uri.host.isNullOrEmpty()) return null
                candidate
            } catch (_: Exception) { null }
        }

        fun isInsecure(url: String): Boolean = url.startsWith("http://", ignoreCase = true)
    }
}
