package com.pim.app.location.highspeed

/** 高速轨迹已记录时长的统一文案（通知栏 7101 / Live Update 7102 / 应用内共用）。 */
fun highSpeedElapsedText(seconds: Long): String {
    if (seconds < 60L) return "${seconds} 秒"
    return "${seconds / 60} 分 ${seconds % 60} 秒"
}
