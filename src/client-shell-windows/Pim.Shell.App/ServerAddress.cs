namespace Pim.Shell.App;

public static class ServerAddress
{
    public static string? Normalize(string? input)
    {
        if (string.IsNullOrWhiteSpace(input)) return null;
        var trimmed = input.Trim();
        if (trimmed.Contains("://", StringComparison.Ordinal))
        {
            if (!trimmed.StartsWith("http://", StringComparison.OrdinalIgnoreCase) &&
                !trimmed.StartsWith("https://", StringComparison.OrdinalIgnoreCase))
                return null;
        }
        else
        {
            trimmed = "https://" + trimmed;
        }
        trimmed = trimmed.TrimEnd('/');
        if (!Uri.TryCreate(trimmed, UriKind.Absolute, out var uri)) return null;
        if (uri.Scheme != Uri.UriSchemeHttp && uri.Scheme != Uri.UriSchemeHttps) return null;
        return trimmed;
    }

    public static bool IsInsecure(string? normalizedServerUrl)
        => normalizedServerUrl is not null && normalizedServerUrl.StartsWith("http:", StringComparison.OrdinalIgnoreCase);
}
