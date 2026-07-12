package com.pim.app.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pim.app.location.service.ForegroundLocationController
import com.pim.app.permissions.PermissionStatusRepository
import com.pim.app.settings.TrackingSettingsStore
import com.pim.app.status.ConnectionProbeOutcome
import com.pim.app.status.ConnectionProbeResult
import com.pim.app.status.ConnectionProbeRunner
import com.pim.core.auth.SecureStorageStatus
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
import java.util.concurrent.atomic.AtomicLong

data class SettingsUiState(
    val apiAddress: String = "",
    val apiWarnings: Set<String> = emptySet(),
    val apiError: String? = null,
    val apiStatus: String? = null,
    val isLoggedIn: Boolean = false,
    val loginStatus: String? = null,
    val continuousCollectionEnabled: Boolean = false,
    val collectionStatus: String? = null,
    val isBusy: Boolean = false
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val serverSettingsStore: ServerSettingsStore,
    private val tokenManager: TokenManager,
    private val serverBoundLoginCoordinator: ServerBoundLoginCoordinator,
    private val trackingSettingsStore: TrackingSettingsStore,
    private val foregroundLocationController: ForegroundLocationController,
    private val permissionStatusRepository: PermissionStatusRepository,
    private val connectionProbeRunner: ConnectionProbeRunner
) : ViewModel() {
    private val _state = MutableStateFlow(SettingsUiState())
    val state: StateFlow<SettingsUiState> = _state.asStateFlow()
    private val probeRequestGeneration = AtomicLong(0L)
    private val activeManualProbeGeneration = AtomicLong(NO_ACTIVE_PROBE)

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            val address = serverSettingsStore.getBaseUrl()
            val validation = ServerUrlValidator.validate(address)
            _state.update {
                it.copy(
                    apiAddress = address,
                    apiWarnings = validation.warnings,
                    apiError = validation.reasonCode?.takeUnless { validation.isValid },
                    isLoggedIn = hasCurrentServerSession(),
                    continuousCollectionEnabled = persistedCollectionEnabled()
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
            probeRequestGeneration.incrementAndGet()
            reloadPersistedServerState(
                apiError = error.message,
                apiStatus = "API 地址保存失败，已重新载入当前配置。"
            )
            return false
        }
        probeRequestGeneration.incrementAndGet()
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
                    apiStatus = "\u6b63\u5728\u6d4b\u8bd5\u8fde\u63a5\u2026"
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
                        error("\u767b\u5f55\u671f\u95f4\u670d\u52a1\u5668\u5730\u5740\u5df2\u66f4\u6539\uff0c\u8bf7\u91cd\u8bd5\u3002")
                    }
                    ServerBoundLoginResult.SessionSaveFailed -> {
                        error("\u767b\u5f55\u51ed\u636e\u65e0\u6cd5\u5b89\u5168\u4fdd\u5b58\uff0c\u8bf7\u91cd\u8bd5\u3002")
                    }
                    is ServerBoundLoginResult.Failure -> throw result.error
                }
            }.fold(
                onSuccess = {
                    _state.update {
                        it.copy(
                            isBusy = false,
                            isLoggedIn = hasCurrentServerSession(),
                            loginStatus = if (tokenManager.storageStatus == SecureStorageStatus.Ephemeral) {
                                "\u767b\u5f55\u6210\u529f\uff0c\u4f46\u5b89\u5168\u5b58\u50a8\u4e0d\u53ef\u7528\uff1b\u5173\u95ed\u5e94\u7528\u540e\u9700\u8981\u91cd\u65b0\u767b\u5f55\u3002"
                            } else {
                                "登录成功。"
                            },
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
            probeRequestGeneration.incrementAndGet()
            reloadPersistedServerState(
                apiError = error.message,
                apiStatus = "API 地址保存失败，已重新载入当前配置。"
            )
            _state.update {
                it.copy(collectionStatus = "API 地址保存失败，持续采集设置保持不变。")
            }
            return
        }
        probeRequestGeneration.incrementAndGet()
        reloadPersistedServerState(apiError = null, apiStatus = state.value.apiStatus)

        if (!hasCurrentServerSession()) {
            showCollectionBlocked("请先登录后再开启持续采集。")
            return
        }

        val missingPermissions = missingCollectionPermissions()
        if (missingPermissions.isNotEmpty()) {
            showCollectionBlocked("缺少权限：${missingPermissions.joinToString("、")}。")
            return
        }

        trackingSettingsStore.setContinuousCollectionEnabled(true)
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
            trackingSettingsStore.setContinuousCollectionEnabled(false)
            _state.update {
                it.copy(
                    continuousCollectionEnabled = false,
                    collectionStatus = "启动失败：${error.message ?: "未知错误"}"
                )
            }
        }
    }

    private fun missingCollectionPermissions(): List<String> {
        val permissions = permissionStatusRepository.snapshot()
        return buildList {
            if (!permissions.notificationGranted) add("通知")
            if (!permissions.preciseLocationGranted) add("精确定位")
            if (!permissions.backgroundLocationGranted) add("后台定位")
            if (!permissions.activityRecognitionGranted) add("活动识别")
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
            connectionProbeRunner.millisUntilRefresh()
        } else {
            PROBE_RETRY_MILLIS
        }
    }

    private suspend fun runConnectionProbe(
        force: Boolean,
        finishBusyState: Boolean = false
    ): Boolean {
        val requestGeneration = if (force) {
            probeRequestGeneration.incrementAndGet()
        } else {
            probeRequestGeneration.get()
        }
        if (finishBusyState) activeManualProbeGeneration.set(requestGeneration)
        return runCatching { connectionProbeRunner.run(force = force) }.fold(
            onSuccess = { probeResult ->
                val isCurrent = probeRequestGeneration.get() == requestGeneration
                val ownsBusyState = finishBusyState && activeManualProbeGeneration.compareAndSet(
                    requestGeneration,
                    NO_ACTIVE_PROBE
                )
                if (isCurrent || ownsBusyState) {
                    _state.update {
                        it.copy(
                            apiStatus = if (isCurrent) probeResult.statusMessage() else it.apiStatus,
                            isBusy = if (ownsBusyState) false else it.isBusy
                        )
                    }
                }
                isCurrent
            },
            onFailure = {
                val isCurrent = probeRequestGeneration.get() == requestGeneration
                val ownsBusyState = finishBusyState && activeManualProbeGeneration.compareAndSet(
                    requestGeneration,
                    NO_ACTIVE_PROBE
                )
                if (isCurrent || ownsBusyState) {
                    _state.update {
                        it.copy(
                            apiStatus = if (isCurrent) {
                                "\u8fde\u63a5\u6d4b\u8bd5\u5931\u8d25\u3002"
                            } else {
                                it.apiStatus
                            },
                            isBusy = if (ownsBusyState) false else it.isBusy
                        )
                    }
                }
                false
            }
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

private const val PROBE_RETRY_MILLIS = 30_000L
private const val NO_ACTIVE_PROBE = -1L

private fun ConnectionProbeResult.statusMessage(): String {
    return when (outcome) {
        ConnectionProbeOutcome.Reachable -> "\u8fde\u63a5\u6210\u529f\u3002"
        ConnectionProbeOutcome.Partial -> safeMessage
            ?: "\u8fde\u63a5\u90e8\u5206\u53ef\u7528\u3002"
        ConnectionProbeOutcome.Blocked -> safeMessage
            ?: "\u65e0\u6cd5\u8fde\u63a5\u670d\u52a1\u5668\u3002"
    }
}
