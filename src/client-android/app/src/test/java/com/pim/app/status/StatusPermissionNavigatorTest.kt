package com.pim.app.status

import android.app.Application
import android.net.Uri
import android.provider.Settings
import androidx.test.core.app.ApplicationProvider
import com.pim.app.TestPimApp
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = TestPimApp::class)
class StatusPermissionNavigatorTest {
    @Test
    fun batteryIssueNavigatesToRequestIgnoreBatteryOptimizations() {
        val context = ApplicationProvider.getApplicationContext<Application>()
        val batteryIssue = StatusIssue(
            code = "battery-optimization-missing",
            severity = StatusSeverity.Warning,
            title = "电池优化未豁免",
            message = "电池优化会影响持续采集。",
            actionLabel = "去授权",
            target = StatusActionTarget.Permissions
        )
        val intent = StatusPermissionNavigator.intentFor(context, batteryIssue)

        assertEquals(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS, intent.action)
        assertEquals(Uri.parse("package:${context.packageName}"), intent.data)
    }

    @Test
    fun unknownIssueFallsBackToAppDetails() {
        val context = ApplicationProvider.getApplicationContext<Application>()
        val unknownIssue = StatusIssue(
            code = "unknown-issue-code",
            severity = StatusSeverity.Info,
            title = "未知",
            message = "测试回退",
            actionLabel = "去设置",
            target = StatusActionTarget.Permissions
        )
        val intent = StatusPermissionNavigator.intentFor(context, unknownIssue)

        assertEquals(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, intent.action)
        assertEquals(Uri.parse("package:${context.packageName}"), intent.data)
    }
}
