using Pim.Client.Core.Models;

namespace Pim.Client.Core.Services;

public sealed class PlannedOfflineReporter
{
    private readonly ApiClient _api;

    public PlannedOfflineReporter(ApiClient api)
    {
        _api = api;
    }

    public async Task ReportAsync(PlannedOfflineRequest request, CancellationToken ct = default)
    {
        await _api.PostAsync<object>("daemon/planned-offline", request, ct);
    }

    public static PlannedOfflineRequest BuildRequest(string deviceId, string reason, DateTimeOffset occurredAt)
    {
        return new PlannedOfflineRequest(deviceId, "windows", reason, occurredAt);
    }
}