package com.pim.app.permissions

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import com.pim.app.mobile.usage.UsageAccessChecker
import com.pim.app.status.PermissionStatusSnapshot
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PermissionStatusRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val usageAccessChecker: UsageAccessChecker
) {
    fun snapshot(): PermissionStatusSnapshot {
        val preciseLocationGranted = isGranted(Manifest.permission.ACCESS_FINE_LOCATION)
        return PermissionStatusSnapshot(
            notificationGranted = hasNotificationPermission(),
            preciseLocationGranted = preciseLocationGranted,
            backgroundLocationGranted = hasBackgroundLocationPermission(preciseLocationGranted),
            usageAccessGranted = usageAccessChecker.hasUsageAccess(),
            activityRecognitionGranted = hasActivityRecognitionPermission()
        )
    }

    private fun hasNotificationPermission(): Boolean {
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            isGranted(Manifest.permission.POST_NOTIFICATIONS)
    }

    private fun hasBackgroundLocationPermission(preciseLocationGranted: Boolean): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            isGranted(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
        } else {
            preciseLocationGranted
        }
    }

    private fun hasActivityRecognitionPermission(): Boolean {
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.Q ||
            isGranted(Manifest.permission.ACTIVITY_RECOGNITION)
    }

    private fun isGranted(permission: String): Boolean {
        return ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
    }
}
