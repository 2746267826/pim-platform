package com.pim.app.location.policy

class LocationPolicyEngine(
    private val policy: TrackingPolicy
) {
    private var activeScheduleId: String? = null
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
                scheduleLowFrequency = false
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

        if (activeScheduleId != activeSchedule.id) {
            activeScheduleId = activeSchedule.id
            scheduleAnchorLocation = null
            movementRecoveryActive = false
        }

        if (movementRecoveryActive) {
            return decision(
                mode = LocationPolicyMode.MovementRecovery,
                intervalMillis = policy.movementIntervalMillis,
                nowMillis = input.nowMillis,
                reason = "日程期间位置变化超过 ${policy.scheduleRecoveryThresholdMeters.toInt()} 米",
                scheduleLowFrequency = false
            )
        }

        if (input.motionSignal.isMoving()) {
            return motionDecision(input.nowMillis, input.motionSignal)
        }

        return decision(
            mode = LocationPolicyMode.ScheduleLowFrequency,
            intervalMillis = policy.scheduleLowFrequencyIntervalMillis,
            nowMillis = input.nowMillis,
            reason = "当前日程包含位置信息，降低定位频率",
            scheduleLowFrequency = true
        )
    }

    fun onAcceptedLocation(location: PolicyLocation) {
        activeScheduleId ?: return
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
            intervalMillis = policy.movementIntervalMillis,
            nowMillis = nowMillis,
            reason = "检测到运动状态：$motionSignal",
            scheduleLowFrequency = false
        )

    private fun decision(
        mode: LocationPolicyMode,
        intervalMillis: Long,
        nowMillis: Long,
        reason: String,
        scheduleLowFrequency: Boolean
    ): PolicyDecision = PolicyDecision(
        mode = mode,
        requestIntervalMillis = intervalMillis,
        nextExpectedLocationAtMillis = nowMillis + intervalMillis,
        reason = reason,
        scheduleLowFrequency = scheduleLowFrequency
    )

    private fun resetScheduleState() {
        activeScheduleId = null
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
}
