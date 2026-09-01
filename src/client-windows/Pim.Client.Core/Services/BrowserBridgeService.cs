using System.Collections.Concurrent;
using System.Net;
using System.Text;
using System.Text.Json;
using System.Threading.Channels;
using Pim.Client.Core.Models;

namespace Pim.Client.Core.Services;

public sealed class BrowserBridgeService : IDisposable
{
    private const int MaxConnections = 32;
    private const int DisconnectAfterSeconds = 120;
    private const int EvictAfterSeconds = 600;
    private static readonly HashSet<string> AllowedBrowserTypes = new(StringComparer.OrdinalIgnoreCase)
    {
        "chrome", "edge", "firefox", "safari", "other"
    };

    private readonly int _port;
    private readonly Channel<BrowserHeartbeat> _channel;
    private readonly TrackerLogger? _logger;
    private HttpListener? _listener;
    private CancellationTokenSource? _cts;
    private Task? _listenTask;
    private DateTimeOffset _lastHeartbeatTime = DateTimeOffset.MinValue;
    private BrowserHeartbeat? _lastHeartbeat;
    private readonly object _lastStateLock = new();
    private readonly ConcurrentDictionary<string, BrowserConnection> _connections = new();
    private Timer? _checkTimer;

    public BrowserBridgeService(int port = 15601, TrackerLogger? logger = null)
    {
        _port = port;
        _logger = logger;
        _channel = Channel.CreateUnbounded<BrowserHeartbeat>(new UnboundedChannelOptions { SingleReader = true, SingleWriter = false });
    }

    public ChannelReader<BrowserHeartbeat> Reader => _channel.Reader;
    public IReadOnlyDictionary<string, BrowserConnection> Connections => _connections;
    // Most recently received heartbeat. OnHeartbeat always records it, so it is
    // non-null whenever _connections is non-empty.
    public BrowserHeartbeat? LastHeartbeat
    {
        get { lock (_lastStateLock) return _lastHeartbeat; }
    }
    public DateTimeOffset LastHeartbeatTime
    {
        get
        {
            lock (_lastStateLock)
            {
                if (_connections.IsEmpty) return _lastHeartbeatTime;
                var latest = DateTimeOffset.MinValue;
                foreach (var kv in _connections)
                {
                    lock (kv.Value.SyncRoot)
                    {
                        if (kv.Value.LastHeartbeat > latest) latest = kv.Value.LastHeartbeat;
                    }
                }
                return latest > _lastHeartbeatTime ? latest : _lastHeartbeatTime;
            }
        }
    }
    public bool IsConnected
    {
        get
        {
            if (_connections.IsEmpty)
            {
                lock (_lastStateLock)
                    return _lastHeartbeat is not null && (DateTimeOffset.UtcNow - _lastHeartbeatTime).TotalSeconds < DisconnectAfterSeconds;
            }
            foreach (var kv in _connections)
            {
                lock (kv.Value.SyncRoot)
                {
                    if (kv.Value.IsConnected) return true;
                }
            }
            return false;
        }
    }

    public void Start()
    {
        if (_listener is not null) return;
        _cts = new CancellationTokenSource();
        _listener = new HttpListener();
        var prefix = $"http://localhost:{_port}/";
        _listener.Prefixes.Add(prefix);
        try
        {
            _listener.Start();
            _logger?.Info("BrowserBridge", $"Listening on {prefix}");
        }
        catch (Exception ex)
        {
            _logger?.Error("BrowserBridge", $"Failed to start HttpListener on {prefix}", ex);
            throw;
        }

        _listenTask = Task.Run(() => ListenLoopAsync(_cts.Token));
        _checkTimer = new Timer(_ => CheckConnections(), null, TimeSpan.FromSeconds(30), TimeSpan.FromSeconds(30));
    }

    public void Stop()
    {
        try { _checkTimer?.Dispose(); } catch { }
        _checkTimer = null;
        try { _cts?.Cancel(); } catch { }
        try { _listener?.Stop(); } catch { }
        try { _listener?.Close(); } catch { }
        _listener = null;
    }

    public void OnHeartbeat(BrowserHeartbeat hb)
    {
        var instanceId = string.IsNullOrWhiteSpace(hb.InstanceId) ? "unknown" : hb.InstanceId.Trim();
        if (instanceId.Length > 128) instanceId = instanceId.Substring(0, 128);
        var browserType = string.IsNullOrWhiteSpace(hb.Browser) ? "other" : hb.Browser.ToLowerInvariant().Trim();
        if (!AllowedBrowserTypes.Contains(browserType)) browserType = "other";
        if (browserType.Length > 16) browserType = browserType.Substring(0, 16);
        hb.Browser = browserType;
        hb.InstanceId = instanceId;

        var conn = _connections.GetOrAdd(instanceId, _ => new BrowserConnection
        {
            InstanceId = instanceId,
            BrowserType = browserType,
            DisplayName = BuildDisplayName(hb),
            FirstSeen = DateTimeOffset.UtcNow,
        });
        lock (conn.SyncRoot)
        {
            // A concurrent eviction may have removed this instance between
            // GetOrAdd and the lock; re-add so the heartbeat is not lost.
            _connections.TryAdd(instanceId, conn);
            conn.BrowserType = browserType;
            conn.IsConnected = true;
            conn.LastHeartbeat = DateTimeOffset.UtcNow;
            conn.LastUrl = hb.Url;
            conn.LastTitle = hb.Title;
            conn.LastAudible = hb.Audible;
            conn.LastTabCount = hb.TabCount;
            conn.LastIncognito = hb.Incognito;
            conn.HeartbeatCount++;
        }

        // Enforce the instance cap: if we are over the limit after adding a new
        // instance, evict the least recently active one (not the fresh one).
        if (_connections.Count > MaxConnections)
        {
            var stale = _connections
                .Where(kv => kv.Key != instanceId)
                .OrderBy(kv => kv.Value.LastHeartbeat)
                .FirstOrDefault();
            if (stale.Key is not null)
            {
                _connections.TryRemove(stale.Key, out _);
                _logger?.Warn("BrowserBridge", $"Instance cap {MaxConnections} reached, evicted {stale.Value.DisplayName}");
            }
        }

        RebuildDisplayNames(browserType);

        lock (_lastStateLock)
        {
            _lastHeartbeat = hb;
            _lastHeartbeatTime = DateTimeOffset.UtcNow;
        }
        _channel.Writer.TryWrite(hb);
        _logger?.Debug("BrowserBridge", $"Heartbeat {hb.Domain} browser={hb.Browser} instance={instanceId} audible={hb.Audible} tabs={hb.TabCount}");
    }

    private void RebuildDisplayNames(string browserType)
    {
        var same = _connections.Values.Where(c => c.BrowserType == browserType).ToList();
        var count = same.Count;
        foreach (var c in same)
        {
            lock (c.SyncRoot)
            {
                c.DisplayName = BuildDisplayNameForConnection(c, count);
            }
        }
    }

    private string BuildDisplayNameForConnection(BrowserConnection conn, int sameTypeCount)
    {
        var type = conn.BrowserType switch
        {
            "chrome" => "Chrome",
            "edge" => "Edge",
            "firefox" => "Firefox",
            "safari" => "Safari",
            _ => conn.BrowserType
        };
        if (conn.LastIncognito == true) return $"{type} (无痕)";
        if (conn.InstanceId == "unknown") return sameTypeCount <= 1 ? type : $"{type} (未知)";
        var shortId = conn.InstanceId.Length > 4 ? conn.InstanceId[^4..] : conn.InstanceId;
        if (sameTypeCount <= 1) return type;
        return $"{type} ({shortId})";
    }

    private string BuildDisplayName(BrowserHeartbeat hb)
    {
        var type = hb.Browser switch
        {
            "chrome" => "Chrome",
            "edge" => "Edge",
            "firefox" => "Firefox",
            "safari" => "Safari",
            _ => hb.Browser
        };
        if (hb.Incognito) return $"{type} (无痕)";
        if (hb.InstanceId == "unknown") return type;
        var shortId = hb.InstanceId.Length > 4 ? hb.InstanceId[^4..] : hb.InstanceId;
        var sameTypeCount = _connections.Values.Count(c => c.BrowserType == hb.Browser) + 1;
        if (sameTypeCount <= 1) return type;
        return $"{type} ({shortId})";
    }

    public void CheckConnections()
    {
        foreach (var conn in _connections.Values)
        {
            lock (conn.SyncRoot)
            {
                var silentSeconds = (DateTimeOffset.UtcNow - conn.LastHeartbeat).TotalSeconds;
                if (conn.IsConnected && silentSeconds > DisconnectAfterSeconds)
                {
                    conn.IsConnected = false;
                    _logger?.Warn("BrowserBridge", $"BrowserBridge {conn.DisplayName} disconnected, silent for {silentSeconds:F0}s");
                }
            }
        }

        // Evict connections that have been disconnected for a long time so the
        // dictionary does not accumulate stale entries (the instance cap bounds
        // growth, this reclaims it). Reconnecting re-adds the entry.
        foreach (var kv in _connections)
        {
            var conn = kv.Value;
            lock (conn.SyncRoot)
            {
                var idleSeconds = (DateTimeOffset.UtcNow - conn.LastHeartbeat).TotalSeconds;
                if (!conn.IsConnected && idleSeconds > EvictAfterSeconds)
                {
                    _connections.TryRemove(kv.Key, out _);
                    _logger?.Info("BrowserBridge", $"Removed idle browser connection {conn.DisplayName}");
                }
            }
        }
    }

    public IReadOnlyList<BrowserConnection> GetConnectionsSnapshot()
    {
        return _connections.Values.OrderBy(c => c.BrowserType).ThenBy(c => c.InstanceId).ToList();
    }

    private async Task ListenLoopAsync(CancellationToken ct)
    {
        while (!ct.IsCancellationRequested && _listener is not null)
        {
            HttpListenerContext? ctx = null;
            try
            {
                ctx = await _listener.GetContextAsync().ConfigureAwait(false);
            }
            catch (HttpListenerException) when (ct.IsCancellationRequested) { break; }
            catch (ObjectDisposedException) { break; }
            catch (Exception ex)
            {
                _logger?.Warn("BrowserBridge", $"GetContext failed: {ex.Message}");
                await Task.Delay(500, ct).ConfigureAwait(false);
                continue;
            }

            _ = Task.Run(() => HandleAsync(ctx), ct);
        }
    }

    private async Task HandleAsync(HttpListenerContext ctx)
    {
        try
        {
            var req = ctx.Request;
            var resp = ctx.Response;

            // Only allow loopback
            try
            {
                var remote = req.RemoteEndPoint?.Address;
                if (remote is not null && !System.Net.IPAddress.IsLoopback(remote))
                {
                    resp.StatusCode = 403;
                    resp.Close();
                    _logger?.Warn("BrowserBridge", $"Rejected non-loopback {remote}");
                    return;
                }
            }
            catch { }

            // Limit body size 8KB
            if (req.ContentLength64 > 8192)
            {
                resp.StatusCode = 413;
                resp.Close();
                _logger?.Warn("BrowserBridge", $"Rejected oversized heartbeat {req.ContentLength64}");
                return;
            }

            if (req.HttpMethod == "GET" && req.Url?.AbsolutePath == "/browser/ping")
            {
                var payload = JsonSerializer.Serialize(new { status = "ok", version = "1.0.0" });
                var bytes = Encoding.UTF8.GetBytes(payload);
                resp.ContentType = "application/json";
                resp.StatusCode = 200;
                await resp.OutputStream.WriteAsync(bytes, 0, bytes.Length).ConfigureAwait(false);
                resp.Close();
                _logger?.Debug("BrowserBridge", "Ping handled");
                return;
            }

            if (req.HttpMethod == "POST" && req.Url?.AbsolutePath == "/browser/heartbeat")
            {
                string body;
                {
                    if (req.ContentLength64 > 8192)
                    {
                        resp.StatusCode = 413;
                        resp.Close();
                        _logger?.Warn("BrowserBridge", $"Rejected oversized heartbeat {req.ContentLength64}");
                        return;
                    }
                    using var ms = new System.IO.MemoryStream();
                    var buffer = new byte[4096];
                    int total = 0;
                    int read;
                    while ((read = await req.InputStream.ReadAsync(buffer, 0, buffer.Length).ConfigureAwait(false)) > 0)
                    {
                        total += read;
                        if (total > 8192)
                        {
                            resp.StatusCode = 413;
                            resp.Close();
                            _logger?.Warn("BrowserBridge", $"Rejected oversized heartbeat total {total}");
                            return;
                        }
                        ms.Write(buffer, 0, read);
                    }
                    body = Encoding.UTF8.GetString(ms.ToArray());
                    if (body.Length > 8192)
                    {
                        resp.StatusCode = 413;
                        resp.Close();
                        return;
                    }
                }

                BrowserHeartbeat? hb = null;
                try
                {
                    hb = JsonSerializer.Deserialize<BrowserHeartbeat>(body, new JsonSerializerOptions { PropertyNameCaseInsensitive = true });
                }
                catch (Exception ex)
                {
                    _logger?.Warn("BrowserBridge", $"Invalid heartbeat JSON: {ex.Message}");
                }

                if (hb is not null)
                {
                    if (string.IsNullOrWhiteSpace(hb.Timestamp))
                        hb.Timestamp = DateTimeOffset.UtcNow.ToString("O");
                    if (string.IsNullOrWhiteSpace(hb.Browser)) hb.Browser = "other";
                    if (string.IsNullOrWhiteSpace(hb.InstanceId)) hb.InstanceId = "unknown";
                    if (hb.InstanceId.Length > 128)
                    {
                        resp.StatusCode = 400;
                        resp.Close();
                        _logger?.Warn("BrowserBridge", $"Rejected heartbeat with instanceId too long {hb.InstanceId.Length}");
                        return;
                    }
                    if (hb.Browser.Length > 16) hb.Browser = hb.Browser.Substring(0, 16);
                    if (_connections.Count >= MaxConnections && !_connections.ContainsKey(hb.InstanceId))
                    {
                        resp.StatusCode = 429;
                        resp.Close();
                        _logger?.Warn("BrowserBridge", $"Rejected heartbeat: too many connections {_connections.Count}");
                        return;
                    }
                    OnHeartbeat(hb);
                }

                resp.StatusCode = 204;
                resp.Close();
                return;
            }

            resp.StatusCode = 404;
            resp.Close();
        }
        catch (Exception ex)
        {
            _logger?.Warn("BrowserBridge", $"Handle error: {ex.Message}");
            try { ctx.Response.StatusCode = 500; ctx.Response.Close(); } catch { }
        }
    }

    public void Dispose()
    {
        Stop();
        _cts?.Dispose();
    }
}
