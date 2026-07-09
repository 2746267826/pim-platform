package com.pim.app.di

import android.content.Context
import android.content.SharedPreferences
import androidx.room.Room
import com.pim.app.data.AppDatabase
import com.pim.app.data.AppUsageDao
import com.pim.app.data.PimDatabaseMigrations
import com.pim.app.settings.TrackingSettingsStore
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

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
}
