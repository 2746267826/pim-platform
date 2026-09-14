# src/modules/Pim.Module.Calendar/Entities/ReportSuggestionEntity.cs

## 元信息
- 语言：C#
- 程序集或包：Pim.Module.Calendar
- 职责：报告建议（report suggestion）EF 实体，表 `report_suggestions`，挂在 `ReportArtifactEntity` 下，存动作/摘要/变更字段与载荷 JSON、状态及可选确认 ID。
- 主要依赖：
  - DataAnnotations / Schema
  - `ReportArtifactEntity`（外键导航）
- 被谁使用：报告/建议相关服务；`PimDbContext` 映射

## 函数级结构化伪代码

### ReportSuggestionEntity
#### 属性与默认值（无自定义方法）
- 输入：无（POCO）
- 输出：持久化字段与导航
- 副作用：无
- 步骤：
  1. 表 `report_suggestions`。
  2. `Id` 默认 NewGuid；`ReportId`/`UserId` 关联报告与用户。
  3. `Action` MaxLength 120；`Summary` 文本。
  4. `ChangedFieldsJson` jsonb 默认 `"[]"`；`PayloadJson` jsonb 默认 `"{}"`。
  5. `Status` 默认 `"Open"` MaxLength 40；`ConfirmationId` 可空。
  6. `CreatedAt`/`UpdatedAt` 默认 UtcNow。
  7. FK `ReportId` → `Report` 导航（非空）。
- 分支与异常：无
- 调用：无

## 近逐行中文伪代码

1. 引入 DataAnnotations 与 Schema。
2. `[Table("report_suggestions")]` 实体类。
3. 主键、report_id、user_id、action、summary。
4. jsonb：changed_fields_json、payload_json。
5. status 默认 Open；confirmation_id 可空；时间戳。
6. ForeignKey 到 ReportArtifactEntity。

## 关系边
```json
{
  "nodes": [
    {
      "id": "src/modules/Pim.Module.Calendar/Entities/ReportSuggestionEntity.cs",
      "label": "ReportSuggestionEntity",
      "path": "src/modules/Pim.Module.Calendar/Entities/ReportSuggestionEntity.cs",
      "doc": "docs/pseudocode/files/src/modules/Pim.Module.Calendar/Entities/ReportSuggestionEntity.cs.md",
      "layer": "module.calendar",
      "kind": "entity"
    }
  ],
  "edges": [
    { "from": "src/modules/Pim.Module.Calendar/Entities/ReportSuggestionEntity.cs", "to": "src/modules/Pim.Module.Calendar/Entities/ReportArtifactEntity.cs", "type": "depends_on" },
    { "from": "src/Pim.Infrastructure/Data/PimDbContext.cs", "to": "src/modules/Pim.Module.Calendar/Entities/ReportSuggestionEntity.cs", "type": "depends_on" }
  ]
}
```
