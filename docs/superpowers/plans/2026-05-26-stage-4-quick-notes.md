# Stage 4 Quick Notes Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 实现第 4 阶段快速记录基础版，让用户可以从任意 Web 页面保存 Markdown 文字和正文内附件，并在独立“快速记录”页面统一管理。

**Architecture:** 新增 `Pim.Module.QuickNotes` 模块承载实体、DTO、服务和 API；服务端保存 `content_markdown` 作为事实来源，附件通过稳定 ID 和对象存储 key 管理。Web 使用 MDXEditor 提供全局常住可拖拽悬浮面板和 `/quick-notes` 全页管理体验，所有状态变更走服务端 API。

**Tech Stack:** .NET 8, ASP.NET Core minimal APIs, EF Core/Npgsql, EF Core InMemory tests, xUnit, MinIO storage wrapper, React 19, TypeScript, TanStack Query, MDXEditor, Vite.

---

## Scope Check

这份 spec 聚焦一个能力包：快速记录基础版。它包含后端模型/API、附件上传下载、Web 悬浮捕获和全页管理；这些部分共享同一数据模型和 API，不能拆成彼此独立的子项目。

本计划不实现 AI 分类、任务/日程自动创建、MCP server、文件系统正式集成、孤儿附件后台清理任务。

## File Structure

新增后端项目和文件：

- `src/modules/Pim.Module.QuickNotes/Pim.Module.QuickNotes.csproj`: 快速记录模块项目。
- `src/modules/Pim.Module.QuickNotes/QuickNotesModule.cs`: 注册服务、注册 EF 配置、映射 `/api/v1/quick-notes` 端点。
- `src/modules/Pim.Module.QuickNotes/Entities/QuickNoteEntity.cs`: `quick_notes` 实体。
- `src/modules/Pim.Module.QuickNotes/Entities/QuickNoteAttachmentEntity.cs`: `quick_note_attachments` 实体。
- `src/modules/Pim.Module.QuickNotes/Entities/QuickNoteEntityConfigurations.cs`: EF 索引、默认值、软删除过滤。
- `src/modules/Pim.Module.QuickNotes/DTOs/QuickNoteDtos.cs`: 请求/响应 DTO。
- `src/modules/Pim.Module.QuickNotes/Services/QuickNoteMarkdownReferences.cs`: 从 Markdown 中提取附件 ID。
- `src/modules/Pim.Module.QuickNotes/Services/IQuickNoteObjectStorage.cs`: 附件对象存储抽象，便于测试。
- `src/modules/Pim.Module.QuickNotes/Services/MinioQuickNoteObjectStorage.cs`: 使用现有 `MinioStorage` 的对象存储适配器。
- `src/modules/Pim.Module.QuickNotes/Services/QuickNoteAttachmentService.cs`: 上传、下载、删除/解绑附件。
- `src/modules/Pim.Module.QuickNotes/Services/QuickNoteService.cs`: 创建、列表、详情、更新、处理、归档、恢复、软删除。

修改后端项目和文件：

- `Pim.sln`: 加入 `Pim.Module.QuickNotes`。
- `src/Pim.Api/Pim.Api.csproj`: 引用新模块，确保模块 DLL 进入 API 输出目录。
- `tests/Pim.UnitTests/Pim.UnitTests.csproj`: 引用新模块用于测试。
- `src/Pim.Infrastructure/Data/Migrations/*`: 新增 EF migration 和更新 model snapshot。

新增后端测试：

- `tests/Pim.UnitTests/QuickNotes/QuickNoteModelTests.cs`: 模型、默认值、软删除过滤。
- `tests/Pim.UnitTests/QuickNotes/QuickNoteServiceTests.cs`: 记录 CRUD、状态流转、审计。
- `tests/Pim.UnitTests/QuickNotes/QuickNoteAttachmentServiceTests.cs`: 上传、下载、绑定、跨用户隔离。
- `tests/Pim.UnitTests/QuickNotes/QuickNoteEndpointPathTests.cs`: API path 常量稳定性。

新增前端文件：

- `src/client-web/src/api/quickNotes.ts`: API path、列表、详情、创建、更新、状态操作、附件上传下载。
- `src/client-web/src/components/quick-notes/QuickNoteEditor.tsx`: MDXEditor 封装。
- `src/client-web/src/components/quick-notes/QuickNoteMarkdownPreview.tsx`: 只读预览和附件链接块渲染。
- `src/client-web/src/components/quick-notes/QuickNoteFloatingButton.tsx`: 全局入口按钮。
- `src/client-web/src/components/quick-notes/QuickNoteFloatingPanel.tsx`: 常住可拖拽悬浮面板。
- `src/client-web/src/components/quick-notes/quickNoteFloatingState.ts`: 面板位置和草稿本地状态工具。
- `src/client-web/src/pages/QuickNotesPage.tsx`: 独立管理页面。

修改前端文件：

- `src/client-web/package.json`: 安装 MDXEditor 依赖。
- `src/client-web/src/types/index.ts`: 增加 Quick Notes 类型。
- `src/client-web/src/layout/AppLayout.tsx`: 挂载全局浮窗和 `/quick-notes` 路由。
- `src/client-web/src/layout/Sidebar.tsx`: 增加“快速记录”导航项。

新增前端测试：

- `tests/client-web/quickNotesApiPath.test.ts`: API path 构造测试。
- `tests/client-web/quickNotesTypes.test.ts`: DTO 类型测试。
- `tests/client-web/quickNoteFloatingState.test.ts`: 面板拖拽位置 clamp、草稿存储 key 测试。
- `tests/client-web/tsconfig.quick-notes.json`: 编译 Quick Notes 类型测试。

新增文档：

- `docs/operations/quick-notes-stage4-acceptance.md`: 手动验收清单。

---

### Task 1: 新增 QuickNotes 模块项目与实体模型

**Files:**
- Create: `src/modules/Pim.Module.QuickNotes/Pim.Module.QuickNotes.csproj`
- Create: `src/modules/Pim.Module.QuickNotes/Entities/QuickNoteEntity.cs`
- Create: `src/modules/Pim.Module.QuickNotes/Entities/QuickNoteAttachmentEntity.cs`
- Create: `src/modules/Pim.Module.QuickNotes/Entities/QuickNoteEntityConfigurations.cs`
- Create: `src/modules/Pim.Module.QuickNotes/DTOs/QuickNoteDtos.cs`
- Create: `src/modules/Pim.Module.QuickNotes/QuickNotesModule.cs`
- Modify: `Pim.sln`
- Modify: `src/Pim.Api/Pim.Api.csproj`
- Modify: `tests/Pim.UnitTests/Pim.UnitTests.csproj`
- Test: `tests/Pim.UnitTests/QuickNotes/QuickNoteModelTests.cs`

- [ ] **Step 1: 编写失败的模型测试**

Create `tests/Pim.UnitTests/QuickNotes/QuickNoteModelTests.cs`:

```csharp
using Microsoft.EntityFrameworkCore;
using Pim.Infrastructure.Data;
using Pim.Module.QuickNotes.Entities;
using Xunit;

namespace Pim.UnitTests.QuickNotes;

public class QuickNoteModelTests
{
    [Fact]
    public async Task QuickNote_DefaultsToInboxAndFiltersSoftDeletedRows()
    {
        PimDbContext.RegisterModuleAssembly(typeof(QuickNoteEntity).Assembly);
        await using var db = CreateDb();
        var userId = Guid.Parse("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");
        var active = new QuickNoteEntity
        {
            UserId = userId,
            ContentMarkdown = "active note",
            Source = "web-page"
        };
        var deleted = new QuickNoteEntity
        {
            UserId = userId,
            ContentMarkdown = "deleted note",
            Source = "web-page",
            DeletedAt = DateTimeOffset.UtcNow
        };

        db.Set<QuickNoteEntity>().AddRange(active, deleted);
        await db.SaveChangesAsync();

        var notes = await db.Set<QuickNoteEntity>().ToListAsync();

        var note = Assert.Single(notes);
        Assert.Equal(active.Id, note.Id);
        Assert.Equal(QuickNoteStatuses.Inbox, note.Status);
        Assert.Equal("{}", note.MetadataJson);
    }

    [Fact]
    public async Task QuickNoteAttachment_CanBeTemporaryBeforeNoteSave()
    {
        PimDbContext.RegisterModuleAssembly(typeof(QuickNoteEntity).Assembly);
        await using var db = CreateDb();
        var userId = Guid.Parse("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");
        var attachment = new QuickNoteAttachmentEntity
        {
            UserId = userId,
            StorageProvider = "minio",
            ObjectKey = "quick-notes/aaaaaaaa/file.txt",
            FileName = "file.txt",
            ContentType = "text/plain",
            SizeBytes = 12
        };

        db.Set<QuickNoteAttachmentEntity>().Add(attachment);
        await db.SaveChangesAsync();

        var saved = await db.Set<QuickNoteAttachmentEntity>().SingleAsync();
        Assert.Null(saved.QuickNoteId);
        Assert.Equal("{}", saved.MetadataJson);
    }

    private static PimDbContext CreateDb()
    {
        var options = new DbContextOptionsBuilder<PimDbContext>()
            .UseInMemoryDatabase($"quick-note-model-{Guid.NewGuid()}")
            .Options;
        return new PimDbContext(options);
    }
}
```

- [ ] **Step 2: 运行测试确认失败**

Run:

```powershell
dotnet test tests/Pim.UnitTests/Pim.UnitTests.csproj --filter FullyQualifiedName~QuickNoteModelTests
```

Expected: FAIL，错误包含 `Pim.Module.QuickNotes` 或 `QuickNoteEntity` 不存在。

- [ ] **Step 3: 创建模块项目文件**

Create `src/modules/Pim.Module.QuickNotes/Pim.Module.QuickNotes.csproj`:

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

- [ ] **Step 4: 创建 Quick Note 实体和状态常量**

Create `src/modules/Pim.Module.QuickNotes/Entities/QuickNoteEntity.cs`:

```csharp
using System.ComponentModel.DataAnnotations;
using System.ComponentModel.DataAnnotations.Schema;
using Pim.Core.Data;

namespace Pim.Module.QuickNotes.Entities;

public static class QuickNoteStatuses
{
    public const string Inbox = "inbox";
    public const string Processed = "processed";
    public const string Archived = "archived";

    public static bool IsValid(string status)
        => status is Inbox or Processed or Archived;
}

public static class QuickNoteSources
{
    public const string WebFloating = "web-floating";
    public const string WebPage = "web-page";
}

[Table("quick_notes")]
public class QuickNoteEntity : ISoftDeletable
{
    [Key]
    [Column("id")]
    public Guid Id { get; set; } = Guid.NewGuid();

    [Column("user_id")]
    public Guid UserId { get; set; }

    [Column("content_markdown")]
    public string ContentMarkdown { get; set; } = string.Empty;

    [Column("status")]
    [MaxLength(32)]
    public string Status { get; set; } = QuickNoteStatuses.Inbox;

    [Column("source")]
    [MaxLength(64)]
    public string Source { get; set; } = QuickNoteSources.WebPage;

    [Column("metadata_json", TypeName = "jsonb")]
    public string MetadataJson { get; set; } = "{}";

    [Column("created_at")]
    public DateTimeOffset CreatedAt { get; set; } = DateTimeOffset.UtcNow;

    [Column("updated_at")]
    public DateTimeOffset UpdatedAt { get; set; } = DateTimeOffset.UtcNow;

    [Column("archived_at")]
    public DateTimeOffset? ArchivedAt { get; set; }

    [Column("deleted_at")]
    public DateTimeOffset? DeletedAt { get; set; }

    public ICollection<QuickNoteAttachmentEntity> Attachments { get; set; } = new List<QuickNoteAttachmentEntity>();
}
```

- [ ] **Step 5: 创建附件实体**

Create `src/modules/Pim.Module.QuickNotes/Entities/QuickNoteAttachmentEntity.cs`:

```csharp
using System.ComponentModel.DataAnnotations;
using System.ComponentModel.DataAnnotations.Schema;
using Pim.Core.Data;

namespace Pim.Module.QuickNotes.Entities;

[Table("quick_note_attachments")]
public class QuickNoteAttachmentEntity : ISoftDeletable
{
    [Key]
    [Column("id")]
    public Guid Id { get; set; } = Guid.NewGuid();

    [Column("quick_note_id")]
    public Guid? QuickNoteId { get; set; }

    [Column("user_id")]
    public Guid UserId { get; set; }

    [Column("storage_provider")]
    [MaxLength(32)]
    public string StorageProvider { get; set; } = "minio";

    [Column("object_key")]
    public string ObjectKey { get; set; } = string.Empty;

    [Column("file_name")]
    public string FileName { get; set; } = string.Empty;

    [Column("content_type")]
    [MaxLength(255)]
    public string ContentType { get; set; } = "application/octet-stream";

    [Column("size_bytes")]
    public long SizeBytes { get; set; }

    [Column("content_hash")]
    [MaxLength(128)]
    public string? ContentHash { get; set; }

    [Column("metadata_json", TypeName = "jsonb")]
    public string MetadataJson { get; set; } = "{}";

    [Column("created_at")]
    public DateTimeOffset CreatedAt { get; set; } = DateTimeOffset.UtcNow;

    [Column("deleted_at")]
    public DateTimeOffset? DeletedAt { get; set; }

    [ForeignKey(nameof(QuickNoteId))]
    public QuickNoteEntity? QuickNote { get; set; }
}
```

- [ ] **Step 6: 创建 EF 配置**

Create `src/modules/Pim.Module.QuickNotes/Entities/QuickNoteEntityConfigurations.cs`:

```csharp
using Microsoft.EntityFrameworkCore;
using Microsoft.EntityFrameworkCore.Metadata.Builders;

namespace Pim.Module.QuickNotes.Entities;

public sealed class QuickNoteEntityConfiguration : IEntityTypeConfiguration<QuickNoteEntity>
{
    public void Configure(EntityTypeBuilder<QuickNoteEntity> builder)
    {
        builder.HasQueryFilter(n => n.DeletedAt == null);
        builder.Property(n => n.ContentMarkdown).HasDefaultValue("");
        builder.Property(n => n.Status).HasDefaultValue(QuickNoteStatuses.Inbox);
        builder.Property(n => n.Source).HasDefaultValue(QuickNoteSources.WebPage);
        builder.Property(n => n.MetadataJson).HasDefaultValue("{}");
        builder.Property(n => n.CreatedAt).HasDefaultValueSql("now()");
        builder.Property(n => n.UpdatedAt).HasDefaultValueSql("now()");
        builder.HasIndex(n => new { n.UserId, n.Status, n.UpdatedAt });
        builder.HasIndex(n => new { n.UserId, n.CreatedAt });
        builder.HasMany(n => n.Attachments)
            .WithOne(a => a.QuickNote)
            .HasForeignKey(a => a.QuickNoteId)
            .OnDelete(DeleteBehavior.SetNull);
    }
}

public sealed class QuickNoteAttachmentEntityConfiguration : IEntityTypeConfiguration<QuickNoteAttachmentEntity>
{
    public void Configure(EntityTypeBuilder<QuickNoteAttachmentEntity> builder)
    {
        builder.HasQueryFilter(a => a.DeletedAt == null);
        builder.Property(a => a.StorageProvider).HasDefaultValue("minio");
        builder.Property(a => a.ContentType).HasDefaultValue("application/octet-stream");
        builder.Property(a => a.MetadataJson).HasDefaultValue("{}");
        builder.Property(a => a.CreatedAt).HasDefaultValueSql("now()");
        builder.HasIndex(a => a.QuickNoteId);
        builder.HasIndex(a => new { a.UserId, a.CreatedAt });
        builder.HasIndex(a => new { a.UserId, a.DeletedAt });
    }
}
```

- [ ] **Step 7: 创建 DTO 文件**

Create `src/modules/Pim.Module.QuickNotes/DTOs/QuickNoteDtos.cs`:

```csharp
using Pim.Core.Common;

namespace Pim.Module.QuickNotes.DTOs;

public sealed record QuickNoteListItemDto(
    Guid Id,
    string ContentPreview,
    string Status,
    string Source,
    int AttachmentCount,
    DateTimeOffset CreatedAt,
    DateTimeOffset UpdatedAt,
    DateTimeOffset? ArchivedAt);

public sealed record QuickNoteAttachmentDto(
    Guid Id,
    string FileName,
    string ContentType,
    long SizeBytes,
    string DownloadUrl,
    string? PreviewUrl,
    DateTimeOffset CreatedAt);

public sealed record QuickNoteDetailDto(
    Guid Id,
    string ContentMarkdown,
    string Status,
    string Source,
    IReadOnlyList<QuickNoteAttachmentDto> Attachments,
    string MetadataJson,
    DateTimeOffset CreatedAt,
    DateTimeOffset UpdatedAt,
    DateTimeOffset? ArchivedAt);

public sealed record CreateQuickNoteRequest(
    string ContentMarkdown,
    string? Source,
    IReadOnlyList<Guid>? AttachmentIds);

public sealed record UpdateQuickNoteRequest(
    string ContentMarkdown,
    string? Status,
    IReadOnlyList<Guid>? AttachmentIds);

public sealed record RestoreQuickNoteRequest(string Status);

public sealed record QuickNoteAttachmentUploadDto(
    Guid Id,
    string FileName,
    string ContentType,
    long SizeBytes,
    string DownloadUrl,
    string? PreviewUrl);

public sealed record QuickNoteListQuery(
    string? Status,
    string? Search,
    int Page,
    int PageSize);

public sealed record QuickNoteListResponse(PagedResult<QuickNoteListItemDto> Result);
```

- [ ] **Step 8: 创建最小模块类**

Create `src/modules/Pim.Module.QuickNotes/QuickNotesModule.cs`:

```csharp
using System.Reflection;
using Microsoft.AspNetCore.Routing;
using Microsoft.Extensions.Configuration;
using Microsoft.Extensions.DependencyInjection;
using Pim.Core.Modules;
using Pim.Infrastructure.Data;

namespace Pim.Module.QuickNotes;

public sealed class QuickNotesModule : IModule
{
    public string Name => "quick-notes";
    public string Version => "1.0.0";

    public void RegisterServices(IServiceCollection services, IConfiguration configuration)
    {
        PimDbContext.RegisterModuleAssembly(Assembly.GetExecutingAssembly());
    }

    public void MapEndpoints(IEndpointRouteBuilder endpoints)
    {
    }

    public Task InitializeAsync(IServiceProvider serviceProvider)
        => Task.CompletedTask;
}
```

- [ ] **Step 9: 添加项目引用**

Run:

```powershell
dotnet sln Pim.sln add src/modules/Pim.Module.QuickNotes/Pim.Module.QuickNotes.csproj
dotnet add src/Pim.Api/Pim.Api.csproj reference src/modules/Pim.Module.QuickNotes/Pim.Module.QuickNotes.csproj
dotnet add tests/Pim.UnitTests/Pim.UnitTests.csproj reference src/modules/Pim.Module.QuickNotes/Pim.Module.QuickNotes.csproj
```

Expected: 每条命令输出项目或引用已添加。

- [ ] **Step 10: 运行模型测试确认通过**

Run:

```powershell
dotnet test tests/Pim.UnitTests/Pim.UnitTests.csproj --filter FullyQualifiedName~QuickNoteModelTests
```

Expected: PASS。

- [ ] **Step 11: Commit**

```powershell
git add Pim.sln src/Pim.Api/Pim.Api.csproj tests/Pim.UnitTests/Pim.UnitTests.csproj src/modules/Pim.Module.QuickNotes tests/Pim.UnitTests/QuickNotes/QuickNoteModelTests.cs
git commit -m "feat(quick-notes): add module models"
```

---

### Task 2: 实现 Markdown 附件引用解析

**Files:**
- Create: `src/modules/Pim.Module.QuickNotes/Services/QuickNoteMarkdownReferences.cs`
- Test: `tests/Pim.UnitTests/QuickNotes/QuickNoteMarkdownReferenceTests.cs`

- [ ] **Step 1: 编写失败的引用解析测试**

Create `tests/Pim.UnitTests/QuickNotes/QuickNoteMarkdownReferenceTests.cs`:

```csharp
using Pim.Module.QuickNotes.Services;
using Xunit;

namespace Pim.UnitTests.QuickNotes;

public class QuickNoteMarkdownReferenceTests
{
    [Fact]
    public void ExtractAttachmentIds_ReturnsIdsFromImageAndFileLinks()
    {
        var imageId = Guid.Parse("11111111-1111-1111-1111-111111111111");
        var fileId = Guid.Parse("22222222-2222-2222-2222-222222222222");
        var markdown = $"""
        ![shot](/api/v1/quick-notes/attachments/{imageId}/download)
        [proposal.pdf](/api/v1/quick-notes/attachments/{fileId}/download)
        """;

        var ids = QuickNoteMarkdownReferences.ExtractAttachmentIds(markdown);

        Assert.Equal(new[] { imageId, fileId }, ids);
    }

    [Fact]
    public void ExtractAttachmentIds_IgnoresDuplicatesAndInvalidUrls()
    {
        var id = Guid.Parse("11111111-1111-1111-1111-111111111111");
        var markdown = $"""
        ![a](/api/v1/quick-notes/attachments/{id}/download)
        [same](/api/v1/quick-notes/attachments/{id}/download)
        [external](https://example.com/file.pdf)
        [bad](/api/v1/quick-notes/attachments/not-a-guid/download)
        """;

        var ids = QuickNoteMarkdownReferences.ExtractAttachmentIds(markdown);

        var single = Assert.Single(ids);
        Assert.Equal(id, single);
    }
}
```

- [ ] **Step 2: 运行测试确认失败**

Run:

```powershell
dotnet test tests/Pim.UnitTests/Pim.UnitTests.csproj --filter FullyQualifiedName~QuickNoteMarkdownReferenceTests
```

Expected: FAIL，错误包含 `QuickNoteMarkdownReferences` 不存在。

- [ ] **Step 3: 实现解析器**

Create `src/modules/Pim.Module.QuickNotes/Services/QuickNoteMarkdownReferences.cs`:

```csharp
using System.Text.RegularExpressions;

namespace Pim.Module.QuickNotes.Services;

public static partial class QuickNoteMarkdownReferences
{
    public static IReadOnlyList<Guid> ExtractAttachmentIds(string? markdown)
    {
        if (string.IsNullOrWhiteSpace(markdown))
            return Array.Empty<Guid>();

        var ids = new List<Guid>();
        var seen = new HashSet<Guid>();
        foreach (Match match in AttachmentUrlRegex().Matches(markdown))
        {
            if (!Guid.TryParse(match.Groups["id"].Value, out var id))
                continue;

            if (seen.Add(id))
                ids.Add(id);
        }

        return ids;
    }

    [GeneratedRegex(@"/api/v1/quick-notes/attachments/(?<id>[0-9a-fA-F-]{36})/download", RegexOptions.Compiled)]
    private static partial Regex AttachmentUrlRegex();
}
```

- [ ] **Step 4: 运行测试确认通过**

Run:

```powershell
dotnet test tests/Pim.UnitTests/Pim.UnitTests.csproj --filter FullyQualifiedName~QuickNoteMarkdownReferenceTests
```

Expected: PASS。

- [ ] **Step 5: Commit**

```powershell
git add src/modules/Pim.Module.QuickNotes/Services/QuickNoteMarkdownReferences.cs tests/Pim.UnitTests/QuickNotes/QuickNoteMarkdownReferenceTests.cs
git commit -m "feat(quick-notes): parse markdown attachment references"
```

---

### Task 3: 实现 QuickNoteService 记录 CRUD 与状态流转

**Files:**
- Create: `src/modules/Pim.Module.QuickNotes/Services/QuickNoteService.cs`
- Modify: `src/modules/Pim.Module.QuickNotes/QuickNotesModule.cs`
- Test: `tests/Pim.UnitTests/QuickNotes/QuickNoteServiceTests.cs`

- [ ] **Step 1: 编写失败的服务测试**

Create `tests/Pim.UnitTests/QuickNotes/QuickNoteServiceTests.cs`:

```csharp
using Microsoft.EntityFrameworkCore;
using Pim.Core.Exceptions;
using Pim.Infrastructure.Auth;
using Pim.Infrastructure.Data;
using Pim.Infrastructure.Operations;
using Pim.Module.QuickNotes.DTOs;
using Pim.Module.QuickNotes.Entities;
using Pim.Module.QuickNotes.Services;
using Xunit;

namespace Pim.UnitTests.QuickNotes;

public class QuickNoteServiceTests
{
    [Fact]
    public async Task CreateAsync_CreatesInboxNoteAndAuditLog()
    {
        var (service, db, userId) = CreateService();

        var note = await service.CreateAsync(
            new CreateQuickNoteRequest("hello **world**", "web-floating", null),
            CancellationToken.None);

        Assert.Equal("hello **world**", note.ContentMarkdown);
        Assert.Equal(QuickNoteStatuses.Inbox, note.Status);
        Assert.Equal("web-floating", note.Source);
        Assert.Equal(userId, await db.Set<QuickNoteEntity>().Select(n => n.UserId).SingleAsync());
        var audit = await db.AuditLogs.SingleAsync();
        Assert.Equal("quick_notes.create", audit.Action);
        Assert.Equal(note.Id.ToString(), audit.ResourceId);
    }

    [Fact]
    public async Task ListAsync_FiltersByStatusAndSearch()
    {
        var (service, _, _) = CreateService();
        await service.CreateAsync(new CreateQuickNoteRequest("alpha meeting", "web-page", null), CancellationToken.None);
        var beta = await service.CreateAsync(new CreateQuickNoteRequest("beta project", "web-page", null), CancellationToken.None);
        await service.ArchiveAsync(beta.Id, CancellationToken.None);

        var inbox = await service.ListAsync(new QuickNoteListQuery("inbox", "alpha", 1, 30), CancellationToken.None);

        var item = Assert.Single(inbox.Items);
        Assert.Contains("alpha", item.ContentPreview);
        Assert.Equal(QuickNoteStatuses.Inbox, item.Status);
    }

    [Fact]
    public async Task UpdateAsync_RejectsInvalidStatus()
    {
        var (service, _, _) = CreateService();
        var note = await service.CreateAsync(new CreateQuickNoteRequest("note", "web-page", null), CancellationToken.None);

        var ex = await Assert.ThrowsAsync<DomainException>(() =>
            service.UpdateAsync(note.Id, new UpdateQuickNoteRequest("note", "done", null), CancellationToken.None));

        Assert.Equal(04003, ex.ErrorCode);
    }

    [Fact]
    public async Task ProcessArchiveRestoreAndDelete_ApplyExpectedState()
    {
        var (service, db, _) = CreateService();
        var note = await service.CreateAsync(new CreateQuickNoteRequest("note", "web-page", null), CancellationToken.None);

        var processed = await service.ProcessAsync(note.Id, CancellationToken.None);
        Assert.Equal(QuickNoteStatuses.Processed, processed.Status);

        var archived = await service.ArchiveAsync(note.Id, CancellationToken.None);
        Assert.Equal(QuickNoteStatuses.Archived, archived.Status);
        Assert.NotNull(archived.ArchivedAt);

        var restored = await service.RestoreAsync(note.Id, QuickNoteStatuses.Inbox, CancellationToken.None);
        Assert.Equal(QuickNoteStatuses.Inbox, restored.Status);
        Assert.Null(restored.ArchivedAt);

        await service.DeleteAsync(note.Id, CancellationToken.None);
        Assert.Empty(await db.Set<QuickNoteEntity>().ToListAsync());
        Assert.NotNull(await db.Set<QuickNoteEntity>().IgnoreQueryFilters().Where(n => n.Id == note.Id).Select(n => n.DeletedAt).SingleAsync());
    }

    [Fact]
    public async Task GetAsync_RejectsOtherUsersNote()
    {
        var (service, db, _) = CreateService();
        db.Set<QuickNoteEntity>().Add(new QuickNoteEntity
        {
            UserId = Guid.Parse("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb"),
            ContentMarkdown = "secret",
            Source = "web-page"
        });
        await db.SaveChangesAsync();
        var id = await db.Set<QuickNoteEntity>().IgnoreQueryFilters().Select(n => n.Id).SingleAsync();

        var ex = await Assert.ThrowsAsync<DomainException>(() => service.GetAsync(id, CancellationToken.None));

        Assert.Equal(04004, ex.ErrorCode);
    }

    private static (QuickNoteService Service, PimDbContext Db, Guid UserId) CreateService()
    {
        PimDbContext.RegisterModuleAssembly(typeof(QuickNoteEntity).Assembly);
        var options = new DbContextOptionsBuilder<PimDbContext>()
            .UseInMemoryDatabase($"quick-note-service-{Guid.NewGuid()}")
            .Options;
        var db = new PimDbContext(options);
        var userId = Guid.Parse("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");
        var service = new QuickNoteService(
            db,
            new FixedCurrentUserService(userId),
            new AuditLogService(db));
        return (service, db, userId);
    }

    private sealed class FixedCurrentUserService(Guid userId) : ICurrentUserService
    {
        public Guid? UserId { get; } = userId;
        public string? Role => "User";
    }
}
```

- [ ] **Step 2: 运行测试确认失败**

Run:

```powershell
dotnet test tests/Pim.UnitTests/Pim.UnitTests.csproj --filter FullyQualifiedName~QuickNoteServiceTests
```

Expected: FAIL，错误包含 `QuickNoteService` 不存在。

- [ ] **Step 3: 实现 QuickNoteService**

Create `src/modules/Pim.Module.QuickNotes/Services/QuickNoteService.cs`:

```csharp
using Microsoft.EntityFrameworkCore;
using Pim.Core.Common;
using Pim.Core.Exceptions;
using Pim.Core.Operations;
using Pim.Infrastructure.Auth;
using Pim.Infrastructure.Data;
using Pim.Module.QuickNotes.DTOs;
using Pim.Module.QuickNotes.Entities;

namespace Pim.Module.QuickNotes.Services;

public sealed class QuickNoteService
{
    private readonly PimDbContext _db;
    private readonly ICurrentUserService _currentUser;
    private readonly IAuditLogService _auditLog;

    public QuickNoteService(PimDbContext db, ICurrentUserService currentUser, IAuditLogService auditLog)
    {
        _db = db;
        _currentUser = currentUser;
        _auditLog = auditLog;
    }

    private Guid UserId => _currentUser.UserId ?? throw new DomainException(01002, "Not authenticated");

    public async Task<PagedResult<QuickNoteListItemDto>> ListAsync(QuickNoteListQuery query, CancellationToken ct)
    {
        var page = Math.Max(1, query.Page);
        var pageSize = Math.Clamp(query.PageSize, 1, 100);
        var notes = _db.Set<QuickNoteEntity>()
            .Include(n => n.Attachments)
            .Where(n => n.UserId == UserId);

        if (!string.IsNullOrWhiteSpace(query.Status))
        {
            if (!QuickNoteStatuses.IsValid(query.Status))
                throw new DomainException(04003, $"Invalid quick note status: {query.Status}");
            notes = notes.Where(n => n.Status == query.Status);
        }

        if (!string.IsNullOrWhiteSpace(query.Search))
        {
            var search = query.Search.Trim();
            notes = notes.Where(n => n.ContentMarkdown.Contains(search));
        }

        var totalCount = await notes.CountAsync(ct);
        var totalPages = (int)Math.Ceiling(totalCount / (double)pageSize);
        var items = await notes
            .OrderByDescending(n => n.UpdatedAt)
            .Skip((page - 1) * pageSize)
            .Take(pageSize)
            .Select(n => new QuickNoteListItemDto(
                n.Id,
                BuildPreview(n.ContentMarkdown),
                n.Status,
                n.Source,
                n.Attachments.Count(a => a.DeletedAt == null),
                n.CreatedAt,
                n.UpdatedAt,
                n.ArchivedAt))
            .ToListAsync(ct);

        return new PagedResult<QuickNoteListItemDto>(items, page, pageSize, totalCount, totalPages);
    }

    public async Task<QuickNoteDetailDto> GetAsync(Guid id, CancellationToken ct)
        => MapDetail(await LoadOwnedNoteAsync(id, ct));

    public async Task<QuickNoteDetailDto> CreateAsync(CreateQuickNoteRequest request, CancellationToken ct)
    {
        var source = string.IsNullOrWhiteSpace(request.Source) ? QuickNoteSources.WebPage : request.Source.Trim();
        var note = new QuickNoteEntity
        {
            UserId = UserId,
            ContentMarkdown = request.ContentMarkdown ?? string.Empty,
            Status = QuickNoteStatuses.Inbox,
            Source = source,
            UpdatedAt = DateTimeOffset.UtcNow,
            CreatedAt = DateTimeOffset.UtcNow
        };

        _db.Set<QuickNoteEntity>().Add(note);
        await _db.SaveChangesAsync(ct);
        await RecordAuditAsync("quick_notes.create", note.Id, ct);
        return MapDetail(note);
    }

    public async Task<QuickNoteDetailDto> UpdateAsync(Guid id, UpdateQuickNoteRequest request, CancellationToken ct)
    {
        var note = await LoadOwnedNoteAsync(id, ct);
        if (request.Status is not null)
        {
            if (!QuickNoteStatuses.IsValid(request.Status))
                throw new DomainException(04003, $"Invalid quick note status: {request.Status}");
            note.Status = request.Status;
            note.ArchivedAt = request.Status == QuickNoteStatuses.Archived ? DateTimeOffset.UtcNow : null;
        }

        note.ContentMarkdown = request.ContentMarkdown ?? string.Empty;
        note.UpdatedAt = DateTimeOffset.UtcNow;
        await _db.SaveChangesAsync(ct);
        await RecordAuditAsync("quick_notes.update", note.Id, ct);
        return MapDetail(note);
    }

    public async Task<QuickNoteDetailDto> ProcessAsync(Guid id, CancellationToken ct)
    {
        var note = await LoadOwnedNoteAsync(id, ct);
        note.Status = QuickNoteStatuses.Processed;
        note.ArchivedAt = null;
        note.UpdatedAt = DateTimeOffset.UtcNow;
        await _db.SaveChangesAsync(ct);
        await RecordAuditAsync("quick_notes.process", note.Id, ct);
        return MapDetail(note);
    }

    public async Task<QuickNoteDetailDto> ArchiveAsync(Guid id, CancellationToken ct)
    {
        var note = await LoadOwnedNoteAsync(id, ct);
        note.Status = QuickNoteStatuses.Archived;
        note.ArchivedAt = DateTimeOffset.UtcNow;
        note.UpdatedAt = DateTimeOffset.UtcNow;
        await _db.SaveChangesAsync(ct);
        await RecordAuditAsync("quick_notes.archive", note.Id, ct);
        return MapDetail(note);
    }

    public async Task<QuickNoteDetailDto> RestoreAsync(Guid id, string status, CancellationToken ct)
    {
        if (!QuickNoteStatuses.IsValid(status))
            throw new DomainException(04003, $"Invalid quick note status: {status}");

        var note = await LoadOwnedNoteAsync(id, ct);
        note.Status = status;
        note.ArchivedAt = status == QuickNoteStatuses.Archived ? DateTimeOffset.UtcNow : null;
        note.UpdatedAt = DateTimeOffset.UtcNow;
        await _db.SaveChangesAsync(ct);
        await RecordAuditAsync("quick_notes.restore", note.Id, ct);
        return MapDetail(note);
    }

    public async Task DeleteAsync(Guid id, CancellationToken ct)
    {
        var note = await LoadOwnedNoteAsync(id, ct);
        note.DeletedAt = DateTimeOffset.UtcNow;
        note.UpdatedAt = DateTimeOffset.UtcNow;
        foreach (var attachment in note.Attachments)
            attachment.DeletedAt = DateTimeOffset.UtcNow;
        await _db.SaveChangesAsync(ct);
        await RecordAuditAsync("quick_notes.delete", note.Id, ct);
    }

    private async Task<QuickNoteEntity> LoadOwnedNoteAsync(Guid id, CancellationToken ct)
    {
        return await _db.Set<QuickNoteEntity>()
            .Include(n => n.Attachments)
            .FirstOrDefaultAsync(n => n.Id == id && n.UserId == UserId, ct)
            ?? throw new DomainException(04004, "Quick note not found");
    }

    private async Task RecordAuditAsync(string action, Guid noteId, CancellationToken ct)
    {
        await _auditLog.RecordAsync(new CreateAuditLogRequest(
            UserId,
            AuditActorType.User,
            action,
            "quick_note",
            noteId.ToString(),
            "web",
            AuditResult.Success,
            null,
            null,
            null,
            new Dictionary<string, string>(),
            null,
            null), ct);
    }

    private static QuickNoteDetailDto MapDetail(QuickNoteEntity note)
        => new(
            note.Id,
            note.ContentMarkdown,
            note.Status,
            note.Source,
            note.Attachments
                .Where(a => a.DeletedAt == null)
                .OrderBy(a => a.CreatedAt)
                .Select(a => new QuickNoteAttachmentDto(
                    a.Id,
                    a.FileName,
                    a.ContentType,
                    a.SizeBytes,
                    $"/api/v1/quick-notes/attachments/{a.Id}/download",
                    a.ContentType.StartsWith("image/", StringComparison.OrdinalIgnoreCase)
                        ? $"/api/v1/quick-notes/attachments/{a.Id}/download"
                        : null,
                    a.CreatedAt))
                .ToList(),
            note.MetadataJson,
            note.CreatedAt,
            note.UpdatedAt,
            note.ArchivedAt);

    private static string BuildPreview(string markdown)
    {
        var compact = markdown
            .Replace("#", "", StringComparison.Ordinal)
            .Replace("*", "", StringComparison.Ordinal)
            .Replace("_", "", StringComparison.Ordinal)
            .Replace("`", "", StringComparison.Ordinal)
            .Replace("\r", " ", StringComparison.Ordinal)
            .Replace("\n", " ", StringComparison.Ordinal)
            .Trim();
        return compact.Length <= 140 ? compact : compact[..140];
    }
}
```

- [ ] **Step 4: 注册服务**

Modify `src/modules/Pim.Module.QuickNotes/QuickNotesModule.cs`:

```csharp
using System.Reflection;
using Microsoft.AspNetCore.Routing;
using Microsoft.Extensions.Configuration;
using Microsoft.Extensions.DependencyInjection;
using Pim.Core.Modules;
using Pim.Infrastructure.Data;
using Pim.Module.QuickNotes.Services;

namespace Pim.Module.QuickNotes;

public sealed class QuickNotesModule : IModule
{
    public string Name => "quick-notes";
    public string Version => "1.0.0";

    public void RegisterServices(IServiceCollection services, IConfiguration configuration)
    {
        PimDbContext.RegisterModuleAssembly(Assembly.GetExecutingAssembly());
        services.AddScoped<QuickNoteService>();
    }

    public void MapEndpoints(IEndpointRouteBuilder endpoints)
    {
    }

    public Task InitializeAsync(IServiceProvider serviceProvider)
        => Task.CompletedTask;
}
```

- [ ] **Step 5: 运行测试确认通过**

Run:

```powershell
dotnet test tests/Pim.UnitTests/Pim.UnitTests.csproj --filter FullyQualifiedName~QuickNoteServiceTests
```

Expected: PASS。

- [ ] **Step 6: Commit**

```powershell
git add src/modules/Pim.Module.QuickNotes/Services/QuickNoteService.cs src/modules/Pim.Module.QuickNotes/QuickNotesModule.cs tests/Pim.UnitTests/QuickNotes/QuickNoteServiceTests.cs
git commit -m "feat(quick-notes): add note service"
```

---

### Task 4: 实现附件对象存储与附件服务

**Files:**
- Create: `src/modules/Pim.Module.QuickNotes/Services/IQuickNoteObjectStorage.cs`
- Create: `src/modules/Pim.Module.QuickNotes/Services/MinioQuickNoteObjectStorage.cs`
- Create: `src/modules/Pim.Module.QuickNotes/Services/QuickNoteAttachmentService.cs`
- Modify: `src/modules/Pim.Module.QuickNotes/QuickNotesModule.cs`
- Modify: `src/modules/Pim.Module.QuickNotes/Services/QuickNoteService.cs`
- Test: `tests/Pim.UnitTests/QuickNotes/QuickNoteAttachmentServiceTests.cs`

- [ ] **Step 1: 编写失败的附件服务测试**

Create `tests/Pim.UnitTests/QuickNotes/QuickNoteAttachmentServiceTests.cs`:

```csharp
using System.Text;
using Microsoft.EntityFrameworkCore;
using Pim.Core.Exceptions;
using Pim.Infrastructure.Auth;
using Pim.Infrastructure.Data;
using Pim.Infrastructure.Operations;
using Pim.Module.QuickNotes.DTOs;
using Pim.Module.QuickNotes.Entities;
using Pim.Module.QuickNotes.Services;
using Xunit;

namespace Pim.UnitTests.QuickNotes;

public class QuickNoteAttachmentServiceTests
{
    [Fact]
    public async Task UploadAsync_CreatesTemporaryAttachmentAndStoresObject()
    {
        var (attachments, db, storage, userId) = CreateServices();
        await using var stream = new MemoryStream(Encoding.UTF8.GetBytes("hello"));

        var result = await attachments.UploadAsync(stream, "hello.txt", "text/plain", 5, CancellationToken.None);

        Assert.Equal("hello.txt", result.FileName);
        Assert.Equal("text/plain", result.ContentType);
        Assert.Equal(5, result.SizeBytes);
        Assert.Contains($"/api/v1/quick-notes/attachments/{result.Id}/download", result.DownloadUrl);
        var entity = await db.Set<QuickNoteAttachmentEntity>().SingleAsync();
        Assert.Equal(userId, entity.UserId);
        Assert.Null(entity.QuickNoteId);
        Assert.True(storage.Objects.ContainsKey(entity.ObjectKey));
    }

    [Fact]
    public async Task CreateNote_BindsTemporaryAttachmentAndValidatesMarkdownReference()
    {
        var (attachments, db, _, _) = CreateServices();
        var notes = CreateNoteService(db);
        await using var stream = new MemoryStream(Encoding.UTF8.GetBytes("image"));
        var uploaded = await attachments.UploadAsync(stream, "shot.png", "image/png", 5, CancellationToken.None);
        var markdown = $"![shot](/api/v1/quick-notes/attachments/{uploaded.Id}/download)";

        var note = await notes.CreateAsync(
            new CreateQuickNoteRequest(markdown, "web-page", new[] { uploaded.Id }),
            CancellationToken.None);

        var attachment = await db.Set<QuickNoteAttachmentEntity>().SingleAsync();
        Assert.Equal(note.Id, attachment.QuickNoteId);
        Assert.Single(note.Attachments);
    }

    [Fact]
    public async Task CreateNote_RejectsAttachmentOwnedByAnotherUser()
    {
        var (attachments, db, _, _) = CreateServices();
        await using var stream = new MemoryStream(Encoding.UTF8.GetBytes("file"));
        var uploaded = await attachments.UploadAsync(stream, "file.pdf", "application/pdf", 4, CancellationToken.None);
        await db.Set<QuickNoteAttachmentEntity>()
            .Where(a => a.Id == uploaded.Id)
            .ExecuteUpdateAsync(setters => setters.SetProperty(a => a.UserId, Guid.Parse("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb")));
        var notes = CreateNoteService(db);

        var ex = await Assert.ThrowsAsync<DomainException>(() =>
            notes.CreateAsync(new CreateQuickNoteRequest("file", "web-page", new[] { uploaded.Id }), CancellationToken.None));

        Assert.Equal(04005, ex.ErrorCode);
    }

    [Fact]
    public async Task DownloadAsync_RejectsOtherUsersAttachment()
    {
        var (attachments, db, _, _) = CreateServices();
        db.Set<QuickNoteAttachmentEntity>().Add(new QuickNoteAttachmentEntity
        {
            UserId = Guid.Parse("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb"),
            ObjectKey = "quick-notes/b/file.txt",
            FileName = "file.txt",
            ContentType = "text/plain",
            SizeBytes = 1
        });
        await db.SaveChangesAsync();
        var id = await db.Set<QuickNoteAttachmentEntity>().IgnoreQueryFilters().Select(a => a.Id).SingleAsync();

        var ex = await Assert.ThrowsAsync<DomainException>(() => attachments.DownloadAsync(id, CancellationToken.None));

        Assert.Equal(04006, ex.ErrorCode);
    }

    private static (QuickNoteAttachmentService Attachments, PimDbContext Db, FakeObjectStorage Storage, Guid UserId) CreateServices()
    {
        PimDbContext.RegisterModuleAssembly(typeof(QuickNoteEntity).Assembly);
        var options = new DbContextOptionsBuilder<PimDbContext>()
            .UseInMemoryDatabase($"quick-note-attachments-{Guid.NewGuid()}")
            .Options;
        var db = new PimDbContext(options);
        var userId = Guid.Parse("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");
        var storage = new FakeObjectStorage();
        var attachments = new QuickNoteAttachmentService(db, new FixedCurrentUserService(userId), storage);
        return (attachments, db, storage, userId);
    }

    private static QuickNoteService CreateNoteService(PimDbContext db)
    {
        var currentUser = new FixedCurrentUserService(Guid.Parse("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa"));
        var storage = new FakeObjectStorage();
        var attachmentService = new QuickNoteAttachmentService(db, currentUser, storage);
        return new QuickNoteService(db, currentUser, new AuditLogService(db), attachmentService);
    }

    private sealed class FixedCurrentUserService(Guid userId) : ICurrentUserService
    {
        public Guid? UserId { get; } = userId;
        public string? Role => "User";
    }

    private sealed class FakeObjectStorage : IQuickNoteObjectStorage
    {
        public Dictionary<string, byte[]> Objects { get; } = new();

        public async Task StoreAsync(string objectKey, Stream content, string contentType, long sizeBytes, CancellationToken ct)
        {
            using var memory = new MemoryStream();
            await content.CopyToAsync(memory, ct);
            Objects[objectKey] = memory.ToArray();
        }

        public Task<Stream> OpenReadAsync(string objectKey, CancellationToken ct)
            => Task.FromResult<Stream>(new MemoryStream(Objects[objectKey]));

        public Task DeleteAsync(string objectKey, CancellationToken ct)
        {
            Objects.Remove(objectKey);
            return Task.CompletedTask;
        }
    }
}
```

- [ ] **Step 2: 运行测试确认失败**

Run:

```powershell
dotnet test tests/Pim.UnitTests/Pim.UnitTests.csproj --filter FullyQualifiedName~QuickNoteAttachmentServiceTests
```

Expected: FAIL，错误包含 `QuickNoteAttachmentService` 或 `IQuickNoteObjectStorage` 不存在。

- [ ] **Step 3: 创建对象存储抽象**

Create `src/modules/Pim.Module.QuickNotes/Services/IQuickNoteObjectStorage.cs`:

```csharp
namespace Pim.Module.QuickNotes.Services;

public interface IQuickNoteObjectStorage
{
    Task StoreAsync(string objectKey, Stream content, string contentType, long sizeBytes, CancellationToken ct);
    Task<Stream> OpenReadAsync(string objectKey, CancellationToken ct);
    Task DeleteAsync(string objectKey, CancellationToken ct);
}
```

- [ ] **Step 4: 创建 MinIO 适配器**

Create `src/modules/Pim.Module.QuickNotes/Services/MinioQuickNoteObjectStorage.cs`:

```csharp
using Pim.Infrastructure.Storage;

namespace Pim.Module.QuickNotes.Services;

public sealed class MinioQuickNoteObjectStorage : IQuickNoteObjectStorage
{
    private readonly MinioStorage _storage;

    public MinioQuickNoteObjectStorage(MinioStorage storage)
    {
        _storage = storage;
    }

    public async Task StoreAsync(string objectKey, Stream content, string contentType, long sizeBytes, CancellationToken ct)
    {
        await _storage.EnsureBucketAsync(ct);
        await _storage.UploadAsync(objectKey, content, contentType, sizeBytes, ct);
    }

    public Task<Stream> OpenReadAsync(string objectKey, CancellationToken ct)
        => _storage.DownloadAsync(objectKey, ct);

    public Task DeleteAsync(string objectKey, CancellationToken ct)
        => _storage.DeleteAsync(objectKey, ct);
}
```

- [ ] **Step 5: 创建附件服务**

Create `src/modules/Pim.Module.QuickNotes/Services/QuickNoteAttachmentService.cs`:

```csharp
using Microsoft.EntityFrameworkCore;
using Pim.Core.Exceptions;
using Pim.Infrastructure.Auth;
using Pim.Infrastructure.Data;
using Pim.Module.QuickNotes.DTOs;
using Pim.Module.QuickNotes.Entities;

namespace Pim.Module.QuickNotes.Services;

public sealed class QuickNoteAttachmentService
{
    private readonly PimDbContext _db;
    private readonly ICurrentUserService _currentUser;
    private readonly IQuickNoteObjectStorage _storage;

    public QuickNoteAttachmentService(
        PimDbContext db,
        ICurrentUserService currentUser,
        IQuickNoteObjectStorage storage)
    {
        _db = db;
        _currentUser = currentUser;
        _storage = storage;
    }

    private Guid UserId => _currentUser.UserId ?? throw new DomainException(01002, "Not authenticated");

    public async Task<QuickNoteAttachmentUploadDto> UploadAsync(
        Stream content,
        string fileName,
        string? contentType,
        long sizeBytes,
        CancellationToken ct)
    {
        if (string.IsNullOrWhiteSpace(fileName))
            throw new DomainException(04007, "Attachment file name is required");
        if (sizeBytes < 0)
            throw new DomainException(04008, "Attachment size is invalid");

        var id = Guid.NewGuid();
        var safeName = Path.GetFileName(fileName);
        var objectKey = $"quick-notes/{UserId:N}/{id:N}/{safeName}";
        var normalizedContentType = string.IsNullOrWhiteSpace(contentType)
            ? "application/octet-stream"
            : contentType;

        await _storage.StoreAsync(objectKey, content, normalizedContentType, sizeBytes, ct);

        var entity = new QuickNoteAttachmentEntity
        {
            Id = id,
            UserId = UserId,
            StorageProvider = "minio",
            ObjectKey = objectKey,
            FileName = safeName,
            ContentType = normalizedContentType,
            SizeBytes = sizeBytes,
            CreatedAt = DateTimeOffset.UtcNow
        };
        _db.Set<QuickNoteAttachmentEntity>().Add(entity);
        await _db.SaveChangesAsync(ct);
        return MapUpload(entity);
    }

    public async Task<(Stream Content, string ContentType, string FileName)> DownloadAsync(Guid id, CancellationToken ct)
    {
        var attachment = await LoadOwnedAttachmentAsync(id, ct);
        var stream = await _storage.OpenReadAsync(attachment.ObjectKey, ct);
        return (stream, attachment.ContentType, attachment.FileName);
    }

    public async Task DeleteAsync(Guid id, CancellationToken ct)
    {
        var attachment = await LoadOwnedAttachmentAsync(id, ct);
        attachment.DeletedAt = DateTimeOffset.UtcNow;
        await _db.SaveChangesAsync(ct);
    }

    internal async Task<IReadOnlyList<QuickNoteAttachmentEntity>> LoadBindableAttachmentsAsync(
        IEnumerable<Guid> attachmentIds,
        Guid? targetNoteId,
        CancellationToken ct)
    {
        var ids = attachmentIds.Distinct().ToArray();
        if (ids.Length == 0)
            return Array.Empty<QuickNoteAttachmentEntity>();

        var attachments = await _db.Set<QuickNoteAttachmentEntity>()
            .Where(a => ids.Contains(a.Id) && a.UserId == UserId)
            .ToListAsync(ct);

        if (attachments.Count != ids.Length)
            throw new DomainException(04005, "One or more attachments are not available");

        foreach (var attachment in attachments)
        {
            if (attachment.QuickNoteId is not null && attachment.QuickNoteId != targetNoteId)
                throw new DomainException(04005, "One or more attachments are already bound to another note");
        }

        return attachments;
    }

    private async Task<QuickNoteAttachmentEntity> LoadOwnedAttachmentAsync(Guid id, CancellationToken ct)
    {
        return await _db.Set<QuickNoteAttachmentEntity>()
            .FirstOrDefaultAsync(a => a.Id == id && a.UserId == UserId, ct)
            ?? throw new DomainException(04006, "Attachment not found");
    }

    private static QuickNoteAttachmentUploadDto MapUpload(QuickNoteAttachmentEntity attachment)
    {
        var downloadUrl = $"/api/v1/quick-notes/attachments/{attachment.Id}/download";
        return new QuickNoteAttachmentUploadDto(
            attachment.Id,
            attachment.FileName,
            attachment.ContentType,
            attachment.SizeBytes,
            downloadUrl,
            attachment.ContentType.StartsWith("image/", StringComparison.OrdinalIgnoreCase) ? downloadUrl : null);
    }
}
```

- [ ] **Step 6: 修改 QuickNoteService 绑定并校验附件**

Modify `src/modules/Pim.Module.QuickNotes/Services/QuickNoteService.cs` constructor and create/update methods:

```csharp
private readonly QuickNoteAttachmentService _attachments;

public QuickNoteService(
    PimDbContext db,
    ICurrentUserService currentUser,
    IAuditLogService auditLog,
    QuickNoteAttachmentService attachments)
{
    _db = db;
    _currentUser = currentUser;
    _auditLog = auditLog;
    _attachments = attachments;
}
```

In `CreateAsync`, before adding the note:

```csharp
var attachmentIds = MergeAttachmentIds(request.AttachmentIds, request.ContentMarkdown);
var attachments = await _attachments.LoadBindableAttachmentsAsync(attachmentIds, null, ct);
```

After `await _db.SaveChangesAsync(ct);` for the new note:

```csharp
foreach (var attachment in attachments)
    attachment.QuickNoteId = note.Id;
await _db.SaveChangesAsync(ct);
```

In `UpdateAsync`, before assigning markdown:

```csharp
var attachmentIds = MergeAttachmentIds(request.AttachmentIds, request.ContentMarkdown);
var bindable = await _attachments.LoadBindableAttachmentsAsync(attachmentIds, note.Id, ct);
foreach (var attachment in note.Attachments)
{
    if (!attachmentIds.Contains(attachment.Id))
        attachment.DeletedAt = DateTimeOffset.UtcNow;
}
foreach (var attachment in bindable)
{
    attachment.QuickNoteId = note.Id;
    attachment.DeletedAt = null;
}
```

Add helper method:

```csharp
private static IReadOnlyList<Guid> MergeAttachmentIds(IReadOnlyList<Guid>? explicitIds, string? markdown)
{
    var ids = new List<Guid>();
    var seen = new HashSet<Guid>();
    foreach (var id in explicitIds ?? Array.Empty<Guid>())
    {
        if (seen.Add(id))
            ids.Add(id);
    }
    foreach (var id in QuickNoteMarkdownReferences.ExtractAttachmentIds(markdown))
    {
        if (seen.Add(id))
            ids.Add(id);
    }
    return ids;
}
```

In `tests/Pim.UnitTests/QuickNotes/QuickNoteServiceTests.cs`, replace the service construction in `CreateService()` with:

```csharp
var currentUser = new FixedCurrentUserService(userId);
var storage = new FakeObjectStorage();
var attachmentService = new QuickNoteAttachmentService(db, currentUser, storage);
var service = new QuickNoteService(
    db,
    currentUser,
    new AuditLogService(db),
    attachmentService);
```

Add this fake storage class to the same test file:

```csharp
private sealed class FakeObjectStorage : IQuickNoteObjectStorage
{
    public Task StoreAsync(string objectKey, Stream content, string contentType, long sizeBytes, CancellationToken ct)
        => Task.CompletedTask;

    public Task<Stream> OpenReadAsync(string objectKey, CancellationToken ct)
        => Task.FromResult<Stream>(new MemoryStream());

    public Task DeleteAsync(string objectKey, CancellationToken ct)
        => Task.CompletedTask;
}
```

- [ ] **Step 7: 注册附件服务**

Modify `src/modules/Pim.Module.QuickNotes/QuickNotesModule.cs` inside `RegisterServices`:

```csharp
services.AddScoped<IQuickNoteObjectStorage, MinioQuickNoteObjectStorage>();
services.AddScoped<QuickNoteAttachmentService>();
services.AddScoped<QuickNoteService>();
```

- [ ] **Step 8: 运行附件测试和服务测试**

Run:

```powershell
dotnet test tests/Pim.UnitTests/Pim.UnitTests.csproj --filter "FullyQualifiedName~QuickNoteAttachmentServiceTests|FullyQualifiedName~QuickNoteServiceTests"
```

Expected: PASS。

- [ ] **Step 9: Commit**

```powershell
git add src/modules/Pim.Module.QuickNotes/Services src/modules/Pim.Module.QuickNotes/QuickNotesModule.cs tests/Pim.UnitTests/QuickNotes
git commit -m "feat(quick-notes): add attachment service"
```

---

### Task 5: 暴露 Quick Notes API 端点

**Files:**
- Modify: `src/modules/Pim.Module.QuickNotes/QuickNotesModule.cs`
- Test: `tests/Pim.UnitTests/QuickNotes/QuickNoteEndpointPathTests.cs`

- [ ] **Step 1: 编写失败的 path 测试**

Create `tests/Pim.UnitTests/QuickNotes/QuickNoteEndpointPathTests.cs`:

```csharp
using Pim.Module.QuickNotes;
using Xunit;

namespace Pim.UnitTests.QuickNotes;

public class QuickNoteEndpointPathTests
{
    [Fact]
    public void QuickNoteEndpointPaths_AreStable()
    {
        Assert.Equal("/api/v1/quick-notes", QuickNoteEndpointPaths.Root);
        Assert.Equal("/api/v1/quick-notes/11111111-1111-1111-1111-111111111111", QuickNoteEndpointPaths.Note("11111111-1111-1111-1111-111111111111"));
        Assert.Equal("/api/v1/quick-notes/attachments", QuickNoteEndpointPaths.Attachments);
        Assert.Equal("/api/v1/quick-notes/attachments/22222222-2222-2222-2222-222222222222/download", QuickNoteEndpointPaths.AttachmentDownload("22222222-2222-2222-2222-222222222222"));
    }
}
```

- [ ] **Step 2: 运行测试确认失败**

Run:

```powershell
dotnet test tests/Pim.UnitTests/Pim.UnitTests.csproj --filter FullyQualifiedName~QuickNoteEndpointPathTests
```

Expected: FAIL，错误包含 `QuickNoteEndpointPaths` 不存在。

- [ ] **Step 3: 添加 path 常量和 endpoints**

Modify `src/modules/Pim.Module.QuickNotes/QuickNotesModule.cs`:

```csharp
using System.Reflection;
using Microsoft.AspNetCore.Http;
using Microsoft.AspNetCore.Mvc;
using Microsoft.AspNetCore.Routing;
using Microsoft.Extensions.Configuration;
using Microsoft.Extensions.DependencyInjection;
using Pim.Core.Common;
using Pim.Core.Modules;
using Pim.Infrastructure.Data;
using Pim.Module.QuickNotes.DTOs;
using Pim.Module.QuickNotes.Services;

namespace Pim.Module.QuickNotes;

public static class QuickNoteEndpointPaths
{
    public const string Root = "/api/v1/quick-notes";
    public const string Attachments = "/api/v1/quick-notes/attachments";

    public static string Note(string id) => $"{Root}/{id}";
    public static string AttachmentDownload(string id) => $"{Attachments}/{id}/download";
}

public sealed class QuickNotesModule : IModule
{
    public string Name => "quick-notes";
    public string Version => "1.0.0";

    public void RegisterServices(IServiceCollection services, IConfiguration configuration)
    {
        PimDbContext.RegisterModuleAssembly(Assembly.GetExecutingAssembly());
        services.AddScoped<IQuickNoteObjectStorage, MinioQuickNoteObjectStorage>();
        services.AddScoped<QuickNoteAttachmentService>();
        services.AddScoped<QuickNoteService>();
    }

    public void MapEndpoints(IEndpointRouteBuilder endpoints)
    {
        var group = endpoints.MapGroup("/api/v1/quick-notes")
            .RequireAuthorization();

        group.MapGet("", async (
            [FromQuery] string? status,
            [FromQuery] string? search,
            [FromQuery] int? page,
            [FromQuery] int? pageSize,
            [FromServices] QuickNoteService service,
            CancellationToken ct) =>
        {
            var result = await service.ListAsync(
                new QuickNoteListQuery(status, search, page ?? 1, pageSize ?? 30),
                ct);
            return Results.Ok(ApiResponse<PagedResult<QuickNoteListItemDto>>.Ok(result));
        });

        group.MapGet("/{id:guid}", async (
            Guid id,
            [FromServices] QuickNoteService service,
            CancellationToken ct) =>
            Results.Ok(ApiResponse<QuickNoteDetailDto>.Ok(await service.GetAsync(id, ct))));

        group.MapPost("", async (
            [FromBody] CreateQuickNoteRequest request,
            [FromServices] QuickNoteService service,
            CancellationToken ct) =>
        {
            var result = await service.CreateAsync(request, ct);
            return Results.Created($"/api/v1/quick-notes/{result.Id}", ApiResponse<QuickNoteDetailDto>.Ok(result));
        });

        group.MapPut("/{id:guid}", async (
            Guid id,
            [FromBody] UpdateQuickNoteRequest request,
            [FromServices] QuickNoteService service,
            CancellationToken ct) =>
            Results.Ok(ApiResponse<QuickNoteDetailDto>.Ok(await service.UpdateAsync(id, request, ct))));

        group.MapPost("/{id:guid}/process", async (
            Guid id,
            [FromServices] QuickNoteService service,
            CancellationToken ct) =>
            Results.Ok(ApiResponse<QuickNoteDetailDto>.Ok(await service.ProcessAsync(id, ct))));

        group.MapPost("/{id:guid}/archive", async (
            Guid id,
            [FromServices] QuickNoteService service,
            CancellationToken ct) =>
            Results.Ok(ApiResponse<QuickNoteDetailDto>.Ok(await service.ArchiveAsync(id, ct))));

        group.MapPost("/{id:guid}/restore", async (
            Guid id,
            [FromBody] RestoreQuickNoteRequest request,
            [FromServices] QuickNoteService service,
            CancellationToken ct) =>
            Results.Ok(ApiResponse<QuickNoteDetailDto>.Ok(await service.RestoreAsync(id, request.Status, ct))));

        group.MapDelete("/{id:guid}", async (
            Guid id,
            [FromServices] QuickNoteService service,
            CancellationToken ct) =>
        {
            await service.DeleteAsync(id, ct);
            return Results.Ok(ApiResponse<string>.Ok("deleted"));
        });

        group.MapPost("/attachments", async (
            HttpRequest request,
            [FromServices] QuickNoteAttachmentService service,
            CancellationToken ct) =>
        {
            if (!request.HasFormContentType)
                return Results.BadRequest(ApiResponse<string>.Error(400, "Expected multipart/form-data"));

            var form = await request.ReadFormAsync(ct);
            var file = form.Files.GetFile("file");
            if (file is null)
                return Results.BadRequest(ApiResponse<string>.Error(400, "No file field"));

            await using var stream = file.OpenReadStream();
            var result = await service.UploadAsync(stream, file.FileName, file.ContentType, file.Length, ct);
            return Results.Ok(ApiResponse<QuickNoteAttachmentUploadDto>.Ok(result));
        });

        group.MapGet("/attachments/{id:guid}/download", async (
            Guid id,
            [FromServices] QuickNoteAttachmentService service,
            CancellationToken ct) =>
        {
            var download = await service.DownloadAsync(id, ct);
            return Results.File(download.Content, download.ContentType, download.FileName);
        });

        group.MapDelete("/attachments/{id:guid}", async (
            Guid id,
            [FromServices] QuickNoteAttachmentService service,
            CancellationToken ct) =>
        {
            await service.DeleteAsync(id, ct);
            return Results.Ok(ApiResponse<string>.Ok("deleted"));
        });
    }

    public Task InitializeAsync(IServiceProvider serviceProvider)
        => Task.CompletedTask;
}
```

- [ ] **Step 4: 运行 path 测试**

Run:

```powershell
dotnet test tests/Pim.UnitTests/Pim.UnitTests.csproj --filter FullyQualifiedName~QuickNoteEndpointPathTests
```

Expected: PASS。

- [ ] **Step 5: 运行 QuickNotes 后端测试**

Run:

```powershell
dotnet test tests/Pim.UnitTests/Pim.UnitTests.csproj --filter FullyQualifiedName~QuickNote
```

Expected: PASS。

- [ ] **Step 6: Commit**

```powershell
git add src/modules/Pim.Module.QuickNotes/QuickNotesModule.cs tests/Pim.UnitTests/QuickNotes/QuickNoteEndpointPathTests.cs
git commit -m "feat(quick-notes): expose note endpoints"
```

---

### Task 6: 添加 EF migration

**Files:**
- Create: `src/Pim.Infrastructure/Data/Migrations/<timestamp>_AddQuickNotes.cs`
- Create: `src/Pim.Infrastructure/Data/Migrations/<timestamp>_AddQuickNotes.Designer.cs`
- Modify: `src/Pim.Infrastructure/Data/Migrations/PimDbContextModelSnapshot.cs`

- [ ] **Step 1: 生成 migration**

Run:

```powershell
dotnet ef migrations add AddQuickNotes --project src/Pim.Infrastructure --startup-project src/Pim.Api --context PimDbContext
```

Expected: 新增 `AddQuickNotes` migration 和更新 `PimDbContextModelSnapshot.cs`。

- [ ] **Step 2: 检查 migration 包含目标表和索引**

Run:

```powershell
rg -n "quick_notes|quick_note_attachments|IX_quick_notes|IX_quick_note_attachments" src/Pim.Infrastructure/Data/Migrations
```

Expected: 输出包含 `CreateTable("quick_notes")`、`CreateTable("quick_note_attachments")` 和对应索引。

- [ ] **Step 3: 运行后端测试**

Run:

```powershell
dotnet test tests/Pim.UnitTests/Pim.UnitTests.csproj --filter FullyQualifiedName~QuickNote
```

Expected: PASS。

- [ ] **Step 4: Commit**

```powershell
git add src/Pim.Infrastructure/Data/Migrations
git commit -m "feat(quick-notes): add database migration"
```

---

### Task 7: 添加前端 API 类型与客户端

**Files:**
- Create: `src/client-web/src/api/quickNotes.ts`
- Modify: `src/client-web/src/types/index.ts`
- Create: `tests/client-web/quickNotesApiPath.test.ts`
- Create: `tests/client-web/quickNotesTypes.test.ts`
- Create: `tests/client-web/tsconfig.quick-notes.json`

- [ ] **Step 1: 编写失败的 API path 测试**

Create `tests/client-web/quickNotesApiPath.test.ts`:

```ts
import assert from 'node:assert/strict';
import { quickNoteApiPaths } from '../../src/client-web/src/api/quickNotes';

assert.equal(quickNoteApiPaths.list({ status: 'inbox', search: 'alpha', page: 2, pageSize: 30 }), '/quick-notes?status=inbox&search=alpha&page=2&pageSize=30');
assert.equal(quickNoteApiPaths.detail('11111111-1111-1111-1111-111111111111'), '/quick-notes/11111111-1111-1111-1111-111111111111');
assert.equal(quickNoteApiPaths.process('11111111-1111-1111-1111-111111111111'), '/quick-notes/11111111-1111-1111-1111-111111111111/process');
assert.equal(quickNoteApiPaths.archive('11111111-1111-1111-1111-111111111111'), '/quick-notes/11111111-1111-1111-1111-111111111111/archive');
assert.equal(quickNoteApiPaths.restore('11111111-1111-1111-1111-111111111111'), '/quick-notes/11111111-1111-1111-1111-111111111111/restore');
assert.equal(quickNoteApiPaths.attachments(), '/quick-notes/attachments');
assert.equal(quickNoteApiPaths.attachmentDownload('22222222-2222-2222-2222-222222222222'), '/quick-notes/attachments/22222222-2222-2222-2222-222222222222/download');
```

- [ ] **Step 2: 编写失败的类型测试**

Create `tests/client-web/quickNotesTypes.test.ts`:

```ts
import assert from 'node:assert/strict';
import type {
  QuickNoteAttachment,
  QuickNoteDetail,
  QuickNoteListItem,
  QuickNoteStatus,
} from '../../src/client-web/src/types';

const status: QuickNoteStatus = 'inbox';

const attachment: QuickNoteAttachment = {
  id: '22222222-2222-2222-2222-222222222222',
  fileName: 'shot.png',
  contentType: 'image/png',
  sizeBytes: 12,
  downloadUrl: '/api/v1/quick-notes/attachments/22222222-2222-2222-2222-222222222222/download',
  previewUrl: '/api/v1/quick-notes/attachments/22222222-2222-2222-2222-222222222222/download',
  createdAt: '2026-05-26T00:00:00Z',
};

const item: QuickNoteListItem = {
  id: '11111111-1111-1111-1111-111111111111',
  contentPreview: 'hello',
  status,
  source: 'web-page',
  attachmentCount: 1,
  createdAt: '2026-05-26T00:00:00Z',
  updatedAt: '2026-05-26T00:00:00Z',
  archivedAt: null,
};

const detail: QuickNoteDetail = {
  ...item,
  contentMarkdown: '![shot](url)',
  attachments: [attachment],
  metadataJson: '{}',
};

assert.equal(detail.status, 'inbox');
assert.equal(detail.attachments[0].fileName, 'shot.png');
```

Create `tests/client-web/tsconfig.quick-notes.json`:

```json
{
  "extends": "../../src/client-web/tsconfig.json",
  "compilerOptions": {
    "noEmit": true,
    "types": ["node"],
    "skipLibCheck": true
  },
  "include": [
    "./quickNotesTypes.test.ts",
    "../../src/client-web/src/types/index.ts"
  ]
}
```

- [ ] **Step 3: 运行测试确认失败**

Run:

```powershell
npm --prefix src/client-web exec tsx -- ..\..\tests\client-web\quickNotesApiPath.test.ts
npm --prefix src/client-web exec tsc -- -p ..\..\tests\client-web\tsconfig.quick-notes.json
```

Expected: FAIL，错误包含 `quickNotes` API 或 Quick Notes 类型不存在。

- [ ] **Step 4: 添加类型定义**

Modify `src/client-web/src/types/index.ts`, append:

```ts
export type QuickNoteStatus = 'inbox' | 'processed' | 'archived';

export interface QuickNoteAttachment {
  id: string;
  fileName: string;
  contentType: string;
  sizeBytes: number;
  downloadUrl: string;
  previewUrl: string | null;
  createdAt: string;
}

export interface QuickNoteListItem {
  id: string;
  contentPreview: string;
  status: QuickNoteStatus;
  source: string;
  attachmentCount: number;
  createdAt: string;
  updatedAt: string;
  archivedAt: string | null;
}

export interface QuickNoteDetail extends QuickNoteListItem {
  contentMarkdown: string;
  attachments: QuickNoteAttachment[];
  metadataJson: string;
}

export interface CreateQuickNoteRequest {
  contentMarkdown: string;
  source?: 'web-floating' | 'web-page' | string;
  attachmentIds?: string[];
}

export interface UpdateQuickNoteRequest {
  contentMarkdown: string;
  status?: QuickNoteStatus;
  attachmentIds?: string[];
}

export interface QuickNoteAttachmentUpload {
  id: string;
  fileName: string;
  contentType: string;
  sizeBytes: number;
  downloadUrl: string;
  previewUrl: string | null;
}
```

- [ ] **Step 5: 添加 API client**

Create `src/client-web/src/api/quickNotes.ts`:

```ts
import { apiDelete, apiGet, apiPost, apiPut } from './client';
import type {
  ApiResponse,
  CreateQuickNoteRequest,
  PagedResult,
  QuickNoteAttachmentUpload,
  QuickNoteDetail,
  QuickNoteListItem,
  QuickNoteStatus,
  UpdateQuickNoteRequest,
} from '../types';

interface QuickNoteListParams {
  status?: QuickNoteStatus;
  search?: string;
  page?: number;
  pageSize?: number;
}

export const quickNoteApiPaths = {
  list(params: QuickNoteListParams = {}) {
    const qs = new URLSearchParams();
    if (params.status) qs.set('status', params.status);
    if (params.search) qs.set('search', params.search);
    if (params.page) qs.set('page', String(params.page));
    if (params.pageSize) qs.set('pageSize', String(params.pageSize));
    const suffix = qs.toString();
    return suffix ? `/quick-notes?${suffix}` : '/quick-notes';
  },
  detail(id: string) {
    return `/quick-notes/${id}`;
  },
  process(id: string) {
    return `/quick-notes/${id}/process`;
  },
  archive(id: string) {
    return `/quick-notes/${id}/archive`;
  },
  restore(id: string) {
    return `/quick-notes/${id}/restore`;
  },
  attachments() {
    return '/quick-notes/attachments';
  },
  attachmentDownload(id: string) {
    return `/quick-notes/attachments/${id}/download`;
  },
};

export async function getQuickNotes(params: QuickNoteListParams = {}) {
  const response = await apiGet<ApiResponse<PagedResult<QuickNoteListItem>>>(quickNoteApiPaths.list(params));
  return response.data;
}

export async function getQuickNote(id: string) {
  const response = await apiGet<ApiResponse<QuickNoteDetail>>(quickNoteApiPaths.detail(id));
  return response.data;
}

export async function createQuickNote(data: CreateQuickNoteRequest) {
  const response = await apiPost<ApiResponse<QuickNoteDetail>>('/quick-notes', data);
  return response.data;
}

export async function updateQuickNote(id: string, data: UpdateQuickNoteRequest) {
  const response = await apiPut<ApiResponse<QuickNoteDetail>>(quickNoteApiPaths.detail(id), data);
  return response.data;
}

export async function processQuickNote(id: string) {
  const response = await apiPost<ApiResponse<QuickNoteDetail>>(quickNoteApiPaths.process(id));
  return response.data;
}

export async function archiveQuickNote(id: string) {
  const response = await apiPost<ApiResponse<QuickNoteDetail>>(quickNoteApiPaths.archive(id));
  return response.data;
}

export async function restoreQuickNote(id: string, status: QuickNoteStatus = 'inbox') {
  const response = await apiPost<ApiResponse<QuickNoteDetail>>(quickNoteApiPaths.restore(id), { status });
  return response.data;
}

export async function deleteQuickNote(id: string) {
  await apiDelete<ApiResponse<string>>(quickNoteApiPaths.detail(id));
}

export async function uploadQuickNoteAttachment(file: File) {
  const form = new FormData();
  form.append('file', file);
  const response = await fetch('/api/v1/quick-notes/attachments', {
    method: 'POST',
    headers: {
      Authorization: `Bearer ${localStorage.getItem('accessToken')}`,
    },
    body: form,
  });
  if (!response.ok) {
    const error = await response.json().catch(() => ({}));
    throw new Error(error.message || `Upload failed: ${response.status}`);
  }
  const json = await response.json() as ApiResponse<QuickNoteAttachmentUpload>;
  return json.data;
}
```

- [ ] **Step 6: 运行前端 contract 测试**

Run:

```powershell
npm --prefix src/client-web exec tsx -- ..\..\tests\client-web\quickNotesApiPath.test.ts
npm --prefix src/client-web exec tsc -- -p ..\..\tests\client-web\tsconfig.quick-notes.json
```

Expected: PASS。

- [ ] **Step 7: Commit**

```powershell
git add src/client-web/src/api/quickNotes.ts src/client-web/src/types/index.ts tests/client-web/quickNotesApiPath.test.ts tests/client-web/quickNotesTypes.test.ts tests/client-web/tsconfig.quick-notes.json
git commit -m "feat(web): add quick notes api client"
```

---

### Task 8: 安装并封装 MDXEditor

**Files:**
- Modify: `src/client-web/package.json`
- Modify: `src/client-web/package-lock.json`
- Create: `src/client-web/src/components/quick-notes/QuickNoteEditor.tsx`
- Create: `src/client-web/src/components/quick-notes/QuickNoteMarkdownPreview.tsx`

- [ ] **Step 1: 安装编辑器依赖**

Run:

```powershell
npm --prefix src/client-web install @mdxeditor/editor
```

Expected: `package.json` 和 `package-lock.json` 增加 `@mdxeditor/editor`。

- [ ] **Step 2: 创建编辑器封装**

Create `src/client-web/src/components/quick-notes/QuickNoteEditor.tsx`:

```tsx
import {
  MDXEditor,
  headingsPlugin,
  imagePlugin,
  linkPlugin,
  listsPlugin,
  markdownShortcutPlugin,
  quotePlugin,
  thematicBreakPlugin,
  toolbarPlugin,
  UndoRedo,
  BoldItalicUnderlineToggles,
  ListsToggle,
  CreateLink,
  InsertImage,
} from '@mdxeditor/editor';
import '@mdxeditor/editor/style.css';
import { uploadQuickNoteAttachment } from '../../api/quickNotes';

interface QuickNoteEditorProps {
  value: string;
  onChange: (value: string) => void;
  minHeight?: number;
  readOnly?: boolean;
}

export default function QuickNoteEditor({
  value,
  onChange,
  minHeight = 220,
  readOnly = false,
}: QuickNoteEditorProps) {
  return (
    <div className="overflow-hidden rounded-lg border border-slate-200 bg-white">
      <MDXEditor
        markdown={value}
        onChange={onChange}
        readOnly={readOnly}
        contentEditableClassName="prose prose-slate max-w-none px-4 py-3 text-sm focus:outline-none"
        className="quick-note-mdx"
        plugins={[
          headingsPlugin(),
          listsPlugin(),
          quotePlugin(),
          thematicBreakPlugin(),
          linkPlugin(),
          imagePlugin({
            imageUploadHandler: async file => {
              const uploaded = await uploadQuickNoteAttachment(file);
              return uploaded.downloadUrl;
            },
          }),
          markdownShortcutPlugin(),
          toolbarPlugin({
            toolbarContents: () => readOnly ? null : (
              <>
                <UndoRedo />
                <BoldItalicUnderlineToggles />
                <ListsToggle />
                <CreateLink />
                <InsertImage />
              </>
            ),
          }),
        ]}
      />
      <style>{`.quick-note-mdx [contenteditable="true"] { min-height: ${minHeight}px; }`}</style>
    </div>
  );
}
```

- [ ] **Step 3: 创建只读预览组件**

Create `src/client-web/src/components/quick-notes/QuickNoteMarkdownPreview.tsx`:

```tsx
import QuickNoteEditor from './QuickNoteEditor';
import type { QuickNoteAttachment } from '../../types';

export default function QuickNoteMarkdownPreview({
  markdown,
  attachments,
}: {
  markdown: string;
  attachments: QuickNoteAttachment[];
}) {
  return (
    <div className="space-y-3">
      <QuickNoteEditor value={markdown || ' '} onChange={() => undefined} readOnly minHeight={80} />
      {attachments.length > 0 && (
        <div className="rounded-lg border border-slate-200 bg-slate-50 p-3">
          <p className="text-xs font-medium text-slate-500">引用附件</p>
          <div className="mt-2 flex flex-wrap gap-2">
            {attachments.map(attachment => (
              <a
                key={attachment.id}
                href={attachment.downloadUrl}
                className="rounded-lg border border-slate-200 bg-white px-3 py-2 text-xs text-slate-700 hover:border-blue-300 hover:text-blue-700"
              >
                {attachment.fileName}
              </a>
            ))}
          </div>
        </div>
      )}
    </div>
  );
}
```

- [ ] **Step 4: 运行前端构建**

Run:

```powershell
npm --prefix src/client-web run build
```

Expected: PASS。

- [ ] **Step 5: Commit**

```powershell
git add src/client-web/package.json src/client-web/package-lock.json src/client-web/src/components/quick-notes
git commit -m "feat(web): add quick note markdown editor"
```

---

### Task 9: 实现常住可拖拽快速记录悬浮面板

**Files:**
- Create: `src/client-web/src/components/quick-notes/quickNoteFloatingState.ts`
- Create: `src/client-web/src/components/quick-notes/QuickNoteFloatingButton.tsx`
- Create: `src/client-web/src/components/quick-notes/QuickNoteFloatingPanel.tsx`
- Modify: `src/client-web/src/layout/AppLayout.tsx`
- Test: `tests/client-web/quickNoteFloatingState.test.ts`

- [ ] **Step 1: 编写失败的浮窗状态测试**

Create `tests/client-web/quickNoteFloatingState.test.ts`:

```ts
import assert from 'node:assert/strict';
import {
  QUICK_NOTE_DRAFT_KEY,
  QUICK_NOTE_PANEL_POSITION_KEY,
  clampPanelPosition,
} from '../../src/client-web/src/components/quick-notes/quickNoteFloatingState';

assert.equal(QUICK_NOTE_DRAFT_KEY, 'pim.quickNotes.floatingDraft');
assert.equal(QUICK_NOTE_PANEL_POSITION_KEY, 'pim.quickNotes.panelPosition');
assert.deepEqual(
  clampPanelPosition({ x: -50, y: 9999 }, { width: 1200, height: 800 }, { width: 360, height: 420 }),
  { x: 12, y: 368 },
);
assert.deepEqual(
  clampPanelPosition({ x: 500, y: 200 }, { width: 1200, height: 800 }, { width: 360, height: 420 }),
  { x: 500, y: 200 },
);
```

- [ ] **Step 2: 运行测试确认失败**

Run:

```powershell
npm --prefix src/client-web exec tsx -- ..\..\tests\client-web\quickNoteFloatingState.test.ts
```

Expected: FAIL，错误包含 `quickNoteFloatingState` 不存在。

- [ ] **Step 3: 添加浮窗状态工具**

Create `src/client-web/src/components/quick-notes/quickNoteFloatingState.ts`:

```ts
export const QUICK_NOTE_DRAFT_KEY = 'pim.quickNotes.floatingDraft';
export const QUICK_NOTE_PANEL_POSITION_KEY = 'pim.quickNotes.panelPosition';

export interface PanelPoint {
  x: number;
  y: number;
}

export interface PanelSize {
  width: number;
  height: number;
}

const PANEL_MARGIN = 12;

export function clampPanelPosition(point: PanelPoint, viewport: PanelSize, panel: PanelSize): PanelPoint {
  const maxX = Math.max(PANEL_MARGIN, viewport.width - panel.width - PANEL_MARGIN);
  const maxY = Math.max(PANEL_MARGIN, viewport.height - panel.height - PANEL_MARGIN);
  return {
    x: Math.min(Math.max(PANEL_MARGIN, point.x), maxX),
    y: Math.min(Math.max(PANEL_MARGIN, point.y), maxY),
  };
}

export function loadPanelPosition(fallback: PanelPoint): PanelPoint {
  const raw = localStorage.getItem(QUICK_NOTE_PANEL_POSITION_KEY);
  if (!raw) return fallback;
  try {
    const parsed = JSON.parse(raw) as Partial<PanelPoint>;
    if (typeof parsed.x === 'number' && typeof parsed.y === 'number') {
      return parsed as PanelPoint;
    }
  } catch {
    return fallback;
  }
  return fallback;
}

export function savePanelPosition(point: PanelPoint) {
  localStorage.setItem(QUICK_NOTE_PANEL_POSITION_KEY, JSON.stringify(point));
}
```

- [ ] **Step 4: 添加悬浮按钮**

Create `src/client-web/src/components/quick-notes/QuickNoteFloatingButton.tsx`:

```tsx
export default function QuickNoteFloatingButton({ onClick }: { onClick: () => void }) {
  return (
    <button
      type="button"
      onClick={onClick}
      className="fixed bottom-5 right-5 z-40 flex h-12 w-12 items-center justify-center rounded-full bg-blue-600 text-2xl leading-none text-white shadow-lg shadow-blue-600/25 transition hover:bg-blue-700"
      aria-label="打开快速记录"
      title="快速记录"
    >
      +
    </button>
  );
}
```

- [ ] **Step 5: 添加悬浮面板**

Create `src/client-web/src/components/quick-notes/QuickNoteFloatingPanel.tsx`:

```tsx
import { useEffect, useRef, useState } from 'react';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import QuickNoteEditor from './QuickNoteEditor';
import {
  QUICK_NOTE_DRAFT_KEY,
  clampPanelPosition,
  loadPanelPosition,
  savePanelPosition,
  type PanelPoint,
} from './quickNoteFloatingState';
import { createQuickNote } from '../../api/quickNotes';

const PANEL_SIZE = { width: 380, height: 460 };

export default function QuickNoteFloatingPanel({ onClose }: { onClose: () => void }) {
  const queryClient = useQueryClient();
  const panelRef = useRef<HTMLDivElement>(null);
  const dragOffset = useRef<PanelPoint | null>(null);
  const [position, setPosition] = useState<PanelPoint>(() =>
    loadPanelPosition({
      x: Math.max(12, window.innerWidth - PANEL_SIZE.width - 20),
      y: Math.max(12, window.innerHeight - PANEL_SIZE.height - 84),
    }),
  );
  const [markdown, setMarkdown] = useState(() => localStorage.getItem(QUICK_NOTE_DRAFT_KEY) ?? '');
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    localStorage.setItem(QUICK_NOTE_DRAFT_KEY, markdown);
  }, [markdown]);

  const createMutation = useMutation({
    mutationFn: () => createQuickNote({ contentMarkdown: markdown, source: 'web-floating' }),
    onSuccess: () => {
      setMarkdown('');
      localStorage.removeItem(QUICK_NOTE_DRAFT_KEY);
      setError(null);
      queryClient.invalidateQueries({ queryKey: ['quick-notes'] });
    },
    onError: err => setError((err as Error).message || '保存失败'),
  });

  function startDrag(event: React.PointerEvent<HTMLDivElement>) {
    const rect = panelRef.current?.getBoundingClientRect();
    if (!rect) return;
    dragOffset.current = { x: event.clientX - rect.left, y: event.clientY - rect.top };
    event.currentTarget.setPointerCapture(event.pointerId);
  }

  function moveDrag(event: React.PointerEvent<HTMLDivElement>) {
    if (!dragOffset.current) return;
    const next = clampPanelPosition(
      { x: event.clientX - dragOffset.current.x, y: event.clientY - dragOffset.current.y },
      { width: window.innerWidth, height: window.innerHeight },
      PANEL_SIZE,
    );
    setPosition(next);
  }

  function endDrag() {
    dragOffset.current = null;
    savePanelPosition(position);
  }

  return (
    <div
      ref={panelRef}
      className="fixed z-50 flex w-[380px] flex-col rounded-xl border border-slate-200 bg-white shadow-2xl shadow-slate-900/20"
      style={{ left: position.x, top: position.y, height: PANEL_SIZE.height }}
      role="dialog"
      aria-label="快速记录"
    >
      <div
        className="flex cursor-move items-center justify-between border-b border-slate-200 px-3 py-2"
        onPointerDown={startDrag}
        onPointerMove={moveDrag}
        onPointerUp={endDrag}
      >
        <div>
          <p className="text-sm font-semibold text-slate-900">快速记录</p>
          <p className="text-xs text-slate-500">文字、图片和文件一起保存</p>
        </div>
        <button
          type="button"
          onClick={onClose}
          className="rounded-lg px-2 py-1 text-sm text-slate-500 hover:bg-slate-100 hover:text-slate-900"
          aria-label="关闭快速记录"
        >
          ×
        </button>
      </div>
      <div className="min-h-0 flex-1 overflow-auto p-3">
        <QuickNoteEditor value={markdown} onChange={setMarkdown} minHeight={250} />
        {error && <p className="mt-2 rounded-lg bg-red-50 px-3 py-2 text-xs text-red-700">{error}</p>}
      </div>
      <div className="flex items-center justify-end gap-2 border-t border-slate-200 p-3">
        <button
          type="button"
          onClick={() => createMutation.mutate()}
          disabled={createMutation.isPending || markdown.trim().length === 0}
          className="pim-button-primary px-4 py-2 text-sm disabled:opacity-50"
        >
          {createMutation.isPending ? '保存中...' : '保存'}
        </button>
      </div>
    </div>
  );
}
```

- [ ] **Step 6: 挂载到 AppLayout**

Modify `src/client-web/src/layout/AppLayout.tsx`:

```tsx
import { useState } from 'react';
import QuickNoteFloatingButton from '../components/quick-notes/QuickNoteFloatingButton';
import QuickNoteFloatingPanel from '../components/quick-notes/QuickNoteFloatingPanel';
```

Inside `AppLayout` after `showCalendarInbox`:

```tsx
const [quickNoteOpen, setQuickNoteOpen] = useState(false);
```

Inside returned shell, after `{showCalendarInbox && <InboxPanel draggable />}`:

```tsx
<QuickNoteFloatingButton onClick={() => setQuickNoteOpen(true)} />
{quickNoteOpen && <QuickNoteFloatingPanel onClose={() => setQuickNoteOpen(false)} />}
```

- [ ] **Step 7: 运行浮窗测试和前端构建**

Run:

```powershell
npm --prefix src/client-web exec tsx -- ..\..\tests\client-web\quickNoteFloatingState.test.ts
npm --prefix src/client-web run build
```

Expected: PASS。

- [ ] **Step 8: Commit**

```powershell
git add src/client-web/src/components/quick-notes src/client-web/src/layout/AppLayout.tsx tests/client-web/quickNoteFloatingState.test.ts
git commit -m "feat(web): add quick note floating panel"
```

---

### Task 10: 实现 `/quick-notes` 独立管理页面和导航

**Files:**
- Create: `src/client-web/src/pages/QuickNotesPage.tsx`
- Modify: `src/client-web/src/layout/AppLayout.tsx`
- Modify: `src/client-web/src/layout/Sidebar.tsx`

- [ ] **Step 1: 创建 QuickNotesPage**

Create `src/client-web/src/pages/QuickNotesPage.tsx`:

```tsx
import { useMemo, useState } from 'react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import {
  archiveQuickNote,
  createQuickNote,
  deleteQuickNote,
  getQuickNote,
  getQuickNotes,
  processQuickNote,
  restoreQuickNote,
  updateQuickNote,
} from '../api/quickNotes';
import QuickNoteEditor from '../components/quick-notes/QuickNoteEditor';
import QuickNoteMarkdownPreview from '../components/quick-notes/QuickNoteMarkdownPreview';
import PageHeader from '../ui/PageHeader';
import EmptyState from '../ui/EmptyState';
import type { QuickNoteStatus } from '../types';

const statusTabs: Array<{ key: QuickNoteStatus; label: string }> = [
  { key: 'inbox', label: '收件箱' },
  { key: 'processed', label: '已处理' },
  { key: 'archived', label: '已归档' },
];

export default function QuickNotesPage() {
  const queryClient = useQueryClient();
  const [status, setStatus] = useState<QuickNoteStatus>('inbox');
  const [search, setSearch] = useState('');
  const [selectedId, setSelectedId] = useState<string | null>(null);
  const [draft, setDraft] = useState('');
  const [editing, setEditing] = useState(false);

  const listQuery = useQuery({
    queryKey: ['quick-notes', status, search],
    queryFn: () => getQuickNotes({ status, search, page: 1, pageSize: 50 }),
  });

  const detailQuery = useQuery({
    queryKey: ['quick-note', selectedId],
    queryFn: () => getQuickNote(selectedId!),
    enabled: !!selectedId,
  });

  const selected = detailQuery.data;

  const createMutation = useMutation({
    mutationFn: () => createQuickNote({ contentMarkdown: draft, source: 'web-page' }),
    onSuccess: note => {
      setDraft('');
      setSelectedId(note.id);
      queryClient.invalidateQueries({ queryKey: ['quick-notes'] });
    },
  });

  const updateMutation = useMutation({
    mutationFn: () => updateQuickNote(selected!.id, {
      contentMarkdown: draft,
      status: selected!.status,
      attachmentIds: selected!.attachments.map(a => a.id),
    }),
    onSuccess: note => {
      setEditing(false);
      setDraft(note.contentMarkdown);
      queryClient.invalidateQueries({ queryKey: ['quick-notes'] });
      queryClient.invalidateQueries({ queryKey: ['quick-note', note.id] });
    },
  });

  const actionMutation = useMutation({
    mutationFn: async (action: 'process' | 'archive' | 'restore' | 'delete') => {
      if (!selectedId) return;
      if (action === 'process') await processQuickNote(selectedId);
      if (action === 'archive') await archiveQuickNote(selectedId);
      if (action === 'restore') await restoreQuickNote(selectedId, 'inbox');
      if (action === 'delete') await deleteQuickNote(selectedId);
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['quick-notes'] });
      queryClient.invalidateQueries({ queryKey: ['quick-note', selectedId] });
      if (status === 'archived') setSelectedId(null);
    },
  });

  const notes = listQuery.data?.items ?? [];
  const content = editing ? draft : selected?.contentMarkdown ?? '';
  const attachments = useMemo(() => selected?.attachments ?? [], [selected]);

  function openNote(id: string) {
    setSelectedId(id);
    setEditing(false);
    setDraft('');
  }

  function startEdit() {
    if (!selected) return;
    setDraft(selected.contentMarkdown);
    setEditing(true);
  }

  return (
    <div className="mx-auto flex max-w-[1500px] flex-col gap-4 pb-8">
      <PageHeader title="快速记录" subtitle="把文字、图片和文件先收进 PIM，再慢慢处理。" />

      <section className="pim-panel p-4">
        <div className="mb-3 flex items-center justify-between gap-3">
          <h2 className="text-sm font-semibold text-slate-900">新建快速记录</h2>
          <button
            type="button"
            onClick={() => createMutation.mutate()}
            disabled={createMutation.isPending || draft.trim().length === 0}
            className="pim-button-primary px-4 py-2 text-sm disabled:opacity-50"
          >
            {createMutation.isPending ? '保存中...' : '保存到收件箱'}
          </button>
        </div>
        <QuickNoteEditor value={draft} onChange={setDraft} minHeight={180} />
      </section>

      <div className="grid min-h-[560px] grid-cols-1 gap-4 xl:grid-cols-[380px_1fr]">
        <section className="pim-panel flex min-h-0 flex-col p-4">
          <div className="flex flex-wrap gap-2">
            {statusTabs.map(tab => (
              <button
                key={tab.key}
                type="button"
                onClick={() => setStatus(tab.key)}
                className={`rounded-lg px-3 py-1.5 text-sm ${
                  status === tab.key ? 'bg-blue-600 text-white' : 'bg-slate-100 text-slate-600 hover:bg-slate-200'
                }`}
              >
                {tab.label}
              </button>
            ))}
          </div>
          <input
            value={search}
            onChange={event => setSearch(event.target.value)}
            placeholder="搜索快速记录"
            className="mt-3 rounded-lg border border-slate-200 px-3 py-2 text-sm outline-none focus:border-blue-400"
          />
          <div className="mt-3 min-h-0 flex-1 overflow-auto space-y-2">
            {listQuery.isLoading ? (
              <EmptyState title="正在加载" description="快速记录正在读取。" />
            ) : notes.length === 0 ? (
              <EmptyState title="暂无记录" description="保存一条快速记录后会显示在这里。" />
            ) : (
              notes.map(note => (
                <button
                  key={note.id}
                  type="button"
                  onClick={() => openNote(note.id)}
                  className={`w-full rounded-lg border p-3 text-left transition ${
                    selectedId === note.id ? 'border-blue-300 bg-blue-50' : 'border-slate-200 bg-white hover:bg-slate-50'
                  }`}
                >
                  <p className="line-clamp-2 text-sm text-slate-900">{note.contentPreview || '空白记录'}</p>
                  <p className="mt-2 text-xs text-slate-500">
                    {new Date(note.updatedAt).toLocaleString('zh-CN')} · {note.attachmentCount} 个附件
                  </p>
                </button>
              ))
            )}
          </div>
        </section>

        <section className="pim-panel min-w-0 p-4">
          {!selected ? (
            <EmptyState title="选择一条快速记录" description="在左侧打开记录后可查看、编辑、归档或标记已处理。" />
          ) : (
            <div className="space-y-3">
              <div className="flex flex-wrap items-center justify-between gap-2">
                <div>
                  <h2 className="text-sm font-semibold text-slate-900">记录详情</h2>
                  <p className="text-xs text-slate-500">{selected.status} · {new Date(selected.updatedAt).toLocaleString('zh-CN')}</p>
                </div>
                <div className="flex flex-wrap gap-2">
                  {!editing && <button type="button" className="pim-button-secondary px-3 py-1.5 text-sm" onClick={startEdit}>编辑</button>}
                  {editing && <button type="button" className="pim-button-primary px-3 py-1.5 text-sm" onClick={() => updateMutation.mutate()}>保存编辑</button>}
                  <button type="button" className="pim-button-secondary px-3 py-1.5 text-sm" onClick={() => actionMutation.mutate('process')}>已处理</button>
                  <button type="button" className="pim-button-secondary px-3 py-1.5 text-sm" onClick={() => actionMutation.mutate(selected.status === 'archived' ? 'restore' : 'archive')}>
                    {selected.status === 'archived' ? '恢复' : '归档'}
                  </button>
                  <button type="button" className="rounded-lg border border-red-200 px-3 py-1.5 text-sm text-red-600 hover:bg-red-50" onClick={() => actionMutation.mutate('delete')}>删除</button>
                </div>
              </div>

              {editing ? (
                <QuickNoteEditor value={draft} onChange={setDraft} minHeight={360} />
              ) : (
                <QuickNoteMarkdownPreview markdown={content} attachments={attachments} />
              )}
            </div>
          )}
        </section>
      </div>
    </div>
  );
}
```

- [ ] **Step 2: 注册路由**

Modify `src/client-web/src/layout/AppLayout.tsx` imports:

```tsx
import QuickNotesPage from '../pages/QuickNotesPage';
```

Add route:

```tsx
<Route path="/quick-notes" element={<QuickNotesPage />} />
```

- [ ] **Step 3: 增加 Sidebar 导航项**

Modify `src/client-web/src/layout/Sidebar.tsx` `navItems`:

```ts
const navItems = [
  { label: '今日', path: '/today', short: '今' },
  { label: '快速记录', path: '/quick-notes', short: '记' },
  { label: '日历', path: '/calendar', short: '历' },
  { label: '任务', path: '/tasks', short: '任' },
  { label: 'PC记录', path: '/pc-tracker', short: 'PC' },
  { label: '分类管理', path: '/pc-classification', short: '分' },
  { label: '状态信息', path: '/status', short: '态' },
  { label: '设置', path: '/settings', short: '设' },
];
```

- [ ] **Step 4: 运行前端构建**

Run:

```powershell
npm --prefix src/client-web run build
```

Expected: PASS。

- [ ] **Step 5: Commit**

```powershell
git add src/client-web/src/pages/QuickNotesPage.tsx src/client-web/src/layout/AppLayout.tsx src/client-web/src/layout/Sidebar.tsx
git commit -m "feat(web): add quick notes page"
```

---

### Task 11: 添加验收文档和最终验证

**Files:**
- Create: `docs/operations/quick-notes-stage4-acceptance.md`

- [ ] **Step 1: 创建验收文档**

Create `docs/operations/quick-notes-stage4-acceptance.md`:

````markdown
# Quick Notes Stage 4 Acceptance

## Scope

Stage 4 implements quick note capture and management.

This stage does not implement:

- AI classification.
- Automatic task creation.
- Automatic event creation.
- Formal file-system organization.
- MCP server exposure.

## API Checks

- `GET /api/v1/quick-notes?status=inbox&page=1&pageSize=30` returns a paged note list.
- `POST /api/v1/quick-notes` creates an inbox note.
- `GET /api/v1/quick-notes/{id}` returns Markdown and attachments.
- `PUT /api/v1/quick-notes/{id}` updates Markdown.
- `POST /api/v1/quick-notes/{id}/process` marks a note processed.
- `POST /api/v1/quick-notes/{id}/archive` archives a note.
- `POST /api/v1/quick-notes/{id}/restore` restores a note.
- `DELETE /api/v1/quick-notes/{id}` soft deletes a note.
- `POST /api/v1/quick-notes/attachments` uploads a file.
- `GET /api/v1/quick-notes/attachments/{id}/download` downloads only user-owned attachments.

## Web Checks

- Open any authenticated Web route.
- Click the bottom-right quick note button.
- Confirm the panel opens and stays open after clicking outside.
- Drag the panel and confirm it moves.
- Close the panel with the close button.
- Reopen it and confirm an unsaved draft from the same browser session remains.
- Save a text-only note.
- Save a note with an inline image.
- Save a note with a non-image file link.
- Open `/quick-notes` from the sidebar.
- Confirm inbox, processed, and archived filters work.
- Edit a note in the full-page editor.
- Mark a note processed.
- Archive and restore a note.
- Delete a note.
- Download an attachment from a note.

## Verification Commands

```powershell
dotnet test Pim.sln
npm --prefix src/client-web run build
npm --prefix src/client-web exec tsx -- ..\..\tests\client-web\quickNotesApiPath.test.ts
npm --prefix src/client-web exec tsc -- -p ..\..\tests\client-web\tsconfig.quick-notes.json
npm --prefix src/client-web exec tsx -- ..\..\tests\client-web\quickNoteFloatingState.test.ts
```
````

- [ ] **Step 2: 运行后端全量测试**

Run:

```powershell
dotnet test Pim.sln
```

Expected: PASS。

- [ ] **Step 3: 运行前端构建**

Run:

```powershell
npm --prefix src/client-web run build
```

Expected: PASS。

- [ ] **Step 4: 运行前端 focused tests**

Run:

```powershell
npm --prefix src/client-web exec tsx -- ..\..\tests\client-web\quickNotesApiPath.test.ts
npm --prefix src/client-web exec tsc -- -p ..\..\tests\client-web\tsconfig.quick-notes.json
npm --prefix src/client-web exec tsx -- ..\..\tests\client-web\quickNoteFloatingState.test.ts
```

Expected: PASS。

- [ ] **Step 5: 检查 git 状态**

Run:

```powershell
git status --short --branch
```

Expected: 只剩预先存在的 `docs/plan.md` 未跟踪，或工作区完全干净。

- [ ] **Step 6: Commit**

```powershell
git add docs/operations/quick-notes-stage4-acceptance.md
git commit -m "docs: add quick notes stage 4 acceptance"
```

---

## Plan Self-Review

- Spec coverage: 本计划覆盖数据模型、附件表、Markdown 事实来源、MDXEditor、全局可拖拽常住浮窗、显式关闭、独立 `/quick-notes` 页面、状态筛选、CRUD/status API、附件上传下载、用户隔离、审计、验收文档和最终验证。
- Out-of-scope coverage: 计划没有实现 AI 分类、任务/日程自动创建、MCP server、正式文件系统集成或孤儿附件后台清理。
- Type consistency: 后端统一使用 `QuickNoteDetailDto`、`QuickNoteAttachmentDto`、`QuickNoteService`、`QuickNoteAttachmentService`；前端统一使用 `QuickNoteStatus`、`QuickNoteDetail`、`quickNoteApiPaths`。
- Placeholder scan: 本计划没有 `TBD`、`TODO`、`FIXME` 或“类似 Task N”的占位描述；代码块中的 `??` 是 C#/TypeScript 空值合并运算符，不是占位符。

