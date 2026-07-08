using System.Text.Json;
using Microsoft.Web.WebView2.Core;
using Microsoft.Web.WebView2.Wpf;
using Pim.Client.Core.Services;

namespace Pim.Client.App;

public sealed class EmbeddedWebViewHost
{
    private readonly ApiClient _apiClient;
    private readonly AuthService _authService;
    private bool _initialized;

    public EmbeddedWebViewHost(ApiClient apiClient, AuthService authService)
    {
        _apiClient = apiClient;
        _authService = authService;
        View = new WebView2();
    }

    public WebView2 View { get; }

    public async Task InitializeAsync()
    {
        if (_initialized)
        {
            return;
        }

        await View.EnsureCoreWebView2Async();
        if (View.CoreWebView2 is not null)
        {
            View.CoreWebView2.Settings.AreDefaultContextMenusEnabled = true;
            View.CoreWebView2.Settings.AreDevToolsEnabled = true;
            View.CoreWebView2.NewWindowRequested += OnNewWindowRequested;
            await InjectAuthTokenAsync();
        }

        _initialized = true;
    }

    public async Task NavigateAsync(string route)
    {
        await InitializeAsync();
        await InjectAuthTokenAsync();
        View.CoreWebView2?.Navigate(BuildWebUrl(route));
    }

    public string BuildWebUrl(string route)
    {
        var root = _apiClient.CurrentBaseUrl.TrimEnd('/');
        if (root.EndsWith("/api/v1", StringComparison.OrdinalIgnoreCase))
        {
            root = root[..^"/api/v1".Length];
        }

        if (string.IsNullOrWhiteSpace(route))
        {
            route = "/today";
        }

        return $"{root}/{route.TrimStart('/')}";
    }

    public async Task InjectAuthTokenAsync()
    {
        if (View.CoreWebView2 is null || string.IsNullOrWhiteSpace(_authService.CurrentAccessToken))
        {
            return;
        }

        var tokenJson = JsonSerializer.Serialize(_authService.CurrentAccessToken);
        var script = $"localStorage.setItem('accessToken', {tokenJson});";

        await View.CoreWebView2.AddScriptToExecuteOnDocumentCreatedAsync(script);
        await View.CoreWebView2.ExecuteScriptAsync(script);
    }

    private static void OnNewWindowRequested(object? sender, CoreWebView2NewWindowRequestedEventArgs e)
    {
        if (sender is CoreWebView2 webView)
        {
            e.Handled = true;
            webView.Navigate(e.Uri);
        }
    }
}
