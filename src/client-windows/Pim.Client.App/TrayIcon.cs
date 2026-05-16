using System.IO;
using System.Windows;

namespace Pim.Client.App;

public class TrayIcon : IDisposable
{
    private System.Windows.Forms.NotifyIcon? _notifyIcon;

    public void Show()
    {
        var iconStream = System.Windows.Application.GetResourceStream(
            new Uri("pack://application:,,,/Pim.Client.App;component/app.ico"))?.Stream
            ?? SystemIcons.Application.ToStream();

        _notifyIcon = new System.Windows.Forms.NotifyIcon
        {
            Icon = new System.Drawing.Icon(iconStream),
            Text = "PIM 数据采集服务",
            Visible = true,
            ContextMenuStrip = new System.Windows.Forms.ContextMenuStrip()
        };

        _notifyIcon.ContextMenuStrip.Items.Add("状态: 运行中").Enabled = false;
        _notifyIcon.ContextMenuStrip.Items.Add(new System.Windows.Forms.ToolStripSeparator());
        _notifyIcon.ContextMenuStrip.Items.Add("打开状态窗口", null, (_, _) => ShowStatusWindow());
        _notifyIcon.ContextMenuStrip.Items.Add("手动同步", null, (_, _) => TriggerSync());
        _notifyIcon.ContextMenuStrip.Items.Add(new System.Windows.Forms.ToolStripSeparator());
        _notifyIcon.ContextMenuStrip.Items.Add("退出", null, (_, _) => Exit());

        _notifyIcon.DoubleClick += (_, _) => ShowStatusWindow();
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

    private void Exit()
    {
        Dispose();
        System.Windows.Application.Current.Shutdown();
    }

    public void Dispose()
    {
        _notifyIcon?.Dispose();
        _notifyIcon = null;
    }
}

static class IconExtensions
{
    public static Stream ToStream(this System.Drawing.Icon icon)
    {
        var ms = new MemoryStream();
        icon.Save(ms);
        ms.Seek(0, SeekOrigin.Begin);
        return ms;
    }
}
