# tests/Pim.UnitTests/Calendar/ScheduleWorkbenchModelTests.cs

## 元信息
- 语言：C#
- 程序集或包：Pim.UnitTests
- 职责：断言 EF 模型中日程工作台实体表名、属性与复合索引。
- 主要依赖：`PimDbContext`、`TaskExecutionSegmentEntity`、`OutlookSyncBatchEntity`、`OutlookConnectionEntity`
- 被谁使用：dotnet test

## 函数级结构化伪代码

### PimDbContext_ConfiguresScheduleWorkbenchEntities
- 步骤：
  1. 注册 Calendar 模块程序集；用 Npgsql 连接串建 context（仅读 Model）
  2. TaskExecutionSegment：表 `task_execution_segments`；StartsAt/EndsAt；索引 UserId+TaskId+StartsAt
  3. OutlookSyncBatch：表 `outlook_sync_batches`；索引 UserId+Provider+StartedAt
  4. OutlookConnection：ClientId/TenantId/Scopes/TokenHealth 属性存在

### CreateDb
- 步骤：UseNpgsql 假连接串

## 近逐行中文伪代码

1. [L10-14] 注册程序集并建 db
2. [L16-26] segment 表/属性/索引
3. [L28-36] batch 表/索引
4. [L38-43] connection 属性
5. [L46-52] CreateDb

## 关系边
```json
{
  "nodes": [
    {
      "id": "tests/Pim.UnitTests/Calendar/ScheduleWorkbenchModelTests.cs",
      "label": "ScheduleWorkbenchModelTests",
      "path": "tests/Pim.UnitTests/Calendar/ScheduleWorkbenchModelTests.cs",
      "doc": "docs/pseudocode/files/tests/Pim.UnitTests/Calendar/ScheduleWorkbenchModelTests.cs.md",
      "layer": "tests",
      "kind": "test"
    }
  ],
  "edges": [
    { "from": "tests/Pim.UnitTests/Calendar/ScheduleWorkbenchModelTests.cs", "to": "src/Pim.Infrastructure/Data/PimDbContext.cs", "type": "tests" },
    { "from": "tests/Pim.UnitTests/Calendar/ScheduleWorkbenchModelTests.cs", "to": "src/Pim.Module.Calendar/Entities/TaskExecutionSegmentEntity.cs", "type": "depends_on" },
    { "from": "tests/Pim.UnitTests/Calendar/ScheduleWorkbenchModelTests.cs", "to": "src/Pim.Module.Calendar/Entities/OutlookSyncBatchEntity.cs", "type": "depends_on" }
  ]
}
```
