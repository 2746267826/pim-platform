using Pim.Client.Core.Models;

namespace Pim.Client.Core.Services;

public static class KeyStatsFixAdvisor
{
    public static KeyStatsFixSuggestion BuildSuggestion(KeyStatsHealthResult? health)
    {
        if (health is null)
            return new KeyStatsFixSuggestion(true, "尚无 KeyStats 健康探测结果。可尝试「一键修复」启动并复检。");

        if (health.DetailState == KeyStatsDetailState.Available && !health.HasForeignSessionProcess)
            return new KeyStatsFixSuggestion(false, "运行正常，无需修复。");

        if (health.DetailState == KeyStatsDetailState.Available && health.HasForeignSessionProcess)
            return new KeyStatsFixSuggestion(true,
                "KeyStats 可用，但存在额外会话实例。建议使用「一键修复」收敛为当前会话单实例。");

        var isStaleZero =
            string.Equals(health.SkipReason, "stale-zero", StringComparison.OrdinalIgnoreCase)
            || health.DetailState == KeyStatsDetailState.ApiOkButStaleZero;

        if (isStaleZero && health.HasForeignSessionProcess)
            return new KeyStatsFixSuggestion(true,
                "检测到非当前会话（常为 Session 0）实例可能占用本地 API。建议使用「一键修复」：结束非当前会话实例 → 在当前会话重启 KeyStats → 自动复检。");

        if (isStaleZero)
            return new KeyStatsFixSuggestion(true,
                "API 可达但计数全 0 或不增长。建议「一键修复」重启后，操作键鼠再刷新；若仍为 0，请复制诊断。");

        if (string.Equals(health.SkipReason, "missing-process", StringComparison.OrdinalIgnoreCase)
            || health.DetailState == KeyStatsDetailState.MissingProcess)
            return new KeyStatsFixSuggestion(true,
                "KeyStats 进程未运行。一键修复将在当前会话启动 KeyStats。");

        var isApiUnreachable =
            string.Equals(health.SkipReason, "api-unreachable", StringComparison.OrdinalIgnoreCase)
            || health.DetailState == KeyStatsDetailState.ApiUnreachable;

        if (isApiUnreachable && health.HasForeignSessionProcess)
            return new KeyStatsFixSuggestion(true,
                "KeyStats API 不可达，且存在非当前会话（常为 Session 0）实例可能占用端口。建议使用「一键修复」收敛后重启。");

        if (isApiUnreachable)
            return new KeyStatsFixSuggestion(true,
                "KeyStats API 不可达。一键修复将收敛进程并重启；若仍失败，请复制诊断。");

        return new KeyStatsFixSuggestion(true,
            $"{health.SummaryZh} 可尝试「一键修复」。");
    }
}
