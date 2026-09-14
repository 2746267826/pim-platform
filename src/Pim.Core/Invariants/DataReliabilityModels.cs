using System;
using System.Collections.Generic;

namespace Pim.Core.Invariants;

/// <summary>
/// S1 (INV-P16): 事件区间模型（用于同类型事件不重叠判定）
/// </summary>
public sealed class EventTimeSpan
{
    public string DeviceId { get; set; } = string.Empty;
    public string EventType { get; set; } = string.Empty;
    public DateTime StartTime { get; set; }
    public DateTime EndTime { get; set; }
    public string? EventId { get; set; }
    public string? ExtraInfo { get; set; }
}

/// <summary>
/// S2 (INV-P17): 超长事件证据模型（用于超长事件三态判定）
/// </summary>
public sealed class LongEventCandidate
{
    public string DeviceId { get; set; } = string.Empty;
    public string EventType { get; set; } = string.Empty;
    public DateTime StartTime { get; set; }
    public DateTime EndTime { get; set; }
    public long Keystrokes { get; set; }
    public long MouseClicks { get; set; }
    public bool IsMediaActive { get; set; }
    public bool IsAudible { get; set; }
    public bool IsGapOrOffline { get; set; }
    public string? EventId { get; set; }
    public string? AppName { get; set; }
}

/// <summary>
/// S3 (INV-P18): 单日活跃时长记录（仅包含操作活跃与观看活跃时长）
/// </summary>
public sealed class DailyActiveDuration
{
    public string Date { get; set; } = string.Empty; // YYYY-MM-DD
    public string DeviceId { get; set; } = string.Empty;
    public double ActiveDurationSeconds { get; set; }
}

/// <summary>
/// S4 (INV-C18): 业务键模型
/// </summary>
public sealed class BusinessRecordKey
{
    public string Domain { get; set; } = string.Empty; // Location, Mobile, Pc
    public string DeviceId { get; set; } = string.Empty;
    public string UniqueKey { get; set; } = string.Empty;
    public DateTime Timestamp { get; set; }
    public string? RecordId { get; set; }

    public static BusinessRecordKey ForLocation(string deviceId, DateTime recordedAt, double lat, double lon, string? recordId = null) => new()
    {
        Domain = "Location",
        DeviceId = deviceId,
        UniqueKey = $"LOC:{deviceId}:{recordedAt:O}:{lat:F6}:{lon:F6}",
        Timestamp = recordedAt,
        RecordId = recordId
    };

    public static BusinessRecordKey ForMobile(string deviceId, string packageName, DateTime eventTime, string eventType, string? recordId = null) => new()
    {
        Domain = "Mobile",
        DeviceId = deviceId,
        UniqueKey = $"MOB:{deviceId}:{packageName}:{eventTime:O}:{eventType}",
        Timestamp = eventTime,
        RecordId = recordId
    };

    public static BusinessRecordKey ForPc(string deviceId, DateTime timestamp, double duration, string eventType, string? appName, string? browser, string? instanceId, string? recordId = null) => new()
    {
        Domain = "Pc",
        DeviceId = deviceId,
        UniqueKey = $"PC:{deviceId}:{timestamp:O}:{duration:F1}:{eventType}:{appName ?? string.Empty}:{browser ?? string.Empty}:{instanceId ?? string.Empty}",
        Timestamp = timestamp,
        RecordId = recordId
    };
}

/// <summary>
/// S5 (INV-P19): 时钟校验项
/// </summary>
public sealed class ClockEventItem
{
    public string DeviceId { get; set; } = string.Empty;
    public DateTime EventTime { get; set; }
    public DateTime ServerReceivedTime { get; set; }
    public string? EventId { get; set; }
}

/// <summary>
/// S6 (INV-P20): 下线声明
/// </summary>
public sealed class OfflineDeclaration
{
    public string DeviceId { get; set; } = string.Empty;
    public DateTime StartTime { get; set; }
    public DateTime EndTime { get; set; }
    public string Reason { get; set; } = string.Empty; // shutdown, sleep, planned-offline
}

/// <summary>
/// S6 (INV-P20): 设备链路追踪包
/// </summary>
public sealed class DeviceActivityTrace
{
    public string DeviceId { get; set; } = string.Empty;
    public IReadOnlyList<DateTime> EventTimes { get; set; } = Array.Empty<DateTime>();
    public IReadOnlyList<OfflineDeclaration> Declarations { get; set; } = Array.Empty<OfflineDeclaration>();
    public IReadOnlyList<(DateTime EventTime, DateTime CreatedAt)> UploadLagSamples { get; set; } = Array.Empty<(DateTime, DateTime)>();
}

/// <summary>
/// S7 (INV-P21): 时间轴区间
/// </summary>
public sealed class TimelineInterval
{
    public string DeviceId { get; set; } = string.Empty;
    public DateTime StartTime { get; set; }
    public DateTime EndTime { get; set; }
    public bool IsGap { get; set; }
    public string? EventType { get; set; }
}

/// <summary>
/// S8 (INV-C19): 日界三层样本
/// </summary>
public sealed class DayBoundarySample
{
    public DateTime EventTimeUtc { get; set; }
    public string DataFieldDateBucket { get; set; } = string.Empty; // 数据字段的日期桶
    public string QueryWindowDate { get; set; } = string.Empty;      // 按日接口的查询窗口
    public string PageDisplayDate { get; set; } = string.Empty;      // 页面展示的业务日
    public string? EventId { get; set; }
}

/// <summary>
/// S9 (INV-C20): 覆盖率信号检查
/// </summary>
public sealed class CoverageSignalReport
{
    public string DeviceId { get; set; } = string.Empty;
    public double OnlineDurationSeconds { get; set; }
    public double ValidDataDurationSeconds { get; set; }
    public string ReportedStatus { get; set; } = string.Empty; // Normal, Healthy, Warning, Error
}

/// <summary>
/// S10 (INV-C21): 后台任务运行记录
/// </summary>
public sealed class BackgroundTaskRun
{
    public string TaskName { get; set; } = string.Empty;
    public DateTime ExecutedAt { get; set; }
    public int ProcessedCount { get; set; }
    public int OutputCount { get; set; }
    public int AvailableDataCount { get; set; }
}

/// <summary>
/// S11 (INV-M21): 批次同步状态记录
/// </summary>
public sealed class BatchSyncStatusRecord
{
    public string BatchId { get; set; } = string.Empty;
    public string Status { get; set; } = string.Empty;
    public int AcceptedCount { get; set; }
    public int FailedCount { get; set; }
    public int RejectedCount { get; set; }
    public int TotalCount { get; set; }
}

/// <summary>
/// S12 (INV-M22): 派生表状态
/// </summary>
public sealed class DerivedTableStatus
{
    public string TableName { get; set; } = string.Empty;
    public int SourceDataCountLast24H { get; set; }
    public int DerivedRowCount { get; set; }
    public bool IsExplicitOnlineCalculation { get; set; }
    public string? DocumentationNote { get; set; }
}

/// <summary>
/// S13 (INV-P22): 采集心跳/事件
/// </summary>
public sealed class CollectionHeartbeat
{
    public string DeviceId { get; set; } = string.Empty;
    public DateTime Timestamp { get; set; }
    public long SessionId { get; set; }
    public double PhaseOffsetSeconds { get; set; }
    public string? InstanceId { get; set; }
}
