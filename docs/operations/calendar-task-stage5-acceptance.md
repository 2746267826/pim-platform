# Calendar And Task Stage 5 Acceptance

## Scope
Stage 5 verifies the calendar and task planning loop.

This stage includes:
- Task and event management hardening.
- Planned task time ranges.
- Outlook-compatible ICS import reports.
- Meeting metadata preservation without meeting workflow.
- Soft-delete recycle bin.
- Strict delete confirmation.
- Restore conflict handling.
- Today calendar/task section refinement.

This stage does not include:
- Outlook two-way sync.
- Meeting accept, reject, tentative, RSVP, attendee state sync, or meeting updates.
- Automatic scheduling algorithm workflow.
- Plan-vs-PC-activity matching.
- Permanent deletion.
- MCP server exposure.

## API Checks
- `GET /api/v1/calendar/tasks?search=...&page=1&pageSize=50` returns paged tasks.
- `POST /api/v1/calendar/tasks/{id}/plan` plans a task without creating an event.
- `POST /api/v1/calendar/tasks/batch-update` updates selected tasks.
- `DELETE /api/v1/calendar/tasks/{id}` moves a task to the recycle bin.
- `POST /api/v1/calendar/tasks/batch-delete` moves selected tasks to the recycle bin.
- `POST /api/v1/calendar/calendars/{id}/delete-preview` returns child impact for calendar and task books.
- `DELETE /api/v1/calendar/calendars/{id}` moves a book and active children to the recycle bin.
- `GET /api/v1/calendar/recycle-bin` lists deleted calendar/task objects.
- `POST /api/v1/calendar/recycle-bin/{type}/{id}/restore-preview` reports conflicts.
- `POST /api/v1/calendar/recycle-bin/{type}/{id}/restore` restores or restores as copy.
- `POST /api/v1/calendar/import-ics` returns imported/skipped counts and skipped reasons.
- `GET /api/v1/calendar/export-ics` excludes deleted events.

## Web Checks
- `/calendar` supports dragging an unscheduled task to the calendar; the task is planned and no event is created.
- `/tasks` supports inbox, today, planned, high priority, and completed filters.
- Strict dialogs mention the recycle bin for batch task delete and single event delete.
- Settings -> Recycle Bin restores a deleted task.
- Deleting a non-empty calendar or task book shows child impact; restoring the book restores same-operation children.
- Deleting an event and then creating the same event succeeds; restoring the old event shows conflict handling.
- Importing Outlook ICS with normal, all-day, recurring, and meeting fields reports counts; meeting context is read-only.
- Today events and planned tasks are distinct and clickable.

## Verification Commands
```powershell
dotnet test Pim.sln
```

```powershell
npm --prefix src/client-web run build
```

```powershell
npm --prefix src/client-web exec tsx -- tests\client-web\calendarApiPath.test.ts
```

```powershell
npm --prefix src/client-web exec tsx -- tests\client-web\recycleBinApiPath.test.ts
```

```powershell
npm --prefix src/client-web exec tsc -- -p tests\client-web\tsconfig.calendar-stage5.json
```

```powershell
npm --prefix src/client-web exec tsx -- tests\client-web\confirmActionDialogModel.test.ts
```
