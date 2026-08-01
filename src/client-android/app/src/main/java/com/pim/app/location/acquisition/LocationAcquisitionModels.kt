package com.pim.app.location.acquisition

import com.pim.app.location.LocationSnapshot

enum class TriggerType(val storageSource: String) {
    MANUAL("manual"),
    AUTOMATIC("auto")
}

enum class AcquisitionPhase {
    Idle,
    Preparing,
    Acquiring,
    Evaluating,
    AwaitingManualSubmit,
    Enqueuing,
    Completed,
    TimedOut,
    Failed,
    Cancelled
}

data class AutomaticSessionContext(
    val priority: Int,
    val policyMode: String,
    val scheduleLowFrequency: Boolean,
    val motionSignal: String
)

data class LocationAcquisitionState(
    val sessionId: String? = null,
    val triggerType: TriggerType? = null,
    val phase: AcquisitionPhase = AcquisitionPhase.Idle,
    val bestLocation: LocationSnapshot? = null,
    val startedAtElapsedRealtimeMs: Long? = null,
    val deadlineAtElapsedRealtimeMs: Long? = null,
    val elapsedMs: Long = 0L,
    val maxUploadAccuracyMetersExclusive: Float = 50f,
    val errorReason: String? = null
) {
    val isBusy: Boolean
        get() = phase in setOf(
            AcquisitionPhase.Preparing,
            AcquisitionPhase.Acquiring,
            AcquisitionPhase.Evaluating,
            AcquisitionPhase.AwaitingManualSubmit,
            AcquisitionPhase.Enqueuing
        )
}

sealed interface SessionStartResult {
    data class Started(val sessionId: String) : SessionStartResult
    data object Busy : SessionStartResult
    data class Rejected(val reason: String) : SessionStartResult
}
