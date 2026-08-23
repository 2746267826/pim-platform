namespace Pim.Shell.App;

public static class UpdateChecker
{
    public static bool IsNewer(string? current, string? remote)
    {
        if (string.IsNullOrWhiteSpace(remote)) return false;
        if (string.IsNullOrWhiteSpace(current)) return true;
        var rn = ParseN(remote!);
        var cn = ParseN(current!);
        if (rn != null && cn != null) return rn > cn;
        // 回退：非法格式按字符串比较并建议打 Warn（调用方负责日志）
        return string.Compare(remote.Trim(), current.Trim(), StringComparison.Ordinal) > 0;
    }
    private static int? ParseN(string v)
    {
        var trimmed = v.Trim();
        if (trimmed.Length == 0) return null;
        // 先去后缀 (+/- 之后均为构建元数据/预发布)，再取最后一段作为 N
        var coreVersion = trimmed.Split(new[]{'+','-'}).FirstOrDefault();
        if (coreVersion == null) return null;
        var last = coreVersion.Split('.').LastOrDefault();
        return int.TryParse(last, out var n) ? n : (int?)null;
    }
}
