package com.pim.app.manifest

import android.app.Application
import android.content.pm.PackageManager
import androidx.test.core.app.ApplicationProvider
import com.pim.app.TestPimApp
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = TestPimApp::class)
class PermissionManifestTest {
    @Test
    fun manifestDeclaresRequestIgnoreBatteryOptimizations() {
        val context = ApplicationProvider.getApplicationContext<Application>()
        val packageInfo = context.packageManager.getPackageInfo(
            context.packageName,
            PackageManager.GET_PERMISSIONS
        )
        val requestedPermissions = packageInfo.requestedPermissions ?: emptyArray()

        assertTrue(
            "Manifest must declare android.permission.REQUEST_IGNORE_BATTERY_OPTIMIZATIONS",
            requestedPermissions.contains("android.permission.REQUEST_IGNORE_BATTERY_OPTIMIZATIONS")
        )
    }

    @Test
    fun manifestDeclaresAccessNetworkState() {
        val context = ApplicationProvider.getApplicationContext<Application>()
        val packageInfo = context.packageManager.getPackageInfo(
            context.packageName,
            PackageManager.GET_PERMISSIONS
        )
        val requestedPermissions = packageInfo.requestedPermissions ?: emptyArray()

        assertTrue(
            "Manifest must declare android.permission.ACCESS_NETWORK_STATE",
            requestedPermissions.contains("android.permission.ACCESS_NETWORK_STATE")
        )
    }

    @Test
    fun manifestDeclaresPostPromotedNotifications() {
        val context = ApplicationProvider.getApplicationContext<Application>()
        val packageInfo = context.packageManager.getPackageInfo(
            context.packageName,
            PackageManager.GET_PERMISSIONS
        )
        val requestedPermissions = packageInfo.requestedPermissions ?: emptyArray()

        assertTrue(
            "Manifest must declare android.permission.POST_PROMOTED_NOTIFICATIONS",
            requestedPermissions.contains("android.permission.POST_PROMOTED_NOTIFICATIONS")
        )
    }
}
