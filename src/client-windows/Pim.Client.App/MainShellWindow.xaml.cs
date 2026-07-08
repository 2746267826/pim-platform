using System.Windows;
using System.Windows.Controls;
using Microsoft.Extensions.DependencyInjection;
using Pim.Client.Core.Services;

namespace Pim.Client.App;

public partial class MainShellWindow : Window
{
    private readonly ApiClient _apiClient;
    private readonly AuthService _authService;
    private readonly AwCollectorService _awCollector;
    private readonly KeyStatsCollectorService _keyStatsCollector;
    private readonly EmbeddedWebViewHost _webHost;
    private string _currentRoute = "/today";

    public MainShellWindow()
    {
        InitializeComponent();

        _apiClient = App.Services.GetRequiredService<ApiClient>();
        _authService = App.Services.GetRequiredService<AuthService>();
        _awCollector = App.Services.GetRequiredService<AwCollectorService>();
        _keyStatsCollector = App.Services.GetRequiredService<KeyStatsCollectorService>();
        _webHost = new EmbeddedWebViewHost(_apiClient, _authService);

        WebHostSlot.Content = _webHost.View;

        var config = DaemonConfig.Load();
        ServerUrlBox.Text = config.ServerUrl;
        RefreshShellState();
    }

    private async void OnLoaded(object sender, RoutedEventArgs e)
    {
        await NavigateToAsync(_currentRoute);
    }

    public void OpenRoute(string route)
    {
        _ = NavigateToAsync(route).ContinueWith(
            task => Services.Logger.Warn($"Shell navigation failed: {task.Exception?.GetBaseException().Message ?? "unknown error"}"),
            TaskContinuationOptions.OnlyOnFaulted);
    }

    private async void OnNavigateRoute(object sender, RoutedEventArgs e)
    {
        if (sender is Button { Tag: string route })
        {
            await NavigateToAsync(route);
        }
    }

    private async void OnRefresh(object sender, RoutedEventArgs e)
    {
        await NavigateToAsync(_currentRoute);
        RefreshShellState();
    }

    private async Task NavigateToAsync(string route)
    {
        _currentRoute = route;
        CurrentRouteText.Text = route;
        await _webHost.NavigateAsync(route);
    }

    private void OnSaveServerUrl(object sender, RoutedEventArgs e)
    {
        var normalized = ApiClient.NormalizeServerUrl(ServerUrlBox.Text.Trim());
        if (!Uri.TryCreate(normalized, UriKind.Absolute, out var uri) ||
            (uri.Scheme != Uri.UriSchemeHttp && uri.Scheme != Uri.UriSchemeHttps))
        {
            MessageBox.Show("请输入有效的服务器地址，例如 http://127.0.0.1:5858。", "PIM", MessageBoxButton.OK, MessageBoxImage.Warning);
            return;
        }

        _apiClient.SetBaseUrl(normalized);
        _authService.ServerUrl = normalized;

        var config = DaemonConfig.Load();
        config.ServerUrl = normalized;
        config.Save();
        ServerUrlBox.Text = normalized;
        RefreshShellState();
    }

    private void OnOpenStatusWindow(object sender, RoutedEventArgs e)
    {
        var existing = Application.Current.Windows.OfType<StatusWindow>().FirstOrDefault();
        if (existing is not null)
        {
            existing.Activate();
            return;
        }

        new StatusWindow().Show();
    }

    private void RefreshShellState()
    {
        AccountStateText.Text = _authService.IsAuthenticated
            ? $"账户状态：{_authService.CurrentUsername} 已登录"
            : "账户状态：未登录";
        UploadStateText.Text =
            $"采集上传状态：ActivityWatch 队列 {_awCollector.QueueCount} 条；" +
            $"AW 错误 {_awCollector.LastUploadError ?? "无"}；" +
            $"KeyStats 错误 {_keyStatsCollector.LastUploadError ?? "无"}";
    }
}
