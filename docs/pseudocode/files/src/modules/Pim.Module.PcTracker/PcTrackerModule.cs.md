# src/modules/Pim.Module.PcTracker/PcTrackerModule.cs

## 元信息
- 语言：C#
- 程序集或包：Pim.Module.PcTracker
- 职责：实现 `IModule`：注册 PC Tracker 服务、映射 `/api/v1/pc*` 读写端点（上传、汇总、分类、App 知识库、分类树、生产力），并在启动时初始化 Schema 与分类种子。
- 主要依赖：`IModule`、`PimDbContext`、`ApiResponse`、PcTracker Services/DTOs、ASP.NET Minimal APIs
- 被谁使用：`ModuleRegistry` / 宿主 `Program` 模块加载

## 函数级结构化伪代码

### PcTrackerModule
#### 属性 `Name` / `Version`
- 输入：无
- 输出：`"pctracker"` / `"1.0.0"`
- 副作用：无
- 步骤：模块标识
- 分支与异常：无
- 调用：无

#### `RegisterServices(services, configuration)`
- 输入：DI 与配置
- 输出：无
- 副作用：注册程序集到 DbContext；AddScoped 十余个服务
- 步骤：
  1. `PimDbContext.RegisterModuleAssembly(ExecutingAssembly)`
  2. 注册：PcTrackerService、Quality、Suggestion、Snapshot、Recompute、Settings、TimelineSmoothing、ClassificationRule、ClassificationRuleDraft、SchemaInitializer、AppSignature、AppKnowledgeContext、AppKnowledgeSuggestion、PcCategory、PcProductivity、PcActivityRecordKey、PcActivityAnalysis
- 分支与异常：无
- 调用：DI 扩展

#### `MapEndpoints(endpoints)`
- 输入：`IEndpointRouteBuilder`
- 输出：无
- 副作用：挂载路由组
- 步骤（概要）：
  1. `readGroup`=`/api/v1/pc`；`writeGroup` 同路径 + RequireAuthorization
  2. 写：keystats/upload、keystats/samples、aw/upload、aw/upload-complete → PcTrackerService
  3. 读：summary、aw/timeline、aw/heatmap、keystats/range、detail（多过滤参数→DetailQueryParams）、quality、categories 列表
  4. 写：categories POST/DELETE
  5. 分类规则/建议/设置/activity-analysis/preview/apply/accept/reject/recompute
  6. heatmap/grid
  7. app-knowledge 读写组（AllowAnonymous 读 / 授权写）：apps、contexts、suggestions preview/apply
  8. app-signatures CRUD 组
  9. categories 树组：GET tree、POST、DELETE、reorder、seed
  10. productivity dashboard/range；timeline/v2
  11. 各 handler 将异常映射 400/404/409 与中文 ApiResponse
- 分支与异常：日期解析/Argument/KeyNotFound/InvalidOperation 等按端点处理
- 调用：各 Service 方法、`TryParseDate`、`NeedsClassificationSuggestion`

#### 分类建议列表 handler（`GET /classification/suggestions`）
- 输入：date 查询
- 输出：建议 DTO 列表
- 副作用：只读查详情并生成建议
- 步骤：
  1. 解析日；QueryCompleteDetail 当日 pageSize=500
  2. 过滤 `NeedsClassificationSuggestion`（Source=fallback 或 Confidence<0.5）
  3. 读设置最小时长；`BuildSuggestionsAsync`
- 分支与异常：日期解析可能抛（未单独 catch）
- 调用：PcTrackerService、ActivitySuggestionService、SettingsService

#### App 知识库 apply handler
- 输入：suggestion id + apply 请求
- 输出：`AppKnowledgeSuggestionApplyDto`
- 副作用：重算分类 + 写知识上下文
- 步骤：PreviewSuggestion → BuildRecommendedContext → ApplySuggestionWithSideEffect（回调 SaveRecommendedContext，回写影响统计）→ 组装消息
- 分支与异常：404/400/409
- 调用：Recompute、Drafts、AppKnowledgeSuggestionService

#### `InitializeAsync(serviceProvider)`
- 输入：根服务提供器
- 输出：Task
- 副作用：Scope 内 Schema 初始化 + SeedDefaults
- 步骤：CreateScope → PcTrackerSchemaInitializer.InitializeAsync → PcCategoryService.SeedDefaultsAsync
- 分支与异常：向上抛
- 调用：上述服务

#### `TryParseDate(value?)`
- 输入：字符串
- 输出：`DateTime?`（仅 Date 部分）或 null
- 副作用：无
- 步骤：TryParse 成功取 `.Date`
- 分支与异常：失败 null
- 调用：无

#### `NeedsClassificationSuggestion(record)`
- 输入：`PcDetailRecord`
- 输出：bool
- 副作用：无
- 步骤：ClassificationSource 等于 fallback（忽略大小写）或 Confidence 有值且 <0.5
- 分支与异常：无
- 调用：无

## 近逐行中文伪代码

1. IModule：Name=pctracker Version=1.0.0
2. RegisterServices：注册程序集与全部 Scoped 服务
3. MapEndpoints：/api/v1/pc 读写组
4. 上传 keystats/AW；读汇总时间线热图详情质量
5. 旧 categories 与分类规则/建议/设置/分析/重算
6. app-knowledge 与 app-signatures；Phase2 分类树与生产力
7. InitializeAsync：Schema + 分类种子
8. TryParseDate；NeedsClassificationSuggestion 低置信/fallback

## 关系边
```json
{
  "nodes": [
    {
      "id": "src/modules/Pim.Module.PcTracker/PcTrackerModule.cs",
      "label": "PcTrackerModule",
      "path": "src/modules/Pim.Module.PcTracker/PcTrackerModule.cs",
      "doc": "docs/pseudocode/files/src/modules/Pim.Module.PcTracker/PcTrackerModule.cs.md",
      "layer": "module.pctracker",
      "kind": "entrypoint"
    }
  ],
  "edges": [
    { "from": "src/modules/Pim.Module.PcTracker/PcTrackerModule.cs", "to": "src/Pim.Core/Modules", "type": "implements" },
    { "from": "src/modules/Pim.Module.PcTracker/PcTrackerModule.cs", "to": "src/Pim.Infrastructure/Data/PimDbContext.cs", "type": "depends_on" },
    { "from": "src/modules/Pim.Module.PcTracker/PcTrackerModule.cs", "to": "src/Pim.Core/Common", "type": "depends_on" },
    { "from": "src/modules/Pim.Module.PcTracker/PcTrackerModule.cs", "to": "src/modules/Pim.Module.PcTracker/DTOs/ActivityClassificationDtos.cs", "type": "depends_on" },
    { "from": "src/modules/Pim.Module.PcTracker/PcTrackerModule.cs", "to": "src/modules/Pim.Module.PcTracker/Services/PcCategoryService.cs", "type": "calls" },
    { "from": "src/modules/Pim.Module.PcTracker/PcTrackerModule.cs", "to": "src/modules/Pim.Module.PcTracker/Services/PcTrackerService.cs", "type": "calls" },
    { "from": "src/modules/Pim.Module.PcTracker/PcTrackerModule.cs", "to": "src/modules/Pim.Module.PcTracker/Services/PcTrackerQualityService.cs", "type": "calls" },
    { "from": "src/modules/Pim.Module.PcTracker/PcTrackerModule.cs", "to": "src/modules/Pim.Module.PcTracker/Services/ActivityClassificationRuleService.cs", "type": "calls" },
    { "from": "src/modules/Pim.Module.PcTracker/PcTrackerModule.cs", "to": "src/modules/Pim.Module.PcTracker/Services/ActivityClassificationRecomputeService.cs", "type": "calls" },
    { "from": "src/modules/Pim.Module.PcTracker/PcTrackerModule.cs", "to": "src/modules/Pim.Module.PcTracker/Services/ActivitySuggestionService.cs", "type": "calls" },
    { "from": "src/modules/Pim.Module.PcTracker/PcTrackerModule.cs", "to": "src/modules/Pim.Module.PcTracker/Services/AppSignatureService.cs", "type": "calls" },
    { "from": "src/modules/Pim.Module.PcTracker/PcTrackerModule.cs", "to": "src/modules/Pim.Module.PcTracker/Services/AppKnowledgeContextService.cs", "type": "calls" },
    { "from": "src/modules/Pim.Module.PcTracker/PcTrackerModule.cs", "to": "src/modules/Pim.Module.PcTracker/Services/AppKnowledgeSuggestionService.cs", "type": "calls" },
    { "from": "src/modules/Pim.Module.PcTracker/PcTrackerModule.cs", "to": "src/modules/Pim.Module.PcTracker/Services/PcProductivityService.cs", "type": "calls" },
    { "from": "src/modules/Pim.Module.PcTracker/PcTrackerModule.cs", "to": "src/modules/Pim.Module.PcTracker/Services/PcActivityAnalysisService.cs", "type": "calls" },
    { "from": "src/Pim.Api/ModuleRegistry.cs", "to": "src/modules/Pim.Module.PcTracker/PcTrackerModule.cs", "type": "depends_on" }
  ]
}
```
