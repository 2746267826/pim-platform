using System.IO;

namespace Pim.Client.App.Services;

/// <summary>
/// 极早期 bootstrap 同步日志：纯 File.AppendAllText，不依赖 Serilog / DI / 第三方库。
/// 文件：%LOCALAPPDATA%\PIM\logs\bootstrap.log；超约 512KB 滚动为 bootstrap.log.old 后重新开始。
/// 任何异常都静默吞掉，绝不影响主流程。
/// </summary>
public static class BootstrapLog
{
    private const long MaxFileBytes = 512 * 1024; // ~512KB

    // internal 而非 private readonly：供单元测试重定向到临时目录（测试经 Compile Link 编入同一程序集，可访问 internal）。
    internal static string LogDir = Path.Combine(
        Environment.GetFolderPath(Environment.SpecialFolder.LocalApplicationData),
        "PIM", "logs");
    internal static string FilePath = Path.Combine(LogDir, "bootstrap.log");
    internal static string OldFilePath = Path.Combine(LogDir, "bootstrap.log.old");
    private static readonly object Sync = new();

    public static void Write(string message)
    {
        try
        {
            lock (Sync)
            {
                Directory.CreateDirectory(LogDir);
                if (new FileInfo(FilePath) is { Exists: true } info && info.Length > MaxFileBytes)
                {
                    File.Move(FilePath, OldFilePath, overwrite: true);
                }

                var line = $"[{DateTime.Now:yyyy-MM-dd HH:mm:ss.fff}] [{Environment.ProcessId}] {message}{Environment.NewLine}";
                File.AppendAllText(FilePath, line);
            }
        }
        catch
        {
            // 静默吞掉：bootstrap 日志失败不得影响主流程。
        }
    }
}
