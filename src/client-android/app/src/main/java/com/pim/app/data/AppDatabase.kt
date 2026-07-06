package com.pim.app.data

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [
        AppUsageEntity::class,
        MobileUsageEventEntity::class,
        MobileUsageSummaryEntity::class,
        MobileAppMetadataEntity::class,
        MobileLocationPointEntity::class,
        MobileSyncBatchEntity::class,
        MobileLogEntity::class,
        MobileDeviceProfileEntity::class
    ],
    version = 2,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun appUsageDao(): AppUsageDao
    abstract fun mobileDataDao(): MobileDataDao
}
