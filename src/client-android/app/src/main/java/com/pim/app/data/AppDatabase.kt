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
        MobileLocationDroppedDiagnosticEntity::class,
        MobileLocationPolicyTransitionEntity::class,
        MobileSyncBatchEntity::class,
        MobileLogEntity::class,
        MobileDeviceProfileEntity::class
    ],
    version = 3,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun appUsageDao(): AppUsageDao
    abstract fun mobileDataDao(): MobileDataDao
}
