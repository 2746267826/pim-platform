# src/modules/Pim.Module.PcTracker/Entities/ActivityClassificationAuditEntity.cs

## 元信息
- 语言：C#
- 程序集或包：Pim.Module.PcTracker
- 职责：PC 活动分类操作审计行，映射表 `pc_activity_classification_audits`
- 主要依赖：DataAnnotations / Column 映射
- 被谁使用：活动分类服务写审计；EF `PimDbContext`

## 函数级结构化伪代码

### ActivityClassificationAuditEntity
#### 属性映射（无业务方法）
- 输入：无
- 输出：审计表字段
- 副作用：无
- 步骤：
  1. `Id` Guid 主键
  2. `Operation`（≤64）：操作类型字符串
  3. 可选关联：`RuleId`、`SuggestionId`
  4. 影响范围：`RangeMode`（≤16）、`DateFrom`/`DateTo`（≤16 字符串日期）
  5. 影响规模：`AffectedRecordCount`、`AffectedDurationSeconds`
  6. `AffectedRecordKeysJson` jsonb 默认 `[]`
  7. `CreatedByUserId` 可选；`CreatedAt` 默认 UtcNow
- 分支与异常：无
- 调用：无

## 近逐行中文伪代码

1. 引入 DataAnnotations
2. 命名空间 `Pim.Module.PcTracker.Entities`
3. 表 `pc_activity_classification_audits`
4. id Guid 主键
5. operation MaxLength 64
6. rule_id、suggestion_id 可空 Guid
7. range_mode MaxLength 16；date_from/date_to 可空 MaxLength 16
8. affected_record_count int；affected_duration_seconds double
9. affected_record_keys jsonb 默认 `[]`
10. created_by_user_id 可空；created_at 默认 UtcNow

## 关系边
```json
{
  "nodes": [
    {
      "id": "src/modules/Pim.Module.PcTracker/Entities/ActivityClassificationAuditEntity.cs",
      "label": "ActivityClassificationAuditEntity",
      "path": "src/modules/Pim.Module.PcTracker/Entities/ActivityClassificationAuditEntity.cs",
      "doc": "docs/pseudocode/files/src/modules/Pim.Module.PcTracker/Entities/ActivityClassificationAuditEntity.cs.md",
      "layer": "module.pctracker",
      "kind": "entity"
    }
  ],
  "edges": [
    { "from": "src/Pim.Infrastructure/Data/PimDbContext.cs", "to": "src/modules/Pim.Module.PcTracker/Entities/ActivityClassificationAuditEntity.cs", "type": "depends_on" }
  ]
}
```
