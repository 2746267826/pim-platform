package com.pim.app.location

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.os.Looper
import android.os.SystemClock
import androidx.core.content.ContextCompat
import com.pim.app.location.quality.AltitudeWaitCoordinator
import com.pim.app.location.quality.QualityAcceptedLocation
import com.pim.app.location.quality.RawLocationFix
import com.pim.app.mobile.sync.MobileSyncScheduler
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
    val autoSubmitted: Boolean = false
)

@Singleton
class LocationCaptureRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val locationQueueRepository: LocationQueueRepository,
    private val mobileSyncScheduler: MobileSyncScheduler
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val manager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
    private val qualityCoordinator = AltitudeWaitCoordinator()

    private var listener: LocationListener? = null
    private var startedAtElapsedMs: Long = 0L

    private val _state = MutableStateFlow(LocationCaptureState())
    val state: StateFlow<LocationCaptureState> = _state.asStateFlow()

    fun startCapture() {
        if (!hasAnyLocationPermission()) {
            _state.update {
                it.copy(
                    isCapturing = false,
                    statusMessage = "缺少定位权限，请先授权精确定位。",
                    inlineReason = "缺少定位权限。"
                )
            }
            return
        }

        stopCapture(clearStatus = true)
        val providers = enabledProviders()
        if (providers.isEmpty()) {
            _state.value = LocationCaptureState(
                statusMessage = "系统定位服务未开启。",
                inlineReason = "请先在系统设置中开启 GPS 或网络定位。"
            )
            return
        }

        startedAtElapsedMs = SystemClock.elapsedRealtime()
        _state.value = LocationCaptureState(
            isCapturing = true,
            statusMessage = "正在等待位置更新..."
        )

        val updateListener = object : LocationListener {
            override fun onLocationChanged(location: Location) {
                handleLocation(location, source = "实时更新")
            }

            override fun onProviderDisabled(provider: String) {
                _state.update { it.copy(statusMessage = "$provider 已关闭，继续等待其他来源。") }
            }

            override fun onProviderEnabled(provider: String) {
                _state.update { it.copy(statusMessage = "$provider 已开启，正在等待位置。") }
            }

            override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) = Unit
        }
        listener = updateListener

        requestUpdates(providers, updateListener)
        seedLastKnownLocation(providers)
        startWaitTimer()
    }

    fun stopCapture() {
        stopCapture(clearStatus = false)
    }

    private fun stopCapture(clearStatus: Boolean) {
        listener?.let { manager.removeUpdates(it) }
        listener = null
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

        val decision = LocationSubmissionPolicy.decide(snapshot.horizontalAccuracyMeters, state.value.autoSubmitted)
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
    private fun requestUpdates(providers: List<String>, updateListener: LocationListener) {
        providers.forEach { provider ->
            manager.requestLocationUpdates(provider, 1_000L, 0f, updateListener, Looper.getMainLooper())
        }
    }

    @SuppressLint("MissingPermission")
    private fun seedLastKnownLocation(providers: List<String>) {
        providers.mapNotNull { provider -> manager.getLastKnownLocation(provider) }
            .maxByOrNull { it.time }
            ?.let { handleLocation(it, source = "缓存位置") }
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
        val decision = LocationSubmissionPolicy.decide(snapshot.horizontalAccuracyMeters, state.value.autoSubmitted)
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
                    inlineReason = droppedReason?.toLocationMessage()
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

    private fun enabledProviders(): List<String> {
        val ordered = listOf(LocationManager.GPS_PROVIDER, LocationManager.NETWORK_PROVIDER)
        return ordered.filter { provider -> manager.isProviderEnabled(provider) }
    }

    private fun hasAnyLocationPermission(): Boolean {
        val fine = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION)
        val coarse = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION)
        return fine == PackageManager.PERMISSION_GRANTED || coarse == PackageManager.PERMISSION_GRANTED
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

    private fun String.toLocationMessage(): String = when (this) {
        "missing-horizontal-accuracy" -> "缺少水平精度信息，不能提交。"
        "horizontal-accuracy-too-low" -> "误差必须小于 50 米，不能提交。"
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
