using System.Security.Claims;
using Microsoft.AspNetCore.Http;

namespace Pim.Infrastructure.Auth;

public interface ICurrentUserService
{
    Guid? UserId { get; }
    string? Role { get; }
}

public class CurrentUserService : ICurrentUserService
{
    public CurrentUserService(IHttpContextAccessor httpContextAccessor)
    {
        var user = httpContextAccessor.HttpContext?.User;
        var userIdClaim = user?.FindFirst(ClaimTypes.NameIdentifier)?.Value;
        UserId = Guid.TryParse(userIdClaim, out var id) ? id : null;
        Role = user?.FindFirst(ClaimTypes.Role)?.Value;
    }

    public Guid? UserId { get; }
    public string? Role { get; }
}
