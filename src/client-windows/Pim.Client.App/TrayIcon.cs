using System.Diagnostics;
using System.Drawing;
using System.IO;
using System.Net.Http.Json;
using Microsoft.Extensions.DependencyInjection;
using Pim.Client.App.Services;
using Pim.Client.Core;
using Pim.Client.Core.Services;

namespace Pim.Client.App;

public partial class TrayIcon : IDisposable
{
    private System.Windows.Forms.NotifyIcon? _notifyIcon;

    public void Show()
    {
        if (_notifyIcon is { Visible: true })
        {
            return;
        }

        _notifyIcon?.Dispose();
        _notifyIcon = new System.Windows.Forms.NotifyIcon
        {
            Icon = LoadIcon(),
            Text = "PIM 守护程序 - 点击查看状态",
            Visible = true,
            ContextMenuStrip = new System.Windows.Forms.ContextMenuStrip()
        };

        _notifyIcon.ContextMenuStrip.Items.Add("打开状态中心", null, (_, _) => ShowStatusWindow());
        _notifyIcon.ContextMenuStrip.Items.Add(new System.Windows.Forms.ToolStripSeparator());
        _notifyIcon.ContextMenuStrip.Items.Add("立即同步", null, async (_, _) => await TriggerSyncAsync());
        _notifyIcon.ContextMenuStrip.Items.Add("回填最近 14 天 ActivityWatch", null, async (_, _) => await TriggerAwBackfillAsync());
        _notifyIcon.ContextMenuStrip.Items.Add("在浏览器打开 Web 工作台", null, (_, _) => OpenWebWorkbench());
        _notifyIcon.ContextMenuStrip.Items.Add(new System.Windows.Forms.ToolStripSeparator());
        var version = GetVersion();
        var serverUrl = ResolveServerUrl(null);
        var trayMenu = BuildMenu(version, serverUrl);
        _notifyIcon.ContextMenuStrip.Items.Add(new System.Windows.Forms.ToolStripMenuItem(trayMenu.Items[0].Text, null, (_, _) => ShowAboutBox(version, serverUrl)));
        _notifyIcon.ContextMenuStrip.Items.Add(new System.Windows.Forms.ToolStripMenuItem(trayMenu.Items[1].Text, null, async (_, _) => await CheckUpdateAsync(version, serverUrl)));
        _notifyIcon.ContextMenuStrip.Items.Add(new System.Windows.Forms.ToolStripSeparator());
        _notifyIcon.ContextMenuStrip.Items.Add("登录...", null, (_, _) => ShowLogin());
        _notifyIcon.ContextMenuStrip.Items.Add("退出", null, (_, _) => ConfirmAndExit());

        _notifyIcon.DoubleClick += (_, _) => ShowStatusWindow();
    }

    private static Icon LoadIcon()
    {
        try
        {
            var streamInfo = System.Windows.Application.GetResourceStream(
                new Uri("pack://application:,,,/Pim.Client.App;component/app.ico"));
            if (streamInfo?.Stream is not null)
            {
                return new Icon(streamInfo.Stream);
            }
        }
        catch (IOException)
        {
        }

        return SystemIcons.Application;
    }

    private static void ShowStatusWindow()
    {
        var existing = System.Windows.Application.Current.Windows.OfType<StatusWindow>().FirstOrDefault();
        if (existing is not null)
        {
            existing.Activate();
            return;
        }

        new StatusWindow().Show();
    }

    private static void OpenWebWorkbench()
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
            System.Windows.Forms.MessageBox.Show($"打开 Web 工作台失败：{ex.Message}", "PIM",
                System.Windows.Forms.MessageBoxButtons.OK, System.Windows.Forms.MessageBoxIcon.Error);
        }
    }

    private static void ShowLogin()
    {
        new LoginWindow().ShowDialog();
    }

    private static async Task TriggerSyncAsync()
    {
        try
        {
            var awCollector = App.Services.GetRequiredService<AwCollectorService>();
            var keyStatsCollector = App.Services.GetRequiredService<KeyStatsCollectorService>();
            await Task.WhenAll(awCollector.SyncNowAsync(), keyStatsCollector.SyncNowAsync());

            var uploadErrors = BuildUploadErrorMessage(awCollector.LastUploadError, keyStatsCollector.LastUploadError);
            if (uploadErrors is not null)
            {
                System.Windows.Forms.MessageBox.Show($"同步已执行，但上传仍有错误：\n{uploadErrors}", "PIM",
                    System.Windows.Forms.MessageBoxButtons.OK, System.Windows.Forms.MessageBoxIcon.Warning);
            }
            else
            {
                System.Windows.Forms.MessageBox.Show("同步完成", "PIM",
                    System.Windows.Forms.MessageBoxButtons.OK, System.Windows.Forms.MessageBoxIcon.Information);
            }
        }
        catch (Exception ex)
        {
            System.Windows.Forms.MessageBox.Show($"同步失败：{ex.Message}", "PIM",
                System.Windows.Forms.MessageBoxButtons.OK, System.Windows.Forms.MessageBoxIcon.Error);
        }
    }

    private static async Task TriggerAwBackfillAsync()
    {
        try
        {
            var awCollector = App.Services.GetRequiredService<AwCollectorService>();
            var endUtc = DateTimeOffset.UtcNow;
            await awCollector.BackfillAsync(endUtc.AddDays(-14), endUtc);

            if (!string.IsNullOrWhiteSpace(awCollector.LastUploadError))
            {
                System.Windows.Forms.MessageBox.Show($"ActivityWatch 回填已执行，但仍有上传错误：\n{awCollector.LastUploadError}", "PIM",
                    System.Windows.Forms.MessageBoxButtons.OK, System.Windows.Forms.MessageBoxIcon.Warning);
            }
            else
            {
                System.Windows.Forms.MessageBox.Show("ActivityWatch 最近 14 天回填完成", "PIM",
                    System.Windows.Forms.MessageBoxButtons.OK, System.Windows.Forms.MessageBoxIcon.Information);
            }
        }
        catch (Exception ex)
        {
            System.Windows.Forms.MessageBox.Show($"ActivityWatch 回填失败：{ex.Message}", "PIM",
                System.Windows.Forms.MessageBoxButtons.OK, System.Windows.Forms.MessageBoxIcon.Error);
        }
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

    private void ConfirmAndExit()
    {
        var result = System.Windows.Forms.MessageBox.Show(
            "确定要退出 PIM 守护程序吗？",
            "PIM",
            System.Windows.Forms.MessageBoxButtons.YesNo,
            System.Windows.Forms.MessageBoxIcon.Question);

        if (result == System.Windows.Forms.DialogResult.Yes)
        {
            Dispose();
            System.Windows.Application.Current.Shutdown();
        }
    }

    private static string ResolveServerUrl(string? serverUrl)
    {
        if (!string.IsNullOrWhiteSpace(serverUrl)) return serverUrl!.Trim();
        try { return DaemonConfig.Load().ServerUrl?.Trim() ?? ClientDefaults.DefaultServerUrl; }
        catch { return ClientDefaults.DefaultServerUrl; }
    }

    private static void ShowAboutBox(string version, string serverUrl)
    {
        var url = ResolveServerUrl(serverUrl);
        System.Windows.Forms.MessageBox.Show($"PIM Daemon v{version}\nAPI: {url}", "关于");
    }

    private static async Task CheckUpdateAsync(string version, string serverUrl)
    {
        var url = ResolveServerUrl(serverUrl);
        try
        {
            using var http = new HttpClient { Timeout = TimeSpan.FromSeconds(5) };
            var latest = await http.GetFromJsonAsync<LatestDto>($"{url.TrimEnd('/')}/api/client/shell/latest");
            if (latest?.error != null)
            {
                Logger.Warn($"Daemon update check failed: {latest.error} checkedAt={latest.checkedAt}");
                System.Windows.Forms.MessageBox.Show($"检查失败：{latest.error}", "PIM");
            }
            else if (latest?.windowsVersion != null && UpdateChecker.IsNewer(version, latest.windowsVersion))
            {
                System.Windows.Forms.MessageBox.Show($"发现新版 {latest.windowsVersion}\n{latest.windowsUrl}", "PIM");
            }
            else
            {
                System.Windows.Forms.MessageBox.Show("已是最新版本", "PIM");
            }
        }
        catch (Exception ex)
        {
            Logger.Warn($"Daemon update check failed: {ex.Message}");
            System.Windows.Forms.MessageBox.Show($"检查失败：{ex.Message}", "PIM");
        }
    }

    private record LatestDto(string? windowsVersion, string? windowsUrl, string? error, string? checkedAt);

    public void Dispose()
    {
        _notifyIcon?.Dispose();
        _notifyIcon = null;
    }
}
