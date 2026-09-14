using System.Runtime.InteropServices;

namespace Pim.Client.Core.Services;

public interface IIdleDetector
{
    TimeSpan GetIdleDuration();
    bool IsScreenOff();
}

public sealed class WindowsIdleDetector : IIdleDetector
{
    public TimeSpan GetIdleDuration()
    {
        if (!OperatingSystem.IsWindows())
            return TimeSpan.Zero;

        try
        {
            var info = new Win32.LASTINPUTINFO { cbSize = (uint)Marshal.SizeOf<Win32.LASTINPUTINFO>() };
            if (!Win32.GetLastInputInfo(ref info))
                return TimeSpan.Zero;
            var tick = Win32.GetTickCount();
            var idleMs = tick - info.dwTime;
            // Handle wrap-around
            if (idleMs < 0) idleMs = 0;
            if (idleMs > int.MaxValue) idleMs = int.MaxValue;
            return TimeSpan.FromMilliseconds(idleMs);
        }
        catch { return TimeSpan.Zero; }
    }

    public bool IsScreenOff()
    {
        if (!OperatingSystem.IsWindows())
            return false;

        try
        {
            var hDesk = Win32.OpenInputDesktop(0, false, 0x0001 /* DESKTOP_READOBJECTS */);
            if (hDesk == IntPtr.Zero)
            {
                // When workstation is locked, screen off, or secure desktop active, OpenInputDesktop fails
                return true;
            }
            Win32.CloseDesktop(hDesk);
            return false;
        }
        catch
        {
            return false;
        }
    }

    private static class Win32
    {
        [StructLayout(LayoutKind.Sequential)]
        public struct LASTINPUTINFO
        {
            public uint cbSize;
            public uint dwTime;
        }
        [DllImport("user32.dll")] public static extern bool GetLastInputInfo(ref LASTINPUTINFO plii);
        [DllImport("kernel32.dll")] public static extern uint GetTickCount();
        [DllImport("user32.dll", SetLastError = true)] public static extern IntPtr OpenInputDesktop(uint dwFlags, bool fInherit, uint dwDesiredAccess);
        [DllImport("user32.dll", SetLastError = true)] public static extern bool CloseDesktop(IntPtr hDesktop);
    }
}

public sealed class FallbackIdleDetector : IIdleDetector
{
    private TimeSpan _idle;
    private bool _isScreenOff;
    public void SetIdle(TimeSpan idle) => _idle = idle;
    public void SetScreenOff(bool screenOff) => _isScreenOff = screenOff;
    public TimeSpan GetIdleDuration() => _idle;
    public bool IsScreenOff() => _isScreenOff;
}
