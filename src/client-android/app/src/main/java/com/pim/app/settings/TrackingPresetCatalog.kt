package com.pim.app.settings

data class TrackingPreset(
    val id: String,
    val displayName: String,
    val normalIntervalMillis: Long,
    val scheduleLowFrequencyIntervalMillis: Long,
    val movementIntervalMillis: Long,
    val scheduleRecoveryThresholdMeters: Double,
    val maxUploadAccuracyMetersExclusive: Float,
    val altitudeWaitTimeoutMillis: Long
) {
    fun applyTo(current: TrackingSettings): TrackingSettings = current.copy(
        profile = id,
        normalIntervalMillis = normalIntervalMillis,
        scheduleLowFrequencyIntervalMillis = scheduleLowFrequencyIntervalMillis,
        movementIntervalMillis = movementIntervalMillis,
        scheduleRecoveryThresholdMeters = scheduleRecoveryThresholdMeters,
        altitudeWaitTimeoutMillis = altitudeWaitTimeoutMillis,
        maxUploadAccuracyMetersExclusive = maxUploadAccuracyMetersExclusive
    )
}

object TrackingPresetCatalog {
    private val allPresets = listOf(
        TrackingPreset(
            id = "power-saving",
            displayName = "省电",
            normalIntervalMillis = 180_000L,
            scheduleLowFrequencyIntervalMillis = 900_000L,
            movementIntervalMillis = 60_000L,
            scheduleRecoveryThresholdMeters = 100.0,
            maxUploadAccuracyMetersExclusive = 50f,
            altitudeWaitTimeoutMillis = 15_000L
        ),
        TrackingPreset(
            id = "standard",
            displayName = "标准",
            normalIntervalMillis = 120_000L,
            scheduleLowFrequencyIntervalMillis = 600_000L,
            movementIntervalMillis = 45_000L,
            scheduleRecoveryThresholdMeters = 75.0,
            maxUploadAccuracyMetersExclusive = 35f,
            altitudeWaitTimeoutMillis = 20_000L
        ),
        TrackingPreset(
            id = "high-precision",
            displayName = "高精度",
            normalIntervalMillis = 60_000L,
            scheduleLowFrequencyIntervalMillis = 300_000L,
            movementIntervalMillis = 30_000L,
            scheduleRecoveryThresholdMeters = 50.0,
            maxUploadAccuracyMetersExclusive = 20f,
            altitudeWaitTimeoutMillis = 30_000L
        )
    )

    fun get(id: String): TrackingPreset? = allPresets.find { it.id == id }

    val presets: List<TrackingPreset> get() = allPresets
}
