package com.pim.app.location.liveupdate

import android.Manifest
import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import com.pim.app.MainActivity
import com.pim.app.notifications.NotificationActionReceiver

internal data class ParsedLiveUpdateUri(
    val sessionId: String,
    val action: String
)

object LocationLiveUpdateNotificationRenderer {
    const val CHANNEL_ID = "pim_location_live_update"
    const val LIVE_UPDATE_NOTIFICATION_ID = 7102
    const val CANCEL_REQUEST_CODE = 71020
    const val OPEN_REQUEST_CODE = 71021
    const val DELETE_REQUEST_CODE = 71022

    const val ACTION_CANCEL_LOCATION_SESSION = "com.pim.app.location.action.CANCEL_LOCATION_SESSION"
    const val ACTION_DISMISS_LOCATION_LIVE_UPDATE = "com.pim.app.location.action.DISMISS_LOCATION_LIVE_UPDATE"

    internal var capabilityOverride: (() -> Boolean)? = null
    internal var canShowNotificationsOverride: ((Context) -> Boolean)? = null
    internal var canPostPromotedOverride: (() -> Boolean)? = null

    fun tryBuildAndNotify(
        ctx: Context,
        content: LocationLiveUpdateContent,
        createChannel: (String, String) -> Unit = { id, name ->
            val manager = ctx.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val channel = NotificationChannel(id, name, NotificationManager.IMPORTANCE_LOW)
            manager.createNotificationChannel(channel)
        },
        notifyFn: (Int, Notification) -> Unit = { id, notification ->
            val manager = ctx.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.notify(id, notification)
        }
    ): Boolean {
        if (!(capabilityOverride?.invoke() ?: LocationLiveUpdateCapability.isAvailable())) return false
        if (!(canShowNotificationsOverride?.invoke(ctx) ?: hasPostNotificationsPermission(ctx))) return false
        if (!(canPostPromotedOverride?.invoke() ?: canPostPromoted(ctx))) return false

        val title = if (content.triggerType.name == "MANUAL") "手动定位" else "自动定位"
        val accuracy = content.accuracyMeters?.let { "%.0f".format(it) } ?: "?"
        val contentText = "阶段：采集中 · 耗时：${content.elapsedSeconds}s · 精度：${accuracy}m · 提供方：${content.providerLabel}"

        createChannel(CHANNEL_ID, "PIM 定位 Live Update")

        val notification = buildNotification(ctx, content, title, contentText)
        notifyFn(LIVE_UPDATE_NOTIFICATION_ID, notification)
        return true
    }

    internal fun parseSessionUri(uri: Uri?): ParsedLiveUpdateUri? {
        if (uri == null) return null
        if (uri.scheme != "pim") return null
        if (uri.authority != "location-live") return null
        val segments = uri.pathSegments
        if (segments.size != 2) return null
        val sessionId = segments[0]
        val action = segments[1]
        if (sessionId.isBlank()) return null
        if (action !in setOf("cancel", "delete")) return null
        return ParsedLiveUpdateUri(sessionId, action)
    }

    private fun hasPostNotificationsPermission(ctx: Context): Boolean {
        if (Build.VERSION.SDK_INT < 33) return true
        return ctx.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
    }

    private fun canPostPromoted(ctx: Context): Boolean {
        if (Build.VERSION.SDK_INT < 36) return false
        return try {
            val mgr = ctx.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            mgr.canPostPromotedNotifications()
        } catch (_: LinkageError) {
            false
        }
    }

    @SuppressLint("NewApi")
    private fun buildNotification(
        ctx: Context,
        content: LocationLiveUpdateContent,
        title: String,
        contentText: String
    ): Notification {
        val builder = Notification.Builder(ctx, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_menu_mylocation)
            .setContentTitle(title)
            .setContentText(collapsedText(content.elapsedSeconds, content.providerLabel))
            .setStyle(Notification.BigTextStyle().bigText(contentText))
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setVisibility(Notification.VISIBILITY_PUBLIC)
            .setContentIntent(openPendingIntent(ctx, content.sessionId))
            .setDeleteIntent(deletePendingIntent(ctx, content.sessionId))

        val cancelIntent = cancelPendingIntent(ctx, content.sessionId)
        builder.addAction(
            Notification.Action.Builder(
                null,
                "取消",
                cancelIntent
            ).build()
        )

        val openIntent = openPendingIntent(ctx, content.sessionId)
        builder.addAction(
            Notification.Action.Builder(
                null,
                "查看",
                openIntent
            ).build()
        )

        if (Build.VERSION.SDK_INT >= 36) {
            try {
                builder.setRequestPromotedOngoing(true)
                builder.setShortCriticalText("定位采集中")
            } catch (_: LinkageError) {
            }
        }

        return builder.build()
    }

    private fun collapsedText(elapsedSec: Long, provider: String): String {
        return "采集中 · ${elapsedSec}s · $provider"
    }

    private fun sessionUri(sessionId: String, action: String): Uri {
        return Uri.Builder()
            .scheme("pim")
            .authority("location-live")
            .path("/$sessionId/$action")
            .build()
    }

    internal fun cancelPendingIntent(ctx: Context, sessionId: String): PendingIntent {
        val intent = Intent(ctx, NotificationActionReceiver::class.java)
            .setAction(ACTION_CANCEL_LOCATION_SESSION)
            .setData(sessionUri(sessionId, "cancel"))
        return PendingIntent.getBroadcast(
            ctx,
            CANCEL_REQUEST_CODE,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    internal fun openPendingIntent(ctx: Context, sessionId: String): PendingIntent {
        val intent = Intent(ctx, MainActivity::class.java)
            .setData(sessionUri(sessionId, "open"))
            .putExtra("com.pim.app.location.extra.OPEN_DESTINATION", "location")
            .addFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_CLEAR_TOP or
                    Intent.FLAG_ACTIVITY_SINGLE_TOP
            )
        return PendingIntent.getActivity(
            ctx,
            OPEN_REQUEST_CODE,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    internal fun deletePendingIntent(ctx: Context, sessionId: String): PendingIntent {
        val intent = Intent(ctx, NotificationActionReceiver::class.java)
            .setAction(ACTION_DISMISS_LOCATION_LIVE_UPDATE)
            .setData(sessionUri(sessionId, "delete"))
        return PendingIntent.getBroadcast(
            ctx,
            DELETE_REQUEST_CODE,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }
}
