package com.pim.shell

class HealthChecker(private val fetchStatus: (String) -> Int) {
    fun check(rawUrl: String): String? {
        val normalized = ServerSettingsStore.normalize(rawUrl) ?: return null
        return try {
            if (fetchStatus("$normalized/health") in 200..299) normalized else null
        } catch (_: Exception) { null }
    }
}
