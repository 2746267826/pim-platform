using System.Net;
using System.Net.Http.Headers;
using System.Net.Http.Json;

namespace Pim.Client.Core.Services;

public class ApiClient
{
    private readonly HttpClient _httpClient;
    private volatile bool _isRefreshing;

    public Func<Task<bool>>? OnUnauthorized { get; set; }

    public ApiClient()
    {
        _httpClient = new HttpClient
        {
            BaseAddress = new Uri("http://localhost:5000/api/v1")
        };
    }

    public void SetBaseUrl(string baseUrl)
    {
        _httpClient.BaseAddress = new Uri(baseUrl.TrimEnd('/') + "/api/v1");
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
            () => _httpClient.GetAsync(endpoint, ct), ct);
    }

    public async Task<T?> PostAsync<T>(string endpoint, object body, CancellationToken ct = default)
    {
        return await SendWithAuthRetryAsync<T>(
            () => _httpClient.PostAsJsonAsync(endpoint, body, ct), ct);
    }

    public async Task<T?> PutAsync<T>(string endpoint, object body, CancellationToken ct = default)
    {
        return await SendWithAuthRetryAsync<T>(
            () => _httpClient.PutAsJsonAsync(endpoint, body, ct), ct);
    }

    public async Task DeleteAsync(string endpoint, CancellationToken ct = default)
    {
        await SendWithAuthRetryAsync<IgnoreResult>(
            () => _httpClient.DeleteAsync(endpoint, ct), ct);
    }

    public async Task<T?> PostStringAsync<T>(string endpoint, string content, CancellationToken ct = default)
    {
        return await SendWithAuthRetryAsync<T>(
            () => _httpClient.PostAsync(endpoint,
                new StringContent(content, System.Text.Encoding.UTF8, "text/calendar"), ct), ct);
    }

    private async Task<T?> SendWithAuthRetryAsync<T>(
        Func<Task<HttpResponseMessage>> request, CancellationToken ct)
    {
        var response = await request();

        if (response.StatusCode == HttpStatusCode.Unauthorized
            && OnUnauthorized is not null
            && !_isRefreshing)
        {
            _isRefreshing = true;
            try
            {
                if (await OnUnauthorized())
                {
                    response = await request();
                }
            }
            finally
            {
                _isRefreshing = false;
            }
        }

        response.EnsureSuccessStatusCode();

        if (typeof(T) == typeof(IgnoreResult))
            return default;

        return await response.Content.ReadFromJsonAsync<T>(ct);
    }

    private sealed class IgnoreResult { }
}
