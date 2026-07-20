package com.pim.app.location.service

import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat
import com.pim.app.MainActivity
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ForegroundLocationController @Inject constructor(
    @ApplicationContext private val context: Context
) {
    fun start() {
        ContextCompat.startForegroundService(context, serviceIntent(ACTION_START_COLLECTION))
    }

    fun pause() {
        context.startService(serviceIntent(ACTION_PAUSE_COLLECTION))
    }

    fun stop() {
        context.startService(serviceIntent(ACTION_STOP_COLLECTION))
    }

    fun syncNow() {
        ContextCompat.startForegroundService(context, serviceIntent(ACTION_SYNC_NOW))
    }

    fun openStatusIntent(): Intent {
        return Intent(context, MainActivity::class.java)
            .putExtra(EXTRA_OPEN_DESTINATION, "status")
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }

    fun startManualSession() {
        ContextCompat.startForegroundService(context, serviceIntent(ACTION_START_MANUAL_SESSION))
    }

    fun cancelLocationSession(expectedSessionId: String?) {
        val intent = serviceIntent(ACTION_CANCEL_LOCATION_SESSION)
        if (expectedSessionId != null) {
            intent.putExtra(EXTRA_SESSION_ID, expectedSessionId)
        }
        context.startService(intent)
    }

    fun openLocationIntent(): Intent {
        return Intent(context, MainActivity::class.java)
            .putExtra(EXTRA_OPEN_DESTINATION, "location")
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
            .addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
    }

    private fun serviceIntent(action: String): Intent {
        return Intent(context, ForegroundLocationService::class.java).setAction(action)
    }

    companion object {
        const val ACTION_START_COLLECTION = "com.pim.app.location.action.START_COLLECTION"
        const val ACTION_PAUSE_COLLECTION = "com.pim.app.location.action.PAUSE_COLLECTION"
        const val ACTION_RESUME_COLLECTION = "com.pim.app.location.action.RESUME_COLLECTION"
        const val ACTION_STOP_COLLECTION = "com.pim.app.location.action.STOP_COLLECTION"
        const val ACTION_SYNC_NOW = "com.pim.app.location.action.SYNC_NOW"
        const val ACTION_OPEN_STATUS = "com.pim.app.location.action.OPEN_STATUS"
        const val EXTRA_OPEN_DESTINATION = "com.pim.app.location.extra.OPEN_DESTINATION"
        const val ACTION_START_MANUAL_SESSION = "com.pim.app.location.action.START_MANUAL_SESSION"
        const val ACTION_CANCEL_LOCATION_SESSION = "com.pim.app.location.action.CANCEL_LOCATION_SESSION"
        const val EXTRA_SESSION_ID = "com.pim.app.location.extra.SESSION_ID"
    }
}
