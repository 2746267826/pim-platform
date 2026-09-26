package com.pim.app.location.sprint

import com.pim.app.location.LocationSnapshot
import com.pim.app.location.acquisition.AcquisitionContext
import com.pim.app.location.acquisition.LocationAcquisitionRunner
import com.pim.app.location.acquisition.LocationUpdateRequest
import com.pim.app.location.policy.LocationPolicyMode
import com.pim.app.location.quality.LocationQualityGate
import com.pim.app.location.quality.QualityAcceptedLocation
import com.pim.app.location.quality.QualityDecision
import com.pim.app.location.quality.RawLocationFix
import com.google.android.gms.location.Priority
import com.pim.app.settings.TrackingSettingsStore
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/** 一次冲刺请求的结果（含被跳过的原因，AC-8.2）。 */
sealed interface SprintStartDecision {
    data class Started(val startedAtUtcMillis: Long) : SprintStartDecision
    data class Skipped(val reason: String) : SprintStartDecision
}

/**
 * 30 秒定位冲刺的编排器（WO-ANDROID-GATE-20260926 REQ-2 ~ REQ-8）。
 *
 * 设计要点（**每条都对应工单里的反面条款，改动前请先读工单**）：
 *
 * - **独立注册**：冲刺用**自己的** [LocationUpdateRequest]（≈1 秒、duration = 30 秒），
 *   绝不改写主流的注册间隔或周期锚点（AC-5.6 明令禁止「临时改间隔到 1 秒再改回」——
 *   那会把下一拍从改回时刻重新计时，周期被拉长）。
 * - **有界窗口**：窗口最长 30 秒且**不早退**（REQ-3 / A6 / D6）；窗口由周期驱动，
 *   不是不受开关控制的常驻高频模式（AC-10.3）。
 * - **运动/车载档照冲**（AC-2.5 / A8）：30 秒硬下限下窗口与周期相接、接近连续采样，
 *   属预期行为，**不得**以「过于频繁」为由跳过或裁剪窗口。因此这里对
 *   `requestIntervalMillis` **不做任何节拍检查**。
 * - **开关真生效**（AC-5.3 / AC-5.5）：每个周期都重新读 [sprintEnabledProvider]，
 *   不做一次性快照，因此切换后无需重启即生效。
 */
@Singleton
class LocationSprintController @Inject constructor(
    private val runner: LocationAcquisitionRunner,
    private val ledger: SprintLedgerPort,
    private val trackingSettingsStore: TrackingSettingsStore
) {
    private val internalScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    /** 测试接缝：注入 TestScope（与 [LocationAcquisitionCoordinator] 同一约定）。 */
    internal var testScope: CoroutineScope? = null
    private val scope: CoroutineScope get() = testScope ?: internalScope

    internal var wallClockMillis: () -> Long = { System.currentTimeMillis() }

    /**
     * 质量门来源。默认**每拍**按当前设置构造，因此「门槛固定 30 米、无可调项」
     * （REQ-7）与「海拔等待沿用既有设置」两条都保持既有语义。
     */
    internal var qualityGateProvider: () -> LocationQualityGate = {
        LocationQualityGate.fromTrackingSettings(trackingSettingsStore.read())
    }

    /**
     * 冲刺开关来源（REQ-5 / AC-5.5）。**每次发起冲刺都重新读**，
     * 因此切换后无需重启即自下一个周期起生效。
     */
    internal var sprintEnabledProvider: () -> Boolean = {
        trackingSettingsStore.read().sprintEnabled
    }

    /** 可注入的延时（测试用虚拟时钟；生产为 `delay`）。 */
    internal var delayMillis: suspend (Long) -> Unit = { delay(it) }
    /**
     * (accepted, accuracy) 回调：由采集引擎负责真正入库（逐条入库，REQ-15）。
     * 冲刺只负责「判定 + 记账」，不直接写库，避免两处入库口径分叉。
     */
    var onAccepted: (suspend (QualityAcceptedLocation, Float?) -> Unit)? = null

    /** 被质量门丢弃的 fix（reason 沿用既有编码，AC-4.3）。 */
    var onDropped: (suspend (RawLocationFix, String) -> Unit)? = null

    /** 窗口结束回调（供状态页/通知刷新）。 */
    var onWindowFinished: (suspend (SprintWindowResult) -> Unit)? = null

    private var window: LocationSprintWindow? = null
    private var windowJob: Job? = null

    /** 窗口是否开放（AC-2.3 用；也供验收方在状态页观察）。 */
    @Synchronized
    fun isWindowOpen(): Boolean = window?.isOpen == true

    /** 最近一次窗口结果（供状态页展示；未冲刺时为 null）。 */
    @Volatile
    var lastWindowResult: SprintWindowResult? = null
        private set

    /**
     * 请求发起一次冲刺（由采集周期的每一拍调用，D2）。
     *
     * @param mode 当前策略档；[LocationPolicyMode.HighSpeed] 时不冲刺（AC-2.4），
     *   [LocationPolicyMode.Off] 时记录「非采集时段」（AC-10.3）。
     * @param blockedReason 前置条件阻塞原因（权限/系统定位未就绪）；非空时记录原因而不是静默失败。
     */
    fun startSprint(
        context: AcquisitionContext,
        mode: LocationPolicyMode,
        blockedReason: String? = null,
        nowUtcMillis: Long = wallClockMillis()
    ): SprintStartDecision {
        // REQ-5 / AC-5.3：开关每拍重新读，关闭后必须**真的不再发起任何冲刺**。
        if (!sprintEnabledProvider()) {
            return skip(SprintSkipReasons.DISABLED, nowUtcMillis)
        }
        if (blockedReason != null) {
            return skip(SprintSkipReasons.PREREQUISITE_BLOCKED, nowUtcMillis)
        }
        // AC-10.3：非采集时段不冲刺。
        if (mode == LocationPolicyMode.Off) {
            return skip(SprintSkipReasons.NOT_COLLECTING, nowUtcMillis)
        }
        // AC-2.4：高速档（2.5 秒一拍）已是密集采样，不叠加冲刺。
        if (mode == LocationPolicyMode.HighSpeed) {
            return skip(SprintSkipReasons.HIGH_SPEED, nowUtcMillis)
        }
        // AC-2.3：同一时刻不得存在两个并发窗口。
        // 注意 AC-2.5：这里**没有**任何「节拍过密即跳过」的规则 —— 运动/车载档
        // （30 秒硬下限）下窗口与周期相接是需求方选定的预期行为（A8）。
        synchronized(this) {
            if (window?.isOpen == true) {
                return skip(SprintSkipReasons.WINDOW_ALREADY_OPEN, nowUtcMillis)
            }
            val fresh = LocationSprintWindow(startedAtUtcMillis = nowUtcMillis)
            window = fresh
            windowJob = scope.launch { runWindow(fresh, context) }
            return SprintStartDecision.Started(nowUtcMillis)
        }
    }

    /**
     * 中止当前窗口（采集停止 / 服务销毁 / 手动会话取消）。
     *
     * 中止**不写**已执行记录：窗口没有跑完，不该产出可被当成「已冲刺」的证据（AC-5.4 同理）。
     */
    fun abort() {
        synchronized(this) {
            window?.let { it.finish(wallClockMillis()) }
            window = null
            windowJob?.cancel()
            windowJob = null
        }
    }

    private suspend fun runWindow(active: LocationSprintWindow, context: AcquisitionContext) {
        // AC-5.6：冲刺必须独立注册；主流注册的 interval 与锚点完全不受影响。
        //
        // durationMillis 让**系统侧**知道这是一段有界请求；但 GMS 到期后只是不再回调，
        // `callbackFlow` 本身不会关闭，因此窗口的**结束必须由这里自己保证**：
        // 用 `remaining` 等待到期再取消自己的注册。绝不能依赖流自然返回 ——
        // 那样窗口永不结束，REQ-3 的台账与 AC-2.2 的跨度都无从谈起。
        val request = LocationUpdateRequest(
            priority = Priority.PRIORITY_HIGH_ACCURACY,
            intervalMillis = LocationSprintContract.SAMPLE_INTERVAL_MILLIS,
            minUpdateIntervalMillis = LocationSprintContract.MIN_SAMPLE_INTERVAL_MILLIS,
            durationMillis = LocationSprintContract.WINDOW_MILLIS
        )
        try {
            coroutineScope {
                val sprintRegistration = launch {
                    try {
                        runner.stream(request) { snapshot ->
                            if (!active.isOpen) return@stream
                            handleSample(active, snapshot, context)
                        }
                    } catch (e: CancellationException) {
                        throw e
                    } catch (_: Exception) {
                        // 注册失败不静默：窗口照常按时间结束并落台账，sampleCount 如实为 0，
                        // 验收方据此可见「这一拍没取到点」而不是以为冲刺正常。
                    }
                }
                // REQ-3：窗口**不早退** —— 无论中途是否已拿到达标/极精确的点，都等满 30 秒。
                val remaining = LocationSprintContract.WINDOW_MILLIS -
                    (wallClockMillis() - active.startedAtUtcMillis)
                if (remaining > 0L) {
                    delayMillis(remaining)
                }
                sprintRegistration.cancel()
            }
        } catch (e: CancellationException) {
            // 外部中止（abort()）：窗口未跑完，不产出「已执行」记录。
            throw e
        }
        finishWindow(active)
    }

    private suspend fun handleSample(
        active: LocationSprintWindow,
        snapshot: LocationSnapshot,
        context: AcquisitionContext
    ) {
        // §6 口径：sampleCount 统计**去重之前**的回调条数。
        // REQ-15 已取消常规流 2 秒去重，这里也没有任何去重/裁剪。
        active.onSample(snapshot)

        val fix = snapshot.toRawFix(context)
        when (val decision = qualityGateProvider().evaluate(fix, wallClockMillis())) {
            is QualityDecision.AcceptNow -> {
                active.onAccepted(decision.accepted, fix.horizontalAccuracyMeters)
                onAccepted?.invoke(decision.accepted, fix.horizontalAccuracyMeters)
            }
            is QualityDecision.WaitForAltitude -> {
                // 与流模式一致：冲刺窗口内不等待海拔（窗口有界），带标记收下，
                // 避免整个窗口被一条缺海拔的点挂住。
                val accepted = QualityAcceptedLocation(
                    fix = fix,
                    altitudeMeters = null,
                    acceptedAtMillis = wallClockMillis(),
                    qualityFlags = setOf(SPRINT_ALTITUDE_MISSING_FLAG)
                )
                active.onAccepted(accepted, fix.horizontalAccuracyMeters)
                onAccepted?.invoke(accepted, fix.horizontalAccuracyMeters)
            }
            is QualityDecision.Drop -> {
                // AC-4.3：不得因处于冲刺窗口而放宽门槛。
                onDropped?.invoke(decision.fix, decision.reason)
            }
        }
    }

    private suspend fun finishWindow(active: LocationSprintWindow) {
        val result = active.finish(wallClockMillis())
        synchronized(this) {
            if (window === active) {
                window = null
                windowJob = null
            }
        }
        lastWindowResult = result
        ledger.recordExecuted(result)
        onWindowFinished?.invoke(result)
    }

    private fun skip(reason: String, nowUtcMillis: Long): SprintStartDecision {
        scope.launch { ledger.recordSkipped(nowUtcMillis, reason) }
        return SprintStartDecision.Skipped(reason)
    }

    private fun LocationSnapshot.toRawFix(context: AcquisitionContext) = RawLocationFix(
        latitude = latitude,
        longitude = longitude,
        horizontalAccuracyMeters = horizontalAccuracyMeters,
        altitudeMeters = altitudeMeters,
        provider = provider,
        recordedAtMillis = timeMillis,
        policyMode = context.policyMode,
        scheduleLowFrequency = context.scheduleLowFrequency,
        motionSignal = context.motionSignal,
        speedMetersPerSecond = speedMetersPerSecond,
        bearingDegrees = bearingDegrees
    )

    companion object {
        /** 冲刺窗口内缺海拔的标记（沿用流模式口径，不新增语义）。 */
        const val SPRINT_ALTITUDE_MISSING_FLAG = "altitude-missing"
    }
}
