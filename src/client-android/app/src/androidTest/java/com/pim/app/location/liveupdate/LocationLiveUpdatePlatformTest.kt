package com.pim.app.location.liveupdate

import android.app.Notification
import android.content.Context
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.pim.app.location.acquisition.TriggerType
import com.pim.app.notifications.LocationNotificationRenderer
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class LocationLiveUpdatePlatformTest {

    private val context: Context = InstrumentationRegistry.getInstrumentation().targetContext

    @Test
    fun liveUpdateNotificationHasPromotableCharacteristics() {
        assumeTrue(LocationLiveUpdateCapability.check())
        val savedCapability = LocationLiveUpdateNotificationRenderer.capabilityOverride
        val savedPermission = LocationLiveUpdateNotificationRenderer.canShowNotificationsOverride
        val savedPromoted = LocationLiveUpdateNotificationRenderer.canPostPromotedOverride
        try {
            LocationLiveUpdateNotificationRenderer.canShowNotificationsOverride = { true }
            LocationLiveUpdateNotificationRenderer.canPostPromotedOverride = { true }

            var capturedNotification: Notification? = null
            LocationLiveUpdateNotificationRenderer.tryBuildAndNotify(
                ctx = context,
                content = LocationLiveUpdateContent(
                    sessionId = "test-session",
                    triggerType = TriggerType.MANUAL,
                    elapsedSeconds = 5,
                    accuracyMeters = 10f,
                    providerLabel = "gps"
                ),
                createChannel = { _, _ -> },
                notifyFn = { _, notification -> capturedNotification = notification }
            )
            assertNotNull("renderer should have produced a notification", capturedNotification)
            assertTrue(
                "notification should have promotable characteristics",
                capturedNotification!!.hasPromotableCharacteristics()
            )
            assertNotEquals(
                "LIVE_UPDATE_NOTIFICATION_ID must differ from LocationNotificationRenderer.NOTIFICATION_ID",
                LocationLiveUpdateNotificationRenderer.LIVE_UPDATE_NOTIFICATION_ID,
                LocationNotificationRenderer.NOTIFICATION_ID
            )
            val content = capturedNotification!!.extras?.getString(Notification.EXTRA_TEXT) ?: ""
            val bigText = capturedNotification!!.extras?.getString(Notification.EXTRA_BIG_TEXT) ?: ""
            assertTrue("content must not contain latitude 31.2304", !content.contains("31.2304"))
            assertTrue("content must not contain longitude 121.4737", !content.contains("121.4737"))
            assertTrue("big text must not contain latitude 31.2304", !bigText.contains("31.2304"))
            assertTrue("big text must not contain longitude 121.4737", !bigText.contains("121.4737"))
        } finally {
            LocationLiveUpdateNotificationRenderer.capabilityOverride = savedCapability
            LocationLiveUpdateNotificationRenderer.canShowNotificationsOverride = savedPermission
            LocationLiveUpdateNotificationRenderer.canPostPromotedOverride = savedPromoted
        }
    }

    @Test
    fun liveUpdateNotificationIdDiffersFromOngoingNotification() {
        assertNotEquals(
            "LIVE_UPDATE_NOTIFICATION_ID must differ from LocationNotificationRenderer.NOTIFICATION_ID",
            LocationLiveUpdateNotificationRenderer.LIVE_UPDATE_NOTIFICATION_ID,
            LocationNotificationRenderer.NOTIFICATION_ID
        )
    }
}
