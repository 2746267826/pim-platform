using System.IO;
using System.Text;
using System.Threading;

namespace Pim.Client.App.Services;

/// <summary>
/// 零依赖、纯同步的极简启动日志器，用于诊断 Serilog 尚未初始化或崩溃时的隐蔽退出。
/// 写入：%LOCALAPPDATA%\PIM\logs\bootstrap.log
/// 仅保留一个滚动旧文件 bootstrap.log.old（上限 512KB）。
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
        for (var attempt = 0; attempt < 3; attempt++)
        {
            try
            {
                lock (Sync)
                {
                    Directory.CreateDirectory(LogDir);
                    if (new FileInfo(FilePath) is { Exists: true } info && info.Length > MaxFileBytes)
                    {
                        try
                        {
                            File.Move(FilePath, OldFilePath, overwrite: true);
                        }
                        catch
                        {
                            // 滚动受阻时不丢弃当次日志，继续追加
                        }
                    }

                    var line = $"[{DateTime.Now:yyyy-MM-dd HH:mm:ss.fff}] [{Environment.ProcessId}] {message}{Environment.NewLine}";
                    using var stream = new FileStream(FilePath, FileMode.Append, FileAccess.Write, FileShare.ReadWrite);
                    using var writer = new StreamWriter(stream, Encoding.UTF8);
                    writer.Write(line);
                    return;
                }
            }
            catch
            {
                if (attempt == 2) break;
                Thread.Sleep(10);
            }
        }
    }
}
