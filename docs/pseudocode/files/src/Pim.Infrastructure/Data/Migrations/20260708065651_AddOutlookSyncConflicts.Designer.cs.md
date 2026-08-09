# src/Pim.Infrastructure/Data/Migrations/20260708065651_AddOutlookSyncConflicts.Designer.cs

## 元信息
- 语言：C#（EF Core 自动生成 Designer）
- 程序集或包：Pim.Infrastructure
- 职责：迁移 `20260708065651_AddOutlookSyncConflicts` 的目标模型快照；在规划对象模型/审计版本/Outlook 同步基础上纳入 `sync_conflicts`，共 61 张表。
- 主要依赖：EF Core Migrations、Npgsql、`PimDbContext`
- 被谁使用：EF 迁移工具链与 `AddOutlookSyncConflicts` partial

## 函数级结构化伪代码

### AddOutlookSyncConflicts（partial）
#### 特性与类声明
- 输入：无
- 输出：绑定 `20260708065651_AddOutlookSyncConflicts`
- 副作用：无
- 步骤：DbContext + Migration 特性；partial class
- 分支与异常：无
- 调用：无

#### `protected override void BuildTargetModel(ModelBuilder modelBuilder)`
- 输入：ModelBuilder
- 输出：void
- 副作用：配置 61 实体目标模型
- 步骤：
  1. 全局注解 8.0.11 / 标识符 63 / Identity。
  2. 基础设施：`audit_versions`、`ai_provider_settings`、`ai_request_logs`、运维与 users。
  3. Calendar 规划扩展：`ai_planning_placeholders`、`availability_windows`、`domain_projects`、`habit_routines`/`habit_occurrences`、`task_books`/`task_checklist_items`、`outlook_sync_batches`、`task_execution_segments` 等。
  4. **本迁移焦点** `SyncConflictEntity`→`sync_conflicts`：
     - 字段：ConflictKind、CreatedAt、ExternalSnapshotJson/PimSnapshotJson（jsonb 默认 `{}`）、GraphEventId、ObjectId/ObjectType（默认 event）、Provider（默认 outlook）、ResolvedConfirmationId、Status（默认 open）、UpdatedAt、UserId。
     - 索引：GraphEventId、ResolvedConfirmationId、(ObjectType,ObjectId)、(UserId,Provider,Status)。
  5. 其余 Files/Mobile/PcTracker/QuickNotes 与前序快照一致。
  6. 关系：HabitOccurrence→HabitRoutine Cascade；TaskBook→DomainProject；Checklist→Task；Task 多 FK（Calendar/DomainProject/Parent/TaskBook）；Segment→Task；Files/PC/QN 既有图。
- 分支与异常：无
- 调用：Fluent API

## 近逐行中文伪代码

1. auto-generated 头；标准 EF/Npgsql using；nullable disable。
2. Migration `20260708065651_AddOutlookSyncConflicts`；partial `AddOutlookSyncConflicts`。
3. BuildTargetModel：全局注解。
4. 先配 AuditVersion、AI 设置/日志、运维用户表。
5. Calendar 大块：规划占位、可用性、项目、事件、习惯、Outlook 连接与同步批、pending、scheduling。
6. **SyncConflictEntity** 全字段 + 四组索引 + ToTable(`sync_conflicts`)。
7. 接 TaskBook/Checklist/Task/Segment，再 Files→Mobile→Pc→QuickNotes。
8. 关系配置：User、Event、Habit、TaskBook、Checklist、Task 多重 FK、Segment、Files 图、PC、QuickNotes。
9. 结束。DDL 在对应非 Designer 迁移类。

## 关系边
```json
{
  "nodes": [
    {
      "id": "src/Pim.Infrastructure/Data/Migrations/20260708065651_AddOutlookSyncConflicts.Designer.cs",
      "label": "AddOutlookSyncConflicts.Designer",
      "path": "src/Pim.Infrastructure/Data/Migrations/20260708065651_AddOutlookSyncConflicts.Designer.cs",
      "doc": "docs/pseudocode/files/src/Pim.Infrastructure/Data/Migrations/20260708065651_AddOutlookSyncConflicts.Designer.cs.md",
      "layer": "infrastructure",
      "kind": "other"
    }
  ],
  "edges": [
    { "from": "src/Pim.Infrastructure/Data/Migrations/20260708065651_AddOutlookSyncConflicts.Designer.cs", "to": "src/Pim.Infrastructure/Data/PimDbContext.cs", "type": "depends_on" },
    { "from": "src/Pim.Infrastructure/Data/Migrations/20260708065651_AddOutlookSyncConflicts.cs", "to": "src/Pim.Infrastructure/Data/Migrations/20260708065651_AddOutlookSyncConflicts.Designer.cs", "type": "depends_on" }
  ]
}
```
