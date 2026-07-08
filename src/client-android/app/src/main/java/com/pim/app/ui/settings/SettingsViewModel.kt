package com.pim.app.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pim.core.auth.TokenManager
import com.pim.core.models.LoginRequest
import com.pim.core.network.ApiClientProvider
import com.pim.core.settings.ServerSettingsStore
import com.pim.core.settings.ServerUrlValidator
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
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
    val isBusy: Boolean = false
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val serverSettingsStore: ServerSettingsStore,
    private val tokenManager: TokenManager,
    private val apiClientProvider: ApiClientProvider
) : ViewModel() {
    private val _state = MutableStateFlow(SettingsUiState())
    val state: StateFlow<SettingsUiState> = _state.asStateFlow()

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
                    isLoggedIn = !tokenManager.getAccessToken().isNullOrBlank()
                )
            }
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

    fun saveApiAddress() {
        val validation = ServerUrlValidator.validate(state.value.apiAddress)
        if (!validation.isValid) {
            _state.update {
                it.copy(
                    apiWarnings = validation.warnings,
                    apiError = validation.reasonCode,
                    apiStatus = "API 地址无效，无法保存。"
                )
            }
            return
        }

        val normalized = serverSettingsStore.setBaseUrl(validation.normalizedUrl)
        _state.update {
            it.copy(
                apiAddress = normalized,
                apiWarnings = validation.warnings,
                apiError = null,
                apiStatus = "API 地址已保存。"
            )
        }
    }

    fun testConnection() {
        val validation = ServerUrlValidator.validate(state.value.apiAddress)
        if (!validation.isValid) {
            _state.update { it.copy(apiError = validation.reasonCode, apiStatus = "请先输入有效的 API 地址。") }
            return
        }
        saveApiAddress()
        _state.update { it.copy(apiStatus = "API 地址格式可用。登录后会使用该地址连接服务器。") }
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

        saveApiAddress()
        viewModelScope.launch {
            _state.update { it.copy(isBusy = true, loginStatus = "正在登录...") }
            runCatching {
                val response = apiClientProvider.refreshApiService().login(
                    LoginRequest(username.trim(), password)
                )
                val auth = response.data
                if (response.code != 0 || auth == null) {
                    error(response.message.ifBlank { "登录失败。" })
                }
                tokenManager.saveTokens(auth.accessToken, auth.refreshToken)
            }.fold(
                onSuccess = {
                    _state.update {
                        it.copy(
                            isBusy = false,
                            isLoggedIn = true,
                            loginStatus = "登录成功。"
                        )
                    }
                },
                onFailure = { error ->
                    _state.update {
                        it.copy(
                            isBusy = false,
                            isLoggedIn = false,
                            loginStatus = "登录失败：${error.message ?: "未知错误"}"
                        )
                    }
                }
            )
        }
    }

    fun logout() {
        tokenManager.clear()
        _state.update {
            it.copy(
                isLoggedIn = false,
                loginStatus = "已退出登录。"
            )
        }
    }
}
