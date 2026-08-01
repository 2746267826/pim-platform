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

data class LocationNotificationState(
    val mode: LocationPolicyMode,
    val nextExpectedLocationText: String,
    val lastAcceptedLocationText: String,
    val lastAccuracyText: String,
    val pendingUploadTotal: Int,
    val apiState: String,
    val lastDroppedReason: String?
)

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

    fun collapsedText(state: LocationNotificationState): String {
        return listOf(
            modeLabel(state.mode),
            state.nextExpectedLocationText,
            "精度 ${state.lastAccuracyText}",
            "待上传 ${state.pendingUploadTotal}",
            state.apiState
        ).joinToString(" · ")
    }

    fun expandedText(state: LocationNotificationState): String {
        return buildList {
            add("策略：${modeLabel(state.mode)}")
            add("下次定位：${state.nextExpectedLocationText}")
            add("最近位置：${state.lastAcceptedLocationText}，精度 ${state.lastAccuracyText}")
            add("待上传 ${state.pendingUploadTotal}，${apiStateLabel(state.apiState)}")
            state.lastDroppedReason?.let { add("最近丢弃：$it") }
        }.joinToString("\n")
    }

    private fun apiStateLabel(apiState: String): String {
        return "API ${apiState.removePrefix("API ")}"
    }

    fun build(context: Context, state: LocationNotificationState): Notification {
        ensureChannel(context)
        val control = collectionControlAction(state.mode)
        val isOngoing = state.mode != LocationPolicyMode.Off
        return NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_menu_mylocation)
            .setContentTitle("PIM 持续定位")
            .setContentText(collapsedText(state))
            .setStyle(NotificationCompat.BigTextStyle().bigText(expandedText(state)))
            .setOngoing(isOngoing)
            .setOnlyAlertOnce(true)
            .setContentIntent(openStatusPendingIntent(context))
            .addAction(0, control.label, receiverPendingIntent(context, control.action, 10))
            .addAction(0, "同步", receiverPendingIntent(context, ForegroundLocationController.ACTION_SYNC_NOW, 11))
            .addAction(0, "状态", receiverPendingIntent(context, ForegroundLocationController.ACTION_OPEN_STATUS, 12))
            .build()
    }

    fun modeLabel(mode: LocationPolicyMode): String = when (mode) {
        LocationPolicyMode.Off -> "已暂停"
        LocationPolicyMode.PowerSavingNormal -> "省电档"
        LocationPolicyMode.ScheduleLowFrequency -> "日程低频"
        LocationPolicyMode.MotionObservation -> "运动观察"
        LocationPolicyMode.MovementRecovery -> "移动恢复"
        LocationPolicyMode.SyncFallback -> "同步兜底"
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
