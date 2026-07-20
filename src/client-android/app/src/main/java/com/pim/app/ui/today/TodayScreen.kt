package com.pim.app.ui.today

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pim.app.ui.shell.PimWebViewScreen

@Composable
fun TodayScreen(
    modifier: Modifier = Modifier,
    onOpenSettings: () -> Unit = {},
    viewModel: TodayViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val syncFeedback by viewModel.syncFeedback.collectAsStateWithLifecycle()
    val refreshVersion by viewModel.refreshVersion.collectAsStateWithLifecycle()

    Column(modifier = modifier.fillMaxSize()) {
        TodayStatusBar(
            state = state,
            syncFeedback = syncFeedback,
            onSyncNow = { viewModel.syncNow() }
        )

        when (state.embedSupported) {
            false -> {
                EmbedUnsupportedBanner(onOpenSettings = onOpenSettings)
            }
            else -> {
                PimWebViewScreen(
                    route = "/embed/android/today",
                    serverUrl = viewModel.serverUrl,
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    bridge = viewModel.bridge,
                    reloadKey = refreshVersion
                )
            }
        }
    }
}

@Composable
internal fun TodayStatusBar(
    state: TodayUiState,
    syncFeedback: String?,
    onSyncNow: () -> Unit
) {
    val busy = state.isSyncButtonDisabled
    val showSpinner = state.syncButtonShowSpinner
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = state.statusTitle,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (state.statusDescription.isNotBlank()) {
                    Text(
                        text = state.statusDescription,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            Spacer(modifier = Modifier.width(8.dp))
            Button(
                onClick = onSyncNow,
                enabled = !busy,
                contentPadding = ButtonDefaults.ButtonWithIconContentPadding
            ) {
                if (showSpinner) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                } else {
                    Icon(
                        Icons.Filled.Sync,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                }
                Text(
                    state.syncButtonLabel,
                    style = MaterialTheme.typography.labelSmall
                )
            }
        }

        if (syncFeedback != null) {
            Text(
                text = syncFeedback,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(top = 4.dp)
            )
        }

        Spacer(modifier = Modifier.height(6.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FactChip("待传总数", state.pendingCount.toString(), "today-pending-total")
            FactChip("定位待传", state.pendingLocationPoints.toString(), "today-pending-location")
            FactChip(
                "上传中",
                if (state.isSyncing) "是" else "否"
            )
            FactChip("已确认", state.confirmedCount.toString())
            FactChip("本轮拒绝", state.rejectedCount.toString())
            FactChip("永久拒绝", state.permanentRejectedCount.toString())
            state.lastSuccessfulUploadAt?.let { FactChip("上次成功", it) }
            state.nextAttemptAt?.let { FactChip("下次尝试", it) }
            state.generatedAt?.let { FactChip("生成时间", it) }
        }
    }
}

@Composable
private fun FactChip(label: String, value: String, tag: String? = null) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        tonalElevation = 0.dp
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp).let { m ->
                if (tag != null) m.testTag(tag) else m
            },
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
private fun EmbedUnsupportedBanner(onOpenSettings: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "服务器版本不支持嵌入页面",
            style = MaterialTheme.typography.bodyLarge
        )
        Text(
            text = "请升级服务器或切换到支持嵌入页面的服务器",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 8.dp)
        )
        Button(
            onClick = onOpenSettings,
            modifier = Modifier.padding(top = 16.dp)
        ) {
            Icon(Icons.Filled.Settings, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text("打开设置")
        }
    }
}
