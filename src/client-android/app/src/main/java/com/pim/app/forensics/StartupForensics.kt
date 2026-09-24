package com.pim.app.forensics

import com.pim.app.location.service.ForegroundLocationService
import com.pim.app.mobile.logs.StructuredLogRepository
import com.pim.app.mobile.sync.MobileSyncScheduler
import javax.inject.Inject
import javax.inject.Provider
import javax.inject.Singleton
import kotlinx.coroutines.CancellationException

/** 一次启动取证的执行结果（供状态页与诊断导出使用）。 */
data class StartupForensicsResult(
    val exitReasonSupported: Boolean,
    val exitReasonFailure: String?,
    val newExitRecordCount: Int,
    val verdict: ForceStopVerdict,
    val heartbeatRecorded: Boolean
)

/**
 * 启动取证编排（REQ-1 ~ REQ-4）。
 *
 * 应用每次被唤醒（冷启动、开机恢复、周期同步、用户打开）都调用一次：
 * 1. 读取系统记录的进程退出原因并落台账（REQ-1）；
 * 2. 判定"强停 / 重启 / 哨兵被清空"（REQ-2）；
 * 3. 写一条存活心跳，附带待机桶、电池优化、Doze、前台服务与上下文（REQ-3 / REQ-4）；
 * 4. 按 30 天窗口清理本地取证台账（AC-6.2）；
 * 5. 刷新哨兵的"最近存活"基线。
 *
 * 整个过程**不新增任何轮询或闹钟**（AC-29.1）；任何一步失败都只影响该步，
 * 不阻断定位采集与既有同步（AC-1.4 / AC-3.3）。
 */
@Singleton
class StartupForensics internal constructor(
    private val exitRecorder: ExitReasonRecorder,
    private val ledger: ForensicLedger,
    private val sentinelStore: ForensicSentinelStore,
    private val sentinelProbe: SentinelProbe,
    private val contextReader: ForensicContextSource,
    private val heartbeatReader: AndroidHeartbeatSnapshotReader,
    private val retention: ForensicRetention,
    private val logs: StructuredLogRepository,
    private val syncScheduler: Provider<MobileSyncScheduler>,
    private val nowUtcMillis: () -> Long,
    private val bootElapsedMillis: () -> Long,
    private val serviceRunning: () -> Boolean
) {
    @Inject
    constructor(
        exitRecorder: ExitReasonRecorder,
        ledger: ForensicLedger,
        sentinelStore: ForensicSentinelStore,
        sentinelProbe: SentinelProbe,
        contextReader: ForensicContextSource,
        heartbeatReader: AndroidHeartbeatSnapshotReader,
        retention: ForensicRetention,
        logs: StructuredLogRepository,
        syncScheduler: Provider<MobileSyncScheduler>
    ) : this(
        exitRecorder,
        ledger,
        sentinelStore,
        sentinelProbe,
        contextReader,
        heartbeatReader,
        retention,
        logs,
        syncScheduler,
        nowUtcMillis = System::currentTimeMillis,
        bootElapsedMillis = BootElapsedClock::now,
        serviceRunning = { ForegroundLocationService.isRunning() }
    )

    suspend fun recordOnStartup(): StartupForensicsResult {
        val now = nowUtcMillis()
        val bootElapsed = bootElapsedMillis()
        val previousState = sentinelStore.read()

        // 1) 进程退出原因台账（REQ-1）。读取失败不影响后续步骤（AC-1.4）。
        val readResult = try {
            exitRecorder.recordNewExits()
        } catch (ex: CancellationException) {
            throw ex
        } catch (ex: Exception) {
            logs.error("forensics", "读取进程退出记录失败：${ex.message ?: ""}", ex)
            ExitReasonReadResult(
                supported = true,
                records = emptyList(),
                failureReason = ex.message ?: ex::class.java.simpleName
            )
        }

        // 2) 强停 / 重启判定（REQ-2）。哨兵 = 既有周期同步作业是否仍在队列里。
        val sentinelPresent = try {
            sentinelProbe.isSentinelPresent()
        } catch (ex: CancellationException) {
            throw ex
        } catch (_: Exception) {
            true
        }

        val lastAliveAt = previousState.lastAliveAtUtcMillis
        val detectionInput = ForceStopDetectionInput(
            nowUtcMillis = now,
            nowBootElapsedMillis = bootElapsed,
            sentinelPresent = sentinelPresent,
            exitRecordAfterLastAlive = lastAliveAt != null &&
                exitRecorder.hasExitRecordAfter(lastAliveAt, readResult.records),
            exitRecordSaysUserRequested = lastAliveAt != null &&
                exitRecorder.hasUserRequestedExitAfter(lastAliveAt, readResult.records),
            permissionChangeAfterArmed = previousState.armedAtUtcMillis?.let { armedAt ->
                exitRecorder.hasPermissionChangeAfter(armedAt, readResult.records)
            } ?: false
        )
        val verdict = ForceStopDetector.detect(previousState, detectionInput)

        var recorded = false
        val kind = ForceStopDetector.kindOf(verdict)
        if (kind != null) {
            val evidence = ForceStopDetector.evidenceOf(verdict, detectionInput)
            recorded = ledger.recordForceStop(
                occurredAtUtcMillis = now,
                kind = kind,
                payloadJson = ForensicPayloads.forceStop(
                    kind = kind,
                    evidence = evidence,
                    inference = ForceStopDetector.inferenceOf(verdict, detectionInput),
                    context = safeContext()
                )
            )
            logs.warn(
                "forensics",
                "启动取证结论：${ForceStopDetector.labelOf(kind)}（依据 $evidence）"
            )
        }

        // 3) 存活心跳（REQ-3 / REQ-4）。
        val heartbeatRecorded = recordHeartbeat(now, bootElapsed)

        // 4) 30 天时间清理：取证台账（AC-6.2）与丢弃原因明细（AC-9.3）都按同一条时间线兜底。
        try {
            retention.purgeExpired(now)
        } catch (ex: CancellationException) {
            throw ex
        } catch (ex: Exception) {
            logs.warn("forensics", "本地取证数据清理失败：${ex.message ?: ""}")
        }

        // 5) 刷新"最近存活"基线，并确保哨兵已登记（周期同步作业入队）。
        try {
            sentinelStore.markAlive(now, bootElapsed)
            syncScheduler.get().ensurePeriodic()
            sentinelStore.arm(now, bootElapsed)
        } catch (ex: CancellationException) {
            throw ex
        } catch (ex: Exception) {
            logs.warn("forensics", "登记保活哨兵失败：${ex.message ?: ""}")
        }

        return StartupForensicsResult(
            exitReasonSupported = readResult.supported,
            exitReasonFailure = readResult.failureReason,
            newExitRecordCount = readResult.records.size,
            verdict = verdict,
            heartbeatRecorded = heartbeatRecorded || recorded
        )
    }

    /** 写一条带完整上下文的心跳；写失败不影响采集（AC-3.3）。 */
    suspend fun recordHeartbeat(nowUtcMillis: Long, bootElapsedMillis: Long): Boolean {
        return try {
            val context = safeContext()
            val snapshot = heartbeatReader.read(
                nowElapsedMillis = bootElapsedMillis,
                lastHeartbeatElapsedMillis = lastHeartbeatBootElapsed,
                foregroundServiceRunning = safeServiceRunning(),
                forensicContext = context
            )
            val written = ledger.recordHeartbeat(
                occurredAtUtcMillis = nowUtcMillis,
                payloadJson = ForensicPayloads.heartbeat(snapshot)
            )
            if (written) {
                lastHeartbeatBootElapsed = bootElapsedMillis
            }
            written
        } catch (ex: CancellationException) {
            throw ex
        } catch (ex: Exception) {
            logs.error("forensics", "写入存活心跳失败：${ex.message ?: ""}", ex)
            false
        }
    }

    private fun safeContext(): ForensicContext = try {
        contextReader.read()
    } catch (ex: CancellationException) {
        throw ex
    } catch (_: Exception) {
        ForensicContext(unavailableFields = listOf("屏幕状态", "解锁状态", "前台应用", "充电状态", "电量百分比"))
    }

    private fun safeServiceRunning(): Boolean = try {
        serviceRunning()
    } catch (ex: CancellationException) {
        throw ex
    } catch (_: Exception) {
        false
    }

    private var lastHeartbeatBootElapsed: Long? = null
}
