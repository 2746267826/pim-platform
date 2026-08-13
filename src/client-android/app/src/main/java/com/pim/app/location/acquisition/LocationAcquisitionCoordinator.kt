package com.pim.app.location.acquisition

import android.os.SystemClock
import com.google.android.gms.location.Priority
import com.pim.app.location.LocationSnapshot
import com.pim.app.location.quality.AltitudeWaitCoordinator
import com.pim.app.location.quality.LocationQualityGate
import com.pim.app.location.quality.QualityAcceptedLocation
import com.pim.app.location.quality.QualityDecision
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

/**
 * 统一采集引擎（设计文档 §3.1/§3.3）：
 * - 手动触发 = 立即执行一次同一引擎（一次性采集，30s 截止，达标入库；超时用
 *   最好 fix 并标记 low-quality，绝不静默）。
 * - 自动采集 = 同一引擎的常驻流：注册时先预热等 GPS 收敛（冷启动），随后系统按
 *   interval 回调 fix，逐点过 20m 质量门入库；≥20m 的 fix 走 drop 诊断，不回退。
 * - priority 恒为 HIGH_ACCURACY（§3.2）；省电只靠采样间隔。
 * 手动会话状态走 [state]，自动流状态走 [streamState]，两者互不干扰。
 */
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
    // Test seam: invoked in cancelCurrentSession after a cancellable state snapshot
    // is read but before the CAS to Cancelled, so a stale-read interleaving with an
    // in-flight acceptance can be reproduced deterministically.
    internal var beforeCancellingSessionJob: (() -> Unit)? = null
    // Test seam: invoked in cancelCurrentSession immediately after the Cancelled
    // state claim wins and before the session job is cancelled/cleared.
    internal var afterSessionCancelledClaim: (() -> Unit)? = null
    // Invoked after every recorded point (manual or stream), so the service can
    // update the policy anchor, notification texts and the next-fix countdown.
    internal var onRecorded: (suspend (LocationSnapshot) -> Unit)? = null

    private val internalScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val scope: CoroutineScope get() = testScope ?: internalScope

    private val _state = MutableStateFlow(LocationAcquisitionState())
    val state: StateFlow<LocationAcquisitionState> = _state.asStateFlow()

    private val _streamState = MutableStateFlow(AutomaticStreamState())
    val streamState: StateFlow<AutomaticStreamState> = _streamState.asStateFlow()

    private var sessionJob: Job? = null
    private var streamJob: Job? = null

    // ─── 手动一次性采集 ─────────────────────────────────────────

    fun startManualSession(): SessionStartResult {
        // Restart semantics: an in-flight manual one-shot is replaced by the new one.
        cancelCurrentSession(_state.value.sessionId)

        when (val precheck = prerequisiteChecker.check(TriggerType.MANUAL)) {
            is LocationPrerequisiteResult.Blocked -> {
                _state.value = LocationAcquisitionState(
                    phase = AcquisitionPhase.Idle,
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

    fun cancelCurrentSession(expectedSessionId: String? = null): Boolean {
        val cancellablePhases = setOf(
            AcquisitionPhase.Preparing,
            AcquisitionPhase.Acquiring,
            AcquisitionPhase.Evaluating
        )
        while (true) {
            val current = _state.value
            if (expectedSessionId != null && current.sessionId != expectedSessionId) return false
            if (current.phase !in cancellablePhases) return false

            val ownerJob = sessionJob
            beforeCancellingSessionJob?.invoke()

            if (_state.compareAndSet(
                    current,
                    current.copy(phase = AcquisitionPhase.Cancelled, errorReason = null)
                )
            ) {
                afterSessionCancelledClaim?.invoke()
                ownerJob?.cancel()
                clearSessionJobIfOwned(ownerJob)
                return true
            }
        }
    }

    // ─── 自动常驻流 ─────────────────────────────────────────────

    fun startAutomaticStream(context: AcquisitionContext): Boolean {
        if (_streamState.value.active) return updateAutomaticStream(context)
        startStreamJob(context, warmUp = true)
        return true
    }

    fun updateAutomaticStream(context: AcquisitionContext): Boolean {
        if (!_streamState.value.active) {
            startStreamJob(context, warmUp = true)
            return true
        }
        if (_streamState.value.requestIntervalMillis != context.requestIntervalMillis) {
            startStreamJob(context, warmUp = false)
        }
        return true
    }

    fun stopAutomaticStream() {
        streamJob?.cancel()
        streamJob = null
        _streamState.value = AutomaticStreamState()
    }

    fun isAutomaticStreamActive(): Boolean = _streamState.value.active

    private fun startStreamJob(context: AcquisitionContext, warmUp: Boolean) {
        streamJob?.cancel()
        lateinit var job: Job
        val newJob = scope.launch(start = CoroutineStart.LAZY) {
            _streamState.update {
                it.copy(
                    active = true,
                    requestIntervalMillis = context.requestIntervalMillis,
                    lastError = null
                )
            }
            try {
                if (warmUp) {
                    warmUpOnce(context)
                }
                val request = LocationUpdateRequest(
                    priority = Priority.PRIORITY_HIGH_ACCURACY,
                    intervalMillis = context.requestIntervalMillis,
                    durationMillis = 0L
                )
                runner.stream(request) { snapshot ->
                    handleStreamFix(snapshot, context)
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _streamState.update { it.copy(lastError = e.message) }
            }
        }
        job = newJob
        streamJob = newJob
        newJob.start()
    }

    /** 冷启动预热：等 GPS 收敛（≤30s），接受首个 <20m fix 并入库；不回退。 */
    private suspend fun warmUpOnce(context: AcquisitionContext) {
        val settings = trackingSettingsStore.read()
        val sessionId = "warmup-${uuidGenerator()}"
        when (val outcome = acquireOneShot(
            sessionId = sessionId,
            triggerType = TriggerType.AUTOMATIC,
            context = context,
            ownerJob = null,
            settings = settings,
            sink = StateSink.STREAM,
            allowLowQualityFallback = false
        )) {
            is OneShotOutcome.Accepted -> {
                try {
                    val raw = rawJson(outcome.accepted, TriggerType.AUTOMATIC.storageSource)
                    operations.enqueueAccepted(outcome.accepted, raw, TriggerType.AUTOMATIC.storageSource)
                    operations.scheduleSync()
                    val snapshot = outcome.accepted.fix.toSnapshot()
                    _streamState.update {
                        it.copy(
                            latestFix = snapshot,
                            latestQualityFlags = outcome.accepted.qualityFlags,
                            lastError = null
                        )
                    }
                    onRecorded?.invoke(snapshot)
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    _streamState.update { it.copy(lastError = e.message) }
                }
            }
            is OneShotOutcome.Failed -> {
                _streamState.update { it.copy(lastError = it.lastError ?: outcome.reason) }
            }
            is OneShotOutcome.NoFix -> {
                // 预热未收敛不阻塞常驻流注册
            }
        }
    }

    private suspend fun handleStreamFix(snapshot: LocationSnapshot, context: AcquisitionContext) {
        val fix = snapshot.toRawFix(TriggerType.AUTOMATIC, context)
        val gate = LocationQualityGate.fromTrackingSettings(trackingSettingsStore.read())
        when (val decision = gate.evaluate(fix, wallClockMillis())) {
            is QualityDecision.AcceptNow -> {
                enqueueStreamFix(decision.accepted, snapshot, decision.accepted.qualityFlags)
            }
            is QualityDecision.WaitForAltitude -> {
                // 流模式不做 15s 等待：缺海拔直接带标记接受（GPS 热态下海拔基本都有）
                enqueueStreamFix(
                    QualityAcceptedLocation(
                        fix = fix,
                        altitudeMeters = null,
                        acceptedAtMillis = wallClockMillis(),
                        qualityFlags = setOf(STREAM_ALTITUDE_MISSING_FLAG)
                    ),
                    snapshot,
                    setOf(STREAM_ALTITUDE_MISSING_FLAG)
                )
            }
            is QualityDecision.Drop -> {
                recordDrop(decision.fix, decision.reason)
            }
        }
    }

    private suspend fun enqueueStreamFix(
        accepted: QualityAcceptedLocation,
        snapshot: LocationSnapshot,
        flags: Set<String>
    ) {
        try {
            val raw = rawJson(accepted, TriggerType.AUTOMATIC.storageSource)
            operations.enqueueAccepted(accepted, raw, TriggerType.AUTOMATIC.storageSource)
            operations.scheduleSync()
            _streamState.update {
                it.copy(latestFix = snapshot, latestQualityFlags = flags, lastError = null)
            }
            onRecorded?.invoke(snapshot)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            _streamState.update { it.copy(lastError = e.message) }
        }
    }

    // ─── 会话内部（一次性采集，手动/自动预热共用） ───────────────

    private enum class StateSink { SESSION, STREAM }

    private sealed interface OneShotOutcome {
        data class Accepted(val accepted: QualityAcceptedLocation) : OneShotOutcome
        data object NoFix : OneShotOutcome
        data class Failed(val reason: String) : OneShotOutcome
    }

    private fun startSession(
        sessionId: String,
        triggerType: TriggerType,
        context: AcquisitionContext?
    ) {
        val nowElapsed = elapsedRealtimeMillis()
        // Snapshot tracking settings once per session; later changes only affect
        // the next session, never the active one.
        val settings = trackingSettingsStore.read()
        _state.value = LocationAcquisitionState(
            sessionId = sessionId,
            triggerType = triggerType,
            phase = AcquisitionPhase.Preparing,
            startedAtElapsedRealtimeMs = nowElapsed,
            deadlineAtElapsedRealtimeMs = nowElapsed + 30_000L
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
        context: AcquisitionContext?,
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
            when (val outcome = acquireOneShot(
                sessionId = sessionId,
                triggerType = triggerType,
                context = context,
                ownerJob = ownerJob,
                settings = settings,
                sink = StateSink.SESSION,
                allowLowQualityFallback = true
            )) {
                is OneShotOutcome.Accepted ->
                    handleAccepted(outcome.accepted, sessionId, triggerType, ownerJob)
                is OneShotOutcome.NoFix ->
                    updateStateIfCurrent(sessionId, ownerJob) {
                        it.copy(
                            phase = AcquisitionPhase.TimedOut,
                            errorReason = "获取位置超时，未获得任何定位结果"
                        )
                    }
                is OneShotOutcome.Failed ->
                    updateStateIfCurrent(sessionId, ownerJob) {
                        it.copy(phase = AcquisitionPhase.Failed, errorReason = outcome.reason)
                    }
            }
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

    private suspend fun acquireOneShot(
        sessionId: String,
        triggerType: TriggerType,
        context: AcquisitionContext?,
        ownerJob: Job?,
        settings: TrackingSettings,
        sink: StateSink,
        allowLowQualityFallback: Boolean
    ): OneShotOutcome = coroutineScope {
        updateSinkPhase(sink, sessionId, ownerJob, AcquisitionPhase.Acquiring)

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
            priority = Priority.PRIORITY_HIGH_ACCURACY,
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
                            if (qualityAccepted.get()) return@acquire
                            bestSnapshot.set(snapshot)
                            updateSinkBest(sink, sessionId, ownerJob, snapshot, AcquisitionPhase.Evaluating)

                            val fix = snapshot.toRawFix(triggerType, context)
                            val qualityJob = launch {
                                altitudeWaitCoordinator.handleFix(
                                    fix = fix,
                                    deadlineCapMillis = deadlineCapMillis,
                                    deadlineCapElapsedRealtimeMillis = deadlineCapElapsedRealtimeMillis,
                                    onAccepted = { accepted -> onQualityAccepted(accepted) },
                                    onDropped = { droppedFix, reason -> recordDrop(droppedFix, reason) }
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

        if (sink == StateSink.SESSION) {
            if (!isCurrentSession(sessionId)) return@coroutineScope OneShotOutcome.NoFix
            // A cancelled terminal state retains its sessionId; leftover session work
            // must not turn it into TimedOut/Failed or start an enqueue.
            if (_state.value.phase == AcquisitionPhase.Cancelled) return@coroutineScope OneShotOutcome.NoFix
        }

        val accepted = acceptedLocation.get()
        if (accepted != null) return@coroutineScope OneShotOutcome.Accepted(accepted)

        val best = bestSnapshot.get()
        if (best != null && allowLowQualityFallback) {
            return@coroutineScope OneShotOutcome.Accepted(
                QualityAcceptedLocation(
                    fix = best.toRawFix(triggerType, context),
                    altitudeMeters = best.altitudeMeters,
                    acceptedAtMillis = wallClockMillis(),
                    qualityFlags = setOf(LocationQualityGate.LOW_QUALITY_ACCURACY_FLAG)
                )
            )
        }

        val completion = engineResult.get()?.completion
        return@coroutineScope if (completion is LocationEngineCompletion.Failed) {
            OneShotOutcome.Failed(completion.reason)
        } else {
            OneShotOutcome.NoFix
        }
    }

    private fun updateSinkPhase(
        sink: StateSink,
        sessionId: String?,
        ownerJob: Job?,
        phase: AcquisitionPhase
    ) {
        when (sink) {
            StateSink.SESSION -> updateStateIfCurrent(sessionId, ownerJob) { it.copy(phase = phase) }
            StateSink.STREAM -> {} // 流预热保持 StreamState.active，无需 phase
        }
    }

    private fun updateSinkBest(
        sink: StateSink,
        sessionId: String?,
        ownerJob: Job?,
        snapshot: LocationSnapshot,
        phase: AcquisitionPhase
    ) {
        when (sink) {
            StateSink.SESSION -> updateStateIfCurrent(sessionId, ownerJob) {
                it.copy(bestLocation = snapshot, phase = phase)
            }
            StateSink.STREAM -> _streamState.update { it.copy(latestFix = snapshot) }
        }
    }

    private suspend fun handleAccepted(
        accepted: QualityAcceptedLocation,
        sessionId: String,
        triggerType: TriggerType,
        ownerJob: Job
    ) {
        if (!isCurrentSession(sessionId)) return
        try {
            val json = rawJson(accepted, triggerType.storageSource)
            operations.enqueueAccepted(accepted, json, triggerType.storageSource)
            // Record is already enqueued; schedule sync at most once even if session changed.
            operations.scheduleSync()
            updateStateIfCurrent(sessionId, ownerJob) {
                it.copy(
                    phase = AcquisitionPhase.Completed,
                    lastQualityFlags = accepted.qualityFlags,
                    errorReason = null
                )
            }
            onRecorded?.invoke(accepted.fix.toSnapshot())
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            updateStateIfCurrent(sessionId, ownerJob) {
                it.copy(phase = AcquisitionPhase.Failed, errorReason = e.message)
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
        context: AcquisitionContext?
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

    private fun RawLocationFix.toSnapshot(): LocationSnapshot = LocationSnapshot(
        latitude = latitude,
        longitude = longitude,
        horizontalAccuracyMeters = horizontalAccuracyMeters,
        provider = provider,
        source = "acquisition",
        altitudeMeters = altitudeMeters,
        speedMetersPerSecond = speedMetersPerSecond,
        bearingDegrees = bearingDegrees,
        timeMillis = recordedAtMillis
    )

    private companion object {
        const val STREAM_ALTITUDE_MISSING_FLAG = "altitude-missing"
    }
}
