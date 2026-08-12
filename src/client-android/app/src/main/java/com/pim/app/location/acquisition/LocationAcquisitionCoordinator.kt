package com.pim.app.location.acquisition

import android.os.SystemClock
import com.pim.app.location.LocationSnapshot
import com.pim.app.location.quality.AltitudeWaitCoordinator
import com.pim.app.location.quality.LocationQualityGate
import com.pim.app.location.quality.QualityAcceptedLocation
import com.pim.app.location.quality.RawLocationFix
import com.pim.app.settings.TrackingSettings
import com.pim.app.settings.TrackingSettingsStore
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
    private val json: Json,
    private val trackingSettingsStore: TrackingSettingsStore
) {
    internal var testScope: CoroutineScope? = null
    internal var uuidGenerator: () -> String = { UUID.randomUUID().toString() }
    internal var wallClockMillis: () -> Long = { System.currentTimeMillis() }
    internal var elapsedRealtimeMillis: () -> Long = { SystemClock.elapsedRealtime() }
    // Test seam: invoked in the automatic-acceptance path immediately before the
    // Enqueuing claim, so a cancellation TOCTOU can be reproduced deterministically.
    internal var beforeAutomaticEnqueueClaim: (() -> Unit)? = null
    // Test seam: invoked in cancelCurrentSession after a cancellable state snapshot
    // is read but before the CAS to Cancelled, so a stale-read interleaving with the
    // automatic Enqueuing claim can be reproduced deterministically.
    internal var beforeCancellingSessionJob: (() -> Unit)? = null
    // Test seam: invoked in cancelCurrentSession immediately after the Cancelled
    // state claim wins and before the session job is cancelled/cleared, so a new
    // session starting in that window can be reproduced deterministically.
    internal var afterSessionCancelledClaim: (() -> Unit)? = null

    private val internalScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val scope: CoroutineScope get() = testScope ?: internalScope

    private val _state = MutableStateFlow(LocationAcquisitionState())
    val state: StateFlow<LocationAcquisitionState> = _state.asStateFlow()

    private var sessionJob: Job? = null
    private var pendingAccepted: QualityAcceptedLocation? = null

    fun startManualSession(replaceAwaitingManual: Boolean = false): SessionStartResult {
        val current = _state.value
        val replacing = current.isBusy &&
            current.phase == AcquisitionPhase.AwaitingManualSubmit &&
            replaceAwaitingManual

        if (current.isBusy && !replacing) return SessionStartResult.Busy

        // Check prerequisites before replacing: a blocked restart must not
        // destroy the accepted-but-unsubmitted manual result.
        when (val precheck = prerequisiteChecker.check(TriggerType.MANUAL)) {
            is LocationPrerequisiteResult.Blocked -> {
                if (!replacing) {
                    _state.value = current.copy(
                        phase = AcquisitionPhase.Idle,
                        sessionId = null,
                        triggerType = null,
                        errorReason = precheck.reason
                    )
                }
                return SessionStartResult.Rejected(precheck.reason)
            }
            is LocationPrerequisiteResult.Ready -> {}
        }

        if (replacing) {
            val oldJob = sessionJob
            oldJob?.cancel()
            clearSessionJobIfOwned(oldJob)
            pendingAccepted = null
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

    fun cancelCurrentSession(expectedSessionId: String? = null): Boolean {
        val cancellablePhases = setOf(
            AcquisitionPhase.Preparing,
            AcquisitionPhase.Acquiring,
            AcquisitionPhase.Evaluating,
            AcquisitionPhase.AwaitingManualSubmit
        )
        while (true) {
            val current = _state.value
            if (expectedSessionId != null && current.sessionId != expectedSessionId) return false
            // Only genuinely cancellable phases may be cancelled. Idle and Enqueuing
            // are excluded (an in-flight confirmed submission must not be represented
            // as cancelled), and terminal phases keep their result: a late cancel
            // intent must not relabel a completed/timed-out/failed/cancelled session.
            if (current.phase !in cancellablePhases) return false

            // Capture the job that owns this snapshot's session while the snapshot
            // is still current: only this job may be cancelled once the Cancelled
            // claim wins. A session started after the claim owns a different job.
            val ownerJob = sessionJob
            beforeCancellingSessionJob?.invoke()

            if (_state.compareAndSet(
                    current,
                    current.copy(phase = AcquisitionPhase.Cancelled, errorReason = null)
                )
            ) {
                // The Cancelled claim won; only now may the captured job be cancelled
                // and its ownership cleared. A lost CAS (e.g. the automatic Evaluating
                // -> Enqueuing claim) is retried and observed as not cancellable.
                // Test seam: invoked immediately after the successful Cancelled claim
                // and before the captured job is cancelled/cleared, so a new session
                // starting in that window can be reproduced deterministically.
                afterSessionCancelledClaim?.invoke()
                ownerJob?.cancel()
                clearSessionJobIfOwned(ownerJob)
                // Stale pendingAccepted (e.g. an accepted-but-unsubmitted manual
                // result) is intentionally retained: claimManualSubmission is gated
                // on the AwaitingManualSubmit phase and the next startSession resets
                // it, so this cleanup can never discard a newer session's result.
                return true
            }
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
                // Record is already enqueued; schedule sync at most once even if session changed.
                operations.scheduleSync()
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

    // Atomic claim of the automatic Enqueuing transition. Returns true only when
    // the transition actually happened; a session already terminal (e.g. Cancelled
    // after a quality accept) must never be enqueued, and once claimed, the
    // in-flight enqueue is the user-approved completion that later cancels ignore.
    private fun claimAutomaticEnqueuing(sessionId: String?): Boolean {
        if (sessionId == null) return false
        while (true) {
            val current = _state.value
            if (current.sessionId != sessionId) return false
            if (current.phase != AcquisitionPhase.Evaluating) return false
            if (_state.compareAndSet(current, current.copy(phase = AcquisitionPhase.Enqueuing))) {
                return true
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
        // Snapshot tracking settings once per session; later changes only affect
        // the next session, never the active one.
        val settings = trackingSettingsStore.read()
        _state.value = LocationAcquisitionState(
            sessionId = sessionId,
            triggerType = triggerType,
            phase = AcquisitionPhase.Preparing,
            startedAtElapsedRealtimeMs = nowElapsed,
            deadlineAtElapsedRealtimeMs = nowElapsed + 30_000L,
            maxUploadAccuracyMetersExclusive = settings.maxUploadAccuracyMetersExclusive
        )
        lateinit var job: Job
        val newJob = scope.launch(start = CoroutineStart.LAZY) {
            runSession(sessionId, triggerType, context, job, settings)
        }
        job = newJob
        sessionJob = newJob
        newJob.start()
    }

    private suspend fun runSession(
        sessionId: String,
        triggerType: TriggerType,
        context: AutomaticSessionContext?,
        ownerJob: Job,
        settings: TrackingSettings
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
            acquireAndEvaluate(sessionId, triggerType, context, ownerJob, settings)
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
        ownerJob: Job,
        settings: TrackingSettings
    ) = coroutineScope {
        updateStateIfCurrent(sessionId, ownerJob) {
            it.copy(phase = AcquisitionPhase.Acquiring)
        }

        val sessionScope = this
        val sessionStartedWallClockMillis = wallClockMillis()
        val sessionStartedElapsedRealtimeMillis = elapsedRealtimeMillis()
        val deadlineCapMillis = sessionStartedWallClockMillis + 30_000L
        val deadlineCapElapsedRealtimeMillis = sessionStartedElapsedRealtimeMillis + 30_000L
        val altitudeWaitCoordinator = AltitudeWaitCoordinator(
            gate = LocationQualityGate.fromTrackingSettings(settings),
            nowMillis = wallClockMillis,
            nowElapsedRealtimeMillis = elapsedRealtimeMillis,
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
        val engineResult = AtomicReference<LocationEngineResult?>(null)
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
                engineResult.set(
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
                                    deadlineCapElapsedRealtimeMillis = deadlineCapElapsedRealtimeMillis,
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
        // A cancelled terminal state retains its sessionId; leftover session work
        // must not turn it into Failed/TimedOut or start an enqueue.
        if (_state.value.phase == AcquisitionPhase.Cancelled) return@coroutineScope

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
            val completion = engineResult.get()?.completion
            if (completion is LocationEngineCompletion.Failed) {
                updateStateIfCurrent(sessionId, ownerJob) {
                    it.copy(phase = AcquisitionPhase.Failed, errorReason = completion.reason)
                }
            } else {
                updateStateIfCurrent(sessionId, ownerJob) {
                    it.copy(phase = AcquisitionPhase.TimedOut)
                }
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
            beforeAutomaticEnqueueClaim?.invoke()
            if (!claimAutomaticEnqueuing(sessionId)) return
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
            if (current.sessionId != sessionId) return@update current
            val next = transform(current)
            // The Cancelled terminal state is final for this session: leftover
            // work must not resurrect or relabel it.
            if (current.phase == AcquisitionPhase.Cancelled &&
                next.phase != AcquisitionPhase.Cancelled
            ) {
                current
            } else {
                next
            }
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
