namespace Pim.Client.Core.Services;

public sealed record EndpointOperationBoundaryResult(bool AllowedOffline, string Kind, string Message);

public class EndpointCollectionBoundaryService
{
    private static readonly HashSet<string> OfflineQueueableOperations = new(StringComparer.OrdinalIgnoreCase)
    {
        "collection-upload",
        "pc-activity",
        "window-context",
        "browser-context",
        "input-activity",
        "device-state",
        "upload-retry"
    };

    public bool CanQueueOffline(string operationKind)
        => OfflineQueueableOperations.Contains(operationKind.Trim());

    public EndpointOperationBoundaryResult Guard(string operationKind)
        => CanQueueOffline(operationKind)
            ? new EndpointOperationBoundaryResult(true, "QueuedOffline", "采集类上传可离线缓存并稍后重试。")
            : new EndpointOperationBoundaryResult(false, "BlockedOnlineOnly", "事实变更、确认、报告编辑、Outlook 回写和恢复删除必须在线执行。");
}
