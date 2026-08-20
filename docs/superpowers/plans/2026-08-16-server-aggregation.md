# PIM 服务端聚合接口（阶段 2）实现计划

> **面向 AI 代理的工作者：** 必需子技能：使用 superpowers:subagent-driven-development（推荐）或 superpowers:executing-plans 逐任务实现此计划。步骤使用复选框（`- [ ]`）语法来跟踪进度。

**目标：** 新增六类服务端聚合能力（专注块 / 应用时长 Top / 深夜使用 / 分类分布 / 常去地点 DBSCAN 聚类 / 移动统计），并把分类快照从「打开页面才生成」改为 Hangfire 后台定时补齐。

**架构：** 纯服务端 .NET 计算（固定格式、可复现，不依赖 AI 现算）。PC 聚合新建 `PcActivityAggregationService`（数据源 `pc_aw_events` / `pc_activity_classifications`），Mobile 新建 `MobileFrequentPlaceService`（DBSCAN）与移动统计扩展；全部端点匿名可读、接 `IAggregateResultCache` + force；快照补齐用 Hangfire recurring job（仿 CalendarModule 的 OutlookCalendarSyncJob 模式）。

**技术栈：** .NET 8（EF Core + Npgsql + Hangfire PostgreSQL）、Xunit + EF InMemory、手写 DBSCAN（无第三方依赖，仓库无科学计算库且不需要）。

**需求文档：** `PIM展示层与分类体系改造需求文档_20260815_2317.md` §3（服务端新增聚合接口表）+ §5.5-3（快照改后台定时聚合）。

**worktree：** `/workspace/pim-wt/agg-api`（分支 `opencode-linux/server-aggregation`，基于 master 8cee35f6）

---

## 0. 已锁定的设计决策

### 0.1 时区口径（新代码统一，不动旧端点）

- 新聚合端点接受可选 `timezone` 参数（默认 `Asia/Shanghai`），解析仿 `MobileLocationAggregationService.ResolveTimezone`（`TimeZoneInfo.FindSystemTimeZoneById` + `China Standard Time` fallback）。
- **业务日**：本地 04:00 起（与现有 `PcTrackerService.BusinessDayStartHour = 4` 一致）。业务日 D 的窗口 = `[D 04:00 local, D+1 04:00 local)`，换算成 UTC 后查询。
- 禁止在新代码使用 `GetBusinessDayStartForQuery`（它依赖服务器本地时区）或 `ToLocalTime()`；测试不得依赖运行机器时区。
- **深夜定义**：业务日 D 的深夜段 = `[D 23:30 local, D+1 04:00 local)`（需求「23:30 后」+ 业务日 4 点收口，深夜活动归当晚业务日）。

### 0.2 PC 聚合口径

- 数据源统一 `pc_aw_events`，窗口过滤 `[rangeStartUtc, rangeEndUtc)`，按 `Timestamp` 排序。
- **事件选择**：`EventType == "window"`（应用时长/专注块口径；web 事件是 URL 层，不算独立应用）；排除 `AfkStatus == "afk"` 的事件。
- **单事件时长封顶 1 小时**（`Math.Min(e.Duration, 3600)`，与现有 heatmap 口径一致，防挂机污染）。
- 应用归一：`AppNameNormalized ?? AppName` 过 `AppNameNormalizer.Normalize`；显示名经 `pc_app_signatures` 匹配（复用阶段 1 的三段式匹配：精确 → 补 .exe → glob）。
- **专注块**：非 afk window 事件按时间排序，相邻事件间隔 `<= 5 分钟` 合并（参考 `MobileTimelineBlockService.BlockBuilder` 的 `BlockMergeGap`）；块时长 = `最后事件时间 + min(其时长, 3600) - 首事件时间`；只保留 `>= 10 分钟` 的块（`MinFocusBlockMinutes`）。输出：起止 UTC/本地、时长、mainApp（块内按时长最长的 app）、topApps（前 3，含分钟）。
- **应用时长 Top**：按归一 app 名 GroupBy，Sum(封顶后时长)，按时长降序取 limit（默认 8，上限 50）；附占比（占总 window 时长）。排除总时长 `< 60 秒` 的 app（噪声）。
- **深夜使用**：按业务日 D 循环，窗口 `[D 23:30, D+1 04:00)`，非 afk window 事件封顶时长求和 → 分钟；同时给当日是否有活动的布尔值（`hadActivity`，全窗口非空）。
- **分类分布**：数据源 `pc_activity_classifications`（快照），窗口按 `StartedAt ∈ [start, end)`；每条时长 = `max(0, (EndedAt - StartedAt).TotalSeconds)` 封顶 1 小时；按 `CategoryName` 分组求和 → 分钟 + 占比；颜色取快照的 `CategoryColor`（缺失时按 `CategoryLegacyMapper.UnifiedColors` 兜底，再兜底 `#64748b`）；排序按时长降序。空数据返回空 items（不报错）。

### 0.3 常去地点 DBSCAN

- 数据源：`mobile_location_points`，过滤 `Quality != "rejected"` 且 `HorizontalAccuracyMeters <= 100`（沿用 `MobileLocationQueryService.DefaultMaxAccuracyMeters`），按 user + 可选 deviceId + 时间范围。
- **坐标处理**：经纬度 → 以数据集平均纬度为原点的局部平面坐标（equirectangular：`x = lon·cos(meanLat)·R, y = lat·R`，R=6371000），DBSCAN 在米制平面跑，质心算完再转回经纬度。
- **DBSCAN 参数（固定，不暴露 API）**：`eps = 75 米`，`minPts = 10`（GPS 精度中位数 8m，75m 覆盖停留散布；minPts=10 过滤路过的零星点）。实现标准 DBSCAN（邻域线性扫描，数据量千级可接受，不建 R-tree）。
- 输出每个聚类：中心经纬度（成员算术平均转回）、点数、到中心最大距离（半径，米）、涉及的不同本地日期数（`visitDayCount`）、isHome 标记。
- **家识别**：夜间点（本地 01:00-06:00）最多的聚类为「家」（需求要出门统计；夜间点不足时退化为点数最多的聚类）。只标记，不命名（§8 拍板：不逆地理命名建筑）。
- 噪声点不输出。

### 0.4 移动统计

- 复用 `MobileLocationAggregationService.BuildTracks/BuildSegments` 的既有输出（move 段距离、时长）。
- **homeCenter**：从 DBSCAN 家聚类取中心；无任何聚类时返回 null 且 outing 指标为 0。
- **出门（outing）**：到家中心距离 `> 150 米`（`HomeRadiusMeters`）的连续区间；区间间隔 `<= 10 分钟`合并；时长 `>= 10 分钟`计一次出门。输出：总次数、总时长（秒）、每次起止时间列表（上限 50 条）。
- **移动里程**：move 段 `DistanceMeters` 总和（jump 点已剔除，与轨迹页一致）。
- **速度峰值**：优先取点自带 `SpeedMetersPerSecond` 最大值（usable 点）；若全空，用 move 段 `DistanceMeters/DurationSeconds` 最大值。
- 按日汇总（本地日历日 00:00，业务日概念不适用于手机）：每日 outingCount / outingSeconds / distanceMeters。

### 0.5 分类快照后台补齐

- 新建 `PcClassificationBackfillService`（scoped）+ 薄壳 `PcClassificationSnapshotJob`（Hangfire 入口，仿 `OutlookCalendarSyncJob`）。
- 注册：`PcTrackerModule.InitializeAsync` 里 `RecurringJob.AddOrUpdate<PcClassificationSnapshotJob>("pc-classification-snapshot", j => j.RunAsync(), "*/30 * * * *")`（仿 CalendarModule.cs:1036-1055；`IRecurringJobManager`/`IBackgroundJobClient` 从 serviceProvider GetService，null 安全跳过——测试环境无 Hangfire）。
- **补齐算法**（`BackfillAsync(int lookbackDays = 14)`）：
  1. 业务日列表 = 最近 lookbackDays 天（含今天），时区固定 `Asia/Shanghai`（与缓存 TTL 口径一致）。
  2. 对每个业务日 D：窗口 `[D 04:00, D+1 04:00)`；查 `pc_aw_events` 窗口内事件数；为 0 → 跳过。
  3. D 是今天（或窗口含 now）→ 总是执行 ensure（增量补新 record_key）。
  4. D 是过去日 → 仅当窗口内 `pc_activity_classifications` 计数 == 0（整日缺口）才执行；部分缺口交给既有 recompute/apply/页面 ensure 路径。
  5. 执行 = 复用 `ActivityClassificationRecomputeService` 的核心路径：加载窗口事件（Duration>0）→ `BrowserPageTimelineBuilder.BuildInterpretedAwRecords` → `EnsureClassificationsAsync`（auditId: null）。为此从 `RecomputeAsync` 抽共享方法（保留原行为：手动 recompute 仍写 audit；后台补齐不写 audit）。
  6. 完成后 `IAggregateResultCache.EvictByPrefix("/api/v1/pc/")`。
- record_key 级幂等（`PcActivityRecordKeyService`）保证重入安全，无重复快照。

### 0.6 端点契约（PC 端点匿名 readGroup + cache + force；Mobile 端点沿用 Mobile 模块既有鉴权组——数据按用户隔离，与现有 location/analytics/* 一致）

```
GET /api/v1/pc/aggregation/focus-blocks?date=YYYY-MM-DD | start&end=YYYY-MM-DD &timezone=
  → { items: [{ startUtc, endUtc, startLocal, endLocal, durationMinutes, mainApp, topApps: [{ name, minutes }] }] }

GET /api/v1/pc/aggregation/app-usage?date | start&end &timezone= &limit=8
  → { items: [{ appName, displayName, totalMinutes, percentage }], totalMinutes }

GET /api/v1/pc/aggregation/late-night?start&end &timezone=
  → { items: [{ date, minutes, hadActivity }] }

GET /api/v1/pc/aggregation/category-distribution?date | start&end &timezone=
  → { items: [{ categoryName, color, minutes, percentage }] }

GET /api/v1/mobile/location/analytics/frequent-places?rangeStartUtc&rangeEndUtc&timezone&deviceId?
  → { home: { centerLat, centerLon, radiusMeters, pointCount, visitDayCount } | null,
      places: [同构 + isHome] }

GET /api/v1/mobile/location/analytics/movement-stats?rangeStartUtc&rangeEndUtc&timezone&deviceId?
  → { homeCenter: {lat,lon} | null, outingCount, outingSeconds, outings: [{startUtc,endUtc,seconds}],
      distanceMeters, maxSpeedMetersPerSecond, perDay: [{date, outingCount, outingSeconds, distanceMeters}] }
```

- PC 端点 `date`（单日）与 `start&end`（范围，含两端）二选一；`start > end`、`date` 与范围同传（以 date 为准并 400？——定：同传时忽略范围，文档写明）、`limit` 越界（钳到 [1,50]）均显式处理。
- 响应统一 `ApiResponse<T>.Ok`；缓存 key 用 `AggregateResultCacheKeys.Build(httpContext.Request, overrides: [date/start/end/timezone/limit/deviceId 规范化])`。
- Mobile 端点参数命名与现有 `MobileLocationEndpointQuery` 对齐（rangeStartUtc/rangeEndUtc/timezone/deviceId），仿 tracks 端点注册。

---

## 任务 1：PC 聚合服务骨架 + 专注块

**文件：**
- 创建：`src/modules/Pim.Module.PcTracker/Services/PcActivityAggregationService.cs`
- 创建：`src/modules/Pim.Module.PcTracker/DTOs/PcAggregationDtos.cs`
- 修改：`src/modules/Pim.Module.PcTracker/PcTrackerModule.cs`（注册服务 + focus-blocks 端点）
- 测试：`tests/Pim.UnitTests/Services/PcActivityAggregationServiceTests.cs`（新建）

- [x] **步骤 1：写失败测试**

```csharp
// tests/Pim.UnitTests/Services/PcActivityAggregationServiceTests.cs 骨架
using Microsoft.EntityFrameworkCore;
using Pim.Infrastructure.Data;
using Pim.Module.PcTracker.DTOs;
using Pim.Module.PcTracker.Entities;
using Pim.Module.PcTracker.Services;
using Xunit;

namespace Pim.UnitTests.Services;

public class PcActivityAggregationServiceTests
{
    private static readonly TimeZoneInfo Tz = TimeZoneInfo.FindSystemTimeZoneById("Asia/Shanghai");

    private static PimDbContext CreateDb()
    {
        PimDbContext.RegisterModuleAssembly(typeof(AwEventEntity).Assembly);
        var options = new DbContextOptionsBuilder<PimDbContext>()
            .UseInMemoryDatabase(Guid.NewGuid().ToString()).Options;
        return new PimDbContext(options);
    }

    private static AwEventEntity Win(string ts, double sec, string app, string title = "t") => new()
    {
        Id = Random.Shared.NextInt64(1, long.MaxValue), DeviceId = "d1",
        Timestamp = DateTimeOffset.Parse(ts), Duration = sec, EventType = "window",
        AppName = app, AppNameNormalized = AppNameNormalizer.Normalize(app),
        WindowTitle = title, AfkStatus = "not-afk", DataJson = "{}",
        CreatedAt = DateTimeOffset.Parse(ts), UpdatedAt = DateTimeOffset.Parse(ts)
    };

    [Fact]
    public async Task GetFocusBlocksAsync_MergesSmallGapsAndFiltersShortBlocks()
    {
        await using var db = CreateDb();
        // Asia/Shanghai 2026-07-10：04:00 窗口（UTC 2026-07-09T20:00 起）
        // 09:00-09:30 连续块（30min），09:33（gap 3min，合并），总计 36min → 1 块
        db.Set<AwEventEntity>().AddRange(
            Win("2026-07-10T01:00:00Z", 300, "Code.exe"),   // 09:00 local
            Win("2026-07-10T01:10:00Z", 300, "Code.exe"),
            Win("2026-07-10T01:25:00Z", 360, "Code.exe"));  // gap 15min→ 不合并？注意阈值 5min
        ...
    }
}
```

（实际测试用例由实现者按最终口径补全，至少覆盖：①gap ≤5min 合并成一块且 durationMinutes 正确；②gap >5min 切两块；③<10min 块被过滤；④afk 事件不参与；⑤mainApp/topApps 按时长排序；⑥单事件 >3600s 封顶。时间戳全部用 UTC 字面量构造，换算用 `TimeZoneInfo.ConvertTime` 断言，不用 ToLocalTime。）

- [x] **步骤 2：运行确认失败**

运行：`dotnet test Pim.sln --filter "FullyQualifiedName~PcActivityAggregationServiceTests" --no-restore`
预期：编译错误（服务/DTO 不存在）

- [x] **步骤 3：实现 DTO + 服务 + 端点**

DTO（positional record，放 `PcAggregationDtos.cs`）：

```csharp
public sealed record PcAggregationQuery(string? Date, string? Start, string? End, string? Timezone);
public sealed record PcFocusBlockItem(
    DateTimeOffset StartUtc, DateTimeOffset EndUtc, string StartLocal, string EndLocal,
    int DurationMinutes, string MainApp, IReadOnlyList<PcAggregationAppMinutes> TopApps);
public sealed record PcAggregationAppMinutes(string Name, int Minutes);
public sealed record PcFocusBlocksResponse(IReadOnlyList<PcFocusBlockItem> Items);
```

服务要点：
- `ResolveRange(PcAggregationQuery)`：date → 单业务日窗口；start/end → `[start 00:00, end+1 00:00)` 转成业务日序列窗口 `[start 04:00 local, end+1 04:00 local)`；timezone 解析（Asia/Shanghai + China Standard Time fallback）；start>end 抛 ArgumentException。
- `GetFocusBlocksAsync(query, ct)`：按 §0.2 实现（事件选择/封顶/合并/过滤/mainApp）。
- 显示名：批量拉 `pc_app_signatures` 内存三段式匹配（复用阶段 1 `ActivityLabelingService` 同款逻辑，可把匹配函数抽成 `AppSignatureMatcher` 静态类共用——若抽取，同步改 ActivityLabelingService 引用并跑其测试）。

端点（readGroup，仿现有 cache 样板）：

```csharp
readGroup.MapGet("/aggregation/focus-blocks", async (
    [FromQuery] string? date, [FromQuery] string? start, [FromQuery] string? end,
    [FromQuery] string? timezone,
    [FromServices] PcActivityAggregationService svc,
    [FromServices] IAggregateResultCache cache, HttpContext httpContext,
    [FromQuery] bool force = false, CancellationToken ct = default) =>
{
    var result = await cache.GetOrCreateAsync(
        AggregateResultCacheKeys.Build(httpContext.Request,
            overrides: [new("date", date ?? ""), new("start", start ?? ""), new("end", end ?? ""), new("timezone", timezone ?? "")]),
        force, () => svc.GetFocusBlocksAsync(new PcAggregationQuery(date, start, end, timezone), ct), ct);
    return Results.Ok(ApiResponse<PcFocusBlocksResponse>.Ok(result));
});
```

（异常处理：ArgumentException → 400，仿现有端点 try/catch。）

- [x] **步骤 4：运行测试确认通过**

运行：`dotnet test Pim.sln --filter "FullyQualifiedName~PcActivityAggregationServiceTests" --no-restore`
预期：PASS

- [x] **步骤 5：全量测试**

运行：`dotnet test Pim.sln --no-restore`。预期全绿（若 ActivityLabelingService 改用共享 AppSignatureMatcher，其测试也须全绿）。

- [x] **步骤 6：Commit**

```bash
git add src/modules/Pim.Module.PcTracker/ tests/Pim.UnitTests/Services/PcActivityAggregationServiceTests.cs
git commit -m "feat: focus block aggregation endpoint with timezone-aware business days / 专注块聚合接口（时区感知业务日）"
```

---

## 任务 2：应用时长 Top + 深夜使用

**文件：**
- 修改：`src/modules/Pim.Module.PcTracker/Services/PcActivityAggregationService.cs`
- 修改：`src/modules/Pim.Module.PcTracker/DTOs/PcAggregationDtos.cs`
- 修改：`src/modules/Pim.Module.PcTracker/PcTrackerModule.cs`（两个端点）
- 测试：`tests/Pim.UnitTests/Services/PcActivityAggregationServiceTests.cs`（追加）

- [x] **步骤 1：写失败测试**

用例至少：
1. `GetAppUsageAsync_SumsCappedDurationAndRanks`：Code.exe 3 条（其中 1 条 7200s 封顶 3600）+ Edge 1 条 → 排序 Code > Edge；percentage 之和 ≈ 100（按未取整和校验 >=99）；AppName 用 `.exe` 原值时归一合并。
2. `GetAppUsageAsync_ExcludesAfkAndWebEvents`：afk 事件与 web 事件不计入。
3. `GetAppUsageAsync_FiltersSubMinuteApps`：总时长 30s 的 app 不出现。
4. `GetAppUsageAsync_RespectsLimit`：limit=2 时只 2 项（totalMinutes 仍是全量）。
5. `GetLateNightAsync_SumsMinutesInLateWindow`：业务日 D 的 23:00（不算）、23:45（算）、次日 02:00（算，归 D）、次日 05:00（不算，属 D+1）→ minutes 正确、hadActivity=true。
6. `GetLateNightAsync_AllDaysWithNoEvents`：窗口内无事件 → minutes=0, hadActivity=false。

- [x] **步骤 2：运行确认失败**（同任务 1 模式）

- [x] **步骤 3：实现**

```csharp
public sealed record PcAppUsageItem(string AppName, string? DisplayName, int TotalMinutes, double Percentage);
public sealed record PcAppUsageResponse(IReadOnlyList<PcAppUsageItem> Items, int TotalMinutes);
public sealed record PcLateNightDayItem(string Date, int Minutes, bool HadActivity);
public sealed record PcLateNightResponse(IReadOnlyList<PcLateNightDayItem> Items);
```

- `GetAppUsageAsync(query, limit, ct)`：§0.2 口径；limit 钳 [1,50]；percentage = minutes/totalMinutes*100 保留 1 位。
- `GetLateNightAsync(query, ct)`：按业务日循环，深夜窗口 `[D 23:30, D+1 04:00)` 求和；hadActivity = 全窗口（04:00-次日04:00）有非 afk window 事件。
- 端点 `/aggregation/app-usage`（limit 参数默认 8，加入 cache overrides）与 `/aggregation/late-night`（只支持 start&end，date 单日也允许——转成 start=end=date）。

- [x] **步骤 4：测试通过 + 全量 + Commit**

```bash
git commit -m "feat: app usage ranking and late-night aggregation endpoints / 应用时长排行与深夜使用聚合接口"
```

---

## 任务 3：分类分布统计

**文件：**
- 修改：`Services/PcActivityAggregationService.cs`、`DTOs/PcAggregationDtos.cs`、`PcTrackerModule.cs`
- 测试：同文件追加

- [x] **步骤 1：写失败测试**

用例至少：
1. `GetCategoryDistributionAsync_SumsSnapshotDurations`：3 条快照（编程/折腾 30min、学习 15min、编程/折腾 15min）→ 编程/折腾 45min 60%、学习 15min 40%。
2. `GetCategoryDistributionAsync_FiltersByStartedAtWindow`：窗口外快照不计。
3. `GetCategoryDistributionAsync_CapsSingleSnapshotHour`：单条 Ended-Started=2h → 只计 60min。
4. `GetCategoryDistributionAsync_EmptyReturnsEmptyItems`：无快照 → items 空、不抛。
5. `GetCategoryDistributionAsync_FallsBackColor`：CategoryColor 为空/非法时按 UnifiedColors / #64748b 兜底。

- [x] **步骤 2：确认失败 → 步骤 3：实现**

```csharp
public sealed record PcCategoryDistributionItem(string CategoryName, string Color, int Minutes, double Percentage);
public sealed record PcCategoryDistributionResponse(IReadOnlyList<PcCategoryDistributionItem> Items);
```

数据源 `pc_activity_classifications`，时长 = clamp((EndedAt-StartedAt).TotalSeconds, 0, 3600)，按 §0.2 聚合。端点 `/aggregation/category-distribution`。

- [x] **步骤 4：测试通过 + 全量 + Commit**

```bash
git commit -m "feat: category distribution aggregation from classification snapshots / 分类分布统计接口"
```

---

## 任务 4：常去地点 DBSCAN 聚类

**文件：**
- 创建：`src/modules/Pim.Module.Mobile/Services/MobileFrequentPlaceService.cs`
- 创建：`src/modules/Pim.Module.Mobile/Services/SimpleDbscan.cs`（通用二维 DBSCAN，静态类）
- 创建：`src/modules/Pim.Module.Mobile/DTOs/MobileFrequentPlaceDtos.cs`
- 修改：`src/modules/Pim.Module.Mobile/MobileModule.cs`（frequent-places 端点）
- 测试：`tests/Pim.UnitTests/Mobile/MobileFrequentPlaceServiceTests.cs`（新建）

- [x] **步骤 1：写失败测试**

用例（用 MobileTestHelpers.CreateDb/CurrentUser + SeedPoint 模式）：
1. `ThreeTightClustersAndNoise_ProducesThreePlaces`：A 组 12 点（半径 ~20m）、B 组 15 点（~30m）、C 组 10 点（~10m）、噪声 4 点（离群 >200m）→ places=3，各 pointCount 正确，噪声不输出；中心误差 <30m。
2. `VisitDayCountCountsDistinctLocalDates`：A 组点分布在两个本地日 → visitDayCount=2。
3. `NightPointsPickHome`：A 组含多个 01:00-06:00 本地时间点、B 组全白天 → home=A 组（isHome=true）。
4. `NoClusterAboveMinPts_ReturnsEmpty`：9 个点一簇（< minPts=10）+ 噪声 → places 空、home null。
5. `RejectedAndLowAccuracyPointsExcluded`：quality=rejected 与 accuracy=150 的点不参与。
6. `CrossDevicePointsNotMerged`：deviceId 过滤时只聚该设备（传 deviceId 只返回该设备聚类）。

- [x] **步骤 2：确认失败 → 步骤 3：实现**

`SimpleDbscan`：

```csharp
public static class SimpleDbscan
{
    public sealed record Point(int Index, double X, double Y);
    public sealed record Result(IReadOnlyList<IReadOnlyList<int>> Clusters, IReadOnlyList<int> Noise);
    public static Result Run(IReadOnlyList<Point> points, double eps, int minPts) { ... }
}
```

标准算法：未访问标记 → 邻域 = 距离 <= eps 的点集（含自身）；邻域数 >= minPts 为核心点，BFS 扩展聚类；边界点归入首个触达聚类；其余为噪声。O(n²) 邻域扫描，n 千级可接受（注释说明）。

`MobileFrequentPlaceService.GetFrequentPlacesAsync(request, ct)`：§0.3 全流程（加载→过滤→投影→DBSCAN(75, 10)→质心/半径/visitDayCount→家标记→DTO）。

DTO：

```csharp
public sealed record MobileFrequentPlaceDto(
    double CenterLatitude, double CenterLongitude, double RadiusMeters,
    int PointCount, int VisitDayCount, bool IsHome);
public sealed record MobileFrequentPlacesResponse(
    MobileFrequentPlaceDto? Home, IReadOnlyList<MobileFrequentPlaceDto> Places);
```

端点仿 tracks（cache + force + [AsParameters] MobileLocationEndpointQuery）。

- [x] **步骤 4：测试通过 + 全量 + Commit**

```bash
git commit -m "feat: DBSCAN frequent place clustering endpoint / 常去地点 DBSCAN 聚类接口"
```

---

## 任务 5：移动统计（出门/里程/速度）

**文件：**
- 修改：`Services/MobileFrequentPlaceService.cs`（或并列新建 `MobileMovementStatsService`，实现者按内聚度定；倾向复用 FrequentPlaceService 的家识别）
- 修改：`DTOs/MobileFrequentPlaceDtos.cs`、`MobileModule.cs`
- 测试：`tests/Pim.UnitTests/Mobile/MobileFrequentPlaceServiceTests.cs`（追加）

- [x] **步骤 1：写失败测试**

用例：
1. `MovementStats_CountsOutingWhenLeavingHomeBeyondRadius`：家簇（夜间点确定 home）→ 走到 300m 外 30min → 回家 → outingCount=1、outingSeconds≈1800、outings 起止正确。
2. `ShortExcursionUnder10MinNotCounted`：离家 5min → 不计。
3. `OutingGapUnder10MinMerged`：离家 20min → 回 8min → 再离 20min → 1 次（间隔 <=10min 合并）。
4. `DistanceSumsMoveSegmentsOnly`：move 段距离之和（jump 剔除）= distanceMeters；stay 段不计。
5. `SpeedPeakPrefersPointSpeedField`：点带 SpeedMetersPerSecond=4.2 → maxSpeed=4.2；全空时退化为段速。
6. `NoHomeReturnsZeroOutings`：无有效聚类 → homeCenter null、outing 指标 0（distance/speed 仍返回）。
7. `PerDaySplitsByLocalCalendarDay`：跨两天的数据 → perDay 按本地 00:00 分组正确。

- [x] **步骤 2：确认失败 → 步骤 3：实现**

按 §0.4：家中心来自 DBSCAN；离家距离序列（对 usable 点按时间）→ 区间合并（>150m 开始，<=150m 结束，间隔 <=10min 桥接）→ >=10min 计次；距离复用 `MobileLocationAggregationService.BuildTracks` 的 move 段求和（该服务已 scoped 注册，直接注入复用，不要复制算法）；速度峰值逻辑如上。DTO 按 §0.6。端点 `/location/analytics/movement-stats`。

- [x] **步骤 4：测试通过 + 全量 + Commit**

```bash
git commit -m "feat: movement stats endpoint with home detection and outings / 移动统计接口（家识别与出门统计）"
```

---

## 任务 6：分类快照后台定时补齐

**文件：**
- 创建：`src/modules/Pim.Module.PcTracker/Services/PcClassificationBackfillService.cs`
- 创建：`src/modules/Pim.Module.PcTracker/Services/PcClassificationSnapshotJob.cs`
- 修改：`Services/ActivityClassificationRecomputeService.cs`（抽共享核心：load events → build records → ensure；保留 audit 参数）
- 修改：`PcTrackerModule.cs`（DI + InitializeAsync 注册 recurring job）
- 测试：`tests/Pim.UnitTests/Services/PcClassificationBackfillServiceTests.cs`（新建）

- [x] **步骤 1：写失败测试**

用例：
1. `BackfillAsync_ProcessesPastDayWithEventsButNoSnapshots`：过去业务日有 aw 事件、0 快照 → 执行后快照生成（按 record_key 计数 >0）。
2. `BackfillAsync_SkipsPastDayAlreadyClassified`：过去日已有快照 → 不重复写（快照数不变，可用固定 TimeProvider 验证「今天」不含该日）。
3. `BackfillAsync_SkipsDayWithoutEvents`：无事件日不产生快照。
4. `BackfillAsync_AlwaysProcessesCurrentDay`：今天部分快照存在 → 新事件 record_key 被补上（增量）。
5. `BackfillAsync_EvictsPcCachePrefix`：写库后调用了 `EvictByPrefix("/api/v1/pc/")`（用可替换 IAggregateResultCache 假实现记录调用）。
6. `RecomputeAsync_StillWritesAudit`（回归）：抽取共享核心后，手动 recompute 仍写 audit（保护既有行为）。

TimeProvider 用 fake（FixedTimeProvider 实现，仿 Calendar 测试的 StubTimeProvider），固定 UTC 时间使「今天」可预测。

- [x] **步骤 2：确认失败 → 步骤 3：实现**

- 从 `RecomputeAsync` 抽 `EnsureSnapshotsForRangeAsync(DateTimeOffset startUtc, DateTimeOffset endUtc, Guid? auditId, CancellationToken ct)`（internal 或 public；原方法调用它并保留 audit 逻辑）。
- `PcClassificationBackfillService.BackfillAsync(int lookbackDays, CancellationToken ct)`：§0.5 算法；注入 `TimeProvider` 与 `IAggregateResultCache`；返回处理统计（processedDays/writtenSnapshots）便于 job 日志。
- `PcClassificationSnapshotJob.RunAsync()`：调 BackfillAsync(14)，logger 记统计与异常（不抛，避免 Hangfire 重试风暴）。
- `PcTrackerModule.InitializeAsync`：`var recurring = serviceProvider.GetService<IRecurringJobManager>(); recurring?.AddOrUpdate<PcClassificationSnapshotJob>("pc-classification-snapshot", j => j.RunAsync(), "*/30 * * * *");`（DI 注册 scoped job + service；无 Hangfire 环境安全跳过）。

- [x] **步骤 4：测试通过 + 全量 + Commit**

```bash
git commit -m "feat: scheduled classification snapshot backfill via Hangfire / 分类快照后台定时补齐"
```

---

## 任务 7：收尾（全量门禁 + PR + 三视角 review + 修复循环）

- [ ] **步骤 1：全量门禁**

```bash
git -C /workspace/pim-wt/agg-api status --short --branch
dotnet test Pim.sln --no-restore
npm --prefix src/client-web run test:schedule-workbench-complete
npm --prefix src/client-web run build
git diff --check origin/master
```

- [ ] **步骤 2：push + PR**（四节双语描述：技术修改/功能变化/如何体验/测试）

- [ ] **步骤 3：CI 门禁**（gh pr checks --watch；全绿才继续）

- [ ] **步骤 4：三视角 review**（sol/terra/flash 并行， Important+ 清零循环修复）

- [ ] **步骤 5：合并后清理**（worktree remove + branch -d + master fast-forward）

---

## 明确不做（阶段 2 边界）

- 不改现有端点的时区口径（summary/timeline 的服务器本地时区问题是存量债务，另行处理；新端点自带 timezone）。
- 不建 PC 持久化聚合表（内存计算 + 缓存已够当前数据量；pc_aw_events 单日千级）。
- 不做常去地点逆地理命名（§8 拍板）。
- 不做 PC 前端 ECharts 改造（阶段 3）；本阶段只交付服务端接口。
- 不做运动细分（骑车/开车/步行，§8 边界）。
