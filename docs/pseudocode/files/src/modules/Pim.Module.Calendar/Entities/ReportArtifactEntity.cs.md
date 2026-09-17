# src/modules/Pim.Module.Calendar/Entities/ReportArtifactEntity.cs

## 元信息
- 语言：C#
- 程序集或包：Pim.Module.Calendar
- 职责：报表产物实体，映射表 `report_artifacts`；实现软删除；挂载建议集合 `Suggestions`。
- 主要依赖：
  - `System.ComponentModel.DataAnnotations` / `Schema`
  - `Pim.Core.Data.ISoftDeletable`
  - 导航：`ReportSuggestionEntity`
- 被谁使用：
  - `ReportArtifactEntityConfiguration`
  - `ReportService` 生成/查询
  - 迁移 `AddReportArtifacts`

## 函数级结构化伪代码

### ReportArtifactEntity
#### 属性集合（POCO，无自定义方法）
- 输入：业务服务赋值
- 输出：EF 可跟踪行
- 副作用：无运行时逻辑
- 步骤：
  1. 表 `report_artifacts`；实现 `ISoftDeletable`。
  2. `Id` 主键 Guid 默认 NewGuid；`UserId`；`Kind` 默认 `"Daily"` MaxLength 40。
  3. 可选 `ProjectId`；`RiskLevel` 默认 `"L0AutomaticArtifact"`。
  4. jsonb：`InputsJson`/`MetricsJson` 默认 `{}`。
  5. `ContentMarkdown` 正文；`GeneratedAt`/`CreatedAt`/`UpdatedAt` 默认 UtcNow。
  6. `Status` 默认 `"Active"`；`DeletedAt` 可空。
  7. 导航 `Suggestions` 初始化空列表。
- 分支与异常：无
- 调用：无

## 近逐行中文伪代码

1. 引入 DataAnnotations、Schema、`ISoftDeletable`。
2. 命名空间 `Pim.Module.Calendar.Entities`。
3. `[Table("report_artifacts")]` 类实现 `ISoftDeletable`。
4. `Id`/`user_id`/`kind`(默认 Daily)/`project_id` 可空。
5. `risk_level` 默认 L0AutomaticArtifact。
6. `inputs_json`/`metrics_json` jsonb 默认 `{}`。
7. `content_markdown` 默认空串。
8. `generated_at`/`created_at`/`updated_at` 默认 UtcNow；`status` Active。
9. `deleted_at` 可空。
10. `Suggestions` 集合初始化。

## 关系边
```json
{
  "nodes": [
    {
      "id": "src/modules/Pim.Module.Calendar/Entities/ReportArtifactEntity.cs",
      "label": "ReportArtifactEntity",
      "path": "src/modules/Pim.Module.Calendar/Entities/ReportArtifactEntity.cs",
      "doc": "docs/pseudocode/files/src/modules/Pim.Module.Calendar/Entities/ReportArtifactEntity.cs.md",
      "layer": "module.calendar",
      "kind": "entity"
    }
  ],
  "edges": [
    { "from": "src/modules/Pim.Module.Calendar/Entities/ReportArtifactEntity.cs", "to": "src/Pim.Core/Data/ISoftDeletable.cs", "type": "implements" },
    { "from": "src/modules/Pim.Module.Calendar/Entities/ReportArtifactEntity.cs", "to": "src/modules/Pim.Module.Calendar/Entities/ReportSuggestionEntity.cs", "type": "depends_on" },
    { "from": "src/modules/Pim.Module.Calendar/Entities/CalendarEntityConfigurations.cs", "to": "src/modules/Pim.Module.Calendar/Entities/ReportArtifactEntity.cs", "type": "depends_on" },
    { "from": "src/modules/Pim.Module.Calendar/Services/ReportService.cs", "to": "src/modules/Pim.Module.Calendar/Entities/ReportArtifactEntity.cs", "type": "depends_on" }
  ]
}
```
