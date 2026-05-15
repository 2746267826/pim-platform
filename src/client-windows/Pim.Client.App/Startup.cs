using Microsoft.Extensions.DependencyInjection;
using Pim.Client.App.Services;
using Pim.Client.App.ViewModels;
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

        // App services
        services.AddSingleton<INavigationService, NavigationService>();

        // ViewModels
        services.AddTransient<LoginViewModel>();
        services.AddSingleton<MainViewModel>();
        services.AddTransient<CalendarViewModel>();
        services.AddTransient<TaskListViewModel>();
        services.AddTransient<SearchViewModel>();

        return services.BuildServiceProvider();
    }
}
