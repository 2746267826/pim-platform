using System.Diagnostics;
using System.Runtime.InteropServices;
using System.Text;
using Pim.Client.Core.Models;

namespace Pim.Client.Core.Services;

public interface IWindowResolver
{
    TrackerWindowInfo? GetForegroundWindowInfo();
}

public sealed class DefaultWindowResolver : IWindowResolver
{
    public TrackerWindowInfo? GetForegroundWindowInfo()
    {
        if (!OperatingSystem.IsWindows())
            return null;

        try
        {
            var hwnd = Win32.GetForegroundWindow();
            if (hwnd == IntPtr.Zero) return null;

            Win32.GetWindowThreadProcessId(hwnd, out var pid);
            if (pid == 0) return null;

            var exePath = GetProcessPath(pid);
            if (string.IsNullOrWhiteSpace(exePath)) return null;

            var title = GetWindowText(hwnd);
            var commandLine = GetCommandLine(pid);

            // UWP handling: ApplicationFrameHost / ShellExperienceHost
            var appName = Path.GetFileNameWithoutExtension(exePath);
            if (string.Equals(appName, "ApplicationFrameHost", StringComparison.OrdinalIgnoreCase)
                || string.Equals(appName, "ShellExperienceHost", StringComparison.OrdinalIgnoreCase))
            {
                var childPid = ResolveChildPid(hwnd, pid);
                if (childPid is not null && childPid.Value != pid)
                {
                    var childPath = GetProcessPath(childPid.Value);
                    if (!string.IsNullOrWhiteSpace(childPath))
                    {
                        exePath = childPath!;
                        appName = Path.GetFileNameWithoutExtension(exePath);
                        var childCmd = GetCommandLine(childPid.Value);
                        if (!string.IsNullOrWhiteSpace(childCmd))
                            commandLine = childCmd;
                    }
                }
            }

            return new TrackerWindowInfo
            {
                Hwnd = hwnd,
                ProcessId = pid,
                ExePath = exePath,
                AppName = NormalizeAppName(appName),
                DisplayName = GetDisplayName(exePath, title),
                WindowTitle = title,
                CommandLine = commandLine,
                CapturedAt = DateTimeOffset.UtcNow
            };
        }
        catch
        {
            return null;
        }
    }

    private static string NormalizeAppName(string appName)
    {
        return appName.ToLowerInvariant();
    }

    private static string GetDisplayName(string exePath, string title)
    {
        try
        {
            var name = Path.GetFileNameWithoutExtension(exePath);
            return name ?? exePath;
        }
        catch { return exePath; }
    }

    private static string GetProcessPath(uint pid)
    {
        try
        {
            var hProcess = Win32.OpenProcess(Win32.PROCESS_QUERY_LIMITED_INFORMATION, false, pid);
            if (hProcess == IntPtr.Zero) return string.Empty;
            try
            {
                var sb = new StringBuilder(1024);
                var size = (uint)sb.Capacity;
                if (Win32.QueryFullProcessImageName(hProcess, 0, sb, ref size))
                    return sb.ToString(0, (int)size);
                return string.Empty;
            }
            finally
            {
                Win32.CloseHandle(hProcess);
            }
        }
        catch { return string.Empty; }
    }

    private static string GetWindowText(IntPtr hwnd)
    {
        try
        {
            var len = Win32.GetWindowTextLength(hwnd);
            if (len == 0) return string.Empty;
            var sb = new StringBuilder(len + 1);
            Win32.GetWindowText(hwnd, sb, sb.Capacity);
            return sb.ToString();
        }
        catch { return string.Empty; }
    }

    private static string? GetCommandLine(uint pid)
    {
        // CommandLine retrieval requires elevated WMI; simplified to null for now to avoid System.Management dependency on Linux
        // On Windows with admin, QueryFullProcessImageName already gives exePath; command line optional
        return null;
    }

    private static uint? ResolveChildPid(IntPtr parentHwnd, uint parentPid)
    {
        try
        {
            uint? found = null;
            Win32.EnumChildWindows(parentHwnd, (hwnd, lParam) =>
            {
                Win32.GetWindowThreadProcessId(hwnd, out var childPid);
                if (childPid != 0 && childPid != parentPid)
                {
                    // Heuristic: visible child window with non-empty title
                    if (Win32.IsWindowVisible(hwnd) && Win32.GetWindowTextLength(hwnd) > 0)
                    {
                        found = childPid;
                        return false; // stop enumeration
                    }
                }
                return true;
            }, IntPtr.Zero);
            return found;
        }
        catch { return null; }
    }

    private static class Win32
    {
        public const uint PROCESS_QUERY_LIMITED_INFORMATION = 0x1000;
        [DllImport("user32.dll")] public static extern IntPtr GetForegroundWindow();
        [DllImport("user32.dll")] public static extern uint GetWindowThreadProcessId(IntPtr hWnd, out uint lpdwProcessId);
        [DllImport("kernel32.dll")] public static extern IntPtr OpenProcess(uint dwDesiredAccess, bool bInheritHandle, uint dwProcessId);
        [DllImport("kernel32.dll")] public static extern bool CloseHandle(IntPtr hObject);
        [DllImport("kernel32.dll", CharSet = CharSet.Unicode)] public static extern bool QueryFullProcessImageName(IntPtr hProcess, uint dwFlags, StringBuilder lpExeName, ref uint lpdwSize);
        [DllImport("user32.dll", CharSet = CharSet.Unicode)] public static extern int GetWindowText(IntPtr hWnd, StringBuilder lpString, int nMaxCount);
        [DllImport("user32.dll")] public static extern int GetWindowTextLength(IntPtr hWnd);
        [DllImport("user32.dll")] public static extern bool EnumChildWindows(IntPtr hWndParent, EnumChildProc lpEnumFunc, IntPtr lParam);
        [DllImport("user32.dll")] public static extern bool IsWindowVisible(IntPtr hWnd);
        public delegate bool EnumChildProc(IntPtr hWnd, IntPtr lParam);
    }
}

public sealed class FallbackWindowResolver : IWindowResolver
{
    private readonly TrackerWindowInfo? _fixed;
    public FallbackWindowResolver(TrackerWindowInfo? fixedWindow = null) => _fixed = fixedWindow;
    public TrackerWindowInfo? GetForegroundWindowInfo() => _fixed;
}
