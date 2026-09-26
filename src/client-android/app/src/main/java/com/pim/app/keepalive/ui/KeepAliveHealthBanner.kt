package com.pim.app.keepalive.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/**
 * 状态页顶部的保活健康红点（REQ-21 的第一条通道）。
 *
 * 规则（逐条对应 AC）：
 * - 四类原因各自能点亮（AC-21.1）——文案由调用方按原因生成，这里只负责呈现；
 * - 原因消除后红点消失（AC-21.2）——[alertText] 为 null 时不渲染任何东西；
 * - **≤15 分钟的普通延迟不点亮**（AC-21.3）——因此本组件只接受来自
 *   [com.pim.app.keepalive.KeepAliveHealthMonitor] 的文案，普通延迟根本不会传进来。
 *
 * 放在状态页**最顶部**（工单第 6 节：状态页顶部点亮红点）。
 */
@Composable
fun KeepAliveHealthBanner(
    alertText: String?,
    onOpenGuidance: () -> Unit,
    modifier: Modifier = Modifier
) {
    // AC-21.2：没有异常时什么都不渲染（而不是渲染一个「正常」的绿点，
    // 那样会在红点熄灭后仍占位，让用户以为这里代表保活状态）。
    if (alertText.isNullOrBlank()) return

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .testTag("status-keepalive-health"),
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.errorContainer,
        tonalElevation = 2.dp
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // 红点本体：让「点亮红点」这一说法在界面上真的可见。
                Surface(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(CircleShape)
                        .testTag("status-keepalive-health-dot"),
                    color = MaterialTheme.colorScheme.error,
                    content = {}
                )
                Text(
                    text = "保活异常",
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onErrorContainer
                )
            }

            Text(
                text = alertText,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onErrorContainer,
                modifier = Modifier.testTag("status-keepalive-health-text")
            )

            TextButton(
                onClick = onOpenGuidance,
                modifier = Modifier.testTag("status-keepalive-health-action")
            ) {
                Text("去检查系统设置")
            }
        }
    }
}
