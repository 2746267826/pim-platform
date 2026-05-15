using System.ComponentModel.DataAnnotations;

namespace Pim.Api.DTOs;

public record RegisterRequest(
    [Required][MaxLength(50)] string Username,
    [Required][MaxLength(255)][EmailAddress] string Email,
    [Required][MinLength(8)][MaxLength(100)] string Password,
    [MaxLength(100)] string? DisplayName
);

public record LoginRequest(
    [Required] string Username,
    [Required] string Password
);

public record RefreshRequest(
    [Required] string RefreshToken
);

public record AuthResponse(
    string AccessToken,
    string RefreshToken,
    DateTimeOffset ExpiresAt,
    UserInfo User
);

public record UserInfo(
    Guid Id,
    string Username,
    string DisplayName,
    string Role
);
