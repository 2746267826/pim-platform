# PC Facts Stage 1 Acceptance

Stage 1 verifies that the PC facts layer reliably preserves facts about what happened on the computer before later classification, AI interpretation, review, or scheduling logic depends on those facts.

## Acceptance Matrix

| Roadmap requirement | Current evidence | Status | Stage 1 gap closure |
| --- | --- | --- | --- |
| Save ActivityWatch bucket metadata | `pc_aw_buckets`, `AwBucketEntity`, and `/api/v1/pc/aw/upload-complete` preserve bucket id, type, client, host, source timestamps, and raw JSON. | Satisfied | `/api/v1/pc/quality` reports missing or stale window, AFK, and web buckets. |
| Save ActivityWatch window events | `pc_aw_events` stores `event_type = 'window'`, app name, window title, timestamp, duration, bucket metadata, and raw JSON. | Satisfied | Quality checks report when no window events exist in the selected range. |
| Save ActivityWatch afk events | `pc_aw_events` stores `event_type = 'afk'` and AFK status from ActivityWatch. | Satisfied | Quality checks verify the AFK bucket is visible and include AFK records in raw detail. |
| Save browser page events | Browser page data is stored as `event_type = 'web'` from web buckets such as `web.tab.current`. | Satisfied | A missing browser page bucket is surfaced as a warning because window events can still preserve coarse activity. |
| Save source event id | `AwEventEntity.SourceEventId` maps uploaded facts back to ActivityWatch source events. | Satisfied for complete uploads | Rows without source event ids are reported as completeness issues so legacy imports can be replaced through re-upload or backfill. |
| Save raw data JSON | `AwEventEntity.DataJson`, `AwBucketEntity.DataJson`, and `KeystatsSampleEntity.RawJson` preserve source payloads. | Satisfied | Quality checks report ActivityWatch rows with missing or invalid `data_json`. |
| Save KeyStats daily compatibility data | `/api/v1/pc/keystats/upload` keeps the existing daily compatibility path. | Satisfied | Daily upload remains available while Stage 1 validation focuses on minute samples as the source for reliable deltas. |
| Save KeyStats minute snapshots | `pc_keystats_samples` and `/api/v1/pc/keystats/samples` store sampled counters, key counts JSON, app stats JSON, and raw JSON. | Satisfied | Quality checks report missing samples, latest sample timing, and sample continuity. |
| Calculate or query KeyStats minute delta | Detail queries produce input-minute records from adjacent KeyStats snapshots. | Satisfied | Quality checks report sample gaps and counter resets that can make deltas unreliable. |
| Detect collection gaps | `PcTrackerQualityService` evaluates ActivityWatch buckets, ActivityWatch events, KeyStats samples, daemon heartbeat, and interpreted timeline inputs. | Satisfied | `/api/v1/pc/quality` returns structured components, issues, and next steps for the selected date or range. |
| Support ActivityWatch backfill | The Windows daemon exposes ActivityWatch recent-history backfill. | Satisfied | Runtime checks include running recent backfill when raw detail or quality output shows missing recent ActivityWatch history. |
| Provide raw data query | `/api/v1/pc/detail?view=raw` returns source-level window, web, AFK, and input records. | Satisfied | Runbook includes a raw detail endpoint check for today's range. |
| Provide interpreted timeline query | `/api/v1/pc/detail` and `/api/v1/pc/aw/timeline` return interpreted activity views. | Satisfied | Quality output includes an interpreted timeline component that depends on raw ActivityWatch events and KeyStats samples. |
| Avoid browser window/page double counting | Browser page timeline logic explains browser windows with page events when possible. | Satisfied | Web checks require interpreted view to avoid counting the same browser activity as both page time and window time. |
| Show data quality status | `/api/v1/pc/quality`, the PC page, and the Status page expose server-owned PC facts quality. | Satisfied | Operators can see component status, issue messages, and next steps without interpreting raw tables manually. |
| Report daemon upload health | `daemon_heartbeats` and daemon upload summaries report source availability and upload errors. | Satisfied | Quality checks include daemon heartbeat details and the Status page shows upload health alongside PC facts quality. |

## Local Verification Commands

Run backend tests:

```powershell
dotnet test Pim.sln
```

Build the web client:

```powershell
npm --prefix src/client-web run build
```

Check current git state before pushing:

```powershell
git status --short --branch
```

## Manual Runtime Checks

Start supporting services:

```powershell
docker compose up -d postgres minio tika
```

Run the API:

```powershell
dotnet run --project src/Pim.Api/Pim.Api.csproj
```

Run the web client:

```powershell
npm --prefix src/client-web run dev
```

Start or restart the Windows daemon from a built Debug output, or launch it from the IDE. The daemon server URL is expected to be:

```text
http://127.0.0.1:5858
```

Check API health:

```powershell
Invoke-WebRequest -UseBasicParsing "http://127.0.0.1:5858/health"
```

Check PC facts quality for today:

```powershell
$today = Get-Date -Format "yyyy-MM-dd"
Invoke-WebRequest -UseBasicParsing "http://127.0.0.1:5858/api/v1/pc/quality?date=$today"
```

Check interpreted detail records:

```powershell
$today = Get-Date -Format "yyyy-MM-dd"
Invoke-WebRequest -UseBasicParsing "http://127.0.0.1:5858/api/v1/pc/detail?dateFrom=$today&dateTo=$today&pageSize=20"
```

Check raw detail records:

```powershell
$today = Get-Date -Format "yyyy-MM-dd"
Invoke-WebRequest -UseBasicParsing "http://127.0.0.1:5858/api/v1/pc/detail?dateFrom=$today&dateTo=$today&view=raw&pageSize=20"
```

Wait at least two minutes after starting the daemon, then check KeyStats samples:

```powershell
docker compose exec -T postgres psql -U pim -d pim -c "select count(*) as sample_count, max(sampled_at_utc) as latest_sample from pc_keystats_samples;"
```

Check ActivityWatch buckets, events, and source ids:

```powershell
docker compose exec -T postgres psql -U pim -d pim -c "select bucket_id, type, client, hostname, seen_at from pc_aw_buckets order by seen_at desc;"
docker compose exec -T postgres psql -U pim -d pim -c "select event_type, count(*) as event_count from pc_aw_events group by event_type order by event_type;"
docker compose exec -T postgres psql -U pim -d pim -c "select count(*) as rows, count(source_event_id) as rows_with_source_id from pc_aw_events;"
```

Trigger ActivityWatch recent backfill from the daemon tray menu when recent history is missing from quality output or raw detail. After backfill completes, rerun the quality, interpreted detail, raw detail, bucket, event, and source id checks.

## Web Checks

Open the web client and verify:

- PC page shows the PC facts quality panel with component status, issue messages, and next steps.
- Status page shows the PC facts quality panel alongside API, database, and daemon health.
- Detail view explains empty results with the relevant collection or quality reason when selected data is unavailable.
- Raw view shows original window, browser page, AFK, and input records when those facts exist.
- Interpreted view does not double count browser page time and browser window time for the same activity interval.

## Common Failure Handling

ActivityWatch unavailable:

- Start ActivityWatch.
- Confirm `http://127.0.0.1:5600/api/0/buckets/` opens locally.
- Run daemon manual sync and recheck `/api/v1/pc/quality?date=$today`.

Browser page bucket missing:

- Install or enable the ActivityWatch browser extension.
- Confirm a browser page bucket appears in ActivityWatch buckets.
- Continue using window records as coarse activity evidence until page-level facts appear.

KeyStats unavailable:

- Start KeyStats.
- Confirm `http://127.0.0.1:18080/api/stats/` opens locally.
- Keep the daemon running for at least two minutes, then recheck `pc_keystats_samples`.

Windows daemon heartbeat missing:

- Start the Windows daemon.
- Confirm the login token is valid.
- Confirm the server URL is `http://127.0.0.1:5858`.
- Recheck the Status page and `/api/v1/pc/quality`.

Upload failures:

- Open the daemon status window and inspect raw upload details.
- Run manual sync.
- Check `http://127.0.0.1:5858/api/v1/status` and `/api/v1/pc/quality`.
- Inspect API logs and database rows when upload errors continue after manual sync.
