using System.Diagnostics;
using Pim.Client.Core.Models;

namespace Pim.Client.Core.Services;

public sealed record KeyStatsConvergencePlan(
    int? KeepProcessId,
    IReadOnlyList<int> ProcessIdsToStop,
    bool ShouldStart);

public sealed class KeyStatsProcessManager
{
    public const string ProcessName = "KeyStats";
    public const string ExeFileName = "KeyStats.exe";

    public static KeyStatsConvergencePlan BuildConvergencePlan(
        IReadOnlyList<KeyStatsProcessInfo> processes,
        int currentSessionId)
    {
        var current = processes
            .Where(p => p.IsCurrentUserSession && p.SessionId == currentSessionId)
            .OrderBy(p => p.ProcessId)
            .ToList();
        var foreign = processes
            .Where(p => !(p.IsCurrentUserSession && p.SessionId == currentSessionId))
            .Select(p => p.ProcessId)
            .ToList();

        if (current.Count == 0)
        {
            return new KeyStatsConvergencePlan(null, foreign, ShouldStart: true);
        }

        var keep = current[0].ProcessId;
        var stopExtraCurrent = current.Skip(1).Select(p => p.ProcessId);
        var stop = foreign.Concat(stopExtraCurrent).Distinct().OrderBy(id => id).ToArray();
        return new KeyStatsConvergencePlan(keep, stop, ShouldStart: false);
    }

    public IReadOnlyList<KeyStatsProcessInfo> ListProcesses(int currentSessionId)
    {
        var result = new List<KeyStatsProcessInfo>();
        foreach (var process in Process.GetProcessesByName(ProcessName))
        {
            try
            {
                var sessionId = process.SessionId;
                result.Add(new KeyStatsProcessInfo(
                    process.Id,
                    sessionId,
                    sessionId == currentSessionId));
            }
            catch
            {
                // ignore processes that exit mid-enumeration
            }
            finally
            {
                process.Dispose();
            }
        }

        return result;
    }

    public KeyStatsConvergencePlan EnsureRunning(string keyStatsExePath, int currentSessionId)
    {
        var processes = ListProcesses(currentSessionId);
        var plan = BuildConvergencePlan(processes, currentSessionId);

        foreach (var pid in plan.ProcessIdsToStop)
        {
            TryStop(pid);
        }

        if (plan.ShouldStart)
        {
            StartInCurrentSession(keyStatsExePath);
        }

        return plan;
    }

    public void Restart(string keyStatsExePath, int currentSessionId)
    {
        foreach (var process in ListProcesses(currentSessionId))
        {
            TryStop(process.ProcessId);
        }

        StartInCurrentSession(keyStatsExePath);
    }

    private static void TryStop(int processId)
    {
        try
        {
            using var process = Process.GetProcessById(processId);
            process.Kill(entireProcessTree: true);
            process.WaitForExit(3000);
        }
        catch
        {
            // best effort
        }
    }

    private static void StartInCurrentSession(string keyStatsExePath)
    {
        if (!File.Exists(keyStatsExePath))
        {
            throw new FileNotFoundException("KeyStats.exe not found", keyStatsExePath);
        }

        Process.Start(new ProcessStartInfo
        {
            FileName = keyStatsExePath,
            UseShellExecute = true,
            WorkingDirectory = Path.GetDirectoryName(keyStatsExePath) ?? Environment.CurrentDirectory
        });
    }
}
