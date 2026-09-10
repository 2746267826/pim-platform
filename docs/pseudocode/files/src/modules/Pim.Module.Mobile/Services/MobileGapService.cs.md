# src/modules/Pim.Module.Mobile/Services/MobileGapService.cs

## 元信息
- 语言：C#
- 程序集或包：Pim.Module.Mobile
- 职责：按设备与时间范围计算移动端用量覆盖缺口（最多回填 14 天），合并事件/非回退摘要/成功同步批次覆盖，并标注缺失原因。
- 主要依赖：`PimDbContext`、`ICurrentUserService`、`TimeProvider`、Mobile 实体与 DTO、`MobileUserContext`
- 被谁使用：Mobile 模块缺口/回填相关端点

## 函数级结构化伪代码

### MobileGapService
#### 构造函数
- 输入：db、currentUser、timeProvider
- 输出：服务实例
- 副作用：保存依赖字段
- 步骤：赋值 `_db`/`_currentUser`/`_timeProvider`；`MaxBackfillAge=14d`
- 分支与异常：无
- 调用：无

#### `Task<MobileGapResponse> GetGapsAsync(MobileGapRequest request, CancellationToken ct)`
- 输入：设备、RangeStart/End、CapabilityJson
- 输出：`MaxBackfillStartUtc` + 缺口窗口列表
- 副作用：只读查询 DB
- 步骤：
  1. `userId = MobileUserContext.RequireUserId`。
  2. now；maxBackfillStart=now-14d；start=max(request.Start, maxBackfill)；end=min(request.End, now)。
  3. end≤start → 空 Windows 响应。
  4. 查询重叠的 `MobileUsageEventEntity` 源窗口 → CoverageWindow。
  5. 查询 `MobileUsageSummaryEntity` 窗口与 SourceKind。
  6. 查询 `MobileSyncBatchEntity`：FailedCount=0 且 Status=completed 的窗口。
  7. coverage = 事件 ∪ 非 fallback 摘要 ∪ 完成批次；fallbackWindows = fallback/summary 源。
  8. 按日切片 cursor→windowEnd：裁剪重叠覆盖、ContinuousCoverageEnd；全日覆盖则跳过。
  9. 否则 gapStart=coveredUntil 或 cursor；Reason(…hasFallback)；加入 GapWindow（SourcePreference=CapabilitiesJson）。
  10. 返回 MobileGapResponse。
- 分支与异常：未登录由 RequireUserId 抛出
- 调用：Clip、ContinuousCoverageEnd、Reason、IsFallbackSource

#### `Clip` / `ContinuousCoverageEnd` / `Reason` / `IsFallbackSource`
- Clip：窗口与 [start,end] 相交裁剪。
- ContinuousCoverageEnd：有序窗口从 start 连续延伸到 coveredUntil，遇间隙 break。
- Reason：尾部缺口 missing-tail；日内部分 partial-day；全日有 fallback → fallback-only 否则 missing-day。
- IsFallbackSource：SourceKind 含 fallback 或 summary（忽略大小写）。

### CoverageWindow（私有 record）
#### 起止 UTC 覆盖段
- 输入：StartUtc、EndUtc
- 输出：不可变窗口
- 副作用：无
- 步骤：数据载体
- 分支与异常：无
- 调用：无

## 近逐行中文伪代码

1. 引入 EF、Auth、Data、Mobile DTO/实体。
2. 服务持有 Db、当前用户、TimeProvider；最大回填 14 天。
3. GetGapsAsync：取 userId；钳制查询区间到 [now-14d, now]。
4. 读事件源窗口、摘要窗口、成功同步批次窗口。
5. 合并“可靠覆盖”与“仅 fallback 覆盖”。
6. 按天扫描：连续覆盖满则跳过；否则生成缺口窗口与原因。
7. Clip/ContinuousCoverageEnd 做区间合并；Reason 区分 tail/partial/fallback-only/missing-day。
8. IsFallbackSource 识别回退摘要源。

## 关系边
```json
{
  "nodes": [
    {
      "id": "src/modules/Pim.Module.Mobile/Services/MobileGapService.cs",
      "label": "MobileGapService",
      "path": "src/modules/Pim.Module.Mobile/Services/MobileGapService.cs",
      "doc": "docs/pseudocode/files/src/modules/Pim.Module.Mobile/Services/MobileGapService.cs.md",
      "layer": "module.mobile",
      "kind": "service"
    }
  ],
  "edges": [
    { "from": "src/modules/Pim.Module.Mobile/Services/MobileGapService.cs", "to": "src/Pim.Infrastructure/Data/PimDbContext.cs", "type": "depends_on" },
    { "from": "src/modules/Pim.Module.Mobile/Services/MobileGapService.cs", "to": "src/Pim.Infrastructure/Auth/CurrentUserService.cs", "type": "depends_on" },
    { "from": "src/modules/Pim.Module.Mobile/Services/MobileGapService.cs", "to": "src/modules/Pim.Module.Mobile/Services/MobileUserContext.cs", "type": "calls" },
    { "from": "src/modules/Pim.Module.Mobile/Services/MobileGapService.cs", "to": "src/modules/Pim.Module.Mobile/DTOs/MobileDtos.cs", "type": "depends_on" },
    { "from": "src/modules/Pim.Module.Mobile/Services/MobileGapService.cs", "to": "src/modules/Pim.Module.Mobile/Entities/MobileUsageEventEntity.cs", "type": "depends_on" },
    { "from": "src/modules/Pim.Module.Mobile/Services/MobileGapService.cs", "to": "src/modules/Pim.Module.Mobile/Entities/MobileUsageSummaryEntity.cs", "type": "depends_on" },
    { "from": "src/modules/Pim.Module.Mobile/Services/MobileGapService.cs", "to": "src/modules/Pim.Module.Mobile/Entities/MobileSyncBatchEntity.cs", "type": "depends_on" }
  ]
}
```
