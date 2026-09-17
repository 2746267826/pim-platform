# src/client-android/app/src/test/java/com/pim/app/ui/settings/SettingsServerMutationTest.kt

## 元信息
- 语言：Kotlin
- 程序集或包：client-android tests / com.pim.app.ui.settings
- 职责：Robolectric 测试设置页服务器切换/登出：token 清理、commit 失败回滚、采集意图保持。
- 主要依赖：`SettingsViewModel`、`ServerSettingsStore`、`TokenManager`、`ServerBoundLoginCoordinator`、`TrackingSettingsStore`、`ConnectionProbeService`/`Store`、Robolectric
- 被谁使用：测试运行器

## 函数级结构化伪代码

### SettingsServerMutationTest
#### setUp / tearDown
- 输入：无
- 输出：无
- 副作用：Main dispatcher 替换；授权定位/通知；清空 prefs
- 步骤：setMain → grantPermissions → clear 四类 prefs；tearDown resetMain
- 分支与异常：无
- 调用：`Dispatchers.setMain`、`shadowOf.grantPermissions`

#### saveCommitFailureAfterTokenClearReloadsServerAWithoutSession
- 步骤：enqueue commit false → 改 URL 到 B → save 失败 → 状态回到 A、未登录、采集仍开
- 调用：`fixture`、`saveApiAddress`

#### successfulServerSwitchReloadsServerBAndClearedSessionWithoutChangingCollectionIntent
- 步骤：改 B → save 成功 → apiAddress=B、未登录、采集意图不变
- 调用：`saveApiAddress`

#### failedSessionClearAbortsUrlSwitch
- 步骤：failSessionClear + 脚本化 commit → save 失败 → URL 与登录态与真实 store 一致，采集意图保持
- 调用：`saveApiAddress`

#### collectionServerSaveFailureAfterTokenClearReloadsServerAWithoutSession
- 步骤：commit 失败后 setContinuousCollectionEnabled 触发路径 → 回到 A 无会话
- 调用：`setContinuousCollectionEnabled`

#### successfulCollectionServerSwitchKeepsIntentWhileNewServerSessionIsMissing
- 步骤：切 B 后开采集 → B 无会话但采集意图 true
- 调用：`setContinuousCollectionEnabled`

#### logoutClearsSessionWithoutChangingCollectionIntent
- 步骤：logout → 未登录且 continuousCollection 仍 true
- 调用：`logout`

#### fixture(failSessionClear)
- 输入：是否让会话 clear commit 失败
- 输出：Fixture(viewModel, stores, scripted prefs)
- 副作用：构造依赖图并预置 SERVER_A token 与采集开关
- 步骤：ScriptedCommit SharedPreferences → TokenManager/ServerSettings → Tracking → Coordinator → Probe → SettingsViewModel
- 调用：各 store/service 构造

### 辅助类型
#### SharedPreferencesContext / TestSecurePreferencesFactory / ScriptedCommitSharedPreferences
- 职责：固定 prefs 注入；按队列返回 commit 成功/失败
- 步骤：Editor.commit 消费队列，空则 true
- 调用：delegate SharedPreferences

## 近逐行中文伪代码

1. [L45-48] Robolectric + SDK34 + 实验协程
2. [L52-71] setUp/tearDown
3. [L73-159] 六个场景测试：commit 失败、切换成功、session clear 失败、采集路径切换、logout
4. [L161-218] fixture 组装依赖
5. [L220-232] successfulProbe 辅助（本文件测试未直接调用）
6. [L234-250] Fixture 与常量 SERVER_A/B
7. [L253-337] prefs 包装与脚本化 commit

## 关系边
```json
{
  "nodes": [
    {
      "id": "src/client-android/app/src/test/java/com/pim/app/ui/settings/SettingsServerMutationTest.kt",
      "label": "SettingsServerMutationTest",
      "path": "src/client-android/app/src/test/java/com/pim/app/ui/settings/SettingsServerMutationTest.kt",
      "doc": "docs/pseudocode/files/src/client-android/app/src/test/java/com/pim/app/ui/settings/SettingsServerMutationTest.kt.md",
      "layer": "tests",
      "kind": "test"
    }
  ],
  "edges": [
    {
      "from": "src/client-android/app/src/test/java/com/pim/app/ui/settings/SettingsServerMutationTest.kt",
      "to": "src/client-android/app/src/main/java/com/pim/app/ui/settings/SettingsViewModel.kt",
      "type": "tests"
    },
    {
      "from": "src/client-android/app/src/test/java/com/pim/app/ui/settings/SettingsServerMutationTest.kt",
      "to": "src/client-android/core/src/main/java/com/pim/core/auth/ServerBoundLoginCoordinator.kt",
      "type": "depends_on"
    },
    {
      "from": "src/client-android/app/src/test/java/com/pim/app/ui/settings/SettingsServerMutationTest.kt",
      "to": "src/client-android/app/src/main/java/com/pim/app/status/ConnectionProbeModels.kt",
      "type": "depends_on"
    }
  ]
}
```
