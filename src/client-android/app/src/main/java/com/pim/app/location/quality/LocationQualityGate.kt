package com.pim.app.location.quality

import com.pim.app.settings.TrackingSettings

data class RawLocationFix(
    val latitude: Double,
    val longitude: Double,
    val horizontalAccuracyMeters: Float?,
    val altitudeMeters: Double?,
    val provider: String,
    val recordedAtMillis: Long,
    val policyMode: String,
    val scheduleLowFrequency: Boolean,
    val motionSignal: String,
    val speedMetersPerSecond: Float? = null,
    val bearingDegrees: Float? = null
)

data class QualityAcceptedLocation(
    val fix: RawLocationFix,
    val altitudeMeters: Double?,
    val acceptedAtMillis: Long,
    val qualityFlags: Set<String>
)

data class PendingAltitudeFix(
    val fix: RawLocationFix,
    val deadlineMillis: Long
)

sealed class QualityDecision {
    data class AcceptNow(val accepted: QualityAcceptedLocation) : QualityDecision()
    data class WaitForAltitude(val pending: PendingAltitudeFix) : QualityDecision()
    data class Drop(val fix: RawLocationFix, val reason: String) : QualityDecision()
}

class LocationQualityGate(
    private val maxAccuracyMetersExclusive: Float = MAX_ACCURACY_METERS_EXCLUSIVE,
    private val altitudeWaitTimeoutMillis: Long = 15_000L
) {
    fun evaluate(fix: RawLocationFix, nowMillis: Long = fix.recordedAtMillis): QualityDecision {
        val accuracy = fix.horizontalAccuracyMeters
            ?: return QualityDecision.Drop(fix, "missing-horizontal-accuracy")

        if (!accuracy.isFinite() || accuracy >= maxAccuracyMetersExclusive) {
            return QualityDecision.Drop(fix, "horizontal-accuracy-too-low")
        }

        val altitude = fix.altitudeMeters
        return if (altitude != null) {
            QualityDecision.AcceptNow(
                QualityAcceptedLocation(
                    fix = fix,
                    altitudeMeters = altitude,
                    acceptedAtMillis = nowMillis,
                    qualityFlags = emptySet()
                )
            )
        } else {
            QualityDecision.WaitForAltitude(
                PendingAltitudeFix(
                    fix = fix,
                    deadlineMillis = fix.recordedAtMillis + altitudeWaitTimeoutMillis
                )
            )
        }
    }

    fun timeoutDecision(pending: PendingAltitudeFix, nowMillis: Long): QualityDecision {
        if (nowMillis < pending.deadlineMillis) {
            return QualityDecision.WaitForAltitude(pending)
        }

        return QualityDecision.AcceptNow(
            QualityAcceptedLocation(
                fix = pending.fix,
                altitudeMeters = null,
                acceptedAtMillis = nowMillis,
                qualityFlags = setOf("altitude-missing-timeout")
            )
        )
    }

    companion object {
        const val MAX_ACCURACY_METERS_EXCLUSIVE = 20f
        const val LOW_QUALITY_ACCURACY_FLAG = "low-quality-accuracy"

        fun fromTrackingSettings(settings: TrackingSettings): LocationQualityGate =
            LocationQualityGate(
                altitudeWaitTimeoutMillis = settings.altitudeWaitTimeoutMillis
            )
    }
}
