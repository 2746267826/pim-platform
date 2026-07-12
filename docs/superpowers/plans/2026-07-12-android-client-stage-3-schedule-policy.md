# Android 客户端阶段 3：日程与采集策略

**最终目标：** 日程页原生显示当前项、下一项和未来 7 天安排，并明确数据时间、空、过期和错误；同一份本地日程缓存驱动现有采集策略，使日程低频、运动间隔和距离恢复可见、可验证且不破坏持续采集意图。

## 实现概要

```
Calendar API → ScheduleWindowRepository → 一份带时间戳 JSON
                                      ├─ SchedulePolicyViewModel → 原生日程页
                                      └─ current window → LocationPolicyEngine
LocationPolicyEngine → ForegroundLocationService → RuntimeState / 现有策略历史
```

- 缓存是应用专属存储中的一个 JSON 文档，不增加 Room 表或数据库版本。
- 页面和策略读取同一 repository 快照；UI 显示所有日程，策略从中选择当前生效窗口。
- 只扩展现有 `LocationPolicyEngine`、service 和状态汇总，不增加策略编排框架。

## 前置依赖

- Stage 1 已完成设置、持续采集意图、状态、诊断和恢复入口。
- 现有 Calendar API、`ScheduleWindowRepository`、`LocationPolicyEngine`、`ForegroundLocationService` 和策略历史实体可复用。

---

## 1. 日程查询与单份 JSON 缓存

**目的：** 查询 `[now-6h, now+7d)`，持久保存最后一次成功数据和最近获取事实；网络失败不把旧数据清空，也不把错误伪装成空结果。

**文件：**
- `src/client-android/app/src/main/java/com/pim/app/schedule/ScheduleWindowRepository.kt`
- `src/client-android/app/src/main/java/com/pim/app/schedule/ScheduleCacheStore.kt`（新建）
- `src/client-android/app/src/main/java/com/pim/app/location/service/ForegroundLocationService.kt`
- `src/client-android/app/src/test/java/com/pim/app/schedule/ScheduleWindowRepositoryTest.kt`
- `src/client-android/app/src/test/java/com/pim/app/schedule/ScheduleCacheStoreTest.kt`（新建）

**复用：** `ApiService.getEvents()`、`EventResponse`、现有 `ScheduleWindow` 和应用已有 JSON 能力；不创建 Calendar DAO、entity 或 worker。

**完成方式：**
1. repository 独占查询范围：从当前时刻前 6 小时到后 7 天，按开始时间排序并缓存所有时间有效的事件；没有 location 的事件也必须保留给日程页。`ForegroundLocationService` 不再自行传入当前的 24 小时范围，只调用 repository 的统一入口。
2. `ScheduleCacheStore` 维护一个 JSON 文档，包含 windows、range、lastSuccessAt、lastAttemptAt、lastError。写入使用临时值后原子替换，损坏 JSON 作为“无可用缓存”并记录诊断，不触碰业务数据库。
3. 一次成功的空列表保存为“真实空”；失败且有缓存返回旧 windows + stale/error；失败且无缓存返回 error。失败只更新尝试时间和错误，不覆盖上次成功列表。
4. 使用一个明确的代码常量判断新鲜度。页面进入和采集启动时调用 `refreshIfStale()`；长时间采集在现有位置/策略评估循环发现缓存过期时复用同一入口，不新增定时 worker 或第二个 scheduler。
5. 页面和 service 可能同时刷新时，由 repository 内一个进程 Mutex 合并请求；它只防重复网络请求，不引入 generation、CAS 或持久锁。

**自动验证：** 覆盖查询边界、含/不含 location 的事件、成功空列表、有缓存失败、无缓存失败、损坏缓存、失败不覆盖健康数据、并发调用只发一次请求和 JSON 不含 token/设置。

**人工验收：** 首次联网打开可见日程；断网后保留上次数据并显示获取时间；服务端确认空时显示真实空状态。

---

## 2. 原生日程页面

**目的：** 用真实数据替换占位内容，清楚展示当前项、下一项、近期列表、新鲜度和策略影响。

**文件：**
- `src/client-android/app/src/main/java/com/pim/app/ui/schedule/SchedulePolicyScreen.kt`
- `src/client-android/app/src/main/java/com/pim/app/ui/schedule/SchedulePolicyViewModel.kt`（新建）
- `src/client-android/app/src/main/java/com/pim/app/ui/root/PimRootScreen.kt`
- `src/client-android/app/src/test/java/com/pim/app/ui/schedule/SchedulePolicyViewModelTest.kt`（新建）

**复用：** 同一份 cached windows、现有 Compose 主题和 Stage 1 运行时状态；日程 Tab 保持原生。

**完成方式：**
1. ViewModel 输出 Loading、Content、Empty、StaleContent、Error 五种状态；`StaleContent` 同时保留列表和警告，`Error` 仅用于没有可显示缓存时。
2. 当前项从所有 `start <= now < end` 的事件中稳定选择；下一项是未来最早开始事件；近期列表覆盖完整缓存范围并按日期分组，不因缺少 location 丢弃。
3. Content 同时显示来源、上次成功时间和当前采集策略影响：是否进入日程低频、当前间隔、触发原因、距离恢复条件。
4. Retry 强制一次刷新并展示进行中反馈；成功空、失败有缓存和失败无缓存必须呈现不同文案。
5. 页面只读，不增加事件编辑、Calendar CRUD、Outlook 管理或 WebView。

**自动验证：** 覆盖五种状态、重试、重叠事件的稳定选择、跨时区显示、无 location 日程可见，以及错误不映射为空列表。

**人工验收：** 有日程时当前/下一项正确；无日程显示“当前无日程安排”；离线显示缓存和“可能过期”；无缓存错误可重试。

---

## 3. 同一缓存接入现有策略引擎

**目的：** service 从 repository 的同一 windows 快照选择当前日程，并使用现有 `LocationPolicyInput.currentScheduleWindow` 驱动策略，不创建第二套模型或缓存。

**文件：**
- `src/client-android/app/src/main/java/com/pim/app/schedule/ScheduleWindowRepository.kt`
- `src/client-android/app/src/main/java/com/pim/app/location/service/ForegroundLocationService.kt`
- `src/client-android/app/src/main/java/com/pim/app/location/service/ForegroundLocationRuntimeState.kt`
- `src/client-android/app/src/main/java/com/pim/app/location/policy/LocationPolicyTypes.kt`
- `src/client-android/app/src/test/java/com/pim/app/schedule/ScheduleWindowRepositoryTest.kt`

**复用：** `ScheduleWindowSelector`、`LocationPolicyInput`、`LocationPolicyEngine.reduce()` 和 service 当前的 `scheduleWindows` 流程。

**完成方式：**
1. 采集启动先读取缓存并异步 `refreshIfStale()`；刷新成功后替换内存快照，失败则继续使用可用旧缓存并单独发布 freshness/error。
2. 每次策略评估从该快照选择 `start <= now < end` 的当前窗口，传入现有 `LocationPolicyInput`。批准设计要求日程时段进入低频，因此不再用 location 是否为空决定事件能否生效；距离恢复仍以该时段第一次接受的位置为锚点。
3. event 时间按服务端 instant 比较，界面按设备时区显示。开始时刻恰好等于 now 为 active；结束时刻恰好等于 now 不再 active；未来事件不提前生效。
4. runtime state 增加缓存新鲜度、上次获取时间和错误，但不持久化权限或实时运动状态。

**自动验证：** 页面与 service 读取同一快照；有/无 location 的活动事件均可进入低频；开始、结束和未来边界正确；刷新失败仍使用旧窗口且状态标记 stale。

**人工验收：** 同一事件在日程页显示，并在活动时段让状态页出现对应的低频原因；修改服务端日程后下一次缓存刷新生效。

---

## 4. 日程低频、运动间隔与距离恢复

**目的：** 补齐现有策略引擎行为，使静止、步行和车载使用不同且有界的有效间隔，日程低频和距离恢复按设置工作，持续采集意图始终保留。

**文件：**
- `src/client-android/app/src/main/java/com/pim/app/location/policy/LocationPolicyEngine.kt`
- `src/client-android/app/src/main/java/com/pim/app/location/policy/LocationPolicyTypes.kt`
- `src/client-android/app/src/main/java/com/pim/app/location/motion/MotionSignalRepository.kt`
- `src/client-android/app/src/main/java/com/pim/app/location/LocationQueueRepository.kt`
- `src/client-android/app/src/main/java/com/pim/app/location/service/ForegroundLocationService.kt`
- `src/client-android/app/src/main/java/com/pim/app/location/service/ForegroundLocationRuntimeState.kt`
- `src/client-android/app/src/main/java/com/pim/app/settings/TrackingSettingsStore.kt`
- `src/client-android/app/src/test/java/com/pim/app/location/policy/LocationPolicyEngineTest.kt`

**复用：** `TrackingPolicy` 的 normal、schedule-low、movement、recovery 参数，现有 motion signals、`GeoDistance`、质量/海拔门控和 `PolicyDecision` 字段。

**完成方式：**
1. 在一个纯函数中把 Still/Unknown、Walking/Running、OnBicycle/InVehicle 映射到 Stage 1 集中参数表派生的有界间隔；标准预设下静止、步行和车载应可区分，车载不得比步行更慢，所有结果受统一安全上下限约束。
2. 无活动日程时按运动状态选择普通或运动观察模式；活动日程且静止时使用 schedule-low 间隔；检测到运动时使用相应运动间隔。
3. 日程开始后第一次接受的位置成为锚点；距离超过恢复阈值后进入 `MovementRecovery`，使用普通/运动间隔，直到该日程结束。此过程不得写回或关闭 `continuousCollectionEnabled`。
4. 沿用实际字段 `requestIntervalMillis`、`nextExpectedLocationAtMillis`、`reason` 和 `mode`；service 只应用 decision，不复制策略判断。
5. 仅在 mode、interval 或 reason 变化时插入现有 `MobileLocationPolicyTransitionEntity`，避免每个定位点产生重复历史。不为未使用的 `SyncFallback` 增加功能。

**自动验证：** 覆盖日程进入/退出、静止/步行/车载的有界间隔、运动覆盖低频、距离阈值前后、跨日程重置、策略未变化不重复写历史和采集意图不变。

**人工验收：** 状态页显示三种运动状态对应的有效间隔；日程内静止降频；移动超过阈值恢复；暂停/恢复和进程重建后用户意图不丢失。

---

## 5. 状态与诊断可见性

**目的：** 用户能看到当前策略为什么生效、下一次定位何时发生、日程缓存是否新鲜，并能从诊断 ZIP 还原最近变化。

**文件：**
- `src/client-android/app/src/main/java/com/pim/app/status/StatusCenterRepository.kt`
- `src/client-android/app/src/main/java/com/pim/app/status/StatusIssue.kt`
- `src/client-android/app/src/main/java/com/pim/app/ui/status/StatusCenterScreen.kt`
- `src/client-android/app/src/main/java/com/pim/app/ui/status/StatusCenterViewModel.kt`
- `src/client-android/app/src/main/java/com/pim/app/data/MobileDataDao.kt`
- `src/client-android/app/src/main/java/com/pim/app/mobile/diagnostics/DiagnosticExportRepository.kt`
- 对应 status 与 diagnostic 测试

**复用：** `ForegroundLocationRuntimeState`、`MobileLocationPolicyTransitionEntity` 和现有 `recentPolicyTransitions()`；不加策略历史表。

**完成方式：**
1. 状态汇总显示 mode、reason、`requestIntervalMillis`、`nextExpectedLocationAtMillis`、cache freshness、lastSuccessAt 和 lastError。
2. stale 且有缓存为“需注意”；无缓存获取失败为可操作“异常”；服务端成功返回空列表为正常事实，不生成错误。
3. 状态页显示当前策略与最近少量切换记录；详细历史留在诊断 ZIP，避免状态页变成日志查看器。
4. 诊断导出复用 Stage 1 白名单和最终 ZIP 扫描，只加入缓存元数据、当前 decision 和现有策略转换，不包含 token 或完整认证设置。

**自动验证：** 覆盖 fresh/empty/stale/error 的严重级别、策略字段映射、最近历史上限和 ZIP 凭据排除。

**人工验收：** 日程低频、运动观察和距离恢复时，状态页原因/间隔/下次定位同步变化；离线缓存和错误状态与日程页一致。

---

## 6. 阶段整体验收

**目的：** 用一次完整自动运行、一次模拟器验收和一次整体审查确认日程与策略闭环。

**自动验证：**
- `dotnet test Pim.sln`
- 在 `src/client-android` 运行 `./gradlew :core:testDebugUnitTest :app:testDebugUnitTest :app:assembleDebug`
- `git diff --check`
- 额外检查 `AppDatabase` 版本、entities 和 migrations 未因日程缓存变化

**人工验收：** 服务端有日程、成功空列表、离线旧缓存、首次无缓存失败与重试；日程进入/退出；静止/步行/车载间隔；距离恢复；状态历史与诊断；进程重建后缓存和采集意图保留。

最后进行一次整体代码审查，重点核对唯一缓存源、时间边界、失败不覆盖健康缓存、策略历史去重和 Stage 1 同步/状态无回归。

---

## 本阶段明确不做

- Room 日程表、`calendar_cache` entity、数据库版本增加或迁移
- `CalendarRefreshWorker`、第二个 scheduler、第二份日程缓存或策略编排器
- 仅为 `SyncFallback` 枚举补功能、重写策略 FSM、增加策略历史表
- 原生日历编辑/CRUD、Outlook 同步实现、WebView 日程页
- 穷举毫秒排列组合、逐任务双重审查或大型证据矩阵

## 完成标准

1. 后端和 Android 完整验证命令零失败，Room schema 保持不变。
2. 模拟器一次通过全部日程、离线和策略场景。
3. 一次整体审查无未处理的数据丢失、状态误报或采集策略关键问题。
