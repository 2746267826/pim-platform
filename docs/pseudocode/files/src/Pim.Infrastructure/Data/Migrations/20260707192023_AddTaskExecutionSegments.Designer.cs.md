# src/Pim.Infrastructure/Data/Migrations/20260707192023_AddTaskExecutionSegments.Designer.cs

## 元信息
- 语言：C#（EF Core 自动生成 Designer）
- 程序集或包：Pim.Infrastructure
- 职责：迁移 `20260707192023_AddTaskExecutionSegments` 的目标模型快照；在 AI/Files/Mobile/PC/QuickNotes 全量模型上纳入 `task_execution_segments`，共 51 张表。
- 主要依赖：EF Core Migrations、Npgsql、`PimDbContext`
- 被谁使用：EF 迁移工具链与 `AddTaskExecutionSegments` partial

## 函数级结构化伪代码

### AddTaskExecutionSegments（partial）
#### 特性与类声明
- 输入：无
- 输出：绑定 `20260707192023_AddTaskExecutionSegments`
- 副作用：无
- 步骤：DbContext + Migration 特性；partial class
- 分支与异常：无
- 调用：无

#### `protected override void BuildTargetModel(ModelBuilder modelBuilder)`
- 输入：ModelBuilder
- 输出：void
- 副作用：配置 51 实体目标模型
- 步骤：
  1. 全局 ProductVersion 8.0.11 / MaxIdentifierLength 63 / Identity。
  2. AI：`ai_provider_settings`、`ai_request_logs`（索引 CorrelationId/Model/Module/Purpose/StartedAt/Status/UserId/SourceObject*）。
  3. 运维与用户：audit_logs、daemon_heartbeats、login_attempts、operation_confirmations、refresh_tokens、users。
  4. Calendar：calendars/events/outlook_connections/pending_confirmations/scheduling_feedback/tasks。
  5. **本迁移焦点** `TaskExecutionSegmentEntity`→`task_execution_segments`：
     - 字段：Id、ConfirmationId、CreatedAt/UpdatedAt/DeletedAt、StartsAt/EndsAt、PlanningReason、Source、Status、TaskId、UserId。
     - 索引：ConfirmationId、TaskId、UserId、复合 (UserId, TaskId, StartsAt)。
     - FK：Task → Cascade。
  6. Files 七表；Mobile 十二表（catalog/devices/location/sync/timeline/usage*）；PcTracker 扩展（audits/knowledge/signatures/categories 等）；QuickNotes 两表。
  7. 关系段：既有 User/Calendar/Files/PC/QN + TaskExecutionSegment→Task。
- 分支与异常：无
- 调用：Fluent API

## 近逐行中文伪代码

1. auto-generated 头与 using；nullable disable。
2. Migration 特性与 partial `AddTaskExecutionSegments`。
3. BuildTargetModel 开头全局注解。
4. 顺序配置 AiProviderSetting、AiRequestLog、Audit…User。
5. Calendar 实体块后插入 TaskExecutionSegment 完整属性/索引/ToTable。
6. 继续 Files → Mobile 全家桶 → PcTracker → QuickNotes。
7. 关系：Login/Refresh→User；Event/Task→Calendar；**Segment→Task Cascade**；Files 图；AppKnowledge/Keystats/PcCategory；QuickNote 附件。
8. 结束 pragma。DDL 在非 Designer 迁移类中。

## 关系边
```json
{
  "nodes": [
    {
      "id": "src/Pim.Infrastructure/Data/Migrations/20260707192023_AddTaskExecutionSegments.Designer.cs",
      "label": "AddTaskExecutionSegments.Designer",
      "path": "src/Pim.Infrastructure/Data/Migrations/20260707192023_AddTaskExecutionSegments.Designer.cs",
      "doc": "docs/pseudocode/files/src/Pim.Infrastructure/Data/Migrations/20260707192023_AddTaskExecutionSegments.Designer.cs.md",
      "layer": "infrastructure",
      "kind": "other"
    }
  ],
  "edges": [
    { "from": "src/Pim.Infrastructure/Data/Migrations/20260707192023_AddTaskExecutionSegments.Designer.cs", "to": "src/Pim.Infrastructure/Data/PimDbContext.cs", "type": "depends_on" },
    { "from": "src/Pim.Infrastructure/Data/Migrations/20260707192023_AddTaskExecutionSegments.cs", "to": "src/Pim.Infrastructure/Data/Migrations/20260707192023_AddTaskExecutionSegments.Designer.cs", "type": "depends_on" }
  ]
}
```
