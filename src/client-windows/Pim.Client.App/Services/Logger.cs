using System.Diagnostics;
using System.IO;

namespace Pim.Client.App.Services;

public static class Logger
{
    private static readonly string LogDir = Path.Combine(
        Environment.GetFolderPath(Environment.SpecialFolder.LocalApplicationData),
        "PIM", "logs");

    private static readonly string LogFile = Path.Combine(LogDir,
        $"pim-{DateTime.Now:yyyy-MM-dd}.log");

    private static readonly object _lock = new();

    public static void Info(string message) => Log("INFO", message, null);
    public static void Warn(string message) => Log("WARN", message, null);
    public static void Error(string message, Exception? ex = null) => Log("ERROR", message, ex);

    public static void Trace(string message)
    {
#if DEBUG
        Log("TRACE", message, null);
#endif
    }

    private static void Log(string level, string message, Exception? ex)
    {
        try
        {
            Directory.CreateDirectory(LogDir);
            var line = $"{DateTimeOffset.UtcNow:yyyy-MM-ddTHH:mm:ss.fffZ} [{level}] {message}";
            if (ex is not null)
                line += $"\n{ex}";
            lock (_lock)
            {
                File.AppendAllText(LogFile, line + Environment.NewLine);
            }
            Debug.WriteLine(line);
        }
        catch
        {
            // Last-resort fallback: output to debugger only
            Debug.WriteLine($"{level}: {message}");
            if (ex is not null) Debug.WriteLine(ex.ToString());
        }
    }

    public static string LogFilePath => LogFile;
}
