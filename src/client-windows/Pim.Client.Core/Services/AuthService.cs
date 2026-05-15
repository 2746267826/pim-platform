using System.Text.Json;

namespace Pim.Client.Core.Services;

public class AuthService
{
    private readonly ApiClient _apiClient;
    private string? _accessToken;
    private string? _refreshToken;
    private DateTimeOffset _accessTokenExpiry;

    public AuthService(ApiClient apiClient)
    {
        _apiClient = apiClient;
    }

    public bool IsAuthenticated => !string.IsNullOrEmpty(_accessToken) &&
                                    DateTimeOffset.UtcNow < _accessTokenExpiry;

    public async Task<bool> LoginAsync(string username, string password)
    {
        var result = await _apiClient.PostAsync<JsonElement>("/auth/login",
            new { username, password });

        if (result.ValueKind == JsonValueKind.Undefined) return false;

        var data = result.GetProperty("data");
        _accessToken = data.GetProperty("accessToken").GetString()!;
        _refreshToken = data.GetProperty("refreshToken").GetString()!;
        _accessTokenExpiry = data.GetProperty("expiresAt").GetDateTimeOffset();

        _apiClient.SetAccessToken(_accessToken);
        return true;
    }

    public async Task<bool> RegisterAsync(
        string username, string email, string password, string? displayName)
    {
        var result = await _apiClient.PostAsync<JsonElement>("/auth/register",
            new { username, email, password, displayName });
        return result.ValueKind != JsonValueKind.Undefined && result.GetProperty("code").GetInt32() == 0;
    }

    public async Task<bool> RefreshAsync()
    {
        if (string.IsNullOrEmpty(_refreshToken)) return false;

        var result = await _apiClient.PostAsync<JsonElement>("/auth/refresh",
            new { refreshToken = _refreshToken });

        if (result.ValueKind == JsonValueKind.Undefined) return false;

        var data = result.GetProperty("data");
        _accessToken = data.GetProperty("accessToken").GetString()!;
        _refreshToken = data.GetProperty("refreshToken").GetString()!;
        _accessTokenExpiry = data.GetProperty("expiresAt").GetDateTimeOffset();

        _apiClient.SetAccessToken(_accessToken);
        return true;
    }
}
