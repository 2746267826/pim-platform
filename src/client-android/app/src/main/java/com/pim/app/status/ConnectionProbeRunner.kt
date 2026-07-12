package com.pim.app.status

import com.pim.core.settings.PimServerEndpoints
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

fun interface ConnectionProbe {
    suspend fun probe(serverUrl: String): ConnectionProbeResult
}

class ConnectionProbeRunner(
    private val probe: ConnectionProbe,
    private val store: ConnectionProbeEvidenceStore,
    private val currentServerUrl: () -> String,
    private val wallClockMillis: () -> Long = System::currentTimeMillis
) {
    private val inFlightMutex = Mutex()
    private val inFlightByServer = mutableMapOf<String, CompletableDeferred<ConnectionProbeResult>>()

    suspend fun run(force: Boolean = false): ConnectionProbeResult {
        val serverUrl = currentServerUrl()
        val serverIdentity = normalizedIdentity(serverUrl)
        if (!force && serverIdentity != null) {
            store.freshResult(serverIdentity, wallClockMillis())?.let { return it }
        }

        val serverKey = serverIdentity ?: serverUrl
        var ownsProbe = false
        val inFlight = inFlightMutex.withLock {
            inFlightByServer[serverKey] ?: CompletableDeferred<ConnectionProbeResult>().also {
                inFlightByServer[serverKey] = it
                ownsProbe = true
            }
        }
        if (!ownsProbe) return inFlight.await()

        try {
            val result = probe.probe(serverUrl)
            if (currentServerKey() == serverKey) {
                check(store.save(result)) { "Connection probe evidence could not be persisted" }
            }
            inFlight.complete(result)
            return result
        } catch (failure: Throwable) {
            inFlight.completeExceptionally(failure)
            throw failure
        } finally {
            withContext(NonCancellable) {
                inFlightMutex.withLock {
                    if (inFlightByServer[serverKey] === inFlight) {
                        inFlightByServer.remove(serverKey)
                    }
                }
            }
        }
    }

    fun millisUntilRefresh(): Long {
        val serverIdentity = normalizedIdentity(currentServerUrl())
            ?: return ConnectionProbeStore.FRESHNESS_MILLIS
        val current = store.result.value ?: return 0L
        if (current.serverIdentity != serverIdentity) return 0L
        val ageMillis = wallClockMillis() - current.checkedAtUtcMillis
        if (ageMillis < 0L) return 0L
        return (ConnectionProbeStore.FRESHNESS_MILLIS - ageMillis).coerceAtLeast(0L)
    }

    private fun currentServerKey(): String {
        val serverUrl = currentServerUrl()
        return normalizedIdentity(serverUrl) ?: serverUrl
    }

    private fun normalizedIdentity(serverUrl: String): String? {
        return runCatching { PimServerEndpoints.from(serverUrl).apiBaseUrl.toString() }.getOrNull()
    }
}
