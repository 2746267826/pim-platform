using System;
using System.Collections.Generic;
using System.Linq;

namespace Pim.Core.Invariants;

/// <summary>
/// 数据可信度 13 根基准尺子（S1–S13）生产实现。
/// 全部满足纯函数约定，无数据库/网络依赖，供 CI 与体检服务共用。
/// </summary>
public static class DataReliabilityInvariants
{
    private static readonly TimeZoneInfo ShanghaiTimeZone = ResolveShanghaiTimeZone();

    private static TimeZoneInfo ResolveShanghaiTimeZone()
    {
        try { return TimeZoneInfo.FindSystemTimeZoneById("Asia/Shanghai"); }
        catch (TimeZoneNotFoundException) { return TimeZoneInfo.FindSystemTimeZoneById("China Standard Time"); }
        catch (InvalidTimeZoneException) { return TimeZoneInfo.FindSystemTimeZoneById("China Standard Time"); }
    }

    #region S1–S5: 尺子组一 · 数据自洽

    /// <summary>
    /// S1 (INV-P16): 同类型事件不重叠
    /// 判据: 同设备、同事件类型的事件区间两两不相交（不存在 A.start &lt; B.end &amp;&amp; B.start &lt; A.end）。
    /// 阈值: 重叠对数 = 0（针对新增数据；存量走黄线）。
    /// 为什么是这个阈值: 同一设备在同一时刻不可能产生两个同级别的互斥前台焦点或互斥状态，重叠说明采集端或入库去重损坏。
    /// </summary>
    public static InvariantResult CheckS1_NoOverlap(
        IEnumerable<EventTimeSpan> events,
        InvariantOptions? options = null,
        DateTime? referenceTimeUtc = null)
    {
        var (opt, fallback, note) = InvariantOptions.Resolve(options);
        var now = referenceTimeUtc ?? DateTime.UtcNow;
        var cutoff = now.AddHours(-opt.RecentWindowHours);

        var list = events.ToList();
        var groups = list.GroupBy(e => (e.DeviceId, e.EventType));

        int totalViolations = 0;
        int newViolations = 0;
        int historicalViolations = 0;
        var samples = new List<string>();
        DateTime? earliest = null;
        DateTime? latest = null;

        foreach (var group in groups)
        {
            var sorted = group.OrderBy(e => e.StartTime).ThenBy(e => e.EndTime).ToList();
            for (int i = 0; i < sorted.Count; i++)
            {
                for (int j = i + 1; j < sorted.Count; j++)
                {
                    var a = sorted[i];
                    var b = sorted[j];

                    // 由于已按 StartTime 排序，若 b.StartTime >= a.EndTime 则与 a 不可能再相交
                    if (b.StartTime >= a.EndTime)
                        break;

                    if (a.StartTime < b.EndTime && b.StartTime < a.EndTime)
                    {
                        totalViolations++;
                        var overlapEnd = a.EndTime < b.EndTime ? a.EndTime : b.EndTime;
                        bool isNew = overlapEnd >= cutoff;
                        if (isNew) newViolations++; else historicalViolations++;

                        earliest = earliest == null || a.StartTime < earliest ? a.StartTime : earliest;
                        latest = latest == null || overlapEnd > latest ? overlapEnd : latest;

                        if (samples.Count < opt.MaxSampleCount)
                        {
                            samples.Add($"Device={a.DeviceId}, Type={a.EventType}: [{a.StartTime:yyyy-MM-dd HH:mm:ss} ~ {a.EndTime:yyyy-MM-dd HH:mm:ss}] overlaps with [{b.StartTime:yyyy-MM-dd HH:mm:ss} ~ {b.EndTime:yyyy-MM-dd HH:mm:ss}] (New={isNew})");
                        }
                    }
                }
            }
        }

        if (totalViolations > 0)
        {
            return InvariantResult.Failure(
                $"INV-P16 FAIL: 检测到 {totalViolations} 对同类型事件重叠 (新增 {newViolations}, 存量 {historicalViolations})",
                totalViolations,
                newViolations,
                historicalViolations,
                samples,
                earliest,
                latest,
                note,
                fallback);
        }

        return InvariantResult.Success("INV-P16 PASS: 无同类型事件重叠", note, fallback);
    }

    /// <summary>
    /// S2 (INV-P17): 超长事件必须拿得出活动证据（三态判定）
    /// 判据: 任何 > 30 分钟 (T1b) 的事件，其区间内必须命中以下三态之一：
    ///   1. 操作活跃: 键鼠输入密度 &gt;= 1 次/分钟 (T1a) —— (按键增量 + 点击增量) ÷ 时长(分钟)
    ///   2. 观看活跃: 区间内存在媒体活动 (is_media_active / audible)
    ///   3. 明确的空档: 事件类型为"缺数据"类 (gap, shutdown, sleep 等)，它本来就声明"这里没有人"
    ///   三者都不满足 ⇒ 判「疑似未收尾」，必须显式标记，且不得计入活跃时长。
    /// 阈值: 超长线 30.0 分钟 (T1b)，输入密度线 1.0 次/分钟 (T1a)。
    /// 为什么是这个阈值: 真实人类操作或媒体播放即使挂机也会有心跳或媒体活动，超过 30m 没有任何信号大概率是采集端未收到退出事件造成的僵尸时长。
    /// </summary>
    public static InvariantResult CheckS2_OverlongEventEvidence(
        IEnumerable<LongEventCandidate> events,
        InvariantOptions? options = null,
        DateTime? referenceTimeUtc = null)
    {
        var (opt, fallback, note) = InvariantOptions.Resolve(options);
        var now = referenceTimeUtc ?? DateTime.UtcNow;
        var cutoff = now.AddHours(-opt.RecentWindowHours);

        int totalViolations = 0;
        int newViolations = 0;
        int historicalViolations = 0;
        var samples = new List<string>();
        DateTime? earliest = null;
        DateTime? latest = null;

        foreach (var e in events)
        {
            var durationMinutes = (e.EndTime - e.StartTime).TotalMinutes;
            if (durationMinutes <= opt.LongEventThresholdMinutes)
            {
                continue; // 不属于超长事件，通过
            }

            // 三态判定
            // 态 3: 明确的空档
            if (e.IsGapOrOffline ||
                e.EventType.Contains("gap", StringComparison.OrdinalIgnoreCase) ||
                e.EventType.Contains("sleep", StringComparison.OrdinalIgnoreCase) ||
                e.EventType.Contains("shutdown", StringComparison.OrdinalIgnoreCase) ||
                e.EventType.Contains("offline", StringComparison.OrdinalIgnoreCase))
            {
                continue;
            }

            // 态 1: 操作活跃
            double inputDensity = durationMinutes > 0 ? (e.Keystrokes + e.MouseClicks) / durationMinutes : 0;
            if (inputDensity >= opt.MinInputDensityPerMinute)
            {
                continue;
            }

            // 态 2: 观看活跃
            if (e.IsMediaActive || e.IsAudible)
            {
                continue;
            }

            // 都不满足：判「疑似未收尾」
            totalViolations++;
            bool isNew = e.EndTime >= cutoff;
            if (isNew) newViolations++; else historicalViolations++;

            earliest = earliest == null || e.StartTime < earliest ? e.StartTime : earliest;
            latest = latest == null || e.EndTime > latest ? e.EndTime : latest;

            if (samples.Count < opt.MaxSampleCount)
            {
                samples.Add($"Device={e.DeviceId}, Event={e.EventId ?? e.EventType}, App={e.AppName ?? "N/A"}, Duration={durationMinutes:F1}m > {opt.LongEventThresholdMinutes:F1}m: 疑似未收尾 (无操作密度[{inputDensity:F2}/min < {opt.MinInputDensityPerMinute:F1}], 无媒体活动, 非明确空档)");
            }
        }

        if (totalViolations > 0)
        {
            return InvariantResult.Failure(
                $"INV-P17 FAIL: 检测到 {totalViolations} 个超长事件疑似未收尾 (新增 {newViolations}, 存量 {historicalViolations})",
                totalViolations,
                newViolations,
                historicalViolations,
                samples,
                earliest,
                latest,
                note,
                fallback);
        }

        return InvariantResult.Success("INV-P17 PASS: 所有超长事件均具备三态证据或明确声明", note, fallback);
    }

    /// <summary>
    /// S3 (INV-P18): 单日时长有界
    /// 判据: 单日"活跃合计"（仅三态中的前两态，排除疑似未收尾和空档）&lt;= 清醒窗口 × 90%；硬上限 24h。
    /// 阈值: 硬上限 24.0h (86400s)；清醒窗口默认 16h → 警告线 14.4h (T5)。
    /// 为什么是这个阈值: 一天物理上限只有 24 小时，单日超过 24h 必定数据造假或重叠累加；超过清醒窗口 90% 提示不符合人类生理节律。
    /// </summary>
    public static InvariantResult CheckS3_DailyDurationBounded(
        IEnumerable<DailyActiveDuration> dailyDurations,
        InvariantOptions? options = null)
    {
        var (opt, fallback, note) = InvariantOptions.Resolve(options);
        var hardCapSeconds = opt.MaxDailyActiveHours * 3600.0;
        var warningSeconds = opt.AwakeWindowHours * opt.AwakeWindowWarningRatio * 3600.0;

        int totalViolations = 0;
        int warningCount = 0;
        var samples = new List<string>();

        foreach (var d in dailyDurations)
        {
            if (d.ActiveDurationSeconds > hardCapSeconds)
            {
                totalViolations++;
                if (samples.Count < opt.MaxSampleCount)
                {
                    samples.Add($"Date={d.Date}, Device={d.DeviceId}: ActiveDuration={d.ActiveDurationSeconds / 3600.0:F2}h 超过单日硬上限 {opt.MaxDailyActiveHours:F1}h");
                }
            }
            else if (d.ActiveDurationSeconds > warningSeconds)
            {
                warningCount++;
                if (samples.Count < opt.MaxSampleCount)
                {
                    samples.Add($"[Warning] Date={d.Date}, Device={d.DeviceId}: ActiveDuration={d.ActiveDurationSeconds / 3600.0:F2}h 超过清醒窗口警告线 {warningSeconds / 3600.0:F2}h");
                }
            }
        }

        if (totalViolations > 0)
        {
            return InvariantResult.Failure(
                $"INV-P18 FAIL: 检测到 {totalViolations} 个单日活跃时长超过硬上限 {opt.MaxDailyActiveHours:F1}h",
                totalViolations,
                totalViolations,
                0,
                samples,
                null,
                null,
                note,
                fallback);
        }

        string passMsg = warningCount > 0
            ? $"INV-P18 PASS: 单日时长未超硬上限 (存在 {warningCount} 天超过清醒窗口警告线 {warningSeconds / 3600.0:F1}h)"
            : "INV-P18 PASS: 单日时长符合生理与物理上限";

        return InvariantResult.Success(passMsg, note, fallback);
    }

    /// <summary>
    /// S4 (INV-C18): 业务键唯一（不重复）
    /// 判据:
    ///   定位: (device, recorded_at, lat, lon) 唯一
    ///   手机事件: (device, package, event_time, event_type) 唯一
    ///   PC 事件: (device, timestamp, duration, event_type, app_name, browser, instance_id) 唯一
    /// 阈值: 新增重复行 = 0（存量走黄线）。
    /// 为什么是这个阈值: 重复事件会导致时长与频次双重虚高，破坏聚合指标的可信度。
    /// </summary>
    public static InvariantResult CheckS4_BusinessKeyUnique(
        IEnumerable<BusinessRecordKey> records,
        InvariantOptions? options = null,
        DateTime? referenceTimeUtc = null)
    {
        var (opt, fallback, note) = InvariantOptions.Resolve(options);
        var now = referenceTimeUtc ?? DateTime.UtcNow;
        var cutoff = now.AddHours(-opt.RecentWindowHours);

        var list = records.ToList();
        var groups = list.GroupBy(r => (r.Domain, r.UniqueKey));

        int totalViolations = 0;
        int newViolations = 0;
        int historicalViolations = 0;
        var samples = new List<string>();
        DateTime? earliest = null;
        DateTime? latest = null;

        foreach (var g in groups)
        {
            var count = g.Count();
            if (count > 1)
            {
                int dups = count - 1;
                totalViolations += dups;

                foreach (var item in g.Skip(1))
                {
                    bool isNew = item.Timestamp >= cutoff;
                    if (isNew) newViolations++; else historicalViolations++;

                    earliest = earliest == null || item.Timestamp < earliest ? item.Timestamp : earliest;
                    latest = latest == null || item.Timestamp > latest ? item.Timestamp : latest;
                }

                if (samples.Count < opt.MaxSampleCount)
                {
                    var first = g.First();
                    samples.Add($"Domain={first.Domain}, Device={first.DeviceId}, Key={first.UniqueKey}: 重复出现 {count} 次");
                }
            }
        }

        if (totalViolations > 0)
        {
            return InvariantResult.Failure(
                $"INV-C18 FAIL: 检测到 {totalViolations} 行业务键重复 (新增 {newViolations}, 存量 {historicalViolations})",
                totalViolations,
                newViolations,
                historicalViolations,
                samples,
                earliest,
                latest,
                note,
                fallback);
        }

        return InvariantResult.Success("INV-C18 PASS: 业务键唯一无重复", note, fallback);
    }

    /// <summary>
    /// S5 (INV-P19): 时钟可信
    /// 判据: 事件时间戳 &lt;= 服务端接收时间 + 容差。
    /// 阈值: 容差 5.0 分钟 (ClockSkewToleranceMinutes)。
    /// 为什么是这个阈值: 客户端时钟可能与网络授时存在少许偏差或时钟漂移，5 分钟为工业标准网络时间容限；超过 5 分钟属于严重超前或时钟穿越。
    /// </summary>
    public static InvariantResult CheckS5_ClockTrustworthy(
        IEnumerable<ClockEventItem> items,
        InvariantOptions? options = null,
        DateTime? referenceTimeUtc = null)
    {
        var (opt, fallback, note) = InvariantOptions.Resolve(options);
        var now = referenceTimeUtc ?? DateTime.UtcNow;
        var cutoff = now.AddHours(-opt.RecentWindowHours);
        var toleranceSeconds = opt.ClockSkewToleranceMinutes * 60.0;

        int totalViolations = 0;
        int newViolations = 0;
        int historicalViolations = 0;
        var samples = new List<string>();
        DateTime? earliest = null;
        DateTime? latest = null;

        foreach (var item in items)
        {
            var skewSeconds = (item.EventTime - item.ServerReceivedTime).TotalSeconds;
            if (skewSeconds > toleranceSeconds)
            {
                totalViolations++;
                bool isNew = item.ServerReceivedTime >= cutoff;
                if (isNew) newViolations++; else historicalViolations++;

                earliest = earliest == null || item.EventTime < earliest ? item.EventTime : earliest;
                latest = latest == null || item.EventTime > latest ? item.EventTime : latest;

                if (samples.Count < opt.MaxSampleCount)
                {
                    samples.Add($"Device={item.DeviceId}, Event={item.EventId}: EventTime={item.EventTime:yyyy-MM-dd HH:mm:ss} 超前 ReceivedTime={item.ServerReceivedTime:yyyy-MM-dd HH:mm:ss} 达到 {skewSeconds / 60.0:F1}m > 容差 {opt.ClockSkewToleranceMinutes:F1}m");
                }
            }
        }

        if (totalViolations > 0)
        {
            return InvariantResult.Failure(
                $"INV-P19 FAIL: 检测到 {totalViolations} 个事件时钟超前 (新增 {newViolations}, 存量 {historicalViolations})",
                totalViolations,
                newViolations,
                historicalViolations,
                samples,
                earliest,
                latest,
                note,
                fallback);
        }

        return InvariantResult.Success("INV-P19 PASS: 所有事件时间戳均在合理时钟容差范围内", note, fallback);
    }

    #endregion

    #region S6–S9: 尺子组二 · 覆盖完整

    /// <summary>
    /// S6 (INV-P20): 设备必须自己声明下线
    /// 判据:
    ///   1. 空档必须被声明：设备无数据的时间段必须有"正常下线"声明（关机/休眠/planned offline）；没有声明的空档 &gt; 30 分钟 = 红
    ///   2. 上传必须及时：created_at - 事件时间 的 p99 &lt;= 30 分钟 (T2)
    ///   3. 停摆必须可解释：相邻事件间隔 &gt; 30 分钟且未声明下线 = 红
    /// 阈值: 无下线声明空档阈值 30.0 分钟 (T2)，上传滞后 p99 阈值 30.0 分钟 (T2)。
    /// 为什么是这个阈值: 现代操作系统关机与睡眠都有系统钩子；若无声明突然停止 30m，说明采集端崩溃或掉线；上传 p99 超过 30m 表明链路堆积积压严重。
    /// </summary>
    public static InvariantResult CheckS6_OfflineDeclared(
        DeviceActivityTrace trace,
        InvariantOptions? options = null)
    {
        var (opt, fallback, note) = InvariantOptions.Resolve(options);
        var gapThresholdMinutes = opt.UndeclaredOfflineGapMinutes;
        var p99LagMinutesThreshold = opt.MaxUploadLagP99Minutes;

        int totalViolations = 0;
        var samples = new List<string>();

        // 1. 检查事件之间的空档是否被声明覆盖
        var sortedTimes = trace.EventTimes.OrderBy(t => t).ToList();
        for (int i = 0; i < sortedTimes.Count - 1; i++)
        {
            var t1 = sortedTimes[i];
            var t2 = sortedTimes[i + 1];
            var gapMinutes = (t2 - t1).TotalMinutes;
            if (gapMinutes > gapThresholdMinutes)
            {
                // 检查是否有下线声明覆盖该空档的大部分或关键区间
                bool declared = trace.Declarations.Any(d =>
                    d.DeviceId == trace.DeviceId &&
                    d.StartTime <= t1.AddMinutes(5) &&
                    d.EndTime >= t2.AddMinutes(-5));

                if (!declared)
                {
                    totalViolations++;
                    if (samples.Count < opt.MaxSampleCount)
                    {
                        samples.Add($"Device={trace.DeviceId}: [{t1:yyyy-MM-dd HH:mm:ss} ~ {t2:yyyy-MM-dd HH:mm:ss}] 存在 {gapMinutes:F1}m 无声明空档 (> {gapThresholdMinutes:F1}m)");
                    }
                }
            }
        }

        // 2. 检查上传滞后 p99
        if (trace.UploadLagSamples.Count > 0)
        {
            var lags = trace.UploadLagSamples
                .Select(s => (s.CreatedAt - s.EventTime).TotalMinutes)
                .OrderBy(l => l)
                .ToList();

            int p99Index = (int)Math.Ceiling(lags.Count * 0.99) - 1;
            p99Index = Math.Clamp(p99Index, 0, lags.Count - 1);
            var p99 = lags[p99Index];

            if (p99 > p99LagMinutesThreshold)
            {
                totalViolations++;
                if (samples.Count < opt.MaxSampleCount)
                {
                    samples.Add($"Device={trace.DeviceId}: 上传滞后 p99 为 {p99:F1}m > 阈值 {p99LagMinutesThreshold:F1}m");
                }
            }
        }

        if (totalViolations > 0)
        {
            return InvariantResult.Failure(
                $"INV-P20 FAIL: 设备下线声明或上传延迟存在 {totalViolations} 项违规",
                totalViolations,
                totalViolations,
                0,
                samples,
                null,
                null,
                note,
                fallback);
        }

        return InvariantResult.Success("INV-P20 PASS: 设备无声明空档与上传延迟均在指标内", note, fallback);
    }

    /// <summary>
    /// S7 (INV-P21): 断档必须在时间轴上被标记
    /// 判据: 相邻事件之间 &gt; 15 分钟的空洞，必须被"缺数据"类事件（gap 或等价标记）完整覆盖。
    /// 阈值: 未标记空洞 = 0，断档判定阈值 15.0 分钟。
    /// 为什么是这个阈值: 超过 15m 的无数据空洞若在 UI 上直接拼接或无解释空白，用户无法分辨是设备没用还是系统漏记；必须显示 gap 标记。
    /// </summary>
    public static InvariantResult CheckS7_TimelineGapMarked(
        IEnumerable<TimelineInterval> intervals,
        InvariantOptions? options = null)
    {
        var (opt, fallback, note) = InvariantOptions.Resolve(options);
        var thresholdMinutes = opt.TimelineGapThresholdMinutes;

        var list = intervals.OrderBy(i => i.StartTime).ThenBy(i => i.EndTime).ToList();
        int totalViolations = 0;
        var samples = new List<string>();

        for (int i = 0; i < list.Count - 1; i++)
        {
            var a = list[i];
            var b = list[i + 1];

            if (b.StartTime > a.EndTime)
            {
                var holeMinutes = (b.StartTime - a.EndTime).TotalMinutes;
                if (holeMinutes > thresholdMinutes)
                {
                    totalViolations++;
                    if (samples.Count < opt.MaxSampleCount)
                    {
                        samples.Add($"Device={a.DeviceId}: [{a.EndTime:yyyy-MM-dd HH:mm:ss} ~ {b.StartTime:yyyy-MM-dd HH:mm:ss}] 存在 {holeMinutes:F1}m 未标记空洞 (> {thresholdMinutes:F1}m)");
                    }
                }
            }
        }

        if (totalViolations > 0)
        {
            return InvariantResult.Failure(
                $"INV-P21 FAIL: 时间轴上存在 {totalViolations} 处未标记的断档空洞",
                totalViolations,
                totalViolations,
                0,
                samples,
                null,
                null,
                note,
                fallback);
        }

        return InvariantResult.Success("INV-P21 PASS: 所有 >15m 空洞均已妥善标记为 gap 事件", note, fallback);
    }

    /// <summary>
    /// S8 (INV-C19): 日界一致（三层）
    /// 判据: 同一时刻在三处必须归属同一天：
    ///   1. 数据字段的日期桶 (date)
    ///   2. 按日接口的查询窗口 (date=YYYY-MM-DD)
    ///   3. 页面展示的业务日
    ///   统一标准：Asia/Shanghai 业务日，04:00 起算（[D 04:00, D+1 04:00) 为日 D）。
    /// 阈值: 不一致行数 = 0。
    /// 为什么是这个阈值: 跨夜作息（如凌晨 2 点工作）属于前一天的夜间延伸，统一以 04:00 作为界线；三层不一致会导致列表查出但聚合统计丢失。
    /// </summary>
    public static InvariantResult CheckS8_DayBoundaryConsistent(
        IEnumerable<DayBoundarySample> samples,
        InvariantOptions? options = null)
    {
        var (opt, fallback, note) = InvariantOptions.Resolve(options);

        int totalViolations = 0;
        var violationSamples = new List<string>();

        foreach (var s in samples)
        {
            var expectedBusinessDay = ComputeBusinessDayString(s.EventTimeUtc);
            bool b1 = s.DataFieldDateBucket == expectedBusinessDay;
            bool b2 = s.QueryWindowDate == expectedBusinessDay;
            bool b3 = s.PageDisplayDate == expectedBusinessDay;

            if (!b1 || !b2 || !b3)
            {
                totalViolations++;
                if (violationSamples.Count < opt.MaxSampleCount)
                {
                    violationSamples.Add($"EventUtc={s.EventTimeUtc:yyyy-MM-dd HH:mm:ss}, Expected={expectedBusinessDay} | Field={s.DataFieldDateBucket}({b1}), Query={s.QueryWindowDate}({b2}), Page={s.PageDisplayDate}({b3})");
                }
            }
        }

        if (totalViolations > 0)
        {
            return InvariantResult.Failure(
                $"INV-C19 FAIL: 检测到 {totalViolations} 处日界归属三层不一致 (标准: Asia/Shanghai 04:00 起算)",
                totalViolations,
                totalViolations,
                0,
                violationSamples,
                null,
                null,
                note,
                fallback);
        }

        return InvariantResult.Success("INV-C19 PASS: 数据桶、接口窗口与页面展示三层日界严格一致", note, fallback);
    }

    /// <summary>
    /// 计算 UTC 时间在 Asia/Shanghai 04:00 起算的业务日字符串 (YYYY-MM-DD)
    /// </summary>
    public static string ComputeBusinessDayString(DateTime utcTime)
    {
        var local = TimeZoneInfo.ConvertTimeFromUtc(DateTime.SpecifyKind(utcTime, DateTimeKind.Utc), ShanghaiTimeZone);
        var businessDate = local.Hour < 4 ? local.Date.AddDays(-1) : local.Date;
        return businessDate.ToString("yyyy-MM-dd");
    }

    /// <summary>
    /// S9 (INV-C20): 有缺口必有信号
    /// 判据: 覆盖率低于红线时必须报"警告 / 红"，绝不得报"正常"。
    /// 阈值: 覆盖率 = 有效数据时长 ÷ 设备在线时长；&lt; 95% 红、&lt; 99% 黄 (T6)。
    /// 为什么是这个阈值: 95% 覆盖率是个人生活记录可信度的基准底线；低覆盖率若显示"正常"属于静默掩盖故障。
    /// </summary>
    public static InvariantResult CheckS9_GapHasSignal(
        CoverageSignalReport report,
        InvariantOptions? options = null)
    {
        var (opt, fallback, note) = InvariantOptions.Resolve(options);

        double ratio = report.OnlineDurationSeconds > 0
            ? report.ValidDataDurationSeconds / report.OnlineDurationSeconds
            : 1.0;

        bool isNormal = report.ReportedStatus.Equals("Normal", StringComparison.OrdinalIgnoreCase) ||
                        report.ReportedStatus.Equals("Healthy", StringComparison.OrdinalIgnoreCase) ||
                        report.ReportedStatus.Equals("OK", StringComparison.OrdinalIgnoreCase);

        // 覆盖率 < 95% 必须报红
        if (ratio < opt.CoverageRedRatio)
        {
            if (isNormal)
            {
                return InvariantResult.Failure(
                    $"INV-C20 FAIL: 设备 {report.DeviceId} 覆盖率仅为 {ratio:P1} (< 红线 {opt.CoverageRedRatio:P0})，但报告状态却为 '{report.ReportedStatus}' (必须为红/错误)",
                    1,
                    1,
                    0,
                    new[] { $"Device={report.DeviceId}: Coverage={ratio:P1}, ReportedStatus={report.ReportedStatus}" },
                    null,
                    null,
                    note,
                    fallback);
            }
        }
        else if (ratio < opt.CoverageYellowRatio)
        {
            // 覆盖率在 95%~99% 之间，必须至少报黄/警告，不得报完全正常
            if (isNormal)
            {
                return InvariantResult.Failure(
                    $"INV-C20 FAIL: 设备 {report.DeviceId} 覆盖率为 {ratio:P1} (< 黄线 {opt.CoverageYellowRatio:P0})，但报告状态为 '{report.ReportedStatus}' (必须报警告/黄线)",
                    1,
                    1,
                    0,
                    new[] { $"Device={report.DeviceId}: Coverage={ratio:P1}, ReportedStatus={report.ReportedStatus}" },
                    null,
                    null,
                    note,
                    fallback);
            }
        }

        return InvariantResult.Success($"INV-C20 PASS: 覆盖率 {ratio:P1} 与健康信号 '{report.ReportedStatus}' 自洽", note, fallback);
    }

    #endregion

    #region S10–S13: 尺子组三 · 链路健康

    /// <summary>
    /// S10 (INV-C21): 后台任务必须有产出（静默失败专治）
    /// 判据: 分类快照补齐、汇总入库、派生表构建等后台任务，在最近一个执行窗口内产出必须 &gt; 0；产出 0 且存在可处理数据 = 红。
    /// 阈值: 产出 0 且可处理数据 &gt; 0 = 红 (产出 0 违规)。
    /// 为什么是这个阈值: 后台任务常因未捕获异常退出、无限等待或查询条件脱节导致空转，空转产出 0 却报成功属于致命静默失败。
    /// </summary>
    public static InvariantResult CheckS10_TaskHasOutput(
        IEnumerable<BackgroundTaskRun> runs,
        InvariantOptions? options = null)
    {
        var (opt, fallback, note) = InvariantOptions.Resolve(options);

        int totalViolations = 0;
        var samples = new List<string>();

        foreach (var r in runs)
        {
            if (r.AvailableDataCount > 0 && r.OutputCount == 0)
            {
                totalViolations++;
                if (samples.Count < opt.MaxSampleCount)
                {
                    samples.Add($"Task={r.TaskName}, ExecutedAt={r.ExecutedAt:yyyy-MM-dd HH:mm:ss}: 可处理数据 {r.AvailableDataCount} 条但产出 0 行 (静默空转)");
                }
            }
        }

        if (totalViolations > 0)
        {
            return InvariantResult.Failure(
                $"INV-C21 FAIL: 检测到 {totalViolations} 个后台任务存在可处理数据但产出为 0 行",
                totalViolations,
                totalViolations,
                0,
                samples,
                null,
                null,
                note,
                fallback);
        }

        return InvariantResult.Success("INV-C21 PASS: 后台任务均有真实产出或无待处理数据", note, fallback);
    }

    /// <summary>
    /// S11 (INV-M21): 状态语义自洽
    /// 判据: failed_count = 0 的批次不得处于"失败"或"completed-with-errors"状态；非 completed 状态必须能解释（有 failed 或明确的 rejected 语义）。
    /// 阈值: 语义不自洽行数 = 0。
    /// 为什么是这个阈值: 正常业务去重导致被拒 (rejected) 不属于系统故障，混淆 rejected 和 failed 会导致报警风暴或排障误判。
    /// </summary>
    public static InvariantResult CheckS11_StatusSemantics(
        IEnumerable<BatchSyncStatusRecord> batches,
        InvariantOptions? options = null)
    {
        var (opt, fallback, note) = InvariantOptions.Resolve(options);

        int totalViolations = 0;
        var samples = new List<string>();

        foreach (var b in batches)
        {
            bool isFailedStatus = b.Status.Equals("failed", StringComparison.OrdinalIgnoreCase) ||
                                 b.Status.Equals("completed-with-errors", StringComparison.OrdinalIgnoreCase);

            // 1. 无真正失败却标为失败
            if (b.FailedCount == 0 && isFailedStatus)
            {
                totalViolations++;
                if (samples.Count < opt.MaxSampleCount)
                {
                    samples.Add($"Batch={b.BatchId}: FailedCount=0, RejectedCount={b.RejectedCount} 但状态被标为 '{b.Status}' (应为 completed 或 rejected 语义)");
                }
            }
            // 2. 有真正失败却标为 completed
            else if (b.FailedCount > 0 && b.Status.Equals("completed", StringComparison.OrdinalIgnoreCase))
            {
                totalViolations++;
                if (samples.Count < opt.MaxSampleCount)
                {
                    samples.Add($"Batch={b.BatchId}: FailedCount={b.FailedCount} > 0 但状态被标为 'completed'");
                }
            }
        }

        if (totalViolations > 0)
        {
            return InvariantResult.Failure(
                $"INV-M21 FAIL: 检测到 {totalViolations} 个批次状态语义与计数指标不自洽",
                totalViolations,
                totalViolations,
                0,
                samples,
                null,
                null,
                note,
                fallback);
        }

        return InvariantResult.Success("INV-M21 PASS: 所有批次状态与其失败/拒绝计数语义一致", note, fallback);
    }

    /// <summary>
    /// S12 (INV-M22): 派生表在使用
    /// 判据: 派生表（时间线块、使用聚合）在最近数据上必须非空；若设计为在线计算，则不得保留空表（二选一，不允许含糊）。
    /// 阈值: 最近 24h 有源数据时，派生表行数 &gt; 0（或显式声明在线计算）。
    /// 为什么是这个阈值: 存在死表或未初始化的空派生表会导致查询落入空表返回空白，或开发者误以为有预聚合而引发性能雪崩。
    /// </summary>
    public static InvariantResult CheckS12_DerivedTableActive(
        IEnumerable<DerivedTableStatus> tables,
        InvariantOptions? options = null)
    {
        var (opt, fallback, note) = InvariantOptions.Resolve(options);

        int totalViolations = 0;
        var samples = new List<string>();

        foreach (var t in tables)
        {
            if (t.SourceDataCountLast24H > 0)
            {
                if (t.DerivedRowCount == 0 && !t.IsExplicitOnlineCalculation)
                {
                    totalViolations++;
                    if (samples.Count < opt.MaxSampleCount)
                    {
                        samples.Add($"Table={t.TableName}: 最近24h存在源数据 {t.SourceDataCountLast24H} 条，派生表行数却为 0 且未声明在线计算");
                    }
                }
            }
        }

        if (totalViolations > 0)
        {
            return InvariantResult.Failure(
                $"INV-M22 FAIL: 检测到 {totalViolations} 个派生表存在源数据但为空且未声明在线计算",
                totalViolations,
                totalViolations,
                0,
                samples,
                null,
                null,
                note,
                fallback);
        }

        return InvariantResult.Success("INV-M22 PASS: 派生表正常更新或已明确声明在线计算", note, fallback);
    }

    /// <summary>
    /// S13 (INV-P22): 实例唯一
    /// 判据: 同一 device_id 在任一时刻只应有一条独立采集流（用轮询相位 / 会话序号连续性判定）。
    /// 阈值: 同一小时内出现 &gt;= 2 条互斥采集流 = 红。
    /// 为什么是这个阈值: 多实例同时采集同一设备会产生竞态覆盖、双倍计数和会话断裂，破坏时序完整性。
    /// </summary>
    public static InvariantResult CheckS13_SingleInstance(
        IEnumerable<CollectionHeartbeat> heartbeats,
        InvariantOptions? options = null)
    {
        var (opt, fallback, note) = InvariantOptions.Resolve(options);

        var list = heartbeats.ToList();
        var groups = list.GroupBy(h => (h.DeviceId, Hour: new DateTime(h.Timestamp.Year, h.Timestamp.Month, h.Timestamp.Day, h.Timestamp.Hour, 0, 0, DateTimeKind.Utc)));

        int totalViolations = 0;
        var samples = new List<string>();

        foreach (var g in groups)
        {
            // 检查同一小时内是否存在不同的 instanceId 或互相冲突的会话序号/相位
            var instanceIds = g.Select(h => h.InstanceId).Where(id => !string.IsNullOrEmpty(id)).Distinct().ToList();
            if (instanceIds.Count >= 2)
            {
                totalViolations++;
                if (samples.Count < opt.MaxSampleCount)
                {
                    samples.Add($"Device={g.Key.DeviceId}, Hour={g.Key.Hour:yyyy-MM-dd HH:00}: 检测到 {instanceIds.Count} 个不同实例ID ({string.Join(", ", instanceIds)})");
                }
                continue;
            }

            // 若无 instanceId，检查是否存在明显互斥的固定相位交错（例如两条不同相位的周期采集流）
            var phases = g.Select(h => Math.Round(h.PhaseOffsetSeconds, 1)).Distinct().ToList();
            if (phases.Count >= 2 && g.Count() >= 6)
            {
                // 检查是否并发交错存在
                totalViolations++;
                if (samples.Count < opt.MaxSampleCount)
                {
                    samples.Add($"Device={g.Key.DeviceId}, Hour={g.Key.Hour:yyyy-MM-dd HH:00}: 检测到 {phases.Count} 个互斥轮询相位并发交错 ({string.Join(", ", phases)}s)");
                }
            }
        }

        if (totalViolations > 0)
        {
            return InvariantResult.Failure(
                $"INV-P22 FAIL: 检测到 {totalViolations} 处同一设备多实例并发采集冲突",
                totalViolations,
                totalViolations,
                0,
                samples,
                null,
                null,
                note,
                fallback);
        }

        return InvariantResult.Success("INV-P22 PASS: 每台设备均保持唯一样本采集实例流", note, fallback);
    }

    #endregion
}
