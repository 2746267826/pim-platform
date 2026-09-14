using System.Globalization;
using Microsoft.EntityFrameworkCore;
using Pim.Infrastructure.Data;
using Pim.Module.Mobile.DTOs;
using Pim.Module.Mobile.Entities;

namespace Pim.Module.Mobile.Services;

/// <summary>
/// 一次物化的结果：删除了多少旧派生行、写入了多少新派生行。
/// </summary>
public sealed record MobileAnalyticsMaterializationResult(
    int RemovedAggregates,
    int WrittenAggregates,
    int RemovedBlocks,
    int WrittenBlocks)
{
    public static readonly MobileAnalyticsMaterializationResult None = new(0, 0, 0, 0);
}

/// <summary>
/// 把在线计算的派生分析数据落库（#247②）。
///
/// 背景：<c>mobile_usage_aggregates</c> / <c>mobile_timeline_blocks</c> 早在实体层就定义好了
/// （唯一索引齐全），但生产里两张表始终是 0 行 —— 块与聚合每次请求都在线计算，每个端点各算一遍。
///
/// 这里的做法：
/// 1. 物化窗口按"本地日"对齐（把批次窗口扩到整日），因此端点只要请求整日区间，
///    边界裁剪口径就与在线计算完全一致；
/// 2. 先删掉窗口内的旧派生行再写入新行（派生数据可重建）；
/// 3. 记录覆盖窗口，只有"被物化过且没有失效标记"的区间才允许端点直接读取。
/// </summary>
public sealed class MobileAnalyticsMaterializationService
{
    private readonly PimDbContext _db;
    private readonly MobileUsageAggregationService _aggregationService;
    private readonly MobileTimelineBlockService _timelineBlockService;
    private readonly TimeProvider _timeProvider;

    public MobileAnalyticsMaterializationService(
        PimDbContext db,
        MobileUsageAggregationService aggregationService,
        MobileTimelineBlockService timelineBlockService,
        TimeProvider timeProvider)
    {
        _db = db;
        _aggregationService = aggregationService;
        _timelineBlockService = timelineBlockService;
        _timeProvider = timeProvider;
    }

    /// <summary>
    /// 物化 [windowStartUtc, windowEndUtc] 覆盖的整日窗口。窗口按默认时区对齐。
    /// </summary>
    public async Task<MobileAnalyticsMaterializationResult> MaterializeAsync(
        Guid userId,
        string deviceId,
        DateTimeOffset windowStartUtc,
        DateTimeOffset windowEndUtc,
        CancellationToken ct = default)
    {
        if (string.IsNullOrWhiteSpace(deviceId) || windowEndUtc <= windowStartUtc)
            return MobileAnalyticsMaterializationResult.None;

        var (rangeStartUtc, rangeEndUtc) = MobileAnalyticsWindow.AlignToLocalDays(
            windowStartUtc,
            windowEndUtc,
            MobileAnalyticsDefaults.DefaultTimezone);

        var buckets = await _aggregationService.BuildHourBucketsAsync(
            deviceId,
            rangeStartUtc,
            rangeEndUtc,
            ct);
        var blocks = await _timelineBlockService.BuildBlocksAsync(
            deviceId,
            rangeStartUtc,
            rangeEndUtc,
            ct);

        var now = _timeProvider.GetUtcNow();
        var aggregateRows = BuildAggregateRows(userId, deviceId, buckets, now);

        // 先删后写：派生表上有唯一索引，同一个 SaveChanges 里插入与删除的顺序无法保证。
        var outdatedAggregates = await _db.Set<MobileUsageAggregateEntity>()
            .Where(row => row.UserId == userId
                && row.DeviceId == deviceId
                && row.BucketStartUtc < rangeEndUtc
                && row.BucketEndUtc > rangeStartUtc)
            .ToListAsync(ct);
        _db.Set<MobileUsageAggregateEntity>().RemoveRange(outdatedAggregates);

        var outdatedBlocks = await _db.Set<MobileTimelineBlockEntity>()
            .Where(row => row.UserId == userId
                && row.DeviceId == deviceId
                && row.StartUtc < rangeEndUtc
                && row.EndUtc > rangeStartUtc)
            .ToListAsync(ct);
        _db.Set<MobileTimelineBlockEntity>().RemoveRange(outdatedBlocks);

        if (outdatedAggregates.Count > 0 || outdatedBlocks.Count > 0)
            await _db.SaveChangesAsync(ct);

        var blockRows = BuildBlockRows(
            userId,
            deviceId,
            blocks,
            MobileAnalyticsDefaults.DefaultTimezone,
            now);
        if (aggregateRows.Count > 0)
            _db.Set<MobileUsageAggregateEntity>().AddRange(aggregateRows);
        if (blockRows.Count > 0)
            _db.Set<MobileTimelineBlockEntity>().AddRange(blockRows);

        await RecordCoverageAsync(
            userId,
            deviceId,
            rangeStartUtc,
            rangeEndUtc,
            MobileAnalyticsDefaults.DefaultTimezone,
            now,
            ct);
        await _db.SaveChangesAsync(ct);

        return new MobileAnalyticsMaterializationResult(
            outdatedAggregates.Count,
            aggregateRows.Count,
            outdatedBlocks.Count,
            blockRows.Count);
    }

    private static List<MobileUsageAggregateEntity> BuildAggregateRows(
        Guid userId,
        string deviceId,
        IReadOnlyList<MobileUsageAggregationService.UsageBucketRow> buckets,
        DateTimeOffset now)
        => buckets
            // 唯一索引不含 source：事件行与兜底汇总行按 (桶, 包, 分类) 合并成一行，
            // 因此带 source 过滤的请求不允许走物化数据（见 MobileAnalyticsMaterializationGate）。
            .GroupBy(bucket => new
            {
                bucket.BucketStartUtc,
                bucket.BucketEndUtc,
                bucket.PackageName,
                bucket.LifeCategory
            })
            .Select(group =>
            {
                var rows = group.ToList();
                return new MobileUsageAggregateEntity
                {
                    UserId = userId,
                    DeviceId = deviceId,
                    Granularity = MobileAnalyticsDefaults.HourGranularity,
                    BucketStartUtc = group.Key.BucketStartUtc,
                    BucketEndUtc = group.Key.BucketEndUtc,
                    Timezone = MobileAnalyticsDefaults.DefaultTimezone,
                    PackageName = group.Key.PackageName,
                    DisplayName = FirstNonBlank(rows.Select(row => row.DisplayName)),
                    LifeCategory = group.Key.LifeCategory,
                    Source = rows.Any(row => string.Equals(row.Source, "events", StringComparison.Ordinal)) ? "events" : "fallback",
                    ForegroundSeconds = rows.Sum(row => row.BucketSeconds),
                    SessionCount = rows.Where(row => string.Equals(row.Source, "events", StringComparison.Ordinal))
                        .Select(row => row.SourceRowIndex)
                        .Distinct()
                        .Count(),
                    IsSystemNoise = rows.All(row => row.IsSystemNoise),
                    QualityFlagsJson = MobileAnalyticsJson.SerializeFlags(
                        rows.SelectMany(row => row.QualityFlags)),
                    IsStale = rows.Any(row => row.IsStale),
                    GeneratedAt = now,
                    CreatedAt = now,
                    UpdatedAt = now
                };
            })
            .ToList();

    private static List<MobileTimelineBlockEntity> BuildBlockRows(
        Guid userId,
        string deviceId,
        IReadOnlyList<MobileTimelineBlockService.ComputedBlock> blocks,
        string timezone,
        DateTimeOffset now)
        => blocks.Select(block => new MobileTimelineBlockEntity
        {
            UserId = userId,
            DeviceId = deviceId,
            BlockId = block.Id,
            StartUtc = block.StartUtc,
            EndUtc = block.Dto.EndUtc,
            LocalDate = LocalDate(block.StartUtc, timezone),
            Timezone = timezone,
            LifeCategory = block.Dto.LifeCategory,
            ForegroundSeconds = block.Dto.ForegroundSeconds,
            SessionCount = block.Dto.SessionCount,
            AppCount = block.Dto.AppCount,
            TopAppsJson = MobileAnalyticsJson.SerializeTopApps(block.Dto.TopApps),
            SourceMixJson = MobileAnalyticsJson.SerializeSourceMix(block.Dto.SourceMix),
            QualityFlagsJson = MobileAnalyticsJson.SerializeFlags(block.Dto.QualityFlags),
            IncludesSystemNoise = block.Dto.IncludesSystemNoise,
            IsStale = false,
            GeneratedAt = now,
            CreatedAt = now,
            UpdatedAt = now
        }).ToList();

    private async Task RecordCoverageAsync(
        Guid userId,
        string deviceId,
        DateTimeOffset rangeStartUtc,
        DateTimeOffset rangeEndUtc,
        string timezone,
        DateTimeOffset now,
        CancellationToken ct)
    {
        var existing = await _db.Set<MobileAnalyticsMaterializationEntity>()
            .Where(row => row.UserId == userId
                && row.DeviceId == deviceId
                && row.CoveredFromUtc == rangeStartUtc
                && row.CoveredToUtc == rangeEndUtc)
            .ToListAsync(ct);

        if (existing.Count > 0)
        {
            foreach (var row in existing)
            {
                row.Timezone = timezone;
                row.GeneratedAt = now;
                row.UpdatedAt = now;
            }

            return;
        }

        _db.Set<MobileAnalyticsMaterializationEntity>().Add(new MobileAnalyticsMaterializationEntity
        {
            UserId = userId,
            DeviceId = deviceId,
            Timezone = timezone,
            CoveredFromUtc = rangeStartUtc,
            CoveredToUtc = rangeEndUtc,
            GeneratedAt = now,
            CreatedAt = now,
            UpdatedAt = now
        });
    }

    private static string LocalDate(DateTimeOffset value, string timezone)
    {
        var local = TimeZoneInfo.ConvertTime(value, ResolveTimezone(timezone));
        return local.ToString("yyyy-MM-dd", CultureInfo.InvariantCulture);
    }

    private static string FirstNonBlank(IEnumerable<string> values)
        => values.FirstOrDefault(value => !string.IsNullOrWhiteSpace(value)) ?? string.Empty;

    internal static TimeZoneInfo ResolveTimezone(string timezone)
    {
        try
        {
            return TimeZoneInfo.FindSystemTimeZoneById(timezone);
        }
        catch (TimeZoneNotFoundException) when (timezone == MobileAnalyticsDefaults.DefaultTimezone)
        {
            return TimeZoneInfo.FindSystemTimeZoneById("China Standard Time");
        }
        catch (InvalidTimeZoneException) when (timezone == MobileAnalyticsDefaults.DefaultTimezone)
        {
            return TimeZoneInfo.FindSystemTimeZoneById("China Standard Time");
        }
    }
}

/// <summary>
/// 物化窗口的对齐规则：把任意区间扩到"本地整日"，
/// 保证端点请求整日区间时的裁剪口径与在线计算一致（#247②）。
/// </summary>
public static class MobileAnalyticsWindow
{
    public static (DateTimeOffset StartUtc, DateTimeOffset EndUtc) AlignToLocalDays(
        DateTimeOffset startUtc,
        DateTimeOffset endUtc,
        string timezone)
    {
        if (endUtc <= startUtc)
            return (startUtc, startUtc);

        var timeZoneInfo = MobileAnalyticsMaterializationService.ResolveTimezone(timezone);
        var localStart = TimeZoneInfo.ConvertTime(startUtc, timeZoneInfo).Date;
        // 结束时间落在本地日边界上时不要多扩一天（半开区间语义）。
        var localEndExclusive = TimeZoneInfo.ConvertTime(endUtc, timeZoneInfo);
        var localEnd = localEndExclusive.TimeOfDay == TimeSpan.Zero
            ? localEndExclusive.Date
            : localEndExclusive.Date.AddDays(1);

        return (LocalDateStartUtc(localStart, timeZoneInfo), LocalDateStartUtc(localEnd, timeZoneInfo));
    }

    /// <summary>请求区间是否与该覆盖窗口逐刻相等（物化数据只在这个前提下可复用）。</summary>
    public static bool MatchesExactly(
        DateTimeOffset coveredFromUtc,
        DateTimeOffset coveredToUtc,
        DateTimeOffset rangeStartUtc,
        DateTimeOffset rangeEndUtc)
        => coveredFromUtc == rangeStartUtc && coveredToUtc == rangeEndUtc;

    private static DateTimeOffset LocalDateStartUtc(DateTime localDate, TimeZoneInfo timeZoneInfo)
    {
        var unspecified = DateTime.SpecifyKind(localDate.Date, DateTimeKind.Unspecified);
        var utc = TimeZoneInfo.ConvertTimeToUtc(unspecified, timeZoneInfo);
        return new DateTimeOffset(utc, TimeSpan.Zero);
    }
}
