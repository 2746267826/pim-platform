using System;
using System.Net.Http.Json;
using System.Reflection;
using System.Threading;
using System.Windows;
using Microsoft.Web.WebView2.Core;
using Pim.Client.Core.Services;
using Pim.Shell.App.Services;

namespace Pim.Shell.App;

public partial class ShellWindow : Window
{
    private readonly string _serverUrl;
    private string? _updateUrl;
    private string _currentVersion = typeof(ShellWindow).Assembly.GetCustomAttributes(false).OfType<AssemblyInformationalVersionAttribute>().FirstOrDefault()?.InformationalVersion ?? "0.0.0-local";
    private readonly PeriodicTimer _updateTimer = new(TimeSpan.FromHours(6));
    private readonly CancellationTokenSource _updateCts = new();
    private int _checking;

    public ShellWindow(string serverUrl)
    {
        InitializeComponent();
        _serverUrl = serverUrl;
        Loaded += async (_, _) => await InitializeAsync();
        StateChanged += OnStateChanged;
    }

    private async Task InitializeAsync()
    {
        await Web.EnsureCoreWebView2Async();
        Web.CoreWebView2.Settings.AreDefaultContextMenusEnabled = true;
        Web.CoreWebView2.Settings.AreDevToolsEnabled = true;
        await Web.CoreWebView2.AddScriptToExecuteOnDocumentCreatedAsync(ShellBridge.Script);
        Web.CoreWebView2.DocumentTitleChanged += (_, _) =>
            Title = string.IsNullOrWhiteSpace(Web.CoreWebView2.DocumentTitle) ? "PIM" : Web.CoreWebView2.DocumentTitle;
        Web.CoreWebView2.NavigationCompleted += OnNavigationCompleted;
        Web.CoreWebView2.Navigate(_serverUrl);

        _ = Task.Run(async () =>
        {
            try { await Task.Delay(TimeSpan.FromSeconds(3), _updateCts.Token); } catch (OperationCanceledException) { return; }
            await CheckUpdateAsync();
            try { while (await _updateTimer.WaitForNextTickAsync(_updateCts.Token)) await CheckUpdateAsync(); } catch (OperationCanceledException) { } catch (ObjectDisposedException) { }
        });
    }

    private async Task CheckUpdateAsync()
    {
        if (Interlocked.Exchange(ref _checking, 1) == 1) return;
        try
        {
            using var http = new HttpClient { Timeout = TimeSpan.FromSeconds(5) };
            var latest = await http.GetFromJsonAsync<LatestDto>($"{_serverUrl.TrimEnd('/')}/api/client/shell/latest");
            if (latest?.error != null) { Logger.Warn($"Update check failed: {latest.error} checkedAt={latest.checkedAt}"); Dispatcher.Invoke(() => { UpdateText.Text = $"检查失败: {latest.error} checkedAt={latest.checkedAt}"; UpdateBar.Visibility = Visibility.Visible; UpdateButton.Visibility = Visibility.Collapsed; _updateUrl = null; }); return; }
            // Prefer shell-specific version/url if present, fallback to classic windows for backward compat
            var remoteVersion = latest?.shellWindowsVersion ?? latest?.windowsVersion;
            var remoteUrl = latest?.shellWindowsUrl ?? latest?.windowsUrl;
            if (remoteVersion != null && UpdateChecker.IsNewer(_currentVersion, remoteVersion) && !string.IsNullOrWhiteSpace(remoteUrl))
            {
                Logger.Info($"Update available current={_currentVersion} latest={remoteVersion}");
                Dispatcher.Invoke(() => { UpdateText.Text = $"发现新版 {remoteVersion}"; UpdateBar.Visibility = Visibility.Visible; UpdateButton.Visibility = Visibility.Visible; _updateUrl = remoteUrl; });
            }
            else
            {
                Logger.Info($"Update check no update current={_currentVersion} latest={remoteVersion} checkedAt={latest?.checkedAt}");
            }
        }
        catch (Exception ex) { Logger.Warn($"Update check exception: {ex.Message}", ex); Dispatcher.Invoke(() => { UpdateText.Text = $"检查异常: {ex.Message}"; UpdateBar.Visibility = Visibility.Visible; UpdateButton.Visibility = Visibility.Collapsed; _updateUrl = null; }); }
        finally { Interlocked.Exchange(ref _checking, 0); }
    }

    private void OnUpdateClick(object s, RoutedEventArgs e)
    {
        if (_updateUrl != null && Uri.TryCreate(_updateUrl, UriKind.Absolute, out var uri) && (uri.Scheme == Uri.UriSchemeHttp || uri.Scheme == Uri.UriSchemeHttps))
            System.Diagnostics.Process.Start(new System.Diagnostics.ProcessStartInfo(uri.AbsoluteUri) { UseShellExecute = true });
        else
            Logger.Warn($"Invalid update URL: {_updateUrl}");
    }

    private async void OnCheckUpdateClick(object sender, RoutedEventArgs e)
    {
        CheckButton.IsEnabled = false;
        try { await CheckUpdateAsync(); } finally { CheckButton.IsEnabled = true; }
    }

    protected override void OnClosed(EventArgs e)
    {
        try { _updateCts.Cancel(); } catch { }
        _updateCts.Dispose();
        _updateTimer.Dispose();
        base.OnClosed(e);
    }
    private record LatestDto(string? windowsVersion, string? windowsUrl, string? androidVersion, string? androidUrl, string? shellWindowsVersion, string? shellWindowsUrl, string? shellAndroidVersion, string? shellAndroidUrl, string? error, string? checkedAt);

    private void OnNavigationCompleted(object? sender, CoreWebView2NavigationCompletedEventArgs e)
        => ErrorOverlay.Visibility = e.IsSuccess ? Visibility.Collapsed : Visibility.Visible;

    private void OnRetryClick(object sender, RoutedEventArgs e)
    {
        ErrorOverlay.Visibility = Visibility.Collapsed;
        Web.CoreWebView2.Navigate(_serverUrl);
    }

    private void OnChangeServerClick(object sender, RoutedEventArgs e)
    {
        new SetupWindow().Show();
        Close();
    }

    private void OnStateChanged(object? sender, EventArgs e)
    {
        if (WindowState == WindowState.Minimized) Hide();
    }
}
