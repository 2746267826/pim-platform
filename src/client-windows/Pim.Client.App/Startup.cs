using Microsoft.Extensions.DependencyInjection;
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

        // ViewModels
        services.AddSingleton<ShellViewModel>();
        services.AddTransient<LoginViewModel>();
        services.AddTransient<TimelineViewModel>();
        services.AddTransient<WeekViewModel>();
        services.AddTransient<MonthViewModel>();
        services.AddTransient<TaskListViewModel>();
        services.AddTransient<InboxPanelViewModel>();
        services.AddTransient<EventEditorViewModel>();
        services.AddTransient<TaskEditorViewModel>();

        return services.BuildServiceProvider();
    }
}
