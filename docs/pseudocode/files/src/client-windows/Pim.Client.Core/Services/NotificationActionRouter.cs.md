# src/client-windows/Pim.Client.Core/Services/NotificationActionRouter.cs

## 元信息
- 语言：C#
- 程序集或包：Pim.Client.Core
- 职责：根据风险等级与动作名路由通知操作——高风险强制打开 Web 详情，低风险允许在线执行，未知动作拒绝。
- 主要依赖：BCL `HashSet`、`Uri`
- 被谁使用：Windows 客户端通知点击处理（App 层可能有同名包装）

## 函数级结构化伪代码

### NotificationActionRoute
#### record
- 输入：构造
- 输出：`Kind`（OpenDetailRequired|Executed|Rejected）、可选 `DetailUrl`、`Message`
- 副作用：无

### NotificationActionRouter
#### 静态字段 `HighRiskLevels`
- 步骤：忽略大小写 HashSet，含 L2/L3/L4 治理级别与 Medium/High 别名。

#### `NotificationActionRoute Route(action, riskLevel, confirmationId?, relatedObjectType?, relatedObjectId?)`
- 输入：动作名、风险等级、可选确认 Id 与关联对象
- 输出：`NotificationActionRoute`
- 副作用：无（纯函数）
- 步骤：
  1. 若 `riskLevel` 在高风险集合：返回 `OpenDetailRequired` + `BuildDetailUrl(...)` + 中文提示需 Web 审计确认。
  2. 否则将 `action` Trim+ToLowerInvariant。
  3. 若为 dismiss/snooze/open/complete：返回 `Executed`、无 URL、低风险可执行文案。
  4. 否则返回 `Rejected` 与「不支持的通知动作」文案。
- 分支与异常：高风险优先；未知动作拒绝；无抛异常
- 调用：`BuildDetailUrl`

#### `static string BuildDetailUrl(confirmationId?, relatedObjectType?, relatedObjectId?)`
- 输入：确认/关联标识
- 输出：相对路径 URL
- 步骤：
  1. 有 confirmationId → `/confirmations/{escaped}`。
  2. 否则 type+id 皆有 → `/audit/{type}/{id}`。
  3. 否则 `/confirmations`。
- 调用：`Uri.EscapeDataString`

## 近逐行中文伪代码

1. 命名空间 `Pim.Client.Core.Services`。
2. 记录 `NotificationActionRoute(Kind, DetailUrl, Message)`。
3. 类内静态高风险集合：L2PimFactChange、L3ExternalSourceOrWriteback、L4BatchOrDestructiveGovernance、Medium、High。
4. Route：高风险→OpenDetailRequired+详情 URL；否则规范化动作。
5. dismiss/snooze/open/complete→Executed；其它→Rejected。
6. BuildDetailUrl：优先确认 Id，其次审计对象路径，默认 /confirmations；路径段 URL 编码。

## 关系边
```json
{
  "nodes": [
    {
      "id": "src/client-windows/Pim.Client.Core/Services/NotificationActionRouter.cs",
      "label": "NotificationActionRouter",
      "path": "src/client-windows/Pim.Client.Core/Services/NotificationActionRouter.cs",
      "doc": "docs/pseudocode/files/src/client-windows/Pim.Client.Core/Services/NotificationActionRouter.cs.md",
      "layer": "client-windows",
      "kind": "service"
    }
  ],
  "edges": [
    { "from": "src/client-windows/Pim.Client.App/NotificationActionRouter.cs", "to": "src/client-windows/Pim.Client.Core/Services/NotificationActionRouter.cs", "type": "depends_on" }
  ]
}
```
