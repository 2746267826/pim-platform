package com.pim.app.location.motion

import com.google.android.gms.location.DetectedActivity
import com.pim.app.location.policy.MotionSignal
import org.junit.Assert.assertEquals
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
        val status = MotionSignalStatus.unavailable("缺少活动识别权限")

        assertEquals(MotionSignal.Unknown, status.signal)
        assertEquals("activity-recognition-unavailable", status.issueCode)
    }
}
