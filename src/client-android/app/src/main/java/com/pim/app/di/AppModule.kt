package com.pim.app.di

import android.content.Context
import android.content.SharedPreferences
import androidx.room.Room
import com.pim.app.data.AppDatabase
import com.pim.app.data.AppUsageDao
import com.pim.app.data.PimDatabaseMigrations
import com.pim.app.schedule.ScheduleCacheStore
import com.pim.app.settings.TrackingSettingsStore
import com.pim.app.status.ConnectionProbeService
import com.pim.app.status.ConnectionProbeStore
import com.pim.app.status.ProbeTokenSource
import com.pim.core.auth.TokenManager
import com.pim.core.network.applyPimApiTimeouts
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import javax.inject.Qualifier
import javax.inject.Singleton

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class AnonymousProbeClient

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class AuthenticatedProbeClient

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class ConnectionProbePreferences

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase {
        return Room.databaseBuilder(context, AppDatabase::class.java, "pim.db")
            .addMigrations(*PimDatabaseMigrations.ALL)
            .build()
    }

    @Provides
    @Singleton
    fun provideAppUsageDao(db: AppDatabase): AppUsageDao = db.appUsageDao()

    @Provides
    @Singleton
    fun provideTrackingSharedPreferences(@ApplicationContext context: Context): SharedPreferences {
        return context.getSharedPreferences("pim_tracking", Context.MODE_PRIVATE)
    }

    @Provides
    @Singleton
    fun provideTrackingSettingsStore(preferences: SharedPreferences): TrackingSettingsStore {
        return TrackingSettingsStore(preferences)
    }

    @Provides
    @Singleton
    @AnonymousProbeClient
    fun provideAnonymousProbeClient(): OkHttpClient {
        return OkHttpClient.Builder()
            .applyPimApiTimeouts()
            .build()
    }

    @Provides
    @Singleton
    @AuthenticatedProbeClient
    fun provideAuthenticatedProbeClient(client: OkHttpClient): OkHttpClient = client

    @Provides
    @Singleton
    fun provideProbeTokenSource(tokenManager: TokenManager): ProbeTokenSource {
        return ProbeTokenSource { serverUrl ->
            tokenManager.getAccessTokenForServer(serverUrl)
        }
    }

    @Provides
    @Singleton
    @ConnectionProbePreferences
    fun provideConnectionProbePreferences(
        @ApplicationContext context: Context
    ): SharedPreferences {
        return context.getSharedPreferences("pim_connection_probe", Context.MODE_PRIVATE)
    }

    @Provides
    @Singleton
    fun provideConnectionProbeStore(
        @ConnectionProbePreferences preferences: SharedPreferences,
        json: Json
    ): ConnectionProbeStore {
        return ConnectionProbeStore(preferences, json)
    }

    @Provides
    @Singleton
    fun provideConnectionProbeService(
        @AnonymousProbeClient anonymousClient: OkHttpClient,
        @AuthenticatedProbeClient authenticatedClient: OkHttpClient,
        tokenSource: ProbeTokenSource
    ): ConnectionProbeService {
        return ConnectionProbeService(anonymousClient, authenticatedClient, tokenSource)
    }

    @Provides
    @Singleton
    fun provideScheduleCacheStore(
        @ApplicationContext context: Context,
        json: Json
    ): ScheduleCacheStore {
        return ScheduleCacheStore(context, json)
    }

}
