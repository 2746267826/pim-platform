using System.Diagnostics;

namespace Pim.Client.Core.Services;

/// <summary>
/// Abstraction for schtasks execution to allow unit testing.
/// </summary>
public interface ISchtasksRunner
{
    (int ExitCode, string Output) Exec(params string[] args);
}

/// <summary>
/// Default runner using schtasks.exe via Process.
/// </summary>
public sealed class DefaultSchtasksRunner : ISchtasksRunner
{
    public (int ExitCode, string Output) Exec(params string[] args)
    {
        try
        {
            var psi = new ProcessStartInfo
            {
                FileName = "schtasks",
                UseShellExecute = false,
                RedirectStandardOutput = true,
                RedirectStandardError = true,
                CreateNoWindow = true,
            };
            foreach (var a in args)
                psi.ArgumentList.Add(a);

            using var p = Process.Start(psi);
            if (p is null) return (1, "failed to start schtasks");
            var outStr = p.StandardOutput.ReadToEnd();
            var errStr = p.StandardError.ReadToEnd();
            p.WaitForExit();
            return (p.ExitCode, outStr + errStr);
        }
        catch (Exception ex)
        {
            return (1, ex.Message);
        }
    }
}

/// <summary>
/// Manages \PIM\ scheduled tasks for daemon and KeyStats.
/// Uses schtasks.exe; falls back gracefully on failure.
/// </summary>
public sealed class ScheduledTaskService
{
    public const string DaemonTaskPath = @"\PIM\PIM Daemon";
    public const string KeyStatsTaskPath = @"\PIM\PIM KeyStats";
    public const string LegacyTaskName = "PimKeyStats";
    public const string LegacyTaskFull = @"\PimKeyStats";

    private readonly ISchtasksRunner _runner;

    public ScheduledTaskService(ISchtasksRunner? runner = null)
    {
        _runner = runner ?? new DefaultSchtasksRunner();
    }

    public bool TaskExists(string taskPath)
    {
        var (code, _) = _runner.Exec("/query", "/tn", taskPath);
        return code == 0;
    }

    public bool IsAvailable()
    {
        try
        {
            // Check executable existence first (Windows)
            if (OperatingSystem.IsWindows())
            {
                var sysDir = Environment.SystemDirectory;
                if (!string.IsNullOrEmpty(sysDir))
                {
                    var exe = Path.Combine(sysDir, "schtasks.exe");
                    if (File.Exists(exe)) return true;
                    // fallback: try runner
                }
            }
            else
            {
                return false;
            }

            var (code, output) = _runner.Exec("/query", "/fo", "LIST", "/v");
            // If runner reports "not found" or file missing, treat as unavailable
            if (!string.IsNullOrEmpty(output) && output.Contains("not found", StringComparison.OrdinalIgnoreCase))
                return false;
            if (output.Contains("No such file", StringComparison.OrdinalIgnoreCase))
                return false;
            // Even if no tasks, schtasks returns 0 or 1 but is available
            return true;
        }
        catch
        {
            return false;
        }
    }

    public bool TryCreateDaemonTask(string exePath)
    {
        return TryCreateTask(DaemonTaskPath, exePath, "limited");
    }

    public bool TryCreateKeyStatsTask(string exePath)
    {
        return TryCreateTask(KeyStatsTaskPath, exePath, "highest");
    }

    private bool TryCreateTask(string taskPath, string exePath, string runLevel)
    {
        var tr = exePath;
        // schtasks /tr expects quoted if path contains spaces; we pass via ArgumentList so quoting is handled,
        // but the value itself should be quoted for task's action. We provide "\"C:\...\"" as single arg.
        var quotedTr = $"\"{tr}\"";

        // Try with delay first (Win8+), then without
        var withDelay = new[] { "/create", "/tn", taskPath, "/tr", quotedTr, "/sc", "onlogon", "/rl", runLevel, "/f", "/delay", "0000:10" };
        var (code, _) = _runner.Exec(withDelay);
        if (code == 0) return true;

        var withoutDelay = new[] { "/create", "/tn", taskPath, "/tr", quotedTr, "/sc", "onlogon", "/rl", runLevel, "/f" };
        var (code2, _) = _runner.Exec(withoutDelay);
        return code2 == 0;
    }

    public bool TryDeleteTask(string taskPath)
    {
        var (code, _) = _runner.Exec("/delete", "/tn", taskPath, "/f");
        return code == 0;
    }

    public bool TryRunTask(string taskPath)
    {
        var (code, _) = _runner.Exec("/run", "/tn", taskPath);
        return code == 0;
    }

    public bool TryRunKeyStatsTask()
    {
        return TryRunTask(KeyStatsTaskPath);
    }

    public bool EnsureDaemonTask(string exePath)
    {
        // /create with /f overwrites existing, so this is idempotent
        return TryCreateDaemonTask(exePath);
    }

    public bool EnsureKeyStatsTask(string exePath)
    {
        return TryCreateKeyStatsTask(exePath);
    }

    public void CleanupLegacyTasks()
    {
        TryDeleteTask(LegacyTaskName);
        TryDeleteTask(LegacyTaskFull);
    }

    public bool IsDaemonTaskRegistered()
    {
        return TaskExists(DaemonTaskPath);
    }

    public bool IsKeyStatsTaskRegistered()
    {
        return TaskExists(KeyStatsTaskPath);
    }
}
