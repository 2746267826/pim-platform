using Pim.Client.Core.Models;

namespace Pim.Client.Core.Services;

/// <summary>
/// Normalizes incoming site-level events (from the Time Tracker fork) and
/// decides which are safe to forward to the server. Pure and stateless so it
/// is trivially unit-testable; the bridge only counts its verdicts.
/// </summary>
public static class SiteEventNormalizer
{
    private const int MaxHostLength = 253;
    private const long MaxSpanMs = 24 * 60 * 60 * 1000L;

    private static readonly HashSet<string> AllowedKinds = new(StringComparer.OrdinalIgnoreCase)
    {
        "focus", "tick", "visit", "run", "media",
    };

    public static SiteEventDto? Normalize(SiteEventDto raw)
    {
        if (raw is null) return null;

        var kind = raw.Kind?.Trim().ToLowerInvariant();
        if (string.IsNullOrEmpty(kind) || !AllowedKinds.Contains(kind)) return null;

        var host = raw.Host?.Trim().ToLowerInvariant();
        if (string.IsNullOrEmpty(host) || host.Length > MaxHostLength) return null;

        var normalized = new SiteEventDto
        {
            Kind = kind,
            Host = host,
            StartMs = raw.StartMs,
            EndMs = raw.EndMs,
            DurationMs = raw.DurationMs,
            Date = raw.Date,
            At = raw.At,
        };

        switch (kind)
        {
            case "focus":
                if (raw.StartMs is not long focusStart || raw.EndMs is not long focusEnd) return null;
                if (focusEnd < focusStart || focusEnd - focusStart > MaxSpanMs) return null;
                normalized.StartMs = focusStart;
                normalized.EndMs = focusEnd;
                normalized.DurationMs = focusEnd - focusStart;
                break;

            case "tick":
                if (raw.StartMs is not long tickStart) return null;
                if (raw.DurationMs is not long tickDuration || tickDuration <= 0 || tickDuration > MaxSpanMs) return null;
                normalized.StartMs = tickStart;
                normalized.DurationMs = tickDuration;
                break;

            case "visit":
                break;

            case "run":
            case "media":
                if (string.IsNullOrWhiteSpace(raw.Date)) return null;
                if (!DateTime.TryParseExact(raw.Date, "yyyy-MM-dd", System.Globalization.CultureInfo.InvariantCulture, System.Globalization.DateTimeStyles.None, out _)) return null;
                if (raw.DurationMs is not long duration || duration <= 0 || duration > MaxSpanMs) return null;
                normalized.DurationMs = duration;
                break;
        }

        return normalized;
    }
}
