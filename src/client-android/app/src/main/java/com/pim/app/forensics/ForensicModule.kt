package com.pim.app.forensics

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * 取证能力的接口绑定。
 *
 * 三个接口都是"可替换的外部世界读数"（系统退出记录、哨兵探针、设备上下文），
 * 绑成接口是为了让判定与编排逻辑能在 JVM 单元测试里用注入数据复现
 * （真机结论另见 PR 中的模拟器取证记录）。
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class ForensicModuleBinds {

    @Binds
    @Singleton
    abstract fun bindExitReasonSource(impl: AndroidExitReasonReader): ExitReasonSource

    @Binds
    @Singleton
    abstract fun bindSentinelProbe(impl: WorkManagerSentinelProbe): SentinelProbe

    @Binds
    @Singleton
    abstract fun bindForensicContextSource(impl: AndroidForensicContextSource): ForensicContextSource

    @Binds
    @Singleton
    abstract fun bindForegroundAppSource(impl: AndroidForegroundAppSource): ForegroundAppSource
}
