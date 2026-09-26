package com.pim.app.location.passive

import com.pim.app.location.LocationPointPayload
import com.pim.app.location.acquisition.LocationAcquisitionOperations
import com.pim.app.location.acquisition.TriggerType
import com.pim.app.location.quality.RawLocationFix
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CancellationException

/**
 * 被动定位的运行时编排（WO-ANDROID-GATE-20260926 REQ-14）。
 *
 * 把「注册监听 → 判定 → 入库/留痕 → 计数落台账」收敛到一个类里，
 * 让 [com.pim.app.location.service.ForegroundLocationService] 只负责生命周期
 * （start/stop），保持既有的「服务不直接碰质量门与丢弃诊断」边界
 * （既有 `serviceDoesNotOwnFusedLocationProviderCallback` 契约测试守着这一点）。
 *
 * 三条硬约束（改动前请读工单）：
 * - AC-14.3：不设限流；
 * - AC-14.7：不触碰主动流的注册与锚点（本类只**读**主动流最近 fix 做重复判定）；
 * - AC-14.8：不产出/不修改冲刺台账口径。
 */
@Singleton
class PassiveLocationCoordinator internal constructor(
    private val source: PassiveLocationSource,
    private val operations: LocationAcquisitionOperations,
    private val ledger: PassiveLocationLedger,
    private val nowUtcMillis: () -> Long
) {
    @Inject
    constructor(
        source: PassiveLocationSource,
        operations: LocationAcquisitionOperations,
        ledger: PassiveLocationLedger
    ) : this(source, operations, ledger, System::currentTimeMillis)

    private var processor: PassiveLocationProcessor? = null
    private var windowStartUtcMillis: Long = nowUtcMillis()

    /** 最近一次注册结果（AC-14.1：注册成功日志 + 可核对状态）。 */
    val registration: PassiveRegistrationResult
        get() = source.registration

    fun isRegistered(): Boolean = source.isRegistered()

    /**
     * 启动被动监听（AC-14.1：随采集服务启动）。
     *
     * @param activeFixProvider 主动流最近 fix 的**只读快照**（AC-14.5 重复判定用）。
     */
    fun start(activeFixProvider: () -> List<RawLocationFix>): PassiveRegistrationResult {
        if (source.isRegistered()) return source.registration
        windowStartUtcMillis = nowUtcMillis()
        val newProcessor = source.processorUsing(activeFixProvider)
        newProcessor.onAccepted = { result ->
            val raw = LocationPointPayload.encode(
                accepted = result.accepted,
                source = result.source,
                submittedAtMillis = nowUtcMillis()
            )
            // REQ-14 规则 6：入库点 source = passive、provider 保持原始提供方。
            operations.enqueueAccepted(result.accepted, raw, result.source)
            operations.scheduleSync()
        }
        newProcessor.onDrop = { fix, reason ->
            // REQ-14 规则 4：原因编码落既有丢弃诊断表（不改库结构）。
            operations.recordDropped(fix, reason)
        }
        processor = newProcessor
        val registered = source.register(newProcessor)
        // AC-14.1：注册成功即留台账痕迹（含 provider 与时刻），供 dumpsys/logcat 交叉核对。
        return registered
    }

    /**
     * 停止被动监听（AC-14.1：服务停止后窗口内不再产生被动点）。
     *
     * 停止前把本窗口的三数计数落台账（AC-14.2）：否则「回调总数」这个分母
     * 会随着服务停止而消失，验收方无法对账。
     */
    suspend fun stop() {
        val active = processor
        processor = null
        source.unregister()
        if (active == null) return
        val counters = active.drainCounters(
            now = nowUtcMillis(),
            windowStart = windowStartUtcMillis
        )
        if (counters.callbackCount == 0) return
        ledger.recordCounters(
            occurredAtUtcMillis = nowUtcMillis(),
            windowStartUtcMillis = windowStartUtcMillis,
            callbackCount = counters.callbackCount,
            acceptedCount = counters.acceptedCount,
            droppedCount = counters.droppedCount,
            duplicateCount = counters.duplicateCount
        )
    }

    /** 当前窗口的三数计数快照（诊断/状态页用）。 */
    fun counters(): PassiveLocationCounters? = processor?.countersSnapshot()
}
