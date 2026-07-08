package com.pim.app.data

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

object PimDatabaseMigrations {
    val MIGRATION_2_3 = object : Migration(2, 3) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE mobile_location_points ADD COLUMN submitted_at_utc INTEGER")
            db.execSQL(
                "ALTER TABLE mobile_location_points ADD COLUMN policy_mode TEXT NOT NULL " +
                    "DEFAULT 'PowerSavingNormal'"
            )
            db.execSQL(
                "ALTER TABLE mobile_location_points ADD COLUMN schedule_low_frequency INTEGER NOT NULL DEFAULT 0"
            )
            db.execSQL("ALTER TABLE mobile_location_points ADD COLUMN motion_state TEXT")
            db.execSQL(
                "ALTER TABLE mobile_location_points ADD COLUMN quality_flags TEXT NOT NULL DEFAULT '[]'"
            )
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS mobile_location_dropped_diagnostics (
                    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    recorded_at_utc INTEGER NOT NULL,
                    provider TEXT,
                    accuracy_meters REAL,
                    policy_mode TEXT NOT NULL,
                    reason TEXT NOT NULL,
                    created_at_utc INTEGER NOT NULL
                )
                """.trimIndent()
            )
            db.execSQL(
                """
                CREATE INDEX IF NOT EXISTS index_mobile_location_dropped_diagnostics_recorded_at_utc
                ON mobile_location_dropped_diagnostics(recorded_at_utc)
                """.trimIndent()
            )
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS mobile_location_policy_transitions (
                    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    from_mode TEXT,
                    to_mode TEXT NOT NULL,
                    reason TEXT NOT NULL,
                    occurred_at_utc INTEGER NOT NULL
                )
                """.trimIndent()
            )
            db.execSQL(
                """
                CREATE INDEX IF NOT EXISTS index_mobile_location_policy_transitions_occurred_at_utc
                ON mobile_location_policy_transitions(occurred_at_utc)
                """.trimIndent()
            )
        }
    }

    val ALL = arrayOf(MIGRATION_2_3)
}
