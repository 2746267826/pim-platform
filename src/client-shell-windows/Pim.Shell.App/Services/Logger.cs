using System.Diagnostics;

namespace Pim.Shell.App.Services;

public static class Logger
{
    public static void Info(string message) => Trace.WriteLine($"[Info] {message}");
    public static void Warn(string message, Exception? ex = null) => Trace.WriteLine($"[Warn] {message} {(ex != null ? ex.ToString() : "")}");
    public static void Error(string message, Exception? ex = null) => Trace.WriteLine($"[Error] {message} {(ex != null ? ex.ToString() : "")}");
}
