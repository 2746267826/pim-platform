# src/Pim.Infrastructure/Endpoints/EndpointStatusEntity.cs

## 元信息
- 语言：C#
- 程序集或包：Pim.Infrastructure
- 职责：映射表 `endpoint_statuses` 的 EF 实体，记录端点设备上传/心跳与缓存计数。
- 主要依赖：`System.ComponentModel.DataAnnotations`、`DataAnnotations.Schema`
- 被谁使用：`PimDbContext.EndpointStatuses`；`EndpointStatusService` 读写与 DTO 映射

## 函数级结构化伪代码

### EndpointStatusEntity
#### 属性默认值与列映射（无实例方法）
- 输入：无
- 输出：无
- 副作用：无运行时逻辑
- 步骤：
  1. `[Table("endpoint_statuses")]`，非密封 `class`
  2. `Id`：Guid 主键，默认新 Guid
  3. `UserId`：所属用户
  4. `DeviceId`：设备 Id，最长 160
  5. `Platform`：平台，默认 `"windows"`，最长 40
  6. `AppVersion`：可空应用版本，最长 80
  7. `UploadStatus`：上传状态字符串，默认 `"Unknown"`，最长 40
  8. `CollectionCacheCount` / `OnlineOnlyBlockedCount`：计数 int
  9. `LastHeartbeatAt`：可空最近心跳
  10. `CreatedAt` / `UpdatedAt`：默认 UTC 现在
- 分支与异常：无
- 调用：属性初始化 `Guid.NewGuid`、`DateTimeOffset.UtcNow`

## 近逐行中文伪代码

1. 引入 DataAnnotations 与 Schema
2. 命名空间 `Pim.Infrastructure.Endpoints`
3. 表 `endpoint_statuses`，类 `EndpointStatusEntity`
4. `Id` → `id` 主键
5. `UserId` → `user_id`
6. `DeviceId` → `device_id` MaxLength 160
7. `Platform` → `platform` 默认 windows
8. `AppVersion` → `app_version` 可空
9. `UploadStatus` → `upload_status` 默认 Unknown
10. `CollectionCacheCount` → `collection_cache_count`
11. `OnlineOnlyBlockedCount` → `online_only_blocked_count`
12. `LastHeartbeatAt` → `last_heartbeat_at` 可空
13. `CreatedAt` → `created_at` 默认 UTC
14. `UpdatedAt` → `updated_at` 默认 UTC
15. （无业务方法）

## 关系边
```json
{
  "nodes": [
    {
      "id": "src/Pim.Infrastructure/Endpoints/EndpointStatusEntity.cs",
      "label": "EndpointStatusEntity",
      "path": "src/Pim.Infrastructure/Endpoints/EndpointStatusEntity.cs",
      "doc": "docs/pseudocode/files/src/Pim.Infrastructure/Endpoints/EndpointStatusEntity.cs.md",
      "layer": "infrastructure",
      "kind": "entity"
    }
  ],
  "edges": [
    { "from": "src/Pim.Infrastructure/Data/PimDbContext.cs", "to": "src/Pim.Infrastructure/Endpoints/EndpointStatusEntity.cs", "type": "depends_on" },
    { "from": "src/Pim.Infrastructure/Endpoints/EndpointStatusService.cs", "to": "src/Pim.Infrastructure/Endpoints/EndpointStatusEntity.cs", "type": "depends_on" },
    { "from": "src/Pim.Infrastructure/Endpoints/EndpointStatusEntity.cs", "to": "src/Pim.Core/Endpoints/EndpointDtos.cs", "type": "depends_on" }
  ]
}
```
