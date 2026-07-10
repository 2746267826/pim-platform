# PIM Android Client Complete Reliability Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 完成 PIM 安卓客户端的状态可观测性、可靠同步、可配置采集、诊断导出、真实 Today/Tracks/Schedule 页面及实机验收，使所有用户可见状态和操作都由真实证据驱动。

**Architecture:** 保留原生五标签壳，Android 负责权限、采集、队列、同步运行和本地诊断；服务端 WebUI 通过两个受信任的嵌入路由负责 Today 与 Tracks 的服务端分析和地图；Schedule 保持原生并使用真实日程缓存与策略状态。工作按三个可独立构建、测试、审阅的 PR 顺序交付，任何阶段都不缩减最终范围。

**Tech Stack:** Kotlin, Jetpack Compose Material3, Hilt, Room 2.6.1, WorkManager 2.9.0, AndroidX WebKit, Retrofit/OkHttp, React 19, TypeScript 6, TanStack Query, React Leaflet, Playwright, .NET 8 Minimal API, EF Core, xUnit, JUnit4, Android instrumentation, GitHub Actions.

---

## Final Objective

产出并执行一个三阶段改造，使用户可以直接判断采集与传输是否正常、每次同步正在做什么、服务端目前收到了什么；所有按钮执行文案所描述的动作；设置和诊断具备可验证的真实行为；Today、Tracks、Schedule 不再显示生产占位数据；最终由签名 APK 的真机证据关闭全部需求。

规划阶段的 Goal 是：为 PIM 安卓客户端的状态可观测性、同步反馈、可配置项、日志导出及真实数据页面完善形成经确认的设计，并转入可执行实施计划。

## Source Of Truth

- 设计规范：`docs/superpowers/specs/2026-07-10-android-client-complete-reliability-design.md`
- 阶段一：`docs/superpowers/plans/2026-07-10-android-client-reliability-phase-1-operational-foundation.md`
- 阶段二：`docs/superpowers/plans/2026-07-10-android-client-reliability-phase-2-server-data-surfaces.md`
- 阶段三：`docs/superpowers/plans/2026-07-10-android-client-reliability-phase-3-schedule-verification.md`
- 总覆盖报告：`docs/superpowers/reports/2026-07-10-android-client-complete-reliability-coverage.md`
- 最终验收报告：`docs/superpowers/reports/2026-07-10-android-client-complete-reliability-verification.md`

`.superpowers/brainstorm/` 是已批准设计的视觉伴侣生成物，保持忽略，不进入任何提交。

## Scope Check And Decomposition

设计同时涉及 Android 运行可靠性、跨进程同步、Web 嵌入、安全鉴权、地图分析、日程策略和设备验收。这些子系统拥有不同文件所有权、测试工具和失败边界，因此不能作为一个巨型实现批次安全交付。本计划按设计规范已经批准的三个阶段拆分：

| 顺序 | 独立结果 | 计划 | 合并门槛 |
| --- | --- | --- | --- |
| 1 | 状态、同步、设置、权限、日志与导出全部真实可用 | Phase 1 | Android/API 自动测试、Room 3→4 真实迁移、Pixel_9 instrumentation、release 构建、PR checks |
| 2 | Today/Tracks 使用服务端 WebUI，原生始终显示传输状态 | Phase 2 | Web/API/Android 测试、两个手机 viewport Playwright、确定性地图像素证据、WebView instrumentation、PR checks |
| 3 | Schedule 使用真实证据并完成全链路设备验收 | Phase 3 | 全量自动测试、两台 AVD、签名 CI APK、用户真机矩阵、覆盖报告无未验证项、PR checks |

每个阶段从前一阶段已经合并的最新 `master` 创建 `codex/` 分支和独立 worktree。不得在三个阶段之间并行修改共享 DTO、Room schema、同步模型、Web bridge protocol 或根导航。

## Locked Cross-Phase Contracts

### Git And PR Order

| Phase | Branch | Depends on | PR scope |
| --- | --- | --- | --- |
| 1 | `codex/android-operational-foundation` | 本计划文档 PR 已合并 | Android + mobile ingest/version API + Phase 1 reports |
| 2 | `codex/android-server-data-surfaces` | Phase 1 已合并 | Android WebView + Web embed + location analytics contract + Phase 2 reports |
| 3 | `codex/android-schedule-completion` | Phase 2 已合并 | Native Schedule + recurrence overlap + verification scripts/reports |

每个 PR 都必须：

- 只暂存源文件、测试、脚本和文档；
- 不提交 `build/`、`dist/`、`src/Pim.Api/wwwroot/`、APK、诊断 ZIP、截图临时目录或 npm/Gradle 缓存；
- 推送分支并创建指向 `master` 的 PR；
- 观察所有按路径触发的 GitHub Actions；
- 对没有触发的 workflow 按 path filter 记录原因。

### WorkManager Names

| Purpose | Canonical name | Policy |
| --- | --- | --- |
| 15-minute fallback | `pim_mobile_sync_periodic` | exactly one unique periodic request |
| manual/foreground/retry | `pim_mobile_sync_once` | one unique immediate chain; duplicate requests join |

Phase 1 migrates and cancels `pim_upload`, `pim_mobile_background_sync`, and the obsolete one-category retry name `pim_location_upload`. Only the first two are duplicate periodic jobs; the third is removed so location retry also uses the canonical immediate chain.

### API Capability Rollout

| Capability | Added | Meaning |
| --- | --- | --- |
| `mobileItemResultsV1` | Phase 1 | usage/apps ingest returns stable item-level acknowledgement |
| `androidEmbedV1` | Phase 2, only after both embed routes pass tests | server serves trusted `/embed/android/today` and `/embed/android/tracks` |

Phase 1 treats missing `mobileItemResultsV1` as a synchronization blocker. Missing `androidEmbedV1` affects only Today/Tracks and remains a visible feature-specific warning until Phase 2 ships; it must not block queue transfer. Phase 2 adds `androidEmbedV1` to `/api/version` only in the same commit that adds passing embed route tests.

### Android Persistence

- Room starts at schema 3 and moves once to schema 4 without destructive fallback.
- Schema 4 adds `sync_runs`, `sync_dead_letters`, and `schedule_window_cache` plus required indexes.
- A sync execution lease lives on the active `sync_runs` row through owner/acquired/expiry columns; no fourth table is introduced.
- The same run persists `allow_metered_once`; a confirmed override replaces only a waiting unmetered request and remains historical evidence.
- `schedule_window_cache` always stores one metadata row for a successful fetch, including a successful empty response, so Phase 3 can distinguish empty from never fetched without schema 5.
- `mobile_logs.sync_status` migrates to `local-only` and is never used in a business queue query.
- Existing business queues, settings, device registration facts, rejected facts, and auth data remain intact.
- Room schema 3 and 4 JSON files are committed under `src/client-android/app/schemas/` because they are migration contracts, not disposable build output.

### Shared Status Semantics

```text
OperationalHealth = Healthy | NeedsAttention | Blocked | Unknown
SyncTrigger = Manual | Foreground | Periodic | Retry
SyncPhase = Queued | CheckingPrerequisites | WaitingForNetwork |
            WaitingForAllowedNetwork | RegisteringDevice | QueryingGaps |
            CollectingUsage | UploadingUsage | UploadingLocations |
            ReportingHeartbeat | Verifying | Succeeded |
            SucceededWithRejects | RetryScheduled | Blocked | Failed |
            Interrupted
```

Upload activity and operational health remain separate. A transfer can be active while health is `Healthy`; collection can be blocked while already queued data uploads successfully.

### Web Embed Routes And Protocol

- `/embed/android/today`
- `/embed/android/tracks`
- bridge object: `window.pimNative`
- native-to-Web events: `access-token`, `sync-completed`, `auth-cleared`
- Web-to-native requests/events: `request-access-token`, `refresh-access-token`, `embed-state`
- allowed origin: exact configured scheme, host, and port
- token boundary: short-lived access token in memory only; refresh token never enters Web content

### Time Boundaries

- persisted timestamps remain UTC;
- UI uses device-local time;
- Today and custom ranges use an IANA timezone and half-open UTC interval `[rangeStartUtc, rangeEndUtc)`;
- no component derives Today from a bare UTC date;
- tests include `Asia/Shanghai` midnight and a daylight-saving timezone.

## Requirement Coverage

| Design requirement | Implemented by | Final evidence |
| --- | --- | --- |
| Overall health, detailed issues, truthful actions | Phase 1 Tasks 10, 12-13 | status unit tests + Compose interaction tests + device matrix |
| Immediate manual feedback and persistent transfer state | Phase 1 Tasks 4-7, 12-13 | run-store tests + WorkManager tests + Status screenshots |
| One periodic job, foreground request, retry/network state | Phase 1 Tasks 7-8 | WorkManager inspection + offline/recovery device cases |
| Item-level acknowledgement and dead letters | Phase 1 Tasks 2, 4, 6 | .NET ingest tests + Android acknowledgement tests + Room evidence |
| Presets, bounded advanced settings, permissions | Phase 1 Tasks 9-10, 13 | validator tests + Settings Compose tests + permission return QA |
| Real connection probe and auth refresh | Phase 1 Tasks 3 and 13 | MockWebServer matrix + live probe QA |
| Local-only logs, retention, clear, ZIP export | Phase 1 Tasks 4 and 11 | ZIP schema/secret tests + share/unzip QA |
| Boot/update/process recovery | Phase 1 Task 8 | coordinator tests + AVD cold boot + physical reboot |
| Server-only Today | Phase 2 Tasks 1-2, 4, 6, 9-10 | Web states + timezone tests + Android wrapper QA |
| Server-only Tracks, maps, filters, pagination | Phase 2 Tasks 1-3, 5-6, 9-10 | API contract + Playwright map/layout + device QA |
| Trusted WebView bridge and navigation | Phase 2 Tasks 1 and 8-10 | TypeScript protocol tests + Android instrumentation |
| Real Schedule state/cache/transitions | Phase 3 Tasks 1-5 | repository/policy/UI tests + live schedule matrix |
| Emulator, signed APK, physical device, coverage | Phase 3 Tasks 6-9 | verification report and CI links |

## Execution Sequence

### Task 1: Execute Phase 1

**Files:**
- Execute: `docs/superpowers/plans/2026-07-10-android-client-reliability-phase-1-operational-foundation.md`
- Create during execution: `docs/superpowers/reports/2026-07-10-android-client-reliability-phase-1.md`

- [ ] **Step 1: Create the isolated Phase 1 worktree**

Invoke `superpowers:using-git-worktrees`, update `master` with `git pull --ff-only`, and create branch `codex/android-operational-foundation` from `origin/master`.

- [ ] **Step 2: Execute every checkbox in the Phase 1 plan**

Use `superpowers:subagent-driven-development` or `superpowers:executing-plans` with:

```text
docs/superpowers/plans/2026-07-10-android-client-reliability-phase-1-operational-foundation.md
```

- [ ] **Step 3: Merge only after Phase 1 gates pass**

Expected: Room migration evidence is green, one scheduler is visible, Status/Settings/diagnostics are behavior-tested, relevant PR checks pass, and the coverage report marks Phase 1 rows `Verified`.

### Task 2: Execute Phase 2

**Files:**
- Execute: `docs/superpowers/plans/2026-07-10-android-client-reliability-phase-2-server-data-surfaces.md`
- Create during execution: `docs/superpowers/reports/2026-07-10-android-client-reliability-phase-2.md`

- [ ] **Step 1: Rebase the next worktree on merged Phase 1**

Create `codex/android-server-data-surfaces` from the updated `origin/master`; do not branch from an unmerged Phase 1 head.

- [ ] **Step 2: Execute every checkbox in the Phase 2 plan**

Use:

```text
docs/superpowers/plans/2026-07-10-android-client-reliability-phase-2-server-data-surfaces.md
```

- [ ] **Step 3: Merge only after Phase 2 gates pass**

Expected: both embed routes are sidebar-free, trusted bridge tests pass, deterministic map tiles and overlays are nonblank at both phone viewports, Android wrapper transfer state remains visible, and relevant PR checks pass.

### Task 3: Execute Phase 3 And Close The Program

**Files:**
- Execute: `docs/superpowers/plans/2026-07-10-android-client-reliability-phase-3-schedule-verification.md`
- Create during execution: `docs/superpowers/reports/2026-07-10-android-client-complete-reliability-verification.md`

- [ ] **Step 1: Rebase the final worktree on merged Phase 2**

Create `codex/android-schedule-completion` from the updated `origin/master`.

- [ ] **Step 2: Execute every checkbox in the Phase 3 plan**

Use:

```text
docs/superpowers/plans/2026-07-10-android-client-reliability-phase-3-schedule-verification.md
```

- [ ] **Step 3: Stop at the physical-device gate when no phone is connected**

Do not substitute a debug APK, emulator, source assertion, or build result for the signed-APK physical matrix. Preserve exact automated results and resume when the user's phone and the signed CI artifacts are available.

- [ ] **Step 4: Mark completion only after all evidence is recorded**

Expected: all 16 completion criteria in the design spec map to passing evidence, the final coverage report has no `Planned`, `Implemented`, `Blocked`, or `Unverified` row, and all relevant PR checks are green.

## Global Verification Commands

Run from repository root unless the command changes directory:

```powershell
dotnet test Pim.sln
npm --prefix src/client-web run test:android-embed
npm --prefix src/client-web run build
Set-Location src/client-android
.\gradlew.bat testDebugUnitTest --no-daemon
.\gradlew.bat :app:connectedDebugAndroidTest --no-daemon
.\gradlew.bat :app:assembleRelease --no-daemon
```

After `npm --prefix src/client-web run build`, restore the generated `src/Pim.Api/wwwroot/` working-tree output to its pre-command state without staging it. Never use a destructive reset against user work; inspect `git status --short --branch` and remove only build artifacts created by the current execution.

## Completion Rule

Passing one phase is a useful checkpoint, not completion. The Android reliability program is complete only after Phase 3 records a successful signed-APK physical-device matrix and the final coverage report contains evidence for every design requirement.
