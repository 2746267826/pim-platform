using System.ComponentModel.DataAnnotations;
using System.ComponentModel.DataAnnotations.Schema;

namespace Pim.Infrastructure.Data.Entities;

[Table("login_attempts")]
public class LoginAttemptEntity
{
    [Key][Column("id")] public Guid Id { get; set; } = Guid.NewGuid();
    [Column("user_id")] public Guid? UserId { get; set; }
    [Column("ip_address")][MaxLength(45)] public string IpAddress { get; set; } = string.Empty;
    [Column("success")] public bool Success { get; set; }
    [Column("attempted_at")] public DateTimeOffset AttemptedAt { get; set; } = DateTimeOffset.UtcNow;

    [ForeignKey(nameof(UserId))]
    public UserEntity? User { get; set; }
}
