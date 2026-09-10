# src/Pim.Infrastructure/Data/Migrations/20260708065004_CompleteOutlookGraphSync.Designer.cs

## 元信息
- 语言：C#（EF Core 自动生成 Designer）
- 程序集或包：Pim.Infrastructure
- 职责：迁移 `CompleteOutlookGraphSync` 完成后的全量目标模型；Up 本身主要为 Outlook 连接/事件补列，快照中同时包含此前已累积的规划对象、审计版本、Outlook 同步批、任务书等
- 主要依赖：EF Core Migrations、Npgsql、`PimDbContext`
- 被谁使用：EF 工具链与 `CompleteOutlookGraphSync` partial

## 函数级结构化伪代码

### CompleteOutlookGraphSync（partial）
#### 特性
- 输入：无
- 输出：绑定 `PimDbContext` 与迁移 Id `20260708065004_CompleteOutlookGraphSync`
- 副作用：无
- 步骤：DbContext/Migration 注解
- 分支与异常：无
- 调用：无

#### protected override void BuildTargetModel(ModelBuilder modelBuilder)
- 输入：`ModelBuilder`
- 输出：无
- 副作用：构建约 60 实体 / 约 60 表的目标模型（本槽位最大 Designer 之一）
- 步骤：
  1. 模型注解 ProductVersion 8.0.11
  2. 核心与 AI/运营实体（含 `AuditVersionEntity`→`audit_versions`）
  3. Calendar/规划扩展实体：
     - `AiPlanningPlaceholder`、`AvailabilityWindow`、`DomainProject`
     - `HabitRoutine`/`HabitOccurrence`
     - `OutlookConnection`（含 access_token_expires_at 等）
     - `OutlookSyncBatch`→`outlook_sync_batches`
     - `TaskBook`/`TaskChecklistItem`/`TaskExecutionSegment` 与 `Task`/`Event`（Event 含 outlook_change_key、outlook_etag、outlook_event_id 索引）
  4. Files 七表；Mobile 十二表；PcTracker 全套；QuickNotes
  5. 关系与 Navigation 段
- 分支与异常：无
- 调用：Fluent 映射 API

### 对应 Up 迁移增量（对照 .cs，非 Designer 全量）
1. `outlook_connections` 增加 `access_token_expires_at`
2. `events` 增加 `outlook_change_key`、`outlook_etag`
3. 为 `outlook_change_key`、`outlook_event_id` 建索引
4. Down 反向删除索引与列

### 快照中相对 MobileAnalytics 额外显著表
- `audit_versions`、`ai_planning_placeholders`、`availability_windows`、`domain_projects`
- `habit_routines`、`habit_occurrences`、`outlook_sync_batches`
- `task_books`、`task_checklist_items`、`task_execution_segments`

## 近逐行中文伪代码

1. auto-generated 头；using EF/Npgsql/`Pim.Infrastructure.Data`
2. partial `CompleteOutlookGraphSync` 绑定迁移 Id
3. `BuildTargetModel`：先 `AuditVersionEntity`（before/after/changed_fields JSON 默认、actor、confirmation_id 等）
4. 再依次展开 AI 设置/日志、运营与 Auth、Calendar 规划与 Outlook、Files、Mobile、PcTracker、QuickNotes
5. Event 映射中出现 outlook_change_key/etag 列与索引（与 Up 一致）
6. OutlookConnection 含 token 过期时间列
7. 关系段配置 FK/删除行为/Navigation
8. pragma restore；结束

## 关系边
```json
{
  "nodes": [
    {
      "id": "src/Pim.Infrastructure/Data/Migrations/20260708065004_CompleteOutlookGraphSync.Designer.cs",
      "label": "CompleteOutlookGraphSync.Designer",
      "path": "src/Pim.Infrastructure/Data/Migrations/20260708065004_CompleteOutlookGraphSync.Designer.cs",
      "doc": "docs/pseudocode/files/src/Pim.Infrastructure/Data/Migrations/20260708065004_CompleteOutlookGraphSync.Designer.cs.md",
      "layer": "infrastructure",
      "kind": "other"
    }
  ],
  "edges": [
    { "from": "src/Pim.Infrastructure/Data/Migrations/20260708065004_CompleteOutlookGraphSync.Designer.cs", "to": "src/Pim.Infrastructure/Data/PimDbContext.cs", "type": "depends_on" },
    { "from": "src/Pim.Infrastructure/Data/Migrations/20260708065004_CompleteOutlookGraphSync.Designer.cs", "to": "src/Pim.Infrastructure/Data/Migrations/20260708065004_CompleteOutlookGraphSync.cs", "type": "depends_on" },
    { "from": "src/Pim.Infrastructure/Data/Migrations/20260708065004_CompleteOutlookGraphSync.Designer.cs", "to": "src/modules/Pim.Module.Calendar/Entities", "type": "depends_on" },
    { "from": "src/Pim.Infrastructure/Data/Migrations/20260708065004_CompleteOutlookGraphSync.cs", "to": "outlook_connections", "type": "depends_on" },
    { "from": "src/Pim.Infrastructure/Data/Migrations/20260708065004_CompleteOutlookGraphSync.cs", "to": "events", "type": "depends_on" }
  ]
}
```
