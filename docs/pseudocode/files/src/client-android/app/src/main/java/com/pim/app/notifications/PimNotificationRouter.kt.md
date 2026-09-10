# src/client-android/app/src/main/java/com/pim/app/notifications/PimNotificationRouter.kt

## 元信息
- 语言：Kotlin
- 程序集或包：com.pim.app.notifications（client-android app）
- 职责：按风险等级、在线状态与 action 将通知动作路由为在线执行、打开详情或待联网重试。
- 主要依赖：无项目内相对导入
- 被谁使用：通知点击/动作分发链路

## 函数级结构化伪代码

### NotificationRoute
#### 密封类层次
- 输入：无
- 输出：路由结果类型
- 副作用：无
- 步骤：
  1. `ExecuteOnline`：可在线直接执行
  2. `OpenDetail(detailUrl)`：打开确认/审计详情 URL
  3. `RetryWhenOnline`：离线时延后重试
- 分支与异常：无
- 调用：无

### PimNotificationRouter
#### route(action, riskLevel, confirmationId?, relatedObjectType?, relatedObjectId?, isOnline=true): NotificationRoute
- 输入：动作名、风险等级、可选确认/关联对象、是否在线
- 输出：`NotificationRoute` 之一
- 副作用：无
- 步骤：
  1. 若 `riskLevel` 属于高风险集合（L2/L3/L4、Medium、High）→ `OpenDetail(detailUrl(...))`
  2. 若 `!isOnline` → `RetryWhenOnline`
  3. 将 `action` trim+lowercase
  4. `dismiss`/`snooze`/`open`/`complete` → `ExecuteOnline`
  5. 其它 action → `OpenDetail("/confirmations")`
- 分支与异常：高风险优先于在线判断
- 调用：`detailUrl`

#### detailUrl(confirmationId?, relatedObjectType?, relatedObjectId?): String
- 输入：确认 ID 或关联对象类型/ID
- 输出：详情路径字符串
- 副作用：无
- 步骤：
  1. 有非空 `confirmationId` → `/confirmations/{id}`
  2. 有类型与 ID → `/audit/{type}/{id}`
  3. 否则 → `/confirmations`
- 分支与异常：空串视为无效
- 调用：`isNullOrBlank`

## 近逐行中文伪代码

1. [L1] 包 `com.pim.app.notifications`
2. [L3-7] 密封类 `NotificationRoute`：在线执行 / 打开详情 / 待联网重试
3. [L9] 类 `PimNotificationRouter`
4. [L10-16] 高风险等级集合（含 L2–L4 与 Medium/High）
5. [L18-25] `route` 入参：action、riskLevel、确认与关联对象、isOnline
6. [L26-28] 高风险 → 打开详情 URL
7. [L30-32] 离线 → RetryWhenOnline
8. [L34-38] 规范化 action；四类已知动作在线执行，否则打开 `/confirmations`
9. [L41-51] `detailUrl`：确认 ID 优先，其次 audit 路径，默认确认列表

## 关系边
```json
{
  "nodes": [
    {
      "id": "src/client-android/app/src/main/java/com/pim/app/notifications/PimNotificationRouter.kt",
      "label": "PimNotificationRouter",
      "path": "src/client-android/app/src/main/java/com/pim/app/notifications/PimNotificationRouter.kt",
      "doc": "docs/pseudocode/files/src/client-android/app/src/main/java/com/pim/app/notifications/PimNotificationRouter.kt.md",
      "layer": "client-android",
      "kind": "service"
    }
  ],
  "edges": []
}
```
