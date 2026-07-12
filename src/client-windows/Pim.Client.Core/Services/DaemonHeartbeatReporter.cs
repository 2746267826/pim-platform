using System.Reflection;
using System.Text.Json;
using Pim.Client.Core.Models;

namespace Pim.Client.Core.Services;

public sealed class DaemonHeartbeatReporter
{
    private readonly ApiClient _api;

    public DaemonHeartbeatReporter(ApiClient api)
    {
        _api = api;
    }

    public async Task ReportAsync(DaemonHeartbeatRequest heartbeat, CancellationToken ct = default)
    {
        await _api.PostAsync<object>("daemon/heartbeat", heartbeat, ct);
    }

    public static DaemonHeartbeatRequest BuildHeartbeat(
        string deviceId,
        string version,
        string serverUrl,
        DateTimeOffset? lastSuccessfulUploadAt,
        DateTimeOffset? lastAttemptedUploadAt,
        string? lastError,
        int? uploadQueueCount = null,
        string activityWatchState = "Unknown",
        string keyStatsState = "Unknown",
        object? statusDetails = null)
    {
        var normalizedServerUrl = ApiClient.NormalizeServerUrl(
            string.IsNullOrWhiteSpace(serverUrl)
                ? ClientDefaults.DefaultServerUrl
                : serverUrl);

        var statusPayload = new Dictionary<string, object?>
        {
            ["machine"] = Environment.MachineName,
            ["process"] = "pim-windows-daemon"
        };

        if (statusDetails is not null)
        {
            foreach (var prop in statusDetails.GetType().GetProperties())
            {
                statusPayload[prop.Name] = prop.GetValue(statusDetails);
            }
        }

        return new DaemonHeartbeatRequest(
            deviceId,
            "windows",
            string.IsNullOrWhiteSpace(version)
                ? Assembly.GetExecutingAssembly().GetName().Version?.ToString() ?? "unknown"
                : version,
            normalizedServerUrl,
            lastSuccessfulUploadAt,
            lastAttemptedUploadAt,
            lastError,
            uploadQueueCount,
            activityWatchState,
            keyStatsState,
            false,
            JsonSerializer.Serialize(statusPayload));
    }
}
