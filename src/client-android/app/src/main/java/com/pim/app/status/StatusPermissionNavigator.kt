package com.pim.app.status

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings

object StatusPermissionNavigator {
    fun open(context: Context, issue: StatusIssue) =
        tryStartActivity(context, intentFor(context, issue))

    fun open(context: Context, issueCode: String) =
        tryStartActivity(context, intentFor(context, issueCode))

    private fun tryStartActivity(context: Context, intent: Intent) {
        if (tryOpen(context, intent)) return

        val fallback = appDetailsIntent(context)
        if (intent.action != fallback.action || intent.data != fallback.data) {
            tryOpen(context, fallback)
        }
    }

    private fun tryOpen(context: Context, intent: Intent): Boolean = try {
        context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        true
    } catch (_: ActivityNotFoundException) {
        false
    } catch (_: SecurityException) {
        false
    }

    fun intentFor(context: Context, issue: StatusIssue): Intent = intentFor(context, issue.code)

    fun intentFor(context: Context, issueCode: String): Intent = when (issueCode) {
        "usage-access-missing" -> Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)
        "notification-permission-missing" -> notificationSettingsIntent(context)
        "battery-optimization-missing" -> Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS)
            .setData(Uri.parse("package:${context.packageName}"))
        "foreground-location-missing",
        "background-location-missing",
        "activity-recognition-missing" -> appDetailsIntent(context)
        else -> appDetailsIntent(context)
    }

    private fun notificationSettingsIntent(context: Context): Intent {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
        } else {
            appDetailsIntent(context)
        }
    }

    private fun appDetailsIntent(context: Context): Intent {
        return Intent(
            Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
            Uri.fromParts("package", context.packageName, null)
        )
    }
}
