# src/modules/Pim.Module.PcTracker/Entities/AwEventEntity.cs

## 元信息
- 语言：C#
- 程序集或包：Pim.Module.PcTracker
- 职责：ActivityWatch 风格 PC 事件 EF 实体，映射 `pc_aw_events`：窗口/AFK 等事件、应用名、桶元数据、规范化应用名与 JSON 载荷。
- 主要依赖：DataAnnotations 表列映射
- 被谁使用：`PcTrackerService`、`PcTrackerQualityService`；schema SQL/`PimDbContext` 迁移

## 函数级结构化伪代码

### AwEventEntity
#### 属性组（EF 实体 POCO，无业务方法）
- 输入：ingest/同步服务赋值
- 输出：一行 AW 事件
- 副作用：默认值在属性初始化
- 步骤：
  1. `Id` long 主键；`DeviceId`（64）；`Timestamp`；`Duration`。
  2. `EventType` 默认 `"window"`；`AppName`/`WindowTitle`/`AfkStatus` 可空。
  3. `CreatedAt` 默认 UtcNow。
  4. AW 侧元数据：`AwDeviceId`、`AwHostname`、`BucketId`、`BucketType`、`BucketClient`、`SourceEventId`。
  5. `DataJson` jsonb 默认 `"{}"`；`AppNameNormalized`；`UpdatedAt` 默认 UtcNow。
- 分支与异常：无
- 调用：被 PcTracker 服务批量 upsert/查询

## 近逐行中文伪代码

1. 命名空间 `Pim.Module.PcTracker.Entities`；`[Table("pc_aw_events")]`。
2. Id、DeviceId、Timestamp、Duration、EventType=window。
3. AppName、WindowTitle、AfkStatus、CreatedAt。
4. AwDeviceId/Hostname、BucketId/Type/Client、SourceEventId。
5. DataJson jsonb、AppNameNormalized、UpdatedAt。

## 关系边
```json
{
  "nodes": [
    {
      "id": "src/modules/Pim.Module.PcTracker/Entities/AwEventEntity.cs",
      "label": "AwEventEntity",
      "path": "src/modules/Pim.Module.PcTracker/Entities/AwEventEntity.cs",
      "doc": "docs/pseudocode/files/src/modules/Pim.Module.PcTracker/Entities/AwEventEntity.cs.md",
      "layer": "module.pctracker",
      "kind": "entity"
    }
  ],
  "edges": [
    { "from": "src/modules/Pim.Module.PcTracker/Services/PcTrackerService.cs", "to": "src/modules/Pim.Module.PcTracker/Entities/AwEventEntity.cs", "type": "depends_on" },
    { "from": "src/modules/Pim.Module.PcTracker/Services/PcTrackerQualityService.cs", "to": "src/modules/Pim.Module.PcTracker/Entities/AwEventEntity.cs", "type": "depends_on" },
    { "from": "src/modules/Pim.Module.PcTracker/Services/PcTrackerSchemaInitializer.cs", "to": "src/modules/Pim.Module.PcTracker/Entities/AwEventEntity.cs", "type": "depends_on" },
    { "from": "src/Pim.Infrastructure/Data/PimDbContext.cs", "to": "src/modules/Pim.Module.PcTracker/Entities/AwEventEntity.cs", "type": "depends_on" }
  ]
}
```
