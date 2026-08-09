# src/client-android/app/src/main/java/com/pim/app/status/StatusCenterRepository.kt

## 元信息
- 语言：Kotlin
- 程序集或包：com.pim.app.status（client-android app）
- 职责：聚合权限、API/鉴权、前台服务、队列与诊断，产出 `StatusCenterState` 及问题规划。
- 主要依赖：`PermissionStatusRepository`、`ServerSettingsStore`、`TokenManager`、`TrackingSettingsStore`、`AppDatabase`/`MobileDataDao`、`MobileSyncCoordinator`、`StatusRefreshSignal`、`StructuredLogRepository`、`ForegroundLocationService`
- 被谁使用：状态中心 UI / ViewModel

## 函数级结构化伪代码

### StatusCenterRepository
#### observe(): Flow\<StatusCenterState\>
- 输入：无（订阅内部 Flow）
- 输出：状态中心状态流
- 副作用：在 IO 调度器上组合多源
- 步骤：
  1. `combine`：队列快照、诊断快照、同步状态、前台定位 runtime、刷新版本
  2. 合并诊断：心跳状态与错误日志来自 `sync`
  3. `buildSnapshot` → `StatusIssuePlanner.plan` → `StatusCenterState`
  4. `flowOn(Dispatchers.IO)`
- 分支与异常：诊断日志空时回退 `sync.lastError`
- 调用：`queueSnapshotFlow`、`diagnosticSnapshotFlow`、`buildSnapshot`、`StatusIssuePlanner.plan`

#### requestRefresh()
- 输入：无
- 输出：Unit
- 副作用：触发 `StatusRefreshSignal`
- 步骤：1. `refreshSignal.requestRefresh()`
- 分支与异常：无
- 调用：`requestRefresh`

#### snapshotNow(queues, diagnostics, runtime): StatusCenterState
- 输入：可选队列/诊断/runtime 默认值
- 输出：一次性快照状态
- 副作用：读设置与令牌（经 `buildSnapshot`）
- 步骤：`buildSnapshot` + `StatusIssuePlanner.plan`
- 分支与异常：无
- 调用：`buildSnapshot`、`plan`

#### buildSnapshot(queues, diagnostics, runtime): StatusCenterSnapshot
- 输入：队列、诊断、runtime
- 输出：完整快照
- 副作用：读 baseUrl、校验、tracking 设置、权限与 token
- 步骤：
  1. 取 baseUrl 并 `ServerUrlValidator.validate`
  2. 读 tracking settings
  3. 组装 permissions / api / auth / service / tracking / queues / diagnostics
- 分支与异常：token 空或过期反映在 auth 快照
- 调用：`permissionStatusRepository.snapshot`、`tokenManager`、`StatusTrackingMapper.fromRuntime`

#### queueSnapshotFlow(): Flow\<QueueStatusSnapshot\>
- 输入：无
- 输出：各 pending 计数字段
- 副作用：读 Room DAO Flow
- 步骤：combine 六类 pending count → `QueueStatusSnapshot`（pendingLogs 固定 0）
- 分支与异常：无
- 调用：DAO pending*Count

#### diagnosticSnapshotFlow(): Flow\<DiagnosticSnapshot\>
- 输入：无
- 输出：丢点与最近日志诊断
- 副作用：读 DAO 与日志仓库
- 步骤：
  1. combine 最近丢点诊断 + refresh 版本
  2. 取最新 drop 与最近 6 条日志
  3. 构造 `DiagnosticSnapshot`（heartbeat 先 null，由 observe 合并）
- 分支与异常：列表可空
- 调用：`dao.recentDroppedLocationDiagnostics`、`logRepository.recent`

## 近逐行中文伪代码

1. [L1-19] 包与导入：DB、定位服务、同步、权限、设置、Token、Flow
2. [L21-31] `@Singleton` 注入依赖；[L32] 取 `MobileDataDao`
3. [L34-52] `observe`：五路 combine → 合并诊断 → 快照+问题规划 → IO
4. [L54-56] `requestRefresh` 转发信号
5. [L58-65] `snapshotNow` 用默认/入参建快照
6. [L67-95] `buildSnapshot`：校验 URL、token、前台服务与 tracking 映射
7. [L97-118] 队列 Flow：六路 pending 计数
8. [L120-136] 诊断 Flow：最近丢点 + 结构化日志

## 关系边
```json
{
  "nodes": [
    {
      "id": "src/client-android/app/src/main/java/com/pim/app/status/StatusCenterRepository.kt",
      "label": "StatusCenterRepository",
      "path": "src/client-android/app/src/main/java/com/pim/app/status/StatusCenterRepository.kt",
      "doc": "docs/pseudocode/files/src/client-android/app/src/main/java/com/pim/app/status/StatusCenterRepository.kt.md",
      "layer": "client-android",
      "kind": "service"
    }
  ],
  "edges": [
    { "from": "src/client-android/app/src/main/java/com/pim/app/status/StatusCenterRepository.kt", "to": "src/client-android/app/src/main/java/com/pim/app/data/AppDatabase.kt", "type": "depends_on" },
    { "from": "src/client-android/app/src/main/java/com/pim/app/status/StatusCenterRepository.kt", "to": "src/client-android/app/src/main/java/com/pim/app/data/MobileDataDao.kt", "type": "depends_on" },
    { "from": "src/client-android/app/src/main/java/com/pim/app/status/StatusCenterRepository.kt", "to": "src/client-android/app/src/main/java/com/pim/app/location/service/ForegroundLocationService.kt", "type": "depends_on" },
    { "from": "src/client-android/app/src/main/java/com/pim/app/status/StatusCenterRepository.kt", "to": "src/client-android/app/src/main/java/com/pim/app/mobile/logs/StructuredLogRepository.kt", "type": "depends_on" },
    { "from": "src/client-android/app/src/main/java/com/pim/app/status/StatusCenterRepository.kt", "to": "src/client-android/app/src/main/java/com/pim/app/mobile/sync/MobileSyncCoordinator.kt", "type": "depends_on" },
    { "from": "src/client-android/app/src/main/java/com/pim/app/status/StatusCenterRepository.kt", "to": "src/client-android/app/src/main/java/com/pim/app/permissions/PermissionStatusRepository.kt", "type": "depends_on" },
    { "from": "src/client-android/app/src/main/java/com/pim/app/status/StatusCenterRepository.kt", "to": "src/client-android/app/src/main/java/com/pim/app/settings/TrackingSettingsStore.kt", "type": "depends_on" },
    { "from": "src/client-android/app/src/main/java/com/pim/app/status/StatusCenterRepository.kt", "to": "src/client-android/core/src/main/java/com/pim/core/auth/TokenManager.kt", "type": "depends_on" },
    { "from": "src/client-android/app/src/main/java/com/pim/app/status/StatusCenterRepository.kt", "to": "src/client-android/core/src/main/java/com/pim/core/settings/ServerSettingsStore.kt", "type": "depends_on" },
    { "from": "src/client-android/app/src/main/java/com/pim/app/status/StatusCenterRepository.kt", "to": "src/client-android/core/src/main/java/com/pim/core/settings/ServerUrlValidator.kt", "type": "calls" }
  ]
}
```
