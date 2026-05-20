using System.Net.Http.Json;
using System.Text.Json.Serialization;
using Pim.Client.Core.Models;

namespace Pim.Client.Core.Services;

public sealed class KeyStatsCollectorService : IDisposable
{
    private static readonly string KeyStatsBase =
        Environment.GetEnvironmentVariable("KEYSTATS_BASE_URL") ?? "http://127.0.0.1:18080";

    private readonly HttpClient _keyStats;
    private readonly ApiClient _api;
    private readonly CancellationTokenSource _cts = new();
    private static readonly object LockObj = new();
    private DateTime? _lastUploadTime;
    private string? _lastUploadError;

    public Action<string>? Log { get; set; }
    public DateTime? LastUploadTime { get { lock (LockObj) return _lastUploadTime; } }
    public string? LastUploadError { get { lock (LockObj) return _lastUploadError; } }

    public KeyStatsCollectorService(ApiClient api)
    {
        _api = api;
        _keyStats = new HttpClient
        {
            BaseAddress = new Uri(KeyStatsBase),
            Timeout = TimeSpan.FromSeconds(5)
        };
    }

    public void Start()
    {
        Task.Run(async () =>
        {
            try
            {
                using var timer = new PeriodicTimer(TimeSpan.FromMinutes(1));
                await CollectAndUploadAsync();

                while (await timer.WaitForNextTickAsync(_cts.Token))
                {
                    await CollectAndUploadAsync();
                }
            }
            catch (OperationCanceledException)
            {
            }
        }, _cts.Token);
    }

    public async Task SyncNowAsync()
    {
        try
        {
            await CollectAndUploadAsync();
        }
        catch (Exception ex)
        {
            Log?.Invoke($"[KeyStatsCollector] Manual sync error: {ex.Message}");
        }
    }

    private async Task CollectAndUploadAsync()
    {
        try
        {
            var stats = await _keyStats.GetFromJsonAsync<KeyStatsSnapshot>("/api/stats/", _cts.Token);
            if (stats is null) return;

            var sample = new KeystatsSampleUploadPayload(
                stats.DeviceId,
                DateTimeOffset.UtcNow.ToString("O"),
                stats.Date,
                stats.KeyPresses,
                stats.KeyPressCounts,
                stats.LeftClicks,
                stats.RightClicks,
                stats.MiddleClicks,
                stats.SideBackClicks,
                stats.SideForwardClicks,
                stats.MouseDistance,
                stats.ScrollDistance,
                stats.PeakKps,
                stats.PeakCps,
                stats.FormattedMouseDistance,
                stats.FormattedScrollDistance,
                stats.AppStats);

            var sampleResult = await TryPostAsync("/pc/keystats/samples", sample);
            var legacyResult = await TryPostAsync("/pc/keystats/upload", stats);
            var healthMessage = BuildUploadHealthMessage(sampleResult is not null, legacyResult is not null);

            if (sampleResult is not null || legacyResult is not null)
            {
                lock (LockObj)
                {
                    _lastUploadTime = DateTime.Now;
                    _lastUploadError = healthMessage;
                }
                Log?.Invoke($"[KeyStatsCollector] Uploaded snapshot for {stats.Date} (sample: {FormatUploadResult(sampleResult)}, legacy: {FormatUploadResult(legacyResult)})");
            }
            else
            {
                lock (LockObj) { _lastUploadError = healthMessage; }
                Log?.Invoke($"[KeyStatsCollector] {healthMessage}");
            }
        }
        catch (Exception ex) when (ex is HttpRequestException or TaskCanceledException)
        {
            lock (LockObj) { _lastUploadError = ex.Message; }
            Log?.Invoke($"[KeyStatsCollector] Unavailable: {ex.Message}");
        }
        catch (Exception ex)
        {
            lock (LockObj) { _lastUploadError = ex.Message; }
            Log?.Invoke($"[KeyStatsCollector] Error: {ex.Message}");
        }
    }

    private async Task<ApiResponse<string>?> TryPostAsync(string endpoint, object payload)
    {
        try
        {
            return await _api.PostAsync<ApiResponse<string>>(endpoint, payload, _cts.Token);
        }
        catch (Exception ex) when (ex is HttpRequestException or TaskCanceledException)
        {
            Log?.Invoke($"[KeyStatsCollector] Upload to {endpoint} failed: {ex.Message}");
            return null;
        }
    }

    private static string FormatUploadResult(ApiResponse<string>? result)
    {
        return result is null ? "failed" : "ok";
    }

    private static string? BuildUploadHealthMessage(bool sampleOk, bool legacyOk)
    {
        return (sampleOk, legacyOk) switch
        {
            (true, true) => null,
            (true, false) => "Sample ok; legacy upload failed",
            (false, true) => "Sample upload failed; legacy ok",
            _ => "Both sample and legacy uploads returned null response"
        };
    }

    public void Dispose()
    {
        _cts.Cancel();
        _cts.Dispose();
        _keyStats.Dispose();
    }

    private sealed class KeyStatsSnapshot
    {
        [JsonPropertyName("deviceId")]
        public string DeviceId { get; set; } = Environment.MachineName;

        [JsonPropertyName("date")]
        public string Date { get; set; } = DateTimeOffset.Now.ToString("O");

        [JsonPropertyName("keyPresses")]
        public int KeyPresses { get; set; }

        [JsonPropertyName("keyPressCounts")]
        public Dictionary<string, int>? KeyPressCounts { get; set; }

        [JsonPropertyName("leftClicks")]
        public int LeftClicks { get; set; }

        [JsonPropertyName("rightClicks")]
        public int RightClicks { get; set; }

        [JsonPropertyName("middleClicks")]
        public int MiddleClicks { get; set; }

        [JsonPropertyName("sideBackClicks")]
        public int SideBackClicks { get; set; }

        [JsonPropertyName("sideForwardClicks")]
        public int SideForwardClicks { get; set; }

        [JsonPropertyName("mouseDistance")]
        public double MouseDistance { get; set; }

        [JsonPropertyName("scrollDistance")]
        public double ScrollDistance { get; set; }

        [JsonPropertyName("formattedMouseDistance")]
        public string? FormattedMouseDistance { get; set; }

        [JsonPropertyName("formattedScrollDistance")]
        public string? FormattedScrollDistance { get; set; }

        [JsonPropertyName("peakKPS")]
        public int PeakKps { get; set; }

        [JsonPropertyName("peakCPS")]
        public int PeakCps { get; set; }

        [JsonPropertyName("appStats")]
        public Dictionary<string, KeyStatsAppStats>? AppStats { get; set; }
    }

    private sealed class KeyStatsAppStats
    {
        public string AppName { get; set; } = string.Empty;
        public string DisplayName { get; set; } = string.Empty;
        public int KeyPresses { get; set; }
        public int LeftClicks { get; set; }
        public int RightClicks { get; set; }
        public int MiddleClicks { get; set; }
        public int SideBackClicks { get; set; }
        public int SideForwardClicks { get; set; }
        public double ScrollDistance { get; set; }
    }

    private sealed record KeystatsSampleUploadPayload(
        string PimDeviceId,
        string SampledAt,
        string Date,
        int KeyPresses,
        Dictionary<string, int>? KeyPressCounts,
        int LeftClicks,
        int RightClicks,
        int MiddleClicks,
        int SideBackClicks,
        int SideForwardClicks,
        double MouseDistance,
        double ScrollDistance,
        [property: JsonPropertyName("peakKPS")]
        int PeakKps,
        [property: JsonPropertyName("peakCPS")]
        int PeakCps,
        [property: JsonPropertyName("formattedMouseDistance")]
        string? FormattedMouseDistance,
        [property: JsonPropertyName("formattedScrollDistance")]
        string? FormattedScrollDistance,
        Dictionary<string, KeyStatsAppStats>? AppStats
    );
}
