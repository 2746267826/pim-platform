using Pim.Client.Core.Models;
using Pim.Client.Core.Services;

namespace Pim.Client.App;

public sealed class NotificationActionRouter
{
    private readonly Pim.Client.Core.Services.NotificationActionRouter _coreRouter;
    private readonly ApiClient _apiClient;

    public NotificationActionRouter(
        Pim.Client.Core.Services.NotificationActionRouter coreRouter,
        ApiClient apiClient)
    {
        _coreRouter = coreRouter;
        _apiClient = apiClient;
    }

    public NotificationActionRoute Route(
        string action,
        string riskLevel,
        string? confirmationId = null,
        string? relatedObjectType = null,
        string? relatedObjectId = null)
        => _coreRouter.Route(action, riskLevel, confirmationId, relatedObjectType, relatedObjectId);

    public async Task<NotificationActionRoute> RouteToastActionAsync(
        string deviceId,
        EndpointNotificationActionRequestDto request,
        CancellationToken ct = default)
    {
        var route = Route(
            request.Action,
            request.RiskLevel,
            request.ConfirmationId,
            request.RelatedObjectType,
            request.RelatedObjectId);

        if (route.Kind != "Executed")
        {
            return route;
        }

        var response = await _apiClient.SendEndpointNotificationActionAsync(deviceId, request, ct);
        return response?.Data is { } data
            ? new NotificationActionRoute(data.Result, data.DetailUrl, data.Message ?? route.Message)
            : route;
    }
}
