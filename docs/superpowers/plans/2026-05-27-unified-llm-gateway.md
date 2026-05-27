# Unified LLM Gateway Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add one governed AI gateway for PIM so backend modules call `IAiGateway`, requests route through LiteLLM, every attempt is logged, structured JSON is schema-validated, and `/settings/ai` exposes status, usage, logs, and request detail.

**Architecture:** Core owns AI contracts and DTOs, Infrastructure owns LiteLLM/OpenAI-compatible chat calls, EF persistence, redaction, schema validation, and usage queries, API maps authenticated `/api/v1/ai/*` endpoints, and the web client renders the AI settings surface. LiteLLM runs as a Docker Compose service on the internal network, while PIM stores only the LiteLLM virtual key and PIM-level request traces.

**Tech Stack:** .NET 8, ASP.NET Core minimal APIs, EF Core/Npgsql, `Microsoft.Extensions.AI.OpenAI` 10.6.0, `JsonSchema.Net` 9.2.1, LiteLLM Proxy Docker image, React/Vite/TypeScript, TanStack Query, Tailwind.

---

## Source Spec

- `docs/superpowers/specs/2026-05-27-unified-llm-gateway-design.md`

## Baseline Verified Before Planning

- `dotnet test Pim.sln`
  - Expected baseline: PASS, 313 tests.
  - Existing warnings: nullable warnings in Calendar and Auth endpoints.
- `npm --prefix src/client-web install`
  - Expected baseline: packages restored.
- `npm --prefix src/client-web run build`
  - Expected baseline: PASS with the existing Vite chunk-size warning.

## File Structure

Create these backend contract files:

- `src/Pim.Core/Ai/AiEnums.cs` - AI message roles and request statuses.
- `src/Pim.Core/Ai/AiDtos.cs` - gateway request/result DTOs, status DTOs, request log DTOs, usage DTOs, and filters.
- `src/Pim.Core/Ai/IAiGateway.cs` - business-facing gateway interface.
- `src/Pim.Core/Ai/IAiSchemaRegistry.cs` - schema registration and lookup interface.
- `src/Pim.Core/Ai/IAiUsageService.cs` - status, log list, detail, and usage interface.

Create these backend infrastructure files:

- `src/Pim.Infrastructure/Ai/AiOptions.cs` - configuration binding for `Ai:*` settings.
- `src/Pim.Infrastructure/Ai/AiChatClientFactory.cs` - creates an OpenAI-compatible `IChatClient` pointed at LiteLLM.
- `src/Pim.Infrastructure/Ai/AiGateway.cs` - applies enablement, attempts, schema validation, repair prompts, provider calls, and log writes.
- `src/Pim.Infrastructure/Ai/AiRedactor.cs` - redacts credential-bearing fields before persistence.
- `src/Pim.Infrastructure/Ai/AiRequestLogWriter.cs` - persists every attempt.
- `src/Pim.Infrastructure/Ai/AiSchemaRegistry.cs` - in-code schema registry with schema snapshots.
- `src/Pim.Infrastructure/Ai/AiUsageService.cs` - reads request logs for status, list, detail, and summaries.
- `src/Pim.Infrastructure/Ai/AiProviderHealthService.cs` - checks LiteLLM reachability and default model.
- `src/Pim.Infrastructure/Data/Entities/AiProviderSettingEntity.cs` - system-level provider state.
- `src/Pim.Infrastructure/Data/Entities/AiRequestLogEntity.cs` - complete per-attempt request log.
- `src/Pim.Infrastructure/Data/Migrations/<timestamp>_AddAiGateway.cs` - EF migration for AI tables.

Modify these backend files:

- `src/Pim.Core/Pim.Core.csproj` - no package changes expected.
- `src/Pim.Infrastructure/Pim.Infrastructure.csproj` - add AI and JSON Schema packages.
- `src/Pim.Infrastructure/Data/PimDbContext.cs` - add DbSets and table mapping.
- `src/Pim.Infrastructure/Extensions/ServiceCollectionExtensions.cs` - bind options and register AI services.
- `src/Pim.Api/Endpoints/AiEndpoints.cs` - create authenticated AI endpoints.
- `src/Pim.Api/Program.cs` - call `app.MapAiEndpoints()`.
- `src/Pim.Api/appsettings.json` and `src/Pim.Api/appsettings.Development.json` - add default AI settings.
- `tests/Pim.UnitTests/Pim.UnitTests.csproj` - package references only if needed by tests.

Modify deployment files:

- `docker-compose.yml` - add `litellm` service and API environment variables.
- `.env.example` - document LiteLLM and upstream provider values without real secrets.
- `litellm-config.yaml` - model alias and LiteLLM settings without credentials.

Create these frontend files:

- `src/client-web/src/api/ai.ts` - API client, normalizers, and endpoint path constants.
- `src/client-web/src/pages/AiSettingsPage.tsx` - `/settings/ai` page.
- `src/client-web/src/components/ai/AiStatusPanel.tsx` - configuration status and test action.
- `src/client-web/src/components/ai/AiUsageOverview.tsx` - usage counters and grouped usage.
- `src/client-web/src/components/ai/AiRequestLogTable.tsx` - filters and log table.
- `src/client-web/src/components/ai/AiRequestDetailPanel.tsx` - detail drawer/panel.

Modify these frontend files:

- `src/client-web/src/App.tsx` - no route changes here because `AppLayout` owns authenticated routes.
- `src/client-web/src/layout/AppLayout.tsx` - add `/settings/ai` route.
- `src/client-web/src/pages/SettingsPage.tsx` - add AI settings entry.
- `src/client-web/src/types/index.ts` - export AI types.

---

### Task 1: Core AI Contracts

**Files:**
- Create: `src/Pim.Core/Ai/AiEnums.cs`
- Create: `src/Pim.Core/Ai/AiDtos.cs`
- Create: `src/Pim.Core/Ai/IAiGateway.cs`
- Create: `src/Pim.Core/Ai/IAiSchemaRegistry.cs`
- Create: `src/Pim.Core/Ai/IAiUsageService.cs`
- Create: `tests/Pim.UnitTests/Ai/AiContractTests.cs`

- [ ] **Step 1: Write failing contract tests**

Create `tests/Pim.UnitTests/Ai/AiContractTests.cs`:

```csharp
using Pim.Core.Ai;
using Xunit;

namespace Pim.UnitTests.Ai;

public class AiContractTests
{
    [Fact]
    public void AiGatewayRequest_ClampsAttemptsToFirstVersionHardLimit()
    {
        var request = new AiGatewayRequest(
            Module: "quick-notes",
            Purpose: "quick-notes.convert",
            SourceObjectType: "quick_note",
            SourceObjectId: "note-1",
            Messages: [new AiMessage(AiMessageRole.User, "convert this note")],
            Model: null,
            SchemaName: "quick-note-conversion",
            SchemaVersion: "1",
            MaxOutputTokens: 800,
            MaxAttempts: 9,
            Metadata: new Dictionary<string, string> { ["origin"] = "unit-test" });

        Assert.Equal(2, request.EffectiveMaxAttempts);
    }

    [Fact]
    public void AiResult_FailedValidationIncludesUserFacingErrorAndLogId()
    {
        var logId = Guid.Parse("11111111-1111-1111-1111-111111111111");

        var result = AiResult.FailedValidation(
            logId,
            ["$.title is required"]);

        Assert.Equal(AiRequestStatus.FailedValidation, result.Status);
        Assert.Equal(logId, result.LogId);
        Assert.Contains("AI response did not match the required format", result.UserFacingError);
        Assert.Equal(["$.title is required"], result.SchemaValidationErrors);
    }
}
```

- [ ] **Step 2: Run the tests and verify they fail**

Run:

```powershell
dotnet test tests\Pim.UnitTests\Pim.UnitTests.csproj --filter AiContractTests
```

Expected: FAIL because the `Pim.Core.Ai` namespace and contract types do not exist.

- [ ] **Step 3: Add core enums**

Create `src/Pim.Core/Ai/AiEnums.cs`:

```csharp
namespace Pim.Core.Ai;

public enum AiMessageRole
{
    System,
    User,
    Assistant
}

public enum AiRequestStatus
{
    Succeeded,
    Failed,
    Blocked,
    TimedOut,
    FailedValidation
}
```

- [ ] **Step 4: Add core DTOs**

Create `src/Pim.Core/Ai/AiDtos.cs`:

```csharp
using Pim.Core.Common;

namespace Pim.Core.Ai;

public sealed record AiMessage(AiMessageRole Role, string Content);

public sealed record AiGatewayRequest(
    string Module,
    string Purpose,
    string SourceObjectType,
    string SourceObjectId,
    IReadOnlyList<AiMessage> Messages,
    string? Model = null,
    string? SchemaName = null,
    string? SchemaVersion = null,
    int? MaxOutputTokens = null,
    int? MaxAttempts = null,
    IReadOnlyDictionary<string, string>? Metadata = null)
{
    public int EffectiveMaxAttempts => Math.Clamp(MaxAttempts ?? 1, 1, 2);
}

public sealed record AiTokenUsage(
    int? PromptTokens,
    int? CompletionTokens,
    int? TotalTokens,
    decimal? EstimatedCost,
    string? Currency);

public sealed record AiResult(
    AiRequestStatus Status,
    string? ResponseText,
    string? ParsedOutputJson,
    IReadOnlyList<string> SchemaValidationErrors,
    AiTokenUsage Usage,
    Guid? LogId,
    string? UserFacingError)
{
    public static AiResult FailedValidation(Guid? logId, IReadOnlyList<string> errors) =>
        new(
            AiRequestStatus.FailedValidation,
            ResponseText: null,
            ParsedOutputJson: null,
            SchemaValidationErrors: errors,
            Usage: new AiTokenUsage(null, null, null, null, null),
            LogId: logId,
            UserFacingError: "AI response did not match the required format. No suggestion was produced.");
}

public sealed record AiSchemaDefinition(
    string Name,
    string Version,
    string JsonSchema,
    string Description);

public sealed record AiStatusDto(
    bool Enabled,
    string Provider,
    string BaseUrl,
    string DefaultModel,
    DateTimeOffset? LastHealthCheckAt,
    string? LastError,
    DateTimeOffset? RecentSuccessfulCallAt);

public sealed record AiRequestLogFilter(
    DateTimeOffset? From,
    DateTimeOffset? To,
    string? Module,
    string? Purpose,
    string? SourceObjectType,
    string? SourceObjectId,
    string? Model,
    AiRequestStatus? Status,
    Guid? UserId,
    int Page = 1,
    int PageSize = 50);

public sealed record AiRequestLogListItemDto(
    Guid Id,
    DateTimeOffset StartedAt,
    string Module,
    string Purpose,
    string Model,
    AiRequestStatus Status,
    int? TotalTokens,
    decimal? EstimatedCost,
    long? DurationMs,
    string SourceObjectType,
    string SourceObjectId,
    string? ErrorSummary);

public sealed record AiRequestLogDetailDto(
    Guid Id,
    Guid? UserId,
    string Module,
    string Purpose,
    string SourceObjectType,
    string SourceObjectId,
    string Provider,
    string Model,
    string? LiteLlmRequestId,
    string CorrelationId,
    AiRequestStatus Status,
    int AttemptNumber,
    int MaxAttempts,
    DateTimeOffset StartedAt,
    DateTimeOffset? FinishedAt,
    long? DurationMs,
    string RequestMessagesJson,
    string RequestPayloadJson,
    string ResponseRawJson,
    string? ResponseText,
    string? ParsedOutputJson,
    string? SchemaName,
    string? SchemaVersion,
    string? SchemaJsonSnapshot,
    string SchemaValidationErrorsJson,
    AiTokenUsage Usage,
    string? ErrorCode,
    string? ErrorMessage,
    string MetadataJson);

public sealed record AiUsageGroupDto(
    string GroupKey,
    int RequestCount,
    int SuccessCount,
    int FailureCount,
    int PromptTokens,
    int CompletionTokens,
    int TotalTokens,
    decimal EstimatedCost);

public sealed record AiUsageSummaryDto(
    int RequestCount,
    int SuccessCount,
    int FailureCount,
    int PromptTokens,
    int CompletionTokens,
    int TotalTokens,
    decimal EstimatedCost,
    IReadOnlyList<AiUsageGroupDto> ByModule,
    IReadOnlyList<AiUsageGroupDto> ByPurpose,
    IReadOnlyList<AiUsageGroupDto> ByModel,
    IReadOnlyList<AiUsageGroupDto> ByStatus);
```

- [ ] **Step 5: Add core interfaces**

Create `src/Pim.Core/Ai/IAiGateway.cs`:

```csharp
namespace Pim.Core.Ai;

public interface IAiGateway
{
    Task<AiResult> CompleteAsync(AiGatewayRequest request, CancellationToken ct = default);
}
```

Create `src/Pim.Core/Ai/IAiSchemaRegistry.cs`:

```csharp
namespace Pim.Core.Ai;

public interface IAiSchemaRegistry
{
    void Register(AiSchemaDefinition schema);
    AiSchemaDefinition? Get(string name, string version);
}
```

Create `src/Pim.Core/Ai/IAiUsageService.cs`:

```csharp
using Pim.Core.Common;

namespace Pim.Core.Ai;

public interface IAiUsageService
{
    Task<AiStatusDto> GetStatusAsync(CancellationToken ct = default);
    Task<PagedResult<AiRequestLogListItemDto>> ListRequestsAsync(AiRequestLogFilter filter, CancellationToken ct = default);
    Task<AiRequestLogDetailDto?> GetRequestDetailAsync(Guid id, CancellationToken ct = default);
    Task<AiUsageSummaryDto> GetUsageSummaryAsync(DateTimeOffset? from, DateTimeOffset? to, CancellationToken ct = default);
}
```

- [ ] **Step 6: Run contract tests and commit**

Run:

```powershell
dotnet test tests\Pim.UnitTests\Pim.UnitTests.csproj --filter AiContractTests
```

Expected: PASS.

Commit:

```powershell
git add src\Pim.Core\Ai tests\Pim.UnitTests\Ai\AiContractTests.cs
git commit -m "feat: add ai gateway contracts"
```

---

### Task 2: AI Persistence Model

**Files:**
- Create: `src/Pim.Infrastructure/Data/Entities/AiProviderSettingEntity.cs`
- Create: `src/Pim.Infrastructure/Data/Entities/AiRequestLogEntity.cs`
- Modify: `src/Pim.Infrastructure/Data/PimDbContext.cs`
- Create: `tests/Pim.UnitTests/Ai/AiPersistenceModelTests.cs`
- Create: `src/Pim.Infrastructure/Data/Migrations/<timestamp>_AddAiGateway.cs`

- [ ] **Step 1: Write failing persistence tests**

Create `tests/Pim.UnitTests/Ai/AiPersistenceModelTests.cs`:

```csharp
using Microsoft.EntityFrameworkCore;
using Pim.Infrastructure.Data;
using Pim.Infrastructure.Data.Entities;
using Xunit;

namespace Pim.UnitTests.Ai;

public class AiPersistenceModelTests
{
    [Fact]
    public async Task AiRequestLogs_PersistCompleteAttemptTrace()
    {
        var options = new DbContextOptionsBuilder<PimDbContext>()
            .UseInMemoryDatabase(Guid.NewGuid().ToString())
            .Options;

        await using var db = new PimDbContext(options);
        var id = Guid.Parse("22222222-2222-2222-2222-222222222222");

        db.AiRequestLogs.Add(new AiRequestLogEntity
        {
            Id = id,
            UserId = Guid.Parse("33333333-3333-3333-3333-333333333333"),
            Module = "quick-notes",
            Purpose = "quick-notes.convert",
            SourceObjectType = "quick_note",
            SourceObjectId = "note-1",
            Provider = "litellm",
            Model = "pim-default",
            CorrelationId = "corr-1",
            Status = "succeeded",
            AttemptNumber = 1,
            MaxAttempts = 1,
            RequestMessagesJson = "[{\"role\":\"user\",\"content\":\"hello\"}]",
            RequestPayloadJson = "{\"model\":\"pim-default\"}",
            ResponseRawJson = "{\"id\":\"chatcmpl-1\"}",
            ResponseText = "{\"title\":\"Hello\"}",
            ParsedOutputJson = "{\"title\":\"Hello\"}",
            SchemaName = "quick-note-conversion",
            SchemaVersion = "1",
            SchemaJsonSnapshot = "{\"type\":\"object\"}",
            SchemaValidationErrorsJson = "[]",
            PromptTokens = 4,
            CompletionTokens = 6,
            TotalTokens = 10,
            EstimatedCost = 0.00012m,
            Currency = "USD",
            InputChars = 5,
            OutputChars = 17,
            InputHash = "input-hash",
            OutputHash = "output-hash",
            MetadataJson = "{\"origin\":\"unit-test\"}"
        });
        await db.SaveChangesAsync();

        var saved = await db.AiRequestLogs.SingleAsync(l => l.Id == id);
        Assert.Equal("litellm", saved.Provider);
        Assert.Equal(10, saved.TotalTokens);
        Assert.Equal("{\"title\":\"Hello\"}", saved.ParsedOutputJson);
    }

    [Fact]
    public async Task AiProviderSettings_PersistSystemProviderState()
    {
        var options = new DbContextOptionsBuilder<PimDbContext>()
            .UseInMemoryDatabase(Guid.NewGuid().ToString())
            .Options;

        await using var db = new PimDbContext(options);
        db.AiProviderSettings.Add(new AiProviderSettingEntity
        {
            Provider = "litellm",
            BaseUrl = "http://litellm:4000",
            VirtualKeySecret = "encrypted-secret",
            DefaultModel = "pim-default",
            Status = "enabled"
        });
        await db.SaveChangesAsync();

        var saved = await db.AiProviderSettings.SingleAsync();
        Assert.Equal("litellm", saved.Provider);
        Assert.Equal("http://litellm:4000", saved.BaseUrl);
    }
}
```

- [ ] **Step 2: Run the tests and verify they fail**

Run:

```powershell
dotnet test tests\Pim.UnitTests\Pim.UnitTests.csproj --filter AiPersistenceModelTests
```

Expected: FAIL because AI entities and DbSets do not exist.

- [ ] **Step 3: Add AI entities**

Create `src/Pim.Infrastructure/Data/Entities/AiProviderSettingEntity.cs`:

```csharp
using System.ComponentModel.DataAnnotations;
using System.ComponentModel.DataAnnotations.Schema;

namespace Pim.Infrastructure.Data.Entities;

[Table("ai_provider_settings")]
public sealed class AiProviderSettingEntity
{
    [Key]
    [Column("id")]
    public Guid Id { get; set; } = Guid.NewGuid();

    [Column("provider")]
    [MaxLength(32)]
    public string Provider { get; set; } = "litellm";

    [Column("base_url")]
    [MaxLength(512)]
    public string BaseUrl { get; set; } = string.Empty;

    [Column("virtual_key_secret")]
    public string VirtualKeySecret { get; set; } = string.Empty;

    [Column("default_model")]
    [MaxLength(128)]
    public string DefaultModel { get; set; } = string.Empty;

    [Column("status")]
    [MaxLength(32)]
    public string Status { get; set; } = "disabled";

    [Column("last_health_check_at")]
    public DateTimeOffset? LastHealthCheckAt { get; set; }

    [Column("last_error")]
    public string? LastError { get; set; }

    [Column("created_at")]
    public DateTimeOffset CreatedAt { get; set; } = DateTimeOffset.UtcNow;

    [Column("updated_at")]
    public DateTimeOffset UpdatedAt { get; set; } = DateTimeOffset.UtcNow;
}
```

Create `src/Pim.Infrastructure/Data/Entities/AiRequestLogEntity.cs`:

```csharp
using System.ComponentModel.DataAnnotations;
using System.ComponentModel.DataAnnotations.Schema;

namespace Pim.Infrastructure.Data.Entities;

[Table("ai_request_logs")]
public sealed class AiRequestLogEntity
{
    [Key]
    [Column("id")]
    public Guid Id { get; set; } = Guid.NewGuid();

    [Column("user_id")]
    public Guid? UserId { get; set; }

    [Column("module")]
    [MaxLength(128)]
    public string Module { get; set; } = string.Empty;

    [Column("purpose")]
    [MaxLength(128)]
    public string Purpose { get; set; } = string.Empty;

    [Column("source_object_type")]
    [MaxLength(128)]
    public string SourceObjectType { get; set; } = string.Empty;

    [Column("source_object_id")]
    [MaxLength(256)]
    public string SourceObjectId { get; set; } = string.Empty;

    [Column("provider")]
    [MaxLength(32)]
    public string Provider { get; set; } = "litellm";

    [Column("model")]
    [MaxLength(128)]
    public string Model { get; set; } = string.Empty;

    [Column("litellm_request_id")]
    [MaxLength(128)]
    public string? LiteLlmRequestId { get; set; }

    [Column("correlation_id")]
    [MaxLength(128)]
    public string CorrelationId { get; set; } = string.Empty;

    [Column("status")]
    [MaxLength(32)]
    public string Status { get; set; } = string.Empty;

    [Column("attempt_number")]
    public int AttemptNumber { get; set; }

    [Column("max_attempts")]
    public int MaxAttempts { get; set; }

    [Column("started_at")]
    public DateTimeOffset StartedAt { get; set; } = DateTimeOffset.UtcNow;

    [Column("finished_at")]
    public DateTimeOffset? FinishedAt { get; set; }

    [Column("duration_ms")]
    public long? DurationMs { get; set; }

    [Column("request_messages_json", TypeName = "jsonb")]
    public string RequestMessagesJson { get; set; } = "[]";

    [Column("request_payload_json", TypeName = "jsonb")]
    public string RequestPayloadJson { get; set; } = "{}";

    [Column("response_raw_json", TypeName = "jsonb")]
    public string ResponseRawJson { get; set; } = "{}";

    [Column("response_text")]
    public string? ResponseText { get; set; }

    [Column("parsed_output_json", TypeName = "jsonb")]
    public string? ParsedOutputJson { get; set; }

    [Column("schema_name")]
    [MaxLength(128)]
    public string? SchemaName { get; set; }

    [Column("schema_version")]
    [MaxLength(32)]
    public string? SchemaVersion { get; set; }

    [Column("schema_json_snapshot", TypeName = "jsonb")]
    public string? SchemaJsonSnapshot { get; set; }

    [Column("schema_validation_errors_json", TypeName = "jsonb")]
    public string SchemaValidationErrorsJson { get; set; } = "[]";

    [Column("prompt_tokens")]
    public int? PromptTokens { get; set; }

    [Column("completion_tokens")]
    public int? CompletionTokens { get; set; }

    [Column("total_tokens")]
    public int? TotalTokens { get; set; }

    [Column("estimated_cost")]
    public decimal? EstimatedCost { get; set; }

    [Column("currency")]
    [MaxLength(16)]
    public string? Currency { get; set; }

    [Column("input_chars")]
    public int InputChars { get; set; }

    [Column("output_chars")]
    public int OutputChars { get; set; }

    [Column("input_hash")]
    [MaxLength(128)]
    public string InputHash { get; set; } = string.Empty;

    [Column("output_hash")]
    [MaxLength(128)]
    public string OutputHash { get; set; } = string.Empty;

    [Column("error_code")]
    [MaxLength(128)]
    public string? ErrorCode { get; set; }

    [Column("error_message")]
    public string? ErrorMessage { get; set; }

    [Column("metadata_json", TypeName = "jsonb")]
    public string MetadataJson { get; set; } = "{}";
}
```

- [ ] **Step 4: Register DbSets and mappings**

Modify `src/Pim.Infrastructure/Data/PimDbContext.cs`:

```csharp
public DbSet<AiProviderSettingEntity> AiProviderSettings => Set<AiProviderSettingEntity>();
public DbSet<AiRequestLogEntity> AiRequestLogs => Set<AiRequestLogEntity>();
```

Add this block inside `OnModelCreating` before module assembly configuration:

```csharp
modelBuilder.Entity<AiProviderSettingEntity>(e =>
{
    e.Property(a => a.Provider).HasDefaultValue("litellm");
    e.Property(a => a.Status).HasDefaultValue("disabled");
    e.Property(a => a.CreatedAt).HasDefaultValueSql("now()");
    e.Property(a => a.UpdatedAt).HasDefaultValueSql("now()");
    e.HasIndex(a => a.Provider).IsUnique();
    e.HasIndex(a => a.Status);
});

modelBuilder.Entity<AiRequestLogEntity>(e =>
{
    e.Property(a => a.Provider).HasDefaultValue("litellm");
    e.Property(a => a.RequestMessagesJson).HasDefaultValue("[]");
    e.Property(a => a.RequestPayloadJson).HasDefaultValue("{}");
    e.Property(a => a.ResponseRawJson).HasDefaultValue("{}");
    e.Property(a => a.SchemaValidationErrorsJson).HasDefaultValue("[]");
    e.Property(a => a.MetadataJson).HasDefaultValue("{}");
    e.HasIndex(a => a.UserId);
    e.HasIndex(a => a.Module);
    e.HasIndex(a => a.Purpose);
    e.HasIndex(a => a.Model);
    e.HasIndex(a => a.Status);
    e.HasIndex(a => a.StartedAt);
    e.HasIndex(a => new { a.SourceObjectType, a.SourceObjectId });
    e.HasIndex(a => a.CorrelationId);
});
```

- [ ] **Step 5: Run persistence tests**

Run:

```powershell
dotnet test tests\Pim.UnitTests\Pim.UnitTests.csproj --filter AiPersistenceModelTests
```

Expected: PASS.

- [ ] **Step 6: Add EF migration**

Run:

```powershell
dotnet ef migrations add AddAiGateway --project src\Pim.Infrastructure --startup-project src\Pim.Api
```

Expected: a migration containing `CreateTable("ai_provider_settings")`, `CreateTable("ai_request_logs")`, and indexes for status, module, purpose, model, started time, source object, user, and correlation id.

- [ ] **Step 7: Run solution tests and commit**

Run:

```powershell
dotnet test Pim.sln
```

Expected: PASS.

Commit:

```powershell
git add src\Pim.Infrastructure\Data src\Pim.Infrastructure\Data\Migrations tests\Pim.UnitTests\Ai\AiPersistenceModelTests.cs
git commit -m "feat: persist ai gateway request logs"
```

---

### Task 3: Redaction And Request Log Writer

**Files:**
- Create: `src/Pim.Infrastructure/Ai/AiRedactor.cs`
- Create: `src/Pim.Infrastructure/Ai/AiRequestLogWriter.cs`
- Create: `tests/Pim.UnitTests/Ai/AiRedactorTests.cs`
- Create: `tests/Pim.UnitTests/Ai/AiRequestLogWriterTests.cs`

- [ ] **Step 1: Write failing redaction tests**

Create `tests/Pim.UnitTests/Ai/AiRedactorTests.cs`:

```csharp
using Pim.Infrastructure.Ai;
using Xunit;

namespace Pim.UnitTests.Ai;

public class AiRedactorTests
{
    [Fact]
    public void RedactJson_RemovesKnownCredentialFields()
    {
        var json = """
        {
          "Authorization": "Bearer secret-token",
          "api_key": "sk-live-secret",
          "refresh_token": "refresh-secret",
          "nested": { "nextcloud_app_password": "app-secret" },
          "safe": "keep-me"
        }
        """;

        var redacted = AiRedactor.RedactJson(json);

        Assert.DoesNotContain("secret-token", redacted);
        Assert.DoesNotContain("sk-live-secret", redacted);
        Assert.DoesNotContain("refresh-secret", redacted);
        Assert.DoesNotContain("app-secret", redacted);
        Assert.Contains("keep-me", redacted);
        Assert.Contains("[REDACTED]", redacted);
    }
}
```

- [ ] **Step 2: Write failing log writer tests**

Create `tests/Pim.UnitTests/Ai/AiRequestLogWriterTests.cs`:

```csharp
using Microsoft.EntityFrameworkCore;
using Pim.Core.Ai;
using Pim.Infrastructure.Ai;
using Pim.Infrastructure.Data;
using Xunit;

namespace Pim.UnitTests.Ai;

public class AiRequestLogWriterTests
{
    [Fact]
    public async Task WriteAsync_PersistsFailuresWithRedactedPayloads()
    {
        var options = new DbContextOptionsBuilder<PimDbContext>()
            .UseInMemoryDatabase(Guid.NewGuid().ToString())
            .Options;

        await using var db = new PimDbContext(options);
        var writer = new AiRequestLogWriter(db);

        var id = await writer.WriteAsync(new AiRequestLogWriteModel(
            UserId: null,
            Module: "files",
            Purpose: "files.summarize",
            SourceObjectType: "file",
            SourceObjectId: "file-1",
            Provider: "litellm",
            Model: "pim-default",
            LiteLlmRequestId: null,
            CorrelationId: "corr-1",
            Status: AiRequestStatus.Failed,
            AttemptNumber: 1,
            MaxAttempts: 1,
            StartedAt: DateTimeOffset.UtcNow,
            FinishedAt: DateTimeOffset.UtcNow.AddMilliseconds(40),
            RequestMessagesJson: """[{"role":"user","content":"hello"}]""",
            RequestPayloadJson: """{"api_key":"sk-secret","model":"pim-default"}""",
            ResponseRawJson: """{"error":"bad key"}""",
            ResponseText: null,
            ParsedOutputJson: null,
            SchemaName: null,
            SchemaVersion: null,
            SchemaJsonSnapshot: null,
            SchemaValidationErrorsJson: "[]",
            PromptTokens: null,
            CompletionTokens: null,
            TotalTokens: null,
            EstimatedCost: null,
            Currency: null,
            ErrorCode: "provider_unavailable",
            ErrorMessage: "LiteLLM returned 401",
            MetadataJson: """{"Authorization":"Bearer secret"}"""), CancellationToken.None);

        var saved = await db.AiRequestLogs.SingleAsync(l => l.Id == id);
        Assert.Equal("failed", saved.Status);
        Assert.DoesNotContain("sk-secret", saved.RequestPayloadJson);
        Assert.DoesNotContain("secret", saved.MetadataJson);
        Assert.Equal("provider_unavailable", saved.ErrorCode);
    }
}
```

- [ ] **Step 3: Run the tests and verify they fail**

Run:

```powershell
dotnet test tests\Pim.UnitTests\Pim.UnitTests.csproj --filter "AiRedactorTests|AiRequestLogWriterTests"
```

Expected: FAIL because redaction and writer classes do not exist.

- [ ] **Step 4: Implement redactor**

Create `src/Pim.Infrastructure/Ai/AiRedactor.cs`:

```csharp
using System.Text.Json;
using System.Text.RegularExpressions;

namespace Pim.Infrastructure.Ai;

public static partial class AiRedactor
{
    private static readonly HashSet<string> SensitiveKeys = new(StringComparer.OrdinalIgnoreCase)
    {
        "authorization",
        "api_key",
        "apikey",
        "access_token",
        "refresh_token",
        "jwt",
        "password",
        "app_password",
        "nextcloud_app_password",
        "virtual_key",
        "virtual_key_secret",
        "litellm_virtual_key"
    };

    public static string RedactJson(string? json)
    {
        if (string.IsNullOrWhiteSpace(json))
        {
            return "{}";
        }

        try
        {
            using var document = JsonDocument.Parse(json);
            using var stream = new MemoryStream();
            using (var writer = new Utf8JsonWriter(stream))
            {
                WriteRedacted(document.RootElement, writer, null);
            }

            return System.Text.Encoding.UTF8.GetString(stream.ToArray());
        }
        catch (JsonException)
        {
            return TokenLikeValueRegex().Replace(json, "[REDACTED]");
        }
    }

    private static void WriteRedacted(JsonElement element, Utf8JsonWriter writer, string? propertyName)
    {
        if (propertyName is not null && SensitiveKeys.Contains(propertyName))
        {
            writer.WriteStringValue("[REDACTED]");
            return;
        }

        switch (element.ValueKind)
        {
            case JsonValueKind.Object:
                writer.WriteStartObject();
                foreach (var property in element.EnumerateObject())
                {
                    writer.WritePropertyName(property.Name);
                    WriteRedacted(property.Value, writer, property.Name);
                }
                writer.WriteEndObject();
                break;
            case JsonValueKind.Array:
                writer.WriteStartArray();
                foreach (var item in element.EnumerateArray())
                {
                    WriteRedacted(item, writer, null);
                }
                writer.WriteEndArray();
                break;
            case JsonValueKind.String:
                writer.WriteStringValue(TokenLikeValueRegex().Replace(element.GetString() ?? string.Empty, "[REDACTED]"));
                break;
            default:
                element.WriteTo(writer);
                break;
        }
    }

    [GeneratedRegex(@"(?i)(bearer\s+[a-z0-9._\-]+|sk-[a-z0-9_\-]{8,}|eyJ[a-z0-9_\-]+\.[a-z0-9_\-]+\.[a-z0-9_\-]+)")]
    private static partial Regex TokenLikeValueRegex();
}
```

- [ ] **Step 5: Implement log write model and writer**

Create `src/Pim.Infrastructure/Ai/AiRequestLogWriter.cs`:

```csharp
using System.Security.Cryptography;
using System.Text;
using Pim.Core.Ai;
using Pim.Infrastructure.Data;
using Pim.Infrastructure.Data.Entities;

namespace Pim.Infrastructure.Ai;

public sealed record AiRequestLogWriteModel(
    Guid? UserId,
    string Module,
    string Purpose,
    string SourceObjectType,
    string SourceObjectId,
    string Provider,
    string Model,
    string? LiteLlmRequestId,
    string CorrelationId,
    AiRequestStatus Status,
    int AttemptNumber,
    int MaxAttempts,
    DateTimeOffset StartedAt,
    DateTimeOffset FinishedAt,
    string RequestMessagesJson,
    string RequestPayloadJson,
    string ResponseRawJson,
    string? ResponseText,
    string? ParsedOutputJson,
    string? SchemaName,
    string? SchemaVersion,
    string? SchemaJsonSnapshot,
    string SchemaValidationErrorsJson,
    int? PromptTokens,
    int? CompletionTokens,
    int? TotalTokens,
    decimal? EstimatedCost,
    string? Currency,
    string? ErrorCode,
    string? ErrorMessage,
    string MetadataJson);

public interface IAiRequestLogWriter
{
    Task<Guid> WriteAsync(AiRequestLogWriteModel model, CancellationToken ct = default);
}

public sealed class AiRequestLogWriter(PimDbContext db) : IAiRequestLogWriter
{
    public async Task<Guid> WriteAsync(AiRequestLogWriteModel model, CancellationToken ct = default)
    {
        var redactedMessages = AiRedactor.RedactJson(model.RequestMessagesJson);
        var redactedPayload = AiRedactor.RedactJson(model.RequestPayloadJson);
        var redactedResponseRaw = AiRedactor.RedactJson(model.ResponseRawJson);
        var redactedMetadata = AiRedactor.RedactJson(model.MetadataJson);
        var redactedResponseText = model.ResponseText is null ? null : AiRedactor.RedactJson(ToJsonString(model.ResponseText));
        var input = redactedMessages + redactedPayload;
        var output = (redactedResponseText ?? string.Empty) + redactedResponseRaw;

        var entity = new AiRequestLogEntity
        {
            UserId = model.UserId,
            Module = model.Module,
            Purpose = model.Purpose,
            SourceObjectType = model.SourceObjectType,
            SourceObjectId = model.SourceObjectId,
            Provider = model.Provider,
            Model = model.Model,
            LiteLlmRequestId = model.LiteLlmRequestId,
            CorrelationId = model.CorrelationId,
            Status = ToStorageStatus(model.Status),
            AttemptNumber = model.AttemptNumber,
            MaxAttempts = model.MaxAttempts,
            StartedAt = model.StartedAt,
            FinishedAt = model.FinishedAt,
            DurationMs = (long)(model.FinishedAt - model.StartedAt).TotalMilliseconds,
            RequestMessagesJson = redactedMessages,
            RequestPayloadJson = redactedPayload,
            ResponseRawJson = redactedResponseRaw,
            ResponseText = redactedResponseText is null ? null : System.Text.Json.JsonSerializer.Deserialize<string>(redactedResponseText),
            ParsedOutputJson = model.ParsedOutputJson,
            SchemaName = model.SchemaName,
            SchemaVersion = model.SchemaVersion,
            SchemaJsonSnapshot = model.SchemaJsonSnapshot,
            SchemaValidationErrorsJson = model.SchemaValidationErrorsJson,
            PromptTokens = model.PromptTokens,
            CompletionTokens = model.CompletionTokens,
            TotalTokens = model.TotalTokens,
            EstimatedCost = model.EstimatedCost,
            Currency = model.Currency,
            InputChars = input.Length,
            OutputChars = output.Length,
            InputHash = Sha256(input),
            OutputHash = Sha256(output),
            ErrorCode = model.ErrorCode,
            ErrorMessage = model.ErrorMessage,
            MetadataJson = redactedMetadata
        };

        db.AiRequestLogs.Add(entity);
        await db.SaveChangesAsync(ct);
        return entity.Id;
    }

    private static string ToStorageStatus(AiRequestStatus status) => status switch
    {
        AiRequestStatus.Succeeded => "succeeded",
        AiRequestStatus.Failed => "failed",
        AiRequestStatus.Blocked => "blocked",
        AiRequestStatus.TimedOut => "timed_out",
        AiRequestStatus.FailedValidation => "failed_validation",
        _ => "failed"
    };

    private static string Sha256(string value)
    {
        var bytes = SHA256.HashData(Encoding.UTF8.GetBytes(value));
        return Convert.ToHexString(bytes).ToLowerInvariant();
    }

    private static string ToJsonString(string value) => System.Text.Json.JsonSerializer.Serialize(value);
}
```

- [ ] **Step 6: Run tests and commit**

Run:

```powershell
dotnet test tests\Pim.UnitTests\Pim.UnitTests.csproj --filter "AiRedactorTests|AiRequestLogWriterTests"
```

Expected: PASS.

Commit:

```powershell
git add src\Pim.Infrastructure\Ai tests\Pim.UnitTests\Ai\AiRedactorTests.cs tests\Pim.UnitTests\Ai\AiRequestLogWriterTests.cs
git commit -m "feat: redact and log ai attempts"
```

---

### Task 4: Schema Registry And Structured Output Validation

**Files:**
- Modify: `src/Pim.Infrastructure/Pim.Infrastructure.csproj`
- Create: `src/Pim.Infrastructure/Ai/AiSchemaRegistry.cs`
- Create: `src/Pim.Infrastructure/Ai/AiSchemaValidator.cs`
- Create: `tests/Pim.UnitTests/Ai/AiSchemaRegistryTests.cs`
- Create: `tests/Pim.UnitTests/Ai/AiSchemaValidatorTests.cs`

- [ ] **Step 1: Add package references**

Run:

```powershell
dotnet add src\Pim.Infrastructure\Pim.Infrastructure.csproj package JsonSchema.Net --version 9.2.1
```

Expected: `src/Pim.Infrastructure/Pim.Infrastructure.csproj` contains:

```xml
<PackageReference Include="JsonSchema.Net" Version="9.2.1" />
```

- [ ] **Step 2: Write failing schema tests**

Create `tests/Pim.UnitTests/Ai/AiSchemaRegistryTests.cs`:

```csharp
using Pim.Core.Ai;
using Pim.Infrastructure.Ai;
using Xunit;

namespace Pim.UnitTests.Ai;

public class AiSchemaRegistryTests
{
    [Fact]
    public void Get_ReturnsRegisteredSchemaByNameAndVersion()
    {
        var registry = new AiSchemaRegistry();
        registry.Register(new AiSchemaDefinition(
            "quick-note-conversion",
            "1",
            """{"type":"object","required":["title"],"properties":{"title":{"type":"string"}}}""",
            "Quick note conversion"));

        var schema = registry.Get("quick-note-conversion", "1");

        Assert.NotNull(schema);
        Assert.Equal("quick-note-conversion", schema.Name);
        Assert.Contains("\"title\"", schema.JsonSchema);
    }
}
```

Create `tests/Pim.UnitTests/Ai/AiSchemaValidatorTests.cs`:

```csharp
using Pim.Infrastructure.Ai;
using Xunit;

namespace Pim.UnitTests.Ai;

public class AiSchemaValidatorTests
{
    [Fact]
    public void Validate_ReturnsParsedJson_WhenOutputMatchesSchema()
    {
        var schema = """{"type":"object","required":["title"],"properties":{"title":{"type":"string"}}}""";
        var result = AiSchemaValidator.Validate("""{"title":"Inbox"}""", schema);

        Assert.True(result.IsValid);
        Assert.Equal("""{"title":"Inbox"}""", result.ParsedOutputJson);
        Assert.Empty(result.Errors);
    }

    [Fact]
    public void Validate_ReturnsErrors_WhenOutputDoesNotMatchSchema()
    {
        var schema = """{"type":"object","required":["title"],"properties":{"title":{"type":"string"}}}""";
        var result = AiSchemaValidator.Validate("""{"name":"Inbox"}""", schema);

        Assert.False(result.IsValid);
        Assert.Contains(result.Errors, error => error.Contains("title", StringComparison.OrdinalIgnoreCase));
    }
}
```

- [ ] **Step 3: Run schema tests and verify they fail**

Run:

```powershell
dotnet test tests\Pim.UnitTests\Pim.UnitTests.csproj --filter "AiSchemaRegistryTests|AiSchemaValidatorTests"
```

Expected: FAIL because registry and validator do not exist.

- [ ] **Step 4: Implement registry**

Create `src/Pim.Infrastructure/Ai/AiSchemaRegistry.cs`:

```csharp
using Pim.Core.Ai;

namespace Pim.Infrastructure.Ai;

public sealed class AiSchemaRegistry : IAiSchemaRegistry
{
    private readonly Dictionary<(string Name, string Version), AiSchemaDefinition> _schemas = new();

    public void Register(AiSchemaDefinition schema)
    {
        _schemas[(schema.Name, schema.Version)] = schema;
    }

    public AiSchemaDefinition? Get(string name, string version)
    {
        return _schemas.TryGetValue((name, version), out var schema) ? schema : null;
    }
}
```

- [ ] **Step 5: Implement validator**

Create `src/Pim.Infrastructure/Ai/AiSchemaValidator.cs`:

```csharp
using System.Text.Json;
using Json.Schema;

namespace Pim.Infrastructure.Ai;

public sealed record AiSchemaValidationResult(bool IsValid, string? ParsedOutputJson, IReadOnlyList<string> Errors);

public static class AiSchemaValidator
{
    public static AiSchemaValidationResult Validate(string responseText, string schemaJson)
    {
        try
        {
            using var responseDocument = JsonDocument.Parse(responseText);
            var schema = JsonSchema.FromText(schemaJson);
            var results = schema.Evaluate(responseDocument.RootElement, new EvaluationOptions
            {
                OutputFormat = OutputFormat.List
            });

            if (results.IsValid)
            {
                return new AiSchemaValidationResult(true, responseDocument.RootElement.GetRawText(), Array.Empty<string>());
            }

            var errors = results.Details
                .Where(detail => detail.HasErrors)
                .SelectMany(detail => detail.Errors?.Select(error => $"{detail.InstanceLocation}: {error.Key} {error.Value}") ?? [])
                .Where(error => !string.IsNullOrWhiteSpace(error))
                .Distinct(StringComparer.Ordinal)
                .ToArray();

            return new AiSchemaValidationResult(false, null, errors.Length == 0 ? ["JSON did not match schema."] : errors);
        }
        catch (JsonException ex)
        {
            return new AiSchemaValidationResult(false, null, [$"Invalid JSON: {ex.Message}"]);
        }
        catch (SchemaLoadException ex)
        {
            return new AiSchemaValidationResult(false, null, [$"Invalid schema: {ex.Message}"]);
        }
    }
}
```

- [ ] **Step 6: Run schema tests and commit**

Run:

```powershell
dotnet test tests\Pim.UnitTests\Pim.UnitTests.csproj --filter "AiSchemaRegistryTests|AiSchemaValidatorTests"
```

Expected: PASS.

Commit:

```powershell
git add src\Pim.Infrastructure\Pim.Infrastructure.csproj src\Pim.Infrastructure\Ai tests\Pim.UnitTests\Ai\AiSchemaRegistryTests.cs tests\Pim.UnitTests\Ai\AiSchemaValidatorTests.cs
git commit -m "feat: validate structured ai output"
```

---

### Task 5: LiteLLM Chat Client Factory And Gateway Execution

**Files:**
- Modify: `src/Pim.Infrastructure/Pim.Infrastructure.csproj`
- Create: `src/Pim.Infrastructure/Ai/AiOptions.cs`
- Create: `src/Pim.Infrastructure/Ai/AiChatClientFactory.cs`
- Create: `src/Pim.Infrastructure/Ai/AiGateway.cs`
- Modify: `src/Pim.Infrastructure/Extensions/ServiceCollectionExtensions.cs`
- Create: `tests/Pim.UnitTests/Ai/AiGatewayTests.cs`

- [ ] **Step 1: Add package reference**

Run:

```powershell
dotnet add src\Pim.Infrastructure\Pim.Infrastructure.csproj package Microsoft.Extensions.AI.OpenAI --version 10.6.0
```

Expected: `src/Pim.Infrastructure/Pim.Infrastructure.csproj` contains:

```xml
<PackageReference Include="Microsoft.Extensions.AI.OpenAI" Version="10.6.0" />
```

- [ ] **Step 2: Write failing gateway tests**

Create `tests/Pim.UnitTests/Ai/AiGatewayTests.cs`:

```csharp
using Microsoft.EntityFrameworkCore;
using Microsoft.Extensions.AI;
using Microsoft.Extensions.Options;
using Pim.Core.Ai;
using Pim.Infrastructure.Ai;
using Pim.Infrastructure.Data;
using Xunit;

namespace Pim.UnitTests.Ai;

public class AiGatewayTests
{
    [Fact]
    public async Task CompleteAsync_ReturnsBlockedAndDoesNotCallProvider_WhenAiDisabled()
    {
        await using var db = CreateDb();
        var fakeClient = new FakeChatClient("""{"title":"Inbox"}""");
        var gateway = CreateGateway(db, fakeClient, enabled: false);

        var result = await gateway.CompleteAsync(BasicRequest());

        Assert.Equal(AiRequestStatus.Blocked, result.Status);
        Assert.Equal(0, fakeClient.CallCount);
        Assert.Contains("AI is disabled", result.UserFacingError);
        Assert.Equal("blocked", (await db.AiRequestLogs.SingleAsync()).Status);
    }

    [Fact]
    public async Task CompleteAsync_LogsSuccessWithTokenUsage()
    {
        await using var db = CreateDb();
        var fakeClient = new FakeChatClient("plain answer", promptTokens: 4, completionTokens: 6);
        var gateway = CreateGateway(db, fakeClient, enabled: true);

        var result = await gateway.CompleteAsync(BasicRequest(schemaName: null, schemaVersion: null));

        Assert.Equal(AiRequestStatus.Succeeded, result.Status);
        Assert.Equal("plain answer", result.ResponseText);
        Assert.Equal(10, result.Usage.TotalTokens);
        Assert.Equal("succeeded", (await db.AiRequestLogs.SingleAsync()).Status);
    }

    [Fact]
    public async Task CompleteAsync_RetriesValidationOnceWithoutExpandingOriginalContext()
    {
        await using var db = CreateDb();
        var fakeClient = new FakeChatClient(["""{"name":"Inbox"}""", """{"title":"Inbox"}"""]);
        var registry = new AiSchemaRegistry();
        registry.Register(new AiSchemaDefinition(
            "quick-note-conversion",
            "1",
            """{"type":"object","required":["title"],"properties":{"title":{"type":"string"}}}""",
            "Quick note conversion"));
        var gateway = CreateGateway(db, fakeClient, enabled: true, registry: registry);

        var result = await gateway.CompleteAsync(BasicRequest(maxAttempts: 2));

        Assert.Equal(AiRequestStatus.Succeeded, result.Status);
        Assert.Equal("""{"title":"Inbox"}""", result.ParsedOutputJson);
        Assert.Equal(2, fakeClient.CallCount);
        Assert.Contains(fakeClient.Requests[1], message => message.Text?.Contains("Fix only the JSON", StringComparison.OrdinalIgnoreCase) == true);
        Assert.DoesNotContain(fakeClient.Requests[1], message => message.Text == "convert this note");
        Assert.Equal(2, await db.AiRequestLogs.CountAsync());
    }

    private static AiGatewayRequest BasicRequest(string? schemaName = "quick-note-conversion", string? schemaVersion = "1", int maxAttempts = 1)
        => new(
            Module: "quick-notes",
            Purpose: "quick-notes.convert",
            SourceObjectType: "quick_note",
            SourceObjectId: "note-1",
            Messages: [new AiMessage(AiMessageRole.User, "convert this note")],
            Model: null,
            SchemaName: schemaName,
            SchemaVersion: schemaVersion,
            MaxOutputTokens: 500,
            MaxAttempts: maxAttempts,
            Metadata: null);

    private static PimDbContext CreateDb()
    {
        var options = new DbContextOptionsBuilder<PimDbContext>()
            .UseInMemoryDatabase(Guid.NewGuid().ToString())
            .Options;
        return new PimDbContext(options);
    }

    private static AiGateway CreateGateway(PimDbContext db, FakeChatClient fakeClient, bool enabled, IAiSchemaRegistry? registry = null)
    {
        var options = Options.Create(new AiOptions
        {
            Enabled = enabled,
            Provider = "litellm",
            BaseUrl = "http://litellm:4000",
            ApiKey = "sk-pim",
            DefaultModel = "pim-default",
            TimeoutSeconds = 30,
            MaxOutputTokensPerRequest = 1000,
            MaxAttemptsPerRequest = 2,
            SaveFullPrompts = true,
            SaveFullResponses = true
        });

        return new AiGateway(
            options,
            new FixedAiChatClientFactory(fakeClient),
            registry ?? new AiSchemaRegistry(),
            new AiRequestLogWriter(db));
    }

    private sealed class FixedAiChatClientFactory(IChatClient client) : IAiChatClientFactory
    {
        public IChatClient Create(string model) => client;
    }
}
```

Add this fake client to the bottom of the test file:

```csharp
internal sealed class FakeChatClient : IChatClient
{
    private readonly Queue<string> _responses;
    private readonly int? _promptTokens;
    private readonly int? _completionTokens;

    public FakeChatClient(string response, int? promptTokens = null, int? completionTokens = null)
        : this([response], promptTokens, completionTokens) { }

    public FakeChatClient(IEnumerable<string> responses, int? promptTokens = null, int? completionTokens = null)
    {
        _responses = new Queue<string>(responses);
        _promptTokens = promptTokens;
        _completionTokens = completionTokens;
    }

    public int CallCount { get; private set; }
    public List<IList<ChatMessage>> Requests { get; } = [];

    public Task<ChatResponse> GetResponseAsync(
        IEnumerable<ChatMessage> messages,
        ChatOptions? options = null,
        CancellationToken cancellationToken = default)
    {
        CallCount++;
        Requests.Add(messages.ToList());
        var response = _responses.Count > 0 ? _responses.Dequeue() : string.Empty;
        var chatResponse = new ChatResponse(new ChatMessage(ChatRole.Assistant, response))
        {
            Usage = new UsageDetails
            {
                InputTokenCount = _promptTokens,
                OutputTokenCount = _completionTokens,
                TotalTokenCount = _promptTokens + _completionTokens
            }
        };
        return Task.FromResult(chatResponse);
    }

    public IAsyncEnumerable<ChatResponseUpdate> GetStreamingResponseAsync(
        IEnumerable<ChatMessage> messages,
        ChatOptions? options = null,
        CancellationToken cancellationToken = default)
        => AsyncEnumerable.Empty<ChatResponseUpdate>();

    public object? GetService(Type serviceType, object? serviceKey = null) => null;
    public void Dispose() { }
}
```

- [ ] **Step 3: Run gateway tests and verify they fail**

Run:

```powershell
dotnet test tests\Pim.UnitTests\Pim.UnitTests.csproj --filter AiGatewayTests
```

Expected: FAIL because options, factory, gateway, and registration do not exist.

- [ ] **Step 4: Implement options and chat factory**

Create `src/Pim.Infrastructure/Ai/AiOptions.cs`:

```csharp
namespace Pim.Infrastructure.Ai;

public sealed class AiOptions
{
    public bool Enabled { get; set; }
    public string Provider { get; set; } = "litellm";
    public string BaseUrl { get; set; } = "http://litellm:4000";
    public string ApiKey { get; set; } = string.Empty;
    public string DefaultModel { get; set; } = "pim-default";
    public int TimeoutSeconds { get; set; } = 30;
    public int MaxOutputTokensPerRequest { get; set; } = 1000;
    public int MaxAttemptsPerRequest { get; set; } = 2;
    public bool SaveFullPrompts { get; set; } = true;
    public bool SaveFullResponses { get; set; } = true;
}
```

Create `src/Pim.Infrastructure/Ai/AiChatClientFactory.cs`:

```csharp
using Microsoft.Extensions.AI;
using Microsoft.Extensions.Options;
using OpenAI;
using OpenAI.Chat;
using System.ClientModel;

namespace Pim.Infrastructure.Ai;

public interface IAiChatClientFactory
{
    IChatClient Create(string model);
}

public sealed class AiChatClientFactory(IOptions<AiOptions> options) : IAiChatClientFactory
{
    public IChatClient Create(string model)
    {
        var ai = options.Value;
        var chatClient = new ChatClient(
            model: model,
            credential: new ApiKeyCredential(ai.ApiKey),
            options: new OpenAIClientOptions
            {
                Endpoint = new Uri(ai.BaseUrl.TrimEnd('/') + "/v1")
            });

        return chatClient.AsIChatClient();
    }
}
```

- [ ] **Step 5: Implement gateway**

Create `src/Pim.Infrastructure/Ai/AiGateway.cs` with these required behaviors:

```csharp
using System.Text.Json;
using Microsoft.Extensions.AI;
using Microsoft.Extensions.Options;
using Pim.Core.Ai;

namespace Pim.Infrastructure.Ai;

public sealed class AiGateway(
    IOptions<AiOptions> options,
    IAiChatClientFactory chatClientFactory,
    IAiSchemaRegistry schemaRegistry,
    IAiRequestLogWriter logWriter) : IAiGateway
{
    public async Task<AiResult> CompleteAsync(AiGatewayRequest request, CancellationToken ct = default)
    {
        var ai = options.Value;
        var model = request.Model ?? ai.DefaultModel;
        var maxAttempts = Math.Min(request.EffectiveMaxAttempts, ai.MaxAttemptsPerRequest);
        var correlationId = Guid.NewGuid().ToString("N");

        if (!ai.Enabled)
        {
            var logId = await WriteLogAsync(request, model, correlationId, AiRequestStatus.Blocked, 1, maxAttempts, "[]", "{}", "{}", null, null, null, "disabled", "AI is disabled.", ct);
            return new AiResult(AiRequestStatus.Blocked, null, null, [], new AiTokenUsage(null, null, null, null, null), logId, "AI is disabled.");
        }

        var schema = ResolveSchema(request);
        IReadOnlyList<ChatMessage> currentMessages = ToChatMessages(request.Messages, schema is not null);
        Guid? lastLogId = null;
        IReadOnlyList<string> lastValidationErrors = [];

        for (var attempt = 1; attempt <= maxAttempts; attempt++)
        {
            var started = DateTimeOffset.UtcNow;
            try
            {
                var client = chatClientFactory.Create(model);
                var response = await client.GetResponseAsync(currentMessages, new ChatOptions
                {
                    MaxOutputTokens = request.MaxOutputTokens ?? ai.MaxOutputTokensPerRequest
                }, ct);
                var finished = DateTimeOffset.UtcNow;
                var text = response.Text ?? string.Empty;
                var usage = ExtractUsage(response);
                var rawJson = JsonSerializer.Serialize(response);
                var payloadJson = JsonSerializer.Serialize(new { model, maxOutputTokens = request.MaxOutputTokens ?? ai.MaxOutputTokensPerRequest, attempt });

                if (schema is not null)
                {
                    var validation = AiSchemaValidator.Validate(text, schema.JsonSchema);
                    if (!validation.IsValid)
                    {
                        lastValidationErrors = validation.Errors;
                        lastLogId = await logWriter.WriteAsync(CreateLogModel(request, model, correlationId, AiRequestStatus.FailedValidation, attempt, maxAttempts, started, finished, payloadJson, rawJson, text, null, schema, validation.Errors, usage, "schema_validation_failed", "AI response failed schema validation."), ct);

                        if (attempt < maxAttempts)
                        {
                            currentMessages = CreateRepairMessages(text, validation.Errors, schema.JsonSchema);
                            continue;
                        }

                        return AiResult.FailedValidation(lastLogId, validation.Errors);
                    }

                    lastLogId = await logWriter.WriteAsync(CreateLogModel(request, model, correlationId, AiRequestStatus.Succeeded, attempt, maxAttempts, started, finished, payloadJson, rawJson, text, validation.ParsedOutputJson, schema, [], usage, null, null), ct);
                    return new AiResult(AiRequestStatus.Succeeded, text, validation.ParsedOutputJson, [], usage, lastLogId, null);
                }

                lastLogId = await logWriter.WriteAsync(CreateLogModel(request, model, correlationId, AiRequestStatus.Succeeded, attempt, maxAttempts, started, finished, payloadJson, rawJson, text, null, null, [], usage, null, null), ct);
                return new AiResult(AiRequestStatus.Succeeded, text, null, [], usage, lastLogId, null);
            }
            catch (OperationCanceledException) when (ct.IsCancellationRequested)
            {
                throw;
            }
            catch (TimeoutException ex)
            {
                lastLogId = await WriteLogAsync(request, model, correlationId, AiRequestStatus.TimedOut, attempt, maxAttempts, "[]", "{}", "{}", null, null, null, "timed_out", ex.Message, ct);
                return new AiResult(AiRequestStatus.TimedOut, null, null, [], new AiTokenUsage(null, null, null, null, null), lastLogId, "AI request timed out.");
            }
            catch (Exception ex)
            {
                lastLogId = await WriteLogAsync(request, model, correlationId, AiRequestStatus.Failed, attempt, maxAttempts, "[]", "{}", "{}", null, null, null, "provider_unavailable", ex.Message, ct);
                return new AiResult(AiRequestStatus.Failed, null, null, [], new AiTokenUsage(null, null, null, null, null), lastLogId, "AI provider is unavailable.");
            }
        }

        return AiResult.FailedValidation(lastLogId, lastValidationErrors);
    }

    private AiSchemaDefinition? ResolveSchema(AiGatewayRequest request)
    {
        if (request.SchemaName is null || request.SchemaVersion is null)
        {
            return null;
        }

        return schemaRegistry.Get(request.SchemaName, request.SchemaVersion)
            ?? throw new InvalidOperationException($"AI schema '{request.SchemaName}' version '{request.SchemaVersion}' is not registered.");
    }

    private static IReadOnlyList<ChatMessage> ToChatMessages(IReadOnlyList<AiMessage> messages, bool structured)
    {
        var converted = messages.Select(message => new ChatMessage(ToChatRole(message.Role), message.Content)).ToList();
        if (structured)
        {
            converted.Insert(0, new ChatMessage(ChatRole.System, "Return only JSON. Do not wrap JSON in Markdown."));
        }
        return converted;
    }

    private static ChatRole ToChatRole(AiMessageRole role) => role switch
    {
        AiMessageRole.System => ChatRole.System,
        AiMessageRole.Assistant => ChatRole.Assistant,
        _ => ChatRole.User
    };

    private static IReadOnlyList<ChatMessage> CreateRepairMessages(string failedJson, IReadOnlyList<string> errors, string schemaJson)
        =>
        [
            new ChatMessage(ChatRole.System, "Fix only the JSON so it validates against the schema. Return only corrected JSON."),
            new ChatMessage(ChatRole.User, JsonSerializer.Serialize(new { failedJson, errors, schema = schemaJson }))
        ];

    private static AiTokenUsage ExtractUsage(ChatResponse response)
    {
        return new AiTokenUsage(
            response.Usage?.InputTokenCount,
            response.Usage?.OutputTokenCount,
            response.Usage?.TotalTokenCount,
            null,
            null);
    }

    private AiRequestLogWriteModel CreateLogModel(
        AiGatewayRequest request,
        string model,
        string correlationId,
        AiRequestStatus status,
        int attempt,
        int maxAttempts,
        DateTimeOffset started,
        DateTimeOffset finished,
        string payloadJson,
        string rawJson,
        string? responseText,
        string? parsedJson,
        AiSchemaDefinition? schema,
        IReadOnlyList<string> validationErrors,
        AiTokenUsage usage,
        string? errorCode,
        string? errorMessage)
    {
        return new AiRequestLogWriteModel(
            UserId: null,
            request.Module,
            request.Purpose,
            request.SourceObjectType,
            request.SourceObjectId,
            options.Value.Provider,
            model,
            LiteLlmRequestId: null,
            correlationId,
            status,
            attempt,
            maxAttempts,
            started,
            finished,
            JsonSerializer.Serialize(request.Messages),
            payloadJson,
            rawJson,
            responseText,
            parsedJson,
            schema?.Name,
            schema?.Version,
            schema?.JsonSchema,
            JsonSerializer.Serialize(validationErrors),
            usage.PromptTokens,
            usage.CompletionTokens,
            usage.TotalTokens,
            usage.EstimatedCost,
            usage.Currency,
            errorCode,
            errorMessage,
            JsonSerializer.Serialize(request.Metadata ?? new Dictionary<string, string>()));
    }

    private async Task<Guid> WriteLogAsync(AiGatewayRequest request, string model, string correlationId, AiRequestStatus status, int attempt, int maxAttempts, string messagesJson, string payloadJson, string rawJson, string? responseText, string? parsedJson, AiSchemaDefinition? schema, string? errorCode, string? errorMessage, CancellationToken ct)
    {
        var now = DateTimeOffset.UtcNow;
        return await logWriter.WriteAsync(new AiRequestLogWriteModel(
            null,
            request.Module,
            request.Purpose,
            request.SourceObjectType,
            request.SourceObjectId,
            options.Value.Provider,
            model,
            null,
            correlationId,
            status,
            attempt,
            maxAttempts,
            now,
            now,
            messagesJson,
            payloadJson,
            rawJson,
            responseText,
            parsedJson,
            schema?.Name,
            schema?.Version,
            schema?.JsonSchema,
            "[]",
            null,
            null,
            null,
            null,
            null,
            errorCode,
            errorMessage,
            JsonSerializer.Serialize(request.Metadata ?? new Dictionary<string, string>())), ct);
    }
}
```

- [ ] **Step 6: Register services**

Modify `src/Pim.Infrastructure/Extensions/ServiceCollectionExtensions.cs`:

```csharp
using Pim.Core.Ai;
using Pim.Infrastructure.Ai;
```

Add inside `AddPimInfrastructure` after EF registration:

```csharp
services.Configure<AiOptions>(configuration.GetSection("Ai"));
services.AddScoped<IAiGateway, AiGateway>();
services.AddScoped<IAiRequestLogWriter, AiRequestLogWriter>();
services.AddSingleton<IAiSchemaRegistry, AiSchemaRegistry>();
services.AddSingleton<IAiChatClientFactory, AiChatClientFactory>();
```

- [ ] **Step 7: Run gateway tests and commit**

Run:

```powershell
dotnet test tests\Pim.UnitTests\Pim.UnitTests.csproj --filter AiGatewayTests
```

Expected: PASS.

Commit:

```powershell
git add src\Pim.Infrastructure\Pim.Infrastructure.csproj src\Pim.Infrastructure\Ai src\Pim.Infrastructure\Extensions\ServiceCollectionExtensions.cs tests\Pim.UnitTests\Ai\AiGatewayTests.cs
git commit -m "feat: call litellm through ai gateway"
```

---

### Task 6: AI Usage Service And Provider Health

**Files:**
- Create: `src/Pim.Infrastructure/Ai/AiUsageService.cs`
- Create: `src/Pim.Infrastructure/Ai/AiProviderHealthService.cs`
- Modify: `src/Pim.Infrastructure/Extensions/ServiceCollectionExtensions.cs`
- Create: `tests/Pim.UnitTests/Ai/AiUsageServiceTests.cs`

- [ ] **Step 1: Write failing usage tests**

Create `tests/Pim.UnitTests/Ai/AiUsageServiceTests.cs`:

```csharp
using Microsoft.EntityFrameworkCore;
using Microsoft.Extensions.Options;
using Pim.Core.Ai;
using Pim.Infrastructure.Ai;
using Pim.Infrastructure.Data;
using Pim.Infrastructure.Data.Entities;
using Xunit;

namespace Pim.UnitTests.Ai;

public class AiUsageServiceTests
{
    [Fact]
    public async Task ListRequestsAsync_FiltersByModuleAndStatus()
    {
        await using var db = CreateDb();
        db.AiRequestLogs.Add(MakeLog("quick-notes", "succeeded", 10));
        db.AiRequestLogs.Add(MakeLog("files", "failed", 3));
        await db.SaveChangesAsync();
        var service = CreateService(db);

        var result = await service.ListRequestsAsync(new AiRequestLogFilter(
            From: null, To: null, Module: "quick-notes", Purpose: null, SourceObjectType: null,
            SourceObjectId: null, Model: null, Status: AiRequestStatus.Succeeded, UserId: null));

        Assert.Equal(1, result.TotalCount);
        Assert.Equal("quick-notes", result.Items[0].Module);
        Assert.Equal(AiRequestStatus.Succeeded, result.Items[0].Status);
    }

    [Fact]
    public async Task GetUsageSummaryAsync_GroupsByModulePurposeModelAndStatus()
    {
        await using var db = CreateDb();
        db.AiRequestLogs.Add(MakeLog("quick-notes", "succeeded", 10));
        db.AiRequestLogs.Add(MakeLog("quick-notes", "failed", 5));
        await db.SaveChangesAsync();
        var service = CreateService(db);

        var summary = await service.GetUsageSummaryAsync(null, null);

        Assert.Equal(2, summary.RequestCount);
        Assert.Equal(1, summary.SuccessCount);
        Assert.Equal(1, summary.FailureCount);
        Assert.Equal(15, summary.TotalTokens);
        Assert.Contains(summary.ByModule, group => group.GroupKey == "quick-notes" && group.RequestCount == 2);
        Assert.Contains(summary.ByStatus, group => group.GroupKey == "failed" && group.FailureCount == 1);
    }

    private static PimDbContext CreateDb()
    {
        var options = new DbContextOptionsBuilder<PimDbContext>()
            .UseInMemoryDatabase(Guid.NewGuid().ToString())
            .Options;
        return new PimDbContext(options);
    }

    private static AiUsageService CreateService(PimDbContext db)
        => new(db, Options.Create(new AiOptions
        {
            Enabled = true,
            Provider = "litellm",
            BaseUrl = "http://litellm:4000",
            DefaultModel = "pim-default"
        }));

    private static AiRequestLogEntity MakeLog(string module, string status, int totalTokens) => new()
    {
        Module = module,
        Purpose = $"{module}.test",
        SourceObjectType = "test",
        SourceObjectId = Guid.NewGuid().ToString("N"),
        Provider = "litellm",
        Model = "pim-default",
        CorrelationId = Guid.NewGuid().ToString("N"),
        Status = status,
        AttemptNumber = 1,
        MaxAttempts = 1,
        StartedAt = DateTimeOffset.UtcNow,
        FinishedAt = DateTimeOffset.UtcNow,
        DurationMs = 20,
        RequestMessagesJson = "[]",
        RequestPayloadJson = "{}",
        ResponseRawJson = "{}",
        SchemaValidationErrorsJson = "[]",
        PromptTokens = totalTokens / 2,
        CompletionTokens = totalTokens - (totalTokens / 2),
        TotalTokens = totalTokens,
        EstimatedCost = 0.001m,
        Currency = "USD",
        InputHash = "input",
        OutputHash = "output",
        MetadataJson = "{}"
    };
}
```

- [ ] **Step 2: Run usage tests and verify they fail**

Run:

```powershell
dotnet test tests\Pim.UnitTests\Pim.UnitTests.csproj --filter AiUsageServiceTests
```

Expected: FAIL because `AiUsageService` does not exist.

- [ ] **Step 3: Implement usage service**

Create `src/Pim.Infrastructure/Ai/AiUsageService.cs`:

```csharp
using Microsoft.EntityFrameworkCore;
using Microsoft.Extensions.Options;
using Pim.Core.Ai;
using Pim.Core.Common;
using Pim.Infrastructure.Data;
using Pim.Infrastructure.Data.Entities;

namespace Pim.Infrastructure.Ai;

public sealed class AiUsageService(PimDbContext db, IOptions<AiOptions> options) : IAiUsageService
{
    public async Task<AiStatusDto> GetStatusAsync(CancellationToken ct = default)
    {
        var ai = options.Value;
        var recentSuccess = await db.AiRequestLogs
            .Where(l => l.Status == "succeeded")
            .OrderByDescending(l => l.StartedAt)
            .Select(l => (DateTimeOffset?)l.StartedAt)
            .FirstOrDefaultAsync(ct);

        var settings = await db.AiProviderSettings.SingleOrDefaultAsync(s => s.Provider == "litellm", ct);

        return new AiStatusDto(
            ai.Enabled,
            ai.Provider,
            ai.BaseUrl,
            ai.DefaultModel,
            settings?.LastHealthCheckAt,
            settings?.LastError,
            recentSuccess);
    }

    public async Task<PagedResult<AiRequestLogListItemDto>> ListRequestsAsync(AiRequestLogFilter filter, CancellationToken ct = default)
    {
        var query = ApplyFilter(db.AiRequestLogs.AsNoTracking(), filter);
        var total = await query.CountAsync(ct);
        var page = Math.Max(1, filter.Page);
        var pageSize = Math.Clamp(filter.PageSize, 1, 200);
        var items = await query
            .OrderByDescending(l => l.StartedAt)
            .Skip((page - 1) * pageSize)
            .Take(pageSize)
            .Select(l => new AiRequestLogListItemDto(
                l.Id, l.StartedAt, l.Module, l.Purpose, l.Model, FromStorageStatus(l.Status),
                l.TotalTokens, l.EstimatedCost, l.DurationMs, l.SourceObjectType, l.SourceObjectId, l.ErrorMessage))
            .ToListAsync(ct);

        return new PagedResult<AiRequestLogListItemDto>(items, page, pageSize, total, total == 0 ? 0 : (int)Math.Ceiling(total / (double)pageSize));
    }

    public async Task<AiRequestLogDetailDto?> GetRequestDetailAsync(Guid id, CancellationToken ct = default)
    {
        var l = await db.AiRequestLogs.AsNoTracking().SingleOrDefaultAsync(x => x.Id == id, ct);
        if (l is null)
        {
            return null;
        }

        return new AiRequestLogDetailDto(
            l.Id, l.UserId, l.Module, l.Purpose, l.SourceObjectType, l.SourceObjectId,
            l.Provider, l.Model, l.LiteLlmRequestId, l.CorrelationId, FromStorageStatus(l.Status),
            l.AttemptNumber, l.MaxAttempts, l.StartedAt, l.FinishedAt, l.DurationMs,
            l.RequestMessagesJson, l.RequestPayloadJson, l.ResponseRawJson, l.ResponseText,
            l.ParsedOutputJson, l.SchemaName, l.SchemaVersion, l.SchemaJsonSnapshot,
            l.SchemaValidationErrorsJson,
            new AiTokenUsage(l.PromptTokens, l.CompletionTokens, l.TotalTokens, l.EstimatedCost, l.Currency),
            l.ErrorCode, l.ErrorMessage, l.MetadataJson);
    }

    public async Task<AiUsageSummaryDto> GetUsageSummaryAsync(DateTimeOffset? from, DateTimeOffset? to, CancellationToken ct = default)
    {
        var filter = new AiRequestLogFilter(from, to, null, null, null, null, null, null, null);
        var logs = await ApplyFilter(db.AiRequestLogs.AsNoTracking(), filter).ToListAsync(ct);
        return new AiUsageSummaryDto(
            logs.Count,
            logs.Count(IsSuccess),
            logs.Count(l => !IsSuccess(l)),
            logs.Sum(l => l.PromptTokens ?? 0),
            logs.Sum(l => l.CompletionTokens ?? 0),
            logs.Sum(l => l.TotalTokens ?? 0),
            logs.Sum(l => l.EstimatedCost ?? 0),
            Group(logs, l => l.Module),
            Group(logs, l => l.Purpose),
            Group(logs, l => l.Model),
            Group(logs, l => l.Status));
    }

    private static IQueryable<AiRequestLogEntity> ApplyFilter(IQueryable<AiRequestLogEntity> query, AiRequestLogFilter filter)
    {
        if (filter.From is not null) query = query.Where(l => l.StartedAt >= filter.From);
        if (filter.To is not null) query = query.Where(l => l.StartedAt <= filter.To);
        if (!string.IsNullOrWhiteSpace(filter.Module)) query = query.Where(l => l.Module == filter.Module);
        if (!string.IsNullOrWhiteSpace(filter.Purpose)) query = query.Where(l => l.Purpose == filter.Purpose);
        if (!string.IsNullOrWhiteSpace(filter.SourceObjectType)) query = query.Where(l => l.SourceObjectType == filter.SourceObjectType);
        if (!string.IsNullOrWhiteSpace(filter.SourceObjectId)) query = query.Where(l => l.SourceObjectId == filter.SourceObjectId);
        if (!string.IsNullOrWhiteSpace(filter.Model)) query = query.Where(l => l.Model == filter.Model);
        if (filter.Status is not null) query = query.Where(l => l.Status == ToStorageStatus(filter.Status.Value));
        if (filter.UserId is not null) query = query.Where(l => l.UserId == filter.UserId);
        return query;
    }

    private static IReadOnlyList<AiUsageGroupDto> Group(IEnumerable<AiRequestLogEntity> logs, Func<AiRequestLogEntity, string> keySelector)
        => logs.GroupBy(keySelector)
            .OrderByDescending(g => g.Count())
            .Select(g => new AiUsageGroupDto(
                g.Key,
                g.Count(),
                g.Count(IsSuccess),
                g.Count(l => !IsSuccess(l)),
                g.Sum(l => l.PromptTokens ?? 0),
                g.Sum(l => l.CompletionTokens ?? 0),
                g.Sum(l => l.TotalTokens ?? 0),
                g.Sum(l => l.EstimatedCost ?? 0)))
            .ToList();

    private static bool IsSuccess(AiRequestLogEntity log) => log.Status == "succeeded";

    private static string ToStorageStatus(AiRequestStatus status) => status switch
    {
        AiRequestStatus.Succeeded => "succeeded",
        AiRequestStatus.Failed => "failed",
        AiRequestStatus.Blocked => "blocked",
        AiRequestStatus.TimedOut => "timed_out",
        AiRequestStatus.FailedValidation => "failed_validation",
        _ => "failed"
    };

    private static AiRequestStatus FromStorageStatus(string status) => status switch
    {
        "succeeded" => AiRequestStatus.Succeeded,
        "blocked" => AiRequestStatus.Blocked,
        "timed_out" => AiRequestStatus.TimedOut,
        "failed_validation" => AiRequestStatus.FailedValidation,
        _ => AiRequestStatus.Failed
    };
}
```

- [ ] **Step 4: Implement provider health service**

Create `src/Pim.Infrastructure/Ai/AiProviderHealthService.cs`:

```csharp
using Microsoft.EntityFrameworkCore;
using Microsoft.Extensions.Options;
using Pim.Infrastructure.Data;
using Pim.Infrastructure.Data.Entities;

namespace Pim.Infrastructure.Ai;

public interface IAiProviderHealthService
{
    Task CheckAsync(CancellationToken ct = default);
}

public sealed class AiProviderHealthService(PimDbContext db, IOptions<AiOptions> options, IHttpClientFactory httpClientFactory) : IAiProviderHealthService
{
    public async Task CheckAsync(CancellationToken ct = default)
    {
        var ai = options.Value;
        var settings = await db.AiProviderSettings.SingleOrDefaultAsync(s => s.Provider == "litellm", ct)
            ?? new AiProviderSettingEntity { Provider = "litellm" };

        settings.BaseUrl = ai.BaseUrl;
        settings.DefaultModel = ai.DefaultModel;
        settings.Status = ai.Enabled ? "enabled" : "disabled";
        settings.LastHealthCheckAt = DateTimeOffset.UtcNow;
        settings.UpdatedAt = DateTimeOffset.UtcNow;

        try
        {
            using var client = httpClientFactory.CreateClient("litellm-health");
            using var request = new HttpRequestMessage(HttpMethod.Get, ai.BaseUrl.TrimEnd('/') + "/v1/models");
            request.Headers.Authorization = new System.Net.Http.Headers.AuthenticationHeaderValue("Bearer", ai.ApiKey);
            using var response = await client.SendAsync(request, ct);
            response.EnsureSuccessStatusCode();
            settings.LastError = null;
        }
        catch (Exception ex)
        {
            settings.Status = "error";
            settings.LastError = ex.Message;
        }

        if (settings.Id == Guid.Empty || db.Entry(settings).State == EntityState.Detached)
        {
            db.AiProviderSettings.Add(settings);
        }

        await db.SaveChangesAsync(ct);
    }
}
```

- [ ] **Step 5: Register usage and health services**

Modify `src/Pim.Infrastructure/Extensions/ServiceCollectionExtensions.cs`:

```csharp
services.AddScoped<IAiUsageService, AiUsageService>();
services.AddScoped<IAiProviderHealthService, AiProviderHealthService>();
services.AddHttpClient("litellm-health");
```

- [ ] **Step 6: Run tests and commit**

Run:

```powershell
dotnet test tests\Pim.UnitTests\Pim.UnitTests.csproj --filter AiUsageServiceTests
```

Expected: PASS.

Commit:

```powershell
git add src\Pim.Infrastructure\Ai src\Pim.Infrastructure\Extensions\ServiceCollectionExtensions.cs tests\Pim.UnitTests\Ai\AiUsageServiceTests.cs
git commit -m "feat: expose ai usage queries"
```

---

### Task 7: Authenticated AI API Endpoints

**Files:**
- Create: `src/Pim.Api/Endpoints/AiEndpoints.cs`
- Modify: `src/Pim.Api/Program.cs`
- Create: `tests/Pim.UnitTests/Ai/AiEndpointPathTests.cs`

- [ ] **Step 1: Write failing endpoint path tests**

Create `tests/Pim.UnitTests/Ai/AiEndpointPathTests.cs`:

```csharp
using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.Builder;
using Microsoft.AspNetCore.Routing;
using Microsoft.Extensions.DependencyInjection;
using Pim.Api.Endpoints;
using Pim.Core.Ai;
using Pim.Core.Common;
using Pim.Infrastructure.Ai;
using Xunit;

namespace Pim.UnitTests.Ai;

public class AiEndpointPathTests
{
    [Fact]
    public async Task MapAiEndpoints_RegistersExpectedAuthorizedRoutes()
    {
        var builder = WebApplication.CreateBuilder();
        builder.Services.AddAuthorization();
        builder.Services.AddSingleton<IAiGateway, FakeAiGateway>();
        builder.Services.AddSingleton<IAiUsageService, FakeAiUsageService>();
        builder.Services.AddSingleton<IAiProviderHealthService, FakeAiProviderHealthService>();
        using var app = builder.Build();

        app.MapAiEndpoints();
        await app.StartAsync();

        var routes = app.Services.GetRequiredService<EndpointDataSource>()
            .Endpoints
            .OfType<RouteEndpoint>()
            .ToLookup(endpoint => NormalizeRoute(endpoint.RoutePattern.RawText ?? string.Empty));

        foreach (var expected in new[]
        {
            "/api/v1/ai/status",
            "/api/v1/ai/test",
            "/api/v1/ai/requests",
            "/api/v1/ai/requests/{id:guid}",
            "/api/v1/ai/usage/summary",
            "/api/v1/ai/health-check"
        })
        {
            var endpoints = routes[expected].ToList();
            Assert.True(endpoints.Count > 0, $"Missing route: {expected}");
            Assert.All(endpoints, endpoint => Assert.NotNull(endpoint.Metadata.GetMetadata<IAuthorizeData>()));
        }
    }

    private static string NormalizeRoute(string route) => route.Length > 1 ? route.TrimEnd('/') : route;

    private sealed class FakeAiGateway : IAiGateway
    {
        public Task<AiResult> CompleteAsync(AiGatewayRequest request, CancellationToken ct = default)
            => Task.FromResult(new AiResult(AiRequestStatus.Succeeded, "ok", null, [], new AiTokenUsage(1, 1, 2, null, null), Guid.NewGuid(), null));
    }

    private sealed class FakeAiUsageService : IAiUsageService
    {
        public Task<AiStatusDto> GetStatusAsync(CancellationToken ct = default)
            => Task.FromResult(new AiStatusDto(true, "litellm", "http://litellm:4000", "pim-default", null, null, null));
        public Task<PagedResult<AiRequestLogListItemDto>> ListRequestsAsync(AiRequestLogFilter filter, CancellationToken ct = default)
            => Task.FromResult(new PagedResult<AiRequestLogListItemDto>([], 1, 50, 0, 0));
        public Task<AiRequestLogDetailDto?> GetRequestDetailAsync(Guid id, CancellationToken ct = default)
            => Task.FromResult<AiRequestLogDetailDto?>(null);
        public Task<AiUsageSummaryDto> GetUsageSummaryAsync(DateTimeOffset? from, DateTimeOffset? to, CancellationToken ct = default)
            => Task.FromResult(new AiUsageSummaryDto(0, 0, 0, 0, 0, 0, 0, [], [], [], []));
    }

    private sealed class FakeAiProviderHealthService : IAiProviderHealthService
    {
        public Task CheckAsync(CancellationToken ct = default) => Task.CompletedTask;
    }
}
```

- [ ] **Step 2: Run endpoint tests and verify they fail**

Run:

```powershell
dotnet test tests\Pim.UnitTests\Pim.UnitTests.csproj --filter AiEndpointPathTests
```

Expected: FAIL because `MapAiEndpoints` does not exist.

- [ ] **Step 3: Implement endpoints**

Create `src/Pim.Api/Endpoints/AiEndpoints.cs`:

```csharp
using Pim.Core.Ai;
using Pim.Core.Common;
using Pim.Core.Exceptions;
using Pim.Infrastructure.Ai;

namespace Pim.Api.Endpoints;

public static class AiEndpoints
{
    public static void MapAiEndpoints(this IEndpointRouteBuilder endpoints)
    {
        var group = endpoints.MapGroup("/api/v1/ai").RequireAuthorization();

        group.MapGet("/status", async (IAiUsageService usage, CancellationToken ct) =>
            Results.Ok(ApiResponse<AiStatusDto>.Ok(await usage.GetStatusAsync(ct))));

        group.MapPost("/test", async (IAiGateway gateway, CancellationToken ct) =>
        {
            var result = await gateway.CompleteAsync(new AiGatewayRequest(
                Module: "system",
                Purpose: "ai.test",
                SourceObjectType: "system",
                SourceObjectId: "ai-test",
                Messages: [new AiMessage(AiMessageRole.User, "Reply with the word ok.")],
                Model: null,
                SchemaName: null,
                SchemaVersion: null,
                MaxOutputTokens: 32,
                MaxAttempts: 1,
                Metadata: new Dictionary<string, string> { ["endpoint"] = "/api/v1/ai/test" }), ct);
            return Results.Ok(ApiResponse<AiResult>.Ok(result));
        });

        group.MapGet("/requests", async (
            DateTimeOffset? from,
            DateTimeOffset? to,
            string? module,
            string? purpose,
            string? sourceObjectType,
            string? sourceObjectId,
            string? model,
            AiRequestStatus? status,
            Guid? userId,
            int? page,
            int? pageSize,
            IAiUsageService usage,
            CancellationToken ct) =>
        {
            var filter = new AiRequestLogFilter(from, to, module, purpose, sourceObjectType, sourceObjectId, model, status, userId, page ?? 1, pageSize ?? 50);
            return Results.Ok(ApiResponse<PagedResult<AiRequestLogListItemDto>>.Ok(await usage.ListRequestsAsync(filter, ct)));
        });

        group.MapGet("/requests/{id:guid}", async (Guid id, IAiUsageService usage, CancellationToken ct) =>
        {
            var detail = await usage.GetRequestDetailAsync(id, ct);
            return detail is null
                ? Results.NotFound(ApiResponse<string>.Error(404, "AI request log not found."))
                : Results.Ok(ApiResponse<AiRequestLogDetailDto>.Ok(detail));
        });

        group.MapGet("/usage/summary", async (DateTimeOffset? from, DateTimeOffset? to, IAiUsageService usage, CancellationToken ct) =>
            Results.Ok(ApiResponse<AiUsageSummaryDto>.Ok(await usage.GetUsageSummaryAsync(from, to, ct))));

        group.MapPost("/health-check", async (IAiProviderHealthService health, IAiUsageService usage, CancellationToken ct) =>
        {
            await health.CheckAsync(ct);
            return Results.Ok(ApiResponse<AiStatusDto>.Ok(await usage.GetStatusAsync(ct)));
        });
    }
}
```

- [ ] **Step 4: Map endpoints in Program**

Modify `src/Pim.Api/Program.cs` after `app.MapTodayEndpoints();`:

```csharp
app.MapAiEndpoints();
```

- [ ] **Step 5: Run endpoint tests and solution tests, then commit**

Run:

```powershell
dotnet test tests\Pim.UnitTests\Pim.UnitTests.csproj --filter AiEndpointPathTests
dotnet test Pim.sln
```

Expected: PASS.

Commit:

```powershell
git add src\Pim.Api\Endpoints\AiEndpoints.cs src\Pim.Api\Program.cs tests\Pim.UnitTests\Ai\AiEndpointPathTests.cs
git commit -m "feat: add ai gateway api endpoints"
```

---

### Task 8: Docker Compose And Configuration Defaults

**Files:**
- Modify: `src/Pim.Api/appsettings.json`
- Modify: `src/Pim.Api/appsettings.Development.json`
- Modify: `docker-compose.yml`
- Modify: `.env.example`
- Create: `litellm-config.yaml`
- Create: `tests/Pim.UnitTests/Ai/AiConfigurationTests.cs`

- [ ] **Step 1: Write failing configuration tests**

Create `tests/Pim.UnitTests/Ai/AiConfigurationTests.cs`:

```csharp
using System.Text.Json;
using Xunit;

namespace Pim.UnitTests.Ai;

public class AiConfigurationTests
{
    [Fact]
    public void Appsettings_DefinesLiteLlmDefaults()
    {
        using var document = JsonDocument.Parse(File.ReadAllText(Path.Combine("..", "..", "..", "..", "src", "Pim.Api", "appsettings.json")));
        var ai = document.RootElement.GetProperty("Ai");

        Assert.Equal("litellm", ai.GetProperty("Provider").GetString());
        Assert.Equal("http://litellm:4000", ai.GetProperty("BaseUrl").GetString());
        Assert.Equal(2, ai.GetProperty("MaxAttemptsPerRequest").GetInt32());
        Assert.True(ai.GetProperty("SaveFullPrompts").GetBoolean());
        Assert.True(ai.GetProperty("SaveFullResponses").GetBoolean());
    }

    [Fact]
    public void DockerCompose_AddsLiteLlmServiceAndApiEnvironment()
    {
        var compose = File.ReadAllText(Path.Combine("..", "..", "..", "..", "docker-compose.yml"));

        Assert.Contains("litellm:", compose);
        Assert.Contains("docker.litellm.ai/berriai/litellm:main-latest", compose);
        Assert.Contains("Ai__BaseUrl=http://litellm:4000", compose);
        Assert.Contains("Ai__Provider=litellm", compose);
    }
}
```

- [ ] **Step 2: Run configuration tests and verify they fail**

Run:

```powershell
dotnet test tests\Pim.UnitTests\Pim.UnitTests.csproj --filter AiConfigurationTests
```

Expected: FAIL because AI configuration and LiteLLM service are not present.

- [ ] **Step 3: Add API settings**

Modify `src/Pim.Api/appsettings.json` and `src/Pim.Api/appsettings.Development.json`:

```json
"Ai": {
  "Enabled": false,
  "Provider": "litellm",
  "BaseUrl": "http://litellm:4000",
  "ApiKey": "",
  "DefaultModel": "pim-default",
  "TimeoutSeconds": 30,
  "MaxOutputTokensPerRequest": 1000,
  "MaxAttemptsPerRequest": 2,
  "SaveFullPrompts": true,
  "SaveFullResponses": true
}
```

In development settings use:

```json
"BaseUrl": "http://127.0.0.1:4000"
```

- [ ] **Step 4: Add LiteLLM config**

Create `litellm-config.yaml`:

```yaml
model_list:
  - model_name: pim-default
    litellm_params:
      model: os.environ/LITELLM_UPSTREAM_MODEL
      api_key: os.environ/LITELLM_UPSTREAM_API_KEY
      api_base: os.environ/LITELLM_UPSTREAM_API_BASE

litellm_settings:
  json_logs: true
  request_timeout: 60
```

- [ ] **Step 5: Add Docker Compose service and API env**

Modify `docker-compose.yml` under `pim-api.environment`:

```yaml
      - Ai__Enabled=${AI_ENABLED:-false}
      - Ai__Provider=litellm
      - Ai__BaseUrl=http://litellm:4000
      - Ai__ApiKey=${PIM_LITELLM_VIRTUAL_KEY}
      - Ai__DefaultModel=${PIM_AI_DEFAULT_MODEL:-pim-default}
      - Ai__TimeoutSeconds=30
      - Ai__MaxOutputTokensPerRequest=1000
      - Ai__MaxAttemptsPerRequest=2
      - Ai__SaveFullPrompts=true
      - Ai__SaveFullResponses=true
```

Add `litellm` to `pim-api.depends_on`:

```yaml
      litellm:
        condition: service_started
```

Add service:

```yaml
  litellm:
    image: docker.litellm.ai/berriai/litellm:main-latest
    restart: unless-stopped
    command: ["--config", "/app/config.yaml", "--port", "4000"]
    environment:
      - DATABASE_URL=postgresql://pim:${PG_PASSWORD}@postgres:5432/pim
      - LITELLM_MASTER_KEY=${LITELLM_MASTER_KEY}
      - LITELLM_SALT_KEY=${LITELLM_SALT_KEY}
      - LITELLM_UPSTREAM_MODEL=${LITELLM_UPSTREAM_MODEL}
      - LITELLM_UPSTREAM_API_KEY=${LITELLM_UPSTREAM_API_KEY}
      - LITELLM_UPSTREAM_API_BASE=${LITELLM_UPSTREAM_API_BASE}
    volumes:
      - ./litellm-config.yaml:/app/config.yaml:ro
    depends_on:
      postgres:
        condition: service_healthy
    networks:
      - pim-net
```

- [ ] **Step 6: Update `.env.example`**

Add:

```dotenv
AI_ENABLED=false
PIM_AI_DEFAULT_MODEL=pim-default
PIM_LITELLM_VIRTUAL_KEY=sk-change-me
LITELLM_MASTER_KEY=sk-change-me-master
LITELLM_SALT_KEY=change-me-long-random-salt
LITELLM_UPSTREAM_MODEL=openai/gpt-4.1-mini
LITELLM_UPSTREAM_API_KEY=sk-change-me-upstream
LITELLM_UPSTREAM_API_BASE=https://api.openai.com/v1
```

- [ ] **Step 7: Run configuration tests and commit**

Run:

```powershell
dotnet test tests\Pim.UnitTests\Pim.UnitTests.csproj --filter AiConfigurationTests
```

Expected: PASS.

Commit:

```powershell
git add src\Pim.Api\appsettings.json src\Pim.Api\appsettings.Development.json docker-compose.yml .env.example litellm-config.yaml tests\Pim.UnitTests\Ai\AiConfigurationTests.cs
git commit -m "feat: add litellm deployment defaults"
```

---

### Task 9: Web AI API Client

**Files:**
- Modify: `src/client-web/src/types/index.ts`
- Create: `src/client-web/src/api/ai.ts`

- [ ] **Step 1: Add frontend AI types**

Modify `src/client-web/src/types/index.ts`:

```ts
export type AiRequestStatus = 'Succeeded' | 'Failed' | 'Blocked' | 'TimedOut' | 'FailedValidation';

export interface AiStatus {
  enabled: boolean;
  provider: string;
  baseUrl: string;
  defaultModel: string;
  lastHealthCheckAt?: string | null;
  lastError?: string | null;
  recentSuccessfulCallAt?: string | null;
}

export interface AiRequestLogListItem {
  id: string;
  startedAt: string;
  module: string;
  purpose: string;
  model: string;
  status: AiRequestStatus;
  totalTokens?: number | null;
  estimatedCost?: number | null;
  durationMs?: number | null;
  sourceObjectType: string;
  sourceObjectId: string;
  errorSummary?: string | null;
}

export interface AiRequestLogDetail extends AiRequestLogListItem {
  userId?: string | null;
  provider: string;
  liteLlmRequestId?: string | null;
  correlationId: string;
  attemptNumber: number;
  maxAttempts: number;
  finishedAt?: string | null;
  requestMessagesJson: string;
  requestPayloadJson: string;
  responseRawJson: string;
  responseText?: string | null;
  parsedOutputJson?: string | null;
  schemaName?: string | null;
  schemaVersion?: string | null;
  schemaJsonSnapshot?: string | null;
  schemaValidationErrorsJson: string;
  usage: {
    promptTokens?: number | null;
    completionTokens?: number | null;
    totalTokens?: number | null;
    estimatedCost?: number | null;
    currency?: string | null;
  };
  errorCode?: string | null;
  errorMessage?: string | null;
  metadataJson: string;
}

export interface AiUsageGroup {
  groupKey: string;
  requestCount: number;
  successCount: number;
  failureCount: number;
  promptTokens: number;
  completionTokens: number;
  totalTokens: number;
  estimatedCost: number;
}

export interface AiUsageSummary {
  requestCount: number;
  successCount: number;
  failureCount: number;
  promptTokens: number;
  completionTokens: number;
  totalTokens: number;
  estimatedCost: number;
  byModule: AiUsageGroup[];
  byPurpose: AiUsageGroup[];
  byModel: AiUsageGroup[];
  byStatus: AiUsageGroup[];
}
```

- [ ] **Step 2: Add API client**

Create `src/client-web/src/api/ai.ts`:

```ts
import { apiGet, apiPost } from './client';
import type { ApiResponse, PagedResult } from '../types';
import type { AiRequestLogDetail, AiRequestLogListItem, AiStatus, AiUsageSummary } from '../types';

export const aiApiPaths = {
  status: '/ai/status',
  test: '/ai/test',
  requests: '/ai/requests',
  requestDetail: (id: string) => `/ai/requests/${id}`,
  usageSummary: '/ai/usage/summary',
  healthCheck: '/ai/health-check',
} as const;

export interface AiRequestFilters {
  module?: string;
  purpose?: string;
  model?: string;
  status?: string;
  page?: number;
  pageSize?: number;
}

function query(params: Record<string, string | number | undefined>) {
  const search = new URLSearchParams();
  Object.entries(params).forEach(([key, value]) => {
    if (value !== undefined && value !== '') search.set(key, String(value));
  });
  const text = search.toString();
  return text ? `?${text}` : '';
}

export async function getAiStatus() {
  const response = await apiGet<ApiResponse<AiStatus>>(aiApiPaths.status);
  return response.data;
}

export async function runAiTest() {
  const response = await apiPost<ApiResponse<unknown>>(aiApiPaths.test);
  return response.data;
}

export async function runAiHealthCheck() {
  const response = await apiPost<ApiResponse<AiStatus>>(aiApiPaths.healthCheck);
  return response.data;
}

export async function getAiRequests(filters: AiRequestFilters) {
  const response = await apiGet<ApiResponse<PagedResult<AiRequestLogListItem>>>(
    `${aiApiPaths.requests}${query(filters)}`
  );
  return response.data;
}

export async function getAiRequestDetail(id: string) {
  const response = await apiGet<ApiResponse<AiRequestLogDetail>>(aiApiPaths.requestDetail(id));
  return response.data;
}

export async function getAiUsageSummary() {
  const response = await apiGet<ApiResponse<AiUsageSummary>>(aiApiPaths.usageSummary);
  return response.data;
}
```

- [ ] **Step 3: Build frontend and commit**

Run:

```powershell
npm --prefix src/client-web run build
```

Expected: PASS with the existing Vite chunk-size warning.

Commit:

```powershell
git add src\client-web\src\types\index.ts src\client-web\src\api\ai.ts
git commit -m "feat: add ai web api client"
```

---

### Task 10: Web AI Settings Page

**Files:**
- Create: `src/client-web/src/components/ai/AiStatusPanel.tsx`
- Create: `src/client-web/src/components/ai/AiUsageOverview.tsx`
- Create: `src/client-web/src/components/ai/AiRequestLogTable.tsx`
- Create: `src/client-web/src/components/ai/AiRequestDetailPanel.tsx`
- Create: `src/client-web/src/pages/AiSettingsPage.tsx`
- Modify: `src/client-web/src/layout/AppLayout.tsx`
- Modify: `src/client-web/src/pages/SettingsPage.tsx`

- [ ] **Step 1: Add status panel**

Create `src/client-web/src/components/ai/AiStatusPanel.tsx`:

```tsx
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { runAiHealthCheck, runAiTest } from '../../api/ai';
import type { AiStatus } from '../../types';
import StatusBadge from '../../ui/StatusBadge';

function formatDate(value?: string | null) {
  if (!value) return '未检查';
  const date = new Date(value);
  return Number.isNaN(date.getTime()) ? value : date.toLocaleString('zh-CN');
}

export default function AiStatusPanel({ status }: { status?: AiStatus }) {
  const queryClient = useQueryClient();
  const health = useMutation({
    mutationFn: runAiHealthCheck,
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ['ai-status'] }),
  });
  const test = useMutation({
    mutationFn: runAiTest,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['ai-status'] });
      queryClient.invalidateQueries({ queryKey: ['ai-requests'] });
      queryClient.invalidateQueries({ queryKey: ['ai-usage'] });
    },
  });

  return (
    <section className="rounded-lg border border-slate-200 bg-white p-4">
      <div className="flex flex-wrap items-start justify-between gap-3">
        <div>
          <h2 className="text-sm font-semibold text-slate-950">AI 配置状态</h2>
          <p className="mt-1 text-xs text-slate-500">{status?.provider || 'litellm'} · {status?.defaultModel || '未配置模型'}</p>
        </div>
        <StatusBadge tone={status?.enabled ? 'success' : 'neutral'}>{status?.enabled ? '已启用' : '已关闭'}</StatusBadge>
      </div>
      <dl className="mt-4 grid grid-cols-1 gap-3 text-sm sm:grid-cols-2 lg:grid-cols-4">
        <div><dt className="text-xs text-slate-400">LiteLLM</dt><dd className="mt-1 break-all text-slate-700">{status?.baseUrl || '-'}</dd></div>
        <div><dt className="text-xs text-slate-400">最近健康检查</dt><dd className="mt-1 text-slate-700">{formatDate(status?.lastHealthCheckAt)}</dd></div>
        <div><dt className="text-xs text-slate-400">最近成功调用</dt><dd className="mt-1 text-slate-700">{formatDate(status?.recentSuccessfulCallAt)}</dd></div>
        <div><dt className="text-xs text-slate-400">最近错误</dt><dd className="mt-1 break-words text-slate-700">{status?.lastError || '无'}</dd></div>
      </dl>
      <div className="mt-4 flex flex-wrap gap-2">
        <button type="button" onClick={() => health.mutate()} disabled={health.isPending} className="pim-button-secondary px-3 py-2 text-sm disabled:opacity-60">健康检查</button>
        <button type="button" onClick={() => test.mutate()} disabled={test.isPending} className="pim-button-primary px-3 py-2 text-sm disabled:opacity-60">测试连接</button>
      </div>
    </section>
  );
}
```

- [ ] **Step 2: Add usage overview**

Create `src/client-web/src/components/ai/AiUsageOverview.tsx`:

```tsx
import MetricCard from '../../ui/MetricCard';
import type { AiUsageSummary } from '../../types';

export default function AiUsageOverview({ usage }: { usage?: AiUsageSummary }) {
  const failureRate = usage && usage.requestCount > 0
    ? `${Math.round((usage.failureCount / usage.requestCount) * 100)}%`
    : '0%';

  return (
    <section className="space-y-3">
      <div className="grid grid-cols-1 gap-3 md:grid-cols-4">
        <MetricCard label="请求" value={usage?.requestCount ?? 0} />
        <MetricCard label="Token" value={usage?.totalTokens ?? 0} />
        <MetricCard label="估算成本" value={`$${(usage?.estimatedCost ?? 0).toFixed(4)}`} />
        <MetricCard label="失败率" value={failureRate} />
      </div>
      <div className="grid grid-cols-1 gap-3 lg:grid-cols-2">
        {(usage?.byModule ?? []).slice(0, 6).map(group => (
          <div key={`module-${group.groupKey}`} className="rounded-lg border border-slate-200 bg-white p-3">
            <div className="flex items-center justify-between gap-3 text-sm">
              <span className="truncate font-medium text-slate-700">{group.groupKey}</span>
              <span className="text-slate-500">{group.requestCount} 次 · {group.totalTokens} token</span>
            </div>
          </div>
        ))}
        {(usage?.byModel ?? []).slice(0, 6).map(group => (
          <div key={`model-${group.groupKey}`} className="rounded-lg border border-slate-200 bg-white p-3">
            <div className="flex items-center justify-between gap-3 text-sm">
              <span className="truncate font-medium text-slate-700">{group.groupKey}</span>
              <span className="text-slate-500">{group.requestCount} 次 · ${group.estimatedCost.toFixed(4)}</span>
            </div>
          </div>
        ))}
      </div>
    </section>
  );
}
```

- [ ] **Step 3: Add request log table**

Create `src/client-web/src/components/ai/AiRequestLogTable.tsx`:

```tsx
import type { AiRequestLogListItem, PagedResult } from '../../types';

export default function AiRequestLogTable({
  data,
  selectedId,
  onSelect,
}: {
  data?: PagedResult<AiRequestLogListItem>;
  selectedId?: string | null;
  onSelect: (id: string) => void;
}) {
  return (
    <section className="rounded-lg border border-slate-200 bg-white">
      <div className="border-b border-slate-100 px-4 py-3">
        <h2 className="text-sm font-semibold text-slate-950">请求日志</h2>
      </div>
      <div className="overflow-auto">
        <table className="min-w-full text-left text-sm">
          <thead className="bg-slate-50 text-xs uppercase text-slate-400">
            <tr>
              <th className="px-4 py-2">时间</th>
              <th className="px-4 py-2">模块</th>
              <th className="px-4 py-2">模型</th>
              <th className="px-4 py-2">状态</th>
              <th className="px-4 py-2">Token</th>
              <th className="px-4 py-2">耗时</th>
              <th className="px-4 py-2">错误</th>
            </tr>
          </thead>
          <tbody className="divide-y divide-slate-100">
            {(data?.items ?? []).map(item => (
              <tr
                key={item.id}
                onClick={() => onSelect(item.id)}
                className={`cursor-pointer hover:bg-slate-50 ${selectedId === item.id ? 'bg-blue-50' : ''}`}
              >
                <td className="px-4 py-2 text-slate-600">{new Date(item.startedAt).toLocaleString('zh-CN')}</td>
                <td className="px-4 py-2"><span className="font-medium text-slate-800">{item.module}</span><span className="block text-xs text-slate-400">{item.purpose}</span></td>
                <td className="px-4 py-2 text-slate-600">{item.model}</td>
                <td className="px-4 py-2 text-slate-600">{item.status}</td>
                <td className="px-4 py-2 text-slate-600">{item.totalTokens ?? '-'}</td>
                <td className="px-4 py-2 text-slate-600">{item.durationMs ?? '-'} ms</td>
                <td className="max-w-[260px] truncate px-4 py-2 text-slate-500">{item.errorSummary || '-'}</td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </section>
  );
}
```

- [ ] **Step 4: Add detail panel**

Create `src/client-web/src/components/ai/AiRequestDetailPanel.tsx`:

```tsx
import type { AiRequestLogDetail } from '../../types';

function JsonBlock({ title, value }: { title: string; value?: string | null }) {
  return (
    <div>
      <h3 className="text-xs font-semibold uppercase text-slate-400">{title}</h3>
      <pre className="mt-2 max-h-64 overflow-auto rounded-lg bg-slate-950 p-3 text-xs text-slate-100">{value || '-'}</pre>
    </div>
  );
}

export default function AiRequestDetailPanel({ detail }: { detail?: AiRequestLogDetail }) {
  if (!detail) {
    return (
      <section className="rounded-lg border border-slate-200 bg-white p-4 text-sm text-slate-500">
        选择一条请求查看完整提示、输出、JSON 和校验结果。
      </section>
    );
  }

  return (
    <section className="space-y-4 rounded-lg border border-slate-200 bg-white p-4">
      <div>
        <h2 className="text-sm font-semibold text-slate-950">{detail.module} · {detail.purpose}</h2>
        <p className="mt-1 text-xs text-slate-500">{detail.model} · {detail.status} · {detail.correlationId}</p>
      </div>
      <div className="grid grid-cols-1 gap-3 text-sm sm:grid-cols-3">
        <div><span className="text-xs text-slate-400">Prompt</span><p className="text-slate-700">{detail.usage.promptTokens ?? '-'}</p></div>
        <div><span className="text-xs text-slate-400">Completion</span><p className="text-slate-700">{detail.usage.completionTokens ?? '-'}</p></div>
        <div><span className="text-xs text-slate-400">Cost</span><p className="text-slate-700">{detail.usage.estimatedCost ?? '-'}</p></div>
      </div>
      <JsonBlock title="Messages" value={detail.requestMessagesJson} />
      <JsonBlock title="Request Payload" value={detail.requestPayloadJson} />
      <JsonBlock title="Response Text" value={detail.responseText} />
      <JsonBlock title="Raw JSON" value={detail.responseRawJson} />
      <JsonBlock title="Parsed JSON" value={detail.parsedOutputJson} />
      <JsonBlock title="Schema Errors" value={detail.schemaValidationErrorsJson} />
    </section>
  );
}
```

- [ ] **Step 5: Add page and route**

Create `src/client-web/src/pages/AiSettingsPage.tsx`:

```tsx
import { useState } from 'react';
import { useQuery } from '@tanstack/react-query';
import { getAiRequestDetail, getAiRequests, getAiStatus, getAiUsageSummary } from '../api/ai';
import AiRequestDetailPanel from '../components/ai/AiRequestDetailPanel';
import AiRequestLogTable from '../components/ai/AiRequestLogTable';
import AiStatusPanel from '../components/ai/AiStatusPanel';
import AiUsageOverview from '../components/ai/AiUsageOverview';
import PageHeader from '../ui/PageHeader';

export default function AiSettingsPage() {
  const [selectedId, setSelectedId] = useState<string | null>(null);
  const status = useQuery({ queryKey: ['ai-status'], queryFn: getAiStatus, refetchInterval: 60_000 });
  const usage = useQuery({ queryKey: ['ai-usage'], queryFn: getAiUsageSummary, refetchInterval: 60_000 });
  const requests = useQuery({ queryKey: ['ai-requests'], queryFn: () => getAiRequests({ page: 1, pageSize: 50 }), refetchInterval: 30_000 });
  const detail = useQuery({
    queryKey: ['ai-request-detail', selectedId],
    queryFn: () => getAiRequestDetail(selectedId!),
    enabled: !!selectedId,
  });

  return (
    <div className="mx-auto w-full max-w-[1280px] space-y-4 pb-8">
      <PageHeader title="AI 设置" subtitle="查看 LiteLLM 状态、用量、请求日志和完整请求详情" />
      <AiStatusPanel status={status.data} />
      <AiUsageOverview usage={usage.data} />
      <div className="grid grid-cols-1 gap-4 xl:grid-cols-[minmax(0,1.35fr)_minmax(360px,0.65fr)]">
        <AiRequestLogTable data={requests.data} selectedId={selectedId} onSelect={setSelectedId} />
        <AiRequestDetailPanel detail={detail.data} />
      </div>
    </div>
  );
}
```

Modify `src/client-web/src/layout/AppLayout.tsx`:

```tsx
import AiSettingsPage from '../pages/AiSettingsPage';
```

Add route:

```tsx
<Route path="/settings/ai" element={<AiSettingsPage />} />
```

Modify `src/client-web/src/pages/SettingsPage.tsx` by adding to `settingsLinks`:

```ts
{
  title: 'AI 网关',
  description: '查看 LiteLLM 状态、用量、请求日志与完整请求详情',
  label: 'AI',
  to: '/settings/ai',
},
```

- [ ] **Step 6: Build frontend and commit**

Run:

```powershell
npm --prefix src/client-web run build
```

Expected: PASS with the existing Vite chunk-size warning.

Do not stage generated `src/Pim.Api/wwwroot` files.

Commit:

```powershell
git add src\client-web\src\components\ai src\client-web\src\pages\AiSettingsPage.tsx src\client-web\src\layout\AppLayout.tsx src\client-web\src\pages\SettingsPage.tsx
git commit -m "feat: add ai settings page"
```

---

### Task 11: End-To-End Verification And Cleanup

**Files:**
- Modify only files required by compiler, test, or build failures found in this task.

- [ ] **Step 1: Run backend tests**

Run:

```powershell
dotnet test Pim.sln
```

Expected: PASS. Existing nullable warnings may remain if they are unchanged from baseline.

- [ ] **Step 2: Run frontend build**

Run:

```powershell
npm --prefix src/client-web run build
```

Expected: PASS with the existing Vite chunk-size warning.

- [ ] **Step 3: Verify generated web output is not staged**

Run:

```powershell
git status --short
```

Expected: source changes only. If `src/Pim.Api/wwwroot/` appears from the build, leave it unstaged unless the repository has explicitly started tracking those files for this branch.

- [ ] **Step 4: Optional manual LiteLLM smoke test**

Run only when `.env` contains real non-production AI credentials:

```powershell
docker compose up -d postgres litellm pim-api
```

Then:

```powershell
curl.exe -f http://127.0.0.1:5858/health
```

Expected: HTTP 200 with a healthy API response.

Open the web app, sign in, navigate to `/settings/ai`, run "健康检查", then run "测试连接". Confirm an `ai_request_logs` row exists with:

- `purpose = ai.test`
- full prompt messages
- response text or user-visible provider failure
- token fields when LiteLLM/provider returns usage
- no credential values in JSON fields

- [ ] **Step 5: Final status and commit**

Run:

```powershell
git status --short --branch
```

Expected: clean branch after intentional commits, or only ignored/generated outputs outside staging.

If fixes were needed during verification:

```powershell
git add <fixed-source-files>
git commit -m "fix: stabilize ai gateway verification"
```

---

## Spec Coverage Review

- System AI configuration: Tasks 5 and 8.
- LiteLLM Proxy service: Task 8.
- .NET abstraction with `Microsoft.Extensions.AI`: Task 5.
- PIM `IAiGateway` entry point: Tasks 1 and 5.
- Full request/response logging: Tasks 2, 3, and 5.
- Token and estimated cost recording: Tasks 1, 2, 5, and 6.
- JSON Schema validation: Task 4 and Task 5.
- Hard retry limit: Task 1 and Task 5.
- AI status, logs, detail, usage summary APIs: Tasks 6 and 7.
- Web settings page: Tasks 9 and 10.
- Credential redaction: Task 3.
- Provider/user-visible failures: Tasks 5, 6, 7, and 10.
- Future modules use `IAiGateway`: Task 1 creates the contract; module adoption should be done in the later feature stages that add AI behavior to those modules.

## Placeholder Scan

This plan contains no placeholder markers, no open-ended validation instructions, no undefined task references, and no implementation step that relies on unstated code. Generated EF migration content is created by the EF tool from the entity model and verified by table/index names.

## Type Consistency Review

The plan consistently uses:

- `AiRequestStatus.FailedValidation` in C# and `FailedValidation` in TypeScript.
- storage status `failed_validation` in `ai_request_logs.status`.
- `AiGatewayRequest.EffectiveMaxAttempts` as the first-version hard limit.
- `IAiGateway.CompleteAsync(...)` as the only business-facing generation entry point.
- `IAiUsageService` for status, list, detail, and usage summaries.
