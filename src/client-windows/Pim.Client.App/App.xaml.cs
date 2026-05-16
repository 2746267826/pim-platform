using System.Windows;
using Microsoft.Extensions.DependencyInjection;
using Pim.Client.App.Services;

namespace Pim.Client.App;

public partial class App : Application
{
    public static IServiceProvider Services { get; private set; } = null!;
    private TrayIcon? _trayIcon;

    protected override void OnStartup(StartupEventArgs e)
    {
        AppDomain.CurrentDomain.UnhandledException += (_, args) =>
        {
            Logger.Error("UnhandledException", args.ExceptionObject as Exception);
        };

        try
        {
            Logger.Info("Daemon starting");
            Services = Pim.Client.App.Startup.ConfigureServices();
            Logger.Info("DI configured");

            var apiClient = Services.GetRequiredService<Pim.Client.Core.Services.ApiClient>();
            apiClient.RequestTiming += (desc, ms) =>
                Logger.Info($"[ApiTiming] {desc} took {ms}ms");

            // Authenticate (use saved token or prompt)
            var authService = Services.GetRequiredService<Core.Services.AuthService>();
            if (!authService.IsAuthenticated)
            {
                Logger.Warn("Not authenticated — daemon running without API access");
            }
            else
            {
                Logger.Info($"Authenticated as {authService.CurrentUsername}");
            }

            // Start tray icon
            _trayIcon = Services.GetRequiredService<TrayIcon>();
            _trayIcon.Show();
            Logger.Info("Tray icon shown");

            // Prevent app from shutting down when no windows are open
            ShutdownMode = ShutdownMode.OnExplicitShutdown;
        }
        catch (Exception ex)
        {
            Logger.Error("Fatal daemon startup error", ex);
            Shutdown();
        }
    }

    protected override void OnExit(ExitEventArgs e)
    {
        _trayIcon?.Dispose();
        Logger.Info("Daemon exiting");
        base.OnExit(e);
    }
}
