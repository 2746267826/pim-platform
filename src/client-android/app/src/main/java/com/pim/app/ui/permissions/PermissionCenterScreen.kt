package com.pim.app.ui.permissions

import android.Manifest
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat

@Composable
fun PermissionCenterScreen(
    modifier: Modifier = Modifier,
    uploadQueueCount: Int = 0,
    collectionQuality: String = "collection quality: waiting"
) {
    val context = LocalContext.current

    Column(
        modifier = modifier.padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = "权限中心",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.SemiBold
        )
        PermissionRow("使用情况权限", if (hasUsageAccess(context)) "已授权" else "未授权")
        PermissionRow("定位权限", if (hasFineLocationPermission(context)) "已授权" else "未授权")
        PermissionRow("通知权限", if (hasNotificationPermission(context)) "已授权" else "未授权")
        PermissionRow("设备状态", "可采集")
        PermissionRow("上传队列", "$uploadQueueCount 条")
        PermissionRow("collection quality", collectionQuality)
        Text(
            text = "复杂日程、确认、报告和数据中心操作会打开嵌入 Web；本地只缓存采集上传。",
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

@Composable
private fun PermissionRow(label: String, value: String) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        tonalElevation = 1.dp,
        shape = MaterialTheme.shapes.medium
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(text = label, fontWeight = FontWeight.Medium)
            Text(text = value)
        }
    }
}

private fun hasUsageAccess(context: Context): Boolean {
    val manager = context.getSystemService(Context.USAGE_STATS_SERVICE) as? UsageStatsManager
        ?: return false
    val now = System.currentTimeMillis()
    return runCatching {
        manager.queryUsageStats(
            UsageStatsManager.INTERVAL_DAILY,
            (now - 24 * 60 * 60 * 1000L).coerceAtLeast(0L),
            now
        ).orEmpty().isNotEmpty()
    }.getOrDefault(false)
}

private fun hasFineLocationPermission(context: Context): Boolean {
    return ContextCompat.checkSelfPermission(
        context,
        Manifest.permission.ACCESS_FINE_LOCATION
    ) == PackageManager.PERMISSION_GRANTED
}

private fun hasNotificationPermission(context: Context): Boolean {
    return Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
        ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED
}
