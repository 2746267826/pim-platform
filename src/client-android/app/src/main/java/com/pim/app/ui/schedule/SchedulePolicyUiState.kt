package com.pim.app.ui.schedule

import com.pim.app.location.policy.ScheduleWindow
import com.pim.app.location.service.ForegroundLocationRuntimeState
import com.pim.app.schedule.ScheduleCacheFreshness
import com.pim.app.schedule.ScheduleCacheSnapshot
import com.pim.app.schedule.ScheduleRefreshErrorKind
import com.pim.app.schedule.ScheduleWindowSelector
import com.pim.app.settings.TrackingSettings
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

sealed interface SchedulePolicyUiState {
    data object Loading : SchedulePolicyUiState
    data class Content(val content: ScheduleContentModel) : SchedulePolicyUiState
    data class Empty(val content: ScheduleContentModel) : SchedulePolicyUiState
    data class StaleContent(val content: ScheduleContentModel) : SchedulePolicyUiState
    data class Error(
        val content: ScheduleContentModel,
        val errorKind: ScheduleRefreshErrorKind,
        val message: String
    ) : SchedulePolicyUiState
}

data class ScheduleContentModel(
    val currentEvent: ScheduleWindow?,
    val nextEvent: ScheduleWindow?,
    val windowsByDate: Map<LocalDate, List<ScheduleWindow>>,
    val lastSuccessAtMillis: Long?,
    val lastAttemptAtMillis: Long?,
    val isRefreshing: Boolean,
    val policySummary: PolicySummary
)

data class PolicySummary(
    val mode: String,
    val reason: String?,
    val requestIntervalMillis: Long?,
    val recoveryThresholdMeters: Double
)

object SchedulePolicyMapper {

    fun stateFor(
        snapshot: ScheduleCacheSnapshot,
        runtimeState: ForegroundLocationRuntimeState,
        settings: TrackingSettings,
        refreshing: Boolean,
        nowMillis: Long,
        zoneId: ZoneId
    ): SchedulePolicyUiState {
        return when (snapshot.freshness) {
            ScheduleCacheFreshness.Missing -> {
                if (snapshot.lastError != null) {
                    SchedulePolicyUiState.Error(
                        content = ScheduleContentModel(
                            currentEvent = null,
                            nextEvent = null,
                            windowsByDate = emptyMap(),
                            lastSuccessAtMillis = snapshot.lastSuccessAtMillis,
                            lastAttemptAtMillis = snapshot.lastAttemptAtMillis,
                            isRefreshing = refreshing,
                            policySummary = PolicySummary(
                                mode = runtimeState.currentPolicyMode,
                                reason = runtimeState.currentPolicyReason,
                                requestIntervalMillis = runtimeState.requestIntervalMillis,
                                recoveryThresholdMeters = settings.scheduleRecoveryThresholdMeters
                            )
                        ),
                        errorKind = snapshot.errorKind ?: ScheduleRefreshErrorKind.Server,
                        message = snapshot.lastError
                    )
                } else {
                    SchedulePolicyUiState.Loading
                }
            }
            ScheduleCacheFreshness.Fresh -> {
                if (snapshot.windows.isEmpty()) {
                    SchedulePolicyUiState.Empty(
                        content = ScheduleContentModel(
                            currentEvent = null,
                            nextEvent = null,
                            windowsByDate = emptyMap(),
                            lastSuccessAtMillis = snapshot.lastSuccessAtMillis,
                            lastAttemptAtMillis = snapshot.lastAttemptAtMillis,
                            isRefreshing = refreshing,
                            policySummary = PolicySummary(
                                mode = runtimeState.currentPolicyMode,
                                reason = runtimeState.currentPolicyReason,
                                requestIntervalMillis = runtimeState.requestIntervalMillis,
                                recoveryThresholdMeters = settings.scheduleRecoveryThresholdMeters
                            )
                        )
                    )
                } else {
                    SchedulePolicyUiState.Content(
                        buildContentModel(snapshot, runtimeState, settings, refreshing, nowMillis, zoneId)
                    )
                }
            }
            ScheduleCacheFreshness.Stale -> {
                SchedulePolicyUiState.StaleContent(
                    buildContentModel(snapshot, runtimeState, settings, refreshing, nowMillis, zoneId)
                )
            }
        }
    }

    private fun buildContentModel(
        snapshot: ScheduleCacheSnapshot,
        runtimeState: ForegroundLocationRuntimeState,
        settings: TrackingSettings,
        refreshing: Boolean,
        nowMillis: Long,
        zoneId: ZoneId
    ): ScheduleContentModel {
        val sortedWindows = snapshot.windows
        val currentEvent = ScheduleWindowSelector.current(sortedWindows, nowMillis)
        val upcoming = ScheduleWindowSelector.upcoming(sortedWindows, nowMillis, limit = 1)
        val nextEvent = upcoming.firstOrNull()
        val windowsByDate = sortedWindows.groupBy { window ->
            Instant.ofEpochMilli(window.startsAtMillis).atZone(zoneId).toLocalDate()
        }.toSortedMap()
        return ScheduleContentModel(
            currentEvent = currentEvent,
            nextEvent = nextEvent,
            windowsByDate = windowsByDate,
            lastSuccessAtMillis = snapshot.lastSuccessAtMillis,
            lastAttemptAtMillis = snapshot.lastAttemptAtMillis,
            isRefreshing = refreshing,
            policySummary = PolicySummary(
                mode = runtimeState.currentPolicyMode,
                reason = runtimeState.currentPolicyReason,
                requestIntervalMillis = runtimeState.requestIntervalMillis,
                recoveryThresholdMeters = settings.scheduleRecoveryThresholdMeters
            )
        )
    }
}
