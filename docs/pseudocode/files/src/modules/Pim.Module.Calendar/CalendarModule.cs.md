# src/modules/Pim.Module.Calendar/CalendarModule.cs

## 元信息
- 语言：C#
- 程序集或包：Pim.Module.Calendar
- 职责：实现 `IModule`：注册日历相关 DI 服务；映射 `/api/v1/calendar` 全量 HTTP 端点（工作台、数据中心、规划对象、提醒、报告、日历/事件/任务、回收站、排程、ICS、Outlook）。
- 主要依赖：`IModule`、`PimDbContext`、Calendar 各 Service、`ICurrentUserService`、`ISearchProvider`/`CalendarSearchProvider`、实体与 DTO
- 被谁使用：模块加载器/`Program` 在启动时 `RegisterServices` + `MapEndpoints`

## 函数级结构化伪代码

### CalendarModule
#### 属性 Name / Version
- 输入：无
- 输出：`"calendar"` / `"1.0.0"`
- 副作用：无
- 步骤：返回常量
- 分支与异常：无
- 调用：无

#### void RegisterServices(IServiceCollection services, IConfiguration configuration)
- 输入：DI 与配置
- 输出：无
- 副作用：注册程序集模型与 Scoped/Singleton 服务、HttpClient
- 步骤：
  1. `PimDbContext.RegisterModuleAssembly(当前程序集)`
  2. Scoped：CalendarService、IcsService、OutlookIcsService、RecurrenceService、SchedulingEngine、OutlookSyncService、OutlookTokenService、MicrosoftGraphDeviceCodeClient→IMicrosoftGraphClient、OutlookConflictService、CalendarAuditWriter、CalendarDeleteService、CalendarRecycleBinService、PlanningModelService、DataCenterQuery/Governance、ReminderService、ReportService
  3. `AddHttpClient("outlook")`
  4. Singleton：`ISearchProvider` → `CalendarSearchProvider`
- 分支与异常：无
- 调用：DI 扩展方法

#### void MapEndpoints(IEndpointRouteBuilder endpoints)
- 输入：路由构建器
- 输出：无
- 副作用：注册需授权的大量最小 API
- 步骤（按路由组）：
  1. 组 `CalendarEndpointPaths.Root` + `RequireAuthorization`
  2. **工作台/数据中心**：GET layers；POST data-center query/batch preview|request-confirmation|execute；GET audit/export；POST restore preview|request-confirmation
  3. **规划对象**：projects/task-books/habits/availability/ai-placeholders CRUD 风格；任务 checklist；segments CRUD
  4. **提醒**：list/create/snooze/dismiss/actions/delivery-log
  5. **报告**：list/generate/get/archive；suggestions request-action → ReportService
  6. **日历**：list/create/update；delete-preview/delete；restore
  7. **事件**：list（无筛选走全量 GetEvents，否则分页）；create/update/delete/restore；batch-delete
  8. **任务**：list（简单 vs 分页）；create/move/plan/update/delete/restore；batch-delete/update
  9. **回收站**：list；restore-preview/restore
  10. **排程**：POST schedule → SchedulingEngine + 当前用户
  11. **ICS**：POST import-ics（multipart file + 可选 calendarId → ImportOutlookIcs）；GET export-ics（时间窗/可选 ids 过滤 → IcsService.ExportEvents 文件下载）
  12. **Outlook**：settings get/put；device-code + poll；sync batches + sync；events list/batch-tag/pause-sync/stop-sync-preview/stop-sync/history（部分直查 PimDbContext）
- 分支与异常：import 缺 form/file → 400；outlook 事件不存在 → 404；其它委托服务抛出
- 调用：PlanningModelService、DataCenter*、ReminderService、ReportService、CalendarService、CalendarDeleteService、CalendarRecycleBinService、SchedulingEngine、OutlookIcsService、IcsService、OutlookSyncService、OutlookConflictService、PimDbContext、ICurrentUserService

#### Task InitializeAsync(IServiceProvider)
- 输入：服务提供者
- 输出：已完成 Task
- 副作用：无
- 步骤：`await Task.CompletedTask`
- 分支与异常：无
- 调用：无

### CalendarEndpointPaths
#### 路径常量与辅助方法
- 输入：部分方法需 id/type
- 输出：字符串路径
- 副作用：无
- 步骤：Root=`/api/v1/calendar` 及回收站/批删/ICS/数据中心/Outlook/报告等常量；`TaskPlan`/`RecycleRestore*` 拼接
- 分支与异常：无
- 调用：无

## 近逐行中文伪代码

1. 引入 Reflection、ASP.NET Core、EF、Configuration、DI、Core Audit/Common/Modules/Operations、Auth、Data、Calendar DTO/Entities/Search/Services
2. 类 `CalendarModule : IModule`，Name/Version
3. `RegisterServices`：注册模块程序集到 DbContext；注册全部日历 Scoped 服务与 outlook HttpClient；搜索提供方单例
4. `MapEndpoints`：授权组下挂工作台、数据中心治理、规划模型、提醒、报告、日历事件任务 CRUD、回收站、排程、ICS 导入导出、Outlook 连接同步与冲突
5. 事件/任务列表：无高级查询参数时走旧版无分页 API，否则分页
6. ICS 导入：校验 multipart 与 file 字段；读全文；可选 calendarId；调 CalendarService.ImportOutlookIcsAsync
7. ICS 导出：取事件实体，可选 ids 过滤，IcsService 导出 UTF-8 文件
8. Outlook 事件批打标/暂停同步等直接改 Source 字段并 SaveChanges
9. `InitializeAsync` 空实现
10. 静态 `CalendarEndpointPaths` 汇总路径常量

## 关系边
```json
{
  "nodes": [
    {
      "id": "src/modules/Pim.Module.Calendar/CalendarModule.cs",
      "label": "CalendarModule",
      "path": "src/modules/Pim.Module.Calendar/CalendarModule.cs",
      "doc": "docs/pseudocode/files/src/modules/Pim.Module.Calendar/CalendarModule.cs.md",
      "layer": "module.calendar",
      "kind": "entrypoint"
    }
  ],
  "edges": [
    { "from": "src/modules/Pim.Module.Calendar/CalendarModule.cs", "to": "Pim.Core.Modules.IModule", "type": "implements" },
    { "from": "src/modules/Pim.Module.Calendar/CalendarModule.cs", "to": "src/Pim.Infrastructure/Data/PimDbContext.cs", "type": "calls" },
    { "from": "src/modules/Pim.Module.Calendar/CalendarModule.cs", "to": "src/modules/Pim.Module.Calendar/Services/IcsService.cs", "type": "calls" },
    { "from": "src/modules/Pim.Module.Calendar/CalendarModule.cs", "to": "src/modules/Pim.Module.Calendar/Services/ReportService.cs", "type": "calls" },
    { "from": "src/modules/Pim.Module.Calendar/CalendarModule.cs", "to": "src/modules/Pim.Module.Calendar/Services/CalendarService.cs", "type": "calls" },
    { "from": "src/modules/Pim.Module.Calendar/CalendarModule.cs", "to": "src/modules/Pim.Module.Calendar/Services/PlanningModelService.cs", "type": "calls" },
    { "from": "src/modules/Pim.Module.Calendar/CalendarModule.cs", "to": "src/modules/Pim.Module.Calendar/Services/OutlookSyncService.cs", "type": "calls" },
    { "from": "src/modules/Pim.Module.Calendar/CalendarModule.cs", "to": "src/modules/Pim.Module.Calendar/Services/ReminderService.cs", "type": "calls" },
    { "from": "src/modules/Pim.Module.Calendar/CalendarModule.cs", "to": "src/modules/Pim.Module.Calendar/Services/SchedulingEngine.cs", "type": "calls" },
    { "from": "src/modules/Pim.Module.Calendar/CalendarModule.cs", "to": "src/modules/Pim.Module.Calendar/Search/CalendarSearchProvider.cs", "type": "depends_on" }
  ]
}
```
