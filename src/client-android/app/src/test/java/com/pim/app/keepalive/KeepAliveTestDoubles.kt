package com.pim.app.keepalive

/**
 * 保活测试共用的替身。
 *
 * 集中在这里而不是各测试文件各写一份：多个测试需要同一套「不会真的碰系统」的替身，
 * 分散实现会出现「A 测试的替身比 B 测试宽松」的漂移（阶段一假 Graph 服务的教训）。
 */
object KeepAliveTestDoubles {

    /** 一个可配置的调度端口替身，默认「有权限、不登记任何真实闹钟」。 */
    fun scheduler(
        permission: Boolean = true,
        scheduled: Boolean = false
    ): KeepAliveSchedulePort = object : KeepAliveSchedulePort {
        override suspend fun scheduleNext(
            intervalMinutes: Int,
            enabled: Boolean
        ): KeepAliveSchedulePort.Result = KeepAliveSchedulePort.Result.Scheduled(
            triggerAtUtcMillis = 0L,
            intervalMinutes = intervalMinutes
        )

        override suspend fun cancel() = Unit

        override fun hasExactAlarmPermission(): Boolean = permission
    }

    /** 通知端口替身（不碰通知栏）。 */
    fun notifications(): KeepAliveNotificationPort = object : KeepAliveNotificationPort {
        override fun isEnabled(): Boolean = true
        override suspend fun ensureResident() = Unit
        override suspend fun updateLastWake(atUtcMillis: Long?) = Unit
        override suspend fun dismissResident() = Unit
    }

    /** 设置访问替身（内存态）。 */
    fun settings(initial: KeepAliveSettings = KeepAliveSettings.defaults()): KeepAliveSettingsAccessor =
        object : KeepAliveSettingsAccessor {
            private var current = initial
            override fun read(): KeepAliveSettings = current
            override fun write(settings: KeepAliveSettings): KeepAliveSettings {
                current = settings
                return current
            }
        }
}
