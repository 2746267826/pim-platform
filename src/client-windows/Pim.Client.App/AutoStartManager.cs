using Microsoft.Win32;
using Pim.Client.App.Services;

namespace Pim.Client.App;

/// <summary>
/// Manages Windows auto-start via HKCU\Run registry key.
/// </summary>
public static class AutoStartManager
{
    private const string RegistryKeyPath = @"Software\Microsoft\Windows\CurrentVersion\Run";
    private const string ValueName = "PIM";

    /// <summary>
    /// Whether the current executable is registered for auto-start.
    /// </summary>
    public static bool IsRegistered
    {
        get
        {
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
    /// </summary>
    public static void Set(bool enabled)
    {
        try
        {
            using var key = Registry.CurrentUser.OpenSubKey(RegistryKeyPath, writable: true);
            if (enabled)
            {
                key?.SetValue(ValueName, ExecutablePath);
            }
            else
            {
                key?.DeleteValue(ValueName, throwOnMissingValue: false);
            }
        }
        catch (Exception ex)
        {
            Logger.Warn($"Failed to {(enabled ? "enable" : "disable")} auto-start: {ex.Message}");
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
