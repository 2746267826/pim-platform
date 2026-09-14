# PIM 项目地图

> 普通仓库导航文件，帮助快速了解系统组成和改动位置。不作为审批材料。
>
> **状态含义：**
> - **working** — 入口或模块已注册，主要数据路径有真实实现；不表示所有功能都已完整验收
> - **partial** — 当前描述的表面只有部分数据路径接通
> - **not-wired** — 文件或目录存在，但正常构建/入口没有引用，或 UI 没有调用真实数据源

## 系统入口

| 表面 | 路径 | 职责 | 证据 |
|------|------|------|------|
| **Pim.Api** (ASP.NET Minimal API) | `src/Pim.Api/Program.cs` | HTTP 入口、中间件管线、模块发现、SPA 托管 | `Program.cs` 调用 `AddPimInfrastructure` + `AddPimAuth` + `ModuleRegistry.DiscoverModules` 扫描 `Pim.Module.*.dll`；末尾 SPA 回退 |
| **client-web** | `src/client-web/src/main.tsx` → `<BrowserRouter>` + `<QueryClientProvider>` + `<App>` | React SPA 入口，TanStack Query 数据获取 | `App.tsx` 含认证守卫，`AppLayout.tsx` 注册工作台路由 |
| **client-windows** | `src/client-windows/Pim.Client.App/App.xaml.cs` → `Startup.cs` | WPF 应用入口，DI 容器、系统托盘、WebView2 Shell | `Startup.cs` 注入 `ApiClient`、`AuthService`，`EmbeddedWebViewHost` 承载 Web 前端 |
| **client-android** | `src/client-android/app/src/main/java/com/pim/app/PimApp.kt` (@HiltAndroidApp) → `MainActivity.kt` → `PimRootScreen.kt` | Android 入口，Hilt DI，Compose 导航 | `PimRootScreen.kt` 5 个底部标签（Today/Tracks/Schedule/Status/Settings） |

## 后端层

| 层 | 路径 | 职责 |
|----|------|------|
| **Pim.Core** | `src/Pim.Core/` | DTO、接口、枚举、异常；含 `IModule`、`ISearchProvider`、`ITodaySectionProvider`、`IAiGateway` 等约定 |
| **Pim.Infrastructure** | `src/Pim.Infrastructure/` | EF Core + Npgsql 持久化、JWT 认证、Hangfire 后台作业、MinIO 存储、Tika 文本提取、AI 网关、操作确认/审计 |
| **Pim.Api** | `src/Pim.Api/` | Minimal API 端点注册、中间件、模块发现（`ModuleRegistry.DiscoverModules`）、SPA 静态文件 |

## 后端模块

| 模块 | 路径 | 职责 | 路由前缀 | 状态 | 状态证据 |
|------|------|------|----------|------|----------|
| **Calendar** | `src/modules/Pim.Module.Calendar/` | 日历事件、任务/习惯/计划、Outlook 同步、ICS 导入导出、数据中心查询/治理、提醒/通知 | `/api/v1/calendar` | **working** | `CalendarModule.cs` 注册端点；`Pim.Api.csproj` 引用；测试在 `tests/Pim.UnitTests/Calendar/` |
| **PcTracker** | `src/modules/Pim.Module.PcTracker/` | PC 活动追踪（AW/Keystats）、活动分类、应用知识库、分类规则、活动仪表板 | `/api/v1/pc` | **working** | `PcTrackerModule.cs` 注册端点；`Pim.Api.csproj` 引用；相关测试位于 `tests/Pim.UnitTests/` |
| **Mobile** | `src/modules/Pim.Module.Mobile/` | 移动端使用数据接入、位置追踪、质量与分析查询 | `/api/v1/mobile` | **working** | `MobileModule.cs` 注册端点；`Pim.Api.csproj` 引用；测试在 `tests/Pim.UnitTests/Mobile/` |
| **Files** | `src/modules/Pim.Module.Files/` | 文件提供者（Nextcloud WebDAV）、文件管理（CRUD/同步/版本）、AI 索引/搜索、向量存储（Qdrant） | `/api/v1/files` | **working** | `FilesModule.cs` 注册端点；`Pim.Api.csproj` 引用；测试在 `tests/Pim.UnitTests/Files/` |
| **QuickNotes** | `src/modules/Pim.Module.QuickNotes/` | 快速笔记 CRUD、归档/恢复、附件（MinIO） | `/api/v1/quick-notes` | **working** | `QuickNotesModule.cs` 注册端点；`Pim.Api.csproj` 引用；测试在 `tests/Pim.UnitTests/QuickNotes/` |
| **Stats** | `src/modules/Pim.Module.Stats/` | 应用使用统计批量上传，并清理 30 天前的记录 | `/api/v1/stats` | **not-wired** | 模块和解决方案项目存在，但 `Pim.Api.csproj` 未引用；正常 API 构建不会把它作为模块依赖带入输出 |

## 客户端

### Web 前端 (React + TypeScript + Vite + Tailwind)

| 关注点 | 路径 | 说明 |
|--------|------|------|
| 主入口 | `src/client-web/src/main.tsx` | BrowserRouter + QueryClientProvider + App |
| 路由布局 | `src/client-web/src/layout/AppLayout.tsx` | 工作台 `<Route>` 与侧边栏导航 |
| API 适配器 | `src/client-web/src/api/client.ts` | `apiGet/apiPost/...` + 401 自动刷新 token |
| Dev server | `src/client-web/vite.config.ts` | 端口 5173，`/api` → `localhost:5858` 代理 |
| 测试 | `tests/client-web/` | 按功能域拆分的 TypeScript/TSX 测试与 tsconfig |

### Windows 客户端 (WPF + WebView2)

| 关注点 | 路径 | 说明 |
|--------|------|------|
| 应用入口 | `src/client-windows/Pim.Client.App/App.xaml.cs` | `OnStartup` → DI + 系统托盘 + 主窗口 |
| DI 配置 | `src/client-windows/Pim.Client.App/Startup.cs` | 注入 `ApiClient`、`AuthService` 等 |
| API 客户端 | `src/client-windows/Pim.Client.Core/Services/ApiClient.cs` | base URL `http://127.0.0.1:5858/api/v1/`，Bearer 认证 |
| WebView2 Shell | `src/client-windows/Pim.Client.App/EmbeddedWebViewHost.cs` | 工作台功能通过此组件加载 Web 前端 |
| 默认地址 | `src/client-windows/Pim.Client.Core/ClientDefaults.cs` | 默认服务器 URL: `http://127.0.0.1:5858` |

### Android 客户端 (Kotlin + Jetpack Compose + Hilt)

| 关注点 | 路径 | 说明 |
|--------|------|------|
| 应用入口 | `src/client-android/app/src/main/java/com/pim/app/PimApp.kt` | `@HiltAndroidApp` |
| Activity | `src/client-android/app/src/main/java/com/pim/app/MainActivity.kt` | `@AndroidEntryPoint` → `PimRootScreen()` |
| 导航壳 | `src/client-android/app/src/main/java/com/pim/app/ui/root/PimRootScreen.kt` | 5 标签底部导航（`PimDestination`） |
| API 接口 | `src/client-android/core/src/main/java/com/pim/core/network/ApiService.kt` | Retrofit2 接口定义 |
| 构建 | `src/client-android/core/build.gradle.kts` 等 | Hilt + Retrofit2 + OkHttp + kotlinx-serialization |

## 共享能力

| 能力 | 实现路径 | 使用者 |
|------|----------|--------|
| **EF Core + Npgsql** | `src/Pim.Infrastructure/Data/PimDbContext.cs` | 所有模块的实体和数据库通信 |
| **JWT 认证** | `src/Pim.Infrastructure/Auth/JwtService.cs` + `src/Pim.Infrastructure/Extensions/AuthExtensions.cs` | 除 `AllowAnonymous` 外的所有 `/api/v1/*` |
| **Hangfire 后台作业** | `src/Pim.Infrastructure/Extensions/ServiceCollectionExtensions.cs` | `Stage0DiagnosticJob`、循环注册 |
| **操作确认/审计** | `src/Pim.Core/Operations/` + `src/Pim.Infrastructure/Operations/` | Calendar（治理操作）、PcTracker（App 知识库构建） |
| **Today 聚合** | `src/Pim.Api/Today/` + `src/Pim.Core/Today/TodayDtos.cs` | Calendar（日程/任务/习惯/提醒/AI）、PcTracker（活动/系统）、Operations（确认/审计） |
| **跨模块搜索** | `src/Pim.Core/Modules/ISearchProvider.cs` + `src/Pim.Api/Search/SearchEndpoints.cs` | Calendar 为唯一已注册搜索提供者 |
| **MinIO 对象存储** | `src/Pim.Infrastructure/Storage/` | QuickNotes（附件） |
| **Tika 文本提取** | `src/Pim.Infrastructure/TextExtraction/` | Files 模块 |
| **外部: Outlook/Graph** | `src/modules/Pim.Module.Calendar/Services/` | Calendar 模块（OAuth 设备码、同步、写回） |
| **外部: Nextcloud** | `src/modules/Pim.Module.Files/` | Files 模块（WebDAV 文件操作） |
| **外部: Qdrant 向量库** | `src/modules/Pim.Module.Files/` | Files 模块（AI 文件搜索） |
| **AI 网关** | `src/Pim.Core/Ai/` + `src/Pim.Infrastructure/Ai/` | Files（AI 文件索引）、Calendar（AI 占位端点） |

## 关键数据流

### 1. 移动数据上传
```
[Android App] → POST /api/v1/mobile/usage/events (MobileUsageEventsUploadRequest)
  → MobileModule.cs 端点处理器
  → MobileUsageIngestService.IngestAsync()
  → 验证 + 写入 PimDbContext.Set<MobileSessionEntity>() / EventEntity
  → SaveChangesAsync()
```
真实路径：`src/modules/Pim.Module.Mobile/MobileModule.cs` 注册端点，`Services/MobileUsageIngestService.cs` 处理写入。

### 2. 日历 + Outlook 同步
```
[Client] → GET/POST /api/v1/calendar/events (CalendarService)
  → PimDbContext.Set<EventEntity>() → PostgreSQL

[Outlook sync] → POST /api/v1/calendar/outlook/sync (OutlookSyncService.SyncAsync)
  → OutlookTokenService (device code OAuth)
  → MicrosoftGraphDeviceCodeClient (/delta 查询)
  → 合并至 EventEntity + 冲突检测 (OutlookConflictService)
  → PimDbContext SaveChanges

[ICS import] → POST /api/v1/calendar/import-ics
  → OutlookIcsService (Ical.Net 解析)
  → CalendarService.ImportOutlookIcsAsync → 写入 EventEntity
```
真实路径：`src/modules/Pim.Module.Calendar/Services/CalendarService.cs`、`OutlookSyncService.cs`、`MicrosoftGraphDeviceCodeClient.cs`。

### 3. Today 聚合
```
[Client] → GET /api/v1/today/sections (TodaySectionService)
  → 聚合 Program.cs 中注册的 ITodaySectionProvider
  → CalendarScheduleTodaySectionProvider → CalendarService
  → PcActivityTodaySectionProvider → PcTrackerService
  → OperationsHealthTodaySectionProvider → SystemStatusService
  → 返回 TodaySectionRegistryDto

[Client] → GET /api/v1/today/sections/{sectionId}
  → 对应 ITodaySectionProvider.BuildAsync
```
真实路径：`src/Pim.Api/Today/TodaySectionService.cs`，各 Provider 在 `Program.cs` 注册。

## 要改什么去哪里

| 改动 | 位置 | 相关测试 |
|------|------|----------|
| Web 日历视图/编辑 | `src/client-web/src/pages/CalendarPage.tsx`, `src/client-web/src/api/calendar.ts` | `tests/client-web/calendarApiPath.test.ts` |
| Web 同步页面 | `src/client-web/src/pages/SyncPage.tsx`, `src/client-web/src/api/calendar.ts` | `tests/client-web/outlookSyncInvalidation.test.ts` |
| Web PC 追踪 | `src/client-web/src/pages/PcTrackerPage.tsx`, `src/client-web/src/api/pcTracker.ts` | `tests/client-web/pcRoute3ApiPath.test.ts` |
| Web Today | `src/client-web/src/pages/TodayPage.tsx`, `src/client-web/src/api/today.ts` | `tests/client-web/todayApiPath.test.ts` |
| Web 路由/导航 | `src/client-web/src/layout/AppLayout.tsx` | — |
| Web API 客户端 | `src/client-web/src/api/client.ts` | — |
| 后端模块/端点 | `src/modules/Pim.Module.*/` | `tests/Pim.UnitTests/` |
| 后端认证 | `src/Pim.Infrastructure/Auth/` | — |
| 后端共享基础设施 | `src/Pim.Infrastructure/` | `tests/Pim.UnitTests/Operations/` |
| 默认端口/地址 | `src/client-windows/Pim.Client.Core/ClientDefaults.cs`, `src/client-web/vite.config.ts`, `src/Pim.Api/appsettings.json`, `src/Pim.Api/appsettings.Development.json` | — |
| Windows 认证 | `src/client-windows/Pim.Client.App/LoginWindow.xaml`, `src/client-windows/Pim.Client.App/LoginWindow.xaml.cs`, `src/client-windows/Pim.Client.Core/Services/AuthService.cs` | `tests/Pim.UnitTests/ClientWindows/ApiClientDefaultsTests.cs` |
| Windows 数据采集 | `src/client-windows/Pim.Client.Core/Services/AwCollectorService.cs`, `src/client-windows/Pim.Client.Core/Services/KeyStatsCollectorService.cs` | `tests/Pim.UnitTests/ClientWindows/WindowsCompanionShellTests.cs` |
| Windows WebView Shell | `src/client-windows/Pim.Client.App/EmbeddedWebViewHost.cs` | `tests/Pim.UnitTests/ClientWindows/WindowsCompanionShellTests.cs` |
| Android 位置采集 | `src/client-android/app/src/main/java/com/pim/app/location/` | `src/client-android/app/src/test/java/com/pim/app/location/policy/LocationPolicyEngineTest.kt` |
| Android 同步 | `src/client-android/app/src/main/java/com/pim/app/mobile/sync/` | `src/client-android/app/src/test/java/com/pim/app/mobile/sync/MobileSyncWindowSplitterTest.kt` |
| Android 状态中心 | `src/client-android/app/src/main/java/com/pim/app/ui/status/StatusCenterScreen.kt` | `src/client-android/app/src/test/java/com/pim/app/status/StatusIssueTest.kt` |
| Android 导航壳 | `src/client-android/app/src/main/java/com/pim/app/ui/root/PimRootScreen.kt` | `src/client-android/app/src/test/java/com/pim/app/v2/AndroidV2NativeShellTest.kt` |
| Android API 接口 | `src/client-android/core/src/main/java/com/pim/core/network/ApiService.kt` | `src/client-android/core/src/test/java/com/pim/core/network/ApiServiceConverterTest.kt` |
| GitHub Actions（仅 CI/release 任务） | `.github/workflows/*.yml` | — |

## 验证命令

| 命令 | 用途 |
|------|------|
| `dotnet restore Pim.sln` | 还原所有 NuGet 包 |
| `dotnet test Pim.sln` | 运行后端全部测试 |
| `npm --prefix src/client-web run build` | 构建 Web 前端（tsc + vite） |
| `npm --prefix src/client-web run test:today` | 运行 Today 相关 Web 测试 |
| `npm --prefix src/client-web run test:files` | 运行 Files 相关 Web 测试 |
| `npm --prefix src/client-web run test:schedule-workbench-complete` | 运行排程工作台完整测试集 |
| `dotnet build src/client-windows/Pim.Client.App/Pim.Client.App.csproj` | 构建 Windows 客户端 |
| `dotnet test tests/Pim.UnitTests/Pim.UnitTests.csproj --filter "FullyQualifiedName~WindowsCompanionShellTests|FullyQualifiedName~WindowsNotificationActionRouterTests|FullyQualifiedName~ApiClientDefaultsTests"` | 运行 CI 使用的 Windows 相关测试 |
| `cd src\client-android && .\gradlew.bat :app:testDebugUnitTest` | 运行 Android 单元测试 |
| `cd src\client-android && .\gradlew.bat :app:assembleDebug` | 构建 Android 调试 APK |

默认构建目录：`C:\pim-lg`

## 已知状态提醒

- **Android TodayScreen** `src/client-android/app/src/main/java/com/pim/app/ui/today/TodayScreen.kt`：`TodayViewModel` 只提供采集/API 状态标签；地图、停留次数、距离、手机使用和策略正文仍是固定文案 → **partial**。
- **Android TracksScreen** `src/client-android/app/src/main/java/com/pim/app/ui/tracks/TracksScreen.kt`：UI 仅含占位文本（`AssistChip(onClick={})` 为空，无 ViewModel 或 API 调用）。底层 API 接口已在 `ApiService.kt` 定义但 UI 未连接 → **not-wired**。
- **Android SchedulePolicyScreen** `src/client-android/app/src/main/java/com/pim/app/ui/schedule/SchedulePolicyScreen.kt`：无 ViewModel 或 API 调用，当前日程、即将到来和策略切换均为固定说明文字 → **not-wired**。
- **Android features/calendar** `src/client-android/features/calendar/`：模块目录存在构建配置，但无 Kotlin 源文件，且原生五标签导航未接入该模块 → **not-wired**。仓库另有 `PimShellActivity` 承载 Web 路由，不应据此把原生日历模块标为 working。
- **Windows 工作台形态**：认证、采集、状态和托盘包含原生实现；日历、文件、PC 追踪等工作台页面由 `EmbeddedWebViewHost` (WebView2) 加载 Web 前端。这是混合架构事实，不因使用 WebView 自动标记为 partial。
- **Stats 模块** `src/modules/Pim.Module.Stats/`：`Pim.Api.csproj` 未包含该项目引用，正常 API 构建不会加载该模块 → **not-wired**。

## 维护规则

- 只在下述内容变化时同步更新此文件：系统入口、后端层职责边界、共享能力增删、模块状态、默认服务地址/端口、验证命令。
- 不在以下情况要求更新：不改变入口、边界、状态、默认值或验证方式的模块内部重构和普通文档修改。
- 此文件不作为提交阻断或审批材料。不存在"地图未更新不得提交"的阻断规则。
