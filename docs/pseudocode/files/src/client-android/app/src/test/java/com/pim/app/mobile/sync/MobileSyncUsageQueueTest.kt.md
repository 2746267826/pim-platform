# src/client-android/app/src/test/java/com/pim/app/mobile/sync/MobileSyncUsageQueueTest.kt

## 元信息
- 语言：Kotlin (Robolectric + Room in-memory)
- 程序集或包：client-android app test
- 职责：验证 loadPendingUsageBatch / pendingUsageRemaining / sortedMergeOutcome 对 PENDING/FAILED 优先级、limit、窗口时间与 outcome 合并规则。
- 主要依赖：AppDatabase、MobileDataDao、MobileSyncStatus、loadPendingUsageBatch、pendingUsageRemaining、sortedMergeOutcome
- 被谁使用：Robolectric 测试运行

## 函数级结构化伪代码

### MobileSyncUsageQueueTest
#### setUp / tearDown
- 内存 Room 建库 allowMainThreadQueries；取 mobileDataDao；关闭 db

#### loadPendingUsageBatch 相关用例
- 含 PENDING+FAILED，排除 SYNCED+REJECTED
- limit=1 时 PENDING 优先于 FAILED
- 501 条 PENDING limit 500 不阻塞，remaining 仍 501
- 仅 appMetadata 时 windowStart/End 合法且 end>start
- 混合 event/summary/meta 窗口取最早到最晚
- 空批 window 为 null

#### pendingUsageRemaining
- 只计 usage event/summary/meta，不计 location
- 返回真实行数而非类别数

#### sortedMergeOutcome
- RETRY > BLOCKED > SUCCESS 的合并优先级

### companion helpers
- event / summary / appMeta 工厂构造实体

## 近逐行中文伪代码

1. RobolectricTestRunner 类。
2. Before 内存库；After close。
3. 多条 @Test 覆盖 batch 加载、remaining、merge outcome。
4. companion 提供测试实体工厂。

## 关系边
```json
{
  "nodes": [
    {
      "id": "src/client-android/app/src/test/java/com/pim/app/mobile/sync/MobileSyncUsageQueueTest.kt",
      "label": "MobileSyncUsageQueueTest",
      "path": "src/client-android/app/src/test/java/com/pim/app/mobile/sync/MobileSyncUsageQueueTest.kt",
      "doc": "docs/pseudocode/files/src/client-android/app/src/test/java/com/pim/app/mobile/sync/MobileSyncUsageQueueTest.kt.md",
      "layer": "tests",
      "kind": "test"
    }
  ],
  "edges": [
    { "from": "src/client-android/app/src/test/java/com/pim/app/mobile/sync/MobileSyncUsageQueueTest.kt", "to": "src/client-android/app/src/main/java/com/pim/app/mobile/sync/MobileSyncCoordinator.kt", "type": "tests" },
    { "from": "src/client-android/app/src/test/java/com/pim/app/mobile/sync/MobileSyncUsageQueueTest.kt", "to": "src/client-android/app/src/main/java/com/pim/app/data/AppDatabase.kt", "type": "depends_on" },
    { "from": "src/client-android/app/src/test/java/com/pim/app/mobile/sync/MobileSyncUsageQueueTest.kt", "to": "src/client-android/app/src/main/java/com/pim/app/data/MobileDataDao.kt", "type": "depends_on" }
  ]
}
```
