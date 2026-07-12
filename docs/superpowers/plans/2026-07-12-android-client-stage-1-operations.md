# Android 客户端阶段 1：运行与可诊断

**最终目标：** 用户完成首次设置后，采集和同步在后台稳定运行；状态页如实反映全线事实；诊断和连接检查帮助定位问题；系统杀死、权限变更、网络波动后恢复入口保证不静默停工。

## 架构概要

```
采集 → Room pending 表 → SyncWorker（唯一，WorkManager 管控）
   ├─ confirmed → 删除
   ├─ rejected → 原行保留同步状态+错误字段
   └─ 未确认 → 继续 pending，下次重试
日志 → 本地文件（不进入业务表）
状态 ← Room + WorkInfo + 网络 + 权限 + 采集意图 + probe
恢复入口 ← Application.onCreate / BOOT_COMPLETED / MY_PACKAGE_REPLACED
```

一个认证仓库（EncryptedSharedPreferences + Mutex），一个 SyncWorker，一个 ensureRunningState。日志不进业务表，永久拒绝使用业务行现有字段。

## 前置依赖

- 实现从 `codex/android-operational-foundation` 暂停点继续；该分支已经提交 Task 1 的 schema/test 基础与 Task 2 的 `VersionEndpoints.cs`，不是从 `origin/master` 重做。
- 服务端 `src/Pim.Api/Endpoints/VersionEndpoints.cs`（`GET /api/version` 返回版本和能力）
- Room v3 schema 已上线（`src/client-android/app/schemas/com.pim.app.data.AppDatabase/3.json`）
- `src/client-android/core/src/main/java/com/pim/core/settings/PimServerEndpoints.kt` 服务端点解析
- Gradle 构建通过，`dotnet test Pim.sln` 通过

---

## 1. 保留与精简 Task 0–3

**目的：** 在现有 `codex/android-operational-foundation` 脏工作树上，只保留有意向的源码/测试/配置变更，排除生成文件后创建源文件检查点；给出 keep/simplify/delete 清单。

**文件：**
- `src/client-android/app/build.gradle.kts`
- `src/client-android/app/schemas/com.pim.app.data.AppDatabase/3.json`
- `src/Pim.Api/Endpoints/VersionEndpoints.cs`
- `src/client-android/core/src/main/java/com/pim/core/auth/AuthSessionStore.kt`
- `src/client-android/core/src/main/java/com/pim/core/auth/TokenManager.kt`
- `src/client-android/core/src/main/java/com/pim/core/auth/AuthRefreshCoordinator.kt`
- `src/client-android/core/src/main/java/com/pim/core/network/AuthInterceptor.kt`
- `src/client-android/core/src/main/java/com/pim/core/settings/PimServerEndpoints.kt`
- `src/client-android/core/src/main/java/com/pim/core/settings/ServerSettingsStore.kt`
- `src/client-android/app/src/main/java/com/pim/app/status/ConnectionProbeModels.kt`
- `src/client-android/app/src/main/java/com/pim/app/status/ConnectionProbeService.kt`
- `src/client-android/app/src/main/java/com/pim/app/status/ConnectionProbeStore.kt`
- 上述行为对应的 `core/src/test/` 与 `app/src/test/` 测试

**复用：** Task 1（测试环境 + Room v3 schema）完整保留。Task 2（逐项确认）完整保留。Task 3 endpoint resolver 保留。Task 3 token 保留行为但简化实现。Task 3 probe 保留失败分类，改为简单函数加时间戳结果。Task 0 覆盖率矩阵仅做历史参考。

**完成方式：**
1. 检查脏工作树，生成变更文件列表。排除 `bin/` `obj/` `build/` `dist/` `publish/` `.dotnet-*` `npm cache` `wwwroot` 生成文件。
2. 逐项标记 keep / simplify / delete。keep 的文件直接纳入分支。simplify 的文件（token refresh → 单 auth repository + 单 Mutex + fail-closed；probe → 3–4 步函数 + 时间戳结果）按新设计重写后纳入。
3. delete 的文件（generation/CAS、tombstone、独立 ProbeRunner、双回滚证明）先确认无调用方再删除。
4. 在恢复或删除任何生成物前创建只包含源码、测试和配置的本地检查点，并复核其文件清单。后续可在阶段提交前整理历史，但不能依赖未保存的工作树状态保护 Task 3 成果。

**自动验证：**
- `dotnet test Pim.sln`
- `src/client-android/` 下 `./gradlew :core:testDebugUnitTest :app:testDebugUnitTest`

**人工验收：**
- 确认检查点只包含意向变更，无生成文件残留。
- 确认 keep 清单与设计文档 Task 0–3 复用决策一致。

---

## 2. 关闭数据与日志边界

**目的：** 日志永远不进业务上传队列；拒绝事实用业务行现有字段保留；pending 总数不包含 `MobileLogEntity`；Room v3 保持无破坏兼容。

**文件：**
- `src/client-android/app/src/main/java/com/pim/app/data/MobileEntities.kt`
- `src/client-android/app/src/main/java/com/pim/app/data/MobileDataDao.kt`
- `src/client-android/app/src/main/java/com/pim/app/data/AppDatabase.kt`
- `src/client-android/app/src/main/java/com/pim/app/data/PimDatabaseMigrations.kt`
- `src/client-android/app/src/main/java/com/pim/app/mobile/sync/MobileAcknowledgementPlanner.kt`
- `src/client-android/app/src/main/java/com/pim/app/mobile/sync/LocationUploadCoordinator.kt`
- `src/client-android/app/src/main/java/com/pim/app/mobile/logs/StructuredLogRepository.kt`
- 以上各文件对应的测试

**复用：** `MobileSyncBatchEntity` / `mobile_sync_batches` 作为精简同步历史表。现有业务行 `syncStatus` 和 `lastError` 字段用于永久拒绝。Room v3 迁移代码不动。

**完成方式：**
1. `StructuredLogRepository` 改为纯本地文件日志，不再新增 `MobileLogEntity` 行；为兼容已发布 schema，暂不删除旧日志表或升级数据库版本。
2. `MobileDataDao` 增加按 `syncStatus` 过滤 confirmed/rejected/unconfirmed 的查询。pending 计数只统计未确认且未永久拒绝的行。
3. `MobileAcknowledgementPlanner` 对服务端 confirmed 回复做删除，对 rejected 回复设置 `syncStatus=REJECTED` + `lastError`，不再重试。
4. 确认 `MobileLogEntity` 不存在于任何 pending 查询中。如果目前存在，改写为只查真正的业务队列表。
5. 保留现有 v2→v3 迁移测试，并增加从当前已发布 v3 schema 打开数据库的回归测试；本阶段 schema 不变，不新增迁移。

**自动验证：**
- Room 迁移测试：旧 → 新 schema 后业务行、同步历史行不变。
- 单元测试：ack confirmed 删除行，ack rejected 保留行并设状态和原因。
- pending 总数查询不含日志类型行。

**人工验收：**
- 新日志只写应用专属文件；兼容保留的旧日志表不再增长，也不出现在业务待传数量中。
- 模拟器上触发 sync，确认 rejected 项持续显示错误原因且不再上传。

---

## 3. 唯一同步执行路径

**目的：** 手动、前台、周期和网络恢复后全部进入同一个 `MobileSyncWorker` 与 `MobileSyncCoordinator`；WorkManager 只保存调度事实，现有进程内 Mutex 保证即时任务和周期任务不会同时执行实际上传；401 最多 refresh 一次，失败后保留队列并 fail-closed。

**文件：**
- `src/client-android/app/src/main/java/com/pim/app/mobile/sync/MobileSyncWorker.kt`
- `src/client-android/app/src/main/java/com/pim/app/mobile/sync/MobileSyncCoordinator.kt`
- `src/client-android/app/src/main/java/com/pim/app/mobile/sync/LocationUploadCoordinator.kt`
- `src/client-android/app/src/main/java/com/pim/app/daemon/UploadWorker.kt`
- `src/client-android/app/src/main/java/com/pim/app/mobile/sync/LocationSyncWorker.kt`
- `src/client-android/app/src/main/java/com/pim/app/sync/EndpointUploadWorker.kt`
- `src/client-android/app/src/main/java/com/pim/app/PimApp.kt`
- `src/client-android/app/src/main/java/com/pim/app/di/PimWorkerFactory.kt`
- 以上各文件对应的测试

**复用：** `MobileSyncCoordinator` 现有 `syncMutex`、状态流与上传顺序；Task 2 的逐项确认；`LocationUploadCoordinator` 的定位逐项处理。不会再增加 broker、lease 或第二套协调器。

**完成方式：**
1. 仅保留一个 `MobileSyncWorker` 类，其 `doWork()` 只调用 `MobileSyncCoordinator`；定位上传继续由 coordinator 内部委托 `LocationUploadCoordinator`，不再另起 worker。
2. WorkManager 注册两个调度事实：`pim_mobile_sync_periodic` 是 15 分钟周期兜底，`pim_mobile_sync_now` 是手动或前台即时请求。用它们替换当前 60 分钟 `MobileSyncWorker` 周期项和旧 `UploadWorker` 15 分钟项；两者使用同一个 Worker 类，各自 unique work 防同类重复，`MobileSyncCoordinator.syncMutex` 防交叉并行。
3. 周期任务使用网络约束和指数退避；“仅非流量网络”偏好改变时更新该周期任务的约束。断网后不监听自建广播，WorkManager 在约束恢复时继续调度。
4. 手动同步也进入 `pim_mobile_sync_now`。用户确认“本次允许流量”时只在该 WorkRequest 的 input data 中携带一次性标志，不写回长期网络偏好。
5. 确认 `daemon/UploadWorker.kt`、`mobile/sync/LocationSyncWorker.kt`、`sync/EndpointUploadWorker.kt` 的数据种类已被新入口覆盖；先迁移调用方和测试，再删除或改为单纯转发，最终不得保留独立上传逻辑。
6. 401 处理：coordinator 调用唯一认证仓库；refresh 成功则重试一次，失败则结束本轮并记录原因，不清除 pending 数据。网络错误、429、超时和 5xx 保留未确认行，交给 WorkManager 退避或下次手动触发。

**自动验证：**
- Unique enqueue 测试：同一类触发重复调用时仅有一个即时任务和一个周期任务；两类同时触发时 coordinator 的实际上传最大并发数为 1。
- 确认后删除：mock server 返回 200 + confirmed body，确认 DAO delete 被调用。
- 拒绝后保留：mock server 返回 200 + rejected body，确认 DAO update 状态但不 delete。
- 401 测试：第一次 401 → 调用 refresh → refresh 成功 → 重试成功；refresh 失败 → sync 失败不重试。
- 429/5xx 测试：assert worker retry 或 workInfo 为 ENQUEUED。

**人工验收：**
- 模拟器上手动同步，观察 WorkManager 日志无重复 sync 并行。
- 关掉服务器，触发同步，确认 pending 数据保留，状态页显示失败。
- 使用流量的手动同步改回非流量偏好后，下次自动同步不被影响。

---

## 4. 设置、采集与权限

**目的：** 完整设置页（服务器/登录/三档预设/网络/持续采集/权限入口/日志/还原）、精确粒度后台采集（精度/海拔/低频/运动/距离恢复）、通知栏控制、六项权限入口和阻塞提示。

**文件：**
- `src/client-android/app/src/main/java/com/pim/app/settings/TrackingSettingsStore.kt`
- `.../ui/settings/SettingsViewModel.kt`
- `.../ui/settings/SettingsScreen.kt`
- `.../permissions/PermissionStatusRepository.kt`
- `.../ui/permissions/PermissionCenterScreen.kt`
- `.../location/quality/`（全部）
- `.../location/motion/`（全部）
- `.../location/policy/`（全部）
- `.../location/service/`（全部）
- `.../notifications/`（全部）

**复用：** 现有质量过滤、海拔等待、运动观察、策略引擎、前台服务、常驻通知、通知 action。现有 `ServerSettingsStore.kt` 读服务器地址和登录 token。

**完成方式：**
1. `SettingsViewModel` 完成服务器地址、连接测试、登录和登出。地址变更先通过解析检查，再使旧服务器 token 失效并要求重新登录；失败不得把旧 token 发往新地址。
2. 默认持续采集关闭，用户明确开启后只持久化意图。三档预设使用一个集中参数表覆盖普通间隔、日程低频间隔、运动观察间隔、恢复距离、精度阈值和海拔等待时间；高级面板对每个值做明确上下限校验。
3. 把同一设置对象接到现有 `LocationQualityGate`、`AltitudeWaitCoordinator`、`MotionSignalRepository`、`LocationPolicyEngine` 和 foreground service；Stage 1 用合成日程/运动输入完成精度过滤、海拔等待、低频、运动、距离恢复和通知控制，Stage 3 只接入真实日程缓存并补齐三类运动的最终映射。
4. 网络偏好切换写 `TrackingSettingsStore`；手动同步的“本次允许流量”不持久化。详细日志开关和保留天数直接驱动 Task 6 的文件日志，详细模式显示自动关闭时间。
5. `PermissionStatusRepository` 每次页面恢复、应用回到前台或权限操作返回时重新读取六项当前状态：通知、精确定位、后台定位、使用情况访问、活动识别、电池优化豁免。屏幕点击跳转系统对应设置页，不把权限事实长期持久化。
6. 通知栏显示采集状态，action 为暂停/恢复采集、立即同步、打开状态页；每个 action 使用 Task 3/5 的唯一入口。
7. `SettingsScreen` 的“恢复默认”只重置采集、网络和日志参数，不清除服务器地址或登录。系统设置返回后重新检查权限并调用 `ensureRunningState()`；权限仍不足时保留采集意图并显示具体阻塞。

**自动验证：**
- 设置表项写入/读取正确。默认值符合设计。
- 预设切换后采集参数正确应用。
- 恢复默认后服务器地址和 token 保留，其他参数还原。
- 权限状态读取正确分类（granted / denied / not granted）。

**人工验收：**
- 模拟器首次安装：输入服务器地址 → 连接测试通过 → 登录成功。
- 手动开启持续采集后，通知栏出现采集状态。
- 关闭定位权限 → 状态页显示具体阻塞 → 点击跳转系统设置 → 恢复后采集重启。
- 恢复默认后服务器和登录不丢失。
- 三档预设切换后日志可见采集间隔变化。

---

## 5. 事实状态与动作

**目的：** 一个仓库聚合 Room / WorkInfo / 网络 / 采集意图 / 权限 / probe / 日志状态，生成总体结论（正常/需注意/异常）和可操作问题列表。按钮执行对应动作后即时反馈。

**文件：**
- `src/client-android/app/src/main/java/com/pim/app/status/StatusCenterRepository.kt`
- `.../status/StatusIssue.kt`
- `.../status/StatusActions.kt`
- `.../ui/status/StatusCenterViewModel.kt`
- `.../ui/status/StatusCenterScreen.kt`
- `.../mobile/summary/MobileOverviewRepository.kt`

**复用：** `MobileOverviewRepository` 读取 Room 业务行和同步历史。WorkManager WorkInfo 查询。ConnectivityManager 实时状态。PermissionStatusRepository（Task 4）。`StructuredLogRepository` 的日志级别和行数。Probe 结果缓存（Task 1 简化版）。

**完成方式：**
1. `StatusCenterRepository` 组合可观察的 Room 查询、WorkInfo、ConnectivityManager、PermissionStatusRepository、采集意图和运行态、最近连接检查及日志摘要；页面可见和动作完成时主动刷新不可观察的数据源。
2. 根据收集的事实生成一个 `StatusSummary`：overall（正常/需注意/异常）、pending/uploading/confirmed/rejected、上次成功和下一次尝试，以及 `List<StatusIssue>`。每个 issue 含标题、具体事实、严重级别和真正可执行的 action。
3. `StatusActions` 执行 syncNow 时：调用 WorkManager enqueueUniqueWork → 立即观察 WorkInfo 变化 → 显示 accepted / running / completed / failed。执行后刷新 `StatusCenterRepository` 缓存。
4. `StatusCenterScreen` 显示总体结论、传输阶段、每个 issue 和对应 action。`去设置` 必须打开相应系统或应用设置，`查看状态` 必须导航到状态详情，`立即同步` 必须显示 accepted/waiting/running/result 并即时刷新事实，不能只展开卡片。
5. 不另建“状态缓存表”或事实规划器；只保存设计要求的最近连接检查和精简同步历史，其他状态都从当前数据源读取。

**自动验证：**
- 单元测试 mock 各数据源，验证 issue 生成逻辑：全部正常→正常；一个权限缺失→需注意；sync 连续失败→异常。
- Action 测试：mock WorkManager，syncNow 调用 unique work；mock ConnectivityManager，connectionCheck 返回正确结果。

**人工验收：**
- 模拟器上首次配置后，状态页显示"正常"或"需注意"（权限提示）。
- 关闭网络后状态页显示"异常"和具体原因。
- 手动同步后状态页立即显示 pending→uploading→confirmed 变化。
- 每个 issue 的 action 按钮跳转到正确页面。

---

## 6. 诊断、连接检查与恢复

**目的：** 日志本地保存、按天滚动、verbose 24h 后自动关闭；ZIP 导出排除凭据、支持原始坐标确认、预检空间、失败删除。连接检查 4 步、结果缓存。`ensureRunningState` 作为唯一恢复入口由三处调用。

**文件：**
- `src/client-android/app/src/main/java/com/pim/app/mobile/logs/StructuredLogRepository.kt`（已有，复用收口）
- `.../mobile/diagnostics/DiagnosticExportRepository.kt`（新建）
- `.../mobile/diagnostics/DiagnosticRedactor.kt`（新建）
- `.../recovery/RunningStateRestorer.kt`（新建）
- `.../recovery/StartupRecoveryReceiver.kt`（新建）
- `src/client-android/app/src/main/java/com/pim/app/PimApp.kt`
- `src/client-android/app/src/main/AndroidManifest.xml`（更新 receiver 和权限声明）

**复用：** `StructuredLogRepository` 现有日志写入和滚动删除。日志级别和标签沿用。前台 service 已有 notification action。WorkManager 注册沿用。

**完成方式：**
1. `StructuredLogRepository`：日志写入 `context.filesDir/logs/`，按日期分区。默认保留 7 天，可配置。debug/verbose 日志只在"详细日志"开启且未超 24h 时写入。超时后自动降级。
2. `DiagnosticExportRepository` 只从白名单字段构造清单、状态、非敏感设置、`mobile_sync_batches` 精简历史、日志和数据库计数，不先序列化整个设置或认证对象。`DiagnosticRedactor` 作为第二层防护，最终还要扫描 ZIP 文件名和文本内容，检测 token、password、Authorization、cookie 等禁止项。
3. 导出前估算空间并预留压缩临时文件；写入临时路径，成功关闭 ZIP 后再发布最终文件，任一步失败都删除半成品。原始坐标只在用户确认后按导出界面明确标出的时间范围加入，并在 manifest 记录范围和条数；取消确认时相关文件不得出现。清除诊断只删日志、导出文件和诊断状态，不动业务队列、服务器或登录。
4. 连接检查收口为 4 步：地址解析与网络/TLS、服务及 `GET /api/version`、有 token 时的认证 API、Web 根页面。每步保存可读失败类别、详情和检查时间到现有 `ConnectionProbeStore`，不增加独立 runner 或并发 store。
5. `RunningStateRestorer.ensureRunningState()` 注册或更新唯一的 15 分钟周期同步、取消已知旧 work 名、读取持续采集意图，并在权限满足时恢复前台服务；系统限制启动时记录具体阻塞原因，不清除用户意图。重复调用必须幂等。
6. `StartupRecoveryReceiver` 仅处理 `BOOT_COMPLETED` 和 `MY_PACKAGE_REPLACED`，与 `PimApp.onCreate()` 一样调用同一个 `ensureRunningState()`；`AndroidManifest.xml` 声明所需接收器、权限和 foreground service 类型，不复制恢复逻辑。

**自动验证：**
- 导出安全测试：输入含 token/password/Authorization/cookie 的设置与日志，最终 ZIP 扫描器确认文件名和文本内容均不含凭据。
- 诊断清理测试：调用 clearDiagnostics 后日志文件和 ZIP 不存在，业务表不变。
- Probe 测试：mock URL 返回各步结果，验证每步失败时正确抛出且保存原因。
- Receiver 测试：发送 BOOT_COMPLETED 和 MY_PACKAGE_REPLACED intent，确认两者都调用同一个 ensureRunningState。
- ensureRunningState 幂等测试：连续调用两次，不重复注册 work、不重复启动 service。

**人工验收：**
- 模拟器上开启详细日志，24h 后确认日志级别降回 info。
- ZIP 导出后解压，确认无 token/密码字段。
- 包含坐标导出时出现确认弹窗；取消后 ZIP 无坐标。
- 强制停止应用 → 重新打开 → 采集恢复。
- 模拟器支持的前提下，重启后采集恢复或状态页显示阻塞原因。

---

## 7. 阶段整体验证

**目的：** 一次完整自动运行、一次模拟器验收、一次代码审查。确保所有任务无回归，关键路径通过。

**自动验证命令：**
```
dotnet test Pim.sln
```
```
cd src/client-android
./gradlew :core:testDebugUnitTest :app:testDebugUnitTest :app:assembleDebug
```
```
git diff --check
```

**严格测试清单：**
- 逐项确认与不丢数据
- 当前已发布 Room v3 schema 无破坏打开，并保留既有 v2→v3 迁移回归
- token 不跨服务器
- 同步 unique enqueue
- ZIP 导出排除凭据

**模拟器验收清单（阶段 1 场景）：**
1. 首次安装：输入服务器地址 → 连接测试 → 登录 → 授予权限 → 手动开启持续采集 → 采集和同步正常运行
2. 权限拒绝：关闭定位 → 状态页显示"定位权限被关闭，点击前往设置" → 点击跳转到系统设置 → 恢复权限 → 采集重启
3. 手动同步：状态页 pending 数 > 0 → 点"立即同步" → 显示 accepted / running / completed 反馈 → confirmed 数据消失
4. 网络断线：开启飞行模式 → 触发同步 → 状态页显示失败 → 恢复网络 → 下次同步自动重试
5. 采集意图断网保留：持续采集开启后断网并恢复 → 本地采集继续、意图仍开启、积压随后上传
6. 强制停止：进程被杀死 → 重新打开应用 → 采集意图恢复 → 同步继续
7. 开机恢复（模拟器支持时）：重启 → 采集意图恢复或状态页给出阻塞原因
8. ZIP 导出：取消坐标 → 确认 ZIP 无坐标；确认坐标 → 确认 ZIP 含坐标且无凭据
9. 恢复默认：重置参数 → 服务器地址和登录保留 → 采集参数回归默认 → 持续采集关闭

**代码审查要点：**
- 所有日志路径不涉及 Room 业务表
- 所有即时 `OneTimeWorkRequest` 使用同一个 `MobileSyncWorker` 和 unique name；不存在第二套上传实现
- Token 不出域（不发送到非所属服务器）
- Room 迁移无 destructive 回退
- `ensureRunningState` 不抛出未捕获异常
- 每个新建文件有对应测试

---

## 本阶段明确不做

- Lease/broker 表与分布式锁
- 新建 Room 同步历史表或 dead-letter 表（复用 `mobile_sync_batches` 和业务行状态字段）
- 多个同步 coordinator 并存
- Generation/CAS/tombstone/双回滚证明
- 独立 ProbeRunner／复杂并发 store
- 穷举线程交错测试
- 源码字符串文案测试
- 逐任务双重审查
- 今日/Tracks embed WebUI（阶段 2）
- 日程页面与策略消耗（阶段 3）
- 设置缓存/日程 WebView 内联

## 完成标准

1. 全量自动运行通过：`dotnet test Pim.sln` + `./gradlew :core:testDebugUnitTest :app:testDebugUnitTest :app:assembleDebug` 均零失败。
2. 模拟器一次通过九项验收场景。
3. 一次整体代码审查，无未处理的关键问题。
