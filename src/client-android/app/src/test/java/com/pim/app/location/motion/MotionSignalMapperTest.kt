package com.pim.app.location.motion

import com.google.android.gms.location.ActivityTransition
import com.google.android.gms.location.DetectedActivity
import com.pim.app.location.policy.MotionSignal
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MotionSignalMapperTest {
    @Test
    fun mapsDetectedActivitiesToPolicySignals() {
        assertEquals(MotionSignal.Still, MotionSignalMapper.fromDetectedActivity(DetectedActivity.STILL))
        assertEquals(MotionSignal.Walking, MotionSignalMapper.fromDetectedActivity(DetectedActivity.WALKING))
        assertEquals(MotionSignal.Running, MotionSignalMapper.fromDetectedActivity(DetectedActivity.RUNNING))
        assertEquals(MotionSignal.OnBicycle, MotionSignalMapper.fromDetectedActivity(DetectedActivity.ON_BICYCLE))
        assertEquals(MotionSignal.InVehicle, MotionSignalMapper.fromDetectedActivity(DetectedActivity.IN_VEHICLE))
        assertEquals(MotionSignal.Unknown, MotionSignalMapper.fromDetectedActivity(DetectedActivity.UNKNOWN))
    }

    @Test
    fun unavailableMotionKeepsPolicyAtUnknownWithStatusIssue() {
        val status = MotionSignalStatus.unavailable(MotionSignalStatus.ACTIVITY_RECOGNITION_PERMISSION_MESSAGE)

        assertEquals(MotionSignal.Unknown, status.signal)
        assertEquals("activity-recognition-unavailable", status.issueCode)
        assertEquals("缺少活动识别权限", status.message)
        assertTrue(status.message!!.all { it.code != 0xfffd })
    }

    @Test
    fun buildsEnterAndExitTransitionsForPolicyMotionSignals() {
        val transitions = MotionTransitionPlanner.transitions()
        val pairs = transitions.map { it.activityType to it.transitionType }.toSet()

        assertEquals(10, transitions.size)
        listOf(
            DetectedActivity.STILL,
            DetectedActivity.WALKING,
            DetectedActivity.RUNNING,
            DetectedActivity.ON_BICYCLE,
            DetectedActivity.IN_VEHICLE
        ).forEach { activityType ->
            assertTrue(activityType to ActivityTransition.ACTIVITY_TRANSITION_ENTER in pairs)
            assertTrue(activityType to ActivityTransition.ACTIVITY_TRANSITION_EXIT in pairs)
        }
    }
}
