# src/client-android/app/src/main/java/com/pim/app/ui/shell/PimShellActivity.kt

## 元信息
- 语言：Kotlin
- 程序集或包：com.pim.app.ui.shell（client-android app）
- 职责：Android Companion 壳 Activity：顶部导航切换嵌入 Web 路由或权限中心。
- 主要依赖：Jetpack Compose Material3、`PermissionCenterScreen`、`PimWebViewScreen`、Hilt `@AndroidEntryPoint`
- 被谁使用：通知/入口 Intent（`intentFor`）、遗留 endpoint shell 导航

## 函数级结构化伪代码

### PimShellActivity
#### onCreate(savedInstanceState: Bundle?)
- 输入：系统 Bundle；Intent 可带 `EXTRA_ROUTE`
- 输出：Unit
- 副作用：设置 Compose 内容
- 步骤：
  1. `super.onCreate`
  2. 读取 `EXTRA_ROUTE`，默认 `"/today"`
  3. `setContent { PimShellScreen(initialRoute) }`
- 分支与异常：无 extra 时用默认路由
- 调用：`setContent`、`PimShellScreen`

#### companion.intentFor(context, route): Intent
- 输入：Context、目标 route 字符串
- 输出：指向本 Activity 且带 EXTRA_ROUTE 的 Intent
- 副作用：无
- 步骤：
  1. `Intent(context, PimShellActivity::class.java).putExtra(EXTRA_ROUTE, route)`
- 分支与异常：无
- 调用：`Intent.putExtra`

### PimShellScreen
#### PimShellScreen(initialRoute: String = "/today")
- 输入：初始路由
- 输出：Composable UI
- 副作用：本地 `route` / `showPermissions` 状态
- 步骤：
  1. `rememberSaveable` 保存当前 route 与是否显示权限中心
  2. 定义中文标签到路径的 routes 列表（今日/任务/日历/报告/Outlook/Data Center/确认）
  3. MaterialTheme + Scaffold：顶栏标题与说明文案
  4. 横向滚动按钮行：权限中心 + 各业务路由
  5. 点击权限中心：`showPermissions = true`
  6. 点击路由：设置 route 并关闭权限中心
  7. 若 showPermissions → `PermissionCenterScreen`；否则 → `PimWebViewScreen(route)`
- 分支与异常：权限中心与 WebView 二选一
- 调用：`PermissionCenterScreen`、`PimWebViewScreen`

## 近逐行中文伪代码

1. [L1] 包 `com.pim.app.ui.shell`
2. [L3-31] 导入 Android/Compose/Hilt/PermissionCenter
3. [L33-34] `@AndroidEntryPoint` 类 `PimShellActivity`
4. [L35-41] `onCreate`：读 EXTRA_ROUTE，默认 /today，setContent
5. [L44-50] companion：EXTRA_ROUTE 常量与 `intentFor`
6. [L53-55] Composable `PimShellScreen`：saveable 的 route 与 showPermissions
7. [L57-65] routes 列表（中文标签 → 路径）
8. [L67-81] Scaffold 顶栏文案
9. [L82-107] 横向按钮：权限中心 + 路由切换
10. [L109-124] 条件渲染 PermissionCenter 或 PimWebViewScreen

## 关系边
```json
{
  "nodes": [
    {
      "id": "src/client-android/app/src/main/java/com/pim/app/ui/shell/PimShellActivity.kt",
      "label": "PimShellActivity",
      "path": "src/client-android/app/src/main/java/com/pim/app/ui/shell/PimShellActivity.kt",
      "doc": "docs/pseudocode/files/src/client-android/app/src/main/java/com/pim/app/ui/shell/PimShellActivity.kt.md",
      "layer": "client-android",
      "kind": "ui"
    }
  ],
  "edges": [
    { "from": "src/client-android/app/src/main/java/com/pim/app/ui/shell/PimShellActivity.kt", "to": "com.pim.app.ui.permissions.PermissionCenterScreen", "type": "calls" },
    { "from": "PimShellScreen", "to": "PimWebViewScreen", "type": "calls" }
  ]
}
```
