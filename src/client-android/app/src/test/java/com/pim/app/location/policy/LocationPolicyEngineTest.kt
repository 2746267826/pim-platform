package com.pim.app.location.policy

import com.pim.app.location.highspeed.HighSpeedMode
import com.pim.app.location.highspeed.HighSpeedTracker
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
    private var fakeElapsed = 0L

    private fun makeEngine(policy: TrackingPolicy = this.policy): LocationPolicyEngine =
        LocationPolicyEngine(
            policy,
            highSpeedTracker = HighSpeedTracker(nowElapsedRealtimeMillis = { fakeElapsed })
        )

    private fun fastSpeed(): Float = HighSpeedTracker.TRIGGER_SPEED_METERS_PER_SECOND

    private fun reduce(
        engine: LocationPolicyEngine,
        speed: Float? = null,
        motion: MotionSignal = MotionSignal.Unknown,
        schedule: ScheduleWindow? = null
    ): PolicyDecision = engine.reduce(
        LocationPolicyInput(
            nowMillis = now,
            collectionEnabled = true,
            currentScheduleWindow = schedule,
            motionSignal = motion,
            speedMetersPerSecond = speed
        )
    )

    /** 通过 3 次高速样本（间隔 5s）让内置 tracker 进入 Active。 */
    private fun activateHighSpeed(engine: LocationPolicyEngine) {
        fakeElapsed = 0L
        reduce(engine, speed = fastSpeed())
        fakeElapsed = 5_000L
        reduce(engine, speed = fastSpeed())
        fakeElapsed = 10_000L
        reduce(engine, speed = fastSpeed())
        assertEquals("precondition: tracker must be active", HighSpeedMode.Active, engine.highSpeedTracker.mode)
    }

    @Test
    fun offBecomesNormalWhenCollectionStarts() {
        val engine = makeEngine(policy)

        val decision = engine.reduce(LocationPolicyInput(nowMillis = now, collectionEnabled = true))

        assertEquals(LocationPolicyMode.PowerSavingNormal, decision.mode)
        assertEquals(policy.normalIntervalMillis, decision.requestIntervalMillis)
    }

    @Test
    fun collectionDisabledHasNoNextExpectedLocation() {
        val engine = makeEngine(policy)

        val decision = engine.reduce(LocationPolicyInput(nowMillis = now, collectionEnabled = false))

        assertEquals(LocationPolicyMode.Off, decision.mode)
        assertEquals(Long.MAX_VALUE, decision.nextExpectedLocationAtMillis)
    }

    @Test
    fun currentScheduleWithLocationEntersLowFrequency() {
        val engine = makeEngine(policy)

        val decision = engine.reduce(
            LocationPolicyInput(nowMillis = now, collectionEnabled = true, currentScheduleWindow = schedule)
        )

        assertEquals(LocationPolicyMode.ScheduleLowFrequency, decision.mode)
        assertEquals(policy.scheduleLowFrequencyIntervalMillis, decision.requestIntervalMillis)
    }

    @Test
    fun scheduleEndsReturnsToNormal() {
        val engine = makeEngine(policy)
        engine.reduce(LocationPolicyInput(nowMillis = now, collectionEnabled = true, currentScheduleWindow = schedule))

        val decision = engine.reduce(
            LocationPolicyInput(nowMillis = now + 120_000L, collectionEnabled = true, currentScheduleWindow = null)
        )

        assertEquals(LocationPolicyMode.PowerSavingNormal, decision.mode)
    }

    @Test
    fun movementRecoveryWithoutMotionUsesNormalInterval() {
        val longSchedule = schedule.copy(endsAtMillis = now + 180_000L)
        val engine = makeEngine(policy)
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
        val engine = makeEngine(policy)
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
    fun `active schedule without location does not enter low frequency`() {
        val noLocation = ScheduleWindow("s2", "会议", "", now - 1_000L, now + 60_000L)
        val engine = makeEngine(policy)

        val decision = engine.reduce(
            LocationPolicyInput(nowMillis = now, collectionEnabled = true, currentScheduleWindow = noLocation)
        )

        assertEquals(LocationPolicyMode.PowerSavingNormal, decision.mode)
        assertFalse(decision.scheduleLowFrequency)
    }

    @Test
    fun `active schedule with blank whitespace location does not enter low frequency`() {
        val noLocation = ScheduleWindow("s2", "会议", "   ", now - 1_000L, now + 60_000L)
        val engine = makeEngine(policy)

        val decision = engine.reduce(
            LocationPolicyInput(nowMillis = now, collectionEnabled = true, currentScheduleWindow = noLocation)
        )

        assertEquals(LocationPolicyMode.PowerSavingNormal, decision.mode)
        assertFalse(decision.scheduleLowFrequency)
    }

    @Test
    fun `vehicle uses the hardcoded thirty second interval regardless of movement setting`() {
        val engine = makeEngine(TrackingPolicy(movementIntervalMillis = 90_000L))

        val decision = engine.reduce(
            LocationPolicyInput(nowMillis = now, collectionEnabled = true, motionSignal = MotionSignal.InVehicle)
        )

        assertEquals(30_000L, decision.requestIntervalMillis)
    }

    @Test
    fun `vehicle reason uses Chinese display name not enum constant`() {
        val engine = makeEngine(policy)

        val decision = engine.reduce(
            LocationPolicyInput(nowMillis = now, collectionEnabled = true, motionSignal = MotionSignal.InVehicle)
        )

        assertTrue("Reason should contain '车载' but was: ${decision.reason}", decision.reason.contains("车载"))
        assertFalse("Reason should not contain 'InVehicle' but was: ${decision.reason}", decision.reason.contains("InVehicle"))
    }

    @Test
    fun `vehicle half interval respects thirty second floor`() {
        val engine = makeEngine(TrackingPolicy(movementIntervalMillis = 30_000L))

        val decision = engine.reduce(
            LocationPolicyInput(nowMillis = now, collectionEnabled = true, motionSignal = MotionSignal.InVehicle)
        )

        assertEquals(TrackingIntervalBounds.MOVEMENT_MIN_MILLIS, decision.requestIntervalMillis)
    }

    @Test
    fun `movement recovery with walking uses movement interval`() {
        val longSchedule = schedule.copy(endsAtMillis = now + 180_000L)
        val engine = makeEngine(policy)
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
    fun `movement recovery with vehicle uses the hardcoded interval`() {
        val longSchedule = schedule.copy(endsAtMillis = now + 180_000L)
        val engine = makeEngine(TrackingPolicy(movementIntervalMillis = 90_000L))
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
    fun `running uses the hardcoded thirty second interval regardless of movement setting`() {
        val engine = makeEngine(TrackingPolicy(movementIntervalMillis = 300_000L))

        val decision = engine.reduce(
            LocationPolicyInput(nowMillis = now, collectionEnabled = true, motionSignal = MotionSignal.Running)
        )

        assertEquals(LocationPolicyMode.MotionObservation, decision.mode)
        assertEquals(30_000L, decision.requestIntervalMillis)
    }

    @Test
    fun `moving uses the hardcoded thirty second interval`() {
        val engine = makeEngine(TrackingPolicy(movementIntervalMillis = 300_000L))

        val decision = engine.reduce(
            LocationPolicyInput(nowMillis = now, collectionEnabled = true, motionSignal = MotionSignal.Moving)
        )

        assertEquals(LocationPolicyMode.MotionObservation, decision.mode)
        assertEquals(30_000L, decision.requestIntervalMillis)
    }

    @Test
    fun `walking uses the configured movement interval`() {
        val engine = makeEngine(TrackingPolicy(movementIntervalMillis = 90_000L))

        val decision = engine.reduce(
            LocationPolicyInput(nowMillis = now, collectionEnabled = true, motionSignal = MotionSignal.Walking)
        )

        assertEquals(90_000L, decision.requestIntervalMillis)
    }

    @Test
    fun `moving signal breaks schedule low frequency and enters motion observation`() {
        val engine = makeEngine(policy)

        val decision = engine.reduce(
            LocationPolicyInput(
                nowMillis = now,
                collectionEnabled = true,
                currentScheduleWindow = schedule,
                motionSignal = MotionSignal.Moving
            )
        )

        assertEquals(LocationPolicyMode.MotionObservation, decision.mode)
        assertFalse(decision.scheduleLowFrequency)
    }

    @Test
    fun `cycling uses the hardcoded thirty second interval`() {
        val engine = makeEngine(TrackingPolicy(movementIntervalMillis = 300_000L))

        val decision = engine.reduce(
            LocationPolicyInput(nowMillis = now, collectionEnabled = true, motionSignal = MotionSignal.OnBicycle)
        )

        assertEquals(LocationPolicyMode.MotionObservation, decision.mode)
        assertEquals(30_000L, decision.requestIntervalMillis)
    }

    @Test
    fun `schedule low frequency reason does not mention location`() {
        val engine = makeEngine(policy)

        val decision = engine.reduce(
            LocationPolicyInput(nowMillis = now, collectionEnabled = true, currentScheduleWindow = schedule)
        )

        assertFalse("Reason should not mention location for schedule low frequency", decision.reason.contains("位置"))
    }

    @Test
    fun `bicycle also uses half movement interval`() {
        val engine = makeEngine(TrackingPolicy(movementIntervalMillis = 60_000L))

        val decision = engine.reduce(
            LocationPolicyInput(nowMillis = now, collectionEnabled = true, motionSignal = MotionSignal.OnBicycle)
        )

        assertEquals(30_000L, decision.requestIntervalMillis)
    }

    @Test
    fun motionSignalShortensInterval() {
        val engine = makeEngine(policy)

        val decision = engine.reduce(
            LocationPolicyInput(nowMillis = now, collectionEnabled = true, motionSignal = MotionSignal.Walking)
        )

        assertEquals(LocationPolicyMode.MotionObservation, decision.mode)
        assertEquals(policy.movementIntervalMillis, decision.requestIntervalMillis)
    }

    @Test
    fun `high speed active overrides schedule low frequency`() {
        val engine = makeEngine()
        activateHighSpeed(engine)

        val decision = reduce(engine, speed = fastSpeed(), schedule = schedule)

        assertEquals(LocationPolicyMode.HighSpeed, decision.mode)
        assertEquals(TrackingIntervalBounds.HIGH_SPEED_INTERVAL_MILLIS, decision.requestIntervalMillis)
        assertFalse(decision.scheduleLowFrequency)
        assertTrue(decision.reason.contains("高速轨迹"))
    }

    @Test
    fun `high speed active overrides motion observation`() {
        val engine = makeEngine()
        activateHighSpeed(engine)

        val decision = reduce(engine, speed = fastSpeed(), motion = MotionSignal.Running)

        assertEquals(LocationPolicyMode.HighSpeed, decision.mode)
        assertEquals(TrackingIntervalBounds.HIGH_SPEED_INTERVAL_MILLIS, decision.requestIntervalMillis)
    }

    @Test
    fun `accumulating high speed requests dense sampling`() {
        val engine = makeEngine()

        fakeElapsed = 0L
        val decision = reduce(engine, speed = fastSpeed())

        assertEquals(LocationPolicyMode.HighSpeed, decision.mode)
        assertEquals(TrackingIntervalBounds.HIGH_SPEED_INTERVAL_MILLIS, decision.requestIntervalMillis)
        assertEquals(HighSpeedMode.Accumulating, engine.highSpeedTracker.mode)
        assertTrue(decision.reason.contains("确认"))
    }

    @Test
    fun `inactive tracker leaves normal flow untouched`() {
        val engine = makeEngine()
        fakeElapsed = 0L
        reduce(engine, speed = 1.5f)

        val decision = reduce(engine, speed = 1.5f)

        assertEquals(LocationPolicyMode.PowerSavingNormal, decision.mode)
        assertEquals(policy.normalIntervalMillis, decision.requestIntervalMillis)
    }

    @Test
    fun `collection disabled wins over high speed`() {
        val engine = makeEngine()
        activateHighSpeed(engine)

        val decision = engine.reduce(
            LocationPolicyInput(nowMillis = now, collectionEnabled = false, speedMetersPerSecond = fastSpeed())
        )

        assertEquals(LocationPolicyMode.Off, decision.mode)
    }

    @Test
    fun `high speed reason distinguishes active from accumulating`() {
        val engine = makeEngine()
        activateHighSpeed(engine)

        val active = reduce(engine, speed = fastSpeed())
        assertTrue(active.reason.contains("持续高速"))

        fakeElapsed = 10_500L
        reduce(engine, speed = 0.1f)
        fakeElapsed = 70_500L
        val fallenBack = reduce(engine, speed = 0.1f)
        assertEquals(LocationPolicyMode.PowerSavingNormal, fallenBack.mode)
    }
}
