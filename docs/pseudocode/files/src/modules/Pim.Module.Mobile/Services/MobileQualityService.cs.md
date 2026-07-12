# src/modules/Pim.Module.Mobile/Services/MobileQualityService.cs

## 元信息
- 语言：C#
- 程序集或包：Pim.Module.Mobile
- 职责：聚合 Android 心跳、使用事件/汇总、同步批次、定位与应用元数据，输出移动端采集质量诊断（组件状态 + 问题列表 + 下一步）。
- 主要依赖：`PimDbContext`、`ICurrentUserService`、`TimeProvider`、Mobile 实体与 DTO、`DaemonHeartbeatEntity`、`PimHealthStatus`
- 被谁使用：Mobile 模块质量相关 HTTP 端点

## 函数级结构化伪代码

### MobileQualityService
#### 构造 `MobileQualityService(db, currentUser, timeProvider)`
- 输入：DbContext、当前用户、时间源
- 输出：服务实例
- 副作用：保存字段；`StaleHeartbeatAge = 30 分钟`
- 步骤：注入依赖
- 分支与异常：无
- 调用：无

#### `GetQualityAsync(rangeStartUtc?, rangeEndUtc?, ct, deviceId?)`
- 输入：可选时间范围、取消令牌、可选设备过滤
- 输出：`MobileQualityResponse`（overall、label、message、checkedAt、components、issues、nextSteps）
- 副作用：只读查询 DB
- 步骤：
  1. `MobileUserContext.RequireUserId` 取当前用户
  2. `checkedAt = GetUtcNow()`；默认 rangeEnd=now、rangeStart=end-1d；若 end<start 则交换
  3. 规范化 deviceId（空白→null）
  4. 查已注册 `MobileDeviceEntity` 设备 ID 列表（可按 device 过滤）
  5. 若有设备：查最新 `DaemonHeartbeatEntity`（DaemonKind=`android`，DeviceId∈列表）
  6. 查范围内 `MobileUsageEventEntity` 包名列表
  7. 查与范围相交的 `MobileUsageSummaryEntity`（PackageName、SourceKind）
  8. 统计 eventCount；fallbackSummaryCount = SourceKind 含 fallback/summary
  9. 查相交 `MobileSyncBatchEntity`、范围内 `MobileLocationPointEntity`
  10. 合并事件+汇总包名集合；对照 `MobileAppCatalogEntity` 算 missingAppMetadataCount
  11. 依次 `CheckHeartbeat/Usage/Sync/Location/AppMetadata` 填充 components 与 issues
  12. overall = 各组件 Status 按 SeverityRank 最高者
  13. 组装 Response：Label/Message、issues 的去重 NextStep 列表
- 分支与异常：无设备则 heartbeat=null；各 Check 内部分支
- 调用：EF `Set<>`、`Check*`、`IsFallbackSource`、`SeverityRank`、`Label`、`Message`

#### `CheckHeartbeat(heartbeat?, checkedAt, issues)`
- 输入：最新心跳、检查时间、问题列表（可变）
- 输出：心跳组件 DTO
- 副作用：可能向 issues 追加
- 步骤：
  1. null → Unknown + issue `mobile-heartbeat-missing`
  2. age>30m → Warning stale；LastError 非空 → Warning error；UploadQueueCount>0 → Warning queue
  3. 任一异常则 Warning 否则 Healthy；message 按优先级选择
  4. details：deviceId、receivedAt、lastSuccessfulUploadAt、uploadQueueCount、lastError
- 分支与异常：无抛出
- 调用：`Component`

#### `CheckUsage(eventCount, fallbackSummaryCount, checkedAt, issues)`
- 输入：事件数、fallback 汇总数
- 输出：使用采集组件
- 副作用：可能追加 issues
- 步骤：
  1. 两者皆 0 → Unknown `mobile-usage-missing`
  2. fallback>0 → Warning `mobile-usage-fallback-only`（文案区分仅有汇总 vs 混有事件）
  3. 否则 Healthy
- 分支与异常：无
- 调用：`Component`

#### `CheckSync(batches, checkedAt, issues)`
- 输入：同步批次集合
- 输出：同步组件
- 副作用：可能追加 issues
- 步骤：
  1. 空 → Unknown `mobile-sync-missing`
  2. failed = FailedCount>0 或 Status≠completed（忽略大小写）
  3. failed>0 → Warning；details 含 batchCount/failedBatchCount/acceptedCount 总和
- 分支与异常：无
- 调用：`Component`

#### `CheckLocation(locations, checkedAt, issues)`
- 输入：定位点集合
- 输出：定位组件
- 副作用：可能追加 issues
- 步骤：
  1. 空 → Unknown `mobile-location-missing`
  2. Quality=`rejected` 计数；rejected>0 → Warning
  3. locationPointCount 记可用点数（总数-拒绝）
- 分支与异常：无
- 调用：`Component`

#### `CheckAppMetadata(appMetadataCount, usedPackageCount, missingAppMetadataCount, checkedAt, issues)`
- 输入：元数据数量、使用包数、缺失数
- 输出：应用元数据组件
- 副作用：appMetadataCount==0 或 missing>0 时追加 issue
- 步骤：missing>0→Warning；仅无元数据→Unknown；否则 Healthy
- 分支与异常：无
- 调用：`Component`

#### 辅助 `Component` / `IsFallbackSource` / `SeverityRank` / `Label` / `Message`
- 输入：状态或 sourceKind 等
- 输出：DTO / bool / 排序分 / 中文标签与总述
- 副作用：无
- 步骤：SeverityRank Healthy0 Unknown1 Warning2 Critical3；Label/Message 映射 overall
- 分支与异常：默认 Unknown 文案
- 调用：无

## 近逐行中文伪代码

1. 注入 Db、当前用户、TimeProvider；陈旧心跳阈值 30 分钟
2. GetQualityAsync：取 userId 与时间窗，可选 device 过滤
3. 读注册设备 → 最新 android 心跳
4. 读使用事件包名、汇总行 SourceKind、同步批次、定位点
5. 合并包名对照应用目录算缺失元数据
6. 五路 Check 产出组件与 issues；按严重度取 overall
7. CheckHeartbeat：缺失/过期/LastError/上传队列
8. CheckUsage：无数据 Unknown；fallback 汇总 Warning
9. CheckSync：无批次 Unknown；失败或非 completed Warning
10. CheckLocation：无点 Unknown；rejected Warning
11. CheckAppMetadata：缺失或为空 Warning/Unknown
12. Component 打包；IsFallbackSource 看 fallback/summary 子串
13. SeverityRank 与 Label/Message 中文总览

## 关系边
```json
{
  "nodes": [
    {
      "id": "src/modules/Pim.Module.Mobile/Services/MobileQualityService.cs",
      "label": "MobileQualityService",
      "path": "src/modules/Pim.Module.Mobile/Services/MobileQualityService.cs",
      "doc": "docs/pseudocode/files/src/modules/Pim.Module.Mobile/Services/MobileQualityService.cs.md",
      "layer": "module.mobile",
      "kind": "service"
    }
  ],
  "edges": [
    { "from": "src/modules/Pim.Module.Mobile/Services/MobileQualityService.cs", "to": "src/Pim.Infrastructure/Data/PimDbContext.cs", "type": "depends_on" },
    { "from": "src/modules/Pim.Module.Mobile/Services/MobileQualityService.cs", "to": "src/Pim.Infrastructure/Auth/CurrentUserService.cs", "type": "calls" },
    { "from": "src/modules/Pim.Module.Mobile/Services/MobileQualityService.cs", "to": "src/modules/Pim.Module.Mobile/Services/MobileUserContext.cs", "type": "calls" },
    { "from": "src/modules/Pim.Module.Mobile/Services/MobileQualityService.cs", "to": "src/modules/Pim.Module.Mobile/Entities/MobileUsageSummaryEntity.cs", "type": "depends_on" },
    { "from": "src/modules/Pim.Module.Mobile/Services/MobileQualityService.cs", "to": "src/modules/Pim.Module.Mobile/Entities/MobileUsageEventEntity.cs", "type": "depends_on" },
    { "from": "src/modules/Pim.Module.Mobile/Services/MobileQualityService.cs", "to": "src/modules/Pim.Module.Mobile/Entities/MobileDeviceEntity.cs", "type": "depends_on" },
    { "from": "src/modules/Pim.Module.Mobile/Services/MobileQualityService.cs", "to": "src/modules/Pim.Module.Mobile/Entities/MobileSyncBatchEntity.cs", "type": "depends_on" },
    { "from": "src/modules/Pim.Module.Mobile/Services/MobileQualityService.cs", "to": "src/modules/Pim.Module.Mobile/Entities/MobileLocationPointEntity.cs", "type": "depends_on" },
    { "from": "src/modules/Pim.Module.Mobile/Services/MobileQualityService.cs", "to": "src/modules/Pim.Module.Mobile/Entities/MobileAppCatalogEntity.cs", "type": "depends_on" },
    { "from": "src/modules/Pim.Module.Mobile/Services/MobileQualityService.cs", "to": "src/Pim.Infrastructure/Data/Entities/DaemonHeartbeatEntity.cs", "type": "depends_on" },
    { "from": "src/modules/Pim.Module.Mobile/Services/MobileQualityService.cs", "to": "src/Pim.Core/Operations", "type": "depends_on" },
    { "from": "src/modules/Pim.Module.Mobile", "to": "src/modules/Pim.Module.Mobile/Services/MobileQualityService.cs", "type": "calls" }
  ]
}
```
