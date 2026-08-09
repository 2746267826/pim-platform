# src/modules/Pim.Module.PcTracker/Entities/AwBucketEntity.cs

## 元信息
- 语言：C#
- 程序集或包：Pim.Module.PcTracker
- 职责：映射表 `pc_aw_buckets`，持久化 ActivityWatch 数据桶元数据（设备、类型、主机、源时间、附加 JSON）。
- 主要依赖：`System.ComponentModel.DataAnnotations`、`DataAnnotations.Schema`
- 被谁使用：`PimDbContext`；AW 上传/同步服务；`PcTrackerQualityService` 质量检查

## 函数级结构化伪代码

### AwBucketEntity
#### 属性默认值与列映射（无实例方法）
- 输入：无
- 输出：无
- 副作用：无运行时逻辑
- 步骤：
  1. 表名 `pc_aw_buckets`，非密封 class
  2. `Id`：long 主键
  3. `PimDeviceId`：PIM 设备 Id，最长 64
  4. `AwDeviceId`：可空 AW 设备 Id，最长 128
  5. `BucketId`：桶 Id，最长 256
  6. `Name`：可空名称
  7. `BucketType`：列 `type`，最长 64
  8. `Client`/`Hostname`：客户端与主机名
  9. `CreatedAtSource`/`LastUpdatedSource`：源侧时间可空
  10. `DataJson`：jsonb，默认 `"{}"`
  11. `SeenAt`：最近见到时间，默认 UTC 现在
- 分支与异常：无
- 调用：属性初始化 `DateTimeOffset.UtcNow`

## 近逐行中文伪代码

1. 引入 DataAnnotations 与 Schema
2. 命名空间 `Pim.Module.PcTracker.Entities`
3. 表 `pc_aw_buckets`，类 `AwBucketEntity`
4. `Id` → `id` 主键 long
5. `PimDeviceId` → `pim_device_id`
6. `AwDeviceId` → `aw_device_id` 可空
7. `BucketId` → `bucket_id`
8. `Name` → `name` 可空
9. `BucketType` → `type`
10. `Client`/`Hostname` 映射
11. 源创建/更新时间可空
12. `DataJson` jsonb 默认空对象
13. `SeenAt` 默认 UTC 现在
14. （无业务方法）

## 关系边
```json
{
  "nodes": [
    {
      "id": "src/modules/Pim.Module.PcTracker/Entities/AwBucketEntity.cs",
      "label": "AwBucketEntity",
      "path": "src/modules/Pim.Module.PcTracker/Entities/AwBucketEntity.cs",
      "doc": "docs/pseudocode/files/src/modules/Pim.Module.PcTracker/Entities/AwBucketEntity.cs.md",
      "layer": "module.pctracker",
      "kind": "entity"
    }
  ],
  "edges": [
    { "from": "src/Pim.Infrastructure/Data/PimDbContext.cs", "to": "src/modules/Pim.Module.PcTracker/Entities/AwBucketEntity.cs", "type": "depends_on" },
    { "from": "src/modules/Pim.Module.PcTracker/Services/PcTrackerQualityService.cs", "to": "src/modules/Pim.Module.PcTracker/Entities/AwBucketEntity.cs", "type": "depends_on" }
  ]
}
```
