using System.Net.Http.Json;
using System.Text.Json.Serialization;
using Pim.Client.Core.Models;

namespace Pim.Client.Core.Services;

public sealed class KeyStatsLocalStatsClient : IDisposable
{
    public static string ResolveBaseUrl()
        => ClientDefaults.KeyStatsBaseUrl;

    private readonly HttpClient _http;
    private readonly bool _ownsHttp;

    public KeyStatsLocalStatsClient(HttpClient? http = null)
    {
        if (http is null)
        {
            _http = new HttpClient
            {
                BaseAddress = new Uri(ResolveBaseUrl()),
                Timeout = TimeSpan.FromSeconds(5)
            };
            _ownsHttp = true;
        }
        else
        {
            _http = http;
            _ownsHttp = false;
        }
    }

    public async Task<(KeyStatsCounterSnapshot? Snapshot, string? Error)> GetSnapshotAsync(
        CancellationToken ct = default)
    {
        try
        {
            var dto = await _http.GetFromJsonAsync<StatsDto>("/api/stats/", ct);
            if (dto is null)
            {
                return (null, "empty snapshot");
            }

            return (new KeyStatsCounterSnapshot(
                dto.KeyPresses,
                dto.LeftClicks,
                dto.RightClicks,
                dto.MiddleClicks,
                dto.SideBackClicks,
                dto.SideForwardClicks,
                dto.MouseDistance,
                dto.ScrollDistance), null);
        }
        catch (OperationCanceledException) when (ct.IsCancellationRequested)
        {
            throw;
        }
        catch (Exception ex)
        {
            return (null, ex.Message);
        }
    }

    public static bool CountersIndicateRecovery(
        KeyStatsCounterSnapshot? before,
        KeyStatsCounterSnapshot? after)
    {
        if (after is null)
        {
            return false;
        }

        return after.HasAnyActivity || after.GrewFrom(before);
    }

    public void Dispose()
    {
        if (_ownsHttp)
        {
            _http.Dispose();
        }
    }

    private sealed class StatsDto
    {
        [JsonPropertyName("keyPresses")]
        public int KeyPresses { get; set; }

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
    }
}
