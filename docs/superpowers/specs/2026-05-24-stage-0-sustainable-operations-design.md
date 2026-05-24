# PIM Stage 0 Sustainable Operations Design

## Purpose

Stage 0 makes PIM safe to evolve for a long time. The goal is not a platform rewrite. The goal is to make upgrades, health visibility, auditability, confirmations, background work, logs, and recovery guidance reliable before later features add more data and automation.

The accepted scope is the complete Stage 0 direction from `docs/plan.md`, with these user choices:

- Database upgrades use a formal migration mechanism.
- Health status is a system-level overview, not only an API ping.
- Audit logs and operation confirmations are shared foundations.
- Background jobs use Hangfire as a mature queue and scheduler.
- Web adds a left navigation item named "状态信息".
- The sidebar top shows an overall status indicator.
- Backup and restore are documented first; scripts and automatic backups are out of scope for this stage.
- Logs use unified structured JSON Lines.

## Current Context

The project already has useful pieces:

- API, Web, and Windows daemon default toward `http://127.0.0.1:5858`, with tests for the daemon default.
- API uses `ApiResponse<T>` and an exception middleware.
- API uses Serilog, but the file sink currently writes plain text.
- `Program.cs` creates schema with `EnsureCreated()`.
- PC Tracker has an idempotent SQL initializer for its current tables.
- Calendar has a module-local `pending_confirmations` model.
- Windows daemon has local configuration and a status window.

Stage 0 should preserve usable modules while turning these pieces into shared, long-lived foundations.

## Non-Goals

Stage 0 does not build the formal MCP server.

Stage 0 does not build a complex approval or permissions platform.

Stage 0 does not rewrite Calendar or PC Tracker business logic.

Stage 0 does not implement full Android status integration.

Stage 0 does not implement automatic backups, retention policy, or restore scripts.

Stage 0 does not turn PIM into an operations monitoring product. Status information must stay useful and quiet.

## Architecture

Use a shared infrastructure foundation:

- `Pim.Core` defines shared enums, DTOs, and service interfaces for health, audit, confirmations, and background jobs.
- `Pim.Infrastructure` implements EF entities, migrations, health contributors, audit services, confirmation services, Hangfire integration, and logging configuration.
- `Pim.Api` exposes stable API endpoints for status, confirmations, audit lookup, and daemon heartbeats.
- Windows daemon reports health and sync state to the API.
- Web displays the server-computed status. It does not duplicate health interpretation logic.

This keeps the service side as the system brain. Web shows state and collects user actions. Daemon reports sensor and upload status.

## Database Migration Strategy

Replace the API-level `EnsureCreated()` startup path with EF Core migrations.

The first Stage 0 migration should establish the currently expected core schema and add the new shared Stage 0 tables. It must support both a fresh database and an existing development database that was previously created by `EnsureCreated()`. The implementation plan should explicitly choose a safe baseline/adoption strategy before applying migrations to existing data. Future entity changes must be represented by migrations.

PC Tracker's idempotent SQL initializer may remain for special cases such as compatibility columns, special indexes, and future partition-like setup. It must not become the normal path for ordinary business schema changes.

Startup behavior:

- API runs `Database.Migrate()` during startup for local and container deployments.
- Migration failure prevents the API from reporting healthy.
- Migration errors are logged with structured fields and a correlation id when available.

Documentation must state how to add a migration, how to apply migrations locally, and how to verify a fresh environment.

## Shared Data Models

### Audit Logs

Add a shared `audit_logs` table for important operations.

It records:

- actor/user id when available
- actor type, such as user, daemon, system, or future MCP
- action name
- resource type and resource id
- source, such as web, daemon, job, or future MCP
- result, such as success, failure, rejected, or pending confirmation
- ip address and user agent when available
- correlation id
- metadata JSON
- error code and error message when available
- creation time

Stage 0 requires explicit audit calls for important operations. It does not require auditing every HTTP request as a database row.

### Operation Confirmations

Add a shared `operation_confirmations` table and service. It becomes the reusable safety gate for risky or automated operations.

Statuses must include:

- `pending`
- `confirmed`
- `rejected`
- `expired`
- `executed`

Each confirmation records:

- operation type
- summary shown to the user
- risk level
- requester
- source
- payload JSON
- preview JSON
- expiration time
- confirmation time
- execution time
- result JSON
- audit correlation id

The existing Calendar `pending_confirmations` model should be migrated or adapted to this shared service. Future Outlook writes, AI suggestions, file operations, scheduling writes, and MCP high-risk operations must reuse this model instead of creating feature-local confirmation tables.

### Daemon Heartbeats

Add a shared `daemon_heartbeats` table for the latest daemon status by device.

The Windows daemon reports:

- device id
- daemon kind, initially `windows`
- daemon version
- configured server URL
- last successful upload time
- last attempted upload time
- recent error message
- upload queue count when known
- ActivityWatch availability
- KeyStats availability
- collection paused/running state
- raw status JSON for future fields
- received time

The server interprets old or missing heartbeat records as warning or critical status depending on age.

### Background Jobs

Use Hangfire as the mature background task foundation.

Stage 0 should configure Hangfire with PostgreSQL storage, server processing, retries, recurring jobs, and dashboard access. Hangfire is the operational source for job state and history.

PIM may also expose a thin API summary over Hangfire so Web can show background job health without embedding Hangfire internals into the main UI.

Expected future job categories include:

- AI analysis
- file indexing
- review generation
- scheduling verification
- external calendar sync
- health snapshot jobs if later needed

Stage 0 should add at least one low-risk recurring or manually triggered diagnostic job so the integration is verified end to end.

## Health System

Health is a server-computed status model with levels:

- `healthy`
- `warning`
- `critical`
- `unknown`

The aggregate status is the worst relevant status across components.

Initial components:

- API process
- database and migration state
- MinIO/storage configuration and availability
- Kopia configuration, reported as unknown or warning if not directly checkable
- Tika availability
- Windows daemon heartbeat freshness
- ActivityWatch source status from the daemon
- KeyStats source status from the daemon
- Hangfire server/storage status
- recent failed background jobs

API endpoints:

- `GET /health` remains a simple anonymous liveness endpoint.
- `GET /api/v1/status/summary` returns the aggregate status for the sidebar indicator.
- `GET /api/v1/status` returns component-level details for the "状态信息" page.
- `POST /api/v1/daemon/heartbeat` accepts authenticated daemon status reports.

The server returns clear next-step messages, such as "Windows daemon has not reported recently" or "Database migration failed."

## Web Experience

Add a left navigation item named "状态信息".

At the top of the sidebar, show a compact overall status indicator:

- green: healthy
- yellow: warning
- red: critical
- gray: unknown

The indicator includes short text, such as "正常", "有警告", "故障", or "未知". It should not dominate the interface.

The "状态信息" page shows:

- overall status
- API and database status
- storage and external dependency status
- Windows daemon status
- ActivityWatch and KeyStats status
- Hangfire/background job status
- recent important errors
- pending confirmations summary
- links or actions for refresh and relevant detail pages

Web must call the status APIs and display server interpretation. It must not recreate status rules in TypeScript.

## Windows Daemon

The daemon keeps using the default server URL `http://127.0.0.1:5858`.

Add daemon heartbeat reporting to the API. It should report on startup, on manual sync, after upload attempts, and periodically while running.

The daemon should continue working if heartbeat reporting fails. Failed heartbeat submission should be logged locally and surfaced in the daemon status window when useful, but it must not block data collection.

The daemon's local status window can keep its current role. Stage 0 focuses on making server/Web aware of daemon health.

## Structured Logs

Use JSON Lines logs for API, daemon, and background job events.

Common fields:

- timestamp
- level
- service
- event name
- correlation id
- user id when available
- device id when available
- request path or job type when relevant
- result
- duration when relevant
- exception details when present

API request logging should include method, path, status code, elapsed time, remote IP, and correlation id.

Daemon logs should include sync attempts, heartbeat attempts, collector availability, upload counts, and errors.

Hangfire jobs should log start, success, retry, and failure with job type and correlation id when available.

## Error Handling

Preserve `ApiResponse<T>` for normal API responses.

Improve error classification so callers can distinguish:

- user/input errors
- authentication or permission errors
- dependency unavailable errors
- internal errors

The status APIs should not expose sensitive exception details to Web users. Detailed errors remain in structured logs and audit metadata where appropriate.

## Backup And Restore Documentation

Write documentation for manual backup and restore.

It must cover:

- PostgreSQL data
- MinIO data
- `/data` application files
- JWT/private keys and local secrets
- Windows daemon local config
- what is not backed up by default
- how to verify a restored environment

Stage 0 does not add backup scripts or automatic backup scheduling.

## Security And Access

Status summary can be visible to authenticated users. Detailed status, audit lookup, Hangfire dashboard, and confirmation administration should require an admin role or equivalent protection.

The Hangfire dashboard must not be exposed publicly without protection. For local development it can be restricted to localhost or authenticated admin access.

Daemon heartbeat must require daemon authentication. If current daemon authentication is token-based through the existing login flow, reuse it.

## Testing And Verification

Backend verification:

- `dotnet test Pim.sln`
- migration applies on a fresh database
- migration applies on an existing development database
- `/health` returns liveness
- `/api/v1/status/summary` returns aggregate status
- `/api/v1/status` returns component details
- daemon heartbeat updates server status
- audit service records an important operation
- confirmation service supports pending, confirmed, rejected, expired, and executed
- Hangfire can persist and execute a diagnostic job

Web verification:

- `npm --prefix src/client-web run build`
- sidebar status indicator renders all four states
- "状态信息" navigation item opens the status page
- status page handles loading, healthy, warning, critical, unknown, and API error states

Daemon verification:

- daemon default server URL remains `http://127.0.0.1:5858`
- heartbeat reporting does not block collection
- failed heartbeat submission is logged
- daemon status window still loads and can check API health

Manual acceptance:

- A fresh environment starts reliably.
- An existing local database upgrades without losing expected data.
- Web shows a useful overall status from the sidebar.
- The "状态信息" page identifies at least API/database/daemon/background job status.
- A simulated daemon outage becomes visible.
- A simulated failed background job becomes visible.
- A risky operation can create a reusable confirmation record.
- An important operation can write an audit record.
- Backup and restore documentation is clear enough for a future manual restore.

## Future Hooks

Future MCP tools reuse the shared audit and confirmation services.

Future AI tasks, file indexing, review generation, and scheduling verification use Hangfire.

Future Android daemon status can report into the same daemon heartbeat model.

Future Today summaries can reference the same status APIs without owning health logic.
