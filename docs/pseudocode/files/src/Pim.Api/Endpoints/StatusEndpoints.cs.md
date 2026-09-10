# src/Pim.Api/Endpoints/StatusEndpoints.cs

## 元信息
- 语言：C#
- 程序集或包：Pim.Api
- 职责：注册 `/api/v1/status` 下需授权的系统状态最小 API：摘要与详情。
- 主要依赖：
  - `Pim.Core.Common.ApiResponse`
  - `Pim.Core.Operations.ISystemStatusService`、`SystemStatusSummaryDto`、`SystemStatusDetailDto`
- 被谁使用：`Program.cs` → `app.MapStatusEndpoints()`

## 函数级结构化伪代码

### StatusEndpoints
#### `static void MapStatusEndpoints(this IEndpointRouteBuilder endpoints)`
- 输入：路由构建器
- 输出：void（副作用注册路由）
- 副作用：挂载两组 GET 处理程序，整组 `RequireAuthorization`
- 步骤：
  1. `MapGroup("/api/v1/status").RequireAuthorization()`。
  2. `GET /summary`：注入 `ISystemStatusService`，`GetSummaryAsync` → `ApiResponse<SystemStatusSummaryDto>.Ok`。
  3. `GET /`：`GetDetailAsync` → `ApiResponse<SystemStatusDetailDto>.Ok`。
- 分支与异常：服务层异常向上；未授权由管道处理
- 调用：`ISystemStatusService.GetSummaryAsync` / `GetDetailAsync`

#### 内联 `GET /summary`
- 输入：`ISystemStatusService`、`CancellationToken`
- 输出：200 + `ApiResponse<SystemStatusSummaryDto>`
- 副作用：只读查询
- 步骤：await 摘要 → Ok 包装
- 分支与异常：无本地分支
- 调用：`status.GetSummaryAsync`

#### 内联 `GET /`
- 输入：`ISystemStatusService`、`CancellationToken`
- 输出：200 + `ApiResponse<SystemStatusDetailDto>`
- 副作用：只读查询
- 步骤：await 详情 → Ok 包装
- 分支与异常：无本地分支
- 调用：`status.GetDetailAsync`

## 近逐行中文伪代码

1. 引入 `Pim.Core.Common`、`Pim.Core.Operations`。
2. 静态类 `StatusEndpoints`。
3. `MapStatusEndpoints`：建组 `/api/v1/status` 并要求授权。
4. 映射 `GET /summary`：调用 `GetSummaryAsync`，返回 `ApiResponse` 包装的摘要 DTO。
5. 映射 `GET /`：调用 `GetDetailAsync`，返回 `ApiResponse` 包装的详情 DTO。

## 关系边
```json
{
  "nodes": [
    {
      "id": "src/Pim.Api/Endpoints/StatusEndpoints.cs",
      "label": "StatusEndpoints",
      "path": "src/Pim.Api/Endpoints/StatusEndpoints.cs",
      "doc": "docs/pseudocode/files/src/Pim.Api/Endpoints/StatusEndpoints.cs.md",
      "layer": "api",
      "kind": "endpoint"
    }
  ],
  "edges": [
    { "from": "src/Pim.Api/Endpoints/StatusEndpoints.cs", "to": "src/Pim.Core/Operations/StatusDtos.cs", "type": "depends_on" },
    { "from": "src/Pim.Api/Endpoints/StatusEndpoints.cs", "to": "src/Pim.Core/Common/ApiResponse.cs", "type": "depends_on" },
    { "from": "src/Pim.Api/Endpoints/StatusEndpoints.cs", "to": "src/Pim.Infrastructure/Operations/SystemStatusService.cs", "type": "calls" },
    { "from": "src/Pim.Api/Program.cs", "to": "src/Pim.Api/Endpoints/StatusEndpoints.cs", "type": "calls" }
  ]
}
```
