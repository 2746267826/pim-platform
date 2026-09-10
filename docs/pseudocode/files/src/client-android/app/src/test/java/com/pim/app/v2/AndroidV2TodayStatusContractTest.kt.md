# src/client-android/app/src/test/java/com/pim/app/v2/AndroidV2TodayStatusContractTest.kt

## 元信息
- 语言：Kotlin / JUnit
- 程序集或包：client-android tests
- 职责：源码契约测试：今日页 API 状态须来自 ViewModel 观测，禁止硬编码「API：待连接」。
- 主要依赖：TodayScreen.kt、TodayViewModel.kt 源文件
- 被谁使用：测试运行器

## 函数级结构化伪代码

### AndroidV2TodayStatusContractTest
#### todayApiChipUsesObservedStatusInsteadOfHardcodedPendingConnection
- 输入：仓库内 TodayScreen/TodayViewModel 源码
- 步骤：
  1. repoFile 向上找 canonical 根并 resolve 路径
  2. 读 TodayScreen 文本
  3. assert 不含 Text("API：待连接")
  4. assert 含 hiltViewModel、collectAsStateWithLifecycle、state.apiStatusLabel
  5. assert TodayViewModel 文件存在

#### repoFile(vararg parts) [private]
- 从当前目录向上找存在路径或 .kt 候选；找不到 error

## 近逐行中文伪代码

1. 定位 app 源码中的 TodayScreen 与 TodayViewModel。
2. 禁止硬编码 API 待连接文案。
3. 必须用 Hilt ViewModel + lifecycle collect + apiStatusLabel。
4. ViewModel 源文件必须存在。
5. repoFile 在工作目录树向上搜索文件。

## 关系边
```json
{
  "nodes": [
    {
      "id": "src/client-android/app/src/test/java/com/pim/app/v2/AndroidV2TodayStatusContractTest.kt",
      "label": "AndroidV2TodayStatusContractTest",
      "path": "src/client-android/app/src/test/java/com/pim/app/v2/AndroidV2TodayStatusContractTest.kt",
      "doc": "docs/pseudocode/files/src/client-android/app/src/test/java/com/pim/app/v2/AndroidV2TodayStatusContractTest.kt.md",
      "layer": "tests",
      "kind": "test"
    }
  ],
  "edges": [
    {
      "from": "src/client-android/app/src/test/java/com/pim/app/v2/AndroidV2TodayStatusContractTest.kt",
      "to": "src/client-android/app/src/main/java/com/pim/app/ui/today/TodayScreen.kt",
      "type": "tests"
    },
    {
      "from": "src/client-android/app/src/test/java/com/pim/app/v2/AndroidV2TodayStatusContractTest.kt",
      "to": "src/client-android/app/src/main/java/com/pim/app/ui/today/TodayViewModel.kt",
      "type": "tests"
    }
  ]
}
```
