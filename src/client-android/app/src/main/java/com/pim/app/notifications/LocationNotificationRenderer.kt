package com.pim.app.notifications

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.pim.app.MainActivity
import com.pim.app.location.policy.LocationPolicyMode
import com.pim.app.location.service.ForegroundLocationController

data class CollectionControlAction(
    val label: String,
    val action: String
)

fun collectionControlAction(mode: LocationPolicyMode): CollectionControlAction {
    return when (mode) {
        LocationPolicyMode.Off -> CollectionControlAction("恢复", ForegroundLocationController.ACTION_RESUME_COLLECTION)
        else -> CollectionControlAction("暂停", ForegroundLocationController.ACTION_PAUSE_COLLECTION)
    }
}

object LocationNotificationRenderer {
    const val CHANNEL_ID = "pim_location_collection"
    const val NOTIFICATION_ID = 7101

    fun build(context: Context, model: LocationNotificationUiModel): Notification {
        ensureChannel(context)
        val control = model.contentAction
        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_menu_mylocation)
            .setContentTitle(model.title)
            .setContentText(model.collapsedText)
            .setStyle(NotificationCompat.BigTextStyle().bigText(model.expandedText))
            .setOngoing(model.isOngoing)
            .setOnlyAlertOnce(true)
            .setContentIntent(openStatusPendingIntent(context))
            .addAction(0, control.label, receiverPendingIntent(context, control.action, 10))
            .addAction(0, "同步", receiverPendingIntent(context, ForegroundLocationController.ACTION_SYNC_NOW, 11))
            .addAction(0, "状态", receiverPendingIntent(context, ForegroundLocationController.ACTION_OPEN_STATUS, 12))
        return LiveUpdateNotificationCompat.applyIfSupported(builder, model).build()
    }

    private fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channel = NotificationChannel(
            CHANNEL_ID,
            "PIM 持续定位",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "显示当前定位策略、队列、精度和连接状态"
        }
        manager.createNotificationChannel(channel)
    }

    private fun openStatusPendingIntent(context: Context): PendingIntent {
        val intent = Intent(context, MainActivity::class.java)
            .putExtra(ForegroundLocationController.EXTRA_OPEN_DESTINATION, "status")
        return PendingIntent.getActivity(context, 20, intent, pendingIntentFlags())
    }

    private fun receiverPendingIntent(context: Context, action: String, requestCode: Int): PendingIntent {
        val intent = Intent(context, NotificationActionReceiver::class.java).setAction(action)
        return PendingIntent.getBroadcast(context, requestCode, intent, pendingIntentFlags())
    }

    private fun pendingIntentFlags(): Int {
        return PendingIntent.FLAG_UPDATE_CURRENT or
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0
    }
}
