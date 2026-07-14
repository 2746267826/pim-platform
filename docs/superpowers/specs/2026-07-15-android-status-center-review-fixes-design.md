# Android 状态中心审查修复设计

## 目标

修复 PR #27 审查确认的状态中心缺陷，使同步、网络、服务器探测、问题列表和操作反馈都基于可解释的事实，同时保持个人项目所需的轻量实现。

本设计是 PR #27 的修复补充，不替代 `2026-07-11-android-client-lightweight-completion-design.md`，也不把尚未实现的 Android 总计划功能写成已完成。

## 范围

本轮处理：

- Accepted 生命周期和历史 WorkInfo 污染。
- 前置阻塞、周期同步和 WorkInfo `BLOCKED` 的相位映射。
- `ACCESS_NETWORK_STATE`、网络三态和真机异常兜底。
- 连接探测并发、旧证据覆盖和用户主动操作反馈。
- 中文展示净化、Info 分组、窄屏和确认成立的 minor 项。
- JVM、Robolectric、Compose 本地门禁和 PR CI 验证。

本轮不处理：

- 新数据库表、WorkManager generation/CAS 或 WorkRequest UUID 跟踪。
- ProbeRunner、全局事件总线、完整 i18n 或模拟器 CI。
- 大规模拆分 `StatusIssue.kt`、`StatusCenterScreen.kt` 或删除旧 UI 栈。
- 今日、轨迹、日程、受信任 Web 内容以及诊断 ZIP 的后续实现。

诊断 ZIP/日志导出仍是 Android 轻量总计划中的保留功能。状态页净化只改变普通 UI 展示，不删除原始日志和错误事实。

## 同步状态

### 事实来源

- WorkInfo 只解释当前活动工作：`ENQUEUED`、`RUNNING` 和 `BLOCKED`。
- 历史 `SUCCEEDED`、`FAILED`、`CANCELLED` 不参与相位选择，因为 unique work 查询没有可依赖的历史排序契约。
- 最后一次完成、真实失败和前置阻塞由已持久化的 `MobileSyncState` 解释。
- `periodic RUNNING` 与 `immediate RUNNING` 都表示正在同步。

### 相位优先级

1. 本次同步请求刚成功提交：`Accepted`。
2. 任一 immediate/periodic work 正在运行：`Running`。持久化的 `isInProgress` 只提供进度文字，不能在没有活动 WorkInfo 时单独证明仍在运行。
3. immediate work 为 `ENQUEUED` 或 WorkInfo `BLOCKED`：`Waiting`。
4. `MobileSyncState.phase` 为 `server-missing`、`auth-missing` 或 `usage-permission-missing`：`Blocked`。
5. 持久状态的 outcome 不是 `SUCCESS`，或 phase 为 `failed`、以 `-failed` 结尾、`completed-with-errors`：`Failed`；前三个前置阻塞 phase 已由第 4 条优先处理。
6. 持久状态 phase 为 `completed`、`uploaded` 或 `location-uploaded`，且不满足失败条件：`Completed`。
7. 其余状态：`Idle`。

`Cancelled` 保留为兼容展示值，但不从无序历史 WorkInfo 推断。

### Accepted 生命周期

`StatusSyncActionRunner` 只在 `enqueueNow()` 成功返回后发布 Accepted；提交异常时不发布，并由状态页显示固定中文错误。

Repository 的 `combine` 只计算映射结果，不修改其输入 Flow。它同时产出状态和“是否应清理 Accepted”的标记；下游先向 UI 发射 Accepted，再在活动 immediate work 出现后清理信号。这样旧终态 WorkInfo 不会清理新信号，UI 也能确定性看到 Accepted 后再进入 Waiting/Running。

进程重启后 Accepted 不恢复。活动工作仍由 WorkInfo 显示 Waiting/Running，终态由持久化 `MobileSyncState` 恢复。

## 网络与服务器探测

### 系统网络三态

`NetworkStatusProvider` 输出：

- `Unavailable`：没有活动网络，或系统 API 因 `SecurityException` 无法读取。
- `Restricted`：存在活动网络，但缺少 `INTERNET` 或 `VALIDATED` 能力。
- `Validated`：同时具备 `INTERNET` 和 `VALIDATED`。

Manifest 增加 normal permission `ACCESS_NETWORK_STATE`。初始读取、callback 注册和 `onLost` 重读分别捕获 `SecurityException` 并 fail closed；仅在成功注册后注销 callback。`onAvailable` 不再无条件报告在线，而是读取 capabilities，后续由 `onCapabilitiesChanged` 更新。

### 网络与 PIM 服务器分层

状态页分别显示“系统网络”和“PIM 服务器”，服务器探测不被系统网络三态短路：

- 系统网络 `Unavailable` 是阻塞问题。
- 系统网络 `Restricted` 且服务器不可达或未检查时是警告。
- 系统网络 `Restricted` 但局域网 PIM 服务器可达时只显示状态信息，不阻止同步判断。
- Probe `Blocked` 和 `Partial` 始终独立生成服务器问题；`Reachable` 明确表示 PIM 服务器可达。

### 探测并发

- 在现有单例 `ConnectionProbeService` 内使用一个 `Mutex`，保证任一时刻只有一次网络探测。
- 用户 force 探测可等待当前自动探测完成；不引入抢占、任务代次或额外协调器。
- 移除状态页 init/ON_RESUME 额外启动的重复探测，保留可见页面的顺序轮询。
- Settings 和 Status 继续共用同一 service/store，并统一走现有探测策略。
- `ConnectionProbeStore.save()` 在同一服务器身份下拒绝更旧的 `checkedAtUtcMillis` 覆盖较新证据；比较和写入都在现有锁内。若当前证据时间位于系统当前时间之后，视为时钟回拨并允许新证据替换。
- 探测抛出未预期异常时沿用 30 秒重试；正常返回的 `Blocked`、`Partial`、`Reachable` 都是有效证据并沿用 5 分钟窗口。自动轮询不产生用户操作提示。

## 操作反馈

反馈是 ViewModel 层的轻量 UI 状态，不进入 Repository 事实模型，也不新增 SnackbarHost：

- 手动连接检查：`检查中`、`检查已完成`、`检查未完成，请稍后重试`。
- 同步调度异常：`同步请求未能提交，请稍后重试`。
- 同步提交成功继续由 Accepted/Waiting/Running 相位反馈。
- WorkManager 执行后的真实失败继续进入同步相位和问题列表，不复用瞬时反馈。
- 自动刷新只更新事实，不覆盖设置页或状态页的用户操作反馈。

所有异常反馈使用固定中文，不拼接原始 exception message。

## UI 展示

### 问题分组

- “需要处理”只显示 Critical 和 Warning，Critical 排在前面。
- Info 全部保留到仅在非空时出现的“状态信息”。
- 总览只统计阻塞与警告；Info 不把整体状态从 Normal 提升。

### 中文净化

增加一个小型纯函数文件，仅映射状态页实际展示的：

- API 地址 reason code。
- tracking profile 和 current policy mode。
- 定位丢弃原因和同步状态。

未知值统一回退为“未知状态”“其他原因”或“暂无”，不得回显 raw code、枚举名或 literal `null`。`ConnectionProbeResult.safeMessage` 是受控 UI 文案，可以显示。

`lastError`、`lastLogMessage` 和 `recentLogMessages` 不直接上屏。普通 UI 只显示固定摘要，例如“最近同步出现异常，请导出日志查看详情”和“有近期诊断记录”。原始内容继续保留在现有日志与状态存储中，供后续诊断 ZIP 使用。

### Minor

- `WorkInfo.State.BLOCKED` 映射为 Waiting；periodic RUNNING 映射为 Running。
- 删除未使用 import。
- 删除 `QueueStatusSnapshot.pendingLogs` 死字段；本地结构化日志不是上传队列，不改为查询 `pendingLogCount()`。
- 修正测试 FakeStore 的 freshness 行为。
- issue 列表使用稳定 `issue.code` key。
- 同步相位文本获得剩余宽度，按钮保持稳定；计数文本限制单行并使用 ellipsis。
- 不因本轮修改拆分整个状态模型或 Screen 文件。

## 测试与验收

实现遵循 TDD，每项先加入能复现审查问题的失败测试。

JVM/Robolectric 必须覆盖：

- 旧 FAILED 与后续成功事实共存时不显示旧失败。
- 旧终态存在时新请求仍依次显示 Accepted、Waiting/Running。
- active immediate、periodic RUNNING、WorkInfo BLOCKED、前置 Blocked 和真实失败。
- 三态网络、`INTERNET + VALIDATED`、`SecurityException` 和 callback 生命周期。
- probe Mutex 不并发、同服务器旧 timestamp 被拒绝、时钟回拨可恢复。
- 手动反馈与自动静默、中文映射、原始错误不进入普通 UI。
- Info 分组、`pendingLogs` 移除和 FakeStore freshness。

Compose 本地门禁覆盖：

- “需要处理”和“状态信息”分区。
- 手动 probe 与同步提交错误的 inline 反馈。
- 网络与服务器事实分开展示。
- 320dp 级窄屏下同步行、计数和 issue 动作不溢出。

实施前先合并最新 `origin/master`；当前上游与 PR #27 无 Android 文件重叠。完成后运行：

```powershell
cd src\client-android
.\gradlew.bat :app:testDebugUnitTest --no-daemon
.\gradlew.bat :app:assembleDebug --no-daemon
.\gradlew.bat :app:connectedDebugAndroidTest --no-daemon
```

最后一条需要已启动的模拟器或真机，不加入 GitHub Actions。`AGENTS.md` 增加这一条本地门禁说明。当前上游包含 Windows 变更，因此合并后还要运行 `dotnet test Pim.sln`。分支推送后等待 PR #27 的 GitHub Actions 全部通过。

## 完成标准

- 审查列出的三个 Critical 均有回归测试并修复。
- 确认成立的 Important 和本设计列出的 minor 均完成；已判定不成立的忙等和网络设置 fallback 不增加代码。
- 状态页不再由历史 WorkInfo、未经验证网络或原始机器文本误导用户。
- 用户主动同步和连接检查都有即时、可理解的反馈。
- Android unit、assemble、本地 Compose 门禁及 PR CI 通过。
- 未引入数据库 schema、复杂协调器、完整 i18n 或 emulator CI。
