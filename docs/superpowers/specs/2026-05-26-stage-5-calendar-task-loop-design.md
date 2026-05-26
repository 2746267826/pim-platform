# Stage 5 Calendar And Task Loop Design

## Goal

Stage 5 turns the existing Calendar module into a reliable personal planning loop.

The project already has calendars, tasks, events, ICS import/export, task drag-to-calendar behavior, a task page, a calendar page, and a calendar data manager. This stage is not a rewrite. It strengthens the existing surface so tasks and events can be managed long term: create, edit, search, filter, import, export, delete safely, audit important writes, restore from a recycle bin, and show the daily plan clearly on Today.

The implementation should keep the server as the business owner. Web presents, edits, confirms, and visualizes. Future MCP tools should be able to wrap the same APIs without duplicating calendar or task rules.

## Accepted Decisions

- Use the existing Calendar module as the foundation.
- Prioritize loop hardening over a broad model rewrite.
- Include interaction and visual refinements for the core planning flow.
- Cover `/calendar`, `/tasks`, `/settings/calendar-data`, a new recycle bin settings page, event/task editor drawers, and Today calendar/task sections.
- Keep FullCalendar as the calendar interaction engine.
- Dragging a task onto the calendar keeps it as a task and only assigns a planned time range. It does not create an event.
- Preserve soft delete for calendars, events, and tasks.
- Add a recycle bin in Settings.
- Add strict in-app delete confirmation and avoid browser `confirm()` for important delete flows.
- Audit delete and restore operations.
- Soft-deleted objects do not block recreating equivalent active objects.
- Restoring from the recycle bin checks for conflicts before making objects active again.
- Deleting a non-empty calendar or task book is allowed, but it soft-deletes the book and its active child objects as one operation.
- Restoring a deleted book restores only child objects deleted by the same book-delete operation.
- Outlook-compatible ICS import targets Outlook-exported calendar data, not full RFC 5545 coverage.
- Meeting workflow is out of scope, but meeting context from Outlook ICS must be preserved.
- Outlook/ICS import skips active duplicates by default and returns a structured import report.

## Scope

In scope:

- Calendar, event, task, and task-book loop hardening.
- Outlook-compatible ICS import/export improvements.
- All-day event support.
- Time zone handling sufficient for Outlook-exported ICS files.
- Recurrence data needed for Outlook common recurring events and recurrence exceptions.
- Preservation of meeting context without meeting workflow.
- Recycle bin query and restore APIs.
- Delete preview APIs for high-impact deletes.
- Delete operation grouping so book deletes can restore the correct child objects.
- Delete and restore audit logging.
- Task search, filtering, pagination, and batch operations.
- Event search, filtering, pagination, batch delete, and restore.
- Calendar and task book delete/restore flows.
- Today calendar/task section refinements.
- Focused backend and frontend tests.
- Manual acceptance documentation.

Out of scope:

- Full RFC 5545 implementation.
- RSVP, accept, reject, tentative, attendee state synchronization, or meeting update workflow.
- Outlook or external calendar two-way sync.
- Automatic scheduling algorithm workflow.
- Plan-vs-actual PC activity matching.
- Task/event/file binding.
- MCP server implementation.
- Permanent deletion or recycle bin emptying.

## Product Model

PIM has two planning object types.

Events are time commitments or blocks. They may come from manual creation or Outlook-compatible ICS import. Events can have all-day state, time zone information, recurrence information, source metadata, and preserved Outlook meeting context.

Tasks are work items. A task may live in the inbox, a task book, or a planned time range. Dragging a task into the calendar means "I plan to work on this task then." It does not turn the task into an event.

Calendar and task books organize objects. Deleting a non-empty book is a high-risk batch operation because it affects child objects. The system should allow it only after preview and strict confirmation.

The recycle bin is a recovery and inspection surface. It is not the main workflow. Active views, Today, normal search, and ICS export hide soft-deleted objects by default.

## Data Model

Keep the existing `CalendarEntity`, `EventEntity`, and `TaskEntity` as the core tables.

### Event Additions

Add fields for Outlook-compatible import and future sync readiness:

- `is_all_day`: whether the event is an all-day event.
- `time_zone_id`: normalized PIM time zone id when known.
- `source_time_zone_id`: original time zone id from the imported ICS, such as a Windows or Outlook time zone id.
- `source`: preserve and expand current source semantics, for example `manual`, `ics`, `outlook-ics`, or later `outlook-sync`.
- `source_uid`: imported source UID when distinct from local UID policy.
- `source_ics_component`: raw or normalized raw `VEVENT` text from the source ICS.
- `external_metadata_json`: structured metadata for Outlook fields that PIM does not fully own.
- `recurrence_id`: recurrence instance id for Outlook recurrence exceptions when present.
- `exdates_json`: recurrence exclusions when present, if not modeled in a separate table.
- `recurrence_metadata_json`: recurrence exception metadata when needed.

Meeting-related fields should be preserved in `external_metadata_json`, not elevated into active meeting workflow. Useful preserved keys include:

- `method`
- `organizer`
- `attendees`
- `sequence`
- `class`
- `transp`
- `priority`
- `categories`
- `htmlDescription`
- `outlookXProperties`
- `recurrenceId`
- `exDates`

### Task Additions

Clarify planned-time semantics for tasks.

The existing `DtStart` can remain as the persisted planned start if migration risk is low, but the API and Web copy should call it "planned start." Add or clarify:

- `planned_start`: mapped from or replacing `dtstart` if a migration is chosen.
- `planned_end`: explicit planned end, or derive from `planned_start + estimated_duration` when the product deliberately uses duration.
- `estimated_duration`: keep for planning.
- `minimum_segment`: keep for future scheduling.

The Stage 5 design should make the invariant explicit: a planned task remains a task.

### Delete Tracking

Add delete operation tracking for calendars, events, and tasks:

- `deleted_by_operation_id`: nullable operation id shared by one delete action.
- `deleted_by_operation_kind`: optional text such as `single-event`, `batch-event`, `calendar-book`, `task-book`.

This allows the system to distinguish objects that were deleted together from objects that had already been independently deleted.

When deleting a non-empty calendar or task book:

- The book receives a new delete operation id.
- Active children deleted as part of that action receive the same delete operation id.
- Children already deleted before the book delete are left unchanged.

When restoring a book:

- Restore the book.
- Restore only children with the same delete operation id.
- Do not restore children that were deleted earlier by another operation.

## Server Behavior

`CalendarService` remains the main business entry point.

The server owns:

- Object state changes.
- Soft delete rules.
- Restore rules.
- Conflict checks.
- Delete previews.
- Import duplicate detection.
- Outlook-compatible parsing decisions.
- Audit logging.
- Structured operation results.

Web owns:

- Layout.
- Editing controls.
- Confirmation UI.
- Filter controls.
- Visual distinction between events and planned tasks.
- User-facing error and conflict presentation.

## Deletion And Restore

Deletion remains soft delete. Normal active views continue to filter `deleted_at is null`.

Single-object delete:

- Soft-delete the object.
- Set delete operation fields.
- Write an audit log.
- Return a structured result with object type, object id, title, and recycle-bin availability.

Batch event/task delete:

- Use strict confirmation in Web.
- Set one delete operation id across the batch.
- Write an audit log with count and representative samples.
- Return deleted count and samples.

Book delete:

- Requires a delete preview before confirmation.
- Preview returns book type, book id, book name, active child count, representative child samples, and risk level.
- Confirmed delete soft-deletes the book and active children as one operation.
- Write audit for the book operation and include affected count and samples.

Recycle bin:

- Query soft-deleted calendars, task books, events, and tasks.
- Support filters by type, deleted date, keyword, source, and book when useful.
- Show deletion time, type, title, original book, key dates, and source.
- Support restore.
- No permanent delete or empty recycle bin in Stage 5.

Restore:

- Check conflicts before restoring.
- If no conflict, clear `deleted_at` and relevant delete operation fields.
- If restoring a book, restore only same-operation children.
- Write audit logs for success, conflict, cancel, and restore-as-copy.

Conflict rules:

- Events conflict with active events by same UID, same source UID, or same title plus start plus end.
- Tasks conflict with active tasks by same title plus due plus planned start.
- Deleted objects do not block create or import.
- Restore never silently overwrites active objects.

If conflict occurs, the API returns a structured conflict result. Web lets the user cancel or restore as an independent copy. Restoring as copy must generate a new local UID and clear external identifiers that would imply it is the same imported source object.

## Outlook-Compatible ICS

Stage 5 targets Outlook-exported `.ics` data. It does not target full RFC 5545 coverage.

Import should handle:

- `VCALENDAR` with one or many `VEVENT` components.
- Outlook `VTIMEZONE` blocks and common Windows/Outlook time zone identifiers.
- UTC date-times.
- `TZID` date-times.
- all-day `VALUE=DATE` events.
- folded lines and escaped text.
- Chinese and Unicode text.
- common event fields: `UID`, `SUMMARY`, `DESCRIPTION`, `LOCATION`, `DTSTART`, `DTEND`, `DTSTAMP`, `CREATED`, `LAST-MODIFIED`, `STATUS`, `CLASS`, `TRANSP`, `PRIORITY`, and `CATEGORIES`.
- common recurrence fields: `RRULE`, `EXDATE`, and `RECURRENCE-ID`.
- recurrence exceptions produced by Outlook when feasible for Stage 5.
- Outlook extension fields such as `X-MICROSOFT-CDO-*`, `X-MS-OLK-*`, and `X-ALT-DESC;FMTTYPE=text/html`.

Meeting behavior:

- Import does not execute meeting workflow.
- PIM does not accept, reject, tentatively accept, send RSVP, sync attendee state, or process meeting updates in Stage 5.
- Meeting context is preserved in `source_ics_component` and `external_metadata_json`.
- Web may show a read-only note that the event came from Outlook meeting data and PIM is preserving but not managing meeting responses.

Duplicate import:

- Only active events participate in duplicate detection.
- Duplicate checks use same UID, same source UID, then same title plus start plus end.
- Duplicates are skipped.
- Existing active events are not updated.
- Duplicates do not create copies.
- The import response includes imported count, skipped count, skipped reason categories, and representative samples.

Export:

- Export active events by default.
- Preserve PIM-owned event fields.
- Include all-day, time zone, recurrence, and relevant preserved Outlook metadata when this can be done safely.
- Do not claim to export a fully functional meeting invitation.

## API Design

Keep the existing `/api/v1/calendar` group and extend it.

Calendar and task books:

- `GET /calendars?kind=calendar|task`
- `POST /calendars`
- `PUT /calendars/{id}`
- `POST /calendars/{id}/delete-preview`
- `DELETE /calendars/{id}`
- `POST /calendars/{id}/restore`

Events:

- `GET /events` with search, calendar id, date range, source, status, page, and page size.
- `POST /events`
- `PUT /events/{id}`
- `POST /events/{id}/delete-preview` if needed for consistent confirmation.
- `DELETE /events/{id}`
- `POST /events/batch-delete-preview`
- `POST /events/batch-delete`
- `POST /events/{id}/restore`

Tasks:

- `GET /tasks` with inbox, search, task book id, status, priority, planned range, due range, page, and page size.
- `POST /tasks`
- `PUT /tasks/{id}`
- `POST /tasks/{id}/plan` for drag-to-calendar planning.
- `POST /tasks/batch-update`
- `POST /tasks/batch-delete-preview`
- `POST /tasks/batch-delete`
- `DELETE /tasks/{id}`
- `POST /tasks/{id}/restore`

Recycle bin:

- `GET /recycle-bin?type=&search=&deletedFrom=&deletedTo=&page=&pageSize=`
- `GET /recycle-bin/{type}/{id}`
- `POST /recycle-bin/{type}/{id}/restore-preview`
- `POST /recycle-bin/{type}/{id}/restore`

ICS:

- `POST /import-ics` returns a structured import report.
- `GET /export-ics` exports active events matching filters.

Important mutation responses should be richer than a plain `"deleted"` string. They should include what changed, affected ids, skipped items, conflicts, whether confirmation was required, and suggested next steps when useful.

## Web Design

The Web experience should feel like a practical planning tool: quiet, dense, and scannable.

### Calendar Page

- Keep timeline and month modes.
- Render events and planned tasks with distinct visual treatments.
- Clicking an event opens event editing/details.
- Clicking a planned task opens task editing/details.
- Dragging an unscheduled task into the calendar opens or confirms the task planning drawer.
- Planning a task writes planned task fields, not an event.
- Calendar controls should use clear labels and compact buttons.

### Task Page

Upgrade the task page into a durable work list:

- Inbox.
- Today.
- Planned.
- Overdue.
- Completed.
- Search.
- Priority filter.
- Task book filter.
- Batch selection for status changes or delete.

Task cards should show title, priority, due time, planned time, status, and task book when useful. Dense rows are preferable to oversized cards.

### Editor Drawers

Task drawer should support:

- Task book.
- Title.
- Description.
- Status.
- Priority.
- Due time.
- Estimated duration.
- Planned start and planned end or planned duration.

Event drawer should support:

- Calendar.
- Title.
- Start and end.
- All-day toggle.
- Time zone display.
- Location.
- Description.
- Recurrence summary or simple recurrence editing if in scope for implementation.
- Outlook source note when imported.
- Read-only meeting context note when applicable.

### Calendar Data Manager

Keep this page as the advanced event table, but align it with the PIM panel style.

It should support:

- Search.
- Calendar filter.
- Date filter.
- Source filter when useful.
- Import Outlook ICS.
- Export active events.
- Batch delete with strict confirmation.
- Detail view with preserved Outlook fields when applicable.

### Recycle Bin

Add a Settings entry for the recycle bin.

The recycle bin page should:

- Show deleted tasks, events, calendars, and task books.
- Filter by type.
- Search by title.
- Show deletion date.
- Show source and original book when useful.
- Open item detail.
- Restore with conflict handling.
- Explain that permanent deletion is not part of Stage 5.

### Strict Confirmation

Replace browser `confirm()` for important delete flows.

Single delete confirmation:

- Object type.
- Object title.
- "This will move the item to the recycle bin."

Batch delete confirmation:

- Count.
- Representative samples.
- "These items will move to the recycle bin."

Book delete confirmation:

- Book name.
- Book kind.
- Child count.
- Representative child samples.
- "The book and these active child items will move to the recycle bin together."

### Today Sections

Today should remain the daily entry point.

`calendar.schedule`:

- Show today's events and planned tasks.
- Let events and tasks open their respective editor/detail flows.
- Clearly distinguish event versus task.

`calendar.tasks`:

- Show overdue, due today, unscheduled, and planned tasks with clear badges.
- Empty states should guide the user to create a task or open the calendar.
- Do not duplicate scheduling or task rules in Web.

## Error Handling

- Delete preview failures should be visible before destructive confirmation.
- Delete and restore errors should keep the drawer/dialog open with a clear message.
- Restore conflicts should not be treated as generic errors; they are expected structured outcomes.
- ICS import parsing errors should report safe file-level errors and partial-import summaries when possible.
- Unsupported Outlook fields should not fail import if the event can otherwise be represented.
- Web should avoid saying "irreversible" for soft delete. It should say the item moves to the recycle bin.

## Testing

Backend tests:

- Soft delete hides calendars, events, and tasks from active queries.
- Deleted objects do not block creating equivalent new active objects.
- Single event delete writes deleted timestamp and audit.
- Single task delete writes deleted timestamp and audit.
- Batch event/task delete uses one operation id.
- Book delete soft-deletes active children with the same operation id.
- Book delete does not modify already-deleted children.
- Book restore restores only same-operation children.
- Restore detects active event conflicts by UID and by title plus start plus end.
- Restore detects active task conflicts by title plus due plus planned start.
- Restore-as-copy creates new local identity and clears external identity.
- Recycle bin list filters by type, search, and deleted time.
- Outlook ICS import handles normal events.
- Outlook ICS import handles all-day events.
- Outlook ICS import handles Outlook time zones.
- Outlook ICS import handles recurrence rules.
- Outlook ICS import preserves recurrence exception metadata.
- Outlook ICS import preserves meeting context without executing meeting workflow.
- Duplicate active imports are skipped and reported.
- Deleted duplicates do not block import.

Frontend tests:

- Calendar API paths are stable.
- Recycle bin API paths are stable.
- Calendar and task DTOs include new fields.
- Today calendar/task section types remain stable.
- Delete confirmation model formats counts and samples.
- Task planning API is used for drag-to-calendar behavior.

Verification commands:

```powershell
dotnet test Pim.sln
npm --prefix src/client-web run build
```

## Manual Acceptance

- Create a task book and a calendar.
- Create tasks and events.
- Drag a task onto the calendar and confirm it remains a task with planned time.
- Open Today and confirm planned tasks and events appear distinctly.
- Delete a single task and confirm it appears in the recycle bin.
- Delete a single event and confirm it appears in the recycle bin.
- Restore a single deleted task or event.
- Delete a non-empty calendar or task book and confirm the strict preview shows affected children.
- Restore the deleted book and confirm only same-operation children are restored.
- Delete an event, create an equivalent new event, and confirm creation succeeds.
- Attempt to restore the old event and confirm conflict handling appears.
- Import an Outlook-exported ICS file with normal events.
- Import an Outlook-exported ICS file with all-day events.
- Import an Outlook-exported ICS file with recurring events.
- Import an Outlook-exported ICS file with meeting fields and confirm context is preserved but no meeting response actions appear.
- Import the same ICS again and confirm duplicates are skipped with a report.
- Export active events and confirm deleted events are excluded.

## Future Extensibility

Stage 5 prepares for:

- Stage 6 plan-vs-actual matching using planned task time ranges.
- Stage 7 automatic scheduling writing task planned time through server APIs.
- Stage 10 external Outlook sync using preserved source metadata.
- Stage 15 MCP tools for task and event read/write through the same API surface.

The API should remain structured enough that a future MCP layer can expose low-risk task/event creation, read-only search, and high-risk delete or batch update flows with dry-run and confirmation.

## Completion Definition

Stage 5 is complete when:

- The user can manage tasks and events as a reliable planning system.
- Tasks and events can be searched, filtered, edited, and batch-managed.
- Task drag-to-calendar produces planned task time, not a new event.
- Outlook-compatible ICS import works for common Outlook-exported event data.
- Meeting context is preserved but meeting workflow is not exposed.
- Soft-deleted objects move to a recycle bin.
- Delete and restore operations are audited.
- Deleting non-empty books has strict preview and operation grouping.
- Restoring from the recycle bin checks conflicts before reactivating objects.
- Today shows the daily plan and task attention state clearly.
- Backend tests and frontend build pass for the touched surfaces.
