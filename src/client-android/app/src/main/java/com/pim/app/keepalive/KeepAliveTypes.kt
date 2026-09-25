package com.pim.app.keepalive

/**
 * 保活闹钟的领域类型与契约常量（WO-ANDROID-KEEPALIVE-20260923 阶段二，REQ-14 ~ REQ-25）。
 *
 * 本文件**不依赖任何 Android 类**，因此闹钟的判定逻辑（压制、降频、兑现）可以脱离设备直接单测——
 * 这些判定是需求方确认过的参数（工单 §9.1 P2），必须有可复算的断言守着。
 */

/** 叫醒结果（工单 §7.1「闹钟兑现」行的「结果」列）。 */
object AlarmOutcomes {
    /** 按时执行：闹钟触发且执行链跑通。 */
    const val EXECUTED = "executed"

    /** 被压制：触发时刻比预定时刻晚超过阈值（AC-17.1）。 */
    const val SUPPRESSED = "suppressed"

    /** 拉起失败：闹钟触发但前台采集服务未能拉起（AC-17.5：**不**触发降频）。 */
    const val PULL_FAILED = "pull-failed"

    /** 因用户暂停未执行（AC-20.1 / AC-20.3：不计为失败、被杀、被压制）。 */
    const val SKIPPED_PAUSED = "skipped-paused"

    /** 因手动采集会话进行中跳过（AC-20.2）。 */
    const val SKIPPED_MANUAL_SESSION = "skipped-manual-session"

    /** 因保活总开关关闭未执行（AC-22.2）。 */
    const val SKIPPED_DISABLED = "skipped-disabled"

    /** 未执行：设备关机等导致没有实际触发时刻（AC-18.3：不计入兑现率分母）。 */
    const val NOT_EXECUTED = "not-executed"

    val ALL = setOf(
        EXECUTED, SUPPRESSED, PULL_FAILED,
        SKIPPED_PAUSED, SKIPPED_MANUAL_SESSION, SKIPPED_DISABLED, NOT_EXECUTED
    )

    fun label(outcome: String): String = when (outcome) {
        EXECUTED -> "按时执行"
        SUPPRESSED -> "被压制（延迟过久）"
        PULL_FAILED -> "拉起采集服务失败"
        SKIPPED_PAUSED -> "因暂停未执行"
        SKIPPED_MANUAL_SESSION -> "手动采集会话进行中，已跳过"
        SKIPPED_DISABLED -> "保活已关闭"
        NOT_EXECUTED -> "未执行（设备关机或未触发）"
        else -> "未知结果"
    }
}

/** 健康红点的四类原因（REQ-21：闹钟被清空 / 权限被撤销 / 检测到强停 / 连续叫醒调用失败）。 */
object KeepAliveHealthReasons {
    /** 闹钟被系统清空（例如被强行停止、权限被撤销连带取消）。 */
    const val ALARM_CLEARED = "alarm-cleared"

    /** 精确闹钟权限被撤销（REQ-14 AC-14.3）。 */
    const val PERMISSION_REVOKED = "permission-revoked"

    /** 检测到强停（沿用阶段一的强停判定）。 */
    const val FORCE_STOPPED = "force-stopped"

    /** 连续叫醒调用失败（AC-17.5 / REQ-21）。 */
    const val WAKE_CALL_FAILED = "wake-call-failed"

    val ALL = setOf(ALARM_CLEARED, PERMISSION_REVOKED, FORCE_STOPPED, WAKE_CALL_FAILED)

    fun label(reason: String): String = when (reason) {
        ALARM_CLEARED -> "系统清空了保活闹钟"
        PERMISSION_REVOKED -> "「闹钟和提醒」权限已被撤销"
        FORCE_STOPPED -> "检测到应用被强行停止"
        WAKE_CALL_FAILED -> "连续多次未能拉起采集服务"
        else -> "保活异常"
    }
}

/**
 * 一条叫醒兑现记录（REQ-18）。
 *
 * @param scheduledAtUtcMillis 预定时刻（登记闹钟时确定）
 * @param actualAtUtcMillis 实际时刻；**为 null 表示没有执行**（设备关机等，AC-18.3）
 * @param outcome 取值见 [AlarmOutcomes]
 */
data class AlarmFulfillmentRecord(
    val scheduledAtUtcMillis: Long,
    val actualAtUtcMillis: Long?,
    val outcome: String
) {
    /** 延迟值；没有实际时刻时为 null（AC-18.1：预定、实际、延迟三者算术一致）。 */
    val delayMillis: Long?
        get() = actualAtUtcMillis?.let { it - scheduledAtUtcMillis }

    /**
     * 是否构成「被压制」证据（AC-17.1）。
     *
     * 判定以 [outcome] 为准而不是就地比延迟：被压制必须同时满足「真的执行了」与「延迟超阈值」，
     * 这个结论由执行链在算出延迟后**一次判定**（见 `WakeExecutionChain`），
     * 避免「同一个事实两处各判一次、判出不同结果」。
     */
    val isSuppressedEvidence: Boolean
        get() = outcome == AlarmOutcomes.SUPPRESSED

    /** 是否构成「按时」证据：真的按时执行完链路（AC-17.3）。 */
    val isOnTimeEvidence: Boolean
        get() = outcome == AlarmOutcomes.EXECUTED
}
