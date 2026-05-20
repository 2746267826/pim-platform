namespace Pim.Client.Core.Services;

public static class AwBucketSelection
{
    private static readonly HashSet<string> SupportedTypes = new(StringComparer.Ordinal)
    {
        "currentwindow",
        "afkstatus",
        "web.tab.current"
    };

    public static bool IsSupportedUploadBucket(string bucketId, string bucketType, string client)
    {
        if (string.Equals(bucketType, "os.hid.input", StringComparison.Ordinal))
            return false;

        if (string.Equals(client, "aw-watcher-input", StringComparison.Ordinal))
            return false;

        if (bucketId.StartsWith("aw-watcher-input_", StringComparison.Ordinal))
            return false;

        return SupportedTypes.Contains(bucketType);
    }

    public static string DescribeBucketKind(string bucketType)
    {
        return bucketType switch
        {
            "currentwindow" => "window",
            "afkstatus" => "afk",
            "web.tab.current" => "web",
            _ => "unknown"
        };
    }
}
