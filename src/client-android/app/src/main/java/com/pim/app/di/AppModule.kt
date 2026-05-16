package com.pim.app.di

import android.content.Context
import androidx.room.Room
import androidx.work.Configuration
import com.pim.app.data.AppDatabase
import com.pim.app.data.AppUsageDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import dagger.hilt.work.HiltWorkerFactory
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase {
        return Room.databaseBuilder(context, AppDatabase::class.java, "pim.db")
            .fallbackToDestructiveMigration()
            .build()
    }

    @Provides
    @Singleton
    fun provideAppUsageDao(db: AppDatabase): AppUsageDao = db.appUsageDao()

    @Provides
    @Singleton
    fun provideWorkerFactory(workerFactory: HiltWorkerFactory): Configuration.Provider {
        return object : Configuration.Provider {
            override val workManagerConfiguration: Configuration
                get() = Configuration.Builder()
                    .setWorkerFactory(workerFactory)
                    .build()
        }
    }
}
