package com.pim.app.status

import com.pim.app.schedule.ScheduleCacheFreshness
import com.pim.app.location.policy.ScheduleWindow
import com.pim.app.schedule.ScheduleCacheSnapshot
import com.pim.app.data.MobileLocationPolicyTransitionEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class StatusCenterRepositoryFlowTest {

    @Test
    fun activeImmediateWorkClearsAcceptedAfterAcceptedWasObserved() = runTest {
        val signal = StatusAcceptedSignal()
        val generation = signal.trigger()

        val collectedSignalDuringEmission = mutableListOf<Boolean>()

        flowOf(
            StatusEmission(
                state = StatusCenterState.empty().copy(syncPhase = SyncPhase.Accepted),
                clearAcceptedGenerationAfterEmission = generation
            )
        ).emitStates(signal::clearIfGeneration)
            .onEach { collectedSignalDuringEmission.add(signal.state.value.isAccepted) }
            .first()

        assertEquals(1, collectedSignalDuringEmission.size)
        assertTrue("signal must be true during onEach before clear", collectedSignalDuringEmission[0])
        assertFalse("signal must be false after emitStates clears", signal.state.value.isAccepted)
    }

    @Test
    fun `scheduleCacheFactsMappedFromRepositorySnapshot`() {
        val schedule = ScheduleCacheStatusSnapshot(
            freshness = ScheduleCacheFreshness.Fresh,
            hasCachedWindows = true,
            lastSuccessAtMillis = 1_000L,
            lastAttemptAtMillis = 1_000L,
            lastError = null
        )
        assertEquals(ScheduleCacheFreshness.Fresh, schedule.freshness)
        assertTrue(schedule.hasCachedWindows)
        assertEquals(1_000L, schedule.lastSuccessAtMillis)
        assertEquals(1_000L, schedule.lastAttemptAtMillis)
        assertNull(schedule.lastError)
    }

    @Test
    fun `policyTransitionSnapshotHoldsFields`() {
        val transition = MobileLocationPolicyTransitionEntity(
            fromMode = "PowerSavingNormal",
            toMode = "ScheduleLowFrequency",
            reason = "当前日程时段，降低定位频率",
            occurredAtUtc = 1_000L
        ).toPolicyTransitionSnapshot()
        assertEquals("PowerSavingNormal", transition.fromMode)
        assertEquals("ScheduleLowFrequency", transition.toMode)
        assertEquals("当前日程时段，降低定位频率", transition.reason)
        assertEquals(1_000L, transition.occurredAtMillis)
    }

    @Test
    fun `scheduleMappedFromRepositorySnapshotUsesFreshnessAndError`() {
        val repoSnapshot = ScheduleCacheSnapshot(
            serverIdentity = "https://example",
            windows = listOf(
                ScheduleWindow("id", "title", "location", 100L, 200L)
            ),
            freshness = ScheduleCacheFreshness.Stale,
            lastAttemptAtMillis = 2_000L,
            lastSuccessAtMillis = 1_000L,
            lastError = "网络不可用",
            errorKind = null
        )
        val mapped = repoSnapshot.toScheduleCacheStatusSnapshot()
        assertEquals(ScheduleCacheFreshness.Stale, mapped.freshness)
        assertTrue(mapped.hasCachedWindows)
        assertEquals(1_000L, mapped.lastSuccessAtMillis)
        assertEquals(2_000L, mapped.lastAttemptAtMillis)
        assertEquals("网络不可用", mapped.lastError)
    }

    @Test
    fun `emptyWindowsScheduleStillTracksFreshness`() {
        val mapped = ScheduleCacheStatusSnapshot(
            freshness = ScheduleCacheFreshness.Fresh,
            hasCachedWindows = false
        )
        assertEquals(ScheduleCacheFreshness.Fresh, mapped.freshness)
        assertFalse(mapped.hasCachedWindows)
        assertNull(mapped.lastError)
    }

    @Test
    fun `scheduleSnapshotFromAnotherServerIsHidden`() {
        val repoSnapshot = ScheduleCacheSnapshot(
            serverIdentity = "https://old.example/api/",
            windows = listOf(ScheduleWindow("id", "title", "", 100L, 200L)),
            freshness = ScheduleCacheFreshness.Fresh,
            lastAttemptAtMillis = 2_000L,
            lastSuccessAtMillis = 1_000L,
            lastError = null,
            errorKind = null
        )

        val mapped = repoSnapshot.toScheduleCacheStatusSnapshot(
            expectedServerIdentity = "https://new.example/api/"
        )

        assertEquals(ScheduleCacheFreshness.Missing, mapped.freshness)
        assertFalse(mapped.hasCachedWindows)
        assertNull(mapped.lastSuccessAtMillis)
        assertNull(mapped.lastAttemptAtMillis)
        assertNull(mapped.lastError)
    }

    @Test
    fun olderEmissionCannotClearNewerAcceptedGeneration() = runTest {
        val signal = StatusAcceptedSignal()
        val firstGeneration = signal.trigger()

        flowOf(
            StatusEmission(
                state = StatusCenterState.empty().copy(syncPhase = SyncPhase.Accepted),
                clearAcceptedGenerationAfterEmission = firstGeneration
            )
        ).emitStates(signal::clearIfGeneration)
            .onEach { signal.trigger() }
            .first()

        assertTrue(signal.state.value.isAccepted)
        assertTrue(signal.state.value.generation > firstGeneration)
    }

}
