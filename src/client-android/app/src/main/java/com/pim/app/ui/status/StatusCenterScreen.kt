package com.pim.app.ui.status

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pim.app.status.StatusActionRoute
import com.pim.app.status.StatusActionRouter
import com.pim.app.status.StatusCenterState
import com.pim.app.status.StatusIssue
import com.pim.app.status.StatusPermissionNavigator
import com.pim.app.status.StatusSeverity
import com.pim.app.ui.components.PimSection

@Composable
fun StatusCenterScreen(
    modifier: Modifier = Modifier,
    onOpenSettings: () -> Unit = {},
    onOpenStatus: () -> Unit = {},
    viewModel: StatusCenterViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    LaunchedEffect(Unit) {
        viewModel.refresh()
    }
    val state by viewModel.state.collectAsStateWithLifecycle()
    val expandedCodes = remember { mutableStateMapOf<String, Boolean>() }
    var isSyncing by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(state.snapshot) {
        isSyncing = false
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("状态中心", style = MaterialTheme.typography.headlineSmall)
            Button(
                onClick = {
                    if (!isSyncing) {
                        isSyncing = true
                        viewModel.syncNow()
                    }
                },
                enabled = !isSyncing
            ) {
                Icon(Icons.Filled.Send, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text(if (isSyncing) "同步中" else "立即同步")
            }
        }

        PimSection("需要处理") {
            if (state.issues.isEmpty()) {
                Text("当前没有阻塞问题。")
            } else {
                state.issues.forEach { issue ->
                    val expanded = expandedCodes[issue.code] == true
                    StatusIssueRow(
                        issue = issue,
                        expanded = expanded,
                        onAction = {
                            when (StatusActionRouter.route(viewModel.onIssueAction(issue))) {
                                StatusActionRoute.OpenSettings -> onOpenSettings()
                                StatusActionRoute.OpenPermissions ->
                                    StatusPermissionNavigator.open(context, issue)
                                StatusActionRoute.TriggerSync -> viewModel.syncNow()
                                StatusActionRoute.StayOnStatus ->
                                    expandedCodes[issue.code] = !expanded
                                StatusActionRoute.None -> Unit
                            }
                        },
                        onToggleDetail = { expandedCodes[issue.code] = !expanded }
                    )
                }
            }
        }

        StatusCenterSnapshotSection(state)
    }
}

@Composable
private fun StatusCenterSnapshotSection(state: StatusCenterState) {
    val snapshot = state.snapshot
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

@Composable
private fun StatusIssueRow(
    issue: StatusIssue,
    expanded: Boolean,
    onAction: () -> Unit,
    onToggleDetail: () -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier
                    .weight(1f)
                    .clickable { onToggleDetail() },
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f, fill = false)) {
                    Text(
                        text = "${issue.severity.label()} · ${issue.title}",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(issue.message, style = MaterialTheme.typography.bodySmall)
                }
                IconButton(onClick = onToggleDetail) {
                    Icon(
                        imageVector = if (expanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                        contentDescription = if (expanded) "收起详情" else "查看详情"
                    )
                }
            }
            TextButton(onClick = onAction) {
                Text(issue.actionLabel)
            }
        }
        AnimatedVisibility(visible = expanded) {
            Column(modifier = Modifier.fillMaxWidth().padding(start = 4.dp, end = 4.dp, bottom = 8.dp)) {
                Text("代码：${issue.code}", style = MaterialTheme.typography.bodySmall)
                issue.lastOccurredAtMillis?.let {
                    Text("最近发生：${formatMillis(it)}", style = MaterialTheme.typography.bodySmall)
                }
                if (issue.message != issue.title) {
                    Text("完整描述：${issue.message}", style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}

private fun formatMillis(millis: Long): String {
    val sdf = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault())
    return sdf.format(java.util.Date(millis))
}

private fun Boolean.toStatusText(): String = if (this) "已就绪" else "未就绪"

private fun StatusSeverity.label(): String = when (this) {
    StatusSeverity.Info -> "提示"
    StatusSeverity.Warning -> "警告"
    StatusSeverity.Critical -> "阻塞"
}
