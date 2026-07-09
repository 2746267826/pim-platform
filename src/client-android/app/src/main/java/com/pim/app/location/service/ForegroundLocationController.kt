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

    fun stop() {
        context.startService(serviceIntent(ACTION_PAUSE_COLLECTION))
    }

    fun syncNow() {
        ContextCompat.startForegroundService(context, serviceIntent(ACTION_SYNC_NOW))
    }

    fun openStatusIntent(): Intent {
        return Intent(context, MainActivity::class.java)
            .putExtra(EXTRA_OPEN_DESTINATION, "status")
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }

    private fun serviceIntent(action: String): Intent {
        return Intent(context, ForegroundLocationService::class.java).setAction(action)
    }

    companion object {
        const val ACTION_START_COLLECTION = "com.pim.app.location.action.START_COLLECTION"
        const val ACTION_PAUSE_COLLECTION = "com.pim.app.location.action.PAUSE_COLLECTION"
        const val ACTION_RESUME_COLLECTION = "com.pim.app.location.action.RESUME_COLLECTION"
        const val ACTION_SYNC_NOW = "com.pim.app.location.action.SYNC_NOW"
        const val ACTION_OPEN_STATUS = "com.pim.app.location.action.OPEN_STATUS"
        const val EXTRA_OPEN_DESTINATION = "com.pim.app.location.extra.OPEN_DESTINATION"
    }
}
