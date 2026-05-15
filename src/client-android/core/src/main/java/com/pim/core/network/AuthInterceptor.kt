package com.pim.core.network

import com.pim.core.auth.TokenManager
import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.Response

class AuthInterceptor(
    private val tokenManager: TokenManager,
    private val onTokenExpired: suspend () -> Boolean
) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val original = chain.request()
        if (tokenManager.isExpired()) {
            val refreshed = runBlocking { onTokenExpired() }
            if (!refreshed) return chain.proceed(original)
        }
        val request = original.newBuilder()
            .header("Authorization", "Bearer ${tokenManager.getAccessToken()}")
            .build()
        return chain.proceed(request)
    }
}
