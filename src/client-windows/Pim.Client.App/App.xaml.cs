using System.Windows;

namespace Pim.Client.App;

public partial class App : Application
{
    public static IServiceProvider Services { get; private set; } = null!;

    protected override void OnStartup(StartupEventArgs e)
    {
        Services = Pim.Client.App.Startup.ConfigureServices();
        var mainWindow = new MainWindow();
        mainWindow.Show();
        base.OnStartup(e);
    }
}
