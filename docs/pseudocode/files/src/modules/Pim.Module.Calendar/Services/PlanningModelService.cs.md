# src/modules/Pim.Module.Calendar/Services/PlanningModelService.cs

## 元信息
- 语言：C#
- 程序集或包：Pim.Module.Calendar
- 职责：规划对象模型服务——多层日历图层聚合、项目/任务本/清单/习惯/可用窗/AI 占位与任务执行段 CRUD，AI 占位确认走操作确认流。
- 主要依赖：`PimDbContext`、`ICurrentUserService`、`IOperationConfirmationService`、`OperationConfirmationService`、Calendar 实体与 Planning DTO、`DomainException`
- 被谁使用：Calendar 规划相关 HTTP 端点

## 函数级结构化伪代码

### PlanningModelService
#### 静态字段 DefaultLayers / OutlookSources
- 输入：无
- 输出：默认图层集合；Outlook 来源集合
- 副作用：无
- 步骤：DefaultLayers=events/task-segments/habits/availability/ai-placeholders；OutlookSources=outlook/outlook-graph/outlook-ics
- 分支与异常：无
- 调用：无

#### 构造 PlanningModelService(PimDbContext db, ICurrentUserService currentUser, IOperationConfirmationService? confirmationService = null)
- 输入：Db、当前用户、可选确认服务
- 输出：实例
- 副作用：保存字段
- 步骤：赋值；confirmation 可空
- 分支与异常：无
- 调用：无

#### Guid UserId
- 输入：无
- 输出：用户 Id
- 副作用：无
- 步骤：空则 DomainException(01002, "Login required")
- 分支与异常：未登录
- 调用：无

#### Task\<CalendarLayerResponse\> GetCalendarLayersAsync(CalendarLayerQuery query, CancellationToken ct)
- 输入：Start/End/Layers/OutlookOnly
- 输出：图层项列表响应
- 副作用：多表查询
- 步骤：
  1. End<=Start → 02027
  2. NormalizeLayers；按请求层分别查询：
     - events：EventEntity 时间重叠 + 可选 Outlook 过滤 → CalendarLayerItem
     - task-segments：TaskExecutionSegmentEntity + Task.Title
     - habits：HabitOccurrenceEntity + HabitRoutine.Title
     - availability：AvailabilityWindowEntity
     - ai-placeholders：AiPlanningPlaceholderEntity，RequiresConfirmation=true
  3. 排序 StartsAt/Layer/Title/ObjectId；返回 CalendarLayerResponse
- 分支与异常：时间窗非法
- 调用：NormalizeLayers、IsOutlookSource

#### 项目/任务本：ListProjectsAsync / CreateProjectAsync / ListTaskBooksAsync / CreateTaskBookAsync
- 输入：创建请求或无
- 输出：DTO 列表或单条
- 副作用：读写 DomainProjectEntity、TaskBookEntity
- 步骤：用户隔离；Create 校验 Name 长度；TaskBook 可选 DomainProjectId 存在性 02028；Status/Kind 默认 Active/task
- 分支与异常：校验失败 02034/02028
- 调用：ValidateRequired、NormalizeShort

#### TaskChecklistItemDto AddChecklistItemAsync(Guid taskId, AddTaskChecklistItemRequest request, ct)
- 输入：任务 id、标题/排序
- 输出：清单项 DTO
- 副作用：插入 TaskChecklistItemEntity
- 步骤：GetTaskAsync；SortOrder 默认=现有条数；Add+Save
- 分支与异常：任务不存在 02004
- 调用：GetTaskAsync

#### 习惯：ListHabitsAsync / CreateHabitAsync / CreateHabitOccurrenceAsync
- 输入：创建习惯/发生请求
- 输出：DTO
- 副作用：插入 HabitRoutine/HabitOccurrence
- 步骤：Cadence 解析为枚举；Occurrence 校验 ends>starts 02029、习惯存在 02030
- 分支与异常：见上
- 调用：ParseCadence、NormalizeShort

#### 可用窗：ListAvailabilityAsync / CreateAvailabilityWindowAsync
- 输入：创建请求
- 输出：DTO
- 副作用：插入 AvailabilityWindowEntity
- 步骤：标题必填；ends>starts 02031；Kind 默认 available
- 分支与异常：02031/02034
- 调用：ValidateRequired

#### CreateAiPlaceholderAsync / ConfirmAiPlaceholderAsync
- 输入：创建请求 / 占位 id
- 输出：AiPlanningPlaceholderDto / OperationConfirmationDto
- 副作用：插入占位；创建 L2 确认并回写 ConfirmationId/Status
- 步骤：
  1. Create：时间校验 02032；Status=Suggested；Source 默认 ai
  2. Confirm：查占位 02033；confirmationService 空则 new OperationConfirmationService(_db)
  3. CreateAsync 操作 kind=calendar.ai_placeholder.confirm，风险 L2PimFactChange，12h 过期
  4. 占位 Status=PendingConfirmation；保存
- 分支与异常：不存在/时间非法
- 调用：IOperationConfirmationService.CreateAsync

#### 执行段：CreateSegmentAsync / ListSegmentsAsync / DeleteSegmentAsync
- 输入：taskId、段请求或 segmentId
- 输出：段响应列表或单条 / 无
- 副作用：插入或软删 TaskExecutionSegmentEntity；创建时更新 Task 收件箱与计划时间
- 步骤：时间校验 02024；Status/Source 1-40 字符；GetTask；List 排序 StartsAt；Delete 设 DeletedAt 02025
- 分支与异常：02024/02025/02026/02004
- 调用：GetTaskAsync、MapSegment

#### 私有辅助：GetTaskAsync / Validate* / Normalize* / ParseCadence / NormalizeLayers / IsOutlookSource / Map*
- 输入：校验值或图层列表
- 输出：实体或规范化集合/DTO
- 副作用：无（除抛异常）
- 步骤：长度与空白校验；图层逗号拆分小写，空则 DefaultLayers；Cadence 解析失败→Custom
- 分支与异常：02004/02026/02034/02035
- 调用：无

## 近逐行中文伪代码

1. 定义默认图层与 Outlook 来源集合
2. 注入 Db、当前用户、可选确认服务
3. UserId 未登录抛 01002
4. GetCalendarLayers：校验时间窗；按层查事件/执行段/习惯/可用窗/AI 占位；可选 OutlookOnly；统一排序
5. 项目与任务本列表/创建；任务本校验项目归属
6. 清单项：按任务计数默认 SortOrder
7. 习惯与发生：Cadence 枚举；发生时间校验
8. 可用窗 CRUD 简化为列表+创建
9. AI 占位创建 Suggested；确认创建操作确认并挂 ConfirmationId
10. 任务执行段创建/列表/软删；创建时同步任务计划字段
11. 辅助：必填与短字段规范化、图层归一、Outlook 判断、DTO 映射

## 关系边
```json
{
  "nodes": [
    {
      "id": "src/modules/Pim.Module.Calendar/Services/PlanningModelService.cs",
      "label": "PlanningModelService",
      "path": "src/modules/Pim.Module.Calendar/Services/PlanningModelService.cs",
      "doc": "docs/pseudocode/files/src/modules/Pim.Module.Calendar/Services/PlanningModelService.cs.md",
      "layer": "module.calendar",
      "kind": "service"
    }
  ],
  "edges": [
    { "from": "src/modules/Pim.Module.Calendar/Services/PlanningModelService.cs", "to": "src/Pim.Infrastructure/Data/PimDbContext.cs", "type": "depends_on" },
    { "from": "src/modules/Pim.Module.Calendar/Services/PlanningModelService.cs", "to": "src/Pim.Infrastructure/Auth/CurrentUserService.cs", "type": "depends_on" },
    { "from": "src/modules/Pim.Module.Calendar/Services/PlanningModelService.cs", "to": "src/Pim.Infrastructure/Operations/OperationConfirmationService.cs", "type": "calls" },
    { "from": "src/modules/Pim.Module.Calendar/Services/PlanningModelService.cs", "to": "src/modules/Pim.Module.Calendar/Entities/EventEntity.cs", "type": "depends_on" },
    { "from": "src/modules/Pim.Module.Calendar/Services/PlanningModelService.cs", "to": "src/modules/Pim.Module.Calendar/Entities/TaskExecutionSegmentEntity.cs", "type": "depends_on" },
    { "from": "src/modules/Pim.Module.Calendar/Services/PlanningModelService.cs", "to": "src/modules/Pim.Module.Calendar/Entities/HabitRoutineEntity.cs", "type": "depends_on" },
    { "from": "src/modules/Pim.Module.Calendar/Services/PlanningModelService.cs", "to": "src/modules/Pim.Module.Calendar/Entities/AiPlanningPlaceholderEntity.cs", "type": "depends_on" },
    { "from": "src/modules/Pim.Module.Calendar/Services/PlanningModelService.cs", "to": "src/Pim.Core/Operations", "type": "depends_on" },
    { "from": "src/modules/Pim.Module.Calendar/Services/PlanningModelService.cs", "to": "src/Pim.Core/Planning", "type": "depends_on" }
  ]
}
```
