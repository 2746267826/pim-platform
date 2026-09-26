package com.pim.app.location.sprint

import com.pim.app.location.LocationPointPayload
import com.pim.app.location.acquisition.AcquisitionContext
import com.pim.app.location.acquisition.LocationAcquisitionOperations
import com.pim.app.location.acquisition.TriggerType
import com.pim.app.location.policy.LocationPolicyMode
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CancellationException

/**
 * 冲刺的运行时编排（WO-ANDROID-GATE-20260926 REQ-2 ~ REQ-8）。
 *
 * 把「按周期发起 → 入库/留痕 → 窗口结束回调」收敛到一个类，
 * 让 [com.pim.app.location.service.ForegroundLocationService] 只负责
 * 「每拍调用一次」与生命周期，保持既有边界（既有契约测试要求
 * 服务自身不出现 `recordDropped` / 质量门符号）。
 */
@Singleton
class LocationSprintRuntime internal constructor(
    private val controller: LocationSprintController,
    private val operations: LocationAcquisitionOperations,
    private val nowUtcMillis: () -> Long
) {
    @Inject
    constructor(
        controller: LocationSprintController,
        operations: LocationAcquisitionOperations
    ) : this(controller, operations, System::currentTimeMillis)

    /** 最近一次窗口结果（诊断/状态页用）。 */
    val lastWindowResult: SprintWindowResult?
        get() = controller.lastWindowResult

    /** 组装入库/丢弃出口。构造一次即可，重复调用是幂等的。 */
    fun wire() {
        controller.onAccepted = { accepted, _ ->
            try {
                val raw = LocationPointPayload.encode(
                    accepted = accepted,
                    // 冲刺点属于自动采集流（source = auto），与主流同一口径。
                    source = TriggerType.AUTOMATIC.storageSource,
                    submittedAtMillis = nowUtcMillis()
                )
                operations.enqueueAccepted(
                    accepted,
                    raw,
                    TriggerType.AUTOMATIC.storageSource
                )
                operations.scheduleSync()
            } catch (ex: CancellationException) {
                throw ex
            } catch (ex: Exception) {
                // 入库失败必须留痕（AC-15.3 不得静默）：转成丢弃诊断记录。
                runCatching { operations.recordDropped(accepted.fix, SPRINT_ENQUEUE_FAILED) }
            }
        }
        controller.onDropped = { fix, reason ->
            // AC-4.3：冲刺窗口内未达标的点照常走丢弃诊断。
            runCatching { operations.recordDropped(fix, reason) }
        }
    }

    /**
     * 按当前周期发起一次冲刺（每拍调用，D2）。
     *
     * @param mode 当前策略档（AC-2.4 高速档不冲、AC-10.3 Off 不冲）。
     */
    fun onPeriod(
        context: AcquisitionContext,
        mode: LocationPolicyMode,
        blockedReason: String? = null
    ): SprintStartDecision = controller.startSprint(context, mode, blockedReason)

    /** 停止采集/服务销毁时中止窗口（不产出「已执行」记录）。 */
    fun abort() = controller.abort()

    fun isWindowOpen(): Boolean = controller.isWindowOpen()

    companion object {
        /** 冲刺点入库失败的原因编码（可核对，不静默）。 */
        const val SPRINT_ENQUEUE_FAILED = "sprint-enqueue-failed"
    }
}
