package com.pim.app.keepalive

/**
 * 兑现率（REQ-18）。
 *
 * 口径（必须与页面说明一致，AC-18.3 要求页面写明）：
 * - **分母** = 有实际时刻的记录数。缺少实际时刻的记录（设备关机未执行、被暂停跳过）
 *   **不计入分母**——把「没执行」算成「未兑现」会把设备关机误报成保活失败。
 * - **分子** = 其中延迟 ≤15 分钟（即按时）的记录数。
 * - 没有任何可判定记录时返回 null，而不是 0%：AC-18.2 明确「若该展示项未启用，页面不得显示为 0%」，
 *   没有数据与 0% 是两件事。
 */
object FulfillmentRate {

    /** 页面上的口径说明原文（AC-18.3：页面需说明该口径）。 */
    const val DEFINITION =
        "兑现率：在统计区间内，实际执行且延迟不超过 " +
            "${AlarmSuppressionPolicy.SUPPRESSION_THRESHOLD_MINUTES} 分钟的叫醒次数 ÷ " +
            "有实际执行时刻的叫醒次数。设备关机或用户暂停导致的未执行不计入分母。"

    data class Result(
        val rate: Double?,
        val fulfilled: Int,
        val considered: Int,
        val excludedNoActualTime: Int
    )

    /** 按上述口径计算。 */
    fun compute(records: List<AlarmFulfillmentRecord>): Result {
        val withActual = records.filter { it.actualAtUtcMillis != null }
        val fulfilled = withActual.count { it.isOnTimeEvidence }
        return Result(
            rate = if (withActual.isEmpty()) null else fulfilled.toDouble() / withActual.size,
            fulfilled = fulfilled,
            considered = withActual.size,
            excludedNoActualTime = records.size - withActual.size
        )
    }

    /** 页面展示文案；无数据时给明确空态而不是 0%（AC-18.2）。 */
    fun format(result: Result): String {
        val rate = result.rate ?: return "无数据（区间内没有已执行的叫醒）"
        val percent = (rate * 1000).toInt() / 10.0
        return "$percent%（${result.fulfilled}/${result.considered} 次按时）"
    }
}
