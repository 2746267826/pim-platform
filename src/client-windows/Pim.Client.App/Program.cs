using System.Threading;
using System.Windows;
using Pim.Client.App.Services;

namespace Pim.Client.App;

/// <summary>
/// 显式入口点：先写 bootstrap、再抢单实例 Mutex，最后构造 App 并运行。
/// </summary>
internal static class Program
{
    // 静态字段持有整个进程生命周期，不提前 Dispose。
    private static Mutex? _singleInstanceMutex;

    [STAThread]
    private static void Main()
    {
        BootstrapLog.Write("Process entered");

        // D3：单实例互斥（Global 命名空间需 SeCreateGlobalPrivilege，守护程序以 HIGHEST 计划任务/管理员运行）。
        try
        {
            _singleInstanceMutex = new Mutex(true, @"Global\PIM_Daemon_SingleInstance", out bool createdNew);
            if (!createdNew)
            {
                BootstrapLog.Write($"Another instance running; exiting (PID={Environment.ProcessId})");
                Environment.Exit(0);
            }

            BootstrapLog.Write("Mutex acquired");
        }
        catch (Exception ex)
        {
            // Global\ 命名空间可能因权限受限而失败：降级为不启用单实例守护，继续启动。
            BootstrapLog.Write($"Mutex creation failed ({ex.Message}); continuing without single-instance guard");
        }

        var app = new App();
        BootstrapLog.Write("App constructed");

        // D2：尽早安装全局崩溃钩子（AppDomain / TaskScheduler / Dispatcher），须在 Run 之前。
        App.InstallExceptionHooks(app);

        app.InitializeComponent();
        try
        {
            app.Run();
        }
        catch (Exception ex)
        {
            BootstrapLog.Write($"Unhandled exception in WPF run loop: {ex}");
            throw;
        }
    }
}
