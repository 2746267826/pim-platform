using System.Text.Json.Serialization;

namespace Pim.Client.Core.Models;

public sealed class TrackerConfig
{
    public bool Enabled { get; set; } = true;
    public int PollIntervalSeconds { get; set; } = 10;
    public int IdleThresholdSeconds { get; set; } = 300;
    public int GapThresholdSeconds { get; set; } = 60;
    public int BrowserBridgePort { get; set; } = 15601;
    public int UploadBatchSize { get; set; } = 500;
    public int UploadIntervalSeconds { get; set; } = 30;
    public int HealthReportIntervalSeconds { get; set; } = 300;
    public int LogRetentionDays { get; set; } = 30;
    public List<string> ExcludedApps { get; set; } = new();
}

public sealed class TrackerWindowInfo
{
    public IntPtr Hwnd { get; set; }
    public uint ProcessId { get; set; }
    public string ExePath { get; set; } = string.Empty;
    public string AppName { get; set; } = string.Empty;
    public string DisplayName { get; set; } = string.Empty;
    public string WindowTitle { get; set; } = string.Empty;
    public string? CommandLine { get; set; }
    public DateTimeOffset CapturedAt { get; set; } = DateTimeOffset.UtcNow;

    public bool IsUwpHost => string.Equals(AppName, "ApplicationFrameHost", StringComparison.OrdinalIgnoreCase)
                             || string.Equals(AppName, "ShellExperienceHost", StringComparison.OrdinalIgnoreCase);
}

public sealed class BrowserHeartbeat
{
    [JsonPropertyName("url")] public string Url { get; set; } = string.Empty;
    [JsonPropertyName("title")] public string Title { get; set; } = string.Empty;
    [JsonPropertyName("audible")] public bool Audible { get; set; }
    [JsonPropertyName("incognito")] public bool Incognito { get; set; }
    [JsonPropertyName("tabCount")] public int TabCount { get; set; }
    [JsonPropertyName("timestamp")] public string Timestamp { get; set; } = DateTimeOffset.UtcNow.ToString("O");
    [JsonPropertyName("browser")] public string Browser { get; set; } = "other";
    [JsonPropertyName("instanceId")] public string InstanceId { get; set; } = string.Empty;

    public DateTimeOffset ParsedTimestamp => DateTimeOffset.TryParse(Timestamp, out var ts) ? ts : DateTimeOffset.UtcNow;
    public string? Domain
    {
        get
        {
            if (Uri.TryCreate(Url, UriKind.Absolute, out var uri))
                return uri.Host;
            return null;
        }
    }
    public string? PagePath
    {
        get
        {
            if (Uri.TryCreate(Url, UriKind.Absolute, out var uri))
                return uri.IsFile ? uri.LocalPath : uri.PathAndQuery;
            return null;
        }
    }
}

public sealed class TrackerSession
{
    public long Id { get; set; }
    public string DeviceId { get; set; } = Environment.MachineName;
    public string ExePath { get; set; } = string.Empty;
    public string AppName { get; set; } = string.Empty;
    public string? WindowTitle { get; set; }
    public DateTimeOffset StartedAt { get; set; }
    public DateTimeOffset? EndedAt { get; set; }
    public double? DurationSecs { get; set; }
    public bool IsIdle { get; set; }
    public bool IsMediaActive { get; set; }
    public string Date => StartedAt.ToString("yyyy-MM-dd");
    public List<TrackerPageVisit> PageVisits { get; set; } = new();
}

public sealed class TrackerPageVisit
{
    public long Id { get; set; }
    public long SessionId { get; set; }
    public string? WindowTitle { get; set; }
    public string? Url { get; set; }
    public string? Domain { get; set; }
    public DateTimeOffset StartedAt { get; set; }
    public DateTimeOffset? EndedAt { get; set; }
    public double? DurationSecs { get; set; }
}

public sealed class TrackerEventForUpload
{
    [JsonPropertyName("timestamp")] public string Timestamp { get; set; } = string.Empty;
    [JsonPropertyName("duration")] public double Duration { get; set; }
    [JsonPropertyName("eventType")] public string EventType { get; set; } = "window";
    [JsonPropertyName("exePath")] public string? ExePath { get; set; }
    [JsonPropertyName("appName")] public string? AppName { get; set; }
    [JsonPropertyName("displayName")] public string? DisplayName { get; set; }
    [JsonPropertyName("windowTitle")] public string? WindowTitle { get; set; }
    [JsonPropertyName("commandLine")] public string? CommandLine { get; set; }
    [JsonPropertyName("isIdle")] public bool IsIdle { get; set; }
    [JsonPropertyName("isMediaActive")] public bool IsMediaActive { get; set; }
    [JsonPropertyName("url")] public string? Url { get; set; }
    [JsonPropertyName("domain")] public string? Domain { get; set; }
    [JsonPropertyName("pagePath")] public string? PagePath { get; set; }
    [JsonPropertyName("audible")] public bool? Audible { get; set; }
    [JsonPropertyName("incognito")] public bool? Incognito { get; set; }
    [JsonPropertyName("tabCount")] public int? TabCount { get; set; }
    [JsonPropertyName("pageVisitCount")] public int PageVisitCount { get; set; }
    [JsonPropertyName("pageVisitDuration")] public double PageVisitDuration { get; set; }
    [JsonPropertyName("rawJson")] public object? RawJson { get; set; }
    [JsonPropertyName("date")] public string Date { get; set; } = string.Empty;
    [JsonPropertyName("browser")] public string? Browser { get; set; }
    [JsonPropertyName("instanceId")] public string? InstanceId { get; set; }
}

public sealed class BrowserConnection
{
    public string InstanceId { get; set; } = string.Empty;
    public string BrowserType { get; set; } = "other";
    public string DisplayName { get; set; } = string.Empty;
    public bool IsConnected { get; set; }
    public DateTimeOffset LastHeartbeat { get; set; }
    public string? LastUrl { get; set; }
    public string? LastTitle { get; set; }
    public bool? LastAudible { get; set; }
    public int? LastTabCount { get; set; }
    public bool? LastIncognito { get; set; }
    public long HeartbeatCount { get; set; }
    public DateTimeOffset FirstSeen { get; set; }
}

public sealed class TrackerEventsUploadRequest
{
    [JsonPropertyName("deviceId")] public string DeviceId { get; set; } = Environment.MachineName;
    [JsonPropertyName("events")] public List<TrackerEventForUpload> Events { get; set; } = new();
}

public sealed class TrackerHealthRequest
{
    [JsonPropertyName("deviceId")] public string DeviceId { get; set; } = Environment.MachineName;
    [JsonPropertyName("status")] public string Status { get; set; } = "running";
    [JsonPropertyName("uptimeSeconds")] public double UptimeSeconds { get; set; }
    [JsonPropertyName("hookActive")] public bool HookActive { get; set; }
    [JsonPropertyName("pollCount")] public long PollCount { get; set; }
    [JsonPropertyName("sessionsCreated")] public long SessionsCreated { get; set; }
    [JsonPropertyName("eventsUploaded")] public long EventsUploaded { get; set; }
    [JsonPropertyName("uploadFailures")] public long UploadFailures { get; set; }
    [JsonPropertyName("lastError")] public string? LastError { get; set; }
    [JsonPropertyName("browserConnected")] public bool BrowserConnected { get; set; }
    [JsonPropertyName("browserHeartbeatAgeSeconds")] public double? BrowserHeartbeatAgeSeconds { get; set; }
}
