# src/modules/Pim.Module.Calendar/Entities/OutlookSyncBatchEntity.cs

## 元信息
- 语言：C#
- 程序集或包：Pim.Module.Calendar
- 职责：持久化一次 Outlook/Graph 同步批次的进度与计数（读/建/更/冲突/确认/失败）、步骤与错误 JSON、起止时间。
- 主要依赖：EF 列映射、`jsonb` 字段
- 被谁使用：Outlook 同步服务写批次；API 映射为 `OutlookSyncBatchResponse`

## 函数级结构化伪代码

### OutlookSyncBatchEntity
#### 属性集合（表 `outlook_sync_batches`）
- 输入：属性赋值
- 输出：实体状态
- 副作用：无（纯 POCO）
- 步骤：
  1. `Id` 默认 `NewGuid`；`UserId` 租户
  2. `Provider` 默认 `"outlook"`；`Status` 默认 `"running"`
  3. 计数：`ReadCount`/`CreatedCount`/`UpdatedCount`/`ConflictCount`/`ConfirmationCount`/`FailureCount`
  4. `StepsJson`/`ErrorsJson` 默认 `"[]"`（jsonb）
  5. `ErrorSummary` 可空；`StartedAt` 默认 UtcNow；`FinishedAt` 可空
- 分支与异常：无
- 调用：无

## 近逐行中文伪代码

1. 引入 DataAnnotations 与 Schema
2. 命名空间 `Pim.Module.Calendar.Entities`
3. 表名 `outlook_sync_batches`；类 `OutlookSyncBatchEntity`
4. 主键 `id`；`user_id`；`provider`/`status` 最长 40
5. 六项计数列；`steps_json`/`errors_json` jsonb 默认空数组
6. `error_summary`；`started_at` 默认当前 UTC；`finished_at` 可空

## 关系边
```json
{
  "nodes": [
    {
      "id": "src/modules/Pim.Module.Calendar/Entities/OutlookSyncBatchEntity.cs",
      "label": "OutlookSyncBatchEntity",
      "path": "src/modules/Pim.Module.Calendar/Entities/OutlookSyncBatchEntity.cs",
      "doc": "docs/pseudocode/files/src/modules/Pim.Module.Calendar/Entities/OutlookSyncBatchEntity.cs.md",
      "layer": "module.calendar",
      "kind": "entity"
    }
  ],
  "edges": [
    { "from": "src/Pim.Infrastructure/Data/PimDbContext.cs", "to": "src/modules/Pim.Module.Calendar/Entities/OutlookSyncBatchEntity.cs", "type": "depends_on" },
    { "from": "src/modules/Pim.Module.Calendar/Services", "to": "src/modules/Pim.Module.Calendar/Entities/OutlookSyncBatchEntity.cs", "type": "depends_on" },
    { "from": "src/modules/Pim.Module.Calendar/Entities/OutlookSyncBatchEntity.cs", "to": "src/modules/Pim.Module.Calendar/DTOs/CalendarDtos.cs", "type": "depends_on" }
  ]
}
```
