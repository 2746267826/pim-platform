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
            profile = preferences.getString(KEY_PROFILE, defaults.profile) ?: defaults.profile,
            continuousCollectionEnabled = preferences.getBoolean(
                KEY_CONTINUOUS_COLLECTION,
                defaults.continuousCollectionEnabled
            ),
            normalIntervalMillis = preferences.getLong(KEY_NORMAL_INTERVAL, defaults.normalIntervalMillis),
            scheduleLowFrequencyIntervalMillis = preferences.getLong(
                KEY_SCHEDULE_LOW_FREQUENCY_INTERVAL,
                defaults.scheduleLowFrequencyIntervalMillis
            ),
            movementIntervalMillis = preferences.getLong(KEY_MOVEMENT_INTERVAL, defaults.movementIntervalMillis),
            scheduleRecoveryThresholdMeters = preferences.getFloat(
                KEY_SCHEDULE_RECOVERY_THRESHOLD,
                defaults.scheduleRecoveryThresholdMeters.toFloat()
            ).toDouble(),
            altitudeWaitTimeoutMillis = preferences.getLong(
                KEY_ALTITUDE_WAIT_TIMEOUT,
                defaults.altitudeWaitTimeoutMillis
            ),
            maxUploadAccuracyMetersExclusive = preferences.getFloat(
                KEY_MAX_UPLOAD_ACCURACY_EXCLUSIVE,
                defaults.maxUploadAccuracyMetersExclusive
            )
        )
    }

    fun write(settings: TrackingSettings): TrackingSettings {
        preferences.edit()
            .putString(KEY_PROFILE, settings.profile)
            .putBoolean(KEY_CONTINUOUS_COLLECTION, settings.continuousCollectionEnabled)
            .putLong(KEY_NORMAL_INTERVAL, settings.normalIntervalMillis)
            .putLong(KEY_SCHEDULE_LOW_FREQUENCY_INTERVAL, settings.scheduleLowFrequencyIntervalMillis)
            .putLong(KEY_MOVEMENT_INTERVAL, settings.movementIntervalMillis)
            .putFloat(KEY_SCHEDULE_RECOVERY_THRESHOLD, settings.scheduleRecoveryThresholdMeters.toFloat())
            .putLong(KEY_ALTITUDE_WAIT_TIMEOUT, settings.altitudeWaitTimeoutMillis)
            .putFloat(KEY_MAX_UPLOAD_ACCURACY_EXCLUSIVE, settings.maxUploadAccuracyMetersExclusive)
            .apply()
        return read()
    }

    fun setContinuousCollectionEnabled(enabled: Boolean): TrackingSettings {
        return write(read().copy(continuousCollectionEnabled = enabled))
    }

    private companion object {
        const val KEY_PROFILE = "tracking.profile"
        const val KEY_CONTINUOUS_COLLECTION = "tracking.continuous_collection_enabled"
        const val KEY_NORMAL_INTERVAL = "tracking.normal_interval_millis"
        const val KEY_SCHEDULE_LOW_FREQUENCY_INTERVAL = "tracking.schedule_low_frequency_interval_millis"
        const val KEY_MOVEMENT_INTERVAL = "tracking.movement_interval_millis"
        const val KEY_SCHEDULE_RECOVERY_THRESHOLD = "tracking.schedule_recovery_threshold_meters"
        const val KEY_ALTITUDE_WAIT_TIMEOUT = "tracking.altitude_wait_timeout_millis"
        const val KEY_MAX_UPLOAD_ACCURACY_EXCLUSIVE = "tracking.max_upload_accuracy_meters_exclusive"
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
