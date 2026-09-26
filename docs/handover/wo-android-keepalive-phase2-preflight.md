# 阶段二 Pre-flight 记录（WO-ANDROID-KEEPALIVE-20260923 v1.3）

> 开工前按 `pim-ai-development-workflow` Pre-flight Gate 逐项核对的结果。
> 基线：`origin/master` = `0ae715e6`（含阶段一 PR #340 与 #345 修复 PR #347）。

## 1. 工单包与规则已读

| 材料 | 状态 |
|---|---|
| `README.md`（阅读指引） | 已读 |
| `docs/工单-主文档.md`（REQ-1~REQ-31） | 已读全文 420 行 |
| `references/决策索引.md`（D1-D40 / R1-R4 映射） | 已读 |
| `references/实证数据.md`（14 天实测 + 复现命令） | 已读 |
| `references/android-平台依据.md`（精确闹钟/Doze/冻结/强停） | 已读 |
| `交接-阶段二.txt`（本次工单正文） | 已读 |
| `AGENTS.md` / `docs/ai-dev-workflow.md` | 已读 |

## 2. 范围确认

- **本次做**：REQ-14 ~ REQ-25（阶段二）+ REQ-26 ~ REQ-31（跨阶段）。
- **不做**：REQ-1 ~ REQ-13（阶段一已完成并合并），不重做。
- 阶段一代码位置已确认（`src/client-android/app/src/main/java/com/pim/app/forensics/` 14 个文件、
  `src/Pim.Core/Liveness/`、`src/modules/Pim.Module.Mobile/`、`src/client-web/.../mobile/`）。

## 3. 现状盘点（基线 0ae715e6，已实查）

| 事实 | 证据 |
|---|---|
| 安卓端**没有**任何 `AlarmManager` / `SCHEDULE_EXACT_ALARM` / `USE_EXACT_ALARM` | `grep -rln "AlarmManager\|SCHEDULE_EXACT_ALARM\|USE_EXACT_ALARM" src/client-android --include=*.kt --include=*.xml` → 零命中 |
| 阶段一已有取证台账/事件类型/心跳/哨兵/上传 | `forensics/` 下 14 个文件 |
| 事件类型契约目前只有 3 种 | `ForensicEventTypes.ALL = {process-exit, force-stop, heartbeat}`（两端各有该常量） |
| 服务端**显式拒绝**未登记事件类型 | `MobileForensicIngestService.KnownEventTypes`，不在其中 → `rejected`（REQ-28 不静默丢弃） |
| 安卓单测门禁可运行 | `./gradlew :app:testDebugUnitTest --offline` → EXIT=0 |
| 模拟器可用且**已在运行** | `adb devices` → `emulator-5554 device`；AVD `pim361`（pixel_7 / Android 16 / API 36 / x86_64 / google_apis） |

## 4. 关键假设与边界（**必读，影响 AC-25 结论的诚实性**）

1. **本机模拟器是 stock Android（`ro.product.brand=google`），不是 ColorOS。**
   → REQ-25 要求的三项实测结论中，「ColorOS 是否派发」「ColorOS 是否自动授予 `USE_EXACT_ALARM`」
   **无法在本机得出**，只能给出模拟器（stock Android 16 / API 36）的可复现证据，
   并把 ColorOS 项列为**需需求方真机确认**。不得用模拟器结论冒充 ColorOS 结论。
2. 模拟器支持 `cmd deviceidle force-idle light|deep`，因此**Doze 下的闹钟派发**可以真实验证。
3. `setExactAndAllowWhileIdle` 的平台约束：每应用每 15 分钟最多一次（工单平台依据 §1）。
   默认 30 分钟节奏高于该下限，符合平台约束。
4. 范围外一律不改（采集频率档位、质量门参数、判定阈值）；发现即记录并单独提出。

## 5. 需求 → 实现落点（初稿，随实现更新）

| REQ | 落点 |
|---|---|
| REQ-14 | Manifest 双声明 + `AlarmPermissionChecker` + 台账事件 + 设置页/引导页入口 |
| REQ-15 | `KeepAliveAlarmScheduler`（`setExactAndAllowWhileIdle`）+ `KeepAliveSettingsStore`（10-120，默认 30） |
| REQ-16 | `KeepAliveWakeReceiver` + `WakeExecutionChain`（台账→查服务→拉起/补传→兜底抓点） |
| REQ-17 | `AlarmSuppressionPolicy`（>15 分钟被压制；连续 3 次 ×2 上限 120；连续 2 次 ≤15 → 回配置值） |
| REQ-18 | 兑现台账（预定/实际/延迟）+ 兑현率计算 |
| REQ-19 | `KeepAliveNotificationRenderer`（常驻静默、滚动更新、与采集通知并存） |
| REQ-20 | 暂停/手动会话语义接入叫醒链 |
| REQ-21 | `KeepAliveHealthMonitor`（四类原因 → 红点 + 通知，双通道） |
| REQ-22 | 设置页「保活与诊断」分区 + 总开关 |
| REQ-23 | `ColorOsGuidanceScreen`（可检测项自动 + 不可检测项手动勾选并记住） |
| REQ-24 | PR 内操作卡（四步） |
| REQ-25 | 模拟器实测结论 + 需真机确认清单 |
| REQ-26~31 | 文案中文、脱敏抽查、失败可见、无新增轮询、回归、双语章节与 AC 映射表 |

## 6. 缺失项 / 阻塞

- **无 Microsoft/ColorOS 真机**：REQ-25 的 ColorOS 三项结论中两项无法在本机完成 → 将显式标
  `NOT-VERIFIED` 并列入「需需求方真机确认清单」，不以模拟器结果冒充。
- 生产库 `pim_prod` 一律不碰；如后续需要真实分布，仅用镜像库 `pim_test`。
