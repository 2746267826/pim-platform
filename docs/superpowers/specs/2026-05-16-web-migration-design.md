# PIM Web 迁移设计文档

> **目标:** 将 PIM 客户端从 WPF + Android 原生 UI 迁移至 React Web 前端 + 本地守护程序架构，解决 WPF UI 稳定性问题并统一跨平台体验。

**设计日期:** 2026-05-16

**核心变化:**
- WPF 客户端删除所有 UI，保留为系统托盘守护程序（数据采集 + 上传）
- Android 客户端删除所有 Compose UI，保留为后台采集 Service
- 新增 React + TypeScript 前端，打包内嵌入 ASP.NET Core wwwroot
- 所有平台通过浏览器访问统一的 Web UI

---

## 一、架构总览

```
┌─ 浏览器 (Desktop + Mobile) ──────────────────┐
│                                                │
│  React SPA (TypeScript)                        │
│  ├─ 日历视图 (FullCalendar)                    │
│  ├─ 任务面板 + 收集箱                          │
│  └─ 文件浏览 / 活动视图 (后续)                 │
│         │                                      │
│         │ REST API (JWT)                       │
└─────────┼──────────────────────────────────────┘
          │
┌─────────┴──────────────────────────────────────┐
│  NAS / Docker                                  │
│  ┌─────────────────────────────────────────┐  │
│  │  nginx → Pim.Api (ASP.NET Core)         │  │
│  │  ├─ /wwwroot/ (React 打包产物)           │  │
│  │  ├─ /api/v1/auth/*                      │  │
│  │  ├─ /api/v1/calendar/*                  │  │
│  │  └─ ...                                 │  │
│  └─────────────────────────────────────────┘  │
└────────────────────────────────────────────────┘
          ▲                    ▲
          │ REST API           │ REST API
          │ (数据上报)         │ (数据上报)
┌─────────┴──────┐  ┌─────────┴──────┐
│ Windows 守护程序 │  │ Android 守护进程 │
│ (WPF 精简化)    │  │ (后台 Service)   │
│ ├─ KeyStats 采集 │  │ └─ UsageStats   │
│ ├─ AW 采集       │  │    采集 + 上报  │
│ ├─ 文件同步      │  │                 │
│ └─ 系统托盘状态   │  │                 │
└────────────────┘  └─────────────────┘
```

---

## 二、Web 前端 (React)

### 技术栈

| 用途 | 选型 |
|------|------|
| 框架 | React 18 + TypeScript |
| 构建 | Vite（生产输出到 `Pim.Api/wwwroot/`） |
| 路由 | React Router v6 |
| 日历核心 | @fullcalendar/core + @fullcalendar/react + @fullcalendar/daygrid + @fullcalendar/timegrid + @fullcalendar/interaction |
| UI 组件 | shadcn/ui (Radix UI + Tailwind CSS) |
| 状态管理 | React Context + @tanstack/react-query |
| 表单 | react-hook-form + zod |
| HTTP | fetch 封装，JWT 存 localStorage |
| 通知 | Service Worker + Web Notification API |

### 路由结构

```
/               → 重定向到 /timeline
/timeline       → 时间轴（日视图）
/week           → 周视图
/month          → 月视图
/tasks          → 任务列表
/settings       → 设置页
```

### 组件树

```
AppLayout
├─ Sidebar
│   ├─ NavButtons (时间轴 / 本周 / 月视图 / 任务)
│   └─ CalendarBooks (日历本列表)
├─ ContentArea (React Router Outlet)
│   ├─ TimelinePage        ← FullCalendar timeGridDay
│   ├─ WeekPage            ← FullCalendar timeGridWeek
│   ├─ MonthPage           ← FullCalendar dayGridMonth
│   └─ TaskListPage        ← 手写任务列表
├─ InboxPanel
│   ├─ InboxTaskCard[]
│   └─ ActionButtons
├─ EventEditorDialog       ← 模态框
└─ TaskEditorDialog        ← 模态框
```

### FullCalendar 集成

- `eventContent`: 颜色条 + 标题 + 时间
- `dateClick`: 打开新建事件对话框
- `eventClick`: 打开编辑事件对话框
- `eventDrop` / `eventResize`: 调用 PUT API 更新时间
- `datesSet`: 用户切换范围时自动请求对应区间数据
- 月视图点击日期 → 底部预览面板联动

### 数据流

```
react-query cache
  ├─ useQuery(["events", start, end]) → GET /api/v1/calendar/events?start=&end=
  ├─ useQuery(["tasks"])             → GET /api/v1/calendar/tasks
  ├─ useQuery(["calendars"])         → GET /api/v1/calendar/calendars
  ├─ useMutation("createEvent")      → POST /api/v1/calendar/events
  └─ useMutation("updateTask")       → PUT /api/v1/calendar/tasks/{id}
```

---

## 三、Windows 守护程序

### 删除清单

| 删除 | 原因 |
|------|------|
| `Views/` 全部 (9 个文件) | UI 已迁到 React |
| `ViewModels/` 全部 (8 个文件) | ViewModel 不再需要 |
| `Converters/Converters.cs` | XAML 绑定已无 |
| `Styles/Theme.xaml` | 主题已无 |
| `MainWindow.xaml(.cs)` | 无主窗口 |
| `App.xaml` 中 MaterialDesign 引用 | 无 Material Design 依赖 |
| NuGet: MaterialDesignThemes | 不再使用 |

### 保留清单

| 保留 | 用途 |
|------|------|
| `Pim.Client.Core/Services/ApiClient.cs` | HTTP 请求 |
| `Pim.Client.Core/Services/AuthService.cs` | JWT 认证 |
| `Pim.Client.Core/Models/` | DTO 定义 |
| `Pim.Client.Infrastructure/` | SQLite、配置 |

### 新增文件

```
src/client-windows/Pim.Client.App/
├── App.xaml(.cs)              # 精简：初始化 DI + 启动守护
├── TrayIcon.cs                # 系统托盘 + 右键菜单
├── StatusWindow.xaml(.cs)     # 双击托盘弹出的状态窗口
├── HostedServices/
│   ├── KeyStatsCollector.cs       # 轮询 KeyStats API
│   ├── ActivityWatchCollector.cs  # 轮询 ActivityWatch API
│   ├── BackgroundUploader.cs      # 批量上传管理
│   └── SyncEngineService.cs       # 文件同步 (V2)
└── Startup.cs                  # DI 容器（精简版）
```

### 托盘菜单

```
┌──────────────┐
│ PIM 采集服务   │ (灰显)
│ ──────────── │
│ 状态: 运行中   │ (绿点)
│ 今日已上传 12条 │
│ ──────────── │
│ 打开状态窗口   │
│ 手动同步       │
│ ──────────── │
│ 退出          │
└──────────────┘
```

### StatusWindow

```
┌─────────────────────────────┐
│ PIM 数据采集状态             │
│ ─────────────────────────── │
│ KeyStats      ✓ 已连接      │
│ ActivityWatch ✓ 已连接      │
│ 上传队列      12 条待上传    │
│ 上次上传      3 分钟前       │
│ ─────────────────────────── │
│ [手动同步] [查看日志]        │
└─────────────────────────────┘
```

### 启动方式

- 安装后写入注册表 `HKCU\Software\Microsoft\Windows\CurrentVersion\Run`
- 开机自动启动，默认隐藏窗口，仅托盘可见

---

## 四、Android 守护进程

### 删除

- 所有 Compose Screen (`CalendarScreen`, `TaskInboxScreen`, `FileBrowserScreen` 等)
- 所有 ViewModel

### 保留

| 保留 | 用途 |
|------|------|
| `core/network/` (ApiService, AuthInterceptor) | HTTP + JWT |
| `core/auth/TokenManager.kt` | Token 管理 |
| `infrastructure/database/` (Room) | 本地缓冲 |
| `features/activity/data/UsageStatsCollector.kt` | 采集 |
| `core/models/` | DTO |

### 新增

```
features/daemon/
├── PimDaemonService.kt       # Foreground Service
├── DataCollector.kt          # UsageStats 定时采集 (每 5 分钟)
├── UploadWorker.kt           # WorkManager 周期上传 (每 15 分钟)
└── StatusActivity.kt         # 点击通知进入的状态页
```

### 前台通知

```
┌──────────────────────────────┐
│ PIM 数据采集                  │
│ 采集运行中                    │
│ 待上传: 5 条                  │
│ 上次上传: 10 分钟前            │
└──────────────────────────────┘
```

### 采集策略

- 5 分钟采集一次 → Room 本地存储 → 标记 `synced = false`
- WorkManager 每 15 分钟触发上传 → `NetworkType.CONNECTED` 约束
- 失败自动重试（指数退避：15s, 30s, 60s）

---

## 五、后端调整

### 新增

| 调整 | 说明 |
|------|------|
| `app.UseDefaultFiles()` | 请求 `/` → `index.html` |
| `app.UseStaticFiles()` | Serve `wwwroot/` |
| `app.MapFallbackToFile("index.html")` | SPA fallback |
| CORS（开发环境） | `localhost:5173` 允许跨域 |
| Serilog 请求中间件 | 记录请求路径、状态码、耗时 |

### 不变

- 所有 `/api/v1/*` 端点
- 认证中间件、JWT 逻辑
- 模块注册
- 数据库、MinIO、Kopia、Tika
- Docker Compose 部署结构

---

## 六、构建与部署

### 开发

```bash
# 前端开发服务器
cd src/client-web
npm run dev          # localhost:5173，代理 API 到 localhost:5000

# 后端
dotnet run --project src/Pim.Api
```

### 生产构建

```bash
# 前端构建 → wwwroot
cd src/client-web
npm run build        # 输出到 ../Pim.Api/wwwroot/

# 后端发布
dotnet publish src/Pim.Api -c Release -o publish
docker build -t pim-api .
```

### Docker Compose 保持不变

前端静态文件随 `Pim.Api` 容器一起打包，不需要独立的 nginx serve 前端逻辑。

---

## 七、日志策略

### 三层日志

| 层 | 技术 | 内容 |
|-----|------|------|
| Web 前端 | `console.log` (开发) + 错误边界 | API 调用错误、渲染异常 |
| Windows 守护 | Serilog → 文件 (JSON Lines) | 采集心跳、上传批次、API 响应码、异常堆栈 |
| Android 守护 | Timber → 文件 | 采集周期、上传状态、权限变化 |
| 后端 | Serilog → 文件 + Console | 所有请求 (路径/耗时/状态码)、认证事件、采集接收量 |

### 日志文件配置

```
Windows: %LOCALAPPDATA%/PIM/logs/pim-daemon-{Date}.log
Android: /data/data/{app}/files/logs/pim-daemon-{Date}.log
Server:  /data/pim/logs/pim-api-{Date}.log

保留: 30 天滚动
级别: DEBUG (开发) / INFO (生产)
```

### Windows 守护日志覆盖点

- 每次采集轮询开始/结束（源、耗时、获取条数）
- 每次 HTTP 请求（URL、方法、状态码、耗时）
- 上传批次（批量大小、压缩比、HTTP 状态）
- Token 刷新事件
- 采集源连接失败（KeyStats/AW 不可达）
- 所有未捕获异常（完整堆栈）

### 后端日志覆盖点

- 所有 HTTP 请求（路径、方法、状态码、耗时、客户端 IP）
- 认证事件（登录成功/失败、Token 刷新、锁定触发）
- 采集接收（POST /api/v1/activity/ingest 的条目数和来源设备）
- 业务异常（错误码 + 详情）
- 外部服务调用（MinIO、Kopia、Tika 耗时和状态）

---

## 八、实施顺序

| 阶段 | 内容 | 依赖 |
|------|------|------|
| 1 | React 项目骨架 + Vite + 路由 + API client | 无 |
| 2 | FullCalendar 集成 + 月/周/日三视图 | 阶段 1 |
| 3 | 任务列表 + 收集箱面板 | 阶段 1 |
| 4 | 事件/任务编辑对话框 | 阶段 2, 3 |
| 5 | 后端 SPA fallback + wwwroot 构建集成 | 阶段 1 |
| 6 | Windows 守护程序（精简 WPF） | 阶段 5 |
| 7 | Android 守护进程（精简原生） | 阶段 5 |
| 8 | 日志完整接入（三层） | 阶段 6, 7 |
| 9 | 端到端测试 + 部署验证 | 阶段 5-8 |
