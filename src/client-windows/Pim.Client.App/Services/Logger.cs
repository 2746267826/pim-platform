using System.Diagnostics;
using System.IO;
using Serilog;
using Serilog.Events;

namespace Pim.Client.App.Services;

public static class Logger
{
    private static readonly string LogDir = Path.Combine(
        Environment.GetFolderPath(Environment.SpecialFolder.LocalApplicationData),
        "PIM", "logs");

    private static ILogger? _serilog;

    public static void Initialize()
    {
        Directory.CreateDirectory(LogDir);

        var logFile = Path.Combine(LogDir, "pim-daemon-.log");
        _serilog = new LoggerConfiguration()
            .MinimumLevel.Debug()
            .WriteTo.Debug()
            .WriteTo.File(
                logFile,
                rollingInterval: RollingInterval.Day,
                retainedFileCountLimit: 30,
                outputTemplate: "{Timestamp:yyyy-MM-ddTHH:mm:ss.fffZ} [{Level}] {Message:lj}{NewLine}{Exception}")
            .CreateLogger();
    }

    public static void Info(string message) => Write(LogEventLevel.Information, message, null);
    public static void Warn(string message) => Write(LogEventLevel.Warning, message, null);
    public static void Error(string message, Exception? ex = null) => Write(LogEventLevel.Error, message, ex);

    public static void Trace(string message)
    {
#if DEBUG
        Write(LogEventLevel.Verbose, message, null);
#endif
    }

    private static void Write(LogEventLevel level, string message, Exception? ex)
    {
        if (_serilog is not null)
        {
            _serilog.Write(level, ex, message);
        }
        else
        {
            // Fallback before Serilog is initialized
            Debug.WriteLine($"{level}: {message}");
            if (ex is not null) Debug.WriteLine(ex.ToString());
        }
    }

    private static string? _logFilePath;
    public static string LogFilePath => _logFilePath ??= Path.Combine(LogDir, $"pim-daemon-{DateTime.Now:yyyy-MM-dd}.log");
}
