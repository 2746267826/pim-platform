package com.pim.app.permissions

import android.app.Application
import android.content.Context
import android.os.PowerManager
import androidx.test.core.app.ApplicationProvider
import com.pim.app.TestPimApp
import com.pim.app.mobile.usage.UsageAccessChecker
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = TestPimApp::class)
class PermissionStatusRepositoryTest {
    @Test
    fun batteryOptimizationIsFalseWhenNotExempted() {
        val context = ApplicationProvider.getApplicationContext<Application>()
        val repo = PermissionStatusRepository(context, UsageAccessChecker(context))
        val snapshot = repo.snapshot()

        assertFalse(snapshot.batteryOptimizationGranted)
    }

    @Test
    fun batteryOptimizationIsTrueWhenExempted() {
        val context = ApplicationProvider.getApplicationContext<Application>()
        val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        shadowOf(powerManager).setIgnoringBatteryOptimizations(context.packageName, true)
        val repo = PermissionStatusRepository(context, UsageAccessChecker(context))
        val snapshot = repo.snapshot()

        assertTrue(snapshot.batteryOptimizationGranted)
    }
}
