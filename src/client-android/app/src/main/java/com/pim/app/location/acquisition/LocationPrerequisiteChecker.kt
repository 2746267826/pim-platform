package com.pim.app.location.acquisition

import android.content.Context
import android.location.LocationManager
import android.os.Build
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

sealed interface LocationPrerequisiteResult {
    data object Ready : LocationPrerequisiteResult
    data class Blocked(val reason: String) : LocationPrerequisiteResult
}

interface LocationPrerequisiteChecker {
    fun check(triggerType: TriggerType): LocationPrerequisiteResult
}

@Singleton
class AndroidLocationPrerequisiteChecker @Inject constructor(
    @ApplicationContext private val context: Context
) : LocationPrerequisiteChecker {

    override fun check(triggerType: TriggerType): LocationPrerequisiteResult {
        val fineLocation = android.Manifest.permission.ACCESS_FINE_LOCATION
        val fineGranted = android.Manifest.permission.ACCESS_FINE_LOCATION.let { perm ->
            android.content.pm.PackageManager.PERMISSION_GRANTED ==
                androidx.core.content.ContextCompat.checkSelfPermission(context, perm)
        }

        if (!fineGranted) {
            return LocationPrerequisiteResult.Blocked("缺少精确定位权限")
        }

        if (triggerType == TriggerType.AUTOMATIC && Build.VERSION.SDK_INT >= 29) {
            val backgroundLocation = android.Manifest.permission.ACCESS_BACKGROUND_LOCATION
            val backgroundGranted = android.content.pm.PackageManager.PERMISSION_GRANTED ==
                androidx.core.content.ContextCompat.checkSelfPermission(context, backgroundLocation)
            if (!backgroundGranted) {
                return LocationPrerequisiteResult.Blocked("缺少后台定位权限")
            }
        }

        val lm = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
        val locationEnabled = if (lm != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                lm.isLocationEnabled
            } else {
                @Suppress("DEPRECATION")
                lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER) ||
                    lm.isProviderEnabled(LocationManager.GPS_PROVIDER)
            }
        } else false

        if (!locationEnabled) {
            return LocationPrerequisiteResult.Blocked("系统定位服务未开启")
        }

        val playServicesAvailable = GoogleApiAvailability.getInstance()
            .isGooglePlayServicesAvailable(context) == ConnectionResult.SUCCESS
        if (!playServicesAvailable) {
            return LocationPrerequisiteResult.Blocked("Google Play Services 不可用")
        }

        return LocationPrerequisiteResult.Ready
    }
}
