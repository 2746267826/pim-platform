package com.pim.app.ui.permissions

import com.pim.app.status.PermissionStatusSnapshot

data class PermissionSettingRow(
    val issueCode: String,
    val title: String,
    val granted: Boolean,
    val isHardBlock: Boolean
)

fun permissionSettingRows(snapshot: PermissionStatusSnapshot): List<PermissionSettingRow> = listOf(
    PermissionSettingRow(
        issueCode = "notification-permission-missing",
        title = "通知权限",
        granted = snapshot.notificationGranted,
        isHardBlock = true
    ),
    PermissionSettingRow(
        issueCode = "foreground-location-missing",
        title = "精确定位权限",
        granted = snapshot.preciseLocationGranted,
        isHardBlock = true
    ),
    PermissionSettingRow(
        issueCode = "background-location-missing",
        title = "后台定位权限",
        granted = snapshot.backgroundLocationGranted,
        isHardBlock = true
    ),
    PermissionSettingRow(
        issueCode = "usage-access-missing",
        title = "使用情况权限",
        granted = snapshot.usageAccessGranted,
        isHardBlock = false
    ),
    PermissionSettingRow(
        issueCode = "activity-recognition-missing",
        title = "运动识别权限",
        granted = snapshot.activityRecognitionGranted,
        isHardBlock = false
    ),
    PermissionSettingRow(
        issueCode = "battery-optimization-missing",
        title = "电池优化豁免",
        granted = snapshot.batteryOptimizationGranted,
        isHardBlock = false
    )
)
