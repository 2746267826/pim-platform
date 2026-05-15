package com.pim.app.ui.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pim.core.auth.TokenManager
import com.pim.core.models.*
import com.pim.core.network.ApiService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class LoginUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val isLoggedIn: Boolean = false,
    val isRegisterMode: Boolean = false,
    val username: String = "",
    val password: String = "",
    val email: String = "",
    val displayName: String = ""
)

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val api: ApiService,
    private val tokenManager: TokenManager
) : ViewModel() {

    private val _state = MutableStateFlow(LoginUiState())
    val state: StateFlow<LoginUiState> = _state.asStateFlow()

    fun updateUsername(value: String) { _state.value = _state.value.copy(username = value) }
    fun updatePassword(value: String) { _state.value = _state.value.copy(password = value) }
    fun updateEmail(value: String) { _state.value = _state.value.copy(email = value) }
    fun updateDisplayName(value: String) { _state.value = _state.value.copy(displayName = value) }
    fun toggleMode() {
        _state.value = _state.value.copy(
            isRegisterMode = !_state.value.isRegisterMode, error = null
        )
    }

    fun login() {
        if (_state.value.username.isBlank() || _state.value.password.isBlank()) {
            _state.value = _state.value.copy(error = "请填写用户名和密码")
            return
        }
        _state.value = _state.value.copy(isLoading = true, error = null)
        viewModelScope.launch {
            try {
                val res = api.login(LoginRequest(
                    username = _state.value.username,
                    password = _state.value.password
                ))
                val data = res.data
                if (res.code == 0 && data != null) {
                    tokenManager.saveTokens(data.accessToken, data.refreshToken)
                    _state.value = _state.value.copy(isLoading = false, isLoggedIn = true)
                } else {
                    _state.value = _state.value.copy(isLoading = false, error = res.message)
                }
            } catch (e: Exception) {
                _state.value = _state.value.copy(isLoading = false, error = "登录失败: ${e.message}")
            }
        }
    }

    fun register() {
        val s = _state.value
        if (s.username.isBlank() || s.password.isBlank() || s.email.isBlank()) {
            _state.value = _state.value.copy(error = "请填写必填字段")
            return
        }
        _state.value = _state.value.copy(isLoading = true, error = null)
        viewModelScope.launch {
            try {
                val res = api.register(RegisterRequest(
                    username = s.username,
                    password = s.password,
                    email = s.email,
                    displayName = s.displayName.ifBlank { null }
                ))
                val data = res.data
                if (res.code == 0 && data != null) {
                    tokenManager.saveTokens(data.accessToken, data.refreshToken)
                    _state.value = _state.value.copy(isLoading = false, isLoggedIn = true)
                } else {
                    _state.value = _state.value.copy(isLoading = false, error = res.message)
                }
            } catch (e: Exception) {
                _state.value = _state.value.copy(isLoading = false, error = "注册失败: ${e.message}")
            }
        }
    }
}
