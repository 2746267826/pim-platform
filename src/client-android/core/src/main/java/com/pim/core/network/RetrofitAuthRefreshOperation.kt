package com.pim.core.network

import com.pim.core.auth.AuthRefreshOperation
import com.pim.core.auth.AuthRefreshResult
import com.pim.core.auth.AuthTokens
import com.pim.core.models.ApiResponse
import com.pim.core.models.AuthResponse
import com.pim.core.models.RefreshRequest
import retrofit2.HttpException
import retrofit2.Response
import java.time.Instant

class RetrofitAuthRefreshOperation(
    private val refreshCall: suspend (
        serverIdentity: String,
        request: RefreshRequest
    ) -> Response<ApiResponse<AuthResponse>>,
    private val nowMillis: () -> Long = System::currentTimeMillis
) : AuthRefreshOperation {
    override suspend fun refresh(
        refreshToken: String,
        serverIdentity: String
    ): AuthRefreshResult {
        val response = refreshCall(serverIdentity, RefreshRequest(refreshToken))
        if (response.code() == 401) return AuthRefreshResult.Rejected
        if (!response.isSuccessful) throw HttpException(response)

        val envelope = response.body() ?: return AuthRefreshResult.Rejected
        val auth = envelope.data ?: return AuthRefreshResult.Rejected
        if (envelope.code != 0) return AuthRefreshResult.Rejected
        val expiresAtUtcMillis = runCatching { Instant.parse(auth.expiresAt).toEpochMilli() }
            .getOrNull()
            ?: return AuthRefreshResult.Rejected
        if (
            auth.accessToken.isBlank() ||
            auth.refreshToken.isBlank() ||
            expiresAtUtcMillis <= nowMillis()
        ) {
            return AuthRefreshResult.Rejected
        }

        return AuthRefreshResult.Success(
            AuthTokens(auth.accessToken, auth.refreshToken, expiresAtUtcMillis)
        )
    }
}
