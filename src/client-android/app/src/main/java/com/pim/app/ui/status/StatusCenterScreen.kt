package com.pim.app.ui.status

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.NetworkCheck
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Sensors
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.key
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pim.app.status.ConnectionProbeResult
import com.pim.app.status.DiagnosticSnapshot
import com.pim.app.status.NetworkAvailability
import com.pim.app.status.NetworkSettingsNavigator
import com.pim.app.status.PermissionStatusSnapshot
import com.pim.app.status.StatusActionRoute
import com.pim.app.status.StatusActionRouter
import com.pim.app.status.StatusActionTarget
import com.pim.app.status.StatusCenterState
import com.pim.app.status.StatusDisplayText
import com.pim.app.status.StatusIssue
import com.pim.app.status.StatusOverall
import com.pim.app.status.StatusPermissionNavigator
import com.pim.app.status.StatusSeverity
import com.pim.app.status.SyncPhase
import com.pim.app.status.actionableStatusIssues
import com.pim.app.status.informationalStatusIssues
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.yield

@Composable
fun StatusCenterScreen(
    modifier: Modifier = Modifier,
    onOpenSettings: () -> Unit = {},
    viewModel: StatusCenterViewModel = hiltViewModel()
) {
    val context = LocalContext.current

    LaunchedEffect(viewModel) {
        while (isActive) {
            val delayMillis = viewModel.refreshConnectionForVisibleScreen()
            if (delayMillis > 0L) delay(delayMillis) else yield()
        }
    }
    val state by viewModel.state.collectAsStateWithLifecycle()
    val feedback by viewModel.feedback.collectAsStateWithLifecycle()
    StatusCenterContent(
        state = state,
        feedback = feedback,
        modifier = modifier,
        onIssueAction = { issue ->
            when (StatusActionRouter.route(viewModel.onIssueAction(issue))) {
                StatusActionRoute.OpenSettings -> onOpenSettings()
                StatusActionRoute.OpenPermissions -> StatusPermissionNavigator.open(context, issue)
                StatusActionRoute.TriggerSync -> viewModel.syncNow()
                StatusActionRoute.OpenNetworkSettings -> {
                    NetworkSettingsNavigator.open(context)
                }
                StatusActionRoute.ConnectionCheck -> viewModel.forceConnectionProbe()
                StatusActionRoute.None -> Unit
            }
        },
        onSyncNow = { viewModel.syncNow() }
    )
}

@Composable
internal fun StatusCenterContent(
    state: StatusCenterState,
    feedback: StatusActionFeedback? = null,
    modifier: Modifier = Modifier,
    onIssueAction: (StatusIssue) -> Unit = {},
    onSyncNow: () -> Unit = {}
) {
    val snapshot = state.snapshot
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            "状态中心",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )

        OverallStatusSurface(state)

        feedback?.let {
            FeedbackRow(it)
        }

        Divider()

        TransportSection(state, onSyncNow)

        Divider()

        CollectionAndConnectionSection(state)

        Divider()

        DiagnosticsSection(snapshot.diagnostics)

        Divider()

        IssuesSection(state.issues, onIssueAction)
    }
}

@Composable
private fun FeedbackRow(feedback: StatusActionFeedback) {
    val text = when (feedback) {
        StatusActionFeedback.ProbeChecking -> "检查中"
        StatusActionFeedback.ProbeCompleted -> "检查已完成"
        StatusActionFeedback.ProbeFailed -> "检查未完成，请稍后重试"
        StatusActionFeedback.SyncSubmitFailed -> "同步请求未能提交，请稍后重试"
    }
    Surface(
        modifier = Modifier.fillMaxWidth().testTag("status-feedback"),
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.secondaryContainer,
        tonalElevation = 2.dp
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(12.dp),
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

@Composable
private fun OverallStatusSurface(state: StatusCenterState) {
    if (state.isLoading) {
        Surface(
            modifier = Modifier.fillMaxWidth().testTag("status-loading"),
            shape = RoundedCornerShape(8.dp),
            color = when (state.overall) {
                StatusOverall.Normal -> MaterialTheme.colorScheme.primaryContainer
                StatusOverall.Attention -> MaterialTheme.colorScheme.tertiaryContainer
                StatusOverall.Abnormal -> MaterialTheme.colorScheme.errorContainer
            },
            tonalElevation = 2.dp
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                Spacer(Modifier.width(12.dp))
                Text("正在读取状态", style = MaterialTheme.typography.bodyMedium)
            }
        }
    } else {
        Surface(
            modifier = Modifier.fillMaxWidth().testTag("status-overall"),
            shape = RoundedCornerShape(8.dp),
            color = MaterialTheme.colorScheme.surfaceVariant,
            tonalElevation = 2.dp
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = when (state.overall) {
                        StatusOverall.Normal -> Icons.Filled.CheckCircle
                        StatusOverall.Attention -> Icons.Filled.Warning
                        StatusOverall.Abnormal -> Icons.Filled.CloudOff
                    },
                    contentDescription = null,
                    modifier = Modifier.size(28.dp),
                    tint = when (state.overall) {
                        StatusOverall.Normal -> MaterialTheme.colorScheme.primary
                        StatusOverall.Attention -> MaterialTheme.colorScheme.tertiary
                        StatusOverall.Abnormal -> MaterialTheme.colorScheme.error
                    }
                )
                Spacer(Modifier.width(12.dp))
                Column {
                    Text(
                        when (state.overall) {
                            StatusOverall.Normal -> "正常"
                            StatusOverall.Attention -> "需注意"
                            StatusOverall.Abnormal -> "异常"
                        },
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        summaryText(state),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2
                    )
                }
            }
        }
    }
}

private fun summaryText(state: StatusCenterState): String {
    val parts = mutableListOf<String>()
    val criticalCount = state.issues.count { it.severity == StatusSeverity.Critical }
    val warningCount = state.issues.count { it.severity == StatusSeverity.Warning }
    if (criticalCount > 0) parts += "${criticalCount}个阻塞"
    if (warningCount > 0) parts += "${warningCount}个警告"
    if (parts.isEmpty()) parts += "一切正常"
    return parts.joinToString("，")
}

@Composable
private fun TransportSection(
    state: StatusCenterState,
    onSyncNow: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        SectionHeader("数据传输", Icons.Filled.CloudSync)

        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = syncPhaseLabel(state.syncPhase),
                modifier = Modifier.weight(1f).testTag("status-sync-phase"),
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Button(
                onClick = onSyncNow,
                modifier = Modifier.width(144.dp).testTag("status-sync-button"),
                enabled = syncButtonEnabled(state)
            ) {
                Icon(Icons.Filled.Sync, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(4.dp))
                Text(
                    text = syncButtonLabel(state.syncPhase),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            CountChip("待传", state.pendingTotal.toString(), "status-pending", Modifier.weight(1f))
            CountChip("本轮确认", state.acceptedCount.toString(), "status-confirmed", Modifier.weight(1f))
            CountChip("本轮拒绝", state.rejectedCount.toString(), "status-rejected", Modifier.weight(1f))
            CountChip("永久拒绝", state.permanentRejectedCount.toString(), "status-permanent-rejected", Modifier.weight(1f))
        }

        FactRow("上次成功", formatRecordedTime(state.lastSuccessfulUploadAt), "status-last-success")
        FactRow("上次尝试", formatRecordedTime(state.lastAttemptedUploadAt), "status-last-attempt")
        FactRow(
            "下次尝试",
            state.nextAttemptAtMillis?.takeIf { it > 0L }?.let(::formatEpochMillis) ?: "未安排",
            "status-next-attempt"
        )
    }
}

internal fun syncPhaseLabel(phase: SyncPhase): String = when (phase) {
    SyncPhase.Idle -> "当前空闲"
    SyncPhase.Accepted -> "请求已接受"
    SyncPhase.Waiting -> "等待网络或系统调度"
    SyncPhase.Running -> "同步中"
    SyncPhase.Blocked -> "同步条件未满足"
    SyncPhase.Completed -> "同步已完成"
    SyncPhase.Failed -> "同步失败"
    SyncPhase.Cancelled -> "已取消"
}

internal fun syncButtonLabel(phase: SyncPhase): String = when (phase) {
    SyncPhase.Idle -> "立即同步"
    SyncPhase.Accepted -> "请求已接受"
    SyncPhase.Waiting -> "等待中"
    SyncPhase.Running -> "同步中"
    SyncPhase.Blocked -> "暂不可同步"
    SyncPhase.Completed -> "再次同步"
    SyncPhase.Failed -> "重新同步"
    SyncPhase.Cancelled -> "再次同步"
}

private fun syncButtonEnabled(phase: SyncPhase): Boolean = when (phase) {
    SyncPhase.Idle, SyncPhase.Completed, SyncPhase.Failed, SyncPhase.Cancelled -> true
    SyncPhase.Accepted, SyncPhase.Waiting, SyncPhase.Running, SyncPhase.Blocked -> false
}

internal fun syncButtonEnabled(state: StatusCenterState): Boolean {
    if (state.isLoading) return false
    return syncButtonEnabled(state.syncPhase)
}

@Composable
private fun CollectionAndConnectionSection(state: StatusCenterState) {
    val snap = state.snapshot
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        SectionHeader("采集与连接", Icons.Filled.Sensors)

        FactRow("服务器地址", snap.api.address.ifBlank { "未配置" })
        FactRow(
            "地址状态",
            if (snap.api.isValid) "格式可用" else StatusDisplayText.apiReason(snap.api.reasonCode),
            "status-api-reason"
        )
        FactRow("登录状态",
            if (snap.auth.hasAccessToken && !snap.auth.isExpired) "已登录"
            else if (snap.auth.hasAccessToken) "已过期"
            else "未登录"
        )
        FactRow("持续采集", if (snap.service.continuousCollectionEnabled) "已开启" else "未开启")
        FactRow("服务运行", if (snap.service.serviceRunning) "运行中" else "已停止")
        FactRow("策略档位", StatusDisplayText.profile(snap.tracking.profile), "status-tracking-profile")
        FactRow("当前策略", StatusDisplayText.policyMode(snap.tracking.currentPolicyMode), "status-policy-mode")
        FactRow(
            "下次定位",
            snap.tracking.nextExpectedLocationAtMillis?.takeIf { it > 0L }?.let(::formatEpochMillis) ?: "未安排",
            "status-next-location"
        )

        PermissionsSection(snap.permissions)

        FactRow("系统网络", when (state.networkAvailability) {
            NetworkAvailability.Unavailable -> "不可用"
            NetworkAvailability.Restricted -> "受限"
            NetworkAvailability.Validated -> "已验证"
        }, "status-network")

        ProbeSection(state.lastProbeResult, state.lastProbeCheckedAtMillis)
    }
}

@Composable
private fun PermissionsSection(permissions: PermissionStatusSnapshot) {
    PermissionRow("通知", permissions.notificationGranted, "status-permission-notification")
    PermissionRow("精确定位", permissions.preciseLocationGranted, "status-permission-precise-location")
    PermissionRow("后台定位", permissions.backgroundLocationGranted, "status-permission-background-location")
    PermissionRow("使用情况", permissions.usageAccessGranted, "status-permission-usage-access")
    PermissionRow("运动识别", permissions.activityRecognitionGranted, "status-permission-activity-recognition")
    PermissionRow("电池优化", permissions.batteryOptimizationGranted, "status-permission-battery-optimization")
}

@Composable
private fun PermissionRow(label: String, granted: Boolean, tag: String) {
    FactRow("权限 $label", if (granted) "已就绪" else "未就绪", tag)
}

@Composable
private fun ProbeSection(result: ConnectionProbeResult?, checkedAtMillis: Long?) {
    val outcomeText = when (result?.outcome) {
        com.pim.app.status.ConnectionProbeOutcome.Reachable -> "可达"
        com.pim.app.status.ConnectionProbeOutcome.Partial -> "部分可达"
        com.pim.app.status.ConnectionProbeOutcome.Blocked -> "不可达"
        null -> "未检查"
    }
    Column(modifier = Modifier.testTag("status-probe")) {
        FactRow("PIM 服务器", outcomeText)
        if (!result?.safeMessage.isNullOrBlank()) {
            FactRow("探测信息", result?.safeMessage.orEmpty())
        }
        val checkedAt = checkedAtMillis ?: result?.checkedAtUtcMillis
        if (checkedAt != null && checkedAt > 0L) {
            FactRow("检查时间", formatEpochMillis(checkedAt))
        }
    }
}

@Composable
private fun DiagnosticsSection(diagnostics: DiagnosticSnapshot) {
    Column(modifier = Modifier.testTag("status-diagnostics"), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        SectionHeader("诊断摘要", Icons.Filled.Info)

        FactRow("最近丢弃", StatusDisplayText.droppedReason(diagnostics.lastDroppedReason), "status-dropped-reason")
        FactRow("心跳", StatusDisplayText.heartbeat(diagnostics.lastHeartbeatStatus), "status-heartbeat")

        val hasRecentLogs = diagnostics.recentLogMessages.isNotEmpty() ||
            !diagnostics.lastLogMessage.isNullOrBlank()
        if (hasRecentLogs) {
            FactRow("最近记录", "有近期诊断记录", "status-diagnostic-record")
        } else {
            FactRow("最近记录", "无", "status-diagnostic-record")
        }
    }
}

@Composable
private fun IssuesSection(
    issues: List<StatusIssue>,
    onIssueAction: (StatusIssue) -> Unit
) {
    val actionable = actionableStatusIssues(issues)
    val information = informationalStatusIssues(issues)

    Column(
        modifier = Modifier.testTag("status-issues"),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Column(
            modifier = Modifier.testTag("status-actionable-issues"),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            SectionHeader("需要处理", Icons.Filled.Shield)
            if (actionable.isEmpty()) {
                Text(
                    "未发现需要处理的问题",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                actionable.forEach { issue ->
                    key(issue.code) {
                        StatusIssueRow(issue, onAction = { onIssueAction(issue) })
                    }
                }
            }
        }

        if (information.isNotEmpty()) {
            Divider()
            Column(
                modifier = Modifier.testTag("status-information-issues"),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                SectionHeader("状态信息", Icons.Filled.Info)
                information.forEach { issue ->
                    key(issue.code) {
                        StatusIssueRow(issue, onAction = { onIssueAction(issue) })
                    }
                }
            }
        }
    }
}

@Composable
internal fun StatusIssueRow(
    issue: StatusIssue,
    onAction: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth().testTag("status-issue-${issue.code}")
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Top
        ) {
            Icon(
                imageVector = when (issue.severity) {
                    StatusSeverity.Critical -> Icons.Filled.Warning
                    StatusSeverity.Warning -> Icons.Filled.Warning
                    StatusSeverity.Info -> Icons.Filled.Info
                },
                contentDescription = null,
                modifier = Modifier.size(20.dp).padding(top = 2.dp),
                tint = when (issue.severity) {
                    StatusSeverity.Critical -> MaterialTheme.colorScheme.error
                    StatusSeverity.Warning -> MaterialTheme.colorScheme.tertiary
                    StatusSeverity.Info -> MaterialTheme.colorScheme.onSurfaceVariant
                }
            )
            Spacer(Modifier.width(8.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    issue.title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    issue.message,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        if (issue.target != StatusActionTarget.None) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(
                    onClick = onAction,
                    modifier = Modifier.testTag("status-issue-action-${issue.code}")
                ) {
                    Icon(issueActionIcon(issue.target), contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text(issue.actionLabel)
                }
            }
        }
    }
}

@Composable
private fun SectionHeader(title: String, icon: androidx.compose.ui.graphics.vector.ImageVector) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(6.dp))
        Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun FactRow(label: String, value: String, tag: String? = null) {
    Row(
        modifier = Modifier.fillMaxWidth().let { m ->
            if (tag != null) m.testTag(tag) else m
        },
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            label,
            modifier = Modifier
                .widthIn(min = 72.dp, max = 112.dp)
                .padding(end = 12.dp),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            value,
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.bodySmall,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.End
        )
    }
}

@Composable
private fun CountChip(label: String, value: String, tag: String, modifier: Modifier = Modifier) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier.testTag(tag)
    ) {
        Text(
            value,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

private fun issueActionIcon(target: StatusActionTarget) = when (target) {
    StatusActionTarget.Settings, StatusActionTarget.Login -> Icons.Filled.Settings
    StatusActionTarget.Permissions -> Icons.Filled.Shield
    StatusActionTarget.Sync, StatusActionTarget.Queue -> Icons.Filled.Sync
    StatusActionTarget.NetworkSettings -> Icons.Filled.Wifi
    StatusActionTarget.ConnectionCheck -> Icons.Filled.NetworkCheck
    StatusActionTarget.None -> Icons.Filled.Info
}

internal fun formatEpochMillis(
    millis: Long,
    zoneId: ZoneId = ZoneId.systemDefault()
): String {
    if (millis <= 0L) return "暂无"
    return runCatching {
        STATUS_TIME_FORMATTER.format(Instant.ofEpochMilli(millis).atZone(zoneId))
    }.getOrDefault("暂无")
}

private fun formatRecordedTime(value: String?): String {
    if (value.isNullOrBlank()) return "暂无"
    return runCatching {
        formatEpochMillis(Instant.parse(value).toEpochMilli())
    }.getOrDefault(value)
}

private val STATUS_TIME_FORMATTER: DateTimeFormatter = DateTimeFormatter.ofPattern("MM-dd HH:mm")
