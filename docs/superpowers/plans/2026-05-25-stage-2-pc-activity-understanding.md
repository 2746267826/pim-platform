# 第 2 阶段 PC 活动理解实施计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 把 PC 记录分类从查询时的临时判断升级为可持久化、可预览、可确认、可审计、可平滑展示的本地理解闭环。

**Architecture:** 原始 ActivityWatch / KeyStats 数据继续作为事实源；分类规则描述“为什么这么判断”；新增分类快照保存“某次解释结果”。Web 只展示、纠错和确认范围；所有分类、预览、重算和审计都在服务端完成。

**Tech Stack:** C# / ASP.NET Core Minimal API / EF Core / PostgreSQL / xUnit / React / TypeScript / React Query / Tailwind CSS。

---

## 文件结构

后端新增文件：

- `src/modules/Pim.Module.PcTracker/Entities/ActivityClassificationEntity.cs`：保存派生分类快照。
- `src/modules/Pim.Module.PcTracker/Entities/ActivityClassificationSettingsEntity.cs`：保存分类体验设置，第一项是推荐最短分类时长。
- `src/modules/Pim.Module.PcTracker/Services/ActivityClassificationRecordKey.cs`：为不同来源记录生成稳定 key。
- `src/modules/Pim.Module.PcTracker/Services/ActivityClassificationSnapshotService.cs`：查询、创建、更新分类快照。
- `src/modules/Pim.Module.PcTracker/Services/ActivityClassificationRecomputeService.cs`：规则影响预览、范围重算、审计。
- `src/modules/Pim.Module.PcTracker/Services/ActivityTimelineSmoothingService.cs`：根据推荐最短分类时长平滑时间线和建议粒度。
- `src/modules/Pim.Module.PcTracker/Services/ActivityClassificationSettingsService.cs`：读取和保存设置。

后端修改文件：

- `src/modules/Pim.Module.PcTracker/DTOs/ActivityClassificationDtos.cs`：补充预览、重算、设置、项目标签 DTO。
- `src/modules/Pim.Module.PcTracker/Entities/EntityConfigurations.cs`：配置新实体索引。
- `src/modules/Pim.Module.PcTracker/Services/PcTrackerSchemaInitializer.cs`：创建新表和默认设置。
- `src/modules/Pim.Module.PcTracker/Services/PcTrackerService.cs`：查询时优先使用分类快照，缺失时补写。
- `src/modules/Pim.Module.PcTracker/Services/ActivitySuggestionService.cs`：让建议聚类尊重平滑阈值。
- `src/modules/Pim.Module.PcTracker/PcTrackerModule.cs`：注册服务和新增 API。

后端测试：

- `tests/Pim.UnitTests/Services/ActivityClassificationSnapshotServiceTests.cs`
- `tests/Pim.UnitTests/Services/ActivityClassificationRecomputeServiceTests.cs`
- `tests/Pim.UnitTests/Services/ActivityTimelineSmoothingServiceTests.cs`
- `tests/Pim.UnitTests/Services/ActivityClassificationSettingsServiceTests.cs`
- 修改 `tests/Pim.UnitTests/Services/ActivitySuggestionServiceTests.cs`
- 修改 `tests/Pim.UnitTests/Services/PcTrackerCompleteCaptureTests.cs`

前端新增文件：

- `src/client-web/src/pages/PcClassificationPage.tsx`：分类管理页。
- `src/client-web/src/components/pc-tracker/ClassificationSuggestionPanel.tsx`：PC 记录页待处理建议。
- `src/client-web/src/components/pc-tracker/QuickClassificationDialog.tsx`：快捷纠错与影响预览。
- `src/client-web/src/components/pc-classification/ClassificationRuleTable.tsx`：规则表。
- `src/client-web/src/components/pc-classification/ClassificationRuleEditor.tsx`：规则编辑器。
- `src/client-web/src/components/pc-classification/ClassificationRecomputePanel.tsx`：历史重算面板。

前端修改文件：

- `src/client-web/src/types/index.ts`：新增分类 DTO 类型。
- `src/client-web/src/api/pcTracker.ts`：新增分类 API 函数。
- `src/client-web/src/pages/PcTrackerPage.tsx`：加入建议面板与快捷纠错入口。
- `src/client-web/src/components/pc-tracker/CategoryTimeline.tsx`：接收服务端平滑后的时间线或显示平滑状态。
- `src/client-web/src/layout/AppLayout.tsx`：新增 `/pc-classification` 路由。
- `src/client-web/src/layout/Sidebar.tsx`：新增“分类管理”导航项。

前端测试：

- `tests/client-web/pcClassificationApiPath.test.ts`
- `tests/client-web/pcClassificationTypes.test.ts`

---

### Task 1: 分类快照与设置数据模型

**Files:**
- Create: `src/modules/Pim.Module.PcTracker/Entities/ActivityClassificationEntity.cs`
- Create: `src/modules/Pim.Module.PcTracker/Entities/ActivityClassificationSettingsEntity.cs`
- Modify: `src/modules/Pim.Module.PcTracker/Entities/EntityConfigurations.cs`
- Modify: `src/modules/Pim.Module.PcTracker/Services/PcTrackerSchemaInitializer.cs`
- Test: `tests/Pim.UnitTests/Operations/PimPcTrackerModelTests.cs`

- [ ] **Step 1: 写失败的模型测试**

在 `tests/Pim.UnitTests/Operations/PimPcTrackerModelTests.cs` 增加：

```csharp
[Fact]
public void PimDbContext_ConfiguresActivityClassificationSnapshotModel()
{
    using var db = CreateDbContext();

    var entity = db.Model.FindEntityType(typeof(ActivityClassificationEntity));

    Assert.NotNull(entity);
    Assert.Equal("pc_activity_classifications", entity!.GetTableName());
    Assert.Contains(entity.GetIndexes(), index =>
        index.GetDatabaseName() == "ux_pc_activity_classifications_record_key");
}

[Fact]
public void PimDbContext_ConfiguresActivityClassificationSettingsModel()
{
    using var db = CreateDbContext();

    var entity = db.Model.FindEntityType(typeof(ActivityClassificationSettingsEntity));

    Assert.NotNull(entity);
    Assert.Equal("pc_activity_classification_settings", entity!.GetTableName());
    Assert.Contains(entity.GetIndexes(), index =>
        index.GetDatabaseName() == "ux_pc_activity_classification_settings_key");
}
```

确认文件顶部包含：

```csharp
using Pim.Module.PcTracker.Entities;
```

- [ ] **Step 2: 运行模型测试确认失败**

Run:

```powershell
dotnet test tests/Pim.UnitTests/Pim.UnitTests.csproj --filter PimPcTrackerModelTests
```

Expected: FAIL，错误包含 `ActivityClassificationEntity` 或 `ActivityClassificationSettingsEntity` 不存在。

- [ ] **Step 3: 新增分类快照实体**

创建 `src/modules/Pim.Module.PcTracker/Entities/ActivityClassificationEntity.cs`：

```csharp
using System.ComponentModel.DataAnnotations;
using System.ComponentModel.DataAnnotations.Schema;

namespace Pim.Module.PcTracker.Entities;

[Table("pc_activity_classifications")]
public class ActivityClassificationEntity
{
    [Key]
    [Column("id")]
    public Guid Id { get; set; }

    [Column("record_key")]
    [MaxLength(256)]
    public string RecordKey { get; set; } = string.Empty;

    [Column("record_type")]
    [MaxLength(32)]
    public string RecordType { get; set; } = string.Empty;

    [Column("device_id")]
    [MaxLength(128)]
    public string DeviceId { get; set; } = string.Empty;

    [Column("source_event_ids", TypeName = "jsonb")]
    public string SourceEventIdsJson { get; set; } = "[]";

    [Column("started_at")]
    public DateTimeOffset StartedAt { get; set; }

    [Column("ended_at")]
    public DateTimeOffset EndedAt { get; set; }

    [Column("category_name")]
    [MaxLength(64)]
    public string CategoryName { get; set; } = "其他";

    [Column("category_color")]
    [MaxLength(7)]
    public string CategoryColor { get; set; } = "#64748b";

    [Column("project_tag")]
    [MaxLength(128)]
    public string? ProjectTag { get; set; }

    [Column("confidence")]
    public double Confidence { get; set; } = 0.2;

    [Column("source")]
    [MaxLength(32)]
    public string Source { get; set; } = "fallback";

    [Column("source_rule_id")]
    public Guid? SourceRuleId { get; set; }

    [Column("explanation")]
    public string Explanation { get; set; } = "No rule or heuristic matched.";

    [Column("classifier_version")]
    [MaxLength(32)]
    public string ClassifierVersion { get; set; } = "local-v1";

    [Column("classified_at")]
    public DateTimeOffset ClassifiedAt { get; set; } = DateTimeOffset.UtcNow;

    [Column("audit_id")]
    public Guid? AuditId { get; set; }
}
```

- [ ] **Step 4: 新增分类设置实体**

创建 `src/modules/Pim.Module.PcTracker/Entities/ActivityClassificationSettingsEntity.cs`：

```csharp
using System.ComponentModel.DataAnnotations;
using System.ComponentModel.DataAnnotations.Schema;

namespace Pim.Module.PcTracker.Entities;

[Table("pc_activity_classification_settings")]
public class ActivityClassificationSettingsEntity
{
    [Key]
    [Column("id")]
    public Guid Id { get; set; }

    [Column("settings_key")]
    [MaxLength(64)]
    public string SettingsKey { get; set; } = "default";

    [Column("recommended_minimum_classification_duration_minutes")]
    public int RecommendedMinimumClassificationDurationMinutes { get; set; } = 5;

    [Column("created_at")]
    public DateTimeOffset CreatedAt { get; set; } = DateTimeOffset.UtcNow;

    [Column("updated_at")]
    public DateTimeOffset UpdatedAt { get; set; } = DateTimeOffset.UtcNow;
}
```

- [ ] **Step 5: 配置 EF 索引**

在 `src/modules/Pim.Module.PcTracker/Entities/EntityConfigurations.cs` 末尾增加：

```csharp
public class ActivityClassificationEntityConfiguration : IEntityTypeConfiguration<ActivityClassificationEntity>
{
    public void Configure(EntityTypeBuilder<ActivityClassificationEntity> builder)
    {
        builder.ToTable("pc_activity_classifications");
        builder.Property(e => e.Id).HasDefaultValueSql("gen_random_uuid()");
        builder.HasIndex(e => e.RecordKey)
            .IsUnique()
            .HasDatabaseName("ux_pc_activity_classifications_record_key");
        builder.HasIndex(e => e.StartedAt)
            .HasDatabaseName("ix_pc_activity_classifications_started_at");
        builder.HasIndex(e => e.DeviceId)
            .HasDatabaseName("ix_pc_activity_classifications_device_id");
        builder.HasIndex(e => e.CategoryName)
            .HasDatabaseName("ix_pc_activity_classifications_category_name");
        builder.HasIndex(e => e.ProjectTag)
            .HasDatabaseName("ix_pc_activity_classifications_project_tag");
        builder.HasIndex(e => e.SourceRuleId)
            .HasDatabaseName("ix_pc_activity_classifications_source_rule_id");
    }
}

public class ActivityClassificationSettingsEntityConfiguration : IEntityTypeConfiguration<ActivityClassificationSettingsEntity>
{
    public void Configure(EntityTypeBuilder<ActivityClassificationSettingsEntity> builder)
    {
        builder.ToTable("pc_activity_classification_settings");
        builder.Property(e => e.Id).HasDefaultValueSql("gen_random_uuid()");
        builder.HasIndex(e => e.SettingsKey)
            .IsUnique()
            .HasDatabaseName("ux_pc_activity_classification_settings_key");
    }
}
```

- [ ] **Step 6: 扩展运行时 schema 初始化**

在 `src/modules/Pim.Module.PcTracker/Services/PcTrackerSchemaInitializer.cs` 的 `SchemaSql` 中加入：

```sql
CREATE TABLE IF NOT EXISTS pc_activity_classifications (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    record_key VARCHAR(256) NOT NULL,
    record_type VARCHAR(32) NOT NULL,
    device_id VARCHAR(128) NOT NULL,
    source_event_ids JSONB NOT NULL DEFAULT '[]'::jsonb,
    started_at TIMESTAMPTZ NOT NULL,
    ended_at TIMESTAMPTZ NOT NULL,
    category_name VARCHAR(64) NOT NULL DEFAULT '其他',
    category_color VARCHAR(7) NOT NULL DEFAULT '#64748b',
    project_tag VARCHAR(128),
    confidence DOUBLE PRECISION NOT NULL DEFAULT 0.2,
    source VARCHAR(32) NOT NULL DEFAULT 'fallback',
    source_rule_id UUID,
    explanation TEXT NOT NULL DEFAULT 'No rule or heuristic matched.',
    classifier_version VARCHAR(32) NOT NULL DEFAULT 'local-v1',
    classified_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    audit_id UUID
);
CREATE UNIQUE INDEX IF NOT EXISTS ux_pc_activity_classifications_record_key
    ON pc_activity_classifications (record_key);
CREATE INDEX IF NOT EXISTS ix_pc_activity_classifications_started_at
    ON pc_activity_classifications (started_at);
CREATE INDEX IF NOT EXISTS ix_pc_activity_classifications_device_id
    ON pc_activity_classifications (device_id);
CREATE INDEX IF NOT EXISTS ix_pc_activity_classifications_category_name
    ON pc_activity_classifications (category_name);
CREATE INDEX IF NOT EXISTS ix_pc_activity_classifications_project_tag
    ON pc_activity_classifications (project_tag);
CREATE INDEX IF NOT EXISTS ix_pc_activity_classifications_source_rule_id
    ON pc_activity_classifications (source_rule_id);

CREATE TABLE IF NOT EXISTS pc_activity_classification_settings (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    settings_key VARCHAR(64) NOT NULL DEFAULT 'default',
    recommended_minimum_classification_duration_minutes INT NOT NULL DEFAULT 5,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
CREATE UNIQUE INDEX IF NOT EXISTS ux_pc_activity_classification_settings_key
    ON pc_activity_classification_settings (settings_key);
INSERT INTO pc_activity_classification_settings (
    settings_key,
    recommended_minimum_classification_duration_minutes
)
VALUES ('default', 5)
ON CONFLICT (settings_key) DO NOTHING;
```

- [ ] **Step 7: 跑测试确认通过**

Run:

```powershell
dotnet test tests/Pim.UnitTests/Pim.UnitTests.csproj --filter PimPcTrackerModelTests
```

Expected: PASS。

- [ ] **Step 8: 提交**

```powershell
git add src/modules/Pim.Module.PcTracker/Entities/ActivityClassificationEntity.cs src/modules/Pim.Module.PcTracker/Entities/ActivityClassificationSettingsEntity.cs src/modules/Pim.Module.PcTracker/Entities/EntityConfigurations.cs src/modules/Pim.Module.PcTracker/Services/PcTrackerSchemaInitializer.cs tests/Pim.UnitTests/Operations/PimPcTrackerModelTests.cs
git commit -m "feat(pc): add activity classification persistence"
```

---

### Task 2: 稳定记录 key 与分类快照服务

**Files:**
- Create: `src/modules/Pim.Module.PcTracker/Services/ActivityClassificationRecordKey.cs`
- Create: `src/modules/Pim.Module.PcTracker/Services/ActivityClassificationSnapshotService.cs`
- Modify: `src/modules/Pim.Module.PcTracker/PcTrackerModule.cs`
- Test: `tests/Pim.UnitTests/Services/ActivityClassificationSnapshotServiceTests.cs`

- [ ] **Step 1: 写失败的快照服务测试**

创建 `tests/Pim.UnitTests/Services/ActivityClassificationSnapshotServiceTests.cs`：

```csharp
using Microsoft.EntityFrameworkCore;
using Pim.Infrastructure.Data;
using Pim.Module.PcTracker.DTOs;
using Pim.Module.PcTracker.Entities;
using Pim.Module.PcTracker.Services;
using Xunit;

namespace Pim.UnitTests.Services;

public class ActivityClassificationSnapshotServiceTests
{
    [Fact]
    public async Task EnsureClassificationsAsync_CreatesDeterministicSnapshotWithoutChangingRecord()
    {
        using var db = CreateDb();
        var service = new ActivityClassificationSnapshotService(db);
        var record = NewRecord("Code", "Editing project");

        var classified = await service.EnsureClassificationsAsync(
            new[] { record },
            new[]
            {
                NewRule("Code is programming", "编程", "#6B5EE4", 500,
                    """{"all":[{"field":"appNameNormalized","op":"equals","value":"code"}]}""")
            },
            auditId: null,
            CancellationToken.None);

        var item = Assert.Single(classified);
        Assert.Equal("编程", item.CategoryName);
        Assert.Equal("其他", record.CategoryName);
        var snapshot = await db.Set<ActivityClassificationEntity>().SingleAsync();
        Assert.Equal(ActivityClassificationRecordKey.FromRecord(record), snapshot.RecordKey);
        Assert.Equal("编程", snapshot.CategoryName);
    }

    [Fact]
    public async Task EnsureClassificationsAsync_UpdatesExistingSnapshotForSameRecordKey()
    {
        using var db = CreateDb();
        var service = new ActivityClassificationSnapshotService(db);
        var record = NewRecord("Code", "Editing project");
        await service.EnsureClassificationsAsync(
            new[] { record },
            new[] { NewRule("Code is programming", "编程", "#6B5EE4", 500, """{"all":[{"field":"appNameNormalized","op":"equals","value":"code"}]}""") },
            null,
            CancellationToken.None);

        var classified = await service.EnsureClassificationsAsync(
            new[] { record },
            new[] { NewRule("Code is office override", "办公", "#F59E0B", 900, """{"all":[{"field":"appNameNormalized","op":"equals","value":"code"}]}""") },
            Guid.NewGuid(),
            CancellationToken.None);

        Assert.Single(classified);
        Assert.Equal("办公", classified[0].CategoryName);
        Assert.Equal(1, await db.Set<ActivityClassificationEntity>().CountAsync());
        var snapshot = await db.Set<ActivityClassificationEntity>().SingleAsync();
        Assert.Equal("办公", snapshot.CategoryName);
        Assert.NotNull(snapshot.AuditId);
    }

    private static PimDbContext CreateDb()
    {
        PimDbContext.RegisterModuleAssembly(typeof(ActivityClassificationEntity).Assembly);
        var options = new DbContextOptionsBuilder<PimDbContext>()
            .UseInMemoryDatabase(Guid.NewGuid().ToString())
            .Options;
        return new PimDbContext(options);
    }

    private static PcDetailRecord NewRecord(string appName, string title)
    {
        return new PcDetailRecord(
            "window",
            "2026-05-25T08:00:00Z",
            "2026-05-25T08:10:00Z",
            600,
            "device-1",
            appName,
            appName,
            "其他",
            title,
            null,
            null,
            null,
            null,
            null,
            null);
    }

    private static ActivityCategoryRuleEntity NewRule(
        string name,
        string category,
        string color,
        int priority,
        string conditionsJson)
    {
        return new ActivityCategoryRuleEntity
        {
            Id = Guid.NewGuid(),
            RuleName = name,
            Scope = "activity",
            CategoryName = category,
            Color = color,
            Priority = priority,
            Source = "user",
            Status = "active",
            ConditionsJson = conditionsJson,
            Confidence = 0.95,
            Explanation = name
        };
    }
}
```

- [ ] **Step 2: 运行测试确认失败**

Run:

```powershell
dotnet test tests/Pim.UnitTests/Pim.UnitTests.csproj --filter ActivityClassificationSnapshotServiceTests
```

Expected: FAIL，错误包含 `ActivityClassificationSnapshotService` 不存在。

- [ ] **Step 3: 实现稳定 key 工具**

创建 `src/modules/Pim.Module.PcTracker/Services/ActivityClassificationRecordKey.cs`：

```csharp
using System.Security.Cryptography;
using System.Text;
using System.Text.Json;
using Pim.Module.PcTracker.DTOs;

namespace Pim.Module.PcTracker.Services;

public static class ActivityClassificationRecordKey
{
    public static string FromRecord(PcDetailRecord record)
    {
        var sourceIds = record.SourceWebEventIds?.Count > 0
            ? string.Join(",", record.SourceWebEventIds.Order())
            : record.SourceWindowEventIds?.Count > 0
                ? string.Join(",", record.SourceWindowEventIds.Order())
                : string.Empty;

        var rawKey = string.Join("|",
            record.RecordType,
            record.DeviceId,
            record.Start,
            record.End ?? record.Start,
            record.AppName ?? string.Empty,
            record.Domain ?? string.Empty,
            record.Path ?? string.Empty,
            record.Title ?? string.Empty,
            sourceIds);

        return $"{record.RecordType}:{ShortHash(rawKey)}";
    }

    public static string SourceEventIdsJson(PcDetailRecord record)
    {
        var ids = record.SourceWebEventIds?.Count > 0
            ? record.SourceWebEventIds
            : record.SourceWindowEventIds?.Count > 0
                ? record.SourceWindowEventIds
                : new List<long>();

        return JsonSerializer.Serialize(ids.Order().ToArray());
    }

    private static string ShortHash(string value)
    {
        var bytes = SHA256.HashData(Encoding.UTF8.GetBytes(value));
        return Convert.ToHexString(bytes).ToLowerInvariant()[..32];
    }
}
```

- [ ] **Step 4: 实现分类快照服务**

创建 `src/modules/Pim.Module.PcTracker/Services/ActivityClassificationSnapshotService.cs`：

```csharp
using Microsoft.EntityFrameworkCore;
using Pim.Infrastructure.Data;
using Pim.Module.PcTracker.DTOs;
using Pim.Module.PcTracker.Entities;

namespace Pim.Module.PcTracker.Services;

public class ActivityClassificationSnapshotService
{
    public const string ClassifierVersion = "local-v1";

    private readonly PimDbContext _db;

    public ActivityClassificationSnapshotService(PimDbContext db)
    {
        _db = db;
    }

    public async Task<List<PcDetailRecord>> EnsureClassificationsAsync(
        IReadOnlyCollection<PcDetailRecord> records,
        IReadOnlyCollection<ActivityCategoryRuleEntity> rules,
        Guid? auditId,
        CancellationToken ct)
    {
        var validRecords = records
            .Where(r => r.DurationSeconds is > 0)
            .ToList();

        if (validRecords.Count == 0)
            return records.ToList();

        var keys = validRecords
            .Select(ActivityClassificationRecordKey.FromRecord)
            .Distinct(StringComparer.Ordinal)
            .ToList();
        var existing = await _db.Set<ActivityClassificationEntity>()
            .Where(c => keys.Contains(c.RecordKey))
            .ToDictionaryAsync(c => c.RecordKey, StringComparer.Ordinal, ct);

        var result = new List<PcDetailRecord>();
        foreach (var record in records)
        {
            if (record.DurationSeconds is not > 0)
            {
                result.Add(record);
                continue;
            }

            var key = ActivityClassificationRecordKey.FromRecord(record);
            var classification = ActivityClassifier.Classify(ToContext(record), rules);
            var now = DateTimeOffset.UtcNow;
            var snapshot = existing.TryGetValue(key, out var found)
                ? found
                : new ActivityClassificationEntity
                {
                    Id = Guid.NewGuid(),
                    RecordKey = key,
                    RecordType = record.RecordType,
                    DeviceId = record.DeviceId,
                    SourceEventIdsJson = ActivityClassificationRecordKey.SourceEventIdsJson(record),
                    StartedAt = DateTimeOffset.Parse(record.Start),
                    EndedAt = DateTimeOffset.Parse(record.End ?? record.Start)
                };

            snapshot.CategoryName = classification.CategoryName;
            snapshot.CategoryColor = classification.CategoryColor;
            snapshot.ProjectTag = classification.ProjectTag;
            snapshot.Confidence = classification.Confidence;
            snapshot.Source = classification.Source;
            snapshot.SourceRuleId = classification.SourceRuleId;
            snapshot.Explanation = classification.Explanation;
            snapshot.ClassifierVersion = ClassifierVersion;
            snapshot.ClassifiedAt = now;
            snapshot.AuditId = auditId;

            if (!existing.ContainsKey(key))
            {
                _db.Set<ActivityClassificationEntity>().Add(snapshot);
                existing[key] = snapshot;
            }

            result.Add(ApplySnapshot(record, snapshot));
        }

        await _db.SaveChangesAsync(ct);
        return result;
    }

    public static ActivityClassificationContext ToContext(PcDetailRecord record)
    {
        return new ActivityClassificationContext(
            record.RecordType,
            record.AppName ?? record.BrowserAppName,
            AppNameNormalizer.Normalize(record.AppName ?? record.BrowserAppName),
            record.Domain,
            record.Path,
            record.Title,
            record.BrowserWindowTitle,
            record.Url,
            record.RecordType);
    }

    private static PcDetailRecord ApplySnapshot(PcDetailRecord record, ActivityClassificationEntity snapshot)
    {
        return record with
        {
            CategoryName = snapshot.CategoryName,
            CategoryColor = snapshot.CategoryColor,
            ProjectTag = snapshot.ProjectTag,
            ClassificationConfidence = snapshot.Confidence,
            ClassificationSource = snapshot.Source,
            ClassificationExplanation = snapshot.Explanation
        };
    }
}
```

- [ ] **Step 5: 注册服务**

在 `src/modules/Pim.Module.PcTracker/PcTrackerModule.cs` 的 `RegisterServices` 中加入：

```csharp
services.AddScoped<ActivityClassificationSnapshotService>();
```

- [ ] **Step 6: 跑测试确认通过**

Run:

```powershell
dotnet test tests/Pim.UnitTests/Pim.UnitTests.csproj --filter ActivityClassificationSnapshotServiceTests
```

Expected: PASS。

- [ ] **Step 7: 提交**

```powershell
git add src/modules/Pim.Module.PcTracker/Services/ActivityClassificationRecordKey.cs src/modules/Pim.Module.PcTracker/Services/ActivityClassificationSnapshotService.cs src/modules/Pim.Module.PcTracker/PcTrackerModule.cs tests/Pim.UnitTests/Services/ActivityClassificationSnapshotServiceTests.cs
git commit -m "feat(pc): persist activity classification snapshots"
```

---

### Task 3: 查询路径使用分类快照

**Files:**
- Modify: `src/modules/Pim.Module.PcTracker/Services/PcTrackerService.cs`
- Test: `tests/Pim.UnitTests/Services/PcTrackerCompleteCaptureTests.cs`

- [ ] **Step 1: 写失败测试**

在 `tests/Pim.UnitTests/Services/PcTrackerCompleteCaptureTests.cs` 增加测试：

```csharp
[Fact]
public async Task QueryCompleteDetailAsync_PersistsAndReturnsClassificationSnapshots()
{
    PimDbContext.RegisterModuleAssembly(typeof(AwEventEntity).Assembly);
    var options = new DbContextOptionsBuilder<PimDbContext>()
        .UseInMemoryDatabase(Guid.NewGuid().ToString())
        .Options;
    using var db = new PimDbContext(options);
    db.Set<ActivityCategoryRuleEntity>().Add(new ActivityCategoryRuleEntity
    {
        Id = Guid.NewGuid(),
        RuleName = "Code window",
        Scope = "activity",
        CategoryName = "编程",
        Color = "#6B5EE4",
        Priority = 500,
        Source = "user",
        Status = "active",
        ConditionsJson = """{"all":[{"field":"appNameNormalized","op":"equals","value":"code"}]}""",
        Confidence = 0.95,
        Explanation = "Code app rule"
    });
    db.Set<AwEventEntity>().Add(new AwEventEntity
    {
        Id = 1,
        DeviceId = "device-1",
        EventType = "window",
        AppName = "Code",
        AppNameNormalized = "code",
        WindowTitle = "project",
        Timestamp = DateTimeOffset.Parse("2026-05-25T08:00:00Z"),
        Duration = 600,
        CreatedAt = DateTimeOffset.UtcNow
    });
    await db.SaveChangesAsync();
    var service = new PcTrackerService(db, new ActivityClassificationSnapshotService(db));

    var result = await service.QueryCompleteDetailAsync(new DetailQueryParams(
        "2026-05-25",
        "2026-05-25",
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        "asc",
        1,
        20), CancellationToken.None);

    var record = Assert.Single(result.Items);
    Assert.Equal("编程", record.CategoryName);
    Assert.Equal(1, await db.Set<ActivityClassificationEntity>().CountAsync());
}
```

如果 `PcTrackerService` 构造函数当前只接收 `PimDbContext`，这个测试会先因构造函数签名失败。

- [ ] **Step 2: 运行测试确认失败**

Run:

```powershell
dotnet test tests/Pim.UnitTests/Pim.UnitTests.csproj --filter QueryCompleteDetailAsync_PersistsAndReturnsClassificationSnapshots
```

Expected: FAIL。

- [ ] **Step 3: 修改服务构造函数**

在 `src/modules/Pim.Module.PcTracker/Services/PcTrackerService.cs` 中把字段与构造函数改为：

```csharp
private readonly PimDbContext _db;
private readonly ActivityClassificationSnapshotService _classificationSnapshots;

public PcTrackerService(
    PimDbContext db,
    ActivityClassificationSnapshotService classificationSnapshots)
{
    _db = db;
    _classificationSnapshots = classificationSnapshots;
}
```

同步更新所有测试里 `new PcTrackerService(db)` 为：

```csharp
new PcTrackerService(db, new ActivityClassificationSnapshotService(db))
```

- [ ] **Step 4: 在完整明细查询中补写快照**

在 `QueryCompleteDetailAsync` 中，生成并过滤 `records` 后、排序分页前加入：

```csharp
records = await _classificationSnapshots.EnsureClassificationsAsync(
    records,
    await GetActivityCategoryRulesAsync(ct),
    auditId: null,
    ct);
```

保持过滤顺序为：先从原始记录构造解释视图，再补分类快照，再按 `categoryName` 等查询参数过滤。

- [ ] **Step 5: 在汇总时间线查询中使用同一快照路径**

把 `BuildInterpretedAwDetailRecordsAsync` 改为：

```csharp
private async Task<List<PcDetailRecord>> BuildInterpretedAwDetailRecordsAsync(
    List<AwEventEntity> awEvents,
    CancellationToken ct)
{
    var records = BrowserPageTimelineBuilder.BuildInterpretedAwRecords(
        awEvents,
        await GetActivityCategoryRulesAsync(ct));
    return await _classificationSnapshots.EnsureClassificationsAsync(
        records,
        await GetActivityCategoryRulesAsync(ct),
        auditId: null,
        ct);
}
```

- [ ] **Step 6: 运行相关测试**

Run:

```powershell
dotnet test tests/Pim.UnitTests/Pim.UnitTests.csproj --filter "PcTrackerCompleteCaptureTests|ActivityClassificationSnapshotServiceTests"
```

Expected: PASS。

- [ ] **Step 7: 提交**

```powershell
git add src/modules/Pim.Module.PcTracker/Services/PcTrackerService.cs tests/Pim.UnitTests/Services/PcTrackerCompleteCaptureTests.cs
git commit -m "feat(pc): use persisted classifications in pc queries"
```

---

### Task 4: 分类设置与时间线平滑

**Files:**
- Create: `src/modules/Pim.Module.PcTracker/Services/ActivityClassificationSettingsService.cs`
- Create: `src/modules/Pim.Module.PcTracker/Services/ActivityTimelineSmoothingService.cs`
- Modify: `src/modules/Pim.Module.PcTracker/DTOs/ActivityClassificationDtos.cs`
- Modify: `src/modules/Pim.Module.PcTracker/PcTrackerModule.cs`
- Test: `tests/Pim.UnitTests/Services/ActivityClassificationSettingsServiceTests.cs`
- Test: `tests/Pim.UnitTests/Services/ActivityTimelineSmoothingServiceTests.cs`

- [ ] **Step 1: 写设置服务失败测试**

创建 `tests/Pim.UnitTests/Services/ActivityClassificationSettingsServiceTests.cs`：

```csharp
using Microsoft.EntityFrameworkCore;
using Pim.Infrastructure.Data;
using Pim.Module.PcTracker.Entities;
using Pim.Module.PcTracker.Services;
using Xunit;

namespace Pim.UnitTests.Services;

public class ActivityClassificationSettingsServiceTests
{
    [Fact]
    public async Task GetSettingsAsync_CreatesDefaultFiveMinuteSetting()
    {
        using var db = CreateDb();
        var service = new ActivityClassificationSettingsService(db);

        var settings = await service.GetSettingsAsync(CancellationToken.None);

        Assert.Equal(5, settings.RecommendedMinimumClassificationDurationMinutes);
        Assert.Equal(1, await db.Set<ActivityClassificationSettingsEntity>().CountAsync());
    }

    [Fact]
    public async Task SaveSettingsAsync_ClampsToSupportedPreset()
    {
        using var db = CreateDb();
        var service = new ActivityClassificationSettingsService(db);

        var settings = await service.SaveSettingsAsync(7, CancellationToken.None);

        Assert.Equal(5, settings.RecommendedMinimumClassificationDurationMinutes);
    }

    private static PimDbContext CreateDb()
    {
        PimDbContext.RegisterModuleAssembly(typeof(ActivityClassificationSettingsEntity).Assembly);
        var options = new DbContextOptionsBuilder<PimDbContext>()
            .UseInMemoryDatabase(Guid.NewGuid().ToString())
            .Options;
        return new PimDbContext(options);
    }
}
```

- [ ] **Step 2: 写平滑服务失败测试**

创建 `tests/Pim.UnitTests/Services/ActivityTimelineSmoothingServiceTests.cs`：

```csharp
using Pim.Module.PcTracker.DTOs;
using Pim.Module.PcTracker.Services;
using Xunit;

namespace Pim.UnitTests.Services;

public class ActivityTimelineSmoothingServiceTests
{
    [Fact]
    public void Smooth_MergesLowConfidenceShortBlockBetweenSameProjectBlocks()
    {
        var service = new ActivityTimelineSmoothingService();
        var items = new[]
        {
            Item("08:00", "08:10", "编程", "PIM", 0.9, "rule"),
            Item("08:10", "08:12", "其他", null, 0.2, "fallback"),
            Item("08:12", "08:30", "编程", "PIM", 0.9, "rule")
        };

        var smoothed = service.Smooth(items, recommendedMinimumMinutes: 5);

        var block = Assert.Single(smoothed);
        Assert.Equal("编程", block.CategoryName);
        Assert.Equal(30, block.DurationMinutes);
    }

    [Fact]
    public void Smooth_KeepsStrongShortCommunicationBlock()
    {
        var service = new ActivityTimelineSmoothingService();
        var items = new[]
        {
            Item("08:00", "08:10", "编程", "PIM", 0.9, "rule"),
            Item("08:10", "08:11", "沟通", null, 0.95, "rule"),
            Item("08:11", "08:30", "编程", "PIM", 0.9, "rule")
        };

        var smoothed = service.Smooth(items, recommendedMinimumMinutes: 5);

        Assert.Equal(3, smoothed.Count);
        Assert.Equal("沟通", smoothed[1].CategoryName);
    }

    private static TimelineItem Item(
        string start,
        string end,
        string category,
        string? projectTag,
        double confidence,
        string source)
    {
        return new TimelineItem(
            $"2026-05-25T{start}:00Z",
            $"2026-05-25T{end}:00Z",
            (DateTimeOffset.Parse($"2026-05-25T{end}:00Z") - DateTimeOffset.Parse($"2026-05-25T{start}:00Z")).TotalMinutes,
            category == "其他" ? "unknown.example.com" : "Code",
            null,
            category,
            category == "编程" ? "#6B5EE4" : category == "沟通" ? "#F5935A" : "#64748b",
            projectTag,
            confidence,
            source,
            source == "fallback" ? "No rule matched." : "Strong rule.");
    }
}
```

- [ ] **Step 3: 运行测试确认失败**

Run:

```powershell
dotnet test tests/Pim.UnitTests/Pim.UnitTests.csproj --filter "ActivityClassificationSettingsServiceTests|ActivityTimelineSmoothingServiceTests"
```

Expected: FAIL。

- [ ] **Step 4: 增加 DTO**

在 `src/modules/Pim.Module.PcTracker/DTOs/ActivityClassificationDtos.cs` 末尾增加：

```csharp
public record ActivityClassificationSettingsDto(
    int RecommendedMinimumClassificationDurationMinutes,
    IReadOnlyList<int> SupportedRecommendedMinimumDurations);

public record SaveActivityClassificationSettingsRequest(
    int RecommendedMinimumClassificationDurationMinutes);
```

- [ ] **Step 5: 实现设置服务**

创建 `src/modules/Pim.Module.PcTracker/Services/ActivityClassificationSettingsService.cs`：

```csharp
using Microsoft.EntityFrameworkCore;
using Pim.Infrastructure.Data;
using Pim.Module.PcTracker.DTOs;
using Pim.Module.PcTracker.Entities;

namespace Pim.Module.PcTracker.Services;

public class ActivityClassificationSettingsService
{
    private const string DefaultKey = "default";
    private static readonly int[] SupportedDurations = [1, 3, 5, 10, 15];
    private readonly PimDbContext _db;

    public ActivityClassificationSettingsService(PimDbContext db)
    {
        _db = db;
    }

    public async Task<ActivityClassificationSettingsDto> GetSettingsAsync(CancellationToken ct)
    {
        var entity = await GetOrCreateAsync(ct);
        return ToDto(entity);
    }

    public async Task<ActivityClassificationSettingsDto> SaveSettingsAsync(int requestedMinutes, CancellationToken ct)
    {
        var entity = await GetOrCreateAsync(ct);
        entity.RecommendedMinimumClassificationDurationMinutes = NearestSupported(requestedMinutes);
        entity.UpdatedAt = DateTimeOffset.UtcNow;
        await _db.SaveChangesAsync(ct);
        return ToDto(entity);
    }

    private async Task<ActivityClassificationSettingsEntity> GetOrCreateAsync(CancellationToken ct)
    {
        var entity = await _db.Set<ActivityClassificationSettingsEntity>()
            .FirstOrDefaultAsync(s => s.SettingsKey == DefaultKey, ct);
        if (entity is not null)
            return entity;

        entity = new ActivityClassificationSettingsEntity
        {
            Id = Guid.NewGuid(),
            SettingsKey = DefaultKey,
            RecommendedMinimumClassificationDurationMinutes = 5,
            CreatedAt = DateTimeOffset.UtcNow,
            UpdatedAt = DateTimeOffset.UtcNow
        };
        _db.Set<ActivityClassificationSettingsEntity>().Add(entity);
        await _db.SaveChangesAsync(ct);
        return entity;
    }

    private static int NearestSupported(int requested)
    {
        return SupportedDurations
            .OrderBy(value => Math.Abs(value - requested))
            .ThenBy(value => value)
            .First();
    }

    private static ActivityClassificationSettingsDto ToDto(ActivityClassificationSettingsEntity entity)
    {
        return new ActivityClassificationSettingsDto(
            entity.RecommendedMinimumClassificationDurationMinutes,
            SupportedDurations);
    }
}
```

- [ ] **Step 6: 实现平滑服务**

创建 `src/modules/Pim.Module.PcTracker/Services/ActivityTimelineSmoothingService.cs`：

```csharp
using Pim.Module.PcTracker.DTOs;

namespace Pim.Module.PcTracker.Services;

public class ActivityTimelineSmoothingService
{
    public List<TimelineItem> Smooth(IReadOnlyList<TimelineItem> items, int recommendedMinimumMinutes)
    {
        if (items.Count < 3 || recommendedMinimumMinutes <= 1)
            return items.ToList();

        var ordered = items
            .OrderBy(item => DateTimeOffset.Parse(item.Start))
            .ToList();
        var result = new List<TimelineItem>();

        for (var i = 0; i < ordered.Count; i++)
        {
            var current = ordered[i];
            if (i > 0
                && i < ordered.Count - 1
                && CanMerge(current, ordered[i - 1], ordered[i + 1], recommendedMinimumMinutes))
            {
                var previous = result[^1];
                result[^1] = previous with
                {
                    End = ordered[i + 1].End,
                    DurationMinutes = previous.DurationMinutes + current.DurationMinutes + ordered[i + 1].DurationMinutes,
                    ClassificationExplanation = $"{previous.ClassificationExplanation} Short low-confidence activity was smoothed into surrounding context."
                };
                i++;
                continue;
            }

            result.Add(current);
        }

        return result;
    }

    private static bool CanMerge(
        TimelineItem current,
        TimelineItem previous,
        TimelineItem next,
        int recommendedMinimumMinutes)
    {
        return current.DurationMinutes < recommendedMinimumMinutes
            && current.ClassificationConfidence < 0.5
            && !string.Equals(current.ClassificationSource, "rule", StringComparison.OrdinalIgnoreCase)
            && string.IsNullOrWhiteSpace(current.ProjectTag)
            && previous.CategoryName == next.CategoryName
            && string.Equals(previous.ProjectTag, next.ProjectTag, StringComparison.OrdinalIgnoreCase)
            && previous.ClassificationConfidence >= 0.7
            && next.ClassificationConfidence >= 0.7;
    }
}
```

- [ ] **Step 7: 注册服务并新增设置 API**

在 `PcTrackerModule.RegisterServices` 加入：

```csharp
services.AddScoped<ActivityClassificationSettingsService>();
services.AddScoped<ActivityTimelineSmoothingService>();
```

在 `PcTrackerModule.MapEndpoints` 中加入：

```csharp
readGroup.MapGet("/classification/settings", async (
    [FromServices] ActivityClassificationSettingsService settingsService,
    CancellationToken ct) =>
{
    var settings = await settingsService.GetSettingsAsync(ct);
    return Results.Ok(ApiResponse<ActivityClassificationSettingsDto>.Ok(settings));
});

writeGroup.MapPut("/classification/settings", async (
    [FromBody] SaveActivityClassificationSettingsRequest req,
    [FromServices] ActivityClassificationSettingsService settingsService,
    CancellationToken ct) =>
{
    var settings = await settingsService.SaveSettingsAsync(
        req.RecommendedMinimumClassificationDurationMinutes,
        ct);
    return Results.Ok(ApiResponse<ActivityClassificationSettingsDto>.Ok(settings));
});
```

- [ ] **Step 8: 在汇总时间线中应用平滑**

给 `PcTrackerService` 注入 `ActivityClassificationSettingsService` 和 `ActivityTimelineSmoothingService`：

```csharp
private readonly ActivityClassificationSettingsService _classificationSettings;
private readonly ActivityTimelineSmoothingService _timelineSmoothing;
```

构造函数参数增加：

```csharp
ActivityClassificationSettingsService classificationSettings,
ActivityTimelineSmoothingService timelineSmoothing
```

在 `GetSummaryAsync` 和 `GetTimelineAsync` 中 `NormalizeTimelineItems` 后加入：

```csharp
var settings = await _classificationSettings.GetSettingsAsync(ct);
timeline = _timelineSmoothing.Smooth(
    timeline,
    settings.RecommendedMinimumClassificationDurationMinutes);
```

同步更新测试中的 `PcTrackerService` 构造方式。

- [ ] **Step 9: 运行测试**

Run:

```powershell
dotnet test tests/Pim.UnitTests/Pim.UnitTests.csproj --filter "ActivityClassificationSettingsServiceTests|ActivityTimelineSmoothingServiceTests|PcTrackerCompleteCaptureTests"
```

Expected: PASS。

- [ ] **Step 10: 提交**

```powershell
git add src/modules/Pim.Module.PcTracker/DTOs/ActivityClassificationDtos.cs src/modules/Pim.Module.PcTracker/Services/ActivityClassificationSettingsService.cs src/modules/Pim.Module.PcTracker/Services/ActivityTimelineSmoothingService.cs src/modules/Pim.Module.PcTracker/Services/PcTrackerService.cs src/modules/Pim.Module.PcTracker/PcTrackerModule.cs tests/Pim.UnitTests/Services/ActivityClassificationSettingsServiceTests.cs tests/Pim.UnitTests/Services/ActivityTimelineSmoothingServiceTests.cs tests/Pim.UnitTests/Services/PcTrackerCompleteCaptureTests.cs
git commit -m "feat(pc): add classification smoothing settings"
```

---

### Task 5: 规则影响预览、应用范围与审计重算

**Files:**
- Create: `src/modules/Pim.Module.PcTracker/Services/ActivityClassificationRecomputeService.cs`
- Modify: `src/modules/Pim.Module.PcTracker/DTOs/ActivityClassificationDtos.cs`
- Modify: `src/modules/Pim.Module.PcTracker/PcTrackerModule.cs`
- Test: `tests/Pim.UnitTests/Services/ActivityClassificationRecomputeServiceTests.cs`

- [ ] **Step 1: 写失败测试**

创建 `tests/Pim.UnitTests/Services/ActivityClassificationRecomputeServiceTests.cs`：

```csharp
using Microsoft.EntityFrameworkCore;
using Pim.Core.Operations;
using Pim.Infrastructure.Data;
using Pim.Infrastructure.Operations;
using Pim.Module.PcTracker.DTOs;
using Pim.Module.PcTracker.Entities;
using Pim.Module.PcTracker.Services;
using Xunit;

namespace Pim.UnitTests.Services;

public class ActivityClassificationRecomputeServiceTests
{
    [Fact]
    public async Task PreviewRuleAsync_ReturnsAffectedCountsWithoutSavingRule()
    {
        using var db = CreateDb();
        AddWindow(db, "Code", "2026-05-25T08:00:00Z", 600);
        await db.SaveChangesAsync();
        var service = NewService(db);

        var preview = await service.PreviewRuleAsync(NewRuleRequest("Code rule"), NewRange("today", "2026-05-25", "2026-05-25"), CancellationToken.None);

        Assert.Equal(1, preview.AffectedRecordCount);
        Assert.Equal(600, preview.AffectedDurationSeconds);
        Assert.Empty(await db.Set<ActivityCategoryRuleEntity>().ToListAsync());
    }

    [Fact]
    public async Task ApplyRuleAsync_SavesRuleRecomputesRangeAndWritesAudit()
    {
        using var db = CreateDb();
        AddWindow(db, "Code", "2026-05-25T08:00:00Z", 600);
        AddWindow(db, "Code", "2026-05-26T08:00:00Z", 600);
        await db.SaveChangesAsync();
        var service = NewService(db);

        var result = await service.ApplyRuleAsync(NewRuleRequest("Code rule"), NewRange("range", "2026-05-25", "2026-05-25"), CancellationToken.None);

        Assert.Equal(1, result.AffectedRecordCount);
        Assert.Single(await db.Set<ActivityCategoryRuleEntity>().ToListAsync());
        Assert.Single(await db.Set<ActivityClassificationEntity>().ToListAsync());
        var audit = await db.AuditLogs.SingleAsync();
        Assert.Equal("pc.classification.rule.apply", audit.Action);
    }

    private static ActivityClassificationRecomputeService NewService(PimDbContext db)
    {
        return new ActivityClassificationRecomputeService(
            db,
            new ActivityClassificationSnapshotService(db),
            new AuditLogService(db));
    }

    private static PimDbContext CreateDb()
    {
        PimDbContext.RegisterModuleAssembly(typeof(ActivityClassificationEntity).Assembly);
        var options = new DbContextOptionsBuilder<PimDbContext>()
            .UseInMemoryDatabase(Guid.NewGuid().ToString())
            .Options;
        return new PimDbContext(options);
    }

    private static SaveActivityClassificationRuleRequest NewRuleRequest(string name)
    {
        return new SaveActivityClassificationRuleRequest(
            name,
            "activity",
            "编程",
            "PIM",
            "#6B5EE4",
            800,
            """{"all":[{"field":"appNameNormalized","op":"equals","value":"code"}]}""",
            0.95,
            "Code rule");
    }

    private static ActivityClassificationApplyRangeRequest NewRange(string mode, string start, string end)
    {
        return new ActivityClassificationApplyRangeRequest(mode, start, end);
    }

    private static void AddWindow(PimDbContext db, string app, string start, double durationSeconds)
    {
        db.Set<AwEventEntity>().Add(new AwEventEntity
        {
            DeviceId = "device-1",
            EventType = "window",
            AppName = app,
            AppNameNormalized = AppNameNormalizer.Normalize(app),
            WindowTitle = "project",
            Timestamp = DateTimeOffset.Parse(start),
            Duration = durationSeconds,
            CreatedAt = DateTimeOffset.UtcNow
        });
    }
}
```

- [ ] **Step 2: 运行测试确认失败**

Run:

```powershell
dotnet test tests/Pim.UnitTests/Pim.UnitTests.csproj --filter ActivityClassificationRecomputeServiceTests
```

Expected: FAIL。

- [ ] **Step 3: 增加预览与应用 DTO**

在 `ActivityClassificationDtos.cs` 末尾增加：

```csharp
public record ActivityClassificationApplyRangeRequest(
    string Mode,
    string? DateFrom,
    string? DateTo);

public record ActivityClassificationPreviewRequest(
    SaveActivityClassificationRuleRequest Rule,
    ActivityClassificationApplyRangeRequest Range);

public record ActivityClassificationPreviewDto(
    int AffectedRecordCount,
    double AffectedDurationSeconds,
    IReadOnlyDictionary<string, int> CurrentCategoryCounts,
    IReadOnlyDictionary<string, int> NewCategoryCounts,
    IReadOnlyList<PcDetailRecord> Samples,
    bool RequiresConfirmation,
    string Summary);

public record ApplyActivityClassificationRuleRequest(
    SaveActivityClassificationRuleRequest Rule,
    ActivityClassificationApplyRangeRequest Range);
```

- [ ] **Step 4: 实现重算服务**

创建 `src/modules/Pim.Module.PcTracker/Services/ActivityClassificationRecomputeService.cs`：

```csharp
using Microsoft.EntityFrameworkCore;
using Pim.Core.Operations;
using Pim.Infrastructure.Data;
using Pim.Module.PcTracker.DTOs;
using Pim.Module.PcTracker.Entities;

namespace Pim.Module.PcTracker.Services;

public class ActivityClassificationRecomputeService
{
    private readonly PimDbContext _db;
    private readonly ActivityClassificationSnapshotService _snapshots;
    private readonly IAuditLogService _audit;

    public ActivityClassificationRecomputeService(
        PimDbContext db,
        ActivityClassificationSnapshotService snapshots,
        IAuditLogService audit)
    {
        _db = db;
        _snapshots = snapshots;
        _audit = audit;
    }

    public async Task<ActivityClassificationPreviewDto> PreviewRuleAsync(
        SaveActivityClassificationRuleRequest ruleRequest,
        ActivityClassificationApplyRangeRequest range,
        CancellationToken ct)
    {
        var records = await LoadWindowRecordsAsync(range, ct);
        var rule = ToRule(ruleRequest, source: "user", status: "active");
        var classified = records
            .Select(record => new { Record = record, Result = ActivityClassifier.Classify(ActivityClassificationSnapshotService.ToContext(record), new[] { rule }) })
            .Where(x => x.Result.SourceRuleId == rule.Id || x.Result.CategoryName == rule.CategoryName)
            .ToList();

        return new ActivityClassificationPreviewDto(
            classified.Count,
            classified.Sum(x => x.Record.DurationSeconds ?? 0),
            records.GroupBy(r => r.CategoryName ?? "其他").ToDictionary(g => g.Key, g => g.Count()),
            classified.GroupBy(x => x.Result.CategoryName).ToDictionary(g => g.Key, g => g.Count()),
            classified.Take(5).Select(x => x.Record).ToList(),
            classified.Count > 0,
            $"将影响 {classified.Count} 条记录，合计 {Math.Round(classified.Sum(x => x.Record.DurationSeconds ?? 0) / 60, 1)} 分钟。");
    }

    public async Task<ActivityClassificationPreviewDto> ApplyRuleAsync(
        SaveActivityClassificationRuleRequest ruleRequest,
        ActivityClassificationApplyRangeRequest range,
        CancellationToken ct)
    {
        var preview = await PreviewRuleAsync(ruleRequest, range, ct);
        var rule = ToRule(ruleRequest, source: "user", status: "active");
        _db.Set<ActivityCategoryRuleEntity>().Add(rule);
        await _db.SaveChangesAsync(ct);

        var audit = await _audit.RecordAsync(new CreateAuditLogRequest(
            null,
            AuditActorType.User,
            "pc.classification.rule.apply",
            "pc_activity_category_rules",
            rule.Id.ToString(),
            "web",
            AuditResult.Success,
            null,
            null,
            null,
            new Dictionary<string, string>
            {
                ["rangeMode"] = range.Mode,
                ["dateFrom"] = range.DateFrom ?? string.Empty,
                ["dateTo"] = range.DateTo ?? string.Empty,
                ["affectedRecordCount"] = preview.AffectedRecordCount.ToString()
            },
            null,
            null),
            ct);

        var records = await LoadWindowRecordsAsync(range, ct);
        await _snapshots.EnsureClassificationsAsync(records, await LoadActiveRulesAsync(ct), audit.Id, ct);
        return preview;
    }

    private async Task<List<ActivityCategoryRuleEntity>> LoadActiveRulesAsync(CancellationToken ct)
    {
        return await _db.Set<ActivityCategoryRuleEntity>()
            .Where(r => r.Status == "active")
            .OrderByDescending(r => r.Priority)
            .ToListAsync(ct);
    }

    private async Task<List<PcDetailRecord>> LoadWindowRecordsAsync(ActivityClassificationApplyRangeRequest range, CancellationToken ct)
    {
        var (start, end) = ParseRange(range);
        var events = await _db.Set<AwEventEntity>()
            .Where(e => e.Timestamp >= start && e.Timestamp < end && e.EventType == "window")
            .OrderBy(e => e.Timestamp)
            .ToListAsync(ct);

        return events
            .Where(e => e.Duration > 0)
            .Select(e => new PcDetailRecord(
                "window",
                e.Timestamp.ToString("O"),
                e.Timestamp.AddSeconds(e.Duration).ToString("O"),
                e.Duration,
                e.DeviceId,
                e.AppName,
                e.AppName,
                "其他",
                e.WindowTitle,
                null,
                null,
                null,
                null,
                null,
                null))
            .ToList();
    }

    private static (DateTimeOffset Start, DateTimeOffset End) ParseRange(ActivityClassificationApplyRangeRequest range)
    {
        var start = DateTimeOffset.Parse(range.DateFrom ?? DateTimeOffset.UtcNow.ToString("yyyy-MM-dd"));
        var end = DateTimeOffset.Parse(range.DateTo ?? range.DateFrom ?? DateTimeOffset.UtcNow.ToString("yyyy-MM-dd")).AddDays(1);
        return (start, end);
    }

    private static ActivityCategoryRuleEntity ToRule(
        SaveActivityClassificationRuleRequest req,
        string source,
        string status)
    {
        return new ActivityCategoryRuleEntity
        {
            Id = Guid.NewGuid(),
            RuleName = req.RuleName,
            Scope = req.Scope,
            CategoryName = req.CategoryName,
            ProjectTag = req.ProjectTag,
            Color = req.Color,
            Priority = req.Priority,
            Source = source,
            Status = status,
            ConditionsJson = req.ConditionsJson,
            Confidence = req.Confidence,
            Explanation = req.Explanation,
            CreatedAt = DateTimeOffset.UtcNow,
            UpdatedAt = DateTimeOffset.UtcNow
        };
    }
}
```

- [ ] **Step 5: 注册服务与 API**

在 `PcTrackerModule.RegisterServices` 加入：

```csharp
services.AddScoped<ActivityClassificationRecomputeService>();
```

在 `PcTrackerModule.MapEndpoints` 中加入：

```csharp
writeGroup.MapPost("/classification/rules/preview", async (
    [FromBody] ActivityClassificationPreviewRequest req,
    [FromServices] ActivityClassificationRecomputeService recomputeService,
    CancellationToken ct) =>
{
    var preview = await recomputeService.PreviewRuleAsync(req.Rule, req.Range, ct);
    return Results.Ok(ApiResponse<ActivityClassificationPreviewDto>.Ok(preview));
});

writeGroup.MapPost("/classification/rules/apply", async (
    [FromBody] ApplyActivityClassificationRuleRequest req,
    [FromServices] ActivityClassificationRecomputeService recomputeService,
    CancellationToken ct) =>
{
    var result = await recomputeService.ApplyRuleAsync(req.Rule, req.Range, ct);
    return Results.Ok(ApiResponse<ActivityClassificationPreviewDto>.Ok(result));
});
```

保留现有 `POST /classification/rules` 作为直接创建低风险规则的兼容入口，前端纠错流程使用 `/classification/rules/preview` 和 `/classification/rules/apply`。

- [ ] **Step 6: 运行测试**

Run:

```powershell
dotnet test tests/Pim.UnitTests/Pim.UnitTests.csproj --filter ActivityClassificationRecomputeServiceTests
```

Expected: PASS。

- [ ] **Step 7: 提交**

```powershell
git add src/modules/Pim.Module.PcTracker/DTOs/ActivityClassificationDtos.cs src/modules/Pim.Module.PcTracker/Services/ActivityClassificationRecomputeService.cs src/modules/Pim.Module.PcTracker/PcTrackerModule.cs tests/Pim.UnitTests/Services/ActivityClassificationRecomputeServiceTests.cs
git commit -m "feat(pc): preview and apply classification rules"
```

---

### Task 6: 建议聚类、最近项目标签与接受建议走预览流程

**Files:**
- Modify: `src/modules/Pim.Module.PcTracker/Services/ActivitySuggestionService.cs`
- Modify: `src/modules/Pim.Module.PcTracker/DTOs/ActivityClassificationDtos.cs`
- Modify: `src/modules/Pim.Module.PcTracker/PcTrackerModule.cs`
- Test: `tests/Pim.UnitTests/Services/ActivitySuggestionServiceTests.cs`

- [ ] **Step 1: 增加失败测试：短碎片尊重阈值**

在 `ActivitySuggestionServiceTests` 增加：

```csharp
[Fact]
public async Task BuildSuggestionsAsync_IgnoresTinyFallbackRecordsBelowRecommendedDuration()
{
    using var db = CreateDbContext();
    var service = new ActivitySuggestionService(db);
    var records = new[]
    {
        NewWebRecord(30, "https://unknown.example.com/path", "fallback"),
        NewWebRecord(20, "https://unknown.example.com/other", "fallback")
    };

    var suggestions = await service.BuildSuggestionsAsync(records, recommendedMinimumMinutes: 5, CancellationToken.None);

    Assert.Empty(suggestions);
}
```

- [ ] **Step 2: 增加失败测试：最近项目标签**

在同一文件增加：

```csharp
[Fact]
public async Task GetRecentProjectTagsAsync_ReturnsTagsFromRulesAndSnapshots()
{
    using var db = CreateDbContext();
    db.Set<ActivityCategoryRuleEntity>().Add(new ActivityCategoryRuleEntity
    {
        Id = Guid.NewGuid(),
        RuleName = "PIM docs",
        Scope = "both",
        CategoryName = "学习",
        ProjectTag = "PIM",
        Color = "#14b8a6",
        Priority = 300,
        Source = "user",
        Status = "active",
        ConditionsJson = """{"all":[{"field":"domain","op":"domainSuffix","value":"docs.example.com"}]}""",
        Confidence = 0.9
    });
    db.Set<ActivityClassificationEntity>().Add(new ActivityClassificationEntity
    {
        Id = Guid.NewGuid(),
        RecordKey = "window:test",
        RecordType = "window",
        DeviceId = "device-1",
        SourceEventIdsJson = "[]",
        StartedAt = DateTimeOffset.UtcNow,
        EndedAt = DateTimeOffset.UtcNow.AddMinutes(5),
        CategoryName = "编程",
        CategoryColor = "#6B5EE4",
        ProjectTag = "projectGPT",
        Confidence = 0.9,
        Source = "rule",
        Explanation = "Existing snapshot"
    });
    await db.SaveChangesAsync();
    var service = new ActivitySuggestionService(db);

    var tags = await service.GetRecentProjectTagsAsync(CancellationToken.None);

    Assert.Contains("PIM", tags);
    Assert.Contains("projectGPT", tags);
}
```

- [ ] **Step 3: 运行测试确认失败**

Run:

```powershell
dotnet test tests/Pim.UnitTests/Pim.UnitTests.csproj --filter ActivitySuggestionServiceTests
```

Expected: FAIL。

- [ ] **Step 4: 修改建议服务方法签名和过滤**

把 `ActivitySuggestionService.BuildSuggestionsAsync` 签名改为：

```csharp
public async Task<List<ActivityClassificationSuggestionDto>> BuildSuggestionsAsync(
    IReadOnlyCollection<PcDetailRecord> records,
    int recommendedMinimumMinutes,
    CancellationToken ct)
```

把候选过滤改为：

```csharp
.Where(record => NeedsSuggestion(record, recommendedMinimumMinutes))
```

把 `NeedsSuggestion` 改为：

```csharp
private static bool NeedsSuggestion(PcDetailRecord record, int recommendedMinimumMinutes)
{
    if ((record.DurationSeconds ?? 0) < recommendedMinimumMinutes * 60)
        return false;

    return string.Equals(record.ClassificationSource, "fallback", StringComparison.OrdinalIgnoreCase)
        || (record.ClassificationConfidence is not null && record.ClassificationConfidence < 0.5);
}
```

更新已有测试调用，传入 `recommendedMinimumMinutes: 1`。

- [ ] **Step 5: 增加最近项目标签服务方法**

在 `ActivitySuggestionService` 中加入：

```csharp
public async Task<List<string>> GetRecentProjectTagsAsync(CancellationToken ct)
{
    var ruleTags = await _db.Set<ActivityCategoryRuleEntity>()
        .Where(r => r.ProjectTag != null && r.ProjectTag != "")
        .OrderByDescending(r => r.UpdatedAt)
        .Select(r => r.ProjectTag!)
        .Take(20)
        .ToListAsync(ct);

    var snapshotTags = await _db.Set<ActivityClassificationEntity>()
        .Where(c => c.ProjectTag != null && c.ProjectTag != "")
        .OrderByDescending(c => c.ClassifiedAt)
        .Select(c => c.ProjectTag!)
        .Take(20)
        .ToListAsync(ct);

    return ruleTags
        .Concat(snapshotTags)
        .Distinct(StringComparer.OrdinalIgnoreCase)
        .Take(20)
        .ToList();
}
```

- [ ] **Step 6: 修改 suggestions endpoint 使用设置**

在 `PcTrackerModule.MapEndpoints` 的 `/classification/suggestions` endpoint 增加 `ActivityClassificationSettingsService` 参数：

```csharp
[FromServices] ActivityClassificationSettingsService settingsService,
```

调用改为：

```csharp
var settings = await settingsService.GetSettingsAsync(ct);
var suggestions = await suggestionService.BuildSuggestionsAsync(
    records,
    settings.RecommendedMinimumClassificationDurationMinutes,
    ct);
```

新增 endpoint：

```csharp
readGroup.MapGet("/classification/project-tags/recent", async (
    [FromServices] ActivitySuggestionService suggestionService,
    CancellationToken ct) =>
{
    var tags = await suggestionService.GetRecentProjectTagsAsync(ct);
    return Results.Ok(ApiResponse<List<string>>.Ok(tags));
});
```

- [ ] **Step 7: 运行测试**

Run:

```powershell
dotnet test tests/Pim.UnitTests/Pim.UnitTests.csproj --filter ActivitySuggestionServiceTests
```

Expected: PASS。

- [ ] **Step 8: 提交**

```powershell
git add src/modules/Pim.Module.PcTracker/Services/ActivitySuggestionService.cs src/modules/Pim.Module.PcTracker/PcTrackerModule.cs tests/Pim.UnitTests/Services/ActivitySuggestionServiceTests.cs
git commit -m "feat(pc): improve classification suggestions"
```

---

### Task 7: 前端 API 类型与路径测试

**Files:**
- Modify: `src/client-web/src/types/index.ts`
- Modify: `src/client-web/src/api/pcTracker.ts`
- Test: `tests/client-web/pcClassificationApiPath.test.ts`
- Test: `tests/client-web/pcClassificationTypes.test.ts`

- [ ] **Step 1: 写 API 路径测试**

创建 `tests/client-web/pcClassificationApiPath.test.ts`：

```ts
import { describe, expect, it } from 'vitest';

const paths = {
  rules: '/pc/classification/rules',
  rulePreview: '/pc/classification/rules/preview',
  ruleApply: '/pc/classification/rules/apply',
  suggestions: '/pc/classification/suggestions?date=2026-05-25',
  settings: '/pc/classification/settings',
  recentTags: '/pc/classification/project-tags/recent',
};

describe('pc classification API paths', () => {
  it('keeps classification paths under /pc/classification', () => {
    expect(paths.rules).toBe('/pc/classification/rules');
    expect(paths.rulePreview).toBe('/pc/classification/rules/preview');
    expect(paths.ruleApply).toBe('/pc/classification/rules/apply');
    expect(paths.suggestions).toBe('/pc/classification/suggestions?date=2026-05-25');
    expect(paths.settings).toBe('/pc/classification/settings');
    expect(paths.recentTags).toBe('/pc/classification/project-tags/recent');
  });
});
```

- [ ] **Step 2: 写类型测试**

创建 `tests/client-web/pcClassificationTypes.test.ts`：

```ts
import { describe, expect, it } from 'vitest';
import type {
  ActivityClassificationPreview,
  ActivityClassificationSettings,
} from '../../src/client-web/src/types';

describe('pc classification types', () => {
  it('supports preview and settings shapes', () => {
    const preview: ActivityClassificationPreview = {
      affectedRecordCount: 2,
      affectedDurationSeconds: 300,
      currentCategoryCounts: { '其他': 2 },
      newCategoryCounts: { '学习': 2 },
      samples: [],
      requiresConfirmation: true,
      summary: '将影响 2 条记录',
    };
    const settings: ActivityClassificationSettings = {
      recommendedMinimumClassificationDurationMinutes: 5,
      supportedRecommendedMinimumDurations: [1, 3, 5, 10, 15],
    };

    expect(preview.requiresConfirmation).toBe(true);
    expect(settings.supportedRecommendedMinimumDurations).toContain(5);
  });
});
```

- [ ] **Step 3: 运行测试确认失败**

Run:

```powershell
npm --prefix src/client-web exec vitest run ../../tests/client-web/pcClassificationApiPath.test.ts ../../tests/client-web/pcClassificationTypes.test.ts
```

Expected: FAIL，因为类型不存在。

- [ ] **Step 4: 增加前端类型**

在 `src/client-web/src/types/index.ts` 增加：

```ts
export interface ActivityClassificationApplyRange {
  mode: 'today' | 'range' | 'all';
  dateFrom?: string | null;
  dateTo?: string | null;
}

export interface SaveActivityClassificationRuleRequest {
  ruleName: string;
  scope: string;
  categoryName: string | null;
  projectTag: string | null;
  color: string;
  priority: number;
  conditionsJson: string;
  confidence: number;
  explanation: string | null;
}

export interface ActivityClassificationPreview {
  affectedRecordCount: number;
  affectedDurationSeconds: number;
  currentCategoryCounts: Record<string, number>;
  newCategoryCounts: Record<string, number>;
  samples: PcDetailRecord[];
  requiresConfirmation: boolean;
  summary: string;
}

export interface ActivityClassificationSettings {
  recommendedMinimumClassificationDurationMinutes: number;
  supportedRecommendedMinimumDurations: number[];
}
```

- [ ] **Step 5: 增加前端 API 函数**

在 `src/client-web/src/api/pcTracker.ts` 的 helper import 中加入 `apiPut`：

```ts
import { apiGet, apiPost, apiPut, apiDelete } from './client';
```

在类型 import 中加入：

```ts
ActivityClassificationApplyRange,
ActivityClassificationPreview,
ActivityClassificationSettings,
SaveActivityClassificationRuleRequest,
```

在文件末尾增加：

```ts
export function previewActivityClassificationRule(
  rule: SaveActivityClassificationRuleRequest,
  range: ActivityClassificationApplyRange
) {
  return apiPost<ApiResponse<ActivityClassificationPreview>>(
    '/pc/classification/rules/preview',
    { rule, range }
  ).then(r => r.data);
}

export function applyActivityClassificationRule(
  rule: SaveActivityClassificationRuleRequest,
  range: ActivityClassificationApplyRange
) {
  return apiPost<ApiResponse<ActivityClassificationPreview>>(
    '/pc/classification/rules/apply',
    { rule, range }
  ).then(r => r.data);
}

export function getActivityClassificationSettings() {
  return apiGet<ApiResponse<ActivityClassificationSettings>>('/pc/classification/settings')
    .then(r => r.data);
}

export function saveActivityClassificationSettings(minutes: number) {
  return apiPut<ApiResponse<ActivityClassificationSettings>>(
    '/pc/classification/settings',
    { recommendedMinimumClassificationDurationMinutes: minutes }
  ).then(r => r.data);
}

export function getRecentActivityProjectTags() {
  return apiGet<ApiResponse<string[]>>('/pc/classification/project-tags/recent')
    .then(r => r.data);
}
```

后端设置保存 endpoint 使用 `PUT /classification/settings`，前端使用现有 `apiPut` helper。

- [ ] **Step 6: 运行前端类型测试**

Run:

```powershell
npm --prefix src/client-web exec vitest run ../../tests/client-web/pcClassificationApiPath.test.ts ../../tests/client-web/pcClassificationTypes.test.ts
```

Expected: PASS。

- [ ] **Step 7: 提交**

```powershell
git add src/client-web/src/types/index.ts src/client-web/src/api/pcTracker.ts tests/client-web/pcClassificationApiPath.test.ts tests/client-web/pcClassificationTypes.test.ts
git commit -m "feat(web): add pc classification api client"
```

---

### Task 8: PC 记录页建议面板与快捷纠错

**Files:**
- Create: `src/client-web/src/components/pc-tracker/ClassificationSuggestionPanel.tsx`
- Create: `src/client-web/src/components/pc-tracker/QuickClassificationDialog.tsx`
- Modify: `src/client-web/src/pages/PcTrackerPage.tsx`

- [ ] **Step 1: 新增建议面板组件**

创建 `src/client-web/src/components/pc-tracker/ClassificationSuggestionPanel.tsx`：

```tsx
import type { ActivityClassificationSuggestion } from '../../types';

interface Props {
  suggestions: ActivityClassificationSuggestion[];
  isLoading: boolean;
  onCorrect: (suggestion: ActivityClassificationSuggestion) => void;
  onReject: (suggestion: ActivityClassificationSuggestion) => void;
}

function formatMinutes(seconds: number) {
  return `${Math.round(seconds / 60)} 分钟`;
}

export default function ClassificationSuggestionPanel({
  suggestions,
  isLoading,
  onCorrect,
  onReject,
}: Props) {
  if (isLoading) {
    return <div className="rounded-lg border border-slate-200 bg-slate-50 p-4 text-sm text-slate-500">正在整理待处理建议...</div>;
  }

  if (!suggestions.length) {
    return <div className="rounded-lg border border-slate-200 bg-slate-50 p-4 text-sm text-slate-500">暂无待处理分类建议</div>;
  }

  return (
    <div className="space-y-2">
      {suggestions.slice(0, 5).map(suggestion => (
        <div key={suggestion.id} className="rounded-lg border border-slate-200 bg-white p-3">
          <div className="flex items-start justify-between gap-3">
            <div className="min-w-0">
              <p className="truncate text-sm font-medium text-slate-900">{suggestion.clusterKey}</p>
              <p className="mt-1 text-xs text-slate-500">
                {suggestion.sampleCount} 条 · {formatMinutes(suggestion.totalDurationSeconds)}
              </p>
            </div>
            <div className="flex shrink-0 gap-2">
              <button
                className="rounded-md border border-blue-200 bg-blue-50 px-2 py-1 text-xs font-medium text-blue-700 hover:bg-blue-100"
                onClick={() => onCorrect(suggestion)}
              >
                纠错
              </button>
              <button
                className="rounded-md border border-slate-200 px-2 py-1 text-xs text-slate-500 hover:bg-slate-50"
                onClick={() => onReject(suggestion)}
              >
                忽略
              </button>
            </div>
          </div>
        </div>
      ))}
    </div>
  );
}
```

- [ ] **Step 2: 新增快捷纠错对话框**

创建 `src/client-web/src/components/pc-tracker/QuickClassificationDialog.tsx`：

```tsx
import { useMemo, useState } from 'react';
import type {
  ActivityClassificationApplyRange,
  ActivityClassificationPreview,
  ActivityClassificationSuggestion,
  SaveActivityClassificationRuleRequest,
} from '../../types';

const categories = [
  { name: '编程', color: '#6B5EE4' },
  { name: '学习', color: '#14b8a6' },
  { name: '沟通', color: '#F5935A' },
  { name: '办公', color: '#F59E0B' },
  { name: '娱乐', color: '#EC4899' },
  { name: '终端', color: '#E05A7A' },
  { name: '文件', color: '#3B82F6' },
  { name: '其他', color: '#64748b' },
];

interface Props {
  suggestion: ActivityClassificationSuggestion | null;
  date: string;
  recentProjectTags: string[];
  preview: ActivityClassificationPreview | null;
  isPreviewing: boolean;
  isApplying: boolean;
  onClose: () => void;
  onPreview: (rule: SaveActivityClassificationRuleRequest, range: ActivityClassificationApplyRange) => void;
  onApply: (rule: SaveActivityClassificationRuleRequest, range: ActivityClassificationApplyRange) => void;
}

export default function QuickClassificationDialog({
  suggestion,
  date,
  recentProjectTags,
  preview,
  isPreviewing,
  isApplying,
  onClose,
  onPreview,
  onApply,
}: Props) {
  const [categoryName, setCategoryName] = useState('学习');
  const [projectTag, setProjectTag] = useState('');
  const [rangeMode, setRangeMode] = useState<'today' | 'range' | 'all'>('today');
  const [dateFrom, setDateFrom] = useState(date);
  const [dateTo, setDateTo] = useState(date);
  const selectedCategory = categories.find(c => c.name === categoryName) ?? categories[0];

  const rule = useMemo<SaveActivityClassificationRuleRequest>(() => ({
    ruleName: suggestion ? `用户规则：${suggestion.clusterKey}` : '用户规则',
    scope: 'both',
    categoryName,
    projectTag: projectTag.trim() || null,
    color: selectedCategory.color,
    priority: 900,
    conditionsJson: suggestion?.clusterKey.startsWith('web:')
      ? JSON.stringify({ all: [{ field: 'domain', op: 'domainSuffix', value: suggestion.clusterKey.slice(4) }] })
      : JSON.stringify({ all: [{ field: 'appNameNormalized', op: 'equals', value: suggestion?.clusterKey.replace('app:', '') ?? '' }] }),
    confidence: 0.95,
    explanation: '用户从 PC 记录页纠错创建的规则。',
  }), [categoryName, projectTag, selectedCategory.color, suggestion]);

  const range: ActivityClassificationApplyRange = {
    mode: rangeMode,
    dateFrom: rangeMode === 'all' ? null : dateFrom,
    dateTo: rangeMode === 'all' ? null : dateTo,
  };

  if (!suggestion) return null;

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-slate-950/30 p-4">
      <div className="w-full max-w-2xl rounded-lg bg-white p-5 shadow-xl">
        <div className="flex items-start justify-between gap-4">
          <div>
            <h2 className="text-base font-semibold text-slate-950">分类纠错</h2>
            <p className="mt-1 text-sm text-slate-500">{suggestion.clusterKey}</p>
          </div>
          <button className="rounded-md px-2 py-1 text-sm text-slate-500 hover:bg-slate-100" onClick={onClose}>关闭</button>
        </div>

        <div className="mt-4 grid gap-3 md:grid-cols-2">
          <label className="text-sm text-slate-600">
            分类
            <select className="mt-1 w-full rounded-lg border border-slate-200 px-3 py-2" value={categoryName} onChange={e => setCategoryName(e.target.value)}>
              {categories.map(category => <option key={category.name} value={category.name}>{category.name}</option>)}
            </select>
          </label>
          <label className="text-sm text-slate-600">
            项目标签
            <input className="mt-1 w-full rounded-lg border border-slate-200 px-3 py-2" value={projectTag} onChange={e => setProjectTag(e.target.value)} list="recent-project-tags" />
            <datalist id="recent-project-tags">
              {recentProjectTags.map(tag => <option key={tag} value={tag} />)}
            </datalist>
          </label>
          <label className="text-sm text-slate-600">
            应用范围
            <select className="mt-1 w-full rounded-lg border border-slate-200 px-3 py-2" value={rangeMode} onChange={e => setRangeMode(e.target.value as 'today' | 'range' | 'all')}>
              <option value="today">仅今天</option>
              <option value="range">日期范围</option>
              <option value="all">全部历史</option>
            </select>
          </label>
          {rangeMode !== 'all' && (
            <div className="grid grid-cols-2 gap-2">
              <input type="date" className="rounded-lg border border-slate-200 px-3 py-2 text-sm" value={dateFrom} onChange={e => setDateFrom(e.target.value)} />
              <input type="date" className="rounded-lg border border-slate-200 px-3 py-2 text-sm" value={dateTo} onChange={e => setDateTo(e.target.value)} />
            </div>
          )}
        </div>

        {preview && (
          <div className="mt-4 rounded-lg border border-blue-200 bg-blue-50 p-3 text-sm text-blue-900">
            <p className="font-medium">{preview.summary}</p>
            <p className="mt-1">影响 {preview.affectedRecordCount} 条，合计 {Math.round(preview.affectedDurationSeconds / 60)} 分钟。</p>
          </div>
        )}

        <div className="mt-5 flex justify-end gap-2">
          <button className="rounded-lg border border-slate-200 px-3 py-2 text-sm text-slate-600 hover:bg-slate-50" onClick={() => onPreview(rule, range)} disabled={isPreviewing}>
            {isPreviewing ? '预览中...' : '预览影响'}
          </button>
          <button className="rounded-lg bg-blue-600 px-3 py-2 text-sm font-medium text-white hover:bg-blue-700 disabled:opacity-50" onClick={() => onApply(rule, range)} disabled={!preview || isApplying}>
            {isApplying ? '应用中...' : '确认应用'}
          </button>
        </div>
      </div>
    </div>
  );
}
```

- [ ] **Step 3: 接入 PC 记录页**

在 `src/client-web/src/pages/PcTrackerPage.tsx` 的 import 中加入：

```tsx
import {
  applyActivityClassificationRule,
  getActivityClassificationSuggestions,
  getRecentActivityProjectTags,
  previewActivityClassificationRule,
  rejectActivityClassificationSuggestion,
} from '../api/pcTracker';
import type {
  ActivityClassificationApplyRange,
  ActivityClassificationPreview,
  ActivityClassificationSuggestion,
  SaveActivityClassificationRuleRequest,
} from '../types';
import ClassificationSuggestionPanel from '../components/pc-tracker/ClassificationSuggestionPanel';
import QuickClassificationDialog from '../components/pc-tracker/QuickClassificationDialog';
```

在组件内加入状态和 query/mutation：

```tsx
const queryClient = useQueryClient();
const [activeSuggestion, setActiveSuggestion] = useState<ActivityClassificationSuggestion | null>(null);
const [preview, setPreview] = useState<ActivityClassificationPreview | null>(null);

const { data: suggestions = [], isLoading: suggestionsLoading } = useQuery({
  queryKey: ['pc-classification-suggestions', dateStr],
  queryFn: () => getActivityClassificationSuggestions(dateStr),
});

const { data: recentProjectTags = [] } = useQuery({
  queryKey: ['pc-classification-project-tags'],
  queryFn: () => getRecentActivityProjectTags(),
});

const previewMut = useMutation({
  mutationFn: ({ rule, range }: { rule: SaveActivityClassificationRuleRequest; range: ActivityClassificationApplyRange }) =>
    previewActivityClassificationRule(rule, range),
  onSuccess: setPreview,
});

const applyMut = useMutation({
  mutationFn: ({ rule, range }: { rule: SaveActivityClassificationRuleRequest; range: ActivityClassificationApplyRange }) =>
    applyActivityClassificationRule(rule, range),
  onSuccess: () => {
    setActiveSuggestion(null);
    setPreview(null);
    queryClient.invalidateQueries({ queryKey: ['pc-summary', dateStr] });
    queryClient.invalidateQueries({ queryKey: ['pc-classification-suggestions', dateStr] });
  },
});
```

如果文件当前未引入 `useMutation` 和 `useQueryClient`，把 React Query import 改为：

```tsx
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
```

在页面 `PcQualitySummary` 下方插入：

```tsx
<AnalysisCard title="待处理分类建议" subtitle="把低置信或未知活动整理成少量可纠错条目">
  <ClassificationSuggestionPanel
    suggestions={suggestions}
    isLoading={suggestionsLoading}
    onCorrect={(suggestion) => {
      setPreview(null);
      setActiveSuggestion(suggestion);
    }}
    onReject={(suggestion) => rejectActivityClassificationSuggestion(suggestion.id).then(() => {
      queryClient.invalidateQueries({ queryKey: ['pc-classification-suggestions', dateStr] });
    })}
  />
</AnalysisCard>
```

在返回 JSX 末尾加入：

```tsx
<QuickClassificationDialog
  suggestion={activeSuggestion}
  date={dateStr}
  recentProjectTags={recentProjectTags}
  preview={preview}
  isPreviewing={previewMut.isPending}
  isApplying={applyMut.isPending}
  onClose={() => {
    setActiveSuggestion(null);
    setPreview(null);
  }}
  onPreview={(rule, range) => previewMut.mutate({ rule, range })}
  onApply={(rule, range) => applyMut.mutate({ rule, range })}
/>
```

- [ ] **Step 4: 补前端 API reject 函数**

在 `src/client-web/src/api/pcTracker.ts` 增加：

```ts
export function rejectActivityClassificationSuggestion(id: string) {
  return apiPost<ApiResponse<string>>(`/pc/classification/suggestions/${id}/reject`, {})
    .then(r => r.data);
}
```

- [ ] **Step 5: 构建前端**

Run:

```powershell
npm --prefix src/client-web run build
```

Expected: PASS。

- [ ] **Step 6: 提交**

```powershell
git add src/client-web/src/components/pc-tracker/ClassificationSuggestionPanel.tsx src/client-web/src/components/pc-tracker/QuickClassificationDialog.tsx src/client-web/src/pages/PcTrackerPage.tsx src/client-web/src/api/pcTracker.ts
git commit -m "feat(web): add pc classification correction flow"
```

---

### Task 9: 分类管理页

**Files:**
- Create: `src/client-web/src/pages/PcClassificationPage.tsx`
- Create: `src/client-web/src/components/pc-classification/ClassificationRuleTable.tsx`
- Create: `src/client-web/src/components/pc-classification/ClassificationRuleEditor.tsx`
- Create: `src/client-web/src/components/pc-classification/ClassificationRecomputePanel.tsx`
- Modify: `src/client-web/src/layout/AppLayout.tsx`
- Modify: `src/client-web/src/layout/Sidebar.tsx`
- Modify: `src/client-web/src/api/pcTracker.ts`

- [ ] **Step 1: 创建规则表组件**

创建 `src/client-web/src/components/pc-classification/ClassificationRuleTable.tsx`：

```tsx
import type { ActivityClassificationRule } from '../../types';

interface Props {
  rules: ActivityClassificationRule[];
  onEdit: (rule: ActivityClassificationRule) => void;
}

export default function ClassificationRuleTable({ rules, onEdit }: Props) {
  return (
    <div className="overflow-x-auto rounded-lg border border-slate-200 bg-white">
      <table className="w-full text-sm">
        <thead className="bg-slate-50 text-xs text-slate-500">
          <tr>
            <th className="px-3 py-2 text-left font-medium">规则</th>
            <th className="px-3 py-2 text-left font-medium">分类</th>
            <th className="px-3 py-2 text-left font-medium">项目</th>
            <th className="px-3 py-2 text-left font-medium">来源</th>
            <th className="px-3 py-2 text-right font-medium">优先级</th>
            <th className="px-3 py-2 text-right font-medium">操作</th>
          </tr>
        </thead>
        <tbody>
          {rules.map(rule => (
            <tr key={rule.id} className="border-t border-slate-100">
              <td className="px-3 py-2">
                <div className="font-medium text-slate-900">{rule.ruleName}</div>
                <div className="max-w-md truncate text-xs text-slate-500">{rule.explanation || rule.conditionsJson}</div>
              </td>
              <td className="px-3 py-2 text-slate-700">{rule.categoryName || '-'}</td>
              <td className="px-3 py-2 text-slate-700">{rule.projectTag || '-'}</td>
              <td className="px-3 py-2 text-slate-500">{rule.source}</td>
              <td className="px-3 py-2 text-right tabular-nums text-slate-700">{rule.priority}</td>
              <td className="px-3 py-2 text-right">
                <button className="rounded-md border border-slate-200 px-2 py-1 text-xs text-slate-600 hover:bg-slate-50" onClick={() => onEdit(rule)}>
                  查看
                </button>
              </td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
}
```

- [ ] **Step 2: 创建规则编辑器组件**

创建 `src/client-web/src/components/pc-classification/ClassificationRuleEditor.tsx`：

```tsx
import type { ActivityClassificationRule } from '../../types';

interface Props {
  rule: ActivityClassificationRule | null;
  onClose: () => void;
}

export default function ClassificationRuleEditor({ rule, onClose }: Props) {
  if (!rule) return null;

  return (
    <div className="rounded-lg border border-slate-200 bg-white p-4">
      <div className="flex items-start justify-between gap-3">
        <div>
          <h2 className="text-sm font-semibold text-slate-950">{rule.ruleName}</h2>
          <p className="mt-1 text-xs text-slate-500">来源 {rule.source} · 状态 {rule.status}</p>
        </div>
        <button className="rounded-md px-2 py-1 text-xs text-slate-500 hover:bg-slate-100" onClick={onClose}>关闭</button>
      </div>
      <dl className="mt-4 grid gap-3 text-sm md:grid-cols-2">
        <div>
          <dt className="text-xs text-slate-500">分类</dt>
          <dd className="mt-1 text-slate-900">{rule.categoryName || '-'}</dd>
        </div>
        <div>
          <dt className="text-xs text-slate-500">项目标签</dt>
          <dd className="mt-1 text-slate-900">{rule.projectTag || '-'}</dd>
        </div>
        <div className="md:col-span-2">
          <dt className="text-xs text-slate-500">条件</dt>
          <dd className="mt-1 rounded-md bg-slate-50 p-2 font-mono text-xs text-slate-700">{rule.conditionsJson}</dd>
        </div>
      </dl>
    </div>
  );
}
```

- [ ] **Step 3: 创建重算面板**

创建 `src/client-web/src/components/pc-classification/ClassificationRecomputePanel.tsx`：

```tsx
import type { ActivityClassificationSettings } from '../../types';

interface Props {
  settings: ActivityClassificationSettings | undefined;
  selectedMinutes: number;
  onSelectedMinutesChange: (minutes: number) => void;
  onSaveSettings: () => void;
  isSaving: boolean;
}

export default function ClassificationRecomputePanel({
  settings,
  selectedMinutes,
  onSelectedMinutesChange,
  onSaveSettings,
  isSaving,
}: Props) {
  const presets = settings?.supportedRecommendedMinimumDurations ?? [1, 3, 5, 10, 15];

  return (
    <section className="rounded-lg border border-slate-200 bg-white p-4">
      <h2 className="text-sm font-semibold text-slate-950">显示与建议粒度</h2>
      <p className="mt-1 text-xs text-slate-500">推荐最短分类时长只影响展示平滑和建议聚类，不会删除原始记录。</p>
      <div className="mt-3 flex flex-wrap gap-2">
        {presets.map(minutes => (
          <button
            key={minutes}
            className={`rounded-md border px-3 py-1 text-sm ${selectedMinutes === minutes ? 'border-blue-600 bg-blue-50 text-blue-700' : 'border-slate-200 text-slate-600 hover:bg-slate-50'}`}
            onClick={() => onSelectedMinutesChange(minutes)}
          >
            {minutes} 分钟
          </button>
        ))}
      </div>
      <button
        className="mt-4 rounded-lg bg-blue-600 px-3 py-2 text-sm font-medium text-white hover:bg-blue-700 disabled:opacity-50"
        onClick={onSaveSettings}
        disabled={isSaving}
      >
        {isSaving ? '保存中...' : '保存设置'}
      </button>
    </section>
  );
}
```

- [ ] **Step 4: 创建分类管理页**

创建 `src/client-web/src/pages/PcClassificationPage.tsx`：

```tsx
import { useEffect, useState } from 'react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import {
  getActivityClassificationRules,
  getActivityClassificationSettings,
  saveActivityClassificationSettings,
} from '../api/pcTracker';
import type { ActivityClassificationRule } from '../types';
import PageHeader from '../ui/PageHeader';
import ClassificationRuleEditor from '../components/pc-classification/ClassificationRuleEditor';
import ClassificationRuleTable from '../components/pc-classification/ClassificationRuleTable';
import ClassificationRecomputePanel from '../components/pc-classification/ClassificationRecomputePanel';

export default function PcClassificationPage() {
  const queryClient = useQueryClient();
  const [selectedRule, setSelectedRule] = useState<ActivityClassificationRule | null>(null);
  const [selectedMinutes, setSelectedMinutes] = useState(5);
  const { data: rules = [] } = useQuery({
    queryKey: ['pc-classification-rules'],
    queryFn: getActivityClassificationRules,
  });
  const { data: settings } = useQuery({
    queryKey: ['pc-classification-settings'],
    queryFn: getActivityClassificationSettings,
  });
  const saveSettingsMut = useMutation({
    mutationFn: saveActivityClassificationSettings,
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ['pc-classification-settings'] }),
  });

  useEffect(() => {
    if (settings) setSelectedMinutes(settings.recommendedMinimumClassificationDurationMinutes);
  }, [settings]);

  return (
    <div className="mx-auto w-full max-w-[1500px] space-y-4 pb-8">
      <PageHeader title="分类管理" subtitle="管理 PC 活动分类规则、显示粒度和历史重算入口" />
      <ClassificationRecomputePanel
        settings={settings}
        selectedMinutes={selectedMinutes}
        onSelectedMinutesChange={setSelectedMinutes}
        onSaveSettings={() => saveSettingsMut.mutate(selectedMinutes)}
        isSaving={saveSettingsMut.isPending}
      />
      <ClassificationRuleTable rules={rules} onEdit={setSelectedRule} />
      <ClassificationRuleEditor rule={selectedRule} onClose={() => setSelectedRule(null)} />
    </div>
  );
}
```

- [ ] **Step 5: 接入路由与导航**

在 `src/client-web/src/layout/AppLayout.tsx` 加入 import：

```tsx
import PcClassificationPage from '../pages/PcClassificationPage';
```

在 routes 中加入：

```tsx
<Route path="/pc-classification" element={<PcClassificationPage />} />
```

在 `src/client-web/src/layout/Sidebar.tsx` 的 `navItems` 中，放在 PC 记录后：

```tsx
{ label: '分类管理', path: '/pc-classification', short: '分' },
```

- [ ] **Step 6: 构建前端**

Run:

```powershell
npm --prefix src/client-web run build
```

Expected: PASS。

- [ ] **Step 7: 提交**

```powershell
git add src/client-web/src/pages/PcClassificationPage.tsx src/client-web/src/components/pc-classification/ClassificationRuleTable.tsx src/client-web/src/components/pc-classification/ClassificationRuleEditor.tsx src/client-web/src/components/pc-classification/ClassificationRecomputePanel.tsx src/client-web/src/layout/AppLayout.tsx src/client-web/src/layout/Sidebar.tsx
git commit -m "feat(web): add pc classification management page"
```

---

### Task 10: 最终验证与验收记录

**Files:**
- Create: `docs/operations/pc-activity-understanding-stage2-acceptance.md`

- [ ] **Step 1: 写验收文档**

创建 `docs/operations/pc-activity-understanding-stage2-acceptance.md`：

```markdown
# PC Activity Understanding Stage 2 Acceptance

## Scope

This checklist verifies the local, non-LLM PC activity understanding loop.

## Manual Checks

- Open the PC records page for a day with code, docs, terminal, file manager, and communication activity.
- Confirm the timeline shows category, project tag, confidence, source, and explanation.
- Confirm unknown or low-confidence activity appears as reviewable suggestion cards.
- Accept one suggestion as a user rule.
- Confirm the preview shows affected record count, duration, current category distribution, and new category distribution.
- Apply the rule to today only.
- Confirm only that date range changes.
- Open classification management.
- Confirm rules are visible.
- Change recommended minimum classification duration to 1, 5, and 10 minutes.
- Confirm 1 minute shows more detail and 10 minutes smooths incidental fragments.
- Confirm short strong communication activity is not hidden by smoothing.
- Confirm audit logs exist for the applied rule.

## Verification Commands

```powershell
dotnet test tests/Pim.UnitTests/Pim.UnitTests.csproj
npm --prefix src/client-web run build
```
```

- [ ] **Step 2: 后端全量单元测试**

Run:

```powershell
dotnet test tests/Pim.UnitTests/Pim.UnitTests.csproj
```

Expected: PASS。

- [ ] **Step 3: 前端构建**

Run:

```powershell
npm --prefix src/client-web run build
```

Expected: PASS。

- [ ] **Step 4: 查看最终状态**

Run:

```powershell
git status --short --branch
```

Expected: 只显示验收文档未提交，或工作区干净。

- [ ] **Step 5: 提交验收文档**

```powershell
git add docs/operations/pc-activity-understanding-stage2-acceptance.md
git commit -m "docs: add pc activity understanding acceptance"
```

---

## 自检

Spec 覆盖：

- 持久分类快照：Task 1、Task 2、Task 3。
- 服务端主导分类与重算：Task 3、Task 5。
- 规则影响预览和确认范围：Task 5、Task 8。
- 审计日志：Task 5、Task 10。
- 未知活动建议：Task 6、Task 8。
- 快捷纠错：Task 8。
- 独立分类管理页：Task 9。
- 自由项目标签和最近建议：Task 6、Task 8。
- 推荐最短分类时长：Task 4、Task 6、Task 9。
- AI 延后接入：本计划没有实现 LLM endpoint，只保留现有 URL 清洗测试和规则草稿边界。

空白项扫描：

- 本计划不包含待补内容或未定义的延后实现步骤。
- 每个代码任务包含测试、实现、运行命令和提交命令。

类型一致性：

- 后端使用 `ActivityClassificationApplyRangeRequest`、`ActivityClassificationPreviewRequest`、`ActivityClassificationPreviewDto`。
- 前端对应类型为 `ActivityClassificationApplyRange`、`ActivityClassificationPreview`。
- 设置字段统一为 `recommendedMinimumClassificationDurationMinutes` / `RecommendedMinimumClassificationDurationMinutes`。
- 快照服务统一使用 `ActivityClassificationRecordKey.FromRecord(record)` 生成 `record_key`。
