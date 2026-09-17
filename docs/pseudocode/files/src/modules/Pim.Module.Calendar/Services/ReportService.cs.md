# src/modules/Pim.Module.Calendar/Services/ReportService.cs

## 元信息
- 语言：C#
- 程序集或包：Pim.Module.Calendar
- 职责：生成/列表/获取/归档报告产物；对报告建议发起需确认的操作。
- 主要依赖：`PimDbContext`、`ICurrentUserService`、`IOperationConfirmationService`、Calendar 实体与 DTO
- 被谁使用：`CalendarModule` 的 `/reports*` 端点

## 函数级结构化伪代码

### ReportService
#### 构造函数 ReportService(PimDbContext, ICurrentUserService, IOperationConfirmationService)
- 输入：DB、当前用户、确认服务
- 输出：实例
- 副作用：保存字段；静态 `JsonOptions` Web 默认
- 步骤：赋值依赖
- 分支与异常：无
- 调用：无

#### Guid UserId（属性）
- 输入：无
- 输出：当前用户 Guid
- 副作用：无
- 步骤：`UserId` 空则 `DomainException(01002, "Login required")`
- 分支与异常：未登录抛域异常
- 调用：`ICurrentUserService`

#### Task\<ReportArtifactDto\> GenerateAsync(GenerateReportRequest, ct)
- 输入：报告种类/日期/项目等
- 输出：新建报告 DTO
- 副作用：写入 `ReportArtifactEntity` 并 Save
- 步骤：
  1. `NormalizeKind` 校验种类
  2. 统计当前用户 tasks/completed/events/reminders/habits 计数
  3. 组装 inputs（含固定数据源名列表）与 Markdown 内容
  4. 新建实体：RiskLevel=`L0AutomaticArtifact`，Status=`Active`，序列化 Inputs/Metrics
  5. Add + SaveChanges → `Map`
- 分支与异常：非法 kind → 02045
- 调用：EF CountAsync/SaveChanges

#### Task\<IReadOnlyList\<ReportArtifactDto\>\> ListAsync(ct)
- 输入：取消令牌
- 输出：用户报告列表（GeneratedAt 降序）
- 副作用：只读查询
- 步骤：AsNoTracking 过滤 UserId，OrderByDescending，Select Map
- 分支与异常：未登录
- 调用：EF

#### Task\<ReportArtifactDto\> GetAsync(Guid id, ct)
- 输入：报告 ID
- 输出：DTO
- 副作用：无
- 步骤：`LoadReportAsync` + `Map`
- 分支与异常：不存在 02044
- 调用：LoadReportAsync

#### Task\<ReportArtifactDto\> ArchiveAsync(Guid id, ct)
- 输入：报告 ID
- 输出：归档后 DTO
- 副作用：Status=`Archived`，更新 UpdatedAt
- 步骤：加载 → 改状态 → Save → Map
- 分支与异常：不存在
- 调用：LoadReportAsync、SaveChanges

#### Task\<OperationConfirmationDto\> RequestSuggestionActionAsync(Guid suggestionId, ct)
- 输入：建议 ID
- 输出：操作确认 DTO
- 副作用：创建确认；建议状态 `PendingConfirmation` 并写 ConfirmationId
- 步骤：
  1. 查 `ReportSuggestionEntity`（含 Report，用户匹配）否则 02043
  2. 解析 ChangedFieldsJson
  3. `_confirmations.CreateAsync`：动作名 `report.suggestion.{Action}`，风险 L2，6 小时过期，选项 confirm/reject
  4. 回写建议并 Save
- 分支与异常：建议不存在；JSON 解析失败时字段列表空
- 调用：`IOperationConfirmationService.CreateAsync`

#### LoadReportAsync / Map / ReadChangedFields / NormalizeKind（私有）
- 输入：见签名
- 输出：实体或 DTO 或字段列表或规范化 kind
- 副作用：无（除调用方）
- 步骤：按 ID+UserId 加载；Map 投影；反序列化失败返回 []；kind 仅 Daily/Weekly/Monthly/Project
- 分支与异常：02044/02045/JsonException
- 调用：JsonSerializer、EF

## 近逐行中文伪代码

1. 引入 Json、EF、DomainException、Operations、Auth、Data、Calendar DTO/实体
2. 密封类 `ReportService`，静态 Web JsonOptions
3. 注入 db/currentUser/confirmations
4. `UserId` 未登录抛 01002
5. `GenerateAsync`：规范化 kind；统计五类指标；写 Markdown；落库 Active 报告
6. `ListAsync`：当前用户报告按生成时间倒序
7. `GetAsync`/`ArchiveAsync`：加载并映射/归档
8. `RequestSuggestionActionAsync`：加载建议→建 L2 确认→挂到建议并 PendingConfirmation
9. 私有：LoadReport、Map、读变更字段、NormalizeKind 白名单

## 关系边
```json
{
  "nodes": [
    {
      "id": "src/modules/Pim.Module.Calendar/Services/ReportService.cs",
      "label": "ReportService",
      "path": "src/modules/Pim.Module.Calendar/Services/ReportService.cs",
      "doc": "docs/pseudocode/files/src/modules/Pim.Module.Calendar/Services/ReportService.cs.md",
      "layer": "module.calendar",
      "kind": "service"
    }
  ],
  "edges": [
    { "from": "src/modules/Pim.Module.Calendar/Services/ReportService.cs", "to": "src/Pim.Infrastructure/Data/PimDbContext.cs", "type": "depends_on" },
    { "from": "src/modules/Pim.Module.Calendar/Services/ReportService.cs", "to": "src/Pim.Infrastructure/Auth", "type": "depends_on" },
    { "from": "src/modules/Pim.Module.Calendar/Services/ReportService.cs", "to": "Pim.Core.Operations.IOperationConfirmationService", "type": "depends_on" },
    { "from": "src/modules/Pim.Module.Calendar/Services/ReportService.cs", "to": "src/modules/Pim.Module.Calendar/Entities/ReportArtifactEntity.cs", "type": "depends_on" },
    { "from": "src/modules/Pim.Module.Calendar/CalendarModule.cs", "to": "src/modules/Pim.Module.Calendar/Services/ReportService.cs", "type": "calls" }
  ]
}
```
