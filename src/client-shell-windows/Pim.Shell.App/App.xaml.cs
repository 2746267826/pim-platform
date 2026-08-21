using System.Windows;

namespace Pim.Shell.App;

public partial class App : Application
{
    protected override void OnStartup(StartupEventArgs e)
    {
        base.OnStartup(e);
        var config = ShellConfig.Load();
        var normalized = ServerAddress.Normalize(config.ServerUrl);
        if (normalized is null) new SetupWindow().Show();
        else new ShellWindow(normalized).Show();
    }
}
