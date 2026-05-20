using System.Windows;
using Microsoft.Extensions.DependencyInjection;
using Pim.Client.App.Services;
using Pim.Client.Core.Services;

namespace Pim.Client.App;

public partial class App : Application
{
    public static IServiceProvider Services { get; private set; } = null!;
    private TrayIcon? _trayIcon;

    protected override async void OnStartup(StartupEventArgs e)
    {
        Logger.Initialize();

        AppDomain.CurrentDomain.UnhandledException += (_, args) =>
        {
            Logger.Error("UnhandledException", args.ExceptionObject as Exception);
        };

        try
        {
            Logger.Info("Daemon starting");
            Services = Pim.Client.App.Startup.ConfigureServices();
            Logger.Info("DI configured");

            var config = DaemonConfig.Load();
            var apiClient = Services.GetRequiredService<ApiClient>();
            var authService = Services.GetRequiredService<AuthService>();
            if (!string.IsNullOrEmpty(config.ServerUrl))
            {
                var serverUrl = ApiClient.NormalizeServerUrl(config.ServerUrl);
                if (!string.Equals(config.ServerUrl, serverUrl, StringComparison.Ordinal))
                {
                    config.ServerUrl = serverUrl;
                    config.Save();
                }

                apiClient.SetBaseUrl(serverUrl);
                authService.ServerUrl = serverUrl;
                Logger.Info($"Server URL: {serverUrl}");
            }

            ShutdownMode = ShutdownMode.OnExplicitShutdown;

            apiClient.RequestTiming += (desc, ms) =>
                Logger.Info($"[ApiTiming] {desc} took {ms}ms");

            _trayIcon = Services.GetRequiredService<TrayIcon>();
            _trayIcon.Show();
            Logger.Info("Tray icon shown");

            var restored = await authService.TryRestoreTokenAsync();
            if (restored)
            {
                Logger.Info($"Authenticated as {authService.CurrentUsername} (token restored)");
            }
            else
            {
                Logger.Info("No saved token; showing login window");
                var loginWindow = new LoginWindow();
                var result = loginWindow.ShowDialog();
                if (result == true)
                    Logger.Info($"Authenticated as {authService.CurrentUsername}");
                else
                    Logger.Warn("Login skipped; daemon running without API access, uploads will fail");
            }

            var awCollector = Services.GetRequiredService<AwCollectorService>();
            awCollector.Log = msg => Logger.Info(msg);
            awCollector.Start();
            Logger.Info("ActivityWatch collector started");

            var keyStatsCollector = Services.GetRequiredService<KeyStatsCollectorService>();
            keyStatsCollector.Log = msg => Logger.Info(msg);
            keyStatsCollector.Start();
            Logger.Info("KeyStats collector started");
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
