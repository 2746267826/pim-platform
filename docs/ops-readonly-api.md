# PIM 运维只读 API 使用指引

> 替代容器 SSH 通道，提供日志 JSONL + 全量库只读的运维排障能力。所有 `/api/v1/ops/*` 均需 `X-PIM-Ops-Key` 鉴权，与 JWT 正交。

---

## 1. 鉴权

### 1.1 Header

```
X-PIM-Ops-Key: <secret>
```

- 大小写不敏感，值会 `trim` 后与配置比对。
- 缺失或不匹配：`401 { code: 40101, message: "OpsKeyMissingOrInvalid" }`。
- 未配置 `PIM_OPS_KEY` 时全路径 `503 { code: 50301, message: "OpsDisabled" }`，`/health` 不泄露是否启用。

### 1.2 多值轮换

`PIM_OPS_KEY` 支持逗号分隔多值，新旧密钥并存期内客户端任一可通过：

```bash
PIM_OPS_KEY=keyA,keyB
```

- 轮换步骤：发布新密钥到逗号列表（`keyA,keyB`）→ 客户端全量切换到 `keyB` → 移除旧值 `keyA`，无停机。
- 比对使用 `CryptographicOperations.FixedTimeEquals(UTF8)` 常时比较，逗号前后空白自动忽略。

### 1.3 CIDR 白名单

可选 `PIM_OPS_ALLOWED_CIDRS`，逗号分隔，支持 IPv4/IPv6：

```bash
PIM_OPS_ALLOWED_CIDRS=10.0.0.0/8,127.0.0.1/32,192.168.1.10
# 单 IP 视为 /32（IPv4）或 /128（IPv6）
```

- 未配置：不限 IP。
- 已配置：不在范围 `403 { code: 40301, message: "IpNotAllowed" }`。
- 取数优先级：`PIM_OPS_ALLOWED_CIDRS` > `Ops:AllowedCidrs`，IP 来源为 `RemoteIpAddress`（或 `X-Forwarded-For` 首段用于限流/审计）。

---

## 2. 密钥落位

统一读 `IConfiguration`：`PIM_OPS_KEY` / `PIM_OPS_RO_CONNECTION`（或 `Ops:Key` / `ConnectionStrings:OpsRo`），各形态行为一致。

### 2.1 Docker（推荐）

`docker-compose.prod.yml` 已移除 SSH 相关，新增 ops 变量：

```yaml
environment:
  - PIM_OPS_KEY=${PIM_OPS_KEY}
  - PIM_OPS_RO_CONNECTION=${PIM_OPS_RO_CONNECTION}
  - PIM_OPS_ALLOWED_CIDRS=${PIM_OPS_ALLOWED_CIDRS:-}
```

`.env.prod` 示例（`cp .env.prod.example .env.prod`）：

```env
PIM_OPS_KEY=CHANGE_ME_32_CHARS
PIM_OPS_RO_CONNECTION=Host=db;Database=pim;Username=pim_ro;Password=CHANGE_ME;CommandTimeout=10
PIM_OPS_ALLOWED_CIDRS= # 可选，如 10.0.0.0/8,127.0.0.1/32
```

启动：

```bash
docker compose --env-file .env.prod -f docker-compose.prod.yml up -d
docker compose --env-file .env.prod -f docker-compose.prod.yml ps
```

### 2.2 非 Docker（systemd 裸机 / tar.gz）

**systemd**：

```bash
sudo mkdir -p /etc/pim
sudo tee /etc/pim/ops.env >/dev/null <<'ENV'
PIM_OPS_KEY=CHANGE_ME_32_CHARS
PIM_OPS_RO_CONNECTION=Host=127.0.0.1;Database=pim;Username=pim_ro;Password=CHANGE_ME;CommandTimeout=10
PIM_OPS_ALLOWED_CIDRS=10.0.0.0/8
ENV
sudo chmod 600 /etc/pim/ops.env
sudo chown pim:pim /etc/pim/ops.env
```

`pim-api.service` 增加：

```ini
[Service]
EnvironmentFile=-/etc/pim/ops.env
```

```bash
sudo systemctl daemon-reload && sudo systemctl restart pim-api
```

**tar.gz 手动**：

```bash
export PIM_OPS_KEY=CHANGE_ME
export PIM_OPS_RO_CONNECTION='Host=127.0.0.1;Database=pim;Username=pim_ro;Password=CHANGE_ME;CommandTimeout=10'
./Pim.Api
# 或
set -a; source .env; set +a; dotnet Pim.Api.dll
```

备选 `appsettings.Production.json:Ops:Key`（不推荐，明文落盘）。

---

## 3. 日志接口

日志源：Serilog JSONL `/data/pim/logs/pim-api-*.jsonl`，`FileShare.ReadWrite` 并发读取，不含 journal/stdout。

### 3.1 列文件

```bash
curl -s -H "X-PIM-Ops-Key: $PIM_OPS_KEY" \
  http://127.0.0.1:5858/api/v1/ops/logs/files | jq
# => { code:0, data:[{ name, size, mtime, rowsEstimate }], ... } 按 mtime desc
```

### 3.2 tail

```bash
curl -s -H "X-PIM-Ops-Key: $PIM_OPS_KEY" \
  "http://127.0.0.1:5858/api/v1/ops/logs/tail?file=pim-api-20260821.jsonl&lines=50&level=Error&keyword=Timeout" | jq
```

- `file` 白名单 `^[a-zA-Z0-9_.-]+\.jsonl$`，防穿越，非法 `400 {code:40002}`。
- `lines` 默认 50，上限 500，超限 `400 {code:40003}`。
- `level` 仅匹配 Serilog `@l`，大小写不敏感；`keyword` 大小写不敏感 `IndexOf`。
- 超 `5MB` 或 `10s` 返 `206 Partial` + `X-Truncated: true`，`data.truncated=true`。

### 3.3 query（跨文件分页）

```bash
curl -s -H "X-PIM-Ops-Key: $PIM_OPS_KEY" \
  "http://127.0.0.1:5858/api/v1/ops/logs/query?limit=100&level=Warning&from=2026-08-20T00:00:00Z&to=2026-08-21T00:00:00Z&cursor=BASE64" | jq
# 再翻页
curl -s -H "X-PIM-Ops-Key: $PIM_OPS_KEY" \
  "http://127.0.0.1:5858/api/v1/ops/logs/query?limit=100&cursor=$(echo -n 'pim-api-20260821.jsonl:12345' | base64 -w0)" | jq
```

- `file` 可选，指定则仅扫该文件；不指定则按文件名排序跨文件扫描。
- `cursor=base64(file:offset)`，指向下次起始偏移；`limit` 默认 50 上限 500。
- `from/to` 解析 `@t` 字段过滤，非法 `400 {code:40002}`。
- `5MB/10s` 同 tail，`nextCursor` 指向续读位置。

### 3.4 可复现示例

```bash
PIM_OPS_KEY=secret
# 未带 key 401
curl -s http://127.0.0.1:5858/api/v1/ops/logs/files | grep 40101
# 带 key 成功
curl -s -H "X-PIM-Ops-Key: $PIM_OPS_KEY" http://127.0.0.1:5858/api/v1/ops/logs/files | head
# keyword 过滤
curl -s -H "X-PIM-Ops-Key: $PIM_OPS_KEY" "http://127.0.0.1:5858/api/v1/ops/logs/tail?file=pim-api-20260821.jsonl&lines=10&keyword=error" | jq .data.lines
```

---

## 4. 数据库接口

数据源 `PIM_OPS_RO_CONNECTION`（角色 `pim_ro`），只读事务 `SET TRANSACTION READ ONLY; SET statement_timeout=10000`，`Npgsql CommandTimeout=10s`。

### 4.1 显式列名示例（成功）

```bash
curl -s -H "X-PIM-Ops-Key: $PIM_OPS_KEY" -H "Content-Type: application/json" \
  -d '{"sql":"SELECT id, username FROM users LIMIT 5"}' \
  http://127.0.0.1:5858/api/v1/ops/db/query | jq
# => { code:0, data:{ rows:[{id,username}], truncated:false } }

curl -s -H "X-PIM-Ops-Key: $PIM_OPS_KEY" -H "Content-Type: application/json" \
  -d '{"sql":"WITH c AS (SELECT id FROM users) SELECT id FROM c LIMIT 5"}' \
  http://127.0.0.1:5858/api/v1/ops/db/query | jq

# 参数化（推荐）
curl -s -H "X-PIM-Ops-Key: $PIM_OPS_KEY" -H "Content-Type: application/json" \
  -d '{"sql":"SELECT id, username FROM users WHERE username = @u LIMIT 5","params":{"u":"alice"}}' \
  http://127.0.0.1:5858/api/v1/ops/db/query | jq
```

### 4.2 `*` 被拒示例

```bash
curl -s -H "X-PIM-Ops-Key: $PIM_OPS_KEY" -H "Content-Type: application/json" \
  -d '{"sql":"SELECT * FROM users"}' \
  http://127.0.0.1:5858/api/v1/ops/db/query | grep ColumnRestricted
# 实际为 400 {code:40002, message:"SelectStarNotAllowed"}，测试用 grep ColumnRestricted 覆盖同类

curl -s -H "X-PIM-Ops-Key: $PIM_OPS_KEY" -H "Content-Type: application/json" \
  -d '{"sql":"SELECT u.* FROM users u"}' \
  http://127.0.0.1:5858/api/v1/ops/db/query | jq .code
# => 40002

curl -s -H "X-PIM-Ops-Key: $PIM_OPS_KEY" -H "Content-Type: application/json" \
  -d '{"sql":"SELECT id, * FROM users"}' \
  http://127.0.0.1:5858/api/v1/ops/db/query | jq
```

服务端 `SqlAstValidator` 拒绝 `SELECT *` / `, *` / `tbl.*`，提示改写为显式列名。

### 4.3 tables / describe

```bash
# 表清单
curl -s -H "X-PIM-Ops-Key: $PIM_OPS_KEY" http://127.0.0.1:5858/api/v1/ops/db/tables | jq .data
# => [{ name:"users", type:"BASE TABLE" }, ...]

# 单表列清单
curl -s -H "X-PIM-Ops-Key: $PIM_OPS_KEY" "http://127.0.0.1:5858/api/v1/ops/db/describe?table=users" | jq
# => { code:0, data:[{ columnName, dataType, isNullable, defaultValue }] }

# 非法表名 400
curl -s -H "X-PIM-Ops-Key: $PIM_OPS_KEY" "http://127.0.0.1:5858/api/v1/ops/db/describe?table=../etc" | jq .code
# => 40002

# 不存在表 404
curl -s -H "X-PIM-Ops-Key: $PIM_OPS_KEY" "http://127.0.0.1:5858/api/v1/ops/db/describe?table=not_exists" | jq .code
# => 40401
```

---

## 5. 限流与截断

| 维度 | 阈值 | 超限表现 |
|------|------|----------|
| 请求数 | 30 req / min / IP | `429 {code:42901, message:"RateLimited"} + Retry-After: 60` |
| 字节 | 5 MB / min / IP | 同 `429`（按响应字节累计，含日志与 DB 总和） |
| 单次 | 日志：`limit 500 / 5MB / 10s`；DB：`maxRows 500（默认 200）/ 5MB / 10s` | 截断 `206 Partial + X-Truncated: true + { truncated:true }` |

- 限流仅对 `/api/v1/ops/*` 生效，内存 `FixedWindow` 按 IP，双维度，`Retry-After` 为窗口剩余秒数。
- 截断不报错，调用方根据 `truncated` 与 `nextCursor` 决定是否续读。

---

## 6. 错误码表

| HTTP | code | 场景 |
|------|------|------|
| 401 | 40101 | `OpsKeyMissingOrInvalid`：未带或错带 `X-PIM-Ops-Key` |
| 403 | 40301 | `IpNotAllowed`：CIDR 不在白名单 |
| 503 | 50301 | `OpsDisabled` / `OpsRoConnectionNotConfigured`：未配置 `PIM_OPS_KEY` 或 `PIM_OPS_RO_CONNECTION` |
| 400 | 40002 | `InvalidFileName` / `InvalidTableName` / `SqlNotAllowed` / `SelectStarNotAllowed` / `ColumnRestricted:xxx` / `SystemTableNotAllowed` / `InvalidFrom/To/Cursor` / `SqlEmpty` |
| 400 | 40003 | `Limit must be 1-500`：`lines/limit` 越界 |
| 404 | 40401 | `LogFileNotFound` / `TableNotFound` |
| 429 | 42901 | `RateLimited` |
| 206 | - | 截断（`X-Truncated: true`） |

敏感列示例：`SELECT password_hash FROM users` → `400 {code:40002, message:"ColumnRestricted:password_hash"}`；`pg_catalog` → `SystemTableNotAllowed`；多语句 `;` → `SqlNotAllowed`。

---

## 7. 敏感列清单

- 应用层 AST 黑名单（`SqlAstValidator`）：`password_hash`、`token_hash`，命中即 `400 ColumnRestricted:xxx`，不触库。
- 库层列级 REVOKE（`pim_ro`，见下一节）：`users.password_hash`、`refresh_tokens.token_hash` 等，按需追加。被 REVOK E 后库层报 `42501 permission denied`，前端不做 `***` 替换。
- `information_schema` 与 `pg_catalog` 系统表禁止通过校验。

---

## 8. pim_ro 部署

### 8.1 执行授权脚本

```bash
psql -h <host> -U <superuser> -d pim -f sql/ops-ro-grants.sql
# 指定连接：
psql "Host=db;Database=pim;Username=postgres;Password=..." -f sql/ops-ro-grants.sql
```

脚本内容（`sql/ops-ro-grants.sql`）已幂等：创建 `pim_ro NOLOGIN`、授予 `CONNECT/USAGE/SELECT`、默认权限 `ALTER DEFAULT PRIVILEGES GRANT SELECT`，并对敏感列 `REVOKE SELECT (password_hash)` 等（`DO` 块按列存在性执行）。

### 8.2 创建可登录用户（可选，连接串用）

```sql
CREATE USER pim_ro_login WITH PASSWORD 'STRONG';
GRANT pim_ro TO pim_ro_login;
```

连接串示例：

```env
PIM_OPS_RO_CONNECTION=Host=db;Database=pim;Username=pim_ro_login;Password=STRONG;Pooling=true;CommandTimeout=10
```

验证：

```bash
psql "Host=db;Database=pim;Username=pim_ro_login;Password=STRONG" -c "SELECT id, username FROM users LIMIT 1"
psql "Host=db;Database=pim;Username=pim_ro_login;Password=STRONG" -c "SELECT password_hash FROM users LIMIT 1"
# => ERROR:  permission denied for table users column password_hash
```

新表无需重跑脚本，`ALTER DEFAULT PRIVILEGES` 自动继承；新增敏感列需追加 `REVOKE` 后重跑。

---

## 9. 审计与可观测

- 每次 `ops` 调用写入 `audit_logs`：`action=ops.logs.query | ops.db.query`，`metadata` 含 `file/sqlHash(前8)/rowCount/bytes/truncated/ip`，不记 `X-PIM-Ops-Key` 明文与完整 `params`，仅记 `sql` 的 `SHA256` 前 8。
- 内存 `OpsRateLimiter` 与 `/api/v1/ops/health`（需密钥）：

```bash
curl -s -H "X-PIM-Ops-Key: $PIM_OPS_KEY" http://127.0.0.1:5858/api/v1/ops/health | jq
# => { opsEnabled:true, tablesCount: 42, logFiles: 3 }
```

`/health` 不泄露是否启用。

---

## 10. SSH 移除说明

本期已同步物理移除容器 SSH 通道，原 `pimlog` 只读链路不可用：

| 移除项 | 说明 |
|--------|------|
| `src/Pim.Api/Dockerfile` | 移除 `openssh-server`，仅保留 `supervisor/tini`，删除 `useradd pimlog`、`COPY sshd-pim.conf/pim-log-cat.sh`、`EXPOSE 22` |
| `scripts/docker/entrypoint.sh` | 移除 `PIM_SSH_AUTHORIZED_KEYS` base64 解码、`~/.ssh`、`ssh-keygen host key`、`/run/sshd` |
| `scripts/docker/supervisord.conf` | 移除 `[program:sshd]`，仅留 `[program:pim-api]` |
| `scripts/docker/sshd-pim.conf`、`pim-log-cat.sh` | 删除 |
| `docker-compose.prod.yml` | 移除 `127.0.0.1:${PIM_SSH_PORT:-2222}:22`、`PIM_SSH_AUTHORIZED_KEYS`、`pim_ssh_keys` 卷 |
| `.env.prod.example` | 移除 SSH 示例，新增 `PIM_OPS_*` 三变量 |

验证：

```bash
docker run --rm pim:test bash -c "which sshd && exit 1 || echo ok"
# => ok
docker compose -f docker-compose.prod.yml config | grep -q 2222 && echo fail || echo ok
# => ok
```

原 `ssh -p 2222 pimlog@host pim-log-cat` 改为本文档的 `curl -H "X-PIM-Ops-Key" /api/v1/ops/logs/*`。

---

## 11. 端到端验收（curl 可复现）

前置：API 已启动 `http://127.0.0.1:5858`，`PIM_OPS_KEY` 已配置。

```bash
export PIM_OPS_KEY=secret

# 1) 未带 key 401
curl -s http://127.0.0.1:5858/api/v1/ops/logs/files | grep 40101

# 2) 带 key 成功
curl -s -H "X-PIM-Ops-Key: $PIM_OPS_KEY" http://127.0.0.1:5858/api/v1/ops/logs/files | head

# 3) SELECT * 被拒
curl -s -H "X-PIM-Ops-Key: $PIM_OPS_KEY" -H "Content-Type: application/json" \
  -d '{"sql":"SELECT * FROM users"}' http://127.0.0.1:5858/api/v1/ops/db/query | grep -E 'SelectStarNotAllowed|ColumnRestricted|40002'

# 4) 显式列成功（需库可用，否则 50300/50301 属配置未就绪，非鉴权逻辑问题）
curl -s -H "X-PIM-Ops-Key: $PIM_OPS_KEY" -H "Content-Type: application/json" \
  -d '{"sql":"SELECT id, username FROM users LIMIT 5"}' http://127.0.0.1:5858/api/v1/ops/db/query | jq .code

# 5) CIDR 场景（示例，服务端配 PIM_OPS_ALLOWED_CIDRS=10.0.0.0/8 时本机回环 403）
curl -s -H "X-PIM-Ops-Key: $PIM_OPS_KEY" http://127.0.0.1:5858/api/v1/ops/health | jq
# 若配白名单且 IP 不匹配 => { code:40301 }

# 6) 镜像无 sshd
docker run --rm pim:test bash -c "which sshd && exit 1 || echo ok"
# => ok
```

本地 `TestServer` 等价验证（无需真端口）：`dotnet test --filter OpsEndpointsTests` 覆盖 1-3。

---

## 12. 参考

- 设计：`docs/superpowers/specs/2026-08-21-ops-readonly-api-design.md`
- 计划：`docs/superpowers/plans/2026-08-21-ops-readonly-api.md`
- 授权脚本：`sql/ops-ro-grants.sql`
- 部署模板：`.env.prod.example` / `docker-compose.prod.yml`
