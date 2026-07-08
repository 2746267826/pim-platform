# Android App v2 Complete Redesign Design

Date: 2026-07-08

## Final Goal

Build a complete Android v2 client for PIM that is usable as a real mobile collection app.

The Android app must no longer feel like a broken launcher or a hidden daemon. It must let the user enter the API address clearly, log in, understand permissions, see today's movement, inspect historical location records, understand schedule-driven low-frequency tracking, diagnose background collection, and configure collection policy.

The final Android v2 outcome must include:

1. A redesigned native Android UI with five bottom tabs: `今日`, `轨迹`, `日程`, `状态`, `设置`.
2. A first-run and settings flow where the API address is a first-class input. The app must support a user-entered public IP or domain. It must not rely on a hardcoded true-device `127.0.0.1` default.
3. A manually enabled `持续采集` mode. The app must not start continuous background location secretly.
4. A location foreground service with a real, informative persistent notification.
5. Background location permission support, because the user explicitly accepted the `始终允许` route for reliable continuous collection.
6. A configurable tracking policy with default power-saving values.
7. Schedule-aware low-frequency tracking: if the current time is inside a schedule item with location information, the app lowers location frequency because the user is likely staying there.
8. Motion-aware recovery: activity or large movement can shorten the interval again.
9. A hard location quality gate: uploaded points must have horizontal accuracy `< 50m`. Points with no horizontal accuracy or accuracy `>= 50m` are not uploaded.
10. Altitude handling: after a horizontally valid fix, wait up to `15s` for altitude. If altitude is still missing, upload `altitude = null` with a quality flag, not fake `0`.
11. Local persistence and upload queues so API/network failures do not lose data.
12. A status center that makes API failures, missing permissions, queue backlog, low-quality dropped points, heartbeat failures, and notification/service state visible and actionable.
13. Tests and manual verification for UI, policy state machine, location quality gate, notification content, API settings, queues, sync, and Android build.

This design is not optional guidance. The follow-up implementation plan must cover every final goal above. It must not drop requirements for convenience, reduce scope silently, or turn confirmed design decisions into vague "future improvements."

## Required Delivery Discipline For Later Work

The follow-up implementation work must:

1. Create a new feature branch before code changes, using the repository convention, for example `codex/android-app-v2-redesign`.
2. Use subagents working concurrently where tasks are independent. The implementation plan must explicitly assign subagents, not merely mention them.
3. Make focused commits at appropriate stable checkpoints, using conventional messages such as `feat:`, `fix:`, `test:`, or `docs:`.
4. Keep generated outputs out of commits, especially `.superpowers/brainstorm/`, Android `build/`, API/Web build outputs, caches, `bin/`, and `obj/`.
5. Push the branch and create a pull request at the end of the implementation.
6. Wait for GitHub Actions automatic builds after pushing the PR. The work is not complete until relevant checks finish successfully or failures are explicitly investigated and documented.
7. Run local verification before pushing. Android changes require Android unit/build verification; backend changes require `dotnet test Pim.sln`; web changes require `npm --prefix src/client-web run build`.
8. If any verification or GA check fails, the branch must not be described as complete unless the failure is proven unrelated and documented with exact details.

Suggested parallel subagent lanes for the implementation plan:

- Subagent 1: Android UI navigation and visual system.
- Subagent 2: API address, login, settings persistence, and Retrofit rebuild behavior.
- Subagent 3: Foreground location service, notification channel, notification actions, and manifest permissions/service types.
- Subagent 4: Location policy engine, schedule-low-frequency logic, movement threshold, and activity recognition integration.
- Subagent 5: Location quality gate, altitude waiting, Room queue, DTO/API payload fields.
- Subagent 6: Status center, permission center, logs, error states, and diagnostics.
- Subagent 7: Sync and WorkManager boundaries, heartbeat, retry behavior, offline queue handling.
- Subagent 8: Android tests, backend contract tests if needed, build scripts, and GA readiness.

## Current Repository Context

The Android client lives under `src/client-android`.

Before this design was written, `master` was fast-forwarded to `origin/master` on 2026-07-08. The latest code now includes an Android companion shell:

- `src/client-android/app/src/main/java/com/pim/app/ui/shell/PimShellActivity.kt`
- `src/client-android/app/src/main/java/com/pim/app/ui/permissions/PermissionCenterScreen.kt`
- `src/client-android/app/src/main/java/com/pim/app/sync/EndpointUploadWorker.kt`
- notification action support under `src/client-android/app/src/main/java/com/pim/app/notifications/`

The current shell still does not satisfy this v2 design:

- The launcher shell is a companion/web-shell style surface, not the confirmed five-tab mobile collection app.
- Existing Android UI strings include mojibake in several files and must be corrected.
- API address configuration exists in earlier Android code, but the app experience still fails the user's requirement because the API address is not a clear first-run requirement and `127.0.0.1` is misleading for real phones.
- `AndroidManifest.xml` has `POST_NOTIFICATIONS` and `FOREGROUND_SERVICE`, but this design needs a real location foreground service declaration with location foreground service type and background location permission flow.
- The existing manual location capture accepts the earlier phase's simple manual-location assumptions. It does not implement continuous collection, policy states, motion signals, 100m schedule-low-frequency recovery, altitude wait, or strict `< 50m` upload gate.
- `WorkManager` exists but must remain a sync/heartbeat/retry mechanism, not the minute-level location scheduler.

This design builds on the existing Android, Room, Retrofit, Hilt, WorkManager, Mobile module, and heartbeat patterns. It does not require replacing the whole Gradle project.

## Conversation Record And Confirmed Decisions

The design emerged from the following discussion. This section records every substantive decision so later planning cannot reinterpret the scope.

1. Initial user problem:
   - Android app is currently not usable.
   - The interface forgot to provide a place to connect to the API.
   - It defaults to a local address that cannot open anything on the phone.
   - UI is visually poor.
   - Need Android UI reconstruction.
   - Need a plan for continuous running under Android background restrictions.
   - A persistent notification is acceptable, but it must show real information rather than exist only to keep the app alive.
   - Need location interval logic: schedule context may allow longer intervals, but large movement should restore normal interval.
   - Motion sensors/activity recognition may shorten intervals when movement is detected.
   - Location accuracy limits must remain.
   - We should research similar projects and Android platform behavior.

2. Scope choice:
   - Options were complete rebuild, basic rescue, or location-first.
   - User chose complete rebuild.
   - Final direction: not just fixing API/login; redesign UI, background collection, notification, dynamic location policy, permission guidance, error handling, and verification.

3. API connection:
   - User clarified there is no need for LAN scanning, QR codes, emulator special cases, or multiple connection flows.
   - The app only needs a visible API address input.
   - Current use is like a public server IP, not a LAN-only local API.
   - Final decision: first-run/settings must provide a clear API address input accepting public IP or domain.

4. Continuous collection activation:
   - Options were login-default continuous collection, manual toggle, or automatic by schedule/context.
   - User chose manual toggle.
   - Final decision: continuous collection starts only when the user turns on `持续采集`.

5. Background location permission:
   - User accepted requesting background location / `始终允许`.
   - Final decision: reliable continuous location collection may require background location permission, requested with clear explanation and ability to disable.

6. Schedule-location logic clarification:
   - Assistant initially misunderstood this as managing location targets or place semantics.
   - User corrected it: if current time is inside a schedule that has location information, the user probably will not move much, so the app can reduce location frequency to save resources.
   - If location changes significantly, for example over `100m`, restore the normal interval.
   - Final decision: this is a time-context strategy, not a place-management feature.

7. Location policy parameters:
   - Options included balanced, battery-saving, precise, and configurable.
   - User chose configurable with default battery-saving.
   - Final default: normal interval `3 min`, schedule-low-frequency interval `15 min`, movement interval `1 min`, large-movement recovery threshold `100m`.
   - These defaults are configurable in settings.

8. Location upload quality:
   - User required uploaded location to include altitude if possible.
   - If a horizontally accurate fix is obtained and altitude is still missing after `15s`, altitude should be missing/defaulted.
   - User required location accuracy to be `< 50m`; points over the limit are not accepted.
   - Final decision: no upload for missing horizontal accuracy or horizontal accuracy `>= 50m`. For missing altitude after waiting, upload `altitude = null` plus a quality flag, never fake `0`.

9. Persistent notification content:
   - User chose comprehensive notification content.
   - Final notification should summarize both collection state and strategy state while staying concise:
     - current mode/strategy,
     - next location time,
     - recent location time,
     - recent accuracy,
     - pending upload count,
     - API/sync status,
     - actions for pause, sync now, and open status.

10. Home screen emphasis:
   - Assistant initially recommended status-first.
   - User rejected status-first: status is important, but it should live in a separate section rather than own the first screen.
   - User chose data overview as home direction.
   - Final decision: status is a full `状态` tab; it is not the app homepage.

11. Global navigation:
   - User chose five bottom tabs.
   - Final tabs: `今日`, `轨迹`, `日程`, `状态`, `设置`.

12. Visual style:
   - User chose clean map-tool style.
   - Final visual direction: light UI, restrained panels, blue/green emphasis, yellow only for quality/strategy warnings, map/data balanced, no dark monitoring dashboard, no diary-first lifestyle UI.

13. Today screen content ratio:
   - User asked to choose from visual mockups rather than abstract text.
   - User chose location-primary and mobile-usage-secondary.
   - Final decision: `今日` first screen is primarily today's movement/track/stays/quality; mobile app usage is shown as a compact summary and link, not equal visual weight.

14. UI architecture:
   - User approved UI skeleton v1.
   - Final UI module structure:
     - `今日`: location-first overview with mobile usage summary.
     - `轨迹`: history map, tracks, stays, raw point details, quality filters.
     - `日程`: schedule windows with location information and their effect on policy.
     - `状态`: API, permissions, foreground service, notification, queue, heartbeat, logs, recent errors.
     - `设置`: API address, login, continuous collection switch, background location permission, notification permission, usage permission, tracking policy parameters.

15. Background location state machine:
   - User approved state machine v1.
   - Final states:
     - off,
     - power-saving normal,
     - schedule low frequency,
     - motion observation,
     - movement recovery,
     - sync fallback.

16. Architecture/data flow:
   - User approved architecture/dataflow v1.
   - Final boundaries:
     - UI reads ViewModel state and sends commands.
     - `ForegroundLocationService` owns continuous location and notification.
     - `LocationPolicyEngine` owns policy decisions.
     - `MotionSignalRepository` supplies activity changes.
     - `LocationQualityGate` owns upload eligibility.
     - Room stores local facts and queues.
     - `MobileSyncCoordinator` uploads.
     - WorkManager is a sync/heartbeat/retry fallback, not the minute-level scheduler.

17. Permissions, errors, testing:
   - User approved the final section.
   - Final decision: errors are visible in `状态` as actionable rows, not hidden toasts or blocking pop-up stacks.

18. Documentation and implementation discipline:
   - User required this document to include the prior discussion and all visual companion pages.
   - User required the final goal to be explicit.
   - User required later plans to fully complete the design without omission.
   - User required later implementation to create a new branch, push a PR, commit at appropriate times, wait for GA/GitHub Actions, and use subagents working concurrently.

## Visual Companion Pages Produced

Raw visual companion HTML lives in `.superpowers/brainstorm/android-app-20260708-213008/content/`. That directory is intentionally ignored by Git according to repository hygiene rules, so raw generated HTML is not committed. This design records every page, its purpose, and the decision it produced.

1. `.superpowers/brainstorm/android-app-20260708-213008/content/android-ui-home-options.html`
   - Purpose: compare first-screen directions.
   - Options shown:
     - status-first mobile collection console,
     - data-overview first screen,
     - location-quality first screen.
   - User chose: data-overview first screen.
   - Design result: homepage should not be status-first.

2. `.superpowers/brainstorm/android-app-20260708-213008/content/android-module-architecture.html`
   - Purpose: compare whole-app navigation structures.
   - Options shown:
     - five bottom tabs,
     - workbench entry grid,
     - sidebar-style management tool.
   - User chose: five bottom tabs.
   - Design result: `今日`, `轨迹`, `日程`, `状态`, `设置`.

3. `.superpowers/brainstorm/android-app-20260708-213008/content/android-visual-style-options.html`
   - Purpose: compare visual style.
   - Options shown:
     - clean map-tool style,
     - dark monitoring dashboard,
     - lifestyle timeline.
   - User chose: clean map-tool style.
   - Design result: light UI, restrained panels, blue/green emphasis, readable in daily use.

4. `.superpowers/brainstorm/android-app-20260708-213008/content/today-home-content-options.html`
   - Purpose: compare the relationship between location records and mobile usage on `今日`.
   - Options shown:
     - location-primary with mobile usage summary,
     - location and mobile usage equally prominent,
     - location-only.
   - User chose: location-primary with mobile usage as secondary summary.
   - Design result: Android app is primarily the movement/location collection client; deep mobile usage analytics remain more appropriate for Web.

5. `.superpowers/brainstorm/android-app-20260708-213008/content/ui-architecture-v1.html`
   - Purpose: show the approved five-tab UI skeleton.
   - Pages shown:
     - Today overview,
     - historical tracks,
     - schedule policy,
     - status center,
     - settings.
   - User approved.
   - Design result: this skeleton is the baseline UI structure for implementation.

6. `.superpowers/brainstorm/android-app-20260708-213008/content/location-state-machine-v1.html`
   - Purpose: show background location policy states and upload quality gate.
   - Content shown:
     - off,
     - power-saving normal,
     - schedule low frequency,
     - motion observation,
     - movement recovery,
     - sync fallback,
     - `< 50m` location quality gate,
     - `15s` altitude wait,
     - notification examples.
   - User approved.
   - Design result: policy state machine is accepted.

7. `.superpowers/brainstorm/android-app-20260708-213008/content/android-architecture-dataflow-v1.html`
   - Purpose: show architecture/data flow across UI, service, policy engine, Room, WorkManager, and API.
   - User approved.
   - Design result: foreground service and policy engine own location; UI and WorkManager do not.

8. `.superpowers/brainstorm/android-app-20260708-213008/content/permissions-errors-testing-v1.html`
   - Purpose: show first-run guidance, status-center errors, and acceptance checks.
   - User approved.
   - Design result: API address first, permission checklist, actionable status errors, explicit verification.

9. `.superpowers/brainstorm/android-app-20260708-213008/content/waiting-after-home-selection.html`
   - Purpose: clear the visual companion after a choice was made.
   - Design result: no product decision; included for completeness.

## Product Positioning

The Android app is a mobile collection and movement review client.

It is not a Web dashboard clone and not a hidden daemon. It should be comfortable to open daily, but its most important responsibility is reliable collection with understandable feedback.

The Android app should answer:

- Is my phone connected to the PIM API?
- Is continuous collection on or off?
- Did today's movement get captured?
- Where did I stay and move today?
- Are location points good enough?
- Why is the app using low-frequency or normal-frequency location?
- Are there pending uploads or errors?
- Which permissions are missing?
- What should I do next if collection is unhealthy?

## Information Architecture

### `今日`

`今日` is the default tab.

Content hierarchy:

1. Page title: `今日概览`.
2. Small collection status chip, for example `运行中`, `暂停`, or `待配置`.
3. Today's map preview:
   - track line,
   - stay markers,
   - optional quality hint,
   - no dense raw-point wall.
4. Metrics:
   - stays,
   - estimated distance,
   - quality/completeness.
5. Mobile usage summary:
   - total foreground time,
   - Top apps,
   - link to Web/deeper analysis if implemented.
6. Current policy summary:
   - power-saving normal,
   - schedule low frequency,
   - moving,
   - next expected location time.

`今日` must not show full status diagnostics. It may show a small warning and link to `状态`.

### `轨迹`

`轨迹` is for history and detail.

Content hierarchy:

1. Range selector: today, 7 days, 30 days, custom.
2. Quality filters:
   - max accuracy, default `< 50m`,
   - show/hide low-quality dropped points as diagnostics,
   - show/hide altitude-missing flags.
3. Map:
   - track segments,
   - stay points,
   - movement segments,
   - gaps,
   - error/accuracy circles when enabled.
4. Segment timeline:
   - movement,
   - stay,
   - gap,
   - low-confidence segment.
5. Selected segment detail:
   - time range,
   - estimated distance or stay duration,
   - point count,
   - average/max accuracy,
   - provider mix,
   - altitude availability.
6. Raw point list:
   - scoped to selected segment by default,
   - paginated or virtualized,
   - not the first visual priority.

### `日程`

`日程` is not a calendar replacement. It explains schedule-driven collection policy.

Content hierarchy:

1. Current schedule window with location information, if any.
2. Current policy effect:
   - `日程低频`,
   - low-frequency interval,
   - anchor point/time,
   - exit conditions.
3. Upcoming schedule windows with location information.
4. Recent policy transitions:
   - entered schedule low frequency,
   - motion detected,
   - moved over 100m,
   - restored normal interval,
   - schedule ended.
5. Diagnostics:
   - schedule has location text but cannot parse or compare,
   - server schedule unavailable,
   - stale schedule cache.

Important: the app does not need to create location targets or semantic places for this phase. It only uses "current time is inside a schedule item that has location information" as a signal to reduce frequency.

### `状态`

`状态` is the operational and diagnostic center.

It must include:

- API connection status,
- authenticated user/token state,
- continuous collection state,
- foreground service state,
- notification permission/state,
- foreground location permission,
- background location permission,
- usage access permission,
- current policy mode,
- next expected location time,
- last accepted location,
- last dropped location reason,
- upload queue count,
- heartbeat status,
- last sync attempt,
- last sync success,
- last API error,
- recent structured logs,
- action buttons:
  - retry,
  - open settings,
  - open permission screen,
  - pause/resume continuous collection,
  - sync now.

Errors must be actionable rows, not modal clutter.

### `设置`

`设置` is for configuration and durable choices.

First group:

- API address input.
- Login/logout.
- Connection test.

Second group:

- Continuous collection toggle.
- Background location permission status and explanation.
- Notification permission status and explanation.
- Usage access permission status and explanation.

Third group:

- Tracking profile: default `省电档`.
- Normal interval: default `3 min`.
- Schedule low-frequency interval: default `15 min`.
- Movement interval: default `1 min`.
- Schedule movement recovery threshold: default `100m`.
- Altitude wait timeout: fixed/default `15s`.
- Upload accuracy threshold: fixed hard gate `< 50m`.

The UI may expose advanced parameters behind a collapsible advanced section, but the defaults above must be visible enough for a user to understand current behavior.

## Visual Design System

Chosen visual direction: clean map-tool style.

Rules:

- Light background.
- White or near-white primary panels.
- Blue for map/location emphasis.
- Green/teal for healthy collection, accepted points, and active running.
- Yellow for strategy/quality warnings such as schedule low-frequency or missing altitude timeout.
- Red only for actual blocking errors.
- No dark monitoring-dashboard default.
- No lifestyle diary-first visual hierarchy.
- No marketing hero sections.
- No nested card stacks.
- Cards/panels use restrained radii, around 8-12dp depending on Android component constraints.
- Text must be Chinese by default, with technical terms such as `API`, `GPS`, `UTC`, and package names only where useful.
- Data screens should be scan-friendly and dense enough for repeated use.

## Continuous Collection And Android Platform Model

### Activation

Continuous collection starts only when the user enables `持续采集`.

When enabled:

1. Validate API address and login.
2. Validate notification permission.
3. Validate foreground precise location permission.
4. Validate background location permission.
5. Start the foreground location service.
6. Show persistent notification with real state.
7. Register heartbeat/status.

When disabled:

- Stop minute-level background location.
- Keep local queued data.
- Allow manual sync/open-app sync.
- Status should say collection is intentionally off.

### Foreground Service

Implement a real `ForegroundLocationService`.

Manifest and runtime requirements:

- Declare the service in `AndroidManifest.xml`.
- Use location foreground service type.
- Include foreground service permission requirements for the app's target SDK.
- Request notification runtime permission on Android 13+.
- Request background location permission through an explicit user flow.

The service owns:

- location update requests,
- policy engine updates,
- notification rendering,
- pause/resume command handling,
- queueing accepted points,
- logging dropped points and policy transitions.

The service does not own:

- large UI composition,
- API URL editing,
- login UI,
- deep analytics rendering.

### WorkManager Boundary

WorkManager is used for:

- network-restored queue upload,
- periodic heartbeat if appropriate,
- retrying failed upload batches,
- coarse sync fallback.

WorkManager must not be used as the minute-level location scheduler. Android periodic work has a minimum interval and is not suited to the confirmed location policy.

## Location Policy Engine

Create `LocationPolicyEngine` with explicit states.

States:

1. `Off`
   - Continuous collection disabled.
   - No background continuous location.

2. `PowerSavingNormal`
   - Default after continuous collection starts.
   - Location interval: `3 min`.
   - Motion transitions are observed.

3. `ScheduleLowFrequency`
   - Active when current time is inside a schedule item with location information.
   - Location interval: `15 min`.
   - Keeps an anchor from the accepted point around entering this state when possible.
   - Still monitors activity/motion.

4. `MotionObservation`
   - Activated when activity recognition indicates meaningful movement.
   - Location interval: `1 min`.
   - Can occur from normal or schedule-low-frequency.

5. `MovementRecovery`
   - Triggered when schedule-low-frequency mode sees displacement over `100m` from the low-frequency anchor or recent accepted point.
   - Restores normal/movement interval.

6. `SyncFallback`
   - Not a location mode.
   - Represents queued upload/heartbeat retry behavior when network/API is unavailable.

Transition rules:

- `Off -> PowerSavingNormal`: user enables continuous collection and required permissions are present.
- `PowerSavingNormal -> ScheduleLowFrequency`: current time enters a schedule window with location information.
- `ScheduleLowFrequency -> PowerSavingNormal`: schedule ends.
- `ScheduleLowFrequency -> MovementRecovery -> PowerSavingNormal`: accepted point shows displacement over `100m`.
- `PowerSavingNormal or ScheduleLowFrequency -> MotionObservation`: activity recognition reports meaningful movement.
- `MotionObservation -> PowerSavingNormal`: activity returns to still and no schedule-low-frequency window is active.
- `MotionObservation -> ScheduleLowFrequency`: activity returns to still and schedule-low-frequency window is still active, unless movement recovery was triggered.
- Any active state -> `Off`: user disables continuous collection or blocking permission is removed.

Motion detection should prefer Android activity transition APIs where available. It should be treated as a policy signal, not as a location fact by itself.

## Location Quality Gate

Create `LocationQualityGate`.

Rules:

1. A point with no horizontal accuracy is rejected locally and not uploaded.
2. A point with horizontal accuracy `>= 50m` is rejected locally and not uploaded.
3. A point with horizontal accuracy `< 50m` is eligible for altitude wait.
4. If altitude is present on the eligible fix, queue it immediately.
5. If altitude is missing, wait up to `15s` for a better eligible fix with altitude.
6. If altitude is still missing after `15s`, queue the location with `altitudeMeters = null`.
7. Missing altitude after timeout adds a quality flag such as `altitude-missing-timeout`.
8. Never use `0` as fake altitude.
9. Store dropped-point diagnostics locally:
   - timestamp,
   - provider,
   - accuracy,
   - policy mode,
   - reason.

Location payloads should include:

- device id,
- recorded timestamp,
- submitted timestamp,
- latitude,
- longitude,
- horizontal accuracy,
- altitude if present,
- provider/source,
- speed/bearing if present,
- current policy mode,
- schedule-low-frequency flag,
- motion state if known,
- quality flags,
- raw provider fields where safe.

## Persistent Notification

The notification must be useful, not just a keepalive artifact.

Collapsed notification should show:

- app title, for example `PIM 持续采集`,
- current strategy, for example `省电档`, `日程低频`, `运动中`,
- next expected location time,
- recent location time,
- recent accepted accuracy,
- pending upload count or API error.

Expanded notification may show:

- API state,
- last sync result,
- last dropped-point reason,
- current schedule-low-frequency reason,
- quick actions.

Notification actions:

- `暂停` or `继续`,
- `立即同步`,
- `打开状态`.

Notification content examples:

- `日程低频中 · 下次定位约 12 分钟后`
- `最近定位 21:24 · 精度 18m · 待上传 3 条`
- `检测到移动，定位间隔临时缩短为 1 分钟`
- `API 无法连接 · 已缓存 18 条`

## API Address And Authentication

The first-run/settings experience must make API address entry obvious.

Rules:

- The app must accept a public IP or domain.
- It may show an example like `http://203.0.113.8:5858/api/v1/` or `https://pim.example.com/api/v1/`.
- Do not present true-device `127.0.0.1` as a useful default for real phones.
- If a stored `127.0.0.1` or `localhost` value exists, show an inline warning explaining that it points to the phone itself on real devices.
- Saving API address should rebuild Retrofit clients through the existing dynamic provider pattern.
- Login must be tied to the currently configured API address.
- API test should distinguish:
  - invalid URL,
  - network timeout,
  - server reachable but unauthorized,
  - TLS/cleartext issue,
  - wrong path.

## Local Storage And Sync

Room should store:

- pending accepted location points,
- rejected/dropped location diagnostics,
- policy transition logs,
- mobile usage records and metadata where existing flow requires,
- sync batches,
- heartbeat state,
- recent structured logs,
- user settings or references to settings store as appropriate.

Accepted points are queued before upload. Upload success marks rows synced. Partial failures must not lose accepted rows.

API failure behavior:

- Keep local queue.
- Update status center.
- Update notification summary.
- Retry through sync coordinator and WorkManager fallback.

## Server/API Contract Impact

Existing endpoints should be reused where suitable:

- `POST /api/v1/mobile/devices/register`
- `POST /api/v1/mobile/sync/gaps`
- `POST /api/v1/mobile/usage/events`
- `POST /api/v1/mobile/location/points`
- `POST /api/v1/daemon/heartbeat`

The Android v2 UI may need mobile query endpoints for app-native summaries:

- `GET /api/v1/mobile/today`
- `GET /api/v1/mobile/location/summary`
- `GET /api/v1/mobile/location/history`
- `GET /api/v1/mobile/location/segments`

If these do not exist, the implementation plan must either add them or explicitly map equivalent existing endpoints. It may not silently replace the confirmed Android UI with an embedded Web shell unless the user approves a scope change.

## Error Handling

Error display belongs primarily in `状态`.

Required errors:

- API address missing.
- API URL invalid.
- API unreachable.
- Login missing or expired.
- Notification permission missing.
- foreground location permission missing.
- background location permission missing.
- usage access missing.
- foreground service not running when continuous collection is enabled.
- point rejected because horizontal accuracy is missing.
- point rejected because horizontal accuracy is `>= 50m`.
- altitude missing after `15s`.
- upload queue backlog.
- heartbeat failure.
- schedule data unavailable.
- activity recognition unavailable or permission missing if applicable.

Each error row should include:

- what happened,
- why it matters,
- last occurrence time,
- action button or next step,
- whether collection is paused or degraded.

## Testing And Verification

### Android Unit Tests

Required coverage:

- API URL normalization and warning for true-device localhost.
- API address save triggers client rebuild.
- continuous collection toggle starts/stops intended state.
- permission state model for notification, foreground location, background location, usage access.
- `LocationPolicyEngine` transitions:
  - off to normal,
  - normal to schedule low frequency,
  - schedule low frequency to normal on schedule end,
  - schedule low frequency to movement recovery when displacement exceeds `100m`,
  - motion observation interval change.
- configurable power-saving defaults.
- `LocationQualityGate`:
  - no accuracy rejected,
  - `49.9m` accepted,
  - `50.0m` rejected,
  - `>50m` rejected,
  - altitude present accepted immediately,
  - altitude missing waits up to `15s`,
  - altitude timeout queues null altitude with quality flag.
- notification renderer text for normal, schedule-low-frequency, moving, API failure, and queue backlog.
- Room queue marking for success, partial failure, retry, and dropped-point diagnostics.

### Android Build/Manual Verification

Run from `src/client-android` as appropriate:

- `.\gradlew.bat testDebugUnitTest`
- `.\gradlew.bat assembleDebug`

Manual verification:

1. Fresh install on a real Android device.
2. First screen requires or clearly presents API address.
3. Enter public server IP URL.
4. Log in.
5. Grant required permissions.
6. Enable continuous collection manually.
7. Confirm persistent notification shows real state.
8. Confirm status center sees foreground service, permissions, queue, API, heartbeat.
9. Simulate or observe schedule-low-frequency window.
10. Simulate/observe movement and confirm interval changes.
11. Confirm points with `>=50m` are dropped locally.
12. Confirm missing altitude behavior after `15s`.
13. Turn API server off and confirm local queue and status error.
14. Turn API server back on and confirm retry upload.

### Backend Verification

If backend endpoints or DTOs are touched:

- `dotnet test Pim.sln`

Backend tests should cover:

- location point rejects `>=50m`,
- accepts `<50m`,
- accepts null altitude with quality flag,
- stores policy/source/quality metadata,
- heartbeat accepts Android status JSON.

### Web Verification

If web pages or mobile analytics contracts are touched:

- `npm --prefix src/client-web run build`

### GA / GitHub Actions Verification

After PR push:

- Wait for Android workflow.
- Wait for API workflow if backend changed.
- Wait for Web workflow if web changed.
- Wait for Windows workflow if shared endpoint contracts affect Windows.

The PR is not done until relevant workflows are green or any failure is fully diagnosed and reported.

## Implementation Plan Requirements

The next implementation plan must be a complete plan for this design. It must explicitly include tasks for:

- branch creation,
- subagent dispatch,
- UI shell and five tabs,
- visual system,
- API address input,
- login and token handling,
- settings persistence,
- permission flows,
- continuous collection toggle,
- foreground location service,
- notification channel/actions/content,
- background location permission,
- activity recognition/motion signal,
- schedule-low-frequency policy,
- 100m recovery,
- configurable policy values,
- location quality gate,
- altitude wait,
- Room queue and dropped diagnostics,
- sync and WorkManager boundaries,
- status center errors,
- Android tests,
- backend/API tests if touched,
- local builds,
- commits,
- PR creation,
- GA wait/check.

The plan must map each final goal and each approved visual/spec section to at least one implementation task. A plan that says only "refactor Android UI" or "improve background tracking" is not acceptable.

## References

Official Android references:

- Foreground service types: https://developer.android.com/develop/background-work/services/fgs/service-types
- Background location guidance: https://developer.android.com/develop/sensors-and-location/location/background
- Activity recognition transitions: https://developer.android.com/develop/sensors-and-location/location/transitions
- Notification runtime permission: https://developer.android.com/develop/ui/views/notifications/notification-permission
- WorkManager periodic work reference: https://developer.android.com/reference/androidx/work/PeriodicWorkRequest
- Google Play services `LocationRequest`: https://developers.google.com/android/reference/com/google/android/gms/location/LocationRequest

Similar project references considered for design direction:

- OwnTracks: https://owntracks.org/
- GPSLogger for Android: https://gpslogger.app/
- Traccar Client: https://www.traccar.org/client/

Design lessons from these references:

- Long-running location apps should be transparent about active collection.
- Notifications should communicate useful state, not just keep a process alive.
- Offline queues are necessary because mobile connectivity is unreliable.
- Time, distance, accuracy, and activity/motion signals should be combined instead of using one fixed interval for all situations.
- Android platform restrictions make a foreground service and explicit permissions the honest route for reliable continuous collection.

## Completion Definition

The Android v2 redesign is complete only when all of the following are true:

1. The app opens into the confirmed five-tab native UI.
2. `今日` is location-primary with mobile usage summary.
3. `轨迹`, `日程`, `状态`, and `设置` match their defined responsibilities.
4. First-run/settings clearly support user-entered API address.
5. A user can connect to a public server IP/domain, log in, and sync.
6. Continuous collection is manually controlled.
7. Background location permission is supported and explained.
8. Foreground location service runs with location service type and real notification.
9. Notification shows current strategy, next location, recent accuracy, queue, and API/sync state.
10. Default power-saving policy is implemented and configurable.
11. Schedule-low-frequency behavior is implemented.
12. Motion observation is implemented or explicitly unavailable with a status warning.
13. Displacement over `100m` exits schedule low frequency.
14. Location upload requires horizontal accuracy `< 50m`.
15. Altitude wait is implemented with `15s` timeout and null-altitude quality flag.
16. WorkManager is used only for sync/heartbeat/retry fallback.
17. Room queues accepted points and retains diagnostics for rejected points.
18. Status center exposes errors and next actions.
19. Android tests and build pass locally.
20. Backend/Web tests pass if those surfaces are touched.
21. A feature branch is pushed.
22. A PR is created.
23. Relevant GitHub Actions complete successfully or failures are fully diagnosed.

