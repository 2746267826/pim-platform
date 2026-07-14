# Microsoft 日历同步轻量 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 在不删减已确认用户功能的前提下，用适合个人项目的最小运行架构完成 Microsoft 日历发现、读取同步、直接写回和可操作的配置界面，并最大化复用现有代码。

**Architecture:** 保留现有实体、迁移、MSAL cache、Device Code session 和 named `HttpClient`。新运行链路只增加 `GraphCalendarClient`、新版 `OutlookCalendarSyncService`、`OutlookEventWriteService`、薄 `OutlookCalendarSyncJob` 和静态 `OutlookEventMapper`；API 继续集中在 `CalendarModule.cs`，Web 继续使用现有 `SyncPage` 与 `EventEditorDialog`。同步采用固定窗口 `calendarView` 对账，写回在用户确认的 HTTP 请求内直接调用 Graph。

**Tech Stack:** .NET 8、ASP.NET Core Minimal APIs、EF Core 8/Npgsql、MSAL、Hangfire、React 19、TanStack Query、TypeScript、xUnit、Playwright

---

## 实施边界

- 权威设计：`docs/superpowers/specs/2026-07-12-microsoft-calendar-sync-lightweight-design.md`。
- 直接复用：Microsoft 相关实体和迁移、`OutlookTokenCacheLock`、`OutlookTokenCacheStore`、`MsalPublicClientAdapter`、`MsalOutlookAuthCoordinator`、`OutlookAuthorizationSessionRunner`、named `HttpClient`。
- 不新增数据库迁移。`ConfirmationCount` 保持 `0`，批次成功状态使用 `completed`。
- 不实现或读取 delta、webhook、outbox、durable execution、自动合并、强制覆盖、确认中心写回或多实例恢复。
- 新同步/写回链路不读 `DeltaLink`、`SyncStrategy`、`BaselineWindowStart`、`BaselineWindowEnd`，也不写 `SyncConflictEntity` 或 `OutlookOperationExecutionEntity`。
- 旧 `OutlookSyncService`、`OutlookTokenService`、`MicrosoftGraphDeviceCodeClient`、`OutlookGraphModels`、`OutlookConflictService` 源码保留；新链路通过最终验收后只移除它们的运行注册和旧路由依赖。
- 本计划中的代码块用于锁定公开契约、关键分支和测试断言。实现时沿用相邻代码风格，不复制整文件。

## 文件结构

**新增运行文件（仅五个）：**

- `src/modules/Pim.Module.Calendar/Services/GraphCalendarClient.cs`
- `src/modules/Pim.Module.Calendar/Services/OutlookCalendarSyncService.cs`
- `src/modules/Pim.Module.Calendar/Services/OutlookEventWriteService.cs`
- `src/modules/Pim.Module.Calendar/Services/OutlookCalendarSyncJob.cs`
- `src/modules/Pim.Module.Calendar/Services/OutlookEventMapper.cs`

**主要修改文件：**

- `src/modules/Pim.Module.Calendar/DTOs/OutlookSyncDtos.cs`
- `src/modules/Pim.Module.Calendar/DTOs/CalendarDtos.cs`
- `src/modules/Pim.Module.Calendar/Services/CalendarService.cs`
- `src/modules/Pim.Module.Calendar/CalendarModule.cs`
- `src/client-web/src/api/calendar.ts`
- `src/client-web/src/types/index.ts`
- `src/client-web/src/pages/SyncPage.tsx`
- `src/client-web/src/pages/CalendarPage.tsx`
- `src/client-web/src/dialogs/EventEditorDialog.tsx`
- `src/client-web/package.json`
- `src/client-web/package-lock.json`

**新增测试/验收文件：**

- `tests/Pim.UnitTests/Calendar/OutlookEventMapperTests.cs`
- `tests/Pim.UnitTests/Calendar/GraphCalendarClientTests.cs`
- `tests/Pim.UnitTests/Calendar/OutlookCalendarSyncServiceTests.cs`
- `tests/Pim.UnitTests/Calendar/OutlookEventWriteServiceTests.cs`
- `tests/Pim.UnitTests/Calendar/OutlookCalendarSyncJobTests.cs`
- `tests/Pim.UnitTests/Calendar/OutlookCalendarApiContractTests.cs`
- `tests/client-web/microsoftCalendarSyncApi.test.ts`
- `tests/client-web/microsoftCalendarSyncUi.test.ts`
- `tests/client-web/outlookEventWritebackUi.test.ts`
- `docs/operations/microsoft-calendar-sync-acceptance.md`

### Task 1: 固定 DTO 与事件映射契约

**Files:**

- Modify: `src/modules/Pim.Module.Calendar/DTOs/OutlookSyncDtos.cs`
- Modify: `src/modules/Pim.Module.Calendar/DTOs/CalendarDtos.cs`
- Create: `src/modules/Pim.Module.Calendar/Services/OutlookEventMapper.cs`
- Create: `tests/Pim.UnitTests/Calendar/OutlookEventMapperTests.cs`
- Test: `tests/Pim.UnitTests/Calendar/OutlookPersistenceModelTests.cs`

- [ ] **Step 1: 写映射失败测试**

测试必须覆盖 UTC 定时事件、上海展示所需的原始时区、全天排他结束日期、series master/occurrence、ETag/changeKey 和写回 payload 不包含 recurrence 修改：

```csharp
[Fact]
public void ApplyGraphEvent_PreservesUtcAllDayAndRecurrenceMetadata()
{
    using var json = JsonDocument.Parse(GraphSamples.AllDayOccurrence);
    var target = new EventEntity();

    OutlookEventMapper.ApplyGraphEvent(
        target, json.RootElement, bindingId, pimCalendarId, connectionId, generation);

    Assert.Equal("event-1", target.OutlookEventId);
    Assert.Equal(new DateOnly(2026, 7, 12), target.AllDayStartDate);
    Assert.Equal(new DateOnly(2026, 7, 13), target.AllDayEndDateExclusive);
    Assert.Equal("Asia/Shanghai", target.OriginalStartTimeZone);
    Assert.Equal("occurrence", target.OutlookEventType);
    Assert.Equal("series-1", target.OutlookSeriesMasterId);
    Assert.Equal("etag-1", target.OutlookEtag);
    Assert.Equal(generation, target.LastSeenSyncGeneration);
}

[Fact]
public void BuildWritePayload_DoesNotEmitRecurrence()
{
    var payload = OutlookEventMapper.BuildWritePayload(Draft(), transactionId: "op-1");
    Assert.Equal("op-1", payload["transactionId"]);
    Assert.DoesNotContain("recurrence", payload.Keys);
}

[Fact]
public void ApplyGraphEvent_MapsSeriesMasterWithoutParentId()
{
    using var json = JsonDocument.Parse(GraphSamples.SeriesMaster);
    var target = new EventEntity();
    OutlookEventMapper.ApplyGraphEvent(
        target, json.RootElement, bindingId, pimCalendarId, connectionId, generation);
    Assert.Equal("seriesMaster", target.OutlookEventType);
    Assert.Null(target.OutlookSeriesMasterId);
    Assert.NotEqual("{}", target.GraphRecurrenceJson);
}
```

- [ ] **Step 2: 运行测试确认失败**

Run: `dotnet test tests/Pim.UnitTests/Pim.UnitTests.csproj --filter "FullyQualifiedName~OutlookEventMapperTests"`

Expected: FAIL，提示 `OutlookEventMapper` 不存在。

- [ ] **Step 3: 增加新链路 DTO 和静态 mapper**

在 `OutlookSyncDtos.cs` 集中放置新链路 DTO，不再创建额外 DTO 服务或模型层：

```csharp
public sealed record OutlookCalendarBindingResponse(
    Guid Id, Guid PimCalendarId, string GraphCalendarId,
    string? GroupId, string? GroupName, string Name, string? Color,
    string? OwnerName, string? OwnerAddress,
    bool IsDefault, bool CanEdit, bool IsSelected, string RemoteState,
    DateTimeOffset? LastSyncedAt, string? LastError);

public sealed record OutlookSyncRequest(
    string Mode,
    IReadOnlyList<Guid>? CalendarBindingIds = null,
    DateTimeOffset? RangeStart = null,
    DateTimeOffset? RangeEnd = null,
    Guid? RetryOfBatchId = null);

public sealed record OutlookWriteRequest(
    string Operation,
    Guid CalendarBindingId,
    Guid? EventId,
    CreateEventRequest? Draft,
    string Scope,
    Guid ClientOperationId,
    string? ExpectedEtag = null);

public sealed record OutlookWriteResult(
    string Status,
    EventResponse? Event,
    string? LatestOutlookJson,
    string? LatestEtag,
    string? ErrorCode,
    string? ErrorMessage);

public sealed record UpdateOutlookClientIdRequest(Guid ClientId);

public sealed record OutlookAuthorizationSessionRequest(Guid SessionId);

public sealed record OutlookLocalDataPreview(
    int BindingCount, int CalendarCount, int EventCount);
```

给现有 `OutlookSettingsResponse` 追加 `UiStatus` 和可选 `ActiveAuthorization`。`UiStatus` 只由 endpoint 读取连接与最新 session 后派生，不写入数据库：Client ID 为空为 `not-configured`，活动 session 为 `waiting-auth`，其余映射到 `connected`、`reauth-required` 或 `failed`。

给现有 `OutlookSyncBatchResponse` 尾部追加可选 `Mode`、`RequestedWindowStart`、`RequestedWindowEnd`、`PerCalendarJson`、`CancelRequested`，供同一个分页历史接口显示逐日历进度、删除计数和失败日历重试；不新增批次表字段。

保留旧后端 `UpdateOutlookSettingsRequest` 和 `OutlookDeviceCodePollRequest` 给 legacy 源码编译；新 endpoints 使用 `UpdateOutlookClientIdRequest` 与 `OutlookAuthorizationSessionRequest`。前端同名 TypeScript settings request 可以只含 `clientId`，它不是 legacy CLR 类型。

`OutlookEventMapper` 只做纯映射，不访问数据库：

```csharp
public static class OutlookEventMapper
{
    public static void ApplyGraphEvent(
        EventEntity target, JsonElement graph, Guid bindingId,
        Guid calendarId, Guid connectionId, Guid generation)
    {
        target.CalendarId = calendarId;
        target.OutlookConnectionId = connectionId;
        target.OutlookCalendarBindingId = bindingId;
        target.OutlookEventId = graph.GetProperty("id").GetString();
        target.DtStart = ParseUtc(graph.GetProperty("start"));
        target.DtEnd = ParseUtc(graph.GetProperty("end"));
        target.IsAllDay = graph.GetProperty("isAllDay").GetBoolean();
        target.AllDayStartDate = target.IsAllDay ? ParseDate(graph, "start") : null;
        target.AllDayEndDateExclusive = target.IsAllDay ? ParseDate(graph, "end") : null;
        target.OriginalStartTimeZone = ReadString(graph, "originalStartTimeZone");
        target.OriginalEndTimeZone = ReadString(graph, "originalEndTimeZone");
        target.OutlookEventType = ReadString(graph, "type");
        target.OutlookSeriesMasterId = ReadString(graph, "seriesMasterId");
        target.OutlookChangeKey = ReadString(graph, "changeKey");
        target.OutlookEtag = ReadString(graph, "@odata.etag");
        target.GraphRecurrenceJson = ReadJsonObject(graph, "recurrence", "{}");
        target.LastSeenSyncGeneration = generation;
        target.Source = "outlook";
    }

    public static Dictionary<string, object?> BuildWritePayload(
        CreateEventRequest draft, string? transactionId)
    {
        var payload = new Dictionary<string, object?>
        {
            ["subject"] = draft.Title,
            ["body"] = new { contentType = "text", content = draft.Description ?? "" },
            ["location"] = new { displayName = draft.Location ?? "" },
            ["start"] = GraphDateTime(draft.DtStart, draft.IsAllDay),
            ["end"] = GraphDateTime(draft.DtEnd, draft.IsAllDay),
            ["isAllDay"] = draft.IsAllDay
        };
        if (transactionId is not null) payload["transactionId"] = transactionId;
        return payload;
    }
}
```

给 `CalendarResponse` 和 `EventResponse` 追加可选 Outlook 字段，保持现有构造调用兼容：

```csharp
string Source = "manual",
Guid? OutlookCalendarBindingId = null,
bool CanEdit = true
```

可选位置参数只能追加在 record 尾部。先运行 `rg -n "new CalendarResponse\\(|new EventResponse\\(" src tests` 核对现有构造点，再以全量编译证明六参数 `CalendarResponse` 调用仍兼容；不要把 record 改为另一种模型形态。

以及：

```csharp
Guid? OutlookCalendarBindingId = null,
string? OutlookEventId = null,
string? OutlookEtag = null,
string? OutlookEventType = null
```

- [ ] **Step 4: 运行映射和持久化回归测试**

Run: `dotnet test tests/Pim.UnitTests/Pim.UnitTests.csproj --filter "FullyQualifiedName~OutlookEventMapperTests|FullyQualifiedName~OutlookPersistenceModelTests"`

Expected: PASS；EF 模型无迁移变化。

- [ ] **Step 5: 提交**

```powershell
git add src/modules/Pim.Module.Calendar/DTOs/OutlookSyncDtos.cs src/modules/Pim.Module.Calendar/DTOs/CalendarDtos.cs src/modules/Pim.Module.Calendar/Services/OutlookEventMapper.cs tests/Pim.UnitTests/Calendar/OutlookEventMapperTests.cs
git commit -m "feat: add lightweight outlook mapping contracts"
```

### Task 2: 实现 GraphCalendarClient

**Files:**

- Create: `src/modules/Pim.Module.Calendar/Services/GraphCalendarClient.cs`
- Modify: `tests/Pim.UnitTests/Calendar/OutlookGraphTestDoubles.cs`
- Create: `tests/Pim.UnitTests/Calendar/GraphCalendarClientTests.cs`

- [ ] **Step 1: 写 Graph HTTP 边界失败测试**

```csharp
[Theory]
[InlineData("https://graph.microsoft.com/v1.0/me/calendars", true)]
[InlineData("https://graph.microsoft.com/v1.0/me/drive", false)]
[InlineData("https://example.com/v1.0/me/calendars", false)]
public void NextLinkWhitelist_IsExact(string value, bool expected)
    => Assert.Equal(expected, GraphCalendarClient.IsAllowedNextLink(value));

[Fact]
public async Task Read_Retries429ButWriteDoesNotRetry()
{
    var handler = new ScriptedGraphHandler(
        Response(429, retryAfterSeconds: 0), Response(200, CalendarPage), Response(503));
    var client = CreateClient(handler);

    await foreach (var _ in client.GetCalendarsAsync(connectionId, CancellationToken.None)) { }
    await Assert.ThrowsAsync<GraphRequestException>(() =>
        client.DeleteEventAsync(connectionId, "cal-1", "event-1", "etag-1", CancellationToken.None));

    Assert.Equal(3, handler.Requests.Count);
}
```

在同一步把 `ScriptedGraphHandler`、`Response(...)` 和 JSON samples 加入 `OutlookGraphTestDoubles.cs`，使首次失败只来自尚未实现的 production client/exception，而不是缺测试 helper。

同一测试类还要断言：每次尝试 30 秒超时、读取总计最多 3 次、`Retry-After` 有界、读写遇到 401 都只 force refresh 并重放一次且第二次 401 抛出现有 `OutlookReauthenticationRequiredException`、写请求对网络/408/429/5xx 不透明重试、普通 4xx 不重试、取消向上传播、每个请求包含两个 `Prefer` header、DELETE 404 返回幂等成功；捕获日志不得包含 access token、Authorization、device/user code、MSAL cache、事件正文或完整 JSON payload。

- [ ] **Step 2: 运行测试确认失败**

Run: `dotnet test tests/Pim.UnitTests/Pim.UnitTests.csproj --filter "FullyQualifiedName~GraphCalendarClientTests"`

Expected: FAIL，提示 `GraphCalendarClient` / `GraphRequestException` 尚未实现。

- [ ] **Step 3: 实现单一 Graph 客户端**

公开契约固定为逐页读取和无透明重试的写操作：

```csharp
public sealed record GraphPage(IReadOnlyList<JsonElement> Items, string? NextLink);

public sealed class GraphRequestException : HttpRequestException
{
    public GraphRequestException(HttpStatusCode statusCode, string message)
        : base(message, null, statusCode) { }
}

public sealed class GraphCalendarClient
{
    public Task<JsonElement> GetMeAsync(Guid connectionId, CancellationToken ct)
        => ReadJsonAsync(connectionId, "/me?$select=id,displayName,userPrincipalName", ct);

    public IAsyncEnumerable<GraphPage> GetCalendarGroupsAsync(Guid connectionId, CancellationToken ct)
        => ReadPagesAsync(connectionId, "/me/calendarGroups?$select=id,name", ct);

    public IAsyncEnumerable<GraphPage> GetGroupCalendarsAsync(Guid connectionId, string groupId, CancellationToken ct)
        => ReadPagesAsync(connectionId,
            $"/me/calendarGroups/{Escape(groupId)}/calendars?$select=id,name,color,owner,isDefaultCalendar,canEdit,canViewPrivateItems", ct);

    public IAsyncEnumerable<GraphPage> GetCalendarsAsync(Guid connectionId, CancellationToken ct)
        => ReadPagesAsync(connectionId,
            "/me/calendars?$select=id,name,color,owner,isDefaultCalendar,canEdit,canViewPrivateItems", ct);

    public IAsyncEnumerable<GraphPage> GetCalendarViewAsync(
        Guid connectionId, string calendarId, DateTimeOffset start, DateTimeOffset end, CancellationToken ct)
        => ReadPagesAsync(connectionId, CalendarViewPath(calendarId, start, end), ct);

    public IAsyncEnumerable<GraphPage> GetEventsAsync(Guid connectionId, string calendarId, CancellationToken ct)
        => ReadPagesAsync(connectionId, $"/me/calendars/{Escape(calendarId)}/events", ct);

    public Task<JsonElement?> GetEventAsync(
        Guid connectionId, string calendarId, string eventId, CancellationToken ct)
        => ReadJsonOrNullAsync(
            connectionId, $"/me/calendars/{Escape(calendarId)}/events/{Escape(eventId)}", ct);

    public Task<JsonElement> CreateEventAsync(
        Guid connectionId, string calendarId,
        IReadOnlyDictionary<string, object?> payload, CancellationToken ct)
        => SendWriteJsonAsync(
            connectionId, HttpMethod.Post, $"/me/calendars/{Escape(calendarId)}/events",
            null, payload, ct);

    public Task<JsonElement> UpdateEventAsync(
        Guid connectionId, string calendarId, string eventId, string etag,
        IReadOnlyDictionary<string, object?> payload, CancellationToken ct)
        => SendWriteJsonAsync(
            connectionId, HttpMethod.Patch,
            $"/me/calendars/{Escape(calendarId)}/events/{Escape(eventId)}",
            etag, payload, ct);

    public Task DeleteEventAsync(
        Guid connectionId, string calendarId, string eventId, string etag, CancellationToken ct)
        => SendDeleteAsync(
            connectionId, $"/me/calendars/{Escape(calendarId)}/events/{Escape(eventId)}", etag, ct);
}
```

所有重试使用 request factory 重建 `HttpRequestMessage`，禁止重复发送已经消费的 request/content：

```csharp
private async Task<HttpResponseMessage> SendReadAsync(
    Guid connectionId, Func<string, HttpRequestMessage> requestFactory, CancellationToken ct)
{
    var forceRefresh = false;
    for (var attempt = 1; attempt <= 3; attempt++)
    {
        var token = await _tokens.AcquireAccessTokenAsync(connectionId, forceRefresh, ct);
        using var request = requestFactory(token);
        using var timeout = CancellationTokenSource.CreateLinkedTokenSource(ct);
        timeout.CancelAfter(TimeSpan.FromSeconds(30));
        HttpResponseMessage response;
        try
        {
            response = await _http.CreateClient("outlook").SendAsync(request, timeout.Token);
        }
        catch (OperationCanceledException) when (!ct.IsCancellationRequested && attempt < 3)
        {
            continue;
        }
        catch (HttpRequestException) when (attempt < 3)
        {
            await DelayAsync(null, attempt, ct);
            continue;
        }
        if (response.StatusCode == HttpStatusCode.Unauthorized && !forceRefresh)
        {
            response.Dispose();
            forceRefresh = true;
            continue;
        }
        if (response.StatusCode == HttpStatusCode.Unauthorized)
        {
            response.Dispose();
            throw new OutlookReauthenticationRequiredException("graph-unauthorized");
        }
        if (IsRetryable(response.StatusCode) && attempt < 3)
        {
            var retryAfter = response.Headers.RetryAfter;
            response.Dispose();
            await DelayAsync(retryAfter, attempt, ct);
            continue;
        }
        return EnsureAllowedResult(response);
    }
    throw new InvalidOperationException("Graph read retry budget exhausted.");
}
```

写 helper 使用相同的 30 秒 timeout 和 request factory，但策略只有 401 单次重放：

```csharp
private async Task<HttpResponseMessage> SendWriteAsync(
    Guid connectionId,
    Func<string, HttpRequestMessage> requestFactory,
    bool allowNotFound,
    CancellationToken ct)
{
    for (var forceRefresh = false;; forceRefresh = true)
    {
        var token = await _tokens.AcquireAccessTokenAsync(connectionId, forceRefresh, ct);
        using var request = requestFactory(token);
        using var timeout = CancellationTokenSource.CreateLinkedTokenSource(ct);
        timeout.CancelAfter(TimeSpan.FromSeconds(30));
        var response = await _http.CreateClient("outlook").SendAsync(request, timeout.Token);
        if (response.StatusCode == HttpStatusCode.Unauthorized && !forceRefresh)
        {
            response.Dispose();
            continue;
        }
        if (response.StatusCode == HttpStatusCode.Unauthorized)
        {
            response.Dispose();
            throw new OutlookReauthenticationRequiredException("graph-unauthorized");
        }
        if (allowNotFound && response.StatusCode == HttpStatusCode.NotFound)
            return response;
        if (!response.IsSuccessStatusCode)
        {
            var statusCode = response.StatusCode;
            response.Dispose();
            throw new GraphRequestException(statusCode, "Microsoft Graph write failed.");
        }
        return response;
    }
}

private static HttpRequestMessage BuildWriteRequest(
    HttpMethod method, string path, string token, string? etag, string? json)
{
    var request = new HttpRequestMessage(method, path);
    request.Headers.Authorization = new AuthenticationHeaderValue("Bearer", token);
    if (etag is not null) request.Headers.TryAddWithoutValidation("If-Match", etag);
    if (json is not null)
        request.Content = new StringContent(json, Encoding.UTF8, "application/json");
    return request;
}
```

`SendWriteJsonAsync` 每次通过 `BuildWriteRequest` 重新序列化 body，读取成功响应为 `JsonElement`；`SendDeleteAsync` 传 `allowNotFound: true` 并把 204/404 都返回为成功。`EnsureAllowedResult` 把其他非成功状态转换成上面的 `GraphRequestException`，412 的 `StatusCode` 供写回服务分支处理。

`CreateEventAsync`、`UpdateEventAsync`、`DeleteEventAsync` 对网络/408/429/5xx 不重试；只有 401 可以在强制静默刷新后安全重放一次。`transactionId` 在 POST JSON body 中，不放 header。nextLink 只接受 `https://graph.microsoft.com/v1.0/` 且路径仍属于本组件允许的 calendar 端点。

`CalendarViewPath`、`GetEventsAsync` 和单事件 GET 都追加同一最小 `$select`：`id,subject,body,start,end,location,isAllDay,type,seriesMasterId,recurrence,iCalUId,changeKey,originalStartTimeZone,originalEndTimeZone`；ETag 从 `@odata.etag` 保留。

`OutlookGraphTestDoubles.cs` 只追加基于 `HttpMessageHandler` 的 `ScriptedGraphHandler`；它不得实现旧 `IMicrosoftGraphClient`，也不得引用 `GraphDeltaPage` 等 legacy models。

- [ ] **Step 4: 运行 Graph client 测试**

Run: `dotnet test tests/Pim.UnitTests/Pim.UnitTests.csproj --filter "FullyQualifiedName~GraphCalendarClientTests"`

Expected: PASS。

- [ ] **Step 5: 提交**

```powershell
git add src/modules/Pim.Module.Calendar/Services/GraphCalendarClient.cs tests/Pim.UnitTests/Calendar/GraphCalendarClientTests.cs tests/Pim.UnitTests/Calendar/OutlookGraphTestDoubles.cs
git commit -m "feat: add graph calendar client"
```

### Task 3: 日历发现与选择

**Files:**

- Create: `src/modules/Pim.Module.Calendar/Services/OutlookCalendarSyncService.cs`
- Create: `tests/Pim.UnitTests/Calendar/OutlookCalendarSyncServiceTests.cs`
- Modify: `src/modules/Pim.Module.Calendar/Services/CalendarService.cs`

- [ ] **Step 1: 写发现和选择失败测试**

```csharp
[Fact]
public async Task DiscoverAsync_MergesGroupsAndRootAndDefaultsNewBindingsToSelected()
{
    var result = await service.DiscoverAsync(userId, CancellationToken.None);
    Assert.Equal(new[] { "course-calendar", "default-calendar", "root-only" },
        result.Select(x => x.GraphCalendarId).Order());
    Assert.All(result, item => Assert.True(item.IsSelected));
    Assert.Contains(result, item => item.GroupName == "课程表" && item.GraphCalendarId == "course-calendar");
}

[Fact]
public async Task FailedDiscovery_DoesNotMarkExistingBindingMissing()
{
    graph.FailPage("/me/calendarGroups/group-1/calendars", page: 2);
    await Assert.ThrowsAsync<GraphRequestException>(() => service.DiscoverAsync(userId, CancellationToken.None));
    Assert.Equal("active", await ReadBindingStateAsync("existing-calendar"));
}

[Fact]
public async Task SetSelection_HidesCalendarWithoutDeletingEvents()
{
    await service.SetSelectionAsync(userId, new[] { selectedBindingId }, CancellationToken.None);
    Assert.False((await LoadBindingAsync(pausedBindingId)).IsSelected);
    Assert.False((await LoadPimCalendarAsync(pausedBindingId)).IsVisible);
    Assert.Null((await LoadEventAsync(pausedBindingId)).DeletedAt);
}
```

再覆盖根日历分页、Graph ID 去重、只读 `CanEdit=false`、首次创建一个独立 `CalendarEntity`、Graph 枚举颜色映射为 `CalendarEntity.Color` 可接受的 7 位 hex、重新发现保留用户选择、全部分页成功后才标 `remote-missing`、跨用户 binding ID 拒绝。

- [ ] **Step 2: 运行测试确认失败**

Run: `dotnet test tests/Pim.UnitTests/Pim.UnitTests.csproj --filter "FullyQualifiedName~OutlookCalendarSyncServiceTests&Name~Discover|FullyQualifiedName~OutlookCalendarSyncServiceTests&Name~Selection"`

Expected: FAIL，提示新版同步服务不存在。

- [ ] **Step 3: 实现发现和选择最小切片**

构造函数只注入现有基础设施和 Task 2 客户端：

```csharp
public OutlookCalendarSyncService(
    PimDbContext db,
    GraphCalendarClient graph,
    TimeProvider timeProvider,
    ILogger<OutlookCalendarSyncService> logger)
```

公开方法：

```csharp
public Task<IReadOnlyList<OutlookCalendarBindingResponse>> DiscoverAsync(Guid userId, CancellationToken ct);
public Task SetSelectionAsync(Guid userId, IReadOnlyCollection<Guid> selectedBindingIds, CancellationToken ct);
public Task<IReadOnlyList<OutlookCalendarBindingResponse>> ListCalendarsAsync(Guid userId, CancellationToken ct);
```

发现顺序固定为 group pages -> each group calendar pages -> root calendar pages。按 `GraphCalendarId` 去重后 upsert；只有 `discoveryCompleted == true` 才执行：

```csharp
foreach (var missing in existing.Where(x => !seenIds.Contains(x.GraphCalendarId)))
    missing.RemoteState = "remote-missing";
```

`SetSelectionAsync` 同时更新 `binding.IsSelected` 与对应 `CalendarEntity.IsVisible`，不删除任何行。`CalendarService.GetCalendarsAsync` 的投影补充 `Source`、binding ID 和 `CanEdit`，供编辑器识别 Outlook 日历。

`CalendarEntity` 没有 binding 导航，投影必须按 `CalendarEntity.Id == OutlookCalendarBindingEntity.PimCalendarId` 做 left join：

```csharp
var query =
    from calendar in _db.Set<CalendarEntity>()
    join binding in _db.Set<OutlookCalendarBindingEntity>()
        on calendar.Id equals binding.PimCalendarId into bindingGroup
    from binding in bindingGroup.DefaultIfEmpty()
    where calendar.UserId == UserId
    select new CalendarResponse(
        calendar.Id, calendar.Name, calendar.Color, calendar.Kind,
        calendar.IsDefault, calendar.Events.Count, calendar.Source,
        binding == null ? null : binding.Id,
        binding == null || binding.CanEdit);
```

不要假设 `calendar.OutlookCalendarBinding` 导航存在。

`binding.Color` 保留 Graph 原始颜色名；PIM calendar 使用服务内静态字典映射为 `#RRGGBB`，未知/`auto` 回退现有默认蓝色，不新增颜色服务。

- [ ] **Step 4: 运行发现、选择和现有日历测试**

Run: `dotnet test tests/Pim.UnitTests/Pim.UnitTests.csproj --filter "FullyQualifiedName~OutlookCalendarSyncServiceTests|FullyQualifiedName~CalendarServiceUiCreationTests"`

Expected: PASS。

- [ ] **Step 5: 提交**

```powershell
git add src/modules/Pim.Module.Calendar/Services/OutlookCalendarSyncService.cs src/modules/Pim.Module.Calendar/Services/CalendarService.cs tests/Pim.UnitTests/Calendar/OutlookCalendarSyncServiceTests.cs
git commit -m "feat: discover and select outlook calendars"
```

### Task 4: 普通同步、历史与连接锁

**Files:**

- Modify: `src/modules/Pim.Module.Calendar/Services/OutlookCalendarSyncService.cs`
- Modify: `tests/Pim.UnitTests/Calendar/OutlookCalendarSyncServiceTests.cs`

- [ ] **Step 1: 写普通同步失败测试**

必须包含以下命名测试和核心断言：

```csharp
[Fact]
public async Task SyncAsync_ComputesWindowOnceForWholeBatch()
{
    time.SetUtcNow(new DateTimeOffset(2026, 7, 12, 0, 0, 0, TimeSpan.Zero));
    await service.SyncAsync(userId, new OutlookSyncRequest("normal"), CancellationToken.None);

    Assert.All(graph.CalendarViewRequests, request =>
    {
        Assert.Equal(new DateTimeOffset(2026, 4, 13, 0, 0, 0, TimeSpan.Zero), request.Start);
        Assert.Equal(new DateTimeOffset(2027, 7, 12, 0, 0, 0, TimeSpan.Zero), request.End);
    });
    Assert.Equal(graph.CalendarViewRequests[0].Start, storedBatch.RequestedWindowStart);
    Assert.Equal(graph.CalendarViewRequests[0].End, storedBatch.RequestedWindowEnd);
}

[Fact]
public async Task MissingEvent_IsDeletedOnlyAfterCompletePagingAndGet404()
{
    graph.ReturnTwoCompletePages();
    graph.ReturnEventNotFound("missing-event");
    await RunNormalSyncAsync();
    Assert.NotNull((await LoadEventAsync("missing-event")).DeletedAt);
}

[Fact]
public async Task FailedPage_NeverInfersDeletion()
{
    graph.FailCalendarViewPage(2);
    await RunNormalSyncAsync();
    Assert.Null((await LoadEventAsync("missing-event")).DeletedAt);
    Assert.Equal("partial", storedBatch.Status);
}
```

同类还需覆盖：逐页 `SaveChangesAsync`、幂等 upsert、相同 Immutable ID 跨 binding 移动且不新增行（源日历先处理、目标日历先处理两种顺序）、单日历失败继续、GET 仍存在则更新、权限错误保留、只处理 selected+active、锁内自动任务跳过、手动任务返回当前批次、旧 running 批次标 `interrupted`、变化/失败事件仅存 ID+标题、`ConfirmationCount == 0`。

- [ ] **Step 2: 运行测试确认失败**

Run: `dotnet test tests/Pim.UnitTests/Pim.UnitTests.csproj --filter "FullyQualifiedName~OutlookCalendarSyncServiceTests&Name~Sync"`

Expected: FAIL，缺少 `SyncAsync` 与普通同步实现。

- [ ] **Step 3: 实现固定窗口对账**

批次开始只读取一次时钟：

```csharp
var now = _timeProvider.GetUtcNow();
var windowStart = now.AddDays(-90);
var windowEnd = now.AddDays(365);
var batch = NewBatch(userId, connection.Id, "normal", windowStart, windowEnd);
```

连接锁保持在服务内部，不新增 lock service：

```csharp
private static readonly ConcurrentDictionary<Guid, SemaphoreSlim> ConnectionLocks = new();
```

每个 binding 顺序执行。每页先按 `(OutlookCalendarBindingId, OutlookEventId)` upsert，再调用一次 `SaveChangesAsync`。跨 binding lookup 按同 connection + Immutable ID 查询并使用 `IgnoreQueryFilters()` 包含刚被旧日历 missing verification 软删的行；命中时清空 `DeletedAt`，同时修改 `CalendarId` 与 `OutlookCalendarBindingId`。该页仍只调用一次 `SaveChangesAsync`，依赖 EF Core 隐式事务。

只有枚举完全部 `calendarView` 页面后执行 missing verification：

```csharp
var missing = await WindowEvents(binding.Id, windowStart, windowEnd)
    .Where(x => x.LastSeenSyncGeneration != generation)
    .ToListAsync(ct);
foreach (var local in missing)
{
    var remote = await _graph.GetEventAsync(connection.Id, binding.GraphCalendarId, local.OutlookEventId!, ct);
    if (remote is null) local.DeletedAt = now;
    else OutlookEventMapper.ApplyGraphEvent(local, remote.Value, binding.Id, binding.PimCalendarId, connection.Id, generation);
}
```

批次状态规则固定：全部成功 `completed`；部分日历失败 `partial`；全部失败 `failed`；用户取消 `canceled`；服务重启遗留 `running` 改 `interrupted`。`PerCalendarJson`、`StepsJson`、`ErrorsJson` 保存结构化摘要，事件正文和完整 payload 不进入历史。

捕获 `OutlookReauthenticationRequiredException` 时，把 connection 更新为 `Status="reauth-required"`、`TokenHealth="interaction-required"` 并停止该连接同步；Graph client 自身仍不保存业务状态。

- [ ] **Step 4: 运行同步测试**

Run: `dotnet test tests/Pim.UnitTests/Pim.UnitTests.csproj --filter "FullyQualifiedName~OutlookCalendarSyncServiceTests"`

Expected: PASS。

- [ ] **Step 5: 提交**

```powershell
git add src/modules/Pim.Module.Calendar/Services/OutlookCalendarSyncService.cs tests/Pim.UnitTests/Calendar/OutlookCalendarSyncServiceTests.cs
git commit -m "feat: reconcile outlook calendar windows"
```

### Task 5: 深度同步、取消与失败日历重试

**Files:**

- Modify: `src/modules/Pim.Module.Calendar/Services/OutlookCalendarSyncService.cs`
- Modify: `tests/Pim.UnitTests/Calendar/OutlookCalendarSyncServiceTests.cs`

- [ ] **Step 1: 写深度同步失败测试**

```csharp
[Fact]
public async Task FullResources_OnlyUpsertsAndNeverDeletesMissingEvents()
{
    await service.SyncAsync(userId, new OutlookSyncRequest("full-resources", new[] { bindingId }), ct);
    Assert.Equal("/events", graph.Requests.Single().Kind);
    Assert.Null((await LoadEventAsync("not-returned")).DeletedAt);
}

[Fact]
public async Task RangeInstances_UsesAtMost180DayChunksAndDeduplicatesIds()
{
    await service.SyncAsync(userId, new OutlookSyncRequest(
        "range-instances", new[] { bindingId }, RangeStart(), RangeEnd()), ct);
    Assert.All(graph.CalendarViewRequests, x => Assert.True(x.End - x.Start <= TimeSpan.FromDays(180)));
    Assert.Equal(1, await CountEventsAsync("duplicate-across-chunks"));
}

[Fact]
public async Task Retry_CreatesNewBatchLinkedToOriginal()
{
    var retry = await service.SyncAsync(userId,
        new OutlookSyncRequest("normal", new[] { failedBindingId }, RetryOfBatchId: originalBatchId), ct);
    Assert.NotEqual(originalBatchId, retry.Id);
    Assert.Contains(originalBatchId.ToString(), retry.PerCalendarJson);
}
```

补充取消测试：设置 `batch.CancelRequested=true` 后，下一页/分片边界停止，已提交页保留，批次为 `canceled`。

- [ ] **Step 2: 运行测试确认失败**

Run: `dotnet test tests/Pim.UnitTests/Pim.UnitTests.csproj --filter "FullyQualifiedName~OutlookCalendarSyncServiceTests&Name~FullResources|FullyQualifiedName~OutlookCalendarSyncServiceTests&Name~RangeInstances|FullyQualifiedName~OutlookCalendarSyncServiceTests&Name~Retry"`

Expected: FAIL，缺少深度模式和重试关联。

- [ ] **Step 3: 实现三个模式的共用批次入口**

```csharp
public Task<OutlookSyncBatchEntity> SyncAsync(Guid userId, OutlookSyncRequest request, CancellationToken ct)
    => request.Mode switch
    {
        "normal" => RunNormalAsync(userId, request, ct),
        "full-resources" => RunFullResourcesAsync(userId, request, ct),
        "range-instances" => RunRangeInstancesAsync(userId, request, ct),
        _ => throw new DomainException(02009, "不支持的 Microsoft 同步模式。")
    };
```

range 分片使用确定的半开区间，避免边界重复：

```csharp
var rangeEnd = request.RangeEnd!.Value;
for (var start = request.RangeStart!.Value; start < rangeEnd;)
{
    var end = start.AddDays(180) < rangeEnd ? start.AddDays(180) : rangeEnd;
    await UpsertCalendarViewRangeAsync(binding, start, end, seenIds, batch, ct);
    start = end;
}
```

每页和每分片前重读 `CancelRequested`。人工重试只接受原批次中失败的 binding，创建新批次并在 JSON 摘要保存 `retryOfBatchId`；不建设后台重试队列。

- [ ] **Step 4: 运行同步服务全部测试**

Run: `dotnet test tests/Pim.UnitTests/Pim.UnitTests.csproj --filter "FullyQualifiedName~OutlookCalendarSyncServiceTests"`

Expected: PASS。

- [ ] **Step 5: 提交**

```powershell
git add src/modules/Pim.Module.Calendar/Services/OutlookCalendarSyncService.cs tests/Pim.UnitTests/Calendar/OutlookCalendarSyncServiceTests.cs
git commit -m "feat: add outlook deep sync controls"
```

### Task 6: 请求内直接写回和二次确认契约

**Files:**

- Create: `src/modules/Pim.Module.Calendar/Services/OutlookEventWriteService.cs`
- Modify: `src/modules/Pim.Module.Calendar/Services/CalendarService.cs`
- Modify: `src/modules/Pim.Module.Calendar/Services/CalendarDeleteService.cs`
- Create: `tests/Pim.UnitTests/Calendar/OutlookEventWriteServiceTests.cs`
- Modify: `tests/Pim.UnitTests/Calendar/CalendarDeleteServiceTests.cs`

- [ ] **Step 1: 写写回失败测试**

```csharp
[Fact]
public async Task Create_SendsStableTransactionIdThenPersistsGraphResult()
{
    var request = CreateRequest(clientOperationId);
    var result = await service.ExecuteAsync(userId, request, ct);

    Assert.Equal(clientOperationId.ToString("D"), graph.LastJsonBody["transactionId"]);
    Assert.Equal("created", result.Status);
    Assert.Equal("graph-event-1", (await LoadCreatedEventAsync()).OutlookEventId);
}

[Fact]
public async Task Update412_DoesNotChangeLocalAndReturnsLatestRemote()
{
    graph.ReturnPreconditionFailedThenLatest();
    var before = await SnapshotLocalAsync(eventId);
    var result = await service.ExecuteAsync(userId, UpdateRequest(eventId), ct);

    Assert.Equal("conflict", result.Status);
    Assert.NotNull(result.LatestOutlookJson);
    Assert.Equal(before, await SnapshotLocalAsync(eventId));
}

[Fact]
public async Task Delete404_IsSuccessfulSoftDeleteWithSingleAudit()
{
    graph.ReturnDeleteNotFound();
    var result = await service.ExecuteAsync(userId, DeleteRequest(eventId), ct);
    Assert.Equal("deleted", result.Status);
    Assert.NotNull((await LoadEventAsync(eventId)).DeletedAt);
    var batch = Assert.Single(await WritebackHistoryAsync(eventId));
    Assert.Equal(0, batch.ConfirmationCount);
}
```

还需覆盖：PATCH/DELETE 的 `If-Match` 取 `ExpectedEtag`（初次编辑来自 `EventResponse.OutlookEtag`）、Graph 失败本地不变、只读 binding 禁止、`OutlookSyncState="legacy-unbound"` 禁止写回、用户隔离、普通 CRUD 无法绕过 Outlook 检查（含单条和批量删除）、更新请求跨 binding 时拒绝、实例/系列目标正确、不允许创建或修改 recurrence、写回批次成功/失败均落历史且 `ConfirmationCount=0`。

- [ ] **Step 2: 运行测试确认失败**

Run: `dotnet test tests/Pim.UnitTests/Pim.UnitTests.csproj --filter "FullyQualifiedName~OutlookEventWriteServiceTests"`

Expected: FAIL，提示 `OutlookEventWriteService` 不存在。

- [ ] **Step 3: 实现直接写回**

```csharp
public sealed class OutlookEventWriteService
{
    public async Task<OutlookWriteResult> ExecuteAsync(
        Guid userId, OutlookWriteRequest request, CancellationToken ct)
    {
        var binding = await LoadOwnedWritableBindingAsync(userId, request.CalendarBindingId, ct);
        ValidateNoRecurrenceMutation(request);
        return request.Operation switch
        {
            "create" => await CreateAsync(userId, binding, request, ct),
            "update" => await UpdateAsync(userId, binding, request, ct),
            "delete" => await DeleteAsync(userId, binding, request, ct),
            _ => throw new DomainException(02009, "不支持的 Microsoft 日程操作。")
        };
    }
}
```

新建把 `request.ClientOperationId.ToString("D")` 明确传给 mapper 的 `transactionId` 参数，成功后才创建本地 `EventEntity`。修改/删除用 `request.ExpectedEtag` 发 `If-Match`，并验证它非空且初次请求等于当前本地 ETag。Graph 成功后才更新本地。412 时 GET 最新远端，把最新 ETag 和内容返回为 `conflict`，不调用 `SaveChangesAsync`。DELETE 204 或 404 都执行一次本地软删除和审计。

写回遇到 `OutlookReauthenticationRequiredException` 时同样更新 connection 状态后返回可重连错误，本地事件保持不变。

实例/系列目标只决定 Graph event ID：`scope="series"` 使用 `OutlookSeriesMasterId ?? OutlookEventId`，`scope="instance"` 使用当前 `OutlookEventId`；payload 始终不包含 recurrence pattern/range。

在 `CalendarService.CreateEventAsync`、`CalendarService.UpdateEventAsync` 和实际承接普通 DELETE endpoints 的 `CalendarDeleteService.DeleteEventAsync` / `BatchDeleteEventsAsync` 增加守卫：目标 calendar 或现有 event 有 Outlook binding 时整次操作拒绝并抛出明确 `DomainException`，只能走确认后的 writeback endpoint。

- [ ] **Step 4: 运行写回和普通日程回归测试**

Run: `dotnet test tests/Pim.UnitTests/Pim.UnitTests.csproj --filter "FullyQualifiedName~OutlookEventWriteServiceTests|FullyQualifiedName~CalendarDeleteServiceTests|FullyQualifiedName~CalendarTaskPlanningTests|FullyQualifiedName~CalendarServiceUiCreationTests"`

Expected: PASS。

- [ ] **Step 5: 提交**

```powershell
git add src/modules/Pim.Module.Calendar/Services/OutlookEventWriteService.cs src/modules/Pim.Module.Calendar/Services/CalendarService.cs src/modules/Pim.Module.Calendar/Services/CalendarDeleteService.cs tests/Pim.UnitTests/Calendar/OutlookEventWriteServiceTests.cs tests/Pim.UnitTests/Calendar/CalendarDeleteServiceTests.cs
git commit -m "feat: write outlook events after confirmation"
```

### Task 7: API、Device Code 接线、调度和连接生命周期

**Files:**

- Create: `src/modules/Pim.Module.Calendar/Services/OutlookCalendarSyncJob.cs`
- Modify: `src/modules/Pim.Module.Calendar/CalendarModule.cs`
- Modify: `src/modules/Pim.Module.Calendar/DTOs/OutlookSyncDtos.cs`
- Modify: `tests/Pim.UnitTests/Calendar/CalendarEndpointPathTests.cs`
- Modify: `tests/Pim.UnitTests/Calendar/OutlookAuthorizationSessionTests.cs`
- Modify: `tests/Pim.UnitTests/Pim.UnitTests.csproj`
- Create: `tests/Pim.UnitTests/Calendar/OutlookCalendarSyncJobTests.cs`
- Create: `tests/Pim.UnitTests/Calendar/OutlookCalendarApiContractTests.cs`

- [ ] **Step 1: 写 API 和 job 失败测试**

```csharp
[Fact]
public void CalendarModule_RegistersOnlyTheFiveNewRuntimeComponents()
{
    var services = BuildCalendarServices();
    AssertScoped<GraphCalendarClient>(services);
    AssertScoped<OutlookCalendarSyncService>(services);
    AssertScoped<OutlookEventWriteService>(services);
    AssertScoped<OutlookCalendarSyncJob>(services);
    Assert.DoesNotContain(services, x => x.ServiceType == typeof(OutlookEventMapper));
}

[Fact]
public async Task DeviceCodeEndpoint_UsesExistingAuthorizationSessionRunner()
{
    var response = await client.PostAsync("/api/v1/calendar/outlook/device-code", JsonContent.Create(new { }));
    var body = await ReadDataAsync<OutlookAuthorizationSessionResponse>(response);
    Assert.Equal("waiting-for-user", body.Status);
    Assert.NotNull(body.UserCode);
}

[Fact]
public async Task Job_ScansOnlyConnectedSelectedAccountsSequentially()
{
    await job.RunAllAsync();
    Assert.Equal(new[] { connectedUserId }, sync.StartedUsers);
}
```

API contract 测试还要覆盖当前用户过滤、Client ID UUID 校验、固定 `common`/scopes、发现/选择、三种 sync mode、取消、单日历重试、分页历史、检查连接、writeback 409、断开保留数据、本地清理不调用 Graph、可靠 Graph ID/iCalUId 重绑。

以下分支必须是独立 `[Fact]`，不能只写在一个大用例里：

```csharp
[Theory]
[InlineData(false, null, "not-configured")]
[InlineData(true, "not-connected", "failed")]
[InlineData(true, "waiting-for-user", "waiting-auth")]
[InlineData(true, "connected", "connected")]
[InlineData(true, "reauth-required", "reauth-required")]
[InlineData(true, "failed", "failed")]
public async Task Settings_DerivesUiStatus(bool hasClientId, string? state, string expected)
{
    var result = await ReadSettingsAsync(hasClientId, state);
    Assert.Equal(expected, result.UiStatus);
}

[Fact]
public async Task Rebind_PrefersExactGraphId()
    => Assert.Equal(exactBindingId, (await RunRebindAsync(EventWithGraphId())).OutlookCalendarBindingId);

[Fact]
public async Task Rebind_UsesOnlyUniqueIcalUid()
    => Assert.Equal(uniqueBindingId, (await RunRebindAsync(EventWithUniqueIcalUid())).OutlookCalendarBindingId);

[Fact]
public async Task Rebind_DoesNotUseDuplicateIcalUid()
    => Assert.Equal("legacy-unbound", (await RunRebindAsync(EventWithDuplicateIcalUid())).OutlookSyncState);

[Fact]
public async Task Rebind_MarksUnmatchedEventLegacyUnbound()
    => Assert.Equal("legacy-unbound", (await RunRebindAsync(UnmatchedEvent())).OutlookSyncState);
```

- [ ] **Step 2: 运行测试确认失败**

Run: `dotnet test tests/Pim.UnitTests/Pim.UnitTests.csproj --filter "FullyQualifiedName~OutlookCalendarApiContractTests|FullyQualifiedName~OutlookCalendarSyncJobTests|FullyQualifiedName~CalendarEndpointPathTests"`

Expected: FAIL，缺少新路由和 job。

- [ ] **Step 3: 注册组件并替换旧路由接线**

为真实的 in-memory HTTP contract test 增加一个测试专用依赖：

```powershell
dotnet add tests/Pim.UnitTests/Pim.UnitTests.csproj package Microsoft.AspNetCore.TestHost --version 8.0.0
```

`OutlookCalendarApiContractTests` 使用 `WebApplication` + `UseTestServer()`，把 fake MSAL/token provider 和可编程 Graph handler 注入容器；不得连接真实 Microsoft 或 PostgreSQL。

DI 只增加以下注册；`OutlookEventMapper` 是静态类，不注册：

```csharp
services.TryAddSingleton(TimeProvider.System);
services.AddScoped<GraphCalendarClient>();
services.AddScoped<OutlookCalendarSyncService>();
services.AddScoped<OutlookEventWriteService>();
services.AddScoped<OutlookCalendarSyncJob>();
```

`TryAddSingleton` 只提供生产默认值；测试在调用 `RegisterServices` 前注册 fixed `TimeProvider`，因此不会被覆盖。

复用现有路径并只补缺失能力：

```csharp
group.MapGet("/outlook/settings", GetOutlookSettings);
group.MapPut("/outlook/settings", UpdateOutlookSettings);
group.MapPost("/outlook/device-code", StartAuthorizationSession);
group.MapPost("/outlook/device-code/poll", ReadAuthorizationSession);
group.MapPost("/outlook/device-code/{sessionId:guid}/cancel", CancelAuthorizationSession);
group.MapPost("/outlook/check", CheckOutlookConnection);
group.MapPost("/outlook/calendars/discover", DiscoverOutlookCalendars);
group.MapPut("/outlook/calendars/selection", UpdateOutlookCalendarSelection);
group.MapPost("/outlook/sync", RunOutlookSync);
group.MapPost("/outlook/sync/{batchId:guid}/cancel", CancelOutlookSync);
group.MapGet("/outlook/sync/batches", ListOutlookBatches);
group.MapPost("/outlook/events/writeback", WriteOutlookEvent);
group.MapPost("/outlook/disconnect", DisconnectOutlook);
group.MapGet("/outlook/local-data/preview", PreviewLocalOutlookData);
group.MapDelete("/outlook/local-data", RemoveLocalOutlookData);
```

Device Code start 创建 `OutlookAuthorizationSessionEntity` 后调用现有 `OutlookAuthorizationSessionRunner.StartAsync`；poll 按 session ID 读取；cancel 调用 runner。connection 初始值保持 `not-connected`，成功 session/connection 都是 `connected`。

settings response 的 `UiStatus` 是展示值，不得回写 connection `Status`；普通 UI 不渲染原始 `TokenHealth`。

`CheckOutlookConnection` 顺序为 silent token -> `/me?$select=id,displayName,userPrincipalName` -> calendars。断开清空加密 MSAL cache，并把 connection 设为 `not-connected` / `missing` 以停止同步，但保留 calendars/events/bindings/history。`local-data/preview` 返回 binding/calendar/event 数量；确认后的 DELETE 软删除 Microsoft events/calendars、移除 binding/cache、永久保留 batch history，且不得发 Graph 写请求。旧事件只按 Graph ID 或唯一 iCalUId 重绑，否则标 `legacy-unbound`。

旧事件重绑包含 `Source="outlook-ics"` 数据，但仍只接受可靠 Graph ID 或唯一 iCalUId；不能可靠匹配时保持原事件可见并标 `legacy-unbound`，不读取 ICS 标题/时间做模糊匹配。

检查连接若第二次 Graph 401，则复用同一 `reauth-required`/`interaction-required` 状态规则。

- [ ] **Step 4: 实现薄 job 与启动恢复**

```csharp
public sealed class OutlookCalendarSyncJob
{
    public async Task RunAllAsync()
    {
        foreach (var userId in await _sync.ListRunnableUsersAsync(CancellationToken.None))
            await _sync.SyncAsync(userId, new OutlookSyncRequest("normal"), CancellationToken.None);
    }
}
```

`CalendarModule.InitializeAsync` 继续调用授权 session 清理，并增加：旧 `running` batch 标 `interrupted`、启动时 enqueue 一次、注册 `*/5 * * * *` recurring job。通过 `GetService<IBackgroundJobClient>()` / `GetService<IRecurringJobManager>()` 获取 Hangfire；测试或降级启动未注册 Hangfire 时记录 warning 并跳过，不能让模块初始化失败。job 不包含同步业务。

Hangfire 为每次 job invocation 创建 DI scope，因此 scoped `OutlookCalendarSyncService` 与 `PimDbContext` 在一次 `RunAllAsync` 内有效；job 和 service 都不得缓存或跨 invocation 复用 DbContext。

此任务先保留旧服务源码和 DI 注册；旧 Outlook endpoints 已全部改接新链路后，Task 10 再移除旧 DI 注册。

- [ ] **Step 5: 运行后端 Outlook 测试并提交**

Run: `dotnet test tests/Pim.UnitTests/Pim.UnitTests.csproj --filter "FullyQualifiedName~Outlook|FullyQualifiedName~CalendarEndpointPathTests"`

Expected: PASS。

```powershell
git add src/modules/Pim.Module.Calendar/CalendarModule.cs src/modules/Pim.Module.Calendar/DTOs/OutlookSyncDtos.cs src/modules/Pim.Module.Calendar/Services/OutlookCalendarSyncJob.cs tests/Pim.UnitTests/Pim.UnitTests.csproj tests/Pim.UnitTests/Calendar/CalendarEndpointPathTests.cs tests/Pim.UnitTests/Calendar/OutlookAuthorizationSessionTests.cs tests/Pim.UnitTests/Calendar/OutlookCalendarSyncJobTests.cs tests/Pim.UnitTests/Calendar/OutlookCalendarApiContractTests.cs
git commit -m "feat: wire lightweight outlook calendar api"
```

### Task 8: 重做 Microsoft 设置与同步界面

**Files:**

- Modify: `src/client-web/src/types/index.ts`
- Modify: `src/client-web/src/api/calendar.ts`
- Modify: `src/client-web/src/pages/SyncPage.tsx`
- Modify: `src/client-web/src/pages/CalendarPage.tsx`
- Modify: `src/client-web/package.json`
- Modify: `src/client-web/package-lock.json`
- Create: `tests/client-web/microsoftCalendarSyncApi.test.ts`
- Create: `tests/client-web/microsoftCalendarSyncUi.test.ts`
- Modify: `tests/client-web/scheduleWorkbenchVisualAudit.test.ts`

- [ ] **Step 1: 写 Web API 与页面失败测试**

```ts
assert.equal(calendarApiPaths.outlookDiscover(), '/calendar/outlook/calendars/discover');
assert.equal(calendarApiPaths.outlookSelection(), '/calendar/outlook/calendars/selection');
assert.equal(calendarApiPaths.outlookWriteback(), '/calendar/outlook/events/writeback');
assert.equal(calendarApiPaths.outlookLocalDataPreview(), '/calendar/outlook/local-data/preview');

const syncPage = readFileSync('src/client-web/src/pages/SyncPage.tsx', 'utf8');
for (const text of ['应用注册', '公共客户端流', 'Calendars.ReadWrite', '复制代码', '打开 Microsoft', '立即同步', '深度同步', '移除本地 Microsoft 数据'])
  assert.ok(syncPage.includes(text), `SyncPage should contain ${text}`);
for (const removed of ['Secret', 'deltaLink', 'writebackDefault', 'conflictPolicy', 'tokenHealth'])
  assert.ok(!syncPage.includes(removed), `SyncPage should not expose ${removed}`);

const calendarPage = readFileSync('src/client-web/src/pages/CalendarPage.tsx', 'utf8');
assert.ok(calendarPage.includes('timeZone="Asia/Shanghai"'));
```

Playwright mock 还要验证 390px、768px、1440px 下无重叠；设备码倒计时不会改变布局；日历分组可全选/单选；只读、暂停、remote-missing 状态可见；同步进度、取消、失败日历重试和两种深度同步可操作。

- [ ] **Step 2: 运行测试确认失败**

Run: `npm --prefix src/client-web exec tsx -- tests/client-web/microsoftCalendarSyncApi.test.ts`

Run: `npm --prefix src/client-web exec tsx -- tests/client-web/microsoftCalendarSyncUi.test.ts`

Expected: FAIL，缺少新 API client 和界面文案/控件。

- [ ] **Step 3: 对齐类型和 API client**

先安装 FullCalendar 官方时区插件：

```powershell
npm --prefix src/client-web install @fullcalendar/luxon3 luxon
npm --prefix src/client-web install --save-dev @types/luxon
```

`UpdateOutlookSettingsRequest` 只保留 `clientId`；tenant/scopes 不再由前端提交。新增方法沿用现有 `apiGet/apiPost/apiPut/apiDelete`：

```ts
export const discoverOutlookCalendars = () =>
  apiPost<ApiResponse<OutlookCalendarBinding[]>>(calendarApiPaths.outlookDiscover(), {});

export const updateOutlookCalendarSelection = (selectedBindingIds: string[]) =>
  apiPut<ApiResponse<OutlookCalendarBinding[]>>(calendarApiPaths.outlookSelection(), { selectedBindingIds });

export const runOutlookSync = (request: OutlookSyncRequest) =>
  apiPost<ApiResponse<OutlookSyncBatch>>(calendarApiPaths.outlookSync(), request);

export const previewLocalOutlookData = () =>
  apiGet<ApiResponse<OutlookLocalDataPreview>>(calendarApiPaths.outlookLocalDataPreview());
```

Device Code start 返回 `OutlookAuthorizationSessionResponse`；poll body 改为 `{ sessionId }`，不再把 `deviceCode` 传回前端或 API。

删除前端旧 `deltaLink`、`syncWindowDays`、`writebackDefault`、`conflictPolicy` 类型字段。`tokenHealth` 即使后端为兼容仍返回，也不得在普通页面展示。

- [ ] **Step 4: 实现实际设置页工作流**

`SyncPage` 第一屏就是可操作设置，不新增 landing page。流程为：Entra 分步引导 -> Client ID -> Device Code -> 自动轮询 -> 日历分组选择 -> 同步状态/历史。按钮按既有样式使用清楚命令；复制、打开、刷新、取消等图标按钮带 tooltip。

普通同步、full-resources、range-instances 都调用同一个 sync endpoint；失败日历重试带 `retryOfBatchId`。危险清理先显示将影响的本地数量并要求确认，文案明确“不修改 Outlook 云端”。

设置页 selection 成功后刷新现有 calendar queries；现有 `CalendarLayerToolbar` 继续只处理图层显隐，不为同步设置复制第二套状态或新增改动。

`CalendarPage` 使用 `@fullcalendar/luxon3` + `luxon` 并显式配置 `timeZone="Asia/Shanghai"`；不得依赖运行浏览器的本地时区。定时事件使用后端 UTC 值渲染，全天事件继续使用日期边界。

- [ ] **Step 5: 运行 Web 测试、构建并提交**

Run: `npm --prefix src/client-web run test:schedule-workbench`

Run: `npm --prefix src/client-web run build`

Expected: PASS。

```powershell
git add src/client-web/src/types/index.ts src/client-web/src/api/calendar.ts src/client-web/src/pages/SyncPage.tsx src/client-web/src/pages/CalendarPage.tsx src/client-web/package.json src/client-web/package-lock.json tests/client-web/microsoftCalendarSyncApi.test.ts tests/client-web/microsoftCalendarSyncUi.test.ts tests/client-web/scheduleWorkbenchVisualAudit.test.ts
git commit -m "feat: guide microsoft calendar setup and sync"
```

### Task 9: 事件编辑器写回预览与 412 恢复

**Files:**

- Modify: `src/client-web/src/dialogs/EventEditorDialog.tsx`
- Modify: `src/client-web/src/api/calendar.ts`
- Modify: `src/client-web/src/types/index.ts`
- Create: `tests/client-web/outlookEventWritebackUi.test.ts`
- Modify: `tests/client-web/scheduleWorkbenchVisualAudit.test.ts`

- [ ] **Step 1: 写编辑器失败测试**

```ts
const editor = readFileSync('src/client-web/src/dialogs/EventEditorDialog.tsx', 'utf8');
for (const contract of ['outlookCalendarBindingId', 'canEdit', 'before', 'after', 'clientOperationId', '最新 Outlook 内容'])
  assert.ok(editor.includes(contract), `EventEditorDialog should contain ${contract}`);

assert.ok(editor.includes("status === 'conflict'"));
assert.ok(editor.includes('实例'));
assert.ok(editor.includes('系列'));
assert.ok(!editor.includes('强制覆盖'));
assert.ok(!editor.includes('复制为 PIM 日程'));
```

Playwright 交互测试覆盖：手动事件仍走原 CRUD；Outlook 新建/修改/删除先预览再确认；只读 binding 不显示保存/删除命令；412 展示最新远端并保留用户草稿；实例/系列范围可选；recurrence 规则不可新建或编辑。

- [ ] **Step 2: 运行测试确认失败**

Run: `npm --prefix src/client-web exec tsx -- tests/client-web/outlookEventWritebackUi.test.ts`

Expected: FAIL，当前编辑器直接调用普通 CRUD。

- [ ] **Step 3: 接入确认后的 writeback 请求**

```tsx
const isOutlook = Boolean(selectedCalendar?.outlookCalendarBindingId);

const submit = (draft: EventDraft) => {
  if (!isOutlook) return saveManualEvent(draft);
  setWritebackPreview({ before: event ?? null, after: draft, operation: event ? 'update' : 'create' });
};

const confirmWriteback = () => writeOutlookEvent({
  operation: writebackPreview.operation,
  calendarBindingId: selectedCalendar!.outlookCalendarBindingId!,
  eventId: event?.id,
  draft: writebackPreview.after,
  scope: recurrenceScope,
  clientOperationId: crypto.randomUUID(),
  expectedEtag: conflict?.latestEtag ?? event?.outlookEtag,
});
```

删除同样先显示 before/after（after 为删除）。API 返回 `conflict` 时保持 dialog 和草稿不关闭，展示 `latestOutlookJson`。用户点击“基于 Outlook 最新版本重新比较”后，把 latest snapshot 设为新的 `before`、保留用户草稿作为 `after`，并把 `latestEtag` 用作下一次确认请求的 `expectedEtag`；不提供强制覆盖或自动字段合并。

若再次发生 412，就重复显示更新后的远端版本；每次重试都必须由用户重新确认，用户可随时关闭 dialog 放弃，不存在自动或无限后台重试。

- [ ] **Step 4: 运行编辑器测试、视觉检查和构建**

Run: `npm --prefix src/client-web exec tsx -- tests/client-web/outlookEventWritebackUi.test.ts`

Run: `npm --prefix src/client-web exec tsx -- tests/client-web/scheduleWorkbenchVisualAudit.test.ts`

Run: `npm --prefix src/client-web run build`

Expected: PASS，三个 viewport 无文字或控件重叠。

- [ ] **Step 5: 提交**

```powershell
git add src/client-web/src/dialogs/EventEditorDialog.tsx src/client-web/src/api/calendar.ts src/client-web/src/types/index.ts tests/client-web/outlookEventWritebackUi.test.ts tests/client-web/scheduleWorkbenchVisualAudit.test.ts
git commit -m "feat: confirm outlook event writeback in editor"
```

### Task 10: 全量回归、真实账号门禁与旧运行路径退役

**Files:**

- Modify: `src/modules/Pim.Module.Calendar/CalendarModule.cs`
- Create: `docs/operations/microsoft-calendar-sync-acceptance.md`

- [ ] **Step 1: 运行自动化回归**

Run: `dotnet test Pim.sln`

Expected: PASS。

Run: `npm --prefix src/client-web run lint`

Expected: PASS。

Run: `npm --prefix src/client-web run test:schedule-workbench`

Expected: PASS。

Run: `npm --prefix src/client-web run build`

Expected: PASS。

- [ ] **Step 2: 启动本地应用并执行真实 Microsoft 账号验收**

在 `docs/operations/microsoft-calendar-sync-acceptance.md` 记录日期、测试账号类型和每项 PASS/FAIL，不记录 token、device code、user code 或事件正文。必须实际验证：

```markdown
- [ ] 仅按页面引导完成 Entra 注册和 Device Code 授权
- [ ] 发现默认、分组、课程表和未分组日历
- [ ] 普通同步、full-resources、range-instances 与手动强制获取全部日程
- [ ] UTC+8 展示、全天边界、重复实例/系列
- [ ] Outlook -> PIM 新增、修改、移动、删除自动应用
- [ ] PIM -> Outlook 新建、修改、删除均经过二次确认
- [ ] ETag 412 停止覆盖并展示最新远端
- [ ] token 静默续期、取消、部分失败和失败日历人工重试
- [ ] 永久历史可查，断开保留数据，本地清理不影响 Outlook
```

任一项 FAIL 时停止本任务，修复并重新运行相关自动化测试和失败的真实账号步骤。

- [ ] **Step 3: 退役旧运行注册，保留源码**

真实账号验收通过后，从 `CalendarModule.RegisterServices` 删除：

```csharp
services.AddScoped<OutlookSyncService>();
services.AddScoped<OutlookTokenService>();
services.AddScoped<IMicrosoftGraphClient, MicrosoftGraphDeviceCodeClient>();
services.AddScoped<OutlookConflictService>();
```

不得删除对应源码、旧列、旧表或历史文档。更新旧测试，使其不再把这些类型视为生产注册要求；旧 delta/writeback/conflict 测试保留为历史场景，但不作为新链路完成证据。

- [ ] **Step 4: 最终扫描和复验**

Run: `rg -n "OutlookSyncService|OutlookTokenService|IMicrosoftGraphClient|OutlookConflictService" src/modules/Pim.Module.Calendar/CalendarModule.cs`

Expected: 无匹配。

Run: `rg -n "DeltaLink|PendingConfirmation|OutlookOperationExecutionEntity|SyncConflictEntity" src/modules/Pim.Module.Calendar/Services/GraphCalendarClient.cs src/modules/Pim.Module.Calendar/Services/OutlookCalendarSyncService.cs src/modules/Pim.Module.Calendar/Services/OutlookEventWriteService.cs`

Expected: 无匹配。

Run: `dotnet test Pim.sln`

Run: `npm --prefix src/client-web run lint`

Run: `npm --prefix src/client-web run test:schedule-workbench`

Run: `npm --prefix src/client-web run build`

Expected: 全部 PASS。

- [ ] **Step 5: 提交并按仓库流程创建 PR**

```powershell
git add src/modules/Pim.Module.Calendar/CalendarModule.cs docs/operations/microsoft-calendar-sync-acceptance.md
git commit -m "chore: retire legacy outlook runtime path"
git status --short --branch
git push -u origin codex/microsoft-calendar-sync
gh pr create --fill
```

等待该 PR 触发的 GitHub Actions 完成；若路径过滤导致没有 workflow，明确记录“未触发”而不是宣称 CI 通过。

## 设计覆盖索引

| 设计能力 | 实施任务 |
|----------|----------|
| MSAL Device Code、cache、静默续期、状态映射 | 2、7、8 |
| calendarGroups + 组内 + 根日历发现、选择、只读、remote-missing | 3、7、8 |
| 固定 -90/+365 普通同步、逐页 upsert、missing verification | 2、4 |
| full-resources、range-instances、取消、人工重试、强制全量 | 5、7、8 |
| UTC、全天、重复事件、跨 binding 移动 | 1、4 |
| 二次确认、transactionId、If-Match、412、DELETE 404 | 2、6、9 |
| 启动同步、5 分钟 job、连接锁、interrupted | 4、7 |
| 永久批次历史、变化/失败事件摘要 | 4、5、6、8 |
| 检查连接、断开、本地清理、旧事件可靠重绑 | 7、8、10 |
| 安全、用户隔离、真实账号 E2E、旧运行路径退役 | 2、7、10 |

## 完成定义

只有以下条件同时满足才算完成：全部自动化命令通过；真实 Microsoft 账号验收有记录且全部 PASS；旧源码仍在但生产注册已退役；新服务不引用 delta/outbox/conflict/确认中心；PR 检查已确认结果。
