package com.pim.app.location.motion

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.SystemClock
import androidx.core.content.ContextCompat
import com.pim.app.location.policy.MotionSignal
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class MotionSignalStatus(
    val signal: MotionSignal,
    val issueCode: String?,
    val message: String?
)

/**
 * 运动信号仓库：驱动自研传感器检测（[SelfMotionDetector]），
 * 提供 [MotionSignalStatus] 状态流供策略引擎消费。
 *
 * [register] 幂等：重复调用不重置检测状态（服务循环会周期性调用）。
 * 缺少 ACTIVITY_RECOGNITION 权限时优雅降级（仅加速度计工作）并在状态里
 * 携带 issue，避免"悄悄失效"。
 */
@Singleton
class MotionSignalRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val _status = MutableStateFlow(MotionSignalStatus(MotionSignal.Unknown, null, null))
    val status: StateFlow<MotionSignalStatus> = _status.asStateFlow()

    private var permissionIssue: String? = null

    private val detector = SelfMotionDetector(
        context = context,
        evaluator = SelfMotionEvaluator(
            nowElapsedRealtimeMillis = { SystemClock.elapsedRealtime() }
        ),
        onSignal = { signal ->
            if (signal != _status.value.signal) {
                _status.value = MotionSignalStatus(
                    signal = signal,
                    issueCode = if (permissionIssue != null) ACTIVITY_RECOGNITION_MISSING_CODE else null,
                    message = permissionIssue
                )
            }
        }
    )

    fun register() {
        val newIssue = if (hasActivityRecognitionPermission()) {
            null
        } else {
            ACTIVITY_RECOGNITION_MISSING_MESSAGE
        }
        permissionIssue = newIssue
        // 权限状态与状态流携带的 issue 不一致时立即同步，不依赖信号变化：
        // 权限被拒但信号稳定（如长期静止）时 UI 也能看到提示；
        // 权限恢复后残留 issue 立即清除。
        val current = _status.value
        val issueStale = if (newIssue == null) {
            current.issueCode != null
        } else {
            current.issueCode != ACTIVITY_RECOGNITION_MISSING_CODE
        }
        if (issueStale) {
            _status.value = MotionSignalStatus(
                signal = current.signal,
                issueCode = if (newIssue != null) ACTIVITY_RECOGNITION_MISSING_CODE else null,
                message = newIssue
            )
        }
        detector.start()
    }

    fun unregister() {
        detector.stop()
    }

    private fun hasActivityRecognitionPermission(): Boolean {
        val permission = ContextCompat.checkSelfPermission(context, Manifest.permission.ACTIVITY_RECOGNITION)
        return permission == PackageManager.PERMISSION_GRANTED
    }

    companion object {
        const val ACTIVITY_RECOGNITION_MISSING_CODE = "activity-recognition-missing"
        const val ACTIVITY_RECOGNITION_MISSING_MESSAGE = "健身运动权限未开启，步数/重大运动检测不可用"
    }
}
