package com.pim.app.forensics

import android.app.ActivityManager
import android.app.usage.UsageStatsManager
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import android.app.KeyguardManager
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.os.Build
import android.os.PowerManager
import android.os.SystemClock

/**
 * 取证上下文（REQ-4）：屏幕是否点亮、是否解锁、当时的前台应用、是否在充电、电量百分比。
 *
 * **读不到的字段留空并标注"不可用"，绝不填猜测值**（AC-4.2）。
 * 只记录包名与应用名，**不记录任何应用使用内容**（AC-4.2 反面）。
 */
interface ForensicContextSource {
    fun read(): ForensicContext
}

/** 一次上下文快照。null 表示该字段不可用；[unavailableFields] 逐项说明为什么不可用。 */
data class ForensicContext(
    val screenOn: Boolean? = null,
    val unlocked: Boolean? = null,
    val foregroundPackage: String? = null,
    val foregroundAppLabel: String? = null,
    val charging: Boolean? = null,
    val batteryPercent: Int? = null,
    val unavailableFields: List<String> = emptyList()
)

/**
 * 真实实现。所有读取都做了异常隔离：任何一项读不到只记进 [ForensicContext.unavailableFields]，
 * 不影响心搏/退出记录本身的写入。
 */
@Singleton
class AndroidForensicContextSource @Inject constructor(
    @ApplicationContext private val context: Context,
    private val foregroundApp: ForegroundAppSource
) : ForensicContextSource {

    override fun read(): ForensicContext {
        val unavailable = mutableListOf<String>()

        val screenOn = runCatching {
            val powerManager = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
            powerManager?.isInteractive
        }.getOrNull()
        if (screenOn == null) unavailable += "屏幕状态"

        val unlocked = runCatching {
            val keyguard = context.getSystemService(Context.KEYGUARD_SERVICE) as? KeyguardManager
            keyguard?.let { !it.isKeyguardLocked }
        }.getOrNull()
        if (unlocked == null) unavailable += "解锁状态"

        val battery = runCatching {
            val intent: Intent? = context.registerReceiver(
                null,
                IntentFilter(Intent.ACTION_BATTERY_CHANGED)
            )
            intent
        }.getOrNull()

        val charging = battery?.let { intent ->
            when (intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1)) {
                BatteryManager.BATTERY_STATUS_CHARGING,
                BatteryManager.BATTERY_STATUS_FULL -> true
                BatteryManager.BATTERY_STATUS_DISCHARGING,
                BatteryManager.BATTERY_STATUS_NOT_CHARGING -> false
                else -> null
            }
        }
        if (charging == null) unavailable += "充电状态"

        val batteryPercent = battery?.let { intent ->
            val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
            val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
            if (level >= 0 && scale > 0) level * 100 / scale else null
        }
        if (batteryPercent == null) unavailable += "电量百分比"

        val foreground = runCatching { foregroundApp.read() }.getOrNull()
        val packageName = foreground?.first
        val label = foreground?.second
        if (packageName == null) unavailable += "前台应用"

        return ForensicContext(
            screenOn = screenOn,
            unlocked = unlocked,
            foregroundPackage = packageName,
            foregroundAppLabel = label,
            charging = charging,
            batteryPercent = batteryPercent,
            unavailableFields = unavailable
        )
    }
}

/** 心跳发生时的一次设备状态快照（REQ-3）。 */
data class HeartbeatSnapshot(
    val bootElapsedMillis: Long,
    val sinceLastHeartbeatMillis: Long?,
    val standbyBucket: Int?,
    val standbyBucketLabel: String,
    val ignoringBatteryOptimizations: Boolean?,
    val dozeMode: Boolean?,
    val powerSaveMode: Boolean?,
    val foregroundServiceRunning: Boolean,
    val context: ForensicContext
)

/**
 * 采集心跳快照。所有系统查询都做异常隔离：读不到就留空（AC-4.2），不阻断心搏写入（AC-3.3）。
 */
@Singleton
class AndroidHeartbeatSnapshotReader @Inject constructor(
    @ApplicationContext private val context: Context
) {

    fun read(
        nowElapsedMillis: Long,
        lastHeartbeatElapsedMillis: Long?,
        foregroundServiceRunning: Boolean,
        forensicContext: ForensicContext
    ): HeartbeatSnapshot {
        val bucket = runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                val usageStatsManager =
                    context.getSystemService(Context.USAGE_STATS_SERVICE) as? UsageStatsManager
                usageStatsManager?.appStandbyBucket
            } else {
                null
            }
        }.getOrNull()

        val ignoringBatteryOptimizations = runCatching {
            val powerManager = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
            powerManager?.isIgnoringBatteryOptimizations(context.packageName)
        }.getOrNull()

        val dozeMode = runCatching {
            val powerManager = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
            powerManager?.isDeviceIdleMode
        }.getOrNull()

        val powerSaveMode = runCatching {
            val powerManager = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
            powerManager?.isPowerSaveMode
        }.getOrNull()

        return HeartbeatSnapshot(
            bootElapsedMillis = nowElapsedMillis,
            sinceLastHeartbeatMillis = lastHeartbeatElapsedMillis?.let { last ->
                (nowElapsedMillis - last).coerceAtLeast(0L)
            },
            standbyBucket = bucket,
            standbyBucketLabel = standbyBucketLabel(bucket),
            ignoringBatteryOptimizations = ignoringBatteryOptimizations,
            dozeMode = dozeMode,
            powerSaveMode = powerSaveMode,
            foregroundServiceRunning = foregroundServiceRunning,
            context = forensicContext
        )
    }

    /**
     * 待机桶中文标签（REQ-3：把"当前待机桶"记入心搏台账，用于解释"活着但工作被推迟"）。
     *
     * 桶号取自 `UsageStatsManager` 的公开取值；`RESTRICTED(45)` / `NEVER(50)` 目前是隐藏常量，
     * 这里用字面量并注明来源，避免为了两个标签引入反射。
     */
    fun standbyBucketLabel(bucket: Int?): String = when (bucket) {
        null -> "未知"
        STANDBY_BUCKET_ACTIVE -> "活跃"
        STANDBY_BUCKET_WORKING_SET -> "工作集"
        STANDBY_BUCKET_FREQUENT -> "常用"
        STANDBY_BUCKET_RARE -> "罕见"
        STANDBY_BUCKET_RESTRICTED -> "受限"
        STANDBY_BUCKET_NEVER -> "从未使用"
        else -> "未知"
    }

    companion object {
        /** `UsageStatsManager.STANDBY_BUCKET_ACTIVE`。 */
        const val STANDBY_BUCKET_ACTIVE = 10

        /** `UsageStatsManager.STANDBY_BUCKET_WORKING_SET`。 */
        const val STANDBY_BUCKET_WORKING_SET = 20

        /** `UsageStatsManager.STANDBY_BUCKET_FREQUENT`。 */
        const val STANDBY_BUCKET_FREQUENT = 30

        /** `UsageStatsManager.STANDBY_BUCKET_RARE`。 */
        const val STANDBY_BUCKET_RARE = 40

        /** `UsageStatsManager.STANDBY_BUCKET_RESTRICTED`（隐藏常量）。 */
        const val STANDBY_BUCKET_RESTRICTED = 45

        /** `UsageStatsManager.STANDBY_BUCKET_NEVER`（隐藏常量）。 */
        const val STANDBY_BUCKET_NEVER = 50
    }
}

/**
 * 当时的前台应用读取（REQ-4 / AC-4.2）。
 *
 * 只返回**包名与应用名**，绝不读取任何应用使用内容（开屏页面、标题、输入内容）。
 * 缺少"应用使用情况"权限时返回 null，由调用方标注"不可用"，不填猜测值。
 */
interface ForegroundAppSource {
    fun read(): Pair<String, String?>?
}

/** 基于 `UsageStatsManager` 的最近前台应用读取（需要已授予"应用使用情况"权限）。 */
@Singleton
class AndroidForegroundAppSource @Inject constructor(
    @ApplicationContext private val context: Context
) : ForegroundAppSource {

    override fun read(): Pair<String, String?>? {
        val usageStatsManager =
            context.getSystemService(Context.USAGE_STATS_SERVICE) as? UsageStatsManager
                ?: return null

        val now = System.currentTimeMillis()
        val events = usageStatsManager.queryEvents(now - LOOKBACK_MILLIS, now) ?: return null

        var latestPackage: String? = null
        var latestTimestamp = Long.MIN_VALUE
        val event = android.app.usage.UsageEvents.Event()
        while (events.hasNextEvent()) {
            events.getNextEvent(event)
            if (event.eventType != android.app.usage.UsageEvents.Event.ACTIVITY_RESUMED &&
                event.eventType != android.app.usage.UsageEvents.Event.MOVE_TO_FOREGROUND
            ) {
                continue
            }
            if (event.timeStamp > latestTimestamp) {
                latestTimestamp = event.timeStamp
                latestPackage = event.packageName
            }
        }

        val packageName = latestPackage?.takeIf { it.isNotBlank() } ?: return null
        val label = runCatching {
            val info = context.packageManager.getApplicationInfo(packageName, 0)
            context.packageManager.getApplicationLabel(info).toString()
        }.getOrNull()

        return packageName to label
    }

    companion object {
        /** 只回看最近 10 分钟：更早的前台应用不代表"当时"的前台应用。 */
        private const val LOOKBACK_MILLIS = 10L * 60L * 1000L
    }
}

/** 当前进程已开机多久（毫秒）。用于区分"设备重启"与"应用被杀"（REQ-2）。 */
object BootElapsedClock {
    fun now(): Long = SystemClock.elapsedRealtime()
}
