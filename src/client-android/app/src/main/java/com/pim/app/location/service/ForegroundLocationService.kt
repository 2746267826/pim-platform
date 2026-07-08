package com.pim.app.location.service

import android.Manifest
import android.annotation.SuppressLint
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.os.IBinder
import android.os.Looper
import androidx.core.content.ContextCompat
import com.pim.app.location.LocationQueueRepository
import com.pim.app.location.motion.MotionSignalRepository
import com.pim.app.location.policy.LocationPolicyEngine
import com.pim.app.location.policy.LocationPolicyInput
import com.pim.app.location.policy.LocationPolicyMode
import com.pim.app.location.policy.PolicyDecision
import com.pim.app.location.policy.PolicyLocation
import com.pim.app.location.policy.ScheduleWindow
import com.pim.app.location.quality.AltitudeWaitCoordinator
import com.pim.app.location.quality.QualityAcceptedLocation
import com.pim.app.location.quality.RawLocationFix
import com.pim.app.mobile.sync.MobileSyncCoordinator
import com.pim.app.notifications.LocationNotificationRenderer
import com.pim.app.notifications.LocationNotificationState
import com.pim.app.schedule.ScheduleWindowRepository
import com.pim.app.schedule.ScheduleWindowSelector
import com.pim.app.settings.TrackingSettingsStore
import com.pim.app.settings.toTrackingPolicy
import dagger.hilt.android.AndroidEntryPoint
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

@AndroidEntryPoint
class ForegroundLocationService : Service() {
    @Inject lateinit var trackingSettingsStore: TrackingSettingsStore
    @Inject lateinit var locationQueueRepository: LocationQueueRepository
    @Inject lateinit var motionSignalRepository: MotionSignalRepository
    @Inject lateinit var scheduleWindowRepository: ScheduleWindowRepository
    @Inject lateinit var mobileSyncCoordinator: MobileSyncCoordinator

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val qualityCoordinator = AltitudeWaitCoordinator()
    private lateinit var manager: LocationManager
    private var listener: LocationListener? = null
    private var registeredIntervalMillis: Long? = null
    private var policyEngine: LocationPolicyEngine? = null
    private var scheduleWindows: List<ScheduleWindow> = emptyList()
    private var currentDecision = PolicyDecision(
        mode = LocationPolicyMode.PowerSavingNormal,
        requestIntervalMillis = 3 * 60 * 1000L,
        nextExpectedLocationAtMillis = System.currentTimeMillis() + 3 * 60 * 1000L,
        reason = "默认省电档",
        scheduleLowFrequency = false
    )
    private var lastAcceptedLocationText = "无"
    private var lastAccuracyText = "无"
    private var pendingUploadCount = 0
    private var apiState = "正常"
    private var lastDroppedReason: String? = null

    override fun onCreate() {
        super.onCreate()
        manager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
        publishRuntimeState(isRunning = true)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ForegroundLocationController.ACTION_PAUSE_COLLECTION -> {
                trackingSettingsStore.setContinuousCollectionEnabled(false)
                stopCollection()
                stopSelf()
                return START_NOT_STICKY
            }
            ForegroundLocationController.ACTION_SYNC_NOW -> {
                runManualSync()
            }
            ForegroundLocationController.ACTION_RESUME_COLLECTION,
            ForegroundLocationController.ACTION_START_COLLECTION -> startCollection(enableCollection = true)
            null -> startCollection(enableCollection = false)
        }
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        stopCollection()
        scope.cancel()
        _runtimeState.value = ForegroundLocationRuntimeState(isRunning = false)
        super.onDestroy()
    }

    private fun startCollection(enableCollection: Boolean) {
        val settings = if (enableCollection) {
            trackingSettingsStore.setContinuousCollectionEnabled(true)
        } else {
            trackingSettingsStore.read()
        }
        policyEngine = LocationPolicyEngine(settings.toTrackingPolicy())
        currentDecision = policyEngine!!.reduce(
            LocationPolicyInput(
                nowMillis = System.currentTimeMillis(),
                collectionEnabled = settings.continuousCollectionEnabled
            )
        )
        publishRuntimeState()
        startForeground(LocationNotificationRenderer.NOTIFICATION_ID, notification())

        if (!settings.continuousCollectionEnabled) {
            lastDroppedReason = "连续采集未开启"
            updateNotification()
            stopCollection()
            stopSelf()
            return
        }
        if (!hasAnyLocationPermission()) {
            lastDroppedReason = "缺少定位权限"
            updateNotification()
            return
        }

        refreshScheduleWindows()
        requestLocationUpdates(currentDecision.requestIntervalMillis)
    }

    private fun stopCollection() {
        listener?.let { manager.removeUpdates(it) }
        listener = null
        registeredIntervalMillis = null
        stopForeground(STOP_FOREGROUND_REMOVE)
    }

    private fun runManualSync() {
        val stopAfterSync = listener == null && !trackingSettingsStore.read().continuousCollectionEnabled
        apiState = "同步中"
        startForeground(LocationNotificationRenderer.NOTIFICATION_ID, notification())
        updateNotification()

        scope.launch {
            try {
                val state = withContext(Dispatchers.IO) {
                    mobileSyncCoordinator.syncOnOpen()
                }
                pendingUploadCount = state.pendingQueueCount
                apiState = if (state.lastError == null) "同步完成" else "同步失败"
                updateNotification()
            } catch (ex: CancellationException) {
                throw ex
            } catch (_: Exception) {
                apiState = "同步失败"
                updateNotification()
            } finally {
                if (stopAfterSync) {
                    stopForeground(STOP_FOREGROUND_REMOVE)
                    stopSelf()
                }
            }
        }
    }

    private fun refreshScheduleWindows() {
        val now = System.currentTimeMillis()
        scope.launch {
            runCatching {
                withContext(Dispatchers.IO) {
                    scheduleWindowRepository.loadWindows(
                        startMillis = now - 6L * 60L * 60L * 1000L,
                        endMillis = now + 24L * 60L * 60L * 1000L
                    )
                }
            }.fold(
                onSuccess = {
                    scheduleWindows = it
                    apiState = "正常"
                    updateNotification()
                },
                onFailure = {
                    apiState = "API 无法连接"
                    updateNotification()
                }
            )
        }
    }

    @SuppressLint("MissingPermission")
    private fun requestLocationUpdates(intervalMillis: Long) {
        if (registeredIntervalMillis == intervalMillis && listener != null) return
        listener?.let { manager.removeUpdates(it) }
        registeredIntervalMillis = intervalMillis
        val updateListener = object : LocationListener {
            override fun onLocationChanged(location: Location) {
                handleLocation(location)
            }

            override fun onProviderDisabled(provider: String) {
                lastDroppedReason = "$provider 已关闭"
                updateNotification()
            }

            override fun onProviderEnabled(provider: String) = Unit
            override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) = Unit
        }
        listener = updateListener
        enabledProviders().forEach { provider ->
            manager.requestLocationUpdates(
                provider,
                intervalMillis.coerceAtLeast(60_000L),
                0f,
                updateListener,
                Looper.getMainLooper()
            )
        }
    }

    private fun handleLocation(location: Location) {
        val settings = trackingSettingsStore.read()
        if (!settings.continuousCollectionEnabled) {
            currentDecision = policyEngine?.reduce(
                LocationPolicyInput(nowMillis = System.currentTimeMillis(), collectionEnabled = false)
            ) ?: currentDecision.copy(
                mode = LocationPolicyMode.Off,
                requestIntervalMillis = 0L,
                nextExpectedLocationAtMillis = Long.MAX_VALUE,
                reason = "连续采集未开启",
                scheduleLowFrequency = false
            )
            updateNotification()
            stopCollection()
            stopSelf()
            return
        }

        val now = System.currentTimeMillis()
        val decision = policyEngine?.reduce(
            LocationPolicyInput(
                nowMillis = now,
                collectionEnabled = true,
                currentScheduleWindow = ScheduleWindowSelector.current(scheduleWindows, now),
                motionSignal = motionSignalRepository.status.value.signal
            )
        ) ?: currentDecision
        currentDecision = decision
        requestLocationUpdates(decision.requestIntervalMillis)
        updateNotification()

        val fix = RawLocationFix(
            latitude = location.latitude,
            longitude = location.longitude,
            horizontalAccuracyMeters = if (location.hasAccuracy()) location.accuracy else null,
            altitudeMeters = if (location.hasAltitude()) location.altitude else null,
            provider = location.provider ?: "unknown",
            recordedAtMillis = location.time.takeIf { it > 0L } ?: now,
            policyMode = decision.mode.name,
            scheduleLowFrequency = decision.scheduleLowFrequency,
            motionSignal = motionSignalRepository.status.value.signal.name,
            speedMetersPerSecond = if (location.hasSpeed()) location.speed else null,
            bearingDegrees = if (location.hasBearing()) location.bearing else null
        )
        scope.launch {
            qualityCoordinator.handleFix(
                fix = fix,
                onAccepted = { accepted -> queueAccepted(accepted) },
                onDropped = { droppedFix, reason -> recordDropped(droppedFix, reason) }
            )
        }
    }

    private suspend fun queueAccepted(accepted: QualityAcceptedLocation) {
        locationQueueRepository.enqueueAccepted(accepted, rawJson(accepted))
        policyEngine?.onAcceptedLocation(
            PolicyLocation(
                latitude = accepted.fix.latitude,
                longitude = accepted.fix.longitude,
                recordedAtMillis = accepted.fix.recordedAtMillis
            )
        )
        pendingUploadCount += 1
        lastAcceptedLocationText = timeFormatter.format(
            Instant.ofEpochMilli(accepted.fix.recordedAtMillis).atZone(ZoneId.systemDefault())
        )
        lastAccuracyText = "${accepted.fix.horizontalAccuracyMeters?.toInt() ?: 0}m"
        lastDroppedReason = null
        updateNotification()
    }

    private suspend fun recordDropped(fix: RawLocationFix, reason: String) {
        locationQueueRepository.recordDropped(fix, reason)
        lastDroppedReason = reason.toLocationMessage()
        updateNotification()
    }

    private fun notification() = LocationNotificationRenderer.build(
        this,
        LocationNotificationState(
            mode = currentDecision.mode,
            nextExpectedLocationText = nextExpectedLocationText(currentDecision),
            lastAcceptedLocationText = lastAcceptedLocationText,
            lastAccuracyText = lastAccuracyText,
            pendingUploadCount = pendingUploadCount,
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
            nextExpectedLocationAtMillis = currentDecision.nextExpectedLocationAtMillis
                .takeUnless { it == Long.MAX_VALUE },
            lastAcceptedLocationText = lastAcceptedLocationText,
            lastAccuracyText = lastAccuracyText,
            pendingUploadCount = pendingUploadCount,
            apiState = apiState,
            lastDroppedReason = lastDroppedReason
        )
    }

    private fun enabledProviders(): List<String> {
        return listOf(LocationManager.GPS_PROVIDER, LocationManager.NETWORK_PROVIDER)
            .filter { manager.isProviderEnabled(it) }
    }

    private fun hasAnyLocationPermission(): Boolean {
        val fine = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
        val coarse = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)
        return fine == PackageManager.PERMISSION_GRANTED || coarse == PackageManager.PERMISSION_GRANTED
    }

    private fun nextExpectedLocationText(decision: PolicyDecision): String {
        if (decision.nextExpectedLocationAtMillis == Long.MAX_VALUE) return "暂停"
        val remainingMillis = (decision.nextExpectedLocationAtMillis - System.currentTimeMillis()).coerceAtLeast(0L)
        val minutes = (remainingMillis + 59_999L) / 60_000L
        return if (minutes <= 0L) "即将定位" else "$minutes 分钟后"
    }

    private fun rawJson(accepted: QualityAcceptedLocation): String {
        val fix = accepted.fix
        return JSONObject()
            .put("latitude", fix.latitude)
            .put("longitude", fix.longitude)
            .put("horizontalAccuracyMeters", fix.horizontalAccuracyMeters?.toDouble() ?: JSONObject.NULL)
            .put("provider", fix.provider)
            .put("altitudeMeters", accepted.altitudeMeters ?: JSONObject.NULL)
            .put("recordedAtUnixMs", fix.recordedAtMillis)
            .put("policyMode", fix.policyMode)
            .put("scheduleLowFrequency", fix.scheduleLowFrequency)
            .put("motionSignal", fix.motionSignal)
            .put("qualityFlags", JSONArray(accepted.qualityFlags.sorted()))
            .toString()
    }

    private fun String.toLocationMessage(): String = when (this) {
        "missing-horizontal-accuracy" -> "缺少水平精度"
        "horizontal-accuracy-too-low" -> "误差必须小于 50 米"
        else -> this
    }

    companion object {
        private val _runtimeState = MutableStateFlow(ForegroundLocationRuntimeState())
        val runtimeState: StateFlow<ForegroundLocationRuntimeState> = _runtimeState.asStateFlow()

        fun isRunning(): Boolean = runtimeState.value.isRunning

        val timeFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")
    }
}
