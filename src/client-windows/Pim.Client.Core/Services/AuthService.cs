using System.Text.Json;
using Pim.Client.Core.Models;

namespace Pim.Client.Core.Services;

public class AuthService
{
    private readonly ApiClient _apiClient;
    private string? _accessToken;
    private string? _refreshToken;
    private DateTimeOffset _accessTokenExpiry;

    private static readonly string TokenDir = Path.Combine(
        Environment.GetFolderPath(Environment.SpecialFolder.LocalApplicationData),
        "PIM");
    private static readonly string TokenPath = Path.Combine(TokenDir, "token.json");

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
    public string? CurrentAccessToken => _accessToken;

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
        SaveToken();
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
            SaveToken();
            return null;
        }

        return result is null
            ? "服务器无响应"
            : $"错误码 {result.Code}: {result.Message}";
    }

    public async Task<bool> TryRestoreTokenAsync()
    {
        try
        {
            if (!File.Exists(TokenPath)) return false;

            var json = await File.ReadAllTextAsync(TokenPath);
            var data = JsonSerializer.Deserialize<PersistedToken>(json);
            if (data is null) return false;

            _accessToken = data.AccessToken;
            _refreshToken = data.RefreshToken;
            _accessTokenExpiry = data.ExpiresAt;
            CurrentUserId = data.UserId;
            CurrentUsername = data.Username;
            CurrentDisplayName = data.DisplayName;

            _apiClient.SetAccessToken(_accessToken!);
            _apiClient.OnUnauthorized = RefreshAsync;

            // If token expired, try refresh
            if (!IsAuthenticated)
                return await RefreshAsync();

            return true;
        }
        catch
        {
            return false;
        }
    }

    private void SaveToken()
    {
        try
        {
            Directory.CreateDirectory(TokenDir);
            var data = new PersistedToken
            {
                AccessToken = _accessToken!,
                RefreshToken = _refreshToken!,
                ExpiresAt = _accessTokenExpiry,
                UserId = CurrentUserId,
                Username = CurrentUsername,
                DisplayName = CurrentDisplayName
            };
            File.WriteAllText(TokenPath, JsonSerializer.Serialize(data));
        }
        catch
        {
            // Best-effort save — don't crash if disk is full
        }
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
        SaveToken();
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
        try { File.Delete(TokenPath); } catch { }
    }

    private class PersistedToken
    {
        public string AccessToken { get; set; } = string.Empty;
        public string RefreshToken { get; set; } = string.Empty;
        public DateTimeOffset ExpiresAt { get; set; }
        public string? UserId { get; set; }
        public string? Username { get; set; }
        public string? DisplayName { get; set; }
    }
}
