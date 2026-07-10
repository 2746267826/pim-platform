package com.pim.core.network

import com.pim.core.auth.AuthRefreshOperation
import com.pim.core.models.ApiResponse
import com.pim.core.models.AuthResponse
import com.pim.core.models.RefreshRequest
import retrofit2.HttpException
import retrofit2.Response

class RetrofitAuthRefreshOperation(
    private val refreshCall: suspend (RefreshRequest) -> Response<ApiResponse<AuthResponse>>,
    private val saveTokens: (AuthResponse) -> Unit
) : AuthRefreshOperation {
    override suspend fun refresh(refreshToken: String): Boolean {
        val response = refreshCall(RefreshRequest(refreshToken))
        if (response.code() == 401) return false
        if (!response.isSuccessful) throw HttpException(response)

        val envelope = response.body() ?: return false
        val auth = envelope.data ?: return false
        if (envelope.code != 0) return false

        saveTokens(auth)
        return true
    }
}
