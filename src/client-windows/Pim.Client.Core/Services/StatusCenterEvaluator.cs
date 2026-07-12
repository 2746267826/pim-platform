namespace Pim.Client.Core.Services;

public static class StatusCenterEvaluator
{
    public static string Rate(
        bool authenticated,
        string activityWatchState,
        string keyStatsState,
        string? keyStatsSkipReason,
        int awQueueCount)
    {
        var awOk = string.Equals(activityWatchState, "Available", StringComparison.OrdinalIgnoreCase);
        var ksOk = string.Equals(keyStatsState, "Available", StringComparison.OrdinalIgnoreCase);
        var hasSkip = !string.IsNullOrWhiteSpace(keyStatsSkipReason);
        var hasQueue = awQueueCount > 0;

        if (!authenticated || (!awOk && !ksOk))
            return "不可用";
        if (awOk && ksOk && !hasSkip && !hasQueue)
            return "正常";
        return "部分异常";
    }
}
