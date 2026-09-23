package com.pim.app.forensics

import android.content.Context
import android.os.Build
import android.provider.Settings
import com.pim.app.data.DroppedDiagnosticExportRow
import com.pim.app.data.ForensicEventDao
import com.pim.app.data.MobileSyncStatus
import com.pim.app.mobile.logs.StructuredLogRepository
import com.pim.app.mobile.sync.sha256Hex
import com.pim.core.models.MobileDroppedReasonSummaryDto
import com.pim.core.models.MobileForensicEventDto
import com.pim.core.models.MobileForensicsUploadRequest
import com.pim.core.network.ApiService
import com.pim.core.util.toCauseChainMessage
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.Instant
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CancellationException

/**
 * 取证数据上传（REQ-5 / REQ-9）。
 *
 * - 在**既有同步周期内顺带上传**，不新增任何轮询/闹钟（AC-29.1）。
 * - 断网期间事件留在本地，恢复后按队列补传，直到服务端条数与设备端一致（AC-5.1）。
 * - 上报失败**不阻塞采集与既有同步**：异常被收敛成日志与台账里的错误字段（AC-5.3）。
 * - 幂等由设备端 `clientItemKey` 与服务端唯一键共同保证（AC-5.2）。
 */
@Singleton
class ForensicUploadCoordinator internal constructor(
    @ApplicationContext private val context: Context,
    private val api: ApiService,
    private val dao: ForensicEventDao,
    private val logs: StructuredLogRepository,
    private val sentinelStore: ForensicSentinelStore,
    private val nowUtcMillis: () -> Long
) {
    @Inject
    constructor(
        @ApplicationContext context: Context,
        api: ApiService,
        dao: ForensicEventDao,
        logs: StructuredLogRepository,
        sentinelStore: ForensicSentinelStore
    ) : this(context, api, dao, logs, sentinelStore, System::currentTimeMillis)

    /** 单批上限与服务端一致，避免整批被拒。 */
    val batchLimit: Int = BATCH_LIMIT

    /**
     * 上传一批待传事件与丢弃原因统计。
     *
     * @return 本次实际上传的事件条数（含服务端跳过的重复条目）。
     */
    suspend fun uploadPending(): Int {
        val pending = try {
            dao.pendingEvents(MobileSyncStatus.PENDING, batchLimit)
        } catch (ex: CancellationException) {
            throw ex
        } catch (ex: Exception) {
            logs.warn("forensics-sync", "读取待传取证事件失败：${ex.message ?: ""}")
            return 0
        }

        val summaries = buildDroppedReasonSummaries()
        if (pending.isEmpty() && summaries.isEmpty()) {
            return 0
        }

        val deviceId = deviceId()
        val batchId = "android-forensics-" + sha256Hex(
            "$deviceId|${pending.joinToString(",") { it.clientItemKey }}|${summaries.size}"
        ).take(24)

        val request = MobileForensicsUploadRequest(
            deviceId = deviceId,
            batchId = batchId,
            events = pending.map { entity ->
                MobileForensicEventDto(
                    clientItemKey = entity.clientItemKey,
                    eventType = entity.eventType,
                    occurredAtUtc = iso(entity.occurredAtUtc),
                    payloadJson = entity.payloadJson
                )
            },
            droppedReasonSummaries = summaries
        )

        val response = try {
            api.uploadMobileForensics(request)
        } catch (ex: CancellationException) {
            throw ex
        } catch (ex: Exception) {
            val detail = ex.toCauseChainMessage()
            markFailed(pending.map { it.clientItemKey }, detail)
            logs.warn("forensics-sync", "取证事件上报失败（本地留存待补传）：$detail")
            return 0
        }

        val body = response.data
        if (response.code != 0 || body == null) {
            val message = response.message.ifBlank { "取证事件上报失败。" }
            markFailed(pending.map { it.clientItemKey }, message)
            logs.warn("forensics-sync", "取证事件上报被拒绝：$message")
            return 0
        }

        val accepted = body.acceptedKeys.toSet() + body.skippedKeys.toSet()
        val rejected = body.rejectedKeys.toSet()

        if (accepted.isNotEmpty()) {
            dao.updateSyncStatusByKeys(accepted.toList(), MobileSyncStatus.SYNCED)
        }
        if (rejected.isNotEmpty()) {
            // 服务端明确拒绝的条目留在 pending 并带上原因，不静默消失（REQ-28）。
            dao.updateSyncStatusByKeys(
                rejected.toList(),
                MobileSyncStatus.PENDING,
                "服务端拒绝了该取证事件条目。"
            )
        }

        if (pending.isNotEmpty()) {
            sentinelStore.markAlive(nowUtcMillis(), BootElapsedClock.now())
        }

        logs.info(
            "forensics-sync",
            "取证事件上报完成。",
            mapOf(
                "uploaded" to pending.size,
                "accepted" to body.acceptedCount,
                "skipped" to body.skippedCount,
                "rejected" to body.rejectedCount,
                "droppedReasonRows" to body.acceptedDroppedReasonCount
            )
        )
        return pending.size
    }

    /** 本地待传取证事件条数（用于同步日志与状态页显示）。 */
    suspend fun pendingCount(): Int = try {
        dao.countBySyncStatus(MobileSyncStatus.PENDING)
    } catch (ex: CancellationException) {
        throw ex
    } catch (_: Exception) {
        0
    }

    private suspend fun markFailed(keys: List<String>, message: String?) {
        if (keys.isEmpty()) return
        try {
            dao.updateSyncStatusByKeys(keys, MobileSyncStatus.PENDING, message)
        } catch (ex: CancellationException) {
            throw ex
        } catch (ex: Exception) {
            logs.error("forensics-sync", "标记取证事件待重传失败：${ex.message ?: ""}", ex)
        }
    }

    /**
     * 按「设备本地日 + 原因」生成统计快照（REQ-9 / AC-9.2）。
     *
     * 逐页扫描明细并**按设备时区**归日，因此：
     * - 本地日与 Android 上报口径完全一致，设备时区不是 UTC+8 也不会错位；
     * - 本地不设条数上限（R4-P3），分页扫描没有固定行数上限，计数不会被截断。
     *
     * 覆盖窗口内的**全部**本地日（缺的填 0），因此设备端删掉明细后服务端也会归零，
     * 不会留下一份越用越偏的陈旧统计；同一份快照重复上报是覆盖写，不会翻倍（幂等）。
     */
    suspend fun buildDroppedReasonSummaries(): List<MobileDroppedReasonSummaryDto> {
        val zone = ZoneId.systemDefault()
        val now = nowUtcMillis()
        val windowStart = now - WINDOW_DAYS * DAY_MILLIS

        val byDayAndReason = mutableMapOf<Pair<String, String>, Int>()
        var afterId = 0L
        try {
            while (true) {
                val page = dao.droppedDiagnosticPage(windowStart, afterId, PAGE_SIZE)
                if (page.isEmpty()) break
                for (row in page) {
                    afterId = row.id
                    val key = localDate(row.recordedAtUtc, zone) to row.reason
                    byDayAndReason[key] = (byDayAndReason[key] ?: 0) + 1
                }
                if (page.size < PAGE_SIZE) break
            }
        } catch (ex: CancellationException) {
            throw ex
        } catch (ex: Exception) {
            logs.warn("forensics-sync", "读取丢弃原因统计失败：${ex.message ?: ""}")
            return emptyList()
        }

        if (byDayAndReason.isEmpty()) return emptyList()

        // 原因集合取窗口内出现过的全部原因；窗口内的每一天都要出现（缺的填 0）。
        val reasons = byDayAndReason.keys.map { it.second }.distinct().sorted()

        return (0 until WINDOW_DAYS)
            .map { offset ->
                Instant.ofEpochMilli(now).atZone(zone).toLocalDate()
                    .minusDays(offset.toLong()).toString()
            }
            .distinct()
            .sorted()
            .flatMap { day ->
                reasons.map { reason ->
                    MobileDroppedReasonSummaryDto(
                        localDate = day,
                        reason = reason,
                        count = byDayAndReason[day to reason] ?: 0
                    )
                }
            }
    }

    /** 最近 20 条丢弃明细（REQ-9：应用内「丢弃原因」页）。 */
    suspend fun recentDroppedDetails(limit: Int = 20): List<DroppedDiagnosticExportRow> = try {
        dao.droppedDiagnosticsForExport(limit)
    } catch (ex: CancellationException) {
        throw ex
    } catch (_: Exception) {
        emptyList()
    }

    private fun deviceId(): String {
        val androidId = Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID)
        val seed = androidId ?: Build.FINGERPRINT ?: "android-device"
        return "android-${sha256Hex(seed).take(16)}"
    }

    private fun localDate(utcMillis: Long, zone: ZoneId): String =
        Instant.ofEpochMilli(utcMillis).atZone(zone).toLocalDate().toString()

    private fun iso(utcMillis: Long): String = Instant.ofEpochMilli(utcMillis).toString()

    companion object {
        /** 与服务端单批上限保持一致。 */
        const val BATCH_LIMIT = 500

        /** 丢弃原因统计的上报窗口：30 天（与本地清理窗口一致，AC-9.3）。 */
        const val WINDOW_DAYS = 30

        private const val DAY_MILLIS = 24L * 60L * 60L * 1000L

        /**
         * 统计扫描的分页大小。这只是**每次查询的行数**，不是明细条数上限：
         * 扫描会一直翻页到窗口内没有更多明细为止（R4-P3 不设条数上限）。
         */
        const val PAGE_SIZE = 1_000
    }
}
