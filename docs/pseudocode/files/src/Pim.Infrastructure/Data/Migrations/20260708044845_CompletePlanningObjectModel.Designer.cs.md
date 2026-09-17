# src/Pim.Infrastructure/Data/Migrations/20260708044845_CompletePlanningObjectModel.Designer.cs

## 元信息
- 语言：C#（EF Core 自动生成）
- 程序集或包：Pim.Infrastructure
- 职责：迁移 `20260708044845_CompletePlanningObjectModel` 的目标模型快照；固化完整规划对象模型（DomainProject/TaskBook/Habit/Availability/执行段等）及 Mobile 全表。
- 主要依赖：EF Core Migrations/Npgsql；`PimDbContext`
- 被谁使用：EF 迁移管线；与同名 `.cs` partial 配对

## 函数级结构化伪代码

### CompletePlanningObjectModel（partial）
#### 特性与类头
- 输入：无
- 输出：Migration Id `20260708044845_CompletePlanningObjectModel`
- 副作用：无
- 步骤：`[DbContext]` + `[Migration]` + partial 类
- 分支与异常：无
- 调用：EF

#### `BuildTargetModel(ModelBuilder modelBuilder)`
- 输入：`modelBuilder`
- 输出：约 59 张表的目标模型
- 副作用：仅内存模型
- 步骤：
  1. 模型注解 EF 8.0.11；Npgsql Identity
  2. 保留 AI/运维/Auth/Files/QuickNotes/PcTracker/Mobile 既有实体
  3. **规划对象模型扩展表**（Calendar 模块）：
     - `ai_planning_placeholders`
     - `availability_windows`
     - `domain_projects`（UserId+Name 唯一）
     - `habit_routines` / `habit_occurrences`
     - `outlook_sync_batches`
     - `task_books` / `task_checklist_items` / `task_execution_segments`
     - `tasks` 扩展字段：DomainProjectId、TaskBookId、ReviewOutcome、StateReason、Source 等
  4. Mobile 全套：`mobile_app_catalog*`、`mobile_devices`、`mobile_location_points`、`mobile_sync_batches`、`mobile_timeline_blocks`、`mobile_usage_*`
  5. 关系新增：HabitOccurrence→HabitRoutine；TaskBook→DomainProject；Checklist/ExecutionSegment→Task；Task→DomainProject/TaskBook/Calendar/ParentTask；Files/Pc/Keystats/QuickNotes 关系沿用
- 分支与异常：无
- 调用：EF

## 近逐行中文伪代码

1. auto-generated + using
2. Migration `CompletePlanningObjectModel` partial
3. `BuildTargetModel` 写注解
4. 逐实体配置列/键/索引/表名（约 59 表）
5. 规划相关：DomainProject、TaskBook、HabitRoutine/Occurrence、AvailabilityWindow、AiPlanningPlaceholder、TaskExecutionSegment、Checklist
6. Task 增加 DomainProjectId/TaskBookId 等索引
7. Mobile 实体块：设备、同步批次、使用事件/会话/聚合/目标/摘要、时间线条、应用目录与规则
8. 关系段：规划 FK + 既有 Files/Pc/User 关系
9. Navigation；结束

## 关系边
```json
{
  "nodes": [
    {
      "id": "src/Pim.Infrastructure/Data/Migrations/20260708044845_CompletePlanningObjectModel.Designer.cs",
      "label": "CompletePlanningObjectModel.Designer",
      "path": "src/Pim.Infrastructure/Data/Migrations/20260708044845_CompletePlanningObjectModel.Designer.cs",
      "doc": "docs/pseudocode/files/src/Pim.Infrastructure/Data/Migrations/20260708044845_CompletePlanningObjectModel.Designer.cs.md",
      "layer": "infrastructure",
      "kind": "other"
    }
  ],
  "edges": [
    { "from": "src/Pim.Infrastructure/Data/Migrations/20260708044845_CompletePlanningObjectModel.Designer.cs", "to": "src/Pim.Infrastructure/Data/PimDbContext.cs", "type": "depends_on" },
    { "from": "src/Pim.Infrastructure/Data/Migrations/20260708044845_CompletePlanningObjectModel.cs", "to": "src/Pim.Infrastructure/Data/Migrations/20260708044845_CompletePlanningObjectModel.Designer.cs", "type": "depends_on" },
    { "from": "src/Pim.Infrastructure/Data/Migrations/20260708044845_CompletePlanningObjectModel.Designer.cs", "to": "src/modules/Pim.Module.Calendar", "type": "depends_on" },
    { "from": "src/Pim.Infrastructure/Data/Migrations/20260708044845_CompletePlanningObjectModel.Designer.cs", "to": "src/modules/Pim.Module.Mobile", "type": "depends_on" }
  ]
}
```
