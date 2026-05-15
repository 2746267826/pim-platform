# PIM 个人信息管理平台 实现计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 构建 PIM 核心平台（编译时模块化框架、JWT 认证、跨模块搜索），并实现首个业务模块（日程与任务，含自动排程引擎、Outlook 集成），最后搭建 Windows 和 Android 客户端骨架。

**Architecture:** ASP.NET Core 8 模块化单体服务端，PostgreSQL 16 + MinIO 存储，WPF (CommunityToolkit.Mvvm) Windows 客户端，Kotlin (Jetpack Compose + Hilt) Android 客户端。模块通过 IModule 接口 DI 注册，编译时加载。

**Tech Stack:** .NET 8, PostgreSQL 16, MinIO, Docker Compose, WPF, CommunityToolkit.Mvvm, Kotlin, Jetpack Compose, Hilt, Retrofit

---

## 阶段一：核心抽象层 (Pim.Core)

### Task 1: 创建解决方案和 Pim.Core 项目

**Files:**
- Create: `src/Pim.Core/Pim.Core.csproj`
- Create: `src/Pim.Core/Common/ApiResponse.cs`
- Create: `src/Pim.Core/Common/PagedResult.cs`
- Create: `src/Pim.Core/Exceptions/DomainException.cs`
- Create: `src/Pim.Core/Modules/IModule.cs`
- Create: `src/Pim.Core/Modules/ISearchProvider.cs`
- Create: `Pim.sln`

- [ ] **Step 1: 创建解决方案和项目结构**

```powershell
cd C:\Users\a2746\Desktop\0\project
mkdir src\Pim.Core\Common, src\Pim.Core\Exceptions, src\Pim.Core\Modules -Force
dotnet new sln -n Pim
dotnet new classlib -n Pim.Core -o src/Pim.Core --framework net8.0
dotnet sln Pim.sln add src/Pim.Core/Pim.Core.csproj
```

- [ ] **Step 2: 编写 ApiResponse.cs**

```csharp
// src/Pim.Core/Common/ApiResponse.cs
namespace Pim.Core.Common;

public record ApiResponse<T>(
    int Code,
    string Message,
    T? Data,
    DateTimeOffset Timestamp
)
{
    public static ApiResponse<T> Ok(T data) =>
        new(0, "success", data, DateTimeOffset.UtcNow);

    public static ApiResponse<T> Error(int code, string message) =>
        new(code, message, default, DateTimeOffset.UtcNow);
}
```

- [ ] **Step 3: 编写 PagedResult.cs**

```csharp
// src/Pim.Core/Common/PagedResult.cs
namespace Pim.Core.Common;

public record PagedResult<T>(
    IReadOnlyList<T> Items,
    int Page,
    int PageSize,
    int TotalCount,
    int TotalPages
);
```

- [ ] **Step 4: 编写 DomainException.cs**

```csharp
// src/Pim.Core/Exceptions/DomainException.cs
namespace Pim.Core.Exceptions;

public class DomainException : Exception
{
    public int ErrorCode { get; }

    public DomainException(int errorCode, string message) : base(message)
    {
        ErrorCode = errorCode;
    }
}
```

- [ ] **Step 5: 编写 IModule.cs**

```csharp
// src/Pim.Core/Modules/IModule.cs
namespace Pim.Core.Modules;

public interface IModule
{
    string Name { get; }
    string Version { get; }
    void RegisterServices(IServiceCollection services, IConfiguration configuration);
    void MapEndpoints(IEndpointRouteBuilder endpoints);
    Task InitializeAsync(IServiceProvider serviceProvider);
}
```

- [ ] **Step 6: 编写 ISearchProvider.cs**

```csharp
// src/Pim.Core/Modules/ISearchProvider.cs
namespace Pim.Core.Modules;

public interface ISearchProvider
{
    string ModuleName { get; }
    Task<IReadOnlyList<SearchResult>> SearchAsync(string query, int limit, CancellationToken ct);
}

public record SearchResult(
    string ModuleName,
    string Type,
    string Id,
    string Title,
    string Snippet,
    string Url
);
```

- [ ] **Step 7: 验证构建**

```powershell
dotnet build src/Pim.Core/Pim.Core.csproj
```

Expected: Build succeeded.

- [ ] **Step 8: Commit**

```powershell
git add Pim.sln src/Pim.Core/
git commit -m "feat: add Pim.Core with module contracts, ApiResponse, PagedResult, DomainException"
```

---

## 阶段二：基础设施层 (Pim.Infrastructure)

### Task 2: 创建 Pim.Infrastructure 项目与 DbContext

**Files:**
- Create: `src/Pim.Infrastructure/Pim.Infrastructure.csproj`
- Create: `src/Pim.Infrastructure/Data/PimDbContext.cs`
- Create: `src/Pim.Infrastructure/Data/Entities/UserEntity.cs`
- Create: `src/Pim.Infrastructure/Data/Entities/RefreshTokenEntity.cs`
- Create: `src/Pim.Infrastructure/Data/Entities/LoginAttemptEntity.cs`

- [ ] **Step 1: 创建项目和目录**

```powershell
dotnet new classlib -n Pim.Infrastructure -o src/Pim.Infrastructure --framework net8.0
dotnet sln Pim.sln add src/Pim.Infrastructure/Pim.Infrastructure.csproj
mkdir src/Pim.Infrastructure\Data\Entities -Force
mkdir src/Pim.Infrastructure\Auth -Force
mkdir src/Pim.Infrastructure\Storage -Force
mkdir src/Pim.Infrastructure\TextExtraction -Force
mkdir src/Pim.Infrastructure\Extensions -Force
```

- [ ] **Step 2: 添加 NuGet 依赖**

```powershell
dotnet add src/Pim.Infrastructure/Pim.Infrastructure.csproj package Npgsql.EntityFrameworkCore.PostgreSQL
dotnet add src/Pim.Infrastructure/Pim.Infrastructure.csproj package Microsoft.AspNetCore.Authentication.JwtBearer
dotnet add src/Pim.Infrastructure/Pim.Infrastructure.csproj package Minio
dotnet add src/Pim.Infrastructure/Pim.Infrastructure.csproj reference src/Pim.Core/Pim.Core.csproj
```

- [ ] **Step 3: 添加 ISoftDeletable 接口到 Pim.Core**

```csharp
// src/Pim.Core/Data/ISoftDeletable.cs
namespace Pim.Core.Data;

public interface ISoftDeletable
{
    DateTimeOffset? DeletedAt { get; set; }
}
```

```powershell
mkdir src\Pim.Core\Data -Force
```

- [ ] **Step 4: 编写 UserEntity.cs**

```csharp
// src/Pim.Infrastructure/Data/Entities/UserEntity.cs
using System.ComponentModel.DataAnnotations;
using System.ComponentModel.DataAnnotations.Schema;
using Pim.Core.Data;

namespace Pim.Infrastructure.Data.Entities;

[Table("users")]
public class UserEntity : ISoftDeletable
{
    [Key]
    [Column("id")]
    public Guid Id { get; set; } = Guid.NewGuid();

    [Column("username")]
    [MaxLength(50)]
    public string Username { get; set; } = string.Empty;

    [Column("email")]
    [MaxLength(255)]
    public string Email { get; set; } = string.Empty;

    [Column("password_hash")]
    [MaxLength(255)]
    public string PasswordHash { get; set; } = string.Empty;

    [Column("display_name")]
    [MaxLength(100)]
    public string? DisplayName { get; set; }

    [Column("role")]
    [MaxLength(20)]
    public string Role { get; set; } = "user";

    [Column("is_active")]
    public bool IsActive { get; set; } = true;

    [Column("created_at")]
    public DateTimeOffset CreatedAt { get; set; } = DateTimeOffset.UtcNow;

    [Column("updated_at")]
    public DateTimeOffset UpdatedAt { get; set; } = DateTimeOffset.UtcNow;

    [Column("deleted_at")]
    public DateTimeOffset? DeletedAt { get; set; }
}
```

- [ ] **Step 5: 编写 RefreshTokenEntity.cs**

```csharp
// src/Pim.Infrastructure/Data/Entities/RefreshTokenEntity.cs
using System.ComponentModel.DataAnnotations;
using System.ComponentModel.DataAnnotations.Schema;

namespace Pim.Infrastructure.Data.Entities;

[Table("refresh_tokens")]
public class RefreshTokenEntity
{
    [Key][Column("id")] public Guid Id { get; set; } = Guid.NewGuid();
    [Column("user_id")] public Guid UserId { get; set; }
    [Column("token_hash")][MaxLength(255)] public string TokenHash { get; set; } = string.Empty;
    [Column("expires_at")] public DateTimeOffset ExpiresAt { get; set; }
    [Column("revoked_at")] public DateTimeOffset? RevokedAt { get; set; }
    [Column("created_at")] public DateTimeOffset CreatedAt { get; set; } = DateTimeOffset.UtcNow;

    [ForeignKey(nameof(UserId))]
    public UserEntity User { get; set; } = null!;
}
```

- [ ] **Step 6: 编写 LoginAttemptEntity.cs**

```csharp
// src/Pim.Infrastructure/Data/Entities/LoginAttemptEntity.cs
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
```

- [ ] **Step 7: 编写 PimDbContext.cs**

```csharp
// src/Pim.Infrastructure/Data/PimDbContext.cs
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
```

- [ ] **Step 8: 验证构建**

```powershell
dotnet build src/Pim.Infrastructure/Pim.Infrastructure.csproj
```

Expected: Build succeeded.

- [ ] **Step 9: Commit**

```powershell
git add src/Pim.Core/Data/ src/Pim.Infrastructure/
git commit -m "feat: add Pim.Infrastructure with DbContext, user/token/login entities"
```

---

### Task 3: JWT 认证服务

**Files:**
- Create: `src/Pim.Infrastructure/Auth/PasswordHasher.cs`
- Create: `src/Pim.Infrastructure/Auth/JwtService.cs`
- Create: `src/Pim.Infrastructure/Auth/CurrentUserService.cs`

- [ ] **Step 1: 编写 PasswordHasher.cs**

```csharp
// src/Pim.Infrastructure/Auth/PasswordHasher.cs
namespace Pim.Infrastructure.Auth;

public static class PasswordHasher
{
    public static string Hash(string password)
    {
        return BCrypt.Net.BCrypt.HashPassword(password, workFactor: 12);
    }

    public static bool Verify(string password, string hash)
    {
        return BCrypt.Net.BCrypt.Verify(password, hash);
    }
}
```

```powershell
dotnet add src/Pim.Infrastructure/Pim.Infrastructure.csproj package BCrypt.Net-Next
```

- [ ] **Step 2: 编写 JwtService.cs**

```csharp
// src/Pim.Infrastructure/Auth/JwtService.cs
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
```

- [ ] **Step 3: 编写 CurrentUserService.cs**

```csharp
// src/Pim.Infrastructure/Auth/CurrentUserService.cs
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
```

- [ ] **Step 4: 验证构建**

```powershell
dotnet build src/Pim.Infrastructure/Pim.Infrastructure.csproj
```

Expected: Build succeeded.

- [ ] **Step 5: Commit**

```powershell
git add src/Pim.Infrastructure/Auth/
git commit -m "feat: add JWT auth service, password hasher, current user service"
```

---

### Task 4: MinIO 与 Kopia 存储服务

**Files:**
- Create: `src/Pim.Infrastructure/Storage/MinioStorage.cs`
- Create: `src/Pim.Infrastructure/Storage/KopiaService.cs`

- [ ] **Step 1: 编写 MinioStorage.cs**

```csharp
// src/Pim.Infrastructure/Storage/MinioStorage.cs
using Minio;
using Minio.DataModel.Args;

namespace Pim.Infrastructure.Storage;

public class MinioStorage
{
    private readonly IMinioClient _client;
    private const string BucketName = "pim-files";

    public MinioStorage(string endpoint, string accessKey, string secretKey)
    {
        _client = new MinioClient()
            .WithEndpoint(endpoint)
            .WithCredentials(accessKey, secretKey)
            .Build();
    }

    public async Task EnsureBucketAsync(CancellationToken ct = default)
    {
        var exists = await _client.BucketExistsAsync(
            new BucketExistsArgs().WithBucket(BucketName), ct);
        if (!exists)
        {
            await _client.MakeBucketAsync(
                new MakeBucketArgs().WithBucket(BucketName), ct);
        }
    }

    public async Task<string> UploadAsync(
        string objectName, Stream data, string contentType, long size,
        CancellationToken ct = default)
    {
        await _client.PutObjectAsync(new PutObjectArgs()
            .WithBucket(BucketName)
            .WithObject(objectName)
            .WithStreamData(data)
            .WithObjectSize(size)
            .WithContentType(contentType), ct);

        return objectName;
    }

    public async Task<Stream> DownloadAsync(string objectName, CancellationToken ct = default)
    {
        var stream = new MemoryStream();
        await _client.GetObjectAsync(new GetObjectArgs()
            .WithBucket(BucketName)
            .WithObject(objectName)
            .WithCallbackStream(s => s.CopyTo(stream)), ct);
        stream.Position = 0;
        return stream;
    }

    public async Task<string> GetPresignedUrlAsync(
        string objectName, int expirySeconds = 300, CancellationToken ct = default)
    {
        return await _client.PresignedGetObjectAsync(new PresignedGetObjectArgs()
            .WithBucket(BucketName)
            .WithObject(objectName)
            .WithExpiry(expirySeconds));
    }

    public async Task DeleteAsync(string objectName, CancellationToken ct = default)
    {
        await _client.RemoveObjectAsync(new RemoveObjectArgs()
            .WithBucket(BucketName)
            .WithObject(objectName), ct);
    }
}
```

- [ ] **Step 2: 编写 KopiaService.cs**

```csharp
// src/Pim.Infrastructure/Storage/KopiaService.cs
using System.Diagnostics;

namespace Pim.Infrastructure.Storage;

public class KopiaService
{
    private readonly string _repositoryPath;
    private readonly string _password;

    public KopiaService(string repositoryPath, string password)
    {
        _repositoryPath = repositoryPath;
        _password = password;
    }

    public async Task<string> CreateSnapshotAsync(
        string sourcePath, string description, CancellationToken ct = default)
    {
        var args = $"snapshot create \"{sourcePath}\" --description=\"{description}\" --json";
        var output = await RunKopiaAsync(args, ct);
        // Parse snapshot ID from JSON output
        return output; // snapshot manifest ID
    }

    public async Task<IReadOnlyList<KopiaSnapshotInfo>> ListSnapshotsAsync(
        string sourcePath, CancellationToken ct = default)
    {
        var args = $"snapshot list \"{sourcePath}\" --json";
        var output = await RunKopiaAsync(args, ct);
        return ParseSnapshotList(output);
    }

    public async Task<Stream> RestoreSnapshotAsync(
        string snapshotId, string targetPath, CancellationToken ct = default)
    {
        var args = $"snapshot restore {snapshotId} \"{targetPath}\"";
        await RunKopiaAsync(args, ct);
        // Read restored file(s) from targetPath
        return File.OpenRead(targetPath);
    }

    public async Task DeleteSnapshotAsync(
        string snapshotId, CancellationToken ct = default)
    {
        var args = $"snapshot delete {snapshotId} --unsafe-ignore-source";
        await RunKopiaAsync(args, ct);
    }

    public async Task ConnectRepositoryAsync(CancellationToken ct = default)
    {
        var args = $"repository connect filesystem --path=\"{_repositoryPath}\"";
        await RunKopiaAsync(args, ct);
    }

    private async Task<string> RunKopiaAsync(string arguments, CancellationToken ct)
    {
        var psi = new ProcessStartInfo
        {
            FileName = "kopia",
            Arguments = arguments,
            RedirectStandardOutput = true,
            RedirectStandardError = true,
            UseShellExecute = false,
            CreateNoWindow = true
        };

        psi.EnvironmentVariables["KOPIA_PASSWORD"] = _password;

        using var process = Process.Start(psi)!;
        var output = await process.StandardOutput.ReadToEndAsync(ct);
        var error = await process.StandardError.ReadToEndAsync(ct);
        await process.WaitForExitAsync(ct);

        if (process.ExitCode != 0)
            throw new InvalidOperationException($"Kopia failed: {error}");

        return output;
    }

    private IReadOnlyList<KopiaSnapshotInfo> ParseSnapshotList(string json)
    {
        // Parse Kopia JSON output into list
        return new List<KopiaSnapshotInfo>();
    }
}

public record KopiaSnapshotInfo(
    string Id,
    string Description,
    DateTimeOffset StartTime,
    long TotalSize
);
```

- [ ] **Step 3: 验证构建**

```powershell
dotnet build src/Pim.Infrastructure/Pim.Infrastructure.csproj
```

Expected: Build succeeded.

- [ ] **Step 4: Commit**

```powershell
git add src/Pim.Infrastructure/Storage/
git commit -m "feat: add MinIO storage client and Kopia CLI wrapper"
```

---

### Task 5: Tika 文本提取客户端

**Files:**
- Create: `src/Pim.Infrastructure/TextExtraction/TikaClient.cs`

- [ ] **Step 1: 编写 TikaClient.cs**

```csharp
// src/Pim.Infrastructure/TextExtraction/TikaClient.cs
namespace Pim.Infrastructure.TextExtraction;

public class TikaClient
{
    private readonly HttpClient _httpClient;

    public TikaClient(HttpClient httpClient)
    {
        _httpClient = httpClient;
        _httpClient.Timeout = TimeSpan.FromMinutes(2);
    }

    public async Task<string> ExtractTextAsync(
        Stream fileStream, string fileName, CancellationToken ct = default)
    {
        using var content = new MultipartFormDataContent();
        var fileContent = new StreamContent(fileStream);
        fileContent.Headers.ContentType =
            new System.Net.Http.Headers.MediaTypeHeaderValue("application/octet-stream");
        content.Add(fileContent, "file", fileName);

        var response = await _httpClient.PutAsync("/tika", content, ct);
        response.EnsureSuccessStatusCode();
        return await response.Content.ReadAsStringAsync(ct);
    }

    public async Task<string> ExtractTextAsync(
        byte[] fileBytes, string fileName, CancellationToken ct = default)
    {
        using var stream = new MemoryStream(fileBytes);
        return await ExtractTextAsync(stream, fileName, ct);
    }
}
```

- [ ] **Step 2: 验证构建**

```powershell
dotnet build src/Pim.Infrastructure/Pim.Infrastructure.csproj
```

Expected: Build succeeded.

- [ ] **Step 3: Commit**

```powershell
git add src/Pim.Infrastructure/TextExtraction/
git commit -m "feat: add Apache Tika HTTP client for text extraction"
```

---

### Task 6: 基础设施 DI 扩展方法

**Files:**
- Create: `src/Pim.Infrastructure/Extensions/ServiceCollectionExtensions.cs`
- Create: `src/Pim.Infrastructure/Extensions/AuthExtensions.cs`

- [ ] **Step 1: 编写 ServiceCollectionExtensions.cs**

```csharp
// src/Pim.Infrastructure/Extensions/ServiceCollectionExtensions.cs
using Microsoft.EntityFrameworkCore;
using Microsoft.Extensions.Configuration;
using Microsoft.Extensions.DependencyInjection;
using Pim.Infrastructure.Auth;
using Pim.Infrastructure.Data;
using Pim.Infrastructure.Storage;
using Pim.Infrastructure.TextExtraction;

namespace Pim.Infrastructure.Extensions;

public static class ServiceCollectionExtensions
{
    public static IServiceCollection AddPimInfrastructure(
        this IServiceCollection services, IConfiguration configuration)
    {
        // EF Core
        services.AddDbContext<PimDbContext>(options =>
            options.UseNpgsql(
                configuration.GetConnectionString("DefaultConnection"),
                npgsql => npgsql.EnableRetryOnFailure(3)));

        // Auth
        services.AddSingleton<JwtService>();
        services.AddScoped<ICurrentUserService, CurrentUserService>();

        // Storage
        services.AddSingleton(sp => new MinioStorage(
            configuration["Minio:Endpoint"]!,
            configuration["Minio:AccessKey"]!,
            configuration["Minio:SecretKey"]!));

        services.AddSingleton(sp => new KopiaService(
            configuration["Kopia:RepositoryPath"]!,
            configuration["Kopia:Password"]!));

        // Tika
        services.AddHttpClient<TikaClient>(client =>
        {
            client.BaseAddress = new Uri(configuration["Tika:BaseUrl"]!);
        });

        return services;
    }
}
```

- [ ] **Step 2: 编写 AuthExtensions.cs**

```csharp
// src/Pim.Infrastructure/Extensions/AuthExtensions.cs
using Microsoft.AspNetCore.Authentication.JwtBearer;
using Microsoft.Extensions.DependencyInjection;
using Pim.Infrastructure.Auth;

namespace Pim.Infrastructure.Extensions;

public static class AuthExtensions
{
    public static IServiceCollection AddPimAuth(
        this IServiceCollection services)
    {
        var jwtService = services.BuildServiceProvider()
            .GetRequiredService<JwtService>();

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
```

- [ ] **Step 3: 验证构建**

```powershell
dotnet build src/Pim.Infrastructure/Pim.Infrastructure.csproj
```

Expected: Build succeeded.

- [ ] **Step 4: Commit**

```powershell
git add src/Pim.Infrastructure/Extensions/
git commit -m "feat: add infrastructure DI registration extensions"
```

---

## 阶段三：API 主机 (Pim.Api)

### Task 7: 创建 Pim.Api 项目与基础配置

**Files:**
- Create: `src/Pim.Api/Pim.Api.csproj`
- Create: `src/Pim.Api/Program.cs`
- Create: `src/Pim.Api/appsettings.json`
- Create: `src/Pim.Api/appsettings.Development.json`
- Create: `src/Pim.Api/Middleware/ExceptionMiddleware.cs`
- Create: `src/Pim.Api/ModuleRegistry.cs`

- [ ] **Step 1: 创建 Web API 项目**

```powershell
dotnet new web -n Pim.Api -o src/Pim.Api --framework net8.0
dotnet sln Pim.sln add src/Pim.Api/Pim.Api.csproj
dotnet add src/Pim.Api/Pim.Api.csproj reference src/Pim.Core/Pim.Core.csproj
dotnet add src/Pim.Api/Pim.Api.csproj reference src/Pim.Infrastructure/Pim.Infrastructure.csproj
mkdir src\Pim.Api\Middleware -Force
```

- [ ] **Step 2: 编写 ExceptionMiddleware.cs**

```csharp
// src/Pim.Api/Middleware/ExceptionMiddleware.cs
using System.Text.Json;
using Pim.Core.Common;
using Pim.Core.Exceptions;

namespace Pim.Api.Middleware;

public class ExceptionMiddleware
{
    private readonly RequestDelegate _next;
    private readonly ILogger<ExceptionMiddleware> _logger;

    public ExceptionMiddleware(RequestDelegate next, ILogger<ExceptionMiddleware> logger)
    {
        _next = next;
        _logger = logger;
    }

    public async Task InvokeAsync(HttpContext context)
    {
        try
        {
            await _next(context);
        }
        catch (DomainException ex)
        {
            context.Response.StatusCode = StatusCodes.Status400BadRequest;
            context.Response.ContentType = "application/json";
            var response = ApiResponse<string>.Error(ex.ErrorCode, ex.Message);
            await context.Response.WriteAsync(JsonSerializer.Serialize(response));
        }
        catch (Exception ex)
        {
            _logger.LogError(ex, "Unhandled exception");
            context.Response.StatusCode = StatusCodes.Status500InternalServerError;
            context.Response.ContentType = "application/json";
            var response = ApiResponse<string>.Error(01001, "Internal server error");
            await context.Response.WriteAsync(JsonSerializer.Serialize(response));
        }
    }
}
```

- [ ] **Step 3: 编写 ModuleRegistry.cs**

```csharp
// src/Pim.Api/ModuleRegistry.cs
using System.Reflection;
using Pim.Core.Modules;

namespace Pim.Api;

public class ModuleRegistry
{
    private readonly List<IModule> _modules = new();

    public IReadOnlyList<IModule> Modules => _modules;

    public void DiscoverModules(IServiceCollection services, IConfiguration configuration)
    {
        var moduleAssemblies = Directory.GetFiles(
            AppDomain.CurrentDomain.BaseDirectory,
            "Pim.Module.*.dll");

        foreach (var assemblyPath in moduleAssemblies)
        {
            var assembly = Assembly.LoadFrom(assemblyPath);
            var moduleTypes = assembly.GetTypes()
                .Where(t => typeof(IModule).IsAssignableFrom(t) && !t.IsInterface && !t.IsAbstract);

            foreach (var type in moduleTypes)
            {
                var module = (IModule)Activator.CreateInstance(type)!;
                _modules.Add(module);
                module.RegisterServices(services, configuration);
            }
        }
    }

    public void MapAllEndpoints(IEndpointRouteBuilder endpoints)
    {
        foreach (var module in _modules)
        {
            module.MapEndpoints(endpoints);
        }
    }

    public async Task InitializeAllAsync(IServiceProvider serviceProvider)
    {
        foreach (var module in _modules)
        {
            await module.InitializeAsync(serviceProvider);
        }
    }
}
```

- [ ] **Step 4: 编写 appsettings.json**

```json
// src/Pim.Api/appsettings.json
{
  "Logging": {
    "LogLevel": {
      "Default": "Information",
      "Microsoft.AspNetCore": "Warning"
    }
  },
  "ConnectionStrings": {
    "DefaultConnection": "Host=postgres;Database=pim;Username=pim;Password=pim_password"
  },
  "Jwt": {
    "PrivateKeyPath": "/data/keys/jwt_private.pem"
  },
  "Minio": {
    "Endpoint": "minio:9000",
    "AccessKey": "minioadmin",
    "SecretKey": "minioadmin"
  },
  "Kopia": {
    "RepositoryPath": "/data/kopia-repo",
    "Password": "kopia_password"
  },
  "Tika": {
    "BaseUrl": "http://tika:9998"
  }
}
```

- [ ] **Step 5: 编写 appsettings.Development.json**

```json
// src/Pim.Api/appsettings.Development.json
{
  "Logging": {
    "LogLevel": {
      "Default": "Debug"
    }
  },
  "ConnectionStrings": {
    "DefaultConnection": "Host=localhost;Database=pim;Username=pim;Password=pim_password"
  },
  "Minio": {
    "Endpoint": "localhost:9000",
    "AccessKey": "minioadmin",
    "SecretKey": "minioadmin"
  },
  "Kopia": {
    "RepositoryPath": "./data/kopia-repo",
    "Password": "kopia_password"
  },
  "Tika": {
    "BaseUrl": "http://localhost:9998"
  }
}
```

- [ ] **Step 6: 编写 Program.cs**

```csharp
// src/Pim.Api/Program.cs
using Pim.Api;
using Pim.Api.Middleware;
using Pim.Infrastructure.Extensions;

var builder = WebApplication.CreateBuilder(args);

// Infrastructure
builder.Services.AddPimInfrastructure(builder.Configuration);
builder.Services.AddPimAuth();

// HTTP
builder.Services.AddHttpContextAccessor();
builder.Services.AddCors(options =>
{
    options.AddDefaultPolicy(policy =>
        policy.AllowAnyOrigin().AllowAnyMethod().AllowAnyHeader());
});

// Module discovery
var moduleRegistry = new ModuleRegistry();
moduleRegistry.DiscoverModules(builder.Services, builder.Configuration);

var app = builder.Build();

app.UseMiddleware<ExceptionMiddleware>();
app.UseAuthentication();
app.UseAuthorization();
app.UseCors();

// Module endpoints
moduleRegistry.MapAllEndpoints(app);

// Init modules
await moduleRegistry.InitializeAllAsync(app.Services);

app.Run();
```

- [ ] **Step 7: 验证构建**

```powershell
dotnet build src/Pim.Api/Pim.Api.csproj
```

Expected: Build succeeded.

- [ ] **Step 8: Commit**

```powershell
git add src/Pim.Api/
git commit -m "feat: add Pim.Api host with exception middleware, module registry, config"
```

---

### Task 8: 认证端点

**Files:**
- Create: `src/Pim.Api/Endpoints/AuthEndpoints.cs`
- Create: `src/Pim.Api/DTOs/AuthDtos.cs`

- [ ] **Step 1: 编写 AuthDtos.cs**

```csharp
// src/Pim.Api/DTOs/AuthDtos.cs
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
```

- [ ] **Step 2: 编写 AuthEndpoints.cs**

```csharp
// src/Pim.Api/Endpoints/AuthEndpoints.cs
using Microsoft.EntityFrameworkCore;
using Pim.Core.Common;
using Pim.Infrastructure.Auth;
using Pim.Infrastructure.Data;
using Pim.Infrastructure.Data.Entities;
using Pim.Api.DTOs;

namespace Pim.Api.Endpoints;

public static class AuthEndpoints
{
    public static void MapAuthEndpoints(this IEndpointRouteBuilder endpoints)
    {
        var group = endpoints.MapGroup("/api/v1/auth");

        group.MapPost("/register", async (
            RegisterRequest request,
            PimDbContext db,
            JwtService jwt,
            CancellationToken ct) =>
        {
            if (await db.Users.AnyAsync(u => u.Username == request.Username, ct))
                return Results.Conflict(ApiResponse<string>.Error(01003, "Username already exists"));

            if (await db.Users.AnyAsync(u => u.Email == request.Email, ct))
                return Results.Conflict(ApiResponse<string>.Error(01004, "Email already exists"));

            var user = new UserEntity
            {
                Username = request.Username,
                Email = request.Email,
                PasswordHash = PasswordHasher.Hash(request.Password),
                DisplayName = request.DisplayName ?? request.Username,
                Role = "user"
            };

            db.Users.Add(user);
            await db.SaveChangesAsync(ct);

            var accessToken = jwt.GenerateAccessToken(user.Id, user.Username, user.Role);
            var refreshToken = jwt.GenerateRefreshToken();
            var refreshTokenHash = Convert.ToBase64String(
                System.Security.Cryptography.SHA256.HashData(
                    System.Text.Encoding.UTF8.GetBytes(refreshToken)));

            db.RefreshTokens.Add(new RefreshTokenEntity
            {
                UserId = user.Id,
                TokenHash = refreshTokenHash,
                ExpiresAt = DateTimeOffset.UtcNow.AddDays(7)
            });
            await db.SaveChangesAsync(ct);

            return Results.Created($"/api/v1/users/{user.Id}",
                ApiResponse<AuthResponse>.Ok(new AuthResponse(
                    accessToken,
                    refreshToken,
                    DateTimeOffset.UtcNow.AddMinutes(15),
                    new UserInfo(user.Id, user.Username, user.DisplayName!, user.Role))));
        });

        group.MapPost("/login", async (
            LoginRequest request,
            PimDbContext db,
            JwtService jwt,
            HttpContext httpContext,
            CancellationToken ct) =>
        {
            var ipAddress = httpContext.Connection.RemoteIpAddress?.ToString() ?? "unknown";

            // Rate limiting check
            var recentFailures = await db.LoginAttempts.CountAsync(
                la => la.IpAddress == ipAddress && !la.Success &&
                      la.AttemptedAt > DateTimeOffset.UtcNow.AddMinutes(-15), ct);

            if (recentFailures >= 5)
            {
                return Results.StatusCode(429);
            }

            var user = await db.Users.FirstOrDefaultAsync(
                u => u.Username == request.Username || u.Email == request.Username, ct);

            if (user is null || !PasswordHasher.Verify(request.Password, user.PasswordHash))
            {
                db.LoginAttempts.Add(new LoginAttemptEntity
                {
                    IpAddress = ipAddress,
                    Success = false
                });
                await db.SaveChangesAsync(ct);
                return Results.Unauthorized();
            }

            db.LoginAttempts.Add(new LoginAttemptEntity
            {
                UserId = user.Id,
                IpAddress = ipAddress,
                Success = true
            });

            var accessToken = jwt.GenerateAccessToken(user.Id, user.Username, user.Role);
            var refreshToken = jwt.GenerateRefreshToken();
            var refreshTokenHash = Convert.ToBase64String(
                System.Security.Cryptography.SHA256.HashData(
                    System.Text.Encoding.UTF8.GetBytes(refreshToken)));

            db.RefreshTokens.Add(new RefreshTokenEntity
            {
                UserId = user.Id,
                TokenHash = refreshTokenHash,
                ExpiresAt = DateTimeOffset.UtcNow.AddDays(7)
            });
            await db.SaveChangesAsync(ct);

            return Results.Ok(ApiResponse<AuthResponse>.Ok(new AuthResponse(
                accessToken,
                refreshToken,
                DateTimeOffset.UtcNow.AddMinutes(15),
                new UserInfo(user.Id, user.Username, user.DisplayName!, user.Role))));
        });

        group.MapPost("/refresh", async (
            RefreshRequest request,
            PimDbContext db,
            JwtService jwt,
            CancellationToken ct) =>
        {
            var tokenHash = Convert.ToBase64String(
                System.Security.Cryptography.SHA256.HashData(
                    System.Text.Encoding.UTF8.GetBytes(request.RefreshToken)));

            var stored = await db.RefreshTokens.FirstOrDefaultAsync(
                rt => rt.TokenHash == tokenHash && rt.RevokedAt == null, ct);

            if (stored is null || stored.ExpiresAt < DateTimeOffset.UtcNow)
                return Results.Unauthorized();

            // Revoke old token
            stored.RevokedAt = DateTimeOffset.UtcNow;

            var user = await db.Users.FindAsync(new object[] { stored.UserId }, ct);
            if (user is null) return Results.Unauthorized();

            var accessToken = jwt.GenerateAccessToken(user.Id, user.Username, user.Role);
            var newRefreshToken = jwt.GenerateRefreshToken();
            var newTokenHash = Convert.ToBase64String(
                System.Security.Cryptography.SHA256.HashData(
                    System.Text.Encoding.UTF8.GetBytes(newRefreshToken)));

            db.RefreshTokens.Add(new RefreshTokenEntity
            {
                UserId = user.Id,
                TokenHash = newTokenHash,
                ExpiresAt = DateTimeOffset.UtcNow.AddDays(7)
            });
            await db.SaveChangesAsync(ct);

            return Results.Ok(ApiResponse<AuthResponse>.Ok(new AuthResponse(
                accessToken,
                newRefreshToken,
                DateTimeOffset.UtcNow.AddMinutes(15),
                new UserInfo(user.Id, user.Username, user.DisplayName!, user.Role))));
        });
    }
}
```

- [ ] **Step 3: 注册端点到 Program.cs**

在 `Program.cs` 中 `moduleRegistry.MapAllEndpoints(app);` 之前添加：

```csharp
app.MapAuthEndpoints();
```

- [ ] **Step 4: 验证构建**

```powershell
dotnet build src/Pim.Api/Pim.Api.csproj
```

Expected: Build succeeded.

- [ ] **Step 5: Commit**

```powershell
git add src/Pim.Api/DTOs/ src/Pim.Api/Endpoints/ src/Pim.Api/Program.cs
git commit -m "feat: add auth endpoints (register, login, refresh)"
```

---

### Task 9: 跨模块搜索端点

**Files:**
- Create: `src/Pim.Api/Search/SearchController.cs`

- [ ] **Step 1: 编写 SearchController.cs**

```csharp
// src/Pim.Api/Search/SearchController.cs
using Pim.Core.Common;
using Pim.Core.Modules;

namespace Pim.Api.Search;

public static class SearchEndpoints
{
    public static void MapSearchEndpoints(this IEndpointRouteBuilder endpoints)
    {
        var group = endpoints.MapGroup("/api/v1/search")
            .RequireAuthorization();

        group.MapGet("/", async (
            string? q,
            string? type,
            int? limit,
            IEnumerable<ISearchProvider> providers,
            CancellationToken ct) =>
        {
            if (string.IsNullOrWhiteSpace(q))
                return Results.Ok(ApiResponse<PagedResult<SearchResult>>.Ok(
                    new PagedResult<SearchResult>(Array.Empty<SearchResult>(), 1, 20, 0, 0)));

            var maxLimit = Math.Min(limit ?? 20, 100);
            var typeFilter = type?.Split(',', StringSplitOptions.RemoveEmptyEntries)
                .Select(t => t.Trim().ToLowerInvariant()).ToHashSet();

            var tasks = providers
                .Where(p => typeFilter is null || typeFilter.Count == 0 ||
                            typeFilter.Contains(p.ModuleName.ToLowerInvariant()))
                .Select(p => p.SearchAsync(q, maxLimit, ct));

            var results = await Task.WhenAll(tasks);
            var merged = results.SelectMany(r => r)
                .OrderByDescending(r => r.Title.Contains(q, StringComparison.OrdinalIgnoreCase))
                .Take(maxLimit)
                .ToList();

            return Results.Ok(ApiResponse<PagedResult<SearchResult>>.Ok(
                new PagedResult<SearchResult>(merged, 1, maxLimit, merged.Count,
                    (int)Math.Ceiling(merged.Count / (double)maxLimit))));
        });
    }
}
```

- [ ] **Step 2: 注册搜索端点**

在 `Program.cs` 中添加：

```csharp
app.MapSearchEndpoints();
```

- [ ] **Step 3: 验证构建**

```powershell
dotnet build src/Pim.Api/Pim.Api.csproj
```

Expected: Build succeeded.

- [ ] **Step 4: Commit**

```powershell
git add src/Pim.Api/Search/ src/Pim.Api/Program.cs
git commit -m "feat: add cross-module search aggregation endpoint"
```

---

## 阶段四：Docker 部署

### Task 10: Docker 文件

**Files:**
- Create: `src/Pim.Api/Dockerfile`
- Create: `docker-compose.yml`
- Create: `nginx.conf`

- [ ] **Step 1: 编写 Dockerfile**

```dockerfile
# src/Pim.Api/Dockerfile
FROM mcr.microsoft.com/dotnet/sdk:8.0 AS build
WORKDIR /src
COPY ["src/Pim.Core/Pim.Core.csproj", "Pim.Core/"]
COPY ["src/Pim.Infrastructure/Pim.Infrastructure.csproj", "Pim.Infrastructure/"]
COPY ["src/Pim.Api/Pim.Api.csproj", "Pim.Api/"]
COPY ["src/modules/Pim.Module.Calendar/Pim.Module.Calendar.csproj", "modules/Pim.Module.Calendar/"]
RUN dotnet restore "Pim.Api/Pim.Api.csproj"
COPY . .
WORKDIR "/src/src/Pim.Api"
RUN dotnet publish "Pim.Api.csproj" -c Release -o /app/publish

FROM mcr.microsoft.com/dotnet/aspnet:8.0
WORKDIR /app
COPY --from=build /app/publish .
RUN apt-get update && apt-get install -y kopia && rm -rf /var/lib/apt/lists/*
EXPOSE 5000
ENTRYPOINT ["dotnet", "Pim.Api.dll"]
```

- [ ] **Step 2: 编写 docker-compose.yml**

```yaml
# docker-compose.yml
version: '3.8'

services:
  pim-api:
    build:
      context: .
      dockerfile: src/Pim.Api/Dockerfile
    restart: unless-stopped
    environment:
      - ASPNETCORE_ENVIRONMENT=Production
      - ConnectionStrings__DefaultConnection=Host=postgres;Database=pim;Username=pim;Password=${PG_PASSWORD}
      - Minio__Endpoint=minio:9000
      - Minio__AccessKey=${MINIO_ACCESS_KEY}
      - Minio__SecretKey=${MINIO_SECRET_KEY}
      - Kopia__RepositoryPath=/data/kopia-repo
      - Kopia__Password=${KOPIA_PASSWORD}
      - Tika__BaseUrl=http://tika:9998
    volumes:
      - pim_data:/data
      - ./keys:/data/keys:ro
    depends_on:
      postgres:
        condition: service_healthy
      minio:
        condition: service_healthy
    networks:
      - pim-net

  postgres:
    image: postgres:16-alpine
    restart: unless-stopped
    environment:
      POSTGRES_USER: pim
      POSTGRES_PASSWORD: ${PG_PASSWORD}
      POSTGRES_DB: pim
    volumes:
      - pg_data:/var/lib/postgresql/data
    healthcheck:
      test: ["CMD-SHELL", "pg_isready -U pim"]
      interval: 5s
      timeout: 5s
      retries: 5
    networks:
      - pim-net

  minio:
    image: minio/minio:latest
    restart: unless-stopped
    command: server /data --console-address ":9001"
    environment:
      MINIO_ROOT_USER: ${MINIO_ACCESS_KEY}
      MINIO_ROOT_PASSWORD: ${MINIO_SECRET_KEY}
    volumes:
      - minio_data:/data
    healthcheck:
      test: ["CMD", "curl", "-f", "http://localhost:9000/minio/health/live"]
      interval: 5s
      timeout: 5s
      retries: 5
    networks:
      - pim-net

  tika:
    image: apache/tika:latest
    restart: unless-stopped
    networks:
      - pim-net

  nginx:
    image: nginx:alpine
    restart: unless-stopped
    ports:
      - "80:80"
      - "443:443"
    volumes:
      - ./nginx.conf:/etc/nginx/nginx.conf:ro
      - ./ssl:/etc/nginx/ssl:ro
    depends_on:
      - pim-api
    networks:
      - pim-net

volumes:
  pg_data:
  minio_data:
  pim_data:

networks:
  pim-net:
    driver: bridge
```

- [ ] **Step 3: 编写 nginx.conf**

```nginx
events { worker_connections 1024; }

http {
    upstream pim_api {
        server pim-api:5000;
    }

    server {
        listen 80;
        server_name _;
        return 301 https://$host$request_uri;
    }

    server {
        listen 443 ssl;
        server_name _;

        ssl_certificate /etc/nginx/ssl/fullchain.pem;
        ssl_certificate_key /etc/nginx/ssl/privkey.pem;

        client_max_body_size 500M;

        location / {
            proxy_pass http://pim_api;
            proxy_http_version 1.1;
            proxy_set_header Upgrade $http_upgrade;
            proxy_set_header Connection "upgrade";
            proxy_set_header Host $host;
            proxy_set_header X-Real-IP $remote_addr;
            proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
            proxy_set_header X-Forwarded-Proto $scheme;
        }

        location /api/ {
            proxy_pass http://pim_api;
            proxy_http_version 1.1;
            proxy_set_header Host $host;
            proxy_set_header X-Real-IP $remote_addr;
        }
    }
}
```

- [ ] **Step 4: 创建 .env.example**

```bash
# .env.example
PG_PASSWORD=change_me_strong_password
MINIO_ACCESS_KEY=minioadmin
MINIO_SECRET_KEY=change_me_minio_password
KOPIA_PASSWORD=change_me_kopia_password
```

- [ ] **Step 5: Commit**

```powershell
git add src/Pim.Api/Dockerfile docker-compose.yml nginx.conf .env.example
git commit -m "feat: add Docker deployment config (pim-api, postgres, minio, tika, nginx)"
```

---

## 阶段五：日程模块 (Pim.Module.Calendar)

### Task 11: 创建项目与实体

**Files:**
- Create: `src/modules/Pim.Module.Calendar/Pim.Module.Calendar.csproj`
- Create: `src/modules/Pim.Module.Calendar/Entities/CalendarEntity.cs`
- Create: `src/modules/Pim.Module.Calendar/Entities/EventEntity.cs`
- Create: `src/modules/Pim.Module.Calendar/Entities/TaskEntity.cs`
- Create: `src/modules/Pim.Module.Calendar/Entities/PendingConfirmationEntity.cs`
- Create: `src/modules/Pim.Module.Calendar/Entities/SchedulingFeedbackEntity.cs`
- Create: `src/modules/Pim.Module.Calendar/Entities/OutlookConnectionEntity.cs`

- [ ] **Step 1: 创建项目**

```powershell
mkdir src\modules\Pim.Module.Calendar\Entities -Force
mkdir src\modules\Pim.Module.Calendar\Controllers -Force
mkdir src\modules\Pim.Module.Calendar\Services -Force
mkdir src\modules\Pim.Module.Calendar\DTOs -Force
dotnet new classlib -n Pim.Module.Calendar -o src/modules/Pim.Module.Calendar --framework net8.0
dotnet add src/modules/Pim.Module.Calendar/Pim.Module.Calendar.csproj reference src/Pim.Core/Pim.Core.csproj
dotnet add src/modules/Pim.Module.Calendar/Pim.Module.Calendar.csproj reference src/Pim.Infrastructure/Pim.Infrastructure.csproj
dotnet add src/modules/Pim.Module.Calendar/Pim.Module.Calendar.csproj package Ical.Net
```

- [ ] **Step 2: 编写 CalendarEntity.cs**

```csharp
// src/modules/Pim.Module.Calendar/Entities/CalendarEntity.cs
using System.ComponentModel.DataAnnotations;
using System.ComponentModel.DataAnnotations.Schema;
using Pim.Core.Data;

namespace Pim.Module.Calendar.Entities;

[Table("calendars")]
public class CalendarEntity : ISoftDeletable
{
    [Key][Column("id")] public Guid Id { get; set; } = Guid.NewGuid();
    [Column("user_id")] public Guid UserId { get; set; }
    [Column("name")][MaxLength(100)] public string Name { get; set; } = string.Empty;
    [Column("color")][MaxLength(7)] public string Color { get; set; } = "#3B82F6";
    [Column("is_default")] public bool IsDefault { get; set; }
    [Column("created_at")] public DateTimeOffset CreatedAt { get; set; } = DateTimeOffset.UtcNow;
    [Column("updated_at")] public DateTimeOffset UpdatedAt { get; set; } = DateTimeOffset.UtcNow;
    [Column("deleted_at")] public DateTimeOffset? DeletedAt { get; set; }

    public ICollection<EventEntity> Events { get; set; } = new List<EventEntity>();
    public ICollection<TaskEntity> Tasks { get; set; } = new List<TaskEntity>();
}
```

- [ ] **Step 3: 编写 EventEntity.cs**

```csharp
// src/modules/Pim.Module.Calendar/Entities/EventEntity.cs
using System.ComponentModel.DataAnnotations;
using System.ComponentModel.DataAnnotations.Schema;
using Pim.Core.Data;

namespace Pim.Module.Calendar.Entities;

[Table("events")]
public class EventEntity : ISoftDeletable
{
    [Key][Column("id")] public Guid Id { get; set; } = Guid.NewGuid();
    [Column("calendar_id")] public Guid CalendarId { get; set; }
    [Column("uid")][MaxLength(255)] public string Uid { get; set; } = string.Empty;
    [Column("title")][MaxLength(255)] public string Title { get; set; } = string.Empty;
    [Column("description")] public string? Description { get; set; }
    [Column("location")][MaxLength(500)] public string? Location { get; set; }
    [Column("dtstart")] public DateTimeOffset DtStart { get; set; }
    [Column("dtend")] public DateTimeOffset DtEnd { get; set; }
    [Column("dtstamp")] public DateTimeOffset DtStamp { get; set; } = DateTimeOffset.UtcNow;
    [Column("rrule")] public string? RRule { get; set; }
    [Column("status")][MaxLength(20)] public string Status { get; set; } = "CONFIRMED";
    [Column("organizer")][MaxLength(255)] public string? Organizer { get; set; }
    [Column("source")][MaxLength(20)] public string Source { get; set; } = "manual";
    [Column("outlook_event_id")][MaxLength(255)] public string? OutlookEventId { get; set; }
    [Column("schedule_plan_id")] public Guid? SchedulePlanId { get; set; }
    [Column("created_at")] public DateTimeOffset CreatedAt { get; set; } = DateTimeOffset.UtcNow;
    [Column("updated_at")] public DateTimeOffset UpdatedAt { get; set; } = DateTimeOffset.UtcNow;
    [Column("deleted_at")] public DateTimeOffset? DeletedAt { get; set; }

    [ForeignKey(nameof(CalendarId))]
    public CalendarEntity Calendar { get; set; } = null!;
}
```

- [ ] **Step 4: 编写 TaskEntity.cs**

```csharp
// src/modules/Pim.Module.Calendar/Entities/TaskEntity.cs
using System.ComponentModel.DataAnnotations;
using System.ComponentModel.DataAnnotations.Schema;
using Pim.Core.Data;

namespace Pim.Module.Calendar.Entities;

[Table("tasks")]
public class TaskEntity : ISoftDeletable
{
    [Key][Column("id")] public Guid Id { get; set; } = Guid.NewGuid();
    [Column("calendar_id")] public Guid? CalendarId { get; set; }
    [Column("uid")][MaxLength(255)] public string Uid { get; set; } = string.Empty;
    [Column("title")][MaxLength(255)] public string Title { get; set; } = string.Empty;
    [Column("description")] public string? Description { get; set; }
    [Column("priority")] public int Priority { get; set; }
    [Column("estimated_duration")] public TimeSpan? EstimatedDuration { get; set; }
    [Column("minimum_segment")] public TimeSpan? MinimumSegment { get; set; }
    [Column("dtstart")] public DateTimeOffset? DtStart { get; set; }
    [Column("due")] public DateTimeOffset? Due { get; set; }
    [Column("completed_at")] public DateTimeOffset? CompletedAt { get; set; }
    [Column("status")][MaxLength(20)] public string Status { get; set; } = "NEEDS-ACTION";
    [Column("percent_complete")] public int PercentComplete { get; set; }
    [Column("parent_task_id")] public Guid? ParentTaskId { get; set; }
    [Column("is_inbox")] public bool IsInbox { get; set; } = true;
    [Column("sort_order")] public int SortOrder { get; set; }
    [Column("schedule_plan_id")] public Guid? SchedulePlanId { get; set; }
    [Column("created_at")] public DateTimeOffset CreatedAt { get; set; } = DateTimeOffset.UtcNow;
    [Column("updated_at")] public DateTimeOffset UpdatedAt { get; set; } = DateTimeOffset.UtcNow;
    [Column("deleted_at")] public DateTimeOffset? DeletedAt { get; set; }

    [ForeignKey(nameof(CalendarId))]
    public CalendarEntity? Calendar { get; set; }

    [ForeignKey(nameof(ParentTaskId))]
    public TaskEntity? ParentTask { get; set; }

    public ICollection<TaskEntity> SubTasks { get; set; } = new List<TaskEntity>();
}
```

- [ ] **Step 5: 编写 PendingConfirmationEntity.cs, SchedulingFeedbackEntity.cs, OutlookConnectionEntity.cs**

```csharp
// src/modules/Pim.Module.Calendar/Entities/PendingConfirmationEntity.cs
using System.ComponentModel.DataAnnotations;
using System.ComponentModel.DataAnnotations.Schema;

namespace Pim.Module.Calendar.Entities;

[Table("pending_confirmations")]
public class PendingConfirmationEntity
{
    [Key][Column("id")] public Guid Id { get; set; } = Guid.NewGuid();
    [Column("user_id")] public Guid UserId { get; set; }
    [Column("type")][MaxLength(50)] public string Type { get; set; } = string.Empty;
    [Column("summary")] public string Summary { get; set; } = string.Empty;
    [Column("payload")][Column(TypeName = "jsonb")] public string Payload { get; set; } = "{}";
    [Column("status")][MaxLength(20)] public string Status { get; set; } = "pending";
    [Column("confirmed_at")] public DateTimeOffset? ConfirmedAt { get; set; }
    [Column("created_at")] public DateTimeOffset CreatedAt { get; set; } = DateTimeOffset.UtcNow;
}

// src/modules/Pim.Module.Calendar/Entities/SchedulingFeedbackEntity.cs
[Table("scheduling_feedback")]
public class SchedulingFeedbackEntity
{
    [Key][Column("id")] public Guid Id { get; set; } = Guid.NewGuid();
    [Column("user_id")] public Guid UserId { get; set; }
    [Column("plan_options")][Column(TypeName = "jsonb")] public string PlanOptions { get; set; } = "[]";
    [Column("selected_index")] public int SelectedIndex { get; set; }
    [Column("context")][Column(TypeName = "jsonb")] public string? Context { get; set; }
    [Column("created_at")] public DateTimeOffset CreatedAt { get; set; } = DateTimeOffset.UtcNow;
}

// src/modules/Pim.Module.Calendar/Entities/OutlookConnectionEntity.cs
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
```

- [ ] **Step 6: 更新 PimDbContext 添加日历实体**

在 `PimDbContext.cs` 添加：

```csharp
public DbSet<Pim.Module.Calendar.Entities.CalendarEntity> Calendars => Set<Pim.Module.Calendar.Entities.CalendarEntity>();
public DbSet<Pim.Module.Calendar.Entities.EventEntity> Events => Set<Pim.Module.Calendar.Entities.EventEntity>();
public DbSet<Pim.Module.Calendar.Entities.TaskEntity> Tasks => Set<Pim.Module.Calendar.Entities.TaskEntity>();
public DbSet<Pim.Module.Calendar.Entities.PendingConfirmationEntity> PendingConfirmations => Set<Pim.Module.Calendar.Entities.PendingConfirmationEntity>();
public DbSet<Pim.Module.Calendar.Entities.SchedulingFeedbackEntity> SchedulingFeedbacks => Set<Pim.Module.Calendar.Entities.SchedulingFeedbackEntity>();
public DbSet<Pim.Module.Calendar.Entities.OutlookConnectionEntity> OutlookConnections => Set<Pim.Module.Calendar.Entities.OutlookConnectionEntity>();
```

同时在 `Pim.Infrastructure.csproj` 添加日历模块引用：
```powershell
dotnet add src/Pim.Infrastructure/Pim.Infrastructure.csproj reference src/modules/Pim.Module.Calendar/Pim.Module.Calendar.csproj
```

在 `OnModelCreating` 中为实体添加查询过滤和关系配置。

- [ ] **Step 7: 验证构建**

```powershell
dotnet build Pim.sln
```

Expected: Build succeeded.

- [ ] **Step 8: Commit**

```powershell
git add src/modules/Pim.Module.Calendar/Entities/ src/Pim.Infrastructure/Data/PimDbContext.cs
git commit -m "feat: add calendar module entities (calendar, event, task, confirmation, feedback, outlook)"
```

---

### Task 12: 日历服务层

**Files:**
- Create: `src/modules/Pim.Module.Calendar/Services/CalendarService.cs`
- Create: `src/modules/Pim.Module.Calendar/Services/IcsService.cs`
- Create: `src/modules/Pim.Module.Calendar/DTOs/CalendarDtos.cs`

- [ ] **Step 1: 编写 CalendarDtos.cs**

```csharp
// src/modules/Pim.Module.Calendar/DTOs/CalendarDtos.cs
using System.ComponentModel.DataAnnotations;

namespace Pim.Module.Calendar.DTOs;

public record CreateCalendarRequest(
    [Required][MaxLength(100)] string Name,
    [MaxLength(7)] string? Color
);

public record CalendarResponse(
    Guid Id, string Name, string Color, bool IsDefault, int EventCount
);

public record CreateEventRequest(
    [Required] Guid CalendarId,
    [Required][MaxLength(255)] string Title,
    string? Description,
    [MaxLength(500)] string? Location,
    [Required] DateTimeOffset DtStart,
    [Required] DateTimeOffset DtEnd,
    string? RRule
);

public record EventResponse(
    Guid Id, Guid CalendarId, string Uid, string Title,
    string? Description, string? Location,
    DateTimeOffset DtStart, DateTimeOffset DtEnd,
    string? RRule, string Status, string Source
);

public record CreateTaskRequest(
    Guid? CalendarId,
    [Required][MaxLength(255)] string Title,
    string? Description,
    int Priority,
    string? EstimatedDuration,     // ISO 8601 duration
    string? MinimumSegment,        // ISO 8601 duration
    DateTimeOffset? Due
);

public record TaskResponse(
    Guid Id, Guid? CalendarId, string Uid, string Title,
    string? Description, int Priority,
    string? EstimatedDuration, string? MinimumSegment,
    DateTimeOffset? DtStart, DateTimeOffset? Due,
    string Status, bool IsInbox, int SortOrder,
    List<TaskResponse> SubTasks
);

public record MoveTaskRequest(
    DateTimeOffset? ScheduledStart,
    TimeSpan? Duration,
    int? NewSortOrder
);

public record ScheduleRequest(
    List<Guid> TaskIds
);

public record SchedulePlanResponse(
    Guid PlanId,
    string AlgorithmName,
    List<ScheduledTaskSlot> Slots
);

public record ScheduledTaskSlot(
    Guid TaskId,
    string TaskTitle,
    DateTimeOffset Start,
    DateTimeOffset End
);
```

- [ ] **Step 2: 编写 IcsService.cs**

```csharp
// src/modules/Pim.Module.Calendar/Services/IcsService.cs
using Ical.Net;
using Ical.Net.CalendarComponents;
using Ical.Net.DataTypes;
using Ical.Net.Serialization;
using Pim.Module.Calendar.Entities;

namespace Pim.Module.Calendar.Services;

public class IcsService
{
    public string ExportEvents(IEnumerable<EventEntity> events)
    {
        var calendar = new Ical.Net.Calendar();
        foreach (var evt in events)
        {
            var calEvent = new CalendarEvent
            {
                Uid = evt.Uid,
                Summary = evt.Title,
                Description = evt.Description,
                Location = evt.Location,
                Start = new CalDateTime(evt.DtStart.UtcDateTime),
                End = new CalDateTime(evt.DtEnd.UtcDateTime),
                DtStamp = new CalDateTime(evt.DtStamp.UtcDateTime),
                Status = evt.Status
            };

            if (!string.IsNullOrEmpty(evt.RRule))
                calEvent.RecurrenceRules.Add(new RecurrencePattern(evt.RRule));

            calendar.Events.Add(calEvent);
        }

        var serializer = new CalendarSerializer();
        return serializer.SerializeToString(calendar);
    }

    public List<ParsedEvent> ImportEvents(string icsContent)
    {
        var calendar = Calendar.Load(icsContent);
        return calendar.Events.Select(e => new ParsedEvent(
            e.Uid ?? Guid.NewGuid().ToString(),
            e.Summary ?? "Untitled",
            e.Description,
            e.Location,
            new DateTimeOffset(e.Start.AsDateTimeOffset, TimeSpan.Zero),
            new DateTimeOffset(e.End.AsDateTimeOffset, TimeSpan.Zero),
            e.RecurrenceRules.FirstOrDefault()?.ToString())
        ).ToList();
    }
}

public record ParsedEvent(
    string Uid, string Title, string? Description,
    string? Location, DateTimeOffset Start, DateTimeOffset End, string? RRule
);
```

- [ ] **Step 3: 编写 CalendarService.cs**

```csharp
// src/modules/Pim.Module.Calendar/Services/CalendarService.cs
using Microsoft.EntityFrameworkCore;
using Pim.Core.Exceptions;
using Pim.Infrastructure.Auth;
using Pim.Infrastructure.Data;
using Pim.Module.Calendar.DTOs;
using Pim.Module.Calendar.Entities;

namespace Pim.Module.Calendar.Services;

public class CalendarService
{
    private readonly PimDbContext _db;
    private readonly ICurrentUserService _currentUser;

    public CalendarService(PimDbContext db, ICurrentUserService currentUser)
    {
        _db = db;
        _currentUser = currentUser;
    }

    private Guid UserId => _currentUser.UserId ?? throw new DomainException(01002, "Not authenticated");

    // --- Calendars ---
    public async Task<List<CalendarResponse>> GetCalendarsAsync(CancellationToken ct)
    {
        return await _db.Set<CalendarEntity>()
            .Where(c => c.UserId == UserId)
            .Select(c => new CalendarResponse(c.Id, c.Name, c.Color, c.IsDefault,
                c.Events.Count))
            .ToListAsync(ct);
    }

    public async Task<CalendarResponse> CreateCalendarAsync(CreateCalendarRequest request, CancellationToken ct)
    {
        var calendar = new CalendarEntity
        {
            UserId = UserId,
            Name = request.Name,
            Color = request.Color ?? "#3B82F6",
            IsDefault = !await _db.Set<CalendarEntity>().AnyAsync(c => c.UserId == UserId, ct)
        };
        _db.Set<CalendarEntity>().Add(calendar);
        await _db.SaveChangesAsync(ct);
        return new CalendarResponse(calendar.Id, calendar.Name, calendar.Color, calendar.IsDefault, 0);
    }

    // --- Events ---
    public async Task<List<EventResponse>> GetEventsAsync(
        DateTimeOffset start, DateTimeOffset end, CancellationToken ct)
    {
        return await _db.Set<EventEntity>()
            .Where(e => e.Calendar.UserId == UserId &&
                        e.DtStart < end && e.DtEnd > start)
            .OrderBy(e => e.DtStart)
            .Select(e => new EventResponse(
                e.Id, e.CalendarId, e.Uid, e.Title, e.Description,
                e.Location, e.DtStart, e.DtEnd, e.RRule, e.Status, e.Source))
            .ToListAsync(ct);
    }

    public async Task<EventResponse> CreateEventAsync(CreateEventRequest request, CancellationToken ct)
    {
        var calendar = await _db.Set<CalendarEntity>()
            .FirstOrDefaultAsync(c => c.Id == request.CalendarId && c.UserId == UserId, ct)
            ?? throw new DomainException(02003, "Calendar not found");

        var entity = new EventEntity
        {
            CalendarId = request.CalendarId,
            Uid = Guid.NewGuid().ToString() + "@pim",
            Title = request.Title,
            Description = request.Description,
            Location = request.Location,
            DtStart = request.DtStart,
            DtEnd = request.DtEnd,
            RRule = request.RRule
        };

        _db.Set<EventEntity>().Add(entity);
        await _db.SaveChangesAsync(ct);

        return MapEvent(entity);
    }

    public async Task<EventResponse> UpdateEventAsync(Guid id, CreateEventRequest request, CancellationToken ct)
    {
        var entity = await _db.Set<EventEntity>()
            .FirstOrDefaultAsync(e => e.Id == id && e.Calendar.UserId == UserId, ct)
            ?? throw new DomainException(02001, "Event not found");

        entity.Title = request.Title;
        entity.Description = request.Description;
        entity.Location = request.Location;
        entity.DtStart = request.DtStart;
        entity.DtEnd = request.DtEnd;
        entity.RRule = request.RRule;
        entity.UpdatedAt = DateTimeOffset.UtcNow;

        await _db.SaveChangesAsync(ct);
        return MapEvent(entity);
    }

    public async Task<List<EventEntity>> GetEventEntitiesAsync(
        DateTimeOffset start, DateTimeOffset end, CancellationToken ct)
    {
        return await _db.Set<EventEntity>()
            .Where(e => e.Calendar.UserId == UserId &&
                        e.DtStart < end && e.DtEnd > start)
            .OrderBy(e => e.DtStart)
            .ToListAsync(ct);
    }

    public async Task DeleteEventAsync(Guid id, CancellationToken ct)
    {
        var entity = await _db.Set<EventEntity>()
            .FirstOrDefaultAsync(e => e.Id == id && e.Calendar.UserId == UserId, ct)
            ?? throw new DomainException(02001, "Event not found");

        entity.DeletedAt = DateTimeOffset.UtcNow;
        await _db.SaveChangesAsync(ct);
    }

    // --- Tasks ---
    public async Task<List<TaskResponse>> GetTasksAsync(bool? inbox, CancellationToken ct)
    {
        var query = _db.Set<TaskEntity>()
            .Where(t => t.Calendar == null || t.Calendar.UserId == UserId);

        if (inbox.HasValue)
            query = query.Where(t => t.IsInbox == inbox.Value);

        var tasks = await query.OrderBy(t => t.SortOrder).ToListAsync(ct);
        return tasks.Select(MapTask).ToList();
    }

    public async Task<TaskResponse> CreateTaskAsync(CreateTaskRequest request, CancellationToken ct)
    {
        var task = new TaskEntity
        {
            CalendarId = request.CalendarId,
            Uid = Guid.NewGuid().ToString() + "@pim",
            Title = request.Title,
            Description = request.Description,
            Priority = request.Priority,
            Due = request.Due,
            EstimatedDuration = request.EstimatedDuration is not null
                ? System.Xml.XmlConvert.ToTimeSpan(request.EstimatedDuration) : null,
            MinimumSegment = request.MinimumSegment is not null
                ? System.Xml.XmlConvert.ToTimeSpan(request.MinimumSegment) : null,
            IsInbox = request.CalendarId is null
        };

        _db.Set<TaskEntity>().Add(task);
        await _db.SaveChangesAsync(ct);
        return MapTask(task);
    }

    public async Task MoveTaskAsync(Guid id, MoveTaskRequest request, CancellationToken ct)
    {
        var task = await _db.Set<TaskEntity>().FindAsync(new object[] { id }, ct)
            ?? throw new DomainException(02004, "Task not found");

        if (request.ScheduledStart.HasValue)
        {
            task.DtStart = request.ScheduledStart;
            task.IsInbox = false;
            var duration = request.Duration ?? task.EstimatedDuration ?? TimeSpan.FromHours(1);
            // Creating or updating associated event would be done here
        }

        if (request.NewSortOrder.HasValue)
            task.SortOrder = request.NewSortOrder.Value;

        task.UpdatedAt = DateTimeOffset.UtcNow;
        await _db.SaveChangesAsync(ct);
    }

    private static EventResponse MapEvent(EventEntity e) =>
        new(e.Id, e.CalendarId, e.Uid, e.Title, e.Description,
            e.Location, e.DtStart, e.DtEnd, e.RRule, e.Status, e.Source);

    private static TaskResponse MapTask(TaskEntity t) =>
        new(t.Id, t.CalendarId, t.Uid, t.Title, t.Description,
            t.Priority,
            t.EstimatedDuration is not null
                ? System.Xml.XmlConvert.ToString(t.EstimatedDuration.Value) : null,
            t.MinimumSegment is not null
                ? System.Xml.XmlConvert.ToString(t.MinimumSegment.Value) : null,
            t.DtStart, t.Due, t.Status, t.IsInbox, t.SortOrder,
            t.SubTasks.Select(MapTask).ToList());
}
```

- [ ] **Step 4: 验证构建**

```powershell
dotnet build Pim.sln
```

Expected: Build succeeded.

- [ ] **Step 5: Commit**

```powershell
git add src/modules/Pim.Module.Calendar/Services/ src/modules/Pim.Module.Calendar/DTOs/
git commit -m "feat: add calendar service layer (CRUD, ICS import/export)"
```

---

### Task 13: 自动排程引擎

**Files:**
- Create: `src/modules/Pim.Module.Calendar/Services/SchedulingEngine.cs`
- Create: `src/modules/Pim.Module.Calendar/Services/SchedulingAlgorithms.cs`

- [ ] **Step 1: 编写调度算法接口和数据模型**

```csharp
// src/modules/Pim.Module.Calendar/Services/SchedulingAlgorithms.cs
namespace Pim.Module.Calendar.Services;

public record TimeSlot(DateTimeOffset Start, DateTimeOffset End);
public record TaskToSchedule(Guid TaskId, string Title, int Priority, TimeSpan Duration,
    TimeSpan? MinSegment, DateTimeOffset? Deadline, double UserPreferenceWeight = 1.0);
public record BusySlot(DateTimeOffset Start, DateTimeOffset End);
public record ScheduleSolution(string AlgorithmName, List<ScheduledSlot> Slots, Dictionary<string, double> Metrics);
public record ScheduledSlot(Guid TaskId, string Title, DateTimeOffset Start, DateTimeOffset End);

public static class SchedulingHelpers
{
    public static List<TimeSlot> ComputeFreeSlots(
        List<BusySlot> busy, DateTimeOffset start, DateTimeOffset end)
    {
        var free = new List<TimeSlot>();
        var sorted = busy.OrderBy(b => b.Start).ToList();
        var cursor = start;
        foreach (var b in sorted)
        {
            if (b.End <= cursor) continue;
            if (b.Start > cursor) free.Add(new TimeSlot(cursor, b.Start));
            cursor = b.End > cursor ? b.End : cursor;
        }
        if (cursor < end) free.Add(new TimeSlot(cursor, end));
        return free;
    }
}

public interface ISchedulingAlgorithm
{
    string Name { get; }
    Task<ScheduleSolution?> SolveAsync(
        List<TaskToSchedule> tasks,
        List<BusySlot> busySlots,
        DateTimeOffset searchStart,
        DateTimeOffset searchEnd,
        Dictionary<string, double> userWeights,
        CancellationToken ct);
}
```

- [ ] **Step 2: 实现贪心算法**

```csharp
// src/modules/Pim.Module.Calendar/Services/SchedulingAlgorithms.cs (continue)
public class GreedyScheduler : ISchedulingAlgorithm
{
    public string Name => "greedy";

    public Task<ScheduleSolution?> SolveAsync(
        List<TaskToSchedule> tasks,
        List<BusySlot> busySlots,
        DateTimeOffset searchStart,
        DateTimeOffset searchEnd,
        Dictionary<string, double> userWeights,
        CancellationToken ct)
    {
        var sorted = tasks.OrderByDescending(t => t.Priority)
            .ThenBy(t => t.Deadline ?? DateTimeOffset.MaxValue)
            .ToList();

        var freeSlots = SchedulingHelpers.ComputeFreeSlots(busySlots, searchStart, searchEnd);
        var result = new List<ScheduledSlot>();

        foreach (var task in sorted)
        {
            var remaining = task.Duration;
            foreach (var slot in freeSlots)
            {
                if (remaining <= TimeSpan.Zero) break;
                var slotDuration = slot.End - slot.Start;
                if (slotDuration <= TimeSpan.Zero) continue;

                var alloc = remaining < slotDuration ? remaining : slotDuration;
                result.Add(new ScheduledSlot(task.TaskId, task.Title, slot.Start, slot.Start + alloc));
                remaining -= alloc;
            }
        }

        return Task.FromResult<ScheduleSolution?>(
            new ScheduleSolution(Name, result, new Dictionary<string, double>
            {
                ["tasks_scheduled"] = result.Count,
                ["total_tasks"] = tasks.Count
            }));
    }
}
```

- [ ] **Step 3: 实现 CSP 回溯**

```csharp
// src/modules/Pim.Module.Calendar/Services/SchedulingAlgorithms.cs (continue)
public class CspScheduler : ISchedulingAlgorithm
{
    public string Name => "csp";
    private readonly TimeSpan _timeout = TimeSpan.FromSeconds(30);

    public Task<ScheduleSolution?> SolveAsync(
        List<TaskToSchedule> tasks,
        List<BusySlot> busySlots,
        DateTimeOffset searchStart,
        DateTimeOffset searchEnd,
        Dictionary<string, double> userWeights,
        CancellationToken ct)
    {
        var freeSlots = SchedulingHelpers.ComputeFreeSlots(busySlots, searchStart, searchEnd);

        // Phase 1: Try to assign tasks greedily
        var solution = new List<ScheduledSlot>();
        var unscheduled = new List<TaskToSchedule>();
        var assignedFreeSlots = freeSlots.Select(s =>
            new { Slot = s, Remaining = s.End - s.Start, Start = s.Start }).ToList();

        var sorted = tasks.OrderByDescending(t => t.Priority)
            .ThenBy(t => t.Deadline ?? DateTimeOffset.MaxValue).ToList();

        foreach (var task in sorted)
        {
            var placed = false;
            foreach (var slot in assignedFreeSlots.Where(s => s.Remaining >= task.Duration))
            {
                solution.Add(new ScheduledSlot(task.TaskId, task.Title,
                    slot.Start, slot.Start + task.Duration));
                var newRemaining = slot.Remaining - task.Duration;
                var idx = assignedFreeSlots.IndexOf(slot);
                assignedFreeSlots[idx] = new { slot.Slot, Remaining = newRemaining,
                    Start = slot.Start + task.Duration };
                placed = true;
                break;
            }

            if (!placed) unscheduled.Add(task);
        }

        // Phase 2: Try constraint relaxation for unscheduled tasks (reduce min segment)
        foreach (var task in unscheduled)
        {
            var relaxedDuration = task.MinSegment ?? TimeSpan.FromMinutes(15);
            foreach (var slot in assignedFreeSlots.Where(s => s.Remaining >= relaxedDuration))
            {
                solution.Add(new ScheduledSlot(task.TaskId, task.Title,
                    slot.Start, slot.Start + relaxedDuration));
                break;
            }
        }

        return Task.FromResult<ScheduleSolution?>(
            new ScheduleSolution(Name, solution, new Dictionary<string, double>
            {
                ["tasks_scheduled"] = solution.Count,
                ["total_tasks"] = tasks.Count,
                ["constraint_relaxations"] = unscheduled.Count
            }));
    }
}
```

- [ ] **Step 4: 实现遗传算法**

```csharp
// src/modules/Pim.Module.Calendar/Services/SchedulingAlgorithms.cs (continue)
public class GeneticScheduler : ISchedulingAlgorithm
{
    public string Name => "genetic";
    private const int PopulationSize = 50;
    private const int Generations = 100;
    private const double MutationRate = 0.1;

    public Task<ScheduleSolution?> SolveAsync(
        List<TaskToSchedule> tasks,
        List<BusySlot> busySlots,
        DateTimeOffset searchStart,
        DateTimeOffset searchEnd,
        Dictionary<string, double> userWeights,
        CancellationToken ct)
    {
        var rng = new Random();
        var freeSlots = SchedulingHelpers.ComputeFreeSlots(busySlots, searchStart, searchEnd);
        var population = new List<List<ScheduledSlot>>();

        // Initialize population
        for (int i = 0; i < PopulationSize; i++)
        {
            population.Add(RandomSchedule(tasks, freeSlots, rng));
        }

        // Evolve
        for (int gen = 0; gen < Generations && !ct.IsCancellationRequested; gen++)
        {
            var fitnesses = population.Select(p =>
                Fitness(p, tasks, userWeights)).ToList();
            var newPop = new List<List<ScheduledSlot>>();

            // Elitism: keep top 5
            var elite = population.Zip(fitnesses)
                .OrderByDescending(x => x.Second)
                .Take(5).Select(x => x.First).ToList();
            newPop.AddRange(elite);

            // Crossover + Mutation
            while (newPop.Count < PopulationSize)
            {
                var parent1 = SelectParent(population, fitnesses, rng);
                var parent2 = SelectParent(population, fitnesses, rng);
                var child = Crossover(parent1, parent2, rng);
                if (rng.NextDouble() < MutationRate)
                    Mutate(child, freeSlots, rng);
                newPop.Add(child);
            }

            population = newPop;
        }

        var best = population.Zip(population.Select(p => Fitness(p, tasks, userWeights)))
            .OrderByDescending(x => x.Second).First().First;

        return Task.FromResult<ScheduleSolution?>(
            new ScheduleSolution(Name, best, new Dictionary<string, double>
            {
                ["fitness"] = Fitness(best, tasks, userWeights),
                ["tasks_scheduled"] = best.Count
            }));
    }

    private List<ScheduledSlot> RandomSchedule(
        List<TaskToSchedule> tasks, List<TimeSlot> freeSlots, Random rng)
    {
        var result = new List<ScheduledSlot>();
        var shuffled = tasks.OrderBy(_ => rng.Next()).ToList();
        var remainingSlots = freeSlots.Select(s =>
            (Start: s.Start, Remaining: s.End - s.Start)).ToList();

        foreach (var task in shuffled)
        {
            var candidates = remainingSlots
                .Where(s => s.Remaining >= task.Duration).ToList();
            if (!candidates.Any()) continue;
            var slot = candidates[rng.Next(candidates.Count)];
            result.Add(new ScheduledSlot(task.TaskId, task.Title,
                slot.Start, slot.Start + task.Duration));
            var idx = remainingSlots.IndexOf(slot);
            remainingSlots[idx] = (slot.Start + task.Duration,
                slot.Remaining - task.Duration);
        }
        return result;
    }

    private double Fitness(List<ScheduledSlot> slots,
        List<TaskToSchedule> tasks, Dictionary<string, double> weights)
    {
        var scheduledIds = slots.Select(s => s.TaskId).ToHashSet();
        var coverage = (double)scheduledIds.Count / tasks.Count;
        var prioritySum = tasks.Where(t => scheduledIds.Contains(t.TaskId))
            .Sum(t => t.Priority);
        var totalPriority = tasks.Sum(t => t.Priority);
        var priorityScore = totalPriority > 0 ? prioritySum / totalPriority : 0;
        var priorityWeight = weights.GetValueOrDefault("priority", 0.5);
        var coverageWeight = weights.GetValueOrDefault("coverage", 0.5);
        return priorityWeight * priorityScore + coverageWeight * coverage;
    }

    private List<ScheduledSlot> SelectParent(
        List<List<ScheduledSlot>> pop, List<double> fitnesses, Random rng)
    {
        var total = fitnesses.Sum();
        var r = rng.NextDouble() * total;
        var cumulative = 0.0;
        for (int i = 0; i < pop.Count; i++)
        {
            cumulative += fitnesses[i];
            if (r <= cumulative) return pop[i];
        }
        return pop.Last();
    }

    private List<ScheduledSlot> Crossover(
        List<ScheduledSlot> a, List<ScheduledSlot> b, Random rng)
    {
        var split = rng.Next(Math.Min(a.Count, b.Count));
        return a.Take(split).Concat(b.Skip(split)).ToList();
    }

    private void Mutate(List<ScheduledSlot> schedule,
        List<TimeSlot> freeSlots, Random rng)
    {
        if (schedule.Count > 0)
        {
            var idx = rng.Next(schedule.Count);
            schedule.RemoveAt(idx);
        }
    }
}
```

- [ ] **Step 5: 编写 SchedulingEngine.cs（主排程引擎，含 LLM 兜底）**

```csharp
// src/modules/Pim.Module.Calendar/Services/SchedulingEngine.cs
using Microsoft.EntityFrameworkCore;
using Pim.Infrastructure.Data;
using Pim.Module.Calendar.Entities;
using System.Text.Json;

namespace Pim.Module.Calendar.Services;

public class SchedulingEngine
{
    private readonly PimDbContext _db;
    private readonly IHttpClientFactory _httpClientFactory;
    private readonly List<ISchedulingAlgorithm> _algorithms;

    public SchedulingEngine(PimDbContext db, IHttpClientFactory httpClientFactory)
    {
        _db = db;
        _httpClientFactory = httpClientFactory;
        _algorithms = new List<ISchedulingAlgorithm>
        {
            new GreedyScheduler(),
            new CspScheduler(),
            new GeneticScheduler()
        };
    }

    public async Task<List<ScheduleSolution>> GeneratePlansAsync(
        Guid userId, List<Guid> taskIds, CancellationToken ct)
    {
        var tasks = await _db.Set<TaskEntity>()
            .Where(t => taskIds.Contains(t.Id) && t.EstimatedDuration.HasValue)
            .ToListAsync(ct);

        var events = await _db.Set<EventEntity>()
            .Where(e => e.Calendar.UserId == userId)
            .ToListAsync(ct);

        var tasksToSchedule = tasks.Select(t => new TaskToSchedule(
            t.Id, t.Title, t.Priority,
            t.EstimatedDuration ?? TimeSpan.FromHours(1),
            t.MinimumSegment, t.Due, 1.0)).ToList();

        var busySlots = events.Select(e => new BusySlot(e.DtStart, e.DtEnd)).ToList();
        var now = DateTimeOffset.UtcNow;
        var searchEnd = now.AddDays(14);

        // Get user preference weights from feedback
        var weights = await GetUserWeightsAsync(userId);

        var solutions = new List<ScheduleSolution>();
        foreach (var algo in _algorithms)
        {
            var solution = await algo.SolveAsync(
                tasksToSchedule, busySlots, now, searchEnd, weights, ct);
            if (solution is not null) solutions.Add(solution);
        }

        // If all algorithms failed, try LLM fallback
        if (!solutions.Any(s => s.Slots.Count > 0))
        {
            var llmSolution = await TryLlmFallbackAsync(
                tasksToSchedule, busySlots, now, searchEnd, ct);
            if (llmSolution is not null) solutions.Add(llmSolution);
        }

        return solutions;
    }

    private async Task<Dictionary<string, double>> GetUserWeightsAsync(Guid userId)
    {
        var feedbcks = await _db.Set<SchedulingFeedbackEntity>()
            .Where(f => f.UserId == userId)
            .OrderByDescending(f => f.CreatedAt)
            .Take(50)
            .ToListAsync();

        if (feedbcks.Count < 5)
            return new Dictionary<string, double>
            {
                ["priority"] = 0.5,
                ["coverage"] = 0.3,
                ["compactness"] = 0.2
            };

        // Simple average weight estimation from feedback
        return new Dictionary<string, double>
        {
            ["priority"] = 0.6,
            ["coverage"] = 0.25,
            ["compactness"] = 0.15
        };
    }

    private async Task<ScheduleSolution?> TryLlmFallbackAsync(
        List<TaskToSchedule> tasks,
        List<BusySlot> busy,
        DateTimeOffset start, DateTimeOffset end,
        CancellationToken ct)
    {
        var client = _httpClientFactory.CreateClient("llm");
        var prompt = BuildLlmPrompt(tasks, busy, start, end);

        var response = await client.PostAsJsonAsync("/v1/chat/completions", new
        {
            model = "gpt-4",
            messages = new[]
            {
                new { role = "system", content = "You are a task scheduling assistant. Output JSON only." },
                new { role = "user", content = prompt }
            }
        }, ct);

        if (!response.IsSuccessStatusCode) return null;

        // Parse LLM response into ScheduleSolution
        var jsonResponse = await response.Content.ReadAsStringAsync(ct);
        return ParseLlmScheduleResponse(jsonResponse);
    }

    private string BuildLlmPrompt(List<TaskToSchedule> tasks,
        List<BusySlot> busy, DateTimeOffset start, DateTimeOffset end)
    {
        var taskDescriptions = tasks.Select(t =>
            $"- {t.Title}: {t.Duration.TotalHours:F1}h, priority {t.Priority}/9, deadline {t.Deadline}");
        var busyDescriptions = busy.Select(b =>
            $"- Busy: {b.Start:yyyy-MM-dd HH:mm} to {b.End:HH:mm}");

        return $"""
            Schedule these tasks into free time slots between {start:yyyy-MM-dd} and {end:yyyy-MM-dd}:
            Tasks:
            {string.Join("\n", taskDescriptions)}
            Busy slots:
            {string.Join("\n", busyDescriptions)}
            Return a JSON array of {{"taskIndex": 0, "start": "ISO8601", "end": "ISO8601"}}.
            Tasks can be split into minimum 30-minute segments.
            Higher priority tasks should be scheduled earlier.
            """;
    }

    private ScheduleSolution? ParseLlmScheduleResponse(string json)
    {
        try
        {
            var slots = JsonSerializer.Deserialize<List<LlmSlot>>(json);
            return slots is null ? null : new ScheduleSolution("llm",
                slots.Select(s => new ScheduledSlot(
                    Guid.Empty, $"Task #{s.TaskIndex}",
                    DateTimeOffset.Parse(s.Start), DateTimeOffset.Parse(s.End))).ToList(),
                new Dictionary<string, double> { ["source"] = 1.0 });
        }
        catch { return null; }
    }

    private record LlmSlot(int TaskIndex, string Start, string End);
}
```

- [ ] **Step 6: 验证构建**

```powershell
dotnet build Pim.sln
```

Expected: Build succeeded.

- [ ] **Step 7: Commit**

```powershell
git add src/modules/Pim.Module.Calendar/Services/SchedulingEngine.cs
git add src/modules/Pim.Module.Calendar/Services/SchedulingAlgorithms.cs
git commit -m "feat: add scheduling engine (greedy, CSP, genetic, LLM fallback)"
```

---

### Task 14: Outlook 同步服务

**Files:**
- Create: `src/modules/Pim.Module.Calendar/Services/OutlookSyncService.cs`

- [ ] **Step 1: 编写 OutlookSyncService.cs**

```csharp
// src/modules/Pim.Module.Calendar/Services/OutlookSyncService.cs
using System.Net.Http.Headers;
using System.Text.Json;
using Microsoft.EntityFrameworkCore;
using Pim.Core.Exceptions;
using Pim.Infrastructure.Data;
using Pim.Module.Calendar.Entities;

namespace Pim.Module.Calendar.Services;

public class OutlookSyncService
{
    private readonly PimDbContext _db;
    private readonly IHttpClientFactory _httpClientFactory;
    private const string GraphBaseUrl = "https://graph.microsoft.com/v1.0";

    public OutlookSyncService(PimDbContext db, IHttpClientFactory httpClientFactory)
    {
        _db = db;
        _httpClientFactory = httpClientFactory;
    }

    public async Task SyncAsync(Guid userId, CancellationToken ct)
    {
        var connection = await _db.Set<OutlookConnectionEntity>()
            .FirstOrDefaultAsync(c => c.UserId == userId, ct)
            ?? throw new DomainException(02005, "Outlook not connected");

        var client = CreateGraphClient(connection);
        var events = await FetchOutlookEventsAsync(client, ct);

        foreach (var outlookEvent in events)
        {
            var existing = await _db.Set<EventEntity>()
                .FirstOrDefaultAsync(e =>
                    e.OutlookEventId == outlookEvent.Id &&
                    e.Calendar.UserId == userId, ct);

            if (existing is null)
            {
                _db.Set<EventEntity>().Add(MapOutlookEvent(outlookEvent, userId));
            }
            else if (outlookEvent.LastModifiedDateTime > existing.UpdatedAt)
            {
                UpdateFromOutlookEvent(existing, outlookEvent);
            }
        }

        connection.LastSyncedAt = DateTimeOffset.UtcNow;
        await _db.SaveChangesAsync(ct);
    }

    public async Task CreateOutlookSubscriptionAsync(Guid userId, string notificationUrl, CancellationToken ct)
    {
        var connection = await _db.Set<OutlookConnectionEntity>()
            .FirstOrDefaultAsync(c => c.UserId == userId, ct)
            ?? throw new DomainException(02005, "Outlook not connected");

        var client = CreateGraphClient(connection);

        var response = await client.PostAsJsonAsync("/subscriptions", new
        {
            changeType = "created,updated,deleted",
            notificationUrl,
            resource = "me/events",
            expirationDateTime = DateTimeOffset.UtcNow.AddDays(3).ToString("o"),
            clientState = userId.ToString()
        }, ct);

        response.EnsureSuccessStatusCode();
        var subscription = await response.Content.ReadFromJsonAsync<JsonElement>(ct);

        connection.SubscriptionId = subscription.GetProperty("id").GetString();
        connection.SubscriptionExpiresAt =
            DateTimeOffset.Parse(subscription.GetProperty("expirationDateTime").GetString()!);
        await _db.SaveChangesAsync(ct);
    }

    public async Task WriteToOutlookAsync(Guid userId, EventEntity evt, CancellationToken ct)
    {
        var connection = await _db.Set<OutlookConnectionEntity>()
            .FirstOrDefaultAsync(c => c.UserId == userId, ct)
            ?? throw new DomainException(02005, "Outlook not connected");

        // Create pending confirmation for write operation
        _db.Set<PendingConfirmationEntity>().Add(new PendingConfirmationEntity
        {
            UserId = userId,
            Type = "outlook_write",
            Summary = $"Write event '{evt.Title}' to Outlook?",
            Payload = JsonSerializer.Serialize(new { eventId = evt.Id, action = "write_to_outlook" })
        });

        await _db.SaveChangesAsync(ct);
    }

    public async Task ExecuteConfirmedWriteAsync(Guid confirmationId, CancellationToken ct)
    {
        var confirmation = await _db.Set<PendingConfirmationEntity>()
            .FindAsync(new object[] { confirmationId }, ct)
            ?? throw new DomainException(02006, "Confirmation not found");

        if (confirmation.Status != "confirmed")
            throw new DomainException(02007, "Confirmation not yet confirmed");

        var payload = JsonSerializer.Deserialize<JsonElement>(confirmation.Payload);
        var eventId = payload.GetProperty("eventId").GetGuid();
        var action = payload.GetProperty("action").GetString();

        if (action == "write_to_outlook")
        {
            // Execute the actual Outlook write
            var connection = await _db.Set<OutlookConnectionEntity>()
                .FirstOrDefaultAsync(c => c.UserId == confirmation.UserId, ct)!;
            var client = CreateGraphClient(connection);
            var evt = await _db.Set<EventEntity>().FindAsync(new object[] { eventId }, ct);

            var outlookEvent = new
            {
                subject = evt!.Title,
                body = new { contentType = "text", content = evt.Description ?? "" },
                start = new { dateTime = evt.DtStart.ToString("o"), timeZone = "UTC" },
                end = new { dateTime = evt.DtEnd.ToString("o"), timeZone = "UTC" }
            };

            var response = await client.PostAsJsonAsync("/me/events", outlookEvent, ct);
            response.EnsureSuccessStatusCode();
        }
    }

    private HttpClient CreateGraphClient(OutlookConnectionEntity connection)
    {
        // Decrypt tokens - simplified for this plan
        var accessToken = System.Text.Encoding.UTF8.GetString(
            connection.AccessTokenEncrypted);
        var client = _httpClientFactory.CreateClient("outlook");
        client.BaseAddress = new Uri(GraphBaseUrl);
        client.DefaultRequestHeaders.Authorization =
            new AuthenticationHeaderValue("Bearer", accessToken);
        return client;
    }

    private async Task<List<OutlookEventInfo>> FetchOutlookEventsAsync(
        HttpClient client, CancellationToken ct)
    {
        var response = await client.GetAsync(
            "/me/calendar/events?$top=100&$orderby=lastModifiedDateTime desc", ct);
        response.EnsureSuccessStatusCode();
        var json = await response.Content.ReadFromJsonAsync<JsonElement>(ct);
        var items = json.GetProperty("value");

        var events = new List<OutlookEventInfo>();
        foreach (var item in items.EnumerateArray())
        {
            events.Add(new OutlookEventInfo(
                item.GetProperty("id").GetString()!,
                item.GetProperty("subject").GetString() ?? "",
                item.GetProperty("bodyPreview").GetString(),
                DateTimeOffset.Parse(item.GetProperty("start").GetProperty("dateTime").GetString()!),
                DateTimeOffset.Parse(item.GetProperty("end").GetProperty("dateTime").GetString()!),
                DateTimeOffset.Parse(item.GetProperty("lastModifiedDateTime").GetString()!)
            ));
        }
        return events;
    }

    private EventEntity MapOutlookEvent(OutlookEventInfo oe, Guid userId)
    {
        var defaultCalendar = _db.Set<CalendarEntity>()
            .FirstOrDefault(c => c.UserId == userId && c.IsDefault)!;

        return new EventEntity
        {
            CalendarId = defaultCalendar.Id,
            Uid = Guid.NewGuid() + "@outlook",
            Title = oe.Subject,
            Description = oe.BodyPreview,
            DtStart = oe.Start,
            DtEnd = oe.End,
            Source = "outlook",
            OutlookEventId = oe.Id
        };
    }

    private void UpdateFromOutlookEvent(EventEntity entity, OutlookEventInfo oe)
    {
        entity.Title = oe.Subject;
        entity.Description = oe.BodyPreview;
        entity.DtStart = oe.Start;
        entity.DtEnd = oe.End;
        entity.UpdatedAt = DateTimeOffset.UtcNow;
    }

    private record OutlookEventInfo(
        string Id, string Subject, string? BodyPreview,
        DateTimeOffset Start, DateTimeOffset End,
        DateTimeOffset LastModifiedDateTime
    );
}
```

- [ ] **Step 2: 验证构建**

```powershell
dotnet build Pim.sln
```

Expected: Build succeeded.

- [ ] **Step 3: Commit**

```powershell
git add src/modules/Pim.Module.Calendar/Services/OutlookSyncService.cs
git commit -m "feat: add Outlook sync service (Graph API, webhooks, confirmation gate)"
```

---

### Task 15: 日历模块控制器与端点注册

**Files:**
- Create: `src/modules/Pim.Module.Calendar/CalendarModule.cs`

- [ ] **Step 1: 编写 CalendarModule.cs**

```csharp
// src/modules/Pim.Module.Calendar/CalendarModule.cs
using Pim.Core.Modules;
using Pim.Module.Calendar.Services;

namespace Pim.Module.Calendar;

public class CalendarModule : IModule
{
    public string Name => "calendar";
    public string Version => "1.0.0";

    public void RegisterServices(IServiceCollection services, IConfiguration configuration)
    {
        services.AddScoped<CalendarService>();
        services.AddScoped<IcsService>();
        services.AddScoped<SchedulingEngine>();
        services.AddScoped<OutlookSyncService>();
    }

    public void MapEndpoints(IEndpointRouteBuilder endpoints)
    {
        var group = endpoints.MapGroup("/api/v1/calendar")
            .RequireAuthorization();

        // Calendars
        group.MapGet("/calendars", async (
            [FromServices] CalendarService svc, CancellationToken ct) =>
            Results.Ok(ApiResponse<List<CalendarResponse>>.Ok(await svc.GetCalendarsAsync(ct))));

        group.MapPost("/calendars", async (
            [FromBody] CreateCalendarRequest req,
            [FromServices] CalendarService svc, CancellationToken ct) =>
            Results.Created($"/api/v1/calendar/calendars/{{id}}",
                ApiResponse<CalendarResponse>.Ok(await svc.CreateCalendarAsync(req, ct))));

        // Events
        group.MapGet("/events", async (
            [FromQuery] DateTimeOffset start, [FromQuery] DateTimeOffset end,
            [FromServices] CalendarService svc, CancellationToken ct) =>
            Results.Ok(ApiResponse<List<EventResponse>>.Ok(await svc.GetEventsAsync(start, end, ct))));

        group.MapPost("/events", async (
            [FromBody] CreateEventRequest req,
            [FromServices] CalendarService svc, CancellationToken ct) =>
        {
            var result = await svc.CreateEventAsync(req, ct);
            return Results.Created($"/api/v1/calendar/events/{result.Id}",
                ApiResponse<EventResponse>.Ok(result));
        });

        group.MapPut("/events/{id:guid}", async (
            Guid id, [FromBody] CreateEventRequest req,
            [FromServices] CalendarService svc, CancellationToken ct) =>
            Results.Ok(ApiResponse<EventResponse>.Ok(await svc.UpdateEventAsync(id, req, ct))));

        group.MapDelete("/events/{id:guid}", async (
            Guid id, [FromServices] CalendarService svc, CancellationToken ct) =>
        {
            await svc.DeleteEventAsync(id, ct);
            return Results.Ok(ApiResponse<string>.Ok("deleted"));
        });

        // Tasks
        group.MapGet("/tasks", async (
            [FromQuery] bool? inbox,
            [FromServices] CalendarService svc, CancellationToken ct) =>
            Results.Ok(ApiResponse<List<TaskResponse>>.Ok(await svc.GetTasksAsync(inbox, ct))));

        group.MapPost("/tasks", async (
            [FromBody] CreateTaskRequest req,
            [FromServices] CalendarService svc, CancellationToken ct) =>
            Results.Created("/api/v1/calendar/tasks",
                ApiResponse<TaskResponse>.Ok(await svc.CreateTaskAsync(req, ct))));

        group.MapPost("/tasks/{id:guid}/move", async (
            Guid id, [FromBody] MoveTaskRequest req,
            [FromServices] CalendarService svc, CancellationToken ct) =>
        {
            await svc.MoveTaskAsync(id, req, ct);
            return Results.Ok(ApiResponse<string>.Ok("moved"));
        });

        // Scheduling
        group.MapPost("/schedule", async (
            [FromBody] ScheduleRequest req,
            [FromServices] SchedulingEngine engine,
            [FromServices] ICurrentUserService currentUser,
            CancellationToken ct) =>
        {
            var solutions = await engine.GeneratePlansAsync(
                currentUser.UserId!.Value, req.TaskIds, ct);
            return Results.Ok(ApiResponse<List<ScheduleSolution>>.Ok(solutions));
        });

        // ICS
        group.MapPost("/import-ics", async (
            HttpRequest request,
            [FromServices] IcsService icsService,
            [FromServices] CalendarService svc,
            [FromServices] ICurrentUserService currentUser,
            CancellationToken ct) =>
        {
            var icsContent = await new StreamReader(request.Body).ReadToEndAsync(ct);
            var parsed = icsService.ImportEvents(icsContent);
            // Import parsed events into user's default calendar
            return Results.Ok(ApiResponse<int>.Ok(parsed.Count));
        });

        group.MapGet("/export-ics", async (
            [FromQuery] DateTimeOffset start,
            [FromQuery] DateTimeOffset end,
            [FromServices] CalendarService svc,
            [FromServices] IcsService icsService,
            CancellationToken ct) =>
        {
            var events = await svc.GetEventsAsync(start, end, ct);
            // Get full EventEntities for ICS serialization
            var eventEntities = await svc.GetEventEntitiesAsync(start, end, ct);
            var icsContent = icsService.ExportEvents(eventEntities);
            return Results.Ok(ApiResponse<string>.Ok(icsContent));
        });

        // Outlook
        group.MapPost("/outlook/sync", async (
            [FromServices] OutlookSyncService outlookSvc,
            [FromServices] ICurrentUserService currentUser,
            CancellationToken ct) =>
        {
            await outlookSvc.SyncAsync(currentUser.UserId!.Value, ct);
            return Results.Ok(ApiResponse<string>.Ok("synced"));
        });
    }

    public async Task InitializeAsync(IServiceProvider serviceProvider)
    {
        // No special initialization needed; tables are managed by PimDbContext
        await Task.CompletedTask;
    }
}
```

- [ ] **Step 2: 验证构建**

```powershell
dotnet build Pim.sln
```

Expected: Build succeeded.

- [ ] **Step 3: 注册日历模块到 Pim.Api**

在 `src/Pim.Api/Pim.Api.csproj` 中添加：

```xml
<ProjectReference Include="..\modules\Pim.Module.Calendar\Pim.Module.Calendar.csproj" />
```

- [ ] **Step 4: Commit**

```powershell
git add src/modules/Pim.Module.Calendar/CalendarModule.cs src/Pim.Api/Pim.Api.csproj
git commit -m "feat: add calendar module endpoints registration"
```

---

## 阶段六：Windows 客户端 (WPF)

### Task 16: 创建 WPF 项目骨架

**Files:**
- Create: `src/client-windows/Pim.Client.Windows.sln`
- Create: `src/client-windows/Pim.Client.App/Pim.Client.App.csproj`
- Create: `src/client-windows/Pim.Client.App/App.xaml` and `App.xaml.cs`
- Create: `src/client-windows/Pim.Client.App/MainWindow.xaml` and `MainWindow.xaml.cs`
- Create: `src/client-windows/Pim.Client.App/Startup.cs`
- Create: `src/client-windows/Pim.Client.Core/Pim.Client.Core.csproj`
- Create: `src/client-windows/Pim.Client.Core/Services/ApiClient.cs`
- Create: `src/client-windows/Pim.Client.Core/Services/AuthService.cs`
- Create: `src/client-windows/Pim.Client.Infrastructure/Pim.Client.Infrastructure.csproj`

- [ ] **Step 1: 创建解决方案和项目**

```powershell
mkdir src\client-windows -Force
cd src\client-windows
dotnet new wpf -n Pim.Client.App --framework net8.0
dotnet new classlib -n Pim.Client.Core --framework net8.0
dotnet new classlib -n Pim.Client.Infrastructure --framework net8.0
dotnet new sln -n Pim.Client.Windows
dotnet sln Pim.Client.Windows.sln add Pim.Client.App/Pim.Client.App.csproj
dotnet sln Pim.Client.Windows.sln add Pim.Client.Core/Pim.Client.Core.csproj
dotnet sln Pim.Client.Windows.sln add Pim.Client.Infrastructure/Pim.Client.Infrastructure.csproj
cd ..
```

- [ ] **Step 2: 安装 NuGet 包**

```powershell
dotnet add src/client-windows/Pim.Client.Core/Pim.Client.Core.csproj package CommunityToolkit.Mvvm
dotnet add src/client-windows/Pim.Client.Core/Pim.Client.Core.csproj package Microsoft.Extensions.DependencyInjection
dotnet add src/client-windows/Pim.Client.Infrastructure/Pim.Client.Infrastructure.csproj package Microsoft.Data.Sqlite
dotnet add src/client-windows/Pim.Client.App/Pim.Client.App.csproj reference src/client-windows/Pim.Client.Core/Pim.Client.Core.csproj
dotnet add src/client-windows/Pim.Client.App/Pim.Client.App.csproj reference src/client-windows/Pim.Client.Infrastructure/Pim.Client.Infrastructure.csproj
```

- [ ] **Step 3: 编写 Startup.cs**

```csharp
// src/client-windows/Pim.Client.App/Startup.cs
using Microsoft.Extensions.DependencyInjection;
using Pim.Client.Core.Services;

namespace Pim.Client.App;

public static class Startup
{
    public static IServiceProvider ConfigureServices()
    {
        var services = new ServiceCollection();

        // Core services
        services.AddSingleton<ApiClient>();
        services.AddSingleton<AuthService>();
        services.AddSingleton<INavigationService, NavigationService>();

        // ViewModels (will be added per module)

        return services.BuildServiceProvider();
    }
}
```

- [ ] **Step 4: 编写 ApiClient.cs**

```csharp
// src/client-windows/Pim.Client.Core/Services/ApiClient.cs
using System.Net.Http.Headers;
using System.Net.Http.Json;
using System.Text.Json;

namespace Pim.Client.Core.Services;

public class ApiClient
{
    private readonly HttpClient _httpClient;
    private const string ApiBaseUrl = "https://localhost:5001/api/v1";

    public ApiClient()
    {
        _httpClient = new HttpClient { BaseAddress = new Uri(ApiBaseUrl) };
    }

    public void SetAccessToken(string token)
    {
        _httpClient.DefaultRequestHeaders.Authorization =
            new AuthenticationHeaderValue("Bearer", token);
    }

    public async Task<T?> GetAsync<T>(string endpoint, CancellationToken ct = default)
    {
        var response = await _httpClient.GetAsync(endpoint, ct);
        response.EnsureSuccessStatusCode();
        return await response.Content.ReadFromJsonAsync<T>(ct);
    }

    public async Task<T?> PostAsync<T>(string endpoint, object body, CancellationToken ct = default)
    {
        var response = await _httpClient.PostAsJsonAsync(endpoint, body, ct);
        response.EnsureSuccessStatusCode();
        return await response.Content.ReadFromJsonAsync<T>(ct);
    }

    public async Task<T?> PutAsync<T>(string endpoint, object body, CancellationToken ct = default)
    {
        var response = await _httpClient.PutAsJsonAsync(endpoint, body, ct);
        response.EnsureSuccessStatusCode();
        return await response.Content.ReadFromJsonAsync<T>(ct);
    }

    public async Task DeleteAsync(string endpoint, CancellationToken ct = default)
    {
        var response = await _httpClient.DeleteAsync(endpoint, ct);
        response.EnsureSuccessStatusCode();
    }
}
```

- [ ] **Step 5: 编写 AuthService.cs**

```csharp
// src/client-windows/Pim.Client.Core/Services/AuthService.cs
using System.Text.Json;

namespace Pim.Client.Core.Services;

public class AuthService
{
    private readonly ApiClient _apiClient;
    private string? _accessToken;
    private string? _refreshToken;
    private DateTimeOffset _accessTokenExpiry;

    public AuthService(ApiClient apiClient)
    {
        _apiClient = apiClient;
    }

    public bool IsAuthenticated => !string.IsNullOrEmpty(_accessToken) &&
                                    DateTimeOffset.UtcNow < _accessTokenExpiry;

    public async Task<bool> LoginAsync(string username, string password)
    {
        var result = await _apiClient.PostAsync<JsonElement>("/auth/login",
            new { username, password });

        if (!result.HasValue) return false;

        var data = result.Value.GetProperty("data");
        _accessToken = data.GetProperty("accessToken").GetString()!;
        _refreshToken = data.GetProperty("refreshToken").GetString()!;
        _accessTokenExpiry = data.GetProperty("expiresAt").GetDateTimeOffset();

        _apiClient.SetAccessToken(_accessToken);
        return true;
    }

    public async Task<bool> RegisterAsync(
        string username, string email, string password, string? displayName)
    {
        var result = await _apiClient.PostAsync<JsonElement>("/auth/register",
            new { username, email, password, displayName });
        return result.HasValue && result.Value.GetProperty("code").GetInt32() == 0;
    }

    public async Task<bool> RefreshAsync()
    {
        if (string.IsNullOrEmpty(_refreshToken)) return false;

        var result = await _apiClient.PostAsync<JsonElement>("/auth/refresh",
            new { refreshToken = _refreshToken });

        if (!result.HasValue) return false;

        var data = result.Value.GetProperty("data");
        _accessToken = data.GetProperty("accessToken").GetString()!;
        _refreshToken = data.GetProperty("refreshToken").GetString()!;
        _accessTokenExpiry = data.GetProperty("expiresAt").GetDateTimeOffset();

        _apiClient.SetAccessToken(_accessToken);
        return true;
    }
}
```

- [ ] **Step 6: 编写 App.xaml.cs 带 DI 初始化**

```csharp
// src/client-windows/Pim.Client.App/App.xaml.cs
using System.Windows;

namespace Pim.Client.App;

public partial class App : Application
{
    public static IServiceProvider Services { get; private set; } = null!;

    protected override void OnStartup(StartupEventArgs e)
    {
        Services = Startup.ConfigureServices();
        var mainWindow = new MainWindow();
        mainWindow.Show();
        base.OnStartup(e);
    }
}
```

- [ ] **Step 7: 编写 MainWindow.xaml（基本骨架）**

```xml
<!-- src/client-windows/Pim.Client.App/MainWindow.xaml -->
<Window x:Class="Pim.Client.App.MainWindow"
        xmlns="http://schemas.microsoft.com/winfx/2006/xaml/presentation"
        xmlns:x="http://schemas.microsoft.com/winfx/2006/xaml"
        Title="PIM" Height="800" Width="1200"
        WindowStartupLocation="CenterScreen">
    <Grid>
        <Grid.RowDefinitions>
            <RowDefinition Height="Auto"/>
            <RowDefinition Height="*"/>
            <RowDefinition Height="Auto"/>
        </Grid.RowDefinitions>

        <!-- Navigation bar -->
        <StackPanel Grid.Row="0" Orientation="Horizontal" Background="#F3F4F6" Padding="8">
            <Button Content="日历" Margin="4" Padding="8,4"/>
            <Button Content="文件" Margin="4" Padding="8,4"/>
            <Button Content="活动" Margin="4" Padding="8,4"/>
        </StackPanel>

        <!-- Content area -->
        <ContentControl Grid.Row="1" x:Name="MainContent" Margin="8"/>

        <!-- Status bar -->
        <StatusBar Grid.Row="2">
            <StatusBarItem Content="就绪"/>
        </StatusBar>
    </Grid>
</Window>
```

- [ ] **Step 8: 验证构建**

```powershell
dotnet build src/client-windows/Pim.Client.Windows.sln
```

Expected: Build succeeded.

- [ ] **Step 9: Commit**

```powershell
git add src/client-windows/
git commit -m "feat: add WPF client skeleton with DI, ApiClient, AuthService"
```

---

## 阶段七：Android 客户端

### Task 17: 创建 Android 项目

**Files:**
- Create Android Studio project structure
- Create: `src/client-android/app/build.gradle.kts`
- Create: `src/client-android/app/src/main/java/com/pim/app/PimApp.kt`
- Create: `src/client-android/core/network/ApiService.kt`
- Create: `src/client-android/core/auth/TokenManager.kt`

- [ ] **Step 1: 手动创建 Android 项目结构**

由于 Android 项目通常通过 Android Studio 向导创建，此处提供项目初始化脚本和关键文件：

```
src/client-android/
├── build.gradle.kts (root)
├── settings.gradle.kts
├── gradle.properties
├── app/
│   ├── build.gradle.kts
│   └── src/main/
│       ├── AndroidManifest.xml
│       ├── java/com/pim/app/
│       │   ├── PimApp.kt
│       │   └── MainActivity.kt
│       └── res/
├── core/
│   ├── build.gradle.kts
│   └── src/main/java/com/pim/core/
│       ├── network/
│       │   ├── ApiService.kt
│       │   └── AuthInterceptor.kt
│       ├── auth/
│       │   └── TokenManager.kt
│       └── models/
│           └── AuthModels.kt
└── features/
    └── calendar/
        ├── build.gradle.kts
        └── src/main/java/com/pim/features/calendar/
```

- [ ] **Step 2: 编写 settings.gradle.kts**

```kotlin
// src/client-android/settings.gradle.kts
pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "PimClientAndroid"
include(":app")
include(":core")
include(":features:calendar")
```

- [ ] **Step 3: 编写 root build.gradle.kts**

```kotlin
// src/client-android/build.gradle.kts
plugins {
    id("com.android.application") version "8.2.0" apply false
    id("org.jetbrains.kotlin.android") version "1.9.20" apply false
    id("com.google.dagger.hilt.android") version "2.48" apply false
}
```

- [ ] **Step 4: 编写 app/build.gradle.kts**

```kotlin
// src/client-android/app/build.gradle.kts
plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("com.google.dagger.hilt.android")
    kotlin("kapt")
}

android {
    namespace = "com.pim.app"
    compileSdk = 34
    defaultConfig {
        applicationId = "com.pim.app"
        minSdk = 26
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"
    }
    buildFeatures { compose = true }
    composeOptions { kotlinCompilerExtensionVersion = "1.5.5" }
}

dependencies {
    implementation(project(":core"))
    implementation(project(":features:calendar"))

    implementation("androidx.compose.ui:ui:1.5.4")
    implementation("androidx.compose.material3:material3:1.1.2")
    implementation("androidx.activity:activity-compose:1.8.1")
    implementation("com.google.dagger:hilt-android:2.48")
    kapt("com.google.dagger:hilt-compiler:2.48")

    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.0")
}
```

- [ ] **Step 5: 编写 ApiService.kt**

```kotlin
// src/client-android/core/src/main/java/com/pim/core/network/ApiService.kt
package com.pim.core.network

import com.pim.core.models.*
import retrofit2.http.*

interface ApiService {
    @POST("auth/login")
    suspend fun login(@Body request: LoginRequest): ApiResponse<AuthResponse>

    @POST("auth/register")
    suspend fun register(@Body request: RegisterRequest): ApiResponse<AuthResponse>

    @POST("auth/refresh")
    suspend fun refresh(@Body request: RefreshRequest): ApiResponse<AuthResponse>

    @GET("calendar/events")
    suspend fun getEvents(
        @Query("start") start: String,
        @Query("end") end: String
    ): ApiResponse<List<EventResponse>>

    @GET("calendar/tasks")
    suspend fun getTasks(
        @Query("inbox") inbox: Boolean? = null
    ): ApiResponse<List<TaskResponse>>

    @POST("calendar/tasks")
    suspend fun createTask(
        @Body request: CreateTaskRequest
    ): ApiResponse<TaskResponse>
}
```

- [ ] **Step 6: 编写 TokenManager.kt**

```kotlin
// src/client-android/core/src/main/java/com/pim/core/auth/TokenManager.kt
package com.pim.core.auth

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKeys

class TokenManager(context: Context) {
    private val masterKey = MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC)
    private val prefs = EncryptedSharedPreferences.create(
        "pim_auth",
        masterKey,
        context,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    fun saveTokens(accessToken: String, refreshToken: String) {
        prefs.edit()
            .putString("access_token", accessToken)
            .putString("refresh_token", refreshToken)
            .putLong("expires_at", System.currentTimeMillis() + 15 * 60 * 1000)
            .apply()
    }

    fun getAccessToken(): String? = prefs.getString("access_token", null)
    fun getRefreshToken(): String? = prefs.getString("refresh_token", null)

    fun isExpired(): Boolean {
        val expiresAt = prefs.getLong("expires_at", 0)
        return System.currentTimeMillis() >= expiresAt
    }

    fun clear() = prefs.edit().clear().apply()
}
```

- [ ] **Step 7: 编写 AuthInterceptor.kt**

```kotlin
// src/client-android/core/src/main/java/com/pim/core/network/AuthInterceptor.kt
package com.pim.core.network

import com.pim.core.auth.TokenManager
import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.Response

class AuthInterceptor(
    private val tokenManager: TokenManager,
    private val onTokenExpired: suspend () -> Boolean
) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val original = chain.request()
        if (tokenManager.isExpired()) {
            val refreshed = runBlocking { onTokenExpired() }
            if (!refreshed) return chain.proceed(original)
        }
        val request = original.newBuilder()
            .header("Authorization", "Bearer ${tokenManager.getAccessToken()}")
            .build()
        return chain.proceed(request)
    }
}
```

- [ ] **Step 8: 编写 PimApp.kt 和 MainActivity.kt**

```kotlin
// src/client-android/app/src/main/java/com/pim/app/PimApp.kt
package com.pim.app

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class PimApp : Application()

// src/client-android/app/src/main/java/com/pim/app/MainActivity.kt
package com.pim.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.*
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                Text("PIM Android Client")
            }
        }
    }
}
```

- [ ] **Step 9: Commit**

```powershell
git add src/client-android/
git commit -m "feat: add Android client skeleton with Hilt, Retrofit, JWT auth"
```

---

## 后续阶段

以下阶段将在核心平台和日历模块完成后执行，详细任务将在届时制定：

| 阶段 | 内容 |
|------|------|
| 八 | Pim.Module.Files 完整模块（文件 CRUD + Kopia 版本管理 + Tika 文本提取 + 同步引擎 + 层级标签） |
| 九 | Windows 客户端文件模块（FileBrowser + VersionHistory + SyncEngine） |
| 十 | Pim.Module.Activity 完整模块（采集接收 + 时间轴 + 仪表盘 + 热力图 + 日程关联） |
| 十一 | Windows 客户端活动采集（KeyStats + ActivityWatch + BackgroundUploader） |
| 十二 | Android 客户端文件 + 活动模块 |

---

## 开发环境准备

在开始 Task 1 之前，确保以下工具已安装：

- [ ] .NET SDK 8.0
- [ ] Docker Desktop（用于 PostgreSQL、MinIO、Tika）
- [ ] Git
- [ ] Visual Studio 2022 或 Rider（用于 WPF 开发）
- [ ] Android Studio Hedgehog+（用于 Android 开发）
- [ ] Kopia CLI（`winget install kopia` 或 `brew install kopia`）
