package com.pim.app.location.policy

class LocationPolicyEngine(
    private val policy: TrackingPolicy
) {
    private var activeScheduleKey: ScheduleKey? = null
    private var scheduleAnchorLocation: PolicyLocation? = null
    private var movementRecoveryActive: Boolean = false

    fun reduce(input: LocationPolicyInput): PolicyDecision {
        if (!input.collectionEnabled) {
            resetScheduleState()
            return decision(
                mode = LocationPolicyMode.Off,
                intervalMillis = 0L,
                nowMillis = input.nowMillis,
                reason = "连续采集未开启",
                scheduleLowFrequency = false,
                nextExpectedLocationAtMillis = Long.MAX_VALUE
            )
        }

        val activeSchedule = input.currentScheduleWindow?.takeIf { it.isActiveAt(input.nowMillis) }
        if (activeSchedule == null) {
            resetScheduleState()
            if (input.motionSignal.isMoving()) {
                return motionDecision(input.nowMillis, input.motionSignal)
            }
            return normalDecision(input.nowMillis, "默认省电档")
        }

        val scheduleKey = ScheduleKey.from(activeSchedule)
        if (activeScheduleKey != scheduleKey) {
            activeScheduleKey = scheduleKey
            scheduleAnchorLocation = null
            movementRecoveryActive = false
        }

        if (movementRecoveryActive) {
            return if (input.motionSignal.isMoving()) {
                decision(
                    mode = LocationPolicyMode.MovementRecovery,
                    intervalMillis = policy.movementIntervalFor(input.motionSignal),
                    nowMillis = input.nowMillis,
                    reason = "日程期间位置变化超过 ${policy.scheduleRecoveryThresholdMeters.toInt()} 米",
                    scheduleLowFrequency = false
                )
            } else {
                decision(
                    mode = LocationPolicyMode.MovementRecovery,
                    intervalMillis = policy.normalIntervalMillis,
                    nowMillis = input.nowMillis,
                    reason = "日程期间位置变化超过 ${policy.scheduleRecoveryThresholdMeters.toInt()} 米",
                    scheduleLowFrequency = false
                )
            }
        }

        if (input.motionSignal.isMoving()) {
            return motionDecision(input.nowMillis, input.motionSignal)
        }

        return decision(
            mode = LocationPolicyMode.ScheduleLowFrequency,
            intervalMillis = policy.scheduleLowFrequencyIntervalMillis,
            nowMillis = input.nowMillis,
            reason = "当前日程时段，降低定位频率",
            scheduleLowFrequency = true
        )
    }

    fun onAcceptedLocation(location: PolicyLocation) {
        activeScheduleKey ?: return
        val anchor = scheduleAnchorLocation
        if (anchor == null) {
            scheduleAnchorLocation = location
            return
        }

        val distanceMeters = GeoDistance.metersBetween(anchor, location)
        if (distanceMeters > policy.scheduleRecoveryThresholdMeters) {
            movementRecoveryActive = true
        }
    }

    private fun normalDecision(nowMillis: Long, reason: String): PolicyDecision =
        decision(
            mode = LocationPolicyMode.PowerSavingNormal,
            intervalMillis = policy.normalIntervalMillis,
            nowMillis = nowMillis,
            reason = reason,
            scheduleLowFrequency = false
        )

    private fun motionDecision(nowMillis: Long, motionSignal: MotionSignal): PolicyDecision =
        decision(
            mode = LocationPolicyMode.MotionObservation,
            intervalMillis = policy.movementIntervalFor(motionSignal),
            nowMillis = nowMillis,
            reason = "检测到运动状态：${motionSignal.displayName}",
            scheduleLowFrequency = false
        )

    private fun decision(
        mode: LocationPolicyMode,
        intervalMillis: Long,
        nowMillis: Long,
        reason: String,
        scheduleLowFrequency: Boolean,
        nextExpectedLocationAtMillis: Long = nowMillis + intervalMillis
    ): PolicyDecision = PolicyDecision(
        mode = mode,
        requestIntervalMillis = intervalMillis,
        nextExpectedLocationAtMillis = nextExpectedLocationAtMillis,
        reason = reason,
        scheduleLowFrequency = scheduleLowFrequency
    )

    private fun resetScheduleState() {
        activeScheduleKey = null
        scheduleAnchorLocation = null
        movementRecoveryActive = false
    }

    private fun MotionSignal.isMoving(): Boolean = when (this) {
        MotionSignal.Walking,
        MotionSignal.Running,
        MotionSignal.OnBicycle,
        MotionSignal.InVehicle -> true
        MotionSignal.Unknown,
        MotionSignal.Still -> false
    }

    private data class ScheduleKey(
        val id: String,
        val startsAtMillis: Long,
        val endsAtMillis: Long
    ) {
        companion object {
            fun from(window: ScheduleWindow): ScheduleKey = ScheduleKey(
                id = window.id,
                startsAtMillis = window.startsAtMillis,
                endsAtMillis = window.endsAtMillis
            )
        }
    }
}
