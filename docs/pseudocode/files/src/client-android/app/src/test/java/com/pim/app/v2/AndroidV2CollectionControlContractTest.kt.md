# src/client-android/app/src/test/java/com/pim/app/v2/AndroidV2CollectionControlContractTest.kt

## 元信息
- 语言：Kotlin
- 程序集或包：client-android tests / com.pim.app.v2
- 职责：源码契约测试：持续采集开关须经 ViewModel + TrackingSettingsStore 持久化；refresh/logout 不破坏采集意图；启停顺序与前台定位权限/运动过渡注册顺序。
- 主要依赖：仓库内 SettingsScreen/ViewModel、ForegroundLocationService 源文件文本
- 被谁使用：Android V2 契约测试

## 函数级结构化伪代码

### AndroidV2CollectionControlContractTest
#### settingsSwitchUsesViewModelControllerAndPersistedTrackingState
- 输入：读取 SettingsScreen.kt / SettingsViewModel.kt
- 输出：禁止 rememberSaveable 本地开关；必须 checked=state + viewModel setter；VM 依赖 TrackingSettingsStore/ForegroundLocationController/PermissionStatusRepository 等
- 调用：repoFile、readText、assertTrue/False

#### settingsRefreshAndLogoutPreserveDurableCollectionIntentAcrossBlockers
- 输入：截取 refresh/logout 函数体
- 输出：refresh 用 persistedCollectionEnabled 显示意图且不 set false；logout 保留 collectionIntent，不 stop/禁用
- 调用：substringAfter/Before

#### enablingCollectionSwitchesServerBeforeCheckingTheBoundSession
- 输入：setContinuousCollectionEnabled 函数内索引
- 输出：setBaseUrl < hasCurrentServerSession < setContinuousCollectionEnabled(true)
- 调用：indexOf

#### foregroundLocationServiceChecksRequiredPermissionsBeforeStartingLocationForeground
- 输入：ForegroundLocationService 源
- 输出：hasRequiredLocationPermissions 在 startForeground 之前；含 FINE/BACKGROUND 权限检查
- 调用：indexOf

#### foregroundLocationServiceRegistersMotionTransitionsWhileCollecting
- 输入：同上
- 输出：registerActivityTransitions 在 requestLocationUpdates 前；存在 unregister
- 调用：indexOf

#### repoFile(vararg parts)
- 输入：相对路径段
- 输出：向上找仓库根后的 File
- 步骤：canonicalFile 向上 parent 直到存在
- 分支：找不到 error

## 近逐行中文伪代码

1. [L8-51] 开关不走本地状态，走 VM 持久化与控制器。
2. [L53-89] refresh/logout 保持采集意图。
3. [L91-122] 启用采集：切服务器 → 绑会话检查 → 再 enable。
4. [L124-146] 前台服务权限门控先于 startForeground。
5. [L148-169] 运动过渡注册/注销顺序。
6. [L171-179] repoFile 向上定位源文件。

## 关系边
```json
{
  "nodes": [
    {
      "id": "src/client-android/app/src/test/java/com/pim/app/v2/AndroidV2CollectionControlContractTest.kt",
      "label": "AndroidV2CollectionControlContractTest",
      "path": "src/client-android/app/src/test/java/com/pim/app/v2/AndroidV2CollectionControlContractTest.kt",
      "doc": "docs/pseudocode/files/src/client-android/app/src/test/java/com/pim/app/v2/AndroidV2CollectionControlContractTest.kt.md",
      "layer": "tests",
      "kind": "test"
    }
  ],
  "edges": [
    {
      "from": "src/client-android/app/src/test/java/com/pim/app/v2/AndroidV2CollectionControlContractTest.kt",
      "to": "src/client-android/app/src/main/java/com/pim/app/ui/settings/SettingsScreen.kt",
      "type": "tests"
    },
    {
      "from": "src/client-android/app/src/test/java/com/pim/app/v2/AndroidV2CollectionControlContractTest.kt",
      "to": "src/client-android/app/src/main/java/com/pim/app/ui/settings/SettingsViewModel.kt",
      "type": "tests"
    },
    {
      "from": "src/client-android/app/src/test/java/com/pim/app/v2/AndroidV2CollectionControlContractTest.kt",
      "to": "src/client-android/app/src/main/java/com/pim/app/location/service/ForegroundLocationService.kt",
      "type": "tests"
    }
  ]
}
```
