using System.Diagnostics;
using System.Net.Http;
using System.Windows;
using Microsoft.Extensions.DependencyInjection;
using Pim.Client.Core.Services;
using MediaBrush = System.Windows.Media.Brush;

namespace Pim.Client.App;

public partial class StatusWindow : Window
{
    private static readonly HttpClient Http = new() { Timeout = TimeSpan.FromSeconds(3) };
    private readonly ApiClient _apiClient;
    private readonly AuthService _authService;
    private readonly AwCollectorService _awCollector;
    private readonly KeyStatsCollectorService _keyStatsCollector;

    internal sealed record StatusDiagnostic(
        string Name,
        string Summary,
        string Detail,
        MediaBrush ToneBrush);

    public StatusWindow()
    {
        InitializeComponent();

        _apiClient = App.Services.GetRequiredService<ApiClient>();
        _authService = App.Services.GetRequiredService<AuthService>();
        _awCollector = App.Services.GetRequiredService<AwCollectorService>();
        _keyStatsCollector = App.Services.GetRequiredService<KeyStatsCollectorService>();

        var config = DaemonConfig.Load();
        ServerUrlBox.Text = config.ServerUrl;
        AutoStartCheckBox.IsChecked = config.AutoStart;
        RefreshAll();
    }

    private void RefreshAll()
    {
        RefreshAuth();
        QueueRefreshStatus();
    }

    private void QueueRefreshStatus()
    {
        _ = RefreshStatusAsync().ContinueWith(
            task => Debug.WriteLine(task.Exception),
            TaskContinuationOptions.OnlyOnFaulted);
    }

    private void RefreshAuth()
    {
        if (_authService.IsAuthenticated)
        {
            AuthStatusText.Text = $"状态：运行中，{_authService.CurrentUsername} 已登录";
            LoginButton.Visibility = Visibility.Collapsed;
        }
        else
        {
            AuthStatusText.Text = "状态：运行中，账户未登录";
            LoginButton.Visibility = Visibility.Visible;
        }
    }

    private async Task RefreshStatusAsync()
    {
        var diagnostics = new List<StatusDiagnostic>
        {
            BuildAccountDiagnostic(),
            await ProbeAsync("KeyStats", "http://127.0.0.1:18080/api/stats/"),
            await ProbeAsync("ActivityWatch", "http://127.0.0.1:5600/api/0/buckets/")
        };

        diagnostics.Add(await BuildApiDiagnosticAsync());
        diagnostics.Add(BuildUploadQueueDiagnostic());

        StatusItems.ItemsSource = diagnostics;
    }

    private StatusDiagnostic BuildAccountDiagnostic()
    {
        var timestamp = DateTime.Now.ToString("yyyy-MM-dd HH:mm:ss");
        if (_authService.IsAuthenticated)
        {
            return new StatusDiagnostic(
                "Account",
                $"{_authService.CurrentUsername} 已登录",
                $"Username: {_authService.CurrentUsername}\nAuthenticated: true\nServer: {_authService.ServerUrl}\nTime: {timestamp}",
                BrushFor("ok"));
        }

        return new StatusDiagnostic(
            "Account",
            "未登录，上传到 PIM API 需要账户",
            $"Authenticated: false\nServer: {_authService.ServerUrl}\nAction: 请点击登录完成授权。\nTime: {timestamp}",
            BrushFor("warn"));
    }

    private async Task<StatusDiagnostic> BuildApiDiagnosticAsync()
    {
        try
        {
            var apiRoot = _apiClient.CurrentBaseUrl.TrimEnd('/');
            if (apiRoot.EndsWith("/api/v1", StringComparison.OrdinalIgnoreCase))
            {
                apiRoot = apiRoot[..^"/api/v1".Length];
            }

            var apiUrl = string.IsNullOrWhiteSpace(apiRoot)
                ? DaemonConfig.Load().ServerUrl.TrimEnd('/') + "/health"
                : apiRoot + "/health";

            return await ProbeAsync("PIM API", apiUrl);
        }
        catch (Exception ex)
        {
            var timestamp = DateTime.Now.ToString("yyyy-MM-dd HH:mm:ss");
            return new StatusDiagnostic(
                "PIM API",
                "检查失败，无法生成健康检查地址",
                $"BaseUrl: {_apiClient.CurrentBaseUrl}\nError: {ex.GetType().Name}\nMessage: {ex.Message}\nTime: {timestamp}",
                BrushFor("error"));
        }
    }

    private async Task<StatusDiagnostic> ProbeAsync(string name, string url)
    {
        var timestamp = DateTime.Now.ToString("yyyy-MM-dd HH:mm:ss");
        try
        {
            using var resp = await Http.GetAsync(url);
            var summary = resp.IsSuccessStatusCode
                ? "已连接"
                : $"HTTP {(int)resp.StatusCode} {resp.ReasonPhrase ?? resp.StatusCode.ToString()}";
            var detail =
                $"URL: {url}\nStatus: {(int)resp.StatusCode} {resp.StatusCode}\nError: {(resp.IsSuccessStatusCode ? "none" : "HTTP")}\nMessage: {resp.ReasonPhrase}\nTime: {timestamp}";

            return new StatusDiagnostic(name, summary, detail, BrushFor(resp.IsSuccessStatusCode ? "ok" : "warn"));
        }
        catch (Exception ex)
        {
            var detail = $"URL: {url}\nStatus: Exception\nError: {ex.GetType().Name}\nMessage: {ex.Message}\nTime: {timestamp}";
            return new StatusDiagnostic(name, "未连接", detail, BrushFor("error"));
        }
    }

    private StatusDiagnostic BuildUploadQueueDiagnostic()
    {
        var queueCount = _awCollector.QueueCount;
        var awSummary = FormatUploadSummary("ActivityWatch", _awCollector.LastUploadTime, _awCollector.LastUploadError);
        var keyStatsSummary = FormatUploadSummary("KeyStats", _keyStatsCollector.LastUploadTime, _keyStatsCollector.LastUploadError);
        var hasErrors = _awCollector.LastUploadError is not null || _keyStatsCollector.LastUploadError is not null;

        var summary = hasErrors
            ? $"有最近上传错误，ActivityWatch 队列 {queueCount} 条"
            : queueCount > 0
                ? $"ActivityWatch 队列 {queueCount} 条待上传"
                : "队列为空，最近上传正常";

        var detail =
            $"ActivityWatch QueueCount: {queueCount}\n{awSummary}\n{keyStatsSummary}\nTime: {DateTime.Now:yyyy-MM-dd HH:mm:ss}";

        return new StatusDiagnostic("Upload Queue", summary, detail, BrushFor(hasErrors ? "error" : queueCount > 0 ? "warn" : "ok"));
    }

    private static string FormatUploadSummary(string name, DateTime? lastUploadTime, string? lastUploadError)
    {
        var lastUpload = lastUploadTime is DateTime last
            ? $"{last:yyyy-MM-dd HH:mm:ss} ({FormatAgo(last)})"
            : "暂无记录";
        var error = string.IsNullOrWhiteSpace(lastUploadError) ? "无" : lastUploadError;
        return $"{name} LastUpload: {lastUpload}\n{name} LastError: {error}";
    }

    private MediaBrush BrushFor(string tone) => tone switch
    {
        "ok" => (MediaBrush)FindResource("PimActivityBrush"),
        "warn" => (MediaBrush)FindResource("PimWarningBrush"),
        "error" => (MediaBrush)FindResource("PimDangerBrush"),
        _ => (MediaBrush)FindResource("PimMutedTextBrush")
    };

    private static string FormatAgo(DateTime last)
    {
        var ago = DateTime.Now - last;
        if (ago.TotalMinutes < 1) return "刚刚";
        if (ago.TotalHours < 1) return $"{(int)ago.TotalMinutes} 分钟前";
        if (ago.TotalDays < 1) return $"{(int)ago.TotalHours} 小时前";
        return $"{(int)ago.TotalDays} 天前";
    }

    private void OnSaveServerUrl(object sender, RoutedEventArgs e)
    {
        var url = ServerUrlBox.Text.Trim();
        if (string.IsNullOrEmpty(url)) return;
        var normalizedUrl = ApiClient.NormalizeServerUrl(url);

        if (!Uri.TryCreate(normalizedUrl, UriKind.Absolute, out var uri) ||
            (uri.Scheme != Uri.UriSchemeHttp && uri.Scheme != Uri.UriSchemeHttps))
        {
            MessageBox.Show("请输入有效的服务器地址，例如 http://localhost:5858 或 https://example.com。", "PIM", MessageBoxButton.OK, MessageBoxImage.Warning);
            return;
        }

        try
        {
            _apiClient.SetBaseUrl(normalizedUrl);
            _authService.ServerUrl = normalizedUrl;

            var config = DaemonConfig.Load();
            config.ServerUrl = normalizedUrl;
            config.Save();
            ServerUrlBox.Text = normalizedUrl;
        }
        catch (Exception ex)
        {
            MessageBox.Show($"服务器地址未保存：{ex.Message}", "PIM", MessageBoxButton.OK, MessageBoxImage.Error);
            return;
        }

        MessageBox.Show($"服务器地址已更新为：\n{url}", "PIM", MessageBoxButton.OK, MessageBoxImage.Information);
        RefreshAll();
    }

    private void OnLogin(object sender, RoutedEventArgs e)
    {
        var loginWindow = new LoginWindow();
        var result = loginWindow.ShowDialog();
        if (result == true)
        {
            RefreshAll();
        }
    }

    private async void OnManualSync(object sender, RoutedEventArgs e)
    {
        Debug.WriteLine("Manual sync triggered");
        ManualSyncButton.IsEnabled = false;
        ManualSyncButton.Content = "同步中...";
        try
        {
            await Task.WhenAll(_awCollector.SyncNowAsync(), _keyStatsCollector.SyncNowAsync());
            await RefreshStatusAsync();

            var uploadErrors = BuildUploadErrorMessage(_awCollector.LastUploadError, _keyStatsCollector.LastUploadError);
            if (uploadErrors is not null)
            {
                MessageBox.Show($"同步已执行，但上传仍有错误：\n{uploadErrors}", "PIM", MessageBoxButton.OK, MessageBoxImage.Warning);
            }
            else
            {
                MessageBox.Show("同步完成", "PIM", MessageBoxButton.OK, MessageBoxImage.Information);
            }
        }
        catch (Exception ex)
        {
            await RefreshStatusAsync();
            MessageBox.Show($"同步失败：{ex.Message}", "PIM", MessageBoxButton.OK, MessageBoxImage.Error);
        }
        finally
        {
            ManualSyncButton.IsEnabled = true;
            ManualSyncButton.Content = "手动同步";
        }
    }

    private void OnViewLogs(object sender, RoutedEventArgs e)
    {
        var logPath = Services.Logger.LogFilePath;
        try
        {
            Process.Start("notepad.exe", logPath);
        }
        catch
        {
            MessageBox.Show($"日志文件：\n{logPath}", "PIM");
        }
    }

    private void OnClose(object sender, RoutedEventArgs e)
    {
        Close();
    }

    private static string? BuildUploadErrorMessage(string? awError, string? keyStatsError)
    {
        var errors = new List<string>();
        if (!string.IsNullOrWhiteSpace(awError))
        {
            errors.Add($"ActivityWatch: {awError}");
        }

        if (!string.IsNullOrWhiteSpace(keyStatsError))
        {
            errors.Add($"KeyStats: {keyStatsError}");
        }

        return errors.Count == 0 ? null : string.Join("\n", errors);
    }

    private void OnAutoStartToggled(object sender, RoutedEventArgs e)
    {
        var enabled = AutoStartCheckBox.IsChecked == true;
        AutoStartManager.Set(enabled);

        var config = DaemonConfig.Load();
        config.AutoStart = enabled;
        config.Save();
    }
}
