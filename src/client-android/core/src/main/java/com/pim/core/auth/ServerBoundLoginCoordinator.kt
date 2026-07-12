package com.pim.core.auth

import com.pim.core.models.ApiResponse
import com.pim.core.models.AuthResponse
import com.pim.core.models.LoginRequest
import com.pim.core.settings.PimServerEndpoints
import com.pim.core.settings.ServerSessionCommitResult
import com.pim.core.settings.ServerSettingsStore
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CancellationException

fun interface ServerBoundLoginTransport {
    suspend fun login(
        serverIdentity: String,
        request: LoginRequest
    ): ApiResponse<AuthResponse>
}

sealed interface ServerBoundLoginResult {
    data object Success : ServerBoundLoginResult
    data object StaleServer : ServerBoundLoginResult
    data object SessionSaveFailed : ServerBoundLoginResult
    data class Failure(val error: Throwable) : ServerBoundLoginResult
}

@Singleton
class ServerBoundLoginCoordinator @Inject constructor(
    private val serverSettingsStore: ServerSettingsStore,
    private val tokenManager: TokenManager,
    private val transport: ServerBoundLoginTransport
) {
    suspend fun login(username: String, password: String): ServerBoundLoginResult {
        val serverUrl = serverSettingsStore.getBaseUrl()
        val serverIdentity = runCatching {
            PimServerEndpoints.from(serverUrl).trustedOrigin
        }.getOrElse { failure ->
            return ServerBoundLoginResult.Failure(failure)
        }
        val response = try {
            transport.login(
                serverIdentity,
                LoginRequest(username.trim(), password)
            )
        } catch (failure: CancellationException) {
            throw failure
        } catch (failure: Exception) {
            return ServerBoundLoginResult.Failure(failure)
        }
        val auth = response.data
        if (response.code != 0 || auth == null) {
            return ServerBoundLoginResult.Failure(
                IllegalStateException(response.message.ifBlank { "Login failed" })
            )
        }

        return when (
            serverSettingsStore.commitSessionIfCurrentServer(serverIdentity) {
                tokenManager.saveTokens(
                    auth.accessToken,
                    auth.refreshToken,
                    auth.expiresAt,
                    serverUrl
                )
            }
        ) {
            ServerSessionCommitResult.Committed -> ServerBoundLoginResult.Success
            ServerSessionCommitResult.ServerChanged -> ServerBoundLoginResult.StaleServer
            ServerSessionCommitResult.SaveFailed -> ServerBoundLoginResult.SessionSaveFailed
            ServerSessionCommitResult.InvalidServer -> ServerBoundLoginResult.Failure(
                IllegalStateException("Captured server identity is invalid")
            )
        }
    }
}
