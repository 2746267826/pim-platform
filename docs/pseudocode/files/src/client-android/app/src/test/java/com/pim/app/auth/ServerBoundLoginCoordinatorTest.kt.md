# src/client-android/app/src/test/java/com/pim/app/auth/ServerBoundLoginCoordinatorTest.kt

## 元信息
- 语言：Kotlin
- 程序集或包：client-android (app test)
- 职责：Robolectric 单测 `ServerBoundLoginCoordinator`：并发切服丢弃陈旧响应、成功落会话、安全存储失败、取消传播。
- 主要依赖：`ServerBoundLoginCoordinator`、`TokenManager`、`ServerSettingsStore`、`ServerBoundLoginTransport`、Robolectric
- 被谁使用：单元测试运行器

## 函数级结构化伪代码

### ServerBoundLoginCoordinatorTest
#### setUp()
- 输入：无
- 输出：无
- 副作用：清空 server settings 与 auth prefs
- 步骤：取 ApplicationContext；clear `pim_server_settings` 与 AUTH_PREFS
- 分支与异常：无
- 调用：`SharedPreferences.edit().clear().commit()`

#### delayedServerAResponseIsDiscardedAfterConcurrentSwitchToServerB()
- 输入：无
- 输出：无（断言）
- 副作用：异步登录 + 中途切 baseUrl
- 步骤：
  1. 配置 SERVER_A，BlockingLoginTransport
  2. async 发起 login，等待 transport.entered
  3. 切换到 SERVER_B，release transport
  4. 断言结果 `StaleServer`；无 token；请求绑定 A 的 trustedOrigin
- 分支与异常：await 超时失败
- 调用：`coordinator.login`、`settings.setBaseUrl`

#### responseIsSavedWhenCapturedServerRemainsCurrent()
- 步骤：SERVER_A 登录成功 → Success；access/refresh/serverIdentity 已写入 TokenManager

#### secureStorageSaveFailureIsReportedWithoutCreatingSession()
- 步骤：CommitFailingSharedPreferences 使 commit 失败 → `SessionSaveFailed`；无会话

#### transportCancellationIsPropagated()
- 步骤：transport 抛 `CancellationException` → assertThrows 同类型

### 辅助类型
#### BlockingLoginTransport.login / TestSecurePreferencesFactory / CommitFailingSharedPreferences
- 阻塞门闩模拟慢响应；commit 恒 false 模拟存储失败

## 近逐行中文伪代码

1. [L33-35] Robolectric SDK34 测试类
2. [L38-48] setUp 清空 prefs
3. [L51-73] 并发切服：陈旧响应 → StaleServer，token 为空
4. [L75-92] 服务器未变：Success 并保存 tokens + identity
5. [L94-111] 存储 commit 失败：SessionSaveFailed，无会话
6. [L113-127] CancellationException 向上传播
7. [L129-154] tokenManager 工厂、successfulResponse、常量 URL
8. [L157-203] BlockingLoginTransport、TestSecurePreferencesFactory、CommitFailing* 测试替身

## 关系边
```json
{
  "nodes": [
    {
      "id": "src/client-android/app/src/test/java/com/pim/app/auth/ServerBoundLoginCoordinatorTest.kt",
      "label": "ServerBoundLoginCoordinatorTest",
      "path": "src/client-android/app/src/test/java/com/pim/app/auth/ServerBoundLoginCoordinatorTest.kt",
      "doc": "docs/pseudocode/files/src/client-android/app/src/test/java/com/pim/app/auth/ServerBoundLoginCoordinatorTest.kt.md",
      "layer": "tests",
      "kind": "test"
    }
  ],
  "edges": [
    { "from": "src/client-android/app/src/test/java/com/pim/app/auth/ServerBoundLoginCoordinatorTest.kt", "to": "src/client-android/core/src/main/java/com/pim/core/auth/ServerBoundLoginCoordinator.kt", "type": "tests" },
    { "from": "src/client-android/app/src/test/java/com/pim/app/auth/ServerBoundLoginCoordinatorTest.kt", "to": "src/client-android/core/src/main/java/com/pim/core/auth/TokenManager.kt", "type": "depends_on" },
    { "from": "src/client-android/app/src/test/java/com/pim/app/auth/ServerBoundLoginCoordinatorTest.kt", "to": "src/client-android/core/src/main/java/com/pim/core/settings/ServerSettingsStore.kt", "type": "depends_on" },
    { "from": "src/client-android/app/src/test/java/com/pim/app/auth/ServerBoundLoginCoordinatorTest.kt", "to": "src/client-android/core/src/main/java/com/pim/core/models/AuthModels.kt", "type": "depends_on" }
  ]
}
```
