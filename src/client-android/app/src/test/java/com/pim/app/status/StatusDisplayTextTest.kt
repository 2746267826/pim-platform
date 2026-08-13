package com.pim.app.status

import org.junit.Assert.assertEquals
import org.junit.Test

class StatusDisplayTextTest {
    @Test
    fun apiReasonNullReturnsFallback() {
        assertEquals("暂无", StatusDisplayText.apiReason(null))
    }

    @Test
    fun apiReasonEmptyReturnsFallback() {
        assertEquals("暂无", StatusDisplayText.apiReason(""))
    }

    @Test
    fun apiReasonMissingReturnsConfiguredLabel() {
        assertEquals("未配置", StatusDisplayText.apiReason("missing"))
    }

    @Test
    fun apiReasonInvalidUrlReturnsFormatLabel() {
        assertEquals("地址格式不正确", StatusDisplayText.apiReason("invalid-api-url"))
    }

    @Test
    fun apiReasonUnknownDoesNotEchoRawCode() {
        assertEquals("未知状态", StatusDisplayText.apiReason("some-unknown-code"))
    }

    @Test
    fun profileNullReturnsFallback() {
        assertEquals("暂无", StatusDisplayText.profile(null))
    }

    @Test
    fun profileEmptyReturnsFallback() {
        assertEquals("暂无", StatusDisplayText.profile(""))
    }

    @Test
    fun profilePowerSavingReturnsChinese() {
        assertEquals("省电", StatusDisplayText.profile("power-saving"))
    }

    @Test
    fun profileStandardReturnsChinese() {
        assertEquals("标准", StatusDisplayText.profile("standard"))
    }

    @Test
    fun profileHighPrecisionReturnsChinese() {
        assertEquals("高精度", StatusDisplayText.profile("high-precision"))
    }

    @Test
    fun profileCustomReturnsChinese() {
        assertEquals("自定义", StatusDisplayText.profile("custom"))
    }

    @Test
    fun profileUnknownDoesNotEchoRawValue() {
        assertEquals("未知状态", StatusDisplayText.profile("some-unknown-profile"))
    }

    @Test
    fun droppedReasonNullReturnsFallback() {
        assertEquals("暂无", StatusDisplayText.droppedReason(null))
    }

    @Test
    fun droppedReasonEmptyReturnsFallback() {
        assertEquals("暂无", StatusDisplayText.droppedReason(""))
    }

    @Test
    fun droppedReasonMissingAccuracyReturnsChinese() {
        assertEquals("缺少水平精度", StatusDisplayText.droppedReason("missing-horizontal-accuracy"))
    }

    @Test
    fun droppedReasonAccuracyTooLowReturnsChinese() {
        assertEquals("定位精度不达标", StatusDisplayText.droppedReason("horizontal-accuracy-too-low"))
    }

    @Test
    fun droppedReasonAltitudeTimeoutReturnsChinese() {
        assertEquals("等待高度超时", StatusDisplayText.droppedReason("altitude-missing-timeout"))
    }

    @Test
    fun droppedReasonUnknownReturnsOtherLabel() {
        assertEquals("其他原因", StatusDisplayText.droppedReason("some-unknown-reason"))
    }

    @Test
    fun policyModeNullReturnsFallback() {
        assertEquals("暂无", StatusDisplayText.policyMode(null))
    }

    @Test
    fun policyModeEmptyReturnsFallback() {
        assertEquals("暂无", StatusDisplayText.policyMode(""))
    }

    @Test
    fun policyModeOffReturnsStopped() {
        assertEquals("已停止", StatusDisplayText.policyMode("Off"))
    }

    @Test
    fun policyModePowerSavingNormalReturnsChinese() {
        assertEquals("常规省电", StatusDisplayText.policyMode("PowerSavingNormal"))
    }

    @Test
    fun policyModeScheduleLowFrequencyReturnsChinese() {
        assertEquals("日程低频", StatusDisplayText.policyMode("ScheduleLowFrequency"))
    }

    @Test
    fun policyModeMotionObservationReturnsChinese() {
        assertEquals("运动观察", StatusDisplayText.policyMode("MotionObservation"))
    }

    @Test
    fun policyModeMovementRecoveryReturnsChinese() {
        assertEquals("移动恢复", StatusDisplayText.policyMode("MovementRecovery"))
    }

    @Test
    fun policyModeSyncFallbackReturnsChinese() {
        assertEquals("同步兜底", StatusDisplayText.policyMode("SyncFallback"))
    }

    @Test
    fun policyModeHighSpeedReturnsChinese() {
        assertEquals("高速轨迹", StatusDisplayText.policyMode("HighSpeed"))
    }

    @Test
    fun policyModeUnknownDoesNotEchoRawValue() {
        assertEquals("未知状态", StatusDisplayText.policyMode("SomeUnknownMode"))
    }

    @Test
    fun heartbeatNullReturnsFallback() {
        assertEquals("暂无", StatusDisplayText.heartbeat(null))
    }

    @Test
    fun heartbeatEmptyReturnsFallback() {
        assertEquals("暂无", StatusDisplayText.heartbeat(""))
    }

    @Test
    fun heartbeatSuccessReturnsNormal() {
        assertEquals("正常", StatusDisplayText.heartbeat("心跳上报成功"))
    }

    @Test
    fun heartbeatFailureReturnsAbnormal() {
        assertEquals("最近上报异常", StatusDisplayText.heartbeat("心跳上报失败"))
    }

    @Test
    fun heartbeatUnknownDoesNotEchoRawValue() {
        assertEquals("未知状态", StatusDisplayText.heartbeat("some-unknown-status"))
    }
}
