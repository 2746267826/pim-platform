using System.Text.Json.Serialization;

namespace Pim.Client.Core.Models;

public sealed class EndpointStatusDto
{
    [JsonPropertyName("deviceId")]
    public string DeviceId { get; set; } = string.Empty;

    [JsonPropertyName("platform")]
    public string Platform { get; set; } = "windows";

    [JsonPropertyName("uploadStatus")]
    public string UploadStatus { get; set; } = "Unknown";

    [JsonPropertyName("collectionCacheCount")]
    public int CollectionCacheCount { get; set; }

    [JsonPropertyName("onlineOnlyBlockedCount")]
    public int OnlineOnlyBlockedCount { get; set; }

    [JsonPropertyName("lastHeartbeatAt")]
    public DateTimeOffset? LastHeartbeatAt { get; set; }
}

public sealed class EndpointCollectionQualityDto
{
    [JsonPropertyName("deviceId")]
    public string DeviceId { get; set; } = string.Empty;

    [JsonPropertyName("platform")]
    public string Platform { get; set; } = "windows";

    [JsonPropertyName("uploadStatus")]
    public string UploadStatus { get; set; } = "Unknown";

    [JsonPropertyName("issueCount")]
    public int IssueCount { get; set; }

    [JsonPropertyName("checkedAt")]
    public DateTimeOffset CheckedAt { get; set; }
}

public sealed class EndpointNotificationActionRequestDto
{
    [JsonPropertyName("action")]
    public string Action { get; set; } = string.Empty;

    [JsonPropertyName("riskLevel")]
    public string RiskLevel { get; set; } = string.Empty;

    [JsonPropertyName("confirmationId")]
    public string? ConfirmationId { get; set; }

    [JsonPropertyName("relatedObjectType")]
    public string? RelatedObjectType { get; set; }

    [JsonPropertyName("relatedObjectId")]
    public string? RelatedObjectId { get; set; }
}

public sealed class EndpointNotificationActionResponseDto
{
    [JsonPropertyName("result")]
    public string Result { get; set; } = string.Empty;

    [JsonPropertyName("detailUrl")]
    public string? DetailUrl { get; set; }

    [JsonPropertyName("message")]
    public string? Message { get; set; }
}
