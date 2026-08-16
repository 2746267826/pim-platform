namespace Pim.Client.Core.Models;

public sealed record PlannedOfflineRequest(
    string DeviceId,
    string DaemonKind,
    string? Reason,
    DateTimeOffset? OccurredAt);