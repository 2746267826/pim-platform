package com.pim.core.di

import android.content.Context
import com.pim.core.auth.AuthRefreshOperation
import com.pim.core.auth.AuthSessionStore
import com.pim.core.auth.ServerBoundLoginTransport
import com.pim.core.auth.TokenManager
import com.pim.core.network.ApiClientProvider
import com.pim.core.network.ApiService
import com.pim.core.network.AuthInterceptor
import com.pim.core.network.AuthRefreshCoordinator
import com.pim.core.network.RetrofitAuthRefreshOperation
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
    fun provideAuthSessionStore(tokenManager: TokenManager): AuthSessionStore {
        return tokenManager
    }

    @Provides
    @Singleton
    fun provideServerBoundLoginTransport(
        apiClientProvider: dagger.Lazy<ApiClientProvider>
    ): ServerBoundLoginTransport {
        return ServerBoundLoginTransport { serverIdentity, request ->
            apiClientProvider.get()
                .refreshApiServiceForServer(serverIdentity)
                .login(request)
        }
    }

    @Provides
    @Singleton
    fun provideJson(): Json = Json { ignoreUnknownKeys = true }

    @Provides
    @Singleton
    fun provideAuthRefreshOperation(
        apiClientProvider: dagger.Lazy<ApiClientProvider>
    ): AuthRefreshOperation {
        return RetrofitAuthRefreshOperation(
            refreshCall = { serverIdentity, request ->
                apiClientProvider.get()
                    .refreshApiServiceForServer(serverIdentity)
                    .refresh(request)
            }
        )
    }

    @Provides
    @Singleton
    fun provideAuthRefreshCoordinator(
        sessionStore: AuthSessionStore,
        refreshOperation: AuthRefreshOperation
    ): AuthRefreshCoordinator {
        return AuthRefreshCoordinator(sessionStore, refreshOperation)
    }

    @Provides
    @Singleton
    fun provideOkHttpClient(
        sessionStore: AuthSessionStore,
        refreshCoordinator: AuthRefreshCoordinator
    ): OkHttpClient {
        return OkHttpClient.Builder()
            .applyPimApiTimeouts()
            .addInterceptor(AuthInterceptor(sessionStore, refreshCoordinator))
            .build()
    }

    @Provides
    @Singleton
    fun provideApiService(apiClientProvider: ApiClientProvider): ApiService {
        return apiClientProvider.dynamicApiService()
    }
}
