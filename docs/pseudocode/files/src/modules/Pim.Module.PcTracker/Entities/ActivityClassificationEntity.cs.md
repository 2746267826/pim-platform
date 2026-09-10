# src/modules/Pim.Module.PcTracker/Entities/ActivityClassificationEntity.cs

## 元信息
- 语言：C#
- 程序集或包：Pim.Module.PcTracker
- 职责：PC 活动分类快照实体，表 `pc_activity_classifications`，按 RecordKey 持久化类别/置信度/来源规则与溯源元数据。
- 主要依赖：无外部类型（纯 POCO）
- 被谁使用：`ActivityClassificationSnapshotService`、重算/审计服务、`PimDbContext`

## 函数级结构化伪代码

### ActivityClassificationEntity
#### 属性与默认值（无自定义方法）
- 输入：无（POCO）
- 输出：字段
- 副作用：无
- 步骤：
  1. 表 `pc_activity_classifications`；`Id` Guid。
  2. `RecordKey`(256)、`RecordType`(32)、`DeviceId`(128)。
  3. `SourceEventIdsJson`/`SourceBucketIdsJson` jsonb 默认 `"[]"`。
  4. `RecordKeyVersion` 默认 `"pc-fallback-v1"`；`RecordKeyStability` 默认 `"low"`。
  5. `SourceType` 默认 `"fallback"`；`InterpretationVersion` 默认 `"interpreted-aw-v1"`。
  6. `StartedAt`/`EndedAt`；`CategoryName` 默认 `"其他"`；`CategoryColor` 默认 `#64748b`。
  7. 可选 `ProjectTag`；`Confidence` 默认 0.2；`Source` 默认 `"fallback"`；可选 `SourceRuleId`。
  8. `Explanation` 默认“没有匹配到规则或启发式分类。”；`ClassifierVersion` 默认 `"local-v1"`。
  9. `ClassifiedAt` 默认 UtcNow；可选 `AuditId`。
- 分支与异常：无
- 调用：无

## 近逐行中文伪代码

1. 命名空间 `Pim.Module.PcTracker.Entities`；映射 `pc_activity_classifications`。
2. 主键与 record 键/类型/设备；源事件与 bucket JSON。
3. 键版本/稳定性/源类型/解释版本；起止时间。
4. 类别名/色、项目标签、置信度、分类来源与规则 Id。
5. 说明文案、分类器版本、分类时间、审计 Id。

## 关系边
```json
{
  "nodes": [
    {
      "id": "src/modules/Pim.Module.PcTracker/Entities/ActivityClassificationEntity.cs",
      "label": "ActivityClassificationEntity",
      "path": "src/modules/Pim.Module.PcTracker/Entities/ActivityClassificationEntity.cs",
      "doc": "docs/pseudocode/files/src/modules/Pim.Module.PcTracker/Entities/ActivityClassificationEntity.cs.md",
      "layer": "module.pctracker",
      "kind": "entity"
    }
  ],
  "edges": [
    { "from": "src/modules/Pim.Module.PcTracker/Services/ActivityClassificationSnapshotService.cs", "to": "src/modules/Pim.Module.PcTracker/Entities/ActivityClassificationEntity.cs", "type": "depends_on" },
    { "from": "src/Pim.Infrastructure/Data/PimDbContext.cs", "to": "src/modules/Pim.Module.PcTracker/Entities/ActivityClassificationEntity.cs", "type": "depends_on" }
  ]
}
```
