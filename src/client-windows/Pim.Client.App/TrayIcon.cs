using System.Drawing;
using System.IO;
using Microsoft.Extensions.DependencyInjection;

namespace Pim.Client.App;

public class TrayIcon : IDisposable
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

        _notifyIcon.ContextMenuStrip.Items.Add("状态：运行中，点击打开详情", null, (_, _) => ShowStatusWindow());
        _notifyIcon.ContextMenuStrip.Items.Add(new System.Windows.Forms.ToolStripSeparator());
        _notifyIcon.ContextMenuStrip.Items.Add("登录...", null, (_, _) => ShowLogin());
        _notifyIcon.ContextMenuStrip.Items.Add("立即同步", null, async (_, _) => await TriggerSyncAsync());
        _notifyIcon.ContextMenuStrip.Items.Add(new System.Windows.Forms.ToolStripSeparator());
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

    private static void ShowLogin()
    {
        new LoginWindow().ShowDialog();
    }

    private static async Task TriggerSyncAsync()
    {
        try
        {
            var awCollector = App.Services.GetRequiredService<Pim.Client.Core.Services.AwCollectorService>();
            var keyStatsCollector = App.Services.GetRequiredService<Pim.Client.Core.Services.KeyStatsCollectorService>();
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

    public void Dispose()
    {
        _notifyIcon?.Dispose();
        _notifyIcon = null;
    }
}
