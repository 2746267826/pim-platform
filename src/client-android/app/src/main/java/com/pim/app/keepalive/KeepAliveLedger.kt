package com.pim.app.keepalive

import com.pim.app.data.ForensicEventDao
import com.pim.app.data.ForensicEventEntity
import com.pim.app.forensics.ForensicEventTypes
import com.pim.app.mobile.logs.StructuredLogRepository
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CancellationException
import org.json.JSONObject

/**
 * 保活叫醒的台账（REQ-18 兑现台账 + REQ-14 权限事件 + REQ-21 红点依据）。
 *
 * 复用阶段一的取证事件通道（`mobile_forensic_events`）而不是另建一张表：
 * - 上传、幂等、30 天清理、诊断导出这些既有能力直接生效，不必重做一套（AC-29.1 也要求不新增轮询/通道）；
 * - 事件类型需要在两端契约里登记，否则服务端会按 REQ-28 显式拒绝
 *   （`MobileForensicIngestService.KnownEventTypes`）。
 *
 * 与 [com.pim.app.forensics.ForensicLedger] 的分工：那个类管阶段一的三种事件，
 * 本类管阶段二新增的三种，避免改动阶段一已验证的写入口。
 */
@Singleton
class KeepAliveLedger @Inject constructor(
    private val dao: ForensicEventDao,
    private val logs: StructuredLogRepository
) : AlarmFulfillmentRecorder {
    /** 写入一条叫醒兑现记录（REQ-18）。 */
    override suspend fun recordFulfillment(record: AlarmFulfillmentRecord): Boolean = record(
        eventType = KeepAliveEventTypes.ALARM_FULFILLMENT,
        occurredAtUtcMillis = record.actualAtUtcMillis ?: record.scheduledAtUtcMillis,
        clientItemKey = fulfillmentKey(record),
        payloadJson = JSONObject()
            .put("scheduledAtUtcMillis", record.scheduledAtUtcMillis)
            .put("actualAtUtcMillis", record.actualAtUtcMillis ?: JSONObject.NULL)
            .put("delayMillis", record.delayMillis ?: JSONObject.NULL)
            .put("outcome", record.outcome)
            .put("outcomeLabel", AlarmOutcomes.label(record.outcome))
            .toString()
    )

    /** 写入一条闹钟登记事件（AC-14.1：已授权状态下台账出现闹钟登记事件）。 */
    suspend fun recordAlarmRegistered(
        intervalMinutes: Int,
        triggerAtUtcMillis: Long,
        trigger: String
    ): Boolean = record(
        eventType = KeepAliveEventTypes.ALARM_REGISTERED,
        occurredAtUtcMillis = triggerAtUtcMillis,
        clientItemKey = "alarm-registered-${triggerAtUtcMillis}",
        payloadJson = JSONObject()
            .put("intervalMinutes", intervalMinutes)
            .put("triggerAtUtcMillis", triggerAtUtcMillis)
            .put("trigger", trigger)
            .toString()
    )

    /** 写入一条保活健康事件（REQ-21 红点依据：权限撤销 / 闹钟被清空 / 连续失败）。 */
    suspend fun recordHealthEvent(
        reason: String,
        occurredAtUtcMillis: Long,
        detail: String?
    ): Boolean {
        val payload = JSONObject()
            .put("reason", reason)
            .put("reasonLabel", KeepAliveHealthReasons.label(reason))
        if (!detail.isNullOrBlank()) payload.put("detail", detail)
        return record(
            eventType = KeepAliveEventTypes.KEEPALIVE_HEALTH,
            occurredAtUtcMillis = occurredAtUtcMillis,
            clientItemKey = "keepalive-health-${occurredAtUtcMillis / 1000L}-$reason",
            payloadJson = payload.toString()
        )
    }

    /**
     * 读取最近 [limit] 条叫醒记录（**从新到旧**），供降频判定与兑现率使用。
     *
     * 默认条数由**降频判定规则本身**推导（见 [KeepAliveLedger.requiredRecordsForSuppressionPolicy]），
     * 而不是一个自拟的「最多看 N 条」窗口：窗口太小会让「连续 3 次被压制」这类判定
     * 因为看不到足够证据而永不触发。§9.1 P3 的不设上限约束的是本地留存，
     * 这里只是**读多少条够判定**，两者不是一回事，但仍必须由规则决定而不是拍一个数。
     */
    suspend fun recentFulfillments(
        limit: Int = requiredRecordsForSuppressionPolicy()
    ): List<AlarmFulfillmentRecord> = try {
        dao.recentByType(KeepAliveEventTypes.ALARM_FULFILLMENT, limit).mapNotNull { entity ->
            runCatching {
                val root = JSONObject(entity.payloadJson)
                AlarmFulfillmentRecord(
                    scheduledAtUtcMillis = root.getLong("scheduledAtUtcMillis"),
                    actualAtUtcMillis = if (root.isNull("actualAtUtcMillis")) {
                        null
                    } else {
                        root.getLong("actualAtUtcMillis")
                    },
                    outcome = root.optString("outcome").ifBlank { AlarmOutcomes.NOT_EXECUTED }
                )
            }.getOrNull()
        }
    } catch (ex: CancellationException) {
        throw ex
    } catch (ex: Exception) {
        logs.warn("keepalive", "读取叫醒台账失败：${ex.message ?: ""}")
        emptyList()
    }

    /** 读取指定区间内的叫醒记录（**从旧到新**），供兑现率展示使用。 */
    suspend fun fulfillmentsInRange(fromUtcMillis: Long, toUtcMillis: Long): List<AlarmFulfillmentRecord> =
        try {
            dao.eventsInRange(fromUtcMillis, toUtcMillis)
                .filter { it.eventType == KeepAliveEventTypes.ALARM_FULFILLMENT }
                .mapNotNull { entity ->
                    runCatching {
                        val root = JSONObject(entity.payloadJson)
                        AlarmFulfillmentRecord(
                            scheduledAtUtcMillis = root.getLong("scheduledAtUtcMillis"),
                            actualAtUtcMillis = if (root.isNull("actualAtUtcMillis")) {
                                null
                            } else {
                                root.getLong("actualAtUtcMillis")
                            },
                            outcome = root.optString("outcome").ifBlank { AlarmOutcomes.NOT_EXECUTED }
                        )
                    }.getOrNull()
                }
        } catch (ex: CancellationException) {
            throw ex
        } catch (ex: Exception) {
            logs.warn("keepalive", "读取区间叫醒记录失败：${ex.message ?: ""}")
            emptyList()
        }

    /** 最近一次健康事件（红点原因）；没有则 null。 */
    suspend fun latestHealthEvent(): Pair<String, Long>? = try {
        dao.recentByType(KeepAliveEventTypes.KEEPALIVE_HEALTH, 1).firstOrNull()?.let { entity ->
            val reason = runCatching { JSONObject(entity.payloadJson).optString("reason") }.getOrNull()
            if (reason.isNullOrBlank()) null else reason to entity.occurredAtUtc
        }
    } catch (ex: CancellationException) {
        throw ex
    } catch (ex: Exception) {
        null
    }

    /** 最近一次叫醒时刻（REQ-19：通知内容更新为最近一次叫醒时间）。 */
    suspend fun lastFulfillmentAtUtc(): Long? = recentFulfillments(1).firstOrNull()
        ?.let { it.actualAtUtcMillis ?: it.scheduledAtUtcMillis }

    private suspend fun record(
        eventType: String,
        occurredAtUtcMillis: Long,
        clientItemKey: String,
        payloadJson: String
    ): Boolean = try {
        dao.insertIgnore(
            ForensicEventEntity(
                eventType = eventType,
                occurredAtUtc = occurredAtUtcMillis,
                clientItemKey = clientItemKey,
                payloadJson = payloadJson
            )
        ) != -1L
    } catch (ex: CancellationException) {
        throw ex
    } catch (ex: Exception) {
        // REQ-28：写台账失败不得影响叫醒与采集，但必须留下可见出口。
        logs.error("keepalive", "写入保活台账失败：${ex.message ?: ex::class.java.simpleName}", ex)
        false
    }

    private fun fulfillmentKey(record: AlarmFulfillmentRecord): String =
        "alarm-fulfillment-${record.scheduledAtUtcMillis}-${record.outcome}"

    companion object {
        /**
         * 降频判定需要读取的最少记录条数。
         *
         * 取两侧门槛的较大者再加 1 条余量：判定看的是「连续前缀」，
         * 因此至少要能看到 [AlarmSuppressionPolicy.SUPPRESSIONS_BEFORE_BACKOFF] 条
         * 或 [AlarmSuppressionPolicy.ON_TIME_BEFORE_RESET] 条，多读 1 条用于判断前缀是否被打断。
         *
         * 这里**刻意不设**一个自拟的「最多看 N 条」窗口：§9.1 P3 明确本地队列不设条数上限，
         * 而窗口太小会让「连续 3 次被压制」因看不到足够证据而永不触发。
         * 上述数值全部来自工单已确认参数，不含自拟值。
         */
        fun requiredRecordsForSuppressionPolicy(): Int =
            maxOf(
                AlarmSuppressionPolicy.SUPPRESSIONS_BEFORE_BACKOFF,
                AlarmSuppressionPolicy.ON_TIME_BEFORE_RESET
            ) + 1
    }
}

/**
 * 阶段二新增的事件类型（工单 §7.1「闹钟兑现」行）。
 *
 * 这**不是**凭空扩展契约：`mobile_forensic_events` 的 `event_type` 需要与
 * `Pim.Core.Liveness.ForensicEventTypes` 及 `MobileForensicIngestService.KnownEventTypes` 三处一致，
 * 服务端对未登记类型会显式拒绝（REQ-28）。
 */
object KeepAliveEventTypes {
    /** 闹钟兑现（预定/实际/延迟/结果）。 */
    const val ALARM_FULFILLMENT = "alarm-fulfillment"

    /** 闹钟登记（AC-14.1 要求台账出现登记事件）。 */
    const val ALARM_REGISTERED = "alarm-registered"

    /** 保活健康事件（REQ-21 红点双通道的依据）。 */
    const val KEEPALIVE_HEALTH = "keepalive-health"

    val ALL = setOf(ALARM_FULFILLMENT, ALARM_REGISTERED, KEEPALIVE_HEALTH)

    fun label(eventType: String): String = when (eventType) {
        ALARM_FULFILLMENT -> "闹钟兑现"
        ALARM_REGISTERED -> "闹钟登记"
        KEEPALIVE_HEALTH -> "保活健康"
        else -> ForensicEventTypes.label(eventType)
    }
}
