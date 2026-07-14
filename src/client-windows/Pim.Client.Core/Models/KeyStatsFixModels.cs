namespace Pim.Client.Core.Models;

public sealed record KeyStatsStopResult(
    int ProcessId,
    bool Succeeded,
    string? Error);

public sealed record KeyStatsFixSuggestion(bool ShowActionHint, string MessageZh);

public enum KeyStatsFixOutcome
{
    Succeeded,
    Partial,
    Failed,
    Cancelled
}

public sealed record KeyStatsFixResult(
    KeyStatsFixOutcome Outcome,
    string Phase1MessageZh,
    string Phase2MessageZh,
    IReadOnlyList<int> StoppedProcessIds,
    IReadOnlyList<int> FailedStopProcessIds,
    bool ElevatedUsed,
    int? ScriptExitCode,
    string? ScriptOutputExcerpt,
    bool ApiReachable,
    bool CountersGrew);
