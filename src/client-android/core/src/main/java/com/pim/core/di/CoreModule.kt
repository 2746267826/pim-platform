package com.pim.core.di

import android.content.Context
import com.pim.core.auth.TokenManager
import com.pim.core.models.RefreshRequest
import com.pim.core.network.ApiClientProvider
import com.pim.core.network.ApiService
import com.pim.core.network.AuthInterceptor
import com.pim.core.network.applyPimApiTimeouts
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object CoreModule {

    @Provides
    @Singleton
    fun provideTokenManager(@ApplicationContext context: Context): TokenManager {
        return TokenManager(context)
    }

    @Provides
    @Singleton
    fun provideJson(): Json = Json { ignoreUnknownKeys = true }

    @Provides
    @Singleton
    fun provideOkHttpClient(
        tokenManager: TokenManager,
        apiClientProvider: dagger.Lazy<ApiClientProvider>
    ): OkHttpClient {
        return OkHttpClient.Builder()
            .applyPimApiTimeouts()
            .addInterceptor(AuthInterceptor(tokenManager) {
                val refreshToken = tokenManager.getRefreshToken()
                if (refreshToken.isNullOrBlank()) {
                    false
                } else {
                    runCatching {
                        val response = apiClientProvider.get().refreshApiService().refresh(RefreshRequest(refreshToken))
                        if (response.code == 0 && response.data != null) {
                            tokenManager.saveTokens(response.data.accessToken, response.data.refreshToken)
                            true
                        } else {
                            false
                        }
                    }.getOrDefault(false)
                }
            })
            .build()
    }

    @Provides
    @Singleton
    fun provideApiService(apiClientProvider: ApiClientProvider): ApiService {
        return apiClientProvider.dynamicApiService()
    }
}
