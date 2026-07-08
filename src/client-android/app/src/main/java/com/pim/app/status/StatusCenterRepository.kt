package com.pim.app.status

import com.pim.app.data.AppDatabase
import com.pim.app.data.MobileDataDao
import com.pim.app.location.service.ForegroundLocationService
import com.pim.app.mobile.sync.MobileSyncCoordinator
import com.pim.app.permissions.PermissionStatusRepository
import com.pim.app.settings.TrackingSettingsStore
import com.pim.core.auth.TokenManager
import com.pim.core.settings.ServerSettingsStore
import com.pim.core.settings.ServerUrlValidator
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

@Singleton
class StatusCenterRepository @Inject constructor(
    private val permissionStatusRepository: PermissionStatusRepository,
    private val serverSettingsStore: ServerSettingsStore,
    private val tokenManager: TokenManager,
    private val trackingSettingsStore: TrackingSettingsStore,
    private val database: AppDatabase,
    private val syncCoordinator: MobileSyncCoordinator
) {
    private val dao: MobileDataDao = database.mobileDataDao()

    fun observe(): Flow<StatusCenterState> {
        return combine(
            queueSnapshotFlow(),
            diagnosticSnapshotFlow(),
            syncCoordinator.currentState
        ) { queues, diagnostics, sync ->
            val mergedDiagnostics = diagnostics.copy(
                lastHeartbeatStatus = sync.heartbeatStatus,
                lastLogMessage = diagnostics.lastLogMessage ?: sync.lastError
            )
            val snapshot = buildSnapshot(queues, mergedDiagnostics)
            StatusCenterState(snapshot, StatusIssuePlanner.plan(snapshot))
        }
    }

    fun snapshotNow(
        queues: QueueStatusSnapshot = QueueStatusSnapshot(0, 0, 0, 0, 0, 0),
        diagnostics: DiagnosticSnapshot = DiagnosticSnapshot(null, null, null, null)
    ): StatusCenterState {
        val snapshot = buildSnapshot(queues, diagnostics)
        return StatusCenterState(snapshot, StatusIssuePlanner.plan(snapshot))
    }

    private fun buildSnapshot(
        queues: QueueStatusSnapshot,
        diagnostics: DiagnosticSnapshot
    ): StatusCenterSnapshot {
        val baseUrl = serverSettingsStore.getBaseUrl()
        val validation = ServerUrlValidator.validate(baseUrl)
        val settings = trackingSettingsStore.read()
        return StatusCenterSnapshot(
            permissions = permissionStatusRepository.snapshot(),
            api = ApiConnectionSnapshot(
                address = baseUrl,
                isValid = validation.isValid,
                reasonCode = validation.reasonCode,
                warnings = validation.warnings
            ),
            auth = AuthStatusSnapshot(
                hasAccessToken = !tokenManager.getAccessToken().isNullOrBlank(),
                isExpired = tokenManager.isExpired()
            ),
            service = ForegroundServiceSnapshot(
                continuousCollectionEnabled = settings.continuousCollectionEnabled,
                serviceRunning = ForegroundLocationService.isRunning()
            ),
            tracking = TrackingPolicySnapshot(
                profile = settings.profile,
                currentPolicyMode = "PowerSavingNormal",
                nextExpectedLocationAtMillis = null
            ),
            queues = queues,
            diagnostics = diagnostics
        )
    }

    private fun queueSnapshotFlow(): Flow<QueueStatusSnapshot> {
        return combine(
            listOf(
                dao.pendingLocationPointCount(),
                dao.pendingUsageEventCount(),
                dao.pendingUsageSummaryCount(),
                dao.pendingAppMetadataCount(),
                dao.pendingLogCount(),
                dao.pendingDeviceProfileCount(),
                dao.pendingSyncBatchCount()
            )
        ) { values ->
            QueueStatusSnapshot(
                pendingLocationPoints = values[0],
                pendingUsageEvents = values[1],
                pendingUsageSummaries = values[2],
                pendingAppMetadata = values[3],
                pendingLogs = values[4],
                pendingDeviceProfile = values[5],
                pendingSyncBatches = values[6]
            )
        }
    }

    private fun diagnosticSnapshotFlow(): Flow<DiagnosticSnapshot> {
        return combine(
            dao.recentDroppedLocationDiagnostics(limit = 1),
            dao.recentLogs(limit = 1)
        ) { dropped, logs ->
            val latestDrop = dropped.firstOrNull()
            val latestLog = logs.firstOrNull()
            DiagnosticSnapshot(
                lastDroppedReason = latestDrop?.reason,
                lastDroppedAtMillis = latestDrop?.recordedAtUtc,
                lastLogMessage = latestLog?.message,
                lastHeartbeatStatus = null
            )
        }
    }
}
