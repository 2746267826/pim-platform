# src/client-android/core/src/main/java/com/pim/core/util/ThrowableMessages.kt

## 元信息
- 语言：Kotlin
- 程序集或包：client-android core / com.pim.core.util
- 职责：将异常 cause 链格式化为可读字符串（类型名 + 截断消息），便于日志/UI。
- 主要依赖：标准库 generateSequence
- 被谁使用：网络/同步错误展示与测试 ThrowableMessagesTest

## 函数级结构化伪代码

### Throwable.toCauseChainMessage(maxDepth = 6)
- 输入：接收者 Throwable；最大深度（至少 1）
- 输出：用 " -> " 连接的字符串
- 副作用：无
- 步骤：
  1. generateSequence(this){ cause } 取前 maxDepth 个
  2. 每项：simpleName（空则 full name）
  3. message 非空白则截断至 500 + "..."
  4. 无 message 只输出类型名，否则 "Type: message"
  5. joinToString(" -> ")
- 分支与异常：无抛出
- 调用：无外部

## 近逐行中文伪代码

1. 扩展函数 toCauseChainMessage。
2. 沿 cause 链生成序列并限深。
3. 类型名 + 可选截断消息。
4. 用箭头连接。
5. MAX_MESSAGE_LENGTH = 500。

## 关系边
```json
{
  "nodes": [
    {
      "id": "src/client-android/core/src/main/java/com/pim/core/util/ThrowableMessages.kt",
      "label": "ThrowableMessages",
      "path": "src/client-android/core/src/main/java/com/pim/core/util/ThrowableMessages.kt",
      "doc": "docs/pseudocode/files/src/client-android/core/src/main/java/com/pim/core/util/ThrowableMessages.kt.md",
      "layer": "client-android",
      "kind": "other"
    }
  ],
  "edges": []
}
```
