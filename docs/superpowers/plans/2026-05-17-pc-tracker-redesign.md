# PC记录 页面重设计 实现计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 将 PC记录 页面从单文件5面板 MVP 升级为 4 模块 + 时间线 + 分类系统 + 设置页查询模块的完整功能页面。

**Architecture:** 后端新增 `pc_app_categories` 表和分类匹配服务，扩展 `PcTrackerService` 加入衍生指标计算和详情查询；前端拆分 `PcTrackerPage.tsx` 为6个独立组件，新增 `PcDetailQueryPage` 设置页。

**Tech Stack:** C# / ASP.NET Core Minimal API / EF Core / PostgreSQL、React / TypeScript / React Query / Tailwind CSS

---

## 文件结构

```
# --- 后端 ---
src/modules/Pim.Module.PcTracker/
├── Entities/
│   ├── KeystatsDailyEntity.cs          # [已有，不修改]
│   ├── KeystatsKeyCountEntity.cs       # [已有，不修改]
│   ├── KeystatsAppBreakdownEntity.cs   # [已有，不修改]
│   ├── AwEventEntity.cs                # [已有，不修改]
│   ├── AppCategoryEntity.cs            # [新建]
│   └── EntityConfigurations.cs         # [修改：添加 AppCategoryEntity 配置]
├── DTOs/PcTrackerDtos.cs               # [修改：添加新 DTO]
├── Services/PcTrackerService.cs        # [修改：添加分类/衍生指标/详情查询]
├── PcTrackerModule.cs                  # [修改：添加新端点]
└── Pim.Module.PcTracker.csproj         # [已有，不修改]

src/Pim.Api/
└── seed_pc_tables.sql                  # [修改：添加 pc_app_categories DDL + 种子数据]

# --- 前端 ---
src/client-web/src/
├── pages/
│   ├── PcTrackerPage.tsx               # [重写：从 ~345 行精简为容器组件]
│   ├── PcDetailQueryPage.tsx           # [新建]
│   └── SettingsPage.tsx                # [修改：添加 PC记录 卡片入口]
├── components/pc-tracker/
│   ├── DateDimensionBar.tsx            # [新建]
│   ├── ActivityHeatmap.tsx             # [新建]
│   ├── CategoryTimeline.tsx            # [新建]
│   ├── DailyActivityPanel.tsx          # [新建]
│   ├── KeyboardHeatmap.tsx             # [新建]
│   └── PcDetailQueryPanel.tsx          # [新建]
├── api/
│   └── pcTracker.ts                    # [修改：添加新 API 函数]
├── layout/
│   └── AppLayout.tsx                   # [修改：添加 /settings/pc-data 路由]
└── types/
    └── index.ts                        # [修改：添加新类型]
```

---

### Task 1: 数据库 — 创建 pc_app_categories 表和实体

**Files:**
- Create: `src/modules/Pim.Module.PcTracker/Entities/AppCategoryEntity.cs`
- Modify: `src/modules/Pim.Module.PcTracker/Entities/EntityConfigurations.cs`
- Modify: `src/Pim.Api/seed_pc_tables.sql`

- [ ] **Step 1: 创建 AppCategoryEntity**

```csharp
// src/modules/Pim.Module.PcTracker/Entities/AppCategoryEntity.cs
namespace Pim.Module.PcTracker.Entities;

public class AppCategoryEntity
{
    public Guid Id { get; set; }
    public string AppPattern { get; set; } = string.Empty;
    public string CategoryName { get; set; } = string.Empty;
    public string Color { get; set; } = "#6B5EE4";
    public int Priority { get; set; }
    public bool IsBuiltin { get; set; }
    public DateTime CreatedAt { get; set; } = DateTime.UtcNow;
}
```

- [ ] **Step 2: 在 EntityConfigurations.cs 末尾添加 EF 配置**

```csharp
// 追加到 src/modules/Pim.Module.PcTracker/Entities/EntityConfigurations.cs
public class AppCategoryEntityConfiguration : IEntityTypeConfiguration<AppCategoryEntity>
{
    public void Configure(EntityTypeBuilder<AppCategoryEntity> builder)
    {
        builder.ToTable("pc_app_categories");
        builder.HasKey(e => e.Id);
        builder.Property(e => e.AppPattern).HasMaxLength(128).IsRequired();
        builder.Property(e => e.CategoryName).HasMaxLength(64).IsRequired();
        builder.Property(e => e.Color).HasMaxLength(7);
        builder.HasIndex(e => e.CategoryName);
        builder.HasIndex(e => e.Priority);
    }
}
```

- [ ] **Step 3: 更新 seed_pc_tables.sql 添加 DDL 和种子数据**

```sql
-- 追加到 src/Pim.Api/seed_pc_tables.sql
CREATE TABLE IF NOT EXISTS pc_app_categories (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    app_pattern VARCHAR(128) NOT NULL,
    category_name VARCHAR(64) NOT NULL,
    color VARCHAR(7) DEFAULT '#6B5EE4',
    priority INT DEFAULT 0,
    is_builtin BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMPTZ DEFAULT NOW()
);

INSERT INTO pc_app_categories (app_pattern, category_name, color, priority, is_builtin) VALUES
('Code', '编程', '#6B5EE4', 100, TRUE),
('Visual Studio', '编程', '#6B5EE4', 100, TRUE),
('VS Code', '编程', '#6B5EE4', 100, TRUE),
('Rider', '编程', '#6B5EE4', 100, TRUE),
('vim', '编程', '#6B5EE4', 90, TRUE),
('nvim', '编程', '#6B5EE4', 90, TRUE),
('chrome', '浏览', '#0EA8A0', 100, TRUE),
('msedge', '浏览', '#0EA8A0', 100, TRUE),
('firefox', '浏览', '#0EA8A0', 100, TRUE),
('Arc', '浏览', '#0EA8A0', 100, TRUE),
('Brave', '浏览', '#0EA8A0', 100, TRUE),
('WeChat', '沟通', '#F5935A', 100, TRUE),
('微信', '沟通', '#F5935A', 100, TRUE),
('DingTalk', '沟通', '#F5935A', 100, TRUE),
('钉钉', '沟通', '#F5935A', 100, TRUE),
('QQ', '沟通', '#F5935A', 100, TRUE),
('Telegram', '沟通', '#F5935A', 100, TRUE),
('Slack', '沟通', '#F5935A', 100, TRUE),
('Discord', '沟通', '#F5935A', 100, TRUE),
('WindowsTerminal', '终端', '#E05A7A', 100, TRUE),
('Terminal', '终端', '#E05A7A', 100, TRUE),
('cmd', '终端', '#E05A7A', 100, TRUE),
('PowerShell', '终端', '#E05A7A', 100, TRUE),
('Alacritty', '终端', '#E05A7A', 90, TRUE),
('iTerm2', '终端', '#E05A7A', 90, TRUE),
('explorer', '文件管理', '#3B82F6', 100, TRUE),
('Finder', '文件管理', '#3B82F6', 100, TRUE),
('TotalCommander', '文件管理', '#3B82F6', 90, TRUE),
('Everything', '文件管理', '#3B82F6', 90, TRUE),
('Spotify', '音乐', '#10B981', 100, TRUE),
('Netease', '音乐', '#10B981', 90, TRUE),
('foobar2000', '音乐', '#10B981', 90, TRUE),
('Word', '办公', '#F59E0B', 100, TRUE),
('Excel', '办公', '#F59E0B', 100, TRUE),
('PowerPoint', '办公', '#F59E0B', 100, TRUE),
('Notion', '办公', '#F59E0B', 90, TRUE),
('Obsidian', '办公', '#F59E0B', 90, TRUE),
('Typora', '办公', '#F59E0B', 90, TRUE)
ON CONFLICT (app_pattern) DO NOTHING;
```

- [ ] **Step 4: 编译验证**

```bash
dotnet build src/modules/Pim.Module.PcTracker/Pim.Module.PcTracker.csproj
```

- [ ] **Step 5: 提交**

```bash
git add src/modules/Pim.Module.PcTracker/Entities/AppCategoryEntity.cs src/modules/Pim.Module.PcTracker/Entities/EntityConfigurations.cs src/Pim.Api/seed_pc_tables.sql
git commit -m "feat: add pc_app_categories table, entity, and seed data"
```

---

### Task 2: 后端 DTO — 添加新数据类型

**Files:**
- Modify: `src/modules/Pim.Module.PcTracker/DTOs/PcTrackerDtos.cs`

- [ ] **Step 1: 在 PcTrackerDtos.cs 末尾追加新 DTO**

```csharp
// 追加到 src/modules/Pim.Module.PcTracker/DTOs/PcTrackerDtos.cs

// 衍生指标
public record DerivedMetrics(
    string TotalRecordedDuration,
    string ActiveInputDuration,
    string IdleDuration,
    int SessionCount,
    int ActiveAppCount,
    int TotalKeyPresses,
    int TotalClicks,
    int AppSwitchCount,
    double SwitchFrequency,
    string MostFocusedApp,
    double KeyClickRatio
);

// 分类汇总
public record CategorySummary(
    string CategoryName,
    string Color,
    double Share,
    int KeyPresses,
    int TotalClicks
);

// 应用分类规则
public record AppCategoryRule(
    Guid Id,
    string AppPattern,
    string CategoryName,
    string Color,
    int Priority,
    bool IsBuiltin
);

// 详情查询参数
public record DetailQueryParams(
    string? DateFrom,
    string? DateTo,
    string? Dimension,
    string? DeviceId,
    string? AppName,
    string? CategoryName,
    string? KeyName,
    string? EventType,
    string? SortBy,
    string? SortDir,
    int Page,
    int PageSize
);

// 详情查询结果
public record DetailQueryResponse(
    List<Dictionary<string, object>> Items,
    int Page,
    int PageSize,
    int TotalCount,
    int TotalPages
);

// 分类规则保存
public record SaveCategoryRequest(
    string AppPattern,
    string CategoryName,
    string Color,
    int Priority
);

// 热力图响应（扩展，支持多行网格）
public record HeatmapGridResponse(
    List<List<HeatmapBucket>> Grid,
    string Dimension,
    double MaxKeyCount
);

// 修改 PcSummaryResponse 添加新字段
// 注意：此 record 在原文件中已定义，需要原地修改
// 原: public record PcSummaryResponse(KeystatsSummary? Keystats, List<HeatmapBucket> Heatmap, List<AppRankingItem> AppRanking, List<TimelineItem> Timeline, List<WorkSessionItem> Sessions)
// 改为:
// public record PcSummaryResponse(KeystatsSummary? Keystats, List<HeatmapBucket> Heatmap, List<AppRankingItem> AppRanking, List<TimelineItem> Timeline, List<WorkSessionItem> Sessions, DerivedMetrics? Metrics, List<CategorySummary> Categories)
```

- [ ] **Step 2: 原地修改 PcSummaryResponse record**

```csharp
// 将第 49-55 行的 PcSummaryResponse 改为：
public record PcSummaryResponse(
    KeystatsSummary? Keystats,
    List<HeatmapBucket> Heatmap,
    List<AppRankingItem> AppRanking,
    List<TimelineItem> Timeline,
    List<WorkSessionItem> Sessions,
    DerivedMetrics? Metrics,
    List<CategorySummary> Categories
);
```

- [ ] **Step 3: 编译验证**

```bash
dotnet build src/modules/Pim.Module.PcTracker/Pim.Module.PcTracker.csproj
```

Expected: PASS（构建成功）

- [ ] **Step 4: 提交**

```bash
git add src/modules/Pim.Module.PcTracker/DTOs/PcTrackerDtos.cs
git commit -m "feat: add DerivedMetrics, CategorySummary, and detail query DTOs"
```

---

### Task 3: 后端 Service — 分类匹配 + 衍生指标 + 详情查询

**Files:**
- Modify: `src/modules/Pim.Module.PcTracker/Services/PcTrackerService.cs`

- [ ] **Step 1: 添加分类匹配私有方法**

在 `PcTrackerService` 类内，`BuildSessions` 方法之上插入：

```csharp
// 缓存分类规则（应用生命期内不变）
private List<AppCategoryRule>? _cachedRules;

private async Task<List<AppCategoryRule>> GetCategoryRulesAsync(CancellationToken ct)
{
    if (_cachedRules is not null) return _cachedRules;
    _cachedRules = await _db.Set<AppCategoryEntity>()
        .OrderByDescending(r => r.Priority)
        .Select(r => new AppCategoryRule(r.Id, r.AppPattern, r.CategoryName, r.Color, r.Priority, r.IsBuiltin))
        .ToListAsync(ct);
    return _cachedRules;
}

private static string ClassifyApp(string appName, List<AppCategoryRule> rules)
{
    foreach (var rule in rules)
    {
        if (string.Equals(appName, rule.AppPattern, StringComparison.OrdinalIgnoreCase))
            return rule.CategoryName;
    }
    return "其他";
}

private static string GetCategoryColor(string categoryName, List<AppCategoryRule> rules)
{
    return rules.FirstOrDefault(r => r.CategoryName == categoryName)?.Color ?? "#8B5CF6";
}
```

- [ ] **Step 2: 添加衍生指标计算方法**

在 `PcTrackerService` 类内，`MakeSession` 方法之前插入：

```csharp
private async Task<DerivedMetrics> ComputeDerivedMetricsAsync(
    DateTime date, KeystatsDailyEntity? keystats, List<AwEventEntity> awEvents, CancellationToken ct)
{
    var dayStart = new DateTimeOffset(date.Date, TimeSpan.Zero);
    var dayEnd = dayStart.AddDays(1);

    var windowEvents = awEvents.Where(e => e.EventType == "window" && e.AppName is not null).ToList();
    var afkEvents = awEvents.Where(e => e.EventType == "afk").ToList();

    // 累计记录时长: AW 首个事件 → 末个事件的时间跨度
    var totalRecorded = windowEvents.Count > 0
        ? (windowEvents.Max(e => e.Timestamp.AddSeconds(e.Duration)) -
           windowEvents.Min(e => e.Timestamp)).TotalMinutes
        : 0;

    // 有输入时长: 有按键的分钟数（基于 keystats 数据）
    double activeInputMin = 0;
    if (keystats is not null)
        activeInputMin = Math.Max(1, keystats.KeyPresses / 30.0); // 估算: 平均每分钟30键

    // 空闲时长: AFK 事件累计
    var idleMin = afkEvents
        .Where(e => e.AfkStatus == "afk")
        .Sum(e => Math.Min(e.Duration, 3600)) / 60;

    // 独立工作会话: AFK 间隙 > 15min
    var sessions = BuildSessions(windowEvents);
    var sessionCount = sessions.Count;

    // 活跃应用数: 去重
    var activeApps = windowEvents.Select(e => e.AppName).Distinct().Count();

    // 按键总数
    var keyPresses = keystats?.KeyPresses ?? 0;

    // 点击总数
    var totalClicks = keystats is not null
        ? keystats.LeftClicks + keystats.RightClicks + keystats.MiddleClicks +
          keystats.SideBackClicks + keystats.SideForwardClicks
        : 0;

    // 应用切换次数
    var appSwitchCount = 0;
    string? prevApp = null;
    foreach (var ev in windowEvents.OrderBy(e => e.Timestamp))
    {
        if (ev.AppName is not null && prevApp is not null && ev.AppName != prevApp)
            appSwitchCount++;
        prevApp = ev.AppName;
    }

    // 切换频率: 次 / 10min
    var switchFreq = totalRecorded > 0 ? Math.Round(appSwitchCount / totalRecorded * 10, 1) : 0;

    // 最专注应用: 单次持续最久
    var longestApp = windowEvents
        .Where(e => e.AppName is not null)
        .OrderByDescending(e => e.Duration)
        .FirstOrDefault()?.AppName ?? "—";

    // 按键/点击比
    var ratio = totalClicks > 0 ? Math.Round((double)keyPresses / totalClicks, 2) : 0;

    return new DerivedMetrics(
        FormatDuration(totalRecorded),
        FormatDuration(activeInputMin),
        FormatDuration(idleMin),
        sessionCount,
        activeApps,
        keyPresses,
        totalClicks,
        appSwitchCount,
        switchFreq,
        longestApp,
        ratio
    );
}

private static string FormatDuration(double minutes)
{
    if (minutes <= 0) return "0m";
    if (minutes >= 60)
    {
        var h = (int)(minutes / 60);
        var m = (int)(minutes % 60);
        return m > 0 ? $"{h}h {m}m" : $"{h}h";
    }
    return $"{Math.Round(minutes)}m";
}
```

- [ ] **Step 3: 添加分类汇总计算和详情查询方法**

在 `GetHeatmapAsync` 方法之后插入：

```csharp
public async Task<List<CategorySummary>> GetCategorySummariesAsync(DateTime date, CancellationToken ct)
{
    var dayStart = new DateTimeOffset(date.Date, TimeSpan.Zero);
    var dayEnd = dayStart.AddDays(1);

    var keystats = await _db.Set<KeystatsDailyEntity>()
        .Include(x => x.AppBreakdowns)
        .Where(x => x.SnapshotDate == date.Date)
        .OrderByDescending(x => x.CreatedAt)
        .FirstOrDefaultAsync(ct);

    if (keystats is null || !keystats.AppBreakdowns.Any()) return new();

    var rules = await GetCategoryRulesAsync(ct);
    var categoryTotals = new Dictionary<string, (int Keys, int Clicks, string Color)>();

    foreach (var app in keystats.AppBreakdowns)
    {
        var cat = ClassifyApp(app.AppName, rules);
        var color = GetCategoryColor(cat, rules);
        if (!categoryTotals.ContainsKey(cat))
            categoryTotals[cat] = (0, 0, color);
        var cur = categoryTotals[cat];
        categoryTotals[cat] = (cur.Keys + app.KeyPresses,
            cur.Clicks + app.LeftClicks + app.RightClicks + app.MiddleClicks + app.SideBackClicks + app.SideForwardClicks,
            cur.Color);
    }

    var grandTotal = categoryTotals.Values.Sum(c => c.Keys + c.Clicks);
    return categoryTotals
        .OrderByDescending(kv => kv.Value.Keys + kv.Value.Clicks)
        .Take(5)
        .Select(kv => new CategorySummary(
            kv.Key, kv.Value.Color,
            grandTotal > 0 ? Math.Round((double)(kv.Value.Keys + kv.Value.Clicks) / grandTotal * 100, 0) : 0,
            kv.Value.Keys, kv.Value.Clicks
        )).ToList();
}

public async Task<DetailQueryResponse> QueryDetailAsync(DetailQueryParams q, CancellationToken ct)
{
    var query = _db.Set<KeystatsDailyEntity>()
        .Include(x => x.KeyCounts)
        .Include(x => x.AppBreakdowns)
        .AsQueryable();

    if (!string.IsNullOrWhiteSpace(q.DateFrom) && DateTime.TryParse(q.DateFrom, out var from))
        query = query.Where(x => x.SnapshotDate >= from.Date);
    if (!string.IsNullOrWhiteSpace(q.DateTo) && DateTime.TryParse(q.DateTo, out var to))
        query = query.Where(x => x.SnapshotDate <= to.Date);
    if (!string.IsNullOrWhiteSpace(q.DeviceId))
        query = query.Where(x => x.DeviceId == q.DeviceId);

    var totalCount = await query.CountAsync(ct);
    var items = await query
        .OrderByDescending(x => x.SnapshotDate)
        .Skip((q.Page - 1) * q.PageSize)
        .Take(q.PageSize)
        .Select(x => new Dictionary<string, object>
        {
            ["date"] = x.SnapshotDate.ToString("yyyy-MM-dd"),
            ["deviceId"] = x.DeviceId,
            ["keyPresses"] = x.KeyPresses,
            ["totalClicks"] = x.LeftClicks + x.RightClicks + x.MiddleClicks + x.SideBackClicks + x.SideForwardClicks,
            ["leftClicks"] = x.LeftClicks,
            ["rightClicks"] = x.RightClicks,
            ["middleClicks"] = x.MiddleClicks,
            ["mouseDistance"] = x.MouseDistance,
            ["scrollDistance"] = x.ScrollDistance,
            ["peakKps"] = x.PeakKps,
            ["peakCps"] = x.PeakCps,
            ["apps"] = x.AppBreakdowns.Select(a => a.DisplayName ?? a.AppName).ToList(),
            ["topKeys"] = x.KeyCounts.OrderByDescending(k => k.Count).Take(5)
                .Select(k => new { k.KeyName, k.Count }).ToList()
        }).ToListAsync(ct);

    return new DetailQueryResponse(
        items, q.Page, q.PageSize, totalCount,
        (int)Math.Ceiling((double)totalCount / q.PageSize));
}

// 分类规则 CRUD
public async Task<List<AppCategoryRule>> GetAllCategoriesAsync(CancellationToken ct)
{
    return await _db.Set<AppCategoryEntity>()
        .OrderByDescending(r => r.Priority)
        .Select(r => new AppCategoryRule(r.Id, r.AppPattern, r.CategoryName, r.Color, r.Priority, r.IsBuiltin))
        .ToListAsync(ct);
}

public async Task<AppCategoryRule> SaveCategoryAsync(SaveCategoryRequest req, CancellationToken ct)
{
    var entity = await _db.Set<AppCategoryEntity>()
        .FirstOrDefaultAsync(e => e.AppPattern == req.AppPattern, ct);

    if (entity is not null)
    {
        entity.CategoryName = req.CategoryName;
        entity.Color = req.Color;
        entity.Priority = req.Priority;
    }
    else
    {
        entity = new AppCategoryEntity
        {
            AppPattern = req.AppPattern,
            CategoryName = req.CategoryName,
            Color = req.Color,
            Priority = req.Priority,
            IsBuiltin = false
        };
        _db.Set<AppCategoryEntity>().Add(entity);
    }

    await _db.SaveChangesAsync(ct);
    _cachedRules = null; // 清除缓存

    return new AppCategoryRule(entity.Id, entity.AppPattern, entity.CategoryName,
        entity.Color, entity.Priority, entity.IsBuiltin);
}

public async Task<bool> DeleteCategoryAsync(Guid id, CancellationToken ct)
{
    var entity = await _db.Set<AppCategoryEntity>().FindAsync(new object[] { id }, ct);
    if (entity is null || entity.IsBuiltin) return false;
    _db.Set<AppCategoryEntity>().Remove(entity);
    await _db.SaveChangesAsync(ct);
    _cachedRules = null;
    return true;
}
```

- [ ] **Step 4: 修改 GetSummaryAsync 方法返回 Metrics 和 Categories**

在 `GetSummaryAsync` 方法的 return 语句（第174行当前），将：

```csharp
return new PcSummaryResponse(ks, heatmap, appRanking, timeline, sessions);
```

改为：

```csharp
var metrics = await ComputeDerivedMetricsAsync(date, keystats, awEvents, ct);
var categories = await GetCategorySummariesAsync(date, ct);
return new PcSummaryResponse(ks, heatmap, appRanking, timeline, sessions, metrics, categories);
```

- [ ] **Step 5: 修改 GetHeatmapAsync 支持 dimension 参数**

修改 `GetHeatmapAsync` 方法签名和实现，使其按维度返回不同网格：

```csharp
public async Task<HeatmapGridResponse> GetHeatmapGridAsync(DateTime start, DateTime end, string dimension, CancellationToken ct)
{
    var s = new DateTimeOffset(start.Date, TimeSpan.Zero);
    var e = new DateTimeOffset(end.Date.AddDays(1), TimeSpan.Zero);

    var keystats = await _db.Set<KeystatsDailyEntity>()
        .Where(x => x.SnapshotDate >= start.Date && x.SnapshotDate <= end.Date)
        .ToListAsync(ct);

    var maxKeyCount = keystats.Any() ? keystats.Max(x => x.KeyPresses) : 1;

    if (dimension == "hour")
    {
        var targetDate = start.Date;
        var daily = keystats.FirstOrDefault(x => x.SnapshotDate == targetDate);
        var grid = new List<List<HeatmapBucket>> { new() };
        for (int h = 0; h < 24; h++)
        {
            var bucketStart = new DateTimeOffset(targetDate.AddHours(h), TimeSpan.Zero);
            var bucketEnd = bucketStart.AddHours(1);
            int keyCount = 0;
            if (daily is not null)
            {
                // 按键按小时均匀分布（KeyStats 不提供小时级数据，用 AW 事件密度估算比例）
                var hourEventCount = (double)1; // fallback
                keyCount = (int)(daily.KeyPresses * (hourEventCount / 24.0));
            }
            grid[0].Add(new HeatmapBucket(bucketStart.ToString("O"), bucketEnd.ToString("O"),
                h, 0, 0, keyCount)); // 复用 IntensityScore 存储按键数
        }
        return new HeatmapGridResponse(grid, dimension, maxKeyCount);
    }

    // day dimension: 按日期排列成 7 列网格
    var days = (end.Date - start.Date).Days + 1;
    var grid2 = new List<List<HeatmapBucket>>();
    var row = new List<HeatmapBucket>();
    for (int d = 0; d < days; d++)
    {
        var day = start.Date.AddDays(d);
        var daily = keystats.FirstOrDefault(x => x.SnapshotDate == day);
        row.Add(new HeatmapBucket(
            new DateTimeOffset(day, TimeSpan.Zero).ToString("O"),
            new DateTimeOffset(day.AddDays(1), TimeSpan.Zero).ToString("O"),
            (int)day.DayOfWeek,
            0, 0,
            daily?.KeyPresses ?? 0));

        if (row.Count == 7)
        {
            grid2.Add(row);
            row = new List<HeatmapBucket>();
        }
    }
    if (row.Count > 0) grid2.Add(row);

    return new HeatmapGridResponse(grid2, dimension, maxKeyCount);
}
```

- [ ] **Step 6: 编译验证**

```bash
dotnet build src/modules/Pim.Module.PcTracker/Pim.Module.PcTracker.csproj
```

Expected: PASS

- [ ] **Step 7: 提交**

```bash
git add src/modules/Pim.Module.PcTracker/Services/PcTrackerService.cs
git commit -m "feat: add classification, derived metrics, detail query, heatmap grid"
```

---

### Task 4: 后端 API — 添加新端点

**Files:**
- Modify: `src/modules/Pim.Module.PcTracker/PcTrackerModule.cs`

- [ ] **Step 1: 在 MapEndpoints 方法中添加新端点**

在 `PcTrackerModule.cs` 的 `MapEndpoints` 方法中，`/keystats/range` 端点之后，方法闭合括号之前，插入：

```csharp
// GET /api/v1/pc/detail — 多功能查询
readGroup.MapGet("/detail", async (
    [FromQuery] string? dateFrom,
    [FromQuery] string? dateTo,
    [FromQuery] string? dimension,
    [FromQuery] string? deviceId,
    [FromQuery] string? appName,
    [FromQuery] string? categoryName,
    [FromQuery] string? keyName,
    [FromQuery] string? eventType,
    [FromQuery] string? sortBy,
    [FromQuery] string? sortDir,
    [FromQuery] int page = 1,
    [FromQuery] int pageSize = 20,
    [FromServices] PcTrackerService svc,
    CancellationToken ct) =>
{
    var q = new DetailQueryParams(dateFrom, dateTo, dimension, deviceId,
        appName, categoryName, keyName, eventType, sortBy, sortDir, page, pageSize);
    var result = await svc.QueryDetailAsync(q, ct);
    return Results.Ok(ApiResponse<DetailQueryResponse>.Ok(result));
});

// GET /api/v1/pc/categories
readGroup.MapGet("/categories", async (
    [FromServices] PcTrackerService svc,
    CancellationToken ct) =>
{
    var list = await svc.GetAllCategoriesAsync(ct);
    return Results.Ok(ApiResponse<List<AppCategoryRule>>.Ok(list));
});

// POST /api/v1/pc/categories
writeGroup.MapPost("/categories", async (
    [FromBody] SaveCategoryRequest req,
    [FromServices] PcTrackerService svc,
    CancellationToken ct) =>
{
    var result = await svc.SaveCategoryAsync(req, ct);
    return Results.Ok(ApiResponse<AppCategoryRule>.Ok(result));
});

// DELETE /api/v1/pc/categories/{id}
writeGroup.MapDelete("/categories/{id}", async (
    Guid id,
    [FromServices] PcTrackerService svc,
    CancellationToken ct) =>
{
    var ok = await svc.DeleteCategoryAsync(id, ct);
    return ok
        ? Results.Ok(ApiResponse<string>.Ok("deleted"))
        : Results.NotFound(ApiResponse<string>.Fail("not found or builtin"));
});

// GET /api/v1/pc/heatmap/grid — 网格化热力图
readGroup.MapGet("/heatmap/grid", async (
    [FromQuery] string? start,
    [FromQuery] string? end,
    [FromQuery] string dimension = "day",
    [FromServices] PcTrackerService svc,
    CancellationToken ct) =>
{
    var s = start is not null ? DateTime.Parse(start) : DateTime.Today.AddDays(-30);
    var e = end is not null ? DateTime.Parse(end) : DateTime.Today;
    var result = await svc.GetHeatmapGridAsync(s, e, dimension, ct);
    return Results.Ok(ApiResponse<HeatmapGridResponse>.Ok(result));
});
```

- [ ] **Step 2: 在文件顶部添加 using**

确认 `using Pim.Module.PcTracker.DTOs;` 已存在（第11行已有，无需修改）。

- [ ] **Step 3: 编译验证**

```bash
dotnet build src/modules/Pim.Module.PcTracker/Pim.Module.PcTracker.csproj
```

Expected: PASS

- [ ] **Step 4: 提交**

```bash
git add src/modules/Pim.Module.PcTracker/PcTrackerModule.cs
git commit -m "feat: add /pc/detail, /pc/categories CRUD, /pc/heatmap/grid endpoints"
```

---

### Task 5: 前端 TypeScript 类型扩展

**Files:**
- Modify: `src/client-web/src/types/index.ts`

- [ ] **Step 1: 在 index.ts 末尾追加新类型**

```typescript
// 追加到 src/client-web/src/types/index.ts

export interface DerivedMetrics {
  totalRecordedDuration: string;
  activeInputDuration: string;
  idleDuration: string;
  sessionCount: number;
  activeAppCount: number;
  totalKeyPresses: number;
  totalClicks: number;
  appSwitchCount: number;
  switchFrequency: number;
  mostFocusedApp: string;
  keyClickRatio: number;
}

export interface CategorySummary {
  categoryName: string;
  color: string;
  share: number;
  keyPresses: number;
  totalClicks: number;
}

export interface AppCategoryRule {
  id: string;
  appPattern: string;
  categoryName: string;
  color: string;
  priority: number;
  isBuiltin: boolean;
}

export interface DetailQueryParams {
  dateFrom?: string;
  dateTo?: string;
  dimension?: 'hour' | 'day' | 'month' | 'year';
  deviceId?: string;
  appName?: string;
  categoryName?: string;
  keyName?: string;
  eventType?: string;
  sortBy?: string;
  sortDir?: 'asc' | 'desc';
  page?: number;
  pageSize?: number;
}

export interface DetailQueryResponse {
  items: Record<string, unknown>[];
  page: number;
  pageSize: number;
  totalCount: number;
  totalPages: number;
}

export interface HeatmapGridResponse {
  grid: HeatmapBucket[][];
  dimension: string;
  maxKeyCount: number;
}
```

- [ ] **Step 2: 修改 PcSummaryResponse 类型**

将第 65-71 行的 `PcSummaryResponse` 改为：

```typescript
export interface PcSummaryResponse {
  keystats: KeystatsSummary | null;
  heatmap: HeatmapBucket[];
  appRanking: AppRankingItem[];
  timeline: TimelineItem[];
  sessions: WorkSessionItem[];
  metrics: DerivedMetrics | null;
  categories: CategorySummary[];
}
```

- [ ] **Step 3: 提交**

```bash
git add src/client-web/src/types/index.ts
git commit -m "feat: add DerivedMetrics, CategorySummary, DetailQuery types"
```

---

### Task 6: 前端 API 函数扩展

**Files:**
- Modify: `src/client-web/src/api/pcTracker.ts`

- [ ] **Step 1: 扩展 pcTracker.ts**

```typescript
// 完整替换 src/client-web/src/api/pcTracker.ts
import { apiGet, apiPost, apiDelete } from './client';
import type { ApiResponse } from '../types';
import type {
  PcSummaryResponse, TimelineItem, HeatmapBucket,
  DetailQueryParams, DetailQueryResponse,
  AppCategoryRule, HeatmapGridResponse
} from '../types';

export function getPcSummary(date: string) {
  return apiGet<ApiResponse<PcSummaryResponse>>(`/pc/summary?date=${date}`).then(r => r.data);
}

export function getPcTimeline(date: string) {
  return apiGet<ApiResponse<TimelineItem[]>>(`/pc/aw/timeline?date=${date}`).then(r => r.data);
}

export function getPcHeatmap(start: string, end: string) {
  return apiGet<ApiResponse<HeatmapBucket[]>>(`/pc/aw/heatmap?start=${start}&end=${end}`).then(r => r.data);
}

export function getPcHeatmapGrid(start: string, end: string, dimension: string) {
  return apiGet<ApiResponse<HeatmapGridResponse>>(
    `/pc/heatmap/grid?start=${start}&end=${end}&dimension=${dimension}`
  ).then(r => r.data);
}

export function queryPcDetail(params: DetailQueryParams) {
  const searchParams = new URLSearchParams();
  Object.entries(params).forEach(([k, v]) => {
    if (v !== undefined && v !== null && v !== '') searchParams.set(k, String(v));
  });
  return apiGet<ApiResponse<DetailQueryResponse>>(`/pc/detail?${searchParams.toString()}`).then(r => r.data);
}

export function getPcCategories() {
  return apiGet<ApiResponse<AppCategoryRule[]>>('/pc/categories').then(r => r.data);
}

export function savePcCategory(rule: { appPattern: string; categoryName: string; color: string; priority: number }) {
  return apiPost<ApiResponse<AppCategoryRule>>('/pc/categories', rule).then(r => r.data);
}

export function deletePcCategory(id: string) {
  return apiDelete<ApiResponse<string>>(`/pc/categories/${id}`).then(r => r.data);
}
```

- [ ] **Step 2: 检查 api/client.ts 是否已有 apiPost 和 apiDelete**

```bash
grep -n "apiPost\|apiDelete" src/client-web/src/api/client.ts
```

如果不存在，需要在 `client.ts` 中添加：

```typescript
export function apiPost<T>(path: string, body: unknown) {
  return httpClient.post<T>(path, body);
}

export function apiDelete<T>(path: string) {
  return httpClient.delete<T>(path);
}
```

- [ ] **Step 3: 提交**

```bash
git add src/client-web/src/api/pcTracker.ts src/client-web/src/api/client.ts
git commit -m "feat: add heatmap grid, detail query, categories CRUD API functions"
```

---

### Task 7: DateDimensionBar 组件

**Files:**
- Create: `src/client-web/src/components/pc-tracker/DateDimensionBar.tsx`

- [ ] **Step 1: 创建 DateDimensionBar.tsx**

```typescript
// src/client-web/src/components/pc-tracker/DateDimensionBar.tsx
import { format } from 'date-fns';
import { zhCN } from 'date-fns/locale';

const DIMENSIONS = [
  { key: 'hour' as const, label: '时' },
  { key: 'day' as const, label: '日' },
  { key: 'month' as const, label: '月' },
  { key: 'year' as const, label: '年' },
];

interface Props {
  date: Date;
  dimension: 'hour' | 'day' | 'month' | 'year';
  onDateChange: (d: Date) => void;
  onDimensionChange: (dim: 'hour' | 'day' | 'month' | 'year') => void;
}

export default function DateDimensionBar({ date, dimension, onDateChange, onDimensionChange }: Props) {
  return (
    <div className="flex items-center justify-between bg-white rounded-xl px-4 py-3 shadow-sm border">
      <div className="flex items-center gap-2">
        <button className="px-3 py-1 text-sm font-medium bg-blue-600 text-white rounded-lg hover:bg-blue-700"
          onClick={() => onDateChange(new Date())}>今天</button>
        <button className="px-2 py-1 text-sm border rounded-lg hover:bg-gray-50"
          onClick={() => onDateChange(new Date(date.getTime() - 86400000))}>‹</button>
        <button className="px-2 py-1 text-sm border rounded-lg hover:bg-gray-50"
          onClick={() => onDateChange(new Date(date.getTime() + 86400000))}>›</button>
        <span className="font-bold text-lg ml-3">
          {format(date, 'yyyy年M月d日 EEEE', { locale: zhCN })}
        </span>
      </div>
      <div className="flex items-center gap-1 bg-gray-100 rounded-lg p-0.5">
        {DIMENSIONS.map(d => (
          <button key={d.key}
            className={`px-3 py-1 text-xs rounded-md transition-colors ${
              dimension === d.key ? 'bg-white text-gray-800 shadow-sm font-medium' : 'text-gray-500 hover:text-gray-700'
            }`}
            onClick={() => onDimensionChange(d.key)}>
            {d.label}
          </button>
        ))}
      </div>
    </div>
  );
}
```

- [ ] **Step 2: 提交**

```bash
git add src/client-web/src/components/pc-tracker/DateDimensionBar.tsx
git commit -m "feat: add DateDimensionBar component"
```

---

### Task 8: ActivityHeatmap 组件

**Files:**
- Create: `src/client-web/src/components/pc-tracker/ActivityHeatmap.tsx`

- [ ] **Step 1: 创建 ActivityHeatmap.tsx**

```typescript
// src/client-web/src/components/pc-tracker/ActivityHeatmap.tsx
import type { HeatmapGridResponse } from '../../types';

const COLOR_STOPS = ['#ebedf0', '#9be9a8', '#40c463', '#30a14e', '#216e39'];

function linearColor(value: number, max: number): string {
  if (value === 0 || max === 0) return COLOR_STOPS[0];
  const ratio = Math.min(value / max, 1);
  const idx = ratio * (COLOR_STOPS.length - 1);
  const low = Math.floor(idx);
  const high = Math.min(low + 1, COLOR_STOPS.length - 1);
  const t = idx - low;
  const l = parseInt(COLOR_STOPS[low].slice(1), 16);
  const h = parseInt(COLOR_STOPS[high].slice(1), 16);
  const r = Math.round(((l >> 16) & 0xff) + t * (((h >> 16) & 0xff) - ((l >> 16) & 0xff)));
  const g = Math.round(((l >> 8) & 0xff) + t * (((h >> 8) & 0xff) - ((l >> 8) & 0xff)));
  const b = Math.round((l & 0xff) + t * ((h & 0xff) - (l & 0xff)));
  return `rgb(${r},${g},${b})`;
}

interface Props {
  data: HeatmapGridResponse | undefined;
  isLoading: boolean;
}

export default function ActivityHeatmap({ data, isLoading }: Props) {
  if (isLoading) return <div className="py-8 text-center text-gray-400">加载中...</div>;
  if (!data || !data.grid.length) return <div className="py-8 text-center text-gray-400">暂无活动数据</div>;

  const maxKey = data.maxKeyCount || 1;

  return (
    <div>
      <div className="flex items-center justify-end gap-1 mb-3 text-[11px] text-gray-400">
        <span>少</span>
        {COLOR_STOPS.map((c, i) => (
          <div key={i} className="w-3 h-3 rounded-sm" style={{ backgroundColor: c }} />
        ))}
        <span>多</span>
      </div>
      <div className="flex flex-col gap-[3px]">
        {data.grid.map((row, ri) => (
          <div key={ri} className="flex gap-[3px]" style={{
            flexDirection: data.dimension === 'hour' ? 'row' : 'row',
          }}>
            {row.map((cell, ci) => (
              <div key={ci} className="relative group flex-1 aspect-square rounded-sm cursor-pointer hover:ring-2 hover:ring-blue-400 transition-all"
                style={{ backgroundColor: linearColor(cell.intensityScore, maxKey) }}
                title={`${cell.start.slice(0, 10)} · ${cell.intensityScore.toLocaleString()} 键`}>
                <div className="absolute bottom-full left-1/2 -translate-x-1/2 mb-1 hidden group-hover:block bg-gray-800 text-white text-[10px] px-2 py-1 rounded whitespace-nowrap z-10">
                  {cell.start.slice(0, 16)} · {cell.intensityScore.toLocaleString()} 键
                </div>
              </div>
            ))}
          </div>
        ))}
      </div>
    </div>
  );
}
```

- [ ] **Step 2: 提交**

```bash
git add src/client-web/src/components/pc-tracker/ActivityHeatmap.tsx
git commit -m "feat: add GitHub-style ActivityHeatmap component"
```

---

### Task 9: CategoryTimeline 组件

**Files:**
- Create: `src/client-web/src/components/pc-tracker/CategoryTimeline.tsx`

- [ ] **Step 1: 创建 CategoryTimeline.tsx**

```typescript
// src/client-web/src/components/pc-tracker/CategoryTimeline.tsx
import { useMemo } from 'react';
import { format } from 'date-fns';
import type { TimelineItem, CategorySummary } from '../../types';

const CAT_COLORS: Record<string, string> = {};

function catColor(name: string, color?: string): string {
  if (!CAT_COLORS[name]) CAT_COLORS[name] = color || '#8B5CF6';
  return CAT_COLORS[name];
}

interface CategoryBlock {
  start: Date;
  end: Date;
  categoryName: string;
  color: string;
  apps: { name: string; share: number }[];
  totalMinutes: number;
}

function buildCategoryBlocks(timeline: TimelineItem[], categories: CategorySummary[]): CategoryBlock[] {
  if (!timeline.length) return [];
  const catMap = new Map(categories.map(c => [c.categoryName, c.color]));
  const sorted = [...timeline].sort((a, b) => new Date(a.start).getTime() - new Date(b.start).getTime());

  const blocks: CategoryBlock[] = [];
  let current: CategoryBlock | null = null;

  for (const item of sorted) {
    const cat = categories.find(c =>
      c.keyPresses > 0 && item.appName.toLowerCase().includes(c.categoryName.toLowerCase())
    )?.categoryName || '其他';
    const color = catMap.get(cat) || '#8B5CF6';

    if (current && current.categoryName === cat) {
      current.end = new Date(item.end);
      current.totalMinutes += item.durationMinutes;
      const existing = current.apps.find(a => a.name === item.appName);
      if (existing) existing.share += item.durationMinutes;
      else current.apps.push({ name: item.appName, share: item.durationMinutes });
    } else {
      if (current) blocks.push(current);
      current = {
        start: new Date(item.start),
        end: new Date(item.end),
        categoryName: cat,
        color,
        apps: [{ name: item.appName, share: item.durationMinutes }],
        totalMinutes: item.durationMinutes,
      };
    }
  }
  if (current) blocks.push(current);

  for (const block of blocks) {
    const total = block.apps.reduce((s, a) => s + a.share, 0);
    for (const app of block.apps) app.share = Math.round((app.share / total) * 100);
  }

  return blocks;
}

function fmtTime(iso: string) {
  try { return format(new Date(iso), 'HH:mm'); } catch { return iso; }
}

interface Props {
  timeline: TimelineItem[];
  categories: CategorySummary[];
}

export default function CategoryTimeline({ timeline, categories }: Props) {
  const blocks = useMemo(() => buildCategoryBlocks(timeline, categories), [timeline, categories]);

  if (!blocks.length) return <div className="py-8 text-center text-gray-400">暂无时间线数据</div>;

  const dayStart = new Date(blocks[0].start);
  dayStart.setHours(0, 0, 0, 0);
  const dayEnd = new Date(dayStart);
  dayEnd.setDate(dayEnd.getDate() + 1);
  const totalMs = dayEnd.getTime() - dayStart.getTime();

  return (
    <div className="relative h-14 bg-gray-50 rounded-lg overflow-hidden">
      {blocks.map((block, i) => {
        const leftPct = ((block.start.getTime() - dayStart.getTime()) / totalMs) * 100;
        const widthPct = Math.max(((block.end.getTime() - block.start.getTime()) / totalMs) * 100, 0.5);
        return (
          <div key={i} className="absolute top-2 h-10 rounded-lg group flex items-center justify-center text-[10px] font-medium text-white truncate px-1"
            style={{ left: `${leftPct}%`, width: `${widthPct}%`, backgroundColor: block.color, opacity: 0.85 }}>
            {block.categoryName}
            <div className="absolute bottom-full left-1/2 -translate-x-1/2 mb-1 hidden group-hover:block bg-gray-800 text-white text-[10px] px-3 py-2 rounded-lg whitespace-nowrap z-10 min-w-[160px]">
              <div className="font-semibold mb-1">{block.categoryName}</div>
              <div>{fmtTime(block.start.toISOString())} — {fmtTime(block.end.toISOString())}</div>
              <div className="text-gray-300 mt-1">
                {block.apps.map(a => (
                  <div key={a.name}>{a.name} {a.share}%</div>
                ))}
              </div>
            </div>
          </div>
        );
      })}
    </div>
  );
}
```

- [ ] **Step 2: 提交**

```bash
git add src/client-web/src/components/pc-tracker/CategoryTimeline.tsx
git commit -m "feat: add CategoryTimeline component with category aggregation"
```

---

### Task 10: DailyActivityPanel 组件

**Files:**
- Create: `src/client-web/src/components/pc-tracker/DailyActivityPanel.tsx`

- [ ] **Step 1: 创建 DailyActivityPanel.tsx**

```typescript
// src/client-web/src/components/pc-tracker/DailyActivityPanel.tsx
import type { DerivedMetrics, CategorySummary, AppRankingItem } from '../../types';

interface Props {
  metrics: DerivedMetrics | null;
  categories: CategorySummary[];
  appRanking: AppRankingItem[];
  selectedCategory: string | null;
  onSelectCategory: (cat: string | null) => void;
  selectedApp: string | null;
  onSelectApp: (app: string | null) => void;
}

function MetricCard({ label, value }: { label: string; value: string | number }) {
  return (
    <div className="bg-white rounded-lg p-3 text-center border border-gray-100">
      <div className="text-[11px] text-gray-400 mb-1">{label}</div>
      <div className="text-base font-bold text-gray-800">{value}</div>
    </div>
  );
}

export default function DailyActivityPanel({ metrics, categories, appRanking, selectedCategory, onSelectCategory, selectedApp, onSelectApp }: Props) {
  if (!metrics) return <div className="py-8 text-center text-gray-400">暂无活动数据</div>;

  const top5Categories = categories.slice(0, 5);
  const top5Apps = appRanking.slice(0, 5);
  const totalInput = top5Apps.reduce((s, a) => s + a.keyPresses + a.totalClicks, 0) || 1;

  return (
    <div className="space-y-4">
      {/* Metrics grid — 4+4+3 */}
      <div className="grid grid-cols-4 gap-3">
        <MetricCard label="累计记录时长" value={metrics.totalRecordedDuration} />
        <MetricCard label="有输入时长" value={metrics.activeInputDuration} />
        <MetricCard label="空闲时长" value={metrics.idleDuration} />
        <MetricCard label="独立工作会话" value={`${metrics.sessionCount} 个`} />
      </div>
      <div className="grid grid-cols-4 gap-3">
        <MetricCard label="活跃应用数" value={`${metrics.activeAppCount} 个`} />
        <MetricCard label="键盘按键总数" value={metrics.totalKeyPresses.toLocaleString()} />
        <MetricCard label="点击总数" value={metrics.totalClicks.toLocaleString()} />
        <MetricCard label="应用切换次数" value={`${metrics.appSwitchCount} 次`} />
      </div>
      <div className="grid grid-cols-3 gap-3">
        <MetricCard label="切换频率" value={`${metrics.switchFrequency} 次/10min`} />
        <MetricCard label="最专注应用" value={metrics.mostFocusedApp} />
        <MetricCard label="按键/点击比" value={`${metrics.keyClickRatio}:1`} />
      </div>

      {/* Top 5 categories */}
      <div>
        <div className="text-xs text-gray-400 mb-2">🏷️ 前五分类</div>
        <div className="flex flex-wrap gap-2">
          {top5Categories.map(c => (
            <button key={c.categoryName} className={`px-3 py-1.5 rounded-lg text-xs font-medium transition-colors ${
              selectedCategory === c.categoryName ? 'ring-2 ring-blue-400 bg-blue-50' : 'bg-gray-100 hover:bg-gray-200'
            }`}
              style={{ backgroundColor: selectedCategory === c.categoryName ? undefined : c.color + '18', color: c.color }}
              onClick={() => onSelectCategory(selectedCategory === c.categoryName ? null : c.categoryName)}>
              {c.categoryName} {c.share}%
            </button>
          ))}
        </div>
      </div>

      {/* Top 5 apps */}
      <div>
        <div className="text-xs text-gray-400 mb-2">⚙️ 前五应用（进程名）</div>
        <div className="flex flex-wrap gap-2">
          {top5Apps.map(a => {
            const share = Math.round((a.keyPresses + a.totalClicks) / totalInput * 100);
            return (
              <button key={a.appName} className={`px-3 py-1.5 rounded-lg text-xs transition-colors ${
                selectedApp === a.appName ? 'ring-2 ring-blue-400 bg-blue-50' : 'bg-gray-100 hover:bg-gray-200'
              }`}
                onClick={() => onSelectApp(selectedApp === a.appName ? null : a.appName)}>
                {a.appName} <span className="text-gray-400">{share}%</span>
              </button>
            );
          })}
        </div>
      </div>
    </div>
  );
}
```

- [ ] **Step 2: 提交**

```bash
git add src/client-web/src/components/pc-tracker/DailyActivityPanel.tsx
git commit -m "feat: add DailyActivityPanel with derived metrics and rankings"
```

---

### Task 11: KeyboardHeatmap 组件

**Files:**
- Create: `src/client-web/src/components/pc-tracker/KeyboardHeatmap.tsx`

- [ ] **Step 1: 创建 KeyboardHeatmap.tsx**

```typescript
// src/client-web/src/components/pc-tracker/KeyboardHeatmap.tsx
import type { KeystatsSummary } from '../../types';

const GREEN_STOPS = ['#ebedf0', '#9be9a8', '#40c463', '#30a14e', '#216e39'];
const MODIFIER_KEYS = new Set(['LCtrl', 'RCtrl', 'LWin', 'RWin', 'LAlt', 'RAlt', 'Esc', 'Tab', 'CapsLock']);

function keyColor(count: number, max: number): string {
  if (count === 0 || max === 0) return GREEN_STOPS[0];
  const ratio = Math.min(count / max, 1);
  const idx = ratio * (GREEN_STOPS.length - 1);
  const low = Math.floor(idx);
  const high = Math.min(low + 1, GREEN_STOPS.length - 1);
  const t = idx - low;
  const l = parseInt(GREEN_STOPS[low].slice(1), 16);
  const h = parseInt(GREEN_STOPS[high].slice(1), 16);
  const r = Math.round(((l >> 16) & 0xff) + t * (((h >> 16) & 0xff) - ((l >> 16) & 0xff)));
  const g = Math.round(((l >> 8) & 0xff) + t * (((h >> 8) & 0xff) - ((l >> 8) & 0xff)));
  const b = Math.round((l & 0xff) + t * ((h & 0xff) - (l & 0xff)));
  return `rgb(${r},${g},${b})`;
}

function textColor(count: number, max: number): string {
  return count > max * 0.4 ? '#fff' : '#374151';
}

// ANSI 104-key layout rows
const KEYBOARD_ROWS = [
  ['Esc', '', '', '', '', '', '', '', '', '', '', '', '', 'Backspace', ''],
  ['Tab', 'Q', 'W', 'E', 'R', 'T', 'Y', 'U', 'I', 'O', 'P', '[', ']', '\\', ''],
  ['Caps', 'A', 'S', 'D', 'F', 'G', 'H', 'J', 'K', 'L', ';', "'", '', 'Enter', ''],
  ['Shift', 'Z', 'X', 'C', 'V', 'B', 'N', 'M', ',', '.', '/', '', 'Shift', '', ''],
  ['Ctrl', 'Win', 'Alt', '', 'Space', '', '', 'Alt', 'Win', 'Ctrl', '', '', '', '↑', ''],
  ['', '', '', '', '', '', '', '', '', '', '', '←', '↓', '→']
];

const KEY_WIDTHS: Record<string, number> = {
  'Backspace': 1.5, 'Tab': 1.3, 'Caps': 1.5, 'Enter': 1.7,
  'Shift': 1.8, 'Ctrl': 1.2, 'Win': 1.1, 'Alt': 1.1,
  'Space': 5, '↑': 1, '↓': 1, '←': 1, '→': 1,
};

interface Props {
  keystats: KeystatsSummary | null;
}

export default function KeyboardHeatmap({ keystats }: Props) {
  if (!keystats) return <div className="py-8 text-center text-gray-400">暂无按键数据</div>;

  const keyCounts = new Map(keystats.topKeys.map(k => [k.keyName, k.count]));
  const allCounts = keystats.topKeys.map(k => k.count);
  const maxKey = Math.max(...allCounts, 1);

  const shortcuts = keystats.topKeys
    .filter(k => k.keyName.includes('+'))
    .sort((a, b) => b.count - a.count);

  return (
    <div className="space-y-4">
      <div className="flex justify-center">
        <div className="flex flex-col gap-[2px]" style={{ maxWidth: 680 }}>
          {KEYBOARD_ROWS.map((row, ri) => (
            <div key={ri} className="flex gap-[2px]">
              {row.map((key, ki) => {
                if (!key) return <div key={ki} style={{ width: 16 }} />;
                const count = keyCounts.get(key) || 0;
                const isMod = MODIFIER_KEYS.has(key);
                const bg = isMod ? '#e5e7eb' : keyColor(count, maxKey);
                const color = isMod ? '#6b7280' : textColor(count, maxKey);
                const width = (KEY_WIDTHS[key] || 1) * 38;
                return (
                  <div key={ki} className="h-8 rounded flex items-center justify-center text-[10px] font-mono relative group"
                    style={{ backgroundColor: bg, color, width, minWidth: 26 }}>
                    {key.length <= 3 ? key : key.slice(0, 3)}
                    {count > 0 && (
                      <span className="absolute -bottom-1 right-0.5 text-[8px] leading-none" style={{ color }}>{count}</span>
                    )}
                    <div className="absolute bottom-full left-1/2 -translate-x-1/2 mb-1 hidden group-hover:block bg-gray-800 text-white text-[10px] px-2 py-1 rounded whitespace-nowrap z-10">
                      {key}: {count.toLocaleString()}
                    </div>
                  </div>
                );
              })}
            </div>
          ))}
        </div>
      </div>

      {/* Mouse clicks */}
      <div className="flex justify-center gap-6 text-xs text-gray-500 pt-2 border-t border-gray-100">
        <span>🖱 左键 {keystats.leftClicks.toLocaleString()}</span>
        <span>右键 {keystats.rightClicks.toLocaleString()}</span>
        <span>中键 {keystats.middleClicks}</span>
        <span>侧后退 {keystats.sideBackClicks}</span>
        <span>侧前进 {keystats.sideForwardClicks}</span>
        <span className="font-medium ml-2">总点击 {keystats.totalClicks.toLocaleString()}</span>
      </div>

      {/* Shortcuts */}
      {shortcuts.length > 0 && (
        <div className="pt-2 border-t border-dashed border-gray-100">
          <div className="text-xs text-gray-400 mb-2">快捷键统计</div>
          <div className="flex flex-wrap gap-2">
            {shortcuts.map(s => (
              <span key={s.keyName} className="px-2 py-1 bg-red-50 border border-red-100 rounded text-xs text-red-600">
                {s.keyName} <span className="text-red-400">{s.count}</span>
              </span>
            ))}
          </div>
        </div>
      )}
    </div>
  );
}
```

- [ ] **Step 2: 提交**

```bash
git add src/client-web/src/components/pc-tracker/KeyboardHeatmap.tsx
git commit -m "feat: add KeyboardHeatmap component with ANSI layout"
```

---

### Task 12: 重写 PcTrackerPage 主页面

**Files:**
- Modify: `src/client-web/src/pages/PcTrackerPage.tsx`

- [ ] **Step 1: 完整重写 PcTrackerPage.tsx**

```typescript
// src/client-web/src/pages/PcTrackerPage.tsx
import { useState } from 'react';
import { useQuery } from '@tanstack/react-query';
import { format, subDays, subMonths } from 'date-fns';
import { getPcSummary, getPcHeatmapGrid } from '../api/pcTracker';
import DateDimensionBar from '../components/pc-tracker/DateDimensionBar';
import ActivityHeatmap from '../components/pc-tracker/ActivityHeatmap';
import CategoryTimeline from '../components/pc-tracker/CategoryTimeline';
import DailyActivityPanel from '../components/pc-tracker/DailyActivityPanel';
import KeyboardHeatmap from '../components/pc-tracker/KeyboardHeatmap';

function PanelCard({ title, subtitle, icon, children }: { title: string; subtitle: string; icon: string; children: React.ReactNode }) {
  return (
    <div className="bg-white rounded-xl shadow-sm border p-5">
      <div className="flex items-center gap-2 mb-4">
        <span className="text-lg">{icon}</span>
        <span className="font-semibold text-gray-800">{title}</span>
        <span className="text-xs text-gray-400 ml-2">{subtitle}</span>
      </div>
      {children}
    </div>
  );
}

export default function PcTrackerPage() {
  const [selectedDate, setSelectedDate] = useState(new Date());
  const [dimension, setDimension] = useState<'hour' | 'day' | 'month' | 'year'>('day');
  const [selectedCategory, setSelectedCategory] = useState<string | null>(null);
  const [selectedApp, setSelectedApp] = useState<string | null>(null);

  const dateStr = format(selectedDate, 'yyyy-MM-dd');

  // Summary query
  const { data, isLoading } = useQuery({
    queryKey: ['pc-summary', dateStr],
    queryFn: () => getPcSummary(dateStr),
    refetchInterval: 30000,
  });

  // Heatmap grid query
  const heatmapRange = dimension === 'hour'
    ? { start: dateStr, end: dateStr }
    : dimension === 'day'
      ? { start: format(subDays(selectedDate, 30), 'yyyy-MM-dd'), end: dateStr }
      : dimension === 'month'
        ? { start: format(subMonths(selectedDate, 12), 'yyyy-MM-dd'), end: dateStr }
        : { start: format(subMonths(selectedDate, 60), 'yyyy-MM-dd'), end: dateStr };

  const { data: heatmapData, isLoading: heatmapLoading } = useQuery({
    queryKey: ['pc-heatmap-grid', heatmapRange.start, heatmapRange.end, dimension],
    queryFn: () => getPcHeatmapGrid(heatmapRange.start, heatmapRange.end, dimension),
  });

  return (
    <div className="max-w-[960px] mx-auto space-y-4 pb-8">
      {/* Module 1: Date + Dimension */}
      <DateDimensionBar date={selectedDate} dimension={dimension}
        onDateChange={setSelectedDate} onDimensionChange={setDimension} />

      {/* Module 2: Heatmap */}
      <PanelCard title="活动热力图" subtitle="按键频率分布（线性绿阶）" icon="📊">
        <ActivityHeatmap data={heatmapData} isLoading={heatmapLoading} />
      </PanelCard>

      {/* Timeline: Category Aggregation */}
      <PanelCard title="时间线" subtitle="按活动分类聚合（悬浮查看详情）" icon="⏱">
        <CategoryTimeline timeline={data?.timeline || []} categories={data?.categories || []} />
      </PanelCard>

      {/* Module 3: Daily Activity */}
      <PanelCard title="当日活动分析" subtitle="综合衍生指标" icon="📈">
        <DailyActivityPanel metrics={data?.metrics || null} categories={data?.categories || []}
          appRanking={data?.appRanking || []} selectedCategory={selectedCategory}
          onSelectCategory={setSelectedCategory} selectedApp={selectedApp}
          onSelectApp={setSelectedApp} />
      </PanelCard>

      {/* Module 4: Keyboard Heatmap */}
      <PanelCard title="键盘鼠标热力图" subtitle="标准 ANSI 布局 + 快捷键统计" icon="⌨">
        <KeyboardHeatmap keystats={data?.keystats || null} />
      </PanelCard>
    </div>
  );
}
```

- [ ] **Step 2: 验证编译**

```bash
cd src/client-web; npx tsc --noEmit
```

Expected: 无类型错误

- [ ] **Step 3: 提交**

```bash
git add src/client-web/src/pages/PcTrackerPage.tsx
git commit -m "feat: rewrite PcTrackerPage with new 4-module layout"
```

---

### Task 13: PcDetailQueryPanel 设置页查询组件

**Files:**
- Create: `src/client-web/src/components/pc-tracker/PcDetailQueryPanel.tsx`
- Create: `src/client-web/src/pages/PcDetailQueryPage.tsx`

- [ ] **Step 1: 创建 PcDetailQueryPanel.tsx**

```typescript
// src/client-web/src/components/pc-tracker/PcDetailQueryPanel.tsx
import { useState } from 'react';
import { useQuery } from '@tanstack/react-query';
import { queryPcDetail } from '../../api/pcTracker';
import type { DetailQueryParams } from '../../types';

function downloadCSV(items: Record<string, unknown>[], filename: string) {
  if (!items.length) return;
  const keys = Object.keys(items[0]);
  const csv = [keys.join(','), ...items.map(row => keys.map(k => JSON.stringify(row[k] ?? '')).join(','))].join('\n');
  const blob = new Blob(['﻿' + csv], { type: 'text/csv;charset=utf-8' });
  const url = URL.createObjectURL(blob);
  const a = document.createElement('a');
  a.href = url; a.download = filename; a.click();
  URL.revokeObjectURL(url);
}

export default function PcDetailQueryPanel() {
  const [params, setParams] = useState<DetailQueryParams>({ page: 1, pageSize: 20 });

  const { data, isLoading } = useQuery({
    queryKey: ['pc-detail', params],
    queryFn: () => queryPcDetail(params),
  });

  const update = (key: string, value: unknown) =>
    setParams(p => ({ ...p, [key]: value, page: key === 'page' ? p.page : 1 }));

  return (
    <div className="space-y-4">
      {/* Filter bar */}
      <div className="grid grid-cols-4 gap-3">
        <div>
          <label className="text-xs text-gray-500">日期起</label>
          <input type="date" className="w-full border rounded-lg px-3 py-2 text-sm"
            onChange={e => update('dateFrom', e.target.value)} />
        </div>
        <div>
          <label className="text-xs text-gray-500">日期止</label>
          <input type="date" className="w-full border rounded-lg px-3 py-2 text-sm"
            onChange={e => update('dateTo', e.target.value)} />
        </div>
        <div>
          <label className="text-xs text-gray-500">维度</label>
          <select className="w-full border rounded-lg px-3 py-2 text-sm"
            onChange={e => update('dimension', e.target.value)}>
            <option value="">全部</option>
            <option value="hour">时</option>
            <option value="day">日</option>
            <option value="month">月</option>
            <option value="year">年</option>
          </select>
        </div>
        <div>
          <label className="text-xs text-gray-500">设备</label>
          <input type="text" className="w-full border rounded-lg px-3 py-2 text-sm" placeholder="输入 device_id"
            onChange={e => update('deviceId', e.target.value)} />
        </div>
        <div>
          <label className="text-xs text-gray-500">应用</label>
          <input type="text" className="w-full border rounded-lg px-3 py-2 text-sm" placeholder="进程名"
            onChange={e => update('appName', e.target.value)} />
        </div>
        <div>
          <label className="text-xs text-gray-500">分类</label>
          <input type="text" className="w-full border rounded-lg px-3 py-2 text-sm" placeholder="分类名"
            onChange={e => update('categoryName', e.target.value)} />
        </div>
        <div>
          <label className="text-xs text-gray-500">按键名</label>
          <input type="text" className="w-full border rounded-lg px-3 py-2 text-sm" placeholder="如 Space"
            onChange={e => update('keyName', e.target.value)} />
        </div>
        <div>
          <label className="text-xs text-gray-500">排序</label>
          <select className="w-full border rounded-lg px-3 py-2 text-sm"
            onChange={e => update('sortBy', e.target.value)}>
            <option value="">默认</option>
            <option value="keyPresses">按键数</option>
            <option value="totalClicks">点击数</option>
            <option value="date">日期</option>
          </select>
        </div>
      </div>

      {/* Export buttons */}
      {data && data.items.length > 0 && (
        <div className="flex gap-2">
          <button className="px-3 py-1 text-xs bg-green-50 text-green-700 border border-green-200 rounded-lg hover:bg-green-100"
            onClick={() => downloadCSV(data.items, `pc-detail-${new Date().toISOString().slice(0, 10)}.csv`)}>
            导出 CSV
          </button>
          <button className="px-3 py-1 text-xs bg-blue-50 text-blue-700 border border-blue-200 rounded-lg hover:bg-blue-100"
            onClick={() => {
              const json = JSON.stringify(data.items, null, 2);
              const blob = new Blob([json], { type: 'application/json' });
              const url = URL.createObjectURL(blob);
              const a = document.createElement('a');
              a.href = url; a.download = `pc-detail-${new Date().toISOString().slice(0, 10)}.json`;
              a.click();
              URL.revokeObjectURL(url);
            }}>
            导出 JSON
          </button>
        </div>
      )}

      {/* Table */}
      <div className="overflow-x-auto">
        {isLoading ? (
          <div className="py-8 text-center text-gray-400">查询中...</div>
        ) : !data || !data.items.length ? (
          <div className="py-8 text-center text-gray-400">暂无数据</div>
        ) : (
          <table className="w-full text-sm border-collapse">
            <thead>
              <tr className="border-b bg-gray-50">
                {Object.keys(data.items[0]).map(k => (
                  <th key={k} className="text-left px-3 py-2 text-xs text-gray-500 font-medium">{k}</th>
                ))}
              </tr>
            </thead>
            <tbody>
              {data.items.map((row, i) => (
                <tr key={i} className="border-b hover:bg-gray-50">
                  {Object.keys(data.items[0]).map(k => (
                    <td key={k} className="px-3 py-2 text-xs text-gray-700 max-w-[200px] truncate">
                      {String(row[k] ?? '—')}
                    </td>
                  ))}
                </tr>
              ))}
            </tbody>
          </table>
        )}
      </div>

      {/* Pagination */}
      {data && data.totalPages > 1 && (
        <div className="flex items-center justify-center gap-2">
          <button className="px-2 py-1 text-xs border rounded disabled:opacity-30"
            disabled={(params.page || 1) <= 1}
            onClick={() => update('page', Math.max(1, (params.page || 1) - 1))}>‹</button>
          <span className="text-xs text-gray-500">第 {data.page} / {data.totalPages} 页（共 {data.totalCount} 条）</span>
          <button className="px-2 py-1 text-xs border rounded disabled:opacity-30"
            disabled={(params.page || 1) >= data.totalPages}
            onClick={() => update('page', (params.page || 1) + 1)}>›</button>
        </div>
      )}
    </div>
  );
}
```

- [ ] **Step 2: 创建 PcDetailQueryPage.tsx**

```typescript
// src/client-web/src/pages/PcDetailQueryPage.tsx
import PcDetailQueryPanel from '../components/pc-tracker/PcDetailQueryPanel';

export default function PcDetailQueryPage() {
  return (
    <div className="max-w-5xl mx-auto">
      <h2 className="text-xl font-bold mb-6">PC记录 详细数据</h2>
      <div className="bg-white rounded-xl shadow-sm border p-5">
        <PcDetailQueryPanel />
      </div>
    </div>
  );
}
```

- [ ] **Step 3: 提交**

```bash
git add src/client-web/src/components/pc-tracker/PcDetailQueryPanel.tsx src/client-web/src/pages/PcDetailQueryPage.tsx
git commit -m "feat: add PcDetailQueryPanel and PcDetailQueryPage with filters and export"
```

---

### Task 14: 更新设置页面 + 路由

**Files:**
- Modify: `src/client-web/src/pages/SettingsPage.tsx`
- Modify: `src/client-web/src/layout/AppLayout.tsx`

- [ ] **Step 1: 更新 SettingsPage.tsx 添加 PC记录 卡片**

在 SettingsPage.tsx 的现有卡片之后、闭合 div 之前插入第二个卡片：

```typescript
// 在 "管理日程数据" 卡片之后添加：
<div
  className="bg-white border rounded-lg p-5 hover:border-blue-300 cursor-pointer transition-colors flex items-center justify-between mt-4"
  onClick={() => navigate('/settings/pc-data')}
>
  <div>
    <h3 className="font-semibold text-base flex items-center gap-2">
      <span>💻</span> PC记录详细数据
    </h3>
    <p className="text-sm text-gray-500 mt-1">
      查询、筛选、导出全部PC记录数据
    </p>
  </div>
  <span className="text-gray-300 text-xl">→</span>
</div>
```

- [ ] **Step 2: 在 AppLayout.tsx 中添加路由**

在 `<Route path="/settings/calendar-data" ... />` 之后添加：

```typescript
import PcDetailQueryPage from '../pages/PcDetailQueryPage';
// ...

<Route path="/settings/pc-data" element={<PcDetailQueryPage />} />
```

- [ ] **Step 3: 验证前端编译**

```bash
cd src/client-web; npx tsc --noEmit
```

Expected: 无类型错误

- [ ] **Step 4: 提交**

```bash
git add src/client-web/src/pages/SettingsPage.tsx src/client-web/src/layout/AppLayout.tsx
git commit -m "feat: add PC记录 detailed data card in settings and route"
```

---

## 验证步骤

完成所有任务后执行：

1. 后端编译：`dotnet build src/Pim.Api/Pim.Api.csproj`
2. 前端类型检查：`cd src/client-web && npx tsc --noEmit`
3. 启动后端和前端，访问 `/pc-tracker` 确认4个模块正常渲染
4. 访问 `/settings` → 点击 "PC记录详细数据" → 确认筛选+表格+导出功能正常
5. 切换热力图维度（时/日/月/年），确认网格重绘
6. 悬停时间线分类块，确认 tooltip 显示应用构成
