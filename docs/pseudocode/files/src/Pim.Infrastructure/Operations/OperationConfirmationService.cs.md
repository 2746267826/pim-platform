# src/Pim.Infrastructure/Operations/OperationConfirmationService.cs

## 元信息
- 语言：C#
- 程序集或包：Pim.Infrastructure
- 职责：实现 `IOperationConfirmationService`：创建/查询/列表待确认操作，支持基础/二级/严格确认、拒绝、标记已执行与过期清理；PreviewJson 内嵌 `_meta` 元数据。
- 主要依赖：
  - `Pim.Core.Operations`（DTO/枚举/接口）
  - `Pim.Core.Exceptions.DomainException`
  - `PimDbContext` / `OperationConfirmationEntity`
  - `System.Text.Json`
- 被谁使用：
  - `OperationsEndpoints`、`TodaySectionProviders`
  - Calendar：`PlanningModelService`、`OutlookSyncService`、`OutlookConflictService`、`DataCenterGovernanceService`、`ReportService`
  - DI：`ServiceCollectionExtensions`
  - 大量 Operations/Calendar 单元测试

## 函数级结构化伪代码

### OperationConfirmationService
#### 构造 `OperationConfirmationService(PimDbContext db)`
- 输入：DbContext
- 输出：服务实例
- 副作用：保存 `_db`
- 步骤：1. 注入上下文
- 分支与异常：无
- 调用：无

#### `Task<OperationConfirmationDto> CreateAsync(CreateOperationConfirmationRequest request, CancellationToken ct)`
- 输入：创建请求、取消令牌
- 输出：新建确认 DTO
- 副作用：校验 JSON；组装 Preview 含 `_meta`；插入 Pending 行
- 步骤：
  1. `ValidateJson(PayloadJson, 3006)`、`ValidateJson(PreviewJson, 3007)`。
  2. `previewJson = BuildPreviewJson(request)` 后再校验 3007。
  3. new `OperationConfirmationEntity`：用户/类型/摘要/风险字符串/来源/Payload/Preview/Status=Pending/Expires/CreatedAt/CorrelationId。
  4. Add + SaveChanges；`Map` 返回。
- 分支与异常：非法 JSON → DomainException 3006/3007
- 调用：`ValidateJson`、`BuildPreviewJson`、`Map`、EF Save

#### `Task<OperationConfirmationDto?> GetAsync(Guid id, CancellationToken ct)`
- 输入：Id
- 输出：DTO 或 null
- 副作用：AsNoTracking 查询
- 步骤：1. SingleOrDefault by Id；null 则 null 否则 Map
- 分支与异常：无
- 调用：`Map`

#### `Task<IReadOnlyList<...>> ListPendingAsync` / `ListPendingForUserAsync`
- 输入：后者带可选 userId
- 输出：Pending 列表（按 ExpiresAt 升序）
- 副作用：先 `ExpireOldAsync(UtcNow)`
- 步骤：
  1. 过期清理。
  2. 过滤 Status=Pending；用户列表额外要求 `RequestedByUserId == null || == userId`。
  3. OrderBy ExpiresAt；Select Map。
- 分支与异常：无
- 调用：`ExpireOldAsync`、`Map`

#### `ConfirmAsync` / `ConfirmSecondLevelAsync` / `ConfirmStrictAsync`
- 输入：id、userId
- 输出：已确认 DTO
- 副作用：状态改为 Confirmed，写 ConfirmedAt
- 步骤：各自委托 `ConfirmWithModeAsync`（Basic / SecondLevel / Strict）
- 分支与异常：见内部
- 调用：`ConfirmWithModeAsync`

#### `private ConfirmWithModeAsync(id, userId, mode, ct)`
- 输入：模式枚举
- 输出：DTO
- 副作用：更新 Pending→Confirmed
- 步骤：
  1. `LoadPendingAsync`；`EnsureUserCanAct`；`EnsureConfirmationMode`。
  2. Status=Confirmed；ConfirmedAt=UtcNow；Save；Map。
- 分支与异常：见子方法
- 调用：Load/Ensure/Map

#### `private static EnsureConfirmationMode(entity, mode)`
- 输入：实体、模式
- 输出：void 或抛错
- 副作用：无
- 步骤：
  1. 从 PreviewJson 抽 metadata。
  2. 若 RequiresStrict 且 mode≠Strict → 3009。
  3. 若 RequiresSecondLevel 且 mode=Basic → 3010。
- 分支与异常：DomainException 3009/3010
- 调用：`ExtractMetadata`

#### `RejectAsync` / `MarkExecutedAsync` / `ExpireOldAsync`
- 输入：见签名
- 输出：DTO 或过期条数
- 副作用：改状态 Rejected/Executed/Expired；Executed 写 ResultJson
- 步骤：
  - Reject：LoadPending + EnsureUserCanAct → Rejected + RejectedAt。
  - MarkExecuted：Find；不存在 3001；非 Confirmed 3002；Validate ResultJson 3008 → Executed。
  - ExpireOld：查 Pending 且 ExpiresAt≤now → Expired；返回 count。
- 分支与异常：3001/3002/3008
- 调用：Load/Validate/Map/EF

#### `private LoadPendingAsync` / `EnsureUserCanAct` / `ValidateJson`
- 输入：id 或 entity/userId 或 json
- 输出：实体 / void
- 副作用：过期时可能写 Expired
- 步骤：
  - Load：Find；null→3001；非 Pending→3003；已过期则标 Expired 保存后 3004。
  - EnsureUser：若有 RequestedByUserId 且 ≠ 当前 userId → 3005。
  - ValidateJson：Parse；JsonException→DomainException(code,msg)。
- 分支与异常：如上
- 调用：EF / JsonDocument

#### `BuildPreviewJson` / `ExtractMetadata` / 读取辅助 / `Map` / `ParseRiskLevel`
- 输入：请求或 previewJson 或 entity
- 输出：合并后的 preview 字符串 / ConfirmationMetadata / DTO / 风险枚举
- 副作用：无（纯变换）
- 步骤：
  1. Build：Parse Preview → Dictionary；写入 `_meta`（changedFields、allowedActions、object、二级/严格标志、before/after、audit、ai、external、recovery）；Serialize。
  2. Extract：无 `_meta` 或解析失败 → Empty；否则读各字段。
  3. ReadStringArray/ReadString/ReadGuid/ReadBool 按 JsonValueKind 安全读取。
  4. Map：Extract + 构造 OperationConfirmationDto（Status/Risk 解析）。
  5. ParseRiskLevel：TryParse 失败则 Medium。
- 分支与异常：Extract 吞 JsonException 返回 Empty
- 调用：JsonSerializer/JsonDocument

#### 私有 `ConfirmationMetadata` record / `ConfirmationMode` enum
- 输入：字段列表
- 输出：元数据或模式枚举
- 副作用：无
- 步骤：Empty 静态默认；模式 Basic/SecondLevel/Strict
- 分支与异常：无
- 调用：无

## 近逐行中文伪代码

1. 引入 Json、EF、DomainException、Core.Operations、Data/Entities。
2. 命名空间 `Pim.Infrastructure.Operations`；密封类实现 `IOperationConfirmationService`。
3. 构造注入 `PimDbContext`。
4. Create：校验 Payload/Preview → BuildPreview 再校验 → 填实体 Pending → Save → Map。
5. Get：AsNoTracking 按 Id；Map 或 null。
6. ListPending / ListPendingForUser：先过期清理，再筛 Pending（用户可见系统+本人），按过期时间排序 Map。
7. Confirm/ConfirmSecondLevel/ConfirmStrict → ConfirmWithMode。
8. ConfirmWithMode：加载 Pending、用户校验、模式校验 → Confirmed。
9. EnsureConfirmationMode：严格/二级标志与 mode 不匹配则 3009/3010。
10. Reject：Pending→Rejected。
11. MarkExecuted：仅 Confirmed；校验 result JSON → Executed。
12. ExpireOld：批量 Pending 且过期 → Expired，返回数量。
13. LoadPending：存在性/状态/过期三道闸。
14. EnsureUserCanAct：绑定用户不一致则 3005。
15. ValidateJson：Parse 失败抛 DomainException。
16. BuildPreviewJson：原 Preview 字典 + `_meta` 扩展字段。
17. ExtractMetadata / Read*：安全解析 `_meta`。
18. Map：实体 + metadata → OperationConfirmationDto。
19. ParseRiskLevel：失败默认 Medium。
20. 私有 ConfirmationMetadata 与 ConfirmationMode。

## 关系边
```json
{
  "nodes": [
    {
      "id": "src/Pim.Infrastructure/Operations/OperationConfirmationService.cs",
      "label": "OperationConfirmationService",
      "path": "src/Pim.Infrastructure/Operations/OperationConfirmationService.cs",
      "doc": "docs/pseudocode/files/src/Pim.Infrastructure/Operations/OperationConfirmationService.cs.md",
      "layer": "infrastructure",
      "kind": "service"
    }
  ],
  "edges": [
    { "from": "src/Pim.Infrastructure/Operations/OperationConfirmationService.cs", "to": "src/Pim.Core/Operations/ConfirmationDtos.cs", "type": "implements" },
    { "from": "src/Pim.Infrastructure/Operations/OperationConfirmationService.cs", "to": "src/Pim.Core/Operations/OperationEnums.cs", "type": "depends_on" },
    { "from": "src/Pim.Infrastructure/Operations/OperationConfirmationService.cs", "to": "src/Pim.Core/Exceptions/DomainException.cs", "type": "depends_on" },
    { "from": "src/Pim.Infrastructure/Operations/OperationConfirmationService.cs", "to": "src/Pim.Infrastructure/Data/PimDbContext.cs", "type": "depends_on" },
    { "from": "src/Pim.Infrastructure/Operations/OperationConfirmationService.cs", "to": "src/Pim.Infrastructure/Data/Entities", "type": "depends_on" },
    { "from": "src/Pim.Api/Endpoints/OperationsEndpoints.cs", "to": "src/Pim.Infrastructure/Operations/OperationConfirmationService.cs", "type": "calls" },
    { "from": "src/Pim.Api/Today/TodaySectionProviders.cs", "to": "src/Pim.Core/Operations/ConfirmationDtos.cs", "type": "calls" },
    { "from": "src/modules/Pim.Module.Calendar/Services/PlanningModelService.cs", "to": "src/Pim.Infrastructure/Operations/OperationConfirmationService.cs", "type": "depends_on" },
    { "from": "src/modules/Pim.Module.Calendar/Services/OutlookSyncService.cs", "to": "src/Pim.Core/Operations/ConfirmationDtos.cs", "type": "depends_on" },
    { "from": "src/Pim.Infrastructure/Extensions/ServiceCollectionExtensions.cs", "to": "src/Pim.Infrastructure/Operations/OperationConfirmationService.cs", "type": "depends_on" },
    { "from": "tests/Pim.UnitTests/Operations/AuditAndConfirmationServiceTests.cs", "to": "src/Pim.Infrastructure/Operations/OperationConfirmationService.cs", "type": "tests" }
  ]
}
```
