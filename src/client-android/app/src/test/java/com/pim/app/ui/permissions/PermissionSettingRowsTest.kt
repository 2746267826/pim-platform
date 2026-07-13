package com.pim.app.ui.permissions

import com.pim.app.status.PermissionStatusSnapshot
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PermissionSettingRowsTest {
    @Test
    fun permissionSettingRowsHasExactlySixItems() {
        val snapshot = PermissionStatusSnapshot(
            notificationGranted = true,
            preciseLocationGranted = false,
            backgroundLocationGranted = false,
            usageAccessGranted = true,
            activityRecognitionGranted = false,
            batteryOptimizationGranted = false
        )
        val rows = permissionSettingRows(snapshot)

        assertEquals(6, rows.size)
    }

    @Test
    fun permissionSettingRowsHaveStableOrderAndIssueCodes() {
        val snapshot = PermissionStatusSnapshot(
            notificationGranted = false,
            preciseLocationGranted = false,
            backgroundLocationGranted = false,
            usageAccessGranted = false,
            activityRecognitionGranted = false,
            batteryOptimizationGranted = false
        )
        val rows = permissionSettingRows(snapshot)

        assertEquals("notification-permission-missing", rows[0].issueCode)
        assertEquals("foreground-location-missing", rows[1].issueCode)
        assertEquals("background-location-missing", rows[2].issueCode)
        assertEquals("usage-access-missing", rows[3].issueCode)
        assertEquals("activity-recognition-missing", rows[4].issueCode)
        assertEquals("battery-optimization-missing", rows[5].issueCode)
    }

    @Test
    fun hardBlockPermissionsAreNotificationPreciseAndBackgroundOnly() {
        val snapshot = PermissionStatusSnapshot(
            notificationGranted = false,
            preciseLocationGranted = false,
            backgroundLocationGranted = false,
            usageAccessGranted = false,
            activityRecognitionGranted = false,
            batteryOptimizationGranted = false
        )
        val rows = permissionSettingRows(snapshot)

        assertTrue("通知 must be hard block", rows[0].isHardBlock)
        assertTrue("精确定位 must be hard block", rows[1].isHardBlock)
        assertTrue("后台定位 must be hard block", rows[2].isHardBlock)
        assertFalse("使用情况 must be recommendation", rows[3].isHardBlock)
        assertFalse("活动识别 must be recommendation", rows[4].isHardBlock)
        assertFalse("电池优化 must be recommendation", rows[5].isHardBlock)
    }

    @Test
    fun permissionSettingRowsShowChineseTitles() {
        val snapshot = PermissionStatusSnapshot(
            notificationGranted = true,
            preciseLocationGranted = true,
            backgroundLocationGranted = true,
            usageAccessGranted = true,
            activityRecognitionGranted = true,
            batteryOptimizationGranted = true
        )
        val rows = permissionSettingRows(snapshot)

        assertTrue(rows[0].title.contains("通知"))
        assertTrue(rows[1].title.contains("定位"))
        assertTrue(rows[2].title.contains("后台"))
        assertTrue(rows[3].title.contains("使用情况"))
        assertEquals("运动识别权限", rows[4].title)
        assertTrue(rows[5].title.contains("电池"))
    }

    @Test
    fun permissionSettingRowsReflectGrantedStatus() {
        val allGranted = PermissionStatusSnapshot(
            notificationGranted = true,
            preciseLocationGranted = true,
            backgroundLocationGranted = true,
            usageAccessGranted = true,
            activityRecognitionGranted = true,
            batteryOptimizationGranted = true
        )
        val rows = permissionSettingRows(allGranted)

        rows.forEach { row ->
            assertTrue("${row.title} should show granted", row.granted)
        }
    }
}
