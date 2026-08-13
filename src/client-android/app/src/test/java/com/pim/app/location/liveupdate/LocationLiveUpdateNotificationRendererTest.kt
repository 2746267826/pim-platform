package com.pim.app.location.liveupdate

import android.app.Application
import android.app.Notification
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.test.core.app.ApplicationProvider
import com.pim.app.TestPimApp
import com.pim.app.location.acquisition.TriggerType
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = TestPimApp::class)
class LocationLiveUpdateNotificationRendererTest {

    private val ctx: Context = ApplicationProvider.getApplicationContext()

    private fun content(
        sessionId: String = "test-session",
        triggerType: TriggerType = TriggerType.MANUAL,
        elapsedSeconds: Long = 5,
        accuracyMeters: Float? = 10f,
        providerLabel: String = "gps"
    ) = LocationLiveUpdateContent(
        sessionId = sessionId,
        triggerType = triggerType,
        elapsedSeconds = elapsedSeconds,
        accuracyMeters = accuracyMeters,
        providerLabel = providerLabel
    )

    @Before
    fun setUp() {
        LocationLiveUpdateNotificationRenderer.capabilityOverride = { true }
        LocationLiveUpdateNotificationRenderer.canShowNotificationsOverride = { true }
        LocationLiveUpdateNotificationRenderer.canPostPromotedOverride = { true }
    }

    @After
    fun tearDown() {
        LocationLiveUpdateNotificationRenderer.capabilityOverride = null
        LocationLiveUpdateNotificationRenderer.canShowNotificationsOverride = null
        LocationLiveUpdateNotificationRenderer.canPostPromotedOverride = null
    }

    @Test
    fun `channel id constant is correct`() {
        assertEquals("pim_location_live_update", LocationLiveUpdateNotificationRenderer.CHANNEL_ID)
    }

    @Test
    fun `notification id constant is 7102`() {
        assertEquals(7102, LocationLiveUpdateNotificationRenderer.LIVE_UPDATE_NOTIFICATION_ID)
    }

    @Test
    fun `request codes are correct`() {
        assertEquals(71020, LocationLiveUpdateNotificationRenderer.CANCEL_REQUEST_CODE)
        assertEquals(71021, LocationLiveUpdateNotificationRenderer.OPEN_REQUEST_CODE)
        assertEquals(71022, LocationLiveUpdateNotificationRenderer.DELETE_REQUEST_CODE)
    }

    @Test
    fun `renderer fails gracefully when capability is false`() {
        LocationLiveUpdateNotificationRenderer.capabilityOverride = { false }
        val result = LocationLiveUpdateNotificationRenderer.tryBuildAndNotify(
            ctx = ctx,
            content = content(),
            createChannel = { _, _ -> },
            notifyFn = { _, _ -> }
        )
        assertFalse(result)
    }

    @Test
    fun `renderer fails gracefully when promoted not available`() {
        LocationLiveUpdateNotificationRenderer.canPostPromotedOverride = { false }
        val result = LocationLiveUpdateNotificationRenderer.tryBuildAndNotify(
            ctx = ctx,
            content = content(),
            createChannel = { _, _ -> },
            notifyFn = { _, _ -> }
        )
        assertFalse(result)
    }

    @Test
    fun `renderer fails when permission denied`() {
        LocationLiveUpdateNotificationRenderer.canShowNotificationsOverride = { false }
        val result = LocationLiveUpdateNotificationRenderer.tryBuildAndNotify(
            ctx = ctx,
            content = content(),
            createChannel = { _, _ -> },
            notifyFn = { _, _ -> }
        )
        assertFalse(result)
    }

    @Test
    fun `permission denied does not call promotion channel or notify`() {
        LocationLiveUpdateNotificationRenderer.canShowNotificationsOverride = { false }
        var channelCalled = false
        var notifyCalled = false
        LocationLiveUpdateNotificationRenderer.tryBuildAndNotify(
            ctx = ctx,
            content = content(),
            createChannel = { _, _ -> channelCalled = true },
            notifyFn = { _, _ -> notifyCalled = true }
        )
        assertFalse("channel must not be created when permission denied", channelCalled)
        assertFalse("notify must not be called when permission denied", notifyCalled)
    }

    @Test
    fun `capability false does not call permission promotion channel or notify`() {
        LocationLiveUpdateNotificationRenderer.capabilityOverride = { false }
        var channelCalled = false
        var notifyCalled = false
        LocationLiveUpdateNotificationRenderer.tryBuildAndNotify(
            ctx = ctx,
            content = content(),
            createChannel = { _, _ -> channelCalled = true },
            notifyFn = { _, _ -> notifyCalled = true }
        )
        assertFalse("channel must not be created when capability false", channelCalled)
        assertFalse("notify must not be called when capability false", notifyCalled)
    }

    @Test
    fun `renderer notifies with valid parameters`() {
        var channelCreated = false
        var notifiedId = -1
        var notifiedNotification: Notification? = null

        val result = LocationLiveUpdateNotificationRenderer.tryBuildAndNotify(
            ctx = ctx,
            content = content(sessionId = "session-abc", providerLabel = "fused"),
            createChannel = { id, name ->
                channelCreated = true
                assertEquals("pim_location_live_update", id)
            },
            notifyFn = { id, notification ->
                notifiedId = id
                notifiedNotification = notification
            }
        )

        assertTrue(result)
        assertTrue(channelCreated)
        assertEquals(7102, notifiedId)
        assertNotNull(notifiedNotification)
    }

    @Test
    fun `rendered notification has expected properties`() {
        var capturedNotification: Notification? = null

        LocationLiveUpdateNotificationRenderer.tryBuildAndNotify(
            ctx = ctx,
            content = content(
                sessionId = "session-xyz",
                triggerType = TriggerType.AUTOMATIC,
                elapsedSeconds = 30,
                accuracyMeters = 25f,
                providerLabel = "network"
            ),
            createChannel = { _, _ -> },
            notifyFn = { _, notification ->
                capturedNotification = notification
            }
        )

        val n = capturedNotification
        assertNotNull(n)
        assertTrue((n!!.flags and Notification.FLAG_ONLY_ALERT_ONCE) != 0)
        assertTrue(n.visibility == Notification.VISIBILITY_PUBLIC)
        assertTrue(n.flags and Notification.FLAG_ONGOING_EVENT != 0)
    }

    @Test
    fun `content text does not contain latitude or longitude`() {
        var capturedNotification: Notification? = null

        LocationLiveUpdateNotificationRenderer.tryBuildAndNotify(
            ctx = ctx,
            content = content(sessionId = "session-no-coords"),
            createChannel = { _, _ -> },
            notifyFn = { _, notification ->
                capturedNotification = notification
            }
        )

        val content = capturedNotification?.extras?.getString(Notification.EXTRA_TEXT) ?: ""
        val bigText = capturedNotification?.extras?.getString(Notification.EXTRA_BIG_TEXT) ?: ""
        assertFalse("content must not contain latitude", content.contains("31.2304") || content.contains("latitude"))
        assertFalse("content must not contain longitude", content.contains("121.4737") || content.contains("longitude"))
        assertFalse("big text must not contain latitude", bigText.contains("31.2304") || bigText.contains("latitude"))
        assertFalse("big text must not contain longitude", bigText.contains("121.4737") || bigText.contains("longitude"))
    }

    @Test
    fun `channel name is 定位动态`() {
        var capturedName = ""
        LocationLiveUpdateNotificationRenderer.tryBuildAndNotify(
            ctx = ctx,
            content = content(),
            createChannel = { _, name -> capturedName = name },
            notifyFn = { _, _ -> }
        )
        assertEquals("定位动态", capturedName)
    }

    @Test
    fun `short critical text is 定位中 when accuracy is null`() {
        assertEquals(
            "定位中",
            LocationLiveUpdateNotificationRenderer.shortCriticalText(null)
        )
    }

    @Test
    fun `short critical text shows rounded accuracy when available`() {
        assertEquals("±18m", LocationLiveUpdateNotificationRenderer.shortCriticalText(18f))
        assertEquals("±3m", LocationLiveUpdateNotificationRenderer.shortCriticalText(3.4f))
        assertEquals("±0m", LocationLiveUpdateNotificationRenderer.shortCriticalText(0.2f))
    }

    @Test
    fun `normalizeProviderLabel uppercases standard labels`() {
        assertEquals("GPS", LocationLiveUpdateNotificationRenderer.normalizeProviderLabel("gps"))
        assertEquals("NETWORK", LocationLiveUpdateNotificationRenderer.normalizeProviderLabel("network"))
        assertEquals("FUSED", LocationLiveUpdateNotificationRenderer.normalizeProviderLabel("fused"))
    }

    @Test
    fun `normalizeProviderLabel uppercases unknown labels`() {
        assertEquals("PASSIVE", LocationLiveUpdateNotificationRenderer.normalizeProviderLabel("passive"))
        assertEquals("TEST", LocationLiveUpdateNotificationRenderer.normalizeProviderLabel("test"))
    }

    @Test
    fun `collapsed text contains normalized provider`() {
        var capturedNotification: Notification? = null
        LocationLiveUpdateNotificationRenderer.tryBuildAndNotify(
            ctx = ctx,
            content = content(providerLabel = "gps"),
            createChannel = { _, _ -> },
            notifyFn = { _, notification -> capturedNotification = notification }
        )
        val collapsed = capturedNotification?.extras?.getString(Notification.EXTRA_TEXT) ?: ""
        assertTrue("collapsed text should contain GPS", collapsed.contains("GPS"))
        assertFalse("collapsed text should not contain raw lowercase gps", collapsed.contains("· gps"))
    }

    @Test
    fun `big text contains normalized provider`() {
        var capturedNotification: Notification? = null
        LocationLiveUpdateNotificationRenderer.tryBuildAndNotify(
            ctx = ctx,
            content = content(providerLabel = "network"),
            createChannel = { _, _ -> },
            notifyFn = { _, notification -> capturedNotification = notification }
        )
        val bigText = capturedNotification?.extras?.getString(Notification.EXTRA_BIG_TEXT) ?: ""
        assertTrue("big text should contain NETWORK", bigText.contains("NETWORK"))
        assertFalse("big text should not contain raw lowercase network", bigText.contains("network"))
    }

    @Test
    fun `cancel pending intent action uses pim scheme with sessionId and cancel`() {
        val cancelIntent = LocationLiveUpdateNotificationRenderer.cancelPendingIntent(ctx, "test-session-42")
        val shadow = Shadows.shadowOf(cancelIntent)
        val data = shadow.savedIntent.data
        assertNotNull("cancel intent must have data URI", data)
        assertEquals("pim", data!!.scheme)
        assertEquals("location-live", data.authority)
        assertEquals("/test-session-42/cancel", data.path)
        assertEquals(
            LocationLiveUpdateNotificationRenderer.ACTION_CANCEL_LOCATION_SESSION,
            shadow.savedIntent.action
        )
    }

    @Test
    fun `delete pending intent action uses pim scheme with sessionId and delete`() {
        val deleteIntent = LocationLiveUpdateNotificationRenderer.deletePendingIntent(ctx, "test-session-99")
        val shadow = Shadows.shadowOf(deleteIntent)
        val data = shadow.savedIntent.data
        assertNotNull("delete intent must have data URI", data)
        assertEquals("pim", data!!.scheme)
        assertEquals("location-live", data.authority)
        assertEquals("/test-session-99/delete", data.path)
        assertEquals(
            LocationLiveUpdateNotificationRenderer.ACTION_DISMISS_LOCATION_LIVE_UPDATE,
            shadow.savedIntent.action
        )
    }

    @Test
    fun `open pending intent uses pim scheme with sessionId and open plus CLEAR_TOP SINGLE_TOP`() {
        val openIntent = LocationLiveUpdateNotificationRenderer.openPendingIntent(ctx, "session-open-77")
        val shadow = Shadows.shadowOf(openIntent)
        val data = shadow.savedIntent.data
        assertNotNull("open intent must have data URI", data)
        assertEquals("pim", data!!.scheme)
        assertEquals("location-live", data.authority)
        assertEquals("/session-open-77/open", data.path)
        val flags = shadow.savedIntent.flags
        assertTrue("must have FLAG_ACTIVITY_NEW_TASK", flags and Intent.FLAG_ACTIVITY_NEW_TASK != 0)
        assertTrue("must have FLAG_ACTIVITY_CLEAR_TOP", flags and Intent.FLAG_ACTIVITY_CLEAR_TOP != 0)
        assertTrue("must have FLAG_ACTIVITY_SINGLE_TOP", flags and Intent.FLAG_ACTIVITY_SINGLE_TOP != 0)
    }

    @Test
    fun `parseSessionUri returns sessionId and action for valid cancel URI`() {
        val uri = Uri.parse("pim://location-live/session-42/cancel")
        val result = LocationLiveUpdateNotificationRenderer.parseSessionUri(uri)
        assertNotNull(result)
        assertEquals("session-42", result!!.sessionId)
        assertEquals("cancel", result.action)
    }

    @Test
    fun `parseSessionUri returns sessionId and action for valid delete URI`() {
        val uri = Uri.parse("pim://location-live/session-99/delete")
        val result = LocationLiveUpdateNotificationRenderer.parseSessionUri(uri)
        assertNotNull(result)
        assertEquals("session-99", result!!.sessionId)
        assertEquals("delete", result.action)
    }

    @Test
    fun `parseSessionUri returns null for wrong scheme`() {
        val uri = Uri.parse("https://location-live/session-42/cancel")
        assertNull(LocationLiveUpdateNotificationRenderer.parseSessionUri(uri))
    }

    @Test
    fun `parseSessionUri returns null for wrong authority`() {
        val uri = Uri.parse("pim://other-host/session-42/cancel")
        assertNull(LocationLiveUpdateNotificationRenderer.parseSessionUri(uri))
    }

    @Test
    fun `parseSessionUri returns null for single segment path`() {
        val uri = Uri.parse("pim://location-live/session-42")
        assertNull(LocationLiveUpdateNotificationRenderer.parseSessionUri(uri))
    }

    @Test
    fun `parseSessionUri returns null for three segment path`() {
        val uri = Uri.parse("pim://location-live/session-42/cancel/extra")
        assertNull(LocationLiveUpdateNotificationRenderer.parseSessionUri(uri))
    }

    @Test
    fun `parseSessionUri returns null for unknown action`() {
        val uri = Uri.parse("pim://location-live/session-42/unknown")
        assertNull(LocationLiveUpdateNotificationRenderer.parseSessionUri(uri))
    }

    @Test
    fun `parseSessionUri returns null for null uri`() {
        assertNull(LocationLiveUpdateNotificationRenderer.parseSessionUri(null))
    }

    @Test
    fun `parseSessionUri returns null for empty path`() {
        val uri = Uri.parse("pim://location-live")
        assertNull(LocationLiveUpdateNotificationRenderer.parseSessionUri(uri))
    }

    @Test
    fun `parseSessionUri returns null when session segment is empty`() {
        val uri = Uri.parse("pim://location-live//cancel")
        assertNull(LocationLiveUpdateNotificationRenderer.parseSessionUri(uri))
    }

    @Test
    fun `high speed notification uses dedicated copy and shares id 7102`() {
        var capturedId = -1
        var captured: Notification? = null
        val result = LocationLiveUpdateNotificationRenderer.tryBuildAndNotifyHighSpeed(
            ctx = ctx,
            content = HighSpeedLiveUpdateContent(elapsedSeconds = 95),
            createChannel = { _, _ -> },
            notifyFn = { id, notification ->
                capturedId = id
                captured = notification
            }
        )

        assertTrue(result)
        assertEquals(7102, capturedId)
        assertNotNull(captured)
        assertEquals(
            "高速轨迹记录中",
            captured!!.extras.getCharSequence(Notification.EXTRA_TITLE).toString()
        )
        assertEquals(
            "已记录 1 分 35 秒 · 2.5s 密集采样",
            captured!!.extras.getCharSequence(Notification.EXTRA_TEXT).toString()
        )
        assertEquals(
            "high-speed live update must not carry a session cancel action",
            true,
            captured!!.actions == null || captured!!.actions.isEmpty()
        )
        assertTrue((captured!!.flags and Notification.FLAG_ONGOING_EVENT) != 0)
    }

    @Test
    fun `high speed renderer fails gracefully when capability is false`() {
        LocationLiveUpdateNotificationRenderer.capabilityOverride = { false }
        val result = LocationLiveUpdateNotificationRenderer.tryBuildAndNotifyHighSpeed(
            ctx = ctx,
            content = HighSpeedLiveUpdateContent(elapsedSeconds = 5),
            createChannel = { _, _ -> },
            notifyFn = { _, _ -> }
        )
        assertFalse(result)
    }

    @Test
    fun `high speed renderer fails when permission denied`() {
        LocationLiveUpdateNotificationRenderer.canShowNotificationsOverride = { false }
        val result = LocationLiveUpdateNotificationRenderer.tryBuildAndNotifyHighSpeed(
            ctx = ctx,
            content = HighSpeedLiveUpdateContent(elapsedSeconds = 5),
            createChannel = { _, _ -> },
            notifyFn = { _, _ -> }
        )
        assertFalse(result)
    }

    @Test
    fun `high speed renderer fails when promoted not available`() {
        LocationLiveUpdateNotificationRenderer.canPostPromotedOverride = { false }
        val result = LocationLiveUpdateNotificationRenderer.tryBuildAndNotifyHighSpeed(
            ctx = ctx,
            content = HighSpeedLiveUpdateContent(elapsedSeconds = 5),
            createChannel = { _, _ -> },
            notifyFn = { _, _ -> }
        )
        assertFalse(result)
    }
}
