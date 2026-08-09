# src/client-android/app/src/test/java/com/pim/app/mobile/sync/MobileSyncAckProcessingTest.kt

## 元信息
- 语言：Kotlin
- 程序集或包：com.pim.app.mobile.sync（client-android app 测试）
- 职责：内存 Room 验证 `processUsageAcknowledgements` 与 pending 计数语义。
- 主要依赖：`AppDatabase`、`MobileDataDao`、实体、`processUsageAcknowledgements`、`MobileIngestResponse`、Robolectric
- 被谁使用：测试运行器

## 函数级结构化伪代码

### MobileSyncAckProcessingTest
#### setUp / tearDown
- 输入：无
- 输出：Unit
- 副作用：建/关内存 DB
- 步骤：`Room.inMemoryDatabaseBuilder` + `allowMainThreadQueries`；`db.close()`
- 分支与异常：无
- 调用：Room API

#### confirmedItemsDeleted
- 输入：插入 event/summary/metadata
- 输出：断言未确认项仍在
- 副作用：DAO 删除已接受项
- 步骤：`processUsageAcknowledgements` 对三类 accepted 删除；保留另一半
- 分支与异常：无
- 调用：`processUsageAcknowledgements`

#### processUsageAcknowledgementsEndToEnd
- 输入：混合 accepted/rejected/failed/skipped
- 输出：状态与 lastError
- 副作用：更新/删除记录
- 步骤：
  1. accepted 删除
  2. rejected 标记 REJECTED 与错误串
  3. failed 回 PENDING 与错误串
  4. app-metadata skipped 也视为确认删除
- 分支与异常：无
- 调用：DAO 按 syncStatus 查询

#### processUsageAcknowledgementsFallbackErrors / PartialErrors
- 输入：空 code/message 或部分信息
- 输出：默认错误文案或单侧信息
- 副作用：写 lastError
- 步骤：空 → server-rejected / server-retry；仅 code 或 message 则用该侧
- 分支与异常：无
- 调用：`processUsageAcknowledgements`

#### processUsageAcknowledgementsCrossTypeSafety
- 输入：相同 clientItemKey 不同类型
- 输出：按 entityType 独立处理
- 副作用：event 接受删除、summary 拒绝保留
- 步骤：同 key 不同 type 不串删
- 分支与异常：eventId 与 key 相等时验证 event 清空
- 调用：DAO

#### pendingCountsExcludeSyncedAndRejected
- 输入：各 syncStatus 样例
- 输出：pending 计数
- 副作用：无（只读 Flow first）
- 步骤：SYNCED/REJECTED 不计；FAILED/PENDING/SYNCING 计
- 分支与异常：无
- 调用：`pending*Count().first()`

#### companion factories
- 输入：可选 override
- 输出：测试实体
- 副作用：无
- 步骤：event/summary/appMeta/locPoint 默认字段
- 分支与异常：无
- 调用：无

## 近逐行中文伪代码

1. [L1-24] 导入 Room/实体/ingest 模型/Robolectric
2. [L25-42] 内存库 setUp/tearDown
3. [L47-72] 确认项删除覆盖三类实体
4. [L77-127] 端到端混合结果与错误串
5. [L132-171] 空/部分错误信息回退
6. [L176-200] 跨类型同 key 安全
7. [L205-236] pending 计数排除 SYNCED/REJECTED
8. [L238-286] 工厂方法构造实体

## 关系边
```json
{
  "nodes": [
    {
      "id": "src/client-android/app/src/test/java/com/pim/app/mobile/sync/MobileSyncAckProcessingTest.kt",
      "label": "MobileSyncAckProcessingTest",
      "path": "src/client-android/app/src/test/java/com/pim/app/mobile/sync/MobileSyncAckProcessingTest.kt",
      "doc": "docs/pseudocode/files/src/client-android/app/src/test/java/com/pim/app/mobile/sync/MobileSyncAckProcessingTest.kt.md",
      "layer": "tests",
      "kind": "test"
    }
  ],
  "edges": [
    { "from": "src/client-android/app/src/test/java/com/pim/app/mobile/sync/MobileSyncAckProcessingTest.kt", "to": "src/client-android/app/src/main/java/com/pim/app/data/AppDatabase.kt", "type": "depends_on" },
    { "from": "src/client-android/app/src/test/java/com/pim/app/mobile/sync/MobileSyncAckProcessingTest.kt", "to": "src/client-android/app/src/main/java/com/pim/app/data/MobileDataDao.kt", "type": "depends_on" },
    { "from": "src/client-android/app/src/test/java/com/pim/app/mobile/sync/MobileSyncAckProcessingTest.kt", "to": "src/client-android/app/src/main/java/com/pim/app/mobile/sync/MobileSyncCoordinator.kt", "type": "tests" },
    { "from": "src/client-android/app/src/test/java/com/pim/app/mobile/sync/MobileSyncAckProcessingTest.kt", "to": "src/client-android/core/src/main/java/com/pim/core/models", "type": "depends_on" }
  ]
}
```
