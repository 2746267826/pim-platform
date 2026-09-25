package com.pim.app.keepalive

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.pim.app.MainActivity
import com.pim.app.notifications.LocationNotificationRenderer
import com.pim.app.mobile.logs.StructuredLogRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 叫醒常驻通知（REQ-19）。
 *
 * 要求逐条对应：
 * - **常驻、低优先级、静默**（不响不震）：用 `IMPORTANCE_MIN` 渠道 + 无声音/无震动；
 * - **内容随叫醒滚动更新**，但仍**只有一条**（AC-19.1）：固定 notificationId，
 *   每次 `notify` 同一个 id 即原地更新，不会新增条目；
 * - **与采集服务的常驻通知并存、互不覆盖**（AC-19.2）：渠道 id 与 notificationId
 *   都与 `LocationNotificationRenderer`（7101）不同，因此通知栏是两条。
 */
@Singleton
class AndroidKeepAliveNotifications @Inject constructor(
    @ApplicationContext private val context: Context,
    private val logs: StructuredLogRepository
) : KeepAliveNotificationPort {

    private var lastWakeAtUtcMillis: Long? = null

    override fun isEnabled(): Boolean = try {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(context, android.Manifest.permission.POST_NOTIFICATIONS) ==
                PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    } catch (_: Exception) {
        false
    }

    override suspend fun ensureResident() {
        if (!isEnabled()) {
            logs.warn("keepalive", "无法显示保活通知：未获得通知权限")
            return
        }
        try {
            ensureChannel()
            notificationManager()?.notify(NOTIFICATION_ID, buildNotification())
        } catch (ex: Exception) {
            logs.warn("keepalive", "显示保活通知失败：${ex.message ?: ""}")
        }
    }

    override suspend fun updateLastWake(atUtcMillis: Long?) {
        lastWakeAtUtcMillis = atUtcMillis
        ensureResident()
    }

    override suspend fun dismissResident() {
        try {
            notificationManager()?.cancel(NOTIFICATION_ID)
        } catch (ex: Exception) {
            logs.warn("keepalive", "移除保活通知失败：${ex.message ?: ""}")
        }
    }

    private fun notificationManager(): NotificationManager? =
        context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager

    private fun ensureChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = notificationManager() ?: return
        if (manager.getNotificationChannel(CHANNEL_ID) != null) return

        val channel = NotificationChannel(
            CHANNEL_ID,
            "保活状态",
            // IMPORTANCE_MIN：不响不震、不进状态栏高优先级区（工单要求「低优先级静默通知」）。
            NotificationManager.IMPORTANCE_MIN
        ).apply {
            description = "显示保活闹钟的最近一次叫醒时间，用于确认应用仍在被系统唤醒。"
            setShowBadge(false)
            enableVibration(false)
            setSound(null, null)
        }
        manager.createNotificationChannel(channel)
    }

    private fun buildNotification(): Notification {
        val openApp = PendingIntent.getActivity(
            context,
            REQUEST_CODE_OPEN,
            Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val content = lastWakeAtUtcMillis?.let { "最近一次叫醒：${formatTime(it)}" }
            ?: "保活已开启，等待第一次叫醒。"

        return NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_menu_recent_history)
            .setContentTitle("PIM 保活")
            .setContentText(content)
            // 静默：不响、不震、不闪烁（REQ-19 明确要求）。
            .setSilent(true)
            .setPriority(NotificationCompat.PRIORITY_MIN)
            .setOngoing(true)
            .setShowWhen(false)
            .setContentIntent(openApp)
            .build()
    }

    companion object {
        /** 与采集通知（[LocationNotificationRenderer.CHANNEL_ID] / NOTIFICATION_ID）都不同，保证两条并存。 */
        const val CHANNEL_ID = "pim_keepalive"
        const val NOTIFICATION_ID = 7201
        const val REQUEST_CODE_OPEN = 7202

        /** 供测试与 AC-19.2 断言使用：与采集通知必须是不同 id。 */
        fun conflictsWithLocationNotification(): Boolean =
            NOTIFICATION_ID == LocationNotificationRenderer.NOTIFICATION_ID ||
                CHANNEL_ID == LocationNotificationRenderer.CHANNEL_ID

        internal fun formatTime(atUtcMillis: Long): String {
            val formatter = java.text.SimpleDateFormat("HH:mm", java.util.Locale.CHINA)
            formatter.timeZone = java.util.TimeZone.getDefault()
            return formatter.format(java.util.Date(atUtcMillis))
        }
    }
}
