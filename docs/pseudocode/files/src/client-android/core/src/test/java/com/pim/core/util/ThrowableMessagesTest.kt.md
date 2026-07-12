# src/client-android/core/src/test/java/com/pim/core/util/ThrowableMessagesTest.kt

## 元信息
- 语言：Kotlin (JUnit)
- 程序集或包：client-android core test
- 职责：验证 toCauseChainMessage 拼接嵌套 cause。
- 主要依赖：ThrowableMessages.kt
- 被谁使用：单元测试

## 函数级结构化伪代码

### ThrowableMessagesTest
#### toCauseChainMessageIncludesNestedCauses
- 构造 IllegalArgumentException 嵌套 IllegalStateException
- 断言输出为 "IllegalArgumentException: ... -> IllegalStateException: ..."

## 近逐行中文伪代码

1. 嵌套两个异常。
2. assertEquals 完整箭头链文案。

## 关系边
```json
{
  "nodes": [
    {
      "id": "src/client-android/core/src/test/java/com/pim/core/util/ThrowableMessagesTest.kt",
      "label": "ThrowableMessagesTest",
      "path": "src/client-android/core/src/test/java/com/pim/core/util/ThrowableMessagesTest.kt",
      "doc": "docs/pseudocode/files/src/client-android/core/src/test/java/com/pim/core/util/ThrowableMessagesTest.kt.md",
      "layer": "tests",
      "kind": "test"
    }
  ],
  "edges": [
    { "from": "src/client-android/core/src/test/java/com/pim/core/util/ThrowableMessagesTest.kt", "to": "src/client-android/core/src/main/java/com/pim/core/util/ThrowableMessages.kt", "type": "tests" }
  ]
}
```
