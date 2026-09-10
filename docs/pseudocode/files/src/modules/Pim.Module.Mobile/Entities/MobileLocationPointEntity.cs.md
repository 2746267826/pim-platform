# src/modules/Pim.Module.Mobile/Entities/MobileLocationPointEntity.cs

## 元信息
- 语言：C#
- 程序集或包：Pim.Module.Mobile
- 职责：移动端定位采样点 EF 实体，映射表 `mobile_location_points`，保存经纬度、精度、速度/航向、模拟点标记、质量与原始 JSON。
- 主要依赖：`System.ComponentModel.DataAnnotations` / `Schema`（表列映射）
- 被谁使用：`MobileLocationService`、`MobileLocationAggregationService`、`MobileUsageQueryService`、`MobileQualityService`；`PimDbContext` 快照/迁移

## 函数级结构化伪代码

### MobileLocationPointEntity
#### 属性组（EF 实体 POCO，无业务方法）
- 输入：定位 ingest / 服务层赋值后由 EF 持久化
- 输出：一行定位点记录
- 副作用：属性初始化给出默认值
- 步骤：
  1. `Id` 默认 `NewGuid`；`UserId`、`DeviceId`（最长 128）标识归属。
  2. `RecordedAtUtc` 采样时刻；`Latitude`/`Longitude`/`HorizontalAccuracyMeters` 必填坐标与水平精度。
  3. `Provider`/`Source`（最长 64）描述定位提供方与业务来源。
  4. 可空运动学字段：`AltitudeMeters`、`VerticalAccuracyMeters`、`SpeedMetersPerSecond`、`SpeedAccuracyMetersPerSecond`、`BearingDegrees`、`BearingAccuracyDegrees`。
  5. `IsMock` 模拟定位标记；`Quality` 默认 `"usable"`。
  6. `RawJson` 列类型 `jsonb`，默认 `"{}"`；`CreatedAt` 默认 UtcNow。
- 分支与异常：本类型无校验逻辑
- 调用：被 Mobile 定位/聚合/质量服务读写

## 近逐行中文伪代码

1. 命名空间 `Pim.Module.Mobile.Entities`；`[Table("mobile_location_points")]` sealed 类。
2. `Id` Key；`UserId`；`DeviceId` MaxLength 128。
3. `RecordedAtUtc`；`Latitude`/`Longitude`/`HorizontalAccuracyMeters`。
4. `Provider`/`Source` MaxLength 64。
5. 可选海拔/垂直精度/速度/航向及对应精度字段。
6. `IsMock`；`Quality` 默认 usable；`RawJson` jsonb；`CreatedAt` UtcNow。

## 关系边
```json
{
  "nodes": [
    {
      "id": "src/modules/Pim.Module.Mobile/Entities/MobileLocationPointEntity.cs",
      "label": "MobileLocationPointEntity",
      "path": "src/modules/Pim.Module.Mobile/Entities/MobileLocationPointEntity.cs",
      "doc": "docs/pseudocode/files/src/modules/Pim.Module.Mobile/Entities/MobileLocationPointEntity.cs.md",
      "layer": "module.mobile",
      "kind": "entity"
    }
  ],
  "edges": [
    { "from": "src/modules/Pim.Module.Mobile/Services/MobileLocationService.cs", "to": "src/modules/Pim.Module.Mobile/Entities/MobileLocationPointEntity.cs", "type": "depends_on" },
    { "from": "src/modules/Pim.Module.Mobile/Services/MobileLocationAggregationService.cs", "to": "src/modules/Pim.Module.Mobile/Entities/MobileLocationPointEntity.cs", "type": "depends_on" },
    { "from": "src/modules/Pim.Module.Mobile/Services/MobileUsageQueryService.cs", "to": "src/modules/Pim.Module.Mobile/Entities/MobileLocationPointEntity.cs", "type": "depends_on" },
    { "from": "src/modules/Pim.Module.Mobile/Services/MobileQualityService.cs", "to": "src/modules/Pim.Module.Mobile/Entities/MobileLocationPointEntity.cs", "type": "depends_on" },
    { "from": "src/Pim.Infrastructure/Data/PimDbContext.cs", "to": "src/modules/Pim.Module.Mobile/Entities/MobileLocationPointEntity.cs", "type": "depends_on" }
  ]
}
```
