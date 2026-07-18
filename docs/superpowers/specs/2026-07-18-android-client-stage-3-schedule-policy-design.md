# Android 客户端 Stage 3：日程与采集策略轻量设计

**最终目标：** 在保留现有 Stage 1/2 功能的前提下，让 Android 日程页、采集策略、状态页和诊断导出都由同一份真实日程事实驱动；用户能明确区分新鲜、空、过期和失败状态。

**设计状态：** 已在 2026-07-18 逐段确认。用户选择运动间隔方案 A 和 15 分钟缓存新鲜度，采用“单份 JSON 缓存 + 原生 Compose 页面”方案。

## 1. 范围与原则

本阶段交付四类用户可见能力：

1. 日程页显示当前项、下一项和前 6 小时至未来 7 天的日程。
2. 日程缓存、刷新时间和失败事实可见，断网时不丢失上次成功数据。
3. 活动日程、运动观察和距离恢复实际影响采集间隔，并在状态页可解释。
4. 状态页和诊断 ZIP 展示缓存与策略事实，不泄露凭据。

实现遵循以下边界：

- 复用现有 Calendar API、`ScheduleWindowRepository`、`LocationPolicyEngine`、设置校验、策略历史和诊断导出。
- 不新增 Room 日程表、数据库迁移、独立刷新 Worker、第二个调度器或策略编排框架。
- 日程页保持原生 Compose；不新增 WebView 日程入口、日程 CRUD 或 Outlook 编辑流程。
- 失败不清空健康缓存；成功返回空列表才表示服务端确实没有日程。

## 2. 架构与数据流

```text
Calendar API
    -> ScheduleWindowRepository
    -> ScheduleCacheStore (单份、按服务器隔离的 JSON)
         |-> SchedulePolicyViewModel -> SchedulePolicyScreen
         |-> ForegroundLocationService -> LocationPolicyEngine
         |-> StatusCenterRepository / DiagnosticExportRepository
```

`ScheduleWindowRepository` 是唯一的查询入口和快照所有者：

- 固定查询范围为 `[now - 6h, now + 7d)`，按开始时间排序。
- 页面和采集服务读取同一快照，不各自请求或维护列表。
- `refreshIfStale()` 由页面进入、采集启动和现有策略评估循环复用。
- 一个进程内 `Mutex` 只合并并发刷新，不引入持久锁、generation 或 CAS。

缓存文件按当前服务器 origin 隔离；切换服务器或退出登录时清理旧服务器缓存，避免跨服务器展示数据。文件只保存日程和时间元数据，不保存 access token、refresh token 或认证设置。

## 3. 缓存契约与新鲜度

`ScheduleCacheStore` 保存一个 JSON 文档，字段如下：

```json
{
  "windows": [],
  "rangeStartMillis": 0,
  "rangeEndMillis": 0,
  "lastAttemptAtMillis": 0,
  "lastSuccessAtMillis": 0,
  "lastError": null
}
```

- 新鲜度阈值固定为 15 分钟。
- 写入先写临时文件，再替换正式文件；读取失败或 JSON 损坏按“无可用缓存”处理，并写入诊断日志。
- API 成功且返回空数组：写入空数组和新的 `lastSuccessAtMillis`，页面进入正常空状态。
- API 失败且存在缓存：保留 `windows` 和 `lastSuccessAtMillis`，更新尝试时间/错误并标记过期。
- API 失败且没有缓存：返回无缓存错误，不伪装成空列表。
- 刷新成功后通过 StateFlow/回调发布新快照；失败只发布 freshness/error，不覆盖健康数据。

事件映射保留可解析时间的全部日程，包括没有地点的事件。地点为空不会使事件从日程页消失，也不再作为策略是否生效的前置条件。

## 4. 原生日程页面

新增 `SchedulePolicyViewModel`，向 `SchedulePolicyScreen` 提供以下五种互斥状态：

- `Loading`：首次加载且没有缓存。
- `Content`：缓存新鲜且有日程。
- `Empty`：服务端成功确认没有日程。
- `StaleContent`：有旧缓存但最近刷新失败，保留列表并显示“可能过期”。
- `Error`：没有可显示缓存且刷新失败，显示可读原因和重试入口。

内容层级：

1. 顶部显示来源、上次成功时间、当前刷新状态和刷新按钮。
2. 当前项按 `start <= now < end` 从所有事件中稳定选择；地点可为空。
3. 下一项选择未来开始时间最早的事件。
4. 近期列表覆盖完整缓存范围，按设备时区的日期分组。
5. 策略摘要显示当前模式、实际间隔、触发原因和距离恢复条件。

页面只读。刷新按钮立即显示进行中反馈；登录失效提供前往设置入口，网络或服务端错误提供重试入口。

## 5. 采集策略行为

所有间隔都经过与 `TrackingSettingsValidator` 相同的安全范围约束：常规 1-15 分钟、日程低频 5-60 分钟、运动观察 30 秒-5 分钟。不得绕过现有设置校验。

| 条件 | 策略模式 | 间隔来源 | `scheduleLowFrequency` |
|---|---|---|---|
| 无活动日程 + 静止/未知 | `PowerSavingNormal` | 常规间隔 | false |
| 无活动日程 + 步行/跑步 | `MotionObservation` | 运动观察间隔 | false |
| 无活动日程 + 骑行/车载 | `MotionObservation` | 运动观察间隔的一半，最低 30 秒 | false |
| 活动日程 + 静止/未知 | `ScheduleLowFrequency` | 日程低频间隔 | true |
| 活动日程 + 任意运动 | `MotionObservation` | 步行/跑步为原值；骑行/车载为一半并限幅 | false |
| 日程内超过恢复距离 | `MovementRecovery` | 按当前运动状态使用常规或运动间隔 | false |

活动日程不要求地点非空；地点只影响展示内容。活动日程开始后的第一次被接受定位作为锚点，超过用户设置的恢复距离后进入 `MovementRecovery`，直到该日程结束或切换。日程结束/切换时重置锚点和恢复标志。策略引擎不得修改持续采集意图。

采集服务只应用引擎返回的 `PolicyDecision`，并将 `mode`、`reason`、`requestIntervalMillis` 和 `nextExpectedLocationAtMillis` 发布到运行时状态。仅当模式、间隔或原因任一变化时写入现有策略历史，完全相同的决定不重复写入。

## 6. 状态与诊断

运行时状态增加日程缓存的派生事实：freshness、`lastSuccessAt`、`lastAttemptAt` 和 `lastError`。状态严重级别统一为：

- 新鲜缓存或服务端确认空列表：正常。
- 旧缓存 + 刷新失败：需注意，保留可用数据和获取时间。
- 无缓存 + 刷新失败：异常，提供重试或设置入口。

状态页继续显示当前策略和下一次定位时间，并增加最近少量策略切换记录，复用现有 DAO 查询。诊断 ZIP 只追加缓存元数据、当前决策和已有策略转换；沿用白名单和凭据扫描，不导出 token、密码或完整认证配置。

## 7. 生命周期与错误边界

- 页面进入、采集启动和现有评估循环都调用同一个 `refreshIfStale()`，不创建第二个后台调度器。
- 进程重建后先读取缓存，再按现有采集恢复入口刷新；缓存和持续采集意图相互独立。
- 服务器切换、登出和凭据清除同时清理对应服务器的日程缓存。
- 401 只走现有认证仓库的刷新/重新登录流程；日程缓存不参与 token 刷新。
- 网络错误、超时和服务端错误保留旧缓存并显示错误事实；不把异常转换成“无日程”。

## 8. 文件边界

新增：

- `src/client-android/app/src/main/java/com/pim/app/schedule/ScheduleCacheStore.kt`
- `src/client-android/app/src/main/java/com/pim/app/ui/schedule/SchedulePolicyViewModel.kt`

按需修改：

- `ScheduleWindowRepository.kt`：统一范围、缓存、地点为空的事件映射和并发刷新。
- `SchedulePolicyScreen.kt`：真实状态和内容布局。
- `ForegroundLocationService.kt`、`ForegroundLocationRuntimeState.kt`：读取快照并发布缓存/策略事实。
- `LocationPolicyEngine.kt`、`LocationPolicyTypes.kt`：运动派生间隔、活动日程边界和安全限幅。
- `StatusCenterRepository.kt`、`StatusIssue.kt`、`StatusCenterScreen.kt`：缓存严重级别和策略历史。
- `DiagnosticExportRepository.kt`：缓存元数据白名单导出。
- 对应单元测试文件。

不修改：Room schema、Calendar API、WebUI embed 路由、Stage 1 同步协议和服务器认证协议。

## 9. 验证与验收

自动验证：

```powershell
dotnet test Pim.sln
cd src/client-android
.\gradlew.bat :core:testDebugUnitTest :app:testDebugUnitTest :app:assembleDebug --no-daemon
git diff --check
```

新增或扩展测试覆盖：缓存读写/损坏/并发/失败保留、五种页面状态、空地点事件、时间边界、运动间隔限幅、距离恢复、策略历史去重、状态严重级别和诊断凭据排除。

有模拟器或真机时运行 `:app:connectedDebugAndroidTest`，人工验证有日程、真实空、离线旧缓存、首次失败、重试、日程切换、运动/距离恢复和进程重启。CI 没有模拟器时，该命令保持本地门禁，不修改 CI 工作流。

## 10. 明确不做

- Room 日程表、数据库迁移、独立 `CalendarRefreshWorker`。
- WebView 日程页、原生日历编辑/删除、Outlook 写回。
- 新的策略 FSM、策略历史表、持久 generation/CAS 或多协调器。
- 为未使用的 `SyncFallback` 枚举扩展功能。
- 物理设备穷举矩阵和企业级发布基础设施。
