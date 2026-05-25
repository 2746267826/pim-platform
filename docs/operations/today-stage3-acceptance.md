# Today Stage 3 Acceptance

## Scope

Stage 3 turns `/today` into a server-backed section surface.

This stage does not implement:

- Quick notes.
- Plan-vs-reality deviation analysis.
- Daily review or scheduling suggestions.

## API Checks

- `GET /api/v1/today/sections?date=YYYY-MM-DD` returns the section registry.
- `GET /api/v1/today/sections/calendar.schedule?date=YYYY-MM-DD` returns the schedule section.
- `GET /api/v1/today/sections/calendar.tasks?date=YYYY-MM-DD` returns the task section.
- `GET /api/v1/today/sections/pc.activity?date=YYYY-MM-DD` returns the PC activity section.
- `GET /api/v1/today/sections/pc.quality?date=YYYY-MM-DD` returns the PC quality section.
- `GET /api/v1/today/sections/operations.health?date=YYYY-MM-DD` returns the health section.
- `GET /api/v1/today/sections/pc.classification_suggestions?date=YYYY-MM-DD` returns the classification suggestion section.
- An unknown section id returns 404.

The registry and section responses must not contain server-owned UI title, layout, order, priority, column, or card-size fields.

## Web Checks

- Open `/today`.
- Confirm the page shows schedule, task attention, PC activity, PC quality, system health, and classification suggestion sections.
- Confirm Web controls Chinese titles and layout.
- Confirm a section failure is shown inside that section without breaking the rest of the page.
- Confirm the schedule section links to `/calendar`.
- Confirm the task section links to `/tasks` or `/calendar`.
- Confirm PC sections link to `/pc-tracker`.
- Confirm health links to `/status`.

## Data State Checks

- Use a day with no PC data and confirm PC activity or quality shows an empty or unavailable state.
- Stop or stale the Windows daemon heartbeat and confirm the health section calls attention to it.
- Create a pending classification suggestion and confirm the suggestion count appears on Today.
- Complete all tasks and confirm the task section does not show overdue or due warning pressure.

## Verification Commands

```powershell
dotnet test Pim.sln
npm --prefix src/client-web run build
npm --prefix src/client-web run test:today
```
