namespace Pim.Client.Core.Services;

public static class UpdateChecker
{
    public static bool IsNewer(string? current, string? remote)
    {
        if (string.IsNullOrWhiteSpace(remote)) return false;
        if (string.IsNullOrWhiteSpace(current)) return true;
        var rv = TryParseVersion(remote!);
        var cv = TryParseVersion(current!);
        if (rv is not null && cv is not null) return rv.CompareTo(cv) > 0;
        return string.Compare(remote.Trim(), current.Trim(), StringComparison.Ordinal) > 0;
    }

    private static Version? TryParseVersion(string v)
    {
        var trimmed = v.Trim().TrimStart('v', 'V');
        if (trimmed.Length == 0) return null;
        // strip prerelease (+/-) metadata
        var core = trimmed.Split(new[] { '+', '-' }, 2)[0];
        core = core.Trim();
        if (Version.TryParse(core, out var ver)) return ver;
        // pad missing segments: e.g. "1" -> "1.0"
        var parts = core.Split('.');
        if (parts.All(p => int.TryParse(p, out _)))
        {
            // Try to normalize to at least 2 parts for Version
            if (parts.Length == 1) core += ".0";
            if (Version.TryParse(core, out ver)) return ver;
        }
        return null;
    }
}
