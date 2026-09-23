# WO-ANDROID-KEEPALIVE-20260923 · 阶段一（死因取证）交付说明

> 本文件是阶段一 PR 的描述，用于让 PR 正文保持可复核。**证据绑定 head `4d678158ee88258b6c876d3372975e93ba60c5a4`**。
> 阶段二（REQ-14 ~ REQ-25 精准闹钟）**未实施**，符合工单"两个 PR"的交付节奏。

---

## 技术修改 / Technical changes

### 服务端（Pim.Core / Pim.Infrastructure / Pim.Module.Mobile / Pim.Module.Mcp）

| 位置 | 内容 |
|---|---|
| `src/Pim.Core/Liveness/DeviceLivenessModels.cs` | 取证事件类型、存活证据来源、静默判定线（30/60 分钟）、覆盖率两口径定义、`IDeviceLivenessInspectionProvider` |
| `src/Pim.Core/Liveness/DeviceLivenessCalculator.cs` | **静默与覆盖率的唯一口径实现**：纯函数，无数据库/时钟依赖 |
| `src/modules/Pim.Module.Mobile/Entities/MobileForensicEntities.cs` | 新表 `mobile_forensic_events`（幂等键 `user+device+clientItemKey` 唯一）与 `mobile_dropped_reason_daily`（天然键 `user+device+localDate+reason` 唯一） |
| EF 迁移 `20260923103301_AddMobileForensics` | 只**新增**两张表与其索引，不改动任何既有表/列 |
| `Services/MobileForensicIngestService.cs` | `POST /api/v1/mobile/forensics/events`：独立通道，幂等（唯一键跳过）；并发重复提交触发唯一约束时翻译成 `skipped` 而不是 500；丢弃原因按天统计走覆盖写 |
| `Services/MobileLivenessService.cs` | 存活概览/摘要/事件分页/丢弃统计取数；实现 `IDeviceLivenessInspectionProvider` |
| `Services/MobileLivenessCauseClassifier.cs` | Android 退出原因常量 → 统一死因键与简体中文标签；未知死因一律带推断依据 |
| `MobileModule.cs` | 4 个新端点 + DI 注册 |
| `Services/DeviceManagementService.cs` | 设备合并/删除纳入新两张表（AC-6.3）：合并去重后改写、同键统计相加、删除时一并清理 |
| `src/Pim.Infrastructure/Operations/DataReliabilityQualityInspector.cs` | 体检输出新增「设备存活」数据项：**独立区块，不参与红/黄/绿统计** |
| `src/Pim.Core/Invariants/DataReliabilityInspectionReport.cs` | 报告新增 `DeviceLiveness` 字段（附加型变更） |
| MCP `McpToolTable` / `McpToolCatalog` / `Contract/mcp-tools.json` | 新增只读工具 `get_mobile_liveness_summary`，**直接复用同一 REST 端点**（因此 MCP 与 REST 结果必然一致） |

### Web（client-web）

新增「手机记录」下的**设备存活**子页（`?view=liveness`，可链接/可刷新）：结论卡、双口径覆盖率（含定义与分子分母）、最长静默与静默时间轴（≥30 分钟黄、≥1 小时红、<30 分钟不标记）、死因分布（含未知 + 推断依据）、手机/平板/未分类分块、事件展开与原始 JSON。

### Android（client-android）

- Room v4：新增 `mobile_forensic_events` 表 + `MIGRATION_3_4`（只增表，不动既有表）
- 新包 `com.pim.app.forensics`：进程退出原因台账、哨兵强停判定、存活心跳、取证上下文、上传协调器、30 天保留策略、本地存活摘要
- 状态页顶部新增存活区块（结论 / 最近心搏 / 双口径覆盖率 / 最近死因 + 推断依据）
- 新增「丢弃原因」页（按原因统计 + 最近 20 条明细）
- 诊断导出新增 `dropped-locations.jsonl`（丢弃明细）与 `forensics.jsonl`（死因台账）
- 取证上报挂在**既有同步周期**内，不新增任何轮询或闹钟

### 关键设计决定

1. **静默判定线只有一条（30 分钟）**：夜间与白天走同一段代码，不存在夜间分支（AC-13.2）。
2. **「按应有心跳」覆盖率的分母取既有 15 分钟周期同步节奏**，不新引入参数；两个覆盖率都暴露分子/分母，可人工复算（AC-13.1）。
3. **存活证据 = 心搏（主）∪ 同步批次到达时刻（辅）**；使用事件与「缺口回补窗口」都不是存活证据（AC-7.6 / AC-13.3）。
4. **强停判定的优先级**：重启 > 哨兵被权限变更清空 > 用户请求停止退出记录 > 其它退出记录 > 哨兵消失。依据与推断文案同源，避免"依据说 A、文案说 B"。
5. **体检不判档**：`DeviceLivenessInspectionItem` 的数据契约里根本没有 status/grade 字段（AC-10.3 的结构性保证）。

---

## 功能变化 / Feature changes

- Web「手机记录 → 设备存活」：一眼看到设备活没活、覆盖率、最长静默与死因分布，可展开逐条事件与原始 JSON。
- 安卓状态页顶部新增「设备存活」区块：结论、最近心搏（相对时间）、双口径覆盖率、最近死因；数据陈旧时标注"数据为 N 小时前"，无数据时明确显示"无数据/未上报"。
- 安卓新增「丢弃原因」页：被丢弃定位点的原因统计与最近 20 条明细。
- 安卓诊断导出包新增丢弃明细与死因台账两个文件。
- 数据可信度体检新增「设备存活」数据项（只展示数据，本版本不判红/黄/绿）。
- 新增 MCP 只读工具 `get_mobile_liveness_summary`。
- **没有**新增用户可见的系统设置引导、闹钟、通知红点（属阶段二）。

---

## 如何体验 / How to try it

### Web 端

1. 打开 PIM Web，左侧导航进入 **手机记录**。
2. 页面顶部出现两个子页签：`使用记录` / `设备存活`——点 **设备存活**（地址栏会变成 `.../mobile-records?view=liveness`，可直接收藏/分享）。
3. 默认展示**最近 7 天**。首屏自上而下依次是：
   - 每台设备的一句话结论（例如"存活有缺口：最长静默 135 分钟，区间内共 1 段 ≥30 分钟静默。"）；
   - **小时覆盖**：`87.5%（7/8 小时）`，下面一行是这条口径的定义；
   - **预期心跳覆盖**：`71.4%（20/28 次）`，同样带定义；
   - 最长静默徽标：≥30 分钟显示**黄色**，≥1 小时显示**红色**，<30 分钟显示"最长静默：无"；
   - 静默时间线：每段静默的起止时刻与分钟数（同样的黄/红标色）；
   - 原因分布：例如"内存不足被系统回收：2"；未识别的死因显示"系统未给出原因"并**紧跟在下面给出推断依据**。
4. 手机 / 平板 / 未分类机型是**三块独立区域**，各自计数与统计，互不相加。
5. 点任意设备卡片的 **展开事件**，可看到逐条存活事件（时刻、类型、原因、上下文如"屏幕灭；电量 60%"）；点某行的 **查看原始 JSON** 会展开该事件的原始负载。
6. 顶部可切换 今天 / 7天 / 30天，或点 **刷新** 重新取数。
7. 设备从未上报时，该块显示"无数据/未上报"，覆盖率显示 `—`（不是 100%，也不是 0%）。

### 安卓端

1. 打开 PIM 安卓应用 → 底部导航 **状态**。
2. 页面最上方就是新的 **设备存活** 区块：
   - 第一行是一句话结论；
   - `最近心搏：距今 3 分钟`；
   - `覆盖率 · 按小时` 与 `覆盖率 · 按应有心跳`，各带定义与分子分母；
   - `最近死因：内存不足被系统回收`，下面一行是"推断依据：…"；若系统没有留下任何退出记录，这里显示"未知"并给出"这只说明系统没有留下记录，不能据此判定设备没有异常"。
   - 数据超过 1 小时没更新时，额外显示"数据为 N 小时前"。
3. 点区块里的 **查看丢弃原因** → 进入「丢弃原因」页：顶部是累计条数与保留窗口，中间是"按原因统计"，下面是"最近 20 条明细"（时刻 / 原因 / 准确度 / provider / 策略档）。点 **返回状态页** 回到状态页。
4. 在状态页点 **导出诊断包**，解压后可看到新增的 `dropped-locations.jsonl`（每条含时刻、原因、准确度、provider、策略档）与 `forensics.jsonl`（进程退出与存活心跳台账）。

### 想观察"死因取证真的在记"（真机 / 模拟器）

```bash
ANDROID_HOME=/home/coder/Android/sdk \
APK_PATH=$PWD/src/client-android/app/build/outputs/apk/debug/app-debug.apk \
bash scripts/qa/android-forensics-emulator.sh
```

脚本会自动做：冷启动 → `am kill` → `am force-stop` → 再次冷启动，并在每一步之后打印应用私有目录里的取证台账。预期能看到进程退出记录、`force-stop`/`reboot` 判定记录与存活心跳；默认不把数据库副本留在本机（`KEEP_LEDGER_COPY=1` 可保留现场）。

---

## 测试 / Tests

所有命令均在本机执行，结果取自 head `4d678158ee88258b6c876d3372975e93ba60c5a4`。

### 环境

- .NET 8（`dotnet --version` → `8.0.425`），隔离库 `pim_test` 起在 `127.0.0.1:55544`（本机一次性 PostgreSQL 16 容器，`pg_dump` 快照之外**不使用任何生产库**）。
- Android SDK `/home/coder/Android/sdk`，模拟器 `pim361`（Android 16 / API 36，`emulator-5554`）。
- Node/npm（`src/client-web`）。

### 1) 后端全量测试

```bash
export PIM_TEST_CONN="Host=127.0.0.1;Port=55544;Database=pim_test;Username=pim;Password=pimtest"
dotnet test Pim.sln --configuration Release \
  --filter "FullyQualifiedName!~MobileTimeline_SpecificHeavyDayLosesDataUnderLegacyCap&FullyQualifiedName!~MobileTimeline_RealDayExceedsLegacy500Cap&FullyQualifiedName!~RealDb_Issue234_PostAwDate_ActivityClassification_UsesNativeEvents&FullyQualifiedName!~EnsureClassificationsAsync_ConcurrentMaterializationDoesNotThrow&FullyQualifiedName!~DeviceMergeRealDbTests"
```

结果：`Passed! - Failed: 0, Passed: 5325, Skipped: 10, Total: 5335`。

排除说明：

- 前 4 条是 CI `build-api.yml` 也排除的"需要生产镜像库"用例；
- `DeviceMergeRealDbTests` 的 `MergeAsync_OnProductionShapedData_ResolvesCrossDeviceDuplicateKeys` 在**基线 `23cf6d27` 上同样失败**（我在独立 worktree 上复跑确认），前提是"镜像库里同一个用户至少有两台设备且存在跨设备重复业务键"，本机隔离库不满足该前提，属**既有问题**。该类的另一条用例 `MergeAsync_WithProductionRetryStrategy_MovesEverythingAndKeepsAppNamesResolvable` 在 CI 上跑并在本 PR 内修好（见下）。

### 2) 本次新增/相关的后端测试

```bash
dotnet test tests/Pim.UnitTests/Pim.UnitTests.csproj \
  --filter "FullyQualifiedName~Liveness|FullyQualifiedName~Forensic|FullyQualifiedName~McpLiveness"
```

结果：全部通过。覆盖：

- `DeviceLivenessCalculatorTests`：覆盖率两口径的可复算性、夜间与白天同一判定线、<30 分钟不标记、≥1 小时标红、尾部静默、未知死因带推断、区间外证据不计入。
- `MobileLivenessServiceTests`：幂等上报、未知事件类型显式拒绝、畸形负载不拖垮整批、丢弃原因覆盖写、机型三块分组、无证据空态、**使用事件不计入存活证据**、**回补窗口不计入存活证据**、未知原因带推断、强停/重启/哨兵被清空可区分、30 天前事件仍可查询、`unavailableFields` 渲染成"不可用字段"。
- `DeviceForensicSemanticsTests`（**真实 PostgreSQL + 完整迁移链**）：合并迁移取证事件并去重、同键丢弃统计相加、删除设备时一并清理、删除预览不受影响。
- `DataReliabilityDeviceLivenessTests`：体检含该数据项、无数据显示"无数据"、**不改变红/黄/绿结论**、数据项契约里没有档位字段。
- `McpLivenessToolTests`：只读目录登记、路由到同一 REST 端点、写方法被只读策略拒绝、契约 schema 与工具表一致。

### 3) 隔离库上的真实 HTTP 回放（AC-5.1 / AC-5.2 / AC-7.x / AC-9.2 / AC-10.1 / AC-11.4）

先按 CI 的方式把 schema 建好（迁移链 + 启动一次 API 触发运行时初始化器），再起一个指向隔离库的 API：

```bash
export PATH="$PATH:$HOME/.dotnet/tools"
dotnet ef database update \
  --project src/Pim.Infrastructure/Pim.Infrastructure.csproj \
  --startup-project src/Pim.Api/Pim.Api.csproj \
  --connection "$PIM_TEST_CONN"

openssl genrsa -out /tmp/ka_jwt.pem 2048
export ConnectionStrings__DefaultConnection="$PIM_TEST_CONN"
export Jwt__PrivateKeyPath=/tmp/ka_jwt.pem
export DataProtection__KeysPath=/tmp/ka_dp_keys
export PIM_OPS_KEY=ka-temporary-ops-key
export PIM_OPS_RO_CONNECTION="$PIM_TEST_CONN"
export ASPNETCORE_ENVIRONMENT=Test
export ASPNETCORE_URLS=http://127.0.0.1:5999
dotnet run --project src/Pim.Api --no-launch-profile --no-restore &

KA_TEST_PASSWORD='<本次回放临时账号口令>' \
  python3 scripts/qa/mobile-liveness-replay.py
```

脚本输出（节选）：

```
registered ka_e2e_421045
ingest#1 accepted=23
ingest#2 (idempotent) skipped=23
overview: phones=1 tablets=2 unclassified=0
phone block: {"deviceId": "android-ka-phone", "deviceKind": "phone", "hasData": true,
  "conclusion": "存活有缺口：最长静默 135 分钟，区间内共 1 段 ≥30 分钟静默。",
  "coverageByHour": 0.875, "coverageByExpectedHeartbeat": 0.7143,
  "observedHours": 7, "totalHours": 8, "observedHeartbeats": 20, "expectedHeartbeats": 28,
  "longestSilenceSeverity": "critical", "hasSilenceOverOneHour": true, ...}
tablet blocks show 无数据/未上报: OK
device summary matches overview block: OK
events page total=23: OK
dropped reasons: [{"localDate": "2026-09-23", "reason": "horizontal-accuracy-too-low", "count": 12},
                  {"localDate": "2026-09-23", "reason": "missing-horizontal-accuracy", "count": 3}]
no-data device 200 / unknown device 404: OK
inspection status=unknown red=0 yellow=0 green=2 unknown=11 deviceLiveness=3
E2E OK
```

证明与不能证明：

- **证明**：真实 HTTP、真实 PostgreSQL、真实迁移链下，取证通道幂等、覆盖率/静默/死因计算结果、REST 摘要与概览块一致、丢弃统计数值与设备端一致、无数据设备与不存在设备的语义可分、体检数据项存在且不含档位。
- **不能证明**：真机上的 Android 行为（见"需要需求方在真机上验证"）。

### 4) Android 单元测试（JVM / Robolectric）

```bash
cd src/client-android
ANDROID_HOME=/home/coder/Android/sdk ./gradlew :app:testDebugUnitTest --no-daemon --rerun-tasks
```

结果：`BUILD SUCCESSFUL`，共 **1362** 个测试、0 失败。

本次新增的取证测试：

| 测试类 | 覆盖 |
|---|---|
| `ForceStopDetectorTest` | AC-2.1 / AC-2.2 / AC-2.3 全部正向与反面组合，含"哨兵仍在时的权限变更不判为哨兵被清空"与"重启不得计为强停" |
| `ForensicLedgerTest` | 同一秒多次唤醒只一条心搏、退出记录按（秒+原因）去重、30 天清理、待传/已同步迁移 |
| `ExitReasonRecorderTest` | AC-1.1 / AC-1.2 / AC-1.3 / AC-1.4 / AC-4.2（不可用字段留空并标注） |
| `ForensicPayloadsTest` | REQ-3/REQ-4 负载字段、AC-4.2 不可用即留空、AC-27.1 负载不含经纬度/令牌/账号、退出原因常量映射 |
| `ForensicUploadCoordinatorTest` | AC-5.1 / AC-5.2 / AC-5.3 / AC-9.2（记录型假 ApiService 捕获真实请求对象） |
| `ForensicRetentionTest` | AC-6.2 / AC-9.3：两条通道都按 30 天清理，窗口内不设条数上限（2000 条验证） |
| `LivenessSummaryCalculatorTest` | AC-13.1 / AC-13.2 / AC-7.4 / AC-8.2 |
| `PimDatabaseMigrationsTest` | 3→4 迁移：只增表、幂等键唯一索引生效、既有数据保留 |

### 5) Android 连接测试门禁（`AGENTS.md` 要求的本地门禁）

```bash
ANDROID_HOME=/home/coder/Android/sdk ./gradlew :app:connectedDebugAndroidTest --no-daemon
```

结果：`BUILD SUCCESSFUL`（`emulator-5554`，Android 16 / API 36）。新增 `StatusLivenessScreenTest` 在真实 Compose 运行时里验证状态页存活区块的四项事实、陈旧标注、无数据空态（显示 `—` 而非 0%/100%），以及丢弃原因页的统计/明细/空态/返回。

### 6) 模拟器死因取证实证（REQ-1 / REQ-2 的真机行为）

```bash
ANDROID_HOME=/home/coder/Android/sdk \
APK_PATH=$PWD/src/client-android/app/build/outputs/apk/debug/app-debug.apk \
bash scripts/qa/android-forensics-emulator.sh
```

实测输出（节选，`emulator-5554`，API 36）：

```
== 场景 3：am force-stop（用户强停，哨兵应被清空） ==
    process-exit   1790170811096 exit-1790170811-REASON_USER_REQUESTED {"reason":"REASON_USER_REQUESTED","importance":100,"rssKb":238160,"screenOn":true,"batteryPercent":100}
    force-stop     1790170817526 forcestop-1790170817-force-stop  {"kind":"force-stop","evidence":"user-requested-exit-record","inference":"系统把这次停机记为「用户请求停止」（REASON_USER_REQUESTED / REASON_USER_STOPPED），与手动强行停止一致。"}
```

**这次实测改掉了实现**：原实现把"系统留下了退出记录"一律当成"已解释死因"，于是把 `am force-stop` 漏报成"无强停"。实测显示 API 36 上 `am force-stop` 会留下 `REASON_USER_REQUESTED`，因此该原因现在是**强停的直接证据**，且依据/文案与检测路径同源。

### 7) Web

```bash
npm --prefix src/client-web run test:mobile-liveness
npm --prefix src/client-web run build
```

结果：`PASS: mobileLivenessApiPath / mobileLivenessModel / mobileLivenessTypes / mobileLivenessUi`；`✓ built in 17.06s`（仅既有的 chunk size 提示）。

### 8) 独立 review

- **第 1 轮（Codex，独立会话）**：判定 `BLOCK`。提出的 Important 项：丢弃明细没有 30 天清理（AC-9.3）、统计扫描有 200,000 行条数截断（AC-9.2/REQ-9）、无退出记录时页面显示"无死亡记录"（AC-1.3）、并发重复提交可能 500（AC-5.2）、服务端按固定 UTC+8 推算本地日、启动取证异常被 `runCatching` 静默吞掉（AC-28.1）、脚本含明文口令。**全部已修复**（提交 `ac8abc72` / `74a3a260` / `7dbb3b66`）。
- **第 2 轮（Codex）**：判定 `BLOCK`，但理由是"安卓取证超出阶段一范围"——该理由**不成立**（工单 §3 的 REQ-1 ~ REQ-13 正是安卓取证本身）。其余 4 项复判见第 3 轮。
- **第 3 轮（Codex）**：确认范围判断错误，确认第 2 轮的"范围越界"是误判；提出**一条真实的 Important 缺陷**：`ForceStopDetector` 在哨兵仍在时也会判为"哨兵被清空（权限变更）"，与 REQ-2 / AC-2.3 的"因权限变更导致哨兵失效"不符。**已修复**（提交 `a4690ac5`）。
- **第 4 轮（Codex）**：对该修复的最终确认，结论见下方"独立 review 最终结论"。
- **CI（GitHub Actions，head `4d678158`）**：`build-api` / `build-web` / `build-android` / `build-docker` / `CI Gate` 全绿。
  首次 CI（head `4ff7cdfd`）抓到一处真实缺口：`DeviceMergeRealDbTests` 的 `DeviceScopedTables` 没把新增的取证两张表克隆进临时 schema，导致合并路径报 `42P01 relation "mobile_forensic_events" does not exist`。
  已在 `4d678158` 补上，CI 随之转绿——**这正是"本地全绿不等于 CI 全绿"的实例**。

---

## 需求与验收映射 / Requirement & Acceptance Mapping

证据均绑定 head `4d678158ee88258b6c876d3372975e93ba60c5a4`。结果状态只用 `PASS` / `FAIL` / `BLOCKED` / `NOT-VERIFIED` / `APPROVED-EXCEPTION`。

### REQ-1 ~ REQ-13（阶段一）

| AC | 实现（文件/函数） | 验证方式 | 结果 | 证据 |
|---|---|---|---|---|
| AC-1.1 | `forensics/ProcessExitReasonReader.kt` `ExitReasonRecorder.recordNewExits`；`ForensicLedger.recordProcessExit`；状态页存活区块 | 单元：`ExitReasonRecorderTest`、`ForensicLedgerTest`；模拟器：`scripts/qa/android-forensics-emulator.sh` | PASS | 模拟器实测写入 `exit-...-REASON_USER_REQUESTED`，含 `reason/importance/rssKb`；重复读取不重复入库由 `AC-1_1 refreshing does not duplicate` 覆盖；刷新后仍存在＝Room 持久化（`ForensicLedgerTest` 读回） |
| AC-1.2 | `ProcessExitReasonReader.kt`（`REASON_USER_REQUESTED` 映射）+ `ForceStopDetector` | 单元：`ExitReasonRecorderTest.AC-1_2`、`ForceStopDetectorTest`；模拟器清单 | PASS | 模拟器同时记录 `REASON_USER_REQUESTED`（用户强停）与 `REASON_PACKAGE_UPDATED`/`REASON_LOW_MEMORY`（系统原因），两者在台账与页面上可区分 |
| AC-1.3 | `ForensicLivenessRepository.snapshot`（`NO_EXIT_RECORD_HINT`）+ `LivenessSection` | 单元 + 连接测试 `StatusLivenessScreenTest.withoutLocalDataTheBlockSaysNoDataAndNeverShowsFullCoverage` | PASS | 无退出记录时页面显示"未知"+ 推断依据"…不能据此判定设备没有异常"；系统不支持/读取失败时显示具体原因 |
| AC-1.4 | `PimApp.onCreate`（独立协程 + 日志）；`AndroidExitReasonReader` 全异常收敛 | 单元：`AC-1_4 a failing reader never throws`；模拟器冷启动 | PASS | 读取失败不影响启动与采集（异常被收敛成 `failureReason`，不进入采集路径）；模拟器冷启动正常进入状态页并写入心跳 |
| AC-2.1 | `ForceStopDetector.detect` + `WorkManagerSentinelProbe` | 单元：`ForceStopDetectorTest`；模拟器证据脚本 | PASS | `am force-stop` 后重开，台账出现 `force-stop` 记录（`evidence=user-requested-exit-record`） |
| AC-2.2 | 同左（`boot-elapsed-decreased` 分支） | 单元：`AC-2_2 boot elapsed going backwards is a reboot` | NOT-VERIFIED | 逻辑与单元测试通过；**未在真机/模拟器上实际重启设备验证**（需需求方确认） |
| AC-2.3 | `ForceStopDetector.detect`（须 `!sentinelPresent && permissionChangeAfterArmed`） | 单元：两条新测试（哨兵仍在 → None；哨兵消失 → 哨兵被清空） | NOT-VERIFIED | 逻辑与正反两条单元测试通过；**未在真机上做权限变更验证**（需需求方确认） |
| AC-3.1 | `StartupForensics.recordHeartbeat` + `ForensicPayloads.heartbeat` | 单元：`ForensicPayloadsTest`、`ForensicLedgerTest` | NOT-VERIFIED | 时刻与开机时长字段写入并有测试；**连续 24 小时含一次重启的真机序列未做**（需需求方确认） |
| AC-3.2 | `AndroidHeartbeatSnapshotReader`（待机桶/电池优化/Doze/省电/前台服务） | 单元：`standbyBucketLabel` 取值测试 | NOT-VERIFIED | 每次唤醒实时读取，测试覆盖标签映射；**改动真机设置后取值随之变化未验证**（需需求方确认） |
| AC-3.3 | `ForensicLedger.recordHeartbeat`（秒级幂等键）+ 全异常吞并记日志 | 单元：`AC-3_3 several wakes within the same second produce exactly one heartbeat`；`ForensicRetentionTest` | PASS | 同一秒三次唤醒只落 1 条；写台账失败走 `logs.error` 且不抛出，不阻断采集/同步 |
| AC-4.1 | `AndroidForensicContextSource.read` + `ForensicPayloads.putContext` | 单元：`AC-4_1 context fields follow the real device state`；模拟器台账 | PASS | 模拟器台账含 `screenOn:true`/`batteryPercent:100`；单元测试覆盖亮灭/充放电变化后的字段变化 |
| AC-4.2 | `ForensicContext` + `unavailableFields`；服务端 `MobileLivenessService.ReadContext` | 单元：`AC-4_2 unreadable fields stay empty and are marked unavailable`、`ForensicPayloadsTest`；服务端 `Ingest_RendersUnavailableContextFieldsInsteadOfOmittingThem` | PASS | 读不到即 `null` + 数组标注；服务端渲染成"不可用字段：解锁状态、电量百分比"；负载不含任何应用使用内容（只有包名+应用名） |
| AC-5.1 | `ForensicUploadCoordinator.uploadPending` + `MobileSyncCoordinator.uploadForensics`（循环排空，最多 10 批/次） | 单元：`ForensicUploadCoordinatorTest.AC-5_1`；隔离库回放 | PASS | 回放脚本 23 条事件一次同步全部到达；`ingest#1 accepted=23` |
| AC-5.2 | 服务端 `(user, device, clientItemKey)` 唯一索引 + 并发唯一约束冲突翻译成 `skipped` | 单元：`Ingest_RepeatedBatch_IsIdempotent`；回放脚本重复提交；`ForensicUploadCoordinatorTest.AC-5_2` | PASS | `ingest#2 (idempotent) accepted=0 skipped=23`；重复提交后服务端条数不变 |
| AC-5.3 | `MobileSyncCoordinator.uploadForensics` 与既有同步解耦；失败只记日志并保留 pending | 单元：`AC-5_3 a failed upload keeps events locally and never throws` | PASS | 上报失败返回 0、本地仍为 pending 且带错误、既有同步不受影响；断网期间定位点继续累积由既有 `LocationUploadCoordinator` 路径保证（本次未改动） |
| AC-6.1 | 服务端 `mobile_forensic_events` 不做时间清理 | 单元：`Events_AreRetainedBeyondThirtyDaysOnTheServer` | PASS | 45 天前的事件仍可查询返回 |
| AC-6.2 | `ForensicRetention.purgeExpired` / `ForensicLedger.purgeExpired` | 单元：`ForensicRetentionTest`（含 2000 条不设上限验证）、`ForensicLedgerTest.AC-6_2` | PASS | 31 天前条目（不论同步状态）被移除；29 天前未上传条目保留，清理后条数与待上传计数一致；窗口内 2000 条一条不少 |
| AC-6.3 | `DeviceManagementService.MergeAsync` / `DeleteAttemptAsync` / `MergeDroppedReasonDailyAsync` | **真实 PostgreSQL**：`DeviceForensicSemanticsTests`（4 条） | PASS | 合并后无孤儿、同键去重、统计同键相加；删除后取证事件与统计一并清除；删除预览不受影响 |
| AC-7.1 | `MobileLivenessService.GetOverviewAsync` + `DeviceLivenessPanel.tsx` | 隔离库回放（默认 7 天、四项结论齐全） | PASS | 回放输出含 `conclusion`、两个覆盖率、`longestSilence*`、`causes`；Web 单测断言四项都在首屏 markup 里 |
| AC-7.2 | `MobileLivenessService.GetEventsAsync` + `DeviceEvents`/`EventRow` | 单元：`Events_ExposeTypeReasonContextAndRawJson`；Web：`mobileLivenessUi` | PASS | 事件含类型/原因/上下文与 `payloadJson`，页面可展开原始 JSON |
| AC-7.3 | `ResolveDeviceKind` + `GetOverviewAsync` 三块；Web `groupDeviceBlocks` | 单元：`Overview_SeparatesPhoneTabletAndUnclassified`；回放 | PASS | 回放 `phones=1 tablets=2 unclassified=0`；`deviceKind` 缺失时用 `smallestScreenWidthDp>=600` 回退，仍不可知则单独成块不混入手机 |
| AC-7.4 | `DeviceLivenessCalculator.ClassifySilence` + `SilenceSeverityClass` | 单元：计算器测试 + Web 模型测试 + 连接测试 | PASS | 29.9→不标记、30→黄、60→红；页面 `data-severity="critical"` 红色徽标 |
| AC-7.5 | 计算器无证据分支 + `hasData` 判定 | 单元：`Overview_DeviceWithoutEvidence_NeverReportsFullCoverage`；连接测试 | PASS | `hasData=false`、覆盖率为 `null`、结论"无数据/未上报"；页面显示 `—` 而非 0%/100% |
| AC-7.6 | 取数只查心搏事件与同步批次到达；**不查** `mobile_usage_events` | 单元：`DeviceLiveness_UsageEventsAloneDoNotCountAsLivenessEvidence` | PASS | 只写使用事件的设备 `hasData=false`、覆盖率为 `null` |
| AC-8.1 | `ForensicLivenessRepository.snapshot` + `LivenessSection` | 连接测试 `StatusLivenessScreenTest`（四项）+ 模拟器 | PASS | 真实 Compose 渲染下四项 testTag 均显示且文案正确；模拟器冷启动后状态页出现该区块 |
| AC-8.2 | `staleNote`（≥1 小时线）+ 无数据分支 + AC-1.3 的"未知" | 连接测试：`staleDataIsLabelledWithItsAge`、`withoutLocalDataTheBlockSaysNoData...` | PASS | 显示"数据为 2 小时前"；无数据显示"无数据/未上报"与 `—`，不出现"无异常" |
| AC-9.1 | `DiagnosticExportRepository.buildDroppedLocationDetails` + `DroppedReasonScreen` | 单元：`DiagnosticExportRepositoryTest`（导出条目）、`DroppedReasonScreen` 连接测试 | NOT-VERIFIED | 导出包含 `dropped-locations.jsonl`（时刻/原因/准确度/provider/策略档）且页面统计口径同源；**"导出包与 App 内页面同一天同一原因计数一致"未做端到端逐项核对** |
| AC-9.2 | `ForensicUploadCoordinator.buildDroppedReasonSummaries` + `MobileForensicIngestService.UpsertDroppedReasonSummariesAsync` | 单元：两侧各自的统计测试；隔离库回放 | PASS | 回放：设备端上报 12/3，服务端返回 12/3；同键重复上报覆盖不累加 |
| AC-9.3 | `ForensicRetention.purgeExpired`（两条通道） | 单元：`ForensicRetentionTest`（含清理后导出不失败） | PASS | 31 天前明细被移除、29 天内保留；清理后导出取数正常且不异常增大 |
| AC-10.1 | `DataReliabilityQualityInspector.LoadDeviceLivenessAsync` + `IDeviceLivenessInspectionProvider` | 单元：`Report_ContainsDeviceLivenessSectionWithDataFields`；隔离库回放 | PASS | 回放：`inspection ... deviceLiveness=3`，可读到结论/两口径/最长静默/死因/最近事件时间 |
| AC-10.2 | 计算器无数据分支 + 体检项 | 单元：`Report_DeviceWithoutData_ShowsNoDataConclusion`、`Report_NoProvider_KeepsSectionPresentWithVisibleNotice` | PASS | 无数据设备结论为"无数据/未上报"且覆盖率为 `null`；取数源缺席有可见 notice |
| AC-10.3 | 数据项契约无 status/grade 字段；不参与红黄绿统计 | 单元：`Report_DeviceLivenessDoesNotChangeRedYellowGreenVerdict`、`DeviceLivenessItem_HasNoHealthGradeField` | PASS | 有/无该数据项时 `Status`/`Red`/`Yellow`/`Green`/`Unknown` 完全一致；反射断言无档位字段；全仓库未自拟体检阈值 |
| AC-11.1 | `MobileLivenessService.GetDeviceLivenessAsync` + `MobileDeviceLivenessDto` | 隔离库回放；单元 `DeviceLiveness_ReturnsRESTCompatibleSummaryFields` | PASS | 返回结论、两口径、最长静默、死因汇总、最近事件时间、`hasSilenceOverOneHour` |
| AC-11.2 | MCP `get_mobile_liveness_summary` 直接路由同一 REST 端点 | 单元：`McpLivenessToolTests`；隔离库回放（REST 摘要在两种取数路径下逐字段一致） | PASS | `LivenessTool_DispatchesToTheSameRESTEndpointAsTheSummaryApi`；回放断言 `device summary matches overview block: OK` |
| AC-11.3 | MCP 只读策略（写方法被拒） | 单元：`LivenessTool_IsAllowedByTheReadOnlyEndpointPolicy` | NOT-VERIFIED | `POST`/`DELETE` 到该路径被 `McpReadEndpointPolicy` 拒绝；但**未做"通过 MCP 调用写入类工具后核对数据未变"的端到端验证**，因此如实记为 `NOT-VERIFIED` |
| AC-11.4 | `GetDeviceLivenessAsync` 返回 `null` → 404；有设备无数据 → 200 + 空态 | 单元：`DeviceLiveness_UnknownDevice_ReturnsNullSoEndpointCanAnswer404`、`Overview_DeviceWithoutEvidence_...`；回放 | PASS | 回放：`no-data device 200 / unknown device 404: OK`；空态不返回 0% |
| AC-12.1 | 本次唯一顺带修复：设备合并/删除未覆盖新表（提交 `ac8abc72`） | 见下方"顺带修复清单" | PASS | 问题、证据、修复点、影响范围已单列 |
| AC-12.2 | `git diff origin/master...HEAD -- src/.../settings src/.../location/policy src/.../location/quality` 为空 | 命令 | PASS | 采集频率档位、质量门参数、判定阈值均未改动 |
| AC-12.3 | 未发现策略设计问题；本文档与 PR 正文如实记录 | 代码审查 + 独立 review | PASS | 无擅自改动的策略设计 |
| AC-13.1 | `DeviceLivenessRules.CoverageBy*Definition` + `DeviceLivenessPanel` | 单元：计算器可复算性测试；Web：`mobileLivenessUi` 断言定义与分子分母出现在 markup | PASS | 页面显示 `87.5%（7/8 小时）` / `71.4%（20/28 次）` 与两段定义，可由同一批数据复算 |
| AC-13.2 | 计算器只有一条判定线，无夜间分支 | 单元：`ClassifySilence_IsIndependentOfTimeOfDay`、`AC-13_2 the same silence length is classified the same at night and during the day` | PASS | 同一时长在夜间与白天得到相同标色 |
| AC-13.3 | `BuildDeviceBlockAsync` 只用 `MobileSyncBatchEntity.CreatedAt` | 单元：`DeviceLiveness_SyncBatchArrivalCountsButBackfillWindowDoesNot` | PASS | 只覆盖"回补窗口"那段时间的查询无存活证据；使用事件同样不计入 |

### REQ-26 ~ REQ-31（跨阶段）

| AC | 实现 | 验证方式 | 结果 | 证据 |
|---|---|---|---|---|
| AC-26.1 | 新增文案全部简体中文，术语与既有界面一致（采集/同步/存活/静默/覆盖率/丢弃原因） | 代码审查 + 连接测试 + Web 单测 | PASS | 状态页/丢弃原因页/Web 子页均为简体中文；测试断言中文字串 |
| AC-26.2 | 无英文占位串 | 代码审查 | PASS | 新增文案无 `TODO`/`Lorem`/英文占位；`MobileLivenessCauseClassifier` 的死因标签全部中文 |
| AC-27.1 | `ForensicPayloads`、服务端只存设备上报的负载 | 单元：`AC-27_1 no payload contains coordinates tokens or credentials`（逐 payload 断言） | PASS | 三种负载都不含 `latitude/longitude/token/password/account/email` 等键 |
| AC-27.2 | PR/注释/脚本不含明文凭据与生产数据 | 代码审查 + 脚本改造 | PASS | 回放脚本口令改从 `KA_TEST_PASSWORD` 读取（源码不内置）；模拟器脚本默认删除可能含坐标的数据库副本；本 PR 不含任何生产数据 |
| AC-27.3 | 抽查上报请求体与诊断导出包 | 单元：`ForensicPayloadsTest`；回放脚本请求体 | PASS | 请求体只含 `clientItemKey/eventType/occurredAtUtc/payloadJson`（负载脱敏）；诊断导出走既有 `DiagnosticRedactor` 并新增两个已脱敏 JSONL |
| AC-28.1 | `ForensicLedger` 结构化日志；`PimApp` 启动取证失败记日志；体检取数失败写 notice | 单元 + 代码审查 | PASS | 每条失败路径都有日志/notice；`runCatching` 静默吞异常已移除 |
| AC-28.2 | 断网 → 本地留存 + 日志；缺权限 → 既有状态页 issue；存储不可写 → 既有导出错误出口 | 单元：`AC-5_3`；既有测试 | NOT-VERIFIED | 取证链路本身有可见出口；**"拒绝权限 / 存储不可写"两种情况下应用自身是否有明确提示未逐项实测**（既有实现属上次交付范围，本次未改动） |
| AC-29.1 | 取巧保活与轮询均未引入 | `git diff` 审查 | PASS | diff 中无新增定时器/轮询/闹钟；取证挂在应用启动与既有同步周期内 |
| AC-29.2 | 无以体积/时长上限作为验收断言 | 代码审查 | PASS | 本地取证不设条数上限（2000 条测试）；分页大小只是单次查询行数，不是条数上限 |
| AC-30.1 | 既有定位采集/同步/时间线/设备管理/体检逐项工作 | 后端全量 `dotnet test`（5325 通过）；Android 全量单测（1362 通过）+ 连接测试；Web build | PASS | 三端既有测试全绿；设备管理新增用例覆盖合并/删除 |
| AC-30.2 | 旧客户端未知字段不导致服务端报错 | 单元：`Ingest_MalformedPayload_DoesNotFailTheBatch`；DTO 默认 JSON 绑定 | PASS | 未知事件类型显式计入 rejected（不静默丢弃），畸形负载不拖垮整批；新表未改动既有端点契约 |
| AC-31.1 | 后端 `dotnet test` + Web 构建 + Android 连接门禁 | 见"测试"章节 1/5/7 | PASS | 5325 通过；`✓ built`；`connectedDebugAndroidTest BUILD SUCCESSFUL` |
| AC-31.2 | 本 PR 含双语"如何体验 / How to try it"与"测试 / Tests" | 本 PR 正文 | PASS | 见上 |
| AC-31.3 | 本 PR 含逐条 AC 映射表，证据绑定 head SHA | 本 PR 正文 | PASS | 本表，head `4d678158` |
| AC-31.4 | 结果状态取值合法、无未覆盖 AC | 本 PR 正文 | PASS | 仅使用五种状态；阶段一 + 跨阶段 54 条 AC 逐条列出 |

---

## 顺带修复清单 / Incidental fixes (REQ-12 / AC-12.1)

本 PR 只有一处顺带修复，且属"实现与需求不一致"，不涉及策略设计：

| # | 问题 | 证据 | 修复点 | 影响范围 |
|---|---|---|---|---|
| 1 | 设备**合并/删除**只处理既有 8 张表；阶段一新增的 `mobile_forensic_events` 与 `mobile_dropped_reason_daily` 会被落下，源设备删除后留下指向已删除 `device_id` 的孤儿取证记录（违反 REQ-6 / AC-6.3 反面） | `DeviceManagementService.MergeAsync` / `DeleteAttemptAsync` 的既有表清单；独立 review 第 1 轮 | `DeviceManagementService.cs`：合并时按唯一键去重后改写取证事件、同键统计**相加**、其余改写；删除时一并清理。全部用集合操作，不加载被跟踪实体 | 仅设备合并/删除两条管理路径；不涉及采集、同步与判定策略 |

未顺带修改、仅记录的问题（AC-12.3）：

- `DeviceMergeRealDbTests.MergeAsync_OnProductionShapedData_ResolvesCrossDeviceDuplicateKeys` 在基线 `23cf6d27` 上即失败（前提是镜像库中存在跨设备重复业务键），属既有问题，本次未处理。

---

## 需要需求方在真机上验证 / Needs verification on the real device

真机在需求方手里，以下条目开发方只能给出可复现的模拟/注入证据，**请在真机上确认**（观察期内系统设置保持不变以维持对比基线）：

1. **AC-2.2 设备重启**：重启手机后重开 PIM，状态页存活区块的"最近死因"是否出现"设备重启"，且**没有**同时出现"疑似强停"。
2. **AC-2.3 权限变更**：在系统设置里撤销/恢复一项权限（例如后台定位），重开应用，确认台账记为"哨兵被清空（权限变更）"而**不是**"疑似强停"。
3. **AC-2.1 手动强停**：设置 → 应用 → PIM → 强行停止，再打开应用，确认出现"疑似强停"。ColorOS 上如果强停**不**产生退出记录，请把状态页存活区块截图给我们。
4. **AC-3.1 / AC-3.2**：连续 24 小时（其间重启一次）后，心搏序列的时刻与开机时长变化是否与真机一致；改动电池优化 / 省电模式后，后续心搏对应字段是否随之变化。
5. **AC-4.1**：点亮/熄灭屏幕、切换前台应用、插拔充电器后，后续事件的对应字段是否随之变化。
6. **AC-7.x / AC-8.1**：在 Web「手机记录 → 设备存活」与安卓状态页存活区块上，对照真实使用情况判断结论是否可信（尤其"最长静默"是否与您感知的断档时段一致）。
7. **AC-9.1**：导出诊断包，核对 `dropped-locations.jsonl` 中同一天同一原因的条数是否与 App 内「丢弃原因」页一致。
8. **AC-9.2**：在 Web 设备存活页/摘要里核对丢弃统计数值是否与手机端一致。
9. **AC-30.1**：确认既有定位采集、同步、时间线与设备管理在装机后逐项照常工作。

---

## CI 状态 / CI status

head `4d678158ee88258b6c876d3372975e93ba60c5a4`：

| Check | 结果 |
|---|---|
| CI Gate | success |
| build-api / build | success |
| build-web / build | success |
| build-android / build | success |
| build-docker / build | success |
| build-windows / build-shell-windows / build-shell-android / build-mcp / 浏览器插件 | skipped（路径过滤，本次无变化） |

> 说明：`build-android` 在 CI 上会跑 `:app:testDebugUnitTest`（安卓 JVM 单测），**不**跑连接测试门禁——连接测试按 `AGENTS.md` 是本地门禁（CI 不提供模拟器），已在上面第 5 节本地执行。

## 未完成 / 阻塞 / 未验证（显式列出）

**未完成（阶段二，本次明确不做）**：REQ-14 ~ REQ-25（精准闹钟保活、叫醒执行链、被压制降频、兑现台账、叫醒通知、暂停语义、健康红点、保活总开关、ColorOS 引导页、操作卡、ColorOS 实测结论）。等需求方观察期结束后另行开工。

**未验证（`NOT-VERIFIED`，需要真机或端到端复核）**：AC-2.2、AC-2.3、AC-3.1、AC-3.2、AC-9.1、AC-11.3、AC-28.2。逐条原因见映射表。

**阻塞**：无。

**范围外发现（只记录，不在本 PR 处理）**：
1. `DeviceMergeRealDbTests` 的真库用例在基线即失败（见上）。
2. `AC-9.2` 的本地日依赖设备时区，服务端把日期窗口放宽 ±1 天来兼容；若未来要在服务端按本地日做日粒度聚合，需要设备上报时区偏移——已记录，不在本 PR 扩大改动。

---

## 独立 review 最终结论 / Independent review verdict

**第 4 轮（Codex 独立会话）结论：`APPROVE-FOR-PR`。**

- **AC-2.3 修复已确认**。权限变更结论只在满足全部前置条件时返回（有存活基线、未检测到重启、`!sentinelPresent && permissionChangeAfterArmed`）；哨兵仍在时不会报告"哨兵被清空"。新测试在回退修复后会失败，具备真实区分能力。
- **未再发现 Critical / Important 缺陷**。
- 该轮同时确认：本分支不含阶段二功能（无 `AlarmManager` / 精准闹钟 / 保活总开关 / 健康红点 / ColorOS 引导页），未改动采集频率档位、质量门参数与判定阈值，未改动 `mobile_sync_batches` 语义。
- 该轮确认的 **必须在 PR 中如实标记 `NOT-VERIFIED`** 的条目为：**AC-2.2、AC-2.3、AC-3.1、AC-3.2、AC-9.1、AC-11.3、AC-28.2** —— 与本文件"未完成 / 阻塞 / 未验证"一节完全一致。

四轮 review 的净效果：第 1 轮发现 6 项 Important（丢弃明细无时间清理、统计扫描条数截断、无退出记录时显示"无死亡记录"、并发重复提交可能 500、本地日时区错位、启动异常被静默吞掉）→ 全部修复；第 2 轮因范围误判而 BLOCK（第 3 轮已纠正）；第 3 轮发现 1 项 Important（哨兵仍在时误判"哨兵被清空"）→ 已修复；第 4 轮通过。
