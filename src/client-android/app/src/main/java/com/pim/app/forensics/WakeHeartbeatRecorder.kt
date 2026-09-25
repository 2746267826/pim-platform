package com.pim.app.forensics

import com.pim.app.location.service.ForegroundLocationService
import com.pim.app.mobile.logs.StructuredLogRepository
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * 存活心搏（REQ-3）的**唯一写入口**。
 *
 * 缺陷 #345：阶段一只有 `PimApp.onCreate()`（进程启动）会写心搏，周期同步这条唤醒路径
 * 一条都没写，真机上因此表现为「一周只有 2 条心搏、按小时覆盖率 1.2%」。
 *
 * 把写入收敛到这里之后，任意唤醒路径（启动、周期同步、用户打开）都调用同一个入口：
 * 字段构造、待机桶/Doze/电池优化读数、「距上次心搏间隔」的基线只有一份实现，
 * 不会出现「启动路径记 A 字段、同步路径记 B 字段」的两套口径。
 *
 * - **不新增任何轮询或闹钟**（AC-29.1）：本类只被既有唤醒路径调用，自身不调度任何东西。
 * - 写台账失败只记日志并返回 `false`，绝不抛出，因此不影响采集与同步（AC-3.3）。
 * - 同一秒内多次唤醒由 [ForensicLedger.heartbeatKey] 去重，只落一条（AC-3.3）。
 */
@Singleton
class WakeHeartbeatRecorder internal constructor(
    private val ledger: ForensicLedger,
    private val contextReader: ForensicContextSource,
    private val snapshotReader: HeartbeatSnapshotSource,
    private val logs: StructuredLogRepository,
    private val nowUtcMillis: () -> Long,
    private val bootElapsedMillis: () -> Long,
    private val serviceRunning: () -> Boolean
) {
    @Inject
    constructor(
        ledger: ForensicLedger,
        contextReader: ForensicContextSource,
        snapshotReader: AndroidHeartbeatSnapshotReader,
        logs: StructuredLogRepository
    ) : this(
        ledger = ledger,
        contextReader = contextReader,
        snapshotReader = { now, last, running, context ->
            snapshotReader.read(
                nowElapsedMillis = now,
                lastHeartbeatElapsedMillis = last,
                foregroundServiceRunning = running,
                forensicContext = context
            )
        },
        logs = logs,
        nowUtcMillis = System::currentTimeMillis,
        bootElapsedMillis = BootElapsedClock::now,
        serviceRunning = { ForegroundLocationService.isRunning() }
    )

    /** 记录一次唤醒，时刻与开机时长取当前值。 */
    suspend fun record(): Boolean = record(nowUtcMillis(), bootElapsedMillis())

    /**
     * 记录一次唤醒，时刻与开机时长由调用方给出。
     *
     * 启动取证需要与其他事件（强停判定、清理）共用同一个时刻，因此保留这个重载，
     * 使阶段一的写入行为保持逐字节不变。
     *
     * @return true 表示本次确实写入了一条新记录（同一秒内重复唤醒返回 false）。
     */
    suspend fun record(nowUtcMillis: Long, bootElapsedMillis: Long): Boolean {
        return try {
            // 「读基线 → 造负载 → 写台账 → 更新基线」必须整体串行（见 [baselineLock]）。
            val written = baselineLock.withLock {
                val snapshot = snapshotReader.read(
                    nowElapsedMillis = bootElapsedMillis,
                    lastHeartbeatElapsedMillis = lastHeartbeatBootElapsed,
                    foregroundServiceRunning = safeServiceRunning(),
                    forensicContext = safeContext()
                )
                val inserted = ledger.recordHeartbeat(
                    occurredAtUtcMillis = nowUtcMillis,
                    payloadJson = ForensicPayloads.heartbeat(snapshot)
                )
                if (inserted) {
                    lastHeartbeatBootElapsed = bootElapsedMillis
                }
                inserted
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
        ForensicContext(
            unavailableFields = listOf("屏幕状态", "解锁状态", "前台应用", "充电状态", "电量百分比")
        )
    }

    private fun safeServiceRunning(): Boolean = try {
        serviceRunning()
    } catch (ex: CancellationException) {
        throw ex
    } catch (_: Exception) {
        false
    }

    /**
     * 保护「读基线 → 造负载 → 写台账 → 更新基线」这段读改写序列。
     *
     * 本类是单例，会被**启动取证**与**周期同步**两条唤醒路径并发调用（周期作业可能与
     * 进程启动取证几乎同时跑）。若不加锁，两次唤醒可能同时读到同一个旧基线、或把基线
     * 乱序写回，从而让后一次心搏记下错误、甚至为负的「距上次心搏间隔」。
     *
     * 只包住这一个入口，不改变同步与采集的任何时序：锁内只有一次系统读数、一次序列化
     * 与一次本地插入，且不涉及网络。
     */
    private val baselineLock = Mutex()

    /**
     * 上一次成功写入心搏时的开机时长，用于算「距上次心搏间隔」。
     *
     * 只在 [baselineLock] 内读写，因此不需要 `@Volatile`：串行化本身已经给出了可见性与原子性。
     * 注意：**去重不依赖它**（去重只按壁钟秒级幂等键），所以它最多影响这一个诊断字段。
     */
    private var lastHeartbeatBootElapsed: Long? = null
}

/**
 * 心跳快照读取的函数式别名。
 *
 * 存在的原因：`WakeHeartbeatRecorder` 要能在测试里把「读快照」这一步卡住，
 * 从而构造出真实的并发交错（否则并发缺陷无法被断言）。
 */
internal fun interface HeartbeatSnapshotSource {
    suspend fun read(
        nowElapsedMillis: Long,
        lastHeartbeatElapsedMillis: Long?,
        foregroundServiceRunning: Boolean,
        forensicContext: ForensicContext
    ): HeartbeatSnapshot
}
