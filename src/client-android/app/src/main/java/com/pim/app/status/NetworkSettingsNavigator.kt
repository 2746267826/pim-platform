package com.pim.app.status

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings

object NetworkSettingsNavigator {
    fun open(context: Context) {
        val primary = intent()
        if (tryOpen(context, primary)) return
        if (primary.action != Settings.ACTION_WIRELESS_SETTINGS && tryOpen(context, wirelessSettingsIntent())) {
            return
        }
        tryOpen(context, Intent(Settings.ACTION_SETTINGS))
    }

    fun intent(): Intent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        Intent(Settings.Panel.ACTION_INTERNET_CONNECTIVITY)
    } else {
        wirelessSettingsIntent()
    }

    private fun wirelessSettingsIntent() = Intent(Settings.ACTION_WIRELESS_SETTINGS)

    private fun tryOpen(context: Context, intent: Intent): Boolean {
        return try {
            context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            true
        } catch (_: ActivityNotFoundException) {
            false
        } catch (_: SecurityException) {
            false
        }
    }
}
