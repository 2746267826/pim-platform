using System.IO;
using System.Text;

namespace Pim.Client.Core.Services;

public sealed class TrackerLogger : IDisposable
{
    private readonly string _logDir;
    private readonly int _retentionDays;
    private readonly object _lock = new();
    private StreamWriter? _writer;
    private string? _currentFile;

    public TrackerLogger(int retentionDays = 30)
    {
        _retentionDays = retentionDays;
        _logDir = Path.Combine(Environment.GetFolderPath(Environment.SpecialFolder.LocalApplicationData), "PIM", "logs");
        Directory.CreateDirectory(_logDir);
        CleanOldLogs();
    }

    public void Info(string component, string message) => Write("INFO", component, message);
    public void Debug(string component, string message) => Write("DEBUG", component, message);
    public void Warn(string component, string message) => Write("WARN", component, message);
    public void Error(string component, string message, Exception? ex = null)
    {
        var extra = ex is null ? string.Empty : $" | {ex.GetType().Name}: {ex.Message}\n{ex.StackTrace}";
        Write("ERROR", component, message + extra);
    }

    private void Write(string level, string component, string message)
    {
        var timestamp = DateTime.Now.ToString("yyyy-MM-dd HH:mm:ss.fff");
        var line = $"[{timestamp}] [{level}] [{component}] {message}";
        lock (_lock)
        {
            try
            {
                EnsureWriter();
                _writer!.WriteLine(line);
                _writer.Flush();
            }
            catch { }
        }
        // Also forward to System.Diagnostics for debug visibility
        System.Diagnostics.Debug.WriteLine(line);
    }

    private void EnsureWriter()
    {
        var file = Path.Combine(_logDir, $"tracker-{DateTime.Now:yyyy-MM-dd}.log");
        if (_currentFile == file && _writer is not null) return;
        _writer?.Dispose();
        _currentFile = file;
        _writer = new StreamWriter(new FileStream(file, FileMode.Append, FileAccess.Write, FileShare.Read), Encoding.UTF8);
    }

    private void CleanOldLogs()
    {
        try
        {
            var cutoff = DateTime.Now.AddDays(-_retentionDays);
            foreach (var file in Directory.GetFiles(_logDir, "tracker-*.log"))
            {
                var name = Path.GetFileName(file);
                if (name.Length >= 16 && DateTime.TryParseExact(name.Substring(8, 10), "yyyy-MM-dd", null, System.Globalization.DateTimeStyles.None, out var d))
                {
                    if (d < cutoff)
                        File.Delete(file);
                }
            }
        }
        catch { }
    }

    public void Dispose()
    {
        lock (_lock)
        {
            _writer?.Dispose();
            _writer = null;
        }
    }
}
