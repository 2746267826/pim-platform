using System.Windows;
using System.Windows.Threading;
using Microsoft.Extensions.DependencyInjection;
using Pim.Client.App.Services;
using Pim.Client.App.ViewModels;
using Pim.Client.App.Views;

namespace Pim.Client.App;

public partial class App : Application
{
    public static IServiceProvider Services { get; private set; } = null!;

    protected override void OnStartup(StartupEventArgs e)
    {
        AppDomain.CurrentDomain.UnhandledException += (_, args) =>
        {
            var ex = args.ExceptionObject as Exception;
            Logger.Error("AppDomain.UnhandledException (fatal)", ex);
        };

        DispatcherUnhandledException += (_, args) =>
        {
            Logger.Error("Dispatcher.UnhandledException", args.Exception);
            args.Handled = true;
            MessageBox.Show($"未处理的错误:\n{args.Exception.Message}\n\n日志已保存到:\n{Logger.LogFilePath}",
                "PIM 错误", MessageBoxButton.OK, MessageBoxImage.Error);
        };

        TaskScheduler.UnobservedTaskException += (_, args) =>
        {
            Logger.Error("UnobservedTaskException", args.Exception);
            args.SetObserved();
        };

        try
        {
            Logger.Info("App starting");
            Services = Pim.Client.App.Startup.ConfigureServices();
            Logger.Info("DI container configured");

            // Hook API timing logs
            var apiClient = Services.GetRequiredService<Pim.Client.Core.Services.ApiClient>();
            apiClient.RequestTiming += (desc, ms) =>
                Logger.Info($"[ApiTiming] {desc} took {ms}ms");

            ShowLoginDialog();
        }
        catch (Exception ex)
        {
            Logger.Error("Fatal startup error", ex);
            MessageBox.Show($"启动失败:\n{ex.Message}\n\n日志已保存到:\n{Logger.LogFilePath}",
                "PIM 启动错误", MessageBoxButton.OK, MessageBoxImage.Error);
            Shutdown();
        }
    }

    private void ShowLoginDialog()
    {
        try
        {
            var loginVm = Services.GetRequiredService<LoginViewModel>();
            var loginWindow = new LoginWindow(loginVm);
            Logger.Info("Showing login window");
            loginWindow.ShowDialog();
        }
        catch (Exception ex)
        {
            Logger.Error("Login window error", ex);
            MessageBox.Show($"登录窗口加载失败:\n{ex.Message}\n\n日志已保存到:\n{Logger.LogFilePath}",
                "PIM 错误", MessageBoxButton.OK, MessageBoxImage.Error);
            Shutdown();
            return;
        }

        var authService = Services.GetRequiredService<Core.Services.AuthService>();
        if (!authService.IsAuthenticated)
        {
            Logger.Info("User not authenticated, shutting down");
            Shutdown();
            return;
        }

        Logger.Info($"User authenticated: {authService.CurrentUsername}");
        ShowMainWindow();
    }

    private void ShowMainWindow()
    {
        try
        {
            var shellVm = Services.GetRequiredService<ShellViewModel>();
            var mainWindow = new MainWindow(shellVm, Services);
            Logger.Info("MainWindow created, showing");
            mainWindow.Show();
        }
        catch (Exception ex)
        {
            Logger.Error("MainWindow creation failed", ex);
            MessageBox.Show($"主窗口加载失败:\n{ex.Message}\n\n这可能是资源文件或样式配置问题。\n\n日志已保存到:\n{Logger.LogFilePath}",
                "PIM 错误", MessageBoxButton.OK, MessageBoxImage.Error);
            Shutdown();
        }
    }
}
