package com.pim.app.status

import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

interface ConnectionProbeEvidenceStore {
    val result: StateFlow<ConnectionProbeResult?>
    fun save(result: ConnectionProbeResult): Boolean
    fun freshResult(serverIdentity: String, nowMillis: Long): ConnectionProbeResult?
}

class ConnectionProbeStore(
    private val preferences: SharedPreferences,
    private val json: Json,
    private val nowMillis: () -> Long = System::currentTimeMillis
) : ConnectionProbeEvidenceStore {
    private val lock = Any()
    private val mutableResult = MutableStateFlow(load())

    override val result: StateFlow<ConnectionProbeResult?> = mutableResult.asStateFlow()

    override fun save(result: ConnectionProbeResult): Boolean {
        return synchronized(lock) {
            val current = mutableResult.value
            val staleSameServer = current != null
                && current.serverIdentity == result.serverIdentity
                && result.checkedAtUtcMillis < current.checkedAtUtcMillis
                && current.checkedAtUtcMillis <= nowMillis()
            if (staleSameServer) {
                false
            } else {
                val encoded = json.encodeToString(result)
                val committed = preferences.edit()
                    .putString(KEY_RESULT, encoded)
                    .commit()
                if (committed) mutableResult.value = result
                committed
            }
        }
    }

    fun clear(): Boolean {
        return synchronized(lock) {
            val committed = preferences.edit()
                .remove(KEY_RESULT)
                .commit()
            if (committed) mutableResult.value = null
            committed
        }
    }

    fun isFresh(serverIdentity: String, nowMillis: Long): Boolean {
        return freshResult(serverIdentity, nowMillis) != null
    }

    override fun freshResult(
        serverIdentity: String,
        nowMillis: Long
    ): ConnectionProbeResult? {
        val current = result.value ?: return null
        if (current.serverIdentity != serverIdentity) return null
        val checkedAt = current.checkedAtUtcMillis
        val ageMillis = nowMillis - checkedAt
        return current.takeIf { ageMillis >= 0L && ageMillis < FRESHNESS_MILLIS }
    }

    private fun load(): ConnectionProbeResult? {
        val persisted = preferences.getString(KEY_RESULT, null) ?: return null
        return runCatching {
            json.decodeFromString<ConnectionProbeResult>(persisted)
        }.getOrNull()
    }

    companion object {
        const val KEY_RESULT = "connection_probe_result"
        const val FRESHNESS_MILLIS = 5L * 60L * 1000L
    }
}
