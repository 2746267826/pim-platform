using System.Reflection;
using Pim.Core.Modules;

namespace Pim.Api;

public class ModuleRegistry
{
    private readonly List<IModule> _modules = new();

    public IReadOnlyList<IModule> Modules => _modules;

    public void DiscoverModules(IServiceCollection services, IConfiguration configuration)
    {
        var baseDir = AppDomain.CurrentDomain.BaseDirectory;
        var moduleFiles = Directory.GetFiles(baseDir, "Pim.Module.*.dll");

        foreach (var assemblyPath in moduleFiles)
        {
            var assembly = Assembly.LoadFrom(assemblyPath);
            var moduleTypes = assembly.GetTypes()
                .Where(t => typeof(IModule).IsAssignableFrom(t) && !t.IsInterface && !t.IsAbstract);

            foreach (var type in moduleTypes)
            {
                var module = (IModule)Activator.CreateInstance(type)!;
                _modules.Add(module);
                module.RegisterServices(services, configuration);
            }
        }
    }

    public void MapAllEndpoints(IEndpointRouteBuilder endpoints)
    {
        foreach (var module in _modules)
        {
            module.MapEndpoints(endpoints);
        }
    }

    public async Task InitializeAllAsync(IServiceProvider serviceProvider)
    {
        foreach (var module in _modules)
        {
            await module.InitializeAsync(serviceProvider);
        }
    }
}
