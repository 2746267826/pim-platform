package com.pim.app.status

import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class ConnectionProbePolicyTest {
    private val probeResult = ConnectionProbeResult(
        outcome = ConnectionProbeOutcome.Reachable,
        checkedAtUtcMillis = 1_000L,
        serverIdentity = "https://example/api/v1/",
        lastCompletedStage = ConnectionProbeStage.WebRoot,
        latencyMillisByStage = emptyMap(),
        capabilities = ServerCapabilities(true, true)
    )

    @Test
    fun noForceWhenFreshResultExistsReturnsFresh() = runTest {
        val store = FakeStore(storedResult = probeResult)
        var probeCalled = false

        val result = resolveProbeResult(
            force = false,
            serverIdentity = "https://example/api/v1/",
            store = store,
            probe = { probeCalled = true; probeResult },
            save = { true },
            nowMillis = 2_000L
        )

        assertEquals(probeResult, result)
        assertEquals(false, probeCalled)
    }

    @Test
    fun noForceWhenNoFreshResultCallsProbe() = runTest {
        val store = FakeStore(storedResult = null)
        var probeCalled = false

        val result = resolveProbeResult(
            force = false,
            serverIdentity = "https://example/api/v1/",
            store = store,
            probe = { probeCalled = true; probeResult },
            save = { true },
            nowMillis = 2_000L
        )

        assertEquals(probeResult, result)
        assertEquals(true, probeCalled)
    }

    @Test
    fun forceAlwaysBypassesFreshAndCallsProbe() = runTest {
        val store = FakeStore(storedResult = probeResult)
        var probeCalled = false

        val result = resolveProbeResult(
            force = true,
            serverIdentity = "https://example/api/v1/",
            store = store,
            probe = { probeCalled = true; probeResult },
            save = { true },
            nowMillis = 2_000L
        )

        assertEquals(probeResult, result)
        assertEquals(true, probeCalled)
    }

    @Test
    fun forceWithNullIdentityStillCallsProbe() = runTest {
        val store = FakeStore(storedResult = null)
        var probeCalled = false

        val result = resolveProbeResult(
            force = true,
            serverIdentity = null,
            store = store,
            probe = { probeCalled = true; probeResult },
            save = { true },
            nowMillis = 2_000L
        )

        assertEquals(probeResult, result)
        assertEquals(true, probeCalled)
    }

    @Test
    fun forceProbeResultIsSaved() = runTest {
        val store = FakeStore(storedResult = null)
        var saved: ConnectionProbeResult? = null

        resolveProbeResult(
            force = true,
            serverIdentity = "https://example/api/v1/",
            store = store,
            probe = { probeResult },
            save = { saved = it; true },
            nowMillis = 2_000L
        )

        assertNotNull(saved)
        assertEquals(probeResult, saved)
    }

    // --- probeRefreshDelayMillis ---

    @Test
    fun probeRefreshDelayMillisNullResultReturns30s() {
        val delay = probeRefreshDelayMillis(null, "id", 100_000L)
        assertEquals(30_000L, delay)
    }

    @Test
    fun probeRefreshDelayMillisIdentityMismatchReturns30s() {
        val result = probeResult.copy(serverIdentity = "https://other/api/v1/")
        val delay = probeRefreshDelayMillis(result, "https://expected/api/v1/", 100_000L)
        assertEquals(30_000L, delay)
    }

    @Test
    fun probeRefreshDelayMillisNowBeforeCheckedAtReturns30s() {
        val delay = probeRefreshDelayMillis(probeResult, "https://example/api/v1/", 500L)
        assertEquals(30_000L, delay)
    }

    @Test
    fun probeRefreshDelayMillisFreshReturnsFreshnessAge() {
        val checkedAt = 100_000L
        val now = checkedAt + 60_000L // One minute old; freshness minus age remains.
        val result = probeResult.copy(
            serverIdentity = "https://example/api/v1/",
            checkedAtUtcMillis = checkedAt
        )
        val expected = ConnectionProbeStore.FRESHNESS_MILLIS - 60_000L
        val delay = probeRefreshDelayMillis(result, "https://example/api/v1/", now)
        assertEquals(expected, delay)
    }

    @Test
    fun probeRefreshDelayMillisExpiredReturns0() {
        val checkedAt = 100_000L
        val now = checkedAt + ConnectionProbeStore.FRESHNESS_MILLIS + 1L
        val result = probeResult.copy(
            serverIdentity = "https://example/api/v1/",
            checkedAtUtcMillis = checkedAt
        )
        val delay = probeRefreshDelayMillis(result, "https://example/api/v1/", now)
        assertEquals(0L, delay)
    }
}

private class FakeStore(
    storedResult: ConnectionProbeResult? = null
) : ConnectionProbeEvidenceStore {
    override val result = kotlinx.coroutines.flow.MutableStateFlow(storedResult)

    override fun save(result: ConnectionProbeResult): Boolean {
        this.result.value = result
        return true
    }

    override fun freshResult(serverIdentity: String, nowMillis: Long): ConnectionProbeResult? {
        val current = result.value ?: return null
        if (current.serverIdentity != serverIdentity) return null
        val ageMillis = nowMillis - current.checkedAtUtcMillis
        return current.takeIf { ageMillis >= 0L && ageMillis < ConnectionProbeStore.FRESHNESS_MILLIS }
    }
}
