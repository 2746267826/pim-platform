using System.Windows;
using Microsoft.Extensions.DependencyInjection;
using Pim.Client.App.ViewModels;
using Pim.Client.App.Views;

namespace Pim.Client.App;

public partial class App : Application
{
    public static IServiceProvider Services { get; private set; } = null!;

    protected override void OnStartup(StartupEventArgs e)
    {
        Services = Pim.Client.App.Startup.ConfigureServices();

        var loginVm = Services.GetRequiredService<LoginViewModel>();
        var loginWindow = new LoginWindow(loginVm);
        loginWindow.ShowDialog();

        // If user closed login without authenticating, exit
        if (!Services.GetRequiredService<Core.Services.AuthService>().IsAuthenticated)
        {
            Shutdown();
            return;
        }

        var mainVm = Services.GetRequiredService<MainViewModel>();
        var mainWindow = new MainWindow(mainVm, Services);
        mainWindow.Show();
        base.OnStartup(e);
    }
}
