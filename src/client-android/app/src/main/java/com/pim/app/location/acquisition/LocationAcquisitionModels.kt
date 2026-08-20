package com.pim.app.location.acquisition

import com.pim.app.location.LocationSnapshot

enum class TriggerType(val storageSource: String) {
    MANUAL("manual"),
    AUTOMATIC("auto")
}

enum class AcquisitionPhase {
    Idle,
    Preparing,
    Acquiring,
    Evaluating,
    Completed,
    TimedOut,
    Failed,
    Cancelled
}

/**
 * 采集上下文：手动/自动统一引擎共享。priority 恒为 HIGH_ACCURACY（设计文档 §3.2），
 * 不再随策略档位映射；策略档位/日程低频/运动信号用于 rawJson 标注，
 * [requestIntervalMillis] 驱动自动常驻流的注册间隔。
 */
data class AcquisitionContext(
    val policyMode: String,
    val scheduleLowFrequency: Boolean,
    val motionSignal: String,
    val requestIntervalMillis: Long
)

data class LocationAcquisitionState(
    val sessionId: String? = null,
    val triggerType: TriggerType? = null,
    val phase: AcquisitionPhase = AcquisitionPhase.Idle,
    val bestLocation: LocationSnapshot? = null,
    val startedAtElapsedRealtimeMs: Long? = null,
    val deadlineAtElapsedRealtimeMs: Long? = null,
    val elapsedMs: Long = 0L,
    val lastQualityFlags: Set<String> = emptySet(),
    val errorReason: String? = null
) {
    val isBusy: Boolean
        get() = phase in setOf(
            AcquisitionPhase.Preparing,
            AcquisitionPhase.Acquiring,
            AcquisitionPhase.Evaluating
        )
}

/**
 * 自动常驻流状态：流是否注册、当前间隔与上下文标注、最近一次 fix 与质量标记。
 * 手动一次性会话状态走 [LocationAcquisitionState]，两者互不干扰。
 */
data class AutomaticStreamState(
    val active: Boolean = false,
    val requestIntervalMillis: Long = 0L,
    val policyMode: String? = null,
    val scheduleLowFrequency: Boolean? = null,
    val motionSignal: String? = null,
    val latestFix: LocationSnapshot? = null,
    val latestQualityFlags: Set<String> = emptySet(),
    val lastError: String? = null
)

sealed interface SessionStartResult {
    data class Started(val sessionId: String) : SessionStartResult
    data object Busy : SessionStartResult
    data class Rejected(val reason: String) : SessionStartResult
}
