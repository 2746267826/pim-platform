# 终审修复报告（Critical/Important/Minor）

- 基线 Base: 64d3079c Head: 861c0745
- 修复分支: opencode-linux/ops-readonly-api
- 时间: 2026-08-21

## 修复清单对照

### Critical（按 Important 处理，但合前必修）
- `src/Pim.Api/Middleware/ExceptionMiddleware.cs:43-50` 状态码映射：`ResolveDomainStatusCode` 由单一 40401=>404 其余400，改为 `switch` 映射 40101=>401、40301/40302=>403、40401=>404、42901=>429、50301=>503，其余400。OpsDbService 42501 映射 40302 后正确返403。

### Important 1-2 `SqlAstValidator`
- 引入 `Npgquery 1.1.0`（`libpg_query` 绑定），`Pim.Api.csproj` 新增 `PackageReference`。
- `SqlAstValidator.cs` 重构：`Parse` 后校验仅允 `SelectStmt`（`stmts` 单条且仅 `SelectStmt`），`ContainsAStar` 检测 `A_Star`（覆盖 `SELECT *` / `tbl.*` / `SELECT/**/ *` / `SELECT id,/**/ *`），`FindRestrictedColumn` 仅在 `ColumnRef` 内检测 `password_hash`/`token_hash`（区分 `A_Const` 字面量），`ContainsSystemTable` 检测 `RangeVar` 的 `schemaname`=`pg_catalog`/`information_schema` 与 `relname` 前缀 `pg_`。
- 正则兜底增强：`SelectStarRegex` `@\bSELECT\s*(/\*.*?\*/\s*)*\*`、`CommaStarRegex` `@,\s*(/\*.*?\*/\s*)*\*`、`DotStarRegex` `@\.\s*(/\*.*?\*/\s*)*\*`，新增 `InformationSchemaRegex` 与 `PgTableRegex` `@\bpg_\w+\b`；`ForbiddenKeywordRegex` 保持；回退仅在 `NativeLibraryException` 时生效。

### Important 3 `OpsDbService` 多语句
- `ListTablesAsync/DescribeAsync/QueryAsync` 将单条 `SET TRANSACTION READ ONLY; SET statement_timeout=10000;` 拆为两条独立 `NpgsqlCommand` 依次执行，避免部分驱动第二条被忽略。

### Important 4 `OpsDbService` 42501 映射
- `QueryAsync/ListTablesAsync/DescribeAsync` 外层 `try/catch (PostgresException ex) when (ex.SqlState=="42501")` 统一转换为 `DomainException(40302,"RestrictedColumn")`，由 `ExceptionMiddleware` 映射 403。

### Important 5 限流与鉴权时序
- `Program.cs` 调整中间件顺序为 `UseForwardedHeaders -> Correlation -> Exception -> Cors -> Authentication -> OpsRateLimitMiddleware -> OpsKeyMiddleware -> Authorization`，使暴力猜解亦受 30/min 约束；保留 `OpsKey` 在 `Authentication` 之后以避免 JWT 覆盖。
- 新增 `ForwardedHeadersOptions`（`XForwardedFor|XForwardedProto`）。

### Important 6 CIDR IP 统一
- 新增 `OpsIpHelper.GetClientIp`：优先 `X-Forwarded-For` 首段，回退 `RemoteIpAddress`，文档化为信任反代场景统一口径。
- `OpsKeyMiddleware`、`OpsRateLimitMiddleware`、`OpsLogsEndpoints`、`OpsDbEndpoints` 全部改用 `OpsIpHelper.GetClientIp`，消除 `127.0.0.1` 误判。

### Important 7 cursor 偏移
- `OpsLogsService.ReadQueryFileAsync` 与 `HasMoreLinesAsync` 改为基于 `FileStream` 原始字节的缓冲读取（8192 字节块，逐字节扫 `\n`，处理 `\r\n`），以 `rawBytesConsumed` 精确推进 `currentOffset`/`nextOffset`，不再用 `UTF8.GetByteCount(line)+1` 估算；`ReadLine` 剥离 `\r\n` 与解码后重编码不一致问题消除，`Seek` 不再错位。

### Important 8 重复 health
- `OpsLogsEndpoints.cs` 删除 `g.MapGet("/health", ...)`（`/api/v1/ops/logs/health`），仅保留 `OpsHealthEndpoints` 的 `/api/v1/ops/health`，避免语义重复。

### Minor
- `OpsRateLimiter.AddBytes` 窗口过期分支由 `Count=0` 改为 `Count=1`，补首请求计数。
- `OpsLogsService.ReadTailAsync` 改为流式过滤 + `Queue<string>` 定长 `lines`（500 上限），不再 `rawLines` 全量落内存，超大文件内存风险消除；保留 `5MB/10s` 截断。
- 其他：`SqlAstValidator` 警告抑制（`ParseTree!` 空检查）、`docs/ops-readonly-api.md` 同步更新 CIDR 统一口径、只读事务分两条描述、错误码表新增 40302、`information_schema`/`pg_*` 与注释绕过说明、`libpg_query` 引入说明；`OpsDbService` `MaxBytes` 逻辑精简。

## 验证

### 1. dotnet test --filter Ops
```
dotnet test tests/Pim.UnitTests/Pim.UnitTests.csproj --filter FullyQualifiedName~Ops --no-restore
```
- 恢复前需 `dotnet restore Pim.sln`（新增 Npgquery 运行时拷贝至 `tests/Pim.UnitTests/bin/Debug/net8.0/runtimes/linux-x64`）
- 结果：`Passed! - Failed: 0, Passed: 41, Skipped: 0, Total: 41`

### 2. dotnet test Pim.sln
```
dotnet test Pim.sln --no-restore
```
- 结果：`Passed! - Failed: 0, Passed: 1668, Skipped: 0, Total: 1668`

### 3. 手动校验（Npgquery）
- `SELECT/**/ *`、`SELECT id,/**/ *`、`SELECT id FROM information_schema.tables`、`SELECT id FROM pg_tables`、`SELECT password_hash FROM users` 均 `SelectStarNotAllowed`/`SystemTableNotAllowed`/`ColumnRestricted`；`SELECT id, username FROM users`、`WITH c AS ...`、`SELECT count(*) FROM users` 通过。

## 修改文件
- 修改：`src/Pim.Api/Middleware/ExceptionMiddleware.cs`
- 修改：`src/Pim.Api/Infrastructure/Ops/SqlAstValidator.cs`
- 修改：`src/Pim.Api/Services/OpsDbService.cs`
- 修改：`src/Pim.Api/Infrastructure/Ops/OpsRateLimiter.cs`
- 修改：`src/Pim.Api/Infrastructure/Ops/OpsRateLimitMiddleware.cs`
- 新建：`src/Pim.Api/Infrastructure/Ops/OpsIpHelper.cs`
- 修改：`src/Pim.Api/Infrastructure/Ops/OpsKeyMiddleware.cs`
- 修改：`src/Pim.Api/Services/OpsLogsService.cs`
- 修改：`src/Pim.Api/Endpoints/OpsLogsEndpoints.cs`
- 修改：`src/Pim.Api/Endpoints/OpsDbEndpoints.cs`
- 修改：`src/Pim.Api/Program.cs`
- 修改：`src/Pim.Api/Pim.Api.csproj`（Npgquery 1.1.0）
- 修改：`docs/ops-readonly-api.md`
- 新建：`.superpowers/sdd/2026-08-21-ops-readonly-api/fix-final-report.md`

## 提交
- 待提交：`fix: ops 终审修复（状态码/AST/CIDR/限流时序/cursor/重复health 等）`
