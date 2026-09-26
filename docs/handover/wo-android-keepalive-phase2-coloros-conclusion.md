# 阶段二 ColorOS / 精确闹钟实测结论（REQ-25）

> 工单 WO-ANDROID-KEEPALIVE-20260923 v1.3 · 阶段二交付物
> 证据文件：`docs/handover/evidence-ak2/`（原始输出，非二次转述）
> **本文件区分「本机实测得出」与「未验证、需真机补测」两类结论，不混为一谈。**

## 0. 实测环境（决定了哪些结论能得出）

| 项 | 值 |
|---|---|
| 设备 | `sdk_gphone64_x86_64`（Android 官方模拟器 AVD `pim361`） |
| 品牌 | `brand=google` —— **stock Android，不是 ColorOS** |
| 系统 | Android 16（API 36），x86_64 |
| 包 | `com.pim.app`（debug 构建，含阶段二全部改动） |
| 关键能力 | 支持 `cmd deviceidle force-idle deep`，因此 **Doze 下行为可真实验证** |

> ⚠️ **边界声明（重要）**：本机是 stock Android。ColorOS 是 OPPO 在 AOSP 之上做了
> 厂商后台管理定制的系统，其「应用速冻 / 自启动 / 睡眠待机优化」等机制**不存在于 stock Android**。
> 因此凡涉及 **ColorOS 特有行为**的结论，本机**无法得出**，一律记为 `NOT-VERIFIED`
> 并列入 §4 的真机补测清单。**不以模拟器结论冒充 ColorOS 结论。**

---

## 1. 结论一：精确闹钟能否被派发（含 Doze 态）—— ✅ 本机 PASS

**结论：能派发，且在 Deep Doze 下延迟接近零，随后自动续登记下一次。**

### 证据（原始输出见 `evidence-ak2/req25-wake-loop-android16.txt`）

前置：设备已进入 Deep Doze（`mState=IDLE`），持续采集已启用。

设备端 SQLite 台账（真实查询 `mobile_forensic_events`，非日志推断）：

```
event_type         cst                  outcome   delay  trigger
-----------------  -------------------  --------  -----  -----------
alarm-registered   2026-09-26 12:45:40                   app-start
alarm-fulfillment  2026-09-26 12:45:40  executed  5
alarm-registered   2026-09-26 12:55:40                   alarm-fired
```

**读法**：
- 12:45:40 闹钟按时触发 → 执行链结果 `executed`，**`delayMillis=5`（延迟 5 毫秒）**；
- 紧接着写入 `alarm-registered @ 12:55:40`，`trigger=alarm-fired` ——
  **说明触发后确实续登记了下一个周期**（间隔恰 10 分钟，本次测试把节奏临时调为最小档 10 分钟）。

这一步是整套方案的核心验证点：**「叫醒一次就永久静默」是保活最常见的失败形态**，
而台账同时出现 `alarm-fulfillment` 与紧随其后的 `alarm-registered(trigger=alarm-fired)`，
正面证明闭环成立。

### 补充证据：闹钟在进程被杀后仍存活

```
-- 进程是否已死: (进程不存在 = 已死)
-- 闹钟是否仍在系统里:
   tag=*walarm*:com.pim.app.keepalive.action.ALARM
   type=RTC_WAKEUP origWhen=... window=0 exactAllowReason=policy_permission
```

进程被杀不带走闹钟（闹钟由系统 AlarmManager 代持），这正是「系统代持的精确闹钟」
能作为保活手段的前提。

### 叫醒后能否稳定接棒（平台依据 §7 第 4 问）

```
isForeground=true foregroundId=7101 types=0x00000008
foregroundNoti=Notification(channel=pim_location_collection ...)
```

叫醒后 `ForegroundLocationService` 处于前台运行（AC-16.2 通过），
不是「只叫醒一次」—— 采集服务确实被拉起来了。

---

## 2. 结论二：`USE_EXACT_ALARM` 是否自动授予 —— ✅ 本机 PASS（仅 stock Android）

**结论：在本机（stock Android 16）**，`USE_EXACT_ALARM` **随安装即授予**。

### 证据

```
$ adb shell dumpsys package com.pim.app | grep EXACT_ALARM
      android.permission.USE_EXACT_ALARM: granted=true
      android.permission.SCHEDULE_EXACT_ALARM
```

进一步，系统侧登记的闹钟带有：

```
exactAllowReason=policy_permission
```

即系统**以「已授予精确闹钟权限」为由放行了这次登记**，与 `granted=true` 相互印证。

### 但这条**不能外推到 ColorOS**

`USE_EXACT_ALARM` 是否在 ColorOS 上也安装即授予，属于**厂商行为**，本机无法回答 → `NOT-VERIFIED`（见 §4 第 1 项）。
若 ColorOS 未自动授予，应用会在「保活与诊断」里显示「未授权」并给出跳转入口（AC-14.2），
这是设计内的降级路径，但**是否真会发生需要真机确认**。

---

## 3. 结论三：实际延迟分布 —— ⚠️ 仅得单次样本，不构成分布

**结论：本机单次观测延迟 5 毫秒；但样本量不足以称为「延迟分布」，ColorOS 的分布更无从得出。**

- 本机测得：**1 次**触发，延迟 **5 ms**（见 §1）。
- 未做长时间多周期采样（模拟器在 Doze 下持续运行数小时的成本高，且**它的分布不代表 ColorOS**）。
- 因此**不给出延迟分布结论**，也不据此校准 15 分钟阈值。

关于 15 分钟阈值是否合适：工单 §9.1 P2 的「延迟 >15 分钟算被压制」是**需求方已确认的参数**，
本次不动。若真机实测出现「被大幅延迟/吞掉」，按 AC-25.2 带数据回需求方（见下）。

### AC-25.2 触发评估

**当前证据不足以触发 AC-25.2 流程**：
- 本机未观测到被压制或被吞；
- ColorOS 的真实延迟分布未测得（无真机）。

→ 结论：**不在 PR 中自行扩大手段**（工单明确禁止），待需求方真机观察数据回来后再判定。
若真机出现「延迟常超 15 分钟」或「长时间不触发」，再带数据走 §9.2 待定项流程。

---

## 4. 未验证项与需真机补测清单（NOT-VERIFIED）

| # | 待确认项 | 为什么本机测不了 | 需求方真机怎么测 |
|---|---|---|---|
| 1 | **ColorOS 是否自动授予 `USE_EXACT_ALARM`** | 厂商策略，stock Android 无此定制 | 装包后打开「设置 → 保活与诊断」，看「闹钟和提醒权限」是「已授权」还是「未授权」；未授权则点进去手动打开 |
| 2 | **ColorOS 是否按预期派发精确闹钟（尤其冻结/深度睡眠时）** | ColorOS 的「应用速冻 / 睡眠待机优化」在 stock Android 不存在 | 按操作卡四步装好并设置后，隔夜观察：手机端「保活与诊断 → 最近一次叫醒」是否约每 30 分钟更新 |
| 3 | **ColorOS 上的实际延迟分布** | 同上；模拟器分布不可外推 | 连续观察 1–2 天，记录「最近一次叫醒」的时间戳间隔；若出现明显 >15 分钟的间隔，即为被压制证据 |
| 4 | 叫醒后前台服务能否**长期**稳定接棒（数天尺度） | 本机只验证了单次接棒 | 与第 2 项同一次观察即可覆盖 |
| 5 | 撤销分享后最终页语义（阶段二无关，阶段一遗留） | — | 已在阶段一 PR 记录，此处不重复 |

**补测所需材料**：操作卡（`wo-android-keepalive-phase2-operator-card.md`）已覆盖全部操作步骤与预期现象。

---

## 5. 与阶段一基线的对比口径（待真机数据补齐）

阶段一真机基线（需求方提供）：近 7 天 8 起可疑死亡全为 `REASON_OTHER + [UNKNOWN] o-kill(...)`，
存在 22.7h / 21.6h / 12.7h / 10.5h 长时间死期，死期内零同步。

阶段二上线后应对比：

| 指标 | 阶段一基线 | 阶段二预期 | 当前状态 |
|---|---|---|---|
| 单次最长静默 | 53.2 小时 | 应显著下降（叫醒周期 30 分钟） | **待真机数据** |
| ≥22 小时全静默窗口 | 4 段 | 应趋近 0 | **待真机数据** |
| 有同步批次到达的小时占比 | 17.0% | 应上升 | **待真机数据** |
| 闹钟兑现率 | —（无此能力） | 新增指标，见 Web 设备存活页 | **待真机数据** |

> 上表在真机观察期结束后由需求方填写；本 PR 只提供指标能力与读取入口，**不预填预期达成的数字**。

---

## 6. 诚实边界小结

- ✅ **本机可得并已验证**：精确闹钟在 Deep Doze 下的真实派发与续登记、进程被杀后闹钟存活、
  叫醒后服务接棒、`USE_EXACT_ALARM` 安装即授予（stock Android）、未使用 `setAlarmClock`。
- ⚠️ **本机不可得，已记 NOT-VERIFIED**：全部 ColorOS 专项行为（四项，见 §4）。
- 🚫 **未做**：延迟分布结论（样本不足）、更长周期采样、真机对比数据。
- 以上边界同样写入 PR 的 AC 映射表，**未做任何「后续优化」式弱化**。
