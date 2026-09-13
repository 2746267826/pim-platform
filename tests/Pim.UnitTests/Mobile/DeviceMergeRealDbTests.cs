using Microsoft.EntityFrameworkCore;
using Npgsql;
using Pim.Infrastructure.Data;
using Pim.Module.Mobile.DTOs;
using Pim.Module.Mobile.Entities;
using Pim.Module.Mobile.Services;
using Xunit;

namespace Pim.UnitTests.Mobile;

/// <summary>
/// 真库端到端验证（issue #230 / #231）。连不上 PostgreSQL 时跳过而非失败，
/// 与仓库既有 RealDb 测试（<c>PcTrackerDedupRealDbTests</c>）保持一致。
///
/// 覆盖两件 SQLite 覆盖不了的事：
/// 1. 用生产同款配置 <c>EnableRetryOnFailure(3)</c>（NpgsqlRetryingExecutionStrategy）跑合并，
///    修复前这里会抛出与生产 500 完全一致的 InvalidOperationException；
/// 2. 合并后用真实的 <see cref="MobileUsageQueryService"/> 按目标设备解析 App 名称/分类
///    （该查询对 DateTimeOffset 排序，SQLite 不支持，真库支持）。
///
/// 所有写入都发生在临时 schema 内，结束时 DROP SCHEMA CASCADE，不碰 public。
/// </summary>
[Trait("DataSource", "RealDb")]
public sealed class DeviceMergeRealDbTests
{
    private const string DefaultConnStr =
        "Host=127.0.0.1;Database=pim;Username=opencode;Password=62f0a50bb963bb648f8e400399def95a;CommandTimeout=60";

    private static string ConnStr =>
        Environment.GetEnvironmentVariable("PIM_TEST_CONN") ?? DefaultConnStr;

    private static readonly string[] DeviceScopedTables =
    [
        "mobile_devices",
        "mobile_app_catalog",
        "mobile_usage_events",
        "mobile_usage_sessions",
        "mobile_usage_summaries",
        "mobile_location_points",
        "mobile_sync_batches",
        "mobile_timeline_blocks",
    ];

    private const string TargetDeviceId = "android-e2e-target";
    private const string SourceDeviceA = "android-e2e-source-a";
    private const string SourceDeviceB = "android-e2e-source-b";
    private static readonly Guid UserId = Guid.Parse("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb");
    private static readonly DateTimeOffset Now = DateTimeOffset.Parse("2026-09-13T02:00:00Z");

    [Fact]
    public async Task MergeAsync_WithProductionRetryStrategy_MovesEverythingAndKeepsAppNamesResolvable()
    {
        MobileTestHelpers.RegisterMobileModule();

        await using var admin = new NpgsqlConnection(ConnStr);
        if (!await TryOpenAsync(admin)) return;

        var schema = $"test_device_merge_{Guid.NewGuid():N}";
        try
        {
            if (!await TryCreateSchemaAsync(admin, schema)) return;

            var searchPathConn = new NpgsqlConnectionStringBuilder(ConnStr) { SearchPath = schema }.ConnectionString;
            var options = new DbContextOptionsBuilder<PimDbContext>()
                .UseNpgsql(searchPathConn, npgsql => npgsql.EnableRetryOnFailure(3))
                .Options;

            await using var db = new PimDbContext(options);
            await SeedAsync(db);

            var service = new DeviceManagementService(
                db, MobileTestHelpers.CurrentUser(UserId), MobileTestHelpers.Time(Now));

            // 修复前：NpgsqlRetryingExecutionStrategy 在事务内第一条命令即抛
            // "does not support user-initiated transactions"（生产实测 HTTP 500，耗时 53ms）。
            await service.MergeAsync([SourceDeviceA, SourceDeviceB], TargetDeviceId, CancellationToken.None);

            // 1) 源设备整体消失，6 张业务表 + catalog 全部改挂目标设备
            Assert.Empty(await db.Set<MobileDeviceEntity>()
                .Where(d => d.DeviceId == SourceDeviceA || d.DeviceId == SourceDeviceB).ToListAsync());
            Assert.Equal(2, await db.Set<MobileUsageEventEntity>().CountAsync());
            Assert.Equal(1, await db.Set<MobileUsageSessionEntity>().CountAsync());
            Assert.Equal(1, await db.Set<MobileUsageSummaryEntity>().CountAsync());
            Assert.Equal(1, await db.Set<MobileLocationPointEntity>().CountAsync());
            Assert.Equal(1, await db.Set<MobileSyncBatchEntity>().CountAsync());

            // 2) catalog 无孤儿、无重复：目标设备每个包名恰好一行
            var catalog = await db.Set<MobileAppCatalogEntity>().ToListAsync();
            Assert.All(catalog, row => Assert.Equal(TargetDeviceId, row.DeviceId));
            Assert.Equal(catalog.Count, catalog.Select(row => row.PackageName).Distinct().Count());

            // 3) 真实查询链路：按目标设备解析并入记录的 App 名称与分类
            var query = new MobileUsageQueryService(
                db, MobileTestHelpers.CurrentUser(UserId), MobileTestHelpers.Time(Now));
            var summary = await query.GetSummaryAsync(
                new MobileSummaryQuery(TargetDeviceId, null, null),
                CancellationToken.None);
            var app = Assert.Single(summary.AppRanking);
            Assert.Equal("com.legacy.app", app.PackageName);
            Assert.Equal("Legacy App", app.DisplayName);
            Assert.Equal("tools", app.CategoryName);
        }
        finally
        {
            await using var cleanup = new NpgsqlConnection(ConnStr);
            await cleanup.OpenAsync();
            await using var drop = new NpgsqlCommand($"DROP SCHEMA IF EXISTS \"{schema}\" CASCADE", cleanup);
            await drop.ExecuteNonQueryAsync();
        }
    }

    private static async Task<bool> TryOpenAsync(NpgsqlConnection conn)
    {
        try
        {
            await conn.OpenAsync();
            return true;
        }
        catch
        {
            // CI 无 PostgreSQL：跳过而非失败。
            return false;
        }
    }

    private static async Task<bool> TryCreateSchemaAsync(NpgsqlConnection conn, string schema)
    {
        try
        {
            await using (var create = new NpgsqlCommand($"CREATE SCHEMA \"{schema}\"", conn))
                await create.ExecuteNonQueryAsync();

            // 借用 pim 库里已存在的表结构，测试库不写 public。
            foreach (var table in DeviceScopedTables)
            {
                await using var copy = new NpgsqlCommand(
                    $"CREATE TABLE \"{schema}\".\"{table}\" (LIKE public.\"{table}\" INCLUDING ALL)", conn);
                await copy.ExecuteNonQueryAsync();
            }

            return true;
        }
        catch
        {
            // 目标库没有可借用的表结构（例如空库）：跳过。
            return false;
        }
    }

    private static async Task SeedAsync(PimDbContext db)
    {
        db.Set<MobileDeviceEntity>().AddRange(
            Device(TargetDeviceId, Now),
            Device(SourceDeviceA, Now.AddDays(-60)),
            Device(SourceDeviceB, Now.AddDays(-50)));

        db.Set<MobileUsageEventEntity>().AddRange(
            Event(SourceDeviceA, "com.legacy.app", Now.AddDays(-60)),
            Event(SourceDeviceB, "com.legacy.app", Now.AddDays(-50)));

        db.Set<MobileUsageSessionEntity>().Add(new MobileUsageSessionEntity
        {
            UserId = UserId,
            DeviceId = SourceDeviceA,
            PackageName = "com.legacy.app",
            StartUtc = Now.AddDays(-60),
            EndUtc = Now.AddDays(-60).AddMinutes(5),
            DurationMs = 300_000,
        });
        db.Set<MobileUsageSummaryEntity>().Add(new MobileUsageSummaryEntity
        {
            UserId = UserId,
            DeviceId = SourceDeviceA,
            PackageName = "com.legacy.app",
            WindowStartUtc = Now.AddDays(-60),
            WindowEndUtc = Now.AddDays(-60).AddMinutes(15),
            TotalTimeVisibleMs = 300_000,
            LastTimeUsedUtc = Now.AddDays(-60).AddMinutes(15),
            SourceKind = "usage-summary",
        });
        db.Set<MobileLocationPointEntity>().Add(new MobileLocationPointEntity
        {
            UserId = UserId,
            DeviceId = SourceDeviceA,
            RecordedAtUtc = Now.AddDays(-60),
            Latitude = 31.2304m,
            Longitude = 121.4737m,
            HorizontalAccuracyMeters = 12.5m,
            Provider = "fused",
            Source = "android",
        });
        db.Set<MobileSyncBatchEntity>().Add(new MobileSyncBatchEntity
        {
            UserId = UserId,
            DeviceId = SourceDeviceA,
            BatchId = "e2e-batch-a",
            WindowStartUtc = Now.AddDays(-60),
            WindowEndUtc = Now.AddDays(-60).AddMinutes(15),
            AcceptedCount = 1,
            Status = "completed",
            CompletedAtUtc = Now.AddDays(-60).AddMinutes(15),
        });

        // 目标设备已有同名包（生产实测 594 条重复），源设备的条目更新 -> 元数据应被覆盖。
        db.Set<MobileAppCatalogEntity>().AddRange(
            Catalog(TargetDeviceId, "com.legacy.app", "Legacy App (stale)", "uncategorized", Now.AddDays(-90)),
            Catalog(SourceDeviceA, "com.legacy.app", "Legacy App", "tools", Now.AddDays(-30)),
            Catalog(SourceDeviceB, "com.unique.b.app", "Unique B", "social", Now.AddDays(-50)));

        await db.SaveChangesAsync();
    }

    private static MobileDeviceEntity Device(string deviceId, DateTimeOffset lastSeen) => new()
    {
        UserId = UserId,
        DeviceId = deviceId,
        DeviceHash = $"hash-{deviceId}",
        DisplayName = "OPPO PLG110",
        Brand = "OPPO",
        Model = "PLG110",
        OsVersion = "15",
        AppVersion = "2026.09.504",
        RegisteredAtUtc = lastSeen,
        LastSeenAtUtc = lastSeen,
    };

    private static MobileUsageEventEntity Event(string deviceId, string packageName, DateTimeOffset timestamp) => new()
    {
        UserId = UserId,
        DeviceId = deviceId,
        PackageName = packageName,
        EventType = "foreground",
        EventTimestampUtc = timestamp,
        SourceWindowStartUtc = timestamp,
        SourceWindowEndUtc = timestamp.AddMinutes(1),
        CollectedAtUtc = timestamp.AddMinutes(1),
    };

    private static MobileAppCatalogEntity Catalog(
        string deviceId, string packageName, string displayName, string category, DateTimeOffset updatedAt) => new()
    {
        UserId = UserId,
        DeviceId = deviceId,
        PackageName = packageName,
        DisplayName = displayName,
        Category = category,
        CreatedAt = updatedAt,
        UpdatedAt = updatedAt,
    };
}
