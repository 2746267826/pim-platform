using System;
using System.Collections.Generic;
using System.Data;
using System.Data.Common;
using System.Diagnostics;
using System.Linq;
using System.Threading;
using System.Threading.Tasks;
using Microsoft.EntityFrameworkCore;
using Microsoft.Extensions.Logging;
using Microsoft.Extensions.Options;
using Pim.Core.Invariants;
using Pim.Infrastructure.Data;

namespace Pim.Infrastructure.Operations;

/// <summary>
/// 数据可靠性体检服务（实现 IDataQualityInspector，真实接入 PimDbContext 数据库消费 Pim.Core.Invariants 纯函数判据库）。
/// 支持 S1–S13 全部 13 根基准尺子真实取数，四态区分（红/黄/绿/未知），带超时、只读与采样上限保护。
/// </summary>
public sealed class DataReliabilityQualityInspector : IDataQualityInspector
{
    private readonly PimDbContext? _db;
    private readonly InvariantOptions _options;
    private readonly ILogger<DataReliabilityQualityInspector> _logger;

    public DataReliabilityQualityInspector(
        PimDbContext? db,
        IOptions<InvariantOptions> options,
        ILogger<DataReliabilityQualityInspector> logger)
    {
        _db = db;
        _options = options?.Value ?? InvariantOptions.Default;
        _logger = logger;
    }

    public string CheckName => "data_reliability";

    public async Task<DataQualityInspectionResult> InspectAsync(DateTimeOffset now, CancellationToken ct = default)
    {
        var sw = Stopwatch.StartNew();
        var details = new Dictionary<string, string>();
        var (resolvedOptions, fallback, fallbackNote) = InvariantOptions.Resolve(_options);

        if (fallback)
        {
            details["options_fallback"] = fallbackNote ?? "配置非法回退默认值";
        }

        int redCount = 0;
        int yellowCount = 0;
        int greenCount = 0;
        int unknownCount = 0;
        int totalIssues = 0;

        if (_db == null)
        {
            _logger.LogWarning("PimDbContext 未注入，数据可靠性体检全部标记为未知");
            for (int i = 1; i <= 13; i++)
            {
                RecordUnknown(GetInvariantKey(i), "数据库上下文未注入或不可用", details, ref unknownCount, ref totalIssues);
            }

            return new DataQualityInspectionResult(
                CheckName,
                IsHealthy: false,
                IssueCount: totalIssues,
                Message: "数据库上下文未配置，全部 13 项判据处于未知状态",
                Details: details);
        }

        DbConnection? conn = null;
        try
        {
            if (_db.Database.IsRelational())
            {
                conn = _db.Database.GetDbConnection();
                if (conn.State != ConnectionState.Open)
                {
                    await conn.OpenAsync(ct);
                }
            }
        }
        catch (Exception ex)
        {
            _logger.LogError(ex, "建立数据可靠性体检数据库连接失败");
        }

        if (conn == null || conn.State != ConnectionState.Open)
        {
            for (int i = 1; i <= 13; i++)
            {
                RecordUnknown(GetInvariantKey(i), "非关系型数据库或数据库连接未打开", details, ref unknownCount, ref totalIssues);
            }

            return new DataQualityInspectionResult(
                CheckName,
                IsHealthy: false,
                IssueCount: totalIssues,
                Message: "数据库连接不可用，全部 13 项判据标记为未知",
                Details: details);
        }

        var nowUtc = now.UtcDateTime;

        // 执行 S1–S13 判据取数与断言
        await ExecuteCheckSafeAsync("S1_INV-P16", () => CheckS1Async(conn, resolvedOptions, nowUtc, ct), details, (r, k) => RecordCheck(k, r, details, ref redCount, ref yellowCount, ref greenCount, ref unknownCount, ref totalIssues));
        await ExecuteCheckSafeAsync("S2_INV-P17", () => CheckS2Async(conn, resolvedOptions, nowUtc, ct), details, (r, k) => RecordCheck(k, r, details, ref redCount, ref yellowCount, ref greenCount, ref unknownCount, ref totalIssues));
        await ExecuteCheckSafeAsync("S3_INV-P18", () => CheckS3Async(conn, resolvedOptions, nowUtc, ct), details, (r, k) => RecordCheck(k, r, details, ref redCount, ref yellowCount, ref greenCount, ref unknownCount, ref totalIssues));
        await ExecuteCheckSafeAsync("S4_INV-C18", () => CheckS4Async(conn, resolvedOptions, nowUtc, ct), details, (r, k) => RecordCheck(k, r, details, ref redCount, ref yellowCount, ref greenCount, ref unknownCount, ref totalIssues));
        await ExecuteCheckSafeAsync("S5_INV-P19", () => CheckS5Async(conn, resolvedOptions, nowUtc, ct), details, (r, k) => RecordCheck(k, r, details, ref redCount, ref yellowCount, ref greenCount, ref unknownCount, ref totalIssues));
        await ExecuteCheckSafeAsync("S6_INV-P20", () => CheckS6Async(conn, resolvedOptions, nowUtc, ct), details, (r, k) => RecordCheck(k, r, details, ref redCount, ref yellowCount, ref greenCount, ref unknownCount, ref totalIssues));
        await ExecuteCheckSafeAsync("S7_INV-P21", () => CheckS7Async(conn, resolvedOptions, nowUtc, ct), details, (r, k) => RecordCheck(k, r, details, ref redCount, ref yellowCount, ref greenCount, ref unknownCount, ref totalIssues));
        await ExecuteCheckSafeAsync("S8_INV-C19", () => CheckS8Async(conn, resolvedOptions, nowUtc, ct), details, (r, k) => RecordCheck(k, r, details, ref redCount, ref yellowCount, ref greenCount, ref unknownCount, ref totalIssues));
        await ExecuteCheckSafeAsync("S9_INV-C20", () => CheckS9Async(conn, resolvedOptions, nowUtc, ct), details, (r, k) => RecordCheck(k, r, details, ref redCount, ref yellowCount, ref greenCount, ref unknownCount, ref totalIssues));
        await ExecuteCheckSafeAsync("S10_INV-C21", () => CheckS10Async(conn, resolvedOptions, nowUtc, ct), details, (r, k) => RecordCheck(k, r, details, ref redCount, ref yellowCount, ref greenCount, ref unknownCount, ref totalIssues));
        await ExecuteCheckSafeAsync("S11_INV-M21", () => CheckS11Async(conn, resolvedOptions, nowUtc, ct), details, (r, k) => RecordCheck(k, r, details, ref redCount, ref yellowCount, ref greenCount, ref unknownCount, ref totalIssues));
        await ExecuteCheckSafeAsync("S12_INV-M22", () => CheckS12Async(conn, resolvedOptions, nowUtc, ct), details, (r, k) => RecordCheck(k, r, details, ref redCount, ref yellowCount, ref greenCount, ref unknownCount, ref totalIssues));
        await ExecuteCheckSafeAsync("S13_INV-P22", () => CheckS13Async(conn, resolvedOptions, nowUtc, ct), details, (r, k) => RecordCheck(k, r, details, ref redCount, ref yellowCount, ref greenCount, ref unknownCount, ref totalIssues));

        sw.Stop();
        bool isHealthy = redCount == 0 && unknownCount == 0 && totalIssues == 0;
        details["summary"] = $"{redCount} Red, {yellowCount} Yellow, {greenCount} Green, {unknownCount} Unknown";

        string message = isHealthy
            ? $"数据可靠性体检全部通过 (13/13 绿灯)。耗时 {sw.ElapsedMilliseconds}ms。"
            : $"数据可靠性体检发现异常：{redCount} 红, {yellowCount} 黄, {greenCount} 绿, {unknownCount} 未知。耗时 {sw.ElapsedMilliseconds}ms。";

        _logger.LogInformation(
            "数据可靠性体检完成：Healthy={IsHealthy}, Red={RedCount}, Yellow={YellowCount}, Green={GreenCount}, Unknown={UnknownCount}, Elapsed={ElapsedMs}ms",
            isHealthy, redCount, yellowCount, greenCount, unknownCount, sw.ElapsedMilliseconds);

        return new DataQualityInspectionResult(
            CheckName,
            isHealthy,
            totalIssues,
            message,
            details);
    }

    #region Check Implementations

    private async Task<InvariantResult> CheckS1Async(DbConnection conn, InvariantOptions options, DateTime nowUtc, CancellationToken ct)
    {
        if (!await TableExistsAsync(conn, "pc_aw_events", ct))
            return InvariantResult.Unknown("INV-P16 UNKNOWN: 数据表 pc_aw_events 不存在");

        var cutoff = nowUtc.AddHours(-options.RecentWindowHours * 7); // 扩大至近期窗口以覆盖代表性数据
        await using var cmd = conn.CreateCommand();
        cmd.CommandTimeout = 15;
        cmd.CommandText = """
            SELECT id, device_id, event_type, timestamp, duration 
            FROM pc_aw_events 
            WHERE event_type IN ('window', 'web', 'web-page') 
            ORDER BY timestamp DESC 
            LIMIT 2000;
            """;

        var list = new List<EventTimeSpan>();
        await using var reader = await cmd.ExecuteReaderAsync(ct);
        while (await reader.ReadAsync(ct))
        {
            long id = reader.GetInt64(0);
            string dev = reader.IsDBNull(1) ? "default" : reader.GetString(1);
            string type = reader.IsDBNull(2) ? "window" : reader.GetString(2);
            DateTime start = reader.GetDateTime(3);
            double dur = reader.GetDouble(4);
            list.Add(new EventTimeSpan
            {
                EventId = id.ToString(),
                DeviceId = dev,
                EventType = type,
                StartTime = start,
                EndTime = start.AddSeconds(dur)
            });
        }

        if (list.Count == 0)
            return InvariantResult.Unknown("INV-P16 UNKNOWN: pc_aw_events 中无可用事件序列");

        return DataReliabilityInvariants.CheckS1_NoOverlap(list, options, referenceTimeUtc: nowUtc);
    }

    private async Task<InvariantResult> CheckS2Async(DbConnection conn, InvariantOptions options, DateTime nowUtc, CancellationToken ct)
    {
        if (!await TableExistsAsync(conn, "pc_aw_events", ct))
            return InvariantResult.Unknown("INV-P17 UNKNOWN: 数据表 pc_aw_events 不存在");

        double thresholdSeconds = options.LongEventThresholdMinutes * 60.0;
        await using var cmd = conn.CreateCommand();
        cmd.CommandTimeout = 15;
        cmd.CommandText = $"""
            SELECT id, device_id, event_type, timestamp, duration, app_name, afk_status 
            FROM pc_aw_events 
            WHERE duration > {thresholdSeconds:F0} 
            ORDER BY duration DESC 
            LIMIT 50;
            """;

        var rawEvents = new List<(long id, string dev, string type, DateTime start, double dur, string? app, string? afk)>();
        await using (var reader = await cmd.ExecuteReaderAsync(ct))
        {
            while (await reader.ReadAsync(ct))
            {
                rawEvents.Add((
                    reader.GetInt64(0),
                    reader.IsDBNull(1) ? "default" : reader.GetString(1),
                    reader.IsDBNull(2) ? "window" : reader.GetString(2),
                    reader.GetDateTime(3),
                    reader.GetDouble(4),
                    reader.IsDBNull(5) ? null : reader.GetString(5),
                    reader.IsDBNull(6) ? null : reader.GetString(6)
                ));
            }
        }

        if (rawEvents.Count == 0)
            return InvariantResult.Unknown("INV-P17 UNKNOWN: 无超过 30 分钟事件记录");

        bool hasKeystats = await TableExistsAsync(conn, "pc_keystats_samples", ct);
        var candidates = new List<LongEventCandidate>();

        foreach (var (id, dev, type, start, dur, app, afk) in rawEvents)
        {
            var end = start.AddSeconds(dur);
            long keystrokes = 0;
            long clicks = 0;

            if (hasKeystats)
            {
                await using var keyCmd = conn.CreateCommand();
                keyCmd.CommandTimeout = 5;
                keyCmd.CommandText = """
                    SELECT COALESCE(SUM(key_presses), 0), COALESCE(SUM(left_clicks + right_clicks), 0)
                    FROM pc_keystats_samples
                    WHERE pim_device_id = @dev AND sampled_at_utc >= @start AND sampled_at_utc <= @end;
                    """;
                var pDev = keyCmd.CreateParameter(); pDev.ParameterName = "@dev"; pDev.Value = dev; keyCmd.Parameters.Add(pDev);
                var pStart = keyCmd.CreateParameter(); pStart.ParameterName = "@start"; pStart.Value = start; keyCmd.Parameters.Add(pStart);
                var pEnd = keyCmd.CreateParameter(); pEnd.ParameterName = "@end"; pEnd.Value = end; keyCmd.Parameters.Add(pEnd);

                await using var keyReader = await keyCmd.ExecuteReaderAsync(ct);
                if (await keyReader.ReadAsync(ct))
                {
                    keystrokes = keyReader.GetInt64(0);
                    clicks = keyReader.GetInt64(1);
                }
            }

            bool isMedia = !string.IsNullOrEmpty(app) &&
                (app.Contains("player", StringComparison.OrdinalIgnoreCase) ||
                 app.Contains("music", StringComparison.OrdinalIgnoreCase) ||
                 app.Contains("video", StringComparison.OrdinalIgnoreCase) ||
                 app.Contains("bilibili", StringComparison.OrdinalIgnoreCase) ||
                 app.Contains("potplayer", StringComparison.OrdinalIgnoreCase) ||
                 app.Contains("spotify", StringComparison.OrdinalIgnoreCase));

            bool isGap = type.Equals("afk", StringComparison.OrdinalIgnoreCase) ||
                         type.Equals("gap", StringComparison.OrdinalIgnoreCase) ||
                         (afk != null && afk.Equals("afk", StringComparison.OrdinalIgnoreCase));

            candidates.Add(new LongEventCandidate
            {
                EventId = id.ToString(),
                DeviceId = dev,
                EventType = type,
                StartTime = start,
                EndTime = end,
                Keystrokes = keystrokes,
                MouseClicks = clicks,
                IsMediaActive = isMedia,
                IsAudible = false,
                IsGapOrOffline = isGap,
                AppName = app
            });
        }

        return DataReliabilityInvariants.CheckS2_OverlongEventEvidence(candidates, options, referenceTimeUtc: nowUtc);
    }

    private async Task<InvariantResult> CheckS3Async(DbConnection conn, InvariantOptions options, DateTime nowUtc, CancellationToken ct)
    {
        if (!await TableExistsAsync(conn, "pc_aw_events", ct))
            return InvariantResult.Unknown("INV-P18 UNKNOWN: 数据表 pc_aw_events 不存在");

        await using var cmd = conn.CreateCommand();
        cmd.CommandTimeout = 15;
        cmd.CommandText = """
            SELECT device_id, ((timestamp + interval '4 hours')::date)::text as biz_date, SUM(duration) as total_sec
            FROM pc_aw_events
            WHERE (afk_status IS NULL OR afk_status != 'afk')
            GROUP BY device_id, biz_date
            ORDER BY biz_date DESC
            LIMIT 30;
            """;

        var list = new List<DailyActiveDuration>();
        await using var reader = await cmd.ExecuteReaderAsync(ct);
        while (await reader.ReadAsync(ct))
        {
            string dev = reader.IsDBNull(0) ? "default" : reader.GetString(0);
            string date = reader.GetString(1);
            double sec = reader.GetDouble(2);
            list.Add(new DailyActiveDuration
            {
                DeviceId = dev,
                Date = date,
                ActiveDurationSeconds = sec
            });
        }

        if (list.Count == 0)
            return InvariantResult.Unknown("INV-P18 UNKNOWN: 无活跃事件可聚合单日时长");

        return DataReliabilityInvariants.CheckS3_DailyDurationBounded(list, options);
    }

    private async Task<InvariantResult> CheckS4Async(DbConnection conn, InvariantOptions options, DateTime nowUtc, CancellationToken ct)
    {
        var keys = new List<BusinessRecordKey>();
        bool anyTableExists = false;

        // 1. 定位去重
        if (await TableExistsAsync(conn, "mobile_location_points", ct))
        {
            anyTableExists = true;
            await using var cmd = conn.CreateCommand();
            cmd.CommandTimeout = 15;
            cmd.CommandText = """
                SELECT device_id, recorded_at_utc, latitude, longitude, count(*) 
                FROM mobile_location_points 
                GROUP BY device_id, recorded_at_utc, latitude, longitude 
                HAVING count(*) > 1 
                LIMIT 50;
                """;
            await using var r = await cmd.ExecuteReaderAsync(ct);
            while (await r.ReadAsync(ct))
            {
                string dev = r.GetString(0);
                DateTime t = r.GetDateTime(1);
                double lat = r.GetDouble(2);
                double lon = r.GetDouble(3);
                long cnt = r.GetInt64(4);
                for (int i = 0; i < cnt; i++)
                {
                    keys.Add(new BusinessRecordKey { Domain = "Location", DeviceId = dev, UniqueKey = $"{t:O}:{lat:F6}:{lon:F6}", Timestamp = t });
                }
            }
        }

        // 2. 手机事件去重
        if (await TableExistsAsync(conn, "mobile_usage_events", ct))
        {
            anyTableExists = true;
            await using var cmd = conn.CreateCommand();
            cmd.CommandTimeout = 15;
            cmd.CommandText = """
                SELECT device_id, package_name, event_timestamp_utc, count(*) 
                FROM mobile_usage_events 
                GROUP BY device_id, package_name, event_timestamp_utc 
                HAVING count(*) > 1 
                LIMIT 50;
                """;
            await using var r = await cmd.ExecuteReaderAsync(ct);
            while (await r.ReadAsync(ct))
            {
                string dev = r.GetString(0);
                string pkg = r.GetString(1);
                DateTime t = r.GetDateTime(2);
                long cnt = r.GetInt64(3);
                for (int i = 0; i < cnt; i++)
                {
                    keys.Add(new BusinessRecordKey { Domain = "Mobile", DeviceId = dev, UniqueKey = $"{pkg}:{t:O}", Timestamp = t });
                }
            }
        }

        // 3. PC 事件去重
        if (await TableExistsAsync(conn, "pc_aw_events", ct))
        {
            anyTableExists = true;
            await using var cmd = conn.CreateCommand();
            cmd.CommandTimeout = 15;
            cmd.CommandText = """
                SELECT device_id, event_type, timestamp, count(*) 
                FROM pc_aw_events 
                GROUP BY device_id, event_type, timestamp 
                HAVING count(*) > 1 
                LIMIT 50;
                """;
            await using var r = await cmd.ExecuteReaderAsync(ct);
            while (await r.ReadAsync(ct))
            {
                string dev = r.GetString(0);
                string type = r.GetString(1);
                DateTime t = r.GetDateTime(2);
                long cnt = r.GetInt64(3);
                for (int i = 0; i < cnt; i++)
                {
                    keys.Add(new BusinessRecordKey { Domain = "Pc", DeviceId = dev, UniqueKey = $"{type}:{t:O}", Timestamp = t });
                }
            }
        }

        if (!anyTableExists)
            return InvariantResult.Unknown("INV-C18 UNKNOWN: 定位与事件相关数据表均不存在");

        if (keys.Count == 0)
        {
            // 若无重复项，采样几条正常项以验证表非空
            await using var sampleCmd = conn.CreateCommand();
            sampleCmd.CommandTimeout = 10;
            sampleCmd.CommandText = "SELECT device_id, timestamp FROM pc_aw_events ORDER BY id DESC LIMIT 10;";
            await using var sr = await sampleCmd.ExecuteReaderAsync(ct);
            while (await sr.ReadAsync(ct))
            {
                keys.Add(new BusinessRecordKey { Domain = "Pc", DeviceId = sr.GetString(0), UniqueKey = Guid.NewGuid().ToString(), Timestamp = sr.GetDateTime(1) });
            }
        }

        if (keys.Count == 0)
            return InvariantResult.Unknown("INV-C18 UNKNOWN: 业务表为空，无数据检验业务键唯一性");

        return DataReliabilityInvariants.CheckS4_BusinessKeyUnique(keys, options, referenceTimeUtc: nowUtc);
    }

    private async Task<InvariantResult> CheckS5Async(DbConnection conn, InvariantOptions options, DateTime nowUtc, CancellationToken ct)
    {
        if (!await TableExistsAsync(conn, "pc_aw_events", ct))
            return InvariantResult.Unknown("INV-P19 UNKNOWN: 数据表 pc_aw_events 不存在");

        await using var cmd = conn.CreateCommand();
        cmd.CommandTimeout = 15;
        cmd.CommandText = """
            SELECT id, device_id, timestamp, created_at 
            FROM pc_aw_events 
            ORDER BY id DESC 
            LIMIT 100;
            """;

        var items = new List<ClockEventItem>();
        await using var reader = await cmd.ExecuteReaderAsync(ct);
        while (await reader.ReadAsync(ct))
        {
            long id = reader.GetInt64(0);
            string dev = reader.IsDBNull(1) ? "default" : reader.GetString(1);
            DateTime ts = reader.GetDateTime(2);
            DateTime created = reader.GetDateTime(3);
            items.Add(new ClockEventItem
            {
                EventId = id.ToString(),
                DeviceId = dev,
                EventTime = ts,
                ServerReceivedTime = created
            });
        }

        if (items.Count == 0)
            return InvariantResult.Unknown("INV-P19 UNKNOWN: pc_aw_events 表为空");

        return DataReliabilityInvariants.CheckS5_ClockTrustworthy(items, options, referenceTimeUtc: nowUtc);
    }

    private async Task<InvariantResult> CheckS6Async(DbConnection conn, InvariantOptions options, DateTime nowUtc, CancellationToken ct)
    {
        if (!await TableExistsAsync(conn, "pc_aw_events", ct))
            return InvariantResult.Unknown("INV-P20 UNKNOWN: 数据表 pc_aw_events 不存在");

        await using var cmd = conn.CreateCommand();
        cmd.CommandTimeout = 15;
        cmd.CommandText = """
            SELECT timestamp, created_at 
            FROM pc_aw_events 
            WHERE event_type = 'window'
            ORDER BY timestamp DESC 
            LIMIT 500;
            """;

        var times = new List<DateTime>();
        var lags = new List<(DateTime, DateTime)>();
        await using (var reader = await cmd.ExecuteReaderAsync(ct))
        {
            while (await reader.ReadAsync(ct))
            {
                DateTime ts = reader.GetDateTime(0);
                DateTime created = reader.GetDateTime(1);
                times.Add(ts);
                lags.Add((ts, created));
            }
        }

        if (times.Count == 0)
            return InvariantResult.Unknown("INV-P20 UNKNOWN: 无事件记录检验下线声明与上传延迟");

        var trace = new DeviceActivityTrace
        {
            DeviceId = "default",
            EventTimes = times,
            Declarations = Array.Empty<OfflineDeclaration>(),
            UploadLagSamples = lags
        };

        return DataReliabilityInvariants.CheckS6_OfflineDeclared(trace, options);
    }

    private async Task<InvariantResult> CheckS7Async(DbConnection conn, InvariantOptions options, DateTime nowUtc, CancellationToken ct)
    {
        if (!await TableExistsAsync(conn, "pc_aw_events", ct))
            return InvariantResult.Unknown("INV-P21 UNKNOWN: 数据表 pc_aw_events 不存在");

        await using var cmd = conn.CreateCommand();
        cmd.CommandTimeout = 15;
        cmd.CommandText = """
            SELECT device_id, timestamp, timestamp + (duration || ' seconds')::interval as end_time, event_type, afk_status
            FROM pc_aw_events
            WHERE event_type = 'window'
            ORDER BY timestamp ASC
            LIMIT 500;
            """;

        var intervals = new List<TimelineInterval>();
        await using var reader = await cmd.ExecuteReaderAsync(ct);
        while (await reader.ReadAsync(ct))
        {
            string dev = reader.IsDBNull(0) ? "default" : reader.GetString(0);
            DateTime start = reader.GetDateTime(1);
            DateTime end = reader.GetDateTime(2);
            string type = reader.IsDBNull(3) ? "window" : reader.GetString(3);
            string? afk = reader.IsDBNull(4) ? null : reader.GetString(4);

            bool isGap = type.Equals("gap", StringComparison.OrdinalIgnoreCase) ||
                         type.Equals("afk", StringComparison.OrdinalIgnoreCase) ||
                         (afk != null && afk.Equals("afk", StringComparison.OrdinalIgnoreCase));

            intervals.Add(new TimelineInterval
            {
                DeviceId = dev,
                StartTime = start,
                EndTime = end,
                EventType = type,
                IsGap = isGap
            });
        }

        if (intervals.Count == 0)
            return InvariantResult.Unknown("INV-P21 UNKNOWN: 无时间线区间可检验断档标记");

        return DataReliabilityInvariants.CheckS7_TimelineGapMarked(intervals, options);
    }

    private async Task<InvariantResult> CheckS8Async(DbConnection conn, InvariantOptions options, DateTime nowUtc, CancellationToken ct)
    {
        if (!await TableExistsAsync(conn, "pc_keystats_samples", ct))
            return InvariantResult.Unknown("INV-C19 UNKNOWN: 数据表 pc_keystats_samples 不存在");

        await using var cmd = conn.CreateCommand();
        cmd.CommandTimeout = 15;
        cmd.CommandText = """
            SELECT sampled_at_utc, stats_date::text 
            FROM pc_keystats_samples 
            ORDER BY id DESC 
            LIMIT 200;
            """;

        var samples = new List<DayBoundarySample>();
        await using var reader = await cmd.ExecuteReaderAsync(ct);
        while (await reader.ReadAsync(ct))
        {
            DateTime sampledAtUtc = reader.GetDateTime(0);
            string statsDateStr = reader.GetString(1);
            string expectedBizDay = DataReliabilityInvariants.ComputeBusinessDayString(sampledAtUtc);

            samples.Add(new DayBoundarySample
            {
                EventTimeUtc = sampledAtUtc,
                DataFieldDateBucket = statsDateStr,
                QueryWindowDate = expectedBizDay,
                PageDisplayDate = expectedBizDay
            });
        }

        if (samples.Count == 0)
            return InvariantResult.Unknown("INV-C19 UNKNOWN: 无样本检验日界一致性");

        return DataReliabilityInvariants.CheckS8_DayBoundaryConsistent(samples, options);
    }

    private async Task<InvariantResult> CheckS9Async(DbConnection conn, InvariantOptions options, DateTime nowUtc, CancellationToken ct)
    {
        if (!await TableExistsAsync(conn, "pc_aw_events", ct))
            return InvariantResult.Unknown("INV-C20 UNKNOWN: 数据表 pc_aw_events 不存在");

        await using var cmd = conn.CreateCommand();
        cmd.CommandTimeout = 15;
        cmd.CommandText = "SELECT COALESCE(SUM(duration), 0) FROM pc_aw_events WHERE timestamp >= (NOW() - interval '24 hours');";
        var scalar = await cmd.ExecuteScalarAsync(ct);
        double validDuration = Convert.ToDouble(scalar ?? 0);

        string reportedStatus = "Normal";
        if (await TableExistsAsync(conn, "endpoint_statuses", ct))
        {
            await using var statCmd = conn.CreateCommand();
            statCmd.CommandTimeout = 5;
            statCmd.CommandText = "SELECT upload_status FROM endpoint_statuses ORDER BY updated_at DESC LIMIT 1;";
            var statusVal = await statCmd.ExecuteScalarAsync(ct);
            if (statusVal != null && statusVal != DBNull.Value)
            {
                reportedStatus = statusVal.ToString()!;
            }
        }

        var report = new CoverageSignalReport
        {
            DeviceId = "default",
            OnlineDurationSeconds = 86400.0,
            ValidDataDurationSeconds = validDuration,
            ReportedStatus = reportedStatus
        };

        return DataReliabilityInvariants.CheckS9_GapHasSignal(report, options);
    }

    private async Task<InvariantResult> CheckS10Async(DbConnection conn, InvariantOptions options, DateTime nowUtc, CancellationToken ct)
    {
        var runs = new List<BackgroundTaskRun>();

        // 检查分类快照任务：生产环境基准验证是否发生静默空转（存在未分类事件业务日但补齐产出为 0）
        if (await TableExistsAsync(conn, "pc_aw_events", ct) && await TableExistsAsync(conn, "pc_activity_classifications", ct))
        {
            await using var cmd = conn.CreateCommand();
            cmd.CommandTimeout = 15;
            cmd.CommandText = """
                SELECT count(*) as unclassified_days, COALESCE(sum(aw_cnt), 0) as unclassified_events
                FROM (
                    SELECT date_trunc('day', timestamp AT TIME ZONE 'Asia/Shanghai') as day, count(*) as aw_cnt
                    FROM pc_aw_events
                    WHERE duration > 0
                    GROUP BY 1
                ) aw
                LEFT JOIN (
                    SELECT date_trunc('day', started_at AT TIME ZONE 'Asia/Shanghai') as day, count(*) as cls_cnt
                    FROM pc_activity_classifications
                    GROUP BY 1
                ) cls ON aw.day = cls.day
                WHERE COALESCE(cls.cls_cnt, 0) = 0;
                """;

            long unclassifiedDays = 0;
            long unclassifiedEvents = 0;
            await using (var reader = await cmd.ExecuteReaderAsync(ct))
            {
                if (await reader.ReadAsync(ct))
                {
                    unclassifiedDays = reader.GetInt64(0);
                    unclassifiedEvents = reader.GetInt64(1);
                }
            }

            runs.Add(new BackgroundTaskRun
            {
                TaskName = "pc_classification_backfill",
                ExecutedAt = nowUtc,
                AvailableDataCount = (int)Math.Min(int.MaxValue, unclassifiedEvents),
                ProcessedCount = 0,
                OutputCount = 0
            });
        }

        // 检查手机汇总任务
        if (await TableExistsAsync(conn, "mobile_usage_events", ct) && await TableExistsAsync(conn, "mobile_usage_summaries", ct))
        {
            await using var cmd1 = conn.CreateCommand();
            cmd1.CommandTimeout = 10;
            cmd1.CommandText = "SELECT count(*) FROM mobile_usage_events WHERE created_at >= (NOW() - interval '14 days');";
            long mobEvents = Convert.ToInt64(await cmd1.ExecuteScalarAsync(ct) ?? 0);

            await using var cmd2 = conn.CreateCommand();
            cmd2.CommandTimeout = 10;
            cmd2.CommandText = "SELECT count(*) FROM mobile_usage_summaries WHERE created_at >= (NOW() - interval '14 days');";
            long mobSummaries = Convert.ToInt64(await cmd2.ExecuteScalarAsync(ct) ?? 0);

            runs.Add(new BackgroundTaskRun
            {
                TaskName = "mobile_usage_summaries",
                ExecutedAt = nowUtc,
                AvailableDataCount = (int)Math.Min(int.MaxValue, mobEvents),
                OutputCount = (int)Math.Min(int.MaxValue, mobSummaries)
            });
        }

        if (runs.Count == 0)
            return InvariantResult.Unknown("INV-C21 UNKNOWN: 相关后台任务数据表不存在");

        return DataReliabilityInvariants.CheckS10_TaskHasOutput(runs, options);
    }

    private async Task<InvariantResult> CheckS11Async(DbConnection conn, InvariantOptions options, DateTime nowUtc, CancellationToken ct)
    {
        if (!await TableExistsAsync(conn, "mobile_sync_batches", ct))
            return InvariantResult.Unknown("INV-M21 UNKNOWN: 数据表 mobile_sync_batches 不存在");

        await using var cmd = conn.CreateCommand();
        cmd.CommandTimeout = 15;
        cmd.CommandText = """
            SELECT batch_id, status, failed_count, accepted_count 
            FROM mobile_sync_batches 
            ORDER BY created_at DESC 
            LIMIT 500;
            """;

        var batches = new List<BatchSyncStatusRecord>();
        await using var reader = await cmd.ExecuteReaderAsync(ct);
        while (await reader.ReadAsync(ct))
        {
            string batchId = reader.GetString(0);
            string status = reader.GetString(1);
            int failed = reader.GetInt32(2);
            int accepted = reader.GetInt32(3);
            batches.Add(new BatchSyncStatusRecord
            {
                BatchId = batchId,
                Status = status,
                AcceptedCount = accepted,
                FailedCount = failed,
                RejectedCount = 0,
                TotalCount = accepted + failed
            });
        }

        if (batches.Count == 0)
            return InvariantResult.Unknown("INV-M21 UNKNOWN: mobile_sync_batches 表为空");

        return DataReliabilityInvariants.CheckS11_StatusSemantics(batches, options);
    }

    private async Task<InvariantResult> CheckS12Async(DbConnection conn, InvariantOptions options, DateTime nowUtc, CancellationToken ct)
    {
        bool hasBlocks = await TableExistsAsync(conn, "mobile_timeline_blocks", ct);
        bool hasAggs = await TableExistsAsync(conn, "mobile_usage_aggregates", ct);
        if (!hasBlocks || !hasAggs)
            return InvariantResult.Unknown("INV-M22 UNKNOWN: 派生表 mobile_timeline_blocks 或 mobile_usage_aggregates 不存在");

        int sourceCount = 0;
        if (await TableExistsAsync(conn, "mobile_usage_events", ct))
        {
            await using var scmd = conn.CreateCommand();
            scmd.CommandTimeout = 10;
            scmd.CommandText = "SELECT count(*) FROM mobile_usage_events WHERE created_at >= (NOW() - interval '24 hours');";
            sourceCount = Convert.ToInt32(await scmd.ExecuteScalarAsync(ct) ?? 0);
            if (sourceCount == 0)
            {
                // 若最近24h无新增，检查全表是否有数据
                await using var allCmd = conn.CreateCommand();
                allCmd.CommandTimeout = 10;
                allCmd.CommandText = "SELECT count(*) FROM (SELECT 1 FROM mobile_usage_events LIMIT 10) t;";
                sourceCount = Convert.ToInt32(await allCmd.ExecuteScalarAsync(ct) ?? 0);
            }
        }

        await using var bcmd = conn.CreateCommand();
        bcmd.CommandTimeout = 10;
        bcmd.CommandText = "SELECT count(*) FROM mobile_timeline_blocks;";
        int blocksCount = Convert.ToInt32(await bcmd.ExecuteScalarAsync(ct) ?? 0);

        await using var acmd = conn.CreateCommand();
        acmd.CommandTimeout = 10;
        acmd.CommandText = "SELECT count(*) FROM mobile_usage_aggregates;";
        int aggsCount = Convert.ToInt32(await acmd.ExecuteScalarAsync(ct) ?? 0);

        var statuses = new List<DerivedTableStatus>
        {
            new()
            {
                TableName = "mobile_timeline_blocks",
                SourceDataCountLast24H = sourceCount,
                DerivedRowCount = blocksCount,
                IsExplicitOnlineCalculation = false
            },
            new()
            {
                TableName = "mobile_usage_aggregates",
                SourceDataCountLast24H = sourceCount,
                DerivedRowCount = aggsCount,
                IsExplicitOnlineCalculation = false
            }
        };

        return DataReliabilityInvariants.CheckS12_DerivedTableActive(statuses, options);
    }

    private async Task<InvariantResult> CheckS13Async(DbConnection conn, InvariantOptions options, DateTime nowUtc, CancellationToken ct)
    {
        if (!await TableExistsAsync(conn, "pc_aw_events", ct))
            return InvariantResult.Unknown("INV-P22 UNKNOWN: 数据表 pc_aw_events 不存在");

        await using var cmd = conn.CreateCommand();
        cmd.CommandTimeout = 15;
        cmd.CommandText = """
            SELECT device_id, timestamp, bucket_id
            FROM pc_aw_events
            WHERE event_type = 'window'
            ORDER BY timestamp DESC
            LIMIT 200;
            """;

        var heartbeats = new List<CollectionHeartbeat>();
        await using var reader = await cmd.ExecuteReaderAsync(ct);
        while (await reader.ReadAsync(ct))
        {
            string dev = reader.IsDBNull(0) ? "default" : reader.GetString(0);
            DateTime ts = reader.GetDateTime(1);
            string bucketId = reader.IsDBNull(2) ? "default" : reader.GetString(2);
            heartbeats.Add(new CollectionHeartbeat
            {
                DeviceId = dev,
                Timestamp = ts,
                InstanceId = bucketId
            });
        }

        if (heartbeats.Count == 0)
            return InvariantResult.Unknown("INV-P22 UNKNOWN: 无采集流数据可检验多实例冲突");

        return DataReliabilityInvariants.CheckS13_SingleInstance(heartbeats, options);
    }

    #endregion

    #region Helpers

    private static async Task<bool> TableExistsAsync(DbConnection conn, string tableName, CancellationToken ct)
    {
        try
        {
            await using var cmd = conn.CreateCommand();
            cmd.CommandTimeout = 5;
            cmd.CommandText = $"SELECT 1 FROM \"{tableName}\" LIMIT 0;";
            await cmd.ExecuteNonQueryAsync(ct);
            return true;
        }
        catch
        {
            return false;
        }
    }

    private static async Task ExecuteCheckSafeAsync(
        string key,
        Func<Task<InvariantResult>> checkFunc,
        Dictionary<string, string> details,
        Action<InvariantResult, string> recordAction)
    {
        try
        {
            var result = await checkFunc();
            recordAction(result, key);
        }
        catch (Exception ex)
        {
            var unknown = InvariantResult.Unknown($"{key} UNKNOWN: 取数执行异常: {ex.Message}");
            recordAction(unknown, key);
        }
    }

    private static void RecordCheck(
        string key,
        InvariantResult result,
        Dictionary<string, string> details,
        ref int redCount,
        ref int yellowCount,
        ref int greenCount,
        ref int unknownCount,
        ref int totalIssues)
    {
        switch (result.Status)
        {
            case InvariantStatus.Fail:
                redCount++;
                totalIssues += Math.Max(1, result.TotalViolations);
                details[key] = $"🔴 FAIL: {result.Detail}";
                break;
            case InvariantStatus.Warning:
                yellowCount++;
                totalIssues += Math.Max(1, result.TotalViolations);
                details[key] = $"🟡 WARN: {result.Detail}";
                break;
            case InvariantStatus.Unknown:
                unknownCount++;
                totalIssues++;
                details[key] = $"⚪ UNKNOWN: {result.Detail}";
                break;
            case InvariantStatus.Pass:
            default:
                greenCount++;
                details[key] = $"🟢 PASS: {result.Detail}";
                break;
        }
    }

    private static void RecordUnknown(
        string key,
        string reason,
        Dictionary<string, string> details,
        ref int unknownCount,
        ref int totalIssues)
    {
        unknownCount++;
        totalIssues++;
        details[key] = $"⚪ UNKNOWN: {reason}";
    }

    private static string GetInvariantKey(int index) => index switch
    {
        1 => "S1_INV-P16",
        2 => "S2_INV-P17",
        3 => "S3_INV-P18",
        4 => "S4_INV-C18",
        5 => "S5_INV-P19",
        6 => "S6_INV-P20",
        7 => "S7_INV-P21",
        8 => "S8_INV-C19",
        9 => "S9_INV-C20",
        10 => "S10_INV-C21",
        11 => "S11_INV-M21",
        12 => "S12_INV-M22",
        13 => "S13_INV-P22",
        _ => $"S{index}"
    };

    #endregion
}
