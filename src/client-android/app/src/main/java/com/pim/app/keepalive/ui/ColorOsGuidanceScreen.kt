package com.pim.app.keepalive.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Divider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.pim.app.keepalive.GuidanceItemState

/**
 * ColorOS 设置引导页的显示状态（REQ-23）。
 *
 * @param jumpFailed 跳转失败时置为 true，页面回退显示文字路径（AC-23.4）。
 */
data class GuidanceScreenState(
    val detectable: List<GuidanceItemState>,
    val manual: List<GuidanceItemState>,
    val jumpFailed: Boolean,
    val jumpFailedKey: String? = null
)

/**
 * ColorOS 设置引导页（REQ-23）。
 *
 * 两段式呈现，与工单的分类一致：
 * - **可自动检测项**：显示系统真实读数（AC-23.2），未就绪时提供跳转；
 * - **不可检测项**：显示文字路径 + 「我已完成」勾选，勾选后记住（AC-23.3）。
 *
 * 跳转失败时页面不崩溃，回退显示文字路径（AC-23.4）。
 */
@Composable
fun ColorOsGuidanceScreen(
    state: GuidanceScreenState,
    onOpenSettings: (String) -> Unit,
    onToggleManual: (String, Boolean) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
            .testTag("guidance-screen"),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("系统设置引导", style = MaterialTheme.typography.titleLarge)
        Text(
            text = "ColorOS 会限制后台活动。下面逐项检查，可自动读取的直接显示当前状态，" +
                "读不到的请按路径手动设置后勾选。",
            style = MaterialTheme.typography.bodyMedium
        )

        // AC-23.4：跳转失败不崩溃，明确告知并给出文字路径。
        if (state.jumpFailed) {
            Text(
                text = "无法自动打开该设置页，请按下面的文字路径手动前往。",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.testTag("guidance-jump-failed")
            )
        }

        Text("可自动检测", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        state.detectable.forEach { item ->
            GuidanceRow(item = item, onToggleManual = null, onOpenSettings = onOpenSettings)
            Divider()
        }

        Text("需要手动确认", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        state.manual.forEach { item ->
            GuidanceRow(item = item, onToggleManual = onToggleManual, onOpenSettings = onOpenSettings)
            Divider()
        }

        TextButton(onClick = onBack, modifier = Modifier.testTag("guidance-back")) {
            Text("返回")
        }
    }
}

@Composable
private fun GuidanceRow(
    item: GuidanceItemState,
    onToggleManual: ((String, Boolean) -> Unit)?,
    onOpenSettings: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .testTag("guidance-item-${item.item.key}")
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text(item.item.title, fontWeight = FontWeight.Medium)
                Text(
                    text = item.item.why,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(
                text = item.statusText(),
                color = if (item.satisfied) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.error
                },
                modifier = Modifier.testTag("guidance-status-${item.item.key}")
            )
        }

        // 文字路径始终显示：既是跳转失败的回退（AC-23.4），也让用户知道去哪找。
        Text(
            text = item.item.manualPath,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.testTag("guidance-path-${item.item.key}")
        )

        Row(verticalAlignment = Alignment.CenterVertically) {
            OutlinedButton(
                onClick = { onOpenSettings(item.item.key) },
                modifier = Modifier.testTag("guidance-open-${item.item.key}")
            ) {
                Text("打开设置")
            }
            if (onToggleManual != null) {
                Row(
                    modifier = Modifier
                        .clickable { onToggleManual(item.item.key, !item.manuallyCompleted) }
                        .padding(start = 8.dp)
                        .testTag("guidance-toggle-${item.item.key}"),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = item.manuallyCompleted,
                        onCheckedChange = { onToggleManual(item.item.key, it) }
                    )
                    Text("我已完成")
                }
            }
        }
    }
}
