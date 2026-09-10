# src/Pim.Infrastructure/Data/Entities/DaemonHeartbeatEntity.cs

## 元信息
- 语言：C#
- 程序集或包：Pim.Infrastructure
- 职责：映射表 `daemon_heartbeats` 的 EF 实体，持久化 Windows/其他守护程序心跳与采集源状态。
- 主要依赖：`System.ComponentModel.DataAnnotations`、`DataAnnotations.Schema`；`Pim.Core.Operations.DaemonSourceState`（默认状态字符串）
- 被谁使用：`PimDbContext.DaemonHeartbeats`；`DaemonHeartbeatService` 写入；`SystemStatusService`、`PcTrackerQualityService`、`MobileQualityService` 查询

## 函数级结构化伪代码

### DaemonHeartbeatEntity
#### 属性默认值与列映射（无实例方法）
- 输入：无（POCO 属性由调用方/EF 赋值）
- 输出：无
- 副作用：无运行时逻辑；注解驱动表/列/主键/长度
- 步骤：
  1. 类标注 `[Table("daemon_heartbeats")]`
  2. `Id`：`Guid` 主键，默认 `Guid.NewGuid()`
  3. `DeviceId`：设备 Id，最长 128，默认空串
  4. `DaemonKind`：守护种类，最长 32，默认 `"windows"`
  5. `Version`：守护版本，最长 64
  6. `ServerUrl`：上报目标服务 URL，最长 512
  7. `LastSuccessfulUploadAt` / `LastAttemptedUploadAt`：可空上传时间
  8. `LastError`：可空错误文本
  9. `UploadQueueCount`：可空队列长度
  10. `ActivityWatchState` / `KeyStatsState`：源状态字符串，默认 `DaemonSourceState.Unknown.ToString()`
  11. `CollectionPaused`：是否暂停采集
  12. `StatusJson`：`jsonb` 扩展状态，默认 `"{}"`
  13. `ReceivedAt`：服务端接收时间，默认 `DateTimeOffset.UtcNow`
- 分支与异常：无
- 调用：无（属性初始化表达式调用 `Guid.NewGuid`、`ToString`、`UtcNow`）

## 近逐行中文伪代码

1. 引入 DataAnnotations 与 Schema
2. 引入 `Pim.Core.Operations`
3. 命名空间 `Pim.Infrastructure.Data.Entities`
4. 表名 `daemon_heartbeats`，密封类 `DaemonHeartbeatEntity`
5. `Id` → 列 `id`，主键，默认新 Guid
6. `DeviceId` → `device_id`，MaxLength 128
7. `DaemonKind` → `daemon_kind`，默认 windows
8. `Version` → `version`
9. `ServerUrl` → `server_url`
10. `LastSuccessfulUploadAt` → `last_successful_upload_at` 可空
11. `LastAttemptedUploadAt` → `last_attempted_upload_at` 可空
12. `LastError` → `last_error` 可空
13. `UploadQueueCount` → `upload_queue_count` 可空 int
14. `ActivityWatchState` → `activity_watch_state`，默认 Unknown
15. `KeyStatsState` → `key_stats_state`，默认 Unknown
16. `CollectionPaused` → `collection_paused` bool
17. `StatusJson` → `status_json` jsonb，默认 `{}`
18. `ReceivedAt` → `received_at`，默认 UTC 现在
19. （无方法体；由服务层填充后 `SaveChanges`）

## 关系边
```json
{
  "nodes": [
    {
      "id": "src/Pim.Infrastructure/Data/Entities/DaemonHeartbeatEntity.cs",
      "label": "DaemonHeartbeatEntity",
      "path": "src/Pim.Infrastructure/Data/Entities/DaemonHeartbeatEntity.cs",
      "doc": "docs/pseudocode/files/src/Pim.Infrastructure/Data/Entities/DaemonHeartbeatEntity.cs.md",
      "layer": "infrastructure",
      "kind": "entity"
    }
  ],
  "edges": [
    { "from": "src/Pim.Infrastructure/Data/Entities/DaemonHeartbeatEntity.cs", "to": "src/Pim.Core/Operations/OperationEnums.cs", "type": "depends_on" },
    { "from": "src/Pim.Infrastructure/Data/PimDbContext.cs", "to": "src/Pim.Infrastructure/Data/Entities/DaemonHeartbeatEntity.cs", "type": "depends_on" },
    { "from": "src/Pim.Infrastructure/Operations/DaemonHeartbeatService.cs", "to": "src/Pim.Infrastructure/Data/Entities/DaemonHeartbeatEntity.cs", "type": "depends_on" },
    { "from": "src/Pim.Infrastructure/Operations/SystemStatusService.cs", "to": "src/Pim.Infrastructure/Data/Entities/DaemonHeartbeatEntity.cs", "type": "depends_on" },
    { "from": "src/modules/Pim.Module.PcTracker/Services/PcTrackerQualityService.cs", "to": "src/Pim.Infrastructure/Data/Entities/DaemonHeartbeatEntity.cs", "type": "depends_on" },
    { "from": "src/modules/Pim.Module.Mobile/Services/MobileQualityService.cs", "to": "src/Pim.Infrastructure/Data/Entities/DaemonHeartbeatEntity.cs", "type": "depends_on" }
  ]
}
```
