namespace Pim.Client.Core.Services;

public static class UpdateChecker
{
    public static bool IsNewer(string? current, string? remote)
    {
        if (string.IsNullOrWhiteSpace(remote)) return false;
        if (string.IsNullOrWhiteSpace(current)) return true;
        var rn = ParseN(remote!);
        var cn = ParseN(current!);
        if (rn != null && cn != null) return rn > cn;
        return string.Compare(remote.Trim(), current.Trim(), StringComparison.Ordinal) > 0;
    }

    private static int? ParseN(string v)
    {
        var trimmed = v.Trim();
        if (trimmed.Length == 0) return null;
        var coreVersion = trimmed.Split(new[] { '+', '-' }).FirstOrDefault();
        if (coreVersion == null) return null;
        var last = coreVersion.Split('.').LastOrDefault();
        return int.TryParse(last, out var n) ? n : (int?)null;
    }
}
