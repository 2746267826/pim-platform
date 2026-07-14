package com.pim.app.status

import com.pim.app.data.AppDatabase
import com.pim.app.data.MobileDataDao
import com.pim.app.location.service.ForegroundLocationService
import com.pim.app.location.service.ForegroundLocationRuntimeState
import com.pim.app.mobile.logs.StructuredLogRepository
import com.pim.app.mobile.sync.MobileSyncCoordinator
import com.pim.app.mobile.sync.MobileSyncState
import com.pim.app.permissions.PermissionStatusRepository
import com.pim.app.settings.TrackingSettingsStore
import com.pim.core.auth.TokenManager
import com.pim.core.settings.ServerSettingsStore
import com.pim.core.settings.ServerUrlValidator
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn

private data class CoreFacts(
    val queues: QueueStatusSnapshot,
    val diagnostics: DiagnosticSnapshot,
    val syncState: MobileSyncState,
    val runtime: ForegroundLocationRuntimeState
)

private data class ExternalFacts(
    val probeResult: ConnectionProbeResult?,
    val connected: Boolean,
    val workInfos: StatusWorkInfos,
    val permanentRejected: Int,
    val justAccepted: Boolean
)

@Singleton
class StatusCenterRepository @Inject constructor(
    private val permissionStatusRepository: PermissionStatusRepository,
    private val serverSettingsStore: ServerSettingsStore,
    private val tokenManager: TokenManager,
    private val trackingSettingsStore: TrackingSettingsStore,
    private val database: AppDatabase,
    private val syncCoordinator: MobileSyncCoordinator,
    private val refreshSignal: StatusRefreshSignal,
    private val logRepository: StructuredLogRepository,
    private val connectionProbeStore: ConnectionProbeStore,
    private val networkStatusProvider: NetworkStatusProvider,
    private val workInfoStatusProvider: WorkInfoStatusProvider,
    private val acceptedSignal: StatusAcceptedSignal
) {
    private val dao: MobileDataDao = database.mobileDataDao()

    fun observe(): Flow<StatusCenterState> {
        val coreFlow = combine(
            queueSnapshotFlow(),
            diagnosticSnapshotFlow(),
            syncCoordinator.currentState,
            ForegroundLocationService.runtimeState
        ) { queues, diagnostics, syncState, runtime ->
            CoreFacts(queues, diagnostics, syncState, runtime)
        }

        val externalFlow = combine(
            connectionProbeStore.result,
            networkStatusProvider.isConnected,
            workInfoStatusProvider.syncWorkInfos,
            dao.aggregateRejectedCount(),
            acceptedSignal.accepted
        ) { probeResult, connected, workInfos, rejected, justAccepted ->
            ExternalFacts(probeResult, connected, workInfos, rejected, justAccepted)
        }

        return combine(coreFlow, externalFlow) { core, external ->
            val mergedDiagnostics = core.diagnostics.copy(
                lastHeartbeatStatus = core.syncState.heartbeatStatus,
                lastLogMessage = core.diagnostics.lastLogMessage ?: core.syncState.lastError,
                recentLogMessages = core.diagnostics.recentLogMessages.ifEmpty {
                    listOfNotNull(core.syncState.lastError)
                }
            )
            val snapshot = buildSnapshot(core.queues, mergedDiagnostics, core.runtime)

            if (StatusResultMapper.shouldClearAcceptedSignal(
                    external.justAccepted,
                    external.workInfos.immediate
                )
            ) {
                acceptedSignal.clearIfSet()
            }

            StatusResultMapper.buildState(
                snapshot = snapshot,
                syncState = core.syncState,
                workInfos = external.workInfos,
                permanentRejected = external.permanentRejected,
                connected = external.connected,
                probeResult = external.probeResult,
                justAccepted = external.justAccepted
            )
        }.flowOn(Dispatchers.IO)
    }

    fun requestRefresh() {
        refreshSignal.requestRefresh()
    }

    private fun buildSnapshot(
        queues: QueueStatusSnapshot,
        diagnostics: DiagnosticSnapshot,
        runtime: ForegroundLocationRuntimeState
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
                hasAccessToken = !tokenManager.getAccessTokenForServer(baseUrl).isNullOrBlank(),
                isExpired = tokenManager.isExpiredForServer(baseUrl)
            ),
            service = ForegroundServiceSnapshot(
                continuousCollectionEnabled = settings.continuousCollectionEnabled,
                serviceRunning = runtime.isRunning
            ),
            tracking = StatusTrackingMapper.fromRuntime(settings.profile, runtime),
            queues = queues,
            diagnostics = diagnostics
        )
    }

    private fun queueSnapshotFlow(): Flow<QueueStatusSnapshot> {
        return combine(
            combine(
                dao.pendingLocationPointCount(),
                dao.pendingUsageEventCount(),
                dao.pendingUsageSummaryCount()
            ) { loc, events, summaries -> Triple(loc, events, summaries) },
            combine(
                dao.pendingAppMetadataCount(),
                dao.pendingDeviceProfileCount(),
                dao.pendingSyncBatchCount()
            ) { meta, profile, batches -> Triple(meta, profile, batches) }
        ) { (loc, events, summaries), (meta, profile, batches) ->
            QueueStatusSnapshot(
                pendingLocationPoints = loc,
                pendingUsageEvents = events,
                pendingUsageSummaries = summaries,
                pendingAppMetadata = meta,
                pendingLogs = 0,
                pendingDeviceProfile = profile,
                pendingSyncBatches = batches
            )
        }
    }

    private fun diagnosticSnapshotFlow(): Flow<DiagnosticSnapshot> {
        return combine(
            dao.recentDroppedLocationDiagnostics(limit = 1),
            refreshSignal.version
        ) { dropped, _ ->
            val latestDrop = dropped.firstOrNull()
            val logs = logRepository.recent(6)
            val latestLog = logs.firstOrNull()
            DiagnosticSnapshot(
                lastDroppedReason = latestDrop?.reason,
                lastDroppedAtMillis = latestDrop?.recordedAtUtc,
                lastLogMessage = latestLog?.message,
                lastHeartbeatStatus = null,
                recentLogMessages = logs.map { it.message }
            )
        }
    }
}
