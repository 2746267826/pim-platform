package com.pim.app.forensics

/**
 * 取证事件类型（工单 WO-ANDROID-KEEPALIVE-20260923 §7.1）。
 * 取值与服务端 `Pim.Core.Liveness.ForensicEventTypes` 一一对应，属于两端契约。
 */
object ForensicEventTypes {
    /** 进程退出原因台账（REQ-1）。 */
    const val PROCESS_EXIT = "process-exit"

    /** 强停 / 设备重启（REQ-2）。 */
    const val FORCE_STOP = "force-stop"

    /** 存活心跳（REQ-3）。 */
    const val HEARTBEAT = "heartbeat"

    val ALL = setOf(PROCESS_EXIT, FORCE_STOP, HEARTBEAT)

    fun label(eventType: String): String = when (eventType) {
        PROCESS_EXIT -> "进程退出"
        FORCE_STOP -> "强停/重启"
        HEARTBEAT -> "存活心跳"
        else -> "未知事件"
    }
}

/** 强停 / 重启检测结果（REQ-2 AC-2.1 ~ AC-2.3）。 */
object ForceStopKinds {
    /** 哨兵缺失且开机时长未回退：疑似被强行停止。 */
    const val FORCE_STOP = "force-stop"

    /** 开机时长回退：设备重启。 */
    const val REBOOT = "reboot"

    /** 哨兵因权限变更被系统清空（AC-2.3：**不得**计为强停）。 */
    const val SENTINEL_CLEARED_PERMISSION = "sentinel-cleared-permission"
}

/**
 * 进程退出原因（Android `ApplicationExitInfo` 常量名，REQ-1）。
 * 这里的是**原始常量名**；中文死因标签由服务端统一翻译，保证 Web、MCP 摘要、体检三处措辞一致。
 */
object ProcessExitReasons {
    const val LOW_MEMORY = "REASON_LOW_MEMORY"
    const val SIGNALED = "REASON_SIGNALED"
    const val FREEZER = "REASON_FREEZER"
    const val USER_REQUESTED = "REASON_USER_REQUESTED"
    const val ANR = "REASON_ANR"
    const val CRASH = "REASON_CRASH"
    const val CRASH_NATIVE = "REASON_CRASH_NATIVE"
    const val EXCESSIVE_RESOURCE_USAGE = "REASON_EXCESSIVE_RESOURCE_USAGE"
    const val EXIT_SELF = "REASON_EXIT_SELF"
    const val DEPENDENCY_DIED = "REASON_DEPENDENCY_DIED"
    const val INITIALIZATION_FAILURE = "REASON_INITIALIZATION_FAILURE"
    const val PACKAGE_STATE_CHANGE = "REASON_PACKAGE_STATE_CHANGE"
    const val PACKAGE_UPDATED = "REASON_PACKAGE_UPDATED"
    const val PERMISSION_CHANGE = "REASON_PERMISSION_CHANGE"
    const val OTHER = "REASON_OTHER"
    const val UNKNOWN = "REASON_UNKNOWN"

    /** `REASON_USER_STOPPED`（API 34 起）：用户主动停止，语义与"用户强停"同类。 */
    const val USER_STOPPED = "REASON_USER_STOPPED"

    /** 系统未提供任何退出记录时使用的占位原因（AC-1.3：显示"未知"并给出可推断线索）。 */
    const val NO_RECORD = "NO_RECORD"

    /**
     * 把 Android 的 `ApplicationExitInfo.getReason()` 数值映射成常量名。
     * 取值来自 `android.app.ApplicationExitInfo`（API 30 起）的官方常量定义；
     * 未识别的数值一律落到 [UNKNOWN]，绝不猜成某个具体原因。
     */
    fun fromApiReason(apiReason: Int): String = when (apiReason) {
        0 -> UNKNOWN
        1 -> EXIT_SELF
        2 -> SIGNALED
        3 -> LOW_MEMORY
        4 -> CRASH
        5 -> CRASH_NATIVE
        6 -> ANR
        7 -> INITIALIZATION_FAILURE
        8 -> PERMISSION_CHANGE
        9 -> EXCESSIVE_RESOURCE_USAGE
        10 -> USER_REQUESTED
        11 -> USER_STOPPED
        12 -> DEPENDENCY_DIED
        13 -> OTHER
        14 -> FREEZER
        15 -> PACKAGE_STATE_CHANGE
        16 -> PACKAGE_UPDATED
        else -> UNKNOWN
    }
}
