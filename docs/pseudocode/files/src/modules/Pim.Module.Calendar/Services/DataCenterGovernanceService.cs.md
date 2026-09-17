# src/modules/Pim.Module.Calendar/Services/DataCenterGovernanceService.cs

## 元信息
- 语言：C#
- 程序集或包：Pim.Module.Calendar
- 职责：数据中心治理：批量操作预览/严格确认/执行（当前仅 archive）、审计导出、恢复预览与恢复确认；对 task/event 软归档、report 状态归档，并写入 AuditVersion。
- 主要依赖：
  - `PimDbContext`、`ICurrentUserService`、`IOperationConfirmationService`、`AuditVersionService`
  - Calendar DTOs（Batch/Restore 请求响应）、TaskEntity/EventEntity/ReportArtifactEntity
  - `DomainException`、`OperationRiskLevel`、System.Text.Json
- 被谁使用：Calendar/DataCenter 相关 API 端点

## 函数级结构化伪代码

### DataCenterGovernanceService
#### 构造函数
- 输入：db、currentUser、confirmations、auditVersions
- 步骤：字段赋值；静态 `JsonOptions` = Web 默认

#### 属性 `UserId`
- 输出：当前用户 Guid；无则 DomainException(01002, "Login required")

#### `Task<DataCenterBatchPreviewResponse> PreviewBatchOperationAsync(request, ct)`
- 输入：`DataCenterBatchOperationRequest`
- 输出：风险 L4、需严格确认、摘要、类型列表、对象数
- 副作用：无（ct 未使用）
- 步骤：
  1. `NormalizeObjects`。
  2. 去重排序 ObjectType；拼多行英文摘要（动作、类型、可恢复路径提示）。
  3. 返回 `OperationRiskLevel.L4BatchOrDestructiveGovernance`、RequiresStrictConfirmation=true。
- 分支与异常：Normalize 可抛 02047/02048
- 调用：`NormalizeObjects`

#### `Task<OperationConfirmationDto> RequestBatchConfirmationAsync(request, ct)`
- 输入：批量请求
- 输出：新建确认 DTO
- 副作用：创建确认记录
- 步骤：
  1. Preview → 序列化 request/preview JSON。
  2. `CreateAsync`：action=`data-center.batch.{Action}`、L4、scope data-center、过期 +8h、允许 confirm-strict/reject、二级+严格确认、AuditBatchId 新 Guid、AI/外部/恢复文案。
- 调用：Preview、`_confirmations.CreateAsync`

#### `Task<DataCenterBatchExecutionResponse> ExecuteConfirmedBatchAsync(confirmationId, ct)`
- 输入：确认 Id
- 输出：确认 Id、状态字符串、affectedCount
- 副作用：归档对象、标记确认已执行
- 步骤：
  1. Get 确认；null → 02046。
  2. `EnsureCanExecute`。
  3. `ReadBatchRequest(PayloadJson)` → `ExecuteBatchActionAsync`。
  4. `MarkExecutedAsync` 写入 executed JSON。
  5. 返回 ExecutionResponse。
- 分支与异常：02046/02051/02052/02055/02053/02054/02049/02050
- 调用：confirmations、ExecuteBatchActionAsync

#### `ExportAuditAsync(start, end, ct)`
- 步骤：委托 `_auditVersions.ExportAsync`

#### `PreviewRestoreAsync(DataCenterRestoreRequest, ct)`
- 步骤：委托 `_auditVersions.PreviewRestoreAsync(AuditVersionId)`

#### `RequestRestoreConfirmationAsync(request, ct)`
- 步骤：PreviewRestore → Create 确认 action=`data-center.restore`、L4、绑定 ObjectType/ObjectId、严格+二级确认
- 调用：PreviewRestore、CreateAsync

#### `NormalizeObjects(objects?)` private static
- 空/null → 02047；过滤空类型与 Empty Guid；trim 后仍空 → 02048；返回列表

#### `ExecuteBatchActionAsync(confirmation, request, ct)` private
- 仅支持 action `archive`（忽略大小写），否则 02049
- 对每个 Normalize 对象 `ArchiveObjectAsync`，累加 affectedCount

#### `ArchiveObjectAsync(obj, confirmationId, operationKind, archivedAt, ct)` private
- **task**：用户任务存在则设 DeletedAt/DeletedBy*、Record 审计字段，返回 1；否则 0
- **event**：含 Calendar，校验 Calendar.UserId；同样软删字段 + 审计
- **report**：Status=`Archived` + UpdatedAt + 审计 status/updatedAt
- 其它类型 → 02050
- 调用：EF Set、`_auditVersions.RecordAsync`

#### `EnsureCanExecute(confirmation)` private
- RequestedByUserId 有值且非本人 → 02051
- Status 必须 Confirmed → 否则 02052
- RequiresStrictConfirmation 必须 true → 否则 02055

#### `ReadBatchRequest(payloadJson)` private static
- Deserialize；null → 02053；JsonException → 02054

## 近逐行中文伪代码

1. 注入 Db、当前用户、确认服务、审计版本服务。
2. 预览：规范化对象 → 类型摘要 → 固定 L4 严格确认预览响应。
3. 请求批量确认：序列化 payload/preview → Create 带 8 小时过期与二级/严格标志。
4. 执行：校验确认存在且本人、已 Confirmed、需严格确认 → 解析 payload → 仅 archive → 逐对象归档 → MarkExecuted。
5. 归档 task/event：软删除元数据 + Record 审计；report 改 Archived。
6. 恢复：审计导出/预览委托；恢复确认同样 L4 严格流程。
7. 错误码覆盖空对象、无效引用、不支持动作/类型、payload 损坏、跨用户与未确认。

## 关系边
```json
{
  "nodes": [
    {
      "id": "src/modules/Pim.Module.Calendar/Services/DataCenterGovernanceService.cs",
      "label": "DataCenterGovernanceService",
      "path": "src/modules/Pim.Module.Calendar/Services/DataCenterGovernanceService.cs",
      "doc": "docs/pseudocode/files/src/modules/Pim.Module.Calendar/Services/DataCenterGovernanceService.cs.md",
      "layer": "module.calendar",
      "kind": "service"
    }
  ],
  "edges": [
    { "from": "src/modules/Pim.Module.Calendar/Services/DataCenterGovernanceService.cs", "to": "src/Pim.Infrastructure/Data/PimDbContext.cs", "type": "depends_on" },
    { "from": "src/modules/Pim.Module.Calendar/Services/DataCenterGovernanceService.cs", "to": "src/Pim.Infrastructure/Auth/CurrentUserService.cs", "type": "depends_on" },
    { "from": "src/modules/Pim.Module.Calendar/Services/DataCenterGovernanceService.cs", "to": "src/Pim.Infrastructure/Operations/OperationConfirmationService.cs", "type": "calls" },
    { "from": "src/modules/Pim.Module.Calendar/Services/DataCenterGovernanceService.cs", "to": "src/Pim.Infrastructure/Audit/AuditVersionService.cs", "type": "calls" },
    { "from": "src/modules/Pim.Module.Calendar/Services/DataCenterGovernanceService.cs", "to": "src/Pim.Core/Exceptions/DomainException.cs", "type": "depends_on" },
    { "from": "src/modules/Pim.Module.Calendar/Services/DataCenterGovernanceService.cs", "to": "src/Pim.Core/Operations", "type": "depends_on" },
    { "from": "src/modules/Pim.Module.Calendar/Services/DataCenterGovernanceService.cs", "to": "src/modules/Pim.Module.Calendar/Entities", "type": "depends_on" }
  ]
}
```
