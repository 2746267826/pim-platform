# tests/Pim.UnitTests/ClientWindows/WindowsNotificationActionRouterTests.cs

## 元信息
- 语言：C#
- 程序集或包：Pim.UnitTests
- 职责：通知动作路由高/低风险行为；离线边界仅允许采集类上传。
- 主要依赖：`NotificationActionRouter`、`EndpointCollectionBoundaryService`
- 被谁使用：dotnet test

## 函数级结构化伪代码

### HighRiskActionsOpenWebAuditDetail
- 步骤：Route(confirm, L3ExternalSourceOrWriteback, id) → OpenDetailRequired + `/confirmations/{id}`

### LowRiskActionsExecuteDirectly
- 步骤：Route(dismiss, L1LowRiskAction) → Executed；DetailUrl null

### OfflineBoundaryOnlyAllowsCollectionUploads
- 步骤：CanQueueOffline 对 collection-upload/pc-activity/window-context/upload-retry 为 true；对 task/event/habit/confirmation/report/outlook/restore 类为 false

## 近逐行中文伪代码

1. [L8-17] 高风险打开详情
2. [L19-28] 低风险直接执行
3. [L30-46] 离线队列白名单/黑名单

## 关系边
```json
{
  "nodes": [
    {
      "id": "tests/Pim.UnitTests/ClientWindows/WindowsNotificationActionRouterTests.cs",
      "label": "WindowsNotificationActionRouterTests",
      "path": "tests/Pim.UnitTests/ClientWindows/WindowsNotificationActionRouterTests.cs",
      "doc": "docs/pseudocode/files/tests/Pim.UnitTests/ClientWindows/WindowsNotificationActionRouterTests.cs.md",
      "layer": "tests",
      "kind": "test"
    }
  ],
  "edges": [
    { "from": "tests/Pim.UnitTests/ClientWindows/WindowsNotificationActionRouterTests.cs", "to": "src/client-windows/Pim.Client.Core/Services/NotificationActionRouter.cs", "type": "tests" },
    { "from": "tests/Pim.UnitTests/ClientWindows/WindowsNotificationActionRouterTests.cs", "to": "src/client-windows/Pim.Client.Core/Services/EndpointCollectionBoundaryService.cs", "type": "tests" }
  ]
}
```
