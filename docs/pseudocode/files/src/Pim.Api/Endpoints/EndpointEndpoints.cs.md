# src/Pim.Api/Endpoints/EndpointEndpoints.cs

## 元信息
- 语言：C#
- 程序集或包：Pim.Api
- 职责：注册端点设备状态 HTTP 路由组 `/api/v1/endpoints`，委托 `EndpointStatusService` 完成列表、心跳、采集质量与通知动作。
- 主要依赖：`EndpointStatusService`、`Pim.Core.Endpoints` DTO/请求、`ApiResponse`、`RequireAuthorization`
- 被谁使用：API 启动时 `MapEndpointEndpoints` 映射

## 函数级结构化伪代码

### EndpointEndpoints
#### void MapEndpointEndpoints(this IEndpointRouteBuilder endpoints)
- 输入：路由构建器
- 输出：无（副作用注册路由）
- 副作用：映射 4 个需授权端点
- 步骤：
  1. `MapGroup("/api/v1/endpoints").RequireAuthorization()`
  2. `GET ""` → `endpointStatus.ListAsync` → `ApiResponse<IReadOnlyList<EndpointStatusDto>>.Ok`
  3. `POST "/{deviceId}/heartbeat"` → `UpsertHeartbeatAsync(deviceId, request)` → `ApiResponse<EndpointStatusDto>.Ok`
  4. `GET "/{deviceId}/collection-quality"` → `GetCollectionQualityAsync` → `ApiResponse<EndpointCollectionQualityDto>.Ok`
  5. `POST "/{deviceId}/notification-actions"` → `HandleNotificationActionAsync` → `ApiResponse<EndpointNotificationActionResponse>.Ok`
- 分支与异常：业务异常由服务/中间件处理；本文件无显式 try/catch
- 调用：`EndpointStatusService` 四个异步方法

## 近逐行中文伪代码

1. 引入 `Pim.Core.Common`、`Pim.Core.Endpoints`、`Pim.Infrastructure.Endpoints`
2. 命名空间 `Pim.Api.Endpoints`
3. 静态类 `EndpointEndpoints`
4. 扩展方法 `MapEndpointEndpoints` 接收 `IEndpointRouteBuilder`
5. 创建组 `/api/v1/endpoints` 并要求授权
6. GET 空路径：注入 `EndpointStatusService` 与取消令牌，列出状态并包装 Ok
7. POST `/{deviceId}/heartbeat`：绑定 `deviceId` 与 `EndpointHeartbeatRequest`，upsert 心跳后 Ok
8. GET `/{deviceId}/collection-quality`：按设备查采集质量并 Ok
9. POST `/{deviceId}/notification-actions`：处理通知动作请求并 Ok

## 关系边
```json
{
  "nodes": [
    {
      "id": "src/Pim.Api/Endpoints/EndpointEndpoints.cs",
      "label": "EndpointEndpoints",
      "path": "src/Pim.Api/Endpoints/EndpointEndpoints.cs",
      "doc": "docs/pseudocode/files/src/Pim.Api/Endpoints/EndpointEndpoints.cs.md",
      "layer": "api",
      "kind": "endpoint"
    }
  ],
  "edges": [
    { "from": "src/Pim.Api/Endpoints/EndpointEndpoints.cs", "to": "src/Pim.Infrastructure/Endpoints/EndpointStatusService.cs", "type": "calls" },
    { "from": "src/Pim.Api/Endpoints/EndpointEndpoints.cs", "to": "src/Pim.Core/Endpoints", "type": "depends_on" },
    { "from": "src/Pim.Api/Endpoints/EndpointEndpoints.cs", "to": "src/Pim.Core/Common", "type": "depends_on" }
  ]
}
```
