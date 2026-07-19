# PIM Android 定位 Live Updates 设计规范

## 背景

Android 16 API 36 引入 progress-centric notification，Android 16 QPR1 full SDK 36.1 进一步公开 `POST_PROMOTED_NOTIFICATIONS` 和 `setRequestPromotedOngoing()`。满足资格的 ongoing 通知可被系统提升到通知抽屉顶部、锁屏和状态栏芯片，但最终提升由系统决定。PIM Android 客户端已有前台定位服务常驻通知，用于承载服务与采集状态；它不能准确表达每一轮几十秒的实际定位窗口。

用户希望将每一轮定位采集（包括手动触发和后台自动采集）的进行状态通过 Live Update 呈现为类似“流体云”的体验，同时在应用内新增独立的定位页面，提供完整的定位结果查看、手动提交和上传队列管理。

## 目标

- 在单次定位采集会话期间，通过 Android Live Update 实时展示采集进展。
- 新增独立的定位页面，包含当前状态、最佳候选结果、手动提交/重新定位操作和上传队列摘要。
- 自动定位结果通过质量门后自动入队上传；手动定位结果由用户显式确认后再入队。
- 现有前台服务常驻通知不受影响，与 Live Update 使用完全独立的 NotificationChannel 和 notification ID。
- 不依赖任何 OEM 私有 API（如 OPPO 流体云、VIVO 原子通知、华为实况窗），仅使用 Android 平台公开 API。

## 现状事实

- `app/build.gradle.kts`：compileSdk 34、targetSdk 34、minSdk 26。
- Android Gradle Plugin：8.4.0。
- Gradle wrapper：8.14。
- 项目使用 Hilt、Compose、Google Play services location 和 Room；根导航由 `PimRootScreen`/`PimDestination` 的枚举状态驱动。
- 底部导航现有 5 项：今日 → 轨迹 → 日程 → 状态 → 设置。
- 现有前台服务 `ForegroundLocationService` 负责连续定位采集调度，常驻通知由 `LocationNotificationRenderer` 构建，ID 为 `7101`，channel ID 为 `pim_location_collection`。
- 现有 `LocationCaptureRepository` 已包含手动高精度连续采样、等待时长、最佳结果展示、质量门和手动/自动提交基础，但只接入旧 `PimAppScaffold`，没有接入当前根导航。
- 现有 `pendingUploadCount` 是内存中只增不减的计数器，不是待上传数量的准确来源。
- 现有 `MobileDataDao.pendingLocationPointCount()` 已提供定位记录待上传数量的权威 Flow，不需要数据库迁移或新表。

## 已确认需求

### 功能需求

1. Live Update 必须覆盖手动定位和后台自动采集的每一轮定位采集会话。
2. 定位采集采用“限时连续采样协调器”：每轮注册高精度或策略指定优先级的位置更新回调，最长约 30 秒，保留本轮最佳候选；成功获取、超时或取消后立即解绑位置回调。不使用长期监听或推测窗口。
3. 新增独立“定位”页面和底部导航入口。
4. 自动定位结果通过质量门后自动加入上传队列；手动定位成功后先展示在页面上，只有用户点击“提交位置”才入队。
5. 定位页面显示经纬度、海拔、来源（Provider）、记录时间等完整结果；Live Update 和锁屏绝不显示经纬度。
6. 现有前台服务常驻通知与 Live Update 完全独立：不同 NotificationChannel 和 notification ID。API 36.1 以下不创建 Live Update 渠道或第二条通知，原常驻通知保持原行为。
7. Android 系统是否最终将 Live Update 提升为状态栏芯片或锁屏强化样式是 best-effort，应用不能保证。
8. “今日”页面、状态中心、定位页面同时展示待上传总数和单独的定位记录数量。

### 非功能需求

- 同时只允许一个采集会话。自动定位进行中时，手动按钮显示“定位进行中”并禁用，但定位页面仍然显示该自动过程的进展。手动定位进行中时，到期自动轮次延后等待；手动会话不改变连续采集开关的开关状态。
- 使用唯一 session ID 忽略取消或替换后迟到的位置回调。
- 所有代码标识、API 名、字段名使用英文；用户可见文案使用简体中文。
- 本规范中的所有产品和实现选择均为已确认决定。

## 架构

### 组件职责

#### `LocationAcquisitionEngine`

纯粹的单轮位置采集执行器，职责范围：

- 接收本轮时限、定位优先级、会话开始时间等参数。
- 只使用 `FusedLocationProviderClient.requestLocationUpdates()` 在有界窗口内连续接收候选位置；本设计不使用 `getCurrentLocation()` 路径。
- 维护一个 **最优候选**：在新位置到来时与当前最佳比较精度（`accuracy`），保留水平精度更好的位置。
- 支持显式解绑（`removeLocationUpdates` / 取消 `Task`）。
- 提供包含 session ID、最佳位置、耗时等信息的采集结果回调。
- 不负责：UI、通知、上传、策略调度、并发互斥。

内部采用 `FusedLocationProviderClient.requestLocationUpdates()` 和 `LocationCallback`，在 30 秒窗口内持续接收更新并筛选最佳候选。早于本轮会话开始时间的缓存结果直接丢弃。Engine 将候选更新交给 Coordinator；是否达到质量要求由 Coordinator 与现有 `LocationQualityGate`/`AltitudeWaitCoordinator` 决定。Coordinator 可以要求 Engine 提前结束。高度等待只能使用 30 秒总期限内的剩余时间；如果水平精度合格但到总期限仍缺海拔，则以 `altitude-missing-timeout` 标记接受。30 秒总期限或取消始终优先，并立即调用 `removeLocationUpdates()`。

#### `LocationAcquisitionCoordinator`

定位采集的总协调器，是一个 Hilt `@Singleton` 业务组件，由前台服务和页面 ViewModel 共同注入。它不是 ViewModel，也不依赖具体页面生命周期。

- 暴露权威的 `StateFlow<LocationAcquisitionState>`，供 UI、Live Update Publisher 和服务层订阅。
- 统一处理手动触发和自动触发入口，确保 **同时只有一个采集会话**。
- 在会话开始前执行 `Preparing` 检查：精确定位权限、系统定位开关、Google Play services 可用性、互斥条件（是否已有活跃会话）、读取连续采集设置。
- `Preparing` 检查失败时不注册位置回调，状态转为 `Failed` 并写入可展示的 `errorReason`。
- 会话开始时生成唯一 `sessionId`（`UUID.randomUUID().toString()`），存入状态并在整个会话生命周期中传递。
- 在收到 `LocationAcquisitionEngine` 的结果回调后进入 `Evaluating` 阶段，判断：
  - 无任何位置 → `TimedOut`（记录超时原因）。
  - 有位置但水平精度缺失或大于等于 `maxUploadAccuracyMetersExclusive` → `Failed`（记录低质量原因；页面可查看但不可提交；自动采集丢弃并记录日志）。
  - 手动触发且位置合格 → `AwaitingManualSubmit`。
  - 自动触发且位置合格 → `Enqueuing`（将位置写入 Room）。
- 自动 `Enqueuing` 成功后状态转为 `Completed`；Room 写入失败时转为 `Failed`，不自动重试。
- 手动模式下用户点击“提交位置”后进入 `Enqueuing`；成功后转为 `Completed`，Room 写入失败时保留候选位置并返回 `AwaitingManualSubmit`，同时展示错误，允许用户重试。
- 用户可在 `Preparing`、`Acquiring`、`Evaluating` 或 `AwaitingManualSubmit` 阶段取消当前 session，状态转为 `Cancelled`。已开始的 Room 入队事务不提供取消入口。
- 迟到回调（session ID 不匹配）直接忽略。

`LocationAcquisitionState` 的核心字段：

```
data class LocationAcquisitionState(
    val sessionId: String?,
    val triggerType: TriggerType?,  // MANUAL, AUTOMATIC; Idle 时为 null
    val phase: AcquisitionPhase,    // Idle, Preparing, Acquiring, Evaluating, AwaitingManualSubmit, Enqueuing, Completed, TimedOut, Failed, Cancelled
    val bestLocation: LocationSnapshot?,
    val startedAtElapsedRealtimeMs: Long?,
    val deadlineAtElapsedRealtimeMs: Long?,
    val elapsedMs: Long,
    val maxUploadAccuracyMetersExclusive: Float,
    val errorReason: String?
)

enum class TriggerType { MANUAL, AUTOMATIC }
enum class AcquisitionPhase { Idle, Preparing, Acquiring, Evaluating, AwaitingManualSubmit, Enqueuing, Completed, TimedOut, Failed, Cancelled }
```

#### 现有手动采集迁移

- 从现有 `LocationCaptureRepository` 提取 bounded engine、状态与提交逻辑，避免创建第二套 Fused Location 回调所有者。
- 旧 `LocationCaptureRepository` 最终改为 Coordinator 的薄 facade 或被其取代；旧 `PimAppScaffold` 的手动定位 ViewModel 不得继续维护独立采集状态机。
- 移除现有手动路径“精度合格后自动提交”的行为。新的手动 session 只能进入 `AwaitingManualSubmit`，必须由用户确认。
- 现有 `LocationQualityGate`、`AltitudeWaitCoordinator`、`LocationQueueRepository` 和 `MobileSyncScheduler` 继续复用。

#### `ForegroundLocationService`

现有前台服务，继续负责：

- 连续定位策略：决定何时启动下一轮自动采集。
- 持有现有常驻通知（`LocationNotificationRenderer.NOTIFICATION_ID` + `LocationNotificationRenderer.CHANNEL_ID`）。
- **不再永久注册位置回调**。取而代之，在策略决定采集时，调用 `LocationAcquisitionCoordinator.startAutomaticSession()`；在该会话结束后等待一段时间（依据策略）再启动下一轮。
- 监听 `LocationAcquisitionCoordinator.stateFlow`，在自动会话结束后按策略安排下一轮。
- 新增手动定位 action。定位页通过 `ForegroundLocationController` 启动该 action，确保用户离开应用前台后，本轮定位仍在 location FGS 中可靠运行。此 action 不改变连续采集开关。
- 如果连续采集未开启，服务只在手动会话的采集阶段保持前台；结束采集后停止自身，但手动结果继续由进程内 Coordinator 保留，等待页面提交。

#### `LocationLiveUpdatePublisher`

仅 API 36.1+ 可用，职责范围：

- 在 `LocationAcquisitionCoordinator.stateFlow` 上订阅。
- 当 phase 进入 `Acquiring` 时，发布一条 ongoing BigTextStyle Live Update。
- 使用平台 `android.app.Notification.Builder` 和 `Notification.BigTextStyle` 构建通知；由于该路径只会在 API 36.1+ 执行，不使用 `NotificationCompat.Builder` 包装这些新平台 API。
- 在 `Acquiring` 内，当精度明显改善（horizontal accuracy 改善超过 `notificationThrottleAccuracyImprovementMeters`）或已等待秒数需要刷新时更新通知；进入该阶段时发布，离开该阶段时撤销。
- 更新节流：最多每 `notificationThrottleIntervalMs`（2 秒）更新一次。
- Live Update 内容规则：
  - `setShortCriticalText()`：初始为“定位中”，有候选后显示类似“±18m”。
  - 展开内容（`setBigText`）：显示“手动/自动定位”、已等待秒数、最佳精度和来源。
  - 绝不显示经纬度。
- 操作：提供“取消”和“打开定位页”两个 `PendingIntent`。
- 当 phase 离开 `Acquiring` 时立即取消（撤销）Live Update，包括进入 `Evaluating`、`AwaitingManualSubmit`、`Enqueuing` 或任何最终态；不发“完成”通知。
- 使用独立的 NotificationChannel（id: `pim_location_live_update`，name: “定位动态”，importance 为 `IMPORTANCE_LOW`，不得为 `IMPORTANCE_MIN`）。
- 使用独立的 notification ID（`LIVE_UPDATE_NOTIFICATION_ID = 7102`）。
- 预检查执行顺序：调用统一 capability helper 判断 full SDK 36.1，随后检查 `POST_NOTIFICATIONS`、`canPostPromotedNotifications()`，最后构建通知并在测试/调试校验 `hasPromotableCharacteristics()`。
- 通知必须设置 `contentTitle`、`setOngoing(true)` 和标准 `BigTextStyle`；不得使用 custom `RemoteViews`、group summary 或 `setColorized(true)`。
- 设置 `deleteIntent`。用户临时隐藏或降级本轮 Live Update 后，同一 session 不得因后续精度更新再次发布；下一轮 session 可重新发布。
- API 36.1 以下或预检查不通过时完全 no-op，不创建第二条普通回退通知。

Capability helper 必须覆盖 36.0 运行时：

1. `SDK_INT < 36` 返回 false。
2. `SDK_INT > 36` 返回 true。
3. `SDK_INT == 36` 时，在隔离方法的 `try` 块内计算 `Build.VERSION.SDK_INT_FULL >= Build.VERSION_CODES_FULL.BAKLAVA_1`；捕获 `LinkageError` 后返回 false。该捕获同时覆盖 36.0 运行时可能出现的 `NoSuchFieldError` 和 `NoClassDefFoundError`。

该 helper 只使用正式类型化平台字段，不使用反射或字符串版本判断。

#### `LocationScreen` / `LocationViewModel`

- `LocationViewModel` 订阅 `LocationAcquisitionCoordinator.stateFlow`，转换为 `LocationUiState`。
- `LocationScreen` 分为四个全宽区域：当前状态、当前最佳位置、结果操作、上传队列。
- 当前状态区域：显示触发类型（手动/自动）、阶段、已等待时间、30 秒期限。
- 当前最佳位置区域：显示精度（水平）、来源（GPS/NETWORK/FUSED）、纬度、经度、海拔、速度、方向和记录时间。
- 结果操作区域：
  - `Idle`：显示“开始定位”按钮。
  - `Preparing` / `Acquiring` / `Evaluating`：显示“取消”按钮。
  - `AwaitingManualSubmit`：显示“提交位置”和“重新定位”按钮。
  - `Enqueuing`：显示不可重复触发的“提交中”状态。
  - 其他最终态：显示“重新定位”按钮。
  - 自动采集进行中时，手动按钮显示“定位进行中”并禁用。
- 上传队列区域：显示 `pendingUploadTotal`（所有类型待上传总数）和 `pendingLocationPoints`（定位记录待上传数量）。

#### 导航更新

- 在 `PimDestination` 中新增 `Location` 条目，放在“今日”和“轨迹”之间；导航顺序变为：今日 → 定位 → 轨迹 → 日程 → 状态 → 设置。
- 图标使用准星/定位标准 Material Icon。
- 底部导航项从 5 项变为 6 项，必须在 320dp/360dp 窄屏上验证两字标签和图标不重叠。若现有 Material 3 `NavigationBar` 无法满足稳定尺寸，实施必须在同一 PR 内调整为能容纳六个固定目的地的等宽底部导航，不得隐藏“定位”入口或依赖横向滚动。

#### 队列计数

- `pendingUploadTotal`：来自全局上传队列状态的跨类型总数。
- `pendingLocationPoints`：来自 Room DAO `pendingLocationPointCount()`，是定位待上传数量的权威来源。
- 必须在“今日”页面、状态中心和定位页面同步展示这两个计数。
- 移除或停止使用 `ForegroundLocationService.pendingUploadCount` 这种只增不减的内存计数。

## 状态机

### 状态转换

```
Idle → Preparing
Preparing ├→ Acquiring
          └→ Failed
Acquiring → Evaluating
Evaluating ├→ AwaitingManualSubmit → Enqueuing → Completed
           ├→ Enqueuing ─────────────────────────→ Completed
           ├→ TimedOut
           └→ Failed
活跃采集或待提交阶段 → Cancelled
```

手动 `Enqueuing` 失败时返回 `AwaitingManualSubmit` 并保留候选位置；自动 `Enqueuing` 失败时进入 `Failed`。只有写入 Room 成功才进入 `Completed`。

### 状态语义

| 阶段 | 含义 |
|------|------|
| `Idle` | 无活跃采集会话 |
| `Preparing` | 检查精确定位权限、系统定位开关、Google Play services 可用性、互斥条件、读取连续采集设置 |
| `Acquiring` | 最长 30 秒位置采集窗口；Live Update 只在此阶段存在；手动使用高精度，自动保持现有策略优先级但拒绝早于会话开始的陈旧缓存结果 |
| `Evaluating` | 硬件采集已结束；判断位置质量；无位置→超时；低质量→失败但页面可查看不可提交；自动采集记录丢弃原因 |
| `AwaitingManualSubmit` | 手动模式合格位置等待用户确认；用户可提交或重新定位；离开页面不自动提交 |
| `Enqueuing` | 自动采集通过质量门，或手动结果经用户确认后，将合格结果写入 Room 并触发现有同步调度 |
| `Completed` | 采集成功完结 |
| `TimedOut` | 30 秒无任何位置 |
| `Failed` | 预检查失败、30 秒内只有低质量位置，或自动入队失败 |
| `Cancelled` | 用户或系统显式取消 |

### 并发规则

- 同时只允许一个采集会话。
- 自动定位进行中时，手动按钮显示“定位进行中”并禁用；定位页面仍显示该自动过程。
- 手动定位进行中时，到期自动轮次延后等待；手动会话不改变连续采集开关。
- 迟到回调（session ID 不匹配）直接丢弃。

## 页面设计

### 底部导航

```
当前位置：[今日] [定位(新)] [轨迹] [日程] [状态] [设置]
```

新“定位”项放置在“今日”和“轨迹”之间，使用 `Icons.Default.MyLocation` 或类似定位图标。

### 定位页面布局

四个全宽区域垂直排列：

**区域 1：当前状态**
- 触发类型：手动定位 / 自动定位
- 阶段：准备中 / 采集中 / 评估中 / 等待提交 / 入队中 / 已完成 / 超时 / 失败 / 已取消
- 已等待时间：动态更新的秒数
- 30 秒期限：倒计时或固定显示
- 错误/原因文字（超时/失败时）

**区域 2：当前最佳位置**
- 水平精度（如 ±15 米）
- 来源（GPS / 网络 / 融合）
- 纬度（数字）
- 经度（数字）
- 海拔（米）
- 速度（米/秒）
- 方向（度）
- 记录时间（ISO 8601 或本地化时间）

**区域 3：结果操作**
- `Idle`：显示“开始定位”按钮
- `Preparing` / `Acquiring` / `Evaluating`：显示“取消”按钮
- `AwaitingManualSubmit`：“提交位置”主要按钮 + “重新定位”次要按钮
- `Enqueuing`：显示“提交中”，禁用重复提交和重新定位
- 最终状态（Completed/TimedOut/Failed/Cancelled）：“重新定位”按钮
- 自动采集中：手动按钮禁用并显示“定位进行中”

**区域 4：上传队列**
- 待上传总数：`pendingUploadTotal`
- 定位记录待上传：`pendingLocationPoints`

### 窄屏适配

- 验证 320dp/360dp 宽度下六项底部导航不重叠。
- 四个区域使用 `LazyColumn` 或 `Column` + `verticalScroll` 确保可滚动。

## 通知设计

### Live Update 通知

| 属性 | 值 |
|------|-----|
| 通知渠道 ID | `pim_location_live_update` |
| 通知渠道名称 | 定位动态 |
| 通知 ID | `LIVE_UPDATE_NOTIFICATION_ID` (`7102`，与 `LocationNotificationRenderer.NOTIFICATION_ID` 不同) |
| 样式 | `Notification.BigTextStyle` |
| `setOngoing` | true |
| `setRequestPromotedOngoing` | true |
| `setShortCriticalText` | 初始“定位中”；有最佳候选后设为类似“±18m” |
| 展开标题 | 手动定位 / 自动定位 |
| 展开内容 | 已等待 X 秒 / 最佳精度 ±Xm / 来源：GPS |
| 操作 | “取消”PendingIntent → 调用 `cancelCurrentSession()` |
| 操作 | “打开定位页”PendingIntent → 导航到 `LocationScreen` |
| 可见性 | `VISIBILITY_PUBLIC`（但绝不包含经纬度） |

### 与常驻通知的隔离

| | 常驻通知 | Live Update |
|--|----------|-------------|
| 渠道 ID | `pim_location_collection` | `pim_location_live_update` |
| 通知 ID | `LocationNotificationRenderer.NOTIFICATION_ID` (`7101`) | `LIVE_UPDATE_NOTIFICATION_ID` (`7102`) |
| 功能 | 保证前台服务存活 | 展示定位采集进展 |
| 显示时机 | 服务运行期间一直显示 | 仅 API 36.1+、权限满足、采集窗口内 |

### 通知节流

- 精度改善超过 `notificationThrottleAccuracyImprovementMeters`（建议 5 米）或阶段改变时触发更新。
- 更新频率不超过每 `notificationThrottleIntervalMs`（2 秒）一次。
- 取消/最终态后立即可撤销，不受节流限制。

### 回退行为

- API < 36.1：完全 no-op，不创建渠道、不发布通知、不显示任何通知行。
- API >= 36.1 但 `canPostPromotedNotifications()` 为 false：完全 no-op，不创建普通通知作为回退。
- 预检查通过后系统仍可能把第二条通知降级为普通通知；应用不能原子地阻止这一系统行为，且不应通过轮询 notification flags 制造闪烁式发布/撤销。

## 兼容与构建

### AGP 与 SDK 升级

| 项 | 当前值 | 目标值 | 说明 |
|---|--------|--------|------|
| app compile SDK | 34.0 | 36.1 | Kotlin DSL 使用 `compileSdk = 36` 与 `compileSdkMinor = 1`，访问 36.1 平台 API |
| app targetSdk | 34 | 34 | 不升级 target，避免 Android 16 行为变更 |
| minSdk | 26 | 26 | 不变 |
| AGP | 8.4.0 | 8.13.2 | AGP 8.13 支持 API 36.1 SDK；8.13.2 是 2026-07-20 已从 Google Maven 核实存在的该系列最新稳定补丁 |
| Gradle | 8.14 | 8.14 | 不变 |

### API 使用

- 版本判断统一走上一节 capability helper。不能只用 `SDK_INT >= 36` 后直接读取 full SDK 字段，因为 Android 36.0 运行时尚未提供这些字段。
- 不使用魔法字符串或反射。
- `36.1` 是 full SDK 版本的概念表示，不作为 `compileSdk` 的浮点字面量；`app/build.gradle.kts` 必须写成 `compileSdk = 36` 和 `compileSdkMinor = 1`。
- Live Update 使用平台 `Notification.Builder`；现有常驻通知可以继续保持自己的 `NotificationCompat` 实现，两者互不要求迁移。
- Manifest 使用普通 `<uses-permission android:name="android.permission.POST_PROMOTED_NOTIFICATIONS" />`；旧系统会忽略未知权限。该权限不能替代现有 `POST_NOTIFICATIONS`。
- Live Update 实现隔离在 `location/liveupdate/` 包中，以 `@RequiresApi(36)` 标注 major API 边界，并由 capability helper 执行 full SDK 36.1 检查；不得把 full SDK 编码值直接传给只接受 major API 的 `@RequiresApi`。
- 不为了本功能升级 AndroidX Core、Kotlin 或 Compose 版本。

## 异常与恢复

### 预检查失败

- 权限不足、定位开关关闭、Google Play services 不可用：不启动采集、不发布 Live Update；页面提示具体原因并提供“打开设置”入口。

### 缺少后台定位权限

- 跳过自动采集。
- 前台手动采集仍然可用（因为应用在前台时有精确位置权限即可）。

### 超时

- 30 秒无任何位置：状态转为 `TimedOut`，页面显示“定位超时”。

### 低质量

- 只有水平精度缺失或大于等于 `maxUploadAccuracyMetersExclusive` 的位置：状态转为 `Failed`。
- 页面显示但不允许提交；自动采集丢弃并记录丢弃原因。

### 取消

- 用户显式取消或承载会话的服务/协程被停止：解绑位置回调、撤销 Live Update。
- 关闭连续采集开关只取消正在进行的自动 session；手动 session 与该开关独立，不因此取消。

### 自动入队失败

- Room 写入失败时状态转为 `Failed`，不自动重试、不重复写入，并记录错误日志。

### 手动提交失败

- 状态返回 `AwaitingManualSubmit`，保留当前结果并展示提交错误，允许用户重试“提交位置”。

### 上传失败

- 不重新显示 Live Update；失败信息只反映在队列计数和同步状态中。

### 进程异常终止

- 不恢复同一 GPS 请求；下一次服务启动或应用打开时，清理残留 Live Update（通过 `NotificationManager.cancel(LIVE_UPDATE_NOTIFICATION_ID)`）。

### 应用启动/服务恢复

- Coordinator 为进程内状态，进程重建后默认没有活跃 session。`PimApp`/`RunningStateRestorer` 在启动和服务恢复时无条件取消残留的 `LIVE_UPDATE_NOTIFICATION_ID`，之后只有新 session 才能重新发布。

## 测试验收

### 单元测试 (`test` 源码集)

| 测试目标 | 场景 |
|---------|------|
| `LocationAcquisitionEngine` | 超时无位置 |
| `LocationAcquisitionEngine` | 取消后解绑 |
| `LocationAcquisitionEngine` | 最佳候选选择（多个位置取最优精度） |
| `LocationAcquisitionEngine` | 丢弃早于会话开始时间的缓存位置 |
| `LocationAcquisitionEngine` | 迟到回调（解绑后收到回调） |
| `LocationAcquisitionCoordinator` | 精度合格且有海拔时提前结束采集 |
| `LocationAcquisitionCoordinator` | 只有低精度位置时进入 `Failed` |
| `LocationAcquisitionCoordinator` | 水平精度合格但海拔等待超时时标记后接受 |
| `LocationAcquisitionCoordinator` | 预检查失败时不启动 Engine 并进入 `Failed` |
| `LocationAcquisitionCoordinator` | 手动确认后入队 |
| `LocationAcquisitionCoordinator` | 手动入队失败时保留结果并返回 `AwaitingManualSubmit` |
| `LocationAcquisitionCoordinator` | 自动单次入队 |
| `LocationAcquisitionCoordinator` | 自动入队失败时进入 `Failed` 且不重试 |
| `LocationAcquisitionCoordinator` | 互斥：手动时自动延后 |
| `LocationAcquisitionCoordinator` | 互斥：自动时手动按钮禁用 |
| `LocationViewModel` / queue repository | 待上传总数和定位记录数发布 |
| `LocationLiveUpdatePublisher` | 通知节流 |
| `LocationLiveUpdatePublisher` | 采集窗口外无通知 |
| `LocationLiveUpdatePublisher` | 最终态撤销 |
| `LocationLiveUpdatePublisher` | 用户隐藏后同一 session 不重新发布 |
| `ForegroundLocationService` | 手动 action 不改变连续采集开关 |

### 平台测试

| 测试目标 | 场景 |
|---------|------|
| API < 36.1 | 不创建 `pim_location_live_update` 渠道 |
| API < 36.1 | 不发布第二条通知 |
| API 36.0 | capability helper 捕获缺失 full SDK 类型或字段造成的 `LinkageError`，不崩溃且不发布 Live Update |
| API 36.1 `canPostPromotedNotifications() = false` | 不发布 Live Update |
| API 36.1 | 构建的 Live Update 满足 `hasPromotableCharacteristics()` |
| 渠道和 ID | `pim_location_live_update` 与 `pim_location_collection` 不同 |
| 渠道和 ID | `LIVE_UPDATE_NOTIFICATION_ID = 7102`，与 `LocationNotificationRenderer.NOTIFICATION_ID = 7101` 不同 |

### UI / 仪器测试

| 测试目标 | 场景 |
|---------|------|
| 导航 | 六项底部导航在 320dp/360dp 不重叠 |
| 页面 | 四个区域完整显示 |
| 状态 | 手动时显示手动状态、自动时显示自动状态 |
| 互斥 | 自动定位进行中时手动按钮禁用 |
| 坐标 | 页面显示经纬度、Live Update 不显示经纬度 |
| 队列 | 总数和定位数均正确更新 |

### 手动验证

- API 36.1 模拟器：验证 Live Update 通知发布、promotion 资格和撤销。
- API 34 模拟器：验证无 Live Update、无第二条通知、常驻通知正常工作。
- 系统是否最终绘制芯片/强化样式只能人工验收，不能作为自动化硬断言。

### 运行命令

```
gradlew.bat :app:testDebugUnitTest --no-daemon
gradlew.bat :app:connectedDebugAndroidTest --no-daemon
```

## 非目标

- OEM 私有流体云/实况窗适配（OPPO ColorOS、VIVO 原子通知、华为实况窗）。
- 地址反向地理编码（将坐标转换为街道地址）。
- Android 37 `MetricStyle`。
- targetSdk 36 升级（当前保持 targetSdk 34）。
- 定位历史页面。
- 持久化未提交的手动定位结果（进程死亡后丢弃）。
- 定位完成通知（采集结束不发通知，只撤销 Live Update）。

## 官方依据

- Android Live Update 文档（2026-07-14 最后更新）：https://developer.android.com/develop/ui/views/notifications/live-update
- `Notification.ProgressStyle` API 36：https://developer.android.com/reference/android/app/Notification.ProgressStyle
- `Build.VERSION_CODES_FULL.BAKLAVA_1`：https://developer.android.com/reference/android/os/Build.VERSION_CODES_FULL#BAKLAVA_1
- `POST_PROMOTED_NOTIFICATIONS`：https://developer.android.com/reference/android/Manifest.permission#POST_PROMOTED_NOTIFICATIONS
- `setRequestPromotedOngoing(boolean)`：https://developer.android.com/reference/android/app/Notification.Builder#setRequestPromotedOngoing(boolean)
- `canPostPromotedNotifications()`：https://developer.android.com/reference/android/app/NotificationManager#canPostPromotedNotifications()
- Promotion 设置入口：https://developer.android.com/reference/android/provider/Settings#ACTION_APP_NOTIFICATION_PROMOTION_SETTINGS
- `FusedLocationProviderClient`：https://developers.google.com/android/reference/com/google/android/gms/location/FusedLocationProviderClient
- AGP 8.13 支持 API 36.1：https://developer.android.com/build/releases/past-releases/agp-8-13-0-release-notes

## 附录：数据流摘要

### 手动定位流程

```
用户点击“定位” → LocationCoordinator.startManualSession()
    → Preparing（检查权限、开关、GMS、互斥）
    → Acquiring（Engine 注册高精度回调，Publisher 发布 Live Update）
    → Evaluating（判断质量）
    → AwaitingManualSubmit（页面显示结果，Live Update 撤销）
    → 用户点击“提交位置” → Enqueuing（写入 Room）
    → Completed
```

### 自动定位流程

```
ForegroundLocationService 策略到期
    → LocationCoordinator.startAutomaticSession()
        → Preparing
        → Acquiring（Engine 注册策略优先级回调，Publisher 发布 Live Update）
        → Evaluating（判断质量）
          → 合格：Enqueuing（写入 Room）→ Completed
          → 不合格：Failed（Live Update 撤销，记录日志）
    → ForegroundLocationService 安排下一轮
```

### 取消流程

```
用户点击取消 / 自动 session 中关闭连续采集 / 承载会话的服务或协程停止
    → LocationCoordinator.cancelCurrentSession()
        → Engine.removeLocationUpdates()
        → state → Cancelled
        → Publisher.cancelLiveUpdate()
```
