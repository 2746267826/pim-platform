# src/client-android/app/src/main/java/com/pim/app/ui/root/PimRootScreen.kt

## 元信息
- 语言：Kotlin
- 程序集或包：client-android / com.pim.app.ui.root
- 职责：Compose 根导航：底栏切换 Today/Tracks/Schedule/Status/Settings 五个目的地。
- 主要依赖：PimDestination、PimTheme、各业务 Screen
- 被谁使用：MainActivity / 壳层入口

## 函数级结构化伪代码

### PimRootScreen
#### PimRootScreen(initialDestination: PimDestination = Today)
- 输入：初始目的地（默认 Today）
- 输出：Unit（Composable UI）
- 副作用：rememberSaveable 保存 selected；点击切换 tab
- 步骤：
  1. `selected` 用 initialDestination.name 作 saveable key 初始化
  2. PimTheme 包裹 Scaffold
  3. bottomBar：遍历 PimDestination.entries 渲染 NavigationBarItem
  4. content：按 selected 分发 Today/Tracks/Schedule/Status/Settings Screen
  5. Status 屏额外提供 onOpenSettings / onOpenStatus 回调切换 selected
- 分支与异常：when(selected) 五分支
- 调用：TodayScreen、TracksScreen、SchedulePolicyScreen、StatusCenterScreen、SettingsScreen

## 近逐行中文伪代码

1. [L22-24] 组合函数；saveable 记住当前目的地。
2. [L26-39] PimTheme + Scaffold + NavigationBar 遍历条目。
3. [L40-41] 内容区应用 innerPadding。
4. [L42-52] when 分发五个屏幕；Status 可跳转 Settings/Status。

## 关系边
```json
{
  "nodes": [
    {
      "id": "src/client-android/app/src/main/java/com/pim/app/ui/root/PimRootScreen.kt",
      "label": "PimRootScreen",
      "path": "src/client-android/app/src/main/java/com/pim/app/ui/root/PimRootScreen.kt",
      "doc": "docs/pseudocode/files/src/client-android/app/src/main/java/com/pim/app/ui/root/PimRootScreen.kt.md",
      "layer": "client-android",
      "kind": "ui"
    }
  ],
  "edges": [
    {
      "from": "src/client-android/app/src/main/java/com/pim/app/ui/root/PimRootScreen.kt",
      "to": "src/client-android/app/src/main/java/com/pim/app/ui/root/PimDestination.kt",
      "type": "depends_on"
    },
    {
      "from": "src/client-android/app/src/main/java/com/pim/app/ui/root/PimRootScreen.kt",
      "to": "src/client-android/app/src/main/java/com/pim/app/ui/today/TodayScreen.kt",
      "type": "calls"
    },
    {
      "from": "src/client-android/app/src/main/java/com/pim/app/ui/root/PimRootScreen.kt",
      "to": "src/client-android/app/src/main/java/com/pim/app/ui/tracks/TracksScreen.kt",
      "type": "calls"
    },
    {
      "from": "src/client-android/app/src/main/java/com/pim/app/ui/root/PimRootScreen.kt",
      "to": "src/client-android/app/src/main/java/com/pim/app/ui/schedule/SchedulePolicyScreen.kt",
      "type": "calls"
    },
    {
      "from": "src/client-android/app/src/main/java/com/pim/app/ui/root/PimRootScreen.kt",
      "to": "src/client-android/app/src/main/java/com/pim/app/ui/status/StatusCenterScreen.kt",
      "type": "calls"
    },
    {
      "from": "src/client-android/app/src/main/java/com/pim/app/ui/root/PimRootScreen.kt",
      "to": "src/client-android/app/src/main/java/com/pim/app/ui/settings/SettingsScreen.kt",
      "type": "calls"
    },
    {
      "from": "src/client-android/app/src/main/java/com/pim/app/ui/root/PimRootScreen.kt",
      "to": "src/client-android/app/src/main/java/com/pim/app/ui/theme/PimTheme.kt",
      "type": "depends_on"
    }
  ]
}
```
