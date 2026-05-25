namespace Pim.Client.Core.Models;

public sealed record DaemonHeartbeatRequest(
    string DeviceId,
    string DaemonKind,
    string Version,
    string ServerUrl,
    DateTimeOffset? LastSuccessfulUploadAt,
    DateTimeOffset? LastAttemptedUploadAt,
    string? LastError,
    int? UploadQueueCount,
    string ActivityWatchState,
    string KeyStatsState,
    bool CollectionPaused,
    string StatusJson);
