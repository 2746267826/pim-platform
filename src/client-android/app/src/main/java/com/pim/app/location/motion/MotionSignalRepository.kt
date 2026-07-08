package com.pim.app.location.motion

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import com.google.android.gms.location.DetectedActivity
import com.pim.app.location.policy.MotionSignal
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object MotionSignalMapper {
    fun fromDetectedActivity(activityType: Int): MotionSignal = when (activityType) {
        DetectedActivity.STILL -> MotionSignal.Still
        DetectedActivity.WALKING -> MotionSignal.Walking
        DetectedActivity.RUNNING -> MotionSignal.Running
        DetectedActivity.ON_BICYCLE -> MotionSignal.OnBicycle
        DetectedActivity.IN_VEHICLE -> MotionSignal.InVehicle
        else -> MotionSignal.Unknown
    }
}

data class MotionSignalStatus(
    val signal: MotionSignal,
    val issueCode: String?,
    val message: String?
) {
    companion object {
        fun unavailable(message: String): MotionSignalStatus = MotionSignalStatus(
            signal = MotionSignal.Unknown,
            issueCode = "activity-recognition-unavailable",
            message = message
        )
    }
}

@Singleton
class MotionSignalRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val _status = MutableStateFlow(initialStatus())
    val status: StateFlow<MotionSignalStatus> = _status.asStateFlow()

    fun updateFromDetectedActivity(activityType: Int) {
        _status.value = MotionSignalStatus(
            signal = MotionSignalMapper.fromDetectedActivity(activityType),
            issueCode = null,
            message = null
        )
    }

    private fun initialStatus(): MotionSignalStatus {
        val permission = ContextCompat.checkSelfPermission(context, Manifest.permission.ACTIVITY_RECOGNITION)
        return if (permission == PackageManager.PERMISSION_GRANTED) {
            MotionSignalStatus(MotionSignal.Unknown, issueCode = null, message = null)
        } else {
            MotionSignalStatus.unavailable("缺少活动识别权限")
        }
    }
}
