package com.pim.app.location

import com.pim.app.location.acquisition.AcquisitionPhase
import com.pim.app.location.acquisition.LocationAcquisitionCoordinator
import com.pim.app.location.acquisition.LocationAcquisitionState
import com.pim.app.location.quality.LocationQualityGate
import com.pim.app.location.service.ForegroundLocationController
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

data class LocationCaptureState(
    val isCapturing: Boolean = false,
    val latest: LocationSnapshot? = null,
    val waitDurationMs: Long = 0L,
    val submitStatus: String = "尚未提交",
    val statusMessage: String = "尚未开始定位",
    val inlineReason: String? = null,
    val lastQualityFlags: Set<String> = emptySet(),
    val showLowQualityWarning: Boolean = false
)

@Singleton
class LocationCaptureRepository @Inject constructor(
    private val coordinator: LocationAcquisitionCoordinator,
    private val controller: ForegroundLocationController
) {
    private val mappingScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private val _state = MutableStateFlow(LocationCaptureState())
    val state: StateFlow<LocationCaptureState> = _state.asStateFlow()

    init {
        mappingScope.launch {
            coordinator.state.collect { acqState ->
                _state.value = acqState.toCaptureState()
            }
        }
    }

    fun startCapture() {
        controller.startManualSession()
    }

    fun stopCapture() {
        controller.cancelLocationSession(coordinator.state.value.sessionId)
    }
}

internal fun formatSubmitStatus(enqueued: Boolean, error: String? = null): String {
    return if (enqueued) {
        "已加入上传队列"
    } else {
        "加入上传队列失败：${error ?: "未知错误"}"
    }
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

internal fun LocationAcquisitionState.toCaptureState(): LocationCaptureState {
    val lowQuality = lastQualityFlags.contains(LocationQualityGate.LOW_QUALITY_ACCURACY_FLAG)
    return LocationCaptureState(
        isCapturing = phase in setOf(
            AcquisitionPhase.Preparing,
            AcquisitionPhase.Acquiring,
            AcquisitionPhase.Evaluating
        ),
        latest = bestLocation,
        waitDurationMs = elapsedMs,
        submitStatus = when (phase) {
            AcquisitionPhase.Completed -> "已加入上传队列"
            AcquisitionPhase.Failed -> "失败：${errorReason ?: "未知错误"}"
            AcquisitionPhase.Cancelled -> "已取消"
            AcquisitionPhase.TimedOut -> "获取超时"
            else -> "尚未提交"
        },
        statusMessage = when (phase) {
            AcquisitionPhase.Idle -> "尚未开始定位"
            AcquisitionPhase.Acquiring -> "正在获取位置..."
            AcquisitionPhase.Evaluating -> "正在评估位置质量..."
            AcquisitionPhase.Completed ->
                if (lowQuality) {
                    "定位完成（精度不足，已标记低质量）"
                } else {
                    "定位完成"
                }
            AcquisitionPhase.TimedOut -> "获取位置超时，未获得任何定位结果"
            AcquisitionPhase.Failed -> "获取位置失败"
            AcquisitionPhase.Cancelled -> "定位已取消"
            else -> "准备中..."
        },
        inlineReason = errorReason,
        lastQualityFlags = lastQualityFlags,
        showLowQualityWarning = lowQuality
    )
}
