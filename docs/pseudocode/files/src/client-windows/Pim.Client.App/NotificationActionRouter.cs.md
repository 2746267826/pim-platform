# src/client-windows/Pim.Client.App/NotificationActionRouter.cs

## 元信息
- 语言：C#
- 程序集或包：Pim.Client.App
- 职责：WPF 层通知动作路由包装：本地路由决策后，对可执行动作调用 API 回写结果。
- 主要依赖：
  - `Pim.Client.Core.Services.NotificationActionRouter`（核心路由）
  - `ApiClient`
  - `EndpointNotificationActionRequestDto` / `NotificationActionRoute`
- 被谁使用：托盘/Toast 通知点击处理、主壳层

## 函数级结构化伪代码

### NotificationActionRouter（App 层）
#### 构造函数
- 输入：coreRouter、apiClient
- 输出：实例
- 副作用：保存依赖字段
- 步骤：
  1. 赋值 `_coreRouter`、`_apiClient`。
- 分支与异常：无
- 调用：无

#### `Route(action, riskLevel, confirmationId?, relatedObjectType?, relatedObjectId?)`
- 输入：动作名、风险级别、可选确认/关联对象
- 输出：`NotificationActionRoute`
- 副作用：无
- 步骤：
  1. 原样委托 `_coreRouter.Route(...)`。
- 分支与异常：无
- 调用：`NotificationActionRouter.Route`（Core）

#### `RouteToastActionAsync(deviceId, request, ct)`
- 输入：设备 ID、通知动作请求 DTO、取消令牌
- 输出：`NotificationActionRoute`（可能被服务端结果覆盖）
- 副作用：可能发起 HTTP 通知动作 API
- 步骤：
  1. 用 request 字段调用本地 `Route`。
  2. 若 `route.Kind != "Executed"` → 直接返回本地路由结果。
  3. 否则 `SendEndpointNotificationActionAsync`。
  4. 有 `response.Data` → 用服务端 Result/DetailUrl/Message 构造新 Route；否则保留本地 route。
- 分支与异常：API 异常由 ApiClient 向上抛
- 调用：`Route`、`ApiClient.SendEndpointNotificationActionAsync`

## 近逐行中文伪代码

1. App 层类持有 Core 路由器与 ApiClient。
2. `Route` 纯转发 Core。
3. Toast 异步：先本地路由；仅 Kind=Executed 时打 API。
4. 成功响应则用服务端 result/detailUrl/message 覆盖返回；无数据则用本地结果。

## 关系边
```json
{
  "nodes": [
    {
      "id": "src/client-windows/Pim.Client.App/NotificationActionRouter.cs",
      "label": "NotificationActionRouter",
      "path": "src/client-windows/Pim.Client.App/NotificationActionRouter.cs",
      "doc": "docs/pseudocode/files/src/client-windows/Pim.Client.App/NotificationActionRouter.cs.md",
      "layer": "client-windows",
      "kind": "service"
    }
  ],
  "edges": [
    { "from": "src/client-windows/Pim.Client.App/NotificationActionRouter.cs", "to": "src/client-windows/Pim.Client.Core/Services/NotificationActionRouter.cs", "type": "calls" },
    { "from": "src/client-windows/Pim.Client.App/NotificationActionRouter.cs", "to": "src/client-windows/Pim.Client.Core/Services/ApiClient.cs", "type": "calls" },
    { "from": "src/client-windows/Pim.Client.App/NotificationActionRouter.cs", "to": "src/client-windows/Pim.Client.Core/Models/EndpointDtos.cs", "type": "depends_on" }
  ]
}
```
