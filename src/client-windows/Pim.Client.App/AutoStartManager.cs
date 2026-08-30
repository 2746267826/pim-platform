using Microsoft.Win32;
using Pim.Client.App.Services;

namespace Pim.Client.App;

/// <summary>
/// Manages Windows auto-start. New installs use Task Scheduler (\PIM\PIM Daemon + \PIM\PIM KeyStats);
/// legacy HKCU\Run is retained as fallback and cleaned up automatically.
/// </summary>
public static class AutoStartManager
{
    private const string RegistryKeyPath = @"Software\Microsoft\Windows\CurrentVersion\Run";
    private const string ValueName = "PIM";

    /// <summary>
    /// Whether the current executable is registered for auto-start.
    /// Prefers Task Scheduler when available, falls back to registry.
    /// </summary>
    public static bool IsRegistered
    {
        get
        {
            try
            {
                // Prefer scheduled task when schtasks available (or test runner injected)
                if (TaskSchedulerAutoStartManager.TestRunner != null || OperatingSystem.IsWindows())
                {
                    try
                    {
                        return TaskSchedulerAutoStartManager.IsRegistered;
                    }
                    catch
                    {
                        // fall through to registry
                    }
                }
            }
            catch { }

            try
            {
                using var key = Registry.CurrentUser.OpenSubKey(RegistryKeyPath);
                var value = key?.GetValue(ValueName) as string;
                return !string.IsNullOrEmpty(value) &&
                       value.Equals(ExecutablePath, StringComparison.OrdinalIgnoreCase);
            }
            catch
            {
                return false;
            }
        }
    }

    /// <summary>
    /// Set or clear auto-start according to <paramref name="enabled"/>.
    /// Delegates to TaskSchedulerAutoStartManager when possible; handles elevation fallback.
    /// Returns true on success (for callers that need to show elevation prompt).
    /// </summary>
    public static bool Set(bool enabled)
    {
        try
        {
            // Delegate to task scheduler manager which also cleans legacy registry entries.
            // On Windows, this will attempt schtasks; on non-Windows or when unavailable it falls back to registry internally.
            var ok = TaskSchedulerAutoStartManager.Set(enabled);
            return ok;
        }
        catch (Exception ex)
        {
            Logger.Warn($"Failed to {(enabled ? "enable" : "disable")} auto-start: {ex.Message}");
            return false;
        }
    }

    /// <summary>
    /// Legacy direct registry setter kept for compatibility/fallback.
    /// </summary>
    internal static void SetRegistryDirect(bool enabled)
    {
        try
        {
            using var key = Registry.CurrentUser.OpenSubKey(RegistryKeyPath, writable: true);
            if (enabled)
                key?.SetValue(ValueName, ExecutablePath);
            else
                key?.DeleteValue(ValueName, throwOnMissingValue: false);
        }
        catch (Exception ex)
        {
            Logger.Warn($"Failed to {(enabled ? "enable" : "disable")} registry auto-start: {ex.Message}");
        }
    }

    /// <summary>
    /// Full path of the current executable, quoted if it contains spaces.
    /// </summary>
    private static string ExecutablePath
    {
        get
        {
            var path = Environment.ProcessPath ?? "";
            return path.Contains(' ') ? $"\"{path}\"" : path;
        }
    }
}
