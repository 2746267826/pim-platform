# Stage 0 Sustainable Operations Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build the Stage 0 operations foundation: formal database migrations, system status, shared audit and confirmations, Hangfire jobs, daemon heartbeat reporting, structured logs, Web status UI, and backup/restore documentation.

**Architecture:** `Pim.Core` owns stable contracts, `Pim.Infrastructure` owns persistence and services, `Pim.Api` exposes endpoints and startup wiring, Windows daemon reports health, and Web displays server-computed status. The migration plan uses a baseline migration plus an adoption service so both fresh databases and existing `EnsureCreated()` databases are safe.

**Tech Stack:** .NET 8, EF Core/Npgsql, ASP.NET Core Minimal APIs, Serilog JSON Lines, Hangfire.AspNetCore 1.8.23, Hangfire.PostgreSql 1.21.1, React 19, React Query, Tailwind CSS, xUnit.

---

## Scope Check

The design spans API, infrastructure, Web, daemon, and documentation. These are not independent products; they are one Stage 0 foundation with ordered dependencies. The plan keeps tasks independently testable and commit-sized:

1. Shared contracts.
2. Migration baseline and adoption.
3. Stage 0 persistence.
4. Audit and confirmation services.
5. Health and daemon status services.
6. API endpoints.
7. Hangfire.
8. Structured logging.
9. Windows daemon heartbeat.
10. Web status UI.
11. Backup/restore docs and final verification.

## File Structure

### Core Contracts

- Create `src/Pim.Core/Operations/OperationEnums.cs`: shared enum values for status, audit, confirmations, and daemon source states.
- Create `src/Pim.Core/Operations/StatusDtos.cs`: status summary/detail DTOs returned by API and consumed by Web.
- Create `src/Pim.Core/Operations/AuditDtos.cs`: audit input/result DTOs and interface.
- Create `src/Pim.Core/Operations/ConfirmationDtos.cs`: confirmation input/result DTOs and interface.
- Create `src/Pim.Core/Operations/DaemonHeartbeatDtos.cs`: daemon heartbeat input/result DTOs and interface.
- Create `src/Pim.Core/Operations/BackgroundJobDtos.cs`: Hangfire summary DTOs and interface.

### Infrastructure Persistence And Services

- Modify `src/Pim.Infrastructure/Pim.Infrastructure.csproj`: add EF design-time, Hangfire, and compact JSON logging dependencies.
- Create `src/Pim.Infrastructure/Data/Entities/AuditLogEntity.cs`: `audit_logs` table model.
- Create `src/Pim.Infrastructure/Data/Entities/OperationConfirmationEntity.cs`: `operation_confirmations` table model.
- Create `src/Pim.Infrastructure/Data/Entities/DaemonHeartbeatEntity.cs`: `daemon_heartbeats` table model.
- Modify `src/Pim.Infrastructure/Data/PimDbContext.cs`: add DbSets and entity configuration.
- Create `src/Pim.Infrastructure/Data/PimMigrationAdoptionService.cs`: marks the baseline migration as applied for existing `EnsureCreated()` databases.
- Create `src/Pim.Infrastructure/Operations/AuditLogService.cs`: explicit audit record service.
- Create `src/Pim.Infrastructure/Operations/OperationConfirmationService.cs`: reusable confirmation lifecycle service.
- Create `src/Pim.Infrastructure/Operations/DaemonHeartbeatService.cs`: heartbeat upsert and lookup service.
- Create `src/Pim.Infrastructure/Operations/SystemStatusService.cs`: aggregate system status.
- Create `src/Pim.Infrastructure/Operations/HangfireJobStatusService.cs`: Web-safe Hangfire status summary.
- Create `src/Pim.Infrastructure/Operations/Stage0DiagnosticJob.cs`: low-risk diagnostic job used to verify Hangfire.
- Modify `src/Pim.Infrastructure/Extensions/ServiceCollectionExtensions.cs`: register Stage 0 services and Hangfire.

### API

- Create `src/Pim.Api/Endpoints/StatusEndpoints.cs`: `/api/v1/status/summary` and `/api/v1/status`.
- Create `src/Pim.Api/Endpoints/DaemonEndpoints.cs`: `/api/v1/daemon/heartbeat`.
- Create `src/Pim.Api/Endpoints/OperationsEndpoints.cs`: audit and confirmation endpoints needed by the status page.
- Create `src/Pim.Api/Infrastructure/CorrelationIdMiddleware.cs`: request correlation id.
- Create `src/Pim.Api/Infrastructure/HangfireAuthorizationFilter.cs`: dashboard protection.
- Modify `src/Pim.Api/Program.cs`: replace `EnsureCreated()` with migration adoption + `Migrate()`, map endpoints, add Hangfire dashboard, configure JSON logging.
- Modify `src/Pim.Api/Pim.Api.csproj`: add `Serilog.Formatting.Compact` for JSON Lines API logs.

### Windows Daemon

- Create `src/client-windows/Pim.Client.Core/Models/DaemonHeartbeatDtos.cs`: daemon heartbeat DTO.
- Modify `src/client-windows/Pim.Client.Core/Services/ApiClient.cs`: expose `PostAsync` use for heartbeat and keep default URL behavior.
- Create `src/client-windows/Pim.Client.Core/Services/DaemonHeartbeatReporter.cs`: gathers and submits daemon status.
- Modify `src/client-windows/Pim.Client.Core/Services/AwCollectorService.cs`: expose collector health fields or last known state.
- Modify `src/client-windows/Pim.Client.Core/Services/KeyStatsCollectorService.cs`: expose collector health fields or last known state.
- Modify `src/client-windows/Pim.Client.App/App.xaml.cs`: start periodic heartbeat reporting.
- Modify `src/client-windows/Pim.Client.App/Services/Logger.cs`: JSON Lines daemon logs.
- Modify `src/client-windows/Pim.Client.App/Pim.Client.App.csproj`: add `Serilog.Formatting.Compact`.

### Web

- Create `src/client-web/src/api/status.ts`: status API client functions.
- Modify `src/client-web/src/types/index.ts`: status DTOs.
- Create `src/client-web/src/components/status/SidebarStatusIndicator.tsx`: compact sidebar indicator.
- Create `src/client-web/src/pages/StatusPage.tsx`: "状态信息" page.
- Modify `src/client-web/src/layout/Sidebar.tsx`: add status indicator and navigation item.
- Modify `src/client-web/src/layout/AppLayout.tsx`: add `/status` route.

### Documentation

- Create `docs/operations/backup-restore.md`: manual backup and restore guidance.
- Create `docs/operations/migrations.md`: migration and existing-database adoption guidance.
- Modify `docs/superpowers/specs/2026-05-24-stage-0-sustainable-operations-design.md` only if implementation reveals a design clarification that should be preserved.

### Tests

- Modify `tests/Pim.UnitTests/Pim.UnitTests.csproj`: add direct references and packages needed for infrastructure/service tests.
- Create `tests/Pim.UnitTests/Operations/Stage0ContractsTests.cs`.
- Create `tests/Pim.UnitTests/Operations/PimMigrationAdoptionServiceTests.cs`.
- Create `tests/Pim.UnitTests/Operations/AuditAndConfirmationServiceTests.cs`.
- Create `tests/Pim.UnitTests/Operations/DaemonHeartbeatServiceTests.cs`.
- Create `tests/Pim.UnitTests/Operations/SystemStatusServiceTests.cs`.
- Create `tests/Pim.UnitTests/Operations/HangfireJobStatusServiceTests.cs`.
- Create or extend `tests/Pim.UnitTests/ClientWindows/DaemonHeartbeatReporterTests.cs`.
- Create `tests/client-web/statusApiPath.test.ts`.

---

## Task 1: Shared Stage 0 Contracts

**Files:**
- Create: `src/Pim.Core/Operations/OperationEnums.cs`
- Create: `src/Pim.Core/Operations/StatusDtos.cs`
- Create: `src/Pim.Core/Operations/AuditDtos.cs`
- Create: `src/Pim.Core/Operations/ConfirmationDtos.cs`
- Create: `src/Pim.Core/Operations/DaemonHeartbeatDtos.cs`
- Create: `src/Pim.Core/Operations/BackgroundJobDtos.cs`
- Create: `tests/Pim.UnitTests/Operations/Stage0ContractsTests.cs`

- [ ] **Step 1: Write the failing contract test**

Create `tests/Pim.UnitTests/Operations/Stage0ContractsTests.cs`:

```csharp
using Pim.Core.Operations;

namespace Pim.UnitTests.Operations;

public class Stage0ContractsTests
{
    [Fact]
    public void HealthStatus_Order_AllowsWorstStatusAggregation()
    {
        Assert.True((int)PimHealthStatus.Unknown < (int)PimHealthStatus.Healthy);
        Assert.True((int)PimHealthStatus.Healthy < (int)PimHealthStatus.Warning);
        Assert.True((int)PimHealthStatus.Warning < (int)PimHealthStatus.Critical);
    }

    [Fact]
    public void ConfirmationStatus_IncludesRequiredLifecycle()
    {
        var names = Enum.GetNames<OperationConfirmationStatus>();

        Assert.Contains("Pending", names);
        Assert.Contains("Confirmed", names);
        Assert.Contains("Rejected", names);
        Assert.Contains("Expired", names);
        Assert.Contains("Executed", names);
    }

    [Fact]
    public void StatusSummary_CanRepresentSidebarIndicator()
    {
        var summary = new SystemStatusSummaryDto(
            PimHealthStatus.Warning,
            "有警告",
            "Windows daemon has not reported recently.",
            DateTimeOffset.Parse("2026-05-24T00:00:00Z"));

        Assert.Equal(PimHealthStatus.Warning, summary.Status);
        Assert.Equal("有警告", summary.Label);
        Assert.Contains("daemon", summary.Message, StringComparison.OrdinalIgnoreCase);
    }
}
```

- [ ] **Step 2: Run the test to verify it fails**

Run:

```powershell
dotnet test tests\Pim.UnitTests\Pim.UnitTests.csproj --filter Stage0ContractsTests
```

Expected: FAIL with compiler errors because `Pim.Core.Operations` types do not exist.

- [ ] **Step 3: Create shared enums**

Create `src/Pim.Core/Operations/OperationEnums.cs`:

```csharp
namespace Pim.Core.Operations;

public enum PimHealthStatus
{
    Unknown = 0,
    Healthy = 1,
    Warning = 2,
    Critical = 3
}

public enum StatusComponentKind
{
    Api,
    Database,
    Storage,
    TextExtraction,
    Daemon,
    ActivityWatch,
    KeyStats,
    BackgroundJobs
}

public enum AuditActorType
{
    User,
    Daemon,
    System,
    Job,
    Mcp
}

public enum AuditResult
{
    Success,
    Failure,
    PendingConfirmation,
    Rejected
}

public enum OperationConfirmationStatus
{
    Pending,
    Confirmed,
    Rejected,
    Expired,
    Executed
}

public enum OperationRiskLevel
{
    Low,
    Medium,
    High
}

public enum DaemonSourceState
{
    Unknown,
    Available,
    Unavailable,
    Paused
}
```

- [ ] **Step 4: Create status DTOs**

Create `src/Pim.Core/Operations/StatusDtos.cs`:

```csharp
namespace Pim.Core.Operations;

public sealed record SystemStatusSummaryDto(
    PimHealthStatus Status,
    string Label,
    string Message,
    DateTimeOffset CheckedAt);

public sealed record StatusComponentDto(
    string Key,
    string Name,
    StatusComponentKind Kind,
    PimHealthStatus Status,
    string Message,
    DateTimeOffset CheckedAt,
    IReadOnlyDictionary<string, string> Details);

public sealed record SystemStatusDetailDto(
    SystemStatusSummaryDto Summary,
    IReadOnlyList<StatusComponentDto> Components,
    IReadOnlyList<string> NextSteps);

public interface ISystemStatusService
{
    Task<SystemStatusSummaryDto> GetSummaryAsync(CancellationToken ct = default);
    Task<SystemStatusDetailDto> GetDetailAsync(CancellationToken ct = default);
}
```

- [ ] **Step 5: Create audit DTOs and interface**

Create `src/Pim.Core/Operations/AuditDtos.cs`:

```csharp
namespace Pim.Core.Operations;

public sealed record CreateAuditLogRequest(
    Guid? UserId,
    AuditActorType ActorType,
    string Action,
    string ResourceType,
    string? ResourceId,
    string Source,
    AuditResult Result,
    string? IpAddress,
    string? UserAgent,
    string? CorrelationId,
    IReadOnlyDictionary<string, string>? Metadata,
    int? ErrorCode,
    string? ErrorMessage);

public sealed record AuditLogDto(
    Guid Id,
    Guid? UserId,
    AuditActorType ActorType,
    string Action,
    string ResourceType,
    string? ResourceId,
    string Source,
    AuditResult Result,
    string? CorrelationId,
    DateTimeOffset CreatedAt);

public interface IAuditLogService
{
    Task<AuditLogDto> RecordAsync(CreateAuditLogRequest request, CancellationToken ct = default);
}
```

- [ ] **Step 6: Create confirmation DTOs and interface**

Create `src/Pim.Core/Operations/ConfirmationDtos.cs`:

```csharp
namespace Pim.Core.Operations;

public sealed record CreateOperationConfirmationRequest(
    Guid? RequestedByUserId,
    string OperationType,
    string Summary,
    OperationRiskLevel RiskLevel,
    string Source,
    string PayloadJson,
    string PreviewJson,
    DateTimeOffset ExpiresAt,
    string? CorrelationId);

public sealed record OperationConfirmationDto(
    Guid Id,
    Guid? RequestedByUserId,
    string OperationType,
    string Summary,
    OperationRiskLevel RiskLevel,
    string Source,
    string PayloadJson,
    string PreviewJson,
    OperationConfirmationStatus Status,
    DateTimeOffset ExpiresAt,
    DateTimeOffset CreatedAt,
    DateTimeOffset? ConfirmedAt,
    DateTimeOffset? ExecutedAt,
    string? ResultJson,
    string? CorrelationId);

public interface IOperationConfirmationService
{
    Task<OperationConfirmationDto> CreateAsync(CreateOperationConfirmationRequest request, CancellationToken ct = default);
    Task<OperationConfirmationDto?> GetAsync(Guid id, CancellationToken ct = default);
    Task<IReadOnlyList<OperationConfirmationDto>> ListPendingAsync(CancellationToken ct = default);
    Task<OperationConfirmationDto> ConfirmAsync(Guid id, Guid? userId, CancellationToken ct = default);
    Task<OperationConfirmationDto> RejectAsync(Guid id, Guid? userId, CancellationToken ct = default);
    Task<OperationConfirmationDto> MarkExecutedAsync(Guid id, string resultJson, CancellationToken ct = default);
    Task<int> ExpireOldAsync(DateTimeOffset now, CancellationToken ct = default);
}
```

- [ ] **Step 7: Create daemon and background job DTOs**

Create `src/Pim.Core/Operations/DaemonHeartbeatDtos.cs`:

```csharp
namespace Pim.Core.Operations;

public sealed record DaemonHeartbeatRequest(
    string DeviceId,
    string DaemonKind,
    string Version,
    string ServerUrl,
    DateTimeOffset? LastSuccessfulUploadAt,
    DateTimeOffset? LastAttemptedUploadAt,
    string? LastError,
    int? UploadQueueCount,
    DaemonSourceState ActivityWatchState,
    DaemonSourceState KeyStatsState,
    bool CollectionPaused,
    string StatusJson);

public sealed record DaemonHeartbeatDto(
    string DeviceId,
    string DaemonKind,
    string Version,
    string ServerUrl,
    DateTimeOffset? LastSuccessfulUploadAt,
    DateTimeOffset? LastAttemptedUploadAt,
    string? LastError,
    int? UploadQueueCount,
    DaemonSourceState ActivityWatchState,
    DaemonSourceState KeyStatsState,
    bool CollectionPaused,
    string StatusJson,
    DateTimeOffset ReceivedAt);

public interface IDaemonHeartbeatService
{
    Task<DaemonHeartbeatDto> UpsertAsync(DaemonHeartbeatRequest request, CancellationToken ct = default);
    Task<DaemonHeartbeatDto?> GetLatestAsync(string deviceId, CancellationToken ct = default);
    Task<DaemonHeartbeatDto?> GetLatestWindowsAsync(CancellationToken ct = default);
}
```

Create `src/Pim.Core/Operations/BackgroundJobDtos.cs`:

```csharp
namespace Pim.Core.Operations;

public sealed record BackgroundJobSummaryDto(
    PimHealthStatus Status,
    int Processing,
    int Enqueued,
    int Scheduled,
    int Failed,
    DateTimeOffset CheckedAt,
    string Message);

public interface IBackgroundJobStatusService
{
    Task<BackgroundJobSummaryDto> GetSummaryAsync(CancellationToken ct = default);
}
```

- [ ] **Step 8: Run the contract tests**

Run:

```powershell
dotnet test tests\Pim.UnitTests\Pim.UnitTests.csproj --filter Stage0ContractsTests
```

Expected: PASS.

- [ ] **Step 9: Commit**

```powershell
git add src\Pim.Core\Operations tests\Pim.UnitTests\Operations\Stage0ContractsTests.cs
git commit -m "feat: add stage 0 operation contracts"
```

---

## Task 2: Migration Baseline And Existing Database Adoption

**Files:**
- Modify: `src/Pim.Infrastructure/Pim.Infrastructure.csproj`
- Create: `src/Pim.Infrastructure/Data/PimMigrationAdoptionService.cs`
- Modify: `src/Pim.Api/Program.cs`
- Create: `tests/Pim.UnitTests/Operations/PimMigrationAdoptionServiceTests.cs`
- Generate: `src/Pim.Infrastructure/Data/Migrations/*_BaselineExistingSchema.cs`
- Generate: `src/Pim.Infrastructure/Data/Migrations/PimDbContextModelSnapshot.cs`

- [ ] **Step 1: Add test project references needed for infrastructure tests**

Modify `tests/Pim.UnitTests/Pim.UnitTests.csproj` so the project references include `Pim.Infrastructure` directly:

```xml
  <ItemGroup>
    <ProjectReference Include="..\..\src\Pim.Infrastructure\Pim.Infrastructure.csproj" />
    <ProjectReference Include="..\..\src\modules\Pim.Module.Calendar\Pim.Module.Calendar.csproj" />
    <ProjectReference Include="..\..\src\modules\Pim.Module.PcTracker\Pim.Module.PcTracker.csproj" />
    <ProjectReference Include="..\..\src\client-windows\Pim.Client.Core\Pim.Client.Core.csproj" />
  </ItemGroup>
```

- [ ] **Step 2: Write the failing migration adoption tests**

Create `tests/Pim.UnitTests/Operations/PimMigrationAdoptionServiceTests.cs`:

```csharp
using Microsoft.EntityFrameworkCore;
using Pim.Infrastructure.Data;

namespace Pim.UnitTests.Operations;

public class PimMigrationAdoptionServiceTests
{
    [Fact]
    public void NeedsBaselineAdoption_ReturnsFalse_WhenNoUsersTableExists()
    {
        Assert.False(PimMigrationAdoptionService.NeedsBaselineAdoption(false, false));
    }

    [Fact]
    public void NeedsBaselineAdoption_ReturnsTrue_WhenUsersTableExistsWithoutHistory()
    {
        Assert.True(PimMigrationAdoptionService.NeedsBaselineAdoption(true, false));
    }

    [Fact]
    public void NeedsBaselineAdoption_ReturnsFalse_WhenHistoryAlreadyExists()
    {
        Assert.False(PimMigrationAdoptionService.NeedsBaselineAdoption(true, true));
    }

    [Fact]
    public void BaselineMigrationId_IsStable()
    {
        Assert.Equal("20260524000000_BaselineExistingSchema", PimMigrationAdoptionService.BaselineMigrationId);
    }
}
```

- [ ] **Step 3: Run the test to verify it fails**

Run:

```powershell
dotnet test tests\Pim.UnitTests\Pim.UnitTests.csproj --filter PimMigrationAdoptionServiceTests
```

Expected: FAIL because `PimMigrationAdoptionService` does not exist.

- [ ] **Step 4: Add EF design package**

Modify `src/Pim.Infrastructure/Pim.Infrastructure.csproj`:

```xml
    <PackageReference Include="Microsoft.EntityFrameworkCore.Design" Version="8.0.11">
      <PrivateAssets>all</PrivateAssets>
      <IncludeAssets>runtime; build; native; contentfiles; analyzers; buildtransitive</IncludeAssets>
    </PackageReference>
```

- [ ] **Step 5: Create the adoption service**

Create `src/Pim.Infrastructure/Data/PimMigrationAdoptionService.cs`:

```csharp
using Microsoft.EntityFrameworkCore;
using Microsoft.Extensions.Logging;

namespace Pim.Infrastructure.Data;

public sealed class PimMigrationAdoptionService
{
    public const string BaselineMigrationId = "20260524000000_BaselineExistingSchema";

    private readonly PimDbContext _db;
    private readonly ILogger<PimMigrationAdoptionService> _logger;

    public PimMigrationAdoptionService(PimDbContext db, ILogger<PimMigrationAdoptionService> logger)
    {
        _db = db;
        _logger = logger;
    }

    public static bool NeedsBaselineAdoption(bool usersTableExists, bool historyTableExists)
        => usersTableExists && !historyTableExists;

    public async Task AdoptExistingSchemaAsync(CancellationToken ct = default)
    {
        var usersTableExists = await TableExistsAsync("public", "users", ct);
        var historyTableExists = await TableExistsAsync("public", "__EFMigrationsHistory", ct);

        if (!NeedsBaselineAdoption(usersTableExists, historyTableExists))
        {
            return;
        }

        _logger.LogWarning("Adopting existing database schema as EF migration baseline {MigrationId}", BaselineMigrationId);

        await _db.Database.ExecuteSqlRawAsync("""
CREATE TABLE IF NOT EXISTS "__EFMigrationsHistory" (
    "MigrationId" character varying(150) NOT NULL,
    "ProductVersion" character varying(32) NOT NULL,
    CONSTRAINT "PK___EFMigrationsHistory" PRIMARY KEY ("MigrationId")
);
INSERT INTO "__EFMigrationsHistory" ("MigrationId", "ProductVersion")
VALUES ('20260524000000_BaselineExistingSchema', '8.0.11')
ON CONFLICT ("MigrationId") DO NOTHING;
""", ct);
    }

    private async Task<bool> TableExistsAsync(string schema, string table, CancellationToken ct)
    {
        var connection = _db.Database.GetDbConnection();

        if (connection.State != System.Data.ConnectionState.Open)
        {
            await connection.OpenAsync(ct);
        }

        await using var command = connection.CreateCommand();
        command.CommandText = """
SELECT EXISTS (
    SELECT 1
    FROM information_schema.tables
    WHERE table_schema = @schema AND table_name = @table
);
""";

        var schemaParameter = command.CreateParameter();
        schemaParameter.ParameterName = "schema";
        schemaParameter.Value = schema;
        command.Parameters.Add(schemaParameter);

        var tableParameter = command.CreateParameter();
        tableParameter.ParameterName = "table";
        tableParameter.Value = table;
        command.Parameters.Add(tableParameter);

        var result = await command.ExecuteScalarAsync(ct);
        return result is bool exists && exists;
    }
}
```

- [ ] **Step 6: Register the adoption service**

Modify `src/Pim.Infrastructure/Extensions/ServiceCollectionExtensions.cs` inside `AddPimInfrastructure`:

```csharp
        services.AddScoped<PimMigrationAdoptionService>();
```

- [ ] **Step 7: Replace `EnsureCreated()` in API startup**

Modify the database block in `src/Pim.Api/Program.cs`:

```csharp
// Apply database migrations. Existing EnsureCreated databases are adopted before Migrate().
using (var scope = app.Services.CreateScope())
{
    var adoption = scope.ServiceProvider.GetRequiredService<Pim.Infrastructure.Data.PimMigrationAdoptionService>();
    await adoption.AdoptExistingSchemaAsync();

    var db = scope.ServiceProvider.GetRequiredService<Pim.Infrastructure.Data.PimDbContext>();
    await db.Database.MigrateAsync();
}
```

Add this using at the top:

```csharp
using Microsoft.EntityFrameworkCore;
```

- [ ] **Step 8: Generate the baseline migration**

Run:

```powershell
dotnet ef migrations add BaselineExistingSchema --project src\Pim.Infrastructure --startup-project src\Pim.Api --context PimDbContext --output-dir Data\Migrations
```

Rename the generated migration class and file prefix so the migration id is exactly:

```text
20260524000000_BaselineExistingSchema
```

Expected: `src/Pim.Infrastructure/Data/Migrations/20260524000000_BaselineExistingSchema.cs` exists and snapshot exists.

- [ ] **Step 9: Run adoption tests**

Run:

```powershell
dotnet test tests\Pim.UnitTests\Pim.UnitTests.csproj --filter PimMigrationAdoptionServiceTests
```

Expected: PASS.

- [ ] **Step 10: Run API build**

Run:

```powershell
dotnet build src\Pim.Api\Pim.Api.csproj
```

Expected: PASS.

- [ ] **Step 11: Commit**

```powershell
git add src\Pim.Infrastructure\Pim.Infrastructure.csproj src\Pim.Infrastructure\Data src\Pim.Api\Program.cs tests\Pim.UnitTests\Pim.UnitTests.csproj tests\Pim.UnitTests\Operations\PimMigrationAdoptionServiceTests.cs
git commit -m "feat: add ef migration baseline adoption"
```

---

## Task 3: Stage 0 Persistence Tables

**Files:**
- Create: `src/Pim.Infrastructure/Data/Entities/AuditLogEntity.cs`
- Create: `src/Pim.Infrastructure/Data/Entities/OperationConfirmationEntity.cs`
- Create: `src/Pim.Infrastructure/Data/Entities/DaemonHeartbeatEntity.cs`
- Modify: `src/Pim.Infrastructure/Data/PimDbContext.cs`
- Create: `tests/Pim.UnitTests/Operations/Stage0PersistenceTests.cs`
- Generate: `src/Pim.Infrastructure/Data/Migrations/*_Stage0OperationsTables.cs`

- [ ] **Step 1: Write failing persistence tests**

Create `tests/Pim.UnitTests/Operations/Stage0PersistenceTests.cs`:

```csharp
using Microsoft.EntityFrameworkCore;
using Pim.Core.Operations;
using Pim.Infrastructure.Data;
using Pim.Infrastructure.Data.Entities;

namespace Pim.UnitTests.Operations;

public class Stage0PersistenceTests
{
    [Fact]
    public async Task PimDbContext_SavesStage0Entities()
    {
        var options = new DbContextOptionsBuilder<PimDbContext>()
            .UseInMemoryDatabase(Guid.NewGuid().ToString())
            .Options;

        await using var db = new PimDbContext(options);

        db.AuditLogs.Add(new AuditLogEntity
        {
            ActorType = AuditActorType.User.ToString(),
            Action = "calendar.event.create",
            ResourceType = "calendar_event",
            Source = "web",
            Result = AuditResult.Success.ToString(),
            MetadataJson = "{}",
            CreatedAt = DateTimeOffset.UtcNow
        });

        db.OperationConfirmations.Add(new OperationConfirmationEntity
        {
            OperationType = "outlook.write",
            Summary = "Write event to Outlook",
            RiskLevel = OperationRiskLevel.High.ToString(),
            Source = "web",
            PayloadJson = "{}",
            PreviewJson = "{}",
            Status = OperationConfirmationStatus.Pending.ToString(),
            ExpiresAt = DateTimeOffset.UtcNow.AddMinutes(15),
            CreatedAt = DateTimeOffset.UtcNow
        });

        db.DaemonHeartbeats.Add(new DaemonHeartbeatEntity
        {
            DeviceId = "pc-main",
            DaemonKind = "windows",
            Version = "1.0.0",
            ServerUrl = "http://127.0.0.1:5858",
            ActivityWatchState = DaemonSourceState.Available.ToString(),
            KeyStatsState = DaemonSourceState.Available.ToString(),
            StatusJson = "{}",
            ReceivedAt = DateTimeOffset.UtcNow
        });

        await db.SaveChangesAsync();

        Assert.Equal(1, await db.AuditLogs.CountAsync());
        Assert.Equal(1, await db.OperationConfirmations.CountAsync());
        Assert.Equal(1, await db.DaemonHeartbeats.CountAsync());
    }
}
```

- [ ] **Step 2: Run the test to verify it fails**

Run:

```powershell
dotnet test tests\Pim.UnitTests\Pim.UnitTests.csproj --filter Stage0PersistenceTests
```

Expected: FAIL because entities and DbSets do not exist.

- [ ] **Step 3: Create `AuditLogEntity`**

Create `src/Pim.Infrastructure/Data/Entities/AuditLogEntity.cs`:

```csharp
using System.ComponentModel.DataAnnotations;
using System.ComponentModel.DataAnnotations.Schema;

namespace Pim.Infrastructure.Data.Entities;

[Table("audit_logs")]
public sealed class AuditLogEntity
{
    [Key]
    [Column("id")]
    public Guid Id { get; set; } = Guid.NewGuid();

    [Column("user_id")]
    public Guid? UserId { get; set; }

    [Column("actor_type")]
    [MaxLength(32)]
    public string ActorType { get; set; } = string.Empty;

    [Column("action")]
    [MaxLength(128)]
    public string Action { get; set; } = string.Empty;

    [Column("resource_type")]
    [MaxLength(128)]
    public string ResourceType { get; set; } = string.Empty;

    [Column("resource_id")]
    [MaxLength(128)]
    public string? ResourceId { get; set; }

    [Column("source")]
    [MaxLength(64)]
    public string Source { get; set; } = string.Empty;

    [Column("result")]
    [MaxLength(32)]
    public string Result { get; set; } = string.Empty;

    [Column("ip_address")]
    [MaxLength(64)]
    public string? IpAddress { get; set; }

    [Column("user_agent")]
    [MaxLength(512)]
    public string? UserAgent { get; set; }

    [Column("correlation_id")]
    [MaxLength(128)]
    public string? CorrelationId { get; set; }

    [Column("metadata_json", TypeName = "jsonb")]
    public string MetadataJson { get; set; } = "{}";

    [Column("error_code")]
    public int? ErrorCode { get; set; }

    [Column("error_message")]
    public string? ErrorMessage { get; set; }

    [Column("created_at")]
    public DateTimeOffset CreatedAt { get; set; } = DateTimeOffset.UtcNow;
}
```

- [ ] **Step 4: Create `OperationConfirmationEntity`**

Create `src/Pim.Infrastructure/Data/Entities/OperationConfirmationEntity.cs`:

```csharp
using System.ComponentModel.DataAnnotations;
using System.ComponentModel.DataAnnotations.Schema;

namespace Pim.Infrastructure.Data.Entities;

[Table("operation_confirmations")]
public sealed class OperationConfirmationEntity
{
    [Key]
    [Column("id")]
    public Guid Id { get; set; } = Guid.NewGuid();

    [Column("requested_by_user_id")]
    public Guid? RequestedByUserId { get; set; }

    [Column("operation_type")]
    [MaxLength(128)]
    public string OperationType { get; set; } = string.Empty;

    [Column("summary")]
    public string Summary { get; set; } = string.Empty;

    [Column("risk_level")]
    [MaxLength(32)]
    public string RiskLevel { get; set; } = string.Empty;

    [Column("source")]
    [MaxLength(64)]
    public string Source { get; set; } = string.Empty;

    [Column("payload_json", TypeName = "jsonb")]
    public string PayloadJson { get; set; } = "{}";

    [Column("preview_json", TypeName = "jsonb")]
    public string PreviewJson { get; set; } = "{}";

    [Column("status")]
    [MaxLength(32)]
    public string Status { get; set; } = "Pending";

    [Column("expires_at")]
    public DateTimeOffset ExpiresAt { get; set; }

    [Column("created_at")]
    public DateTimeOffset CreatedAt { get; set; } = DateTimeOffset.UtcNow;

    [Column("confirmed_at")]
    public DateTimeOffset? ConfirmedAt { get; set; }

    [Column("rejected_at")]
    public DateTimeOffset? RejectedAt { get; set; }

    [Column("executed_at")]
    public DateTimeOffset? ExecutedAt { get; set; }

    [Column("result_json", TypeName = "jsonb")]
    public string? ResultJson { get; set; }

    [Column("correlation_id")]
    [MaxLength(128)]
    public string? CorrelationId { get; set; }
}
```

- [ ] **Step 5: Create `DaemonHeartbeatEntity`**

Create `src/Pim.Infrastructure/Data/Entities/DaemonHeartbeatEntity.cs`:

```csharp
using System.ComponentModel.DataAnnotations;
using System.ComponentModel.DataAnnotations.Schema;

namespace Pim.Infrastructure.Data.Entities;

[Table("daemon_heartbeats")]
public sealed class DaemonHeartbeatEntity
{
    [Key]
    [Column("id")]
    public Guid Id { get; set; } = Guid.NewGuid();

    [Column("device_id")]
    [MaxLength(128)]
    public string DeviceId { get; set; } = string.Empty;

    [Column("daemon_kind")]
    [MaxLength(32)]
    public string DaemonKind { get; set; } = "windows";

    [Column("version")]
    [MaxLength(64)]
    public string Version { get; set; } = string.Empty;

    [Column("server_url")]
    [MaxLength(512)]
    public string ServerUrl { get; set; } = string.Empty;

    [Column("last_successful_upload_at")]
    public DateTimeOffset? LastSuccessfulUploadAt { get; set; }

    [Column("last_attempted_upload_at")]
    public DateTimeOffset? LastAttemptedUploadAt { get; set; }

    [Column("last_error")]
    public string? LastError { get; set; }

    [Column("upload_queue_count")]
    public int? UploadQueueCount { get; set; }

    [Column("activity_watch_state")]
    [MaxLength(32)]
    public string ActivityWatchState { get; set; } = "Unknown";

    [Column("key_stats_state")]
    [MaxLength(32)]
    public string KeyStatsState { get; set; } = "Unknown";

    [Column("collection_paused")]
    public bool CollectionPaused { get; set; }

    [Column("status_json", TypeName = "jsonb")]
    public string StatusJson { get; set; } = "{}";

    [Column("received_at")]
    public DateTimeOffset ReceivedAt { get; set; } = DateTimeOffset.UtcNow;
}
```

- [ ] **Step 6: Register DbSets and indexes**

Modify `src/Pim.Infrastructure/Data/PimDbContext.cs`:

```csharp
    public DbSet<AuditLogEntity> AuditLogs => Set<AuditLogEntity>();
    public DbSet<OperationConfirmationEntity> OperationConfirmations => Set<OperationConfirmationEntity>();
    public DbSet<DaemonHeartbeatEntity> DaemonHeartbeats => Set<DaemonHeartbeatEntity>();
```

Add this configuration inside `OnModelCreating` before module assembly configuration:

```csharp
        modelBuilder.Entity<AuditLogEntity>(e =>
        {
            e.HasIndex(a => a.UserId);
            e.HasIndex(a => a.Action);
            e.HasIndex(a => a.ResourceType);
            e.HasIndex(a => a.CorrelationId);
            e.HasIndex(a => a.CreatedAt);
        });

        modelBuilder.Entity<OperationConfirmationEntity>(e =>
        {
            e.HasIndex(o => o.RequestedByUserId);
            e.HasIndex(o => o.OperationType);
            e.HasIndex(o => o.Status);
            e.HasIndex(o => o.ExpiresAt);
        });

        modelBuilder.Entity<DaemonHeartbeatEntity>(e =>
        {
            e.HasIndex(d => new { d.DeviceId, d.DaemonKind }).IsUnique();
            e.HasIndex(d => d.ReceivedAt);
        });
```

- [ ] **Step 7: Generate Stage 0 table migration**

Run:

```powershell
dotnet ef migrations add Stage0OperationsTables --project src\Pim.Infrastructure --startup-project src\Pim.Api --context PimDbContext --output-dir Data\Migrations
```

Expected: a new migration creates `audit_logs`, `operation_confirmations`, and `daemon_heartbeats` only. If it attempts to recreate baseline tables, stop and fix the baseline snapshot before continuing.

- [ ] **Step 8: Run persistence tests**

Run:

```powershell
dotnet test tests\Pim.UnitTests\Pim.UnitTests.csproj --filter Stage0PersistenceTests
```

Expected: PASS.

- [ ] **Step 9: Run API build**

Run:

```powershell
dotnet build src\Pim.Api\Pim.Api.csproj
```

Expected: PASS.

- [ ] **Step 10: Commit**

```powershell
git add src\Pim.Infrastructure\Data tests\Pim.UnitTests\Operations\Stage0PersistenceTests.cs
git commit -m "feat: add stage 0 persistence tables"
```

---

## Task 4: Audit And Confirmation Services

**Files:**
- Create: `src/Pim.Infrastructure/Operations/AuditLogService.cs`
- Create: `src/Pim.Infrastructure/Operations/OperationConfirmationService.cs`
- Modify: `src/Pim.Infrastructure/Extensions/ServiceCollectionExtensions.cs`
- Create: `tests/Pim.UnitTests/Operations/AuditAndConfirmationServiceTests.cs`

- [ ] **Step 1: Write failing service tests**

Create `tests/Pim.UnitTests/Operations/AuditAndConfirmationServiceTests.cs`:

```csharp
using Microsoft.EntityFrameworkCore;
using Pim.Core.Operations;
using Pim.Infrastructure.Data;
using Pim.Infrastructure.Operations;

namespace Pim.UnitTests.Operations;

public class AuditAndConfirmationServiceTests
{
    private static PimDbContext CreateDb()
    {
        var options = new DbContextOptionsBuilder<PimDbContext>()
            .UseInMemoryDatabase(Guid.NewGuid().ToString())
            .Options;
        return new PimDbContext(options);
    }

    [Fact]
    public async Task AuditLogService_RecordsAudit()
    {
        await using var db = CreateDb();
        var service = new AuditLogService(db);

        var audit = await service.RecordAsync(new CreateAuditLogRequest(
            Guid.Parse("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa"),
            AuditActorType.User,
            "calendar.event.delete",
            "calendar_event",
            "event-1",
            "web",
            AuditResult.Success,
            "127.0.0.1",
            "UnitTest",
            "corr-1",
            new Dictionary<string, string> { ["reason"] = "test" },
            null,
            null));

        Assert.NotEqual(Guid.Empty, audit.Id);
        Assert.Equal("calendar.event.delete", audit.Action);
        Assert.Equal(1, await db.AuditLogs.CountAsync());
    }

    [Fact]
    public async Task OperationConfirmationService_HandlesLifecycle()
    {
        await using var db = CreateDb();
        var service = new OperationConfirmationService(db);

        var created = await service.CreateAsync(new CreateOperationConfirmationRequest(
            Guid.Parse("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb"),
            "outlook.write",
            "Write event to Outlook",
            OperationRiskLevel.High,
            "web",
            "{}",
            "{\"count\":1}",
            DateTimeOffset.UtcNow.AddMinutes(30),
            "corr-2"));

        var confirmed = await service.ConfirmAsync(created.Id, created.RequestedByUserId);
        var executed = await service.MarkExecutedAsync(created.Id, "{\"ok\":true}");

        Assert.Equal(OperationConfirmationStatus.Pending, created.Status);
        Assert.Equal(OperationConfirmationStatus.Confirmed, confirmed.Status);
        Assert.Equal(OperationConfirmationStatus.Executed, executed.Status);
        Assert.NotNull(executed.ExecutedAt);
    }

    [Fact]
    public async Task OperationConfirmationService_ExpiresOldPendingRecords()
    {
        await using var db = CreateDb();
        var service = new OperationConfirmationService(db);

        await service.CreateAsync(new CreateOperationConfirmationRequest(
            null,
            "file.move",
            "Move files",
            OperationRiskLevel.High,
            "job",
            "{}",
            "{}",
            DateTimeOffset.UtcNow.AddMinutes(-1),
            "corr-3"));

        var expired = await service.ExpireOldAsync(DateTimeOffset.UtcNow);

        Assert.Equal(1, expired);
        Assert.Equal(OperationConfirmationStatus.Expired.ToString(), (await db.OperationConfirmations.SingleAsync()).Status);
    }
}
```

- [ ] **Step 2: Run tests to verify they fail**

Run:

```powershell
dotnet test tests\Pim.UnitTests\Pim.UnitTests.csproj --filter AuditAndConfirmationServiceTests
```

Expected: FAIL because services do not exist.

- [ ] **Step 3: Implement `AuditLogService`**

Create `src/Pim.Infrastructure/Operations/AuditLogService.cs`:

```csharp
using System.Text.Json;
using Pim.Core.Operations;
using Pim.Infrastructure.Data;
using Pim.Infrastructure.Data.Entities;

namespace Pim.Infrastructure.Operations;

public sealed class AuditLogService : IAuditLogService
{
    private readonly PimDbContext _db;

    public AuditLogService(PimDbContext db)
    {
        _db = db;
    }

    public async Task<AuditLogDto> RecordAsync(CreateAuditLogRequest request, CancellationToken ct = default)
    {
        var entity = new AuditLogEntity
        {
            UserId = request.UserId,
            ActorType = request.ActorType.ToString(),
            Action = request.Action,
            ResourceType = request.ResourceType,
            ResourceId = request.ResourceId,
            Source = request.Source,
            Result = request.Result.ToString(),
            IpAddress = request.IpAddress,
            UserAgent = request.UserAgent,
            CorrelationId = request.CorrelationId,
            MetadataJson = JsonSerializer.Serialize(request.Metadata ?? new Dictionary<string, string>()),
            ErrorCode = request.ErrorCode,
            ErrorMessage = request.ErrorMessage,
            CreatedAt = DateTimeOffset.UtcNow
        };

        _db.AuditLogs.Add(entity);
        await _db.SaveChangesAsync(ct);

        return new AuditLogDto(
            entity.Id,
            entity.UserId,
            Enum.Parse<AuditActorType>(entity.ActorType),
            entity.Action,
            entity.ResourceType,
            entity.ResourceId,
            entity.Source,
            Enum.Parse<AuditResult>(entity.Result),
            entity.CorrelationId,
            entity.CreatedAt);
    }
}
```

- [ ] **Step 4: Implement `OperationConfirmationService`**

Create `src/Pim.Infrastructure/Operations/OperationConfirmationService.cs`:

```csharp
using Microsoft.EntityFrameworkCore;
using Pim.Core.Exceptions;
using Pim.Core.Operations;
using Pim.Infrastructure.Data;
using Pim.Infrastructure.Data.Entities;

namespace Pim.Infrastructure.Operations;

public sealed class OperationConfirmationService : IOperationConfirmationService
{
    private readonly PimDbContext _db;

    public OperationConfirmationService(PimDbContext db)
    {
        _db = db;
    }

    public async Task<OperationConfirmationDto> CreateAsync(CreateOperationConfirmationRequest request, CancellationToken ct = default)
    {
        var entity = new OperationConfirmationEntity
        {
            RequestedByUserId = request.RequestedByUserId,
            OperationType = request.OperationType,
            Summary = request.Summary,
            RiskLevel = request.RiskLevel.ToString(),
            Source = request.Source,
            PayloadJson = request.PayloadJson,
            PreviewJson = request.PreviewJson,
            Status = OperationConfirmationStatus.Pending.ToString(),
            ExpiresAt = request.ExpiresAt,
            CreatedAt = DateTimeOffset.UtcNow,
            CorrelationId = request.CorrelationId
        };

        _db.OperationConfirmations.Add(entity);
        await _db.SaveChangesAsync(ct);
        return Map(entity);
    }

    public async Task<OperationConfirmationDto?> GetAsync(Guid id, CancellationToken ct = default)
    {
        var entity = await _db.OperationConfirmations.FindAsync(new object[] { id }, ct);
        return entity is null ? null : Map(entity);
    }

    public async Task<IReadOnlyList<OperationConfirmationDto>> ListPendingAsync(CancellationToken ct = default)
    {
        var rows = await _db.OperationConfirmations
            .Where(c => c.Status == OperationConfirmationStatus.Pending.ToString())
            .OrderBy(c => c.ExpiresAt)
            .ToListAsync(ct);

        return rows.Select(Map).ToList();
    }

    public async Task<OperationConfirmationDto> ConfirmAsync(Guid id, Guid? userId, CancellationToken ct = default)
    {
        var entity = await LoadPendingAsync(id, ct);
        entity.Status = OperationConfirmationStatus.Confirmed.ToString();
        entity.ConfirmedAt = DateTimeOffset.UtcNow;
        await _db.SaveChangesAsync(ct);
        return Map(entity);
    }

    public async Task<OperationConfirmationDto> RejectAsync(Guid id, Guid? userId, CancellationToken ct = default)
    {
        var entity = await LoadPendingAsync(id, ct);
        entity.Status = OperationConfirmationStatus.Rejected.ToString();
        entity.RejectedAt = DateTimeOffset.UtcNow;
        await _db.SaveChangesAsync(ct);
        return Map(entity);
    }

    public async Task<OperationConfirmationDto> MarkExecutedAsync(Guid id, string resultJson, CancellationToken ct = default)
    {
        var entity = await _db.OperationConfirmations.FindAsync(new object[] { id }, ct)
            ?? throw new DomainException(03001, "Confirmation not found");

        if (entity.Status != OperationConfirmationStatus.Confirmed.ToString())
        {
            throw new DomainException(03002, "Only confirmed operations can be executed");
        }

        entity.Status = OperationConfirmationStatus.Executed.ToString();
        entity.ExecutedAt = DateTimeOffset.UtcNow;
        entity.ResultJson = resultJson;
        await _db.SaveChangesAsync(ct);
        return Map(entity);
    }

    public async Task<int> ExpireOldAsync(DateTimeOffset now, CancellationToken ct = default)
    {
        var rows = await _db.OperationConfirmations
            .Where(c => c.Status == OperationConfirmationStatus.Pending.ToString() && c.ExpiresAt <= now)
            .ToListAsync(ct);

        foreach (var row in rows)
        {
            row.Status = OperationConfirmationStatus.Expired.ToString();
        }

        await _db.SaveChangesAsync(ct);
        return rows.Count;
    }

    private async Task<OperationConfirmationEntity> LoadPendingAsync(Guid id, CancellationToken ct)
    {
        var entity = await _db.OperationConfirmations.FindAsync(new object[] { id }, ct)
            ?? throw new DomainException(03001, "Confirmation not found");

        if (entity.Status != OperationConfirmationStatus.Pending.ToString())
        {
            throw new DomainException(03003, "Confirmation is not pending");
        }

        if (entity.ExpiresAt <= DateTimeOffset.UtcNow)
        {
            entity.Status = OperationConfirmationStatus.Expired.ToString();
            await _db.SaveChangesAsync(ct);
            throw new DomainException(03004, "Confirmation has expired");
        }

        return entity;
    }

    private static OperationConfirmationDto Map(OperationConfirmationEntity entity)
        => new(
            entity.Id,
            entity.RequestedByUserId,
            entity.OperationType,
            entity.Summary,
            Enum.Parse<OperationRiskLevel>(entity.RiskLevel),
            entity.Source,
            entity.PayloadJson,
            entity.PreviewJson,
            Enum.Parse<OperationConfirmationStatus>(entity.Status),
            entity.ExpiresAt,
            entity.CreatedAt,
            entity.ConfirmedAt,
            entity.ExecutedAt,
            entity.ResultJson,
            entity.CorrelationId);
}
```

- [ ] **Step 5: Register services**

Modify `src/Pim.Infrastructure/Extensions/ServiceCollectionExtensions.cs`:

```csharp
        services.AddScoped<IAuditLogService, AuditLogService>();
        services.AddScoped<IOperationConfirmationService, OperationConfirmationService>();
```

Add using:

```csharp
using Pim.Core.Operations;
using Pim.Infrastructure.Operations;
```

- [ ] **Step 6: Run service tests**

Run:

```powershell
dotnet test tests\Pim.UnitTests\Pim.UnitTests.csproj --filter AuditAndConfirmationServiceTests
```

Expected: PASS.

- [ ] **Step 7: Commit**

```powershell
git add src\Pim.Infrastructure\Operations src\Pim.Infrastructure\Extensions\ServiceCollectionExtensions.cs tests\Pim.UnitTests\Operations\AuditAndConfirmationServiceTests.cs
git commit -m "feat: add audit and confirmation services"
```

---

## Task 5: Daemon Heartbeat And System Status Services

**Files:**
- Create: `src/Pim.Infrastructure/Operations/DaemonHeartbeatService.cs`
- Create: `src/Pim.Infrastructure/Operations/SystemStatusService.cs`
- Modify: `src/Pim.Infrastructure/Extensions/ServiceCollectionExtensions.cs`
- Create: `tests/Pim.UnitTests/Operations/DaemonHeartbeatServiceTests.cs`
- Create: `tests/Pim.UnitTests/Operations/SystemStatusServiceTests.cs`

- [ ] **Step 1: Write failing daemon heartbeat tests**

Create `tests/Pim.UnitTests/Operations/DaemonHeartbeatServiceTests.cs`:

```csharp
using Microsoft.EntityFrameworkCore;
using Pim.Core.Operations;
using Pim.Infrastructure.Data;
using Pim.Infrastructure.Operations;

namespace Pim.UnitTests.Operations;

public class DaemonHeartbeatServiceTests
{
    [Fact]
    public async Task UpsertAsync_ReplacesExistingDeviceHeartbeat()
    {
        var options = new DbContextOptionsBuilder<PimDbContext>()
            .UseInMemoryDatabase(Guid.NewGuid().ToString())
            .Options;

        await using var db = new PimDbContext(options);
        var service = new DaemonHeartbeatService(db);

        await service.UpsertAsync(new DaemonHeartbeatRequest(
            "pc-main",
            "windows",
            "1.0.0",
            "http://127.0.0.1:5858",
            null,
            null,
            null,
            0,
            DaemonSourceState.Available,
            DaemonSourceState.Available,
            false,
            "{}"));

        await service.UpsertAsync(new DaemonHeartbeatRequest(
            "pc-main",
            "windows",
            "1.0.1",
            "http://127.0.0.1:5858",
            DateTimeOffset.UtcNow,
            DateTimeOffset.UtcNow,
            null,
            2,
            DaemonSourceState.Available,
            DaemonSourceState.Unavailable,
            false,
            "{\"note\":\"second\"}"));

        var latest = await service.GetLatestWindowsAsync();

        Assert.Equal(1, await db.DaemonHeartbeats.CountAsync());
        Assert.Equal("1.0.1", latest!.Version);
        Assert.Equal(DaemonSourceState.Unavailable, latest.KeyStatsState);
    }
}
```

- [ ] **Step 2: Write failing status aggregation tests**

Create `tests/Pim.UnitTests/Operations/SystemStatusServiceTests.cs`:

```csharp
using Microsoft.EntityFrameworkCore;
using Pim.Core.Operations;
using Pim.Infrastructure.Data;
using Pim.Infrastructure.Data.Entities;
using Pim.Infrastructure.Operations;

namespace Pim.UnitTests.Operations;

public class SystemStatusServiceTests
{
    [Fact]
    public async Task GetSummaryAsync_ReturnsWarning_WhenDaemonHeartbeatIsOld()
    {
        var options = new DbContextOptionsBuilder<PimDbContext>()
            .UseInMemoryDatabase(Guid.NewGuid().ToString())
            .Options;

        await using var db = new PimDbContext(options);
        db.DaemonHeartbeats.Add(new DaemonHeartbeatEntity
        {
            DeviceId = "pc-main",
            DaemonKind = "windows",
            Version = "1.0.0",
            ServerUrl = "http://127.0.0.1:5858",
            ActivityWatchState = DaemonSourceState.Available.ToString(),
            KeyStatsState = DaemonSourceState.Available.ToString(),
            StatusJson = "{}",
            ReceivedAt = DateTimeOffset.UtcNow.AddMinutes(-20)
        });
        await db.SaveChangesAsync();

        var service = new SystemStatusService(db, new FakeBackgroundJobStatusService());
        var summary = await service.GetSummaryAsync();

        Assert.Equal(PimHealthStatus.Warning, summary.Status);
        Assert.Equal("有警告", summary.Label);
    }

    private sealed class FakeBackgroundJobStatusService : IBackgroundJobStatusService
    {
        public Task<BackgroundJobSummaryDto> GetSummaryAsync(CancellationToken ct = default)
            => Task.FromResult(new BackgroundJobSummaryDto(PimHealthStatus.Healthy, 0, 0, 0, 0, DateTimeOffset.UtcNow, "Background jobs healthy."));
    }
}
```

- [ ] **Step 3: Run tests to verify they fail**

Run:

```powershell
dotnet test tests\Pim.UnitTests\Pim.UnitTests.csproj --filter "DaemonHeartbeatServiceTests|SystemStatusServiceTests"
```

Expected: FAIL because services do not exist.

- [ ] **Step 4: Implement `DaemonHeartbeatService`**

Create `src/Pim.Infrastructure/Operations/DaemonHeartbeatService.cs`:

```csharp
using Microsoft.EntityFrameworkCore;
using Pim.Core.Operations;
using Pim.Infrastructure.Data;
using Pim.Infrastructure.Data.Entities;

namespace Pim.Infrastructure.Operations;

public sealed class DaemonHeartbeatService : IDaemonHeartbeatService
{
    private readonly PimDbContext _db;

    public DaemonHeartbeatService(PimDbContext db)
    {
        _db = db;
    }

    public async Task<DaemonHeartbeatDto> UpsertAsync(DaemonHeartbeatRequest request, CancellationToken ct = default)
    {
        var entity = await _db.DaemonHeartbeats
            .FirstOrDefaultAsync(d => d.DeviceId == request.DeviceId && d.DaemonKind == request.DaemonKind, ct);

        if (entity is null)
        {
            entity = new DaemonHeartbeatEntity
            {
                DeviceId = request.DeviceId,
                DaemonKind = request.DaemonKind
            };
            _db.DaemonHeartbeats.Add(entity);
        }

        entity.Version = request.Version;
        entity.ServerUrl = request.ServerUrl;
        entity.LastSuccessfulUploadAt = request.LastSuccessfulUploadAt;
        entity.LastAttemptedUploadAt = request.LastAttemptedUploadAt;
        entity.LastError = request.LastError;
        entity.UploadQueueCount = request.UploadQueueCount;
        entity.ActivityWatchState = request.ActivityWatchState.ToString();
        entity.KeyStatsState = request.KeyStatsState.ToString();
        entity.CollectionPaused = request.CollectionPaused;
        entity.StatusJson = string.IsNullOrWhiteSpace(request.StatusJson) ? "{}" : request.StatusJson;
        entity.ReceivedAt = DateTimeOffset.UtcNow;

        await _db.SaveChangesAsync(ct);
        return Map(entity);
    }

    public async Task<DaemonHeartbeatDto?> GetLatestAsync(string deviceId, CancellationToken ct = default)
    {
        var entity = await _db.DaemonHeartbeats
            .Where(d => d.DeviceId == deviceId)
            .OrderByDescending(d => d.ReceivedAt)
            .FirstOrDefaultAsync(ct);
        return entity is null ? null : Map(entity);
    }

    public async Task<DaemonHeartbeatDto?> GetLatestWindowsAsync(CancellationToken ct = default)
    {
        var entity = await _db.DaemonHeartbeats
            .Where(d => d.DaemonKind == "windows")
            .OrderByDescending(d => d.ReceivedAt)
            .FirstOrDefaultAsync(ct);
        return entity is null ? null : Map(entity);
    }

    private static DaemonHeartbeatDto Map(DaemonHeartbeatEntity entity)
        => new(
            entity.DeviceId,
            entity.DaemonKind,
            entity.Version,
            entity.ServerUrl,
            entity.LastSuccessfulUploadAt,
            entity.LastAttemptedUploadAt,
            entity.LastError,
            entity.UploadQueueCount,
            Enum.Parse<DaemonSourceState>(entity.ActivityWatchState),
            Enum.Parse<DaemonSourceState>(entity.KeyStatsState),
            entity.CollectionPaused,
            entity.StatusJson,
            entity.ReceivedAt);
}
```

- [ ] **Step 5: Implement `SystemStatusService`**

Create `src/Pim.Infrastructure/Operations/SystemStatusService.cs`:

```csharp
using Microsoft.EntityFrameworkCore;
using Pim.Core.Operations;
using Pim.Infrastructure.Data;

namespace Pim.Infrastructure.Operations;

public sealed class SystemStatusService : ISystemStatusService
{
    private static readonly TimeSpan DaemonWarningAge = TimeSpan.FromMinutes(10);
    private static readonly TimeSpan DaemonCriticalAge = TimeSpan.FromMinutes(60);

    private readonly PimDbContext _db;
    private readonly IBackgroundJobStatusService _backgroundJobs;

    public SystemStatusService(PimDbContext db, IBackgroundJobStatusService backgroundJobs)
    {
        _db = db;
        _backgroundJobs = backgroundJobs;
    }

    public async Task<SystemStatusSummaryDto> GetSummaryAsync(CancellationToken ct = default)
    {
        var detail = await GetDetailAsync(ct);
        return detail.Summary;
    }

    public async Task<SystemStatusDetailDto> GetDetailAsync(CancellationToken ct = default)
    {
        var now = DateTimeOffset.UtcNow;
        var components = new List<StatusComponentDto>
        {
            new("api", "API", StatusComponentKind.Api, PimHealthStatus.Healthy, "API process is running.", now, new Dictionary<string, string>())
        };

        components.Add(await BuildDatabaseComponentAsync(now, ct));
        components.Add(await BuildDaemonComponentAsync(now, ct));

        var background = await _backgroundJobs.GetSummaryAsync(ct);
        components.Add(new StatusComponentDto(
            "background-jobs",
            "后台任务",
            StatusComponentKind.BackgroundJobs,
            background.Status,
            background.Message,
            background.CheckedAt,
            new Dictionary<string, string>
            {
                ["processing"] = background.Processing.ToString(),
                ["enqueued"] = background.Enqueued.ToString(),
                ["scheduled"] = background.Scheduled.ToString(),
                ["failed"] = background.Failed.ToString()
            }));

        var status = components.Max(c => c.Status);
        var nextSteps = components
            .Where(c => c.Status is PimHealthStatus.Warning or PimHealthStatus.Critical)
            .Select(c => c.Message)
            .ToList();

        return new SystemStatusDetailDto(
            new SystemStatusSummaryDto(status, Label(status), Message(status), now),
            components,
            nextSteps);
    }

    private async Task<StatusComponentDto> BuildDatabaseComponentAsync(DateTimeOffset now, CancellationToken ct)
    {
        try
        {
            if (_db.Database.ProviderName == "Microsoft.EntityFrameworkCore.InMemory")
            {
                return new StatusComponentDto("database", "数据库", StatusComponentKind.Database, PimHealthStatus.Healthy, "Database is reachable.", now, new Dictionary<string, string>());
            }

            await _db.Database.ExecuteSqlRawAsync("SELECT 1", ct);
            return new StatusComponentDto("database", "数据库", StatusComponentKind.Database, PimHealthStatus.Healthy, "Database is reachable.", now, new Dictionary<string, string>());
        }
        catch (Exception ex)
        {
            return new StatusComponentDto("database", "数据库", StatusComponentKind.Database, PimHealthStatus.Critical, "Database is unavailable.", now, new Dictionary<string, string> { ["error"] = ex.Message });
        }
    }

    private async Task<StatusComponentDto> BuildDaemonComponentAsync(DateTimeOffset now, CancellationToken ct)
    {
        var daemon = await _db.DaemonHeartbeats
            .Where(d => d.DaemonKind == "windows")
            .OrderByDescending(d => d.ReceivedAt)
            .FirstOrDefaultAsync(ct);

        if (daemon is null)
        {
            return new StatusComponentDto("windows-daemon", "Windows daemon", StatusComponentKind.Daemon, PimHealthStatus.Unknown, "Windows daemon has not reported yet.", now, new Dictionary<string, string>());
        }

        var age = now - daemon.ReceivedAt;
        var status = age >= DaemonCriticalAge
            ? PimHealthStatus.Critical
            : age >= DaemonWarningAge
                ? PimHealthStatus.Warning
                : PimHealthStatus.Healthy;

        var message = status switch
        {
            PimHealthStatus.Critical => "Windows daemon heartbeat is stale.",
            PimHealthStatus.Warning => "Windows daemon has not reported recently.",
            _ => "Windows daemon is reporting."
        };

        return new StatusComponentDto(
            "windows-daemon",
            "Windows daemon",
            StatusComponentKind.Daemon,
            status,
            message,
            now,
            new Dictionary<string, string>
            {
                ["deviceId"] = daemon.DeviceId,
                ["version"] = daemon.Version,
                ["receivedAt"] = daemon.ReceivedAt.ToString("O"),
                ["activityWatch"] = daemon.ActivityWatchState,
                ["keyStats"] = daemon.KeyStatsState
            });
    }

    private static string Label(PimHealthStatus status) => status switch
    {
        PimHealthStatus.Healthy => "正常",
        PimHealthStatus.Warning => "有警告",
        PimHealthStatus.Critical => "故障",
        _ => "未知"
    };

    private static string Message(PimHealthStatus status) => status switch
    {
        PimHealthStatus.Healthy => "All checked systems are healthy.",
        PimHealthStatus.Warning => "Some systems need attention.",
        PimHealthStatus.Critical => "One or more systems are failing.",
        _ => "System status is unknown."
    };
}
```

- [ ] **Step 6: Register services and temporary background status**

Modify `src/Pim.Infrastructure/Extensions/ServiceCollectionExtensions.cs`:

```csharp
        services.AddScoped<IDaemonHeartbeatService, DaemonHeartbeatService>();
        services.AddScoped<ISystemStatusService, SystemStatusService>();
```

Add this temporary class in `src/Pim.Infrastructure/Operations/NoopBackgroundJobStatusService.cs` because Hangfire is introduced in Task 7:

```csharp
using Pim.Core.Operations;

namespace Pim.Infrastructure.Operations;

public sealed class NoopBackgroundJobStatusService : IBackgroundJobStatusService
{
    public Task<BackgroundJobSummaryDto> GetSummaryAsync(CancellationToken ct = default)
        => Task.FromResult(new BackgroundJobSummaryDto(PimHealthStatus.Unknown, 0, 0, 0, 0, DateTimeOffset.UtcNow, "Background jobs are not configured yet."));
}
```

Register it:

```csharp
        services.AddScoped<IBackgroundJobStatusService, NoopBackgroundJobStatusService>();
```

Task 7 replaces this registration with Hangfire-backed status.

- [ ] **Step 7: Run tests**

Run:

```powershell
dotnet test tests\Pim.UnitTests\Pim.UnitTests.csproj --filter "DaemonHeartbeatServiceTests|SystemStatusServiceTests"
```

Expected: PASS.

- [ ] **Step 8: Commit**

```powershell
git add src\Pim.Infrastructure\Operations src\Pim.Infrastructure\Extensions\ServiceCollectionExtensions.cs tests\Pim.UnitTests\Operations\DaemonHeartbeatServiceTests.cs tests\Pim.UnitTests\Operations\SystemStatusServiceTests.cs
git commit -m "feat: add daemon heartbeat and system status services"
```

---

## Task 6: Status, Daemon, Audit, And Confirmation API Endpoints

**Files:**
- Create: `src/Pim.Api/Endpoints/StatusEndpoints.cs`
- Create: `src/Pim.Api/Endpoints/DaemonEndpoints.cs`
- Create: `src/Pim.Api/Endpoints/OperationsEndpoints.cs`
- Modify: `src/Pim.Api/Program.cs`

- [ ] **Step 1: Create status endpoints**

Create `src/Pim.Api/Endpoints/StatusEndpoints.cs`:

```csharp
using Pim.Core.Common;
using Pim.Core.Operations;

namespace Pim.Api.Endpoints;

public static class StatusEndpoints
{
    public static void MapStatusEndpoints(this IEndpointRouteBuilder endpoints)
    {
        var group = endpoints.MapGroup("/api/v1/status").RequireAuthorization();

        group.MapGet("/summary", async (
            ISystemStatusService status,
            CancellationToken ct) =>
        {
            var result = await status.GetSummaryAsync(ct);
            return Results.Ok(ApiResponse<SystemStatusSummaryDto>.Ok(result));
        });

        group.MapGet("/", async (
            ISystemStatusService status,
            CancellationToken ct) =>
        {
            var result = await status.GetDetailAsync(ct);
            return Results.Ok(ApiResponse<SystemStatusDetailDto>.Ok(result));
        });
    }
}
```

- [ ] **Step 2: Create daemon heartbeat endpoint**

Create `src/Pim.Api/Endpoints/DaemonEndpoints.cs`:

```csharp
using Pim.Core.Common;
using Pim.Core.Operations;

namespace Pim.Api.Endpoints;

public static class DaemonEndpoints
{
    public static void MapDaemonEndpoints(this IEndpointRouteBuilder endpoints)
    {
        var group = endpoints.MapGroup("/api/v1/daemon").RequireAuthorization();

        group.MapPost("/heartbeat", async (
            DaemonHeartbeatRequest request,
            IDaemonHeartbeatService heartbeats,
            CancellationToken ct) =>
        {
            var result = await heartbeats.UpsertAsync(request, ct);
            return Results.Ok(ApiResponse<DaemonHeartbeatDto>.Ok(result));
        });
    }
}
```

- [ ] **Step 3: Create operations endpoints**

Create `src/Pim.Api/Endpoints/OperationsEndpoints.cs`:

```csharp
using Pim.Core.Common;
using Pim.Core.Operations;

namespace Pim.Api.Endpoints;

public static class OperationsEndpoints
{
    public static void MapOperationsEndpoints(this IEndpointRouteBuilder endpoints)
    {
        var group = endpoints.MapGroup("/api/v1/operations").RequireAuthorization();

        group.MapGet("/confirmations/pending", async (
            IOperationConfirmationService confirmations,
            CancellationToken ct) =>
        {
            var result = await confirmations.ListPendingAsync(ct);
            return Results.Ok(ApiResponse<IReadOnlyList<OperationConfirmationDto>>.Ok(result));
        });

        group.MapPost("/confirmations/{id:guid}/confirm", async (
            Guid id,
            IOperationConfirmationService confirmations,
            CancellationToken ct) =>
        {
            var result = await confirmations.ConfirmAsync(id, null, ct);
            return Results.Ok(ApiResponse<OperationConfirmationDto>.Ok(result));
        });

        group.MapPost("/confirmations/{id:guid}/reject", async (
            Guid id,
            IOperationConfirmationService confirmations,
            CancellationToken ct) =>
        {
            var result = await confirmations.RejectAsync(id, null, ct);
            return Results.Ok(ApiResponse<OperationConfirmationDto>.Ok(result));
        });
    }
}
```

- [ ] **Step 4: Wire endpoints in Program**

Modify `src/Pim.Api/Program.cs` after search endpoint mapping:

```csharp
app.MapStatusEndpoints();
app.MapDaemonEndpoints();
app.MapOperationsEndpoints();
```

- [ ] **Step 5: Build API**

Run:

```powershell
dotnet build src\Pim.Api\Pim.Api.csproj
```

Expected: PASS.

- [ ] **Step 6: Commit**

```powershell
git add src\Pim.Api\Endpoints src\Pim.Api\Program.cs
git commit -m "feat: expose stage 0 operations endpoints"
```

---

## Task 7: Hangfire Background Jobs

**Files:**
- Modify: `src/Pim.Infrastructure/Pim.Infrastructure.csproj`
- Create: `src/Pim.Infrastructure/Operations/HangfireJobStatusService.cs`
- Create: `src/Pim.Infrastructure/Operations/Stage0DiagnosticJob.cs`
- Modify: `src/Pim.Infrastructure/Extensions/ServiceCollectionExtensions.cs`
- Create: `src/Pim.Api/Infrastructure/HangfireAuthorizationFilter.cs`
- Modify: `src/Pim.Api/Program.cs`
- Create: `tests/Pim.UnitTests/Operations/HangfireJobStatusServiceTests.cs`

- [ ] **Step 1: Add Hangfire packages**

Modify `src/Pim.Infrastructure/Pim.Infrastructure.csproj`:

```xml
    <PackageReference Include="Hangfire.AspNetCore" Version="1.8.23" />
    <PackageReference Include="Hangfire.PostgreSql" Version="1.21.1" />
```

- [ ] **Step 2: Write a focused status mapping test**

Create `tests/Pim.UnitTests/Operations/HangfireJobStatusServiceTests.cs`:

```csharp
using Pim.Core.Operations;
using Pim.Infrastructure.Operations;

namespace Pim.UnitTests.Operations;

public class HangfireJobStatusServiceTests
{
    [Theory]
    [InlineData(0, PimHealthStatus.Healthy)]
    [InlineData(1, PimHealthStatus.Warning)]
    public void MapFailedCountToStatus_ReturnsExpectedStatus(int failed, PimHealthStatus expected)
    {
        Assert.Equal(expected, HangfireJobStatusService.MapFailedCountToStatus(failed));
    }
}
```

- [ ] **Step 3: Run the test to verify it fails**

Run:

```powershell
dotnet test tests\Pim.UnitTests\Pim.UnitTests.csproj --filter HangfireJobStatusServiceTests
```

Expected: FAIL because `HangfireJobStatusService` does not exist.

- [ ] **Step 4: Implement diagnostic job**

Create `src/Pim.Infrastructure/Operations/Stage0DiagnosticJob.cs`:

```csharp
using Microsoft.Extensions.Logging;

namespace Pim.Infrastructure.Operations;

public sealed class Stage0DiagnosticJob
{
    private readonly ILogger<Stage0DiagnosticJob> _logger;

    public Stage0DiagnosticJob(ILogger<Stage0DiagnosticJob> logger)
    {
        _logger = logger;
    }

    public Task RunAsync()
    {
        _logger.LogInformation("Stage0 diagnostic job executed at {ExecutedAt}", DateTimeOffset.UtcNow);
        return Task.CompletedTask;
    }
}
```

- [ ] **Step 5: Implement Hangfire status service**

Create `src/Pim.Infrastructure/Operations/HangfireJobStatusService.cs`:

```csharp
using Hangfire;
using Hangfire.Storage;
using Pim.Core.Operations;

namespace Pim.Infrastructure.Operations;

public sealed class HangfireJobStatusService : IBackgroundJobStatusService
{
    public Task<BackgroundJobSummaryDto> GetSummaryAsync(CancellationToken ct = default)
    {
        var monitoring = JobStorage.Current.GetMonitoringApi();
        var queues = monitoring.Queues();
        var processing = monitoring.ProcessingCount();
        var scheduled = monitoring.ScheduledCount();
        var failed = (int)monitoring.FailedCount();
        var enqueued = queues.Sum(q => q.Length);
        var status = MapFailedCountToStatus(failed);

        return Task.FromResult(new BackgroundJobSummaryDto(
            status,
            (int)processing,
            (int)enqueued,
            (int)scheduled,
            failed,
            DateTimeOffset.UtcNow,
            failed > 0 ? "Some background jobs have failed." : "Background jobs are healthy."));
    }

    public static PimHealthStatus MapFailedCountToStatus(int failed)
        => failed > 0 ? PimHealthStatus.Warning : PimHealthStatus.Healthy;
}
```

- [ ] **Step 6: Configure Hangfire services**

Modify `src/Pim.Infrastructure/Extensions/ServiceCollectionExtensions.cs`.

Replace the `NoopBackgroundJobStatusService` registration with:

```csharp
        services.AddHangfire(config =>
            config.UsePostgreSqlStorage(options =>
                options.UseNpgsqlConnection(configuration.GetConnectionString("DefaultConnection"))));
        services.AddHangfireServer();
        services.AddScoped<IBackgroundJobStatusService, HangfireJobStatusService>();
        services.AddScoped<Stage0DiagnosticJob>();
```

Add using:

```csharp
using Hangfire;
```

- [ ] **Step 7: Protect Hangfire dashboard**

Create `src/Pim.Api/Infrastructure/HangfireAuthorizationFilter.cs`:

```csharp
using Hangfire.Dashboard;

namespace Pim.Api.Infrastructure;

public sealed class HangfireAuthorizationFilter : IDashboardAuthorizationFilter
{
    public bool Authorize(DashboardContext context)
    {
        var http = context.GetHttpContext();
        return http.User.Identity?.IsAuthenticated == true
            && http.User.IsInRole("admin");
    }
}
```

- [ ] **Step 8: Map Hangfire dashboard and recurring diagnostic job**

Modify `src/Pim.Api/Program.cs`:

```csharp
using Hangfire;
using Pim.Api.Infrastructure;
using Pim.Infrastructure.Operations;
```

After auth middleware:

```csharp
app.UseHangfireDashboard("/hangfire", new DashboardOptions
{
    Authorization = new[] { new HangfireAuthorizationFilter() }
});
```

After `moduleRegistry.InitializeAllAsync(app.Services);`:

```csharp
RecurringJob.AddOrUpdate<Stage0DiagnosticJob>(
    "stage0-diagnostic",
    job => job.RunAsync(),
    Cron.Hourly);
```

- [ ] **Step 9: Run tests and build**

Run:

```powershell
dotnet test tests\Pim.UnitTests\Pim.UnitTests.csproj --filter HangfireJobStatusServiceTests
dotnet build src\Pim.Api\Pim.Api.csproj
```

Expected: both PASS.

- [ ] **Step 10: Commit**

```powershell
git add src\Pim.Infrastructure\Pim.Infrastructure.csproj src\Pim.Infrastructure\Operations src\Pim.Infrastructure\Extensions\ServiceCollectionExtensions.cs src\Pim.Api\Infrastructure src\Pim.Api\Program.cs tests\Pim.UnitTests\Operations\HangfireJobStatusServiceTests.cs
git commit -m "feat: add hangfire background jobs"
```

---

## Task 8: Structured JSON Lines Logging And Error Classification

**Files:**
- Modify: `src/Pim.Api/Pim.Api.csproj`
- Create: `src/Pim.Api/Infrastructure/CorrelationIdMiddleware.cs`
- Modify: `src/Pim.Api/Middleware/ExceptionMiddleware.cs`
- Modify: `src/Pim.Api/Program.cs`
- Modify: `src/client-windows/Pim.Client.App/Pim.Client.App.csproj`
- Modify: `src/client-windows/Pim.Client.App/Services/Logger.cs`

- [ ] **Step 1: Add compact JSON logging package**

Modify `src/Pim.Api/Pim.Api.csproj` and `src/client-windows/Pim.Client.App/Pim.Client.App.csproj`:

```xml
    <PackageReference Include="Serilog.Formatting.Compact" Version="3.0.0" />
```

- [ ] **Step 2: Add correlation middleware**

Create `src/Pim.Api/Infrastructure/CorrelationIdMiddleware.cs`:

```csharp
namespace Pim.Api.Infrastructure;

public sealed class CorrelationIdMiddleware
{
    public const string HeaderName = "X-Correlation-Id";

    private readonly RequestDelegate _next;

    public CorrelationIdMiddleware(RequestDelegate next)
    {
        _next = next;
    }

    public async Task InvokeAsync(HttpContext context)
    {
        var correlationId = context.Request.Headers.TryGetValue(HeaderName, out var incoming) && !string.IsNullOrWhiteSpace(incoming)
            ? incoming.ToString()
            : Guid.NewGuid().ToString("N");

        context.Items[HeaderName] = correlationId;
        context.Response.Headers[HeaderName] = correlationId;

        using (Serilog.Context.LogContext.PushProperty("CorrelationId", correlationId))
        {
            await _next(context);
        }
    }
}
```

- [ ] **Step 3: Configure API JSON Lines logs**

Modify the `Log.Logger` block in `src/Pim.Api/Program.cs`:

```csharp
using Serilog.Formatting.Compact;
```

```csharp
Log.Logger = new LoggerConfiguration()
    .MinimumLevel.Debug()
    .Enrich.FromLogContext()
    .Enrich.WithProperty("Service", "pim-api")
    .WriteTo.Console(new CompactJsonFormatter())
    .WriteTo.File(new CompactJsonFormatter(), "/data/pim/logs/pim-api-.jsonl",
        rollingInterval: RollingInterval.Day,
        retainedFileCountLimit: 30)
    .CreateLogger();
```

Add middleware before exception middleware:

```csharp
app.UseMiddleware<CorrelationIdMiddleware>();
```

- [ ] **Step 4: Improve exception classification**

Modify `src/Pim.Api/Middleware/ExceptionMiddleware.cs` so the generic catch logs correlation id:

```csharp
var correlationId = context.Items[CorrelationIdMiddleware.HeaderName]?.ToString();
_logger.LogError(ex, "Unhandled exception with correlation id {CorrelationId}", correlationId);
```

Add using:

```csharp
using Pim.Api.Infrastructure;
```

Keep `DomainException` as `400` for this plan. Do not add new exception classes in Stage 0.

- [ ] **Step 5: Configure daemon JSON Lines logs**

Modify `src/client-windows/Pim.Client.App/Services/Logger.cs`:

```csharp
using Serilog.Formatting.Compact;
```

Replace the file sink:

```csharp
        var logFile = Path.Combine(LogDir, "pim-daemon-.jsonl");
        _serilog = new LoggerConfiguration()
            .MinimumLevel.Debug()
            .Enrich.WithProperty("Service", "pim-daemon")
            .WriteTo.Debug()
            .WriteTo.File(
                new CompactJsonFormatter(),
                logFile,
                rollingInterval: RollingInterval.Day,
                retainedFileCountLimit: 30)
            .CreateLogger();
```

Change `LogFilePath`:

```csharp
    public static string LogFilePath => _logFilePath ??= Path.Combine(LogDir, $"pim-daemon-{DateTime.Now:yyyy-MM-dd}.jsonl");
```

- [ ] **Step 6: Build API and daemon**

Run:

```powershell
dotnet build src\Pim.Api\Pim.Api.csproj
dotnet build src\client-windows\Pim.Client.App\Pim.Client.App.csproj
```

Expected: both PASS.

- [ ] **Step 7: Commit**

```powershell
git add src\Pim.Api src\client-windows\Pim.Client.App
git commit -m "feat: use structured json lines logs"
```

---

## Task 9: Windows Daemon Heartbeat Reporting

**Files:**
- Create: `src/client-windows/Pim.Client.Core/Models/DaemonHeartbeatDtos.cs`
- Create: `src/client-windows/Pim.Client.Core/Services/DaemonHeartbeatReporter.cs`
- Modify: `src/client-windows/Pim.Client.App/App.xaml.cs`
- Create: `tests/Pim.UnitTests/ClientWindows/DaemonHeartbeatReporterTests.cs`

- [ ] **Step 1: Write failing reporter tests**

Create `tests/Pim.UnitTests/ClientWindows/DaemonHeartbeatReporterTests.cs`:

```csharp
using Pim.Client.Core;
using Pim.Client.Core.Services;

namespace Pim.UnitTests.ClientWindows;

public class DaemonHeartbeatReporterTests
{
    [Fact]
    public void BuildHeartbeat_UsesIpv4LoopbackDefaultServerUrl()
    {
        var heartbeat = DaemonHeartbeatReporter.BuildHeartbeat(
            "device-1",
            "1.0.0",
            ClientDefaults.DefaultServerUrl,
            DateTimeOffset.Parse("2026-05-24T00:00:00Z"),
            null,
            null);

        Assert.Equal("http://127.0.0.1:5858", heartbeat.ServerUrl);
        Assert.Equal("windows", heartbeat.DaemonKind);
        Assert.Equal("device-1", heartbeat.DeviceId);
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run:

```powershell
dotnet test tests\Pim.UnitTests\Pim.UnitTests.csproj --filter DaemonHeartbeatReporterTests
```

Expected: FAIL because reporter and DTO do not exist.

- [ ] **Step 3: Create daemon heartbeat DTO**

Create `src/client-windows/Pim.Client.Core/Models/DaemonHeartbeatDtos.cs`:

```csharp
namespace Pim.Client.Core.Models;

public sealed record DaemonHeartbeatRequest(
    string DeviceId,
    string DaemonKind,
    string Version,
    string ServerUrl,
    DateTimeOffset? LastSuccessfulUploadAt,
    DateTimeOffset? LastAttemptedUploadAt,
    string? LastError,
    int? UploadQueueCount,
    string ActivityWatchState,
    string KeyStatsState,
    bool CollectionPaused,
    string StatusJson);
```

- [ ] **Step 4: Create heartbeat reporter**

Create `src/client-windows/Pim.Client.Core/Services/DaemonHeartbeatReporter.cs`:

```csharp
using System.Reflection;
using System.Text.Json;
using Pim.Client.Core.Models;

namespace Pim.Client.Core.Services;

public sealed class DaemonHeartbeatReporter
{
    private readonly ApiClient _api;

    public DaemonHeartbeatReporter(ApiClient api)
    {
        _api = api;
    }

    public async Task ReportAsync(DaemonHeartbeatRequest heartbeat, CancellationToken ct = default)
    {
        await _api.PostAsync<object>("daemon/heartbeat", heartbeat, ct);
    }

    public static DaemonHeartbeatRequest BuildHeartbeat(
        string deviceId,
        string version,
        string serverUrl,
        DateTimeOffset? lastSuccessfulUploadAt,
        DateTimeOffset? lastAttemptedUploadAt,
        string? lastError)
    {
        var normalizedServerUrl = ApiClient.NormalizeServerUrl(serverUrl);
        var statusJson = JsonSerializer.Serialize(new
        {
            machine = Environment.MachineName,
            process = "pim-windows-daemon"
        });

        return new DaemonHeartbeatRequest(
            deviceId,
            "windows",
            string.IsNullOrWhiteSpace(version)
                ? Assembly.GetExecutingAssembly().GetName().Version?.ToString() ?? "unknown"
                : version,
            normalizedServerUrl,
            lastSuccessfulUploadAt,
            lastAttemptedUploadAt,
            lastError,
            null,
            "Unknown",
            "Unknown",
            false,
            statusJson);
    }
}
```

- [ ] **Step 5: Register the reporter in daemon DI**

Modify `src/client-windows/Pim.Client.App/Startup.cs`:

```csharp
        services.AddSingleton<DaemonHeartbeatReporter>();
```

- [ ] **Step 6: Wire reporter into app startup**

Modify `src/client-windows/Pim.Client.App/App.xaml.cs`.

Add a cancellation source field beside `_trayIcon`:

```csharp
private readonly CancellationTokenSource _shutdown = new();
private readonly PeriodicTimer _heartbeatTimer = new(TimeSpan.FromMinutes(2));
```

Add this method to the `App` class:

```csharp
private async Task RunHeartbeatLoopAsync(CancellationToken ct)
{
    var reporter = Services.GetRequiredService<DaemonHeartbeatReporter>();
    while (await _heartbeatTimer.WaitForNextTickAsync(ct))
    {
        try
        {
            var config = DaemonConfig.Load();
            var heartbeat = DaemonHeartbeatReporter.BuildHeartbeat(
                Environment.MachineName,
                typeof(App).Assembly.GetName().Version?.ToString() ?? "unknown",
                config.ServerUrl,
                null,
                DateTimeOffset.UtcNow,
                null);
            await reporter.ReportAsync(heartbeat, ct);
            Logger.Info("Daemon heartbeat reported");
        }
        catch (Exception ex)
        {
            Logger.Warn($"Daemon heartbeat failed: {ex.Message}");
        }
    }
}
```

After the KeyStats collector starts in `OnStartup`, add:

```csharp
            _ = Task.Run(() => RunHeartbeatLoopAsync(_shutdown.Token));
            Logger.Info("Daemon heartbeat loop started");
```

At the beginning of `OnExit`, add:

```csharp
        _shutdown.Cancel();
        _heartbeatTimer.Dispose();
```

- [ ] **Step 7: Run daemon tests and build**

Run:

```powershell
dotnet test tests\Pim.UnitTests\Pim.UnitTests.csproj --filter DaemonHeartbeatReporterTests
dotnet build src\client-windows\Pim.Client.App\Pim.Client.App.csproj
```

Expected: both PASS.

- [ ] **Step 8: Commit**

```powershell
git add src\client-windows\Pim.Client.Core src\client-windows\Pim.Client.App\App.xaml.cs src\client-windows\Pim.Client.App\Startup.cs tests\Pim.UnitTests\ClientWindows\DaemonHeartbeatReporterTests.cs
git commit -m "feat: report windows daemon heartbeat"
```

---

## Task 10: Web Status API, Sidebar Indicator, And Status Page

**Files:**
- Modify: `src/client-web/src/types/index.ts`
- Create: `src/client-web/src/api/status.ts`
- Create: `src/client-web/src/components/status/SidebarStatusIndicator.tsx`
- Create: `src/client-web/src/pages/StatusPage.tsx`
- Modify: `src/client-web/src/layout/Sidebar.tsx`
- Modify: `src/client-web/src/layout/AppLayout.tsx`
- Create: `tests/client-web/statusApiPath.test.ts`

- [ ] **Step 1: Write status API path test**

Create `tests/client-web/statusApiPath.test.ts` using the existing Node `assert` style:

```ts
import assert from 'node:assert/strict';
import { statusApiPaths } from '../../src/client-web/src/api/status';

assert.equal(statusApiPaths.summary, '/status/summary');
assert.equal(statusApiPaths.detail, '/status/');
```

- [ ] **Step 2: Run client-web path test to verify it fails**

Run from `src/client-web`:

```powershell
npm exec tsx -- ..\..\tests\client-web\statusApiPath.test.ts
```

Expected: FAIL because `src/client-web/src/api/status.ts` does not exist.

- [ ] **Step 3: Add status TypeScript types**

Modify `src/client-web/src/types/index.ts`:

```ts
export type PimHealthStatus = 'Unknown' | 'Healthy' | 'Warning' | 'Critical';

export interface SystemStatusSummary {
  status: PimHealthStatus;
  label: string;
  message: string;
  checkedAt: string;
}

export interface StatusComponent {
  key: string;
  name: string;
  kind: string;
  status: PimHealthStatus;
  message: string;
  checkedAt: string;
  details: Record<string, string>;
}

export interface SystemStatusDetail {
  summary: SystemStatusSummary;
  components: StatusComponent[];
  nextSteps: string[];
}
```

- [ ] **Step 4: Add status API client**

Create `src/client-web/src/api/status.ts`:

```ts
import { apiGet } from './client';
import type { ApiResponse, SystemStatusDetail, SystemStatusSummary } from '../types';

export const statusApiPaths = {
  summary: '/status/summary',
  detail: '/status/',
};

export async function getStatusSummary(): Promise<SystemStatusSummary> {
  const response = await apiGet<ApiResponse<SystemStatusSummary>>(statusApiPaths.summary);
  return response.data;
}

export async function getStatusDetail(): Promise<SystemStatusDetail> {
  const response = await apiGet<ApiResponse<SystemStatusDetail>>(statusApiPaths.detail);
  return response.data;
}
```

- [ ] **Step 5: Create sidebar indicator**

Create `src/client-web/src/components/status/SidebarStatusIndicator.tsx`:

```tsx
import { useQuery } from '@tanstack/react-query';
import { getStatusSummary } from '../../api/status';
import type { PimHealthStatus } from '../../types';

const statusClass: Record<PimHealthStatus, string> = {
  Healthy: 'bg-emerald-500',
  Warning: 'bg-amber-500',
  Critical: 'bg-red-500',
  Unknown: 'bg-slate-400',
};

export default function SidebarStatusIndicator() {
  const { data, isLoading, isError } = useQuery({
    queryKey: ['status-summary'],
    queryFn: getStatusSummary,
    refetchInterval: 60_000,
  });

  const status = isLoading || isError || !data ? 'Unknown' : data.status;
  const label = isLoading ? '检查中' : isError ? '未知' : data?.label ?? '未知';
  const message = data?.message ?? '系统状态暂不可用';

  return (
    <div className="mx-3 mb-3 rounded-lg border border-slate-200 bg-white px-3 py-2 text-left">
      <div className="flex items-center gap-2">
        <span className={`h-2.5 w-2.5 rounded-full ${statusClass[status]}`} aria-hidden="true" />
        <span className="text-xs font-medium text-slate-700">{label}</span>
      </div>
      <p className="mt-1 line-clamp-2 text-[11px] leading-4 text-slate-500">{message}</p>
    </div>
  );
}
```

- [ ] **Step 6: Create status page**

Create `src/client-web/src/pages/StatusPage.tsx`:

```tsx
import { useQuery } from '@tanstack/react-query';
import { getStatusDetail } from '../api/status';
import PageHeader from '../ui/PageHeader';
import type { PimHealthStatus } from '../types';

const badgeClass: Record<PimHealthStatus, string> = {
  Healthy: 'bg-emerald-50 text-emerald-700 border-emerald-200',
  Warning: 'bg-amber-50 text-amber-700 border-amber-200',
  Critical: 'bg-red-50 text-red-700 border-red-200',
  Unknown: 'bg-slate-50 text-slate-600 border-slate-200',
};

export default function StatusPage() {
  const { data, isLoading, isError, refetch } = useQuery({
    queryKey: ['status-detail'],
    queryFn: getStatusDetail,
    refetchInterval: 60_000,
  });

  return (
    <div className="mx-auto max-w-6xl space-y-4">
      <PageHeader
        title="状态信息"
        description="查看 API、数据库、daemon、采集源和后台任务状态。"
        actions={
          <button
            onClick={() => refetch()}
            className="rounded-lg border border-slate-200 px-3 py-2 text-sm text-slate-600 hover:bg-slate-50"
          >
            刷新
          </button>
        }
      />

      {isLoading && <div className="rounded-lg border border-slate-200 bg-white p-4 text-sm text-slate-500">正在检查系统状态...</div>}
      {isError && <div className="rounded-lg border border-red-200 bg-red-50 p-4 text-sm text-red-700">状态信息暂不可用。</div>}

      {data && (
        <>
          <section className={`rounded-lg border p-4 ${badgeClass[data.summary.status]}`}>
            <p className="text-sm font-semibold">{data.summary.label}</p>
            <p className="mt-1 text-sm">{data.summary.message}</p>
          </section>

          {data.nextSteps.length > 0 && (
            <section className="rounded-lg border border-amber-200 bg-amber-50 p-4">
              <h2 className="text-sm font-semibold text-amber-800">需要关注</h2>
              <ul className="mt-2 space-y-1 text-sm text-amber-800">
                {data.nextSteps.map(step => <li key={step}>{step}</li>)}
              </ul>
            </section>
          )}

          <section className="grid gap-3 md:grid-cols-2">
            {data.components.map(component => (
              <article key={component.key} className="rounded-lg border border-slate-200 bg-white p-4">
                <div className="flex items-start justify-between gap-3">
                  <div>
                    <h2 className="text-sm font-semibold text-slate-900">{component.name}</h2>
                    <p className="mt-1 text-sm text-slate-500">{component.message}</p>
                  </div>
                  <span className={`rounded-full border px-2 py-1 text-xs ${badgeClass[component.status]}`}>
                    {component.status}
                  </span>
                </div>
                {Object.keys(component.details).length > 0 && (
                  <dl className="mt-3 grid grid-cols-2 gap-2 text-xs text-slate-500">
                    {Object.entries(component.details).map(([key, value]) => (
                      <div key={key}>
                        <dt className="font-medium text-slate-400">{key}</dt>
                        <dd className="truncate">{value}</dd>
                      </div>
                    ))}
                  </dl>
                )}
              </article>
            ))}
          </section>
        </>
      )}
    </div>
  );
}
```

- [ ] **Step 7: Add sidebar indicator and navigation**

Modify `src/client-web/src/layout/Sidebar.tsx`:

```tsx
import SidebarStatusIndicator from '../components/status/SidebarStatusIndicator';
```

Add nav item:

```ts
  { label: '状态信息', path: '/status', short: '态' },
```

Render the indicator directly below the PIM title block:

```tsx
      <SidebarStatusIndicator />
```

- [ ] **Step 8: Add route**

Modify `src/client-web/src/layout/AppLayout.tsx`:

```tsx
import StatusPage from '../pages/StatusPage';
```

Add route:

```tsx
            <Route path="/status" element={<StatusPage />} />
```

- [ ] **Step 9: Build Web**

Run:

```powershell
npm --prefix src/client-web run build
```

Expected: PASS.

- [ ] **Step 10: Commit**

```powershell
git add src\client-web\src tests\client-web\statusApiPath.test.ts
git commit -m "feat: add system status page"
```

---

## Task 11: Backup, Restore, Migration Docs, And Final Verification

**Files:**
- Create: `docs/operations/backup-restore.md`
- Create: `docs/operations/migrations.md`
- Modify: `docs/superpowers/specs/2026-05-24-stage-0-sustainable-operations-design.md` only if implementation changed the accepted design.

- [ ] **Step 1: Create backup and restore documentation**

Create `docs/operations/backup-restore.md`:

```markdown
# PIM Backup And Restore

## What To Back Up

- PostgreSQL database `pim`.
- MinIO data volume.
- API `/data` volume, including logs when needed.
- JWT private key files under `keys/` or `/data/keys`.
- Local deployment `.env` values.
- Windows daemon config at `%LOCALAPPDATA%\PIM\config.json`.

## What Is Not Backed Up Automatically

- Generated `bin/`, `obj/`, `build/`, `dist/`, and API `wwwroot` build artifacts.
- npm caches and temporary `.dotnet-*` directories.
- Local logs unless the operator explicitly copies them.

## Manual Restore Verification

1. Restore PostgreSQL and MinIO data.
2. Restore keys and environment values.
3. Start the API at `http://127.0.0.1:5858`.
4. Open Web and confirm login works.
5. Open "状态信息" and confirm API and database are healthy.
6. Start the Windows daemon and confirm its heartbeat appears.
7. Run `dotnet test Pim.sln`.
8. Run `npm --prefix src/client-web run build`.
```

- [ ] **Step 2: Create migration documentation**

Create `docs/operations/migrations.md`:

```markdown
# PIM Database Migrations

## Rules

- Ordinary schema changes use EF Core migrations.
- `Program.cs` runs migration adoption and then `Database.Migrate()`.
- PC Tracker idempotent SQL remains only for special compatibility SQL, special indexes, or future partition-style setup.
- Do not add new ordinary business tables through ad hoc startup SQL.

## Add A Migration

```powershell
dotnet ef migrations add <Name> --project src\Pim.Infrastructure --startup-project src\Pim.Api --context PimDbContext --output-dir Data\Migrations
```

## Apply Migrations Locally

```powershell
dotnet ef database update --project src\Pim.Infrastructure --startup-project src\Pim.Api --context PimDbContext
```

## Existing Development Databases

Databases previously created by `EnsureCreated()` are adopted by `PimMigrationAdoptionService`.

The service marks `20260524000000_BaselineExistingSchema` as already applied when it finds the existing `users` table and no EF migrations history table. After that, normal migrations apply only the changes after the baseline.

## Fresh Databases

Fresh databases run all migrations from the baseline onward.
```

- [ ] **Step 3: Run backend verification**

Run:

```powershell
dotnet test Pim.sln
```

Expected: PASS.

- [ ] **Step 4: Run Web verification**

Run:

```powershell
npm --prefix src/client-web run build
```

Expected: PASS.

- [ ] **Step 5: Inspect git status**

Run:

```powershell
git status --short --branch
```

Expected: only intentional documentation changes are unstaged or staged.

- [ ] **Step 6: Commit**

```powershell
git add docs\operations docs\superpowers\specs\2026-05-24-stage-0-sustainable-operations-design.md
git commit -m "docs: add stage 0 operations runbooks"
```

---

## Final Verification Checklist

Run these commands after all tasks:

```powershell
dotnet test Pim.sln
npm --prefix src/client-web run build
git status --short --branch
```

Expected:

- All .NET tests pass.
- Web build passes.
- `docs/plan.md` may remain untracked if it is still intentionally outside commits.
- No generated outputs are staged.

Manual checks:

- Fresh database starts and migrates.
- Existing development database is adopted and then migrates.
- `/health` returns liveness.
- `/api/v1/status/summary` returns a sidebar status payload.
- `/api/v1/status` returns component details.
- `/api/v1/daemon/heartbeat` updates daemon status.
- Sidebar shows top status indicator.
- Left navigation contains "状态信息".
- Hangfire dashboard is protected.
- Structured API and daemon logs are JSON Lines.
