# PIM Ops API — 开发者使用指南

> 给 opencode / 脚本 / 自动化工具调用的运维只读接口。所有请求需 `X-PIM-Ops-Key` 鉴权。

---

## 快速开始

```bash
# 设好密钥（与 PIM 部署时配置的 PIM_OPS_KEY 一致）
export OPS_KEY="你的密钥"

# 验证连通
curl -s -H "X-PIM-Ops-Key: $OPS_KEY" http://127.0.0.1:5858/api/v1/ops/health | jq
# => { opsEnabled: true, tablesCount: 42, logFiles: 3 }
```

---

## 鉴权

所有 `/api/v1/ops/*` 接口统一通过 Header 鉴权：

```
X-PIM-Ops-Key: <secret>
```

- 缺失或不匹配 → `401 { code: 40101, message: "OpsKeyMissingOrInvalid" }`
- 服务端未配置 `PIM_OPS_KEY` → `503 { code: 50301, message: "OpsDisabled" }`

---

## 接口一览

| 方法 | 路径 | 用途 |
|------|------|------|
| GET | `/api/v1/ops/health` | 健康检查（需鉴权） |
| GET | `/api/v1/ops/logs/files` | 列出日志文件 |
| GET | `/api/v1/ops/logs/tail` | 读日志末尾 N 行 |
| GET | `/api/v1/ops/logs/query` | 跨文件分页查询日志 |
| GET | `/api/v1/ops/db/tables` | 列出所有表 |
| GET | `/api/v1/ops/db/describe?table=xxx` | 查看单表列结构 |
| POST | `/api/v1/ops/db/query` | 执行只读 SQL |

基础地址：`http://127.0.0.1:5858`（生产环境替换为实际域名和端口）。

---

## 日志接口

### 列出文件

```bash
curl -s -H "X-PIM-Ops-Key: $OPS_KEY" http://127.0.0.1:5858/api/v1/ops/logs/files | jq
```

响应：
```json
{
  "code": 0,
  "data": [
    { "name": "pim-api-20260821.jsonl", "size": 1048576, "mtime": "2026-08-21T10:00:00Z", "rowsEstimate": 5000 }
  ]
}
```

### Tail（读末尾）

```bash
curl -s -H "X-PIM-Ops-Key: $OPS_KEY" \
  "http://127.0.0.1:5858/api/v1/ops/logs/tail?file=pim-api-20260821.jsonl&lines=50" | jq
```

可选参数：

| 参数 | 默认 | 说明 |
|------|------|------|
| `file` | 必填 | 文件名，仅允许 `[a-zA-Z0-9_.-]` + `.jsonl` |
| `lines` | 50 | 1-500 |
| `level` | - | 按 Serilog 级别过滤（`Error`/`Warning`/`Information` 等） |
| `keyword` | - | 关键词匹配（不区分大小写） |

### Query（跨文件分页）

```bash
# 第一页
curl -s -H "X-PIM-Ops-Key: $OPS_KEY" \
  "http://127.0.0.1:5858/api/v1/ops/logs/query?limit=100&level=Error&from=2026-08-20T00:00:00Z&to=2026-08-21T00:00:00Z" | jq

# 翻页（用上一次返回的 nextCursor）
curl -s -H "X-PIM-Ops-Key: $OPS_KEY" \
  "http://127.0.0.1:5858/api/v1/ops/logs/query?limit=100&cursor=<nextCursor的值>" | jq
```

可选参数：

| 参数 | 默认 | 说明 |
|------|------|------|
| `limit` | 50 | 1-500 |
| `file` | - | 指定文件名，不传则跨所有文件 |
| `level` | - | 级别过滤 |
| `keyword` | - | 关键词匹配 |
| `from` | - | 起始时间（ISO8601，过滤日志 `@t` 字段） |
| `to` | - | 截止时间 |
| `cursor` | - | 上次返回的 `nextCursor`（base64 编码的 `file:offset`） |

截断：超过 5MB 或 10 秒时返回 `206 Partial` + `X-Truncated: true`，用 `nextCursor` 续读。

---

## 数据库接口

### 列出表

```bash
curl -s -H "X-PIM-Ops-Key: $OPS_KEY" http://127.0.0.1:5858/api/v1/ops/db/tables | jq
```

### 查看列结构

```bash
curl -s -H "X-PIM-Ops-Key: $OPS_KEY" "http://127.0.0.1:5858/api/v1/ops/db/describe?table=users" | jq
```

### 执行 SQL

```bash
curl -s -H "X-PIM-Ops-Key: $OPS_KEY" -H "Content-Type: application/json" \
  -d '{"sql":"SELECT id, username FROM users LIMIT 5"}' \
  http://127.0.0.1:5858/api/v1/ops/db/query | jq
```

参数化查询（推荐）：

```bash
curl -s -H "X-PIM-Ops-Key: $OPS_KEY" -H "Content-Type: application/json" \
  -d '{"sql":"SELECT id, username FROM users WHERE username = @u LIMIT 5","params":{"u":"alice"}}' \
  http://127.0.0.1:5858/api/v1/ops/db/query | jq
```

**⚠️ SQL 限制：**

- 禁止 `SELECT *`（含 `tbl.*`、`u.*`），需显式列出列名
- 仅允许 `SELECT` 和 `WITH ... SELECT`，禁止 `INSERT`/`UPDATE`/`DELETE`/`DROP` 等
- 禁止多语句（`:` 分隔）
- 系统表（`pg_catalog`/`information_schema`/`pg_*`）禁止查询
- 敏感列（`password_hash` 等）已从数据库层面 REVOKE

返回格式：
```json
{
  "code": 0,
  "data": {
    "rows": [{ "id": 1, "username": "alice" }],
    "truncated": false,
    "nextCursor": null
  }
}
```

---

## 限流

- 单 IP 并发上限 **2**，超限返回 `429 { code: 42901 }` + `Retry-After: 5`
- 单次请求：日志 `limit 500` / 5MB / 10s；DB `maxRows 500`（默认 200）/ 5MB / 10s
- 超限截断不报错，返回 `206 Partial` + `X-Truncated: true`，用 `nextCursor` 续读

---

## 错误码速查

| HTTP | code | 含义 |
|------|------|------|
| 401 | 40101 | 密钥缺失或不匹配 |
| 400 | 40002 | 参数非法（文件名/表名/SQL/星号/敏感列等） |
| 400 | 40003 | `lines`/`limit` 越界（需 1-500） |
| 403 | 40302 | 敏感列被 REVOKE |
| 404 | 40401 | 日志文件或表不存在 |
| 429 | 42901 | 并发限流 |
| 503 | 50301 | 服务端未配置 Ops Key |
| 206 | - | 结果被截断（看 `X-Truncated` header） |

---

## opencode 集成示例

```python
import requests, os

OPS_KEY = os.environ["PIM_OPS_KEY"]
BASE = "http://127.0.0.1:5858"
HEADERS = {"X-PIM-Ops-Key": OPS_KEY}

# 查最近错误日志
resp = requests.get(f"{BASE}/api/v1/ops/logs/tail",
    params={"file": "pim-api-20260821.jsonl", "lines": 20, "level": "Error"},
    headers=HEADERS)

# 查数据库
resp = requests.post(f"{BASE}/api/v1/ops/db/query",
    json={"sql": "SELECT id, username, created_at FROM users ORDER BY created_at DESC LIMIT 10"},
    headers=HEADERS)
```
