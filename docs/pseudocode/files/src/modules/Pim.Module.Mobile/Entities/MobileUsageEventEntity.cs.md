# src/modules/Pim.Module.Mobile/Entities/MobileUsageEventEntity.cs

## 元信息
- 语言：C#
- 程序集或包：Pim.Module.Mobile
- 职责：移动端应用使用事件持久化实体（表 `mobile_usage_events`），记录前台切换等原始事件与质量标志。
- 主要依赖：
  - System.ComponentModel.DataAnnotations / Schema
- 被谁使用：`MobileUsageQueryService`（启动次数）、ingest/同步流水线、`PimDbContext`

## 函数级结构化伪代码

### MobileUsageEventEntity
#### 属性与默认值（无自定义方法）
- 输入：无（POCO）
- 输出：字段
- 副作用：无
- 步骤：
  1. 表映射 `mobile_usage_events`；主键 `Id` 默认 NewGuid。
  2. `UserId`、`DeviceId`(128)、`PackageName`(256)、`EventType`(64)。
  3. `EventTimestampUtc`；可选 `ClassName`(512)。
  4. 源窗口 `SourceWindowStartUtc` / `SourceWindowEndUtc`；`CollectedAtUtc`。
  5. jsonb：`RawJson` 默认 `{}`；`QualityFlagsJson` 默认 `[]`。
  6. `CreatedAt` 默认 UtcNow。
- 分支与异常：无
- 调用：无

## 近逐行中文伪代码

1. 命名空间 `Pim.Module.Mobile.Entities`；sealed 类。
2. 列：id、user_id、device_id、package_name、event_type、event_timestamp_utc。
3. 可选 class_name；source_window 起止；collected_at_utc。
4. raw_json / quality_flags_json（jsonb）；created_at。

## 关系边
```json
{
  "nodes": [
    {
      "id": "src/modules/Pim.Module.Mobile/Entities/MobileUsageEventEntity.cs",
      "label": "MobileUsageEventEntity",
      "path": "src/modules/Pim.Module.Mobile/Entities/MobileUsageEventEntity.cs",
      "doc": "docs/pseudocode/files/src/modules/Pim.Module.Mobile/Entities/MobileUsageEventEntity.cs.md",
      "layer": "module.mobile",
      "kind": "entity"
    }
  ],
  "edges": [
    { "from": "src/Pim.Infrastructure/Data/PimDbContext.cs", "to": "src/modules/Pim.Module.Mobile/Entities/MobileUsageEventEntity.cs", "type": "depends_on" },
    { "from": "src/modules/Pim.Module.Mobile/Services/MobileUsageQueryService.cs", "to": "src/modules/Pim.Module.Mobile/Entities/MobileUsageEventEntity.cs", "type": "depends_on" }
  ]
}
```
