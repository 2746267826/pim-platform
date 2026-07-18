package com.pim.app.ui.schedule

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pim.app.location.policy.ScheduleWindow
import com.pim.app.schedule.ScheduleRefreshErrorKind
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun SchedulePolicyScreen(
    modifier: Modifier = Modifier,
    onOpenSettings: () -> Unit = {},
    viewModel: SchedulePolicyViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    LaunchedEffect(viewModel) {
        viewModel.refreshIfStale()
    }
    SchedulePolicyContent(
        state = state,
        onRefresh = { viewModel.refresh() },
        onRetry = { viewModel.retry() },
        onOpenSettings = onOpenSettings,
        modifier = modifier
    )
}

@Composable
internal fun SchedulePolicyContent(
    state: SchedulePolicyUiState,
    onRefresh: () -> Unit = {},
    onRetry: () -> Unit = {},
    onOpenSettings: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val zoneId = ZoneId.systemDefault()
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        when (state) {
            is SchedulePolicyUiState.Loading -> {
                PolicyHeader(
                    content = null,
                    isRefreshing = false,
                    onRefresh = {},
                    zoneId = zoneId
                )
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.testTag("schedule-loading"),
                        strokeWidth = 2.dp
                    )
                }
            }

            is SchedulePolicyUiState.Content -> {
                PolicyHeader(
                    content = state.content,
                    isRefreshing = state.content.isRefreshing,
                    onRefresh = onRefresh,
                    zoneId = zoneId
                )
                Divider()
                ContentSection(state.content, zoneId)
                Divider()
                PolicySummarySection(state.content.policySummary)
            }

            is SchedulePolicyUiState.Empty -> {
                PolicyHeader(
                    content = state.content,
                    isRefreshing = state.content.isRefreshing,
                    onRefresh = onRefresh,
                    zoneId = zoneId
                )
                Divider()
                Text(
                    text = "暂无日程安排",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 24.dp),
                    textAlign = TextAlign.Center
                )
                Divider()
                PolicySummarySection(state.content.policySummary)
            }

            is SchedulePolicyUiState.StaleContent -> {
                StaleWarningBanner()
                Spacer(Modifier.height(4.dp))
                PolicyHeader(
                    content = state.content,
                    isRefreshing = state.content.isRefreshing,
                    onRefresh = onRefresh,
                    zoneId = zoneId
                )
                Divider()
                ContentSection(state.content, zoneId)
                Divider()
                PolicySummarySection(state.content.policySummary)
            }

            is SchedulePolicyUiState.Error -> {
                PolicyHeader(
                    content = state.content,
                    isRefreshing = state.content.isRefreshing,
                    onRefresh = onRefresh,
                    zoneId = zoneId
                )
                Divider()
                ErrorSection(
                    message = state.message,
                    errorKind = state.errorKind,
                    isRefreshing = state.content.isRefreshing,
                    onRetry = onRetry,
                    onOpenSettings = onOpenSettings
                )
                if (state.content.lastSuccessAtMillis != null || state.content.policySummary.requestIntervalMillis != null) {
                    Divider()
                    PolicySummarySection(state.content.policySummary)
                }
            }
        }
    }
}

@Composable
private fun PolicyHeader(
    content: ScheduleContentModel?,
    isRefreshing: Boolean,
    onRefresh: () -> Unit,
    zoneId: ZoneId
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "日程",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = "服务端日程",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(Modifier.height(2.dp))
            Text(
                text = "上次成功: ${formatEpochMillis(content?.lastSuccessAtMillis, zoneId)}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = "上次尝试: ${formatEpochMillis(content?.lastAttemptAtMillis, zoneId)}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Box(
            modifier = Modifier
                .size(48.dp)
                .testTag("schedule-refresh"),
            contentAlignment = Alignment.Center
        ) {
            if (isRefreshing) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    strokeWidth = 2.dp
                )
            } else {
                IconButton(
                    onClick = onRefresh,
                    enabled = content != null,
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Refresh,
                        contentDescription = "刷新"
                    )
                }
            }
        }
    }
}

@Composable
private fun StaleWarningBanner() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("schedule-stale-warning"),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Filled.Warning,
            contentDescription = null,
            modifier = Modifier.size(18.dp),
            tint = MaterialTheme.colorScheme.error
        )
        Spacer(Modifier.width(6.dp))
        Text(
            text = "日程数据可能过期",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.error,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
private fun ContentSection(content: ScheduleContentModel, zoneId: ZoneId) {
    CurrentEventSection(content.currentEvent, zoneId)
    Divider(modifier = Modifier.padding(vertical = 4.dp))
    NextEventSection(content.nextEvent, zoneId)
    Divider(modifier = Modifier.padding(vertical = 4.dp))
    UpcomingListSection(content.windowsByDate, zoneId)
}

@Composable
private fun CurrentEventSection(currentEvent: ScheduleWindow?, zoneId: ZoneId) {
    Column(modifier = Modifier.testTag("schedule-current").fillMaxWidth()) {
        SectionLabel("当前日程")
        if (currentEvent != null) {
            EventRow(event = currentEvent, zoneId = zoneId)
        } else {
            Text(
                text = "当前没有进行中的日程",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun NextEventSection(nextEvent: ScheduleWindow?, zoneId: ZoneId) {
    Column(modifier = Modifier.testTag("schedule-upcoming").fillMaxWidth()) {
        SectionLabel("下一项")
        if (nextEvent != null) {
            EventRow(event = nextEvent, zoneId = zoneId)
        } else {
            Text(
                text = "后续暂无日程",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier.padding(bottom = 4.dp)
    )
}

@Composable
private fun EventRow(event: ScheduleWindow, zoneId: ZoneId, includeDate: Boolean = true) {
    Column {
        Text(
            text = event.title,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = buildEventTimeRange(event.startsAtMillis, event.endsAtMillis, zoneId, includeDate),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        if (event.locationText.isNotBlank()) {
            Text(
                text = event.locationText,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun UpcomingListSection(windowsByDate: Map<LocalDate, List<ScheduleWindow>>, zoneId: ZoneId) {
    if (windowsByDate.isEmpty()) return
    Column(modifier = Modifier.testTag("schedule-upcoming-list").fillMaxWidth()) {
        SectionLabel("近期日程")
        windowsByDate.forEach { (date, windows) ->
            Text(
                text = formatDateHeader(date),
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 6.dp, bottom = 2.dp)
            )
            windows.forEach { window ->
                EventRow(event = window, zoneId = zoneId, includeDate = false)
                Spacer(Modifier.height(4.dp))
            }
        }
    }
}

@Composable
private fun PolicySummarySection(summary: PolicySummary) {
    Column(modifier = Modifier.testTag("schedule-policy").fillMaxWidth()) {
        SectionLabel("当前策略")
        FactRow("策略模式", policyModeLabel(summary.mode))
        if (summary.reason != null) {
            FactRow("触发原因", summary.reason)
        }
        if (summary.requestIntervalMillis != null) {
            FactRow("定位间隔", formatInterval(summary.requestIntervalMillis))
        }
        FactRow("恢复阈值", formatThreshold(summary.recoveryThresholdMeters))
    }
}

@Composable
private fun FactRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.widthIn(max = 80.dp).padding(end = 8.dp),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.End,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun ErrorSection(
    message: String,
    errorKind: ScheduleRefreshErrorKind,
    isRefreshing: Boolean,
    onRetry: () -> Unit,
    onOpenSettings: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Filled.Warning,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = MaterialTheme.colorScheme.error
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.weight(1f)
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End
        ) {
            when (errorKind) {
                ScheduleRefreshErrorKind.Authentication -> {
                    Button(
                        onClick = onOpenSettings,
                        enabled = !isRefreshing,
                        modifier = Modifier.testTag("schedule-settings")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Settings,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(4.dp))
                        Text("前往设置")
                    }
                }
                ScheduleRefreshErrorKind.Network,
                ScheduleRefreshErrorKind.Server,
                ScheduleRefreshErrorKind.Cache -> {
                    Button(
                        onClick = onRetry,
                        enabled = !isRefreshing,
                        modifier = Modifier.testTag("schedule-retry")
                    ) {
                        if (isRefreshing) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                strokeWidth = 2.dp
                            )
                            Spacer(Modifier.width(4.dp))
                        }
                        Text(if (isRefreshing) "重试中" else "重试")
                    }
                }
            }
        }
    }
}

internal fun buildEventTimeRange(
    startMillis: Long, endMillis: Long, zoneId: ZoneId, includeDate: Boolean
): String {
    return if (includeDate) {
        val startDate = Instant.ofEpochMilli(startMillis).atZone(zoneId).toLocalDate()
        val endDate = Instant.ofEpochMilli(endMillis).atZone(zoneId).toLocalDate()
        val startStr = formatEventTime(startMillis, zoneId, withDate = true)
        if (startDate == endDate) {
            "$startStr - ${formatEventTime(endMillis, zoneId, withDate = false)}"
        } else {
            "$startStr - ${formatEventTime(endMillis, zoneId, withDate = true)}"
        }
    } else {
        "${formatEventTime(startMillis, zoneId, withDate = false)} - ${formatEventTime(endMillis, zoneId, withDate = false)}"
    }
}

internal fun formatEventTime(millis: Long, zoneId: ZoneId, withDate: Boolean = true): String {
    val fmt = if (withDate) SCHEDULE_TIME_FORMATTER else EVENT_TIME_FORMATTER
    return runCatching {
        fmt.format(Instant.ofEpochMilli(millis).atZone(zoneId))
    }.getOrDefault("")
}

internal fun formatThreshold(meters: Double): String {
    val whole = meters.toLong()
    return if (meters == whole.toDouble()) "${whole} m" else "%.1f m".format(meters)
}

private fun policyModeLabel(mode: String): String = when (mode) {
    "Off" -> "已停止"
    "PowerSavingNormal" -> "常规省电"
    "ScheduleLowFrequency" -> "日程低频"
    "MotionObservation" -> "运动观察"
    "MovementRecovery" -> "移动恢复"
    "SyncFallback" -> "同步兜底"
    "Normal" -> "常规"
    "" -> "暂无"
    else -> "未知状态"
}

internal fun formatInterval(millis: Long): String {
    if (millis <= 0L) return "暂无"
    val minutes = millis / 60_000L
    val seconds = (millis % 60_000L) / 1_000L
    return if (minutes > 0) {
        if (seconds > 0) "${minutes}分${seconds}秒" else "${minutes} 分钟"
    } else {
        "${seconds} 秒"
    }
}

private fun formatEpochMillis(millis: Long?, zoneId: ZoneId): String {
    if (millis == null || millis <= 0L) return "暂无"
    return runCatching {
        SCHEDULE_TIME_FORMATTER.format(Instant.ofEpochMilli(millis).atZone(zoneId))
    }.getOrDefault("暂无")
}

private fun formatDateHeader(date: LocalDate): String {
    return runCatching {
        DATE_HEADER_FORMATTER.format(date)
    }.getOrDefault(date.toString())
}

private val SCHEDULE_TIME_FORMATTER = DateTimeFormatter.ofPattern("MM-dd HH:mm")
private val EVENT_TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm")
private val DATE_HEADER_FORMATTER = DateTimeFormatter.ofPattern("M月d日 EEEE", Locale.CHINESE)
