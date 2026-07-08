using System.Diagnostics;
using System.IO;
using System.Threading;
using System.Windows;
using Microsoft.Extensions.DependencyInjection;
using Pim.Client.App.Services;
using Pim.Client.Core.Services;

namespace Pim.Client.App;

public partial class App : Application
{
    public static IServiceProvider Services { get; private set; } = null!;
    private TrayIcon? _trayIcon;
    private readonly CancellationTokenSource _shutdown = new();
    private readonly PeriodicTimer _heartbeatTimer = new(TimeSpan.FromMinutes(2));
    private Task? _heartbeatTask;

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
            // Apply auto-start setting (synchronizes registry with config at every boot)
            AutoStartManager.Set(config.AutoStart);
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

            ShowMainShellWindow();
            Logger.Info("Companion shell window shown");

            var awCollector = Services.GetRequiredService<AwCollectorService>();
            awCollector.Log = msg => Logger.Info(msg);
            awCollector.Start();
            Logger.Info("ActivityWatch collector started");

            EnsureKeyStatsRunning();

            var keyStatsCollector = Services.GetRequiredService<KeyStatsCollectorService>();
            keyStatsCollector.Log = msg => Logger.Info(msg);
            keyStatsCollector.Start();
            Logger.Info("KeyStats collector started");

            _heartbeatTask = Task.Run(() => RunHeartbeatLoopAsync(_shutdown.Token));
            Logger.Info("Daemon heartbeat loop started");
        }
        catch (Exception ex)
        {
            Logger.Error("Fatal daemon startup error", ex);
            Shutdown();
        }
    }

    internal static void ShowMainShellWindow(string? route = null)
    {
        var existing = Current.Windows.OfType<MainShellWindow>().FirstOrDefault();
        if (existing is not null)
        {
            if (!string.IsNullOrWhiteSpace(route))
            {
                existing.OpenRoute(route);
            }

            existing.Activate();
            return;
        }

        var shell = new MainShellWindow();
        shell.Show();
        if (!string.IsNullOrWhiteSpace(route))
        {
            shell.OpenRoute(route);
        }
    }

    private async Task RunHeartbeatLoopAsync(CancellationToken ct)
    {
        try
        {
            await ReportHeartbeatOnceAsync(ct);

            while (await _heartbeatTimer.WaitForNextTickAsync(ct))
            {
                await ReportHeartbeatOnceAsync(ct);
            }
        }
        catch (OperationCanceledException) when (ct.IsCancellationRequested)
        {
        }
    }

    private static async Task ReportHeartbeatOnceAsync(CancellationToken ct)
    {
        try
        {
            var reporter = Services.GetRequiredService<DaemonHeartbeatReporter>();
            var config = DaemonConfig.Load();
            var heartbeat = DaemonHeartbeatReporter.BuildHeartbeat(
                Environment.MachineName,
                typeof(App).Assembly
                    .GetCustomAttributes(false)
                    .OfType<System.Reflection.AssemblyInformationalVersionAttribute>()
                    .FirstOrDefault()?.InformationalVersion ?? "0.0.0(unknown)",
                config.ServerUrl,
                null,
                DateTimeOffset.UtcNow,
                null);
            await reporter.ReportAsync(heartbeat, ct);
            Logger.Info("Daemon heartbeat reported");
        }
        catch (OperationCanceledException) when (ct.IsCancellationRequested)
        {
            throw;
        }
        catch (Exception ex)
        {
            Logger.Warn($"Daemon heartbeat failed: {ex.Message}");
        }
    }

    protected override void OnExit(ExitEventArgs e)
    {
        _shutdown.Cancel();
        _heartbeatTimer.Dispose();
        _heartbeatTask?.ContinueWith(
            task => Logger.Warn($"Daemon heartbeat loop faulted: {task.Exception?.GetBaseException().Message ?? "unknown error"}"),
            TaskContinuationOptions.OnlyOnFaulted);
        _shutdown.Dispose();
        _trayIcon?.Dispose();
        Logger.Info("Daemon exiting");
        base.OnExit(e);
    }

    /// <summary>
    /// 确保 KeyStats 进程在运行。如果未运行，通过计划任务静默拉起。
    /// 创建计划任务需要管理员权限，但运行已有任务不需要。
    /// </summary>
    private static void EnsureKeyStatsRunning()
    {
        try
        {
            if (Process.GetProcessesByName("KeyStats").Length > 0)
            {
                Logger.Info("KeyStats process already running");
                return;
            }

            // 先找同目录下的 KeyStats.exe
            var baseDir = AppDomain.CurrentDomain.BaseDirectory;
            var keyStatsPath = Path.Combine(baseDir, "KeyStats.exe");

            if (!File.Exists(keyStatsPath))
            {
                Logger.Warn($"KeyStats.exe not found at {keyStatsPath}");
                return;
            }

            // 尝试通过计划任务静默启动（不需要管理员权限）
            const string taskName = "PimKeyStats";
            var taskRun = Process.Start(new ProcessStartInfo("schtasks", $"/run /tn \"{taskName}\"")
            {
                CreateNoWindow = true,
                UseShellExecute = false,
            });
            taskRun?.WaitForExit(3000);

            if (taskRun?.ExitCode == 0)
            {
                Logger.Info("KeyStats launched via scheduled task");
                // 等待进程起来
                for (int i = 0; i < 10; i++)
                {
                    if (Process.GetProcessesByName("KeyStats").Length > 0) return;
                    Thread.Sleep(500);
                }
                return;
            }

            // 计划任务不存在或启动失败，尝试直接拉起（会弹 UAC）
            Logger.Warn("Scheduled task not found, launching KeyStats directly (UAC may prompt)");
            Process.Start(new ProcessStartInfo(keyStatsPath)
            {
                UseShellExecute = true,
                Verb = "runas",
            });
        }
        catch (Exception ex)
        {
            Logger.Error("Failed to ensure KeyStats is running", ex);
        }
    }
}
