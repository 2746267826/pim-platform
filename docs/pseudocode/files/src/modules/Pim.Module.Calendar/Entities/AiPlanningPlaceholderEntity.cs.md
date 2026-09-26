# src/modules/Pim.Module.Calendar/Entities/AiPlanningPlaceholderEntity.cs

## 元信息
- 语言：C#
- 程序集或包：Pim.Module.Calendar
- 职责：映射表 `ai_planning_placeholders` 的 AI 规划占位时段实体（建议时段、原因、确认关联、软删除）。
- 主要依赖：`System.ComponentModel.DataAnnotations`/`Schema`；`Pim.Core.Data.ISoftDeletable`
- 被谁使用：EF 模型与规划服务；`PlanningObjectModelTests` / `PlanningModelServiceCompletionTests` 等

## 函数级结构化伪代码

### AiPlanningPlaceholderEntity
#### 属性集（无行为方法）
- 输入：各属性赋值（调用方/EF）
- 输出：行状态
- 副作用：无（纯 POCO）；实现 `ISoftDeletable` 供全局软删除过滤
- 步骤：
  1. `Id`：主键 Guid，默认 `NewGuid`
  2. `UserId`：所属用户
  3. `Title`：标题，最长 255
  4. `StartsAt` / `EndsAt`：占位起止时间
  5. `Reason`：建议原因全文
  6. `Source`：来源，默认 `"ai"`，最长 40
  7. `Status`：状态，默认 `"Suggested"`，最长 40
  8. `ConfirmationId`：可选关联确认 Id
  9. `CreatedAt` / `UpdatedAt`：默认 UTC 现在
  10. `DeletedAt`：软删除时间，可空
- 分支与异常：无运行时分支
- 调用：无

## 近逐行中文伪代码

1. 引入 DataAnnotations、Schema、`ISoftDeletable`
2. 命名空间 `Pim.Module.Calendar.Entities`
3. 表 `ai_planning_placeholders`；类实现 `ISoftDeletable`
4. 主键 `id`、用户 `user_id`、标题 `title`
5. 时段 `starts_at`/`ends_at`、原因 `reason`
6. 来源默认 ai、状态默认 Suggested
7. 可选 `confirmation_id`；时间戳与 `deleted_at`

## 关系边
```json
{
  "nodes": [
    {
      "id": "src/modules/Pim.Module.Calendar/Entities/AiPlanningPlaceholderEntity.cs",
      "label": "AiPlanningPlaceholderEntity",
      "path": "src/modules/Pim.Module.Calendar/Entities/AiPlanningPlaceholderEntity.cs",
      "doc": "docs/pseudocode/files/src/modules/Pim.Module.Calendar/Entities/AiPlanningPlaceholderEntity.cs.md",
      "layer": "module.calendar",
      "kind": "entity"
    }
  ],
  "edges": [
    { "from": "src/modules/Pim.Module.Calendar/Entities/AiPlanningPlaceholderEntity.cs", "to": "src/Pim.Core/Data/ISoftDeletable.cs", "type": "implements" },
    { "from": "src/Pim.Infrastructure/Data/PimDbContext.cs", "to": "src/modules/Pim.Module.Calendar/Entities/AiPlanningPlaceholderEntity.cs", "type": "depends_on" }
  ]
}
```
