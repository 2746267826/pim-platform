package com.pim.app.data

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [AppUsageEntity::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun appUsageDao(): AppUsageDao
}
