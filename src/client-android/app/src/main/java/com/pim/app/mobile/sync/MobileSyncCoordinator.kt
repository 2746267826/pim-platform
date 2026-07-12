package com.pim.app.mobile.sync

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import androidx.work.ExistingWorkPolicy
import androidx.work.WorkManager
import com.pim.app.data.AppDatabase
import com.pim.app.data.MobileAppMetadataEntity
import com.pim.app.data.MobileDeviceProfileEntity
import com.pim.app.data.MobileSyncStatus
import com.pim.app.data.MobileUsageEventEntity
import com.pim.app.data.MobileUsageSummaryEntity
import com.pim.app.mobile.logs.StructuredLogRepository
import com.pim.app.mobile.usage.AppMetadataCollector
import com.pim.app.mobile.usage.UsageAccessChecker
import com.pim.app.mobile.usage.UsageEventCollector
import com.pim.core.auth.TokenManager
import com.pim.core.models.MobileAppMetadataDto
import com.pim.core.models.MobileDeviceRegisterRequest
import com.pim.core.models.MobileGapRequest
import com.pim.core.models.MobileIngestResponse
import com.pim.core.models.MobileUsageEventDto
import com.pim.core.models.MobileUsageEventsUploadRequest
import com.pim.core.models.MobileUsageSummaryDto
import com.pim.core.network.ApiService
import com.pim.core.settings.ServerSettingsStore
import com.pim.core.util.toCauseChainMessage
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import org.json.JSONObject
import java.net.URI
import java.security.MessageDigest
import java.time.Instant
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

data class MobileSyncState(
    val phase: String,
    val progressText: String,
    val isInProgress: Boolean = false,
    val acceptedCount: Int = 0,
    val skippedCount: Int = 0,
    val rejectedCount: Int = 0,
    val failedCount: Int = 0,
    val lastError: String? = null,
    val lastErrorDetail: String? = null,
    val pendingQueueCount: Int = 0,
    val gapWindowCount: Int = 0,
    val currentWindowIndex: Int = 0,
    val currentWindowStartUtc: String? = null,
    val currentWindowEndUtc: String? = null,
    val currentEventCount: Int = 0,
    val currentSummaryCount: Int = 0,
    val currentAppMetadataCount: Int = 0,
    val lastBatchId: String? = null,
    val lastBatchStatus: String? = null,
    val heartbeatStatus: String? = null,
    val lastAttemptedUploadAt: String? = null,
    val lastSuccessfulUploadAt: String? = null
)

@Singleton
class MobileSyncCoordinator @Inject constructor(
    @ApplicationContext private val context: Context,
    private val api: ApiService,
    private val tokenManager: TokenManager,
    private val usageAccessChecker: UsageAccessChecker,
    private val usageEventCollector: UsageEventCollector,
    private val appMetadataCollector: AppMetadataCollector,
    private val database: AppDatabase,
    private val logs: StructuredLogRepository,
    private val heartbeatReporter: MobileHeartbeatReporter,
    private val serverSettingsStore: ServerSettingsStore,
    private val locationUploadCoordinator: LocationUploadCoordinator
) {
    private val mobileDataDao = database.mobileDataDao()
    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val syncMutex = Mutex()
    private val _state = MutableStateFlow(readPersistedState())
    val currentState: StateFlow<MobileSyncState> = _state.asStateFlow()

    suspend fun syncOnOpen(): MobileSyncState {
        if (!syncMutex.tryLock()) {
            val running = _state.value.copy(
                isInProgress = true,
                progressText = "同步正在进行中。"
            )
            persistState(running)
            return running
        }

        return try {
            runSyncOnOpen()
        } finally {
            syncMutex.unlock()
        }
    }

    fun refreshPersistedState() {
        _state.value = readPersistedState()
    }

    private suspend fun runSyncOnOpen(): MobileSyncState {
        val attemptedAt = nowIso()
        val serverUrl = configuredServerUrl()
        val deviceIdentity = deviceIdentity()
        val hasToken = !tokenManager
            .getAccessTokenForServer(serverSettingsStore.getBaseUrl())
            .isNullOrBlank()
        val hasUsageAccess = usageAccessChecker.hasUsageAccess()

        if (serverUrl.isBlank()) {
            return finishWithLocalError(
                deviceId = deviceIdentity.deviceId,
                serverUrl = serverUrl,
                usagePermissionGranted = hasUsageAccess,
                attemptedAt = attemptedAt,
                phase = "server-missing",
                message = "服务器地址未配置，已跳过同步。"
            )
        }

        if (!hasToken) {
            logs.warn("mobile-sync", "缺少登录令牌，已跳过同步。")
            val authMissing = state(
                phase = "auth-missing",
                progressText = "缺少登录令牌，已跳过同步。请登录后重新同步。",
                lastError = "缺少登录令牌",
                lastAttemptedUploadAt = attemptedAt
            )
            persistState(authMissing)
            return authMissing
        }

        if (!hasUsageAccess) {
            logs.warn("mobile-sync", "缺少应用使用情况权限，已跳过使用记录同步。")
            val missingPermissionState = state(
                phase = "usage-permission-missing",
                progressText = "缺少应用使用情况权限，已跳过同步。",
                skippedCount = 1,
                lastError = "缺少应用使用情况权限",
                lastAttemptedUploadAt = attemptedAt
            )
            persistState(missingPermissionState)
            val locationState = uploadQueuedLocations(missingPermissionState, attemptedAt)
            sendHeartbeat(deviceIdentity.deviceId, serverUrl, false, locationState)
            return locationState
        }

        return try {
            logs.info(
                "mobile-sync",
                "开始执行打开 App 后的手机同步。",
                mapOf("deviceId" to deviceIdentity.deviceId, "serverUrl" to serverUrl)
            )

            val preparing = state(
                phase = "preparing",
                progressText = "正在注册设备并准备同步。",
                isInProgress = true,
                pendingQueueCount = pendingQueueCount(),
                lastAttemptedUploadAt = attemptedAt
            )
            persistState(preparing)

            val profile = buildDeviceProfile(deviceIdentity, nowUtc = System.currentTimeMillis())
            mobileDataDao.upsertDeviceProfile(profile)
            registerDevice(deviceIdentity, profile)
            mobileDataDao.updateDeviceProfileSyncStatus(syncStatus = MobileSyncStatus.SYNCED)

            val rangeEndUtc = System.currentTimeMillis()
            val rangeStartUtc = rangeEndUtc - FOURTEEN_DAYS_MS
            val gapChecking = state(
                phase = "gap-checking",
                progressText = "正在询问服务器缺失时间窗。",
                isInProgress = true,
                pendingQueueCount = pendingQueueCount(),
                lastAttemptedUploadAt = attemptedAt
            )
            persistState(gapChecking)
            val gapResponse = api.getMobileGaps(
                MobileGapRequest(
                    deviceIdentity.deviceId,
                    iso(rangeStartUtc),
                    iso(rangeEndUtc),
                    capabilityJson(hasUsageAccess)
                )
            )

            val gapData = gapResponse.data
            if (gapResponse.code != 0 || gapData == null) {
                throw IllegalStateException(gapResponse.message.ifBlank { "服务器缺口查询失败。" })
            }

            val serverWindows = gapData.windows.mapNotNull { window ->
                val originalStart = parseIsoMillis(window.windowStartUtc)
                val originalEnd = parseIsoMillis(window.windowEndUtc)
                clampGapWindow(
                    windowStartUtc = originalStart,
                    windowEndUtc = originalEnd,
                    maxBackfillStartUtc = parseIsoMillis(gapData.maxBackfillStartUtc),
                    nowUtc = rangeEndUtc
                )?.also { clamped ->
                    if (clamped.windowStartUtc != originalStart || clamped.windowEndUtc != originalEnd) {
                        logs.warn(
                            "mobile-sync",
                            "服务器缺口窗口已按 Android 14 天补全上限裁剪。",
                            mapOf(
                                "originalStartUtc" to window.windowStartUtc,
                                "originalEndUtc" to window.windowEndUtc,
                                "clampedStartUtc" to iso(clamped.windowStartUtc),
                                "clampedEndUtc" to iso(clamped.windowEndUtc)
                            )
                        )
                    }
                }
            }
            val windows = serverWindows.flatMap { window ->
                splitGapWindowForUpload(window.windowStartUtc, window.windowEndUtc)
            }
            logs.info(
                "mobile-sync",
                "服务器返回 ${serverWindows.size} 个缺口窗口，已拆为 ${windows.size} 个上传窗口。",
                mapOf("serverWindowCount" to serverWindows.size, "uploadWindowCount" to windows.size)
            )

            var current = state(
                phase = "collecting",
                progressText = "正在采集服务器要求补全的窗口。",
                isInProgress = true,
                gapWindowCount = windows.size,
                pendingQueueCount = pendingQueueCount(),
                lastAttemptedUploadAt = attemptedAt
            )
            persistState(current)

            for ((index, window) in windows.withIndex()) {
                val windowStartUtc = iso(window.windowStartUtc)
                val windowEndUtc = iso(window.windowEndUtc)
                current = current.copy(
                    phase = "collecting",
                    progressText = "正在采集第 ${index + 1}/${windows.size} 个窗口。",
                    isInProgress = true,
                    currentWindowIndex = index + 1,
                    currentWindowStartUtc = windowStartUtc,
                    currentWindowEndUtc = windowEndUtc,
                    gapWindowCount = windows.size,
                    pendingQueueCount = pendingQueueCount(),
                    lastAttemptedUploadAt = attemptedAt
                )
                persistState(current)

                logs.info(
                    "mobile-sync",
                    "正在采集服务器缺口窗口的使用记录。",
                    mapOf(
                        "windowStartUtc" to windowStartUtc,
                        "windowEndUtc" to windowEndUtc
                    )
                )

                val collection = usageEventCollector.collectUsage(window.windowStartUtc, window.windowEndUtc)
                val eventIds = mobileDataDao.insertUsageEvents(collection.events)
                val summaryIds = mobileDataDao.insertUsageSummaries(collection.summaries)
                val packageNames = packageNames(collection.events, collection.summaries)
                val appMetadata = appMetadataCollector.collectForPackages(packageNames)
                if (appMetadata.isNotEmpty()) {
                    mobileDataDao.upsertAppMetadata(appMetadata)
                }

                current = current.copy(
                    phase = "uploading",
                    progressText = "正在上传第 ${index + 1}/${windows.size} 个窗口。",
                    isInProgress = true,
                    currentWindowIndex = index + 1,
                    currentWindowStartUtc = windowStartUtc,
                    currentWindowEndUtc = windowEndUtc,
                    currentEventCount = collection.events.size,
                    currentSummaryCount = collection.summaries.size,
                    currentAppMetadataCount = appMetadata.size,
                    pendingQueueCount = pendingQueueCount(),
                    lastAttemptedUploadAt = attemptedAt
                )
                persistState(current)

                val uploadState = uploadWindow(
                    deviceId = deviceIdentity.deviceId,
                    windowStartUtc = windowStartUtc,
                    windowEndUtc = windowEndUtc,
                    events = collection.events,
                    summaries = collection.summaries,
                    apps = appMetadata,
                    eventIds = eventIds,
                    summaryIds = summaryIds
                )

                val merged = current.merge(uploadState)
                val hasUploadErrors = merged.failedCount > 0 || merged.lastError != null
                current = merged.copy(
                    phase = if (hasUploadErrors) {
                        "upload-failed"
                    } else {
                        "uploading"
                    },
                    progressText = if (hasUploadErrors) {
                        merged.lastError ?: uploadState.progressText
                    } else {
                        "第 ${index + 1}/${windows.size} 个窗口上传完成。"
                    },
                    isInProgress = true,
                    gapWindowCount = windows.size,
                    currentWindowIndex = index + 1,
                    currentWindowStartUtc = windowStartUtc,
                    currentWindowEndUtc = windowEndUtc,
                    currentEventCount = collection.events.size,
                    currentSummaryCount = collection.summaries.size,
                    currentAppMetadataCount = appMetadata.size,
                    lastBatchId = uploadState.lastBatchId,
                    lastBatchStatus = uploadState.lastBatchStatus,
                    pendingQueueCount = pendingQueueCount(),
                    lastAttemptedUploadAt = attemptedAt
                )
                persistState(current)
            }

            current = uploadQueuedLocations(current, attemptedAt)

            val completed = current.copy(
                phase = if (current.failedCount == 0) "completed" else "completed-with-errors",
                progressText = if (current.failedCount == 0) {
                    "手机同步已完成。"
                } else {
                    "手机同步已完成，但部分上传失败。"
                },
                isInProgress = false,
                pendingQueueCount = pendingQueueCount(),
                lastSuccessfulUploadAt = if (current.failedCount == 0) nowIso() else current.lastSuccessfulUploadAt
            )
            persistState(completed)
            sendHeartbeat(deviceIdentity.deviceId, serverUrl, true, completed)
            logs.info(
                "mobile-sync",
                "手机同步已完成。",
                mapOf(
                    "acceptedCount" to completed.acceptedCount,
                    "skippedCount" to completed.skippedCount,
                    "rejectedCount" to completed.rejectedCount,
                    "failedCount" to completed.failedCount
                )
            )
            completed
        } catch (ex: Exception) {
            val previous = _state.value
            val detail = ex.toCauseChainMessage()
            val failed = previous.copy(
                phase = "failed",
                progressText = "手机同步失败。",
                isInProgress = false,
                failedCount = maxOf(1, previous.failedCount),
                lastError = ex.message ?: ex::class.java.simpleName,
                lastErrorDetail = detail,
                pendingQueueCount = pendingQueueCount(),
                lastAttemptedUploadAt = attemptedAt
            )
            logs.error("mobile-sync", "手机同步失败：$detail", ex)
            persistState(failed)
            sendHeartbeat(deviceIdentity.deviceId, serverUrl, true, failed)
            failed
        }
    }

    private suspend fun uploadQueuedLocations(
        current: MobileSyncState,
        attemptedAt: String
    ): MobileSyncState {
        val updates = locationUploadCoordinator.uploadPending()
        if (updates.syncedIds.isEmpty() && updates.failedIds.isEmpty()) {
            val idle = current.copy(
                pendingQueueCount = pendingQueueCount(),
                lastAttemptedUploadAt = attemptedAt
            )
            persistState(idle)
            return idle
        }

        if (updates.shouldRetry) {
            enqueueLocationRetry()
        }

        val syncedCount = updates.syncedIds.size
        val locationFailedCount = updates.failedIds.size
        val next = current.copy(
            phase = when {
                updates.shouldRetry -> "location-upload-failed"
                current.phase == "usage-permission-missing" -> current.phase
                else -> "location-uploaded"
            },
            progressText = when {
                updates.shouldRetry -> "定位队列上传失败，已安排网络重试。"
                current.phase == "usage-permission-missing" ->
                    "${current.progressText} 定位队列已同步 $syncedCount 条。"
                else -> "定位队列已同步 $syncedCount 条。"
            },
            acceptedCount = current.acceptedCount + syncedCount,
            failedCount = current.failedCount + locationFailedCount,
            lastError = if (updates.shouldRetry) updates.failedReason ?: current.lastError else current.lastError,
            lastErrorDetail = if (updates.shouldRetry) updates.failedReason ?: current.lastErrorDetail else current.lastErrorDetail,
            pendingQueueCount = pendingQueueCount(),
            lastAttemptedUploadAt = attemptedAt
        )
        persistState(next)

        val details = mapOf(
            "syncedCount" to syncedCount,
            "failedCount" to locationFailedCount
        )
        if (updates.shouldRetry) {
            logs.warn("mobile-location-sync", "定位队列上传未完成，已安排 WorkManager 重试。", details)
        } else {
            logs.info("mobile-location-sync", "定位队列上传完成。", details)
        }
        return next
    }

    private fun enqueueLocationRetry() {
        WorkManager.getInstance(context).enqueueUniqueWork(
            LocationSyncWorker.WORK_NAME,
            ExistingWorkPolicy.REPLACE,
            LocationSyncWorker.oneTimeRequest()
        )
    }

    private suspend fun uploadWindow(
        deviceId: String,
        windowStartUtc: String,
        windowEndUtc: String,
        events: List<MobileUsageEventEntity>,
        summaries: List<MobileUsageSummaryEntity>,
        apps: List<MobileAppMetadataEntity>,
        eventIds: List<Long>,
        summaryIds: List<Long>
    ): MobileSyncState {
        val batchId = stableBatchId(deviceId, windowStartUtc, windowEndUtc)
        val request = MobileUsageEventsUploadRequest(
            deviceId,
            batchId,
            windowStartUtc,
            windowEndUtc,
            apps.map { it.toDto() },
            events.mapIndexed { index, event -> event.toDto(eventIds[index].toString()) },
            summaries.mapIndexed { index, summary -> summary.toDto(summaryIds[index].toString()) }
        )

        val response = api.uploadMobileUsage(request)
        val ingest = response.data
        if (response.code != 0 || ingest == null) {
            val message = response.message.ifBlank { "Usage upload failed." }
            markUsageFailed(eventIds, summaryIds, apps, message)
            logs.warn(
                "mobile-sync",
                "Usage upload failed.",
                mapOf("windowStartUtc" to windowStartUtc, "windowEndUtc" to windowEndUtc, "message" to message)
            )
            return state(
                phase = "upload-failed",
                progressText = message,
                failedCount = maxOf(1, events.size + summaries.size),
                lastError = message,
                lastErrorDetail = message,
                currentWindowStartUtc = windowStartUtc,
                currentWindowEndUtc = windowEndUtc,
                currentEventCount = events.size,
                currentSummaryCount = summaries.size,
                currentAppMetadataCount = apps.size,
                lastBatchId = batchId,
                lastBatchStatus = "failed",
                pendingQueueCount = pendingQueueCount()
            )
        }

        val sentItems = linkedSetOf<MobileAcknowledgementItem>().apply {
            eventIds.forEach { add(MobileAcknowledgementItem("usage-event", it.toString())) }
            summaryIds.forEach { add(MobileAcknowledgementItem("usage-summary", it.toString())) }
            apps.forEach { add(MobileAcknowledgementItem("app-metadata", "${it.packageName}@${it.versionCode}")) }
        }
        processUsageAcknowledgements(mobileDataDao, sentItems, ingest)
        logs.info(
            "mobile-sync",
            "使用记录窗口已上传。",
            mapOf(
                "windowStartUtc" to windowStartUtc,
                "windowEndUtc" to windowEndUtc,
                "acceptedCount" to ingest.acceptedCount,
                "skippedCount" to ingest.skippedCount,
                "rejectedCount" to ingest.rejectedCount,
                "failedCount" to ingest.failedCount
            )
        )

        return ingest.toState(
            batchId = batchId,
            windowStartUtc = windowStartUtc,
            windowEndUtc = windowEndUtc,
            eventCount = events.size,
            summaryCount = summaries.size,
            appMetadataCount = apps.size
        )
    }

    private suspend fun registerDevice(
        identity: DeviceIdentity,
        profile: MobileDeviceProfileEntity
    ) {
        val response = api.registerMobileDevice(
            MobileDeviceRegisterRequest(
                identity.deviceId,
                identity.androidIdHash,
                displayName(profile),
                profile.manufacturer,
                profile.brand,
                profile.model,
                profile.androidVersion,
                profile.sdkInt,
                profile.appVersionName ?: "unknown",
                profile.rawJson
            )
        )

        if (response.code != 0 || response.data == null) {
            val message = response.message.ifBlank { "Device registration failed." }
            mobileDataDao.updateDeviceProfileSyncStatus(
                syncStatus = MobileSyncStatus.FAILED,
                lastError = message
            )
            throw IllegalStateException(message)
        }

        logs.info("mobile-sync", "Android 设备已注册。", mapOf("deviceId" to identity.deviceId))
    }

    private suspend fun markUsageSynced(
        eventIds: List<Long>,
        summaryIds: List<Long>,
        apps: List<MobileAppMetadataEntity>
    ) {
        if (eventIds.isNotEmpty()) {
            mobileDataDao.updateUsageEventSyncStatus(eventIds, MobileSyncStatus.SYNCED)
        }
        if (summaryIds.isNotEmpty()) {
            mobileDataDao.updateUsageSummarySyncStatus(summaryIds, MobileSyncStatus.SYNCED)
        }
        val packageNames = apps.map { it.packageName }
        if (packageNames.isNotEmpty()) {
            mobileDataDao.updateAppMetadataSyncStatus(packageNames, MobileSyncStatus.SYNCED)
        }
    }

    private suspend fun markUsageFailed(
        eventIds: List<Long>,
        summaryIds: List<Long>,
        apps: List<MobileAppMetadataEntity>,
        message: String
    ) {
        if (eventIds.isNotEmpty()) {
            mobileDataDao.updateUsageEventSyncStatus(eventIds, MobileSyncStatus.FAILED, message)
        }
        if (summaryIds.isNotEmpty()) {
            mobileDataDao.updateUsageSummarySyncStatus(summaryIds, MobileSyncStatus.FAILED, message)
        }
        val packageNames = apps.map { it.packageName }
        if (packageNames.isNotEmpty()) {
            mobileDataDao.updateAppMetadataSyncStatus(packageNames, MobileSyncStatus.FAILED, message)
        }
    }

    private suspend fun finishWithLocalError(
        deviceId: String,
        serverUrl: String,
        usagePermissionGranted: Boolean,
        attemptedAt: String,
        phase: String,
        message: String
    ): MobileSyncState {
        logs.warn("mobile-sync", message)
        val failed = state(
            phase = phase,
            progressText = message,
            lastError = message,
            lastErrorDetail = message,
            pendingQueueCount = pendingQueueCount(),
            lastAttemptedUploadAt = attemptedAt
        )
        sendHeartbeat(deviceId, serverUrl, usagePermissionGranted, failed)
        persistState(failed)
        return failed
    }

    private suspend fun sendHeartbeat(
        deviceId: String,
        serverUrl: String,
        usagePermissionGranted: Boolean,
        state: MobileSyncState
    ) {
        try {
            heartbeatReporter.report(deviceId, serverUrl, usagePermissionGranted, state)
            persistState(state.copy(heartbeatStatus = "心跳上报成功"))
            logs.info("mobile-heartbeat", "Android 心跳已上报。", mapOf("phase" to state.phase))
        } catch (ex: Exception) {
            val detail = ex.toCauseChainMessage()
            persistState(state.copy(heartbeatStatus = "心跳上报失败", lastErrorDetail = detail))
            logs.error("mobile-heartbeat", "Android 心跳上报失败：$detail", ex)
        }
    }

    private suspend fun pendingQueueCount(): Int {
        return mobileDataDao.pendingUsageEventCount().first() +
            mobileDataDao.pendingUsageSummaryCount().first() +
            mobileDataDao.pendingAppMetadataCount().first() +
            mobileDataDao.pendingLocationPointCount().first()
    }

    private fun buildDeviceProfile(
        identity: DeviceIdentity,
        nowUtc: Long
    ): MobileDeviceProfileEntity {
        val version = appVersion()
        val rawJson = JSONObject()
            .put("deviceId", identity.deviceId)
            .put("androidIdHash", identity.androidIdHash ?: JSONObject.NULL)
            .put("manufacturer", Build.MANUFACTURER ?: "")
            .put("brand", Build.BRAND ?: "")
            .put("model", Build.MODEL ?: "")
            .put("hardware", Build.HARDWARE ?: "")
            .put("androidVersion", Build.VERSION.RELEASE ?: "")
            .put("sdkInt", Build.VERSION.SDK_INT)
            .put("appVersionName", version.first ?: JSONObject.NULL)
            .put("appVersionCode", version.second ?: JSONObject.NULL)
            .put("collectedAtUtc", nowUtc)
            .toString()

        return MobileDeviceProfileEntity(
            deviceId = identity.deviceId,
            manufacturer = Build.MANUFACTURER ?: "",
            brand = Build.BRAND ?: "",
            model = Build.MODEL ?: "",
            hardware = Build.HARDWARE ?: "",
            androidVersion = Build.VERSION.RELEASE ?: "",
            sdkInt = Build.VERSION.SDK_INT,
            appVersionName = version.first,
            appVersionCode = version.second,
            collectedAtUtc = nowUtc,
            rawJson = rawJson
        )
    }

    private fun deviceIdentity(): DeviceIdentity {
        val androidId = Settings.Secure.getString(
            context.contentResolver,
            Settings.Secure.ANDROID_ID
        )
        val seed = androidId ?: Build.FINGERPRINT ?: "android-device"
        val hash = sha256(seed)
        return DeviceIdentity(
            deviceId = "android-${hash.take(16)}",
            androidIdHash = androidId?.let { sha256(it) }
        )
    }

    private fun configuredServerUrl(): String {
        return normalizeServerUrl(serverSettingsStore.getBaseUrl())
    }

    private fun normalizeServerUrl(value: String): String {
        val trimmed = value.trim().trimEnd('/')
        return try {
            val uri = URI(trimmed)
            if (uri.host.equals("localhost", ignoreCase = true)) {
                URI(uri.scheme, uri.userInfo, "127.0.0.1", uri.port, uri.path, uri.query, uri.fragment)
                    .toString()
                    .trimEnd('/')
            } else {
                trimmed
            }
        } catch (_: Exception) {
            trimmed
        }
    }

    private fun capabilityJson(usagePermissionGranted: Boolean): String {
        return JSONObject()
            .put("usageEvents", usagePermissionGranted)
            .put("usageStatsFallback", usagePermissionGranted)
            .put("appMetadata", true)
            .put("maxBackfillDays", 14)
            .put("client", "android")
            .toString()
    }

    private fun persistState(state: MobileSyncState) {
        prefs.edit()
            .putString("phase", state.phase)
            .putString("progress_text", state.progressText)
            .putBoolean("is_in_progress", state.isInProgress)
            .putInt("accepted_count", state.acceptedCount)
            .putInt("skipped_count", state.skippedCount)
            .putInt("rejected_count", state.rejectedCount)
            .putInt("failed_count", state.failedCount)
            .putString("last_error", state.lastError)
            .putString("last_error_detail", state.lastErrorDetail)
            .putInt("pending_queue_count", state.pendingQueueCount)
            .putInt("gap_window_count", state.gapWindowCount)
            .putInt("current_window_index", state.currentWindowIndex)
            .putString("current_window_start_utc", state.currentWindowStartUtc)
            .putString("current_window_end_utc", state.currentWindowEndUtc)
            .putInt("current_event_count", state.currentEventCount)
            .putInt("current_summary_count", state.currentSummaryCount)
            .putInt("current_app_metadata_count", state.currentAppMetadataCount)
            .putString("last_batch_id", state.lastBatchId)
            .putString("last_batch_status", state.lastBatchStatus)
            .putString("heartbeat_status", state.heartbeatStatus)
            .putString("last_attempted_upload_at", state.lastAttemptedUploadAt)
            .putString("last_successful_upload_at", state.lastSuccessfulUploadAt)
            .commit()
        _state.value = state
    }

    private fun readPersistedState(): MobileSyncState {
        return MobileSyncState(
            phase = prefs.getString("phase", null) ?: "waiting",
            progressText = prefs.getString("progress_text", null) ?: "打开 App 后会自动同步一次。",
            isInProgress = prefs.getBoolean("is_in_progress", false),
            acceptedCount = prefs.getInt("accepted_count", 0),
            skippedCount = prefs.getInt("skipped_count", 0),
            rejectedCount = prefs.getInt("rejected_count", 0),
            failedCount = prefs.getInt("failed_count", 0),
            lastError = prefs.getString("last_error", null),
            lastErrorDetail = prefs.getString("last_error_detail", null),
            pendingQueueCount = prefs.getInt("pending_queue_count", 0),
            gapWindowCount = prefs.getInt("gap_window_count", 0),
            currentWindowIndex = prefs.getInt("current_window_index", 0),
            currentWindowStartUtc = prefs.getString("current_window_start_utc", null),
            currentWindowEndUtc = prefs.getString("current_window_end_utc", null),
            currentEventCount = prefs.getInt("current_event_count", 0),
            currentSummaryCount = prefs.getInt("current_summary_count", 0),
            currentAppMetadataCount = prefs.getInt("current_app_metadata_count", 0),
            lastBatchId = prefs.getString("last_batch_id", null),
            lastBatchStatus = prefs.getString("last_batch_status", null),
            heartbeatStatus = prefs.getString("heartbeat_status", null),
            lastAttemptedUploadAt = prefs.getString("last_attempted_upload_at", null),
            lastSuccessfulUploadAt = prefs.getString("last_successful_upload_at", null)
        )
    }

    private fun previousSuccessfulUploadAt(): String? {
        return prefs.getString("last_successful_upload_at", null)
    }

    private fun state(
        phase: String,
        progressText: String,
        isInProgress: Boolean = false,
        acceptedCount: Int = 0,
        skippedCount: Int = 0,
        rejectedCount: Int = 0,
        failedCount: Int = 0,
        lastError: String? = null,
        lastErrorDetail: String? = null,
        pendingQueueCount: Int = 0,
        gapWindowCount: Int = 0,
        currentWindowIndex: Int = 0,
        currentWindowStartUtc: String? = null,
        currentWindowEndUtc: String? = null,
        currentEventCount: Int = 0,
        currentSummaryCount: Int = 0,
        currentAppMetadataCount: Int = 0,
        lastBatchId: String? = null,
        lastBatchStatus: String? = null,
        heartbeatStatus: String? = null,
        lastAttemptedUploadAt: String? = null,
        lastSuccessfulUploadAt: String? = null
    ): MobileSyncState {
        return MobileSyncState(
            phase = phase,
            progressText = progressText,
            isInProgress = isInProgress,
            acceptedCount = acceptedCount,
            skippedCount = skippedCount,
            rejectedCount = rejectedCount,
            failedCount = failedCount,
            lastError = lastError,
            lastErrorDetail = lastErrorDetail,
            pendingQueueCount = pendingQueueCount,
            gapWindowCount = gapWindowCount,
            currentWindowIndex = currentWindowIndex,
            currentWindowStartUtc = currentWindowStartUtc,
            currentWindowEndUtc = currentWindowEndUtc,
            currentEventCount = currentEventCount,
            currentSummaryCount = currentSummaryCount,
            currentAppMetadataCount = currentAppMetadataCount,
            lastBatchId = lastBatchId,
            lastBatchStatus = lastBatchStatus,
            heartbeatStatus = heartbeatStatus,
            lastAttemptedUploadAt = lastAttemptedUploadAt,
            lastSuccessfulUploadAt = lastSuccessfulUploadAt ?: previousSuccessfulUploadAt()
        )
    }

    private fun displayName(profile: MobileDeviceProfileEntity): String {
        return listOf(profile.manufacturer, profile.model)
            .filter { it.isNotBlank() }
            .joinToString(" ")
            .ifBlank { "Android device" }
    }

    private fun appVersion(): Pair<String?, Long?> {
        return try {
            val info = packageInfo(context.packageManager, context.packageName)
            info.versionName to versionCode(info)
        } catch (_: Exception) {
            null to null
        }
    }

    private fun packageInfo(packageManager: PackageManager, packageName: String): PackageInfo {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            packageManager.getPackageInfo(packageName, PackageManager.PackageInfoFlags.of(0))
        } else {
            @Suppress("DEPRECATION")
            packageManager.getPackageInfo(packageName, 0)
        }
    }

    private fun versionCode(packageInfo: PackageInfo): Long {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            packageInfo.longVersionCode
        } else {
            @Suppress("DEPRECATION")
            packageInfo.versionCode.toLong()
        }
    }

    private fun sha256(value: String): String {
        val bytes = MessageDigest.getInstance("SHA-256").digest(value.toByteArray())
        return bytes.joinToString("") { "%02x".format(Locale.US, it) }
    }

    private fun stableBatchId(deviceId: String, windowStartUtc: String, windowEndUtc: String): String {
        return "android-${sha256("$deviceId|$windowStartUtc|$windowEndUtc").take(24)}"
    }

    private data class DeviceIdentity(
        val deviceId: String,
        val androidIdHash: String?
    )

    companion object {
        private const val FOURTEEN_DAYS_MS = 14L * 24L * 60L * 60L * 1000L
        private const val PREFS_NAME = "pim_mobile_sync_state"
    }
}

private const val MAX_UPLOAD_WINDOW_MS = 2L * 60L * 60L * 1000L

private data class ClampedGapWindow(
    val windowStartUtc: Long,
    val windowEndUtc: Long
)

data class UploadWindow(
    val windowStartUtc: Long,
    val windowEndUtc: Long
)

private fun clampGapWindow(
    windowStartUtc: Long,
    windowEndUtc: Long,
    maxBackfillStartUtc: Long,
    nowUtc: Long
): ClampedGapWindow? {
    val effectiveStart = maxOf(
        windowStartUtc,
        maxBackfillStartUtc,
        nowUtc - 14L * 24L * 60L * 60L * 1000L
    )
    val effectiveEnd = minOf(windowEndUtc, nowUtc)
    return if (effectiveStart < effectiveEnd) {
        ClampedGapWindow(effectiveStart, effectiveEnd)
    } else {
        null
    }
}

fun splitGapWindowForUpload(
    windowStartUtc: Long,
    windowEndUtc: Long
): List<UploadWindow> {
    if (windowStartUtc >= windowEndUtc) {
        return emptyList()
    }

    val windows = mutableListOf<UploadWindow>()
    var start = windowStartUtc
    while (start < windowEndUtc) {
        val end = minOf(start + MAX_UPLOAD_WINDOW_MS, windowEndUtc)
        windows.add(UploadWindow(start, end))
        start = end
    }

    return windows
}

private fun MobileIngestResponse.toState(
    batchId: String,
    windowStartUtc: String,
    windowEndUtc: String,
    eventCount: Int,
    summaryCount: Int,
    appMetadataCount: Int
): MobileSyncState {
    return MobileSyncState(
        phase = "uploaded",
        progressText = "使用记录批次已上传。",
        acceptedCount = acceptedCount,
        skippedCount = skippedCount,
        rejectedCount = rejectedCount,
        failedCount = failedCount,
        currentWindowStartUtc = windowStartUtc,
        currentWindowEndUtc = windowEndUtc,
        currentEventCount = eventCount,
        currentSummaryCount = summaryCount,
        currentAppMetadataCount = appMetadataCount,
        lastBatchId = batchId,
        lastBatchStatus = if (failedCount == 0) "completed" else "completed-with-errors"
    )
}

private fun MobileSyncState.merge(other: MobileSyncState): MobileSyncState {
    return copy(
        acceptedCount = acceptedCount + other.acceptedCount,
        skippedCount = skippedCount + other.skippedCount,
        rejectedCount = rejectedCount + other.rejectedCount,
        failedCount = failedCount + other.failedCount,
        lastError = other.lastError ?: lastError,
        lastErrorDetail = other.lastErrorDetail ?: lastErrorDetail,
        lastBatchId = other.lastBatchId ?: lastBatchId,
        lastBatchStatus = other.lastBatchStatus ?: lastBatchStatus
    )
}

internal fun MobileUsageEventEntity.toDto(clientItemKey: String = id.toString()): MobileUsageEventDto {
    return MobileUsageEventDto(
        packageName,
        eventName,
        iso(eventTimeUtc),
        className,
        iso(collectedAtUtc),
        rawJson,
        clientItemKey
    )
}

internal fun MobileUsageSummaryEntity.toDto(clientItemKey: String = id.toString()): MobileUsageSummaryDto {
    return MobileUsageSummaryDto(
        packageName,
        iso(windowStartUtc),
        iso(windowEndUtc),
        totalTimeForegroundMs,
        iso(lastTimeUsedUtc),
        source.replace('_', '-'),
        rawJson,
        clientItemKey
    )
}

internal fun MobileAppMetadataEntity.toDto(): MobileAppMetadataDto {
    val categoryName = androidCategoryName(category)
    return MobileAppMetadataDto(
        packageName,
        label,
        versionName,
        versionCode,
        isSystemApp,
        categoryName,
        installerPackageName,
        iso(firstInstallTimeUtc),
        iso(lastUpdateTimeUtc),
        mergeCategoryName(rawJson, categoryName),
        iso(collectedAtUtc),
        "$packageName@$versionCode"
    )
}

private fun androidCategoryName(category: Int?): String? {
    if (category == null) return null
    return when (category) {
        ApplicationInfo.CATEGORY_GAME -> "game"
        ApplicationInfo.CATEGORY_AUDIO -> "audio"
        ApplicationInfo.CATEGORY_VIDEO -> "video"
        ApplicationInfo.CATEGORY_IMAGE -> "camera"
        ApplicationInfo.CATEGORY_SOCIAL -> "social"
        ApplicationInfo.CATEGORY_NEWS -> "news"
        ApplicationInfo.CATEGORY_MAPS -> "maps"
        ApplicationInfo.CATEGORY_PRODUCTIVITY -> "productivity"
        else -> null
    }
}

private fun mergeCategoryName(rawJson: String, categoryName: String?): String {
    if (categoryName.isNullOrBlank()) return rawJson
    return try {
        JSONObject(rawJson)
            .put("categoryName", categoryName)
            .toString()
    } catch (_: Exception) {
        JSONObject()
            .put("categoryName", categoryName)
            .toString()
    }
}

private fun packageNames(
    events: List<MobileUsageEventEntity>,
    summaries: List<MobileUsageSummaryEntity>
): Set<String> {
    return (events.map { it.packageName } + summaries.map { it.packageName })
        .filter { it.isNotBlank() }
        .toSet()
}

private fun nowIso(): String = iso(System.currentTimeMillis())

private fun iso(epochMillis: Long): String {
    return Instant.ofEpochMilli(epochMillis).toString()
}

private fun parseIsoMillis(value: String): Long {
    return Instant.parse(value).toEpochMilli()
}
