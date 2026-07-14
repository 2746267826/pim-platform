package com.pim.app.status

object StatusDisplayText {
    fun apiReason(code: String?): String = when (code) {
        null, "" -> "暂无"
        "missing" -> "未配置"
        "invalid-api-url" -> "地址格式不正确"
        else -> "未知状态"
    }

    fun profile(value: String?): String = when (value) {
        "power-saving" -> "省电"
        "standard" -> "标准"
        "high-precision" -> "高精度"
        "custom" -> "自定义"
        null, "" -> "暂无"
        else -> "未知状态"
    }

    fun droppedReason(value: String?): String = when (value) {
        "missing-horizontal-accuracy" -> "缺少水平精度"
        "horizontal-accuracy-too-low" -> "定位精度不达标"
        "altitude-missing-timeout" -> "等待高度超时"
        null, "" -> "暂无"
        else -> "其他原因"
    }

    fun policyMode(value: String?): String = when (value) {
        "Off" -> "已停止"
        "PowerSavingNormal" -> "常规省电"
        "ScheduleLowFrequency" -> "日程低频"
        "MotionObservation" -> "运动观察"
        "MovementRecovery" -> "移动恢复"
        "SyncFallback" -> "同步兜底"
        null, "" -> "暂无"
        else -> "未知状态"
    }

    fun heartbeat(value: String?): String = when (value) {
        "心跳上报成功" -> "正常"
        "心跳上报失败" -> "最近上报异常"
        null, "" -> "暂无"
        else -> "未知状态"
    }
}
