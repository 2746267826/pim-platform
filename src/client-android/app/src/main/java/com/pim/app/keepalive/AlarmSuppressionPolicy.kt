package com.pim.app.keepalive

/**
 * 被压制判定与自动降频（REQ-17）。
 *
 * 参数来自工单 §9.1 P2（需求方 2026-09-23 确认），**不得自行改数**：
 * - 单次延迟 > 15 分钟记为「被压制」（AC-17.1）
 * - 连续 3 次被压制 → 间隔 ×2，上限 120 分钟（AC-17.2）
 * - 连续 2 次延迟 ≤15 分钟 → 回到配置值（AC-17.3）
 * - 因暂停 / 关机导致的未执行**不计为**被压制（AC-17.4）
 * - 调用失败**不**触发降频，只记台账与红点并在下个周期重试（AC-17.5）
 *
 * 本类是纯函数式的：给定「已生效的间隔」与「最近若干次记录」，算出「下一次应使用的间隔」。
 * 它不依赖 Android 与时钟，因此参数边界可以被逐条断言。
 */
object AlarmSuppressionPolicy {

    /** 延迟超过该值即「被压制」（15 分钟，§9.1 P2）。 */
    const val SUPPRESSION_THRESHOLD_MINUTES = 15

    /** 连续被压制多少次后开始降频（3 次，§9.1 P2）。 */
    const val SUPPRESSIONS_BEFORE_BACKOFF = 3

    /** 连续多少次按时后回到配置值（2 次，§9.1 P2）。 */
    const val ON_TIME_BEFORE_RESET = 2

    /** 降频倍率（×2，§9.1 P2）。 */
    const val BACKOFF_MULTIPLIER = 2

    /** 降频后间隔上限：120 分钟（§9.1 P2）。 */
    const val MAX_BACKOFF_INTERVAL_MINUTES = 120

    /** 默认节奏：30 分钟（§9.1 / REQ-15）。 */
    const val DEFAULT_INTERVAL_MINUTES = 30

    /** 可调下限：10 分钟（REQ-15）。 */
    const val MIN_INTERVAL_MINUTES = 10

    /** 可调上限：120 分钟（REQ-15）。 */
    const val MAX_INTERVAL_MINUTES = 120

    val SUPPRESSION_THRESHOLD_MILLIS: Long = SUPPRESSION_THRESHOLD_MINUTES * 60_000L

    /**
     * 延迟是否构成「被压制」（AC-17.1）。
     *
     * 判定线是**严格大于** 15 分钟：恰好 15 分钟**不算**被压制。
     * 这是本文件唯一的阈值比较点，执行链与策略都调它，避免两处各判一次判出不同结果。
     */
    fun isSuppressedDelay(delayMillis: Long): Boolean = delayMillis > SUPPRESSION_THRESHOLD_MILLIS

    /** 延迟是否算「按时」（与 [isSuppressedDelay] 互补，同一条判定线）。 */
    fun isOnTimeDelay(delayMillis: Long): Boolean = !isSuppressedDelay(delayMillis)

    /**
     * 把用户配置的节奏夹到允许区间（AC-15.1：10-120 分钟，默认 30）。
     */
    fun clampConfiguredMinutes(minutes: Int): Int =
        minutes.coerceIn(MIN_INTERVAL_MINUTES, MAX_INTERVAL_MINUTES)

    /**
     * 根据最近记录算出**下一次实际使用**的间隔分钟数。
     *
     * @param configuredMinutes 用户配置的节奏（已夹到 10-120）
     * @param effectiveMinutes 当前生效的间隔（可能因降频高于配置值）
     * @param recentRecords 最近的兑现记录，**按时间从新到旧**
     *
     * 判定顺序（与 AC 一一对应）：
     * 1. 先看是否满足「连续 [ON_TIME_BEFORE_RESET] 次按时」→ 回到配置值（AC-17.3）；
     * 2. 再看尾部是否连续 [SUPPRESSIONS_BEFORE_BACKOFF] 次被压制 → 在当前生效值上 ×2 封顶（AC-17.2）。
     *
     * 只有「真的执行了」的记录参与判定：[AlarmFulfillmentRecord.isSuppressedEvidence] /
     * [AlarmFulfillmentRecord.isOnTimeEvidence] 都要求 `actualAtUtcMillis != null`，
     * 因此暂停、手动会话跳过、关机未执行（AC-17.4 / AC-20.3 / AC-18.3）以及
     * 拉起失败（AC-17.5）都不会推进任何一侧的连续计数。
     */
    fun resolveEffectiveMinutes(
        configuredMinutes: Int,
        effectiveMinutes: Int,
        recentRecords: List<AlarmFulfillmentRecord>
    ): Int {
        val configured = clampConfiguredMinutes(configuredMinutes)

        // 只有「按时 / 被压制」两类是有效证据；其余结果不参与降频判定（AC-17.4 / AC-17.5 / AC-20.3）。
        val evidence = recentRecords.filter { it.isSuppressedEvidence || it.isOnTimeEvidence }

        // AC-17.3：连续 2 次延迟 ≤15 分钟 → 回到配置值。
        val consecutiveOnTime = evidence.takeWhile { it.isOnTimeEvidence }.size
        if (consecutiveOnTime >= ON_TIME_BEFORE_RESET) return configured

        // AC-17.2：连续 3 次被压制 → 在当前生效值上 ×2，且不超过 120 分钟。
        val consecutiveSuppressed = evidence.takeWhile { it.isSuppressedEvidence }.size
        if (consecutiveSuppressed >= SUPPRESSIONS_BEFORE_BACKOFF) {
            val backedOff = maxOf(effectiveMinutes, configured) * BACKOFF_MULTIPLIER
            return backedOff.coerceAtMost(MAX_BACKOFF_INTERVAL_MINUTES)
        }

        // 未触发降频也未触发复位：保持当前生效值（但不得低于配置值）。
        return maxOf(effectiveMinutes, configured)
    }

    /** 状态页是否应显示降频提示（AC-17.2：出现提示 / AC-17.3：提示消失）。 */
    fun isBackedOff(configuredMinutes: Int, effectiveMinutes: Int): Boolean =
        effectiveMinutes > clampConfiguredMinutes(configuredMinutes)

    /** 降频提示文案（AC-26.1：简体中文，术语与既有界面一致）。 */
    fun backoffHint(configuredMinutes: Int, effectiveMinutes: Int): String =
        "检测到闹钟连续被系统压制，已把保活间隔从 $configuredMinutes 分钟临时放宽到 " +
            "$effectiveMinutes 分钟（上限 $MAX_BACKOFF_INTERVAL_MINUTES 分钟）；" +
            "连续 $ON_TIME_BEFORE_RESET 次按时后会回到配置值。"
}
