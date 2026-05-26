# Stage 11/12 Files: Nextcloud Integration And AI File Layer Design

## Purpose

Stage 11/12 adds a file materials system without turning PIM into a custom cloud drive.

The selected provider stack is:

- Nextcloud as the file source of truth.
- OnlyOffice Docs as the online office editor integrated with Nextcloud.
- PIM as a thin file control console and AI understanding layer.
- Qdrant as the vector search service for current-version file chunks.
- The unified PIM LLM Gateway for all large-language-model calls.

This design depends on `2026-05-27-unified-llm-gateway-design.md`. The file module must not call LLM provider APIs directly.

## Scope

This design covers:

- Local Docker Compose support for Nextcloud, OnlyOffice, Qdrant, and supporting services.
- Per-PIM-user binding to a Nextcloud app password.
- A PIM file provider adapter for Nextcloud.
- File browsing, upload, download, move, rename, delete-to-trash, restore, version list, and version restore.
- Stable PIM file and version references based on provider ids, not paths alone.
- Thin Web file control console.
- Links to Nextcloud for complex native features.
- Current-version text extraction, chunking, local embedding, and Qdrant indexing.
- LLM-backed summaries, tags, and organization suggestions through `IAiGateway`.
- AI suggestions that are reviewable and non-executing in the first version.

This design does not cover:

- Task-file binding.
- Event-file binding.
- Today file context.
- Project/workspace binding.
- File recommendations for tasks or events.
- Android file UI.
- MCP file tools.
- Replacing Nextcloud sharing, permissions, comments, or collaboration UI.
- Iframe embedding of Nextcloud.
- Full historical-version vector indexing.

Task, event, and file workspace binding remains Stage 13.

## Architecture

### Service Roles

Nextcloud

- Owns file contents.
- Owns folders.
- Owns trash and restore.
- Owns version history and version restore.
- Owns normal sync clients.
- Owns sharing and complex permissions.
- Owns the OnlyOffice integration.

OnlyOffice Docs

- Provides online office viewing/editing for common office files.
- Is accessed through Nextcloud links or Nextcloud Web UI.
- Is not embedded directly into PIM.

PIM Files module

- Stores provider bindings.
- Syncs file and version metadata.
- Provides a thin file control console.
- Calls Nextcloud via a server-side adapter.
- Records audit logs for PIM-initiated file operations.
- Stores file AI state and suggestions.
- Coordinates indexing jobs.

PostgreSQL

- Stores PIM file metadata.
- Stores version metadata.
- Stores index jobs.
- Stores text chunks and AI results.
- Stores AI suggestions and audit links.

Qdrant

- Stores current-version chunk vectors.
- Stores only retrieval payload metadata.
- Is rebuildable from PostgreSQL and Nextcloud.
- Is not a source of truth.

Unified LLM Gateway

- Provides all LLM calls through `IAiGateway`.
- Routes through Microsoft.Extensions.AI and LiteLLM.
- Logs complete prompt/output, tokens, and schema validation details.
- Enforces schema and retry limits.

### Important Boundaries

PIM does not:

- Reimplement a cloud drive.
- Reimplement Office editing.
- Embed Nextcloud UI in an iframe.
- Store upstream Nextcloud passwords in plaintext.
- Use paths as the only file identity.
- Let AI move, rename, delete, or restore files automatically.
- Send files to LLMs outside the unified `IAiGateway`.

PIM does:

- Provide controlled common file operations.
- Keep stable file and version references.
- Provide AI search and understanding over current versions.
- Link out to Nextcloud for complex native workflows.

## Provider Choice

Nextcloud is selected over Seafile for the first provider because:

- It is closer to a complete cloud drive and collaboration platform.
- WebDAV and related APIs expose stable file metadata such as file ids and etags.
- It integrates naturally with OnlyOffice.
- It fits the "real cloud drive first, PIM intelligence second" goal.

Seafile remains a possible future provider for strong block-level sync and efficient large-version workloads.

OnlyOffice is selected over Collabora because:

- The user's primary risk is preserving Word/Office report structure.
- OnlyOffice is more directly oriented around OOXML formats such as `docx`, `xlsx`, and `pptx`.
- The first version should prioritize common Microsoft Office document compatibility.

No online editor is treated as perfectly safe for complex reports. PIM should default to view mode and keep editing explicit.

## View And Edit Rules

Default file click is view/read-only.

Viewing should:

- Not write source files.
- Not create new versions.
- Not update file modification time.
- Prefer provider/native preview links.

Editing should:

- Be a separate explicit action.
- Prefer OnlyOffice through Nextcloud.
- Be enabled primarily for supported office formats.
- Rely on Nextcloud version history for recovery.
- Show a caution for complex Word reports that Windows Word remains the safest editor.

Non-OOXML or conversion-prone formats should show a warning before online editing because conversion or save may affect formatting.

## User Binding Model

Each PIM user binds their own Nextcloud account using an app password.

Stored provider settings:

- Nextcloud base URL.
- Nextcloud username.
- App password secret.
- Status and last sync details.

PIM operations run as the bound Nextcloud user. This keeps permissions, trash, versions, and sharing behavior aligned with Nextcloud.

This stage does not implement unified SSO, OIDC, or per-user LLM keys.

## Data Model

### `file_providers`

- `id`
- `user_id`
- `provider`: `nextcloud`
- `base_url`
- `internal_base_url`
- `username`
- `app_password_secret`
- `status`
- `last_sync_at`
- `last_error`
- `created_at`
- `updated_at`

### `file_items`

Current metadata for files and folders.

- `id`
- `provider_id`
- `external_file_id`
- `parent_external_file_id`
- `path`
- `name`
- `item_type`: `file` or `folder`
- `mime_type`
- `size`
- `etag`
- `content_hash`
- `current_version_id`
- `permissions`
- `is_deleted`
- `deleted_at`
- `last_seen_at`
- `created_at`
- `modified_at`
- `synced_at`

`external_file_id` is the stable provider identity. `path` is display and navigation data, not the only identity.

### `file_versions`

Version references.

- `id`
- `file_item_id`
- `external_version_id`
- `etag`
- `size`
- `modified_at`
- `source`: `current` or `history`
- `is_current`
- `synced_at`

Only current versions are automatically indexed into Qdrant. Historical versions remain available for metadata, download, and restore.

### `file_index_jobs`

Asynchronous indexing work.

- `id`
- `file_item_id`
- `version_id`
- `status`: `pending`, `running`, `succeeded`, `failed`, or `skipped`
- `stage`: `metadata`, `text`, `chunk`, `embedding`, `summary`, `tags`, or `suggestions`
- `attempt_count`
- `last_error`
- `started_at`
- `finished_at`

### `file_chunks`

Current-version text chunks.

- `id`
- `file_item_id`
- `version_id`
- `chunk_index`
- `text`
- `text_hash`
- `start_offset`
- `end_offset`
- `qdrant_point_id`

Chunks are the evidence source for summaries, tags, semantic search, and suggestions.

### `file_ai_results`

Derived AI understanding for the current version.

- `id`
- `file_item_id`
- `version_id`
- `summary`
- `tags_json`
- `language`
- `sensitivity`
- `generated_at`
- `model`
- `ai_request_log_id`
- `evidence_chunk_ids_json`

The `ai_request_log_id` links to the unified AI request log that stores the full prompt, output, token usage, and schema validation details.

### `file_suggestions`

AI-generated suggestions.

- `id`
- `file_item_id`
- `suggestion_type`: `rename`, `move`, `tag`, `duplicate`, `stale`, or `unfiled`
- `title`
- `reason`
- `confidence`
- `payload_json`
- `status`: `pending`, `dismissed`, or `accepted`
- `ai_request_log_id`
- `created_at`
- `updated_at`

First version acceptance means "the user agrees this suggestion is useful." It does not automatically execute file operations.

## API Design

Root path:

`/api/v1/files`

Provider APIs:

- `GET /providers`
- `POST /providers/nextcloud`
- `POST /providers/{id}/test`
- `POST /providers/{id}/sync`

File APIs:

- `GET /items?path=/...`
- `GET /items/{id}`
- `POST /items/upload`
- `GET /items/{id}/download`
- `POST /items/{id}/move`
- `POST /items/{id}/rename`
- `DELETE /items/{id}`

Trash APIs:

- `GET /trash`
- `POST /trash/{id}/restore`

Version APIs:

- `GET /items/{id}/versions`
- `GET /items/{id}/versions/{versionId}/download`
- `POST /items/{id}/versions/{versionId}/restore-preview`
- `POST /items/{id}/versions/{versionId}/restore`

Index and search APIs:

- `POST /items/{id}/index`
- `GET /search?q=...&mode=keyword|semantic|hybrid`
- `GET /suggestions`
- `POST /suggestions/{id}/dismiss`
- `POST /suggestions/{id}/accept`

Open links:

- `GET /items/{id}/open-link?mode=view|edit|nextcloud`

This returns a provider URL or a redirect target. PIM does not iframe the target.

## Web Design

Add `/files`.

Layout:

- Left rail: provider status, sync action, folder tree.
- Main pane: breadcrumb, search, upload action, sortable file list.
- Detail pane: metadata, version state, index state, summary, tags, suggestions, and actions.

Primary actions:

- View.
- Edit.
- Download.
- Upload.
- Move.
- Rename.
- Delete to trash.
- Restore.
- Version history.
- Open in Nextcloud.

Complex actions link out to Nextcloud:

- Sharing.
- Complex permissions.
- Comments.
- Collaborative member management.
- Advanced OnlyOffice session behavior.
- Bulk workflows beyond PIM's controlled operations.

The UI must make view and edit distinct. Viewing is the default.

## Nextcloud Adapter

The provider adapter exposes provider-neutral operations:

- Test connection.
- List folder.
- Get metadata.
- Upload.
- Download.
- Move.
- Rename.
- Delete to trash.
- List trash.
- Restore from trash.
- List versions.
- Download version.
- Restore version.
- Build view/edit/open links.

The Nextcloud implementation uses Nextcloud APIs/WebDAV as appropriate. PIM code outside the adapter should not depend on Nextcloud-specific response shapes.

## AI Indexing Flow

1. Sync metadata from Nextcloud.
2. Detect changed current version by etag/version id.
3. Create index job.
4. Download current file version from Nextcloud.
5. Extract text locally using Tika or local parsers.
6. Mark unsupported or empty files as `skipped`.
7. Chunk extracted text.
8. Store chunks in PostgreSQL.
9. Generate local embeddings.
10. Upsert chunk vectors into Qdrant.
11. Remove or mark old current-version vectors when current version changes.
12. Use `IAiGateway` for summaries, tags, and organization suggestions.
13. Store `file_ai_results` and `file_suggestions` with links to `ai_request_logs`.

Historical versions:

- Are not automatically vector indexed.
- Remain visible in version history.
- Can be downloaded or restored.
- May support traditional metadata search.
- Can be indexed later only if a future explicit feature requests it.

## Qdrant Design

Qdrant stores one point per current-version chunk.

Payload:

- `userId`
- `providerId`
- `fileId`
- `versionId`
- `chunkId`
- `path`
- `mimeType`
- `modifiedAt`

Qdrant is filtered by user/provider/file metadata before results are returned.

Qdrant is rebuildable from:

- Nextcloud current files.
- PostgreSQL file metadata.
- PostgreSQL chunks.

No irreplaceable facts live only in Qdrant.

## LLM Usage

All LLM calls use the unified `IAiGateway`.

File purposes:

- `file.summary`
- `file.tags`
- `file.organization_suggestions`
- `file.duplicate_explanation`
- `file.stale_reason`

Each structured output task registers a JSON Schema through the AI schema registry.

The file module must pass:

- `module = files`
- A purpose such as `file.summary`
- `sourceObjectType = file`
- `sourceObjectId = file_items.id`
- Evidence chunk ids in metadata

The AI request log stores:

- Complete prompt.
- Complete output.
- Token usage.
- Model.
- Schema result.
- The file id and version id.

The file module must not make direct HTTP calls to LiteLLM or any upstream model provider.

## Safety Rules

AI may suggest:

- Tags.
- Summary.
- Similar/duplicate relationship.
- Possible stale status.
- Possible unfiled status.
- Possible rename.
- Possible destination folder.

AI may not directly:

- Move a file.
- Rename a file.
- Delete a file.
- Restore a version.
- Share a file.
- Change permissions.

Future execution of AI file operation suggestions must use:

- Impact preview.
- User confirmation.
- Server-side execution.
- Audit logging.
- Recovery path.

## Deployment

Docker Compose adds:

- Nextcloud.
- Nextcloud database.
- Redis, if required by the Nextcloud setup.
- OnlyOffice Docs.
- Qdrant.
- Tika, optional but recommended.
- LiteLLM from the unified LLM gateway stage.

The PIM API connects to:

- Nextcloud internal URL for server-side adapter operations.
- Nextcloud public URL for links returned to Web.
- Qdrant internal URL.
- LiteLLM through `IAiGateway`.

Suggested configuration:

- `Nextcloud:PublicBaseUrl`
- `Nextcloud:InternalBaseUrl`
- `OnlyOffice:PublicUrl`
- `OnlyOffice:JwtSecret`
- `Qdrant:Url`
- `Qdrant:Collection`
- `Embedding:Provider`
- `Embedding:BaseUrl`
- `Files:MaxInlineTextBytes`
- `Files:AiDisabledPathPatterns`

LLM configuration belongs to the unified AI gateway configuration, not the files module.

## Testing

Backend tests:

- Provider settings are saved per user.
- Nextcloud adapter contract maps stable ids, paths, etags, sizes, and mime types.
- File item identity does not rely on path only.
- Move and rename preserve stable external id when provider supports it.
- Delete uses trash semantics.
- Version list and restore preview work.
- Index jobs are created when current version changes.
- Historical versions are not vector indexed automatically.
- Qdrant payload references current file/version/chunk ids.
- File AI calls go through `IAiGateway`.
- AI suggestions do not execute file operations.

Frontend tests:

- File API paths are stable.
- File DTO types include provider id, external id, version id, and index status.
- View and edit actions are distinct.
- Search modes produce expected request URLs.
- Suggestion dismissal and acceptance call the correct endpoints.

Manual verification:

- Start Docker Compose.
- Bind a PIM user to Nextcloud with an app password.
- Sync metadata.
- Browse files and folders.
- Upload and download a file.
- Move and rename a file.
- Delete a file to trash and restore it.
- View version history.
- Restore a version after confirmation.
- Open a `.docx` in view mode without changing it.
- Open a `.docx` in edit mode through Nextcloud/OnlyOffice.
- Confirm the edited file creates a recoverable version in Nextcloud.
- Index a current text-like file.
- Run keyword, semantic, and hybrid search.
- Generate summary, tags, and suggestions through `IAiGateway`.
- Confirm AI request logs show full prompt/output and token usage.

## Completion Definition

Stage 11/12 is complete when:

- Nextcloud and OnlyOffice run from the local compose setup.
- PIM users can bind Nextcloud app passwords.
- PIM can browse and operate files through a server-side Nextcloud adapter.
- PIM preserves stable file and version references.
- PIM can view, edit, and open files through provider links without iframe embedding.
- Current-version indexing works through local extraction, local embeddings, and Qdrant.
- Historical versions are available but not automatically vector indexed.
- File summaries, tags, and suggestions use the unified `IAiGateway`.
- AI suggestions are visible and dismissible but do not automatically execute operations.
- Task, event, Today, and project binding are left for Stage 13.
