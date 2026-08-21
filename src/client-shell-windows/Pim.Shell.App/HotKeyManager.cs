using System;
using System.Runtime.InteropServices;

namespace Pim.Shell.App;

public sealed class HotKeyManager : IDisposable
{
    public const int DefaultKey = 0x4E; // N
    public const int DefaultModifiers = 0x0002 | 0x0001; // MOD_CONTROL | MOD_ALT
    public const int MOD_ALT = 0x0001;
    public const int MOD_CONTROL = 0x0002;

    [DllImport("user32.dll")] private static extern bool RegisterHotKey(IntPtr hWnd, int id, uint fsModifiers, uint vk);
    [DllImport("user32.dll")] private static extern bool UnregisterHotKey(IntPtr hWnd, int id);

    private readonly IntPtr _hwnd;
    private readonly int _id;
    private bool _registered;

    public HotKeyManager(IntPtr hwnd, int id = 9000, int modifiers = DefaultModifiers, int key = DefaultKey)
    {
        _hwnd = hwnd;
        _id = id;
        if (RegisterHotKey(_hwnd, _id, (uint)modifiers, (uint)key)) _registered = true;
    }

    public static int BuildId(int modifiers, int key) => (modifiers << 16) | key;

    public void Dispose()
    {
        if (_registered) UnregisterHotKey(_hwnd, _id);
    }
}
