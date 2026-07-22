package com.pim.app.location.acquisition

import android.os.SystemClock
import com.pim.app.location.LocationSnapshot
import com.pim.app.location.quality.AltitudeWaitCoordinator
import com.pim.app.location.quality.LocationQualityGate
import com.pim.app.location.quality.QualityAcceptedLocation
import com.pim.app.location.quality.RawLocationFix
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import java.util.UUID
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference
import javax.inject.Inject
import javax.inject.Singleton

interface LocationAcquisitionOperations {
    suspend fun enqueueAccepted(accepted: QualityAcceptedLocation, rawJson: String, source: String)
    suspend fun recordDropped(fix: RawLocationFix, reason: String)
    fun scheduleSync()
}

@Singleton
class LocationAcquisitionCoordinator @Inject constructor(
    private val runner: LocationAcquisitionRunner,
    private val prerequisiteChecker: LocationPrerequisiteChecker,
    private val operations: LocationAcquisitionOperations,
    private val json: Json
) {
    internal var testScope: CoroutineScope? = null
    internal var uuidGenerator: () -> String = { UUID.randomUUID().toString() }
    internal var wallClockMillis: () -> Long = { System.currentTimeMillis() }
    internal var elapsedRealtimeMillis: () -> Long = { SystemClock.elapsedRealtime() }

    private val internalScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val scope: CoroutineScope get() = testScope ?: internalScope

    private val _state = MutableStateFlow(LocationAcquisitionState())
    val state: StateFlow<LocationAcquisitionState> = _state.asStateFlow()

    private var sessionJob: Job? = null
    private var pendingAccepted: QualityAcceptedLocation? = null

    fun startManualSession(replaceAwaitingManual: Boolean = false): SessionStartResult {
        val current = _state.value
        if (current.isBusy) {
            if (current.phase == AcquisitionPhase.AwaitingManualSubmit && replaceAwaitingManual) {
                val oldJob = sessionJob
                oldJob?.cancel()
                clearSessionJobIfOwned(oldJob)
                pendingAccepted = null
            } else {
                return SessionStartResult.Busy
            }
        }

        when (val precheck = prerequisiteChecker.check(TriggerType.MANUAL)) {
            is LocationPrerequisiteResult.Blocked -> {
                _state.value = current.copy(
                    phase = AcquisitionPhase.Idle,
                    sessionId = null,
                    triggerType = null,
                    errorReason = precheck.reason
                )
                return SessionStartResult.Rejected(precheck.reason)
            }
            is LocationPrerequisiteResult.Ready -> {}
        }

        val sessionId = uuidGenerator()
        startSession(sessionId, TriggerType.MANUAL, null)
        return SessionStartResult.Started(sessionId)
    }

    fun startAutomaticSession(context: AutomaticSessionContext): SessionStartResult {
        if (_state.value.isBusy) return SessionStartResult.Busy

        when (val precheck = prerequisiteChecker.check(TriggerType.AUTOMATIC)) {
            is LocationPrerequisiteResult.Blocked -> {
                _state.value = _state.value.copy(
                    phase = AcquisitionPhase.Idle,
                    sessionId = null,
                    triggerType = null,
                    errorReason = precheck.reason
                )
                return SessionStartResult.Rejected(precheck.reason)
            }
            is LocationPrerequisiteResult.Ready -> {}
        }

        val sessionId = uuidGenerator()
        startSession(sessionId, TriggerType.AUTOMATIC, context)
        return SessionStartResult.Started(sessionId)
    }

    fun cancelCurrentSession(expectedSessionId: String? = null) {
        val current = _state.value
        if (expectedSessionId != null && current.sessionId != expectedSessionId) return
        if (current.phase == AcquisitionPhase.Idle) return

        val job = sessionJob
        job?.cancel()
        clearSessionJobIfOwned(job)
        pendingAccepted = null
        updateStateIfCurrent(current.sessionId, job) {
            it.copy(phase = AcquisitionPhase.Cancelled, sessionId = null, errorReason = null)
        }
    }

    fun submitManualResult() {
        val claim = claimManualSubmission() ?: return

        scope.launch {
            if (!isCurrentSession(claim.sessionId) ||
                _state.value.phase != AcquisitionPhase.Enqueuing
            ) {
                return@launch
            }
            try {
                val rawJson = rawJson(claim.accepted, TriggerType.MANUAL.storageSource)
                operations.enqueueAccepted(
                    claim.accepted,
                    rawJson,
                    TriggerType.MANUAL.storageSource
                )
                updateStateIfCurrent(claim.sessionId, claim.ownerJob) {
                    it.copy(phase = AcquisitionPhase.Completed, errorReason = null)
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                updateStateIfCurrent(claim.sessionId, claim.ownerJob) {
                    it.copy(
                        phase = AcquisitionPhase.AwaitingManualSubmit,
                        errorReason = e.message
                    )
                }
            }
        }
    }

    private fun claimManualSubmission(): ManualSubmissionClaim? {
        while (true) {
            val current = _state.value
            if (current.phase != AcquisitionPhase.AwaitingManualSubmit) return null

            val accepted = pendingAccepted ?: return null
            val sessionId = current.sessionId ?: return null
            val claimed = current.copy(
                phase = AcquisitionPhase.Enqueuing,
                errorReason = null
            )
            if (_state.compareAndSet(current, claimed)) {
                return ManualSubmissionClaim(
                    accepted = accepted,
                    sessionId = sessionId,
                    ownerJob = sessionJob
                )
            }
        }
    }

    private fun startSession(
        sessionId: String,
        triggerType: TriggerType,
        context: AutomaticSessionContext?
    ) {
        val nowElapsed = elapsedRealtimeMillis()
        pendingAccepted = null
        _state.value = LocationAcquisitionState(
            sessionId = sessionId,
            triggerType = triggerType,
            phase = AcquisitionPhase.Preparing,
            startedAtElapsedRealtimeMs = nowElapsed,
            deadlineAtElapsedRealtimeMs = nowElapsed + 30_000L
        )
        lateinit var job: Job
        val newJob = scope.launch(start = CoroutineStart.LAZY) {
            runSession(sessionId, triggerType, context, job)
        }
        job = newJob
        sessionJob = newJob
        newJob.start()
    }

    private suspend fun runSession(
        sessionId: String,
        triggerType: TriggerType,
        context: AutomaticSessionContext?,
        ownerJob: Job
    ) = coroutineScope {
        val tickerJob = launch {
            while (isActive) {
                delay(1_000L)
                updateStateIfCurrent(sessionId, ownerJob) { state ->
                    val start = state.startedAtElapsedRealtimeMs
                    if (start != null) {
                        state.copy(elapsedMs = elapsedRealtimeMillis() - start)
                    } else {
                        state
                    }
                }
            }
        }
        try {
            acquireAndEvaluate(sessionId, triggerType, context, ownerJob)
        } catch (e: CancellationException) {
            updateStateIfCurrent(sessionId, ownerJob) {
                it.copy(phase = AcquisitionPhase.Cancelled)
            }
            throw e
        } catch (e: Exception) {
            updateStateIfCurrent(sessionId, ownerJob) {
                it.copy(phase = AcquisitionPhase.Failed, errorReason = e.message)
            }
        } finally {
            tickerJob.cancel()
            clearSessionJobIfOwned(ownerJob)
        }
    }

    private suspend fun acquireAndEvaluate(
        sessionId: String,
        triggerType: TriggerType,
        context: AutomaticSessionContext?,
        ownerJob: Job
    ) = coroutineScope {
        updateStateIfCurrent(sessionId, ownerJob) {
            it.copy(phase = AcquisitionPhase.Acquiring)
        }

        val sessionScope = this
        val sessionStartedWallClockMillis = wallClockMillis()
        val deadlineCapMillis = sessionStartedWallClockMillis + 30_000L
        val altitudeWaitCoordinator = AltitudeWaitCoordinator(
            gate = LocationQualityGate(),
            nowMillis = wallClockMillis,
            delayMillis = { delay(it) }
        )

        val request = LocationEngineRequest(
            sessionId = sessionId,
            priority = context?.priority ?: 100,
            timeoutMillis = 30_000L,
            startedAtWallClockMillis = sessionStartedWallClockMillis
        )

        val bestSnapshot = AtomicReference<LocationSnapshot?>(null)
        val acceptedLocation = AtomicReference<QualityAcceptedLocation?>(null)
        val qualityAccepted = AtomicBoolean(false)
        val qualityJobs = mutableListOf<Job>()
        val engineJobRef = AtomicReference<Job?>(null)

        fun onQualityAccepted(accepted: QualityAcceptedLocation) {
            if (qualityAccepted.compareAndSet(false, true)) {
                acceptedLocation.set(accepted)
                altitudeWaitCoordinator.cancelPending()
                engineJobRef.get()?.cancel()
            }
        }

        val engineJob = launch {
            try {
                runner.acquire(
                    request = request,
                    onCandidate = { snapshot ->
                        if (!isCurrentSession(sessionId) || qualityAccepted.get()) return@acquire
                        bestSnapshot.set(snapshot)
                        updateStateIfCurrent(sessionId, ownerJob) {
                            it.copy(bestLocation = snapshot, phase = AcquisitionPhase.Evaluating)
                        }

                        val fix = snapshot.toRawFix(triggerType, context)
                        // Concurrent quality work: later candidates can satisfy altitude while an
                        // earlier missing-altitude wait is still suspended inside the coordinator.
                        val qualityJob = sessionScope.launch {
                            altitudeWaitCoordinator.handleFix(
                                fix = fix,
                                deadlineCapMillis = deadlineCapMillis,
                                onAccepted = { accepted -> onQualityAccepted(accepted) },
                                onDropped = { droppedFix, reason ->
                                    if (triggerType == TriggerType.AUTOMATIC) {
                                        recordDrop(droppedFix, reason)
                                    }
                                }
                            )
                        }
                        qualityJobs += qualityJob
                    }
                )
            } catch (_: CancellationException) {
                // quality acceptance or external cancel
            }
        }
        engineJobRef.set(engineJob)

        engineJob.join()
        qualityJobs.toList().forEach { job ->
            if (job.isActive) {
                job.join()
            }
        }

        if (!isCurrentSession(sessionId)) return@coroutineScope

        val accepted = acceptedLocation.get()
        if (accepted != null) {
            handleAccepted(accepted, sessionId, triggerType, ownerJob)
            return@coroutineScope
        }

        val best = bestSnapshot.get()
        if (best != null) {
            updateStateIfCurrent(sessionId, ownerJob) {
                it.copy(phase = AcquisitionPhase.Failed, bestLocation = best)
            }
        } else {
            updateStateIfCurrent(sessionId, ownerJob) {
                it.copy(phase = AcquisitionPhase.TimedOut)
            }
        }
    }

    private suspend fun handleAccepted(
        accepted: QualityAcceptedLocation,
        sessionId: String,
        triggerType: TriggerType,
        ownerJob: Job
    ) {
        if (!isCurrentSession(sessionId)) return

        if (triggerType == TriggerType.MANUAL) {
            pendingAccepted = accepted
            updateStateIfCurrent(sessionId, ownerJob) {
                it.copy(phase = AcquisitionPhase.AwaitingManualSubmit)
            }
        } else {
            updateStateIfCurrent(sessionId, ownerJob) {
                it.copy(phase = AcquisitionPhase.Enqueuing)
            }
            try {
                val json = rawJson(accepted, triggerType.storageSource)
                operations.enqueueAccepted(accepted, json, triggerType.storageSource)
                // Record is already enqueued; schedule sync at most once even if session changed.
                operations.scheduleSync()
                updateStateIfCurrent(sessionId, ownerJob) {
                    it.copy(phase = AcquisitionPhase.Completed)
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                updateStateIfCurrent(sessionId, ownerJob) {
                    it.copy(phase = AcquisitionPhase.Failed, errorReason = e.message)
                }
            }
        }
    }

    private suspend fun recordDrop(fix: RawLocationFix, reason: String) {
        try {
            operations.recordDropped(fix, reason)
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
        }
    }

    private fun clearSessionJobIfOwned(ownerJob: Job?) {
        if (ownerJob != null && sessionJob === ownerJob) {
            sessionJob = null
        }
    }

    private fun updateStateIfCurrent(
        sessionId: String?,
        ownerJob: Job? = null,
        transform: (LocationAcquisitionState) -> LocationAcquisitionState
    ) {
        if (sessionId == null) return
        if (ownerJob != null && sessionJob != null && sessionJob !== ownerJob) return
        if (_state.value.sessionId != sessionId) return
        _state.update { current ->
            if (current.sessionId != sessionId) current else transform(current)
        }
    }

    private fun isCurrentSession(sessionId: String?): Boolean =
        sessionId != null && _state.value.sessionId == sessionId

    private fun rawJson(accepted: QualityAcceptedLocation, source: String): String {
        val fix = accepted.fix
        val payload = buildJsonObject {
            put("latitude", JsonPrimitive(fix.latitude))
            put("longitude", JsonPrimitive(fix.longitude))
            put(
                "horizontalAccuracyMeters",
                finiteJsonNumberOrNull(fix.horizontalAccuracyMeters?.toDouble())
            )
            put("provider", JsonPrimitive(fix.provider))
            put("source", JsonPrimitive(source))
            put("altitudeMeters", finiteJsonNumberOrNull(accepted.altitudeMeters))
            put(
                "speedMetersPerSecond",
                finiteJsonNumberOrNull(fix.speedMetersPerSecond?.toDouble())
            )
            put(
                "bearingDegrees",
                finiteJsonNumberOrNull(fix.bearingDegrees?.toDouble())
            )
            put("recordedAtUnixMs", JsonPrimitive(fix.recordedAtMillis))
            put("submittedAtUnixMs", JsonPrimitive(wallClockMillis()))
            put("policyMode", JsonPrimitive(fix.policyMode))
            put("scheduleLowFrequency", JsonPrimitive(fix.scheduleLowFrequency))
            put("motionSignal", JsonPrimitive(fix.motionSignal))
            put(
                "qualityFlags",
                buildJsonArray {
                    accepted.qualityFlags.sorted().forEach { add(JsonPrimitive(it)) }
                }
            )
        }
        return json.encodeToString(JsonElement.serializer(), payload)
    }

    private fun finiteJsonNumberOrNull(value: Double?): JsonElement =
        if (value == null || !value.isFinite()) JsonNull else JsonPrimitive(value)

    private fun LocationSnapshot.toRawFix(
        triggerType: TriggerType,
        context: AutomaticSessionContext?
    ): RawLocationFix = RawLocationFix(
        latitude = latitude,
        longitude = longitude,
        horizontalAccuracyMeters = horizontalAccuracyMeters,
        altitudeMeters = altitudeMeters,
        provider = provider,
        recordedAtMillis = timeMillis,
        policyMode = context?.policyMode ?: "PowerSavingNormal",
        scheduleLowFrequency = context?.scheduleLowFrequency ?: false,
        motionSignal = context?.motionSignal ?: "Unknown",
        speedMetersPerSecond = speedMetersPerSecond,
        bearingDegrees = bearingDegrees
    )

    private data class ManualSubmissionClaim(
        val accepted: QualityAcceptedLocation,
        val sessionId: String,
        val ownerJob: Job?
    )
}
