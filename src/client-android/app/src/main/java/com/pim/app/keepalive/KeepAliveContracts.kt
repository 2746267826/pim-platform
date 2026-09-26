package com.pim.app.keepalive

/**
 * 叫醒执行链依赖的两个窄接口。
 *
 * 为什么要抽接口：执行链的分支（REQ-16 顺序、REQ-20 暂停/手动会话、AC-16.3 兜底）
 * 是本次最容易写错、后果最直接的部分——写错就会在用户主动暂停后仍偷偷拉起服务。
 * 生产实现是 [KeepAliveLedger] 与 [KeepAliveSettingsStore]，测试用内存替身，
 * 这样这些分支可以在 JVM 上逐条断言，而不必依赖真机观察。
 */

/** 叫醒台账的写入能力（REQ-18）。 */
interface AlarmFulfillmentRecorder {
    suspend fun recordFulfillment(record: AlarmFulfillmentRecord): Boolean
}

/** 保活设置的读写能力（REQ-15 / REQ-17 / REQ-22）。 */
interface KeepAliveSettingsAccessor {
    fun read(): KeepAliveSettings
    fun write(settings: KeepAliveSettings): KeepAliveSettings
}
