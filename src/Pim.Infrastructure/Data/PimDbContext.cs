using Microsoft.EntityFrameworkCore;
using Pim.Core.Data;
using Pim.Infrastructure.Data.Entities;

namespace Pim.Infrastructure.Data;

public class PimDbContext : DbContext
{
    public PimDbContext(DbContextOptions<PimDbContext> options) : base(options) { }

    public DbSet<UserEntity> Users => Set<UserEntity>();
    public DbSet<RefreshTokenEntity> RefreshTokens => Set<RefreshTokenEntity>();
    public DbSet<LoginAttemptEntity> LoginAttempts => Set<LoginAttemptEntity>();

    protected override void OnModelCreating(ModelBuilder modelBuilder)
    {
        modelBuilder.Entity<UserEntity>(e =>
        {
            e.HasIndex(u => u.Username).IsUnique();
            e.HasIndex(u => u.Email).IsUnique();
            e.HasQueryFilter(u => u.DeletedAt == null);
        });

        modelBuilder.Entity<RefreshTokenEntity>(e =>
        {
            e.HasIndex(r => r.TokenHash);
            e.HasOne(r => r.User).WithMany().HasForeignKey(r => r.UserId);
        });

        modelBuilder.Entity<LoginAttemptEntity>(e =>
        {
            e.HasIndex(l => new { l.IpAddress, l.AttemptedAt });
        });
    }
}
