# src/client-android/app/src/main/java/com/pim/app/ui/status/StatusCenterScreen.kt

## 元信息
- 语言：Kotlin / Jetpack Compose
- 程序集或包：client-android / com.pim.app.ui.status
- 职责：状态中心界面：周期刷新连接探测、展示 issues/API/权限/前台服务/上传队列/诊断，并路由问题操作。
- 主要依赖：StatusCenterViewModel、StatusActionRouter、StatusIssue、PimSection、collectAsStateWithLifecycle
- 被谁使用：导航到状态中心的 Shell/Root 路由

## 函数级结构化伪代码

### StatusCenterScreen
#### StatusCenterScreen(modifier, onOpenSettings, onOpenStatus, viewModel)
- 输入：Modifier；打开设置/状态回调；默认 hiltViewModel
- 输出：Composable
- 副作用：LaunchedEffect 循环调用 refreshConnectionForVisibleScreen
- 步骤：
  1. LaunchedEffect(viewModel)：while isActive 调 refreshConnectionForVisibleScreen，delay>0 则 delay 否则 yield
  2. collectAsStateWithLifecycle 取 state
  3. 渲染 StatusCenterContent
  4. onIssueAction：viewModel.onIssueAction(issue) → StatusActionRouter.route
     - OpenSettings / OpenPermissions → onOpenSettings()
     - TriggerSync → viewModel.syncNow()
     - StayOnStatus → onOpenStatus()
     - None → 无操作
- 分支与异常：按 StatusActionRoute 分支
- 调用：StatusCenterViewModel、StatusActionRouter

### StatusCenterContent
#### StatusCenterContent(state, modifier, onIssueAction, onOpenStatus)
- 输入：StatusCenterState 与回调
- 输出：Composable
- 副作用：无（纯展示）
- 步骤：
  1. 可滚动 Column，标题「状态中心」
  2. PimSection「需要处理」：issues 空则提示无阻塞；否则 StatusIssueRow
  3. 「API 与登录」：地址、格式有效性、登录/过期
  4. 「权限」：通知/精确定位/后台定位/使用情况/运动识别 → toStatusText
  5. 「前台服务」：连续采集、服务运行、策略模式与档位
  6. 「上传队列」：各 pending 计数；Button「立即同步」仅 pending>0 可用，点击 onOpenStatus
  7. 若 lastLogMessage 为同步完成且 pending=0，显示上次同步文案
  8. 「最近诊断」：丢弃原因、心跳、最近错误、最近 5 条日志
- 分支与异常：issues 空/非空；pending 是否启用按钮
- 调用：PimSection、StatusIssueRow

### StatusIssueRow
#### StatusIssueRow(issue, onAction)
- 输入：StatusIssue、动作回调
- 输出：Composable
- 步骤：Row 左 Column 显示 severity.label + title + message；右 TextButton actionLabel
- 调用：StatusSeverity.label

### Boolean.toStatusText / StatusSeverity.label
- true→已就绪 false→未就绪；Info/Warning/Critical → 提示/警告/阻塞

## 近逐行中文伪代码

1. 导入 Compose、Hilt ViewModel、状态模型与 PimSection。
2. StatusCenterScreen：循环刷新连接；收集 state；问题动作经 Router 分发。
3. StatusCenterContent：六大分区展示 snapshot。
4. 队列区「立即同步」按钮绑定 onOpenStatus 且依赖 pendingUploadTotal。
5. StatusIssueRow 展示严重度标签与操作按钮。
6. 布尔与 Severity 转中文文案辅助函数。

## 关系边
```json
{
  "nodes": [
    {
      "id": "src/client-android/app/src/main/java/com/pim/app/ui/status/StatusCenterScreen.kt",
      "label": "StatusCenterScreen",
      "path": "src/client-android/app/src/main/java/com/pim/app/ui/status/StatusCenterScreen.kt",
      "doc": "docs/pseudocode/files/src/client-android/app/src/main/java/com/pim/app/ui/status/StatusCenterScreen.kt.md",
      "layer": "client-android",
      "kind": "ui"
    }
  ],
  "edges": [
    { "from": "src/client-android/app/src/main/java/com/pim/app/ui/status/StatusCenterScreen.kt", "to": "src/client-android/app/src/main/java/com/pim/app/ui/components/PimSection.kt", "type": "depends_on" },
    { "from": "src/client-android/app/src/main/java/com/pim/app/ui/status/StatusCenterScreen.kt", "to": "src/client-android/app/src/main/java/com/pim/app/status/StatusActionRouter.kt", "type": "calls" },
    { "from": "src/client-android/app/src/main/java/com/pim/app/ui/status/StatusCenterScreen.kt", "to": "src/client-android/app/src/main/java/com/pim/app/status/StatusIssue.kt", "type": "depends_on" },
    { "from": "src/client-android/app/src/main/java/com/pim/app/ui/status/StatusCenterScreen.kt", "to": "src/client-android/app/src/main/java/com/pim/app/ui/status/StatusCenterViewModel.kt", "type": "depends_on" }
  ]
}
```
