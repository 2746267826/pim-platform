# src/client-web/src/api/endpoints.ts

## 元信息
- 语言：TypeScript
- 程序集或包：client-web
- 职责：端点（endpoint）管理 HTTP API 封装：路径工厂 + 列表/心跳/采集质量/通知动作。
- 主要依赖：`./client` 的 `apiGet`/`apiPost`；`../types` 中 Endpoint 相关类型
- 被谁使用：Web 端端点状态页、通知动作 UI

## 函数级结构化伪代码

### endpointApiPaths
#### list / heartbeat / collectionQuality / notificationActions
- 输入：部分路径需 `deviceId`
- 输出：API 路径字符串（deviceId 经 `encodeURIComponent`）
- 副作用：无
- 步骤：
  1. `list` → `/endpoints`。
  2. `heartbeat(deviceId)` → `/endpoints/{id}/heartbeat`。
  3. `collectionQuality(deviceId)` → `.../collection-quality`。
  4. `notificationActions(deviceId)` → `.../notification-actions`。
- 分支与异常：无
- 调用：`encodeURIComponent`

### listEndpointStatuses()
- 输入：无
- 输出：`EndpointStatus[]`（解包 `ApiResponse.data`）
- 副作用：HTTP GET
- 步骤：`apiGet` list 路径；返回 `r.data`。
- 分支与异常：透传 client 异常
- 调用：`apiGet`

### heartbeatEndpoint(deviceId, data)
- 输入：设备 Id、`EndpointHeartbeatRequest`
- 输出：`EndpointStatus`
- 副作用：HTTP POST 心跳
- 步骤：`apiPost` heartbeat 路径与 body；返回 `r.data`。
- 分支与异常：透传
- 调用：`apiPost`

### getEndpointCollectionQuality(deviceId)
- 输入：设备 Id
- 输出：`EndpointCollectionQuality`
- 副作用：HTTP GET
- 步骤：`apiGet` collectionQuality 路径；返回 `r.data`。
- 分支与异常：透传
- 调用：`apiGet`

### handleEndpointNotificationAction(deviceId, data)
- 输入：设备 Id、`EndpointNotificationActionRequest`
- 输出：`EndpointNotificationActionResponse`
- 副作用：HTTP POST 通知动作
- 步骤：`apiPost` notificationActions 路径；返回 `r.data`。
- 分支与异常：透传
- 调用：`apiPost`

## 近逐行中文伪代码

1. 从 client 与 types 引入 HTTP 与 DTO 类型。
2. `endpointApiPaths` 四条路径工厂，deviceId 编码。
3. `listEndpointStatuses` GET 列表解包 data。
4. `heartbeatEndpoint` POST 心跳。
5. `getEndpointCollectionQuality` GET 质量。
6. `handleEndpointNotificationAction` POST 通知动作。

## 关系边
```json
{
  "nodes": [
    {
      "id": "src/client-web/src/api/endpoints.ts",
      "label": "endpoints",
      "path": "src/client-web/src/api/endpoints.ts",
      "doc": "docs/pseudocode/files/src/client-web/src/api/endpoints.ts.md",
      "layer": "client-web",
      "kind": "other"
    }
  ],
  "edges": [
    { "from": "src/client-web/src/api/endpoints.ts", "to": "src/client-web/src/api/client.ts", "type": "calls" },
    { "from": "src/client-web/src/api/endpoints.ts", "to": "src/client-web/src/types", "type": "depends_on" },
    { "from": "src/client-web/src/api/endpoints.ts", "to": "src/Pim.Api/Endpoints/EndpointEndpoints.cs", "type": "http" }
  ]
}
```
