# src/modules/Pim.Module.Mobile/Entities/MobileSyncBatchEntity.cs

## 元信息
- 语言：C#
- 程序集或包：Pim.Module.Mobile
- 职责：移动端同步批次持久化实体，表 `mobile_sync_batches`，记录设备某次上传窗口的接受/失败计数与状态。
- 主要依赖：`System.ComponentModel.DataAnnotations`、Schema 列映射
- 被谁使用：`MobileUsageAggregationService`（失败批次计数、最近同步时间）、Mobile 同步写入路径、`PimDbContext`

## 函数级结构化伪代码

### MobileSyncBatchEntity
#### 属性与默认值（无自定义方法）
- 输入：无（POCO）
- 输出：表行字段
- 副作用：无
- 步骤：
  1. 映射表 `mobile_sync_batches`；`Id` 主键 Guid，默认 `NewGuid()`。
  2. `UserId` 所属用户；`DeviceId` 最长 128；`BatchId` 最长 128。
  3. `WindowStartUtc` / `WindowEndUtc` 同步时间窗。
  4. `AcceptedCount` / `FailedCount` 接受与失败条数。
  5. `Status` 最长 32，默认 `"completed"`。
  6. `ErrorJson` jsonb，默认 `"{}"`。
  7. `CreatedAt` 默认 UtcNow；`CompletedAtUtc` 可空完成时间。
- 分支与异常：无运行时逻辑
- 调用：无

## 近逐行中文伪代码

1. 引入 DataAnnotations / Schema。
2. 命名空间 `Pim.Module.Mobile.Entities`；sealed 类映射 `mobile_sync_batches`。
3. Id、UserId、DeviceId、BatchId。
4. WindowStartUtc、WindowEndUtc、AcceptedCount、FailedCount。
5. Status 默认 completed；ErrorJson 默认空对象。
6. CreatedAt、CompletedAtUtc 时间戳。

## 关系边
```json
{
  "nodes": [
    {
      "id": "src/modules/Pim.Module.Mobile/Entities/MobileSyncBatchEntity.cs",
      "label": "MobileSyncBatchEntity",
      "path": "src/modules/Pim.Module.Mobile/Entities/MobileSyncBatchEntity.cs",
      "doc": "docs/pseudocode/files/src/modules/Pim.Module.Mobile/Entities/MobileSyncBatchEntity.cs.md",
      "layer": "module.mobile",
      "kind": "entity"
    }
  ],
  "edges": [
    { "from": "src/Pim.Infrastructure/Data/PimDbContext.cs", "to": "src/modules/Pim.Module.Mobile/Entities/MobileSyncBatchEntity.cs", "type": "depends_on" },
    { "from": "src/modules/Pim.Module.Mobile/Services/MobileUsageAggregationService.cs", "to": "src/modules/Pim.Module.Mobile/Entities/MobileSyncBatchEntity.cs", "type": "depends_on" }
  ]
}
```
