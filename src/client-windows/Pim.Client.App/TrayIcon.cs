using System.Drawing;
using System.IO;
using System.Windows;

namespace Pim.Client.App;

public class TrayIcon : IDisposable
{
    private System.Windows.Forms.NotifyIcon? _notifyIcon;

    public void Show()
    {
        var icon = LoadIcon();

        _notifyIcon = new System.Windows.Forms.NotifyIcon
        {
            Icon = icon,
            Text = "PIM 数据采集服务",
            Visible = true,
            ContextMenuStrip = new System.Windows.Forms.ContextMenuStrip()
        };

        _notifyIcon.ContextMenuStrip.Items.Add("状态: 运行中").Enabled = false;
        _notifyIcon.ContextMenuStrip.Items.Add(new System.Windows.Forms.ToolStripSeparator());
        _notifyIcon.ContextMenuStrip.Items.Add("打开状态窗口", null, (_, _) => ShowStatusWindow());
        _notifyIcon.ContextMenuStrip.Items.Add("手动同步", null, (_, _) => TriggerSync());
        _notifyIcon.ContextMenuStrip.Items.Add(new System.Windows.Forms.ToolStripSeparator());
        _notifyIcon.ContextMenuStrip.Items.Add("退出", null, (_, _) => ConfirmAndExit());

        _notifyIcon.DoubleClick += (_, _) => ShowStatusWindow();
    }

    private static Icon LoadIcon()
    {
        // Try embedded resource first
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
            // Resource not found, fall through to fallback
        }

        // Fallback: use system application icon
        return SystemIcons.Application;
    }

    private void ShowStatusWindow()
    {
        var existing = System.Windows.Application.Current.Windows.OfType<StatusWindow>().FirstOrDefault();
        if (existing is not null)
        {
            existing.Activate();
            return;
        }
        var window = new StatusWindow();
        window.Show();
    }

    private async void TriggerSync()
    {
        System.Diagnostics.Debug.WriteLine("Manual sync triggered");
    }

    private void ConfirmAndExit()
    {
        var result = System.Windows.Forms.MessageBox.Show(
            "确定要退出 PIM 数据采集服务吗？",
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
