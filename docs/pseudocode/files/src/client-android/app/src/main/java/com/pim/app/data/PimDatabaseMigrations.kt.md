# src/client-android/app/src/main/java/com/pim/app/data/PimDatabaseMigrations.kt

## 元信息
- 语言：Kotlin
- 程序集或包：client-android
- 职责：数据库迁移 `PimDatabaseMigrations`：Room schema 版本升级。
- 主要依赖：无项目内相对导入（或仅外部包）
- 被谁使用：阅读时由总控/关系图汇总；本文件边中列出 depends_on

## 函数级结构化伪代码

### PimDatabaseMigrations
#### 类型/结构声明
- 输入：无
- 输出：类型符号
- 副作用：无
- 步骤：1. 在 L6 声明 `PimDatabaseMigrations`
- 分支与异常：无
- 调用：无

### migrate
#### migrate(db: SupportSQLiteDatabase)
- 输入：db: SupportSQLiteDatabase
- 输出：见返回值/JSX/Unit
- 副作用：见步骤中的状态更新/IO/导航
- 步骤：
  1. 覆盖方法 `migrate`
  2. 执行：db.execSQL("ALTER TABLE mobile_location_points ADD COLUMN submitted_at_utc INTEGER")
  3. 执行：db.execSQL(
  4. 执行："ALTER TABLE mobile_location_points ADD COLUMN policy_mode TEXT NOT NULL " +
  5. 执行："DEFAULT 'PowerSavingNormal'"
  6. 执行："ALTER TABLE mobile_location_points ADD COLUMN schedule_low_frequency INTEGER NOT NULL DEFAULT 0"
  7. 执行：db.execSQL("ALTER TABLE mobile_location_points ADD COLUMN motion_state TEXT")
  8. 执行："ALTER TABLE mobile_location_points ADD COLUMN quality_flags TEXT NOT NULL DEFAULT '[]'"
  9. 执行：CREATE TABLE IF NOT EXISTS mobile_location_dropped_diagnostics (
  10. 执行：id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
  11. 执行：recorded_at_utc INTEGER NOT NULL,
  12. 执行：provider TEXT,
  13. 执行：accuracy_meters REAL,
  14. 执行：policy_mode TEXT NOT NULL,
  15. 执行：reason TEXT NOT NULL,
  16. 执行：created_at_utc INTEGER NOT NULL
  17. 执行：""".trimIndent()
  18. 执行：CREATE INDEX IF NOT EXISTS index_mobile_location_dropped_diagnostics_recorded_at_utc
  19. 执行：ON mobile_location_dropped_diagnostics(recorded_at_utc)
  20. 执行：CREATE TABLE IF NOT EXISTS mobile_location_policy_transitions (
  21. 执行：from_mode TEXT,
  22. 执行：to_mode TEXT NOT NULL,
  23. 执行：occurred_at_utc INTEGER NOT NULL
  24. 执行：CREATE INDEX IF NOT EXISTS index_mobile_location_policy_transitions_occurred_at_utc
  25. 执行：ON mobile_location_policy_transitions(occurred_at_utc)
- 分支与异常：无显著分支
- 调用：migrate、db.execSQL、mobile_location_dropped_diagnostics、trimIndent、mobile_location_policy_transitions

## 近逐行中文伪代码

1. [L6] 单例 object `PimDatabaseMigrations`
2. [L7] 执行：val MIGRATION_2_3 = object : Migration(2, 3) {
3. [L8] 覆盖方法 `migrate`
4. [L9] 执行：db.execSQL("ALTER TABLE mobile_location_points ADD COLUMN submitted_at_utc INTEGER")
5. [L10] 执行：db.execSQL(
6. [L11] 执行："ALTER TABLE mobile_location_points ADD COLUMN policy_mode TEXT NOT NULL " +
7. [L12] 执行："DEFAULT 'PowerSavingNormal'"
8. [L14] 执行：db.execSQL(
9. [L15] 执行："ALTER TABLE mobile_location_points ADD COLUMN schedule_low_frequency INTEGER NOT NULL DEFAULT 0"
10. [L17] 执行：db.execSQL("ALTER TABLE mobile_location_points ADD COLUMN motion_state TEXT")
11. [L18] 执行：db.execSQL(
12. [L19] 执行："ALTER TABLE mobile_location_points ADD COLUMN quality_flags TEXT NOT NULL DEFAULT '[]'"
13. [L21] 执行：db.execSQL(
14. [L23] 执行：CREATE TABLE IF NOT EXISTS mobile_location_dropped_diagnostics (
15. [L24] 执行：id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
16. [L25] 执行：recorded_at_utc INTEGER NOT NULL,
17. [L26] 执行：provider TEXT,
18. [L27] 执行：accuracy_meters REAL,
19. [L28] 执行：policy_mode TEXT NOT NULL,
20. [L29] 执行：reason TEXT NOT NULL,
21. [L30] 执行：created_at_utc INTEGER NOT NULL
22. [L32] 执行：""".trimIndent()
23. [L34] 执行：db.execSQL(
24. [L36] 执行：CREATE INDEX IF NOT EXISTS index_mobile_location_dropped_diagnostics_recorded_at_utc
25. [L37] 执行：ON mobile_location_dropped_diagnostics(recorded_at_utc)
26. [L38] 执行：""".trimIndent()
27. [L40] 执行：db.execSQL(
28. [L42] 执行：CREATE TABLE IF NOT EXISTS mobile_location_policy_transitions (
29. [L43] 执行：id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
30. [L44] 执行：from_mode TEXT,
31. [L45] 执行：to_mode TEXT NOT NULL,
32. [L46] 执行：reason TEXT NOT NULL,
33. [L47] 执行：occurred_at_utc INTEGER NOT NULL
34. [L49] 执行：""".trimIndent()
35. [L51] 执行：db.execSQL(
36. [L53] 执行：CREATE INDEX IF NOT EXISTS index_mobile_location_policy_transitions_occurred_at_utc
37. [L54] 执行：ON mobile_location_policy_transitions(occurred_at_utc)
38. [L55] 执行：""".trimIndent()
39. [L60] 执行：val ALL = arrayOf(MIGRATION_2_3)

## 关系边
```json
{
  "nodes": [
    {
      "id": "src/client-android/app/src/main/java/com/pim/app/data/PimDatabaseMigrations.kt",
      "label": "PimDatabaseMigrations",
      "path": "src/client-android/app/src/main/java/com/pim/app/data/PimDatabaseMigrations.kt",
      "doc": "docs/pseudocode/files/src/client-android/app/src/main/java/com/pim/app/data/PimDatabaseMigrations.kt.md",
      "layer": "client-android",
      "kind": "service"
    }
  ],
  "edges": []
}
```
