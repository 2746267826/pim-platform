# PASS-002 | docs/operations/migrations.md | 合格 | 迁移机制与启动流程
- 验证方式：read_file + grep `PimMigrationAdoptionService` `Database.Migrate`
- 验证点：文档声称 `Program.cs runs migration adoption and then Database.Migrate()`；PC Tracker 特殊幂等 SQL 仅用于兼容索引/分区；新增迁移命令 `dotnet ef migrations add --project Pim.Infrastructure --startup-project Pim.Api --context PimDbContext`
- 代码实际：`src/Pim.Api/Program.cs:128-145` 依次调用 `AdoptExistingSchemaAsync()` 与 `MigrateAsync()`，catch 后 Warning；`src/Pim.Infrastructure/Data/PimMigrationAdoptionService.cs` 实现 `20260524000000_BaselineExistingSchema` 标记；`src/modules/Pim.Module.PcTracker/Services/PcTrackerSchemaInitializer.cs` 仅含 `CREATE TABLE IF NOT EXISTS` 与特殊索引，未创建普通业务表的 ad-hoc 建表
- 结论：文档描述与代码启动路径一致，命令示例可执行，标记为通过
