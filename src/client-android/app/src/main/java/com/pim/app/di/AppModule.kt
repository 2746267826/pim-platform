package com.pim.app.di

import android.content.Context
import androidx.room.Room
import com.pim.app.data.AppDatabase
import com.pim.app.data.AppUsageDao
import com.pim.app.data.PimDatabaseMigrations
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
}
