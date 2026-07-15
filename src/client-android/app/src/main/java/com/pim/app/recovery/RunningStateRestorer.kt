package com.pim.app.recovery

import com.pim.app.location.service.ForegroundLocationController
import com.pim.app.location.service.ForegroundLocationService
import com.pim.app.mobile.logs.StructuredLogRepository
import com.pim.app.mobile.sync.MobileSyncScheduler
import com.pim.app.permissions.PermissionStatusRepository
import com.pim.app.settings.TrackingSettingsStore
import kotlinx.coroutines.CancellationException
import javax.inject.Inject
import javax.inject.Singleton

data class RunningStateRecoveryResult(
    val syncScheduled: Boolean,
    val collectionState: CollectionState,
    val detail: String? = null,
    val missingPermissions: List<String> = emptyList()
)

enum class CollectionState {
    Disabled,
    Blocked,
    AlreadyRunning,
    StartRequested,
    Failed
}

@Singleton
class RunningStateRestorer internal constructor(
    private val trackingSettingsStore: TrackingSettingsStore,
    private val permissionStatusRepository: PermissionStatusRepository,
    private val structuredLogRepository: StructuredLogRepository,
    private val cancelLegacySyncWork: () -> Unit,
    private val ensurePeriodicSync: () -> Unit,
    private val isServiceRunning: () -> Boolean,
    private val startCollection: () -> Unit
) {
    @Inject
    constructor(
        mobileSyncScheduler: MobileSyncScheduler,
        trackingSettingsStore: TrackingSettingsStore,
        permissionStatusRepository: PermissionStatusRepository,
        structuredLogRepository: StructuredLogRepository,
        foregroundLocationController: ForegroundLocationController
    ) : this(
        trackingSettingsStore,
        permissionStatusRepository,
        structuredLogRepository,
        cancelLegacySyncWork = { mobileSyncScheduler.cancelOldWork() },
        ensurePeriodicSync = { mobileSyncScheduler.ensurePeriodic() },
        isServiceRunning = { ForegroundLocationService.isRunning() },
        startCollection = { foregroundLocationController.start() }
    )

    suspend fun ensureRunningState(): RunningStateRecoveryResult {
        try {
            cancelLegacySyncWork()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            structuredLogRepository.error(
                "running-state-recovery",
                "取消历史同步失败",
                e
            )
        }

        val syncScheduled = try {
            ensurePeriodicSync()
            true
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            structuredLogRepository.error(
                "running-state-recovery",
                "定时同步调度失败",
                e
            )
            false
        }

        val settings = try {
            trackingSettingsStore.read()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            structuredLogRepository.error(
                "running-state-recovery",
                "读取设置失败",
                e
            )
            return RunningStateRecoveryResult(syncScheduled, CollectionState.Failed, "settings-read-failed")
        }

        if (!settings.continuousCollectionEnabled) {
            return RunningStateRecoveryResult(syncScheduled, CollectionState.Disabled)
        }

        val permissions = try {
            permissionStatusRepository.snapshot()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            structuredLogRepository.error(
                "running-state-recovery",
                "读取权限状态失败",
                e
            )
            return RunningStateRecoveryResult(syncScheduled, CollectionState.Failed, "permission-read-failed")
        }

        val missingHard = mutableListOf<String>()
        if (!permissions.notificationGranted) missingHard.add("notification")
        if (!permissions.preciseLocationGranted) missingHard.add("precise_location")
        if (!permissions.backgroundLocationGranted) missingHard.add("background_location")

        if (missingHard.isNotEmpty()) {
            structuredLogRepository.warn(
                "running-state-recovery",
                "缺少必需权限，无法启动采集",
                details = mapOf("missingPermissions" to missingHard)
            )
            return RunningStateRecoveryResult(
                syncScheduled = syncScheduled,
                collectionState = CollectionState.Blocked,
                detail = "missing-hard-permissions",
                missingPermissions = missingHard
            )
        }

        val isRunning = try {
            isServiceRunning()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            structuredLogRepository.error(
                "running-state-recovery",
                "检查服务运行状态失败",
                e
            )
            return RunningStateRecoveryResult(syncScheduled, CollectionState.Failed, "service-state-read-failed")
        }

        if (isRunning) {
            return RunningStateRecoveryResult(syncScheduled, CollectionState.AlreadyRunning)
        }

        return try {
            startCollection()
            RunningStateRecoveryResult(syncScheduled, CollectionState.StartRequested)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            structuredLogRepository.error(
                "running-state-recovery",
                "启动采集服务失败",
                e
            )
            RunningStateRecoveryResult(syncScheduled, CollectionState.Failed, "service-start-failed")
        }
    }
}
