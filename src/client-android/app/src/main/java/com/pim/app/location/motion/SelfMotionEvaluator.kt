package com.pim.app.location.motion

import com.pim.app.location.policy.MotionSignal
import kotlin.math.sqrt

/**
 * 自研运动检测纯逻辑：对加速度计模长样本做标准差分块判定（60 样本 ≈3s 窗口），
 * 叠加时间累计式双防抖（运动 ≥5s、静止 ≥20s），再结合步数增量输出运动信号。
 * 不依赖 GMS 活动识别，不依赖 Android API，可单测。
 *
 * 映射规则（设计文档 §3.4）：
 * - 防抖态 MOVING + 本次移动段落有步数增量 → Walking
 * - 防抖态 MOVING + 无步数 → Moving
 * - 防抖态 STILL → Still
 * - Running/OnBicycle/InVehicle 保留枚举但不产生（二期细分）
 */
class SelfMotionEvaluator(
    private val windowSizeSamples: Int = 60,
    private val windowDurationMillis: Long = 3_000L,
    private val movingDebounceMillis: Long = 5_000L,
    private val stillDebounceMillis: Long = 20_000L,
    private val nowElapsedRealtimeMillis: () -> Long
) {
    enum class RawState { STILL, SHAKING, MOVING }

    private val magnitudes = ArrayDeque<Double>()
    private var lastWindowAtMillis: Long? = null
    private var movingStreakMillis = 0L
    private var stillStreakMillis = 0L
    private var debouncedMoving = false
    private var episodeStepTotal = 0L
    private var lastStepTotal = -1L
    private var signal = MotionSignal.Unknown

    fun accelMagnitude(magnitude: Double) {
        magnitudes.addLast(magnitude)
        if (magnitudes.size >= windowSizeSamples) {
            val now = nowElapsedRealtimeMillis()
            val windowDuration = lastWindowAtMillis?.let { now - it } ?: windowDurationMillis
            val raw = evaluateRaw(magnitudes)
            magnitudes.clear()
            accumulate(raw, windowDuration.coerceAtLeast(0L))
            lastWindowAtMillis = now
            recomputeSignal()
        }
    }

    fun stepCount(total: Long) {
        if (lastStepTotal == -1L) {
            lastStepTotal = total
            return
        }
        if (total > lastStepTotal) {
            episodeStepTotal += total - lastStepTotal
            lastStepTotal = total
            recomputeSignal()
        }
    }

    fun significantMotionTriggered() {
        if (!debouncedMoving) {
            movingStreakMillis += windowDurationMillis
            if (movingStreakMillis >= movingDebounceMillis) {
                enterMovingEpisode()
            }
            recomputeSignal()
        }
    }

    fun currentSignal(): MotionSignal = signal

    private fun evaluateRaw(samples: ArrayDeque<Double>): RawState {
        val mean = samples.average()
        val std = sqrt(samples.map { (it - mean) * (it - mean) }.average())
        return when {
            std < 0.25 -> RawState.STILL
            std < 1.0 -> RawState.SHAKING
            else -> RawState.MOVING
        }
    }

    private fun accumulate(raw: RawState, windowDuration: Long) {
        if (raw == RawState.STILL) {
            movingStreakMillis = 0L
            stillStreakMillis += windowDuration
            if (debouncedMoving && stillStreakMillis >= stillDebounceMillis) {
                debouncedMoving = false
                episodeStepTotal = 0L
                movingStreakMillis = 0L
            }
        } else {
            stillStreakMillis = 0L
            movingStreakMillis += windowDuration
            if (!debouncedMoving && movingStreakMillis >= movingDebounceMillis) {
                enterMovingEpisode()
            }
        }
    }

    private fun enterMovingEpisode() {
        debouncedMoving = true
        episodeStepTotal = 0L
        stillStreakMillis = 0L
    }

    private fun recomputeSignal() {
        signal = if (!debouncedMoving) {
            MotionSignal.Still
        } else if (episodeStepTotal > 0L) {
            MotionSignal.Walking
        } else {
            MotionSignal.Moving
        }
    }
}
