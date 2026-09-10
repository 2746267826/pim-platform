# src/client-android/app/src/main/java/com/pim/app/status/StatusIssue.kt

## 元信息
- 语言：Kotlin
- 程序集或包：com.pim.app.status（client-android app）
- 职责：定义状态中心问题模型、动作路由、各类快照数据结构，以及根据快照规划 `StatusIssue` 列表的 `StatusIssuePlanner`。
- 主要依赖：`com.pim.app.location.service.ForegroundLocationRuntimeState`
- 被谁使用：状态中心 UI、定位运行时状态汇总、权限/队列诊断展示

## 函数级结构化伪代码

### StatusSeverity / StatusActionTarget / StatusActionRoute
#### 枚举定义
- 输入：无
- 输出：严重级别、动作目标、动作路由枚举值
- 副作用：无
- 步骤：
  1. Severity：Info / Warning / Critical
  2. Target：Settings / Login / Permissions / Status / Sync / Queue / None
  3. Route：OpenSettings / OpenPermissions / TriggerSync / StayOnStatus / None
- 分支与异常：无
- 调用：无

### StatusActionRouter
#### route(target: StatusActionTarget): StatusActionRoute
- 输入：`target` 动作目标
- 输出：对应导航/动作路由
- 副作用：无
- 步骤：
  1. Settings、Login → OpenSettings
  2. Permissions → OpenPermissions
  3. Sync、Queue → TriggerSync
  4. Status → StayOnStatus
  5. None → None
- 分支与异常：`when` 穷尽
- 调用：无

### StatusIssue
#### companion 工厂方法（apiAddressMissing、loginMissing 等）
- 输入：部分方法接受 reasonCode、message、count、mode、lastOccurredAtMillis 等
- 输出：预填 code/severity/title/message/actionLabel/target 的 `StatusIssue`
- 副作用：无
- 步骤：
  1. `requiredIssueCodes()` 返回必须覆盖的 code 集合
  2. 各工厂方法构造固定文案与目标页的问题条目
- 分支与异常：message 可空时使用默认文案（如 heartbeatFailure）
- 调用：`StatusIssue` 构造

### QueueStatusSnapshot
#### pendingUploadTotal（计算属性）
- 输入：各 pending* 字段
- 输出：除 pendingLogs 外的待上传总量
- 副作用：无
- 步骤：
  1. 求和 location + usage events/summaries + app metadata + device profile + sync batches
- 分支与异常：无
- 调用：无

### StatusTrackingMapper
#### fromRuntime(profile, runtime): TrackingPolicySnapshot
- 输入：配置 profile 名、前台定位运行时状态
- 输出：`TrackingPolicySnapshot`
- 副作用：无
- 步骤：
  1. 映射 `currentPolicyMode`、`nextExpectedLocationAtMillis`
- 分支与异常：无
- 调用：读取 `ForegroundLocationRuntimeState` 字段

### StatusCenterState
#### empty(): StatusCenterState
- 输入：无
- 输出：默认空快照 + 由 planner 生成的 issues
- 副作用：无
- 步骤：
  1. 构造偏“未就绪”的默认 `StatusCenterSnapshot`
  2. 调用 `StatusIssuePlanner.plan(snapshot)` 生成 issues
  3. 返回 `StatusCenterState(snapshot, issues)`
- 分支与异常：无
- 调用：`StatusIssuePlanner.plan`

### StatusIssuePlanner
#### plan(snapshot: StatusCenterSnapshot): List<StatusIssue>
- 输入：完整状态中心快照
- 输出：去重后的问题列表（按 code distinct）
- 副作用：无
- 步骤：
  1. 初始化可变列表
  2. API：空白/missing → apiAddressMissing；无效 → apiUrlInvalid；warnings 含 real-device-localhost → localhost 警告
  3. Auth：无 token → loginMissing；过期 → loginExpired
  4. 权限：通知/前台定位/后台定位/使用情况/运动识别缺失时追加对应 issue
  5. 服务：持续采集开启但服务未运行 → foregroundServiceNotRunning
  6. 策略：currentPolicyMode 非空 → currentPolicyState
  7. 诊断丢弃原因：精度相关 → locationAccuracyRejected；altitude-missing-timeout → altitudeMissingTimeout；任意非空 reason → recentDroppedLocation
  8. 队列：pendingLocationPoints ≥ 10 → uploadQueueBacklog
  9. 心跳状态含 fail/失败 → heartbeatFailure
  10. 最近日志非空 → recentError
  11. `distinctBy { it.code }` 后返回
- 分支与异常：多条件顺序追加；最后按 code 去重
- 调用：`StatusIssue.*` 工厂方法

## 近逐行中文伪代码

1. [L1] 包 `com.pim.app.status`
2. [L3] 导入 `ForegroundLocationRuntimeState`
3. [L5-9] 枚举 `StatusSeverity`
4. [L11-19] 枚举 `StatusActionTarget`
5. [L21-27] 枚举 `StatusActionRoute`
6. [L29-38] `StatusActionRouter.route` 将 Target 映射到 Route
7. [L41-48] 数据类 `StatusIssue` 字段定义
8. [L51-58] `requiredIssueCodes` 返回六类必检 code
9. [L60-223] 各工厂：API/登录/权限/服务/精度/高度/队列/心跳/最近错误/策略/丢弃定位
10. [L227-282] 各类 Snapshot 数据类：权限、API、Auth、前台服务、策略、队列、诊断
11. [L267-273] `pendingUploadTotal` 汇总多队列（不含 logs）
12. [L284-292] `StatusTrackingMapper.fromRuntime` 映射策略快照
13. [L295-303] `StatusCenterSnapshot` 聚合
14. [L305-328] `StatusCenterState` 与 `empty()` 默认态
15. [L331-405] `StatusIssuePlanner.plan`：按快照字段顺序生成 issues 并 distinctBy code

## 关系边
```json
{
  "nodes": [
    {
      "id": "src/client-android/app/src/main/java/com/pim/app/status/StatusIssue.kt",
      "label": "StatusIssue",
      "path": "src/client-android/app/src/main/java/com/pim/app/status/StatusIssue.kt",
      "doc": "docs/pseudocode/files/src/client-android/app/src/main/java/com/pim/app/status/StatusIssue.kt.md",
      "layer": "client-android",
      "kind": "dto"
    }
  ],
  "edges": [
    { "from": "src/client-android/app/src/main/java/com/pim/app/status/StatusIssue.kt", "to": "src/client-android/app/src/main/java/com/pim/app/location/service/ForegroundLocationRuntimeState.kt", "type": "depends_on" },
    { "from": "StatusIssuePlanner", "to": "StatusIssue", "type": "calls" },
    { "from": "StatusTrackingMapper", "to": "ForegroundLocationRuntimeState", "type": "depends_on" }
  ]
}
```
