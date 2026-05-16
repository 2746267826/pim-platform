using Pim.Client.Core.Models;

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
        _apiClient.RequestTiming += (desc, ms) =>
            System.Diagnostics.Debug.WriteLine($"[ApiTiming] {desc} took {ms}ms");
    }

    public bool IsAuthenticated => !string.IsNullOrEmpty(_accessToken) &&
                                    DateTimeOffset.UtcNow < _accessTokenExpiry;

    public string? CurrentUserId { get; private set; }
    public string? CurrentUsername { get; private set; }
    public string? CurrentDisplayName { get; private set; }

    public string ServerUrl
    {
        get => _apiClient.CurrentBaseUrl;
        set => _apiClient.SetBaseUrl(value);
    }

    public async Task<bool> LoginAsync(string username, string password)
    {
        var result = await _apiClient.PostAsync<ApiResponse<AuthResponse>>("/auth/login",
            new { username, password });

        if (result?.Data is null) return false;

        ApplyTokens(result.Data);
        return true;
    }

    public async Task<string?> RegisterAsync(
        string username, string email, string password, string? displayName)
    {
        var result = await _apiClient.PostAsync<ApiResponse<AuthResponse>>("/auth/register",
            new { username, email, password, displayName });

        if (result is { Code: 0, Data: not null })
        {
            ApplyTokens(result.Data);
            return null;
        }

        return result is null
            ? "服务器无响应"
            : $"错误码 {result.Code}: {result.Message}";
    }

    private void ApplyTokens(AuthResponse data)
    {
        _accessToken = data.AccessToken;
        _refreshToken = data.RefreshToken;
        _accessTokenExpiry = data.ExpiresAt;

        if (data.UserInfo is not null)
        {
            CurrentUserId = data.UserInfo.Id;
            CurrentUsername = data.UserInfo.Username;
            CurrentDisplayName = data.UserInfo.DisplayName;
        }

        _apiClient.SetAccessToken(_accessToken);
        _apiClient.OnUnauthorized = RefreshAsync;
    }

    public async Task<bool> RefreshAsync()
    {
        if (string.IsNullOrEmpty(_refreshToken)) return false;

        var result = await _apiClient.PostAsync<ApiResponse<AuthResponse>>("/auth/refresh",
            new { refreshToken = _refreshToken });

        if (result?.Data is null) return false;

        _accessToken = result.Data.AccessToken;
        _refreshToken = result.Data.RefreshToken;
        _accessTokenExpiry = result.Data.ExpiresAt;

        _apiClient.SetAccessToken(_accessToken);
        return true;
    }

    public void Logout()
    {
        _accessToken = null;
        _refreshToken = null;
        CurrentUserId = null;
        CurrentUsername = null;
        CurrentDisplayName = null;
        _apiClient.ClearAccessToken();
    }
}
