# PIM 个人信息管理平台 — 核心平台设计

## 概述

个人信息管理分析系统（PIM），采用客户端-服务端架构，支持模块化扩展。

- **客户端**: Windows (WPF) + Android (Kotlin 原生)
- **服务端**: ASP.NET Core 模块化单体
- **部署**: Docker Compose，自建 NAS

---

## 模块与优先级

| 阶段 | 模块 | 描述 |
|------|------|------|
| V1 | 核心平台 | 认证、API 框架、模块注册、用户管理、跨模块搜索 |
| V1 | 日程与任务 | RFC 5545 .ics 标准，日历本 + 任务收集箱 + 自动排程 |
| V2 | 文件资料管理 | 网盘视图，版本管理（Kopia），文件预览编辑 |
| V2 | 活动记录 | 键鼠操作、窗口活动、时间轴 + 热力图 + 日程关联 |

---

## 一、技术栈总览

| 层次 | 选型 |
|------|------|
| 服务端框架 | ASP.NET Core 8, C# 12 |
| 数据库 | PostgreSQL 16 |
| 文件存储 | MinIO |
| API 风格 | RESTful, JSON, URL 版本号 |
| 认证 | JWT 双 Token (access 15min + refresh 7d), bcrypt |
| Windows 客户端 | WPF (.NET 8), CommunityToolkit.Mvvm |
| Android 客户端 | Kotlin, Jetpack Compose, Hilt, Retrofit |
| 部署 | Docker Compose, nginx 反向代理 |
| 模块化 | 编译时模块化, DI 注册, IModule 接口 |
| 版控引擎 | Kopia (CLI 调用) |
| 文本提取 | Apache Tika (独立容器) |
| 日历库 | Ical.Net |
| 外部日历 | Microsoft Graph API + Change Notifications / Webhooks |
| 活动采集 | KeyStats API + ActivityWatch API (Windows) / PACKAGE_USAGE_STATS (Android) |

---

## 二、服务端架构

### 2.1 分层结构

```
┌──────────────────────────────────────────┐
│              API Layer                    │
│  Controllers, DTOs, Auth Middleware       │
├──────────────────────────────────────────┤
│           Application Layer               │
│  Module Registry, Use Cases, Validation   │
├──────────────────────────────────────────┤
│             Domain Layer                  │
│  Entities, Value Objects, Module Contracts│
├──────────────────────────────────────────┤
│          Infrastructure Layer             │
│  EF Core (PG), MinIO, JWT, Logging       │
└──────────────────────────────────────────┘
```

### 2.2 项目结构

```
src/
├── Pim.Core/                    # 核心抽象
│   ├── Modules/
│   │   ├── IModule.cs           # 模块接口契约
│   │   └── ISearchProvider.cs   # 跨模块搜索接口
│   ├── Exceptions/
│   │   └── DomainException.cs   # 业务异常基类
│   └── Common/
│       ├── ApiResponse.cs       # 统一响应
│       └── PagedResult.cs       # 分页结果
├── Pim.Infrastructure/          # 基础设施
│   ├── Data/
│   │   └── PimDbContext.cs      # EF Core DbContext
│   ├── Auth/
│   │   ├── JwtService.cs
│   │   └── PasswordHasher.cs
│   ├── Storage/
│   │   ├── MinioStorage.cs
│   │   └── KopiaService.cs      # Kopia CLI 封装
│   ├── TextExtraction/
│   │   └── TikaClient.cs        # Apache Tika HTTP 客户端
│   └── Extensions/              # DI 扩展方法
├── Pim.Api/                     # 主机入口
│   ├── Program.cs
│   ├── appsettings.json
│   ├── Middleware/
│   │   └── ExceptionMiddleware.cs
│   ├── ModuleRegistry.cs        # 扫描并注册所有 IModule
│   └── Search/
│       └── SearchController.cs  # 跨模块搜索聚合
├── modules/
│   ├── Pim.Module.Calendar/
│   │   ├── Pim.Module.Calendar.csproj
│   │   ├── CalendarModule.cs
│   │   ├── Controllers/
│   │   ├── Services/
│   │   │   ├── CalendarService.cs
│   │   │   ├── IcsService.cs          # Ical.Net 封装
│   │   │   ├── SchedulingEngine.cs    # 自动排程引擎
│   │   │   └── OutlookSyncService.cs  # MS Graph 集成
│   │   └── Entities/
│   ├── Pim.Module.Files/
│   │   ├── Pim.Module.Files.csproj
│   │   ├── FilesModule.cs
│   │   ├── Controllers/
│   │   ├── Services/
│   │   │   ├── FileService.cs
│   │   │   ├── VersionService.cs      # Kopia 版本管理
│   │   │   ├── SyncService.cs         # 双向同步引擎
│   │   │   └── TagService.cs          # 层级标签
│   │   └── Entities/
│   └── Pim.Module.Activity/
│       ├── Pim.Module.Activity.csproj
│       ├── ActivityModule.cs
│       ├── Controllers/
│       ├── Services/
│       │   ├── ActivityIngestionService.cs
│       │   ├── TimelineService.cs
│       │   ├── HeatmapService.cs
│       │   └── CalendarCorrelationService.cs
│       └── Entities/
└── shared/
    └── Pim.Shared.Contracts/    # DTO, API 路径常量, 搜索契约
```

### 2.3 模块接口 `IModule`

```csharp
public interface IModule
{
    string Name { get; }
    string Version { get; }
    void RegisterServices(IServiceCollection services, IConfiguration configuration);
    void MapEndpoints(IEndpointRouteBuilder endpoints);
    Task InitializeAsync(IServiceProvider serviceProvider);
}
```

### 2.4 跨模块搜索接口 `ISearchProvider`

每个需要被搜索的模块实现此接口：

```csharp
public interface ISearchProvider
{
    string ModuleName { get; }
    Task<IReadOnlyList<SearchResult>> SearchAsync(string query, int limit, CancellationToken ct);
}

public record SearchResult(
    string ModuleName,
    string Type,         // "event" | "task" | "file" | "activity_session"
    string Id,
    string Title,
    string Snippet,      // 匹配内容摘要
    string Url           // 客户端跳转路径
);
```

服务端 `/api/v1/search?q=xxx&type=event,task,file` 聚合各模块结果，按 `type` 参数过滤。

---

## 三、API 规范

### 3.1 统一响应格式

```json
{
  "code": 0,
  "message": "success",
  "data": { },
  "timestamp": "2026-05-15T10:30:00Z"
}
```

### 3.2 分页响应

```json
{
  "code": 0,
  "data": {
    "items": [],
    "page": 1,
    "pageSize": 20,
    "totalCount": 157,
    "totalPages": 8
  }
}
```

### 3.3 路由规范

```
/api/v1/{module}/{resource}

# 认证
POST   /api/v1/auth/register
POST   /api/v1/auth/login
POST   /api/v1/auth/refresh

# 跨模块搜索
GET    /api/v1/search?q=xxx&type=event,task,file&limit=20

# 日程
GET    /api/v1/calendar/calendars
POST   /api/v1/calendar/calendars
GET    /api/v1/calendar/events?start=...&end=...
POST   /api/v1/calendar/events
PUT    /api/v1/calendar/events/{id}
DELETE /api/v1/calendar/events/{id}
GET    /api/v1/calendar/tasks?inbox=true
POST   /api/v1/calendar/tasks
PUT    /api/v1/calendar/tasks/{id}
DELETE /api/v1/calendar/tasks/{id}
POST   /api/v1/calendar/tasks/{id}/move        # 拖拽移动任务
POST   /api/v1/calendar/schedule               # 触发自动排程
POST   /api/v1/calendar/schedule/confirm        # 确认排程方案
POST   /api/v1/calendar/import-ics
GET    /api/v1/calendar/export-ics
POST   /api/v1/calendar/outlook/sync
GET    /api/v1/calendar/outlook/status

# 文件
GET    /api/v1/files/items?path=/docs
POST   /api/v1/files/upload
GET    /api/v1/files/items/{id}/download
DELETE /api/v1/files/items/{id}
PUT    /api/v1/files/items/{id}/rename
PUT    /api/v1/files/items/{id}/move
GET    /api/v1/files/items/{id}/versions
POST   /api/v1/files/items/{id}/versions/{versionId}/restore
GET    /api/v1/files/items/{id}/preview
POST   /api/v1/files/tags
GET    /api/v1/files/tags

# 活动记录
POST   /api/v1/activity/ingest              # 客户端批量上传
GET    /api/v1/activity/timeline?date=...
GET    /api/v1/activity/heatmap?start=...&end=...
GET    /api/v1/activity/summary?period=daily|weekly|monthly
GET    /api/v1/activity/correlation?event_id=...  # 日程关联
```

### 3.4 客户端上传队列（活动记录专用）

活动记录数据量大，客户端需实现低优先级上传通道：

- 独立的 `BackgroundUploader` 服务，使用独立的 `HttpClient` 实例
- 上传使用 HTTP/2 多路复用，请求体 gzip 压缩
- 本地 SQLite 作为上传队列缓冲区，上传成功后才删除本地记录
- 控制并发连接数（最多 2 个），避免阻塞其他 API 请求
- 网络不可用时数据保留本地，恢复后自动续传

---

## 四、认证与权限

### 4.1 JWT 双 Token 流程

```
注册/登录 → { accessToken (15min), refreshToken (7d) }
每次请求 → Authorization: Bearer {accessToken}
accessToken 过期 → POST /api/v1/auth/refresh { refreshToken } → 返回新 token pair
```

- `accessToken`: 15 分钟，内存持有，不做服务端失效
- `refreshToken`: 7 天，存数据库，可通过改密码/登出标记撤销
- 密码使用 bcrypt 哈希（cost factor 12）

### 4.2 登录保护

- 同一 IP 连续失败 5 次 → 临时锁定 15 分钟
- 锁定记录存 `login_attempts` 表

### 4.3 权限模型

- **Role**: `user` / `admin`
- **数据隔离**: 非 admin 所有查询自动过滤 `user_id = current_user_id`
- 各模块可定义额外细粒度权限
- Outlook 写操作：必须用户手动确认（双端提示）

---

## 五、日程与任务模块 (V1)

### 5.1 功能清单

| 功能 | 描述 |
|------|------|
| 日历 CRUD | 创建/编辑/删除日历，颜色标识，默认日历 |
| 事件 CRUD | 创建/编辑/删除事件，标题/描述/地点/时间 |
| 重复事件 | 支持 RRULE（每天/每周/每月/每年/自定义） |
| 多视图 | 月视图、周视图、日视图、议程列表 |
| .ics 导入/导出 | 通过 Ical.Net 服务端解析/生成 |
| 任务收集箱 | 右侧面板，新建任务自动入箱 |
| 拖拽排程 | 从收集箱拖拽任务到日历网格（开放 API） |
| 排程按钮 | 勾选任务后底部按钮触发自动排程 |
| 自动排程 | 贪心 + CSP + 遗传算法，LLM 兜底，多方案对比 |
| 任务分段 | 手动拆分 + 自动分段（递归），自动操作需用户确认 |
| 排程验证 | 结合活动记录验证执行情况，未执行自动重排延后 |
| 遗传算法训练 | 日常弹窗随机展示方案，收集用户偏好反馈 |
| 提醒通知 | 事件到期前本地通知（Windows Toast / Android Notification） |
| 搜索 | PG 全文搜索 (zhparser)，标题+描述 |
| Outlook 集成 | Microsoft Graph API，Webhooks 自动同步，写操作需确认 |

### 5.2 UI 布局

```
┌─────────────────────┬──────────────┐
│                     │  任务收集箱    │
│   日历视图           │  ┌─────────┐  │
│  (月/周/日/议程)     │  │ Task 1 ☐ │  │
│                     │  │ Task 2 ☑ │  │
│                     │  │ Task 3 ☐ │  │
│                     │  └─────────┘  │
│                     │ [ 排程选中任务 ]│
└─────────────────────┴──────────────┘
```

- 左侧日历区域 70% 宽度，右侧收集箱 30% 宽度
- 收集箱内任务可拖拽到日历网格上的时间位置
- 勾选任务后点击底部按钮自动排程

### 5.3 自动排程引擎

#### 算法层次

```
┌─ 用户偏好层 ──────────────────────────────────┐
│  遗传算法权重（通过弹窗反馈持续训练）            │
├─ 求解层 ──────────────────────────────────────┤
│  1. 优先级贪心 → 最快方案（< 100ms）           │
│  2. CSP 回溯   → 最优方案（超时 30s 回退）     │
│  3. 遗传算法   → 用户偏好方案                   │
├─ 兜底层 ──────────────────────────────────────┤
│  LLM (OpenAI 兼容 API) → 算法无解时调用        │
├─ 约束校验层 ───────────────────────────────────┤
│  硬约束：时间不冲突、不超过截止日期             │
│  软约束：偏好时段、连续工作时长、分段间隔       │
└───────────────────────────────────────────────┘
```

#### 排程输入

- 日历中所有已有事件（占用时段）
- 收集箱中勾选的任务（预估时长、优先级、截止日期、最小时段）
- 用户偏好权重（通过遗传算法弹窗训练得出）

#### 排程输出

- 多方案列表（每种算法一个，LLM 兜底方案可额外加入）
- 用户对比选择 → 手动确认 → 任务变为日历事件
- 确认后的方案记录 `schedule_plan_id` 用于后续关联验证

#### 遗传算法训练机制

- 日常随机弹窗展示 2-3 个排程方案
- 用户选择偏好方案 → 记录选择到 `scheduling_feedback` 表
- 权重定期更新（如每 50 条反馈重新训练）

#### 排程验证反馈

- 活动记录模块提供时间段内用户实际处于目标应用的时间
- 若任务安排的时间段内用户没有对应活动 → 标记为"未执行"
- 未执行任务自动回到收集箱，下次排程时优先级加权

### 5.4 Outlook 集成

- 通过 Microsoft Graph API 与 Outlook 日历双向同步
- 使用 Outlook Change Notifications + Webhooks 实时获取变更
- 服务端缓存 Outlook 事件副本，客户端通过 Pim.Api 间接访问
- 保留手动触发同步按钮
- **写入 Outlook 的操作必须用户双端确认**

### 5.5 确认机制

所有自动操作（排程结果、拆分决策、重排建议、Outlook 写入）需用户手动确认：
1. 生成待确认项，存入 `pending_confirmations` 表
2. Windows 和 Android 双端同时推送通知
3. 用户在任一端确认后，服务端执行操作并同步状态到另一端

---

## 六、文件资料模块 (V2)

### 6.1 功能清单

| 功能 | 描述 |
|------|------|
| 基础操作 | 上传/下载/删除/重命名/移动，文件夹管理 |
| 版本管理 | Kopia 快照，右键查看版本列表，选择版本下载 |
| 文件预览 | 非二进制文件/已知格式双击后下载并调用默认程序打开 |
| 双向同步 | FileSystemWatcher 实时监听 + 定时增量比对 |
| 内容搜索 | Tika 提取文本 + PostgreSQL 全文索引 |
| 标签 | 层级标签 + 自定义文字描述 |

### 6.2 Kopia 版本管理

```
                          ┌──────────────────────┐
  文件上传                 │   MinIO              │
  ┌────────┐              │  ┌────────────────┐  │
  │ 用户   │── 上传 ──→    │  │ file_bucket/   │  │
  └────────┘              │  │  {id}/v1.pdf   │  │
                          │  └────────────────┘  │
                          │                      │
  版本快照                 │  ┌────────────────┐  │
  ┌────────┐              │  │ kopia_repo/    │  │  ← Kopia 管理
  │ Pim.Api│── kopia snap │  │  snapshot_1/   │  │    去重+加密
  └────────┘              │  │  snapshot_2/   │  │
                          │  └────────────────┘  │
                          └──────────────────────┘
```

**流程**:

1. 用户上传文件 → MinIO → 触发 Kopia 快照
2. Kopia 创建快照 → 记录 `snapshot_id` 到 `file_versions.snapshot_id`
3. 用户右键查看版本列表 → 服务端从 Kopia 列出快照
4. 用户选择某版本下载 → 服务端 `kopia restore` 提取 → 返回文件流

**Kopia 调用方式**: 服务端通过 `Process.Start` 调用 `kopia` CLI，封装在 `KopiaService` 中。

### 6.3 Apache Tika 文本提取

```
文件上传 → MinIO 存储 → Tika 容器 (HTTP 提取) → 提取文本 → PostgreSQL tsvector 索引
```

- Tika 作为独立 Docker 容器运行，端口 9998
- `TikaClient.cs` 发送文件流到 Tika `/tika` endpoint，返回纯文本
- 提取的文本存 `file_items.content_text` 列（仅供索引，不返回原始文本）
- 支持格式：PDF, DOCX, XLSX, PPTX, TXT, HTML, 图片 OCR（Tika 自动处理）

### 6.4 双向同步引擎

```
┌──────────────────────────────────────────────┐
│              Windows 同步引擎                  │
│                                              │
│  FileSystemWatcher ──→ 变更队列                │
│       │                    │                 │
│       ▼                    ▼                  │
│  [防抖 5s]          [定时全量比对 (每小时)]     │
│       │                    │                 │
│       └────────┬───────────┘                 │
│                ▼                              │
│         Diff 结果 (增/改/删)                  │
│                │                              │
│         ┌──────┴──────┐                      │
│         ▼              ▼                      │
│     本地→服务端    服务端→本地                  │
│     (上传变更)     (下载变更)                   │
│         │              │                      │
│         └──────┬──────┘                      │
│                ▼                              │
│          冲突: 最后写入胜出                     │
└──────────────────────────────────────────────┘
```

- `FileSystemWatcher` 监听事件做 5 秒防抖后入队
- 每小时做一次全量时间戳+哈希比对修复不一致
- 同步目录由用户在设置中指定（默认 `%USERPROFILE%\PimSync`）

### 6.5 层级标签

```sql
tags (
  id UUID PRIMARY KEY,
  user_id UUID NOT NULL REFERENCES users(id),
  parent_id UUID REFERENCES tags(id),       -- null = 根标签
  name VARCHAR(100) NOT NULL,
  color VARCHAR(7),
  created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
)

file_tags (
  file_item_id UUID NOT NULL REFERENCES file_items(id),
  tag_id UUID NOT NULL REFERENCES tags(id),
  PRIMARY KEY (file_item_id, tag_id)
)
```

- 标签支持嵌套（如 `工作/项目A/设计稿`）
- 每个文件可附加多个标签
- 文件自定义描述存 `file_items.description` 字段

---

## 七、活动记录模块 (V2)

### 7.1 功能清单

| 功能 | 描述 |
|------|------|
| 数据采集 (Windows) | KeyStats API (键鼠统计) + ActivityWatch API (窗口事件) |
| 数据采集 (Android) | PACKAGE_USAGE_STATS |
| 批量上传 | 客户端队列 + gzip + 低优先级通道 |
| 时间轴 | 每日时间线，展示每时段应用使用 + 操作数据 |
| 统计仪表盘 | 应用使用排行、按键/点击趋势、生产力评分 |
| GitHub 风格热力图 | 年度/月度活动密度视图 |
| 日程关联 | 匹配日历事件与实际执行，生成偏差报告 |

### 7.2 数据来源

#### Windows: KeyStats (http://127.0.0.1:18080)

```
GET /api/stats/
→ { keyPresses, leftClicks, mouseDistance, appStats: {...}, peakKPS, ... }
```

- 日聚合统计数据，建议 1 秒轮询
- 包含按进程名细分的键盘/鼠标/滚轮统计

#### Windows: ActivityWatch (http://127.0.0.1:5600)

```
GET  /api/0/buckets/                           → 列出所有 bucket
GET  /api/0/buckets/{id}/events?start=&end=    → 获取时间范围事件
POST /api/0/buckets/{id}/heartbeat             → 心跳更新
```

- 事件级窗口切换和活动记录，Bucket/Event/Heartbeat 模型
- 周期性拉取事件，全量上传到 Pim 服务端

#### Android: PACKAGE_USAGE_STATS

```kotlin
val usageStatsManager = getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
val stats = usageStatsManager.queryUsageStats(
    UsageStatsManager.INTERVAL_DAILY,
    startTime, endTime
)
```

- 系统权限，获取应用使用时长和前台时间

### 7.3 采集流程

```
┌─ Windows 客户端 ─────────────────────────────┐
│                                               │
│  Timer(1s) ──→ KeyStats API ──→ 数据点缓存   │
│  Timer(5s) ──→ ActivityWatch ──→ 事件缓存     │
│                          │                    │
│                   本地 SQLite 缓冲区           │
│                          │                    │
│              BackgroundUploader               │
│              (低优先级, gzip, 最多2并发)       │
│                          │                    │
└──────────────────────────┼────────────────────┘
                           │
                    POST /api/v1/activity/ingest
                    (批量, 压缩, HTTP/2)
                           │
                    ┌──────┴──────┐
                    │  Pim.Api     │
                    │  Ingestion   │
                    │  Service     │
                    └─────────────┘
```

### 7.4 分析展示

#### 时间轴视图

```
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
 09:00  VS Code    编程    ████████████
 10:30  Chrome      浏览    ██████████
 11:15  Outlook    邮件    ████
 12:00  ── 午餐 ──
 13:00  VS Code    编程    ████████████████
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
```

#### 热力图（GitHub 风格）

```
        Mon Tue Wed Thu Fri Sat Sun
  5/1   ██  ███  █   ███  █   ░   ░
  5/8   ███  ██  ██  ██  ███  ░   █
  ...
```

#### 日程关联

```
事件: "编写模块文档" (14:00-16:00)
实际: VS Code 活动 1.5h → 完成度 75%
事件: "项目会议" (10:00-11:00)
实际: Microsoft Teams 活动 0.9h → 完成度 90%
```

所有分析计算在服务端执行，客户端仅负责展示。

---

## 八、数据模型

### 8.1 核心表

```sql
users (
  id UUID PRIMARY KEY,
  username VARCHAR(50) UNIQUE NOT NULL,
  email VARCHAR(255) UNIQUE NOT NULL,
  password_hash VARCHAR(255) NOT NULL,
  display_name VARCHAR(100),
  role VARCHAR(20) NOT NULL DEFAULT 'user',
  is_active BOOLEAN NOT NULL DEFAULT true,
  created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
)

refresh_tokens (
  id UUID PRIMARY KEY,
  user_id UUID NOT NULL REFERENCES users(id),
  token_hash VARCHAR(255) NOT NULL,
  expires_at TIMESTAMPTZ NOT NULL,
  revoked_at TIMESTAMPTZ,
  created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
)

login_attempts (
  id UUID PRIMARY KEY,
  user_id UUID REFERENCES users(id),
  ip_address VARCHAR(45) NOT NULL,
  success BOOLEAN NOT NULL,
  attempted_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
)
```

### 8.2 日程模块

```sql
calendars (
  id UUID PRIMARY KEY,
  user_id UUID NOT NULL REFERENCES users(id),
  name VARCHAR(100) NOT NULL,
  color VARCHAR(7) NOT NULL DEFAULT '#3B82F6',
  is_default BOOLEAN NOT NULL DEFAULT false,
  created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
)

events (
  id UUID PRIMARY KEY,
  calendar_id UUID NOT NULL REFERENCES calendars(id),
  uid VARCHAR(255) NOT NULL,
  title VARCHAR(255) NOT NULL,
  description TEXT,
  location VARCHAR(500),
  dtstart TIMESTAMPTZ NOT NULL,
  dtend TIMESTAMPTZ NOT NULL,
  dtstamp TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  rrule TEXT,
  status VARCHAR(20) NOT NULL DEFAULT 'CONFIRMED',
  organizer VARCHAR(255),
  source VARCHAR(20) NOT NULL DEFAULT 'manual',    -- 'manual' | 'outlook' | 'schedule'
  outlook_event_id VARCHAR(255),                   -- Outlook 端事件 ID
  schedule_plan_id UUID,                           -- 排程方案 ID
  search_vector tsvector,
  created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  deleted_at TIMESTAMPTZ
)

tasks (
  id UUID PRIMARY KEY,
  calendar_id UUID REFERENCES calendars(id),
  uid VARCHAR(255) NOT NULL,
  title VARCHAR(255) NOT NULL,
  description TEXT,
  priority INTEGER NOT NULL DEFAULT 0,
  estimated_duration INTERVAL,                     -- 预估耗时
  minimum_segment INTERVAL,                        -- 最小时段（自动分段用）
  dtstart TIMESTAMPTZ,
  due TIMESTAMPTZ,
  completed_at TIMESTAMPTZ,
  status VARCHAR(20) NOT NULL DEFAULT 'NEEDS-ACTION',
  percent_complete INTEGER NOT NULL DEFAULT 0,
  parent_task_id UUID REFERENCES tasks(id),        -- 父任务（手动拆分）
  is_inbox BOOLEAN NOT NULL DEFAULT true,          -- 是否在收集箱中
  sort_order INTEGER NOT NULL DEFAULT 0,           -- 收集箱中的排序
  schedule_plan_id UUID,
  search_vector tsvector,
  created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  deleted_at TIMESTAMPTZ
)

pending_confirmations (
  id UUID PRIMARY KEY,
  user_id UUID NOT NULL REFERENCES users(id),
  type VARCHAR(50) NOT NULL,                       -- 'schedule' | 'task_split' | 'reschedule' | 'outlook_write'
  summary TEXT NOT NULL,
  payload JSONB NOT NULL,
  status VARCHAR(20) NOT NULL DEFAULT 'pending',   -- 'pending' | 'confirmed' | 'rejected'
  confirmed_at TIMESTAMPTZ,
  created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
)

scheduling_feedback (
  id UUID PRIMARY KEY,
  user_id UUID NOT NULL REFERENCES users(id),
  plan_options JSONB NOT NULL,                     -- 展示的方案选项
  selected_index INTEGER NOT NULL,                 -- 用户选择的方案索引
  context JSONB,                                   -- 排程时的上下文（时间、任务数等）
  created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
)

outlook_connections (
  id UUID PRIMARY KEY,
  user_id UUID NOT NULL REFERENCES users(id),
  access_token_encrypted BYTEA NOT NULL,
  refresh_token_encrypted BYTEA,
  subscription_id VARCHAR(255),
  subscription_expires_at TIMESTAMPTZ,
  last_synced_at TIMESTAMPTZ,
  created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
)
```

### 8.3 文件资料模块

```sql
file_items (
  id UUID PRIMARY KEY,
  user_id UUID NOT NULL REFERENCES users(id),
  parent_id UUID REFERENCES file_items(id),
  name VARCHAR(255) NOT NULL,
  item_type VARCHAR(10) NOT NULL,                   -- 'file' | 'folder'
  size BIGINT NOT NULL DEFAULT 0,
  mime_type VARCHAR(100),
  checksum VARCHAR(64),
  description TEXT,                                 -- 自定义文字描述
  content_text TEXT,                                -- Tika 提取的全文索引内容
  search_vector tsvector,
  kopia_snapshot_id VARCHAR(255),                   -- Kopia 最新快照 ID
  sync_root_id UUID,                                -- 所属同步根目录
  created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  deleted_at TIMESTAMPTZ
)

file_versions (
  id UUID PRIMARY KEY,
  file_item_id UUID NOT NULL REFERENCES file_items(id),
  version_number INTEGER NOT NULL,
  size BIGINT NOT NULL,
  checksum VARCHAR(64) NOT NULL,
  storage_path VARCHAR(500) NOT NULL,
  kopia_snapshot_id VARCHAR(255) NOT NULL,
  comment VARCHAR(500),
  created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
)

tags (
  id UUID PRIMARY KEY,
  user_id UUID NOT NULL REFERENCES users(id),
  parent_id UUID REFERENCES tags(id),
  name VARCHAR(100) NOT NULL,
  color VARCHAR(7),
  UNIQUE (user_id, parent_id, name),
  created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
)

file_tags (
  file_item_id UUID NOT NULL REFERENCES file_items(id),
  tag_id UUID NOT NULL REFERENCES tags(id),
  PRIMARY KEY (file_item_id, tag_id)
)

sync_roots (
  id UUID PRIMARY KEY,
  user_id UUID NOT NULL REFERENCES users(id),
  local_path VARCHAR(500) NOT NULL,
  remote_root_id UUID REFERENCES file_items(id),
  device_name VARCHAR(100) NOT NULL,
  last_synced_at TIMESTAMPTZ,
  created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
)
```

### 8.4 活动记录模块

```sql
activity_sessions (
  id UUID PRIMARY KEY,
  user_id UUID NOT NULL REFERENCES users(id),
  device_name VARCHAR(100) NOT NULL,
  source VARCHAR(20) NOT NULL,                      -- 'keystats' | 'activitywatch' | 'android'
  started_at TIMESTAMPTZ NOT NULL,
  ended_at TIMESTAMPTZ,
  created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
)

-- 按月分表: window_events_202605
window_events (
  id BIGINT PRIMARY KEY,
  session_id UUID NOT NULL REFERENCES activity_sessions(id),
  timestamp TIMESTAMPTZ NOT NULL,
  window_title VARCHAR(500) NOT NULL,
  process_name VARCHAR(200) NOT NULL,
  event_type VARCHAR(20) NOT NULL,                  -- 'focus' | 'blur' | 'title_change'
  duration_seconds INTEGER                          -- 该窗口停留时长（秒）
)

-- 按月分表: keyboard_events_202605
keyboard_events (
  id BIGINT PRIMARY KEY,
  session_id UUID NOT NULL REFERENCES activity_sessions(id),
  timestamp TIMESTAMPTZ NOT NULL,
  key_code INTEGER NOT NULL,
  action VARCHAR(10) NOT NULL,                      -- 'press' | 'release'
  key_name VARCHAR(50),                             -- KeyStats 按键名称
  window_title VARCHAR(500),
  process_name VARCHAR(200)
)

-- 按月分表: mouse_events_202605
mouse_events (
  id BIGINT PRIMARY KEY,
  session_id UUID NOT NULL REFERENCES activity_sessions(id),
  timestamp TIMESTAMPTZ NOT NULL,
  event_type VARCHAR(20) NOT NULL,                  -- 'move' | 'click' | 'scroll'
  x INTEGER,
  y INTEGER,
  button VARCHAR(10),                               -- 'left' | 'right' | 'middle' | 'back' | 'forward'
  window_title VARCHAR(500),
  process_name VARCHAR(200)
)

-- 每日汇总（用于仪表盘和热力图）
activity_daily_summary (
  id UUID PRIMARY KEY,
  user_id UUID NOT NULL REFERENCES users(id),
  date DATE NOT NULL,
  total_key_presses INTEGER NOT NULL DEFAULT 0,
  total_left_clicks INTEGER NOT NULL DEFAULT 0,
  total_mouse_distance_meters DECIMAL(10,2) NOT NULL DEFAULT 0,
  active_minutes INTEGER NOT NULL DEFAULT 0,
  top_apps JSONB,                                   -- [{"process":"Code","minutes":120}, ...]
  created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  UNIQUE (user_id, date)
)

-- 日程关联报告
activity_calendar_correlations (
  id UUID PRIMARY KEY,
  user_id UUID NOT NULL REFERENCES users(id),
  event_id UUID REFERENCES events(id),
  event_title VARCHAR(255) NOT NULL,
  scheduled_start TIMESTAMPTZ NOT NULL,
  scheduled_end TIMESTAMPTZ NOT NULL,
  actual_active_minutes INTEGER NOT NULL DEFAULT 0,
  actual_processes JSONB,
  completion_ratio DECIMAL(4,3),                    -- 0.000 ~ 1.000
  generated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
)
```

### 8.5 ER 关系

```
users ──┬── calendars ──┬── events ──── activity_calendar_correlations
        │               └── tasks
        ├── file_items ──┬── file_versions
        │                └── file_tags ─── tags
        ├── sync_roots
        ├── activity_sessions ──┬── window_events
        │                       ├── keyboard_events
        │                       └── mouse_events
        ├── activity_daily_summary
        ├── pending_confirmations
        ├── scheduling_feedback
        ├── outlook_connections
        ├── refresh_tokens
        └── login_attempts
```

---

## 九、客户端架构

### 9.1 共享策略

两端不共享代码，但共享：
- API 契约（端点路径、请求/响应 JSON 格式）
- DTO 字段命名和类型约定
- JWT 存储和刷新流程
- 待确认项同步通知

### 9.2 Windows 客户端 (WPF)

```
Pim.Client.Windows/
├── Pim.Client.App/
│   ├── App.xaml(.cs)
│   ├── MainWindow.xaml(.cs)
│   └── Startup.cs                         # DI 容器配置
├── Pim.Client.Core/
│   ├── Models/
│   ├── Services/
│   │   ├── ApiClient.cs
│   │   ├── AuthService.cs
│   │   └── BackgroundUploader.cs          # 低优先级上传队列
│   ├── Navigation/
│   └── Notifications/
│       └── ToastService.cs                # Windows Toast 通知
├── Pim.Client.Modules/
│   ├── Pim.Client.Calendar/
│   │   ├── ViewModels/
│   │   │   ├── CalendarViewModel.cs
│   │   │   ├── TaskInboxViewModel.cs
│   │   │   └── ScheduleViewModel.cs
│   │   ├── Views/
│   │   │   ├── CalendarView.xaml          # 月/周/日/议程视图
│   │   │   ├── TaskInboxPanel.xaml
│   │   │   └── ScheduleComparison.xaml    # 排程方案对比
│   │   └── Services/
│   │       └── CalendarApiService.cs
│   ├── Pim.Client.Files/
│   │   ├── ViewModels/
│   │   ├── Views/
│   │   │   ├── FileBrowserView.xaml
│   │   │   └── VersionHistoryPanel.xaml
│   │   └── Services/
│   │       ├── FileApiService.cs
│   │       └── SyncEngineService.cs       # FileSystemWatcher + 定时比对
│   └── Pim.Client.Activity/
│       ├── ViewModels/
│       ├── Views/
│       │   ├── TimelineView.xaml
│       │   ├── DashboardView.xaml
│       │   └── HeatmapView.xaml
│       └── Services/
│           ├── KeyStatsCollector.cs       # 轮询 KeyStats API
│           ├── ActivityWatchCollector.cs   # 轮询 ActivityWatch API
│           └── ActivityUploader.cs
└── Pim.Client.Infrastructure/
    ├── Database/
    │   └── LocalDbContext.cs               # SQLite
    └── Configuration/
        └── SyncSettings.cs
```

- **MVVM**: CommunityToolkit.Mvvm
- **DI**: Microsoft.Extensions.DependencyInjection
- **日历控件**: 基于开源 WPF Calendar 控件二次开发
- **文本提取**: 不适用（服务端处理）

### 9.3 Android 客户端 (Kotlin)

```
Pim.Client.Android/
├── app/
│   ├── PimApp.kt
│   └── MainActivity.kt
├── core/
│   ├── models/
│   ├── network/
│   │   ├── ApiService.kt
│   │   └── AuthInterceptor.kt
│   └── auth/
│       └── TokenManager.kt
├── features/
│   ├── calendar/
│   │   ├── ui/                            # Compose Screens
│   │   │   ├── CalendarScreen.kt
│   │   │   └── TaskInboxScreen.kt
│   │   ├── viewmodel/
│   │   └── data/
│   ├── files/
│   │   ├── ui/
│   │   │   ├── FileBrowserScreen.kt
│   │   │   └── VersionHistoryScreen.kt
│   │   └── viewmodel/
│   └── activity/
│       ├── ui/
│       │   ├── TimelineScreen.kt
│       │   ├── DashboardScreen.kt
│       │   └── HeatmapScreen.kt
│       ├── viewmodel/
│       └── data/
│           └── UsageStatsCollector.kt     # PACKAGE_USAGE_STATS
└── infrastructure/
    ├── database/
    │   └── LocalDatabase.kt               # Room
    └── preferences/
```

- **UI**: Jetpack Compose
- **网络**: Retrofit + OkHttp + kotlinx.serialization
- **DI**: Hilt
- **本地存储**: Room (SQLite) + EncryptedSharedPreferences

### 9.4 离线策略

- 日程/文件列表: 缓存优先，本地 5 分钟有效期
- 活动记录: 本地缓冲 + 批量上传，不上传时保留到上传成功
- 离线修改: 草稿存本地 SQLite，恢复网络后同步
- 冲突处理: "最后写入胜出"，时间戳比较

---

## 十、部署架构

### 10.1 整体拓扑

```
┌──────────────────────────────────────────────────────┐
│                自建服务器 / NAS (Docker)                │
│                                                       │
│  ┌───────────────────────────────────────────────┐   │
│  │  nginx (Reverse Proxy)                         │   │
│  │  Port 443, SSL 终止 (Let's Encrypt)            │   │
│  └──────────────┬────────────────────────────────┘   │
│                 │                                     │
│  ┌──────────────┴────────────────────────────────┐   │
│  │  Pim.Api (ASP.NET Core)                        │   │
│  │  容器, 内部端口 5000                            │   │
│  └───┬──────────┬──────────┬─────────────────────┘   │
│      │          │          │                          │
│  ┌───┴────┐ ┌───┴───┐ ┌───┴──────────┐              │
│  │PostgreSQL│ │MinIO  │ │Apache Tika   │              │
│  │  16     │ │9000   │ │  9998        │              │
│  └─────────┘ └───────┘ └──────────────┘              │
│                                                       │
│  ┌───────────────────────────────────────────────┐   │
│  │  ┌───────────────────────────────────────┐    │   │
│  │  │  Kopia Repository (MinIO 内)           │    │   │
│  │  └───────────────────────────────────────┘    │   │
│  └───────────────────────────────────────────────┘   │
│                                                       │
│  ┌───────────────────────────────────────────────┐   │
│  │  备份 (cron)                                   │   │
│  │  pg_dump + minio mirror → 本地 + 异地          │   │
│  └───────────────────────────────────────────────┘   │
└──────────────────────────────────────────────────────┘
```

### 10.2 docker-compose 服务

| 服务 | 镜像 | 端口 (宿主机) |
|------|------|--------------|
| pim-api | 自构建 (Dockerfile) | 5000 (仅内部) |
| postgres | postgres:16 | 5432 (仅内部) |
| minio | minio/minio | 9000, 9001 (仅内部) |
| tika | apache/tika:latest | 9998 (仅内部) |
| nginx | nginx:alpine | 80, 443 |

### 10.3 数据持久化

- PostgreSQL 数据: Docker volume → 宿主机 `/data/pim/postgres`
- MinIO 数据: Docker volume → 宿主机 `/data/pim/minio`
- Kopia 仓库: 通过 MinIO S3 接口，数据在 MinIO 内部
- 备份输出: 宿主机 `/data/pim/backups`

### 10.4 备份策略

- `pg_dump` 每天凌晨 3:00 执行，保留最近 30 天
- MinIO `mc mirror` 每天凌晨 4:00 执行
- Kopia snapshot 自带 dedup 和加密，备份时直接复制 repository 目录

---

## 十一、安全考量

- 所有密码使用 bcrypt (cost factor 12)
- JWT 使用 RS256 非对称签名
- refreshToken 存哈希值，不存明文
- Outlook access_token 使用 AES-256 加密存储
- API 统一异常处理，不泄露内部错误细节
- PostgreSQL 不暴露宿主机端口，仅容器内网访问
- nginx 处理 rate limiting 和请求体大小限制
- MinIO 预签名 URL 用于文件下载，时效 5 分钟
- Kopia 仓库使用加密（通过 Kopia 本身不依赖外部密钥）
- KeyStats / ActivityWatch 仅监听 localhost，数据不外泄

---

## 十二、开发顺序

| 步骤 | 内容 |
|------|------|
| 1 | `Pim.Core` 核心抽象 + IModule + ISearchProvider 接口 |
| 2 | `Pim.Infrastructure` 基础设施 (DbContext, JWT, MinIO, Kopia CLI 封装) |
| 3 | `Pim.Api` 主程序 + 认证端点 + JWT 中间件 + ModuleRegistry + SearchController |
| 4 | Docker 部署配置 (包括 Tika 容器) |
| 5 | `Pim.Module.Calendar` 完整模块 (日历+任务+排程引擎+Ics+Outlook) |
| 6 | Windows 客户端骨架 + 认证 + 日历模块 |
| 7 | Android 客户端骨架 + 认证 + 日历模块 |
| 8 | `Pim.Module.Files` 完整模块 |
| 9 | Windows 客户端文件模块 + SyncEngine |
| 10 | `Pim.Module.Activity` 完整模块 |
| 11 | Windows 客户端活动采集 + 上传 |
| 12 | Android 客户端文件 + 活动模块 |
