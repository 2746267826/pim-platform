package com.pim.app.status

internal suspend fun resolveProbeResult(
    force: Boolean,
    serverIdentity: String?,
    store: ConnectionProbeEvidenceStore,
    probe: suspend () -> ConnectionProbeResult,
    save: (ConnectionProbeResult) -> Boolean,
    nowMillis: Long
): ConnectionProbeResult {
    val fresh = if (!force) serverIdentity?.let {
        store.freshResult(it, nowMillis)
    } else null
    if (fresh != null) return fresh
    val result = probe()
    save(result)
    return result
}
