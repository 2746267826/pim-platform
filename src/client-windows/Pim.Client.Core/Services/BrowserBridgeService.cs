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

    public BrowserBridgeService(int port = 15601, TrackerLogger? logger = null)
    {
        _port = port;
        _logger = logger;
        _channel = Channel.CreateUnbounded<BrowserHeartbeat>(new UnboundedChannelOptions { SingleReader = true, SingleWriter = false });
    }

    public ChannelReader<BrowserHeartbeat> Reader => _channel.Reader;
    public BrowserHeartbeat? LastHeartbeat => _lastHeartbeat;
    public DateTimeOffset LastHeartbeatTime => _lastHeartbeatTime;
    public bool IsConnected => _lastHeartbeat is not null && (DateTimeOffset.UtcNow - _lastHeartbeatTime).TotalSeconds < 120;

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
    }

    public void Stop()
    {
        try { _cts?.Cancel(); } catch { }
        try { _listener?.Stop(); } catch { }
        try { _listener?.Close(); } catch { }
        _listener = null;
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
                    _lastHeartbeat = hb;
                    _lastHeartbeatTime = DateTimeOffset.UtcNow;
                    _channel.Writer.TryWrite(hb);
                    _logger?.Debug("BrowserBridge", $"Heartbeat {hb.Domain} audible={hb.Audible} tabs={hb.TabCount}");
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
