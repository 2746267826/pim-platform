package com.pim.app.ui.status

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.pim.app.forensics.ForensicLivenessRepository
import com.pim.app.forensics.LivenessUiSnapshot

/**
 * 状态页顶部的存活区块（REQ-8）。
 *
 * 首屏四项：一句话结论、最近一次心搏（相对时间）、当前覆盖率、最近死因（AC-8.1）。
 * 数据陈旧时标注"数据为 N 小时前"；离线且无本地数据时显示"无数据"，不给"无异常"结论（AC-8.2）。
 */
@Composable
internal fun LivenessSection(
    snapshot: LivenessUiSnapshot?,
    onOpenDroppedReasons: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .testTag("status-liveness"),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                "设备存活",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )

            if (snapshot == null) {
                Text("正在读取本地取证台账…", modifier = Modifier.testTag("status-liveness-loading"))
                return@Column
            }

            Text(
                snapshot.conclusion,
                modifier = Modifier.testTag("status-liveness-conclusion"),
                style = MaterialTheme.typography.bodyMedium
            )

            LivenessFact(
                label = "最近心搏",
                value = snapshot.lastHeartbeatAtUtcMillis
                    ?.let { "距今 ${ForensicLivenessRepository.formatAge(currentTimeMillis() - it)}" }
                    ?: "无记录",
                tag = "status-liveness-last-heartbeat"
            )

            LivenessFact(
                label = "覆盖率 · 按小时",
                value = snapshot.coverageByHourText,
                tag = "status-liveness-coverage-hour"
            )
            LivenessFact(
                label = "覆盖率 · 按应有心跳",
                value = snapshot.coverageByExpectedHeartbeatText,
                tag = "status-liveness-coverage-heartbeat"
            )
            Text(
                snapshot.coverageByHourDefinition,
                style = MaterialTheme.typography.bodySmall
            )
            Text(
                snapshot.coverageByExpectedHeartbeatDefinition,
                style = MaterialTheme.typography.bodySmall
            )

            LivenessFact(
                label = "最近死因",
                // AC-1.3：没有读到退出记录时显示"未知"并紧跟推断依据，绝不给"无异常"式结论。
                value = snapshot.latestCauseLabel ?: "未知",
                tag = "status-liveness-last-cause"
            )
            snapshot.latestCauseInference?.let { inference ->
                Text(
                    "推断依据：$inference",
                    modifier = Modifier.testTag("status-liveness-cause-inference"),
                    style = MaterialTheme.typography.bodySmall
                )
            }

            snapshot.staleNote?.let { note ->
                Text(
                    note,
                    modifier = Modifier.testTag("status-liveness-stale"),
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Medium
                )
            }

            snapshot.exitReasonUnavailableReason?.let { reason ->
                Text(
                    reason,
                    modifier = Modifier.testTag("status-liveness-exit-unsupported"),
                    style = MaterialTheme.typography.bodySmall
                )
            }

            TextButton(
                onClick = onOpenDroppedReasons,
                modifier = Modifier.testTag("status-open-dropped-reasons")
            ) {
                Text("查看丢弃原因")
            }
        }
    }
}

@Composable
private fun LivenessFact(label: String, value: String, tag: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
        Text(value, modifier = Modifier.testTag(tag), style = MaterialTheme.typography.bodyMedium)
    }
}

private fun currentTimeMillis(): Long = System.currentTimeMillis()
