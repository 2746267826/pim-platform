# Stage 4 Quick Notes Design

## Goal

Build the Stage 4 quick notes foundation: a low-friction capture surface where the user can save text and attachments together without deciding whether the material is a task, event, reminder, document, idea, or project note.

This stage makes quick notes a durable, server-owned input stream:

- The user can capture from any Web page through a persistent floating panel.
- The user can manage notes in a dedicated Quick Notes page.
- Markdown content is the primary source of truth.
- Attachments have stable server ids and appear inside the note content flow.
- The data model leaves room for future AI understanding, MCP creation, and task/event conversion.

This stage does not implement AI classification, automatic task creation, automatic event creation, reminders, or file-system organization.

## Accepted Decisions

- Use a global bottom-right quick note button.
- Opening the button shows a persistent draggable floating panel.
- The floating panel must not close when the user clicks outside it.
- The floating panel must have an explicit close button.
- The floating panel can be dragged, and its position should persist locally.
- Add a dedicated left-sidebar route named `快速记录` for full-page capture and management.
- Text and attachments are one note experience, not separate text and attachment areas.
- Use MDXEditor as the mature Markdown editor.
- Save Markdown as the primary content source.
- Save attachment metadata in a stable attachment table.
- Images are inserted inline in the Markdown content flow.
- Non-image files are inserted into the Markdown flow as attachment links rendered by Web as file blocks.

## Scope

In scope:

- Quick note domain model and EF migration.
- Quick note attachment metadata model.
- Quick note service with user scoping and status transitions.
- Multipart attachment upload through the API.
- Authenticated attachment download.
- API endpoints for create, list, read, update, process, archive, restore, delete, upload, download, and attachment delete/removal.
- Web API client and types.
- Global quick note floating capture panel.
- Draggable panel behavior with explicit close.
- Local draft preservation for the floating panel.
- Dedicated `/quick-notes` page.
- Sidebar navigation item for quick notes.
- Full-page MDXEditor capture and edit surface.
- Inbox, processed, and archived filters.
- Search over quick note content.
- Basic attachment rendering inside Markdown content.
- Backend and frontend verification.
- Manual acceptance checklist.

Out of scope:

- AI classification or summarization.
- Automatic conversion to tasks, events, reminders, or project items.
- User-selected note ownership/project/category.
- Formal file-system integration beyond the existing storage strategy.
- Rich block JSON as the primary source of truth.
- Real-time collaboration.
- Background cleanup of orphaned uploads, except leaving a future path.
- MCP server implementation.

## Product Model

Quick notes are durable inbox records.

A quick note is a single content object with Markdown body and attached files referenced from that Markdown. The note may start as a rough thought, pasted image, dragged file, meeting note, URL, reminder-like text, or working scratchpad. The system preserves it first and interprets it later.

The user has two capture and management surfaces.

### Global Floating Capture

The global floating capture is for fast input:

- A fixed bottom-right button appears after login on all Web routes.
- Clicking it opens a compact panel.
- The panel is persistent and does not close on outside click.
- The panel has an explicit close button.
- Closing hides the panel but does not clear the current draft.
- The panel can be dragged by a header or drag handle.
- The last panel position is saved in `localStorage`.
- The panel contains a compact MDXEditor instance.
- It accepts text, pasted images, dragged images, and dragged files.
- Save creates a quick note and clears the local draft only after success.
- Save failure keeps the draft and shows an actionable error.

### Dedicated Quick Notes Page

The dedicated `/quick-notes` page is for full management:

- The sidebar adds `快速记录`.
- The page provides full-page quick note creation and editing.
- The page lists notes by status: `inbox`, `processed`, and `archived`.
- The page supports search.
- The page supports detail viewing, editing, processing, archiving, restoring, and deleting.
- Desktop layout should favor a two-pane management view: list on one side, editor/detail on the other.
- Narrow layouts may switch between list and detail/edit mode.
- Text and attachments remain one content flow in both edit and read modes.

## Editor Choice

Use MDXEditor for Stage 4.

Rationale:

- It is a mature React WYSIWYG Markdown editor.
- Markdown is the editor input and output, which matches the quick note source-of-truth requirement.
- Image paste, drag, and upload can be routed through an `imageUploadHandler` that returns a server URL.
- It keeps Stage 4 lighter than a block-JSON editor while leaving a path to richer rendering later.

BlockNote remains a future option if PIM later needs Notion-like block editing. For Stage 4, BlockNote's lossless source of truth is block JSON, which is heavier than the current requirement.

## Data Model

Add a dedicated quick notes module, for example `Pim.Module.QuickNotes`, rather than placing this logic in Calendar, Today, or PC Tracker.

### `quick_notes`

Columns:

- `id` UUID primary key.
- `user_id` UUID, required.
- `content_markdown` text, required, default empty string.
- `status` varchar, required: `inbox`, `processed`, or `archived`.
- `source` varchar, required: initially `web-floating` or `web-page`; future values may include `mcp`, `android`, or `desktop`.
- `metadata_json` json/jsonb, required, default `{}`.
- `created_at` timestamp with time zone.
- `updated_at` timestamp with time zone.
- `archived_at` timestamp with time zone, nullable.
- `deleted_at` timestamp with time zone, nullable.

Indexes:

- `(user_id, status, updated_at)` for management lists.
- `(user_id, created_at)` for recent capture.
- Optional text/search index later; Stage 4 can start with simple content search.

### `quick_note_attachments`

Columns:

- `id` UUID primary key.
- `quick_note_id` UUID, nullable for temporary uploads before note save.
- `user_id` UUID, required.
- `storage_provider` varchar, required, initially `minio`.
- `object_key` text, required.
- `file_name` text, required.
- `content_type` varchar, required.
- `size_bytes` bigint, required.
- `content_hash` varchar, nullable if hashing is deferred.
- `metadata_json` json/jsonb, required, default `{}`.
- `created_at` timestamp with time zone.
- `deleted_at` timestamp with time zone, nullable.

Indexes:

- `(quick_note_id)` for detail loading.
- `(user_id, created_at)` for temporary upload lookup.
- `(user_id, deleted_at)` for access checks and cleanup.

### Content and Attachment Relationship

Markdown is the note's primary content.

Attachments are stable server objects referenced from Markdown by authenticated API URLs. A note's attachment table records the stable attachment ids, storage keys, MIME types, and file metadata.

Images are inserted inline, for example:

```markdown
![screenshot.png](/api/v1/quick-notes/attachments/{attachmentId}/download)
```

Non-image files are inserted as Markdown links, for example:

```markdown
[proposal.pdf](/api/v1/quick-notes/attachments/{attachmentId}/download)
```

Web can render such links as file blocks, but the persisted source remains Markdown.

The service must validate that attachment references in Markdown belong to the current user and are either already bound to the note or are eligible temporary uploads by that user.

## API Design

Add `/api/v1/quick-notes` endpoints. All endpoints require authorization.

### List Notes

`GET /api/v1/quick-notes?status=inbox&search=&page=1&pageSize=30`

Returns a paged list of notes. Each item includes:

- `id`
- `contentPreview`
- `status`
- `source`
- `attachmentCount`
- `createdAt`
- `updatedAt`
- `archivedAt`

The preview is a convenience projection, not the fact source.

### Get Note

`GET /api/v1/quick-notes/{id}`

Returns:

- `id`
- `contentMarkdown`
- `status`
- `source`
- `attachments`
- `metadata`
- `createdAt`
- `updatedAt`
- `archivedAt`

### Create Note

`POST /api/v1/quick-notes`

Request:

```json
{
  "contentMarkdown": "text and attachment markdown",
  "source": "web-floating",
  "attachmentIds": ["..."]
}
```

Behavior:

- Validates content and attachment ownership.
- Binds referenced temporary attachments to the note.
- Creates the note with status `inbox`.
- Writes an audit log entry.
- Returns the created note.

### Update Note

`PUT /api/v1/quick-notes/{id}`

Request may include:

- `contentMarkdown`
- `status`
- `attachmentIds`

Behavior:

- Validates ownership.
- Validates status transitions.
- Validates Markdown attachment references.
- Binds newly referenced attachments.
- Marks removed attachment references as deleted or unbound according to service policy.
- Updates `updated_at`.
- Writes an audit log entry.

### Process Note

`POST /api/v1/quick-notes/{id}/process`

Marks the note `processed`. It does not create tasks, events, reminders, or AI suggestions.

### Archive Note

`POST /api/v1/quick-notes/{id}/archive`

Marks the note `archived` and sets `archived_at`.

### Restore Note

`POST /api/v1/quick-notes/{id}/restore`

Request:

```json
{
  "status": "inbox"
}
```

Restores an archived or processed note to `inbox` or another allowed basic status.

### Delete Note

`DELETE /api/v1/quick-notes/{id}`

Soft deletes the note and its attachment references. Physical object deletion can be deferred.

### Upload Attachment

`POST /api/v1/quick-notes/attachments`

Multipart request with a `file` field.

Returns:

- `id`
- `fileName`
- `contentType`
- `sizeBytes`
- `downloadUrl`
- `previewUrl` when useful and identical to authenticated download for Stage 4.

Uploads can be temporary with `quick_note_id = null` until the note is saved.

### Download Attachment

`GET /api/v1/quick-notes/attachments/{id}/download`

Authenticated download. The service must not expose raw object keys to the client.

### Delete Attachment

`DELETE /api/v1/quick-notes/attachments/{id}`

Deletes or unbinds an attachment reference owned by the current user. It must not remove objects used by another note.

## Server Responsibilities

The server owns:

- Quick note state transitions.
- User scoping and authorization.
- Attachment upload metadata.
- Stable attachment ids.
- Attachment binding and validation.
- Markdown attachment-reference validation.
- Audit logging for create, update, process, archive, restore, and delete.
- Safe error responses.

The server does not own:

- Web layout.
- Editor UI state.
- AI interpretation.
- Task/event conversion.
- File organization decisions.

## Web Responsibilities

Web owns:

- Global quick note button placement.
- Floating panel layout, dragging, close behavior, and local position persistence.
- Local draft preservation for unsaved floating input.
- MDXEditor integration.
- Attachment upload calls from editor handlers.
- Rendering attachment links as inline images or file blocks.
- Quick Notes page layout, titles, filters, and empty states.
- User-facing retry behavior after save/upload failures.

Web does not own:

- Quick note business state transitions.
- Attachment ownership rules.
- Object storage paths.
- AI interpretation.

## Error Handling

Attachment upload failure:

- Do not insert a broken Markdown link.
- Keep the editor content and show the failed file name.
- Let the user retry.

Note save failure:

- Do not clear the draft.
- Show a visible error near the save action.
- Preserve uploaded attachment references that still belong to the user.

Invalid attachment references:

- Reject save/update if Markdown references an attachment that is missing, deleted, or owned by another user.
- Return a clear validation error without leaking object keys.

Attachment download:

- Require authorization.
- Return 404 or 403 for inaccessible attachments.
- Never expose raw MinIO object keys.

Delete and archive:

- Note deletion is soft delete.
- Archive, process, and restore are reversible status transitions.

Orphan uploads:

- Stage 4 may leave temporary uploads unbound when the user abandons a draft.
- The data model should make future cleanup straightforward.

Editor loading failure:

- The affected panel/page should show a recoverable error.
- Other routes should keep working.

## Testing

Backend tests:

- Create a quick note with Markdown only.
- Create a quick note with image and file attachments.
- List notes by `inbox`, `processed`, and `archived`.
- Search notes by content.
- Read note details with attachments.
- Update Markdown content.
- Bind temporary attachment uploads on create/update.
- Reject attachment ids owned by another user.
- Reject Markdown references to inaccessible attachments.
- Process a note.
- Archive a note.
- Restore a note.
- Soft delete a note.
- Download only user-owned attachments.
- Delete or unbind an attachment.
- Write audit entries for important write operations.

Frontend tests:

- Quick notes API paths are stable.
- Quick notes TypeScript DTOs match expected shapes.
- Floating panel remains open on outside click.
- Floating panel closes only through the explicit close action.
- Dragging updates panel position state.
- Save failure preserves draft content.
- Successful save clears the floating draft.
- Sidebar includes the `快速记录` route.
- Quick Notes page renders status filters and editor surface.

Verification commands:

```powershell
dotnet test Pim.sln
npm --prefix src/client-web run build
```

Add focused frontend contract tests under `tests/client-web` for API paths and types, following the existing Today tests pattern.

## Manual Acceptance

- Open any authenticated Web page.
- Click the bottom-right quick note button.
- Confirm the floating panel opens.
- Click elsewhere on the page and confirm the panel stays open.
- Drag the panel and confirm it moves.
- Close the panel with its explicit close button.
- Reopen it and confirm unsaved draft text is still present during the same browser session.
- Create a note with text only.
- Create a note with pasted or dragged image content.
- Create a note with a non-image file link/block.
- Save and confirm the note appears on `/quick-notes`.
- Open `/quick-notes` from the sidebar.
- Filter inbox, processed, and archived notes.
- Edit a note in the full-page editor.
- Mark a note processed.
- Archive a note.
- Restore a note.
- Delete a note and confirm it disappears from normal lists.
- Download an attachment from a note.
- Confirm images and files appear as part of the note content flow, not as a separate primary attachment gallery.

## Future Extensibility

Stage 4 prepares but does not implement:

- AI quick note analysis.
- Conversion suggestions to tasks, events, reminders, files, or project ideas.
- Structured AI suggestion records.
- MCP low-risk `create_quick_note` tool.
- Desktop or Android quick note capture.
- Orphan-upload cleanup jobs.
- File-system integration after the mature file system stage.
- Attachment text extraction after the file indexing stage.

Future AI stages should treat `content_markdown` and attachment metadata as the raw source. AI suggestions must remain structured, reviewable, and user-confirmed before creating tasks, events, reminders, or file operations.

## Completion Definition

Stage 4 is complete when:

- Users can create quick notes with text and attachments from any Web page.
- The floating quick note panel is persistent, draggable, and explicitly closable.
- Users can manage notes from a dedicated `快速记录` page.
- Notes support `inbox`, `processed`, and `archived` statuses.
- Users can view, edit, process, archive, restore, and soft delete notes.
- Images and non-image attachments appear inside the note content flow.
- Attachments have stable ids and authenticated download URLs.
- The server owns note and attachment facts through APIs.
- Important writes are auditable.
- Backend tests and frontend build pass.
- The implementation leaves a clear path for future AI understanding and MCP creation without rewriting the foundation.
