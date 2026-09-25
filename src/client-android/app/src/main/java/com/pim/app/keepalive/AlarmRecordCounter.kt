package com.pim.app.keepalive

/**
 * 叫醒兑现台账（REQ-18）的内存视图 + 计数推进（REQ-17）。
 *
 * 「预定时刻 / 实际时刻 / 延迟」是需求契约字段（工单 §7.1），因此这里的时间一律是**壁钟毫秒**，
 * 与取证台账、服务端摘要保持同一时间基准。
 *
 * 计数推进规则**逐条对应 AC**，不要在这里再写一套判定：
 * - 被压制（延迟 >15 分钟）→ 压制计数 +1、按时计数清零（AC-17.1 / AC-17.2）
 * - 按时（延迟 ≤15 分钟）→ 按时计数 +1、压制计数清零（AC-17.3）
 * - 拉起失败 → 只推失败计数，**不动**压制/按时计数（AC-17.5 不降频；且失败不是「延迟」证据）
 * - 暂停 / 手动会话跳过 / 关机未执行 → 三类计数都**不动**（AC-17.4 / AC-20.3 / AC-18.3）
 */
object AlarmRecordCounter {

    data class Counters(
        val consecutiveSuppressed: Int,
        val consecutiveOnTime: Int,
        val consecutiveWakeFailures: Int
    )

    fun advance(previous: Counters, record: AlarmFulfillmentRecord): Counters = when {
        record.isSuppressedEvidence -> Counters(
            consecutiveSuppressed = previous.consecutiveSuppressed + 1,
            consecutiveOnTime = 0,
            consecutiveWakeFailures = previous.consecutiveWakeFailures
        )

        record.isOnTimeEvidence -> Counters(
            consecutiveSuppressed = 0,
            consecutiveOnTime = previous.consecutiveOnTime + 1,
            consecutiveWakeFailures = previous.consecutiveWakeFailures
        )

        record.outcome == AlarmOutcomes.PULL_FAILED -> Counters(
            consecutiveSuppressed = previous.consecutiveSuppressed,
            consecutiveOnTime = previous.consecutiveOnTime,
            consecutiveWakeFailures = previous.consecutiveWakeFailures + 1
        )

        else -> previous
    }

    /**
     * 把一组记录（从旧到新）折叠成计数。
     * 用于「从台账重建状态」——进程被杀后重启时计数器必须能被重建，不能只活在内存里。
     */
    fun fold(recordsOldestFirst: List<AlarmFulfillmentRecord>): Counters =
        recordsOldestFirst.fold(Counters(0, 0, 0)) { acc, record -> advance(acc, record) }
}
