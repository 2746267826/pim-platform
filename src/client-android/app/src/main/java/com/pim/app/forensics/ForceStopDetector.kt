package com.pim.app.forensics

/**
 * 哨兵状态（REQ-2）。「哨兵」= 应用在工作状态时登记的一个会被系统在强停时清空的触发器
 * （阶段一复用既有的周期同步作业 [com.pim.app.mobile.sync.MobileSyncScheduler.PERIODIC_NAME]，
 * **不新增任何轮询或闹钟**，满足 AC-29.1）。
 *
 * 这里只保存判定所需的持久化事实，判定逻辑在 [ForceStopDetector.detect] 这个纯函数里，
 * 因此"强停 / 重启 / 权限变更"三种结论都能用注入数据在单元测试里复现（真机结论另附）。
 */
data class SentinelState(
    /** 应用上次确认处于工作状态时，哨兵是否已登记。 */
    val armed: Boolean = false,
    /** 哨兵最近一次登记的时刻（UTC 毫秒）。 */
    val armedAtUtcMillis: Long? = null,
    /** 最近一次"确认存活"时的开机时长（毫秒），用于识别设备重启。 */
    val lastAliveBootElapsedMillis: Long? = null,
    /** 最近一次"确认存活"的时刻（UTC 毫秒）。 */
    val lastAliveAtUtcMillis: Long? = null
)

/** 一次启动时可观察到的判定输入。 */
data class ForceStopDetectionInput(
    val nowUtcMillis: Long,
    val nowBootElapsedMillis: Long,
    /** 哨兵是否仍然存在（例如周期作业仍处于 ENQUEUED）。 */
    val sentinelPresent: Boolean,
    /** 是否存在晚于"最近一次存活"的进程退出记录（系统留下了死因 ⇒ 通常不是强停）。 */
    val exitRecordAfterLastAlive: Boolean,
    /**
     * 该退出记录是否就是"用户强停"（`REASON_USER_REQUESTED` / `REASON_USER_STOPPED`）。
     *
     * 实测（API 36 模拟器）：`am force-stop` 会留下一条 `REASON_USER_REQUESTED` 记录，
     * 用户从系统设置强停同理；因此这种情况**本身就是强停证据**，不能被
     * [exitRecordAfterLastAlive] 当成"系统已解释死因"而放过。
     */
    val exitRecordSaysUserRequested: Boolean,
    /** 是否存在晚于哨兵登记时刻的 `REASON_PERMISSION_CHANGE` 退出记录。 */
    val permissionChangeAfterArmed: Boolean
)

/** 判定结论。 */
enum class ForceStopVerdict {
    /** 没有可解释的异常（首装、正常运行、或被系统如实记录了死因）。 */
    None,

    /** 设备重启（AC-2.2）：开机时长回退，**不得**计为强停。 */
    Reboot,

    /** 疑似强停（AC-2.1）。 */
    ForceStop,

    /** 哨兵被清空且同期有权限变更（AC-2.3）：**不得**计为强停。 */
    SentinelClearedByPermissionChange
}

/**
 * 强停 / 重启判定（REQ-2）。纯函数，无 Android 依赖，因此可以用注入数据在 JVM 单元测试里逐条覆盖
 * AC-2.1 / AC-2.2 / AC-2.3。
 */
object ForceStopDetector {

    fun detect(state: SentinelState, input: ForceStopDetectionInput): ForceStopVerdict {
        // 首次运行（没有"上次存活"基线）：无从判断，不给任何死因结论。
        val lastAliveBootElapsed = state.lastAliveBootElapsedMillis ?: return ForceStopVerdict.None

        // 开机时长回退 ⇒ 设备在此期间重启过（AC-2.2）。重启优先于强停判定，
        // 因为重启同样会清空闹钟与作业，若不先判重启就会把每次重启误报成强停。
        if (input.nowBootElapsedMillis < lastAliveBootElapsed) {
            return ForceStopVerdict.Reboot
        }

        // 哨兵被清空且同期有权限变更 ⇒ 记为"哨兵被清空（权限变更）"，不得计为强停（AC-2.3）。
        //
        // 两个条件缺一不可：REQ-2 的语义是"**因权限变更导致哨兵失效**"。
        // 若哨兵仍在（sentinelPresent == true），权限变更只是让进程被系统结束了一次，
        // 哨兵并没有被清空——这时把它记成"哨兵被清空（权限变更）"是凭空捏造的死因。
        // 这一条也必须排在"用户强停"之前：权限变更同样会让哨兵消失。
        if (!input.sentinelPresent && input.permissionChangeAfterArmed) {
            return ForceStopVerdict.SentinelClearedByPermissionChange
        }

        // 系统把这次停机记成"用户请求停止" ⇒ 就是强停（AC-2.1）。
        // 实测 Android 16（API 36）模拟器上 `am force-stop` 留下的正是 REASON_USER_REQUESTED。
        if (input.exitRecordSaysUserRequested) {
            return ForceStopVerdict.ForceStop
        }

        // 系统留下了其它退出记录 ⇒ 进程是被"有原因地"结束的（低内存/崩溃/被信号杀…），不是强停。
        if (input.exitRecordAfterLastAlive) {
            return ForceStopVerdict.None
        }

        // 哨兵消失、设备没重启、系统也没留下退出记录 ⇒ 疑似强停（AC-2.1）。
        if (!input.sentinelPresent && state.armed) {
            return ForceStopVerdict.ForceStop
        }

        return ForceStopVerdict.None
    }

    /** 结论对应的取证事件类型/负载 kind；[ForceStopVerdict.None] 不产生事件。 */
    fun kindOf(verdict: ForceStopVerdict): String? = when (verdict) {
        ForceStopVerdict.Reboot -> ForceStopKinds.REBOOT
        ForceStopVerdict.ForceStop -> ForceStopKinds.FORCE_STOP
        ForceStopVerdict.SentinelClearedByPermissionChange -> ForceStopKinds.SENTINEL_CLEARED_PERMISSION
        ForceStopVerdict.None -> null
    }

    /** 结论对应的展示文案（AC-26.1：简体中文）。 */
    fun labelOf(kind: String): String = when (kind) {
        ForceStopKinds.REBOOT -> "设备重启"
        ForceStopKinds.FORCE_STOP -> "疑似强停"
        ForceStopKinds.SENTINEL_CLEARED_PERMISSION -> "哨兵被清空（权限变更）"
        else -> "未知"
    }

    /**
     * 结论依据（写进负载，便于事后复核）。
     *
     * 需要 [input] 才能区分两条会得到同一个 [ForceStopVerdict.ForceStop] 的路径：
     * "系统记成用户请求停止"（最直接）与"哨兵消失且无任何退出记录"。
     * 写错依据会让事后核对得出相反的解释。
     */
    fun evidenceOf(verdict: ForceStopVerdict, input: ForceStopDetectionInput? = null): String =
        when (verdict) {
            ForceStopVerdict.Reboot -> "boot-elapsed-decreased"
            ForceStopVerdict.ForceStop ->
                if (input?.exitRecordSaysUserRequested == true) "user-requested-exit-record"
                else "sentinel-missing"
            ForceStopVerdict.SentinelClearedByPermissionChange -> "permission-change"
            ForceStopVerdict.None -> "none"
        }

    /** 与 [evidenceOf] 匹配的中文推断依据；与依据不一致的文案会把事后核对引向错误结论。 */
    fun inferenceOf(verdict: ForceStopVerdict, input: ForceStopDetectionInput? = null): String? =
        when (verdict) {
            ForceStopVerdict.ForceStop ->
                if (input?.exitRecordSaysUserRequested == true) {
                    "系统把这次停机记为「用户请求停止」（REASON_USER_REQUESTED / REASON_USER_STOPPED），" +
                        "与手动强行停止一致。"
                } else {
                    "哨兵（周期同步作业）已消失，且设备未重启、系统也没有留下该次进程退出的记录。"
                }
            ForceStopVerdict.SentinelClearedByPermissionChange ->
                "哨兵消失的同时存在权限变更记录，因此记为哨兵被清空，不计为强停。"
            ForceStopVerdict.Reboot -> "开机时长较上次存活时回退，判定为设备重启。"
            ForceStopVerdict.None -> null
        }
}
