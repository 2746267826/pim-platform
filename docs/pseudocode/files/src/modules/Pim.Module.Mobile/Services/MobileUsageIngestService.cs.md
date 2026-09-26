# src/modules/Pim.Module.Mobile/Services/MobileUsageIngestService.cs

## 元信息
- 语言：C#
- 程序集或包：Pim.Module.Mobile
- 职责：幂等摄取移动端应用元数据、用量事件与回退摘要；写同步批次信封；重建会话并标记受影响分析陈旧。
- 主要依赖：`PimDbContext`、`ICurrentUserService`、`MobileSessionInterpreter`、`TimeProvider`、可选 `MobileAppCatalogOverrideService`、`MobileSyncBatchEnvelopeCodec`、`MobileUserContext`
- 被谁使用：Mobile 用量上传端点

## 函数级结构化伪代码

### MobileUsageIngestService
#### 构造函数
- 输入：db、currentUser、sessionInterpreter、timeProvider、可选 catalogOverrideService
- 输出：服务实例
- 副作用：保存依赖
- 步骤：字段赋值
- 分支与异常：无
- 调用：无

#### `Task<MobileUsageIngestResult> IngestAsync(MobileUsageEventsUploadRequest request, ct)`
- 输入：上传批次请求
- 输出：批次摄取结果
- 副作用：经 EF 执行策略调用 IngestAttemptAsync（可能写库）
- 步骤：RequireUserId → CreateExecutionStrategy → ExecuteAsync(IngestAttemptAsync)
- 分支与异常：策略重试由 EF 处理
- 调用：`MobileUserContext.RequireUserId`、`IngestAttemptAsync`

#### `IngestAttemptAsync(userId, request, ct)`
- 输入：用户、请求
- 输出：`MobileUsageIngestResult`
- 副作用：事务内 upsert 应用/事件/摘要；SaveChanges；重建会话；标记分析 stale；写 batch
- 步骤：
  1. Clear ChangeTracker；按 user/device/BatchId 查已有 batch → 有则 `BuildPersistedResult` 返回（幂等）。
  2. 关系库则 BeginTransaction。
  3. 新建 MobileSyncBatchEntity（status completed 初值）。
  4. 逐 App `UpsertAppAsync`；`AddEventsIfMissingAsync`；逐 Summary `UpsertSummaryAsync`。
  5. `BuildResult` 统计；batch.AcceptedCount 仅计 usage-event accepted；FailedCount；有 rejected/failed → completed-with-errors。
  6. ErrorJson = EnvelopeCodec.Serialize(itemResults, batchErrors)。
  7. SaveChanges → RebuildSessionsAsync(窗口) → MarkAffectedAnalyticsStaleAsync。
  8. Add batch 再 SaveChanges；Commit。
  9. DbUpdateException：回滚、Clear；再查 winner batch，有则返回持久化结果否则 rethrow。
  10. 其它异常：回滚、Clear、rethrow；finally Dispose 事务。
- 分支与异常：见上
- 调用：UpsertAppAsync、AddEventsIfMissingAsync、UpsertSummaryAsync、BuildResult、BuildPersistedResult、RebuildSessions、MarkAffectedAnalyticsStale

#### `UpsertAppAsync` / `AddEventsIfMissingAsync` / `UpsertSummaryAsync`
- App：校验 → Local/DB 按 package 找 → 完全匹配则 skipped duplicate → 否则 insert/update 字段 accepted。
- Events：无事件返回 []；预取时间范围内已有 EventKey；逐事件校验/去重/插入 MobileUsageEventEntity（附源窗口与 QualityFlags=[]）。
- Summary：校验 → 按 package+窗口+SourceKind 找 → 匹配 skipped → 否则 upsert 可见时长/LastUsed/RawJson。

#### 校验与键
- ValidateApp/Event/Summary/PackageName/Json：字段长度、时间顺序、JSON 可解析。
- ClientItemKey：优先客户端键，否则 package@version / event:hash / summary:hash。
- NaturalKeyHash：长度前缀拼接 + SHA256 小写 hex。
- AppMatches/SummaryMatches：字段全等（含 RawJson 默认 `{}`）。

#### `MarkAffectedAnalyticsStaleAsync`
- 若无 catalogOverrideService 直接返回；收集 apps/events/summaries 包名 Distinct；逐包 MarkAnalyticsStaleAsync(range)。

#### 辅助
- BuildResult 按 outcome 计数；BuildPersistedResult 反序列化 ErrorJson 信封或回落 Accepted/Failed 计数。
- RollbackAndDisposeAsync、Item/Rejected、NormalizeClassName、EventKey、ValidationError。

## 近逐行中文伪代码

1. 引入 CultureInfo、Cryptography、Text、Json、EF、Auth、Data、Mobile DTO/实体。
2. 服务依赖 Db、用户、会话解释器、时间、可选目录覆盖服务。
3. IngestAsync：取 userId，用执行策略跑 IngestAttemptAsync。
4. Attempt：幂等查 batch；事务内处理 apps→events→summaries；写批次信封与状态。
5. 保存后重建会话、标记分析陈旧、再保存 batch 并提交。
6. 唯一冲突类 DbUpdateException 时回读 winner batch 返回。
7. UpsertApp/Summary 与事件去重插入；校验包名/时间/JSON。
8. ClientItemKey 与 SHA256 自然键；信封编解码恢复幂等结果。

## 关系边
```json
{
  "nodes": [
    {
      "id": "src/modules/Pim.Module.Mobile/Services/MobileUsageIngestService.cs",
      "label": "MobileUsageIngestService",
      "path": "src/modules/Pim.Module.Mobile/Services/MobileUsageIngestService.cs",
      "doc": "docs/pseudocode/files/src/modules/Pim.Module.Mobile/Services/MobileUsageIngestService.cs.md",
      "layer": "module.mobile",
      "kind": "service"
    }
  ],
  "edges": [
    { "from": "src/modules/Pim.Module.Mobile/Services/MobileUsageIngestService.cs", "to": "src/Pim.Infrastructure/Data/PimDbContext.cs", "type": "depends_on" },
    { "from": "src/modules/Pim.Module.Mobile/Services/MobileUsageIngestService.cs", "to": "src/Pim.Infrastructure/Auth/CurrentUserService.cs", "type": "depends_on" },
    { "from": "src/modules/Pim.Module.Mobile/Services/MobileUsageIngestService.cs", "to": "src/modules/Pim.Module.Mobile/Services/MobileUserContext.cs", "type": "calls" },
    { "from": "src/modules/Pim.Module.Mobile/Services/MobileUsageIngestService.cs", "to": "src/modules/Pim.Module.Mobile/Services/MobileSessionInterpreter.cs", "type": "calls" },
    { "from": "src/modules/Pim.Module.Mobile/Services/MobileUsageIngestService.cs", "to": "src/modules/Pim.Module.Mobile/Services/MobileSyncBatchEnvelopeCodec.cs", "type": "calls" },
    { "from": "src/modules/Pim.Module.Mobile/Services/MobileUsageIngestService.cs", "to": "src/modules/Pim.Module.Mobile/Services/MobileAppCatalogOverrideService.cs", "type": "calls" },
    { "from": "src/modules/Pim.Module.Mobile/Services/MobileUsageIngestService.cs", "to": "src/modules/Pim.Module.Mobile/DTOs/MobileDtos.cs", "type": "depends_on" },
    { "from": "src/modules/Pim.Module.Mobile/Services/MobileUsageIngestService.cs", "to": "src/modules/Pim.Module.Mobile/Entities/MobileSyncBatchEntity.cs", "type": "depends_on" },
    { "from": "src/modules/Pim.Module.Mobile/Services/MobileUsageIngestService.cs", "to": "src/modules/Pim.Module.Mobile/Entities/MobileAppCatalogEntity.cs", "type": "depends_on" },
    { "from": "src/modules/Pim.Module.Mobile/Services/MobileUsageIngestService.cs", "to": "src/modules/Pim.Module.Mobile/Entities/MobileUsageEventEntity.cs", "type": "depends_on" },
    { "from": "src/modules/Pim.Module.Mobile/Services/MobileUsageIngestService.cs", "to": "src/modules/Pim.Module.Mobile/Entities/MobileUsageSummaryEntity.cs", "type": "depends_on" }
  ]
}
```
