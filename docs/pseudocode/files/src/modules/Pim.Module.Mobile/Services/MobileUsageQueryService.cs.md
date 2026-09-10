# src/modules/Pim.Module.Mobile/Services/MobileUsageQueryService.cs

## 元信息
- 语言：C#
- 程序集或包：Pim.Module.Mobile
- 职责：查询移动使用汇总与时间线：聚合 summary/session/event/sync batch/location 点，产出排行、完整性与 fallback 时间线条目。
- 主要依赖：
  - `PimDbContext`、`ICurrentUserService`、`TimeProvider`
  - 实体：UsageSummary/Session/Event、SyncBatch、LocationPoint、AppCatalog
  - DTO、`MobileUserContext`、`MobileSyncBatchEnvelopeCodec`
- 被谁使用：Mobile 使用统计相关端点

## 函数级结构化伪代码

### MobileUsageQueryService
#### 构造 `(db, currentUser, timeProvider)`
- 注入依赖

#### `GetSummaryAsync(query, ct)`
- 输入：`MobileSummaryQuery`（可选 DeviceId、时间范围）
- 输出：`MobileUsageSummaryResponse`
- 副作用：只读 DB
- 步骤：
  1. RequireUserId；查 `MobileUsageSummaryEntity` 按用户/设备/窗口重叠过滤。
  2. 并行辅助：AppCatalog、SessionCounts、LaunchCounts、SyncBatches、LocationPoints。
  3. 总可见时长 ms→秒；fallback 源时长；appSwitch=session 计数和；appsUsed=包名数。
  4. failedBatch：Status≠completed 或 FailedCount>0。
  5. 按 PackageName 分组排行 Top50：前台秒、session/launch、最新 LastTimeUsed、source events|fallback、占比。
  6. 同步批次 Top20：附窗口内 location 接受/拒绝计数与 ErrorMessage。
  7. Completeness(total, fallback, failedBatchCount)；GeneratedAt=now。
- 分支与异常：未登录由 MobileUserContext
- 调用：AppCatalog、SessionCounts、LaunchCounts、SyncBatches、LocationPoints、Completeness

#### `GetTimelineAsync(query, ct)`
- 步骤：
  1. 查 sessions（重叠范围，OrderBy StartUtc Take 500）→ kind=session, source=events, confidence=1。
  2. 查 fallback summaries（WhereFallbackSummaries，Take 500）→ kind=fallback, confidence=0.6, note=「汇总数据」。
  3. AppCatalog 解析 DisplayName；合并 items 按 Start/Package 排序。
  4. 响应含 sessionItems、fallbackItems、合并 items。
- 调用：WhereFallbackSummaries、AppCatalog、DisplayName、DurationMs

#### `AppCatalog(userId, deviceId, packageNames, ct)` private
- 空包名 → 空字典；按用户+包名（可选设备）取每包最新 UpdatedAt 一条

#### `WhereFallbackSummaries` static public
- SourceKind 小写含 fallback 或 summary

#### `SessionCounts` / `LaunchCounts` private
- Session：按包计条数（时间重叠）
- Launch：EventType 含 FOREGROUND 的事件按包计数

#### `SyncBatches` / `LocationPoints` private
- 用户+设备+窗口重叠过滤列表

#### `IsFallbackSource` / `Completeness` / `DateLabel` / `DisplayName` / `DurationMs` static
- Completeness：无时长 0；全 fallback → 0.65 否则 1；每 issue -0.1 最多 -0.3
- DateLabel：RangeStartUtc 或 UtcNow 的 yyyy-MM-dd
- DurationMs：无 end → 0

## 近逐行中文伪代码

1. 注入 db、currentUser、timeProvider。
2. **汇总**：summary 行聚合时长与排行；session/event 补切换与启动；batch+location 补同步与质量。
3. **时间线**：真实 session 优先，fallback 汇总补洞；各最多 500 条后合并排序。
4. **目录**：包名 → 最新 DisplayName/Category。
5. **完整性**：fallback 降权 + 失败批次惩罚。
6. 过滤统一：UserId、可选 DeviceId、时间窗重叠。

## 关系边
```json
{
  "nodes": [
    {
      "id": "src/modules/Pim.Module.Mobile/Services/MobileUsageQueryService.cs",
      "label": "MobileUsageQueryService",
      "path": "src/modules/Pim.Module.Mobile/Services/MobileUsageQueryService.cs",
      "doc": "docs/pseudocode/files/src/modules/Pim.Module.Mobile/Services/MobileUsageQueryService.cs.md",
      "layer": "module.mobile",
      "kind": "service"
    }
  ],
  "edges": [
    { "from": "src/modules/Pim.Module.Mobile/Services/MobileUsageQueryService.cs", "to": "src/Pim.Infrastructure/Data/PimDbContext.cs", "type": "depends_on" },
    { "from": "src/modules/Pim.Module.Mobile/Services/MobileUsageQueryService.cs", "to": "src/Pim.Infrastructure/Auth/CurrentUserService.cs", "type": "depends_on" },
    { "from": "src/modules/Pim.Module.Mobile/Services/MobileUsageQueryService.cs", "to": "src/modules/Pim.Module.Mobile/Services/MobileUserContext.cs", "type": "calls" },
    { "from": "src/modules/Pim.Module.Mobile/Services/MobileUsageQueryService.cs", "to": "src/modules/Pim.Module.Mobile/Entities/MobileUsageEventEntity.cs", "type": "depends_on" },
    { "from": "src/modules/Pim.Module.Mobile/Services/MobileUsageQueryService.cs", "to": "src/modules/Pim.Module.Mobile/Entities", "type": "depends_on" },
    { "from": "src/modules/Pim.Module.Mobile/Services/MobileUsageQueryService.cs", "to": "src/modules/Pim.Module.Mobile/DTOs", "type": "depends_on" }
  ]
}
```
