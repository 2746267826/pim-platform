package com.pim.app.location.policy

enum class LocationPolicyMode {
    Off,
    PowerSavingNormal,
    ScheduleLowFrequency,
    MotionObservation,
    MovementRecovery,
    SyncFallback
}

object TrackingIntervalBounds {
    const val NORMAL_MIN_MILLIS = 60_000L
    const val NORMAL_MAX_MILLIS = 900_000L
    const val SCHEDULE_MIN_MILLIS = 300_000L
    const val SCHEDULE_MAX_MILLIS = 3_600_000L
    const val MOVEMENT_MIN_MILLIS = 30_000L
    const val MOVEMENT_MAX_MILLIS = 300_000L
}

fun TrackingPolicy.movementIntervalFor(signal: MotionSignal): Long = when (signal) {
    MotionSignal.OnBicycle, MotionSignal.InVehicle ->
        (movementIntervalMillis / 2L).coerceAtLeast(TrackingIntervalBounds.MOVEMENT_MIN_MILLIS)
    else -> movementIntervalMillis
}.coerceIn(
    TrackingIntervalBounds.MOVEMENT_MIN_MILLIS,
    TrackingIntervalBounds.MOVEMENT_MAX_MILLIS
)

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
        nowMillis in startsAtMillis until endsAtMillis
}

enum class MotionSignal(val displayName: String) {
    Unknown("未知"),
    Still("静止"),
    Walking("步行"),
    Running("跑步"),
    OnBicycle("骑行"),
    InVehicle("车载")
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
