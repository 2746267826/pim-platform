package com.pim.core.network

import com.pim.core.auth.AuthMode
import com.pim.core.auth.AuthSessionStore
import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody

class AuthInterceptor(
    private val sessionStore: AuthSessionStore,
    private val refreshCoordinator: AuthRefreshCoordinator
) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val original = chain.request()
        val authMode = original.tag(AuthMode::class.java) ?: AuthMode.Required
        if (authMode == AuthMode.Anonymous) {
            return chain.proceed(
                original.newBuilder()
                    .removeHeader(AUTHORIZATION)
                    .build()
            )
        }

        runBlocking { refreshCoordinator.refreshIfExpired() }
        val firstAccessToken = sessionStore.accessToken().nonblank()
        val firstResponse = chain.proceed(original.withAccessToken(firstAccessToken))
        if (firstResponse.code != 401) return firstResponse

        val unauthorizedResponse = firstResponse.newBuilder()
            .body(ByteArray(0).toResponseBody(firstResponse.body?.contentType()))
            .build()
        firstResponse.close()
        val refreshed = runBlocking {
            refreshCoordinator.refreshAfterUnauthorized(firstAccessToken)
        }
        if (!refreshed) return unauthorizedResponse

        unauthorizedResponse.close()
        return chain.proceed(original.withAccessToken(sessionStore.accessToken().nonblank()))
    }

    private fun okhttp3.Request.withAccessToken(accessToken: String?): okhttp3.Request {
        return newBuilder()
            .removeHeader(AUTHORIZATION)
            .apply {
                if (accessToken != null) header(AUTHORIZATION, "Bearer $accessToken")
            }
            .build()
    }

    private fun String?.nonblank(): String? = this?.takeIf(String::isNotBlank)

    private companion object {
        const val AUTHORIZATION = "Authorization"
    }
}
