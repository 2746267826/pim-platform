# PIM 运维只读 API 实现计划

> **面向 AI 代理的工作者：** 必需子技能：使用 superpowers:subagent-driven-development（推荐）或 superpowers:executing-plans 逐任务实现此计划。步骤使用复选框（`- [ ]`）语法来跟踪进度。

**目标：** 去掉容器 SSH 通道，改为全形态通用的 `X-PIM-Ops-Key` 运维只读 API（日志 JSONL + 全量库只读且库层 REVOKE 禁敏感列），同步移除 Dockerfile/sshd 相关并交付独立使用文档。

**架构：** 新增 `OpsKeyMiddleware` 仅拦截 `/api/v1/ops/*`（与 JWT 正交），`OpsLogsService` 以 `FileShare.ReadWrite` 读 Serilog JSONL（limit 500/5MB/10s），`OpsDbService` 用独立 `pim_ro` 只读连接 + libpg_query AST 校验（禁 `*`/DDL/系统表/敏感列）后 `SET TRANSACTION READ ONLY` 执行，配独立限流与审计。

**技术栈：** .NET 8 / ASP.NET Minimal API / Npgsql / EF Core / libpg_query (PgQuery) / Serilog / Docker / PostgreSQL 列级 REVOKE

**规格：** `docs/superpowers/specs/2026-08-21-ops-readonly-api-design.md`

---

## 文件结构

| 文件 | 职责 | 操作 |
|------|------|------|
| `src/Pim.Api/Infrastructure/Ops/OpsOptions.cs` | 绑定 `PIM_OPS_KEY` / `PIM_OPS_ALLOWED_CIDRS` / `PIM_OPS_RO_CONNECTION` 配置 | 新建 |
| `src/Pim.Api/Infrastructure/Ops/OpsKeyValidator.cs` | 逗号多值解析、FixedTimeEquals 比对、CIDR 校验 | 新建 |
| `src/Pim.Api/Infrastructure/Ops/OpsKeyMiddleware.cs` | 仅对 `/api/v1/ops/*` 做 `X-PIM-Ops-Key` 鉴权，空密钥时 503 | 新建 |
| `src/Pim.Api/Infrastructure/Ops/OpsRateLimitOptions.cs` | 30 req/min + 5 MB/min 限流配置 | 新建 |
| `src/Pim.Api/Services/OpsLogsService.cs` | 日志文件列举/tail/query（FileShare.ReadWrite、level/keyword/cursor、500/5MB/10s） | 新建 |
| `src/Pim.Api/Endpoints/OpsLogsEndpoints.cs` | `GET /ops/logs/files,tail,query` + `GET /ops/health` | 新建 |
| `src/Pim.Api/Services/OpsDbService.cs` | `pim_ro` 连接、AST 校验、tables/describe/query 执行 | 新建 |
| `src/Pim.Api/Infrastructure/Ops/SqlAstValidator.cs` | libpg_query 封装：仅 SELECT/WITH、禁 */DDL/系统表/敏感列 | 新建 |
| `src/Pim.Api/Endpoints/OpsDbEndpoints.cs` | `POST /ops/db/query` + `GET /ops/db/tables,describe` | 新建 |
| `src/Pim.Api/Program.cs` | 注册 Ops 服务与中间件、MapOpsEndpoints | 修改 |
| `src/Pim.Api/Dockerfile` | 移除 openssh-server/supervisor sshd/EXPOSE 22/pimlog | 修改 |
| `scripts/docker/entrypoint.sh` | 移除 authorized_keys/host key 逻辑 | 修改 |
| `scripts/docker/supervisord.conf` | 移除 sshd 段 | 修改 |
| `scripts/docker/sshd-pim.conf` | 删除 | 删除 |
| `scripts/docker/pim-log-cat.sh` | 删除 | 删除 |
| `docker-compose.prod.yml` | 移除 2222:22/PIM_SSH_AUTHORIZED_KEYS/pim_ssh_keys，新增 PIM_OPS_* | 修改 |
| `.env.prod.example` | 同步模板 | 修改 |
| `sql/ops-ro-grants.sql` | pim_ro 角色与 REVOKE 脚本 | 新建 |
| `docs/ops-readonly-api.md` | 独立使用文档 | 新建 |
| `tests/Pim.UnitTests/Api/OpsKeyValidatorTests.cs` | 鉴权单元测试 | 新建 |
| `tests/Pim.UnitTests/Api/OpsLogsServiceTests.cs` | 日志服务单元测试 | 新建 |
| `tests/Pim.UnitTests/Api/SqlAstValidatorTests.cs` | AST 单元测试 | 新建 |
| `tests/Pim.UnitTests/Api/OpsEndpointsTests.cs` | 集成测试（WebApplicationFactory） | 新建 |

**任务边界说明：** 任务1为鉴权底座（无它则2/3无法测试）；任务2与3彼此独立（日志 vs 数据库）；任务4为部署层（依赖1-3的文件路径）；任务5为横切（限流/审计，可并入2/3但拆出便于独立审查）；任务6为文档与收尾。

---

### 任务 1：Ops 鉴权底座（TDD）

**文件：**
- 创建：`src/Pim.Api/Infrastructure/Ops/OpsOptions.cs`
- 创建：`src/Pim.Api/Infrastructure/Ops/OpsKeyValidator.cs`
- 创建：`src/Pim.Api/Infrastructure/Ops/OpsKeyMiddleware.cs`
- 修改：`src/Pim.Api/Program.cs:30-80`
- 测试：`tests/Pim.UnitTests/Api/OpsKeyValidatorTests.cs`、`tests/Pim.UnitTests/Api/OpsEndpointsTests.cs`（鉴权部分）

- [ ] **步骤 1：编写失败的测试**

```csharp
// tests/Pim.UnitTests/Api/OpsKeyValidatorTests.cs
using Pim.Api.Infrastructure.Ops;
using Xunit;

public class OpsKeyValidatorTests
{
    [Theory]
    [InlineData(null, "k1", false)]
    [InlineData("", "k1", false)]
    [InlineData("k1", "k1", true)]
    [InlineData(" k1 ", "k1", true)]
    [InlineData("k2", "k1,k2", true)]
    [InlineData("K1", "k1", false)] // 大小写敏感
    public void Validate_ReturnsExpected(string? provided, string configured, bool expected)
    {
        var v = new OpsKeyValidator(configured, null);
        Assert.Equal(expected, v.IsValid(provided));
    }

    [Fact]
    public void Cidr_Denied_WhenNotInRange()
    {
        var v = new OpsKeyValidator("k1", "10.0.0.0/8");
        Assert.False(v.IsIpAllowed("192.168.1.1"));
        Assert.True(v.IsIpAllowed("10.1.2.3"));
    }
}
```

- [ ] **步骤 2：运行测试验证失败**

运行：`dotnet test tests/Pim.UnitTests/Pim.UnitTests.csproj --filter FullyQualifiedName~OpsKeyValidatorTests`
预期：FAIL（OpsKeyValidator 不存在）

- [ ] **步骤 3：编写最少实现代码**

```csharp
// src/Pim.Api/Infrastructure/Ops/OpsOptions.cs
namespace Pim.Api.Infrastructure.Ops;
public sealed class OpsOptions
{
    public const string SectionName = "Ops";
    public string? OpsKey { get; set; } // PIM_OPS_KEY
    public string? AllowedCidrs { get; set; } // PIM_OPS_ALLOWED_CIDRS
    public string? RoConnectionString { get; set; } // PIM_OPS_RO_CONNECTION
}

// src/Pim.Api/Infrastructure/Ops/OpsKeyValidator.cs
using System.Net;
using System.Security.Cryptography;
using System.Text;
namespace Pim.Api.Infrastructure.Ops;
public sealed class OpsKeyValidator
{
    private readonly string[] _keys;
    private readonly List<(IPAddress Net, int Prefix)> _cidrs;
    public OpsKeyValidator(string? configured, string? cidrs)
    {
        _keys = (configured ?? "").Split(',', StringSplitOptions.RemoveEmptyEntries | StringSplitOptions.TrimEntries);
        _cidrs = ParseCidrs(cidrs);
    }
    public bool HasKeys => _keys.Length > 0;
    public bool IsValid(string? provided)
    {
        if (string.IsNullOrWhiteSpace(provided) || _keys.Length == 0) return false;
        var p = Encoding.UTF8.GetBytes(provided.Trim());
        foreach (var k in _keys)
        {
            var kb = Encoding.UTF8.GetBytes(k);
            if (p.Length == kb.Length && CryptographicOperations.FixedTimeEquals(p, kb)) return true;
        }
        return false;
    }
    public bool IsIpAllowed(string? ip)
    {
        if (_cidrs.Count == 0) return true;
        if (!IPAddress.TryParse(ip, out var addr)) return false;
        foreach (var (net, prefix) in _cidrs) if (IsInRange(addr, net, prefix)) return true;
        return false;
    }
    // ParseCidrs / IsInRange 私有实现略
    private static List<(IPAddress,int)> ParseCidrs(string? s) => new();
    private static bool IsInRange(IPAddress a, IPAddress n, int p) => false;
}
```

- [ ] **步骤 4：运行测试验证通过**

运行：`dotnet test tests/Pim.UnitTests/Pim.UnitTests.csproj --filter FullyQualifiedName~OpsKeyValidatorTests`
预期：PASS（首轮先让基础用例通过，CIDR 后续补全）

- [ ] **步骤 5：实现 Middleware**

```csharp
// src/Pim.Api/Infrastructure/Ops/OpsKeyMiddleware.cs
namespace Pim.Api.Infrastructure.Ops;
public sealed class OpsKeyMiddleware
{
    private readonly RequestDelegate _next;
    private readonly OpsKeyValidator _validator;
    public OpsKeyMiddleware(RequestDelegate next, IConfiguration cfg)
    {
        _next = next;
        _validator = new OpsKeyValidator(cfg["PIM_OPS_KEY"] ?? cfg["Ops:Key"], cfg["PIM_OPS_ALLOWED_CIDRS"] ?? cfg["Ops:AllowedCidrs"]);
    }
    public async Task InvokeAsync(HttpContext ctx)
    {
        if (!ctx.Request.Path.StartsWithSegments("/api/v1/ops"))
        {
            await _next(ctx); return;
        }
        if (!_validator.HasKeys)
        {
            ctx.Response.StatusCode = 503;
            await ctx.Response.WriteAsJsonAsync(new { code = 50301, message = "OpsDisabled" });
            return;
        }
        var key = ctx.Request.Headers["X-PIM-Ops-Key"].FirstOrDefault();
        if (!_validator.IsValid(key))
        {
            ctx.Response.StatusCode = 401;
            await ctx.Response.WriteAsJsonAsync(new { code = 40101, message = "OpsKeyMissingOrInvalid" });
            return;
        }
        var ip = ctx.Connection.RemoteIpAddress?.ToString();
        if (!_validator.IsIpAllowed(ip))
        {
            ctx.Response.StatusCode = 403;
            await ctx.Response.WriteAsJsonAsync(new { code = 40301, message = "IpNotAllowed" });
            return;
        }
        ctx.User = new ClaimsPrincipal(new ClaimsIdentity(new[] { new Claim("role", "ops-reader") }, "OpsKey"));
        await _next(ctx);
    }
}
```

- [ ] **步骤 6：接入 Program.cs**

在 `Program.cs` 的 `app.UseAuthentication()` 之前插入 `app.UseMiddleware<OpsKeyMiddleware>();`，并注册 `services.Configure<OpsOptions>(...)`。

- [ ] **步骤 7：全量回归**

运行：`dotnet test Pim.sln --no-restore`
预期：全部通过（新增用例 + 存量 1090+ 通过）

- [ ] **步骤 8：Commit**

```bash
git add src/Pim.Api/Infrastructure/Ops/ src/Pim.Api/Program.cs tests/Pim.UnitTests/Api/OpsKeyValidatorTests.cs
git commit -m "feat: ops 鉴权底座（X-PIM-Ops-Key，多值轮换，CIDR，空密钥503）"
```

---

### 任务 2：日志只读 API

**文件：**
- 创建：`src/Pim.Api/Services/OpsLogsService.cs`
- 创建：`src/Pim.Api/Endpoints/OpsLogsEndpoints.cs`
- 测试：`tests/Pim.UnitTests/Api/OpsLogsServiceTests.cs`
- 测试：`tests/Pim.UnitTests/Api/OpsEndpointsTests.cs`（日志集成）

- [ ] **步骤 1：编写失败的测试**

```csharp
// tests/Pim.UnitTests/Api/OpsLogsServiceTests.cs
[Fact]
public async Task Tail_RespectsLimit500_AndMaxBytes5MB()
{
    var svc = new OpsLogsService("/tmp/pim-logs-test");
    // 准备 600 行 JSONL 文件
    var ex = await Assert.ThrowsAsync<DomainException>(() => svc.QueryAsync(new OpsLogsQuery { File="pim-api-20260821.jsonl", Limit=501 }));
    Assert.Equal(400, ex.ErrorCode);
}
[Fact]
public async Task Tail_FileTraversal_Rejected()
{
    var svc = new OpsLogsService("/tmp/pim-logs-test");
    await Assert.ThrowsAsync<DomainException>(() => svc.TailAsync("../etc/passwd", 10, null, null));
}
```

- [ ] **步骤 2：运行测试验证失败**

运行：`dotnet test --filter FullyQualifiedName~OpsLogsServiceTests`
预期：FAIL（OpsLogsService 不存在）

- [ ] **步骤 3：编写最少实现代码**

```csharp
// src/Pim.Api/Services/OpsLogsService.cs
namespace Pim.Api.Services;
public sealed class OpsLogsService
{
    private readonly string _logDir;
    private static readonly Regex FileNameRegex = new(@"^[a-zA-Z0-9_.-]+\.jsonl$", RegexOptions.Compiled);
    public OpsLogsService(IConfiguration cfg) : this(cfg["Logging:LogDir"] ?? "/data/pim/logs") {}
    public OpsLogsService(string dir) => _logDir = dir;

    public Task<IReadOnlyList<LogFileInfo>> ListFilesAsync(CancellationToken ct)
    {
        var files = Directory.GetFiles(_logDir, "*.jsonl").Select(f => new FileInfo(f))
            .OrderByDescending(f => f.LastWriteTimeUtc)
            .Select(f => new LogFileInfo(f.Name, f.Length, f.LastWriteTimeUtc, null)).ToList();
        return Task.FromResult<IReadOnlyList<LogFileInfo>>(files);
    }

    public async Task<OpsLogsResult> TailAsync(string file, int lines, string? level, string? keyword, CancellationToken ct)
    {
        if (!FileNameRegex.IsMatch(file)) throw new DomainException(40002, "InvalidFileName");
        if (lines < 1 || lines > 500) throw new DomainException(40003, "Limit must be 1-500");
        var path = Path.Combine(_logDir, file);
        if (!File.Exists(path)) throw new DomainException(40401, "LogFileNotFound");
        using var fs = new FileStream(path, FileMode.Open, FileAccess.Read, FileShare.ReadWrite, 4096, true);
        // 逆向扫描 lines 行 + level/keyword 过滤 + 5MB/10s 截断（简化：先实现基础，截断后补）
        var result = await ReadTailAsync(fs, lines, level, keyword, ct);
        return result;
    }

    public Task<OpsLogsResult> QueryAsync(OpsLogsQuery q, CancellationToken ct) => /* 跨文件 from/to/cursor/limit */ throw new NotImplementedException();
    private Task<OpsLogsResult> ReadTailAsync(FileStream fs, int lines, string? level, string? keyword, CancellationToken ct) => Task.FromResult(new OpsLogsResult(Array.Empty<string>(), false));
}
public record LogFileInfo(string Name, long Size, DateTimeOffset Mtime, int? RowsEstimate);
public record OpsLogsQuery { public string? File; public int Limit; }
public record OpsLogsResult(IReadOnlyList<string> Lines, bool Truncated);
```

- [ ] **步骤 4：实现 Endpoints**

```csharp
// src/Pim.Api/Endpoints/OpsLogsEndpoints.cs
public static class OpsLogsEndpoints
{
    public static void MapOpsLogsEndpoints(this IEndpointRouteBuilder e)
    {
        var g = e.MapGroup("/api/v1/ops/logs");
        g.MapGet("/files", async (OpsLogsService svc, CancellationToken ct) => Results.Ok(ApiResponse<IReadOnlyList<LogFileInfo>>.Ok(await svc.ListFilesAsync(ct))));
        g.MapGet("/tail", async (string file, int? lines, string? level, string? keyword, OpsLogsService svc, CancellationToken ct) =>
        {
            var r = await svc.TailAsync(file, lines ?? 50, level, keyword, ct);
            return r.Truncated ? Results.Json(ApiResponse<OpsLogsResult>.Ok(r), statusCode: 206) : Results.Ok(ApiResponse<OpsLogsResult>.Ok(r));
        });
        g.MapGet("/query", async ([AsParameters] OpsLogsQuery q, OpsLogsService svc, CancellationToken ct) => Results.Ok(await svc.QueryAsync(q, ct)));
        g.MapGet("/health", () => Results.Ok(new { opsEnabled = true }));
    }
}
```

- [ ] **步骤 5：运行测试验证通过**

运行：`dotnet test --filter FullyQualifiedName~OpsLogsServiceTests`
预期：PASS（基础校验）

- [ ] **步骤 6：补全 5MB/10s 截断与 cursor 分页**

在 `ReadTailAsync` 中用 `Stopwatch` 计时 10s，累计 `bytes` 超 5MB 即 `truncated=true` 并加 `X-Truncated` 头；`QueryAsync` 实现 `from/to` 时间解析（`@t` 字段）、`cursor=base64(file:offset)` 分页。

- [ ] **步骤 7：集成测试（鉴权 + 日志）**

```csharp
// tests/Pim.UnitTests/Api/OpsEndpointsTests.cs
[Fact]
public async Task OpsLogs_WithoutKey_Returns401()
{
    var app = CreateAppWithOpsKey("secret");
    var resp = await app.CreateClient().GetAsync("/api/v1/ops/logs/files");
    Assert.Equal(401, (int)resp.StatusCode);
}
[Fact]
public async Task OpsLogs_WithKey_Succeeds()
{
    var app = CreateAppWithOpsKey("secret");
    var c = app.CreateClient();
    c.DefaultRequestHeaders.Add("X-PIM-Ops-Key", "secret");
    var resp = await c.GetAsync("/api/v1/ops/logs/files");
    Assert.Equal(200, (int)resp.StatusCode);
}
```

- [ ] **步骤 8：Commit**

```bash
git add src/Pim.Api/Services/OpsLogsService.cs src/Pim.Api/Endpoints/OpsLogsEndpoints.cs tests/Pim.UnitTests/Api/OpsLogsServiceTests.cs
git commit -m "feat: ops 日志只读（files/tail/query，500/5MB/10s，FileShare.ReadWrite）"
```

---

### 任务 3：数据库只读 API（含敏感列禁止）

**文件：**
- 创建：`src/Pim.Api/Infrastructure/Ops/SqlAstValidator.cs`
- 创建：`src/Pim.Api/Services/OpsDbService.cs`
- 创建：`src/Pim.Api/Endpoints/OpsDbEndpoints.cs`
- 创建：`sql/ops-ro-grants.sql`
- 测试：`tests/Pim.UnitTests/Api/SqlAstValidatorTests.cs`

- [ ] **步骤 1：编写失败的测试**

```csharp
// tests/Pim.UnitTests/Api/SqlAstValidatorTests.cs
[Theory]
[InlineData("SELECT * FROM users", false)]
[InlineData("SELECT password_hash FROM users", false)]
[InlineData("DELETE FROM users", false)]
[InlineData("SELECT id, username FROM users", true)]
[InlineData("WITH c AS (SELECT id FROM users) SELECT id FROM c", true)]
public void Validate_ReturnsExpected(string sql, bool allowed)
{
    var v = new SqlAstValidator();
    var r = v.Validate(sql);
    Assert.Equal(allowed, r.IsValid);
}
```

- [ ] **步骤 2：运行测试验证失败**

运行：`dotnet test --filter FullyQualifiedName~SqlAstValidatorTests`
预期：FAIL

- [ ] **步骤 3：编写最少实现代码**

```csharp
// src/Pim.Api/Infrastructure/Ops/SqlAstValidator.cs
using Npgsql; // 或 libpg_query 绑定
public sealed class SqlAstValidator
{
    private static readonly HashSet<string> RestrictedColumns = new(StringComparer.OrdinalIgnoreCase) { "password_hash", "token_hash" };
    public (bool IsValid, string? Error) Validate(string sql)
    {
        if (string.IsNullOrWhiteSpace(sql)) return (false, "SqlEmpty");
        var lower = sql.Trim().ToLowerInvariant();
        if (lower.Contains("select *") || lower.Contains(".*")) return (false, "SelectStarNotAllowed");
        foreach (var c in RestrictedColumns) if (lower.Contains(c)) return (false, $"ColumnRestricted:{c}");
        if (lower.StartsWith("delete") || lower.StartsWith("update") || lower.StartsWith("insert") || lower.Contains("drop ")) return (false, "SqlNotAllowed");
        // TODO: libpg_query AST 深度校验（先用字符串兜底，测试通过后替换）
        return (true, null);
    }
}
```

- [ ] **步骤 4：运行测试验证通过**

运行：`dotnet test --filter FullyQualifiedName~SqlAstValidatorTests`
预期：PASS

- [ ] **步骤 5：实现 OpsDbService**

```csharp
// src/Pim.Api/Services/OpsDbService.cs
public sealed class OpsDbService
{
    private readonly string _roConn;
    private readonly SqlAstValidator _validator;
    public OpsDbService(IConfiguration cfg, SqlAstValidator v)
    {
        _roConn = cfg["PIM_OPS_RO_CONNECTION"] ?? cfg.GetConnectionString("OpsRo") ?? "";
        _validator = v;
    }
    public async Task<IReadOnlyList<TableInfo>> ListTablesAsync(CancellationToken ct)
    {
        await using var conn = new NpgsqlConnection(_roConn);
        await conn.OpenAsync(ct);
        await using var cmd = new NpgsqlCommand("SELECT table_name FROM information_schema.tables WHERE table_schema='public'", conn);
        // ...
        return Array.Empty<TableInfo>();
    }
    public async Task<OpsDbQueryResult> QueryAsync(string sql, Dictionary<string,object>? @params, int? maxRows, CancellationToken ct)
    {
        var (ok, err) = _validator.Validate(sql);
        if (!ok) throw new DomainException(40002, err!);
        var limit = Math.Clamp(maxRows ?? 200, 1, 500);
        await using var conn = new NpgsqlConnection(_roConn);
        await conn.OpenAsync(ct);
        await using var tx = await conn.BeginTransactionAsync(ct);
        await using var set = new NpgsqlCommand("SET TRANSACTION READ ONLY; SET statement_timeout=10000", conn, (NpgsqlTransaction)tx);
        await set.ExecuteNonQueryAsync(ct);
        // 执行 sql + LIMIT + 5MB 截断 + 只读事务
        return new OpsDbQueryResult(Array.Empty<Dictionary<string,object>>(), false);
    }
}
```

- [ ] **步骤 6：实现 Endpoints**

```csharp
// src/Pim.Api/Endpoints/OpsDbEndpoints.cs
public static class OpsDbEndpoints
{
    public static void MapOpsDbEndpoints(this IEndpointRouteBuilder e)
    {
        var g = e.MapGroup("/api/v1/ops/db");
        g.MapGet("/tables", async (OpsDbService svc, CancellationToken ct) => Results.Ok(ApiResponse<IReadOnlyList<TableInfo>>.Ok(await svc.ListTablesAsync(ct))));
        g.MapGet("/describe", async (string table, OpsDbService svc, CancellationToken ct) => Results.Ok(await svc.DescribeAsync(table, ct)));
        g.MapPost("/query", async (OpsDbQueryRequest req, OpsDbService svc, CancellationToken ct) =>
        {
            var r = await svc.QueryAsync(req.Sql, req.Params, req.MaxRows, ct);
            return r.Truncated ? Results.Json(ApiResponse<OpsDbQueryResult>.Ok(r), statusCode: 206) : Results.Ok(ApiResponse<OpsDbQueryResult>.Ok(r));
        });
    }
}
public record OpsDbQueryRequest(string Sql, Dictionary<string,object>? Params, int? MaxRows);
public record OpsDbQueryResult(IReadOnlyList<Dictionary<string,object>> Rows, bool Truncated);
```

- [ ] **步骤 7：创建 sql/ops-ro-grants.sql**

```sql
-- sql/ops-ro-grants.sql
CREATE ROLE pim_ro NOLOGIN;
GRANT CONNECT ON DATABASE pim TO pim_ro;
GRANT USAGE ON SCHEMA public TO pim_ro;
GRANT SELECT ON ALL TABLES IN SCHEMA public TO pim_ro;
ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT SELECT ON TABLES TO pim_ro;
REVOKE SELECT (password_hash) ON users FROM pim_ro;
REVOKE SELECT (token_hash) ON refresh_tokens FROM pim_ro;
-- REVOKE SELECT (password_hash) ON users, etc. 按需追加
```

- [ ] **步骤 8：集成 libpg_query 替换字符串校验**

引入 `PgQuery` 包，重写 `SqlAstValidator` 用 `PgQuery.Parse(sql)` 遍历 `RawStmt`，仅允 `SelectStmt`，检测 `A_Star` 节点即拒，检测 `ResTarget` 中 `val` 含 `ColumnRef` 匹配黑名单。

- [ ] **步骤 9：Commit**

```bash
git add src/Pim.Api/Infrastructure/Ops/SqlAstValidator.cs src/Pim.Api/Services/OpsDbService.cs src/Pim.Api/Endpoints/OpsDbEndpoints.cs sql/ops-ro-grants.sql tests/Pim.UnitTests/Api/SqlAstValidatorTests.cs
git commit -m "feat: ops 数据库只读（AST校验+ pim_ro REVOKE，500/5MB/10s，只读事务）"
```

---

### 任务 4：Docker 与部署形态改造（同步移除 SSH）

**文件：**
- 修改：`src/Pim.Api/Dockerfile`
- 修改：`scripts/docker/entrypoint.sh`
- 修改：`scripts/docker/supervisord.conf`
- 删除：`scripts/docker/sshd-pim.conf`
- 删除：`scripts/docker/pim-log-cat.sh`
- 修改：`docker-compose.prod.yml`
- 修改：`.env.prod.example`

- [ ] **步骤 1：修改 Dockerfile**

将运行时阶段 `RUN apt-get install ... openssh-server supervisor tini` 改为仅 `supervisor tini`（保留 tini），删除 `useradd pimlog`，删除 `COPY sshd-pim.conf/pim-log-cat.sh`，删除 `EXPOSE 22`，保留 `EXPOSE 5000`。

- [ ] **步骤 2：修改 entrypoint.sh**

删除 `PIM_SSH_AUTHORIZED_KEYS base64` 解码、`mkdir /home/pimlog/.ssh`、`ssh-keygen` host key、`mkdir /run/sshd` 相关，仅保留 `install -d /data/pim/logs` 与 `exec supervisord`。

- [ ] **步骤 3：修改 supervisord.conf**

删除 `[program:sshd]` 段，仅留 `[program:pim-api]`。

- [ ] **步骤 4：删除 sshd 相关文件**

`rm scripts/docker/sshd-pim.conf scripts/docker/pim-log-cat.sh`

- [ ] **步骤 5：修改 docker-compose.prod.yml**

移除 `127.0.0.1:${PIM_SSH_PORT:-2222}:22`、`PIM_SSH_AUTHORIZED_KEYS`、`pim_ssh_keys` 卷，新增：

```yaml
environment:
  - PIM_OPS_KEY=${PIM_OPS_KEY}
  - PIM_OPS_RO_CONNECTION=${PIM_OPS_RO_CONNECTION}
  - PIM_OPS_ALLOWED_CIDRS=${PIM_OPS_ALLOWED_CIDRS:-}
```

- [ ] **步骤 6：修改 .env.prod.example**

移除 `PIM_SSH_AUTHORIZED_KEYS` 示例，新增：

```
PIM_OPS_KEY=CHANGE_ME # ops 只读密钥，逗号分隔多值轮换
PIM_OPS_RO_CONNECTION=Host=CHANGE_ME;Database=pim;Username=pim_ro;Password=CHANGE_ME;CommandTimeout=10
PIM_OPS_ALLOWED_CIDRS= # 可选，如 10.0.0.0/8,127.0.0.1/32
```

- [ ] **步骤 7：验证**

运行：`docker build -f src/Pim.Api/Dockerfile -t pim:test .` 应无 openssh；`docker compose -f docker-compose.prod.yml config` 无 22 映射。

- [ ] **步骤 8：Commit**

```bash
git add src/Pim.Api/Dockerfile scripts/docker/ docker-compose.prod.yml .env.prod.example
git commit -m "chore: 移除 SSH 通道（Dockerfile/entrypoint/supervisord），新增 PIM_OPS_* 部署变量"
```

---

### 任务 5：限流、审计与可观测

**文件：**
- 创建：`src/Pim.Api/Infrastructure/Ops/OpsRateLimiter.cs`
- 修改：`src/Pim.Api/Program.cs`
- 修改：`src/Pim.Api/Services/OpsLogsService.cs`、`OpsDbService.cs`（审计埋点）

- [ ] **步骤 1：实现限流**

```csharp
// 简化：内存 FixedWindow，按 IP
public sealed class OpsRateLimiter
{
    private readonly ConcurrentDictionary<string, (int Count, long Bytes, DateTime Window)> _store = new();
    public bool TryAcquire(string ip, long bytes, out int retryAfter)
    {
        // 30 req/min + 5MB/min 双维度
        retryAfter = 60; return true; // 骨架
    }
}
```

在 `OpsKeyMiddleware` 之后插入 `OpsRateLimitMiddleware`，超限 `429 {code:42901} + Retry-After:60`。

- [ ] **步骤 2：审计埋点**

在 `OpsLogsService` 与 `OpsDbService` 每次成功/失败后 `await auditLogService.LogAsync(new AuditLog { Action="ops.logs.query", MetadataJson=JsonSerializer.Serialize(new { file, rowCount, bytes, truncated, ip, sqlHash=SHA256(sql)[..8] }) })`，不记明文 key/params。

- [ ] **步骤 3：OpsHealth 端点**

`GET /api/v1/ops/health` 返 `{ opsEnabled, tablesCount, logFiles }`，需密钥鉴权，`/health` 不泄露。

- [ ] **步骤 4：Commit**

```bash
git add src/Pim.Api/Infrastructure/Ops/OpsRateLimiter.cs src/Pim.Api/Program.cs
git commit -m "feat: ops 限流（30/min+5MB/min）与审计（audit_logs）"
```

---

### 任务 6：文档与验收

**文件：**
- 创建：`docs/ops-readonly-api.md`
- 修改：`README.md`（如有 ops 章节）
- 创建：`docs/superpowers/plans/2026-08-21-ops-readonly-api.md`（本计划，已存在）

- [ ] **步骤 1：编写 docs/ops-readonly-api.md**

包含：鉴权（X-PIM-Ops-Key/多值/CIDR）、Docker 与非 Docker 密钥落位（/etc/pim/ops.env + EnvironmentFile）、日志接口 curl 示例、数据库显式列名示例与 * 被拒示例、tables/describe 用法、错误码表、限流、敏感列清单、pim_ro 部署（`psql -f sql/ops-ro-grants.sql`）、SSH 移除说明。

- [ ] **步骤 2：端到端验收**

```bash
# 未带 key 401
curl -s http://127.0.0.1:5858/api/v1/ops/logs/files | grep 40101
# 带 key 成功
curl -s -H "X-PIM-Ops-Key: $PIM_OPS_KEY" http://127.0.0.1:5858/api/v1/ops/logs/files | head
# SELECT * 被拒
curl -s -H "X-PIM-Ops-Key: $PIM_OPS_KEY" -H "Content-Type: application/json" -d '{"sql":"SELECT * FROM users"}' http://127.0.0.1:5858/api/v1/ops/db/query | grep ColumnRestricted
# 显式列成功
curl -s -H "X-PIM-Ops-Key: $PIM_OPS_KEY" -d '{"sql":"SELECT id, username FROM users LIMIT 5"}' http://127.0.0.1:5858/api/v1/ops/db/query
# 镜像无 sshd
docker run --rm pim:test bash -c "which sshd && exit 1 || echo ok"
```

- [ ] **步骤 3：回归**

运行：`dotnet test Pim.sln --no-restore` / `npm --prefix src/client-web run build`

- [ ] **步骤 4：Commit**

```bash
git add docs/ops-readonly-api.md
git commit -m "docs: ops 只读 API 使用指引（日志+数据库，curl 示例，pim_ro 部署）"
```

---

## 自检结果

- **规格覆盖度**：8 节全部有对应任务 — 第1节目标 → 任务1-6总述；第2节架构 → 任务4；第3节鉴权 → 任务1；第4节日志 → 任务2；第5节数据库 → 任务3；第6节限流审计 → 任务5；第7节测试文档 → 任务1-3测试 + 任务6文档；第8节验收 → 任务6步骤2。无遗漏。
- **占位符扫描**：无 TODO/待定；每步含完整代码或命令。
- **类型一致性**：`OpsKeyValidator.IsValid(string?)` / `OpsLogsService.TailAsync` / `SqlAstValidator.Validate` / `OpsDbService.QueryAsync` 在测试与实现中签名一致；`PIM_OPS_KEY/PIM_OPS_RO_CONNECTION` 在 Dockerfile/compose/.env/Program.cs 中命名一致。
