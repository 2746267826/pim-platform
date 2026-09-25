package com.pim.app.keepalive

/**
 * ColorOS 设置引导页的模型（REQ-23）。
 *
 * 工单把引导项分成两类，处理方式不同（依据 `references/android-平台依据.md` §6）：
 * - **可检测项**：电池优化、闹钟权限、待机桶 → 自动呈现当前状态（AC-23.2）；
 * - **不可检测项**：自启动、允许后台运行、应用速冻、睡眠待机优化 → 系统无稳定公开 API，
 *   只能提供跳转/文字路径 + 「我已完成」手动勾选，并记住状态（AC-23.3）。
 *
 * 本文件是纯模型（不依赖 Android），因此文案与分类可以被逐条断言。
 */

/** 引导项的检测能力。 */
enum class GuidanceDetectability {
    /** 可自动检测当前状态。 */
    DETECTABLE,

    /** 系统无稳定公开 API，只能手动确认。 */
    MANUAL
}

/** 一个引导项。 */
data class GuidanceItem(
    val key: String,
    val title: String,
    val why: String,
    val detectability: GuidanceDetectability,
    /** 手动项的路径文字（跳转失败时的回退，AC-23.4）。 */
    val manualPath: String
)

/**
 * 引导项清单。
 *
 * 文字全部为简体中文（AC-26.1），术语沿用系统设置里的名称（工单第 11 节术语说明）。
 */
object ColorOsGuidanceCatalog {

    /** 可检测：精确闹钟权限（REQ-14）。 */
    const val EXACT_ALARM = "exact-alarm"

    /** 可检测：电池优化。 */
    const val BATTERY_OPTIMIZATION = "battery-optimization"

    /** 可检测：待机桶。 */
    const val STANDBY_BUCKET = "standby-bucket"

    /** 不可检测：自启动。 */
    const val AUTO_START = "auto-start"

    /** 不可检测：允许后台运行。 */
    const val BACKGROUND_RUNNING = "background-running"

    /** 不可检测：应用速冻。 */
    const val APP_FREEZE = "app-freeze"

    /** 不可检测：睡眠待机优化。 */
    const val SLEEP_STANDBY = "sleep-standby"

    val ITEMS: List<GuidanceItem> = listOf(
        GuidanceItem(
            key = EXACT_ALARM,
            title = "闹钟和提醒",
            why = "没有这个权限，系统不会在预定的精确时刻把应用叫醒，保活闹钟无法登记。",
            detectability = GuidanceDetectability.DETECTABLE,
            manualPath = "设置 → 应用管理 → PIM → 特殊权限 →「闹钟和提醒」→ 允许"
        ),
        GuidanceItem(
            key = BATTERY_OPTIMIZATION,
            title = "电池优化",
            why = "未加入不优化名单时，系统会在息屏后限制后台活动，同步与采集被推迟。",
            detectability = GuidanceDetectability.DETECTABLE,
            manualPath = "设置 → 电池 → 更多设置 → 电池优化 → PIM → 不优化"
        ),
        GuidanceItem(
            key = STANDBY_BUCKET,
            title = "待机分区",
            why = "被划入「受限」分区时，作业与闹钟会被大幅推迟；活跃分区下限制最松。",
            detectability = GuidanceDetectability.DETECTABLE,
            manualPath = "设置 → 电池 → 更多设置 → 应用待机分区（部分机型无此入口）"
        ),
        GuidanceItem(
            key = AUTO_START,
            title = "自启动",
            why = "关闭自启动后，设备重启或应用被清理后无法自动恢复。",
            detectability = GuidanceDetectability.MANUAL,
            manualPath = "设置 → 应用管理 → PIM → 自启动 → 允许"
        ),
        GuidanceItem(
            key = BACKGROUND_RUNNING,
            title = "允许后台运行",
            why = "关闭后应用退到后台会被立即限制，采集随之中断。",
            detectability = GuidanceDetectability.MANUAL,
            manualPath = "设置 → 应用管理 → PIM → 耗电管理 → 允许后台运行"
        ),
        GuidanceItem(
            key = APP_FREEZE,
            title = "应用速冻",
            why = "被速冻的应用在后台完全不执行代码，任何保活手段都无效。",
            detectability = GuidanceDetectability.MANUAL,
            manualPath = "设置 → 电池 → 更多设置 → 应用速冻 → PIM → 关闭"
        ),
        GuidanceItem(
            key = SLEEP_STANDBY,
            title = "睡眠待机优化",
            why = "开启后夜间会深度限制后台；需要把 PIM 设为「不优化」。",
            detectability = GuidanceDetectability.MANUAL,
            manualPath = "设置 → 电池 → 更多设置 → 睡眠待机优化 → PIM → 不优化"
        )
    )

    fun item(key: String): GuidanceItem? = ITEMS.firstOrNull { it.key == key }

    /** 可检测项（自动呈现状态）。 */
    val detectable: List<GuidanceItem> get() = ITEMS.filter { it.detectability == GuidanceDetectability.DETECTABLE }

    /** 不可检测项（手动勾选）。 */
    val manual: List<GuidanceItem> get() = ITEMS.filter { it.detectability == GuidanceDetectability.MANUAL }
}

/** 引导项的当前状态（供界面渲染）。 */
data class GuidanceItemState(
    val item: GuidanceItem,
    /** 可检测项的自动读数；手动项为 null。 */
    val detectedOk: Boolean?,
    /** 手动项是否已被用户勾选「我已完成」。 */
    val manuallyCompleted: Boolean
) {
    /** 是否已就绪（可检测项按读数、手动项按勾选）。 */
    val satisfied: Boolean
        get() = detectedOk ?: manuallyCompleted

    /** 状态文案（AC-26.1 简体中文）。 */
    fun statusText(): String = when {
        detectedOk == true -> "已就绪"
        detectedOk == false -> "需要处理"
        manuallyCompleted -> "已标记完成"
        else -> "待确认"
    }
}
