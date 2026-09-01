# MCP Server 使用文档 / MCP Server Guide

> PIM MCP v2 — 101 只读工具，0 写入，给自家可信 AI Agent 专用。单次对话可拿全天/周视图。
> PIM MCP v2 — 101 read-only tools, zero writes, for trusted internal AI Agent. One round-trip for daily/weekly view.

## 目录 / Table of Contents
- [1. 概览 / Overview](#1-概览--overview)
- [2. 快速开始 / Quick Start](#2-快速开始--quick-start)
- [3. 认证 / Authentication](#3-认证--authentication)
- [4. 通用约定 / Conventions](#4-通用约定--conventions)
- [5. 工具全表 / Tools](#5-工具全表--tools)
  - [5.1 Calendar 31](#51-calendar-31)
  - [5.2 PcTracker 27](#52-pctracker-27)
  - [5.3 Mobile 18](#53-mobile-18)
  - [5.4 QuickNotes 3](#54-quicknotes-3)
  - [5.5 Files 8](#55-files-8)
  - [5.6 Core/Infra 14](#56-coreinfra-14)
- [6. 典型调用流 / Recipes](#6-典型调用流--recipes)
- [7. 脱敏与隐私 / Redaction](#7-脱敏与隐私--redaction)
- [8. 限流与分页 / Pagination & Limits](#8-限流与分页--pagination--limits)
- [9. 常见问题 / FAQ](#9-常见问题--faq)
- [10. 变更日志 / Changelog](#10-变更日志--changelog)

## 1. 概览 / Overview

### 是什么 / What it is
- Python `FastMCP` 服务，`stdio` 为主（支持 `Streamable HTTP`），地址 `scripts/mcp/pim_mcp_server.py`。
- 仅只读：101 工具 = `Calendar 31 + PcTracker 27 + Mobile 18 + QuickNotes 3 + Files 8 + Core/Infra 14`。写入 0。
- Python `FastMCP` stdio service at `scripts/mcp/pim_mcp_server.py`. 101 read-only tools, 0 writes.

### 能做什么 / What it can do
- L1 原子查询：一次查一条记录（event/task/note/file）。
- L2 聚合：一次拿周报（focus-blocks / productivity-range / category-distribution / mobile overview）。
- 覆盖 AI 常用场景：今天有啥事 / 这周效率 / 人在哪在干啥 / 找文件笔记。

### 不能做什么 / What it cannot do
- 任何写入：`create/update/delete/sync/import/batch-execute/request-confirmation/writeback` 110+ 路由全部不开放。
- 文件二进制下载 `items/{id}/download` 等流式下载不开放，只给元数据与搜索。
- `/ops/*` 运维接口不开放。第三方限流/OAuth 不做（自家可信）。

### 架构 / Architecture
```
AI Agent --(MCP stdio/HTTP + Bearer)--> pim_mcp_server.py --(HTTP + Bearer透传)--> Pim.Api (http://127.0.0.1:5858) --> PostgreSQL/MinIO
```
- 认证透传：MCP 透传 `Authorization: Bearer <PIM JWT>` 到 `Pim.Api`，审计记真实 `userId`。
- 脱敏：MCP 侧后处理，仅脱敏 `url`，`title` 保留。

## 2. 快速开始 / Quick Start

### 依赖 / Dependencies
```bash
python3 --version  # >=3.10
pip install mcp httpx
# or with uv (宿主 PEP668)
uv pip install --python /usr/bin/python3 --break-system-packages mcp httpx
```

### 环境变量 / Environment
| 变量 | 默认 | 说明 |
|---|---|---|
| `PIM_API_URL` | `http://127.0.0.1:5858` | Pim.Api 地址 |
| `PIM_ACCESS_TOKEN` | - | stdio 模式下的 Bearer（选一：`PIM_ACCESS_TOKEN`/`PIM_TOKEN`/`MCP_BEARER_TOKEN`）|
| `PIM_TOKEN_FILE` | `<script>/.token` | stdio 刷新：token 文件路径（支持 plain JWT 或 JSON `{accessToken, refreshToken}`），按 mtime 热重载 |
| `PIM_REFRESH_TOKEN` | - | stdio 刷新：refreshToken，401 时自动 `POST /api/v1/auth/refresh` 并回写文件 |
| `PIM_TOKEN_PATH` | - | `PIM_TOKEN_FILE` 别名 |

### 启动 / Run
```bash
# stdio (Claude / Codex / OpenCode)
PIM_API_URL=http://127.0.0.1:5858 PIM_ACCESS_TOKEN=<jwt> python scripts/mcp/pim_mcp_server.py
# HTTP (Streamable)
# FastMCP 默认 stdio；如需 HTTP，包装 mcp.run(transport='streamable-http', port=8080)
```

### 客户端配置 / Client configs
**`mcp.json` (通用)**
```json
{
  "mcpServers": {
    "pim": {
      "command": "python",
      "args": [
        "scripts/mcp/pim_mcp_server.py"
      ],
      "env": {
        "PIM_API_URL": "http://127.0.0.1:5858",
        "PIM_ACCESS_TOKEN": "<jwt>"
      }
    }
  }
}
```

**`claude_desktop_config.json`**
```json
{
  "mcpServers": {
    "pim": {
      "command": "python",
      "args": [
        "/absolute/path/scripts/mcp/pim_mcp_server.py"
      ],
      "env": {
        "PIM_API_URL": "http://127.0.0.1:5858",
        "PIM_ACCESS_TOKEN": "<jwt>"
      }
    }
  }
}
```

**HTTP (Bearer 透传)** — Agent 先登录拿 token，调 MCP 时在 HTTP 头带 `Authorization: Bearer <jwt>`，MCP 透传到 Pim.Api。

## 3. 认证 / Authentication

### 登录拿 token / Login
```http
POST /api/v1/auth/login
Content-Type: application/json

{"username":"alice","password":"***"}
```
返回 ` {code:0, data:{accessToken, refreshToken}}`。`accessToken` 即 JWT。

### MCP 透传原理 / Pass-through
- MCP 从调用方上下文取 `Authorization: Bearer <token>`（HTTP 头）或环境变量 `PIM_ACCESS_TOKEN`（stdio）。
- `_api()` 透传到 `Pim.Api`：`headers={Authorization: Bearer <token>}`，不再使用 `PIM_USERNAME/PASSWORD` 固定登录。
- 未带 Bearer：工具返回 `{"error":"missing bearer token...", "code":401}`，Pim.Api 侧审计不到（401 前）。
- 带 Bearer：审计记真实 `userId`（查 `audit_versions` 或日志）。
- stdio 长驻进程：支持运行时刷新（issue #174）— `PIM_TOKEN_FILE`（或脚本旁 `.token`）按 `mtime` 重读，JWT `exp` 提前 60s 判定过期；若配置 `PIM_REFRESH_TOKEN`，401 时自动 `POST /api/v1/auth/refresh` 并回写文件/env 后重试一次。

### 401 处理 / Handling 401
```json
{
  "error": "HTTP 401: Unauthorized",
  "details": {
    "code": 401,
    "message": "Token expired or invalid"
  },
  "code": 401
}
```
- 无 `PIM_REFRESH_TOKEN`：Agent 收到 401 应重新 `POST /auth/login` 刷新后重试（或外部 cron 每 10 分钟刷新 `PIM_TOKEN_FILE`）。
- 有 `PIM_REFRESH_TOKEN`：MCP 自动刷新并重试一次，失败才返回 401；成功后新 `accessToken` 已持久化到 `PIM_TOKEN_FILE` 与进程 env。

## 4. 通用约定 / Conventions

| 约定 | 规则 | 示例 |
|---|---|---|
| `start`/`end` | ISO8601 UTC 闭区间，`start<=end`，最大跨度 366 天，超限 400 | `2026-08-24T16:00:00Z` / `2026-08-31T16:00:00Z` |
| `timezone` | IANA，默认 `Asia/Shanghai`，聚合内部按此切天 | `Asia/Shanghai`, `UTC` |
| `date` | `YYYY-MM-DD`，等价 `start=dateT00:00:00Z` | `2026-08-31` |
| `page`/`pageSize` | `page>=1`，`1<=pageSize<=100` 默认 20 | `page=1&pageSize=20` |
| `redactUrls` | `true` 默认脱敏 12 位 `sha256(url)[0:12]` → `urlHash`，`false` 返回原文 | 见 §7 |
| 成功 | `{"code":0,"data":... ,"page":1,"pageSize":20,"total":123}` 透传 `ApiResponse` |  |
| 失败 | `{"error":"HTTP 400: ...", "details":..., "code":400}` |  |
| 超大 | `{"truncated":true,"nextPage":2,"_note":"response >50KB"}` |  |

错误码速查：`400` 参数/时间/分页越界；`401` 缺/过期 token；`404` 资源不存在；`500/504` 服务/超时。

## 5. 工具全表 / Tools

**总览 101**：`Calendar 31 | PcTracker 27 | Mobile 18 | QuickNotes 3 | Files 8 | Core 14`。

| 模块 | 工具数 | 常用 20 选 |
|---|---|---|
| Calendar | 31 | `get_events`, `get_tasks`, `get_calendar_layers`, `search_calendar_events`, `get_calendars` |
| PcTracker | 27 | `get_pc_timeline_v2`, `get_pc_productivity_range`, `get_pc_focus_blocks`, `get_pc_category_distribution`, `get_pc_quality` |
| Mobile | 18 | `get_mobile_timeline`, `get_mobile_location_latest`, `get_mobile_analytics_overview`, `get_mobile_quality` |
| QuickNotes | 3 | `get_quick_notes`, `get_quick_note` |
| Files | 8 | `search_files`, `get_files`, `get_file` |
| Core | 14 | `get_today_sections`, `search_pim`, `get_version` |

> 提示：工具数多易胀上下文，日常对话按场景选 20 常用即可（见 §6）。

### 5.1 Calendar 31

#### `get_calendar_layers` — Overlay of events/tasks/habits in range. Default layers=all.
- **API**: `GET /calendar/layers?start&end&layers`
- **参数**: `start,end,layers?=all,timezone,redactUrls`
- **返回**: `CalendarLayerResponse`

**签名 / Signature**
```python
async def get_calendar_layers(start: str, end: str, timezone: str = 'Asia/Shanghai', redactUrls: bool = True) -> Any: ...
```

**返回示例 / Success**
```json
{
  "code": 0,
  "data": {
    "start": "2026-08-31T00:00:00Z",
    "end": "2026-08-31T16:00:00Z",
    "items": [
      {
        "id": "1",
        "layer": "event",
        "title": "Standup",
        "startsAt": "2026-08-31T01:00:00Z",
        "endsAt": "2026-08-31T01:30:00Z"
      }
    ]
  }
}
```

**错误示例 / Error**
```json
{
  "error": "HTTP 400: time range too large: max span 366 days",
  "details": {
    "code": 400
  },
  "code": 400
}
```

#### `query_data_center` — Universal query (read semantic POST).
- **API**: `POST /calendar/data-center/query`
- **参数**: `search?,objectType?,source?,pendingOnly,page/pageSize,timezone`
- **返回**: `DataCenterQueryResponse`

**签名 / Signature**
```python
async def query_data_center(search: Optional[str] = None, objectType: Optional[str] = None, source: Optional[str] = None, pendingOnly: bool = False, page: int = 1, pageSize: int = 20, timezone: str = 'Asia/Shanghai') -> Any: ...
```

**返回示例 / Success**
```json
{
  "code": 0,
  "data": "<DataCenterQueryResponse example - see DTO>",
  "page": 1,
  "pageSize": 20,
  "total": 123
}
```

**错误示例 / Error**
```json
{
  "error": "HTTP 400: pageSize must be between 1 and 100",
  "details": {
    "code": 400,
    "message": "pageSize must be between 1 and 100"
  },
  "code": 400
}
```

#### `preview_data_center_batch` — Preview only, no execution.
- **API**: `POST /calendar/data-center/batch/preview`
- **参数**: `action,objects:DataCenterObjectRef[],reason?`
- **返回**: `BatchPreviewResponse`

**签名 / Signature**
```python
async def preview_data_center_batch(action,objects:DataCenterObjectRef[],reason?) -> Any: ...
```

**返回示例 / Success**
```json
{
  "code": 0,
  "data": "<BatchPreviewResponse example - see DTO>"
}
```

**错误示例 / Error**
```json
{
  "error": "missing bearer token: call MCP with Authorization: Bearer <PIM JWT>",
  "code": 401
}
```

#### `get_data_center_audit_export` — Export audit log.
- **API**: `GET /calendar/data-center/audit/export?start&end`
- **参数**: `start,end,timezone`
- **返回**: `AuditExport`

**签名 / Signature**
```python
async def get_data_center_audit_export(start: str, end: str, timezone: str = 'Asia/Shanghai') -> Any: ...
```

**返回示例 / Success**
```json
{
  "code": 0,
  "data": "<AuditExport example - see DTO>"
}
```

**错误示例 / Error**
```json
{
  "error": "HTTP 400: time range too large: max span 366 days",
  "details": {
    "code": 400
  },
  "code": 400
}
```

#### `preview_data_center_restore` — Preview restore only.
- **API**: `POST /calendar/data-center/restore/preview`
- **参数**: `auditVersionId,reason?`
- **返回**: `RestorePreview`

**签名 / Signature**
```python
async def preview_data_center_restore(auditVersionId,reason?) -> Any: ...
```

**返回示例 / Success**
```json
{
  "code": 0,
  "data": "<RestorePreview example - see DTO>"
}
```

**错误示例 / Error**
```json
{
  "error": "missing bearer token: call MCP with Authorization: Bearer <PIM JWT>",
  "code": 401
}
```

#### `get_projects` — List projects.
- **API**: `GET /calendar/projects`
- **参数**: `page/pageSize`
- **返回**: `DomainProject[]`

**签名 / Signature**
```python
async def get_projects(page: int = 1, pageSize: int = 20) -> Any: ...
```

**返回示例 / Success**
```json
{
  "code": 0,
  "data": "<DomainProject[] example - see DTO>",
  "page": 1,
  "pageSize": 20,
  "total": 123
}
```

**错误示例 / Error**
```json
{
  "error": "HTTP 400: pageSize must be between 1 and 100",
  "details": {
    "code": 400,
    "message": "pageSize must be between 1 and 100"
  },
  "code": 400
}
```

#### `get_task_books` — List task books.
- **API**: `GET /calendar/task-books`
- **参数**: `page/pageSize`
- **返回**: `TaskBook[]`

**签名 / Signature**
```python
async def get_task_books(page: int = 1, pageSize: int = 20) -> Any: ...
```

**返回示例 / Success**
```json
{
  "code": 0,
  "data": "<TaskBook[] example - see DTO>",
  "page": 1,
  "pageSize": 20,
  "total": 123
}
```

**错误示例 / Error**
```json
{
  "error": "HTTP 400: pageSize must be between 1 and 100",
  "details": {
    "code": 400,
    "message": "pageSize must be between 1 and 100"
  },
  "code": 400
}
```

#### `get_habits` — List habits.
- **API**: `GET /calendar/habits`
- **参数**: `page/pageSize,start/end?`
- **返回**: `HabitRoutine[]`

**签名 / Signature**
```python
async def get_habits(page: int = 1, pageSize: int = 20, start: Optional[str] = None, end: Optional[str] = None) -> Any: ...
```

**返回示例 / Success**
```json
{
  "code": 0,
  "data": "<HabitRoutine[] example - see DTO>",
  "page": 1,
  "pageSize": 20,
  "total": 123
}
```

**错误示例 / Error**
```json
{
  "error": "HTTP 400: pageSize must be between 1 and 100",
  "details": {
    "code": 400,
    "message": "pageSize must be between 1 and 100"
  },
  "code": 400
}
```

#### `get_availability_windows` — List availability windows.
- **API**: `GET /calendar/availability?start&end`
- **参数**: `start,end`
- **返回**: `AvailabilityWindow[]`

**签名 / Signature**
```python
async def get_availability_windows(start: str, end: str) -> Any: ...
```

**返回示例 / Success**
```json
{
  "code": 0,
  "data": "<AvailabilityWindow[] example - see DTO>"
}
```

**错误示例 / Error**
```json
{
  "error": "HTTP 400: time range too large: max span 366 days",
  "details": {
    "code": 400
  },
  "code": 400
}
```

#### `get_reminders` — List reminders.
- **API**: `GET /calendar/reminders?start&end`
- **参数**: `start,end?`
- **返回**: `Reminder[]`

**签名 / Signature**
```python
async def get_reminders(start: str, end: str) -> Any: ...
```

**返回示例 / Success**
```json
{
  "code": 0,
  "data": "<Reminder[] example - see DTO>"
}
```

**错误示例 / Error**
```json
{
  "error": "HTTP 400: time range too large: max span 366 days",
  "details": {
    "code": 400
  },
  "code": 400
}
```

#### `get_reminder_delivery_log` — Delivery log.
- **API**: `GET /calendar/reminders/delivery-log?start&end`
- **参数**: `start,end,page/pageSize`
- **返回**: `ReminderDelivery[]`

**签名 / Signature**
```python
async def get_reminder_delivery_log(start: str, end: str, page: int = 1, pageSize: int = 20) -> Any: ...
```

**返回示例 / Success**
```json
{
  "code": 0,
  "data": "<ReminderDelivery[] example - see DTO>",
  "page": 1,
  "pageSize": 20,
  "total": 123
}
```

**错误示例 / Error**
```json
{
  "error": "HTTP 400: pageSize must be between 1 and 100",
  "details": {
    "code": 400,
    "message": "pageSize must be between 1 and 100"
  },
  "code": 400
}
```

#### `get_reports` — List reports.
- **API**: `GET /calendar/reports?start&end`
- **参数**: `start,end,page/pageSize`
- **返回**: `ReportArtifact[]`

**签名 / Signature**
```python
async def get_reports(start: str, end: str, page: int = 1, pageSize: int = 20) -> Any: ...
```

**返回示例 / Success**
```json
{
  "code": 0,
  "data": "<ReportArtifact[] example - see DTO>",
  "page": 1,
  "pageSize": 20,
  "total": 123
}
```

**错误示例 / Error**
```json
{
  "error": "HTTP 400: pageSize must be between 1 and 100",
  "details": {
    "code": 400,
    "message": "pageSize must be between 1 and 100"
  },
  "code": 400
}
```

#### `get_report` — Single report.
- **API**: `GET /calendar/reports/{id}`
- **参数**: `id`
- **返回**: `ReportArtifact`

**签名 / Signature**
```python
async def get_report(id) -> Any: ...
```

**返回示例 / Success**
```json
{
  "code": 0,
  "data": "<ReportArtifact example - see DTO>"
}
```

**错误示例 / Error**
```json
{
  "error": "missing bearer token: call MCP with Authorization: Bearer <PIM JWT>",
  "code": 401
}
```

#### `get_calendars` — List calendars.
- **API**: `GET /calendar/calendars`
- **参数**: 无
- **返回**: `Calendar[]`

**签名 / Signature**
```python
async def get_calendars(-) -> Any: ...
```

**返回示例 / Success**
```json
{
  "code": 0,
  "data": "<Calendar[] example - see DTO>"
}
```

**错误示例 / Error**
```json
{
  "error": "missing bearer token: call MCP with Authorization: Bearer <PIM JWT>",
  "code": 401
}
```

#### `get_events` — Core event query.
- **API**: `GET /calendar/events?start&end&calendarId`
- **参数**: `start,end,calendarId?,page/pageSize,redactUrls`
- **返回**: `Event[]`

**签名 / Signature**
```python
async def get_events(start: str, end: str, redactUrls: bool = True, page: int = 1, pageSize: int = 20) -> Any: ...
```

**返回示例 / Success**
```json
{
  "code": 0,
  "data": {
    "items": [
      {
        "id": "b2e...d",
        "title": "Sprint Planning",
        "dtStart": "2026-08-31T02:00:00Z",
        "dtEnd": "2026-08-31T03:00:00Z",
        "urlHash": "a1b2c3d4e5f6"
      }
    ],
    "page": 1,
    "pageSize": 20,
    "total": 1,
    "totalPages": 1
  }
}
```

**错误示例 / Error**
```json
{
  "error": "HTTP 400: pageSize must be between 1 and 100",
  "details": {
    "code": 400,
    "message": "pageSize must be between 1 and 100"
  },
  "code": 400
}
```

#### `get_tasks` — List tasks.
- **API**: `GET /calendar/tasks?status&calendarId`
- **参数**: `status?,calendarId?,page/pageSize`
- **返回**: `Task[]`

**签名 / Signature**
```python
async def get_tasks(page: int = 1, pageSize: int = 20, status?: str) -> Any: ...
```

**返回示例 / Success**
```json
{
  "code": 0,
  "data": {
    "items": [
      {
        "id": "b2e...d",
        "title": "Sprint Planning"
      }
    ],
    "page": 1,
    "pageSize": 20,
    "total": 1,
    "totalPages": 1
  }
}
```

**错误示例 / Error**
```json
{
  "error": "HTTP 400: pageSize must be between 1 and 100",
  "details": {
    "code": 400,
    "message": "pageSize must be between 1 and 100"
  },
  "code": 400
}
```

#### `get_task_segments` — Segments for task.
- **API**: `GET /calendar/tasks/{id}/segments`
- **参数**: `id`
- **返回**: `TaskExecutionSegment[]`

**签名 / Signature**
```python
async def get_task_segments(id) -> Any: ...
```

**返回示例 / Success**
```json
{
  "code": 0,
  "data": "<TaskExecutionSegment[] example - see DTO>"
}
```

**错误示例 / Error**
```json
{
  "error": "missing bearer token: call MCP with Authorization: Bearer <PIM JWT>",
  "code": 401
}
```

#### `get_recycle_bin` — Recycle bin.
- **API**: `GET /calendar/recycle-bin?start&end`
- **参数**: `start,end?,type?,page/pageSize`
- **返回**: `RecycleBinItem[]`

**签名 / Signature**
```python
async def get_recycle_bin(start: str, end: str, page: int = 1, pageSize: int = 20) -> Any: ...
```

**返回示例 / Success**
```json
{
  "code": 0,
  "data": "<RecycleBinItem[] example - see DTO>",
  "page": 1,
  "pageSize": 20,
  "total": 123
}
```

**错误示例 / Error**
```json
{
  "error": "HTTP 400: pageSize must be between 1 and 100",
  "details": {
    "code": 400,
    "message": "pageSize must be between 1 and 100"
  },
  "code": 400
}
```

#### `preview_recycle_bin_restore` — Preview only.
- **API**: `POST /calendar/recycle-bin/{type}/{id}/restore-preview`
- **参数**: `type,id`
- **返回**: `RestorePreview`

**签名 / Signature**
```python
async def preview_recycle_bin_restore(type,id) -> Any: ...
```

**返回示例 / Success**
```json
{
  "code": 0,
  "data": "<RestorePreview example - see DTO>"
}
```

**错误示例 / Error**
```json
{
  "error": "missing bearer token: call MCP with Authorization: Bearer <PIM JWT>",
  "code": 401
}
```

#### `get_export_ics` — Export ICS.
- **API**: `GET /calendar/export-ics?start&end&calendarId`
- **参数**: `start,end,calendarId?/ids?`
- **返回**: `ics text`

**签名 / Signature**
```python
async def get_export_ics(start: str, end: str, calendarId: Optional[str] = None, ids: Optional[str] = None) -> Any: ...
```

**返回示例 / Success**
```json
{
  "code": 0,
  "data": "BEGIN:VCALENDAR\r\nVERSION:2.0\r\n...END:VCALENDAR",
  "contentType": "text/calendar"
}
```

**错误示例 / Error**
```json
{
  "error": "HTTP 400: time range too large: max span 366 days",
  "details": {
    "code": 400
  },
  "code": 400
}
```

#### `get_outlook_settings` — Read-only settings.
- **API**: `GET /calendar/outlook/settings`
- **参数**: 无
- **返回**: `OutlookSettingsResponse`

**签名 / Signature**
```python
async def get_outlook_settings(-) -> Any: ...
```

**返回示例 / Success**
```json
{
  "code": 0,
  "data": "<OutlookSettingsResponse example - see DTO>"
}
```

**错误示例 / Error**
```json
{
  "error": "missing bearer token: call MCP with Authorization: Bearer <PIM JWT>",
  "code": 401
}
```

#### `get_outlook_sync_batches` — Sync batches.
- **API**: `GET /calendar/outlook/sync/batches`
- **参数**: `page/pageSize`
- **返回**: `SyncBatch[]`

**签名 / Signature**
```python
async def get_outlook_sync_batches(page: int = 1, pageSize: int = 20) -> Any: ...
```

**返回示例 / Success**
```json
{
  "code": 0,
  "data": "<SyncBatch[] example - see DTO>",
  "page": 1,
  "pageSize": 20,
  "total": 123
}
```

**错误示例 / Error**
```json
{
  "error": "HTTP 400: pageSize must be between 1 and 100",
  "details": {
    "code": 400,
    "message": "pageSize must be between 1 and 100"
  },
  "code": 400
}
```

#### `get_outlook_local_data_preview` — Local data preview.
- **API**: `GET /calendar/outlook/local-data/preview`
- **参数**: `page/pageSize`
- **返回**: `Preview`

**签名 / Signature**
```python
async def get_outlook_local_data_preview(page: int = 1, pageSize: int = 20) -> Any: ...
```

**返回示例 / Success**
```json
{
  "code": 0,
  "data": "<Preview example - see DTO>",
  "page": 1,
  "pageSize": 20,
  "total": 123
}
```

**错误示例 / Error**
```json
{
  "error": "HTTP 400: pageSize must be between 1 and 100",
  "details": {
    "code": 400,
    "message": "pageSize must be between 1 and 100"
  },
  "code": 400
}
```

#### `get_event_by_id` — Single event via id.
- **API**: `GET /calendar/events/{id} (or filtered list)`
- **参数**: `id`
- **返回**: `Event`

**签名 / Signature**
```python
async def get_event_by_id(id) -> Any: ...
```

**返回示例 / Success**
```json
{
  "code": 0,
  "data": "<Event example - see DTO>"
}
```

**错误示例 / Error**
```json
{
  "error": "HTTP 404: not found",
  "details": {
    "code": 404,
    "message": "not found"
  },
  "code": 404
}
```

#### `get_task_by_id` — Single task.
- **API**: `GET /calendar/tasks/{id} (or filtered list)`
- **参数**: `id`
- **返回**: `Task`

**签名 / Signature**
```python
async def get_task_by_id(id) -> Any: ...
```

**返回示例 / Success**
```json
{
  "code": 0,
  "data": "<Task example - see DTO>"
}
```

**错误示例 / Error**
```json
{
  "error": "HTTP 404: not found",
  "details": {
    "code": 404,
    "message": "not found"
  },
  "code": 404
}
```

#### `get_habit_occurrences` — Habit occurrences.
- **API**: `GET /calendar/habits/{id}/occurrences?start&end`
- **参数**: `id,start,end`
- **返回**: `Occurrence[]`

**签名 / Signature**
```python
async def get_habit_occurrences(habit_id: str, start: str, end: str) -> Any: ...
```

**返回示例 / Success**
```json
{
  "code": 0,
  "data": "<Occurrence[] example - see DTO>"
}
```

**错误示例 / Error**
```json
{
  "error": "HTTP 400: time range too large: max span 366 days",
  "details": {
    "code": 400
  },
  "code": 400
}
```

#### `get_schedule_preview` — Schedule preview.
- **API**: `POST /calendar/schedule (preview)`
- **参数**: `taskIds?`
- **返回**: `SchedulePlan`

**签名 / Signature**
```python
async def get_schedule_preview(taskIds?) -> Any: ...
```

**返回示例 / Success**
```json
{
  "code": 0,
  "data": "<SchedulePlan example - see DTO>"
}
```

**错误示例 / Error**
```json
{
  "error": "missing bearer token: call MCP with Authorization: Bearer <PIM JWT>",
  "code": 401
}
```

#### `get_calendar_by_id` — Single calendar.
- **API**: `GET /calendar/calendars/{id} (filtered)`
- **参数**: `id`
- **返回**: `Calendar`

**签名 / Signature**
```python
async def get_calendar_by_id(id) -> Any: ...
```

**返回示例 / Success**
```json
{
  "code": 0,
  "data": "<Calendar example - see DTO>"
}
```

**错误示例 / Error**
```json
{
  "error": "missing bearer token: call MCP with Authorization: Bearer <PIM JWT>",
  "code": 401
}
```

#### `get_task_checklist` — Checklist.
- **API**: `GET /calendar/tasks/{id}/checklist`
- **参数**: `id`
- **返回**: `ChecklistItem[]`

**签名 / Signature**
```python
async def get_task_checklist(id) -> Any: ...
```

**返回示例 / Success**
```json
{
  "code": 0,
  "data": "<ChecklistItem[] example - see DTO>"
}
```

**错误示例 / Error**
```json
{
  "error": "missing bearer token: call MCP with Authorization: Bearer <PIM JWT>",
  "code": 401
}
```

#### `search_calendar_events` — Search events.
- **API**: `GET /calendar/events?search`
- **参数**: `q,start/end,page/pageSize`
- **返回**: `Event[]`

**签名 / Signature**
```python
async def search_calendar_events(q: str, start: Optional[str] = None, end: Optional[str] = None, page: int = 1, pageSize: int = 20) -> Any: ...
```

**返回示例 / Success**
```json
{
  "code": 0,
  "data": {
    "items": [
      {
        "id": "b2e...d",
        "title": "Sprint Planning"
      }
    ],
    "page": 1,
    "pageSize": 20,
    "total": 1,
    "totalPages": 1
  }
}
```

**错误示例 / Error**
```json
{
  "error": "HTTP 400: pageSize must be between 1 and 100",
  "details": {
    "code": 400,
    "message": "pageSize must be between 1 and 100"
  },
  "code": 400
}
```

#### `search_calendar_tasks` — Search tasks.
- **API**: `GET /calendar/tasks?search`
- **参数**: `q,start/end,page/pageSize`
- **返回**: `Task[]`

**签名 / Signature**
```python
async def search_calendar_tasks(q: str, start: Optional[str] = None, end: Optional[str] = None, page: int = 1, pageSize: int = 20) -> Any: ...
```

**返回示例 / Success**
```json
{
  "code": 0,
  "data": {
    "items": [
      {
        "id": "b2e...d",
        "title": "Sprint Planning"
      }
    ],
    "page": 1,
    "pageSize": 20,
    "total": 1,
    "totalPages": 1
  }
}
```

**错误示例 / Error**
```json
{
  "error": "HTTP 400: pageSize must be between 1 and 100",
  "details": {
    "code": 400,
    "message": "pageSize must be between 1 and 100"
  },
  "code": 400
}
```

### 5.2 PcTracker 27

#### `get_pc_summary` — Daily summary.
- **API**: `GET /pc/summary?date`
- **参数**: `date,timezone`
- **返回**: `PcSummaryResponse`

**签名 / Signature**
```python
async def get_pc_summary(date: str, timezone: str = 'Asia/Shanghai') -> Any: ...
```

**返回示例 / Success**
```json
{
  "code": 0,
  "data": "<PcSummaryResponse example - see DTO>"
}
```

**错误示例 / Error**
```json
{
  "error": "missing bearer token: call MCP with Authorization: Bearer <PIM JWT>",
  "code": 401
}
```

#### `get_pc_detail` — Detail records.
- **API**: `GET /pc/detail?date`
- **参数**: `date/dateFrom/dateTo,timezone,redactUrls,page/pageSize`
- **返回**: `TypedDetailQueryResponse`

**签名 / Signature**
```python
async def get_pc_detail(date: Optional[str] = None, dateFrom: Optional[str] = None, dateTo: Optional[str] = None, timezone: str = 'Asia/Shanghai', redactUrls: bool = True, page: int = 1, pageSize: int = 20) -> Any: ...
```

**返回示例 / Success**
```json
{
  "code": 0,
  "data": "<TypedDetailQueryResponse example - see DTO>",
  "page": 1,
  "pageSize": 20,
  "total": 123
}
```

**错误示例 / Error**
```json
{
  "error": "HTTP 400: pageSize must be between 1 and 100",
  "details": {
    "code": 400,
    "message": "pageSize must be between 1 and 100"
  },
  "code": 400
}
```

#### `get_pc_timeline` — Raw timeline v1.
- **API**: `GET /pc/aw/timeline?date`
- **参数**: `date,timezone`
- **返回**: `TimelineItem[]`

**签名 / Signature**
```python
async def get_pc_timeline(date: str, timezone: str = 'Asia/Shanghai') -> Any: ...
```

**返回示例 / Success**
```json
{
  "code": 0,
  "data": "<TimelineItem[] example - see DTO>"
}
```

**错误示例 / Error**
```json
{
  "error": "missing bearer token: call MCP with Authorization: Bearer <PIM JWT>",
  "code": 401
}
```

#### `get_pc_timeline_v2` — Smoothed timeline v2.
- **API**: `GET /pc/timeline/v2?date`
- **参数**: `date,timezone,redactUrls`
- **返回**: `TimelineV2Item[]`

**签名 / Signature**
```python
async def get_pc_timeline_v2(date: str, timezone: str = 'Asia/Shanghai', redactUrls: bool = True) -> Any: ...
```

**返回示例 / Success**
```json
{
  "code": 0,
  "data": "<TimelineV2Item[] example - see DTO>"
}
```

**错误示例 / Error**
```json
{
  "error": "missing bearer token: call MCP with Authorization: Bearer <PIM JWT>",
  "code": 401
}
```

#### `get_pc_heatmap` — Heatmap grid.
- **API**: `GET /pc/heatmap/grid?start&end&dimension`
- **参数**: `start,end,dimension=day,timezone`
- **返回**: `HeatmapGridResponse`

**签名 / Signature**
```python
async def get_pc_heatmap(start: str, end: str, timezone: str = 'Asia/Shanghai') -> Any: ...
```

**返回示例 / Success**
```json
{
  "code": 0,
  "data": "<HeatmapGridResponse example - see DTO>"
}
```

**错误示例 / Error**
```json
{
  "error": "HTTP 400: time range too large: max span 366 days",
  "details": {
    "code": 400
  },
  "code": 400
}
```

#### `get_pc_activity_analysis` — Activity analysis.
- **API**: `GET /pc/activity-analysis?date&blockMinutes`
- **参数**: `date,blockMinutes,timezone`
- **返回**: `PcActivityAnalysisResponse`

**签名 / Signature**
```python
async def get_pc_activity_analysis(date: str, timezone: str = 'Asia/Shanghai') -> Any: ...
```

**返回示例 / Success**
```json
{
  "code": 0,
  "data": "<PcActivityAnalysisResponse example - see DTO>"
}
```

**错误示例 / Error**
```json
{
  "error": "missing bearer token: call MCP with Authorization: Bearer <PIM JWT>",
  "code": 401
}
```

#### `get_pc_quality` — Quality report.
- **API**: `GET /pc/quality?date`
- **参数**: `date/dateFrom/dateTo,timezone`
- **返回**: `PcQualityResponse`

**签名 / Signature**
```python
async def get_pc_quality(date: str, timezone: str = 'Asia/Shanghai') -> Any: ...
```

**返回示例 / Success**
```json
{
  "code": 0,
  "data": "<PcQualityResponse example - see DTO>"
}
```

**错误示例 / Error**
```json
{
  "error": "missing bearer token: call MCP with Authorization: Bearer <PIM JWT>",
  "code": 401
}
```

#### `get_pc_aw_heatmap` — AW heatmap.
- **API**: `GET /pc/aw/heatmap?start&end`
- **参数**: `start,end,timezone`
- **返回**: `HeatmapBucket[]`

**签名 / Signature**
```python
async def get_pc_aw_heatmap(start: str, end: str, timezone: str = 'Asia/Shanghai') -> Any: ...
```

**返回示例 / Success**
```json
{
  "code": 0,
  "data": "<HeatmapBucket[] example - see DTO>"
}
```

**错误示例 / Error**
```json
{
  "error": "HTTP 400: time range too large: max span 366 days",
  "details": {
    "code": 400
  },
  "code": 400
}
```

#### `get_pc_keystats_range` — Keystats range.
- **API**: `GET /pc/keystats/range?start&end`
- **参数**: `start,end,timezone`
- **返回**: `KeystatsSummary[]`

**签名 / Signature**
```python
async def get_pc_keystats_range(start: str, end: str, timezone: str = 'Asia/Shanghai') -> Any: ...
```

**返回示例 / Success**
```json
{
  "code": 0,
  "data": "<KeystatsSummary[] example - see DTO>"
}
```

**错误示例 / Error**
```json
{
  "error": "HTTP 400: time range too large: max span 366 days",
  "details": {
    "code": 400
  },
  "code": 400
}
```

#### `get_pc_focus_blocks` — Focus blocks (weekly core).
- **API**: `GET /pc/aggregation/focus-blocks?start&end`
- **参数**: `start,end,timezone`
- **返回**: `PcFocusBlocksResponse`

**签名 / Signature**
```python
async def get_pc_focus_blocks(start: str, end: str, timezone: str = 'Asia/Shanghai') -> Any: ...
```

**返回示例 / Success**
```json
{
  "code": 0,
  "data": "<PcFocusBlocksResponse example - see DTO>"
}
```

**错误示例 / Error**
```json
{
  "error": "HTTP 400: time range too large: max span 366 days",
  "details": {
    "code": 400
  },
  "code": 400
}
```

#### `get_pc_app_usage` — App usage.
- **API**: `GET /pc/aggregation/app-usage?start&end`
- **参数**: `start,end,timezone,limit`
- **返回**: `PcAppUsageResponse`

**签名 / Signature**
```python
async def get_pc_app_usage(start: str, end: str, timezone: str = 'Asia/Shanghai') -> Any: ...
```

**返回示例 / Success**
```json
{
  "code": 0,
  "data": "<PcAppUsageResponse example - see DTO>"
}
```

**错误示例 / Error**
```json
{
  "error": "HTTP 400: time range too large: max span 366 days",
  "details": {
    "code": 400
  },
  "code": 400
}
```

#### `get_pc_late_night` — Late-night.
- **API**: `GET /pc/aggregation/late-night?start&end`
- **参数**: `start,end,timezone`
- **返回**: `PcLateNightResponse`

**签名 / Signature**
```python
async def get_pc_late_night(start: str, end: str, timezone: str = 'Asia/Shanghai') -> Any: ...
```

**返回示例 / Success**
```json
{
  "code": 0,
  "data": "<PcLateNightResponse example - see DTO>"
}
```

**错误示例 / Error**
```json
{
  "error": "HTTP 400: time range too large: max span 366 days",
  "details": {
    "code": 400
  },
  "code": 400
}
```

#### `get_pc_category_distribution` — Category distribution.
- **API**: `GET /pc/aggregation/category-distribution?start&end`
- **参数**: `start,end,timezone`
- **返回**: `PcCategoryDistributionResponse`

**签名 / Signature**
```python
async def get_pc_category_distribution(start: str, end: str, timezone: str = 'Asia/Shanghai') -> Any: ...
```

**返回示例 / Success**
```json
{
  "code": 0,
  "data": "<PcCategoryDistributionResponse example - see DTO>"
}
```

**错误示例 / Error**
```json
{
  "error": "HTTP 400: time range too large: max span 366 days",
  "details": {
    "code": 400
  },
  "code": 400
}
```

#### `get_pc_categories` — Categories flat.
- **API**: `GET /pc/categories`
- **参数**: 无
- **返回**: `AppCategoryRule[]`

**签名 / Signature**
```python
async def get_pc_categories(-) -> Any: ...
```

**返回示例 / Success**
```json
{
  "code": 0,
  "data": "<AppCategoryRule[] example - see DTO>"
}
```

**错误示例 / Error**
```json
{
  "error": "missing bearer token: call MCP with Authorization: Bearer <PIM JWT>",
  "code": 401
}
```

#### `get_pc_category_tree` — Category tree.
- **API**: `GET /pc/categories/tree`
- **参数**: 无
- **返回**: `CategoryTreeNode[]`

**签名 / Signature**
```python
async def get_pc_category_tree(-) -> Any: ...
```

**返回示例 / Success**
```json
{
  "code": 0,
  "data": "<CategoryTreeNode[] example - see DTO>"
}
```

**错误示例 / Error**
```json
{
  "error": "missing bearer token: call MCP with Authorization: Bearer <PIM JWT>",
  "code": 401
}
```

#### `get_pc_category_dictionary` — Category dictionary.
- **API**: `GET /pc/categories/dictionary`
- **参数**: 无
- **返回**: `CategoryDictionaryItemDto[]`

**签名 / Signature**
```python
async def get_pc_category_dictionary(-) -> Any: ...
```

**返回示例 / Success**
```json
{
  "code": 0,
  "data": "<CategoryDictionaryItemDto[] example - see DTO>"
}
```

**错误示例 / Error**
```json
{
  "error": "missing bearer token: call MCP with Authorization: Bearer <PIM JWT>",
  "code": 401
}
```

#### `get_pc_productivity_dashboard` — Productivity dashboard.
- **API**: `GET /pc/productivity/dashboard?date`
- **参数**: `date,timezone`
- **返回**: `ProductivityDashboardDto`

**签名 / Signature**
```python
async def get_pc_productivity_dashboard(date: str, timezone: str = 'Asia/Shanghai') -> Any: ...
```

**返回示例 / Success**
```json
{
  "code": 0,
  "data": "<ProductivityDashboardDto example - see DTO>"
}
```

**错误示例 / Error**
```json
{
  "error": "missing bearer token: call MCP with Authorization: Bearer <PIM JWT>",
  "code": 401
}
```

#### `get_pc_productivity_range` — Productivity range weekly.
- **API**: `GET /pc/productivity/range?start&end`
- **参数**: `start,end,timezone`
- **返回**: `DailyProductivityDto[]`

**签名 / Signature**
```python
async def get_pc_productivity_range(start: str, end: str, timezone: str = 'Asia/Shanghai') -> Any: ...
```

**返回示例 / Success**
```json
{
  "code": 0,
  "data": "<DailyProductivityDto[] example - see DTO>"
}
```

**错误示例 / Error**
```json
{
  "error": "HTTP 400: time range too large: max span 366 days",
  "details": {
    "code": 400
  },
  "code": 400
}
```

#### `get_classification_rules` — Classification rules.
- **API**: `GET /pc/classification/rules`
- **参数**: 无
- **返回**: `ActivityClassificationRuleDto[]`

**签名 / Signature**
```python
async def get_classification_rules(-) -> Any: ...
```

**返回示例 / Success**
```json
{
  "code": 0,
  "data": "<ActivityClassificationRuleDto[] example - see DTO>"
}
```

**错误示例 / Error**
```json
{
  "error": "missing bearer token: call MCP with Authorization: Bearer <PIM JWT>",
  "code": 401
}
```

#### `get_classification_suggestions` — Suggestions.
- **API**: `GET /pc/classification/suggestions?date`
- **参数**: `date`
- **返回**: `ActivityClassificationSuggestionDto[]`

**签名 / Signature**
```python
async def get_classification_suggestions(date: str) -> Any: ...
```

**返回示例 / Success**
```json
{
  "code": 0,
  "data": "<ActivityClassificationSuggestionDto[] example - see DTO>"
}
```

**错误示例 / Error**
```json
{
  "error": "missing bearer token: call MCP with Authorization: Bearer <PIM JWT>",
  "code": 401
}
```

#### `get_classification_queue` — Label queue.
- **API**: `GET /pc/classification/queue`
- **参数**: `limit,mode`
- **返回**: `ActivityLabelingQueueResponse`

**签名 / Signature**
```python
async def get_classification_queue(limit,mode) -> Any: ...
```

**返回示例 / Success**
```json
{
  "code": 0,
  "data": "<ActivityLabelingQueueResponse example - see DTO>"
}
```

**错误示例 / Error**
```json
{
  "error": "missing bearer token: call MCP with Authorization: Bearer <PIM JWT>",
  "code": 401
}
```

#### `get_classification_project_tags_recent` — Recent tags.
- **API**: `GET /pc/classification/project-tags/recent`
- **参数**: `limit`
- **返回**: `string[]`

**签名 / Signature**
```python
async def get_classification_project_tags_recent(limit) -> Any: ...
```

**返回示例 / Success**
```json
{
  "code": 0,
  "data": "<string[] example - see DTO>"
}
```

**错误示例 / Error**
```json
{
  "error": "missing bearer token: call MCP with Authorization: Bearer <PIM JWT>",
  "code": 401
}
```

#### `get_app_knowledge_apps` — Knowledge apps.
- **API**: `GET /pc/app-knowledge/apps?search`
- **参数**: `search?,page/pageSize`
- **返回**: `AppKnowledgeAppDto[]`

**签名 / Signature**
```python
async def get_app_knowledge_apps(page: int = 1, pageSize: int = 20, q: str) -> Any: ...
```

**返回示例 / Success**
```json
{
  "code": 0,
  "data": "<AppKnowledgeAppDto[] example - see DTO>",
  "page": 1,
  "pageSize": 20,
  "total": 123
}
```

**错误示例 / Error**
```json
{
  "error": "HTTP 400: pageSize must be between 1 and 100",
  "details": {
    "code": 400,
    "message": "pageSize must be between 1 and 100"
  },
  "code": 400
}
```

#### `get_app_knowledge_contexts` — App contexts.
- **API**: `GET /pc/app-knowledge/apps/{appId}/contexts`
- **参数**: `appId`
- **返回**: `AppKnowledgeContextDto[]`

**签名 / Signature**
```python
async def get_app_knowledge_contexts(appId) -> Any: ...
```

**返回示例 / Success**
```json
{
  "code": 0,
  "data": "<AppKnowledgeContextDto[] example - see DTO>"
}
```

**错误示例 / Error**
```json
{
  "error": "missing bearer token: call MCP with Authorization: Bearer <PIM JWT>",
  "code": 401
}
```

#### `get_app_signatures` — App signatures.
- **API**: `GET /pc/app-signatures`
- **参数**: `search?,page/pageSize`
- **返回**: `AppSignatureDto[]`

**签名 / Signature**
```python
async def get_app_signatures(search: Optional[str] = None, page: int = 1, pageSize: int = 50) -> Any: ...
```

**返回示例 / Success**
```json
{
  "code": 0,
  "data": "<AppSignatureDto[] example - see DTO>",
  "page": 1,
  "pageSize": 20,
  "total": 123
}
```

**错误示例 / Error**
```json
{
  "error": "HTTP 400: pageSize must be between 1 and 100",
  "details": {
    "code": 400,
    "message": "pageSize must be between 1 and 100"
  },
  "code": 400
}
```

#### `lookup_app_signature` — Lookup signature.
- **API**: `GET /pc/app-signatures/lookup/{processName}`
- **参数**: `processName`
- **返回**: `AppSignatureDto`

**签名 / Signature**
```python
async def lookup_app_signature(processName) -> Any: ...
```

**返回示例 / Success**
```json
{
  "code": 0,
  "data": "<AppSignatureDto example - see DTO>"
}
```

**错误示例 / Error**
```json
{
  "error": "missing bearer token: call MCP with Authorization: Bearer <PIM JWT>",
  "code": 401
}
```

#### `get_classification_settings` — Settings.
- **API**: `GET /pc/classification/settings`
- **参数**: 无
- **返回**: `ActivityClassificationSettingsDto`

**签名 / Signature**
```python
async def get_classification_settings(-) -> Any: ...
```

**返回示例 / Success**
```json
{
  "code": 0,
  "data": "<ActivityClassificationSettingsDto example - see DTO>"
}
```

**错误示例 / Error**
```json
{
  "error": "missing bearer token: call MCP with Authorization: Bearer <PIM JWT>",
  "code": 401
}
```

### 5.3 Mobile 18

#### `get_mobile_summary` — Mobile summary.
- **API**: `GET /mobile/summary?date`
- **参数**: `date,deviceId?,timezone`
- **返回**: `MobileUsageSummaryResponse`

**签名 / Signature**
```python
async def get_mobile_summary(date: str, timezone: str = 'Asia/Shanghai') -> Any: ...
```

**返回示例 / Success**
```json
{
  "code": 0,
  "data": "<MobileUsageSummaryResponse example - see DTO>"
}
```

**错误示例 / Error**
```json
{
  "error": "missing bearer token: call MCP with Authorization: Bearer <PIM JWT>",
  "code": 401
}
```

#### `get_mobile_timeline` — Mobile timeline.
- **API**: `GET /mobile/timeline?date`
- **参数**: `date,deviceId?,timezone,redactUrls`
- **返回**: `MobileTimelineResponse`

**签名 / Signature**
```python
async def get_mobile_timeline(date: str, timezone: str = 'Asia/Shanghai', redactUrls: bool = True) -> Any: ...
```

**返回示例 / Success**
```json
{
  "code": 0,
  "data": "<MobileTimelineResponse example - see DTO>"
}
```

**错误示例 / Error**
```json
{
  "error": "missing bearer token: call MCP with Authorization: Bearer <PIM JWT>",
  "code": 401
}
```

#### `get_mobile_location_history` — Location history.
- **API**: `GET /mobile/location/history?start&end`
- **参数**: `start,end,maxAccuracyMeters,deviceId?`
- **返回**: `MobileLocationHistoryResponse`

**签名 / Signature**
```python
async def get_mobile_location_history(start: str, end: str) -> Any: ...
```

**返回示例 / Success**
```json
{
  "code": 0,
  "data": "<MobileLocationHistoryResponse example - see DTO>"
}
```

**错误示例 / Error**
```json
{
  "error": "HTTP 400: time range too large: max span 366 days",
  "details": {
    "code": 400
  },
  "code": 400
}
```

#### `get_mobile_location_latest` — Latest point.
- **API**: `GET /mobile/location/history (latest)`
- **参数**: `maxAccuracyMeters,deviceId?`
- **返回**: `MobileLocationPointDto`

**签名 / Signature**
```python
async def get_mobile_location_latest(maxAccuracyMeters,deviceId?) -> Any: ...
```

**返回示例 / Success**
```json
{
  "code": 0,
  "data": "<MobileLocationPointDto example - see DTO>"
}
```

**错误示例 / Error**
```json
{
  "error": "missing bearer token: call MCP with Authorization: Bearer <PIM JWT>",
  "code": 401
}
```

#### `get_mobile_location_tracks` — Tracks.
- **API**: `GET /mobile/location/analytics/tracks`
- **参数**: `start,end,timezone,maxAccuracyMeters`
- **返回**: `MobileLocationTrackDto[]`

**签名 / Signature**
```python
async def get_mobile_location_tracks(start: str, end: str, timezone: str = 'Asia/Shanghai') -> Any: ...
```

**返回示例 / Success**
```json
{
  "code": 0,
  "data": "<MobileLocationTrackDto[] example - see DTO>"
}
```

**错误示例 / Error**
```json
{
  "error": "HTTP 400: time range too large: max span 366 days",
  "details": {
    "code": 400
  },
  "code": 400
}
```

#### `get_mobile_location_overview` — Location overview.
- **API**: `GET /mobile/location/analytics/overview`
- **参数**: `start,end,timezone`
- **返回**: `MobileLocationAnalyticsOverviewResponse`

**签名 / Signature**
```python
async def get_mobile_location_overview(start: str, end: str, timezone: str = 'Asia/Shanghai') -> Any: ...
```

**返回示例 / Success**
```json
{
  "code": 0,
  "data": "<MobileLocationAnalyticsOverviewResponse example - see DTO>"
}
```

**错误示例 / Error**
```json
{
  "error": "HTTP 400: time range too large: max span 366 days",
  "details": {
    "code": 400
  },
  "code": 400
}
```

#### `get_mobile_location_frequent_places` — Frequent places.
- **API**: `GET /mobile/location/analytics/frequent-places`
- **参数**: `start,end,timezone`
- **返回**: `MobileFrequentPlacesResponse`

**签名 / Signature**
```python
async def get_mobile_location_frequent_places(start: str, end: str, timezone: str = 'Asia/Shanghai') -> Any: ...
```

**返回示例 / Success**
```json
{
  "code": 0,
  "data": "<MobileFrequentPlacesResponse example - see DTO>"
}
```

**错误示例 / Error**
```json
{
  "error": "HTTP 400: time range too large: max span 366 days",
  "details": {
    "code": 400
  },
  "code": 400
}
```

#### `get_mobile_location_movement_stats` — Movement stats.
- **API**: `GET /mobile/location/analytics/movement-stats`
- **参数**: `start,end,timezone`
- **返回**: `MobileMovementStatsResponse`

**签名 / Signature**
```python
async def get_mobile_location_movement_stats(start: str, end: str, timezone: str = 'Asia/Shanghai') -> Any: ...
```

**返回示例 / Success**
```json
{
  "code": 0,
  "data": "<MobileMovementStatsResponse example - see DTO>"
}
```

**错误示例 / Error**
```json
{
  "error": "HTTP 400: time range too large: max span 366 days",
  "details": {
    "code": 400
  },
  "code": 400
}
```

#### `get_mobile_quality` — Quality.
- **API**: `GET /mobile/quality?date`
- **参数**: `date,deviceId?,timezone`
- **返回**: `MobileQualityResponse`

**签名 / Signature**
```python
async def get_mobile_quality(date: str, timezone: str = 'Asia/Shanghai') -> Any: ...
```

**返回示例 / Success**
```json
{
  "code": 0,
  "data": "<MobileQualityResponse example - see DTO>"
}
```

**错误示例 / Error**
```json
{
  "error": "missing bearer token: call MCP with Authorization: Bearer <PIM JWT>",
  "code": 401
}
```

#### `get_mobile_analytics_overview` — Analytics overview (weekly core).
- **API**: `GET /mobile/analytics/overview?start&end`
- **参数**: `start,end,timezone`
- **返回**: `MobileAnalyticsOverviewResponse`

**签名 / Signature**
```python
async def get_mobile_analytics_overview(start: str, end: str, timezone: str = 'Asia/Shanghai') -> Any: ...
```

**返回示例 / Success**
```json
{
  "code": 0,
  "data": "<MobileAnalyticsOverviewResponse example - see DTO>"
}
```

**错误示例 / Error**
```json
{
  "error": "HTTP 400: time range too large: max span 366 days",
  "details": {
    "code": 400
  },
  "code": 400
}
```

#### `get_mobile_analytics_heatmap` — Heatmap.
- **API**: `GET /mobile/analytics/heatmap?start&end`
- **参数**: `start,end,timezone`
- **返回**: `MobileHeatmapBucketDto[]`

**签名 / Signature**
```python
async def get_mobile_analytics_heatmap(start: str, end: str, timezone: str = 'Asia/Shanghai') -> Any: ...
```

**返回示例 / Success**
```json
{
  "code": 0,
  "data": "<MobileHeatmapBucketDto[] example - see DTO>"
}
```

**错误示例 / Error**
```json
{
  "error": "HTTP 400: time range too large: max span 366 days",
  "details": {
    "code": 400
  },
  "code": 400
}
```

#### `get_mobile_analytics_charts` — Charts.
- **API**: `GET /mobile/analytics/charts?start&end`
- **参数**: `start,end,timezone`
- **返回**: `MobileAnalyticsChartDto[]`

**签名 / Signature**
```python
async def get_mobile_analytics_charts(start: str, end: str, timezone: str = 'Asia/Shanghai') -> Any: ...
```

**返回示例 / Success**
```json
{
  "code": 0,
  "data": "<MobileAnalyticsChartDto[] example - see DTO>"
}
```

**错误示例 / Error**
```json
{
  "error": "HTTP 400: time range too large: max span 366 days",
  "details": {
    "code": 400
  },
  "code": 400
}
```

#### `get_mobile_timeline_blocks` — Timeline blocks.
- **API**: `GET /mobile/analytics/timeline-blocks?start&end`
- **参数**: `start,end,timezone,page/pageSize`
- **返回**: `MobileTimelineBlockPageDto`

**签名 / Signature**
```python
async def get_mobile_timeline_blocks(start: str, end: str, timezone: str = 'Asia/Shanghai', page: int = 1, pageSize: int = 20) -> Any: ...
```

**返回示例 / Success**
```json
{
  "code": 0,
  "data": "<MobileTimelineBlockPageDto example - see DTO>",
  "page": 1,
  "pageSize": 20,
  "total": 123
}
```

**错误示例 / Error**
```json
{
  "error": "HTTP 400: pageSize must be between 1 and 100",
  "details": {
    "code": 400,
    "message": "pageSize must be between 1 and 100"
  },
  "code": 400
}
```

#### `get_mobile_devices` — Devices simple.
- **API**: `GET /mobile/devices`
- **参数**: 无
- **返回**: `MobileDeviceDto[]`

**签名 / Signature**
```python
async def get_mobile_devices(-) -> Any: ...
```

**返回示例 / Success**
```json
{
  "code": 0,
  "data": "<MobileDeviceDto[] example - see DTO>"
}
```

**错误示例 / Error**
```json
{
  "error": "missing bearer token: call MCP with Authorization: Bearer <PIM JWT>",
  "code": 401
}
```

#### `get_mobile_devices_manage` — Devices manage.
- **API**: `GET /mobile/devices/manage?sortBy`
- **参数**: `sortBy?`
- **返回**: `DeviceListDto[]`

**签名 / Signature**
```python
async def get_mobile_devices_manage(sortBy?) -> Any: ...
```

**返回示例 / Success**
```json
{
  "code": 0,
  "data": "<DeviceListDto[] example - see DTO>"
}
```

**错误示例 / Error**
```json
{
  "error": "missing bearer token: call MCP with Authorization: Bearer <PIM JWT>",
  "code": 401
}
```

#### `get_mobile_apps_catalog_overrides` — Catalog overrides.
- **API**: `GET /mobile/apps/catalog-overrides`
- **参数**: 无
- **返回**: `MobileAppCatalogOverrideDto[]`

**签名 / Signature**
```python
async def get_mobile_apps_catalog_overrides(-) -> Any: ...
```

**返回示例 / Success**
```json
{
  "code": 0,
  "data": "<MobileAppCatalogOverrideDto[] example - see DTO>"
}
```

**错误示例 / Error**
```json
{
  "error": "missing bearer token: call MCP with Authorization: Bearer <PIM JWT>",
  "code": 401
}
```

#### `get_mobile_apps_category_rules` — Category rules.
- **API**: `GET /mobile/apps/category-rules`
- **参数**: 无
- **返回**: `MobileAppCategoryRuleDto[]`

**签名 / Signature**
```python
async def get_mobile_apps_category_rules(-) -> Any: ...
```

**返回示例 / Success**
```json
{
  "code": 0,
  "data": "<MobileAppCategoryRuleDto[] example - see DTO>"
}
```

**错误示例 / Error**
```json
{
  "error": "missing bearer token: call MCP with Authorization: Bearer <PIM JWT>",
  "code": 401
}
```

#### `get_mobile_goals` — Usage goals.
- **API**: `GET /mobile/analytics/goals`
- **参数**: 无
- **返回**: `MobileUsageGoalDto[]`

**签名 / Signature**
```python
async def get_mobile_goals(-) -> Any: ...
```

**返回示例 / Success**
```json
{
  "code": 0,
  "data": "<MobileUsageGoalDto[] example - see DTO>"
}
```

**错误示例 / Error**
```json
{
  "error": "missing bearer token: call MCP with Authorization: Bearer <PIM JWT>",
  "code": 401
}
```

### 5.4 QuickNotes 3

#### `get_quick_notes` — List notes.
- **API**: `GET /quick-notes?status&search&page&pageSize`
- **参数**: `status?,search?,page/pageSize`
- **返回**: `PagedResult<QuickNoteListItemDto>`

**签名 / Signature**
```python
async def get_quick_notes(page: int = 1, pageSize: int = 20, q: str, status?: str) -> Any: ...
```

**返回示例 / Success**
```json
{
  "code": 0,
  "data": "<PagedResult<QuickNoteListItemDto> example - see DTO>",
  "page": 1,
  "pageSize": 20,
  "total": 123
}
```

**错误示例 / Error**
```json
{
  "error": "HTTP 400: pageSize must be between 1 and 100",
  "details": {
    "code": 400,
    "message": "pageSize must be between 1 and 100"
  },
  "code": 400
}
```

#### `get_quick_note` — Single note.
- **API**: `GET /quick-notes/{id}`
- **参数**: `id`
- **返回**: `QuickNoteDetailDto`

**签名 / Signature**
```python
async def get_quick_note(id) -> Any: ...
```

**返回示例 / Success**
```json
{
  "code": 0,
  "data": "<QuickNoteDetailDto example - see DTO>"
}
```

**错误示例 / Error**
```json
{
  "error": "HTTP 404: not found",
  "details": {
    "code": 404,
    "message": "not found"
  },
  "code": 404
}
```

#### `get_quick_note_attachment_meta` — Attachment meta only, no binary.
- **API**: `GET /quick-notes/attachments/{id}/download (meta)`
- **参数**: `id`
- **返回**: `metadata`

**签名 / Signature**
```python
async def get_quick_note_attachment_meta(id) -> Any: ...
```

**返回示例 / Success**
```json
{
  "code": 0,
  "data": "<metadata example - see DTO>"
}
```

**错误示例 / Error**
```json
{
  "error": "missing bearer token: call MCP with Authorization: Bearer <PIM JWT>",
  "code": 401
}
```

### 5.5 Files 8

#### `get_file_providers` — Providers.
- **API**: `GET /files/providers`
- **参数**: 无
- **返回**: `FileProviderDto[]`

**签名 / Signature**
```python
async def get_file_providers(-) -> Any: ...
```

**返回示例 / Success**
```json
{
  "code": 0,
  "data": "<FileProviderDto[] example - see DTO>"
}
```

**错误示例 / Error**
```json
{
  "error": "missing bearer token: call MCP with Authorization: Bearer <PIM JWT>",
  "code": 401
}
```

#### `get_files` — List files.
- **API**: `GET /files/items?folderId&page&pageSize`
- **参数**: `folderId?,page/pageSize,redactUrls`
- **返回**: `FileListResponse`

**签名 / Signature**
```python
async def get_files(page: int = 1, pageSize: int = 20) -> Any: ...
```

**返回示例 / Success**
```json
{
  "code": 0,
  "data": "<FileListResponse example - see DTO>",
  "page": 1,
  "pageSize": 20,
  "total": 123
}
```

**错误示例 / Error**
```json
{
  "error": "HTTP 400: pageSize must be between 1 and 100",
  "details": {
    "code": 400,
    "message": "pageSize must be between 1 and 100"
  },
  "code": 400
}
```

#### `get_file` — Single file.
- **API**: `GET /files/items/{id}`
- **参数**: `id`
- **返回**: `FileItemDto`

**签名 / Signature**
```python
async def get_file(id) -> Any: ...
```

**返回示例 / Success**
```json
{
  "code": 0,
  "data": "<FileItemDto example - see DTO>"
}
```

**错误示例 / Error**
```json
{
  "error": "HTTP 404: not found",
  "details": {
    "code": 404,
    "message": "not found"
  },
  "code": 404
}
```

#### `get_file_versions` — Versions.
- **API**: `GET /files/items/{id}/versions`
- **参数**: `id`
- **返回**: `FileVersion[]`

**签名 / Signature**
```python
async def get_file_versions(id) -> Any: ...
```

**返回示例 / Success**
```json
{
  "code": 0,
  "data": "<FileVersion[] example - see DTO>"
}
```

**错误示例 / Error**
```json
{
  "error": "missing bearer token: call MCP with Authorization: Bearer <PIM JWT>",
  "code": 401
}
```

#### `get_file_trash` — Trash.
- **API**: `GET /files/trash`
- **参数**: `page/pageSize`
- **返回**: `ProviderTrashItem[]`

**签名 / Signature**
```python
async def get_file_trash(page: int = 1, pageSize: int = 20) -> Any: ...
```

**返回示例 / Success**
```json
{
  "code": 0,
  "data": "<ProviderTrashItem[] example - see DTO>",
  "page": 1,
  "pageSize": 20,
  "total": 123
}
```

**错误示例 / Error**
```json
{
  "error": "HTTP 400: pageSize must be between 1 and 100",
  "details": {
    "code": 400,
    "message": "pageSize must be between 1 and 100"
  },
  "code": 400
}
```

#### `search_files` — Search files (RAG core).
- **API**: `GET /files/search?q&page&pageSize`
- **参数**: `q,page/pageSize`
- **返回**: `FileSearchResponse`

**签名 / Signature**
```python
async def search_files(page: int = 1, pageSize: int = 20, q: str) -> Any: ...
```

**返回示例 / Success**
```json
{
  "code": 0,
  "data": "<FileSearchResponse example - see DTO>",
  "page": 1,
  "pageSize": 20,
  "total": 123
}
```

**错误示例 / Error**
```json
{
  "error": "HTTP 400: pageSize must be between 1 and 100",
  "details": {
    "code": 400,
    "message": "pageSize must be between 1 and 100"
  },
  "code": 400
}
```

#### `get_file_suggestions` — Suggestions.
- **API**: `GET /files/suggestions`
- **参数**: `page/pageSize`
- **返回**: `FileSuggestion[]`

**签名 / Signature**
```python
async def get_file_suggestions(page: int = 1, pageSize: int = 20) -> Any: ...
```

**返回示例 / Success**
```json
{
  "code": 0,
  "data": "<FileSuggestion[] example - see DTO>",
  "page": 1,
  "pageSize": 20,
  "total": 123
}
```

**错误示例 / Error**
```json
{
  "error": "HTTP 400: pageSize must be between 1 and 100",
  "details": {
    "code": 400,
    "message": "pageSize must be between 1 and 100"
  },
  "code": 400
}
```

#### `get_file_open_link` — Open link (redacted by default).
- **API**: `GET /files/items/{id}/open-link`
- **参数**: `id`
- **返回**: `{openLink}`

**签名 / Signature**
```python
async def get_file_open_link(id) -> Any: ...
```

**返回示例 / Success**
```json
{
  "code": 0,
  "data": "<{openLink} example - see DTO>"
}
```

**错误示例 / Error**
```json
{
  "error": "missing bearer token: call MCP with Authorization: Bearer <PIM JWT>",
  "code": 401
}
```

### 5.6 Core/Infra 14

#### `get_today_sections` — Today registry.
- **API**: `GET /today/sections?date`
- **参数**: `date?,timezone`
- **返回**: `TodaySectionRegistryDto`

**签名 / Signature**
```python
async def get_today_sections(date: str, timezone: str = 'Asia/Shanghai') -> Any: ...
```

**返回示例 / Success**
```json
{
  "code": 0,
  "data": "<TodaySectionRegistryDto example - see DTO>"
}
```

**错误示例 / Error**
```json
{
  "error": "missing bearer token: call MCP with Authorization: Bearer <PIM JWT>",
  "code": 401
}
```

#### `get_today_section` — Single section.
- **API**: `GET /today/sections/{sectionId}?date`
- **参数**: `sectionId,date?`
- **返回**: `TodaySectionDto`

**签名 / Signature**
```python
async def get_today_section(date: str) -> Any: ...
```

**返回示例 / Success**
```json
{
  "code": 0,
  "data": "<TodaySectionDto example - see DTO>"
}
```

**错误示例 / Error**
```json
{
  "error": "missing bearer token: call MCP with Authorization: Bearer <PIM JWT>",
  "code": 401
}
```

#### `search_pim` — Global search.
- **API**: `GET /search?q&type&limit`
- **参数**: `q,type?=event,task,note,file,limit`
- **返回**: `PagedResult<SearchResult>`

**签名 / Signature**
```python
async def search_pim(q,type?=event,task,note,file,limit) -> Any: ...
```

**返回示例 / Success**
```json
{
  "code": 0,
  "data": "<PagedResult<SearchResult> example - see DTO>"
}
```

**错误示例 / Error**
```json
{
  "error": "missing bearer token: call MCP with Authorization: Bearer <PIM JWT>",
  "code": 401
}
```

#### `get_system_status` — System status.
- **API**: `GET /status`
- **参数**: 无
- **返回**: `StatusResponse`

**签名 / Signature**
```python
async def get_system_status(-) -> Any: ...
```

**返回示例 / Success**
```json
{
  "code": 0,
  "data": "<StatusResponse example - see DTO>"
}
```

**错误示例 / Error**
```json
{
  "error": "missing bearer token: call MCP with Authorization: Bearer <PIM JWT>",
  "code": 401
}
```

#### `get_system_health` — Health check.
- **API**: `GET /health`
- **参数**: 无
- **返回**: `{status,timestamp}`

**签名 / Signature**
```python
async def get_system_health(-) -> Any: ...
```

**返回示例 / Success**
```json
{
  "code": 0,
  "data": "<{status,timestamp} example - see DTO>"
}
```

**错误示例 / Error**
```json
{
  "error": "missing bearer token: call MCP with Authorization: Bearer <PIM JWT>",
  "code": 401
}
```

#### `get_status_summary` — Status summary.
- **API**: `GET /status/summary`
- **参数**: 无
- **返回**: `SummaryResponse`

**签名 / Signature**
```python
async def get_status_summary(-) -> Any: ...
```

**返回示例 / Success**
```json
{
  "code": 0,
  "data": "<SummaryResponse example - see DTO>"
}
```

**错误示例 / Error**
```json
{
  "error": "missing bearer token: call MCP with Authorization: Bearer <PIM JWT>",
  "code": 401
}
```

#### `get_ai_status` — AI status.
- **API**: `GET /ai/status`
- **参数**: 无
- **返回**: `{enabled,model,health}`

**签名 / Signature**
```python
async def get_ai_status(-) -> Any: ...
```

**返回示例 / Success**
```json
{
  "code": 0,
  "data": "<{enabled,model,health} example - see DTO>"
}
```

**错误示例 / Error**
```json
{
  "error": "missing bearer token: call MCP with Authorization: Bearer <PIM JWT>",
  "code": 401
}
```

#### `get_ai_requests` — AI requests.
- **API**: `GET /ai/requests?from&to&module&status`
- **参数**: `from,to?,module?,status?,page/pageSize`
- **返回**: `PagedResult<AiRequest>`

**签名 / Signature**
```python
async def get_ai_requests(from_time: Optional[str] = None, to: Optional[str] = None, module: Optional[str] = None, status: Optional[str] = None, page: int = 1, pageSize: int = 20) -> Any: ...
```

**返回示例 / Success**
```json
{
  "code": 0,
  "data": "<PagedResult<AiRequest> example - see DTO>",
  "page": 1,
  "pageSize": 20,
  "total": 123
}
```

**错误示例 / Error**
```json
{
  "error": "HTTP 400: pageSize must be between 1 and 100",
  "details": {
    "code": 400,
    "message": "pageSize must be between 1 and 100"
  },
  "code": 400
}
```

#### `get_ai_usage_summary` — AI usage summary.
- **API**: `GET /ai/usage/summary?from&to`
- **参数**: `from,to?`
- **返回**: `{totalRequests,tokens,cost}`

**签名 / Signature**
```python
async def get_ai_usage_summary(from,to?) -> Any: ...
```

**返回示例 / Success**
```json
{
  "code": 0,
  "data": "<{totalRequests,tokens,cost} example - see DTO>"
}
```

**错误示例 / Error**
```json
{
  "error": "missing bearer token: call MCP with Authorization: Bearer <PIM JWT>",
  "code": 401
}
```

#### `get_audit_timeline` — Audit timeline.
- **API**: `GET /operations/audit/{objectType}/{objectId}`
- **参数**: `objectType,objectId`
- **返回**: `AuditVersion[]`

**签名 / Signature**
```python
async def get_audit_timeline(objectType,objectId) -> Any: ...
```

**返回示例 / Success**
```json
{
  "code": 0,
  "data": "<AuditVersion[] example - see DTO>"
}
```

**错误示例 / Error**
```json
{
  "error": "missing bearer token: call MCP with Authorization: Bearer <PIM JWT>",
  "code": 401
}
```

#### `get_audit_export` — Audit export.
- **API**: `GET /operations/audit/export?start&end`
- **参数**: `start,end`
- **返回**: `AuditExport`

**签名 / Signature**
```python
async def get_audit_export(start: str, end: str) -> Any: ...
```

**返回示例 / Success**
```json
{
  "code": 0,
  "data": "<AuditExport example - see DTO>"
}
```

**错误示例 / Error**
```json
{
  "error": "HTTP 400: time range too large: max span 366 days",
  "details": {
    "code": 400
  },
  "code": 400
}
```

#### `get_confirmations_pending` — Pending confirmations.
- **API**: `GET /operations/confirmations/pending`
- **参数**: 无
- **返回**: `OperationConfirmationDto[]`

**签名 / Signature**
```python
async def get_confirmations_pending(-) -> Any: ...
```

**返回示例 / Success**
```json
{
  "code": 0,
  "data": "<OperationConfirmationDto[] example - see DTO>"
}
```

**错误示例 / Error**
```json
{
  "error": "missing bearer token: call MCP with Authorization: Bearer <PIM JWT>",
  "code": 401
}
```

#### `get_endpoints` — Endpoints.
- **API**: `GET /endpoints`
- **参数**: 无
- **返回**: `EndpointDto[]`

**签名 / Signature**
```python
async def get_endpoints(-) -> Any: ...
```

**返回示例 / Success**
```json
{
  "code": 0,
  "data": "<EndpointDto[] example - see DTO>"
}
```

**错误示例 / Error**
```json
{
  "error": "missing bearer token: call MCP with Authorization: Bearer <PIM JWT>",
  "code": 401
}
```

#### `get_version` — Version info.
- **API**: `GET /api/version`
- **参数**: 无
- **返回**: `{version,gitSha,buildTime}`

**签名 / Signature**
```python
async def get_version(-) -> Any: ...
```

**返回示例 / Success**
```json
{
  "code": 0,
  "data": "<{version,gitSha,buildTime} example - see DTO>"
}
```

**错误示例 / Error**
```json
{
  "error": "missing bearer token: call MCP with Authorization: Bearer <PIM JWT>",
  "code": 401
}
```

## 6. 典型调用流 / Recipes

### 6.1 今天有啥事 / What's today (4 steps)
```json
[
  {
    "tool": "get_today_sections",
    "arguments": {
      "date": "2026-08-31"
    }
  },
  {
    "tool": "get_calendar_layers",
    "arguments": {
      "start": "2026-08-31T00:00:00Z",
      "end": "2026-08-31T16:00:00Z",
      "layers": "all",
      "timezone": "Asia/Shanghai"
    }
  },
  {
    "tool": "get_tasks",
    "arguments": {
      "status": "pending",
      "pageSize": 20
    }
  },
  {
    "tool": "get_reminders",
    "arguments": {
      "start": "2026-08-31T00:00:00Z",
      "end": "2026-08-31T16:00:00Z"
    }
  }
]
```

### 6.2 这周效率咋样 / Weekly efficiency (4 steps)
```json
[
  {
    "tool": "get_pc_productivity_range",
    "arguments": {
      "start": "2026-08-24T16:00:00Z",
      "end": "2026-08-31T16:00:00Z",
      "timezone": "Asia/Shanghai"
    }
  },
  {
    "tool": "get_pc_focus_blocks",
    "arguments": {
      "start": "2026-08-24T16:00:00Z",
      "end": "2026-08-31T16:00:00Z",
      "timezone": "Asia/Shanghai"
    }
  },
  {
    "tool": "get_pc_category_distribution",
    "arguments": {
      "start": "2026-08-24T16:00:00Z",
      "end": "2026-08-31T16:00:00Z",
      "timezone": "Asia/Shanghai"
    }
  },
  {
    "tool": "get_mobile_analytics_overview",
    "arguments": {
      "start": "2026-08-24T16:00:00Z",
      "end": "2026-08-31T16:00:00Z",
      "timezone": "Asia/Shanghai"
    }
  }
]
```

### 6.3 人在哪，在干啥 / Where & what (4 steps)
```json
[
  {
    "tool": "get_mobile_location_latest",
    "arguments": {
      "maxAccuracyMeters": 50
    }
  },
  {
    "tool": "get_mobile_timeline",
    "arguments": {
      "date": "2026-08-31",
      "timezone": "Asia/Shanghai"
    }
  },
  {
    "tool": "get_mobile_quality",
    "arguments": {
      "date": "2026-08-31",
      "timezone": "Asia/Shanghai"
    }
  },
  {
    "tool": "get_pc_timeline_v2",
    "arguments": {
      "date": "2026-08-31",
      "timezone": "Asia/Shanghai"
    }
  }
]
```

### 6.4 找个文件/笔记 / Find note/file (3 steps)
```json
[
  {
    "tool": "search_pim",
    "arguments": {
      "q": "Q3 计划",
      "type": "note,file",
      "limit": 10
    }
  },
  {
    "tool": "search_files",
    "arguments": {
      "q": "Q3 计划",
      "pageSize": 10
    }
  },
  {
    "tool": "get_quick_notes",
    "arguments": {
      "search": "Q3",
      "pageSize": 10
    }
  }
]
```

## 7. 脱敏与隐私 / Redaction

- 仅脱敏 `url`/`link`/`href`：`redactUrls=true`（默认）时，任何 key 含 `url`/`link`/`href` 的字段（`url`, `onlineMeetingUrl`, `openLink`, `externalLink`, `href`）值被替换为 `sha256(url)[0:12]`，字段改名为 `urlHash`/`xxxUrlHash`/`hrefHash`，原文不返回。（按工单 C7 扩展 `link/href` 以防 `openLink` 泄露）
- `title`/`description`/`location` 保留原文。
- MCP 侧后处理，不改 DB。
- `redactUrls=false` 返回原文（仍需登录，审计留痕）。何时用 `false`：Agent 需点击原文链接时，显式传 `redactUrls=false`。

```json
{
  "input": {
    "url": "https://example.com/page?a=1",
    "title": "My Page"
  },
  "redactUrls_true_example": {
    "urlHash": "74e432cd3d91",
    "title": "My Page"
  },
  "redactUrls_false": {
    "url": "https://example.com/page?a=1",
    "title": "MyPage"
  }
}
```

**示例 / Example**
```json
{
  "redactUrls": true,
  "in": {
    "url": "https://example.com/secret?token=abc",
    "title": "Secret Doc"
  },
  "out": {
    "urlHash": "62611908c0cf",
    "title": "Secret Doc"
  }
}
```
```json
{
  "redactUrls": false,
  "in": {
    "url": "https://example.com/secret?token=abc"
  },
  "out": {
    "url": "https://example.com/secret?token=abc"
  }
}
```

## 8. 限流与分页 / Pagination & Limits

- `page>=1`, `1<=pageSize<=100` 默认 20，`pageSize>100` 返回 400。
- `max_items` MCP 熔断：单工具最多 100 条，超限提示 `nextPage`。
- 超大 `>50KB` 截断：返回 `{"truncated":true,"nextPage":2,"_note":"response >50KB..."}`，客户端用 `nextPage` 继续。
- 时间最大跨度 366 天，超限 400。时区默认 `Asia/Shanghai`，聚合内部按此切天。

```json
{
  "error": "pageSize must be between 1 and 100",
  "code": 400
}
```
```json
{
  "code": 0,
  "data": {
    "items": [
      "...100 items..."
    ],
    "page": 1,
    "pageSize": 100,
    "total": 250
  },
  "truncated": true,
  "nextPage": 2,
  "_note": "response >50KB"
}
```

## 9. 常见问题 / FAQ

**Q: stdio 模式如何传 token？ / How to pass token in stdio?**
A: 设环境变量 `PIM_ACCESS_TOKEN=<jwt>`（也支持 `PIM_TOKEN`/`MCP_BEARER_TOKEN`），或在 HTTP 模式下带 `Authorization` 头。

**Q: token 过期怎么办？ / Token expired?**
A: 重新 `POST /api/v1/auth/login` 拿新 `accessToken`，更新环境变量或请求头后重试。stdio 长驻进程建议：① 设置 `PIM_TOKEN_FILE` 指向外部刷新的 `.token` 文件（MCP 按 mtime 热重载），或 ② 设置 `PIM_REFRESH_TOKEN` 让 MCP 在 401 时自动刷新；HTTP 模式每次请求带新头即可。

**Q: get_event_by_id 找不到？ / Event not found?**
A: 事件可能被软删除进回收站，用 `get_recycle_bin(type='event')` 或扩大时间范围 `start/end` 后 `get_events` 搜索。

**Q: 返回被截断 / Truncated?**
A: `>50KB` 自动加 `truncated`，用 `nextPage` 分页继续。或缩小时间范围、加 `calendarId` 过滤。

**Q: 需要写入怎么办？ / Need writes?**
A: 本版 0 写入，写入能力 Phase 3 再开，已在 `README TODO` 占坑。

**Q: 时区跨天不准？ / Timezone day cut?**
A: 所有聚合按 `timezone` 切天，默认为 `Asia/Shanghai`。传入 `timezone=UTC` 可按 UTC 切天。

## 10. 变更日志 / Changelog

| 版本 | 日期 | 变更 |
|---|---|---|
| v1.0 | 2026-08-15 | 24 工具：`PcTracker 16 + Mobile 位置 4 + Today/Search/Status 4` |
| v2.0 | 2026-08-31 | 新增 77 工具 → 101 工具闭环：`Calendar 31 + PcTracker +11 + Mobile +14 + QuickNotes 3 + Files 8 + Core/Infra +10`，兼容 v1，写入 0 |

---

**维护 / Maintenance**
- 代码：`scripts/mcp/pim_mcp_server.py`（主）+ 生产镜像 `/root/.hermes/scripts/pim_mcp_server.py`
- 测试：`dotnet test Pim.sln` 不受 MCP 影响；MCP 冒烟：带真实 token 调用 §6 四流。
- 反馈：提 PR 至 `opencode-linux/mcp-v2-readonly` 分支。