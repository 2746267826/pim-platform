package com.pim.app.location.service

import android.Manifest
import android.app.Application
import android.app.Notification
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.os.Looper
import androidx.test.core.app.ApplicationProvider
import androidx.work.testing.WorkManagerTestInitHelper
import com.pim.app.TestPimApp
import com.pim.app.location.policy.LocationPolicyMode
import com.pim.app.mobile.sync.MobileSyncScheduler
import com.pim.app.notifications.LocationNotificationRenderer
import com.pim.app.notifications.LocationNotificationState
import com.pim.app.settings.TrackingSettingsStore
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.annotation.LooperMode

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = TestPimApp::class)
class ForegroundLocationServiceTest {

    @Test
    fun resolveRequestIntervalPreservedBelowSixtySeconds() {
        // ForegroundLocationService.requestLocationUpdates silently clamps to 60s,
        // overriding legal 30s and 45s intervals from presets.
        // The fix should expose a companion method that returns the interval unchanged.
        assertEquals(30_000L, ForegroundLocationService.resolveRequestInterval(30_000L))
        assertEquals(45_000L, ForegroundLocationService.resolveRequestInterval(45_000L))
        assertEquals(60_000L, ForegroundLocationService.resolveRequestInterval(60_000L))
        assertEquals(120_000L, ForegroundLocationService.resolveRequestInterval(120_000L))
    }

    @Test
    fun permissionDenialMustNotOverwritePersistedCollectionIntent() {
        val context = ApplicationProvider.getApplicationContext<Application>()
        shadowOf(context).denyPermissions(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_BACKGROUND_LOCATION
        )
        val prefs = context.getSharedPreferences("fg_perm_test", android.content.Context.MODE_PRIVATE)
        prefs.edit().clear().commit()
        val store = TrackingSettingsStore(prefs)
        store.setContinuousCollectionEnabled(true)
        assertTrue(store.read().continuousCollectionEnabled)

        val service = Robolectric.buildService(ForegroundLocationService::class.java).get()
        service.trackingSettingsStore = store
        service.onStartCommand(null, 0, 1)

        assertTrue(store.read().continuousCollectionEnabled)
    }

    private fun findNotification(nm: NotificationManager): Notification? {
        return nm.activeNotifications
            .firstOrNull { it.id == LocationNotificationRenderer.NOTIFICATION_ID }
            ?.notification
    }

    private fun pausedState() = LocationNotificationState(
        mode = LocationPolicyMode.Off,
        nextExpectedLocationText = "暂停",
        lastAcceptedLocationText = "无",
        lastAccuracyText = "无",
        pendingUploadCount = 0,
        apiState = "等待采集",
        lastDroppedReason = null
    )

    @Test
    fun pauseLeavesDismissibleResumeNotification() {
        val context = ApplicationProvider.getApplicationContext<Application>()
        val prefs = context.getSharedPreferences("fg_pause_test", Context.MODE_PRIVATE)
        prefs.edit().clear().commit()
        val store = TrackingSettingsStore(prefs)
        store.setContinuousCollectionEnabled(true)

        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.cancel(LocationNotificationRenderer.NOTIFICATION_ID)

        val service = Robolectric.buildService(ForegroundLocationService::class.java).get()
        service.trackingSettingsStore = store
        service.onStartCommand(
            Intent(context, ForegroundLocationService::class.java)
                .setAction(ForegroundLocationController.ACTION_PAUSE_COLLECTION),
            0, 1
        )

        assertFalse(store.read().continuousCollectionEnabled)

        val n = findNotification(nm)
        assertNotNull(n)
        assertTrue((n!!.flags and Notification.FLAG_ONGOING_EVENT) == 0)
        assertEquals("恢复", n.actions!![0].title.toString())

        service.onDestroy()
        val n2 = findNotification(nm)
        assertNotNull("暂停通知在 onDestroy 后应保留", n2)
        assertTrue("暂停通知在 onDestroy 后仍应是非 ongoing", (n2!!.flags and Notification.FLAG_ONGOING_EVENT) == 0)
    }

    @Test
    fun explicitStopCancelsPausedNotification() {
        val context = ApplicationProvider.getApplicationContext<Application>()
        val prefs = context.getSharedPreferences("fg_stop_test", Context.MODE_PRIVATE)
        prefs.edit().clear().commit()
        val store = TrackingSettingsStore(prefs)
        store.setContinuousCollectionEnabled(true)

        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.notify(
            LocationNotificationRenderer.NOTIFICATION_ID,
            LocationNotificationRenderer.build(context, pausedState())
        )

        assertTrue(nm.activeNotifications.any { it.id == LocationNotificationRenderer.NOTIFICATION_ID })

        val service = Robolectric.buildService(ForegroundLocationService::class.java).get()
        service.trackingSettingsStore = store
        service.onStartCommand(
            Intent(context, ForegroundLocationService::class.java)
                .setAction(ForegroundLocationController.ACTION_STOP_COLLECTION),
            0, 1
        )

        assertFalse(store.read().continuousCollectionEnabled)
        assertFalse(nm.activeNotifications.any { it.id == LocationNotificationRenderer.NOTIFICATION_ID })
    }

    @Test
    fun pauseStopsOnlyItsOwnStartId() {
        val context = ApplicationProvider.getApplicationContext<Application>()
        val prefs = context.getSharedPreferences("fg_pause_stopself_test", Context.MODE_PRIVATE)
        prefs.edit().clear().commit()
        val store = TrackingSettingsStore(prefs)

        val service = Robolectric.buildService(ForegroundLocationService::class.java).get()
        service.trackingSettingsStore = store
        service.onStartCommand(
            Intent(context, ForegroundLocationService::class.java)
                .setAction(ForegroundLocationController.ACTION_PAUSE_COLLECTION),
            0, 41
        )

        assertEquals(41, shadowOf(service).stopSelfId)
    }

    @Test
    fun explicitStopStopsOnlyItsOwnStartId() {
        val context = ApplicationProvider.getApplicationContext<Application>()
        val prefs = context.getSharedPreferences("fg_stop_stopself_test", Context.MODE_PRIVATE)
        prefs.edit().clear().commit()
        val store = TrackingSettingsStore(prefs)

        val service = Robolectric.buildService(ForegroundLocationService::class.java).get()
        service.trackingSettingsStore = store
        service.onStartCommand(
            Intent(context, ForegroundLocationService::class.java)
                .setAction(ForegroundLocationController.ACTION_STOP_COLLECTION),
            0, 42
        )

        assertEquals(42, shadowOf(service).stopSelfId)
    }

    @Test
    fun resumeAttemptClearsPausedLifecycleFlag() {
        val context = ApplicationProvider.getApplicationContext<Application>()
        shadowOf(context).denyPermissions(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_BACKGROUND_LOCATION
        )
        val prefs = context.getSharedPreferences("fg_resume_clear_flag_test", Context.MODE_PRIVATE)
        prefs.edit().clear().commit()
        val store = TrackingSettingsStore(prefs)

        val service = Robolectric.buildService(ForegroundLocationService::class.java).get()
        service.trackingSettingsStore = store

        // PAUSE sets isPausing = true
        service.onStartCommand(
            Intent(context, ForegroundLocationService::class.java)
                .setAction(ForegroundLocationController.ACTION_PAUSE_COLLECTION),
            0, 1
        )

        // Call onStartCommand with null intent — should reset isPausing
        service.onStartCommand(null, 0, 1)

        // Reflectively read isPausing
        val isPausingField = ForegroundLocationService::class.java.getDeclaredField("isPausing")
        isPausingField.isAccessible = true
        assertFalse(isPausingField.getBoolean(service))
    }

    @Test
    fun resumePermissionDenialPreservesRequestedCollectionIntent() {
        val context = ApplicationProvider.getApplicationContext<Application>()
        shadowOf(context).denyPermissions(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_BACKGROUND_LOCATION
        )
        val prefs = context.getSharedPreferences("fg_resume_perm_deny_test", Context.MODE_PRIVATE)
        prefs.edit().clear().commit()
        val store = TrackingSettingsStore(prefs)
        // initial: continuousCollectionEnabled == false (default)
        assertFalse(store.read().continuousCollectionEnabled)

        val service = Robolectric.buildService(ForegroundLocationService::class.java).get()
        service.trackingSettingsStore = store
        service.onStartCommand(
            Intent(context, ForegroundLocationService::class.java)
                .setAction(ForegroundLocationController.ACTION_RESUME_COLLECTION),
            0, 55
        )

        assertTrue(store.read().continuousCollectionEnabled)
        assertEquals(55, shadowOf(service).stopSelfId)
    }

    @Test
    fun syncWhilePausedPreservesResumeNotificationAndShowsResult() {
        val context = ApplicationProvider.getApplicationContext<Application>()
        WorkManagerTestInitHelper.initializeTestWorkManager(context)

        val prefs = context.getSharedPreferences("fg_sync_paused_test", Context.MODE_PRIVATE)
        prefs.edit().clear().commit()
        val store = TrackingSettingsStore(prefs)
        store.setContinuousCollectionEnabled(true)

        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.cancel(LocationNotificationRenderer.NOTIFICATION_ID)

        // service1: PAUSE then onDestroy
        val service1 = Robolectric.buildService(ForegroundLocationService::class.java).get()
        service1.trackingSettingsStore = store
        service1.onStartCommand(
            Intent(context, ForegroundLocationService::class.java)
                .setAction(ForegroundLocationController.ACTION_PAUSE_COLLECTION),
            0, 1
        )

        var n = findNotification(nm)
        assertNotNull(n)
        assertTrue((n!!.flags and Notification.FLAG_ONGOING_EVENT) == 0)
        assertEquals("恢复", n.actions!![0].title.toString())

        service1.onDestroy()

        val nAfterDestroy = findNotification(nm)
        assertNotNull("暂停通知在 onDestroy 后应保留", nAfterDestroy)
        assertTrue("暂停通知在 onDestroy 后仍应是非 ongoing",
            (nAfterDestroy!!.flags and Notification.FLAG_ONGOING_EVENT) == 0)

        // service2: simulate service rebuild, inject same store + scheduler
        val scheduler = MobileSyncScheduler(context, store)
        val service2 = Robolectric.buildService(ForegroundLocationService::class.java).get()
        service2.trackingSettingsStore = store
        service2.mobileSyncScheduler = scheduler

        service2.onStartCommand(
            Intent(context, ForegroundLocationService::class.java)
                .setAction(ForegroundLocationController.ACTION_SYNC_NOW),
            0, 73
        )

        // idle main looper to let the coroutine complete
        shadowOf(Looper.getMainLooper()).idle()

        n = findNotification(nm)
        assertNotNull("sync 后暂停通知仍应存在", n)
        assertTrue("sync 后通知仍应是非 ongoing",
            (n!!.flags and Notification.FLAG_ONGOING_EVENT) == 0)
        assertEquals("恢复", n.actions!![0].title.toString())
        val syncMsg = n!!.extras?.getCharSequence(android.app.Notification.EXTRA_TEXT)?.toString() ?: ""
        assertTrue(
            "collapsedText 应包含同步状态: $syncMsg",
            syncMsg.contains("同步请求已提交")
        )
        assertEquals(73, shadowOf(service2).stopSelfId)

        service2.onDestroy()
        val nFinal = findNotification(nm)
        assertNotNull("onDestroy 后暂停通知仍应保留", nFinal)
    }

    // Separate thread in this test queues Dispatchers.Main.immediate so the
    // first notification can be checked before the coroutine completes.
    @Test
    @LooperMode(LooperMode.Mode.PAUSED)
    fun syncFromNewServiceAfterPauseMustNotShowPowerSavingTransientState() {
        val context = ApplicationProvider.getApplicationContext<Application>()
        WorkManagerTestInitHelper.initializeTestWorkManager(context)

        val prefs = context.getSharedPreferences("fg_transient_free_sync", Context.MODE_PRIVATE)
        prefs.edit().clear().commit()
        val store = TrackingSettingsStore(prefs)
        store.setContinuousCollectionEnabled(true)

        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.cancel(LocationNotificationRenderer.NOTIFICATION_ID)

        val service1 = Robolectric.buildService(ForegroundLocationService::class.java).get()
        service1.trackingSettingsStore = store
        service1.onStartCommand(
            Intent(context, ForegroundLocationService::class.java)
                .setAction(ForegroundLocationController.ACTION_PAUSE_COLLECTION),
            0, 1
        )
        service1.onDestroy()

        val nBefore = findNotification(nm)
        assertNotNull("暂停通知应存在", nBefore)

        val scheduler = MobileSyncScheduler(context, store)
        val service2 = Robolectric.buildService(ForegroundLocationService::class.java).get()
        service2.trackingSettingsStore = store
        service2.mobileSyncScheduler = scheduler

        val executor = Executors.newSingleThreadExecutor()
        try {
            executor.submit {
                service2.onStartCommand(
                    Intent(context, ForegroundLocationService::class.java)
                        .setAction(ForegroundLocationController.ACTION_SYNC_NOW),
                    0, 74
                )
            }.get(5, TimeUnit.SECONDS)
        } finally {
            executor.shutdownNow()
        }

        val n = findNotification(nm)
        assertNotNull("同步协程执行前通知应存在", n)
        assertEquals(
            "第一 action 应为恢复（保持暂停状态）",
            "恢复",
            n!!.actions!![0].title.toString()
        )
        assertTrue((n.flags and Notification.FLAG_ONGOING_EVENT) == 0)
        val collapsed = n.extras?.getCharSequence(android.app.Notification.EXTRA_TEXT)?.toString() ?: ""
        assertTrue("collapsedText 应包含已暂停: $collapsed", collapsed.contains("已暂停"))
        assertFalse("collapsedText 不应包含省电档: $collapsed", collapsed.contains("省电档"))
        assertTrue("collapsedText 应包含同步中状态: $collapsed", collapsed.contains("同步中"))

        shadowOf(Looper.getMainLooper()).idle()
        val completed = findNotification(nm)
        assertNotNull("同步完成后通知应存在", completed)
        val completedText = completed!!.extras
            ?.getCharSequence(android.app.Notification.EXTRA_TEXT)
            ?.toString()
            .orEmpty()
        assertTrue(completedText.contains("同步请求已提交"))

        service2.onDestroy()
    }

    @Test
    fun syncAfterPauseOnSameInstancePreservesResumeNotification() {
        val context = ApplicationProvider.getApplicationContext<Application>()
        WorkManagerTestInitHelper.initializeTestWorkManager(context)

        val prefs = context.getSharedPreferences("fg_same_instance_pause_sync", Context.MODE_PRIVATE)
        prefs.edit().clear().commit()
        val store = TrackingSettingsStore(prefs)
        store.setContinuousCollectionEnabled(true)

        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.cancel(LocationNotificationRenderer.NOTIFICATION_ID)

        val service = Robolectric.buildService(ForegroundLocationService::class.java).get()
        service.trackingSettingsStore = store
        // PAUSE on this instance
        service.onStartCommand(
            Intent(context, ForegroundLocationService::class.java)
                .setAction(ForegroundLocationController.ACTION_PAUSE_COLLECTION),
            0, 41
        )

        // The PAUSE stops the service, so a fresh service would normally be needed.
        // But for the regression test we simulate: paused notification is active.
        // Send SYNC to the same service instance
        val scheduler = MobileSyncScheduler(context, store)
        service.mobileSyncScheduler = scheduler

        service.onStartCommand(
            Intent(context, ForegroundLocationService::class.java)
                .setAction(ForegroundLocationController.ACTION_SYNC_NOW),
            0, 42
        )

        shadowOf(Looper.getMainLooper()).idle()

        val n = findNotification(nm)
        assertNotNull("PAUSE+SYNC 后暂停通知应存在", n)
        assertTrue("通知应为非 ongoing", (n!!.flags and Notification.FLAG_ONGOING_EVENT) == 0)
        assertEquals("恢复", n.actions!![0].title.toString())
        val collapsed = n.extras?.getCharSequence(android.app.Notification.EXTRA_TEXT)?.toString() ?: ""
        assertTrue("应包含已暂停: $collapsed", collapsed.contains("已暂停"))
        assertTrue("应包含同步结果: $collapsed", collapsed.contains("同步请求已提交"))

        service.onDestroy()
    }

    @Test
    fun syncAfterStopDoesNotLeaveNotification() {
        val context = ApplicationProvider.getApplicationContext<Application>()
        WorkManagerTestInitHelper.initializeTestWorkManager(context)

        val prefs = context.getSharedPreferences("fg_stop_then_sync", Context.MODE_PRIVATE)
        prefs.edit().clear().commit()
        val store = TrackingSettingsStore(prefs)

        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.cancel(LocationNotificationRenderer.NOTIFICATION_ID)
        assertFalse("不应有残留通知", nm.activeNotifications.any { it.id == LocationNotificationRenderer.NOTIFICATION_ID })

        val service = Robolectric.buildService(ForegroundLocationService::class.java).get()
        service.trackingSettingsStore = store

        // STOP_COLLECTION (no prior PAUSE — collection was never enabled)
        service.onStartCommand(
            Intent(context, ForegroundLocationService::class.java)
                .setAction(ForegroundLocationController.ACTION_STOP_COLLECTION),
            0, 1
        )

        assertFalse("STOP 后不应有通知", nm.activeNotifications.any { it.id == LocationNotificationRenderer.NOTIFICATION_ID })

        // Now SYNC — should NOT restore a notification
        val scheduler = MobileSyncScheduler(context, store)
        service.mobileSyncScheduler = scheduler

        service.onStartCommand(
            Intent(context, ForegroundLocationService::class.java)
                .setAction(ForegroundLocationController.ACTION_SYNC_NOW),
            0, 2
        )

        shadowOf(Looper.getMainLooper()).idle()

        assertFalse("STOP+SYNC 后不应有通知", nm.activeNotifications.any { it.id == LocationNotificationRenderer.NOTIFICATION_ID })

        service.onDestroy()
    }
}
