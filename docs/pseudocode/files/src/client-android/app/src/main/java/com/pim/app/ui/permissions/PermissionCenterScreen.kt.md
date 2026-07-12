# src/client-android/app/src/main/java/com/pim/app/ui/permissions/PermissionCenterScreen.kt

## 元信息
- 语言：Kotlin / Jetpack Compose
- 程序集或包：client-android / com.pim.app.ui.permissions
- 职责：权限中心只读 UI：展示使用情况/定位/通知授权、设备状态、上传队列与 collection quality。
- 主要依赖：Compose Material3、UsageStatsManager、ContextCompat
- 被谁使用：导航到权限中心的壳层

## 函数级结构化伪代码

### PermissionCenterScreen(modifier, uploadQueueCount, collectionQuality)
- 输入：修饰符；上传队列条数默认 0；质量文案默认 waiting
- 输出：Compose UI
- 副作用：读取系统权限与 usage stats
- 步骤：
  1. LocalContext
  2. Column 标题「权限中心」
  3. 行：使用情况 / 定位 / 通知 / 设备状态 / 上传队列 / collection quality
  4. 底部说明：复杂操作走嵌入 Web，本地只缓存采集上传
- 调用：PermissionRow、hasUsageAccess、hasFineLocationPermission、hasNotificationPermission

### PermissionRow(label, value) [private Composable]
- Surface + Row 左右展示 label/value

### hasUsageAccess(context)
- 取 USAGE_STATS_SERVICE；queryUsageStats 近 24h 非空则视为已授权；失败 false

### hasFineLocationPermission(context)
- ACCESS_FINE_LOCATION == GRANTED

### hasNotificationPermission(context)
- API < TIRAMISU 视为已授权；否则 POST_NOTIFICATIONS

## 近逐行中文伪代码

1. Composable 取 LocalContext。
2. 纵向列表展示各权限状态「已授权/未授权」。
3. 设备状态固定「可采集」。
4. 上传队列显示条数；collection quality 用入参文案。
5. 提示复杂操作走 Web。
6. PermissionRow 用 Surface 卡片样式。
7. 使用情况：queryUsageStats 有结果即有权限。
8. 定位：checkSelfPermission FINE。
9. 通知：Tiramisu 以下默认通过，否则 POST_NOTIFICATIONS。

## 关系边
```json
{
  "nodes": [
    {
      "id": "src/client-android/app/src/main/java/com/pim/app/ui/permissions/PermissionCenterScreen.kt",
      "label": "PermissionCenterScreen",
      "path": "src/client-android/app/src/main/java/com/pim/app/ui/permissions/PermissionCenterScreen.kt",
      "doc": "docs/pseudocode/files/src/client-android/app/src/main/java/com/pim/app/ui/permissions/PermissionCenterScreen.kt.md",
      "layer": "client-android",
      "kind": "ui"
    }
  ],
  "edges": []
}
```
