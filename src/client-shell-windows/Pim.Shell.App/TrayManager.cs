using System;
using System.Drawing;
using System.IO;
using System.Reflection;
using System.Windows;

namespace Pim.Shell.App;

public sealed class TrayManager : IDisposable
{
    private System.Windows.Forms.NotifyIcon? _icon;

    public void Show(string serverUrl, Action onShowMain, Action onQuickNote, Action onChangeServer, Action onExit)
    {
        _icon?.Dispose();
        _icon = new System.Windows.Forms.NotifyIcon
        {
            Icon = LoadIcon(),
            Text = "PIM",
            Visible = true,
            ContextMenuStrip = new System.Windows.Forms.ContextMenuStrip()
        };
        _icon.ContextMenuStrip.Items.Add("显示主窗口", null, (_, _) => onShowMain());
        _icon.ContextMenuStrip.Items.Add("快速笔记", null, (_, _) => onQuickNote());
        _icon.ContextMenuStrip.Items.Add(new System.Windows.Forms.ToolStripSeparator());
        _icon.ContextMenuStrip.Items.Add("更换服务器", null, (_, _) => onChangeServer());
        var version = typeof(TrayManager).Assembly.GetCustomAttributes(false).OfType<AssemblyInformationalVersionAttribute>().FirstOrDefault()?.InformationalVersion ?? "0.0.0-local";
        _icon.ContextMenuStrip.Items.Add($"关于 PIM v{version}", null, (_, _) => System.Windows.MessageBox.Show($"PIM Shell v{version}\n服务器: {serverUrl}", "关于"));
        _icon.ContextMenuStrip.Items.Add("退出", null, (_, _) => onExit());
        _icon.DoubleClick += (_, _) => onShowMain();
    }

    private static Icon LoadIcon()
    {
        try
        {
            var s = System.Windows.Application.GetResourceStream(new Uri("pack://application:,,,/Pim.Shell.App;component/app.ico"))?.Stream;
            if (s != null) return new Icon(s);
        }
        catch (IOException) { }
        return SystemIcons.Application;
    }

    public void Dispose() { _icon?.Dispose(); _icon = null; }
}
