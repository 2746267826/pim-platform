package com.pim.app.status

import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class ConnectionProbeStore(
    private val preferences: SharedPreferences,
    private val json: Json
) {
    private val mutableResult = MutableStateFlow(load())

    val result: StateFlow<ConnectionProbeResult?> = mutableResult.asStateFlow()

    fun save(result: ConnectionProbeResult) {
        preferences.edit()
            .putString(KEY_RESULT, json.encodeToString(result))
            .apply()
        mutableResult.value = result
    }

    fun isFresh(nowMillis: Long): Boolean {
        val checkedAt = result.value?.checkedAtUtcMillis ?: return false
        val ageMillis = nowMillis - checkedAt
        return ageMillis >= 0L && ageMillis < FRESHNESS_MILLIS
    }

    private fun load(): ConnectionProbeResult? {
        val persisted = preferences.getString(KEY_RESULT, null) ?: return null
        return runCatching {
            json.decodeFromString<ConnectionProbeResult>(persisted)
        }.getOrNull()
    }

    private companion object {
        const val KEY_RESULT = "connection_probe_result"
        const val FRESHNESS_MILLIS = 5L * 60L * 1000L
    }
}
