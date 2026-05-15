using System.IdentityModel.Tokens.Jwt;
using System.Security.Claims;
using System.Security.Cryptography;
using Microsoft.Extensions.Configuration;
using Microsoft.IdentityModel.Tokens;

namespace Pim.Infrastructure.Auth;

public class JwtService
{
    private readonly RSA _rsa;

    public JwtService(IConfiguration configuration)
    {
        _rsa = RSA.Create();
        var keyPath = configuration["Jwt:PrivateKeyPath"];
        if (!string.IsNullOrEmpty(keyPath) && File.Exists(keyPath))
        {
            _rsa.ImportFromPem(File.ReadAllText(keyPath));
        }
        // Development: generate ephemeral key
    }

    public string GenerateAccessToken(Guid userId, string username, string role)
    {
        var credentials = new SigningCredentials(
            new RsaSecurityKey(_rsa),
            SecurityAlgorithms.RsaSha256
        );

        var claims = new[]
        {
            new Claim(ClaimTypes.NameIdentifier, userId.ToString()),
            new Claim(ClaimTypes.Name, username),
            new Claim(ClaimTypes.Role, role),
            new Claim(JwtRegisteredClaimNames.Jti, Guid.NewGuid().ToString())
        };

        var token = new JwtSecurityToken(
            issuer: "pim",
            audience: "pim-client",
            claims: claims,
            expires: DateTimeOffset.UtcNow.AddMinutes(15).UtcDateTime,
            signingCredentials: credentials
        );

        return new JwtSecurityTokenHandler().WriteToken(token);
    }

    public string GenerateRefreshToken()
    {
        var bytes = new byte[64];
        using var rng = RandomNumberGenerator.Create();
        rng.GetBytes(bytes);
        return Convert.ToBase64String(bytes);
    }

    public TokenValidationParameters GetValidationParameters()
    {
        return new TokenValidationParameters
        {
            ValidateIssuer = true,
            ValidIssuer = "pim",
            ValidateAudience = true,
            ValidAudience = "pim-client",
            ValidateLifetime = true,
            ValidateIssuerSigningKey = true,
            IssuerSigningKey = new RsaSecurityKey(_rsa),
            ClockSkew = TimeSpan.FromSeconds(30)
        };
    }
}
