# src/client-android/app/src/test/java/com/pim/app/mobile/logs/StructuredLogRepositoryTest.kt

## 元信息
- 语言：Kotlin
- 程序集或包：client-android tests / com.pim.app.mobile.logs
- 职责：Robolectric 验证 StructuredLogRepository 写 JSONL、recent 排序、NaN 容错、跳过坏行；确认不写入 Room mobile_logs。
- 主要依赖：StructuredLogRepository、AppDatabase、Robolectric、Room in-memory
- 被谁使用：Android 单元测试套件

## 函数级结构化伪代码

### StructuredLogRepositoryTest
#### setUp / tearDown
- 输入：无
- 输出：Unit
- 副作用：清理 logs 目录；创建/关闭 in-memory DB
- 步骤：ApplicationProvider 取 Context；deleteRecursively logs；建 repository 与 Room
- 调用：Room.inMemoryDatabaseBuilder

#### writesJsonlFileOnInfo
- 输入：info 调用
- 输出：断言通过
- 步骤：写一条 info → 存在 .jsonl → 字段 level/tag/message/details/occurredAtUtc/source=android；Room pendingLogCount=0
- 调用：repository.info、JSONObject

#### recentReturnsLatestFirst
- 输入：三条 info
- 输出：recent(2) 为 op3、op2
- 调用：repository.recent

#### logWithNanDetailsDoesNotThrowAndWritesSubsequentLogs
- 输入：details 含 Double.NaN 后正常日志
- 输出：recent 非空且最新为 good-op
- 调用：repository.info、recent

#### recentSkipsCorruptLines
- 输入：有效行后追加非法 JSON 行
- 输出：recent 仍只解析 1 条 valid-op
- 调用：appendText、recent

## 近逐行中文伪代码

1. [L19-39] Robolectric 夹具：清理 logs、建 repo 与内存 DB。
2. [L41-63] 验证 JSONL 写入与 Room 不落库。
3. [L65-77] recent 最新优先。
4. [L79-87] NaN details 不阻断后续写入。
5. [L89-100] 损坏行被跳过。

## 关系边
```json
{
  "nodes": [
    {
      "id": "src/client-android/app/src/test/java/com/pim/app/mobile/logs/StructuredLogRepositoryTest.kt",
      "label": "StructuredLogRepositoryTest",
      "path": "src/client-android/app/src/test/java/com/pim/app/mobile/logs/StructuredLogRepositoryTest.kt",
      "doc": "docs/pseudocode/files/src/client-android/app/src/test/java/com/pim/app/mobile/logs/StructuredLogRepositoryTest.kt.md",
      "layer": "tests",
      "kind": "test"
    }
  ],
  "edges": [
    {
      "from": "src/client-android/app/src/test/java/com/pim/app/mobile/logs/StructuredLogRepositoryTest.kt",
      "to": "src/client-android/app/src/main/java/com/pim/app/mobile/logs/StructuredLogRepository.kt",
      "type": "tests"
    },
    {
      "from": "src/client-android/app/src/test/java/com/pim/app/mobile/logs/StructuredLogRepositoryTest.kt",
      "to": "src/client-android/app/src/main/java/com/pim/app/data/AppDatabase.kt",
      "type": "depends_on"
    }
  ]
}
```
