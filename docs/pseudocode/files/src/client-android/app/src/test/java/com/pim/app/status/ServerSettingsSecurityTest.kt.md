# src/client-android/app/src/test/java/com/pim/app/status/ServerSettingsSecurityTest.kt

## 元信息
- 语言：Kotlin (Robolectric)
- 程序集或包：client-android app test
- 职责：验证 ServerSettingsStore 切换 baseUrl 时会话绑定安全：跨 origin 清会话、commit 失败、刷新钉住旧服、并发晚到登录不可提交、清会话失败中止切换。
- 主要依赖：ServerSettingsStore、AuthSessionStore、ApiClientProvider、PimServerEndpoints、MockWebServer、SharedPreferences 替身
- 被谁使用：Robolectric SDK 34 测试

## 函数级结构化伪代码

### ServerSettingsSecurityTest
#### setUp
- 清空 pim_server_settings SharedPreferences

#### setBaseUrlInvalidatesDifferentOriginSessionBeforeReturning
- 同 origin 不 clear；换 server-b 则 clearCalls=1 且 tokens 空

#### setBaseUrlCommitFailureClearsSessionAndThrows
- ScriptedCommit 返回 false → IllegalStateException；会话已 clear

#### explicitRefreshServiceStaysPinnedToServerAAfterSettingsSwitchesToB
- MockWebServer A/B；设置切到 B 后 refreshApiServiceForServer(A) 仍打 A 的 /auth/refresh 得 401，B 无请求

#### lateServerALoginResponseCannotCommitAfterConcurrentSwitchToServerB
- 并发：saveSessionIfCurrentServer(A) 等待期间切到 B，提交返回 false，tokens 仍空

#### serverSwitchAbortsWhenOldSessionCannotBeDurablyCleared
- clearSucceeds=false → 抛异常；baseUrl 与 access token 保持 A

#### urlCommitFailureAfterSessionClearPreservesOldUrlWithTokenCleared
- commit 失败：URL 仍 A，但 serverIdentity/token 已清

### 测试替身
- SharedPreferencesContext：包装 Context 返回脚本 prefs
- ScriptedCommitSharedPreferences：可队列化 commit 结果
- RecordingBoundSessionStore：记录 clearCalls、可模拟 clear 失败

## 近逐行中文伪代码

1. Robolectric @Config(sdk=34)。
2. 六个安全场景测试覆盖跨服会话与 refresh 钉扎。
3. 本地私有类实现 prefs/session 替身以注入失败路径。

## 关系边
```json
{
  "nodes": [
    {
      "id": "src/client-android/app/src/test/java/com/pim/app/status/ServerSettingsSecurityTest.kt",
      "label": "ServerSettingsSecurityTest",
      "path": "src/client-android/app/src/test/java/com/pim/app/status/ServerSettingsSecurityTest.kt",
      "doc": "docs/pseudocode/files/src/client-android/app/src/test/java/com/pim/app/status/ServerSettingsSecurityTest.kt.md",
      "layer": "tests",
      "kind": "test"
    }
  ],
  "edges": [
    { "from": "src/client-android/app/src/test/java/com/pim/app/status/ServerSettingsSecurityTest.kt", "to": "src/client-android/core/src/main/java/com/pim/core/settings/ServerSettingsStore.kt", "type": "tests" },
    { "from": "src/client-android/app/src/test/java/com/pim/app/status/ServerSettingsSecurityTest.kt", "to": "src/client-android/core/src/main/java/com/pim/core/network/ApiClientProvider.kt", "type": "depends_on" },
    { "from": "src/client-android/app/src/test/java/com/pim/app/status/ServerSettingsSecurityTest.kt", "to": "src/client-android/core/src/main/java/com/pim/core/auth/AuthSessionStore.kt", "type": "depends_on" }
  ]
}
```
