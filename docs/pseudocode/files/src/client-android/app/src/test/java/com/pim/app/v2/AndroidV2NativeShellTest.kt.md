# src/client-android/app/src/test/java/com/pim/app/v2/AndroidV2NativeShellTest.kt

## 元信息
- 语言：Kotlin
- 程序集或包：com.pim.app.v2（client-android app 测试）
- 职责：源码级约束：原生壳五 Tab 文案存在，且根屏不使用 WebView 作为主体验。
- 主要依赖：`PimDestination.kt`、`PimRootScreen.kt` 源文件、JUnit
- 被谁使用：测试运行器

## 函数级结构化伪代码

### AndroidV2NativeShellTest
#### rootDefinesApprovedFiveTabsAndNoWebViewPrimaryExperience
- 输入：仓库内两份 Kotlin 源文本
- 输出：断言通过/失败
- 副作用：读磁盘文件
- 步骤：
  1. `repoFile` 定位 `PimDestination.kt` 与 `PimRootScreen.kt`
  2. Destination 含「今日/轨迹/日程/状态/设置」
  3. Root 含 `NavigationBar`、`PimTheme`
  4. Root 不含 `PimWebViewScreen` 与 `WebView`
- 分支与异常：文件找不到则 error
- 调用：`repoFile`、`readText`

#### repoFile(vararg parts): File
- 输入：相对路径片段
- 输出：存在的 File
- 副作用：向上遍历 cwd 祖先
- 步骤：从 canonical cwd 起 fold resolve；存在则返回；否则 parent；全无则 error
- 分支与异常：未找到抛 error
- 调用：`File` API

## 近逐行中文伪代码

1. [L1-7] 包与 JUnit 导入
2. [L8-21] 读 Destination/Root 文本并断言五 Tab 与无 WebView 主路径
3. [L23-31] `repoFile` 向上搜索仓库文件

## 关系边
```json
{
  "nodes": [
    {
      "id": "src/client-android/app/src/test/java/com/pim/app/v2/AndroidV2NativeShellTest.kt",
      "label": "AndroidV2NativeShellTest",
      "path": "src/client-android/app/src/test/java/com/pim/app/v2/AndroidV2NativeShellTest.kt",
      "doc": "docs/pseudocode/files/src/client-android/app/src/test/java/com/pim/app/v2/AndroidV2NativeShellTest.kt.md",
      "layer": "tests",
      "kind": "test"
    }
  ],
  "edges": [
    { "from": "src/client-android/app/src/test/java/com/pim/app/v2/AndroidV2NativeShellTest.kt", "to": "src/client-android/app/src/main/java/com/pim/app/ui/root/PimDestination.kt", "type": "tests" },
    { "from": "src/client-android/app/src/test/java/com/pim/app/v2/AndroidV2NativeShellTest.kt", "to": "src/client-android/app/src/main/java/com/pim/app/ui/root/PimRootScreen.kt", "type": "tests" }
  ]
}
```
