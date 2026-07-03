using System.Diagnostics;
using System.Net;
using System.Net.Http.Headers;
using System.Net.Http.Json;
using Pim.Client.Core;

namespace Pim.Client.Core.Services;

public class ApiClient
{
    private readonly HttpClient _httpClient;
    private volatile bool _isRefreshing;

    public Func<Task<bool>>? OnUnauthorized { get; set; }

    public event Action<string, long>? RequestTiming;

    public ApiClient()
    {
        var handler = new HttpClientHandler
        {
            UseProxy = false
        };
        _httpClient = new HttpClient(handler)
        {
            BaseAddress = new Uri($"{ClientDefaults.DefaultServerUrl}/api/v1/")
        };
    }

    public void SetBaseUrl(string baseUrl)
    {
        var normalized = NormalizeServerUrl(baseUrl);
        var uri = new Uri(normalized.TrimEnd('/') + "/api/v1/");

        // Build a fresh HttpClient so we can safely change the base address
        // even after the previous client has started sending requests.
        var handler = new HttpClientHandler { UseProxy = false };
        var newClient = new HttpClient(handler) { BaseAddress = uri };

        // Preserve the current auth header
        if (_httpClient.DefaultRequestHeaders.Authorization is { } auth)
            newClient.DefaultRequestHeaders.Authorization = auth;

        // Atomically swap (safe because HttpClient disposal is immediate
        // on .NET — all in-flight operations are cancelled by the OS).
        Interlocked.Exchange(ref _httpClient, newClient).Dispose();
    }

    public string CurrentBaseUrl => _httpClient.BaseAddress?.ToString().TrimEnd('/') ?? "";

    private string Resolve(string endpoint)
    {
        return endpoint.TrimStart('/');
    }

    public static string NormalizeServerUrl(string baseUrl)
    {
        var trimmed = baseUrl.Trim().TrimEnd('/');
        if (!Uri.TryCreate(trimmed, UriKind.Absolute, out var uri))
        {
            return trimmed;
        }

        return uri.Host.Equals("localhost", StringComparison.OrdinalIgnoreCase)
            ? new UriBuilder(uri) { Host = "127.0.0.1" }.Uri.ToString().TrimEnd('/')
            : trimmed;
    }

    public void SetAccessToken(string token)
    {
        _httpClient.DefaultRequestHeaders.Authorization =
            new AuthenticationHeaderValue("Bearer", token);
    }

    public void ClearAccessToken()
    {
        _httpClient.DefaultRequestHeaders.Authorization = null;
    }

    public async Task<T?> GetAsync<T>(string endpoint, CancellationToken ct = default)
    {
        return await SendWithAuthRetryAsync<T>(
            () => _httpClient.GetAsync(Resolve(endpoint), ct), ct);
    }

    public async Task<T?> PostAsync<T>(string endpoint, object body, CancellationToken ct = default)
    {
        return await SendWithAuthRetryAsync<T>(
            () => _httpClient.PostAsJsonAsync(Resolve(endpoint), body, ct), ct);
    }

    public async Task<T?> PutAsync<T>(string endpoint, object body, CancellationToken ct = default)
    {
        return await SendWithAuthRetryAsync<T>(
            () => _httpClient.PutAsJsonAsync(Resolve(endpoint), body, ct), ct);
    }

    public async Task DeleteAsync(string endpoint, CancellationToken ct = default)
    {
        await SendWithAuthRetryAsync<IgnoreResult>(
            () => _httpClient.DeleteAsync(Resolve(endpoint), ct), ct);
    }

    public async Task<T?> PostStringAsync<T>(string endpoint, string content, CancellationToken ct = default)
    {
        return await SendWithAuthRetryAsync<T>(
            () => _httpClient.PostAsync(Resolve(endpoint),
                new StringContent(content, System.Text.Encoding.UTF8, "text/calendar"), ct), ct);
    }

    private async Task<T?> SendWithAuthRetryAsync<T>(
        Func<Task<HttpResponseMessage>> request, CancellationToken ct)
    {
        var sw = Stopwatch.StartNew();
        var response = await request();
        var firstHopMs = sw.ElapsedMilliseconds;

        if (response.StatusCode == HttpStatusCode.Unauthorized
            && OnUnauthorized is not null
            && !_isRefreshing)
        {
            _isRefreshing = true;
            try
            {
                if (await OnUnauthorized())
                {
                    sw.Restart();
                    response = await request();
                    RequestTiming?.Invoke($"{response.RequestMessage?.Method} {response.RequestMessage?.RequestUri} (after refresh)", sw.ElapsedMilliseconds);
                }
            }
            finally
            {
                _isRefreshing = false;
            }
        }
        else
        {
            RequestTiming?.Invoke($"{response.RequestMessage?.Method} {response.RequestMessage?.RequestUri}", firstHopMs);
        }

        response.EnsureSuccessStatusCode();

        if (typeof(T) == typeof(IgnoreResult))
            return default;

        return await response.Content.ReadFromJsonAsync<T>(ct);
    }

    private sealed class IgnoreResult { }
}
