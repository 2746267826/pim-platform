package com.pim.app.keepalive

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * 保活能力的接口绑定（REQ-16 ~ REQ-22）。
 *
 * [AlarmFulfillmentRecorder] 与 [KeepAliveSettingsAccessor] 是叫醒执行链的两个依赖面，
 * 绑成接口是为了让「暂停不拉起」「手动会话不打断」这类分支能在 JVM 上断言，
 * 而不是只能靠真机观察（真机结论另见 PR 的模拟器取证记录）。
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class KeepAliveModuleBinds {

    @Binds
    @Singleton
    abstract fun bindAlarmFulfillmentRecorder(impl: KeepAliveLedger): AlarmFulfillmentRecorder

    @Binds
    @Singleton
    abstract fun bindKeepAliveSettingsAccessor(impl: KeepAliveSettingsStore): KeepAliveSettingsAccessor

    @Binds
    @Singleton
    abstract fun bindWakeEnvironment(impl: AndroidWakeEnvironment): WakeEnvironment

    @Binds
    @Singleton
    abstract fun bindKeepAliveSchedulePort(impl: KeepAliveAlarmScheduler): KeepAliveSchedulePort

    @Binds
    @Singleton
    abstract fun bindKeepAliveNotificationPort(
        impl: AndroidKeepAliveNotifications
    ): KeepAliveNotificationPort
}
