package com.pim.app.keepalive.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.pim.app.keepalive.AlarmSuppressionPolicy
import com.pim.app.ui.components.PimSection

/**
 * 设置页「保活与诊断」分区的显示状态（REQ-22）。
 *
 * 与 ViewModel 解耦的纯数据，便于在 Robolectric/Compose 测试里构造各种状态
 * （已关闭、权限缺失、降频中等）。
 */
data class KeepAliveSectionState(
    val enabled: Boolean,
    val configuredIntervalMinutes: Int,
    val effectiveIntervalMinutes: Int,
    val backoffHint: String?,
    /** 精确闹钟权限是否已授予；null 表示本设备不适用。 */
    val exactAlarmGranted: Boolean?,
    val standbyBucketLabel: String,
    val ignoresBatteryOptimizations: Boolean?,
    val lastWakeAtUtcMillis: Long?,
    val healthAlert: String?,
    /** 台账里的登记状态文案（AC-22.2：界面显示「已关闭」）。 */
    val scheduleStatusText: String
)

/**
 * 「保活与诊断」分区（REQ-22 AC-22.1）。
 *
 * 四项内容：总开关、节奏调节、当前状态（权限/待机桶/电池优化/最近叫醒）、引导页入口。
 *
 * 文案为简体中文（AC-26.1）；关闭总开关时明确显示「已关闭」而不是留空
 * （AC-22.2：台账与界面均显示「已关闭」）。
 */
@Composable
fun KeepAliveSection(
    state: KeepAliveSectionState,
    onToggleEnabled: (Boolean) -> Unit,
    onIntervalChange: (Int) -> Unit,
    onOpenGuidance: () -> Unit,
    onOpenExactAlarmSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    PimSection("保活与诊断", modifier = modifier.testTag("settings-keepalive")) {
        // ── 总开关（AC-22.1 第一项 / AC-22.2 / AC-22.3）──
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text("保活总开关")
                Text(
                    text = if (state.enabled) "已开启：按下方节奏登记精确闹钟" else "已关闭：不登记任何闹钟",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.testTag("keepalive-enabled-text")
                )
            }
            Switch(
                checked = state.enabled,
                onCheckedChange = onToggleEnabled,
                modifier = Modifier.testTag("keepalive-enabled-switch")
            )
        }

        // ── 节奏调节（AC-22.1 第二项 / AC-15.1）──
        Column(Modifier.fillMaxWidth()) {
            Text("叫醒节奏")
            Text(
                text = "每 ${state.configuredIntervalMinutes} 分钟" +
                    if (state.effectiveIntervalMinutes != state.configuredIntervalMinutes) {
                        "（当前生效 ${state.effectiveIntervalMinutes} 分钟）"
                    } else {
                        ""
                    },
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.testTag("keepalive-interval-text")
            )
            Slider(
                value = state.configuredIntervalMinutes.toFloat(),
                onValueChange = { onIntervalChange(it.toInt()) },
                valueRange = AlarmSuppressionPolicy.MIN_INTERVAL_MINUTES.toFloat()..
                    AlarmSuppressionPolicy.MAX_INTERVAL_MINUTES.toFloat(),
                steps = (AlarmSuppressionPolicy.MAX_INTERVAL_MINUTES -
                    AlarmSuppressionPolicy.MIN_INTERVAL_MINUTES) / 10 - 1,
                enabled = state.enabled,
                modifier = Modifier.testTag("keepalive-interval-slider")
            )
            Text(
                text = "可调范围 ${AlarmSuppressionPolicy.MIN_INTERVAL_MINUTES}-" +
                    "${AlarmSuppressionPolicy.MAX_INTERVAL_MINUTES} 分钟，默认 " +
                    "${AlarmSuppressionPolicy.DEFAULT_INTERVAL_MINUTES} 分钟。",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // AC-17.2 / AC-17.3：降频中要给出提示；复位后提示消失（由 state 决定）。
        state.backoffHint?.let { hint ->
            Text(
                text = hint,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.tertiary,
                modifier = Modifier.testTag("keepalive-backoff-hint")
            )
        }

        Divider()

        // ── 当前状态（AC-22.1 第三项）──
        Text("当前状态", fontWeight = FontWeight.Medium)

        StatusRow(
            label = "登记状态",
            value = state.scheduleStatusText,
            tag = "keepalive-status-schedule"
        )
        StatusRow(
            label = "闹钟和提醒权限",
            value = when (state.exactAlarmGranted) {
                true -> "已授权"
                false -> "未授权"
                null -> "本设备不适用"
            },
            tag = "keepalive-status-permission",
            emphasise = state.exactAlarmGranted == false,
            onClick = if (state.exactAlarmGranted == false) onOpenExactAlarmSettings else null
        )
        StatusRow(
            label = "待机分区",
            value = state.standbyBucketLabel,
            tag = "keepalive-status-bucket"
        )
        StatusRow(
            label = "电池优化",
            value = when (state.ignoresBatteryOptimizations) {
                true -> "已排除（不优化）"
                false -> "未排除（系统可能限制后台）"
                null -> "无法读取"
            },
            tag = "keepalive-status-battery"
        )
        StatusRow(
            label = "最近一次叫醒",
            value = state.lastWakeAtUtcMillis?.let { "距今 ${formatAge(System.currentTimeMillis() - it)}" }
                ?: "无记录",
            tag = "keepalive-status-last-wake"
        )

        // AC-21.1：健康异常要在设置页也可见（红点之外的文字出口）。
        state.healthAlert?.let { alert ->
            Text(
                text = alert,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.testTag("keepalive-health-alert")
            )
        }

        Divider()

        // ── 引导页入口（AC-22.1 第四项 / AC-23.1 双入口之一）──
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onOpenGuidance)
                .padding(vertical = 10.dp)
                .testTag("keepalive-open-guidance"),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text("系统设置引导")
                Text(
                    text = "逐项检查 ColorOS 的后台限制设置",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Icon(
                Icons.Default.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun StatusRow(
    label: String,
    value: String,
    tag: String,
    emphasise: Boolean = false,
    onClick: (() -> Unit)? = null
) {
    val rowModifier = if (onClick != null) {
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 6.dp)
    } else {
        Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
    }
    Row(
        modifier = rowModifier,
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium)
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium,
                color = if (emphasise) {
                    MaterialTheme.colorScheme.error
                } else {
                    MaterialTheme.colorScheme.onSurface
                },
                modifier = Modifier.testTag(tag)
            )
            if (onClick != null) {
                Icon(
                    Icons.Default.KeyboardArrowRight,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

/** 相对时间文案（与阶段一存活区块口径一致）。 */
internal fun formatAge(millis: Long): String {
    val minutes = millis / 60_000L
    if (minutes < 1) return "不到 1 分钟"
    if (minutes < 60) return "$minutes 分钟"
    val hours = minutes / 60
    if (hours < 48) return "$hours 小时"
    return "${hours / 24} 天"
}
