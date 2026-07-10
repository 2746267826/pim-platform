# Microsoft Calendar Sync Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 实现一套由 MSAL Device Code Flow 驱动、覆盖全部 Microsoft 日历、支持增量与深度同步并对 Outlook 编辑执行二级确认的可靠同步体验。

**Architecture:** 在现有 PIM API 内把微软认证、Graph REST、日历发现、逐日历同步和确认后执行拆成边界清晰的服务，PostgreSQL 保存加密 MSAL cache、逐日历游标、同步运行和 durable execution。Graph 读取使用有界重试，Graph 写入由确认后的持久执行器串行恢复；Web 只调用用例级 API，并以四步向导、日历选择和二级确认呈现状态。

**Tech Stack:** .NET 8、ASP.NET Core Minimal APIs、EF Core 8/Npgsql、Microsoft.Identity.Client 4.86.0、Microsoft.Extensions.Http.Resilience 8.10.0、Hangfire、React 19、TypeScript 6、TanStack Query、Playwright、xUnit

---

## 实施边界

- 每个 PIM 用户只允许一个活动 Microsoft connection；一个 connection 可以绑定多个 Graph calendar。
- 首次权限固定为 `Calendars.ReadWrite` 与 `User.Read`，authority 默认 `https://login.microsoftonline.com/common`，不接受 Client Secret。
- 默认日历使用已文档化的 `/me/calendarView/delta`；非默认日历只使用 `/me/calendars/{id}/calendarView` 做窗口对账。
- 自动窗口固定为过去 90 天至未来 365 天；默认日历每天重建一次滚动基线。
- `full-resources` 只 upsert `/events` 资源；`range-instances` 按 180 天半开区间分片读取实例；二者都不凭缺失结果推断删除。
- 内部定时事件使用 UTC；Web 以 `Asia/Shanghai` 显示；全天事件以开始日期和排他结束日期保存。
- Graph 读取每次尝试 30 秒，最多 3 次总尝试；PATCH/DELETE 不做透明重试。
- Outlook 投影的更新和删除必须先创建 L3 confirmation，再经二级确认创建 durable execution；Graph 成功后才提交本地事实和审计。

## 文件结构

### 后端新增文件

- `src/modules/Pim.Module.Calendar/DTOs/OutlookSyncDtos.cs`：Microsoft 设置、授权会话、日历选择、同步运行、诊断和变更预览契约。
- `src/modules/Pim.Module.Calendar/Entities/OutlookAuthorizationSessionEntity.cs`：仅保存 UI 可见的设备授权会话状态。
- `src/modules/Pim.Module.Calendar/Entities/OutlookCalendarBindingEntity.cs`：Graph calendar 到 PIM calendar 的逐日历绑定和游标。
- `src/modules/Pim.Module.Calendar/Entities/OutlookOperationExecutionEntity.cs`：确认后的 durable execution/outbox。
- `src/modules/Pim.Module.Calendar/Services/OutlookConnectionLock.cs`：按 connection 串行认证和同步。
- `src/modules/Pim.Module.Calendar/Services/OutlookTokenCacheStore.cs`：Data Protection 加密的 MSAL V3 cache blob 存取。
- `src/modules/Pim.Module.Calendar/Services/MsalPublicClientAdapter.cs`：唯一直接依赖 MSAL 的 public-client 适配器。
- `src/modules/Pim.Module.Calendar/Services/MsalOutlookAuthCoordinator.cs`：静默 token、重新授权状态与设备授权会话编排。
- `src/modules/Pim.Module.Calendar/Services/OutlookAuthorizationSessionRunner.cs`：持有可取消的长时 MSAL device-code acquisition task。
- `src/modules/Pim.Module.Calendar/Services/GraphCalendarModels.cs`：内部 Graph JSON DTO，不泄漏到领域/API。
- `src/modules/Pim.Module.Calendar/Services/GraphCalendarClient.cs`：Graph REST 请求、分页、nextLink 校验与安全的 401 重放。
- `src/modules/Pim.Module.Calendar/Services/OutlookCalendarDiscoveryService.cs`：calendarGroups、分组日历和根日历发现与去重。
- `src/modules/Pim.Module.Calendar/Services/OutlookEventMapper.cs`：UTC、全天日期和 recurrence 映射。
- `src/modules/Pim.Module.Calendar/Services/OutlookEventProjectionService.cs`：远端事件 upsert、变更确认和删除核验。
- `src/modules/Pim.Module.Calendar/Services/OutlookCalendarSyncService.cs`：默认 delta、非默认窗口对账和深度同步。
- `src/modules/Pim.Module.Calendar/Services/OutlookCalendarSyncCoordinator.cs`：connection 锁、最多两个日历并发、运行进度和取消。
- `src/modules/Pim.Module.Calendar/Services/OutlookDiagnosticsService.cs`：连接、权限、发现、读取和时区诊断。
- `src/modules/Pim.Module.Calendar/Services/OutlookChangePreviewService.cs`：编辑、删除和只读复制用例。
- `src/modules/Pim.Module.Calendar/Services/OutlookConfirmedOperationHandler.cs`：条件写入、冲突和幂等补交。
- `src/modules/Pim.Module.Calendar/Services/OutlookSyncJobs.cs`：启动、5 分钟、每日基线和 execution 唤醒任务。
- `src/modules/Pim.Module.Calendar/Services/OutlookSyncFacade.cs`：API 的用户/connection 边界。
- `src/modules/Pim.Module.Calendar/Services/OutlookLegacyRebindingService.cs`：旧连接重新授权和可靠外部身份重绑。
- `src/Pim.Infrastructure/Data/Migrations/20260710000000_MicrosoftCalendarSync.cs`：确定性 schema 迁移；模型快照仍由 EF 生成更新。

### 后端修改/删除文件

- 修改 `src/modules/Pim.Module.Calendar/Entities/OutlookConnectionEntity.cs`、`OutlookSyncBatchEntity.cs`、`EventEntity.cs`、`CalendarEntity.cs`、`SyncConflictEntity.cs` 和 `CalendarEntityConfigurations.cs`。
- 修改 `src/modules/Pim.Module.Calendar/CalendarModule.cs`、`Pim.Module.Calendar.csproj`、`Services/CalendarService.cs` 与 `Services/CalendarDeleteService.cs`。
- 修改 `src/Pim.Core/Operations/ConfirmationDtos.cs`、`src/Pim.Infrastructure/Operations/OperationConfirmationService.cs` 和 `src/Pim.Infrastructure/Data/PimDbContext.cs`。
- 删除 `src/modules/Pim.Module.Calendar/Services/MicrosoftGraphDeviceCodeClient.cs`、`OutlookTokenService.cs`、`OutlookGraphModels.cs` 和旧的单体 `OutlookSyncService.cs`，其生产职责由上述小服务接管。

### Web 新增文件

- `src/client-web/src/components/outlook/EntraSetupGuide.tsx`
- `src/client-web/src/components/outlook/OutlookAuthorizationPanel.tsx`
- `src/client-web/src/components/outlook/OutlookCalendarPicker.tsx`
- `src/client-web/src/components/outlook/OutlookSyncControls.tsx`
- `src/client-web/src/components/outlook/OutlookDiagnosticsPanel.tsx`
- `src/client-web/src/components/outlook/OutlookWritebackDialog.tsx`
- `src/client-web/src/utils/calendarTime.ts`
- `tests/client-web/outlookSyncApiPath.test.ts`
- `tests/client-web/outlookSyncTypes.test.ts`
- `tests/client-web/outlookSyncUi.test.tsx`
- `tests/client-web/outlookSyncFlow.test.ts`
- `tests/client-web/tsconfig.outlook-sync.json`

### Web 修改文件

- `src/client-web/src/api/calendar.ts`、`src/client-web/src/types/index.ts`、`src/client-web/src/pages/SyncPage.tsx`。
- `src/client-web/src/dialogs/EventEditorDialog.tsx`、`src/client-web/src/pages/ConfirmationsPage.tsx`、`src/client-web/src/pages/CalendarPage.tsx`。
- `src/client-web/package.json` 与 `src/client-web/package-lock.json`。

## Task 1: 建立隔离执行环境和基线

**Files:**
- Read: `AGENTS.md`
- Read: `docs/superpowers/specs/2026-07-10-microsoft-calendar-sync-design.md`
- Read: `docs/superpowers/plans/2026-07-10-microsoft-calendar-sync.md`

- [ ] **Step 1: 调用 worktree 子技能**

执行前调用 `superpowers:using-git-worktrees`，从已包含本计划和设计文档的最新提交创建隔离 worktree，工作分支命名为 `codex/microsoft-calendar-sync`。

- [ ] **Step 2: 检查远端和脏文件**

Run:

```powershell
git status --short --branch
git fetch --all --prune
git branch --show-current
```

Expected: 当前分支以 `codex/` 开头；新 worktree 没有用户的 Android 计划改动或 `.opencode/`；`git fetch` 成功。

- [ ] **Step 3: 运行后端基线**

Run:

```powershell
dotnet test Pim.sln
```

Expected: PASS；记录测试总数和耗时。若失败，先确认失败是否也能在基线提交复现，不在本任务顺手修复无关测试。

- [ ] **Step 4: 运行 Web 基线**

Run:

```powershell
npm --prefix src/client-web ci
npm --prefix src/client-web run build
npm --prefix src/client-web run lint
npm --prefix src/client-web run test:schedule-workbench
```

Expected: 四条命令全部退出 0；生成目录保持未跟踪且不进入提交。

## Task 2: 扩展 Microsoft 同步持久化模型

**Files:**
- Create: `src/modules/Pim.Module.Calendar/Entities/OutlookAuthorizationSessionEntity.cs`
- Create: `src/modules/Pim.Module.Calendar/Entities/OutlookCalendarBindingEntity.cs`
- Create: `src/modules/Pim.Module.Calendar/Entities/OutlookOperationExecutionEntity.cs`
- Create: `tests/Pim.UnitTests/Calendar/OutlookPersistenceModelTests.cs`
- Modify: `src/modules/Pim.Module.Calendar/Entities/OutlookConnectionEntity.cs`
- Modify: `src/modules/Pim.Module.Calendar/Entities/OutlookSyncBatchEntity.cs`
- Modify: `src/modules/Pim.Module.Calendar/Entities/EventEntity.cs`
- Modify: `src/modules/Pim.Module.Calendar/Entities/CalendarEntity.cs`
- Modify: `src/modules/Pim.Module.Calendar/Entities/SyncConflictEntity.cs`
- Modify: `src/modules/Pim.Module.Calendar/Entities/CalendarEntityConfigurations.cs`

- [ ] **Step 1: 写失败的模型测试**

Create `tests/Pim.UnitTests/Calendar/OutlookPersistenceModelTests.cs`:

```csharp
using Microsoft.EntityFrameworkCore;
using Pim.Infrastructure.Data;
using Pim.Module.Calendar.Entities;
using Xunit;

namespace Pim.UnitTests.Calendar;

public sealed class OutlookPersistenceModelTests
{
    [Fact]
    public void MicrosoftSyncModel_HasPerCalendarAndDurableExecutionConstraints()
    {
        PimDbContext.RegisterModuleAssembly(typeof(OutlookConnectionEntity).Assembly);
        var options = new DbContextOptionsBuilder<PimDbContext>()
            .UseNpgsql("Host=localhost;Database=pim_model_tests")
            .Options;
        using var db = new PimDbContext(options);

        var connection = db.Model.FindEntityType(typeof(OutlookConnectionEntity))!;
        Assert.NotNull(connection.FindProperty(nameof(OutlookConnectionEntity.MsalCacheEncrypted)));
        Assert.NotNull(connection.FindProperty(nameof(OutlookConnectionEntity.HomeAccountId)));
        Assert.True(connection.FindProperty(nameof(OutlookConnectionEntity.Version))!.IsConcurrencyToken);
        Assert.True(connection.FindIndex(connection.FindProperty(nameof(OutlookConnectionEntity.UserId))!)!.IsUnique);

        var binding = db.Model.FindEntityType(typeof(OutlookCalendarBindingEntity))!;
        Assert.True(binding.GetIndexes().Single(index =>
            index.Properties.Select(property => property.Name).SequenceEqual([
                nameof(OutlookCalendarBindingEntity.ConnectionId),
                nameof(OutlookCalendarBindingEntity.GraphCalendarId)])).IsUnique);
        Assert.Contains(binding.GetForeignKeys(), foreignKey =>
            foreignKey.PrincipalEntityType.ClrType == typeof(OutlookConnectionEntity)
            && foreignKey.DeleteBehavior == DeleteBehavior.Cascade);
        Assert.Contains(binding.GetForeignKeys(), foreignKey =>
            foreignKey.PrincipalEntityType.ClrType == typeof(CalendarEntity)
            && foreignKey.DeleteBehavior == DeleteBehavior.Restrict);

        var execution = db.Model.FindEntityType(typeof(OutlookOperationExecutionEntity))!;
        Assert.True(execution.GetIndexes().Single(index =>
            index.Properties.Single().Name == nameof(OutlookOperationExecutionEntity.ConfirmationId)).IsUnique);

        var conflict = db.Model.FindEntityType(typeof(SyncConflictEntity))!;
        Assert.NotNull(conflict.FindProperty(nameof(SyncConflictEntity.SourceConfirmationId)));
        Assert.NotNull(conflict.FindIndex(conflict.FindProperty(nameof(SyncConflictEntity.SourceConfirmationId))!));

        var outlookEvent = db.Model.FindEntityType(typeof(EventEntity))!;
        var externalIdentity = outlookEvent.GetIndexes().Single(index =>
            index.Properties.Select(property => property.Name).SequenceEqual([
                nameof(EventEntity.OutlookCalendarBindingId),
                nameof(EventEntity.OutlookEventId)]));
        Assert.True(externalIdentity.IsUnique);
        Assert.Equal(
            "\"outlook_calendar_binding_id\" IS NOT NULL AND \"outlook_event_id\" IS NOT NULL AND \"deleted_at\" IS NULL",
            externalIdentity.GetFilter());
        Assert.Contains(outlookEvent.GetForeignKeys(), foreignKey =>
            foreignKey.PrincipalEntityType.ClrType == typeof(OutlookCalendarBindingEntity)
            && foreignKey.DeleteBehavior == DeleteBehavior.SetNull);
        Assert.Contains(outlookEvent.GetForeignKeys(), foreignKey =>
            foreignKey.PrincipalEntityType.ClrType == typeof(OutlookConnectionEntity)
            && foreignKey.DeleteBehavior == DeleteBehavior.SetNull);
    }
}
```

- [ ] **Step 2: 运行测试并确认缺少类型/字段**

Run:

```powershell
dotnet test tests/Pim.UnitTests/Pim.UnitTests.csproj --filter FullyQualifiedName~OutlookPersistenceModelTests
```

Expected: FAIL，编译错误明确指出 `OutlookCalendarBindingEntity`、`OutlookOperationExecutionEntity` 或新属性不存在。

- [ ] **Step 3: 新增授权会话实体**

Create `src/modules/Pim.Module.Calendar/Entities/OutlookAuthorizationSessionEntity.cs`:

```csharp
using System.ComponentModel.DataAnnotations;
using System.ComponentModel.DataAnnotations.Schema;

namespace Pim.Module.Calendar.Entities;

[Table("outlook_authorization_sessions")]
public sealed class OutlookAuthorizationSessionEntity
{
    [Key, Column("id")] public Guid Id { get; set; } = Guid.NewGuid();
    [Column("user_id")] public Guid UserId { get; set; }
    [Column("connection_id")] public Guid ConnectionId { get; set; }
    [Column("status"), MaxLength(32)] public string Status { get; set; } = "starting";
    [Column("verification_uri"), MaxLength(512)] public string? VerificationUri { get; set; }
    [Column("user_code"), MaxLength(64)] public string? UserCode { get; set; }
    [Column("expires_at")] public DateTimeOffset? ExpiresAt { get; set; }
    [Column("account_display_name"), MaxLength(255)] public string? AccountDisplayName { get; set; }
    [Column("account_login_hint"), MaxLength(255)] public string? AccountLoginHint { get; set; }
    [Column("error_code"), MaxLength(128)] public string? ErrorCode { get; set; }
    [Column("error_message")] public string? ErrorMessage { get; set; }
    [Column("created_at")] public DateTimeOffset CreatedAt { get; set; } = DateTimeOffset.UtcNow;
    [Column("updated_at")] public DateTimeOffset UpdatedAt { get; set; } = DateTimeOffset.UtcNow;
}
```

- [ ] **Step 4: 新增逐日历 binding 实体**

Create `src/modules/Pim.Module.Calendar/Entities/OutlookCalendarBindingEntity.cs`:

```csharp
using System.ComponentModel.DataAnnotations;
using System.ComponentModel.DataAnnotations.Schema;

namespace Pim.Module.Calendar.Entities;

[Table("outlook_calendar_bindings")]
public sealed class OutlookCalendarBindingEntity
{
    [Key, Column("id")] public Guid Id { get; set; } = Guid.NewGuid();
    [Column("connection_id")] public Guid ConnectionId { get; set; }
    [Column("pim_calendar_id")] public Guid PimCalendarId { get; set; }
    [Column("graph_calendar_id"), MaxLength(512)] public string GraphCalendarId { get; set; } = string.Empty;
    [Column("graph_group_id"), MaxLength(512)] public string? GraphGroupId { get; set; }
    [Column("graph_group_name"), MaxLength(255)] public string? GraphGroupName { get; set; }
    [Column("name"), MaxLength(255)] public string Name { get; set; } = string.Empty;
    [Column("color"), MaxLength(64)] public string? Color { get; set; }
    [Column("owner_name"), MaxLength(255)] public string? OwnerName { get; set; }
    [Column("owner_address"), MaxLength(320)] public string? OwnerAddress { get; set; }
    [Column("is_default_calendar")] public bool IsDefaultCalendar { get; set; }
    [Column("can_edit")] public bool CanEdit { get; set; }
    [Column("can_view_private_items")] public bool CanViewPrivateItems { get; set; }
    [Column("is_selected")] public bool IsSelected { get; set; } = true;
    [Column("remote_state"), MaxLength(32)] public string RemoteState { get; set; } = "active";
    [Column("sync_strategy"), MaxLength(32)] public string SyncStrategy { get; set; } = "window-reconcile";
    [Column("delta_link")] public string? DeltaLink { get; set; }
    [Column("baseline_window_start")] public DateTimeOffset? BaselineWindowStart { get; set; }
    [Column("baseline_window_end")] public DateTimeOffset? BaselineWindowEnd { get; set; }
    [Column("last_full_baseline_at")] public DateTimeOffset? LastFullBaselineAt { get; set; }
    [Column("last_discovery_at")] public DateTimeOffset? LastDiscoveryAt { get; set; }
    [Column("last_synced_at")] public DateTimeOffset? LastSyncedAt { get; set; }
    [Column("last_successful_generation")] public Guid? LastSuccessfulGeneration { get; set; }
    [Column("last_error_code"), MaxLength(128)] public string? LastErrorCode { get; set; }
    [Column("last_error_message")] public string? LastErrorMessage { get; set; }
    [Column("created_at")] public DateTimeOffset CreatedAt { get; set; } = DateTimeOffset.UtcNow;
    [Column("updated_at")] public DateTimeOffset UpdatedAt { get; set; } = DateTimeOffset.UtcNow;
}
```

- [ ] **Step 5: 新增 durable execution 实体**

Create `src/modules/Pim.Module.Calendar/Entities/OutlookOperationExecutionEntity.cs`:

```csharp
using System.ComponentModel.DataAnnotations;
using System.ComponentModel.DataAnnotations.Schema;

namespace Pim.Module.Calendar.Entities;

[Table("outlook_operation_executions")]
public sealed class OutlookOperationExecutionEntity
{
    [Key, Column("id")] public Guid Id { get; set; } = Guid.NewGuid();
    [Column("confirmation_id")] public Guid ConfirmationId { get; set; }
    [Column("user_id")] public Guid UserId { get; set; }
    [Column("operation_type"), MaxLength(128)] public string OperationType { get; set; } = string.Empty;
    [Column("proposed_hash"), MaxLength(64)] public string ProposedHash { get; set; } = string.Empty;
    [Column("payload_json", TypeName = "jsonb")] public string PayloadJson { get; set; } = "{}";
    [Column("state"), MaxLength(32)] public string State { get; set; } = "queued";
    [Column("attempt_count")] public int AttemptCount { get; set; }
    [Column("next_attempt_at")] public DateTimeOffset? NextAttemptAt { get; set; }
    [Column("last_error_code"), MaxLength(128)] public string? LastErrorCode { get; set; }
    [Column("last_error_message")] public string? LastErrorMessage { get; set; }
    [Column("created_at")] public DateTimeOffset CreatedAt { get; set; } = DateTimeOffset.UtcNow;
    [Column("updated_at")] public DateTimeOffset UpdatedAt { get; set; } = DateTimeOffset.UtcNow;
    [Column("completed_at")] public DateTimeOffset? CompletedAt { get; set; }
}
```

- [ ] **Step 6: 扩展 connection、run、calendar 和 event 字段**

Add these exact properties to the named entities:

```csharp
// OutlookConnectionEntity
[Column("authority"), MaxLength(512)]
public string Authority { get; set; } = "https://login.microsoftonline.com/common";
[Column("home_account_id"), MaxLength(512)] public string? HomeAccountId { get; set; }
[Column("account_display_name"), MaxLength(255)] public string? AccountDisplayName { get; set; }
[Column("account_login_hint"), MaxLength(255)] public string? AccountLoginHint { get; set; }
[Column("msal_cache_encrypted")] public byte[]? MsalCacheEncrypted { get; set; }
[Column("version"), ConcurrencyCheck] public long Version { get; set; }

// OutlookSyncBatchEntity
[Column("connection_id")] public Guid? ConnectionId { get; set; }
[Column("mode"), MaxLength(32)] public string Mode { get; set; } = "incremental";
[Column("requested_window_start")] public DateTimeOffset? RequestedWindowStart { get; set; }
[Column("requested_window_end")] public DateTimeOffset? RequestedWindowEnd { get; set; }
[Column("requested_calendar_ids_json", TypeName = "jsonb")] public string RequestedCalendarIdsJson { get; set; } = "[]";
[Column("per_calendar_json", TypeName = "jsonb")] public string PerCalendarJson { get; set; } = "[]";
[Column("cancel_requested")] public bool CancelRequested { get; set; }
[Column("updated_at")] public DateTimeOffset UpdatedAt { get; set; } = DateTimeOffset.UtcNow;

// CalendarEntity
[Column("source"), MaxLength(32)] public string Source { get; set; } = "manual";
[Column("is_visible")] public bool IsVisible { get; set; } = true;

// SyncConflictEntity
[Column("source_confirmation_id")] public Guid? SourceConfirmationId { get; set; }

// EventEntity
[Column("outlook_connection_id")] public Guid? OutlookConnectionId { get; set; }
[Column("outlook_calendar_binding_id")] public Guid? OutlookCalendarBindingId { get; set; }
[ForeignKey(nameof(OutlookCalendarBindingId))]
public OutlookCalendarBindingEntity? OutlookCalendarBinding { get; set; }
[Column("outlook_series_master_id"), MaxLength(512)] public string? OutlookSeriesMasterId { get; set; }
[Column("outlook_event_type"), MaxLength(32)] public string? OutlookEventType { get; set; }
[Column("original_start_time_zone"), MaxLength(128)] public string? OriginalStartTimeZone { get; set; }
[Column("original_end_time_zone"), MaxLength(128)] public string? OriginalEndTimeZone { get; set; }
[Column("all_day_start_date")] public DateOnly? AllDayStartDate { get; set; }
[Column("all_day_end_date_exclusive")] public DateOnly? AllDayEndDateExclusive { get; set; }
[Column("graph_recurrence_json", TypeName = "jsonb")] public string GraphRecurrenceJson { get; set; } = "{}";
[Column("last_seen_sync_generation")] public Guid? LastSeenSyncGeneration { get; set; }
[Column("outlook_sync_state"), MaxLength(32)] public string? OutlookSyncState { get; set; }
```

Also add `using System.ComponentModel.DataAnnotations;` where `[ConcurrencyCheck]` is introduced. Keep the legacy token and connection-level delta columns in `OutlookConnectionEntity`; new services must not read them.

- [ ] **Step 7: 配置默认值、关系和唯一索引**

Add these configurations to `CalendarEntityConfigurations.cs`:

```csharp
builder.Property(c => c.Source).HasDefaultValue("manual");
builder.Property(c => c.IsVisible).HasDefaultValue(true);

builder.Property(e => e.GraphRecurrenceJson).HasDefaultValue("{}");
builder.HasIndex(e => new { e.OutlookCalendarBindingId, e.OutlookEventId })
    .IsUnique()
    .HasFilter("\"outlook_calendar_binding_id\" IS NOT NULL AND \"outlook_event_id\" IS NOT NULL AND \"deleted_at\" IS NULL");
builder.HasOne(e => e.OutlookCalendarBinding)
    .WithMany()
    .HasForeignKey(e => e.OutlookCalendarBindingId)
    .OnDelete(DeleteBehavior.SetNull);
builder.HasOne<OutlookConnectionEntity>()
    .WithMany()
    .HasForeignKey(e => e.OutlookConnectionId)
    .OnDelete(DeleteBehavior.SetNull);

builder.Property(o => o.Authority).HasDefaultValue("https://login.microsoftonline.com/common");
builder.Property(o => o.Version).HasDefaultValue(0).IsConcurrencyToken();

builder.Property(o => o.Mode).HasDefaultValue("incremental");
builder.Property(o => o.RequestedCalendarIdsJson).HasDefaultValue("[]");
builder.Property(o => o.PerCalendarJson).HasDefaultValue("[]");
builder.Property(o => o.UpdatedAt).HasDefaultValueSql("now()");

// Existing SyncConflictEntityConfiguration.Configure
builder.HasIndex(c => c.SourceConfirmationId);

public sealed class OutlookAuthorizationSessionEntityConfiguration
    : IEntityTypeConfiguration<OutlookAuthorizationSessionEntity>
{
    public void Configure(EntityTypeBuilder<OutlookAuthorizationSessionEntity> builder)
    {
        builder.Property(entity => entity.Status).HasDefaultValue("starting");
        builder.HasIndex(entity => new { entity.UserId, entity.CreatedAt });
        builder.HasIndex(entity => new { entity.ConnectionId, entity.Status });
    }
}

public sealed class OutlookCalendarBindingEntityConfiguration
    : IEntityTypeConfiguration<OutlookCalendarBindingEntity>
{
    public void Configure(EntityTypeBuilder<OutlookCalendarBindingEntity> builder)
    {
        builder.Property(entity => entity.IsSelected).HasDefaultValue(true);
        builder.Property(entity => entity.RemoteState).HasDefaultValue("active");
        builder.Property(entity => entity.SyncStrategy).HasDefaultValue("window-reconcile");
        builder.HasIndex(entity => new { entity.ConnectionId, entity.GraphCalendarId }).IsUnique();
        builder.HasIndex(entity => entity.PimCalendarId).IsUnique();
        builder.HasOne<OutlookConnectionEntity>()
            .WithMany()
            .HasForeignKey(entity => entity.ConnectionId)
            .OnDelete(DeleteBehavior.Cascade);
        builder.HasOne<CalendarEntity>()
            .WithMany()
            .HasForeignKey(entity => entity.PimCalendarId)
            .OnDelete(DeleteBehavior.Restrict);
    }
}

public sealed class OutlookOperationExecutionEntityConfiguration
    : IEntityTypeConfiguration<OutlookOperationExecutionEntity>
{
    public void Configure(EntityTypeBuilder<OutlookOperationExecutionEntity> builder)
    {
        builder.Property(entity => entity.PayloadJson).HasDefaultValue("{}");
        builder.Property(entity => entity.State).HasDefaultValue("queued");
        builder.HasIndex(entity => entity.ConfirmationId).IsUnique();
        builder.HasIndex(entity => new { entity.State, entity.NextAttemptAt });
    }
}
```

- [ ] **Step 8: 运行模型测试**

Run:

```powershell
dotnet test tests/Pim.UnitTests/Pim.UnitTests.csproj --filter FullyQualifiedName~OutlookPersistenceModelTests
```

Expected: PASS。

- [ ] **Step 9: 提交模型检查点**

```powershell
git add src/modules/Pim.Module.Calendar/Entities tests/Pim.UnitTests/Calendar/OutlookPersistenceModelTests.cs
git commit -m "feat: add microsoft calendar sync model"
```

Expected: commit 只包含实体、配置和模型测试。

## Task 3: 添加确定性 EF migration

**Files:**
- Create: `src/Pim.Infrastructure/Data/Migrations/20260710000000_MicrosoftCalendarSync.cs`
- Modify: `src/Pim.Infrastructure/Data/Migrations/PimDbContextModelSnapshot.cs`
- Test: `tests/Pim.UnitTests/Operations/PimDbContextModelCacheTests.cs`

- [ ] **Step 1: 增加 migration 发现测试**

Add to `tests/Pim.UnitTests/Operations/PimDbContextModelCacheTests.cs`:

```csharp
[Fact]
public void MicrosoftCalendarSyncMigration_HasStableIdentifier()
{
    var migration = typeof(Pim.Infrastructure.Data.Migrations.MicrosoftCalendarSync);
    var attribute = migration.GetCustomAttributes(typeof(MigrationAttribute), false)
        .Cast<MigrationAttribute>()
        .Single();

    Assert.Equal("20260710000000_MicrosoftCalendarSync", attribute.Id);
}
```

Add these usings if absent:

```csharp
using Microsoft.EntityFrameworkCore.Migrations;
using System.Reflection;
```

- [ ] **Step 2: 运行测试并确认 migration 不存在**

Run:

```powershell
dotnet test tests/Pim.UnitTests/Pim.UnitTests.csproj --filter FullyQualifiedName~MicrosoftCalendarSyncMigration_HasStableIdentifier
```

Expected: FAIL，`MicrosoftCalendarSync` 类型不存在。

- [ ] **Step 3: 创建 migration 的 Up/Down**

Create `src/Pim.Infrastructure/Data/Migrations/20260710000000_MicrosoftCalendarSync.cs` with `[DbContext(typeof(PimDbContext))]` and `[Migration("20260710000000_MicrosoftCalendarSync")]`. Its `Up` must execute these concrete groups in order:

```csharp
namespace Pim.Infrastructure.Data.Migrations;

[DbContext(typeof(PimDbContext))]
[Migration("20260710000000_MicrosoftCalendarSync")]
public sealed class MicrosoftCalendarSync : Migration
{
    protected override void Up(MigrationBuilder migrationBuilder)
    {
        migrationBuilder.AddColumn<string>("authority", "outlook_connections", maxLength: 512, nullable: false,
            defaultValue: "https://login.microsoftonline.com/common");
        migrationBuilder.AddColumn<string>("home_account_id", "outlook_connections", maxLength: 512, nullable: true);
        migrationBuilder.AddColumn<string>("account_display_name", "outlook_connections", maxLength: 255, nullable: true);
        migrationBuilder.AddColumn<string>("account_login_hint", "outlook_connections", maxLength: 255, nullable: true);
        migrationBuilder.AddColumn<byte[]>("msal_cache_encrypted", "outlook_connections", nullable: true);
        migrationBuilder.AddColumn<long>("version", "outlook_connections", nullable: false, defaultValue: 0L);

        migrationBuilder.AddColumn<Guid>("connection_id", "outlook_sync_batches", nullable: true);
        migrationBuilder.AddColumn<string>("mode", "outlook_sync_batches", maxLength: 32, nullable: false, defaultValue: "incremental");
        migrationBuilder.AddColumn<DateTimeOffset>("requested_window_start", "outlook_sync_batches", nullable: true);
        migrationBuilder.AddColumn<DateTimeOffset>("requested_window_end", "outlook_sync_batches", nullable: true);
        migrationBuilder.AddColumn<string>("requested_calendar_ids_json", "outlook_sync_batches", type: "jsonb", nullable: false, defaultValue: "[]");
        migrationBuilder.AddColumn<string>("per_calendar_json", "outlook_sync_batches", type: "jsonb", nullable: false, defaultValue: "[]");
        migrationBuilder.AddColumn<bool>("cancel_requested", "outlook_sync_batches", nullable: false, defaultValue: false);
        migrationBuilder.AddColumn<DateTimeOffset>("updated_at", "outlook_sync_batches", nullable: false, defaultValueSql: "now()");

        migrationBuilder.AddColumn<string>("source", "calendars", maxLength: 32, nullable: false, defaultValue: "manual");
        migrationBuilder.AddColumn<bool>("is_visible", "calendars", nullable: false, defaultValue: true);

        migrationBuilder.AddColumn<Guid>("outlook_connection_id", "events", nullable: true);
        migrationBuilder.AddColumn<Guid>("outlook_calendar_binding_id", "events", nullable: true);
        migrationBuilder.AddColumn<string>("outlook_series_master_id", "events", maxLength: 512, nullable: true);
        migrationBuilder.AddColumn<string>("outlook_event_type", "events", maxLength: 32, nullable: true);
        migrationBuilder.AddColumn<string>("original_start_time_zone", "events", maxLength: 128, nullable: true);
        migrationBuilder.AddColumn<string>("original_end_time_zone", "events", maxLength: 128, nullable: true);
        migrationBuilder.AddColumn<DateOnly>("all_day_start_date", "events", type: "date", nullable: true);
        migrationBuilder.AddColumn<DateOnly>("all_day_end_date_exclusive", "events", type: "date", nullable: true);
        migrationBuilder.AddColumn<string>("graph_recurrence_json", "events", type: "jsonb", nullable: false, defaultValue: "{}");
        migrationBuilder.AddColumn<Guid>("last_seen_sync_generation", "events", nullable: true);
        migrationBuilder.AddColumn<string>("outlook_sync_state", "events", maxLength: 32, nullable: true);

        migrationBuilder.AddColumn<Guid>("source_confirmation_id", "sync_conflicts", nullable: true);
        migrationBuilder.CreateIndex(
            "IX_sync_conflicts_source_confirmation_id",
            "sync_conflicts",
            "source_confirmation_id");
        migrationBuilder.Sql("""
            UPDATE sync_conflicts
            SET source_confirmation_id = resolved_confirmation_id,
                resolved_confirmation_id = NULL
            WHERE provider = 'outlook'
              AND status = 'open'
              AND source_confirmation_id IS NULL
              AND resolved_confirmation_id IS NOT NULL;
            """);

        CreateAuthorizationSessions(migrationBuilder);
        CreateCalendarBindings(migrationBuilder);
        CreateOperationExecutions(migrationBuilder);

        migrationBuilder.CreateIndex(
            "IX_events_outlook_calendar_binding_id_outlook_event_id",
            "events",
            ["outlook_calendar_binding_id", "outlook_event_id"],
            unique: true,
            filter: "\"outlook_calendar_binding_id\" IS NOT NULL AND \"outlook_event_id\" IS NOT NULL AND \"deleted_at\" IS NULL");
        migrationBuilder.CreateIndex(
            "IX_events_outlook_connection_id",
            "events",
            "outlook_connection_id");
        migrationBuilder.AddForeignKey(
            "FK_events_outlook_calendar_bindings_outlook_calendar_binding_id",
            "events",
            "outlook_calendar_binding_id",
            "outlook_calendar_bindings",
            principalColumn: "id",
            onDelete: ReferentialAction.SetNull);
        migrationBuilder.AddForeignKey(
            "FK_events_outlook_connections_outlook_connection_id",
            "events",
            "outlook_connection_id",
            "outlook_connections",
            principalColumn: "id",
            onDelete: ReferentialAction.SetNull);
    }

    protected override void Down(MigrationBuilder migrationBuilder)
    {
        migrationBuilder.DropForeignKey(
            "FK_events_outlook_calendar_bindings_outlook_calendar_binding_id",
            "events");
        migrationBuilder.DropForeignKey(
            "FK_events_outlook_connections_outlook_connection_id",
            "events");
        migrationBuilder.DropTable("outlook_operation_executions");
        migrationBuilder.DropTable("outlook_authorization_sessions");
        migrationBuilder.DropTable("outlook_calendar_bindings");
        migrationBuilder.DropIndex("IX_events_outlook_calendar_binding_id_outlook_event_id", "events");
        migrationBuilder.DropIndex("IX_events_outlook_connection_id", "events");
        migrationBuilder.Sql("""
            UPDATE sync_conflicts
            SET resolved_confirmation_id = source_confirmation_id
            WHERE provider = 'outlook'
              AND status = 'open'
              AND resolved_confirmation_id IS NULL
              AND source_confirmation_id IS NOT NULL;
            """);
        migrationBuilder.DropIndex("IX_sync_conflicts_source_confirmation_id", "sync_conflicts");
        migrationBuilder.DropColumn("source_confirmation_id", "sync_conflicts");

        foreach (var column in new[] { "outlook_connection_id", "outlook_calendar_binding_id", "outlook_series_master_id",
                     "outlook_event_type", "original_start_time_zone", "original_end_time_zone", "all_day_start_date",
                     "all_day_end_date_exclusive", "graph_recurrence_json", "last_seen_sync_generation", "outlook_sync_state" })
            migrationBuilder.DropColumn(column, "events");
        foreach (var column in new[] { "source", "is_visible" })
            migrationBuilder.DropColumn(column, "calendars");
        foreach (var column in new[] { "connection_id", "mode", "requested_window_start", "requested_window_end",
                     "requested_calendar_ids_json", "per_calendar_json", "cancel_requested", "updated_at" })
            migrationBuilder.DropColumn(column, "outlook_sync_batches");
        foreach (var column in new[] { "authority", "home_account_id", "account_display_name", "account_login_hint",
                     "msal_cache_encrypted", "version" })
            migrationBuilder.DropColumn(column, "outlook_connections");
    }

    private static void CreateAuthorizationSessions(MigrationBuilder migrationBuilder)
    {
        migrationBuilder.CreateTable(
            "outlook_authorization_sessions",
            table => new
            {
                id = table.Column<Guid>(nullable: false), user_id = table.Column<Guid>(nullable: false),
                connection_id = table.Column<Guid>(nullable: false), status = table.Column<string>(maxLength: 32, nullable: false, defaultValue: "starting"),
                verification_uri = table.Column<string>(maxLength: 512, nullable: true), user_code = table.Column<string>(maxLength: 64, nullable: true),
                expires_at = table.Column<DateTimeOffset>(nullable: true), account_display_name = table.Column<string>(maxLength: 255, nullable: true),
                account_login_hint = table.Column<string>(maxLength: 255, nullable: true), error_code = table.Column<string>(maxLength: 128, nullable: true),
                error_message = table.Column<string>(nullable: true), created_at = table.Column<DateTimeOffset>(nullable: false),
                updated_at = table.Column<DateTimeOffset>(nullable: false)
            }, constraints => constraints.PrimaryKey("PK_outlook_authorization_sessions", row => row.id));
        migrationBuilder.CreateIndex("IX_outlook_authorization_sessions_user_id_created_at", "outlook_authorization_sessions", ["user_id", "created_at"]);
        migrationBuilder.CreateIndex("IX_outlook_authorization_sessions_connection_id_status", "outlook_authorization_sessions", ["connection_id", "status"]);
    }

    private static void CreateCalendarBindings(MigrationBuilder migrationBuilder)
    {
        migrationBuilder.CreateTable(
            "outlook_calendar_bindings",
            table => new
            {
                id = table.Column<Guid>(nullable: false), connection_id = table.Column<Guid>(nullable: false), pim_calendar_id = table.Column<Guid>(nullable: false),
                graph_calendar_id = table.Column<string>(maxLength: 512, nullable: false), graph_group_id = table.Column<string>(maxLength: 512, nullable: true),
                graph_group_name = table.Column<string>(maxLength: 255, nullable: true), name = table.Column<string>(maxLength: 255, nullable: false),
                color = table.Column<string>(maxLength: 64, nullable: true), owner_name = table.Column<string>(maxLength: 255, nullable: true),
                owner_address = table.Column<string>(maxLength: 320, nullable: true), is_default_calendar = table.Column<bool>(nullable: false),
                can_edit = table.Column<bool>(nullable: false), can_view_private_items = table.Column<bool>(nullable: false), is_selected = table.Column<bool>(nullable: false, defaultValue: true),
                remote_state = table.Column<string>(maxLength: 32, nullable: false, defaultValue: "active"), sync_strategy = table.Column<string>(maxLength: 32, nullable: false, defaultValue: "window-reconcile"),
                delta_link = table.Column<string>(nullable: true), baseline_window_start = table.Column<DateTimeOffset>(nullable: true), baseline_window_end = table.Column<DateTimeOffset>(nullable: true),
                last_full_baseline_at = table.Column<DateTimeOffset>(nullable: true), last_discovery_at = table.Column<DateTimeOffset>(nullable: true), last_synced_at = table.Column<DateTimeOffset>(nullable: true),
                last_successful_generation = table.Column<Guid>(nullable: true), last_error_code = table.Column<string>(maxLength: 128, nullable: true), last_error_message = table.Column<string>(nullable: true),
                created_at = table.Column<DateTimeOffset>(nullable: false), updated_at = table.Column<DateTimeOffset>(nullable: false)
            }, constraints =>
            {
                constraints.PrimaryKey("PK_outlook_calendar_bindings", row => row.id);
                constraints.ForeignKey("FK_outlook_calendar_bindings_outlook_connections_connection_id", row => row.connection_id, "outlook_connections", "id", onDelete: ReferentialAction.Cascade);
                constraints.ForeignKey("FK_outlook_calendar_bindings_calendars_pim_calendar_id", row => row.pim_calendar_id, "calendars", "id", onDelete: ReferentialAction.Restrict);
            });
        migrationBuilder.CreateIndex("IX_outlook_calendar_bindings_connection_id_graph_calendar_id", "outlook_calendar_bindings", ["connection_id", "graph_calendar_id"], unique: true);
        migrationBuilder.CreateIndex("IX_outlook_calendar_bindings_pim_calendar_id", "outlook_calendar_bindings", "pim_calendar_id", unique: true);
    }

    private static void CreateOperationExecutions(MigrationBuilder migrationBuilder)
    {
        migrationBuilder.CreateTable(
            "outlook_operation_executions",
            table => new
            {
                id = table.Column<Guid>(nullable: false), confirmation_id = table.Column<Guid>(nullable: false), user_id = table.Column<Guid>(nullable: false),
                operation_type = table.Column<string>(maxLength: 128, nullable: false), proposed_hash = table.Column<string>(maxLength: 64, nullable: false),
                payload_json = table.Column<string>(type: "jsonb", nullable: false, defaultValue: "{}"), state = table.Column<string>(maxLength: 32, nullable: false, defaultValue: "queued"),
                attempt_count = table.Column<int>(nullable: false), next_attempt_at = table.Column<DateTimeOffset>(nullable: true), last_error_code = table.Column<string>(maxLength: 128, nullable: true),
                last_error_message = table.Column<string>(nullable: true), created_at = table.Column<DateTimeOffset>(nullable: false), updated_at = table.Column<DateTimeOffset>(nullable: false),
                completed_at = table.Column<DateTimeOffset>(nullable: true)
            }, constraints => constraints.PrimaryKey("PK_outlook_operation_executions", row => row.id));
        migrationBuilder.CreateIndex("IX_outlook_operation_executions_confirmation_id", "outlook_operation_executions", "confirmation_id", unique: true);
        migrationBuilder.CreateIndex("IX_outlook_operation_executions_state_next_attempt_at", "outlook_operation_executions", ["state", "next_attempt_at"]);
    }
}
```

The file also needs these exact usings:

```csharp
using Microsoft.EntityFrameworkCore.Infrastructure;
using Microsoft.EntityFrameworkCore.Migrations;
using Pim.Infrastructure.Data;
```

- [ ] **Step 4: 更新 EF model snapshot**

Run the scaffold once so EF writes the target model, then keep only the snapshot change:

```powershell
dotnet ef migrations add MicrosoftCalendarSyncSnapshot --project src/Pim.Infrastructure --startup-project src/Pim.Api --output-dir Data/Migrations
git status --short src/Pim.Infrastructure/Data/Migrations
```

Expected: EF creates a temporary `*_MicrosoftCalendarSyncSnapshot.cs`, matching designer, and updates `PimDbContextModelSnapshot.cs`. Delete only the two temporary migration files with `apply_patch`; keep the snapshot and the deterministic `20260710000000_MicrosoftCalendarSync.cs`.

- [ ] **Step 5: 验证 migration 列表和模型**

Run:

```powershell
dotnet ef migrations list --project src/Pim.Infrastructure --startup-project src/Pim.Api
dotnet test tests/Pim.UnitTests/Pim.UnitTests.csproj --filter "FullyQualifiedName~MicrosoftCalendarSyncMigration_HasStableIdentifier|FullyQualifiedName~OutlookPersistenceModelTests"
```

Expected: migration 列表最后一项是 `20260710000000_MicrosoftCalendarSync`；两个测试 PASS。

- [ ] **Step 6: 提交 schema 检查点**

```powershell
git add src/Pim.Infrastructure/Data/Migrations tests/Pim.UnitTests/Operations/PimDbContextModelCacheTests.cs
git commit -m "feat: migrate microsoft calendar sync state"
```

Expected: commit 不包含 `bin/`、`obj/` 或临时 snapshot migration。

## Task 4: 用加密 MSAL cache 替换手工 refresh token

**Files:**
- Create: `src/modules/Pim.Module.Calendar/Services/OutlookConnectionLock.cs`
- Create: `src/modules/Pim.Module.Calendar/Services/OutlookTokenCacheStore.cs`
- Create: `src/modules/Pim.Module.Calendar/Services/MsalPublicClientAdapter.cs`
- Create: `src/modules/Pim.Module.Calendar/Services/MsalOutlookAuthCoordinator.cs`
- Create: `tests/Pim.UnitTests/Calendar/OutlookMsalAuthenticationTests.cs`
- Modify: `src/modules/Pim.Module.Calendar/Pim.Module.Calendar.csproj`

- [ ] **Step 1: 添加 MSAL 依赖**

Modify the package item group in `Pim.Module.Calendar.csproj`:

```xml
<PackageReference Include="Ical.Net" Version="5.2.2" />
<PackageReference Include="Microsoft.Identity.Client" Version="4.86.0" />
<PackageReference Include="Microsoft.Extensions.Http.Resilience" Version="8.10.0" />
```

Run:

```powershell
dotnet restore Pim.sln
rg -n "PersistKeysToFileSystem|DataProtection:KeysPath" src/Pim.Infrastructure src/Pim.Api
```

Expected: restore 退出 0，lock/asset 输出只位于被忽略的 `obj/`；基础设施继续把 Data Protection keys 持久化到受保护目录，而不是在 API 重启时生成一次性 key ring。

- [ ] **Step 2: 写 cache 与 silent auth 的失败测试**

Create `tests/Pim.UnitTests/Calendar/OutlookMsalAuthenticationTests.cs`:

```csharp
using Microsoft.EntityFrameworkCore;
using Pim.Infrastructure.Data;
using Pim.Module.Calendar.Entities;
using Pim.Module.Calendar.Services;
using Xunit;

namespace Pim.UnitTests.Calendar;

public sealed class OutlookMsalAuthenticationTests
{
    [Fact]
    public async Task CacheStore_EncryptsWholeMsalBlob()
    {
        await using var db = CreateDb();
        var connection = Connection();
        db.Set<OutlookConnectionEntity>().Add(connection);
        await db.SaveChangesAsync();
        var store = new OutlookTokenCacheStore(db, new TestSecretProtector());

        await store.SaveAsync(connection.Id, [1, 2, 3, 4], CancellationToken.None);

        var raw = await db.Set<OutlookConnectionEntity>().AsNoTracking().SingleAsync();
        Assert.NotNull(raw.MsalCacheEncrypted);
        Assert.DoesNotContain(new byte[] { 1, 2, 3, 4 }, raw.MsalCacheEncrypted!);
        Assert.Equal(new byte[] { 1, 2, 3, 4 }, await store.LoadAsync(connection.Id, CancellationToken.None));
    }

    [Fact]
    public async Task SilentAuth_MarksConnectionReauthRequiredWithoutExposingRefreshToken()
    {
        await using var db = CreateDb();
        var connection = Connection();
        db.Set<OutlookConnectionEntity>().Add(connection);
        await db.SaveChangesAsync();
        var coordinator = new MsalOutlookAuthCoordinator(
            db,
            new FakeMsalClient { SilentException = new OutlookReauthenticationRequiredException("interaction_required") },
            new OutlookConnectionLock());

        await Assert.ThrowsAsync<OutlookReauthenticationRequiredException>(() =>
            coordinator.AcquireAccessTokenAsync(connection.Id, false, CancellationToken.None));

        var stored = await db.Set<OutlookConnectionEntity>().SingleAsync();
        Assert.Equal("reauth-required", stored.Status);
        Assert.Equal("interaction-required", stored.TokenHealth);
    }

    private static OutlookConnectionEntity Connection() => new()
    {
        UserId = Guid.NewGuid(),
        ClientId = "11111111-1111-1111-1111-111111111111",
        TenantId = "common",
        Authority = "https://login.microsoftonline.com/common",
        HomeAccountId = "home-account",
        Status = "connected",
        TokenHealth = "healthy"
    };

    private static PimDbContext CreateDb()
    {
        PimDbContext.RegisterModuleAssembly(typeof(OutlookConnectionEntity).Assembly);
        return new PimDbContext(new DbContextOptionsBuilder<PimDbContext>()
            .UseInMemoryDatabase($"outlook-msal-{Guid.NewGuid()}")
            .Options);
    }
}

internal sealed class FakeMsalClient : IMsalPublicClientAdapter
{
    public Exception? SilentException { get; set; }
    public MsalAuthenticationResult Result { get; set; } = new(
        "access-token", "home-account", "user@example.com", "User", DateTimeOffset.UtcNow.AddHours(1),
        ["Calendars.ReadWrite", "User.Read"]);

    public Task<MsalAuthenticationResult> AcquireTokenSilentAsync(
        OutlookAuthContext context, bool forceRefresh, CancellationToken ct)
        => SilentException is null ? Task.FromResult(Result) : Task.FromException<MsalAuthenticationResult>(SilentException);

    public Task<MsalAuthenticationResult> AcquireTokenWithDeviceCodeAsync(
        OutlookAuthContext context,
        Func<OutlookDeviceCodePrompt, Task> onPrompt,
        CancellationToken ct)
        => Task.FromResult(Result);
}

internal sealed class TestSecretProtector : ISecretProtector
{
    public string Protect(string value)
        => "protected:" + Convert.ToBase64String(Encoding.UTF8.GetBytes(value));

    public string Unprotect(string protectedValue)
        => Encoding.UTF8.GetString(Convert.FromBase64String(protectedValue["protected:".Length..]));
}
```

Add `using System.Text;` and `using Pim.Infrastructure.Secrets;`, and use `new TestSecretProtector()` in `CacheStore_EncryptsWholeMsalBlob`. This keeps the MSAL tests independent from the legacy Graph doubles that Task 17 deletes.

- [ ] **Step 3: 运行测试并确认新服务不存在**

Run:

```powershell
dotnet test tests/Pim.UnitTests/Pim.UnitTests.csproj --filter FullyQualifiedName~OutlookMsalAuthenticationTests
```

Expected: FAIL，编译错误指向 `OutlookTokenCacheStore`、`IMsalPublicClientAdapter` 和 coordinator。

- [ ] **Step 4: 实现按 connection 的异步互斥锁**

Create `src/modules/Pim.Module.Calendar/Services/OutlookConnectionLock.cs`:

```csharp
using System.Collections.Concurrent;

namespace Pim.Module.Calendar.Services;

public sealed class OutlookConnectionLock
{
    private readonly ConcurrentDictionary<Guid, SemaphoreSlim> _locks = new();

    public async ValueTask<IAsyncDisposable> AcquireAsync(Guid connectionId, CancellationToken ct)
    {
        var gate = _locks.GetOrAdd(connectionId, _ => new SemaphoreSlim(1, 1));
        await gate.WaitAsync(ct);
        return new Releaser(gate);
    }

    private sealed class Releaser(SemaphoreSlim gate) : IAsyncDisposable
    {
        public ValueTask DisposeAsync()
        {
            gate.Release();
            return ValueTask.CompletedTask;
        }
    }
}
```

- [ ] **Step 5: 实现加密 cache blob 存取**

Create `src/modules/Pim.Module.Calendar/Services/OutlookTokenCacheStore.cs`:

```csharp
using System.Text;
using Microsoft.EntityFrameworkCore;
using Pim.Infrastructure.Data;
using Pim.Infrastructure.Secrets;
using Pim.Module.Calendar.Entities;

namespace Pim.Module.Calendar.Services;

public sealed class OutlookTokenCacheStore
{
    private readonly PimDbContext _db;
    private readonly ISecretProtector _protector;

    public OutlookTokenCacheStore(PimDbContext db, ISecretProtector protector)
    {
        _db = db;
        _protector = protector;
    }

    public async Task<byte[]?> LoadAsync(Guid connectionId, CancellationToken ct)
    {
        var encrypted = await _db.Set<OutlookConnectionEntity>()
            .Where(connection => connection.Id == connectionId)
            .Select(connection => connection.MsalCacheEncrypted)
            .SingleAsync(ct);
        if (encrypted is not { Length: > 0 }) return null;

        try
        {
            var protectedText = Encoding.UTF8.GetString(encrypted);
            return Convert.FromBase64String(_protector.Unprotect(protectedText));
        }
        catch (Exception exception) when (exception is FormatException or CryptographicException)
        {
            throw new OutlookTokenCacheCorruptedException(exception);
        }
    }

    public async Task SaveAsync(Guid connectionId, byte[] cacheBlob, CancellationToken ct)
    {
        var connection = await _db.Set<OutlookConnectionEntity>().SingleAsync(item => item.Id == connectionId, ct);
        var protectedText = _protector.Protect(Convert.ToBase64String(cacheBlob));
        connection.MsalCacheEncrypted = Encoding.UTF8.GetBytes(protectedText);
        connection.Version++;
        connection.UpdatedAt = DateTimeOffset.UtcNow;
        await _db.SaveChangesAsync(ct);
    }

    public async Task ClearAsync(Guid connectionId, CancellationToken ct)
    {
        var connection = await _db.Set<OutlookConnectionEntity>().SingleAsync(item => item.Id == connectionId, ct);
        connection.MsalCacheEncrypted = null;
        connection.HomeAccountId = null;
        connection.Version++;
        connection.UpdatedAt = DateTimeOffset.UtcNow;
        await _db.SaveChangesAsync(ct);
    }
}

public sealed class OutlookTokenCacheCorruptedException(Exception innerException)
    : Exception("The encrypted MSAL token cache cannot be read.", innerException);
```

Add `using System.Security.Cryptography;` to the file.

- [ ] **Step 6: 定义可替换的 MSAL adapter 契约**

Start `src/modules/Pim.Module.Calendar/Services/MsalPublicClientAdapter.cs` with these exact contracts:

```csharp
using Microsoft.Identity.Client;

namespace Pim.Module.Calendar.Services;

public static class OutlookAuthScopes
{
    public static readonly string[] Required = ["Calendars.ReadWrite", "User.Read"];
}

public sealed record OutlookAuthContext(
    Guid ConnectionId,
    string ClientId,
    string Authority,
    string? HomeAccountId);

public sealed record OutlookDeviceCodePrompt(
    string UserCode,
    string VerificationUri,
    DateTimeOffset ExpiresAt,
    string Message);

public sealed record MsalAuthenticationResult(
    string AccessToken,
    string HomeAccountId,
    string? Username,
    string? DisplayName,
    DateTimeOffset ExpiresOn,
    IReadOnlyList<string> Scopes);

public interface IMsalPublicClientAdapter
{
    Task<MsalAuthenticationResult> AcquireTokenSilentAsync(
        OutlookAuthContext context,
        bool forceRefresh,
        CancellationToken ct);

    Task<MsalAuthenticationResult> AcquireTokenWithDeviceCodeAsync(
        OutlookAuthContext context,
        Func<OutlookDeviceCodePrompt, Task> onPrompt,
        CancellationToken ct);
}

public sealed class OutlookReauthenticationRequiredException(string code, Exception? innerException = null)
    : Exception("Microsoft account interaction is required.", innerException)
{
    public string Code { get; } = code;
}
```

- [ ] **Step 7: 实现 production MSAL adapter**

Append this implementation to the same file:

```csharp
public sealed class MsalPublicClientAdapter : IMsalPublicClientAdapter
{
    private readonly OutlookTokenCacheStore _cacheStore;

    public MsalPublicClientAdapter(OutlookTokenCacheStore cacheStore) => _cacheStore = cacheStore;

    public async Task<MsalAuthenticationResult> AcquireTokenSilentAsync(
        OutlookAuthContext context,
        bool forceRefresh,
        CancellationToken ct)
    {
        var app = Build(context);
        BindCache(app.UserTokenCache, context.ConnectionId, ct);
        var accounts = await app.GetAccountsAsync();
        var account = accounts.SingleOrDefault(item => item.HomeAccountId.Identifier == context.HomeAccountId)
            ?? accounts.SingleOrDefault();
        if (account is null) throw new OutlookReauthenticationRequiredException("account-missing");

        try
        {
            var result = await app.AcquireTokenSilent(OutlookAuthScopes.Required, account)
                .WithForceRefresh(forceRefresh)
                .ExecuteAsync(ct);
            return Map(result);
        }
        catch (MsalUiRequiredException exception)
        {
            throw new OutlookReauthenticationRequiredException(exception.ErrorCode, exception);
        }
    }

    public async Task<MsalAuthenticationResult> AcquireTokenWithDeviceCodeAsync(
        OutlookAuthContext context,
        Func<OutlookDeviceCodePrompt, Task> onPrompt,
        CancellationToken ct)
    {
        var app = Build(context);
        BindCache(app.UserTokenCache, context.ConnectionId, ct);
        var result = await app.AcquireTokenWithDeviceCode(
                OutlookAuthScopes.Required,
                code => onPrompt(new OutlookDeviceCodePrompt(
                    code.UserCode,
                    code.VerificationUrl,
                    code.ExpiresOn,
                    code.Message)))
            .ExecuteAsync(ct);
        return Map(result);
    }

    private static IPublicClientApplication Build(OutlookAuthContext context)
        => PublicClientApplicationBuilder.Create(context.ClientId)
            .WithAuthority(context.Authority)
            .Build();

    private void BindCache(ITokenCache tokenCache, Guid connectionId, CancellationToken outerToken)
    {
        tokenCache.SetBeforeAccessAsync(async args =>
        {
            var bytes = await _cacheStore.LoadAsync(connectionId, outerToken);
            if (bytes is { Length: > 0 }) args.TokenCache.DeserializeMsalV3(bytes, shouldClearExistingCache: true);
        });
        tokenCache.SetAfterAccessAsync(async args =>
        {
            if (args.HasStateChanged)
                await _cacheStore.SaveAsync(connectionId, args.TokenCache.SerializeMsalV3(), outerToken);
        });
    }

    private static MsalAuthenticationResult Map(AuthenticationResult result)
        => new(
            result.AccessToken,
            result.Account.HomeAccountId.Identifier,
            result.Account.Username,
            result.ClaimsPrincipal?.Identity?.Name,
            result.ExpiresOn,
            result.Scopes.ToArray());
}
```

- [ ] **Step 8: 实现 silent auth coordinator**

Create `src/modules/Pim.Module.Calendar/Services/MsalOutlookAuthCoordinator.cs`:

```csharp
using Microsoft.EntityFrameworkCore;
using Pim.Infrastructure.Data;
using Pim.Module.Calendar.Entities;

namespace Pim.Module.Calendar.Services;

public interface IOutlookAccessTokenProvider
{
    Task<string> AcquireAccessTokenAsync(Guid connectionId, bool forceRefresh, CancellationToken ct);
}

public sealed class MsalOutlookAuthCoordinator : IOutlookAccessTokenProvider
{
    private readonly PimDbContext _db;
    private readonly IMsalPublicClientAdapter _msal;
    private readonly OutlookConnectionLock _connectionLock;

    public MsalOutlookAuthCoordinator(
        PimDbContext db,
        IMsalPublicClientAdapter msal,
        OutlookConnectionLock connectionLock)
    {
        _db = db;
        _msal = msal;
        _connectionLock = connectionLock;
    }

    public async Task<string> AcquireAccessTokenAsync(Guid connectionId, bool forceRefresh, CancellationToken ct)
    {
        await using var held = await _connectionLock.AcquireAsync(connectionId, ct);
        var connection = await _db.Set<OutlookConnectionEntity>().SingleAsync(item => item.Id == connectionId, ct);
        if (string.IsNullOrWhiteSpace(connection.ClientId))
            throw new InvalidOperationException("Microsoft Client ID is not configured.");

        try
        {
            var result = await _msal.AcquireTokenSilentAsync(
                new OutlookAuthContext(connection.Id, connection.ClientId, connection.Authority, connection.HomeAccountId),
                forceRefresh,
                ct);
            connection.HomeAccountId = result.HomeAccountId;
            connection.AccountDisplayName = result.DisplayName;
            connection.AccountLoginHint = result.Username;
            connection.Status = "connected";
            connection.TokenHealth = "healthy";
            connection.LastError = null;
            connection.Version++;
            connection.UpdatedAt = DateTimeOffset.UtcNow;
            await _db.SaveChangesAsync(ct);
            return result.AccessToken;
        }
        catch (OutlookReauthenticationRequiredException)
        {
            connection.Status = "reauth-required";
            connection.TokenHealth = "interaction-required";
            connection.LastError = "Microsoft requires the account to be authorized again.";
            connection.Version++;
            connection.UpdatedAt = DateTimeOffset.UtcNow;
            await _db.SaveChangesAsync(ct);
            throw;
        }
        catch (OutlookTokenCacheCorruptedException)
        {
            connection.Status = "reauth-required";
            connection.TokenHealth = "cache-corrupted";
            connection.LastError = "The local Microsoft token cache cannot be decrypted.";
            connection.Version++;
            connection.UpdatedAt = DateTimeOffset.UtcNow;
            await _db.SaveChangesAsync(ct);
            throw;
        }
    }
}
```

- [ ] **Step 9: 运行认证测试**

Run:

```powershell
dotnet test tests/Pim.UnitTests/Pim.UnitTests.csproj --filter FullyQualifiedName~OutlookMsalAuthenticationTests
```

Expected: PASS；日志和断言中都不存在 access token、refresh token 或 cache 明文。

- [ ] **Step 10: 提交认证基础**

```powershell
git add src/modules/Pim.Module.Calendar/Pim.Module.Calendar.csproj src/modules/Pim.Module.Calendar/Services tests/Pim.UnitTests/Calendar/OutlookMsalAuthenticationTests.cs
git commit -m "feat: persist encrypted msal token cache"
```

Expected: focused commit；旧 OAuth client 尚未删除，但后续新路径不再依赖它。

## Task 5: 实现设备授权会话状态机

**Files:**
- Create: `src/modules/Pim.Module.Calendar/DTOs/OutlookSyncDtos.cs`
- Create: `src/modules/Pim.Module.Calendar/Services/OutlookAuthorizationSessionRunner.cs`
- Create: `tests/Pim.UnitTests/Calendar/OutlookAuthorizationSessionTests.cs`
- Modify: `src/modules/Pim.Module.Calendar/Services/MsalOutlookAuthCoordinator.cs`

- [ ] **Step 1: 定义设置和授权会话契约**

Create the beginning of `src/modules/Pim.Module.Calendar/DTOs/OutlookSyncDtos.cs`:

```csharp
using System.ComponentModel.DataAnnotations;

namespace Pim.Module.Calendar.DTOs;

public sealed record UpdateOutlookSettingsRequest(
    [Required] string ClientId,
    [Required] string AccountScope,
    string? TenantId);

public sealed record OutlookSettingsResponse(
    string Provider,
    string ClientId,
    string AccountScope,
    string TenantId,
    string Authority,
    IReadOnlyList<string> Scopes,
    string Status,
    string TokenHealth,
    string? AccountDisplayName,
    string? AccountLoginHint,
    DateTimeOffset? LastSyncedAt,
    DateTimeOffset? NextScheduledSyncAt,
    string? LastError);

public sealed record OutlookAuthorizationSessionResponse(
    Guid Id,
    string Status,
    string? VerificationUri,
    string? UserCode,
    DateTimeOffset? ExpiresAt,
    string? AccountDisplayName,
    string? AccountLoginHint,
    string? ErrorCode,
    string? ErrorMessage,
    string? RecoveryAction);
```

Remove the old Outlook settings/device-code records from `CalendarDtos.cs` only after all compilation references have moved to this file.

- [ ] **Step 2: 写状态转换失败测试**

Create `tests/Pim.UnitTests/Calendar/OutlookAuthorizationSessionTests.cs`:

```csharp
using Microsoft.EntityFrameworkCore;
using Microsoft.Extensions.DependencyInjection;
using Pim.Infrastructure.Data;
using Pim.Module.Calendar.Entities;
using Pim.Module.Calendar.Services;
using Xunit;

namespace Pim.UnitTests.Calendar;

public sealed class OutlookAuthorizationSessionTests
{
    [Fact]
    public async Task Runner_PublishesPromptThenConnectsWithoutPersistingDeviceCode()
    {
        var services = Services(new PromptingMsalClient());
        await using var provider = services.BuildServiceProvider();
        var ids = await SeedAsync(provider);
        var runner = provider.GetRequiredService<OutlookAuthorizationSessionRunner>();

        var waiting = await runner.StartAsync(ids.SessionId, CancellationToken.None);
        Assert.Equal("waiting-for-user", waiting.Status);
        Assert.Equal("ABCD-EFGH", waiting.UserCode);

        await runner.WaitForCompletionAsync(ids.SessionId, CancellationToken.None);
        await using var scope = provider.CreateAsyncScope();
        var db = scope.ServiceProvider.GetRequiredService<PimDbContext>();
        var stored = await db.Set<OutlookAuthorizationSessionEntity>().SingleAsync();
        var connection = await db.Set<OutlookConnectionEntity>().SingleAsync();
        Assert.Equal("connected", stored.Status);
        Assert.Equal("home-account", connection.HomeAccountId);
        Assert.Empty(connection.AccessTokenEncrypted);
        Assert.Null(connection.RefreshTokenEncrypted);
    }

    [Fact]
    public async Task Runner_CancelMarksWaitingSessionCanceled()
    {
        var msal = new BlockingMsalClient();
        var services = Services(msal);
        await using var provider = services.BuildServiceProvider();
        var ids = await SeedAsync(provider);
        var runner = provider.GetRequiredService<OutlookAuthorizationSessionRunner>();
        await runner.StartAsync(ids.SessionId, CancellationToken.None);

        await runner.CancelAsync(ids.SessionId, ids.UserId, CancellationToken.None);
        await runner.WaitForCompletionAsync(ids.SessionId, CancellationToken.None);

        await using var scope = provider.CreateAsyncScope();
        var status = await scope.ServiceProvider.GetRequiredService<PimDbContext>()
            .Set<OutlookAuthorizationSessionEntity>()
            .Select(item => item.Status)
            .SingleAsync();
        Assert.Equal("canceled", status);
    }
}
```

Insert these complete helpers before the test class's closing brace:

```csharp
private sealed record SeedResult(Guid UserId, Guid SessionId);

private static ServiceCollection Services(IMsalPublicClientAdapter msal)
{
    PimDbContext.RegisterModuleAssembly(typeof(OutlookConnectionEntity).Assembly);
    var databaseName = $"outlook-auth-session-{Guid.NewGuid()}";
    var services = new ServiceCollection();
    services.AddDbContext<PimDbContext>(options => options.UseInMemoryDatabase(databaseName));
    services.AddSingleton<IMsalPublicClientAdapter>(msal);
    services.AddSingleton<OutlookConnectionLock>();
    services.AddSingleton<OutlookAuthorizationSessionRunner>();
    return services;
}

private static async Task<SeedResult> SeedAsync(ServiceProvider provider)
{
    await using var scope = provider.CreateAsyncScope();
    var db = scope.ServiceProvider.GetRequiredService<PimDbContext>();
    var userId = Guid.NewGuid();
    var connection = new OutlookConnectionEntity
    {
        UserId = userId,
        ClientId = "11111111-1111-1111-1111-111111111111",
        TenantId = "common",
        Authority = "https://login.microsoftonline.com/common",
        Status = "not-connected",
        TokenHealth = "missing"
    };
    var session = new OutlookAuthorizationSessionEntity
    {
        UserId = userId,
        ConnectionId = connection.Id,
        Status = "starting"
    };
    db.AddRange(connection, session);
    await db.SaveChangesAsync();
    return new SeedResult(userId, session.Id);
}

private sealed class PromptingMsalClient : FakeMsalClient
{
    public override async Task<MsalAuthenticationResult> AcquireTokenWithDeviceCodeAsync(
        OutlookAuthContext context,
        Func<OutlookDeviceCodePrompt, Task> onPrompt,
        CancellationToken ct)
    {
        await onPrompt(new OutlookDeviceCodePrompt(
            "ABCD-EFGH",
            "https://microsoft.com/devicelogin",
            DateTimeOffset.UtcNow.AddMinutes(15),
            "Open the page and enter the code."));
        return Result;
    }
}

private sealed class BlockingMsalClient : PromptingMsalClient
{
    public override async Task<MsalAuthenticationResult> AcquireTokenWithDeviceCodeAsync(
        OutlookAuthContext context,
        Func<OutlookDeviceCodePrompt, Task> onPrompt,
        CancellationToken ct)
    {
        await onPrompt(new OutlookDeviceCodePrompt(
            "ABCD-EFGH",
            "https://microsoft.com/devicelogin",
            DateTimeOffset.UtcNow.AddMinutes(15),
            "Open the page and enter the code."));
        await Task.Delay(Timeout.InfiniteTimeSpan, ct);
        return Result;
    }
}
```

Change `FakeMsalClient` in `OutlookMsalAuthenticationTests.cs` from `sealed` to a non-sealed class and mark both interface methods `virtual` so these test doubles compile.

- [ ] **Step 3: 运行测试并确认 runner 不存在**

Run:

```powershell
dotnet test tests/Pim.UnitTests/Pim.UnitTests.csproj --filter FullyQualifiedName~OutlookAuthorizationSessionTests
```

Expected: FAIL，编译错误指出 `OutlookAuthorizationSessionRunner` 不存在。

- [ ] **Step 4: 实现授权会话 runner 的启动和提示发布**

Create `src/modules/Pim.Module.Calendar/Services/OutlookAuthorizationSessionRunner.cs` with this class shape and exact start path:

```csharp
using System.Collections.Concurrent;
using Microsoft.EntityFrameworkCore;
using Microsoft.Extensions.DependencyInjection;
using Pim.Infrastructure.Data;
using Pim.Module.Calendar.Entities;

namespace Pim.Module.Calendar.Services;

public sealed class OutlookAuthorizationSessionRunner
{
    private readonly IServiceScopeFactory _scopeFactory;
    private readonly ConcurrentDictionary<Guid, RunningSession> _running = new();

    public OutlookAuthorizationSessionRunner(IServiceScopeFactory scopeFactory) => _scopeFactory = scopeFactory;

    public async Task<OutlookAuthorizationSessionEntity> StartAsync(Guid sessionId, CancellationToken requestToken)
    {
        var ready = new TaskCompletionSource<OutlookAuthorizationSessionEntity>(TaskCreationOptions.RunContinuationsAsynchronously);
        var cancellation = new CancellationTokenSource();
        var completion = RunAsync(sessionId, ready, cancellation.Token);
        if (!_running.TryAdd(sessionId, new RunningSession(cancellation, completion)))
        {
            cancellation.Dispose();
            throw new InvalidOperationException("This Microsoft authorization session is already running.");
        }

        _ = completion.ContinueWith(
            _ => Remove(sessionId),
            CancellationToken.None,
            TaskContinuationOptions.ExecuteSynchronously,
            TaskScheduler.Default);
        return await ready.Task.WaitAsync(TimeSpan.FromSeconds(30), requestToken);
    }

    public async Task CancelAsync(Guid sessionId, Guid userId, CancellationToken ct)
    {
        await using var scope = _scopeFactory.CreateAsyncScope();
        var db = scope.ServiceProvider.GetRequiredService<PimDbContext>();
        var session = await db.Set<OutlookAuthorizationSessionEntity>()
            .SingleAsync(item => item.Id == sessionId && item.UserId == userId, ct);
        if (session.Status is not ("starting" or "waiting-for-user")) return;
        if (_running.TryGetValue(sessionId, out var running)) running.Cancellation.Cancel();
    }

    public async Task WaitForCompletionAsync(Guid sessionId, CancellationToken ct)
    {
        if (_running.TryGetValue(sessionId, out var running)) await running.Completion.WaitAsync(ct);
    }

    public async Task<int> FailInterruptedSessionsAsync(CancellationToken ct)
    {
        await using var scope = _scopeFactory.CreateAsyncScope();
        var db = scope.ServiceProvider.GetRequiredService<PimDbContext>();
        var interrupted = await db.Set<OutlookAuthorizationSessionEntity>()
            .Where(item => item.Status == "starting" || item.Status == "waiting-for-user")
            .ToListAsync(ct);
        foreach (var session in interrupted)
        {
            session.Status = "failed";
            session.ErrorCode = "service-restarted";
            session.ErrorMessage = "PIM restarted while Microsoft authorization was waiting.";
            session.UpdatedAt = DateTimeOffset.UtcNow;
        }
        await db.SaveChangesAsync(ct);
        return interrupted.Count;
    }

    private void Remove(Guid sessionId)
    {
        if (_running.TryRemove(sessionId, out var running)) running.Cancellation.Dispose();
    }

    private sealed record RunningSession(CancellationTokenSource Cancellation, Task Completion);
}
```

- [ ] **Step 5: 实现 runner 的 MSAL 完成与错误状态**

Add `RunAsync` inside the runner:

```csharp
private async Task RunAsync(
    Guid sessionId,
    TaskCompletionSource<OutlookAuthorizationSessionEntity> ready,
    CancellationToken ct)
{
    await using var scope = _scopeFactory.CreateAsyncScope();
    var db = scope.ServiceProvider.GetRequiredService<PimDbContext>();
    var msal = scope.ServiceProvider.GetRequiredService<IMsalPublicClientAdapter>();
    var connectionLock = scope.ServiceProvider.GetRequiredService<OutlookConnectionLock>();
    var session = await db.Set<OutlookAuthorizationSessionEntity>().SingleAsync(item => item.Id == sessionId, ct);
    var connection = await db.Set<OutlookConnectionEntity>().SingleAsync(item => item.Id == session.ConnectionId, ct);

    try
    {
        await using var held = await connectionLock.AcquireAsync(connection.Id, ct);
        var result = await msal.AcquireTokenWithDeviceCodeAsync(
            new OutlookAuthContext(connection.Id, connection.ClientId!, connection.Authority, connection.HomeAccountId),
            async prompt =>
            {
                session.Status = "waiting-for-user";
                session.VerificationUri = prompt.VerificationUri;
                session.UserCode = prompt.UserCode;
                session.ExpiresAt = prompt.ExpiresAt;
                session.UpdatedAt = DateTimeOffset.UtcNow;
                await db.SaveChangesAsync(ct);
                ready.TrySetResult(Clone(session));
            },
            ct);

        connection.HomeAccountId = result.HomeAccountId;
        connection.AccountDisplayName = result.DisplayName;
        connection.AccountLoginHint = result.Username;
        connection.Status = "connected";
        connection.TokenHealth = "healthy";
        connection.LastError = null;
        connection.AccessTokenEncrypted = [];
        connection.RefreshTokenEncrypted = null;
        connection.AccessTokenExpiresAt = null;
        connection.DeltaLink = null;
        connection.Version++;
        connection.UpdatedAt = DateTimeOffset.UtcNow;
        session.Status = "connected";
        session.AccountDisplayName = result.DisplayName;
        session.AccountLoginHint = result.Username;
        session.UserCode = null;
        session.UpdatedAt = DateTimeOffset.UtcNow;
        await db.SaveChangesAsync(ct);
        ready.TrySetResult(Clone(session));
    }
    catch (OperationCanceledException) when (ct.IsCancellationRequested)
    {
        session.Status = "canceled";
        session.UserCode = null;
        session.UpdatedAt = DateTimeOffset.UtcNow;
        await db.SaveChangesAsync(CancellationToken.None);
        ready.TrySetResult(Clone(session));
    }
    catch (Exception exception)
    {
        session.Status = exception is MsalClientException { ErrorCode: "device_code_expired" } ? "expired" : "failed";
        session.ErrorCode = MapErrorCode(exception);
        session.ErrorMessage = SafeMessage(session.ErrorCode);
        session.UserCode = null;
        session.UpdatedAt = DateTimeOffset.UtcNow;
        await db.SaveChangesAsync(CancellationToken.None);
        ready.TrySetResult(Clone(session));
    }
}

private static string MapErrorCode(Exception exception) => exception switch
{
    MsalClientException { ErrorCode: "invalid_client" } => "invalid-client-id",
    MsalServiceException { ErrorCode: "unauthorized_client" } => "public-client-disabled",
    MsalServiceException { ErrorCode: "authorization_declined" } => "user-canceled",
    MsalServiceException { ErrorCode: "expired_token" } => "device-code-expired",
    MsalServiceException { ErrorCode: "consent_required" } => "admin-consent-required",
    HttpRequestException => "network-failure",
    OutlookTokenCacheCorruptedException => "cache-corrupted",
    _ => "authorization-failed"
};

private static string SafeMessage(string code) => code switch
{
    "invalid-client-id" => "Client ID 无效，请从 Entra 应用概述页重新复制。",
    "public-client-disabled" => "请在 Entra 身份验证设置中启用公共客户端流。",
    "user-canceled" => "你取消了 Microsoft 授权，可以重新请求设备代码。",
    "device-code-expired" => "设备代码已过期，请重新请求。",
    "admin-consent-required" => "租户策略需要管理员批准 Calendars.ReadWrite 与 User.Read。",
    "network-failure" => "PIM 无法连接 Microsoft 登录服务，请检查网络后重试。",
    "cache-corrupted" => "本地授权缓存无法解密，需要重新连接 Microsoft 账号。",
    _ => "Microsoft 授权未完成，请查看技术详情后重试。"
};

private static OutlookAuthorizationSessionEntity Clone(OutlookAuthorizationSessionEntity source) => new()
{
    Id = source.Id,
    UserId = source.UserId,
    ConnectionId = source.ConnectionId,
    Status = source.Status,
    VerificationUri = source.VerificationUri,
    UserCode = source.UserCode,
    ExpiresAt = source.ExpiresAt,
    AccountDisplayName = source.AccountDisplayName,
    AccountLoginHint = source.AccountLoginHint,
    ErrorCode = source.ErrorCode,
    ErrorMessage = source.ErrorMessage,
    CreatedAt = source.CreatedAt,
    UpdatedAt = source.UpdatedAt
};
```

Add `using Microsoft.Identity.Client;` to the runner. Do not log `UserCode`, `AccessToken`, cache bytes, or MSAL exception response bodies.

- [ ] **Step 6: 运行授权会话测试**

Run:

```powershell
dotnet test tests/Pim.UnitTests/Pim.UnitTests.csproj --filter FullyQualifiedName~OutlookAuthorizationSessionTests
```

Expected: PASS，测试覆盖 `waiting-for-user -> connected` 和 `waiting-for-user -> canceled`。

- [ ] **Step 7: 提交设备授权状态机**

```powershell
git add src/modules/Pim.Module.Calendar/DTOs/OutlookSyncDtos.cs src/modules/Pim.Module.Calendar/Services tests/Pim.UnitTests/Calendar/OutlookAuthorizationSessionTests.cs tests/Pim.UnitTests/Calendar/OutlookMsalAuthenticationTests.cs
git commit -m "feat: run microsoft device authorization sessions"
```

Expected: API 尚未暴露 session，但后端状态机可独立测试。

## Task 6: 建立安全且有界重试的 Graph REST client

**Files:**
- Create: `src/modules/Pim.Module.Calendar/Services/GraphCalendarModels.cs`
- Create: `src/modules/Pim.Module.Calendar/Services/GraphCalendarClient.cs`
- Create: `tests/Pim.UnitTests/Calendar/GraphCalendarClientTests.cs`
- Modify: `src/modules/Pim.Module.Calendar/CalendarModule.cs`

- [ ] **Step 1: 写 endpoint/header/retry 失败测试**

Create `tests/Pim.UnitTests/Calendar/GraphCalendarClientTests.cs`:

```csharp
using System.Net;
using Microsoft.Extensions.DependencyInjection;
using Pim.Module.Calendar.Services;
using Xunit;

namespace Pim.UnitTests.Calendar;

public sealed class GraphCalendarClientTests
{
    [Fact]
    public async Task Read_FollowsOnlyGraphHttpsNextLinkAndSendsRequiredPreferHeaders()
    {
        var handler = new RecordingHandler([
            Json(HttpStatusCode.OK, """{"value":[],"@odata.nextLink":"https://graph.microsoft.com/v1.0/me/calendarGroups?$skiptoken=next"}"""),
            Json(HttpStatusCode.OK, """{"value":[]}""")
        ]);
        var client = CreateClient(handler);

        var first = await client.GetCalendarGroupsPageAsync(Guid.NewGuid(), null, CancellationToken.None);
        await client.GetCalendarGroupsPageAsync(Guid.NewGuid(), first.NextLink, CancellationToken.None);

        Assert.All(handler.Requests, request =>
        {
            Assert.Contains("outlook.timezone=\"UTC\"", request.Prefer);
            Assert.Contains("IdType=\"ImmutableId\"", request.Prefer);
            Assert.NotNull(request.ClientRequestId);
        });
        await Assert.ThrowsAsync<InvalidOperationException>(() =>
            client.GetCalendarGroupsPageAsync(Guid.NewGuid(), "https://example.com/steal", CancellationToken.None));
    }

    [Fact]
    public async Task Read_Retries429And5xxAtMostThreeTotalAttempts()
    {
        var handler = new RecordingHandler([
            Json(HttpStatusCode.TooManyRequests, "{}", retryAfterSeconds: 0),
            Json(HttpStatusCode.ServiceUnavailable, "{}"),
            Json(HttpStatusCode.OK, """{"value":[]}""")
        ]);
        var client = CreateClient(handler);

        await client.GetCalendarsPageAsync(Guid.NewGuid(), null, CancellationToken.None);

        Assert.Equal(3, handler.Requests.Count);
    }

    [Fact]
    public async Task Write_DoesNotTransparentlyRetry()
    {
        var handler = new RecordingHandler([Json(HttpStatusCode.ServiceUnavailable, "{}")]);
        var client = CreateClient(handler);

        await Assert.ThrowsAsync<GraphRequestException>(() => client.PatchEventAsync(
            Guid.NewGuid(), "calendar", "event", "etag", new GraphEventPatch("Title", null, null, null, null, false), CancellationToken.None));

        Assert.Single(handler.Requests);
    }
}
```

Insert these helpers before the test class's closing brace:

```csharp
private static IGraphCalendarClient CreateClient(RecordingHandler handler)
{
    var services = new ServiceCollection();
    services.AddSingleton<IOutlookAccessTokenProvider>(new FixedTokenProvider());
    services.AddHttpClient(OutlookHttpClients.GraphRead, client => client.Timeout = Timeout.InfiniteTimeSpan)
        .ConfigurePrimaryHttpMessageHandler(() => handler)
        .AddStandardResilienceHandler(options =>
        {
            options.AttemptTimeout.Timeout = TimeSpan.FromSeconds(30);
            options.TotalRequestTimeout.Timeout = TimeSpan.FromMinutes(3);
            options.Retry.MaxRetryAttempts = 2;
            options.Retry.Delay = TimeSpan.Zero;
            options.Retry.UseJitter = false;
            options.Retry.ShouldRetryAfterHeader = true;
            options.Retry.ShouldHandle = new PredicateBuilder<HttpResponseMessage>()
                .Handle<HttpRequestException>()
                .Handle<TimeoutRejectedException>()
                .HandleResult(response => response.StatusCode is HttpStatusCode.RequestTimeout or HttpStatusCode.TooManyRequests
                    || (int)response.StatusCode >= 500);
        });
    services.AddHttpClient(OutlookHttpClients.GraphWrite, client => client.Timeout = TimeSpan.FromSeconds(30))
        .ConfigurePrimaryHttpMessageHandler(() => handler);
    services.AddTransient<IGraphCalendarClient, GraphCalendarClient>();
    return services.BuildServiceProvider().GetRequiredService<IGraphCalendarClient>();
}

private static HttpResponseMessage Json(HttpStatusCode status, string body, int? retryAfterSeconds = null)
{
    var response = new HttpResponseMessage(status)
    {
        Content = new StringContent(body, Encoding.UTF8, "application/json")
    };
    if (retryAfterSeconds is { } seconds)
        response.Headers.RetryAfter = new RetryConditionHeaderValue(TimeSpan.FromSeconds(seconds));
    return response;
}

private sealed class FixedTokenProvider : IOutlookAccessTokenProvider
{
    public Task<string> AcquireAccessTokenAsync(Guid connectionId, bool forceRefresh, CancellationToken ct)
        => Task.FromResult(forceRefresh ? "force-refreshed-token" : "access-token");
}

private sealed record RecordedRequest(Uri? Uri, string Prefer, string? ClientRequestId);

private sealed class RecordingHandler(IEnumerable<HttpResponseMessage> responses) : HttpMessageHandler
{
    private readonly Queue<HttpResponseMessage> _responses = new(responses);
    public List<RecordedRequest> Requests { get; } = [];

    protected override Task<HttpResponseMessage> SendAsync(HttpRequestMessage request, CancellationToken ct)
    {
        Requests.Add(new RecordedRequest(
            request.RequestUri,
            request.Headers.TryGetValues("Prefer", out var prefer) ? string.Join(',', prefer) : string.Empty,
            request.Headers.TryGetValues("client-request-id", out var ids) ? ids.Single() : null));
        if (_responses.Count == 0)
            throw new InvalidOperationException("The test handler has no queued response.");
        return Task.FromResult(_responses.Dequeue());
    }
}
```

Add these exact test usings:

```csharp
using System.Net.Http.Headers;
using System.Text;
using Microsoft.Extensions.DependencyInjection;
using Polly;
using Polly.Timeout;
```

- [ ] **Step 2: 运行测试并确认 Graph client 不存在**

Run:

```powershell
dotnet test tests/Pim.UnitTests/Pim.UnitTests.csproj --filter FullyQualifiedName~GraphCalendarClientTests
```

Expected: FAIL，编译错误指向 Graph models/client。

- [ ] **Step 3: 定义内部 Graph DTO**

Create `src/modules/Pim.Module.Calendar/Services/GraphCalendarModels.cs`:

```csharp
using System.Text.Json;
using System.Text.Json.Serialization;

namespace Pim.Module.Calendar.Services;

public sealed record GraphPage<T>(
    [property: JsonPropertyName("value")] IReadOnlyList<T> Value,
    [property: JsonPropertyName("@odata.nextLink")] string? NextLink,
    [property: JsonPropertyName("@odata.deltaLink")] string? DeltaLink);

public sealed record GraphUserDto(
    [property: JsonPropertyName("id")] string Id,
    [property: JsonPropertyName("displayName")] string? DisplayName,
    [property: JsonPropertyName("userPrincipalName")] string? UserPrincipalName);

public sealed record GraphCalendarGroupDto(
    [property: JsonPropertyName("id")] string Id,
    [property: JsonPropertyName("name")] string Name);

public sealed record GraphEmailAddressDto(
    [property: JsonPropertyName("name")] string? Name,
    [property: JsonPropertyName("address")] string? Address);

public sealed record GraphCalendarDto(
    [property: JsonPropertyName("id")] string Id,
    [property: JsonPropertyName("name")] string Name,
    [property: JsonPropertyName("color")] string? Color,
    [property: JsonPropertyName("isDefaultCalendar")] bool IsDefaultCalendar,
    [property: JsonPropertyName("canEdit")] bool CanEdit,
    [property: JsonPropertyName("canViewPrivateItems")] bool CanViewPrivateItems,
    [property: JsonPropertyName("owner")] GraphEmailAddressDto? Owner);

public sealed record GraphDateTimeTimeZoneDto(
    [property: JsonPropertyName("dateTime")] string DateTime,
    [property: JsonPropertyName("timeZone")] string? TimeZone);

public sealed record GraphLocationDto(
    [property: JsonPropertyName("displayName")] string? DisplayName);

public sealed record GraphEventDto(
    [property: JsonPropertyName("id")] string Id,
    [property: JsonPropertyName("subject")] string? Subject,
    [property: JsonPropertyName("bodyPreview")] string? BodyPreview,
    [property: JsonPropertyName("start")] GraphDateTimeTimeZoneDto? Start,
    [property: JsonPropertyName("end")] GraphDateTimeTimeZoneDto? End,
    [property: JsonPropertyName("isAllDay")] bool IsAllDay,
    [property: JsonPropertyName("iCalUId")] string? ICalUId,
    [property: JsonPropertyName("seriesMasterId")] string? SeriesMasterId,
    [property: JsonPropertyName("type")] string? Type,
    [property: JsonPropertyName("changeKey")] string? ChangeKey,
    [property: JsonPropertyName("@odata.etag")] string? ETag,
    [property: JsonPropertyName("lastModifiedDateTime")] DateTimeOffset? LastModifiedDateTime,
    [property: JsonPropertyName("location")] GraphLocationDto? Location,
    [property: JsonPropertyName("recurrence")] JsonElement? Recurrence,
    [property: JsonPropertyName("@removed")] JsonElement? Removed,
    [property: JsonPropertyName("originalStartTimeZone")] string? OriginalStartTimeZone = null,
    [property: JsonPropertyName("originalEndTimeZone")] string? OriginalEndTimeZone = null)
{
    [JsonIgnore] public bool IsRemoved => Removed is { ValueKind: JsonValueKind.Object };
}

public sealed record GraphEventPatch(
    [property: JsonPropertyName("subject")] string Subject,
    [property: JsonPropertyName("body")] object? Body,
    [property: JsonPropertyName("location")] GraphLocationDto? Location,
    [property: JsonPropertyName("start")] GraphDateTimeTimeZoneDto? Start,
    [property: JsonPropertyName("end")] GraphDateTimeTimeZoneDto? End,
    [property: JsonPropertyName("isAllDay")] bool IsAllDay);
```

- [ ] **Step 4: 定义 Graph client 接口和 URL 白名单**

Start `src/modules/Pim.Module.Calendar/Services/GraphCalendarClient.cs`:

```csharp
using System.Net;
using System.Net.Http.Headers;
using System.Net.Http.Json;
using System.Text.Json;

namespace Pim.Module.Calendar.Services;

public static class OutlookHttpClients
{
    public const string GraphRead = "outlook-graph-read";
    public const string GraphWrite = "outlook-graph-write";
}

public interface IGraphCalendarClient
{
    Task<GraphUserDto> GetMeAsync(Guid connectionId, CancellationToken ct);
    Task<GraphPage<GraphCalendarGroupDto>> GetCalendarGroupsPageAsync(Guid connectionId, string? nextLink, CancellationToken ct);
    Task<GraphPage<GraphCalendarDto>> GetGroupCalendarsPageAsync(Guid connectionId, string groupId, string? nextLink, CancellationToken ct);
    Task<GraphPage<GraphCalendarDto>> GetCalendarsPageAsync(Guid connectionId, string? nextLink, CancellationToken ct);
    Task<GraphPage<GraphEventDto>> GetDefaultDeltaPageAsync(Guid connectionId, string url, CancellationToken ct);
    Task<GraphPage<GraphEventDto>> GetCalendarViewPageAsync(Guid connectionId, string calendarId, string url, CancellationToken ct);
    Task<GraphPage<GraphEventDto>> GetEventsPageAsync(Guid connectionId, string calendarId, string? nextLink, CancellationToken ct);
    Task<GraphEventDto?> GetEventAsync(Guid connectionId, string calendarId, string eventId, CancellationToken ct);
    Task<GraphEventDto> PatchEventAsync(Guid connectionId, string calendarId, string eventId, string etag, GraphEventPatch patch, CancellationToken ct);
    Task DeleteEventAsync(Guid connectionId, string calendarId, string eventId, string etag, CancellationToken ct);
}

public sealed class GraphRequestException(
    HttpStatusCode statusCode,
    string? graphRequestId,
    string clientRequestId,
    string message) : Exception(message)
{
    public HttpStatusCode StatusCode { get; } = statusCode;
    public string? GraphRequestId { get; } = graphRequestId;
    public string ClientRequestId { get; } = clientRequestId;
}

public sealed class GraphCalendarClient : IGraphCalendarClient
{
    private const string BaseUrl = "https://graph.microsoft.com/v1.0";
    private static readonly JsonSerializerOptions JsonOptions = new(JsonSerializerDefaults.Web);
    private readonly IHttpClientFactory _clients;
    private readonly IOutlookAccessTokenProvider _tokens;

    public GraphCalendarClient(IHttpClientFactory clients, IOutlookAccessTokenProvider tokens)
    {
        _clients = clients;
        _tokens = tokens;
    }

    private static string SafeUrl(string url)
    {
        if (!Uri.TryCreate(url, UriKind.Absolute, out var absolute)) return BaseUrl + (url.StartsWith('/') ? url : "/" + url);
        if (absolute.Scheme != Uri.UriSchemeHttps || !string.Equals(absolute.Host, "graph.microsoft.com", StringComparison.OrdinalIgnoreCase))
            throw new InvalidOperationException("Graph nextLink must use https://graph.microsoft.com.");
        return absolute.AbsoluteUri;
    }
}
```

- [ ] **Step 5: 配置 read/write HttpClient 策略**

Replace the old `services.AddHttpClient("outlook")` registration in `CalendarModule.RegisterServices` with:

```csharp
services.AddHttpClient(OutlookHttpClients.GraphRead, client =>
    {
        client.BaseAddress = new Uri("https://graph.microsoft.com/v1.0/");
        client.Timeout = Timeout.InfiniteTimeSpan;
    })
    .AddStandardResilienceHandler(options =>
    {
        options.AttemptTimeout.Timeout = TimeSpan.FromSeconds(30);
        options.TotalRequestTimeout.Timeout = TimeSpan.FromMinutes(3);
        options.Retry.MaxRetryAttempts = 2;
        options.Retry.BackoffType = DelayBackoffType.Exponential;
        options.Retry.UseJitter = true;
        options.Retry.ShouldRetryAfterHeader = true;
        options.Retry.ShouldHandle = new PredicateBuilder<HttpResponseMessage>()
            .Handle<HttpRequestException>()
            .Handle<TimeoutRejectedException>()
            .HandleResult(response => response.StatusCode is HttpStatusCode.RequestTimeout or HttpStatusCode.TooManyRequests
                || (int)response.StatusCode >= 500);
    });
services.AddHttpClient(OutlookHttpClients.GraphWrite, client =>
{
    client.BaseAddress = new Uri("https://graph.microsoft.com/v1.0/");
    client.Timeout = TimeSpan.FromSeconds(30);
});
```

Add these usings to `CalendarModule.cs`:

```csharp
using System.Net;
using Polly;
using Polly.Timeout;
```

- [ ] **Step 6: 实现读取请求、401 单次 force refresh 与错误元数据**

Add these methods inside `GraphCalendarClient`:

```csharp
private async Task<HttpResponseMessage> SendReadAsync(Guid connectionId, string url, CancellationToken ct)
{
    for (var authAttempt = 0; authAttempt < 2; authAttempt++)
    {
        var token = await _tokens.AcquireAccessTokenAsync(connectionId, authAttempt == 1, ct);
        var request = CreateRequest(HttpMethod.Get, url, token, out var clientRequestId);
        var response = await _clients.CreateClient(OutlookHttpClients.GraphRead).SendAsync(request, ct);
        request.Dispose();
        if (response.StatusCode == HttpStatusCode.Unauthorized && authAttempt == 0)
        {
            response.Dispose();
            continue;
        }
        if (!response.IsSuccessStatusCode)
        {
            var exception = Error(response, clientRequestId);
            response.Dispose();
            throw exception;
        }
        return response;
    }
    throw new InvalidOperationException("Graph authentication replay did not return a response.");
}

private static HttpRequestMessage CreateRequest(
    HttpMethod method,
    string url,
    string accessToken,
    out string clientRequestId)
{
    clientRequestId = Guid.NewGuid().ToString();
    var request = new HttpRequestMessage(method, SafeUrl(url));
    request.Headers.Authorization = new AuthenticationHeaderValue("Bearer", accessToken);
    request.Headers.TryAddWithoutValidation("Prefer", "outlook.timezone=\"UTC\"");
    request.Headers.TryAddWithoutValidation("Prefer", "IdType=\"ImmutableId\"");
    request.Headers.TryAddWithoutValidation("client-request-id", clientRequestId);
    request.Headers.TryAddWithoutValidation("return-client-request-id", "true");
    return request;
}

private static GraphRequestException Error(HttpResponseMessage response, string clientRequestId)
{
    response.Headers.TryGetValues("request-id", out var values);
    return new GraphRequestException(
        response.StatusCode,
        values?.FirstOrDefault(),
        clientRequestId,
        $"Microsoft Graph returned HTTP {(int)response.StatusCode}.");
}

private async Task<T> ReadAsync<T>(Guid connectionId, string url, CancellationToken ct)
{
    using var response = await SendReadAsync(connectionId, url, ct);
    return await response.Content.ReadFromJsonAsync<T>(JsonOptions, ct)
        ?? throw new InvalidOperationException("Microsoft Graph returned an empty JSON body.");
}
```

The error message deliberately excludes the Graph body because it may contain event data. Log only connection/binding/run IDs, status, Graph request ID, client request ID, duration, and retry count.

- [ ] **Step 7: 实现全部读取 endpoint**

Add the public read methods:

```csharp
public Task<GraphUserDto> GetMeAsync(Guid connectionId, CancellationToken ct)
    => ReadAsync<GraphUserDto>(connectionId, "/me?$select=id,displayName,userPrincipalName", ct);

public Task<GraphPage<GraphCalendarGroupDto>> GetCalendarGroupsPageAsync(Guid connectionId, string? nextLink, CancellationToken ct)
    => ReadAsync<GraphPage<GraphCalendarGroupDto>>(connectionId,
        nextLink ?? "/me/calendarGroups?$select=id,name", ct);

public Task<GraphPage<GraphCalendarDto>> GetGroupCalendarsPageAsync(Guid connectionId, string groupId, string? nextLink, CancellationToken ct)
    => ReadAsync<GraphPage<GraphCalendarDto>>(connectionId,
        nextLink ?? $"/me/calendarGroups/{Uri.EscapeDataString(groupId)}/calendars?$select=id,name,color,isDefaultCalendar,canEdit,canViewPrivateItems,owner", ct);

public Task<GraphPage<GraphCalendarDto>> GetCalendarsPageAsync(Guid connectionId, string? nextLink, CancellationToken ct)
    => ReadAsync<GraphPage<GraphCalendarDto>>(connectionId,
        nextLink ?? "/me/calendars?$select=id,name,color,isDefaultCalendar,canEdit,canViewPrivateItems,owner", ct);

public Task<GraphPage<GraphEventDto>> GetDefaultDeltaPageAsync(Guid connectionId, string url, CancellationToken ct)
    => ReadAsync<GraphPage<GraphEventDto>>(connectionId, url, ct);

public Task<GraphPage<GraphEventDto>> GetCalendarViewPageAsync(Guid connectionId, string calendarId, string url, CancellationToken ct)
    => ReadAsync<GraphPage<GraphEventDto>>(connectionId, url, ct);

public Task<GraphPage<GraphEventDto>> GetEventsPageAsync(Guid connectionId, string calendarId, string? nextLink, CancellationToken ct)
    => ReadAsync<GraphPage<GraphEventDto>>(connectionId,
        nextLink ?? $"/me/calendars/{Uri.EscapeDataString(calendarId)}/events?$select=id,subject,bodyPreview,start,end,isAllDay,iCalUId,seriesMasterId,type,changeKey,lastModifiedDateTime,location,recurrence,originalStartTimeZone,originalEndTimeZone", ct);

public async Task<GraphEventDto?> GetEventAsync(Guid connectionId, string calendarId, string eventId, CancellationToken ct)
{
    try
    {
        return await ReadAsync<GraphEventDto>(connectionId,
            $"/me/calendars/{Uri.EscapeDataString(calendarId)}/events/{Uri.EscapeDataString(eventId)}", ct);
    }
    catch (GraphRequestException exception) when (exception.StatusCode == HttpStatusCode.NotFound)
    {
        return null;
    }
}
```

- [ ] **Step 8: 实现无透明重试的 PATCH/DELETE**

Add the write path:

```csharp
public async Task<GraphEventDto> PatchEventAsync(
    Guid connectionId,
    string calendarId,
    string eventId,
    string etag,
    GraphEventPatch patch,
    CancellationToken ct)
{
    using var response = await SendWriteAsync(connectionId, HttpMethod.Patch, calendarId, eventId, etag, patch, ct);
    return await response.Content.ReadFromJsonAsync<GraphEventDto>(JsonOptions, ct)
        ?? throw new InvalidOperationException("Microsoft Graph returned an empty event after PATCH.");
}

public async Task DeleteEventAsync(
    Guid connectionId,
    string calendarId,
    string eventId,
    string etag,
    CancellationToken ct)
{
    using var response = await SendWriteAsync(connectionId, HttpMethod.Delete, calendarId, eventId, etag, null, ct);
}

private async Task<HttpResponseMessage> SendWriteAsync(
    Guid connectionId,
    HttpMethod method,
    string calendarId,
    string eventId,
    string etag,
    object? body,
    CancellationToken ct)
{
    var token = await _tokens.AcquireAccessTokenAsync(connectionId, false, ct);
    using var request = CreateRequest(method,
        $"/me/calendars/{Uri.EscapeDataString(calendarId)}/events/{Uri.EscapeDataString(eventId)}",
        token,
        out var clientRequestId);
    request.Headers.TryAddWithoutValidation("If-Match", etag);
    if (body is not null) request.Content = JsonContent.Create(body, options: JsonOptions);
    var response = await _clients.CreateClient(OutlookHttpClients.GraphWrite).SendAsync(request, ct);
    if (!response.IsSuccessStatusCode)
    {
        var exception = Error(response, clientRequestId);
        response.Dispose();
        throw exception;
    }
    return response;
}
```

- [ ] **Step 9: 运行 Graph client 测试**

Run:

```powershell
dotnet test tests/Pim.UnitTests/Pim.UnitTests.csproj --filter FullyQualifiedName~GraphCalendarClientTests
```

Expected: PASS；读取最多发送 3 次，写入只发送 1 次，非 Graph host 被拒绝。

- [ ] **Step 10: 提交 Graph transport**

```powershell
git add src/modules/Pim.Module.Calendar/CalendarModule.cs src/modules/Pim.Module.Calendar/Services/GraphCalendarModels.cs src/modules/Pim.Module.Calendar/Services/GraphCalendarClient.cs tests/Pim.UnitTests/Calendar/GraphCalendarClientTests.cs
git commit -m "feat: add resilient microsoft graph calendar client"
```

Expected: commit 只引入 Graph transport；业务服务尚未解释 event 内容。

## Task 7: 发现全部日历并保存用户选择

**Files:**
- Create: `src/modules/Pim.Module.Calendar/Services/OutlookCalendarDiscoveryService.cs`
- Create: `tests/Pim.UnitTests/Calendar/OutlookGraphFakes.cs`
- Create: `tests/Pim.UnitTests/Calendar/OutlookCalendarDiscoveryTests.cs`
- Modify: `src/modules/Pim.Module.Calendar/DTOs/OutlookSyncDtos.cs`
- Modify: `src/modules/Pim.Module.Calendar/Services/CalendarService.cs`

- [ ] **Step 1: 扩展日历发现/选择 DTO**

Append to `OutlookSyncDtos.cs`:

```csharp
public sealed record OutlookCalendarBindingResponse(
    Guid Id,
    Guid PimCalendarId,
    string GraphCalendarId,
    string? GroupId,
    string GroupName,
    string Name,
    string? Color,
    bool IsDefault,
    bool CanEdit,
    bool CanViewPrivateItems,
    bool IsSelected,
    string RemoteState,
    string SyncStrategy,
    DateTimeOffset? LastSyncedAt,
    string? LastErrorCode,
    string? LastErrorMessage);

public sealed record OutlookCalendarGroupResponse(
    string? Id,
    string Name,
    IReadOnlyList<OutlookCalendarBindingResponse> Calendars);

public sealed record UpdateOutlookCalendarSelectionRequest(
    [Required] IReadOnlyList<Guid> SelectedBindingIds);

public sealed record OutlookCalendarDiscoveryResponse(
    int DiscoveredCount,
    int NewCount,
    int RemoteMissingCount,
    IReadOnlyList<OutlookCalendarGroupResponse> Groups);
```

- [ ] **Step 2: 创建可编程 Graph fake**

Create `tests/Pim.UnitTests/Calendar/OutlookGraphFakes.cs`:

```csharp
using System.Text.Json;
using Pim.Module.Calendar.Services;

namespace Pim.UnitTests.Calendar;

internal sealed class ProgrammableGraphCalendarClient : IGraphCalendarClient
{
    public Queue<GraphPage<GraphCalendarGroupDto>> GroupPages { get; } = new();
    public Dictionary<string, Queue<GraphPage<GraphCalendarDto>>> GroupCalendarPages { get; } = new();
    public Queue<GraphPage<GraphCalendarDto>> CalendarPages { get; } = new();
    public Queue<GraphPage<GraphEventDto>> DefaultDeltaPages { get; } = new();
    public Dictionary<string, Queue<GraphPage<GraphEventDto>>> CalendarViewPages { get; } = new();
    public Dictionary<string, Queue<GraphPage<GraphEventDto>>> EventPages { get; } = new();
    public Dictionary<(string CalendarId, string EventId), GraphEventDto?> Events { get; } = new();
    public List<(string CalendarId, string EventId)> GetEventCalls { get; } = [];
    public List<(string CalendarId, string EventId, string ETag, GraphEventPatch Patch)> Patches { get; } = [];
    public List<(string CalendarId, string EventId, string ETag)> Deletes { get; } = [];
    public List<string> DefaultDeltaUrls { get; } = [];
    public List<string> CalendarViewUrls { get; } = [];
    public Exception? CalendarListException { get; set; }
    public Exception? CalendarViewException { get; set; }
    public Exception? DefaultDeltaException { get; set; }
    public Exception? PatchExceptionAfterApplying { get; set; }

    public Task<GraphUserDto> GetMeAsync(Guid connectionId, CancellationToken ct)
        => Task.FromResult(new GraphUserDto("user", "Test User", "test@example.com"));

    public Task<GraphPage<GraphCalendarGroupDto>> GetCalendarGroupsPageAsync(
        Guid connectionId, string? nextLink, CancellationToken ct)
        => Task.FromResult(GroupPages.Count > 0
            ? GroupPages.Dequeue()
            : new GraphPage<GraphCalendarGroupDto>([], null, null));

    public Task<GraphPage<GraphCalendarDto>> GetGroupCalendarsPageAsync(
        Guid connectionId, string groupId, string? nextLink, CancellationToken ct)
        => Task.FromResult(GroupCalendarPages.TryGetValue(groupId, out var pages) && pages.Count > 0
            ? pages.Dequeue()
            : new GraphPage<GraphCalendarDto>([], null, null));

    public Task<GraphPage<GraphCalendarDto>> GetCalendarsPageAsync(
        Guid connectionId, string? nextLink, CancellationToken ct)
    {
        if (CalendarListException is not null)
            return Task.FromException<GraphPage<GraphCalendarDto>>(CalendarListException);
        return Task.FromResult(CalendarPages.Count > 0
            ? CalendarPages.Dequeue()
            : new GraphPage<GraphCalendarDto>([], null, null));
    }

    public Task<GraphPage<GraphEventDto>> GetDefaultDeltaPageAsync(
        Guid connectionId, string url, CancellationToken ct)
    {
        DefaultDeltaUrls.Add(url);
        if (DefaultDeltaException is not null)
            return Task.FromException<GraphPage<GraphEventDto>>(DefaultDeltaException);
        return Task.FromResult(DefaultDeltaPages.Dequeue());
    }

    public Task<GraphPage<GraphEventDto>> GetCalendarViewPageAsync(
        Guid connectionId, string calendarId, string url, CancellationToken ct)
    {
        CalendarViewUrls.Add(url);
        if (CalendarViewException is not null)
            return Task.FromException<GraphPage<GraphEventDto>>(CalendarViewException);
        return Task.FromResult(CalendarViewPages[calendarId].Dequeue());
    }

    public Task<GraphPage<GraphEventDto>> GetEventsPageAsync(
        Guid connectionId, string calendarId, string? nextLink, CancellationToken ct)
        => Task.FromResult(EventPages[calendarId].Dequeue());

    public Task<GraphEventDto?> GetEventAsync(
        Guid connectionId, string calendarId, string eventId, CancellationToken ct)
    {
        GetEventCalls.Add((calendarId, eventId));
        Events.TryGetValue((calendarId, eventId), out var remote);
        return Task.FromResult(remote);
    }

    public Task<GraphEventDto> PatchEventAsync(
        Guid connectionId,
        string calendarId,
        string eventId,
        string etag,
        GraphEventPatch patch,
        CancellationToken ct)
    {
        Patches.Add((calendarId, eventId, etag, patch));
        Events.TryGetValue((calendarId, eventId), out var current);
        var description = patch.Body is null
            ? current?.BodyPreview
            : JsonSerializer.SerializeToElement(patch.Body).TryGetProperty("content", out var content)
                ? content.GetString()
                : current?.BodyPreview;
        var updated = (current ?? new GraphEventDto(
            eventId, patch.Subject, description, patch.Start, patch.End, patch.IsAllDay,
            null, null, "singleInstance", null, etag, DateTimeOffset.UtcNow,
            patch.Location, null, null)) with
        {
            Subject = patch.Subject,
            BodyPreview = description,
            Location = patch.Location,
            Start = patch.Start,
            End = patch.End,
            IsAllDay = patch.IsAllDay,
            ChangeKey = "change-after",
            ETag = "etag-after"
        };
        Events[(calendarId, eventId)] = updated;
        if (PatchExceptionAfterApplying is not null)
            return Task.FromException<GraphEventDto>(PatchExceptionAfterApplying);
        return Task.FromResult(updated);
    }

    public Task DeleteEventAsync(
        Guid connectionId, string calendarId, string eventId, string etag, CancellationToken ct)
    {
        Deletes.Add((calendarId, eventId, etag));
        Events[(calendarId, eventId)] = null;
        return Task.CompletedTask;
    }
}
```

- [ ] **Step 3: 写发现分页、去重和失败保护测试**

Create `tests/Pim.UnitTests/Calendar/OutlookCalendarDiscoveryTests.cs`:

```csharp
using Microsoft.EntityFrameworkCore;
using Pim.Infrastructure.Data;
using Pim.Module.Calendar.Entities;
using Pim.Module.Calendar.Services;
using Xunit;

namespace Pim.UnitTests.Calendar;

public sealed class OutlookCalendarDiscoveryTests
{
    [Fact]
    public async Task Discovery_CombinesGroupsAndRootCalendarsWithoutDuplicates()
    {
        await using var db = CreateDb();
        var connection = SeedConnection(db);
        var graph = new ProgrammableGraphCalendarClient();
        graph.GroupPages.Enqueue(new GraphPage<GraphCalendarGroupDto>(
            [new("group-1", "学校")], "https://graph.microsoft.com/v1.0/me/calendarGroups?$skiptoken=2", null));
        graph.GroupPages.Enqueue(new GraphPage<GraphCalendarGroupDto>([], null, null));
        graph.GroupCalendarPages["group-1"] = new Queue<GraphPage<GraphCalendarDto>>([
            new([Calendar("default", "默认日历", true), Calendar("course", "课程表", false)], null, null)
        ]);
        graph.CalendarPages.Enqueue(new GraphPage<GraphCalendarDto>(
            [Calendar("course", "课程表", false), Calendar("other", "考试")], null, null));
        var service = new OutlookCalendarDiscoveryService(db, graph);

        var result = await service.DiscoverAsync(connection.UserId, connection.Id, CancellationToken.None);

        Assert.Equal(3, result.DiscoveredCount);
        var bindings = await db.Set<OutlookCalendarBindingEntity>().OrderBy(item => item.GraphCalendarId).ToListAsync();
        Assert.All(bindings, item => Assert.True(item.IsSelected));
        Assert.Equal("default-delta", bindings.Single(item => item.GraphCalendarId == "default").SyncStrategy);
        Assert.Equal("学校", bindings.Single(item => item.GraphCalendarId == "course").GraphGroupName);
        Assert.Null(bindings.Single(item => item.GraphCalendarId == "other").GraphGroupId);
        Assert.Equal(3, await db.Set<CalendarEntity>().CountAsync(item => item.Source == "outlook"));
    }

    [Fact]
    public async Task DiscoveryFailure_DoesNotMarkExistingBindingRemoteMissing()
    {
        await using var db = CreateDb();
        var connection = SeedConnection(db);
        var calendar = new CalendarEntity { UserId = connection.UserId, Name = "Existing", Source = "outlook" };
        db.Add(calendar);
        db.Add(new OutlookCalendarBindingEntity
        {
            ConnectionId = connection.Id,
            PimCalendarId = calendar.Id,
            GraphCalendarId = "existing",
            Name = "Existing",
            RemoteState = "active"
        });
        await db.SaveChangesAsync();
        var graph = new ProgrammableGraphCalendarClient { CalendarListException = new HttpRequestException("offline") };
        graph.GroupPages.Enqueue(new GraphPage<GraphCalendarGroupDto>([], null, null));
        var service = new OutlookCalendarDiscoveryService(db, graph);

        await Assert.ThrowsAsync<HttpRequestException>(() =>
            service.DiscoverAsync(connection.UserId, connection.Id, CancellationToken.None));

        Assert.Equal("active", (await db.Set<OutlookCalendarBindingEntity>().SingleAsync()).RemoteState);
    }
}
```

Add helper methods to the test file with exact defaults:

```csharp
private static GraphCalendarDto Calendar(string id, string name, bool isDefault = false)
    => new(id, name, "lightBlue", isDefault, true, false, new GraphEmailAddressDto("Owner", "owner@example.com"));

private static OutlookConnectionEntity SeedConnection(PimDbContext db)
{
    var connection = new OutlookConnectionEntity
    {
        UserId = Guid.NewGuid(),
        ClientId = Guid.NewGuid().ToString(),
        Status = "connected",
        TokenHealth = "healthy"
    };
    db.Add(connection);
    db.SaveChanges();
    return connection;
}

private static PimDbContext CreateDb()
{
    PimDbContext.RegisterModuleAssembly(typeof(OutlookConnectionEntity).Assembly);
    return new PimDbContext(new DbContextOptionsBuilder<PimDbContext>()
        .UseInMemoryDatabase($"outlook-discovery-{Guid.NewGuid()}")
        .Options);
}
```

- [ ] **Step 4: 运行测试并确认 discovery service 不存在**

Run:

```powershell
dotnet test tests/Pim.UnitTests/Pim.UnitTests.csproj --filter FullyQualifiedName~OutlookCalendarDiscoveryTests
```

Expected: FAIL，编译错误指向 `OutlookCalendarDiscoveryService`。

- [ ] **Step 5: 实现完整分页收集**

Create `src/modules/Pim.Module.Calendar/Services/OutlookCalendarDiscoveryService.cs` with these records and collection method:

```csharp
using Microsoft.EntityFrameworkCore;
using Pim.Infrastructure.Data;
using Pim.Module.Calendar.DTOs;
using Pim.Module.Calendar.Entities;

namespace Pim.Module.Calendar.Services;

internal sealed record DiscoveredCalendar(GraphCalendarDto Calendar, string? GroupId, string? GroupName);

public sealed class OutlookCalendarDiscoveryService
{
    private readonly PimDbContext _db;
    private readonly IGraphCalendarClient _graph;

    public OutlookCalendarDiscoveryService(PimDbContext db, IGraphCalendarClient graph)
    {
        _db = db;
        _graph = graph;
    }

    private async Task<Dictionary<string, DiscoveredCalendar>> CollectAsync(Guid connectionId, CancellationToken ct)
    {
        var result = new Dictionary<string, DiscoveredCalendar>(StringComparer.Ordinal);
        string? groupsNext = null;
        do
        {
            var groupsPage = await _graph.GetCalendarGroupsPageAsync(connectionId, groupsNext, ct);
            foreach (var group in groupsPage.Value)
            {
                string? calendarsNext = null;
                do
                {
                    var calendarsPage = await _graph.GetGroupCalendarsPageAsync(connectionId, group.Id, calendarsNext, ct);
                    foreach (var calendar in calendarsPage.Value)
                        result.TryAdd(calendar.Id, new DiscoveredCalendar(calendar, group.Id, group.Name));
                    calendarsNext = calendarsPage.NextLink;
                } while (calendarsNext is not null);
            }
            groupsNext = groupsPage.NextLink;
        } while (groupsNext is not null);

        string? rootNext = null;
        do
        {
            var rootPage = await _graph.GetCalendarsPageAsync(connectionId, rootNext, ct);
            foreach (var calendar in rootPage.Value)
                result.TryAdd(calendar.Id, new DiscoveredCalendar(calendar, null, null));
            rootNext = rootPage.NextLink;
        } while (rootNext is not null);

        return result;
    }
}
```

- [ ] **Step 6: 实现原子 upsert 和 stale 标记**

Add `DiscoverAsync` to the service:

```csharp
public async Task<OutlookCalendarDiscoveryResponse> DiscoverAsync(Guid userId, Guid connectionId, CancellationToken ct)
{
    var connection = await _db.Set<OutlookConnectionEntity>()
        .SingleAsync(item => item.Id == connectionId && item.UserId == userId, ct);
    if (connection.Status != "connected")
        throw new InvalidOperationException("Microsoft connection is not ready for calendar discovery.");

    var discovered = await CollectAsync(connectionId, ct);
    var existing = await _db.Set<OutlookCalendarBindingEntity>()
        .Where(item => item.ConnectionId == connectionId)
        .ToDictionaryAsync(item => item.GraphCalendarId, StringComparer.Ordinal, ct);
    var now = DateTimeOffset.UtcNow;
    var newCount = 0;

    foreach (var item in discovered.Values)
    {
        if (!existing.TryGetValue(item.Calendar.Id, out var binding))
        {
            var pimCalendar = new CalendarEntity
            {
                UserId = userId,
                Name = item.Calendar.Name,
                Color = PimColor(item.Calendar.Color),
                Kind = "calendar",
                Source = "outlook",
                IsVisible = true
            };
            binding = new OutlookCalendarBindingEntity
            {
                ConnectionId = connectionId,
                PimCalendarId = pimCalendar.Id,
                GraphCalendarId = item.Calendar.Id,
                IsSelected = true
            };
            _db.Add(pimCalendar);
            _db.Add(binding);
            existing.Add(item.Calendar.Id, binding);
            newCount++;
        }

        binding.GraphGroupId = item.GroupId;
        binding.GraphGroupName = item.GroupName;
        binding.Name = item.Calendar.Name;
        binding.Color = item.Calendar.Color;
        binding.OwnerName = item.Calendar.Owner?.Name;
        binding.OwnerAddress = item.Calendar.Owner?.Address;
        binding.IsDefaultCalendar = item.Calendar.IsDefaultCalendar;
        binding.CanEdit = item.Calendar.CanEdit;
        binding.CanViewPrivateItems = item.Calendar.CanViewPrivateItems;
        binding.RemoteState = "active";
        binding.SyncStrategy = item.Calendar.IsDefaultCalendar ? "default-delta" : "window-reconcile";
        binding.LastDiscoveryAt = now;
        binding.LastErrorCode = null;
        binding.LastErrorMessage = null;
        binding.UpdatedAt = now;
    }

    foreach (var stale in existing.Values.Where(item => !discovered.ContainsKey(item.GraphCalendarId)))
    {
        stale.RemoteState = "remote-missing";
        stale.LastDiscoveryAt = now;
        stale.UpdatedAt = now;
    }

    await _db.SaveChangesAsync(ct);
    var groups = await ListAsync(userId, connectionId, ct);
    return new OutlookCalendarDiscoveryResponse(
        discovered.Count,
        newCount,
        existing.Values.Count(item => item.RemoteState == "remote-missing"),
        groups);
}

private static string PimColor(string? graphColor) => graphColor switch
{
    "lightBlue" => "#3B82F6",
    "lightGreen" => "#22C55E",
    "lightOrange" => "#F59E0B",
    "lightGray" => "#64748B",
    "lightYellow" => "#EAB308",
    "lightTeal" => "#14B8A6",
    "lightPink" => "#EC4899",
    "lightBrown" => "#A16207",
    "lightRed" => "#EF4444",
    "maxColor" => "#6366F1",
    _ => "#2563EB"
};
```

Because collection completes before any tracked entity is changed, a paging exception leaves all old bindings untouched.

- [ ] **Step 7: 实现列表和选择更新**

Add these methods:

```csharp
public async Task<IReadOnlyList<OutlookCalendarGroupResponse>> ListAsync(
    Guid userId,
    Guid connectionId,
    CancellationToken ct)
{
    var ownsConnection = await _db.Set<OutlookConnectionEntity>()
        .AnyAsync(item => item.Id == connectionId && item.UserId == userId, ct);
    if (!ownsConnection) throw new InvalidOperationException("Microsoft connection does not belong to the current user.");

    var bindings = await _db.Set<OutlookCalendarBindingEntity>()
        .AsNoTracking()
        .Where(item => item.ConnectionId == connectionId)
        .OrderBy(item => item.GraphGroupName)
        .ThenBy(item => item.Name)
        .ToListAsync(ct);
    return bindings
        .GroupBy(item => new { item.GraphGroupId, Name = item.GraphGroupName ?? "其他/未分组" })
        .Select(group => new OutlookCalendarGroupResponse(
            group.Key.GraphGroupId,
            group.Key.Name,
            group.Select(Map).ToList()))
        .ToList();
}

public async Task<IReadOnlyList<OutlookCalendarGroupResponse>> UpdateSelectionAsync(
    Guid userId,
    Guid connectionId,
    IReadOnlyCollection<Guid> selectedBindingIds,
    CancellationToken ct)
{
    var bindings = await _db.Set<OutlookCalendarBindingEntity>()
        .Where(item => item.ConnectionId == connectionId)
        .ToListAsync(ct);
    var ownsConnection = await _db.Set<OutlookConnectionEntity>()
        .AnyAsync(item => item.Id == connectionId && item.UserId == userId, ct);
    if (!ownsConnection) throw new InvalidOperationException("Microsoft connection does not belong to the current user.");
    if (selectedBindingIds.Any(id => bindings.All(item => item.Id != id)))
        throw new InvalidOperationException("Selection contains a calendar from another connection.");

    var calendars = await _db.Set<CalendarEntity>()
        .Where(item => bindings.Select(binding => binding.PimCalendarId).Contains(item.Id))
        .ToDictionaryAsync(item => item.Id, ct);
    foreach (var binding in bindings)
    {
        var selected = selectedBindingIds.Contains(binding.Id);
        if (selected && !binding.IsSelected)
        {
            binding.DeltaLink = null;
            binding.BaselineWindowStart = null;
            binding.BaselineWindowEnd = null;
            binding.LastSuccessfulGeneration = null;
        }
        binding.IsSelected = selected;
        binding.UpdatedAt = DateTimeOffset.UtcNow;
        calendars[binding.PimCalendarId].IsVisible = selected;
        calendars[binding.PimCalendarId].UpdatedAt = DateTimeOffset.UtcNow;
    }
    await _db.SaveChangesAsync(ct);
    return await ListAsync(userId, connectionId, ct);
}

private static OutlookCalendarBindingResponse Map(OutlookCalendarBindingEntity item) => new(
    item.Id, item.PimCalendarId, item.GraphCalendarId, item.GraphGroupId,
    item.GraphGroupName ?? "其他/未分组", item.Name, item.Color,
    item.IsDefaultCalendar, item.CanEdit, item.CanViewPrivateItems, item.IsSelected,
    item.RemoteState, item.SyncStrategy, item.LastSyncedAt, item.LastErrorCode, item.LastErrorMessage);
```

- [ ] **Step 8: 隐藏已取消选择的 PIM calendar layer**

In `CalendarService.GetCalendarsAsync`, add the visibility predicate before ordering/projecting:

```csharp
query = query.Where(calendar => calendar.IsVisible);
```

Do not delete the `CalendarEntity` or its events when a binding is unselected.

- [ ] **Step 9: 运行发现测试**

Run:

```powershell
dotnet test tests/Pim.UnitTests/Pim.UnitTests.csproj --filter FullyQualifiedName~OutlookCalendarDiscoveryTests
```

Expected: PASS；课程表和未分组日历都存在，重复 Graph ID 只产生一个 binding，失败不标 remote missing。

- [ ] **Step 10: 提交发现与选择**

```powershell
git add src/modules/Pim.Module.Calendar/DTOs/OutlookSyncDtos.cs src/modules/Pim.Module.Calendar/Services/OutlookCalendarDiscoveryService.cs src/modules/Pim.Module.Calendar/Services/CalendarService.cs tests/Pim.UnitTests/Calendar/OutlookGraphFakes.cs tests/Pim.UnitTests/Calendar/OutlookCalendarDiscoveryTests.cs
git commit -m "feat: discover and select microsoft calendars"
```

Expected: commit 在没有同步事件的情况下已经能可靠列出全部 calendar binding。

## Task 8: 映射 UTC、全天日期和 recurrence 元数据

**Files:**
- Create: `src/modules/Pim.Module.Calendar/Services/OutlookEventMapper.cs`
- Create: `tests/Pim.UnitTests/Calendar/OutlookEventMapperTests.cs`

- [ ] **Step 1: 写时间语义失败测试**

Create `tests/Pim.UnitTests/Calendar/OutlookEventMapperTests.cs`:

```csharp
using System.Text.Json;
using Pim.Module.Calendar.Entities;
using Pim.Module.Calendar.Services;
using Xunit;

namespace Pim.UnitTests.Calendar;

public sealed class OutlookEventMapperTests
{
    private readonly OutlookEventMapper _mapper = new();
    private readonly OutlookCalendarBindingEntity _binding = new()
    {
        Id = Guid.NewGuid(),
        ConnectionId = Guid.NewGuid(),
        PimCalendarId = Guid.NewGuid(),
        GraphCalendarId = "calendar"
    };

    [Fact]
    public void TimedEvent_ParsesOffsetlessGraphValueAsUtc()
    {
        var mapped = _mapper.MapNew(Event(
            start: new GraphDateTimeTimeZoneDto("2026-07-10T01:00:00", "UTC"),
            end: new GraphDateTimeTimeZoneDto("2026-07-10T02:00:00", "UTC"),
            originalStartTimeZone: "China Standard Time",
            originalEndTimeZone: "China Standard Time"),
            _binding,
            Guid.NewGuid());

        Assert.Equal(new DateTimeOffset(2026, 7, 10, 1, 0, 0, TimeSpan.Zero), mapped.DtStart);
        Assert.Equal(TimeSpan.Zero, mapped.DtStart.Offset);
        Assert.Equal("China Standard Time", mapped.OriginalStartTimeZone);
        Assert.Null(mapped.AllDayStartDate);
    }

    [Fact]
    public void AllDayEvent_PreservesExclusiveDatesWithoutUtcPlusEightShift()
    {
        var mapped = _mapper.MapNew(Event(
            isAllDay: true,
            start: new GraphDateTimeTimeZoneDto("2026-07-10T00:00:00.0000000", "UTC"),
            end: new GraphDateTimeTimeZoneDto("2026-07-12T00:00:00.0000000", "UTC")),
            _binding,
            Guid.NewGuid());

        Assert.Equal(new DateOnly(2026, 7, 10), mapped.AllDayStartDate);
        Assert.Equal(new DateOnly(2026, 7, 12), mapped.AllDayEndDateExclusive);
        Assert.Equal(new DateTimeOffset(2026, 7, 10, 0, 0, 0, TimeSpan.Zero), mapped.DtStart);
    }

    [Fact]
    public void RecurrenceMaster_PreservesRawGraphRule()
    {
        using var json = JsonDocument.Parse("""{"pattern":{"type":"weekly","interval":1},"range":{"type":"noEnd","startDate":"2026-07-10"}}""");
        var mapped = _mapper.MapNew(Event(recurrence: json.RootElement.Clone(), type: "seriesMaster"), _binding, Guid.NewGuid());

        Assert.Equal("seriesMaster", mapped.OutlookEventType);
        Assert.Contains("weekly", mapped.GraphRecurrenceJson);
        Assert.Null(mapped.RRule);
    }

    private static GraphEventDto Event(
        bool isAllDay = false,
        GraphDateTimeTimeZoneDto? start = null,
        GraphDateTimeTimeZoneDto? end = null,
        JsonElement? recurrence = null,
        string type = "singleInstance",
        string? originalStartTimeZone = null,
        string? originalEndTimeZone = null) => new(
            "event-1", "课程", "说明",
            start ?? new GraphDateTimeTimeZoneDto("2026-07-10T09:00:00Z", "UTC"),
            end ?? new GraphDateTimeTimeZoneDto("2026-07-10T10:00:00Z", "UTC"),
            isAllDay, "ical-1", null, type, "change-1", "etag-1", DateTimeOffset.UtcNow,
            new GraphLocationDto("教室"), recurrence, null,
            originalStartTimeZone, originalEndTimeZone);
}
```

- [ ] **Step 2: 运行测试并确认 mapper 不存在**

Run:

```powershell
dotnet test tests/Pim.UnitTests/Pim.UnitTests.csproj --filter FullyQualifiedName~OutlookEventMapperTests
```

Expected: FAIL，编译错误指向 `OutlookEventMapper`。

- [ ] **Step 3: 定义稳定的事件快照**

Create the beginning of `src/modules/Pim.Module.Calendar/Services/OutlookEventMapper.cs`:

```csharp
using System.Globalization;
using System.Text.Json;
using Pim.Module.Calendar.Entities;

namespace Pim.Module.Calendar.Services;

public sealed record OutlookEventSnapshot(
    string Title,
    string? Description,
    string? Location,
    DateTimeOffset DtStart,
    DateTimeOffset DtEnd,
    bool IsAllDay,
    DateOnly? AllDayStartDate,
    DateOnly? AllDayEndDateExclusive,
    string? OriginalStartTimeZone,
    string? OriginalEndTimeZone,
    string GraphRecurrenceJson);

public sealed class OutlookEventMapper
{
    public OutlookEventSnapshot Snapshot(GraphEventDto graph)
    {
        if (graph.Start is null || graph.End is null)
            throw new InvalidOperationException($"Graph event {graph.Id} has no start or end.");

        if (graph.IsAllDay)
        {
            var startDate = ParseDate(graph.Start.DateTime);
            var endDate = ParseDate(graph.End.DateTime);
            return new OutlookEventSnapshot(
                graph.Subject ?? "(无标题)", graph.BodyPreview, graph.Location?.DisplayName,
                UtcMidnight(startDate), UtcMidnight(endDate), true,
                startDate, endDate,
                graph.OriginalStartTimeZone ?? graph.Start.TimeZone,
                graph.OriginalEndTimeZone ?? graph.End.TimeZone,
                RawRecurrence(graph.Recurrence));
        }

        return new OutlookEventSnapshot(
            graph.Subject ?? "(无标题)", graph.BodyPreview, graph.Location?.DisplayName,
            ParseUtc(graph.Start.DateTime), ParseUtc(graph.End.DateTime), false,
            null, null,
            graph.OriginalStartTimeZone ?? graph.Start.TimeZone,
            graph.OriginalEndTimeZone ?? graph.End.TimeZone,
            RawRecurrence(graph.Recurrence));
    }

    private static DateTimeOffset ParseUtc(string value)
        => DateTimeOffset.Parse(
            value,
            CultureInfo.InvariantCulture,
            DateTimeStyles.AllowWhiteSpaces | DateTimeStyles.AssumeUniversal | DateTimeStyles.AdjustToUniversal);

    private static DateOnly ParseDate(string value)
        => DateOnly.ParseExact(value[..10], "yyyy-MM-dd", CultureInfo.InvariantCulture);

    private static DateTimeOffset UtcMidnight(DateOnly value)
        => new(value.Year, value.Month, value.Day, 0, 0, 0, TimeSpan.Zero);

    private static string RawRecurrence(JsonElement? recurrence)
        => recurrence is { ValueKind: JsonValueKind.Object } value ? value.GetRawText() : "{}";
}
```

- [ ] **Step 4: 实现新 projection 和快照应用**

Add these methods to `OutlookEventMapper`:

```csharp
public EventEntity MapNew(
    GraphEventDto graph,
    OutlookCalendarBindingEntity binding,
    Guid generation)
{
    var snapshot = Snapshot(graph);
    var entity = new EventEntity
    {
        CalendarId = binding.PimCalendarId,
        Uid = graph.ICalUId ?? $"{graph.Id}@outlook",
        Source = "outlook",
        SourceUid = graph.ICalUId,
        OutlookConnectionId = binding.ConnectionId,
        OutlookCalendarBindingId = binding.Id,
        OutlookEventId = graph.Id,
        OutlookSeriesMasterId = graph.SeriesMasterId,
        OutlookEventType = graph.Type,
        OutlookChangeKey = graph.ChangeKey,
        OutlookEtag = graph.ETag,
        LastSeenSyncGeneration = generation,
        OutlookSyncState = "active",
        CreatedAt = DateTimeOffset.UtcNow,
        UpdatedAt = DateTimeOffset.UtcNow
    };
    Apply(entity, snapshot);
    return entity;
}

public void Apply(EventEntity entity, OutlookEventSnapshot snapshot)
{
    entity.Title = snapshot.Title;
    entity.Description = snapshot.Description;
    entity.Location = snapshot.Location;
    entity.DtStart = snapshot.DtStart.ToUniversalTime();
    entity.DtEnd = snapshot.DtEnd.ToUniversalTime();
    entity.IsAllDay = snapshot.IsAllDay;
    entity.AllDayStartDate = snapshot.AllDayStartDate;
    entity.AllDayEndDateExclusive = snapshot.AllDayEndDateExclusive;
    entity.OriginalStartTimeZone = snapshot.OriginalStartTimeZone;
    entity.OriginalEndTimeZone = snapshot.OriginalEndTimeZone;
    entity.GraphRecurrenceJson = snapshot.GraphRecurrenceJson;
    entity.TimeZoneId = snapshot.IsAllDay ? null : "UTC";
    entity.SourceTimeZoneId = snapshot.OriginalStartTimeZone;
    entity.UpdatedAt = DateTimeOffset.UtcNow;
}

public static OutlookEventSnapshot Snapshot(EventEntity entity) => new(
    entity.Title,
    entity.Description,
    entity.Location,
    entity.DtStart.ToUniversalTime(),
    entity.DtEnd.ToUniversalTime(),
    entity.IsAllDay,
    entity.AllDayStartDate,
    entity.AllDayEndDateExclusive,
    entity.OriginalStartTimeZone,
    entity.OriginalEndTimeZone,
    entity.GraphRecurrenceJson);

public static IReadOnlyList<string> ChangedFields(
    OutlookEventSnapshot before,
    OutlookEventSnapshot after)
{
    var changed = new List<string>();
    if (before.Title != after.Title) changed.Add("title");
    if (before.Description != after.Description) changed.Add("description");
    if (before.Location != after.Location) changed.Add("location");
    if (before.DtStart != after.DtStart) changed.Add("dtStart");
    if (before.DtEnd != after.DtEnd) changed.Add("dtEnd");
    if (before.IsAllDay != after.IsAllDay) changed.Add("isAllDay");
    if (before.AllDayStartDate != after.AllDayStartDate) changed.Add("allDayStartDate");
    if (before.AllDayEndDateExclusive != after.AllDayEndDateExclusive) changed.Add("allDayEndDateExclusive");
    if (before.GraphRecurrenceJson != after.GraphRecurrenceJson) changed.Add("recurrence");
    return changed;
}
```

Never synthesize an `RRule` from Graph recurrence when the PIM recurrence model cannot represent the rule losslessly. The raw Graph JSON is the authority for future confirmed writeback.

- [ ] **Step 5: 运行 mapper 测试**

Run:

```powershell
dotnet test tests/Pim.UnitTests/Pim.UnitTests.csproj --filter FullyQualifiedName~OutlookEventMapperTests
```

Expected: PASS on any host timezone；offsetless UTC `01:00` remains `01:00Z`，原始 `China Standard Time` 元数据保留，全天日期保持 July 10 through exclusive July 12。

- [ ] **Step 6: 提交时间映射**

```powershell
git add src/modules/Pim.Module.Calendar/Services/OutlookEventMapper.cs tests/Pim.UnitTests/Calendar/OutlookEventMapperTests.cs
git commit -m "feat: map outlook event time semantics"
```

Expected: commit 只解释 Graph event，不发起同步或写回。

## Task 9: 在二级确认事务内创建 durable execution

**Files:**
- Create: `src/modules/Pim.Module.Calendar/Services/OutlookOperationOutboxWriter.cs`
- Create: `tests/Pim.UnitTests/Operations/ConfirmedOperationOutboxTests.cs`
- Modify: `src/Pim.Core/Operations/ConfirmationDtos.cs`
- Modify: `src/Pim.Infrastructure/Operations/OperationConfirmationService.cs`
- Modify: `src/Pim.Infrastructure/Data/PimDbContext.cs`
- Modify: `src/Pim.Infrastructure/Data/Migrations/20260710000000_MicrosoftCalendarSync.cs`
- Modify: `src/Pim.Infrastructure/Data/Migrations/PimDbContextModelSnapshot.cs`

- [ ] **Step 1: 定义确认事务扩展点**

Append to `src/Pim.Core/Operations/ConfirmationDtos.cs`:

```csharp
public sealed record ConfirmedOperationContext(
    Guid ConfirmationId,
    Guid? RequestedByUserId,
    string OperationType,
    string PayloadJson,
    string PreviewJson,
    DateTimeOffset ConfirmedAt);

public interface IConfirmedOperationOutboxWriter
{
    bool CanHandle(string operationType);
    Task EnqueueAsync(ConfirmedOperationContext operation, CancellationToken ct);
}
```

- [ ] **Step 2: 写原子 outbox 失败测试**

Create `tests/Pim.UnitTests/Operations/ConfirmedOperationOutboxTests.cs`:

```csharp
using Microsoft.EntityFrameworkCore;
using Pim.Core.Operations;
using Pim.Infrastructure.Data;
using Pim.Infrastructure.Operations;
using Pim.Module.Calendar.Entities;
using Pim.Module.Calendar.Services;
using Xunit;

namespace Pim.UnitTests.Operations;

public sealed class ConfirmedOperationOutboxTests
{
    [Fact]
    public async Task SecondLevelConfirmation_AddsQueuedExecutionInSameSave()
    {
        await using var db = CreateDb();
        var userId = Guid.NewGuid();
        var service = new OperationConfirmationService(db, [new OutlookOperationOutboxWriter(db)]);
        var created = await service.CreateAsync(Request(userId));

        var confirmed = await service.ConfirmSecondLevelAsync(created.Id, userId);

        var execution = await db.Set<OutlookOperationExecutionEntity>().SingleAsync();
        Assert.Equal(OperationConfirmationStatus.Confirmed, confirmed.Status);
        Assert.Equal(created.Id, execution.ConfirmationId);
        Assert.Equal("queued", execution.State);
        Assert.Equal(new string('a', 64), execution.ProposedHash);
    }

    [Fact]
    public async Task OutboxFailure_DoesNotPersistConfirmedStatus()
    {
        await using var db = CreateDb();
        var userId = Guid.NewGuid();
        var service = new OperationConfirmationService(db, [new ThrowingWriter()]);
        var created = await service.CreateAsync(Request(userId));

        await Assert.ThrowsAsync<InvalidOperationException>(() =>
            service.ConfirmSecondLevelAsync(created.Id, userId));
        db.ChangeTracker.Clear();

        Assert.Equal(OperationConfirmationStatus.Pending.ToString(),
            await db.OperationConfirmations.Where(item => item.Id == created.Id).Select(item => item.Status).SingleAsync());
        Assert.Empty(await db.Set<OutlookOperationExecutionEntity>().ToListAsync());
    }

    private static CreateOperationConfirmationRequest Request(Guid userId) => new(
        userId,
        "outlook.event.update",
        "Update Outlook event",
        OperationRiskLevel.L3ExternalSourceOrWriteback,
        "outlook",
        $$"""{"eventId":"{{Guid.NewGuid()}}","proposedHash":"{{new string('a', 64)}}"}""",
        "{}",
        DateTimeOffset.UtcNow.AddHours(1),
        Guid.NewGuid().ToString(),
        RequiresSecondLevelConfirmation: true);

    private static PimDbContext CreateDb()
    {
        PimDbContext.RegisterModuleAssembly(typeof(OutlookOperationExecutionEntity).Assembly);
        return new PimDbContext(new DbContextOptionsBuilder<PimDbContext>()
            .UseInMemoryDatabase($"confirmation-outbox-{Guid.NewGuid()}")
            .Options);
    }

    private sealed class ThrowingWriter : IConfirmedOperationOutboxWriter
    {
        public bool CanHandle(string operationType) => true;
        public Task EnqueueAsync(ConfirmedOperationContext operation, CancellationToken ct)
            => Task.FromException(new InvalidOperationException("outbox unavailable"));
    }
}
```

- [ ] **Step 3: 运行测试并确认 service 没有 writer 支持**

Run:

```powershell
dotnet test tests/Pim.UnitTests/Pim.UnitTests.csproj --filter FullyQualifiedName~ConfirmedOperationOutboxTests
```

Expected: FAIL，`OperationConfirmationService` 构造函数或 `OutlookOperationOutboxWriter` 不存在。

- [ ] **Step 4: 让 confirmation service 在同一次 SaveChanges 前调用 writer**

Change the constructor and `ConfirmWithModeAsync` in `OperationConfirmationService`:

```csharp
private readonly IReadOnlyList<IConfirmedOperationOutboxWriter> _outboxWriters;

public OperationConfirmationService(
    PimDbContext db,
    IEnumerable<IConfirmedOperationOutboxWriter>? outboxWriters = null)
{
    _db = db;
    _outboxWriters = outboxWriters?.ToList() ?? [];
}
```

Replace the confirmation save block with:

```csharp
var confirmedAt = DateTimeOffset.UtcNow;
entity.Status = OperationConfirmationStatus.Confirmed.ToString();
entity.ConfirmedAt = confirmedAt;

var matchingWriters = _outboxWriters.Where(writer => writer.CanHandle(entity.OperationType)).ToList();
if (matchingWriters.Count > 1)
    throw new InvalidOperationException($"Multiple outbox writers handle {entity.OperationType}.");
if (matchingWriters.Count == 1)
{
    await matchingWriters[0].EnqueueAsync(new ConfirmedOperationContext(
        entity.Id,
        entity.RequestedByUserId,
        entity.OperationType,
        entity.PayloadJson,
        entity.PreviewJson,
        confirmedAt), ct);
}

await _db.SaveChangesAsync(ct);
return Map(entity);
```

There must be no earlier `SaveChangesAsync` between changing the confirmation status and adding the execution entity.

- [ ] **Step 5: 实现 Outlook outbox writer**

Create `src/modules/Pim.Module.Calendar/Services/OutlookOperationOutboxWriter.cs`:

```csharp
using System.Text.Json;
using Microsoft.EntityFrameworkCore;
using Pim.Core.Operations;
using Pim.Infrastructure.Data;
using Pim.Module.Calendar.Entities;

namespace Pim.Module.Calendar.Services;

public sealed class OutlookOperationOutboxWriter : IConfirmedOperationOutboxWriter
{
    private static readonly HashSet<string> Supported = new(StringComparer.Ordinal)
    {
        "outlook.event.update",
        "outlook.event.delete",
        "outlook.event.pull-update",
        "outlook.event.pull-delete",
        "outlook.event.resolve-conflict"
    };

    private readonly PimDbContext _db;

    public OutlookOperationOutboxWriter(PimDbContext db) => _db = db;

    public bool CanHandle(string operationType) => Supported.Contains(operationType);

    public async Task EnqueueAsync(ConfirmedOperationContext operation, CancellationToken ct)
    {
        if (operation.RequestedByUserId is not { } userId)
            throw new InvalidOperationException("Outlook operations must belong to a PIM user.");
        using var payload = JsonDocument.Parse(operation.PayloadJson);
        var hash = payload.RootElement.GetProperty("proposedHash").GetString();
        if (hash is null || hash.Length != 64)
            throw new InvalidOperationException("Outlook operation payload has no valid proposed hash.");
        if (await _db.Set<OutlookOperationExecutionEntity>()
            .AnyAsync(item => item.ConfirmationId == operation.ConfirmationId, ct)) return;

        _db.Add(new OutlookOperationExecutionEntity
        {
            ConfirmationId = operation.ConfirmationId,
            UserId = userId,
            OperationType = operation.OperationType,
            ProposedHash = hash,
            PayloadJson = operation.PayloadJson,
            State = "queued",
            NextAttemptAt = operation.ConfirmedAt,
            CreatedAt = operation.ConfirmedAt,
            UpdatedAt = operation.ConfirmedAt
        });
    }
}
```

The writer intentionally does not call `SaveChangesAsync`; the confirmation service owns the transaction boundary.

- [ ] **Step 6: 注册 writer**

In `CalendarModule.RegisterServices` add:

```csharp
services.AddScoped<IConfirmedOperationOutboxWriter, OutlookOperationOutboxWriter>();
```

- [ ] **Step 7: 使审计按 confirmation 幂等**

Replace the existing `AuditVersionEntity.ConfirmationId` index in `PimDbContext.OnModelCreating` with:

```csharp
e.HasIndex(a => a.ConfirmationId)
    .IsUnique()
    .HasFilter("\"confirmation_id\" IS NOT NULL");
```

In `MicrosoftCalendarSync.Up`, replace the old non-unique index:

```csharp
migrationBuilder.DropIndex("IX_audit_versions_confirmation_id", "audit_versions");
migrationBuilder.CreateIndex(
    "IX_audit_versions_confirmation_id",
    "audit_versions",
    "confirmation_id",
    unique: true,
    filter: "\"confirmation_id\" IS NOT NULL");
```

In `Down`, drop that index and recreate the original non-unique index:

```csharp
migrationBuilder.DropIndex("IX_audit_versions_confirmation_id", "audit_versions");
migrationBuilder.CreateIndex("IX_audit_versions_confirmation_id", "audit_versions", "confirmation_id");
```

Regenerate only `PimDbContextModelSnapshot.cs` using the temporary-snapshot procedure from Task 3.

- [ ] **Step 8: 运行 outbox 和现有 confirmation 测试**

Run:

```powershell
dotnet test tests/Pim.UnitTests/Pim.UnitTests.csproj --filter "FullyQualifiedName~ConfirmedOperationOutboxTests|FullyQualifiedName~AuditAndConfirmationServiceTests|FullyQualifiedName~ScheduleFactConfirmationGateTests"
```

Expected: PASS；非 Outlook confirmations 仍可在没有 writer 的情况下正常确认。

- [ ] **Step 9: 提交 durable confirmation 基础**

```powershell
git add src/Pim.Core/Operations/ConfirmationDtos.cs src/Pim.Infrastructure/Operations/OperationConfirmationService.cs src/Pim.Infrastructure/Data/PimDbContext.cs src/Pim.Infrastructure/Data/Migrations src/modules/Pim.Module.Calendar/CalendarModule.cs src/modules/Pim.Module.Calendar/Services/OutlookOperationOutboxWriter.cs tests/Pim.UnitTests/Operations/ConfirmedOperationOutboxTests.cs
git commit -m "feat: enqueue outlook operations on confirmation"
```

Expected: confirmation 状态与 execution 是同一数据库提交；Hangfire 尚未参与正确性。

## Task 10: 封死普通编辑旁路并创建 Outlook 变更预览

**Files:**
- Create: `src/modules/Pim.Module.Calendar/Services/OutlookChangePreviewService.cs`
- Create: `tests/Pim.UnitTests/Calendar/OutlookChangePreviewTests.cs`
- Modify: `src/modules/Pim.Module.Calendar/DTOs/OutlookSyncDtos.cs`
- Modify: `src/modules/Pim.Module.Calendar/Services/CalendarService.cs`
- Modify: `src/modules/Pim.Module.Calendar/Services/CalendarDeleteService.cs`

- [ ] **Step 1: 定义 Outlook 编辑和执行 payload**

Append to `OutlookSyncDtos.cs`:

```csharp
public sealed record OutlookEventChangeRequest(
    [Required, MaxLength(255)] string Title,
    string? Description,
    [MaxLength(500)] string? Location,
    DateTimeOffset? DtStart,
    DateTimeOffset? DtEnd,
    bool IsAllDay,
    DateOnly? AllDayStartDate,
    DateOnly? AllDayEndDateExclusive);

public sealed record CopyOutlookEventRequest(Guid? TargetCalendarId);

public sealed record OutlookConfirmedOperationPayload(
    Guid UserId,
    Guid ConnectionId,
    Guid BindingId,
    Guid PimEventId,
    string GraphCalendarId,
    string GraphEventId,
    string? ExpectedEtag,
    string? ExpectedChangeKey,
    string ProposedHash,
    string Action,
    OutlookEventSnapshot Before,
    OutlookEventSnapshot? Proposed);
```

Add `using Pim.Module.Calendar.Services;` because the payload serializes the stable snapshot record.

- [ ] **Step 2: 写 preview、只读和普通入口门禁测试**

Create `tests/Pim.UnitTests/Calendar/OutlookChangePreviewTests.cs`:

```csharp
using Microsoft.EntityFrameworkCore;
using Pim.Core.Exceptions;
using Pim.Core.Operations;
using Pim.Infrastructure.Auth;
using Pim.Infrastructure.Data;
using Pim.Infrastructure.Operations;
using Pim.Module.Calendar.DTOs;
using Pim.Module.Calendar.Entities;
using Pim.Module.Calendar.Services;
using Xunit;

namespace Pim.UnitTests.Calendar;

public sealed class OutlookChangePreviewTests
{
    [Fact]
    public async Task PreviewUpdate_CreatesL3SecondLevelConfirmationWithoutMutatingEvent()
    {
        await using var db = CreateDb();
        var seeded = Seed(db, canEdit: true);
        var confirmations = new OperationConfirmationService(db);
        var service = new OutlookChangePreviewService(db, confirmations, new OutlookEventMapper());

        var preview = await service.PreviewUpdateAsync(seeded.UserId, seeded.EventId, new OutlookEventChangeRequest(
            "新标题", "新说明", "新地点",
            new DateTimeOffset(2026, 7, 10, 3, 0, 0, TimeSpan.Zero),
            new DateTimeOffset(2026, 7, 10, 4, 0, 0, TimeSpan.Zero),
            false, null, null), CancellationToken.None);

        Assert.Equal(OperationRiskLevel.L3ExternalSourceOrWriteback, preview.RiskLevel);
        Assert.True(preview.RequiresSecondLevelConfirmation);
        Assert.Equal("outlook.event.update", preview.OperationType);
        Assert.Equal("旧标题", (await db.Set<EventEntity>().SingleAsync()).Title);
        Assert.Contains("新标题", preview.AfterJson);
    }

    [Fact]
    public async Task ReadOnlyBinding_RejectsWritebackButCanCopyToManualCalendar()
    {
        await using var db = CreateDb();
        var seeded = Seed(db, canEdit: false);
        var service = new OutlookChangePreviewService(db, new OperationConfirmationService(db), new OutlookEventMapper());

        await Assert.ThrowsAsync<DomainException>(() => service.PreviewDeleteAsync(
            seeded.UserId, seeded.EventId, CancellationToken.None));
        var copy = await service.CopyToPimAsync(seeded.UserId, seeded.EventId, null, CancellationToken.None);

        Assert.Equal("manual", copy.Source);
        var stored = await db.Set<EventEntity>().SingleAsync(item => item.Id == copy.Id);
        Assert.Null(stored.OutlookCalendarBindingId);
        Assert.Null(stored.OutlookEventId);
    }

    [Fact]
    public async Task NormalCalendarUpdate_RejectsOutlookProjection()
    {
        await using var db = CreateDb();
        var seeded = Seed(db, canEdit: true);
        var service = new CalendarService(
            db,
            new FixedCurrentUser(seeded.UserId),
            new RecurrenceService(NullLogger<RecurrenceService>.Instance));

        var exception = await Assert.ThrowsAsync<DomainException>(() => service.UpdateEventAsync(
            seeded.EventId,
            new UpdateEventRequest(Guid.Empty, "绕过", null, null, DateTimeOffset.UtcNow, DateTimeOffset.UtcNow.AddHours(1), null),
            CancellationToken.None));

        Assert.Equal(2041, exception.Code);
        Assert.Equal("旧标题", (await db.Set<EventEntity>().SingleAsync()).Title);
    }
}
```

Insert these helpers before the test class's closing brace:

```csharp
private sealed record SeedResult(Guid UserId, Guid EventId);

private static SeedResult Seed(PimDbContext db, bool canEdit)
{
    var userId = Guid.NewGuid();
    var manual = new CalendarEntity
    {
        UserId = userId,
        Name = "默认日历",
        Source = "manual",
        IsDefault = true
    };
    var outlook = new CalendarEntity
    {
        UserId = userId,
        Name = "Outlook",
        Source = "outlook"
    };
    var connection = new OutlookConnectionEntity
    {
        UserId = userId,
        ClientId = Guid.NewGuid().ToString(),
        Status = "connected",
        TokenHealth = "healthy"
    };
    var binding = new OutlookCalendarBindingEntity
    {
        ConnectionId = connection.Id,
        PimCalendarId = outlook.Id,
        GraphCalendarId = "graph-calendar",
        Name = "Outlook",
        CanEdit = canEdit,
        IsSelected = true
    };
    var evt = new EventEntity
    {
        Calendar = outlook,
        CalendarId = outlook.Id,
        Uid = "ical-event",
        Title = "旧标题",
        DtStart = new DateTimeOffset(2026, 7, 10, 1, 0, 0, TimeSpan.Zero),
        DtEnd = new DateTimeOffset(2026, 7, 10, 2, 0, 0, TimeSpan.Zero),
        Source = "outlook",
        OutlookConnectionId = connection.Id,
        OutlookCalendarBindingId = binding.Id,
        OutlookEventId = "graph-event",
        OutlookChangeKey = "change-before",
        OutlookEtag = "etag-before"
    };
    db.AddRange(manual, outlook, connection, binding, evt);
    db.SaveChanges();
    return new SeedResult(userId, evt.Id);
}

private static PimDbContext CreateDb()
{
    PimDbContext.RegisterModuleAssembly(typeof(OutlookConnectionEntity).Assembly);
    return new PimDbContext(new DbContextOptionsBuilder<PimDbContext>()
        .UseInMemoryDatabase($"outlook-preview-{Guid.NewGuid()}")
        .Options);
}

private sealed class FixedCurrentUser(Guid userId) : ICurrentUserService
{
    public Guid? UserId { get; } = userId;
    public string? Role => "user";
}
```

Add `using Microsoft.Extensions.Logging.Abstractions;`.

- [ ] **Step 3: 运行测试并确认 preview service 不存在**

Run:

```powershell
dotnet test tests/Pim.UnitTests/Pim.UnitTests.csproj --filter FullyQualifiedName~OutlookChangePreviewTests
```

Expected: FAIL，编译错误指向 `OutlookChangePreviewService`。

- [ ] **Step 4: 在普通更新/删除入口增加后端硬门禁**

Immediately after loading the event in `CalendarService.UpdateEventAsync`, add:

```csharp
if (entity.OutlookCalendarBindingId is not null
    || string.Equals(entity.Source, "outlook", StringComparison.OrdinalIgnoreCase))
{
    throw new DomainException(02041, "Outlook 日程必须通过变更预览和二级确认后回写。");
}
```

Immediately after loading the event in `CalendarDeleteService.DeleteEventAsync`, add:

```csharp
if (evt.OutlookCalendarBindingId is not null
    || string.Equals(evt.Source, "outlook", StringComparison.OrdinalIgnoreCase))
{
    throw new DomainException(02042, "Outlook 日程必须通过删除预览和二级确认后删除。");
}
```

Before mutating any row in `BatchDeleteEventsAsync`, reject a batch containing Outlook projections:

```csharp
if (events.Any(evt => evt.OutlookCalendarBindingId is not null
    || string.Equals(evt.Source, "outlook", StringComparison.OrdinalIgnoreCase)))
{
    throw new DomainException(02043, "批量删除包含 Outlook 日程；请使用 L4 治理操作。");
}
```

- [ ] **Step 5: 实现规范化请求和 proposed hash**

Create `src/modules/Pim.Module.Calendar/Services/OutlookChangePreviewService.cs`:

```csharp
using System.Security.Cryptography;
using System.Text;
using System.Text.Json;
using Microsoft.EntityFrameworkCore;
using Pim.Core.Exceptions;
using Pim.Core.Operations;
using Pim.Infrastructure.Data;
using Pim.Module.Calendar.DTOs;
using Pim.Module.Calendar.Entities;

namespace Pim.Module.Calendar.Services;

public sealed class OutlookChangePreviewService
{
    private static readonly JsonSerializerOptions JsonOptions = new(JsonSerializerDefaults.Web);
    private readonly PimDbContext _db;
    private readonly IOperationConfirmationService _confirmations;
    private readonly OutlookEventMapper _mapper;

    public OutlookChangePreviewService(
        PimDbContext db,
        IOperationConfirmationService confirmations,
        OutlookEventMapper mapper)
    {
        _db = db;
        _confirmations = confirmations;
        _mapper = mapper;
    }

    private static OutlookEventSnapshot Proposed(OutlookEventChangeRequest request)
    {
        if (request.IsAllDay)
        {
            if (request.AllDayStartDate is not { } start || request.AllDayEndDateExclusive is not { } end || end <= start)
                throw new DomainException(02044, "全天日程必须提供有效的开始日期和排他结束日期。");
            return new OutlookEventSnapshot(
                request.Title, request.Description, request.Location,
                new DateTimeOffset(start.Year, start.Month, start.Day, 0, 0, 0, TimeSpan.Zero),
                new DateTimeOffset(end.Year, end.Month, end.Day, 0, 0, 0, TimeSpan.Zero),
                true, start, end, "UTC", "UTC", "{}");
        }

        if (request.DtStart is not { } timedStart || request.DtEnd is not { } timedEnd || timedEnd <= timedStart)
            throw new DomainException(02045, "定时日程必须提供有效的开始和结束时间。");
        return new OutlookEventSnapshot(
            request.Title, request.Description, request.Location,
            timedStart.ToUniversalTime(), timedEnd.ToUniversalTime(),
            false, null, null, "UTC", "UTC", "{}");
    }

    private static string Hash(OutlookEventSnapshot? snapshot)
        => Convert.ToHexString(SHA256.HashData(Encoding.UTF8.GetBytes(
            JsonSerializer.Serialize(snapshot, JsonOptions)))).ToLowerInvariant();
}
```

- [ ] **Step 6: 实现 update/delete preview**

Add these methods:

```csharp
public async Task<OperationConfirmationDto> PreviewUpdateAsync(
    Guid userId,
    Guid eventId,
    OutlookEventChangeRequest request,
    CancellationToken ct)
{
    var loaded = await LoadAsync(userId, eventId, ct);
    EnsureEditable(loaded.Binding);
    var before = OutlookEventMapper.Snapshot(loaded.Event);
    var proposed = Proposed(request) with
    {
        OriginalStartTimeZone = before.OriginalStartTimeZone,
        OriginalEndTimeZone = before.OriginalEndTimeZone,
        GraphRecurrenceJson = before.GraphRecurrenceJson
    };
    var changed = OutlookEventMapper.ChangedFields(before, proposed);
    if (changed.Count == 0) throw new DomainException(02046, "没有可回写的变更。");
    var hash = Hash(proposed);
    var payload = Payload(loaded, userId, "update", hash, before, proposed);
    var decision = ScheduleFactConfirmationPolicy.Classify("outlook", changed, externalWriteback: true);
    return await _confirmations.CreateAsync(new CreateOperationConfirmationRequest(
        userId,
        "outlook.event.update",
        $"回写 Outlook 日程“{loaded.Event.Title}”",
        decision.RiskLevel,
        "outlook",
        JsonSerializer.Serialize(payload, JsonOptions),
        "{}",
        DateTimeOffset.UtcNow.AddHours(1),
        $"outlook-update-{eventId}-{hash}",
        changed,
        ["confirm", "reject"],
        "event",
        eventId,
        decision.RequiresSecondLevelConfirmation,
        JsonSerializer.Serialize(before, JsonOptions),
        JsonSerializer.Serialize(proposed, JsonOptions),
        decision.RequiresStrictConfirmation,
        ExternalEffect: "Microsoft Graph PATCH",
        RecoveryPath: "读取远端状态后恢复，未知结果不会盲目重试。"), ct);
}

public async Task<OperationConfirmationDto> PreviewDeleteAsync(Guid userId, Guid eventId, CancellationToken ct)
{
    var loaded = await LoadAsync(userId, eventId, ct);
    EnsureEditable(loaded.Binding);
    var before = OutlookEventMapper.Snapshot(loaded.Event);
    var hash = Hash(null);
    var payload = Payload(loaded, userId, "delete", hash, before, null);
    var decision = ScheduleFactConfirmationPolicy.Classify("outlook", ["delete"], externalWriteback: true);
    return await _confirmations.CreateAsync(new CreateOperationConfirmationRequest(
        userId,
        "outlook.event.delete",
        $"从 Outlook 删除日程“{loaded.Event.Title}”",
        decision.RiskLevel,
        "outlook",
        JsonSerializer.Serialize(payload, JsonOptions),
        "{}",
        DateTimeOffset.UtcNow.AddHours(1),
        $"outlook-delete-{eventId}-{loaded.Event.OutlookEtag}",
        ["delete"], ["confirm", "reject"], "event", eventId,
        decision.RequiresSecondLevelConfirmation,
        JsonSerializer.Serialize(before, JsonOptions),
        "null",
        decision.RequiresStrictConfirmation,
        ExternalEffect: "Microsoft Graph DELETE",
        RecoveryPath: "删除结果未知时先读取远端事件。"), ct);
}
```

- [ ] **Step 7: 实现加载、payload 和只读复制**

Add the remaining methods:

```csharp
private async Task<LoadedOutlookEvent> LoadAsync(Guid userId, Guid eventId, CancellationToken ct)
{
    var evt = await _db.Set<EventEntity>()
        .Include(item => item.Calendar)
        .SingleOrDefaultAsync(item => item.Id == eventId && item.Calendar.UserId == userId, ct)
        ?? throw new DomainException(02001, "日程不存在");
    if (evt.OutlookCalendarBindingId is not { } bindingId || evt.OutlookConnectionId is not { } connectionId || string.IsNullOrWhiteSpace(evt.OutlookEventId))
        throw new DomainException(02047, "该日程没有可用的 Outlook 外部身份。");
    var binding = await _db.Set<OutlookCalendarBindingEntity>()
        .SingleAsync(item => item.Id == bindingId && item.ConnectionId == connectionId, ct);
    return new LoadedOutlookEvent(evt, binding);
}

private static void EnsureEditable(OutlookCalendarBindingEntity binding)
{
    if (!binding.CanEdit) throw new DomainException(02048, "该 Outlook 日历为只读；可以复制为 PIM 日程。");
}

private static OutlookConfirmedOperationPayload Payload(
    LoadedOutlookEvent loaded,
    Guid userId,
    string action,
    string hash,
    OutlookEventSnapshot before,
    OutlookEventSnapshot? proposed) => new(
        userId,
        loaded.Binding.ConnectionId,
        loaded.Binding.Id,
        loaded.Event.Id,
        loaded.Binding.GraphCalendarId,
        loaded.Event.OutlookEventId!,
        loaded.Event.OutlookEtag,
        loaded.Event.OutlookChangeKey,
        hash,
        action,
        before,
        proposed);

public async Task<EventEntity> CopyToPimAsync(
    Guid userId,
    Guid eventId,
    Guid? targetCalendarId,
    CancellationToken ct)
{
    var loaded = await LoadAsync(userId, eventId, ct);
    var calendar = targetCalendarId is { } target
        ? await _db.Set<CalendarEntity>().SingleAsync(item => item.Id == target && item.UserId == userId && item.Source == "manual", ct)
        : await _db.Set<CalendarEntity>().FirstAsync(item => item.UserId == userId && item.Source == "manual" && item.Kind == "calendar", ct);
    var source = loaded.Event;
    var copy = new EventEntity
    {
        CalendarId = calendar.Id,
        Uid = $"{Guid.NewGuid():N}@pim",
        Title = source.Title,
        Description = source.Description,
        Location = source.Location,
        DtStart = source.DtStart,
        DtEnd = source.DtEnd,
        IsAllDay = source.IsAllDay,
        AllDayStartDate = source.AllDayStartDate,
        AllDayEndDateExclusive = source.AllDayEndDateExclusive,
        Source = "manual",
        TimeZoneId = source.TimeZoneId,
        CreatedAt = DateTimeOffset.UtcNow,
        UpdatedAt = DateTimeOffset.UtcNow
    };
    _db.Add(copy);
    await _db.SaveChangesAsync(ct);
    return copy;
}

private sealed record LoadedOutlookEvent(EventEntity Event, OutlookCalendarBindingEntity Binding);
```

- [ ] **Step 8: 运行 preview 和门禁测试**

Run:

```powershell
dotnet test tests/Pim.UnitTests/Pim.UnitTests.csproj --filter "FullyQualifiedName~OutlookChangePreviewTests|FullyQualifiedName~OutlookSourceGovernanceTests"
```

Expected: PASS；preview 前事件不变，只读 binding 无 PATCH/DELETE 路径，普通更新稳定返回 2041。

- [ ] **Step 9: 提交 preview 与硬门禁**

```powershell
git add src/modules/Pim.Module.Calendar/DTOs/OutlookSyncDtos.cs src/modules/Pim.Module.Calendar/Services/OutlookChangePreviewService.cs src/modules/Pim.Module.Calendar/Services/CalendarService.cs src/modules/Pim.Module.Calendar/Services/CalendarDeleteService.cs tests/Pim.UnitTests/Calendar/OutlookChangePreviewTests.cs
git commit -m "feat: require confirmation for outlook event changes"
```

Expected: 生产普通 PUT/DELETE 已不能绕过确认；Graph 尚未被修改。

## Task 11: 执行 Graph-first 写入、ETag 冲突和幂等恢复

**Files:**
- Create: `src/modules/Pim.Module.Calendar/Services/OutlookConfirmedOperationHandler.cs`
- Create: `tests/Pim.UnitTests/Calendar/OutlookConfirmedOperationHandlerTests.cs`
- Modify: `tests/Pim.UnitTests/Calendar/OutlookGraphFakes.cs`

- [ ] **Step 1: 写 Graph-first 和恢复失败测试**

Create `tests/Pim.UnitTests/Calendar/OutlookConfirmedOperationHandlerTests.cs` with these three tests:

```csharp
[Fact]
public async Task Update_PatchesGraphBeforeCommittingEventAndAudit()
{
    await using var db = CreateDb();
    var seed = SeedExecution(db, action: "update", remoteEtag: "etag-before");
    var graph = GraphWithRemote(seed, "旧标题", "etag-before");
    var handler = new OutlookConfirmedOperationHandler(db, graph, new OutlookEventMapper());

    await handler.ExecuteAsync(seed.ExecutionId, CancellationToken.None);

    Assert.Single(graph.Patches);
    Assert.Equal("新标题", (await db.Set<EventEntity>().SingleAsync()).Title);
    Assert.Equal("completed", (await db.Set<OutlookOperationExecutionEntity>().SingleAsync()).State);
    Assert.Equal(OperationConfirmationStatus.Executed.ToString(),
        (await db.OperationConfirmations.SingleAsync()).Status);
    Assert.Equal(seed.ConfirmationId, (await db.AuditVersions.SingleAsync()).ConfirmationId);
}

[Fact]
public async Task ChangedRemoteEtag_MovesExecutionToConflictWithoutWritingEitherSide()
{
    await using var db = CreateDb();
    var seed = SeedExecution(db, action: "update", remoteEtag: "etag-before");
    var graph = GraphWithRemote(seed, "第三方新标题", "etag-newer");
    var handler = new OutlookConfirmedOperationHandler(db, graph, new OutlookEventMapper());

    await handler.ExecuteAsync(seed.ExecutionId, CancellationToken.None);

    Assert.Empty(graph.Patches);
    Assert.Equal("旧标题", (await db.Set<EventEntity>().SingleAsync()).Title);
    Assert.Equal("conflict", (await db.Set<OutlookOperationExecutionEntity>().SingleAsync()).State);
    Assert.Single(await db.Set<SyncConflictEntity>().ToListAsync());
}

[Fact]
public async Task RemoteAlreadyMatchesProposal_SkipsSecondPatchAndRepairsLocalCommit()
{
    await using var db = CreateDb();
    var seed = SeedExecution(db, action: "update", remoteEtag: "etag-before");
    var graph = GraphWithRemote(seed, "新标题", "etag-after");
    var handler = new OutlookConfirmedOperationHandler(db, graph, new OutlookEventMapper());

    await handler.ExecuteAsync(seed.ExecutionId, CancellationToken.None);

    Assert.Empty(graph.Patches);
    Assert.Equal("新标题", (await db.Set<EventEntity>().SingleAsync()).Title);
    Assert.Equal("completed", (await db.Set<OutlookOperationExecutionEntity>().SingleAsync()).State);
}
```

Insert these helpers in `OutlookConfirmedOperationHandlerTests`:

```csharp
private static readonly JsonSerializerOptions WebJson = new(JsonSerializerDefaults.Web);

private sealed record ExecutionSeed(
    Guid UserId,
    Guid ExecutionId,
    Guid ConfirmationId,
    string GraphCalendarId,
    string GraphEventId);

private static ExecutionSeed SeedExecution(PimDbContext db, string action, string remoteEtag)
{
    var userId = Guid.NewGuid();
    var connection = new OutlookConnectionEntity
    {
        UserId = userId,
        ClientId = Guid.NewGuid().ToString(),
        Status = "connected",
        TokenHealth = "healthy"
    };
    var calendar = new CalendarEntity { UserId = userId, Name = "Outlook", Source = "outlook" };
    var binding = new OutlookCalendarBindingEntity
    {
        ConnectionId = connection.Id,
        PimCalendarId = calendar.Id,
        GraphCalendarId = "graph-calendar",
        Name = "Outlook",
        CanEdit = true,
        IsSelected = true
    };
    var evt = new EventEntity
    {
        CalendarId = calendar.Id,
        Uid = "ical-event",
        Title = "旧标题",
        DtStart = new DateTimeOffset(2026, 7, 10, 1, 0, 0, TimeSpan.Zero),
        DtEnd = new DateTimeOffset(2026, 7, 10, 2, 0, 0, TimeSpan.Zero),
        Source = "outlook",
        OutlookConnectionId = connection.Id,
        OutlookCalendarBindingId = binding.Id,
        OutlookEventId = "graph-event",
        OutlookChangeKey = "change-before",
        OutlookEtag = remoteEtag,
        OriginalStartTimeZone = "UTC",
        OriginalEndTimeZone = "UTC",
        GraphRecurrenceJson = "{}"
    };
    var before = OutlookEventMapper.Snapshot(evt);
    var proposed = action == "delete" ? null : before with { Title = "新标题" };
    var hash = Convert.ToHexString(SHA256.HashData(Encoding.UTF8.GetBytes(
        JsonSerializer.Serialize(proposed, WebJson)))).ToLowerInvariant();
    var confirmation = new OperationConfirmationEntity
    {
        RequestedByUserId = userId,
        OperationType = action == "delete" ? "outlook.event.delete" : "outlook.event.update",
        Summary = "Confirmed Outlook operation",
        RiskLevel = OperationRiskLevel.L3ExternalSourceOrWriteback.ToString(),
        Source = "outlook",
        PayloadJson = "{}",
        PreviewJson = "{}",
        Status = OperationConfirmationStatus.Confirmed.ToString(),
        ExpiresAt = DateTimeOffset.UtcNow.AddHours(1),
        ConfirmedAt = DateTimeOffset.UtcNow
    };
    var payload = new OutlookConfirmedOperationPayload(
        userId, connection.Id, binding.Id, evt.Id, binding.GraphCalendarId,
        evt.OutlookEventId, remoteEtag, evt.OutlookChangeKey, hash, action, before, proposed);
    var execution = new OutlookOperationExecutionEntity
    {
        ConfirmationId = confirmation.Id,
        UserId = userId,
        OperationType = confirmation.OperationType,
        ProposedHash = hash,
        PayloadJson = JsonSerializer.Serialize(payload, WebJson),
        State = "queued",
        NextAttemptAt = DateTimeOffset.UtcNow
    };
    confirmation.PayloadJson = execution.PayloadJson;
    db.AddRange(connection, calendar, binding, evt, confirmation, execution);
    db.SaveChanges();
    return new ExecutionSeed(userId, execution.Id, confirmation.Id, binding.GraphCalendarId, evt.OutlookEventId);
}

private static ProgrammableGraphCalendarClient GraphWithRemote(
    ExecutionSeed seed,
    string title,
    string etag)
{
    var graph = new ProgrammableGraphCalendarClient();
    graph.Events[(seed.GraphCalendarId, seed.GraphEventId)] = new GraphEventDto(
        seed.GraphEventId, title, null,
        new GraphDateTimeTimeZoneDto("2026-07-10T01:00:00Z", "UTC"),
        new GraphDateTimeTimeZoneDto("2026-07-10T02:00:00Z", "UTC"),
        false, "ical-event", null, "singleInstance", "change-remote", etag,
        DateTimeOffset.UtcNow, null, null, null);
    return graph;
}

private static PimDbContext CreateDb()
{
    PimDbContext.RegisterModuleAssembly(typeof(OutlookConnectionEntity).Assembly);
    return new PimDbContext(new DbContextOptionsBuilder<PimDbContext>()
        .UseInMemoryDatabase($"outlook-confirmed-handler-{Guid.NewGuid()}")
        .Options);
}
```

Add `using System.Security.Cryptography;`, `using System.Text;`, `using System.Text.Json;`, `using Pim.Infrastructure.Data.Entities;`, and `using Pim.Core.Operations;`.

- [ ] **Step 2: 运行测试并确认 handler 不存在**

Run:

```powershell
dotnet test tests/Pim.UnitTests/Pim.UnitTests.csproj --filter FullyQualifiedName~OutlookConfirmedOperationHandlerTests
```

Expected: FAIL，编译错误指向 `OutlookConfirmedOperationHandler`。

- [ ] **Step 3: 实现 execution 状态领取和 payload 校验**

Create `src/modules/Pim.Module.Calendar/Services/OutlookConfirmedOperationHandler.cs`:

```csharp
using System.Net;
using System.Text.Json;
using Microsoft.EntityFrameworkCore;
using Microsoft.EntityFrameworkCore.Storage;
using Pim.Core.Operations;
using Pim.Infrastructure.Audit;
using Pim.Infrastructure.Data;
using Pim.Infrastructure.Data.Entities;
using Pim.Module.Calendar.DTOs;
using Pim.Module.Calendar.Entities;

namespace Pim.Module.Calendar.Services;

public sealed class OutlookConfirmedOperationHandler
{
    private static readonly JsonSerializerOptions JsonOptions = new(JsonSerializerDefaults.Web);
    private readonly PimDbContext _db;
    private readonly IGraphCalendarClient _graph;
    private readonly OutlookEventMapper _mapper;

    public OutlookConfirmedOperationHandler(PimDbContext db, IGraphCalendarClient graph, OutlookEventMapper mapper)
    {
        _db = db;
        _graph = graph;
        _mapper = mapper;
    }

    public async Task ExecuteAsync(Guid executionId, CancellationToken ct)
    {
        var execution = await _db.Set<OutlookOperationExecutionEntity>()
            .SingleAsync(item => item.Id == executionId, ct);
        if (execution.State == "completed") return;
        if (execution.State == "conflict") return;

        var payload = JsonSerializer.Deserialize<OutlookConfirmedOperationPayload>(execution.PayloadJson, JsonOptions)
            ?? throw new InvalidOperationException("Outlook execution payload cannot be deserialized.");
        if (!string.Equals(payload.ProposedHash, execution.ProposedHash, StringComparison.Ordinal))
            throw new InvalidOperationException("Outlook execution proposed hash does not match its payload.");

        execution.State = "executing";
        execution.AttemptCount++;
        execution.LastErrorCode = null;
        execution.LastErrorMessage = null;
        execution.UpdatedAt = DateTimeOffset.UtcNow;
        await _db.SaveChangesAsync(ct);

        try
        {
            await ExecuteCoreAsync(executionId, payload, ct);
        }
        catch (GraphRequestException exception) when (exception.StatusCode == HttpStatusCode.PreconditionFailed)
        {
            await MarkConflictAsync(executionId, payload, "etag-conflict", ct);
        }
        catch (Exception exception) when (IsRetryable(exception))
        {
            await MarkRetryableAsync(executionId, exception, ct);
        }
        catch (DbUpdateException exception)
        {
            await MarkRetryableAsync(executionId, exception, ct);
        }
    }
}
```

- [ ] **Step 4: 实现 update/delete/pull 的远端状态决策**

Add these methods:

```csharp
private async Task ExecuteCoreAsync(
    Guid executionId,
    OutlookConfirmedOperationPayload payload,
    CancellationToken ct)
{
    var remote = await _graph.GetEventAsync(
        payload.ConnectionId, payload.GraphCalendarId, payload.GraphEventId, ct);

    if (payload.Action is "delete" or "pull-delete")
    {
        if (payload.Action == "pull-delete" && remote is not null)
        {
            await MarkConflictAsync(executionId, payload, "remote-delete-no-longer-current", ct);
            return;
        }
        if (remote is not null)
        {
            if (!string.Equals(remote.ETag, payload.ExpectedEtag, StringComparison.Ordinal))
            {
                await MarkConflictAsync(executionId, payload, "etag-conflict", ct);
                return;
            }
            await _graph.DeleteEventAsync(
                payload.ConnectionId, payload.GraphCalendarId, payload.GraphEventId,
                payload.ExpectedEtag ?? "*", ct);
        }
        await CommitLocalAsync(executionId, payload, null, ct);
        return;
    }

    if (remote is null)
    {
        await MarkConflictAsync(executionId, payload, "remote-event-missing", ct);
        return;
    }
    var proposed = payload.Proposed
        ?? throw new InvalidOperationException("Update execution has no proposed snapshot.");
    if (Equivalent(_mapper.Snapshot(remote), proposed))
    {
        await CommitLocalAsync(executionId, payload, remote, ct);
        return;
    }
    if (payload.Action == "pull-update"
        || !string.Equals(remote.ETag, payload.ExpectedEtag, StringComparison.Ordinal))
    {
        await MarkConflictAsync(executionId, payload, "etag-conflict", ct);
        return;
    }

    var patched = await _graph.PatchEventAsync(
        payload.ConnectionId,
        payload.GraphCalendarId,
        payload.GraphEventId,
        payload.ExpectedEtag ?? "*",
        ToPatch(proposed),
        ct);
    await CommitLocalAsync(executionId, payload, patched, ct);
}

private static bool Equivalent(OutlookEventSnapshot left, OutlookEventSnapshot right)
    => left.Title == right.Title
       && left.Description == right.Description
       && left.Location == right.Location
       && left.DtStart == right.DtStart
       && left.DtEnd == right.DtEnd
       && left.IsAllDay == right.IsAllDay
       && left.AllDayStartDate == right.AllDayStartDate
       && left.AllDayEndDateExclusive == right.AllDayEndDateExclusive;

private static GraphEventPatch ToPatch(OutlookEventSnapshot proposed)
    => new(
        proposed.Title,
        new { contentType = "text", content = proposed.Description ?? string.Empty },
        new GraphLocationDto(proposed.Location),
        GraphTime(proposed.DtStart, proposed.AllDayStartDate, proposed.OriginalStartTimeZone),
        GraphTime(proposed.DtEnd, proposed.AllDayEndDateExclusive, proposed.OriginalEndTimeZone),
        proposed.IsAllDay);

private static GraphDateTimeTimeZoneDto GraphTime(
    DateTimeOffset utc,
    DateOnly? allDayDate,
    string? originalTimeZone)
{
    if (allDayDate is { } date)
        return new GraphDateTimeTimeZoneDto($"{date:yyyy-MM-dd}T00:00:00.0000000", "UTC");
    var zoneId = string.IsNullOrWhiteSpace(originalTimeZone) ? "UTC" : originalTimeZone;
    try
    {
        var zone = TimeZoneInfo.FindSystemTimeZoneById(zoneId);
        var local = TimeZoneInfo.ConvertTime(utc, zone);
        return new GraphDateTimeTimeZoneDto(local.ToString("yyyy-MM-ddTHH:mm:ss.fffffff"), zoneId);
    }
    catch (TimeZoneNotFoundException)
    {
        return new GraphDateTimeTimeZoneDto(utc.UtcDateTime.ToString("yyyy-MM-ddTHH:mm:ss.fffffff"), "UTC");
    }
}
```

- [ ] **Step 5: 实现本地原子收尾**

Add `CommitLocalAsync`:

```csharp
private async Task CommitLocalAsync(
    Guid executionId,
    OutlookConfirmedOperationPayload payload,
    GraphEventDto? remote,
    CancellationToken ct)
{
    _db.ChangeTracker.Clear();
    IDbContextTransaction? transaction = null;
    if (_db.Database.IsRelational()) transaction = await _db.Database.BeginTransactionAsync(ct);
    try
    {
        var execution = await _db.Set<OutlookOperationExecutionEntity>().SingleAsync(item => item.Id == executionId, ct);
        var confirmation = await _db.OperationConfirmations.SingleAsync(item => item.Id == execution.ConfirmationId, ct);
        var evt = await _db.Set<EventEntity>().IgnoreQueryFilters().SingleAsync(item => item.Id == payload.PimEventId, ct);
        var before = OutlookEventMapper.Snapshot(evt);

        if (payload.Action is "delete" or "pull-delete")
        {
            evt.DeletedAt = DateTimeOffset.UtcNow;
            evt.DeletedByOperationId = execution.ConfirmationId;
            evt.DeletedByOperationKind = "outlook-confirmed-delete";
            evt.UpdatedAt = DateTimeOffset.UtcNow;
        }
        else
        {
            _mapper.Apply(evt, payload.Proposed!);
            evt.OutlookChangeKey = remote?.ChangeKey ?? evt.OutlookChangeKey;
            evt.OutlookEtag = remote?.ETag ?? evt.OutlookEtag;
            evt.OutlookSyncState = "active";
        }

        var after = payload.Action is "delete" or "pull-delete" ? null : OutlookEventMapper.Snapshot(evt);
        if (!await _db.AuditVersions.AnyAsync(item => item.ConfirmationId == execution.ConfirmationId, ct))
        {
            _db.AuditVersions.Add(new AuditVersionEntity
            {
                ObjectType = "event",
                ObjectId = evt.Id,
                ConfirmationId = execution.ConfirmationId,
                Source = "outlook",
                Actor = "system",
                BeforeJson = JsonSerializer.Serialize(before, JsonOptions),
                AfterJson = JsonSerializer.Serialize(after, JsonOptions),
                ChangedFieldsJson = JsonSerializer.Serialize(
                    after is null ? new[] { "delete" } : OutlookEventMapper.ChangedFields(before, after), JsonOptions),
                CreatedAt = DateTimeOffset.UtcNow
            });
        }

        execution.State = "completed";
        execution.CompletedAt = DateTimeOffset.UtcNow;
        execution.NextAttemptAt = null;
        execution.UpdatedAt = DateTimeOffset.UtcNow;
        confirmation.Status = OperationConfirmationStatus.Executed.ToString();
        confirmation.ExecutedAt = DateTimeOffset.UtcNow;
        confirmation.ResultJson = JsonSerializer.Serialize(new { executionId, state = "completed" }, JsonOptions);
        await _db.SaveChangesAsync(ct);
        if (transaction is not null) await transaction.CommitAsync(ct);
    }
    finally
    {
        if (transaction is not null) await transaction.DisposeAsync();
    }
}
```

This is the only local commit after a Graph side effect. Event, external version, audit, confirmation, and execution change together.

- [ ] **Step 6: 实现冲突和 retryable-failed 状态**

Add:

```csharp
private async Task MarkConflictAsync(
    Guid executionId,
    OutlookConfirmedOperationPayload payload,
    string code,
    CancellationToken ct)
{
    _db.ChangeTracker.Clear();
    var execution = await _db.Set<OutlookOperationExecutionEntity>().SingleAsync(item => item.Id == executionId, ct);
    execution.State = "conflict";
    execution.LastErrorCode = code;
    execution.LastErrorMessage = "远端事件自预览后发生变化，需要重新生成确认。";
    execution.NextAttemptAt = null;
    execution.UpdatedAt = DateTimeOffset.UtcNow;
    if (!await _db.Set<SyncConflictEntity>().AnyAsync(item => item.SourceConfirmationId == execution.ConfirmationId, ct))
    {
        _db.Add(new SyncConflictEntity
        {
            UserId = payload.UserId,
            Provider = "outlook",
            ObjectType = "event",
            ObjectId = payload.PimEventId,
            GraphEventId = payload.GraphEventId,
            ConflictKind = code,
            Status = "open",
            PimSnapshotJson = JsonSerializer.Serialize(payload.Proposed, JsonOptions),
            ExternalSnapshotJson = "{}",
            SourceConfirmationId = execution.ConfirmationId,
            ResolvedConfirmationId = null,
            CreatedAt = DateTimeOffset.UtcNow
        });
    }
    await _db.SaveChangesAsync(ct);
}

private async Task MarkRetryableAsync(Guid executionId, Exception exception, CancellationToken ct)
{
    _db.ChangeTracker.Clear();
    var execution = await _db.Set<OutlookOperationExecutionEntity>().SingleAsync(item => item.Id == executionId, ct);
    execution.State = "retryable-failed";
    execution.LastErrorCode = exception is GraphRequestException graph
        ? $"graph-{(int)graph.StatusCode}"
        : "unknown-write-result";
    execution.LastErrorMessage = "写入结果不确定；下次执行会先读取远端状态。";
    execution.NextAttemptAt = DateTimeOffset.UtcNow.AddMinutes(Math.Min(30, 1 << Math.Min(execution.AttemptCount, 5)));
    execution.UpdatedAt = DateTimeOffset.UtcNow;
    await _db.SaveChangesAsync(ct);
}

private static bool IsRetryable(Exception exception) => exception switch
{
    HttpRequestException => true,
    TimeoutException => true,
    TaskCanceledException => true,
    GraphRequestException graph when graph.StatusCode is HttpStatusCode.RequestTimeout
        or HttpStatusCode.TooManyRequests
        or HttpStatusCode.InternalServerError
        or HttpStatusCode.BadGateway
        or HttpStatusCode.ServiceUnavailable
        or HttpStatusCode.GatewayTimeout => true,
    _ => false
};
```

For permanent 400/403 failures, call `MarkConflictAsync` with `graph-400` or `graph-403` in a final `catch (GraphRequestException exception)` block so the execution does not spin forever.

- [ ] **Step 7: 增加 delete 和未知结果恢复测试**

Add two tests to `OutlookConfirmedOperationHandlerTests`:

```csharp
[Fact]
public async Task Delete_RemoteGoneIsIdempotentAndSoftDeletesLocal()
{
    await using var db = CreateDb();
    var seed = SeedExecution(db, action: "delete", remoteEtag: "etag-before");
    var graph = new ProgrammableGraphCalendarClient();
    graph.Events[(seed.GraphCalendarId, seed.GraphEventId)] = null;
    var handler = new OutlookConfirmedOperationHandler(db, graph, new OutlookEventMapper());

    await handler.ExecuteAsync(seed.ExecutionId, CancellationToken.None);

    Assert.Empty(graph.Deletes);
    Assert.NotNull((await db.Set<EventEntity>().IgnoreQueryFilters().SingleAsync()).DeletedAt);
    Assert.Equal("completed", (await db.Set<OutlookOperationExecutionEntity>().SingleAsync()).State);
}

[Fact]
public async Task UnknownPatchResult_RetryReadsRemoteAndDoesNotPatchTwice()
{
    await using var db = CreateDb();
    var seed = SeedExecution(db, action: "update", remoteEtag: "etag-before");
    var graph = GraphWithRemote(seed, "旧标题", "etag-before");
    graph.PatchExceptionAfterApplying = new HttpRequestException("connection dropped");
    var handler = new OutlookConfirmedOperationHandler(db, graph, new OutlookEventMapper());

    await handler.ExecuteAsync(seed.ExecutionId, CancellationToken.None);
    graph.PatchExceptionAfterApplying = null;
    await handler.ExecuteAsync(seed.ExecutionId, CancellationToken.None);

    Assert.Single(graph.Patches);
    Assert.Equal("新标题", (await db.Set<EventEntity>().SingleAsync()).Title);
    Assert.Equal("completed", (await db.Set<OutlookOperationExecutionEntity>().SingleAsync()).State);
}
```

`ProgrammableGraphCalendarClient.PatchEventAsync` already records the patch, updates its remote event dictionary, and then throws `PatchExceptionAfterApplying`; this models a network break after Microsoft accepted the PATCH.

- [ ] **Step 8: 运行 handler 测试**

Run:

```powershell
dotnet test tests/Pim.UnitTests/Pim.UnitTests.csproj --filter FullyQualifiedName~OutlookConfirmedOperationHandlerTests
```

Expected: PASS；ETag mismatch 不写两侧，未知结果重试只产生一次 PATCH，远端已删除时本地可幂等收尾。

- [ ] **Step 9: 提交 confirmed operation handler**

```powershell
git add src/modules/Pim.Module.Calendar/Services/OutlookConfirmedOperationHandler.cs tests/Pim.UnitTests/Calendar/OutlookConfirmedOperationHandlerTests.cs tests/Pim.UnitTests/Calendar/OutlookGraphFakes.cs
git commit -m "feat: execute confirmed outlook changes safely"
```

Expected: commit 提供完整生产执行者，但还没有 Hangfire 唤醒器。

## Task 12: 投影远端事件并为核心变更/删除创建 L3 确认

**Files:**
- Create: `src/modules/Pim.Module.Calendar/Services/OutlookEventProjectionService.cs`
- Create: `tests/Pim.UnitTests/Calendar/OutlookEventProjectionTests.cs`

- [ ] **Step 1: 写新建、变更去重和删除核验测试**

Create `tests/Pim.UnitTests/Calendar/OutlookEventProjectionTests.cs`:

```csharp
[Fact]
public async Task NewRemoteEvent_CreatesProjectionAutomatically()
{
    await using var db = CreateDb();
    var seed = SeedBinding(db);
    var service = Service(db, new ProgrammableGraphCalendarClient());

    var result = await service.UpsertAsync(
        seed.UserId, seed.Binding, Remote("event-1", "课程", "etag-1"), Guid.NewGuid(), CancellationToken.None);

    Assert.Equal("created", result.Outcome);
    var stored = await db.Set<EventEntity>().SingleAsync();
    Assert.Equal(seed.Binding.Id, stored.OutlookCalendarBindingId);
    Assert.Equal("event-1", stored.OutlookEventId);
}

[Fact]
public async Task ChangedRemoteCoreFact_CreatesOnePullConfirmationWithoutLocalMutation()
{
    await using var db = CreateDb();
    var seed = SeedBinding(db);
    SeedEvent(db, seed.Binding, "旧标题", "etag-old");
    var service = Service(db, new ProgrammableGraphCalendarClient());
    var remote = Remote("event-1", "新标题", "etag-new");

    await service.UpsertAsync(seed.UserId, seed.Binding, remote, Guid.NewGuid(), CancellationToken.None);
    await service.UpsertAsync(seed.UserId, seed.Binding, remote, Guid.NewGuid(), CancellationToken.None);

    Assert.Equal("旧标题", (await db.Set<EventEntity>().SingleAsync()).Title);
    var confirmation = Assert.Single(await db.OperationConfirmations.ToListAsync());
    Assert.Equal("outlook.event.pull-update", confirmation.OperationType);
    Assert.Equal(OperationRiskLevel.L3ExternalSourceOrWriteback.ToString(), confirmation.RiskLevel);
}

[Fact]
public async Task MissingRemoteEvent_RequiresSingleGet404BeforeDeleteConfirmation()
{
    await using var db = CreateDb();
    var seed = SeedBinding(db);
    var evt = SeedEvent(db, seed.Binding, "旧标题", "etag-old");
    var graph = new ProgrammableGraphCalendarClient();
    graph.Events[(seed.Binding.GraphCalendarId, evt.OutlookEventId!)] = null;
    var service = Service(db, graph);

    await service.VerifyMissingAsync(seed.UserId, seed.Binding, evt.Id, CancellationToken.None);

    Assert.Null((await db.Set<EventEntity>().SingleAsync()).DeletedAt);
    Assert.Equal("outlook.event.pull-delete", (await db.OperationConfirmations.SingleAsync()).OperationType);
}
```

Insert these helpers in `OutlookEventProjectionTests`:

```csharp
private sealed record ProjectionSeed(Guid UserId, OutlookCalendarBindingEntity Binding);

private static ProjectionSeed SeedBinding(PimDbContext db)
{
    var userId = Guid.NewGuid();
    var connection = new OutlookConnectionEntity
    {
        UserId = userId,
        ClientId = Guid.NewGuid().ToString(),
        Status = "connected",
        TokenHealth = "healthy"
    };
    var calendar = new CalendarEntity { UserId = userId, Name = "课程表", Source = "outlook" };
    var binding = new OutlookCalendarBindingEntity
    {
        ConnectionId = connection.Id,
        PimCalendarId = calendar.Id,
        GraphCalendarId = "course-calendar",
        Name = "课程表",
        CanEdit = true,
        IsSelected = true,
        RemoteState = "active"
    };
    db.AddRange(connection, calendar, binding);
    db.SaveChanges();
    return new ProjectionSeed(userId, binding);
}

private static EventEntity SeedEvent(
    PimDbContext db,
    OutlookCalendarBindingEntity binding,
    string title,
    string etag)
{
    var evt = new EventEntity
    {
        CalendarId = binding.PimCalendarId,
        Uid = "ical-event-1",
        SourceUid = "ical-event-1",
        Title = title,
        DtStart = new DateTimeOffset(2026, 7, 10, 9, 0, 0, TimeSpan.Zero),
        DtEnd = new DateTimeOffset(2026, 7, 10, 10, 0, 0, TimeSpan.Zero),
        Source = "outlook",
        OutlookConnectionId = binding.ConnectionId,
        OutlookCalendarBindingId = binding.Id,
        OutlookEventId = "event-1",
        OutlookChangeKey = "change-old",
        OutlookEtag = etag,
        GraphRecurrenceJson = "{}"
    };
    db.Add(evt);
    db.SaveChanges();
    return evt;
}

private static GraphEventDto Remote(string id, string title, string etag) => new(
    id, title, null,
    new GraphDateTimeTimeZoneDto("2026-07-10T09:00:00Z", "UTC"),
    new GraphDateTimeTimeZoneDto("2026-07-10T10:00:00Z", "UTC"),
    false, $"ical-{id}", null, "singleInstance", $"change-{id}", etag,
    DateTimeOffset.UtcNow, null, null, null);

private static OutlookEventProjectionService Service(
    PimDbContext db,
    ProgrammableGraphCalendarClient graph)
    => new(db, graph, new OutlookEventMapper(), new OperationConfirmationService(db));

private static PimDbContext CreateDb()
{
    PimDbContext.RegisterModuleAssembly(typeof(OutlookConnectionEntity).Assembly);
    return new PimDbContext(new DbContextOptionsBuilder<PimDbContext>()
        .UseInMemoryDatabase($"outlook-projection-{Guid.NewGuid()}")
        .Options);
}
```

Add `using Pim.Infrastructure.Operations;`.

- [ ] **Step 2: 运行测试并确认 projection service 不存在**

Run:

```powershell
dotnet test tests/Pim.UnitTests/Pim.UnitTests.csproj --filter FullyQualifiedName~OutlookEventProjectionTests
```

Expected: FAIL，编译错误指向 `OutlookEventProjectionService`。

- [ ] **Step 3: 实现新事件 upsert 和非核心版本刷新**

Create `src/modules/Pim.Module.Calendar/Services/OutlookEventProjectionService.cs`:

```csharp
using System.Security.Cryptography;
using System.Text;
using System.Text.Json;
using Microsoft.EntityFrameworkCore;
using Pim.Core.Operations;
using Pim.Infrastructure.Data;
using Pim.Module.Calendar.DTOs;
using Pim.Module.Calendar.Entities;

namespace Pim.Module.Calendar.Services;

public sealed record OutlookProjectionResult(string Outcome, Guid EventId, Guid? ConfirmationId = null);

public sealed class OutlookEventProjectionService
{
    private static readonly JsonSerializerOptions JsonOptions = new(JsonSerializerDefaults.Web);
    private readonly PimDbContext _db;
    private readonly IGraphCalendarClient _graph;
    private readonly OutlookEventMapper _mapper;
    private readonly IOperationConfirmationService _confirmations;

    public OutlookEventProjectionService(
        PimDbContext db,
        IGraphCalendarClient graph,
        OutlookEventMapper mapper,
        IOperationConfirmationService confirmations)
    {
        _db = db;
        _graph = graph;
        _mapper = mapper;
        _confirmations = confirmations;
    }

    public async Task<OutlookProjectionResult> UpsertAsync(
        Guid userId,
        OutlookCalendarBindingEntity binding,
        GraphEventDto remote,
        Guid generation,
        CancellationToken ct)
    {
        if (remote.IsRemoved) return await VerifyRemovedAsync(userId, binding, remote.Id, ct);
        var existing = await _db.Set<EventEntity>()
            .SingleOrDefaultAsync(item => item.OutlookCalendarBindingId == binding.Id && item.OutlookEventId == remote.Id, ct);
        if (existing is null)
        {
            var created = _mapper.MapNew(remote, binding, generation);
            _db.Add(created);
            await _db.SaveChangesAsync(ct);
            return new OutlookProjectionResult("created", created.Id);
        }

        var before = OutlookEventMapper.Snapshot(existing);
        var after = _mapper.Snapshot(remote);
        var changed = OutlookEventMapper.ChangedFields(before, after);
        if (changed.Count > 0)
            return await CreatePullConfirmationAsync(userId, binding, existing, remote, before, after, changed, ct);

        existing.OutlookChangeKey = remote.ChangeKey;
        existing.OutlookEtag = remote.ETag;
        existing.OutlookSeriesMasterId = remote.SeriesMasterId;
        existing.OutlookEventType = remote.Type;
        existing.LastSeenSyncGeneration = generation;
        existing.OutlookSyncState = "active";
        existing.UpdatedAt = DateTimeOffset.UtcNow;
        await _db.SaveChangesAsync(ct);
        return new OutlookProjectionResult("unchanged", existing.Id);
    }
}
```

- [ ] **Step 4: 实现 pull-update confirmation 和去重**

Add:

```csharp
private async Task<OutlookProjectionResult> CreatePullConfirmationAsync(
    Guid userId,
    OutlookCalendarBindingEntity binding,
    EventEntity existing,
    GraphEventDto remote,
    OutlookEventSnapshot before,
    OutlookEventSnapshot after,
    IReadOnlyList<string> changed,
    CancellationToken ct)
{
    var hash = Hash(after);
    var correlation = $"outlook-pull-update-{binding.Id}-{remote.Id}-{remote.ETag}";
    var pending = await _db.OperationConfirmations.AsNoTracking().SingleOrDefaultAsync(item =>
        item.CorrelationId == correlation && item.Status == OperationConfirmationStatus.Pending.ToString(), ct);
    if (pending is not null) return new OutlookProjectionResult("confirmation-existing", existing.Id, pending.Id);

    var payload = new OutlookConfirmedOperationPayload(
        userId, binding.ConnectionId, binding.Id, existing.Id,
        binding.GraphCalendarId, remote.Id, remote.ETag, remote.ChangeKey,
        hash, "pull-update", before, after);
    var decision = ScheduleFactConfirmationPolicy.Classify("outlook", changed, externalWriteback: false);
    var confirmation = await _confirmations.CreateAsync(new CreateOperationConfirmationRequest(
        userId,
        "outlook.event.pull-update",
        $"复核 Outlook 对“{existing.Title}”的远端变更",
        decision.RiskLevel,
        "outlook",
        JsonSerializer.Serialize(payload, JsonOptions),
        "{}",
        DateTimeOffset.UtcNow.AddDays(7),
        correlation,
        changed,
        ["confirm", "reject"],
        "event",
        existing.Id,
        decision.RequiresSecondLevelConfirmation,
        JsonSerializer.Serialize(before, JsonOptions),
        JsonSerializer.Serialize(after, JsonOptions),
        decision.RequiresStrictConfirmation,
        ExternalEffect: "接受 Microsoft Graph 中已经发生的事实变更",
        RecoveryPath: "执行前重新核对远端 ETag。"), ct);
    return new OutlookProjectionResult("confirmation-created", existing.Id, confirmation.Id);
}

private static string Hash(OutlookEventSnapshot? snapshot)
    => Convert.ToHexString(SHA256.HashData(Encoding.UTF8.GetBytes(
        JsonSerializer.Serialize(snapshot, JsonOptions)))).ToLowerInvariant();
```

- [ ] **Step 5: 实现 tombstone/missing 单事件核验**

Add:

```csharp
public async Task<OutlookProjectionResult> VerifyRemovedAsync(
    Guid userId,
    OutlookCalendarBindingEntity binding,
    string graphEventId,
    CancellationToken ct)
{
    var existing = await _db.Set<EventEntity>()
        .SingleOrDefaultAsync(item => item.OutlookCalendarBindingId == binding.Id && item.OutlookEventId == graphEventId, ct);
    if (existing is null) return new OutlookProjectionResult("already-absent", Guid.Empty);
    var remote = await _graph.GetEventAsync(binding.ConnectionId, binding.GraphCalendarId, graphEventId, ct);
    if (remote is not null)
        return await UpsertAsync(userId, binding, remote, Guid.NewGuid(), ct);
    return await CreatePullDeleteConfirmationAsync(userId, binding, existing, ct);
}

public async Task<OutlookProjectionResult> VerifyMissingAsync(
    Guid userId,
    OutlookCalendarBindingEntity binding,
    Guid eventId,
    CancellationToken ct)
{
    var existing = await _db.Set<EventEntity>()
        .SingleAsync(item => item.Id == eventId && item.OutlookCalendarBindingId == binding.Id, ct);
    var remote = await _graph.GetEventAsync(
        binding.ConnectionId, binding.GraphCalendarId, existing.OutlookEventId!, ct);
    if (remote is null) return await CreatePullDeleteConfirmationAsync(userId, binding, existing, ct);
    var result = await UpsertAsync(userId, binding, remote, Guid.NewGuid(), ct);
    if (remote.Start is not null && remote.End is not null)
    {
        existing.OutlookSyncState = "out-of-window";
        await _db.SaveChangesAsync(ct);
    }
    return result;
}

private async Task<OutlookProjectionResult> CreatePullDeleteConfirmationAsync(
    Guid userId,
    OutlookCalendarBindingEntity binding,
    EventEntity existing,
    CancellationToken ct)
{
    var before = OutlookEventMapper.Snapshot(existing);
    var hash = Hash(null);
    var correlation = $"outlook-pull-delete-{binding.Id}-{existing.OutlookEventId}-{existing.OutlookEtag}";
    var pending = await _db.OperationConfirmations.AsNoTracking().SingleOrDefaultAsync(item =>
        item.CorrelationId == correlation && item.Status == OperationConfirmationStatus.Pending.ToString(), ct);
    if (pending is not null) return new OutlookProjectionResult("confirmation-existing", existing.Id, pending.Id);

    var payload = new OutlookConfirmedOperationPayload(
        userId, binding.ConnectionId, binding.Id, existing.Id,
        binding.GraphCalendarId, existing.OutlookEventId!, existing.OutlookEtag, existing.OutlookChangeKey,
        hash, "pull-delete", before, null);
    var confirmation = await _confirmations.CreateAsync(new CreateOperationConfirmationRequest(
        userId,
        "outlook.event.pull-delete",
        $"复核 Outlook 中已删除的日程“{existing.Title}”",
        OperationRiskLevel.L3ExternalSourceOrWriteback,
        "outlook",
        JsonSerializer.Serialize(payload, JsonOptions),
        "{}",
        DateTimeOffset.UtcNow.AddDays(7),
        correlation,
        ["delete"], ["confirm", "reject"], "event", existing.Id,
        RequiresSecondLevelConfirmation: true,
        BeforeJson: JsonSerializer.Serialize(before, JsonOptions),
        AfterJson: "null",
        ExternalEffect: "接受 Microsoft Graph 中已经发生的删除",
        RecoveryPath: "执行前再次 GET，只有 404 才软删除 PIM 投影。"), ct);
    return new OutlookProjectionResult("confirmation-created", existing.Id, confirmation.Id);
}
```

- [ ] **Step 6: 运行 projection 测试**

Run:

```powershell
dotnet test tests/Pim.UnitTests/Pim.UnitTests.csproj --filter FullyQualifiedName~OutlookEventProjectionTests
```

Expected: PASS；远端新事件自动建投影，核心事实不先落本地，同一 ETag 只产生一个 pending confirmation，404 才产生删除确认。

- [ ] **Step 7: 提交 projection service**

```powershell
git add src/modules/Pim.Module.Calendar/Services/OutlookEventProjectionService.cs tests/Pim.UnitTests/Calendar/OutlookEventProjectionTests.cs
git commit -m "feat: project and confirm outlook remote changes"
```

Expected: 事件级规则完整，下一任务只负责选择读取策略和窗口。

## Task 13: 实现默认 delta 与非默认 calendarView 对账

**Files:**
- Create: `src/modules/Pim.Module.Calendar/Services/OutlookCalendarSyncService.cs`
- Create: `tests/Pim.UnitTests/Calendar/OutlookCalendarSyncStrategyTests.cs`
- Modify: `tests/Pim.UnitTests/Calendar/OutlookGraphFakes.cs`
- Modify: `src/modules/Pim.Module.Calendar/Services/OutlookEventProjectionService.cs`

- [ ] **Step 1: 写默认 delta 固定窗口测试**

Create `tests/Pim.UnitTests/Calendar/OutlookCalendarSyncStrategyTests.cs` with a fixed `TimeProvider` at `2026-07-10T00:00:00Z`:

```csharp
[Fact]
public async Task DefaultCalendar_BuildsMinus90Plus365BaselineAndStoresOnlyTerminalDeltaLink()
{
    await using var db = CreateDb();
    var seed = SeedBinding(db, isDefault: true);
    var graph = new ProgrammableGraphCalendarClient();
    graph.DefaultDeltaPages.Enqueue(new GraphPage<GraphEventDto>(
        [Remote("event-1")], "https://graph.microsoft.com/v1.0/me/calendarView/delta?$skiptoken=next", null));
    graph.DefaultDeltaPages.Enqueue(new GraphPage<GraphEventDto>(
        [Remote("event-2")], null, "https://graph.microsoft.com/v1.0/me/calendarView/delta?$deltatoken=done"));
    var service = Service(db, graph, FixedTime());

    var result = await service.SyncIncrementalAsync(seed.UserId, seed.Binding.Id, forceBaseline: false, CancellationToken.None);

    Assert.Equal(2, result.ReadCount);
    Assert.Contains("2026-04-11", graph.DefaultDeltaUrls[0]);
    Assert.Contains("2027-07-10", graph.DefaultDeltaUrls[0]);
    var binding = await db.Set<OutlookCalendarBindingEntity>().SingleAsync();
    Assert.Contains("$deltatoken=done", binding.DeltaLink);
    Assert.Equal(new DateTimeOffset(2026, 4, 11, 0, 0, 0, TimeSpan.Zero), binding.BaselineWindowStart);
}
```

- [ ] **Step 2: 写非默认完整分页后 missing verification 测试**

Add:

```csharp
[Fact]
public async Task NonDefaultCalendar_VerifiesMissingOnlyAfterCompletePagination()
{
    await using var db = CreateDb();
    var seed = SeedBinding(db, isDefault: false);
    var missing = SeedEvent(db, seed.Binding, "missing", new DateTimeOffset(2026, 7, 9, 9, 0, 0, TimeSpan.Zero));
    var graph = new ProgrammableGraphCalendarClient();
    graph.CalendarViewPages[seed.Binding.GraphCalendarId] = new Queue<GraphPage<GraphEventDto>>([
        new([Remote("seen")], null, null)
    ]);
    graph.Events[(seed.Binding.GraphCalendarId, missing.OutlookEventId!)] = null;
    var service = Service(db, graph, FixedTime());

    await service.SyncIncrementalAsync(seed.UserId, seed.Binding.Id, false, CancellationToken.None);

    Assert.Equal("outlook.event.pull-delete", (await db.OperationConfirmations.SingleAsync()).OperationType);
}

[Fact]
public async Task NonDefaultReadFailure_DoesNotInferDeletion()
{
    await using var db = CreateDb();
    var seed = SeedBinding(db, isDefault: false);
    SeedEvent(db, seed.Binding, "missing", new DateTimeOffset(2026, 7, 9, 9, 0, 0, TimeSpan.Zero));
    var graph = new ProgrammableGraphCalendarClient { CalendarViewException = new HttpRequestException("offline") };
    var service = Service(db, graph, FixedTime());

    await Assert.ThrowsAsync<HttpRequestException>(() =>
        service.SyncIncrementalAsync(seed.UserId, seed.Binding.Id, false, CancellationToken.None));

    Assert.Empty(await db.OperationConfirmations.ToListAsync());
}
```

Insert these complete helpers in `OutlookCalendarSyncStrategyTests`:

```csharp
private sealed record SeededBinding(Guid UserId, OutlookCalendarBindingEntity Binding);

private static PimDbContext CreateDb()
{
    PimDbContext.RegisterModuleAssembly(typeof(OutlookConnectionEntity).Assembly);
    return new PimDbContext(new DbContextOptionsBuilder<PimDbContext>()
        .UseInMemoryDatabase($"outlook-sync-strategy-{Guid.NewGuid()}")
        .Options);
}

private static SeededBinding SeedBinding(PimDbContext db, bool isDefault)
{
    var userId = Guid.NewGuid();
    var connection = new OutlookConnectionEntity
    {
        UserId = userId,
        ClientId = Guid.NewGuid().ToString(),
        Status = "connected",
        TokenHealth = "healthy"
    };
    var calendar = new CalendarEntity { UserId = userId, Name = "Outlook", Source = "outlook" };
    var binding = new OutlookCalendarBindingEntity
    {
        ConnectionId = connection.Id,
        PimCalendarId = calendar.Id,
        GraphCalendarId = isDefault ? "default" : "course",
        Name = isDefault ? "默认日历" : "课程表",
        IsDefaultCalendar = isDefault,
        SyncStrategy = isDefault ? "default-delta" : "window-reconcile",
        IsSelected = true,
        RemoteState = "active",
        CanEdit = true
    };
    db.AddRange(connection, calendar, binding);
    db.SaveChanges();
    return new SeededBinding(userId, binding);
}

private static EventEntity SeedEvent(
    PimDbContext db,
    OutlookCalendarBindingEntity binding,
    string graphId,
    DateTimeOffset start)
{
    var evt = new EventEntity
    {
        CalendarId = binding.PimCalendarId,
        Uid = $"ical-{graphId}",
        SourceUid = $"ical-{graphId}",
        Title = graphId,
        DtStart = start,
        DtEnd = start.AddHours(1),
        Source = "outlook",
        OutlookConnectionId = binding.ConnectionId,
        OutlookCalendarBindingId = binding.Id,
        OutlookEventId = graphId,
        OutlookChangeKey = $"change-{graphId}",
        OutlookEtag = $"etag-{graphId}",
        OutlookSyncState = "active"
    };
    db.Add(evt);
    db.SaveChanges();
    return evt;
}

private static GraphEventDto Remote(string id) => new(
    id, id, null,
    new GraphDateTimeTimeZoneDto("2026-07-10T09:00:00Z", "UTC"),
    new GraphDateTimeTimeZoneDto("2026-07-10T10:00:00Z", "UTC"),
    false, $"ical-{id}", null, "singleInstance", $"change-{id}", $"etag-{id}",
    DateTimeOffset.UtcNow, null, null, null);

private static OutlookCalendarSyncService Service(
    PimDbContext db,
    ProgrammableGraphCalendarClient graph,
    TimeProvider time)
{
    var projection = new OutlookEventProjectionService(
        db, graph, new OutlookEventMapper(), new OperationConfirmationService(db));
    return new OutlookCalendarSyncService(db, graph, projection, time);
}

private static TimeProvider FixedTime()
    => new FixedTimeProvider(new DateTimeOffset(2026, 7, 10, 0, 0, 0, TimeSpan.Zero));

private sealed class FixedTimeProvider(DateTimeOffset value) : TimeProvider
{
    public override DateTimeOffset GetUtcNow() => value;
}
```

Add `using Pim.Infrastructure.Operations;`.

- [ ] **Step 3: 运行策略测试并确认 sync service 不存在**

Run:

```powershell
dotnet test tests/Pim.UnitTests/Pim.UnitTests.csproj --filter FullyQualifiedName~OutlookCalendarSyncStrategyTests
```

Expected: FAIL，编译错误指向 `OutlookCalendarSyncService` 或 fake 记录字段。

- [ ] **Step 4: 让 projection 对 pending 事件记录本次 seen generation**

Before calling `CreatePullConfirmationAsync` in `OutlookEventProjectionService.UpsertAsync`, set only reconciliation metadata:

```csharp
if (changed.Count > 0)
{
    existing.LastSeenSyncGeneration = generation;
    existing.OutlookSyncState = "active";
    return await CreatePullConfirmationAsync(userId, binding, existing, remote, before, after, changed, ct);
}
```

Do not overwrite local core fields, ETag, or changeKey until the pull confirmation executes.

- [ ] **Step 5: 定义同步结果和窗口**

Create `src/modules/Pim.Module.Calendar/Services/OutlookCalendarSyncService.cs`:

```csharp
using Microsoft.EntityFrameworkCore;
using Pim.Infrastructure.Data;
using Pim.Module.Calendar.Entities;

namespace Pim.Module.Calendar.Services;

public sealed record OutlookSyncWindow(DateTimeOffset Start, DateTimeOffset End);
public sealed record OutlookCalendarSyncResult(
    Guid BindingId,
    string Status,
    int ReadCount,
    int CreatedCount,
    int ConfirmationCount,
    int FailureCount,
    string? ErrorCode = null,
    string? ErrorMessage = null);

public sealed class OutlookCalendarSyncService
{
    private readonly PimDbContext _db;
    private readonly IGraphCalendarClient _graph;
    private readonly OutlookEventProjectionService _projection;
    private readonly TimeProvider _time;

    public OutlookCalendarSyncService(
        PimDbContext db,
        IGraphCalendarClient graph,
        OutlookEventProjectionService projection,
        TimeProvider time)
    {
        _db = db;
        _graph = graph;
        _projection = projection;
        _time = time;
    }

    public OutlookSyncWindow CurrentWindow()
    {
        var now = _time.GetUtcNow();
        return new OutlookSyncWindow(now.AddDays(-90), now.AddDays(365));
    }

    public async Task<OutlookCalendarSyncResult> SyncIncrementalAsync(
        Guid userId,
        Guid bindingId,
        bool forceBaseline,
        CancellationToken ct)
    {
        var binding = await _db.Set<OutlookCalendarBindingEntity>().SingleAsync(item => item.Id == bindingId, ct);
        if (!binding.IsSelected || binding.RemoteState != "active")
            return new OutlookCalendarSyncResult(binding.Id, "skipped", 0, 0, 0, 0);
        return binding.IsDefaultCalendar
            ? await SyncDefaultAsync(userId, binding, forceBaseline, ct)
            : await ReconcileWindowAsync(userId, binding, CurrentWindow(), ct);
    }
}
```

- [ ] **Step 6: 实现默认日历 delta**

Add:

```csharp
private async Task<OutlookCalendarSyncResult> SyncDefaultAsync(
    Guid userId,
    OutlookCalendarBindingEntity binding,
    bool forceBaseline,
    CancellationToken ct)
{
    var window = CurrentWindow();
    var buildingBaseline = forceBaseline || string.IsNullOrWhiteSpace(binding.DeltaLink);
    var url = buildingBaseline ? DefaultDeltaUrl(window) : binding.DeltaLink!;
    var read = 0;
    var created = 0;
    var confirmations = 0;
    string? terminalDelta = null;

    try
    {
        while (url is not null)
        {
            var page = await _graph.GetDefaultDeltaPageAsync(binding.ConnectionId, url, ct);
            foreach (var remote in page.Value)
            {
                var projected = await _projection.UpsertAsync(userId, binding, remote, Guid.NewGuid(), ct);
                read++;
                if (projected.Outcome == "created") created++;
                if (projected.ConfirmationId is not null) confirmations++;
            }
            terminalDelta = page.DeltaLink ?? terminalDelta;
            url = page.NextLink;
        }
    }
    catch (GraphRequestException exception) when (!buildingBaseline && exception.StatusCode == HttpStatusCode.Gone)
    {
        binding.DeltaLink = null;
        await _db.SaveChangesAsync(ct);
        return await SyncDefaultAsync(userId, binding, forceBaseline: true, ct);
    }

    if (string.IsNullOrWhiteSpace(terminalDelta))
        throw new InvalidOperationException("Default calendar delta ended without a deltaLink.");
    binding.DeltaLink = terminalDelta;
    if (buildingBaseline)
    {
        binding.BaselineWindowStart = window.Start;
        binding.BaselineWindowEnd = window.End;
        binding.LastFullBaselineAt = _time.GetUtcNow();
    }
    binding.LastSyncedAt = _time.GetUtcNow();
    binding.LastErrorCode = null;
    binding.LastErrorMessage = null;
    binding.UpdatedAt = _time.GetUtcNow();
    await _db.SaveChangesAsync(ct);
    return new OutlookCalendarSyncResult(binding.Id, "completed", read, created, confirmations, 0);
}

private static string DefaultDeltaUrl(OutlookSyncWindow window)
    => $"/me/calendarView/delta?startDateTime={Uri.EscapeDataString(window.Start.ToString("O"))}&endDateTime={Uri.EscapeDataString(window.End.ToString("O"))}&$select=id,subject,bodyPreview,start,end,isAllDay,iCalUId,seriesMasterId,type,changeKey,lastModifiedDateTime,location,recurrence,originalStartTimeZone,originalEndTimeZone";
```

Add `using System.Net;`. A forced daily baseline keeps the old `DeltaLink` in the entity until all new pages return a terminal deltaLink; therefore a failed rebuild leaves the prior cursor usable.

- [ ] **Step 7: 实现非默认窗口对账**

Add:

```csharp
private async Task<OutlookCalendarSyncResult> ReconcileWindowAsync(
    Guid userId,
    OutlookCalendarBindingEntity binding,
    OutlookSyncWindow window,
    CancellationToken ct)
{
    var generation = Guid.NewGuid();
    var url = CalendarViewUrl(binding.GraphCalendarId, window);
    var read = 0;
    var created = 0;
    var confirmations = 0;

    while (url is not null)
    {
        var page = await _graph.GetCalendarViewPageAsync(binding.ConnectionId, binding.GraphCalendarId, url, ct);
        foreach (var remote in page.Value)
        {
            var projected = await _projection.UpsertAsync(userId, binding, remote, generation, ct);
            read++;
            if (projected.Outcome == "created") created++;
            if (projected.ConfirmationId is not null) confirmations++;
        }
        url = page.NextLink;
    }

    var missingIds = await _db.Set<EventEntity>()
        .Where(item => item.OutlookCalendarBindingId == binding.Id
            && item.DtStart < window.End
            && item.DtEnd > window.Start
            && item.LastSeenSyncGeneration != generation)
        .Select(item => item.Id)
        .ToListAsync(ct);
    foreach (var eventId in missingIds)
    {
        var verified = await _projection.VerifyMissingAsync(userId, binding, eventId, ct);
        if (verified.ConfirmationId is not null) confirmations++;
    }

    var naturallyExpired = await _db.Set<EventEntity>()
        .Where(item => item.OutlookCalendarBindingId == binding.Id && item.DtEnd <= window.Start)
        .ToListAsync(ct);
    foreach (var item in naturallyExpired) item.OutlookSyncState = "out-of-window";

    binding.BaselineWindowStart = window.Start;
    binding.BaselineWindowEnd = window.End;
    binding.LastSuccessfulGeneration = generation;
    binding.LastFullBaselineAt = _time.GetUtcNow();
    binding.LastSyncedAt = _time.GetUtcNow();
    binding.LastErrorCode = null;
    binding.LastErrorMessage = null;
    binding.UpdatedAt = _time.GetUtcNow();
    await _db.SaveChangesAsync(ct);
    return new OutlookCalendarSyncResult(binding.Id, "completed", read, created, confirmations, 0);
}

private static string CalendarViewUrl(string graphCalendarId, OutlookSyncWindow window)
    => $"/me/calendars/{Uri.EscapeDataString(graphCalendarId)}/calendarView?startDateTime={Uri.EscapeDataString(window.Start.ToString("O"))}&endDateTime={Uri.EscapeDataString(window.End.ToString("O"))}&$select=id,subject,bodyPreview,start,end,isAllDay,iCalUId,seriesMasterId,type,changeKey,lastModifiedDateTime,location,recurrence,originalStartTimeZone,originalEndTimeZone";
```

The missing query runs only after the paging loop exits successfully. Timeout, cancellation, 403, or any page failure exits before missing verification.

- [ ] **Step 8: 运行策略和 projection 测试**

Run:

```powershell
dotnet test tests/Pim.UnitTests/Pim.UnitTests.csproj --filter "FullyQualifiedName~OutlookCalendarSyncStrategyTests|FullyQualifiedName~OutlookEventProjectionTests"
```

Expected: PASS；默认日历只使用 `/me/calendarView/delta`，非默认日历没有任何 `/calendarView/delta` 路径。

- [ ] **Step 9: 提交同步策略**

```powershell
git add src/modules/Pim.Module.Calendar/Services/OutlookCalendarSyncService.cs src/modules/Pim.Module.Calendar/Services/OutlookEventProjectionService.cs tests/Pim.UnitTests/Calendar/OutlookCalendarSyncStrategyTests.cs tests/Pim.UnitTests/Calendar/OutlookGraphFakes.cs
git commit -m "feat: sync default and nondefault outlook calendars"
```

Expected: 单日历同步已经可靠；批次、调度和深度模式留给下一任务。

## Task 14: 实现两种手动深度同步

**Files:**
- Create: `tests/Pim.UnitTests/Calendar/OutlookDeepSyncTests.cs`
- Modify: `src/modules/Pim.Module.Calendar/Services/OutlookCalendarSyncService.cs`
- Modify: `tests/Pim.UnitTests/Calendar/OutlookGraphFakes.cs`

- [ ] **Step 1: 写 full-resources recurrence 测试**

Create `tests/Pim.UnitTests/Calendar/OutlookDeepSyncTests.cs`:

```csharp
[Fact]
public async Task FullResources_UpsertsSeriesMasterWithoutInferringDeletion()
{
    await using var db = CreateDb();
    var seed = SeedBinding(db);
    using var recurrence = JsonDocument.Parse("""{"pattern":{"type":"weekly","interval":1},"range":{"type":"noEnd","startDate":"2026-07-10"}}""");
    var graph = new ProgrammableGraphCalendarClient();
    graph.EventPages[seed.Binding.GraphCalendarId] = new Queue<GraphPage<GraphEventDto>>([
        new([Remote("master", type: "seriesMaster", recurrence: recurrence.RootElement.Clone())], null, null)
    ]);
    var service = Service(db, graph);

    await service.SyncFullResourcesAsync(seed.UserId, seed.Binding.Id, CancellationToken.None);

    var stored = await db.Set<EventEntity>().SingleAsync(item => item.OutlookEventId == "master");
    Assert.Equal("seriesMaster", stored.OutlookEventType);
    Assert.Contains("weekly", stored.GraphRecurrenceJson);
    Assert.Empty(await db.OperationConfirmations.Where(item => item.OperationType == "outlook.event.pull-delete").ToListAsync());
}
```

- [ ] **Step 2: 写 180 天半开分片和去重测试**

Add:

```csharp
[Fact]
public async Task RangeInstances_SplitsAt180DaysAndDeduplicatesImmutableIds()
{
    await using var db = CreateDb();
    var seed = SeedBinding(db);
    var graph = new ProgrammableGraphCalendarClient();
    graph.CalendarViewPages[seed.Binding.GraphCalendarId] = new Queue<GraphPage<GraphEventDto>>([
        new([Remote("shared"), Remote("first")], null, null),
        new([Remote("shared"), Remote("second")], null, null),
        new([Remote("third")], null, null)
    ]);
    var service = Service(db, graph);
    var start = new DateTimeOffset(2026, 1, 1, 0, 0, 0, TimeSpan.Zero);
    var end = start.AddDays(400);

    var result = await service.SyncRangeInstancesAsync(
        seed.UserId, seed.Binding.Id, new OutlookSyncWindow(start, end), CancellationToken.None);

    Assert.Equal(3, graph.CalendarViewUrls.Count);
    Assert.Equal(5, result.ReadCount);
    Assert.Equal(4, await db.Set<EventEntity>().CountAsync());
    Assert.Contains(Uri.EscapeDataString(start.AddDays(180).ToString("O")), graph.CalendarViewUrls[1]);
    Assert.Contains(Uri.EscapeDataString(start.AddDays(360).ToString("O")), graph.CalendarViewUrls[2]);
}
```

Insert these helpers in `OutlookDeepSyncTests`:

```csharp
private sealed record Seeded(Guid UserId, OutlookCalendarBindingEntity Binding);

private static PimDbContext CreateDb()
{
    PimDbContext.RegisterModuleAssembly(typeof(OutlookConnectionEntity).Assembly);
    return new PimDbContext(new DbContextOptionsBuilder<PimDbContext>()
        .UseInMemoryDatabase($"outlook-deep-sync-{Guid.NewGuid()}")
        .Options);
}

private static Seeded SeedBinding(PimDbContext db)
{
    var userId = Guid.NewGuid();
    var connection = new OutlookConnectionEntity
    {
        UserId = userId,
        ClientId = Guid.NewGuid().ToString(),
        Status = "connected",
        TokenHealth = "healthy"
    };
    var calendar = new CalendarEntity { UserId = userId, Name = "课程表", Source = "outlook" };
    var binding = new OutlookCalendarBindingEntity
    {
        ConnectionId = connection.Id,
        PimCalendarId = calendar.Id,
        GraphCalendarId = "course",
        Name = "课程表",
        IsSelected = true,
        RemoteState = "active",
        CanEdit = true
    };
    db.AddRange(connection, calendar, binding);
    db.SaveChanges();
    return new Seeded(userId, binding);
}

private static GraphEventDto Remote(
    string id,
    string type = "singleInstance",
    JsonElement? recurrence = null) => new(
        id, id, null,
        new GraphDateTimeTimeZoneDto("2026-07-10T09:00:00Z", "UTC"),
        new GraphDateTimeTimeZoneDto("2026-07-10T10:00:00Z", "UTC"),
        false, $"ical-{id}", null, type, $"change-{id}", $"etag-{id}",
        DateTimeOffset.UtcNow, null, recurrence, null);

private static OutlookCalendarSyncService Service(PimDbContext db, ProgrammableGraphCalendarClient graph)
{
    var projection = new OutlookEventProjectionService(
        db, graph, new OutlookEventMapper(), new OperationConfirmationService(db));
    return new OutlookCalendarSyncService(db, graph, projection, TimeProvider.System);
}
```

Add `using Microsoft.EntityFrameworkCore;`, `using Pim.Infrastructure.Data;`, `using Pim.Infrastructure.Operations;`, `using Pim.Module.Calendar.Entities;`, and `using Pim.Module.Calendar.Services;`.

- [ ] **Step 3: 运行测试并确认深度方法不存在**

Run:

```powershell
dotnet test tests/Pim.UnitTests/Pim.UnitTests.csproj --filter FullyQualifiedName~OutlookDeepSyncTests
```

Expected: FAIL，两个深度同步方法不存在。

- [ ] **Step 4: 实现 full-resources**

Add to `OutlookCalendarSyncService`:

```csharp
public async Task<OutlookCalendarSyncResult> SyncFullResourcesAsync(
    Guid userId,
    Guid bindingId,
    CancellationToken ct)
{
    var binding = await _db.Set<OutlookCalendarBindingEntity>().SingleAsync(item => item.Id == bindingId, ct);
    var generation = Guid.NewGuid();
    string? nextLink = null;
    var read = 0;
    var created = 0;
    var confirmations = 0;
    do
    {
        var page = await _graph.GetEventsPageAsync(binding.ConnectionId, binding.GraphCalendarId, nextLink, ct);
        foreach (var remote in page.Value)
        {
            var projected = await _projection.UpsertAsync(userId, binding, remote, generation, ct);
            read++;
            if (projected.Outcome == "created") created++;
            if (projected.ConfirmationId is not null) confirmations++;
        }
        nextLink = page.NextLink;
    } while (nextLink is not null);

    binding.LastSyncedAt = _time.GetUtcNow();
    binding.UpdatedAt = _time.GetUtcNow();
    await _db.SaveChangesAsync(ct);
    return new OutlookCalendarSyncResult(binding.Id, "completed", read, created, confirmations, 0);
}
```

Do not run a missing-ID query in this method. `/events` resource enumeration is not a calendar-view deletion authority.

- [ ] **Step 5: 实现 180 天分片**

Add:

```csharp
public static IReadOnlyList<OutlookSyncWindow> SplitRange(OutlookSyncWindow requested)
{
    if (requested.End <= requested.Start)
        throw new ArgumentException("Range end must be after range start.", nameof(requested));
    var result = new List<OutlookSyncWindow>();
    var cursor = requested.Start;
    while (cursor < requested.End)
    {
        var end = cursor.AddDays(180);
        if (end > requested.End) end = requested.End;
        result.Add(new OutlookSyncWindow(cursor, end));
        cursor = end;
    }
    return result;
}
```

- [ ] **Step 6: 实现 range-instances 跨分片去重**

Add:

```csharp
public async Task<OutlookCalendarSyncResult> SyncRangeInstancesAsync(
    Guid userId,
    Guid bindingId,
    OutlookSyncWindow requested,
    CancellationToken ct)
{
    var binding = await _db.Set<OutlookCalendarBindingEntity>().SingleAsync(item => item.Id == bindingId, ct);
    var generation = Guid.NewGuid();
    var seen = new HashSet<string>(StringComparer.Ordinal);
    var read = 0;
    var created = 0;
    var confirmations = 0;

    foreach (var slice in SplitRange(requested))
    {
        string? url = CalendarViewUrl(binding.GraphCalendarId, slice);
        while (url is not null)
        {
            var page = await _graph.GetCalendarViewPageAsync(binding.ConnectionId, binding.GraphCalendarId, url, ct);
            foreach (var remote in page.Value)
            {
                read++;
                if (!seen.Add(remote.Id)) continue;
                var projected = await _projection.UpsertAsync(userId, binding, remote, generation, ct);
                if (projected.Outcome == "created") created++;
                if (projected.ConfirmationId is not null) confirmations++;
            }
            url = page.NextLink;
        }
    }

    binding.LastSyncedAt = _time.GetUtcNow();
    binding.UpdatedAt = _time.GetUtcNow();
    await _db.SaveChangesAsync(ct);
    return new OutlookCalendarSyncResult(binding.Id, "completed", read, created, confirmations, 0);
}
```

This method also performs no missing verification. It is an explicit instance backfill for the requested half-open range.

- [ ] **Step 7: 运行深度同步测试**

Run:

```powershell
dotnet test tests/Pim.UnitTests/Pim.UnitTests.csproj --filter FullyQualifiedName~OutlookDeepSyncTests
```

Expected: PASS；400 天分为 180/180/40，重复 immutable event ID 只 upsert 一次，资源扫描不生成删除确认。

- [ ] **Step 8: 提交深度模式**

```powershell
git add src/modules/Pim.Module.Calendar/Services/OutlookCalendarSyncService.cs tests/Pim.UnitTests/Calendar/OutlookDeepSyncTests.cs tests/Pim.UnitTests/Calendar/OutlookGraphFakes.cs
git commit -m "feat: add outlook deep sync modes"
```

Expected: 两种用户要求的手动强制获取能力都有独立测试。

## Task 15: 编排批次、并发、取消与 Hangfire 调度

**Files:**
- Create: `src/modules/Pim.Module.Calendar/Services/OutlookCalendarSyncCoordinator.cs`
- Create: `src/modules/Pim.Module.Calendar/Services/OutlookSyncJobs.cs`
- Create: `tests/Pim.UnitTests/Calendar/OutlookSyncCoordinatorTests.cs`
- Create: `tests/Pim.UnitTests/Calendar/OutlookSyncJobScheduleTests.cs`
- Modify: `src/modules/Pim.Module.Calendar/DTOs/OutlookSyncDtos.cs`
- Modify: `src/modules/Pim.Module.Calendar/Services/OutlookCalendarSyncService.cs`
- Modify: `src/modules/Pim.Module.Calendar/CalendarModule.cs`

- [ ] **Step 1: 定义 run API 契约**

Append to `OutlookSyncDtos.cs`:

```csharp
public sealed record StartOutlookSyncRunRequest(
    [Required] string Mode,
    IReadOnlyList<Guid>? CalendarBindingIds,
    DateTimeOffset? Start,
    DateTimeOffset? End);

public sealed record OutlookCalendarRunProgress(
    Guid BindingId,
    string CalendarName,
    string Status,
    int ReadCount,
    int CreatedCount,
    int ConfirmationCount,
    int FailureCount,
    string? ErrorCode,
    string? ErrorMessage);

public sealed record OutlookSyncRunResponse(
    Guid Id,
    string Mode,
    string Status,
    bool CancelRequested,
    DateTimeOffset? RequestedStart,
    DateTimeOffset? RequestedEnd,
    IReadOnlyList<OutlookCalendarRunProgress> Calendars,
    int ReadCount,
    int CreatedCount,
    int ConfirmationCount,
    int FailureCount,
    DateTimeOffset StartedAt,
    DateTimeOffset? FinishedAt,
    string? ErrorSummary);
```

- [ ] **Step 2: 抽出单日历同步接口**

Add above `OutlookCalendarSyncService` and implement it on the class:

```csharp
public interface IOutlookCalendarSyncService
{
    Task<OutlookCalendarSyncResult> SyncIncrementalAsync(Guid userId, Guid bindingId, bool forceBaseline, CancellationToken ct);
    Task<OutlookCalendarSyncResult> SyncFullResourcesAsync(Guid userId, Guid bindingId, CancellationToken ct);
    Task<OutlookCalendarSyncResult> SyncRangeInstancesAsync(Guid userId, Guid bindingId, OutlookSyncWindow requested, CancellationToken ct);
}

public sealed class OutlookCalendarSyncService : IOutlookCalendarSyncService
```

- [ ] **Step 3: 写最多两个日历并发和 partial 状态测试**

Create `tests/Pim.UnitTests/Calendar/OutlookSyncCoordinatorTests.cs`:

```csharp
[Fact]
public async Task Run_UsesAtMostTwoCalendarWorkersAndReportsPartialFailure()
{
    await using var provider = BuildProvider(new CountingSyncService(failThird: true));
    var seed = await SeedThreeBindingsAsync(provider);
    var coordinator = provider.GetRequiredService<OutlookCalendarSyncCoordinator>();
    var run = await coordinator.CreateRunAsync(seed.UserId, new StartOutlookSyncRunRequest(
        "incremental", null, null, null), CancellationToken.None);

    await coordinator.ExecuteRunAsync(run.Id, CancellationToken.None);

    await using var scope = provider.CreateAsyncScope();
    var stored = await scope.ServiceProvider.GetRequiredService<PimDbContext>()
        .Set<OutlookSyncBatchEntity>().SingleAsync();
    var worker = provider.GetRequiredService<CountingSyncService>();
    Assert.True(worker.MaxActive <= 2);
    Assert.Equal("partial", stored.Status);
    Assert.Equal(1, stored.FailureCount);
}

[Fact]
public async Task Cancel_SetsDurableFlagAndCancelsActiveReads()
{
    var worker = new CountingSyncService(blockUntilCanceled: true);
    await using var provider = BuildProvider(worker);
    var seed = await SeedThreeBindingsAsync(provider);
    var coordinator = provider.GetRequiredService<OutlookCalendarSyncCoordinator>();
    var run = await coordinator.CreateRunAsync(seed.UserId, new StartOutlookSyncRunRequest(
        "incremental", null, null, null), CancellationToken.None);
    var executing = coordinator.ExecuteRunAsync(run.Id, CancellationToken.None);
    await worker.Started.Task;

    await coordinator.CancelRunAsync(seed.UserId, run.Id, CancellationToken.None);
    await executing;

    await using var scope = provider.CreateAsyncScope();
    var stored = await scope.ServiceProvider.GetRequiredService<PimDbContext>()
        .Set<OutlookSyncBatchEntity>().SingleAsync();
    Assert.True(stored.CancelRequested);
    Assert.Equal("canceled", stored.Status);
}
```

Insert these helpers in `OutlookSyncCoordinatorTests`:

```csharp
private sealed record CoordinatorSeed(Guid UserId);

private static ServiceProvider BuildProvider(CountingSyncService worker)
{
    PimDbContext.RegisterModuleAssembly(typeof(OutlookConnectionEntity).Assembly);
    var databaseName = $"outlook-coordinator-{Guid.NewGuid()}";
    var services = new ServiceCollection();
    services.AddDbContext<PimDbContext>(options => options.UseInMemoryDatabase(databaseName));
    services.AddSingleton(worker);
    services.AddSingleton<IOutlookCalendarSyncService>(worker);
    services.AddSingleton<OutlookConnectionLock>();
    services.AddSingleton<OutlookCalendarSyncCoordinator>();
    return services.BuildServiceProvider();
}

private static async Task<CoordinatorSeed> SeedThreeBindingsAsync(ServiceProvider provider)
{
    await using var scope = provider.CreateAsyncScope();
    var db = scope.ServiceProvider.GetRequiredService<PimDbContext>();
    var userId = Guid.NewGuid();
    var connection = new OutlookConnectionEntity
    {
        UserId = userId,
        ClientId = Guid.NewGuid().ToString(),
        Status = "connected",
        TokenHealth = "healthy"
    };
    db.Add(connection);
    foreach (var number in Enumerable.Range(1, 3))
    {
        var calendar = new CalendarEntity
        {
            UserId = userId,
            Name = $"Calendar {number}",
            Source = "outlook"
        };
        db.AddRange(calendar, new OutlookCalendarBindingEntity
        {
            ConnectionId = connection.Id,
            PimCalendarId = calendar.Id,
            GraphCalendarId = $"graph-{number}",
            Name = calendar.Name,
            IsSelected = true,
            RemoteState = "active"
        });
    }
    await db.SaveChangesAsync();
    return new CoordinatorSeed(userId);
}

private sealed class CountingSyncService : IOutlookCalendarSyncService
{
    private readonly bool _failThird;
    private readonly bool _blockUntilCanceled;
    private int _calls;
    private int _active;
    private int _maxActive;

    public CountingSyncService(bool failThird = false, bool blockUntilCanceled = false)
    {
        _failThird = failThird;
        _blockUntilCanceled = blockUntilCanceled;
    }

    public int MaxActive => Volatile.Read(ref _maxActive);
    public TaskCompletionSource Started { get; } = new(TaskCreationOptions.RunContinuationsAsynchronously);

    public Task<OutlookCalendarSyncResult> SyncIncrementalAsync(
        Guid userId, Guid bindingId, bool forceBaseline, CancellationToken ct) => RunAsync(bindingId, ct);
    public Task<OutlookCalendarSyncResult> SyncFullResourcesAsync(
        Guid userId, Guid bindingId, CancellationToken ct) => RunAsync(bindingId, ct);
    public Task<OutlookCalendarSyncResult> SyncRangeInstancesAsync(
        Guid userId, Guid bindingId, OutlookSyncWindow requested, CancellationToken ct) => RunAsync(bindingId, ct);

    private async Task<OutlookCalendarSyncResult> RunAsync(Guid bindingId, CancellationToken ct)
    {
        var call = Interlocked.Increment(ref _calls);
        var active = Interlocked.Increment(ref _active);
        UpdateMaximum(active);
        Started.TrySetResult();
        try
        {
            if (_blockUntilCanceled) await Task.Delay(Timeout.InfiniteTimeSpan, ct);
            else await Task.Delay(50, ct);
            if (_failThird && call == 3) throw new HttpRequestException("third calendar failed");
            return new OutlookCalendarSyncResult(bindingId, "completed", 1, 1, 0, 0);
        }
        finally
        {
            Interlocked.Decrement(ref _active);
        }
    }

    private void UpdateMaximum(int candidate)
    {
        var current = Volatile.Read(ref _maxActive);
        while (candidate > current)
        {
            var observed = Interlocked.CompareExchange(ref _maxActive, candidate, current);
            if (observed == current) return;
            current = observed;
        }
    }
}
```

Add `using Microsoft.Extensions.DependencyInjection;`.

- [ ] **Step 4: 运行 coordinator 测试并确认类型不存在**

Run:

```powershell
dotnet test tests/Pim.UnitTests/Pim.UnitTests.csproj --filter FullyQualifiedName~OutlookSyncCoordinatorTests
```

Expected: FAIL，编译错误指向 coordinator。

- [ ] **Step 5: 实现 durable run 创建和读取**

Create `src/modules/Pim.Module.Calendar/Services/OutlookCalendarSyncCoordinator.cs`:

```csharp
using System.Collections.Concurrent;
using System.Text.Json;
using Microsoft.EntityFrameworkCore;
using Microsoft.Extensions.DependencyInjection;
using Pim.Infrastructure.Data;
using Pim.Module.Calendar.DTOs;
using Pim.Module.Calendar.Entities;

namespace Pim.Module.Calendar.Services;

public sealed class OutlookCalendarSyncCoordinator
{
    private static readonly JsonSerializerOptions JsonOptions = new(JsonSerializerDefaults.Web);
    private static readonly HashSet<string> Modes = new(StringComparer.Ordinal)
        { "incremental", "rolling-baseline", "full-resources", "range-instances" };
    private readonly IServiceScopeFactory _scopeFactory;
    private readonly OutlookConnectionLock _connectionLock;
    private readonly ConcurrentDictionary<Guid, CancellationTokenSource> _active = new();

    public OutlookCalendarSyncCoordinator(IServiceScopeFactory scopeFactory, OutlookConnectionLock connectionLock)
    {
        _scopeFactory = scopeFactory;
        _connectionLock = connectionLock;
    }

    public async Task<OutlookSyncRunResponse> CreateRunAsync(
        Guid userId,
        StartOutlookSyncRunRequest request,
        CancellationToken ct)
    {
        if (!Modes.Contains(request.Mode)) throw new ArgumentException("Unknown Outlook sync mode.", nameof(request));
        if (request.Mode == "range-instances"
            && (request.Start is null || request.End is null || request.End <= request.Start))
            throw new ArgumentException("Range instance sync requires a valid start and end.", nameof(request));

        await using var scope = _scopeFactory.CreateAsyncScope();
        var db = scope.ServiceProvider.GetRequiredService<PimDbContext>();
        var connection = await db.Set<OutlookConnectionEntity>().SingleAsync(item => item.UserId == userId, ct);
        var selected = await db.Set<OutlookCalendarBindingEntity>()
            .Where(item => item.ConnectionId == connection.Id && item.IsSelected && item.RemoteState == "active")
            .ToListAsync(ct);
        if (request.CalendarBindingIds is { Count: > 0 })
        {
            if (request.CalendarBindingIds.Any(id => selected.All(item => item.Id != id)))
                throw new InvalidOperationException("Sync request contains an unselected or foreign calendar.");
            selected = selected.Where(item => request.CalendarBindingIds.Contains(item.Id)).ToList();
        }

        var progress = selected.Select(item => new OutlookCalendarRunProgress(
            item.Id, item.Name, "queued", 0, 0, 0, 0, null, null)).ToList();
        var run = new OutlookSyncBatchEntity
        {
            UserId = userId,
            ConnectionId = connection.Id,
            Mode = request.Mode,
            Status = "queued",
            RequestedWindowStart = request.Start,
            RequestedWindowEnd = request.End,
            RequestedCalendarIdsJson = JsonSerializer.Serialize(selected.Select(item => item.Id), JsonOptions),
            PerCalendarJson = JsonSerializer.Serialize(progress, JsonOptions),
            StartedAt = DateTimeOffset.UtcNow
        };
        db.Add(run);
        await db.SaveChangesAsync(ct);
        return Map(run);
    }

    public async Task<OutlookSyncRunResponse> GetRunAsync(Guid userId, Guid runId, CancellationToken ct)
    {
        await using var scope = _scopeFactory.CreateAsyncScope();
        var run = await scope.ServiceProvider.GetRequiredService<PimDbContext>()
            .Set<OutlookSyncBatchEntity>().AsNoTracking()
            .SingleAsync(item => item.Id == runId && item.UserId == userId, ct);
        return Map(run);
    }

    private static OutlookSyncRunResponse Map(OutlookSyncBatchEntity run) => new(
        run.Id, run.Mode, run.Status, run.CancelRequested,
        run.RequestedWindowStart, run.RequestedWindowEnd,
        JsonSerializer.Deserialize<IReadOnlyList<OutlookCalendarRunProgress>>(run.PerCalendarJson, JsonOptions) ?? [],
        run.ReadCount, run.CreatedCount, run.ConfirmationCount, run.FailureCount,
        run.StartedAt, run.FinishedAt, run.ErrorSummary);
}
```

- [ ] **Step 6: 实现 connection 锁和最多两个 worker**

Add `ExecuteRunAsync`:

```csharp
public async Task ExecuteRunAsync(Guid runId, CancellationToken jobToken)
{
    var cancellation = CancellationTokenSource.CreateLinkedTokenSource(jobToken);
    if (!_active.TryAdd(runId, cancellation)) return;
    try
    {
        var seed = await LoadRunSeedAsync(runId, cancellation.Token);
        await using var held = await _connectionLock.AcquireAsync(seed.ConnectionId, cancellation.Token);
        await SetRunStateAsync(runId, "running", cancellation.Token);
        var results = new ConcurrentBag<OutlookCalendarRunProgress>();
        using var progressGate = new SemaphoreSlim(1, 1);

        await Parallel.ForEachAsync(seed.BindingIds, new ParallelOptions
        {
            MaxDegreeOfParallelism = 2,
            CancellationToken = cancellation.Token
        }, async (bindingId, ct) =>
        {
            await using var scope = _scopeFactory.CreateAsyncScope();
            var sync = scope.ServiceProvider.GetRequiredService<IOutlookCalendarSyncService>();
            try
            {
                var result = seed.Mode switch
                {
                    "incremental" => await sync.SyncIncrementalAsync(seed.UserId, bindingId, false, ct),
                    "rolling-baseline" => await sync.SyncIncrementalAsync(seed.UserId, bindingId, true, ct),
                    "full-resources" => await sync.SyncFullResourcesAsync(seed.UserId, bindingId, ct),
                    "range-instances" => await sync.SyncRangeInstancesAsync(
                        seed.UserId, bindingId, new OutlookSyncWindow(seed.Start!.Value, seed.End!.Value), ct),
                    _ => throw new InvalidOperationException("Unsupported Outlook sync mode.")
                };
                results.Add(new OutlookCalendarRunProgress(
                    bindingId, seed.Names[bindingId], result.Status, result.ReadCount,
                    result.CreatedCount, result.ConfirmationCount, result.FailureCount,
                    result.ErrorCode, result.ErrorMessage));
            }
            catch (OperationCanceledException) when (ct.IsCancellationRequested)
            {
                results.Add(new OutlookCalendarRunProgress(bindingId, seed.Names[bindingId], "canceled", 0, 0, 0, 0, null, null));
            }
            catch (Exception exception)
            {
                results.Add(new OutlookCalendarRunProgress(
                    bindingId, seed.Names[bindingId], "failed", 0, 0, 0, 1,
                    exception.GetType().Name, exception.Message));
            }
            await progressGate.WaitAsync(CancellationToken.None);
            try { await StoreProgressAsync(runId, results, CancellationToken.None); }
            finally { progressGate.Release(); }
        });

        await FinishAsync(runId, results, canceled: false, CancellationToken.None);
    }
    catch (OperationCanceledException)
    {
        await FinishAsync(runId, [], canceled: true, CancellationToken.None);
    }
    finally
    {
        if (_active.TryRemove(runId, out var removed)) removed.Dispose();
    }
}
```

Add these complete helpers to the coordinator:

```csharp
private sealed record RunSeed(
    Guid UserId,
    Guid ConnectionId,
    string Mode,
    DateTimeOffset? Start,
    DateTimeOffset? End,
    IReadOnlyList<Guid> BindingIds,
    IReadOnlyDictionary<Guid, string> Names);

private async Task<RunSeed> LoadRunSeedAsync(Guid runId, CancellationToken ct)
{
    await using var scope = _scopeFactory.CreateAsyncScope();
    var db = scope.ServiceProvider.GetRequiredService<PimDbContext>();
    var run = await db.Set<OutlookSyncBatchEntity>().AsNoTracking().SingleAsync(item => item.Id == runId, ct);
    if (run.CancelRequested) throw new OperationCanceledException("Outlook sync run was canceled.", ct);
    var bindingIds = JsonSerializer.Deserialize<IReadOnlyList<Guid>>(run.RequestedCalendarIdsJson, JsonOptions) ?? [];
    var names = await db.Set<OutlookCalendarBindingEntity>()
        .Where(item => bindingIds.Contains(item.Id))
        .ToDictionaryAsync(item => item.Id, item => item.Name, ct);
    if (names.Count != bindingIds.Count)
        throw new InvalidOperationException("One or more Outlook calendar bindings no longer exist.");
    return new RunSeed(
        run.UserId,
        run.ConnectionId ?? throw new InvalidOperationException("Outlook sync run has no connection."),
        run.Mode,
        run.RequestedWindowStart,
        run.RequestedWindowEnd,
        bindingIds,
        names);
}

private async Task SetRunStateAsync(Guid runId, string status, CancellationToken ct)
{
    await using var scope = _scopeFactory.CreateAsyncScope();
    var db = scope.ServiceProvider.GetRequiredService<PimDbContext>();
    var run = await db.Set<OutlookSyncBatchEntity>().SingleAsync(item => item.Id == runId, ct);
    run.Status = status;
    run.UpdatedAt = DateTimeOffset.UtcNow;
    await db.SaveChangesAsync(ct);
}

private async Task StoreProgressAsync(
    Guid runId,
    IEnumerable<OutlookCalendarRunProgress> current,
    CancellationToken ct)
{
    await using var scope = _scopeFactory.CreateAsyncScope();
    var db = scope.ServiceProvider.GetRequiredService<PimDbContext>();
    var run = await db.Set<OutlookSyncBatchEntity>().SingleAsync(item => item.Id == runId, ct);
    var stored = JsonSerializer.Deserialize<IReadOnlyList<OutlookCalendarRunProgress>>(run.PerCalendarJson, JsonOptions) ?? [];
    var updates = current.ToDictionary(item => item.BindingId);
    var merged = stored.Select(item => updates.GetValueOrDefault(item.BindingId, item))
        .OrderBy(item => item.CalendarName)
        .ToList();
    run.PerCalendarJson = JsonSerializer.Serialize(merged, JsonOptions);
    run.ReadCount = merged.Sum(item => item.ReadCount);
    run.CreatedCount = merged.Sum(item => item.CreatedCount);
    run.ConfirmationCount = merged.Sum(item => item.ConfirmationCount);
    run.FailureCount = merged.Sum(item => item.FailureCount);
    run.UpdatedAt = DateTimeOffset.UtcNow;
    await db.SaveChangesAsync(ct);
}

private async Task FinishAsync(
    Guid runId,
    IEnumerable<OutlookCalendarRunProgress> current,
    bool canceled,
    CancellationToken ct)
{
    await StoreProgressAsync(runId, current, ct);
    await using var scope = _scopeFactory.CreateAsyncScope();
    var db = scope.ServiceProvider.GetRequiredService<PimDbContext>();
    var run = await db.Set<OutlookSyncBatchEntity>().SingleAsync(item => item.Id == runId, ct);
    var progress = JsonSerializer.Deserialize<IReadOnlyList<OutlookCalendarRunProgress>>(run.PerCalendarJson, JsonOptions) ?? [];
    var failures = progress.Count(item => item.Status == "failed");
    var successes = progress.Count(item => item.Status == "completed");
    run.Status = canceled || run.CancelRequested
        ? "canceled"
        : failures == 0 ? "completed"
        : successes > 0 ? "partial"
        : "failed";
    run.ErrorSummary = failures == 0 ? null : $"{failures} 个日历同步失败。";
    run.FinishedAt = DateTimeOffset.UtcNow;
    run.UpdatedAt = DateTimeOffset.UtcNow;
    await db.SaveChangesAsync(ct);
}
```

- [ ] **Step 7: 实现 durable + in-process 取消**

Add:

```csharp
public async Task CancelRunAsync(Guid userId, Guid runId, CancellationToken ct)
{
    await using var scope = _scopeFactory.CreateAsyncScope();
    var db = scope.ServiceProvider.GetRequiredService<PimDbContext>();
    var run = await db.Set<OutlookSyncBatchEntity>()
        .SingleAsync(item => item.Id == runId && item.UserId == userId, ct);
    run.CancelRequested = true;
    run.UpdatedAt = DateTimeOffset.UtcNow;
    await db.SaveChangesAsync(ct);
    if (_active.TryGetValue(runId, out var active)) active.Cancel();
}
```

`LoadRunSeedAsync` above rejects a persisted canceled run before any Graph read. Cancellation tokens flow into Graph reads and stop retries immediately.

- [ ] **Step 8: 写调度常量测试**

Create `tests/Pim.UnitTests/Calendar/OutlookSyncJobScheduleTests.cs`:

```csharp
using Pim.Module.Calendar.Services;
using Xunit;

namespace Pim.UnitTests.Calendar;

public sealed class OutlookSyncJobScheduleTests
{
    [Fact]
    public void Schedules_AreFiveMinutesDailyBaselineAndExecutionWakeup()
    {
        Assert.Contains(OutlookSyncJobs.Schedules, item => item.Id == "outlook-sync-five-minute" && item.Cron == "*/5 * * * *");
        Assert.Contains(OutlookSyncJobs.Schedules, item => item.Id == "outlook-default-calendar-daily-baseline" && item.Cron == "15 3 * * *");
        Assert.Contains(OutlookSyncJobs.Schedules, item => item.Id == "outlook-operation-execution-wakeup" && item.Cron == "* * * * *");
    }
}
```

- [ ] **Step 9: 实现 Hangfire job methods 和 durable execution 唤醒**

Create `src/modules/Pim.Module.Calendar/Services/OutlookSyncJobs.cs`:

```csharp
using Hangfire;
using Microsoft.EntityFrameworkCore;
using Pim.Infrastructure.Data;
using Pim.Module.Calendar.DTOs;
using Pim.Module.Calendar.Entities;

namespace Pim.Module.Calendar.Services;

public sealed record OutlookRecurringSchedule(string Id, string Cron);

public sealed class OutlookSyncJobs
{
    public static readonly IReadOnlyList<OutlookRecurringSchedule> Schedules =
    [
        new("outlook-sync-five-minute", "*/5 * * * *"),
        new("outlook-default-calendar-daily-baseline", "15 3 * * *"),
        new("outlook-operation-execution-wakeup", "* * * * *")
    ];

    private readonly PimDbContext _db;
    private readonly OutlookCalendarSyncCoordinator _coordinator;
    private readonly OutlookConfirmedOperationHandler _operations;

    public OutlookSyncJobs(
        PimDbContext db,
        OutlookCalendarSyncCoordinator coordinator,
        OutlookConfirmedOperationHandler operations)
    {
        _db = db;
        _coordinator = coordinator;
        _operations = operations;
    }

    public Task EnqueueIncrementalAsync() => EnqueueForConnectionsAsync("incremental");
    public Task EnqueueDailyBaselinesAsync() => EnqueueForConnectionsAsync("rolling-baseline");
    public Task RunSyncAsync(Guid runId) => _coordinator.ExecuteRunAsync(runId, CancellationToken.None);

    public async Task WakeExecutionsAsync()
    {
        var due = await _db.Set<OutlookOperationExecutionEntity>()
            .Where(item => (item.State == "queued" || item.State == "retryable-failed")
                && (item.NextAttemptAt == null || item.NextAttemptAt <= DateTimeOffset.UtcNow))
            .OrderBy(item => item.CreatedAt)
            .Select(item => item.Id)
            .Take(20)
            .ToListAsync();
        foreach (var executionId in due)
            await _operations.ExecuteAsync(executionId, CancellationToken.None);
    }

    private async Task EnqueueForConnectionsAsync(string mode)
    {
        var userIds = await _db.Set<OutlookConnectionEntity>()
            .Where(item => item.Status == "connected")
            .Select(item => item.UserId)
            .ToListAsync();
        foreach (var userId in userIds)
        {
            var hasActive = await _db.Set<OutlookSyncBatchEntity>().AnyAsync(item =>
                item.UserId == userId && (item.Status == "queued" || item.Status == "running"));
            if (hasActive) continue;
            var run = await _coordinator.CreateRunAsync(
                userId, new StartOutlookSyncRunRequest(mode, null, null, null), CancellationToken.None);
            BackgroundJob.Enqueue<OutlookSyncJobs>(job => job.RunSyncAsync(run.Id));
        }
    }

    public static void RegisterRecurring()
    {
        RecurringJob.AddOrUpdate<OutlookSyncJobs>(Schedules[0].Id, job => job.EnqueueIncrementalAsync(), Schedules[0].Cron);
        RecurringJob.AddOrUpdate<OutlookSyncJobs>(Schedules[1].Id, job => job.EnqueueDailyBaselinesAsync(), Schedules[1].Cron);
        RecurringJob.AddOrUpdate<OutlookSyncJobs>(Schedules[2].Id, job => job.WakeExecutionsAsync(), Schedules[2].Cron);
    }
}
```

Hangfire only wakes rows already persisted in `outlook_sync_batches` or `outlook_operation_executions`; losing an enqueued Hangfire item cannot lose the business operation.

- [ ] **Step 10: 注册生命周期和启动同步**

In `CalendarModule.RegisterServices`, register `TimeProvider.System`, all sync dependencies, and singleton coordinator:

```csharp
services.AddSingleton(TimeProvider.System);
services.AddSingleton<OutlookConnectionLock>();
services.AddSingleton<OutlookCalendarSyncCoordinator>();
services.AddScoped<IOutlookCalendarSyncService, OutlookCalendarSyncService>();
services.AddScoped<OutlookSyncJobs>();
services.AddScoped<OutlookConfirmedOperationHandler>();
```

Replace `InitializeAsync` with:

```csharp
public async Task InitializeAsync(IServiceProvider serviceProvider)
{
    using var scope = serviceProvider.CreateScope();
    await scope.ServiceProvider.GetRequiredService<OutlookAuthorizationSessionRunner>()
        .FailInterruptedSessionsAsync(CancellationToken.None);
    OutlookSyncJobs.RegisterRecurring();
    BackgroundJob.Enqueue<OutlookSyncJobs>(job => job.EnqueueIncrementalAsync());
}
```

Add `using Hangfire;`.

- [ ] **Step 11: 运行 coordinator/job 测试**

Run:

```powershell
dotnet test tests/Pim.UnitTests/Pim.UnitTests.csproj --filter "FullyQualifiedName~OutlookSyncCoordinatorTests|FullyQualifiedName~OutlookSyncJobScheduleTests"
```

Expected: PASS；最多两个 worker，并发运行可取消，三类 recurring cron 精确匹配。

- [ ] **Step 12: 提交编排和调度**

```powershell
git add src/modules/Pim.Module.Calendar/DTOs/OutlookSyncDtos.cs src/modules/Pim.Module.Calendar/Services/OutlookCalendarSyncCoordinator.cs src/modules/Pim.Module.Calendar/Services/OutlookCalendarSyncService.cs src/modules/Pim.Module.Calendar/Services/OutlookSyncJobs.cs src/modules/Pim.Module.Calendar/CalendarModule.cs tests/Pim.UnitTests/Calendar/OutlookSyncCoordinatorTests.cs tests/Pim.UnitTests/Calendar/OutlookSyncJobScheduleTests.cs
git commit -m "feat: schedule and coordinate outlook sync runs"
```

Expected: 启动、每 5 分钟、每日基线和手动 run 共用同一 coordinator。

## Task 16: 暴露设置、授权、发现、同步、诊断和变更 API

**Files:**
- Create: `src/modules/Pim.Module.Calendar/OutlookEndpoints.cs`
- Create: `src/modules/Pim.Module.Calendar/Services/OutlookDiagnosticsService.cs`
- Create: `src/modules/Pim.Module.Calendar/Services/OutlookSyncFacade.cs`
- Create: `tests/Pim.UnitTests/Calendar/OutlookApiContractTests.cs`
- Modify: `src/modules/Pim.Module.Calendar/DTOs/OutlookSyncDtos.cs`
- Modify: `src/modules/Pim.Module.Calendar/CalendarModule.cs`
- Modify: `tests/Pim.UnitTests/Pim.UnitTests.csproj`

- [ ] **Step 1: 增加 TestServer 依赖**

Add to `tests/Pim.UnitTests/Pim.UnitTests.csproj`:

```xml
<PackageReference Include="Microsoft.AspNetCore.TestHost" Version="8.0.11" />
```

Run `dotnet restore tests/Pim.UnitTests/Pim.UnitTests.csproj`; expected exit code 0.

- [ ] **Step 2: 定义诊断 DTO**

Append to `OutlookSyncDtos.cs`:

```csharp
public sealed record OutlookDiagnosticCheck(
    string Code,
    string Label,
    string Status,
    string Message,
    string? TechnicalCode = null);

public sealed record OutlookDiagnosticsResponse(
    string Status,
    DateTimeOffset CheckedAt,
    IReadOnlyList<OutlookDiagnosticCheck> Checks);
```

- [ ] **Step 3: 写 API 路径、敏感字段和用户边界失败测试**

Create `tests/Pim.UnitTests/Calendar/OutlookApiContractTests.cs` using `WebApplication`, `UseTestServer`, a test authentication scheme with a fixed `sub` claim, and a recording fake `IOutlookSyncFacade`:

```csharp
[Fact]
public async Task SettingsAndAuthSessionContracts_DoNotExposeSecretsOrDeviceCode()
{
    await using var app = await CreateAppAsync();
    var client = app.GetTestClient();

    var settings = await client.GetStringAsync("/api/v1/calendar/outlook/settings");
    var auth = await (await client.PostAsJsonAsync("/api/v1/calendar/outlook/auth-sessions", new { })).Content.ReadAsStringAsync();

    Assert.Contains("Calendars.ReadWrite", settings);
    Assert.DoesNotContain("refreshToken", settings, StringComparison.OrdinalIgnoreCase);
    Assert.DoesNotContain("msalCache", settings, StringComparison.OrdinalIgnoreCase);
    Assert.Contains("userCode", auth);
    Assert.DoesNotContain("deviceCode", auth, StringComparison.OrdinalIgnoreCase);
}

[Theory]
[InlineData("POST", "/api/v1/calendar/outlook/calendars/discover")]
[InlineData("GET", "/api/v1/calendar/outlook/calendars")]
[InlineData("POST", "/api/v1/calendar/outlook/sync-runs")]
[InlineData("POST", "/api/v1/calendar/outlook/diagnostics")]
public async Task OutlookEndpoints_PassAuthenticatedUserToFacade(string method, string path)
{
    await using var app = await CreateAppAsync();
    var client = app.GetTestClient();
    using var request = new HttpRequestMessage(new HttpMethod(method), path);
    if (method == "POST") request.Content = JsonContent.Create(path.EndsWith("sync-runs")
        ? new { mode = "incremental" }
        : new { });

    var response = await client.SendAsync(request);

    Assert.True(response.IsSuccessStatusCode);
    Assert.Equal(TestUserId, app.Services.GetRequiredService<RecordingOutlookFacade>().LastUserId);
}
```

Insert these complete helpers in `OutlookApiContractTests`:

```csharp
private static readonly Guid TestUserId = Guid.Parse("99999999-9999-9999-9999-999999999999");

private static async Task<WebApplication> CreateAppAsync()
{
    var builder = WebApplication.CreateBuilder();
    builder.WebHost.UseTestServer();
    builder.Services.AddAuthorization();
    builder.Services.AddAuthentication("test")
        .AddScheme<AuthenticationSchemeOptions, TestAuthenticationHandler>("test", _ => { });
    builder.Services.AddSingleton<ICurrentUserService>(new FixedCurrentUser());
    builder.Services.AddSingleton<RecordingOutlookFacade>();
    builder.Services.AddSingleton<IOutlookSyncFacade>(provider => provider.GetRequiredService<RecordingOutlookFacade>());
    var app = builder.Build();
    app.UseAuthentication();
    app.UseAuthorization();
    app.MapGroup("/api/v1/calendar").RequireAuthorization().MapOutlookEndpoints();
    await app.StartAsync();
    return app;
}

private sealed class TestAuthenticationHandler(
    IOptionsMonitor<AuthenticationSchemeOptions> options,
    ILoggerFactory logger,
    UrlEncoder encoder) : AuthenticationHandler<AuthenticationSchemeOptions>(options, logger, encoder)
{
    protected override Task<AuthenticateResult> HandleAuthenticateAsync()
    {
        var identity = new ClaimsIdentity([new Claim(ClaimTypes.NameIdentifier, TestUserId.ToString())], "test");
        return Task.FromResult(AuthenticateResult.Success(
            new AuthenticationTicket(new ClaimsPrincipal(identity), "test")));
    }
}

private sealed class FixedCurrentUser : ICurrentUserService
{
    public Guid? UserId => TestUserId;
    public string? Role => "user";
}

private sealed class RecordingOutlookFacade : IOutlookSyncFacade
{
    public Guid? LastUserId { get; private set; }

    public Task<OutlookSettingsResponse> GetSettingsAsync(Guid userId, CancellationToken ct)
        => Seen(userId, new OutlookSettingsResponse(
            "outlook", "11111111-1111-1111-1111-111111111111", "common", "common",
            "https://login.microsoftonline.com/common", ["Calendars.ReadWrite", "User.Read"],
            "connected", "healthy", "Test User", "test@example.com", null, null, null));

    public Task<OutlookSettingsResponse> UpdateSettingsAsync(
        Guid userId, UpdateOutlookSettingsRequest request, CancellationToken ct)
        => GetSettingsAsync(userId, ct);

    public Task<OutlookAuthorizationSessionResponse> StartAuthorizationAsync(Guid userId, CancellationToken ct)
        => Seen(userId, new OutlookAuthorizationSessionResponse(
            Guid.NewGuid(), "waiting-for-user", "https://microsoft.com/devicelogin", "ABCD-EFGH",
            DateTimeOffset.UtcNow.AddMinutes(15), null, null, null, null, null));

    public Task<OutlookAuthorizationSessionResponse> GetAuthorizationAsync(Guid userId, Guid sessionId, CancellationToken ct)
        => StartAuthorizationAsync(userId, ct);

    public Task CancelAuthorizationAsync(Guid userId, Guid sessionId, CancellationToken ct)
        => Seen(userId);

    public Task<OutlookCalendarDiscoveryResponse> DiscoverCalendarsAsync(Guid userId, CancellationToken ct)
        => Seen(userId, new OutlookCalendarDiscoveryResponse(0, 0, 0, []));

    public Task<IReadOnlyList<OutlookCalendarGroupResponse>> ListCalendarsAsync(Guid userId, CancellationToken ct)
        => Seen<IReadOnlyList<OutlookCalendarGroupResponse>>(userId, []);

    public Task<IReadOnlyList<OutlookCalendarGroupResponse>> UpdateSelectionAsync(
        Guid userId, UpdateOutlookCalendarSelectionRequest request, CancellationToken ct)
        => Seen<IReadOnlyList<OutlookCalendarGroupResponse>>(userId, []);

    public Task<OutlookSyncRunResponse> StartRunAsync(
        Guid userId, StartOutlookSyncRunRequest request, CancellationToken ct)
        => Seen(userId, Run(request.Mode));

    public Task<OutlookSyncRunResponse> GetRunAsync(Guid userId, Guid runId, CancellationToken ct)
        => Seen(userId, Run("incremental") with { Id = runId });

    public Task CancelRunAsync(Guid userId, Guid runId, CancellationToken ct) => Seen(userId);

    public Task<OperationConfirmationDto> PreviewUpdateAsync(
        Guid userId, Guid eventId, OutlookEventChangeRequest request, CancellationToken ct)
        => Task.FromException<OperationConfirmationDto>(new NotSupportedException());

    public Task<OperationConfirmationDto> PreviewDeleteAsync(Guid userId, Guid eventId, CancellationToken ct)
        => Task.FromException<OperationConfirmationDto>(new NotSupportedException());

    public Task<EventEntity> CopyToPimAsync(
        Guid userId, Guid eventId, CopyOutlookEventRequest request, CancellationToken ct)
        => Task.FromException<EventEntity>(new NotSupportedException());

    public Task<OutlookDiagnosticsResponse> RunDiagnosticsAsync(Guid userId, CancellationToken ct)
        => Seen(userId, new OutlookDiagnosticsResponse("passed", DateTimeOffset.UtcNow,
            [new OutlookDiagnosticCheck("profile", "账号读取", "passed", "可读取账号。")]));

    private Task<T> Seen<T>(Guid userId, T value)
    {
        LastUserId = userId;
        return Task.FromResult(value);
    }

    private Task Seen(Guid userId)
    {
        LastUserId = userId;
        return Task.CompletedTask;
    }

    private static OutlookSyncRunResponse Run(string mode) => new(
        Guid.NewGuid(), mode, "queued", false, null, null, [],
        0, 0, 0, 0, DateTimeOffset.UtcNow, null, null);
}
```

Add these test usings:

```csharp
using System.Net.Http.Json;
using System.Security.Claims;
using System.Text.Encodings.Web;
using Microsoft.AspNetCore.Authentication;
using Microsoft.AspNetCore.Builder;
using Microsoft.AspNetCore.TestHost;
using Microsoft.Extensions.DependencyInjection;
using Microsoft.Extensions.Logging;
using Microsoft.Extensions.Options;
using Pim.Core.Operations;
using Pim.Infrastructure.Auth;
using Pim.Module.Calendar.DTOs;
using Pim.Module.Calendar.Entities;
using Pim.Module.Calendar.Services;
```

- [ ] **Step 4: 运行 API 测试并确认 endpoint/facade 不存在**

Run:

```powershell
dotnet test tests/Pim.UnitTests/Pim.UnitTests.csproj --filter FullyQualifiedName~OutlookApiContractTests
```

Expected: FAIL，编译错误指向 `IOutlookSyncFacade` 或 `MapOutlookEndpoints`。

- [ ] **Step 5: 实现 diagnostics service**

Create `src/modules/Pim.Module.Calendar/Services/OutlookDiagnosticsService.cs`:

```csharp
using Microsoft.EntityFrameworkCore;
using Pim.Infrastructure.Data;
using Pim.Module.Calendar.DTOs;
using Pim.Module.Calendar.Entities;

namespace Pim.Module.Calendar.Services;

public sealed class OutlookDiagnosticsService
{
    private readonly PimDbContext _db;
    private readonly IOutlookAccessTokenProvider _tokens;
    private readonly IGraphCalendarClient _graph;
    private readonly OutlookEventMapper _mapper;

    public OutlookDiagnosticsService(
        PimDbContext db,
        IOutlookAccessTokenProvider tokens,
        IGraphCalendarClient graph,
        OutlookEventMapper mapper)
    {
        _db = db;
        _tokens = tokens;
        _graph = graph;
        _mapper = mapper;
    }

    public async Task<OutlookDiagnosticsResponse> RunAsync(Guid userId, CancellationToken ct)
    {
        var checks = new List<OutlookDiagnosticCheck>();
        var connection = await _db.Set<OutlookConnectionEntity>().SingleOrDefaultAsync(item => item.UserId == userId, ct);
        if (connection is null)
            return Result([new("connection", "连接配置", "failed", "尚未保存 Microsoft Client ID。")]);
        checks.Add(new OutlookDiagnosticCheck("connection", "连接配置", "passed", "已找到当前用户的 Microsoft 连接。"));

        try
        {
            _ = await _tokens.AcquireAccessTokenAsync(connection.Id, false, ct);
            checks.Add(new OutlookDiagnosticCheck("token", "静默授权", "passed", "MSAL cache 可以静默获取访问令牌。"));
        }
        catch (Exception exception)
        {
            checks.Add(new OutlookDiagnosticCheck("token", "静默授权", "failed", "需要重新连接 Microsoft 账号。", exception.GetType().Name));
            return Result(checks);
        }

        try
        {
            var me = await _graph.GetMeAsync(connection.Id, ct);
            checks.Add(new OutlookDiagnosticCheck("profile", "账号读取", "passed", $"Microsoft 账号：{me.DisplayName ?? me.UserPrincipalName ?? me.Id}"));
            var groups = await CountGroupsAsync(connection.Id, ct);
            var calendars = await CountCalendarsAsync(connection.Id, ct);
            checks.Add(new OutlookDiagnosticCheck("discovery", "日历发现", "passed", $"可读取 {groups} 个分组和 {calendars} 个根日历。"));
        }
        catch (GraphRequestException exception)
        {
            var message = exception.StatusCode == HttpStatusCode.Forbidden
                ? "Microsoft 拒绝日历权限，请检查委托权限或管理员同意。"
                : "Microsoft Graph 读取失败，请稍后重试。";
            checks.Add(new OutlookDiagnosticCheck("graph", "Graph 读取", "failed", message, $"HTTP {(int)exception.StatusCode}"));
            return Result(checks);
        }

        var probe = new GraphEventDto(
            "probe", "probe", null,
            new GraphDateTimeTimeZoneDto("2026-07-10T09:00:00", "UTC"),
            new GraphDateTimeTimeZoneDto("2026-07-10T10:00:00", "UTC"),
            false, null, null, "singleInstance", null, null, null, null, null, null);
        var mapped = _mapper.Snapshot(probe);
        checks.Add(new OutlookDiagnosticCheck(
            "timezone", "时间语义", mapped.DtStart.Offset == TimeSpan.Zero ? "passed" : "failed",
            "Graph UTC 时间可稳定映射，界面将按 Asia/Shanghai 显示。"));
        return Result(checks);
    }

    private async Task<int> CountGroupsAsync(Guid connectionId, CancellationToken ct)
    {
        var count = 0;
        string? next = null;
        do { var page = await _graph.GetCalendarGroupsPageAsync(connectionId, next, ct); count += page.Value.Count; next = page.NextLink; }
        while (next is not null);
        return count;
    }

    private async Task<int> CountCalendarsAsync(Guid connectionId, CancellationToken ct)
    {
        var count = 0;
        string? next = null;
        do { var page = await _graph.GetCalendarsPageAsync(connectionId, next, ct); count += page.Value.Count; next = page.NextLink; }
        while (next is not null);
        return count;
    }

    private static OutlookDiagnosticsResponse Result(IReadOnlyList<OutlookDiagnosticCheck> checks)
        => new(checks.Any(item => item.Status == "failed") ? "failed" : "passed", DateTimeOffset.UtcNow, checks);
}
```

Add `using System.Net;`.

- [ ] **Step 6: 定义 facade 接口**

Create the beginning of `src/modules/Pim.Module.Calendar/Services/OutlookSyncFacade.cs`:

```csharp
using Hangfire;
using Microsoft.EntityFrameworkCore;
using Pim.Core.Operations;
using Pim.Infrastructure.Data;
using Pim.Module.Calendar.DTOs;
using Pim.Module.Calendar.Entities;

namespace Pim.Module.Calendar.Services;

public interface IOutlookSyncFacade
{
    Task<OutlookSettingsResponse> GetSettingsAsync(Guid userId, CancellationToken ct);
    Task<OutlookSettingsResponse> UpdateSettingsAsync(Guid userId, UpdateOutlookSettingsRequest request, CancellationToken ct);
    Task<OutlookAuthorizationSessionResponse> StartAuthorizationAsync(Guid userId, CancellationToken ct);
    Task<OutlookAuthorizationSessionResponse> GetAuthorizationAsync(Guid userId, Guid sessionId, CancellationToken ct);
    Task CancelAuthorizationAsync(Guid userId, Guid sessionId, CancellationToken ct);
    Task<OutlookCalendarDiscoveryResponse> DiscoverCalendarsAsync(Guid userId, CancellationToken ct);
    Task<IReadOnlyList<OutlookCalendarGroupResponse>> ListCalendarsAsync(Guid userId, CancellationToken ct);
    Task<IReadOnlyList<OutlookCalendarGroupResponse>> UpdateSelectionAsync(Guid userId, UpdateOutlookCalendarSelectionRequest request, CancellationToken ct);
    Task<OutlookSyncRunResponse> StartRunAsync(Guid userId, StartOutlookSyncRunRequest request, CancellationToken ct);
    Task<OutlookSyncRunResponse> GetRunAsync(Guid userId, Guid runId, CancellationToken ct);
    Task CancelRunAsync(Guid userId, Guid runId, CancellationToken ct);
    Task<OperationConfirmationDto> PreviewUpdateAsync(Guid userId, Guid eventId, OutlookEventChangeRequest request, CancellationToken ct);
    Task<OperationConfirmationDto> PreviewDeleteAsync(Guid userId, Guid eventId, CancellationToken ct);
    Task<EventEntity> CopyToPimAsync(Guid userId, Guid eventId, CopyOutlookEventRequest request, CancellationToken ct);
    Task<OutlookDiagnosticsResponse> RunDiagnosticsAsync(Guid userId, CancellationToken ct);
}
```

- [ ] **Step 7: 实现设置校验和 authority**

Add the class constructor and settings methods:

```csharp
public sealed class OutlookSyncFacade : IOutlookSyncFacade
{
    private readonly PimDbContext _db;
    private readonly OutlookAuthorizationSessionRunner _sessions;
    private readonly OutlookCalendarDiscoveryService _discovery;
    private readonly OutlookCalendarSyncCoordinator _coordinator;
    private readonly OutlookChangePreviewService _changes;
    private readonly OutlookDiagnosticsService _diagnostics;

    public OutlookSyncFacade(
        PimDbContext db,
        OutlookAuthorizationSessionRunner sessions,
        OutlookCalendarDiscoveryService discovery,
        OutlookCalendarSyncCoordinator coordinator,
        OutlookChangePreviewService changes,
        OutlookDiagnosticsService diagnostics)
    {
        _db = db;
        _sessions = sessions;
        _discovery = discovery;
        _coordinator = coordinator;
        _changes = changes;
        _diagnostics = diagnostics;
    }

    public async Task<OutlookSettingsResponse> UpdateSettingsAsync(
        Guid userId, UpdateOutlookSettingsRequest request, CancellationToken ct)
    {
        if (!Guid.TryParse(request.ClientId, out _))
            throw new ArgumentException("Client ID 必须是 Entra 应用概述页中的 UUID。", nameof(request));
        if (request.AccountScope is not ("common" or "organization"))
            throw new ArgumentException("账号范围必须是 common 或 organization。", nameof(request));
        var tenant = request.AccountScope == "common" ? "common" : request.TenantId;
        if (request.AccountScope == "organization" && !Guid.TryParse(tenant, out _))
            throw new ArgumentException("仅组织账号模式需要 Directory tenant ID。", nameof(request));

        var connection = await _db.Set<OutlookConnectionEntity>().SingleOrDefaultAsync(item => item.UserId == userId, ct)
            ?? new OutlookConnectionEntity { UserId = userId };
        if (_db.Entry(connection).State == EntityState.Detached) _db.Add(connection);
        connection.ClientId = request.ClientId;
        connection.TenantId = tenant!;
        connection.Authority = $"https://login.microsoftonline.com/{tenant}";
        connection.Scopes = string.Join(' ', OutlookAuthScopes.Required);
        connection.Status = connection.MsalCacheEncrypted is { Length: > 0 } ? "reauth-required" : "not-connected";
        connection.TokenHealth = connection.MsalCacheEncrypted is { Length: > 0 } ? "interaction-required" : "missing";
        connection.Version++;
        connection.UpdatedAt = DateTimeOffset.UtcNow;
        await _db.SaveChangesAsync(ct);
        return MapSettings(connection);
    }

    public async Task<OutlookSettingsResponse> GetSettingsAsync(Guid userId, CancellationToken ct)
    {
        var connection = await _db.Set<OutlookConnectionEntity>().AsNoTracking().SingleOrDefaultAsync(item => item.UserId == userId, ct);
        return connection is null
            ? new OutlookSettingsResponse("outlook", "", "common", "common", "https://login.microsoftonline.com/common",
                OutlookAuthScopes.Required, "not-connected", "missing", null, null, null, null, null)
            : MapSettings(connection);
    }

    private static OutlookSettingsResponse MapSettings(OutlookConnectionEntity connection) => new(
        "outlook",
        connection.ClientId ?? "",
        connection.TenantId == "common" ? "common" : "organization",
        connection.TenantId,
        connection.Authority,
        OutlookAuthScopes.Required,
        connection.Status,
        connection.TokenHealth,
        connection.AccountDisplayName,
        connection.AccountLoginHint,
        connection.LastSyncedAt,
        DateTimeOffset.UtcNow.AddMinutes(5),
        connection.LastError);
}
```

The next scheduled value is informational; the job dashboard remains the source for exact Hangfire execution timing.

- [ ] **Step 8: 实现 session 和用例委托**

Add to the facade:

```csharp
public async Task<OutlookAuthorizationSessionResponse> StartAuthorizationAsync(Guid userId, CancellationToken ct)
{
    var connection = await _db.Set<OutlookConnectionEntity>().SingleAsync(item => item.UserId == userId, ct);
    if (string.IsNullOrWhiteSpace(connection.ClientId)) throw new InvalidOperationException("请先保存 Microsoft Client ID。");
    var active = await _db.Set<OutlookAuthorizationSessionEntity>()
        .Where(item => item.UserId == userId && (item.Status == "starting" || item.Status == "waiting-for-user"))
        .ToListAsync(ct);
    foreach (var item in active) item.Status = "canceled";
    var session = new OutlookAuthorizationSessionEntity { UserId = userId, ConnectionId = connection.Id };
    _db.Add(session);
    await _db.SaveChangesAsync(ct);
    return MapSession(await _sessions.StartAsync(session.Id, ct));
}

public async Task<OutlookAuthorizationSessionResponse> GetAuthorizationAsync(Guid userId, Guid sessionId, CancellationToken ct)
    => MapSession(await _db.Set<OutlookAuthorizationSessionEntity>().AsNoTracking()
        .SingleAsync(item => item.Id == sessionId && item.UserId == userId, ct));

public Task CancelAuthorizationAsync(Guid userId, Guid sessionId, CancellationToken ct)
    => _sessions.CancelAsync(sessionId, userId, ct);

public async Task<OutlookCalendarDiscoveryResponse> DiscoverCalendarsAsync(Guid userId, CancellationToken ct)
{
    var connection = await ConnectionAsync(userId, ct);
    return await _discovery.DiscoverAsync(userId, connection.Id, ct);
}

public async Task<IReadOnlyList<OutlookCalendarGroupResponse>> ListCalendarsAsync(Guid userId, CancellationToken ct)
{
    var connection = await ConnectionAsync(userId, ct);
    return await _discovery.ListAsync(userId, connection.Id, ct);
}

public async Task<IReadOnlyList<OutlookCalendarGroupResponse>> UpdateSelectionAsync(
    Guid userId, UpdateOutlookCalendarSelectionRequest request, CancellationToken ct)
{
    var connection = await ConnectionAsync(userId, ct);
    return await _discovery.UpdateSelectionAsync(userId, connection.Id, request.SelectedBindingIds, ct);
}

public async Task<OutlookSyncRunResponse> StartRunAsync(Guid userId, StartOutlookSyncRunRequest request, CancellationToken ct)
{
    var run = await _coordinator.CreateRunAsync(userId, request, ct);
    BackgroundJob.Enqueue<OutlookSyncJobs>(job => job.RunSyncAsync(run.Id));
    return run;
}

public Task<OutlookSyncRunResponse> GetRunAsync(Guid userId, Guid runId, CancellationToken ct)
    => _coordinator.GetRunAsync(userId, runId, ct);
public Task CancelRunAsync(Guid userId, Guid runId, CancellationToken ct)
    => _coordinator.CancelRunAsync(userId, runId, ct);
public Task<OperationConfirmationDto> PreviewUpdateAsync(Guid userId, Guid eventId, OutlookEventChangeRequest request, CancellationToken ct)
    => _changes.PreviewUpdateAsync(userId, eventId, request, ct);
public Task<OperationConfirmationDto> PreviewDeleteAsync(Guid userId, Guid eventId, CancellationToken ct)
    => _changes.PreviewDeleteAsync(userId, eventId, ct);
public Task<EventEntity> CopyToPimAsync(Guid userId, Guid eventId, CopyOutlookEventRequest request, CancellationToken ct)
    => _changes.CopyToPimAsync(userId, eventId, request.TargetCalendarId, ct);
public Task<OutlookDiagnosticsResponse> RunDiagnosticsAsync(Guid userId, CancellationToken ct)
    => _diagnostics.RunAsync(userId, ct);

private Task<OutlookConnectionEntity> ConnectionAsync(Guid userId, CancellationToken ct)
    => _db.Set<OutlookConnectionEntity>().SingleAsync(item => item.UserId == userId, ct);

private static OutlookAuthorizationSessionResponse MapSession(OutlookAuthorizationSessionEntity session) => new(
    session.Id, session.Status, session.VerificationUri, session.UserCode, session.ExpiresAt,
    session.AccountDisplayName, session.AccountLoginHint, session.ErrorCode, session.ErrorMessage,
    session.ErrorCode switch
    {
        "invalid-client-id" => "返回 Entra 应用概述页重新复制 Client ID。",
        "public-client-disabled" => "在 Entra 身份验证的高级设置中启用公共客户端流。",
        "admin-consent-required" => "联系租户管理员批准委托权限。",
        "service-restarted" => "重新请求设备代码。",
        null => null,
        _ => "检查网络和 Entra 配置后重试。"
    });
```

In integration tests, replace the static Hangfire call by registering a test Hangfire storage or by using a fake facade that does not execute this production method. Unit coordinator tests cover run persistence.

- [ ] **Step 9: 映射专用 Outlook endpoints**

Create `src/modules/Pim.Module.Calendar/OutlookEndpoints.cs`:

```csharp
using Microsoft.AspNetCore.Mvc;
using Pim.Core.Common;
using Pim.Infrastructure.Auth;
using Pim.Module.Calendar.DTOs;
using Pim.Module.Calendar.Services;

namespace Pim.Module.Calendar;

public static class OutlookEndpoints
{
    public static RouteGroupBuilder MapOutlookEndpoints(this RouteGroupBuilder group)
    {
        group.MapGet("/outlook/settings", Call((facade, user, ct) => facade.GetSettingsAsync(user, ct)));
        group.MapPut("/outlook/settings", async ([FromBody] UpdateOutlookSettingsRequest request, IOutlookSyncFacade facade, ICurrentUserService current, CancellationToken ct)
            => Results.Ok(ApiResponse<OutlookSettingsResponse>.Ok(await facade.UpdateSettingsAsync(User(current), request, ct))));
        group.MapPost("/outlook/auth-sessions", Call((facade, user, ct) => facade.StartAuthorizationAsync(user, ct)));
        group.MapGet("/outlook/auth-sessions/{id:guid}", async (Guid id, IOutlookSyncFacade facade, ICurrentUserService current, CancellationToken ct)
            => Results.Ok(ApiResponse<OutlookAuthorizationSessionResponse>.Ok(await facade.GetAuthorizationAsync(User(current), id, ct))));
        group.MapDelete("/outlook/auth-sessions/{id:guid}", async (Guid id, IOutlookSyncFacade facade, ICurrentUserService current, CancellationToken ct) =>
        { await facade.CancelAuthorizationAsync(User(current), id, ct); return Results.NoContent(); });
        group.MapPost("/outlook/calendars/discover", Call((facade, user, ct) => facade.DiscoverCalendarsAsync(user, ct)));
        group.MapGet("/outlook/calendars", Call((facade, user, ct) => facade.ListCalendarsAsync(user, ct)));
        group.MapPut("/outlook/calendars/selection", async ([FromBody] UpdateOutlookCalendarSelectionRequest request, IOutlookSyncFacade facade, ICurrentUserService current, CancellationToken ct)
            => Results.Ok(ApiResponse<IReadOnlyList<OutlookCalendarGroupResponse>>.Ok(await facade.UpdateSelectionAsync(User(current), request, ct))));
        group.MapPost("/outlook/sync-runs", async ([FromBody] StartOutlookSyncRunRequest request, IOutlookSyncFacade facade, ICurrentUserService current, CancellationToken ct)
            => Results.Accepted(value: ApiResponse<OutlookSyncRunResponse>.Ok(await facade.StartRunAsync(User(current), request, ct))));
        group.MapGet("/outlook/sync-runs/{id:guid}", async (Guid id, IOutlookSyncFacade facade, ICurrentUserService current, CancellationToken ct)
            => Results.Ok(ApiResponse<OutlookSyncRunResponse>.Ok(await facade.GetRunAsync(User(current), id, ct))));
        group.MapDelete("/outlook/sync-runs/{id:guid}", async (Guid id, IOutlookSyncFacade facade, ICurrentUserService current, CancellationToken ct) =>
        { await facade.CancelRunAsync(User(current), id, ct); return Results.NoContent(); });
        group.MapPost("/outlook/events/{id:guid}/change-preview", async (Guid id, [FromBody] OutlookEventChangeRequest request, IOutlookSyncFacade facade, ICurrentUserService current, CancellationToken ct)
            => Results.Ok(ApiResponse<OperationConfirmationDto>.Ok(await facade.PreviewUpdateAsync(User(current), id, request, ct))));
        group.MapPost("/outlook/events/{id:guid}/delete-preview", async (Guid id, IOutlookSyncFacade facade, ICurrentUserService current, CancellationToken ct)
            => Results.Ok(ApiResponse<OperationConfirmationDto>.Ok(await facade.PreviewDeleteAsync(User(current), id, ct))));
        group.MapPost("/outlook/events/{id:guid}/copy-to-pim", async (Guid id, [FromBody] CopyOutlookEventRequest request, IOutlookSyncFacade facade, ICurrentUserService current, CancellationToken ct)
            => Results.Ok(ApiResponse<object>.Ok(await facade.CopyToPimAsync(User(current), id, request, ct))));
        group.MapPost("/outlook/diagnostics", Call((facade, user, ct) => facade.RunDiagnosticsAsync(user, ct)));
        return group;
    }

    private static Delegate Call<T>(Func<IOutlookSyncFacade, Guid, CancellationToken, Task<T>> action)
        => async (IOutlookSyncFacade facade, ICurrentUserService current, CancellationToken ct)
            => Results.Ok(ApiResponse<T>.Ok(await action(facade, User(current), ct)));

    private static Guid User(ICurrentUserService current)
        => current.UserId ?? throw new UnauthorizedAccessException("Current PIM user is required.");
}
```

Add `using Pim.Core.Operations;` for `OperationConfirmationDto`.

- [ ] **Step 10: 替换旧 Outlook endpoint block 并注册服务**

In `CalendarModule.MapEndpoints`, replace the old settings/device-code/sync block and inline Outlook event management endpoints with:

```csharp
group.MapOutlookEndpoints();
```

In `RegisterServices`, add all new scoped services and facade:

```csharp
services.AddScoped<OutlookTokenCacheStore>();
services.AddScoped<IMsalPublicClientAdapter, MsalPublicClientAdapter>();
services.AddScoped<IOutlookAccessTokenProvider, MsalOutlookAuthCoordinator>();
services.AddSingleton<OutlookAuthorizationSessionRunner>();
services.AddScoped<IGraphCalendarClient, GraphCalendarClient>();
services.AddScoped<OutlookEventMapper>();
services.AddScoped<OutlookCalendarDiscoveryService>();
services.AddScoped<OutlookEventProjectionService>();
services.AddScoped<OutlookChangePreviewService>();
services.AddScoped<OutlookDiagnosticsService>();
services.AddScoped<IOutlookSyncFacade, OutlookSyncFacade>();
```

Remove registrations for `OutlookSyncService`, `OutlookTokenService`, and `IMicrosoftGraphClient` after all compilation references have moved.

- [ ] **Step 11: 运行 API、认证和路径测试**

Run:

```powershell
dotnet test tests/Pim.UnitTests/Pim.UnitTests.csproj --filter "FullyQualifiedName~OutlookApiContractTests|FullyQualifiedName~OutlookAuthorizationSessionTests|FullyQualifiedName~CalendarEndpointPathTests"
```

Expected: PASS；session JSON 没有 OAuth device code，所有 use-case endpoints 传入当前 JWT 用户。

- [ ] **Step 12: 提交 API vertical slice**

```powershell
git add src/modules/Pim.Module.Calendar tests/Pim.UnitTests/Pim.UnitTests.csproj tests/Pim.UnitTests/Calendar/OutlookApiContractTests.cs
git commit -m "feat: expose microsoft calendar sync api"
```

Expected: 后端 API 可由 fake MSAL/Graph 完整自动化测试，不需要真实 Microsoft 凭据。

## Task 17: 迁移旧 token/delta/event 并删除手工 OAuth 路径

**Files:**
- Create: `src/modules/Pim.Module.Calendar/Services/OutlookLegacyRebindingService.cs`
- Create: `tests/Pim.UnitTests/Calendar/OutlookLegacyMigrationTests.cs`
- Modify: `src/modules/Pim.Module.Calendar/Services/OutlookEventProjectionService.cs`
- Modify: `src/modules/Pim.Module.Calendar/Services/OutlookAuthorizationSessionRunner.cs`
- Modify: `src/Pim.Infrastructure/Data/Migrations/20260710000000_MicrosoftCalendarSync.cs`
- Modify: `tests/Pim.UnitTests/Calendar/OutlookCalendarSyncStrategyTests.cs`
- Modify: `tests/Pim.UnitTests/Calendar/OutlookDeepSyncTests.cs`
- Delete: `src/modules/Pim.Module.Calendar/Services/MicrosoftGraphDeviceCodeClient.cs`
- Delete: `src/modules/Pim.Module.Calendar/Services/OutlookTokenService.cs`
- Delete: `src/modules/Pim.Module.Calendar/Services/OutlookGraphModels.cs`
- Delete: `src/modules/Pim.Module.Calendar/Services/OutlookSyncService.cs`
- Delete: `tests/Pim.UnitTests/Calendar/OutlookGraphDeviceCodeFlowTests.cs`
- Delete: `tests/Pim.UnitTests/Calendar/OutlookGraphDeltaSyncTests.cs`
- Delete: `tests/Pim.UnitTests/Calendar/OutlookGraphSyncFoundationTests.cs`
- Delete: `tests/Pim.UnitTests/Calendar/OutlookGraphTestDoubles.cs`
- Delete: `tests/Pim.UnitTests/Calendar/OutlookGraphWritebackTests.cs`

- [ ] **Step 1: 写可靠重绑和禁止相似度匹配测试**

Create `tests/Pim.UnitTests/Calendar/OutlookLegacyMigrationTests.cs`:

```csharp
using Microsoft.EntityFrameworkCore;
using Pim.Infrastructure.Data;
using Pim.Module.Calendar.Entities;
using Pim.Module.Calendar.Services;
using Xunit;

namespace Pim.UnitTests.Calendar;

public sealed class OutlookLegacyMigrationTests
{
[Fact]
public async Task ExactIcalUid_RebindsLegacyEventWithoutCreatingDuplicate()
{
    await using var db = CreateDb();
    var seed = Seed(db);
    var legacy = Legacy(seed.Calendar, "ical-exact", "old-graph-id", "课程");
    db.Add(legacy);
    await db.SaveChangesAsync();
    var service = new OutlookLegacyRebindingService(db);

    var rebound = await service.TryRebindAsync(
        seed.UserId, seed.Binding, Remote("new-immutable-id", "ical-exact", "课程"), CancellationToken.None);

    Assert.Equal(legacy.Id, rebound?.Id);
    Assert.Equal(seed.Binding.Id, rebound?.OutlookCalendarBindingId);
    Assert.Equal("new-immutable-id", rebound?.OutlookEventId);
    Assert.Equal(1, await db.Set<EventEntity>().CountAsync());
}

[Fact]
public async Task SimilarTitleAndTime_WithoutReliableIdentityStaysLegacyUnbound()
{
    await using var db = CreateDb();
    var seed = Seed(db);
    var legacy = Legacy(seed.Calendar, null, "old-graph-id", "同名课程");
    db.Add(legacy);
    await db.SaveChangesAsync();
    var service = new OutlookLegacyRebindingService(db);

    var rebound = await service.TryRebindAsync(
        seed.UserId, seed.Binding, Remote("different-id", null, "同名课程"), CancellationToken.None);

    Assert.Null(rebound);
    Assert.Equal("legacy-unbound", (await db.Set<EventEntity>().SingleAsync()).OutlookSyncState);
}

private sealed record LegacySeed(
    Guid UserId,
    CalendarEntity Calendar,
    OutlookCalendarBindingEntity Binding);

private static LegacySeed Seed(PimDbContext db)
{
    var userId = Guid.NewGuid();
    var connection = new OutlookConnectionEntity
    {
        UserId = userId,
        ClientId = Guid.NewGuid().ToString(),
        Status = "connected",
        TokenHealth = "healthy"
    };
    var calendar = new CalendarEntity
    {
        UserId = userId,
        Name = "Outlook 旧日历",
        Source = "outlook"
    };
    var binding = new OutlookCalendarBindingEntity
    {
        ConnectionId = connection.Id,
        PimCalendarId = calendar.Id,
        GraphCalendarId = "graph-calendar",
        Name = "Outlook 旧日历",
        CanEdit = true,
        IsSelected = true
    };
    db.AddRange(connection, calendar, binding);
    db.SaveChanges();
    return new LegacySeed(userId, calendar, binding);
}

private static EventEntity Legacy(
    CalendarEntity calendar,
    string? iCalUid,
    string graphEventId,
    string title) => new()
{
    Calendar = calendar,
    CalendarId = calendar.Id,
    Uid = iCalUid ?? $"legacy-{Guid.NewGuid():N}",
    SourceUid = iCalUid,
    Title = title,
    DtStart = new DateTimeOffset(2026, 7, 10, 1, 0, 0, TimeSpan.Zero),
    DtEnd = new DateTimeOffset(2026, 7, 10, 2, 0, 0, TimeSpan.Zero),
    Source = "outlook",
    OutlookEventId = graphEventId,
    OutlookChangeKey = "legacy-change",
    OutlookEtag = "legacy-etag"
};

private static GraphEventDto Remote(
    string id,
    string? iCalUid,
    string title) => new(
        id,
        title,
        null,
        new GraphDateTimeTimeZoneDto("2026-07-10T01:00:00Z", "UTC"),
        new GraphDateTimeTimeZoneDto("2026-07-10T02:00:00Z", "UTC"),
        false,
        iCalUid,
        null,
        "singleInstance",
        "remote-change",
        "remote-etag",
        DateTimeOffset.UtcNow,
        null,
        null,
        null);

private static PimDbContext CreateDb()
{
    PimDbContext.RegisterModuleAssembly(typeof(OutlookConnectionEntity).Assembly);
    return new PimDbContext(new DbContextOptionsBuilder<PimDbContext>()
        .UseInMemoryDatabase($"outlook-legacy-{Guid.NewGuid()}")
        .Options);
}
}
```

Both remote and legacy fixtures deliberately use the same time. The second test therefore proves that title/time similarity never participates in rebinding.

- [ ] **Step 2: 运行测试并确认 rebinding service 不存在**

Run:

```powershell
dotnet test tests/Pim.UnitTests/Pim.UnitTests.csproj --filter FullyQualifiedName~OutlookLegacyMigrationTests
```

Expected: FAIL，编译错误指向 `OutlookLegacyRebindingService`。

- [ ] **Step 3: 实现只按可靠外部身份重绑**

Create `src/modules/Pim.Module.Calendar/Services/OutlookLegacyRebindingService.cs`:

```csharp
using Microsoft.EntityFrameworkCore;
using Pim.Infrastructure.Data;
using Pim.Module.Calendar.Entities;

namespace Pim.Module.Calendar.Services;

public sealed class OutlookLegacyRebindingService
{
    private readonly PimDbContext _db;

    public OutlookLegacyRebindingService(PimDbContext db) => _db = db;

    public async Task<EventEntity?> TryRebindAsync(
        Guid userId,
        OutlookCalendarBindingEntity binding,
        GraphEventDto remote,
        CancellationToken ct)
    {
        var candidates = await _db.Set<EventEntity>()
            .Include(item => item.Calendar)
            .Where(item => item.Calendar.UserId == userId
                && item.OutlookCalendarBindingId == null
                && item.OutlookEventId != null
                && item.Source.StartsWith("outlook"))
            .ToListAsync(ct);
        EventEntity? match = null;
        if (!string.IsNullOrWhiteSpace(remote.ICalUId))
        {
            var byIcal = candidates.Where(item => item.SourceUid == remote.ICalUId || item.Uid == remote.ICalUId).ToList();
            if (byIcal.Count == 1) match = byIcal[0];
        }
        if (match is null)
        {
            var byExactGraphId = candidates.Where(item => item.OutlookEventId == remote.Id).ToList();
            if (byExactGraphId.Count == 1) match = byExactGraphId[0];
        }

        foreach (var candidate in candidates.Where(item => item.Id != match?.Id))
            candidate.OutlookSyncState = "legacy-unbound";
        if (match is null)
        {
            await _db.SaveChangesAsync(ct);
            return null;
        }

        match.CalendarId = binding.PimCalendarId;
        match.OutlookConnectionId = binding.ConnectionId;
        match.OutlookCalendarBindingId = binding.Id;
        match.OutlookEventId = remote.Id;
        match.SourceUid = remote.ICalUId ?? match.SourceUid;
        match.OutlookChangeKey = remote.ChangeKey;
        match.OutlookEtag = remote.ETag;
        match.OutlookSyncState = "active";
        match.Source = "outlook";
        match.UpdatedAt = DateTimeOffset.UtcNow;
        await _db.SaveChangesAsync(ct);
        return match;
    }

    public async Task<int> MarkLegacyConnectionsForReauthorizationAsync(CancellationToken ct)
    {
        var legacy = await _db.Set<OutlookConnectionEntity>()
            .Where(item => item.MsalCacheEncrypted == null
                && (item.AccessTokenEncrypted.Length > 0 || item.RefreshTokenEncrypted != null))
            .ToListAsync(ct);
        foreach (var connection in legacy)
        {
            connection.Status = "reauth-required";
            connection.TokenHealth = "interaction-required";
            connection.LastError = "Microsoft 同步已升级为 MSAL，请重新授权。";
            connection.Version++;
            connection.UpdatedAt = DateTimeOffset.UtcNow;
        }
        await _db.SaveChangesAsync(ct);
        return legacy.Count;
    }
}
```

- [ ] **Step 4: 在创建新投影前尝试重绑**

Add the dependency and constructor assignment to `OutlookEventProjectionService`:

```csharp
private readonly OutlookLegacyRebindingService _legacy;

public OutlookEventProjectionService(
    PimDbContext db,
    IGraphCalendarClient graph,
    OutlookEventMapper mapper,
    IOperationConfirmationService confirmations,
    OutlookLegacyRebindingService legacy)
{
    _db = db;
    _graph = graph;
    _mapper = mapper;
    _confirmations = confirmations;
    _legacy = legacy;
}
```

Replace the `existing is null` branch with:

```csharp
if (existing is null)
{
    existing = await _legacy.TryRebindAsync(userId, binding, remote, ct);
    if (existing is null)
    {
        var created = _mapper.MapNew(remote, binding, generation);
        _db.Add(created);
        await _db.SaveChangesAsync(ct);
        return new OutlookProjectionResult("created", created.Id);
    }
}
```

The rest of `UpsertAsync` then compares the rebound local facts with the remote snapshot and creates a pull confirmation when necessary.

Update both test helpers created in Tasks 13 and 14 so their constructors match:

```csharp
var projection = new OutlookEventProjectionService(
    db,
    graph,
    new OutlookEventMapper(),
    new OperationConfirmationService(db),
    new OutlookLegacyRebindingService(db));
```

- [ ] **Step 5: 标记旧 connection 与 event 的迁移状态**

Add to `MicrosoftCalendarSync.Up` after new columns exist:

```csharp
migrationBuilder.Sql("""
    UPDATE outlook_connections
    SET status = 'reauth-required',
        token_health = 'interaction-required',
        last_error = 'Microsoft sync upgraded to MSAL; authorization is required.'
    WHERE msal_cache_encrypted IS NULL
      AND (octet_length(access_token_encrypted) > 0 OR refresh_token_encrypted IS NOT NULL);

    UPDATE events
    SET outlook_sync_state = 'legacy-unbound'
    WHERE outlook_event_id IS NOT NULL
      AND outlook_calendar_binding_id IS NULL;
    """);
```

Do not attempt to deserialize or transform the old refresh token into MSAL's private cache format. Keep legacy columns for one rollback-capable release cycle.

- [ ] **Step 6: 在 module 初始化时运行幂等 legacy 标记**

Resolve `OutlookLegacyRebindingService` in `CalendarModule.InitializeAsync` and call:

```csharp
await scope.ServiceProvider.GetRequiredService<OutlookLegacyRebindingService>()
    .MarkLegacyConnectionsForReauthorizationAsync(CancellationToken.None);
```

Register it scoped in `RegisterServices`.

- [ ] **Step 7: 删除手工 OAuth、connection delta 和旧测试路径**

Use `apply_patch` to delete the four legacy service files and the five tests listed in this task. Before deletion run:

```powershell
rg -n "MicrosoftGraphDeviceCodeClient|OutlookTokenService|IMicrosoftGraphClient|OutlookSyncService|AccessTokenEncrypted|RefreshTokenEncrypted|\.DeltaLink" src/modules/Pim.Module.Calendar tests/Pim.UnitTests/Calendar
```

Expected before deletion: matches are confined to the legacy files/tests, retained entity columns, migration, new authorization cleanup, and explicit migration tests. Expected after deletion: production code only writes legacy token fields to clear them and never reads them for Graph access; connection-level `DeltaLink` is not read by sync code.

- [ ] **Step 8: 运行迁移和全部 Outlook 后端测试**

Run:

```powershell
dotnet test tests/Pim.UnitTests/Pim.UnitTests.csproj --filter FullyQualifiedName~Outlook
```

Expected: PASS；没有旧 fake client 编译引用，没有相似标题/时间自动合并。

- [ ] **Step 9: 提交定向迁移和旧路径删除**

```powershell
git add src/modules/Pim.Module.Calendar src/Pim.Infrastructure/Data/Migrations tests/Pim.UnitTests/Calendar
git commit -m "refactor: retire legacy outlook token sync path"
```

Expected: 新代码只通过 `IOutlookAccessTokenProvider` 获取 token，只通过逐日历 binding 保存 cursor。

## Task 18: 对齐 Web 类型、API 路径和上海时间工具

**Files:**
- Create: `src/client-web/src/utils/calendarTime.ts`
- Create: `tests/client-web/outlookSyncApiPath.test.ts`
- Create: `tests/client-web/outlookSyncTypes.test.ts`
- Create: `tests/client-web/tsconfig.outlook-sync.json`
- Modify: `src/client-web/src/types/index.ts`
- Modify: `src/client-web/src/api/calendar.ts`

- [ ] **Step 1: 写 API path 失败测试**

Create `tests/client-web/outlookSyncApiPath.test.ts`:

```ts
import assert from 'node:assert/strict';
import { calendarApiPaths } from '../../src/client-web/src/api/calendar';

assert.equal(calendarApiPaths.outlookSettings(), '/calendar/outlook/settings');
assert.equal(calendarApiPaths.outlookAuthSessions(), '/calendar/outlook/auth-sessions');
assert.equal(calendarApiPaths.outlookAuthSession('a/b'), '/calendar/outlook/auth-sessions/a%2Fb');
assert.equal(calendarApiPaths.outlookCalendars(), '/calendar/outlook/calendars');
assert.equal(calendarApiPaths.outlookCalendarDiscovery(), '/calendar/outlook/calendars/discover');
assert.equal(calendarApiPaths.outlookCalendarSelection(), '/calendar/outlook/calendars/selection');
assert.equal(calendarApiPaths.outlookSyncRuns(), '/calendar/outlook/sync-runs');
assert.equal(calendarApiPaths.outlookSyncRun('run/1'), '/calendar/outlook/sync-runs/run%2F1');
assert.equal(calendarApiPaths.outlookChangePreview('event/1'), '/calendar/outlook/events/event%2F1/change-preview');
assert.equal(calendarApiPaths.outlookDeletePreview('event/1'), '/calendar/outlook/events/event%2F1/delete-preview');
assert.equal(calendarApiPaths.outlookCopyToPim('event/1'), '/calendar/outlook/events/event%2F1/copy-to-pim');
assert.equal(calendarApiPaths.outlookDiagnostics(), '/calendar/outlook/diagnostics');
```

- [ ] **Step 2: 写类型和时间转换失败测试**

Create `tests/client-web/outlookSyncTypes.test.ts`:

```ts
import assert from 'node:assert/strict';
import type {
  OutlookAuthorizationSession,
  OutlookCalendarGroup,
  OutlookSyncRun,
  UpdateOutlookSettingsRequest,
} from '../../src/client-web/src/types';
import { fromShanghaiInputToUtc, toShanghaiInputValue } from '../../src/client-web/src/utils/calendarTime';

const settings = {
  clientId: '11111111-1111-1111-1111-111111111111',
  accountScope: 'common',
  tenantId: null,
} satisfies UpdateOutlookSettingsRequest;
const session = { id: 's', status: 'waiting-for-user', userCode: 'ABCD-EFGH' } as OutlookAuthorizationSession;
const groups: OutlookCalendarGroup[] = [];
const run = { id: 'r', mode: 'range-instances', status: 'running', calendars: [] } as OutlookSyncRun;

assert.equal(settings.accountScope, 'common');
assert.equal(session.status, 'waiting-for-user');
assert.equal(groups.length, 0);
assert.equal(run.mode, 'range-instances');
assert.equal(fromShanghaiInputToUtc('2026-07-10T09:00'), '2026-07-10T01:00:00.000Z');
assert.equal(toShanghaiInputValue('2026-07-10T01:00:00.000Z'), '2026-07-10T09:00');
```

Create `tests/client-web/tsconfig.outlook-sync.json`:

```json
{
  "extends": "../../src/client-web/tsconfig.json",
  "compilerOptions": {
    "noEmit": true,
    "types": ["node"],
    "typeRoots": ["../../src/client-web/node_modules/@types"]
  },
  "include": ["./outlookSyncTypes.test.ts"]
}
```

- [ ] **Step 3: 运行测试并确认新 path/type 不存在**

Run:

```powershell
npm --prefix src/client-web exec tsx -- tests/client-web/outlookSyncApiPath.test.ts
npm --prefix src/client-web exec tsc -- -p tests/client-web/tsconfig.outlook-sync.json
```

Expected: FAIL，旧 device-code/sync 路径和类型仍存在，新类型/工具缺失。

- [ ] **Step 4: 替换 Outlook Web 类型**

Replace the old Outlook settings/device-code/batch interfaces in `src/client-web/src/types/index.ts` with:

```ts
export type OutlookAccountScope = 'common' | 'organization';
export type OutlookAuthorizationStatus =
  | 'starting' | 'waiting-for-user' | 'connected' | 'expired' | 'canceled' | 'failed';
export type OutlookSyncMode = 'incremental' | 'rolling-baseline' | 'full-resources' | 'range-instances';

export interface OutlookSettingsResponse {
  provider: 'outlook';
  clientId: string;
  accountScope: OutlookAccountScope;
  tenantId: string;
  authority: string;
  scopes: string[];
  status: string;
  tokenHealth: string;
  accountDisplayName?: string | null;
  accountLoginHint?: string | null;
  lastSyncedAt?: string | null;
  nextScheduledSyncAt?: string | null;
  lastError?: string | null;
}

export interface UpdateOutlookSettingsRequest {
  clientId: string;
  accountScope: OutlookAccountScope;
  tenantId?: string | null;
}

export interface OutlookAuthorizationSession {
  id: string;
  status: OutlookAuthorizationStatus;
  verificationUri?: string | null;
  userCode?: string | null;
  expiresAt?: string | null;
  accountDisplayName?: string | null;
  accountLoginHint?: string | null;
  errorCode?: string | null;
  errorMessage?: string | null;
  recoveryAction?: string | null;
}

export interface OutlookCalendarBinding {
  id: string;
  pimCalendarId: string;
  graphCalendarId: string;
  groupId?: string | null;
  groupName: string;
  name: string;
  color?: string | null;
  isDefault: boolean;
  canEdit: boolean;
  canViewPrivateItems: boolean;
  isSelected: boolean;
  remoteState: string;
  syncStrategy: string;
  lastSyncedAt?: string | null;
  lastErrorCode?: string | null;
  lastErrorMessage?: string | null;
}

export interface OutlookCalendarGroup {
  id?: string | null;
  name: string;
  calendars: OutlookCalendarBinding[];
}

export interface OutlookCalendarRunProgress {
  bindingId: string;
  calendarName: string;
  status: string;
  readCount: number;
  createdCount: number;
  confirmationCount: number;
  failureCount: number;
  errorCode?: string | null;
  errorMessage?: string | null;
}

export interface OutlookSyncRun {
  id: string;
  mode: OutlookSyncMode;
  status: string;
  cancelRequested: boolean;
  requestedStart?: string | null;
  requestedEnd?: string | null;
  calendars: OutlookCalendarRunProgress[];
  readCount: number;
  createdCount: number;
  confirmationCount: number;
  failureCount: number;
  startedAt: string;
  finishedAt?: string | null;
  errorSummary?: string | null;
}

export interface OutlookDiagnosticCheck {
  code: string;
  label: string;
  status: string;
  message: string;
  technicalCode?: string | null;
}

export interface OutlookDiagnostics {
  status: string;
  checkedAt: string;
  checks: OutlookDiagnosticCheck[];
}
```

Extend `EventResponse`:

```ts
outlookCalendarBindingId?: string | null;
outlookCanEdit?: boolean | null;
allDayStartDate?: string | null;
allDayEndDateExclusive?: string | null;
outlookSyncState?: string | null;
```

- [ ] **Step 5: 替换 calendar API paths**

Replace the old Outlook entries in `calendarApiPaths`:

```ts
outlookSettings: () => '/calendar/outlook/settings',
outlookAuthSessions: () => '/calendar/outlook/auth-sessions',
outlookAuthSession: (id: string) => `/calendar/outlook/auth-sessions/${encodeURIComponent(id)}`,
outlookCalendars: () => '/calendar/outlook/calendars',
outlookCalendarDiscovery: () => '/calendar/outlook/calendars/discover',
outlookCalendarSelection: () => '/calendar/outlook/calendars/selection',
outlookSyncRuns: () => '/calendar/outlook/sync-runs',
outlookSyncRun: (id: string) => `/calendar/outlook/sync-runs/${encodeURIComponent(id)}`,
outlookChangePreview: (id: string) => `/calendar/outlook/events/${encodeURIComponent(id)}/change-preview`,
outlookDeletePreview: (id: string) => `/calendar/outlook/events/${encodeURIComponent(id)}/delete-preview`,
outlookCopyToPim: (id: string) => `/calendar/outlook/events/${encodeURIComponent(id)}/copy-to-pim`,
outlookDiagnostics: () => '/calendar/outlook/diagnostics',
```

- [ ] **Step 6: 实现所有用例级 API functions**

Replace old device-code/sync functions with:

```ts
export async function startOutlookAuthorization() {
  const response = await apiPost<ApiResponse<OutlookAuthorizationSession>>(calendarApiPaths.outlookAuthSessions(), {});
  return response.data;
}
export async function getOutlookAuthorization(id: string) {
  const response = await apiGet<ApiResponse<OutlookAuthorizationSession>>(calendarApiPaths.outlookAuthSession(id));
  return response.data;
}
export async function cancelOutlookAuthorization(id: string) {
  await apiDelete<void>(calendarApiPaths.outlookAuthSession(id));
}
export async function discoverOutlookCalendars() {
  const response = await apiPost<ApiResponse<{ groups: OutlookCalendarGroup[] }>>(calendarApiPaths.outlookCalendarDiscovery(), {});
  return response.data;
}
export async function getOutlookCalendars() {
  const response = await apiGet<ApiResponse<OutlookCalendarGroup[]>>(calendarApiPaths.outlookCalendars());
  return response.data;
}
export async function updateOutlookCalendarSelection(selectedBindingIds: string[]) {
  const response = await apiPut<ApiResponse<OutlookCalendarGroup[]>>(
    calendarApiPaths.outlookCalendarSelection(), { selectedBindingIds });
  return response.data;
}
export async function startOutlookSyncRun(request: {
  mode: OutlookSyncMode;
  calendarBindingIds?: string[] | null;
  start?: string | null;
  end?: string | null;
}) {
  const response = await apiPost<ApiResponse<OutlookSyncRun>>(calendarApiPaths.outlookSyncRuns(), request);
  return response.data;
}
export async function getOutlookSyncRun(id: string) {
  const response = await apiGet<ApiResponse<OutlookSyncRun>>(calendarApiPaths.outlookSyncRun(id));
  return response.data;
}
export async function cancelOutlookSyncRun(id: string) {
  await apiDelete<void>(calendarApiPaths.outlookSyncRun(id));
}
export async function runOutlookDiagnostics() {
  const response = await apiPost<ApiResponse<OutlookDiagnostics>>(calendarApiPaths.outlookDiagnostics(), {});
  return response.data;
}
export async function previewOutlookEventChange(id: string, request: OutlookEventChangeRequest) {
  const response = await apiPost<ApiResponse<OperationConfirmation>>(calendarApiPaths.outlookChangePreview(id), request);
  return response.data;
}
export async function previewOutlookEventDelete(id: string) {
  const response = await apiPost<ApiResponse<OperationConfirmation>>(calendarApiPaths.outlookDeletePreview(id), {});
  return response.data;
}
export async function copyOutlookEventToPim(id: string, targetCalendarId?: string) {
  const response = await apiPost<ApiResponse<EventResponse>>(calendarApiPaths.outlookCopyToPim(id), {
    targetCalendarId: targetCalendarId ?? null,
  });
  return response.data;
}
```

Add the imported types and define:

```ts
export interface OutlookEventChangeRequest {
  title: string;
  description?: string | null;
  location?: string | null;
  dtStart?: string | null;
  dtEnd?: string | null;
  isAllDay: boolean;
  allDayStartDate?: string | null;
  allDayEndDateExclusive?: string | null;
}
```

- [ ] **Step 7: 实现上海时间和全天日期工具**

Create `src/client-web/src/utils/calendarTime.ts`:

```ts
export const DISPLAY_TIME_ZONE = 'Asia/Shanghai';

const shanghaiInputFormatter = new Intl.DateTimeFormat('sv-SE', {
  timeZone: DISPLAY_TIME_ZONE,
  year: 'numeric', month: '2-digit', day: '2-digit',
  hour: '2-digit', minute: '2-digit', hourCycle: 'h23',
});

export function toShanghaiInputValue(utcIso: string): string {
  return shanghaiInputFormatter.format(new Date(utcIso)).replace(' ', 'T');
}

export function fromShanghaiInputToUtc(value: string): string {
  if (!/^\d{4}-\d{2}-\d{2}T\d{2}:\d{2}$/.test(value)) {
    throw new Error('上海时间必须使用 YYYY-MM-DDTHH:mm 格式');
  }
  return new Date(`${value}:00+08:00`).toISOString();
}

export function allDayDate(value?: string | null): string {
  return value?.slice(0, 10) ?? '';
}

export function formatShanghai(value?: string | null): string {
  if (!value) return '暂无';
  return new Intl.DateTimeFormat('zh-CN', {
    timeZone: DISPLAY_TIME_ZONE,
    year: 'numeric', month: '2-digit', day: '2-digit',
    hour: '2-digit', minute: '2-digit', hourCycle: 'h23',
  }).format(new Date(value));
}
```

- [ ] **Step 8: 运行 path/type/time tests**

Run:

```powershell
npm --prefix src/client-web exec tsx -- tests/client-web/outlookSyncApiPath.test.ts
npm --prefix src/client-web exec tsc -- -p tests/client-web/tsconfig.outlook-sync.json
npm --prefix src/client-web exec tsx -- tests/client-web/outlookSyncTypes.test.ts
```

Expected: PASS；路径全部带 `/calendar/outlook`，上海 09:00 转为 UTC 01:00，全天日期不经过 Date 时刻转换。

- [ ] **Step 9: 提交 Web contract**

```powershell
git add src/client-web/src/api/calendar.ts src/client-web/src/types/index.ts src/client-web/src/utils/calendarTime.ts tests/client-web/outlookSyncApiPath.test.ts tests/client-web/outlookSyncTypes.test.ts tests/client-web/tsconfig.outlook-sync.json
git commit -m "feat: align microsoft sync web contracts"
```

Expected: Web 可编译的 API 契约与后端 DTO 一致，不再展示不存在的 deltaLink/writebackDefault 字段。

## Task 19: 实现 Entra 四步向导和自动设备授权

**Files:**
- Create: `src/client-web/src/components/outlook/EntraSetupGuide.tsx`
- Create: `src/client-web/src/components/outlook/OutlookAuthorizationPanel.tsx`
- Create: `tests/client-web/outlookSyncUi.test.tsx`
- Modify: `src/client-web/src/pages/SyncPage.tsx`
- Modify: `src/client-web/package.json`
- Modify: `src/client-web/package-lock.json`

- [ ] **Step 1: 安装图标依赖**

Run:

```powershell
npm --prefix src/client-web install lucide-react@^1.24.0
```

Expected: only `package.json` and `package-lock.json` tracked changes; `node_modules` remains ignored.

- [ ] **Step 2: 写向导与等待授权 UI 失败测试**

Create `tests/client-web/outlookSyncUi.test.tsx`:

```tsx
import assert from 'node:assert/strict';
import path from 'node:path';
import { createRequire } from 'node:module';
import EntraSetupGuide from '../../src/client-web/src/components/outlook/EntraSetupGuide';
import OutlookAuthorizationPanel from '../../src/client-web/src/components/outlook/OutlookAuthorizationPanel';

const requireFromClient = createRequire(path.join(process.cwd(), 'src/client-web/package.json'));
const React = requireFromClient('react') as typeof import('react');
const { renderToStaticMarkup } = requireFromClient('react-dom/server') as typeof import('react-dom/server');
(globalThis as typeof globalThis & { React: typeof React }).React = React;

const guide = renderToStaticMarkup(React.createElement(EntraSetupGuide));
assert.ok(guide.includes('应用注册'));
assert.ok(guide.includes('允许公共客户端流'));
assert.ok(guide.includes('Calendars.ReadWrite'));
assert.ok(guide.includes('User.Read'));
assert.ok(guide.includes('Client ID 不是密码'));
assert.ok(guide.includes('不要创建或填写 Client Secret'));
assert.ok(!/<input[^>]*(client.?secret|密钥)/i.test(guide));

const waiting = renderToStaticMarkup(React.createElement(OutlookAuthorizationPanel, {
  session: {
    id: 'session', status: 'waiting-for-user', userCode: 'ABCD-EFGH',
    verificationUri: 'https://microsoft.com/devicelogin', expiresAt: '2026-07-10T12:00:00Z',
  },
  isStarting: false,
  onStart: () => undefined,
  onCancel: () => undefined,
}));
assert.ok(waiting.includes('ABCD-EFGH'));
assert.ok(waiting.includes('等待你在微软页面完成授权'));
assert.ok(waiting.includes('复制'));
assert.ok(!waiting.includes('完成连接'));
```

- [ ] **Step 3: 运行测试并确认组件不存在**

Run:

```powershell
npm --prefix src/client-web exec tsx -- tests/client-web/outlookSyncUi.test.tsx
```

Expected: FAIL，组件文件不存在。

- [ ] **Step 4: 实现 Entra 注册清单**

Create `src/client-web/src/components/outlook/EntraSetupGuide.tsx`:

```tsx
import { ExternalLink, ShieldCheck } from 'lucide-react';

const steps = [
  '在 Microsoft Entra 管理中心打开“应用注册”，新建 PIM Calendar Sync。',
  '支持的账户类型选择“任何组织目录中的账户和个人 Microsoft 账户”。',
  '打开“身份验证 -> 高级设置”，启用“允许公共客户端流”。',
  'Device Code Flow 不需要添加重定向 URI。',
  '在 Microsoft Graph 委托权限中添加 Calendars.ReadWrite 和 User.Read。',
  '回到应用“概述”，复制“应用程序(客户端) ID”。',
] as const;

export default function EntraSetupGuide() {
  return (
    <section aria-labelledby="entra-guide-title" className="border-b border-slate-200 pb-5">
      <div className="flex flex-wrap items-start justify-between gap-3">
        <div className="min-w-0">
          <h2 id="entra-guide-title" className="text-base font-semibold text-slate-950">1. 注册 Microsoft Entra 应用</h2>
          <p className="mt-1 text-sm leading-6 text-slate-600">按微软页面中的原始名称完成一次配置。</p>
        </div>
        <a
          href="https://entra.microsoft.com/#view/Microsoft_AAD_RegisteredApps/ApplicationsListBlade"
          target="_blank"
          rel="noreferrer"
          className="pim-button-secondary inline-flex items-center gap-2 px-3 py-2 text-sm"
        >
          打开 Entra 管理中心 <ExternalLink aria-hidden="true" className="h-4 w-4" />
        </a>
      </div>
      <ol className="mt-4 grid gap-2 md:grid-cols-2">
        {steps.map((step, index) => (
          <li key={step} className="flex min-w-0 gap-3 border-l-2 border-slate-200 py-2 pl-3 text-sm leading-6 text-slate-700">
            <span className="font-semibold text-slate-400">{index + 1}</span>
            <span>{step}</span>
          </li>
        ))}
      </ol>
      <div className="mt-4 flex items-start gap-2 rounded-lg border border-emerald-200 bg-emerald-50 px-3 py-2 text-sm text-emerald-800">
        <ShieldCheck aria-hidden="true" className="mt-0.5 h-4 w-4 shrink-0" />
        <p><strong>Client ID 不是密码。</strong>不要创建或填写 Client Secret；PIM 使用公共客户端设备授权。</p>
      </div>
    </section>
  );
}
```

- [ ] **Step 5: 实现自动等待授权面板**

Create `src/client-web/src/components/outlook/OutlookAuthorizationPanel.tsx`:

```tsx
import { useEffect, useState } from 'react';
import { Copy, ExternalLink, RefreshCw, X } from 'lucide-react';
import type { OutlookAuthorizationSession } from '../../types';

interface Props {
  session?: OutlookAuthorizationSession | null;
  isStarting: boolean;
  onStart: () => void;
  onCancel: () => void;
}

function remaining(expiresAt?: string | null) {
  if (!expiresAt) return '';
  const seconds = Math.max(0, Math.floor((new Date(expiresAt).getTime() - Date.now()) / 1000));
  return `${Math.floor(seconds / 60)}:${String(seconds % 60).padStart(2, '0')}`;
}

export default function OutlookAuthorizationPanel({ session, isStarting, onStart, onCancel }: Props) {
  const [, tick] = useState(0);
  useEffect(() => {
    if (session?.status !== 'waiting-for-user') return undefined;
    const timer = window.setInterval(() => tick(value => value + 1), 1000);
    return () => window.clearInterval(timer);
  }, [session?.status]);

  const waiting = session?.status === 'waiting-for-user';
  return (
    <section aria-labelledby="authorization-title" className="border-b border-slate-200 py-5">
      <h2 id="authorization-title" className="text-base font-semibold text-slate-950">3. 授权 Microsoft 账号</h2>
      {!waiting ? (
        <div className="mt-3 flex flex-wrap items-center gap-3">
          <button type="button" onClick={onStart} disabled={isStarting}
            className="pim-button-primary inline-flex items-center gap-2 px-4 py-2 text-sm disabled:opacity-50">
            <RefreshCw aria-hidden="true" className={`h-4 w-4 ${isStarting ? 'animate-spin' : ''}`} />
            {isStarting ? '正在请求授权码' : session?.status === 'connected' ? '重新授权' : '获取授权码'}
          </button>
          {session?.status === 'connected' && <p className="text-sm text-emerald-700">Microsoft 账号已连接。</p>}
        </div>
      ) : (
        <div className="mt-4 grid gap-4 lg:grid-cols-[minmax(0,1fr)_auto] lg:items-end">
          <div className="min-w-0">
            <p className="text-sm font-medium text-slate-700">等待你在微软页面完成授权</p>
            <div className="mt-2 flex flex-wrap items-center gap-2">
              <code className="max-w-full break-all rounded-lg border border-slate-300 bg-slate-50 px-4 py-3 text-2xl font-semibold text-slate-950">
                {session.userCode}
              </code>
              <button type="button" title="复制授权码"
                onClick={() => session.userCode && navigator.clipboard.writeText(session.userCode)}
                className="pim-button-secondary inline-flex h-10 items-center gap-2 px-3 text-sm">
                <Copy aria-hidden="true" className="h-4 w-4" /> 复制
              </button>
            </div>
            <p className="mt-2 text-xs text-slate-500">剩余时间 {remaining(session.expiresAt)}</p>
          </div>
          <div className="flex flex-wrap gap-2">
            <a href={session.verificationUri ?? 'https://microsoft.com/devicelogin'} target="_blank" rel="noreferrer"
              className="pim-button-primary inline-flex items-center gap-2 px-4 py-2 text-sm">
              打开微软授权页 <ExternalLink aria-hidden="true" className="h-4 w-4" />
            </a>
            <button type="button" onClick={onCancel} title="取消授权"
              className="pim-button-secondary inline-flex h-10 w-10 items-center justify-center" aria-label="取消授权">
              <X aria-hidden="true" className="h-4 w-4" />
            </button>
          </div>
        </div>
      )}
      {session?.errorMessage && (
        <div role="alert" className="mt-3 rounded-lg border border-red-200 bg-red-50 px-3 py-2 text-sm text-red-800">
          <p>{session.errorMessage}</p>
          {session.recoveryAction && <p className="mt-1 font-medium">{session.recoveryAction}</p>}
          {session.errorCode && <details className="mt-2"><summary>技术详情</summary><code>{session.errorCode}</code></details>}
        </div>
      )}
    </section>
  );
}
```

- [ ] **Step 6: 实现 Client ID/账号范围表单和 2 秒 session polling**

Replace the setup portion of `SyncPage.tsx` with state and queries using this exact behavior:

```tsx
const [clientId, setClientId] = useState('');
const [accountScope, setAccountScope] = useState<OutlookAccountScope>('common');
const [tenantId, setTenantId] = useState('');
const [sessionId, setSessionId] = useState<string | null>(null);

const settingsQuery = useQuery({ queryKey: ['outlook-settings'], queryFn: getOutlookSettings });
useEffect(() => {
  if (!settingsQuery.data) return;
  setClientId(settingsQuery.data.clientId);
  setAccountScope(settingsQuery.data.accountScope);
  setTenantId(settingsQuery.data.accountScope === 'organization' ? settingsQuery.data.tenantId : '');
}, [settingsQuery.data]);

const authorizationQuery = useQuery({
  queryKey: ['outlook-auth-session', sessionId],
  queryFn: () => getOutlookAuthorization(sessionId!),
  enabled: sessionId !== null,
  refetchInterval: query => query.state.data?.status === 'waiting-for-user' ? 2_000 : false,
});

const startAuthorization = useMutation({
  mutationFn: async () => {
    if (!/^[0-9a-f]{8}-[0-9a-f]{4}-[1-5][0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$/i.test(clientId.trim())) {
      throw new Error('Client ID 必须是 Entra 概述页中的 UUID');
    }
    if (accountScope === 'organization' && !tenantId.trim()) throw new Error('请填写 Directory tenant ID');
    await updateOutlookSettings({
      clientId: clientId.trim(), accountScope,
      tenantId: accountScope === 'organization' ? tenantId.trim() : null,
    });
    return startOutlookAuthorization();
  },
  onSuccess: session => setSessionId(session.id),
});
```

Render `EntraSetupGuide`, then a section titled `2. 填写应用标识` containing:

```tsx
<label className="block text-sm font-medium text-slate-700">
  应用程序(客户端) ID
  <input value={clientId} onChange={event => setClientId(event.target.value)}
    placeholder="00000000-0000-0000-0000-000000000000"
    aria-describedby="client-id-help" className="mt-1 w-full rounded-lg border border-slate-300 px-3 py-2" />
</label>
<p id="client-id-help" className="mt-1 text-xs text-slate-500">来自 Entra 应用“概述”，不是密码。</p>
<div role="group" aria-label="Microsoft 账号范围" className="mt-3 inline-flex rounded-lg border border-slate-300 p-1">
  <button type="button" aria-pressed={accountScope === 'common'} onClick={() => setAccountScope('common')}
    className={accountScope === 'common' ? 'rounded-md bg-slate-900 px-3 py-1.5 text-sm text-white' : 'rounded-md px-3 py-1.5 text-sm text-slate-600'}>
    组织账号 + 个人账号
  </button>
  <button type="button" aria-pressed={accountScope === 'organization'} onClick={() => setAccountScope('organization')}
    className={accountScope === 'organization' ? 'rounded-md bg-slate-900 px-3 py-1.5 text-sm text-white' : 'rounded-md px-3 py-1.5 text-sm text-slate-600'}>
    仅指定组织
  </button>
</div>
{accountScope === 'organization' && (
  <label className="mt-3 block text-sm font-medium text-slate-700">Directory tenant ID
    <input value={tenantId} onChange={event => setTenantId(event.target.value)}
      className="mt-1 w-full rounded-lg border border-slate-300 px-3 py-2" />
  </label>
)}
<div className="mt-3 text-sm text-slate-600">
  委托权限：<span className="font-medium">Calendars.ReadWrite</span>、<span className="font-medium">User.Read</span>
</div>
```

Scopes are text, not inputs. There is no Client Secret control.

- [ ] **Step 7: 把授权状态接入页面**

Render:

```tsx
<OutlookAuthorizationPanel
  session={authorizationQuery.data ?? startAuthorization.data}
  isStarting={startAuthorization.isPending}
  onStart={() => startAuthorization.mutate()}
  onCancel={() => sessionId && cancelOutlookAuthorization(sessionId).then(() => setSessionId(null))}
/>
```

Add `useRef` to the React import and use this effect to process each completed session exactly once:

```tsx
const handledConnectedSession = useRef<string | null>(null);

useEffect(() => {
  const session = authorizationQuery.data;
  if (!sessionId || session?.status !== 'connected' || handledConnectedSession.current === sessionId) return;
  handledConnectedSession.current = sessionId;
  queryClient.invalidateQueries({ queryKey: ['outlook-settings'] });
  queryClient.invalidateQueries({ queryKey: ['outlook-calendars'] });
  window.requestAnimationFrame(() => document.getElementById('calendar-picker-title')?.focus());
}, [authorizationQuery.data, queryClient, sessionId]);
```

The query's existing `refetchInterval` returns `false` for `expired`, `canceled`, `failed`, and `connected`, so polling stops in every terminal state. The panel then renders `recoveryAction` and the existing `获取授权码` action. Remove the old `pollOutlookDeviceCode` import and all `完成连接` UI.

- [ ] **Step 8: 运行组件测试、build 和 lint**

Run:

```powershell
npm --prefix src/client-web exec tsx -- tests/client-web/outlookSyncUi.test.tsx
npm --prefix src/client-web run build
npm --prefix src/client-web run lint
```

Expected: PASS；visible UI 明确提示不要创建或填写 Client Secret，但没有 Secret 输入框、editable scopes 或 `完成连接`，TypeScript 没有旧 deviceCode 字段。

- [ ] **Step 9: 提交向导和自动授权**

```powershell
git add src/client-web/package.json src/client-web/package-lock.json src/client-web/src/components/outlook src/client-web/src/pages/SyncPage.tsx tests/client-web/outlookSyncUi.test.tsx
git commit -m "feat: guide microsoft device authorization"
```

Expected: 未连接用户只按页面即可知道去哪里、填什么和授权后发生什么。

## Task 20: 实现日历选择、深度同步、进度和诊断 UI

**Files:**
- Create: `src/client-web/src/components/outlook/OutlookCalendarPicker.tsx`
- Create: `src/client-web/src/components/outlook/OutlookSyncControls.tsx`
- Create: `src/client-web/src/components/outlook/OutlookDiagnosticsPanel.tsx`
- Create: `tests/client-web/outlookSyncFlow.test.ts`
- Modify: `src/client-web/src/pages/SyncPage.tsx`
- Modify: `src/client-web/package.json`

- [ ] **Step 1: 扩展 UI static test**

Append to `tests/client-web/outlookSyncUi.test.tsx` imports and assertions that render one group with a default editable calendar and one read-only course calendar:

```tsx
import OutlookCalendarPicker from '../../src/client-web/src/components/outlook/OutlookCalendarPicker';
import OutlookSyncControls from '../../src/client-web/src/components/outlook/OutlookSyncControls';

const groups = [{
  id: 'group', name: '学校', calendars: [
    { id: 'default', pimCalendarId: 'p1', graphCalendarId: 'g1', groupName: '学校', name: '默认日历', isDefault: true, canEdit: true, canViewPrivateItems: false, isSelected: true, remoteState: 'active', syncStrategy: 'default-delta' },
    { id: 'course', pimCalendarId: 'p2', graphCalendarId: 'g2', groupName: '学校', name: '课程表', isDefault: false, canEdit: false, canViewPrivateItems: false, isSelected: true, remoteState: 'active', syncStrategy: 'window-reconcile' },
  ],
}];
const picker = renderToStaticMarkup(React.createElement(OutlookCalendarPicker, {
  groups, isSaving: false, onChange: () => undefined, onDiscover: () => undefined,
}));
assert.ok(picker.includes('学校'));
assert.ok(picker.includes('课程表'));
assert.ok(picker.includes('默认'));
assert.ok(picker.includes('只读'));
assert.equal((picker.match(/checked=""/g) ?? []).length, 2);

const controls = renderToStaticMarkup(React.createElement(OutlookSyncControls, {
  activeRun: null, isStarting: false,
  onStart: () => undefined, onCancel: () => undefined,
}));
assert.ok(controls.includes('立即刷新'));
assert.ok(controls.includes('全部事件资源'));
assert.ok(controls.includes('指定范围补齐'));
assert.ok(controls.includes('运行诊断'));
```

- [ ] **Step 2: 运行测试并确认组件不存在**

Run:

```powershell
npm --prefix src/client-web exec tsx -- tests/client-web/outlookSyncUi.test.tsx
```

Expected: FAIL，新组件不存在。

- [ ] **Step 3: 实现按组选择日历**

Create `src/client-web/src/components/outlook/OutlookCalendarPicker.tsx`:

```tsx
import { RefreshCw } from 'lucide-react';
import type { OutlookCalendarGroup } from '../../types';

interface Props {
  groups: OutlookCalendarGroup[];
  isSaving: boolean;
  onChange: (ids: string[]) => void;
  onDiscover: () => void;
}

export default function OutlookCalendarPicker({ groups, isSaving, onChange, onDiscover }: Props) {
  const selected = groups.flatMap(group => group.calendars).filter(calendar => calendar.isSelected).map(calendar => calendar.id);
  const toggle = (id: string, checked: boolean) => onChange(checked
    ? Array.from(new Set([...selected, id]))
    : selected.filter(selectedId => selectedId !== id));
  const toggleGroup = (group: OutlookCalendarGroup, checked: boolean) => {
    const groupIds = new Set(group.calendars.map(calendar => calendar.id));
    onChange(checked
      ? Array.from(new Set([...selected, ...groupIds]))
      : selected.filter(id => !groupIds.has(id)));
  };

  return (
    <section aria-labelledby="calendar-picker-title" className="border-b border-slate-200 py-5">
      <div className="flex flex-wrap items-center justify-between gap-3">
        <h2 id="calendar-picker-title" tabIndex={-1} className="text-base font-semibold text-slate-950">4. 选择同步日历</h2>
        <button type="button" onClick={onDiscover} className="pim-button-secondary inline-flex items-center gap-2 px-3 py-2 text-sm">
          <RefreshCw aria-hidden="true" className="h-4 w-4" /> 重新发现
        </button>
      </div>
      <div className="mt-4 divide-y divide-slate-200 border-y border-slate-200">
        {groups.map(group => {
          const all = group.calendars.length > 0 && group.calendars.every(calendar => calendar.isSelected);
          return (
            <fieldset key={group.id ?? group.name} className="py-3">
              <legend className="flex w-full items-center gap-2 px-1 text-sm font-semibold text-slate-800">
                <input type="checkbox" checked={all} onChange={event => toggleGroup(group, event.target.checked)} />
                {group.name}
              </legend>
              <div className="mt-2 grid gap-1 md:grid-cols-2">
                {group.calendars.map(calendar => (
                  <label key={calendar.id} className="flex min-w-0 items-center gap-3 rounded-lg px-2 py-2 hover:bg-slate-50">
                    <input type="checkbox" checked={calendar.isSelected} disabled={isSaving}
                      onChange={event => toggle(calendar.id, event.target.checked)} />
                    <span aria-hidden="true" className="h-3 w-3 shrink-0 rounded-sm border border-black/10"
                      style={{ backgroundColor: calendar.color || '#64748b' }} />
                    <span className="min-w-0 flex-1 truncate text-sm text-slate-800">{calendar.name}</span>
                    {calendar.isDefault && <span className="text-xs font-medium text-blue-700">默认</span>}
                    {!calendar.canEdit && <span className="text-xs font-medium text-slate-500">只读</span>}
                    {calendar.lastErrorCode && <span className="text-xs font-medium text-red-700">需处理</span>}
                  </label>
                ))}
              </div>
            </fieldset>
          );
        })}
      </div>
    </section>
  );
}
```

- [ ] **Step 4: 实现同步命令、范围和进度**

Create `src/client-web/src/components/outlook/OutlookSyncControls.tsx`:

```tsx
import { useState } from 'react';
import { CalendarRange, Database, RefreshCw, Square, Stethoscope } from 'lucide-react';
import type { OutlookSyncMode, OutlookSyncRun } from '../../types';

interface Props {
  activeRun?: OutlookSyncRun | null;
  isStarting: boolean;
  onStart: (mode: OutlookSyncMode, start?: string, end?: string) => void;
  onCancel: () => void;
  onDiagnostics?: () => void;
}

export default function OutlookSyncControls({ activeRun, isStarting, onStart, onCancel, onDiagnostics }: Props) {
  const [showRange, setShowRange] = useState(false);
  const [start, setStart] = useState('');
  const [end, setEnd] = useState('');
  const running = activeRun?.status === 'queued' || activeRun?.status === 'running';
  return (
    <section aria-labelledby="sync-controls-title" className="py-5">
      <h2 id="sync-controls-title" className="text-base font-semibold text-slate-950">同步操作</h2>
      <div className="mt-3 flex flex-wrap gap-2">
        <button type="button" disabled={isStarting || running} onClick={() => onStart('incremental')}
          className="pim-button-primary inline-flex items-center gap-2 px-3 py-2 text-sm disabled:opacity-50">
          <RefreshCw aria-hidden="true" className="h-4 w-4" /> 立即刷新
        </button>
        <button type="button" disabled={isStarting || running} onClick={() => onStart('full-resources')}
          className="pim-button-secondary inline-flex items-center gap-2 px-3 py-2 text-sm disabled:opacity-50">
          <Database aria-hidden="true" className="h-4 w-4" /> 全部事件资源
        </button>
        <button type="button" disabled={isStarting || running} onClick={() => setShowRange(value => !value)}
          className="pim-button-secondary inline-flex items-center gap-2 px-3 py-2 text-sm disabled:opacity-50">
          <CalendarRange aria-hidden="true" className="h-4 w-4" /> 指定范围补齐
        </button>
        <button type="button" onClick={onDiagnostics}
          className="pim-button-secondary inline-flex items-center gap-2 px-3 py-2 text-sm">
          <Stethoscope aria-hidden="true" className="h-4 w-4" /> 运行诊断
        </button>
        {running && (
          <button type="button" onClick={onCancel} className="pim-button-secondary inline-flex items-center gap-2 px-3 py-2 text-sm text-red-700">
            <Square aria-hidden="true" className="h-4 w-4" /> 取消
          </button>
        )}
      </div>
      {showRange && (
        <div className="mt-3 flex flex-wrap items-end gap-3 border-l-2 border-slate-300 pl-3">
          <label className="text-sm text-slate-700">开始日期
            <input type="date" value={start} onChange={event => setStart(event.target.value)} className="mt-1 block rounded-lg border border-slate-300 px-3 py-2" />
          </label>
          <label className="text-sm text-slate-700">结束日期
            <input type="date" value={end} onChange={event => setEnd(event.target.value)} className="mt-1 block rounded-lg border border-slate-300 px-3 py-2" />
          </label>
          <button type="button" disabled={!start || !end || end <= start}
            onClick={() => onStart('range-instances', `${start}T00:00:00+08:00`, `${end}T00:00:00+08:00`)}
            className="pim-button-primary px-3 py-2 text-sm disabled:opacity-50">开始补齐</button>
        </div>
      )}
      {activeRun && (
        <div className="mt-4" aria-live="polite">
          <div className="flex flex-wrap items-center justify-between gap-2 text-sm">
            <span className="font-medium text-slate-800">{activeRun.status}</span>
            <span className="text-slate-500">读取 {activeRun.readCount} · 创建 {activeRun.createdCount} · 待确认 {activeRun.confirmationCount}</span>
          </div>
          <div className="mt-2 divide-y divide-slate-200 border-y border-slate-200">
            {activeRun.calendars.map(calendar => (
              <div key={calendar.bindingId} className="flex flex-wrap justify-between gap-2 py-2 text-sm">
                <span className="min-w-0 truncate text-slate-700">{calendar.calendarName}</span>
                <span className={calendar.status === 'failed' ? 'text-red-700' : 'text-slate-500'}>{calendar.status} · {calendar.readCount}</span>
              </div>
            ))}
          </div>
        </div>
      )}
    </section>
  );
}
```

- [ ] **Step 5: 实现诊断结果面板**

Create `src/client-web/src/components/outlook/OutlookDiagnosticsPanel.tsx`:

```tsx
import type { OutlookDiagnostics } from '../../types';

export default function OutlookDiagnosticsPanel({ result }: { result?: OutlookDiagnostics | null }) {
  if (!result) return null;
  return (
    <section aria-labelledby="diagnostics-title" className="border-t border-slate-200 py-4">
      <h2 id="diagnostics-title" className="text-sm font-semibold text-slate-950">诊断结果</h2>
      <div className="mt-2 divide-y divide-slate-200">
        {result.checks.map(check => (
          <div key={check.code} className="grid gap-1 py-2 sm:grid-cols-[10rem_minmax(0,1fr)]">
            <span className={check.status === 'passed' ? 'text-sm font-medium text-emerald-700' : 'text-sm font-medium text-red-700'}>
              {check.label} · {check.status === 'passed' ? '通过' : '失败'}
            </span>
            <div className="min-w-0 text-sm text-slate-600">
              <p>{check.message}</p>
              {check.technicalCode && <details className="mt-1 text-xs"><summary>技术详情</summary><code>{check.technicalCode}</code></details>}
            </div>
          </div>
        ))}
      </div>
    </section>
  );
}
```

- [ ] **Step 6: 在 SyncPage 接入选择、run polling 和 diagnostics**

Add these queries/mutations and render the three components only when settings status is `connected`:

```tsx
const calendarsQuery = useQuery({
  queryKey: ['outlook-calendars'], queryFn: getOutlookCalendars,
  enabled: settingsQuery.data?.status === 'connected',
});
const [activeRunId, setActiveRunId] = useState<string | null>(null);
const runQuery = useQuery({
  queryKey: ['outlook-sync-run', activeRunId],
  queryFn: () => getOutlookSyncRun(activeRunId!),
  enabled: activeRunId !== null,
  refetchInterval: query => ['queued', 'running'].includes(query.state.data?.status ?? '') ? 2_000 : false,
});
const selectionMutation = useMutation({
  mutationFn: updateOutlookCalendarSelection,
  onSuccess: groups => queryClient.setQueryData(['outlook-calendars'], groups),
});
const discoverMutation = useMutation({
  mutationFn: discoverOutlookCalendars,
  onSuccess: data => queryClient.setQueryData(['outlook-calendars'], data.groups),
});
const runMutation = useMutation({
  mutationFn: ({ mode, start, end }: { mode: OutlookSyncMode; start?: string; end?: string }) =>
    startOutlookSyncRun({ mode, start, end }),
  onSuccess: run => setActiveRunId(run.id),
});
const diagnosticsMutation = useMutation({ mutationFn: runOutlookDiagnostics });
```

Render:

```tsx
<OutlookCalendarPicker
  groups={calendarsQuery.data ?? []}
  isSaving={selectionMutation.isPending}
  onChange={ids => selectionMutation.mutate(ids)}
  onDiscover={() => discoverMutation.mutate()}
/>
<OutlookSyncControls
  activeRun={runQuery.data ?? runMutation.data}
  isStarting={runMutation.isPending}
  onStart={(mode, start, end) => runMutation.mutate({ mode, start, end })}
  onCancel={() => activeRunId && cancelOutlookSyncRun(activeRunId)}
  onDiagnostics={() => diagnosticsMutation.mutate()}
/>
<OutlookDiagnosticsPanel result={diagnosticsMutation.data} />
```

Add terminal-run invalidation and one actionable error surface:

```tsx
useEffect(() => {
  const status = runQuery.data?.status;
  if (!status || !['completed', 'partial', 'failed', 'canceled'].includes(status)) return;
  for (const queryKey of [
    ['outlook-settings'],
    ['outlook-calendars'],
    ['calendar-layers'],
    ['events'],
    ['pending-confirmations'],
    ['outlook-sync-history'],
  ]) {
    queryClient.invalidateQueries({ queryKey });
  }
}, [queryClient, runQuery.data?.id, runQuery.data?.status]);

const requestFailure = selectionMutation.error
  ?? discoverMutation.error
  ?? runMutation.error
  ?? diagnosticsMutation.error;
const visibleFailure = requestFailure instanceof Error
  ? requestFailure.message
  : settingsQuery.data?.lastError;

{visibleFailure && (
  <div role="alert" className="border-y border-red-200 bg-red-50 px-3 py-3 text-sm text-red-800">
    <div className="flex flex-wrap items-center justify-between gap-2">
      <p>Microsoft 同步操作失败，请检查配置或网络后重试。</p>
      <button type="button" className="pim-button-secondary px-3 py-1.5 text-sm"
        onClick={() => { void settingsQuery.refetch(); void calendarsQuery.refetch(); }}>
        重试
      </button>
    </div>
    <details className="mt-2 text-xs"><summary>技术详情</summary><code>{visibleFailure}</code></details>
  </div>
)}
```

- [ ] **Step 7: 添加真实交互 Playwright test**

Create `tests/client-web/outlookSyncFlow.test.ts`:

```ts
import assert from 'node:assert/strict';
import { spawn, type ChildProcessWithoutNullStreams } from 'node:child_process';
import { createRequire } from 'node:module';
import { createServer } from 'node:net';
import { fileURLToPath } from 'node:url';

const requireFromWeb = createRequire(new URL('../../src/client-web/package.json', import.meta.url));
const { chromium } = requireFromWeb('playwright') as typeof import('playwright');
type Browser = import('playwright').Browser;
type Route = import('playwright').Route;

const clientId = '11111111-1111-4111-8111-111111111111';
const viewports = [{ width: 360, height: 800 }, { width: 1440, height: 1000 }] as const;

interface RunRequest {
  mode: 'incremental' | 'rolling-baseline' | 'full-resources' | 'range-instances';
  start?: string | null;
  end?: string | null;
}

interface MockState {
  clientId: string;
  status: string;
  tokenHealth: string;
  authPolls: number;
  selectedBindingIds: string[];
  selectionBodies: string[][];
  runRequests: RunRequest[];
  cancelRequests: string[];
  runs: Map<string, ReturnType<typeof runResponse>>;
}

async function main() {
  const port = await freePort();
  const baseUrl = `http://127.0.0.1:${port}`;
  const server = startVite(port);
  let browser: Browser | undefined;

  try {
    await waitForServer(baseUrl);
    browser = await chromium.launch({ headless: true });

    for (const viewport of viewports) {
      const state = createState();
      const context = await browser.newContext({ viewport });
      await context.addInitScript(() => {
        localStorage.setItem('accessToken', 'outlook-sync-flow-token');
      });
      await context.route('**/api/v1/**', route => handleApi(route, state));
      const page = await context.newPage();

      await page.goto(`${baseUrl}/settings/sync`, { waitUntil: 'domcontentloaded' });
      await page.getByLabel('应用程序(客户端) ID').fill(clientId);
      await page.getByRole('button', { name: '获取授权码' }).click();
      await page.getByText('ABCD-EFGH').waitFor();
      await page.getByRole('heading', { name: '4. 选择同步日历' }).waitFor({ timeout: 10_000 });

      const defaultCalendar = page.getByRole('checkbox', { name: /默认日历/ });
      const courseCalendar = page.getByRole('checkbox', { name: /课程表/ });
      assert.equal(await defaultCalendar.isChecked(), true);
      assert.equal(await courseCalendar.isChecked(), true);
      await courseCalendar.uncheck();
      await waitUntil(() => state.selectionBodies.length === 1, 'calendar selection PUT');
      assert.deepEqual(state.selectionBodies[0], ['default']);

      await page.getByRole('button', { name: '立即刷新' }).click();
      await waitUntil(() => state.runRequests.some(request => request.mode === 'incremental'), 'incremental run POST');
      await page.getByRole('button', { name: '取消' }).click();
      await waitUntil(() => state.cancelRequests.length === 1, 'run DELETE');

      const fullResources = page.getByRole('button', { name: '全部事件资源' });
      await waitUntil(() => fullResources.isEnabled(), 'full resources button to re-enable');
      await fullResources.click();
      await waitUntil(() => state.runRequests.some(request => request.mode === 'full-resources'), 'full-resources run POST');

      await page.getByRole('button', { name: '指定范围补齐' }).click();
      await page.getByLabel('开始日期').fill('2026-01-01');
      await page.getByLabel('结束日期').fill('2026-12-31');
      await page.getByRole('button', { name: '开始补齐' }).click();
      await waitUntil(() => state.runRequests.some(request => request.mode === 'range-instances'), 'range-instances run POST');
      const range = state.runRequests.find(request => request.mode === 'range-instances');
      assert.equal(range?.start, '2026-01-01T00:00:00+08:00');
      assert.equal(range?.end, '2026-12-31T00:00:00+08:00');

      await page.getByRole('button', { name: '运行诊断' }).click();
      await page.getByText('账号读取', { exact: false }).waitFor();
      await page.getByText('日历发现', { exact: false }).waitFor();
      assert.equal(await page.getByText('不要创建或填写 Client Secret', { exact: false }).count(), 1);
      assert.equal(await page.locator('input[name*=secret i], input[id*=secret i]').count(), 0);

      const layout = await page.evaluate(() => ({
        horizontalOverflow: document.documentElement.scrollWidth - window.innerWidth,
        clippedButtons: Array.from(document.querySelectorAll('button')).filter(button =>
          getComputedStyle(button).whiteSpace === 'nowrap' && button.scrollWidth > button.clientWidth + 2
        ).map(button => button.textContent?.trim()),
        overlapping: Array.from(document.querySelectorAll('main button, main input, main a')).some((element, index, all) => {
          const a = element.getBoundingClientRect();
          return all.slice(index + 1).some(other => {
            const b = other.getBoundingClientRect();
            return a.width > 0 && b.width > 0
              && a.left < b.right && a.right > b.left && a.top < b.bottom && a.bottom > b.top;
          });
        }),
      }));
      assert.ok(layout.horizontalOverflow <= 4, `${viewport.width}px viewport overflowed horizontally`);
      assert.deepEqual(layout.clippedButtons, []);
      assert.equal(layout.overlapping, false);

      await context.close();
    }
  } finally {
    await browser?.close();
    stopServer(server);
  }
}

function createState(): MockState {
  return {
    clientId: '',
    status: 'not-connected',
    tokenHealth: 'missing',
    authPolls: 0,
    selectedBindingIds: ['default', 'course'],
    selectionBodies: [],
    runRequests: [],
    cancelRequests: [],
    runs: new Map(),
  };
}

async function handleApi(route: Route, state: MockState) {
  const request = route.request();
  const method = request.method();
  const pathname = new URL(request.url()).pathname;

  if (pathname === '/api/v1/calendar/outlook/settings' && method === 'GET') {
    return json(route, settings(state));
  }
  if (pathname === '/api/v1/calendar/outlook/settings' && method === 'PUT') {
    const body = request.postDataJSON() as { clientId: string };
    state.clientId = body.clientId;
    return json(route, settings(state));
  }
  if (pathname === '/api/v1/calendar/outlook/auth-sessions' && method === 'POST') {
    return json(route, authSession('waiting-for-user'));
  }
  if (/\/api\/v1\/calendar\/outlook\/auth-sessions\/[^/]+$/.test(pathname) && method === 'GET') {
    state.authPolls++;
    if (state.authPolls >= 2) {
      state.status = 'connected';
      state.tokenHealth = 'healthy';
      return json(route, authSession('connected'));
    }
    return json(route, authSession('waiting-for-user'));
  }
  if (/\/api\/v1\/calendar\/outlook\/auth-sessions\/[^/]+$/.test(pathname) && method === 'DELETE') {
    return route.fulfill({ status: 204 });
  }
  if (pathname === '/api/v1/calendar/outlook/calendars' && method === 'GET') {
    return json(route, calendarGroups(state));
  }
  if (pathname === '/api/v1/calendar/outlook/calendars/discover' && method === 'POST') {
    return json(route, { discoveredCount: 2, createdCount: 0, updatedCount: 2, groups: calendarGroups(state) });
  }
  if (pathname === '/api/v1/calendar/outlook/calendars/selection' && method === 'PUT') {
    const body = request.postDataJSON() as { selectedBindingIds: string[] };
    state.selectedBindingIds = [...body.selectedBindingIds];
    state.selectionBodies.push([...body.selectedBindingIds]);
    return json(route, calendarGroups(state));
  }
  if (pathname === '/api/v1/calendar/outlook/sync-runs' && method === 'POST') {
    const body = request.postDataJSON() as RunRequest;
    state.runRequests.push(body);
    const id = `00000000-0000-4000-8000-${String(state.runRequests.length).padStart(12, '0')}`;
    const run = runResponse(id, body, body.mode === 'incremental' ? 'running' : 'completed');
    state.runs.set(id, run);
    return json(route, run, 202);
  }
  const runMatch = pathname.match(/^\/api\/v1\/calendar\/outlook\/sync-runs\/([^/]+)$/);
  if (runMatch && method === 'GET') {
    return json(route, state.runs.get(runMatch[1]) ?? runResponse(runMatch[1], { mode: 'incremental' }, 'completed'));
  }
  if (runMatch && method === 'DELETE') {
    state.cancelRequests.push(runMatch[1]);
    const run = state.runs.get(runMatch[1]);
    if (run) state.runs.set(runMatch[1], { ...run, status: 'canceled', cancelRequested: true, finishedAt: new Date().toISOString() });
    return route.fulfill({ status: 204 });
  }
  if (pathname === '/api/v1/calendar/outlook/diagnostics' && method === 'POST') {
    return json(route, {
      status: 'passed',
      checkedAt: new Date().toISOString(),
      checks: [
        { code: 'profile', label: '账号读取', status: 'passed', message: '可读取 Microsoft 账号。' },
        { code: 'discovery', label: '日历发现', status: 'passed', message: '发现 2 个日历。' },
      ],
    });
  }
  if (pathname.includes('/calendar/data-center/query')) {
    return json(route, { items: [], page: 1, pageSize: 25, totalCount: 0 });
  }
  return json(route, []);
}

function settings(state: MockState) {
  return {
    provider: 'outlook',
    clientId: state.clientId,
    accountScope: 'common',
    tenantId: 'common',
    authority: 'https://login.microsoftonline.com/common',
    scopes: ['Calendars.ReadWrite', 'User.Read'],
    status: state.status,
    tokenHealth: state.tokenHealth,
    accountDisplayName: state.status === 'connected' ? '测试用户' : null,
    accountLoginHint: state.status === 'connected' ? 'test@example.com' : null,
    lastSyncedAt: null,
    nextScheduledSyncAt: null,
    lastError: null,
  };
}

function authSession(status: 'waiting-for-user' | 'connected') {
  return {
    id: 'aaaaaaaa-aaaa-4aaa-8aaa-aaaaaaaaaaaa',
    status,
    verificationUri: 'https://microsoft.com/devicelogin',
    userCode: status === 'waiting-for-user' ? 'ABCD-EFGH' : null,
    expiresAt: new Date(Date.now() + 15 * 60_000).toISOString(),
    accountDisplayName: status === 'connected' ? '测试用户' : null,
    accountLoginHint: status === 'connected' ? 'test@example.com' : null,
    errorCode: null,
    errorMessage: null,
    recoveryAction: null,
  };
}

function calendarGroups(state: MockState) {
  const calendar = (id: string, name: string, isDefault: boolean, canEdit: boolean) => ({
    id,
    pimCalendarId: `pim-${id}`,
    graphCalendarId: `graph-${id}`,
    groupId: 'school',
    groupName: '学校',
    name,
    color: isDefault ? '#2563eb' : '#16a34a',
    isDefault,
    canEdit,
    canViewPrivateItems: false,
    isSelected: state.selectedBindingIds.includes(id),
    remoteState: 'active',
    syncStrategy: isDefault ? 'default-delta' : 'window-reconcile',
    lastSyncedAt: null,
    lastErrorCode: null,
    lastErrorMessage: null,
  });
  return [{
    id: 'school',
    name: '学校',
    calendars: [calendar('default', '默认日历', true, true), calendar('course', '课程表', false, false)],
  }];
}

function runResponse(id: string, request: RunRequest, status: string) {
  return {
    id,
    mode: request.mode,
    status,
    cancelRequested: false,
    requestedStart: request.start ?? null,
    requestedEnd: request.end ?? null,
    calendars: [{
      bindingId: 'default', calendarName: '默认日历', status,
      readCount: 2, createdCount: 1, confirmationCount: 0, failureCount: 0,
      errorCode: null, errorMessage: null,
    }],
    readCount: 2,
    createdCount: 1,
    confirmationCount: 0,
    failureCount: 0,
    startedAt: new Date().toISOString(),
    finishedAt: status === 'running' ? null : new Date().toISOString(),
    errorSummary: null,
  };
}

function json(route: Route, data: unknown, status = 200) {
  return route.fulfill({
    status,
    contentType: 'application/json',
    body: JSON.stringify({ code: 0, message: 'OK', data, timestamp: new Date().toISOString() }),
  });
}

function startVite(port: number): ChildProcessWithoutNullStreams {
  const viteBin = fileURLToPath(new URL('../../src/client-web/node_modules/vite/bin/vite.js', import.meta.url));
  const child = spawn(
    process.execPath,
    [viteBin, '--host', '127.0.0.1', '--port', String(port)],
    { cwd: 'src/client-web', stdio: ['ignore', 'pipe', 'pipe'] },
  );
  child.stdout.on('data', chunk => process.stdout.write(chunk));
  child.stderr.on('data', chunk => process.stderr.write(chunk));
  return child;
}

function stopServer(server: ChildProcessWithoutNullStreams) {
  if (!server.killed) server.kill('SIGTERM');
}

async function waitForServer(baseUrl: string) {
  for (let attempt = 0; attempt < 80; attempt++) {
    try {
      const response = await fetch(baseUrl);
      if (response.ok) return;
    } catch {
      // Vite is still starting.
    }
    await delay(250);
  }
  throw new Error(`Timed out waiting for Vite at ${baseUrl}`);
}

async function freePort(): Promise<number> {
  return new Promise((resolve, reject) => {
    const server = createServer();
    server.on('error', reject);
    server.listen(0, '127.0.0.1', () => {
      const address = server.address();
      if (!address || typeof address === 'string') {
        reject(new Error('Could not allocate a local port'));
        return;
      }
      const port = address.port;
      server.close(() => resolve(port));
    });
  });
}

async function waitUntil(check: () => boolean | Promise<boolean>, label: string) {
  for (let attempt = 0; attempt < 100; attempt++) {
    if (await check()) return;
    await delay(50);
  }
  throw new Error(`Timed out waiting for ${label}`);
}

function delay(ms: number) {
  return new Promise(resolve => setTimeout(resolve, ms));
}

main().catch((error: unknown) => {
  console.error(error);
  process.exit(1);
});
```

The test does not write screenshots or other generated artifacts into the repository.

- [ ] **Step 8: 添加专用 test script**

Add to `src/client-web/package.json`:

```json
"test:outlook-sync": "cd ../.. && npm --prefix src/client-web exec tsx -- tests/client-web/outlookSyncApiPath.test.ts && npm --prefix src/client-web exec tsc -- -p tests/client-web/tsconfig.outlook-sync.json && npm --prefix src/client-web exec tsx -- tests/client-web/outlookSyncTypes.test.ts && npm --prefix src/client-web exec tsx -- tests/client-web/outlookSyncUi.test.tsx && npm --prefix src/client-web exec tsx -- tests/client-web/outlookSyncFlow.test.ts"
```

- [ ] **Step 9: 运行专用 Web 测试**

Run:

```powershell
npm --prefix src/client-web run test:outlook-sync
npm --prefix src/client-web run build
npm --prefix src/client-web run lint
```

Expected: PASS at 360px and desktop；授权自动轮询、默认全选、只读标记、两种深度 run、取消和诊断都有证据。

- [ ] **Step 10: 提交同步控制台**

```powershell
git add src/client-web/src/components/outlook src/client-web/src/pages/SyncPage.tsx src/client-web/package.json tests/client-web/outlookSyncUi.test.tsx tests/client-web/outlookSyncFlow.test.ts
git commit -m "feat: add microsoft calendar sync controls"
```

Expected: Microsoft 配置页从原始字段表单变为可完成的工作流，不再把 deltaLink 当主要用户信息。

## Task 21: 接入 Event Editor 二级确认、只读复制和全天日期

**Files:**
- Create: `src/client-web/src/components/outlook/OutlookWritebackDialog.tsx`
- Create: `tests/client-web/outlookEventGovernanceUi.test.tsx`
- Modify: `src/modules/Pim.Module.Calendar/DTOs/CalendarDtos.cs`
- Modify: `src/modules/Pim.Module.Calendar/Services/CalendarService.cs`
- Modify: `src/client-web/src/api/calendar.ts`
- Modify: `src/client-web/src/dialogs/EventEditorDialog.tsx`
- Modify: `src/client-web/src/pages/ConfirmationsPage.tsx`
- Modify: `src/client-web/src/pages/CalendarPage.tsx`
- Modify: `src/client-web/package.json`
- Modify: `src/client-web/package-lock.json`

- [ ] **Step 1: 让 Event API 返回 binding 编辑能力和全天日期**

Task 2 已创建 `EventEntity.OutlookCalendarBinding` navigation，并把 binding 与 connection 两个外键配置为 `DeleteBehavior.SetNull`；Task 3 已将相同约束写入数据库。

Append to `EventResponse`:

```csharp
Guid? OutlookCalendarBindingId = null,
bool? OutlookCanEdit = null,
DateOnly? AllDayStartDate = null,
DateOnly? AllDayEndDateExclusive = null,
string? OutlookSyncState = null
```

Replace `CreateEventRequest` and `UpdateEventRequest` with:

```csharp
public record CreateEventRequest(
    [Required] Guid CalendarId,
    [Required][MaxLength(255)] string Title,
    string? Description,
    [MaxLength(500)] string? Location,
    DateTimeOffset? DtStart,
    DateTimeOffset? DtEnd,
    string? RRule,
    string? Uid = null,
    bool IsAllDay = false,
    string? TimeZoneId = null,
    DateOnly? AllDayStartDate = null,
    DateOnly? AllDayEndDateExclusive = null);

public record UpdateEventRequest(
    [Required] Guid CalendarId,
    [Required][MaxLength(255)] string Title,
    string? Description,
    [MaxLength(500)] string? Location,
    DateTimeOffset? DtStart,
    DateTimeOffset? DtEnd,
    string? RRule,
    string? Uid = null,
    bool? IsAllDay = null,
    string? TimeZoneId = null,
    DateOnly? AllDayStartDate = null,
    DateOnly? AllDayEndDateExclusive = null);
```

The service, rather than model binding, validates the active time representation. Add this helper inside `CalendarService`:

```csharp
private sealed record NormalizedEventTime(
    DateTimeOffset Start,
    DateTimeOffset End,
    DateOnly? AllDayStart,
    DateOnly? AllDayEndExclusive);

private static NormalizedEventTime NormalizeEventTime(
    bool isAllDay,
    DateTimeOffset? start,
    DateTimeOffset? end,
    DateOnly? allDayStart,
    DateOnly? allDayEndExclusive)
{
    if (isAllDay)
    {
        if (allDayStart is not { } dateStart
            || allDayEndExclusive is not { } dateEnd
            || dateEnd <= dateStart)
            throw new DomainException(02044, "全天日程必须提供有效的开始日期和排他结束日期。");
        return new NormalizedEventTime(
            new DateTimeOffset(dateStart.Year, dateStart.Month, dateStart.Day, 0, 0, 0, TimeSpan.Zero),
            new DateTimeOffset(dateEnd.Year, dateEnd.Month, dateEnd.Day, 0, 0, 0, TimeSpan.Zero),
            dateStart,
            dateEnd);
    }

    if (start is not { } timedStart || end is not { } timedEnd || timedEnd <= timedStart)
        throw new DomainException(02045, "定时日程必须提供有效的开始和结束时间。");
    return new NormalizedEventTime(
        timedStart.ToUniversalTime(), timedEnd.ToUniversalTime(), null, null);
}
```

At the start of `CreateEventAsync`, after loading the calendar, compute and use:

```csharp
var time = NormalizeEventTime(
    request.IsAllDay, request.DtStart, request.DtEnd,
    request.AllDayStartDate, request.AllDayEndDateExclusive);

// EventEntity initializer
DtStart = time.Start,
DtEnd = time.End,
IsAllDay = request.IsAllDay,
AllDayStartDate = time.AllDayStart,
AllDayEndDateExclusive = time.AllDayEndExclusive,
TimeZoneId = request.IsAllDay ? null : request.TimeZoneId,
```

In `UpdateEventAsync`, after the Outlook hard gate from Task 10 and before assigning fields, use:

```csharp
var isAllDay = request.IsAllDay ?? entity.IsAllDay;
var time = NormalizeEventTime(
    isAllDay,
    request.DtStart,
    request.DtEnd,
    request.AllDayStartDate ?? (isAllDay ? entity.AllDayStartDate : null),
    request.AllDayEndDateExclusive ?? (isAllDay ? entity.AllDayEndDateExclusive : null));
entity.DtStart = time.Start;
entity.DtEnd = time.End;
entity.IsAllDay = isAllDay;
entity.AllDayStartDate = time.AllDayStart;
entity.AllDayEndDateExclusive = time.AllDayEndExclusive;
entity.TimeZoneId = isAllDay ? null : request.TimeZoneId ?? entity.TimeZoneId;
```

Delete the old direct `request.DtStart`, `request.DtEnd`, and conditional `IsAllDay` assignments. Include `OutlookCalendarBinding` in event queries and append these arguments in both `MapEvent` and `MapExpandedEvent`:

```csharp
e.OutlookCalendarBindingId,
e.OutlookCalendarBinding?.CanEdit,
e.AllDayStartDate,
e.AllDayEndDateExclusive,
e.OutlookSyncState
```

Define the mutation request separately from `EventResponse` in `calendar.ts`, so null all-day timestamps are type-safe:

```ts
export interface EventMutationData {
  calendarId: string;
  title: string;
  description?: string | null;
  location?: string | null;
  dtStart?: string | null;
  dtEnd?: string | null;
  rrule?: string | null;
  uid?: string | null;
  isAllDay: boolean;
  timeZoneId?: string | null;
  allDayStartDate?: string | null;
  allDayEndDateExclusive?: string | null;
}

export async function createEvent(data: EventMutationData) {
  const response = await apiPost<ApiResponse<EventResponse>>('/calendar/events', data);
  return response.data;
}

export async function updateEvent(id: string, data: EventMutationData) {
  const response = await apiPut<ApiResponse<EventResponse>>(`/calendar/events/${encodeURIComponent(id)}`, data);
  return response.data;
}
```

- [ ] **Step 2: 写二次动作和编辑路由失败测试**

Create `tests/client-web/outlookEventGovernanceUi.test.tsx`:

```tsx
import assert from 'node:assert/strict';
import { readFileSync } from 'node:fs';
import { getOutlookWritebackActionState } from '../../src/client-web/src/components/outlook/OutlookWritebackDialog';

assert.deepEqual(getOutlookWritebackActionState(false, false), {
  label: '确认内容', disabled: false, submit: false,
});
assert.deepEqual(getOutlookWritebackActionState(true, false), {
  label: '确认并回写 Outlook', disabled: false, submit: true,
});
assert.deepEqual(getOutlookWritebackActionState(true, true), {
  label: '正在提交', disabled: true, submit: false,
});

const editor = readFileSync('src/client-web/src/dialogs/EventEditorDialog.tsx', 'utf8');
assert.ok(editor.includes('previewOutlookEventChange'));
assert.ok(editor.includes('previewOutlookEventDelete'));
assert.ok(editor.includes('copyOutlookEventToPim'));
assert.ok(editor.includes('预览回写'));
assert.ok(editor.includes('复制为 PIM 日程'));
assert.ok(editor.includes('allDayStartDate'));
assert.ok(editor.includes('type="date"'));
```

- [ ] **Step 3: 运行测试并确认 dialog 不存在**

Run:

```powershell
npm --prefix src/client-web exec tsx -- tests/client-web/outlookEventGovernanceUi.test.tsx
```

Expected: FAIL，dialog 或 state function 不存在。

- [ ] **Step 4: 实现 Outlook 二级确认 dialog**

Create `src/client-web/src/components/outlook/OutlookWritebackDialog.tsx`:

```tsx
import { useState } from 'react';
import type { OperationConfirmation } from '../../types';
import BeforeAfterDiff from '../schedule/BeforeAfterDiff';

export function getOutlookWritebackActionState(armed: boolean, pending: boolean) {
  if (pending) return { label: '正在提交', disabled: true, submit: false } as const;
  if (!armed) return { label: '确认内容', disabled: false, submit: false } as const;
  return { label: '确认并回写 Outlook', disabled: false, submit: true } as const;
}

interface Props {
  confirmation?: OperationConfirmation | null;
  isPending: boolean;
  onConfirm: (id: string) => void;
  onClose: () => void;
}

export default function OutlookWritebackDialog({ confirmation, isPending, onConfirm, onClose }: Props) {
  const [armed, setArmed] = useState(false);
  if (!confirmation) return null;
  const action = getOutlookWritebackActionState(armed, isPending);
  return (
    <div role="dialog" aria-modal="true" aria-labelledby="outlook-writeback-title"
      className="fixed inset-0 z-50 flex items-end bg-black/30 sm:items-center sm:justify-center">
      <section className="max-h-[92vh] w-full overflow-y-auto bg-white p-4 sm:max-w-2xl sm:rounded-lg sm:border sm:border-slate-200">
        <h2 id="outlook-writeback-title" className="text-base font-semibold text-slate-950">Outlook 二级确认</h2>
        <p className="mt-1 text-sm text-slate-600">{confirmation.summary}</p>
        <div className="mt-4">
          <BeforeAfterDiff beforeJson={confirmation.beforeJson} afterJson={confirmation.afterJson}
            changedFields={confirmation.changedFields ?? []} />
        </div>
        {armed && (
          <div role="alert" className="mt-4 rounded-lg border border-amber-300 bg-amber-50 px-3 py-2 text-sm text-amber-900">
            下一次点击会先修改 Microsoft Graph；成功后才更新 PIM。
          </div>
        )}
        <div className="mt-5 flex flex-wrap justify-end gap-2">
          <button type="button" onClick={onClose} disabled={isPending} className="pim-button-secondary px-4 py-2 text-sm">取消</button>
          <button type="button" disabled={action.disabled}
            onClick={() => action.submit ? onConfirm(confirmation.id) : setArmed(true)}
            className="pim-button-primary px-4 py-2 text-sm disabled:opacity-50">{action.label}</button>
        </div>
      </section>
    </div>
  );
}
```

- [ ] **Step 5: 安装 FullCalendar named-time-zone plugin**

Run:

```powershell
npm --prefix src/client-web install @fullcalendar/luxon3@6.1.20 luxon@^3.7.2
```

Expected: package manifest/lock only. In `CalendarPage.tsx`, import `luxonPlugin`, add it to the FullCalendar plugin array, and set:

```tsx
timeZone="Asia/Shanghai"
```

- [ ] **Step 6: 规范化 Event Editor 初始值和提交 payload**

In `EventEditorDialog.tsx`, import the Shanghai helpers and initialize timed/all-day state separately:

```tsx
const [dtStart, setDtStart] = useState(event && !event.isAllDay ? toShanghaiInputValue(event.dtStart) : defaultStart || '');
const [dtEnd, setDtEnd] = useState(event && !event.isAllDay ? toShanghaiInputValue(event.dtEnd) : defaultEnd || '');
const [allDayStartDate, setAllDayStartDate] = useState(allDayDate(event?.allDayStartDate));
const [allDayEndDateExclusive, setAllDayEndDateExclusive] = useState(allDayDate(event?.allDayEndDateExclusive));
```

Build the request:

```tsx
const EMPTY_CALENDAR_ID = '00000000-0000-0000-0000-000000000000';
const data = {
  title, description, location,
  dtStart: isAllDay ? null : fromShanghaiInputToUtc(dtStart),
  dtEnd: isAllDay ? null : fromShanghaiInputToUtc(dtEnd),
  isAllDay,
  allDayStartDate: isAllDay ? allDayStartDate : null,
  allDayEndDateExclusive: isAllDay ? allDayEndDateExclusive : null,
  calendarId: selectedCalendarId || event?.calendarId || EMPTY_CALENDAR_ID,
};
```

Render `type="date"` inputs when all-day and `type="datetime-local"` otherwise. Do not convert an all-day date through `Date`.

- [ ] **Step 7: 分流 Outlook update/delete/copy**

Add:

```tsx
const isOutlook = Boolean(event?.outlookCalendarBindingId || event?.source === 'outlook');
const isReadOnlyOutlook = isOutlook && event?.outlookCanEdit === false;
const [outlookConfirmation, setOutlookConfirmation] = useState<OperationConfirmation | null>(null);

const previewUpdateMut = useMutation({
  mutationFn: (data: OutlookEventChangeRequest) => previewOutlookEventChange(event!.id, data),
  onSuccess: setOutlookConfirmation,
});
const previewDeleteMut = useMutation({
  mutationFn: () => previewOutlookEventDelete(event!.id),
  onSuccess: setOutlookConfirmation,
});
const confirmOutlookMut = useMutation({
  mutationFn: (id: string) => confirmOperationSecondLevel(id),
  onSuccess: () => {
    queryClient.invalidateQueries({ queryKey: ['pending-confirmations'] });
    queryClient.invalidateQueries({ queryKey: ['events'] });
    setOutlookConfirmation(null);
    onClose();
  },
});
const copyMut = useMutation({
  mutationFn: () => copyOutlookEventToPim(event!.id),
  onSuccess: () => {
    queryClient.invalidateQueries({ queryKey: ['events'] });
    onClose();
  },
});
```

Change submit routing:

```tsx
if (!event) createMut.mutate(data);
else if (isOutlook) previewUpdateMut.mutate(data);
else updateMut.mutate(data);
```

Change delete routing to `previewDeleteMut.mutate()` for Outlook and the existing confirmation/delete for manual events. For `isReadOnlyOutlook`, disable fact inputs and show only `复制为 PIM 日程`; do not render preview/delete commands. Change the Outlook editable submit label to `预览回写`.

- [ ] **Step 8: 挂载 dialog 并修正确认中心文案**

Render after the editor:

```tsx
<OutlookWritebackDialog
  confirmation={outlookConfirmation}
  isPending={confirmOutlookMut.isPending}
  onConfirm={id => confirmOutlookMut.mutate(id)}
  onClose={() => setOutlookConfirmation(null)}
/>
```

In `ConfirmationsPage.tsx`, replace English action labels:

```ts
export function getConfirmActionState(requiresArm: boolean, armed: boolean) {
  if (!requiresArm) return { label: '确认', requiresArm: false };
  if (!armed) return { label: '复核内容', requiresArm: true };
  return { label: '确认并执行', requiresArm: false };
}
```

Render `StrictConfirmationPanel` only when `confirmation.requiresSecondLevelConfirmation || confirmation.requiresStrictConfirmation`. Update `tests/client-web/confirmationSecondLevel.test.ts` expected Chinese labels.

- [ ] **Step 9: 运行 Web governance、专用测试和构建**

Add `outlookEventGovernanceUi.test.tsx` to `test:outlook-sync`, then run:

```powershell
npm --prefix src/client-web run test:outlook-sync
npm --prefix src/client-web run build
npm --prefix src/client-web run lint
```

Expected: PASS；Outlook 编辑不调用普通 update/delete，二级确认需要两个明确动作，只读日历只能复制，全天日期不偏移。

- [ ] **Step 10: 运行后端旁路和只读测试**

Run:

```powershell
dotnet test tests/Pim.UnitTests/Pim.UnitTests.csproj --filter "FullyQualifiedName~OutlookChangePreviewTests|FullyQualifiedName~OutlookConfirmedOperationHandlerTests|FullyQualifiedName~OutlookSourceGovernanceTests"
```

Expected: PASS；前端即使被绕过，普通 PUT/DELETE 仍由后端拒绝。

- [ ] **Step 11: 提交编辑器治理链**

```powershell
git add src/modules/Pim.Module.Calendar src/client-web/package.json src/client-web/package-lock.json src/client-web/src/components/outlook/OutlookWritebackDialog.tsx src/client-web/src/dialogs/EventEditorDialog.tsx src/client-web/src/pages/ConfirmationsPage.tsx src/client-web/src/pages/CalendarPage.tsx tests/client-web
git commit -m "feat: confirm outlook edits before graph writeback"
```

Expected: 编辑、删除、只读复制、上海时区和全天日期形成端到端 UI 契约。

## Task 22: 完成 ETag 冲突重新预览和用户决策

**Files:**
- Create: `src/modules/Pim.Module.Calendar/Services/OutlookChangeProposal.cs`
- Create: `tests/Pim.UnitTests/Calendar/OutlookConflictRecoveryTests.cs`
- Modify: `src/modules/Pim.Module.Calendar/Services/OutlookConflictService.cs`
- Modify: `src/modules/Pim.Module.Calendar/Services/OutlookConfirmedOperationHandler.cs`
- Modify: `src/modules/Pim.Module.Calendar/Services/OutlookChangePreviewService.cs`
- Modify: `src/modules/Pim.Module.Calendar/DTOs/OutlookSyncDtos.cs`
- Modify: `src/modules/Pim.Module.Calendar/Services/OutlookSyncFacade.cs`
- Modify: `src/modules/Pim.Module.Calendar/OutlookEndpoints.cs`
- Modify: `tests/Pim.UnitTests/Calendar/OutlookApiContractTests.cs`
- Modify: `src/client-web/src/types/index.ts`
- Modify: `src/client-web/src/api/calendar.ts`
- Modify: `src/client-web/src/pages/SyncPage.tsx`
- Modify: `src/client-web/src/components/schedule/OutlookConflictResolver.tsx`
- Modify: `tests/client-web/outlookSyncApiPath.test.ts`
- Modify: `tests/client-web/outlookEventGovernanceUi.test.tsx`

- [ ] **Step 1: 定义稳定来源和冲突 API 契约**

`SyncConflictEntity.SourceConfirmationId` and its migration/index were introduced in Tasks 2 and 3. Append these request/response contracts to `OutlookSyncDtos.cs`:

```csharp
public sealed record ResolveOutlookConflictRequest(
    [Required] string Action,
    OutlookEventChangeRequest? Merged);

public sealed record OutlookConflictResponse(
    Guid Id,
    Guid EventId,
    string GraphEventId,
    string ConflictKind,
    string Status,
    string PimSnapshotJson,
    string ExternalSnapshotJson,
    Guid? SourceConfirmationId,
    Guid? ResolvedConfirmationId,
    bool CanEdit);
```

Allowed action values are exactly `keep-pim`, `accept-outlook`, `merge`, and `defer`. `SourceConfirmationId` always identifies the confirmation whose execution entered conflict; `ResolvedConfirmationId` is null until a new resolution confirmation is created.

- [ ] **Step 2: 写最新 ETag、只读边界和 defer 失败测试**

Create `tests/Pim.UnitTests/Calendar/OutlookConflictRecoveryTests.cs`:

```csharp
using System.Text.Json;
using Microsoft.EntityFrameworkCore;
using Pim.Infrastructure.Auth;
using Pim.Infrastructure.Data;
using Pim.Infrastructure.Operations;
using Pim.Module.Calendar.DTOs;
using Pim.Module.Calendar.Entities;
using Pim.Module.Calendar.Services;
using Xunit;

namespace Pim.UnitTests.Calendar;

public sealed class OutlookConflictRecoveryTests
{
    private static readonly JsonSerializerOptions WebJson = new(JsonSerializerDefaults.Web);

    [Fact]
    public async Task KeepPim_LoadsLatestRemoteAndCreatesNewConfirmationWithLatestEtag()
    {
        await using var db = CreateDb();
        var seed = SeedConflict(db, canEdit: true, expectedEtag: "etag-old", proposedTitle: "PIM 标题");
        var graph = GraphWithRemote(seed, "Outlook 最新标题", "etag-latest");
        var service = Service(db, graph, seed.UserId);

        var confirmation = await service.ResolveOutlookAsync(
            seed.UserId, seed.ConflictId,
            new ResolveOutlookConflictRequest("keep-pim", null), CancellationToken.None);

        Assert.NotNull(confirmation);
        Assert.Equal("outlook.event.update", confirmation!.OperationType);
        Assert.Contains("etag-latest", confirmation.PayloadJson);
        Assert.NotEqual(seed.OriginalConfirmationId, confirmation.Id);
        var conflict = await db.Set<SyncConflictEntity>().SingleAsync();
        Assert.Equal(seed.OriginalConfirmationId, conflict.SourceConfirmationId);
        Assert.Equal(confirmation.Id, conflict.ResolvedConfirmationId);
    }

    [Fact]
    public async Task AcceptOutlook_CreatesPullConfirmationWithoutGraphPatch()
    {
        await using var db = CreateDb();
        var seed = SeedConflict(db, canEdit: false, expectedEtag: "etag-old", proposedTitle: "PIM 标题");
        var graph = GraphWithRemote(seed, "Outlook 最新标题", "etag-latest");
        var service = Service(db, graph, seed.UserId);

        var confirmation = await service.ResolveOutlookAsync(
            seed.UserId, seed.ConflictId,
            new ResolveOutlookConflictRequest("accept-outlook", null), CancellationToken.None);

        Assert.Equal("outlook.event.pull-update", confirmation?.OperationType);
        Assert.Empty(graph.Patches);
    }

    [Fact]
    public async Task ConfirmedRecovery_CompletesExecutionAndResolvesConflict()
    {
        await using var db = CreateDb();
        var seed = SeedConflict(db, canEdit: true, expectedEtag: "etag-old", proposedTitle: "PIM 标题");
        var graph = GraphWithRemote(seed, "Outlook 最新标题", "etag-latest");
        var service = Service(db, graph, seed.UserId);
        var confirmation = await service.ResolveOutlookAsync(
            seed.UserId, seed.ConflictId,
            new ResolveOutlookConflictRequest("keep-pim", null), CancellationToken.None);
        var confirmations = new OperationConfirmationService(db, [new OutlookOperationOutboxWriter(db)]);

        await confirmations.ConfirmSecondLevelAsync(confirmation!.Id, seed.UserId);
        var executionId = await db.Set<OutlookOperationExecutionEntity>()
            .Where(item => item.ConfirmationId == confirmation.Id)
            .Select(item => item.Id)
            .SingleAsync();
        await new OutlookConfirmedOperationHandler(db, graph, new OutlookEventMapper())
            .ExecuteAsync(executionId, CancellationToken.None);

        Assert.Equal("resolved", (await db.Set<SyncConflictEntity>().SingleAsync()).Status);
        Assert.Equal("PIM 标题", (await db.Set<EventEntity>().SingleAsync()).Title);
        Assert.Single(graph.Patches);
    }

    [Fact]
    public async Task ReadOnlyBinding_RejectsKeepPimButDeferLeavesConflictOpen()
    {
        await using var db = CreateDb();
        var seed = SeedConflict(db, canEdit: false, expectedEtag: "etag-old", proposedTitle: "PIM 标题");
        var graph = GraphWithRemote(seed, "Outlook 最新标题", "etag-latest");
        var service = Service(db, graph, seed.UserId);

        await Assert.ThrowsAsync<Pim.Core.Exceptions.DomainException>(() => service.ResolveOutlookAsync(
            seed.UserId, seed.ConflictId,
            new ResolveOutlookConflictRequest("keep-pim", null), CancellationToken.None));
        var deferred = await service.ResolveOutlookAsync(
            seed.UserId, seed.ConflictId,
            new ResolveOutlookConflictRequest("defer", null), CancellationToken.None);

        Assert.Null(deferred);
        Assert.Equal("open", (await db.Set<SyncConflictEntity>().SingleAsync()).Status);
    }

    private sealed record ConflictSeed(
        Guid UserId,
        Guid ConflictId,
        Guid OriginalConfirmationId,
        string GraphCalendarId,
        string GraphEventId);

    private static ConflictSeed SeedConflict(
        PimDbContext db,
        bool canEdit,
        string expectedEtag,
        string proposedTitle)
    {
        var userId = Guid.NewGuid();
        var originalConfirmationId = Guid.NewGuid();
        var connection = new OutlookConnectionEntity
        {
            UserId = userId,
            ClientId = Guid.NewGuid().ToString(),
            Status = "connected",
            TokenHealth = "healthy"
        };
        var calendar = new CalendarEntity { UserId = userId, Name = "Outlook", Source = "outlook" };
        var binding = new OutlookCalendarBindingEntity
        {
            ConnectionId = connection.Id,
            PimCalendarId = calendar.Id,
            GraphCalendarId = "graph-calendar",
            Name = "Outlook",
            CanEdit = canEdit,
            IsSelected = true
        };
        var evt = new EventEntity
        {
            Calendar = calendar,
            CalendarId = calendar.Id,
            Uid = "ical-event",
            Title = "本地旧标题",
            DtStart = new DateTimeOffset(2026, 7, 10, 1, 0, 0, TimeSpan.Zero),
            DtEnd = new DateTimeOffset(2026, 7, 10, 2, 0, 0, TimeSpan.Zero),
            Source = "outlook",
            OutlookConnectionId = connection.Id,
            OutlookCalendarBindingId = binding.Id,
            OutlookEventId = "graph-event",
            OutlookChangeKey = "change-old",
            OutlookEtag = expectedEtag,
            OriginalStartTimeZone = "UTC",
            OriginalEndTimeZone = "UTC",
            GraphRecurrenceJson = "{}"
        };
        var before = OutlookEventMapper.Snapshot(evt);
        var proposed = before with { Title = proposedTitle };
        var hash = OutlookChangeProposal.Hash(proposed);
        var payload = new OutlookConfirmedOperationPayload(
            userId, connection.Id, binding.Id, evt.Id, binding.GraphCalendarId,
            evt.OutlookEventId!, expectedEtag, evt.OutlookChangeKey,
            hash, "update", before, proposed);
        var execution = new OutlookOperationExecutionEntity
        {
            ConfirmationId = originalConfirmationId,
            UserId = userId,
            OperationType = "outlook.event.update",
            ProposedHash = hash,
            PayloadJson = JsonSerializer.Serialize(payload, WebJson),
            State = "conflict"
        };
        var conflict = new SyncConflictEntity
        {
            UserId = userId,
            Provider = "outlook",
            ObjectType = "event",
            ObjectId = evt.Id,
            GraphEventId = evt.OutlookEventId,
            ConflictKind = "etag-conflict",
            Status = "open",
            PimSnapshotJson = JsonSerializer.Serialize(proposed, WebJson),
            ExternalSnapshotJson = JsonSerializer.Serialize(before, WebJson),
            SourceConfirmationId = originalConfirmationId
        };
        db.AddRange(connection, calendar, binding, evt, execution, conflict);
        db.SaveChanges();
        return new ConflictSeed(userId, conflict.Id, originalConfirmationId, binding.GraphCalendarId, evt.OutlookEventId!);
    }

    private static ProgrammableGraphCalendarClient GraphWithRemote(
        ConflictSeed seed,
        string title,
        string etag)
    {
        var graph = new ProgrammableGraphCalendarClient();
        graph.Events[(seed.GraphCalendarId, seed.GraphEventId)] = new GraphEventDto(
            seed.GraphEventId, title, null,
            new GraphDateTimeTimeZoneDto("2026-07-10T01:00:00Z", "UTC"),
            new GraphDateTimeTimeZoneDto("2026-07-10T02:00:00Z", "UTC"),
            false, "ical-event", null, "singleInstance", "change-latest", etag,
            DateTimeOffset.UtcNow, null, null, null);
        return graph;
    }

    private static OutlookConflictService Service(
        PimDbContext db,
        ProgrammableGraphCalendarClient graph,
        Guid userId) => new(
            db,
            new FixedCurrentUser(userId),
            new OperationConfirmationService(db),
            graph,
            new OutlookEventMapper());

    private static PimDbContext CreateDb()
    {
        PimDbContext.RegisterModuleAssembly(typeof(OutlookConnectionEntity).Assembly);
        return new PimDbContext(new DbContextOptionsBuilder<PimDbContext>()
            .UseInMemoryDatabase($"outlook-conflict-recovery-{Guid.NewGuid()}")
            .Options);
    }

    private sealed class FixedCurrentUser(Guid userId) : ICurrentUserService
    {
        public Guid? UserId { get; } = userId;
        public string? Role => "user";
    }
}
```

- [ ] **Step 3: 运行测试并确认恢复服务不满足契约**

Run:

```powershell
dotnet test tests/Pim.UnitTests/Pim.UnitTests.csproj --filter FullyQualifiedName~OutlookConflictRecoveryTests
```

Expected: FAIL，编译错误指向 `OutlookChangeProposal`、新 constructor 或 `ResolveOutlookAsync`。

- [ ] **Step 4: 让 handler 保存来源确认和最新远端快照**

Replace the `PreconditionFailed` and permanent Graph catches in `ExecuteAsync` with:

```csharp
catch (GraphRequestException exception) when (exception.StatusCode == HttpStatusCode.PreconditionFailed)
{
    await MarkConflictAsync(
        executionId, payload, "etag-conflict", await TryGetLatestAsync(payload, ct), ct);
}
catch (Exception exception) when (IsRetryable(exception))
{
    await MarkRetryableAsync(executionId, exception, ct);
}
catch (DbUpdateException exception)
{
    await MarkRetryableAsync(executionId, exception, ct);
}
catch (GraphRequestException exception)
{
    var code = exception.StatusCode == HttpStatusCode.Forbidden
        ? "graph-403"
        : $"graph-{(int)exception.StatusCode}";
    await MarkConflictAsync(executionId, payload, code, await TryGetLatestAsync(payload, ct), ct);
}
```

In `ExecuteCoreAsync`, pass `remote` to `MarkConflictAsync` for `remote-delete-no-longer-current` and both in-memory ETag decisions; pass null for `remote-event-missing`:

```csharp
await MarkConflictAsync(executionId, payload, "remote-delete-no-longer-current", remote, ct);
await MarkConflictAsync(executionId, payload, "etag-conflict", remote, ct);
await MarkConflictAsync(executionId, payload, "remote-event-missing", null, ct);
```

Replace `MarkConflictAsync` and add the latest-state helper:

```csharp
private async Task<GraphEventDto?> TryGetLatestAsync(
    OutlookConfirmedOperationPayload payload,
    CancellationToken ct)
{
    try
    {
        return await _graph.GetEventAsync(
            payload.ConnectionId, payload.GraphCalendarId, payload.GraphEventId, ct);
    }
    catch (OperationCanceledException) when (ct.IsCancellationRequested)
    {
        throw;
    }
    catch (Exception exception) when (exception is HttpRequestException
        or TimeoutException or TaskCanceledException or GraphRequestException)
    {
        return null;
    }
}

private async Task MarkConflictAsync(
    Guid executionId,
    OutlookConfirmedOperationPayload payload,
    string code,
    GraphEventDto? remote,
    CancellationToken ct)
{
    _db.ChangeTracker.Clear();
    var execution = await _db.Set<OutlookOperationExecutionEntity>()
        .SingleAsync(item => item.Id == executionId, ct);
    execution.State = "conflict";
    execution.LastErrorCode = code;
    execution.LastErrorMessage = "远端事件自预览后发生变化，需要重新生成确认。";
    execution.NextAttemptAt = null;
    execution.UpdatedAt = DateTimeOffset.UtcNow;

    var conflict = await _db.Set<SyncConflictEntity>()
        .SingleOrDefaultAsync(item => item.SourceConfirmationId == execution.ConfirmationId, ct)
        ?? await _db.Set<SyncConflictEntity>()
            .SingleOrDefaultAsync(item => item.ResolvedConfirmationId == execution.ConfirmationId, ct)
        ?? await _db.Set<SyncConflictEntity>().SingleOrDefaultAsync(item =>
            item.UserId == payload.UserId
            && item.ObjectId == payload.PimEventId
            && item.GraphEventId == payload.GraphEventId
            && item.ConflictKind == code
            && item.Status == "open", ct);
    if (conflict is null)
    {
        conflict = new SyncConflictEntity
        {
            UserId = payload.UserId,
            Provider = "outlook",
            ObjectType = "event",
            ObjectId = payload.PimEventId,
            GraphEventId = payload.GraphEventId,
            ConflictKind = code,
            Status = "open",
            SourceConfirmationId = execution.ConfirmationId,
            CreatedAt = DateTimeOffset.UtcNow
        };
        _db.Add(conflict);
    }
    conflict.SourceConfirmationId = execution.ConfirmationId;
    conflict.ConflictKind = code;
    conflict.Status = "open";
    conflict.PimSnapshotJson = JsonSerializer.Serialize(payload.Proposed, JsonOptions);
    conflict.ExternalSnapshotJson = remote is null
        ? "null"
        : JsonSerializer.Serialize(_mapper.Snapshot(remote), JsonOptions);
    conflict.ResolvedConfirmationId = null;
    conflict.UpdatedAt = DateTimeOffset.UtcNow;
    await _db.SaveChangesAsync(ct);
}
```

In `CommitLocalAsync`, immediately before its final `SaveChangesAsync`, close the conflict whose replacement confirmation just executed:

```csharp
var resolvedConflicts = await _db.Set<SyncConflictEntity>()
    .Where(item => item.ResolvedConfirmationId == execution.ConfirmationId)
    .ToListAsync(ct);
foreach (var conflict in resolvedConflicts)
{
    conflict.Status = "resolved";
    conflict.UpdatedAt = DateTimeOffset.UtcNow;
}
```

The original confirmation remains `Confirmed`, its execution remains terminal `conflict`, and only `SourceConfirmationId` points back to it.

- [ ] **Step 5: 共享 proposed/hash 并按最新 Graph 状态创建新确认**

Create `src/modules/Pim.Module.Calendar/Services/OutlookChangeProposal.cs`:

```csharp
using System.Security.Cryptography;
using System.Text;
using System.Text.Json;
using Pim.Core.Exceptions;
using Pim.Module.Calendar.DTOs;

namespace Pim.Module.Calendar.Services;

public static class OutlookChangeProposal
{
    private static readonly JsonSerializerOptions JsonOptions = new(JsonSerializerDefaults.Web);

    public static OutlookEventSnapshot FromRequest(
        OutlookEventChangeRequest request,
        OutlookEventSnapshot template)
    {
        if (request.IsAllDay)
        {
            if (request.AllDayStartDate is not { } start
                || request.AllDayEndDateExclusive is not { } end
                || end <= start)
                throw new DomainException(02044, "全天日程必须提供有效的开始日期和排他结束日期。");
            return new OutlookEventSnapshot(
                request.Title, request.Description, request.Location,
                new DateTimeOffset(start.Year, start.Month, start.Day, 0, 0, 0, TimeSpan.Zero),
                new DateTimeOffset(end.Year, end.Month, end.Day, 0, 0, 0, TimeSpan.Zero),
                true, start, end, "UTC", "UTC", template.GraphRecurrenceJson);
        }

        if (request.DtStart is not { } timedStart
            || request.DtEnd is not { } timedEnd
            || timedEnd <= timedStart)
            throw new DomainException(02045, "定时日程必须提供有效的开始和结束时间。");
        return new OutlookEventSnapshot(
            request.Title, request.Description, request.Location,
            timedStart.ToUniversalTime(), timedEnd.ToUniversalTime(),
            false, null, null,
            template.OriginalStartTimeZone, template.OriginalEndTimeZone,
            template.GraphRecurrenceJson);
    }

    public static string Hash(OutlookEventSnapshot? snapshot)
        => Convert.ToHexString(SHA256.HashData(Encoding.UTF8.GetBytes(
            JsonSerializer.Serialize(snapshot, JsonOptions)))).ToLowerInvariant();
}
```

In `OutlookChangePreviewService`, delete its private `Proposed` and `Hash` methods and replace their call sites exactly:

```csharp
var proposed = OutlookChangeProposal.FromRequest(request, before);
var hash = OutlookChangeProposal.Hash(proposed);
// PreviewDeleteAsync:
var hash = OutlookChangeProposal.Hash(null);
```

Add `IGraphCalendarClient` and `OutlookEventMapper` fields to `OutlookConflictService`, then replace its constructor with:

```csharp
public OutlookConflictService(
    PimDbContext db,
    ICurrentUserService currentUser,
    IOperationConfirmationService confirmations,
    IGraphCalendarClient? graph = null,
    OutlookEventMapper? mapper = null)
{
    _db = db;
    _currentUser = currentUser;
    _confirmations = confirmations;
    _graph = graph;
    _mapper = mapper ?? new OutlookEventMapper();
}
```

Keep the existing `GetAsync`, `RequestActionAsync`, and stop-sync methods so current governance endpoints and tests remain source-compatible. Add these user-scoped recovery methods alongside them:

```csharp
private static readonly HashSet<string> RecoveryActions =
    new(StringComparer.Ordinal) { "keep-pim", "accept-outlook", "merge", "defer" };

public async Task<IReadOnlyList<OutlookConflictResponse>> ListOutlookAsync(
    Guid userId,
    CancellationToken ct)
{
    await ReopenRejectedResolutionsAsync(userId, ct);
    var conflicts = await _db.Set<SyncConflictEntity>().AsNoTracking()
        .Where(item => item.UserId == userId
            && item.Provider == "outlook"
            && item.Status == "open")
        .OrderByDescending(item => item.UpdatedAt)
        .ToListAsync(ct);
    var result = new List<OutlookConflictResponse>(conflicts.Count);
    foreach (var conflict in conflicts) result.Add(await MapOutlookAsync(conflict, userId, ct));
    return result;
}

private async Task ReopenRejectedResolutionsAsync(Guid userId, CancellationToken ct)
{
    var pending = await _db.Set<SyncConflictEntity>()
        .Where(item => item.UserId == userId
            && item.Provider == "outlook"
            && item.Status == "pending-confirmation"
            && item.ResolvedConfirmationId != null)
        .ToListAsync(ct);
    if (pending.Count == 0) return;
    var confirmationIds = pending.Select(item => item.ResolvedConfirmationId!.Value).ToList();
    var terminal = await _db.OperationConfirmations.AsNoTracking()
        .Where(item => confirmationIds.Contains(item.Id)
            && (item.Status == OperationConfirmationStatus.Rejected.ToString()
                || item.Status == OperationConfirmationStatus.Expired.ToString()))
        .Select(item => item.Id)
        .ToListAsync(ct);
    foreach (var conflict in pending.Where(item => terminal.Contains(item.ResolvedConfirmationId!.Value)))
    {
        conflict.Status = "open";
        conflict.ResolvedConfirmationId = null;
        conflict.UpdatedAt = DateTimeOffset.UtcNow;
    }
    if (terminal.Count > 0) await _db.SaveChangesAsync(ct);
}

public async Task<OutlookConflictResponse> GetOutlookAsync(
    Guid userId,
    Guid conflictId,
    CancellationToken ct)
{
    var conflict = await LoadOutlookConflictAsync(userId, conflictId, ct);
    return await MapOutlookAsync(conflict, userId, ct);
}

public async Task<OperationConfirmationDto?> ResolveOutlookAsync(
    Guid userId,
    Guid conflictId,
    ResolveOutlookConflictRequest request,
    CancellationToken ct)
{
    if (!RecoveryActions.Contains(request.Action))
        throw new DomainException(02040, "不支持的 Outlook 冲突处理动作。");
    var conflict = await LoadOutlookConflictAsync(userId, conflictId, ct);
    if (conflict.Status != "open")
        throw new DomainException(02050, "该 Outlook 冲突已经进入处理流程。");
    if (request.Action == "defer") return null;

    var evt = await _db.Set<EventEntity>().IgnoreQueryFilters()
        .Include(item => item.Calendar)
        .SingleOrDefaultAsync(item => item.Id == conflict.ObjectId && item.Calendar.UserId == userId, ct)
        ?? throw new DomainException(02001, "日程不存在。");
    if (evt.OutlookCalendarBindingId is not { } bindingId
        || evt.OutlookConnectionId is not { } connectionId
        || string.IsNullOrWhiteSpace(evt.OutlookEventId))
        throw new DomainException(02047, "该日程没有可用的 Outlook 日历绑定。");
    var binding = await _db.Set<OutlookCalendarBindingEntity>()
        .SingleAsync(item => item.Id == bindingId && item.ConnectionId == connectionId, ct);
    if (!binding.CanEdit && request.Action is "keep-pim" or "merge")
        throw new DomainException(02048, "该 Outlook 日历为只读，只能采用 Outlook 最新值或暂不处理。");

    var graph = _graph ?? throw new DomainException(02055, "Microsoft Graph client 未配置。");
    var remote = await graph.GetEventAsync(
        binding.ConnectionId, binding.GraphCalendarId, evt.OutlookEventId!, ct);
    if (remote is null)
        throw new DomainException(02049, "Outlook 日程已不存在，请改用删除冲突流程。");
    if (conflict.SourceConfirmationId is not { } sourceConfirmationId)
        throw new DomainException(02051, "冲突缺少原始确认来源，不能安全恢复。");
    var originalExecution = await _db.Set<OutlookOperationExecutionEntity>().AsNoTracking()
        .SingleAsync(item => item.UserId == userId && item.ConfirmationId == sourceConfirmationId, ct);
    var originalPayload = JsonSerializer.Deserialize<OutlookConfirmedOperationPayload>(
        originalExecution.PayloadJson, JsonOptions)
        ?? throw new DomainException(02052, "原始 Outlook 执行载荷无效。");

    var before = OutlookEventMapper.Snapshot(evt);
    var latest = _mapper.Snapshot(remote);
    var proposed = request.Action switch
    {
        "keep-pim" when originalPayload.Proposed is not null => originalPayload.Proposed,
        "accept-outlook" => latest,
        "merge" when request.Merged is not null => OutlookChangeProposal.FromRequest(request.Merged, before),
        "merge" => throw new DomainException(02053, "逐字段合并必须提交完整日程值。"),
        _ => throw new DomainException(02054, "原始 PIM 拟议值不可用。");
    };
    var operationType = request.Action == "accept-outlook"
        ? "outlook.event.pull-update"
        : "outlook.event.update";
    var action = request.Action == "accept-outlook" ? "pull-update" : "update";
    var hash = OutlookChangeProposal.Hash(proposed);
    var changed = OutlookEventMapper.ChangedFields(before, proposed);
    var payload = new OutlookConfirmedOperationPayload(
        userId, binding.ConnectionId, binding.Id, evt.Id, binding.GraphCalendarId,
        remote.Id, remote.ETag, remote.ChangeKey, hash, action, before, proposed);
    var confirmation = await _confirmations.CreateAsync(new CreateOperationConfirmationRequest(
        userId,
        operationType,
        $"重新处理 Outlook 冲突：{evt.Title}",
        OperationRiskLevel.L3ExternalSourceOrWriteback,
        "outlook",
        JsonSerializer.Serialize(payload, JsonOptions),
        JsonSerializer.Serialize(new { conflictId, before, latest, proposed }, JsonOptions),
        DateTimeOffset.UtcNow.AddHours(1),
        $"outlook-conflict-{conflict.Id}-{remote.ETag}-{hash}",
        changed,
        ["confirm", "reject"],
        "event",
        evt.Id,
        RequiresSecondLevelConfirmation: true,
        BeforeJson: JsonSerializer.Serialize(before, JsonOptions),
        AfterJson: JsonSerializer.Serialize(proposed, JsonOptions),
        ExternalEffect: operationType == "outlook.event.update"
            ? "Microsoft Graph PATCH"
            : "接受 Outlook 最新事实",
        RecoveryPath: "使用最新 ETag 生成新的 L3 确认。"), ct);

    conflict.ExternalSnapshotJson = JsonSerializer.Serialize(latest, JsonOptions);
    conflict.Status = "pending-confirmation";
    conflict.ResolvedConfirmationId = confirmation.Id;
    conflict.UpdatedAt = DateTimeOffset.UtcNow;
    await _db.SaveChangesAsync(ct);
    return confirmation;
}

private async Task<SyncConflictEntity> LoadOutlookConflictAsync(
    Guid userId,
    Guid conflictId,
    CancellationToken ct)
    => await _db.Set<SyncConflictEntity>()
        .SingleOrDefaultAsync(item => item.Id == conflictId
            && item.UserId == userId
            && item.Provider == "outlook", ct)
        ?? throw new DomainException(02039, "Outlook 同步冲突不存在。");

private async Task<OutlookConflictResponse> MapOutlookAsync(
    SyncConflictEntity conflict,
    Guid userId,
    CancellationToken ct)
{
    var bindingId = await _db.Set<EventEntity>().IgnoreQueryFilters()
        .Where(item => item.Id == conflict.ObjectId && item.Calendar.UserId == userId)
        .Select(item => item.OutlookCalendarBindingId)
        .SingleOrDefaultAsync(ct);
    var canEdit = bindingId is { } id
        && await _db.Set<OutlookCalendarBindingEntity>()
            .Where(item => item.Id == id)
            .Select(item => item.CanEdit)
            .SingleOrDefaultAsync(ct);
    return new OutlookConflictResponse(
        conflict.Id,
        conflict.ObjectId,
        conflict.GraphEventId ?? string.Empty,
        conflict.ConflictKind,
        conflict.Status,
        conflict.PimSnapshotJson,
        conflict.ExternalSnapshotJson,
        conflict.SourceConfirmationId,
        conflict.ResolvedConfirmationId,
        canEdit);
}
```

Add the required fields/usings if absent:

```csharp
private readonly IGraphCalendarClient? _graph;
private readonly OutlookEventMapper _mapper;
// using Pim.Module.Calendar.DTOs;
```

- [ ] **Step 6: 映射完整 conflict facade 和 endpoints**

Add to `IOutlookSyncFacade`:

```csharp
Task<IReadOnlyList<OutlookConflictResponse>> ListConflictsAsync(Guid userId, CancellationToken ct);
Task<OutlookConflictResponse> GetConflictAsync(Guid userId, Guid conflictId, CancellationToken ct);
Task<OperationConfirmationDto?> ResolveConflictAsync(
    Guid userId, Guid conflictId, ResolveOutlookConflictRequest request, CancellationToken ct);
```

Add `OutlookConflictService conflicts` to the `OutlookSyncFacade` constructor, assign it to `_conflicts`, and add:

```csharp
public Task<IReadOnlyList<OutlookConflictResponse>> ListConflictsAsync(Guid userId, CancellationToken ct)
    => _conflicts.ListOutlookAsync(userId, ct);

public Task<OutlookConflictResponse> GetConflictAsync(Guid userId, Guid conflictId, CancellationToken ct)
    => _conflicts.GetOutlookAsync(userId, conflictId, ct);

public Task<OperationConfirmationDto?> ResolveConflictAsync(
    Guid userId,
    Guid conflictId,
    ResolveOutlookConflictRequest request,
    CancellationToken ct)
    => _conflicts.ResolveOutlookAsync(userId, conflictId, request, ct);
```

Add these concrete mappings before `return group;` in `OutlookEndpoints.MapOutlookEndpoints`:

```csharp
group.MapGet("/outlook/conflicts", async (
    IOutlookSyncFacade facade, ICurrentUserService current, CancellationToken ct)
    => Results.Ok(ApiResponse<IReadOnlyList<OutlookConflictResponse>>.Ok(
        await facade.ListConflictsAsync(User(current), ct))));
group.MapGet("/outlook/conflicts/{id:guid}", async (
    Guid id, IOutlookSyncFacade facade, ICurrentUserService current, CancellationToken ct)
    => Results.Ok(ApiResponse<OutlookConflictResponse>.Ok(
        await facade.GetConflictAsync(User(current), id, ct))));
group.MapPost("/outlook/conflicts/{id:guid}/actions", async (
    Guid id,
    [FromBody] ResolveOutlookConflictRequest request,
    IOutlookSyncFacade facade,
    ICurrentUserService current,
    CancellationToken ct)
    => Results.Ok(ApiResponse<OperationConfirmationDto?>.Ok(
        await facade.ResolveConflictAsync(User(current), id, request, ct))));
```

Extend `RecordingOutlookFacade` in `OutlookApiContractTests` so the new interface remains complete:

```csharp
public Task<IReadOnlyList<OutlookConflictResponse>> ListConflictsAsync(Guid userId, CancellationToken ct)
    => Seen<IReadOnlyList<OutlookConflictResponse>>(userId, []);

public Task<OutlookConflictResponse> GetConflictAsync(Guid userId, Guid conflictId, CancellationToken ct)
    => Seen(userId, new OutlookConflictResponse(
        conflictId, Guid.NewGuid(), "graph-event", "etag-conflict", "open",
        "{}", "{}", Guid.NewGuid(), null, true));

public Task<OperationConfirmationDto?> ResolveConflictAsync(
    Guid userId, Guid conflictId, ResolveOutlookConflictRequest request, CancellationToken ct)
    => Seen<OperationConfirmationDto?>(userId, null);
```

Add `GET /api/v1/calendar/outlook/conflicts` to `OutlookEndpoints_PassAuthenticatedUserToFacade`. The production service and all three API routes now filter with the authenticated PIM user; `defer` returns `data: null` and does not change the row.

- [ ] **Step 7: 接入 Web 冲突路径、四种动作和完整合并表单**

Append path assertions to `outlookSyncApiPath.test.ts`:

```ts
assert.equal(calendarApiPaths.outlookConflicts(), '/calendar/outlook/conflicts');
assert.equal(calendarApiPaths.outlookConflict('a/b'), '/calendar/outlook/conflicts/a%2Fb');
assert.equal(calendarApiPaths.outlookConflictActions('a/b'), '/calendar/outlook/conflicts/a%2Fb/actions');
```

Add to `src/client-web/src/types/index.ts`:

```ts
export interface OutlookConflict {
  id: string;
  eventId: string;
  graphEventId: string;
  conflictKind: string;
  status: string;
  pimSnapshotJson: string;
  externalSnapshotJson: string;
  sourceConfirmationId?: string | null;
  resolvedConfirmationId?: string | null;
  canEdit: boolean;
}
```

Add the paths and API functions to `calendar.ts`:

```ts
outlookConflicts: () => '/calendar/outlook/conflicts',
outlookConflict: (id: string) => `/calendar/outlook/conflicts/${encodeURIComponent(id)}`,
outlookConflictActions: (id: string) => `/calendar/outlook/conflicts/${encodeURIComponent(id)}/actions`,

export async function listOutlookConflicts() {
  const response = await apiGet<ApiResponse<OutlookConflict[]>>(calendarApiPaths.outlookConflicts());
  return response.data;
}

export async function getOutlookConflict(id: string) {
  const response = await apiGet<ApiResponse<OutlookConflict>>(calendarApiPaths.outlookConflict(id));
  return response.data;
}

export async function resolveOutlookConflict(
  id: string,
  action: 'keep-pim' | 'accept-outlook' | 'merge' | 'defer',
  merged?: OutlookEventChangeRequest | null,
) {
  const response = await apiPost<ApiResponse<OperationConfirmation | null>>(
    calendarApiPaths.outlookConflictActions(id), { action, merged: merged ?? null });
  return response.data;
}
```

Import `OutlookConflict` and `OperationConfirmation` from `../types`. Replace `OutlookConflictResolver.tsx` with:

```tsx
import { useState } from 'react';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { Clock3, Download, GitMerge, Upload } from 'lucide-react';
import {
  resolveOutlookConflict,
  type OutlookEventChangeRequest,
} from '../../api/calendar';
import { confirmOperationSecondLevel } from '../../api/operations';
import type { OperationConfirmation, OutlookConflict } from '../../types';
import { fromShanghaiInputToUtc, toShanghaiInputValue } from '../../utils/calendarTime';
import OutlookWritebackDialog from '../outlook/OutlookWritebackDialog';

interface Props {
  conflicts: OutlookConflict[];
  onChanged: () => void;
}

interface MergeDraft {
  title: string;
  description: string;
  location: string;
  dtStart: string;
  dtEnd: string;
  isAllDay: boolean;
  allDayStartDate: string;
  allDayEndDateExclusive: string;
}

export default function OutlookConflictResolver({ conflicts, onChanged }: Props) {
  const queryClient = useQueryClient();
  const [confirmation, setConfirmation] = useState<OperationConfirmation | null>(null);
  const [mergeConflict, setMergeConflict] = useState<OutlookConflict | null>(null);
  const [draft, setDraft] = useState<MergeDraft | null>(null);
  const resolveMutation = useMutation({
    mutationFn: (input: {
      id: string;
      action: 'keep-pim' | 'accept-outlook' | 'merge' | 'defer';
      merged?: OutlookEventChangeRequest | null;
    }) => resolveOutlookConflict(input.id, input.action, input.merged),
    onSuccess: result => {
      setConfirmation(result);
      setMergeConflict(null);
      setDraft(null);
      onChanged();
    },
  });
  const confirmMutation = useMutation({
    mutationFn: confirmOperationSecondLevel,
    onSuccess: () => {
      setConfirmation(null);
      queryClient.invalidateQueries({ queryKey: ['pending-confirmations'] });
      queryClient.invalidateQueries({ queryKey: ['events'] });
      onChanged();
    },
  });

  const resolve = (
    conflict: OutlookConflict,
    action: 'keep-pim' | 'accept-outlook' | 'defer',
  ) => resolveMutation.mutate({ id: conflict.id, action });

  const openMerge = (conflict: OutlookConflict) => {
    setMergeConflict(conflict);
    setDraft(toDraft(conflict.pimSnapshotJson));
  };

  return (
    <section className="border-t border-slate-200 py-5" aria-label="Outlook 冲突队列">
      <div className="flex flex-wrap items-center justify-between gap-2">
        <h2 className="text-base font-semibold text-slate-950">冲突队列</h2>
        <span className="text-sm text-amber-700">{conflicts.length} 个待处理冲突</span>
      </div>
      <div className="mt-3 divide-y divide-slate-200 border-y border-slate-200">
        {conflicts.map(conflict => (
          <article key={conflict.id} className="py-4">
            <div className="flex flex-wrap items-center justify-between gap-2">
              <p className="text-sm font-semibold text-slate-900">{conflict.conflictKind}</p>
              <span className="text-xs text-slate-500">{conflict.status}</span>
            </div>
            <div className="mt-3 flex flex-wrap gap-2">
              <button type="button" disabled={!conflict.canEdit || resolveMutation.isPending}
                onClick={() => resolve(conflict, 'keep-pim')}
                className="pim-button-primary inline-flex items-center gap-2 px-3 py-2 text-sm disabled:opacity-50">
                <Upload aria-hidden="true" className="h-4 w-4" /> 保留 PIM 拟议值
              </button>
              <button type="button" disabled={resolveMutation.isPending}
                onClick={() => resolve(conflict, 'accept-outlook')}
                className="pim-button-secondary inline-flex items-center gap-2 px-3 py-2 text-sm">
                <Download aria-hidden="true" className="h-4 w-4" /> 采用 Outlook 最新值
              </button>
              <button type="button" disabled={!conflict.canEdit || resolveMutation.isPending}
                onClick={() => openMerge(conflict)}
                className="pim-button-secondary inline-flex items-center gap-2 px-3 py-2 text-sm disabled:opacity-50">
                <GitMerge aria-hidden="true" className="h-4 w-4" /> 逐字段合并
              </button>
              <button type="button" disabled={resolveMutation.isPending}
                onClick={() => resolve(conflict, 'defer')}
                className="pim-button-secondary inline-flex items-center gap-2 px-3 py-2 text-sm">
                <Clock3 aria-hidden="true" className="h-4 w-4" /> 暂不处理
              </button>
            </div>
          </article>
        ))}
        {conflicts.length === 0 && <p className="py-6 text-center text-sm text-slate-500">暂无 Outlook 冲突。</p>}
      </div>

      {mergeConflict && draft && (
        <form className="mt-4 border-t border-slate-200 pt-4" onSubmit={event => {
          event.preventDefault();
          resolveMutation.mutate({ id: mergeConflict.id, action: 'merge', merged: toRequest(draft) });
        }}>
          <h3 className="text-sm font-semibold text-slate-900">逐字段合并</h3>
          <p className="mt-1 text-xs text-slate-500">Outlook 最新快照：{summary(mergeConflict.externalSnapshotJson)}</p>
          <div className="mt-3 grid gap-3 sm:grid-cols-2">
            <label className="text-sm">标题<input required value={draft.title}
              onChange={event => setDraft({ ...draft, title: event.target.value })}
              className="mt-1 w-full rounded-lg border border-slate-300 px-3 py-2" /></label>
            <label className="text-sm">地点<input value={draft.location}
              onChange={event => setDraft({ ...draft, location: event.target.value })}
              className="mt-1 w-full rounded-lg border border-slate-300 px-3 py-2" /></label>
            <label className="text-sm sm:col-span-2">说明<textarea value={draft.description}
              onChange={event => setDraft({ ...draft, description: event.target.value })}
              className="mt-1 min-h-20 w-full rounded-lg border border-slate-300 px-3 py-2" /></label>
            <label className="flex items-center gap-2 text-sm sm:col-span-2">
              <input type="checkbox" checked={draft.isAllDay}
                onChange={event => setDraft({ ...draft, isAllDay: event.target.checked })} /> 全天日程
            </label>
            {draft.isAllDay ? <>
              <label className="text-sm">开始日期<input required type="date" value={draft.allDayStartDate}
                onChange={event => setDraft({ ...draft, allDayStartDate: event.target.value })}
                className="mt-1 w-full rounded-lg border border-slate-300 px-3 py-2" /></label>
              <label className="text-sm">排他结束日期<input required type="date" value={draft.allDayEndDateExclusive}
                onChange={event => setDraft({ ...draft, allDayEndDateExclusive: event.target.value })}
                className="mt-1 w-full rounded-lg border border-slate-300 px-3 py-2" /></label>
            </> : <>
              <label className="text-sm">开始时间<input required type="datetime-local" value={draft.dtStart}
                onChange={event => setDraft({ ...draft, dtStart: event.target.value })}
                className="mt-1 w-full rounded-lg border border-slate-300 px-3 py-2" /></label>
              <label className="text-sm">结束时间<input required type="datetime-local" value={draft.dtEnd}
                onChange={event => setDraft({ ...draft, dtEnd: event.target.value })}
                className="mt-1 w-full rounded-lg border border-slate-300 px-3 py-2" /></label>
            </>}
          </div>
          <div className="mt-4 flex justify-end gap-2">
            <button type="button" onClick={() => { setMergeConflict(null); setDraft(null); }}
              className="pim-button-secondary px-3 py-2 text-sm">取消</button>
            <button type="submit" disabled={resolveMutation.isPending}
              className="pim-button-primary px-3 py-2 text-sm disabled:opacity-50">生成合并确认</button>
          </div>
        </form>
      )}

      <OutlookWritebackDialog confirmation={confirmation}
        isPending={confirmMutation.isPending}
        onConfirm={id => confirmMutation.mutate(id)}
        onClose={() => setConfirmation(null)} />
    </section>
  );
}

function parseSnapshot(json: string): Partial<OutlookEventChangeRequest> {
  try { return JSON.parse(json) as Partial<OutlookEventChangeRequest>; }
  catch { return {}; }
}

function toDraft(json: string): MergeDraft {
  const value = parseSnapshot(json);
  return {
    title: value.title ?? '',
    description: value.description ?? '',
    location: value.location ?? '',
    dtStart: value.dtStart ? toShanghaiInputValue(value.dtStart) : '',
    dtEnd: value.dtEnd ? toShanghaiInputValue(value.dtEnd) : '',
    isAllDay: value.isAllDay ?? false,
    allDayStartDate: value.allDayStartDate ?? '',
    allDayEndDateExclusive: value.allDayEndDateExclusive ?? '',
  };
}

function toRequest(draft: MergeDraft): OutlookEventChangeRequest {
  return {
    title: draft.title,
    description: draft.description || null,
    location: draft.location || null,
    dtStart: draft.isAllDay ? null : fromShanghaiInputToUtc(draft.dtStart),
    dtEnd: draft.isAllDay ? null : fromShanghaiInputToUtc(draft.dtEnd),
    isAllDay: draft.isAllDay,
    allDayStartDate: draft.isAllDay ? draft.allDayStartDate : null,
    allDayEndDateExclusive: draft.isAllDay ? draft.allDayEndDateExclusive : null,
  };
}

function summary(json: string) {
  const value = parseSnapshot(json);
  return [value.title, value.location].filter(Boolean).join(' · ') || '远端日程已删除或快照不可用';
}
```

Replace the old data-center conflict query in `SyncPage.tsx` with:

```tsx
const conflictsQuery = useQuery({
  queryKey: ['outlook-conflicts'],
  queryFn: listOutlookConflicts,
  enabled: settingsQuery.data?.status === 'connected',
});

<OutlookConflictResolver
  conflicts={conflictsQuery.data ?? []}
  onChanged={() => queryClient.invalidateQueries({ queryKey: ['outlook-conflicts'] })}
/>
```

Append these assertions to `outlookEventGovernanceUi.test.tsx` so all four commands and read-only gating remain visible in source:

```ts
const conflictResolver = readFileSync('src/client-web/src/components/schedule/OutlookConflictResolver.tsx', 'utf8');
assert.ok(conflictResolver.includes('保留 PIM 拟议值'));
assert.ok(conflictResolver.includes('采用 Outlook 最新值'));
assert.ok(conflictResolver.includes('逐字段合并'));
assert.ok(conflictResolver.includes('暂不处理'));
assert.ok(conflictResolver.includes('!conflict.canEdit'));
```

- [ ] **Step 8: 运行冲突、API、Web 和 handler 测试**

Run:

```powershell
dotnet test tests/Pim.UnitTests/Pim.UnitTests.csproj --filter "FullyQualifiedName~OutlookConflictRecoveryTests|FullyQualifiedName~OutlookConfirmedOperationHandlerTests|FullyQualifiedName~OutlookConflictResolutionTests|FullyQualifiedName~OutlookApiContractTests"
npm --prefix src/client-web run test:outlook-sync
npm --prefix src/client-web run build
npm --prefix src/client-web run lint
```

Expected: PASS；恢复始终读取最新 ETag，原 execution 只能通过 `SourceConfirmationId` 定位，所有回写动作生成新的 L3 confirmation，采用 Outlook 不发 PATCH，只读日历没有回写入口。

- [ ] **Step 9: 提交冲突恢复**

```powershell
git add src/modules/Pim.Module.Calendar tests/Pim.UnitTests/Calendar/OutlookConflictRecoveryTests.cs tests/Pim.UnitTests/Calendar/OutlookApiContractTests.cs src/client-web/src/types/index.ts src/client-web/src/api/calendar.ts src/client-web/src/pages/SyncPage.tsx src/client-web/src/components/schedule/OutlookConflictResolver.tsx tests/client-web
git commit -m "feat: recover outlook etag conflicts"
```

Expected: ETag 冲突有四个明确用户出口，原始确认和解决确认语义分离，不会静默覆盖任一侧。

## Task 23: 完整验证、真实账号验收和交付

**Files:**
- Create: `docs/operations/microsoft-calendar-sync-acceptance.md`
- Modify: source/test files only when a verification finding is directly related to this feature

- [ ] **Step 1: 创建不含凭据的验收记录**

Create `docs/operations/microsoft-calendar-sync-acceptance.md`:

```markdown
# Microsoft Calendar Sync Acceptance

## Automated Evidence

- [ ] `dotnet test Pim.sln`
- [ ] `npm --prefix src/client-web run build`
- [ ] `npm --prefix src/client-web run lint`
- [ ] `npm --prefix src/client-web run test:outlook-sync`
- [ ] Fresh PostgreSQL migration
- [ ] Upgrade PostgreSQL migration from the previous release

## Real Account Evidence

Account category: personal / organization
Acceptance date:
Tester:

- [ ] The Entra app was registered using only the in-app guide.
- [ ] Public client flow and delegated `Calendars.ReadWrite` plus `User.Read` were accepted.
- [ ] Device authorization completed without a manual “finish connection” action.
- [ ] Default, grouped course, and ungrouped calendars were discovered.
- [ ] Timed, all-day, single, master, and occurrence events match Outlook in `Asia/Shanghai`.
- [ ] Silent acquisition succeeds after access-token expiry.
- [ ] Startup, five-minute, manual incremental, full-resources, and range-instances runs complete.
- [ ] An Outlook edit leaves both sides unchanged before L3 confirmation, then updates both sides and audit after confirmation.
- [ ] A read-only calendar offers copy-to-PIM and no Graph write/delete action.
- [ ] Simulated 429, 30-second timeout, ETag conflict, and reauthorization each show a recoverable state.
- [ ] Legacy Outlook events remain visible without title/time similarity merges or duplicate imports.

## Notes

Do not record Client IDs tied to private tenants, user codes, tokens, cache bytes, event bodies, or account addresses in this file.
```

- [ ] **Step 2: 运行 focused backend suite**

Run:

```powershell
dotnet test tests/Pim.UnitTests/Pim.UnitTests.csproj --filter "FullyQualifiedName~Outlook|FullyQualifiedName~ConfirmedOperationOutboxTests|FullyQualifiedName~ScheduleFactConfirmationGateTests"
```

Expected: PASS；记录测试数。任何失败先按 `superpowers:systematic-debugging` 定位根因。

- [ ] **Step 3: 运行全部后端 suite**

Run:

```powershell
dotnet test Pim.sln
```

Expected: PASS；没有旧 Outlook 类型的编译引用，也没有 unrelated regression。

- [ ] **Step 4: 运行 Web 完整门禁**

Run:

```powershell
npm --prefix src/client-web run test:outlook-sync
npm --prefix src/client-web run test:schedule-workbench
npm --prefix src/client-web run build
npm --prefix src/client-web run lint
```

Expected: PASS；360px、桌面、键盘焦点、二级确认和两种深度同步交互均有证据。

- [ ] **Step 5: 检查敏感数据边界**

Run:

```powershell
rg -n "Logger.*(AccessToken|RefreshToken|MsalCache|UserCode)|Log.*(AccessToken|RefreshToken|MsalCache|UserCode)" src/modules/Pim.Module.Calendar
rg -n "DeviceCode|RefreshToken|MsalCacheEncrypted|AccessTokenEncrypted" src/modules/Pim.Module.Calendar/DTOs src/client-web/src/types src/client-web/src/api
```

Expected: no output。实体、内部 adapter 和 migration 可以包含这些标识；API DTO、Web 类型和日志调用不能包含敏感值。

- [ ] **Step 6: 验证 fresh PostgreSQL migration**

Run in PowerShell with Docker available:

```powershell
$name = 'pim-ms-sync-postgres'
docker run --rm -d --name $name -e POSTGRES_PASSWORD=pim -e POSTGRES_USER=pim -e POSTGRES_DB=pim -p 55432:5432 postgres:16-alpine
do { Start-Sleep -Seconds 1; $ready = docker exec $name pg_isready -U pim } until ($LASTEXITCODE -eq 0)
$env:ConnectionStrings__DefaultConnection = 'Host=127.0.0.1;Port=55432;Database=pim;Username=pim;Password=pim'
dotnet ef database update --project src/Pim.Infrastructure --startup-project src/Pim.Api
docker exec $name psql -U pim -d pim -c "select count(*) from outlook_calendar_bindings;"
docker stop $name
Remove-Item Env:ConnectionStrings__DefaultConnection
```

Expected: migration update exits 0; table query returns count 0; container stops cleanly.

- [ ] **Step 7: 验证 upgrade migration**

Run this complete upgrade fixture in PowerShell:

```powershell
$name = 'pim-ms-sync-upgrade-postgres'
$env:ConnectionStrings__DefaultConnection = 'Host=127.0.0.1;Port=55433;Database=pim;Username=pim;Password=pim'
try {
    docker run --rm -d --name $name -e POSTGRES_PASSWORD=pim -e POSTGRES_USER=pim -e POSTGRES_DB=pim -p 55433:5432 postgres:16-alpine
    do { Start-Sleep -Seconds 1; docker exec $name pg_isready -U pim | Out-Null } until ($LASTEXITCODE -eq 0)
    dotnet ef database update 20260708094627_AddEndpointStatusPersistence --project src/Pim.Infrastructure --startup-project src/Pim.Api
    if ($LASTEXITCODE -ne 0) { throw 'baseline migration failed' }

    @'
INSERT INTO calendars
    (id, user_id, name, color, kind, is_default, created_at, updated_at)
VALUES
    ('22222222-2222-4222-8222-222222222222', '11111111-1111-4111-8111-111111111111',
     'Legacy Outlook', '#2563EB', 'calendar', false, now(), now());

INSERT INTO outlook_connections
    (id, user_id, access_token_encrypted, refresh_token_encrypted,
     client_id, tenant_id, scopes, status, token_health, created_at, updated_at)
VALUES
    ('33333333-3333-4333-8333-333333333333', '11111111-1111-4111-8111-111111111111',
     decode('01', 'hex'), decode('02', 'hex'), '44444444-4444-4444-8444-444444444444',
     'common', 'Calendars.ReadWrite User.Read', 'connected', 'healthy', now(), now());

INSERT INTO events
    (id, calendar_id, uid, source_uid, title, dtstart, dtend, dtstamp,
     status, source, outlook_event_id, created_at, updated_at)
VALUES
    ('55555555-5555-4555-8555-555555555555', '22222222-2222-4222-8222-222222222222',
     'legacy-ical-uid', 'legacy-ical-uid', 'Legacy event',
     '2026-07-10T01:00:00Z', '2026-07-10T02:00:00Z', '2026-07-10T00:00:00Z',
     'CONFIRMED', 'outlook', 'legacy-graph-id', now(), now());
'@ | docker exec -i $name psql -v ON_ERROR_STOP=1 -U pim -d pim
    if ($LASTEXITCODE -ne 0) { throw 'legacy fixture seed failed' }

    dotnet ef database update 20260710000000_MicrosoftCalendarSync --project src/Pim.Infrastructure --startup-project src/Pim.Api
    if ($LASTEXITCODE -ne 0) { throw 'Microsoft calendar sync upgrade failed' }
    docker exec $name psql -v ON_ERROR_STOP=1 -U pim -d pim -c "SELECT status, token_health FROM outlook_connections;"
    docker exec $name psql -v ON_ERROR_STOP=1 -U pim -d pim -c "SELECT outlook_sync_state, source_uid FROM events;"
    docker exec $name psql -v ON_ERROR_STOP=1 -U pim -d pim -c "SELECT COUNT(*) FROM outlook_calendar_bindings;"
    docker exec $name psql -v ON_ERROR_STOP=1 -U pim -d pim -c "SELECT COUNT(*) FROM events WHERE id = '55555555-5555-4555-8555-555555555555';"
} finally {
    docker stop $name | Out-Null
    Remove-Item Env:ConnectionStrings__DefaultConnection -ErrorAction SilentlyContinue
}
```

Expected: connection is `reauth-required/interaction-required`; event is `legacy-unbound` with `legacy-ical-uid`; binding count is 0; final event count is exactly 1.

- [ ] **Step 8: 启动本地 API/Web 并做 Playwright 视觉核查**

Run the normal development stack, using port `5858` for API and an available Vite port. Open `/settings/sync` and an Outlook event editor at 360x800, 768x1024, and 1440x1000. Verify there is no horizontal overflow, clipped button text, incoherent overlap, blank state, hidden recovery action, or inaccessible focus order. Store any inspection screenshots only under ignored `.superpowers/brainstorm/`.

Expected: setup, waiting code, calendar selection, active run, diagnostics, read-only event, and L3 confirmation states all render correctly.

- [ ] **Step 9: 执行真实 Microsoft 账号验收**

Use one personal Microsoft account and, when available, one organization/school account. Follow every checkbox in `docs/operations/microsoft-calendar-sync-acceptance.md` without reading source code. Do not put real identifiers or credentials in git.

Expected: every applicable checkbox passes. If no real account is available, leave the checkbox open and report the feature as awaiting real-account acceptance rather than complete.

- [ ] **Step 10: 调用完成前验证技能**

Before claiming completion, invoke `superpowers:verification-before-completion`, rerun the commands it requires, and base the completion statement on fresh output.

- [ ] **Step 11: 检查提交范围**

Run:

```powershell
git status --short --branch
git diff --check
git log --oneline --decorate origin/master..HEAD
git diff --name-only origin/master...HEAD
```

Expected: only Microsoft sync source/tests/docs and dependency lock changes; no `bin/`, `obj/`, `build/`, `dist/`, `wwwroot` artifacts, `.superpowers/brainstorm/`, Android plan, or `.opencode/`.

- [ ] **Step 12: 提交验收记录**

```powershell
git add docs/operations/microsoft-calendar-sync-acceptance.md
git commit -m "docs: record microsoft sync acceptance"
```

Expected: acceptance evidence accurately distinguishes automated pass from real-account pass.

- [ ] **Step 13: 推送、创建 PR 并等待 Actions**

Run:

```powershell
git push -u origin codex/microsoft-calendar-sync
gh pr create --base master --head codex/microsoft-calendar-sync --title "feat: complete Microsoft calendar sync" --body-file docs/operations/microsoft-calendar-sync-acceptance.md
gh pr checks --watch
```

Expected: API and Web workflows trigger because `src/` and test files changed; all required checks pass. Do not modify `.github/workflows/*` for this feature.

- [ ] **Step 14: 请求代码审查并处理 findings**

Invoke `superpowers:requesting-code-review`. Address correctness, security, migration, retry, confirmation, time-zone, and UI findings with focused tests and commits; rerun the full verification and `gh pr checks --watch` after the last push.

Expected: no unresolved high/medium correctness finding and all triggered Actions green.
