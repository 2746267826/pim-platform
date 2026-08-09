# src/modules/Pim.Module.Calendar/Services/OutlookConflictService.cs

## 元信息
- 语言：C#
- 程序集或包：Pim.Module.Calendar
- 职责：Outlook 同步冲突查询与解析确认；停同步预览/执行；与 `IOperationConfirmationService` 及审计版本联动。
- 主要依赖：`PimDbContext`、`ICurrentUserService`、`IOperationConfirmationService`、`AuditVersionService`、`DomainException`、Calendar DTOs/Entities
- 被谁使用：Calendar Outlook 相关端点/服务；`OutlookSourceGovernanceTests`

## 函数级结构化伪代码

### OutlookConflictService
#### OutlookConflictService(PimDbContext, ICurrentUserService, IOperationConfirmationService)
- 输入：DbContext、当前用户、确认服务
- 输出：实例
- 副作用：保存字段
- 步骤：赋值依赖
- 分支与异常：无
- 调用：无

#### private Guid UserId
- 输入：无
- 输出：当前用户 Guid
- 副作用：未登录抛 `DomainException(01002)`
- 步骤：`_currentUser.UserId` 有值则返回，否则抛异常
- 分支与异常：未登录
- 调用：无

#### Task<SyncConflictDetailDto> GetAsync(Guid conflictId, CancellationToken ct)
- 输入：冲突 Id
- 输出：冲突详情 DTO
- 副作用：读库
- 步骤：`LoadConflictAsync` → `Map`
- 分支与异常：不存在 → 02039
- 调用：`LoadConflictAsync`、`Map`

#### Task<OperationConfirmationDto> RequestActionAsync(Guid conflictId, ConflictResolutionRequest request, CancellationToken ct)
- 输入：冲突 Id、解析动作请求
- 输出：操作确认 DTO
- 副作用：创建确认；冲突状态改为 `pending-confirmation` 并关联确认 Id；SaveChanges
- 步骤：
  1. 加载冲突；`NormalizeAction(request.Action)`
  2. `CreateConfirmationAsync`（含快照与 mergedFields）
  3. 更新 conflict Status/ResolvedConfirmationId/UpdatedAt；保存
- 分支与异常：非法 action → 02040
- 调用：`CreateConfirmationAsync`、`SaveChangesAsync`

#### Task<OperationConfirmationDto> RequestStopSyncPreviewAsync(Guid eventId, CancellationToken ct)
- 输入：事件 Id
- 输出：停同步确认预览
- 副作用：创建 L4 严格确认
- 步骤：按用户查 `EventEntity`+Calendar；序列化 PIM/Outlook 快照；`CreateConfirmationAsync("stop_sync", ...)`
- 分支与异常：事件不存在 → 02001
- 调用：`CreateConfirmationAsync`

#### Task ExecuteConfirmedResolutionAsync(Guid confirmationId, CancellationToken ct)
- 输入：已确认的 confirmationId
- 输出：无
- 副作用：若 payload 含 conflictId 则标记冲突 resolved；MarkExecuted
- 步骤：
  1. Get 确认；状态必须 Confirmed
  2. 解析 PayloadJson；若有合法 conflictId → 加载冲突并 resolved
  3. `MarkExecutedAsync` 写结果 JSON
- 分支与异常：不存在 02006；未确认 02007
- 调用：`_confirmations.GetAsync`/`MarkExecutedAsync`、`LoadConflictAsync`

#### Task<object> ExecuteStopSyncAsync(Guid eventId, Guid confirmationId, CancellationToken ct)
- 输入：事件 Id、确认 Id
- 输出：`{ Id, Source }`
- 副作用：清除事件 Outlook 绑定字段；审计版本；MarkExecuted
- 步骤：
  1. 校验确认存在、Confirmed、RequiresStrictConfirmation、请求用户匹配
  2. 解析 payload：action 必须 `stop_sync` 且 objectId 匹配 eventId
  3. 加载事件；记录 before；清空 Source→manual 与 Outlook* 字段
  4. `AuditVersionService.RecordAsync`；`MarkExecutedAsync`
- 分支与异常：02006/02007/02043/02041/02042/02001
- 调用：`AuditVersionService`、`_confirmations`

#### private Task<OperationConfirmationDto> CreateConfirmationAsync(...)
- 输入：action、conflictId、对象类型/Id、Graph 事件 Id、双端快照、合并字段、原因
- 输出：创建后的确认 DTO
- 副作用：经确认服务落库
- 步骤：
  1. risk：`stop_sync` → L4，否则 L3
  2. 序列化 payloadJson / previewJson（含 pim/outlook 快照元素）
  3. `CreateAsync`：action 名 `outlook.stop_sync` 或 `outlook.conflict.{action}`，2 小时过期，字段/动作列表，二级或严格确认标志
- 分支与异常：无本地分支
- 调用：`_confirmations.CreateAsync`

#### private Task<SyncConflictEntity> LoadConflictAsync(Guid conflictId, CancellationToken ct)
- 输入：冲突 Id
- 输出：实体
- 副作用：读库
- 步骤：按 Id+UserId 查询，否则 02039
- 分支与异常：不存在
- 调用：EF

#### private static string NormalizeAction(string action)
- 输入：动作字符串
- 输出：规范大小写的合法动作
- 副作用：无
- 步骤：白名单 `ConflictActions` 忽略大小写匹配；否则 02040
- 分支与异常：空或不支持
- 调用：无

#### private static SyncConflictDetailDto Map(SyncConflictEntity conflict)
- 输入：实体
- 输出：详情 DTO
- 副作用：无
- 步骤：字段投影
- 分支与异常：无
- 调用：无

## 近逐行中文伪代码

1. 引入 Json、EF、DomainException、Operations、Audit、Auth、Data、Calendar DTO/Entity
2. 密封类；静态 Web JsonOptions；动作白名单 keep_pim/keep_outlook/merge_by_field/create_merge_copy/skip_batch/stop_sync
3. 构造注入 db/currentUser/confirmations；UserId 未登录抛 01002
4. GetAsync：加载冲突并 Map
5. RequestActionAsync：规范化动作→建确认→冲突 pending-confirmation→保存
6. RequestStopSyncPreviewAsync：校验事件属主→建 stop_sync 确认
7. ExecuteConfirmedResolutionAsync：确认已 Confirmed→可选解析 conflictId 为 resolved→MarkExecuted
8. ExecuteStopSyncAsync：严格确认与用户/payload 校验→清空 Outlook 绑定→审计→MarkExecuted
9. CreateConfirmationAsync：L3/L4 风险、payload/preview、CreateAsync 元数据
10. LoadConflictAsync / NormalizeAction / Map 辅助

## 关系边
```json
{
  "nodes": [
    {
      "id": "src/modules/Pim.Module.Calendar/Services/OutlookConflictService.cs",
      "label": "OutlookConflictService",
      "path": "src/modules/Pim.Module.Calendar/Services/OutlookConflictService.cs",
      "doc": "docs/pseudocode/files/src/modules/Pim.Module.Calendar/Services/OutlookConflictService.cs.md",
      "layer": "module.calendar",
      "kind": "service"
    }
  ],
  "edges": [
    { "from": "src/modules/Pim.Module.Calendar/Services/OutlookConflictService.cs", "to": "src/Pim.Infrastructure/Data/PimDbContext.cs", "type": "depends_on" },
    { "from": "src/modules/Pim.Module.Calendar/Services/OutlookConflictService.cs", "to": "src/Pim.Infrastructure/Auth/CurrentUserService.cs", "type": "depends_on" },
    { "from": "src/modules/Pim.Module.Calendar/Services/OutlookConflictService.cs", "to": "src/Pim.Infrastructure/Operations/OperationConfirmationService.cs", "type": "calls" },
    { "from": "src/modules/Pim.Module.Calendar/Services/OutlookConflictService.cs", "to": "src/Pim.Infrastructure/Audit/AuditVersionService.cs", "type": "calls" },
    { "from": "src/modules/Pim.Module.Calendar/Services/OutlookConflictService.cs", "to": "src/Pim.Core/Exceptions/DomainException.cs", "type": "depends_on" },
    { "from": "src/modules/Pim.Module.Calendar/Services/OutlookConflictService.cs", "to": "src/modules/Pim.Module.Calendar/Entities/SyncConflictEntity.cs", "type": "depends_on" },
    { "from": "src/modules/Pim.Module.Calendar/Services/OutlookConflictService.cs", "to": "src/modules/Pim.Module.Calendar/Entities/EventEntity.cs", "type": "depends_on" },
    { "from": "tests/Pim.UnitTests/Calendar/OutlookSourceGovernanceTests.cs", "to": "src/modules/Pim.Module.Calendar/Services/OutlookConflictService.cs", "type": "tests" }
  ]
}
```
