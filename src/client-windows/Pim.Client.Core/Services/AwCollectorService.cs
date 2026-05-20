using System.Net.Http.Json;
using System.Text.Json.Serialization;
using Pim.Client.Core.Models;

namespace Pim.Client.Core.Services;

public class AwCollectorService : IDisposable
{
    private readonly HttpClient _aw;
    private readonly ApiClient _api;
    private readonly CancellationTokenSource _cts = new();
    private readonly AwCollectorCursorState _cursorState = new();
    private int _queueCount;
    private DateTime? _lastUploadTime;
    private string? _lastUploadError;
    private static readonly object _lock = new();
    private static readonly string AwBase = Environment.GetEnvironmentVariable("AW_BASE_URL") ?? "http://127.0.0.1:5600";
    private static readonly string BucketId = $"aw-watcher-window_{Environment.MachineName}";
    private static readonly string AfkBucketId = $"aw-watcher-afk_{Environment.MachineName}";
    private AwInfoPayload? _awInfo;
    private readonly Dictionary<string, AwBucketPayload> _bucketCache = new();

    public Action<string>? Log { get; set; }
    public int QueueCount { get { lock (_lock) return _queueCount; } }
    public DateTime? LastUploadTime { get { lock (_lock) return _lastUploadTime; } }
    public string? LastUploadError { get { lock (_lock) return _lastUploadError; } }

    public AwCollectorService(ApiClient apiClient)
    {
        _aw = new HttpClient { BaseAddress = new Uri(AwBase) };
        _api = apiClient;
    }

    public async Task SyncNowAsync()
    {
        try
        {
            await CollectAndUploadAsync();
        }
        catch (Exception ex)
        {
            Log?.Invoke($"[AwCollector] Manual sync error: {ex.Message}");
        }
    }

    public void Start()
    {
        Task.Run(async () =>
        {
            while (!_cts.Token.IsCancellationRequested)
            {
                try
                {
                    await Task.Delay(30_000, _cts.Token);
                    await CollectAndUploadAsync();
                }
                catch (TaskCanceledException) { break; }
                catch (Exception ex)
                {
                    Log?.Invoke($"[AwCollector] Error: {ex.Message}");
                }
            }
        });
    }

    private async Task CollectAndUploadAsync()
    {
        _awInfo ??= await FetchAwInfoAsync();

        var windowOutcome = await CollectBucketAndUploadAsync(BucketId, isAfk: false);
        var afkOutcome = await CollectBucketAndUploadAsync(AfkBucketId, isAfk: true);
        var pending = Math.Max(0, windowOutcome.Fetched - windowOutcome.Uploaded)
            + Math.Max(0, afkOutcome.Fetched - afkOutcome.Uploaded);
        var healthMessage = BuildUploadHealthMessage(
            windowOutcome.Fetched,
            windowOutcome.Uploaded,
            afkOutcome.Fetched,
            afkOutcome.Uploaded);

        lock (_lock) { _queueCount = pending; }

        if (windowOutcome.Uploaded + afkOutcome.Uploaded > 0)
        {
            lock (_lock)
            {
                _lastUploadTime = DateTime.Now;
                _lastUploadError = healthMessage;
            }
        }
    }

    private async Task<AwBucketUploadOutcome> CollectBucketAndUploadAsync(string bucketId, bool isAfk)
    {
        var lastId = isAfk ? _cursorState.LastAfkId : _cursorState.LastWindowId;
        var rawEvents = FetchNewEvents(bucketId, lastId, out var pendingLastId);

        if (rawEvents.Count == 0) return new AwBucketUploadOutcome(0, 0);

        var bucket = await FetchBucketAsync(bucketId);
        if (bucket is null)
        {
            var message = $"ActivityWatch bucket metadata unavailable for {bucketId}";
            Log?.Invoke($"[AwCollector] {message}");
            lock (_lock) { _lastUploadError = message; }
            return new AwBucketUploadOutcome(rawEvents.Count, 0);
        }

        var events = rawEvents
            .Select(e => new AwEventPayload(e.Id, e.Timestamp, e.Duration, e.Data))
            .ToList();

        var request = new CompleteAwUploadPayload(Environment.MachineName, _awInfo, bucket, events);

        try
        {
            var result = await _api.PostAsync<ApiResponse<int>>("/pc/aw/upload-complete", request, _cts.Token);
            if (result is not null)
            {
                if (isAfk)
                    _cursorState.RecordFetched(_cursorState.LastWindowId, pendingLastId);
                else
                    _cursorState.RecordFetched(pendingLastId, _cursorState.LastAfkId);

                _cursorState.CommitFetched();

                Log?.Invoke($"[AwCollector] Uploaded {events.Count} complete {(isAfk ? "afk" : "window")} events -> {result.Data} saved");
                lock (_lock)
                {
                    _queueCount = 0;
                }
                return new AwBucketUploadOutcome(rawEvents.Count, events.Count);
            }

            Log?.Invoke("[AwCollector] Complete upload returned null response (check auth)");
            lock (_lock) { _lastUploadError = "Authentication failed"; }
        }
        catch (HttpRequestException ex)
        {
            Log?.Invoke($"[AwCollector] Complete upload failed: {ex.Message}");
            lock (_lock) { _lastUploadError = ex.Message; }
        }
        catch (Exception ex)
        {
            lock (_lock) { _lastUploadError = ex.Message; }
        }

        return new AwBucketUploadOutcome(rawEvents.Count, 0);
    }

    private async Task<AwInfoPayload?> FetchAwInfoAsync()
    {
        try
        {
            return await _aw.GetFromJsonAsync<AwInfoPayload>("/api/0/info", _cts.Token);
        }
        catch (Exception ex)
        {
            Log?.Invoke($"[AwCollector] AW info unavailable: {ex.Message}");
            return null;
        }
    }

    private async Task<AwBucketPayload?> FetchBucketAsync(string bucketId)
    {
        if (_bucketCache.TryGetValue(bucketId, out var cached))
            return cached;

        try
        {
            var bucket = await _aw.GetFromJsonAsync<AwBucketPayload>($"/api/0/buckets/{Uri.EscapeDataString(bucketId)}", _cts.Token);
            if (bucket is not null)
                _bucketCache[bucketId] = bucket;
            return bucket;
        }
        catch (Exception ex)
        {
            Log?.Invoke($"[AwCollector] Bucket fetch failed for {bucketId}: {ex.Message}");
            return null;
        }
    }

    private List<RawAwEvent> FetchNewEvents(string bucketId, long lastId, out long pendingLastId)
    {
        pendingLastId = lastId;
        try
        {
            var url = $"/api/0/buckets/{bucketId}/events?limit=100";
            var response = _aw.GetAsync(url, _cts.Token).GetAwaiter().GetResult();
            if (!response.IsSuccessStatusCode) return new();

            var all = response.Content.ReadFromJsonAsync<List<RawAwEvent>>(cancellationToken: _cts.Token)
                .GetAwaiter().GetResult() ?? new();

            var currentLast = lastId;
            var unprocessed = all.Where(e => e.Id > currentLast).ToList();
            if (unprocessed.Count > 0)
                pendingLastId = unprocessed.Max(e => e.Id);
            return unprocessed;
        }
        catch { return new(); }
    }

    private static string? BuildUploadHealthMessage(int windowFetched, int windowUploaded, int afkFetched, int afkUploaded)
    {
        var pendingWindow = Math.Max(0, windowFetched - windowUploaded);
        var pendingAfk = Math.Max(0, afkFetched - afkUploaded);
        if (pendingWindow == 0 && pendingAfk == 0)
            return null;

        return $"Partial AW upload failure: window pending {pendingWindow}, afk pending {pendingAfk}";
    }

    public void Dispose()
    {
        _cts.Cancel();
        _cts.Dispose();
        _aw.Dispose();
    }

    private record RawAwEvent(long Id, string Timestamp, double Duration, Dictionary<string, object>? Data)
    {
        public Dictionary<string, object> Data { get; init; } = Data ?? new();
    }

    private sealed record AwInfoPayload(
        string? Hostname,
        string? Version,
        bool Testing,
        [property: JsonPropertyName("device_id")]
        string? DeviceId
    );

    private sealed record AwBucketPayload(
        string Id,
        string? Name,
        string Type,
        string Client,
        string Hostname,
        string? Created,
        [property: JsonPropertyName("last_updated")]
        string? LastUpdated,
        Dictionary<string, object>? Data
    );

    private sealed record AwEventPayload(
        long SourceEventId,
        string Timestamp,
        double Duration,
        Dictionary<string, object>? Data
    );

    private sealed record CompleteAwUploadPayload(
        string PimDeviceId,
        AwInfoPayload? AwInfo,
        AwBucketPayload Bucket,
        List<AwEventPayload> Events
    );

    private readonly record struct AwBucketUploadOutcome(int Fetched, int Uploaded);
}

public sealed class AwCollectorCursorState
{
    private long _pendingWindowId;
    private long _pendingAfkId;

    public long LastWindowId { get; private set; }
    public long LastAfkId { get; private set; }

    public void RecordFetched(long windowLastId, long afkLastId)
    {
        _pendingWindowId = Math.Max(_pendingWindowId, windowLastId);
        _pendingAfkId = Math.Max(_pendingAfkId, afkLastId);
    }

    public void CommitFetched()
    {
        LastWindowId = Math.Max(LastWindowId, _pendingWindowId);
        LastAfkId = Math.Max(LastAfkId, _pendingAfkId);
    }
}
