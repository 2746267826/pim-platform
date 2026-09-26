package com.pim.app.keepalive

import android.content.Context
import com.pim.app.location.acquisition.LocationAcquisitionCoordinator
import com.pim.app.location.acquisition.TriggerType
import com.pim.app.location.service.ForegroundLocationController
import com.pim.app.location.policy.LocationPolicyMode
import com.pim.app.location.service.ForegroundLocationRuntimeState
import com.pim.app.location.service.ForegroundLocationService
import com.pim.app.mobile.logs.StructuredLogRepository
import com.pim.app.mobile.sync.MobileSyncScheduler
import com.pim.app.settings.TrackingSettingsStore
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Provider
import javax.inject.Singleton
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withTimeoutOrNull

/**
 * [WakeEnvironment] 的 Android 实现（REQ-16 / REQ-20）。
 *
 * 「暂停」的判定口径：沿用既有实现事实——`ACTION_PAUSE_COLLECTION` 会把
 * `continuousCollectionEnabled` 置为 false（见 `ForegroundLocationService`），
 * 因此这里以该开关为准，而不是自造一个第二份暂停状态（否则两处会不一致）。
 *
 * 「手动采集会话进行中」的判定口径：采集协调器的当前会话是人工触发（`TriggerType` 为手动）
 * 且仍在进行（`phase` 忙）时，认为用户正在进行手动采集。
 */
@Singleton
class AndroidWakeEnvironment @Inject constructor(
    @ApplicationContext private val context: Context,
    private val trackingSettingsStore: TrackingSettingsStore,
    private val controller: ForegroundLocationController,
    private val syncScheduler: Provider<MobileSyncScheduler>,
    private val acquisitionCoordinator: Provider<LocationAcquisitionCoordinator>,
    private val logs: StructuredLogRepository
) : WakeEnvironment {

    override suspend fun isPaused(): Boolean = try {
        // 与 ForegroundLocationService.ACTION_PAUSE_COLLECTION 设置的是同一个开关。
        !trackingSettingsStore.read().continuousCollectionEnabled
    } catch (ex: Exception) {
        // **fail-closed**：读不到暂停状态时按「已暂停」处理，即不拉起服务、不采集。
        //
        // 理由：REQ-20 是保护用户显式意图的条款（用户暂停了就不许偷偷拉起）。
        // 取 fail-open（按未暂停处理）会在读数失败时违背这个意图——而读数失败本身
        // 已经说明出了异常，此时更不该擅自启动采集。宁可少叫醒一次（下个周期会重试，
        // 且红点会提示），也不能违背用户明确表达的「暂停」。
        logs.warn("keepalive", "读取暂停状态失败，按已暂停处理（fail-closed）：${ex.message ?: ""}")
        true
    }

    override suspend fun isManualSessionActive(): Boolean = try {
        val state = acquisitionCoordinator.get().state.value
        state.isBusy && state.triggerType == TriggerType.MANUAL
    } catch (ex: Exception) {
        // **fail-closed**：读不到会话状态时按「有手动会话」处理，跳过本次叫醒。
        // 理由同 [isPaused]：AC-20.2 要求不得打断用户正在进行的会话，
        // 而「读不到」意味着我们无法确认没有会话，此时不打断才是安全的一侧。
        logs.warn("keepalive", "读取采集会话语义失败，按存在手动会话处理（fail-closed）：${ex.message ?: ""}")
        true
    }

    override fun isForegroundServiceRunning(): Boolean = try {
        ForegroundLocationService.isRunning()
    } catch (ex: Exception) {
        false
    }

    override suspend fun requestSyncNow(): Boolean = try {
        syncScheduler.get().enqueueNow()
        true
    } catch (ex: Exception) {
        logs.warn("keepalive", "叫醒后请求补传失败：${ex.message ?: ""}")
        false
    }

    /**
     * 拉起前台采集服务**并确认它真的起来了**。
     *
     * `ForegroundLocationController.start()` 只是 `startForegroundService()`（异步派发 Intent），
     * 返回成功并不代表服务已进入前台。若据此直接报告成功，会把「服务其实没起来」
     * 记成 `executed`（false-green），并跳过 AC-16.3 要求的兜底抓点。
     * 因此这里发完 Intent 后**等一下确认**（事件驱动，不是定时轮询）。
     */
    override suspend fun startForegroundService(): Boolean = try {
        controller.start()

        // 等待服务**真的**进入运行态，而不是假定它成功了。
        //
        // 只等 `isRunning == true` 是不够的：`ForegroundLocationService.onCreate()` 会先
        // 发布 `isRunning = true`，而真正的启动检查（权限、定位开关、Play 服务）发生在
        // `onStartCommand → startCollection`，失败时它会把状态改成 `isRunning = false`
        // 且 `currentPolicyMode = Off` 并 `stopSelf`。若只等第一个 true，
        // 就会把「起来又立刻倒下」记成 executed（false-green）并跳过 AC-16.3 兜底。
        //
        // 因此这里等的是**已进入实际采集策略**：`isRunning && mode != Off`。
        // 仍用 StateFlow 事件驱动（`first{...}` + 超时），不写 `while + delay` 轮询
        // ——AC-29.1 明确禁止新增定时轮询，且这是可 grep 核对的。
        val running = withTimeoutOrNull(START_CONFIRM_TIMEOUT_MILLIS) {
            ForegroundLocationService.runtimeState.first {
                it.isRunning && it.currentPolicyMode != LocationPolicyMode.Off.name
            }
        } != null

        if (!running) {
            logs.warn(
                "keepalive",
                "已发出拉起指令，但 ${START_CONFIRM_TIMEOUT_MILLIS}ms 内采集服务未进入运行态"
            )
        }
        running
    } catch (ex: CancellationException) {
        throw ex
    } catch (ex: Exception) {
        // Android 12+ 后台启动前台服务受限，但精确闹钟在豁免清单内（平台依据 §2）；
        // 若仍被系统拒绝，如实返回 false，由执行链走 AC-16.3 的兜底。
        logs.warn("keepalive", "拉起前台采集服务失败：${ex.message ?: ex::class.java.simpleName}")
        false
    }

    override suspend fun captureSinglePointFallback(): Boolean = try {
        // 兜底：请求协调器做一次单点采集（不依赖前台服务是否成功起来）。
        // Started 才算真的抓到了；Busy/Rejected 都视为兜底失败（如实记账，不假成功）。
        acquisitionCoordinator.get().startManualSession() is
            com.pim.app.location.acquisition.SessionStartResult.Started
    } catch (ex: CancellationException) {
        throw ex
    } catch (ex: Exception) {
        logs.warn("keepalive", "兜底抓取定位点失败：${ex.message ?: ex::class.java.simpleName}")
        false
    }

    /** 当前运行状态（供设置页「当前状态」展示，AC-22.1）。 */
    fun runtimeState(): ForegroundLocationRuntimeState = ForegroundLocationService.runtimeState.value

    private companion object {
        /**
         * 拉起后确认服务进入运行态的等待上限。
         *
         * 这是一个**工程实现常量**，不是需求参数：工单只要求「叫醒后前台采集服务重新运行」
         * （AC-16.2），没有规定等多长时间。它取 5 秒的理由是：
         * - 前台服务的 `onCreate → startForeground` 在同一进程内是毫秒级操作，5 秒足够宽松；
         * - 不能无限等：等太久会让本次叫醒一直占着广播的 `goAsync` 预算，
         *   反而拖慢下一次续登记（而续登记是保活的核心）。
         * 因此它是「保守的等待上限」而非判定阈值；超时只意味着「这次没等到」，
         * 执行链会如实记为 `pull-failed` 并走 AC-16.3 的兜底，不会谎报成功。
         */
        const val START_CONFIRM_TIMEOUT_MILLIS = 5_000L

        // 刻意不设「轮询间隔」常量：确认方式是 StateFlow 事件等待，不是轮询。
    }
}
