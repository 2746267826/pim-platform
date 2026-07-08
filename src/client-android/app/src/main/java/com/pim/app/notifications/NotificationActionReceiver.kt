package com.pim.app.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.pim.app.ui.shell.PimShellActivity

class NotificationActionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val route = PimNotificationRouter().route(
            action = intent.getStringExtra(EXTRA_ACTION).orEmpty(),
            riskLevel = intent.getStringExtra(EXTRA_RISK_LEVEL).orEmpty(),
            confirmationId = intent.getStringExtra(EXTRA_CONFIRMATION_ID),
            relatedObjectType = intent.getStringExtra(EXTRA_RELATED_OBJECT_TYPE),
            relatedObjectId = intent.getStringExtra(EXTRA_RELATED_OBJECT_ID),
            isOnline = intent.getBooleanExtra(EXTRA_ONLINE, true)
        )

        when (route) {
            NotificationRoute.ExecuteOnline -> {
                // The endpoint API records the action when online; high-risk work is routed to Web detail.
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
    }
}
