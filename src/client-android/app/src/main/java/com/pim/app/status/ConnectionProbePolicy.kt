package com.pim.app.status

internal fun probeRefreshDelayMillis(
    result: ConnectionProbeResult?,
    serverIdentity: String?,
    nowMillis: Long
): Long {
    if (result == null) return 30_000L
    if (result.serverIdentity != serverIdentity) return 30_000L
    val ageMillis = nowMillis - result.checkedAtUtcMillis
    if (ageMillis < 0L) return 30_000L
    if (ageMillis >= ConnectionProbeStore.FRESHNESS_MILLIS) return 0L
    return ConnectionProbeStore.FRESHNESS_MILLIS - ageMillis
}

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
