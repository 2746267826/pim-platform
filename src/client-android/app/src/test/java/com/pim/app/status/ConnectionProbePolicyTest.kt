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

        val result = resolveProbeResult(
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
}

private class FakeStore(
    private val storedResult: ConnectionProbeResult?
) : ConnectionProbeEvidenceStore {
    override val result = kotlinx.coroutines.flow.MutableStateFlow(storedResult)

    override fun save(result: ConnectionProbeResult): Boolean = true

    override fun freshResult(serverIdentity: String, nowMillis: Long): ConnectionProbeResult? {
        return storedResult?.takeIf { it.serverIdentity == serverIdentity }
    }
}
