# src/client-android/app/src/test/java/com/pim/app/mobile/sync/MobileAcknowledgementPlannerTest.kt

## 元信息
- 语言：Kotlin
- 程序集或包：client-android (app test)
- 职责：单测 `MobileAcknowledgementPlanner`：按 item/outcome 划分 confirmed/retry/deadLetter；类型化键与 legacy 裸 key 路径；聚合计数一致性与实体映射 clientItemKey。
- 主要依赖：`MobileAcknowledgementPlanner`、`MobileIngestResponse`/`MobileIngestItemResult`、Room 实体 toDto
- 被谁使用：单元测试运行器

## 函数级结构化伪代码

### MobileAcknowledgementPlannerTest（代表性用例）
#### partialResponseSeparatesConfirmedRetryAndDeadLetterKeys()
- 步骤：sentKeys 11-14 + 四 outcome → confirmed{11,14} dead{12} retry{13}

#### typedItemsWithSameClientKeyArePlannedIndependently()
- 步骤：同 clientKey 不同 entityType 独立 confirmed/dead/retry

#### missingTypedResultRetriesOnlyTheMissingItem()
- 步骤：缺结果项进 retry，failureCode=`server-ack-ambiguous`

#### duplicateResultForSameTypedItemRetriesThatItem() / unknownOutcomeRetriesOnlyThatTypedItem()
- 步骤：重复结果或未知 outcome → 歧义重试

#### explicitItemResultsMustMatchEveryAggregateCount()
- 步骤：聚合计数与 itemResults 不一致 → 全部 retry + ambiguous

#### mismatchedEntityType / unknownEntityType / unexpectedExtraTypedResult
- 步骤：类型不匹配、未知类型、多余结果 → 歧义

#### typedAggregateOnlySuccessConfirmsEverySentItem / ambiguousTypedAggregateOnlyResponseRetriesEverySentItem
- 步骤：无 itemResults 时仅当 accepted+skipped 覆盖全部才确认

#### legacyBareKeyPath* / aggregateOnlySuccess* / ambiguousAggregateOnly*
- 步骤：legacy plan(sentKeys) 对跨类型重复、类型不符、多余结果、聚合歧义同样保守 retry

#### uploadMappingsUseStableRoomAndPackageVersionKeys()
- 步骤：event/summary id 字符串；app `package@versionCode` 作为 clientItemKey

## 近逐行中文伪代码

1. [L12] 测试类 `MobileAcknowledgementPlannerTest`
2. [L13-36] 部分响应：accepted/skipped→确认，rejected→死信，failed→重试
3. [L38-63] 同 key 不同类型独立规划
4. [L65-132] 缺失/重复/未知 outcome → ambiguous + 定点重试
5. [L134-232] 聚合不一致、类型错、未知类型、多余结果全部保守
6. [L234-273] 仅聚合成功/歧义（typed）
7. [L275-366] legacy 裸 key 路径对称断言
8. [L368-414] 实体 toDto clientItemKey 稳定性

## 关系边
```json
{
  "nodes": [
    {
      "id": "src/client-android/app/src/test/java/com/pim/app/mobile/sync/MobileAcknowledgementPlannerTest.kt",
      "label": "MobileAcknowledgementPlannerTest",
      "path": "src/client-android/app/src/test/java/com/pim/app/mobile/sync/MobileAcknowledgementPlannerTest.kt",
      "doc": "docs/pseudocode/files/src/client-android/app/src/test/java/com/pim/app/mobile/sync/MobileAcknowledgementPlannerTest.kt.md",
      "layer": "tests",
      "kind": "test"
    }
  ],
  "edges": [
    { "from": "src/client-android/app/src/test/java/com/pim/app/mobile/sync/MobileAcknowledgementPlannerTest.kt", "to": "src/client-android/app/src/main/java/com/pim/app/mobile/sync/MobileAcknowledgementPlanner.kt", "type": "tests" },
    { "from": "src/client-android/app/src/test/java/com/pim/app/mobile/sync/MobileAcknowledgementPlannerTest.kt", "to": "src/client-android/core/src/main/java/com/pim/core/models/MobileModels.kt", "type": "depends_on" },
    { "from": "src/client-android/app/src/test/java/com/pim/app/mobile/sync/MobileAcknowledgementPlannerTest.kt", "to": "src/client-android/app/src/main/java/com/pim/app/data/MobileUsageEventEntity.kt", "type": "depends_on" }
  ]
}
```
