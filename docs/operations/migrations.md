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
