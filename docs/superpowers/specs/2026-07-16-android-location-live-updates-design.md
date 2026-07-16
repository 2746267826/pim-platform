# Android Location Live Updates Design

## Goal

为 PIM Android 端**已有**的前台持续定位过程增加 Android 16 标准 Live Updates（流体云）展示，让用户在锁屏/状态栏相关区域快速看到定位是否在跑、多久更新过、精度与策略模式。

本设计**不修改**定位采样策略、质量门、上传队列语义；采样/断点/耗电仅作研究附录。

## Existing Context

仓库：`src/client-android`。

已具备完整后台定位链路：

| 组件 | 路径 | 作用 |
|---|---|---|
| 前台定位服务 | `app/.../location/service/ForegroundLocationService.kt` | `LocationManager` 订阅、策略驱动 interval、质量门、入队、FGS |
| 策略引擎 | `app/.../location/policy/LocationPolicyEngine.kt` | 省电 / 日程低频 / 运动观察 / 移动恢复 / 同步兜底 |
| 通知渲染 | `app/.../notifications/LocationNotificationRenderer.kt` | 通知 ID `7101`，channel `pim_location_collection` |
| 动作 | `NotificationActionReceiver` | 暂停 / 恢复 / 同步 |

当前通知折叠文案把策略、下次定位、精度、待上传、API 挤在一行；无 Live Updates / ProgressStyle；`compileSdk` / `targetSdk` 为 34。

## Chosen Approach

**方案 B：`LocationLiveUpdatePresenter`（纯 Kotlin 状态机）+ `LocationNotificationRenderer`（只组装 Notification）。**

理由：

- 采集逻辑已稳定，展示状态（含 30s 成功保持）适合独立可测。
- 仍使用**同一条** FGS 通知 `7101`，与现有常驻通知无双条冲突。
- 后续扩展流体云字段不必继续膨胀 `ForegroundLocationService`。

拒绝的方案：

- **A 仅增强 Renderer**：更快，但 phase/30s/文案优先级会堆在 Service 与字符串函数里，可测性差。
- **C 双通知（FGS + 独立 Live Update）**：易双条并存，与「与常驻通知无冲突」目标相悖。

## Scope

### In scope

- Android 16+ 标准 Live Updates（`requestPromotedOngoing` + ProgressStyle 方向 API）。
- 展示状态机：Collecting / SuccessHold(30s) / Degraded / Paused。
- 折叠简略文案 + 展开详细文案。
- `ForegroundLocationService` 薄集成：事件 → Presenter → notify。
- `compileSdk` 升到 36；低版本忽略 Live Update API，保留新文案的常驻通知。
- 单元测试与现有 Service/Renderer 回归。

### Out of scope

- 修改 `LocationPolicyEngine` 间隔、运动升频、distanceFilter。
- 轨迹缺口虚线/分段渲染（地图侧）。
- 国产 ROM 私有流体云 / 灵动岛 API。
- 第二条定位通知。
- 上传协议、队列、服务端 API 变更。
- OEM 保活白名单引导（可另开任务）。

## Product Requirements

1. **只要定位采集在进行（含后台自动定位）就展示**对应通知；API 36+ 请求 Live Update 提升。
2. **简略（折叠/流体云）**：默认 `定位中 + 相对时间`；成功后保持约 30s 显示精度与相关摘要；展开显示完整详情。
3. **与现有常驻 FGS 通知同一条**，不并行第二条定位 ongoing 通知。
4. **低版本**：不调用 Live Update API；不崩溃；仍显示更新后的折叠/展开文案。
5. **动作保留**：暂停/恢复、同步、状态。
6. **展示失败不影响采集**（权限不足等采集层决策除外）。

## Architecture

```text
LocationManager callback
        │
        ▼
ForegroundLocationService          # 采集 / 策略 / 队列（策略不改）
  accepted / dropped / mode / api / queue / provider
        │  LocationLiveUpdateEvent
        ▼
LocationLiveUpdatePresenter        # 新建，纯 Kotlin
  reduce(event) → LocationNotificationUiModel
  successHoldDeadlineMillis()
        │
        ▼
LocationNotificationRenderer       # 改造：只组装
  build(context, uiModel)
        │
        ▼
Notification ID 7101
  startForeground / notify
  API 36+：promote + ProgressStyle
  API <36：普通 ongoing 通知
```

### Responsibilities

| 组件 | 负责 | 不负责 |
|---|---|---|
| `ForegroundLocationService` | 定位、策略、入队；派发事件；按 deadline 调度 `Tick` | 折叠/展开文案、promote 细节 |
| `LocationLiveUpdatePresenter` | phase、文案模型、ongoing、是否希望 promote、30s hold、进度语义 | Android Framework |
| `LocationNotificationRenderer` | channel、图标、actions、Notification 组装、SDK 门闩 | 业务判断 |
| `LocationPolicyEngine` | 保持现状 | 展示 |

### File layout (expected)

```text
notifications/
  LocationLiveUpdatePresenter.kt    # new (events + phase + reduce)
  LocationNotificationUiModel.kt    # new (or colocated with presenter)
  LocationNotificationRenderer.kt   # change: build(UiModel)
  LiveUpdateNotificationCompat.kt   # new: API 36 apply helper
location/service/
  ForegroundLocationService.kt      # thin integration
app/build.gradle.kts                # compileSdk 36
```

## State Machine and Copy

### Phases

| Phase | Enter | Exit |
|---|---|---|
| `Collecting` | 连续采集开启且 mode ≠ Off | accepted / degraded / paused |
| `SuccessHold` | 刚 accepted 一点 | 30s 到期 → Collecting；再 accepted → 重置 30s |
| `Degraded` | 权限不足、provider 关闭、严重异常等 | 恢复或暂停 |
| `Paused` | mode == Off / 用户暂停 | 恢复采集 |

**Priority when multiple signals apply:**

```text
Paused > Degraded(permission/provider) > SuccessHold > Degraded(drop/api soft) > Collecting
```

普通丢点不打断 SuccessHold 主句；展开区仍可显示「最近丢弃」。

### Events (Service → Presenter)

```text
Snapshot(...)           # start / resume / full refresh
Accepted(...)
Dropped(...)
PolicyChanged(...)
ApiChanged(...)
QueueChanged(...)
ProviderDisabled(...)
Paused
Tick                    # hold deadline or recompute relative time
```

### UiModel (Presenter → Renderer)

```text
phase
mode
isOngoing
requestLiveUpdate       # business intent; Renderer also checks SDK >= 36
title                   # "PIM 定位"
collapsedText
expandedText
shortStatus             # mode short label
progressPercent         # null or 0..100 toward next expected fix
contentAction           # pause/resume action
```

### Collapsed / Live Update short copy

| Phase | Primary |
|---|---|
| Collecting | `定位中 · {相对时间}` 或 `定位中 · 等待首次定位` |
| SuccessHold | `已定位 · 精度 {xxm}` |
| Degraded (drop) | `定位异常 · {短原因}` |
| Degraded (provider) | `定位中断 · GPS/网络已关` |
| Degraded (permission) | `无法定位 · 权限不足` |
| Paused | `定位已暂停` |

相对时间：`刚刚` / `N秒前` / `N分钟前` / `HH:mm`。

模式短名：省电、日程低频、运动、移动恢复、同步兜底、已暂停。

### Expanded copy (fixed order)

```text
状态：{phase 中文}
策略：{完整模式名}
最近更新：{相对时间}（{HH:mm}）
精度：{lastAccuracyText}
下次定位：{nextExpectedLocationText}
最近位置：{lastAcceptedLocationText}
待上传 {n}，API {apiState}
最近丢弃：{reason}          # optional
```

### SuccessHold (30s)

1. `Accepted` → phase = SuccessHold，`deadline = now + 30_000`。
2. Service 取消旧 `Runnable`，`Handler(main).postDelayed(Tick, delay)`。
3. `Tick` 且已过期且仍在采集 → Collecting 并 notify。
4. hold 内再次 Accepted → 重置 30s。
5. hold 内 Paused → 立即 Paused，取消定时器。
6. **只改展示，不改是否请求定位。**

### ProgressStyle / Live Update (API 36+)

| Item | Rule |
|---|---|
| `requestPromotedOngoing` | `isOngoing == true` |
| short status | mode short label |
| progress | 有 nextExpected 时 0..100 时间比例；否则 indeterminate 或省略 |
| segments | 第一版不做复杂分段 |

`< 36`：不调用 promote/ProgressStyle；collapsed/expanded 仍用新规则。

### Actions (unchanged)

- Active: 暂停 / 同步 / 状态  
- Paused: 恢复 / 同步 / 状态  

## Component Interfaces

### Presenter

```kotlin
class LocationLiveUpdatePresenter(
    private val successHoldMillis: Long = 30_000L,
    private val clock: () -> Long = { System.currentTimeMillis() }
) {
    fun reduce(event: LocationLiveUpdateEvent): LocationNotificationUiModel
    fun current(): LocationNotificationUiModel
    fun successHoldDeadlineMillis(): Long?
}
```

文案拼装**全部在 Presenter**。删除生产路径对 `LocationNotificationState` 的依赖；测试迁移到 Event/UiModel。

### Renderer

```kotlin
object LocationNotificationRenderer {
    const val CHANNEL_ID = "pim_location_collection"
    const val NOTIFICATION_ID = 7101
    fun build(context: Context, model: LocationNotificationUiModel): Notification
}
```

`LiveUpdateNotificationCompat.applyIfSupported(...)` 隔离平台 API；异常时回退普通通知。

Channel：第一版保持 `IMPORTANCE_LOW` 与现有 id。若真机无法 promote，另开任务换**新 channel id**（旧 channel 改 importance 对已装用户无效）。

### Service integration

```text
dispatch(event) =
  presenter.reduce(event)
  publishRuntimeState()
  nm.notify(7101, Renderer.build(current))
  scheduleSuccessHoldTick(deadline)
```

`startForeground` 与 `updateNotification` 共用同一构建路径。

挂载点：

| Existing site | Event |
|---|---|
| startCollection before/at startForeground | Snapshot |
| policy change in handleLocation | PolicyChanged or Snapshot fields |
| queueAccepted | Accepted |
| recordDropped | Dropped |
| onProviderDisabled | ProviderDisabled |
| apiState changes | ApiChanged |
| pause | Paused |
| resume/start | Snapshot |
| hold Runnable | Tick |

`onDestroy` / `stopCollection`：移除 hold 回调。

`runtimeState` 第一版可不暴露 phase；可选后续加。

## Build configuration

| Item | Decision |
|---|---|
| `compileSdk` | **36**（必须） |
| `targetSdk` | 实现阶段评估；允许先 **compile 36 / target 保持 34**，验证 promote 后再决定是否升 target |
| Third-party Live Update libs | 不引入 |

## Error handling and degradation

| Case | Behavior |
|---|---|
| SDK &lt; 36 | 无 promote；新文案常驻通知 |
| promote/ProgressStyle 失败 | catch 后普通通知；采集继续 |
| 缺定位权限 | Degraded 文案；沿用现有不启动采集逻辑 |
| provider disabled | Degraded 主句；FGS 策略按现有 |
| quality drop | expanded 显示丢弃；可不打断 SuccessHold 主句 |
| pause | ongoing=false；取消 Tick |
| stop | cancel 7101 |
| process death + FGS restart | Snapshot 重建；无 hold |

**Hard rule:** 展示层失败不得停止 `requestLocationUpdates`（采集层权限/开关决策除外）。

## Testing

### `LocationLiveUpdatePresenterTest` (pure JVM)

1. Snapshot → Collecting  
2. Accepted → SuccessHold + deadline  
3. Second Accepted resets deadline  
4. Expired Tick → Collecting  
5. Early Tick keeps SuccessHold  
6. Paused during hold  
7. Dropped appears in expanded; priority rules  
8. Provider/permission degraded primary copy  
9. Mode short labels  
10. progressPercent bounds / null when paused  

### `LocationNotificationRendererTest` (Robolectric)

1. ongoing follows `isOngoing`  
2. pause shows 恢复  
3. content from UiModel  
4. `requestLiveUpdate=false` does not promote  
5. channel id unchanged  

### `ForegroundLocationServiceTest` (regression)

1. start → 7101 present  
2. pause → notification semantics preserved  
3. stop → no 7101  
4. sync paths no stray notification bugs  

### Manual

1. Android 16：采集中 Live Update 可见性  
2. 成功后 ~30s 主句回落  
3. 暂停/恢复/同步  
4. Android 14/15：仅常驻通知，无崩溃  

## Research appendix: sampling gaps and battery (not implementation)

### Observed problem

运动中点间隔过大时，轨迹两点被直线硬连，路径失真。

### Relation to current defaults

| Mode | Default interval | Validator min |
|---|---|---|
| Power saving | 3 min | 60s |
| Motion observation | 60s | 30s |
| Schedule low frequency | 15 min | 5 min |

Also: accuracy gate (default upload only if accuracy &lt; 50m), OEM background limits, motion transition lag, time-based sparsity even with `minDistance=0`.

### Do not

Make max-rate GPS the all-day default. Battery and heat cost is high; stationary periods gain little.

### Recommended future directions (separate work)

1. **Tiered sampling**: keep low rate when still; shorten interval (e.g. 5–15s) or time+distance triggers when Walking/Running/Bicycle/InVehicle.  
2. **Gap rendering**: if point gap &gt; threshold, draw dashed/segmented lines instead of solid false continuity.  
3. **Use existing motion signals** to upshift/downshift rather than only fixed timers.  
4. **Decouple quality drops from map density**: mark gaps; surface drop reasons (Live Update already helps awareness).  
5. **Live Update value**: does not fix gaps; shows staleness, drops, and mode so users know recording quality in real time.

### Battery heuristic

High-rate GPS for a 30–90 minute sport session is often acceptable. All-day 5–15s GPS usually is not. Prefer session upshift + idle downshift.

## Success criteria

1. 连续定位开启时，通知 7101 持续反映 Presenter 状态。  
2. API 36 设备上请求 Live Update 提升；失败时静默回退。  
3. 定位 accepted 后主句约 30s 显示精度相关摘要，然后回到「定位中 · 相对时间」。  
4. 展开信息不少于现网（策略、下次定位、精度、队列、API、丢弃原因），并增加状态与相对时间。  
5. 暂停/停止/同步回归通过。  
6. 采样策略代码路径无行为变更。  

## Implementation note

实现须在 `codex/` 前缀分支进行，按仓库 `AGENTS.md`：验证、PR、等待 CI。本 spec 获批后下一步为 `writing-plans` 产出实现计划，再编码。
