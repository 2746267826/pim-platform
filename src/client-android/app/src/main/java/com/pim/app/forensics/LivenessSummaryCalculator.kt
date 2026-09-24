package com.pim.app.forensics

import java.util.Locale

/** 静默标色（AC-7.4）：≥30 分钟警告、≥1 小时严重、<30 分钟不标记。 */
enum class SilenceSeverity {
    None,
    Warning,
    Critical;

    val label: String
        get() = when (this) {
            Critical -> "严重（≥1 小时）"
            Warning -> "警告（≥30 分钟）"
            None -> "未达标记线"
        }
}

/** 一段静默。 */
data class SilenceSpan(
    val startUtcMillis: Long,
    val endUtcMillis: Long,
    val minutes: Double,
    val severity: SilenceSeverity
)

/** 死因计数（客户端侧；只用于本地展示的「最近死因」，Web 侧由服务端统一翻译）。 */
data class CauseCount(val label: String, val count: Int)

/**
 * 设备端存活摘要（REQ-8 / REQ-13）。
 *
 * 口径与服务端 `Pim.Core.Liveness.DeviceLivenessCalculator` **完全一致**：
 * 静默判定线只有一条（30 分钟），夜间与白天同一套代码（AC-13.2）；
 * 覆盖率给出两个口径并暴露分子/分母，可被复算（AC-13.1）。
 */
data class DeviceLivenessSummary(
    val hasData: Boolean,
    val conclusion: String,
    val coverageByHour: Double?,
    val coverageByExpectedHeartbeat: Double?,
    val observedHours: Int,
    val totalHours: Int,
    val observedHeartbeats: Int,
    val expectedHeartbeats: Int,
    val longestSilenceMinutes: Int,
    val longestSilenceSeverity: SilenceSeverity,
    val lastHeartbeatAtUtcMillis: Long?,
    val lastCauseLabel: String?
)

/** 存活判定口径常量。 */
object LivenessRules {
    const val SILENCE_WARNING_MINUTES = 30
    const val SILENCE_CRITICAL_MINUTES = 60

    /**
     * 「按应有心跳」覆盖率的分母节奏（分钟）：取既有的周期同步节奏（15 分钟），
     * 不新引入轮询、也不自拟档位（AC-29.1 / AC-10.3）。
     */
    const val EXPECTED_HEARTBEAT_INTERVAL_MINUTES = 15

    const val NO_DATA_CONCLUSION = "无数据/未上报：所选区间内没有存活心跳。"

    const val COVERAGE_BY_HOUR_DEFINITION =
        "按小时覆盖率：有存活心跳的整点小时数 ÷ 区间小时数。"
    const val COVERAGE_BY_EXPECTED_HEARTBEAT_DEFINITION =
        "按应有心跳覆盖率：区间内心跳条数 ÷ 应有心跳条数（区间分钟数 ÷ 15，向上取整），上限 100%。"
}

/**
 * 设备端存活摘要计算（纯函数）。输入是心跳时刻（UTC 毫秒）与服务端下发不参与计算，
 * 因此可以用固定时钟在 JVM 单元测试里复算（AC-13.1 / AC-13.2）。
 */
object LivenessSummaryCalculator {

    fun summarize(
        heartbeatTimestampsUtcMillis: List<Long>,
        rangeStartUtcMillis: Long,
        rangeEndUtcMillis: Long,
        lastCauseLabel: String? = null
    ): DeviceLivenessSummary {
        require(rangeEndUtcMillis > rangeStartUtcMillis) { "区间终点必须晚于起点。" }

        val heartbeats = heartbeatTimestampsUtcMillis
            .filter { it >= rangeStartUtcMillis && it < rangeEndUtcMillis }
            .sorted()

        val totalHours = countHourBuckets(rangeStartUtcMillis, rangeEndUtcMillis)
        val observedHours = minOf(
            totalHours,
            heartbeats.map { floorToHour(it) }.distinct().size
        )

        val totalMinutes = (rangeEndUtcMillis - rangeStartUtcMillis).toDouble() / 60000.0
        val expectedHeartbeats = maxOf(
            1,
            kotlin.math.ceil(totalMinutes / LivenessRules.EXPECTED_HEARTBEAT_INTERVAL_MINUTES).toInt()
        )

        val silences = buildSilences(heartbeats, rangeStartUtcMillis, rangeEndUtcMillis)
        val longest = silences.maxByOrNull { it.minutes }

        val hasData = heartbeats.isNotEmpty()

        return DeviceLivenessSummary(
            hasData = hasData,
            conclusion = buildConclusion(hasData, longest, silences.size),
            coverageByHour = if (hasData) round4(observedHours.toDouble() / totalHours) else null,
            coverageByExpectedHeartbeat = if (hasData) {
                round4(minOf(1.0, heartbeats.size.toDouble() / expectedHeartbeats))
            } else {
                null
            },
            observedHours = observedHours,
            totalHours = totalHours,
            observedHeartbeats = heartbeats.size,
            expectedHeartbeats = expectedHeartbeats,
            longestSilenceMinutes = longest?.minutes?.toInt() ?: 0,
            longestSilenceSeverity = longest?.severity ?: SilenceSeverity.None,
            lastHeartbeatAtUtcMillis = heartbeats.lastOrNull(),
            lastCauseLabel = lastCauseLabel
        )
    }

    /** 只按静默时长标色，**不看**它发生在夜间还是白天（AC-13.2）。 */
    fun classifySilence(minutes: Double): SilenceSeverity = when {
        minutes >= LivenessRules.SILENCE_CRITICAL_MINUTES -> SilenceSeverity.Critical
        minutes >= LivenessRules.SILENCE_WARNING_MINUTES -> SilenceSeverity.Warning
        else -> SilenceSeverity.None
    }

    /**
     * 静默时段：起点→第一跳、相邻两跳之间、最后一跳→终点。
     * 只有 ≥30 分钟的缺口才会出现在结果里（AC-7.4）。
     */
    fun buildSilences(
        sortedHeartbeats: List<Long>,
        rangeStartUtcMillis: Long,
        rangeEndUtcMillis: Long
    ): List<SilenceSpan> {
        val spans = mutableListOf<SilenceSpan>()

        if (sortedHeartbeats.isEmpty()) {
            addSpan(spans, rangeStartUtcMillis, rangeEndUtcMillis)
            return spans
        }

        addSpan(spans, rangeStartUtcMillis, sortedHeartbeats.first())
        for (index in 1 until sortedHeartbeats.size) {
            addSpan(spans, sortedHeartbeats[index - 1], sortedHeartbeats[index])
        }
        addSpan(spans, sortedHeartbeats.last(), rangeEndUtcMillis)
        return spans
    }

    private fun addSpan(spans: MutableList<SilenceSpan>, startUtcMillis: Long, endUtcMillis: Long) {
        if (endUtcMillis <= startUtcMillis) return
        val minutes = (endUtcMillis - startUtcMillis).toDouble() / 60000.0
        if (minutes < LivenessRules.SILENCE_WARNING_MINUTES) return
        spans.add(SilenceSpan(startUtcMillis, endUtcMillis, minutes, classifySilence(minutes)))
    }

    private fun buildConclusion(
        hasData: Boolean,
        longest: SilenceSpan?,
        silenceCount: Int
    ): String {
        if (!hasData) {
            // AC-8.2：离线且无本地数据时显示"无数据"，不得给出"无异常"结论。
            return LivenessRules.NO_DATA_CONCLUSION
        }
        if (longest == null) {
            return "存活连续：所选区间内没有 ≥30 分钟的静默。"
        }
        val minutes = longest.minutes.toInt()
        return if (longest.severity == SilenceSeverity.Critical) {
            String.format(
                Locale.US,
                "存活有缺口：最长静默 %d 分钟，区间内共 %d 段 ≥30 分钟静默。",
                minutes,
                silenceCount
            )
        } else {
            String.format(
                Locale.US,
                "存活有短暂中断：最长静默 %d 分钟（未达 1 小时）。",
                minutes
            )
        }
    }

    /**
     * 区间覆盖多少个整点小时桶（左闭右开）。
     * 用 `end - 1ms` 取最后一桶，避免"终点正好落在整点"时把区间之外的那个小时也算进分母。
     */
    private fun countHourBuckets(rangeStartUtcMillis: Long, rangeEndUtcMillis: Long): Int {
        if (rangeEndUtcMillis <= rangeStartUtcMillis) return 1
        val firstBucket = floorToHour(rangeStartUtcMillis)
        val lastBucket = floorToHour(rangeEndUtcMillis - 1L)
        val hours = ((lastBucket - firstBucket) / 3_600_000L).toInt() + 1
        return maxOf(1, hours)
    }

    private fun floorToHour(utcMillis: Long): Long =
        (utcMillis / 3_600_000L) * 3_600_000L

    private fun round4(value: Double): Double =
        kotlin.math.round(value * 10000.0) / 10000.0
}

/** 把覆盖率格式化成页面上可核对的形式（同时给出分子/分母，AC-13.1）。 */
object CoverageFormatting {
    fun format(coverage: Double?): String =
        if (coverage == null) "—" else String.format(Locale.US, "%.1f%%", coverage * 100.0)

    fun formatByHour(summary: DeviceLivenessSummary): String =
        "${format(summary.coverageByHour)}（${summary.observedHours}/${summary.totalHours} 小时）"

    fun formatByExpectedHeartbeat(summary: DeviceLivenessSummary): String =
        "${format(summary.coverageByExpectedHeartbeat)}" +
            "（${summary.observedHeartbeats}/${summary.expectedHeartbeats} 次）"
}
