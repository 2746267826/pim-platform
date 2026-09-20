# PIM Backup And Restore

## What To Back Up

- PostgreSQL database `pim`.
- API `/data` volume, including logs when needed.

File **content** is not part of a PIM backup: since files v2 the source of truth is the user's own
OneDrive (`designs/onedrive-files-v2.md`). What PIM stores locally is metadata plus:
- `file_items` / `file_providers` — the OneDrive file tree and binding.
- `file_text_snapshots` — pre-edit text snapshots (PostgreSQL, not object storage).
- `quick_note_attachments` — attachment metadata; content lives in OneDrive under `/PIM/...`.
Backing up these tables keeps the tree, snapshots and attachment links consistent. MinIO is no
longer used by the files or attachments line (retired in P4).
- JWT private key files under `keys/` or `/data/keys`.
- Local deployment `.env` values.
- Windows daemon config at `%LOCALAPPDATA%\PIM\config.json`.

## What Is Not Backed Up Automatically

- Generated `bin/`, `obj/`, `build/`, `dist/`, and API `wwwroot` build artifacts.
- npm caches and temporary `.dotnet-*` directories.
- Local logs unless the operator explicitly copies them.

## Manual Restore Verification

1. Restore PostgreSQL data (metadata, snapshots, attachment links).
   File content comes back from OneDrive once the user re-binds — no object-store restore needed.
2. Restore keys and environment values.
3. Start the API at `http://127.0.0.1:5858`.
4. Open Web and confirm login works.
5. Open `状态信息` and confirm API and database are healthy.
6. Start the Windows daemon and confirm its heartbeat appears.
7. Run `dotnet test Pim.sln`.
8. Run `npm --prefix src/client-web run build`.
