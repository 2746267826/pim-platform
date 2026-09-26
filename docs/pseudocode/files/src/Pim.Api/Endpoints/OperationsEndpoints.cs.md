# src/Pim.Api/Endpoints/OperationsEndpoints.cs

## 元信息
- 语言：C#
- 程序集或包：Pim.Api
- 职责：注册 `/api/v1/operations` 下运维确认与审计相关最小 API：待确认列表、单条查询、确认/二级确认/严格确认/拒绝，以及审计时间线、恢复预览与导出。
- 主要依赖：
  - `IOperationConfirmationService`、`ICurrentUserService`
  - `AuditVersionService`
  - `Pim.Core.Common.ApiResponse`、`Pim.Core.Operations` DTOs
  - `DomainException`
- 被谁使用：`Program.cs` → `app.MapOperationsEndpoints()`

## 函数级结构化伪代码

### OperationsEndpoints
#### `static void MapOperationsEndpoints(this IEndpointRouteBuilder endpoints)`
- 输入：路由构建器
- 输出：void（副作用注册路由）
- 副作用：挂载需授权的 GET/POST 处理程序
- 步骤：
  1. `MapGroup("/api/v1/operations").RequireAuthorization()`。
  2. 注册 confirmations 与 audit 相关端点（见下）。
- 分支与异常：各 handler 内处理；未登录由 `RequireCurrentUserId` 抛 01002
- 调用：Minimal API MapGet/MapPost

#### 内联 `GET /confirmations/pending`
- 输入：`IOperationConfirmationService`、`ICurrentUserService`、ct
- 输出：200 + `IReadOnlyList<OperationConfirmationDto>`
- 副作用：无写库
- 步骤：
  1. `RequireCurrentUserId` 取当前用户。
  2. `ListPendingForUserAsync(userId)`。
  3. `ApiResponse.Ok` 包装返回。
- 分支与异常：未登录 → DomainException 01002
- 调用：`confirmations.ListPendingForUserAsync`

#### 内联 `GET /confirmations/{id:guid}`
- 输入：id、confirmations、currentUser、ct
- 输出：200 + DTO；不存在 3001；非本人 3005
- 副作用：无
- 步骤：
  1. `GetAsync(id)`；null → 3001「Confirmation record does not exist.」。
  2. 若 `RequestedByUserId` 有值且 ≠ 当前用户 → 3005。
  3. Ok 返回 DTO。
- 分支与异常：DomainException 3001/3005/01002
- 调用：`confirmations.GetAsync`

#### 内联 `POST /confirmations/{id:guid}/confirm`
- 输入：id、confirmations、currentUser、ct
- 输出：200 + 确认后 DTO
- 副作用：服务内更新确认状态
- 步骤：`ConfirmAsync(id, userId)` → Ok
- 分支与异常：服务/未登录异常向上
- 调用：`confirmations.ConfirmAsync`

#### 内联 `POST /confirmations/{id:guid}/confirm-second-level`
- 输入：同上
- 输出：200 + DTO
- 副作用：二级确认状态变更
- 步骤：`ConfirmSecondLevelAsync(id, userId)` → Ok
- 分支与异常：服务层
- 调用：`confirmations.ConfirmSecondLevelAsync`

#### 内联 `POST /confirmations/{id:guid}/confirm-strict`
- 输入：同上
- 输出：200 + DTO
- 副作用：严格确认状态变更
- 步骤：`ConfirmStrictAsync(id, userId)` → Ok
- 分支与异常：服务层
- 调用：`confirmations.ConfirmStrictAsync`

#### 内联 `POST /confirmations/{id:guid}/reject`
- 输入：同上
- 输出：200 + DTO
- 副作用：拒绝确认
- 步骤：`RejectAsync(id, userId)` → Ok
- 分支与异常：服务层
- 调用：`confirmations.RejectAsync`

#### 内联 `GET /audit/{objectType}/{objectId:guid}`
- 输入：objectType、objectId、AuditVersionService、currentUser、ct
- 输出：200 + 时间线 object
- 副作用：无
- 步骤：校验登录；`GetTimelineAsync`；Ok
- 分支与异常：未登录 01002
- 调用：`audit.GetTimelineAsync`

#### 内联 `POST /audit/{auditVersionId:guid}/restore-preview`
- 输入：auditVersionId、audit、currentUser、ct
- 输出：200 + 恢复预览 object
- 副作用：无写库（预览）
- 步骤：校验登录；`PreviewRestoreAsync`；Ok
- 分支与异常：未登录/服务层
- 调用：`audit.PreviewRestoreAsync`

#### 内联 `GET /audit/export`
- 输入：可选 start/end、audit、currentUser、ct
- 输出：200 + 导出结果 object
- 副作用：无
- 步骤：校验登录；start 默认 MinValue、end 默认 MaxValue；`ExportAsync`；Ok
- 分支与异常：未登录
- 调用：`audit.ExportAsync`

#### `private static Guid RequireCurrentUserId(ICurrentUserService currentUser)`
- 输入：currentUser
- 输出：Guid UserId
- 副作用：无
- 步骤：`UserId` 有值则返回，否则 DomainException(01002, "未登录")
- 分支与异常：01002
- 调用：无

## 近逐行中文伪代码

1. 引入 ApiResponse、DomainException、Operations、Auth、Audit。
2. 静态类 `OperationsEndpoints`；`MapOperationsEndpoints` 建组 `/api/v1/operations` 并 `RequireAuthorization`。
3. **pending**：当前用户 → 列待确认 → Ok 列表。
4. **get by id**：取确认；不存在 3001；有请求人且非本人 3005；Ok。
5. **confirm / confirm-second-level / confirm-strict / reject**：分别调对应服务方法，Ok 返回 DTO。
6. **audit timeline**：登录校验 → 按 objectType+objectId 取时间线 → Ok。
7. **restore-preview**：登录校验 → 按 auditVersionId 预览恢复 → Ok。
8. **audit export**：登录校验 → 时间窗默认全量 → Export → Ok。
9. `RequireCurrentUserId`：无 UserId 抛 01002「未登录」。

## 关系边
```json
{
  "nodes": [
    {
      "id": "src/Pim.Api/Endpoints/OperationsEndpoints.cs",
      "label": "OperationsEndpoints",
      "path": "src/Pim.Api/Endpoints/OperationsEndpoints.cs",
      "doc": "docs/pseudocode/files/src/Pim.Api/Endpoints/OperationsEndpoints.cs.md",
      "layer": "api",
      "kind": "endpoint"
    }
  ],
  "edges": [
    { "from": "src/Pim.Api/Endpoints/OperationsEndpoints.cs", "to": "src/Pim.Infrastructure/Operations/OperationConfirmationService.cs", "type": "calls" },
    { "from": "src/Pim.Api/Endpoints/OperationsEndpoints.cs", "to": "src/Pim.Infrastructure/Audit/AuditVersionService.cs", "type": "calls" },
    { "from": "src/Pim.Api/Endpoints/OperationsEndpoints.cs", "to": "src/Pim.Infrastructure/Auth/CurrentUserService.cs", "type": "depends_on" },
    { "from": "src/Pim.Api/Endpoints/OperationsEndpoints.cs", "to": "src/Pim.Core/Operations", "type": "depends_on" },
    { "from": "src/Pim.Api/Endpoints/OperationsEndpoints.cs", "to": "src/Pim.Core/Common/ApiResponse.cs", "type": "depends_on" },
    { "from": "src/Pim.Api/Endpoints/OperationsEndpoints.cs", "to": "src/Pim.Core/Exceptions/DomainException.cs", "type": "depends_on" },
    { "from": "src/Pim.Api/Program.cs", "to": "src/Pim.Api/Endpoints/OperationsEndpoints.cs", "type": "calls" }
  ]
}
```
