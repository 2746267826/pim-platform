package com.pim.app.status

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.provider.Settings
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
class NetworkSettingsNavigatorTest {
    @Test
    @Config(sdk = [34])
    fun api34UsesInternetConnectivityPanel() {
        val intent = NetworkSettingsNavigator.intent()
        assertEquals(Settings.Panel.ACTION_INTERNET_CONNECTIVITY, intent.action)
    }

    @Test
    @Config(sdk = [28])
    fun api28UsesWirelessSettings() {
        val intent = NetworkSettingsNavigator.intent()
        assertEquals(Settings.ACTION_WIRELESS_SETTINGS, intent.action)
    }

    @Test
    @Config(sdk = [34])
    fun unavailableInternetPanelFallsBackToWirelessSettings() {
        val context = RecordingContext(ApplicationProvider.getApplicationContext())

        NetworkSettingsNavigator.open(context)

        assertEquals(2, context.actions.size)
        assertEquals(Settings.Panel.ACTION_INTERNET_CONNECTIVITY, context.actions[0])
        assertEquals(Settings.ACTION_WIRELESS_SETTINGS, context.actions[1])
    }

    private class RecordingContext(base: Context) : ContextWrapper(base) {
        val actions = mutableListOf<String?>()

        override fun startActivity(intent: Intent) {
            actions += intent.action
            if (actions.size == 1) throw ActivityNotFoundException("panel unavailable")
        }
    }
}
