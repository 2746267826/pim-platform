package com.pim.app.location.service

import android.Manifest
import android.app.Notification
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.LocationManager
import android.os.Build
import android.os.IBinder
import androidx.core.content.ContextCompat
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability
import com.google.android.gms.location.Priority
import com.pim.app.location.acquisition.AutomaticSessionContext
import com.pim.app.location.acquisition.LocationAcquisitionCoordinator
import com.pim.app.location.acquisition.LocationAcquisitionState
import com.pim.app.location.acquisition.SessionStartResult
import com.pim.app.location.motion.MotionSignalRepository
import com.pim.app.location.policy.LocationPolicyEngine
import com.pim.app.location.policy.LocationPolicyInput
import com.pim.app.location.policy.LocationPolicyMode
import com.pim.app.location.policy.PolicyDecision
import com.pim.app.location.policy.PolicyLocation
import com.pim.app.mobile.sync.MobileSyncScheduler
import com.pim.app.notifications.LocationNotificationRenderer
import com.pim.app.notifications.LocationNotificationState
import com.pim.app.schedule.ScheduleCacheFreshness
import com.pim.app.schedule.ScheduleCacheSnapshot
import com.pim.app.schedule.ScheduleWindowRepository
import com.pim.app.schedule.ScheduleWindowSelector
import com.pim.app.settings.TrackingSettings
import com.pim.app.settings.TrackingSettingsStore
import com.pim.app.settings.toTrackingPolicy
import com.pim.app.status.QueueStatusRepository
import dagger.hilt.android.AndroidEntryPoint
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

@AndroidEntryPoint
class ForegroundLocationService : Service() {
    @Inject lateinit var trackingSettingsStore: TrackingSettingsStore
    @Inject lateinit var motionSignalRepository: MotionSignalRepository
    @Inject lateinit var scheduleWindowRepository: ScheduleWindowRepository
    @Inject lateinit var mobileSyncScheduler: MobileSyncScheduler
    @Inject lateinit var locationAcquisitionCoordinator: LocationAcquisitionCoordinator
    @Inject lateinit var queueStatusRepository: QueueStatusRepository

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var scheduleRefreshJob: Job? = null
    private var snapshotCollectJob: Job? = null
    private var automaticLoopJob: Job? = null
    private var queueObservationJob: Job? = null
    private var policyEngine: LocationPolicyEngine? = null
    private var scheduleFreshness: ScheduleCacheFreshness = ScheduleCacheFreshness.Missing
    private var scheduleLastSuccessAtMillis: Long? = null
    private var scheduleLastAttemptAtMillis: Long? = null
    private var scheduleLastError: String? = null
    private var currentDecision = PolicyDecision(
        mode = LocationPolicyMode.PowerSavingNormal,
        requestIntervalMillis = 3 * 60 * 1000L,
        nextExpectedLocationAtMillis = System.currentTimeMillis() + 3 * 60 * 1000L,
        reason = "默认省电档",
        scheduleLowFrequency = false
    )
    private val policyTransitionDeduper = PolicyTransitionDeduper()
    private val policyTransitionWriteMutex = Mutex()
    private var lastAcceptedLocationText = "无"
    private var lastAccuracyText = "无"
    private var pendingUploadTotal = 0
    private var apiState = "等待日程数据"
    private var lastDroppedReason: String? = null
    private var isPausing = false
    private var explicitTeardown = false
    // The manual session this instance actually started and therefore owns.
    // Only this session may be cancelled from an unexpected onDestroy(); sessions
    // started by other instances, the UI/controller or a manual-sync teardown are
    // never this instance's to cancel.
    private var ownedManualSessionId: String? = null
    private var policyTransitionWriteJob: Job? = null
    internal var policyTransitionWriter: (suspend (LocationPolicyMode?, PolicyDecision) -> Unit)? = null

    override fun onCreate() {
        super.onCreate()
        publishRuntimeState(isRunning = true)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ForegroundLocationController.ACTION_PAUSE_COLLECTION -> {
                isPausing = true
                explicitTeardown = true
                trackingSettingsStore.setContinuousCollectionEnabled(false)
                applyDecision(
                    currentDecision.copy(
                        mode = LocationPolicyMode.Off,
                        requestIntervalMillis = 0L,
                        nextExpectedLocationAtMillis = Long.MAX_VALUE,
                        reason = "已暂停",
                        scheduleLowFrequency = false
                    ),
                    isRunning = false
                )
                stopCollection()
                val nm = getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
                nm.notify(LocationNotificationRenderer.NOTIFICATION_ID, notification())
                stopSelf(startId)
                return START_NOT_STICKY
            }
            ForegroundLocationController.ACTION_STOP_COLLECTION -> {
                isPausing = false
                explicitTeardown = true
                trackingSettingsStore.setContinuousCollectionEnabled(false)
                stopCollection()
                val nm = getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
                nm.cancel(LocationNotificationRenderer.NOTIFICATION_ID)
                stopSelf(startId)
                return START_NOT_STICKY
            }
            ForegroundLocationController.ACTION_SYNC_NOW -> {
                runManualSync(startId)
            }
            ForegroundLocationController.ACTION_RESUME_COLLECTION -> startCollection(
                enableCollection = true,
                startId = startId,
                persistCollectionIntentBeforePrerequisites = true
            )
            ForegroundLocationController.ACTION_START_COLLECTION -> startCollection(
                enableCollection = true,
                startId = startId
            )
            ForegroundLocationController.ACTION_START_MANUAL_SESSION -> startManualSession(startId)
            ForegroundLocationController.ACTION_CANCEL_LOCATION_SESSION -> {
                val sessionId = intent.getStringExtra(ForegroundLocationController.EXTRA_SESSION_ID)
                if (sessionId == null) {
                    // Fail-closed: a cancel without a session id must never be
                    // forwarded as the coordinator's wildcard cancellation of the
                    // current session. Leave the active session, the 7101
                    // foreground notification and the service untouched; the
                    // owning terminal waiter (if any) retires this instance's
                    // foreground when its session ends.
                    return START_STICKY
                }
                val cancelled = locationAcquisitionCoordinator.cancelCurrentSession(sessionId)
                if (!cancelled) {
                    // Fail-closed: a missing/stale/wrong session id (or a
                    // non-cancellable phase such as Enqueuing) must not stop a
                    // valid current session, remove the 7101 foreground
                    // notification, or induce onDestroy() to cancel the valid
                    // session. Leave everything untouched; the owning terminal
                    // waiter (if any) retires this instance's foreground when
                    // its session ends.
                    return START_STICKY
                }
                // manual-only 实例（无自动循环且连续采集未启用）可能没有终结
                // waiter（例如新实例处理上一个 waiter 已停止的旧实例留下的
                // cancel）；此时取消必须移除非前台通知并无条件停止服务、返回
                // 非 sticky，否则该实例成为僵尸且可能被 null sticky intent
                // 重建。自动采集启用/运行中时不得停止其服务或 7101 通知。
                if (automaticLoopJob?.isActive != true &&
                    !trackingSettingsStore.read().continuousCollectionEnabled
                ) {
                    stopForeground(STOP_FOREGROUND_REMOVE)
                    stopSelf()
                    return START_NOT_STICKY
                }
            }
            null -> startCollection(enableCollection = false, startId = startId)
        }
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        scope.cancel()
        // Foreground-notification cleanup is an ownership decision: only an
        // instance that engaged its own foreground lifecycle (an active
        // automatic loop, an owned manual session, or an explicit teardown)
        // may remove the shared 7101 notification. Sync-only and Busy-adopted
        // instances never own another session's 7101 and must not cancel an
        // automatic session they did not start.
        val ownsForegroundLifecycle = ownedManualSessionId != null ||
            automaticLoopJob?.isActive == true ||
            explicitTeardown
        if (!isPausing && ownsForegroundLifecycle) {
            stopCollection()
            val nm = getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
            nm.cancel(LocationNotificationRenderer.NOTIFICATION_ID)
        }
        if (!explicitTeardown) {
            // Unexpected service destruction: cancel only the manual session this
            // instance actually started AND that is still in a cancellable
            // capture phase. An owned AwaitingManualSubmit result must be
            // preserved — the user may still submit or cancel it via the UI, and
            // the terminal waiter clears ownership once it resumes. Sessions
            // started by another service instance, the UI/controller or an
            // unrelated ACTION_SYNC_NOW teardown are never this instance's to
            // cancel.
            val ownedId = ownedManualSessionId
            if (ownedId != null) {
                val current = locationAcquisitionCoordinator.state.value
                if (current.sessionId == ownedId &&
                    current.phase in setOf(
                        com.pim.app.location.acquisition.AcquisitionPhase.Preparing,
                        com.pim.app.location.acquisition.AcquisitionPhase.Acquiring,
                        com.pim.app.location.acquisition.AcquisitionPhase.Evaluating
                    )
                ) {
                    locationAcquisitionCoordinator.cancelCurrentSession(ownedId)
                }
            }
        }
        publishRuntimeState(isRunning = false)
        super.onDestroy()
    }

    private fun startCollection(
        enableCollection: Boolean,
        startId: Int,
        persistCollectionIntentBeforePrerequisites: Boolean = false
    ) {
        isPausing = false
        explicitTeardown = false
        if (enableCollection && persistCollectionIntentBeforePrerequisites) {
            trackingSettingsStore.setContinuousCollectionEnabled(true)
        }
        if (!hasRequiredLocationPermissions()) {
            lastDroppedReason = "缺少精确或后台定位权限"
            applyDecision(
                currentDecision.copy(
                    mode = LocationPolicyMode.Off,
                    requestIntervalMillis = 0L,
                    nextExpectedLocationAtMillis = Long.MAX_VALUE,
                    reason = "缺少精确或后台定位权限",
                    scheduleLowFrequency = false
                ),
                isRunning = false
            )
            stopCollection()
            stopSelf(startId)
            return
        }

        if (GoogleApiAvailability.getInstance()
                .isGooglePlayServicesAvailable(this) != ConnectionResult.SUCCESS
        ) {
            lastDroppedReason = "Google Play Services 不可用"
            applyDecision(
                currentDecision.copy(
                    mode = LocationPolicyMode.Off,
                    requestIntervalMillis = 0L,
                    nextExpectedLocationAtMillis = Long.MAX_VALUE,
                    reason = "Google Play Services 不可用",
                    scheduleLowFrequency = false
                ),
                isRunning = false
            )
            stopCollection()
            stopSelf(startId)
            return
        }

        if (!isLocationEnabled()) {
            lastDroppedReason = "系统定位服务未开启"
            applyDecision(
                currentDecision.copy(
                    mode = LocationPolicyMode.Off,
                    requestIntervalMillis = 0L,
                    nextExpectedLocationAtMillis = Long.MAX_VALUE,
                    reason = "系统定位服务未开启",
                    scheduleLowFrequency = false
                ),
                isRunning = false
            )
            stopCollection()
            stopSelf(startId)
            return
        }

        if (enableCollection) {
            trackingSettingsStore.setContinuousCollectionEnabled(true)
        }

        val settings = trackingSettingsStore.read()
        initializeAutomaticRuntime(settings)
        startForeground(LocationNotificationRenderer.NOTIFICATION_ID, notification())

        if (!settings.continuousCollectionEnabled) {
            lastDroppedReason = "连续采集未开启"
            stopCollection()
            stopSelf(startId)
            return
        }

        startAutomaticLoop()
    }

    private fun stopCollection() {
        automaticLoopJob?.cancel()
        automaticLoopJob = null
        queueObservationJob?.cancel()
        queueObservationJob = null
        scheduleRefreshJob?.cancel()
        snapshotCollectJob?.cancel()
        runCatching { motionSignalRepository.unregisterActivityTransitions() }
        cancelActiveAutomaticSession()
        stopForeground(STOP_FOREGROUND_REMOVE)
    }

    private fun cancelActiveAutomaticSession() {
        val current = locationAcquisitionCoordinator.state.value
        if (current.triggerType != com.pim.app.location.acquisition.TriggerType.AUTOMATIC) return
        // Enqueuing is intentionally excluded: the coordinator ignores
        // cancellation during Enqueuing, so an already-confirmed submission
        // must not be cancelled or rolled back by the service.
        if (current.phase !in setOf(
                com.pim.app.location.acquisition.AcquisitionPhase.Preparing,
                com.pim.app.location.acquisition.AcquisitionPhase.Acquiring,
                com.pim.app.location.acquisition.AcquisitionPhase.Evaluating
            )
        ) {
            return
        }
        locationAcquisitionCoordinator.cancelCurrentSession(current.sessionId)
    }

    private fun startManualSession(startId: Int) {
        // A PAUSE/STOP on the same instance must not leak its teardown flags into
        // a fresh manual session: later unexpected destruction then cancels the
        // new session and removes the notification instead of preserving it.
        isPausing = false
        explicitTeardown = false
        val result = locationAcquisitionCoordinator.startManualSession(replaceAwaitingManual = true)
        if (result is SessionStartResult.Rejected) {
            stopSelf(startId)
            return
        }
        if (result is SessionStartResult.Started) {
            ownedManualSessionId = result.sessionId
        }

        startForeground(LocationNotificationRenderer.NOTIFICATION_ID, notification())
        val settings = trackingSettingsStore.read()
        var automaticRuntimeReady = automaticLoopJob?.isActive == true
        if (settings.continuousCollectionEnabled) {
            if (
                hasRequiredLocationPermissions() &&
                !automaticRuntimeReady
            ) {
                initializeAutomaticRuntime(settings)
                startAutomaticLoop()
                automaticRuntimeReady = true
            }
            if (automaticRuntimeReady &&
                (result is SessionStartResult.Started || result is SessionStartResult.Busy)
            ) {
                return
            }
        }

        when (result) {
            is SessionStartResult.Started -> {
                val startedId = result.sessionId
                scope.launch {
                    // 观察本会话的终结状态；若该会话在 waiter 挂起期间被新的
                    // 手动会话替换（replaceAwaitingManual 或旧终态后的再次启动，
                    // 会话 ID 变化且不再回到本会话），waiter 不得永远等待，也
                    // 不得误把替换会话的终态当成自己的终态。
                    locationAcquisitionCoordinator.state.first { acqState ->
                        acqState.sessionId != startedId ||
                            acqState.phase in setOf(
                                com.pim.app.location.acquisition.AcquisitionPhase.AwaitingManualSubmit,
                                com.pim.app.location.acquisition.AcquisitionPhase.Completed,
                                com.pim.app.location.acquisition.AcquisitionPhase.TimedOut,
                                com.pim.app.location.acquisition.AcquisitionPhase.Failed,
                                com.pim.app.location.acquisition.AcquisitionPhase.Cancelled
                            )
                    }
                    // 终结状态被观察到后、前台/服务拆除前，重新校验当前会话
                    // 仍是本 waiter 捕获的 startedId；若已被新的手动会话替换，
                    // 只退役本实例的本地服务，不得拆除新会话的前台通知或取消
                    // 该会话（同时清除所有权，防止随后的 onDestroy 误删其 7101）。
                    // 但仅当本实例仍拥有被替换的 startedId（即替换来自外部实例
                    // 或 UI/控制器）时才可自我退役；若同一实例已通过新的
                    // ACTION_START_MANUAL_SESSION 把 owner 设为替换会话，旧
                    // waiter 不得清除新 owner，也不得停止服务或触碰前台。
                    if (locationAcquisitionCoordinator.state.value.sessionId != startedId) {
                        if (ownedManualSessionId == startedId) {
                            ownedManualSessionId = null
                            stopSelf()
                        }
                        return@launch
                    }
                    // 本实例拥有的会话已由其终结 waiter 接管收尾：清除所有权，
                    // 使随后的 onDestroy() 不再尝试取消该会话（例如
                    // AwaitingManualSubmit 仍可被协调器取消，但已由 waiter 按
                    // 用户流程交给提交/取消路径处理，不得被服务拆除误取消）。
                    ownedManualSessionId = null
                    if (!trackingSettingsStore.read().continuousCollectionEnabled) {
                        stopForeground(STOP_FOREGROUND_REMOVE)
                        // The captured startId may be older than a later
                        // ACTION_CANCEL_LOCATION_SESSION startId, which Android
                        // ignores for stopSelf(startId); stop unconditionally so
                        // the manual-only service really terminates.
                        stopSelf()
                    }
                }
            }
            is SessionStartResult.Busy -> {
                // 幂等处理：首个手动会话仍在 Preparing/Acquiring 时收到重复
                // 启动，必须保留首个会话及其前台通知，由其终结 waiter 负责
                // 拆除；此处拆除会触发 onDestroy 取消首个会话。
                // 全新手动专用实例收到 Busy 时采纳现有会话的生命周期：挂接该
                // 会话的终结 waiter，在会话结束时拆除自身前台并停止，且拆除
                // 前重新校验会话 ID，防止误停替换会话；实例不得长期滞留前台。
                val existingSessionId = locationAcquisitionCoordinator.state.value.sessionId
                if (existingSessionId == null) {
                    stopSelf(startId)
                    return
                }
                scope.launch {
                    // 观察被采纳会话的终结状态；若该会话被新的手动会话替换
                    // （会话 ID 变化且不再回到终结状态），waiter 不得永远等待。
                    locationAcquisitionCoordinator.state.first { acqState ->
                        acqState.sessionId != existingSessionId ||
                            acqState.phase in setOf(
                                com.pim.app.location.acquisition.AcquisitionPhase.AwaitingManualSubmit,
                                com.pim.app.location.acquisition.AcquisitionPhase.Completed,
                                com.pim.app.location.acquisition.AcquisitionPhase.TimedOut,
                                com.pim.app.location.acquisition.AcquisitionPhase.Failed,
                                com.pim.app.location.acquisition.AcquisitionPhase.Cancelled
                            )
                    }
                    // 终结状态被观察到后、前台/服务拆除前，重新校验当前会话
                    // 仍是本 waiter 捕获的 existingSessionId；若已被新的手动
                    // 会话替换，只退役本实例的本地服务，不得执行
                    // stopForeground(REMOVE)，也不得触发随后 onDestroy 对替换
                    // 会话 7101 通知的清理。
                    val currentSessionId = locationAcquisitionCoordinator.state.value.sessionId
                    if (currentSessionId != existingSessionId) {
                        // 本实例可能通过同实例 replacement 已拥有替换会话（owner
                        // 非 null 且不等于 existingSessionId）；此时不得清除新
                        // owner、不得停止服务，也不得触碰前台，否则 onDestroy
                        // 会取消替换会话。若本实例没有 owner（外部 Busy-adoption）
                        // 或仍拥有 existingSessionId，则保持旧实例退役行为。
                        val ownedId = ownedManualSessionId
                        if (ownedId != null && ownedId != existingSessionId) {
                            return@launch
                        }
                        if (ownedId == existingSessionId) {
                            ownedManualSessionId = null
                        }
                        stopSelf()
                        return@launch
                    }
                    if (!trackingSettingsStore.read().continuousCollectionEnabled) {
                        stopForeground(STOP_FOREGROUND_REMOVE)
                        stopSelf()
                    }
                }
                return
            }
            is SessionStartResult.Rejected -> {
                // handled above; unreachable here
            }
        }
    }

    private fun initializeAutomaticRuntime(settings: TrackingSettings) {
        policyEngine = LocationPolicyEngine(settings.toTrackingPolicy())
        applyDecision(
            policyEngine!!.reduce(
                LocationPolicyInput(
                    nowMillis = System.currentTimeMillis(),
                    collectionEnabled = settings.continuousCollectionEnabled
                )
            )
        )
        refreshScheduleWindows()
        motionSignalRepository.registerActivityTransitions()
        observeQueueStatus()
    }

    private fun startAutomaticLoop() {
        automaticLoopJob?.cancel()
        automaticLoopJob = scope.launch {
            while (trackingSettingsStore.read().continuousCollectionEnabled) {
                refreshScheduleWindows()
                motionSignalRepository.registerActivityTransitions()
                applyScheduleSnapshot(scheduleWindowRepository.snapshotForCurrentServer())
                val decision = recomputePolicyDecision()
                applyDecision(decision)
                updateNotification()

                val result = locationAcquisitionCoordinator.startAutomaticSession(
                    AutomaticSessionContext(
                        priority = resolveLocationPriority(decision.mode),
                        policyMode = decision.mode.name,
                        scheduleLowFrequency = decision.scheduleLowFrequency,
                        motionSignal = motionSignalRepository.status.value.signal.name
                    )
                )
                when (result) {
                    is SessionStartResult.Busy -> {
                        locationAcquisitionCoordinator.state.first { !it.isBusy }
                        continue
                    }
                    is SessionStartResult.Rejected -> {
                        lastDroppedReason = result.reason
                        updateNotification()
                        delay(decision.requestIntervalMillis.coerceAtLeast(1_000L))
                        continue
                    }
                    is SessionStartResult.Started -> {
                        val startedId = result.sessionId
                        val finalState = locationAcquisitionCoordinator.state.first { state ->
                            // 等待本会话的终态；若会话已被替换（sessionId 变化），
                            // 观察必须放行，不能永久等待已被覆盖的旧终态。
                            (state.sessionId == startedId && state.phase in setOf(
                                com.pim.app.location.acquisition.AcquisitionPhase.Completed,
                                com.pim.app.location.acquisition.AcquisitionPhase.TimedOut,
                                com.pim.app.location.acquisition.AcquisitionPhase.Failed,
                                com.pim.app.location.acquisition.AcquisitionPhase.Cancelled
                            )) || state.sessionId != startedId
                        }
                        if (finalState.sessionId != startedId) {
                            // 本会话已被手动替换：跳过旧会话的
                            // accepted-location/policy/delay 处理，继续下一轮，
                            // 由 Busy 分支协调替换会话的生命周期。
                            continue
                        }
                        if (finalState.phase == com.pim.app.location.acquisition.AcquisitionPhase.Completed) {
                            finalState.bestLocation?.let { snapshot ->
                                policyEngine?.onAcceptedLocation(
                                    PolicyLocation(
                                        latitude = snapshot.latitude,
                                        longitude = snapshot.longitude,
                                        recordedAtMillis = snapshot.timeMillis
                                    )
                                )
                                lastAcceptedLocationText = timeFormatter.format(
                                    Instant.ofEpochMilli(snapshot.timeMillis)
                                        .atZone(ZoneId.systemDefault())
                                )
                                lastAccuracyText = "${snapshot.horizontalAccuracyMeters?.toInt() ?: 0}m"
                                lastDroppedReason = null
                            }
                        }
                        val nextDecision = recomputePolicyDecision()
                        applyDecision(nextDecision)
                        updateNotification()
                        delay(nextDecision.requestIntervalMillis.coerceAtLeast(1_000L))
                    }
                }
            }
            // The loop exits because collection became disabled; this is an
            // explicit shutdown, so the imminent onDestroy must not cancel an
            // active manual session or keep a foreground notification.
            explicitTeardown = true
            isPausing = false
            stopCollection()
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
        }
    }

    private fun recomputePolicyDecision(): PolicyDecision {
        val now = System.currentTimeMillis()
        val settings = trackingSettingsStore.read()
        return policyEngine?.reduce(
            LocationPolicyInput(
                nowMillis = now,
                collectionEnabled = settings.continuousCollectionEnabled,
                currentScheduleWindow = ScheduleWindowSelector.current(
                    scheduleWindowRepository.snapshotForCurrentServer().windows,
                    now
                ),
                motionSignal = motionSignalRepository.status.value.signal
            )
        ) ?: currentDecision
    }

    private fun observeQueueStatus() {
        queueObservationJob?.cancel()
        queueObservationJob = scope.launch {
            queueStatusRepository.observe().collect { snapshot ->
                pendingUploadTotal = snapshot.pendingUploadTotal
                publishRuntimeState()
                updateNotification()
            }
        }
    }

    private fun runManualSync(_startId: Int) {
        // A manual sync shares the foreground lifecycle: clear stale teardown
        // flags so a later unexpected destruction is not treated as explicit.
        isPausing = false
        explicitTeardown = false
        val hasLoop = automaticLoopJob?.isActive == true
        // A same-instance sync must not retire this manual-only instance while
        // it owns a currently active manual session (Preparing/Acquiring/
        // Evaluating): tearing down here stops the instance and onDestroy would
        // then cancel the very session this instance is running. The owning
        // terminal waiter retires the foreground/service when that session ends.
        val coordinatorState = locationAcquisitionCoordinator.state.value
        val ownsActiveManualSession = ownedManualSessionId != null &&
            coordinatorState.sessionId == ownedManualSessionId &&
            coordinatorState.phase in setOf(
                com.pim.app.location.acquisition.AcquisitionPhase.Preparing,
                com.pim.app.location.acquisition.AcquisitionPhase.Acquiring,
                com.pim.app.location.acquisition.AcquisitionPhase.Evaluating
            )
        val stopAfterSync = !hasLoop &&
            !trackingSettingsStore.read().continuousCollectionEnabled &&
            !ownsActiveManualSession
        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
        val restorePausedNotification = stopAfterSync && nm.activeNotifications.any {
            it.id == LocationNotificationRenderer.NOTIFICATION_ID &&
                (it.notification.flags and Notification.FLAG_ONGOING_EVENT) == 0
        }
        if (restorePausedNotification) {
            markPausedState()
        }
        apiState = "同步中"
        startForeground(LocationNotificationRenderer.NOTIFICATION_ID, notification())
        updateNotification()

        scope.launch {
            try {
                mobileSyncScheduler.enqueueNow()
                apiState = "同步请求已提交。"
                updateNotification()
            } catch (ex: CancellationException) {
                throw ex
            } catch (_: Exception) {
                apiState = "同步请求提交失败"
                updateNotification()
            } finally {
                if (stopAfterSync) {
                    stopForeground(STOP_FOREGROUND_REMOVE)
                    if (restorePausedNotification) {
                        markPausedState()
                        nm.notify(LocationNotificationRenderer.NOTIFICATION_ID, notification())
                    }
                    // 使用无条件 stopSelf()：若期间收到更新的 cancel/sync
                    // intent，startId 不再匹配时 stopSelf(startId) 会被
                    // Android 忽略，导致手动专用服务成为僵尸。
                    stopSelf()
                }
            }
        }
    }

    private fun markPausedState() {
        applyDecision(
            currentDecision.copy(
                mode = LocationPolicyMode.Off,
                requestIntervalMillis = 0L,
                nextExpectedLocationAtMillis = Long.MAX_VALUE,
                reason = "已暂停",
                scheduleLowFrequency = false
            ),
            isRunning = false
        )
        isPausing = true
    }

    private fun ensureSnapshotObserver() {
        if (snapshotCollectJob?.isActive == true) return
        if (!::scheduleWindowRepository.isInitialized) return
        snapshotCollectJob = scope.launch {
            scheduleWindowRepository.snapshot.collect { snapshot ->
                applyScheduleSnapshot(snapshot)
                publishRuntimeState()
            }
        }
    }

    private fun refreshScheduleWindows(force: Boolean = false) {
        ensureSnapshotObserver()
        if (scheduleRefreshJob?.isActive == true) return
        scheduleRefreshJob = scope.launch {
            try {
                applyScheduleSnapshot(scheduleWindowRepository.snapshotForCurrentServer())
                val snapshot = withContext(Dispatchers.IO) {
                    scheduleWindowRepository.refreshIfStale(force = force)
                }
                applyScheduleSnapshot(snapshot)
                updateNotification()
            } catch (ex: CancellationException) {
                throw ex
            } catch (_: Exception) {
                applyScheduleSnapshot(scheduleWindowRepository.snapshotForCurrentServer())
                updateNotification()
            }
        }
    }

    private fun applyScheduleSnapshot(snapshot: ScheduleCacheSnapshot) {
        scheduleFreshness = snapshot.freshness
        scheduleLastSuccessAtMillis = snapshot.lastSuccessAtMillis
        scheduleLastAttemptAtMillis = snapshot.lastAttemptAtMillis
        scheduleLastError = snapshot.lastError
        apiState = scheduleApiStateText(snapshot)
    }

    private fun scheduleApiStateText(snapshot: ScheduleCacheSnapshot): String {
        return when {
            snapshot.freshness == ScheduleCacheFreshness.Fresh && snapshot.lastError != null -> "日程缓存异常"
            snapshot.freshness == ScheduleCacheFreshness.Fresh && snapshot.lastError == null -> "正常"
            snapshot.freshness == ScheduleCacheFreshness.Stale -> "日程缓存可能过期"
            snapshot.freshness == ScheduleCacheFreshness.Missing && snapshot.lastError != null -> "日程暂不可用"
            snapshot.freshness == ScheduleCacheFreshness.Missing &&
                snapshot.lastError == null &&
                snapshot.lastSuccessAtMillis == null -> "等待日程数据"
            else -> "正常"
        }
    }

    private fun applyDecision(decision: PolicyDecision, isRunning: Boolean = isRunning()) {
        currentDecision = decision
        val transition = policyTransitionDeduper.note(decision)
        if (transition != null) {
            policyTransitionWriteJob = scope.launch {
                policyTransitionWriteMutex.withLock {
                    try {
                        val writer = policyTransitionWriter
                        if (writer != null) {
                            writer(transition.fromMode, transition.decision)
                        }
                    } catch (ex: CancellationException) {
                        throw ex
                    } catch (_: Exception) {
                    }
                }
            }
        }
        publishRuntimeState(isRunning = isRunning)
    }

    private fun isLocationEnabled(): Boolean {
        val lm = getSystemService(Context.LOCATION_SERVICE) as LocationManager
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            lm.isLocationEnabled
        } else {
            @Suppress("DEPRECATION")
            lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER) ||
                lm.isProviderEnabled(LocationManager.GPS_PROVIDER)
        }
    }

    private fun notification() = LocationNotificationRenderer.build(
        this,
        LocationNotificationState(
            mode = currentDecision.mode,
            nextExpectedLocationText = nextExpectedLocationText(currentDecision),
            lastAcceptedLocationText = lastAcceptedLocationText,
            lastAccuracyText = lastAccuracyText,
            pendingUploadTotal = pendingUploadTotal,
            apiState = apiState,
            lastDroppedReason = lastDroppedReason
        )
    )

    private fun updateNotification() {
        publishRuntimeState()
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
        notificationManager.notify(LocationNotificationRenderer.NOTIFICATION_ID, notification())
    }

    private fun publishRuntimeState(isRunning: Boolean = isRunning()) {
        _runtimeState.value = ForegroundLocationRuntimeState(
            isRunning = isRunning,
            currentPolicyMode = currentDecision.mode.name,
            currentPolicyReason = currentDecision.reason,
            requestIntervalMillis = currentDecision.requestIntervalMillis.takeUnless { it <= 0L },
            nextExpectedLocationAtMillis = currentDecision.nextExpectedLocationAtMillis
                .takeUnless { it == Long.MAX_VALUE },
            lastAcceptedLocationText = lastAcceptedLocationText,
            lastAccuracyText = lastAccuracyText,
            pendingUploadTotal = pendingUploadTotal,
            apiState = apiState,
            lastDroppedReason = lastDroppedReason,
            scheduleFreshness = scheduleFreshness,
            scheduleLastSuccessAtMillis = scheduleLastSuccessAtMillis,
            scheduleLastAttemptAtMillis = scheduleLastAttemptAtMillis,
            scheduleLastError = scheduleLastError
        )
    }

    private fun hasRequiredLocationPermissions(): Boolean {
        val fine = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
        val background = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_BACKGROUND_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
        return fine == PackageManager.PERMISSION_GRANTED && background
    }

    private fun nextExpectedLocationText(decision: PolicyDecision): String {
        if (decision.nextExpectedLocationAtMillis == Long.MAX_VALUE) return "暂停"
        val remainingMillis = (decision.nextExpectedLocationAtMillis - System.currentTimeMillis()).coerceAtLeast(0L)
        val minutes = (remainingMillis + 59_999L) / 60_000L
        return if (minutes <= 0L) "即将定位" else "$minutes 分钟后"
    }

    companion object {
        private val _runtimeState = MutableStateFlow(ForegroundLocationRuntimeState())
        val runtimeState: StateFlow<ForegroundLocationRuntimeState> = _runtimeState.asStateFlow()

        fun isRunning(): Boolean = runtimeState.value.isRunning

        val timeFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")

        fun resolveRequestInterval(intervalMillis: Long): Long {
            require(intervalMillis > 0L) { "intervalMillis must be positive" }
            return intervalMillis
        }

        fun resolveLocationPriority(mode: LocationPolicyMode): Int =
            Priority.PRIORITY_HIGH_ACCURACY
    }
}
