package com.pim.app.status

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ConnectionProbeRunnerTest {
    private lateinit var store: ConnectionProbeStore
    private var serverUrl = "https://pim.example/api/v1/"

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val preferences = context.getSharedPreferences("probe-runner-test", Context.MODE_PRIVATE)
        preferences.edit().clear().commit()
        store = ConnectionProbeStore(preferences, Json { ignoreUnknownKeys = true })
    }

    @Test
    fun concurrentForcedRunsForTheSameServerShareOneProbeAndPersistIt() = runTest {
        val calls = AtomicInteger(0)
        val entered = CompletableDeferred<Unit>()
        val release = CompletableDeferred<Unit>()
        val expected = reachable(serverUrl, checkedAt = 1_000L)
        val runner = ConnectionProbeRunner(
            probe = ConnectionProbe {
                calls.incrementAndGet()
                entered.complete(Unit)
                release.await()
                expected
            },
            store = store,
            currentServerUrl = { serverUrl },
            wallClockMillis = { 1_000L }
        )

        val first = async { runner.run(force = true) }
        runCurrent()
        assertTrue(entered.isCompleted)
        val second = async { runner.run(force = true) }
        runCurrent()

        assertEquals(1, calls.get())
        release.complete(Unit)
        assertEquals(expected, first.await())
        assertEquals(expected, second.await())
        assertEquals(expected, store.result.value)
    }

    @Test
    fun nonForcedRunReusesOnlyFreshEvidenceForTheCurrentServer() = runTest {
        val original = reachable(serverUrl, checkedAt = 1_000L)
        assertTrue(store.save(original))
        val calls = AtomicInteger(0)
        val runner = ConnectionProbeRunner(
            probe = ConnectionProbe { requestedUrl ->
                calls.incrementAndGet()
                reachable(requestedUrl, checkedAt = 2_000L)
            },
            store = store,
            currentServerUrl = { serverUrl },
            wallClockMillis = { 2_000L }
        )

        assertEquals(original, runner.run(force = false))
        assertEquals(0, calls.get())

        serverUrl = "https://other.example/api/v1/"
        val changedServer = runner.run(force = false)

        assertEquals(1, calls.get())
        assertEquals(serverUrl, changedServer.serverIdentity)
        assertEquals(changedServer, store.result.value)
    }

    @Test
    fun freshSnapshotIsReturnedEvenWhenPublishedStateChangesImmediatelyAfterLookup() = runTest {
        val original = reachable(serverUrl, checkedAt = 1_000L)
        val replacement = reachable(
            "https://other.example/api/v1/",
            checkedAt = 2_000L
        )
        val evidenceStore = SwappingEvidenceStore(original, replacement)
        val calls = AtomicInteger(0)
        val runner = ConnectionProbeRunner(
            probe = ConnectionProbe {
                calls.incrementAndGet()
                error("fresh evidence must not trigger a probe")
            },
            store = evidenceStore,
            currentServerUrl = { serverUrl },
            wallClockMillis = { 2_000L }
        )

        val result = runner.run(force = false)

        assertEquals(original, result)
        assertEquals(replacement, evidenceStore.result.value)
        assertEquals(0, calls.get())
    }

    @Test
    fun forcedRunIgnoresFreshEvidence() = runTest {
        assertTrue(store.save(reachable(serverUrl, checkedAt = 1_000L)))
        val calls = AtomicInteger(0)
        val runner = ConnectionProbeRunner(
            probe = ConnectionProbe { requestedUrl ->
                calls.incrementAndGet()
                reachable(requestedUrl, checkedAt = 2_000L)
            },
            store = store,
            currentServerUrl = { serverUrl },
            wallClockMillis = { 2_000L }
        )

        val refreshed = runner.run(force = true)

        assertEquals(1, calls.get())
        assertEquals(2_000L, refreshed.checkedAtUtcMillis)
        assertEquals(refreshed, store.result.value)
    }

    @Test
    fun cancelledOwnerIsRemovedSoTheNextRunCanProbeAgain() = runTest {
        val calls = AtomicInteger(0)
        val entered = CompletableDeferred<Unit>()
        val expected = reachable(serverUrl, checkedAt = 2_000L)
        val runner = ConnectionProbeRunner(
            probe = ConnectionProbe {
                if (calls.incrementAndGet() == 1) {
                    entered.complete(Unit)
                    awaitCancellation()
                }
                expected
            },
            store = store,
            currentServerUrl = { serverUrl },
            wallClockMillis = { 2_000L }
        )

        val cancelled = async { runner.run(force = true) }
        runCurrent()
        assertTrue(entered.isCompleted)
        cancelled.cancelAndJoin()

        assertEquals(expected, runner.run(force = true))
        assertEquals(2, calls.get())
    }

    @Test
    fun refreshDelayExpiresAtTheExactFiveMinuteBoundary() {
        val nowMillis = AtomicLong(300_999L)
        assertTrue(store.save(reachable(serverUrl, checkedAt = 1_000L)))
        val runner = ConnectionProbeRunner(
            probe = ConnectionProbe { error("fresh evidence must not probe") },
            store = store,
            currentServerUrl = { serverUrl },
            wallClockMillis = nowMillis::get
        )

        assertEquals(1L, runner.millisUntilRefresh())
        nowMillis.set(301_000L)
        assertEquals(0L, runner.millisUntilRefresh())
        serverUrl = "https://other.example/api/v1/"
        assertEquals(0L, runner.millisUntilRefresh())
    }

    private fun reachable(identity: String, checkedAt: Long): ConnectionProbeResult {
        return ConnectionProbeResult(
            outcome = ConnectionProbeOutcome.Reachable,
            checkedAtUtcMillis = checkedAt,
            serverIdentity = identity,
            lastCompletedStage = ConnectionProbeStage.EmbedBootstrap,
            latencyMillisByStage = emptyMap(),
            capabilities = ServerCapabilities(true, true)
        )
    }
}

private class SwappingEvidenceStore(
    initial: ConnectionProbeResult,
    private val replacement: ConnectionProbeResult
) : ConnectionProbeEvidenceStore {
    private val mutableResult = MutableStateFlow<ConnectionProbeResult?>(initial)
    override val result: StateFlow<ConnectionProbeResult?> = mutableResult

    override fun save(result: ConnectionProbeResult): Boolean {
        mutableResult.value = result
        return true
    }

    override fun freshResult(
        serverIdentity: String,
        nowMillis: Long
    ): ConnectionProbeResult? {
        val captured = mutableResult.value
        mutableResult.value = replacement
        return captured?.takeIf {
            it.serverIdentity == serverIdentity &&
                nowMillis - it.checkedAtUtcMillis in 0 until ConnectionProbeStore.FRESHNESS_MILLIS
        }
    }
}
