using System.Net.Http.Headers;
using System.Net.Http.Json;

namespace Pim.Client.Core.Services;

public class ApiClient
{
    private readonly HttpClient _httpClient;
    private const string ApiBaseUrl = "https://localhost:5001/api/v1";

    public ApiClient()
    {
        _httpClient = new HttpClient { BaseAddress = new Uri(ApiBaseUrl) };
    }

    public void SetAccessToken(string token)
    {
        _httpClient.DefaultRequestHeaders.Authorization =
            new AuthenticationHeaderValue("Bearer", token);
    }

    public async Task<T?> GetAsync<T>(string endpoint, CancellationToken ct = default)
    {
        var response = await _httpClient.GetAsync(endpoint, ct);
        response.EnsureSuccessStatusCode();
        return await response.Content.ReadFromJsonAsync<T>(ct);
    }

    public async Task<T?> PostAsync<T>(string endpoint, object body, CancellationToken ct = default)
    {
        var response = await _httpClient.PostAsJsonAsync(endpoint, body, ct);
        response.EnsureSuccessStatusCode();
        return await response.Content.ReadFromJsonAsync<T>(ct);
    }

    public async Task<T?> PutAsync<T>(string endpoint, object body, CancellationToken ct = default)
    {
        var response = await _httpClient.PutAsJsonAsync(endpoint, body, ct);
        response.EnsureSuccessStatusCode();
        return await response.Content.ReadFromJsonAsync<T>(ct);
    }

    public async Task DeleteAsync(string endpoint, CancellationToken ct = default)
    {
        var response = await _httpClient.DeleteAsync(endpoint, ct);
        response.EnsureSuccessStatusCode();
    }
}
