package com.pim.app.status

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ConnectionProbeStoreTest {
    private lateinit var preferences: android.content.SharedPreferences
    private val json = Json { ignoreUnknownKeys = true }
    private val serverIdA = "https://server-a.example/api/v1/"
    private val serverIdB = "https://server-b.example/api/v1/"
    private val now = 100_000L

    private val freshResult = ConnectionProbeResult(
        outcome = ConnectionProbeOutcome.Reachable,
        checkedAtUtcMillis = now,
        serverIdentity = serverIdA,
        lastCompletedStage = ConnectionProbeStage.WebRoot,
        latencyMillisByStage = emptyMap(),
        capabilities = ServerCapabilities(true, true)
    )

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        preferences = context.getSharedPreferences("probe-store-test", Context.MODE_PRIVATE)
        preferences.edit().clear().commit()
    }

    private fun store(): ConnectionProbeStore =
        ConnectionProbeStore(preferences, json, nowMillis = { now })

    @Test
    fun saveRejectsOlderEvidenceForSameServer() {
        val s = store()
        s.save(freshResult)

        val older = freshResult.copy(checkedAtUtcMillis = now - 10_000)
        val saved = s.save(older)

        assertFalse(saved)
        assertEquals(freshResult, s.result.value)

        val reloaded = store()
        assertEquals(freshResult, reloaded.result.value)
    }

    @Test
    fun saveAcceptsOlderEvidenceForDifferentServer() {
        val s = store()
        s.save(freshResult)

        val different = freshResult.copy(
            checkedAtUtcMillis = now - 10_000,
            serverIdentity = serverIdB
        )
        val saved = s.save(different)

        assertTrue(saved)
        assertEquals(different, s.result.value)

        val reloaded = store()
        assertEquals(different, reloaded.result.value)
    }

    @Test
    fun saveAcceptsOlderEvidenceWhenClockRewound() {
        val s = store()
        s.save(freshResult.copy(checkedAtUtcMillis = now + 60_000))

        val older = freshResult.copy(checkedAtUtcMillis = now)
        val saved = s.save(older)

        assertTrue(saved)
        assertEquals(older, s.result.value)

        val reloaded = store()
        assertEquals(older, reloaded.result.value)
    }

    @Test
    fun saveAcceptsEqualTimestamp() {
        val s = store()
        s.save(freshResult)

        val sameTime = freshResult.copy(outcome = ConnectionProbeOutcome.Partial)
        val saved = s.save(sameTime)

        assertTrue(saved)
        assertEquals(sameTime, s.result.value)

        val reloaded = store()
        assertEquals(sameTime, reloaded.result.value)
    }
}
