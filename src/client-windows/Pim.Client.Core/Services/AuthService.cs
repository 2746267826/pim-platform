using System.Security.Cryptography;
using System.Text;
using System.Text.Json;
using Pim.Client.Core.Models;

namespace Pim.Client.Core.Services;

public class AuthService
{
    private readonly ApiClient _apiClient;
    private readonly string _tokenPath;
    private readonly string _tokenDir;
    private string? _accessToken;
    private string? _refreshToken;
    private DateTimeOffset _accessTokenExpiry;

    public AuthService(ApiClient apiClient, string? tokenPath = null)
    {
        _apiClient = apiClient;
        _tokenPath = tokenPath ?? Path.Combine(
            Environment.GetFolderPath(Environment.SpecialFolder.LocalApplicationData),
            "PIM", "token.json");
        _tokenDir = Path.GetDirectoryName(_tokenPath) ?? "";
        _apiClient.RequestTiming += (desc, ms) =>
            System.Diagnostics.Debug.WriteLine($"[ApiTiming] {desc} took {ms}ms");
    }

    public bool IsAuthenticated => !string.IsNullOrEmpty(_accessToken) &&
                                    DateTimeOffset.UtcNow < _accessTokenExpiry;

    public bool HasSavedToken => File.Exists(_tokenPath);
    public bool IsTokenExpired => !string.IsNullOrEmpty(_accessToken) && DateTimeOffset.UtcNow >= _accessTokenExpiry;

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

    public async Task<TokenRestoreResult> TryRestoreTokenDetailedAsync()
    {
        try
        {
            if (!File.Exists(_tokenPath)) return TokenRestoreResult.NoSavedToken;

            var data = await LoadPersistedTokenAsync(_tokenPath);
            if (data is null) return TokenRestoreResult.NoSavedToken;

            _accessToken = data.AccessToken;
            _refreshToken = data.RefreshToken;
            _accessTokenExpiry = data.ExpiresAt;
            CurrentUserId = data.UserId;
            CurrentUsername = data.Username;
            CurrentDisplayName = data.DisplayName;

            if (!string.IsNullOrEmpty(_accessToken))
            {
                _apiClient.SetAccessToken(_accessToken);
            }
            _apiClient.OnUnauthorized = RefreshAsync;

            // If token not expired, restored successfully
            if (IsAuthenticated)
                return TokenRestoreResult.Success;

            // If token expired, try refresh
            if (!string.IsNullOrEmpty(_refreshToken))
            {
                try
                {
                    var refreshed = await RefreshAsync();
                    if (refreshed)
                    {
                        return TokenRestoreResult.Success;
                    }
                    if (!HasSavedToken || string.IsNullOrEmpty(_refreshToken))
                    {
                        return TokenRestoreResult.InvalidCredentials;
                    }
                    return TokenRestoreResult.PendingNetwork;
                }
                catch (System.Net.Http.HttpRequestException ex) when (ex.StatusCode == System.Net.HttpStatusCode.Unauthorized)
                {
                    Logout();
                    return TokenRestoreResult.InvalidCredentials;
                }
                catch
                {
                    return TokenRestoreResult.PendingNetwork;
                }
            }

            return TokenRestoreResult.NoSavedToken;
        }
        catch
        {
            return TokenRestoreResult.NoSavedToken;
        }
    }

    public async Task<bool> TryRestoreTokenAsync()
    {
        var result = await TryRestoreTokenDetailedAsync();
        return result == TokenRestoreResult.Success;
    }

    private void SaveToken()
    {
        try
        {
            if (!string.IsNullOrEmpty(_tokenDir))
            {
                Directory.CreateDirectory(_tokenDir);
            }
            var data = new PersistedToken
            {
                AccessToken = _accessToken!,
                RefreshToken = _refreshToken!,
                ExpiresAt = _accessTokenExpiry,
                UserId = CurrentUserId,
                Username = CurrentUsername,
                DisplayName = CurrentDisplayName
            };
            var json = JsonSerializer.Serialize(data);
            var bytes = Encoding.UTF8.GetBytes(json);
            byte[] toWrite;
            if (OperatingSystem.IsWindows())
            {
                try
                {
                    toWrite = ProtectedData.Protect(bytes, null, DataProtectionScope.CurrentUser);
                }
                catch
                {
                    toWrite = bytes;
                }
            }
            else
            {
                // Non-Windows fallback: plaintext (encrypted storage unavailable)
                toWrite = bytes;
            }
            File.WriteAllBytes(_tokenPath, toWrite);
        }
        catch
        {
            // Best-effort save — don't crash if disk is full
        }
    }

    private static async Task<PersistedToken?> LoadPersistedTokenAsync(string tokenPath)
    {
        var raw = await File.ReadAllBytesAsync(tokenPath);
        // Try DPAPI unprotect on Windows; fallback to plaintext
        if (OperatingSystem.IsWindows())
        {
            try
            {
                var unprotected = ProtectedData.Unprotect(raw, null, DataProtectionScope.CurrentUser);
                var json = Encoding.UTF8.GetString(unprotected);
                var data = JsonSerializer.Deserialize<PersistedToken>(json);
                if (data is not null) return data;
            }
            catch
            {
                // Fall through to plaintext attempt (migration / fallback)
            }
        }
        try
        {
            var json = Encoding.UTF8.GetString(raw);
            return JsonSerializer.Deserialize<PersistedToken>(json);
        }
        catch
        {
            return null;
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

        try
        {
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
        catch (System.Net.Http.HttpRequestException ex) when (ex.StatusCode == System.Net.HttpStatusCode.Unauthorized)
        {
            Logout();
            return false;
        }
        catch (Exception ex)
        {
            System.Diagnostics.Debug.WriteLine($"[AuthService] Refresh failed due to transient/network error: {ex.Message}");
            return false;
        }
    }

    public void Logout()
    {
        _accessToken = null;
        _refreshToken = null;
        CurrentUserId = null;
        CurrentUsername = null;
        CurrentDisplayName = null;
        _apiClient.ClearAccessToken();
        try { File.Delete(_tokenPath); } catch { }
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
