# src/client-android/app/src/test/java/com/pim/app/v2/AndroidV2LoginFlowContractTest.kt

## 元信息
- 语言：Kotlin (源码静态契约测试)
- 程序集或包：client-android test / com.pim.app.v2
- 职责：通过读取 SettingsViewModel / PimAppScaffold / ServerBoundLoginCoordinator / CoreModule 源文本，锁定登录/切服/登出的安全顺序与 fail-closed 约束。
- 主要依赖：仓库内源文件路径解析 `repoFile`
- 被谁使用：测试运行器

## 函数级结构化伪代码

### AndroidV2LoginFlowContractTest
#### settingsLoginUsesConfiguredApiAndStoresReturnedTokens()
- 输入：无
- 输出：断言通过
- 副作用：读 SettingsViewModel.kt 文本
- 步骤：
  1. 校验顺序：validate API → saveApiAddress → serverBoundLoginCoordinator.login
  2. 必须处理 StaleServer / SessionSaveFailed
  3. 失败分支先 CancellationException rethrow 再 UI update
  4. 禁止 Ephemeral；失败 isLoggedIn 用 hasCurrentServerSession
  5. logout 检查 tokenManager.clear 失败；禁止直接 saveTokens
- 分支与异常：源码缺片段则 assert 失败
- 调用：`repoFile`、indexOf

#### testConnectionInvalidatesServerASessionBeforeSavingAndProbingServerB()
- 步骤：saveApiAddress 经 serverSettingsStore.setBaseUrl；testConnection 先 save 再 force probe

#### legacyMobileLoginRejectsInvalidReturnedTokenSession()
- 步骤：对 PimAppScaffold 做与设置页同类的登录/取消/fail-closed 契约

#### sharedLoginCoordinatorPinsTransportAndUsesAtomicServerCommit()
- 步骤：Coordinator 含 commitSessionIfCurrentServer；CoreModule 含 refreshApiServiceForServer + login(request)

#### serverUrlMutationEntrypointsReloadPersistedUrlAndCurrentServerSession()
- 步骤：settings/collection/legacy 保存路径至少两次 reloadPersistedServerState；legacy reload 读 getBaseUrl 与 hasCurrentServerSession

#### repoFile / countOccurrences / sectionBetween
- 步骤：向上找仓库根拼路径；窗口计数；截取函数体区间

## 近逐行中文伪代码

1. [L7] 类 AndroidV2LoginFlowContractTest
2. [L8-L62] 设置页登录顺序与安全断言
3. [L64-L87] testConnection 先存后探
4. [L89-L140] 遗留 Scaffold 登录契约
5. [L142-L181] 共享 Coordinator + CoreModule 绑定服务器
6. [L183-L241] URL 变更入口 reload 次数与会话派生
7. [L243-L259] 工具：repoFile / countOccurrences / sectionBetween

## 关系边
```json
{
  "nodes": [
    {
      "id": "src/client-android/app/src/test/java/com/pim/app/v2/AndroidV2LoginFlowContractTest.kt",
      "label": "AndroidV2LoginFlowContractTest",
      "path": "src/client-android/app/src/test/java/com/pim/app/v2/AndroidV2LoginFlowContractTest.kt",
      "doc": "docs/pseudocode/files/src/client-android/app/src/test/java/com/pim/app/v2/AndroidV2LoginFlowContractTest.kt.md",
      "layer": "tests",
      "kind": "test"
    }
  ],
  "edges": [
    {
      "from": "src/client-android/app/src/test/java/com/pim/app/v2/AndroidV2LoginFlowContractTest.kt",
      "to": "src/client-android/app/src/main/java/com/pim/app/ui/settings/SettingsViewModel.kt",
      "type": "tests"
    },
    {
      "from": "src/client-android/app/src/test/java/com/pim/app/v2/AndroidV2LoginFlowContractTest.kt",
      "to": "src/client-android/app/src/main/java/com/pim/app/ui/PimAppScaffold.kt",
      "type": "tests"
    },
    {
      "from": "src/client-android/app/src/test/java/com/pim/app/v2/AndroidV2LoginFlowContractTest.kt",
      "to": "src/client-android/core/src/main/java/com/pim/core/auth/ServerBoundLoginCoordinator.kt",
      "type": "tests"
    },
    {
      "from": "src/client-android/app/src/test/java/com/pim/app/v2/AndroidV2LoginFlowContractTest.kt",
      "to": "src/client-android/core/src/main/java/com/pim/core/di/CoreModule.kt",
      "type": "tests"
    }
  ]
}
```
