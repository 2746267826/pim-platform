package com.pim.app.mobile.sync

import android.content.Context
import android.content.SharedPreferences
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.pim.app.TestPimApp
import com.pim.app.data.AppDatabase
import com.pim.app.data.ForensicEventDao
import com.pim.app.location.TrajectoryCompressor
import com.pim.app.forensics.AndroidHeartbeatSnapshotReader
import com.pim.app.forensics.ForensicContext
import com.pim.app.forensics.ForensicContextSource
import com.pim.app.forensics.ForensicEventTypes
import com.pim.app.forensics.ForensicLedger
import com.pim.app.forensics.ForensicSentinelStore
import com.pim.app.forensics.ForensicUploadCoordinator
import com.pim.app.forensics.WakeHeartbeatRecorder
import com.pim.app.mobile.logs.StructuredLogRepository
import com.pim.app.mobile.usage.AppMetadataCollector
import com.pim.app.mobile.usage.UsageAccessChecker
import com.pim.app.mobile.usage.UsageEventCollector
import com.pim.app.settings.TrackingSettingsStore
import com.pim.core.auth.SecurePreferencesFactory
import com.pim.core.auth.TokenManager
import com.pim.core.models.ApiResponse
import com.pim.core.network.ApiService
import com.pim.core.settings.ServerSettingsStore
import java.lang.reflect.Proxy
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * 缺陷 #345 / REQ-3：**周期同步**这条唤醒路径也必须写一条心搏。
 *
 * 阶段一只在 `PimApp.onCreate()` 里调用了启动取证，因此真机上「一周只有 2 条心搏」——
 * 周期同步跑了 35 次/24h，却一条心搏都没记。这里断言的就是那次缺失的写入：
 * 跑一次真正的同步编排（`syncOnOpen()`），台账里就必须多出一条心搏。
 *
 * 这里走的是「服务器地址未配置」分支——它是真机日志里同样会出现的最短真实路径，
 * 且心搏发生在任何提前返回之前，因此正好证明"同步被跳过，但唤醒仍被记录"。
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = TestPimApp::class)
class MobileSyncPeriodicHeartbeatTest {

    private lateinit var context: Context
    private lateinit var db: AppDatabase

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
    }

    @After
    fun tearDown() {
        db.close()
    }

    /**
     * 只实现同步链路会碰到的接口；其余方法一律抛出，避免用"意外调用"掩盖装配错误。
     * `sendHeartbeat` 返回一个空的成功响应（本测试不关心服务端心跳）。
     */
    private fun fakeApi(): ApiService = Proxy.newProxyInstance(
        ApiService::class.java.classLoader,
        arrayOf(ApiService::class.java)
    ) { _, method, _ ->
        when (method.name) {
            "sendHeartbeat" -> ApiResponse<Any>(code = 0, message = "ok", data = null)
            "toString" -> "StubApiService"
            "hashCode" -> System.identityHashCode(this)
            "equals" -> false
            else -> error("Unexpected API call in test: ${method.name}")
        }
    } as ApiService

    private fun coordinator(
        heartbeatRecorder: WakeHeartbeatRecorder? = null
    ): MobileSyncCoordinator {
        val api = fakeApi()
        val trackPrefs = context.getSharedPreferences("heartbeat-sync-test", Context.MODE_PRIVATE)
        trackPrefs.edit().clear().commit()
        val trackSettings = TrackingSettingsStore(trackPrefs)
        val logs = StructuredLogRepository(context, trackSettings) { System.currentTimeMillis() }
        val tokenManager = TokenManager(TestSecurePreferencesFactory(trackPrefs))

        return MobileSyncCoordinator(
            context = context,
            api = api,
            tokenManager = tokenManager,
            usageAccessChecker = UsageAccessChecker(context),
            usageEventCollector = UsageEventCollector(context, UsageAccessChecker(context)),
            appMetadataCollector = AppMetadataCollector(context),
            database = db,
            logs = logs,
            heartbeatReporter = MobileHeartbeatReporter(context, api),
            serverSettingsStore = ServerSettingsStore(context, tokenManager),
            locationUploadCoordinator = LocationUploadCoordinator(
                context, db, api, TrajectoryCompressor()
            ),
            forensicUploadCoordinator = ForensicUploadCoordinator(
                context = context,
                api = api,
                dao = db.forensicEventDao(),
                logs = logs,
                sentinelStore = ForensicSentinelStore(context)
            ),
            wakeHeartbeatRecorder = heartbeatRecorder ?: WakeHeartbeatRecorder(
                ledger = ForensicLedger(db.forensicEventDao(), logs),
                contextReader = FixedForensicContextSource(ForensicContext()),
                snapshotReader = AndroidHeartbeatSnapshotReader(context),
                logs = logs
            ),
            syncScheduler = { MobileSyncScheduler(context, trackSettings) }
        )
    }

    private suspend fun heartbeats() =
        db.forensicEventDao().recentByType(ForensicEventTypes.HEARTBEAT, 10)

    @Test
    fun `issue345 a periodic sync wake writes exactly one heartbeat to the ledger`() = runTest {
        assertEquals(0, heartbeats().size)

        coordinator().syncOnOpen()

        val rows = heartbeats()
        assertEquals(1, rows.size)
        assertEquals(ForensicEventTypes.HEARTBEAT, rows.single().eventType)
    }

    @Test
    fun `issue345 the sync-wake heartbeat carries the REQ-3 fields`() = runTest {
        coordinator().syncOnOpen()

        val payload = heartbeats().single().payloadJson
        // REQ-3 要求字段：时刻（台账列）、开机时长、距上次心搏间隔、待机桶、电池优化、Doze/省电、前台服务。
        assertTrue("缺少 bootElapsedMs：$payload", payload.contains("bootElapsedMs"))
        assertTrue("缺少 sinceLastHeartbeatMs：$payload", payload.contains("sinceLastHeartbeatMs"))
        assertTrue("缺少 standbyBucket：$payload", payload.contains("standbyBucket"))
        assertTrue("缺少 ignoringBatteryOptimizations：$payload", payload.contains("ignoringBatteryOptimizations"))
        assertTrue("缺少 dozeMode：$payload", payload.contains("dozeMode"))
        assertTrue("缺少 powerSaveMode：$payload", payload.contains("powerSaveMode"))
        assertTrue("缺少 foregroundServiceRunning：$payload", payload.contains("foregroundServiceRunning"))
    }

    @Test
    fun `AC-3_3 two sync wakes within the same second still produce one heartbeat`() = runTest {
        // 注入固定时钟：AC-3.3 说的是"同一秒内多次唤醒"，不是"两次调用之间真实流逝了多久"。
        // 两次唤醒刻意取同一秒里不同的毫秒（+10ms），这样断言的是秒级幂等键本身，
        // 而不是"两次调用恰好拿到同一个毫秒值"——否则把幂等键的粒度改成毫秒也照样能过。
        val trackPrefs = context.getSharedPreferences("heartbeat-sync-test", Context.MODE_PRIVATE)
        val logs = StructuredLogRepository(context, TrackingSettingsStore(trackPrefs)) { FIXED_NOW }
        var now = FIXED_NOW
        val recorder = WakeHeartbeatRecorder(
            ledger = ForensicLedger(db.forensicEventDao(), logs),
            contextReader = FixedForensicContextSource(ForensicContext()),
            snapshotReader = AndroidHeartbeatSnapshotReader(context),
            logs = logs,
            nowUtcMillis = { now },
            bootElapsedMillis = { FIXED_BOOT_ELAPSED },
            serviceRunning = { false }
        )
        val sync = coordinator(recorder)

        sync.syncOnOpen()
        now += 10L
        sync.syncOnOpen()

        assertEquals(1, heartbeats().size)
    }

    @Test
    fun `AC-3_3 a failing ledger write does not break the sync run`() = runTest {
        // 让"写台账"确定性地失败：包一层 DAO，每次插入都抛。
        // 这比关数据库更可靠——关库后 Room 在 Robolectric 下有时仍会复用已打开的连接，
        // 断言就可能变成"其实写成功了，只是没读回来"。
        val trackPrefs = context.getSharedPreferences("heartbeat-sync-test", Context.MODE_PRIVATE)
        val logs = StructuredLogRepository(context, TrackingSettingsStore(trackPrefs)) { FIXED_NOW }
        val failingDao = FailingInsertForensicDao(db.forensicEventDao())
        val recorder = WakeHeartbeatRecorder(
            ledger = ForensicLedger(failingDao, logs),
            contextReader = FixedForensicContextSource(ForensicContext()),
            snapshotReader = AndroidHeartbeatSnapshotReader(context),
            logs = logs,
            nowUtcMillis = { FIXED_NOW },
            bootElapsedMillis = { FIXED_BOOT_ELAPSED },
            serviceRunning = { false }
        )

        // 先证明"失败"是真的：心搏写入确实返回 false（而不是默默成功、让同步测试空过）。
        assertFalse("台账写入失败时 record() 必须返回 false", recorder.record())
        assertEquals("写入失败不得落任何心搏行", 0, heartbeats().size)

        val state = coordinator(recorder).syncOnOpen()

        // 服务器未配置 → 同步本身有序地结束在 "server-missing"，而不是因为写心搏失败而异常。
        assertEquals("server-missing", state.phase)
    }

    @Test
    fun `issue345 a wake skipped by the sync lock is still recorded`() = runTest {
        // 两次唤醒同时到达：第二次因乐观锁直接返回"同步正在进行中"。
        // 它同样是真实唤醒，心搏不能因为"同步没跑起来"而丢失——否则又会出现
        // 「同步批次有、心搏没有」的口径缺口（正是 #345 的成因）。
        val trackPrefs = context.getSharedPreferences("heartbeat-sync-test", Context.MODE_PRIVATE)
        val logs = StructuredLogRepository(context, TrackingSettingsStore(trackPrefs)) { FIXED_NOW }
        // 两次唤醒时刻不同（不同秒），确保去重不会把它们合成一条。
        var now = FIXED_NOW
        val recorder = WakeHeartbeatRecorder(
            ledger = ForensicLedger(db.forensicEventDao(), logs),
            contextReader = FixedForensicContextSource(ForensicContext()),
            snapshotReader = AndroidHeartbeatSnapshotReader(context),
            logs = logs,
            nowUtcMillis = { now },
            bootElapsedMillis = { FIXED_BOOT_ELAPSED },
            serviceRunning = { false }
        )
        val sync = coordinator(recorder)

        // 手工占住互斥锁，模拟"已有同步在跑"。
        val lockField = MobileSyncCoordinator::class.java.getDeclaredField("syncMutex")
        lockField.isAccessible = true
        val mutex = lockField.get(sync) as kotlinx.coroutines.sync.Mutex
        mutex.lock()

        try {
            now += 60_000L
            val state = sync.syncOnOpen()
            assertEquals("同步正在进行中。", state.progressText)
        } finally {
            mutex.unlock()
        }

        // 被锁挡下的那次唤醒仍然留下了心搏。
        assertEquals(1, heartbeats().size)
    }

    private companion object {
        /** AC-3.3 的同一秒内：毫秒只在同一秒里抖动。 */
        const val FIXED_NOW = 1_790_300_000_000L
        const val FIXED_BOOT_ELAPSED = 82_000L
    }
}

/** 测试用的内存 SharedPreferences 工厂（TokenManager 只依赖这个接口）。 */
private class TestSecurePreferencesFactory(
    private val preferences: SharedPreferences
) : SecurePreferencesFactory {
    override fun open(): SharedPreferences = preferences
}

/**
 * 只让 `insertIgnore` 失败的 DAO 代理（其余方法原样转发给真实 DAO）。
 * 用动态代理而不是"关掉数据库"，是为了让"写入失败"这件事确定性地发生。
 */
private fun FailingInsertForensicDao(delegate: ForensicEventDao): ForensicEventDao =
    Proxy.newProxyInstance(
        ForensicEventDao::class.java.classLoader,
        arrayOf(ForensicEventDao::class.java)
    ) { _, method, args ->
        when (method.name) {
            "insertIgnore" -> throw android.database.sqlite.SQLiteException("disk I/O error (injected)")
            "toString" -> "FailingInsertForensicDao"
            "hashCode" -> System.identityHashCode(delegate)
            "equals" -> false
            else -> method.invoke(delegate, *(args ?: emptyArray()))
        }
    } as ForensicEventDao

/** 固定的取证上下文：本测试只关心"唤醒是否落一条心搏"，不关心上下文读数。 */
private class FixedForensicContextSource(
    private val value: ForensicContext
) : ForensicContextSource {
    override fun read(): ForensicContext = value
}
