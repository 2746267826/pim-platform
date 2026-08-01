package com.pim.app.location.service

import android.app.Application
import android.content.Intent
import androidx.test.core.app.ApplicationProvider
import com.pim.app.TestPimApp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = TestPimApp::class)
class ForegroundLocationControllerTest {
    @Test
    fun pauseSendsPauseCollectionAction() {
        val context = ApplicationProvider.getApplicationContext<Application>()
        val controller = ForegroundLocationController(context)
        drainStartedServices(context)

        controller.pause()

        val intent = shadowOf(context).nextStartedService
        assertNotNull("pause must send a service intent", intent)
        assertEquals(ForegroundLocationController.ACTION_PAUSE_COLLECTION, intent?.action)
    }

    @Test
    fun stopSendsStopCollectionAction() {
        val context = ApplicationProvider.getApplicationContext<Application>()
        val controller = ForegroundLocationController(context)
        drainStartedServices(context)

        controller.stop()

        val intent = shadowOf(context).nextStartedService
        assertNotNull("stop must send a service intent", intent)
        assertEquals(ForegroundLocationController.ACTION_STOP_COLLECTION, intent?.action)
    }

    @Test
    fun startManualSessionSendsStartManualSessionAction() {
        val context = ApplicationProvider.getApplicationContext<Application>()
        val controller = ForegroundLocationController(context)
        drainStartedServices(context)

        controller.startManualSession()

        val intent = shadowOf(context).nextStartedService
        assertNotNull("startManualSession must send a service intent", intent)
        assertEquals(ForegroundLocationController.ACTION_START_MANUAL_SESSION, intent?.action)
    }

    @Test
    fun startManualSessionUsesForegroundService() {
        val context = ApplicationProvider.getApplicationContext<Application>()
        val controller = ForegroundLocationController(context)
        drainStartedServices(context)

        controller.startManualSession()

        val intent = shadowOf(context).nextStartedService
        assertNotNull(intent)
        // Robolectric shadows startForegroundService as nextStartedService,
        // so the intent comes from startForegroundService.
        assertEquals(ForegroundLocationController.ACTION_START_MANUAL_SESSION, intent?.action)
    }

    @Test
    fun cancelLocationSessionWithNonNullIdSendsIntentWithExtra() {
        val context = ApplicationProvider.getApplicationContext<Application>()
        val controller = ForegroundLocationController(context)
        drainStartedServices(context)

        controller.cancelLocationSession("session-123")

        val intent = shadowOf(context).nextStartedService
        assertNotNull("cancelLocationSession must send a service intent", intent)
        assertEquals(ForegroundLocationController.ACTION_CANCEL_LOCATION_SESSION, intent?.action)
        assertEquals("session-123", intent?.getStringExtra(ForegroundLocationController.EXTRA_SESSION_ID))
    }

    @Test
    fun cancelLocationSessionWithNullIdSendsIntentWithoutExtra() {
        val context = ApplicationProvider.getApplicationContext<Application>()
        val controller = ForegroundLocationController(context)
        drainStartedServices(context)

        controller.cancelLocationSession(null)

        val intent = shadowOf(context).nextStartedService
        assertNotNull("cancelLocationSession(null) must send a service intent", intent)
        assertEquals(ForegroundLocationController.ACTION_CANCEL_LOCATION_SESSION, intent?.action)
        assertNull("EXTRA_SESSION_ID must be absent for null id",
            intent?.getStringExtra(ForegroundLocationController.EXTRA_SESSION_ID))
    }

    @Test
    fun openLocationIntentTargetsMainActivityWithLocationDestination() {
        val context = ApplicationProvider.getApplicationContext<Application>()
        val controller = ForegroundLocationController(context)

        val intent = controller.openLocationIntent()

        assertEquals(com.pim.app.MainActivity::class.java.name, intent.component?.className)
        assertEquals("location", intent.getStringExtra(ForegroundLocationController.EXTRA_OPEN_DESTINATION))
    }

    @Test
    fun openLocationIntentHasNewTaskClearTopAndSingleTop() {
        val context = ApplicationProvider.getApplicationContext<Application>()
        val controller = ForegroundLocationController(context)

        val intent = controller.openLocationIntent()

        assertTrue((intent.flags and Intent.FLAG_ACTIVITY_NEW_TASK) != 0)
        assertTrue((intent.flags and Intent.FLAG_ACTIVITY_CLEAR_TOP) != 0)
        assertTrue((intent.flags and Intent.FLAG_ACTIVITY_SINGLE_TOP) != 0)
    }

    @Test
    fun startManualSessionDoesNotMutateContinuousCollection() {
        val context = ApplicationProvider.getApplicationContext<Application>()
        val controller = ForegroundLocationController(context)
        drainStartedServices(context)

        controller.startManualSession()

        val intent = shadowOf(context).nextStartedService
        assertNotNull(intent)
        assertEquals(ForegroundLocationController.ACTION_START_MANUAL_SESSION, intent?.action)
    }

    private fun drainStartedServices(application: Application) {
        while (shadowOf(application).nextStartedService != null) {
            // Drain intents left by earlier actions in the shared Robolectric application.
        }
    }
}
