package com.pim.app.notifications

import android.app.Application
import androidx.core.app.NotificationCompat
import androidx.test.core.app.ApplicationProvider
import com.pim.app.TestPimApp
import com.pim.app.location.policy.LocationPolicyMode
import com.pim.app.location.service.ForegroundLocationController
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertSame
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = TestPimApp::class)
class LiveUpdateNotificationCompatTest {
    @Test
    fun applyIfSupportedOnSdk34WithLiveUpdateDoesNotThrowAndBuilds() {
        val context = ApplicationProvider.getApplicationContext<Application>()
        val builder = NotificationCompat.Builder(context, LocationNotificationRenderer.CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_menu_mylocation)
            .setContentTitle("PIM 定位")
            .setContentText("定位中 · 刚刚")
            .setOngoing(true)

        val result = LiveUpdateNotificationCompat.applyIfSupported(builder, uiModel(requestLiveUpdate = true))

        assertSame(builder, result)
        val notification = result.build()
        assertNotNull(notification)
        assertEquals(LocationNotificationRenderer.CHANNEL_ID, notification.channelId)
    }

    @Test
    fun applyIfSupportedReturnsSameBuilderWhenLiveUpdateNotRequested() {
        val context = ApplicationProvider.getApplicationContext<Application>()
        val builder = NotificationCompat.Builder(context, LocationNotificationRenderer.CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_menu_mylocation)
            .setContentTitle("PIM 定位")
            .setContentText("定位已暂停")
            .setOngoing(false)

        val result = LiveUpdateNotificationCompat.applyIfSupported(
            builder,
            uiModel(
                mode = LocationPolicyMode.Off,
                isOngoing = false,
                requestLiveUpdate = false
            )
        )

        assertSame(builder, result)
        assertNotNull(result.build())
    }

    private fun uiModel(
        mode: LocationPolicyMode = LocationPolicyMode.PowerSavingNormal,
        isOngoing: Boolean = mode != LocationPolicyMode.Off,
        requestLiveUpdate: Boolean = isOngoing
    ) = LocationNotificationUiModel(
        phase = if (isOngoing) LocationLiveUpdatePhase.Collecting else LocationLiveUpdatePhase.Paused,
        mode = mode,
        isOngoing = isOngoing,
        requestLiveUpdate = requestLiveUpdate,
        title = "PIM 定位",
        collapsedText = if (isOngoing) "定位中 · 刚刚" else "定位已暂停",
        expandedText = if (isOngoing) "状态：定位中\n策略：省电档" else "状态：已暂停\n策略：已暂停",
        shortStatus = if (isOngoing) "省电" else "已暂停",
        progressPercent = if (isOngoing) 40 else null,
        contentAction = CollectionControlAction(
            label = if (isOngoing) "暂停" else "恢复",
            action = if (isOngoing) {
                ForegroundLocationController.ACTION_PAUSE_COLLECTION
            } else {
                ForegroundLocationController.ACTION_RESUME_COLLECTION
            }
        )
    )
}
