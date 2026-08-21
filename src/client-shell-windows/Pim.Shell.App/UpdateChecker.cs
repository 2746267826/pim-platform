namespace Pim.Shell.App;

public static class UpdateChecker
{
    public static bool IsNewer(string? current, string? remote)
    {
        if (string.IsNullOrWhiteSpace(remote)) return false;
        if (string.IsNullOrWhiteSpace(current)) return true;
        return string.Compare(remote.Trim(), current.Trim(), StringComparison.Ordinal) != 0
            && string.Compare(remote.Trim(), current.Trim(), StringComparison.Ordinal) > 0;
    }
}
