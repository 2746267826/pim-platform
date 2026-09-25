package com.pim.app.keepalive

import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import com.pim.app.mobile.logs.StructuredLogRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/** 精确闹钟权限的当前状态（REQ-14 / REQ-23 可检测项）。 */
enum class ExactAlarmPermissionState {
    /** 已授权。 */
    GRANTED,

    /** 未授权，需要用户去「闹钟和提醒」打开。 */
    DENIED,

    /** 系统不提供该能力（理论上不会出现；保留以对不可检测项降级）。 */
    NOT_APPLICABLE
}

/**
 * 精确闹钟权限检测与系统设置页跳转（REQ-14 / REQ-23）。
 *
 * REQ-14 明确「运行时以系统查询结果为准」——因此这里只读
 * `AlarmManager.canScheduleExactAlarms()`（API 31+），**不**根据清单声明推断授权状态。
 *
 * 跳转目标是系统「闹钟和提醒」特殊权限页（`ACTION_REQUEST_SCHEDULE_EXACT_ALARM`）。
 * 无法跳转时返回 false，由界面回退到文字路径（AC-23.4）。
 */
@Singleton
class ExactAlarmPermissionChecker @Inject constructor(
    @ApplicationContext private val context: Context,
    private val scheduler: KeepAliveSchedulePort,
    private val logs: StructuredLogRepository
) {
    fun state(): ExactAlarmPermissionState = when {
        !isSupported() -> ExactAlarmPermissionState.NOT_APPLICABLE
        scheduler.hasExactAlarmPermission() -> ExactAlarmPermissionState.GRANTED
        else -> ExactAlarmPermissionState.DENIED
    }

    /** 本设备是否具备该权限机制（API 31+）。 */
    fun isSupported(): Boolean = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S

    /**
     * 构造「闹钟和提醒」设置页的跳转 Intent。
     *
     * 返回 null 表示本设备没有该页面（API 31 以下），调用方应回退到文字路径（AC-23.4）。
     */
    fun settingsIntent(): Intent? {
        if (!isSupported()) return null
        return Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
    }

    /**
     * 尝试跳转；返回是否真的启动了某个 Activity。
     * 失败时**不抛出**（AC-23.4：跳转失败时页面不崩溃，回退后显示文字路径）。
     */
    suspend fun openSettings(): Boolean {
        val intent = settingsIntent() ?: return false
        return try {
            context.startActivity(intent)
            true
        } catch (ex: Exception) {
            logs.warn("keepalive", "打开「闹钟和提醒」设置页失败：${ex.message ?: ""}")
            false
        }
    }

    /** 跳转失败时展示的文字路径（AC-23.4）。 */
    fun manualPathText(): String =
        "请手动前往：设置 → 应用管理 → PIM → 特殊权限（或「其他权限」）→「闹钟和提醒」→ 允许。"
}
