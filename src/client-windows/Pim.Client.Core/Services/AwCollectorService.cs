using System.Net.Http.Json;
using System.Text.Json.Serialization;
using Pim.Client.Core.Models;

namespace Pim.Client.Core.Services;

public class AwCollectorService : IDisposable
{
    private const int ActivityWatchUnboundedLimit = -1;
    private const int CompleteAwUploadBatchSize = 500;
    private readonly HttpClient _aw;
    private readonly ApiClient _api;
    private readonly CancellationTokenSource _cts = new();
    private readonly SemaphoreSlim _collectionGate = new(1, 1);
    private readonly AwCollectorCursorState _cursorState = new();
    private int _queueCount;
    private DateTime? _lastUploadTime;
    private string? _lastUploadError;
    private static readonly object _lock = new();
    private static readonly string AwBase = ClientDefaults.AwBaseUrl;
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

    public async Task BackfillAsync(DateTimeOffset startUtc, DateTimeOffset endUtc)
    {
        await _collectionGate.WaitAsync(_cts.Token);
        try
        {
            if (endUtc <= startUtc)
            {
                const string message = "ActivityWatch backfill skipped: end must be after start";
                Log?.Invoke($"[AwCollector] {message}");
                lock (_lock) { _lastUploadError = message; }
                return;
            }

            _awInfo ??= await FetchAwInfoAsync();
            var buckets = await FetchSupportedBucketsAsync();

            Log?.Invoke($"[AwCollector] Backfill started: {startUtc:O} - {endUtc:O}");
            var outcomes = new List<AwBucketUploadOutcome>();
            foreach (var bucket in buckets)
                outcomes.Add(await BackfillBucketAsync(bucket, startUtc, endUtc));

            var error = BuildUploadHealthMessage(outcomes);
            var errorDetails = outcomes
                .Select(o => o.Error)
                .Prepend(error)
                .Where(e => !string.IsNullOrWhiteSpace(e));
            var backfillError = string.Join("; ", errorDetails);

            if (outcomes.Sum(o => o.Uploaded) > 0)
            {
                lock (_lock)
                {
                    _lastUploadTime = DateTime.Now;
                    _lastUploadError = string.IsNullOrWhiteSpace(backfillError) ? null : backfillError;
                }
            }

            var summary = string.Join(", ", buckets.Zip(outcomes, (bucket, outcome) => $"{bucket.Id} {outcome.Uploaded}/{outcome.Fetched}"));
            Log?.Invoke($"[AwCollector] Backfill finished: {summary}");
        }
        finally
        {
            _collectionGate.Release();
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
        await _collectionGate.WaitAsync(_cts.Token);
        try
        {
            _awInfo ??= await FetchAwInfoAsync();
            var buckets = await FetchSupportedBucketsAsync();

            var outcomes = new List<AwBucketUploadOutcome>();
            foreach (var bucket in buckets)
                outcomes.Add(await CollectBucketAndUploadAsync(bucket));

            var pending = outcomes.Sum(o => Math.Max(0, o.Fetched - o.Uploaded));
            var healthMessage = BuildUploadHealthMessage(outcomes);

            lock (_lock) { _queueCount = pending; }

            if (outcomes.Sum(o => o.Uploaded) > 0)
            {
                lock (_lock)
                {
                    _lastUploadTime = DateTime.Now;
                    _lastUploadError = healthMessage;
                }
            }
        }
        finally
        {
            _collectionGate.Release();
        }
    }

    private async Task<AwBucketUploadOutcome> CollectBucketAndUploadAsync(AwBucketPayload bucket)
    {
        var bucketId = bucket.Id;
        var lastId = _cursorState.LastForBucket(bucketId);
        var rawEvents = FetchNewEvents(bucketId, lastId, out var pendingLastId);

        if (rawEvents.Count == 0) return new AwBucketUploadOutcome(0, 0);

        var uploaded = 0;
        var kind = AwBucketSelection.DescribeBucketKind(bucket.Type);
        try
        {
            foreach (var batch in ChunkCompleteAwUploadEvents(rawEvents))
            {
                var events = batch
                    .Select(e => new AwEventPayload(e.Id, e.Timestamp, e.Duration, e.Data))
                    .ToList();
                var request = new CompleteAwUploadPayload(Environment.MachineName, _awInfo, bucket, events);
                var result = await _api.PostAsync<ApiResponse<int>>("/pc/aw/upload-complete", request, _cts.Token);
                if (result is null)
                {
                    const string message = "Authentication failed";
                    Log?.Invoke("[AwCollector] Complete upload returned null response (check auth)");
                    lock (_lock) { _lastUploadError = message; }
                    return new AwBucketUploadOutcome(rawEvents.Count, uploaded, message);
                }

                if (!IsSuccessResponse(result))
                {
                    var message = $"Complete upload rejected for {bucketId}: code {result.Code}, message {result.Message}";
                    Log?.Invoke($"[AwCollector] {message}");
                    lock (_lock) { _lastUploadError = message; }
                    return new AwBucketUploadOutcome(rawEvents.Count, uploaded, message);
                }

                uploaded += events.Count;
                Log?.Invoke($"[AwCollector] Uploaded {events.Count} complete {kind} events -> {result.Data} saved");
            }

            _cursorState.RecordFetched(bucketId, pendingLastId);
            _cursorState.CommitFetched();

            lock (_lock)
            {
                _queueCount = 0;
            }
            return new AwBucketUploadOutcome(rawEvents.Count, uploaded);
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

    private async Task<AwBucketUploadOutcome> BackfillBucketAsync(AwBucketPayload bucket, DateTimeOffset startUtc, DateTimeOffset endUtc)
    {
        var bucketId = bucket.Id;
        var kind = AwBucketSelection.DescribeBucketKind(bucket.Type);

        List<RawAwEvent> rawEvents;
        try
        {
            var start = Uri.EscapeDataString(startUtc.ToString("O"));
            var end = Uri.EscapeDataString(endUtc.ToString("O"));
            var url = $"/api/0/buckets/{Uri.EscapeDataString(bucketId)}/events?start={start}&end={end}";
            rawEvents = await _aw.GetFromJsonAsync<List<RawAwEvent>>(url, _cts.Token) ?? new();
        }
        catch (Exception ex)
        {
            var message = $"ActivityWatch backfill fetch failed for {bucketId}: {ex.Message}";
            Log?.Invoke($"[AwCollector] {message}");
            lock (_lock) { _lastUploadError = message; }
            return new AwBucketUploadOutcome(0, 0, message);
        }

        if (rawEvents.Count == 0)
        {
            Log?.Invoke($"[AwCollector] Backfill found no events for {bucketId}");
            return new AwBucketUploadOutcome(0, 0);
        }

        var uploaded = 0;
        var errors = new List<string>();
        foreach (var batch in rawEvents.Chunk(200))
        {
            var events = batch
                .Select(e => new AwEventPayload(e.Id, e.Timestamp, e.Duration, e.Data))
                .ToList();
            var request = new CompleteAwUploadPayload(Environment.MachineName, _awInfo, bucket, events);

            try
            {
                var result = await _api.PostAsync<ApiResponse<int>>("/pc/aw/upload-complete", request, _cts.Token);
                if (result is null)
                {
                    var message = $"Backfill upload returned null response for {bucketId}";
                    errors.Add(message);
                    Log?.Invoke($"[AwCollector] {message}");
                    continue;
                }

                if (!IsSuccessResponse(result))
                {
                    var message = $"Backfill upload rejected for {bucketId}: code {result.Code}, message {result.Message}";
                    errors.Add(message);
                    Log?.Invoke($"[AwCollector] {message}");
                    continue;
                }

                uploaded += events.Count;
                Log?.Invoke($"[AwCollector] Backfill accepted {events.Count} {kind} events -> {result.Data} saved/upserted");
            }
            catch (Exception ex)
            {
                var message = $"Backfill upload failed for {bucketId}: {ex.Message}";
                errors.Add(message);
                Log?.Invoke($"[AwCollector] {message}");
            }
        }

        lock (_lock)
        {
            _lastUploadError = errors.Count == 0 ? null : string.Join("; ", errors);
        }

        return new AwBucketUploadOutcome(rawEvents.Count, uploaded, errors.Count == 0 ? null : string.Join("; ", errors));
    }

    private static bool IsSuccessResponse<T>(ApiResponse<T> result) =>
        result.Code is 0 or 200;

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

    private async Task<IReadOnlyList<AwBucketPayload>> FetchSupportedBucketsAsync()
    {
        try
        {
            var buckets = await _aw.GetFromJsonAsync<Dictionary<string, AwBucketPayload>>("/api/0/buckets/", _cts.Token) ?? new();
            var supported = new List<AwBucketPayload>();

            foreach (var (bucketId, bucket) in buckets)
            {
                var bucketWithId = EnsureBucketId(bucketId, bucket);
                _bucketCache[bucketWithId.Id] = bucketWithId;
                if (AwBucketSelection.IsSupportedUploadBucket(bucketWithId.Id, bucketWithId.Type, bucketWithId.Client))
                    supported.Add(bucketWithId);
            }

            return supported
                .OrderBy(b => b.Id, StringComparer.Ordinal)
                .ToList();
        }
        catch (Exception ex)
        {
            var message = $"ActivityWatch bucket discovery failed: {ex.Message}";
            Log?.Invoke($"[AwCollector] {message}");
            lock (_lock) { _lastUploadError = message; }
            return Array.Empty<AwBucketPayload>();
        }
    }

    private static AwBucketPayload EnsureBucketId(string bucketId, AwBucketPayload bucket)
    {
        bucket.Id = string.IsNullOrWhiteSpace(bucket.Id) ? bucketId : bucket.Id;
        return bucket;
    }

    private async Task<AwBucketPayload?> FetchBucketAsync(string bucketId)
    {
        if (_bucketCache.TryGetValue(bucketId, out var cached))
            return cached;

        try
        {
            var bucket = await _aw.GetFromJsonAsync<AwBucketPayload>($"/api/0/buckets/{Uri.EscapeDataString(bucketId)}", _cts.Token);
            if (bucket is not null)
            {
                var bucketWithId = EnsureBucketId(bucketId, bucket);
                _bucketCache[bucketWithId.Id] = bucketWithId;
                return bucketWithId;
            }

            return null;
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
            var url = BuildEventsUrl(bucketId);
            var response = _aw.GetAsync(url, _cts.Token).GetAwaiter().GetResult();
            if (!response.IsSuccessStatusCode) return new();

            var all = response.Content.ReadFromJsonAsync<List<RawAwEvent>>(cancellationToken: _cts.Token)
                .GetAwaiter().GetResult() ?? new();

            var currentLast = lastId;
            var unprocessed = all
                .Where(e => e.Id > currentLast)
                .OrderBy(e => e.Id)
                .ToList();
            if (unprocessed.Count > 0)
                pendingLastId = unprocessed.Max(e => e.Id);
            return unprocessed;
        }
        catch { return new(); }
    }

    private static string BuildEventsUrl(string bucketId) =>
        $"/api/0/buckets/{Uri.EscapeDataString(bucketId)}/events?limit={ActivityWatchUnboundedLimit}";

    private static IEnumerable<IReadOnlyList<T>> ChunkCompleteAwUploadEvents<T>(IEnumerable<T> events) =>
        events.Chunk(CompleteAwUploadBatchSize).Select(batch => (IReadOnlyList<T>)batch);

    private static string? BuildUploadHealthMessage(IEnumerable<AwBucketUploadOutcome> outcomes)
    {
        var pending = outcomes.Sum(o => Math.Max(0, o.Fetched - o.Uploaded));
        return pending == 0 ? null : $"Partial AW upload failure: pending {pending} events";
    }

    public void Dispose()
    {
        _cts.Cancel();
        _cts.Dispose();
        _collectionGate.Dispose();
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

    private sealed class AwBucketPayload
    {
        public string Id { get; set; } = "";
        public string? Name { get; set; }
        public string Type { get; set; } = "";
        public string Client { get; set; } = "";
        public string Hostname { get; set; } = "";
        public string? Created { get; set; }

        [JsonPropertyName("last_updated")]
        public string? LastUpdated { get; set; }

        public Dictionary<string, object>? Data { get; set; }
    }

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

    private readonly record struct AwBucketUploadOutcome(int Fetched, int Uploaded, string? Error = null);
}

public sealed class AwCollectorCursorState
{
    private readonly Dictionary<string, long> _committed = new(StringComparer.Ordinal);
    private readonly Dictionary<string, long> _pending = new(StringComparer.Ordinal);

    public long LastForBucket(string bucketId)
    {
        return _committed.GetValueOrDefault(bucketId);
    }

    public void RecordFetched(string bucketId, long lastId)
    {
        _pending[bucketId] = Math.Max(_pending.GetValueOrDefault(bucketId), lastId);
    }

    public void CommitFetched()
    {
        foreach (var (bucketId, lastId) in _pending)
            _committed[bucketId] = Math.Max(_committed.GetValueOrDefault(bucketId), lastId);

        _pending.Clear();
    }
}
