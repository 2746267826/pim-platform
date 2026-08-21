# PIM 运维只读 API 设计（替代 SSH 日志通道）

- 日期：2026-08-21
- 状态：已批准（头脑风暴分节确认通过）
- 目标：去掉容器 `sshd/pimlog` 通道，改为 **API 级只读能力**（日志 `JSONL` + 全量库只读且禁止敏感列），密钥与用户体系解耦（存环境变量），全部署形态通用，交付独立使用文档。

## 决策摘要

| 问题 | 决策 |
|------|------|
| 总体方案 | 方案 A：独立只读端点集 + 双数据源 + AST 校验 |
| 部署形态 | 全形态通用（Docker / systemd 裸机 / tar.gz 直跑），统一走 `IConfiguration` 环境变量 |
| 鉴权 | `X-PIM-Ops-Key` 比对 `PIM_OPS_KEY`（逗号分隔多值轮换，FixedTimeEquals），可选 `PIM_OPS_ALLOWED_CIDRS` 白名单 |
| 日志范围 | 仅 Serilog JSONL（`/data/pim/logs/pim-api-*.jsonl`），不含 journal/stdout |
| 日志限流 | `limit` 上限 500、`maxBytes 5MB`、`timeout 10s`，超限 `206 Partial` |
| 数据库范围 | 全量表/视图只读（`SELECT/WITH`），`AST` 前置 + 库层 `REVOKE` 禁敏感列 |
| 敏感列 | `REVOKE SELECT (password_hash) ON users` 等，命中 `403 RestrictedColumn`，不做 `***` 脱敏 |
| SSH 处置 | 本期同步物理移除（Dockerfile openssh/supervisor sshd、sshd-pim.conf、pim-log-cat.sh、entrypoint 密钥/host key、EXPOSE 22、2222 映射、pim_ssh_keys 卷） |
| 文档 | 新增 `docs/ops-readonly-api.md` 详细使用指引 |

## 1. 目标与范围

**目标**：以 API 替代 SSH 实现更完善的只读排障能力；密钥独立于用户体系（`PIM_OPS_KEY` 环境变量）；覆盖日志与数据库；全形态通用。

**包含**：`OpsKey` 中间件、`OpsLogsEndpoints`、`OpsDbEndpoints`、`OpsDbConnection`（`pim_ro`）、`libpg_query AST` 校验、`REVOKE` 脚本、使用文档、`docker-compose.prod.yml`/`.env` 更新、SSH 相关移除。

**不包含**：`journal/stdout`、`SELECT` 以外 SQL、写入 API、前端页面、Sidecar、JWT 改造。

**非目标**：替代业务查询接口，仅为运维/排障通道。

## 2. 架构与部署

```
[调用方: opencode/curl] --X-PIM-Ops-Key--> [pim:5000 /api/v1/ops/*]
                                        ├─ OpsKeyMiddleware (pre-auth)
                                        ├─ OpsLogsService -> /data/pim/logs/*.jsonl (FileShare.ReadWrite)
                                        └─ OpsDbService -> Npgsql (PIM_OPS_RO_CONNECTION -> role pim_ro)
[业务流量] --JWT Bearer--> [同容器 /api/v1/*] -> DefaultConnection (pim, 读写)
```

- 运行时：`Dockerfile` 保留 `tini`，`supervisord` 仅留 `dotnet`；移除 `openssh-server`、`EXPOSE 22`、`sshd` 段。
- 配置：`docker-compose.prod.yml` 移除 `PIM_SSH_AUTHORIZED_KEYS`、`2222:22`、`pim_ssh_keys` 卷，新增 `PIM_OPS_KEY`、`PIM_OPS_RO_CONNECTION`、`PIM_OPS_ALLOWED_CIDRS`；`.env.prod.example` 同步。
- 网络：仍 `127.0.0.1:5858->5000`，对外与否由宿主机反代决定，不新增端口。
- 依赖：新增 `libpg_query` 绑定（如 `PgQuery`/`PGLast`）仅用于 AST 校验。

**全形态通用落位**：统一读 `IConfiguration["PIM_OPS_KEY"]` / `["ConnectionStrings:OpsRo"]` 或 `PIM_OPS_RO_CONNECTION`。

| 形态 | 密钥落位 |
|------|----------|
| Docker | `docker-compose --env-file .env.prod` / `-e PIM_OPS_KEY` |
| systemd 裸机 | `/etc/pim/ops.env`（`600`，`PIM_OPS_KEY=...`）+ `EnvironmentFile=-/etc/pim/ops.env` |
| tar.gz 手动 | `export PIM_OPS_KEY` 或 `source .env && ./Pim.Api` |
| 备选 | `appsettings.Production.json:Ops:Key`（不推荐，明文） |

启动时 `PIM_OPS_KEY` 为空则 `ops` 全 `503 OpsDisabled`，各形态行为一致。

## 3. 鉴权与密钥管理

- 变量：`PIM_OPS_KEY`（单一密钥，支持逗号分隔多值轮换，如 `keyA,keyB`），`PIM_OPS_ALLOWED_CIDRS`（可选，如 `10.0.0.0/8,127.0.0.1/32`）。
- 中间件：自定义 `OpsKeyAuthenticationHandler` 仅作用于 `/api/v1/ops/*`，流程：取 `X-PIM-Ops-Key`（大小写不敏感）→ `trim` → 与各密钥 `CryptographicOperations.FixedTimeEquals(UTF8)` 比对；缺失/不匹配 `401 {code:40101}`，`CIDR` 不匹配 `403`，成功则 `ClaimsPrincipal` 标记 `ops-reader`。
- 与 JWT 正交：`ops` 路径跳过 `JwtBearer`，不要求 `Authorization`，不产生 JWT 审计噪音；其他路径不受影响。
- 存储：仅内存，不落盘、不回显、不记日志；`/health` 不泄露是否配置。
- 轮换：发布新密钥到逗号列表 → 客户端全量切换 → 移除旧值，无停机。

## 4. 日志只读 API

**路由**（均需 `X-PIM-Ops-Key`）：

- `GET /api/v1/ops/logs/files` → 列文件（`name/size/mtime/rowsEstimate`，按 `mtime desc`）
- `GET /api/v1/ops/logs/tail?file=pim-api-20260821.jsonl&lines=50&level=Error&keyword=Timeout` → 尾部 N 行，默认 50 上限 500，超出 400
- `GET /api/v1/ops/logs/query?from=2026-08-20T00:00:00Z&to=...&level=Warning&keyword=...&cursor=...&limit=100` → 跨文件时间范围分页，`cursor=base64(file:offset)`，每行返原始 JSONL + 解析字段 `@t/@l/@mt`

**实现**：`File.Open(..., FileShare.ReadWrite)` + `StreamReader` 逆向扫描/正向过滤，不 `ReadAllLines`；`level` 仅匹配 `Serilog @l`，`keyword` 大小写不敏感 `IndexOf`；单次 `limit` 上限 500，`maxBytes 5MB`，`timeout 10s`，超限 `206 Partial` 带 `X-Truncated: bytes`。

**约束**：`PIM_LOG_RETAINED_FILES` 仍控滚动；文件名白名单 `^[a-zA-Z0-9_.-]+\.jsonl$`，仅文件名防穿越。

**错误**：`404 FileNotFound` / `400 InvalidRange` / `503 OpsDisabled`。

## 5. 数据库只读 API（含敏感列禁止）

**路由**：

- `POST /api/v1/ops/db/query` → `JSON { sql, params?, maxRows? }`，仅 `SELECT/WITH`
- `GET /api/v1/ops/db/tables` → 库内 `information_schema` 表/视图清单（含列名/类型）
- `GET /api/v1/ops/db/describe?table=users` → 单表列清单，敏感列标注 `restricted:true`

**执行**：数据源 `PIM_OPS_RO_CONNECTION`（`role pim_ro`），示例 `Host=...;Database=pim;Username=pim_ro;Password=...;Pooling=true;CommandTimeout=10`。

**安全（双层）**：

1. 库层 `REVOKE`（`sql/ops-ro-grants.sql`）：
   ```sql
   CREATE ROLE pim_ro NOLOGIN;
   GRANT CONNECT ON DATABASE pim TO pim_ro;
   GRANT USAGE ON SCHEMA public TO pim_ro;
   GRANT SELECT ON ALL TABLES IN SCHEMA public TO pim_ro;
   ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT SELECT ON TABLES TO pim_ro;
   REVOKE SELECT (password_hash) ON users FROM pim_ro;
   REVOKE SELECT (token_hash) ON refresh_tokens FROM pim_ro;
   -- 按需追加 login_attempts/ip 等敏感列
   ```
   新表通过 `ALTER DEFAULT PRIVILEGES` 自动继承。

2. AST 校验（`libpg_query`）：仅允 `SelectStmt/CommonTableExpr`，拒绝 `INSERT/UPDATE/DELETE/DDL/CALL/COPY/TRUNCATE`，拒绝 `SELECT *`/`tbl.*`（提示显式列名），拒绝 `pg_catalog/information_schema/pg_*` 系统访问，黑名单列 `password_hash/token_hash` 命中 `400 ColumnRestricted`。

**限流截断**：`maxRows` 默认 200 上限 500、`maxBytes 5MB`、`statement_timeout=10s`，超限截断返 `truncated:true` + `rowCount/bytes`；全程只读事务 `SET TRANSACTION READ ONLY`。

**禁止语义**：敏感列被 `REVOKE` 后库层报 `42501 permission denied` → `403 RestrictedColumn`，不做 `***` 替换。

## 6. 限流、审计与错误

- 限流：`ops` 前缀独立 `FixedWindow`（`30 req/min/IP` + `5 MB/min/IP`），超限 `429 Retry-After:60`，与业务 JWT 限流隔离。
- 审计：每次 `ops` 调写入 `audit_logs`（`action=ops.logs.query|ops.db.query`，`metadataJson` 含 `file/sqlHash/rowCount/bytes/truncated/ip`，不记 `X-PIM-Ops-Key` 明文与完整 `params`，仅记 `sql SHA256` 前 8 位），`Serilog Information` 同步落 JSONL；敏感列命中记 `Warning`。
- 错误：统一 `ApiResponse<T>`，码段 `40101 OpsKeyMissing/Invalid`、`40301 IpNotAllowed`、`40302 ColumnRestricted`、`40002 SqlNotAllowed`（`*/DDL` 提示）、`40401 LogFileNotFound`、`50301 OpsDisabled`、`429`、`206 Partial`，不泄露连接串与栈。
- 可观测：`/health` 不暴露 `ops` 是否启用，`GET /api/v1/ops/health`（需密钥）返 `opsEnabled/tablesCount/logFiles`。

## 7. 测试与文档

**测试**：

- 单元：`OpsKeyHandler`（空/错/多密钥/常时比较/CIDR）、`LogService`（`level/keyword/cursor/5MB截断`）、`SqlAstValidator`（`*`/`DELETE`/`pg_catalog`/`password_hash` 均 400）。
- 集成：`WebApplicationFactory` 起真实 `ops` 端点，`FileShare.ReadWrite` 并发验证、`Npgsql` 接 `pim_ro` 验 `REVOKE` 后 `permission denied`、`statement_timeout 10s` 超时。
- 回归：`dotnet test Pim.sln` 无影响，`docker build` 后 `curl /health` 仍 `healthy`，`ops` 未配置密钥时 `503`。

**文档**（`docs/ops-readonly-api.md` + `.env.prod.example` 注释 + `MIGRATION.md`）：

- 鉴权（`X-PIM-Ops-Key`/`PIM_OPS_KEY` 多值轮换/`CIDR`）、日志接口（`files/tail/query` + `curl -H "X-PIM-Ops-Key: $KEY" "http://127.0.0.1:5858/api/v1/ops/logs/query?from=...&limit=100"`）、数据库（`POST /ops/db/query` 显式列名示例、`*` 被拒示例、`tables/describe`）、错误码表、限流（`30/min,5MB/min,10s`）、敏感列禁止清单、`pim_ro` 部署步骤（`psql -f sql/ops-ro-grants.sql`）、SSH 移除说明、非 Docker 的 `/etc/pim/ops.env` + `EnvironmentFile` 指引。

**迁移**：`Dockerfile` 移除 `openssh-server`/`sshd`/`pim-log-cat.sh`，`entrypoint` 移除密钥/host key，`compose` 移除 `2222:22` 与 `PIM_SSH_AUTHORIZED_KEYS/pim_ssh_keys`，提供迁移说明。

## 8. 验收标准

1. 未带/错带 `X-PIM-Ops-Key` 访问 `ops` 均 `401`，命中 `CIDR` 外 `403`，正确密钥可查日志与库。
2. `SELECT *` / `SELECT password_hash` / `DELETE` 均 `400/403` 且不触库；`SELECT id,username FROM users` 成功且不含 `password_hash`；`pim_ro` 直连同结论。
3. 日志 `limit=500/maxBytes=5MB/timeout=10s` 生效，超限 `206`；`500+` 行请求 `400`。
4. 未配置 `PIM_OPS_KEY` 时 `ops` 全 `503`，`/health` 不泄露。
5. 文档按 `.env` + `curl` 可复现，`ops` 调用均落 `audit_logs`。
6. 镜像不再含 `sshd`、`docker ps` 无 `22` 映射，原 `pimlog` 通道不可用。
