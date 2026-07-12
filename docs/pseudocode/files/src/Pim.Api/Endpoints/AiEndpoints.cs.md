# src/Pim.Api/Endpoints/AiEndpoints.cs

## 元信息
- 语言：C#
- 程序集或包：Pim.Api
- 职责：注册 `/api/v1/ai` 管理端点（admin）：状态、连通测试、请求日志分页/详情、用量汇总、提供商健康检查。
- 主要依赖：`IAiUsageService`、`IAiGateway`、`IAiProviderHealthService`；`ApiResponse`/`PagedResult`；`Ai*` DTO/枚举
- 被谁使用：`Program.cs` 调用 `MapAiEndpoints()`

## 函数级结构化伪代码

### AiEndpoints
#### `void MapAiEndpoints(this IEndpointRouteBuilder endpoints)`
- 输入：路由构建器
- 输出：无
- 副作用：注册一组需 `admin` 角色的 HTTP 端点
- 步骤：
  1. 映射组 `/api/v1/ai` + `RequireAuthorization(Roles=admin)`
  2. `GET /status` → `usage.GetStatusAsync` → `ApiResponse<AiStatusDto>.Ok`
  3. `POST /test` → 构造固定 `AiGatewayRequest`（module system、purpose ai.test、用户消息 “Reply with the word ok.”、MaxOutputTokens 32、MaxAttempts 1）→ `gateway.CompleteAsync` → `ApiResponse<AiResult>.Ok`
  4. `GET /requests`：查询参数过滤；`TryParseStatus` 失败则 400；否则 `ListRequestsAsync` 分页（默认 page=1,pageSize=50）
  5. `GET /requests/{id}`：`GetRequestDetailAsync`，空则 404，否则详情 DTO
  6. `GET /usage/summary`：可选 from/to → `GetUsageSummaryAsync`
  7. `POST /health-check`：`health.CheckAsync` 后返回最新 `GetStatusAsync`
- 分支与异常：状态非法 400；日志不存在 404；网关/健康检查异常由中间件处理
- 调用：`IAiUsageService`、`IAiGateway`、`IAiProviderHealthService`、`TryParseStatus`

#### `bool TryParseStatus(string? value, out AiRequestStatus? status)`
- 输入：可选状态查询字符串
- 输出：是否可接受；`out` 为 null（未指定）或解析后的枚举
- 副作用：无
- 步骤：
  1. 默认 `status=null`
  2. 空白 → 返回 true（表示未过滤）
  3. Trim + ToLowerInvariant 映射：succeeded/failed/blocked/timedout|timed_out/failedvalidation|failed_validation
  4. 未匹配 → `status=null` 且返回 false；匹配返回 true
- 分支与异常：无抛错
- 调用：无

## 近逐行中文伪代码

1. 引入 Authorization、Core.Ai、Core.Common、Infrastructure.Ai
2. 静态类 `AiEndpoints`
3. `MapAiEndpoints`：建 group `/api/v1/ai` 要求 admin
4. GET status：异步取用量服务状态并 Ok 包装
5. POST test：发一条最小 Complete 请求测 AI 网关
6. GET requests：绑定多过滤参数；解析 status 失败 BadRequest 中文错误；成功 ListRequests
7. GET requests/{id}：详情或 NotFound 中文
8. GET usage/summary：时间窗汇总
9. POST health-check：先健康检查再返回状态
10. `TryParseStatus`：空串视为合法未指定；否则小写映射到 `AiRequestStatus`；未知返回 false

## 关系边
```json
{
  "nodes": [
    {
      "id": "src/Pim.Api/Endpoints/AiEndpoints.cs",
      "label": "AiEndpoints",
      "path": "src/Pim.Api/Endpoints/AiEndpoints.cs",
      "doc": "docs/pseudocode/files/src/Pim.Api/Endpoints/AiEndpoints.cs.md",
      "layer": "api",
      "kind": "endpoint"
    }
  ],
  "edges": [
    { "from": "src/Pim.Api/Program.cs", "to": "src/Pim.Api/Endpoints/AiEndpoints.cs", "type": "calls" },
    { "from": "src/Pim.Api/Endpoints/AiEndpoints.cs", "to": "src/Pim.Core/Ai/IAiUsageService.cs", "type": "calls" },
    { "from": "src/Pim.Api/Endpoints/AiEndpoints.cs", "to": "src/Pim.Core/Ai/IAiGateway.cs", "type": "calls" },
    { "from": "src/Pim.Api/Endpoints/AiEndpoints.cs", "to": "src/Pim.Infrastructure/Ai/AiProviderHealthService.cs", "type": "calls" },
    { "from": "src/Pim.Api/Endpoints/AiEndpoints.cs", "to": "src/Pim.Core/Ai/AiDtos.cs", "type": "depends_on" },
    { "from": "src/Pim.Api/Endpoints/AiEndpoints.cs", "to": "src/Pim.Core/Ai/AiEnums.cs", "type": "depends_on" },
    { "from": "src/Pim.Api/Endpoints/AiEndpoints.cs", "to": "src/Pim.Core/Common/ApiResponse.cs", "type": "depends_on" },
    { "from": "src/Pim.Api/Endpoints/AiEndpoints.cs", "to": "src/Pim.Core/Common/PagedResult.cs", "type": "depends_on" }
  ]
}
```
