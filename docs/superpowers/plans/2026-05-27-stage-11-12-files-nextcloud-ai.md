# Stage 11/12 Files Nextcloud AI Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build a Nextcloud-backed file console with stable file/version references, current-version indexing, Qdrant search, and AI summaries/tags/suggestions through the unified `IAiGateway`.

**Architecture:** Add a new `Pim.Module.Files` module that stores PIM metadata in PostgreSQL and delegates content operations to a provider-neutral `IFileProviderAdapter`. Nextcloud remains the source of truth for contents, folders, trash, version history, sharing, and OnlyOffice integration; PIM stores metadata, index state, AI results, suggestions, and audit records. Current-version file text is extracted locally, chunked, embedded locally, upserted to Qdrant, and sent to the unified AI gateway only as controlled evidence snippets.

**Tech Stack:** .NET 8 minimal APIs, EF Core/Npgsql, Hangfire, `HttpClient`, WebDAV XML parsing, Apache Tika, Qdrant HTTP API, React 19, TanStack Query, Vite, TypeScript.

---

## Scope Check

The design spans infrastructure, backend file operations, indexing/AI, and Web UI. Keep it as one module delivery because the file console is not useful without provider binding and basic browsing, and the AI layer depends on the same metadata model. The task sequence below still produces independently testable commits: module skeleton, persistence, provider binding, adapter, file operations, indexing/search, AI, Web UI, and compose wiring.

This plan assumes the unified LLM gateway stage has landed before Task 7. If `src/Pim.Core/Ai/IAiGateway.cs` is missing when Task 7 starts, merge or implement the gateway stage first; do not create direct LiteLLM/OpenAI calls in the files module.

Official Nextcloud WebDAV references used for endpoint details:
- Basic file operations: `https://docs.nextcloud.com/server/latest/developer_manual/client_apis/WebDAV/basic.html`
- Trashbin: `https://docs.nextcloud.com/server/22/developer_manual/client_apis/WebDAV/trashbin.html`
- Versions: `https://docs.nextcloud.com/server/22/developer_manual/client_apis/WebDAV/versions.html`
- User WebDAV URL and app password guidance: `https://docs.nextcloud.com/server/stable/user_manual/en/files/access_webdav.html`

## File Structure

Create backend module:
- `src/modules/Pim.Module.Files/Pim.Module.Files.csproj`: module project referenced by `Pim.Api` build output and tests.
- `src/modules/Pim.Module.Files/FilesModule.cs`: service registration and `/api/v1/files` endpoint mapping.
- `src/modules/Pim.Module.Files/DTOs/FileDtos.cs`: request/response records shared by module endpoints and services.
- `src/modules/Pim.Module.Files/Entities/FileProviderEntity.cs`: per-user provider binding.
- `src/modules/Pim.Module.Files/Entities/FileItemEntity.cs`: current file/folder metadata.
- `src/modules/Pim.Module.Files/Entities/FileVersionEntity.cs`: current and historical version metadata.
- `src/modules/Pim.Module.Files/Entities/FileIndexJobEntity.cs`: indexing job state.
- `src/modules/Pim.Module.Files/Entities/FileChunkEntity.cs`: current-version text chunks.
- `src/modules/Pim.Module.Files/Entities/FileAiResultEntity.cs`: AI summaries/tags.
- `src/modules/Pim.Module.Files/Entities/FileSuggestionEntity.cs`: reviewable suggestions.
- `src/modules/Pim.Module.Files/Entities/FileEntityConfigurations.cs`: EF table mapping, defaults, indexes, relationships.
- `src/modules/Pim.Module.Files/Providers/IFileProviderAdapter.cs`: provider-neutral adapter contract.
- `src/modules/Pim.Module.Files/Providers/NextcloudFileProviderAdapter.cs`: Nextcloud WebDAV implementation.
- `src/modules/Pim.Module.Files/Providers/NextcloudDavXmlParser.cs`: XML multi-status parser.
- `src/modules/Pim.Module.Files/Providers/NextcloudOptions.cs`: public/internal URL and timeout options.
- `src/modules/Pim.Module.Files/Services/FileProviderBindingService.cs`: bind/test/sync providers.
- `src/modules/Pim.Module.Files/Services/FileOperationService.cs`: browse and file operation orchestration.
- `src/modules/Pim.Module.Files/Services/FileIndexingService.cs`: extract, chunk, embed, Qdrant, AI orchestration.
- `src/modules/Pim.Module.Files/Services/FileChunker.cs`: deterministic chunking.
- `src/modules/Pim.Module.Files/Services/IFileEmbeddingService.cs`: local embedding boundary.
- `src/modules/Pim.Module.Files/Services/HashingFileEmbeddingService.cs`: deterministic local embedding implementation for first version.
- `src/modules/Pim.Module.Files/Services/QdrantFileVectorStore.cs`: Qdrant collection upsert/search/delete.
- `src/modules/Pim.Module.Files/Services/FileAiService.cs`: `IAiGateway` calls for summaries, tags, and suggestions.

Modify backend/infrastructure:
- `Pim.sln`: add `Pim.Module.Files` project and nest it under `modules`.
- `tests/Pim.UnitTests/Pim.UnitTests.csproj`: add project reference to `Pim.Module.Files`.
- `src/Pim.Infrastructure/Extensions/ServiceCollectionExtensions.cs`: register data protection and secret protection.
- `src/Pim.Infrastructure/Pim.Infrastructure.csproj`: add `Microsoft.AspNetCore.DataProtection.EntityFrameworkCore` only if key persistence through EF is chosen; otherwise no package is needed because `Microsoft.AspNetCore.App` covers data protection for the API runtime.
- `src/Pim.Infrastructure/Secrets/ISecretProtector.cs`: protect/unprotect server-side secrets.
- `src/Pim.Infrastructure/Secrets/DataProtectionSecretProtector.cs`: ASP.NET data protection implementation.
- `src/Pim.Api/appsettings.json`: add `Nextcloud`, `Qdrant`, `Embedding`, and `Files` defaults.
- `src/Pim.Api/appsettings.Development.json`: add local development defaults.
- `.env.example`: add Nextcloud, OnlyOffice, Qdrant, and LiteLLM values.
- `docker-compose.yml`: add Nextcloud, Nextcloud Postgres, Redis, OnlyOffice Docs, Qdrant, and LiteLLM environment wiring.
- `src/Pim.Infrastructure/Data/Migrations/<timestamp>_AddFilesModule.cs`: EF migration.
- `src/Pim.Infrastructure/Data/Migrations/PimDbContextModelSnapshot.cs`: generated model snapshot.

Create backend tests:
- `tests/Pim.UnitTests/Files/FileEndpointPathTests.cs`
- `tests/Pim.UnitTests/Files/FileModelTests.cs`
- `tests/Pim.UnitTests/Files/FileProviderBindingServiceTests.cs`
- `tests/Pim.UnitTests/Files/NextcloudDavXmlParserTests.cs`
- `tests/Pim.UnitTests/Files/NextcloudFileProviderAdapterTests.cs`
- `tests/Pim.UnitTests/Files/FileOperationServiceTests.cs`
- `tests/Pim.UnitTests/Files/FileChunkerTests.cs`
- `tests/Pim.UnitTests/Files/FileIndexingServiceTests.cs`
- `tests/Pim.UnitTests/Files/QdrantFileVectorStoreTests.cs`
- `tests/Pim.UnitTests/Files/FileAiServiceTests.cs`

Create/modify Web:
- `src/client-web/src/api/files.ts`: file API client and stable path builders.
- `src/client-web/src/types/index.ts`: file DTO TypeScript types.
- `src/client-web/src/pages/FilesPage.tsx`: `/files` control console.
- `src/client-web/src/layout/AppLayout.tsx`: lazy route for `/files`.
- `src/client-web/src/layout/Sidebar.tsx`: sidebar entry for files.
- `tests/client-web/filesApiPath.test.ts`: file path builder tests.
- `tests/client-web/filesTypes.test.ts`: TypeScript DTO compilation smoke test.
- `tests/client-web/tsconfig.files.json`: test TypeScript config.
- `src/client-web/package.json`: add `test:files` script.

## Task 1: Module Project And Endpoint Contract

**Files:**
- Create: `src/modules/Pim.Module.Files/Pim.Module.Files.csproj`
- Create: `src/modules/Pim.Module.Files/FilesModule.cs`
- Create: `src/modules/Pim.Module.Files/DTOs/FileDtos.cs`
- Modify: `Pim.sln`
- Modify: `tests/Pim.UnitTests/Pim.UnitTests.csproj`
- Create: `tests/Pim.UnitTests/Files/FileEndpointPathTests.cs`

- [ ] **Step 1: Write failing endpoint path tests**

```csharp
using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.Builder;
using Microsoft.AspNetCore.Routing;
using Microsoft.Extensions.DependencyInjection;
using Pim.Module.Files;
using Xunit;

namespace Pim.UnitTests.Files;

public class FileEndpointPathTests
{
    [Fact]
    public void FileEndpointPaths_AreStable()
    {
        Assert.Equal("/api/v1/files", FileEndpointPaths.Root);
        Assert.Equal("/api/v1/files/providers", FileEndpointPaths.Providers);
        Assert.Equal("/api/v1/files/providers/nextcloud", FileEndpointPaths.NextcloudProviders);
        Assert.Equal("/api/v1/files/providers/11111111-1111-1111-1111-111111111111/test", FileEndpointPaths.ProviderTest("11111111-1111-1111-1111-111111111111"));
        Assert.Equal("/api/v1/files/items/22222222-2222-2222-2222-222222222222/download", FileEndpointPaths.ItemDownload("22222222-2222-2222-2222-222222222222"));
        Assert.Equal("/api/v1/files/items/22222222-2222-2222-2222-222222222222/versions/33333333-3333-3333-3333-333333333333/restore", FileEndpointPaths.VersionRestore("22222222-2222-2222-2222-222222222222", "33333333-3333-3333-3333-333333333333"));
    }

    [Fact]
    public async Task MapEndpoints_RegistersAuthorizedRoutes()
    {
        var builder = WebApplication.CreateBuilder();
        builder.Services.AddAuthorization();
        using var app = builder.Build();

        new FilesModule().MapEndpoints(app);
        await app.StartAsync();

        var routeEndpoints = app.Services
            .GetRequiredService<EndpointDataSource>()
            .Endpoints
            .OfType<RouteEndpoint>()
            .ToLookup(endpoint => NormalizeRoute(endpoint.RoutePattern.RawText ?? string.Empty));

        var expectedRoutes = new[]
        {
            "/api/v1/files/providers",
            "/api/v1/files/providers/nextcloud",
            "/api/v1/files/providers/{id:guid}/test",
            "/api/v1/files/providers/{id:guid}/sync",
            "/api/v1/files/items",
            "/api/v1/files/items/{id:guid}",
            "/api/v1/files/items/upload",
            "/api/v1/files/items/{id:guid}/download",
            "/api/v1/files/items/{id:guid}/move",
            "/api/v1/files/items/{id:guid}/rename",
            "/api/v1/files/items/{id:guid}",
            "/api/v1/files/trash",
            "/api/v1/files/trash/{id:guid}/restore",
            "/api/v1/files/items/{id:guid}/versions",
            "/api/v1/files/items/{id:guid}/versions/{versionId:guid}/download",
            "/api/v1/files/items/{id:guid}/versions/{versionId:guid}/restore-preview",
            "/api/v1/files/items/{id:guid}/versions/{versionId:guid}/restore",
            "/api/v1/files/items/{id:guid}/index",
            "/api/v1/files/search",
            "/api/v1/files/suggestions",
            "/api/v1/files/suggestions/{id:guid}/dismiss",
            "/api/v1/files/suggestions/{id:guid}/accept",
            "/api/v1/files/items/{id:guid}/open-link"
        };

        foreach (var expectedRoute in expectedRoutes)
        {
            var endpoints = routeEndpoints[expectedRoute].ToList();
            Assert.True(endpoints.Count > 0, $"Missing route: {expectedRoute}");
            Assert.All(endpoints, endpoint => Assert.NotNull(endpoint.Metadata.GetMetadata<IAuthorizeData>()));
        }
    }

    private static string NormalizeRoute(string route)
        => route.Length > 1 ? route.TrimEnd('/') : route;
}
```

- [ ] **Step 2: Run the failing test**

Run: `dotnet test tests/Pim.UnitTests/Pim.UnitTests.csproj --filter FullyQualifiedName~FileEndpointPathTests -v minimal`

Expected: FAIL because `Pim.Module.Files` and `FileEndpointPaths` do not exist.

- [ ] **Step 3: Create the module project**

```xml
<Project Sdk="Microsoft.NET.Sdk">
  <ItemGroup>
    <FrameworkReference Include="Microsoft.AspNetCore.App" />
  </ItemGroup>

  <ItemGroup>
    <ProjectReference Include="..\..\Pim.Core\Pim.Core.csproj" />
    <ProjectReference Include="..\..\Pim.Infrastructure\Pim.Infrastructure.csproj" />
  </ItemGroup>

  <PropertyGroup>
    <TargetFramework>net8.0</TargetFramework>
    <ImplicitUsings>enable</ImplicitUsings>
    <Nullable>enable</Nullable>
  </PropertyGroup>
</Project>
```

- [ ] **Step 4: Add initial DTO records**

Create `src/modules/Pim.Module.Files/DTOs/FileDtos.cs`:

```csharp
using Pim.Core.Common;

namespace Pim.Module.Files.DTOs;

public sealed record FileProviderDto(
    Guid Id,
    string Provider,
    string BaseUrl,
    string? InternalBaseUrl,
    string Username,
    string Status,
    DateTimeOffset? LastSyncAt,
    string? LastError,
    DateTimeOffset CreatedAt,
    DateTimeOffset UpdatedAt);

public sealed record BindNextcloudProviderRequest(
    string BaseUrl,
    string? InternalBaseUrl,
    string Username,
    string AppPassword);

public sealed record FileProviderTestDto(bool Success, string Status, string? ErrorMessage);

public sealed record FileItemDto(
    Guid Id,
    Guid ProviderId,
    string ExternalFileId,
    string? ParentExternalFileId,
    string Path,
    string Name,
    string ItemType,
    string? MimeType,
    long? Size,
    string? Etag,
    string? ContentHash,
    Guid? CurrentVersionId,
    string? Permissions,
    bool IsDeleted,
    DateTimeOffset? DeletedAt,
    DateTimeOffset? LastSeenAt,
    DateTimeOffset CreatedAt,
    DateTimeOffset ModifiedAt,
    DateTimeOffset SyncedAt,
    string IndexStatus,
    FileAiResultDto? Ai);

public sealed record FileVersionDto(
    Guid Id,
    Guid FileItemId,
    string ExternalVersionId,
    string? Etag,
    long? Size,
    DateTimeOffset ModifiedAt,
    string Source,
    bool IsCurrent,
    DateTimeOffset SyncedAt);

public sealed record FileAiResultDto(
    Guid Id,
    Guid FileItemId,
    Guid VersionId,
    string Summary,
    IReadOnlyList<string> Tags,
    string? Language,
    string? Sensitivity,
    DateTimeOffset GeneratedAt,
    string? Model,
    Guid? AiRequestLogId,
    IReadOnlyList<Guid> EvidenceChunkIds);

public sealed record FileSuggestionDto(
    Guid Id,
    Guid FileItemId,
    string SuggestionType,
    string Title,
    string Reason,
    decimal Confidence,
    string PayloadJson,
    string Status,
    Guid? AiRequestLogId,
    DateTimeOffset CreatedAt,
    DateTimeOffset UpdatedAt);

public sealed record FileListQuery(string? Path);
public sealed record FileSearchQuery(string? Q, string? Mode);
public sealed record FileSearchResultDto(IReadOnlyList<FileItemDto> Items, IReadOnlyList<FileChunkSearchHitDto> Chunks);
public sealed record FileChunkSearchHitDto(Guid ChunkId, Guid FileItemId, Guid VersionId, string Text, decimal Score);
public sealed record MoveFileRequest(string DestinationPath);
public sealed record RenameFileRequest(string Name);
public sealed record FileOpenLinkDto(string Url, string Mode);
public sealed record VersionRestorePreviewDto(Guid FileItemId, Guid VersionId, string CurrentVersionLabel, string RestoreVersionLabel, bool RequiresConfirmation, string Summary);
public sealed record FileIndexJobDto(Guid Id, Guid FileItemId, Guid? VersionId, string Status, string Stage, int AttemptCount, string? LastError);
public sealed record FileSuggestionStatusRequest(string Status);
public sealed record FileListResponse(PagedResult<FileItemDto> Result);
```

- [ ] **Step 5: Create endpoint skeleton**

Create `src/modules/Pim.Module.Files/FilesModule.cs` with authorized routes returning `501` until services land:

```csharp
using System.Reflection;
using Microsoft.AspNetCore.Builder;
using Microsoft.AspNetCore.Http;
using Microsoft.AspNetCore.Mvc;
using Microsoft.AspNetCore.Routing;
using Microsoft.Extensions.Configuration;
using Microsoft.Extensions.DependencyInjection;
using Pim.Core.Common;
using Pim.Core.Modules;
using Pim.Infrastructure.Data;
using Pim.Module.Files.DTOs;

namespace Pim.Module.Files;

public sealed class FilesModule : IModule
{
    public string Name => "files";
    public string Version => "1.0.0";

    public void RegisterServices(IServiceCollection services, IConfiguration configuration)
    {
        PimDbContext.RegisterModuleAssembly(Assembly.GetExecutingAssembly());
    }

    public void MapEndpoints(IEndpointRouteBuilder endpoints)
    {
        var group = endpoints.MapGroup(FileEndpointPaths.Root).RequireAuthorization();

        group.MapGet("/providers", () => NotImplemented());
        group.MapPost("/providers/nextcloud", ([FromBody] BindNextcloudProviderRequest request) => NotImplemented());
        group.MapPost("/providers/{id:guid}/test", (Guid id) => NotImplemented());
        group.MapPost("/providers/{id:guid}/sync", (Guid id) => NotImplemented());
        group.MapGet("/items", ([FromQuery] string? path) => NotImplemented());
        group.MapGet("/items/{id:guid}", (Guid id) => NotImplemented());
        group.MapPost("/items/upload", (HttpRequest request) => NotImplemented());
        group.MapGet("/items/{id:guid}/download", (Guid id) => NotImplemented());
        group.MapPost("/items/{id:guid}/move", (Guid id, [FromBody] MoveFileRequest request) => NotImplemented());
        group.MapPost("/items/{id:guid}/rename", (Guid id, [FromBody] RenameFileRequest request) => NotImplemented());
        group.MapDelete("/items/{id:guid}", (Guid id) => NotImplemented());
        group.MapGet("/trash", () => NotImplemented());
        group.MapPost("/trash/{id:guid}/restore", (Guid id) => NotImplemented());
        group.MapGet("/items/{id:guid}/versions", (Guid id) => NotImplemented());
        group.MapGet("/items/{id:guid}/versions/{versionId:guid}/download", (Guid id, Guid versionId) => NotImplemented());
        group.MapPost("/items/{id:guid}/versions/{versionId:guid}/restore-preview", (Guid id, Guid versionId) => NotImplemented());
        group.MapPost("/items/{id:guid}/versions/{versionId:guid}/restore", (Guid id, Guid versionId) => NotImplemented());
        group.MapPost("/items/{id:guid}/index", (Guid id) => NotImplemented());
        group.MapGet("/search", ([FromQuery] string? q, [FromQuery] string? mode) => NotImplemented());
        group.MapGet("/suggestions", () => NotImplemented());
        group.MapPost("/suggestions/{id:guid}/dismiss", (Guid id) => NotImplemented());
        group.MapPost("/suggestions/{id:guid}/accept", (Guid id) => NotImplemented());
        group.MapGet("/items/{id:guid}/open-link", (Guid id, [FromQuery] string? mode) => NotImplemented());
    }

    public Task InitializeAsync(IServiceProvider serviceProvider) => Task.CompletedTask;

    private static IResult NotImplemented()
        => Results.Json(ApiResponse<string>.Error(501, "Files module endpoint is not implemented yet"), statusCode: 501);
}

public static class FileEndpointPaths
{
    public const string Root = "/api/v1/files";
    public const string Providers = $"{Root}/providers";
    public const string NextcloudProviders = $"{Providers}/nextcloud";

    public static string ProviderTest(string id) => $"{Providers}/{id}/test";
    public static string ProviderSync(string id) => $"{Providers}/{id}/sync";
    public static string Item(string id) => $"{Root}/items/{id}";
    public static string ItemDownload(string id) => $"{Item(id)}/download";
    public static string VersionRestore(string id, string versionId) => $"{Item(id)}/versions/{versionId}/restore";
}
```

- [ ] **Step 6: Add project references**

Run:

```powershell
dotnet sln Pim.sln add src/modules/Pim.Module.Files/Pim.Module.Files.csproj
dotnet add tests/Pim.UnitTests/Pim.UnitTests.csproj reference src/modules/Pim.Module.Files/Pim.Module.Files.csproj
```

Then edit `Pim.sln` if the new module is not nested under the existing `modules` solution folder. Use the same `NestedProjects` style as `Pim.Module.QuickNotes`.

- [ ] **Step 7: Run endpoint path tests**

Run: `dotnet test tests/Pim.UnitTests/Pim.UnitTests.csproj --filter FullyQualifiedName~FileEndpointPathTests -v minimal`

Expected: PASS.

- [ ] **Step 8: Commit**

```bash
git add Pim.sln tests/Pim.UnitTests/Pim.UnitTests.csproj src/modules/Pim.Module.Files tests/Pim.UnitTests/Files/FileEndpointPathTests.cs
git commit -m "feat: add files module endpoint shell"
```

## Task 2: Files Persistence Model

**Files:**
- Create: `src/modules/Pim.Module.Files/Entities/FileProviderEntity.cs`
- Create: `src/modules/Pim.Module.Files/Entities/FileItemEntity.cs`
- Create: `src/modules/Pim.Module.Files/Entities/FileVersionEntity.cs`
- Create: `src/modules/Pim.Module.Files/Entities/FileIndexJobEntity.cs`
- Create: `src/modules/Pim.Module.Files/Entities/FileChunkEntity.cs`
- Create: `src/modules/Pim.Module.Files/Entities/FileAiResultEntity.cs`
- Create: `src/modules/Pim.Module.Files/Entities/FileSuggestionEntity.cs`
- Create: `src/modules/Pim.Module.Files/Entities/FileEntityConfigurations.cs`
- Create: `tests/Pim.UnitTests/Files/FileModelTests.cs`
- Create: `src/Pim.Infrastructure/Data/Migrations/<timestamp>_AddFilesModule.cs`
- Modify: `src/Pim.Infrastructure/Data/Migrations/PimDbContextModelSnapshot.cs`

- [ ] **Step 1: Write failing model tests**

```csharp
using Microsoft.EntityFrameworkCore;
using Pim.Infrastructure.Data;
using Pim.Module.Files.Entities;
using Xunit;

namespace Pim.UnitTests.Files;

public class FileModelTests
{
    private static readonly Guid UserId = Guid.Parse("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");

    [Fact]
    public async Task FileProvider_DefaultsToNextcloudPendingAndProtectsPerUserUniqueness()
    {
        await using var db = CreateDb();
        var provider = new FileProviderEntity
        {
            UserId = UserId,
            BaseUrl = "https://cloud.example.test",
            InternalBaseUrl = "http://nextcloud",
            Username = "alice",
            AppPasswordSecret = "protected-secret"
        };

        db.Set<FileProviderEntity>().Add(provider);
        await db.SaveChangesAsync();

        var saved = await db.Set<FileProviderEntity>().SingleAsync();
        Assert.Equal("nextcloud", saved.Provider);
        Assert.Equal("pending", saved.Status);
        Assert.Equal("protected-secret", saved.AppPasswordSecret);
    }

    [Fact]
    public async Task FileItem_UsesExternalFileIdAsStableProviderIdentity()
    {
        await using var db = CreateDb();
        var provider = CreateProvider();
        var item = new FileItemEntity
        {
            Provider = provider,
            ExternalFileId = "00000123ocabc",
            ParentExternalFileId = "00000001ocabc",
            Path = "/Reports/report.docx",
            Name = "report.docx",
            ItemType = "file",
            MimeType = "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
            Size = 12345,
            Etag = "\"abc\"",
            Permissions = "RGDNVW",
            CreatedAt = DateTimeOffset.UtcNow,
            ModifiedAt = DateTimeOffset.UtcNow,
            SyncedAt = DateTimeOffset.UtcNow
        };

        db.Set<FileItemEntity>().Add(item);
        await db.SaveChangesAsync();

        var saved = await db.Set<FileItemEntity>().SingleAsync();
        Assert.Equal("00000123ocabc", saved.ExternalFileId);
        Assert.Equal("/Reports/report.docx", saved.Path);
        Assert.False(saved.IsDeleted);
    }

    [Fact]
    public async Task HistoricalVersions_AreNotCurrentByDefault()
    {
        await using var db = CreateDb();
        var item = CreateItem(CreateProvider());
        var current = new FileVersionEntity
        {
            FileItem = item,
            ExternalVersionId = "current:etag-1",
            Etag = "etag-1",
            Size = 100,
            ModifiedAt = DateTimeOffset.UtcNow,
            Source = "current",
            IsCurrent = true,
            SyncedAt = DateTimeOffset.UtcNow
        };
        var history = new FileVersionEntity
        {
            FileItem = item,
            ExternalVersionId = "1700000000",
            Etag = "etag-old",
            Size = 90,
            ModifiedAt = DateTimeOffset.UtcNow.AddDays(-1),
            Source = "history",
            IsCurrent = false,
            SyncedAt = DateTimeOffset.UtcNow
        };

        db.Set<FileVersionEntity>().AddRange(current, history);
        await db.SaveChangesAsync();

        Assert.Single(await db.Set<FileVersionEntity>().Where(v => v.IsCurrent).ToListAsync());
        Assert.Single(await db.Set<FileVersionEntity>().Where(v => v.Source == "history").ToListAsync());
    }

    [Fact]
    public async Task DeletedItems_AreVisibleToFilesModuleQueries()
    {
        await using var db = CreateDb();
        var item = CreateItem(CreateProvider());
        item.IsDeleted = true;
        item.DeletedAt = DateTimeOffset.UtcNow;

        db.Set<FileItemEntity>().Add(item);
        await db.SaveChangesAsync();

        var saved = await db.Set<FileItemEntity>().SingleAsync();
        Assert.True(saved.IsDeleted);
        Assert.NotNull(saved.DeletedAt);
    }

    private static PimDbContext CreateDb()
    {
        PimDbContext.RegisterModuleAssembly(typeof(FileProviderEntity).Assembly);
        var options = new DbContextOptionsBuilder<PimDbContext>()
            .UseInMemoryDatabase($"files-model-{Guid.NewGuid()}")
            .Options;
        return new PimDbContext(options);
    }

    private static FileProviderEntity CreateProvider() => new()
    {
        UserId = UserId,
        BaseUrl = "https://cloud.example.test",
        InternalBaseUrl = "http://nextcloud",
        Username = "alice",
        AppPasswordSecret = "protected-secret"
    };

    private static FileItemEntity CreateItem(FileProviderEntity provider) => new()
    {
        Provider = provider,
        ExternalFileId = "00000123ocabc",
        Path = "/report.txt",
        Name = "report.txt",
        ItemType = "file",
        MimeType = "text/plain",
        Size = 12,
        Etag = "etag-1",
        CreatedAt = DateTimeOffset.UtcNow,
        ModifiedAt = DateTimeOffset.UtcNow,
        SyncedAt = DateTimeOffset.UtcNow
    };
}
```

- [ ] **Step 2: Run the failing model tests**

Run: `dotnet test tests/Pim.UnitTests/Pim.UnitTests.csproj --filter FullyQualifiedName~FileModelTests -v minimal`

Expected: FAIL because entity types do not exist.

- [ ] **Step 3: Add entity classes**

Use these complete property sets. Apply `[MaxLength]` and `[Column]` attributes only if the repository starts using them elsewhere; otherwise keep mapping in `FileEntityConfigurations.cs`.

```csharp
namespace Pim.Module.Files.Entities;

public sealed class FileProviderEntity
{
    public Guid Id { get; set; } = Guid.NewGuid();
    public Guid UserId { get; set; }
    public string Provider { get; set; } = "nextcloud";
    public string BaseUrl { get; set; } = string.Empty;
    public string? InternalBaseUrl { get; set; }
    public string Username { get; set; } = string.Empty;
    public string AppPasswordSecret { get; set; } = string.Empty;
    public string Status { get; set; } = "pending";
    public DateTimeOffset? LastSyncAt { get; set; }
    public string? LastError { get; set; }
    public DateTimeOffset CreatedAt { get; set; } = DateTimeOffset.UtcNow;
    public DateTimeOffset UpdatedAt { get; set; } = DateTimeOffset.UtcNow;

    public List<FileItemEntity> Items { get; } = new();
}
```

```csharp
namespace Pim.Module.Files.Entities;

public sealed class FileItemEntity
{
    public Guid Id { get; set; } = Guid.NewGuid();
    public Guid ProviderId { get; set; }
    public FileProviderEntity? Provider { get; set; }
    public string ExternalFileId { get; set; } = string.Empty;
    public string? ParentExternalFileId { get; set; }
    public string Path { get; set; } = "/";
    public string Name { get; set; } = string.Empty;
    public string ItemType { get; set; } = "file";
    public string? MimeType { get; set; }
    public long? Size { get; set; }
    public string? Etag { get; set; }
    public string? ContentHash { get; set; }
    public Guid? CurrentVersionId { get; set; }
    public string? Permissions { get; set; }
    public bool IsDeleted { get; set; }
    public DateTimeOffset? DeletedAt { get; set; }
    public DateTimeOffset? LastSeenAt { get; set; }
    public DateTimeOffset CreatedAt { get; set; } = DateTimeOffset.UtcNow;
    public DateTimeOffset ModifiedAt { get; set; } = DateTimeOffset.UtcNow;
    public DateTimeOffset SyncedAt { get; set; } = DateTimeOffset.UtcNow;

    public List<FileVersionEntity> Versions { get; } = new();
    public List<FileIndexJobEntity> IndexJobs { get; } = new();
    public List<FileChunkEntity> Chunks { get; } = new();
    public List<FileAiResultEntity> AiResults { get; } = new();
    public List<FileSuggestionEntity> Suggestions { get; } = new();
}
```

```csharp
namespace Pim.Module.Files.Entities;

public sealed class FileVersionEntity
{
    public Guid Id { get; set; } = Guid.NewGuid();
    public Guid FileItemId { get; set; }
    public FileItemEntity? FileItem { get; set; }
    public string ExternalVersionId { get; set; } = string.Empty;
    public string? Etag { get; set; }
    public long? Size { get; set; }
    public DateTimeOffset ModifiedAt { get; set; } = DateTimeOffset.UtcNow;
    public string Source { get; set; } = "history";
    public bool IsCurrent { get; set; }
    public DateTimeOffset SyncedAt { get; set; } = DateTimeOffset.UtcNow;
}
```

```csharp
namespace Pim.Module.Files.Entities;

public sealed class FileIndexJobEntity
{
    public Guid Id { get; set; } = Guid.NewGuid();
    public Guid FileItemId { get; set; }
    public FileItemEntity? FileItem { get; set; }
    public Guid? VersionId { get; set; }
    public FileVersionEntity? Version { get; set; }
    public string Status { get; set; } = "pending";
    public string Stage { get; set; } = "metadata";
    public int AttemptCount { get; set; }
    public string? LastError { get; set; }
    public DateTimeOffset? StartedAt { get; set; }
    public DateTimeOffset? FinishedAt { get; set; }
}
```

```csharp
namespace Pim.Module.Files.Entities;

public sealed class FileChunkEntity
{
    public Guid Id { get; set; } = Guid.NewGuid();
    public Guid FileItemId { get; set; }
    public FileItemEntity? FileItem { get; set; }
    public Guid VersionId { get; set; }
    public FileVersionEntity? Version { get; set; }
    public int ChunkIndex { get; set; }
    public string Text { get; set; } = string.Empty;
    public string TextHash { get; set; } = string.Empty;
    public int StartOffset { get; set; }
    public int EndOffset { get; set; }
    public string? QdrantPointId { get; set; }
}
```

```csharp
namespace Pim.Module.Files.Entities;

public sealed class FileAiResultEntity
{
    public Guid Id { get; set; } = Guid.NewGuid();
    public Guid FileItemId { get; set; }
    public FileItemEntity? FileItem { get; set; }
    public Guid VersionId { get; set; }
    public FileVersionEntity? Version { get; set; }
    public string Summary { get; set; } = string.Empty;
    public string TagsJson { get; set; } = "[]";
    public string? Language { get; set; }
    public string? Sensitivity { get; set; }
    public DateTimeOffset GeneratedAt { get; set; } = DateTimeOffset.UtcNow;
    public string? Model { get; set; }
    public Guid? AiRequestLogId { get; set; }
    public string EvidenceChunkIdsJson { get; set; } = "[]";
}
```

```csharp
namespace Pim.Module.Files.Entities;

public sealed class FileSuggestionEntity
{
    public Guid Id { get; set; } = Guid.NewGuid();
    public Guid FileItemId { get; set; }
    public FileItemEntity? FileItem { get; set; }
    public string SuggestionType { get; set; } = string.Empty;
    public string Title { get; set; } = string.Empty;
    public string Reason { get; set; } = string.Empty;
    public decimal Confidence { get; set; }
    public string PayloadJson { get; set; } = "{}";
    public string Status { get; set; } = "pending";
    public Guid? AiRequestLogId { get; set; }
    public DateTimeOffset CreatedAt { get; set; } = DateTimeOffset.UtcNow;
    public DateTimeOffset UpdatedAt { get; set; } = DateTimeOffset.UtcNow;
}
```

- [ ] **Step 4: Add EF configuration**

Create `FileEntityConfigurations.cs` with tables:

```csharp
using Microsoft.EntityFrameworkCore;
using Microsoft.EntityFrameworkCore.Metadata.Builders;

namespace Pim.Module.Files.Entities;

public sealed class FileProviderEntityConfiguration : IEntityTypeConfiguration<FileProviderEntity>
{
    public void Configure(EntityTypeBuilder<FileProviderEntity> builder)
    {
        builder.ToTable("file_providers");
        builder.Property(e => e.Provider).HasMaxLength(32).HasDefaultValue("nextcloud");
        builder.Property(e => e.BaseUrl).HasMaxLength(1024);
        builder.Property(e => e.InternalBaseUrl).HasMaxLength(1024);
        builder.Property(e => e.Username).HasMaxLength(255);
        builder.Property(e => e.Status).HasMaxLength(32).HasDefaultValue("pending");
        builder.Property(e => e.CreatedAt).HasDefaultValueSql("now()");
        builder.Property(e => e.UpdatedAt).HasDefaultValueSql("now()");
        builder.HasIndex(e => new { e.UserId, e.Provider, e.BaseUrl, e.Username }).IsUnique();
        builder.HasIndex(e => new { e.UserId, e.Status });
    }
}

public sealed class FileItemEntityConfiguration : IEntityTypeConfiguration<FileItemEntity>
{
    public void Configure(EntityTypeBuilder<FileItemEntity> builder)
    {
        builder.ToTable("file_items");
        builder.Property(e => e.ExternalFileId).HasMaxLength(255);
        builder.Property(e => e.ParentExternalFileId).HasMaxLength(255);
        builder.Property(e => e.Path).HasColumnType("text");
        builder.Property(e => e.Name).HasMaxLength(512);
        builder.Property(e => e.ItemType).HasMaxLength(16).HasDefaultValue("file");
        builder.Property(e => e.MimeType).HasMaxLength(255);
        builder.Property(e => e.Etag).HasMaxLength(255);
        builder.Property(e => e.ContentHash).HasMaxLength(128);
        builder.Property(e => e.Permissions).HasMaxLength(64);
        builder.Property(e => e.CreatedAt).HasDefaultValueSql("now()");
        builder.Property(e => e.ModifiedAt).HasDefaultValueSql("now()");
        builder.Property(e => e.SyncedAt).HasDefaultValueSql("now()");
        builder.HasOne(e => e.Provider).WithMany(p => p.Items).HasForeignKey(e => e.ProviderId).OnDelete(DeleteBehavior.Cascade);
        builder.HasIndex(e => new { e.ProviderId, e.ExternalFileId }).IsUnique();
        builder.HasIndex(e => new { e.ProviderId, e.Path });
        builder.HasIndex(e => new { e.ProviderId, e.ParentExternalFileId });
        builder.HasIndex(e => new { e.ProviderId, e.IsDeleted });
    }
}

public sealed class FileVersionEntityConfiguration : IEntityTypeConfiguration<FileVersionEntity>
{
    public void Configure(EntityTypeBuilder<FileVersionEntity> builder)
    {
        builder.ToTable("file_versions");
        builder.Property(e => e.ExternalVersionId).HasMaxLength(255);
        builder.Property(e => e.Etag).HasMaxLength(255);
        builder.Property(e => e.Source).HasMaxLength(32).HasDefaultValue("history");
        builder.Property(e => e.SyncedAt).HasDefaultValueSql("now()");
        builder.HasOne(e => e.FileItem).WithMany(i => i.Versions).HasForeignKey(e => e.FileItemId).OnDelete(DeleteBehavior.Cascade);
        builder.HasIndex(e => new { e.FileItemId, e.ExternalVersionId }).IsUnique();
        builder.HasIndex(e => new { e.FileItemId, e.IsCurrent });
    }
}

public sealed class FileIndexJobEntityConfiguration : IEntityTypeConfiguration<FileIndexJobEntity>
{
    public void Configure(EntityTypeBuilder<FileIndexJobEntity> builder)
    {
        builder.ToTable("file_index_jobs");
        builder.Property(e => e.Status).HasMaxLength(32).HasDefaultValue("pending");
        builder.Property(e => e.Stage).HasMaxLength(32).HasDefaultValue("metadata");
        builder.HasOne(e => e.FileItem).WithMany(i => i.IndexJobs).HasForeignKey(e => e.FileItemId).OnDelete(DeleteBehavior.Cascade);
        builder.HasOne(e => e.Version).WithMany().HasForeignKey(e => e.VersionId).OnDelete(DeleteBehavior.SetNull);
        builder.HasIndex(e => new { e.FileItemId, e.Status });
        builder.HasIndex(e => new { e.Status, e.Stage });
    }
}

public sealed class FileChunkEntityConfiguration : IEntityTypeConfiguration<FileChunkEntity>
{
    public void Configure(EntityTypeBuilder<FileChunkEntity> builder)
    {
        builder.ToTable("file_chunks");
        builder.Property(e => e.Text).HasColumnType("text");
        builder.Property(e => e.TextHash).HasMaxLength(128);
        builder.Property(e => e.QdrantPointId).HasMaxLength(128);
        builder.HasOne(e => e.FileItem).WithMany(i => i.Chunks).HasForeignKey(e => e.FileItemId).OnDelete(DeleteBehavior.Cascade);
        builder.HasOne(e => e.Version).WithMany().HasForeignKey(e => e.VersionId).OnDelete(DeleteBehavior.Cascade);
        builder.HasIndex(e => new { e.FileItemId, e.VersionId, e.ChunkIndex }).IsUnique();
        builder.HasIndex(e => e.QdrantPointId);
    }
}

public sealed class FileAiResultEntityConfiguration : IEntityTypeConfiguration<FileAiResultEntity>
{
    public void Configure(EntityTypeBuilder<FileAiResultEntity> builder)
    {
        builder.ToTable("file_ai_results");
        builder.Property(e => e.Summary).HasColumnType("text");
        builder.Property(e => e.TagsJson).HasColumnType("jsonb").HasDefaultValue("[]");
        builder.Property(e => e.Language).HasMaxLength(32);
        builder.Property(e => e.Sensitivity).HasMaxLength(32);
        builder.Property(e => e.Model).HasMaxLength(255);
        builder.Property(e => e.EvidenceChunkIdsJson).HasColumnType("jsonb").HasDefaultValue("[]");
        builder.Property(e => e.GeneratedAt).HasDefaultValueSql("now()");
        builder.HasOne(e => e.FileItem).WithMany(i => i.AiResults).HasForeignKey(e => e.FileItemId).OnDelete(DeleteBehavior.Cascade);
        builder.HasOne(e => e.Version).WithMany().HasForeignKey(e => e.VersionId).OnDelete(DeleteBehavior.Cascade);
        builder.HasIndex(e => new { e.FileItemId, e.VersionId }).IsUnique();
        builder.HasIndex(e => e.AiRequestLogId);
    }
}

public sealed class FileSuggestionEntityConfiguration : IEntityTypeConfiguration<FileSuggestionEntity>
{
    public void Configure(EntityTypeBuilder<FileSuggestionEntity> builder)
    {
        builder.ToTable("file_suggestions");
        builder.Property(e => e.SuggestionType).HasMaxLength(32);
        builder.Property(e => e.Title).HasMaxLength(255);
        builder.Property(e => e.Reason).HasColumnType("text");
        builder.Property(e => e.PayloadJson).HasColumnType("jsonb").HasDefaultValue("{}");
        builder.Property(e => e.Status).HasMaxLength(32).HasDefaultValue("pending");
        builder.Property(e => e.CreatedAt).HasDefaultValueSql("now()");
        builder.Property(e => e.UpdatedAt).HasDefaultValueSql("now()");
        builder.HasOne(e => e.FileItem).WithMany(i => i.Suggestions).HasForeignKey(e => e.FileItemId).OnDelete(DeleteBehavior.Cascade);
        builder.HasIndex(e => new { e.FileItemId, e.Status });
        builder.HasIndex(e => e.SuggestionType);
        builder.HasIndex(e => e.AiRequestLogId);
    }
}
```

- [ ] **Step 5: Generate migration**

Run:

```powershell
dotnet ef migrations add AddFilesModule --project src/Pim.Infrastructure --startup-project src/Pim.Api
```

Expected: a migration creating `file_providers`, `file_items`, `file_versions`, `file_index_jobs`, `file_chunks`, `file_ai_results`, and `file_suggestions`.

- [ ] **Step 6: Run model tests**

Run: `dotnet test tests/Pim.UnitTests/Pim.UnitTests.csproj --filter FullyQualifiedName~FileModelTests -v minimal`

Expected: PASS.

- [ ] **Step 7: Commit**

```bash
git add src/modules/Pim.Module.Files/Entities src/Pim.Infrastructure/Data/Migrations tests/Pim.UnitTests/Files/FileModelTests.cs
git commit -m "feat: add files persistence model"
```

## Task 3: Secret Protection And Provider Binding

**Files:**
- Create: `src/Pim.Infrastructure/Secrets/ISecretProtector.cs`
- Create: `src/Pim.Infrastructure/Secrets/DataProtectionSecretProtector.cs`
- Modify: `src/Pim.Infrastructure/Extensions/ServiceCollectionExtensions.cs`
- Create: `src/modules/Pim.Module.Files/Services/FileProviderBindingService.cs`
- Modify: `src/modules/Pim.Module.Files/FilesModule.cs`
- Create: `tests/Pim.UnitTests/Files/FileProviderBindingServiceTests.cs`

- [ ] **Step 1: Write failing provider binding tests**

```csharp
using Microsoft.EntityFrameworkCore;
using Pim.Core.Exceptions;
using Pim.Infrastructure.Auth;
using Pim.Infrastructure.Data;
using Pim.Infrastructure.Secrets;
using Pim.Module.Files.Entities;
using Pim.Module.Files.DTOs;
using Pim.Module.Files.Providers;
using Pim.Module.Files.Services;
using Xunit;

namespace Pim.UnitTests.Files;

public class FileProviderBindingServiceTests
{
    private static readonly Guid UserId = Guid.Parse("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");

    [Fact]
    public async Task BindNextcloudAsync_ProtectsAppPasswordAndDoesNotReturnIt()
    {
        await using var db = CreateDb();
        var service = new FileProviderBindingService(db, new FixedCurrentUserService(UserId), new FakeSecretProtector(), new FakeAdapter());

        var dto = await service.BindNextcloudAsync(new BindNextcloudProviderRequest(
            "https://cloud.example.test/",
            "http://nextcloud/",
            "alice",
            "app-password"));

        Assert.Equal("https://cloud.example.test", dto.BaseUrl);
        Assert.Equal("http://nextcloud", dto.InternalBaseUrl);
        Assert.Equal("alice", dto.Username);
        Assert.Equal("pending", dto.Status);

        var entity = await db.Set<FileProviderEntity>().SingleAsync();
        Assert.Equal("protected:app-password", entity.AppPasswordSecret);
        Assert.DoesNotContain("app-password", dto.ToString(), StringComparison.Ordinal);
    }

    [Fact]
    public async Task BindNextcloudAsync_RejectsNonHttpBaseUrl()
    {
        await using var db = CreateDb();
        var service = new FileProviderBindingService(db, new FixedCurrentUserService(UserId), new FakeSecretProtector(), new FakeAdapter());

        var error = await Assert.ThrowsAsync<DomainException>(() => service.BindNextcloudAsync(new BindNextcloudProviderRequest(
            "ftp://cloud.example.test",
            null,
            "alice",
            "app-password")));

        Assert.Equal(5101, error.ErrorCode);
    }

    [Fact]
    public async Task TestProviderAsync_UsesUnprotectedSecret()
    {
        await using var db = CreateDb();
        var adapter = new FakeAdapter();
        var service = new FileProviderBindingService(db, new FixedCurrentUserService(UserId), new FakeSecretProtector(), adapter);
        var provider = await service.BindNextcloudAsync(new BindNextcloudProviderRequest(
            "https://cloud.example.test",
            "http://nextcloud",
            "alice",
            "app-password"));

        var result = await service.TestProviderAsync(provider.Id);

        Assert.True(result.Success);
        Assert.Equal("app-password", adapter.LastPassword);
    }

    private static PimDbContext CreateDb()
    {
        PimDbContext.RegisterModuleAssembly(typeof(FileProviderEntity).Assembly);
        var options = new DbContextOptionsBuilder<PimDbContext>()
            .UseInMemoryDatabase($"files-binding-{Guid.NewGuid()}")
            .Options;
        return new PimDbContext(options);
    }

    private sealed class FixedCurrentUserService(Guid userId) : ICurrentUserService
    {
        public Guid? UserId { get; } = userId;
        public string? Role => "user";
    }

    private sealed class FakeSecretProtector : ISecretProtector
    {
        public string Protect(string value) => $"protected:{value}";
        public string Unprotect(string protectedValue) => protectedValue["protected:".Length..];
    }

    private sealed class FakeAdapter : IFileProviderAdapter
    {
        public string? LastPassword { get; private set; }
        public Task<FileProviderTestResult> TestConnectionAsync(FileProviderConnection connection, CancellationToken ct = default)
        {
            LastPassword = connection.AppPassword;
            return Task.FromResult(new FileProviderTestResult(true, "ok", null));
        }
    }
}
```

- [ ] **Step 2: Add secret protector**

Create `ISecretProtector`:

```csharp
namespace Pim.Infrastructure.Secrets;

public interface ISecretProtector
{
    string Protect(string value);
    string Unprotect(string protectedValue);
}
```

Create `DataProtectionSecretProtector`:

```csharp
using Microsoft.AspNetCore.DataProtection;

namespace Pim.Infrastructure.Secrets;

public sealed class DataProtectionSecretProtector : ISecretProtector
{
    private readonly IDataProtector _protector;

    public DataProtectionSecretProtector(IDataProtectionProvider provider)
    {
        _protector = provider.CreateProtector("Pim.ServerSideSecrets.v1");
    }

    public string Protect(string value) => _protector.Protect(value);

    public string Unprotect(string protectedValue) => _protector.Unprotect(protectedValue);
}
```

Modify `ServiceCollectionExtensions.AddPimInfrastructure`:

```csharp
services.AddDataProtection()
    .SetApplicationName("Pim");
services.AddSingleton<ISecretProtector, DataProtectionSecretProtector>();
```

- [ ] **Step 3: Add provider adapter contract minimal test surface**

Create `src/modules/Pim.Module.Files/Providers/IFileProviderAdapter.cs`:

```csharp
namespace Pim.Module.Files.Providers;

public sealed record FileProviderConnection(
    Guid ProviderId,
    string BaseUrl,
    string? InternalBaseUrl,
    string Username,
    string AppPassword);

public sealed record FileProviderTestResult(bool Success, string Status, string? ErrorMessage);

public interface IFileProviderAdapter
{
    Task<FileProviderTestResult> TestConnectionAsync(FileProviderConnection connection, CancellationToken ct = default);
}
```

- [ ] **Step 4: Implement provider binding service**

Create `FileProviderBindingService.cs`:

```csharp
using Microsoft.EntityFrameworkCore;
using Pim.Core.Exceptions;
using Pim.Infrastructure.Auth;
using Pim.Infrastructure.Data;
using Pim.Infrastructure.Secrets;
using Pim.Module.Files.DTOs;
using Pim.Module.Files.Entities;
using Pim.Module.Files.Providers;

namespace Pim.Module.Files.Services;

public sealed class FileProviderBindingService(
    PimDbContext db,
    ICurrentUserService currentUser,
    ISecretProtector secretProtector,
    IFileProviderAdapter adapter)
{
    private Guid UserId => currentUser.UserId ?? throw new DomainException(1002, "Not authenticated");

    public async Task<IReadOnlyList<FileProviderDto>> ListProvidersAsync(CancellationToken ct = default)
    {
        var userId = UserId;
        var providers = await db.Set<FileProviderEntity>()
            .AsNoTracking()
            .Where(p => p.UserId == userId)
            .OrderBy(p => p.Provider)
            .ThenBy(p => p.Username)
            .ToListAsync(ct);

        return providers.Select(MapProvider).ToList();
    }

    public async Task<FileProviderDto> BindNextcloudAsync(BindNextcloudProviderRequest request, CancellationToken ct = default)
    {
        var userId = UserId;
        var baseUrl = NormalizeHttpUrl(request.BaseUrl, "Nextcloud base URL");
        var internalBaseUrl = string.IsNullOrWhiteSpace(request.InternalBaseUrl)
            ? null
            : NormalizeHttpUrl(request.InternalBaseUrl, "Nextcloud internal base URL");
        var username = NormalizeRequired(request.Username, "Nextcloud username");
        var appPassword = NormalizeRequired(request.AppPassword, "Nextcloud app password");
        var now = DateTimeOffset.UtcNow;

        var existing = await db.Set<FileProviderEntity>()
            .FirstOrDefaultAsync(p => p.UserId == userId && p.Provider == "nextcloud" && p.BaseUrl == baseUrl && p.Username == username, ct);

        var entity = existing ?? new FileProviderEntity
        {
            UserId = userId,
            Provider = "nextcloud",
            BaseUrl = baseUrl,
            Username = username,
            CreatedAt = now
        };

        entity.InternalBaseUrl = internalBaseUrl;
        entity.AppPasswordSecret = secretProtector.Protect(appPassword);
        entity.Status = "pending";
        entity.LastError = null;
        entity.UpdatedAt = now;

        if (existing is null)
            db.Set<FileProviderEntity>().Add(entity);

        await db.SaveChangesAsync(ct);
        return MapProvider(entity);
    }

    public async Task<FileProviderTestDto> TestProviderAsync(Guid providerId, CancellationToken ct = default)
    {
        var provider = await LoadProviderAsync(providerId, ct);
        var result = await adapter.TestConnectionAsync(ToConnection(provider), ct);
        provider.Status = result.Success ? "connected" : "error";
        provider.LastError = result.ErrorMessage;
        provider.UpdatedAt = DateTimeOffset.UtcNow;
        await db.SaveChangesAsync(ct);
        return new FileProviderTestDto(result.Success, result.Status, result.ErrorMessage);
    }

    public async Task<FileProviderConnection> GetConnectionAsync(Guid providerId, CancellationToken ct = default)
    {
        var provider = await LoadProviderAsync(providerId, ct);
        return ToConnection(provider);
    }

    private async Task<FileProviderEntity> LoadProviderAsync(Guid providerId, CancellationToken ct)
    {
        var userId = UserId;
        return await db.Set<FileProviderEntity>()
            .FirstOrDefaultAsync(p => p.Id == providerId && p.UserId == userId, ct)
            ?? throw new DomainException(5104, "File provider not found");
    }

    private FileProviderConnection ToConnection(FileProviderEntity provider)
        => new(provider.Id, provider.BaseUrl, provider.InternalBaseUrl, provider.Username, secretProtector.Unprotect(provider.AppPasswordSecret));

    private static FileProviderDto MapProvider(FileProviderEntity provider)
        => new(provider.Id, provider.Provider, provider.BaseUrl, provider.InternalBaseUrl, provider.Username, provider.Status, provider.LastSyncAt, provider.LastError, provider.CreatedAt, provider.UpdatedAt);

    private static string NormalizeRequired(string? value, string label)
    {
        var normalized = value?.Trim();
        if (string.IsNullOrWhiteSpace(normalized))
            throw new DomainException(5100, $"{label} is required");
        return normalized;
    }

    private static string NormalizeHttpUrl(string? value, string label)
    {
        var normalized = NormalizeRequired(value, label).TrimEnd('/');
        if (!Uri.TryCreate(normalized, UriKind.Absolute, out var uri) || (uri.Scheme != Uri.UriSchemeHttp && uri.Scheme != Uri.UriSchemeHttps))
            throw new DomainException(5101, $"{label} must be an absolute HTTP or HTTPS URL");
        return normalized;
    }
}
```

- [ ] **Step 5: Register service and map real provider endpoints**

Modify `FilesModule.RegisterServices`:

```csharp
services.AddScoped<FileProviderBindingService>();
services.AddHttpClient<NextcloudFileProviderAdapter>();
services.AddScoped<IFileProviderAdapter>(sp => sp.GetRequiredService<NextcloudFileProviderAdapter>());
```

Modify provider endpoints in `FilesModule.MapEndpoints`:

```csharp
group.MapGet("/providers", async ([FromServices] FileProviderBindingService service, CancellationToken ct) =>
    Results.Ok(ApiResponse<IReadOnlyList<FileProviderDto>>.Ok(await service.ListProvidersAsync(ct))));

group.MapPost("/providers/nextcloud", async ([FromBody] BindNextcloudProviderRequest request, [FromServices] FileProviderBindingService service, CancellationToken ct) =>
{
    var result = await service.BindNextcloudAsync(request, ct);
    return Results.Ok(ApiResponse<FileProviderDto>.Ok(result));
});

group.MapPost("/providers/{id:guid}/test", async (Guid id, [FromServices] FileProviderBindingService service, CancellationToken ct) =>
    Results.Ok(ApiResponse<FileProviderTestDto>.Ok(await service.TestProviderAsync(id, ct))));
```

- [ ] **Step 6: Run provider tests**

Run: `dotnet test tests/Pim.UnitTests/Pim.UnitTests.csproj --filter FullyQualifiedName~FileProviderBindingServiceTests -v minimal`

Expected: PASS.

- [ ] **Step 7: Commit**

```bash
git add src/Pim.Infrastructure/Secrets src/Pim.Infrastructure/Extensions/ServiceCollectionExtensions.cs src/modules/Pim.Module.Files tests/Pim.UnitTests/Files/FileProviderBindingServiceTests.cs
git commit -m "feat: bind nextcloud file providers"
```

## Task 4: Nextcloud Adapter And WebDAV Parser

**Files:**
- Modify: `src/modules/Pim.Module.Files/Providers/IFileProviderAdapter.cs`
- Create: `src/modules/Pim.Module.Files/Providers/NextcloudFileProviderAdapter.cs`
- Create: `src/modules/Pim.Module.Files/Providers/NextcloudDavXmlParser.cs`
- Create: `src/modules/Pim.Module.Files/Providers/NextcloudOptions.cs`
- Create: `tests/Pim.UnitTests/Files/NextcloudDavXmlParserTests.cs`
- Create: `tests/Pim.UnitTests/Files/NextcloudFileProviderAdapterTests.cs`

- [ ] **Step 1: Extend adapter contract**

Replace `IFileProviderAdapter.cs` with:

```csharp
namespace Pim.Module.Files.Providers;

public sealed record FileProviderConnection(Guid ProviderId, string BaseUrl, string? InternalBaseUrl, string Username, string AppPassword);
public sealed record FileProviderTestResult(bool Success, string Status, string? ErrorMessage);
public sealed record ProviderFileItem(string ExternalFileId, string? ParentExternalFileId, string Path, string Name, string ItemType, string? MimeType, long? Size, string? Etag, string? Permissions, DateTimeOffset ModifiedAt);
public sealed record ProviderFileVersion(string ExternalVersionId, string? Etag, long? Size, DateTimeOffset ModifiedAt, string Source, bool IsCurrent);
public sealed record ProviderTrashItem(string TrashId, string OriginalLocation, string Name, string ItemType, long? Size, DateTimeOffset DeletedAt);
public sealed record ProviderOpenLink(string Url, string Mode);
public sealed record ProviderDownload(Stream Content, string ContentType, string FileName);

public interface IFileProviderAdapter
{
    Task<FileProviderTestResult> TestConnectionAsync(FileProviderConnection connection, CancellationToken ct = default);
    Task<IReadOnlyList<ProviderFileItem>> ListFolderAsync(FileProviderConnection connection, string path, CancellationToken ct = default);
    Task<ProviderFileItem> GetMetadataAsync(FileProviderConnection connection, string path, CancellationToken ct = default);
    Task<ProviderFileItem> UploadAsync(FileProviderConnection connection, string destinationPath, Stream content, string contentType, CancellationToken ct = default);
    Task<ProviderDownload> DownloadAsync(FileProviderConnection connection, string path, CancellationToken ct = default);
    Task<ProviderFileItem> MoveAsync(FileProviderConnection connection, string sourcePath, string destinationPath, CancellationToken ct = default);
    Task<ProviderFileItem> RenameAsync(FileProviderConnection connection, string sourcePath, string name, CancellationToken ct = default);
    Task DeleteToTrashAsync(FileProviderConnection connection, string path, CancellationToken ct = default);
    Task<IReadOnlyList<ProviderTrashItem>> ListTrashAsync(FileProviderConnection connection, CancellationToken ct = default);
    Task RestoreTrashAsync(FileProviderConnection connection, string trashId, CancellationToken ct = default);
    Task<IReadOnlyList<ProviderFileVersion>> ListVersionsAsync(FileProviderConnection connection, string externalFileId, CancellationToken ct = default);
    Task<ProviderDownload> DownloadVersionAsync(FileProviderConnection connection, string externalFileId, string externalVersionId, string fileName, CancellationToken ct = default);
    Task RestoreVersionAsync(FileProviderConnection connection, string externalFileId, string externalVersionId, CancellationToken ct = default);
    ProviderOpenLink BuildOpenLink(FileProviderConnection connection, string path, string mode);
}
```

- [ ] **Step 2: Write parser tests**

```csharp
using Pim.Module.Files.Providers;
using Xunit;

namespace Pim.UnitTests.Files;

public class NextcloudDavXmlParserTests
{
    [Fact]
    public void ParseItems_MapsStableIdsPathsEtagsAndFolders()
    {
        var xml = """
        <?xml version="1.0"?>
        <d:multistatus xmlns:d="DAV:" xmlns:oc="http://owncloud.org/ns" xmlns:nc="http://nextcloud.org/ns">
          <d:response>
            <d:href>/remote.php/dav/files/alice/Reports/</d:href>
            <d:propstat><d:prop><d:resourcetype><d:collection /></d:resourcetype><oc:fileid>10</oc:fileid><oc:permissions>RGDNVCK</oc:permissions><d:getetag>&quot;folder-etag&quot;</d:getetag><d:getlastmodified>Wed, 20 May 2026 10:00:00 GMT</d:getlastmodified></d:prop></d:propstat>
          </d:response>
          <d:response>
            <d:href>/remote.php/dav/files/alice/Reports/report.docx</d:href>
            <d:propstat><d:prop><d:resourcetype /><oc:fileid>11</oc:fileid><oc:permissions>RGDNVW</oc:permissions><d:getetag>&quot;file-etag&quot;</d:getetag><d:getcontentlength>123</d:getcontentlength><d:getcontenttype>application/vnd.openxmlformats-officedocument.wordprocessingml.document</d:getcontenttype><d:getlastmodified>Wed, 20 May 2026 10:01:00 GMT</d:getlastmodified></d:prop></d:propstat>
          </d:response>
        </d:multistatus>
        """;

        var items = NextcloudDavXmlParser.ParseItems(xml, "/remote.php/dav/files/alice", "/Reports");

        Assert.Equal(2, items.Count);
        Assert.Equal("10", items[0].ExternalFileId);
        Assert.Equal("/Reports", items[0].Path);
        Assert.Equal("folder", items[0].ItemType);
        Assert.Equal("11", items[1].ExternalFileId);
        Assert.Equal("/Reports/report.docx", items[1].Path);
        Assert.Equal("file", items[1].ItemType);
        Assert.Equal(123, items[1].Size);
        Assert.Equal("\"file-etag\"", items[1].Etag);
    }
}
```

- [ ] **Step 3: Implement XML parser**

Create parser using `XDocument` and namespace constants. It must:
- URL-decode `<d:href>`.
- Remove the WebDAV prefix `/remote.php/dav/files/{username}`.
- Normalize root to `/`.
- Treat `<d:collection />` inside `<d:resourcetype>` as `folder`, otherwise `file`.
- Read `oc:fileid`, `oc:permissions`, `d:getetag`, `d:getcontentlength`, `d:getcontenttype`, `d:getlastmodified`.
- Throw `DomainException(5201, "Nextcloud response did not include a file id")` when `oc:fileid` is missing for normal file entries.

- [ ] **Step 4: Write adapter HTTP tests**

Use a fake `HttpMessageHandler` that captures method, URL, headers, and request body. Cover:
- `ListFolderAsync` sends `PROPFIND` to `/remote.php/dav/files/alice/Reports` with `Depth: 1`.
- `DownloadAsync` sends `GET` to `/remote.php/dav/files/alice/Reports/report.docx`.
- `UploadAsync` sends `PUT` to the destination URL with `Content-Type`.
- `MoveAsync` sends `MOVE` with `Destination` and `Overwrite: F`.
- `DeleteToTrashAsync` sends `DELETE` to the source URL.
- `ListTrashAsync` sends `PROPFIND` to `/remote.php/dav/trashbin/alice/trash`.
- `RestoreTrashAsync` sends `MOVE` to `/remote.php/dav/trashbin/alice/restore`.
- `ListVersionsAsync` sends `PROPFIND` to `/remote.php/dav/versions/alice/versions/{fileId}`.
- `RestoreVersionAsync` sends `MOVE` to `/remote.php/dav/versions/alice/restore`.

- [ ] **Step 5: Implement adapter**

`NextcloudFileProviderAdapter` uses `HttpClient.SendAsync` so WebDAV verbs can be represented:

```csharp
private static readonly HttpMethod PropFind = new("PROPFIND");
private static readonly HttpMethod Move = new("MOVE");
private static readonly HttpMethod Mkcol = new("MKCOL");

private static string DavRoot(FileProviderConnection connection)
    => $"{(connection.InternalBaseUrl ?? connection.BaseUrl).TrimEnd('/')}/remote.php/dav";

private static string FilesRoot(FileProviderConnection connection)
    => $"{DavRoot(connection)}/files/{Uri.EscapeDataString(connection.Username)}";

private static string FileUrl(FileProviderConnection connection, string path)
    => $"{FilesRoot(connection)}{EscapePath(path)}";
```

Authentication:
- Use HTTP Basic auth with `connection.Username` and `connection.AppPassword`.
- Do not log request headers.

Open links:
- `mode=nextcloud` returns `{BaseUrl}/apps/files/files?dir={parentPath}&openfile={externalFileId}` when the external id is available in the caller.
- `mode=view` and `mode=edit` return the same Nextcloud Files link in this task; Task 5 passes the file id and the UI labels the action distinctly. OnlyOffice stays behind Nextcloud.

- [ ] **Step 6: Run adapter tests**

Run:

```powershell
dotnet test tests/Pim.UnitTests/Pim.UnitTests.csproj --filter "FullyQualifiedName~NextcloudDavXmlParserTests|FullyQualifiedName~NextcloudFileProviderAdapterTests" -v minimal
```

Expected: PASS.

- [ ] **Step 7: Commit**

```bash
git add src/modules/Pim.Module.Files/Providers tests/Pim.UnitTests/Files/NextcloudDavXmlParserTests.cs tests/Pim.UnitTests/Files/NextcloudFileProviderAdapterTests.cs
git commit -m "feat: add nextcloud file provider adapter"
```

## Task 5: File Operations, Sync, Versions, Trash, And Audit

**Files:**
- Create: `src/modules/Pim.Module.Files/Services/FileOperationService.cs`
- Modify: `src/modules/Pim.Module.Files/Services/FileProviderBindingService.cs`
- Modify: `src/modules/Pim.Module.Files/FilesModule.cs`
- Create: `tests/Pim.UnitTests/Files/FileOperationServiceTests.cs`

- [ ] **Step 1: Write operation service tests**

Cover these concrete behaviors:
- `SyncProviderAsync` upserts provider items by `(ProviderId, ExternalFileId)` and updates path/name/etag without changing `Id`.
- `SyncProviderAsync` marks previously seen missing items as `IsDeleted=true`.
- `ListItemsAsync("/")` returns only non-deleted direct children.
- `MoveAsync` and `RenameAsync` call the adapter, preserve `ExternalFileId`, update `Path`, and record audit action `files.move` or `files.rename`.
- `DeleteAsync` calls `DeleteToTrashAsync`, marks local item deleted, and records `files.delete_to_trash`.
- `ListVersionsAsync` stores historical versions and does not create index jobs for `Source == "history"`.
- `RestoreVersionPreviewAsync` returns `RequiresConfirmation=true`.
- `AcceptSuggestionAsync` changes only suggestion status to `accepted` and does not call move/rename/delete/restore adapter methods.

- [ ] **Step 2: Implement file operation service**

`FileOperationService` constructor:

```csharp
public sealed class FileOperationService(
    PimDbContext db,
    ICurrentUserService currentUser,
    IAuditLogService auditLog,
    FileProviderBindingService providerBindings,
    IFileProviderAdapter adapter)
```

Core methods:

```csharp
public Task<PagedResult<FileItemDto>> ListItemsAsync(FileListQuery query, CancellationToken ct = default);
public Task<FileItemDto> GetItemAsync(Guid id, CancellationToken ct = default);
public Task<FileItemDto> UploadAsync(Guid providerId, string destinationPath, Stream content, string contentType, CancellationToken ct = default);
public Task<ProviderDownload> DownloadAsync(Guid id, CancellationToken ct = default);
public Task<FileItemDto> MoveAsync(Guid id, MoveFileRequest request, CancellationToken ct = default);
public Task<FileItemDto> RenameAsync(Guid id, RenameFileRequest request, CancellationToken ct = default);
public Task DeleteAsync(Guid id, CancellationToken ct = default);
public Task<IReadOnlyList<FileItemDto>> SyncProviderAsync(Guid providerId, CancellationToken ct = default);
public Task<IReadOnlyList<ProviderTrashItem>> ListTrashAsync(CancellationToken ct = default);
public Task RestoreTrashAsync(Guid providerId, string trashId, CancellationToken ct = default);
public Task<IReadOnlyList<FileVersionDto>> ListVersionsAsync(Guid id, CancellationToken ct = default);
public Task<ProviderDownload> DownloadVersionAsync(Guid id, Guid versionId, CancellationToken ct = default);
public Task<VersionRestorePreviewDto> RestoreVersionPreviewAsync(Guid id, Guid versionId, CancellationToken ct = default);
public Task RestoreVersionAsync(Guid id, Guid versionId, CancellationToken ct = default);
public Task<FileOpenLinkDto> BuildOpenLinkAsync(Guid id, string? mode, CancellationToken ct = default);
public Task<IReadOnlyList<FileSuggestionDto>> ListSuggestionsAsync(CancellationToken ct = default);
public Task<FileSuggestionDto> DismissSuggestionAsync(Guid id, CancellationToken ct = default);
public Task<FileSuggestionDto> AcceptSuggestionAsync(Guid id, CancellationToken ct = default);
```

Rules:
- Load items by `Id` and current `UserId` through provider ownership.
- Normalize display paths to leading slash and no trailing slash except root.
- Upload destination path must include a file name.
- Rename name must not contain `/`, `\`, or an empty trimmed name.
- Download folders returns `DomainException(5303, "Folders cannot be downloaded through this endpoint")`.
- AI suggestion acceptance updates `Status` to `accepted`, updates `UpdatedAt`, records audit action `files.suggestion_accept`, and performs no provider operation.

- [ ] **Step 3: Wire endpoints**

Replace the 501 file operation endpoints in `FilesModule.MapEndpoints` with service calls. Multipart upload reads fields:
- `providerId`: required GUID.
- `path`: required destination path such as `/Reports/report.docx`.
- `file`: required file field.

Download endpoints return `Results.File(download.Content, download.ContentType, download.FileName)`.

`GET /items/{id}/open-link?mode=view|edit|nextcloud` returns `ApiResponse<FileOpenLinkDto>`.

- [ ] **Step 4: Run operation tests**

Run: `dotnet test tests/Pim.UnitTests/Pim.UnitTests.csproj --filter FullyQualifiedName~FileOperationServiceTests -v minimal`

Expected: PASS.

- [ ] **Step 5: Run backend tests**

Run: `dotnet test Pim.sln`

Expected: PASS with the existing nullable warnings only.

- [ ] **Step 6: Commit**

```bash
git add src/modules/Pim.Module.Files tests/Pim.UnitTests/Files/FileOperationServiceTests.cs
git commit -m "feat: add nextcloud-backed file operations"
```

## Task 6: Current-Version Indexing, Local Embeddings, Qdrant Search

**Files:**
- Create: `src/modules/Pim.Module.Files/Services/FileChunker.cs`
- Create: `src/modules/Pim.Module.Files/Services/IFileEmbeddingService.cs`
- Create: `src/modules/Pim.Module.Files/Services/HashingFileEmbeddingService.cs`
- Create: `src/modules/Pim.Module.Files/Services/QdrantFileVectorStore.cs`
- Create: `src/modules/Pim.Module.Files/Services/FileIndexingService.cs`
- Modify: `src/modules/Pim.Module.Files/FilesModule.cs`
- Modify: `src/Pim.Api/appsettings.json`
- Modify: `src/Pim.Api/appsettings.Development.json`
- Create: `tests/Pim.UnitTests/Files/FileChunkerTests.cs`
- Create: `tests/Pim.UnitTests/Files/FileIndexingServiceTests.cs`
- Create: `tests/Pim.UnitTests/Files/QdrantFileVectorStoreTests.cs`

- [ ] **Step 1: Write chunker tests**

```csharp
using Pim.Module.Files.Services;
using Xunit;

namespace Pim.UnitTests.Files;

public class FileChunkerTests
{
    [Fact]
    public void Chunk_SplitsTextWithOffsetsAndStableHashes()
    {
        var chunks = FileChunker.Chunk("alpha beta gamma delta epsilon", maxChars: 12, overlapChars: 3);

        Assert.True(chunks.Count >= 3);
        Assert.Equal(0, chunks[0].ChunkIndex);
        Assert.Equal(0, chunks[0].StartOffset);
        Assert.True(chunks[0].EndOffset > chunks[0].StartOffset);
        Assert.Equal(64, chunks[0].TextHash.Length);
        Assert.All(chunks, chunk => Assert.False(string.IsNullOrWhiteSpace(chunk.Text)));
    }
}
```

- [ ] **Step 2: Implement chunker**

`FileChunker.Chunk` returns `IReadOnlyList<FileTextChunk>`:

```csharp
public sealed record FileTextChunk(int ChunkIndex, string Text, string TextHash, int StartOffset, int EndOffset);
```

Algorithm:
- Trim control-only text to empty result.
- Use `maxChars` default `1600`, `overlapChars` default `160`.
- Prefer splitting at whitespace before `maxChars`.
- If no whitespace exists in the window, split exactly at `maxChars`.
- Hash each chunk with SHA-256 lowercase hex.

- [ ] **Step 3: Write indexing tests**

Cover:
- Unsupported MIME type creates `skipped` job and no chunks.
- Empty extracted text creates `skipped` job and no vectors.
- Changed current version deletes old chunks/vectors for the file before inserting new ones.
- Historical versions are never indexed unless a future explicit method is added.
- Qdrant payload includes `userId`, `providerId`, `fileId`, `versionId`, `chunkId`, `path`, `mimeType`, and `modifiedAt`.

- [ ] **Step 4: Implement local embedding service**

Create:

```csharp
namespace Pim.Module.Files.Services;

public interface IFileEmbeddingService
{
    int Dimensions { get; }
    Task<float[]> EmbedAsync(string text, CancellationToken ct = default);
}
```

First implementation:
- `HashingFileEmbeddingService` uses a deterministic feature hashing bag-of-words vector.
- Default dimensions: 384.
- Lowercase Unicode text, split on whitespace and punctuation, hash each token to a dimension, increment by `1 / sqrt(tokenCount)`, L2-normalize the vector.
- This is local, deterministic, and replaceable without changing callers.

- [ ] **Step 5: Implement Qdrant vector store**

`QdrantFileVectorStore` methods:

```csharp
public Task EnsureCollectionAsync(CancellationToken ct = default);
public Task UpsertChunksAsync(IReadOnlyList<FileChunkVector> vectors, CancellationToken ct = default);
public Task DeleteFileVectorsAsync(Guid fileItemId, CancellationToken ct = default);
public Task<IReadOnlyList<FileChunkSearchHit>> SearchAsync(float[] vector, Guid userId, string? mode, CancellationToken ct = default);
```

HTTP details:
- `PUT /collections/{collection}` with vector size equal to `IFileEmbeddingService.Dimensions` and distance `Cosine`.
- `PUT /collections/{collection}/points` for upsert.
- `POST /collections/{collection}/points/delete` with filter `must fileId == {fileItemId}`.
- `POST /collections/{collection}/points/search` with `userId` filter before returning results.

- [ ] **Step 6: Implement indexing service**

`FileIndexingService.IndexCurrentVersionAsync(Guid fileItemId)`:
1. Load item with provider and current version.
2. Reject folders with skipped job.
3. Download current file through `FileOperationService.DownloadAsync`.
4. Extract text using existing `TikaClient`.
5. Skip empty text.
6. Delete existing chunks and vectors for this file.
7. Chunk text.
8. Persist chunks.
9. Embed chunks.
10. Upsert to Qdrant.
11. Mark job `succeeded`.

Supported MIME types for first version:
- `text/plain`
- `text/markdown`
- `text/csv`
- `application/pdf`
- `application/vnd.openxmlformats-officedocument.wordprocessingml.document`
- `application/vnd.openxmlformats-officedocument.spreadsheetml.sheet`
- `application/vnd.openxmlformats-officedocument.presentationml.presentation`

- [ ] **Step 7: Wire endpoints and registration**

Register:

```csharp
services.AddScoped<FileIndexingService>();
services.AddSingleton<IFileEmbeddingService, HashingFileEmbeddingService>();
services.AddHttpClient<QdrantFileVectorStore>();
```

Map:
- `POST /items/{id}/index` to `FileIndexingService.IndexCurrentVersionAsync`.
- `GET /search?q=...&mode=keyword|semantic|hybrid` to keyword database search plus Qdrant semantic search.

- [ ] **Step 8: Run indexing tests**

Run:

```powershell
dotnet test tests/Pim.UnitTests/Pim.UnitTests.csproj --filter "FullyQualifiedName~FileChunkerTests|FullyQualifiedName~FileIndexingServiceTests|FullyQualifiedName~QdrantFileVectorStoreTests" -v minimal
```

Expected: PASS.

- [ ] **Step 9: Commit**

```bash
git add src/modules/Pim.Module.Files/Services src/modules/Pim.Module.Files/FilesModule.cs src/Pim.Api/appsettings*.json tests/Pim.UnitTests/Files/FileChunkerTests.cs tests/Pim.UnitTests/Files/FileIndexingServiceTests.cs tests/Pim.UnitTests/Files/QdrantFileVectorStoreTests.cs
git commit -m "feat: index current file versions"
```

## Task 7: File AI Through Unified IAiGateway

**Files:**
- Create: `src/modules/Pim.Module.Files/Services/FileAiService.cs`
- Modify: `src/modules/Pim.Module.Files/Services/FileIndexingService.cs`
- Modify: `src/modules/Pim.Module.Files/FilesModule.cs`
- Create: `tests/Pim.UnitTests/Files/FileAiServiceTests.cs`

- [ ] **Step 1: Confirm gateway interface exists**

Run: `Test-Path src/Pim.Core/Ai/IAiGateway.cs`

Expected: `True`.

If it returns `False`, merge the unified LLM gateway implementation before this task. The files module must not use `HttpClient` to call LiteLLM or upstream model providers.

- [ ] **Step 2: Write AI service tests**

Tests must use a fake `IAiGateway` and assert:
- Summary request uses `Module = "files"`, `Purpose = "file.summary"`, `SourceObjectType = "file"`, and `SourceObjectId = file item id.
- Metadata contains file id, version id, and evidence chunk ids.
- Successful structured summary stores `file_ai_results` with `ai_request_log_id`.
- Organization suggestions create `file_suggestions` with `status = "pending"`.
- Failed gateway result creates no suggestions.
- Suggestion acceptance is still non-executing and belongs to `FileOperationServiceTests`.

- [ ] **Step 3: Implement AI service**

`FileAiService` public methods:

```csharp
public Task<FileAiResultDto?> GenerateSummaryAndTagsAsync(Guid fileItemId, CancellationToken ct = default);
public Task<IReadOnlyList<FileSuggestionDto>> GenerateOrganizationSuggestionsAsync(Guid fileItemId, CancellationToken ct = default);
```

Prompt construction:
- Use up to 8 chunks ordered by `ChunkIndex`.
- Include path, name, MIME type, modified time, and chunk ids.
- Do not include app password, provider credentials, request headers, or raw download URLs.
- Purposes:
  - `file.summary`
  - `file.tags`
  - `file.organization_suggestions`

Structured output shapes:

```json
{
  "summary": "one paragraph",
  "tags": ["tag-one", "tag-two"],
  "language": "zh-CN",
  "sensitivity": "normal"
}
```

```json
{
  "suggestions": [
    {
      "suggestionType": "rename",
      "title": "Use clearer report name",
      "reason": "The document title mentions the final 2026 budget report.",
      "confidence": 0.82,
      "payload": { "proposedName": "2026-budget-final-report.docx" }
    }
  ]
}
```

- [ ] **Step 4: Register schemas with gateway registry**

If the gateway stage exposes `IAiSchemaRegistry`, register:
- `files.summary.v1`
- `files.organization_suggestions.v1`

Register inside `FilesModule.RegisterServices` by adding a scoped initializer or singleton schema registration consistent with the gateway stage. The schema must limit `suggestionType` to `rename`, `move`, `tag`, `duplicate`, `stale`, and `unfiled`.

- [ ] **Step 5: Run AI service tests**

Run: `dotnet test tests/Pim.UnitTests/Pim.UnitTests.csproj --filter FullyQualifiedName~FileAiServiceTests -v minimal`

Expected: PASS.

- [ ] **Step 6: Commit**

```bash
git add src/modules/Pim.Module.Files tests/Pim.UnitTests/Files/FileAiServiceTests.cs
git commit -m "feat: add governed file ai results"
```

## Task 8: Web API Client, Types, And Route Tests

**Files:**
- Create: `src/client-web/src/api/files.ts`
- Modify: `src/client-web/src/types/index.ts`
- Create: `tests/client-web/filesApiPath.test.ts`
- Create: `tests/client-web/filesTypes.test.ts`
- Create: `tests/client-web/tsconfig.files.json`
- Modify: `src/client-web/package.json`

- [ ] **Step 1: Write failing API path tests**

```ts
import assert from 'node:assert/strict';
import { fileApiPaths } from '../../src/client-web/src/api/files';

assert.equal(fileApiPaths.providers(), '/files/providers');
assert.equal(fileApiPaths.bindNextcloud(), '/files/providers/nextcloud');
assert.equal(fileApiPaths.providerTest('11111111-1111-1111-1111-111111111111'), '/files/providers/11111111-1111-1111-1111-111111111111/test');
assert.equal(fileApiPaths.items('/Reports'), '/files/items?path=%2FReports');
assert.equal(fileApiPaths.item('22222222-2222-2222-2222-222222222222'), '/files/items/22222222-2222-2222-2222-222222222222');
assert.equal(fileApiPaths.itemDownload('22222222-2222-2222-2222-222222222222'), '/files/items/22222222-2222-2222-2222-222222222222/download');
assert.equal(fileApiPaths.move('22222222-2222-2222-2222-222222222222'), '/files/items/22222222-2222-2222-2222-222222222222/move');
assert.equal(fileApiPaths.rename('22222222-2222-2222-2222-222222222222'), '/files/items/22222222-2222-2222-2222-222222222222/rename');
assert.equal(fileApiPaths.versions('22222222-2222-2222-2222-222222222222'), '/files/items/22222222-2222-2222-2222-222222222222/versions');
assert.equal(fileApiPaths.versionRestore('22222222-2222-2222-2222-222222222222', '33333333-3333-3333-3333-333333333333'), '/files/items/22222222-2222-2222-2222-222222222222/versions/33333333-3333-3333-3333-333333333333/restore');
assert.equal(fileApiPaths.search('budget report', 'hybrid'), '/files/search?q=budget+report&mode=hybrid');
assert.equal(fileApiPaths.suggestions(), '/files/suggestions');
```

- [ ] **Step 2: Implement file API paths and calls**

Create `files.ts`:

```ts
import { apiDelete, apiDownloadBlob, apiGet, apiPost, apiUpload } from './client';
import type {
  ApiResponse,
  BindNextcloudProviderRequest,
  FileItem,
  FileOpenLink,
  FileProvider,
  FileProviderTest,
  FileSearchResult,
  FileSuggestion,
  FileVersion,
  MoveFileRequest,
  RenameFileRequest,
  VersionRestorePreview,
} from '../types';

export const fileApiPaths = {
  providers: () => '/files/providers',
  bindNextcloud: () => '/files/providers/nextcloud',
  providerTest: (id: string) => `/files/providers/${id}/test`,
  providerSync: (id: string) => `/files/providers/${id}/sync`,
  items: (path = '/') => `/files/items?${new URLSearchParams({ path }).toString()}`,
  item: (id: string) => `/files/items/${id}`,
  upload: () => '/files/items/upload',
  itemDownload: (id: string) => `/files/items/${id}/download`,
  move: (id: string) => `/files/items/${id}/move`,
  rename: (id: string) => `/files/items/${id}/rename`,
  trash: () => '/files/trash',
  trashRestore: (id: string) => `/files/trash/${id}/restore`,
  versions: (id: string) => `/files/items/${id}/versions`,
  versionDownload: (id: string, versionId: string) => `/files/items/${id}/versions/${versionId}/download`,
  versionRestorePreview: (id: string, versionId: string) => `/files/items/${id}/versions/${versionId}/restore-preview`,
  versionRestore: (id: string, versionId: string) => `/files/items/${id}/versions/${versionId}/restore`,
  index: (id: string) => `/files/items/${id}/index`,
  search: (q: string, mode: 'keyword' | 'semantic' | 'hybrid') => `/files/search?${new URLSearchParams({ q, mode }).toString()}`,
  suggestions: () => '/files/suggestions',
  dismissSuggestion: (id: string) => `/files/suggestions/${id}/dismiss`,
  acceptSuggestion: (id: string) => `/files/suggestions/${id}/accept`,
  openLink: (id: string, mode: 'view' | 'edit' | 'nextcloud') => `/files/items/${id}/open-link?${new URLSearchParams({ mode }).toString()}`,
} as const;

export function getFileProviders() {
  return apiGet<ApiResponse<FileProvider[]>>(fileApiPaths.providers()).then(r => r.data);
}

export function bindNextcloudProvider(data: BindNextcloudProviderRequest) {
  return apiPost<ApiResponse<FileProvider>>(fileApiPaths.bindNextcloud(), data).then(r => r.data);
}

export function testFileProvider(id: string) {
  return apiPost<ApiResponse<FileProviderTest>>(fileApiPaths.providerTest(id), {}).then(r => r.data);
}

export function syncFileProvider(id: string) {
  return apiPost<ApiResponse<FileItem[]>>(fileApiPaths.providerSync(id), {}).then(r => r.data);
}

export function getFileItems(path = '/') {
  return apiGet<ApiResponse<FileItem[]>>(fileApiPaths.items(path)).then(r => r.data);
}

export function getFileItem(id: string) {
  return apiGet<ApiResponse<FileItem>>(fileApiPaths.item(id)).then(r => r.data);
}

export function uploadFile(providerId: string, path: string, file: File) {
  const form = new FormData();
  form.append('providerId', providerId);
  form.append('path', path);
  form.append('file', file);
  return apiUpload<ApiResponse<FileItem>>(fileApiPaths.upload(), form).then(r => r.data);
}

export function downloadFileBlob(id: string): Promise<Blob> {
  return apiDownloadBlob(fileApiPaths.itemDownload(id));
}

export function moveFile(id: string, data: MoveFileRequest) {
  return apiPost<ApiResponse<FileItem>>(fileApiPaths.move(id), data).then(r => r.data);
}

export function renameFile(id: string, data: RenameFileRequest) {
  return apiPost<ApiResponse<FileItem>>(fileApiPaths.rename(id), data).then(r => r.data);
}

export function deleteFile(id: string) {
  return apiDelete<ApiResponse<string>>(fileApiPaths.item(id)).then(r => r.data);
}

export function getFileVersions(id: string) {
  return apiGet<ApiResponse<FileVersion[]>>(fileApiPaths.versions(id)).then(r => r.data);
}

export function restoreFileVersionPreview(id: string, versionId: string) {
  return apiPost<ApiResponse<VersionRestorePreview>>(fileApiPaths.versionRestorePreview(id, versionId), {}).then(r => r.data);
}

export function restoreFileVersion(id: string, versionId: string) {
  return apiPost<ApiResponse<string>>(fileApiPaths.versionRestore(id, versionId), {}).then(r => r.data);
}

export function indexFile(id: string) {
  return apiPost<ApiResponse<unknown>>(fileApiPaths.index(id), {}).then(r => r.data);
}

export function searchFiles(q: string, mode: 'keyword' | 'semantic' | 'hybrid') {
  return apiGet<ApiResponse<FileSearchResult>>(fileApiPaths.search(q, mode)).then(r => r.data);
}

export function getFileSuggestions() {
  return apiGet<ApiResponse<FileSuggestion[]>>(fileApiPaths.suggestions()).then(r => r.data);
}

export function dismissFileSuggestion(id: string) {
  return apiPost<ApiResponse<FileSuggestion>>(fileApiPaths.dismissSuggestion(id), {}).then(r => r.data);
}

export function acceptFileSuggestion(id: string) {
  return apiPost<ApiResponse<FileSuggestion>>(fileApiPaths.acceptSuggestion(id), {}).then(r => r.data);
}

export function getFileOpenLink(id: string, mode: 'view' | 'edit' | 'nextcloud') {
  return apiGet<ApiResponse<FileOpenLink>>(fileApiPaths.openLink(id, mode)).then(r => r.data);
}
```

- [ ] **Step 3: Add TypeScript types**

Append exact DTO types to `src/client-web/src/types/index.ts`.

- [ ] **Step 4: Add test scripts**

Add:

```json
"test:files": "cd ../.. && npm --prefix src/client-web exec tsx -- tests/client-web/filesApiPath.test.ts && npm --prefix src/client-web exec tsc -- -p tests/client-web/tsconfig.files.json && npm --prefix src/client-web exec tsx -- tests/client-web/filesTypes.test.ts"
```

- [ ] **Step 5: Run Web API tests**

Run: `npm --prefix src/client-web run test:files`

Expected: PASS.

- [ ] **Step 6: Commit**

```bash
git add src/client-web/src/api/files.ts src/client-web/src/types/index.ts tests/client-web/filesApiPath.test.ts tests/client-web/filesTypes.test.ts tests/client-web/tsconfig.files.json src/client-web/package.json
git commit -m "feat: add web file api client"
```

## Task 9: Files Web Control Console

**Files:**
- Create: `src/client-web/src/pages/FilesPage.tsx`
- Modify: `src/client-web/src/layout/AppLayout.tsx`
- Modify: `src/client-web/src/layout/Sidebar.tsx`

- [ ] **Step 1: Implement `/files` page**

Build one work-focused console, not a landing page:
- Left rail: provider status, bind form, test action, sync action, folder tree.
- Main pane: breadcrumb, search input, upload action, sortable file list.
- Detail pane: metadata, version state, index state, summary, tags, suggestions, actions.

Actions:
- View is the default primary action.
- Edit is a separate action with caution text for OOXML files.
- Download, upload, move, rename, delete to trash, restore version, open in Nextcloud.
- Suggestions show Accept and Dismiss; Accept says it only marks the suggestion as useful.

Use existing app styling: compact bordered sections, white backgrounds, slate/blue/emerald/red status tones, no nested cards.

- [ ] **Step 2: Wire route**

In `AppLayout.tsx`:

```tsx
const FilesPage = lazy(() => import('../pages/FilesPage'));
```

Add route:

```tsx
<Route path="/files" element={<FilesPage />} />
```

- [ ] **Step 3: Add sidebar entry**

Add nav item:

```ts
{ label: '文件', path: '/files', short: '文' },
```

- [ ] **Step 4: Run Web build**

Run: `npm --prefix src/client-web run build`

Expected: PASS. Vite may keep the existing large chunk warning.

- [ ] **Step 5: Commit**

```bash
git add src/client-web/src/pages/FilesPage.tsx src/client-web/src/layout/AppLayout.tsx src/client-web/src/layout/Sidebar.tsx
git commit -m "feat: add files web console"
```

## Task 10: Docker Compose And Configuration

**Files:**
- Modify: `docker-compose.yml`
- Modify: `.env.example`
- Modify: `src/Pim.Api/appsettings.json`
- Modify: `src/Pim.Api/appsettings.Development.json`

- [ ] **Step 1: Add configuration defaults**

Production `appsettings.json`:

```json
"Nextcloud": {
  "PublicBaseUrl": "http://127.0.0.1:8080",
  "InternalBaseUrl": "http://nextcloud"
},
"OnlyOffice": {
  "PublicUrl": "http://127.0.0.1:8082",
  "JwtSecret": ""
},
"Qdrant": {
  "Url": "http://qdrant:6333",
  "Collection": "pim_file_chunks"
},
"Embedding": {
  "Provider": "hashing",
  "Dimensions": 384
},
"Files": {
  "MaxInlineTextBytes": 1048576,
  "AiDisabledPathPatterns": ["/Secrets/*", "/Passwords/*"]
}
```

Development `appsettings.Development.json` uses localhost ports:
- `Nextcloud:PublicBaseUrl = http://localhost:8080`
- `Nextcloud:InternalBaseUrl = http://localhost:8080`
- `Qdrant:Url = http://localhost:6333`

- [ ] **Step 2: Add compose services**

Add services:
- `nextcloud-db` using `postgres:16-alpine`.
- `redis` using `redis:7-alpine`.
- `nextcloud` using `nextcloud:apache`, depends on `nextcloud-db` and `redis`, published on `127.0.0.1:8080:80`.
- `onlyoffice` using `onlyoffice/documentserver`, published on `127.0.0.1:8082:80`, with JWT secret env.
- `qdrant` using `qdrant/qdrant:latest`, published on `127.0.0.1:6333:6333`.
- `litellm` only if the unified gateway stage has not already added it; otherwise keep the gateway stage definition and only connect `pim-api` to it.

Add `pim-api` environment:
- `Nextcloud__PublicBaseUrl=${NEXTCLOUD_PUBLIC_BASE_URL}`
- `Nextcloud__InternalBaseUrl=http://nextcloud`
- `OnlyOffice__PublicUrl=${ONLYOFFICE_PUBLIC_URL}`
- `OnlyOffice__JwtSecret=${ONLYOFFICE_JWT_SECRET}`
- `Qdrant__Url=http://qdrant:6333`
- `Qdrant__Collection=pim_file_chunks`

Add volumes:
- `nextcloud_data`
- `nextcloud_db_data`
- `qdrant_data`

- [ ] **Step 3: Add env example values**

`.env.example`:

```env
NEXTCLOUD_ADMIN_USER=admin
NEXTCLOUD_ADMIN_PASSWORD=change_me_nextcloud_admin
NEXTCLOUD_DB_PASSWORD=change_me_nextcloud_db
NEXTCLOUD_PUBLIC_BASE_URL=http://127.0.0.1:8080
ONLYOFFICE_PUBLIC_URL=http://127.0.0.1:8082
ONLYOFFICE_JWT_SECRET=change_me_onlyoffice_jwt
LITELLM_MASTER_KEY=change_me_litellm_master_key
PIM_LITELLM_VIRTUAL_KEY=change_me_pim_litellm_virtual_key
```

- [ ] **Step 4: Validate compose config**

Run: `docker compose config`

Expected: Compose renders without schema errors.

- [ ] **Step 5: Commit**

```bash
git add docker-compose.yml .env.example src/Pim.Api/appsettings.json src/Pim.Api/appsettings.Development.json
git commit -m "feat: add files infrastructure services"
```

## Task 11: Full Verification

**Files:**
- No source files unless a verification step exposes a defect.

- [ ] **Step 1: Run backend tests**

Run: `dotnet test Pim.sln`

Expected: PASS.

- [ ] **Step 2: Run Web file tests**

Run: `npm --prefix src/client-web run test:files`

Expected: PASS.

- [ ] **Step 3: Run Web build**

Run: `npm --prefix src/client-web run build`

Expected: PASS. Do not commit `src/Pim.Api/wwwroot` build output.

- [ ] **Step 4: Verify generated outputs are not staged**

Run: `git status --short`

Expected source changes only. Exclude:
- `src/Pim.Api/wwwroot/`
- `src/**/bin/`
- `src/**/obj/`
- `tests/**/bin/`
- `tests/**/obj/`
- `src/client-web/node_modules/`
- `src/client-web/dist/`

- [ ] **Step 5: Manual smoke test**

Run:

```powershell
docker compose up -d postgres minio tika qdrant nextcloud nextcloud-db redis onlyoffice pim-api
```

Manual checks:
1. Open `http://127.0.0.1:5858`.
2. Log in to PIM.
3. Open `/files`.
4. Bind a Nextcloud app password.
5. Test provider connection.
6. Sync metadata.
7. Browse root and one folder.
8. Upload `sample.txt`.
9. Download `sample.txt`.
10. Rename and move it.
11. Delete to trash and restore from trash.
12. Open version history.
13. Restore a version after preview.
14. Open a `.docx` in View.
15. Open the same `.docx` in Edit through Nextcloud/OnlyOffice.
16. Index `sample.txt`.
17. Search with keyword, semantic, and hybrid modes.
18. Generate file summary, tags, and suggestions.
19. Confirm `ai_request_logs` contains the file id, version id, purpose, prompt, output, token usage, and schema result.
20. Accept an AI rename suggestion and confirm the file was not renamed.

- [ ] **Step 6: Final commit if verification required fixes**

```bash
git add <fixed-source-files>
git commit -m "fix: stabilize files verification"
```

## Self-Review Notes

Spec coverage:
- Nextcloud, OnlyOffice, Qdrant, Tika, and LiteLLM compose wiring is covered in Task 10.
- Per-user Nextcloud app password binding is covered in Task 3 with protected storage.
- Stable file and version ids are covered in Task 2 and Task 5.
- Browse/upload/download/move/rename/delete/trash/versions/open links are covered in Task 5.
- Current-version indexing, local text extraction, local embeddings, and Qdrant are covered in Task 6.
- `IAiGateway` usage and non-executing suggestions are covered in Task 7 and Task 5.
- Web `/files` console is covered in Tasks 8 and 9.
- Task/event/Today/project binding, Android UI, MCP tools, iframe embedding, and full historical-vector indexing remain outside this plan.

Placeholder scan:
- The plan contains no unresolved placeholders, no open-ended error handling request, and no direct provider LLM calls.

Type consistency:
- Backend DTO names match Web type names.
- Provider adapter records are consumed by binding, operations, indexing, and tests.
- Suggestion statuses are `pending`, `dismissed`, and `accepted`; acceptance does not execute file operations.
