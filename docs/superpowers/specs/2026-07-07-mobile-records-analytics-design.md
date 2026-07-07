# Mobile Records Analytics Redesign

Date: 2026-07-07

## Context

The current `手机记录` page proves that Android upload works, but it is not readable enough for real use. The page mostly shows raw app sessions as repeated cards. In real data this becomes a long wall of package names such as `com.tencent.mobileqq`, `com.android.launcher`, and `com.heytap.quicksearchbox`. The timeline endpoint also currently takes only the first 500 sessions or fallback rows for a selected day, so users cannot inspect large days or broader ranges reliably.

The repository already has useful foundations:

- Android uploads `UsageEvents`, fallback `UsageStats`, app metadata, sync batches, location points, and device status.
- The backend has mobile ingest, session interpretation, quality checks, summary, and timeline endpoints.
- The web client has a mobile records page with date/device filters, metric cards, app ranking, timeline, sync batches, and quality status.
- App catalog metadata exists, but missing or weak metadata still causes package-name-heavy UI.

This redesign turns `手机记录` from a raw event list into a full analytics workbench.

## Goals

- Default to a readable, useful overview instead of raw timeline cards.
- Show total phone use, trends, category breakdowns, top apps, heatmaps, anomalies, and goals.
- Support `今天 / 7天 / 30天 / 自定义` range shortcuts, defaulting to the last 7 days.
- Use Beijing time (`Asia/Shanghai`) as the default calendar and heatmap timezone.
- Classify apps with both human-friendly life categories and raw Android metadata.
- Let the user correct app names and categories globally for their account.
- Replace the hardcoded first-500 timeline behavior with paginated, drill-down timeline APIs.
- Keep raw events traceable for diagnostics while defaulting the UI to aggregated summaries.
- Hide system noise and 0-1 second transition events by default without deleting them.
- Make chart trustworthiness explicit through quality flags and stale/rebuild states.

## Chosen Approach

Use **analytics APIs plus derived aggregation caches**.

Raw events, sessions, fallback summaries, app metadata, sync batches, and quality diagnostics remain the fact source. New derived data supports fast, readable analytics:

- App classification and user overrides.
- Hour/day/app/category aggregation for charts and heatmaps.
- Timeline blocks derived from sessions for readable drill-down.
- Insight records or computed responses for goals, anomalies, comparisons, and suggestions.

This is preferred over frontend-only aggregation because the requested experience includes date ranges, pagination, Beijing-time grouping, category correction, goals, anomalies, and drill-down. Those need consistent backend semantics and will not be robust if every chart reprocesses raw timeline rows in the browser.

## User Experience

`手机记录` becomes an analytics workbench. On first load it shows the last 7 days in Beijing time. The top controls include:

- Range shortcut: `今天`, `7天`, `30天`, `自定义`.
- Device selector.
- Category selector.
- App selector/search.
- Data source selector: events, fallback, mixed.
- Toggle: show system and short events.
- Refresh.
- Quality status.

The first screen is organized from aggregate to detail:

1. Insight summary strip.
2. Usage heatmap.
3. Charts and analysis panels.
4. Double-layer timeline.
5. App catalog and rule management.

### Insight Summary

The summary strip shows:

- Total foreground usage.
- Daily average.
- Change versus the previous comparable period.
- Highest-use day.
- Peak hour.
- App count.
- Switch or pickup count.
- Data completeness.
- Quality issue count.
- Goal and limit progress.

### Heatmap

The default heatmap is 7 days by 24 hours. Clicking a day or hour drills into 15-minute or 30-minute buckets for that selected window. Heatmap values honor active filters such as category, app, device, source, and system-noise visibility.

### Charts

The first version includes:

- Category share.
- Top app ranking.
- Daily total duration trend.
- Hour-of-day distribution.
- Switch or pickup count trend.
- Category trend.
- Week-over-week and previous-period comparisons.
- Goal and limit progress.
- Anomaly detection.
- Usage suggestions.

Chart interactions update the shared filter state. Clicking a category filters the workbench to that category. Clicking a top app filters to that app. Clicking a heatmap cell narrows the range. Clearing filters returns to the selected range.

### Timeline

The timeline is double-layered:

- Default layer: readable time blocks, grouped by time and dominant life category. Example: `20:00-21:00 社交沟通 42 分钟`.
- Session layer: expanding a block shows app sessions inside it.
- Event layer: expanding a session shows raw `UsageEvents` or fallback `UsageStats`, package name, confidence, source, timestamps, and quality flags.

Timeline blocks are paginated. The page no longer loads or displays the first 500 raw rows as the primary experience.

### System Noise

System desktop, launchers, input methods, system UI, quick search boxes, and 0-1 second transition events are folded away by default. They can still count toward diagnostics and can be shown with `显示系统与短事件`. The UI must make this mode visible so users understand why raw event counts differ from the readable timeline.

## Classification Model

The UI uses a human-friendly life category as the primary grouping. Raw Android category, package name, installer, system flag, and app metadata stay available for debugging and rule logic.

Initial life categories:

- 社交沟通
- 短视频/娱乐
- 游戏
- 音乐/音频
- 阅读/资讯
- 学习
- 工作/生产力
- 工具/系统
- 浏览器/搜索
- 出行/地图
- 购物/外卖
- 金融/支付
- 健康/运动
- 相机/创作
- 生活服务
- 未分类

Classification sources are applied in this order:

1. User global app override by exact package name.
2. User global category rule by exact package name.
3. User global category rule by package prefix.
4. User global category rule by keyword.
5. User global category rule by Android/system category or installer/source.
6. Built-in package rules for common apps.
7. Built-in prefix and keyword rules.
8. Android metadata category mapping.
9. `未分类`.

Manual corrections are global to the current user. If the user renames `com.tencent.mobileqq` to `QQ` and classifies it as `社交沟通`, all devices for that user use the same display and category.

## Backend Data Design

Keep the existing raw tables and add derived tables or equivalent persistent projections.

### App Catalog Overrides

Store user-global app display and classification overrides:

- User id.
- Package name.
- Display name override.
- Life category.
- Is system noise.
- Hide short events by default.
- Notes or rule source.
- Created and updated timestamps.

The existing device-specific app catalog remains the source for Android-provided display name, version, installer, system flag, system category, install time, update time, and raw metadata.

### Category Rules

Store user-global category rules:

- User id.
- Rule type: exact package, package prefix, keyword, Android category, installer/source.
- Pattern.
- Life category.
- Display-name override if applicable.
- System-noise flag if applicable.
- Priority.
- Enabled flag.
- Created and updated timestamps.

Rules should be deterministic. If two enabled rules match, higher priority wins; ties use most recent update, then stable id ordering.

### Aggregation Cache

Generate hour-level and day-level aggregates in Beijing-time calendar windows while storing timestamps in UTC:

- User id.
- Device id or all-device rollup marker.
- Window start UTC.
- Window end UTC.
- Timezone, default `Asia/Shanghai`.
- Granularity: hour, day, 30 minutes, or 15 minutes.
- Package name.
- Display name snapshot.
- Life category.
- Source: events, fallback, mixed.
- Foreground seconds.
- Session count.
- Launch count.
- Switch or pickup count when available.
- Is system noise.
- Short-event seconds.
- Quality flags JSON.
- Generated timestamp.

Aggregation must be recomputable from raw facts. Classification changes should mark affected windows stale or enqueue recomputation for the affected package and date range.

### Timeline Blocks

Timeline blocks are derived from sessions. A block has:

- User id.
- Device id.
- Block id.
- Start and end UTC.
- Beijing local date label.
- Dominant life category.
- Foreground seconds.
- Session count.
- App count.
- Top apps.
- Source mix.
- Quality flags.
- Includes system noise flag.

The block builder groups adjacent or overlapping app sessions into readable windows. It should avoid exploding a busy day into hundreds of cards while preserving drill-down to sessions and raw events.

## API Design

Keep old `/api/v1/mobile/summary` and `/api/v1/mobile/timeline` for compatibility. The redesigned page uses new analytics endpoints.

Common query parameters:

- `rangeStartUtc`
- `rangeEndUtc`
- `timezone`, default `Asia/Shanghai`
- `deviceId`
- `category`
- `packageName`
- `source`
- `includeSystemNoise`, default `false`
- `minDurationSeconds`, default excludes 0-1 second noise from readable views

### `GET /api/v1/mobile/analytics/overview`

Returns:

- Selected range and timezone.
- Total foreground seconds.
- Daily average.
- Previous-period comparison.
- Highest-use day.
- Peak hour.
- App count.
- Switch or pickup count.
- Completeness and quality summary.
- Goal and limit progress.
- Anomaly summary.
- Suggestions.
- `generatedAt` and `isStale`.

### `GET /api/v1/mobile/analytics/heatmap`

Returns bucketed heatmap data. Supports `granularity=hour|30m|15m|day`. Default is `hour`. The response includes bucket start/end, local label, foreground seconds, category/app breakdown if requested, and quality flags.

### `GET /api/v1/mobile/analytics/charts`

Returns:

- Category share.
- Category trend.
- Daily total trend.
- Hour-of-day distribution.
- Top apps.
- Switch or pickup trend.
- Previous-period comparison series.
- Goal markers.

The response should be shaped for frontend rendering without requiring the browser to regroup raw sessions.

### `GET /api/v1/mobile/analytics/timeline-blocks`

Returns paginated timeline blocks:

- `items`
- `nextCursor`
- `hasMore`
- Effective filters
- `generatedAt`
- `isStale`

Ordering is descending by start time by default, with an option for ascending if needed.

### `GET /api/v1/mobile/analytics/timeline-blocks/{blockId}/sessions`

Returns app sessions for a selected block:

- Session id.
- Package name.
- Resolved display name.
- Life category.
- Start/end.
- Duration.
- Source.
- Confidence.
- Quality flags.
- Whether it was hidden as system or short noise in the default view.

### `GET /api/v1/mobile/analytics/sessions/{sessionId}/events`

Returns raw event drill-down:

- Event id.
- Package name.
- Class name.
- Event type.
- Timestamp UTC.
- Beijing local timestamp.
- Source window.
- Raw JSON.
- Quality flags.

For fallback sessions, return fallback summary details and the reason raw events are unavailable.

### `GET /api/v1/mobile/apps/catalog-overrides`

Returns user-global overrides and current resolved metadata for matching packages.

### `PUT /api/v1/mobile/apps/catalog-overrides`

Creates or updates a user-global app override. Updating display name, life category, or noise flags marks affected analytics stale.

### `GET /api/v1/mobile/apps/category-rules`

Lists user-global rules.

### `POST /api/v1/mobile/apps/category-rules`

Creates a rule and marks affected analytics stale.

### `PUT /api/v1/mobile/apps/category-rules/{ruleId}`

Updates a rule and marks affected analytics stale.

### `DELETE /api/v1/mobile/apps/category-rules/{ruleId}`

Disables or deletes a rule and marks affected analytics stale.

## Android Metadata Requirements

Android should keep uploading metadata for every package involved in uploaded windows. Metadata collection should include:

- Package name.
- Display label from `loadLabel`, falling back to package name only if empty or unavailable.
- Version name and code.
- System app flag.
- Android category when available.
- Installer package name.
- First install time.
- Last update time.
- Raw JSON with any extra platform fields.

The server should not depend on Android metadata being complete. Missing metadata falls back to user rules, built-in rules, Android category mappings, and finally the package name with a `missing-metadata` quality flag.

## Quality and Trust

Quality shifts from a standalone panel to a property of every analytics response.

Overview quality includes:

- UsageEvents coverage.
- Fallback foreground seconds and share.
- Missing metadata app count.
- System-noise share.
- Short-event share.
- Failed or partial sync batch count.
- Last sync time.
- Stale aggregate state.
- Possible timezone boundary warnings.

Chart buckets, timeline blocks, sessions, and raw events may include:

- `fallback-only`
- `missing-metadata`
- `short-event-noise`
- `system-noise-hidden`
- `partial-sync`
- `stale-aggregate`
- `timezone-boundary`

When aggregates are stale but usable, the API returns the previous generated data with `isStale=true`. When rebuild is running, the UI displays old data with a clear `正在更新` state.

## Goals, Limits, Anomalies, and Suggestions

Goals and limits are analytic settings, not enforcement:

- Total daily phone limit.
- Per-category daily limit.
- Per-app daily limit.
- Optional weekly average target.

Overview returns current progress, remaining time, over-limit status, and trend.

Initial anomaly rules:

- Total duration changed sharply versus previous comparable period.
- A category increased sharply.
- Night usage is high.
- A continuous session is unusually long.
- Switching or pickup count is unusually high.
- An app enters Top N for the first time in the selected comparison period.
- A visible data gap or fallback-only period affects charts.

Suggestions must be evidence-based and drillable. Example: `过去 7 天短视频/娱乐集中在 22:00 后，较上一周期增加 38%。` The suggestion links to the relevant heatmap range, category chart, or timeline block.

## Frontend Components

`MobileAnalyticsPage`

- Owns shared filter state.
- Sets default range to last 7 Beijing-time days.
- Coordinates data queries and refresh.

`MobileAnalyticsHeader`

- Range shortcuts.
- Custom range.
- Device selector.
- Refresh button.
- Quality status.

`MobileInsightStrip`

- Total duration.
- Daily average.
- Comparisons.
- Peak day/hour.
- App count.
- Switch or pickup count.
- Data completeness.
- Goal progress.

`MobileUsageHeatmap`

- Default hourly heatmap.
- Drill-down to 15-minute or 30-minute buckets.
- Click-to-filter interactions.

`MobileChartsGrid`

- Category share.
- Top apps.
- Daily trend.
- Hour distribution.
- Category trend.
- Switch or pickup trend.
- Comparison charts.
- Goal markers.

`MobileTimelineBlocks`

- Cursor pagination.
- Block summary rows.
- Expand sessions.
- Expand raw events.
- System and short-event visibility mode.

`MobileAppCatalogManager`

- App search.
- Display-name override.
- Category override.
- Noise flag.
- Rule list and editor.

`MobileAnomalyPanel`

- Anomalies.
- Suggestions.
- Links that apply filters or open drill-down views.

## Backend Services

`MobileAnalyticsQueryService`

- Applies range, timezone, device, category, app, source, and noise filters.
- Provides shared query helpers for all analytics endpoints.

`MobileUsageAggregationService`

- Builds and refreshes hour/day/app/category aggregates.
- Marks aggregates stale after ingest or classification changes.
- Rebuilds affected windows from raw sessions and fallback summaries.

`MobileTimelineBlockService`

- Builds readable timeline blocks from sessions.
- Supports paginated block queries.
- Resolves session and raw event drill-down.

`MobileAppClassificationService`

- Resolves display name, life category, and system-noise flags.
- Merges Android metadata, user overrides, user rules, built-in rules, and fallback labels.

`MobileUsageInsightService`

- Computes comparisons, goal progress, anomalies, and suggestions.

`MobileUsageQualityService`

- Extends existing diagnostics with per-range and per-bucket quality flags.

## Error Handling

- If UsageEvents are unavailable, analytics falls back to UsageStats summaries and marks affected data as `fallback-only`.
- If app metadata is missing, show the package name with `missing-metadata`; do not block charts.
- If aggregates are stale, return existing aggregate data with `isStale=true`.
- If rebuild is running, return previous data plus a rebuild state.
- If no data exists for the selected range, show an empty state with sync and permission guidance.
- If the requested range is large, charts still use aggregate data; raw events remain paginated and drill-down-only.
- If timezone is missing, default to `Asia/Shanghai`.
- If timezone is invalid, reject the request with a validation error and keep the UI on the previous valid selection.

## Migration and Backfill

Implementation should:

1. Add schema for overrides, rules, aggregates, and timeline blocks or equivalent projections.
2. Backfill aggregates for existing mobile sessions and fallback summaries.
3. Apply built-in classification rules during backfill.
4. Mark old app catalog rows as raw metadata, not user overrides.
5. Keep old endpoints operational until the new page is fully switched.
6. Provide a way to rebuild a user/date range after rule edits or ingest fixes.

## Testing Strategy

Backend tests:

- Default last-7-days range in Beijing time.
- Beijing-time day boundaries from UTC events.
- Hour heatmap and 15/30 minute drill-down buckets.
- Category rule priority.
- User-global app overrides.
- Metadata fallback to package name.
- Built-in classification fallback.
- System-noise folding.
- 0-1 second short-event filtering.
- Cursor pagination for timeline blocks.
- Session drill-down and raw event drill-down.
- Classification change marks aggregates stale or triggers recomputation.
- Fallback-only quality flags.
- Missing-metadata quality flags.
- Previous-period comparison.
- Goal and limit progress.
- Each anomaly rule.

Frontend tests:

- Default range renders as 7 days.
- `今天 / 7天 / 30天 / 自定义` shortcuts update queries.
- Chinese life categories render correctly.
- Heatmap click updates filters.
- Category chart click updates filters.
- Top app click updates filters.
- Timeline block expands to sessions.
- Session expands to raw events.
- Pagination loads more blocks.
- System and short-event toggle changes query params.
- App override form submits and updates display.
- Rule management creates, edits, and deletes rules.
- Stale/rebuild/empty/error states render clearly.

Verification commands for implementation:

- `dotnet test Pim.sln`
- `npm --prefix src/client-web run build`
- Add focused `tests/client-web` component and type tests for new API shapes and UI interactions.

## Acceptance Criteria

- Opening `手机记录` defaults to last 7 Beijing-time days.
- The first screen shows overview, heatmap, charts, and readable timeline blocks.
- The page no longer depends on the first 500 raw timeline rows for readability.
- App names use Android labels, user overrides, built-in rules, or package-name fallback with a visible quality flag.
- User app-name and category corrections apply globally to that user across devices.
- The life category list matches the confirmed initial set.
- System and short events are hidden by default and visible when toggled.
- Heatmap supports hourly overview and 15/30 minute drill-down.
- Timeline supports block -> session -> raw event drill-down.
- Charts include category share, Top App, daily trend, hour distribution, category trend, switch/pickup trend, comparisons, goals, anomalies, and suggestions.
- Quality flags explain fallback, missing metadata, stale aggregates, partial sync, and hidden noise.
- Old mobile summary and timeline endpoints remain compatible during transition.
