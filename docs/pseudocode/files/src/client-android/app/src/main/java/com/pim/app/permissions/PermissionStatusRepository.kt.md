# src/client-android/app/src/main/java/com/pim/app/permissions/PermissionStatusRepository.kt

## 元信息
- 语言：Kotlin
- 程序集或包：client-android / com.pim.app.permissions
- 职责：汇总当前权限快照（通知、精确定位、后台定位、使用情况访问、活动识别），供状态中心等展示。
- 主要依赖：Context、UsageAccessChecker、ContextCompat、Build.VERSION
- 被谁使用：状态/权限相关 UI 与服务

## 函数级结构化伪代码

### PermissionStatusRepository
#### snapshot() → PermissionStatusSnapshot
- 输入：无
- 输出：PermissionStatusSnapshot
- 副作用：无（只读权限）
- 步骤：
  1. 查 ACCESS_FINE_LOCATION → preciseLocationGranted
  2. hasNotificationPermission
  3. hasBackgroundLocationPermission(precise)
  4. usageAccessChecker.hasUsageAccess()
  5. hasActivityRecognitionPermission
  6. 组装 Snapshot

#### hasNotificationPermission()
- SDK < TIRAMISU 视为已授权；否则检查 POST_NOTIFICATIONS

#### hasBackgroundLocationPermission(preciseLocationGranted)
- SDK ≥ Q：检查 ACCESS_BACKGROUND_LOCATION
- 更低版本：等同于 preciseLocationGranted

#### hasActivityRecognitionPermission()
- SDK < Q 视为已授权；否则检查 ACTIVITY_RECOGNITION

#### isGranted(permission)
- ContextCompat.checkSelfPermission == PERMISSION_GRANTED

## 近逐行中文伪代码

1. @Singleton 注入 ApplicationContext 与 UsageAccessChecker。
2. snapshot：读精确定位权限后构造五字段快照。
3. 通知：Android 13 以下恒 true，否则 POST_NOTIFICATIONS。
4. 后台定位：Android 10+ 查 BACKGROUND，否则回退精确权限。
5. 活动识别：Android 10 以下恒 true，否则 ACTIVITY_RECOGNITION。
6. isGranted：PackageManager.PERMISSION_GRANTED 判断。

## 关系边
```json
{
  "nodes": [
    {
      "id": "src/client-android/app/src/main/java/com/pim/app/permissions/PermissionStatusRepository.kt",
      "label": "PermissionStatusRepository",
      "path": "src/client-android/app/src/main/java/com/pim/app/permissions/PermissionStatusRepository.kt",
      "doc": "docs/pseudocode/files/src/client-android/app/src/main/java/com/pim/app/permissions/PermissionStatusRepository.kt.md",
      "layer": "client-android",
      "kind": "service"
    }
  ],
  "edges": [
    { "from": "src/client-android/app/src/main/java/com/pim/app/permissions/PermissionStatusRepository.kt", "to": "src/client-android/app/src/main/java/com/pim/app/mobile/usage/UsageAccessChecker.kt", "type": "depends_on" },
    { "from": "src/client-android/app/src/main/java/com/pim/app/permissions/PermissionStatusRepository.kt", "to": "src/client-android/app/src/main/java/com/pim/app/status/PermissionStatusSnapshot", "type": "depends_on" }
  ]
}
```
