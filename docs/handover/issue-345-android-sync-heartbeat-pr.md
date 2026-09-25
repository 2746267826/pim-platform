# fix(android): 周期同步唤醒补写取证心跳 / record a forensic heartbeat on the periodic-sync wake path

**Fixes #345**

修的是工单 `WO-ANDROID-KEEPALIVE-20260923` 的 **REQ-3**（存活心跳台账）在**周期同步**这条唤醒路径上的缺口；阶段一 PR #340（merge `47ad1e97`）已合并的部分不重做。

- 分支：`dsh-linux/android-heartbeat-345`
- base：`origin/master`（`fbf1a86e`）
- 评审 head：`245dd5ee5c7e139abb596a3e2a4f9b98c8debf68`（**代码与测试的最终提交**；其后仅有一个只改本说明文档的 docs 提交，代码/测试一字未动，故所有代码与测试证据对本分支最新 head 同样成立）

---

## 需求与验收映射 / Requirement & Acceptance Mapping

| 编号 | 实现（文件/函数） | 验证方式 | 结果 | 证据 |
|---|---|---|---|---|
| REQ-3（周期同步路径） | `WakeHeartbeatRecorder.record()` + `MobileSyncCoordinator.syncOnOpen()`（写心搏置于乐观锁之前） | 真机/模拟器：杀掉进程后强制既有周期作业，唤醒即落一条心搏 | PASS | 下方「真机实测 1」；`MobileSyncPeriodicHeartbeatTest` |
| AC-3.1 | `WakeHeartbeatRecorder` 读 `BootElapsedClock.now()`；`ForensicLedger.recordHeartbeat` 记时刻 | 模拟器重启一次，比对壁钟与开机时长 | PASS | 下方「真机实测 3」（开机时长 119632s → 101s，与设备 uptime 115s 一致），并正确判为 `reboot` |
| AC-3.2（省电模式部分） | `AndroidHeartbeatSnapshotReader`（省电/Doze/待机桶读数） | 改真机设置后观察后续心搏字段 | PASS | 下方「真机实测 4」（省电 True→False；Doze False→True） |
| AC-3.2（电池优化部分） | 同上（`isIgnoringBatteryOptimizations`） | — | NOT-VERIFIED | 需用户授权/系统设置才能翻转，模拟器无法可靠复现；见「已知限制」 |
| AC-3.3（同秒去重） | 既有 `ForensicLedger.heartbeatKey`（秒级幂等键）+ 同步路径调用同一入口 | 单测注入时钟，两次唤醒取同一秒内不同毫秒 | PASS | `AC-3_3 two sync wakes within the same second still produce one heartbeat`；真机 20 条心搏 20 个不同 key |
| AC-3.3（写失败不阻断） | `WakeHeartbeatRecorder` 全异常吞并转日志；`MobileSyncCoordinator.recordWakeHeartbeat()` 再包一层 | 单测注入必失败的 DAO | PASS | `AC-3_3 a failing ledger write does not break the sync run`（同步仍有序结束在 `server-missing`） |
| AC-29.1（不新增轮询/闹钟） | 本 PR 无任何调度代码 | diff 审查 | PASS | diff 内无 `AlarmManager`/`setExact*`/`WorkManager` 调度/`Handler`/`Timer`/`postDelayed` |

---

## 技术修改 / Technical changes

改的是 `src/client-android`（Android 客户端），4 个文件：

| 文件 | 内容 |
|---|---|
| `forensics/WakeHeartbeatRecorder.kt`（新增） | REQ-3 心搏的**唯一写入口**：字段构造、待机桶 / Doze / 电池优化 / 前台服务读数、「距上次心搏间隔」基线只有一份实现 |
| `mobile/sync/MobileSyncCoordinator.kt` | `syncOnOpen()` 在**乐观锁之前**调用一次写心搏，覆盖全部同步结局 |
| `forensics/StartupForensics.kt` | 改为委托同一入口；启动路径行为不变 |
| `test/.../MobileSyncPeriodicHeartbeatTest.kt`（新增） | 7 条测试，含两条反面、一条并发交错与多组故障对照 |

### 关键设计决定

1. **写心搏放在乐观锁之前**：「唤醒发生了」与「本次同步是否真的跑起来」是两件事。排队等锁而被跳过的那次唤醒同样是真实唤醒，也必须记。若放在锁之后，会重新制造「同步批次有、心搏没有」的口径缺口——正是 #345 的成因（已用故障对照证明：把调用挪到锁后，`a wake skipped by the sync lock is still recorded` 立即失败）。
2. **收敛到单一入口**：避免出现「启动路径记 A 字段、同步路径记 B 字段」两套口径。
3. **零新增调度**：只被既有唤醒路径调用，自身不调度任何东西（AC-29.1）。
4. **失败隔离**：写台账失败只记结构化日志并返回 `false`，绝不改变同步结果（AC-3.3）。

### 范围外 / 未改动（按工单硬约束）

未改动采集频率档位、质量门参数、判定阈值；未新增轮询或闹钟。

---

## 功能变化 / Feature changes

- 手机端状态页「设备存活」区块的**按应有心跳覆盖率**与**按小时覆盖率**不再只反映进程启动次数，而是跟随真实的周期同步活动（缺陷现象：一周 2 条心搏 → 1.2% 按小时覆盖率、9087 分钟静默）。
- 医生诊断包 `forensics.jsonl` 与设备私库 `mobile_forensic_events` 里的 `heartbeat` 行数会与周期同步节奏对齐。
- **没有**新增任何用户可见的界面、通知、设置项或闹钟。

---

## 如何体验 / How to try it

前提：一台 Android 手机或模拟器，装上本 PR 的 debug 包，并用一台你能访问到的 PIM 服务端登录（未配置服务端时同步会走「服务器未配置」分支，但唤醒心搏照记——这一点正好也是本次修复要保证的）。

1. **看到修复前的现象（可选对比）**：在旧包上打开 App，等一天，状态页「设备存活」区块的覆盖率极低、静默时间以天计。
2. **装上本 PR 的包并打开一次**：状态页「设备存活」区块出现本次启动的心搏。
3. **让 App 退到后台并把进程杀掉**（模拟系统回收）：
   ```bash
   adb shell input keyevent KEYCODE_HOME
   adb shell am kill com.pim.app
   adb shell pidof com.pim.app          # 无输出 = 进程已死
   ```
4. **手动触发一次既有的周期同步作业**（不新增闹钟，跑的就是既有的那个 15 分钟周期作业）：
   ```bash
   JOB=$(adb shell dumpsys jobscheduler | grep -oE "JOB #u0a[0-9]+/[0-9]+: [0-9a-f]+ com.pim.app/androidx.work" | head -1 | grep -oE "/[0-9]+:" | tr -d '/:')
   adb shell cmd jobscheduler run -f com.pim.app $JOB
   ```
5. **预期看到**：进程被重新拉起，并且**新增一条心搏**；反复做第 3-4 步，每做一次就多一条。这正是修复前缺失的那条写入。
6. **等 15 分钟**：不做任何操作，周期同步自己跑一次，心搏条数随之增加——心搏节奏与同步节奏一致（不再是「一周 2 条」）。
7. **改系统设置观察字段**：开「省电模式」后再触发一次唤醒，最新心搏的 `powerSaveMode` 变为 `true`；关掉后回 `false`。同理 `adb shell dumpsys deviceidle force-idle` 进入 Doze 后 `dozeMode` 变 `true`。
8. **看真机序列**：
   ```bash
   adb shell run-as com.pim.app cat databases/pim.db > /tmp/pim.db   # 需要 debug 包
   sqlite3 /tmp/pim.db "SELECT occurred_at_utc, payload_json FROM mobile_forensic_events WHERE event_type='heartbeat' ORDER BY occurred_at_utc;"
   ```

---

## 测试 / Tests

### 单元测试（本地实跑，head `245dd5ee5c7e139abb596a3e2a4f9b98c8debf68`）

```bash
cd src/client-android
export ANDROID_HOME=/home/coder/Android/sdk
./gradlew :app:testDebugUnitTest --tests "com.pim.app.mobile.sync.MobileSyncPeriodicHeartbeatTest" --no-daemon
./gradlew :app:testDebugUnitTest :core:testDebugUnitTest --no-daemon
./gradlew :app:assembleDebug --no-daemon
```

- `MobileSyncPeriodicHeartbeatTest`：7/7 通过
- `:app:testDebugUnitTest :core:testDebugUnitTest`：**1426 条，0 失败，0 跳过**
- `:app:assembleDebug`：成功（debug APK 18.7 MB），证明 Hilt 依赖图仍然有效

### 故障对照（证明测试不是空过）

| 故障注入 | 结果 |
|---|---|
| 把 `wakeHeartbeatRecorder.record()` 改成空操作 | **4 条**测试立即失败 |
| 把 `recordWakeHeartbeat()` 从乐观锁之前挪到之后 | `a wake skipped by the sync lock is still recorded` 失败 |
| 把秒级幂等键改成毫秒级 | 两条 AC-3.3 去重测试失败 |
| 注入必失败的 DAO | 插入尝试计数 ≥1（证明协调器确实尝试写过），0 行落库，同步仍有序结束 |
| 注释掉"写入成功后更新基线" | `REQ-3 the second wake ... carries a real time-since-last-heartbeat` 失败 |
| 去掉并发串行锁（`baselineLock`） | `concurrent wakes never produce a wrong or negative time-since-last-heartbeat` 失败（`JSONException: Value null at sinceLastHeartbeatMs`） |

### 独立 review（DSH → Codex，两轮）

| 轮次 | 结论 | 处理 |
|---|---|---|
| 第 1 轮 | **Deny** | 1 条 Important + 2 条 Minor |
| 第 2 轮（复核修复结果） | **ALLOW** | 三条全部确认解决，无 Critical / Important 遗留 |

- **Important（已修）**：心搏入口改成单例后，「读基线 → 造负载 → 写台账 → 更新基线」不是原子的；启动取证与周期同步并发进入时，会读到同一旧基线或乱序写回，记下错误/为负的「距上次心搏间隔」。→ 用 `Mutex` 把该序列整体串行；抽出 `HeartbeatSnapshotSource` 让测试能卡住读快照这一步，构造**真实并发交错**并断言间隔仍正确（去掉锁即失败）。
- **Minor（已修）**：REQ-3 字段测试原来只查子串（字段恒 null 也能过）→ 改为解析 JSON 断言取值与类型；写失败测试原来只断言 `phase=="server-missing"`（不调用写心搏也照样过）→ 改为统计插入尝试次数并断言 ≥1。
- review 另确认：写心搏置于乐观锁之前的判断正确；`StartupForensics` 重构保持了顺序/时刻/回退/`heartbeatRecorded` 语义；无新增调度、未动采集档位/质量门/判定阈值；`syncOnOpen()` 同时覆盖既有立即同步（用户打开）调用方。


### 连接 Android 测试门禁（本地模拟器，CI 无模拟器）

```bash
./gradlew :app:connectedDebugAndroidTest --no-daemon
```
`emulator-5554`（API 36）上 **67/67 通过，0 跳过，0 失败**。

### 真机/模拟器实测（本 PR 的核心证据）

> 环境：`emulator-5554`，Android API 36（`sdk_gphone64_x86_64`），本 PR 的 debug APK。以下均为**真实设备行为**，不是单元测试推断。

**实测 1 — 杀掉进程后由既有周期作业唤醒，#345 的原始缺陷场景**

```
[BEFORE] heartbeats=5      pid before job: []      ← 进程已死
         adb shell cmd jobscheduler run -f com.pim.app 11 → Running job [FORCED]
[AFTER ] heartbeats=6      pid after job: 12714    ← 进程被拉起
         14:01:14 boot=119026989  sinceLast=None
```
修复前该路径**不写任何心搏**。

**实测 2 — 进程存活时强制既有周期作业，心搏间隔与同步节奏一致**

```
[baseline] heartbeats=2
  13:58:50 boot=118882683
  13:58:53 boot=118885376  sinceLast=2693
[后] heartbeats=3
  13:59:20 boot=118912969  sinceLast=27593   ← 该次周期同步写下的心搏
```

**实测 3 — AC-3.1 重启一次，开机时长与壁钟一致**

```
14:11:22 boot=119634430        ← 重启前
--- adb reboot（设备 uptime 归零）---
14:13:34 boot=100800           ← 重启后：开机时长回落到 101s
14:13:44 boot=110819  sinceLast=10019
```
- 设备 `cat /proc/uptime` = `115.48s`，与心搏里的 111s 一致（采样相差数秒）。
- 同一时刻台账正确落了一条 `force-stop kind=reboot evidence=boot-elapsed-decreased`，说明两端对「重启」的判定一致。

**实测 4 — AC-3.2 改真机设置，后续心搏字段随之变化**

```
（拔掉电池模拟器、开省电）
14:10:29 powerSave=True   doze=False  ← 省电开启后
14:10:58 powerSave=False  doze=False  ← 关掉省电后回落
（adb shell dumpsys deviceidle force-idle）
14:11:22 powerSave=False  doze=True   ← 进入 Doze 后
```

**实测 5 — AC-3.3 同秒去重（真机数据核对）**

```
heartbeats=20  distinct_keys=20  duplicate client_item_key rows: none
```

**实测 6 — 不施任何人工操作，等自然周期同步自己跑（心搏节奏与同步节奏一致）**

进程于 `14:15:43` 启动后，未做任何操作，既有的 15 分钟周期作业在 `14:29:20` 自行运行：

```
14:15:43 boot=229170    sinceLast=3847      ← 进程启动
14:29:20 boot=1046948   sinceLast=817778    ← 自然周期同步唤醒（≈13.6 分钟，与 15 分钟周期一致）
```

即：**无需人工干预，心搏会随周期同步自然增长**；同一进程内 `sinceLastHeartbeatMs` 也如实反映了两跳之间的真实间隔（817778ms ≈ 13.6 分钟）。

**实测 7 — 同步运行与心搏一一对应（当日全部日志回放）**

把当日日志里每一次走到「同步」的唤醒，与台账心搏按 10 秒窗口配对：

```
sync runs (skipped, server unconfigured) = 12
heartbeats                              = 23
matched 12/12 sync runs to a heartbeat
```

**12/12 次同步唤醒都恰好对应一条心搏**，这正是 #345 缺失的那条写入。

### 已知限制 / NOT-VERIFIED

- **连续 24 小时含重启的真机观察本身**仍未做（开发方手上没有可连续跑 24 小时的真机）。本 PR 给的是：模拟器上重启一次前后的心搏序列 + 每次唤醒都落一条的可复现证据；**AC-3.1 的「连续 24 小时」仍需需求方在真机确认**。
- **`ignoringBatteryOptimizations` 的真机状态切换未验证**：模拟器上省电模式与 Doze 两个字段已实测随设置变化（实测 4），但「被排除电池优化」是一项需要用户授权/系统设置的 UI 操作，模拟器上无法可靠翻转，因此该字段仍是 `NOT-VERIFIED`，需在真机确认。
- **`sinceLastHeartbeatMs` 在进程重启后的第一条心搏为 `null`**：该基线目前只存在内存里（`WakeHeartbeatRecorder.lastHeartbeatBootElapsed`），这是阶段一就有的行为，本 PR 未改变。诊断包导出（`forensics.jsonl`）在**同一进程内**为 2693 / 27593 / 35991 等正常间隔；每次进程重启后的第一条为 `null`。**此为本 PR 范围外，仅记录**（改它要动持久化契约，不属于「只补周期同步这条唤醒路径」）。

---

## 影响面 / Risk

- 每次唤醒多一次台账写入 + 一次系统读数（待机桶 / 电池优化 / Doze / 省电 / 前台服务），均为**既有唤醒路径上的同步操作**，未引入新的唤醒源，因此不增加待机电量开销的**次数**。
- 写心搏全程异常隔离，失败只留结构化日志，不改变同步结果（AC-3.3 已用注入失败的测试覆盖）。
- 无 API / 数据库 schema / 服务端改动。
