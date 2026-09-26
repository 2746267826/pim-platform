package com.pim.app.location.sprint

import com.pim.app.settings.TrackingSettingsStore
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 状态页「最近 24 小时冲刺次数」的展示口径（REQ-9 / AC-9.2）。
 *
 * 两态必须分开（AC-9.2 明令分两态）：
 * - [Empty]：最近 24 小时**无任何采集数据** → 显示「暂无」；
 * - [Value]：**有采集数据** → 如实显示次数（含 0 次）。
 *
 * 之所以不用「次数 == 0 就是暂无」，是因为那会让「冲刺关了、采集还在跑」与
 * 「压根没采集」在页面上长得一样 —— 验收方复验 AC-5.3 时正是靠这个区分。
 */
sealed interface SprintCountDisplay {
    /** 无采集数据：显示「暂无（最近 24 小时无采集数据）」。 */
    data object Empty : SprintCountDisplay

    /** 有采集数据：显示「N 次」（N 可以为 0）。 */
    data class Value(val count: Int) : SprintCountDisplay
}

/** 状态页冲刺区块的展示数据（AC-9.1 / AC-9.2）。 */
data class SprintSummary(
    /** 冲刺开关当前状态（AC-9.2：空态也必须展示）。 */
    val enabled: Boolean,
    /** 最近 24 小时已执行冲刺次数；无采集数据时为 null（与「0 次」区分）。 */
    val count: Int?,
    val countDisplay: SprintCountDisplay
)

/**
 * 冲刺概况读取器（REQ-9 / D7：统计窗口 = 最近 24 小时）。
 *
 * 口径 = **设备端本地台账**，不新增接口（REQ-9 明文）。
 */
@Singleton
class LocationSprintSummaryRepository @Inject constructor(
    private val ledger: com.pim.app.location.sprint.LocationSprintLedger,
    private val trackingSettingsStore: TrackingSettingsStore,
    private val nowUtcMillis: () -> Long = System::currentTimeMillis
) {
    suspend fun read(): SprintSummary {
        val now = nowUtcMillis()
        val windowStart = now - WINDOW_MILLIS
        val enabled = trackingSettingsStore.read().sprintEnabled

        val hasAnyData = ledger.hasAnyLedgerDataSince(windowStart)
        if (!hasAnyData) {
            // AC-9.2 态一：无采集数据 → 「暂无」。
            return SprintSummary(enabled = enabled, count = null, countDisplay = SprintCountDisplay.Empty)
        }

        // AC-9.2 态二 / AC-9.1：有采集数据 → 如实显示已执行次数（可以为 0）。
        val count = ledger.executedCountSince(windowStart)
        return SprintSummary(
            enabled = enabled,
            count = count,
            countDisplay = SprintCountDisplay.Value(count)
        )
    }

    companion object {
        /** 统计窗口：最近 24 小时（D7）。 */
        const val WINDOW_MILLIS = 24L * 60L * 60L * 1_000L
    }
}
