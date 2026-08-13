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
import com.pim.app.notifications.LocationNotificationRenderer
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

    internal fun shortCriticalText(accuracyMeters: Float?): String =
        if (accuracyMeters != null) "±%.0fm".format(accuracyMeters) else "定位中"

    internal fun normalizeProviderLabel(provider: String): String =
        provider.uppercase()

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
        val normalizedProvider = normalizeProviderLabel(content.providerLabel)
        val contentText = "阶段：采集中 · 耗时：${content.elapsedSeconds}s · 精度：${accuracy}m · 提供方：$normalizedProvider"

        createChannel(CHANNEL_ID, "定位动态")

        val notification = buildNotification(ctx, content, title, contentText)
        notifyFn(LIVE_UPDATE_NOTIFICATION_ID, notification)
        return true
    }

    /**
     * 高速档 Live Update：文案与定位会话专用版不同，且没有"取消会话"动作
     * （高速档是长期状态，无会话可取消）。与 [tryBuildAndNotify] 共用 7102 单 ID，
     * 由 [LocationLiveUpdatePublisher] 按"高速档优先/覆盖会话"规则切换。
     */
    fun tryBuildAndNotifyHighSpeed(
        ctx: Context,
        content: HighSpeedLiveUpdateContent,
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

        createChannel(CHANNEL_ID, "定位动态")

        val title = "高速轨迹记录中"
        val contentText = "已记录 ${LocationNotificationRenderer.elapsedText(content.elapsedSeconds)} · 2.5s 密集采样"

        val builder = Notification.Builder(ctx, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_menu_mylocation)
            .setContentTitle(title)
            .setContentText(contentText)
            .setStyle(Notification.BigTextStyle().bigText(contentText))
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setVisibility(Notification.VISIBILITY_PUBLIC)
            .setContentIntent(openLocationPendingIntent(ctx))

        if (Build.VERSION.SDK_INT >= 36) {
            try {
                builder.setRequestPromotedOngoing(true)
            } catch (_: LinkageError) {
            }
        }

        notifyFn(LIVE_UPDATE_NOTIFICATION_ID, builder.build())
        return true
    }

    private fun openLocationPendingIntent(ctx: Context): PendingIntent {
        val intent = Intent(ctx, MainActivity::class.java)
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
        val collapsedProvider = normalizeProviderLabel(content.providerLabel)
        val builder = Notification.Builder(ctx, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_menu_mylocation)
            .setContentTitle(title)
            .setContentText(collapsedText(content.elapsedSeconds, collapsedProvider))
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
                builder.setShortCriticalText(shortCriticalText(content.accuracyMeters))
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
