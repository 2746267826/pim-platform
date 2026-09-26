package com.pim.app.keepalive

import android.app.usage.UsageStatsManager
import android.content.Context
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import com.pim.app.mobile.logs.StructuredLogRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 引导页「可检测项」的真实读数（REQ-23 AC-23.2：显示与真机实际设置一致）。
 *
 * 三项各有其权威数据源：
 * - 电池优化：`PowerManager.isIgnoringBatteryOptimizations(packageName)`；
 * - 闹钟权限：`AlarmManager.canScheduleExactAlarms()`（经 [ExactAlarmPermissionChecker]）；
 * - 待机桶：`UsageStatsManager.getAppStandbyBucket()`（API 28+），旧版本标为不可用。
 *
 * **读不到时返回 null 而不是猜一个值**：AC-23.2 要求「与真机实际设置一致」，
 * 猜值会让引导页撒谎；界面按 null 显示「无法读取」。
 */
@Singleton
class GuidanceDetector @Inject constructor(
    @ApplicationContext private val context: Context,
    private val exactAlarmChecker: ExactAlarmPermissionChecker,
    private val logs: StructuredLogRepository
) {
    /** 电池优化是否已排除本应用（true = 已在「不优化」名单里）。 */
    suspend fun ignoresBatteryOptimizations(): Boolean? = try {
        val power = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
        power?.isIgnoringBatteryOptimizations(context.packageName)
    } catch (ex: Exception) {
        logs.warn("keepalive", "读取电池优化状态失败：${ex.message ?: ""}")
        null
    }

    /** 精确闹钟权限是否已授予。 */
    suspend fun exactAlarmGranted(): Boolean? = try {
        when (exactAlarmChecker.state()) {
            ExactAlarmPermissionState.GRANTED -> true
            ExactAlarmPermissionState.DENIED -> false
            ExactAlarmPermissionState.NOT_APPLICABLE -> null
        }
    } catch (ex: Exception) {
        null
    }

    /** 当前待机桶；读不到返回 null。 */
    suspend fun standbyBucket(): String? = try {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) {
            null
        } else {
            val manager = context.getSystemService(Context.USAGE_STATS_SERVICE) as? UsageStatsManager
            manager?.appStandbyBucket?.let { bucketOf(it) }
        }
    } catch (ex: Exception) {
        logs.warn("keepalive", "读取待机桶失败：${ex.message ?: ""}")
        null
    }

    /** 待机桶是否为「活跃」档（活跃档限制最松）。读不到返回 null。 */
    suspend fun standbyBucketIsActive(): Boolean? = try {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) {
            null
        } else {
            val manager = context.getSystemService(Context.USAGE_STATS_SERVICE) as? UsageStatsManager
            manager?.appStandbyBucket?.let {
                it <= UsageStatsManager.STANDBY_BUCKET_FREQUENT
            }
        }
    } catch (ex: Exception) {
        null
    }

    /** 组装引导页的可检测项状态（AC-23.2）。 */
    suspend fun detectableStates(completedKeys: Set<String>): List<GuidanceItemState> =
        ColorOsGuidanceCatalog.detectable.map { item ->
            val detected = when (item.key) {
                ColorOsGuidanceCatalog.EXACT_ALARM -> exactAlarmGranted()
                ColorOsGuidanceCatalog.BATTERY_OPTIMIZATION -> ignoresBatteryOptimizations()
                ColorOsGuidanceCatalog.STANDBY_BUCKET -> standbyBucketIsActive()
                else -> null
            }
            GuidanceItemState(item = item, detectedOk = detected, manuallyCompleted = false)
        }

    /** 组装引导页的手动项状态（AC-23.3：勾选后记住）。 */
    fun manualStates(completedKeys: Set<String>): List<GuidanceItemState> =
        ColorOsGuidanceCatalog.manual.map { item ->
            GuidanceItemState(
                item = item,
                detectedOk = null,
                manuallyCompleted = completedKeys.contains(item.key)
            )
        }

    /** 待机桶读数文案（供设置页「当前状态」展示，AC-22.1）。 */
    suspend fun standbyBucketLabel(): String = standbyBucket() ?: "无法读取"

    companion object {
        /**
         * 把 `UsageStatsManager` 的桶编号转成可读中文（AC-26.1）。
         *
         * 用 `when(bucket)` 对公开常量做分支，未识别的编号落到「未知」而不是猜一个档位：
         * 引导页显示错档位会让需求方据此做出错误的系统设置判断。
         * （`STANDBY_BUCKET_EXEMPTED` 在部分 SDK 存根里缺失，故按数值 5 兼容处理，
         * 该值自 API 28 起稳定。）
         */
        fun bucketOf(bucket: Int): String = when (bucket) {
            STANDBY_BUCKET_EXEMPTED_VALUE -> "已豁免"
            UsageStatsManager.STANDBY_BUCKET_ACTIVE -> "活跃"
            UsageStatsManager.STANDBY_BUCKET_WORKING_SET -> "常用"
            UsageStatsManager.STANDBY_BUCKET_FREQUENT -> "经常使用"
            UsageStatsManager.STANDBY_BUCKET_RARE -> "很少使用"
            UsageStatsManager.STANDBY_BUCKET_RESTRICTED -> "受限（后台活动被大幅限制）"
            else -> "未知"
        }

        /** `UsageStatsManager.STANDBY_BUCKET_EXEMPTED` 的稳定数值（API 28 起）。 */
        private const val STANDBY_BUCKET_EXEMPTED_VALUE = 5
    }
}
