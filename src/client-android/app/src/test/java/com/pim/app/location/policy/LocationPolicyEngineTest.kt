package com.pim.app.location.policy

import com.pim.app.settings.TrackingSettings
import com.pim.app.settings.toTrackingPolicy
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LocationPolicyEngineTest {
    private val policy = TrackingSettings.defaults().toTrackingPolicy()
    private val now = 1_000_000L
    private val schedule = ScheduleWindow("s1", "办公室", "上海市黄浦区", now - 1_000L, now + 60_000L)

    @Test
    fun offBecomesNormalWhenCollectionStarts() {
        val engine = LocationPolicyEngine(policy)

        val decision = engine.reduce(LocationPolicyInput(nowMillis = now, collectionEnabled = true))

        assertEquals(LocationPolicyMode.PowerSavingNormal, decision.mode)
        assertEquals(policy.normalIntervalMillis, decision.requestIntervalMillis)
    }

    @Test
    fun collectionDisabledHasNoNextExpectedLocation() {
        val engine = LocationPolicyEngine(policy)

        val decision = engine.reduce(LocationPolicyInput(nowMillis = now, collectionEnabled = false))

        assertEquals(LocationPolicyMode.Off, decision.mode)
        assertEquals(Long.MAX_VALUE, decision.nextExpectedLocationAtMillis)
    }

    @Test
    fun currentScheduleWithLocationEntersLowFrequency() {
        val engine = LocationPolicyEngine(policy)

        val decision = engine.reduce(
            LocationPolicyInput(nowMillis = now, collectionEnabled = true, currentScheduleWindow = schedule)
        )

        assertEquals(LocationPolicyMode.ScheduleLowFrequency, decision.mode)
        assertEquals(policy.scheduleLowFrequencyIntervalMillis, decision.requestIntervalMillis)
    }

    @Test
    fun scheduleEndsReturnsToNormal() {
        val engine = LocationPolicyEngine(policy)
        engine.reduce(LocationPolicyInput(nowMillis = now, collectionEnabled = true, currentScheduleWindow = schedule))

        val decision = engine.reduce(
            LocationPolicyInput(nowMillis = now + 120_000L, collectionEnabled = true, currentScheduleWindow = null)
        )

        assertEquals(LocationPolicyMode.PowerSavingNormal, decision.mode)
    }

    @Test
    fun movementRecoveryWithoutMotionUsesNormalInterval() {
        val longSchedule = schedule.copy(endsAtMillis = now + 180_000L)
        val engine = LocationPolicyEngine(policy)
        engine.reduce(LocationPolicyInput(nowMillis = now, collectionEnabled = true, currentScheduleWindow = longSchedule))
        engine.onAcceptedLocation(PolicyLocation(31.230416, 121.473701, now))
        engine.reduce(LocationPolicyInput(nowMillis = now + 60_000L, collectionEnabled = true, currentScheduleWindow = longSchedule))

        engine.onAcceptedLocation(PolicyLocation(31.232000, 121.473701, now + 60_000L))
        val recovered = engine.reduce(
            LocationPolicyInput(nowMillis = now + 61_000L, collectionEnabled = true, currentScheduleWindow = longSchedule)
        )

        assertEquals(LocationPolicyMode.MovementRecovery, recovered.mode)
        assertEquals(policy.normalIntervalMillis, recovered.requestIntervalMillis)
    }

    @Test
    fun sameScheduleIdWithChangedWindowResetsRecoveryState() {
        val longSchedule = schedule.copy(endsAtMillis = now + 180_000L)
        val engine = LocationPolicyEngine(policy)
        engine.reduce(LocationPolicyInput(nowMillis = now, collectionEnabled = true, currentScheduleWindow = longSchedule))
        engine.onAcceptedLocation(PolicyLocation(31.230416, 121.473701, now))
        engine.onAcceptedLocation(PolicyLocation(31.232000, 121.473701, now + 60_000L))
        engine.reduce(LocationPolicyInput(nowMillis = now + 61_000L, collectionEnabled = true, currentScheduleWindow = longSchedule))

        val updatedSameIdSchedule = longSchedule.copy(locationText = "上海市徐汇区", startsAtMillis = now + 70_000L)
        val decision = engine.reduce(
            LocationPolicyInput(
                nowMillis = now + 71_000L,
                collectionEnabled = true,
                currentScheduleWindow = updatedSameIdSchedule
            )
        )

        assertEquals(LocationPolicyMode.ScheduleLowFrequency, decision.mode)
    }

    @Test
    fun `schedule is active at start time inclusive`() {
        assertTrue(schedule.isActiveAt(schedule.startsAtMillis))
    }

    @Test
    fun `schedule is not active at end time exclusive`() {
        assertFalse(schedule.isActiveAt(schedule.endsAtMillis))
    }

    @Test
    fun `active schedule without location still enters low frequency`() {
        val noLocation = ScheduleWindow("s2", "会议", "", now - 1_000L, now + 60_000L)
        val engine = LocationPolicyEngine(policy)

        val decision = engine.reduce(
            LocationPolicyInput(nowMillis = now, collectionEnabled = true, currentScheduleWindow = noLocation)
        )

        assertEquals(LocationPolicyMode.ScheduleLowFrequency, decision.mode)
        assertTrue(decision.scheduleLowFrequency)
    }

    @Test
    fun `vehicle uses half movement interval but never below thirty seconds`() {
        val engine = LocationPolicyEngine(TrackingPolicy(movementIntervalMillis = 60_000L))

        val decision = engine.reduce(
            LocationPolicyInput(nowMillis = now, collectionEnabled = true, motionSignal = MotionSignal.InVehicle)
        )

        assertEquals(30_000L, decision.requestIntervalMillis)
    }

    @Test
    fun `vehicle reason uses Chinese display name not enum constant`() {
        val engine = LocationPolicyEngine(policy)

        val decision = engine.reduce(
            LocationPolicyInput(nowMillis = now, collectionEnabled = true, motionSignal = MotionSignal.InVehicle)
        )

        assertTrue("Reason should contain '车载' but was: ${decision.reason}", decision.reason.contains("车载"))
        assertFalse("Reason should not contain 'InVehicle' but was: ${decision.reason}", decision.reason.contains("InVehicle"))
    }

    @Test
    fun `vehicle half interval respects thirty second floor`() {
        val engine = LocationPolicyEngine(TrackingPolicy(movementIntervalMillis = 30_000L))

        val decision = engine.reduce(
            LocationPolicyInput(nowMillis = now, collectionEnabled = true, motionSignal = MotionSignal.InVehicle)
        )

        assertEquals(TrackingIntervalBounds.MOVEMENT_MIN_MILLIS, decision.requestIntervalMillis)
    }

    @Test
    fun `movement recovery with walking uses movement interval`() {
        val longSchedule = schedule.copy(endsAtMillis = now + 180_000L)
        val engine = LocationPolicyEngine(policy)
        engine.reduce(LocationPolicyInput(nowMillis = now, collectionEnabled = true, currentScheduleWindow = longSchedule))
        engine.onAcceptedLocation(PolicyLocation(31.230416, 121.473701, now))
        engine.onAcceptedLocation(PolicyLocation(31.232000, 121.473701, now + 60_000L))

        val recovered = engine.reduce(
            LocationPolicyInput(nowMillis = now + 61_000L, collectionEnabled = true, currentScheduleWindow = longSchedule, motionSignal = MotionSignal.Walking)
        )

        assertEquals(LocationPolicyMode.MovementRecovery, recovered.mode)
        assertEquals(policy.movementIntervalMillis, recovered.requestIntervalMillis)
    }

    @Test
    fun `movement recovery with vehicle uses derived interval`() {
        val longSchedule = schedule.copy(endsAtMillis = now + 180_000L)
        val engine = LocationPolicyEngine(TrackingPolicy(movementIntervalMillis = 60_000L))
        engine.reduce(LocationPolicyInput(nowMillis = now, collectionEnabled = true, currentScheduleWindow = longSchedule))
        engine.onAcceptedLocation(PolicyLocation(31.230416, 121.473701, now))
        engine.onAcceptedLocation(PolicyLocation(31.232000, 121.473701, now + 60_000L))

        val recovered = engine.reduce(
            LocationPolicyInput(nowMillis = now + 61_000L, collectionEnabled = true, currentScheduleWindow = longSchedule, motionSignal = MotionSignal.InVehicle)
        )

        assertEquals(LocationPolicyMode.MovementRecovery, recovered.mode)
        assertEquals(30_000L, recovered.requestIntervalMillis)
    }

    @Test
    fun `schedule low frequency reason does not mention location`() {
        val engine = LocationPolicyEngine(policy)

        val decision = engine.reduce(
            LocationPolicyInput(nowMillis = now, collectionEnabled = true, currentScheduleWindow = schedule)
        )

        assertFalse("Reason should not mention location for schedule low frequency", decision.reason.contains("位置"))
    }

    @Test
    fun `bicycle also uses half movement interval`() {
        val engine = LocationPolicyEngine(TrackingPolicy(movementIntervalMillis = 60_000L))

        val decision = engine.reduce(
            LocationPolicyInput(nowMillis = now, collectionEnabled = true, motionSignal = MotionSignal.OnBicycle)
        )

        assertEquals(30_000L, decision.requestIntervalMillis)
    }

    @Test
    fun motionSignalShortensInterval() {
        val engine = LocationPolicyEngine(policy)

        val decision = engine.reduce(
            LocationPolicyInput(nowMillis = now, collectionEnabled = true, motionSignal = MotionSignal.Walking)
        )

        assertEquals(LocationPolicyMode.MotionObservation, decision.mode)
        assertEquals(policy.movementIntervalMillis, decision.requestIntervalMillis)
    }
}
