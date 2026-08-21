namespace Pim.Shell.App;

public enum HealthCheckStatus { Healthy, Unreachable }
public sealed record HealthCheckResult(HealthCheckStatus Status, string NormalizedUrl);

public sealed class ServerHealthClient
{
    private readonly HttpClient _http;
    public ServerHealthClient(HttpClient http) => _http = http;

    public async Task<HealthCheckResult> CheckAsync(string serverUrl, CancellationToken ct = default)
    {
        var normalized = ServerAddress.Normalize(serverUrl);
        if (normalized is null) return new HealthCheckResult(HealthCheckStatus.Unreachable, "");
        try
        {
            using var response = await _http.GetAsync(new Uri(new Uri(normalized), "/health"), ct);
            var status = response.IsSuccessStatusCode ? HealthCheckStatus.Healthy : HealthCheckStatus.Unreachable;
            return new HealthCheckResult(status, normalized);
        }
        catch (Exception)
        {
            return new HealthCheckResult(HealthCheckStatus.Unreachable, normalized);
        }
    }
}
