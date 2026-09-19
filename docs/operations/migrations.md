# PIM Database Migrations

## Rules

- Ordinary schema changes use EF Core migrations.
- `Program.cs` runs migration adoption and then `Database.Migrate()`.
- PC Tracker idempotent SQL remains only for special compatibility SQL, special indexes, or future partition-style setup.
- Do not add new ordinary business tables through ad hoc startup SQL.

## Add A Migration

```powershell
dotnet ef migrations add <Name> --project src\Pim.Infrastructure --startup-project src\Pim.Api --context PimDbContext --output-dir Data\Migrations
```

## Apply Migrations Locally

```powershell
dotnet ef database update --project src\Pim.Infrastructure --startup-project src\Pim.Api --context PimDbContext
```

## Existing Development Databases

Databases previously created by `EnsureCreated()` are adopted by `PimMigrationAdoptionService`.

The service marks `20260524000000_BaselineExistingSchema` as already applied when it finds the existing `users` table and no EF migrations history table. After that, normal migrations apply only the changes after the baseline.

## Fresh Databases

Fresh databases run all migrations from the baseline onward.

## Snapshot Sync And Runtime-Owned Objects

Some tables and columns are owned by the runtime `PcTrackerSchemaInitializer`
(idempotent `CREATE TABLE IF NOT EXISTS` / `ADD COLUMN IF NOT EXISTS` on every
startup, after migrations), not by EF migrations:

- `pc_tracker_events` and its `browser`/`instance_id` columns, `pc_tracker_health`
  `site_*` columns, `pc_activity_classifications` `app_name`/`app_display_name`/
  `window_title`, `pc_activity_classification_settings.daily_productive_hours_goal`
- whole tables `pc_browser_site_daily` / `pc_browser_site_tick` / `pc_browser_site_meta`
  and `pc_suggestion_feedback`

Because these entities exist in the EF model, a plain `dotnet ef migrations add`
will try to absorb them into a new migration. Applying that migration on an
existing database crashes at startup (duplicate column/table, #271). Never keep
generated DDL for these objects.

If the model snapshot drifts (entities changed without regenerating the snapshot):

1. Generate the migration as usual, then empty `Up()`/`Down()` completely while
   KEEPING the generated `Designer.cs` and `PimDbContextModelSnapshot.cs`.
   Precedents: `20260906132048_AddDaemonHeartbeatsUniqueIndex` (#271),
   `20260919110327_SyncPcTrackerModelSnapshot` (#320).
2. Before pushing, verify `dotnet ef migrations has-pending-model-changes`
   reports no changes, and that the guard tests pass:
   `MigrationSnapshotSyncTests` (snapshot vs model zero diff, same differ path as
   `dotnet ef`) and `MigrationGuardedDdlTests.Issue320Migration_StaysEmpty`.

