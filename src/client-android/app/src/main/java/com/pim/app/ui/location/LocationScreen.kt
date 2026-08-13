package com.pim.app.ui.location

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Button
import androidx.compose.material3.Divider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Composable
fun LocationScreen(
    modifier: Modifier = Modifier,
    onOpenSettings: () -> Unit = {},
    viewModel: LocationViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    LocationScreen(
        state = state,
        onStart = { viewModel.startOrRestart() },
        onCancel = { viewModel.cancel() },
        onRestart = { viewModel.startOrRestart() },
        onOpenSettings = onOpenSettings,
        modifier = modifier
    )
}

@Composable
internal fun LocationScreen(
    state: LocationUiState,
    onStart: () -> Unit,
    onCancel: () -> Unit,
    onRestart: () -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier.padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item { StatusSection(state) }

        item { Divider() }

        item { BestLocationSection(state) }

        item { Divider() }

        item { ActionsSection(state, onStart, onCancel, onRestart, onOpenSettings) }

        item { Divider() }

        item { QueueSection(state) }
    }
}

@Composable
private fun StatusSection(state: LocationUiState) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("location-status-section")
    ) {
        Text("定位状态", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)

        Spacer(Modifier.height(4.dp))

        Row(modifier = Modifier.fillMaxWidth()) {
            Text("触发方式", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.width(8.dp))
            Text(state.triggerLabel, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium)
        }

        Row(modifier = Modifier.fillMaxWidth()) {
            Text("阶段", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.width(8.dp))
            Text(state.phaseLabel, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium)
        }

        Row(modifier = Modifier.fillMaxWidth()) {
            Text("已用时间", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.width(8.dp))
            Text(state.elapsedText, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium)
        }

        Row(modifier = Modifier.fillMaxWidth()) {
            Text("截止时间", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.width(8.dp))
            Text(state.deadlineText, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium)
        }
    }
}

@Composable
private fun BestLocationSection(state: LocationUiState) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("location-best-section")
    ) {
        Text("最佳位置", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)

        Spacer(Modifier.height(4.dp))

        val best = state.bestLocation
        if (best != null) {
            FieldRow("精度", formatOrPlaceholder(best.horizontalAccuracyMeters, "%.1f 米"), "location-accuracy")
            FieldRow("Provider", best.provider, "location-provider")
            FieldRow("纬度", "%.6f".format(best.latitude), "location-latitude")
            FieldRow("经度", "%.6f".format(best.longitude), "location-longitude")
            FieldRow("海拔", formatOrPlaceholder(best.altitudeMeters, "%.1f 米"), "location-altitude")
            FieldRow("速度", formatOrPlaceholder(best.speedMetersPerSecond, "%.1f m/s"), "location-speed")
            FieldRow("方位角", formatOrPlaceholder(best.bearingDegrees, "%.1f°"), "location-bearing")
            FieldRow("记录时间", formatRecordedTime(best.timeMillis), "location-recorded-time")
        } else {
            Text(
                "暂无最佳位置",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun ActionsSection(
    state: LocationUiState,
    onStart: () -> Unit,
    onCancel: () -> Unit,
    onRestart: () -> Unit,
    onOpenSettings: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("location-actions-section"),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text("操作", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)

        if (state.showStart) {
            Button(
                onClick = onStart,
                modifier = Modifier.fillMaxWidth().testTag("location-start"),
                enabled = state.manualStartEnabled
            ) {
                Text(if (state.manualStartEnabled) "开始定位" else "定位进行中")
            }
        }

        if (state.showCancel) {
            Button(
                onClick = onCancel,
                modifier = Modifier.fillMaxWidth().testTag("location-cancel")
            ) {
                Text("取消")
            }
        }

        if (state.showRestart) {
            OutlinedButton(
                onClick = onRestart,
                modifier = Modifier.fillMaxWidth().testTag("location-restart")
            ) {
                Text("重新定位")
            }
        }

        if (state.showOpenSettings) {
            Button(
                onClick = onOpenSettings,
                modifier = Modifier.fillMaxWidth().testTag("location-open-settings")
            ) {
                Text("打开设置")
            }
        }

        if (state.showLowQualityWarning) {
            Text(
                "精度不足，已标记低质量",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("location-low-quality-warning")
            )
        }

        if (state.errorMessage != null) {
            Text(
                state.errorMessage,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(top = 4.dp)
            )
        }
    }
}

@Composable
private fun QueueSection(state: LocationUiState) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("location-queue-section")
    ) {
        Text("上传队列", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)

        Spacer(Modifier.height(4.dp))

        Row(modifier = Modifier.fillMaxWidth()) {
            Text("待传总数", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.width(8.dp))
            Text(
                state.pendingUploadTotal.toString(),
                modifier = Modifier.testTag("location-pending-total"),
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Medium
            )
        }

        Row(modifier = Modifier.fillMaxWidth()) {
            Text("定位记录数", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.width(8.dp))
            Text(
                state.pendingLocationPoints.toString(),
                modifier = Modifier.testTag("location-pending-points"),
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
private fun FieldRow(label: String, value: String, tag: String) {
    Row(modifier = Modifier.fillMaxWidth()) {
        Text(
            label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(72.dp)
        )
        Text(
            value,
            modifier = Modifier.testTag(tag),
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

private fun formatOrPlaceholder(value: Float?, format: String): String {
    return if (value != null) format.format(value) else "暂无"
}

private fun formatOrPlaceholder(value: Double?, format: String): String {
    return if (value != null) format.format(value) else "暂无"
}

private fun formatRecordedTime(epochMillis: Long): String {
    return try {
        val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
            .withZone(ZoneId.systemDefault())
        formatter.format(Instant.ofEpochMilli(epochMillis))
    } catch (_: Exception) {
        epochMillis.toString()
    }
}
