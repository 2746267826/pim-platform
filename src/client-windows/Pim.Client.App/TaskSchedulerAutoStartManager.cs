using System;
using System.Diagnostics;
using System.IO;
using Microsoft.Win32;
using Pim.Client.App.Services;
using Pim.Client.Core.Services;

namespace Pim.Client.App;

/// <summary>
/// Manages auto-start via Task Scheduler (\PIM\PIM Daemon + \PIM\PIM KeyStats),
/// with legacy HKCU\Run cleanup and graceful fallback to registry when not elevated.
/// </summary>
public static class TaskSchedulerAutoStartManager
{
    public const string DaemonTaskPath = ScheduledTaskService.DaemonTaskPath;
    public const string KeyStatsTaskPath = ScheduledTaskService.KeyStatsTaskPath;
    public const string LegacyTaskName = ScheduledTaskService.LegacyTaskName;

    // Test hook: inject custom runner
    internal static ISchtasksRunner? TestRunner { get; set; }

    private static ScheduledTaskService CreateService()
        => new ScheduledTaskService(TestRunner);

    private static string ExecutablePath
    {
        get
        {
            var path = Environment.ProcessPath ?? "";
            return path;
        }
    }

    private static string KeyStatsExePath
    {
        get
        {
            var dir = Path.GetDirectoryName(ExecutablePath) ?? AppDomain.CurrentDomain.BaseDirectory;
            return Path.Combine(dir, KeyStatsProcessManager.ExeFileName);
        }
    }

    /// <summary>
    /// Whether daemon task is registered (exists). Disabled state is treated as not registered; deletion is used for disable.
    /// </summary>
    public static bool IsRegistered
    {
        get
        {
            try
            {
                var svc = CreateService();
                // Prefer task; if schtasks unavailable, fallback to registry check
                if (IsSchtasksAvailable(svc))
                {
                    return svc.IsDaemonTaskRegistered();
                }
                return IsRegistryRegistered();
            }
            catch
            {
                return IsRegistryRegistered();
            }
        }
    }

    /// <summary>
    /// Set or clear auto-start. Returns true if operation succeeded via tasks or registry fallback.
    /// New design: only \PIM\PIM Daemon (HIGHEST), KeyStats runs as child process inheriting privilege.
    /// When enabled, ensures daemon exists and cleans legacy KeyStats task/registry entries.
    /// When disabled, deletes daemon task and cleans all.
    /// </summary>
    public static bool Set(bool enabled)
    {
        try
        {
            var svc = CreateService();
            bool schtasksAvailable = IsSchtasksAvailable(svc);

            if (enabled)
            {
                if (schtasksAvailable)
                {
                    bool daemonExists = false;
                    try { daemonExists = svc.IsDaemonTaskRegistered(); } catch { }

                    if (daemonExists)
                    {
                        // Already registered, clean legacy KeyStats artifacts
                        try { svc.TryDeleteTask(KeyStatsTaskPath); } catch { }
                        try { svc.CleanupLegacyTasks(); } catch { }
                        CleanLegacyRunEntries();
                        return true;
                    }

                    bool daemonOk = svc.TryCreateDaemonTask(ExecutablePath);
                    // Ensure legacy KeyStats task removed
                    try { svc.TryDeleteTask(KeyStatsTaskPath); } catch { }
                    try { svc.CleanupLegacyTasks(); } catch { }
                    CleanLegacyRunEntries();

                    if (daemonOk)
                        return true;

                    Logger.Warn($"Failed to create daemon scheduled task (exit non-zero), need elevation.");
                    return false;
                }
                // schtasks not available (non-Windows): fallback to registry
                SetRegistry(true);
                try { svc.TryDeleteTask(KeyStatsTaskPath); } catch { }
                try { svc.CleanupLegacyTasks(); } catch { }
                CleanLegacyKeyStatsOnly();
                return true;
            }
            else
            {
                if (schtasksAvailable)
                {
                    bool daemonExists = false;
                    try { daemonExists = svc.IsDaemonTaskRegistered(); } catch { }
                    if (!daemonExists)
                    {
                        try { svc.TryDeleteTask(KeyStatsTaskPath); } catch { }
                        try { svc.CleanupLegacyTasks(); } catch { }
                        CleanLegacyRunEntries();
                        SetRegistry(false);
                        return true;
                    }

                    var delDaemon = svc.TryDeleteTask(DaemonTaskPath);
                    try { svc.TryDeleteTask(KeyStatsTaskPath); } catch { }
                    try { svc.CleanupLegacyTasks(); } catch { }
                    CleanLegacyRunEntries();
                    var stillExists = false;
                    try { stillExists = svc.IsDaemonTaskRegistered(); } catch { }
                    if (!stillExists)
                    {
                        SetRegistry(false);
                        return true;
                    }
                    Logger.Warn("Failed to delete scheduled tasks (maybe elevation required), falling back to registry");
                }
                SetRegistry(false);
                CleanLegacyRunEntries();
                bool still = false;
                try { still = svc.IsDaemonTaskRegistered(); } catch { }
                return !schtasksAvailable || !still ? true : false;
            }
        }
        catch (Exception ex)
        {
            Logger.Warn($"TaskSchedulerAutoStartManager.Set({enabled}) failed: {ex.Message}");
            try { SetRegistry(enabled); } catch { }
            return false;
        }
    }

    /// <summary>
    /// Sync current config to system (idempotent). Used at startup.
    /// </summary>
    public static void Sync(bool enabled)
    {
        // Best-effort; don't throw
        try { Set(enabled); } catch (Exception ex) { Logger.Warn($"Sync auto-start failed: {ex.Message}"); }
    }

    public static bool TryRunKeyStatsTask()
    {
        try
        {
            var svc = CreateService();
            if (!IsSchtasksAvailable(svc)) return false;
            if (!svc.IsKeyStatsTaskRegistered()) return false;
            return svc.TryRunTask(KeyStatsTaskPath);
        }
        catch (Exception ex)
        {
            Logger.Warn($"TryRunKeyStatsTask failed: {ex.Message}");
            return false;
        }
    }

    internal static bool IsSchtasksAvailable(ScheduledTaskService svc)
    {
        try
        {
            if (TestRunner != null) return true;
            if (!OperatingSystem.IsWindows()) return false;
            // Probe actual availability via service
            try { return svc.IsAvailable(); } catch { return true; }
        }
        catch
        {
            return false;
        }
    }

    private static bool IsRegistryRegistered()
    {
        try
        {
            using var key = Registry.CurrentUser.OpenSubKey(@"Software\Microsoft\Windows\CurrentVersion\Run");
            var value = key?.GetValue("PIM") as string;
            var exe = ExecutablePath;
            var quoted = exe.Contains(' ') ? $"\"{exe}\"" : exe;
            return !string.IsNullOrEmpty(value) &&
                   value.Equals(quoted, StringComparison.OrdinalIgnoreCase);
        }
        catch
        {
            return false;
        }
    }

    private static void SetRegistry(bool enabled)
    {
        try
        {
            using var key = Registry.CurrentUser.OpenSubKey(@"Software\Microsoft\Windows\CurrentVersion\Run", writable: true);
            var exe = ExecutablePath;
            var quoted = exe.Contains(' ') ? $"\"{exe}\"" : exe;
            if (enabled)
                key?.SetValue("PIM", quoted);
            else
                key?.DeleteValue("PIM", throwOnMissingValue: false);
        }
        catch (Exception ex)
        {
            Logger.Warn($"Failed to {(enabled ? "enable" : "disable")} registry auto-start: {ex.Message}");
        }
    }

    internal static void CleanLegacyRunEntries()
    {
        try
        {
            // HKCU
            using (var k = Registry.CurrentUser.OpenSubKey(@"Software\Microsoft\Windows\CurrentVersion\Run", writable: true))
            {
                k?.DeleteValue("PIM", false);
                k?.DeleteValue("KeyStats", false);
            }
            // HKLM (defensive, may require elevation)
            try
            {
                using var k2 = Registry.LocalMachine.OpenSubKey(@"Software\Microsoft\Windows\CurrentVersion\Run", writable: true);
                k2?.DeleteValue("PIM", false);
                k2?.DeleteValue("KeyStats", false);
            }
            catch { }
        }
        catch { }
    }

    internal static void CleanLegacyKeyStatsOnly()
    {
        try
        {
            using (var k = Registry.CurrentUser.OpenSubKey(@"Software\Microsoft\Windows\CurrentVersion\Run", writable: true))
            {
                k?.DeleteValue("KeyStats", false);
            }
            try
            {
                using var k2 = Registry.LocalMachine.OpenSubKey(@"Software\Microsoft\Windows\CurrentVersion\Run", writable: true);
                k2?.DeleteValue("KeyStats", false);
            }
            catch { }
        }
        catch { }
    }
}
