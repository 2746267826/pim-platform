using System.Diagnostics;
using System.IO;
using System.Net.Http;
using System.Text;
using System.Windows;
using Microsoft.Extensions.DependencyInjection;
using Pim.Client.Core;
using Pim.Client.Core.Models;
using Pim.Client.Core.Services;

namespace Pim.Client.App;

public partial class StatusWindow : Window
{
    private static readonly HttpClient Http = new() { Timeout = TimeSpan.FromSeconds(3) };
    private readonly ApiClient _apiClient;
    private readonly AuthService _authService;
    private readonly AwCollectorService _awCollector;
    private readonly KeyStatsCollectorService _keyStatsCollector;
    private readonly KeyStatsProcessManager _processManager;
    private readonly KeyStatsOneClickFixService _fixService;

    private string _lastDiagnosticsReport = string.Empty;
    private string _awState = "Unknown";
    private string _ksState = "Unknown";
    private string? _ksSkipReason;
    private bool _apiOk;

    public StatusWindow()
    {
        InitializeComponent();

        _apiClient = App.Services.GetRequiredService<ApiClient>();
        _authService = App.Services.GetRequiredService<AuthService>();
        _awCollector = App.Services.GetRequiredService<AwCollectorService>();
        _keyStatsCollector = App.Services.GetRequiredService<KeyStatsCollectorService>();
        _processManager = App.Services.GetRequiredService<KeyStatsProcessManager>();
        _fixService = new KeyStatsOneClickFixService(_processManager, new KeyStatsLocalStatsClient());

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
            AccountSummaryText.Text = $"{_authService.CurrentUsername} 已登录 · {_authService.ServerUrl}";
            LoginButton.Visibility = Visibility.Collapsed;
        }
        else
        {
            AccountSummaryText.Text = "未登录 · 上传到 PIM API 需要账户";
            LoginButton.Visibility = Visibility.Visible;
        }
    }

    private async Task RefreshStatusAsync()
    {
        var timestamp = DateTime.Now.ToString("yyyy-MM-dd HH:mm:ss");
        var sessionId = Process.GetCurrentProcess().SessionId;

        var apiDiag = await BuildApiProbeAsync();
        _apiOk = apiDiag.Ok;
        ApiConnectivityText.Text = apiDiag.Summary;

        var awProbe = await ProbeEndpointAsync("http://127.0.0.1:5600/api/0/buckets/");
        _awState = awProbe.Ok ? "Available" : "Unavailable";
        AwSummaryText.Text = awProbe.Ok ? "ActivityWatch 已连接" : "ActivityWatch 未连接";
        AwDetailText.Text =
            $"URL: http://127.0.0.1:5600/api/0/buckets/\n" +
            $"Status: {awProbe.StatusLine}\n" +
            $"Message: {awProbe.Message}\n" +
            $"Time: {timestamp}";

        var processes = _processManager.ListProcesses(sessionId);
        var health = _keyStatsCollector.LastHealth;
        _ksSkipReason = _keyStatsCollector.LastSkipReason ?? health?.SkipReason;
        _ksState = health?.DaemonSourceState
                   ?? (processes.Count > 0 ? "Unknown" : "Unavailable");

        if (health is not null)
        {
            KeyStatsSummaryText.Text = health.SummaryZh;
            KeyStatsDetailText.Text =
                $"DaemonSourceState: {health.DaemonSourceState}\n" +
                $"DetailState: {health.DetailState}\n" +
                $"ProcessCount: {health.ProcessCount}\n" +
                $"HasForeignSession: {health.HasForeignSessionProcess}\n" +
                $"SkipReason: {health.SkipReason ?? "none"}\n" +
                $"CanUpload: {health.CanUpload}\n" +
                $"Processes: {FormatProcesses(processes)}\n" +
                $"Time: {timestamp}";
        }
        else
        {
            var liveCount = processes.Count;
            KeyStatsSummaryText.Text = liveCount == 0
                ? "KeyStats 进程未运行（尚无健康探测结果）"
                : $"检测到 {liveCount} 个 KeyStats 进程（尚无健康探测结果）";
            KeyStatsDetailText.Text =
                $"DaemonSourceState: {_ksState}\n" +
                $"ProcessCount: {liveCount}\n" +
                $"Processes: {FormatProcesses(processes)}\n" +
                $"SkipReason: {_ksSkipReason ?? "none"}\n" +
                $"Time: {timestamp}";
            if (liveCount == 0)
            {
                _ksState = "Unavailable";
                _ksSkipReason ??= "missing-process";
            }
        }

        var queueCount = _awCollector.QueueCount;
        AwQueueText.Text = queueCount > 0
            ? $"ActivityWatch 队列 {queueCount} 条待上传"
            : "ActivityWatch 队列为空";

        KeyStatsUploadText.Text = FormatUploadLine(
            "KeyStats",
            _keyStatsCollector.LastUploadTime,
            _keyStatsCollector.LastUploadError);

        KeyStatsSkipText.Text = string.IsNullOrWhiteSpace(_ksSkipReason)
            ? "无"
            : _ksSkipReason;

        var errors = new List<string>();
        if (!string.IsNullOrWhiteSpace(_awCollector.LastUploadError))
            errors.Add($"ActivityWatch: {_awCollector.LastUploadError}");
        if (!string.IsNullOrWhiteSpace(_keyStatsCollector.LastUploadError))
            errors.Add($"KeyStats: {_keyStatsCollector.LastUploadError}");
        LastErrorsText.Text = errors.Count == 0 ? "无" : string.Join("\n", errors);

        var overall = StatusCenterEvaluator.Rate(
            _authService.IsAuthenticated,
            _awState,
            _ksState,
            _ksSkipReason,
            queueCount);
        OverviewHealthText.Text = overall;
        OverallHealthText.Text = $"整体状态：{overall}";

        var suggestion = KeyStatsFixAdvisor.BuildSuggestion(health);
        KeyStatsFixSuggestionText.Text = suggestion.MessageZh;

        _lastDiagnosticsReport = BuildDiagnosticsReport(
            timestamp,
            apiDiag,
            awProbe,
            processes,
            queueCount,
            overall);
    }

    private async Task<(bool Ok, string Summary, string StatusLine, string Message)> BuildApiProbeAsync()
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

            var probe = await ProbeEndpointAsync(apiUrl);
            var summary = probe.Ok
                ? $"PIM API 已连接 · {apiUrl}"
                : $"PIM API 异常 · {probe.StatusLine}";
            return (probe.Ok, summary, probe.StatusLine, probe.Message);
        }
        catch (Exception ex)
        {
            return (false, "检查失败，无法生成健康检查地址", "Exception", ex.Message);
        }
    }

    private static async Task<(bool Ok, string StatusLine, string Message)> ProbeEndpointAsync(string url)
    {
        try
        {
            using var resp = await Http.GetAsync(url);
            var ok = resp.IsSuccessStatusCode;
            var statusLine = $"{(int)resp.StatusCode} {resp.StatusCode}";
            var message = resp.ReasonPhrase ?? (ok ? "ok" : "HTTP error");
            return (ok, statusLine, message);
        }
        catch (Exception ex)
        {
            return (false, "Exception", $"{ex.GetType().Name}: {ex.Message}");
        }
    }

    private static string FormatProcesses(IReadOnlyList<Pim.Client.Core.Models.KeyStatsProcessInfo> processes)
    {
        if (processes.Count == 0) return "none";
        return string.Join(", ", processes.Select(p =>
            $"pid={p.ProcessId}/session={p.SessionId}/current={p.IsCurrentUserSession}"));
    }

    private static string FormatUploadLine(string name, DateTime? lastUploadTime, string? lastUploadError)
    {
        var lastUpload = lastUploadTime is DateTime last
            ? $"{last:yyyy-MM-dd HH:mm:ss} ({FormatAgo(last)})"
            : "暂无记录";
        var error = string.IsNullOrWhiteSpace(lastUploadError) ? "无" : lastUploadError;
        return $"{name} 最近上传：{lastUpload}\n错误：{error}";
    }

    private static string FormatAgo(DateTime last)
    {
        var ago = DateTime.Now - last;
        if (ago.TotalMinutes < 1) return "刚刚";
        if (ago.TotalHours < 1) return $"{(int)ago.TotalMinutes} 分钟前";
        if (ago.TotalDays < 1) return $"{(int)ago.TotalHours} 小时前";
        return $"{(int)ago.TotalDays} 天前";
    }

    private string BuildDiagnosticsReport(
        string timestamp,
        (bool Ok, string Summary, string StatusLine, string Message) api,
        (bool Ok, string StatusLine, string Message) aw,
        IReadOnlyList<Pim.Client.Core.Models.KeyStatsProcessInfo> processes,
        int queueCount,
        string overall)
    {
        var sb = new StringBuilder();
        sb.AppendLine("PIM Status Center Diagnostics");
        sb.AppendLine($"Time: {timestamp}");
        sb.AppendLine($"Overall: {overall}");
        sb.AppendLine($"Authenticated: {_authService.IsAuthenticated}");
        sb.AppendLine($"Username: {_authService.CurrentUsername ?? "(none)"}");
        sb.AppendLine($"Server: {_authService.ServerUrl}");
        sb.AppendLine($"API: {api.Summary} ({api.StatusLine})");
        sb.AppendLine($"ActivityWatch: {_awState} / {aw.StatusLine} / {aw.Message}");
        sb.AppendLine($"KeyStats: {_ksState} skip={_ksSkipReason ?? "none"}");
        sb.AppendLine($"KeyStats processes: {FormatProcesses(processes)}");
        sb.AppendLine($"AW Queue: {queueCount}");
        sb.AppendLine($"AW LastUpload: {_awCollector.LastUploadTime?.ToString("O") ?? "none"}");
        sb.AppendLine($"AW LastError: {_awCollector.LastUploadError ?? "none"}");
        sb.AppendLine($"KS LastUpload: {_keyStatsCollector.LastUploadTime?.ToString("O") ?? "none"}");
        sb.AppendLine($"KS LastError: {_keyStatsCollector.LastUploadError ?? "none"}");
        sb.AppendLine($"KS LastSkip: {_keyStatsCollector.LastSkipReason ?? "none"}");
        sb.AppendLine($"InstallDir: {AppDomain.CurrentDomain.BaseDirectory}");
        return sb.ToString();
    }

    private void OnRefresh(object sender, RoutedEventArgs e) => RefreshAll();

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

        MessageBox.Show($"服务器地址已更新为：\n{normalizedUrl}", "PIM", MessageBoxButton.OK, MessageBoxImage.Information);
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

    private async void OnOneClickFixKeyStats(object sender, RoutedEventArgs e)
    {
        KeyStatsOneClickFixButton.IsEnabled = false;
        KeyStatsRestartButton.IsEnabled = false;
        RefreshButton.IsEnabled = false;
        KeyStatsOneClickFixButton.Content = "修复中...";
        KeyStatsFixResultText.Text = "修复中...";
        try
        {
            var baseDir = AppDomain.CurrentDomain.BaseDirectory;
            var exe = Path.Combine(baseDir, KeyStatsProcessManager.ExeFileName);
            var script = Path.Combine(baseDir, KeyStatsOneClickFixService.FixScriptFileName); // fix-keystats-session.ps1
            var result = await _fixService.RunAsync(
                exe,
                script,
                Process.GetCurrentProcess().SessionId,
                confirmElevation: msg =>
                    MessageBox.Show(msg, "PIM", MessageBoxButton.YesNo, MessageBoxImage.Warning) == MessageBoxResult.Yes);

            KeyStatsFixResultText.Text =
                $"阶段1：{result.Phase1MessageZh}\n阶段2：{result.Phase2MessageZh}";
            try { await _keyStatsCollector.SyncNowAsync(); } catch { }
            await RefreshStatusAsync();

            if (result.Outcome == KeyStatsFixOutcome.Failed)
                MessageBox.Show($"KeyStats 修复失败：\n{result.Phase1MessageZh}\n{result.Phase2MessageZh}", "PIM", MessageBoxButton.OK, MessageBoxImage.Error);
            else if (result.Outcome == KeyStatsFixOutcome.Cancelled)
                MessageBox.Show("已取消管理员授权，未完成跨会话清理。", "PIM", MessageBoxButton.OK, MessageBoxImage.Information);
            else if (result.Outcome == KeyStatsFixOutcome.Partial)
                MessageBox.Show("进程与 API 已处理，但计数仍为 0。请敲几下键盘后点「刷新」。", "PIM", MessageBoxButton.OK, MessageBoxImage.Warning);
            else
                MessageBox.Show("KeyStats 修复已完成。", "PIM", MessageBoxButton.OK, MessageBoxImage.Information);
        }
        catch (Exception ex)
        {
            KeyStatsFixResultText.Text = $"失败：{ex.Message}";
            await RefreshStatusAsync();
            MessageBox.Show($"KeyStats 修复失败：{ex.Message}", "PIM", MessageBoxButton.OK, MessageBoxImage.Error);
        }
        finally
        {
            KeyStatsOneClickFixButton.IsEnabled = true;
            KeyStatsRestartButton.IsEnabled = true;
            RefreshButton.IsEnabled = true;
            KeyStatsOneClickFixButton.Content = "一键修复";
        }
    }

    private void OnRestartKeyStats(object sender, RoutedEventArgs e)
    {
        try
        {
            var exe = Path.Combine(AppDomain.CurrentDomain.BaseDirectory, KeyStatsProcessManager.ExeFileName);
            if (!File.Exists(exe))
            {
                MessageBox.Show($"未找到 KeyStats.exe：\n{exe}", "PIM", MessageBoxButton.OK, MessageBoxImage.Warning);
                return;
            }

            _processManager.Restart(exe, Process.GetCurrentProcess().SessionId);
            MessageBox.Show("已请求重启 KeyStats。", "PIM", MessageBoxButton.OK, MessageBoxImage.Information);
            RefreshAll();
        }
        catch (Exception ex)
        {
            MessageBox.Show($"重启 KeyStats 失败：{ex.Message}", "PIM", MessageBoxButton.OK, MessageBoxImage.Error);
        }
    }

    private void OnOpenInstallDir(object sender, RoutedEventArgs e)
    {
        try
        {
            var dir = AppDomain.CurrentDomain.BaseDirectory;
            Process.Start(new ProcessStartInfo
            {
                FileName = "explorer.exe",
                Arguments = dir,
                UseShellExecute = true
            });
        }
        catch (Exception ex)
        {
            MessageBox.Show($"打开安装目录失败：{ex.Message}", "PIM", MessageBoxButton.OK, MessageBoxImage.Error);
        }
    }

    private void OnCopyDiagnostics(object sender, RoutedEventArgs e)
    {
        try
        {
            var text = string.IsNullOrWhiteSpace(_lastDiagnosticsReport)
                ? "诊断信息尚未生成，请先刷新。"
                : _lastDiagnosticsReport;
            Clipboard.SetText(text);
            MessageBox.Show("诊断信息已复制到剪贴板。", "PIM", MessageBoxButton.OK, MessageBoxImage.Information);
        }
        catch (Exception ex)
        {
            MessageBox.Show($"复制诊断失败：{ex.Message}", "PIM", MessageBoxButton.OK, MessageBoxImage.Error);
        }
    }

    private void OnOpenWebBrowser(object sender, RoutedEventArgs e)
    {
        try
        {
            var config = DaemonConfig.Load();
            var root = string.IsNullOrWhiteSpace(config.ServerUrl)
                ? ClientDefaults.DefaultServerUrl
                : ApiClient.NormalizeServerUrl(config.ServerUrl);
            root = root.TrimEnd('/');
            if (root.EndsWith("/api/v1", StringComparison.OrdinalIgnoreCase))
            {
                root = root[..^"/api/v1".Length].TrimEnd('/');
            }

            if (string.IsNullOrWhiteSpace(root))
            {
                root = ClientDefaults.DefaultServerUrl;
            }

            var url = $"{root}/today";
            Process.Start(new ProcessStartInfo
            {
                FileName = url,
                UseShellExecute = true
            });
        }
        catch (Exception ex)
        {
            MessageBox.Show($"打开 Web 工作台失败：{ex.Message}", "PIM", MessageBoxButton.OK, MessageBoxImage.Error);
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

    private void OnClose(object sender, RoutedEventArgs e) => Close();

    private static string? BuildUploadErrorMessage(string? awError, string? keyStatsError)
    {
        var errors = new List<string>();
        if (!string.IsNullOrWhiteSpace(awError))
            errors.Add($"ActivityWatch: {awError}");
        if (!string.IsNullOrWhiteSpace(keyStatsError))
            errors.Add($"KeyStats: {keyStatsError}");
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
