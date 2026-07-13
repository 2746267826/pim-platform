package com.pim.app.settings

data class ValidationError(val code: String, val message: String)

class ValidationException(val errors: List<ValidationError>) : IllegalArgumentException(
    "Validation failed: ${errors.joinToString("; ") { "${it.code}: ${it.message}" }}"
)

object TrackingSettingsValidator {
    private const val NORMAL_MIN = 60_000L
    private const val NORMAL_MAX = 900_000L
    private const val SCHEDULE_MIN = 300_000L
    private const val SCHEDULE_MAX = 3_600_000L
    private const val MOVEMENT_MIN = 30_000L
    private const val MOVEMENT_MAX = 300_000L
    private const val RECOVERY_MIN = 25.0
    private const val RECOVERY_MAX = 500.0
    private const val ACCURACY_MIN = 10f
    private const val ACCURACY_MAX = 50f
    private const val ALTITUDE_MIN = 0L
    private const val ALTITUDE_MAX = 30_000L
    private val VALID_LOG_RETENTIONS = setOf(1, 7, 14, 30)

    fun validate(settings: TrackingSettings): List<ValidationError> {
        val errors = mutableListOf<ValidationError>()

        if (settings.normalIntervalMillis < NORMAL_MIN || settings.normalIntervalMillis > NORMAL_MAX) {
            errors.add(
                ValidationError(
                    "NORMAL_INTERVAL_OUT_OF_RANGE",
                    "normalIntervalMillis must be between $NORMAL_MIN and $NORMAL_MAX, got ${settings.normalIntervalMillis}"
                )
            )
        }

        if (settings.scheduleLowFrequencyIntervalMillis < SCHEDULE_MIN || settings.scheduleLowFrequencyIntervalMillis > SCHEDULE_MAX) {
            errors.add(
                ValidationError(
                    "SCHEDULE_INTERVAL_OUT_OF_RANGE",
                    "scheduleLowFrequencyIntervalMillis must be between $SCHEDULE_MIN and $SCHEDULE_MAX, got ${settings.scheduleLowFrequencyIntervalMillis}"
                )
            )
        }

        if (settings.movementIntervalMillis < MOVEMENT_MIN || settings.movementIntervalMillis > MOVEMENT_MAX) {
            errors.add(
                ValidationError(
                    "MOVEMENT_INTERVAL_OUT_OF_RANGE",
                    "movementIntervalMillis must be between $MOVEMENT_MIN and $MOVEMENT_MAX, got ${settings.movementIntervalMillis}"
                )
            )
        }

        if (!settings.scheduleRecoveryThresholdMeters.isFinite() || settings.scheduleRecoveryThresholdMeters < RECOVERY_MIN || settings.scheduleRecoveryThresholdMeters > RECOVERY_MAX) {
            errors.add(
                ValidationError(
                    "RECOVERY_THRESHOLD_OUT_OF_RANGE",
                    "scheduleRecoveryThresholdMeters must be between $RECOVERY_MIN and $RECOVERY_MAX, got ${settings.scheduleRecoveryThresholdMeters}"
                )
            )
        }

        if (!settings.maxUploadAccuracyMetersExclusive.isFinite() || settings.maxUploadAccuracyMetersExclusive < ACCURACY_MIN || settings.maxUploadAccuracyMetersExclusive > ACCURACY_MAX) {
            errors.add(
                ValidationError(
                    "ACCURACY_OUT_OF_RANGE",
                    "maxUploadAccuracyMetersExclusive must be between $ACCURACY_MIN and $ACCURACY_MAX, got ${settings.maxUploadAccuracyMetersExclusive}"
                )
            )
        }

        if (settings.altitudeWaitTimeoutMillis < ALTITUDE_MIN || settings.altitudeWaitTimeoutMillis > ALTITUDE_MAX) {
            errors.add(
                ValidationError(
                    "ALTITUDE_WAIT_OUT_OF_RANGE",
                    "altitudeWaitTimeoutMillis must be between $ALTITUDE_MIN and $ALTITUDE_MAX, got ${settings.altitudeWaitTimeoutMillis}"
                )
            )
        }

        if (settings.logRetentionDays !in VALID_LOG_RETENTIONS) {
            errors.add(
                ValidationError(
                    "LOG_RETENTION_INVALID",
                    "logRetentionDays must be one of $VALID_LOG_RETENTIONS, got ${settings.logRetentionDays}"
                )
            )
        }

        return errors
    }

    fun validateOrThrow(settings: TrackingSettings) {
        val errors = validate(settings)
        if (errors.isNotEmpty()) {
            throw ValidationException(errors)
        }
    }
}
