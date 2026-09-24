package com.pim.app.forensics

import android.content.Context
import android.content.SharedPreferences
import androidx.work.WorkInfo
import androidx.work.WorkManager
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 哨兵状态的持久化（REQ-2）。
 *
 * 哨兵本身是既有的周期同步作业（强停会被系统清空闹钟与作业），这里只保存判定所需的
 * 持久化事实：是否已登记、登记时刻、最近一次确认存活的开机时长与时刻。
 */
@Singleton
class ForensicSentinelStore @Inject constructor(
    @ApplicationContext context: Context
) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun read(): SentinelState = SentinelState(
        armed = prefs.getBoolean(KEY_ARMED, false),
        armedAtUtcMillis = prefs.getLong(KEY_ARMED_AT, -1L).takeIf { it > 0 },
        lastAliveBootElapsedMillis = prefs.getLong(KEY_LAST_ALIVE_BOOT, -1L).takeIf { it >= 0 },
        lastAliveAtUtcMillis = prefs.getLong(KEY_LAST_ALIVE_AT, -1L).takeIf { it > 0 }
    )

    fun write(state: SentinelState) {
        prefs.edit()
            .putBoolean(KEY_ARMED, state.armed)
            .putLong(KEY_ARMED_AT, state.armedAtUtcMillis ?: -1L)
            .putLong(KEY_LAST_ALIVE_BOOT, state.lastAliveBootElapsedMillis ?: -1L)
            .putLong(KEY_LAST_ALIVE_AT, state.lastAliveAtUtcMillis ?: -1L)
            .commit()
    }

    /** 登记哨兵（周期同步作业入队成功后调用）。 */
    fun arm(nowUtcMillis: Long, bootElapsedMillis: Long) {
        val current = read()
        if (current.armed && current.lastAliveAtUtcMillis == nowUtcMillis) return
        write(
            current.copy(
                armed = true,
                armedAtUtcMillis = current.armedAtUtcMillis ?: nowUtcMillis,
                lastAliveBootElapsedMillis = bootElapsedMillis,
                lastAliveAtUtcMillis = nowUtcMillis
            )
        )
    }

    /** 记录"此刻仍然存活"（每次心跳/同步成功都刷新）。 */
    fun markAlive(nowUtcMillis: Long, bootElapsedMillis: Long) {
        val current = read()
        write(
            current.copy(
                lastAliveBootElapsedMillis = bootElapsedMillis,
                lastAliveAtUtcMillis = nowUtcMillis
            )
        )
    }

    companion object {
        const val PREFS_NAME = "pim_forensic_sentinel"
        private const val KEY_ARMED = "sentinel_armed"
        private const val KEY_ARMED_AT = "sentinel_armed_at_utc"
        private const val KEY_LAST_ALIVE_BOOT = "last_alive_boot_elapsed"
        private const val KEY_LAST_ALIVE_AT = "last_alive_at_utc"
    }
}

/** 哨兵是否仍然存在（供强停判定使用）。 */
interface SentinelProbe {
    suspend fun isSentinelPresent(): Boolean
}

/**
 * 通过 WorkManager 查询既有周期同步作业是否仍在队列里。
 * 用户强停会清空该应用的作业（平台依据：stopped 状态下闹钟与作业被清除），因此作业消失
 * 就是"哨兵被清空"；`am kill` 与低内存回收不会取消该作业，因此不会误判成强停。
 */
@Singleton
class WorkManagerSentinelProbe @Inject constructor(
    @ApplicationContext private val context: Context
) : SentinelProbe {

    override suspend fun isSentinelPresent(): Boolean {
        return try {
            val infos = WorkManager.getInstance(context)
                .getWorkInfosForUniqueWork(PERIODIC_WORK_NAME)
                .get()
            infos.any { it.state == WorkInfo.State.ENQUEUED || it.state == WorkInfo.State.RUNNING }
        } catch (_: Exception) {
            // 查不到就按"哨兵仍在"处理：宁可漏报强停，也不要把普通进程回收误报成用户强停。
            true
        }
    }

    companion object {
        /** 与 `MobileSyncScheduler.PERIODIC_NAME` 保持一致（不新增任何调度，AC-29.1）。 */
        const val PERIODIC_WORK_NAME = "pim_mobile_sync_periodic"
    }
}
