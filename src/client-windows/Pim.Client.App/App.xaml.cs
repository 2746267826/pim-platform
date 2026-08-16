using System.Diagnostics;
using System.IO;
using System.Threading;
using System.Windows;
using Microsoft.Extensions.DependencyInjection;
using Microsoft.Win32;
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
    private int _plannedOfflineSent;
    private Task? _plannedOfflineTask;

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

            // Fire-once best-effort report of planned offline before shutdown/suspend/logoff.
            SystemEvents.SessionEnding += (_, e) =>
                TryReportPlannedOffline(e.Reason == SessionEndReasons.SystemShutdown ? "shutdown" : "logoff", wait: true);
            SystemEvents.PowerModeChanged += (_, e) =>
            {
                if (e.Mode == PowerModes.Suspend)
                {
                    TryReportPlannedOffline("suspend");
                }
                else if (e.Mode == PowerModes.Resume)
                {
                    // 唤醒后心跳会清服务端 planned 标记，下次关机需要重新上报；重置在途防重标记。
                    Interlocked.Exchange(ref _plannedOfflineSent, 0);
                    _plannedOfflineTask = null;
                }
            };

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
            var aw = Services.GetRequiredService<AwCollectorService>();
            var ks = Services.GetRequiredService<KeyStatsCollectorService>();

            var awState = aw.LastUploadError is null && aw.LastUploadTime is not null
                ? "Available"
                : aw.LastUploadError is null ? "Unknown" : "Unavailable";
            var ksHealth = ks.LastHealth;
            var ksState = ksHealth?.DaemonSourceState ?? "Unknown";

            var lastSuccess = MaxTime(aw.LastUploadTime, ks.LastUploadTime);
            var lastError = aw.LastUploadError ?? ks.LastUploadError;
            var version = typeof(App).Assembly
                .GetCustomAttributes(false)
                .OfType<System.Reflection.AssemblyInformationalVersionAttribute>()
                .FirstOrDefault()?.InformationalVersion ?? "0.0.0(unknown)";

            var heartbeat = DaemonHeartbeatReporter.BuildHeartbeat(
                Environment.MachineName,
                version,
                config.ServerUrl,
                lastSuccess is DateTime dt ? new DateTimeOffset(dt) : null,
                DateTimeOffset.UtcNow,
                lastError,
                aw.QueueCount,
                awState,
                ksState,
                new
                {
                    keyStatsDetailState = ksHealth?.DetailState.ToString(),
                    keyStatsProcessCount = ksHealth?.ProcessCount,
                    keyStatsSkipReason = ks.LastSkipReason,
                    awQueueCount = aw.QueueCount
                });
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

    private static DateTime? MaxTime(DateTime? a, DateTime? b)
    {
        if (a is null) return b;
        if (b is null) return a;
        return a > b ? a : b;
    }

    private void TryReportPlannedOffline(string reason, bool wait = false)
    {
        if (Interlocked.Exchange(ref _plannedOfflineSent, 1) == 1)
        {
            // 已在途：等待既有上报，不重发。
            if (wait && _plannedOfflineTask is not null)
            {
                try
                {
                    _plannedOfflineTask.Wait(TimeSpan.FromSeconds(2));
                }
                catch (AggregateException)
                {
                    // 任务内部已 catch，等待超时/异常不抛出。
                }
            }

            return;
        }

        var reporter = Services?.GetService<PlannedOfflineReporter>();
        if (reporter is null)
        {
            return;
        }

        var request = PlannedOfflineReporter.BuildRequest(Environment.MachineName, reason, DateTimeOffset.UtcNow);
        var task = Task.Run(async () =>
        {
            try
            {
                using var cts = new CancellationTokenSource(TimeSpan.FromSeconds(2));
                await reporter.ReportAsync(request, cts.Token);
                Logger.Info($"Planned offline reported ({reason})");
            }
            catch (Exception ex)
            {
                Logger.Warn($"Planned offline report failed ({reason}): {ex.Message}");
            }
        });

        _plannedOfflineTask = task;

        if (wait)
        {
            try
            {
                task.Wait(TimeSpan.FromSeconds(2));
            }
            catch (AggregateException)
            {
                // 任务内部已 catch，等待超时/异常不抛出。
            }
        }
    }

    protected override void OnExit(ExitEventArgs e)
    {
        TryReportPlannedOffline("exit", wait: true);
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
    /// 确保 KeyStats 在当前用户会话收敛为单实例并优先用户态启动。
    /// </summary>
    private static void EnsureKeyStatsRunning()
    {
        try
        {
            var manager = Services.GetRequiredService<KeyStatsProcessManager>();
            var exe = Path.Combine(AppDomain.CurrentDomain.BaseDirectory, KeyStatsProcessManager.ExeFileName);
            if (!File.Exists(exe))
            {
                Logger.Warn($"KeyStats.exe not found at {exe}");
                return;
            }

            var plan = manager.EnsureRunning(exe, Process.GetCurrentProcess().SessionId);
            if (plan.ShouldStart)
            {
                Logger.Info("KeyStats ensure-running started process in current session");
            }
            else if (plan.KeepProcessId is int keepPid)
            {
                Logger.Info($"KeyStats ensure-running kept process {keepPid}");
            }
            else
            {
                Logger.Info("KeyStats ensure-running completed with no keep process");
            }

            if (plan.ProcessIdsToStop.Count > 0)
            {
                Logger.Info($"KeyStats ensure-running stopped {plan.ProcessIdsToStop.Count} extra process(es)");
            }
        }
        catch (Exception ex)
        {
            Logger.Error("Failed to ensure KeyStats is running", ex);
        }
    }
}
