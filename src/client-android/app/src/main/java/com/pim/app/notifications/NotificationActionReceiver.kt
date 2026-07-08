package com.pim.app.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.pim.app.location.service.ForegroundLocationController
import com.pim.app.ui.shell.PimShellActivity
import com.pim.core.network.ApiClientProvider
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@AndroidEntryPoint
class NotificationActionReceiver : BroadcastReceiver() {
    @Inject lateinit var apiClientProvider: ApiClientProvider
    @Inject lateinit var foregroundLocationController: ForegroundLocationController

    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            ForegroundLocationController.ACTION_PAUSE_COLLECTION -> {
                foregroundLocationController.stop()
                return
            }
            ForegroundLocationController.ACTION_RESUME_COLLECTION -> {
                foregroundLocationController.start()
                return
            }
            ForegroundLocationController.ACTION_SYNC_NOW -> {
                foregroundLocationController.syncNow()
                return
            }
            ForegroundLocationController.ACTION_OPEN_STATUS -> {
                context.startActivity(foregroundLocationController.openStatusIntent())
                return
            }
        }

        val action = intent.getStringExtra(EXTRA_ACTION).orEmpty()
        val riskLevel = intent.getStringExtra(EXTRA_RISK_LEVEL).orEmpty()
        val confirmationId = intent.getStringExtra(EXTRA_CONFIRMATION_ID)
        val relatedObjectType = intent.getStringExtra(EXTRA_RELATED_OBJECT_TYPE)
        val relatedObjectId = intent.getStringExtra(EXTRA_RELATED_OBJECT_ID)
        val route = PimNotificationRouter().route(
            action = action,
            riskLevel = riskLevel,
            confirmationId = confirmationId,
            relatedObjectType = relatedObjectType,
            relatedObjectId = relatedObjectId,
            isOnline = intent.getBooleanExtra(EXTRA_ONLINE, true)
        )

        when (route) {
            NotificationRoute.ExecuteOnline -> {
                val pending = goAsync()
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        EndpointNotificationActionDispatcher(apiClientProvider.apiService()).execute(
                            deviceId = notificationDeviceId(intent),
                            action = action,
                            riskLevel = riskLevel,
                            confirmationId = confirmationId,
                            relatedObjectType = relatedObjectType,
                            relatedObjectId = relatedObjectId
                        )
                    } finally {
                        pending.finish()
                    }
                }
            }
            is NotificationRoute.OpenDetail -> {
                context.startActivity(
                    PimShellActivity.intentFor(context, route.detailUrl)
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                )
            }
            NotificationRoute.RetryWhenOnline -> {
                context.startActivity(
                    PimShellActivity.intentFor(context, "/endpoint-shell")
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                )
            }
        }
    }

    companion object {
        const val EXTRA_ACTION = "com.pim.app.extra.ACTION"
        const val EXTRA_RISK_LEVEL = "com.pim.app.extra.RISK_LEVEL"
        const val EXTRA_CONFIRMATION_ID = "com.pim.app.extra.CONFIRMATION_ID"
        const val EXTRA_RELATED_OBJECT_TYPE = "com.pim.app.extra.RELATED_OBJECT_TYPE"
        const val EXTRA_RELATED_OBJECT_ID = "com.pim.app.extra.RELATED_OBJECT_ID"
        const val EXTRA_ONLINE = "com.pim.app.extra.ONLINE"
        const val EXTRA_DEVICE_ID = "com.pim.app.extra.DEVICE_ID"

        private fun notificationDeviceId(intent: Intent): String =
            intent.getStringExtra(EXTRA_DEVICE_ID)
                ?.takeIf { it.isNotBlank() }
                ?: "android-companion"
    }
}
