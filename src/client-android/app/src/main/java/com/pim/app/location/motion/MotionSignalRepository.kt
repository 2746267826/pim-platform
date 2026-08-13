package com.pim.app.location.motion

import android.content.Context
import android.os.SystemClock
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
 */
@Singleton
class MotionSignalRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val _status = MutableStateFlow(MotionSignalStatus(MotionSignal.Unknown, null, null))
    val status: StateFlow<MotionSignalStatus> = _status.asStateFlow()

    private val detector = SelfMotionDetector(
        context = context,
        evaluator = SelfMotionEvaluator(
            nowElapsedRealtimeMillis = { SystemClock.elapsedRealtime() }
        ),
        onSignal = { signal ->
            if (signal != _status.value.signal) {
                _status.value = MotionSignalStatus(signal, issueCode = null, message = null)
            }
        }
    )

    fun register() {
        detector.start()
    }

    fun unregister() {
        detector.stop()
    }
}
