# Android Tracking Phase 1 Design

## Goal

Build a complete first Android tracking slice for PIM.

The Android app should become a real visible client instead of a launcher that starts background services and exits. When the user opens it, it should automatically collect and upload Android app usage, ask the server which recent data is missing, backfill at most the last 14 days, show status and logs clearly, and allow manual GNSS-first location capture.

This phase intentionally does not implement background keepalive, background location, or a unified PC + phone page. It creates the mobile data foundation so a later page can combine PC ActivityWatch/KeyStats data with Android usage and location history.

## Existing Context

The repository already has an Android client under `src/client-android` with Room, Retrofit, WorkManager, Hilt, a foreground service skeleton, usage-stat collection, and a basic `StatusActivity`. It also has a `StatsModule` that accepts simple Android app-usage uploads through `/api/v1/stats/upload`.

That existing shape is too narrow for this phase:

- `MainActivity` starts a service, schedules periodic upload, opens the Web UI, and exits.
- The current Android collection stores coarse `UsageStats` rows but not an event timeline.
- The upload contract is too small for app metadata, gap filling, batch diagnostics, and location.
- The server has no mobile location model or Web map page.
- The current status page only treats Windows daemon heartbeat as a first-class diagnostic component.

PC tracking already has a richer pattern: raw facts are preserved, derived views are computed server-side, and Web pages present quality and classification context. The mobile design should follow that direction without coupling the phone data into the PC tracker module.

## Chosen Approach

Create a new backend module, `Pim.Module.Mobile`, and keep PC tracking focused on ActivityWatch and KeyStats.

Reasons:

- Mobile usage, mobile app metadata, manual location, and mobile sync batches are a coherent subsystem.
- PC tracker internals remain stable.
- The future unified PC + phone page can consume both modules instead of requiring this phase to migrate PC records.
- Mobile diagnostics can reuse existing authentication, heartbeat, status, audit, and Web UI conventions.

Rejected alternatives:

- Extending only `StatsModule` would keep this phase small but would blur app usage, mobile location, app catalog, and diagnostics into a legacy upload endpoint.
- Building a fully unified activity module now would be cleaner long term but would touch too much mature PC tracking surface in one phase.

## Scope

In scope:

- Android app UI with login, server settings, status, logs, usage sync, manual location, and permission guidance.
- App usage collection from Android usage access permissions.
- `UsageEvents` as the preferred raw source for app foreground/background timelines.
- `UsageStats` as a marked fallback when event data is unavailable or incomplete.
- Mobile app metadata upload: package name, display name, version, whether it is a system app, system category when available, install/update times when available, and installer/source when available.
- Server-driven gap detection and backfill up to the most recent 14 days.
- Manual location capture with GNSS priority, live quality display, automatic submit at 10m accuracy, manual submit at up to 50m accuracy, and rejection above 50m.
- Service-side data model, ingest API, query API, diagnostics, and migrations.
- Web mobile records page.
- Web historical location page with OpenStreetMap tiles.
- Status page extension for Android device health.

Out of scope:

- Background keepalive.
- Background location.
- Periodic sync while the app is closed.
- Push notifications.
- Continuous route tracking.
- Automatic place inference.
- Geofencing.
- A combined PC + phone activity page.
- Deleting or retention automation for location data.
- Offline map tile caching or bulk tile prefetching.

## Product Requirements

Opening the Android app should show a usable UI immediately.

The default screen should show:

- Logged-in user and server URL.
- Connection status.
- Usage access permission state.
- Precise location permission state.
- Sync status and progress.
- Upload queue count.
- Last attempted upload.
- Last successful upload.
- Last error, if any.
- Recent structured logs.
- Actions for immediate sync, manual location, settings, and diagnostics.

On open, after login and permission checks, the app automatically starts a foreground-in-UI sync cycle:

1. Check usage permission, network, server URL, and token.
2. Register or update the mobile device profile.
3. Ask the server for missing windows within the last 14 days.
4. Collect usage events and app metadata for the returned windows.
5. Upload batches with visible progress.
6. Report final accepted, skipped, rejected, and failed counts.

The first successful usage-access grant should trigger automatic 14-day backfill. Later app opens should only backfill the server-confirmed gaps.

The location screen should:

- Start location capture only when the user enters the screen or taps a location action.
- Prefer GNSS/GPS-quality fixes.
- Permit fused/network fixes only when their reported accuracy is good enough and the source is retained.
- Show live parameters on the page instead of using modal dialogs:
  - latitude
  - longitude
  - horizontal accuracy
  - provider/source
  - altitude when available
  - speed when available
  - bearing when available
  - timestamp
  - elapsed wait time
  - submission state
- Automatically submit when horizontal accuracy is at or below 10m.
- Allow manual submit when horizontal accuracy is at or below 50m.
- Disable submit and show an inline reason when horizontal accuracy is above 50m.

## Android Client Design

### Navigation

Use a simple Compose UI with four top-level areas:

- Status
- Usage
- Location
- Settings

Status is the default entry. The UI should be quiet and operational, closer to a desktop daemon status panel than a marketing mobile app.

### Login And Server Settings

Implement in-app login.

The app should allow the user to configure the server base URL. The default should align with the repository local API default: `http://127.0.0.1:5858`. For a physical Android phone, settings must support a LAN or public URL because device-local `127.0.0.1` is the phone itself.

The Retrofit base URL should no longer be hardcoded to a remote IP. It should be read from persisted settings and rebuilt when the setting changes.

Use the existing auth endpoints:

- `POST /api/v1/auth/login`
- `POST /api/v1/auth/refresh`

Store access and refresh tokens through the existing `TokenManager` pattern, with secure local storage where practical.

### Permission Handling

Usage access:

- Declare `android.permission.PACKAGE_USAGE_STATS`.
- Detect whether usage access is actually granted; manifest declaration alone is not enough.
- Provide an inline card that opens Android usage access settings.
- Never claim usage sync is healthy when access is missing.

Location:

- Request foreground precise location permission.
- Do not request background location in this phase.
- If only approximate location is available, show a warning and do not auto-submit unless reported accuracy still passes the 50m rule.

### Usage Collection

Use Android `UsageStatsManager` in two modes.

Preferred mode:

- Query `UsageEvents` for each requested time window.
- Preserve raw event fields:
  - package name
  - event type
  - timestamp
  - class name where available
  - configuration or shortcut data where available
  - standby bucket or related metadata where available on the platform
  - source window start/end
  - collection timestamp
- Build local provisional foreground sessions for UI display, but treat server interpretation as authoritative.

Fallback mode:

- Query `UsageStats` when event data is unavailable, empty, or clearly incomplete.
- Upload summaries with a quality marker such as `sourceKind = usage-stats-fallback`.
- The Web UI must distinguish fallback summaries from event-derived timelines.

The app should collect app metadata for any package seen in either source:

- package name
- application label
- version name
- version code
- first install time
- last update time
- whether the app is a system app
- system category when available
- installer package name when available
- icon handling can be deferred; do not upload large binary icons in this phase.

### Local Storage

Use Room for:

- pending usage events
- pending usage fallback summaries
- pending app metadata
- pending location submissions
- sync batches
- recent logs
- current device record

Local rows should carry sync status:

- pending
- uploading
- synced
- failed
- rejected

Successfully uploaded synced rows may be pruned locally after a short local retention window, but server history is the source of truth.

### Sync UI And Logs

Every sync cycle should write structured log entries visible in-app:

- timestamp
- level
- operation
- message
- optional details JSON

Examples:

- `Server gap check returned 4 windows.`
- `Collecting usage events for 2026-07-03 00:00-23:59.`
- `Uploaded 238 events; accepted 236, skipped 2.`
- `Location rejected locally: accuracy 68m exceeds 50m.`
- `Upload failed: unauthorized; refresh token expired.`

These logs should not replace server diagnostics, but they should make the app understandable when used manually.

### Heartbeat

Android should report device status through the existing `/api/v1/daemon/heartbeat` endpoint with `daemonKind = android`.

The heartbeat `statusJson` should include:

- Android brand/model
- Android version and SDK
- app version
- usage permission state
- precise location permission state
- last usage sync result
- last gap check result
- pending queue count
- location capability summary
- most recent location submission result

The existing daemon heartbeat model has `ActivityWatchState` and `KeyStatsState`, which are Windows-specific names. For Android, set those legacy source fields to `Unknown` unless a future schema generalizes them, and put mobile-specific source details in `statusJson`. The Mobile module's `mobile_sync_batches` and `mobile_location_points` tables provide the detailed diagnostic history.

## Backend Module

Create `src/modules/Pim.Module.Mobile`.

Register it as a normal `IModule`, call `PimDbContext.RegisterModuleAssembly`, and keep all mobile entities/configurations inside this module.

### Data Model

`mobile_devices`

- `id`
- `user_id`
- `device_id`
- `android_id_hash`
- `display_name`
- `manufacturer`
- `brand`
- `model`
- `android_version`
- `sdk_int`
- `app_version`
- `first_seen_at`
- `last_seen_at`
- `last_heartbeat_at`
- `metadata_json`

Unique keys:

- `(user_id, device_id)`

`mobile_app_catalog`

- `id`
- `user_id`
- `device_id`
- `package_name`
- `display_name`
- `version_name`
- `version_code`
- `is_system_app`
- `system_category`
- `installer_package_name`
- `first_install_time`
- `last_update_time`
- `first_seen_at`
- `last_seen_at`
- `metadata_json`

Unique keys:

- `(user_id, device_id, package_name)`

`mobile_usage_events`

- `id`
- `user_id`
- `device_id`
- `package_name`
- `event_type`
- `event_timestamp_utc`
- `class_name`
- `source_window_start_utc`
- `source_window_end_utc`
- `collected_at_utc`
- `raw_json`
- `quality_flags_json`
- `created_at`

Unique keys:

- `(user_id, device_id, package_name, event_type, event_timestamp_utc, class_name)`

`mobile_usage_summaries`

Used only for fallback data when event timelines are not reliable.

- `id`
- `user_id`
- `device_id`
- `package_name`
- `window_start_utc`
- `window_end_utc`
- `total_time_foreground_ms`
- `last_time_used_utc`
- `source_kind`
- `raw_json`
- `quality_flags_json`
- `created_at`

Unique keys:

- `(user_id, device_id, package_name, window_start_utc, window_end_utc, source_kind)`

`mobile_usage_sessions`

Derived by the server from `mobile_usage_events`.

- `id`
- `user_id`
- `device_id`
- `package_name`
- `start_utc`
- `end_utc`
- `duration_ms`
- `source_event_ids_json`
- `interpretation_version`
- `quality_flags_json`
- `created_at`
- `updated_at`

Indexes:

- `(user_id, device_id, start_utc)`
- `(user_id, package_name, start_utc)`

`mobile_location_points`

- `id`
- `user_id`
- `device_id`
- `recorded_at_utc`
- `submitted_at_utc`
- `latitude`
- `longitude`
- `horizontal_accuracy_meters`
- `provider`
- `source_kind`
- `altitude_meters`
- `vertical_accuracy_meters`
- `speed_meters_per_second`
- `speed_accuracy_meters_per_second`
- `bearing_degrees`
- `bearing_accuracy_degrees`
- `is_auto_submitted`
- `quality`
- `raw_json`
- `created_at`

Validation:

- reject if `horizontal_accuracy_meters > 50`
- reject invalid coordinates

Indexes:

- `(user_id, device_id, recorded_at_utc)`
- spatial indexing can be deferred unless PostGIS is already available.

`mobile_sync_batches`

- `id`
- `user_id`
- `device_id`
- `operation`
- `window_start_utc`
- `window_end_utc`
- `status`
- `requested_count`
- `accepted_count`
- `skipped_count`
- `rejected_count`
- `failed_count`
- `error_message`
- `started_at`
- `finished_at`
- `details_json`

### Gap Detection

The server owns gap detection.

The Android app sends:

- device id
- requested range
- local capability summary
- optional known local windows

The server returns windows no older than 14 days from now. Windows should be day-sized by default, but can be split into smaller windows if data is partially present or previous upload attempts failed.

Gap response examples:

- today has no events after 09:00
- 2026-07-03 has no mobile usage data
- 2026-07-01 exists only as fallback summaries, event timeline desired if available

The client should not blindly rescan 14 days on every open. It should only collect windows returned by the server after the first authorization backfill.

### Usage Session Interpretation

The server should interpret raw usage events into sessions.

Rules:

- foreground/resume-style events start a session.
- background/pause/stop-style events end the current session for that package.
- if an app switch occurs without a clean background event, close the previous foreground app at the next foreground event.
- cap suspiciously long sessions with quality flags rather than silently trusting them.
- fallback summaries are shown as summaries, not precise event sessions.

Raw usage events must remain the fact source. Sessions are a derived cache and can be recomputed.

### API Design

All endpoints require authorization unless explicitly noted otherwise.

`POST /api/v1/mobile/devices/register`

Registers or updates the mobile device profile.

`POST /api/v1/mobile/sync/gaps`

Request:

- device id
- range start/end
- client capability summary

Response:

- windows to collect
- reason for each window
- max allowed backfill range
- server time

`POST /api/v1/mobile/usage/events`

Request:

- device id
- source window
- app metadata list
- usage events list
- fallback summaries list
- client batch id

Response:

- batch id
- accepted count
- skipped count
- rejected count
- failed count
- per-error summary
- whether the client should retry

`POST /api/v1/mobile/location/points`

Request:

- device id
- recorded at
- coordinates
- accuracy and optional sensor fields
- provider/source
- auto/manual submission flag
- raw provider details

Response:

- saved point
- quality label

`GET /api/v1/mobile/summary?date=&deviceId=`

Returns daily mobile usage metrics, app rankings, quality summary, and sync status.

`GET /api/v1/mobile/timeline?date=&deviceId=`

Returns event-derived sessions and fallback summary blocks for a day.

`GET /api/v1/mobile/location/history?start=&end=&deviceId=&maxAccuracyMeters=`

Returns map-ready location points and list rows.

`GET /api/v1/mobile/quality?date=&deviceId=`

Returns mobile-specific quality components:

- usage permission/heartbeat freshness
- event coverage
- fallback-only days
- sync batch failures
- location accuracy rejections
- app metadata completeness

### Compatibility

Keep `/api/v1/stats/upload` available for any old Android client behavior until the new mobile client is fully migrated. Do not build new UI on the old `app_usage` table.

## Web UI Design

### Navigation

Add two primary navigation items:

- `手机记录`
- `历史位置`

Status page remains the system diagnostic surface and should include mobile components.

### Mobile Records Page

The page should visually echo the PC tracker page without copying PC-specific controls.

Primary controls:

- date picker
- device selector
- refresh

Top metrics:

- total foreground time
- app switch count
- number of apps used
- data completeness
- quality issue count
- last upload time

Main panels:

- daily mobile timeline
- app ranking
- category/app knowledge hints
- sync gap and batch status
- quality warnings

The page should make fallback data obvious. For example, an app block derived from `UsageStats` should not look as precise as an event-derived session.

### Historical Location Page

Use OpenStreetMap tiles for phase 1.

Constraints:

- load tiles only for the current viewport.
- do not bulk prefetch.
- do not implement offline tile caching.
- keep the map provider replaceable for later Gaode, Tencent, Tianditu, or another provider.

Page controls:

- time range
- device selector
- max accuracy filter
- show auto/manual submissions
- refresh

Map features:

- point markers
- marker clustering if point count grows
- optional line connecting points in chronological order
- selected point details

List features:

- recorded time
- submitted time
- accuracy
- provider/source
- auto/manual flag
- coordinate
- quality

### Status Page

Extend the status detail page with Android/mobile components.

Components:

- Android device heartbeat
- Mobile usage collection
- Mobile sync batches
- Mobile location capture
- Mobile app catalog metadata

The component details should include enough key/value fields to debug without querying the database:

- device id
- model
- app version
- last heartbeat
- usage permission state
- precise location permission state
- pending queue count
- last successful upload
- last failed upload
- last location submission

## Privacy And Security

Location points are sensitive. This phase uses authenticated APIs only and stores points indefinitely by default because that was explicitly chosen.

Do not log precise coordinates in server logs unless needed for a structured audit entry. App-visible logs may show coordinates on the location page, but generic sync logs should avoid repeating precise coordinates.

Use a stable PIM device id for sync. Android ID should be hashed before upload or stored only where needed for device reconciliation.

Do not upload app icons or unnecessary binary assets in this phase.

## Error Handling

Android client:

- Missing usage permission: show inline action to open settings; skip usage sync.
- Missing location permission: show inline action; disable location capture.
- Server unavailable: keep local pending queue; show last error and retry action.
- Login expired: attempt refresh; if refresh fails, show login-required state.
- Upload partial failure: mark accepted rows synced, keep retryable failures pending, show rejected rows separately.
- Location accuracy over 50m: reject locally and show inline reason; server also rejects if called.

Server:

- Validate every coordinate and accuracy value.
- Make usage event ingest idempotent.
- Return detailed batch counts.
- Keep raw events even if session interpretation fails.
- Mark derived session quality issues rather than hiding suspicious data.
- Reject backfill windows older than the 14-day limit.

## Testing

Backend tests:

- Mobile device registration upserts by user and device id.
- Usage event upload is idempotent.
- App catalog metadata upserts by package name.
- Gap detection returns no windows older than 14 days.
- First missing day returns a backfill window.
- Existing complete day does not return a duplicate gap.
- Fallback summaries are stored separately from raw events.
- Usage sessions are derived from foreground/background events.
- Suspicious sessions receive quality flags.
- Location point with accuracy `50m` is accepted.
- Location point with accuracy greater than `50m` is rejected.
- Mobile summary returns event-derived and fallback data with source markers.
- Mobile quality reports stale heartbeat, failed sync batches, and fallback-only days.

Android tests:

- Login screen saves tokens and server URL.
- Status screen reports missing usage permission.
- Opening the app triggers gap check when logged in.
- Gap windows drive collection rather than a blind 14-day scan.
- Usage event collection stores pending rows.
- Upload success marks rows synced and logs counts.
- Location UI disables submit above 50m.
- Location UI auto-submits at or below 10m.
- Manual submit works at or below 50m.

Web tests:

- Sidebar routes include mobile records and historical location.
- Mobile records page renders summary, timeline, app ranking, and quality panels.
- Fallback usage blocks are visually distinguishable.
- Location page renders map and list from API data.
- Location filters update query parameters.
- Status page renders Android/mobile diagnostic components.

Manual verification:

- Install the Android app on a device or emulator with usage access.
- Log in to the local or configured server.
- Open the app and confirm it asks the server for gaps.
- Confirm first authorization triggers backfill up to the last 14 days.
- Confirm subsequent app opens only collect returned gaps.
- Confirm Web mobile records show app names instead of only package names.
- Enter the location screen outdoors and observe live accuracy.
- Confirm auto-submit at 10m or better.
- Confirm manual submit is disabled above 50m.
- Confirm historical location appears on the Web map and list.
- Confirm Status page shows Android heartbeat and recent mobile sync information.

## Completion Definition

This phase is complete when:

- Android has a real UI and no longer exits immediately after startup.
- The app supports in-app login and configurable server URL.
- Opening the app automatically syncs usage data when permissions and login are valid.
- Server-driven gap fill works up to the most recent 14 days.
- Usage event timelines are uploaded when available, with marked fallback summaries when not.
- App metadata is uploaded and used by Web UI.
- Manual location capture shows live quality parameters.
- Location auto-submit at 10m and manual submit at up to 50m both work.
- Server rejects location points above 50m accuracy.
- Web has mobile records and historical location pages.
- Status page includes mobile diagnostics.
- No background keepalive or background location behavior is introduced.
- PC tracker remains functionally independent from the new Mobile module.

## References

- Android `UsageStatsManager`: https://developer.android.com/reference/android/app/usage/UsageStatsManager
- Android location permissions: https://developer.android.com/develop/sensors-and-location/location/permissions
- OpenStreetMap tile usage policy: https://operations.osmfoundation.org/policies/tiles/
