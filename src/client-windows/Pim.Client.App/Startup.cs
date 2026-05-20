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
        services.AddSingleton<AwCollectorService>();
        services.AddSingleton<KeyStatsCollectorService>();
        services.AddSingleton<TrayIcon>();

        return services.BuildServiceProvider();
    }
}
