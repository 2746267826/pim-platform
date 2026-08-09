# src/Pim.Infrastructure/Data/PimMigrationAdoptionService.cs

## 元信息
- 语言：C#
- 程序集或包：Pim.Infrastructure
- 职责：在已有业务库（`users` 已存在）但尚无 `__EFMigrationsHistory` 时，创建历史表并登记基线迁移 Id，使后续 `Migrate` 从 Stage0 等增量迁移继续。
- 主要依赖：
  - `Microsoft.EntityFrameworkCore`（`PimDbContext`、原始 SQL、连接）
  - `ILogger<PimMigrationAdoptionService>`
- 被谁使用：
  - `Pim.Api/Program.cs` 启动时 `AdoptExistingSchemaAsync`
  - `ServiceCollectionExtensions` `AddScoped`
  - 单元测试 `PimMigrationAdoptionServiceTests`

## 函数级结构化伪代码

### PimMigrationAdoptionService
#### 常量 `BaselineMigrationId`
- 输入：无
- 输出：字符串 `"20260524000000_BaselineExistingSchema"`
- 副作用：无
- 步骤：1. 作为写入 `__EFMigrationsHistory` 的基线 MigrationId
- 分支与异常：无
- 调用：无

#### 构造 `PimMigrationAdoptionService(PimDbContext db, ILogger<...> logger)`
- 输入：DbContext、日志
- 输出：服务实例
- 副作用：保存字段
- 步骤：1. 赋值 `_db`、`_logger`
- 分支与异常：无
- 调用：无

#### `static bool NeedsBaselineAdoption(bool usersTableExists, bool historyTableExists)`
- 输入：是否存在 `users`、是否存在 `__EFMigrationsHistory`
- 输出：需要收养基线时为 true
- 副作用：无
- 步骤：1. 返回 `usersTableExists && !historyTableExists`
- 分支与异常：无
- 调用：无

#### `Task AdoptExistingSchemaAsync(CancellationToken ct = default)`
- 输入：取消令牌
- 输出：完成 Task
- 副作用：可能创建 `__EFMigrationsHistory` 并插入基线行；写 Warning 日志
- 步骤：
  1. `TableExistsAsync("public","users")`、`TableExistsAsync("public","__EFMigrationsHistory")`。
  2. 若 `!NeedsBaselineAdoption` 则直接 return。
  3. 记录 Warning：正在收养基线 `BaselineMigrationId`。
  4. `ExecuteSqlRawAsync`：`CREATE TABLE IF NOT EXISTS "__EFMigrationsHistory"`（MigrationId PK、ProductVersion）；`INSERT ... VALUES (BaselineMigrationId, '8.0.11') ON CONFLICT DO NOTHING`。
- 分支与异常：不需要收养则 no-op；SQL/连接异常向上抛出
- 调用：`TableExistsAsync`、`Database.ExecuteSqlRawAsync`、`_logger.LogWarning`

#### `private Task<bool> TableExistsAsync(string schema, string table, CancellationToken ct)`
- 输入：schema/表名、取消令牌
- 输出：表是否存在
- 副作用：必要时打开连接
- 步骤：
  1. 取 `_db.Database.GetDbConnection()`；若未 Open 则 `OpenAsync`。
  2. 建命令：`SELECT EXISTS (SELECT 1 FROM information_schema.tables WHERE table_schema=@schema AND table_name=@table)`。
  3. 添加参数 schema/table；`ExecuteScalarAsync`；结果为 true 的 bool 则返回 true。
- 分支与异常：连接/查询异常向上抛出；非 bool 结果视为 false
- 调用：ADO.NET 连接/命令

## 近逐行中文伪代码

1. 引入 EF Core 与 Logging。
2. 命名空间 `Pim.Infrastructure.Data`；密封类 `PimMigrationAdoptionService`。
3. 常量 `BaselineMigrationId = "20260524000000_BaselineExistingSchema"`。
4. 字段 `_db`、`_logger`；构造注入赋值。
5. `NeedsBaselineAdoption`：用户表在且历史表不在 → true。
6. `AdoptExistingSchemaAsync`：
7. 查 `users` 与 `__EFMigrationsHistory` 是否存在。
8. 不需要收养则 return。
9. Warning 日志带 MigrationId。
10. 原始 SQL 创建历史表（若不存在）并插入基线行（冲突忽略）。
11. `TableExistsAsync`：打开连接 → 参数化 EXISTS 查询 → 返回 bool。

## 关系边
```json
{
  "nodes": [
    {
      "id": "src/Pim.Infrastructure/Data/PimMigrationAdoptionService.cs",
      "label": "PimMigrationAdoptionService",
      "path": "src/Pim.Infrastructure/Data/PimMigrationAdoptionService.cs",
      "doc": "docs/pseudocode/files/src/Pim.Infrastructure/Data/PimMigrationAdoptionService.cs.md",
      "layer": "infrastructure",
      "kind": "service"
    }
  ],
  "edges": [
    { "from": "src/Pim.Infrastructure/Data/PimMigrationAdoptionService.cs", "to": "src/Pim.Infrastructure/Data/PimDbContext.cs", "type": "depends_on" },
    { "from": "src/Pim.Infrastructure/Data/PimMigrationAdoptionService.cs", "to": "src/Pim.Infrastructure/Data/PimDbContext.cs", "type": "calls" },
    { "from": "src/Pim.Api/Program.cs", "to": "src/Pim.Infrastructure/Data/PimMigrationAdoptionService.cs", "type": "calls" },
    { "from": "src/Pim.Infrastructure/Extensions/ServiceCollectionExtensions.cs", "to": "src/Pim.Infrastructure/Data/PimMigrationAdoptionService.cs", "type": "depends_on" },
    { "from": "tests/Pim.UnitTests/Operations/PimMigrationAdoptionServiceTests.cs", "to": "src/Pim.Infrastructure/Data/PimMigrationAdoptionService.cs", "type": "tests" }
  ]
}
```
