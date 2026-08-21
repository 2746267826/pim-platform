using System.Net.Http;
using System.Windows;

namespace Pim.Shell.App;

public partial class SetupWindow : Window
{
    public SetupWindow() => InitializeComponent();

    private async void OnConnectClick(object sender, RoutedEventArgs e)
    {
        ConnectButton.IsEnabled = false;
        HintText.Text = "正在连接…";
        using var http = new HttpClient { Timeout = TimeSpan.FromSeconds(5) };
        var health = new ServerHealthClient(http);
        var result = await health.CheckAsync(ServerInput.Text);
        ConnectButton.IsEnabled = true;
        if (result.Status != HealthCheckStatus.Healthy)
        {
            HintText.Text = "无法连接到服务器，请检查地址与网络。";
            return;
        }
        if (ServerAddress.IsInsecure(result.NormalizedUrl))
        {
            var confirm = MessageBox.Show(this,
                "该地址使用明文 HTTP，数据在传输中可能被窃听。仍要继续吗？",
                "安全提示", MessageBoxButton.YesNo, MessageBoxImage.Warning);
            if (confirm != MessageBoxResult.Yes) return;
        }
        new ShellConfig { ServerUrl = result.NormalizedUrl }.Save();
        new ShellWindow(result.NormalizedUrl).Show();
        Close();
    }
}
