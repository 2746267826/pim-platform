using System.Collections.Concurrent;
using System.Net;
using System.Text;
using System.Text.Json;
using System.Threading.Channels;
using Pim.Client.Core.Models;

namespace Pim.Client.Core.Services;

public sealed class BrowserBridgeService : IDisposable
{
    private readonly int _port;
    private readonly Channel<BrowserHeartbeat> _channel;
    private readonly TrackerLogger? _logger;
    private HttpListener? _listener;
    private CancellationTokenSource? _cts;
    private Task? _listenTask;
    private DateTimeOffset _lastHeartbeatTime = DateTimeOffset.MinValue;
    private BrowserHeartbeat? _lastHeartbeat;
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
    public BrowserHeartbeat? LastHeartbeat
    {
        get
        {
            if (_connections.IsEmpty) return _lastHeartbeat;
            var latest = _connections.Values.OrderByDescending(c => c.LastHeartbeat).FirstOrDefault();
            if (latest is null) return _lastHeartbeat;
            if (_lastHeartbeat is null) return new BrowserHeartbeat
            {
                Url = latest.LastUrl ?? string.Empty,
                Title = latest.LastTitle ?? string.Empty,
                Audible = latest.LastAudible ?? false,
                Incognito = latest.LastIncognito ?? false,
                TabCount = latest.LastTabCount ?? 0,
                Browser = latest.BrowserType,
                InstanceId = latest.InstanceId,
                Timestamp = latest.LastHeartbeat.ToString("O")
            };
            return _lastHeartbeat;
        }
    }
    public DateTimeOffset LastHeartbeatTime
    {
        get
        {
            if (_connections.IsEmpty) return _lastHeartbeatTime;
            var latest = _connections.Values.Max(c => c.LastHeartbeat);
            return latest > _lastHeartbeatTime ? latest : _lastHeartbeatTime;
        }
    }
    public bool IsConnected => _connections.IsEmpty
        ? (_lastHeartbeat is not null && (DateTimeOffset.UtcNow - _lastHeartbeatTime).TotalSeconds < 120)
        : _connections.Values.Any(c => c.IsConnected);

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
        var instanceId = string.IsNullOrWhiteSpace(hb.InstanceId) ? "unknown" : hb.InstanceId;
        var browserType = string.IsNullOrWhiteSpace(hb.Browser) ? "other" : hb.Browser.ToLowerInvariant();
        hb.Browser = browserType;
        hb.InstanceId = instanceId;

        var conn = _connections.GetOrAdd(instanceId, _ => new BrowserConnection
        {
            InstanceId = instanceId,
            BrowserType = browserType,
            DisplayName = BuildDisplayName(hb),
            FirstSeen = DateTimeOffset.UtcNow,
        });
        conn.BrowserType = browserType;
        conn.IsConnected = true;
        conn.LastHeartbeat = DateTimeOffset.UtcNow;
        conn.LastUrl = hb.Url;
        conn.LastTitle = hb.Title;
        conn.LastAudible = hb.Audible;
        conn.LastTabCount = hb.TabCount;
        conn.LastIncognito = hb.Incognito;
        conn.HeartbeatCount++;

        RebuildDisplayNames(browserType);

        _lastHeartbeat = hb;
        _lastHeartbeatTime = DateTimeOffset.UtcNow;
        _channel.Writer.TryWrite(hb);
        _logger?.Debug("BrowserBridge", $"Heartbeat {hb.Domain} browser={hb.Browser} instance={instanceId} audible={hb.Audible} tabs={hb.TabCount}");
    }

    private void RebuildDisplayNames(string browserType)
    {
        var same = _connections.Values.Where(c => c.BrowserType == browserType).ToList();
        var count = same.Count;
        foreach (var c in same)
        {
            c.DisplayName = BuildDisplayNameForConnection(c, count);
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
        var shortId = hb.InstanceId.Length > 4 ? hb.InstanceId[^4..] : hb.InstanceId;
        var sameTypeCount = _connections.Values.Count(c => c.BrowserType == hb.Browser) + 1;
        if (sameTypeCount <= 1) return type;
        return $"{type} ({shortId})";
    }

    public void CheckConnections()
    {
        foreach (var conn in _connections.Values)
        {
            var silentSeconds = (DateTimeOffset.UtcNow - conn.LastHeartbeat).TotalSeconds;
            if (conn.IsConnected && silentSeconds > 120)
            {
                conn.IsConnected = false;
                _logger?.Warn("BrowserBridge", $"BrowserBridge {conn.DisplayName} disconnected, silent for {silentSeconds:F0}s");
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
                using (var reader = new StreamReader(req.InputStream, req.ContentEncoding ?? Encoding.UTF8))
                    body = await reader.ReadToEndAsync().ConfigureAwait(false);

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
