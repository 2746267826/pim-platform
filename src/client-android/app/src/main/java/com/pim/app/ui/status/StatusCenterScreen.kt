package com.pim.app.ui.status

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pim.app.status.StatusCenterState
import com.pim.app.status.StatusIssue
import com.pim.app.status.StatusSeverity
import com.pim.app.ui.components.PimSection

@Composable
fun StatusCenterScreen(
    modifier: Modifier = Modifier,
    viewModel: StatusCenterViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    StatusCenterContent(
        state = state,
        modifier = modifier,
        onIssueAction = { issue -> viewModel.onIssueAction(issue) }
    )
}

@Composable
private fun StatusCenterContent(
    state: StatusCenterState,
    modifier: Modifier = Modifier,
    onIssueAction: (StatusIssue) -> Unit = {}
) {
    val snapshot = state.snapshot
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("状态中心", style = MaterialTheme.typography.headlineSmall)

        PimSection("需要处理") {
            if (state.issues.isEmpty()) {
                Text("当前没有阻塞问题。")
            } else {
                state.issues.forEach { issue ->
                    StatusIssueRow(issue, onAction = { onIssueAction(issue) })
                }
            }
        }

        PimSection("API 与登录") {
            Text("API 地址：${snapshot.api.address.ifBlank { "未配置" }}")
            Text("地址状态：${if (snapshot.api.isValid) "格式可用" else snapshot.api.reasonCode ?: "不可用"}")
            Text("登录状态：${if (snapshot.auth.hasAccessToken && !snapshot.auth.isExpired) "已登录" else "需要登录"}")
        }

        PimSection("权限") {
            Text("通知：${snapshot.permissions.notificationGranted.toStatusText()}")
            Text("精确定位：${snapshot.permissions.preciseLocationGranted.toStatusText()}")
            Text("后台定位：${snapshot.permissions.backgroundLocationGranted.toStatusText()}")
            Text("使用情况：${snapshot.permissions.usageAccessGranted.toStatusText()}")
            Text("运动识别：${snapshot.permissions.activityRecognitionGranted.toStatusText()}")
        }

        PimSection("前台服务") {
            Text("持续采集：${if (snapshot.service.continuousCollectionEnabled) "已开启" else "未开启"}")
            Text("服务运行：${snapshot.service.serviceRunning.toStatusText()}")
            Text("当前策略：${snapshot.tracking.currentPolicyMode}")
            Text("策略档位：${snapshot.tracking.profile}")
        }

        PimSection("上传队列") {
            Text("待上传定位：${snapshot.queues.pendingLocationPoints}")
            Text("待上传使用记录：${snapshot.queues.pendingUsageEvents + snapshot.queues.pendingUsageSummaries}")
            Text("待上传应用信息：${snapshot.queues.pendingAppMetadata}")
            Text("待上传日志：${snapshot.queues.pendingLogs}")
            Text("总待处理：${snapshot.queues.pendingUploadTotal}")
        }

        PimSection("最近诊断") {
            Text("最近丢弃原因：${snapshot.diagnostics.lastDroppedReason ?: "无"}")
            Text("心跳状态：${snapshot.diagnostics.lastHeartbeatStatus ?: "等待同步"}")
            Text("最近错误：${snapshot.diagnostics.lastLogMessage ?: "无"}")
            snapshot.diagnostics.recentLogMessages.take(5).forEach { message ->
                Text("日志：$message", style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@Composable
private fun StatusIssueRow(
    issue: StatusIssue,
    onAction: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "${issue.severity.label()} · ${issue.title}",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold
            )
            Text(issue.message, style = MaterialTheme.typography.bodySmall)
        }
        TextButton(onClick = onAction) {
            Text(issue.actionLabel)
        }
    }
}

private fun Boolean.toStatusText(): String = if (this) "已就绪" else "未就绪"

private fun StatusSeverity.label(): String = when (this) {
    StatusSeverity.Info -> "提示"
    StatusSeverity.Warning -> "警告"
    StatusSeverity.Critical -> "阻塞"
}
