using Microsoft.AspNetCore.Authentication.JwtBearer;
using Microsoft.Extensions.DependencyInjection;
using Pim.Infrastructure.Auth;

namespace Pim.Infrastructure.Extensions;

public static class AuthExtensions
{
    public static IServiceCollection AddPimAuth(
        this IServiceCollection services)
    {
        var sp = services.BuildServiceProvider();
        var jwtService = sp.GetRequiredService<JwtService>();

        services.AddAuthentication(JwtBearerDefaults.AuthenticationScheme)
            .AddJwtBearer(options =>
            {
                options.TokenValidationParameters =
                    jwtService.GetValidationParameters();
            });

        services.AddAuthorization();

        return services;
    }
}
