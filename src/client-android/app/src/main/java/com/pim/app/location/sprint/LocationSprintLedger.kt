package com.pim.app.location.sprint

import com.pim.app.data.ForensicEventDao
import com.pim.app.data.ForensicEventEntity
import com.pim.app.forensics.ForensicEventTypes
import com.pim.app.mobile.logs.StructuredLogRepository
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CancellationException
import org.json.JSONObject

/**
 * 冲刺台账的设备端写入口（WO-ANDROID-GATE-20260926 REQ-8 / AC-8.1 ~ AC-8.4）。
 *
 * 复用既有取证事件通道 `mobile_forensic_events`（先例 `KeepAliveLedger`）：
 * 上传、幂等、30 天清理、诊断导出这些既有能力直接生效，不必重做一套。
 *
 * **口径纪律（AC-5.4 / AC-14.8）**：
 * - 只有 [SprintOutcome.EXECUTED] 的记录才算「已冲刺」；
 * - 关闭状态下写的是 [SprintOutcome.SKIPPED] 记录，原因见 [SprintSkipReasons]
 *   —— 既满足 AC-8.2（未冲刺原因非空），又不违反 AC-5.4（不得伪造已执行记录）；
 * - 被动点**不**计入本类统计（由 [PassiveLocationLedger] 单独记账）。
 *
 * 负载**只含结构化诊断字段**，不含经纬度（AC-8.3）。
 */
@Singleton
class LocationSprintLedger @Inject constructor(
    private val dao: ForensicEventDao,
    private val logs: StructuredLogRepository
) : SprintLedgerPort {

    /** 写入一条**已执行**的冲刺记录（AC-8.1：字段齐备）。 */
    override suspend fun recordExecuted(result: SprintWindowResult): Boolean = record(
        eventType = LocationSprintEventTypes.SPRINT,
        occurredAtUtcMillis = result.startedAtUtcMillis,
        clientItemKey = sprintKey(result.startedAtUtcMillis, SprintOutcome.EXECUTED, null),
        payload = JSONObject()
            .put("outcome", SprintOutcome.EXECUTED)
            .put("outcomeLabel", SprintOutcome.label(SprintOutcome.EXECUTED))
            .put("sprintStartedAtUtcMillis", result.startedAtUtcMillis)
            .put("sprintEndedAtUtcMillis", result.endedAtUtcMillis)
            .put("sprintDurationMillis", result.durationMillis)
            .put("sprintSampleCount", result.sampleCount)
            .put(
                "sprintBestAccuracyMeters",
                result.bestAccuracyMeters?.toDouble() ?: JSONObject.NULL
            )
            .put("sprintAcceptedCount", result.acceptedCount)
            .put("sprintSkipReason", JSONObject.NULL)
    )

    /**
     * 写入一条**未冲刺**记录（AC-8.2：原因非空）。
     *
     * 注意 AC-5.4：这里写入的是「跳过」而不是「已冲刺」，
     * 台账消费方必须按 `outcome` 区分，不得把跳过计成已冲刺。
     */
    override suspend fun recordSkipped(
        occurredAtUtcMillis: Long,
        reason: String
    ): Boolean = record(
        eventType = LocationSprintEventTypes.SPRINT,
        occurredAtUtcMillis = occurredAtUtcMillis,
        clientItemKey = sprintKey(occurredAtUtcMillis, SprintOutcome.SKIPPED, reason),
        payload = JSONObject()
            .put("outcome", SprintOutcome.SKIPPED)
            .put("outcomeLabel", SprintOutcome.label(SprintOutcome.SKIPPED))
            .put("sprintStartedAtUtcMillis", JSONObject.NULL)
            .put("sprintEndedAtUtcMillis", JSONObject.NULL)
            .put("sprintDurationMillis", JSONObject.NULL)
            .put("sprintSampleCount", JSONObject.NULL)
            .put("sprintBestAccuracyMeters", JSONObject.NULL)
            .put("sprintAcceptedCount", JSONObject.NULL)
            .put("sprintSkipReason", reason)
            .put("sprintSkipReasonLabel", SprintSkipReasons.label(reason))
    )

    /**
     * 最近 24 小时内的**已执行**冲刺次数（REQ-9 / D7）。
     *
     * 只数 `outcome = executed` 的记录：AC-5.3.1 的验收口径是「台账不再新增**已冲刺**记录」，
     * 关闭期间产生的跳过记录不得被算进去。
     */
    suspend fun executedCountSince(fromUtcMillis: Long): Int = try {
        dao.recentByType(LocationSprintEventTypes.SPRINT, EXECUTED_SCAN_LIMIT)
            .count { entity ->
                entity.occurredAtUtc >= fromUtcMillis &&
                    payloadOutcome(entity.payloadJson) == SprintOutcome.EXECUTED
            }
    } catch (ex: CancellationException) {
        throw ex
    } catch (ex: Exception) {
        logs.error("location-sprint", "读取冲刺台账失败：${ex.message ?: ex::class.java.simpleName}", ex)
        0
    }

    /** 最近 24 小时内是否存在任何采集数据（用于 AC-9.2 的「暂无」空态判定）。 */
    suspend fun hasAnyLedgerDataSince(fromUtcMillis: Long): Boolean = try {
        dao.eventsInRange(fromUtcMillis, Long.MAX_VALUE).isNotEmpty()
    } catch (ex: CancellationException) {
        throw ex
    } catch (_: Exception) {
        false
    }

    private suspend fun record(
        eventType: String,
        occurredAtUtcMillis: Long,
        clientItemKey: String,
        payload: JSONObject
    ): Boolean = try {
        dao.insertIgnore(
            ForensicEventEntity(
                eventType = eventType,
                occurredAtUtc = occurredAtUtcMillis,
                clientItemKey = clientItemKey,
                payloadJson = payload.toString()
            )
        ) != -1L
    } catch (ex: CancellationException) {
        throw ex
    } catch (ex: Exception) {
        // 写台账失败不得中断采集（沿用阶段一 AC-3.3 的约定），但必须留可见出口。
        logs.error("location-sprint", "写入冲刺台账失败：${ex.message ?: ex::class.java.simpleName}", ex)
        false
    }

    /**
     * 冲刺记录的幂等键。
     *
     * 已执行记录按「发起时刻」去重（同一窗口只可能有一条）；
     * 跳过记录额外带上原因，避免同一时刻的不同原因互相覆盖。
     */
    fun sprintKey(occurredAtUtcMillis: Long, outcome: String, skipReason: String?): String =
        "sprint-${occurredAtUtcMillis / 1_000L}-$outcome${skipReason?.let { "-$it" } ?: ""}"

    companion object {
        /**
         * 单次查询扫描的台账条数上限。
         *
         * 这不是本地留存上限（本地不设条数上限，靠 30 天时间清理兜底），
         * 而是「读多少条够算 24 小时」：冲刺每周期一条，最长周期 15 分钟
         * → 24 小时最多 96 条；这里给足余量以容纳跳过记录与被动源记录。
         */
        const val EXECUTED_SCAN_LIMIT = 4_096

        /** 冲刺台账的事件类型（供谓词与测试引用，避免字符串散落）。 */
        val SPRINT_EVENT_TYPE = LocationSprintEventTypes.SPRINT

        /** 从台账负载里读出 outcome；无法解析时按「非已执行」处理（保守，不虚报）。 */
        fun payloadOutcome(payloadJson: String): String? = runCatching {
            val root = JSONObject(payloadJson)
            if (root.isNull("outcome")) null else root.optString("outcome").takeIf { it.isNotBlank() }
        }.getOrNull()
    }
}

/**
 * 被动定位的计数台账（WO-ANDROID-GATE-20260926 REQ-14 / AC-14.2 / AC-14.3）。
 *
 * AC-14.2 / AC-14.3 要求对账三个数：**被动回调总数（去重前）/ 入库数 / 丢弃记录数**，
 * 且**缺口 = 0**。入库数与丢弃数分别落在点表 `source = passive` 与丢弃诊断表
 * （原因编码 `passive-*`），回调总数没有落点 —— 因此按周期写一条摘要事件，
 * 让验收方能在台账里直接读出分母。
 */
@Singleton
class PassiveLocationLedger @Inject constructor(
    private val dao: ForensicEventDao,
    private val logs: StructuredLogRepository
) {

    /** 写入一条被动回调计数摘要。 */
    suspend fun recordCounters(
        occurredAtUtcMillis: Long,
        windowStartUtcMillis: Long,
        callbackCount: Int,
        acceptedCount: Int,
        droppedCount: Int,
        duplicateCount: Int
    ): Boolean = try {
        val payload = JSONObject()
            .put("windowStartUtcMillis", windowStartUtcMillis)
            .put("passiveCallbackCount", callbackCount)
            .put("passiveAcceptedCount", acceptedCount)
            .put("passiveDroppedCount", droppedCount)
            .put("passiveDuplicateCount", duplicateCount)
            // 三数对账缺口（AC-14.2）：回调 = 入库 + 丢弃 + 重复 时必须为 0。
            .put(
                "passiveUnaccountedCount",
                callbackCount - acceptedCount - droppedCount - duplicateCount
            )
        dao.insertIgnore(
            ForensicEventEntity(
                eventType = PassiveLocationEventTypes.PASSIVE_COUNTER,
                occurredAtUtc = occurredAtUtcMillis,
                clientItemKey = "passive-counter-${windowStartUtcMillis / 1_000L}",
                payloadJson = payload.toString()
            )
        ) != -1L
    } catch (ex: CancellationException) {
        throw ex
    } catch (ex: Exception) {
        logs.error(
            "passive-location",
            "写入被动定位计数失败：${ex.message ?: ex::class.java.simpleName}",
            ex
        )
        false
    }
}
