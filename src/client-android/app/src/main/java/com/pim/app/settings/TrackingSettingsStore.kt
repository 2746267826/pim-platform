package com.pim.app.settings

import android.content.SharedPreferences
import com.pim.app.location.policy.TrackingPolicy

data class TrackingSettings(
    val profile: String,
    val continuousCollectionEnabled: Boolean,
    val normalIntervalMillis: Long,
    val scheduleLowFrequencyIntervalMillis: Long,
    val movementIntervalMillis: Long,
    val scheduleRecoveryThresholdMeters: Double,
    val altitudeWaitTimeoutMillis: Long,
    val maxUploadAccuracyMetersExclusive: Float
) {
    companion object {
        fun defaults(): TrackingSettings = TrackingSettings(
            profile = "power-saving",
            continuousCollectionEnabled = false,
            normalIntervalMillis = 3 * 60 * 1000L,
            scheduleLowFrequencyIntervalMillis = 15 * 60 * 1000L,
            movementIntervalMillis = 60 * 1000L,
            scheduleRecoveryThresholdMeters = 100.0,
            altitudeWaitTimeoutMillis = 15 * 1000L,
            maxUploadAccuracyMetersExclusive = 50f
        )
    }
}

class TrackingSettingsStore(
    private val preferences: SharedPreferences
) {
    fun read(): TrackingSettings {
        val defaults = TrackingSettings.defaults()
        return defaults.copy(
            continuousCollectionEnabled = preferences.getBoolean(
                KEY_CONTINUOUS_COLLECTION,
                defaults.continuousCollectionEnabled
            )
        )
    }

    fun setContinuousCollectionEnabled(enabled: Boolean): TrackingSettings {
        preferences.edit().putBoolean(KEY_CONTINUOUS_COLLECTION, enabled).apply()
        return read()
    }

    private companion object {
        const val KEY_CONTINUOUS_COLLECTION = "tracking.continuous_collection_enabled"
    }
}

fun TrackingSettings.toTrackingPolicy(): TrackingPolicy = TrackingPolicy(
    normalIntervalMillis = normalIntervalMillis,
    scheduleLowFrequencyIntervalMillis = scheduleLowFrequencyIntervalMillis,
    movementIntervalMillis = movementIntervalMillis,
    scheduleRecoveryThresholdMeters = scheduleRecoveryThresholdMeters,
    altitudeWaitTimeoutMillis = altitudeWaitTimeoutMillis,
    maxUploadAccuracyMetersExclusive = maxUploadAccuracyMetersExclusive
)
