package com.pim.app.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pim.app.location.service.ForegroundLocationController
import com.pim.app.mobile.sync.MobileSyncScheduler
import com.pim.app.permissions.PermissionStatusRepository
import com.pim.app.settings.TrackingSettings
import com.pim.app.settings.TrackingSettingsStore
import com.pim.app.status.ConnectionProbeOutcome
import com.pim.app.status.ConnectionProbeResult
import com.pim.app.status.ConnectionProbeService
import com.pim.app.status.ConnectionProbeStore
import com.pim.app.status.probeRefreshDelayMillis
import com.pim.app.status.PermissionStatusSnapshot
import com.pim.core.auth.ServerBoundLoginCoordinator
import com.pim.core.auth.ServerBoundLoginResult
import com.pim.core.auth.TokenManager
import com.pim.core.settings.ServerSettingsStore
import com.pim.core.settings.ServerUrlValidator
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SettingsUiState(
    val apiAddress: String = "",
    val apiWarnings: Set<String> = emptySet(),
    val apiError: String? = null,
    val apiStatus: String? = null,
    val isLoggedIn: Boolean = false,
    val loginStatus: String? = null,
    val continuousCollectionEnabled: Boolean = false,
    val collectionStatus: String? = null,
    val isBusy: Boolean = false,
    val trackingProfile: String = TrackingSettings.defaults().profile,
    val normalMinText: String = (TrackingSettings.defaults().normalIntervalMillis / 60_000.0).toDisplayNumber(),
    val scheduleMinText: String = (TrackingSettings.defaults().scheduleLowFrequencyIntervalMillis / 60_000.0).toDisplayNumber(),
    val movementSecText: String = (TrackingSettings.defaults().movementIntervalMillis / 1_000.0).toDisplayNumber(),
    val recoveryMetersText: String = TrackingSettings.defaults().scheduleRecoveryThresholdMeters.toFloat().toDisplayNumber(),
    val accuracyMetersText: String = TrackingSettings.defaults().maxUploadAccuracyMetersExclusive.toDisplayNumber(),
    val altitudeSecText: String = (TrackingSettings.defaults().altitudeWaitTimeoutMillis / 1_000.0).toDisplayNumber(),
    val advancedErrors: Map<String, String> = emptyMap(),
    val syncOnUnmeteredOnly: Boolean = false,
    val verboseLoggingEnabled: Boolean = false,
    val verboseLoggingUntilUtcMillis: Long? = null,
    val logRetentionDays: Int = 7,
    val operationalStatus: String? = null,
    val permissions: PermissionStatusSnapshot = PermissionStatusSnapshot(false, false, false, false, false, false)
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val serverSettingsStore: ServerSettingsStore,
    private val tokenManager: TokenManager,
    private val serverBoundLoginCoordinator: ServerBoundLoginCoordinator,
    private val trackingSettingsStore: TrackingSettingsStore,
    private val foregroundLocationController: ForegroundLocationController,
    private val permissionStatusRepository: PermissionStatusRepository,
    private val connectionProbeService: ConnectionProbeService,
    private val connectionProbeStore: ConnectionProbeStore,
    private val mobileSyncScheduler: MobileSyncScheduler
) : ViewModel() {
    private val _state = MutableStateFlow(SettingsUiState())
    val state: StateFlow<SettingsUiState> = _state.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        reloadOperationalState()
        viewModelScope.launch {
            val address = serverSettingsStore.getBaseUrl()
            val validation = ServerUrlValidator.validate(address)
            _state.update {
                it.copy(
                    apiAddress = address,
                    apiWarnings = validation.warnings,
                    apiError = validation.reasonCode?.takeUnless { validation.isValid },
                    isLoggedIn = hasCurrentServerSession(),
                    continuousCollectionEnabled = persistedCollectionEnabled(),
                    permissions = permissionStatusRepository.snapshot()
                )
            }
            runConnectionProbe(force = false)
        }
    }

    fun updateApiAddress(value: String) {
        val validation = ServerUrlValidator.validate(value)
        _state.update {
            it.copy(
                apiAddress = value,
                apiWarnings = validation.warnings,
                apiError = validation.reasonCode?.takeUnless { validation.isValid },
                apiStatus = null
            )
        }
    }

    fun saveApiAddress(): Boolean {
        val validation = ServerUrlValidator.validate(state.value.apiAddress)
        if (!validation.isValid) {
            _state.update {
                it.copy(
                    apiWarnings = validation.warnings,
                    apiError = validation.reasonCode,
                    apiStatus = "API 地址无效，无法保存。"
                )
            }
            return false
        }

        runCatching {
            serverSettingsStore.setBaseUrl(validation.normalizedUrl)
        }.getOrElse { error ->
            reloadPersistedServerState(
                apiError = error.message,
                apiStatus = "API 地址保存失败，已重新载入当前配置。"
            )
            return false
        }
        reloadPersistedServerState(
            apiError = null,
            apiStatus = "API 地址已保存。"
        )
        return true
    }

    fun testConnection() {
        val validation = ServerUrlValidator.validate(state.value.apiAddress)
        if (!validation.isValid) {
            _state.update { it.copy(apiError = validation.reasonCode, apiStatus = "请先输入有效的 API 地址。") }
            return
        }
        if (!saveApiAddress()) return
        viewModelScope.launch {
            _state.update {
                it.copy(
                    isBusy = true,
                    apiStatus = "正在测试连接…"
                )
            }
            runConnectionProbe(force = true, finishBusyState = true)
        }
    }

    fun login(username: String, password: String) {
        if (username.isBlank() || password.isBlank()) {
            _state.update { it.copy(loginStatus = "请输入用户名和密码。") }
            return
        }
        val validation = ServerUrlValidator.validate(state.value.apiAddress)
        if (!validation.isValid) {
            _state.update { it.copy(apiError = validation.reasonCode, loginStatus = "请先保存有效的 API 地址。") }
            return
        }

        if (!saveApiAddress()) return
        viewModelScope.launch {
            _state.update { it.copy(isBusy = true, loginStatus = "正在登录...") }
            runCatching {
                when (val result = serverBoundLoginCoordinator.login(username, password)) {
                    ServerBoundLoginResult.Success -> Unit
                    ServerBoundLoginResult.StaleServer -> {
                        error("登录期间服务器地址已更改，请重试。")
                    }
                    ServerBoundLoginResult.SessionSaveFailed -> {
                        error("登录凭据无法安全保存，请重试。")
                    }
                    is ServerBoundLoginResult.Failure -> throw result.error
                }
            }.fold(
                onSuccess = {
                    _state.update {
                        it.copy(
                            isBusy = false,
                            isLoggedIn = hasCurrentServerSession(),
                            loginStatus = "登录成功。",
                            continuousCollectionEnabled = persistedCollectionEnabled()
                        )
                    }
                },
                onFailure = { error ->
                    if (error is CancellationException) throw error
                    _state.update {
                        it.copy(
                            isBusy = false,
                            isLoggedIn = hasCurrentServerSession(),
                            loginStatus = "登录失败：${error.message ?: "未知错误"}"
                        )
                    }
                }
            )
        }
    }

    fun logout() {
        if (!tokenManager.clear()) {
            _state.update {
                it.copy(
                    isLoggedIn = hasCurrentServerSession(),
                    loginStatus = "退出失败：安全存储暂时不可用。"
                )
            }
            return
        }
        val collectionIntent = persistedCollectionEnabled()
        _state.update {
            it.copy(
                isLoggedIn = false,
                loginStatus = "已退出登录。",
                continuousCollectionEnabled = collectionIntent,
                collectionStatus = "已退出登录，持续采集设置保持不变。"
            )
        }
    }

    fun setContinuousCollectionEnabled(enabled: Boolean) {
        if (!enabled) {
            trackingSettingsStore.setContinuousCollectionEnabled(false)
            foregroundLocationController.stop()
            _state.update {
                it.copy(
                    continuousCollectionEnabled = false,
                    collectionStatus = "持续采集已关闭。"
                )
            }
            return
        }

        val validation = ServerUrlValidator.validate(state.value.apiAddress)
        if (!validation.isValid) {
            keepCollectionOff("请先保存有效的 API 地址。") {
                it.copy(apiError = validation.reasonCode)
            }
            return
        }

        val normalized = runCatching {
            serverSettingsStore.setBaseUrl(validation.normalizedUrl)
        }.getOrElse { error ->
            reloadPersistedServerState(
                apiError = error.message,
                apiStatus = "API 地址保存失败，已重新载入当前配置。"
            )
            _state.update {
                it.copy(collectionStatus = "API 地址保存失败，持续采集设置保持不变。")
            }
            return
        }
        reloadPersistedServerState(apiError = null, apiStatus = state.value.apiStatus)

        if (!hasCurrentServerSession()) {
            showCollectionBlocked("请先登录后再开启持续采集。")
            return
        }

        trackingSettingsStore.setContinuousCollectionEnabled(true)

        val missingPermissions = missingCollectionPermissions()
        if (missingPermissions.isNotEmpty()) {
            _state.update {
                it.copy(
                    apiAddress = normalized,
                    apiWarnings = validation.warnings,
                    apiError = null,
                    continuousCollectionEnabled = true,
                    collectionStatus = "缺少权限：${missingPermissions.joinToString("、")}。"
                )
            }
            return
        }

        _state.update {
            it.copy(
                apiAddress = normalized,
                apiWarnings = validation.warnings,
                apiError = null,
                continuousCollectionEnabled = true,
                collectionStatus = "正在启动前台定位服务。"
            )
        }
        runCatching {
            foregroundLocationController.start()
        }.onFailure { error ->
            _state.update {
                it.copy(
                    continuousCollectionEnabled = true,
                    collectionStatus = "启动失败：${error.message ?: "未知错误"}"
                )
            }
        }
    }

    fun applyTrackingPreset(profileId: String) {
        trackingSettingsStore.applyPreset(profileId)
        reloadOperationalState()
        reloadForegroundCollectionIfEnabled()
    }

    fun updateNormalMinText(value: String) {
        _state.update { it.copy(normalMinText = value) }
    }

    fun updateScheduleMinText(value: String) {
        _state.update { it.copy(scheduleMinText = value) }
    }

    fun updateMovementSecText(value: String) {
        _state.update { it.copy(movementSecText = value) }
    }

    fun updateRecoveryMetersText(value: String) {
        _state.update { it.copy(recoveryMetersText = value) }
    }

    fun updateAccuracyMetersText(value: String) {
        _state.update { it.copy(accuracyMetersText = value) }
    }

    fun updateAltitudeSecText(value: String) {
        _state.update { it.copy(altitudeSecText = value) }
    }

    fun saveAdvancedSettings(): Boolean {
        val errors = mutableMapOf<String, String>()

        val normalMin = state.value.normalMinText.toDoubleOrNull()
        val scheduleMin = state.value.scheduleMinText.toDoubleOrNull()
        val movementSec = state.value.movementSecText.toDoubleOrNull()
        val recoveryMeters = state.value.recoveryMetersText.toDoubleOrNull()
        val accuracyMeters = state.value.accuracyMetersText.toDoubleOrNull()
        val altitudeSec = state.value.altitudeSecText.toDoubleOrNull()

        if (normalMin == null || !normalMin.isFinite() || normalMin < 1.0 || normalMin > 15.0) {
            errors["normalInterval"] = "正常间隔需在 1–15 分钟之间。"
        }
        if (scheduleMin == null || !scheduleMin.isFinite() || scheduleMin < 5.0 || scheduleMin > 60.0) {
            errors["scheduleInterval"] = "调度间隔需在 5–60 分钟之间。"
        }
        if (movementSec == null || !movementSec.isFinite() || movementSec < 30.0 || movementSec > 300.0) {
            errors["movementInterval"] = "移动间隔需在 30–300 秒之间。"
        }
        if (recoveryMeters == null || !recoveryMeters.isFinite() || recoveryMeters < 25.0 || recoveryMeters > 500.0) {
            errors["recoveryThreshold"] = "恢复阈值需在 25–500 米之间。"
        }
        if (accuracyMeters == null || !accuracyMeters.isFinite() || accuracyMeters < 10.0 || accuracyMeters > 50.0) {
            errors["accuracy"] = "精度需在 10–50 米之间。"
        }
        if (altitudeSec == null || !altitudeSec.isFinite() || altitudeSec < 0.0 || altitudeSec > 30.0) {
            errors["altitudeWait"] = "海拔等待需在 0–30 秒之间。"
        }

        if (errors.isNotEmpty()) {
            _state.update { it.copy(advancedErrors = errors) }
            return false
        }

        val current = trackingSettingsStore.read()
        trackingSettingsStore.write(
            current.copy(
                profile = "custom",
                normalIntervalMillis = (normalMin!! * 60_000).toLong(),
                scheduleLowFrequencyIntervalMillis = (scheduleMin!! * 60_000).toLong(),
                movementIntervalMillis = (movementSec!! * 1_000).toLong(),
                scheduleRecoveryThresholdMeters = recoveryMeters!!,
                maxUploadAccuracyMetersExclusive = accuracyMeters!!.toFloat(),
                altitudeWaitTimeoutMillis = (altitudeSec!! * 1_000).toLong()
            )
        )
        reloadOperationalState()
        _state.update { it.copy(advancedErrors = emptyMap()) }
        reloadForegroundCollectionIfEnabled()
        return true
    }

    fun setSyncOnUnmeteredOnly(enabled: Boolean) {
        trackingSettingsStore.write(trackingSettingsStore.read().copy(syncOnUnmeteredOnly = enabled))
        mobileSyncScheduler.ensurePeriodic()
        _state.update { it.copy(syncOnUnmeteredOnly = trackingSettingsStore.read().syncOnUnmeteredOnly) }
    }

    fun setVerboseLoggingEnabled(enabled: Boolean) {
        val now = System.currentTimeMillis()
        trackingSettingsStore.setVerboseLoggingEnabled(enabled, now)
        val settings = trackingSettingsStore.read()
        _state.update {
            it.copy(
                verboseLoggingEnabled = enabled,
                verboseLoggingUntilUtcMillis = settings.verboseLoggingUntilUtcMillis
            )
        }
    }

    fun setLogRetentionDays(days: Int) {
        if (days !in setOf(1, 7, 14, 30)) return
        trackingSettingsStore.write(trackingSettingsStore.read().copy(logRetentionDays = days))
        _state.update { it.copy(logRetentionDays = trackingSettingsStore.read().logRetentionDays) }
    }

    fun resetOperationalDefaults() {
        trackingSettingsStore.resetOperationalDefaults()
        foregroundLocationController.stop()
        mobileSyncScheduler.ensurePeriodic()
        reloadOperationalState()
    }

    fun onResume() {
        val snapshot = permissionStatusRepository.snapshot()
        val collectionIntent = persistedCollectionEnabled()
        _state.update {
            it.copy(
                permissions = snapshot,
                continuousCollectionEnabled = collectionIntent
            )
        }

        if (!collectionIntent) return

        val hardPermissions = missingCollectionPermissions(snapshot)

        if (hardPermissions.isNotEmpty()) {
            _state.update {
                it.copy(collectionStatus = "缺少权限：${hardPermissions.joinToString("、")}。")
            }
            return
        }

        runCatching {
            foregroundLocationController.start()
        }.fold(
            onSuccess = {
                _state.update {
                    it.copy(collectionStatus = "已恢复持续采集。")
                }
            },
            onFailure = { error ->
                _state.update {
                    it.copy(
                        continuousCollectionEnabled = true,
                        collectionStatus = "恢复失败：${error.message ?: "未知错误"}"
                    )
                }
            }
        )
    }

    private fun missingCollectionPermissions(snapshot: PermissionStatusSnapshot? = null): List<String> {
        val permissions = snapshot ?: permissionStatusRepository.snapshot()
        return buildList {
            if (!permissions.notificationGranted) add("通知")
            if (!permissions.preciseLocationGranted) add("精确定位")
            if (!permissions.backgroundLocationGranted) add("后台定位")
        }
    }

    private fun keepCollectionOff(
        message: String,
        extraState: (SettingsUiState) -> SettingsUiState = { it }
    ) {
        trackingSettingsStore.setContinuousCollectionEnabled(false)
        foregroundLocationController.stop()
        _state.update {
            extraState(
                it.copy(
                    continuousCollectionEnabled = false,
                    collectionStatus = message
                )
            )
        }
    }

    private fun showCollectionBlocked(message: String) {
        _state.update {
            it.copy(
                continuousCollectionEnabled = persistedCollectionEnabled(),
                collectionStatus = message
            )
        }
    }

    suspend fun refreshConnectionForVisibleScreen(): Long {
        val succeeded = runConnectionProbe(force = false)
        return if (succeeded) {
            millisUntilRefresh()
        } else {
            PROBE_RETRY_MILLIS
        }
    }

    private fun reloadOperationalState() {
        val now = System.currentTimeMillis()
        val verboseEnabled = trackingSettingsStore.isVerboseLoggingEnabled(now)
        val settings = trackingSettingsStore.read()
        _state.update {
            it.copy(
                trackingProfile = settings.profile,
                syncOnUnmeteredOnly = settings.syncOnUnmeteredOnly,
                verboseLoggingEnabled = verboseEnabled,
                verboseLoggingUntilUtcMillis = settings.verboseLoggingUntilUtcMillis,
                logRetentionDays = settings.logRetentionDays,
                continuousCollectionEnabled = settings.continuousCollectionEnabled,
                normalMinText = (settings.normalIntervalMillis / 60_000.0).toDisplayNumber(),
                scheduleMinText = (settings.scheduleLowFrequencyIntervalMillis / 60_000.0).toDisplayNumber(),
                movementSecText = (settings.movementIntervalMillis / 1_000.0).toDisplayNumber(),
                recoveryMetersText = settings.scheduleRecoveryThresholdMeters.toFloat().toDisplayNumber(),
                accuracyMetersText = settings.maxUploadAccuracyMetersExclusive.toDisplayNumber(),
                altitudeSecText = (settings.altitudeWaitTimeoutMillis / 1_000.0).toDisplayNumber()
            )
        }
    }

    private fun reloadForegroundCollectionIfEnabled() {
        if (!trackingSettingsStore.read().continuousCollectionEnabled) return
        runCatching {
            foregroundLocationController.start()
        }.onFailure { error ->
            _state.update {
                it.copy(collectionStatus = "设置已保存，但采集重载失败：${error.message ?: "未知错误"}")
            }
        }
    }

    private suspend fun runConnectionProbe(
        force: Boolean,
        finishBusyState: Boolean = false
    ): Boolean {
        val targetUrl = serverSettingsStore.getBaseUrl()
        val result = runCatching {
            if (force) {
                connectionProbeService.probe(targetUrl).also { result ->
                    if (serverSettingsStore.getBaseUrl() == targetUrl) {
                        connectionProbeStore.save(result)
                    }
                }
            } else {
                val serverIdentity = runCatching {
                    com.pim.core.settings.PimServerEndpoints.from(targetUrl).apiBaseUrl.toString()
                }.getOrNull()
                connectionProbeStore.freshResult(serverIdentity ?: targetUrl, System.currentTimeMillis())
                    ?: connectionProbeService.probe(targetUrl).also { result ->
                        if (serverSettingsStore.getBaseUrl() == targetUrl) {
                            connectionProbeStore.save(result)
                        }
                    }
            }
        }.fold(
            onSuccess = { probeResult ->
                val currentUrl = serverSettingsStore.getBaseUrl()
                if (currentUrl == targetUrl) {
                    _state.update {
                        it.copy(
                            apiStatus = probeResult.statusMessage(),
                            isBusy = if (finishBusyState) false else it.isBusy
                        )
                    }
                } else if (finishBusyState) {
                    _state.update { it.copy(isBusy = false) }
                }
                true
            },
            onFailure = {
                val currentUrl = serverSettingsStore.getBaseUrl()
                if (currentUrl == targetUrl) {
                    _state.update {
                        it.copy(
                            apiStatus = "连接测试失败。",
                            isBusy = if (finishBusyState) false else it.isBusy
                        )
                    }
                } else if (finishBusyState) {
                    _state.update { it.copy(isBusy = false) }
                }
                false
            }
        )
        return result
    }

    private fun millisUntilRefresh(): Long {
        val serverUrl = serverSettingsStore.getBaseUrl()
        val serverIdentity = runCatching {
            com.pim.core.settings.PimServerEndpoints.from(serverUrl).apiBaseUrl.toString()
        }.getOrNull() ?: return ConnectionProbeStore.FRESHNESS_MILLIS
        return probeRefreshDelayMillis(
            result = connectionProbeStore.result.value,
            serverIdentity = serverIdentity,
            nowMillis = System.currentTimeMillis()
        )
    }

    private fun persistedCollectionEnabled(): Boolean {
        return trackingSettingsStore.read().continuousCollectionEnabled
    }

    private fun reloadPersistedServerState(
        apiError: String?,
        apiStatus: String?
    ) {
        val address = serverSettingsStore.getBaseUrl()
        val validation = ServerUrlValidator.validate(address)
        _state.update {
            it.copy(
                apiAddress = address,
                apiWarnings = validation.warnings,
                apiError = apiError ?: validation.reasonCode?.takeUnless { validation.isValid },
                apiStatus = apiStatus,
                isLoggedIn = hasCurrentServerSession(),
                continuousCollectionEnabled = persistedCollectionEnabled()
            )
        }
    }

    private fun hasCurrentServerSession(): Boolean {
        return !tokenManager
            .getAccessTokenForServer(serverSettingsStore.getBaseUrl())
            .isNullOrBlank()
    }
}

private fun Double.toDisplayNumber(): String =
    if (this == toLong().toDouble()) toLong().toString() else toString()

private fun Float.toDisplayNumber(): String =
    if (this == toLong().toFloat()) toLong().toString() else toString()

private const val PROBE_RETRY_MILLIS = 30_000L

private fun ConnectionProbeResult.statusMessage(): String {
    return when (outcome) {
        ConnectionProbeOutcome.Reachable -> "连接成功。"
        ConnectionProbeOutcome.Partial -> safeMessage
            ?: "连接部分可用。"
        ConnectionProbeOutcome.Blocked -> safeMessage
            ?: "无法连接服务器。"
    }
}
