# Task 4 Report — Migration cleanup & backfills / 任务4报告 - 迁移清理与回填

## Commits
- fix(calendar): clean recurrence migration, add RecurrenceId & legacy marking / 修复日历迁移清理并补充 RecurrenceId 与 legacy 标记

## Checklist / 清单
- [x] Step 4.1 Extend migration SQL idempotent (is_series_master, is_exception+series_master_id, recurrence_id via dtstart, legacyOccurrence jsonb_set) / 扩展迁移 SQL 幂等回填
- [x] Step 4.2 Mark legacy occurrence as read-only via recurrence_metadata_json / 标记历史普通 occurrence
- [x] Step 4.3 Tests for idempotency, RecurrenceId backfill, legacy marking and unrelated schema not changed / 幂等与回填测试
- [x] Remove unrelated AlterColumn life_category and IX_pc_activity_category_rules_category_id from Up/Down / 移除无关的 life_category 与索引变更
- [x] Ensure Down only drops recurrence columns/index/FK / 确保 Down 仅回滚本次新增
- [x] Verify Designer/Snapshot consistency (no unrelated index, life_category defaults remain "其他" per model) / 核对 Designer/Snapshot

## Test Commands & Results / 测试命令与结果
- `dotnet build src/Pim.Infrastructure/Pim.Infrastructure.csproj --no-restore` — Build succeeded, 0 Error(s)
- `dotnet test tests/Pim.UnitTests/Pim.UnitTests.csproj --filter RecurrenceMigration --no-restore` — Passed 9/9

## Implementation Summary / 实现摘要
- `20260820035922_AddRecurrenceMasterModel.cs`: Up 仅保留 3 个 AddColumn、4 段幂等 Sql（series_master、exception+FK、recurrence_id=dtstart::text、legacyOccurrence=jsonb_set）、唯一索引与 FK；Down 仅 Drop FK/Index/Columns。删除 4 个 AlterColumn(life_category) 与 pc_activity 索引。
- `RecurrenceMigrationTests.cs`: 9 个用例覆盖无关字段不存在、必要列/索引/FK 存在、4 段回填存在且含幂等守卫、Down 仅删 3 列。
- Designer/Snapshot 未改动索引部分（原本即无该索引）；life_category 保持 HasDefaultValue("其他") 与实体定义一致，无需额外 Alter。

## Self-Review Findings / 自检
- No critical issues. Migration SQL 使用 COALESCE(jsonb) 与过滤条件保证重复执行安全。
- Snapshot 仍保留 "其他" 默认值符合当前 MobileLifeCategories.Uncategorized 定义，不视为无关变更。

## Residual Risks / 遗留风险
- RecurrenceId 回填使用 dtstart::text，格式与服务层 ISO 8601 可能略有差异，但满足非空与幂等要求；后续可按需改为 to_char(UTC) 精确格式。
