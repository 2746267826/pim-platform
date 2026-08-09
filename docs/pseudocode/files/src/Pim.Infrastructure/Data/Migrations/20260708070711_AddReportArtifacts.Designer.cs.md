# src/Pim.Infrastructure/Data/Migrations/20260708070711_AddReportArtifacts.Designer.cs

## 元信息
- 语言：C#（EF Core 自动生成）
- 程序集或包：Pim.Infrastructure
- 职责：迁移 `20260708070711_AddReportArtifacts` 的目标模型快照；在完整规划/Outlook/提醒模型上纳入报告产物、建议、同步冲突与审计版本等表。
- 主要依赖：EF Core Migrations/Npgsql；`PimDbContext`；含 `Pim.Infrastructure.Audit.AuditVersionEntity`
- 被谁使用：EF 迁移管线；与 `20260708070711_AddReportArtifacts.cs` partial 配对

## 函数级结构化伪代码

### AddReportArtifacts（partial）
#### 特性与类头
- 输入：无
- 输出：Migration Id `20260708070711_AddReportArtifacts`
- 副作用：无
- 步骤：DbContext/Migration 特性 + partial 类
- 分支与异常：无
- 调用：EF

#### `BuildTargetModel(ModelBuilder modelBuilder)`
- 输入：`modelBuilder`
- 输出：约 65 张表的目标模型（本批 Designer 中实体最多之一）
- 副作用：仅内存模型
- 步骤：
  1. 模型注解 EF 8.0.11；Npgsql Identity
  2. 首实体即 `AuditVersionEntity` → `audit_versions`（索引 ConfirmationId；ObjectType+ObjectId+CreatedAt）
  3. 保留 AI/运维/规划/Mobile/Files/PcTracker/QuickNotes
  4. **相对 CompletePlanning 增量（快照中可见）**：
     - `report_artifacts` / `report_suggestions`
     - `reminders` / `reminder_deliveries`
     - `sync_conflicts`
     - Event 增加 OutlookChangeKey/OutlookEtag 等；OutlookConnection 增加 AccessTokenExpiresAt 等
  5. 关系新增：ReminderDelivery→Reminder；ReportSuggestion→ReportArtifact；其余规划/Files/Pc/User 关系沿用
- 分支与异常：无
- 调用：EF

## 近逐行中文伪代码

1. auto-generated + using
2. Migration `AddReportArtifacts` partial
3. `BuildTargetModel` 注解
4. 配置 `AuditVersionEntity`（before/after/changed_fields_json、actor/source、object_type/id）
5. 配置 AI/运维/用户等基表
6. 配置规划与 Outlook 扩展实体（含 reminders、sync_conflicts）
7. 配置 `ReportArtifactEntity`：ContentMarkdown、Kind、MetricsJson/InputsJson、ProjectId、RiskLevel、GeneratedAt 等；索引 UserId+ProjectId、UserId+Kind+GeneratedAt
8. 配置 `ReportSuggestionEntity`：ReportId、Action、PayloadJson、ConfirmationId、Status
9. Files/Mobile/PcTracker/QuickNotes 实体块
10. 关系：ReportSuggestion→ReportArtifact；ReminderDelivery→Reminder；规划/Files/Pc 等
11. Navigation；结束

## 关系边
```json
{
  "nodes": [
    {
      "id": "src/Pim.Infrastructure/Data/Migrations/20260708070711_AddReportArtifacts.Designer.cs",
      "label": "AddReportArtifacts.Designer",
      "path": "src/Pim.Infrastructure/Data/Migrations/20260708070711_AddReportArtifacts.Designer.cs",
      "doc": "docs/pseudocode/files/src/Pim.Infrastructure/Data/Migrations/20260708070711_AddReportArtifacts.Designer.cs.md",
      "layer": "infrastructure",
      "kind": "other"
    }
  ],
  "edges": [
    { "from": "src/Pim.Infrastructure/Data/Migrations/20260708070711_AddReportArtifacts.Designer.cs", "to": "src/Pim.Infrastructure/Data/PimDbContext.cs", "type": "depends_on" },
    { "from": "src/Pim.Infrastructure/Data/Migrations/20260708070711_AddReportArtifacts.cs", "to": "src/Pim.Infrastructure/Data/Migrations/20260708070711_AddReportArtifacts.Designer.cs", "type": "depends_on" },
    { "from": "src/Pim.Infrastructure/Data/Migrations/20260708070711_AddReportArtifacts.Designer.cs", "to": "src/Pim.Infrastructure/Audit/AuditVersionEntity.cs", "type": "depends_on" },
    { "from": "src/Pim.Infrastructure/Data/Migrations/20260708070711_AddReportArtifacts.Designer.cs", "to": "src/modules/Pim.Module.Calendar", "type": "depends_on" }
  ]
}
```
