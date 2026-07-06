package com.pim.app.data

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object MobileDataModule {
    @Provides
    @Singleton
    fun provideMobileDataDao(database: AppDatabase): MobileDataDao {
        return database.mobileDataDao()
    }
}
