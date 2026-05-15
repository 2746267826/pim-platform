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
    }

    public bool IsAuthenticated => !string.IsNullOrEmpty(_accessToken) &&
                                    DateTimeOffset.UtcNow < _accessTokenExpiry;

    public string? CurrentUserId { get; private set; }
    public string? CurrentUsername { get; private set; }
    public string? CurrentDisplayName { get; private set; }

    public async Task<bool> LoginAsync(string username, string password)
    {
        var result = await _apiClient.PostAsync<ApiResponse<AuthResponse>>("/auth/login",
            new { username, password });

        if (result?.Data is null) return false;

        _accessToken = result.Data.AccessToken;
        _refreshToken = result.Data.RefreshToken;
        _accessTokenExpiry = result.Data.ExpiresAt;

        if (result.Data.UserInfo is not null)
        {
            CurrentUserId = result.Data.UserInfo.Id;
            CurrentUsername = result.Data.UserInfo.Username;
            CurrentDisplayName = result.Data.UserInfo.DisplayName;
        }

        _apiClient.SetAccessToken(_accessToken);
        return true;
    }

    public async Task<bool> RegisterAsync(
        string username, string email, string password, string? displayName)
    {
        var result = await _apiClient.PostAsync<ApiResponse<AuthResponse>>("/auth/register",
            new { username, email, password, displayName });
        return result is { Code: 0, Data: not null };
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
