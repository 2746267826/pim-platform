using System.Windows;
using Microsoft.Extensions.DependencyInjection;
using Pim.Client.App.Services;
using Pim.Client.App.ViewModels;
using Pim.Client.App.Views;

namespace Pim.Client.App;

public partial class MainWindow : Window
{
    private readonly ShellViewModel _shellVm;
    private readonly IServiceProvider _services;
    private bool _isLoggingOut;

    public event Action? LoggedOutAndReauthenticated;

    public MainWindow(ShellViewModel shellVm, IServiceProvider services)
    {
        Logger.Info("MainWindow constructing");
        InitializeComponent();
        _shellVm = shellVm;
        _services = services;
        DataContext = shellVm;

        var authService = _services.GetRequiredService<Core.Services.AuthService>();
        _shellVm.SetUserInfo(authService.CurrentDisplayName ?? authService.CurrentUsername ?? "");

        Loaded += async (_, _) => await _shellVm.LoadCalendarsAsync();
        Logger.Info("MainWindow constructed");
    }

    protected override void OnClosing(System.ComponentModel.CancelEventArgs e)
    {
        if (!_isLoggingOut)
        {
            Logger.Info("MainWindow closing (user exit), shutting down");
            Application.Current.Shutdown();
        }
        base.OnClosing(e);
    }
}
