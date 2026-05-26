# Quick Notes Stage 4 Acceptance

Stage 4 verifies the quick note capture and management loop: server-backed Markdown notes, inline attachments, a persistent floating capture panel, and the full-page `/quick-notes` management view.

## Scope

- Capture Markdown quick notes from the Web client.
- Keep note text and attachment references together in Markdown.
- Upload image and non-image attachments to server storage.
- Bind referenced attachments to notes during create and update.
- Support inbox, processed, and archived note states.
- Soft delete notes and attachments.
- Provide a persistent, draggable floating quick note panel with an explicit close button.
- Provide a full-page quick note management route at `/quick-notes`.
- Leave AI classification, automatic task or event creation, MCP exposure, formal file-system organization, and orphan attachment cleanup jobs for later stages.

## API Checks

- `GET /api/v1/quick-notes?status=inbox&page=1&pageSize=30` returns a paged note list.
- `POST /api/v1/quick-notes` creates an inbox note.
- `GET /api/v1/quick-notes/{id}` returns Markdown and attachments.
- `PUT /api/v1/quick-notes/{id}` updates Markdown and preserves referenced attachments.
- `POST /api/v1/quick-notes/{id}/process` marks a note processed.
- `POST /api/v1/quick-notes/{id}/archive` archives a note.
- `POST /api/v1/quick-notes/{id}/restore` restores a note to the requested status.
- `DELETE /api/v1/quick-notes/{id}` soft deletes a note.
- `POST /api/v1/quick-notes/attachments` uploads a file.
- `GET /api/v1/quick-notes/attachments/{id}/download` downloads only user-owned, non-deleted attachments.

## Web Checks

- Open any authenticated Web route.
- Click the bottom-right quick note button.
- Confirm the panel opens and stays open after clicking elsewhere.
- Drag the panel and confirm it moves without leaving the viewport.
- Close the panel with its close button.
- Reopen it and confirm an unsaved draft from the same browser session remains.
- Save a text-only note from the floating panel.
- Save a note with an inline image from the editor.
- Save a note with a non-image file link from the editor.
- Open `/quick-notes` from the sidebar.
- Confirm inbox, processed, and archived filters work.
- Search quick notes from the management page.
- Edit a note in the full-page editor and confirm existing attachments remain available.
- Mark a note processed.
- Archive and restore a note.
- Delete a note and confirm selection does not jump back to the deleted note.
- Select another note while an action is pending and confirm the later response does not steal selection.
- Download an attachment from a note.

## Data State Checks

- Confirm newly created notes default to `inbox`.
- Confirm archived notes have `archivedAt` set.
- Confirm restored notes clear archive state.
- Confirm soft-deleted notes no longer appear in normal list or detail responses.
- Confirm attachments referenced by Markdown are returned with the note detail.
- Confirm deleted attachments cannot be rebound by stale Markdown references.
- Confirm another user's note or attachment cannot be listed, updated, deleted, or downloaded.

## Verification Commands

Run backend tests:

```powershell
dotnet test Pim.sln
```

Build the web client:

```powershell
npm --prefix src/client-web run build
```

Run focused Web checks:

```powershell
npm --prefix src/client-web exec tsx -- tests\client-web\quickNotesApiPath.test.ts
npm --prefix src/client-web exec tsc -- -p tests\client-web\tsconfig.quick-notes.json
npm --prefix src/client-web exec tsx -- tests\client-web\quickNoteFloatingState.test.ts
```

Check current git state:

```powershell
git status --short --branch
```
