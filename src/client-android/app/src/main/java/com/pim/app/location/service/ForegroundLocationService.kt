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
                locationAcquisitionCoordinator.cancelCurrentSession(sessionId)
            }
            null -> startCollection(enableCollection = false, startId = startId)
        }
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        scope.cancel()
        if (!isPausing) {
            stopCollection()
            val nm = getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
            nm.cancel(LocationNotificationRenderer.NOTIFICATION_ID)
        }
        val phase = locationAcquisitionCoordinator.state.value.phase
        if (phase in setOf(
                com.pim.app.location.acquisition.AcquisitionPhase.Preparing,
                com.pim.app.location.acquisition.AcquisitionPhase.Acquiring,
                com.pim.app.location.acquisition.AcquisitionPhase.Evaluating
            )
        ) {
            locationAcquisitionCoordinator.cancelCurrentSession()
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
        if (current.phase !in setOf(
                com.pim.app.location.acquisition.AcquisitionPhase.Preparing,
                com.pim.app.location.acquisition.AcquisitionPhase.Acquiring,
                com.pim.app.location.acquisition.AcquisitionPhase.Evaluating,
                com.pim.app.location.acquisition.AcquisitionPhase.Enqueuing
            )
        ) {
            return
        }
        locationAcquisitionCoordinator.cancelCurrentSession(current.sessionId)
    }

    private fun startManualSession(startId: Int) {
        val result = locationAcquisitionCoordinator.startManualSession(replaceAwaitingManual = true)
        if (result is SessionStartResult.Rejected) {
            stopSelf(startId)
            return
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
                    locationAcquisitionCoordinator.state.first { acqState ->
                        acqState.sessionId == startedId &&
                            acqState.phase in setOf(
                                com.pim.app.location.acquisition.AcquisitionPhase.AwaitingManualSubmit,
                                com.pim.app.location.acquisition.AcquisitionPhase.Completed,
                                com.pim.app.location.acquisition.AcquisitionPhase.TimedOut,
                                com.pim.app.location.acquisition.AcquisitionPhase.Failed,
                                com.pim.app.location.acquisition.AcquisitionPhase.Cancelled
                            )
                    }
                    if (!trackingSettingsStore.read().continuousCollectionEnabled) {
                        stopForeground(STOP_FOREGROUND_REMOVE)
                        stopSelf(startId)
                    }
                }
            }
            is SessionStartResult.Busy -> {
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf(startId)
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
                            state.sessionId == startedId && state.phase in setOf(
                                com.pim.app.location.acquisition.AcquisitionPhase.Completed,
                                com.pim.app.location.acquisition.AcquisitionPhase.TimedOut,
                                com.pim.app.location.acquisition.AcquisitionPhase.Failed,
                                com.pim.app.location.acquisition.AcquisitionPhase.Cancelled
                            )
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

    private fun runManualSync(startId: Int) {
        val hasLoop = automaticLoopJob?.isActive == true
        val stopAfterSync = !hasLoop && !trackingSettingsStore.read().continuousCollectionEnabled
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
                    stopSelf(startId)
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

        fun resolveLocationPriority(mode: LocationPolicyMode): Int = when (mode) {
            LocationPolicyMode.PowerSavingNormal,
            LocationPolicyMode.ScheduleLowFrequency,
            LocationPolicyMode.Off,
            LocationPolicyMode.SyncFallback -> Priority.PRIORITY_BALANCED_POWER_ACCURACY
            LocationPolicyMode.MotionObservation,
            LocationPolicyMode.MovementRecovery -> Priority.PRIORITY_HIGH_ACCURACY
        }
    }
}
