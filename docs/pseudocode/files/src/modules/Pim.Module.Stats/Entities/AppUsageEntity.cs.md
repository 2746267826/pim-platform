# src/modules/Pim.Module.Stats/Entities/AppUsageEntity.cs

## 元信息
- 语言：C#
- 程序集或包：Pim.Module.Stats
- 职责：应用使用时段实体（表 `app_usage`），记录设备上某包名的起止时间与时长。
- 主要依赖：无（纯 POCO）
- 被谁使用：`StatsService`、`PimDbContext`、Stats 模块聚合/查询

## 函数级结构化伪代码

### AppUsageEntity
#### 属性与默认值（无自定义方法）
- 输入：无（POCO）
- 输出：字段
- 副作用：无
- 步骤：
  1. 主键 `Id` long。
  2. `DeviceId` MaxLength 64；`PackageName` MaxLength 256。
  3. `StartTime` / `EndTime` / `LastTimeUsed` 为 DateTimeOffset。
  4. `DurationMs` long 时长毫秒。
  5. `CreatedAt` 默认 UtcNow。
- 分支与异常：无
- 调用：无

## 近逐行中文伪代码

1. 表 `app_usage`。
2. 列：id、device_id、package_name、start_time、end_time、duration_ms、last_time_used、created_at。
3. 字符串字段带 MaxLength；CreatedAt 默认当前 UTC。

## 关系边
```json
{
  "nodes": [
    {
      "id": "src/modules/Pim.Module.Stats/Entities/AppUsageEntity.cs",
      "label": "AppUsageEntity",
      "path": "src/modules/Pim.Module.Stats/Entities/AppUsageEntity.cs",
      "doc": "docs/pseudocode/files/src/modules/Pim.Module.Stats/Entities/AppUsageEntity.cs.md",
      "layer": "module.stats",
      "kind": "entity"
    }
  ],
  "edges": [
    { "from": "src/Pim.Infrastructure/Data/PimDbContext.cs", "to": "src/modules/Pim.Module.Stats/Entities/AppUsageEntity.cs", "type": "depends_on" },
    { "from": "src/modules/Pim.Module.Stats/Services/StatsService.cs", "to": "src/modules/Pim.Module.Stats/Entities/AppUsageEntity.cs", "type": "depends_on" }
  ]
}
```
