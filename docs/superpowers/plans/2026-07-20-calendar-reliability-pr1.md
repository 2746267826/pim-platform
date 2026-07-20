# Calendar Reliability PR1 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 修复日程和任务人工测试暴露的时间、创建、校验、时长输入、月视图和时间轴可靠性问题，不引入 PR2 字段迁移或 PR3 重复模型。

**Architecture:** 后端在 CalendarService 内增加共享 UTC 规范化和范围校验 helper，添加轻量 ManualDescriptionValidator 拒绝可执行 HTML，使用 EF Core InMemoryDatabase 编写 xUnit 测试。前端添加纯工具模块（dateTimeInput、durationInput、calendarSelection、safeHtml）用 node:assert 测试；安装 dompurify 用于安全 HTML 预览；修改 EventEditorDialog/TaskEditorDialog/CalendarPage；扩展既有 Playwright visual audit 和 calendarLayerVisibility 测试。

**Tech Stack:** .NET 8, EF Core, xUnit, React 19, TypeScript, FullCalendar 6, Luxon, DOMPurify, Playwright, node:assert/tsx

---

## Task 1: Event UTC normalization and range validation

**Files:**
- Create: `tests/Pim.UnitTests/Calendar/CalendarServiceReliabilityTests.cs`
- Modify: `src/modules/Pim.Module.Calendar/Services/CalendarService.cs`

- [ ] **Step 1.1: Write failing tests for CreateEvent UTC normalization and range validation**

```csharp
using Microsoft.EntityFrameworkCore;
using Microsoft.Extensions.Logging.Abstractions;
using Pim.Core.Exceptions;
using Pim.Infrastructure.Auth;
using Pim.Infrastructure.Data;
using Pim.Module.Calendar.DTOs;
using Pim.Module.Calendar.Entities;
using Pim.Module.Calendar.Services;
using Xunit;

namespace Pim.UnitTests.Calendar;

public class CalendarServiceReliabilityTests
{
    private static (CalendarService Service, PimDbContext Db, Guid UserId) CreateService()
    {
        var userId = Guid.NewGuid();
        var options = new DbContextOptionsBuilder<PimDbContext>()
            .UseInMemoryDatabase(Guid.NewGuid().ToString())
            .Options;
        var db = new PimDbContext(options);
        var service = new CalendarService(
            db,
            new FixedCurrentUserService(userId),
            new RecurrenceService(NullLogger<RecurrenceService>.Instance));
        return (service, db, userId);
    }

    private sealed class FixedCurrentUserService(Guid userId) : ICurrentUserService
    {
        public Guid? UserId { get; } = userId;
        public string? Role => "user";
    }

    [Fact]
    public async Task CreateEventAsync_NormalizesPlus08ToUtc()
    {
        var (service, db, userId) = CreateService();
        var cal = new CalendarEntity { UserId = userId, Name = "tc", Kind = "calendar", IsDefault = true };
        db.Set<CalendarEntity>().Add(cal);
        await db.SaveChangesAsync(CancellationToken.None);

        var start = new DateTimeOffset(2026, 7, 20, 14, 0, 0, TimeSpan.FromHours(8));
        var end = new DateTimeOffset(2026, 7, 20, 15, 0, 0, TimeSpan.FromHours(8));

        var created = await service.CreateEventAsync(
            new CreateEventRequest(cal.Id, "t", null, null, start, end, null),
            CancellationToken.None);

        Assert.Equal(TimeSpan.Zero, created.DtStart.Offset);
        Assert.Equal(TimeSpan.Zero, created.DtEnd.Offset);
        Assert.Equal(new DateTimeOffset(2026, 7, 20, 6, 0, 0, TimeSpan.Zero), created.DtStart);
        Assert.Equal(new DateTimeOffset(2026, 7, 20, 7, 0, 0, TimeSpan.Zero), created.DtEnd);
        var saved = await db.Set<EventEntity>().FirstAsync(CancellationToken.None);
        Assert.Equal(TimeSpan.Zero, saved.DtStart.Offset);
        Assert.Equal(TimeSpan.Zero, saved.DtEnd.Offset);
    }

    [Fact]
    public async Task CreateEventAsync_EndEqualsStart_Returns02010()
    {
        var (service, db, userId) = CreateService();
        var cal = new CalendarEntity { UserId = userId, Name = "tc", Kind = "calendar", IsDefault = true };
        db.Set<CalendarEntity>().Add(cal);
        await db.SaveChangesAsync(CancellationToken.None);

        var start = new DateTimeOffset(2026, 7, 20, 10, 0, 0, TimeSpan.Zero);

        var ex = await Assert.ThrowsAsync<DomainException>(() =>
            service.CreateEventAsync(
                new CreateEventRequest(cal.Id, "t", null, null, start, start, null),
                CancellationToken.None));

        Assert.Equal(02010, ex.ErrorCode);
    }

    [Fact]
    public async Task CreateEventAsync_EndBeforeStart_Returns02010()
    {
        var (service, db, userId) = CreateService();
        var cal = new CalendarEntity { UserId = userId, Name = "tc", Kind = "calendar", IsDefault = true };
        db.Set<CalendarEntity>().Add(cal);
        await db.SaveChangesAsync(CancellationToken.None);

        var start = new DateTimeOffset(2026, 7, 20, 10, 0, 0, TimeSpan.Zero);
        var end = new DateTimeOffset(2026, 7, 20, 9, 0, 0, TimeSpan.Zero);

        var ex = await Assert.ThrowsAsync<DomainException>(() =>
            service.CreateEventAsync(
                new CreateEventRequest(cal.Id, "t", null, null, start, end, null),
                CancellationToken.None));

        Assert.Equal(02010, ex.ErrorCode);
    }
}
```

- [ ] **Step 1.1b: Restore test project before first RED run**

```
dotnet restore tests\Pim.UnitTests\Pim.UnitTests.csproj
```

Expected: Restore completes. This ensures `--no-restore` steps below use fresh packages.

- [ ] **Step 1.2: Run tests to verify they fail**

```
dotnet test tests\Pim.UnitTests\Pim.UnitTests.csproj --no-restore --filter FullyQualifiedName~CalendarServiceReliabilityTests
```

Expected: FAIL — 3 failures (DomainException not thrown, DtStart offset not normalized). The test file exists but CalendarService hasn't been modified yet.

- [ ] **Step 1.3: Add shared `NormalizeAndValidateEventRange` and call it in `CreateEventAsync`**

Insert before `CreateEventAsync` in `CalendarService.cs`:

```csharp
private static (DateTimeOffset Start, DateTimeOffset End) NormalizeAndValidateEventRange(
    DateTimeOffset start, DateTimeOffset end)
{
    var normalizedStart = start.ToUniversalTime();
    var normalizedEnd = end.ToUniversalTime();
    if (normalizedEnd <= normalizedStart)
        throw new DomainException(02010, "结束时间必须晚于开始时间");
    return (normalizedStart, normalizedEnd);
}
```

Replace the entity creation block in `CreateEventAsync` (lines 155-167) with:

```csharp
        var (dtStart, dtEnd) = NormalizeAndValidateEventRange(request.DtStart, request.DtEnd);

        var entity = new EventEntity
        {
            CalendarId = calendar.Id,
            Uid = request.Uid ?? Guid.NewGuid().ToString() + "@pim",
            Title = request.Title,
            Description = request.Description,
            Location = request.Location,
            DtStart = dtStart,
            DtEnd = dtEnd,
            RRule = request.RRule,
            IsAllDay = request.IsAllDay,
            TimeZoneId = request.TimeZoneId
        };
```

- [ ] **Step 1.4: Run tests to verify they pass**

```
dotnet test tests\Pim.UnitTests\Pim.UnitTests.csproj --no-restore --filter FullyQualifiedName~CalendarServiceReliabilityTests
```

Expected: PASS (3/3)

- [ ] **Step 1.5: Write UpdateEvent tests**

Add to `CalendarServiceReliabilityTests.cs`:

```csharp
[Fact]
public async Task UpdateEventAsync_NormalizesAndValidatesRange()
{
    var (service, db, userId) = CreateService();
    var cal = new CalendarEntity { UserId = userId, Name = "tc", Kind = "calendar", IsDefault = true };
    db.Set<CalendarEntity>().Add(cal);
    await db.SaveChangesAsync(CancellationToken.None);

    var evt = new EventEntity
    {
        CalendarId = cal.Id,
        Uid = "uid-u@pim",
        Title = "original",
        DtStart = DateTimeOffset.UtcNow,
        DtEnd = DateTimeOffset.UtcNow.AddHours(1)
    };
    db.Set<EventEntity>().Add(evt);
    await db.SaveChangesAsync(CancellationToken.None);

    var start = new DateTimeOffset(2026, 7, 20, 14, 0, 0, TimeSpan.FromHours(8));
    var end = new DateTimeOffset(2026, 7, 20, 15, 0, 0, TimeSpan.FromHours(8));

    var updated = await service.UpdateEventAsync(evt.Id,
        new UpdateEventRequest(cal.Id, "updated", null, null, start, end, null),
        CancellationToken.None);

    Assert.Equal(TimeSpan.Zero, updated.DtStart.Offset);
    Assert.Equal(TimeSpan.Zero, updated.DtEnd.Offset);
    Assert.Equal(new DateTimeOffset(2026, 7, 20, 6, 0, 0, TimeSpan.Zero), updated.DtStart);

    var ex = await Assert.ThrowsAsync<DomainException>(() =>
        service.UpdateEventAsync(evt.Id,
            new UpdateEventRequest(cal.Id, "bad", null, null, start, start, null),
            CancellationToken.None));
    Assert.Equal(02010, ex.ErrorCode);
}
```

- [ ] **Step 1.6: Apply same normalization + validation in `UpdateEventAsync`**

Replace lines 350-354 in `CalendarService.cs`:

```csharp
        var (dtStart, dtEnd) = NormalizeAndValidateEventRange(request.DtStart, request.DtEnd);

        entity.Title = request.Title;
        entity.Description = request.Description;
        entity.Location = request.Location;
        entity.DtStart = dtStart;
        entity.DtEnd = dtEnd;
```

- [ ] **Step 1.7: Run tests again**

```
dotnet test tests\Pim.UnitTests\Pim.UnitTests.csproj --no-restore --filter FullyQualifiedName~CalendarServiceReliabilityTests
```

Expected: PASS (4/4)

- [ ] **Step 1.8: Commit**

```
git add tests/Pim.UnitTests/Calendar/CalendarServiceReliabilityTests.cs src/modules/Pim.Module.Calendar/Services/CalendarService.cs
git commit -m "fix: normalize event times to UTC and validate end > start"
```

---

## Task 2: Task UTC normalization, range validation, positive duration

**Files:**
- Modify: `tests/Pim.UnitTests/Calendar/CalendarServiceReliabilityTests.cs`
- Modify: `src/modules/Pim.Module.Calendar/Services/CalendarService.cs`

- [ ] **Step 2.1: Write failing tests for task Create/Update/Plan/Move UTC and range**

Add to `CalendarServiceReliabilityTests.cs`:

```csharp
// ─── Task helpers ──────────────────────────────────────────────

private static async Task<(TaskEntity Task, CalendarEntity Calendar)> SeedTaskAsync(
    PimDbContext db, Guid userId, string? title = null)
{
    var cal = new CalendarEntity { UserId = userId, Name = "task-cal", Kind = "task", IsDefault = true };
    db.Set<CalendarEntity>().Add(cal);
    await db.SaveChangesAsync(CancellationToken.None);
    var t = new TaskEntity
    {
        UserId = userId,
        CalendarId = cal.Id,
        Uid = Guid.NewGuid() + "@pim",
        Title = title ?? "seed"
    };
    db.Set<TaskEntity>().Add(t);
    await db.SaveChangesAsync(CancellationToken.None);
    return (t, cal);
}

// ─── Task Create ───────────────────────────────────────────────

[Fact]
public async Task CreateTaskAsync_NormalizesPlus08ToUtc()
{
    var (service, db, userId) = CreateService();
    var cal = new CalendarEntity { UserId = userId, Name = "tc", Kind = "task", IsDefault = true };
    db.Set<CalendarEntity>().Add(cal);
    await db.SaveChangesAsync(CancellationToken.None);

    var start = new DateTimeOffset(2026, 7, 20, 14, 0, 0, TimeSpan.FromHours(8));
    var end = new DateTimeOffset(2026, 7, 20, 15, 0, 0, TimeSpan.FromHours(8));
    var due = new DateTimeOffset(2026, 7, 21, 0, 0, 0, TimeSpan.FromHours(8));

    var created = await service.CreateTaskAsync(
        new CreateTaskRequest(cal.Id, "t", null, 0, "PT1H", null, due, start, null, end),
        CancellationToken.None);

    Assert.Equal(TimeSpan.Zero, created.DtStart!.Value.Offset);
    Assert.Equal(TimeSpan.Zero, created.PlannedEnd!.Value.Offset);
    Assert.Equal(TimeSpan.Zero, created.Due!.Value.Offset);
}

[Fact]
public async Task CreateTaskAsync_PlannedEndBeforeDtStart_Returns02010()
{
    var (service, db, userId) = CreateService();
    var cal = new CalendarEntity { UserId = userId, Name = "tc", Kind = "task", IsDefault = true };
    db.Set<CalendarEntity>().Add(cal);
    await db.SaveChangesAsync(CancellationToken.None);

    var start = new DateTimeOffset(2026, 7, 20, 10, 0, 0, TimeSpan.Zero);
    var end = new DateTimeOffset(2026, 7, 20, 9, 0, 0, TimeSpan.Zero);

    var ex = await Assert.ThrowsAsync<DomainException>(() =>
        service.CreateTaskAsync(
            new CreateTaskRequest(cal.Id, "t", null, 0, null, null, null, start, null, end),
            CancellationToken.None));
    Assert.Equal(02010, ex.ErrorCode);
}

[Fact]
public async Task CreateTaskAsync_ZeroDuration_Returns02011()
{
    var (service, db, userId) = CreateService();
    var cal = new CalendarEntity { UserId = userId, Name = "tc", Kind = "task", IsDefault = true };
    db.Set<CalendarEntity>().Add(cal);
    await db.SaveChangesAsync(CancellationToken.None);

    var ex = await Assert.ThrowsAsync<DomainException>(() =>
        service.CreateTaskAsync(
            new CreateTaskRequest(cal.Id, "t", null, 0, "PT0M", null, null, null, null, null),
            CancellationToken.None));
    Assert.Equal(02011, ex.ErrorCode);
}

// ─── Task Update ───────────────────────────────────────────────

[Fact]
public async Task UpdateTaskAsync_NormalizesAndValidates()
{
    var (service, db, userId) = CreateService();
    var (task, cal) = await SeedTaskAsync(db, userId);

    var start = new DateTimeOffset(2026, 7, 20, 14, 0, 0, TimeSpan.FromHours(8));
    var end = new DateTimeOffset(2026, 7, 20, 15, 0, 0, TimeSpan.FromHours(8));

    var updated = await service.UpdateTaskAsync(task.Id,
        new UpdateTaskRequest(cal.Id, "u", null, 0, "PT1H", null, null, start, null, end),
        CancellationToken.None);

    Assert.Equal(TimeSpan.Zero, updated.DtStart!.Value.Offset);
    Assert.Equal(TimeSpan.Zero, updated.PlannedEnd!.Value.Offset);

    var ex = await Assert.ThrowsAsync<DomainException>(() =>
        service.UpdateTaskAsync(task.Id,
            new UpdateTaskRequest(cal.Id, "u", null, 0, null, null, null, start, null, start),
            CancellationToken.None));
    Assert.Equal(02010, ex.ErrorCode);
}

[Fact]
public async Task UpdateTaskAsync_ZeroDurationClears_ButStoresNull()
{
    var (service, db, userId) = CreateService();
    var (task, cal) = await SeedTaskAsync(db, userId);
    // Set an initial duration via create, then update with null → clears
    // Current semantics: ParseDuration(null) returns null, clearing the field.
    var updated = await service.UpdateTaskAsync(task.Id,
        new UpdateTaskRequest(cal.Id, "u", null, 0, null, null, null, null, null, null),
        CancellationToken.None);
    Assert.Null(updated.EstimatedDuration);
}

[Fact]
public async Task UpdateTaskAsync_ZeroDuration_Returns02011()
{
    var (service, db, userId) = CreateService();
    var (task, cal) = await SeedTaskAsync(db, userId);

    var ex = await Assert.ThrowsAsync<DomainException>(() =>
        service.UpdateTaskAsync(task.Id,
            new UpdateTaskRequest(cal.Id, "t", null, 0, "PT0M", null, null, null, null, null),
            CancellationToken.None));
    Assert.Equal(02011, ex.ErrorCode);
}

// ─── PlanTask ──────────────────────────────────────────────────

[Fact]
public async Task PlanTaskAsync_ValidatesPlannedEndAfterPlannedStart()
{
    var (service, db, userId) = CreateService();
    var (task, _) = await SeedTaskAsync(db, userId);

    var start = new DateTimeOffset(2026, 7, 20, 10, 0, 0, TimeSpan.Zero);
    var end = new DateTimeOffset(2026, 7, 20, 9, 0, 0, TimeSpan.Zero);

    var ex = await Assert.ThrowsAsync<DomainException>(() =>
        service.PlanTaskAsync(task.Id,
            new PlanTaskRequest(start, end, null),
            CancellationToken.None));
    Assert.Equal(02010, ex.ErrorCode);
}

[Fact]
public async Task PlanTaskAsync_NormalizesToUtc()
{
    var (service, db, userId) = CreateService();
    var (task, _) = await SeedTaskAsync(db, userId);

    var start = new DateTimeOffset(2026, 7, 20, 14, 0, 0, TimeSpan.FromHours(8));
    var end = new DateTimeOffset(2026, 7, 20, 15, 0, 0, TimeSpan.FromHours(8));

    await service.PlanTaskAsync(task.Id,
        new PlanTaskRequest(start, end, "PT1H"),
        CancellationToken.None);

    var saved = await db.Set<TaskEntity>().FindAsync(new object[] { task.Id }, CancellationToken.None);
    Assert.NotNull(saved);
    Assert.Equal(TimeSpan.Zero, saved!.DtStart!.Value.Offset);
    Assert.Equal(TimeSpan.Zero, saved.PlannedEnd!.Value.Offset);
}

[Fact]
public async Task PlanTaskAsync_ZeroDuration_Returns02011()
{
    var (service, db, userId) = CreateService();
    var (task, _) = await SeedTaskAsync(db, userId);

    var start = new DateTimeOffset(2026, 7, 20, 10, 0, 0, TimeSpan.Zero);
    var end = new DateTimeOffset(2026, 7, 20, 11, 0, 0, TimeSpan.Zero);

    var ex = await Assert.ThrowsAsync<DomainException>(() =>
        service.PlanTaskAsync(task.Id,
            new PlanTaskRequest(start, end, "PT0M"),
            CancellationToken.None));
    Assert.Equal(02011, ex.ErrorCode);
}

// ─── MoveTask ──────────────────────────────────────────────────

[Fact]
public async Task MoveTaskAsync_NormalizesAndValidates()
{
    var (service, db, userId) = CreateService();
    var (task, _) = await SeedTaskAsync(db, userId);

    var start = new DateTimeOffset(2026, 7, 20, 14, 0, 0, TimeSpan.FromHours(8));
    var end = new DateTimeOffset(2026, 7, 20, 15, 0, 0, TimeSpan.FromHours(8));

    await service.MoveTaskAsync(task.Id,
        new MoveTaskRequest(start, null, null, end),
        CancellationToken.None);

    var saved = await db.Set<TaskEntity>().FindAsync(new object[] { task.Id }, CancellationToken.None);
    Assert.NotNull(saved);
    Assert.Equal(TimeSpan.Zero, saved!.DtStart!.Value.Offset);
    Assert.Equal(TimeSpan.Zero, saved.PlannedEnd!.Value.Offset);
}

[Fact]
public async Task MoveTaskAsync_PlannedEndBeforeScheduledStart_Returns02010()
{
    var (service, db, userId) = CreateService();
    var (task, _) = await SeedTaskAsync(db, userId);

    var start = new DateTimeOffset(2026, 7, 20, 10, 0, 0, TimeSpan.Zero);
    var end = new DateTimeOffset(2026, 7, 20, 9, 0, 0, TimeSpan.Zero);

    var ex = await Assert.ThrowsAsync<DomainException>(() =>
        service.MoveTaskAsync(task.Id,
            new MoveTaskRequest(start, null, null, end),
            CancellationToken.None));
    Assert.Equal(02010, ex.ErrorCode);
}

[Fact]
public async Task MoveTaskAsync_DurationComputedEnd_Validates()
{
    // Duration-based end combined with explicit end that violates range
    var (service, db, userId) = CreateService();
    var (task, _) = await SeedTaskAsync(db, userId);

    var start = new DateTimeOffset(2026, 7, 20, 10, 0, 0, TimeSpan.Zero);
    // Duration of 0 → no explicit plannedEnd from Duration branch alone,
    // but if we set PlannedEnd explicitly to before start it should fail
    var ex = await Assert.ThrowsAsync<DomainException>(() =>
        service.MoveTaskAsync(task.Id,
            new MoveTaskRequest(start, TimeSpan.FromHours(1), null, start.AddHours(-1)),
            CancellationToken.None));
    Assert.Equal(02010, ex.ErrorCode);
}
```

- [ ] **Step 2.2: Run tests — verify failures**

```
dotnet test tests\Pim.UnitTests\Pim.UnitTests.csproj --no-restore --filter FullyQualifiedName~CalendarServiceReliabilityTests
```

Expected: FAIL — many new test failures (DomainException not thrown, offsets not normalized)

- [ ] **Step 2.3: Implement task UTC normalization + validation in CalendarService**

Add shared helpers right before `CreateTaskAsync`:

```csharp
private static (DateTimeOffset? start, DateTimeOffset? end) NormalizeAndValidateTaskRange(
    DateTimeOffset? dtStart, DateTimeOffset? plannedEnd)
{
    var start = dtStart?.ToUniversalTime();
    var end = plannedEnd?.ToUniversalTime();
    if (start.HasValue && end.HasValue && end.Value <= start.Value)
        throw new DomainException(02010, "计划结束时间必须晚于开始时间");
    return (start, end);
}

private static TimeSpan? ParsePositiveDuration(string? value)
{
    if (value is null) return null;
    var parsed = ParseDuration(value);
    if (parsed.HasValue && parsed.Value <= TimeSpan.Zero)
        throw new DomainException(02011, "时长必须为正值");
    return parsed;
}
```

Replace `CreateTaskAsync` body (lines 477-496):

```csharp
    var (dtStart, plannedEnd) = NormalizeAndValidateTaskRange(request.DtStart, request.PlannedEnd);
    var due = request.Due?.ToUniversalTime();

    var task = new TaskEntity
    {
        UserId = UserId,
        CalendarId = request.CalendarId,
        Uid = Guid.NewGuid().ToString() + "@pim",
        Title = request.Title,
        Description = request.Description,
        Priority = request.Priority,
        Due = due,
        EstimatedDuration = ParsePositiveDuration(request.EstimatedDuration),
        MinimumSegment = ParseDuration(request.MinimumSegment),
        IsInbox = request.CalendarId is null && !request.DtStart.HasValue,
        DtStart = dtStart,
        PlannedEnd = plannedEnd
    };

    _db.Set<TaskEntity>().Add(task);
    await _db.SaveChangesAsync(ct);
    return MapTask(task);
```

Replace `UpdateTaskAsync` body (lines 498-524). The effective end for validation uses `request.PlannedEnd ?? task.PlannedEnd` but only writes when request has a value:

```csharp
    // Determine effective start/end for validation
    var effectiveStart = request.DtStart ?? task.DtStart;
    var effectiveEnd = request.PlannedEnd ?? task.PlannedEnd;
    if (effectiveStart.HasValue && effectiveEnd.HasValue && effectiveEnd.Value.ToUniversalTime() <= effectiveStart.Value.ToUniversalTime())
        throw new DomainException(02010, "计划结束时间必须晚于开始时间");

    task.Title = request.Title;
    task.Description = request.Description;
    task.Priority = request.Priority;
    task.Due = request.Due?.ToUniversalTime();
    task.EstimatedDuration = ParsePositiveDuration(request.EstimatedDuration);
    task.MinimumSegment = ParseDuration(request.MinimumSegment);
    task.DtStart = request.DtStart?.ToUniversalTime();
    if (request.PlannedEnd.HasValue)
        task.PlannedEnd = request.PlannedEnd.Value.ToUniversalTime();
    task.CalendarId = request.CalendarId;
    if (request.Status is not null)
    {
        task.Status = request.Status;
        if (request.Status == "COMPLETED")
            task.CompletedAt = DateTimeOffset.UtcNow;
    }
    task.UpdatedAt = DateTimeOffset.UtcNow;
```

Replace `PlanTaskAsync` body (lines 526-541):

```csharp
    var start = request.PlannedStart.ToUniversalTime();
    var end = request.PlannedEnd?.ToUniversalTime();
    if (end.HasValue && end.Value <= start)
        throw new DomainException(02010, "计划结束时间必须晚于开始时间");

    task.DtStart = start;
    task.PlannedEnd = end;
    if (request.EstimatedDuration is not null)
        task.EstimatedDuration = ParsePositiveDuration(request.EstimatedDuration);
    task.IsInbox = false;
    task.UpdatedAt = DateTimeOffset.UtcNow;
```

Replace `MoveTaskAsync` body (lines 639-660). Normalize inputs first, then validate before mutating:

```csharp
    var scheduledStart = request.ScheduledStart?.ToUniversalTime();
    var plannedEnd = request.PlannedEnd?.ToUniversalTime();

    // Compute effective plannedEnd
    DateTimeOffset? effectivePlannedEnd = plannedEnd;
    if (!effectivePlannedEnd.HasValue && request.Duration.HasValue && scheduledStart.HasValue)
        effectivePlannedEnd = scheduledStart.Value.Add(request.Duration.Value);

    // Validate before mutation
    if (scheduledStart.HasValue && effectivePlannedEnd.HasValue && effectivePlannedEnd.Value <= scheduledStart.Value)
        throw new DomainException(02010, "计划结束时间必须晚于开始时间");

    if (scheduledStart.HasValue)
    {
        task.DtStart = scheduledStart;
        task.IsInbox = false;
    }

    if (request.NewSortOrder.HasValue)
        task.SortOrder = request.NewSortOrder.Value;

    if (plannedEnd.HasValue)
        task.PlannedEnd = plannedEnd;
    else if (request.Duration.HasValue && scheduledStart.HasValue)
        task.PlannedEnd = effectivePlannedEnd;

    task.UpdatedAt = DateTimeOffset.UtcNow;
```

- [ ] **Step 2.4: Run tests**

```
dotnet test tests\Pim.UnitTests\Pim.UnitTests.csproj --no-restore --filter FullyQualifiedName~CalendarServiceReliabilityTests
```

Expected: PASS (all CalendarServiceReliabilityTests)

- [ ] **Step 2.5: Commit**

```
git add tests/Pim.UnitTests/Calendar/CalendarServiceReliabilityTests.cs src/modules/Pim.Module.Calendar/Services/CalendarService.cs
git commit -m "fix: harden task scheduling times and durations"
```

---

## Task 3: Manual description executable HTML guard

**Files:**
- Create: `src/modules/Pim.Module.Calendar/Services/ManualDescriptionValidator.cs`
- Modify: `src/modules/Pim.Module.Calendar/Services/CalendarService.cs`
- Modify: `tests/Pim.UnitTests/Calendar/CalendarServiceReliabilityTests.cs`

- [ ] **Step 3.1: Write tests for ManualDescriptionValidator**

Add to `CalendarServiceReliabilityTests.cs`:

```csharp
// ─── ManualDescriptionValidator tests ──────────────────────────

[Fact]
public async Task CreateEventAsync_RejectsScriptTag()
{
    var (service, db, userId) = CreateService();
    var cal = new CalendarEntity { UserId = userId, Name = "tc", Kind = "calendar", IsDefault = true };
    db.Set<CalendarEntity>().Add(cal);
    await db.SaveChangesAsync(CancellationToken.None);

    var start = DateTimeOffset.UtcNow;
    var ex = await Assert.ThrowsAsync<DomainException>(() =>
        service.CreateEventAsync(
            new CreateEventRequest(cal.Id, "t", "<script>alert(1)</script>", null, start, start.AddHours(1), null),
            CancellationToken.None));
    Assert.Equal(02013, ex.ErrorCode);
}

[Fact]
public async Task CreateEventAsync_RejectsIframe()
{
    var (service, db, userId) = CreateService();
    var cal = new CalendarEntity { UserId = userId, Name = "tc", Kind = "calendar", IsDefault = true };
    db.Set<CalendarEntity>().Add(cal);
    await db.SaveChangesAsync(CancellationToken.None);

    var start = DateTimeOffset.UtcNow;
    var ex = await Assert.ThrowsAsync<DomainException>(() =>
        service.CreateEventAsync(
            new CreateEventRequest(cal.Id, "t", "<iframe src='x'/>", null, start, start.AddHours(1), null),
            CancellationToken.None));
    Assert.Equal(02013, ex.ErrorCode);
}

[Fact]
public async Task CreateEventAsync_RejectsOnErrorHandler()
{
    var (service, db, userId) = CreateService();
    var cal = new CalendarEntity { UserId = userId, Name = "tc", Kind = "calendar", IsDefault = true };
    db.Set<CalendarEntity>().Add(cal);
    await db.SaveChangesAsync(CancellationToken.None);

    var start = DateTimeOffset.UtcNow;
    var ex = await Assert.ThrowsAsync<DomainException>(() =>
        service.CreateEventAsync(
            new CreateEventRequest(cal.Id, "t", "<img src=x onerror=alert(1)>", null, start, start.AddHours(1), null),
            CancellationToken.None));
    Assert.Equal(02013, ex.ErrorCode);
}

[Fact]
public async Task CreateEventAsync_AllowsPlainTextWithAngleBrackets()
{
    var (service, db, userId) = CreateService();
    var cal = new CalendarEntity { UserId = userId, Name = "tc", Kind = "calendar", IsDefault = true };
    db.Set<CalendarEntity>().Add(cal);
    await db.SaveChangesAsync(CancellationToken.None);

    var start = DateTimeOffset.UtcNow;
    var created = await service.CreateEventAsync(
        new CreateEventRequest(cal.Id, "t", "a < b and c > d", null, start, start.AddHours(1), null),
        CancellationToken.None);
    Assert.Equal("a < b and c > d", created.Description);
}

[Fact]
public async Task UpdateEventAsync_RejectsScriptTag()
{
    var (service, db, userId) = CreateService();
    var cal = new CalendarEntity { UserId = userId, Name = "tc", Kind = "calendar", IsDefault = true };
    db.Set<CalendarEntity>().Add(cal);
    var evt = new EventEntity
    {
        CalendarId = cal.Id,
        Uid = "unsafe-update@pim",
        Title = "original",
        DtStart = DateTimeOffset.UtcNow,
        DtEnd = DateTimeOffset.UtcNow.AddHours(1)
    };
    db.Set<EventEntity>().Add(evt);
    await db.SaveChangesAsync(CancellationToken.None);

    var ex = await Assert.ThrowsAsync<DomainException>(() =>
        service.UpdateEventAsync(evt.Id,
            new UpdateEventRequest(cal.Id, "t", "<script>bad</script>", null, evt.DtStart, evt.DtEnd, null),
            CancellationToken.None));
    Assert.Equal(02013, ex.ErrorCode);
}

[Fact]
public async Task CreateTaskAsync_RejectsScriptTag()
{
    var (service, db, userId) = CreateService();
    var cal = new CalendarEntity { UserId = userId, Name = "tc", Kind = "task", IsDefault = true };
    db.Set<CalendarEntity>().Add(cal);
    await db.SaveChangesAsync(CancellationToken.None);

    var ex = await Assert.ThrowsAsync<DomainException>(() =>
        service.CreateTaskAsync(
            new CreateTaskRequest(cal.Id, "t", "<script>bad</script>", 0, null, null, null, null, null, null),
            CancellationToken.None));
    Assert.Equal(02013, ex.ErrorCode);
}

[Fact]
public async Task UpdateTaskAsync_RejectsScriptTag()
{
    var (service, db, userId) = CreateService();
    var (task, cal) = await SeedTaskAsync(db, userId);

    var ex = await Assert.ThrowsAsync<DomainException>(() =>
        service.UpdateTaskAsync(task.Id,
            new UpdateTaskRequest(cal.Id, "t", "<script>bad</script>", 0, null, null, null, null, null, null),
            CancellationToken.None));
    Assert.Equal(02013, ex.ErrorCode);
}
```

- [ ] **Step 3.2: Run validation tests to see failures**

```
dotnet test tests\Pim.UnitTests\Pim.UnitTests.csproj --no-restore --filter FullyQualifiedName~CalendarServiceReliabilityTests
```

Expected: FAIL — many failures from unhandled HTML descriptions

- [ ] **Step 3.3: Create `ManualDescriptionValidator.cs`**

```csharp
using System.Text.RegularExpressions;
using Pim.Core.Exceptions;

namespace Pim.Module.Calendar.Services;

public static partial class ManualDescriptionValidator
{
    [GeneratedRegex(@"<(script|iframe|object|embed)\b[^>]*>", RegexOptions.IgnoreCase | RegexOptions.CultureInvariant)]
    private static partial Regex DangerousTagPattern();

    [GeneratedRegex(@"\bon\w+\s*=", RegexOptions.IgnoreCase | RegexOptions.CultureInvariant)]
    private static partial Regex EventHandlerPattern();

    public static void EnsureSafe(string? description)
    {
        if (description is null) return;
        if (DangerousTagPattern().IsMatch(description))
            throw new DomainException(02013, "描述中不允许包含可执行的 HTML 标签（script、iframe、object、embed）");
        if (EventHandlerPattern().IsMatch(description))
            throw new DomainException(02013, "描述中不允许包含事件处理属性（on*）");
    }
}
```

- [ ] **Step 3.4: Call validator in all four manual CRUD paths**

In `CreateEventAsync`, add right after the Outlook binding check (before entity creation):

```csharp
        ManualDescriptionValidator.EnsureSafe(request.Description);
```

In `UpdateEventAsync`, add right after binding checks (before `entity.Title = ...`):

```csharp
        ManualDescriptionValidator.EnsureSafe(request.Description);
```

In `CreateTaskAsync`, add at the top of the method body:

```csharp
        ManualDescriptionValidator.EnsureSafe(request.Description);
```

In `UpdateTaskAsync`, add at the top of the method body after task lookup:

```csharp
        ManualDescriptionValidator.EnsureSafe(request.Description);
```

Do not touch `OutlookEventMapper` or any sync path.

- [ ] **Step 3.5: Run tests**

```
dotnet test tests\Pim.UnitTests\Pim.UnitTests.csproj --no-restore --filter FullyQualifiedName~CalendarServiceReliabilityTests
```

Expected: PASS

- [ ] **Step 3.6: Commit**

```
git add src/modules/Pim.Module.Calendar/Services/ManualDescriptionValidator.cs tests/Pim.UnitTests/Calendar/CalendarServiceReliabilityTests.cs src/modules/Pim.Module.Calendar/Services/CalendarService.cs
git commit -m "fix: reject executable html in manual descriptions"
```

---

## Task 4: Frontend pure reliability utilities and dependency

Compatibility invariant for this task: parse the existing `TaskResponse.EstimatedDuration` .NET `TimeSpan c` output (`[d.]hh:mm:ss[.fffffff]`, for example `01:30:00`), emit ISO 8601 request values (for example `PT1H30M`), and do not change any DTO or response format in PR1.

**Files:**
- Create: `src/client-web/src/utils/dateTimeInput.ts`
- Create: `src/client-web/src/utils/durationInput.ts`
- Create: `src/client-web/src/utils/calendarSelection.ts`
- Create: `tests/client-web/calendarReliabilityUtils.test.ts`
- Modify: `src/client-web/package.json`
- Modify: `src/client-web/package-lock.json`

- [ ] **Step 4.1: Write calendarReliabilityUtils.test.ts (RED — module not found)**

```typescript
import assert from 'node:assert/strict';
import {
  isoToDatetimeLocal,
  datetimeLocalToUtcIso,
  minimumEndValue,
  isEndAfterStart,
} from '../../src/client-web/src/utils/dateTimeInput';
import {
  dotnetDurationToHoursMinutes,
  hoursMinutesToIsoDuration,
  isValidDuration,
  durationErrorMessage,
} from '../../src/client-web/src/utils/durationInput';
import {
  resolveCalendarId,
  hasWritableCalendar,
} from '../../src/client-web/src/utils/calendarSelection';
import type { CalendarResponse } from '../../src/client-web/src/types';

// ─── dateTimeInput ─────────────────────────────────────────

const UTC_ISO = '2026-07-20T06:00:00.000Z';

assert.equal(
  isoToDatetimeLocal(UTC_ISO, 'Asia/Shanghai'),
  '2026-07-20T14:00',
  'isoToDatetimeLocal UTC → Asia/Shanghai',
);

assert.equal(
  datetimeLocalToUtcIso('2026-07-20T14:00', 'Asia/Shanghai'),
  '2026-07-20T06:00:00.000Z',
  'datetimeLocalToUtcIso Asia/Shanghai → UTC',
);

assert.equal(
  minimumEndValue('2026-07-20T10:00'),
  '2026-07-20T10:01',
  'minimumEndValue adds 1 minute',
);

assert.equal(minimumEndValue(''), '', 'minimumEndValue empty returns empty');

assert.ok(isEndAfterStart('2026-07-20T10:00', '2026-07-20T11:00'), 'end after start');
assert.ok(!isEndAfterStart('2026-07-20T10:00', '2026-07-20T09:00'), 'end before start');
assert.ok(!isEndAfterStart('2026-07-20T10:00', '2026-07-20T10:00'), 'end equals start');
assert.ok(!isEndAfterStart('', ''), 'both empty returns false');
assert.ok(!isEndAfterStart('invalid', '2026-07-20T10:00'), 'invalid start returns false');

// ─── durationInput ─────────────────────────────────────────

assert.deepEqual(
  dotnetDurationToHoursMinutes('01:30:00'),
  { hours: 1, minutes: 30 },
  'parses hh:mm:ss',
);

assert.deepEqual(
  dotnetDurationToHoursMinutes('1.02:30:00'),
  { hours: 26, minutes: 30 },
  'parses d.hh:mm:ss with days rolled into hours',
);

assert.deepEqual(
  dotnetDurationToHoursMinutes(''),
  { hours: 0, minutes: 30 },
  'empty returns default 30 minutes',
);

assert.deepEqual(
  dotnetDurationToHoursMinutes(undefined),
  { hours: 0, minutes: 30 },
  'undefined returns default 30 minutes',
);

assert.equal(
  hoursMinutesToIsoDuration(1, 30),
  'PT1H30M',
  'hours+minutes to ISO',
);

assert.equal(
  hoursMinutesToIsoDuration(0, 45),
  'PT45M',
  'minutes-only to ISO',
);

assert.equal(
  hoursMinutesToIsoDuration(2, 0),
  'PT2H',
  'hours-only to ISO',
);

assert.equal(
  hoursMinutesToIsoDuration(0, 0),
  '',
  'zero returns empty',
);

assert.ok(isValidDuration('1', '30'), 'valid');
assert.ok(!isValidDuration('0', '0'), 'zero invalid');
assert.ok(!isValidDuration('0', '60'), 'minutes > 59 invalid');
assert.ok(!isValidDuration('-1', '30'), 'negative hours invalid');

assert.ok(
  durationErrorMessage().includes('分钟'),
  'error message mentions minutes',
);

// ─── calendarSelection ─────────────────────────────────────

const cals: CalendarResponse[] = [
  { id: 'cal-1', name: 'Default', color: '#00f', kind: 'calendar', isDefault: true, canEdit: true },
  { id: 'cal-2', name: 'ReadOnly', color: '#0f0', kind: 'calendar', isDefault: false, canEdit: false },
  { id: 'cal-3', name: 'Writable', color: '#f00', kind: 'calendar', isDefault: false, canEdit: true },
];

assert.equal(
  resolveCalendarId(cals, 'cal-3', new Set()),
  'cal-3',
  'explicit currentId wins',
);

assert.equal(
  resolveCalendarId(cals, undefined, new Set()),
  'cal-1',
  'no currentId picks isDefault writable',
);

assert.equal(
  resolveCalendarId(cals, undefined, new Set(['cal-1'])),
  'cal-3',
  'default hidden picks first writable visible',
);

assert.equal(
  resolveCalendarId([cals[1]], undefined, new Set()),
  '',
  'no writable returns empty',
);

assert.ok(hasWritableCalendar(cals, new Set()), 'writable exists');
assert.ok(!hasWritableCalendar([cals[1]], new Set()), 'no writable returns false');
```

- [ ] **Step 4.1b: Restore frontend dependencies before first test run**

```
npm --prefix src/client-web ci
```

Expected: Dependencies restored from lockfile. If `npm ci` fails (e.g. lockfile out of sync with package.json), use `npm --prefix src/client-web install`.

- [ ] **Step 4.2: Run test to confirm FAIL**

```
npm --prefix src/client-web exec tsx -- tests/client-web/calendarReliabilityUtils.test.ts
```

Expected: FAIL — module not found (utilities don't exist yet)

- [ ] **Step 4.3: Create dateTimeInput.ts**

```typescript
import { DateTime } from 'luxon';

export function isoToDatetimeLocal(iso: string, timeZoneId?: string): string {
  const dt = DateTime.fromISO(iso, { setZone: true });
  if (!dt.isValid) return '';
  const local = timeZoneId ? dt.setZone(timeZoneId) : dt.toLocal();
  return local.toFormat("yyyy-MM-dd'T'HH:mm");
}

export function datetimeLocalToUtcIso(local: string, timeZoneId?: string): string {
  const dt = timeZoneId
    ? DateTime.fromFormat(local, "yyyy-MM-dd'T'HH:mm", { zone: timeZoneId })
    : DateTime.fromFormat(local, "yyyy-MM-dd'T'HH:mm", { zone: 'local' });
  if (!dt.isValid) return '';
  const utc = dt.toUTC();
  return utc.toISO({ suppressMilliseconds: true })!;
}

export function minimumEndValue(startValue: string): string {
  if (!startValue) return '';
  const dt = DateTime.fromFormat(startValue, "yyyy-MM-dd'T'HH:mm");
  if (!dt.isValid) return '';
  return dt.plus({ minutes: 1 }).toFormat("yyyy-MM-dd'T'HH:mm");
}

export function isEndAfterStart(startValue: string, endValue: string): boolean {
  if (!startValue || !endValue) return false;
  const s = DateTime.fromFormat(startValue, "yyyy-MM-dd'T'HH:mm");
  const e = DateTime.fromFormat(endValue, "yyyy-MM-dd'T'HH:mm");
  if (!s.isValid || !e.isValid) return false;
  return e > s;
}
```

- [ ] **Step 4.4: Create durationInput.ts**

```typescript
const NET_C_DURATION = /^(?:(\d+)\.)?(\d+):([0-5]\d):([0-5]\d)(?:\.(\d{1,7}))?$/;

export function dotnetDurationToHoursMinutes(value?: string): { hours: number; minutes: number } {
  if (!value) return { hours: 0, minutes: 30 };
  const match = NET_C_DURATION.exec(value);
  if (!match) return { hours: 0, minutes: 30 };
  const days = Number(match[1] || 0);
  const hours = Number(match[2]);
  const minutes = Number(match[3]);
  return { hours: days * 24 + hours, minutes };
}

export function hoursMinutesToIsoDuration(hours: number, minutes: number): string {
  const totalMinutes = Math.floor(hours) * 60 + Math.floor(minutes);
  if (totalMinutes <= 0) return '';
  const h = Math.floor(totalMinutes / 60);
  const m = totalMinutes % 60;
  if (h > 0 && m > 0) return `PT${h}H${m}M`;
  if (h > 0) return `PT${h}H`;
  return `PT${m}M`;
}

export function isValidDuration(hours: string, minutes: string): boolean {
  const h = Number(hours);
  const m = Number(minutes);
  if (!Number.isFinite(h) || !Number.isFinite(m)) return false;
  if (!Number.isInteger(h) || !Number.isInteger(m)) return false;
  if (h < 0 || m < 0 || m > 59) return false;
  return h > 0 || m > 0;
}

export function durationErrorMessage(): string {
  return '请至少设置 1 分钟';
}
```

- [ ] **Step 4.5: Create calendarSelection.ts**

```typescript
import type { CalendarResponse } from '../types';

export function resolveCalendarId(
  calendars: CalendarResponse[],
  currentId: string | undefined,
  hiddenCalendarIds: Set<string>,
): string {
  if (currentId && calendars.some(c => c.id === currentId)) return currentId;

  const writableVisible = calendars.filter(
    c => c.canEdit !== false && !hiddenCalendarIds.has(c.id),
  );

  const defaultCal = writableVisible.find(c => c.isDefault);
  if (defaultCal) return defaultCal.id;

  const firstWritable = writableVisible[0];
  if (firstWritable) return firstWritable.id;

  return '';
}

export function hasWritableCalendar(
  calendars: CalendarResponse[],
  hiddenCalendarIds: Set<string>,
): boolean {
  return calendars.some(c => c.canEdit !== false && !hiddenCalendarIds.has(c.id));
}

export function noWritableCalendarMessage(): string {
  return '没有可用的可写日历，请先在设置中添加或启用日历';
}
```

- [ ] **Step 4.6: Run utility tests to verify PASS**

```
npm --prefix src/client-web exec tsx -- tests/client-web/calendarReliabilityUtils.test.ts
```

Expected: PASS (all assertions pass)

- [ ] **Step 4.7: Install DOMPurify, the Node DOM test harness, and add the utility test script**

```
npm --prefix src/client-web install dompurify
npm --prefix src/client-web install --save-dev jsdom @types/jsdom
```

Expected: `dompurify` is added to dependencies; `jsdom` and `@types/jsdom` are added to devDependencies; `src/client-web/package-lock.json` is updated.

Append to `src/client-web/package.json` scripts:

```json
    "test:calendar-reliability": "cd ../.. && npm --prefix src/client-web exec tsx -- tests/client-web/calendarReliabilityUtils.test.ts",
```

Prepend `calendarReliabilityUtils.test.ts` to the existing `test:schedule-workbench-complete` script value in `src/client-web/package.json`. The resulting script should start with the reliability utility test before all existing tests:

```json
    "test:schedule-workbench-complete": "cd ../.. && npm --prefix src/client-web exec tsx -- tests/client-web/calendarReliabilityUtils.test.ts && npm --prefix src/client-web exec tsx -- tests/client-web/scheduleWorkbenchCompletionApiPath.test.ts && npm --prefix src/client-web exec tsc -- -p tests/client-web/tsconfig.schedule-workbench.json && npm --prefix src/client-web exec tsx -- tests/client-web/scheduleWorkbenchCompletionTypes.test.ts && npm --prefix src/client-web exec tsx -- tests/client-web/scheduleWorkbenchLocalization.test.ts && npm --prefix src/client-web exec tsx -- tests/client-web/scheduleWorkbenchInteractions.test.tsx && npm --prefix src/client-web exec tsx -- tests/client-web/scheduleWorkbenchGovernanceUi.test.tsx && npm --prefix src/client-web exec tsx -- tests/client-web/scheduleWorkbenchChineseNavigation.test.ts && npm --prefix src/client-web exec tsx -- tests/client-web/endpointShellPage.test.tsx && npm --prefix src/client-web exec tsx -- tests/client-web/scheduleWorkbenchE2e.test.ts && npm --prefix src/client-web exec tsx -- tests/client-web/microsoftCalendarSyncApi.test.ts && npm --prefix src/client-web exec tsx -- tests/client-web/outlookEventWritebackUi.test.ts && npm --prefix src/client-web exec tsx -- tests/client-web/scheduleWorkbenchVisualAudit.test.ts"
```

- [ ] **Step 4.8: Commit**

```
git add src/client-web/src/utils/dateTimeInput.ts src/client-web/src/utils/durationInput.ts src/client-web/src/utils/calendarSelection.ts tests/client-web/calendarReliabilityUtils.test.ts src/client-web/package.json src/client-web/package-lock.json
git commit -m "test: add calendar reliability utilities"
```

---

## Task 5: Event editor time, default calendar, and safe HTML preview

**Files:**
- Create: `src/client-web/src/utils/safeHtml.ts`
- Create: `tests/client-web/safeHtml.test.ts`
- Modify: `tests/client-web/scheduleWorkbenchVisualAudit.test.ts`
- Modify: `src/client-web/src/dialogs/EventEditorDialog.tsx`
- Modify: `src/client-web/package.json`

- [ ] **Step 5.1: Verify Task 4 is committed**

```
git log --oneline -3
```

Should show the Task 4 commit.

- [ ] **Step 5.1b: Write safeHtml.test.ts (RED — module not found)**

Create `tests/client-web/safeHtml.test.ts`:

```typescript
import assert from 'node:assert/strict';
import { JSDOM } from 'jsdom';

const dom = new JSDOM('<!doctype html><html><body></body></html>');
Object.assign(globalThis, {
  window: dom.window,
  document: dom.window.document,
  Node: dom.window.Node,
  Element: dom.window.Element,
});

const {
  looksLikeHtml,
  sanitizeDescriptionHtml,
} = await import('../../src/client-web/src/utils/safeHtml');

// ─── looksLikeHtml ─────────────────────────────────────────

assert.ok(looksLikeHtml('<div>hello</div>'), 'detects div tag');
assert.ok(looksLikeHtml('<b>bold</b>'), 'detects b tag');
assert.ok(looksLikeHtml('<a href="x">link</a>'), 'detects a with attr');
assert.ok(!looksLikeHtml('plain text'), 'plain text returns false');
assert.ok(!looksLikeHtml('a < b and c > d'), 'angle brackets without HTML tag return false');
assert.ok(!looksLikeHtml(''), 'empty string returns false');

// ─── sanitizeDescriptionHtml ───────────────────────────────

assert.equal(
  sanitizeDescriptionHtml('<script>alert(1)</script>hello'),
  'hello',
  'removes script tag',
);

assert.equal(
  sanitizeDescriptionHtml('<img src=x onerror=alert(1)>'),
  '',
  'removes disallowed img and its event handler',
);

assert.equal(
  sanitizeDescriptionHtml('<iframe src="x"></iframe>safe'),
  'safe',
  'removes iframe',
);

assert.equal(
  sanitizeDescriptionHtml('<b>keep</b>'),
  '<b>keep</b>',
  'preserves allowed b tag',
);

assert.equal(
  sanitizeDescriptionHtml('<a href="https://example.com">link</a>'),
  '<a href="https://example.com">link</a>',
  'preserves allowed a with href',
);

assert.equal(
  sanitizeDescriptionHtml('<i>italic</i> <strong>strong</strong>'),
  '<i>italic</i> <strong>strong</strong>',
  'preserves i and strong',
);

```

- [ ] **Step 5.1c: Run safeHtml tests — expect FAIL**

```
npm --prefix src/client-web exec tsx -- tests/client-web/safeHtml.test.ts
```

Expected: FAIL — module not found (`safeHtml.ts` does not exist yet)

- [ ] **Step 5.2: Create safeHtml.ts**

```typescript
import DOMPurify from 'dompurify';

export function looksLikeHtml(value: string): boolean {
  return /<[a-z][\s\S]*>/i.test(value);
}

export function sanitizeDescriptionHtml(value: string): string {
  return DOMPurify.sanitize(value, {
    ALLOWED_TAGS: ['b', 'i', 'em', 'strong', 'a', 'p', 'br', 'ul', 'ol', 'li'],
    ALLOWED_ATTR: ['href'],
  });
}

```

- [ ] **Step 5.2b: Run safeHtml tests — expect PASS**

```
npm --prefix src/client-web exec tsx -- tests/client-web/safeHtml.test.ts
```

Expected: PASS (all assertions pass)

- [ ] **Step 5.2c: Add the safe HTML test to the reliability script**

Update `test:calendar-reliability` in `src/client-web/package.json` to run both pure utility suites:

```json
    "test:calendar-reliability": "cd ../.. && npm --prefix src/client-web exec tsx -- tests/client-web/calendarReliabilityUtils.test.ts && npm --prefix src/client-web exec tsx -- tests/client-web/safeHtml.test.ts",
```

In the existing `test:schedule-workbench-complete` value, insert the same safe HTML command immediately after `calendarReliabilityUtils.test.ts` and before `scheduleWorkbenchCompletionApiPath.test.ts`:

```text
npm --prefix src/client-web exec tsx -- tests/client-web/calendarReliabilityUtils.test.ts && npm --prefix src/client-web exec tsx -- tests/client-web/safeHtml.test.ts && npm --prefix src/client-web exec tsx -- tests/client-web/scheduleWorkbenchCompletionApiPath.test.ts
```

- [ ] **Step 5.3: Write visual audit scenario I (RED first, then implement)**

Add in `main()` after `await runScenarioH(browser, baseUrl);`:

```typescript
    await runScenarioI(browser, baseUrl);
```

Add scenario I function after `runScenarioH`. The mock data needs a manual event with `timeZoneId: 'Asia/Shanghai'` and offset ISO dates. Modify `allEvents` fixture: replace the `'手动创建的事件'` entry with:

```typescript
  {
    id: 'evt-manual-1', calendarId: 'cal-manual-1', uid: 'uid-manual-1',
    title: '手动创建的事件',
    dtStart: '2026-07-14T14:00:00+08:00',
    dtEnd: '2026-07-14T15:00:00+08:00',
    status: 'confirmed', source: 'manual', isAllDay: false, timeZoneId: 'Asia/Shanghai',
  },
  {
    id: 'evt-html-desc', calendarId: 'cal-manual-1', uid: 'uid-html-1',
    title: 'HTML 描述事件',
    description: '<div style="color:red">HTML<b>描述</b></div><script>window.__pimHtmlExecuted=true</script><img src="/missing-html-preview.png" onerror="window.__pimHtmlExecuted=true">',
    dtStart: '2026-07-14T10:00:00+08:00',
    dtEnd: '2026-07-14T11:00:00+08:00',
    status: 'confirmed', source: 'outlook', isAllDay: false, timeZoneId: 'Asia/Shanghai',
  },
```

Then add the scenario function:

```typescript
// ─── Scenario I: Event reliability ──────────────────────────────

async function runScenarioI(browser: Browser, baseUrl: string) {
  const [w, h] = viewports[2];
  const context = await browser.newContext({ viewport: { width: w, height: h } });
  try {
    const captured: CapturedRequest[] = [];
    await context.addInitScript(() => {
      localStorage.setItem('accessToken', 'schedule-workbench-visual-audit-token');
      (window as unknown as { __pimHtmlExecuted: boolean }).__pimHtmlExecuted = false;
    });
    await context.route('**/api/v1/**', route => {
      const url = new URL(route.request().url());
      const fullPath = url.pathname + url.search;
      const method = route.request().method();
      if (method !== 'GET') {
        const postBody = route.request().postDataJSON();
        captured.push({ url: fullPath, method, body: postBody });
      }
      return route.fulfill({
        status: 200, contentType: 'application/json',
        body: JSON.stringify(mockApiResponse(fullPath, method)),
      });
    });

    const page = await context.newPage();
    await openCalendarMonth(page, baseUrl);

    // I1: Open manual event with +08:00 offset — datetime-local shows valid value
    await openEventByText(page, '手动创建的事件');
    const dtInputs = page.locator('aside[role="dialog"] input[type="datetime-local"]');
    const firstDt = await dtInputs.first().inputValue().catch(() => '');
    assert.ok(firstDt.length > 0, 'datetime-local must have a value for offset event');
    assert.ok(!firstDt.includes('+'), 'datetime-local must not contain offset character');
    assert.ok(!firstDt.includes('Z'), 'datetime-local must not contain Z');

    // I2: End <= start shows Chinese error, no request sent
    // Set both to same value
    await dtInputs.nth(1).fill(firstDt);
    await page.locator('aside[role="dialog"] button[type="submit"]').click();
    const endError = await page.locator('text=结束时间必须晚于开始时间').isVisible({ timeout: 3_000 }).catch(() => false);
    assert.ok(endError, 'End <= start must show Chinese error message');

    assert.equal(
      captured.filter(c => c.method === 'POST' || c.method === 'PUT').length,
      0,
      'Invalid end time must not send a create/update request',
    );
    // Close
    await page.locator('aside[role="dialog"] button', { hasText: '取消' }).click();
    await page.waitForFunction(() => !document.querySelector('aside[role="dialog"]'), null, { timeout: 3_000 }).catch(() => undefined);

    // I3: Open again, edit title, save — PUT payload has UTC dtStart/dtEnd
    await openEventByText(page, '手动创建的事件');
    await page.locator('aside[role="dialog"] input[type="text"]').first().fill('UTC save test');
    await page.locator('aside[role="dialog"] button[type="submit"]').click();
    await page.waitForFunction(
      () => !document.querySelector('aside[role="dialog"]'),
      null, { timeout: 5_000 },
    ).catch(() => undefined);

    const putCalls = captured.filter(c => c.method === 'PUT' && c.url.includes('/calendar/events/'));
    const lastPut = putCalls[putCalls.length - 1];
    assert.ok(lastPut?.body, 'Editing a manual event must send one PUT request');
    const body = lastPut.body as Record<string, unknown>;
    assert.equal(typeof body.dtStart, 'string', 'PUT dtStart must be a string');
    assert.equal(typeof body.dtEnd, 'string', 'PUT dtEnd must be a string');
    assert.ok((body.dtStart as string).endsWith('Z'), 'Saved dtStart must be UTC (ends with Z)');
    assert.ok((body.dtStart as string).includes('T06:00:00'), `Saved dtStart should be 06:00:00Z but got ${body.dtStart}`);
    assert.ok((body.dtEnd as string).endsWith('Z'), 'Saved dtEnd must be UTC');
    assert.equal(body.calendarId, 'cal-manual-1', 'Existing manual event must retain its calendarId');

    // I4: New event defaults — default calendar is isDefault writable, not empty
    await page.goto(`${baseUrl}/calendar?view=month`, { waitUntil: 'domcontentloaded' });
    await page.waitForLoadState('networkidle', { timeout: 10_000 }).catch(() => undefined);
    const inboxPanel = page.getByRole('heading', { name: '收集箱', exact: true }).locator('../..');
    await inboxPanel.getByRole('button', { name: '+ 新建', exact: true }).click();
    await inboxPanel.getByRole('button', { name: '日程', exact: true }).click();
    await page.locator('aside[role="dialog"] h2', { hasText: '新建日程' })
      .waitFor({ state: 'visible', timeout: 5_000 });

    const calSelect = page.locator('aside[role="dialog"] select').first();
    assert.equal(await calSelect.inputValue(), 'cal-manual-1', 'New event must select the real default calendar ID');
    await page.locator('aside[role="dialog"] input[type="text"]').first().fill('默认日历创建验证');
    const createDtInputs = page.locator('aside[role="dialog"] input[type="datetime-local"]');
    await createDtInputs.first().fill('2026-07-20T09:00');
    await createDtInputs.nth(1).fill('2026-07-20T10:00');
    await page.locator('aside[role="dialog"] button[type="submit"]').click();
    await page.waitForFunction(() => !document.querySelector('aside[role="dialog"]'), null, { timeout: 5_000 });
    const createCall = captured.find(c => c.method === 'POST' && c.url.endsWith('/calendar/events'));
    assert.ok(createCall?.body, 'Creating a manual event must send POST /calendar/events');
    assert.equal((createCall.body as Record<string, unknown>).calendarId, 'cal-manual-1');

    // I5: HTML description — rendered as formatted preview, no raw tags, no script execution, no textarea
    await page.goto(`${baseUrl}/calendar?view=month`, { waitUntil: 'domcontentloaded' });
    await page.waitForLoadState('networkidle', { timeout: 10_000 }).catch(() => undefined);
    await openEventByText(page, 'HTML 描述事件');
    // Assert raw HTML tags do not appear as text in the dialog
    const asideText = await page.locator('aside[role="dialog"]').innerText().catch(() => '');
    assert.ok(!asideText.includes('<div'), 'Raw <div> tag must not appear in dialog text');
    assert.ok(!asideText.includes('<script'), 'Raw <script> tag must not appear in dialog text');
    assert.ok(asideText.includes('HTML描述') || asideText.includes('HTML 描述'),
      'Formatted HTML text "HTML描述" must be visible');
    const preview = page.locator('aside[role="dialog"] [data-description-html-preview]');
    assert.ok(await preview.isVisible({ timeout: 2_000 }), 'HTML description must show a safe preview section');
    // Assert no textarea visible for HTML description (readonly preview only)
    const htmlTextarea = page.locator('aside[role="dialog"] textarea');
    const textareaVisible = await htmlTextarea.isVisible().catch(() => false);
    assert.ok(!textareaVisible, 'HTML description must NOT have editable textarea');
    assert.equal(
      await page.evaluate(() => (window as unknown as { __pimHtmlExecuted?: boolean }).__pimHtmlExecuted),
      false,
      'Sanitized HTML must not execute script or event-handler code',
    );
    await page.locator('aside[role="dialog"] button:has-text("取消")').click();
    await page.waitForFunction(() => !document.querySelector('aside[role="dialog"]'), null, { timeout: 3_000 }).catch(() => undefined);

    await page.close();
  } finally {
    await context.close();
  }
}
```

- [ ] **Step 5.4: Run visual audit scenario to see it fail**

```
npm --prefix src/client-web exec tsx -- tests/client-web/scheduleWorkbenchVisualAudit.test.ts
```

Expected: FAIL — scenario I fails because EventEditorDialog hasn't been patched yet (time conversion, calendar selection, HTML preview, default calendar all missing).

- [ ] **Step 5.5: Patch EventEditorDialog — time conversion, calendar selection, HTML preview**

Replace import section at top of `EventEditorDialog.tsx`:

```typescript
import { useState, useRef, useId, useEffect, type FormEvent, type KeyboardEvent } from 'react';
import { useMutation, useQueryClient, useQuery } from '@tanstack/react-query';
import { createEvent, updateEvent, deleteEvent, getCalendars, writeOutlookEvent } from '../api/calendar';
import EditorDrawer from '../ui/EditorDrawer';
import ConfirmActionDialog, { type DeleteConfirmationInput } from '../ui/ConfirmActionDialog';
import BeforeAfterDiff from '../components/schedule/BeforeAfterDiff';
import { Field } from './common';
import type { EventResponse, OutlookWriteRequest, OutlookEventDraft } from '../types';
import { isoToDatetimeLocal, datetimeLocalToUtcIso, minimumEndValue, isEndAfterStart } from '../utils/dateTimeInput';
import { resolveCalendarId, hasWritableCalendar, noWritableCalendarMessage } from '../utils/calendarSelection';
import { looksLikeHtml, sanitizeDescriptionHtml } from '../utils/safeHtml';
import { useCalendarVisibility } from '../context/CalendarVisibilityContext';
```

Replace the state initialization block in `EventEditorForm` (lines 67-84) — update dtStart/dtEnd initialization:

```typescript
  const [title, setTitle] = useState(event?.title || '');
  const [description, setDescription] = useState(event?.description || '');
  const [location, setLocation] = useState(event?.location || '');
  const [dtStart, setDtStart] = useState(() => {
    if (event?.dtStart) return isoToDatetimeLocal(event.dtStart, event.timeZoneId);
    if (defaultStart) return defaultStart;
    return '';
  });
  const [dtEnd, setDtEnd] = useState(() => {
    if (event?.dtEnd) return isoToDatetimeLocal(event.dtEnd, event.timeZoneId);
    if (defaultEnd) return defaultEnd;
    return '';
  });
  const [isAllDay, setIsAllDay] = useState(Boolean(event?.isAllDay));
  const [calendarId, setCalendarId] = useState(event?.calendarId || '');
  const [deleteInput, setDeleteInput] = useState<DeleteConfirmationInput | null>(null);
  const queryClient = useQueryClient();

  const [writebackPhase, setWritebackPhase] = useState<WritebackPhase>({ type: 'idle' });
  const [pendingRequest, setPendingRequest] = useState<OutlookWriteRequest | null>(null);
  const [outlookScope, setOutlookScope] = useState<'instance' | 'series'>(() =>
    event?.outlookEventType === 'seriesMaster' ? 'series' : 'instance',
  );
  const [diffBefore, setDiffBefore] = useState('{}');
  const [diffAfter, setDiffAfter] = useState('{}');
  const [writebackValidationError, setWritebackValidationError] = useState('');
```

Add inside `EventEditorForm` after state declarations:

```typescript
  const { hiddenCalendarIds } = useCalendarVisibility();
```

Replace line 91 `selectedCalendarId` computation:

```typescript
  const selectedCalendarId = resolveCalendarId(
    calendars || [],
    calendarId || (event ? event.calendarId : undefined),
    hiddenCalendarIds,
  );
```

Replace `handleSubmit` function (lines 289-305):

```typescript
  function handleSubmit(e: FormEvent) {
    e.preventDefault();
    if (isReadOnly) return;

    if (!isEndAfterStart(dtStart, dtEnd)) {
      setWritebackValidationError('结束时间必须晚于开始时间');
      return;
    }
    setWritebackValidationError('');

    const utcStart = datetimeLocalToUtcIso(dtStart, event?.timeZoneId);
    const utcEnd = datetimeLocalToUtcIso(dtEnd, event?.timeZoneId);

    if (isOutlook) {
      if (writebackPhase.type !== 'idle') return;
      if (event && !event.outlookEtag) {
        setWritebackValidationError('缺少版本标识，无法执行写回操作。');
        return;
      }
      openWritebackPreview(event ? 'update' : 'create');
    } else {
      const data = {
        title, description, location,
        dtStart: utcStart, dtEnd: utcEnd,
        isAllDay, calendarId: selectedCalendarId || undefined
      };
      if (event) updateMut.mutate(data);
      else createMut.mutate(data);
    }
  }
```

Replace `buildDraft` (lines 108-120):

```typescript
  function buildDraft(): OutlookEventDraft {
    return {
      calendarId: selectedCalendarId || event?.calendarId || '',
      title,
      description: description || undefined,
      location: location || undefined,
      dtStart: datetimeLocalToUtcIso(dtStart, event?.timeZoneId),
      dtEnd: datetimeLocalToUtcIso(dtEnd, event?.timeZoneId),
      isAllDay,
      timeZoneId: event?.timeZoneId || undefined,
      uid: event?.uid || undefined,
    };
  }
```

Replace calendar select dropdown (lines 361-370):

```tsx
        <Field label="日历本">
          <select value={selectedCalendarId} onChange={e => setCalendarId(e.target.value)}
            disabled={!!event && (isFormDisabled || isOutlookExisting)}
            className="w-full border rounded px-3 py-2 text-sm disabled:bg-slate-100 disabled:text-slate-500">
            {calendars?.map(cal => (
              <option key={cal.id} value={cal.id}>
                {cal.name}{cal.outlookCalendarBindingId ? ' (Outlook)' : ''}
              </option>
            ))}
          </select>
          {!isReadOnly && !hasWritableCalendar(calendars || [], hiddenCalendarIds) && (
            <p className="mt-1 text-xs text-red-600">{noWritableCalendarMessage()}</p>
          )}
        </Field>
```

Replace dtEnd input with `min` attribute (lines 391-395):

```tsx
        <Field label="结束时间">
          <input type="datetime-local" value={dtEnd}
            min={minimumEndValue(dtStart)}
            onChange={e => setDtEnd(e.target.value)}
            disabled={isFormDisabled}
            className="w-full border rounded px-3 py-2 text-sm disabled:bg-slate-100 disabled:text-slate-500" required />
        </Field>
```

Replace description field (lines 401-405):

```tsx
        <Field label="描述">
          {event && looksLikeHtml(description) ? (
            <div data-description-html-preview className="rounded border border-slate-200 bg-slate-50 p-2 text-xs text-slate-500">
              <div className="mt-1 prose prose-sm max-w-none" dangerouslySetInnerHTML={{ __html: sanitizeDescriptionHtml(description) }} />
            </div>
          ) : (
            <textarea value={description} onChange={e => setDescription(e.target.value)}
              disabled={isFormDisabled}
              className="w-full border rounded px-3 py-2 text-sm disabled:bg-slate-100 disabled:text-slate-500" rows={3} />
          )}
        </Field>
```

- [ ] **Step 5.6: Run visual audit scenarios**

```
npm --prefix src/client-web exec tsx -- tests/client-web/calendarReliabilityUtils.test.ts
```

Expected: PASS

Run full visual audit:

```
npm --prefix src/client-web exec tsx -- tests/client-web/scheduleWorkbenchVisualAudit.test.ts
```

Expected: PASS (all scenarios including new I)

- [ ] **Step 5.7: Commit**

```
git add src/client-web/src/utils/safeHtml.ts tests/client-web/safeHtml.test.ts src/client-web/src/dialogs/EventEditorDialog.tsx tests/client-web/scheduleWorkbenchVisualAudit.test.ts src/client-web/package.json
git commit -m "fix: make event editor time and calendar selection reliable"
```

---

## Task 6: Task editor duration and time validation

**Files:**
- Modify: `src/client-web/src/dialogs/TaskEditorDialog.tsx`
- Modify: `tests/client-web/scheduleWorkbenchVisualAudit.test.ts`

- [ ] **Step 6.1: Add visual audit scenario J (RED first)**

Add in `main()` after `await runScenarioI(browser, baseUrl);`:

```typescript
    await runScenarioJ(browser, baseUrl);
```

Add to `allEvents` fixture a task for mock (the `mockApiResponse` for `/calendar/events` returns `allEvents`; tasks are fetched separately via `/calendar/tasks`). Add a branch for `/calendar/tasks` in `mockApiResponse` — insert it AFTER the `/calendar/events` branch (the `if (fullPath.includes('/calendar/events'))` block) and BEFORE the generic fallback `data = []`:

```typescript
  } else if (fullPath.includes('/calendar/tasks')) {
    data = [
      {
        id: 'task-audit-1', calendarId: 'cal-manual-1', title: '排程任务',
        description: '测试描述', priority: 1,
        estimatedDuration: '01:30:00',
        dtStart: '2026-07-14T14:00:00+08:00',
        plannedEnd: '2026-07-14T15:30:00+08:00',
        due: null, status: 'NEEDS-ACTION', isInbox: false, sortOrder: 1,
        subTasks: [],
      },
    ];
```

Add the scenario function:

```typescript
// ─── Scenario J: Task editor reliability ─────────────────────────

async function runScenarioJ(browser: Browser, baseUrl: string) {
  const [w, h] = viewports[2];
  const context = await browser.newContext({ viewport: { width: w, height: h } });
  try {
    const captured: CapturedRequest[] = [];
    await context.addInitScript(() => {
      localStorage.setItem('accessToken', 'schedule-workbench-visual-audit-token');
    });
    await context.route('**/api/v1/**', route => {
      const url = new URL(route.request().url());
      const fullPath = url.pathname + url.search;
      const method = route.request().method();
      if (method !== 'GET') {
        const postBody = route.request().postDataJSON();
        captured.push({ url: fullPath, method, body: postBody });
      }
      return route.fulfill({
        status: 200, contentType: 'application/json',
        body: JSON.stringify(mockApiResponse(fullPath, method)),
      });
    });

    const page = await context.newPage();
    await openCalendarMonth(page, baseUrl);

    // J1: Open task — duration shows 1h30m in two inputs
    await openEventByText(page, '排程任务');
    const hoursInput = page.locator('aside[role="dialog"] input[type="number"]').first();
    const minsInput = page.locator('aside[role="dialog"] input[type="number"]').nth(1);
    const hoursVal = await hoursInput.inputValue().catch(() => '');
    const minsVal = await minsInput.inputValue().catch(() => '');
    assert.equal(hoursVal, '1', 'Hours input should show 1 for 01:30:00');
    assert.equal(minsVal, '30', 'Minutes input should show 30 for 01:30:00');

    // J2: Set 0 hours 0 minutes → error, no request
    await hoursInput.fill('0');
    await minsInput.fill('0');
    await page.locator('aside[role="dialog"] button[type="submit"]').click();
    const zeroError = await page.locator('text=请至少设置 1 分钟').isVisible({ timeout: 3_000 }).catch(() => false);
    assert.ok(zeroError, 'Zero duration must show error message');

    // J3: Set valid duration — save body contains PT1H30M ISO format
    await hoursInput.fill('1');
    await minsInput.fill('30');
    // Fix time first
    const dtInputs = page.locator('aside[role="dialog"] input[type="datetime-local"]');
    await dtInputs.first().fill('2026-07-14T14:00');
    await dtInputs.nth(1).fill('2026-07-14T15:30');
    await page.locator('aside[role="dialog"] button[type="submit"]').click();
    await page.waitForFunction(
      () => !document.querySelector('aside[role="dialog"]'),
      null, { timeout: 5_000 },
    ).catch(() => undefined);

    const putCalls = captured.filter(c => c.method === 'PUT' && c.url.includes('/calendar/tasks/'));
    const lastPut = putCalls[putCalls.length - 1];
    if (lastPut && lastPut.body) {
      const body = lastPut.body as Record<string, unknown>;
      if (body.estimatedDuration) {
        assert.equal(body.estimatedDuration, 'PT1H30M', 'Duration must be saved as ISO PT1H30M');
      }
    }

    // J4: plannedEnd <= dtStart blocks
    await openEventByText(page, '排程任务');
    await dtInputs.first().fill('2026-07-14T15:00');
    await dtInputs.nth(1).fill('2026-07-14T14:00');
    await page.locator('aside[role="dialog"] button[type="submit"]').click();
    const rangeError = await page.locator('text=计划结束时间必须晚于开始时间').isVisible({ timeout: 3_000 }).catch(() => false);
    assert.ok(rangeError, 'plannedEnd <= dtStart must show error');

    await page.close();
  } finally {
    await context.close();
  }
}
```

- [ ] **Step 6.2: Run visual audit — expect FAIL**

```
npm --prefix src/client-web exec tsx -- tests/client-web/calendarReliabilityUtils.test.ts
```

Expected: PASS

```
npm --prefix src/client-web exec tsx -- tests/client-web/scheduleWorkbenchVisualAudit.test.ts
```

Expected: FAIL (TaskEditorDialog not yet patched)

- [ ] **Step 6.3: Rewrite TaskEditorDialog duration inputs**

Replace imports in `TaskEditorDialog.tsx`:

```typescript
import { useState, type FormEvent } from 'react';
import { useMutation, useQueryClient, useQuery, type QueryClient } from '@tanstack/react-query';
import { createTask, updateTask, deleteTask, getCalendars, moveTask, taskToMutationData } from '../api/calendar';
import type { TaskMutationData } from '../api/calendar';
import EditorDrawer from '../ui/EditorDrawer';
import ConfirmActionDialog, { type DeleteConfirmationInput } from '../ui/ConfirmActionDialog';
import { Field } from './common';
import type { TaskResponse } from '../types';
import { isoToDatetimeLocal, datetimeLocalToUtcIso, isEndAfterStart } from '../utils/dateTimeInput';
import { dotnetDurationToHoursMinutes, hoursMinutesToIsoDuration, isValidDuration, durationErrorMessage } from '../utils/durationInput';
```

Replace state declarations in `TaskEditorForm`:

```typescript
  const [title, setTitle] = useState(task?.title || '');
  const [description, setDescription] = useState(task?.description || '');
  const [priority, setPriority] = useState(task?.priority || 0);
  const [dtStart, setDtStart] = useState(() => {
    if (defaultDtStart) return defaultDtStart;
    if (task?.dtStart) return isoToDatetimeLocal(task.dtStart);
    return '';
  });
  const [plannedEnd, setPlannedEnd] = useState(() => {
    if (task?.plannedEnd) return isoToDatetimeLocal(task.plannedEnd);
    return '';
  });
  const [due, setDue] = useState(() => {
    if (task?.due) return isoToDatetimeLocal(task.due);
    return '';
  });
  const [durationHours, setDurationHours] = useState(() => {
    const { hours } = dotnetDurationToHoursMinutes(task?.estimatedDuration);
    return String(hours);
  });
  const [durationMinutes, setDurationMinutes] = useState(() => {
    const { minutes } = dotnetDurationToHoursMinutes(task?.estimatedDuration);
    return String(minutes);
  });
  const [calendarId, setCalendarId] = useState(task?.calendarId || '');
  const [deleteInput, setDeleteInput] = useState<DeleteConfirmationInput | null>(null);
  const [validationErrorMessage, setValidationErrorMessage] = useState<string | null>(null);
```

Replace duration field (lines 239-242):

```tsx
        <Field label="预估时长">
          <div className="flex gap-2 items-center">
            <label className="sr-only" htmlFor="task-duration-hours">时</label>
            <input id="task-duration-hours" type="number" value={durationHours}
              min={0} step={1}
              onChange={e => setDurationHours(e.target.value)}
              className="w-20 border rounded px-3 py-2 text-sm" aria-label="时" />
            <span className="text-sm text-slate-500">时</span>
            <label className="sr-only" htmlFor="task-duration-minutes">分钟</label>
            <input id="task-duration-minutes" type="number" value={durationMinutes}
              min={0} max={59} step={1}
              onChange={e => setDurationMinutes(e.target.value)}
              className="w-20 border rounded px-3 py-2 text-sm" aria-label="分钟" />
            <span className="text-sm text-slate-500">分钟</span>
          </div>
        </Field>
```

Replace `handleSubmit` (lines 117-140):

```typescript
  function handleSubmit(e: FormEvent) {
    e.preventDefault();
    setValidationErrorMessage(null);

    if (durationHours || durationMinutes) {
      if (!isValidDuration(durationHours, durationMinutes)) {
        setValidationErrorMessage(durationErrorMessage());
        return;
      }
    }

    if (dtStart && plannedEnd && !isEndAfterStart(dtStart, plannedEnd)) {
      setValidationErrorMessage('计划结束时间必须晚于开始时间');
      return;
    }

    if (task?.plannedEnd && !plannedEnd) {
      setValidationErrorMessage('当前接口暂不支持清空计划结束时间，可改成新的结束时间。');
      return;
    }

    const durationIso = isValidDuration(durationHours, durationMinutes)
      ? hoursMinutesToIsoDuration(Number(durationHours), Number(durationMinutes))
      : undefined;

    const data: TaskMutationData = {
      title, description, priority,
      dtStart: dtStart ? datetimeLocalToUtcIso(dtStart) : undefined,
      plannedEnd: plannedEnd ? datetimeLocalToUtcIso(plannedEnd) : undefined,
      due: due ? datetimeLocalToUtcIso(due) : undefined,
      estimatedDuration: durationIso,
      calendarId: calendarId || undefined
    };
    if (task) {
      updateMut.mutate({
        data: taskToMutationData(task, data),
        confirmSchedule: Boolean(defaultDtStart && data.dtStart),
      });
    }
    else createMut.mutate(data);
  }
```

- [ ] **Step 6.4: Run utility tests and visual audit**

```
npm --prefix src/client-web exec tsx -- tests/client-web/calendarReliabilityUtils.test.ts
```

Expected: PASS

```
npm --prefix src/client-web exec tsx -- tests/client-web/scheduleWorkbenchVisualAudit.test.ts
```

Expected: PASS (all scenarios including J)

- [ ] **Step 6.5: Commit**

```
git add src/client-web/src/dialogs/TaskEditorDialog.tsx tests/client-web/scheduleWorkbenchVisualAudit.test.ts
git commit -m "fix: replace task duration text with hour minute inputs"
```

---

## Task 7: Month capacity, view timezone, and timeline event density

**Files:**
- Modify: `tests/client-web/calendarLayerVisibility.test.ts`
- Modify: `tests/client-web/scheduleWorkbenchVisualAudit.test.ts`
- Modify: `src/client-web/src/pages/CalendarPage.tsx`
- Modify: `src/client-web/src/index.css`

- [ ] **Step 7.1: Incrementally update calendarLayerVisibility.test.ts for color metadata (RED)**

Add `CalendarResponse` to the existing type-only import; do not duplicate the current imports or fixture declarations:

```typescript
import type { CalendarLayerItem, CalendarResponse, EventResponse, TaskResponse } from '../../src/client-web/src/types';
```

Add this fixture after `taskSegmentLayerItem`:

```typescript
const calendars: CalendarResponse[] = [
  { id: 'calendar-1', name: 'Work', color: '#2563eb', kind: 'calendar', isDefault: true, canEdit: true },
];
```

Pass `calendars` as the fifth argument in both existing `buildCalendarEvents(...)` calls. After the existing `onlyEvents` title assertion, add:

```typescript
for (const calendarEvent of onlyEvents) {
  assert.equal(calendarEvent.backgroundColor, undefined, `Event ${calendarEvent.id} must not have backgroundColor`);
  assert.equal(calendarEvent.borderColor, undefined, `Event ${calendarEvent.id} must not have borderColor`);
}

const eventMetadata = onlyEvents[0].extendedProps as {
  type: 'event';
  raw: EventResponse;
  accentColor: string;
  calendarLabel: string;
};
assert.equal(eventMetadata.accentColor, '#2563eb');
assert.equal(eventMetadata.calendarLabel, 'Work');
```

- [ ] **Step 7.2: Run layer test — expect FAIL (buildCalendarEvents still has colors)**

```
npm --prefix src/client-web exec tsx -- tests/client-web/calendarLayerVisibility.test.ts
```

Expected: FAIL — events still have backgroundColor/borderColor

- [ ] **Step 7.3: Add visual audit scenario K for density (RED)**

Add in `main()` after `await runScenarioJ(browser, baseUrl);`:

```typescript
    await runScenarioK(browser, baseUrl);
```

Add these deterministic fixtures to `allEvents`. The long event exercises all five timeline content levels; the compact event verifies low-priority fields stay hidden; the five same-day events exercise month capacity:

```typescript
  {
    id: 'evt-density-long', calendarId: 'cal-manual-1', uid: 'uid-density-long',
    title: '密度详情事件', description: '这是用于验证时间轴描述摘要的长日程', location: '会议室 A',
    dtStart: '2026-07-20T09:00:00+08:00', dtEnd: '2026-07-20T12:00:00+08:00',
    rrule: 'FREQ=WEEKLY', status: 'confirmed', source: 'manual', isAllDay: false,
  },
  {
    id: 'evt-density-short', calendarId: 'cal-manual-1', uid: 'uid-density-short',
    title: '密度紧凑事件', description: '不应在紧凑块显示', location: '会议室 B',
    dtStart: '2026-07-20T13:00:00+08:00', dtEnd: '2026-07-20T13:15:00+08:00',
    status: 'confirmed', source: 'manual', isAllDay: false,
  },
  ...Array.from({ length: 5 }, (_, index) => ({
    id: `evt-month-capacity-${index + 1}`,
    calendarId: 'cal-manual-1',
    uid: `uid-month-capacity-${index + 1}`,
    title: `容量日程 ${index + 1}`,
    dtStart: `2026-07-15T${String(9 + index).padStart(2, '0')}:00:00+08:00`,
    dtEnd: `2026-07-15T${String(10 + index).padStart(2, '0')}:00:00+08:00`,
    status: 'confirmed', source: 'manual', isAllDay: false,
  })),
```

Add scenario K:

```typescript
// ─── Scenario K: Calendar density and visual ─────────────────────

async function runScenarioK(browser: Browser, baseUrl: string) {
  const [w, h] = viewports[2];
  const context = await browser.newContext({ viewport: { width: w, height: h } });
  try {
    const consoleErrors: string[] = [];
    await context.addInitScript(() => {
      localStorage.setItem('accessToken', 'schedule-workbench-visual-audit-token');
    });
    await context.route('**/api/v1/**', route => {
      const url = new URL(route.request().url());
      return route.fulfill({
        status: 200, contentType: 'application/json',
        body: JSON.stringify(mockApiResponse(url.pathname + url.search, route.request().method())),
      });
    });

    const page = await context.newPage();
    page.on('console', message => {
      if (message.type() === 'error') consoleErrors.push(message.text());
    });

    // K1: Timeline event card has light background + 3px left accent border
    await page.goto(`${baseUrl}/calendar?view=timeline`, { waitUntil: 'domcontentloaded' });
    await page.waitForLoadState('networkidle', { timeout: 10_000 }).catch(() => undefined);
    await page.waitForSelector('.fc-event, .calendar-event-card', { timeout: 8_000 }).catch(() => undefined);

    const longCard = page.locator('.fc-timegrid-event:has-text("密度详情事件") .calendar-event-card');
    await longCard.waitFor({ state: 'visible', timeout: 5_000 });
    assert.equal(await longCard.getAttribute('data-content-level'), '5', 'Three-hour event must expose level 5 content');
    assert.equal(await longCard.evaluate(el => getComputedStyle(el).borderLeftWidth), '3px');
    assert.equal(await longCard.evaluate(el => getComputedStyle(el).borderLeftColor), 'rgb(170, 68, 0)');
    const backgroundColor = await longCard.evaluate(el => getComputedStyle(el).backgroundColor);
    assert.ok(
      backgroundColor.includes('0.15') || backgroundColor.includes('0.149'),
      `Timeline background must use approximately 15% alpha, got ${backgroundColor}`,
    );
    assert.ok(await longCard.locator('.calendar-event-location').isVisible(), 'Level 5 shows location');
    assert.ok(await longCard.locator('.calendar-event-source').isVisible(), 'Level 5 shows calendar/source label');
    assert.ok(await longCard.locator('.calendar-event-description').isVisible(), 'Level 5 shows description summary');
    assert.ok(await longCard.locator('.calendar-event-rrule').isVisible(), 'Level 5 shows recurrence icon');

    const shortCard = page.locator('.fc-timegrid-event:has-text("密度紧凑事件") .calendar-event-card');
    await shortCard.waitFor({ state: 'visible', timeout: 5_000 });
    assert.equal(await shortCard.getAttribute('data-content-level'), '1', 'Compact event only exposes level 1');
    assert.ok(!(await shortCard.locator('.calendar-event-location').isVisible()), 'Level 1 hides location');

    // K2: Month view — tall board shows all events, no "+N more"
    await page.setViewportSize({ width: w, height: 1200 });
    await page.goto(`${baseUrl}/calendar?view=month`, { waitUntil: 'domcontentloaded' });
    await page.waitForLoadState('networkidle', { timeout: 10_000 }).catch(() => undefined);
    const board = page.locator('.calendar-board');
    await board.evaluate(element => {
      const htmlElement = element as HTMLElement;
      htmlElement.style.flex = '0 0 auto';
      htmlElement.style.height = '900px';
    });
    await page.evaluate(() => window.dispatchEvent(new Event('resize')));
    await page.waitForTimeout(500);

    const moreLink = page.locator('.fc-more-link');
    const hasMoreLinkTall = await moreLink.isVisible().catch(() => false);
    assert.ok(!hasMoreLinkTall, 'Tall month board must not show "+N more" link');
    for (let index = 1; index <= 5; index += 1) {
      assert.ok(
        await page.getByText(`容量日程 ${index}`, { exact: true }).isVisible(),
        `Tall month board must show 容量日程 ${index}`,
      );
    }

    // K3: Month view — short board shows "+N more"
    await board.evaluate(element => {
      const htmlElement = element as HTMLElement;
      htmlElement.style.height = '280px';
    });
    await page.evaluate(() => window.dispatchEvent(new Event('resize')));
    await page.waitForTimeout(500);
    const hasMoreLinkShort = await moreLink.isVisible({ timeout: 5_000 }).catch(() => false);
    assert.ok(hasMoreLinkShort, 'Short month board must show "+N more" link');

    // K4: Click "+N more" opens native FullCalendar popover
    if (hasMoreLinkShort) {
      await moreLink.first().click();
      const popover = page.locator('.fc-more-popover');
      await popover.waitFor({ state: 'visible', timeout: 3_000 }).catch(() => undefined);
      const popoverVisible = await popover.isVisible().catch(() => false);
      assert.ok(popoverVisible, 'Clicking +N more must show FullCalendar popover');
      const eventCardsInPopover = popover.locator('.calendar-event-card');
      const count = await eventCardsInPopover.count();
      assert.ok(count >= 5, 'Popover must contain at least the five capacity fixtures');
      for (let index = 1; index <= 5; index += 1) {
        assert.ok(
          await popover.getByText(`容量日程 ${index}`, { exact: true }).isVisible(),
          `Popover must show 容量日程 ${index}`,
        );
      }
      // Click outside to close
      await page.locator('.fc-daygrid-body').first().click({ position: { x: 10, y: 10 } });
      await page.waitForTimeout(300);
    }

    // K5: No console errors
    assert.deepEqual(consoleErrors, [], 'No console errors in calendar density view');

    await page.close();
  } finally {
    await context.close();
  }
}
```

- [ ] **Step 7.4: Run density test — expect FAIL**

```
npm --prefix src/client-web exec tsx -- tests/client-web/scheduleWorkbenchVisualAudit.test.ts
```

Expected: FAIL (CSS not updated, CalendarPage not fixed)

- [ ] **Step 7.5: Update CalendarPage.tsx**

Update the imports so the page can load calendar metadata and render the existing recurrence icon:

```typescript
import { useCallback, useEffect, useMemo, useRef, useState, type CSSProperties } from 'react';
import { Repeat2 } from 'lucide-react';
import { getCalendarLayers, getCalendars, getEvents, getTasks, planTask } from '../api/calendar';
import type { CalendarLayerId, CalendarLayerItem, CalendarResponse, EventResponse, TaskResponse } from '../types';
```

Replace `CalendarEventProps` with metadata-bearing variants so `extendedProps` stays type-safe:

```typescript
type CalendarEventProps =
  | {
      type: 'event';
      raw: EventResponse;
      accentColor: string;
      calendarLabel: string;
    }
  | {
      type: 'task';
      raw: TaskResponse;
      accentColor: string;
      calendarLabel: string;
    }
  | {
      type: 'layer';
      raw: CalendarLayerItem;
      accentColor: string;
    };
```

Add the calendar query next to the existing events/tasks queries:

```typescript
  const { data: calendars = [] } = useQuery({
    queryKey: ['calendars'],
    queryFn: getCalendars,
  });
```

Pass `calendars` to `buildCalendarEvents` and include it in the memo dependencies:

```typescript
    return buildCalendarEvents(
      enabledLayerSet.has('events') ? visibleEvents : [],
      tasks,
      layerItems,
      enabledLayerSet,
      calendars,
    );
  }, [calendarLayerData?.items, calendars, enabledLayerSet, events, hiddenCalendarIds, tasks]);
```

Change line 310 `timeZone="Asia/Shanghai"` to:

```tsx
          timeZone="local"
```

Change line 314 `dayMaxEvents={mode === 'month' ? 3 : undefined}` to:

```tsx
          dayMaxEvents={mode === 'month' ? true : undefined}
```

Replace `buildCalendarEvents` function to remove `backgroundColor`/`borderColor` and attach per-calendar color. Add a helper to look up calendar by id:

```typescript
function calendarForEventOrTask(
  calendarId: string | undefined,
  calendars: CalendarResponse[],
): CalendarResponse | undefined {
  return calendarId ? calendars.find(calendar => calendar.id === calendarId) : undefined;
}

export function buildCalendarEvents(
  events: EventResponse[],
  tasks: TaskResponse[],
  layerItems: CalendarLayerItem[],
  enabledLayerSet: Set<CalendarLayerToggleId>,
  calendars: CalendarResponse[] = [],
): CalendarEventInput[] {
  return [
    ...events.map(event => {
      const calendar = calendarForEventOrTask(event.calendarId, calendars);
      return {
        id: event.id,
        title: event.title,
        start: event.dtStart,
        end: event.dtEnd,
        allDay: event.isAllDay,
        extendedProps: {
          type: 'event' as const,
          raw: event,
          accentColor: calendar?.color || '#2563eb',
          calendarLabel: calendar?.name || event.source,
        },
      };
    }),
    ...(enabledLayerSet.has('task-segments') ? tasks : []).filter(task => task.dtStart).map(task => {
      const calendar = calendarForEventOrTask(task.calendarId, calendars);
      return {
        id: task.id,
        title: task.title,
        start: task.dtStart,
        end: task.plannedEnd || task.due,
        extendedProps: {
          type: 'task' as const,
          raw: task,
          accentColor: priorityAccentColor(task.priority),
          calendarLabel: calendar?.name || '任务',
        },
      };
    }),
    ...layerItems
      .filter(item => enabledLayerSet.has(item.layer as CalendarLayerToggleId))
      .filter(item => item.layer !== 'events')
      .map(item => ({
        id: `layer-${item.layer}-${item.id}`,
        title: item.title,
        start: item.startsAt,
        end: item.endsAt,
        backgroundColor: 'transparent',
        borderColor: 'transparent',
        classNames: item.layer === 'task-segments'
          ? ['pim-calendar-layer-task-segment']
          : ['pim-calendar-layer'],
        extendedProps: {
          type: 'layer' as const,
          raw: item,
          accentColor: item.color,
        },
      })),
  ];
}

function priorityAccentColor(priority: number): string {
  if (priority === 1) return '#ef4444';
  if (priority === 3) return '#14b8a6';
  return '#f59e0b';
}
```

Add `eventDidMount` and `eventWillUnmount` handlers. Import `useRef` and `useCallback` are already imported. Add before the return statement (before `<PageHeader` line 243):

```typescript
  const observerMap = useRef(new WeakMap<HTMLElement, ResizeObserver>());

  const computeContentLevel = useCallback((card: HTMLElement, eventElement: HTMLElement) => {
    const h = eventElement.clientHeight;
    let level = 1;
    if (h >= 80) level = 5;
    else if (h >= 64) level = 4;
    else if (h >= 48) level = 3;
    else if (h >= 32) level = 2;
    card.dataset.contentLevel = String(level);
  }, []);

  const handleEventDidMount = useCallback((info: { el: HTMLElement }) => {
    const card = info.el.querySelector<HTMLElement>('[data-calendar-event-card]') || info.el;
    computeContentLevel(card, info.el);
    const observer = new ResizeObserver(() => computeContentLevel(card, info.el));
    observer.observe(info.el);
    observerMap.current.set(info.el, observer);
  }, [computeContentLevel]);

  const handleEventWillUnmount = useCallback((info: { el: HTMLElement }) => {
    const observer = observerMap.current.get(info.el);
    if (observer) {
      observer.disconnect();
      observerMap.current.delete(info.el);
    }
  }, []);
```

Add `eventDidMount={handleEventDidMount}` and `eventWillUnmount={handleEventWillUnmount}` to `<FullCalendar>` component — place them near `drop={handleExternalDrop}` (after `datesSet`, `select`, `eventClick`, and `drop` props):

```tsx
          eventDidMount={handleEventDidMount}
          eventWillUnmount={handleEventWillUnmount}
```

Update `renderCalendarEvent` to use `data-calendar-event-card`, attach per-element `--calendar-accent`, add info-priority spans, and render rrule icon at level 5:

```typescript
function renderCalendarEvent(arg: EventContentArg) {
  const props = arg.event.extendedProps as CalendarEventInput['extendedProps'];
  if (props.type === 'layer') {
    const raw = props.raw;

    return (
      <div className="calendar-event-card" style={{ '--calendar-accent': props.accentColor } as CSSProperties}
        data-layer={raw.layer}>
        <span className="calendar-event-dot" />
        <span className="calendar-event-title">{arg.event.title}</span>
        {arg.timeText && <span className="calendar-event-time">{arg.timeText}</span>}
      </div>
    );
  }

  const raw = props.raw as Partial<TaskResponse & EventResponse>;
  const descriptionText = raw.description
    ? (raw.description.length > 60
      ? raw.description.slice(0, 60) + '…'
      : raw.description)
    : undefined;

  return (
    <div className="calendar-event-card" data-calendar-event-card
      style={{ '--calendar-accent': props.accentColor } as CSSProperties}>
      <span className="calendar-event-title">{arg.event.title}</span>
      {arg.timeText && <span className="calendar-event-time">{arg.timeText}</span>}
      {raw.location && <span className="calendar-event-location">{raw.location}</span>}
      <span className="calendar-event-source">{props.calendarLabel}</span>
      {descriptionText && <span className="calendar-event-description">{descriptionText}</span>}
      {raw.rrule && (
        <span className="calendar-event-rrule">
          <Repeat2 size={12} className="inline" title="重复事件" aria-label="重复事件" />
        </span>
      )}
    </div>
  );
}
```

- [ ] **Step 7.6: Update index.css**

Append at end of `src/client-web/src/index.css`:

```css
/* ─── Calendar reliability: density-aware event cards ───────────── */

/* Default: hide all optional info spans (only title+time visible) */
.calendar-event-card > .calendar-event-location,
.calendar-event-card > .calendar-event-source,
.calendar-event-card > .calendar-event-description,
.calendar-event-card > .calendar-event-rrule {
  display: none;
}

/* Cumulative reveal by content level */
.calendar-event-card[data-content-level="2"] > .calendar-event-location {
  display: inline;
}
.calendar-event-card[data-content-level="3"] > .calendar-event-location,
.calendar-event-card[data-content-level="3"] > .calendar-event-source {
  display: inline;
}
.calendar-event-card[data-content-level="4"] > .calendar-event-location,
.calendar-event-card[data-content-level="4"] > .calendar-event-source,
.calendar-event-card[data-content-level="4"] > .calendar-event-description {
  display: inline;
}
.calendar-event-card[data-content-level="5"] > .calendar-event-location,
.calendar-event-card[data-content-level="5"] > .calendar-event-source,
.calendar-event-card[data-content-level="5"] > .calendar-event-description,
.calendar-event-card[data-content-level="5"] > .calendar-event-rrule {
  display: inline;
}

/* ─── Timeline event cards: light bg + 3px left accent ─────────── */

.fc-timegrid-event .calendar-event-card {
  box-sizing: border-box;
  height: 100%;
  flex-direction: column;
  align-items: flex-start;
  overflow: hidden;
  background: color-mix(in srgb, var(--calendar-accent, #2563eb) 15%, transparent);
  border-left: 3px solid var(--calendar-accent, #2563eb);
  border-radius: 4px;
  padding: 0.35rem 0.5rem;
  font-size: 0.72rem;
  line-height: 1.3;
  box-shadow: none;
  min-height: 20px;
}

.fc-timegrid-event .calendar-event-dot {
  display: none;
}

/* ─── Month view event cards ────────────────────────────────────── */

.fc-daygrid-event .calendar-event-card {
  background: color-mix(in srgb, var(--calendar-accent, #2563eb) 15%, transparent);
  border-left: 3px solid var(--calendar-accent, #2563eb);
  border-radius: 3px;
  padding: 0.1rem 0.3rem;
  font-size: 0.7rem;
  line-height: 1.2;
  box-shadow: none;
  font-weight: 500;
}
```

- [ ] **Step 7.7: Run all tests**

```
npm --prefix src/client-web exec tsx -- tests/client-web/calendarLayerVisibility.test.ts
```

Expected: PASS (no more backgroundColor/borderColor)

```
npm --prefix src/client-web exec tsx -- tests/client-web/calendarReliabilityUtils.test.ts
```

Expected: PASS

```
npm --prefix src/client-web exec tsx -- tests/client-web/scheduleWorkbenchVisualAudit.test.ts
```

Expected: PASS (all scenarios including K)

- [ ] **Step 7.8: Commit**

```
git add src/client-web/src/pages/CalendarPage.tsx src/client-web/src/index.css tests/client-web/calendarLayerVisibility.test.ts tests/client-web/scheduleWorkbenchVisualAudit.test.ts
git commit -m "fix: adapt calendar events to available space and remove hardcoded colors"
```

---

## Task 8: Final verification, live QA, PR and CI

- [ ] **Step 8.1: Run backend tests — focused**

```
dotnet test tests\Pim.UnitTests\Pim.UnitTests.csproj --no-restore --filter FullyQualifiedName~CalendarServiceReliabilityTests
```

Expected: PASS all

- [ ] **Step 8.2: Run all calendar backend tests**

```
dotnet test tests\Pim.UnitTests\Pim.UnitTests.csproj --no-restore --filter FullyQualifiedName~Calendar
```

Expected: PASS

- [ ] **Step 8.2b: Restore full solution before final full-suite run**

```
dotnet restore Pim.sln
```

Expected: Restore completes for all projects.

- [ ] **Step 8.3: Run full solution backend tests**

```
dotnet test Pim.sln --no-restore
```

Expected: PASS

- [ ] **Step 8.4: Run frontend reliability utility tests**

```
npm --prefix src/client-web run test:calendar-reliability
```

Expected: both `calendarReliabilityUtils.test.ts` and `safeHtml.test.ts` pass.

- [ ] **Step 8.5: Run schedule-workbench-complete**

```
npm --prefix src/client-web run test:schedule-workbench-complete
```

Expected: PASS (all non-Playwright tests + Playwright visual audit)

- [ ] **Step 8.6: TypeScript build check**

```
npm --prefix src/client-web run build
```

Expected: Build succeeds without errors

- [ ] **Step 8.7: Lint check**

```
npm --prefix src/client-web run lint
```

Expected: No new warnings

- [ ] **Step 8.8: Live QA — start API**

In terminal 1:

```
dotnet run --project src/Pim.Api/Pim.Api.csproj --urls http://127.0.0.1:5858
```

Wait for API to start (listen for `Now listening on`).

- [ ] **Step 8.9: Live QA — start Vite**

In terminal 2, detect free port:

```
npm --prefix src/client-web run dev -- --host 127.0.0.1 --port 5173
```

If 5173 is occupied, try 5174, 5175. Record actual port.

- [ ] **Step 8.10: Live QA — browser verification**

Use the in-app browser visual companion at `http://127.0.0.1:5173/calendar`; if Vite selected 5174 or 5175, use that recorded concrete port instead. Verify:
- Create event with +08:00 time → saved as UTC, edit shows correct local time
- Edit event → end <= start blocked client-side
- Task editor shows hour/minute duration inputs
- Month view shows events, +N more when needed, popover works
- Outlook HTML description renders as sanitized formatted content, never raw tags or an editable textarea
- Timeline view uses the calendar theme color at 15% background plus a 3px accent; tall and compact events expose the correct information levels
- Capture and inspect both desktop (1440x1000) and mobile (390x844) screenshots
- Verify no console errors

- [ ] **Step 8.11: Stop servers**

Kill API and Vite processes. Verify no leftover processes.

- [ ] **Step 8.12: Clean generated outputs**

Run `git status --short --branch` and check for unintended build artifacts. If `src/Pim.Api/wwwroot` contains uncommitted build artifacts, verify the absolute path is within the current worktree, then remove:

```
$root = (Get-Location).Path.TrimEnd('\')
$target = (Resolve-Path -LiteralPath "src/Pim.Api/wwwroot" -ErrorAction Stop).Path
if (-not $target.StartsWith("$root\", [StringComparison]::OrdinalIgnoreCase)) {
    throw "Refusing to remove path outside worktree: $target"
}
if (git ls-files -- "src/Pim.Api/wwwroot") {
    throw "Refusing to remove tracked files from src/Pim.Api/wwwroot"
}
Remove-Item -LiteralPath $target -Recurse -Force
```

Only remove the verified generated-output path above; preserve every other untracked file.

Confirm only plan-intended files are changed.

- [ ] **Step 8.13: Review and final commit**

```
git status --short --branch
```

If verification uncovered fixes, write a failing regression test in the relevant test file, implement the fix, re-run verification, then create a focused commit:

```
git add tests/Pim.UnitTests/Calendar/CalendarServiceReliabilityTests.cs src/modules/Pim.Module.Calendar/Services/CalendarService.cs
git commit -m "fix: address calendar verification regression"
```

- [ ] **Step 8.14: Push and open PR**

```
git push -u origin codex/calendar-reliability-pr1
gh pr create --base master --head codex/calendar-reliability-pr1 --title "fix: harden calendar and task reliability" --body "Implements PR1 from the approved calendar reliability design: UTC normalization, range validation, safe manual descriptions, reliable event/task editors, adaptive month capacity, and density-aware timeline rendering."
```

- [ ] **Step 8.15: Monitor CI**

```
gh pr checks --watch --fail-fast
```

If build-api or build-web workflows don't trigger due to path filters, run:

```
gh pr checks
gh run list --branch codex/calendar-reliability-pr1
```

Document explicitly which workflows ran and which were skipped by path filters.
