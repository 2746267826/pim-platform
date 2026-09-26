package com.pim.app.keepalive.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pim.app.keepalive.AlarmSuppressionPolicy
import com.pim.app.keepalive.ColorOsGuidanceCatalog
import com.pim.app.keepalive.ExactAlarmPermissionChecker
import com.pim.app.keepalive.ExactAlarmPermissionState
import com.pim.app.keepalive.GuidanceDetector
import com.pim.app.keepalive.KeepAliveCoordinator
import com.pim.app.keepalive.KeepAliveHealthMonitor
import com.pim.app.keepalive.KeepAliveLedger
import com.pim.app.keepalive.KeepAliveScheduleOutcome
import com.pim.app.keepalive.KeepAliveSettingsAccessor
import com.pim.app.keepalive.KeepAliveSettingsStore
import com.pim.app.mobile.logs.StructuredLogRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * 「保活与诊断」分区与引导页的取数（REQ-22 / REQ-23）。
 *
 * 只读本地状态并转发用户操作；每个异步动作都自行处理失败并给出可见出口（REQ-28），
 * 不让异常冒到 UI 层变成崩溃或静默无反应。
 */
@HiltViewModel
class KeepAliveViewModel @Inject constructor(
    private val settingsStore: KeepAliveSettingsStore,
    private val coordinator: KeepAliveCoordinator,
    private val ledger: KeepAliveLedger,
    private val guidanceDetector: GuidanceDetector,
    private val permissionChecker: ExactAlarmPermissionChecker,
    private val health: KeepAliveHealthMonitor,
    private val logs: StructuredLogRepository
) : ViewModel() {

    private val _sectionState = MutableStateFlow(emptyKeepAliveSectionState())
    val sectionState: StateFlow<KeepAliveSectionState> = _sectionState.asStateFlow()

    /** 红点文案（null = 不点亮）；状态页顶部与设置页共用同一来源。 */
    private val _healthAlert = MutableStateFlow<String?>(null)
    val healthAlert: StateFlow<String?> = _healthAlert.asStateFlow()

    private val _guidanceState = MutableStateFlow(
        GuidanceScreenState(detectable = emptyList(), manual = emptyList(), jumpFailed = false)
    )
    val guidanceState: StateFlow<GuidanceScreenState> = _guidanceState.asStateFlow()

    init {
        refresh()
    }

    /** 重新读取全部状态（进入页面 / 返回前台时调用）。 */
    fun refresh() {
        viewModelScope.launch {
            try {
                val settings = settingsStore.read()
                val lastWake = ledger.lastFulfillmentAtUtc()
                val configured = settings.configuredIntervalMinutes
                val effective = settings.effectiveIntervalMinutes

                _healthAlert.value = health.summaryText()

                _sectionState.value = KeepAliveSectionState(
                    enabled = settings.enabled,
                    configuredIntervalMinutes = configured,
                    effectiveIntervalMinutes = effective,
                    backoffHint = if (AlarmSuppressionPolicy.isBackedOff(configured, effective)) {
                        AlarmSuppressionPolicy.backoffHint(configured, effective)
                    } else {
                        null
                    },
                    exactAlarmGranted = guidanceDetector.exactAlarmGranted(),
                    standbyBucketLabel = guidanceDetector.standbyBucketLabel(),
                    ignoresBatteryOptimizations = guidanceDetector.ignoresBatteryOptimizations(),
                    lastWakeAtUtcMillis = lastWake,
                    healthAlert = health.summaryText(),
                    scheduleStatusText = scheduleStatusText(settings.enabled)
                )

                _guidanceState.value = GuidanceScreenState(
                    detectable = guidanceDetector.detectableStates(settings.completedGuidanceKeys),
                    manual = guidanceDetector.manualStates(settings.completedGuidanceKeys),
                    jumpFailed = _guidanceState.value.jumpFailed,
                    jumpFailedKey = _guidanceState.value.jumpFailedKey
                )
            } catch (ex: CancellationException) {
                throw ex
            } catch (ex: Exception) {
                // REQ-28：取数失败必须有可见出口。
                logs.error("keepalive", "读取保活设置失败：${ex.message ?: ""}", ex)
                _sectionState.value = _sectionState.value.copy(
                    healthAlert = "读取保活设置失败：${ex.message ?: "未知错误"}"
                )
            }
        }
    }

    /**
     * 切换总开关（AC-22.2 / AC-22.3）。
     * 打开后立即恢复登记，关闭后不登记任何闹钟。
     */
    fun setEnabled(enabled: Boolean) {
        viewModelScope.launch {
            try {
                settingsStore.setEnabled(enabled)
                val outcome = if (enabled) {
                    coordinator.reconcile("user-toggle-on")
                } else {
                    coordinator.scheduleNext("user-toggle-off")
                }
                logs.info("keepalive", "保活总开关${if (enabled) "打开" else "关闭"}，结果：$outcome")
                refresh()
            } catch (ex: CancellationException) {
                throw ex
            } catch (ex: Exception) {
                logs.error("keepalive", "切换保活总开关失败：${ex.message ?: ""}", ex)
                refresh()
            }
        }
    }

    /** 调整节奏（AC-15.1）。改动后按新值重新登记，使下一个周期立即生效。 */
    fun setIntervalMinutes(minutes: Int) {
        viewModelScope.launch {
            try {
                settingsStore.setConfiguredInterval(minutes)
                coordinator.scheduleNext("user-interval-change")
                refresh()
            } catch (ex: CancellationException) {
                throw ex
            } catch (ex: Exception) {
                logs.error("keepalive", "调整保活节奏失败：${ex.message ?: ""}", ex)
                refresh()
            }
        }
    }

    /** 打开某个引导项对应的系统设置页；失败时置 jumpFailed，页面回退文字路径（AC-23.4）。 */
    fun openSettingsFor(key: String) {
        viewModelScope.launch {
            val opened = try {
                when (key) {
                    // 只有闹钟权限有专用 Intent；其余项目前只能给文字路径。
                    ColorOsGuidanceCatalog.EXACT_ALARM -> permissionChecker.openSettings()
                    else -> false
                }
            } catch (ex: CancellationException) {
                throw ex
            } catch (ex: Exception) {
                logs.warn("keepalive", "打开引导项设置失败（$key）：${ex.message ?: ""}")
                false
            }

            if (!opened) {
                // AC-23.4：跳转失败时页面不崩溃，回退显示文字路径。
                _guidanceState.value = _guidanceState.value.copy(jumpFailed = true, jumpFailedKey = key)
            }
        }
    }

    /** 手动勾选「我已完成」（AC-23.3：记住状态）。 */
    fun setManualCompleted(key: String, completed: Boolean) {
        viewModelScope.launch {
            try {
                if (completed) {
                    settingsStore.markGuidanceCompleted(key)
                } else {
                    settingsStore.clearGuidanceCompleted(key)
                }
                refresh()
            } catch (ex: CancellationException) {
                throw ex
            } catch (ex: Exception) {
                logs.error("keepalive", "记录引导完成状态失败：${ex.message ?: ""}", ex)
            }
        }
    }

    /** 权限状态文案（AC-22.2：关闭时界面显示「已关闭」）。 */
    private fun scheduleStatusText(enabled: Boolean): String = if (!enabled) {
        "已关闭"
    } else {
        when (permissionChecker.state()) {
            ExactAlarmPermissionState.GRANTED -> "已登记"
            ExactAlarmPermissionState.DENIED -> "未登记（缺少「闹钟和提醒」权限）"
            ExactAlarmPermissionState.NOT_APPLICABLE -> "本设备不支持精确闹钟"
        }
    }
}

/** 空态（用于首帧，避免闪烁出错误数值）。 */
private fun emptyKeepAliveSectionState(): KeepAliveSectionState = KeepAliveSectionState(
    enabled = false,
    configuredIntervalMinutes = AlarmSuppressionPolicy.DEFAULT_INTERVAL_MINUTES,
    effectiveIntervalMinutes = AlarmSuppressionPolicy.DEFAULT_INTERVAL_MINUTES,
    backoffHint = null,
    exactAlarmGranted = null,
    standbyBucketLabel = "读取中…",
    ignoresBatteryOptimizations = null,
    lastWakeAtUtcMillis = null,
    healthAlert = null,
    scheduleStatusText = "读取中…"
)
