package com.pim.app.keepalive

import android.content.SharedPreferences
import dagger.hilt.android.qualifiers.ApplicationContext
import android.content.Context
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 「保活与诊断」分区的持久化状态（REQ-22 / REQ-23 / REQ-15）。
 *
 * 单独一个 store 而不是塞进 [com.pim.app.settings.TrackingSettingsStore]：
 * 保活开关与节奏属于**新增**能力，混进采集设置会让「范围外不改采集档位」这条约束难以自证
 * （AC-12.2 要求 diff 中不出现对采集频率档位的改动）。
 */
data class KeepAliveSettings(
    /** 保活总开关（REQ-22）。默认开启：需求方优先级是「进程不中断 > 数据连续 > 可解释性」。 */
    val enabled: Boolean,
    /** 用户配置的节奏分钟数（AC-15.1：10-120，默认 30）。 */
    val configuredIntervalMinutes: Int,
    /** 当前实际生效的间隔（可能因降频高于配置值，REQ-17）。 */
    val effectiveIntervalMinutes: Int,
    /** 连续被压制次数（AC-17.2 的判定输入）。 */
    val consecutiveSuppressed: Int,
    /** 连续按时次数（AC-17.3 的判定输入）。 */
    val consecutiveOnTime: Int,
    /** 连续叫醒调用失败次数（AC-17.5：只点亮红点，不降频）。 */
    val consecutiveWakeFailures: Int,
    /** 用户标记为「我已完成」的不可检测引导项 key 集合（AC-23.3）。 */
    val completedGuidanceKeys: Set<String>,
    /**
     * 当前已登记闹钟的**预定触发时刻**（壁钟毫秒）。
     *
     * 持久化而不是放在 Intent extra 里：闹钟可能在被杀进程后由系统派发，
     * 此时进程是全新的，只有落盘的数据还在。延迟值（REQ-18）由它与实际时刻算出。
     */
    val pendingScheduledAtUtcMillis: Long?,
    /**
     * 当前点亮的健康红点原因集合（REQ-21）。
     *
     * **必须持久化**：保活要解决的正是「应用被杀」，若红点只存在内存里，
     * 进程重启后异常会静默消失——那比不显示更糟，用户会以为一切正常。
     */
    val activeHealthReasons: Set<String> = emptySet()
) {
    companion object {
        fun defaults(): KeepAliveSettings = KeepAliveSettings(
            enabled = true,
            configuredIntervalMinutes = AlarmSuppressionPolicy.DEFAULT_INTERVAL_MINUTES,
            effectiveIntervalMinutes = AlarmSuppressionPolicy.DEFAULT_INTERVAL_MINUTES,
            consecutiveSuppressed = 0,
            consecutiveOnTime = 0,
            consecutiveWakeFailures = 0,
            completedGuidanceKeys = emptySet(),
            pendingScheduledAtUtcMillis = null,
            activeHealthReasons = emptySet()
        )
    }
}

@Singleton
class KeepAliveSettingsStore @Inject constructor(
    @ApplicationContext context: Context
) : KeepAliveSettingsAccessor {
    private val preferences: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    override fun read(): KeepAliveSettings {
        val defaults = KeepAliveSettings.defaults()
        return KeepAliveSettings(
            enabled = preferences.getBoolean(KEY_ENABLED, defaults.enabled),
            configuredIntervalMinutes = AlarmSuppressionPolicy.clampConfiguredMinutes(
                preferences.getInt(KEY_CONFIGURED_INTERVAL, defaults.configuredIntervalMinutes)
            ),
            effectiveIntervalMinutes = AlarmSuppressionPolicy.clampConfiguredMinutes(
                preferences.getInt(KEY_EFFECTIVE_INTERVAL, defaults.effectiveIntervalMinutes)
            ),
            consecutiveSuppressed = preferences.getInt(KEY_CONSECUTIVE_SUPPRESSED, 0),
            consecutiveOnTime = preferences.getInt(KEY_CONSECUTIVE_ON_TIME, 0),
            consecutiveWakeFailures = preferences.getInt(KEY_CONSECUTIVE_WAKE_FAILURES, 0),
            completedGuidanceKeys = preferences.getStringSet(KEY_COMPLETED_GUIDANCE, emptySet())
                ?.toSet() ?: emptySet(),
            pendingScheduledAtUtcMillis = preferences.getLong(KEY_PENDING_SCHEDULED_AT, 0L)
                .takeIf { it > 0L },
            activeHealthReasons = preferences.getStringSet(KEY_ACTIVE_HEALTH_REASONS, emptySet())
                ?.toSet() ?: emptySet()
        )
    }

    /**
     * 写入设置。
     *
     * 这里用 **`commit()` 同步落盘**而不是 `apply()`：
     * 本 store 的字段正是为「进程被杀之后仍然保有状态」而存在的
     * （红点原因、已登记的预定时刻、连续计数），而 `apply()` 是异步写盘——
     * 在 `raise()` 之后立刻被杀，这次写入可能还没落盘，状态就随进程一起消失
     * 了（独立 review 指出）。写入频率很低（用户操作 + 每个叫醒周期一次），
     * 同步写盘的成本可以接受。
     */
    override fun write(settings: KeepAliveSettings): KeepAliveSettings {
        preferences.edit()
            .putBoolean(KEY_ENABLED, settings.enabled)
            .putInt(
                KEY_CONFIGURED_INTERVAL,
                AlarmSuppressionPolicy.clampConfiguredMinutes(settings.configuredIntervalMinutes)
            )
            .putInt(
                KEY_EFFECTIVE_INTERVAL,
                AlarmSuppressionPolicy.clampConfiguredMinutes(settings.effectiveIntervalMinutes)
            )
            .putInt(KEY_CONSECUTIVE_SUPPRESSED, settings.consecutiveSuppressed)
            .putInt(KEY_CONSECUTIVE_ON_TIME, settings.consecutiveOnTime)
            .putInt(KEY_CONSECUTIVE_WAKE_FAILURES, settings.consecutiveWakeFailures)
            .putStringSet(KEY_COMPLETED_GUIDANCE, settings.completedGuidanceKeys)
            .putLong(KEY_PENDING_SCHEDULED_AT, settings.pendingScheduledAtUtcMillis ?: 0L)
            .putStringSet(KEY_ACTIVE_HEALTH_REASONS, settings.activeHealthReasons)
            .commit()
        return read()
    }

    /** 总开关（AC-22.2 / AC-22.3）。 */
    fun setEnabled(enabled: Boolean): KeepAliveSettings = write(read().copy(enabled = enabled))

    /** 用户调整节奏（AC-15.1）：同时把生效值拉回新配置值，避免旧的降频值残留。 */
    fun setConfiguredInterval(minutes: Int): KeepAliveSettings {
        val clamped = AlarmSuppressionPolicy.clampConfiguredMinutes(minutes)
        return write(
            read().copy(
                configuredIntervalMinutes = clamped,
                effectiveIntervalMinutes = clamped,
                consecutiveSuppressed = 0,
                consecutiveOnTime = 0
            )
        )
    }

    /** 手动勾选「我已完成」（AC-23.3：记住状态）。 */
    fun markGuidanceCompleted(key: String): KeepAliveSettings =
        write(read().copy(completedGuidanceKeys = read().completedGuidanceKeys + key))

    /** 取消勾选。 */
    fun clearGuidanceCompleted(key: String): KeepAliveSettings =
        write(read().copy(completedGuidanceKeys = read().completedGuidanceKeys - key))

    fun isGuidanceCompleted(key: String): Boolean = read().completedGuidanceKeys.contains(key)

    private companion object {
        const val PREFS_NAME = "pim_keepalive"
        const val KEY_ENABLED = "keepalive.enabled"
        const val KEY_CONFIGURED_INTERVAL = "keepalive.configured_interval_minutes"
        const val KEY_EFFECTIVE_INTERVAL = "keepalive.effective_interval_minutes"
        const val KEY_CONSECUTIVE_SUPPRESSED = "keepalive.consecutive_suppressed"
        const val KEY_CONSECUTIVE_ON_TIME = "keepalive.consecutive_on_time"
        const val KEY_CONSECUTIVE_WAKE_FAILURES = "keepalive.consecutive_wake_failures"
        const val KEY_COMPLETED_GUIDANCE = "keepalive.completed_guidance_keys"
        const val KEY_PENDING_SCHEDULED_AT = "keepalive.pending_scheduled_at_utc_millis"
        const val KEY_ACTIVE_HEALTH_REASONS = "keepalive.active_health_reasons"
    }
}
