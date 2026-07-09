package com.pim.app.location.motion

import android.Manifest
import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import com.google.android.gms.location.ActivityRecognition
import com.google.android.gms.location.ActivityTransition
import com.google.android.gms.location.ActivityTransitionRequest
import com.google.android.gms.location.ActivityTransitionResult
import com.google.android.gms.location.DetectedActivity
import com.pim.app.location.policy.MotionSignal
import dagger.hilt.android.AndroidEntryPoint
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

object MotionTransitionPlanner {
    private val activityTypes = listOf(
        DetectedActivity.STILL,
        DetectedActivity.WALKING,
        DetectedActivity.RUNNING,
        DetectedActivity.ON_BICYCLE,
        DetectedActivity.IN_VEHICLE
    )

    fun transitions(): List<ActivityTransition> = activityTypes.flatMap { activityType ->
        listOf(
            transition(activityType, ActivityTransition.ACTIVITY_TRANSITION_ENTER),
            transition(activityType, ActivityTransition.ACTIVITY_TRANSITION_EXIT)
        )
    }

    fun request(): ActivityTransitionRequest = ActivityTransitionRequest(transitions())

    private fun transition(activityType: Int, transitionType: Int): ActivityTransition =
        ActivityTransition.Builder()
            .setActivityType(activityType)
            .setActivityTransition(transitionType)
            .build()
}

data class MotionSignalStatus(
    val signal: MotionSignal,
    val issueCode: String?,
    val message: String?
) {
    companion object {
        const val ACTIVITY_RECOGNITION_PERMISSION_MESSAGE = "缺少活动识别权限"

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

    fun updateFromActivityTransition(activityType: Int, transitionType: Int) {
        _status.value = if (transitionType == ActivityTransition.ACTIVITY_TRANSITION_ENTER) {
            MotionSignalStatus(
                signal = MotionSignalMapper.fromDetectedActivity(activityType),
                issueCode = null,
                message = null
            )
        } else {
            MotionSignalStatus(MotionSignal.Unknown, issueCode = null, message = null)
        }
    }

    @SuppressLint("MissingPermission")
    fun registerActivityTransitions() {
        if (!hasActivityRecognitionPermission()) {
            _status.value = MotionSignalStatus.unavailable(
                MotionSignalStatus.ACTIVITY_RECOGNITION_PERMISSION_MESSAGE
            )
            return
        }
        ActivityRecognition.getClient(context).requestActivityTransitionUpdates(
            MotionTransitionPlanner.request(),
            MotionTransitionReceiver.pendingIntent(context)
        )
    }

    fun unregisterActivityTransitions() {
        ActivityRecognition.getClient(context).removeActivityTransitionUpdates(
            MotionTransitionReceiver.pendingIntent(context)
        )
    }

    private fun initialStatus(): MotionSignalStatus {
        return if (hasActivityRecognitionPermission()) {
            MotionSignalStatus(MotionSignal.Unknown, issueCode = null, message = null)
        } else {
            MotionSignalStatus.unavailable(MotionSignalStatus.ACTIVITY_RECOGNITION_PERMISSION_MESSAGE)
        }
    }

    private fun hasActivityRecognitionPermission(): Boolean {
        val permission = ContextCompat.checkSelfPermission(context, Manifest.permission.ACTIVITY_RECOGNITION)
        return permission == PackageManager.PERMISSION_GRANTED
    }
}

@AndroidEntryPoint
class MotionTransitionReceiver : BroadcastReceiver() {
    @Inject lateinit var motionSignalRepository: MotionSignalRepository

    override fun onReceive(context: Context, intent: Intent) {
        if (!ActivityTransitionResult.hasResult(intent)) return
        val result = ActivityTransitionResult.extractResult(intent) ?: return
        result.transitionEvents.forEach { event ->
            motionSignalRepository.updateFromActivityTransition(event.activityType, event.transitionType)
        }
    }

    companion object {
        const val ACTION = "com.pim.app.location.motion.ACTIVITY_TRANSITION"
        private const val REQUEST_CODE = 3801

        fun pendingIntent(context: Context): PendingIntent {
            val intent = Intent(context, MotionTransitionReceiver::class.java).setAction(ACTION)
            return PendingIntent.getBroadcast(
                context,
                REQUEST_CODE,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        }
    }
}
