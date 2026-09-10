# src/client-android/app/src/test/java/com/pim/app/status/StatusIssueTest.kt

## 元信息
- 语言：Kotlin / JUnit
- 程序集或包：client-android tests
- 职责：覆盖 StatusIssue 工厂、Planner、Tracking 映射、ActionRouter、SyncRunner、RefreshSignal、队列合计。
- 主要依赖：StatusIssue、StatusIssuePlanner、StatusTrackingMapper、StatusActionRouter、StatusSyncActionRunner、StatusRefreshSignal、各类 Snapshot
- 被谁使用：测试运行器

## 函数级结构化伪代码

### StatusIssueTest
#### requiredIssuesHaveReadableActionLabels
- requiredIssueCodes 含关键 code；各工厂 actionLabel 为「去设置」

#### snapshotPlannerAddsActionableBlockingIssues
- 构造残缺权限/API/服务/队列/诊断快照
- plan 后校验 api-address-missing、background-location、FGS、精度、队列积压、recent-error、policy、dropped 等 title

#### trackingSnapshotUsesForegroundServiceRuntimeState
- StatusTrackingMapper.fromRuntime 映射 profile/mode/nextExpected

#### diagnosticSnapshotKeepsRecentLogs
- recentLogMessages 保序

#### actionRouterMapsTargetsToVisibleActions
- Settings/Login→OpenSettings；Permissions→OpenPermissions；Sync/Queue→TriggerSync；Status→StayOnStatus

#### syncActionRunnerRunsMobileSyncAndRefreshesStatus
- TriggerSync 时 syncNow + refresh 均执行

#### syncActionRunnerIgnoresNonSyncRoutes
- OpenSettings 不触发 sync

#### refreshSignalStartsAtZeroAndAdvances
- version 0 → requestRefresh → 1

#### pendingUploadTotalExcludesLogs
- pendingUploadTotal = 位置+usage+summary+metadata+device+batches（不含 logs）

## 近逐行中文伪代码

1. 校验必需 issue code 与「去设置」文案。
2. 用残缺快照驱动 Planner，断言阻塞问题标题。
3. 运行时状态映射到 TrackingPolicySnapshot。
4. 诊断保留最近日志列表。
5. ActionRouter 目标到路由枚举。
6. SyncRunner 仅对 TriggerSync 跑同步与刷新。
7. RefreshSignal 版本递增。
8. 队列合计排除 pendingLogs。

## 关系边
```json
{
  "nodes": [
    {
      "id": "src/client-android/app/src/test/java/com/pim/app/status/StatusIssueTest.kt",
      "label": "StatusIssueTest",
      "path": "src/client-android/app/src/test/java/com/pim/app/status/StatusIssueTest.kt",
      "doc": "docs/pseudocode/files/src/client-android/app/src/test/java/com/pim/app/status/StatusIssueTest.kt.md",
      "layer": "tests",
      "kind": "test"
    }
  ],
  "edges": [
    {
      "from": "src/client-android/app/src/test/java/com/pim/app/status/StatusIssueTest.kt",
      "to": "src/client-android/app/src/main/java/com/pim/app/status/StatusIssue.kt",
      "type": "tests"
    },
    {
      "from": "src/client-android/app/src/test/java/com/pim/app/status/StatusIssueTest.kt",
      "to": "src/client-android/app/src/main/java/com/pim/app/status",
      "type": "depends_on"
    }
  ]
}
```
