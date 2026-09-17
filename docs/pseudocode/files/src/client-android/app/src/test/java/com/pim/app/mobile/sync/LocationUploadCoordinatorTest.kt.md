# src/client-android/app/src/test/java/com/pim/app/mobile/sync/LocationUploadCoordinatorTest.kt

## 元信息
- 语言：Kotlin (JUnit + Robolectric + Room in-memory)
- 程序集或包：client-android test / com.pim.app.mobile.sync
- 职责：验证定位上传规划与状态落库：部分失败、可重试/永久拒绝、删除已确认点、错误分类映射。
- 主要依赖：`LocationUploadPlanner`、`applyLocationStatusUpdates`、`MobileDataDao`、`AppDatabase`、`MobileSyncErrorClassifier`
- 被谁使用：测试运行器；不参与生产运行时

## 函数级结构化伪代码

### LocationUploadCoordinatorTest
#### setUp()
- 输入：无
- 输出：Unit
- 副作用：创建内存 Room DB 与 dao
- 步骤：ApplicationProvider → inMemoryDatabaseBuilder → mobileDataDao
- 分支与异常：无
- 调用：Room API

#### tearDown()
- 输入：无
- 输出：Unit
- 副作用：关闭 DB
- 步骤：`db.close()`
- 分支与异常：无
- 调用：无

#### partialFailureKeepsFailedRowsQueued()
- 输入：无
- 输出：断言通过
- 副作用：无
- 步骤：构造 batch（synced 1,2 / failed 3 / timeout）→ planStatusUpdates → 断言 ID 与 reason
- 分支与异常：无
- 调用：`LocationUploadPlanner.planStatusUpdates`

#### allSuccessfulUploadNeedsNoRetry()
- 步骤：全成功 batch → shouldRetry=false

#### anyFailureNeedsRetry()
- 步骤：有 retryableFailedIds → shouldRetry=true

#### nonRetryableFailureDoesNotScheduleWorkerRetry()
- 步骤：failed 但无 retryable → shouldRetry=false

#### confirmedLocationPointsAreDeleted()
- 步骤：插入两点 → apply 全 synced → 各 sync 状态列表均为 0

#### permanentlyRejectedPointKeepsOwnReason()
- 步骤：两失败 + perItemErrors → REJECTED 且各自 lastError

#### retryablePointKeepsPendingWithOwnReason()
- 步骤：可重试失败 → 仍 PENDING 且各自 lastError

#### mixedConfirmedRejectedRetryable()
- 步骤：确认/永久拒绝/可重试/未触达 混合 → 状态与错误正确

#### planStatusUpdates preserves retryableFailedIds / perItemErrors
- 步骤：规划结果保留 retryable 列表；copy 后 perItemErrors 可透传

#### retryableFailedIds not counted in rejected
- 步骤：用集合差集验证永久失败 ID 集合

#### applyStatusUpdates deletes synced and keeps retryable as pending permanent as rejected
- 步骤：落库后 pending/rejected 各一条

#### classify RETRY to PENDING and BLOCKED to REJECTED
- 步骤：SocketTimeoutException→RETRY；HTTP 400→BLOCKED

#### companion point()
- 步骤：构造默认 MobileLocationPointEntity 测试夹具

## 近逐行中文伪代码

1. [L21-L24] Robolectric 测试类；持有 db/dao
2. [L26-L38] setUp 内存库；tearDown 关闭
3. [L42-L97] planStatusUpdates：部分失败 / 全成功 / 需重试 / 不可重试
4. [L101-L181] applyStatusUpdates：删除确认、永久拒绝原因、可重试 PENDING、混合场景
5. [L185-L213] 保留 retryableFailedIds 与 perItemErrors
6. [L217-L257] 永久 vs 可重试计数与落库
7. [L261-L268] MobileSyncErrorClassifier 集成断言
8. [L270-L279] point() 夹具

## 关系边
```json
{
  "nodes": [
    {
      "id": "src/client-android/app/src/test/java/com/pim/app/mobile/sync/LocationUploadCoordinatorTest.kt",
      "label": "LocationUploadCoordinatorTest",
      "path": "src/client-android/app/src/test/java/com/pim/app/mobile/sync/LocationUploadCoordinatorTest.kt",
      "doc": "docs/pseudocode/files/src/client-android/app/src/test/java/com/pim/app/mobile/sync/LocationUploadCoordinatorTest.kt.md",
      "layer": "tests",
      "kind": "test"
    }
  ],
  "edges": [
    {
      "from": "src/client-android/app/src/test/java/com/pim/app/mobile/sync/LocationUploadCoordinatorTest.kt",
      "to": "src/client-android/app/src/main/java/com/pim/app/mobile/sync/LocationUploadPlanner.kt",
      "type": "tests"
    },
    {
      "from": "src/client-android/app/src/test/java/com/pim/app/mobile/sync/LocationUploadCoordinatorTest.kt",
      "to": "src/client-android/app/src/main/java/com/pim/app/data/MobileDataDao.kt",
      "type": "depends_on"
    },
    {
      "from": "src/client-android/app/src/test/java/com/pim/app/mobile/sync/LocationUploadCoordinatorTest.kt",
      "to": "src/client-android/app/src/main/java/com/pim/app/mobile/sync/MobileSyncErrorClassifier.kt",
      "type": "tests"
    }
  ]
}
```
