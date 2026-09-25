package com.pim.app.keepalive

import com.pim.app.mobile.logs.StructuredLogRepository
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CancellationException

/**
 * 叫醒执行链（REQ-16）。
 *
 * 工单要求的顺序（不可调换，AC-16.1 ~ AC-16.4 逐条对应）：
 * 1. 写台账（先记「我来过」，即使后面全失败也留有证据）；
 * 2. 检查前台采集服务是否在跑；
 * 3. 未运行 → 拉起接管；已运行 → 触发一次补传；
 * 4. 拉起失败时至少抓一颗定位点存本地待传（兜底，AC-16.3）。
 *
 * 与暂停 / 手动会话的关系（REQ-20）：
 * - 暂停中：仍写台账，但**不拉起服务、不采集**（AC-20.1），并记为「因暂停未执行」（AC-20.3）。
 * - 手动采集会话进行中：跳过本次叫醒，且不得打断会话（AC-20.2）。
 *
 * 本类通过 [WakeEnvironment] 与系统交互，因此执行链本身可以脱离 Android 单测。
 */
@Singleton
class WakeExecutionChain internal constructor(
    private val ledger: AlarmFulfillmentRecorder,
    private val settingsStore: KeepAliveSettingsAccessor,
    private val environment: WakeEnvironment,
    private val logs: StructuredLogRepository,
    private val nowUtcMillis: () -> Long
) {
    /** 生产构造：时钟由这里给出，因此 Hilt 不需要绑定 `() -> Long`（与阶段一同款做法）。 */
    @Inject
    constructor(
        ledger: AlarmFulfillmentRecorder,
        settingsStore: KeepAliveSettingsAccessor,
        environment: WakeEnvironment,
        logs: StructuredLogRepository
    ) : this(ledger, settingsStore, environment, logs, System::currentTimeMillis)
    /**
     * 执行一次叫醒。
     *
     * @param scheduledAtUtcMillis 预定时刻（登记闹钟时确定，用于算延迟）
     * @return 本次执行结果（同时已写入台账）
     */
    suspend fun execute(scheduledAtUtcMillis: Long): AlarmFulfillmentRecord {
        val now = nowUtcMillis()
        val settings = settingsStore.read()

        // 顺序 1：先写台账。AC-16.1 要求「杀死进程后，下一个闹钟周期内被叫醒，台账记录本次动作」，
        // 因此这一步必须在任何可能失败的动作之前。
        val outcome = try {
            runChain(settings, now)
        } catch (ex: CancellationException) {
            throw ex
        } catch (ex: Exception) {
            // REQ-28：失败必须有可见出口，不能静默。
            logs.error("keepalive", "叫醒执行链失败：${ex.message ?: ex::class.java.simpleName}", ex)
            AlarmOutcomes.PULL_FAILED
        }

        val record = AlarmFulfillmentRecord(
            scheduledAtUtcMillis = scheduledAtUtcMillis,
            // AC-18.3：只有真的执行了才写实际时刻；跳过与未执行保持 null，不进兑现率分母。
            actualAtUtcMillis = if (outcome == AlarmOutcomes.EXECUTED || outcome == AlarmOutcomes.SUPPRESSED ||
                outcome == AlarmOutcomes.PULL_FAILED
            ) {
                now
            } else {
                null
            },
            // 延迟是否超阈值在这里**一次判定**：链路真的执行完（EXECUTED）但延迟过大时，
            // 结果改写为「被压制」（AC-17.1）。这样「被压制」这个结论只有一处产生，
            // 降频策略与计数推进都读它，不会出现两处各判一次判出不同结果。
            outcome = if (outcome == AlarmOutcomes.EXECUTED &&
                AlarmSuppressionPolicy.isSuppressedDelay(now - scheduledAtUtcMillis)
            ) {
                AlarmOutcomes.SUPPRESSED
            } else {
                outcome
            }
        )

        ledger.recordFulfillment(record)
        advanceCounters(settings, record)
        return record
    }

    private suspend fun runChain(settings: KeepAliveSettings, now: Long): String {
        // 总开关关闭：不执行（AC-22.2），但循环由调度侧负责，这里只如实记账。
        if (!settings.enabled) return AlarmOutcomes.SKIPPED_DISABLED

        // REQ-20：暂停中只写台账、不拉起、不采集。
        if (environment.isPaused()) {
            logs.info("keepalive", "叫醒时采集处于暂停状态：只记台账，不拉起、不采集")
            return AlarmOutcomes.SKIPPED_PAUSED
        }

        // REQ-20 / AC-20.2：手动采集会话进行中跳过，且不得中断会话。
        if (environment.isManualSessionActive()) {
            logs.info("keepalive", "叫醒时正在进行手动采集会话：跳过本次叫醒，不打断会话")
            return AlarmOutcomes.SKIPPED_MANUAL_SESSION
        }

        if (environment.isForegroundServiceRunning()) {
            // 已在运行：触发一次补传（AC-16.1 的「已运行则触发一次补传」分支）。
            val synced = environment.requestSyncNow()
            if (!synced) {
                logs.warn("keepalive", "叫醒时请求补传失败，将在下个周期重试")
            }
            return AlarmOutcomes.EXECUTED
        }

        // 未运行：拉起接管（精确闹钟不受后台启动前台服务限制，平台依据 §2）。
        val started = environment.startForegroundService()
        if (started) {
            logs.info("keepalive", "叫醒后已拉起前台采集服务（AC-16.2）")
            return AlarmOutcomes.EXECUTED
        }

        // AC-16.3：拉起失败时至少抓一颗定位点存本地待传，保证叫醒不是白醒。
        logs.warn("keepalive", "拉起前台采集服务失败，改为兜底抓取一颗定位点")
        val captured = environment.captureSinglePointFallback()
        if (!captured) {
            logs.error("keepalive", "兜底抓点也失败：本次叫醒未产生任何数据", null)
        }
        return AlarmOutcomes.PULL_FAILED
    }

    /** 按本次结果推进连续计数（REQ-17）。 */
    private fun advanceCounters(settings: KeepAliveSettings, record: AlarmFulfillmentRecord) {
        val next = AlarmRecordCounter.advance(
            AlarmRecordCounter.Counters(
                consecutiveSuppressed = settings.consecutiveSuppressed,
                consecutiveOnTime = settings.consecutiveOnTime,
                consecutiveWakeFailures = settings.consecutiveWakeFailures
            ),
            record
        )
        settingsStore.write(
            settings.copy(
                consecutiveSuppressed = next.consecutiveSuppressed,
                consecutiveOnTime = next.consecutiveOnTime,
                consecutiveWakeFailures = next.consecutiveWakeFailures
            )
        )
    }
}

/**
 * 叫醒执行链与系统之间的接口。
 *
 * 抽出来是为了让执行链的顺序与分支（尤其是 REQ-20 的暂停/手动会话、AC-16.3 的兜底）
 * 可以在 JVM 上逐条断言，而不是只能靠真机观察。
 */
interface WakeEnvironment {
    /** 采集是否处于用户暂停状态（REQ-20）。 */
    suspend fun isPaused(): Boolean

    /** 是否有手动采集会话正在进行（AC-20.2）。 */
    suspend fun isManualSessionActive(): Boolean

    /** 前台采集服务是否在运行。 */
    fun isForegroundServiceRunning(): Boolean

    /** 请求一次补传（服务已在运行时的分支）。 */
    suspend fun requestSyncNow(): Boolean

    /** 拉起前台采集服务；返回是否成功。 */
    suspend fun startForegroundService(): Boolean

    /** 兜底抓取一颗定位点并存本地待传（AC-16.3）。 */
    suspend fun captureSinglePointFallback(): Boolean
}
