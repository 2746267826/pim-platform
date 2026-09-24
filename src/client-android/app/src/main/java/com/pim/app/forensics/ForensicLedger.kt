package com.pim.app.forensics

import com.pim.app.data.ForensicEventDao
import com.pim.app.data.ForensicEventEntity
import com.pim.app.data.MobileSyncStatus
import com.pim.app.mobile.logs.StructuredLogRepository
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CancellationException

/**
 * 取证台账（REQ-1 ~ REQ-6）的设备端唯一写入口。
 *
 * 约定：
 * - 本地**不设条数上限**（R4-P3），靠 30 天时间清理兜底（AC-6.2）。
 * - 幂等键由 [clientItemKey] 决定：心跳按秒去重（AC-3.3），退出/强停按（类型 + 时刻 + 原因）去重
 *   （`ApplicationExitInfo` 没有唯一 ID，平台依据要求实现方自行去重）。
 * - **写台账失败不影响采集与同步**（AC-3.3）：所有写入都吞掉异常并转成结构化日志。
 */
@Singleton
class ForensicLedger @Inject constructor(
    private val dao: ForensicEventDao,
    private val logs: StructuredLogRepository
) {
    /** 时间清理窗口：30 天（AC-6.2）。 */
    val retentionMillis: Long = RETENTION_DAYS * 24L * 60L * 60L * 1000L

    /**
     * 写入一条心跳。同一秒内多次唤醒只产生一条（AC-3.3）。
     *
     * @return true 表示本次确实写入了一条新记录。
     */
    suspend fun recordHeartbeat(
        occurredAtUtcMillis: Long,
        payloadJson: String
    ): Boolean = record(
        eventType = ForensicEventTypes.HEARTBEAT,
        occurredAtUtcMillis = occurredAtUtcMillis,
        clientItemKey = heartbeatKey(occurredAtUtcMillis),
        payloadJson = payloadJson
    )

    /** 写入一条进程退出记录（REQ-1）。 */
    suspend fun recordProcessExit(
        occurredAtUtcMillis: Long,
        reason: String,
        payloadJson: String
    ): Boolean = record(
        eventType = ForensicEventTypes.PROCESS_EXIT,
        occurredAtUtcMillis = occurredAtUtcMillis,
        clientItemKey = exitKey(occurredAtUtcMillis, reason),
        payloadJson = payloadJson
    )

    /** 写入一条强停 / 重启记录（REQ-2）。 */
    suspend fun recordForceStop(
        occurredAtUtcMillis: Long,
        kind: String,
        payloadJson: String
    ): Boolean = record(
        eventType = ForensicEventTypes.FORCE_STOP,
        occurredAtUtcMillis = occurredAtUtcMillis,
        clientItemKey = forceStopKey(occurredAtUtcMillis, kind),
        payloadJson = payloadJson
    )

    private suspend fun record(
        eventType: String,
        occurredAtUtcMillis: Long,
        clientItemKey: String,
        payloadJson: String
    ): Boolean {
        return try {
            val rowId = dao.insertIgnore(
                ForensicEventEntity(
                    eventType = eventType,
                    occurredAtUtc = occurredAtUtcMillis,
                    clientItemKey = clientItemKey,
                    payloadJson = payloadJson
                )
            )
            rowId != -1L
        } catch (ex: CancellationException) {
            throw ex
        } catch (ex: Exception) {
            // AC-3.3 / REQ-28：写台账失败不得中断采集与同步，但必须留下可见出口。
            logs.error("forensics", "写入取证台账失败：${ex.message ?: ex::class.java.simpleName}", ex)
            false
        }
    }

    /** 30 天时间清理（AC-6.2）：超过 30 天的条目不论是否已上传都移除。 */
    suspend fun purgeExpired(nowUtcMillis: Long): Int {
        return try {
            dao.deleteOlderThan(nowUtcMillis - retentionMillis)
        } catch (ex: CancellationException) {
            throw ex
        } catch (ex: Exception) {
            logs.error("forensics", "取证台账时间清理失败：${ex.message ?: ex::class.java.simpleName}", ex)
            0
        }
    }

    suspend fun pendingCount(): Int = try {
        dao.countBySyncStatus(MobileSyncStatus.PENDING)
    } catch (ex: CancellationException) {
        throw ex
    } catch (_: Exception) {
        0
    }

    suspend fun totalCount(): Int = try {
        dao.totalCount()
    } catch (ex: CancellationException) {
        throw ex
    } catch (_: Exception) {
        0
    }

    suspend fun markSynced(keys: List<String>) {
        if (keys.isEmpty()) return
        try {
            dao.updateSyncStatusByKeys(keys, MobileSyncStatus.SYNCED)
        } catch (ex: CancellationException) {
            throw ex
        } catch (ex: Exception) {
            logs.error("forensics", "标记取证事件已同步失败：${ex.message ?: ex::class.java.simpleName}", ex)
        }
    }

    suspend fun markFailed(keys: List<String>, message: String?) {
        if (keys.isEmpty()) return
        try {
            dao.updateSyncStatusByKeys(keys, MobileSyncStatus.PENDING, message)
        } catch (ex: CancellationException) {
            throw ex
        } catch (ex: Exception) {
            logs.error("forensics", "标记取证事件待重试失败：${ex.message ?: ex::class.java.simpleName}", ex)
        }
    }

    suspend fun eventsInRange(fromUtcMillis: Long, toUtcMillis: Long): List<ForensicEventEntity> =
        try {
            dao.eventsInRange(fromUtcMillis, toUtcMillis)
        } catch (ex: CancellationException) {
            throw ex
        } catch (_: Exception) {
            emptyList()
        }

    suspend fun lastHeartbeatAtUtc(): Long? = try {
        dao.recentByType(ForensicEventTypes.HEARTBEAT, 1).firstOrNull()?.occurredAtUtc
    } catch (ex: CancellationException) {
        throw ex
    } catch (_: Exception) {
        null
    }

    /** 最近一次死因记录（REQ-8「最近死因」）：进程退出或强停，取时刻最新的一条。 */
    suspend fun latestCauseEvent(): ForensicEventEntity? = try {
        dao.recentByTypes(
            listOf(ForensicEventTypes.PROCESS_EXIT, ForensicEventTypes.FORCE_STOP),
            1
        ).firstOrNull()
    } catch (ex: CancellationException) {
        throw ex
    } catch (_: Exception) {
        null
    }

    /** 心跳幂等键：同一秒内只允许一条（AC-3.3）。 */
    fun heartbeatKey(occurredAtUtcMillis: Long): String =
        "heartbeat-${occurredAtUtcMillis / 1000L}"

    /** 退出记录幂等键：`ApplicationExitInfo` 无唯一 ID，按（时刻秒 + 原因）去重。 */
    fun exitKey(occurredAtUtcMillis: Long, reason: String): String =
        "exit-${occurredAtUtcMillis / 1000L}-$reason"

    /** 强停/重启幂等键：同一秒同一检测结果只记一条。 */
    fun forceStopKey(occurredAtUtcMillis: Long, kind: String): String =
        "forcestop-${occurredAtUtcMillis / 1000L}-$kind"

    companion object {
        /** 时间清理窗口：30 天（AC-6.2 / R4-P3）。 */
        const val RETENTION_DAYS = 30L
    }
}
