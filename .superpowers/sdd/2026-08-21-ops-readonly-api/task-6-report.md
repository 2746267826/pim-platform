# 任务6报告：文档与验收

## 实现内容
- 新建 docs/ops-readonly-api.md（386 行，12 节）：鉴权 X-PIM-Ops-Key/多值逗号轮换/CIDR、Docker 与非 Docker 落位（docker-compose --env-file + /etc/pim/ops.env 600 + EnvironmentFile）、日志 files/tail/query curl 示例（含 level/keyword/cursor/from/to）、数据库显式列名成功与 SELECT * 被拒示例、tables/describe 用法、错误码表（40101/40301/50301/40002/40003/40401/42901/206）、限流 30/min+5MB/min 双维度 + 单次 500/5MB/10s 截断 206、敏感列 password_hash/token_hash 双层禁止、pim_ro 部署 psql -f sql/ops-ro-grants.sql 含用户创建与验证、审计/health、SSH 移除对照表、端到端 6 条 curl 可复现
- README.md 无 ops 章节，跳过更新

## 验证
- 端到端（TestServer 等价于 curl 6 条）：无 key 40101/带 key 200/SELECT * 40002 SelectStarNotAllowed/显式列逻辑通过/CIDR 40301/镜像无 sshd 均通过；聚焦 dotnet test --filter Ops => 41 passed
- 回归：dotnet test Pim.sln --no-restore => 1668 passed, 0 failed；npm --prefix src/client-web run build => 失败（环境 rolldown binding 缺失 + node 20.18 < 20.19，非本任务代码问题，已重装仍复现）
- 部署形态验证：grep Dockerfile 无 openssh/EXPOSE22，supervisord 无 sshd，compose 无 2222，sshd-* 已删除

## 修改文件
- 新建：docs/ops-readonly-api.md

## 提交
- Head: 861c07456088ce00de4cdb007e3acb956ccc9120

---

## 终审修复（2026-08-21，Critical/Important/Minor 全量）

### 修复项
- `ExceptionMiddleware.cs` 403/401/429/503 映射补全（`switch`：40101=>401,40301/40302=>403,42901=>429,50301=>503）
- `SqlAstValidator.cs` 接入 `Npgquery 1.1.0`：`Parse` 后仅允 `SelectStmt`，`A_Star` 拒、`ColumnRef` 黑名单拒、`RangeVar` `pg_catalog/information_schema/pg_%` 拒；正则补 `information_schema` 与 `pg_\w+`、点星注释容忍 `\.\\s*(/\\*.*?\\*/\\s*)*\\*`
- `OpsDbService.cs` 拆 `SET TRANSACTION READ ONLY` 与 `SET statement_timeout=10000` 为两条 `NpgsqlCommand`；`catch PostgresException 42501 => DomainException 40302`
- `Program.cs` 限流前置：`OpsRateLimitMiddleware` -> `OpsKeyMiddleware`，并加 `UseForwardedHeaders`；`OpsIpHelper` 统一 `X-Forwarded-For` 首段优先，回退 `RemoteIpAddress`
- `OpsLogsService.cs` cursor 用原始字节缓冲读取（8192 块，`\r\n` 处理），`ReadTailAsync` 改 `Queue` 定长避免全量 `rawLines`
- `OpsLogsEndpoints.cs` 删除 `/logs/health` 重复
- `OpsRateLimiter.cs` `AddBytes` 窗口过期 `Count=1`
- `docs/ops-readonly-api.md` 同步 CIDR 口径、错误码 40302、只读事务分两条等

### 验证
- `dotnet test tests/Pim.UnitTests/Pim.UnitTests.csproj --filter FullyQualifiedName~Ops --no-restore` => Passed 41/41
- `dotnet test Pim.sln --no-restore` => Passed 1668/1668

### 提交
- 待提交 Head：见本轮 `git log`

详见 `fix-final-report.md`
