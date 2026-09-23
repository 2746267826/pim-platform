package com.pim.app.forensics

import com.pim.app.data.ForensicEventDao
import com.pim.app.mobile.logs.StructuredLogRepository
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CancellationException

/** 状态页顶部的存活区块数据（REQ-8）。 */
data class LivenessUiSnapshot(
    val hasData: Boolean,
    val conclusion: String,
    val lastHeartbeatAtUtcMillis: Long?,
    val coverageByHourText: String,
    val coverageByExpectedHeartbeatText: String,
    val coverageByHourDefinition: String,
    val coverageByExpectedHeartbeatDefinition: String,
    val latestCauseLabel: String?,
    val latestCauseInference: String?,
    val staleNote: String?,
    val exitReasonSupported: Boolean,
    val exitReasonUnavailableReason: String?
)

/** 「丢弃原因」页的统计与明细（REQ-9）。 */
data class DroppedReasonUiState(
    val totalCount: Int,
    val byReason: List<DroppedReasonCountOnly>,
    val recentDetails: List<com.pim.app.data.DroppedDiagnosticExportRow>,
    val detailLimit: Int
)

/** 按原因聚合的计数（UI 用）。 */
data class DroppedReasonCountOnly(val reason: String, val count: Int)

/**
 * 状态页存活区块的取数（REQ-8 / REQ-13）。
 *
 * 只读本地取证台账（REQ-3 的心搏），**不触发任何网络请求**，因此离线时也能给出本地结论；
 * 离线且本地无数据时明确显示"无数据/未上报"，绝不报平安（AC-8.2）。
 */
@Singleton
class ForensicLivenessRepository @Inject constructor(
    private val dao: ForensicEventDao,
    private val exitReasonSource: ExitReasonSource,
    private val logs: StructuredLogRepository,
    private val nowUtcMillis: () -> Long = System::currentTimeMillis
) {

    /** 窗口：最近 7 天，与 Web「设备存活」页默认区间一致（AC-7.1）。 */
    val windowDays: Int = DEFAULT_WINDOW_DAYS

    suspend fun snapshot(windowDays: Int = DEFAULT_WINDOW_DAYS): LivenessUiSnapshot {
        val now = nowUtcMillis()
        val start = now - windowDays * DAY_MILLIS

        val events = try {
            dao.eventsInRange(start, now)
        } catch (ex: CancellationException) {
            throw ex
        } catch (ex: Exception) {
            logs.warn("forensics", "读取本地取证台账失败：${ex.message ?: ""}")
            emptyList()
        }

        val heartbeats = events
            .filter { it.eventType == ForensicEventTypes.HEARTBEAT }
            .map { it.occurredAtUtc }

        val latestCause = events
            .filter {
                it.eventType == ForensicEventTypes.PROCESS_EXIT ||
                    it.eventType == ForensicEventTypes.FORCE_STOP
            }
            .maxByOrNull { it.occurredAtUtc }

        val causeLabel = latestCause?.let { describeCause(it.eventType, it.payloadJson) }

        val summary = LivenessSummaryCalculator.summarize(
            heartbeatTimestampsUtcMillis = heartbeats,
            rangeStartUtcMillis = start,
            rangeEndUtcMillis = now,
            lastCauseLabel = causeLabel?.first
        )

        val lastHeartbeat = heartbeats.maxOrNull()
        val staleMillis = lastHeartbeat?.let { now - it }

        // 陈旧标注沿用 AC-7.4 已经确认的"≥1 小时"判定线，不另立阈值。
        val staleNote = if (hasData(summary) && staleMillis != null &&
            staleMillis >= LivenessRules.SILENCE_CRITICAL_MINUTES * 60_000L
        ) {
            "数据为 ${formatAge(staleMillis)}前"
        } else {
            null
        }

        // AC-1.3：系统不支持 / 读取失败时，页面必须能说明"为什么是未知"，而不是给"无异常"。
        val exitProbe = runCatching { exitReasonSource.read(1) }.getOrNull()

        return LivenessUiSnapshot(
            hasData = summary.hasData,
            conclusion = summary.conclusion,
            lastHeartbeatAtUtcMillis = lastHeartbeat,
            coverageByHourText = CoverageFormatting.formatByHour(summary),
            coverageByExpectedHeartbeatText = CoverageFormatting.formatByExpectedHeartbeat(summary),
            coverageByHourDefinition = LivenessRules.COVERAGE_BY_HOUR_DEFINITION,
            coverageByExpectedHeartbeatDefinition = LivenessRules.COVERAGE_BY_EXPECTED_HEARTBEAT_DEFINITION,
            latestCauseLabel = causeLabel?.first,
            latestCauseInference = causeLabel?.second,
            staleNote = staleNote,
            exitReasonSupported = exitProbe?.supported ?: true,
            exitReasonUnavailableReason = exitProbe?.failureReason
        )
    }

    suspend fun droppedReasons(limit: Int = DROPPED_DETAIL_LIMIT): DroppedReasonUiState {
        val details = try {
            dao.droppedDiagnosticsForExport(limit)
        } catch (ex: CancellationException) {
            throw ex
        } catch (ex: Exception) {
            logs.warn("forensics", "读取丢弃原因失败：${ex.message ?: ""}")
            emptyList()
        }

        val counts = try {
            dao.droppedReasonCounts(0L)
        } catch (ex: CancellationException) {
            throw ex
        } catch (_: Exception) {
            emptyList()
        }

        return DroppedReasonUiState(
            totalCount = counts.sumOf { it.count },
            byReason = counts.map { DroppedReasonCountOnly(it.reason, it.count) },
            recentDetails = details,
            detailLimit = limit
        )
    }

    /** 「最近死因」的中文文案与推断依据（AC-1.3 / AC-7.1）。 */
    fun describeCause(eventType: String, payloadJson: String): Pair<String, String?>? {
        val root = runCatching { org.json.JSONObject(payloadJson) }.getOrNull()
        val inference = root?.optString("inference")?.takeIf { it.isNotBlank() && it != "null" }

        if (eventType == ForensicEventTypes.FORCE_STOP) {
            val kind = root?.optString("kind") ?: return "未知" to inference
            return ForceStopDetector.labelOf(kind) to inference
        }

        val reason = root?.optString("reason")
        if (reason.isNullOrBlank() || reason == "null") {
            return "未知（系统未提供退出记录）" to
                (inference ?: "系统未提供该次进程退出的原因，无法判定死因。")
        }

        val label = ExitReasonLabels.labelOf(reason)
        return label to inference
    }

    private fun hasData(summary: DeviceLivenessSummary): Boolean = summary.hasData

    companion object {
        const val DEFAULT_WINDOW_DAYS = 7
        const val DROPPED_DETAIL_LIMIT = 20
        private const val DAY_MILLIS = 24L * 60L * 60L * 1000L

        /** 把毫秒时长格式化成"N 小时前 / N 分钟前"。 */
        fun formatAge(millis: Long): String {
            val minutes = millis / 60_000L
            if (minutes < 60) return "$minutes 分钟"
            val hours = minutes / 60
            if (hours < 48) return "$hours 小时"
            return "${hours / 24} 天"
        }
    }
}

/**
 * 设备端死因文案（与 `mobile_liveness_cause_classifier` 的服务端映射保持一致）。
 * 服务端仍会按原始原因常量重新翻译一次，页面与 Web 因此不会出现两套措辞。
 */
object ExitReasonLabels {
    fun labelOf(reason: String): String = when (reason) {
        ProcessExitReasons.LOW_MEMORY -> "内存不足被系统回收"
        ProcessExitReasons.SIGNALED -> "被系统信号终止"
        ProcessExitReasons.FREEZER -> "被冻结器回收（缓存进程）"
        ProcessExitReasons.USER_REQUESTED -> "用户强停"
        ProcessExitReasons.USER_STOPPED -> "用户主动停止"
        ProcessExitReasons.ANR -> "应用无响应（ANR）"
        ProcessExitReasons.CRASH -> "应用崩溃（Java）"
        ProcessExitReasons.CRASH_NATIVE -> "原生崩溃"
        ProcessExitReasons.EXCESSIVE_RESOURCE_USAGE -> "资源占用过高被系统终止"
        ProcessExitReasons.EXIT_SELF -> "应用主动退出"
        ProcessExitReasons.DEPENDENCY_DIED -> "依赖进程死亡"
        ProcessExitReasons.INITIALIZATION_FAILURE -> "初始化失败"
        ProcessExitReasons.PACKAGE_STATE_CHANGE -> "包状态变更"
        ProcessExitReasons.PACKAGE_UPDATED -> "应用被更新"
        ProcessExitReasons.PERMISSION_CHANGE -> "权限变更"
        ProcessExitReasons.OTHER -> "其他原因"
        ProcessExitReasons.UNKNOWN -> "系统未给出原因"
        ProcessExitReasons.NO_RECORD -> "未知（系统未提供退出记录）"
        else -> "系统未给出原因"
    }
}
