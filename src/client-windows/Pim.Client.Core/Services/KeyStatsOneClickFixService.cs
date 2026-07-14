using System.ComponentModel;
using System.Diagnostics;
using System.Text;
using Pim.Client.Core.Models;

namespace Pim.Client.Core.Services;

public sealed class KeyStatsOneClickFixService
{
    public const string FixScriptFileName = "fix-keystats-session.ps1";
    public const string FixLogFileName = "pim-keystats-fix-last.log";

    private readonly KeyStatsProcessManager _processes;
    private readonly KeyStatsLocalStatsClient _stats;
    private readonly Func<IReadOnlyList<int>, IReadOnlyList<KeyStatsStopResult>> _stop;
    private readonly Action<string> _start;
    private readonly Func<string, string, string, Task<(int ExitCode, string Output, bool Cancelled)>> _runElevatedScript;
    private readonly Func<CancellationToken, Task> _delayPhase1;
    private readonly Func<CancellationToken, Task> _delayPhase2;
    private readonly Func<int, IReadOnlyList<KeyStatsProcessInfo>> _listProcesses;

    public KeyStatsOneClickFixService(
        KeyStatsProcessManager processes,
        KeyStatsLocalStatsClient stats,
        Func<IReadOnlyList<int>, IReadOnlyList<KeyStatsStopResult>>? stop = null,
        Action<string>? start = null,
        Func<string, string, string, Task<(int ExitCode, string Output, bool Cancelled)>>? runElevatedScript = null,
        Func<CancellationToken, Task>? delayPhase1 = null,
        Func<CancellationToken, Task>? delayPhase2 = null,
        Func<int, IReadOnlyList<KeyStatsProcessInfo>>? listProcesses = null)
    {
        _processes = processes;
        _stats = stats;
        _stop = stop ?? (ids => processes.StopProcesses(ids));
        _start = start ?? processes.StartInCurrentSession;
        _runElevatedScript = runElevatedScript ?? DefaultRunElevatedAsync;
        _delayPhase1 = delayPhase1 ?? (ct => Task.Delay(1500, ct));
        _delayPhase2 = delayPhase2 ?? (ct => Task.Delay(8000, ct));
        _listProcesses = listProcesses ?? processes.ListProcesses;
    }

    public async Task<KeyStatsFixResult> RunAsync(
        string keyStatsExePath,
        string fixScriptPath,
        int currentSessionId,
        Func<string, bool> confirmElevation,
        CancellationToken ct = default)
    {
        if (!File.Exists(keyStatsExePath))
        {
            return FailEarly(
                "未找到 KeyStats.exe，请确认客户端安装目录完整。",
                stopped: Array.Empty<int>(),
                failed: Array.Empty<int>());
        }

        var listed = _listProcesses(currentSessionId);
        var plan = KeyStatsProcessManager.BuildConvergencePlan(listed, currentSessionId);
        var stopResults = _stop(plan.ProcessIdsToStop);
        var stoppedIds = stopResults.Where(r => r.Succeeded).Select(r => r.ProcessId).ToArray();
        var failedIds = KeyStatsProcessManager.FailedStopIds(stopResults).ToArray();

        listed = _listProcesses(currentSessionId);
        // Foreign remaining always elevates. Access-denied only elevates if those PIDs still listed.
        // Timeout-failed current-session PIDs still present without foreign do not elevate.
        var failedStillPresent = failedIds.Any(id => listed.Any(p => p.ProcessId == id));
        var needsElevate =
            HasForeign(listed, currentSessionId) ||
            (KeyStatsProcessManager.NeedsElevation(stopResults) && failedStillPresent);

        var elevatedUsed = false;
        int? scriptExit = null;
        string? scriptOut = null;

        if (needsElevate)
        {
            if (!File.Exists(fixScriptPath))
            {
                return FailEarly(
                    "需要管理员权限清理非当前会话进程，但修复脚本缺失。请重装客户端或复制 fix-keystats-session.ps1。",
                    stoppedIds,
                    failedIds);
            }

            var confirmMessage =
                "普通权限无法结束部分 KeyStats 进程（可能位于其他会话）。将仅以管理员身份运行修复脚本清理进程，不会提升主程序权限。是否继续？";
            if (!confirmElevation(confirmMessage))
            {
                return new KeyStatsFixResult(
                    KeyStatsFixOutcome.Cancelled,
                    "用户取消了管理员修复确认。",
                    string.Empty,
                    stoppedIds,
                    failedIds,
                    ElevatedUsed: false,
                    ScriptExitCode: null,
                    ScriptOutputExcerpt: null,
                    ApiReachable: false,
                    CountersGrew: false);
            }

            ct.ThrowIfCancellationRequested();
            var logPath = ResolveSharedLogPath();
            var (exitCode, output, cancelled) = await _runElevatedScript(fixScriptPath, keyStatsExePath, logPath);
            scriptExit = exitCode;
            scriptOut = Truncate(output, 2048);

            if (cancelled)
            {
                return new KeyStatsFixResult(
                    KeyStatsFixOutcome.Cancelled,
                    "用户取消了 UAC 提权提示。",
                    string.Empty,
                    stoppedIds,
                    failedIds,
                    ElevatedUsed: false,
                    ScriptExitCode: null,
                    ScriptOutputExcerpt: scriptOut,
                    ApiReachable: false,
                    CountersGrew: false);
            }

            if (exitCode != 0)
            {
                return new KeyStatsFixResult(
                    KeyStatsFixOutcome.Failed,
                    $"管理员修复脚本失败（退出码 {exitCode}）。{ExcerptHint(scriptOut)}",
                    string.Empty,
                    stoppedIds,
                    failedIds,
                    ElevatedUsed: true,
                    ScriptExitCode: exitCode,
                    ScriptOutputExcerpt: scriptOut,
                    ApiReachable: false,
                    CountersGrew: false);
            }

            elevatedUsed = true;
            try
            {
                _start(keyStatsExePath);
            }
            catch (Exception ex)
            {
                return new KeyStatsFixResult(
                    KeyStatsFixOutcome.Failed,
                    $"管理员清理成功，但启动 KeyStats 失败：{ex.Message}",
                    string.Empty,
                    stoppedIds,
                    failedIds,
                    ElevatedUsed: true,
                    ScriptExitCode: exitCode,
                    ScriptOutputExcerpt: scriptOut,
                    ApiReachable: false,
                    CountersGrew: false);
            }
        }
        else
        {
            var hasCurrent = listed.Any(p => p.IsCurrentUserSession && p.SessionId == currentSessionId);
            if (plan.ShouldStart || !hasCurrent)
                _start(keyStatsExePath);
        }

        await _delayPhase1(ct);
        ct.ThrowIfCancellationRequested();

        listed = _listProcesses(currentSessionId);
        var (snap1, err1) = await _stats.GetSnapshotAsync(ct);
        var apiOk = snap1 is not null && err1 is null;
        var processOk = IsProcessOk(listed, currentSessionId);
        var phase1 = BuildPhase1Message(stoppedIds, failedIds, elevatedUsed, processOk, apiOk, listed, currentSessionId);

        await _delayPhase2(ct);
        ct.ThrowIfCancellationRequested();

        var (snap2, err2) = await _stats.GetSnapshotAsync(ct);
        if (snap2 is not null && err2 is null)
            apiOk = true;
        else if (snap2 is null)
            apiOk = false;

        listed = _listProcesses(currentSessionId);
        processOk = IsProcessOk(listed, currentSessionId);
        var grew = KeyStatsLocalStatsClient.CountersIndicateRecovery(snap1, snap2);

        var outcome = ResolveOutcome(processOk, apiOk, grew);
        var phase2 = BuildPhase2Message(outcome, apiOk, processOk);

        return new KeyStatsFixResult(
            outcome,
            phase1,
            phase2,
            stoppedIds,
            failedIds,
            elevatedUsed,
            scriptExit,
            scriptOut,
            apiOk,
            grew);
    }

    internal static string ResolveSharedLogPath()
    {
        var dir = Path.Combine(
            Environment.GetFolderPath(Environment.SpecialFolder.LocalApplicationData),
            "PIM");
        Directory.CreateDirectory(dir);
        return Path.Combine(dir, FixLogFileName);
    }

    private static KeyStatsFixOutcome ResolveOutcome(bool processOk, bool apiOk, bool grew)
    {
        if (!processOk)
            return KeyStatsFixOutcome.Failed;
        if (!apiOk)
            return KeyStatsFixOutcome.Failed;
        if (!grew)
            return KeyStatsFixOutcome.Partial;
        return KeyStatsFixOutcome.Succeeded;
    }

    private static string BuildPhase1Message(
        IReadOnlyList<int> stoppedIds,
        IReadOnlyList<int> failedIds,
        bool elevatedUsed,
        bool processOk,
        bool apiOk,
        IReadOnlyList<KeyStatsProcessInfo> listed,
        int currentSessionId)
    {
        var sb = new StringBuilder();
        sb.Append($"成功结束 {stoppedIds.Count} 个进程");
        if (failedIds.Count > 0)
            sb.Append($"，失败 {failedIds.Count} 个");
        sb.Append('。');
        if (elevatedUsed)
            sb.Append("已使用管理员脚本清理。");
        sb.Append(processOk
            ? "当前会话进程状态正常。"
            : HasForeign(listed, currentSessionId)
                ? "仍存在非当前会话进程。"
                : "当前会话未检测到 KeyStats 进程。");
        sb.Append(apiOk ? "本地 API 可达。" : "本地 API 不可达。");
        return sb.ToString();
    }

    private static string BuildPhase2Message(
        KeyStatsFixOutcome outcome,
        bool apiOk,
        bool processOk)
    {
        if (outcome == KeyStatsFixOutcome.Succeeded)
            return "计数开始增长，修复成功。";
        if (processOk && apiOk)
            return "API 可达但计数仍为 0。请敲几下键盘或移动鼠标后点「刷新」。";
        if (!processOk)
            return "进程状态异常，请复制诊断后重试。";
        return "本地 API 仍不可达，请复制诊断后重试。";
    }

    private static bool HasForeign(IReadOnlyList<KeyStatsProcessInfo> processes, int currentSessionId)
        => processes.Any(p => !p.IsCurrentUserSession || p.SessionId != currentSessionId);

    private static bool IsProcessOk(IReadOnlyList<KeyStatsProcessInfo> processes, int currentSessionId)
    {
        var hasCurrent = processes.Any(p => p.IsCurrentUserSession && p.SessionId == currentSessionId);
        return hasCurrent && !HasForeign(processes, currentSessionId);
    }

    private static KeyStatsFixResult FailEarly(
        string phase1,
        IReadOnlyList<int> stopped,
        IReadOnlyList<int> failed)
        => new(
            KeyStatsFixOutcome.Failed,
            phase1,
            string.Empty,
            stopped,
            failed,
            ElevatedUsed: false,
            ScriptExitCode: null,
            ScriptOutputExcerpt: null,
            ApiReachable: false,
            CountersGrew: false);

    private static string? Truncate(string? text, int max)
    {
        if (string.IsNullOrEmpty(text))
            return text;
        return text.Length <= max ? text : text[^max..];
    }

    private static string ExcerptHint(string? excerpt)
        => string.IsNullOrWhiteSpace(excerpt) ? string.Empty : $" 输出摘要：{excerpt}";

    private const int ErrorCancelled = 1223;
    private const int ElevateWaitTimeoutMs = 60_000;

    private static async Task<(int ExitCode, string Output, bool Cancelled)> DefaultRunElevatedAsync(
        string scriptPath,
        string keyStatsExePath,
        string logPath)
    {
        try
        {
            var psi = new ProcessStartInfo
            {
                FileName = "powershell.exe",
                Arguments =
                    $"-NoProfile -ExecutionPolicy Bypass -File \"{scriptPath}\" -KeyStatsExe \"{keyStatsExePath}\" -LogPath \"{logPath}\"",
                UseShellExecute = true,
                Verb = "runas",
                WorkingDirectory = Path.GetDirectoryName(scriptPath) ?? Environment.CurrentDirectory
            };

            return await Task.Run(() =>
            {
                using var process = Process.Start(psi);
                if (process is null)
                    return (-1, "failed to start elevated powershell", false);

                if (!process.WaitForExit(ElevateWaitTimeoutMs))
                {
                    try { process.Kill(entireProcessTree: true); } catch { /* best effort */ }
                    return (-1, $"elevated script timed out after {ElevateWaitTimeoutMs / 1000}s", false);
                }

                var output = ReadLogExcerpt(logPath);
                return (process.ExitCode, output, false);
            }).ConfigureAwait(false);
        }
        catch (Win32Exception ex) when (ex.NativeErrorCode == ErrorCancelled)
        {
            return (-1, ex.Message, true);
        }
        catch (Win32Exception ex)
        {
            return (-1, ex.Message, false);
        }
        catch (Exception ex)
        {
            return (-1, ex.Message, false);
        }
    }

    private static string ReadLogExcerpt(string logPath)
    {
        try
        {
            if (!File.Exists(logPath))
                return string.Empty;

            using var stream = new FileStream(logPath, FileMode.Open, FileAccess.Read, FileShare.ReadWrite);
            var length = stream.Length;
            const int max = 2048;
            if (length > max)
                stream.Seek(-max, SeekOrigin.End);

            using var reader = new StreamReader(stream, Encoding.UTF8, detectEncodingFromByteOrderMarks: true);
            return reader.ReadToEnd();
        }
        catch
        {
            return string.Empty;
        }
    }
}
