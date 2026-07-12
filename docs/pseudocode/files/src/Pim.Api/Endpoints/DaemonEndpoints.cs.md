# src/Pim.Api/Endpoints/DaemonEndpoints.cs

## 元信息
- 语言：C#
- 程序集或包：Pim.Api
- 职责：注册 `/api/v1/daemon` 下需鉴权的心跳端点，将 Windows 守护进程心跳委托给 `IDaemonHeartbeatService` 并包装为 `ApiResponse`。
- 主要依赖：
  - `Pim.Core.Common.ApiResponse`
  - `Pim.Core.Operations`（`DaemonHeartbeatRequest`、`DaemonHeartbeatDto`、`IDaemonHeartbeatService`）
  - ASP.NET Core Minimal API
- 被谁使用：`Program.cs` → `app.MapDaemonEndpoints()`

## 函数级结构化伪代码

### DaemonEndpoints
#### `static void MapDaemonEndpoints(this IEndpointRouteBuilder endpoints)`
- 输入：路由构建器
- 输出：void（副作用注册路由）
- 副作用：挂载 `POST /api/v1/daemon/heartbeat`，并要求授权
- 步骤：
  1. `MapGroup("/api/v1/daemon").RequireAuthorization()`。
  2. 注册 `POST /heartbeat` 内联异步委托。
- 分支与异常：handler 内由服务抛出
- 调用：Minimal API MapPost

#### 内联 `POST /heartbeat`
- 输入：`DaemonHeartbeatRequest`、`IDaemonHeartbeatService`、`CancellationToken`
- 输出：200 + `ApiResponse<DaemonHeartbeatDto>`
- 副作用：经服务 upsert 心跳记录
- 步骤：
  1. 调用 `heartbeats.UpsertAsync(request, ct)`。
  2. 返回 `Results.Ok(ApiResponse.Ok(result))`。
- 分支与异常：未授权由中间件处理；服务异常向上
- 调用：`IDaemonHeartbeatService.UpsertAsync`

## 近逐行中文伪代码

1. 引入 `Pim.Core.Common` 与 `Pim.Core.Operations`。
2. 静态类 `DaemonEndpoints`；扩展方法 `MapDaemonEndpoints`。
3. 建组 `/api/v1/daemon` 并 `RequireAuthorization`。
4. `POST /heartbeat`：注入请求体、心跳服务、取消令牌。
5. `await heartbeats.UpsertAsync` → 包装 `ApiResponse.Ok` → `Results.Ok`。

## 关系边
```json
{
  "nodes": [
    {
      "id": "src/Pim.Api/Endpoints/DaemonEndpoints.cs",
      "label": "DaemonEndpoints",
      "path": "src/Pim.Api/Endpoints/DaemonEndpoints.cs",
      "doc": "docs/pseudocode/files/src/Pim.Api/Endpoints/DaemonEndpoints.cs.md",
      "layer": "api",
      "kind": "endpoint"
    }
  ],
  "edges": [
    { "from": "src/Pim.Api/Endpoints/DaemonEndpoints.cs", "to": "src/Pim.Core/Operations/DaemonHeartbeatDtos.cs", "type": "depends_on" },
    { "from": "src/Pim.Api/Endpoints/DaemonEndpoints.cs", "to": "src/Pim.Core/Common/ApiResponse.cs", "type": "depends_on" },
    { "from": "src/Pim.Api/Endpoints/DaemonEndpoints.cs", "to": "src/Pim.Infrastructure/Operations/DaemonHeartbeatService.cs", "type": "calls" },
    { "from": "src/Pim.Api/Program.cs", "to": "src/Pim.Api/Endpoints/DaemonEndpoints.cs", "type": "calls" }
  ]
}
```
