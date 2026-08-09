# src/client-android/app/src/main/java/com/pim/app/status/StatusPermissionNavigator.kt

## 元信息
- 语言：Kotlin
- 程序集或包：client-android / com.pim.app.status
- 职责：根据 StatusIssue.code 跳转到系统设置页（使用情况访问、通知、应用详情），失败时回退应用详情。
- 主要依赖：Context、Intent、Settings、Uri、Build
- 被谁使用：状态中心权限问题操作入口

## 函数级结构化伪代码

### StatusPermissionNavigator (object)
#### open(context, issue)
- 输入：Context、StatusIssue
- 输出：无
- 副作用：startActivity
- 步骤：
  1. intentFor + FLAG_ACTIVITY_NEW_TASK
  2. try startActivity
  3. catch ActivityNotFoundException → appDetailsIntent 再启动

#### intentFor(context, issue) → Intent
- "usage-access-missing" → ACTION_USAGE_ACCESS_SETTINGS
- "notification-permission-missing" → notificationSettingsIntent
- else → appDetailsIntent

#### notificationSettingsIntent(context)
- SDK ≥ O：ACTION_APP_NOTIFICATION_SETTINGS + EXTRA_APP_PACKAGE
- 否则 appDetailsIntent

#### appDetailsIntent(context)
- ACTION_APPLICATION_DETAILS_SETTINGS + package Uri

## 近逐行中文伪代码

1. object StatusPermissionNavigator。
2. open：构造带 NEW_TASK 的 Intent，启动；捕获 ActivityNotFound 回退应用详情。
3. intentFor：按 issue.code 分支到使用情况、通知或默认应用详情。
4. notificationSettingsIntent：Oreo+ 用应用通知设置，否则应用详情。
5. appDetailsIntent：package scheme 指向本应用详情页。

## 关系边
```json
{
  "nodes": [
    {
      "id": "src/client-android/app/src/main/java/com/pim/app/status/StatusPermissionNavigator.kt",
      "label": "StatusPermissionNavigator",
      "path": "src/client-android/app/src/main/java/com/pim/app/status/StatusPermissionNavigator.kt",
      "doc": "docs/pseudocode/files/src/client-android/app/src/main/java/com/pim/app/status/StatusPermissionNavigator.kt.md",
      "layer": "client-android",
      "kind": "service"
    }
  ],
  "edges": [
    { "from": "src/client-android/app/src/main/java/com/pim/app/status/StatusPermissionNavigator.kt", "to": "src/client-android/app/src/main/java/com/pim/app/status/StatusIssue.kt", "type": "depends_on" }
  ]
}
```
