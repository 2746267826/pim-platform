namespace Pim.Client.Core.Services;

public static class UpdateChecker
{
    public static bool IsNewer(string? current, string? remote)
    {
        if (string.IsNullOrWhiteSpace(remote)) return false;
        if (string.IsNullOrWhiteSpace(current)) return true;
        var rn = ParseN(remote!);
        var cn = ParseN(current!);
        if (rn is not null && cn is not null) return rn.Value > cn.Value;
        return string.Compare(remote!.Trim(), current!.Trim(), StringComparison.Ordinal) > 0;
    }

    private static int? ParseN(string v)
    {
        var trimmed = v.Trim().TrimStart('v', 'V');
        if (trimmed.Length == 0) return null;
        // strip prerelease/build metadata like -pr.5 or +android.1, keep core before first +/-
        var core = trimmed.Split(new[] { '+', '-' }, 2)[0].Trim();
        if (core.Length == 0) return null;
        var last = core.Split('.').LastOrDefault();
        if (last != null && int.TryParse(last, out var n)) return n;
        return null;
    }
}
