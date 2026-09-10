# src/Pim.Core/Operations/ConfirmationDtos.cs

## 元信息
- 语言：C#
- 程序集或包：Pim.Core
- 职责：定义高风险操作确认的创建请求 DTO、对外 DTO，以及 `IOperationConfirmationService` 生命周期契约。
- 主要依赖：`OperationRiskLevel`、`OperationConfirmationStatus`（`OperationEnums`）
- 被谁使用：`OperationConfirmationService`（实现）、`OperationsEndpoints`、`TodaySectionProviders`、Calendar 模块治理/同步/冲突/规划/报告服务

## 函数级结构化伪代码

### CreateOperationConfirmationRequest
#### 记录构造（位置参数 record）
- 输入：
  - `RequestedByUserId`：可选发起用户
  - `OperationType` / `Summary`：操作类型与摘要
  - `RiskLevel`：风险等级枚举
  - `Source`：来源
  - `PayloadJson` / `PreviewJson`：载荷与预览 JSON
  - `ExpiresAt`：过期时间
  - `CorrelationId`：可选关联 Id
  - 可选扩展：`ChangedFields`、`AllowedActions`、`ObjectType`/`ObjectId`
  - 确认策略：`RequiresSecondLevelConfirmation`、`RequiresStrictConfirmation`
  - 审计快照：`BeforeJson`/`AfterJson`、`AuditBatchId`
  - AI/外部/恢复：`AiRecommendation`、`ExternalEffect`、`RecoveryPath`
- 输出：不可变创建请求
- 副作用：无
- 步骤：
  1. 业务方组装字段
  2. 调用 `IOperationConfirmationService.CreateAsync`
- 分支与异常：本类型无逻辑
- 调用：Calendar 治理、Outlook 同步/冲突、规划与报告等创建确认点

### OperationConfirmationDto
#### 记录构造（位置参数 record）
- 输入：与请求字段对应，并增加：
  - `Id`、`Status`、`CreatedAt`
  - `ConfirmedAt` / `ExecutedAt` / `ResultJson`（生命周期结果字段）
- 输出：操作确认对外 DTO
- 副作用：无
- 步骤：
  1. 由服务实现从实体映射
- 分支与异常：无
- 调用：作为服务各方法的返回值

### IOperationConfirmationService
#### Task<OperationConfirmationDto> CreateAsync(CreateOperationConfirmationRequest request, CancellationToken ct = default)
- 输入：创建请求；取消令牌
- 输出：新建确认 DTO
- 副作用：持久化一条 Pending 确认（实现侧）
- 步骤：
  1. 接收请求并写入存储
  2. 映射为 DTO 返回
- 分支与异常：契约不规定具体异常；遵循 `ct`
- 调用：业务创建高风险操作前

#### Task<OperationConfirmationDto?> GetAsync(Guid id, CancellationToken ct = default)
- 输入：确认 Id
- 输出：DTO 或 null
- 副作用：只读查询
- 步骤：
  1. 按 Id 查询并映射
- 分支与异常：不存在时返回 null
- 调用：详情/执行前校验

#### Task<IReadOnlyList<OperationConfirmationDto>> ListPendingAsync(CancellationToken ct = default)
- 输入：取消令牌
- 输出：全部待处理确认列表
- 副作用：只读
- 步骤：
  1. 过滤 Pending 状态并映射列表
- 分支与异常：无
- 调用：运营/今日面板等

#### Task<IReadOnlyList<OperationConfirmationDto>> ListPendingForUserAsync(Guid? userId, CancellationToken ct = default)
- 输入：可选用户 Id
- 输出：该用户相关待处理列表
- 副作用：只读
- 步骤：
  1. 按用户过滤 Pending
- 分支与异常：无
- 调用：用户维度待办

#### Task<OperationConfirmationDto> ConfirmAsync(Guid id, Guid? userId, CancellationToken ct = default)
- 输入：确认 Id、可选确认用户
- 输出：更新后的 DTO
- 副作用：状态推进为 Confirmed（实现侧）
- 步骤：
  1. 校验可确认
  2. 写入确认时间与状态
- 分支与异常：非法状态由实现抛出
- 调用：一级确认 API

#### Task<OperationConfirmationDto> ConfirmSecondLevelAsync(Guid id, Guid? userId, CancellationToken ct = default)
- 输入：确认 Id、用户
- 输出：更新后的 DTO
- 副作用：完成二级确认（实现侧）
- 步骤：
  1. 校验需要二级确认且当前允许
  2. 记录二级确认并推进状态
- 分支与异常：策略不满足时失败
- 调用：二级确认 API

#### Task<OperationConfirmationDto> ConfirmStrictAsync(Guid id, Guid? userId, CancellationToken ct = default)
- 输入：确认 Id、用户
- 输出：更新后的 DTO
- 副作用：完成严格确认（实现侧）
- 步骤：
  1. 校验 `RequiresStrictConfirmation`
  2. 完成严格确认流程
- 分支与异常：策略不满足时失败
- 调用：严格确认 API

#### Task<OperationConfirmationDto> RejectAsync(Guid id, Guid? userId, CancellationToken ct = default)
- 输入：确认 Id、用户
- 输出：拒绝后的 DTO
- 副作用：状态为 Rejected
- 步骤：
  1. 校验可拒绝
  2. 更新状态
- 分支与异常：非法状态失败
- 调用：拒绝 API

#### Task<OperationConfirmationDto> MarkExecutedAsync(Guid id, string resultJson, CancellationToken ct = default)
- 输入：确认 Id、执行结果 JSON
- 输出：执行后的 DTO
- 副作用：状态为 Executed，写入 `ResultJson`/`ExecutedAt`
- 步骤：
  1. 要求当前为 Confirmed
  2. 标记已执行
- 分支与异常：非 Confirmed 时失败
- 调用：业务真正执行完后回写

#### Task<int> ExpireOldAsync(DateTimeOffset now, CancellationToken ct = default)
- 输入：当前时间
- 输出：过期条数
- 副作用：将超时 Pending 标为 Expired
- 步骤：
  1. 查找 `ExpiresAt < now` 且 Pending 的记录
  2. 批量更新并返回数量
- 分支与异常：无
- 调用：后台清理/定时任务

## 近逐行中文伪代码

1. 命名空间：`Pim.Core.Operations`
2. 定义密封 record `CreateOperationConfirmationRequest`，字段包括：
3.   - 发起用户、操作类型、摘要、风险等级、来源
4.   - `PayloadJson`、`PreviewJson`、过期时间、关联 Id
5.   - 可选变更字段/允许动作、对象类型与 Id
6.   - 是否需要二级确认、前后 JSON、是否严格确认
7.   - 审计批次 Id、AI 建议、外部影响、恢复路径
8. 定义密封 record `OperationConfirmationDto`：
9.   - 在请求字段基础上增加 `Id`、`Status`、`CreatedAt`
10.   - 增加 `ConfirmedAt`、`ExecutedAt`、`ResultJson`
11. 定义接口 `IOperationConfirmationService`
12. 方法 `CreateAsync`：创建确认并返回 DTO
13. 方法 `GetAsync`：按 Id 获取，可能为 null
14. 方法 `ListPendingAsync`：列出全部待处理
15. 方法 `ListPendingForUserAsync`：按用户列待处理
16. 方法 `ConfirmAsync`：一级确认
17. 方法 `ConfirmSecondLevelAsync`：二级确认
18. 方法 `ConfirmStrictAsync`：严格确认
19. 方法 `RejectAsync`：拒绝
20. 方法 `MarkExecutedAsync`：标记已执行并写入结果 JSON
21. 方法 `ExpireOldAsync`：按时间过期旧记录，返回过期数量

## 关系边
```json
{
  "nodes": [
    {
      "id": "src/Pim.Core/Operations/ConfirmationDtos.cs",
      "label": "ConfirmationDtos",
      "path": "src/Pim.Core/Operations/ConfirmationDtos.cs",
      "doc": "docs/pseudocode/files/src/Pim.Core/Operations/ConfirmationDtos.cs.md",
      "layer": "core",
      "kind": "dto"
    }
  ],
  "edges": [
    { "from": "src/Pim.Core/Operations/ConfirmationDtos.cs", "to": "src/Pim.Core/Operations/OperationEnums.cs", "type": "depends_on" },
    { "from": "src/Pim.Infrastructure/Operations/OperationConfirmationService.cs", "to": "src/Pim.Core/Operations/ConfirmationDtos.cs", "type": "implements" },
    { "from": "src/Pim.Api/Endpoints/OperationsEndpoints.cs", "to": "src/Pim.Core/Operations/ConfirmationDtos.cs", "type": "calls" },
    { "from": "src/Pim.Api/Today/TodaySectionProviders.cs", "to": "src/Pim.Core/Operations/ConfirmationDtos.cs", "type": "calls" },
    { "from": "src/modules/Pim.Module.Calendar/Services/DataCenterGovernanceService.cs", "to": "src/Pim.Core/Operations/ConfirmationDtos.cs", "type": "calls" },
    { "from": "src/modules/Pim.Module.Calendar/Services/OutlookSyncService.cs", "to": "src/Pim.Core/Operations/ConfirmationDtos.cs", "type": "calls" },
    { "from": "src/modules/Pim.Module.Calendar/Services/OutlookConflictService.cs", "to": "src/Pim.Core/Operations/ConfirmationDtos.cs", "type": "calls" },
    { "from": "src/modules/Pim.Module.Calendar/Services/PlanningModelService.cs", "to": "src/Pim.Core/Operations/ConfirmationDtos.cs", "type": "calls" },
    { "from": "src/modules/Pim.Module.Calendar/Services/ReportService.cs", "to": "src/Pim.Core/Operations/ConfirmationDtos.cs", "type": "calls" }
  ]
}
```
