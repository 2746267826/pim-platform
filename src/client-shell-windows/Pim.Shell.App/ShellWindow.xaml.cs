using System;
using System.Net.Http.Json;
using System.Windows;
using Microsoft.Web.WebView2.Core;

namespace Pim.Shell.App;

public partial class ShellWindow : Window
{
    private readonly string _serverUrl;
    private string? _updateUrl;

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

        _ = Task.Run(async () => {
            try {
                using var http = new HttpClient { Timeout = TimeSpan.FromSeconds(3) };
                var latest = await http.GetFromJsonAsync<LatestDto>($"{_serverUrl.TrimEnd('/')}/api/client/shell/latest");
                if (latest?.windowsVersion != null && UpdateChecker.IsNewer("0.1.0", latest.windowsVersion) && !string.IsNullOrWhiteSpace(latest.windowsUrl))
                    Dispatcher.Invoke(() => { UpdateText.Text = $"发现新版 {latest.windowsVersion}"; UpdateBar.Visibility = Visibility.Visible; _updateUrl = latest.windowsUrl; });
            } catch { }
        });
    }

    private void OnUpdateClick(object s, RoutedEventArgs e) { if (_updateUrl != null) System.Diagnostics.Process.Start(new System.Diagnostics.ProcessStartInfo(_updateUrl) { UseShellExecute = true }); }
    private record LatestDto(string? windowsVersion, string? windowsUrl, string? androidVersion, string? androidUrl);

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
