using System.ComponentModel.DataAnnotations;
using System.ComponentModel.DataAnnotations.Schema;

namespace Pim.Module.Calendar.Entities;

[Table("outlook_connections")]
public class OutlookConnectionEntity
{
    [Key][Column("id")] public Guid Id { get; set; } = Guid.NewGuid();
    [Column("user_id")] public Guid UserId { get; set; }
    [Column("access_token_encrypted")] public byte[] AccessTokenEncrypted { get; set; } = Array.Empty<byte>();
    [Column("refresh_token_encrypted")] public byte[]? RefreshTokenEncrypted { get; set; }
    [Column("subscription_id")][MaxLength(255)] public string? SubscriptionId { get; set; }
    [Column("subscription_expires_at")] public DateTimeOffset? SubscriptionExpiresAt { get; set; }
    [Column("last_synced_at")] public DateTimeOffset? LastSyncedAt { get; set; }
    [Column("created_at")] public DateTimeOffset CreatedAt { get; set; } = DateTimeOffset.UtcNow;
    [Column("updated_at")] public DateTimeOffset UpdatedAt { get; set; } = DateTimeOffset.UtcNow;
}
