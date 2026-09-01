# PIM MCP Phase 3 写入能力 + HTTP 多客户端 + 权限管理 — 实现计划

> **面向 AI 代理的工作者：** 必需子技能：使用 superpowers:subagent-driven-development（推荐）或 superpowers:executing-plans 逐任务实现此计划。步骤使用复选框（`- [ ]`）语法来跟踪进度。

**目标：** 实现 MCP Phase 3：新增 50 个写入工具（HTTP MCP 暴露）、Pim.Api 提供 mcp_clients 表 + 管理/verify API、MCP server 支持 Streamable HTTP 模式 + Token 鉴权 + 工具级权限校验，WebUI 新增 MCP 管理页，更新文档。

**架构：**
- Pim.Api 新增 `Pim.Module.Mcp` 模块：`mcp_clients` 表（Token SHA-256 哈希 + 权限 JSONB + 连接状态），管理 API（CRUD/revoke/catalog）+ 内部 `/api/v1/mcp/verify`（校验 Token、做工具级权限鉴权、发一次性操作 JWT、记活跃与审计）。
- MCP server（`scripts/mcp/pim_mcp_server.py`）新增 HTTP 模式（FastMCP `streamable-http`）：从请求头取 `pim_mcp_` Token → 调 `/verify` → 拿 JWT + 权限集 → 包装全部工具做权限校验，stdio 模式原样保留。
- WebUI 新增 `/settings/mcp` 设置页：客户端列表（状态/活跃/调用次数）、新建客户端 + 一次性 Token、读 101 + 写 50 工具级权限开关（模块级折叠 + 半选态）。

**技术栈：** .NET 8 / EF Core 8 / Npgsql JSONB；Python FastMCP 1.12.4（streamable-http）；React 19 + TS + Tailwind + react-query。

---

## 文件结构总览

**新增（后端，Pim.Module.Mcp）：**
- `src/modules/Pim.Module.Mcp/Pim.Module.Mcp.csproj`
- `src/modules/Pim.Module.Mcp/McpModule.cs`（IModule + McpEndpointPaths）
- `src/modules/Pim.Module.Mcp/Entities/McpClientEntity.cs`
- `src/modules/Pim.Module.Mcp/Entities/McpClientEntityConfiguration.cs`
- `src/modules/Pim.Module.Mcp/DTOs/McpDtos.cs`
- `src/modules/Pim.Module.Mcp/Services/McpToolCatalog.cs`（101 读 + 50 写工具清单）
- `src/modules/Pim.Module.Mcp/Services/McpTokenService.cs`（生成/哈希/前缀）
- `src/modules/Pim.Module.Mcp/Services/McpClientService.cs`（CRUD/revoke/verify/audit）
- `src/Pim.Infrastructure/Data/Migrations/<ts>_AddMcpClients.cs`（dotnet ef 生成）
- 测试：`tests/Pim.UnitTests/Mcp/*`

**修改（后端）：** `Pim.sln`（加入新项目）

**新增（MCP server）：** 全部在 `scripts/mcp/pim_mcp_server.py` 内修改（HTTP 模式 + 包装 + 50 写入工具）+ `scripts/mcp/test_pim_mcp_server.py`（pytest 冒烟）

**新增（WebUI）：**
- `src/client-web/src/api/mcp.ts`
- `src/client-web/src/pages/McpSettingsPage.tsx`（+ `components/mcp/PermissionEditor.tsx`）
- `src/client-web/src/types/index.ts`（追加 MCP 类型）
- `src/client-web/src/layout/AppLayout.tsx`（路由）、`src/client-web/src/pages/SettingsPage.tsx`（卡片）
- `tests/client-web/mcpApiPath.test.ts`、`tsconfig.mcp.json`

**修改（文档）：** `docs/mcp.md`、`README.md`

---

## 任务 M1：后端 Pim.Module.Mcp（TDD）

### 任务 M1-T1：项目骨架 + 实体 + 迁移

**文件：**
- 创建：`src/modules/Pim.Module.Mcp/Pim.Module.Mcp.csproj`
- 创建：`src/modules/Pim.Module.Mcp/McpModule.cs`
- 创建：`src/modules/Pim.Module.Mcp/Entities/McpClientEntity.cs`
- 创建：`src/modules/Pim.Module.Mcp/Entities/McpClientEntityConfiguration.cs`
- 修改：`Pim.sln`

- [ ] **步骤 1：写失败测试** `tests/Pim.UnitTests/Mcp/McpModuleEndpointTests.cs`
```csharp
public sealed class McpModuleEndpointTests
{
    [Fact]
    public void McpEndpoints_AreMappedUnderApiV1Mcp()
    {
        var builder = WebApplication.CreateBuilder();
        builder.Services.AddRouting();
        builder.Services.AddAuthorization();
        var app = builder.Build();
        new McpModule().MapEndpoints(app);
        var endpoints = app.DataSources.SelectMany(s => s.Endpoints).OfType<RouteEndpoint>().ToList();
        var paths = endpoints.Select(e => e.RoutePattern.RawText).ToHashSet();
        Assert.Contains("/api/v1/mcp/clients", paths);
        Assert.Contains("/api/v1/mcp/clients/{id}", paths);
        Assert.Contains("/api/v1/mcp/clients/{id}/revoke", paths);
        Assert.Contains("/api/v1/mcp/verify", paths);
        Assert.Contains("/api/v1/mcp/catalog", paths);
    }
}
```
- [ ] **步骤 2：跑失败**：`dotnet test Pim.sln --no-restore --filter McpModuleEndpointTests` → 编译失败（项目不存在）
- [ ] **步骤 3：实现骨架**。csproj 参考 QuickNotes 项目（`Pim.Core`/`Pim.Infrastructure` ProjectReference + `FrameworkReference Microsoft.AspNetCore.App`）。`McpModule.cs`：
```csharp
public class McpModule : IModule
{
    public string Name => "mcp";
    public string Version => "1.0.0";
    public void RegisterServices(IServiceCollection services, IConfiguration configuration)
    {
        PimDbContext.RegisterModuleAssembly(Assembly.GetExecutingAssembly());
        services.AddScoped<McpTokenService>();
        services.AddScoped<McpClientService>();
    }
    public void MapEndpoints(IEndpointRouteBuilder endpoints)
    {
        var mgmt = endpoints.MapGroup(McpEndpointPaths.Root).RequireAuthorization();
        mgmt.MapGet("/clients", ...);           // list
        mgmt.MapPost("/clients", ...);          // create
        mgmt.MapPut("/clients/{id:guid}", ...); // update name/permissions
        mgmt.MapPost("/clients/{id:guid}/revoke", ...);
        mgmt.MapDelete("/clients/{id:guid}", ...);
        mgmt.MapGet("/catalog", ...);           // tool catalog
        var verify = endpoints.MapGroup("/api/v1/mcp");
        verify.MapPost("/verify", ...);         // NO RequireAuthorization
    }
    public Task InitializeAsync(IServiceProvider sp) => Task.CompletedTask;
}
```
- [ ] **步骤 4：实体 + 配置**
```csharp
public class McpClientEntity
{
    public Guid Id { get; set; }
    public string Name { get; set; } = "";
    public string TokenHash { get; set; } = "";      // SHA-256 hex
    public string TokenPrefix { get; set; } = "";    // pim_mcp_ab12… 展示用
    public Dictionary<string, Dictionary<string, bool>> Permissions { get; set; } = new(); // {read:{}, write:{}}
    public string Status { get; set; } = "active";   // active/revoked
    public DateTimeOffset CreatedAt { get; set; }
    public DateTimeOffset? RevokedAt { get; set; }
    public DateTimeOffset? LastSeenAt { get; set; }
    public long CallCount { get; set; }
    public long WriteCallCount { get; set; }
    public string? LastTool { get; set; }
    public Guid CreatedBy { get; set; }
}
```
配置类实现 `IEntityTypeConfiguration<McpClientEntity>`：表 `mcp_clients`，`Name` 唯一索引，`Permissions` 映射 jsonb（Npgsql 自动），`CreatedBy` 外键到 `users`（可选，若 UserEntity 为 `Pim.Infrastructure.Data.Entities` 可直接引用，否则仅存 Guid 不建 FK）。
- [ ] **步骤 5：sln 添加项目**：`dotnet sln Pim.sln add src/modules/Pim.Module.Mcp/Pim.Module.Mcp.csproj`
- [ ] **步骤 6：跑测试**：`dotnet test Pim.sln --no-restore --filter McpModuleEndpointTests` → 端点列表断言 PASS（此时各端点 handler 可为占位 `Results.Ok(ApiResponse<string>.Ok(""))`，但 `McpClientService`/`McpTokenService` 必须已注册或端点先最小实现）
- [ ] **步骤 7：Commit** `feat(mcp): Pim.Module.Mcp 骨架与端点路由 / scaffold Mcp module skeleton`

> 说明：迁移（migration）在本里程碑最后统一生成，因为实体/配置定型后一次性 `dotnet ef migrations add AddMcpClients` 更干净；但实体类在此任务先落地，保证单元测试可编译。

### 任务 M1-T2：McpToolCatalog（工具清单）

**文件：** 创建 `src/modules/Pim.Module.Mcp/Services/McpToolCatalog.cs`；测试 `tests/Pim.UnitTests/Mcp/McpToolCatalogTests.cs`

- [ ] **步骤 1：写失败测试**
```csharp
[Fact] public void Catalog_Has101ReadAnd50Write() {
    Assert.Equal(101, McpToolCatalog.ReadTools.Count);
    Assert.Equal(50, McpToolCatalog.WriteTools.Count);
}
[Fact] public void WriteTools_CoverExpectedModules() {
    var names = McpToolCatalog.WriteTools.Select(t => t.Name).ToHashSet();
    Assert.Contains("create_event", names);
    Assert.Contains("create_task", names);
    Assert.Contains("create_reminder", names);
    Assert.Contains("create_quick_note", names);
    Assert.Contains("upload_file", names);
    Assert.Contains("create_category", names);
    Assert.Contains("create_mobile_goal", names);
}
```
- [ ] **步骤 2：跑失败** → 类不存在
- [ ] **步骤 3：实现 catalog**。`McpTool` record `(string Name, string Group, string Description, bool IsWrite)`。ReadTools 列出现有 MCP server 全部 101 个函数名（对照 `scripts/mcp/pim_mcp_server.py` 的 `@mcp.tool()` 清单，Group 按 Calendar/PcTracker/Mobile/QuickNotes/Files/CoreInfra）。WriteTools 列 50 个（按设计 §4，Group 用 `calendar.events`/`calendar.tasks`/`calendar.reminders`/`calendar.habits`/`calendar.calendars`/`quicknotes`/`files`/`pctracker.categories`/`mobile.goals`）。
- [ ] **步骤 4：跑测试** → PASS。再补一个断言：ReadTools 无重名、WriteTools 无重名、读写集合不重叠。
- [ ] **步骤 5：Commit** `feat(mcp): 工具目录 101 读 + 50 写 / add Mcp tool catalog`

### 任务 M1-T3：McpTokenService

**文件：** 创建 `src/modules/Pim.Module.Mcp/Services/McpTokenService.cs`；测试 `tests/Pim.UnitTests/Mcp/McpTokenServiceTests.cs`

- [ ] **步骤 1：写失败测试**
```csharp
[Fact] public void Generate_ProducesPrefixedToken() {
    var token = McpTokenService.GenerateToken();
    Assert.StartsWith("pim_mcp_", token);
    Assert.Equal(8 + 32, token.Length); // prefix + 32 chars
}
[Fact] public void Hash_IsStableSha256Hex() {
    var a = McpTokenService.HashToken("pim_mcp_abc");
    var b = McpTokenService.HashToken("pim_mcp_abc");
    Assert.Equal(a, b);
    Assert.Matches(@"^[0-9a-f]{64}$", a);
}
[Fact] public void Prefix_IsFirst12Chars() {
    Assert.Equal("pim_mcp_ab12", McpTokenService.TokenPrefix("pim_mcp_ab12cd34ef56"));
}
```
- [ ] **步骤 2：跑失败**
- [ ] **步骤 3：实现**
```csharp
public static class McpTokenService {
    public static string GenerateToken() {
        var bytes = new byte[24];
        RandomNumberGenerator.Fill(bytes);
        return "pim_mcp_" + Convert.ToHexStringLower(bytes); // 48 hex = 48 chars
    }
    public static string HashToken(string token) =>
        Convert.ToHexStringLower(SHA256.HashData(Encoding.UTF8.GetBytes(token)));
    public static string TokenPrefix(string token) =>
        token.Length <= 12 ? token : token[..12];
}
```
> 注意：设计文档说 32 位随机串，这里用 24 字节 → 48 hex 字符（更强且 URL 安全）。Token 总长 `pim_mcp_` + 48。测试断言按此调整（`Assert.StartsWith` + 长度）。

- [ ] **步骤 4：跑测试** → PASS
- [ ] **步骤 5：Commit** `feat(mcp): Token 生成/哈希/前缀 / add Mcp token service`

### 任务 M1-T4：McpClientService（CRUD + verify + audit）

**文件：** 创建 `src/modules/Pim.Module.Mcp/Services/McpClientService.cs`；测试 `tests/Pim.UnitTests/Mcp/McpClientServiceTests.cs`

- [ ] **步骤 1：写失败测试**（用内存 SQLite/EF InMemory + 手工注入，参照现有 Service 测试惯例，如 `tests/Pim.UnitTests/Calendar/` 下 Fixtures）
```csharp
// CreateAsync returns one-time token, stores hash, default perms = read all on / write all off
// VerifyAsync: wrong hash -> McpVerifyResult.Failure(401); revoked -> 401; valid -> success with permissions + accessToken for CreatedBy user
// RevokeAsync sets status=revoked
// UpdateAsync changes name/permissions
```
> 若 InMemory 对 jsonb 不支持，改用真 Npgsql 的测试库不可行（无 DB）——用 EF Core InMemory provider（`Microsoft.EntityFrameworkCore.InMemory`，若已在测试工程引用）。JSONB 字段在 InMemory 下作为字符串/字典存取，实体映射不变，测试只验证逻辑。
- [ ] **步骤 2：跑失败**
- [ ] **步骤 3：实现服务**
```csharp
public sealed class McpClientService {
    // CreateAsync(name, createdBy): 默认权限 = McpToolCatalog.DefaultPermissions()；生成 token；存 hash+prefix；返回 (client, plainToken)
    // ListAsync(): 全部客户端（含状态、callCount、lastSeen）
    // UpdateAsync(id, name?, permissions?): 校验 name 唯一；权限只接受 catalog 内的 key
    // RevokeAsync(id): status=revoked, revokedAt=now
    // DeleteAsync(id)
    // VerifyAsync(rawToken): hash 查库 -> 不存在/revoked => 401; 否则调 JwtService.GenerateAccessToken(createdByUser 的 userId/username/role) -> 返回 VerifyResult
    // RecordCallAsync(client, tool, isWrite): 更新 lastSeenAt/callCount/writeCallCount/lastTool；write 时写 audit_logs（IAuditLogService，action="mcp.write.{tool}"）
}
```
权限校验放在 VerifyAsync：入参带 `tool` 名时查 permissions dict：读工具查 `read[name]`，写工具查 `write[name]`；不存在 key 时读默认放行、写默认拒绝；拒绝返回 403 + `permission denied: {tool}`。
> `CreatedBy` 用户信息（username/role）需查 `db.Users`。`JwtService` 通过 DI 注入。
- [ ] **步骤 4：跑测试** → PASS
- [ ] **步骤 5：Commit** `feat(mcp): 客户端 CRUD/verify 与审计 / add Mcp client service`

### 任务 M1-T5：McpModule 端点补全 + 迁移

**文件：** 修改 `src/modules/Pim.Module.Mcp/McpModule.cs`；创建 `src/modules/Pim.Module.Mcp/DTOs/McpDtos.cs`

- [ ] **步骤 1：DTO**（`McpDtos.cs`）
```csharp
public record McpClientDto(Guid Id, string Name, string Status, string TokenPrefix,
    Dictionary<string, Dictionary<string, bool>> Permissions,
    DateTimeOffset CreatedAt, DateTimeOffset? LastSeenAt, long CallCount, long WriteCallCount, string? LastTool, bool Online);
public record McpClientCreateResult(McpClientDto Client, string Token); // Token 仅此一次
public record McpClientUpdateRequest(string? Name, Dictionary<string, Dictionary<string, bool>>? Permissions);
public record McpVerifyRequest(string? Tool, string? ParamsSummary);
public record McpVerifyResult(string ClientId, string ClientName, Guid UserId,
    Dictionary<string, Dictionary<string, bool>> Permissions, string AccessToken, bool IsWrite);
```
- [ ] **步骤 2：写失败测试**（扩展 M1-T1 的端点测试：verify 端点无 `IAuthorizeData`，其余有；catalog 端点返回 101/50）
- [ ] **步骤 3：实现端点 handler**：
  - GET `/clients` → `McpClientDto[]`（`Online = LastSeenAt > now-5min`）
  - POST `/clients` → 201 `McpClientCreateResult`（Token 一次性返回，不落日志）
  - PUT `/clients/{id}` → `McpClientDto`
  - POST `/clients/{id}/revoke` → `McpClientDto`
  - DELETE `/clients/{id}` → 204
  - GET `/catalog` → `{ read: McpToolDto[], write: McpToolDto[] }`
  - POST `/verify`（无 JWT）：读 `Authorization: Bearer` → `McpClientService.VerifyAsync(token, tool)`；401/403 用 `Results.Unauthorized()` / `Results.Json(statusCode:403)` 返回 ApiResponse 包装
- [ ] **步骤 4：生成迁移**
```bash
dotnet ef migrations add AddMcpClients --project src/Pim.Infrastructure --startup-project src/Pim.Api --context PimDbContext --output-dir Data/Migrations
```
> 若 `dotnet ef` 未装：`dotnet tool install --global dotnet-ef`（或 `--tool-path /tmp/dotnet-tools`）。无需连接数据库（`migrations add` 离线生成）。
- [ ] **步骤 5：跑全量后端测试**：`dotnet test Pim.sln --no-restore` → 全绿（含既有 1092+）
- [ ] **步骤 6：Commit** `feat(mcp): 管理/verify/catalog 端点与迁移 / add Mcp endpoints and migration`

---

## 任务 M2：MCP server（Python）— HTTP + Token + 50 写入工具（TDD 于 pytest）

> 先安装 pytest：`python3 -m pip install pytest`（或用 `uv`）。

### 任务 M2-T1：HTTP 模式 + Token 身份解析 + 工具包装

**文件：** 修改 `scripts/mcp/pim_mcp_server.py`；创建 `scripts/mcp/test_pim_mcp_server.py`

- [ ] **步骤 1：写失败测试**
```python
# test_pim_mcp_server.py
from pim_mcp_server import _strip_bearer, _build_permission_denied, _catalog_write_names
def test_strip_bearer(): assert _strip_bearer("Bearer abc") == "abc"
def test_write_catalog_has_50(): assert len(_catalog_write_names()) == 50
def test_permission_denied_message(): assert "permission denied: create_task" in _build_permission_denied("create_task")
```
- [ ] **步骤 2：跑失败**（`python3 -m pytest scripts/mcp/test_pim_mcp_server.py`）
- [ ] **步骤 3：实现 HTTP 身份解析**（本任务核心）：
  - 构造 `mcp` 时读环境变量：`PIM_MCP_HOST`（默认 `0.0.0.0`）、`PIM_MCP_PORT`（默认 `8080`）、`PIM_MCP_PATH`（默认 `/mcp`）→ `FastMCP("pim-mcp-server", host=..., port=..., streamable_http_path=...)`
  - `_current_identity: ContextVar`（`accessToken`/`clientId`/`clientName`/`permissions`）
  - `_get_raw_request_token()`：`mcp.get_context().request_context.request.headers["authorization"]` → `_strip_bearer`
  - `_call_verify(raw_token, tool, params_summary)`：直连 `POST {PIM_API_URL}/api/v1/mcp/verify`，`Authorization: Bearer {raw_token}`，body `{tool, params}`；返回 200 的 `data`（含 accessToken/permissions/clientId）或错误 dict
  - `_resolve_http_identity(tool, args)`：HTTP 模式才启用 → 取 raw token（缺失 401）→ `_call_verify` → 非 200 原样返回（401/403 透传）→ 成功则 `_current_identity.set(data)` 返回 None
  - `_wrap_tools_for_http()`：遍历 `mcp._tool_manager.list_tools()`，`tool.fn = async wrapped(**kwargs)`，先 `_resolve_http_identity` 再调原 fn（用闭包捕获 name/orig；`tool._pim_wrapped=True` 防重复包装）
  - `_get_token()`：HTTP 模式下优先返回 `_current_identity.get()["accessToken"]`；否则走原 env/文件逻辑
  - `main()`：`PIM_MCP_TRANSPORT=http|streamable-http` → `_wrap_tools_for_http()` + `mcp.run(transport="streamable-http")`；默认 stdio
- [ ] **步骤 4：跑测试** → PASS；再 `python3 -c "import ast; ast.parse(open('scripts/mcp/pim_mcp_server.py').read())"` 语法自检
- [ ] **步骤 5：Commit** `feat(mcp): HTTP streamable 模式与 Token 鉴权 / add HTTP mode and token auth`

### 任务 M2-T2：Calendar 写入工具（30 个）

**文件：** 修改 `scripts/mcp/pim_mcp_server.py`（新增 `# ===== Calendar writes =====` 段）

- [ ] **步骤 1：写失败测试**（逐模块断言工具名已注册 + 参数 schema 存在）
```python
import json, importlib, sys, os
sys.path.insert(0, os.path.dirname(__file__))
import pim_mcp_server as s
def _tool_names(): return {t["name"] for t in s._list_tools_meta()}
def test_calendar_write_tools_registered():
    for name in ["create_event","update_event","delete_event","restore_event","batch_delete_events",
        "create_task","update_task","delete_task","restore_task","move_task","plan_task",
        "create_task_segment","delete_task_segment","add_task_checklist_item","batch_delete_tasks",
        "batch_update_tasks","create_task_book","create_project","schedule_tasks",
        "create_reminder","snooze_reminder","dismiss_reminder","create_habit","create_habit_occurrence",
        "create_availability_window","import_ics","create_calendar","update_calendar","delete_calendar","restore_calendar"]:
        assert name in _tool_names(), name
```
> `_list_tools_meta()`：MCP server 里加一个测试辅助，返回 `[{"name","group","isWrite"}]`（直接读装饰器注册，无需起服务）。
- [ ] **步骤 2：跑失败**（缺失工具）
- [ ] **步骤 3：实现 30 个工具**。模式：`@mcp.tool()` + async def + 手工校验 + `await _call_api(method, path, json_body=body)`，请求体字段按上文 API 调研表。关键实现约定：
  - 全用 `json_body`（POST/PUT），DELETE 无 body
  - `import_ics`/`upload_*` 需 multipart：新增 `_call_api_multipart(method, path, files, fields)`（httpx `files={"file": ...}` + `data=fields`），读取本地/二进制字节
  - `update_event`/`delete_event` 的 `scope`/`recurrenceId` 走 query params
  - `move_task` 的 `duration` 传 TimeSpan 字符串 `"01:30:00"`
  - 工具描述写清「用途 + 会改什么 + 需要写权限」
- [ ] **步骤 4：跑测试** → 30 工具注册 PASS
- [ ] **步骤 5：Commit** `feat(mcp): Calendar 30 写入工具 / add 30 calendar write tools`

### 任务 M2-T3：QuickNotes(8) + Files(6) 写入工具

**文件：** 修改 `scripts/mcp/pim_mcp_server.py`

- [ ] **步骤 1：写失败测试**（14 个工具名）
- [ ] **步骤 2：跑失败**
- [ ] **步骤 3：实现**。`upload_quick_note_attachment` 与 `upload_file` 走 multipart（`providerId`+`path`+`file` 字段）。
- [ ] **步骤 4：跑测试** → PASS
- [ ] **步骤 5：Commit** `feat(mcp): QuickNotes 8 + Files 6 写入工具 / add quick-notes and files write tools`

### 任务 M2-T4：PcTracker(4) + Mobile(2) 写入工具

**文件：** 修改 `scripts/mcp/pim_mcp_server.py`

- [ ] **步骤 1：写失败测试**（6 个工具名）
- [ ] **步骤 2：跑失败**
- [ ] **步骤 3：实现**
  - `create_category`：**用 legacy `SaveCategoryRequest` 模型**（`appPattern`/`categoryName`/`color`/`priority`）POST `/api/v1/pc/categories`（路由冲突下新树模型被遮蔽，设计文档§4 注明"按实际路由暴露"）
  - `update_categories_order` PUT `/api/v1/pc/categories/reorder`（body `{items:[{id,parentId,sortOrder}]}`）
  - `delete_category` DELETE `/api/v1/pc/categories/{id}`
  - `seed_categories` POST `/api/v1/pc/categories/seed`
  - `create_mobile_goal` POST `/api/v1/mobile/analytics/goals`；`delete_mobile_goal` DELETE `/api/v1/mobile/analytics/goals/{goalId}`
- [ ] **步骤 4：跑测试** → PASS
- [ ] **步骤 5：Commit** `feat(mcp): PcTracker 4 + Mobile 2 写入工具 / add pctracker and mobile write tools`

### 任务 M2-T5：写入工具目录一致性与全量自检

**文件：** 修改 `scripts/mcp/pim_mcp_server.py`（新增 `_list_tools_meta`、`--check` CLI 模式）

- [ ] **步骤 1：写失败测试**
```python
def test_write_catalog_matches_python_tools():
    py_write = {n for n in _tool_names() if _is_write(n)}
    # 与 catalog 对齐：Python 侧 write 工具集合 = McpToolCatalog.WriteTools 名称
    assert len(py_write) == 50
```
- [ ] **步骤 2：跑失败**
- [ ] **步骤 3：实现**：`_WRITE_TOOL_NAMES` 集合（50 个）集中定义；`_is_write(name)` 查集合；`--check` 打印 101+50 工具数量、校验无重复、写工具全部在 `_WRITE_TOOL_NAMES`；供部署冒烟。用 `_call_api` 时写工具必带写权限校验（已由包装层完成）。
- [ ] **步骤 4：跑测试** → PASS；`python3 scripts/mcp/pim_mcp_server.py --check`
- [ ] **步骤 5：Commit** `chore(mcp): 写入工具清单校验与 --check 冒烟 / add write-tool inventory self-check`

---

## 任务 M3：WebUI MCP 管理页

### 任务 M3-T1：api/mcp.ts + 类型

**文件：** 创建 `src/client-web/src/api/mcp.ts`；修改 `src/client-web/src/types/index.ts`；测试 `tests/client-web/mcpApiPath.test.ts` + `tsconfig.mcp.json`

- [ ] **步骤 1：写失败测试**（仿 `filesApiPath.test.ts`）
```typescript
import assert from 'node:assert/strict';
import { mcpApiPaths } from '../../src/client-web/src/api/mcp';
assert.equal(mcpApiPaths.list(), '/mcp/clients');
assert.equal(mcpApiPaths.client('11111111-1111-1111-1111-111111111111'), '/mcp/clients/11111111-1111-1111-1111-111111111111');
assert.equal(mcpApiPaths.revoke('11111111-1111-1111-1111-111111111111'), '/mcp/clients/11111111-1111-1111-1111-111111111111/revoke');
assert.equal(mcpApiPaths.catalog(), '/mcp/catalog');
assert.equal(mcpApiPaths.verify(), '/mcp/verify');
```
- [ ] **步骤 2：跑失败**（tsx）
- [ ] **步骤 3：实现**：`mcpApiPaths` 常量 + `listClients/createClient/updateClient/revokeClient/deleteClient/getCatalog`（`apiGet/apiPost/apiPut/apiDelete`，`.data` 解包）。类型追加到 `types/index.ts`：`McpClient`、`McpClientCreateResult`、`McpToolInfo`、`McpCatalog`、`McpPermissionMap`。
- [ ] **步骤 4：跑测试**：`npx tsx tests/client-web/mcpApiPath.test.ts`
- [ ] **步骤 5：Commit** `feat(web): MCP api 模块与类型 / add MCP api client and types`

### 任务 M3-T2：权限编辑器组件（读 101 + 写 50 开关）

**文件：** 创建 `src/client-web/src/components/mcp/PermissionEditor.tsx`

- [ ] **步骤 1：写失败测试**（vitest 组件测试，放 `src/client-web/src/components/mcp/__tests__/PermissionEditor.test.tsx`）——组头 checkbox 半选态 + 全选/全关 + 保存回调
- [ ] **步骤 2：跑失败**（`npm --prefix src/client-web test PermissionEditor`）
- [ ] **步骤 3：实现**：props `{ readTools, writeTools, permissions, onChange }`；`<details>` 折叠 + 组头 checkbox（`ref` 设置 `indeterminate`）+ 单项 checkbox；读/写两个大区块；组标题带计数。全开/全关按钮。
- [ ] **步骤 4：跑测试** → PASS
- [ ] **步骤 5：Commit** `feat(web): 权限编辑器组件 / add permission editor component`

### 任务 M3-T3：McpSettingsPage 页面

**文件：** 创建 `src/client-web/src/pages/McpSettingsPage.tsx`；修改 `src/client-web/src/layout/AppLayout.tsx`、`src/client-web/src/pages/SettingsPage.tsx`

- [ ] **步骤 1：写失败测试**（`tests/client-web/mcpTypes.test.ts` 用 tsx 校验类型导出；页面交互测试用 vitest + jsdom 或按现有惯例做轻量 smoke）——若无现成页面级测试框架，退化为构建期类型检查 + 手工冒烟，测试任务记 `npm run build` 通过
- [ ] **步骤 2：实现页面**三段式：
  1. 客户端列表表格：名称 / 状态徽标（在线=lastSeen<5min）/ 最后活跃 / 调用次数(读+写) / 最近工具 / 行内操作（编辑权限、吊销、删除）
  2. 新建客户端：名称输入 + 生成 Token → 一次性展示（`font-mono` 大字号 + 复制按钮 + `mcp.json` 配置示例）+「我已完成保存」
  3. 权限编辑：选中客户端后 `PermissionEditor` 内嵌/抽屉，保存调 `updateClient`
  - 数据：`useQuery(['mcp-clients'])` + `refetchInterval`；变更 `useMutation` + `invalidateQueries`
- [ ] **步骤 3：路由与入口**：AppLayout 加 `<Route path="/settings/mcp" element={<McpSettingsPage/>}/>`；SettingsPage `settingsLinks` 加 `{title:'MCP 管理', description:'MCP 客户端连接与工具级权限', label:'MCP', to:'/settings/mcp'}`
- [ ] **步骤 4：验证**：`npm --prefix src/client-web run build`（含 tsc）+ `npm --prefix src/client-web run lint`
- [ ] **步骤 5：Commit** `feat(web): MCP 管理页与路由入口 / add MCP settings page`

---

## 任务 M4：文档

**文件：** 修改 `docs/mcp.md`、`README.md`

- [ ] **步骤 1：更新 `docs/mcp.md`**：
  - 头部版本号 → v3；概述加"101 读 + 50 写"
  - 新增「Phase 3 HTTP 接入」小节：`PIM_MCP_TRANSPORT=http`、host/port/path、nginx 反代 `/mcp` 示例、Token 获取（WebUI 设置 → MCP 管理页）、`mcp.json` HTTP 配置示例（`"type":"http","url":"https://home.hsww.party:15858/mcp","headers":{"Authorization":"Bearer pim_mcp_..."}`）
  - 新增「写入工具表」：50 个工具按模块列全（含方法/路径/主要参数）
  - 权限说明：读全开/写全关默认、permission denied 403 语义、401/403 处理
  - stdio 模式保留说明（v2 兼容）
- [ ] **步骤 2：更新 `README.md`** TODO：把「MCP 写入能力」待办项标记为已交付（移入功能特性/删除占位）
- [ ] **步骤 3：验证**：无构建影响（docs-only 路径不过 CI 构建），但保持中英双语
- [ ] **步骤 4：Commit** `docs(mcp): Phase 3 写入工具与 HTTP 接入文档 / document write tools and HTTP access`

---

## 任务 M5：验证、多子代理 Review、PR 与 CI

- [ ] **步骤 1：全量验证**（worktree 内）：
  - `dotnet test Pim.sln --no-restore` 全绿
  - `npm --prefix src/client-web run build` 通过；`npm --prefix src/client-web run lint` 无错
  - `python3 -m pytest scripts/mcp/test_pim_mcp_server.py -q` 通过；`python3 scripts/mcp/pim_mcp_server.py --check`
- [ ] **步骤 2：隔离检查**：主工作区 `git status` 干净；worktree diff 仅含预期文件
- [ ] **步骤 3：commit 全部 → push 分支 → `gh pr create`**（PR 描述含四段双语章节：技术修改/功能变化/如何体验/测试）
- [ ] **步骤 4：三视角 review**：派 `review-sol`/`review-terra`/`review-flash` 并行只读审查 PR；汇总分级（Critical/Important/Minor）；修复 Important+ 后重新 push → 新 head 再 review，直到无 Important+
- [ ] **步骤 5：CI 门禁**：`gh pr checks <N> --watch` 等 api/web 构建全绿
- [ ] **步骤 6：收尾**：汇报 review 结论 + CI 状态；不主动 merge（除非用户要求）

---

## 自检清单

- 规格覆盖：设计 §4 全 50 工具 ✓（M2-T2/3/4）；§5 HTTP+Token+权限 ✓（M2-T1）；§6 WebUI ✓（M3）；§7 表结构 ✓（M1-T1）；§8 管理 API ✓（M1-T5）；§9 里程碑 ✓；§10 验收标准逐条对应；§11 注意事项（复用服务、权限默认安全、Token 不落日志、multipart、verify 内网、stdio 兼容）✓
- 占位符：无
- 类型一致：`McpToolCatalog.WriteTools` 名称与 Python `_WRITE_TOOL_NAMES` 一一对应（M2-T5 校验）；`McpClientDto`/`McpVerifyResult` 与 WebUI `McpClient` 类型一致
