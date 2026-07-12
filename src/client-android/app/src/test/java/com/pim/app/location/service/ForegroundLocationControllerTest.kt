package com.pim.app.location.service

import android.app.Application
import android.content.Intent
import androidx.test.core.app.ApplicationProvider
import com.pim.app.TestPimApp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
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

    private fun drainStartedServices(application: Application) {
        while (shadowOf(application).nextStartedService != null) {
            // Drain intents left by earlier actions in the shared Robolectric application.
        }
    }
}
