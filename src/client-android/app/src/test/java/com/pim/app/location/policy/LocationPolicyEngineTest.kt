package com.pim.app.location.policy

import com.pim.app.settings.TrackingSettings
import com.pim.app.settings.toTrackingPolicy
import org.junit.Assert.assertEquals
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
    fun movementOverOneHundredMetersRecoversFromScheduleLowFrequency() {
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
        assertEquals(policy.movementIntervalMillis, recovered.requestIntervalMillis)
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
