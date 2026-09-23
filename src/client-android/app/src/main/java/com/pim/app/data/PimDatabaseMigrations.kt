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

    /**
     * 阶段一取证事件表（REQ-1 ~ REQ-6）。
     *
     * 只新增一张表，不动任何既有表/列：既有采集、同步、轨迹与诊断导出在升级后行为不变。
     * 本地不设条数上限（R4-P3），靠 30 天时间清理兜底（[ForensicEventDao.deleteOlderThan]）。
     */
    val MIGRATION_3_4 = object : Migration(3, 4) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS mobile_forensic_events (
                    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    event_type TEXT NOT NULL,
                    occurred_at_utc INTEGER NOT NULL,
                    client_item_key TEXT NOT NULL,
                    payload_json TEXT NOT NULL,
                    sync_status TEXT NOT NULL,
                    last_error TEXT,
                    created_at_utc INTEGER NOT NULL
                )
                """.trimIndent()
            )
            db.execSQL(
                """
                CREATE INDEX IF NOT EXISTS index_mobile_forensic_events_occurred_at_utc
                ON mobile_forensic_events(occurred_at_utc)
                """.trimIndent()
            )
            db.execSQL(
                """
                CREATE UNIQUE INDEX IF NOT EXISTS index_mobile_forensic_events_client_item_key
                ON mobile_forensic_events(client_item_key)
                """.trimIndent()
            )
            db.execSQL(
                """
                CREATE INDEX IF NOT EXISTS index_mobile_forensic_events_sync_status
                ON mobile_forensic_events(sync_status)
                """.trimIndent()
            )
        }
    }

    val ALL = arrayOf(MIGRATION_2_3, MIGRATION_3_4)
}
