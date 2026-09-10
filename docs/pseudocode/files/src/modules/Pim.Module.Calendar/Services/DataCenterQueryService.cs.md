# src/modules/Pim.Module.Calendar/Services/DataCenterQueryService.cs

## 元信息
- 语言：C#
- 程序集或包：Pim.Module.Calendar
- 职责：数据中心统一查询：聚合当前用户相关日历/任务/习惯/提醒/报告/同步/审计/确认与回收站条目，内存过滤分页后返回 `DataCenterQueryResponse`。
- 主要依赖：
  - `PimDbContext`、`ICurrentUserService`
  - 多实体：`EventEntity`、`TaskEntity`、`HabitRoutineEntity`、`ReminderEntity`、`AuditVersionEntity` 等
  - `DataCenterQueryRequest`/`DataCenterItem`/`DataCenterQueryResponse` DTO
  - `DomainException`、`OperationConfirmationStatus`
- 被谁使用：Calendar 模块数据中心相关端点/服务

## 函数级结构化伪代码

### DataCenterQueryService
#### 构造与 `UserId`
- 输入：db、currentUser
- 输出：实例；`UserId` 属性读当前用户
- 副作用：无登录时 `DomainException(01002)`
- 步骤：字段赋值；`UserId` 取 `_currentUser.UserId` 或抛异常
- 分支与异常：未登录 01002
- 调用：`ICurrentUserService`

#### `async Task<DataCenterQueryResponse> QueryAsync(DataCenterQueryRequest request, CancellationToken ct)`
- 输入：分页/筛选请求
- 输出：分页后的统一条目列表
- 副作用：大量只读 EF 查询（含 IgnoreQueryFilters 回收站）
- 步骤：
  1. 取 userId；page≥1；pageSize clamp 1–100。
  2. 依次加载并投影为 `DataCenterItem`：
     - event / task / task-segment / habit / habit-occurrence
     - availability / ai-placeholder / reminder / reminder-delivery
     - report / report-suggestion / sync-connection / sync-batch / sync-conflict
     - audit-version（**未按用户过滤**）
     - confirmation（RequestedBy 为空或等于当前用户）
     - recycle-bin：已删 calendar / event / task
  3. `ApplyFilters` → 多键排序（StartsAt/EndsAt/ObjectType/Title/ObjectId）。
  4. Skip/Take 分页；返回 items + page + pageSize + totalCount。
- 分支与异常：未登录见 UserId；DB 异常向上
- 调用：EF Set/Include/Where/ToListAsync；`ApplyFilters`、`BuildEventSummary`、`FirstText`

#### `static IEnumerable<DataCenterItem> ApplyFilters(items, request)`
- 输入：全量条目与请求筛选
- 输出：过滤后的序列
- 副作用：无
- 步骤：
  1. Search：Title/Summary/Source/Status 忽略大小写 Contains。
  2. ObjectType / Source 精确忽略大小写匹配。
  3. PendingOnly：类型 confirmation 且 Status 在 Pending 集合且 EndsAt 空或未过期。
- 分支与异常：无
- 调用：`ContainsIgnoreCase`、`PendingStatuses`

#### `static bool ContainsIgnoreCase` / `static string FirstText` / `static string BuildEventSummary`
- 输入：字符串或 EventEntity
- 输出：布尔 / 首个非空白串 / 事件摘要
- 副作用：无
- 步骤：
  1. ContainsIgnoreCase：null 则 false。
  2. FirstText：FirstOrDefault 非空白否则空串。
  3. BuildEventSummary：拼接 Description/Location/Calendar.Name；若 Source 以 outlook 开头附加 GraphEventId/ChangeKey。
- 分支与异常：无
- 调用：无

## 近逐行中文伪代码

1. 引入 EF、DomainException、Operations、Audit、Auth、Data、Calendar DTOs/Entities。
2. 静态 PendingStatuses：枚举 Pending 字符串与 `"pending"`。
3. 注入 DbContext 与 ICurrentUserService；UserId 未登录抛 01002。
4. QueryAsync：规范化分页；拉事件（含日历）→ 任务 → 执行段 → 习惯 → 习惯发生 → 可用窗口 → AI 占位 → 提醒 → 投递 → 报告 → 报告建议 → Outlook 连接/批次/冲突 → 审计版本（全表）→ 操作确认 → 软删日历/事件/任务作回收站。
5. 每类映射 DataCenterItem（类型、Id、标题、来源、状态、起止、摘要）。
6. ApplyFilters + 排序 + Skip/Take → Response。
7. ApplyFilters 支持搜索、对象类型、来源、仅待确认。
8. 辅助：忽略大小写包含、FirstText、事件摘要（Outlook 附加 Graph 字段）。

## 关系边
```json
{
  "nodes": [
    {
      "id": "src/modules/Pim.Module.Calendar/Services/DataCenterQueryService.cs",
      "label": "DataCenterQueryService",
      "path": "src/modules/Pim.Module.Calendar/Services/DataCenterQueryService.cs",
      "doc": "docs/pseudocode/files/src/modules/Pim.Module.Calendar/Services/DataCenterQueryService.cs.md",
      "layer": "module.calendar",
      "kind": "service"
    }
  ],
  "edges": [
    { "from": "src/modules/Pim.Module.Calendar/Services/DataCenterQueryService.cs", "to": "src/Pim.Infrastructure/Data/PimDbContext.cs", "type": "depends_on" },
    { "from": "src/modules/Pim.Module.Calendar/Services/DataCenterQueryService.cs", "to": "src/Pim.Infrastructure/Auth/CurrentUserService.cs", "type": "depends_on" },
    { "from": "src/modules/Pim.Module.Calendar/Services/DataCenterQueryService.cs", "to": "src/Pim.Infrastructure/Audit/AuditVersionEntity.cs", "type": "depends_on" },
    { "from": "src/modules/Pim.Module.Calendar/Services/DataCenterQueryService.cs", "to": "src/modules/Pim.Module.Calendar/Entities/HabitRoutineEntity.cs", "type": "depends_on" },
    { "from": "src/modules/Pim.Module.Calendar/Services/DataCenterQueryService.cs", "to": "src/modules/Pim.Module.Calendar/DTOs", "type": "depends_on" },
    { "from": "src/modules/Pim.Module.Calendar/Services/DataCenterQueryService.cs", "to": "src/Pim.Core/Exceptions/DomainException.cs", "type": "depends_on" }
  ]
}
```
