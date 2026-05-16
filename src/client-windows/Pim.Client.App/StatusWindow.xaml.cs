using System.Diagnostics;
using System.Net.Http;
using System.Windows;

namespace Pim.Client.App;

public partial class StatusWindow : Window
{
    public StatusWindow()
    {
        InitializeComponent();
        RefreshStatus();
    }

    private async void RefreshStatus()
    {
        // Check KeyStats
        try
        {
            using var http = new HttpClient { Timeout = TimeSpan.FromSeconds(3) };
            var resp = await http.GetAsync("http://127.0.0.1:18080/api/stats/");
            KeyStatsStatus.Text = resp.IsSuccessStatusCode
                ? "KeyStats      ✓ 已连接 (18080)"
                : $"KeyStats      ✗ HTTP {resp.StatusCode}";
        }
        catch
        {
            KeyStatsStatus.Text = "KeyStats      ✗ 未连接";
        }

        // Check ActivityWatch
        try
        {
            using var http = new HttpClient { Timeout = TimeSpan.FromSeconds(3) };
            var resp = await http.GetAsync("http://127.0.0.1:5600/api/0/buckets/");
            AWStatus.Text = resp.IsSuccessStatusCode
                ? "ActivityWatch ✓ 已连接 (5600)"
                : $"ActivityWatch ✗ HTTP {resp.StatusCode}";
        }
        catch
        {
            AWStatus.Text = "ActivityWatch ✗ 未连接";
        }

        QueueStatus.Text = "上传队列      -- 条待上传";
        LastUploadStatus.Text = "上次上传      --";
    }

    private void OnManualSync(object sender, RoutedEventArgs e)
    {
        Debug.WriteLine("Manual sync triggered");
        MessageBox.Show("同步已触发", "PIM", MessageBoxButton.OK, MessageBoxImage.Information);
    }

    private void OnViewLogs(object sender, RoutedEventArgs e)
    {
        var logPath = Services.Logger.LogFilePath;
        try { Process.Start("notepad.exe", logPath); }
        catch { MessageBox.Show($"日志文件: {logPath}", "PIM"); }
    }
}
