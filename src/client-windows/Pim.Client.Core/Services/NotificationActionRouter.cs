namespace Pim.Client.Core.Services;

public sealed record NotificationActionRoute(string Kind, string? DetailUrl, string Message);

public class NotificationActionRouter
{
    private static readonly HashSet<string> HighRiskLevels = new(StringComparer.OrdinalIgnoreCase)
    {
        "L2PimFactChange",
        "L3ExternalSourceOrWriteback",
        "L4BatchOrDestructiveGovernance",
        "Medium",
        "High"
    };

    public NotificationActionRoute Route(
        string action,
        string riskLevel,
        string? confirmationId = null,
        string? relatedObjectType = null,
        string? relatedObjectId = null)
    {
        if (HighRiskLevels.Contains(riskLevel))
        {
            return new NotificationActionRoute(
                "OpenDetailRequired",
                BuildDetailUrl(confirmationId, relatedObjectType, relatedObjectId),
                "高风险通知动作需要在 Web 审计详情中确认。");
        }

        var normalizedAction = action.Trim().ToLowerInvariant();
        return normalizedAction is "dismiss" or "snooze" or "open" or "complete"
            ? new NotificationActionRoute("Executed", null, "低风险通知动作可在线直接执行。")
            : new NotificationActionRoute("Rejected", null, $"不支持的通知动作：{action}");
    }

    private static string BuildDetailUrl(string? confirmationId, string? relatedObjectType, string? relatedObjectId)
    {
        if (!string.IsNullOrWhiteSpace(confirmationId))
        {
            return $"/confirmations/{Uri.EscapeDataString(confirmationId)}";
        }

        if (!string.IsNullOrWhiteSpace(relatedObjectType) && !string.IsNullOrWhiteSpace(relatedObjectId))
        {
            return $"/audit/{Uri.EscapeDataString(relatedObjectType)}/{Uri.EscapeDataString(relatedObjectId)}";
        }

        return "/confirmations";
    }
}
