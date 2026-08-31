using Microsoft.Extensions.DependencyInjection;
using Pim.Client.Core.Services;

namespace Pim.Client.App;

public static class Startup
{
    public static IServiceProvider ConfigureServices()
    {
        var services = new ServiceCollection();

        // Core services
        services.AddSingleton<ApiClient>();
        services.AddSingleton<AuthService>();
        services.AddSingleton<TrackerLogger>();
        services.AddSingleton<BrowserBridgeService>(sp =>
        {
            var cfg = DaemonConfig.Load().Tracker;
            var logger = sp.GetRequiredService<TrackerLogger>();
            return new BrowserBridgeService(cfg.BrowserBridgePort, logger);
        });
        services.AddSingleton<NativeTrackerService>(sp =>
        {
            var api = sp.GetRequiredService<ApiClient>();
            var cfg = DaemonConfig.Load().Tracker;
            var logger = sp.GetRequiredService<TrackerLogger>();
            var bridge = sp.GetRequiredService<BrowserBridgeService>();
            return new NativeTrackerService(api, cfg, null, null, bridge, logger);
        });
        services.AddSingleton<KeyStatsProcessManager>();
        services.AddSingleton<KeyStatsCollectorService>();
        services.AddSingleton<DaemonHeartbeatReporter>();
        services.AddSingleton<PlannedOfflineReporter>();
        services.AddSingleton<Pim.Client.Core.Services.NotificationActionRouter>();
        services.AddSingleton<EndpointCollectionBoundaryService>();
        services.AddSingleton<NotificationActionRouter>();
        services.AddSingleton<TrayIcon>();

        return services.BuildServiceProvider();
    }
}
