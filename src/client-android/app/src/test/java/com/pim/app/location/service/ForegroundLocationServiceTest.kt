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
import com.pim.app.location.policy.PolicyDecision
import com.pim.app.location.policy.ScheduleWindow
import com.pim.app.mobile.sync.MobileSyncScheduler
import com.pim.app.notifications.LocationNotificationRenderer
import com.pim.app.notifications.LocationNotificationState
import com.pim.app.schedule.ScheduleCacheDocument
import com.pim.app.schedule.ScheduleCacheFreshness
import com.pim.app.schedule.ScheduleCacheSnapshot
import com.pim.app.schedule.ScheduleCacheStore
import com.pim.app.schedule.ScheduleCacheWindow
import com.pim.app.schedule.ScheduleWindowRepository
import com.pim.app.settings.TrackingSettingsStore
import com.pim.core.auth.AuthSessionSnapshot
import com.pim.core.auth.AuthSessionStore
import com.pim.core.models.ApiResponse
import com.pim.core.models.EventResponse
import com.pim.core.network.ApiService
import com.pim.core.settings.PimServerEndpoints
import com.pim.core.settings.ServerSettingsStore
import java.io.IOException
import java.lang.reflect.Proxy
import java.time.Instant
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Job
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
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
    private val cacheDirs = mutableListOf<java.io.File>()

    @org.junit.After
    fun cleanUp() {
        cacheDirs.forEach { it.deleteRecursively() }
        cacheDirs.clear()
    }

    @Test
    fun runtimeStateDefaultApiStateMatchesScheduleWaitingText() {
        assertEquals("等待日程数据", ForegroundLocationRuntimeState().apiState)
    }

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
    fun resolveLocationPriorityMapsPolicyModes() {
        assertEquals(
            com.google.android.gms.location.Priority.PRIORITY_BALANCED_POWER_ACCURACY,
            ForegroundLocationService.resolveLocationPriority(LocationPolicyMode.PowerSavingNormal)
        )
        assertEquals(
            com.google.android.gms.location.Priority.PRIORITY_BALANCED_POWER_ACCURACY,
            ForegroundLocationService.resolveLocationPriority(LocationPolicyMode.ScheduleLowFrequency)
        )
        assertEquals(
            com.google.android.gms.location.Priority.PRIORITY_BALANCED_POWER_ACCURACY,
            ForegroundLocationService.resolveLocationPriority(LocationPolicyMode.Off)
        )
        assertEquals(
            com.google.android.gms.location.Priority.PRIORITY_BALANCED_POWER_ACCURACY,
            ForegroundLocationService.resolveLocationPriority(LocationPolicyMode.SyncFallback)
        )
        assertEquals(
            com.google.android.gms.location.Priority.PRIORITY_HIGH_ACCURACY,
            ForegroundLocationService.resolveLocationPriority(LocationPolicyMode.MotionObservation)
        )
        assertEquals(
            com.google.android.gms.location.Priority.PRIORITY_HIGH_ACCURACY,
            ForegroundLocationService.resolveLocationPriority(LocationPolicyMode.MovementRecovery)
        )
    }

    @Test
    fun resolveLocationPriorityCoversEveryPolicyMode() {
        LocationPolicyMode.entries.forEach { mode ->
            // Must not throw; exhaustive when on all enum values.
            ForegroundLocationService.resolveLocationPriority(mode)
        }
    }

    @Test
    fun shouldSkipLocationReregisterWhenIntervalAndPriorityMatch() {
        assertTrue(
            ForegroundLocationService.shouldSkipLocationReregister(
                registeredIntervalMillis = 60_000L,
                registeredPriority = com.google.android.gms.location.Priority.PRIORITY_HIGH_ACCURACY,
                hasActiveCallback = true,
                nextIntervalMillis = 60_000L,
                nextPriority = com.google.android.gms.location.Priority.PRIORITY_HIGH_ACCURACY
            )
        )
    }

    @Test
    fun shouldNotSkipLocationReregisterWhenPriorityChanges() {
        assertFalse(
            ForegroundLocationService.shouldSkipLocationReregister(
                registeredIntervalMillis = 60_000L,
                registeredPriority = com.google.android.gms.location.Priority.PRIORITY_BALANCED_POWER_ACCURACY,
                hasActiveCallback = true,
                nextIntervalMillis = 60_000L,
                nextPriority = com.google.android.gms.location.Priority.PRIORITY_HIGH_ACCURACY
            )
        )
    }

    @Test
    fun shouldNotSkipLocationReregisterWithoutActiveCallback() {
        assertFalse(
            ForegroundLocationService.shouldSkipLocationReregister(
                registeredIntervalMillis = 60_000L,
                registeredPriority = com.google.android.gms.location.Priority.PRIORITY_HIGH_ACCURACY,
                hasActiveCallback = false,
                nextIntervalMillis = 60_000L,
                nextPriority = com.google.android.gms.location.Priority.PRIORITY_HIGH_ACCURACY
            )
        )
    }

    @Test
    fun resolveMinUpdateIntervalUsesEightyPercentFloor() {
        assertEquals(48_000L, ForegroundLocationService.resolveMinUpdateIntervalMillis(60_000L))
        assertEquals(800L, ForegroundLocationService.resolveMinUpdateIntervalMillis(1_000L))
        assertEquals(1L, ForegroundLocationService.resolveMinUpdateIntervalMillis(1L))
    }

    @Test
    fun locationRequestRetryDelayIsThirtySeconds() {
        assertEquals(30_000L, ForegroundLocationService.LOCATION_REQUEST_RETRY_DELAY_MILLIS)
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
    fun nonPauseOnDestroyExplicitlyCancelsNotification() {
        val context = ApplicationProvider.getApplicationContext<Application>()
        val prefs = context.getSharedPreferences("fg_ondestroy_explicit_cancel", Context.MODE_PRIVATE)
        prefs.edit().clear().commit()
        val store = TrackingSettingsStore(prefs)

        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.cancel(LocationNotificationRenderer.NOTIFICATION_ID)

        val service = Robolectric.buildService(ForegroundLocationService::class.java).get()
        service.trackingSettingsStore = store

        // STOP_COLLECTION sets isPausing = false
        service.onStartCommand(
            Intent(context, ForegroundLocationService::class.java)
                .setAction(ForegroundLocationController.ACTION_STOP_COLLECTION),
            0, 1
        )

        // Simulate late coroutine re-posting (e.g. refreshScheduleWindows completing after STOP)
        val lateState = LocationNotificationState(
            mode = LocationPolicyMode.ScheduleLowFrequency,
            nextExpectedLocationText = "3 分钟后",
            lastAcceptedLocationText = "12:00",
            lastAccuracyText = "10m",
            pendingUploadCount = 1,
            apiState = "正常",
            lastDroppedReason = null
        )
        nm.notify(
            LocationNotificationRenderer.NOTIFICATION_ID,
            LocationNotificationRenderer.build(context, lateState)
        )
        assertTrue(
            "晚到协程重发后通知应可见",
            nm.activeNotifications.any { it.id == LocationNotificationRenderer.NOTIFICATION_ID }
        )

        // onDestroy for !isPausing should remove all notifications
        service.onDestroy()

        val n = findNotification(nm)
        assertNull("onDestroy 后应无通知残留", n)
    }

    @Test
    @LooperMode(LooperMode.Mode.PAUSED)
    fun cancelledScheduleRefreshDoesNotPublishFailureNotification() {
        val context = ApplicationProvider.getApplicationContext<Application>()
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.cancel(LocationNotificationRenderer.NOTIFICATION_ID)

        val apiService = Proxy.newProxyInstance(
            ApiService::class.java.classLoader,
            arrayOf(ApiService::class.java)
        ) { _, method, _ ->
            if (method.name == "getEvents") {
                throw CancellationException("test cancellation")
            }
            error("Unexpected ApiService call: ${method.name}")
        } as ApiService

        val service = Robolectric.buildService(ForegroundLocationService::class.java).get()
        val testCacheDir = java.io.File(context.filesDir, "fg-test-cache-" + java.lang.System.nanoTime())
        testCacheDir.mkdirs()
        cacheDirs.add(testCacheDir)
        val testCacheStore = com.pim.app.schedule.ScheduleCacheStore(testCacheDir, kotlinx.serialization.json.Json { ignoreUnknownKeys = true })
        val testAuthStore = object : com.pim.core.auth.AuthSessionStore {
            override fun snapshot() = com.pim.core.auth.AuthSessionSnapshot(null, null)
            override fun save(accessToken: String, refreshToken: String, expiresAtUtcMillis: Long, serverIdentity: String) = true
            override fun clear() = true
        }
        val testServerSettings = com.pim.core.settings.ServerSettingsStore(context, testAuthStore)
        kotlin.runCatching { testServerSettings.setBaseUrl("http://127.0.0.1:5858/api/v1/") }
        service.scheduleWindowRepository = ScheduleWindowRepository(apiService, testCacheStore, testServerSettings)

        val refresh = ForegroundLocationService::class.java
            .getDeclaredMethod("refreshScheduleWindows", Boolean::class.javaPrimitiveType)
            .apply { isAccessible = true }
        refresh.invoke(service, false)

        repeat(20) {
            Thread.sleep(25)
            shadowOf(Looper.getMainLooper()).idle()
        }

        assertNull(
            "取消日程刷新不应被当作 API 失败并重发通知",
            findNotification(nm)
        )
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

    @Test
    fun policyTransitionDeduperRecordsModeIntervalReasonChangesOnce() {
        val deduper = PolicyTransitionDeduper()
        val base = PolicyDecision(
            mode = LocationPolicyMode.PowerSavingNormal,
            requestIntervalMillis = 180_000L,
            nextExpectedLocationAtMillis = 1_000L,
            reason = "默认省电档",
            scheduleLowFrequency = false
        )

        val first = deduper.note(base)
        assertNotNull(first)
        assertNull(first!!.fromMode)
        assertEquals(LocationPolicyMode.PowerSavingNormal, first.decision.mode)

        assertNull("完全相同 decision 不应再写", deduper.note(base))

        val modeChanged = deduper.note(
            base.copy(mode = LocationPolicyMode.ScheduleLowFrequency, scheduleLowFrequency = true, reason = "日程低频")
        )
        assertNotNull(modeChanged)
        assertEquals(LocationPolicyMode.PowerSavingNormal, modeChanged!!.fromMode)
        assertEquals(LocationPolicyMode.ScheduleLowFrequency, modeChanged.decision.mode)

        val intervalChanged = deduper.note(
            modeChanged.decision.copy(requestIntervalMillis = 300_000L)
        )
        assertNotNull(intervalChanged)
        assertEquals(LocationPolicyMode.ScheduleLowFrequency, intervalChanged!!.fromMode)
        assertEquals(300_000L, intervalChanged.decision.requestIntervalMillis)

        val reasonChanged = deduper.note(
            intervalChanged.decision.copy(reason = "原因变更")
        )
        assertNotNull(reasonChanged)
        assertEquals("原因变更", reasonChanged!!.decision.reason)

        assertNull(
            deduper.note(reasonChanged.decision.copy(nextExpectedLocationAtMillis = 9_999L))
        )
    }

    @Test
    @LooperMode(LooperMode.Mode.PAUSED)
    fun stoppingCollectionCancelsScheduleRefreshAndSnapshotJobs() {
        val fixture = ScheduleServiceFixture()
        val started = CompletableDeferred<Unit>()
        val release = CompletableDeferred<Unit>()
        fixture.api.started = started
        fixture.api.block = release

        val service = fixture.createService()
        invokeRefresh(service)
        idleUntil {
            started.isCompleted &&
                isScheduleRefreshJobActive(service) &&
                isSnapshotCollectorActive(service)
        }

        invokeStopCollection(service)

        assertTrue("停止采集后应取消日程刷新协程", readScheduleRefreshJob(service)!!.isCancelled)
        assertTrue("停止采集后应取消日程快照收集协程", readSnapshotCollectorJob(service)!!.isCancelled)

        release.complete(Unit)
        service.onDestroy()
        fixture.cleanup()
    }

    @Test
    @LooperMode(LooperMode.Mode.PAUSED)
    fun refreshScheduleWindowsUsesRepositorySnapshotAndRuntimeFields() {
        val fixture = ScheduleServiceFixture()
        val now = System.currentTimeMillis()
        val startIso = Instant.ofEpochMilli(now - 60_000L).toString()
        val endIso = Instant.ofEpochMilli(now + 3_600_000L).toString()
        fixture.api.events = listOf(
            EventResponse(
                id = "evt-1",
                title = "会议",
                location = "办公室",
                dtStart = startIso,
                dtEnd = endIso
            )
        )

        val service = fixture.createService()
        invokeRefresh(service)
        idleUntil {
            ForegroundLocationService.runtimeState.value.scheduleFreshness == ScheduleCacheFreshness.Fresh
        }

        assertEquals(1, fixture.api.callCount)
        assertTrue("应走 refreshIfStale，不得调用 loadWindows 自定义范围", fixture.api.capturedStart != null)
        val runtime = ForegroundLocationService.runtimeState.value
        assertEquals(ScheduleCacheFreshness.Fresh, runtime.scheduleFreshness)
        assertNotNull(runtime.scheduleLastSuccessAtMillis)
        assertNotNull(runtime.scheduleLastAttemptAtMillis)
        assertNull(runtime.scheduleLastError)
        assertEquals("正常", runtime.apiState)
        assertEquals(
            listOf("evt-1"),
            readScheduleWindows(service).map { it.id }
        )
        service.onDestroy()
        fixture.cleanup()
    }

    @Test
    @LooperMode(LooperMode.Mode.PAUSED)
    fun refreshFailureKeepsStaleWindowsAndCollectionIntent() {
        val fixture = ScheduleServiceFixture()
        val identity = PimServerEndpoints.from(fixture.serverSettings.getBaseUrl()).apiBaseUrl.toString()
        val oldWindow = ScheduleCacheWindow(
            id = "cached",
            title = "旧会议",
            locationText = "A",
            startsAtMillis = 1_000L,
            endsAtMillis = 2_000L
        )
        fixture.cacheStore.write(
            identity,
            ScheduleCacheDocument(
                windows = listOf(oldWindow),
                rangeStartMillis = 0L,
                rangeEndMillis = 10_000L,
                lastAttemptAtMillis = 100L,
                lastSuccessAtMillis = 100L,
                lastError = null,
                lastErrorKind = null
            )
        )
        fixture.store.setContinuousCollectionEnabled(true)
        assertTrue(fixture.store.read().continuousCollectionEnabled)

        fixture.api.failNext = IOException("network down")
        val service = fixture.createService()
        invokeRefresh(service)
        idleUntil {
            ForegroundLocationService.runtimeState.value.scheduleLastError != null
        }

        val runtime = ForegroundLocationService.runtimeState.value
        assertEquals(ScheduleCacheFreshness.Stale, runtime.scheduleFreshness)
        assertEquals("网络不可用", runtime.scheduleLastError)
        assertEquals("日程缓存可能过期", runtime.apiState)
        assertEquals(listOf("cached"), readScheduleWindows(service).map { it.id })
        assertTrue(
            "刷新失败不得关闭 continuousCollectionEnabled",
            fixture.store.read().continuousCollectionEnabled
        )
        service.onDestroy()
        fixture.cleanup()
    }

    @Test
    @LooperMode(LooperMode.Mode.PAUSED)
    fun serverIdentityChangeClearsOldWindowsBeforeNewSnapshot() {
        val fixture = ScheduleServiceFixture()
        val oldIdentity = PimServerEndpoints.from(fixture.serverSettings.getBaseUrl()).apiBaseUrl.toString()
        fixture.cacheStore.write(
            oldIdentity,
            ScheduleCacheDocument(
                windows = listOf(
                    ScheduleCacheWindow("old-server", "旧服", "", 1_000L, 2_000L)
                ),
                rangeStartMillis = 0L,
                rangeEndMillis = 10_000L,
                lastAttemptAtMillis = 50L,
                lastSuccessAtMillis = 50L,
                lastError = null,
                lastErrorKind = null
            )
        )

        // Seed service memory with old windows via first successful refresh.
        fixture.api.events = listOf(
            EventResponse(
                id = "old-server",
                title = "旧服",
                location = null,
                dtStart = Instant.ofEpochMilli(1_000L).toString(),
                dtEnd = Instant.ofEpochMilli(2_000L).toString()
            )
        )
        val service = fixture.createService()
        invokeRefresh(service)
        idleUntil { readScheduleWindows(service).any { it.id == "old-server" } }
        // Repository may publish before the Main refresh job resumes; wait until it finishes
        // so the next refresh is not skipped by scheduleRefreshJob?.isActive.
        idleUntil { !isScheduleRefreshJobActive(service) }

        // Switch server; next refresh must clear old windows before applying new ones.
        fixture.serverSettings.setBaseUrl("http://other-server:5858/api/v1/")
        val started = CompletableDeferred<Unit>()
        val release = CompletableDeferred<Unit>()
        fixture.api.started = started
        fixture.api.block = release
        fixture.api.events = listOf(
            EventResponse(
                id = "new-server",
                title = "新服",
                location = null,
                dtStart = Instant.ofEpochMilli(3_000L).toString(),
                dtEnd = Instant.ofEpochMilli(4_000L).toString()
            )
        )

        invokeRefresh(service)
        // PAUSED looper: drain Main so the refresh job can reach Dispatchers.IO/API.
        idleUntil(timeoutMillis = 5_000L) { started.isCompleted }
        idleUntil(timeoutMillis = 5_000L) {
            readScheduleWindows(service).isEmpty()
        }
        assertTrue(
            "server identity 变化后应先清空旧窗口",
            readScheduleWindows(service).isEmpty()
        )
        release.complete(Unit)
        idleUntil { readScheduleWindows(service).any { it.id == "new-server" } }
        assertEquals(listOf("new-server"), readScheduleWindows(service).map { it.id })
        service.onDestroy()
        fixture.cleanup()
    }

    @Test
    @LooperMode(LooperMode.Mode.PAUSED)
    fun applyDecisionDedupesTransitionsAndPublishesRuntimePolicyFields() {
        val recorded = CopyOnWriteArrayList<Pair<LocationPolicyMode?, PolicyDecision>>()
        val service = Robolectric.buildService(ForegroundLocationService::class.java).get()
        setPolicyTransitionWriter(service) { from, decision ->
            recorded += from to decision
        }

        val d1 = PolicyDecision(
            mode = LocationPolicyMode.PowerSavingNormal,
            requestIntervalMillis = 180_000L,
            nextExpectedLocationAtMillis = 1_000L,
            reason = "默认省电档",
            scheduleLowFrequency = false
        )
        val d2 = d1.copy(mode = LocationPolicyMode.ScheduleLowFrequency, reason = "日程低频", scheduleLowFrequency = true)
        val d3 = d2.copy(requestIntervalMillis = 300_000L)
        val d4 = d3.copy(reason = "原因变更")

        invokeApplyDecision(service, d1)
        invokeApplyDecision(service, d1)
        invokeApplyDecision(service, d2)
        invokeApplyDecision(service, d3)
        invokeApplyDecision(service, d4)
        invokeApplyDecision(service, d4.copy(nextExpectedLocationAtMillis = 99_000L))
        shadowOf(Looper.getMainLooper()).idle()

        assertEquals(4, recorded.size)
        assertNull(recorded[0].first)
        assertEquals(LocationPolicyMode.PowerSavingNormal, recorded[0].second.mode)
        assertEquals(LocationPolicyMode.PowerSavingNormal, recorded[1].first)
        assertEquals(LocationPolicyMode.ScheduleLowFrequency, recorded[1].second.mode)
        assertEquals(LocationPolicyMode.ScheduleLowFrequency, recorded[2].first)
        assertEquals(300_000L, recorded[2].second.requestIntervalMillis)
        assertEquals("原因变更", recorded[3].second.reason)

        val runtime = ForegroundLocationService.runtimeState.value
        assertEquals(LocationPolicyMode.ScheduleLowFrequency.name, runtime.currentPolicyMode)
        assertEquals("原因变更", runtime.currentPolicyReason)
        assertEquals(300_000L, runtime.requestIntervalMillis)

        service.onDestroy()
    }

    @Test
    @LooperMode(LooperMode.Mode.PAUSED)
    fun cancelledScheduleRefreshDoesNotMarkApiFailureOrClearCollection() {
        val fixture = ScheduleServiceFixture()
        fixture.store.setContinuousCollectionEnabled(true)
        fixture.api.failNext = CancellationException("test cancellation")

        val nm = fixture.context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.cancel(LocationNotificationRenderer.NOTIFICATION_ID)

        val service = fixture.createService()
        invokeRefresh(service)
        repeat(10) {
            shadowOf(Looper.getMainLooper()).idle()
        }

        val runtime = ForegroundLocationService.runtimeState.value
        assertNotEquals("API 无法连接", runtime.apiState)
        assertFalse(
            (runtime.apiState ?: "").contains("API 失败") ||
                (runtime.apiState ?: "").contains("API 无法连接")
        )
        assertTrue(fixture.store.read().continuousCollectionEnabled)
        service.onDestroy()
        fixture.cleanup()
    }

    @Test
    @LooperMode(LooperMode.Mode.PAUSED)
    fun emptySuccessfulScheduleFollowedByFailureShowsStaleCacheState() {
        val fixture = ScheduleServiceFixture()
        val identity = PimServerEndpoints.from(fixture.serverSettings.getBaseUrl()).apiBaseUrl.toString()
        fixture.cacheStore.write(
            identity,
            ScheduleCacheDocument(
                windows = emptyList(),
                rangeStartMillis = 0L,
                rangeEndMillis = 10_000L,
                lastAttemptAtMillis = 100L,
                lastSuccessAtMillis = 100L,
                lastError = null,
                lastErrorKind = null
            )
        )
        fixture.api.failNext = IOException("network down")

        val service = fixture.createService()
        invokeRefresh(service)
        idleUntil { ForegroundLocationService.runtimeState.value.scheduleLastError != null }

        assertEquals(ScheduleCacheFreshness.Stale, ForegroundLocationService.runtimeState.value.scheduleFreshness)
        assertEquals("日程缓存可能过期", ForegroundLocationService.runtimeState.value.apiState)
        service.onDestroy()
        fixture.cleanup()
    }

    @Test
    @LooperMode(LooperMode.Mode.PAUSED)
    fun destroyingServicePreservesPublishedScheduleFacts() {
        val fixture = ScheduleServiceFixture()
        val now = System.currentTimeMillis()
        fixture.api.events = listOf(
            EventResponse(
                id = "persisted-fact",
                title = "事实",
                location = null,
                dtStart = Instant.ofEpochMilli(now - 1_000L).toString(),
                dtEnd = Instant.ofEpochMilli(now + 60_000L).toString()
            )
        )

        val service = fixture.createService()
        invokeRefresh(service)
        idleUntil { ForegroundLocationService.runtimeState.value.scheduleFreshness == ScheduleCacheFreshness.Fresh }
        val before = ForegroundLocationService.runtimeState.value

        service.onDestroy()

        val after = ForegroundLocationService.runtimeState.value
        assertEquals(false, after.isRunning)
        assertEquals(before.scheduleFreshness, after.scheduleFreshness)
        assertEquals(before.scheduleLastSuccessAtMillis, after.scheduleLastSuccessAtMillis)
        assertEquals(before.scheduleLastAttemptAtMillis, after.scheduleLastAttemptAtMillis)
        fixture.cleanup()
    }

    @Test
    fun serviceUsesRepositorySnapshotInsteadOfSecondScheduleList() {
        val relativePath = "app/src/main/java/com/pim/app/location/service/ForegroundLocationService.kt"
        val source = sequenceOf(
            java.io.File(relativePath),
            java.io.File(relativePath.removePrefix("app/")),
            java.io.File("..", relativePath)
        ).firstOrNull { it.isFile }?.readText()
            ?: error("source not found for $relativePath (cwd=${java.io.File(".").absolutePath})")

        assertFalse("service must not maintain a second schedule list", source.contains("private var scheduleWindows"))
        assertTrue(
            "location policy must read repository snapshot",
            source.contains("scheduleWindowRepository.snapshotForCurrentServer()") ||
                source.contains("scheduleWindowRepository.snapshot.value.windows")
        )
    }

    @Test
    fun acceptedLocationDecisionBranchRefreshesNotification() {
        val relativePath = "app/src/main/java/com/pim/app/location/service/ForegroundLocationService.kt"
        val source = sequenceOf(
            java.io.File(relativePath),
            java.io.File(relativePath.removePrefix("app/")),
            java.io.File("..", relativePath)
        ).firstOrNull { it.isFile }?.readText()
            ?: error("source not found for $relativePath (cwd=${java.io.File(".").absolutePath})")

        val decisionBranch = source
            .substringAfter("if (reduced != null) {")
            .substringBefore("} else {")
        assertTrue("accepted decision branch must apply the reduced policy", decisionBranch.contains("applyDecision(reduced)"))
        assertTrue("accepted decision branch must refresh the notification", decisionBranch.contains("updateNotification()"))
    }

    @Test
    fun freshSnapshotWithCacheErrorIsNotReportedAsNormal() {
        val service = Robolectric.buildService(ForegroundLocationService::class.java).get()
        val method = ForegroundLocationService::class.java
            .getDeclaredMethod("scheduleApiStateText", ScheduleCacheSnapshot::class.java)
            .apply { isAccessible = true }
        val text = method.invoke(
            service,
            ScheduleCacheSnapshot(
                serverIdentity = "https://server.example",
                windows = emptyList(),
                freshness = ScheduleCacheFreshness.Fresh,
                lastAttemptAtMillis = 2L,
                lastSuccessAtMillis = 2L,
                lastError = "本地日程缓存不可用",
                errorKind = com.pim.app.schedule.ScheduleRefreshErrorKind.Cache
            )
        ) as String

        assertEquals("日程缓存异常", text)
        service.onDestroy()
    }

    @Test
    @LooperMode(LooperMode.Mode.PAUSED)
    fun policyTransitionWriterDoesNotSwallowCancellation() {
        val service = Robolectric.buildService(ForegroundLocationService::class.java).get()
        setPolicyTransitionWriter(service) { _, _ ->
            throw CancellationException("cancel transition write")
        }

        invokeApplyDecision(
            service,
            PolicyDecision(
                mode = LocationPolicyMode.MotionObservation,
                requestIntervalMillis = 30_000L,
                nextExpectedLocationAtMillis = 1_000L,
                reason = "测试运动",
                scheduleLowFrequency = false
            )
        )
        shadowOf(Looper.getMainLooper()).idle()

        val field = ForegroundLocationService::class.java
            .getDeclaredField("policyTransitionWriteJob")
            .apply { isAccessible = true }
        assertTrue("取消必须传递到策略写入协程", (field.get(service) as Job).isCancelled)
        service.onDestroy()
    }

    @Test
    @LooperMode(LooperMode.Mode.PAUSED)
    fun policyTransitionWritesDoNotDropRapidChanges() {
        val service = Robolectric.buildService(ForegroundLocationService::class.java).get()
        val started = CompletableDeferred<Unit>()
        val release = CompletableDeferred<Unit>()
        val recorded = CopyOnWriteArrayList<String>()
        var first = true
        setPolicyTransitionWriter(service) { _, decision ->
            if (first) {
                first = false
                started.complete(Unit)
                release.await()
            }
            recorded += decision.reason
        }

        val firstDecision = PolicyDecision(
            mode = LocationPolicyMode.PowerSavingNormal,
            requestIntervalMillis = 180_000L,
            nextExpectedLocationAtMillis = 1_000L,
            reason = "第一条",
            scheduleLowFrequency = false
        )
        val secondDecision = firstDecision.copy(
            mode = LocationPolicyMode.MotionObservation,
            requestIntervalMillis = 30_000L,
            reason = "第二条"
        )
        invokeApplyDecision(service, firstDecision)
        runBlocking { withTimeout(5_000) { started.await() } }
        invokeApplyDecision(service, secondDecision)
        release.complete(Unit)
        shadowOf(Looper.getMainLooper()).idle()

        assertEquals(listOf("第一条", "第二条"), recorded.toList())
        service.onDestroy()
    }

    private fun invokeRefresh(service: ForegroundLocationService) {
        ForegroundLocationService::class.java
            .getDeclaredMethod("refreshScheduleWindows", Boolean::class.javaPrimitiveType)
            .apply { isAccessible = true }
            .invoke(service, false)
    }

    private fun invokeApplyDecision(service: ForegroundLocationService, decision: PolicyDecision) {
        ForegroundLocationService::class.java
            .getDeclaredMethod(
                "applyDecision",
                PolicyDecision::class.java,
                Boolean::class.javaPrimitiveType
            )
            .apply { isAccessible = true }
            .invoke(service, decision, true)
    }

    private fun readScheduleWindows(service: ForegroundLocationService): List<ScheduleWindow> {
        return service.scheduleWindowRepository.snapshot.value.windows
    }

    private fun isScheduleRefreshJobActive(service: ForegroundLocationService): Boolean {
        return readScheduleRefreshJob(service)?.isActive == true
    }

    private fun isSnapshotCollectorActive(service: ForegroundLocationService): Boolean {
        return readSnapshotCollectorJob(service)?.isActive == true
    }

    private fun readScheduleRefreshJob(service: ForegroundLocationService): Job? {
        val field = ForegroundLocationService::class.java.getDeclaredField("scheduleRefreshJob")
        field.isAccessible = true
        return field.get(service) as Job?
    }

    private fun readSnapshotCollectorJob(service: ForegroundLocationService): Job? {
        val field = ForegroundLocationService::class.java.getDeclaredField("snapshotCollectJob")
        field.isAccessible = true
        return field.get(service) as Job?
    }

    private fun invokeStopCollection(service: ForegroundLocationService) {
        ForegroundLocationService::class.java
            .getDeclaredMethod("stopCollection")
            .apply { isAccessible = true }
            .invoke(service)
    }

    private fun setPolicyTransitionWriter(
        service: ForegroundLocationService,
        writer: suspend (LocationPolicyMode?, PolicyDecision) -> Unit
    ) {
        val field = ForegroundLocationService::class.java.getDeclaredField("policyTransitionWriter")
        field.isAccessible = true
        field.set(service, writer)
    }

    private fun idleUntil(timeoutMillis: Long = 5_000L, predicate: () -> Boolean) {
        val deadline = System.nanoTime() + TimeUnit.MILLISECONDS.toNanos(timeoutMillis)
        while (!predicate()) {
            shadowOf(Looper.getMainLooper()).idle()
            if (System.nanoTime() > deadline) {
                throw AssertionError("condition not met within ${timeoutMillis}ms")
            }
            Thread.yield()
        }
        shadowOf(Looper.getMainLooper()).idle()
    }

    private class ScheduleServiceFixture {
        val context: Application = ApplicationProvider.getApplicationContext()
        val prefs = context.getSharedPreferences(
            "fg_schedule_fixture_" + System.nanoTime(),
            Context.MODE_PRIVATE
        ).also { it.edit().clear().commit() }
        val store = TrackingSettingsStore(prefs)
        val cacheDir = java.io.File(context.filesDir, "fg-schedule-cache-" + System.nanoTime()).also {
            it.mkdirs()
        }
        val cacheStore = ScheduleCacheStore(cacheDir, Json { ignoreUnknownKeys = true })
        val authStore = object : AuthSessionStore {
            override fun snapshot() = AuthSessionSnapshot(null, null)
            override fun save(
                accessToken: String,
                refreshToken: String,
                expiresAtUtcMillis: Long,
                serverIdentity: String
            ) = true
            override fun clear() = true
        }
        val serverSettings = ServerSettingsStore(context, authStore).also {
            it.setBaseUrl("http://test-server:5858/api/v1/")
        }
        val api = GatedApiService()
        val repository = ScheduleWindowRepository(api, cacheStore, serverSettings)

        fun createService(): ForegroundLocationService {
            val service = Robolectric.buildService(ForegroundLocationService::class.java).get()
            service.trackingSettingsStore = store
            service.scheduleWindowRepository = repository
            return service
        }

        fun cleanup() {
            cacheDir.deleteRecursively()
        }
    }

    private class GatedApiService : ApiService {
        var events: List<EventResponse> = emptyList()
        var failNext: Throwable? = null
        var callCount = 0
        var block: CompletableDeferred<Unit>? = null
        var started: CompletableDeferred<Unit>? = null
        var capturedStart: String? = null
        var capturedEnd: String? = null

        override suspend fun getEvents(start: String, end: String): ApiResponse<List<EventResponse>> {
            callCount++
            capturedStart = start
            capturedEnd = end
            started?.complete(Unit)
            block?.await()
            failNext?.let { t ->
                failNext = null
                throw t
            }
            return ApiResponse(code = 0, message = "ok", data = events)
        }

        override suspend fun login(body: com.pim.core.models.LoginRequest) = error("not mocked")
        override suspend fun register(body: com.pim.core.models.RegisterRequest) = error("not mocked")
        override suspend fun refresh(body: com.pim.core.models.RefreshRequest) = error("not mocked")
        override suspend fun getCalendars() = error("not mocked")
        override suspend fun createCalendar(body: com.pim.core.models.CreateCalendarRequest) = error("not mocked")
        override suspend fun createEvent(body: com.pim.core.models.CreateEventRequest) = error("not mocked")
        override suspend fun updateEvent(id: String, body: com.pim.core.models.CreateEventRequest) = error("not mocked")
        override suspend fun deleteEvent(id: String) = error("not mocked")
        override suspend fun getTasks(inbox: Boolean?) = error("not mocked")
        override suspend fun createTask(body: com.pim.core.models.CreateTaskRequest) = error("not mocked")
        override suspend fun updateTask(id: String, body: com.pim.core.models.CreateTaskRequest) = error("not mocked")
        override suspend fun deleteTask(id: String) = error("not mocked")
        override suspend fun search(query: String, type: String?) = error("not mocked")
        override suspend fun importIcs(body: okhttp3.RequestBody) = error("not mocked")
        override suspend fun exportIcs(start: String, end: String) = error("not mocked")
        override suspend fun syncOutlook() = error("not mocked")
        override suspend fun uploadStats(batch: com.pim.core.models.UploadBatch) = error("not mocked")
        override suspend fun registerMobileDevice(body: com.pim.core.models.MobileDeviceRegisterRequest) = error("not mocked")
        override suspend fun getMobileGaps(body: com.pim.core.models.MobileGapRequest) = error("not mocked")
        override suspend fun uploadMobileUsage(body: com.pim.core.models.MobileUsageEventsUploadRequest) = error("not mocked")
        override suspend fun uploadMobileLocation(body: com.pim.core.models.MobileLocationPointRequest) = error("not mocked")
        override suspend fun getMobileSummary(date: String?, deviceId: String?) = error("not mocked")
        override suspend fun getMobileTimeline(date: String?, deviceId: String?) = error("not mocked")
        override suspend fun getMobileQuality(date: String?, deviceId: String?, rangeStartUtc: String?, rangeEndUtc: String?) = error("not mocked")
        override suspend fun getMobileLocationHistory(rangeStartUtc: String?, rangeEndUtc: String?, deviceId: String?, maxAccuracyMeters: Double, includeRejected: Boolean, cursor: String?, pageSize: Int?) = error("not mocked")
        override suspend fun getMobileLocationOverview(rangeStartUtc: String, rangeEndUtc: String, deviceId: String?, maxAccuracyMeters: Double) = error("not mocked")
        override suspend fun getMobileLocationTracks(rangeStartUtc: String, rangeEndUtc: String, deviceId: String?, maxAccuracyMeters: Double) = error("not mocked")
        override suspend fun getMobileLocationSegmentPoints(segmentId: String, rangeStartUtc: String?, rangeEndUtc: String?, timezone: String?, deviceId: String?, maxAccuracyMeters: Double, includeRejected: Boolean, cursor: String?, pageSize: Int?) = error("not mocked")
        override suspend fun sendHeartbeat(body: com.pim.core.models.DaemonHeartbeatRequest) = error("not mocked")
        override suspend fun sendEndpointNotificationAction(deviceId: String, body: com.pim.core.models.EndpointNotificationActionRequestDto) = error("not mocked")
    }
}
