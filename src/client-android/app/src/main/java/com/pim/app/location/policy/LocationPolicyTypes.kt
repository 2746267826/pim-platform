package com.pim.app.location.policy

enum class LocationPolicyMode {
    Off,
    PowerSavingNormal,
    ScheduleLowFrequency,
    MotionObservation,
    MovementRecovery,
    SyncFallback
}

data class TrackingPolicy(
    val normalIntervalMillis: Long = 3 * 60 * 1000L,
    val scheduleLowFrequencyIntervalMillis: Long = 15 * 60 * 1000L,
    val movementIntervalMillis: Long = 60 * 1000L,
    val scheduleRecoveryThresholdMeters: Double = 100.0,
    val altitudeWaitTimeoutMillis: Long = 15 * 1000L,
    val maxUploadAccuracyMetersExclusive: Float = 50f
)

data class PolicyDecision(
    val mode: LocationPolicyMode,
    val requestIntervalMillis: Long,
    val nextExpectedLocationAtMillis: Long,
    val reason: String,
    val scheduleLowFrequency: Boolean
)

data class ScheduleWindow(
    val id: String,
    val title: String,
    val locationText: String,
    val startsAtMillis: Long,
    val endsAtMillis: Long
) {
    fun isActiveAt(nowMillis: Long): Boolean =
        nowMillis in startsAtMillis until endsAtMillis && locationText.isNotBlank()
}

enum class MotionSignal {
    Unknown,
    Still,
    Walking,
    Running,
    OnBicycle,
    InVehicle
}

data class PolicyLocation(
    val latitude: Double,
    val longitude: Double,
    val recordedAtMillis: Long
)

data class LocationPolicyInput(
    val nowMillis: Long,
    val collectionEnabled: Boolean,
    val currentScheduleWindow: ScheduleWindow? = null,
    val motionSignal: MotionSignal = MotionSignal.Unknown
)
