package com.pim.app.notifications

import android.app.Application
import android.app.Notification
import androidx.test.core.app.ApplicationProvider
import com.pim.app.TestPimApp
import com.pim.app.location.policy.LocationPolicyMode
import com.pim.app.location.service.ForegroundLocationController
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = TestPimApp::class)
class LocationNotificationRendererTest {
    @Test
    fun collectionControlActionShowsPauseWhenActive() {
        val action = collectionControlAction(LocationPolicyMode.PowerSavingNormal)

        assertEquals("暂停", action.label)
        assertEquals(ForegroundLocationController.ACTION_PAUSE_COLLECTION, action.action)
    }

    @Test
    fun collectionControlActionShowsResumeWhenPaused() {
        val action = collectionControlAction(LocationPolicyMode.Off)

        assertEquals("恢复", action.label)
        assertEquals(ForegroundLocationController.ACTION_RESUME_COLLECTION, action.action)
    }

    @Test
    fun ongoingEventFlagWhenActive() {
        val context = ApplicationProvider.getApplicationContext<Application>()
        val notification = LocationNotificationRenderer.build(context, uiModel())

        assertTrue((notification.flags and Notification.FLAG_ONGOING_EVENT) != 0)
    }

    @Test
    fun noOngoingEventWhenPaused() {
        val context = ApplicationProvider.getApplicationContext<Application>()
        val notification = LocationNotificationRenderer.build(
            context,
            uiModel(mode = LocationPolicyMode.Off)
        )

        assertFalse((notification.flags and Notification.FLAG_ONGOING_EVENT) != 0)
    }

    @Test
    fun pausedStateShowsResumeAction() {
        val context = ApplicationProvider.getApplicationContext<Application>()
        val notification = LocationNotificationRenderer.build(
            context,
            uiModel(mode = LocationPolicyMode.Off)
        )

        assertEquals("恢复", notification.actions[0].title)
    }

    @Test
    fun contentComesFromUiModel() {
        val context = ApplicationProvider.getApplicationContext<Application>()
        val model = uiModel(
            collapsedText = "定位中 · 等待首次定位",
            expandedText = "状态：定位中\n策略：省电档\n待上传 0"
        )
        val notification = LocationNotificationRenderer.build(context, model)

        assertEquals(model.title, notification.extras.getCharSequence(Notification.EXTRA_TITLE)?.toString())
        assertEquals(model.collapsedText, notification.extras.getCharSequence(Notification.EXTRA_TEXT)?.toString())
        assertEquals(model.expandedText, notification.extras.getCharSequence(Notification.EXTRA_BIG_TEXT)?.toString())
    }

    @Test
    fun channelIdIsLocationCollection() {
        val context = ApplicationProvider.getApplicationContext<Application>()
        val notification = LocationNotificationRenderer.build(context, uiModel())

        assertEquals(LocationNotificationRenderer.CHANNEL_ID, notification.channelId)
        assertEquals("pim_location_collection", notification.channelId)
    }

    @Test
    fun notificationIdConstant() {
        assertEquals(7101, LocationNotificationRenderer.NOTIFICATION_ID)
    }

    @Test
    fun actionsOrderWhenActive() {
        val context = ApplicationProvider.getApplicationContext<Application>()
        val notification = LocationNotificationRenderer.build(context, uiModel())

        assertEquals(3, notification.actions.size)
        assertEquals("暂停", notification.actions[0].title)
        assertEquals("同步", notification.actions[1].title)
        assertEquals("状态", notification.actions[2].title)
    }

    @Test
    fun actionsOrderWhenPaused() {
        val context = ApplicationProvider.getApplicationContext<Application>()
        val notification = LocationNotificationRenderer.build(
            context,
            uiModel(mode = LocationPolicyMode.Off)
        )

        assertEquals(3, notification.actions.size)
        assertEquals("恢复", notification.actions[0].title)
        assertEquals("同步", notification.actions[1].title)
        assertEquals("状态", notification.actions[2].title)
    }

    private fun uiModel(
        mode: LocationPolicyMode = LocationPolicyMode.PowerSavingNormal,
        isOngoing: Boolean = mode != LocationPolicyMode.Off,
        requestLiveUpdate: Boolean = isOngoing,
        collapsedText: String = "定位中 · 刚刚",
        expandedText: String = "状态：定位中\n策略：省电档"
    ) = LocationNotificationUiModel(
        phase = if (isOngoing) LocationLiveUpdatePhase.Collecting else LocationLiveUpdatePhase.Paused,
        mode = mode,
        isOngoing = isOngoing,
        requestLiveUpdate = requestLiveUpdate,
        title = "PIM 定位",
        collapsedText = collapsedText,
        expandedText = expandedText,
        shortStatus = "省电",
        progressPercent = 40,
        contentAction = collectionControlAction(mode)
    )
}
