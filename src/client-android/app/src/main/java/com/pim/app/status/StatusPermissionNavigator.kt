package com.pim.app.status

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings

object StatusPermissionNavigator {
    fun open(context: Context, issue: StatusIssue) {
        val intent = intentFor(context, issue)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        try {
            context.startActivity(intent)
        } catch (_: ActivityNotFoundException) {
            context.startActivity(appDetailsIntent(context).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        }
    }

    fun intentFor(context: Context, issue: StatusIssue): Intent = when (issue.code) {
        "usage-access-missing" -> Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)
        "notification-permission-missing" -> notificationSettingsIntent(context)
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
