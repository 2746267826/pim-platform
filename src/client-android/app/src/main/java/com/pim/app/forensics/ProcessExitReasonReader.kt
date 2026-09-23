package com.pim.app.forensics

import android.app.ActivityManager
import android.content.Context
import android.os.Build
import javax.inject.Inject
import javax.inject.Singleton

/** 系统记录的一条历史进程退出（REQ-1）。 */
data class HistoricalExitRecord(
    val timestampMillis: Long,
    val apiReason: Int,
    val reason: String,
    val importance: Int?,
    val pssKb: Long?,
    val rssKb: Long?,
    val description: String?
)

/**
 * 进程退出记录读取结果。
 *
 * [supported] 为 false 时表示系统不提供该能力（API < 30）：页面必须显示"未知"并给出可推断线索，
 * **不得**给出"无异常"结论（AC-1.3）。
 */
data class ExitReasonReadResult(
    val supported: Boolean,
    val records: List<HistoricalExitRecord>,
    val failureReason: String? = null
)

/** 进程退出记录的来源抽象，便于在测试里注入。 */
interface ExitReasonSource {
    fun read(limit: Int): ExitReasonReadResult
}

/**
 * 读取 Android 系统记录的"本应用历次进程退出原因"
 * （`ActivityManager.getHistoricalProcessExitReasons`，API 30+，只能查询自己进程）。
 *
 * **读取失败不得影响应用启动与定位采集**（AC-1.4）：所有异常都被收敛成
 * [ExitReasonReadResult.failureReason]，调用方据此在页面显示"未知"。
 */
@Singleton
class AndroidExitReasonReader @Inject constructor(
    private val context: Context
) : ExitReasonSource {

    override fun read(limit: Int): ExitReasonReadResult {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
            return ExitReasonReadResult(
                supported = false,
                records = emptyList(),
                failureReason = "当前系统版本不提供进程退出记录（需要 Android 11 / API 30 及以上）。"
            )
        }

        return try {
            val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
                ?: return ExitReasonReadResult(
                    supported = false,
                    records = emptyList(),
                    failureReason = "无法获取 ActivityManager。"
                )

            val infos = activityManager.getHistoricalProcessExitReasons(
                context.packageName,
                0,
                limit.coerceIn(1, MAX_RECORDS)
            )

            val records = infos?.map { info ->
                HistoricalExitRecord(
                    timestampMillis = info.timestamp,
                    apiReason = info.reason,
                    reason = ProcessExitReasons.fromApiReason(info.reason),
                    importance = runCatching { info.importance }.getOrNull(),
                    pssKb = runCatching { info.pss }.getOrNull()?.takeIf { it > 0 },
                    rssKb = runCatching { info.rss }.getOrNull()?.takeIf { it > 0 },
                    description = runCatching { info.description }.getOrNull()?.takeIf { it.isNotBlank() }
                )
            } ?: emptyList()

            ExitReasonReadResult(supported = true, records = records)
        } catch (ex: Exception) {
            // AC-1.4：读取失败不影响启动与采集，只把失败原因如实带出来。
            ExitReasonReadResult(
                supported = true,
                records = emptyList(),
                failureReason = ex.message ?: ex::class.java.simpleName
            )
        }
    }

    companion object {
        /** 系统历史条数有限，取一个够用的上限即可（平台依据：条数由系统配置决定）。 */
        const val MAX_RECORDS = 32
    }
}

/**
 * 把读到的历史退出记录落成台账（REQ-1 / AC-1.1 / AC-1.2）。
 *
 * 去重按（时刻秒 + 原因）进行：`ApplicationExitInfo` 没有唯一 ID，
 * 平台依据明确要求实现方自行去重，否则每次启动都会重复写同一批历史记录。
 */
@Singleton
class ExitReasonRecorder @Inject constructor(
    private val source: ExitReasonSource,
    private val ledger: ForensicLedger,
    private val contextReader: ForensicContextSource,
    private val nowUtcMillis: () -> Long = System::currentTimeMillis
) {
    /** 已记录的幂等键；避免重复读同一批历史时反复查库。 */
    private val recordedKeys = mutableSetOf<String>()

    /**
     * 读取历史退出记录并把新出现的条目写入台账。
     *
     * @return 读取结果（含 supported / failureReason），供状态页显示"未知 + 推断线索"（AC-1.3）。
     */
    suspend fun recordNewExits(limit: Int = AndroidExitReasonReader.MAX_RECORDS): ExitReasonReadResult {
        val result = source.read(limit)
        if (result.records.isEmpty()) {
            return result
        }

        // 退出记录的上下文只能取"现在"的：进程死亡瞬间的状态系统不提供，
        // 因此这里明确按"事后读取"记录，读不到的字段留空并标注不可用（AC-4.2）。
        val context = contextReader.read()

        for (record in result.records.sortedBy { it.timestampMillis }) {
            val key = ledger.exitKey(record.timestampMillis, record.reason)
            if (!recordedKeys.add(key)) continue

            ledger.recordProcessExit(
                occurredAtUtcMillis = record.timestampMillis,
                reason = record.reason,
                payloadJson = ForensicPayloads.processExit(
                    reason = record.reason,
                    occurredAtMillis = record.timestampMillis,
                    importance = record.importance,
                    pssKb = record.pssKb,
                    rssKb = record.rssKb,
                    description = record.description,
                    inference = if (record.reason == ProcessExitReasons.NO_RECORD) {
                        "系统未提供该次退出的原因。"
                    } else {
                        null
                    },
                    context = context
                )
            )
        }

        return result
    }

    /** 供强停判定使用的"最近一次存活之后是否留有退出记录"。 */
    fun hasExitRecordAfter(millis: Long, records: List<HistoricalExitRecord>): Boolean =
        records.any { it.timestampMillis > millis }

    /** 供 AC-2.3 使用：哨兵登记之后是否出现过权限变更导致的退出。 */
    fun hasPermissionChangeAfter(millis: Long, records: List<HistoricalExitRecord>): Boolean =
        records.any {
            it.timestampMillis > millis && it.reason == ProcessExitReasons.PERMISSION_CHANGE
        }

    /** 测试/诊断用：当前进程已记录过的键数量。 */
    fun recordedKeyCount(): Int = recordedKeys.size
}
