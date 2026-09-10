using System.Diagnostics;
using System.IO;
using System.Windows;
using Pim.Client.App.Services;

namespace Pim.Client.App;

/// <summary>
/// 显式入口点：先写 bootstrap、再抢单实例 Mutex，最后构造 App 并运行。
/// </summary>
internal static class Program
{
    [STAThread]
    private static void Main()
    {
        BootstrapLog.Write("Process entered");

        // D3：单实例互斥（开机自启 HIGHEST 任务与用户手动双击均受保护）。
        if (!SingleInstanceGuard.TryAcquire())
        {
            BootstrapLog.Write($"Another instance running; exiting (PID={Environment.ProcessId})");
            Environment.Exit(0);
        }

        var app = new App();
        BootstrapLog.Write("App constructed");

        // D2：尽早安装全局崩溃钩子（AppDomain / TaskScheduler / Dispatcher），须在 Run 之前。
        App.InstallExceptionHooks(app);

        try
        {
            app.InitializeComponent();
            app.Run();
        }
        catch (Exception ex)
        {
            // 覆盖资源加载 / WPF 运行期未捕获异常（放进 try 前已注册 AppDomain 钩子也会兜底）
            BootstrapLog.Write($"Unhandled exception in WPF run loop: {ex}");
            throw;
        }
        finally
        {
            // 退出后落盘剩余日志并释放互斥体
            Logger.Shutdown();
            SingleInstanceGuard.Release();
        }
    }
}
