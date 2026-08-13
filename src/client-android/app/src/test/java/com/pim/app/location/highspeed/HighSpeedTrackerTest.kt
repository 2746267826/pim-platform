package com.pim.app.location.highspeed

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class HighSpeedTrackerTest {

    private var now = 0L

    private fun tracker() = HighSpeedTracker(nowElapsedRealtimeMillis = { now })

    private fun fast(): Float = HighSpeedTracker.TRIGGER_SPEED_METERS_PER_SECOND

    private fun slow(): Float = 0.1f

    private fun fallbackEdge(): Float = HighSpeedTracker.FALLBACK_SPEED_METERS_PER_SECOND

    @Test
    fun sustainedFastSamplesForTenSecondsActivate() {
        val tracker = tracker()

        tracker.observe(fast(), atMillis = 0)
        tracker.observe(fast(), atMillis = 4_000)
        assertEquals(HighSpeedMode.Accumulating, tracker.mode)
        tracker.observe(fast(), atMillis = 9_000)
        assertEquals(HighSpeedMode.Accumulating, tracker.mode)
        tracker.observe(fast(), atMillis = 10_000)

        assertEquals(HighSpeedMode.Active, tracker.mode)
    }

    @Test
    fun belowThresholdNeverTriggers() {
        val tracker = tracker()

        val below = 7.9f / 3.6f
        for (elapsed in listOf(0L, 5_000L, 10_000L, 30_000L, 60_000L)) {
            tracker.observe(below, atMillis = elapsed)
        }

        assertEquals(HighSpeedMode.Inactive, tracker.mode)
    }

    @Test
    fun singleFastSampleOnlyAccumulates() {
        val tracker = tracker()

        tracker.observe(fast(), atMillis = 0)

        assertEquals(HighSpeedMode.Accumulating, tracker.mode)
    }

    @Test
    fun slowSampleDuringAccumulationResetsStreak() {
        val tracker = tracker()
        tracker.observe(fast(), atMillis = 0)
        tracker.observe(fast(), atMillis = 5_000)

        tracker.observe(slow(), atMillis = 6_000)

        assertEquals(HighSpeedMode.Inactive, tracker.mode)
        tracker.observe(fast(), atMillis = 7_000)
        tracker.observe(fast(), atMillis = 17_000)
        assertEquals(HighSpeedMode.Active, tracker.mode)
    }

    @Test
    fun activeFallsBackAfterSixtySecondsOfSlowSamples() {
        val tracker = activate()

        tracker.observe(slow(), atMillis = 10_000)
        tracker.observe(slow(), atMillis = 40_000)
        tracker.observe(slow(), atMillis = 69_999)
        assertEquals(HighSpeedMode.Active, tracker.mode)

        tracker.observe(slow(), atMillis = 70_000)
        assertEquals(HighSpeedMode.Inactive, tracker.mode)
    }

    @Test
    fun redLightThirtySecondsDoesNotFallBack() {
        val tracker = activate()

        tracker.observe(slow(), atMillis = 10_000)
        tracker.observe(slow(), atMillis = 40_000)
        assertEquals(HighSpeedMode.Active, tracker.mode)

        tracker.observe(fast(), atMillis = 42_000)
        tracker.observe(fast(), atMillis = 50_000)
        assertEquals(HighSpeedMode.Active, tracker.mode)

        tracker.observe(slow(), atMillis = 80_000)
        tracker.observe(slow(), atMillis = 139_999)
        assertEquals(HighSpeedMode.Active, tracker.mode)
        tracker.observe(slow(), atMillis = 140_000)
        assertEquals(HighSpeedMode.Inactive, tracker.mode)
    }

    @Test
    fun triggerThenImmediateSlowSamplesStillRequireSixtySecondDebounce() {
        val tracker = activate()

        tracker.observe(slow(), atMillis = 10_001)
        tracker.observe(slow(), atMillis = 70_000)
        assertEquals(HighSpeedMode.Active, tracker.mode)
        tracker.observe(slow(), atMillis = 70_001)
        assertEquals(HighSpeedMode.Inactive, tracker.mode)
    }

    @Test
    fun speedFluctuationResetsFastStreak() {
        val tracker = tracker()

        tracker.observe(fast(), atMillis = 0)
        tracker.observe(fast(), atMillis = 3_000)
        tracker.observe(slow(), atMillis = 3_500)
        tracker.observe(fast(), atMillis = 6_000)
        tracker.observe(fast(), atMillis = 9_000)
        tracker.observe(slow(), atMillis = 9_500)
        tracker.observe(fast(), atMillis = 12_000)
        tracker.observe(fast(), atMillis = 15_000)
        assertEquals(HighSpeedMode.Accumulating, tracker.mode)

        tracker.observe(slow(), atMillis = 15_500)
        assertEquals(HighSpeedMode.Inactive, tracker.mode)
    }

    @Test
    fun nullSpeedDoesNotTriggerButCountsAsSlowWhileActive() {
        val tracker = tracker()

        tracker.observe(null, atMillis = 0)
        tracker.observe(null, atMillis = 20_000)
        assertEquals(HighSpeedMode.Inactive, tracker.mode)

        val active = activate()
        active.observe(null, atMillis = 10_000)
        active.observe(null, atMillis = 69_999)
        assertEquals(HighSpeedMode.Active, active.mode)
        active.observe(null, atMillis = 70_000)
        assertEquals(HighSpeedMode.Inactive, active.mode)
    }

    @Test
    fun exactThresholdActivatesAtExactlyTenSeconds() {
        val tracker = tracker()

        tracker.observe(fast(), atMillis = 0)
        tracker.observe(fast(), atMillis = 10_000)

        assertEquals(HighSpeedMode.Active, tracker.mode)
    }

    @Test
    fun fallbackRequiresStrictlyBelowOneKmh() {
        val tracker = activate()

        tracker.observe(fallbackEdge(), atMillis = 10_000)
        tracker.observe(fallbackEdge(), atMillis = 40_000)
        tracker.observe(fallbackEdge(), atMillis = 70_000)
        assertEquals(
            "speed exactly at fallback threshold must not accumulate",
            HighSpeedMode.Active,
            tracker.mode
        )

        tracker.observe(0.99f / 3.6f, atMillis = 71_000)
        tracker.observe(0.99f / 3.6f, atMillis = 130_999)
        assertEquals(HighSpeedMode.Active, tracker.mode)
        tracker.observe(0.99f / 3.6f, atMillis = 131_000)
        assertEquals(HighSpeedMode.Inactive, tracker.mode)
    }

    @Test
    fun activeSinceExposedAndReset() {
        val tracker = tracker()
        assertNull(tracker.activeSinceElapsedRealtimeMillis)

        tracker.observe(fast(), atMillis = 0)
        tracker.observe(fast(), atMillis = 10_000)
        assertEquals(10_000L, tracker.activeSinceElapsedRealtimeMillis)

        tracker.reset()
        assertEquals(HighSpeedMode.Inactive, tracker.mode)
        assertNull(tracker.activeSinceElapsedRealtimeMillis)

        val second = activate()
        second.observe(slow(), atMillis = 11_000)
        second.observe(slow(), atMillis = 71_000)
        assertEquals(HighSpeedMode.Inactive, second.mode)
        assertNull(second.activeSinceElapsedRealtimeMillis)
    }

    private fun activate(): HighSpeedTracker {
        val tracker = tracker()
        tracker.observe(fast(), atMillis = 0)
        tracker.observe(fast(), atMillis = 5_000)
        tracker.observe(fast(), atMillis = 10_000)
        assertTrue("precondition: tracker must be active", tracker.mode == HighSpeedMode.Active)
        return tracker
    }

    private fun HighSpeedTracker.observe(speed: Float?, atMillis: Long) {
        now = atMillis
        observe(speed)
    }
}
