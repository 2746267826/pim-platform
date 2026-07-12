namespace Pim.Client.Core.Models;

public enum KeyStatsDetailState
{
    MissingProcess,
    ApiUnreachable,
    ApiOkButStaleZero,
    Available
}

public sealed record KeyStatsProcessInfo(
    int ProcessId,
    int SessionId,
    bool IsCurrentUserSession);

public sealed record KeyStatsCounterSnapshot(
    int KeyPresses,
    int LeftClicks,
    int RightClicks,
    int MiddleClicks,
    int SideBackClicks,
    int SideForwardClicks,
    double MouseDistance,
    double ScrollDistance)
{
    public int TotalClicks =>
        LeftClicks + RightClicks + MiddleClicks + SideBackClicks + SideForwardClicks;

    public bool HasAnyActivity =>
        KeyPresses > 0 ||
        TotalClicks > 0 ||
        MouseDistance > 0 ||
        ScrollDistance > 0;

    public bool GrewFrom(KeyStatsCounterSnapshot? previous)
    {
        if (previous is null)
        {
            return false;
        }

        return KeyPresses > previous.KeyPresses ||
               TotalClicks > previous.TotalClicks ||
               MouseDistance > previous.MouseDistance ||
               ScrollDistance > previous.ScrollDistance;
    }
}

public sealed record KeyStatsHealthResult(
    KeyStatsDetailState DetailState,
    string DaemonSourceState,
    bool CanUpload,
    string? SkipReason,
    int ProcessCount,
    bool HasForeignSessionProcess,
    KeyStatsCounterSnapshot? Snapshot,
    string SummaryZh);
