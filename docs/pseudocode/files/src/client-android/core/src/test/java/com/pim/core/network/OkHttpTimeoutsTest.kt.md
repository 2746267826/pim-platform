# src/client-android/core/src/test/java/com/pim/core/network/OkHttpTimeoutsTest.kt

## 元信息
- 语言：Kotlin
- 程序集或包：com.pim.core.network（client-android core 测试）
- 职责：断言 `applyPimApiTimeouts` 允许较大移动上传超时。
- 主要依赖：`OkHttpClient`、`applyPimApiTimeouts`、JUnit
- 被谁使用：测试运行器

## 函数级结构化伪代码

### OkHttpTimeoutsTest
#### applyPimApiTimeoutsAllowsLargeMobileUploads
- 输入：无
- 输出：断言四超时
- 副作用：构建客户端
- 步骤：
  1. `OkHttpClient.Builder().applyPimApiTimeouts().build()`
  2. connect=15s、read=60s、write=60s、call=90s
- 分支与异常：无
- 调用：`applyPimApiTimeouts`

## 近逐行中文伪代码

1. [L1-6] 包与 OkHttp/JUnit 导入
2. [L7-18] 构建客户端并断言四类超时毫秒

## 关系边
```json
{
  "nodes": [
    {
      "id": "src/client-android/core/src/test/java/com/pim/core/network/OkHttpTimeoutsTest.kt",
      "label": "OkHttpTimeoutsTest",
      "path": "src/client-android/core/src/test/java/com/pim/core/network/OkHttpTimeoutsTest.kt",
      "doc": "docs/pseudocode/files/src/client-android/core/src/test/java/com/pim/core/network/OkHttpTimeoutsTest.kt.md",
      "layer": "tests",
      "kind": "test"
    }
  ],
  "edges": [
    { "from": "src/client-android/core/src/test/java/com/pim/core/network/OkHttpTimeoutsTest.kt", "to": "src/client-android/core/src/main/java/com/pim/core/network", "type": "tests" }
  ]
}
```
