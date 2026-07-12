using Pim.Client.Core.Models;

namespace Pim.Client.Core.Services;

public static class KeyStatsHealthProbe
{
    public static KeyStatsHealthResult Evaluate(
        IReadOnlyList<KeyStatsProcessInfo> processes,
        int currentSessionId,
        KeyStatsCounterSnapshot? snapshot,
        KeyStatsCounterSnapshot? previousSnapshot,
        string? apiError)
    {
        var processCount = processes.Count;
        var hasForeign = processes.Any(p => !p.IsCurrentUserSession || p.SessionId != currentSessionId);

        if (processCount == 0)
        {
            return new KeyStatsHealthResult(
                KeyStatsDetailState.MissingProcess,
                "Unavailable",
                CanUpload: false,
                SkipReason: "missing-process",
                processCount,
                hasForeign,
                snapshot,
                "KeyStats 进程未运行");
        }

        if (!string.IsNullOrWhiteSpace(apiError) || snapshot is null)
        {
            return new KeyStatsHealthResult(
                KeyStatsDetailState.ApiUnreachable,
                "Unavailable",
                CanUpload: false,
                SkipReason: "api-unreachable",
                processCount,
                hasForeign,
                snapshot,
                $"KeyStats API 不可达：{apiError ?? "empty snapshot"}");
        }

        var available = snapshot.HasAnyActivity || snapshot.GrewFrom(previousSnapshot);
        if (!available)
        {
            return new KeyStatsHealthResult(
                KeyStatsDetailState.ApiOkButStaleZero,
                "Unavailable",
                CanUpload: false,
                SkipReason: "stale-zero",
                processCount,
                hasForeign,
                snapshot,
                hasForeign
                    ? "KeyStats API 可达但计数全 0，且存在非当前会话实例"
                    : "KeyStats API 可达但计数全 0 或不增长");
        }

        return new KeyStatsHealthResult(
            KeyStatsDetailState.Available,
            "Available",
            CanUpload: true,
            SkipReason: null,
            processCount,
            hasForeign,
            snapshot,
            hasForeign
                ? "KeyStats 可用，但存在额外会话实例"
                : "KeyStats 可用");
    }
}
