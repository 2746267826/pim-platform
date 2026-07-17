package com.pim.app.location

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.os.Build
import android.os.Looper
import android.os.SystemClock
import androidx.core.content.ContextCompat
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationAvailability
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.pim.app.location.quality.AltitudeWaitCoordinator
import com.pim.app.location.quality.LocationQualityGate
import com.pim.app.location.quality.QualityAcceptedLocation
import com.pim.app.location.quality.RawLocationFix
import com.pim.app.mobile.sync.MobileSyncScheduler
import com.pim.app.settings.TrackingSettingsStore
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject

data class LocationSnapshot(
    val latitude: Double,
    val longitude: Double,
    val horizontalAccuracyMeters: Float?,
    val provider: String,
    val source: String,
    val altitudeMeters: Double?,
    val speedMetersPerSecond: Float?,
    val bearingDegrees: Float?,
    val timeMillis: Long
)

data class LocationCaptureState(
    val isCapturing: Boolean = false,
    val latest: LocationSnapshot? = null,
    val waitDurationMs: Long = 0L,
    val submitStatus: String = "尚未提交",
    val statusMessage: String = "尚未开始定位",
    val inlineReason: String? = null,
    val isSubmitting: Boolean = false,
    val autoSubmitted: Boolean = false,
    val maxUploadAccuracyMetersExclusive: Float = 50f
)

@Singleton
class LocationCaptureRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val locationQueueRepository: LocationQueueRepository,
    private val mobileSyncScheduler: MobileSyncScheduler,
    private val trackingSettingsStore: TrackingSettingsStore
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val fusedClient = LocationServices.getFusedLocationProviderClient(context)
    private lateinit var qualityCoordinator: AltitudeWaitCoordinator

    private var locationCallback: LocationCallback? = null
    private var startedAtElapsedMs: Long = 0L

    private val _state = MutableStateFlow(LocationCaptureState())
    val state: StateFlow<LocationCaptureState> = _state.asStateFlow()

    fun startCapture() {
        val settings = trackingSettingsStore.read()
        cancelPendingQualityWait()

        if (GoogleApiAvailability.getInstance()
                .isGooglePlayServicesAvailable(context) != ConnectionResult.SUCCESS
        ) {
            _state.update {
                it.copy(
                    statusMessage = "Google Play Services 不可用，无法定位。",
                    inlineReason = "请安装或更新 Google Play Services。"
                )
            }
            return
        }

        if (!hasAnyLocationPermission()) {
            _state.update {
                it.copy(
                    isCapturing = false,
                    statusMessage = "缺少定位权限，请先授权精确定位。",
                    inlineReason = "缺少定位权限。",
                    maxUploadAccuracyMetersExclusive = settings.maxUploadAccuracyMetersExclusive
                )
            }
            return
        }

        if (!isLocationEnabled()) {
            _state.value = LocationCaptureState(
                statusMessage = "系统定位服务未开启。",
                inlineReason = "请先在系统设置中开启定位服务。",
                maxUploadAccuracyMetersExclusive = settings.maxUploadAccuracyMetersExclusive
            )
            return
        }

        stopCapture(clearStatus = true)

        startedAtElapsedMs = SystemClock.elapsedRealtime()
        qualityCoordinator = AltitudeWaitCoordinator(
            LocationQualityGate.fromTrackingSettings(settings)
        )
        _state.value = LocationCaptureState(
            isCapturing = true,
            statusMessage = "正在等待位置更新...",
            maxUploadAccuracyMetersExclusive = settings.maxUploadAccuracyMetersExclusive
        )

        val request = LocationRequest.Builder(1_000L)
            .setMinUpdateIntervalMillis(800L)
            .setPriority(Priority.PRIORITY_HIGH_ACCURACY)
            .build()

        val callback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                if (locationCallback !== this || !_state.value.isCapturing) return
                result.lastLocation?.let { handleLocation(it, source = "实时更新") }
            }

            override fun onLocationAvailability(availability: LocationAvailability) {
                if (locationCallback !== this) return
                if (!availability.isLocationAvailable && _state.value.isCapturing) {
                    _state.update {
                        it.copy(
                            statusMessage = "定位暂时不可用",
                            inlineReason = "请检查系统定位是否开启。"
                        )
                    }
                }
            }
        }
        locationCallback = callback
        fusedClient.requestLocationUpdates(request, callback, Looper.getMainLooper())
            .addOnFailureListener { e ->
                if (locationCallback === callback) {
                    runCatching { fusedClient.removeLocationUpdates(callback) }
                    locationCallback = null
                    _state.update { current ->
                        applyLocationRequestFailure(current, e.message)
                    }
                }
            }
        seedLastKnownLocation()
        startWaitTimer()
    }

    fun stopCapture() {
        stopCapture(clearStatus = false)
    }

    private fun stopCapture(clearStatus: Boolean) {
        cancelPendingQualityWait()
        locationCallback?.let { fusedClient.removeLocationUpdates(it) }
        locationCallback = null
        if (!clearStatus) {
            _state.update { it.copy(isCapturing = false, statusMessage = "定位已停止") }
        }
    }

    fun submitCurrentLocationManually() {
        val snapshot = state.value.latest
        if (snapshot == null) {
            _state.update {
                it.copy(
                    submitStatus = "没有可提交的位置。",
                    inlineReason = "请先获取当前位置。"
                )
            }
            return
        }

        val decision = LocationSubmissionPolicy.decide(
            snapshot.horizontalAccuracyMeters,
            state.value.autoSubmitted,
            state.value.maxUploadAccuracyMetersExclusive
        )
        if (!decision.canSubmitManually) {
            _state.update {
                it.copy(
                    submitStatus = "未提交",
                    inlineReason = decision.reason
                )
            }
            return
        }

        scope.launch { submitSnapshot(snapshot, isAutoSubmitted = false) }
    }

    @SuppressLint("MissingPermission")
    private fun seedLastKnownLocation() {
        fusedClient.lastLocation
            .addOnSuccessListener { location ->
                // 只在仍活跃时处理，防止 stop 后传入过期/陈旧缓存
                if (
                    location != null &&
                    locationCallback != null &&
                    state.value.isCapturing &&
                    isUsableSeedLocation(location.time, System.currentTimeMillis())
                ) {
                    handleLocation(location, source = "缓存位置")
                }
            }
            .addOnFailureListener { /* seed 失败不阻塞整体流程 */ }
    }

    private fun isLocationEnabled(): Boolean {
        val lm = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            lm.isLocationEnabled
        } else {
            @Suppress("DEPRECATION")
            lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER) ||
                lm.isProviderEnabled(LocationManager.GPS_PROVIDER)
        }
    }

    private fun startWaitTimer() {
        scope.launch {
            while (isActive && state.value.isCapturing) {
                _state.update { it.copy(waitDurationMs = SystemClock.elapsedRealtime() - startedAtElapsedMs) }
                delay(1_000L)
            }
        }
    }

    private fun handleLocation(location: Location, source: String) {
        val snapshot = LocationSnapshot(
            latitude = location.latitude,
            longitude = location.longitude,
            horizontalAccuracyMeters = if (location.hasAccuracy()) location.accuracy else null,
            provider = location.provider ?: "unknown",
            source = source,
            altitudeMeters = if (location.hasAltitude()) location.altitude else null,
            speedMetersPerSecond = if (location.hasSpeed()) location.speed else null,
            bearingDegrees = if (location.hasBearing()) location.bearing else null,
            timeMillis = location.time
        )
        val decision = LocationSubmissionPolicy.decide(
            snapshot.horizontalAccuracyMeters,
            state.value.autoSubmitted,
            state.value.maxUploadAccuracyMetersExclusive
        )
        _state.update {
            it.copy(
                latest = snapshot,
                waitDurationMs = SystemClock.elapsedRealtime() - startedAtElapsedMs,
                statusMessage = "已收到 ${snapshot.provider} 位置。",
                inlineReason = decision.reason
            )
        }

        if (decision.shouldAutoSubmit) {
            scope.launch { submitSnapshot(snapshot, isAutoSubmitted = true) }
        }
    }

    private suspend fun submitSnapshot(snapshot: LocationSnapshot, isAutoSubmitted: Boolean) {
        if (state.value.isSubmitting) return
        var acceptedLocation: QualityAcceptedLocation? = null
        var droppedReason: String? = null
        qualityCoordinator.handleFix(
            fix = snapshot.toRawLocationFix(),
            onAccepted = { acceptedLocation = it },
            onDropped = { _, reason -> droppedReason = reason }
        )

        val accepted = acceptedLocation
        if (accepted == null) {
            _state.update {
                it.copy(
                    submitStatus = "未提交",
                    inlineReason = droppedReason?.toLocationMessage(state.value.maxUploadAccuracyMetersExclusive)
                )
            }
            return
        }
        if (state.value.isSubmitting) return

        _state.update {
            it.copy(
                isSubmitting = true,
                submitStatus = if (isAutoSubmitted) {
                    "误差符合要求，自动提交中..."
                } else {
                    "手动提交中..."
                },
                inlineReason = null
            )
        }

        val json = rawJson(accepted, snapshot.source, isAutoSubmitted)
        val result = enqueueThenSchedule(
            enqueue = { locationQueueRepository.enqueueAccepted(accepted, json) },
            schedule = { mobileSyncScheduler.enqueueNow() }
        )

        _state.update { current ->
            current.copy(
                isSubmitting = false,
                autoSubmitted = resolveAutoSubmittedState(current.autoSubmitted, isAutoSubmitted, result.isSuccess),
                submitStatus = formatSubmitStatus(result.isSuccess, result.exceptionOrNull()?.message),
                inlineReason = if (result.isSuccess) null else result.exceptionOrNull()?.message
            )
        }
    }

    private fun LocationSnapshot.toRawLocationFix(): RawLocationFix = RawLocationFix(
        latitude = latitude,
        longitude = longitude,
        horizontalAccuracyMeters = horizontalAccuracyMeters,
        altitudeMeters = altitudeMeters,
        provider = provider,
        recordedAtMillis = timeMillis,
        policyMode = "PowerSavingNormal",
        scheduleLowFrequency = false,
        motionSignal = "Unknown",
        speedMetersPerSecond = speedMetersPerSecond,
        bearingDegrees = bearingDegrees
    )

    private fun hasAnyLocationPermission(): Boolean {
        val fine = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION)
        val coarse = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION)
        return fine == PackageManager.PERMISSION_GRANTED || coarse == PackageManager.PERMISSION_GRANTED
    }

    private fun cancelPendingQualityWait() {
        if (::qualityCoordinator.isInitialized) {
            qualityCoordinator.cancelPending()
        }
    }

    private fun rawJson(
        accepted: QualityAcceptedLocation,
        source: String,
        isAutoSubmitted: Boolean
    ): String {
        val fix = accepted.fix
        return JSONObject()
            .put("latitude", fix.latitude)
            .put("longitude", fix.longitude)
            .put("horizontalAccuracyMeters", fix.horizontalAccuracyMeters?.toDouble() ?: JSONObject.NULL)
            .put("provider", fix.provider)
            .put("source", source)
            .put("altitudeMeters", accepted.altitudeMeters ?: JSONObject.NULL)
            .put("speedMetersPerSecond", fix.speedMetersPerSecond?.toDouble() ?: JSONObject.NULL)
            .put("bearingDegrees", fix.bearingDegrees?.toDouble() ?: JSONObject.NULL)
            .put("recordedAtUnixMs", fix.recordedAtMillis)
            .put("submittedAtUnixMs", System.currentTimeMillis())
            .put("isAutoSubmitted", isAutoSubmitted)
            .put("policyMode", fix.policyMode)
            .put("scheduleLowFrequency", fix.scheduleLowFrequency)
            .put("motionSignal", fix.motionSignal)
            .put("qualityFlags", JSONArray(accepted.qualityFlags.sorted()))
            .toString()
    }

    private fun String.toLocationMessage(threshold: Float): String = when (this) {
        "missing-horizontal-accuracy" -> "缺少水平精度信息，不能提交。"
        "horizontal-accuracy-too-low" -> "误差必须小于 ${formatAccuracyThresholdMeters(threshold)} 米，不能提交。"
        else -> this
    }

}

internal fun formatSubmitStatus(enqueued: Boolean, error: String? = null): String {
    return if (enqueued) {
        "已加入上传队列"
    } else {
        "加入上传队列失败：${error ?: "未知错误"}"
    }
}

internal fun resolveAutoSubmittedState(current: Boolean, isAutoSubmit: Boolean, success: Boolean): Boolean {
    return if (isAutoSubmit && success) true else current
}

internal fun applyLocationRequestFailure(
    current: LocationCaptureState,
    errorMessage: String?
): LocationCaptureState {
    return current.copy(
        isCapturing = false,
        statusMessage = "定位请求失败",
        inlineReason = errorMessage ?: "未知错误"
    )
}

internal const val SEED_LOCATION_MAX_AGE_MILLIS: Long = 5L * 60L * 1000L

internal fun isUsableSeedLocation(locationTimeMillis: Long, nowMillis: Long): Boolean {
    if (locationTimeMillis <= 0L) return false
    val ageMillis = nowMillis - locationTimeMillis
    return ageMillis in 0L..SEED_LOCATION_MAX_AGE_MILLIS
}

internal suspend fun enqueueThenSchedule(
    enqueue: suspend () -> Unit,
    schedule: () -> Unit
): Result<Unit> {
    try {
        enqueue()
        schedule()
        return Result.success(Unit)
    } catch (ex: kotlinx.coroutines.CancellationException) {
        throw ex
    } catch (ex: Exception) {
        return Result.failure(ex)
    }
}
