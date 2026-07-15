package com.pim.app.recovery

import android.content.Intent
import android.content.pm.PackageManager
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class StartupRecoveryReceiverTest {

    @Test
    fun `dispatchStartupRecovery calls recover once for boot completed`() = runTest {
        var callCount = 0
        val result = StartupRecoveryReceiver.dispatchStartupRecovery(Intent.ACTION_BOOT_COMPLETED) {
            callCount++
        }
        assertTrue(result)
        assertEquals(1, callCount)
    }

    @Test
    fun `dispatchStartupRecovery calls recover once for package replaced`() = runTest {
        var callCount = 0
        val result = StartupRecoveryReceiver.dispatchStartupRecovery(Intent.ACTION_MY_PACKAGE_REPLACED) {
            callCount++
        }
        assertTrue(result)
        assertEquals(1, callCount)
    }

    @Test
    fun `dispatchStartupRecovery does not call recover for unknown action`() = runTest {
        var callCount = 0
        val result = StartupRecoveryReceiver.dispatchStartupRecovery(Intent.ACTION_AIRPLANE_MODE_CHANGED) {
            callCount++
        }
        assertFalse(result)
        assertEquals(0, callCount)
    }

    @Test
    fun `dispatchStartupRecovery does not call recover for null`() = runTest {
        var callCount = 0
        val result = StartupRecoveryReceiver.dispatchStartupRecovery(null) {
            callCount++
        }
        assertFalse(result)
        assertEquals(0, callCount)
    }

    @Test
    fun `manifest declares both boot completed and package replaced for receiver`() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val pm = context.packageManager
        val bootReceivers = pm.queryBroadcastReceivers(
            Intent(Intent.ACTION_BOOT_COMPLETED),
            PackageManager.GET_INTENT_FILTERS
        )
        val replacedReceivers = pm.queryBroadcastReceivers(
            Intent(Intent.ACTION_MY_PACKAGE_REPLACED),
            PackageManager.GET_INTENT_FILTERS
        )
        val targetName = StartupRecoveryReceiver::class.java.name
        assertTrue("BOOT_COMPLETED must resolve to StartupRecoveryReceiver",
            bootReceivers.any { it.activityInfo.name == targetName })
        assertTrue("MY_PACKAGE_REPLACED must resolve to StartupRecoveryReceiver",
            replacedReceivers.any { it.activityInfo.name == targetName })
    }
}
