package com.pim.app.mobile.sync

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import com.pim.app.data.AppDatabase
import com.pim.app.data.MobileAppMetadataEntity
import com.pim.app.data.MobileDeviceProfileEntity
import com.pim.app.data.MobileSyncStatus
import com.pim.app.data.MobileUsageEventEntity
import com.pim.app.data.MobileUsageSummaryEntity
import com.pim.app.mobile.logs.StructuredLogRepository
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
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
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
    val acceptedCount: Int = 0,
    val skippedCount: Int = 0,
    val rejectedCount: Int = 0,
    val failedCount: Int = 0,
    val lastError: String? = null,
    val pendingQueueCount: Int = 0,
    val gapWindowCount: Int = 0,
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
    private val database: AppDatabase,
    private val logs: StructuredLogRepository,
    private val heartbeatReporter: MobileHeartbeatReporter,
    private val serverSettingsStore: ServerSettingsStore
) {
    private val mobileDataDao = database.mobileDataDao()

    suspend fun syncOnOpen(): MobileSyncState {
        val attemptedAt = nowIso()
        val serverUrl = configuredServerUrl()
        val deviceIdentity = deviceIdentity()
        val hasToken = !tokenManager.getAccessToken().isNullOrBlank()
        val hasUsageAccess = usageAccessChecker.hasUsageAccess()

        if (serverUrl.isBlank()) {
            return finishWithLocalError(
                deviceId = deviceIdentity.deviceId,
                serverUrl = serverUrl,
                usagePermissionGranted = hasUsageAccess,
                attemptedAt = attemptedAt,
                phase = "server-missing",
                message = "Server URL is not configured."
            )
        }

        if (!hasToken) {
            logs.warn("mobile-sync", "Skipping sync because auth token is missing.")
            return state(
                phase = "auth-missing",
                progressText = "Auth token missing; sync skipped.",
                lastError = "Auth token missing",
                lastAttemptedUploadAt = attemptedAt
            )
        }

        if (!hasUsageAccess) {
            logs.warn("mobile-sync", "Skipping usage sync because usage access is missing.")
            val missingPermissionState = state(
                phase = "usage-permission-missing",
                progressText = "Usage access is missing; sync skipped.",
                skippedCount = 1,
                lastError = "Usage access permission missing",
                lastAttemptedUploadAt = attemptedAt
            )
            sendHeartbeat(deviceIdentity.deviceId, serverUrl, false, missingPermissionState)
            return missingPermissionState
        }

        return try {
            logs.info(
                "mobile-sync",
                "Starting mobile sync on app open.",
                mapOf("deviceId" to deviceIdentity.deviceId, "serverUrl" to serverUrl)
            )

            val profile = buildDeviceProfile(deviceIdentity, nowUtc = System.currentTimeMillis())
            mobileDataDao.upsertDeviceProfile(profile)
            registerDevice(deviceIdentity, profile)
            mobileDataDao.updateDeviceProfileSyncStatus(syncStatus = MobileSyncStatus.SYNCED)

            val rangeEndUtc = System.currentTimeMillis()
            val rangeStartUtc = rangeEndUtc - FOURTEEN_DAYS_MS
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
                throw IllegalStateException(gapResponse.message.ifBlank { "Gap check failed." })
            }

            val windows = gapData.windows
            logs.info(
                "mobile-sync",
                "Server gap check returned ${windows.size} windows.",
                mapOf("windowCount" to windows.size)
            )

            var current = state(
                phase = "collecting",
                progressText = "Collecting server-requested windows.",
                gapWindowCount = windows.size,
                lastAttemptedUploadAt = attemptedAt
            )

            for (window in windows) {
                val windowStartUtc = parseIsoMillis(window.windowStartUtc)
                val windowEndUtc = parseIsoMillis(window.windowEndUtc)
                logs.info(
                    "mobile-sync",
                    "Collecting usage for server gap window.",
                    mapOf(
                        "windowStartUtc" to window.windowStartUtc,
                        "windowEndUtc" to window.windowEndUtc,
                        "reason" to window.reason,
                        "sourcePreference" to window.sourcePreference
                    )
                )

                val collection = usageEventCollector.collectUsage(windowStartUtc, windowEndUtc)
                val eventIds = mobileDataDao.insertUsageEvents(collection.events)
                val summaryIds = mobileDataDao.insertUsageSummaries(collection.summaries)
                val packageNames = packageNames(collection.events, collection.summaries)
                val appMetadata = collectAppMetadata(packageNames)
                if (appMetadata.isNotEmpty()) {
                    mobileDataDao.upsertAppMetadata(appMetadata)
                }

                val uploadState = uploadWindow(
                    deviceId = deviceIdentity.deviceId,
                    windowStartUtc = window.windowStartUtc,
                    windowEndUtc = window.windowEndUtc,
                    events = collection.events,
                    summaries = collection.summaries,
                    apps = appMetadata,
                    eventIds = eventIds,
                    summaryIds = summaryIds
                )

                current = current.merge(uploadState)
            }

            val completed = current.copy(
                phase = "completed",
                progressText = "Mobile sync completed.",
                pendingQueueCount = pendingQueueCount(),
                lastSuccessfulUploadAt = if (current.failedCount == 0) nowIso() else current.lastSuccessfulUploadAt
            )
            persistState(completed)
            sendHeartbeat(deviceIdentity.deviceId, serverUrl, true, completed)
            logs.info(
                "mobile-sync",
                "Mobile sync completed.",
                mapOf(
                    "acceptedCount" to completed.acceptedCount,
                    "skippedCount" to completed.skippedCount,
                    "rejectedCount" to completed.rejectedCount,
                    "failedCount" to completed.failedCount
                )
            )
            completed
        } catch (ex: Exception) {
            val failed = state(
                phase = "failed",
                progressText = "Mobile sync failed.",
                failedCount = 1,
                lastError = ex.message ?: ex::class.java.simpleName,
                pendingQueueCount = pendingQueueCount(),
                lastAttemptedUploadAt = attemptedAt
            )
            logs.error("mobile-sync", "Mobile sync failed.", ex)
            persistState(failed)
            sendHeartbeat(deviceIdentity.deviceId, serverUrl, true, failed)
            failed
        }
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
        val request = MobileUsageEventsUploadRequest(
            deviceId,
            "android-${System.currentTimeMillis()}-${windowStartUtc.hashCode()}",
            windowStartUtc,
            windowEndUtc,
            apps.map { it.toDto() },
            events.map { it.toDto() },
            summaries.map { it.toDto() }
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
                failedCount = events.size + summaries.size,
                lastError = message,
                pendingQueueCount = pendingQueueCount()
            )
        }

        markUsageSynced(eventIds, summaryIds, apps)
        logs.info(
            "mobile-sync",
            "Uploaded usage window.",
            mapOf(
                "windowStartUtc" to windowStartUtc,
                "windowEndUtc" to windowEndUtc,
                "acceptedCount" to ingest.acceptedCount,
                "skippedCount" to ingest.skippedCount,
                "rejectedCount" to ingest.rejectedCount,
                "failedCount" to ingest.failedCount
            )
        )

        return ingest.toState()
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

        logs.info("mobile-sync", "Registered Android device.", mapOf("deviceId" to identity.deviceId))
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
            pendingQueueCount = pendingQueueCount(),
            lastAttemptedUploadAt = attemptedAt
        )
        sendHeartbeat(deviceId, serverUrl, usagePermissionGranted, failed)
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
            logs.info("mobile-heartbeat", "Reported Android heartbeat.", mapOf("phase" to state.phase))
        } catch (ex: Exception) {
            logs.error("mobile-heartbeat", "Android heartbeat failed.", ex)
        }
    }

    private suspend fun pendingQueueCount(): Int {
        return mobileDataDao.pendingUsageEventCount().first() +
            mobileDataDao.pendingUsageSummaryCount().first() +
            mobileDataDao.pendingAppMetadataCount().first() +
            mobileDataDao.pendingLocationPointCount().first()
    }

    private fun collectAppMetadata(packageNames: Set<String>): List<MobileAppMetadataEntity> {
        val packageManager = context.packageManager
        val collectedAtUtc = System.currentTimeMillis()
        return packageNames.mapNotNull { packageName ->
            try {
                val packageInfo = packageInfo(packageManager, packageName)
                val appInfo = applicationInfo(packageManager, packageName)
                val label = appInfo.loadLabel(packageManager)?.toString().orEmpty()
                MobileAppMetadataEntity(
                    packageName = packageName,
                    label = label.ifBlank { packageName },
                    versionName = packageInfo.versionName,
                    versionCode = versionCode(packageInfo),
                    firstInstallTimeUtc = packageInfo.firstInstallTime,
                    lastUpdateTimeUtc = packageInfo.lastUpdateTime,
                    isSystemApp = (appInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0,
                    category = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) appInfo.category else null,
                    installerPackageName = installerPackageName(packageManager, packageName),
                    collectedAtUtc = collectedAtUtc,
                    rawJson = appMetadataJson(packageName, packageInfo, appInfo, collectedAtUtc)
                )
            } catch (ex: Exception) {
                null
            }
        }
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
        context.getSharedPreferences("pim_mobile_sync_state", Context.MODE_PRIVATE)
            .edit()
            .putString("phase", state.phase)
            .putString("progress_text", state.progressText)
            .putInt("accepted_count", state.acceptedCount)
            .putInt("skipped_count", state.skippedCount)
            .putInt("rejected_count", state.rejectedCount)
            .putInt("failed_count", state.failedCount)
            .putString("last_error", state.lastError)
            .putInt("pending_queue_count", state.pendingQueueCount)
            .putString("last_attempted_upload_at", state.lastAttemptedUploadAt)
            .putString("last_successful_upload_at", state.lastSuccessfulUploadAt)
            .apply()
    }

    private fun state(
        phase: String,
        progressText: String,
        acceptedCount: Int = 0,
        skippedCount: Int = 0,
        rejectedCount: Int = 0,
        failedCount: Int = 0,
        lastError: String? = null,
        pendingQueueCount: Int = 0,
        gapWindowCount: Int = 0,
        lastAttemptedUploadAt: String? = null,
        lastSuccessfulUploadAt: String? = null
    ): MobileSyncState {
        return MobileSyncState(
            phase = phase,
            progressText = progressText,
            acceptedCount = acceptedCount,
            skippedCount = skippedCount,
            rejectedCount = rejectedCount,
            failedCount = failedCount,
            lastError = lastError,
            pendingQueueCount = pendingQueueCount,
            gapWindowCount = gapWindowCount,
            lastAttemptedUploadAt = lastAttemptedUploadAt,
            lastSuccessfulUploadAt = lastSuccessfulUploadAt
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

    private fun applicationInfo(packageManager: PackageManager, packageName: String): ApplicationInfo {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            packageManager.getApplicationInfo(packageName, PackageManager.ApplicationInfoFlags.of(0))
        } else {
            @Suppress("DEPRECATION")
            packageManager.getApplicationInfo(packageName, 0)
        }
    }

    private fun installerPackageName(packageManager: PackageManager, packageName: String): String? {
        return try {
            @Suppress("DEPRECATION")
            packageManager.getInstallerPackageName(packageName)
        } catch (_: Exception) {
            null
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

    private fun appMetadataJson(
        packageName: String,
        packageInfo: PackageInfo,
        appInfo: ApplicationInfo,
        collectedAtUtc: Long
    ): String {
        return JSONObject()
            .put("packageName", packageName)
            .put("versionName", packageInfo.versionName ?: JSONObject.NULL)
            .put("versionCode", versionCode(packageInfo))
            .put("firstInstallTimeUtc", packageInfo.firstInstallTime)
            .put("lastUpdateTimeUtc", packageInfo.lastUpdateTime)
            .put("isSystemApp", (appInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0)
            .put("category", if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) appInfo.category else JSONObject.NULL)
            .put("collectedAtUtc", collectedAtUtc)
            .toString()
    }

    private fun sha256(value: String): String {
        val bytes = MessageDigest.getInstance("SHA-256").digest(value.toByteArray())
        return bytes.joinToString("") { "%02x".format(Locale.US, it) }
    }

    private data class DeviceIdentity(
        val deviceId: String,
        val androidIdHash: String?
    )

    companion object {
        private const val FOURTEEN_DAYS_MS = 14L * 24L * 60L * 60L * 1000L
    }
}

private fun MobileIngestResponse.toState(): MobileSyncState {
    return MobileSyncState(
        phase = "uploaded",
        progressText = "Usage batch uploaded.",
        acceptedCount = acceptedCount,
        skippedCount = skippedCount,
        rejectedCount = rejectedCount,
        failedCount = failedCount
    )
}

private fun MobileSyncState.merge(other: MobileSyncState): MobileSyncState {
    return copy(
        acceptedCount = acceptedCount + other.acceptedCount,
        skippedCount = skippedCount + other.skippedCount,
        rejectedCount = rejectedCount + other.rejectedCount,
        failedCount = failedCount + other.failedCount,
        lastError = other.lastError ?: lastError
    )
}

private fun MobileUsageEventEntity.toDto(): MobileUsageEventDto {
    return MobileUsageEventDto(
        packageName,
        eventName,
        iso(eventTimeUtc),
        className,
        iso(collectedAtUtc),
        rawJson
    )
}

private fun MobileUsageSummaryEntity.toDto(): MobileUsageSummaryDto {
    return MobileUsageSummaryDto(
        packageName,
        iso(windowStartUtc),
        iso(windowEndUtc),
        totalTimeForegroundMs,
        iso(lastTimeUsedUtc),
        source.replace('_', '-'),
        rawJson
    )
}

private fun MobileAppMetadataEntity.toDto(): MobileAppMetadataDto {
    return MobileAppMetadataDto(
        packageName,
        label,
        versionName,
        versionCode,
        isSystemApp,
        category?.toString(),
        installerPackageName,
        iso(firstInstallTimeUtc),
        iso(lastUpdateTimeUtc),
        rawJson
    )
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
