package com.pim.app.location.highspeed

/** 高速轨迹模式状态：未激活 / 触发累计中（密集采样确认）/ 高速档生效。 */
enum class HighSpeedMode {
    Inactive,
    Accumulating,
    Active
}

/**
 * 高速轨迹模式状态机（设计文档 PR#63 §2.1）：纯逻辑，不依赖 Android API，可单测。
 *
 * - 未激活：GPS speed ≥ 8 km/h 持续 10s → [HighSpeedMode.Active]；期间为
 *   [HighSpeedMode.Accumulating]（策略引擎借此切换到 2.5s 密集采样以便快速确认）。
 *   低于阈值或 speed 为 null 的样本立即重置触发累计（起步速度波动不误触发）。
 * - 激活：speed < 1 km/h 持续 60s → 回落 [HighSpeedMode.Inactive]（等红灯 30s
 *   不掉档）；speed 为 null 视为无法确认运动，计入回落累计（对电量和数据量保守）。
 * - 速度来源为 GPS 多普勒速度 [android.location.Location.getSpeed]，不依赖运动检测。
 *
 * 时钟使用单调时钟（elapsedRealtime），避免墙钟回拨/调整破坏防抖累计。
 */
class HighSpeedTracker(
    private val triggerSpeedMetersPerSecond: Float = TRIGGER_SPEED_METERS_PER_SECOND,
    private val fallbackSpeedMetersPerSecond: Float = FALLBACK_SPEED_METERS_PER_SECOND,
    private val triggerDebounceMillis: Long = TRIGGER_DEBOUNCE_MILLIS,
    private val fallbackDebounceMillis: Long = FALLBACK_DEBOUNCE_MILLIS,
    private val nowElapsedRealtimeMillis: () -> Long
) {
    var mode: HighSpeedMode = HighSpeedMode.Inactive
        private set

    /** 进入高速档的单调时钟时刻；未激活时为 null。 */
    var activeSinceElapsedRealtimeMillis: Long? = null
        private set

    private var fastStreakStartAtMillis: Long? = null
    private var slowStreakStartAtMillis: Long? = null

    fun observe(speedMetersPerSecond: Float?) {
        val now = nowElapsedRealtimeMillis()
        when (mode) {
            HighSpeedMode.Active -> observeWhileActive(speedMetersPerSecond, now)
            else -> observeWhileIdle(speedMetersPerSecond, now)
        }
    }

    fun reset() {
        mode = HighSpeedMode.Inactive
        activeSinceElapsedRealtimeMillis = null
        fastStreakStartAtMillis = null
        slowStreakStartAtMillis = null
    }

    private fun observeWhileIdle(speedMetersPerSecond: Float?, now: Long) {
        if (speedMetersPerSecond != null && speedMetersPerSecond >= triggerSpeedMetersPerSecond) {
            val start = fastStreakStartAtMillis ?: now
            fastStreakStartAtMillis = start
            if (now - start >= triggerDebounceMillis) {
                enterActive(now)
            } else {
                mode = HighSpeedMode.Accumulating
            }
        } else {
            fastStreakStartAtMillis = null
            if (mode == HighSpeedMode.Accumulating) {
                mode = HighSpeedMode.Inactive
            }
        }
    }

    private fun observeWhileActive(speedMetersPerSecond: Float?, now: Long) {
        if (speedMetersPerSecond == null || speedMetersPerSecond < fallbackSpeedMetersPerSecond) {
            val start = slowStreakStartAtMillis ?: now
            slowStreakStartAtMillis = start
            if (now - start >= fallbackDebounceMillis) {
                exitActive()
            }
        } else {
            slowStreakStartAtMillis = null
        }
    }

    private fun enterActive(now: Long) {
        mode = HighSpeedMode.Active
        activeSinceElapsedRealtimeMillis = now
        fastStreakStartAtMillis = null
        slowStreakStartAtMillis = null
    }

    private fun exitActive() {
        mode = HighSpeedMode.Inactive
        activeSinceElapsedRealtimeMillis = null
        fastStreakStartAtMillis = null
        slowStreakStartAtMillis = null
    }

    companion object {
        const val TRIGGER_SPEED_KMH = 8f
        const val FALLBACK_SPEED_KMH = 1f
        const val TRIGGER_DEBOUNCE_MILLIS = 10_000L
        const val FALLBACK_DEBOUNCE_MILLIS = 60_000L
        const val TRIGGER_SPEED_METERS_PER_SECOND = TRIGGER_SPEED_KMH / 3.6f
        const val FALLBACK_SPEED_METERS_PER_SECOND = FALLBACK_SPEED_KMH / 3.6f
    }
}
